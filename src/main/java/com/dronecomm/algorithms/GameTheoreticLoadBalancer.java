package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;

import java.util.*;
import java.util.stream.Collectors;

public class GameTheoreticLoadBalancer {
    
    private List<DroneBaseStation> droneStations;
    private List<GroundBaseStation> groundStations;
    private List<MobileUser> users;
    
    private double cooperationWeight = 0.6;
    private double competitionWeight = 0.4;
    private double energyImportance = 0.3;
    private double qosImportance = 0.7;
    
    private GameType currentGameType = GameType.NASH_EQUILIBRIUM;
    private int maxIterations = 100;
    private double convergenceThreshold = 0.01;
    
    public enum GameType {
        NASH_EQUILIBRIUM,
        STACKELBERG_GAME,
        COOPERATIVE_GAME,
        AUCTION_BASED
    }
    
    public GameTheoreticLoadBalancer(List<DroneBaseStation> droneStations,
                                   List<GroundBaseStation> groundStations,
                                   List<MobileUser> users) {
        this.droneStations = new ArrayList<>(droneStations);
        this.groundStations = new ArrayList<>(groundStations);
        this.users = new ArrayList<>(users);
    }
    
    public LoadBalancingResult executeLoadBalancing() {
        switch (currentGameType) {
            case NASH_EQUILIBRIUM:
                return executeNashEquilibriumWithResearchAlgorithms();
            case STACKELBERG_GAME:
                return executeStackelbergGameWithResearchAlgorithms();
            case COOPERATIVE_GAME:
                return executeCooperativeGameWithResearchAlgorithms();
            case AUCTION_BASED:
                return executeAuctionBasedWithResearchAlgorithms();
            default:
                return executeNashEquilibriumWithResearchAlgorithms();
        }
    }
    
    private LoadBalancingResult executeNashEquilibriumWithResearchAlgorithms() {
        try {
            AGCTLBProblemFormulation.ProblemConstraints constraints = 
                new AGCTLBProblemFormulation.ProblemConstraints(
                    0, 5000, 0, 5000, 50, 300, 0.8,
                    createCapacityMap(), 1000, 
                    AlphaFairnessLoadBalancer.FairnessPolicy.PROPORTIONAL_FAIR
                );
            
            AGCTLBProblemFormulation.AGCTLBSolution result = 
                AGCTLBProblemFormulation.solveAGCTLB(droneStations, groundStations, users, constraints);
            
            // Convert user assignments format
            Map<Object, Set<MobileUser>> convertedAssignments = convertUserAssignments(result.userAssignments);
            
            return new LoadBalancingResult(convertedAssignments, 
                convertLoadsToUtilities(result.baseStationLoads), 1, result.feasible);
                
        } catch (Exception e) {
            return executeSimpleNashEquilibrium();
        }
    }
    
    private LoadBalancingResult executeStackelbergGameWithResearchAlgorithms() {
        try {
            // Initialize drone positions for game iterations
            Map<DroneBaseStation, Position3D> currentPositions = new HashMap<>();
            for (DroneBaseStation dbs : droneStations) {
                currentPositions.put(dbs, dbs.getCurrentPosition());
            }
            
            // Stackelberg iterations: Leader decides, followers respond
            double prevObjective = Double.POSITIVE_INFINITY;
            boolean gameConverged = false;
            int gameIterations = 0;
            final int MAX_STACKELBERG_ITERATIONS = 5; // Limit iterations
            
            for (gameIterations = 0; gameIterations < MAX_STACKELBERG_ITERATIONS && !gameConverged; gameIterations++) {
                // PHASE 1: Leader (Ground Stations) optimize user assignment
                PSCAAlgorithm.PSCAResult pscaResult = PSCAAlgorithm.solveUserAssociation(
                    droneStations, groundStations, users,
                    AlphaFairnessLoadBalancer.FairnessPolicy.LATENCY_OPTIMAL, 1000.0);
                
                double currentObjective = calculateStackelbergObjective(pscaResult.finalLoads);
                
                // PHASE 2: Followers (Drones) respond by repositioning
                boolean positionChanged = false;
                for (DroneBaseStation dbs : droneStations) {
                    Position3D newPosition = optimizeDronePositionForStackelberg(
                        dbs, currentPositions, groundStations, users, pscaResult.binaryAssignments);
                    
                    if (!newPosition.equals(currentPositions.get(dbs))) {
                        currentPositions.put(dbs, newPosition);
                        dbs.setCurrentPosition(newPosition);
                        positionChanged = true;
                    }
                }
                
                // Check convergence
                if (!positionChanged || Math.abs(prevObjective - currentObjective) < 1e-6) {
                    gameConverged = true;
                }
                prevObjective = currentObjective;
            }
            
            // Re-run P-SCA with optimized drone positions
            PSCAAlgorithm.PSCAResult finalResult = PSCAAlgorithm.solveUserAssociation(
                droneStations, groundStations, users,
                AlphaFairnessLoadBalancer.FairnessPolicy.LATENCY_OPTIMAL, 1000.0);
            
            return new LoadBalancingResult(finalResult.binaryAssignments,
                convertLoadsToUtilities(finalResult.finalLoads), gameIterations, gameConverged);
                
        } catch (Exception e) {
            return executeSimpleStackelbergGame();
        }
    }

    /**
     * Simple fallback implementation for Stackelberg Game load balancing.
     */
    private LoadBalancingResult executeSimpleStackelbergGame() {
        // Simple greedy assignment as a fallback
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        for (MobileUser user : users) {
            Object bestStation = findBestStationForUser(user, assignments);
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        Map<Object, Double> loads = calculateLoadsUsingAlphaFairness(assignments);
        return new LoadBalancingResult(assignments, convertLoadsToUtilities(loads), 1, false);
    }
    
    /**
     * Optimize drone position for Stackelberg follower phase
     * Given user assignment from GBS, find best position for this drone
     */
    private Position3D optimizeDronePositionForStackelberg(
            DroneBaseStation dbs,
            Map<DroneBaseStation, Position3D> currentPositions,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            Map<Object, Set<MobileUser>> userAssignments) {
        
        // Users assigned to this drone
        Set<MobileUser> assignedUsers = userAssignments.getOrDefault(dbs, new HashSet<>());
        if (assignedUsers.isEmpty()) {
            return currentPositions.get(dbs); // No optimization needed
        }
        
        // Search for position maximizing coverage of assigned users
        Position3D bestPosition = currentPositions.get(dbs);
        double bestCoverage = calculateDroneUsageCoverage(dbs, assignedUsers);
        
        // Sample nearby positions on grid
        List<Position3D> candidates = generateNearbyPositions(currentPositions.get(dbs), 200.0);
        
        for (Position3D candidate : candidates) {
            dbs.setCurrentPosition(candidate);
            double coverage = calculateDroneUsageCoverage(dbs, assignedUsers);
            if (coverage > bestCoverage) {
                bestCoverage = coverage;
                bestPosition = candidate;
            }
        }
        
        // Restore original position if no improvement
        dbs.setCurrentPosition(currentPositions.get(dbs));
        return bestPosition;
    }

    /**
     * Calculate how well drone covers its assigned users (coverage metric)
     */
    private double calculateDroneUsageCoverage(DroneBaseStation dbs, Set<MobileUser> users) {
        if (users.isEmpty()) return 0.0;
        
        double totalCoverage = 0.0;
        for (MobileUser user : users) {
            if (!dbs.isUserInRange(user)) {
                totalCoverage += 0.0; // User not in coverage
            } else {
                double distance = user.getCurrentPosition().distanceTo(dbs.getCurrentPosition());
                double maxDistance = dbs.getCurrentCoverageRadius();
                totalCoverage += 1.0 - (distance / maxDistance); // Coverage factor
            }
        }
        
        return totalCoverage / users.size();
    }

    /**
     * Calculate Stackelberg game objective (fairness-based)
     */
    private double calculateStackelbergObjective(Map<Object, Double> loads) {
        return AlphaFairnessLoadBalancer.calculateAlphaFairnessObjective(loads, 
            AlphaFairnessLoadBalancer.FairnessPolicy.LATENCY_OPTIMAL);
    }

    /**
     * Generate candidate positions near current position for local search
     */
    private List<Position3D> generateNearbyPositions(Position3D center, double radius) {
        List<Position3D> candidates = new ArrayList<>();
        
        // Add positions in grid around center
        double step = radius / 2;
        for (double dx = -radius; dx <= radius; dx += step) {
            for (double dy = -radius; dy <= radius; dy += step) {
                for (double dh = -100; dh <= 100; dh += 50) {
                    Position3D candidate = new Position3D(
                        center.getX() + dx,
                        center.getY() + dy,
                        center.getZ() + dh
                    );
                    candidates.add(candidate);
                }
            }
        }
        
        return candidates;
    }
    
    private LoadBalancingResult executeCooperativeGameWithResearchAlgorithms() {
        try {
            AlphaFairnessLoadBalancer.LoadBalancingResult alphResult = 
                AlphaFairnessLoadBalancer.optimizeLoadBalancing(
                    droneStations, groundStations, users,
                    AlphaFairnessLoadBalancer.FairnessPolicy.MIN_MAX, 1000.0, true);
            
            return new LoadBalancingResult(alphResult.assignments,
                convertLoadsToUtilities(alphResult.baseStationLoads), 1, true);
                
        } catch (Exception e) {
            return executeSimpleCooperativeGame();
        }
    }
    
    private LoadBalancingResult executeAuctionBasedWithResearchAlgorithms() {
        try {
            // Run VCG auction mechanism for truthful bidding
            VCGAuctionMechanism auctionMechanism = new VCGAuctionMechanism(
                droneStations, groundStations, users);
            
            VCGAuctionMechanism.VCGResult auctionResult = auctionMechanism.runAuction();
            
            // Convert to LoadBalancingResult format
            Map<Object, Double> utilities = new HashMap<>();
            for (Object station : auctionResult.assignments.keySet()) {
                utilities.put(station, 1.0 - calculateStationLoad(station, auctionResult.assignments));
            }
            
            // Calculate convergence based on revenue
            boolean converged = auctionResult.totalRevenue > 0;
            
            return new LoadBalancingResult(auctionResult.assignments, utilities, 1, converged);
            
        } catch (Exception e) {
            return executeAuctionBasedWithGreedyFallback();
        }
    }
    
    /**
     * Greedy fallback for auction mechanism
     */
    private LoadBalancingResult executeAuctionBasedWithGreedyFallback() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object bestStation = findBestStationUsingAFRelay(user);
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        Map<Object, Double> loads = calculateLoadsUsingAlphaFairness(assignments);
        return new LoadBalancingResult(assignments, convertLoadsToUtilities(loads), 1, true);
    }
    
    private Object findBestStationUsingAFRelay(MobileUser user) {
        double bestRate = 0.0;
        Object bestStation = null;
        
        for (DroneBaseStation dbs : droneStations) {
            if (!dbs.isUserInRange(user)) continue;
            
            for (GroundBaseStation gbs : groundStations) {
                AFRelayModel.AFRelayResult relayResult = AFRelayModel.calculateAFRelayRate(
                    user, dbs, gbs, 1.0, 5.0, dbs.getBandwidth());
                
                if (relayResult.totalRate > bestRate) {
                    bestRate = relayResult.totalRate;
                    bestStation = dbs;
                }
            }
        }
        
        for (GroundBaseStation gbs : groundStations) {
            if (gbs.isUserInRange(user)) {
                double channelGain = A2GChannelModel.calculateUEToMBSChannelGain(user, gbs);
                double snr = A2GChannelModel.calculateSNR(1.0, channelGain, gbs.getBandwidth());
                double rate = gbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
                
                if (rate > bestRate) {
                    bestRate = rate;
                    bestStation = gbs;
                }
            }
        }
        
        return bestStation;
    }
    
    private Map<Object, Double> calculateLoadsUsingAlphaFairness(Map<Object, Set<MobileUser>> assignments) {
        Map<Object, Double> loads = new HashMap<>();
        
        for (Map.Entry<Object, Set<MobileUser>> entry : assignments.entrySet()) {
            Object station = entry.getKey();
            Set<MobileUser> users = entry.getValue();
            
            double load = AlphaFairnessLoadBalancer.calculateTrafficLoad(
                station, users, 1000.0, station instanceof DroneBaseStation);
            loads.put(station, load);
        }
        
        return loads;
    }
    
    private Map<Object, Double> convertLoadsToUtilities(Map<Object, Double> loads) {
        Map<Object, Double> utilities = new HashMap<>();
        
        for (Map.Entry<Object, Double> entry : loads.entrySet()) {
            double load = entry.getValue();
            double utility = Math.max(0.0, 1.0 - load);
            utilities.put(entry.getKey(), utility);
        }
        
        return utilities;
    }
    
    private Map<Object, Set<MobileUser>> convertUserAssignments(Map<MobileUser, Object> userAssignments) {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        
        // Initialize empty sets for all stations
        for (DroneBaseStation dbs : droneStations) {
            assignments.put(dbs, new HashSet<>());
        }
        for (GroundBaseStation gbs : groundStations) {
            assignments.put(gbs, new HashSet<>());
        }
        
        // Convert assignments
        for (Map.Entry<MobileUser, Object> entry : userAssignments.entrySet()) {
            Object station = entry.getValue();
            assignments.computeIfAbsent(station, k -> new HashSet<>()).add(entry.getKey());
        }
        
        return assignments;
    }
    
    private LoadBalancingResult executeSimpleNashEquilibrium() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object bestStation = findBestStationForUser(user, assignments);
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        Map<Object, Double> loads = calculateLoadsUsingAlphaFairness(assignments);
        return new LoadBalancingResult(assignments, convertLoadsToUtilities(loads), 1, false);
    }
    
    private LoadBalancingResult executeSimpleCooperativeGame() {
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        initializeAssignments(assignments);
        
        for (MobileUser user : users) {
            Object bestStation = findBestStationForUser(user, assignments);
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        Map<Object, Double> loads = calculateLoadsUsingAlphaFairness(assignments);
        return new LoadBalancingResult(assignments, convertLoadsToUtilities(loads), 1, false);
    }
    
    private Map<Object, Integer> createCapacityMap() {
        Map<Object, Integer> capacities = new HashMap<>();
        
        for (DroneBaseStation dbs : droneStations) {
            capacities.put(dbs, (int) dbs.getMaxUserCapacity());
        }
        
        for (GroundBaseStation gbs : groundStations) {
            capacities.put(gbs, (int) gbs.getMaxUserCapacity());
        }
        
        return capacities;
    }
    
    /**
     * Updates ground station strategy in Nash equilibrium
     */
    private void updateGroundStationStrategy(GroundBaseStation gbs,
                                           Map<Object, Set<MobileUser>> assignments,
                                           Map<Object, Double> utilities) {
        Set<MobileUser> currentUsers = assignments.getOrDefault(gbs, new HashSet<>());
        Set<MobileUser> bestAssignment = new HashSet<>(currentUsers);
        double bestUtility = calculateGroundStationUtility(gbs, currentUsers);
        
        // Consider collaboration with nearby drones
        List<DroneBaseStation> nearbyDrones = droneStations.stream()
            .filter(dbs -> isInCollaborationRange(gbs, dbs))
            .collect(Collectors.toList());
        
        // Optimize user assignment considering collaboration
        List<MobileUser> candidateUsers = users.stream()
            .filter(user -> gbs.isUserInRange(user))
            .collect(Collectors.toList());
        
        // Simple greedy optimization for ground station
        Set<MobileUser> optimizedAssignment = optimizeGroundStationAssignment(
            gbs, candidateUsers, nearbyDrones);
        
        double newUtility = calculateGroundStationUtility(gbs, optimizedAssignment);
        if (newUtility > bestUtility) {
            bestAssignment = optimizedAssignment;
            bestUtility = newUtility;
        }
        
        assignments.put(gbs, bestAssignment);
        utilities.put(gbs, bestUtility);
    }
    
    /**
     * Calculate the total data load on a base station
     */
    private double calculateStationLoad(Object station, Map<Object, Set<MobileUser>> assignments) {
        Set<MobileUser> assignedUsers = assignments.getOrDefault(station, new HashSet<>());
        return assignedUsers.stream().mapToDouble(MobileUser::getDataRate).sum();
    }
    
    /**
     * Calculates utility for a drone base station
     */
    private double calculateDroneUtility(DroneBaseStation dbs, Set<MobileUser> users) {
        if (users.isEmpty()) {
            return 0.0;
        }
        
        // Load balancing factor
        double loadFactor = 1.0 - (double) users.size() / dbs.getMaxUserCapacity();
        
        // Energy efficiency factor
        double energyFactor = dbs.getEnergyPercentage() / 100.0;
        
        // QoS satisfaction factor
        double qosFactor = users.stream()
            .mapToDouble(user -> calculateUserQoSSatisfaction(user, dbs))
            .average()
            .orElse(0.0);
        
        // Coverage efficiency
        double coverageFactor = users.stream()
            .mapToDouble(user -> 1.0 - (dbs.getCurrentPosition().distanceTo(user.getCurrentPosition()) 
                                      / dbs.getCurrentCoverageRadius()))
            .average()
            .orElse(0.0);
        
        return (loadFactor * 0.3) + (energyFactor * energyImportance) + 
               (qosFactor * qosImportance) + (coverageFactor * 0.2);
    }
    
    /**
     * Calculates utility for a ground base station
     */
    private double calculateGroundStationUtility(GroundBaseStation gbs, Set<MobileUser> users) {
        if (users.isEmpty()) {
            return 0.0;
        }
        
        // Load balancing factor
        double loadFactor = 1.0 - (double) users.size() / gbs.getMaxUserCapacity();
        
        // QoS satisfaction factor
        double qosFactor = users.stream()
            .mapToDouble(user -> calculateUserQoSSatisfaction(user, gbs))
            .average()
            .orElse(0.0);
        
        // Collaboration bonus
        double collaborationBonus = gbs.getCollaboratingDrones().size() * 0.1;
        
        // Coverage efficiency
        double coverageFactor = users.stream()
            .mapToDouble(user -> 1.0 - (gbs.getPosition().distance2DTo(user.getCurrentPosition()) 
                                      / gbs.getCoverageRadius()))
            .average()
            .orElse(0.0);
        
        return (loadFactor * 0.4) + (qosFactor * qosImportance) + 
               (collaborationBonus * 0.2) + (coverageFactor * 0.2);
    }
    
    /**
     * Calculates QoS satisfaction for a user connected to a base station
     */
    private double calculateUserQoSSatisfaction(MobileUser user, Object baseStation) {
        double distance;
        double maxThroughput;
        
        if (baseStation instanceof DroneBaseStation) {
            DroneBaseStation dbs = (DroneBaseStation) baseStation;
            distance = user.getCurrentPosition().distanceTo(dbs.getCurrentPosition());
            maxThroughput = dbs.getBandwidth() / Math.max(1, dbs.getCurrentConnectedUserCount());
        } else if (baseStation instanceof GroundBaseStation) {
            GroundBaseStation gbs = (GroundBaseStation) baseStation;
            distance = user.getCurrentPosition().distance2DTo(gbs.getPosition());
            maxThroughput = gbs.getBandwidth() / Math.max(1, gbs.getCurrentConnectedUserCount());
        } else {
            return 0.0;
        }
        
        // Simple signal-to-noise ratio model
        double signalStrength = 1.0 / (1.0 + distance / 1000.0); // Normalize distance
        double estimatedLatency = distance / 300000000.0 * 1000 + 10; // Speed of light + processing
        
        double latencySatisfaction = estimatedLatency <= user.getMaxAcceptableLatency() ? 1.0 :
            Math.max(0.0, 1.0 - ((estimatedLatency - user.getMaxAcceptableLatency()) 
                               / user.getMaxAcceptableLatency()));
        
        double throughputSatisfaction = maxThroughput >= user.getMinRequiredThroughput() ? 1.0 :
            Math.max(0.0, maxThroughput / user.getMinRequiredThroughput());
        
        return (latencySatisfaction * 0.5) + (throughputSatisfaction * 0.3) + (signalStrength * 0.2);
    }
    
    /**
     * Solves optimal assignment problem using linear programming
     */
    private OptimalAssignmentResult solveOptimalAssignment() {
        // This is a simplified version - in practice, you'd use a more sophisticated solver
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        
        // Initialize assignments
        initializeAssignments(assignments);
        
        // Greedy optimization for demonstration
        for (MobileUser user : users) {
            Object bestStation = findBestStationForUser(user, assignments);
            if (bestStation != null) {
                assignments.computeIfAbsent(bestStation, k -> new HashSet<>()).add(user);
            }
        }
        
        return new OptimalAssignmentResult(assignments, calculateSystemUtility(assignments));
    }
    
    private Object findBestStationForUser(MobileUser user, Map<Object, Set<MobileUser>> assignments) {
        Object bestStation = null;
        double bestUtility = 0.0;
        
        // Check drone stations
        for (DroneBaseStation dbs : droneStations) {
            if (dbs.isUserInRange(user) && 
                assignments.getOrDefault(dbs, new HashSet<>()).size() < dbs.getMaxUserCapacity()) {
                double utility = calculateUserQoSSatisfaction(user, dbs);
                if (utility > bestUtility) {
                    bestUtility = utility;
                    bestStation = dbs;
                }
            }
        }
        
        // Check ground stations
        for (GroundBaseStation gbs : groundStations) {
            if (gbs.isUserInRange(user) && 
                assignments.getOrDefault(gbs, new HashSet<>()).size() < gbs.getMaxUserCapacity()) {
                double utility = calculateUserQoSSatisfaction(user, gbs);
                if (utility > bestUtility) {
                    bestUtility = utility;
                    bestStation = gbs;
                }
            }
        }
        
        return bestStation;
    }
    
    // Helper methods
    private void initializeAssignments(Map<Object, Set<MobileUser>> assignments) {
        droneStations.forEach(dbs -> assignments.put(dbs, new HashSet<>()));
        groundStations.forEach(gbs -> assignments.put(gbs, new HashSet<>()));
    }
    
    private double calculateSystemUtility(Map<Object, Set<MobileUser>> assignments) {
        double totalUtility = 0.0;
        
        for (DroneBaseStation dbs : droneStations) {
            totalUtility += calculateDroneUtility(dbs, assignments.getOrDefault(dbs, new HashSet<>()));
        }
        
        for (GroundBaseStation gbs : groundStations) {
            totalUtility += calculateGroundStationUtility(gbs, assignments.getOrDefault(gbs, new HashSet<>()));
        }
        
        return totalUtility;
    }
    
    private boolean isInCollaborationRange(GroundBaseStation gbs, DroneBaseStation dbs) {
        return gbs.getPosition().distanceTo(dbs.getCurrentPosition()) <= 2000; // 2km collaboration range
    }
    
    // Placeholder implementations for complex methods
    private void optimizeGroundStationAsLeader(GroundBaseStation gbs, 
                                             Map<Object, Set<MobileUser>> assignments,
                                             Map<Object, Double> utilities) {
        updateGroundStationStrategy(gbs, assignments, utilities);
    }
    
    private void optimizeDroneAsFollower(DroneBaseStation dbs,
                                       Map<Object, Set<MobileUser>> assignments,
                                       Map<Object, Double> utilities) {
        updateDroneStrategy(dbs, assignments, utilities);
    }
    
    private void updateDroneStrategy(DroneBaseStation dbs,
                                   Map<Object, Set<MobileUser>> assignments,
                                   Map<Object, Double> utilities) {
        Set<MobileUser> currentUsers = assignments.getOrDefault(dbs, new HashSet<>());
        Set<MobileUser> bestAssignment = new HashSet<>(currentUsers);
        double bestUtility = calculateDroneUtility(dbs, currentUsers);
        
        // Simple optimization for drone strategy
        List<MobileUser> candidateUsers = users.stream()
            .filter(user -> dbs.isUserInRange(user))
            .collect(Collectors.toList());
        
        for (MobileUser user : candidateUsers) {
            Set<MobileUser> testAssignment = new HashSet<>(currentUsers);
            testAssignment.add(user);
            
            if (testAssignment.size() <= dbs.getMaxUserCapacity()) {
                double newUtility = calculateDroneUtility(dbs, testAssignment);
                if (newUtility > bestUtility) {
                    bestAssignment = testAssignment;
                    bestUtility = newUtility;
                }
            }
        }
        
        assignments.put(dbs, bestAssignment);
        utilities.put(dbs, bestUtility);
    }
    
    private void refineUserAssignments(Map<Object, Set<MobileUser>> assignments,
                                     Map<Object, Double> utilities) {
        // Implement user preference-based refinement
    }
    
    private Map<Object, Double> calculateShapleyValues(OptimalAssignmentResult result) {
        Map<Object, Double> shapleyValues = new HashMap<>();
        double totalUtility = result.getTotalUtility();
        
        // Simplified Shapley value calculation
        for (DroneBaseStation dbs : droneStations) {
            shapleyValues.put(dbs, totalUtility / (droneStations.size() + groundStations.size()));
        }
        
        for (GroundBaseStation gbs : groundStations) {
            shapleyValues.put(gbs, totalUtility / (droneStations.size() + groundStations.size()));
        }
        
        return shapleyValues;
    }
    
    private void conductUserAuction(MobileUser user,
                                  Map<Object, Set<MobileUser>> assignments,
                                  Map<Object, Double> utilities) {
        // Implement auction mechanism for user assignment
        Object bestBidder = findBestStationForUser(user, assignments);
        if (bestBidder != null) {
            assignments.computeIfAbsent(bestBidder, k -> new HashSet<>()).add(user);
        }
    }
    
    private Set<MobileUser> optimizeGroundStationAssignment(GroundBaseStation gbs,
                                                          List<MobileUser> candidates,
                                                          List<DroneBaseStation> collaboratingDrones) {
        // Implement optimization considering collaboration
        return candidates.stream()
            .filter(user -> gbs.isUserInRange(user))
            .limit(gbs.getMaxUserCapacity())
            .collect(Collectors.toSet());
    }
    
    private List<Set<MobileUser>> generateCombinations(List<MobileUser> users, int size) {
        List<Set<MobileUser>> combinations = new ArrayList<>();
        if (size == 0) {
            combinations.add(new HashSet<>());
            return combinations;
        }
        
        // Generate limited combinations to avoid exponential complexity
        if (users.size() <= 10) { // Only for small sets
            generateCombinationsRecursive(users, size, 0, new HashSet<>(), combinations);
        } else {
            // For larger sets, use greedy selection
            combinations.add(users.stream().limit(size).collect(Collectors.toSet()));
        }
        
        return combinations;
    }
    
    private void generateCombinationsRecursive(List<MobileUser> users, int size, int start,
                                             Set<MobileUser> current, List<Set<MobileUser>> result) {
        if (current.size() == size) {
            result.add(new HashSet<>(current));
            return;
        }
        
        for (int i = start; i < users.size(); i++) {
            current.add(users.get(i));
            generateCombinationsRecursive(users, size, i + 1, current, result);
            current.remove(users.get(i));
        }
    }
    
    // Getters and Setters
    public GameType getCurrentGameType() { return currentGameType; }
    public void setCurrentGameType(GameType gameType) { this.currentGameType = gameType; }
    
    public void setCooperationWeight(double weight) { this.cooperationWeight = weight; }
    public void setCompetitionWeight(double weight) { this.competitionWeight = weight; }
    public void setEnergyImportance(double importance) { this.energyImportance = importance; }
    public void setQosImportance(double importance) { this.qosImportance = importance; }
    
    /**
     * Result class for load balancing operations
     */
    public static class LoadBalancingResult {
        private final Map<Object, Set<MobileUser>> assignments;
        private final Map<Object, Double> utilities;
        private final int iterations;
        private final boolean converged;
        
        public LoadBalancingResult(Map<Object, Set<MobileUser>> assignments,
                                 Map<Object, Double> utilities,
                                 int iterations, boolean converged) {
            this.assignments = new HashMap<>(assignments);
            this.utilities = new HashMap<>(utilities);
            this.iterations = iterations;
            this.converged = converged;
        }
        
        public Map<Object, Set<MobileUser>> getAssignments() { return assignments; }
        public Map<Object, Double> getUtilities() { return utilities; }
        public int getIterations() { return iterations; }
        public boolean isConverged() { return converged; }
        
        public double getTotalUtility() {
            return utilities.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }
    
    /**
     * Result class for optimal assignment
     */
    private static class OptimalAssignmentResult {
        private final Map<Object, Set<MobileUser>> assignments;
        private final double totalUtility;
        
        public OptimalAssignmentResult(Map<Object, Set<MobileUser>> assignments, double totalUtility) {
            this.assignments = assignments;
            this.totalUtility = totalUtility;
        }
        
        public Map<Object, Set<MobileUser>> getAssignments() { return assignments; }
        public double getTotalUtility() { return totalUtility; }
    }
}