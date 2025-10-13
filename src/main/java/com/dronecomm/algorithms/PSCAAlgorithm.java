package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.algorithms.AlphaFairnessLoadBalancer.FairnessPolicy;

import java.util.*;

/**
 * Penalty-based Successive Convex Approximation (P-SCA) Algorithm
 * 
 * This class implements the P-SCA algorithm from the research paper for solving
 * the user association subproblem with binary variable relaxation:
 * 
 * - Binary variable relaxation: xij  xij in [0,1]
 * - Penalty term: (1/lambda) * Sum(xij - xij^2) 
 * - First-order Taylor approximation for convexification
 * - Double-loop optimization with lambda := lambda
 * 
 * Solves the relaxed problem:
 * min phialpha(rho) + (1/lambda) * Sum(xij - xij^2)
 * s.t. Sumxij = 1, 0 <= xij <= 1, capacity constraints
 */
public class PSCAAlgorithm {
    
    // Algorithm parameters
    private static final double INITIAL_LAMBDA = 1.0;      // Initial penalty parameter
    private static final double LAMBDA_SCALING = 0.5;      //  scaling factor
    private static final double CONVERGENCE_TOL = 1e-6;    // Convergence tolerance
    private static final int MAX_OUTER_ITERATIONS = 50;    // Max outer loop iterations
    private static final int MAX_INNER_ITERATIONS = 100;   // Max inner loop iterations
    private static final double MIN_LAMBDA = 1e-6;         // Minimum lambda value
    
    /**
     * Result of P-SCA optimization
     */
    public static class PSCAResult {
        public final Map<MobileUser, Map<Object, Double>> relaxedAssignments; // xij values
        public final Map<Object, Set<MobileUser>> binaryAssignments;          // Final binary assignments
        public final double objectiveValue;                                   // Final objective
        public final int outerIterations;                                     // Convergence iterations
        public final boolean converged;                                       // Convergence status
        public final Map<Object, Double> finalLoads;                         // Final load distribution
        
        public PSCAResult(Map<MobileUser, Map<Object, Double>> relaxedAssignments,
                         Map<Object, Set<MobileUser>> binaryAssignments,
                         double objectiveValue, int iterations, boolean converged,
                         Map<Object, Double> finalLoads) {
            this.relaxedAssignments = new HashMap<>(relaxedAssignments);
            this.binaryAssignments = new HashMap<>(binaryAssignments);
            this.objectiveValue = objectiveValue;
            this.outerIterations = iterations;
            this.converged = converged;
            this.finalLoads = new HashMap<>(finalLoads);
        }
    }
    
    /**
     * Solve user association subproblem using P-SCA algorithm
     * 
     * @param droneStations Available drone base stations
     * @param groundStations Available ground base stations  
     * @param users Mobile users to assign
     * @param policy alpha-fairness policy
     * @param meanPacketSize Average packet size for load calculation
     * @return PSCAResult containing optimal assignments
     */
    public static PSCAResult solveUserAssociation(
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize) {
        
        List<Object> allBaseStations = new ArrayList<>();
        allBaseStations.addAll(droneStations);
        allBaseStations.addAll(groundStations);
        
        // Initialize relaxed variables xij
        Map<MobileUser, Map<Object, Double>> relaxedAssignments = initializeRelaxedAssignments(users, allBaseStations);
        
        double lambda = INITIAL_LAMBDA;
        double prevObjective = Double.POSITIVE_INFINITY;
        boolean converged = false;
        int outerIter = 0;
        
        // Outer loop: Update penalty parameter
        for (outerIter = 0; outerIter < MAX_OUTER_ITERATIONS && !converged; outerIter++) {
            
            // Inner loop: Solve penalized problem with fixed lambda
            double currentObjective = solveInnerProblem(relaxedAssignments, allBaseStations, 
                                                      users, policy, meanPacketSize, lambda);
            
            // Check convergence
            double improvement = Math.abs(prevObjective - currentObjective);
            if (improvement < CONVERGENCE_TOL || lambda < MIN_LAMBDA) {
                converged = true;
                break;
            }
            
            prevObjective = currentObjective;
            lambda *= LAMBDA_SCALING; // lambda := lambda
        }
        
        // Convert relaxed assignments to binary assignments
        Map<Object, Set<MobileUser>> binaryAssignments = convertToBinaryAssignments(relaxedAssignments);
        
        // Calculate final loads and objective
        Map<Object, Double> finalLoads = calculateFinalLoads(binaryAssignments, allBaseStations, meanPacketSize);
        double finalObjective = AlphaFairnessLoadBalancer.calculateAlphaFairnessObjective(finalLoads, policy);
        
        return new PSCAResult(relaxedAssignments, binaryAssignments, finalObjective, 
                            outerIter, converged, finalLoads);
    }
    
    /**
     * Initialize relaxed assignment variables xij uniformly
     */
    private static Map<MobileUser, Map<Object, Double>> initializeRelaxedAssignments(
            List<MobileUser> users, List<Object> baseStations) {
        
        Map<MobileUser, Map<Object, Double>> assignments = new HashMap<>();
        double uniformValue = 1.0 / baseStations.size();
        
        for (MobileUser user : users) {
            Map<Object, Double> userAssignments = new HashMap<>();
            for (Object bs : baseStations) {
                userAssignments.put(bs, uniformValue);
            }
            assignments.put(user, userAssignments);
        }
        
        return assignments;
    }
    
    /**
     * Solve inner penalized problem using successive convex approximation
     */
    private static double solveInnerProblem(
            Map<MobileUser, Map<Object, Double>> relaxedAssignments,
            List<Object> baseStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize,
            double lambda) {
        
        double prevObjective = Double.POSITIVE_INFINITY;
        
        // Inner SCA iterations
        for (int innerIter = 0; innerIter < MAX_INNER_ITERATIONS; innerIter++) {
            
            // Store current feasible point for Taylor approximation
            Map<MobileUser, Map<Object, Double>> feasiblePoint = deepCopyAssignments(relaxedAssignments);
            
            // Update each user's assignment using coordinate descent
            for (MobileUser user : users) {
                updateUserAssignment(user, relaxedAssignments, baseStations, feasiblePoint, 
                                   policy, meanPacketSize, lambda);
            }
            
            // Calculate current objective
            double currentObjective = calculatePenalizedObjective(relaxedAssignments, baseStations, 
                                                                users, policy, meanPacketSize, lambda, feasiblePoint);
            
            // Check inner convergence
            if (Math.abs(prevObjective - currentObjective) < CONVERGENCE_TOL) {
                break;
            }
            prevObjective = currentObjective;
        }
        
        return prevObjective;
    }
    
    /**
     * Update assignment for a single user using coordinate descent
     */
    private static void updateUserAssignment(
            MobileUser user,
            Map<MobileUser, Map<Object, Double>> assignments,
            List<Object> baseStations,
            Map<MobileUser, Map<Object, Double>> feasiblePoint,
            FairnessPolicy policy,
            double meanPacketSize,
            double lambda) {
        
        Map<Object, Double> bestAssignments = new HashMap<>();
        double bestObjective = Double.POSITIVE_INFINITY;
        
        // Try different assignment distributions for this user
        for (int primaryBS = 0; primaryBS < baseStations.size(); primaryBS++) {
            Map<Object, Double> testAssignments = new HashMap<>();
            
            // Assign most weight to primary BS, distribute rest
            for (int i = 0; i < baseStations.size(); i++) {
                Object bs = baseStations.get(i);
                if (i == primaryBS) {
                    testAssignments.put(bs, 0.8); // Primary assignment
                } else {
                    testAssignments.put(bs, 0.2 / (baseStations.size() - 1)); // Distributed
                }
            }
            
            // Normalize to satisfy Sumxij = 1
            normalizeAssignments(testAssignments);
            
            // Temporarily update assignments
            assignments.put(user, testAssignments);
            
            // Calculate objective with penalty
            double objective = calculatePenalizedObjective(assignments, baseStations, 
                                                         Arrays.asList(user), policy, meanPacketSize, lambda, feasiblePoint);
            
            if (objective < bestObjective) {
                bestObjective = objective;
                bestAssignments = new HashMap<>(testAssignments);
            }
        }
        
        // Apply best assignment
        assignments.put(user, bestAssignments);
    }
    
    /**
     * Calculate penalized objective with Taylor approximation
     * Implements: phialpha(rho) + (1/lambda) * Sum(xij - xij^2) with convex approximation
     */
    private static double calculatePenalizedObjective(
            Map<MobileUser, Map<Object, Double>> assignments,
            List<Object> baseStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize,
            double lambda,
            Map<MobileUser, Map<Object, Double>> feasiblePoint) {
        
        // Calculate loads based on current assignments
        Map<Object, Double> loads = new HashMap<>();
        for (Object bs : baseStations) {
            double load = 0.0;
            for (MobileUser user : users) {
                double xij = assignments.get(user).get(bs);
                double rate = calculateAssignmentWeightedRate(user, bs, meanPacketSize);
                if (rate > 0) {
                    double lambda_i = user.getDataRate() / (meanPacketSize * 8);
                    load += xij * lambda_i * meanPacketSize / rate;
                }
            }
            loads.put(bs, Math.min(1.0, load));
        }
        
        // alpha-fairness objective
        double alphaObjective = AlphaFairnessLoadBalancer.calculateAlphaFairnessObjective(loads, policy);
        
        // Penalty term with Taylor approximation: xij - xij^2 <= xij - (xij^f)^2 - 2*xij^f*(xij - xij^f)
        double penaltyTerm = 0.0;
        for (MobileUser user : users) {
            for (Object bs : baseStations) {
                double xij = assignments.get(user).get(bs);
                double xij_f = feasiblePoint.get(user).get(bs);
                
                // Linear approximation of penalty term
                double linearPenalty = xij - xij_f * xij_f - 2 * xij_f * (xij - xij_f);
                penaltyTerm += linearPenalty;
            }
        }
        
        return alphaObjective + penaltyTerm / lambda;
    }
    
    /**
     * Calculate rate for fractional assignment
     */
    private static double calculateAssignmentWeightedRate(MobileUser user, Object baseStation, double meanPacketSize) {
        // This is a simplified rate calculation for fractional assignments
        // In practice, this would involve more complex interference modeling
        return AlphaFairnessLoadBalancer.calculateTrafficLoad(baseStation, 
                Set.of(user), meanPacketSize, false) * 1e6; // Approximate rate
    }
    
    /**
     * Normalize assignments to satisfy Sumxij = 1
     */
    private static void normalizeAssignments(Map<Object, Double> assignments) {
        double sum = assignments.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            assignments.replaceAll((bs, value) -> value / sum);
        }
    }
    
    /**
     * Convert relaxed assignments to binary assignments
     */
    private static Map<Object, Set<MobileUser>> convertToBinaryAssignments(
            Map<MobileUser, Map<Object, Double>> relaxedAssignments) {
        
        Map<Object, Set<MobileUser>> binaryAssignments = new HashMap<>();
        
        // Initialize empty sets
        for (MobileUser user : relaxedAssignments.keySet()) {
            for (Object bs : relaxedAssignments.get(user).keySet()) {
                binaryAssignments.putIfAbsent(bs, new HashSet<>());
            }
        }
        
        // Assign each user to base station with highest xij value
        for (MobileUser user : relaxedAssignments.keySet()) {
            Object bestBS = relaxedAssignments.get(user).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (bestBS != null) {
                binaryAssignments.get(bestBS).add(user);
            }
        }
        
        return binaryAssignments;
    }
    
    /**
     * Calculate final loads for binary assignments
     */
    private static Map<Object, Double> calculateFinalLoads(
            Map<Object, Set<MobileUser>> assignments,
            List<Object> baseStations,
            double meanPacketSize) {
        
        Map<Object, Double> loads = new HashMap<>();
        for (Object bs : baseStations) {
            double load = AlphaFairnessLoadBalancer.calculateTrafficLoad(
                bs, assignments.getOrDefault(bs, new HashSet<>()), meanPacketSize, false);
            loads.put(bs, load);
        }
        return loads;
    }
    
    /**
     * Deep copy assignments for Taylor approximation reference point
     */
    private static Map<MobileUser, Map<Object, Double>> deepCopyAssignments(
            Map<MobileUser, Map<Object, Double>> original) {
        
        Map<MobileUser, Map<Object, Double>> copy = new HashMap<>();
        for (Map.Entry<MobileUser, Map<Object, Double>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }
    
    /**
     * Validate P-SCA solution constraints
     */
    public static boolean validateSolution(PSCAResult result) {
        // Check if each user is assigned to exactly one base station
        for (MobileUser user : result.relaxedAssignments.keySet()) {
            double sum = result.relaxedAssignments.get(user).values().stream()
                .mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 1.0) > 1e-6) {
                return false; // Constraint Sumxij = 1 violated
            }
        }
        
        // Check capacity constraints (simplified)
        for (Object bs : result.binaryAssignments.keySet()) {
            int assigned = result.binaryAssignments.get(bs).size();
            int capacity = getMaxCapacity(bs);
            if (assigned > capacity) {
                return false; // Capacity constraint violated
            }
        }
        
        return true;
    }
    
    private static int getMaxCapacity(Object baseStation) {
        if (baseStation instanceof DroneBaseStation) {
            return (int) ((DroneBaseStation) baseStation).getMaxUserCapacity();
        } else if (baseStation instanceof GroundBaseStation) {
            return (int) ((GroundBaseStation) baseStation).getMaxUserCapacity();
        }
        return 50;
    }
}