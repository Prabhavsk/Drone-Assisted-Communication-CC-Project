package com.dronecomm.entities;

/**
 * Represents a 3D position with x, y coordinates and altitude z.
 * Used for positioning drones, base stations, and mobile users in the simulation.
 */
public class Position3D {
    private double x; // meters
    private double y; // meters
    private double z; // altitude in meters
    
    public Position3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Position3D(Position3D other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    
    /**
     * Calculates Euclidean distance to another position
     */
    public double distanceTo(Position3D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    /**
     * Calculates 2D distance (ignoring altitude)
     */
    public double distance2DTo(Position3D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx*dx + dy*dy);
    }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position3D position = (Position3D) obj;
        return Double.compare(position.x, x) == 0 &&
               Double.compare(position.y, y) == 0 &&
               Double.compare(position.z, z) == 0;
    }
    
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}