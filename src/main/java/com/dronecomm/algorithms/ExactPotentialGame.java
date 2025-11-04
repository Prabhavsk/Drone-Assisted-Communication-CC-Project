package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;
import com.dronecomm.algorithms.AlphaFairnessLoadBalancer.FairnessPolicy;

import java.util.*;

/**
 * Potential-game based DBS deployment helper.
 *
 * Uses a 3D grid and Gibbs-sampling updates to find DBS placements. Prioritises
 * clarity and reproducibility for experiments.
 */
public class ExactPotentialGame {
    
    // Grid parameters
    private static final double DELTA_X = 50.0;    // Grid granularity in X (meters)
    private static final double DELTA_Y = 50.0;    // Grid granularity in Y (meters)  
    private static final double DELTA_H = 20.0;    // Grid granularity in altitude (meters)
    
    // Game parameters
    private static final double INITIAL_PSI = 1.0;  // Initial Boltzmann parameter 
    private static final double PSI_INCREMENT = 0.1; //  increment per iteration
    private static final int MAX_ITERATIONS = 1000;  // Maximum game iterations
    private static final double CONVERGENCE_TOL = 1e-6; // Convergence tolerance
    
    // Deployment region bounds
    private final double xMin, xMax, yMin, yMax, hMin, hMax;
    private final List<Position3D> gridPoints;
    
    /**
     * Action (position) for a DBS player
     */
    public static class DBSAction {
        public final Position3D position;
        public final double costValue;    // Individual cost C_j
        public final double utilityValue; // Utility value (negative cost)
        
        public DBSAction(Position3D position, double cost) {
            this.position = position;
            this.costValue = cost;
            this.utilityValue = -cost; // Utility is negative cost
        }
    }
    
    /**
     * Game state representing all DBS positions
     */
    public static class GameState {
        public final Map<DroneBaseStation, Position3D> positions;
        public final double potentialValue;      // Potential function 
        public final Map<DroneBaseStation, Double> individualCosts; // C_j for each DBS
        public final boolean isNashEquilibrium;
        
        public GameState(Map<DroneBaseStation, Position3D> positions, double potential,
                        Map<DroneBaseStation, Double> costs, boolean isNash) {
            this.positions = new HashMap<>(positions);
            this.potentialValue = potential;
            this.individualCosts = new HashMap<>(costs);
            this.isNashEquilibrium = isNash;
        }
    }
    
    /**
     * Result of the potential game optimization
     */
    public static class PotentialGameResult {
        public final GameState finalState;
        public final List<GameState> convergenceHistory;
        public final int iterations;
        public final boolean converged;
        public final Map<Object, Set<MobileUser>> finalAssignments;
        
        public PotentialGameResult(GameState finalState, List<GameState> history,
                                 int iterations, boolean converged,
                                 Map<Object, Set<MobileUser>> assignments) {
            this.finalState = finalState;
            this.convergenceHistory = new ArrayList<>(history);
            this.iterations = iterations;
            this.converged = converged;
            this.finalAssignments = new HashMap<>(assignments);
        }
    }
    
    /**
     * Constructor with deployment region specification
     */
    public ExactPotentialGame(double xMin, double xMax, double yMin, double yMax, 
                             double hMin, double hMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.hMin = hMin;
        this.hMax = hMax;
        this.gridPoints = generateGridPoints();
    }
    
    /**
     * Generate 3D discrete grid points
     */
    private List<Position3D> generateGridPoints() {
        List<Position3D> points = new ArrayList<>();
        
        for (double x = xMin; x <= xMax; x += DELTA_X) {
            for (double y = yMin; y <= yMax; y += DELTA_Y) {
                for (double h = hMin; h <= hMax; h += DELTA_H) {
                    points.add(new Position3D(x, y, h));
                }
            }
        }
        
        return points;
    }
    
    /**
     * Run Gibbs-sampling updates to find a low-potential DBS deployment.
     * Returns positions, convergence history and final user assignments.
     */
    public PotentialGameResult solveDeployment(
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize) {
        
        // Initialize DBS positions randomly on grid
        Map<DroneBaseStation, Position3D> currentPositions = initializePositions(droneStations);
        
        List<GameState> history = new ArrayList<>();
        double psi = INITIAL_PSI;
        boolean converged = false;
        int iteration = 0;
        
        for (iteration = 0; iteration < MAX_ITERATIONS && !converged; iteration++) {
            // Update positions for each DBS sequentially using Gibbs sampling
            boolean positionChanged = false;
            
            for (DroneBaseStation dbs : droneStations) {
                Position3D newPosition = updateDBSPosition(dbs, currentPositions, 
                                                         groundStations, users, policy, 
                                                         meanPacketSize, psi);
                
                if (!newPosition.equals(currentPositions.get(dbs))) {
                    currentPositions.put(dbs, newPosition);
                    positionChanged = true;
                }
            }
            
            // Calculate current game state
            GameState currentState = calculateGameState(currentPositions, droneStations, 
                                                      groundStations, users, policy, meanPacketSize);
            history.add(currentState);
            
            // Check convergence
            if (!positionChanged || (history.size() > 1 && 
                Math.abs(currentState.potentialValue - history.get(history.size()-2).potentialValue) < CONVERGENCE_TOL)) {
                converged = true;
            }
            
            // Increase exploitation (reduce exploration)
            psi += PSI_INCREMENT;
        }
        
        // Calculate final user assignments based on optimal positions
        Map<Object, Set<MobileUser>> finalAssignments = calculateFinalAssignments(
            currentPositions, droneStations, groundStations, users, meanPacketSize);
        
        GameState finalState = history.get(history.size() - 1);
        return new PotentialGameResult(finalState, history, iteration, converged, finalAssignments);
    }
    
    /**
     * Initialize DBS positions randomly on the grid
     */
    private Map<DroneBaseStation, Position3D> initializePositions(List<DroneBaseStation> droneStations) {
        Map<DroneBaseStation, Position3D> positions = new HashMap<>();
        Random random = new Random();
        
        for (DroneBaseStation dbs : droneStations) {
            Position3D randomPosition = gridPoints.get(random.nextInt(gridPoints.size()));
            positions.put(dbs, randomPosition);
        }
        
        return positions;
    }
    
    /**
     * Update DBS position using Gibbs sampling with constrained strategy profile
     */
    private Position3D updateDBSPosition(
            DroneBaseStation dbs,
            Map<DroneBaseStation, Position3D> currentPositions,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize,
            double psi) {
        
        Position3D currentPos = currentPositions.get(dbs);
        
        // Generate constrained strategy profile (neighboring positions)
        List<Position3D> candidatePositions = getConstrainedStrategyProfile(currentPos);
        
        // Calculate costs for each candidate position
        List<Double> costs = new ArrayList<>();
        double maxCost = Double.NEGATIVE_INFINITY;
        
        for (Position3D candidatePos : candidatePositions) {
            // Temporarily update position
            currentPositions.put(dbs, candidatePos);
            
            // Calculate individual cost
            double cost = calculateIndividualCost(dbs, currentPositions, groundStations, 
                                                users, policy, meanPacketSize);
            costs.add(cost);
            maxCost = Math.max(maxCost, cost);
        }
        
        // Restore original position
        currentPositions.put(dbs, currentPos);
        
        // Calculate state transfer probabilities using Boltzmann distribution
        // Pr(a_j^t|a_j^{t-1}) = exp(-C_j) / Sumexp(-C_j)
        List<Double> probabilities = new ArrayList<>();
        double sumExp = 0.0;
        
        for (double cost : costs) {
            double expValue = Math.exp(-psi * (cost - maxCost)); // Subtract maxCost for numerical stability
            probabilities.add(expValue);
            sumExp += expValue;
        }
        
        // Normalize probabilities
        for (int i = 0; i < probabilities.size(); i++) {
            probabilities.set(i, probabilities.get(i) / sumExp);
        }
        
        // Sample new position based on probabilities
        return samplePosition(candidatePositions, probabilities);
    }
    
    /**
     * Generate constrained strategy profile (current position + neighbors)
     */
    private List<Position3D> getConstrainedStrategyProfile(Position3D currentPos) {
        List<Position3D> candidates = new ArrayList<>();
        candidates.add(currentPos); // Current position
        
        // Add neighboring positions (6-connectivity in 3D grid)
        double[] dx = {-DELTA_X, DELTA_X, 0, 0, 0, 0};
        double[] dy = {0, 0, -DELTA_Y, DELTA_Y, 0, 0};
        double[] dh = {0, 0, 0, 0, -DELTA_H, DELTA_H};
        
        for (int i = 0; i < dx.length; i++) {
            double newX = currentPos.getX() + dx[i];
            double newY = currentPos.getY() + dy[i];
            double newH = currentPos.getZ() + dh[i];
            
            // Check bounds
            if (newX >= xMin && newX <= xMax && 
                newY >= yMin && newY <= yMax && 
                newH >= hMin && newH <= hMax) {
                candidates.add(new Position3D(newX, newY, newH));
            }
        }
        
        return candidates;
    }
    
    /**
     * Calculate individual cost function C_j for a DBS
     * This corresponds to the alpha-fairness objective component for DBS j
     */
    private double calculateIndividualCost(
            DroneBaseStation dbs,
            Map<DroneBaseStation, Position3D> positions,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize) {
        
        // Update DBS position temporarily
        Position3D originalPos = dbs.getCurrentPosition();
        dbs.setCurrentPosition(positions.get(dbs));
        
        try {
            // Calculate user assignments with new position
            List<Object> allBS = new ArrayList<>();
            allBS.addAll(Arrays.asList(dbs));
            allBS.addAll(groundStations);
            
            // Simple greedy assignment for cost calculation
            Map<Object, Set<MobileUser>> assignments = calculateGreedyAssignments(allBS, users, meanPacketSize);
            
            // Calculate load for this DBS
            Set<MobileUser> assignedUsers = assignments.getOrDefault(dbs, new HashSet<>());
            double load = AlphaFairnessLoadBalancer.calculateTrafficLoad(dbs, assignedUsers, meanPacketSize, false);
            
            // Individual cost based on alpha-fairness contribution
            if (load >= 1.0) {
                return Double.POSITIVE_INFINITY; // Infeasible
            }
            
            double alpha = policy.alpha;
            if (Double.isInfinite(alpha)) {
                return load; // Min-max cost
            } else if (Math.abs(alpha - 1.0) < 1e-9) {
                return -Math.log(1.0 - load); // Proportional-fair cost
            } else {
                return Math.pow(1.0 - load, 1.0 - alpha) / (alpha - 1.0); // General alpha-fairness cost
            }
            
        } finally {
            // Restore original position
            dbs.setCurrentPosition(originalPos);
        }
    }
    
    /**
     * Sample position based on probability distribution
     */
    private Position3D samplePosition(List<Position3D> candidates, List<Double> probabilities) {
        Random random = new Random();
        double r = random.nextDouble();
        double cumulative = 0.0;
        
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += probabilities.get(i);
            if (r <= cumulative) {
                return candidates.get(i);
            }
        }
        
        return candidates.get(candidates.size() - 1); // Fallback to last candidate
    }
    
    /**
     * Calculate current game state with potential function
     */
    private GameState calculateGameState(
            Map<DroneBaseStation, Position3D> positions,
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize) {
        
        // Update all DBS positions
        for (DroneBaseStation dbs : droneStations) {
            dbs.setCurrentPosition(positions.get(dbs));
        }
        
        // Calculate individual costs
        Map<DroneBaseStation, Double> costs = new HashMap<>();
        for (DroneBaseStation dbs : droneStations) {
            double cost = calculateIndividualCost(dbs, positions, groundStations, users, policy, meanPacketSize);
            costs.put(dbs, cost);
        }
        
        // Potential function:  = Sum C_j (sum of individual costs)
        double potential = costs.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Check if current state is Nash Equilibrium (simplified check)
        boolean isNash = checkNashEquilibrium(droneStations, positions, groundStations, users, policy, meanPacketSize);
        
        return new GameState(positions, potential, costs, isNash);
    }
    
    /**
     * Simple Nash Equilibrium check
     */
    private boolean checkNashEquilibrium(
            List<DroneBaseStation> droneStations,
            Map<DroneBaseStation, Position3D> positions,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize) {
        
        // For each DBS, check if unilateral deviation improves its cost
        for (DroneBaseStation dbs : droneStations) {
            double currentCost = calculateIndividualCost(dbs, positions, groundStations, users, policy, meanPacketSize);
            
            // Try a few alternative positions
            List<Position3D> alternatives = getConstrainedStrategyProfile(positions.get(dbs));
            for (Position3D altPos : alternatives) {
                if (!altPos.equals(positions.get(dbs))) {
                    Map<DroneBaseStation, Position3D> testPositions = new HashMap<>(positions);
                    testPositions.put(dbs, altPos);
                    
                    double altCost = calculateIndividualCost(dbs, testPositions, groundStations, users, policy, meanPacketSize);
                    
                    if (altCost < currentCost - CONVERGENCE_TOL) {
                        return false; // Found beneficial deviation
                    }
                }
            }
        }
        
        return true; // No beneficial deviations found
    }
    
    /**
     * Calculate greedy user assignments for cost evaluation
     */
    private Map<Object, Set<MobileUser>> calculateGreedyAssignments(
            List<Object> baseStations, List<MobileUser> users, double meanPacketSize) {
        
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        for (Object bs : baseStations) {
            assignments.put(bs, new HashSet<>());
        }
        
        // Assign each user to nearest base station (simplified)
        for (MobileUser user : users) {
            Object bestBS = baseStations.stream()
                .min(Comparator.comparingDouble(bs -> getDistanceToUser(bs, user)))
                .orElse(baseStations.get(0));
            
            assignments.get(bestBS).add(user);
        }
        
        return assignments;
    }
    
    /**
     * Get distance from base station to user
     */
    private double getDistanceToUser(Object baseStation, MobileUser user) {
        if (baseStation instanceof DroneBaseStation) {
            return user.getCurrentPosition().distanceTo(((DroneBaseStation) baseStation).getCurrentPosition());
        } else if (baseStation instanceof GroundBaseStation) {
            return user.getCurrentPosition().distanceTo(((GroundBaseStation) baseStation).getPosition());
        }
        return Double.MAX_VALUE;
    }
    
    /**
     * Calculate final optimal user assignments
     */
    private Map<Object, Set<MobileUser>> calculateFinalAssignments(
            Map<DroneBaseStation, Position3D> positions,
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            double meanPacketSize) {
        
        // Update all positions
        for (DroneBaseStation dbs : droneStations) {
            dbs.setCurrentPosition(positions.get(dbs));
        }
        
        List<Object> allBS = new ArrayList<>();
        allBS.addAll(droneStations);
        allBS.addAll(groundStations);
        
        // Use more sophisticated assignment (could integrate with P-SCA here)
        return calculateGreedyAssignments(allBS, users, meanPacketSize);
    }
}