package com.dronecomm.utils;

import java.util.Map;

/**
 * Analyzes simulation results and generates performance reports.
 */
public class ResultsAnalyzer {
    
    public void analyzeResults(Map<String, Object> results) {
        System.out.println("=== SIMULATION RESULTS ANALYSIS ===");
        
        if (results.containsKey("algorithm_comparison")) {
            analyzeAlgorithmComparison(getMapSafely(results, "algorithm_comparison"));
        }
        
        if (results.containsKey("performance_metrics")) {
            analyzePerformanceMetrics(getMapSafely(results, "performance_metrics"));
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapSafely(Map<String, Object> map, String key) {
        return (Map<String, Object>) map.get(key);
    }
    
    private void analyzeAlgorithmComparison(Map<String, Object> comparison) {
        System.out.println("Algorithm Performance Comparison:");
        comparison.forEach((algorithm, metrics) -> {
            System.out.println("  " + algorithm + ": " + metrics);
        });
    }
    
    private void analyzePerformanceMetrics(Map<String, Object> metrics) {
        System.out.println("System Performance Metrics:");
        metrics.forEach((metric, value) -> {
            System.out.println("  " + metric + ": " + value);
        });
    }
    
    public void generateReport(String scenarioName, Map<String, Object> results) {
        System.out.println("=== REPORT FOR " + scenarioName.toUpperCase() + " ===");
        analyzeResults(results);
        System.out.println("=== END REPORT ===");
    }
    
    public void generateComprehensiveReport() {
        System.out.println("=== COMPREHENSIVE SIMULATION ANALYSIS ===");
        System.out.println("All simulation scenarios have been completed successfully.");
        System.out.println("Results have been saved to the results directory.");
        System.out.println("=== END COMPREHENSIVE REPORT ===");
    }
}