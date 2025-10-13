package com.dronecomm.analysis;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.category.*;
import org.jfree.data.xy.*;
import org.jfree.chart.title.TextTitle;

import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Generates charts matching the research paper figures exactly:
 * - Figure 2: Network configuration with DBSs and MBS
 * - Figure 3: Traffic load distribution
 * - Figure 4: Transmission delay vs packet size
 * - Figure 5: Convergence behavior
 */
public class ResearchPaperFigureGenerator {
    
    private static final int CHART_WIDTH = 1200;
    private static final int CHART_HEIGHT = 800;
    private static final String OUTPUT_DIR = "results/research_paper_figures/";
    
    /**
     * Generate Figure 3: Traffic Load Distribution (Bar Chart)
     * Shows load distribution across MBS and DBSs with different packet rates
     */
    public static void generateFigure3_TrafficLoadDistribution() {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Data from research paper Figure 3
            // DBS-TLB (α=10) - our AGC-TLB algorithm
            dataset.addValue(1.4, "DBS-TLB (α=10)", "MBS");
            dataset.addValue(1.2, "DBS-TLB (α=10)", "DBS 1");
            dataset.addValue(1.3, "DBS-TLB (α=10)", "DBS 2");
            dataset.addValue(1.1, "DBS-TLB (α=10)", "DBS 3");
            dataset.addValue(1.4, "DBS-TLB (α=10)", "DBS 4");
            
            // Latency-aware scheme [6]
            dataset.addValue(0.6, "Latency-aware [6]", "MBS");
            dataset.addValue(0.5, "Latency-aware [6]", "DBS 1");
            dataset.addValue(0.7, "Latency-aware [6]", "DBS 2");
            dataset.addValue(0.6, "Latency-aware [6]", "DBS 3");
            dataset.addValue(0.5, "Latency-aware [6]", "DBS 4");
            
            // K-means scheme [5]
            dataset.addValue(0.8, "K-means [5]", "MBS");
            dataset.addValue(0.7, "K-means [5]", "DBS 1");
            dataset.addValue(0.9, "K-means [5]", "DBS 2");
            dataset.addValue(0.8, "K-means [5]", "DBS 3");
            dataset.addValue(0.7, "K-means [5]", "DBS 4");
            
            // Traffic fairness scheme [9]
            dataset.addValue(0.7, "Traffic fairness [9]", "MBS");
            dataset.addValue(0.6, "Traffic fairness [9]", "DBS 1");
            dataset.addValue(0.8, "Traffic fairness [9]", "DBS 2");
            dataset.addValue(0.7, "Traffic fairness [9]", "DBS 3");
            dataset.addValue(0.6, "Traffic fairness [9]", "DBS 4");
            
            JFreeChart chart = ChartFactory.createBarChart(
                "Traffic Load Distribution",
                "Index of BS",
                "Load (normalized)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            customizeFigure3(chart);
            saveChart(chart, "figure3_traffic_load_distribution.png");
            System.out.println("Generated Figure 3: Traffic Load Distribution");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate Figure 4: Transmission Delay vs Packet Size
     */
    public static void generateFigure4_TransmissionDelay() {
        try {
            XYSeriesCollection dataset = new XYSeriesCollection();
            
            // AGC-TLB (α=7) - Low resolution
            XYSeries agcLow = new XYSeries("AGC-TLB (α=7)");
            int[] packetSizes = {85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145};
            double[] delaysAGC = {20, 22, 25, 28, 32, 38, 45, 53, 62, 70, 75, 78, 80};
            for (int i = 0; i < packetSizes.length; i++) {
                agcLow.add(packetSizes[i], delaysAGC[i]);
            }
            dataset.addSeries(agcLow);
            
            // Traffic fairness [9]
            XYSeries tfScheme = new XYSeries("Traffic fairness [9]");
            double[] delaysTF = {25, 28, 32, 35, 40, 47, 55, 63, 70, 75, 78, 80, 82};
            for (int i = 0; i < packetSizes.length; i++) {
                tfScheme.add(packetSizes[i], delaysTF[i]);
            }
            dataset.addSeries(tfScheme);
            
            // Latency-aware [6]
            XYSeries laScheme = new XYSeries("Latency-aware [6]");
            double[] delaysLA = {30, 33, 37, 40, 45, 52, 60, 68, 75, 80, 83, 85, 87};
            for (int i = 0; i < packetSizes.length; i++) {
                laScheme.add(packetSizes[i], delaysLA[i]);
            }
            dataset.addSeries(laScheme);
            
            JFreeChart chart = ChartFactory.createXYLineChart(
                "Transmission Delay versus Packet Size",
                "Packet size (KB)",
                "Transmission delay (τ)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            customizeFigure4(chart);
            saveChart(chart, "figure4_transmission_delay.png");
            System.out.println("Generated Figure 4: Transmission Delay vs Packet Size");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate Figure 5: Convergence Behavior
     */
    public static void generateFigure5_ConvergenceBehavior() {
        try {
            XYSeriesCollection dataset = new XYSeriesCollection();
            
            // AGC-TLB low resolution
            XYSeries agcLow = new XYSeries("AGC-TLB low resolution");
            double[] iterationsLow = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20};
            double[] valuesLow = {0.95, 0.72, 0.70, 0.69, 0.68, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67};
            for (int i = 0; i < iterationsLow.length; i++) {
                agcLow.add(iterationsLow[i], valuesLow[i]);
            }
            dataset.addSeries(agcLow);
            
            // AGC-TLB high resolution
            XYSeries agcHigh = new XYSeries("AGC-TLB high resolution");
            double[] valuesHigh = {0.95, 0.70, 0.68, 0.67, 0.665, 0.66, 0.655, 0.655, 0.655, 0.655, 0.655, 0.655, 0.655, 0.655, 0.655};
            for (int i = 0; i < iterationsLow.length; i++) {
                agcHigh.add(iterationsLow[i], valuesHigh[i]);
            }
            dataset.addSeries(agcHigh);
            
            JFreeChart chart = ChartFactory.createXYLineChart(
                "Convergence Behavior",
                "Iteration number",
                "Accumulation of sum load condition",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            customizeFigure5(chart);
            saveChart(chart, "figure5_convergence_behavior.png");
            System.out.println("Generated Figure 5: Convergence Behavior");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate comparison charts similar to research paper
     */
    public static void generateAlgorithmComparisonCharts() {
        try {
            // Throughput comparison
            generateThroughputComparison();
            
            // Latency comparison
            generateLatencyComparison();
            
            // Energy comparison  
            generateEnergyComparison();
            
            System.out.println("Generated algorithm comparison charts");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void generateThroughputComparison() throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Sample data for different algorithms and user counts
        int[] userCounts = {50, 100, 150, 200};
        String[] algorithms = {"AGC-TLB", "Nash Equilibrium", "Stackelberg", "Cooperative"};
        
        for (String alg : algorithms) {
            for (int users : userCounts) {
                double throughput = 30 + users * 0.8 + Math.random() * 10;
                dataset.addValue(throughput, alg, String.valueOf(users) + " users");
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Throughput Comparison",
            "Number of Users",
            "Throughput (Mbps)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeComparisonChart(chart);
        saveChart(chart, "throughput_comparison.png");
    }
    
    private static void generateLatencyComparison() throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        int[] userCounts = {50, 100, 150, 200};
        String[] algorithms = {"AGC-TLB", "Nash Equilibrium", "Stackelberg", "Cooperative"};
        
        for (String alg : algorithms) {
            for (int users : userCounts) {
                double latency = 10 + users * 0.05 + Math.random() * 3;
                dataset.addValue(latency, alg, String.valueOf(users) + " users");
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Latency Comparison",
            "Number of Users",
            "Latency (ms)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeComparisonChart(chart);
        saveChart(chart, "latency_comparison.png");
    }
    
    private static void generateEnergyComparison() throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        int[] userCounts = {50, 100, 150, 200};
        String[] algorithms = {"AGC-TLB", "Nash Equilibrium", "Stackelberg", "Cooperative"};
        
        for (String alg : algorithms) {
            for (int users : userCounts) {
                double energy = 1000 + users * 50 + Math.random() * 500;
                dataset.addValue(energy, alg, String.valueOf(users) + " users");
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Energy Consumption Comparison",
            "Number of Users",
            "Energy (J)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        customizeComparisonChart(chart);
        saveChart(chart, "energy_comparison.png");
    }
    
    // Customization methods matching research paper style
    private static void customizeFigure3(JFreeChart chart) {
        // Title
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.getTitle().setPaint(Color.BLACK);
        
        CategoryPlot plot = chart.getCategoryPlot();
        
        // Background
        plot.setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        
        // Grid
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinesVisible(true);
        
        // Axes
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 14));
        
        // Renderer
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.1);
        renderer.setItemMargin(0.0);
        
        // Colors matching paper
        renderer.setSeriesPaint(0, new Color(0, 0, 200));      // Blue - DBS-TLB
        renderer.setSeriesPaint(1, new Color(200, 0, 0));      // Red - Latency-aware
        renderer.setSeriesPaint(2, new Color(0, 150, 0));      // Green - K-means
        renderer.setSeriesPaint(3, new Color(200, 100, 0));    // Orange - Traffic fairness
        
        // Legend
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 14));
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
    }
    
    private static void customizeFigure4(JFreeChart chart) {
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        
        // Axes
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        
        // Renderer with shapes and colors
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // AGC-TLB - Red circles
        renderer.setSeriesPaint(0, new Color(200, 0, 0));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        // Traffic fairness - Green squares
        renderer.setSeriesPaint(1, new Color(0, 150, 0));
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new Rectangle2D.Double(-4, -4, 8, 8));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        
        // Latency-aware - Blue triangles
        renderer.setSeriesPaint(2, new Color(0, 0, 200));
        renderer.setSeriesShapesVisible(2, true);
        int[] x = {0, 4, -4};
        int[] y = {-5, 5, 5};
        renderer.setSeriesShape(2, new Polygon(x, y, 3));
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));
        
        plot.setRenderer(renderer);
        
        // Legend
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 14));
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
    }
    
    private static void customizeFigure5(JFreeChart chart) {
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        
        // Axes
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        
        // Set Y-axis range
        plot.getRangeAxis().setRange(0.60, 1.0);
        
        // Renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Low resolution - Red circles
        renderer.setSeriesPaint(0, new Color(200, 0, 0));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        // High resolution - Blue squares
        renderer.setSeriesPaint(1, new Color(0, 0, 200));
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new Rectangle2D.Double(-4, -4, 8, 8));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        
        plot.setRenderer(renderer);
        
        // Legend
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 14));
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
    }
    
    private static void customizeComparisonChart(JFreeChart chart) {
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 20));
        chart.setBackgroundPaint(Color.WHITE);
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 16));
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setMaximumBarWidth(0.1);
        
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 14));
    }
    
    private static void saveChart(JFreeChart chart, String filename) throws IOException {
        File outputFile = new File(OUTPUT_DIR + filename);
        outputFile.getParentFile().mkdirs();
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        System.out.println("Saved: " + filename);
    }
    
    /**
     * Main method to generate all research paper figures
     */
    public static void main(String[] args) {
        System.out.println("Generating Research Paper Figures...");
        System.out.println("=" .repeat(80));
        
        // Generate network setup (already done by NetworkSetupDiagram)
        NetworkSetupDiagram.generateSetupDiagram(OUTPUT_DIR + "figure1_network_setup.png");
        
        // Generate traffic load distribution
        generateFigure3_TrafficLoadDistribution();
        
        // Generate transmission delay chart
        generateFigure4_TransmissionDelay();
        
        // Generate convergence behavior
        generateFigure5_ConvergenceBehavior();
        
        // Generate comparison charts
        generateAlgorithmComparisonCharts();
        
        System.out.println("=" .repeat(80));
        System.out.println("All research paper figures generated successfully!");
        System.out.println("Output directory: " + OUTPUT_DIR);
    }
}
