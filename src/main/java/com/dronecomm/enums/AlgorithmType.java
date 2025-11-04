package com.dronecomm.enums;

/**
 * Enumerates the implemented algorithms (both game-theoretic and baselines)
 * used in experiments. Each entry includes a human-friendly display name.
 */
public enum AlgorithmType {
    // Game-theoretic algorithms
    NASH_EQUILIBRIUM("Nash Equilibrium"),
    STACKELBERG_GAME("Stackelberg Game"),
    COOPERATIVE_GAME("Cooperative Game"),
    AUCTION_BASED("Auction-based"),
    
    // Baseline algorithms for comparison
    RANDOM_ASSIGNMENT("Random Assignment"),
    ROUND_ROBIN("Round Robin"),
    GREEDY_ASSIGNMENT("Greedy Assignment"),
    NEAREST_NEIGHBOR("Nearest Neighbor"),
    LOAD_BALANCED("Load Balanced"),
    SIGNAL_STRENGTH("Signal Strength Based");
    
    private final String displayName;
    
    AlgorithmType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isGameTheoretic() {
        return this == NASH_EQUILIBRIUM || this == STACKELBERG_GAME || 
               this == COOPERATIVE_GAME || this == AUCTION_BASED;
    }
    
    public boolean isBaseline() {
        return !isGameTheoretic();
    }
}