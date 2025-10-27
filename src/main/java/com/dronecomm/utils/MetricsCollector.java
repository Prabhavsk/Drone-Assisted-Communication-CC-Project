package com.dronecomm.utils;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects and computes various QoS metrics during simulation.
 * Tracks performance indicators for load balancing evaluation.
 */
public class MetricsCollector {
    
    // Removed logger to avoid dependency issues
    // private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);
    
    // Metrics storage
    private Map<Double, Double> latencyOverTime;
    private Map<Double, Double> throughputOverTime;
    private Map<Double, Double> energyConsumptionOverTime;
    private Map<Double, Double> loadDistributionOverTime;
    private Map<Double, Integer> userSatisfactionOverTime;
    
    // Entity references for tracking
    private List<DroneBaseStation> drones;
    private List<GroundBaseStation> groundStations;
    private List<MobileUser> users;
    
    // Cumulative metrics
    private double totalEnergyConsumed;
    private double totalDataTransferred;
    private int totalHandovers;
    private int totalConnections;
    
    // Current simulation time tracking
    private double currentSimulationTime = 0.0;
    
    public MetricsCollector() {
        latencyOverTime = new HashMap<>();
        throughputOverTime = new HashMap<>();
        energyConsumptionOverTime = new HashMap<>();
        loadDistributionOverTime = new HashMap<>();
        userSatisfactionOverTime = new HashMap<>();
        
        totalEnergyConsumed = 0.0;
        totalDataTransferred = 0.0;
        totalHandovers = 0;
        totalConnections = 0;
    }
    
    public void setEntities(List<DroneBaseStation> drones, 
                           List<GroundBaseStation> groundStations, 
                           List<MobileUser> users) {
        // defensively handle null inputs to avoid NPEs elsewhere
        this.drones = drones == null ? Collections.emptyList() : new ArrayList<>(drones);
        this.groundStations = groundStations == null ? Collections.emptyList() : new ArrayList<>(groundStations);
        this.users = users == null ? Collections.emptyList() : new ArrayList<>(users);
    }
    
    /**
     * Collect metrics at current simulation time.
     */
    public void collectMetrics() {
        collectLatencyMetrics(currentSimulationTime);
        collectThroughputMetrics(currentSimulationTime);
        collectEnergyMetrics(currentSimulationTime);
        collectLoadDistributionMetrics(currentSimulationTime);
        collectUserSatisfactionMetrics(currentSimulationTime);
        
        // logger.debug("Collected metrics at time: {}", currentSimulationTime);
        System.out.println("Collected metrics at time: " + currentSimulationTime);
    }
    
    /**
     * Update the current simulation time.
     */
    public void updateSimulationTime(double time) {
        this.currentSimulationTime = time;
    }
    
    private void collectLatencyMetrics(double currentTime) {
        if (users == null || users.isEmpty()) return;
        
        double totalLatency = 0.0;
        int connectedUsers = 0;
        
        for (MobileUser user : users) {
            if (user.isConnected()) {
                totalLatency += user.getExperiencedLatency();
                connectedUsers++;
            }
        }
        
        double averageLatency = connectedUsers > 0 ? totalLatency / connectedUsers : 0.0;
        latencyOverTime.put(currentTime, averageLatency);
    }
    
    private void collectThroughputMetrics(double currentTime) {
        double totalThroughput = 0.0;
        
        // Aggregate throughput from all base stations using data transmission
        if (drones != null) {
            for (DroneBaseStation drone : drones) {
                // Estimate throughput from data rate capacity
                totalThroughput += drone.getBandwidth() * drone.getCurrentLoadPercentage() / 100.0;
            }
        }
        
        if (groundStations != null) {
            for (GroundBaseStation station : groundStations) {
                // Estimate throughput from load percentage
                totalThroughput += station.getBandwidth() * station.getCurrentLoadPercentage() / 100.0;
            }
        }
        
        throughputOverTime.put(currentTime, totalThroughput);
        totalDataTransferred += totalThroughput;
    }
    
    private void collectEnergyMetrics(double currentTime) {
        double totalEnergy = 0.0;
        
        if (drones != null) {
            for (DroneBaseStation drone : drones) {
                // Calculate energy consumption from current energy level
                double energyUsed = drone.getTotalEnergyCapacity() - drone.getCurrentEnergyLevel();
                totalEnergy += energyUsed;
            }
        }
        
        if (groundStations != null) {
            for (GroundBaseStation station : groundStations) {
                // Ground stations have fixed energy consumption based on load
                double baseConsumption = 1000.0; // Base consumption in watts
                double loadFactor = station.getCurrentLoadPercentage() / 100.0;
                totalEnergy += baseConsumption * (0.3 + 0.7 * loadFactor); // Min 30%, max 100%
            }
        }
        
        energyConsumptionOverTime.put(currentTime, totalEnergy);
        totalEnergyConsumed += totalEnergy;
    }
    
    private void collectLoadDistributionMetrics(double currentTime) {
        List<Integer> loads = new ArrayList<>();
        
        if (drones != null) {
            loads.addAll(drones.stream()
                    .map(DroneBaseStation::getCurrentConnectedUserCount)
                    .collect(Collectors.toList()));
        }
        
        if (groundStations != null) {
            loads.addAll(groundStations.stream()
                    .map(GroundBaseStation::getCurrentConnectedUserCount)
                    .collect(Collectors.toList()));
        }
        
        double loadVariance = calculateVariance(loads);
        loadDistributionOverTime.put(currentTime, loadVariance);
    }
    
    private void collectUserSatisfactionMetrics(double currentTime) {
        if (users == null || users.isEmpty()) return;
        
        int satisfiedUsers = 0;
        for (MobileUser user : users) {
            // User is satisfied if satisfaction level is above 70%
            if (user.getSatisfactionLevel() > 0.7) {
                satisfiedUsers++;
            }
        }
        
        userSatisfactionOverTime.put(currentTime, satisfiedUsers);
    }
    
    private double calculateVariance(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average()
                .orElse(0.0);
        
        return variance;
    }
    
    // Getter methods for final results
    public double getAverageLatency() {
        return latencyOverTime.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    public double getAverageThroughput() {
        return throughputOverTime.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }
    
    public double getAverageLoadVariance() {
        return loadDistributionOverTime.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    public double getAverageUserSatisfaction() {
        if (users == null || users.isEmpty()) return 0.0;
        
        return userSatisfactionOverTime.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0) / users.size();
    }
    
    public int getTotalHandovers() {
        return totalHandovers;
    }
    
    public void incrementHandovers() {
        totalHandovers++;
    }
    
    public int getTotalConnections() {
        return totalConnections;
    }
    
    public void incrementConnections() {
        totalConnections++;
    }
    
    // Getters for time series data
    public Map<Double, Double> getLatencyOverTime() {
        return new HashMap<>(latencyOverTime);
    }
    
    public Map<Double, Double> getThroughputOverTime() {
        return new HashMap<>(throughputOverTime);
    }
    
    public Map<Double, Double> getEnergyConsumptionOverTime() {
        return new HashMap<>(energyConsumptionOverTime);
    }
    
    public Map<Double, Double> getLoadDistributionOverTime() {
        return new HashMap<>(loadDistributionOverTime);
    }
    
    public Map<Double, Integer> getUserSatisfactionOverTime() {
        return new HashMap<>(userSatisfactionOverTime);
    }
    
    /**
     * Generate a summary report of all collected metrics.
     */
    public String generateSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Simulation Metrics Summary ===\n");
        // NOTE: units are "model units" and depend on how base stations and users report them.
        // - Latency is reported using user.getExperiencedLatency() units
        // - Throughput is aggregated from base station bandwidth/load; confirm units (bps or Mbps) in the station classes
        // - Energy is a model-specific metric (power/energy units depend on station implementations)
        report.append(String.format("Average Latency (model units): %.2f\n", getAverageLatency()));
        report.append(String.format("Average Throughput (model units): %.2f\n", getAverageThroughput()));
        report.append(String.format("Total Energy (model units): %.2f\n", getTotalEnergyConsumed()));
        report.append(String.format("Average Load Variance: %.2f\n", getAverageLoadVariance()));
        report.append(String.format("Average User Satisfaction: %.2f%%\n", getAverageUserSatisfaction() * 100));
        report.append(String.format("Total Handovers: %d\n", getTotalHandovers()));
        report.append(String.format("Total Connections: %d\n", getTotalConnections()));
        
        return report.toString();
    }
    
    // Additional methods needed by simulation
    private String scenarioName;
    
    public void reset() {
        latencyOverTime.clear();
        throughputOverTime.clear();
        energyConsumptionOverTime.clear();
        loadDistributionOverTime.clear();
        userSatisfactionOverTime.clear();
        // reset cumulative counters as well
        totalEnergyConsumed = 0.0;
        totalDataTransferred = 0.0;
        totalHandovers = 0;
        totalConnections = 0;
    }
    
    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }
    
    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();
        results.put("latency", latencyOverTime);
        results.put("throughput", throughputOverTime);
        results.put("energy", energyConsumptionOverTime);
        results.put("load_distribution", loadDistributionOverTime);
        results.put("user_satisfaction", userSatisfactionOverTime);
        results.put("scenario_name", scenarioName);
        return results;
    }
}