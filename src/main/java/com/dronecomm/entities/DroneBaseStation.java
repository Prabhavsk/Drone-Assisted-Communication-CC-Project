package com.dronecomm.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents a Drone Base Station (DBS) in the air-ground collaborative network.
 * 
 * A DBS is a mobile aerial platform that provides wireless communication services
 * to mobile users. It operates at varying altitudes and can dynamically adjust
 * its position to optimize network coverage and load distribution.
 */
public class DroneBaseStation {
    
    private static int nextId = 1;
    
    // Basic properties
    private int id;
    private String name;
    private double mips;
    private int ram;
    private long bandwidth;
    private long storage;
    
    // Physical properties
    private Position3D currentPosition;
    private Position3D targetPosition;
    private double maxSpeed;
    private double maxAltitude;
    private double coverageRadius;
    
    // Energy management
    private double currentEnergyLevel;
    private double maxEnergyCapacity;
    private double energyConsumptionRate;
    
    // Communication and users
    private Set<MobileUser> connectedUsers;
    private double currentLoad;
    private boolean isActive;
    
    // Game theory related
    private double utilityValue;
    private boolean participatingInGame;
    
    public DroneBaseStation(String name, double mips, int ram, long bandwidth, long storage, 
                           Position3D position, double energyCapacity, double coverageRadius) {
        this.id = nextId++;
        this.name = name;
        this.mips = mips;
        this.ram = ram;
        this.bandwidth = bandwidth;
        this.storage = storage;
        this.currentPosition = new Position3D(position.getX(), position.getY(), position.getZ());
        this.targetPosition = new Position3D(position.getX(), position.getY(), position.getZ());
        this.maxEnergyCapacity = energyCapacity;
        this.currentEnergyLevel = energyCapacity;
        this.coverageRadius = coverageRadius;
        this.maxSpeed = 15.0; // 15 m/s typical for drones
        this.maxAltitude = 300.0; // 300m max altitude
        this.energyConsumptionRate = 1.0; // 1 unit per time unit
        this.connectedUsers = new HashSet<>();
        this.currentLoad = 0.0;
        this.isActive = true;
        this.utilityValue = 0.0;
        this.participatingInGame = true;
    }
    
    public DroneBaseStation(double mips, int ram, long bandwidth, long storage) {
        this("Drone-" + nextId, mips, ram, bandwidth, storage, 
             new Position3D(0, 0, 100), 100.0, 300.0); // Increased coverage from 50 to 300 meters
    }
    
    // Position and movement methods
    public void updatePosition(double deltaTime) {
        if (!currentPosition.equals(targetPosition)) {
            double distance = currentPosition.distanceTo(targetPosition);
            double maxDistance = maxSpeed * deltaTime;
            
            if (distance <= maxDistance) {
                currentPosition = new Position3D(targetPosition.getX(), 
                                               targetPosition.getY(), 
                                               targetPosition.getZ());
            } else {
                double ratio = maxDistance / distance;
                double newX = currentPosition.getX() + 
                             (targetPosition.getX() - currentPosition.getX()) * ratio;
                double newY = currentPosition.getY() + 
                             (targetPosition.getY() - currentPosition.getY()) * ratio;
                double newZ = currentPosition.getZ() + 
                             (targetPosition.getZ() - currentPosition.getZ()) * ratio;
                
                // Ensure altitude constraints
                newZ = Math.max(10, Math.min(maxAltitude, newZ));
                currentPosition = new Position3D(newX, newY, newZ);
            }
        }
    }
    
    public void setTargetPosition(Position3D target) {
        this.targetPosition = new Position3D(target.getX(), target.getY(), 
                                           Math.max(10, Math.min(maxAltitude, target.getZ())));
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
        double distance = currentPosition.distanceTo(user.getCurrentPosition());
        return distance <= coverageRadius;
    }
    
    public boolean canServeUser(MobileUser user) {
        double totalDemand = connectedUsers.stream()
            .mapToDouble(MobileUser::getDataRate)
            .sum() + user.getDataRate();
        return totalDemand <= bandwidth && isActive && currentEnergyLevel > 0;
    }
    
    private void updateLoad() {
        double totalDemand = connectedUsers.stream()
            .mapToDouble(MobileUser::getDataRate)
            .sum();
        this.currentLoad = totalDemand / bandwidth;
    }
    
    // Energy management
    public void updateEnergy(double deltaTime) {
        double consumption = energyConsumptionRate * deltaTime * (1 + currentLoad);
        currentEnergyLevel = Math.max(0, currentEnergyLevel - consumption);
        
        if (currentEnergyLevel <= 0) {
            isActive = false;
            connectedUsers.clear();
        }
    }
    
    public void rechargeEnergy(double amount) {
        currentEnergyLevel = Math.min(maxEnergyCapacity, currentEnergyLevel + amount);
        if (currentEnergyLevel > 0) {
            isActive = true;
        }
    }
    
    // Utility calculation for game theory
    public double calculateUtility() {
        if (!isActive) return 0.0;
        
        double loadFactor = 1.0 - currentLoad; // Higher utility for lower load
        double energyFactor = currentEnergyLevel / maxEnergyCapacity;
        double usersFactor = Math.min(1.0, connectedUsers.size() / 10.0); // Optimal around 10 users
        
        utilityValue = (loadFactor * 0.4 + energyFactor * 0.3 + usersFactor * 0.3) * 100;
        return utilityValue;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Position3D getCurrentPosition() { return currentPosition; }
    public Position3D getTargetPosition() { return targetPosition; }
    
    public double getCurrentLoad() { return currentLoad; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public double getCurrentEnergyLevel() { return currentEnergyLevel; }
    public double getMaxEnergyCapacity() { return maxEnergyCapacity; }
    public double getEnergyConsumptionRate() { return energyConsumptionRate; }
    
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
    
    // Additional methods needed by simulation
    public void setCurrentPosition(Position3D position) {
        this.currentPosition = new Position3D(position.getX(), position.getY(), position.getZ());
    }
    
    public List<MobileUser> getCurrentConnectedUsers() { return new ArrayList<>(connectedUsers); }
    
    public int getCurrentConnectedUserCount() { return connectedUsers.size(); }
    
    public double getCurrentLoadPercentage() { return currentLoad * 100; }
    
    public double getTotalEnergyCapacity() { return maxEnergyCapacity; }
    
    public long getMaxUserCapacity() { return 20; } // Assume max 20 users per drone
    
    public double getEnergyPercentage() { return (currentEnergyLevel / maxEnergyCapacity) * 100; }
    
    public double getCurrentCoverageRadius() { return coverageRadius; }
    
    public void updateEnergyConsumption(double deltaTime, double transmittedBits) {
        updateEnergy(deltaTime);
    }
    
    public void setMaxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; }

    public void updateEnergyConsumption(double timeStep) {
        // Calculate energy consumption based on current load and operations
        double baseConsumption = 2.0; // Base consumption per second
        double loadBasedConsumption = getCurrentLoadPercentage() * 0.05; // Load-based consumption
        double totalConsumption = (baseConsumption + loadBasedConsumption) * timeStep;
        
        this.currentEnergyLevel = Math.max(0, this.currentEnergyLevel - totalConsumption);
    }

    public void setCurrentEnergyLevel(double energyLevel) { 
        this.currentEnergyLevel = energyLevel; 
    }
    
    @Override
    public String toString() {
        return String.format("DroneBaseStation{id=%d, name='%s', position=%s, load=%.2f, energy=%.1f/%.1f, users=%d, active=%s}",
                           id, name, currentPosition, currentLoad, currentEnergyLevel, maxEnergyCapacity, 
                           connectedUsers.size(), isActive);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DroneBaseStation that = (DroneBaseStation) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}