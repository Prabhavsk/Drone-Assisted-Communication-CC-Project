package com.dronecomm.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

/**
 * Generates a visual diagram of the network setup showing:
 * - Ground Base Stations (GBS)
 * - Drone Base Stations (DBS) 
 * - Mobile Users
 * - Coverage areas
 */
public class NetworkSetupDiagram {
    
    private static final int CHART_WIDTH = 1600;
    private static final int CHART_HEIGHT = 1200;
    
    /**
     * Generates a network topology diagram showing the system setup
     */
    public static void generateSetupDiagram(String outputPath) {
        try {
            // Create dataset
            XYSeriesCollection dataset = new XYSeriesCollection();
            
            // Ground Base Stations (3 stations in strategic locations)
            XYSeries groundStations = new XYSeries("Ground BS");
            groundStations.add(200, 200);  // GBS 1
            groundStations.add(800, 200);  // GBS 2
            groundStations.add(500, 700);  // GBS 3
            dataset.addSeries(groundStations);
            
            // Drone Base Stations (5 drones at elevated positions)
            XYSeries droneStations = new XYSeries("Drone BS");
            droneStations.add(300, 400);  // DBS 1
            droneStations.add(600, 300);  // DBS 2
            droneStations.add(450, 550);  // DBS 3
            droneStations.add(700, 500);  // DBS 4
            droneStations.add(350, 650);  // DBS 5
            dataset.addSeries(droneStations);
            
            // Mobile Users (sample distribution showing hotspot areas)
            XYSeries mobileUsers = new XYSeries("Mobile Users");
            // Hotspot 1 (around center)
            for (int i = 0; i < 15; i++) {
                mobileUsers.add(400 + Math.random() * 200, 400 + Math.random() * 200);
            }
            // Hotspot 2 (upper right)
            for (int i = 0; i < 10; i++) {
                mobileUsers.add(650 + Math.random() * 150, 250 + Math.random() * 150);
            }
            // Scattered users
            for (int i = 0; i < 10; i++) {
                mobileUsers.add(200 + Math.random() * 600, 200 + Math.random() * 500);
            }
            dataset.addSeries(mobileUsers);
            
            // Create chart
            JFreeChart chart = ChartFactory.createScatterPlot(
                "Drone-Assisted Communication Network Setup",
                "X Position (meters)",
                "Y Position (meters)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            );
            
            // Customize chart appearance
            customizeSetupChart(chart);
            
            // Save chart
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
            
            System.out.println("Network setup diagram saved to: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("Error generating network setup diagram: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Customizes the chart appearance with professional styling
     */
    private static void customizeSetupChart(JFreeChart chart) {
        // Title font
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 24));
        chart.getTitle().setPaint(Color.BLACK);
        
        XYPlot plot = chart.getXYPlot();
        
        // Background
        plot.setBackgroundPaint(new Color(245, 245, 250));
        chart.setBackgroundPaint(Color.WHITE);
        
        // Grid lines
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        
        // Axis fonts
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 18));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 18));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 14));
        
        // Renderer customization
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        
        // Ground Base Stations - Brown squares (larger)
        renderer.setSeriesShape(0, new Rectangle2D.Double(-8, -8, 16, 16));
        renderer.setSeriesPaint(0, new Color(139, 69, 19));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesOutlinePaint(0, Color.BLACK);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(2.5f));
        
        // Drone Base Stations - Blue triangles
        int[] xPoints = {0, 7, -7};
        int[] yPoints = {-8, 8, 8};
        Polygon triangle = new Polygon(xPoints, yPoints, 3);
        renderer.setSeriesShape(1, triangle);
        renderer.setSeriesPaint(1, new Color(30, 144, 255));
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesOutlinePaint(1, Color.BLACK);
        renderer.setSeriesOutlineStroke(1, new BasicStroke(2.5f));
        
        // Mobile Users - Red circles (smaller)
        renderer.setSeriesShape(2, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesPaint(2, new Color(220, 20, 60));
        renderer.setSeriesShapesVisible(2, true);
        renderer.setSeriesLinesVisible(2, false);
        renderer.setSeriesOutlinePaint(2, new Color(150, 0, 0));
        renderer.setSeriesOutlineStroke(2, new BasicStroke(1.5f));
        
        plot.setRenderer(renderer);
        
        // Legend
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 16));
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.BLACK));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        String outputDir = "results/research_paper_figures/";
        generateSetupDiagram(outputDir + "network_setup_diagram.png");
        System.out.println("Network setup diagram generation complete!");
    }
}
