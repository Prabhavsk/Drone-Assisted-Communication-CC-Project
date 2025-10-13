package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;
import com.dronecomm.algorithms.AlphaFairnessLoadBalancer.FairnessPolicy;

import java.util.*;

/**
 * AGC-TLB Problem Formulation Implementation
 * 
 * This class implements the complete AGC-TLB (Air-Ground Collaborative Traffic Load Balancing)
 * problem formulation from the research paper:
 * 
 * Problem (10):
 * min phialpha(rho) subject to:
 * - Constraint (10b): Sum xij = 1, xij in {0,1}, foralli in I (each UE to one BS)
 * - Constraint (10c): Sum xij <= Nj, forallj in J^s (capacity constraints)
 * - Constraint (10d): 0 <= rhoj <= rhomax, forallj in J^s (load thresholds)
 * - Constraint (10e): qj in F, forallj in J (deployment region)
 * 
 * Where:
 * - Q := {qj, j in J} (DBS positions)
 * - X := {xij, i in I, j in J^s} (user associations)
 * - F = [xmin, xmax] * [ymin, ymax] * [hmin, hmax] (feasible region)
 */
public class AGCTLBProblemFormulation {
    
    /**
     * Problem parameters and constraints
     */
    public static class ProblemConstraints {
        // Deployment region F
        public final double xMin, xMax, yMin, yMax, hMin, hMax;
        
        // Load constraints
        public final double maxLoad;           // rhomax threshold
        
        // Capacity constraints
        public final Map<Object, Integer> maxUserCapacities; // Nj for each BS
        
        // Network parameters
        public final double meanPacketSize;    // mu (bits)
        public final FairnessPolicy policy;    // alpha-fairness policy
        
        public ProblemConstraints(double xMin, double xMax, double yMin, double yMax,
                                double hMin, double hMax, double maxLoad,
                                Map<Object, Integer> capacities, double packetSize,
                                FairnessPolicy policy) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
            this.hMin = hMin;
            this.hMax = hMax;
            this.maxLoad = maxLoad;
            this.maxUserCapacities = new HashMap<>(capacities);
            this.meanPacketSize = packetSize;
            this.policy = policy;
        }
    }
    
    /**
     * Complete AGC-TLB solution
     */
    public static class AGCTLBSolution {
        public final Map<DroneBaseStation, Position3D> dronePositions;     // Q (DBS positions)
        public final Map<MobileUser, Object> userAssignments;              // X (user associations)
        public final Map<Object, Double> baseStationLoads;                 // rho (load distribution)
        public final double objectiveValue;                                // phialpha(rho)
        public final boolean feasible;                                     // Constraint satisfaction
        public final String infeasibilityReason;                          // If infeasible
        public final ConstraintViolations violations;                      // Constraint violations
        
        public AGCTLBSolution(Map<DroneBaseStation, Position3D> positions,
                             Map<MobileUser, Object> assignments,
                             Map<Object, Double> loads, double objective,
                             boolean feasible, String reason,
                             ConstraintViolations violations) {
            this.dronePositions = new HashMap<>(positions);
            this.userAssignments = new HashMap<>(assignments);
            this.baseStationLoads = new HashMap<>(loads);
            this.objectiveValue = objective;
            this.feasible = feasible;
            this.infeasibilityReason = reason;
            this.violations = violations;
        }
    }
    
    /**
     * Constraint violation tracking
     */
    public static class ConstraintViolations {
        public final List<String> assignmentViolations;    // Constraint (10b) violations
        public final List<String> capacityViolations;      // Constraint (10c) violations
        public final List<String> loadViolations;          // Constraint (10d) violations
        public final List<String> deploymentViolations;    // Constraint (10e) violations
        
        public ConstraintViolations() {
            this.assignmentViolations = new ArrayList<>();
            this.capacityViolations = new ArrayList<>();
            this.loadViolations = new ArrayList<>();
            this.deploymentViolations = new ArrayList<>();
        }
        
        public boolean hasViolations() {
            return !assignmentViolations.isEmpty() || !capacityViolations.isEmpty() ||
                   !loadViolations.isEmpty() || !deploymentViolations.isEmpty();
        }
        
        public int getTotalViolations() {
            return assignmentViolations.size() + capacityViolations.size() +
                   loadViolations.size() + deploymentViolations.size();
        }
    }
    
    /**
     * Solve the complete AGC-TLB problem using alternating optimization
     * Algorithm 1 from the research paper
     */
    public static AGCTLBSolution solveAGCTLB(
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            ProblemConstraints constraints) {
        
        List<Object> allBaseStations = new ArrayList<>();
        allBaseStations.addAll(droneStations);
        allBaseStations.addAll(groundStations);
        
        // Initialize with feasible positions and assignments
        Map<DroneBaseStation, Position3D> currentPositions = initializeFeasiblePositions(droneStations, constraints);
        Map<MobileUser, Object> currentAssignments = initializeFeasibleAssignments(users, allBaseStations, constraints);
        
        double prevObjective = Double.POSITIVE_INFINITY;
        boolean converged = false;
        int maxIterations = 100;
        double tolerance = 1e-6;
        
        // Alternating optimization main loop (Algorithm 1)
        for (int iter = 0; iter < maxIterations && !converged; iter++) {
            
            // Step 1: Update user association X with fixed drone positions Q
            PSCAAlgorithm.PSCAResult userAssocResult = PSCAAlgorithm.solveUserAssociation(
                droneStations, groundStations, users, constraints.policy, constraints.meanPacketSize);
            
            currentAssignments = convertToUserAssignments(userAssocResult.binaryAssignments, users);
            
            // Step 2: Update drone deployment Q with fixed user associations X
            ExactPotentialGame potentialGame = new ExactPotentialGame(
                constraints.xMin, constraints.xMax, constraints.yMin, constraints.yMax,
                constraints.hMin, constraints.hMax);
            
            ExactPotentialGame.PotentialGameResult deploymentResult = potentialGame.solveDeployment(
                droneStations, groundStations, users, constraints.policy, constraints.meanPacketSize);
            
            currentPositions = deploymentResult.finalState.positions;
            
            // Update drone positions in the objects
            for (DroneBaseStation dbs : droneStations) {
                dbs.setCurrentPosition(currentPositions.get(dbs));
            }
            
            // Calculate current objective
            Map<Object, Double> currentLoads = calculateLoads(currentAssignments, allBaseStations, constraints.meanPacketSize);
            double currentObjective = AlphaFairnessLoadBalancer.calculateAlphaFairnessObjective(currentLoads, constraints.policy);
            
            // Check convergence
            if (Math.abs(prevObjective - currentObjective) < tolerance) {
                converged = true;
            }
            prevObjective = currentObjective;
        }
        
        // Calculate final solution and validate constraints
        Map<Object, Double> finalLoads = calculateLoads(currentAssignments, allBaseStations, constraints.meanPacketSize);
        double finalObjective = AlphaFairnessLoadBalancer.calculateAlphaFairnessObjective(finalLoads, constraints.policy);
        
        // Validate all constraints
        ConstraintViolations violations = validateConstraints(currentPositions, currentAssignments, 
                                                            finalLoads, droneStations, allBaseStations, constraints);
        
        boolean feasible = !violations.hasViolations();
        String infeasibilityReason = feasible ? null : generateInfeasibilityReport(violations);
        
        return new AGCTLBSolution(currentPositions, currentAssignments, finalLoads, 
                                finalObjective, feasible, infeasibilityReason, violations);
    }
    
    /**
     * Initialize feasible drone positions within deployment region F
     */
    private static Map<DroneBaseStation, Position3D> initializeFeasiblePositions(
            List<DroneBaseStation> droneStations, ProblemConstraints constraints) {
        
        Map<DroneBaseStation, Position3D> positions = new HashMap<>();
        Random random = new Random();
        
        for (DroneBaseStation dbs : droneStations) {
            // Generate random position within feasible region F
            double x = constraints.xMin + random.nextDouble() * (constraints.xMax - constraints.xMin);
            double y = constraints.yMin + random.nextDouble() * (constraints.yMax - constraints.yMin);
            double h = constraints.hMin + random.nextDouble() * (constraints.hMax - constraints.hMin);
            
            Position3D feasiblePos = new Position3D(x, y, h);
            positions.put(dbs, feasiblePos);
            dbs.setCurrentPosition(feasiblePos);
        }
        
        return positions;
    }
    
    /**
     * Initialize feasible user assignments satisfying capacity constraints
     */
    private static Map<MobileUser, Object> initializeFeasibleAssignments(
            List<MobileUser> users, List<Object> baseStations, ProblemConstraints constraints) {
        
        Map<MobileUser, Object> assignments = new HashMap<>();
        Map<Object, Integer> currentCapacity = new HashMap<>();
        
        // Initialize capacity counters
        for (Object bs : baseStations) {
            currentCapacity.put(bs, 0);
        }
        
        // Assign users while respecting capacity constraints
        for (MobileUser user : users) {
            Object assignedBS = null;
            
            // Find a base station with available capacity
            for (Object bs : baseStations) {
                int maxCap = constraints.maxUserCapacities.getOrDefault(bs, 50);
                if (currentCapacity.get(bs) < maxCap) {
                    assignedBS = bs;
                    break;
                }
            }
            
            // If no capacity available, assign to least loaded
            if (assignedBS == null) {
                assignedBS = baseStations.stream()
                    .min(Comparator.comparingInt(currentCapacity::get))
                    .orElse(baseStations.get(0));
            }
            
            assignments.put(user, assignedBS);
            currentCapacity.put(assignedBS, currentCapacity.get(assignedBS) + 1);
        }
        
        return assignments;
    }
    
    /**
     * Convert base station assignments to user assignments
     */
    private static Map<MobileUser, Object> convertToUserAssignments(
            Map<Object, Set<MobileUser>> bsAssignments, List<MobileUser> users) {
        
        Map<MobileUser, Object> userAssignments = new HashMap<>();
        
        for (Map.Entry<Object, Set<MobileUser>> entry : bsAssignments.entrySet()) {
            Object bs = entry.getKey();
            for (MobileUser user : entry.getValue()) {
                userAssignments.put(user, bs);
            }
        }
        
        // Ensure all users are assigned
        for (MobileUser user : users) {
            if (!userAssignments.containsKey(user)) {
                // Assign to first available base station
                Object firstBS = bsAssignments.keySet().iterator().next();
                userAssignments.put(user, firstBS);
            }
        }
        
        return userAssignments;
    }
    
    /**
     * Calculate load distribution from user assignments
     */
    private static Map<Object, Double> calculateLoads(
            Map<MobileUser, Object> assignments, List<Object> baseStations, double meanPacketSize) {
        
        Map<Object, Set<MobileUser>> bsAssignments = new HashMap<>();
        for (Object bs : baseStations) {
            bsAssignments.put(bs, new HashSet<>());
        }
        
        for (Map.Entry<MobileUser, Object> entry : assignments.entrySet()) {
            bsAssignments.get(entry.getValue()).add(entry.getKey());
        }
        
        Map<Object, Double> loads = new HashMap<>();
        for (Object bs : baseStations) {
            double load = AlphaFairnessLoadBalancer.calculateTrafficLoad(
                bs, bsAssignments.get(bs), meanPacketSize, false);
            loads.put(bs, load);
        }
        
        return loads;
    }
    
    /**
     * Validate all AGC-TLB constraints
     */
    private static ConstraintViolations validateConstraints(
            Map<DroneBaseStation, Position3D> positions,
            Map<MobileUser, Object> assignments,
            Map<Object, Double> loads,
            List<DroneBaseStation> droneStations,
            List<Object> allBaseStations,
            ProblemConstraints constraints) {
        
        ConstraintViolations violations = new ConstraintViolations();
        
        // Constraint (10b): Each UE assigned to exactly one BS
        validateAssignmentConstraints(assignments, violations);
        
        // Constraint (10c): Capacity constraints
        validateCapacityConstraints(assignments, allBaseStations, constraints, violations);
        
        // Constraint (10d): Load threshold constraints
        validateLoadConstraints(loads, constraints, violations);
        
        // Constraint (10e): Deployment region constraints
        validateDeploymentConstraints(positions, constraints, violations);
        
        return violations;
    }
    
    /**
     * Validate constraint (10b): Sum xij = 1, xij in {0,1}
     */
    private static void validateAssignmentConstraints(
            Map<MobileUser, Object> assignments, ConstraintViolations violations) {
        
        for (MobileUser user : assignments.keySet()) {
            if (assignments.get(user) == null) {
                violations.assignmentViolations.add(
                    "User " + user.getId() + " is not assigned to any base station");
            }
        }
    }
    
    /**
     * Validate constraint (10c): Sum xij <= Nj
     */
    private static void validateCapacityConstraints(
            Map<MobileUser, Object> assignments, List<Object> baseStations,
            ProblemConstraints constraints, ConstraintViolations violations) {
        
        Map<Object, Integer> currentLoads = new HashMap<>();
        for (Object bs : baseStations) {
            currentLoads.put(bs, 0);
        }
        
        for (Object bs : assignments.values()) {
            currentLoads.put(bs, currentLoads.get(bs) + 1);
        }
        
        for (Object bs : baseStations) {
            int maxCap = constraints.maxUserCapacities.getOrDefault(bs, 50);
            int currentLoad = currentLoads.get(bs);
            
            if (currentLoad > maxCap) {
                violations.capacityViolations.add(
                    "Base station " + bs + " exceeds capacity: " + currentLoad + " > " + maxCap);
            }
        }
    }
    
    /**
     * Validate constraint (10d): 0 <= rhoj <= rhomax
     */
    private static void validateLoadConstraints(
            Map<Object, Double> loads, ProblemConstraints constraints, ConstraintViolations violations) {
        
        for (Map.Entry<Object, Double> entry : loads.entrySet()) {
            Object bs = entry.getKey();
            double load = entry.getValue();
            
            if (load < 0 || load > constraints.maxLoad) {
                violations.loadViolations.add(
                    "Base station " + bs + " load violation: " + load + " not in [0, " + constraints.maxLoad + "]");
            }
        }
    }
    
    /**
     * Validate constraint (10e): qj in F
     */
    private static void validateDeploymentConstraints(
            Map<DroneBaseStation, Position3D> positions, ProblemConstraints constraints, 
            ConstraintViolations violations) {
        
        for (Map.Entry<DroneBaseStation, Position3D> entry : positions.entrySet()) {
            DroneBaseStation dbs = entry.getKey();
            Position3D pos = entry.getValue();
            
            if (pos.getX() < constraints.xMin || pos.getX() > constraints.xMax ||
                pos.getY() < constraints.yMin || pos.getY() > constraints.yMax ||
                pos.getZ() < constraints.hMin || pos.getZ() > constraints.hMax) {
                
                violations.deploymentViolations.add(
                    "DBS " + dbs.getId() + " position " + pos + " outside feasible region F");
            }
        }
    }
    
    /**
     * Generate infeasibility report
     */
    private static String generateInfeasibilityReport(ConstraintViolations violations) {
        StringBuilder report = new StringBuilder();
        report.append("AGC-TLB Problem Infeasible:\n");
        
        if (!violations.assignmentViolations.isEmpty()) {
            report.append("Assignment violations (10b):\n");
            violations.assignmentViolations.forEach(v -> report.append("  - ").append(v).append("\n"));
        }
        
        if (!violations.capacityViolations.isEmpty()) {
            report.append("Capacity violations (10c):\n");
            violations.capacityViolations.forEach(v -> report.append("  - ").append(v).append("\n"));
        }
        
        if (!violations.loadViolations.isEmpty()) {
            report.append("Load violations (10d):\n");
            violations.loadViolations.forEach(v -> report.append("  - ").append(v).append("\n"));
        }
        
        if (!violations.deploymentViolations.isEmpty()) {
            report.append("Deployment violations (10e):\n");
            violations.deploymentViolations.forEach(v -> report.append("  - ").append(v).append("\n"));
        }
        
        return report.toString();
    }
    
    /**
     * Calculate solution quality metrics
     */
    public static Map<String, Double> calculateSolutionMetrics(AGCTLBSolution solution) {
        Map<String, Double> metrics = new HashMap<>();
        
        metrics.put("objective_value", solution.objectiveValue);
        metrics.put("feasible", solution.feasible ? 1.0 : 0.0);
        metrics.put("total_violations", (double) solution.violations.getTotalViolations());
        
        if (!solution.baseStationLoads.isEmpty()) {
            double maxLoad = solution.baseStationLoads.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double avgLoad = solution.baseStationLoads.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double minLoad = solution.baseStationLoads.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            metrics.put("max_load", maxLoad);
            metrics.put("avg_load", avgLoad);
            metrics.put("min_load", minLoad);
            metrics.put("load_variance", calculateLoadVariance(solution.baseStationLoads));
        }
        
        return metrics;
    }
    
    private static double calculateLoadVariance(Map<Object, Double> loads) {
        double mean = loads.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return loads.values().stream()
            .mapToDouble(load -> Math.pow(load - mean, 2))
            .average().orElse(0.0);
    }
}