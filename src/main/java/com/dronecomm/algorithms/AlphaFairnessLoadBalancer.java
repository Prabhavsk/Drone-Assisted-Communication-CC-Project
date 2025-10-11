package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;

import java.util.*;

/**
 * Alpha-Fairness Load Balancing Implementation
 * 
 * This class implements the α-fairness load balancing formulation from the research paper:
 * - Equation (8): ρj = Σ xij * λi * μ / rij (traffic load calculation)
 * - Equation (9): φα(ρ) = Σ(1-ρj)^(1-α)/(α-1) for α≥0, α≠1
 *                        = -Σlog(1-ρj) for α=1
 * 
 * Different α values provide different fairness policies:
 * - α = 0: Min-sum load policy (maximize total idle time)
 * - α = 1: Proportional-fair policy (maximize geometric mean of idle time)
 * - α = 2: Latency-optimal policy (minimize average latency)
 * - α = ∞: Min-max load policy (minimize maximum load)
 */
public class AlphaFairnessLoadBalancer {
    
    /**
     * Fairness policy types corresponding to different α values
     */
    public enum FairnessPolicy {
        MIN_SUM(0.0),           // α = 0: Minimize sum of loads
        PROPORTIONAL_FAIR(1.0), // α = 1: Proportional fairness
        LATENCY_OPTIMAL(2.0),   // α = 2: Latency optimization
        MIN_MAX(Double.POSITIVE_INFINITY); // α = ∞: Min-max fairness
        
        public final double alpha;
        
        FairnessPolicy(double alpha) {
            this.alpha = alpha;
        }
    }
    
    /**
     * Traffic load and fairness calculation result
     */
    public static class LoadBalancingResult {
        public final Map<Object, Double> baseStationLoads;  // ρj for each base station
        public final double fairnessObjective;              // φα(ρ)
        public final Map<Object, Set<MobileUser>> assignments; // User assignments
        public final FairnessPolicy policy;
        public final double totalLoad;
        public final double maxLoad;
        public final double loadVariance;
        
        public LoadBalancingResult(Map<Object, Double> loads, double objective, 
                                 Map<Object, Set<MobileUser>> assignments,
                                 FairnessPolicy policy) {
            this.baseStationLoads = new HashMap<>(loads);
            this.fairnessObjective = objective;
            this.assignments = new HashMap<>(assignments);
            this.policy = policy;
            
            // Calculate additional metrics
            this.totalLoad = loads.values().stream().mapToDouble(Double::doubleValue).sum();
            this.maxLoad = loads.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            
            double meanLoad = totalLoad / loads.size();
            this.loadVariance = loads.values().stream()
                .mapToDouble(load -> Math.pow(load - meanLoad, 2))
                .average().orElse(0.0);
        }
    }
    
    /**
     * Calculate traffic load for a base station
     * Implements equation (8): ρj = Σ xij * λi * μ / rij
     * 
     * @param baseStation Target base station
     * @param assignedUsers Set of users assigned to this base station
     * @param meanPacketSize Average packet size μ (bits)
     * @param useAFRelay Whether to use AF relay model for rate calculation
     * @return Traffic load ρj ∈ [0,1]
     */
    public static double calculateTrafficLoad(Object baseStation, Set<MobileUser> assignedUsers,
                                            double meanPacketSize, boolean useAFRelay) {
        double totalLoad = 0.0;
        
        for (MobileUser user : assignedUsers) {
            double lambda_i = user.getDataRate() / (meanPacketSize * 8); // Convert to packets/sec
            double r_ij = calculateAchievableRate(user, baseStation, useAFRelay);
            
            if (r_ij > 0) {
                totalLoad += lambda_i * meanPacketSize / r_ij;
            }
        }
        
        return Math.min(1.0, totalLoad); // Ensure load ∈ [0,1]
    }
    
    /**
     * Calculate achievable data rate based on base station type and relay model
     */
    private static double calculateAchievableRate(MobileUser user, Object baseStation, boolean useAFRelay) {
        if (baseStation instanceof DroneBaseStation) {
            DroneBaseStation dbs = (DroneBaseStation) baseStation;
            
            if (useAFRelay) {
                // Use AF relay model - need to find target MBS
                // For simplicity, assume direct transmission for now
                A2GChannelModel.A2GChannelResult channel = A2GChannelModel.calculateA2GChannel(user, dbs);
                double txPower = 0.02; // 20 mW UE transmission power
                double snr = A2GChannelModel.calculateSNR(txPower, channel.channelGain, dbs.getBandwidth());
                return dbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
            } else {
                // Direct A2G transmission
                A2GChannelModel.A2GChannelResult channel = A2GChannelModel.calculateA2GChannel(user, dbs);
                double txPower = 0.02; // 20 mW
                double snr = A2GChannelModel.calculateSNR(txPower, channel.channelGain, dbs.getBandwidth());
                return dbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
            }
        } else if (baseStation instanceof GroundBaseStation) {
            GroundBaseStation gbs = (GroundBaseStation) baseStation;
            double channelGain = A2GChannelModel.calculateUEToMBSChannelGain(user, gbs);
            double txPower = 0.02; // 20 mW
            double snr = A2GChannelModel.calculateSNR(txPower, channelGain, gbs.getBandwidth());
            return gbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
        }
        
        return 1e6; // Default 1 Mbps if calculation fails
    }
    
    /**
     * Calculate α-fairness objective function
     * Implements equation (9) from the research paper
     */
    public static double calculateAlphaFairnessObjective(Map<Object, Double> loads, FairnessPolicy policy) {
        double alpha = policy.alpha;
        double objective = 0.0;
        
        if (Double.isInfinite(alpha)) {
            // Min-max policy: φα(ρ) = max{ρj}
            return loads.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        } else if (Math.abs(alpha - 1.0) < 1e-9) {
            // Proportional-fair policy: φα(ρ) = -Σlog(1-ρj)
            for (double load : loads.values()) {
                if (load < 1.0) {
                    objective -= Math.log(1.0 - load);
                } else {
                    return Double.POSITIVE_INFINITY; // Infeasible
                }
            }
        } else {
            // General α-fairness: φα(ρ) = Σ(1-ρj)^(1-α)/(α-1)
            for (double load : loads.values()) {
                if (load < 1.0) {
                    objective += Math.pow(1.0 - load, 1.0 - alpha) / (alpha - 1.0);
                } else {
                    return Double.POSITIVE_INFINITY; // Infeasible
                }
            }
        }
        
        return objective;
    }
    
    /**
     * Optimize load balancing using α-fairness with greedy assignment
     * This is a heuristic approach; the paper uses P-SCA for exact optimization
     */
    public static LoadBalancingResult optimizeLoadBalancing(
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            FairnessPolicy policy,
            double meanPacketSize,
            boolean useAFRelay) {
        
        List<Object> allBaseStations = new ArrayList<>();
        allBaseStations.addAll(droneStations);
        allBaseStations.addAll(groundStations);
        
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        for (Object bs : allBaseStations) {
            assignments.put(bs, new HashSet<>());
        }
        
        // Greedy assignment based on α-fairness objective
        for (MobileUser user : users) {
            Object bestBS = null;
            double bestObjective = Double.POSITIVE_INFINITY;
            
            for (Object bs : allBaseStations) {
                // Check capacity constraints
                if (assignments.get(bs).size() >= getMaxUserCapacity(bs)) {
                    continue;
                }
                
                // Try assigning user to this base station
                assignments.get(bs).add(user);
                
                // Calculate resulting loads and objective
                Map<Object, Double> loads = new HashMap<>();
                for (Object testBS : allBaseStations) {
                    loads.put(testBS, calculateTrafficLoad(testBS, assignments.get(testBS), 
                                                         meanPacketSize, useAFRelay));
                }
                
                double objective = calculateAlphaFairnessObjective(loads, policy);
                
                if (objective < bestObjective) {
                    bestObjective = objective;
                    bestBS = bs;
                }
                
                // Remove user for next iteration
                assignments.get(bs).remove(user);
            }
            
            // Assign user to best base station
            if (bestBS != null) {
                assignments.get(bestBS).add(user);
            } else {
                // Fallback: assign to least loaded station
                Object leastLoaded = allBaseStations.stream()
                    .min(Comparator.comparingInt(bs -> assignments.get(bs).size()))
                    .orElse(allBaseStations.get(0));
                assignments.get(leastLoaded).add(user);
            }
        }
        
        // Calculate final loads and objective
        Map<Object, Double> finalLoads = new HashMap<>();
        for (Object bs : allBaseStations) {
            finalLoads.put(bs, calculateTrafficLoad(bs, assignments.get(bs), meanPacketSize, useAFRelay));
        }
        
        double finalObjective = calculateAlphaFairnessObjective(finalLoads, policy);
        
        return new LoadBalancingResult(finalLoads, finalObjective, assignments, policy);
    }
    
    /**
     * Get maximum user capacity for a base station
     */
    private static int getMaxUserCapacity(Object baseStation) {
        if (baseStation instanceof DroneBaseStation) {
            return (int) ((DroneBaseStation) baseStation).getMaxUserCapacity();
        } else if (baseStation instanceof GroundBaseStation) {
            return (int) ((GroundBaseStation) baseStation).getMaxUserCapacity();
        }
        return 50; // Default capacity
    }
    
    /**
     * Compare different fairness policies for the same network scenario
     */
    public static Map<FairnessPolicy, LoadBalancingResult> compareAllPolicies(
            List<DroneBaseStation> droneStations,
            List<GroundBaseStation> groundStations,
            List<MobileUser> users,
            double meanPacketSize,
            boolean useAFRelay) {
        
        Map<FairnessPolicy, LoadBalancingResult> results = new HashMap<>();
        
        for (FairnessPolicy policy : FairnessPolicy.values()) {
            LoadBalancingResult result = optimizeLoadBalancing(droneStations, groundStations, 
                                                             users, policy, meanPacketSize, useAFRelay);
            results.put(policy, result);
        }
        
        return results;
    }
    
    /**
     * Calculate load balancing efficiency metrics
     */
    public static double calculateLoadBalancingEfficiency(LoadBalancingResult result) {
        double maxLoad = result.maxLoad;
        
        if (maxLoad <= 0) return 1.0;
        
        // Efficiency = 1 - (load_variance / max_possible_variance)
        double maxPossibleVariance = Math.pow(maxLoad, 2);
        return Math.max(0.0, 1.0 - result.loadVariance / maxPossibleVariance);
    }
}