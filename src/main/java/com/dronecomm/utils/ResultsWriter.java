package com.dronecomm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Exports simulation outputs (JSON, CSV, text) and generates optional charts.
 * Small, reusable helper used by runners to save results and visualizations.
 */
public class ResultsWriter {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultsWriter.class);
    
    private final String outputDirectory;
    private final ObjectMapper objectMapper;
    
    public ResultsWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        
        // Create output directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(outputDirectory));
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", outputDirectory, e);
        }
    }
    
    /**
     * Write metrics data to JSON file.
     */
    public void writeMetricsToJson(MetricsCollector metrics, String filename) {
        try {
            MetricsData data = new MetricsData();
            data.averageLatency = metrics.getAverageLatency();
            data.averageThroughput = metrics.getAverageThroughput();
            data.totalEnergyConsumed = metrics.getTotalEnergyConsumed();
            data.averageLoadVariance = metrics.getAverageLoadVariance();
            data.averageUserSatisfaction = metrics.getAverageUserSatisfaction();
            data.totalHandovers = metrics.getTotalHandovers();
            data.totalConnections = metrics.getTotalConnections();
            data.latencyOverTime = metrics.getLatencyOverTime();
            data.throughputOverTime = metrics.getThroughputOverTime();
            data.energyConsumptionOverTime = metrics.getEnergyConsumptionOverTime();
            data.loadDistributionOverTime = metrics.getLoadDistributionOverTime();
            data.userSatisfactionOverTime = metrics.getUserSatisfactionOverTime();
            
            String filePath = Paths.get(outputDirectory, filename).toString();
            objectMapper.writeValue(new File(filePath), data);
            logger.info("Metrics written to JSON file: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to write metrics to JSON: {}", filename, e);
        }
    }
    
    /**
     * Write metrics data to CSV file.
     */
    public void writeMetricsToCsv(MetricsCollector metrics, String filename) {
        try {
            String filePath = Paths.get(outputDirectory, filename).toString();
            FileWriter writer = new FileWriter(filePath);
            
            // Write header
            writer.write("Time,Latency,Throughput,EnergyConsumption,LoadVariance,UserSatisfaction\n");
            
            // Get all time points
            Map<Double, Double> latencyData = metrics.getLatencyOverTime();
            Map<Double, Double> throughputData = metrics.getThroughputOverTime();
            Map<Double, Double> energyData = metrics.getEnergyConsumptionOverTime();
            Map<Double, Double> loadData = metrics.getLoadDistributionOverTime();
            Map<Double, Integer> satisfactionData = metrics.getUserSatisfactionOverTime();
            
            // Write time series data
            for (Double time : latencyData.keySet()) {
                writer.write(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%d\n",
                        time,
                        latencyData.getOrDefault(time, 0.0),
                        throughputData.getOrDefault(time, 0.0),
                        energyData.getOrDefault(time, 0.0),
                        loadData.getOrDefault(time, 0.0),
                        satisfactionData.getOrDefault(time, 0)));
            }
            
            writer.close();
            logger.info("Metrics written to CSV file: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to write metrics to CSV: {}", filename, e);
        }
    }
    
    /**
     * Generate summary report text file.
     */
    public void writeSummaryReport(MetricsCollector metrics, String scenarioName, String filename) {
        try {
            String filePath = Paths.get(outputDirectory, filename).toString();
            FileWriter writer = new FileWriter(filePath);
            
            writer.write("=== Drone Communication Simulation Report ===\n");
            writer.write("Scenario: " + scenarioName + "\n");
            writer.write("Generated at: " + java.time.LocalDateTime.now() + "\n\n");
            
            writer.write(metrics.generateSummaryReport());
            
            writer.write("\n=== Performance Analysis ===\n");
            writer.write("Network Performance:\n");
            writer.write(String.format("- Latency Quality: %s\n", 
                    metrics.getAverageLatency() < 50 ? "Excellent" : 
                    metrics.getAverageLatency() < 100 ? "Good" : "Poor"));
            writer.write(String.format("- Load Balancing: %s\n", 
                    metrics.getAverageLoadVariance() < 5 ? "Well Balanced" : 
                    metrics.getAverageLoadVariance() < 15 ? "Moderate" : "Unbalanced"));
            writer.write(String.format("- User Experience: %s\n", 
                    metrics.getAverageUserSatisfaction() > 0.8 ? "Excellent" : 
                    metrics.getAverageUserSatisfaction() > 0.6 ? "Good" : "Poor"));
            
            writer.close();
            logger.info("Summary report written to: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to write summary report: {}", filename, e);
        }
    }
    
    /**
     * Generate latency over time chart.
     */
    public void generateLatencyChart(Map<Double, Double> latencyData, String filename) {
        try {
            XYSeries series = new XYSeries("Latency");
            
            for (Map.Entry<Double, Double> entry : latencyData.entrySet()) {
                series.add(entry.getKey(), entry.getValue());
            }
            
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);
            
            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Network Latency Over Time",
                    "Time (seconds)",
                    "Latency (ms)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            
            // Customize chart appearance
            chart.setBackgroundPaint(Color.WHITE);
            chart.getXYPlot().setBackgroundPaint(Color.LIGHT_GRAY);
            chart.getXYPlot().setDomainGridlinePaint(Color.WHITE);
            chart.getXYPlot().setRangeGridlinePaint(Color.WHITE);
            
            String filePath = Paths.get(outputDirectory, filename).toString();
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            logger.info("Latency chart saved to: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to generate latency chart: {}", filename, e);
        }
    }
    
    /**
     * Generate throughput over time chart.
     */
    public void generateThroughputChart(Map<Double, Double> throughputData, String filename) {
        try {
            XYSeries series = new XYSeries("Throughput");
            
            for (Map.Entry<Double, Double> entry : throughputData.entrySet()) {
                series.add(entry.getKey(), entry.getValue());
            }
            
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);
            
            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Network Throughput Over Time",
                    "Time (seconds)",
                    "Throughput (Mbps)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            
            // Customize chart appearance
            chart.setBackgroundPaint(Color.WHITE);
            chart.getXYPlot().setBackgroundPaint(Color.LIGHT_GRAY);
            chart.getXYPlot().setDomainGridlinePaint(Color.WHITE);
            chart.getXYPlot().setRangeGridlinePaint(Color.WHITE);
            
            String filePath = Paths.get(outputDirectory, filename).toString();
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            logger.info("Throughput chart saved to: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to generate throughput chart: {}", filename, e);
        }
    }
    
    /**
     * Generate energy consumption chart.
     */
    public void generateEnergyChart(Map<Double, Double> energyData, String filename) {
        try {
            XYSeries series = new XYSeries("Energy Consumption");
            
            for (Map.Entry<Double, Double> entry : energyData.entrySet()) {
                series.add(entry.getKey(), entry.getValue());
            }
            
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);
            
            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Energy Consumption Over Time",
                    "Time (seconds)",
                    "Energy (Joules)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            
            // Customize chart appearance
            chart.setBackgroundPaint(Color.WHITE);
            chart.getXYPlot().setBackgroundPaint(Color.LIGHT_GRAY);
            chart.getXYPlot().setDomainGridlinePaint(Color.WHITE);
            chart.getXYPlot().setRangeGridlinePaint(Color.WHITE);
            
            String filePath = Paths.get(outputDirectory, filename).toString();
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            logger.info("Energy chart saved to: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to generate energy chart: {}", filename, e);
        }
    }
    
    /**
     * Generate comparison chart for different scenarios.
     */
    public void generateComparisonChart(Map<String, Double> scenarioResults, 
                                      String metricName, String filename) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            for (Map.Entry<String, Double> entry : scenarioResults.entrySet()) {
                dataset.addValue(entry.getValue(), metricName, entry.getKey());
            }
            
            JFreeChart chart = ChartFactory.createBarChart(
                    metricName + " Comparison Across Scenarios",
                    "Scenario",
                    metricName,
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            
            // Customize chart appearance
            chart.setBackgroundPaint(Color.WHITE);
            chart.getCategoryPlot().setBackgroundPaint(Color.LIGHT_GRAY);
            chart.getCategoryPlot().setDomainGridlinePaint(Color.WHITE);
            chart.getCategoryPlot().setRangeGridlinePaint(Color.WHITE);
            
            String filePath = Paths.get(outputDirectory, filename).toString();
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            logger.info("Comparison chart saved to: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to generate comparison chart: {}", filename, e);
        }
    }
    
    /**
     * Data class for JSON serialization.
     */
    public static class MetricsData {
        public double averageLatency;
        public double averageThroughput;
        public double totalEnergyConsumed;
        public double averageLoadVariance;
        public double averageUserSatisfaction;
        public int totalHandovers;
        public int totalConnections;
        public Map<Double, Double> latencyOverTime;
        public Map<Double, Double> throughputOverTime;
        public Map<Double, Double> energyConsumptionOverTime;
        public Map<Double, Double> loadDistributionOverTime;
        public Map<Double, Integer> userSatisfactionOverTime;
    }
}