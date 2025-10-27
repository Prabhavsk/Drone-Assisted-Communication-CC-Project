package com.dronecomm.entities;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a Ground Base Station (GBS) in the air-ground collaborative network.
 * 
 * A GBS is a fixed terrestrial infrastructure that provides wireless communication
 * services to mobile users with high reliability and power.
 */
public class GroundBaseStation {
    
    private static int nextId = 1;
    
    // Basic properties
    private int id;
    private String name;
    private double mips;
    private int ram;
    private long bandwidth;
    private long storage;
    
    // Physical properties
    private Position3D position;
    private double coverageRadius;
    private double transmissionPower;
    
    // Communication and users
    private Set<MobileUser> connectedUsers;
    private double currentLoad;
    private boolean isActive;
    
    // Game theory related
    private double utilityValue;
    private boolean participatingInGame;
    
    public GroundBaseStation(String name, double mips, int ram, long bandwidth, long storage, 
                           Position3D position, double coverageRadius) {
        this.id = nextId++;
        this.name = name;
        this.mips = mips;
        this.ram = ram;
        this.bandwidth = bandwidth;
        this.storage = storage;
        this.position = new Position3D(position.getX(), position.getY(), position.getZ());
        this.coverageRadius = coverageRadius;
        this.transmissionPower = 20.0; // Higher power than drones
        this.connectedUsers = new HashSet<>();
        this.currentLoad = 0.0;
        this.isActive = true;
        this.utilityValue = 0.0;
        this.participatingInGame = true;
    }
    
    public GroundBaseStation(double mips, int ram, long bandwidth, long storage) {
        this("Ground-" + nextId, mips, ram, bandwidth, storage, 
             new Position3D(0, 0, 0), 1500.0); // Increased coverage to 1500 meters for better user reach
    }
    
    // User management
    public boolean connectUser(MobileUser user) {
        if (isUserInRange(user) && canServeUser(user)) {
            connectedUsers.add(user);
            updateLoad();
            return true;
        }
        return false;
    }
    
    public void disconnectUser(MobileUser user) {
        connectedUsers.remove(user);
        updateLoad();
    }
    
    public boolean isUserInRange(MobileUser user) {
        if (user == null || user.getCurrentPosition() == null) return false;
        double distance = position.distanceTo(user.getCurrentPosition());
        return distance <= coverageRadius;
    }
    
    public boolean canServeUser(MobileUser user) {
        double totalDemand = connectedUsers.stream()
            .mapToDouble(MobileUser::getDataRate)
            .sum() + user.getDataRate();
        return totalDemand <= bandwidth && isActive;
    }
    
    private void updateLoad() {
        double totalDemand = connectedUsers.stream()
            .mapToDouble(MobileUser::getDataRate)
            .sum();
        this.currentLoad = totalDemand / bandwidth;
    }
    
    // Utility calculation for game theory
    public double calculateUtility() {
        if (!isActive) return 0.0;
        
        double loadFactor = 1.0 - currentLoad; // Higher utility for lower load
        double usersFactor = Math.min(1.0, connectedUsers.size() / 20.0); // Optimal around 20 users
        double stabilityFactor = 1.0; // Ground stations are stable
        
        utilityValue = (loadFactor * 0.5 + usersFactor * 0.3 + stabilityFactor * 0.2) * 100;
        return utilityValue;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Position3D getPosition() { return position; }
    public void setPosition(Position3D position) { this.position = position; }
    
    public double getCurrentLoad() { return currentLoad; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public Set<MobileUser> getConnectedUsers() { return new HashSet<>(connectedUsers); }
    public int getConnectedUserCount() { return connectedUsers.size(); }
    
    public double getCoverageRadius() { return coverageRadius; }
    public void setCoverageRadius(double coverageRadius) { this.coverageRadius = coverageRadius; }
    
    public double getUtilityValue() { return utilityValue; }
    public boolean isParticipatingInGame() { return participatingInGame; }
    public void setParticipatingInGame(boolean participating) { this.participatingInGame = participating; }
    
    public double getMips() { return mips; }
    public int getRam() { return ram; }
    public long getBandwidth() { return bandwidth; }
    public long getStorage() { return storage; }
    public double getTransmissionPower() { return transmissionPower; }
    
    // Additional methods needed by simulation
    public List<MobileUser> getCurrentConnectedUsers() { return new ArrayList<>(connectedUsers); }
    
    public int getCurrentConnectedUserCount() { return connectedUsers.size(); }
    
    public double getCurrentLoadPercentage() { return currentLoad * 100; }
    
    public long getMaxUserCapacity() { return 30; } // Assume max 30 users per ground station
    
    public double calculateUtilityValue() { return calculateUtility(); }
    
    public List<DroneBaseStation> getCollaboratingDrones() { 
        return new ArrayList<>(); // Return empty list for now
    }
    
    @Override
    public String toString() {
        return String.format("GroundBaseStation{id=%d, name='%s', position=%s, load=%.2f, users=%d, active=%s}",
                           id, name, position, currentLoad, connectedUsers.size(), isActive);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroundBaseStation that = (GroundBaseStation) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}