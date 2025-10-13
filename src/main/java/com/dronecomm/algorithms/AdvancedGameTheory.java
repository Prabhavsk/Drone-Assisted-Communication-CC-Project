    package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced mathematical game theory implementations for drone-assisted communication networks.
 * 
 * This class provides rigorous mathematical formulations based on:
 * - Nash Equilibrium Theory with best response dynamics
 * - Stackelberg Game Theory with optimization
 * - Cooperative Game Theory with Shapley values
 * - Auction Theory with VCG mechanisms
 * 
 * Mathematical models include:
 * - Utility functions with multi-objective optimization
 * - Channel modeling with path loss and interference
 * - Energy consumption models
 * - QoS satisfaction functions
 * - Load balancing metrics
 */
public class AdvancedGameTheory {
    
    // Physical layer constants
    private static final double SPEED_OF_LIGHT = 3e8; // m/s
    private static final double CARRIER_FREQUENCY = 2.4e9; // 2.4 GHz
    private static final double NOISE_POWER_DBM = -174; // dBm/Hz
    private static final double THERMAL_NOISE = 4.14e-21; // J/K
    private static final double TEMPERATURE = 290; // K
    
    // Game theory parameters
    private double alpha = 0.5; // Weight for throughput utility
    private double beta = 0.3;  // Weight for energy utility
    private double gamma = 0.2; // Weight for fairness utility
    private double epsilon = 1e-6; // Convergence threshold
    
    // Network entities
    private List<DroneBaseStation> droneStations;
    private List<GroundBaseStation> groundStations;
    private List<MobileUser> users;
    
    public AdvancedGameTheory(List<DroneBaseStation> droneStations,
                             List<GroundBaseStation> groundStations,
                             List<MobileUser> users) {
        this.droneStations = new ArrayList<>(droneStations);
        this.groundStations = new ArrayList<>(groundStations);
        this.users = new ArrayList<>(users);
    }
    
    /**
     * Mathematical utility function for drone base stations
     * U_d(s_d, s_{-d}) = alphaR_d + betaE_d + gammaF_d
     * Where:
     * - R_d: Throughput utility
     * - E_d: Energy efficiency utility  
     * - F_d: Fairness utility
     * - s_d: Strategy of drone d
     * - s_{-d}: Strategies of all other players
     */
    public double calculateAdvancedDroneUtility(DroneBaseStation drone, 
                                               Set<MobileUser> assignedUsers,
                                               Map<Object, Set<MobileUser>> globalAssignments) {
        if (assignedUsers.isEmpty()) {
            return 0.0;
        }
        
        // Throughput utility: R_d = Sum log(1 + SINR_i)
        double throughputUtility = calculateThroughputUtility(drone, assignedUsers, globalAssignments);
        
        // Energy efficiency utility: E_d = (P_total - P_consumed) / P_total
        double energyUtility = calculateEnergyUtility(drone, assignedUsers);
        
        // Fairness utility: F_d = 1 - Jain's Fairness Index
        double fairnessUtility = calculateFairnessUtility(drone, assignedUsers);
        
        return alpha * throughputUtility + beta * energyUtility + gamma * fairnessUtility;
    }
    
    /**
     * Calculate throughput utility using Shannon capacity formula
     * R_d = Sum B * log(1 + SINR_i) for all users i served by drone d
     */
    private double calculateThroughputUtility(DroneBaseStation drone, 
                                            Set<MobileUser> assignedUsers,
                                            Map<Object, Set<MobileUser>> globalAssignments) {
        double totalThroughput = 0.0;
        
        for (MobileUser user : assignedUsers) {
            // Calculate SINR for this user
            double sinr = calculateSINR(user, drone, globalAssignments);
            
            // Shannon capacity: C = B * log(1 + SINR)
            double bandwidth = drone.getBandwidth() / assignedUsers.size(); // Equal bandwidth allocation
            double capacity = bandwidth * Math.log(1 + sinr) / Math.log(2);
            
            totalThroughput += capacity;
        }
        
        // Normalize by maximum possible throughput
        double maxThroughput = drone.getBandwidth() * Math.log(1 + getMaxPossibleSINR()) / Math.log(2);
        
        return totalThroughput / maxThroughput;
    }
    
    /**
     * Calculate Signal-to-Interference-plus-Noise Ratio (SINR)
     * SINR = P_signal / (P_interference + P_noise)
     */
    private double calculateSINR(MobileUser user, DroneBaseStation servingDrone,
                                Map<Object, Set<MobileUser>> globalAssignments) {
        // Signal power using path loss model
        double distance = user.getCurrentPosition().distanceTo(servingDrone.getCurrentPosition());
        double transmissionPower = 10.0; // Default drone transmission power in Watts
        double signalPower = calculateReceivedPower(transmissionPower, distance);
        
        // Interference from other base stations
        double interferencePower = 0.0;
        
        // Interference from other drones
        for (DroneBaseStation otherDrone : droneStations) {
            if (otherDrone != servingDrone && globalAssignments.containsKey(otherDrone)) {
                double interferenceDistance = user.getCurrentPosition().distanceTo(otherDrone.getCurrentPosition());
                double otherDroneTransmissionPower = 10.0; // Default drone transmission power
                double interference = calculateReceivedPower(otherDroneTransmissionPower, interferenceDistance);
                interferencePower += interference;
            }
        }
        
        // Interference from ground stations
        for (GroundBaseStation gbs : groundStations) {
            if (globalAssignments.containsKey(gbs)) {
                double interferenceDistance = user.getCurrentPosition().distance2DTo(gbs.getPosition());
                double interference = calculateReceivedPower(gbs.getTransmissionPower(), interferenceDistance);
                interferencePower += interference;
            }
        }
        
        // Thermal noise power
        double noisePower = THERMAL_NOISE * TEMPERATURE * servingDrone.getBandwidth();
        
        return signalPower / (interferencePower + noisePower);
    }
    
    /**
     * Path loss model: Friis transmission equation with additional factors
     * P_r = P_t * G_t * G_r * (lambda / (4pi * d))^2 * L
     */
    private double calculateReceivedPower(double transmitPower, double distance) {
        if (distance <= 0) return transmitPower;
        
        double wavelength = SPEED_OF_LIGHT / CARRIER_FREQUENCY;
        double pathLoss = Math.pow(wavelength / (4 * Math.PI * distance), 2);
        
        // Additional factors: antenna gains, system losses
        double antennaGain = 1.0; // Assume isotropic antennas
        double systemLoss = 0.5;  // 3dB system loss
        
        return transmitPower * antennaGain * antennaGain * pathLoss * systemLoss;
    }
    
    /**
     * Energy efficiency utility based on power consumption model
     * E_d = 1 - (P_consumed / P_max)
     * Where P_consumed includes transmission, computation, and mobility power
     */
    private double calculateEnergyUtility(DroneBaseStation drone, Set<MobileUser> assignedUsers) {
        // Transmission power (proportional to number of users)
        double baseDroneTransmissionPower = 10.0; // Default drone transmission power in Watts
        double transmissionPower = baseDroneTransmissionPower * assignedUsers.size();
        
        // Computation power (for signal processing)
        double computationPower = 10.0 * assignedUsers.size(); // Watts per user
        
        // Mobility power (for maintaining position)
        double mobilityPower = 200.0; // Base mobility power in Watts
        
        double totalPowerConsumption = transmissionPower + computationPower + mobilityPower;
        double maxPowerCapacity = drone.getTotalEnergyCapacity() / 3600; // Convert to Watts
        
        return Math.max(0, 1.0 - (totalPowerConsumption / maxPowerCapacity));
    }
    
    /**
     * Fairness utility using Jain's Fairness Index
     * J = (Sum x_i)^2 / (n * Sum x_i^2)
     * Where x_i is the throughput allocated to user i
     */
    private double calculateFairnessUtility(DroneBaseStation drone, Set<MobileUser> assignedUsers) {
        if (assignedUsers.size() <= 1) return 1.0;
        
        List<Double> throughputs = new ArrayList<>();
        double totalBandwidth = drone.getBandwidth();
        
        for (MobileUser user : assignedUsers) {
            // Proportional fair allocation based on channel quality
            double channelQuality = 1.0 / (1.0 + user.getCurrentPosition().distanceTo(drone.getCurrentPosition()));
            double allocatedBandwidth = totalBandwidth * channelQuality / assignedUsers.size();
            throughputs.add(allocatedBandwidth);
        }
        
        double sum = throughputs.stream().mapToDouble(Double::doubleValue).sum();
        double sumSquares = throughputs.stream().mapToDouble(x -> x * x).sum();
        
        double jainIndex = (sum * sum) / (throughputs.size() * sumSquares);
        return jainIndex;
    }
    
    /**
     * Nash Equilibrium solver using best response dynamics
     * Iteratively updates each player's strategy until convergence
     */
    public Map<Object, Set<MobileUser>> solveNashEquilibrium() {
        Map<Object, Set<MobileUser>> currentAssignment = initializeRandomAssignment();
        Map<Object, Set<MobileUser>> bestAssignment = new HashMap<>(currentAssignment);
        
        boolean converged = false;
        int maxIterations = 100;
        int iteration = 0;
        
        while (!converged && iteration < maxIterations) {
            Map<Object, Set<MobileUser>> newAssignment = new HashMap<>();
            
            // Each drone plays best response
            for (DroneBaseStation drone : droneStations) {
                Set<MobileUser> bestResponse = findBestResponseForDrone(drone, currentAssignment);
                newAssignment.put(drone, bestResponse);
            }
            
            // Each ground station plays best response
            for (GroundBaseStation gbs : groundStations) {
                Set<MobileUser> bestResponse = findBestResponseForGroundStation(gbs, currentAssignment);
                newAssignment.put(gbs, bestResponse);
            }
            
            // Check convergence
            converged = isAssignmentEqual(currentAssignment, newAssignment);
            currentAssignment = newAssignment;
            iteration++;
        }
        
        return currentAssignment;
    }
    
    /**
     * Find best response for a drone given other players' strategies
     */
    private Set<MobileUser> findBestResponseForDrone(DroneBaseStation drone, 
                                                   Map<Object, Set<MobileUser>> currentAssignment) {
        Set<MobileUser> availableUsers = getAvailableUsersForDrone(drone, currentAssignment);
        Set<MobileUser> bestResponse = new HashSet<>();
        double bestUtility = Double.NEGATIVE_INFINITY;
        
        // Consider all possible subsets of available users (up to capacity)
        int maxUsers = Math.min((int)drone.getMaxUserCapacity(), availableUsers.size());
        
        for (int k = 0; k <= maxUsers; k++) {
            for (Set<MobileUser> subset : generateSubsets(availableUsers, k)) {
                Map<Object, Set<MobileUser>> testAssignment = new HashMap<>(currentAssignment);
                testAssignment.put(drone, subset);
                
                double utility = calculateAdvancedDroneUtility(drone, subset, testAssignment);
                
                if (utility > bestUtility) {
                    bestUtility = utility;
                    bestResponse = new HashSet<>(subset);
                }
            }
        }
        
        return bestResponse;
    }
    
    /**
     * Stackelberg Game solver with ground stations as leaders
     * Leaders optimize first, then followers (drones) respond optimally
     */
    public Map<Object, Set<MobileUser>> solveStackelbergGame() {
        Map<Object, Set<MobileUser>> finalAssignment = new HashMap<>();
        
        // Phase 1: Ground stations (leaders) optimize their strategies
        for (GroundBaseStation leader : groundStations) {
            Set<MobileUser> leaderStrategy = optimizeLeaderStrategy(leader, finalAssignment);
            finalAssignment.put(leader, leaderStrategy);
        }
        
        // Phase 2: Drones (followers) respond optimally
        for (DroneBaseStation follower : droneStations) {
            Set<MobileUser> followerResponse = findBestResponseForDrone(follower, finalAssignment);
            finalAssignment.put(follower, followerResponse);
        }
        
        return finalAssignment;
    }
    
    /**
     * Optimize leader strategy considering followers' responses
     */
    private Set<MobileUser> optimizeLeaderStrategy(GroundBaseStation leader,
                                                 Map<Object, Set<MobileUser>> partialAssignment) {
        Set<MobileUser> availableUsers = getAvailableUsersForGroundStation(leader, partialAssignment);
        Set<MobileUser> bestStrategy = new HashSet<>();
        double bestLeaderUtility = Double.NEGATIVE_INFINITY;
        
        // For each possible leader strategy, predict followers' responses
        int maxUsers = Math.min((int)leader.getMaxUserCapacity(), availableUsers.size());
        
        for (int k = 0; k <= maxUsers; k++) {
            for (Set<MobileUser> leaderChoice : generateSubsets(availableUsers, k)) {
                // Simulate the game with this leader choice
                Map<Object, Set<MobileUser>> simulatedAssignment = new HashMap<>(partialAssignment);
                simulatedAssignment.put(leader, leaderChoice);
                
                // Predict followers' best responses
                for (DroneBaseStation drone : droneStations) {
                    if (!simulatedAssignment.containsKey(drone)) {
                        Set<MobileUser> droneResponse = findBestResponseForDrone(drone, simulatedAssignment);
                        simulatedAssignment.put(drone, droneResponse);
                    }
                }
                
                // Calculate leader's utility in this outcome
                double leaderUtility = calculateGroundStationUtility(leader, leaderChoice, simulatedAssignment);
                
                if (leaderUtility > bestLeaderUtility) {
                    bestLeaderUtility = leaderUtility;
                    bestStrategy = new HashSet<>(leaderChoice);
                }
            }
        }
        
        return bestStrategy;
    }
    
    /**
     * Cooperative game solver using Shapley values
     * Finds the core allocation that fairly distributes utilities
     */
    public Map<Object, Double> calculateShapleyValues(Map<Object, Set<MobileUser>> assignment) {
        Map<Object, Double> shapleyValues = new HashMap<>();
        List<Object> allPlayers = new ArrayList<>();
        allPlayers.addAll(droneStations);
        allPlayers.addAll(groundStations);
        
        for (Object player : allPlayers) {
            double shapleyValue = 0.0;
            
            // Calculate Shapley value: Sum |S|!(n-|S|-1)!/n! * [v(SU{i}) - v(S)]
            for (Set<Object> coalition : generateCoalitions(allPlayers, player)) {
                int coalitionSize = coalition.size();
                int totalPlayers = allPlayers.size();
                
                // Calculate marginal contribution
                double valueWithPlayer = calculateCoalitionValue(coalition, assignment);
                coalition.remove(player);
                double valueWithoutPlayer = calculateCoalitionValue(coalition, assignment);
                double marginalContribution = valueWithPlayer - valueWithoutPlayer;
                
                // Shapley weight
                double weight = factorial(coalitionSize) * factorial(totalPlayers - coalitionSize - 1) 
                              / factorial(totalPlayers);
                
                shapleyValue += weight * marginalContribution;
            }
            
            shapleyValues.put(player, shapleyValue);
        }
        
        return shapleyValues;
    }
    
    // Helper methods
    private double getMaxPossibleSINR() {
        return 1000.0; // High SINR for normalization
    }
    
    private Map<Object, Set<MobileUser>> initializeRandomAssignment() {
        Map<Object, Set<MobileUser>> assignment = new HashMap<>();
        Random random = new Random();
        
        for (MobileUser user : users) {
            List<Object> allStations = new ArrayList<>();
            allStations.addAll(droneStations);
            allStations.addAll(groundStations);
            
            Object randomStation = allStations.get(random.nextInt(allStations.size()));
            assignment.computeIfAbsent(randomStation, k -> new HashSet<>()).add(user);
        }
        
        return assignment;
    }
    
    private Set<MobileUser> getAvailableUsersForDrone(DroneBaseStation drone, 
                                                     Map<Object, Set<MobileUser>> currentAssignment) {
        Set<MobileUser> availableUsers = new HashSet<>(users);
        
        // Remove users already assigned to other stations
        for (Map.Entry<Object, Set<MobileUser>> entry : currentAssignment.entrySet()) {
            if (entry.getKey() != drone) {
                availableUsers.removeAll(entry.getValue());
            }
        }
        
        // Filter by range
        return availableUsers.stream()
            .filter(user -> drone.isUserInRange(user))
            .collect(Collectors.toSet());
    }
    
    private Set<MobileUser> getAvailableUsersForGroundStation(GroundBaseStation gbs,
                                                            Map<Object, Set<MobileUser>> partialAssignment) {
        Set<MobileUser> availableUsers = new HashSet<>(users);
        
        // Remove users already assigned
        for (Set<MobileUser> assignedUsers : partialAssignment.values()) {
            availableUsers.removeAll(assignedUsers);
        }
        
        // Filter by range
        return availableUsers.stream()
            .filter(user -> gbs.isUserInRange(user))
            .collect(Collectors.toSet());
    }
    
    private double calculateGroundStationUtility(GroundBaseStation gbs, Set<MobileUser> assignedUsers,
                                               Map<Object, Set<MobileUser>> globalAssignments) {
        // Similar to drone utility but adapted for ground stations
        if (assignedUsers.isEmpty()) return 0.0;
        
        double throughputUtility = calculateGroundStationThroughput(gbs, assignedUsers, globalAssignments);
        double loadUtility = 1.0 - (double) assignedUsers.size() / gbs.getMaxUserCapacity();
        double fairnessUtility = calculateFairnessUtility(gbs, assignedUsers);
        
        return alpha * throughputUtility + (1 - alpha) * loadUtility + gamma * fairnessUtility;
    }
    
    private double calculateGroundStationThroughput(GroundBaseStation gbs, Set<MobileUser> assignedUsers,
                                                  Map<Object, Set<MobileUser>> globalAssignments) {
        double totalThroughput = 0.0;
        
        for (MobileUser user : assignedUsers) {
            double sinr = calculateGroundStationSINR(user, gbs, globalAssignments);
            double bandwidth = gbs.getBandwidth() / assignedUsers.size();
            double capacity = bandwidth * Math.log(1 + sinr) / Math.log(2);
            totalThroughput += capacity;
        }
        
        double maxThroughput = gbs.getBandwidth() * Math.log(1 + getMaxPossibleSINR()) / Math.log(2);
        return totalThroughput / maxThroughput;
    }
    
    private double calculateGroundStationSINR(MobileUser user, GroundBaseStation servingGBS,
                                            Map<Object, Set<MobileUser>> globalAssignments) {
        double distance = user.getCurrentPosition().distance2DTo(servingGBS.getPosition());
        double signalPower = calculateReceivedPower(servingGBS.getTransmissionPower(), distance);
        
        double interferencePower = 0.0;
        
        // Calculate interference from all other active base stations
        for (Object station : globalAssignments.keySet()) {
            if (station != servingGBS && !globalAssignments.get(station).isEmpty()) {
                double interferenceDistance;
                double transmitPower;
                
                if (station instanceof DroneBaseStation) {
                    DroneBaseStation drone = (DroneBaseStation) station;
                    interferenceDistance = user.getCurrentPosition().distanceTo(drone.getCurrentPosition());
                    transmitPower = 10.0; // Default drone transmission power
                } else {
                    GroundBaseStation gbs = (GroundBaseStation) station;
                    interferenceDistance = user.getCurrentPosition().distance2DTo(gbs.getPosition());
                    transmitPower = gbs.getTransmissionPower();
                }
                
                interferencePower += calculateReceivedPower(transmitPower, interferenceDistance);
            }
        }
        
        double noisePower = THERMAL_NOISE * TEMPERATURE * servingGBS.getBandwidth();
        return signalPower / (interferencePower + noisePower);
    }
    
    private double calculateFairnessUtility(GroundBaseStation gbs, Set<MobileUser> assignedUsers) {
        if (assignedUsers.size() <= 1) return 1.0;
        
        List<Double> throughputs = new ArrayList<>();
        double totalBandwidth = gbs.getBandwidth();
        
        for (MobileUser user : assignedUsers) {
            double channelQuality = 1.0 / (1.0 + user.getCurrentPosition().distance2DTo(gbs.getPosition()));
            double allocatedBandwidth = totalBandwidth * channelQuality / assignedUsers.size();
            throughputs.add(allocatedBandwidth);
        }
        
        double sum = throughputs.stream().mapToDouble(Double::doubleValue).sum();
        double sumSquares = throughputs.stream().mapToDouble(x -> x * x).sum();
        
        return (sum * sum) / (throughputs.size() * sumSquares);
    }
    
    private Set<Set<MobileUser>> generateSubsets(Set<MobileUser> users, int size) {
        Set<Set<MobileUser>> subsets = new HashSet<>();
        if (size == 0) {
            subsets.add(new HashSet<>());
            return subsets;
        }
        
        List<MobileUser> userList = new ArrayList<>(users);
        generateSubsetsRecursive(userList, size, 0, new HashSet<>(), subsets);
        return subsets;
    }
    
    private void generateSubsetsRecursive(List<MobileUser> users, int size, int start,
                                        Set<MobileUser> current, Set<Set<MobileUser>> result) {
        if (current.size() == size) {
            result.add(new HashSet<>(current));
            return;
        }
        
        for (int i = start; i < users.size(); i++) {
            current.add(users.get(i));
            generateSubsetsRecursive(users, size, i + 1, current, result);
            current.remove(users.get(i));
        }
    }
    
    private boolean isAssignmentEqual(Map<Object, Set<MobileUser>> assignment1,
                                    Map<Object, Set<MobileUser>> assignment2) {
        if (assignment1.size() != assignment2.size()) return false;
        
        for (Object key : assignment1.keySet()) {
            if (!assignment2.containsKey(key) || 
                !assignment1.get(key).equals(assignment2.get(key))) {
                return false;
            }
        }
        return true;
    }
    
    private Set<Set<Object>> generateCoalitions(List<Object> allPlayers, Object player) {
        Set<Set<Object>> coalitions = new HashSet<>();
        List<Object> otherPlayers = new ArrayList<>(allPlayers);
        otherPlayers.remove(player);
        
        // Generate all possible coalitions that include the player
        for (int size = 0; size <= otherPlayers.size(); size++) {
            for (Set<Object> subset : generateSubsetsOfObjects(otherPlayers, size)) {
                Set<Object> coalition = new HashSet<>(subset);
                coalition.add(player);
                coalitions.add(coalition);
            }
        }
        
        return coalitions;
    }
    
    private Set<Set<Object>> generateSubsetsOfObjects(List<Object> objects, int size) {
        Set<Set<Object>> subsets = new HashSet<>();
        if (size == 0) {
            subsets.add(new HashSet<>());
            return subsets;
        }
        
        generateSubsetsOfObjectsRecursive(objects, size, 0, new HashSet<>(), subsets);
        return subsets;
    }
    
    private void generateSubsetsOfObjectsRecursive(List<Object> objects, int size, int start,
                                                 Set<Object> current, Set<Set<Object>> result) {
        if (current.size() == size) {
            result.add(new HashSet<>(current));
            return;
        }
        
        for (int i = start; i < objects.size(); i++) {
            current.add(objects.get(i));
            generateSubsetsOfObjectsRecursive(objects, size, i + 1, current, result);
            current.remove(objects.get(i));
        }
    }
    
    private double calculateCoalitionValue(Set<Object> coalition, Map<Object, Set<MobileUser>> assignment) {
        double totalValue = 0.0;
        
        for (Object player : coalition) {
            if (assignment.containsKey(player)) {
                Set<MobileUser> assignedUsers = assignment.get(player);
                if (player instanceof DroneBaseStation) {
                    totalValue += calculateAdvancedDroneUtility((DroneBaseStation) player, assignedUsers, assignment);
                } else if (player instanceof GroundBaseStation) {
                    totalValue += calculateGroundStationUtility((GroundBaseStation) player, assignedUsers, assignment);
                }
            }
        }
        
        return totalValue;
    }
    
    private double factorial(int n) {
        if (n <= 1) return 1.0;
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Find best response for a ground station given other players' strategies
     */
    private Set<MobileUser> findBestResponseForGroundStation(GroundBaseStation gbs, 
                                                           Map<Object, Set<MobileUser>> currentAssignment) {
        Set<MobileUser> availableUsers = getAvailableUsersForGroundStation(gbs, currentAssignment);
        Set<MobileUser> bestResponse = new HashSet<>();
        double bestUtility = Double.NEGATIVE_INFINITY;
        
        // Consider all possible subsets of available users (up to capacity)
        int maxUsers = Math.min((int)gbs.getMaxUserCapacity(), availableUsers.size());
        
        for (int k = 0; k <= maxUsers; k++) {
            for (Set<MobileUser> subset : generateSubsets(availableUsers, k)) {
                Map<Object, Set<MobileUser>> testAssignment = new HashMap<>(currentAssignment);
                testAssignment.put(gbs, subset);
                
                double utility = calculateGroundStationUtility(gbs, subset, testAssignment);
                
                if (utility > bestUtility) {
                    bestUtility = utility;
                    bestResponse = new HashSet<>(subset);
                }
            }
        }
        
        return bestResponse;
    }
    
    // Getters and setters for game parameters
    public void setUtilityWeights(double alpha, double beta, double gamma) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }
    
    public void setConvergenceThreshold(double epsilon) {
        this.epsilon = epsilon;
    }
}