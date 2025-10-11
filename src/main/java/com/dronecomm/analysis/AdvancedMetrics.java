package com.dronecomm.analysis;

import com.dronecomm.entities.MobileUser;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced performance metrics for research validation
 */
public class AdvancedMetrics {
    
    /**
     * Calculates Social Welfare (sum of all user utilities)
     */
    public static double calculateSocialWelfare(Map<Object, Set<MobileUser>> assignments, 
                                              Map<MobileUser, Double> userUtilities) {
        return userUtilities.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
    
    /**
     * Calculates Price of Anarchy (ratio of worst Nash equilibrium to optimal solution)
     */
    public static double calculatePriceOfAnarchy(double nashWelfare, double optimalWelfare) {
        if (optimalWelfare == 0) return 1.0;
        return optimalWelfare / nashWelfare;
    }
    
    /**
     * Calculates Price of Stability (ratio of best Nash equilibrium to optimal solution)
     */
    public static double calculatePriceOfStability(double bestNashWelfare, double optimalWelfare) {
        if (optimalWelfare == 0) return 1.0;
        return optimalWelfare / bestNashWelfare;
    }
    
    /**
     * Calculates Regret bounds for online algorithms
     */
    public static RegretBounds calculateRegretBounds(List<Double> onlineUtilities, 
                                                   List<Double> optimalUtilities) {
        if (onlineUtilities.size() != optimalUtilities.size() || onlineUtilities.isEmpty()) {
            return new RegretBounds(0, 0, 0);
        }
        
        double totalRegret = 0;
        double maxRegret = 0;
        
        for (int t = 0; t < onlineUtilities.size(); t++) {
            double instantRegret = optimalUtilities.get(t) - onlineUtilities.get(t);
            totalRegret += instantRegret;
            maxRegret = Math.max(maxRegret, instantRegret);
        }
        
        double averageRegret = totalRegret / onlineUtilities.size();
        
        return new RegretBounds(totalRegret, averageRegret, maxRegret);
    }
    
    /**
     * Calculates Nash Product (product of user utilities - measures fairness)
     */
    public static double calculateNashProduct(Map<MobileUser, Double> userUtilities) {
        return userUtilities.values().stream()
                .mapToDouble(utility -> Math.max(0.001, utility)) // Avoid zero
                .reduce(1.0, (a, b) -> a * b);
    }
    
    /**
     * Calculates Shapley Value for cooperative games
     */
    public static Map<Object, Double> calculateShapleyValues(Map<Object, Set<MobileUser>> assignments,
                                                           Map<Object, Double> stationUtilities) {
        Map<Object, Double> shapleyValues = new HashMap<>();
        Set<Object> players = assignments.keySet();
        int n = players.size();
        
        for (Object player : players) {
            double shapleyValue = 0.0;
            
            // Calculate marginal contributions for all possible coalitions
            List<Object> otherPlayers = players.stream()
                    .filter(p -> !p.equals(player))
                    .collect(Collectors.toList());
            
            // For computational efficiency, sample subset of coalitions
            int maxCoalitions = Math.min(100, (int) Math.pow(2, Math.min(n-1, 10)));
            
            for (int i = 0; i < maxCoalitions; i++) {
                Set<Object> coalition = sampleCoalition(otherPlayers, i);
                
                double coalitionValue = calculateCoalitionValue(coalition, assignments, stationUtilities);
                double coalitionWithPlayerValue = calculateCoalitionValue(
                        addToSet(coalition, player), assignments, stationUtilities);
                
                double marginalContribution = coalitionWithPlayerValue - coalitionValue;
                
                // Weight by coalition size probability
                double weight = factorial(coalition.size()) * factorial(n - coalition.size() - 1) 
                              / factorial(n);
                
                shapleyValue += weight * marginalContribution;
            }
            
            shapleyValues.put(player, shapleyValue / maxCoalitions);
        }
        
        return shapleyValues;
    }
    
    /**
     * Calculates System Efficiency (ratio of achieved utility to maximum possible)
     */
    public static double calculateSystemEfficiency(double achievedUtility, double maxPossibleUtility) {
        if (maxPossibleUtility == 0) return 1.0;
        return achievedUtility / maxPossibleUtility;
    }
    
    /**
     * Calculates Load Distribution Metrics
     */
    public static LoadDistributionMetrics calculateLoadDistribution(Map<Object, Set<MobileUser>> assignments) {
        List<Integer> loads = assignments.values().stream()
                .map(Set::size)
                .collect(Collectors.toList());
        
        if (loads.isEmpty()) {
            return new LoadDistributionMetrics(0, 0, 0, 0, 1.0);
        }
        
        double mean = loads.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int min = loads.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = loads.stream().mapToInt(Integer::intValue).max().orElse(0);
        
        double variance = loads.stream()
                .mapToDouble(load -> Math.pow(load - mean, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        double jainIndex = StatisticalValidation.calculateJainsFairnessIndex(
                loads.stream().map(Integer::doubleValue).collect(Collectors.toList()));
        
        return new LoadDistributionMetrics(mean, min, max, stdDev, jainIndex);
    }
    
    /**
     * Calculates Convergence Metrics for iterative algorithms
     */
    public static ConvergenceMetrics calculateConvergenceMetrics(List<Double> utilities, 
                                                               double threshold) {
        if (utilities.size() < 2) {
            return new ConvergenceMetrics(false, utilities.size(), 0);
        }
        
        int convergenceIteration = -1;
        double finalValue = utilities.get(utilities.size() - 1);
        
        // Find when algorithm converged within threshold
        for (int i = utilities.size() - 1; i >= 1; i--) {
            if (Math.abs(utilities.get(i) - utilities.get(i-1)) > threshold) {
                convergenceIteration = i + 1;
                break;
            }
        }
        
        boolean hasConverged = convergenceIteration != -1;
        if (!hasConverged) convergenceIteration = utilities.size();
        
        // Calculate convergence rate (geometric decay assumption)
        double convergenceRate = 0;
        if (utilities.size() > 10) {
            int start = Math.max(0, utilities.size() - 10);
            List<Double> recent = utilities.subList(start, utilities.size());
            
            double initialDiff = Math.abs(recent.get(0) - finalValue);
            double finalDiff = Math.abs(recent.get(recent.size()-1) - finalValue);
            
            if (initialDiff > 0) {
                convergenceRate = Math.log(finalDiff / initialDiff) / recent.size();
            }
        }
        
        return new ConvergenceMetrics(hasConverged, convergenceIteration, convergenceRate);
    }
    
    /**
     * Calculates Quality of Service Metrics
     */
    public static QoSMetrics calculateQoSMetrics(Map<Object, Set<MobileUser>> assignments,
                                               Map<MobileUser, Double> throughputs,
                                               Map<MobileUser, Double> latencies) {
        List<MobileUser> allUsers = assignments.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
        
        if (allUsers.isEmpty()) {
            return new QoSMetrics(0, 0, 0, 1.0, 1.0);
        }
        
        // Calculate average metrics
        double avgThroughput = allUsers.stream()
                .mapToDouble(user -> throughputs.getOrDefault(user, 0.0))
                .average().orElse(0.0);
        
        double avgLatency = allUsers.stream()
                .mapToDouble(user -> latencies.getOrDefault(user, 0.0))
                .average().orElse(0.0);
        
        // Calculate SLA violations
        long throughputViolations = allUsers.stream()
                .mapToLong(user -> {
                    double achieved = throughputs.getOrDefault(user, 0.0);
                    double required = user.getMinRequiredThroughput();
                    return (achieved < required) ? 1 : 0;
                })
                .sum();
        
        long latencyViolations = allUsers.stream()
                .mapToLong(user -> {
                    double achieved = latencies.getOrDefault(user, 0.0);
                    double required = user.getMaxAcceptableLatency();
                    return (achieved > required) ? 1 : 0;
                })
                .sum();
        
        double throughputSLARate = 1.0 - (double) throughputViolations / allUsers.size();
        double latencySLARate = 1.0 - (double) latencyViolations / allUsers.size();
        
        return new QoSMetrics(avgThroughput, avgLatency, 
                            throughputViolations + latencyViolations, 
                            throughputSLARate, latencySLARate);
    }
    
    // Helper methods
    private static Set<Object> sampleCoalition(List<Object> players, int seed) {
        Random random = new Random(seed);
        Set<Object> coalition = new HashSet<>();
        
        for (Object player : players) {
            if (random.nextBoolean()) {
                coalition.add(player);
            }
        }
        
        return coalition;
    }
    
    private static Set<Object> addToSet(Set<Object> original, Object element) {
        Set<Object> newSet = new HashSet<>(original);
        newSet.add(element);
        return newSet;
    }
    
    private static double calculateCoalitionValue(Set<Object> coalition,
                                                Map<Object, Set<MobileUser>> assignments,
                                                Map<Object, Double> stationUtilities) {
        return coalition.stream()
                .mapToDouble(station -> stationUtilities.getOrDefault(station, 0.0))
                .sum();
    }
    
    private static long factorial(int n) {
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= Math.min(n, 20); i++) { // Limit to prevent overflow
            result *= i;
        }
        return result;
    }
    
    // Result classes
    public static class RegretBounds {
        public final double totalRegret;
        public final double averageRegret;
        public final double maxRegret;
        
        public RegretBounds(double totalRegret, double averageRegret, double maxRegret) {
            this.totalRegret = totalRegret;
            this.averageRegret = averageRegret;
            this.maxRegret = maxRegret;
        }
        
        @Override
        public String toString() {
            return String.format("Total: %.3f, Avg: %.3f, Max: %.3f", 
                               totalRegret, averageRegret, maxRegret);
        }
    }
    
    public static class LoadDistributionMetrics {
        public final double meanLoad;
        public final int minLoad;
        public final int maxLoad;
        public final double standardDeviation;
        public final double jainsFairnessIndex;
        
        public LoadDistributionMetrics(double meanLoad, int minLoad, int maxLoad, 
                                     double standardDeviation, double jainsFairnessIndex) {
            this.meanLoad = meanLoad;
            this.minLoad = minLoad;
            this.maxLoad = maxLoad;
            this.standardDeviation = standardDeviation;
            this.jainsFairnessIndex = jainsFairnessIndex;
        }
        
        @Override
        public String toString() {
            return String.format("Mean: %.2f, Range: [%d, %d], StdDev: %.2f, Jain: %.3f",
                               meanLoad, minLoad, maxLoad, standardDeviation, jainsFairnessIndex);
        }
    }
    
    public static class ConvergenceMetrics {
        public final boolean hasConverged;
        public final int convergenceIteration;
        public final double convergenceRate;
        
        public ConvergenceMetrics(boolean hasConverged, int convergenceIteration, double convergenceRate) {
            this.hasConverged = hasConverged;
            this.convergenceIteration = convergenceIteration;
            this.convergenceRate = convergenceRate;
        }
        
        @Override
        public String toString() {
            return String.format("Converged: %s, Iteration: %d, Rate: %.4f",
                               hasConverged, convergenceIteration, convergenceRate);
        }
    }
    
    public static class QoSMetrics {
        public final double averageThroughput;
        public final double averageLatency;
        public final long totalViolations;
        public final double throughputSLARate;
        public final double latencySLARate;
        
        public QoSMetrics(double averageThroughput, double averageLatency, long totalViolations,
                         double throughputSLARate, double latencySLARate) {
            this.averageThroughput = averageThroughput;
            this.averageLatency = averageLatency;
            this.totalViolations = totalViolations;
            this.throughputSLARate = throughputSLARate;
            this.latencySLARate = latencySLARate;
        }
        
        @Override
        public String toString() {
            return String.format("Throughput: %.2f Mbps, Latency: %.2f ms, Violations: %d, SLA: %.1f%%",
                               averageThroughput/1e6, averageLatency, totalViolations, 
                               Math.min(throughputSLARate, latencySLARate) * 100);
        }
    }
}