package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of baseline algorithms for comparison with game-theoretic approaches
 */
public class BaselineAlgorithms {
    
    private List<DroneBaseStation> droneStations;
    private List<GroundBaseStation> groundStations;
    private List<MobileUser> users;
    
    public BaselineAlgorithms(List<DroneBaseStation> droneStations,
                            List<GroundBaseStation> groundStations,
                            List<MobileUser> users) {
        this.droneStations = new ArrayList<>(droneStations);
        this.groundStations = new ArrayList<>(groundStations);
        this.users = new ArrayList<>(users);
    }
    
    /**
     * Random Assignment: Randomly assigns users to available base stations
     */
    public BaselineResult randomAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        Random random = new Random();
        
        for (MobileUser user : users) {
            List<Object> validStations = getValidStationsForUser(user);
            if (!validStations.isEmpty()) {
                Object selectedStation = validStations.get(random.nextInt(validStations.size()));
                assignments.computeIfAbsent(selectedStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new BaselineResult("Random Assignment", assignments, calculateUtility(assignments));
    }
    
    /**
     * Round Robin: Assigns users to stations in round-robin fashion
     */
    public BaselineResult roundRobinAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        List<Object> allStations = getAllStations();
        int stationIndex = 0;
        
        for (MobileUser user : users) {
            List<Object> validStations = getValidStationsForUser(user);
            if (!validStations.isEmpty()) {
                // Find next valid station in round-robin order
                Object selectedStation = null;
                for (int i = 0; i < allStations.size(); i++) {
                    Object station = allStations.get((stationIndex + i) % allStations.size());
                    if (validStations.contains(station) && hasCapacity(station, assignments)) {
                        selectedStation = station;
                        stationIndex = (stationIndex + i + 1) % allStations.size();
                        break;
                    }
                }
                
                if (selectedStation != null) {
                    assignments.computeIfAbsent(selectedStation, k -> new HashSet<>()).add(user);
                }
            }
        }
        
        return new BaselineResult("Round Robin", assignments, calculateUtility(assignments));
    }
    
    /**
     * Greedy Assignment: Assigns each user to the station with highest utility
     */
    public BaselineResult greedyAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object bestStation = null;
            double bestUtility = Double.NEGATIVE_INFINITY;
            
            for (Object station : getAllStations()) {
                if (isValidAssignment(user, station) && hasCapacity(station, assignments)) {
                    double utility = calculateUserStationUtility(user, station, assignments);
                    if (utility > bestUtility) {
                        bestUtility = utility;
                        bestStation = station;
                    }
                }
            }
            
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new BaselineResult("Greedy Assignment", assignments, calculateUtility(assignments));
    }
    
    /**
     * Nearest Neighbor: Assigns users to the geographically closest station
     */
    public BaselineResult nearestNeighborAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object nearestStation = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Object station : getAllStations()) {
                if (isValidAssignment(user, station) && hasCapacity(station, assignments)) {
                    double distance = calculateDistance(user, station);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestStation = station;
                    }
                }
            }
            
            if (nearestStation != null) {
                assignments.computeIfAbsent(nearestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new BaselineResult("Nearest Neighbor", assignments, calculateUtility(assignments));
    }
    
    /**
     * Load Balanced: Assigns users to minimize load imbalance across stations
     */
    public BaselineResult loadBalancedAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        // Sort users by data rate (descending) for better load balancing
        List<MobileUser> sortedUsers = users.stream()
                .sorted((u1, u2) -> Double.compare(u2.getDataRate(), u1.getDataRate()))
                .collect(Collectors.toList());
        
        for (MobileUser user : sortedUsers) {
            Object leastLoadedStation = null;
            double minLoad = Double.MAX_VALUE;
            
            for (Object station : getAllStations()) {
                if (isValidAssignment(user, station) && hasCapacity(station, assignments)) {
                    double currentLoad = calculateStationLoad(station, assignments);
                    if (currentLoad < minLoad) {
                        minLoad = currentLoad;
                        leastLoadedStation = station;
                    }
                }
            }
            
            if (leastLoadedStation != null) {
                assignments.computeIfAbsent(leastLoadedStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new BaselineResult("Load Balanced", assignments, calculateUtility(assignments));
    }
    
    /**
     * Signal Strength Based: Assigns users based on signal strength/SINR
     */
    public BaselineResult signalStrengthBasedAssignment() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object bestStation = null;
            double bestSignalStrength = Double.NEGATIVE_INFINITY;
            
            for (Object station : getAllStations()) {
                if (isValidAssignment(user, station) && hasCapacity(station, assignments)) {
                    double signalStrength = calculateSignalStrength(user, station);
                    if (signalStrength > bestSignalStrength) {
                        bestSignalStrength = signalStrength;
                        bestStation = station;
                    }
                }
            }
            
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new BaselineResult("Signal Strength Based", assignments, calculateUtility(assignments));
    }
    
    // Helper methods
    private void initializeAssignments(Map<Object, Set<MobileUser>> assignments) {
        droneStations.forEach(dbs -> assignments.put(dbs, new HashSet<>()));
        groundStations.forEach(gbs -> assignments.put(gbs, new HashSet<>()));
    }
    
    private List<Object> getAllStations() {
        List<Object> stations = new ArrayList<>();
        stations.addAll(droneStations);
        stations.addAll(groundStations);
        return stations;
    }
    
    private List<Object> getValidStationsForUser(MobileUser user) {
        return getAllStations().stream()
                .filter(station -> isValidAssignment(user, station))
                .collect(Collectors.toList());
    }
    
    private boolean isValidAssignment(MobileUser user, Object station) {
        if (station instanceof DroneBaseStation) {
            return ((DroneBaseStation) station).isUserInRange(user);
        } else if (station instanceof GroundBaseStation) {
            return ((GroundBaseStation) station).isUserInRange(user);
        }
        return false;
    }
    
    private boolean hasCapacity(Object station, Map<Object, Set<MobileUser>> assignments) {
        int currentUsers = assignments.getOrDefault(station, new HashSet<>()).size();
        
        if (station instanceof DroneBaseStation) {
            return currentUsers < ((DroneBaseStation) station).getMaxUserCapacity();
        } else if (station instanceof GroundBaseStation) {
            return currentUsers < ((GroundBaseStation) station).getMaxUserCapacity();
        }
        return false;
    }
    
    private double calculateDistance(MobileUser user, Object station) {
        Position3D userPos = user.getCurrentPosition();
        
        if (station instanceof DroneBaseStation) {
            return userPos.distanceTo(((DroneBaseStation) station).getCurrentPosition());
        } else if (station instanceof GroundBaseStation) {
            return userPos.distance2DTo(((GroundBaseStation) station).getPosition());
        }
        return Double.MAX_VALUE;
    }
    
    private double calculateStationLoad(Object station, Map<Object, Set<MobileUser>> assignments) {
        Set<MobileUser> assignedUsers = assignments.getOrDefault(station, new HashSet<>());
        return assignedUsers.stream().mapToDouble(MobileUser::getDataRate).sum();
    }
    
    private double calculateUserStationUtility(MobileUser user, Object station, 
                                             Map<Object, Set<MobileUser>> assignments) {
        double distance = calculateDistance(user, station);
        double load = calculateStationLoad(station, assignments);
        double signalStrength = calculateSignalStrength(user, station);
        
        // Combined utility: signal strength / (distance * load)
        return signalStrength / (1 + distance/1000.0 * (1 + load/1e6));
    }
    
    private double calculateSignalStrength(MobileUser user, Object station) {
        double distance = calculateDistance(user, station);
        double pathLoss = 20 * Math.log10(distance) + 20 * Math.log10(2.4e9) - 147.55; // Free space path loss
        return -pathLoss; // Higher is better
    }
    
    private double calculateUtility(Map<Object, Set<MobileUser>> assignments) {
        double totalUtility = 0.0;
        
        for (Map.Entry<Object, Set<MobileUser>> entry : assignments.entrySet()) {
            Object station = entry.getKey();
            Set<MobileUser> users = entry.getValue();
            
            if (!users.isEmpty()) {
                double stationUtility = users.stream()
                        .mapToDouble(user -> calculateUserStationUtility(user, station, assignments))
                        .average().orElse(0.0);
                totalUtility += stationUtility;
            }
        }
        
        return totalUtility;
    }
    
    /**
     * Result class for baseline algorithms
     */
    public static class BaselineResult {
        private final String algorithmName;
        private final Map<Object, Set<MobileUser>> assignments;
        private final double totalUtility;
        
        public BaselineResult(String algorithmName, Map<Object, Set<MobileUser>> assignments, double totalUtility) {
            this.algorithmName = algorithmName;
            this.assignments = new HashMap<>(assignments);
            this.totalUtility = totalUtility;
        }
        
        public String getAlgorithmName() { return algorithmName; }
        public Map<Object, Set<MobileUser>> getAssignments() { return assignments; }
        public double getTotalUtility() { return totalUtility; }
        
        public int getTotalAssignedUsers() {
            return assignments.values().stream().mapToInt(Set::size).sum();
        }
        
        public double getLoadBalanceIndex() {
            List<Integer> loads = assignments.values().stream()
                    .map(Set::size)
                    .collect(Collectors.toList());
            
            if (loads.isEmpty()) return 0.0;
            
            double mean = loads.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double variance = loads.stream()
                    .mapToDouble(load -> Math.pow(load - mean, 2))
                    .average().orElse(0.0);
            
            return Math.sqrt(variance) / mean; // Coefficient of variation
        }
    }
}