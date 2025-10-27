package com.dronecomm.analysis;

import com.dronecomm.enums.AlgorithmType;
import com.dronecomm.enums.ScenarioType;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ChartGenerator {
    private static final String CHARTS_DIR = "results/charts";
    private final String timestamp;
    
    public ChartGenerator() {
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        createChartsDirectory();
    }
    
    private void createChartsDirectory() {
        File chartsDir = new File(CHARTS_DIR);
        if (!chartsDir.exists()) {
            chartsDir.mkdirs();
        }
    }
    
    /**
     * Create readable scenario labels for charts
     */
    private String getReadableScenarioLabel(ScenarioType scenario, Integer userCount) {
        String scenarioName;
        switch (scenario) {
            case LOW_MOBILITY:
                scenarioName = "Low Mobility";
                break;
            case HIGH_MOBILITY:
                scenarioName = "High Mobility";
                break;
            case URBAN_HOTSPOT:
                scenarioName = "Urban Hotspot";
                break;
            case MIXED_TRAFFIC:
                scenarioName = "Mixed Traffic";
                break;
            case HOTSPOT_SCENARIO:
                scenarioName = "Hotspot";
                break;
            case ENERGY_CONSTRAINED:
                scenarioName = "Energy Constrained";
                break;
            default:
                scenarioName = scenario.name();
        }
        return scenarioName + " (" + userCount + " users)";
    }
    
    public void generateAllCharts(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        generateThroughputComparisonChart(allResults);
        generateLatencyComparisonChart(allResults);
        generateEnergyConsumptionChart(allResults);
        // QoS Violation chart removed - not in research paper
        generateUserSatisfactionChart(allResults);
        generateScalabilityChart(allResults);
        // Disabled: Algorithm Performance Summary chart (4-column view with hardcoded demo values)
        // generateAlgorithmPerformanceRadarChart(allResults);
    }
    
    private void generateThroughputComparisonChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (ScenarioType scenario : allResults.keySet()) {
            for (Integer userCount : allResults.get(scenario).keySet()) {
                String category = getReadableScenarioLabel(scenario, userCount);
                
                for (AlgorithmType algorithm : allResults.get(scenario).get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                    dataset.addValue(result.getAverageThroughput() / 1e6, algorithm.getDisplayName(), category);
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Average Throughput Comparison Across Scenarios",
            "Scenario and User Count",
            "Throughput (Mbps)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.BLUE);
        saveChart(chart, "throughput_comparison");
    }
    
    private void generateLatencyComparisonChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (ScenarioType scenario : allResults.keySet()) {
            for (Integer userCount : allResults.get(scenario).keySet()) {
                String category = getReadableScenarioLabel(scenario, userCount);
                
                for (AlgorithmType algorithm : allResults.get(scenario).get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                    dataset.addValue(result.getAverageLatency(), algorithm.getDisplayName(), category);
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Average Latency Comparison Across Scenarios",
            "Scenario and User Count",
            "Latency (ms)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.RED);
        saveChart(chart, "latency_comparison");
    }
    
    private void generateEnergyConsumptionChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (ScenarioType scenario : allResults.keySet()) {
            for (Integer userCount : allResults.get(scenario).keySet()) {
                String category = getReadableScenarioLabel(scenario, userCount);
                
                for (AlgorithmType algorithm : allResults.get(scenario).get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                    dataset.addValue(result.getTotalEnergyConsumption() / 1000, algorithm.getDisplayName(), category);
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Energy Consumption Comparison Across Scenarios",
            "Scenario and User Count", 
            "Energy Consumption (kJ)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.GREEN);
        saveChart(chart, "energy_consumption");
    }
    
    
    // QoS Violation chart method removed - not in research paper
    
    private void generateUserSatisfactionChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (ScenarioType scenario : allResults.keySet()) {
            for (Integer userCount : allResults.get(scenario).keySet()) {
                String category = getReadableScenarioLabel(scenario, userCount);
                
                for (AlgorithmType algorithm : allResults.get(scenario).get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                    dataset.addValue(result.getUserSatisfaction() * 100, algorithm.getDisplayName(), category);
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "User Satisfaction Comparison Across Scenarios",
            "Scenario and User Count",
            "User Satisfaction (%)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.MAGENTA);
        saveChart(chart, "user_satisfaction");
    }
    
    private void generateScalabilityChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Focus on one scenario for scalability analysis
        ScenarioType targetScenario = ScenarioType.HIGH_MOBILITY;
        if (allResults.containsKey(targetScenario)) {
            Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>> scenarioResults = allResults.get(targetScenario);
            
            for (Integer userCount : scenarioResults.keySet()) {
                for (AlgorithmType algorithm : scenarioResults.get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = scenarioResults.get(userCount).get(algorithm);
                    dataset.addValue(result.getAverageThroughput() / 1e6, algorithm.getDisplayName(), userCount.toString());
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
            "Scalability Analysis - Throughput vs User Count (High Mobility Scenario)",
            "Number of Users",
            "Throughput (Mbps)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.CYAN);
        saveChart(chart, "scalability_analysis");
    }
    
    private void generateAlgorithmPerformanceRadarChart(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        // For simplicity, create a summary performance chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Calculate average performance across all scenarios
        Map<AlgorithmType, Double> avgThroughput = new java.util.HashMap<>();
        Map<AlgorithmType, Double> avgLatency = new java.util.HashMap<>();
        Map<AlgorithmType, Double> avgEnergy = new java.util.HashMap<>();
        Map<AlgorithmType, Double> avgSatisfaction = new java.util.HashMap<>();
        Map<AlgorithmType, Integer> counts = new java.util.HashMap<>();
        
        for (ScenarioType scenario : allResults.keySet()) {
            for (Integer userCount : allResults.get(scenario).keySet()) {
                for (AlgorithmType algorithm : allResults.get(scenario).get(userCount).keySet()) {
                    ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                    
                    avgThroughput.put(algorithm, avgThroughput.getOrDefault(algorithm, 0.0) + result.getAverageThroughput());
                    avgLatency.put(algorithm, avgLatency.getOrDefault(algorithm, 0.0) + result.getAverageLatency());
                    avgEnergy.put(algorithm, avgEnergy.getOrDefault(algorithm, 0.0) + result.getTotalEnergyConsumption());
                    avgSatisfaction.put(algorithm, avgSatisfaction.getOrDefault(algorithm, 0.0) + result.getUserSatisfaction());
                    counts.put(algorithm, counts.getOrDefault(algorithm, 0) + 1);
                }
            }
        }
        
        // Normalize scores (0-100 scale)
        for (AlgorithmType algorithm : avgThroughput.keySet()) {
            int count = counts.get(algorithm);
            double throughputScore = (avgThroughput.get(algorithm) / count) / 1e6 * 2; // Scale to 0-100
            double latencyScore = Math.max(0, 100 - (avgLatency.get(algorithm) / count) * 10); // Lower is better
            double energyScore = Math.max(0, 100 - (avgEnergy.get(algorithm) / count) / 50000); // Lower is better
            double satisfactionScore = (avgSatisfaction.get(algorithm) / count); // Already percentage
            
            dataset.addValue(Math.min(100, throughputScore), algorithm.getDisplayName(), "Throughput");
            dataset.addValue(Math.min(100, latencyScore), algorithm.getDisplayName(), "Latency");
            dataset.addValue(Math.min(100, energyScore), algorithm.getDisplayName(), "Energy Efficiency");
            dataset.addValue(Math.min(100, satisfactionScore), algorithm.getDisplayName(), "Satisfaction");
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Algorithm Performance Summary (Normalized Scores 0-100)",
            "Performance Metrics",
            "Normalized Score",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeChart(chart, Color.DARK_GRAY);
        saveChart(chart, "algorithm_performance_summary");
    }
    
    private void customizeChart(JFreeChart chart, Color themeColor) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.getTitle().setPaint(Color.BLACK);
        
        if (chart.getCategoryPlot() != null) {
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(new Color(220, 220, 220));
            plot.setRangeGridlinePaint(new Color(220, 220, 220));
            plot.setOutlineVisible(true);
            plot.setOutlinePaint(Color.BLACK);
            plot.setOutlineStroke(new BasicStroke(1.0f));
            
            // Improve axis labeling
            plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
            plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
            plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
            plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
            
            // Rotate x-axis labels for better readability
            plot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.UP_45);
            
            if (plot.getRenderer() instanceof BarRenderer) {
                BarRenderer renderer = (BarRenderer) plot.getRenderer();
                
                // Set distinct colors for each algorithm - matching research paper style
                Color[] algorithmColors = {
                    new Color(31, 119, 180),    // Nash Equilibrium - Blue
                    new Color(255, 127, 14),    // Stackelberg Game - Orange  
                    new Color(44, 160, 44),     // Cooperative Game - Green
                    new Color(214, 39, 40),     // Auction-based - Red
                    new Color(148, 103, 189),   // Random Assignment - Purple
                    new Color(140, 86, 75),     // Round Robin - Brown
                    new Color(227, 119, 194),   // Greedy Assignment - Pink
                    new Color(127, 127, 127),   // Nearest Neighbor - Gray
                    new Color(188, 189, 34),    // Load Balanced - Olive
                    new Color(23, 190, 207)     // Signal Strength - Cyan
                };
                
                // Apply colors to series (algorithms)
                for (int i = 0; i < algorithmColors.length && i < plot.getDataset().getColumnCount(); i++) {
                    renderer.setSeriesPaint(i, algorithmColors[i]);
                }
                
                // Enhanced bar styling
                renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
                renderer.setShadowVisible(false);
                renderer.setDrawBarOutline(true);
                renderer.setItemMargin(0.1);
                renderer.setMaximumBarWidth(0.08);
            }
            
            // Legend styling
            if (chart.getLegend() != null) {
                chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 13));
                chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
            }
        }
    }
    
    private void saveChart(JFreeChart chart, String fileName) {
        try {
            String fullFileName = String.format("%s/%s_%s.png", CHARTS_DIR, fileName, timestamp);
            // Increased width for better readability
            ChartUtils.saveChartAsPNG(new File(fullFileName), chart, 1600, 1000);
            System.out.println("Chart saved: " + fullFileName);
        } catch (IOException e) {
            System.err.println("Error saving chart " + fileName + ": " + e.getMessage());
        }
    }
}