package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * VCG auction helper.
 *
 * Collect valuations, run a simple winner-determination and compute Vickrey prices.
 * Used as a clear, truthful baseline for experiments.
 */
public class VCGAuctionMechanism {
    
    private List<DroneBaseStation> droneStations;
    private List<GroundBaseStation> groundStations;
    private List<MobileUser> users;
    
    public static class VCGResult {
        public final Map<Object, Set<MobileUser>> assignments;      // Winner allocations
        public final Map<MobileUser, Double> prices;                // Vickrey prices
        public final Map<MobileUser, Double> valuations;            // User valuations
        public final double totalWelfare;                           // Sum of valuations
        public final double totalRevenue;                           // Sum of prices
        
        public VCGResult(Map<Object, Set<MobileUser>> assignments, 
                        Map<MobileUser, Double> prices,
                        Map<MobileUser, Double> valuations,
                        double welfare, double revenue) {
            this.assignments = assignments;
            this.prices = prices;
            this.valuations = valuations;
            this.totalWelfare = welfare;
            this.totalRevenue = revenue;
        }
    }
    
    public VCGAuctionMechanism(List<DroneBaseStation> droneStations,
                             List<GroundBaseStation> groundStations,
                             List<MobileUser> users) {
        this.droneStations = new ArrayList<>(droneStations);
        this.groundStations = new ArrayList<>(groundStations);
        this.users = new ArrayList<>(users);
    }
    
    /**
     * Run VCG auction mechanism
     * 
     * @return VCGResult with winner assignments and Vickrey prices
     */
    public VCGResult runAuction() {
        // STEP 1: Collect valuations (bids) from all users for all stations
        Map<MobileUser, Map<Object, Double>> valuations = collectUserValuations();
        
        // STEP 2: Winner determination - maximize social welfare
        WinnerDeterminationResult wdResult = solveWinnerDetermination(valuations);
        
        // STEP 3: Price calculation - Vickrey prices
        Map<MobileUser, Double> vickreyPrices = calculateVickreyPrices(
            wdResult.winnerAssignments, wdResult.allocatedStations, valuations);
        
        // Calculate metrics
        double totalWelfare = calculateTotalWelfare(wdResult.winnerAssignments, valuations);
        double totalRevenue = vickreyPrices.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        
        return new VCGResult(wdResult.winnerAssignments, vickreyPrices, 
            flattenValuations(valuations), totalWelfare, totalRevenue);
    }
    
    /**
     * STEP 1: Collect valuations from users
     * User i's valuation for station j = data rate they would achieve
     */
    private Map<MobileUser, Map<Object, Double>> collectUserValuations() {
        Map<MobileUser, Map<Object, Double>> valuations = new HashMap<>();
        
        List<Object> allStations = new ArrayList<>();
        allStations.addAll(droneStations);
        allStations.addAll(groundStations);
        
        for (MobileUser user : users) {
            Map<Object, Double> userValuations = new HashMap<>();
            
            for (Object station : allStations) {
                // Valuation = achievable data rate (higher is better)
                double valuation = calculateUserValuationForStation(user, station);
                userValuations.put(station, valuation);
            }
            
            valuations.put(user, userValuations);
        }
        
        return valuations;
    }
    
    /**
     * Calculate user's valuation for a station (achievable data rate)
     */
    private double calculateUserValuationForStation(MobileUser user, Object station) {
        if (!isValidAssignment(user, station)) {
            return 0.0; // User out of range
        }
        
        // Get achievable rate through this station
        if (station instanceof DroneBaseStation) {
            DroneBaseStation dbs = (DroneBaseStation) station;
            A2GChannelModel.A2GChannelResult channel = A2GChannelModel.calculateA2GChannel(user, dbs);
            double snr = A2GChannelModel.calculateSNR(0.02, channel.channelGain, dbs.getBandwidth());
            return dbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
        } else if (station instanceof GroundBaseStation) {
            GroundBaseStation gbs = (GroundBaseStation) station;
            double channelGain = A2GChannelModel.calculateUEToMBSChannelGain(user, gbs);
            double snr = A2GChannelModel.calculateSNR(0.02, channelGain, gbs.getBandwidth());
            return gbs.getBandwidth() * Math.log(1 + snr) / Math.log(2);
        }
        
        return 0.0;
    }
    
    /**
     * STEP 2: Winner Determination
     * Find allocation maximizing social welfare Σ bid[i][j]
     * Subject to: each user assigned to at most one station, capacity constraints
     */
    private WinnerDeterminationResult solveWinnerDetermination(
            Map<MobileUser, Map<Object, Double>> valuations) {
        
        List<Object> allStations = new ArrayList<>();
        allStations.addAll(droneStations);
        allStations.addAll(groundStations);
        
        Map<Object, Set<MobileUser>> assignments = new HashMap<>();
        for (Object station : allStations) {
            assignments.put(station, new HashSet<>());
        }
        
        Set<Object> allocatedStations = new HashSet<>();
        
        // Greedy allocation: sort users by highest valuation, assign sequentially
        List<MobileUser> sortedUsers = users.stream()
            .sorted((u1, u2) -> {
                double maxVal1 = valuations.get(u1).values().stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);
                double maxVal2 = valuations.get(u2).values().stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);
                return Double.compare(maxVal2, maxVal1); // Descending
            })
            .collect(Collectors.toList());
        
        for (MobileUser user : sortedUsers) {
            // Find best station for this user
            Object bestStation = null;
            double bestValuation = 0.0;
            
            for (Object station : allStations) {
                if (!hasCapacity(station, assignments)) continue;
                
                double valuation = valuations.get(user).getOrDefault(station, 0.0);
                if (valuation > bestValuation) {
                    bestValuation = valuation;
                    bestStation = station;
                }
            }
            
            if (bestStation != null && bestValuation > 0) {
                assignments.get(bestStation).add(user);
                allocatedStations.add(bestStation);
            }
        }
        
        return new WinnerDeterminationResult(assignments, allocatedStations);
    }
    
    /**
     * STEP 3: Vickrey Price Calculation
     * Winner i pays the value they displaced (next-best allocation without them)
     */
    private Map<MobileUser, Double> calculateVickreyPrices(
            Map<Object, Set<MobileUser>> winnerAssignments,
            Set<Object> allocatedStations,
            Map<MobileUser, Map<Object, Double>> valuations) {
        
        Map<MobileUser, Double> prices = new HashMap<>();
        
        // For each winner, calculate price
        for (MobileUser winner : users) {
            boolean isWinner = winnerAssignments.values().stream()
                .anyMatch(set -> set.contains(winner));
            
            if (!isWinner) {
                prices.put(winner, 0.0);
                continue;
            }
            
            // Find which station this user won at
            Object winningStation = null;
            for (Object station : winnerAssignments.keySet()) {
                if (winnerAssignments.get(station).contains(winner)) {
                    winningStation = station;
                    break;
                }
            }
            
            // Get this user's valuation at winning station
            double myValuation = valuations.get(winner).get(winningStation);
            
            // Find highest valuation of displaced user at this station
            double displacedValuation = 0.0;
            for (MobileUser other : users) {
                if (other.equals(winner)) continue;
                if (valuations.get(other).get(winningStation) > displacedValuation) {
                    displacedValuation = valuations.get(other).get(winningStation);
                }
            }
            
            // Vickrey price: pay the next-highest valuation
            prices.put(winner, displacedValuation);
        }
        
        return prices;
    }
    
    /**
     * Calculate total welfare (sum of valuations of winners)
     */
    private double calculateTotalWelfare(
            Map<Object, Set<MobileUser>> winnerAssignments,
            Map<MobileUser, Map<Object, Double>> valuations) {
        double totalWelfare = 0.0;
        
        for (Object station : winnerAssignments.keySet()) {
            for (MobileUser user : winnerAssignments.get(station)) {
                totalWelfare += valuations.get(user).getOrDefault(station, 0.0);
            }
        }
        
        return totalWelfare;
    }
    
    // Helper methods
    private boolean isValidAssignment(MobileUser user, Object station) {
        if (station instanceof DroneBaseStation) {
            return ((DroneBaseStation) station).isUserInRange(user);
        } else if (station instanceof GroundBaseStation) {
            return ((GroundBaseStation) station).isUserInRange(user);
        }
        return false;
    }
    
    private boolean hasCapacity(Object station, Map<Object, Set<MobileUser>> assignments) {
        int currentCount = assignments.getOrDefault(station, new HashSet<>()).size();
        if (station instanceof DroneBaseStation) {
            return currentCount < ((DroneBaseStation) station).getMaxUserCapacity();
        } else if (station instanceof GroundBaseStation) {
            return currentCount < ((GroundBaseStation) station).getMaxUserCapacity();
        }
        return false;
    }
    
    private Map<MobileUser, Double> flattenValuations(
            Map<MobileUser, Map<Object, Double>> valuations) {
        Map<MobileUser, Double> flat = new HashMap<>();
        for (MobileUser user : valuations.keySet()) {
            flat.put(user, valuations.get(user).values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0));
        }
        return flat;
    }
    
    private static class WinnerDeterminationResult {
        final Map<Object, Set<MobileUser>> winnerAssignments;
        final Set<Object> allocatedStations;
        
        WinnerDeterminationResult(Map<Object, Set<MobileUser>> assignments, Set<Object> stations) {
            this.winnerAssignments = assignments;
            this.allocatedStations = stations;
        }
    }
}
