package com.dronecomm.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mobile user entity.
 *
 * Models basic user behavior: movement patterns, traffic generation and
 * lightweight QoS metrics used by the load-balancers. Designed to be
 * realistic enough for experiments while remaining fast.
 */
public class MobileUser {
    
    private static int nextId = 10000;
    private static final Random random = new Random();
    
    private final int id;
    private String name;
    private Position3D currentPosition;
    private Position3D targetPosition;
    private double movementSpeed;
    
    // Traffic characteristics
    private double dataRate; // bps - current data generation rate
    private double maxDataRate; // bps - maximum data generation rate
    private List<Double> generatedTraffic; // Simple traffic data amounts
    
    // QoS requirements
    private double maxAcceptableLatency; // milliseconds
    private double minRequiredThroughput; // bps
    private double priorityLevel; // 1.0 = highest, 0.1 = lowest
    
    // Connection state
    private Object connectedBaseStation; // Can be DroneBaseStation or GroundBaseStation
    private boolean isConnected;
    private double connectionQuality; // 0.0 to 1.0
    private double receivedThroughput; // bps
    private double experiencedLatency; // milliseconds
    
    // Movement pattern
    private MovementPattern movementPattern;
    private double movementRadius; // For circular/random movement
    private Position3D homePosition; // Center point for movement
    
    // Performance metrics
    private double totalDataGenerated; // bits
    private double totalDataReceived; // bits
    private double averageLatency; // milliseconds
    private double satisfactionLevel; // 0.0 to 1.0
    private int handoverCount;
    
    public enum MovementPattern {
        STATIC,           // No movement
        RANDOM_WALK,      // Random direction changes
        CIRCULAR,         // Circular movement around home position
        LINEAR,           // Straight line movement
        HOTSPOT_MOBILE    // Movement between hotspot areas
    }
    
    public MobileUser(Position3D initialPosition, MovementPattern pattern) {
        this.id = nextId++;
        this.currentPosition = new Position3D(initialPosition);
        this.homePosition = new Position3D(initialPosition);
        this.targetPosition = new Position3D(initialPosition);
        
        this.movementPattern = pattern;
        this.movementSpeed = 1.0 + random.nextDouble() * 4.0; // 1-5 m/s (walking to running)
        this.movementRadius = 50 + random.nextDouble() * 100; // 50-150m movement radius
        
        // Initialize traffic characteristics
        this.maxDataRate = 1e6 + random.nextDouble() * 9e6; // 1-10 Mbps
        this.dataRate = this.maxDataRate * (0.1 + random.nextDouble() * 0.4); // 10-50% of max initially
        
        // QoS requirements
        this.maxAcceptableLatency = 50 + random.nextDouble() * 100; // 50-150ms
        this.minRequiredThroughput = this.maxDataRate * 0.3; // At least 30% of max data rate
        this.priorityLevel = 0.3 + random.nextDouble() * 0.7; // 0.3-1.0 priority
        
        // Initialize state
        this.isConnected = false;
        this.connectedBaseStation = null;
        this.connectionQuality = 0.0;
        this.generatedTraffic = new ArrayList<>();
        
        // Performance tracking
        this.totalDataGenerated = 0;
        this.totalDataReceived = 0;
        this.averageLatency = 0;
        this.satisfactionLevel = 1.0;
        this.handoverCount = 0;
    }
    
    /**
     * Update position according to the configured movement pattern.
     */
    public void updatePosition(double deltaTime) {
        switch (movementPattern) {
            case STATIC:
                // No movement
                break;
                
            case RANDOM_WALK:
                updateRandomWalkPosition(deltaTime);
                break;
                
            case CIRCULAR:
                updateCircularPosition(deltaTime);
                break;
                
            case LINEAR:
                updateLinearPosition(deltaTime);
                break;
                
            case HOTSPOT_MOBILE:
                updateHotspotMobilePosition(deltaTime);
                break;
        }
    }
    
    private void updateRandomWalkPosition(double deltaTime) {
        if (hasReachedTarget()) {
            // Choose new random target within movement radius from home
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * movementRadius;
            
            double newX = homePosition.getX() + Math.cos(angle) * distance;
            double newY = homePosition.getY() + Math.sin(angle) * distance;
            targetPosition = new Position3D(newX, newY, 0);
        }
        
        moveTowardsTarget(deltaTime);
    }
    
    private void updateCircularPosition(double deltaTime) {
        // Circular movement around home position
        double currentTime = System.currentTimeMillis() / 1000.0;
        double angularSpeed = movementSpeed / movementRadius; // rad/s
        double angle = currentTime * angularSpeed;
        
        double newX = homePosition.getX() + Math.cos(angle) * movementRadius;
        double newY = homePosition.getY() + Math.sin(angle) * movementRadius;
        currentPosition = new Position3D(newX, newY, 0);
    }
    
    private void updateLinearPosition(double deltaTime) {
        if (hasReachedTarget()) {
            // Choose new target in roughly the same direction
            double dx = targetPosition.getX() - currentPosition.getX();
            double dy = targetPosition.getY() - currentPosition.getY();
            double length = Math.sqrt(dx*dx + dy*dy);
            
            if (length > 0) {
                // Continue in same direction
                double newX = currentPosition.getX() + (dx / length) * movementRadius;
                double newY = currentPosition.getY() + (dy / length) * movementRadius;
                targetPosition = new Position3D(newX, newY, 0);
            } else {
                // Choose random direction
                double angle = random.nextDouble() * 2 * Math.PI;
                double newX = currentPosition.getX() + Math.cos(angle) * movementRadius;
                double newY = currentPosition.getY() + Math.sin(angle) * movementRadius;
                targetPosition = new Position3D(newX, newY, 0);
            }
        }
        
        moveTowardsTarget(deltaTime);
    }
    
    private void updateHotspotMobilePosition(double deltaTime) {
        // Movement between predefined hotspot areas
        // Simplified: random walk with bias towards certain areas
        updateRandomWalkPosition(deltaTime);
    }
    
    private void moveTowardsTarget(double deltaTime) {
        if (hasReachedTarget()) {
            return;
        }
        
        double distanceToTarget = currentPosition.distance2DTo(targetPosition);
        double maxDistance = movementSpeed * deltaTime;
        
        if (distanceToTarget <= maxDistance) {
            currentPosition = new Position3D(targetPosition.getX(), targetPosition.getY(), 0);
        } else {
            double ratio = maxDistance / distanceToTarget;
            double newX = currentPosition.getX() + 
                         (targetPosition.getX() - currentPosition.getX()) * ratio;
            double newY = currentPosition.getY() + 
                         (targetPosition.getY() - currentPosition.getY()) * ratio;
            currentPosition = new Position3D(newX, newY, 0);
        }
    }
    
    /**
     * Generates traffic (simple packet-size units) for the current time step.
     */
    public List<Double> generateTraffic(double deltaTime) {
        List<Double> newTraffic = new ArrayList<>();
        
        // Calculate data to generate in this time period
        double bitsToGenerate = dataRate * deltaTime;
        totalDataGenerated += bitsToGenerate;
        
        // Create traffic units representing data packets
        if (bitsToGenerate > 1000) { // At least 1000 bits
            // Convert bits to packets (assume 1500 byte packets)
            double packetSize = 12000; // 1500 bytes = 12000 bits
            int numPackets = (int) Math.ceil(bitsToGenerate / packetSize);
            
            for (int i = 0; i < numPackets; i++) {
                double dataAmount = Math.min(packetSize, bitsToGenerate - i * packetSize);
                newTraffic.add(dataAmount);
                generatedTraffic.add(dataAmount);
            }
        }
        
        return newTraffic;
    }
    
    /**
     * Connect this user to a base station (DBS or GBS). Tracks handovers.
     */
    public void connectToBaseStation(Object baseStation) {
        if (isConnected && connectedBaseStation != baseStation) {
            handoverCount++;
        }
        
        this.connectedBaseStation = baseStation;
        this.isConnected = true;
        updateConnectionQuality();
    }
    
    /**
     * Disconnects from current base station
     */
    public void disconnectFromBaseStation() {
        this.connectedBaseStation = null;
        this.isConnected = false;
        this.connectionQuality = 0.0;
        this.receivedThroughput = 0.0;
    }
    
    /**
     * Updates connection quality based on distance and base station capabilities
     */
    private void updateConnectionQuality() {
        if (!isConnected) {
            connectionQuality = 0.0;
            return;
        }
        
        double distance;
        double maxRange;
        
        if (connectedBaseStation instanceof DroneBaseStation) {
            DroneBaseStation dbs = (DroneBaseStation) connectedBaseStation;
            distance = currentPosition.distanceTo(dbs.getCurrentPosition());
            maxRange = dbs.getCurrentCoverageRadius();
        } else if (connectedBaseStation instanceof GroundBaseStation) {
            GroundBaseStation gbs = (GroundBaseStation) connectedBaseStation;
            distance = currentPosition.distance2DTo(gbs.getPosition());
            maxRange = gbs.getCoverageRadius();
        } else {
            connectionQuality = 0.0;
            return;
        }
        
        // Simple signal strength model
        connectionQuality = Math.max(0.1, 1.0 - (distance / maxRange));
    }
    
    /**
     * Updates user experience metrics
     */
    public void updateExperience(double latency, double throughput) {
        this.experiencedLatency = latency;
        this.receivedThroughput = throughput;
        
        // Update running average
        averageLatency = (averageLatency * 0.9) + (latency * 0.1);
        
        // Calculate satisfaction based on QoS requirements
        double latencySatisfaction = latency <= maxAcceptableLatency ? 1.0 : 
            Math.max(0.0, 1.0 - ((latency - maxAcceptableLatency) / maxAcceptableLatency));
        double throughputSatisfaction = throughput >= minRequiredThroughput ? 1.0 :
            Math.max(0.0, throughput / minRequiredThroughput);
        
        satisfactionLevel = (latencySatisfaction * 0.5) + (throughputSatisfaction * 0.5);
    }
    
    /**
     * Adapts data rate based on network conditions
     */
    public void adaptDataRate() {
        if (isConnected && connectionQuality > 0) {
            // Increase data rate if good connection and low latency
            if (connectionQuality > 0.8 && experiencedLatency < maxAcceptableLatency * 0.5) {
                dataRate = Math.min(maxDataRate, dataRate * 1.1);
            }
            // Decrease data rate if poor connection or high latency
            else if (connectionQuality < 0.5 || experiencedLatency > maxAcceptableLatency) {
                dataRate = Math.max(maxDataRate * 0.1, dataRate * 0.9);
            }
        } else {
            // Not connected - reduce data rate
            dataRate = Math.max(maxDataRate * 0.1, dataRate * 0.8);
        }
    }
    
    private boolean hasReachedTarget() {
        return currentPosition.distance2DTo(targetPosition) < 1.0; // Within 1 meter
    }
    
    // Getters and Setters
    public int getId() { return id; }
    
    public Position3D getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(Position3D position) { this.currentPosition = position; }
    
    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double speed) { this.movementSpeed = speed; }
    
    public double getDataRate() { return dataRate; }
    public void setDataRate(double dataRate) { this.dataRate = dataRate; }
    public void setName(String name) { this.name = name; }
    public double getMaxDataRate() { return maxDataRate; }

    public int getHandoffCount() { 
        // Return actual tracked handover count (deterministic)
        return handoverCount;
    }

    public boolean isQoSViolated() {
        // Check if current performance meets QoS requirements
        return experiencedLatency > maxAcceptableLatency || 
               receivedThroughput < minRequiredThroughput;
    }

    public double getSatisfactionLevel() {
        // Calculate user satisfaction based on actual QoS metrics (deterministic)
        if (isQoSViolated()) {
            // Calculate how badly QoS is violated
            double throughputRatio = Math.min(1.0, receivedThroughput / minRequiredThroughput);
            double latencyRatio = Math.max(0.0, 1.0 - (experiencedLatency / maxAcceptableLatency));
            return 0.3 + (throughputRatio * 0.2) + (latencyRatio * 0.2); // 30-70% based on actual metrics
        } else {
            // Calculate how well QoS requirements are met
            double throughputRatio = Math.min(1.0, receivedThroughput / (minRequiredThroughput * 1.5));
            double latencyRatio = Math.max(0.0, 1.0 - (experiencedLatency / maxAcceptableLatency));
            return 0.7 + (throughputRatio * 0.15) + (latencyRatio * 0.15); // 70-100% based on actual metrics
        }
    }
    
    public double getMaxAcceptableLatency() { return maxAcceptableLatency; }
    public double getMinRequiredThroughput() { return minRequiredThroughput; }
    public double getPriorityLevel() { return priorityLevel; }
    
    public Object getConnectedBaseStation() { return connectedBaseStation; }
    public boolean isConnected() { return isConnected; }
    public double getConnectionQuality() { return connectionQuality; }
    
    public double getExperiencedLatency() { return experiencedLatency; }
    public double getReceivedThroughput() { return receivedThroughput; }
    
    public double getTotalDataGenerated() { return totalDataGenerated; }
    public double getTotalDataReceived() { return totalDataReceived; }
    public int getHandoverCount() { return handoverCount; }
    
    public MovementPattern getMovementPattern() { return movementPattern; }
    public void setMovementPattern(MovementPattern pattern) { this.movementPattern = pattern; }
    
    @Override
    public String toString() {
        return String.format("User[id=%d, pos=%.1f,%.1f, connected=%s, satisfaction=%.2f, data=%.1fMbps]",
            id, 
            currentPosition.getX(), currentPosition.getY(),
            isConnected ? "YES" : "NO",
            satisfactionLevel,
            dataRate / 1e6);
    }
}