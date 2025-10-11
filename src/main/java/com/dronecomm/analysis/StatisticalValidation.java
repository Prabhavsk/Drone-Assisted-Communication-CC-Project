package com.dronecomm.analysis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Statistical validation framework for research results
 */
public class StatisticalValidation {
    
    private static final double CONFIDENCE_LEVEL_95 = 1.96;
    private static final double CONFIDENCE_LEVEL_99 = 2.576;
    
    /**
     * Calculates confidence interval for a dataset
     */
    public static ConfidenceInterval calculateConfidenceInterval(List<Double> data, double confidenceLevel) {
        if (data.isEmpty()) {
            return new ConfidenceInterval(0, 0, 0);
        }
        
        double mean = calculateMean(data);
        double stdDev = calculateStandardDeviation(data);
        double standardError = stdDev / Math.sqrt(data.size());
        
        double zScore = (confidenceLevel == 0.95) ? CONFIDENCE_LEVEL_95 : CONFIDENCE_LEVEL_99;
        double marginOfError = zScore * standardError;
        
        return new ConfidenceInterval(mean, mean - marginOfError, mean + marginOfError);
    }
    
    /**
     * Performs t-test between two datasets
     */
    public static TTestResult performTTest(List<Double> group1, List<Double> group2) {
        if (group1.isEmpty() || group2.isEmpty()) {
            return new TTestResult(0, 1.0, false);
        }
        
        double mean1 = calculateMean(group1);
        double mean2 = calculateMean(group2);
        double var1 = calculateVariance(group1);
        double var2 = calculateVariance(group2);
        
        int n1 = group1.size();
        int n2 = group2.size();
        
        // Welch's t-test (unequal variances)
        double pooledStdError = Math.sqrt((var1/n1) + (var2/n2));
        double tStatistic = (mean1 - mean2) / pooledStdError;
        
        // Degrees of freedom (Welch-Satterthwaite equation)
        double numerator = Math.pow((var1/n1) + (var2/n2), 2);
        double denominator = (Math.pow(var1/n1, 2)/(n1-1)) + (Math.pow(var2/n2, 2)/(n2-1));
        int degreesOfFreedom = (int) Math.floor(numerator / denominator);
        
        // Approximate p-value (two-tailed)
        double pValue = 2 * (1 - approximateTCDF(Math.abs(tStatistic), degreesOfFreedom));
        boolean isSignificant = pValue < 0.05;
        
        return new TTestResult(tStatistic, pValue, isSignificant);
    }
    
    /**
     * Performs ANOVA test for multiple groups
     */
    public static ANOVAResult performANOVA(Map<String, List<Double>> groups) {
        if (groups.size() < 2) {
            return new ANOVAResult(0, 1.0, false);
        }
        
        // Calculate overall mean
        List<Double> allValues = groups.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        double overallMean = calculateMean(allValues);
        
        // Calculate sum of squares
        double ssBetween = 0.0;
        double ssWithin = 0.0;
        int totalCount = 0;
        
        for (List<Double> group : groups.values()) {
            double groupMean = calculateMean(group);
            int groupSize = group.size();
            totalCount += groupSize;
            
            // Between-group sum of squares
            ssBetween += groupSize * Math.pow(groupMean - overallMean, 2);
            
            // Within-group sum of squares
            for (double value : group) {
                ssWithin += Math.pow(value - groupMean, 2);
            }
        }
        
        int dfBetween = groups.size() - 1;
        int dfWithin = totalCount - groups.size();
        
        double msBetween = ssBetween / dfBetween;
        double msWithin = ssWithin / dfWithin;
        
        double fStatistic = msBetween / msWithin;
        double pValue = approximateFCDF(fStatistic, dfBetween, dfWithin);
        boolean isSignificant = pValue < 0.05;
        
        return new ANOVAResult(fStatistic, pValue, isSignificant);
    }
    
    /**
     * Calculates Jain's Fairness Index
     */
    public static double calculateJainsFairnessIndex(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double sumSquares = values.stream().mapToDouble(x -> x * x).sum();
        
        return (sum * sum) / (values.size() * sumSquares);
    }
    
    /**
     * Calculates coefficient of variation
     */
    public static double calculateCoefficientOfVariation(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        
        double mean = calculateMean(data);
        double stdDev = calculateStandardDeviation(data);
        
        return (mean != 0) ? (stdDev / Math.abs(mean)) : 0.0;
    }
    
    /**
     * Performs correlation analysis
     */
    public static CorrelationResult calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return new CorrelationResult(0.0, 1.0, false);
        }
        
        int n = x.size();
        double meanX = calculateMean(x);
        double meanY = calculateMean(y);
        
        double numerator = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;
        
        for (int i = 0; i < n; i++) {
            double xDiff = x.get(i) - meanX;
            double yDiff = y.get(i) - meanY;
            
            numerator += xDiff * yDiff;
            denomX += xDiff * xDiff;
            denomY += yDiff * yDiff;
        }
        
        double correlation = numerator / Math.sqrt(denomX * denomY);
        
        // Calculate t-statistic for significance test
        double tStat = correlation * Math.sqrt((n - 2) / (1 - correlation * correlation));
        double pValue = 2 * (1 - approximateTCDF(Math.abs(tStat), n - 2));
        boolean isSignificant = pValue < 0.05;
        
        return new CorrelationResult(correlation, pValue, isSignificant);
    }
    
    // Helper methods
    private static double calculateMean(List<Double> data) {
        return data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private static double calculateVariance(List<Double> data) {
        if (data.size() <= 1) return 0.0;
        
        double mean = calculateMean(data);
        double sumSquaredDiffs = data.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .sum();
        
        return sumSquaredDiffs / (data.size() - 1);
    }
    
    private static double calculateStandardDeviation(List<Double> data) {
        return Math.sqrt(calculateVariance(data));
    }
    
    // Approximate t-distribution CDF using normal approximation for large df
    private static double approximateTCDF(double t, int df) {
        if (df > 30) {
            // Use normal approximation for large degrees of freedom
            return approximateNormalCDF(t);
        } else {
            // Simple approximation for small df
            return 0.5 + 0.5 * Math.tanh(t / Math.sqrt(df / (df - 2.0)));
        }
    }
    
    // Approximate normal CDF
    private static double approximateNormalCDF(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }
    
    // Approximate error function
    private static double erf(double x) {
        // Abramowitz and Stegun approximation
        double a1 =  0.254829592;
        double a2 = -0.284496736;
        double a3 =  1.421413741;
        double a4 = -1.453152027;
        double a5 =  1.061405429;
        double p  =  0.3275911;
        
        int sign = (x >= 0) ? 1 : -1;
        x = Math.abs(x);
        
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return sign * y;
    }
    
    // Approximate F-distribution CDF (simplified)
    private static double approximateFCDF(double f, int df1, int df2) {
        // Very simplified approximation - in practice, use proper F-distribution
        if (f < 1.0) return f / 2.0;
        return Math.min(0.95, 1.0 - Math.exp(-f));
    }
    
    // Result classes
    public static class ConfidenceInterval {
        public final double mean;
        public final double lowerBound;
        public final double upperBound;
        
        public ConfidenceInterval(double mean, double lowerBound, double upperBound) {
            this.mean = mean;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
        
        @Override
        public String toString() {
            return String.format("Mean: %.3f, CI: [%.3f, %.3f]", mean, lowerBound, upperBound);
        }
    }
    
    public static class TTestResult {
        public final double tStatistic;
        public final double pValue;
        public final boolean isSignificant;
        
        public TTestResult(double tStatistic, double pValue, boolean isSignificant) {
            this.tStatistic = tStatistic;
            this.pValue = pValue;
            this.isSignificant = isSignificant;
        }
        
        @Override
        public String toString() {
            return String.format("t=%.3f, p=%.3f, significant=%s", tStatistic, pValue, isSignificant);
        }
    }
    
    public static class ANOVAResult {
        public final double fStatistic;
        public final double pValue;
        public final boolean isSignificant;
        
        public ANOVAResult(double fStatistic, double pValue, boolean isSignificant) {
            this.fStatistic = fStatistic;
            this.pValue = pValue;
            this.isSignificant = isSignificant;
        }
        
        @Override
        public String toString() {
            return String.format("F=%.3f, p=%.3f, significant=%s", fStatistic, pValue, isSignificant);
        }
    }
    
    public static class CorrelationResult {
        public final double correlation;
        public final double pValue;
        public final boolean isSignificant;
        
        public CorrelationResult(double correlation, double pValue, boolean isSignificant) {
            this.correlation = correlation;
            this.pValue = pValue;
            this.isSignificant = isSignificant;
        }
        
        @Override
        public String toString() {
            return String.format("r=%.3f, p=%.3f, significant=%s", correlation, pValue, isSignificant);
        }
    }
}