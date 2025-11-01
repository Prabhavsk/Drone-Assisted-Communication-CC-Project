package com.dronecomm.analysis;

import com.dronecomm.enums.AlgorithmType;
import com.dronecomm.enums.ScenarioType;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.block.BlockBorder;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import com.dronecomm.utils.ConfigurationLoader;

/**
 * Includes system model, network topology, performance comparisons, and convergence analysis.
 */
public class ResearchPaperCharts {
    private static final String CHARTS_DIR = "results/research_paper_figures";
    private final String timestamp;
    
    public ResearchPaperCharts() {
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
     * Figure 1: System model showing network topology with drones, ground stations, and users.
     */
    public void generateSystemModelFigure() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        XYSeries groundStations = new XYSeries("Ground Base Stations");
        groundStations.add(1000, 1000);
        groundStations.add(4000, 1000);
        groundStations.add(1000, 4000);
        groundStations.add(4000, 4000);
        dataset.addSeries(groundStations);
        
        XYSeries droneStations = new XYSeries("Drone Base Stations");
        droneStations.add(1500, 1500);
        droneStations.add(3500, 1500);
        droneStations.add(2500, 3000);
        droneStations.add(1500, 3500);
        droneStations.add(3500, 3500);
        droneStations.add(2500, 2000);
        dataset.addSeries(droneStations);
        
        XYSeries mobileUsers = new XYSeries("Mobile Users");
        Random random = new Random(42);
        for (int i = 0; i < 20; i++) {
            mobileUsers.add(1000 + random.nextDouble() * 3000, 1000 + random.nextDouble() * 3000);
        }
        dataset.addSeries(mobileUsers);
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "AGC-TLB System Model: Drone-Assisted Communication Network",
            "X Position (m)",
            "Y Position (m)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeSystemModelChart(chart);
        saveChart(chart, "Figure1_SystemModel", 1600, 1200);
    }
    
    /**
     * Figure 2: Algorithm performance comparison across all scenarios and user counts.
     */
    public void generateAlgorithmComparisonFigure(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Focus on specific user counts for paper presentation
        int[] userCounts = {50, 100, 150, 200};
        
        for (int userCount : userCounts) {
            for (ScenarioType scenario : Arrays.asList(ScenarioType.LOW_MOBILITY, ScenarioType.HIGH_MOBILITY)) {
                if (allResults.containsKey(scenario) && allResults.get(scenario).containsKey(userCount)) {
                    String category = userCount + " Users";
                    
                    for (AlgorithmType algorithm : Arrays.asList(
                            AlgorithmType.NASH_EQUILIBRIUM, 
                            AlgorithmType.STACKELBERG_GAME,
                            AlgorithmType.COOPERATIVE_GAME,
                            AlgorithmType.AUCTION_BASED)) {
                        
                        if (allResults.get(scenario).get(userCount).containsKey(algorithm)) {
                            ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                            dataset.addValue(result.getAverageThroughput() / 1e6, 
                                           algorithm.getDisplayName(), category);
                        }
                    }
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Average Throughput Performance Comparison",
            "Number of Users",
            "Average Throughput (Mbps)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizePaperChart(chart);
        saveChart(chart, "Figure2_AlgorithmComparison", 1600, 1000);
    }
    
    /**
     * Generate Figure 3: Traffic Load Distribution
     * Bar chart showing load distribution across base stations
     */
    public void generateTrafficLoadDistributionFigure(Map<AlgorithmType, ResultsExporter.SimulationResult> scenarioResults) {
        if (scenarioResults == null || scenarioResults.isEmpty()) return;
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (AlgorithmType algorithm : scenarioResults.keySet()) {
            ResultsExporter.SimulationResult result = scenarioResults.get(algorithm);
            Map<String, Double> loads = result.getBaseStationLoads();
            if (loads != null) {
                for (Map.Entry<String, Double> entry : loads.entrySet()) {
                    dataset.addValue(entry.getValue(), algorithm.getDisplayName(), entry.getKey());
                }
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Traffic Load Distribution",
            "Base Station",
            "Load (packets/s)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        customizePaperChart(chart);
        saveChart(chart, "Figure3_TrafficLoadDistribution", 1600, 1000);
    }

    /**
     * Generate Figure 4: Energy Efficiency Analysis
     * Shows energy consumption vs throughput trade-offs with proper paper styling
     */
    public void generateEnergyEfficiencyFigure(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        for (AlgorithmType algorithm : Arrays.asList(
                AlgorithmType.NASH_EQUILIBRIUM, 
                AlgorithmType.STACKELBERG_GAME,
                AlgorithmType.COOPERATIVE_GAME,
                AlgorithmType.AUCTION_BASED)) {
            
            XYSeries series = new XYSeries(algorithm.getDisplayName());
            
            for (ScenarioType scenario : allResults.keySet()) {
                for (Integer userCount : allResults.get(scenario).keySet()) {
                    if (allResults.get(scenario).get(userCount).containsKey(algorithm)) {
                        ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                        double throughput = result.getAverageThroughput() / 1e6;
                        double energy = result.getTotalEnergyConsumption() / 1e6; // Convert to MJ
                        series.add(energy, throughput);
                    }
                }
            }
            dataset.addSeries(series);
        }
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Energy Efficiency Analysis: Throughput vs Energy Consumption",
            "Energy Consumption (MJ)",
            "Average Throughput (Mbps)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        // Enhanced styling for paper quality
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultLinesVisible(false);
        renderer.setDefaultShapesVisible(true);
        
        // Paper colors for algorithms - distinct and vibrant
        Color[] colors = {
            new Color(31, 119, 180),    // Blue - Nash
            new Color(255, 127, 14),    // Orange - Stackelberg
            new Color(44, 160, 44),     // Green - Cooperative
            new Color(214, 39, 40)      // Red - Auction
        };
        
        Shape[] shapes = {
            new Ellipse2D.Double(-5, -5, 10, 10),                               // Circle
            new Rectangle(-5, -5, 10, 10),                                      // Square
            createTriangle(6),                                                   // Triangle
            createDiamond(5)                                                     // Diamond
        };
        
        for (int i = 0; i < 4; i++) {
            renderer.setSeriesPaint(i, colors[i]);
            renderer.setSeriesShape(i, shapes[i]);
            renderer.setSeriesOutlineStroke(i, new BasicStroke(1.5f));
            renderer.setSeriesOutlinePaint(i, colors[i].darker());
        }
        
        // Axis styling
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        
        // Title styling
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(Color.BLACK);
        
        // Legend styling - position at bottom
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setPosition(RectangleEdge.BOTTOM);
            legend.setItemFont(new Font("Arial", Font.PLAIN, 11));
            legend.setFrame(BlockBorder.NONE);
            legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
        }
        
        saveChart(chart, "Figure4_EnergyEfficiency", 1200, 900);
    }
    
    /**
     * Generate Figure 5: Transmission Delay
     * Line chart showing delay patterns across algorithms with proper paper styling
     */
    public void generateTransmissionDelayFigure(Map<AlgorithmType, ResultsExporter.SimulationResult> scenarioResults) {
        if (scenarioResults == null || scenarioResults.isEmpty()) return;
        
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Simulated delay data based on algorithm performance
        int[] packetSizes = {10, 50, 100, 200, 300, 500, 750, 1000};
        
        for (AlgorithmType algorithm : Arrays.asList(
                AlgorithmType.NASH_EQUILIBRIUM,
                AlgorithmType.STACKELBERG_GAME,
                AlgorithmType.COOPERATIVE_GAME,
                AlgorithmType.AUCTION_BASED)) {
            
            if (!scenarioResults.containsKey(algorithm)) continue;
            
            XYSeries series = new XYSeries(algorithm.getDisplayName());
            ResultsExporter.SimulationResult result = scenarioResults.get(algorithm);
            
            // Use average latency from result to scale simulated delay
            double avgLatency = result.getAverageLatency();
            
            for (int packetSize : packetSizes) {
                // Realistic delay = base_latency + (packet_size / bandwidth)
                // Simulate transmission delay based on packet size with realistic values
                double transmissionTime = (packetSize * 8.0) / 4.0;  // ~4 Mbps bandwidth
                double totalDelay = avgLatency + transmissionTime;
                
                series.add(packetSize, totalDelay);
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Transmission Delay vs Packet Size",
            "Packet Size (KB)",
            "Delay (ms)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        // Enhanced styling for paper quality
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultLinesVisible(true);
        renderer.setDefaultShapesVisible(true);
        
        // Paper colors and line styles
        Color[] colors = {
            new Color(31, 119, 180),    // Blue
            new Color(255, 127, 14),    // Orange
            new Color(44, 160, 44),     // Green
            new Color(214, 39, 40)      // Red
        };
        
        Shape[] shapes = {
            new Ellipse2D.Double(-4, -4, 8, 8),
            new Rectangle(-4, -4, 8, 8),
            createTriangle(5),
            createDiamond(4)
        };
        
        for (int i = 0; i < Math.min(4, dataset.getSeriesCount()); i++) {
            renderer.setSeriesPaint(i, colors[i]);
            renderer.setSeriesStroke(i, new BasicStroke(2.2f));
            renderer.setSeriesShape(i, shapes[i]);
            renderer.setSeriesShapesVisible(i, true);
        }
        
        // Axis styling
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        
        // Title styling
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(Color.BLACK);
        
        // Legend styling
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setPosition(RectangleEdge.BOTTOM);
            legend.setItemFont(new Font("Arial", Font.PLAIN, 11));
            legend.setFrame(BlockBorder.NONE);
            legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
        }
        
        saveChart(chart, "Figure5_TransmissionDelay", 1200, 900);
    }

    
    /**
     * Generate Table 1: Simulation Parameters
     */
    public void generateSimulationParametersTable() {
        StringBuilder table = new StringBuilder();
        table.append("Table 1: Simulation Parameters\n");
        table.append("=".repeat(60)).append("\n");
        table.append(String.format("%-30s | %-25s\n", "Parameter", "Value"));
        table.append("-".repeat(60)).append("\n");
        table.append(String.format("%-30s | %-25s\n", "Simulation Area", "5000m * 5000m"));
        table.append(String.format("%-30s | %-25s\n", "Number of Ground BS", "4"));
        table.append(String.format("%-30s | %-25s\n", "Number of Drone BS", "6"));
        table.append(String.format("%-30s | %-25s\n", "Number of Users", "50, 100, 150, 200"));
        table.append(String.format("%-30s | %-25s\n", "Drone Height", "50-300 m"));
        table.append(String.format("%-30s | %-25s\n", "Carrier Frequency", "2 GHz"));
        table.append(String.format("%-30s | %-25s\n", "Bandwidth", "20 MHz"));
        table.append(String.format("%-30s | %-25s\n", "UE Transmission Power", "20 mW"));
        table.append(String.format("%-30s | %-25s\n", "DBS Transmission Power", "1 W"));
        table.append(String.format("%-30s | %-25s\n", "Noise Power Density", "-174 dBm/Hz"));
        table.append(String.format("%-30s | %-25s\n", "alpha-Fairness Parameter", "0.5, 1.0, 2.0"));
        table.append(String.format("%-30s | %-25s\n", "Simulation Time", "100 seconds"));
        table.append(String.format("%-30s | %-25s\n", "Number of Runs", "50"));
        table.append("=".repeat(60)).append("\n");
        
        saveTable(table.toString(), "Table1_SimulationParameters");
    }
    
    /**
     * Generate Table 2: Algorithm Performance Summary
     */
    public void generatePerformanceSummaryTable(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        StringBuilder table = new StringBuilder();
        table.append("Table 2: Algorithm Performance Summary\n");
        table.append("=".repeat(100)).append("\n");
        table.append(String.format("%-20s | %-12s | %-12s | %-12s | %-12s\n", 
                     "Algorithm", "Throughput", "Latency", "Energy", "Satisfaction"));
        table.append(String.format("%-20s | %-12s | %-12s | %-12s | %-12s\n", 
                     "", "(Mbps)", "(ms)", "(MJ)", "(%)"));
        table.append("-".repeat(85)).append("\n");
        
        for (AlgorithmType algorithm : AlgorithmType.values()) {
            if (algorithm.isGameTheoretic()) {
                double avgThroughput = 0, avgLatency = 0, avgEnergy = 0, avgSat = 0;
                int count = 0;
                
                for (ScenarioType scenario : allResults.keySet()) {
                    for (Integer userCount : allResults.get(scenario).keySet()) {
                        if (allResults.get(scenario).get(userCount).containsKey(algorithm)) {
                            ResultsExporter.SimulationResult result = allResults.get(scenario).get(userCount).get(algorithm);
                            avgThroughput += result.getAverageThroughput() / 1e6;
                            avgLatency += result.getAverageLatency();
                            avgEnergy += result.getTotalEnergyConsumption() / 1e6;
                            avgSat += result.getUserSatisfaction() * 100;
                            count++;
                        }
                    }
                }
                
                if (count > 0) {
                    table.append(String.format("%-20s | %12.2f | %12.2f | %12.2f | %12.2f\n",
                               algorithm.getDisplayName(),
                               avgThroughput / count,
                               avgLatency / count,
                               avgEnergy / count,
                               avgSat / count));
                }
            }
        }
        
        saveTable(table.toString(), "Table2_PerformanceSummary");
    }
    
    
    // Chart customization methods
    private void customizeSystemModelChart(JFreeChart chart) {
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        
        // Ground stations - squares (brown)
        renderer.setSeriesShape(0, new Rectangle(-7, -7, 14, 14));
        renderer.setSeriesPaint(0, new Color(139, 69, 19));
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesFilled(0, true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesOutlinePaint(0, Color.BLACK);
        
        // Drone stations - triangles (blue)
        Polygon triangle = new Polygon();
        triangle.addPoint(0, -9);
        triangle.addPoint(-7, 7);
        triangle.addPoint(7, 7);
        renderer.setSeriesShape(1, triangle);
        renderer.setSeriesPaint(1, new Color(0, 100, 200));
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesFilled(1, true);
        renderer.setSeriesOutlineStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesOutlinePaint(1, Color.BLACK);
        
        // Mobile users - circles (red)
        renderer.setSeriesShape(2, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesPaint(2, new Color(200, 0, 0));
        renderer.setSeriesLinesVisible(2, false);
        renderer.setSeriesShapesFilled(2, true);
        renderer.setSeriesOutlineStroke(2, new BasicStroke(1.5f));
        renderer.setSeriesOutlinePaint(2, Color.BLACK);
        
        // Enhanced fonts
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.getTitle().setPaint(Color.BLACK);
        
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        
        // Legend styling
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Arial", Font.BOLD, 14));
            chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
            chart.getLegend().setBackgroundPaint(new Color(255, 255, 255, 200));
        }
    }
    
    private void customizePaperChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        
        // Research paper colors - more vibrant and distinctive
        Color[] colors = {
            new Color(31, 119, 180),   // Blue - Nash
            new Color(255, 127, 14),   // Orange - Stackelberg
            new Color(44, 160, 44),    // Green - Cooperative
            new Color(214, 39, 40)     // Red - Auction
        };
        
        for (int i = 0; i < colors.length; i++) {
            renderer.setSeriesPaint(i, colors[i]);
        }
        
        // Enhanced bar styling
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(true);
        renderer.setMaximumBarWidth(0.15);
        
        // Enhanced fonts
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.getTitle().setPaint(Color.BLACK);
        
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        
        // Grid styling
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        
        // Legend styling
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 13));
            chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
        }
    }
    

    
    private void customizeEnergyChart(JFreeChart chart) {
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultLinesVisible(false);
        renderer.setDefaultShapesVisible(true);
        
        Color[] colors = {
            new Color(31, 119, 180),   // Blue
            new Color(255, 127, 14),   // Orange  
            new Color(44, 160, 44),    // Green
            new Color(214, 39, 40)     // Red
        };
        
        // Enhanced shapes for different series
        Shape[] shapes = {
            new Ellipse2D.Double(-5, -5, 10, 10),  // Large circle
            new Rectangle(-5, -5, 10, 10),          // Large square
            new Polygon(new int[]{0, -5, 5}, new int[]{-6, 5, 5}, 3),  // Triangle
            createDiamond(5)  // Diamond
        };
        
        for (int i = 0; i < colors.length; i++) {
            renderer.setSeriesPaint(i, colors[i]);
            renderer.setSeriesShape(i, shapes[i % shapes.length]);
            renderer.setSeriesOutlineStroke(i, new BasicStroke(1.5f));
            renderer.setSeriesOutlinePaint(i, colors[i].darker());
        }
        
        // Enhanced fonts and axes
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.getTitle().setPaint(Color.BLACK);
        
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 13));
        
        // Legend styling
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 13));
            chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
        }
    }
    
    // Helper method to create diamond shape
    private Shape createDiamond(int size) {
        Polygon diamond = new Polygon();
        diamond.addPoint(0, -size);
        diamond.addPoint(size, 0);
        diamond.addPoint(0, size);
        diamond.addPoint(-size, 0);
        return diamond;
    }
    
    private void saveChart(JFreeChart chart, String fileName, int width, int height) {
        try {
            String fullFileName = String.format("%s/%s_%s.png", CHARTS_DIR, fileName, timestamp);
            ChartUtils.saveChartAsPNG(new File(fullFileName), chart, width, height);
            System.out.println("Research paper chart saved: " + fullFileName);
        } catch (IOException e) {
            System.err.println("Error saving chart " + fileName + ": " + e.getMessage());
        }
    }
    
    private void saveTable(String tableContent, String fileName) {
        try {
            String fullFileName = String.format("%s/%s_%s.txt", CHARTS_DIR, fileName, timestamp);
            java.nio.file.Files.write(java.nio.file.Paths.get(fullFileName), tableContent.getBytes());
            System.out.println("Research paper table saved: " + fullFileName);
        } catch (IOException e) {
            System.err.println("Error saving table " + fileName + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate Figure 2: Network Configuration Topology with Assignment Lines
     * Shows algorithm comparison with connection lines showing user-BS assignments.
     * Generates one topology figure per algorithm in the provided resultsMap (no hardcoded names/matching).
     */
    public void generateNetworkTopologyFigure(Map<com.dronecomm.enums.AlgorithmType, ResultsExporter.SimulationResult> resultsMap) {
        if (resultsMap == null || resultsMap.isEmpty()) {
            System.out.println("Skipping topology figures — no simulation results provided.");
            return;
        }

        // Generate one topology figure per algorithm present in the resultsMap.
        // Use the algorithm's real display name and real SimulationResult data (no synthetic matching).
        int algoIdx = 0;
        for (java.util.Map.Entry<com.dronecomm.enums.AlgorithmType, ResultsExporter.SimulationResult> entry : resultsMap.entrySet()) {
            com.dronecomm.enums.AlgorithmType algorithm = entry.getKey();
            ResultsExporter.SimulationResult result = entry.getValue();
            String algorithmName = algorithm.getDisplayName();

            if (result == null) {
                System.out.println("Skipping topology figure for '" + algorithmName + "' — simulation result is null.");
                continue;
            }

            // Get real positions and assignments from the matched result
            List<double[]> userPositions = result.getUserPositions();
            List<double[]> dronePositions = result.getDronePositions();
            List<double[]> groundPositions = result.getGroundPositions();
            Map<String, List<Integer>> assignments = result.getAssignments();

            // Verify all users are assigned
            if (assignments != null && !assignments.isEmpty() && userPositions != null) {
                Set<Integer> assignedUsers = new HashSet<>();
                for (List<Integer> users : assignments.values()) {
                    if (users != null) {
                        assignedUsers.addAll(users);
                    }
                }
                
                // Report if any users are missing
                if (assignedUsers.size() < userPositions.size()) {
                    List<Integer> missingUsers = new ArrayList<>();
                    for (int i = 0; i < userPositions.size(); i++) {
                        if (!assignedUsers.contains(i)) {
                            missingUsers.add(i);
                        }
                    }
                    System.out.println("WARNING [" + algorithmName + "]: " + missingUsers.size() + 
                        " users not assigned: " + missingUsers);
                }
            }

            // Require real users and assignments and at least one base-station position; otherwise skip
            if (userPositions == null || userPositions.isEmpty() || assignments == null || assignments.isEmpty() ||
                ((dronePositions == null || dronePositions.isEmpty()) && (groundPositions == null || groundPositions.isEmpty()))) {
                System.out.println("Skipping topology figure for '" + algorithmName + "' — missing required positions or assignments in simulation result.");
                continue;
            }

            XYSeriesCollection dataset = new XYSeriesCollection();

            // Add DBS positions (drone base stations)
            XYSeries dbsSeries = new XYSeries("DBS");
            List<double[]> effectiveDronePos = new ArrayList<>();
            if (dronePositions != null && !dronePositions.isEmpty()) {
                for (double[] pos : dronePositions) {
                    dbsSeries.add(pos[0], pos[1]);
                    effectiveDronePos.add(pos);
                }
            }
            dataset.addSeries(dbsSeries);
            
            // Add MBS positions (ground base stations)
            XYSeries mbsSeries = new XYSeries("MBS");
            List<double[]> effectiveGroundPos = new ArrayList<>();
            if (groundPositions != null && !groundPositions.isEmpty()) {
                for (double[] pos : groundPositions) {
                    mbsSeries.add(pos[0], pos[1]);
                    effectiveGroundPos.add(pos);
                }
            }
            dataset.addSeries(mbsSeries);
            
            // Add UEs with different packet rates (colored by rate)
            XYSeries uesHigh = new XYSeries("UE (1.5 pkt/s)");
            XYSeries uesMedium = new XYSeries("UE (1.0 pkt/s)");
            XYSeries uesLow = new XYSeries("UE (0.5 pkt/s)");
            
            List<double[]> effectiveUserPos = new ArrayList<>();
            // Use REAL user positions
            int count = 0;
            for (double[] pos : userPositions) {
                effectiveUserPos.add(pos);
                // Distribute users across 3 rate categories evenly
                if (count % 3 == 0) {
                    uesHigh.add(pos[0], pos[1]);
                } else if (count % 3 == 1) {
                    uesMedium.add(pos[0], pos[1]);
                } else {
                    uesLow.add(pos[0], pos[1]);
                }
                count++;
            }
            
            dataset.addSeries(uesHigh);
            dataset.addSeries(uesMedium);
            dataset.addSeries(uesLow);
            
            // Create the chart with algorithm-specific title
            
            String chartTitle = "Network Topology: " + algorithmName;
            JFreeChart chart = ChartFactory.createScatterPlot(
                chartTitle,
                "X axis (m)",
                "Y axis (m)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            XYPlot plot = chart.getXYPlot();
            
            // Use REAL assignments from the SimulationResult only. Do NOT apply
            // visualization-only reassignment or synthetic fallbacks here: figures
            // should reflect exported simulation data. If a user is unassigned in
            // the SimulationResult, it will remain unconnected in the plot.
            drawAssignmentLines(plot, assignments, effectiveUserPos, effectiveDronePos, effectiveGroundPos);
            
            // Customize renderer for points - MATCH PAPER FIGURE STYLE EXACTLY
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
            
            // DBS - filled blue triangles (matching paper Figure 2)
            renderer.setSeriesPaint(0, new Color(0, 0, 255));
            renderer.setSeriesShape(0, createTriangle(10));  // Size 10 for visibility
            renderer.setSeriesShapesFilled(0, true);
            renderer.setSeriesOutlinePaint(0, Color.BLACK);
            renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
            
            // MBS - larger filled blue triangles (matching paper Figure 2)
            renderer.setSeriesPaint(1, new Color(0, 0, 255));
            renderer.setSeriesShape(1, createTriangle(14));  // Larger for MBS
            renderer.setSeriesShapesFilled(1, true);
            renderer.setSeriesOutlinePaint(1, Color.BLACK);
            renderer.setSeriesOutlineStroke(1, new BasicStroke(1.5f));
            
            // UEs - filled circles colored by packet rate (matching paper Figure 2 colors exactly)
            renderer.setSeriesPaint(2, new Color(255, 0, 0));      // Red - 1.5 pkt/s
            renderer.setSeriesPaint(3, new Color(255, 200, 0));    // Yellow/Orange - 1.0 pkt/s
            renderer.setSeriesPaint(4, new Color(0, 180, 0));      // Green - 0.5 pkt/s
            
            for (int s = 2; s <= 4; s++) {
                renderer.setSeriesShape(s, new Ellipse2D.Double(-4, -4, 8, 8));  // Filled circles
                renderer.setSeriesShapesFilled(s, true);
                renderer.setSeriesShapesVisible(s, true);
            }
            
            plot.setRenderer(0, renderer);  // Explicitly set renderer 0 for dataset 0
            
            // White background with light gray dotted grid (matching paper style)
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(new Color(200, 200, 200));
            plot.setRangeGridlinePaint(new Color(200, 200, 200));
            plot.setDomainGridlineStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
            plot.setRangeGridlineStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
            
            // Border around plot
            plot.setOutlineVisible(true);
            plot.setOutlinePaint(Color.BLACK);
            plot.setOutlineStroke(new BasicStroke(1.0f));
            
            // Set axis ranges to a consistent research-paper area to avoid
            // misleading zooming when scenarios use larger areas (e.g., extended scenarios)
            // Read the simulation area from configuration instead of hardcoding.
            ConfigurationLoader cfg = new ConfigurationLoader();
            final double PAPER_AREA_MAX = cfg.getSimulationArea();

            // Compute dynamic max but clamp to PAPER_AREA_MAX
            double maxX = 1000, maxY = 1000;
            if (dronePositions != null && !dronePositions.isEmpty()) {
                for (double[] pos : dronePositions) {
                    maxX = Math.max(maxX, pos[0]);
                    maxY = Math.max(maxY, pos[1]);
                }
            }
            if (userPositions != null && !userPositions.isEmpty()) {
                for (double[] pos : userPositions) {
                    maxX = Math.max(maxX, pos[0]);
                    maxY = Math.max(maxY, pos[1]);
                }
            }

            maxX = Math.min(maxX * 1.1, PAPER_AREA_MAX);
            maxY = Math.min(maxY * 1.1, PAPER_AREA_MAX);
            plot.getDomainAxis().setRange(0, Math.max(1000, maxX));
            plot.getRangeAxis().setRange(0, Math.max(1000, maxY));

            // Configure axes to match paper style - clean numeric labels at regular intervals
            NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            
            domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
            domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
            domainAxis.setAutoRangeIncludesZero(true);
            
            rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
            rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
            rangeAxis.setAutoRangeIncludesZero(true);

            // Title styling to match paper
            chart.getTitle().setFont(new Font("Arial", Font.BOLD, 12));
            chart.getTitle().setPaint(Color.BLACK);
            
            // Legend positioning and styling - match paper (bottom, horizontal layout)
            LegendTitle legend = chart.getLegend();
            if (legend != null) {
                legend.setPosition(RectangleEdge.BOTTOM);
                legend.setItemFont(new Font("Arial", Font.PLAIN, 9));
                legend.setFrame(BlockBorder.NONE);  // No border around legend
                legend.setBackgroundPaint(Color.WHITE);
                legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
            }
            
            // Use algorithm name in filename (sanitized) to make figures easily identifiable.
            // Use letter suffix (a,b,c...) for compatibility with paper Figure 2 naming.
            String sanitizedName = algorithmName.replaceAll("[^a-zA-Z0-9_-]", "_");
            saveChart(chart, "Figure2_NetworkTopology_" + (char)('a' + algoIdx) + "_" + sanitizedName, 800, 600);
            algoIdx++;
        }
        
        System.out.println("Research paper chart saved: Figure2 - Network topology figures for " + algoIdx + " algorithms");
    }

    /**
     * Visualization-only: ensure every BS has at least one assigned UE by moving
     * a nearest user from a BS that currently has >1 users. This keeps figures
     * visually consistent with paper schematics where each BS is shown serving
     * some users.
     * NOTE: This method is not currently used as SimulationResult data should reflect
     * actual assignments without visualization adjustments.
     */
    private void ensureEveryBSHasUserDeprecated(Map<String, List<Integer>> assignments,
                                      List<double[]> userPos,
                                      List<double[]> dronePos,
                                      List<double[]> groundPos) {
        if (assignments == null || assignments.isEmpty() || userPos == null || userPos.isEmpty()) return;

        // Build list of BS names and positions
        java.util.List<String> bsNames = new java.util.ArrayList<>();
        java.util.List<double[]> bsPositions = new java.util.ArrayList<>();
        if (dronePos != null) {
            for (int i = 0; i < dronePos.size(); i++) {
                bsNames.add("DBS-" + (i + 1));
                bsPositions.add(dronePos.get(i));
            }
        }
        if (groundPos != null) {
            for (int i = 0; i < groundPos.size(); i++) {
                bsNames.add("GBS-" + (i + 1));
                bsPositions.add(groundPos.get(i));
            }
        }
        if (bsNames.isEmpty()) return;

        // Find BS with zero assignments
        List<String> zeroBs = new java.util.ArrayList<>();
        for (String bs : bsNames) {
            List<Integer> list = assignments.get(bs);
            if (list == null || list.isEmpty()) zeroBs.add(bs);
        }

        if (zeroBs.isEmpty()) return; // nothing to do

        // Build a list of donor BS (with >1 users)
        java.util.List<String> donorBs = new java.util.ArrayList<>();
        for (String bs : assignments.keySet()) {
            List<Integer> list = assignments.get(bs);
            if (list != null && list.size() > 1) donorBs.add(bs);
        }

        if (donorBs.isEmpty()) return; // no donors available

        // For each zero-BS, find nearest user overall and try to reassign from a donor
        for (String targetBs : zeroBs) {
            double[] targetPos = findBsPosition(targetBs, bsNames, bsPositions);
            if (targetPos == null) continue;

            // Find the nearest user index to the target BS
            int bestUser = -1;
            double bestDist = Double.MAX_VALUE;
            for (int u = 0; u < userPos.size(); u++) {
                double[] up = userPos.get(u);
                double d = Math.hypot(up[0] - targetPos[0], up[1] - targetPos[1]);
                if (d < bestDist) { bestDist = d; bestUser = u; }
            }

            if (bestUser < 0) continue;

            // Find a donor BS that currently contains bestUser and has >1 users
            String donorFound = null;
            for (String donor : donorBs) {
                List<Integer> dl = assignments.get(donor);
                if (dl != null && dl.contains(bestUser) && dl.size() > 1) {
                    donorFound = donor; break;
                }
            }

            // If not found, try any donor and move its closest user to target
            if (donorFound == null) {
                // choose donor with largest load
                int maxSize = 0; String chosen = null;
                for (String donor : donorBs) {
                    List<Integer> dl = assignments.get(donor);
                    if (dl != null && dl.size() > maxSize) { maxSize = dl.size(); chosen = donor; }
                }
                donorFound = chosen;
            }

            if (donorFound == null) continue;

            // Move the user from donorFound to targetBs
            List<Integer> donorList = assignments.get(donorFound);
            if (donorList != null && donorList.remove(Integer.valueOf(bestUser))) {
                assignments.computeIfAbsent(targetBs, k -> new java.util.ArrayList<>()).add(bestUser);
            }
        }
    }

    private double[] findBsPosition(String bsName, java.util.List<String> bsNames, java.util.List<double[]> bsPositions) {
        if (bsNames == null || bsPositions == null) return null;
        for (int i = 0; i < bsNames.size(); i++) {
            if (bsNames.get(i).equals(bsName)) return bsPositions.get(i);
        }
        return null;
    }
    
    /**
     * Draw lines connecting users to their assigned base stations
     */
    private void drawAssignmentLines(XYPlot plot, Map<String, List<Integer>> assignments,
                                     List<double[]> userPos, List<double[]> dronePos, List<double[]> groundPos) {
        
        // Create a custom renderer for drawing connection lines
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        
        XYSeriesCollection lineDataset = new XYSeriesCollection();
        int seriesIdx = 0;
        int skippedConnections = 0;
        
        // For each base station and its assigned users
        for (Map.Entry<String, List<Integer>> entry : assignments.entrySet()) {
            String bsName = entry.getKey();
            List<Integer> assignedUsers = entry.getValue();
            
            if (assignedUsers == null || assignedUsers.isEmpty()) continue;
            
            // Find the base station position
            double[] bsPos = null;
            boolean isDrone = bsName.startsWith("DBS");
            
            if (isDrone) {
                // Extract drone index from name (e.g., "DBS-1" -> index 0)
                try {
                    int droneIdx = Integer.parseInt(bsName.substring(bsName.lastIndexOf('-') + 1)) - 1;
                    if (droneIdx >= 0 && droneIdx < dronePos.size()) {
                        bsPos = dronePos.get(droneIdx);
                    }
                } catch (Exception e) {
                    System.out.println("Warning: could not parse drone index from '" + bsName + "'");
                }
            } else {
                // Ground station: parse index from name 'GBS-<n>' to find correct groundPos
                if (groundPos != null && !groundPos.isEmpty()) {
                    try {
                        int groundIdx = Integer.parseInt(bsName.substring(bsName.lastIndexOf('-') + 1)) - 1;
                        if (groundIdx >= 0 && groundIdx < groundPos.size()) {
                            bsPos = groundPos.get(groundIdx);
                        }
                    } catch (Exception e) {
                        System.out.println("Warning: could not parse ground station index from '" + bsName + "'");
                    }
                }
            }
            
            if (bsPos == null) {
                skippedConnections += assignedUsers.size();
                continue;
            }
            
            // Draw a line from BS to each assigned user
            for (Integer userIdx : assignedUsers) {
                if (userIdx < 0 || userIdx >= userPos.size()) {
                    skippedConnections++;
                    continue;
                }
                
                double[] uPos = userPos.get(userIdx);
                
                // Create line series with autoSort=false to prevent legend issues
                XYSeries lineSeries = new XYSeries("Connection_" + seriesIdx, false, false);
                lineSeries.add(bsPos[0], bsPos[1]);  // Base station position
                lineSeries.add(uPos[0], uPos[1]);    // User position
                lineDataset.addSeries(lineSeries);
                
                // Set line appearance - VERY VISIBLE thick dark lines
                lineRenderer.setSeriesPaint(seriesIdx, new Color(50, 50, 50));  // Almost black, fully opaque
                lineRenderer.setSeriesStroke(seriesIdx, new BasicStroke(2.0f));  // Thicker lines
                lineRenderer.setSeriesVisibleInLegend(seriesIdx, false); // HIDE FROM LEGEND!
                
                seriesIdx++;
            }
        }
        
        // Report summary
        if (skippedConnections > 0) {
            System.out.println("WARNING: Skipped " + skippedConnections + " connections due to missing positions or invalid indices");
        }
        
        // Add the line dataset to the plot as a secondary dataset (dataset index 1)
        if (lineDataset.getSeriesCount() > 0) {
            plot.setDataset(1, lineDataset);
            plot.setRenderer(1, lineRenderer);
        }
    }
    
    /**
     * Helper method to create triangle shape for base stations
     */
    private Shape createTriangle(int size) {
        int[] xPoints = {0, size, -size};
        int[] yPoints = {-size, size, size};
        return new Polygon(xPoints, yPoints, 3);
    }
    
    
    
    /**
     * Generate Table 1: Load Metric versus Different α
     * Shows load metrics for α-fairness with α = 0, 1, 2, 10
     * Based on actual simulation data from base station loads
     */
    public void generateLoadMetricTable(ResultsExporter.SimulationResult nashResult) {
        try {
            String filename = CHARTS_DIR + "/Table1_LoadMetricVsAlpha_" + timestamp + ".txt";
            java.io.FileWriter writer = new java.io.FileWriter(filename);
            
            writer.write("TABLE 1\n");
            writer.write("LOAD METRIC VERSUS DIFFERENT α\n");
            writer.write("=" .repeat(70) + "\n\n");
            writer.write(String.format("%-35s | α=0    | α=1    | α=2    | α=10\n", "Metric"));
            writer.write("-".repeat(75) + "\n");
            
            // Get real alpha metrics from simulation result
            Map<Double, Map<String, Double>> alphaMetrics = null;
            if (nashResult != null) {
                alphaMetrics = nashResult.getAlphaMetrics();
            }
            
            if (alphaMetrics == null || alphaMetrics.isEmpty()) {
                System.out.println("Table 1: No alpha metrics available in simulation result.");
                writer.write("No simulation data available\n");
                writer.close();
                return;
            }
            
            // The key names from DetailedDataCollector
            String[] metricNames = {
                "Σ ρ_j (sum of loads)",
                "-Σ log(1 - ρ_j) (log fairness)",
                "Σ ρ_j/(1-ρ_j) (ratio sum)",
                "max{ρ_j} (max load)"
            };
            
            String[] metricKeys = {"sum_loads", "neg_log_sum", "ratio_sum", "max_load"};
            double[] alphas = {0.0, 1.0, 2.0, 10.0};
            
            for (int i = 0; i < metricNames.length; i++) {
                writer.write(String.format("%-35s |", metricNames[i]));
                for (double alpha : alphas) {
                    Map<String, Double> metrics = alphaMetrics.get(alpha);
                    if (metrics != null && metrics.containsKey(metricKeys[i])) {
                        double value = metrics.get(metricKeys[i]);
                        writer.write(String.format(" %6.2f |", value));
                    } else {
                        writer.write("  N/A  |");
                    }
                }
                writer.write("\n");
            }
            
            writer.write("=".repeat(75) + "\n");
            writer.write("Note: ρ_j represents the traffic load (normalized to 0-1) on base station j\n");
            writer.write("      α is the fairness parameter in α-fairness optimization\n");
            writer.write("      Higher α values emphasize fairness over efficiency\n");
            
            writer.close();
            System.out.println("Research paper table saved: " + filename);
        } catch (IOException e) {
            System.err.println("Error generating Table 1: " + e.getMessage());
        }
    }
    
    
    /**
     * Generate all research paper figures and tables
     */
    public void generateAllResearchPaperOutputs(Map<ScenarioType, Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>>> allResults) {
        System.out.println("Generating research paper figures and tables...");
        
        // Extract Nash Equilibrium result for Table 1 and any result for topology
        ResultsExporter.SimulationResult nashResult = null;
        ResultsExporter.SimulationResult anyResult = null;
        Map<AlgorithmType, ResultsExporter.SimulationResult> scenarioResults = null;
        
        for (Map<Integer, Map<AlgorithmType, ResultsExporter.SimulationResult>> scenarioMap : allResults.values()) {
            for (Map<AlgorithmType, ResultsExporter.SimulationResult> userCountMap : scenarioMap.values()) {
                if (userCountMap.containsKey(AlgorithmType.NASH_EQUILIBRIUM)) {
                    nashResult = userCountMap.get(AlgorithmType.NASH_EQUILIBRIUM);
                }
                if (anyResult == null && !userCountMap.isEmpty()) {
                    anyResult = userCountMap.values().iterator().next();
                }
                if (scenarioResults == null && !userCountMap.isEmpty()) {
                    scenarioResults = userCountMap;
                }
                if (nashResult != null && anyResult != null && scenarioResults != null) break;
            }
            if (nashResult != null && anyResult != null && scenarioResults != null) break;
        }
        
        generateSystemModelFigure();                                     // Figure 1
        generateNetworkTopologyFigure(scenarioResults);                  // Figure 2 (3 algorithm variants)
        if (nashResult != null) {
            generateLoadMetricTable(nashResult);                         // Table 1 with real alpha metrics
        }
        generateTrafficLoadDistributionFigure(scenarioResults);          // Figure 3 with real loads!
        generateEnergyEfficiencyFigure(allResults);                      // Figure 4: Energy Efficiency 
        generateTransmissionDelayFigure(scenarioResults);                  // Figure 6: Convergence Behavior
        generateAlgorithmComparisonFigure(allResults);                   // Convergence Analysis
        generateSimulationParametersTable();                             // Simulation Parameters Table
        generatePerformanceSummaryTable(allResults);                     // Table 2
        
        System.out.println("Research paper outputs generation complete!");
    }
}