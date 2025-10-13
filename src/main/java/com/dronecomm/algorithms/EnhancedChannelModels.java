package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;

import java.util.Random;

/**
 * Enhanced channel models with realistic fading, interference, and propagation
 */
public class EnhancedChannelModels {
    
    private static final double LIGHT_SPEED = 3e8; // m/s
    private static final double CARRIER_FREQUENCY = 2.4e9; // Hz (2.4 GHz)
    private static final double NOISE_POWER_DBM = -174; // dBm/Hz
    private static final Random random = new Random();
    
    /**
     * Advanced A2G Channel Model with Rician/Rayleigh fading
     */
    public static class AdvancedA2GChannel {
        
        /**
         * Calculates channel gain with Rician fading for LoS and Rayleigh for NLoS
         */
        public static double calculateChannelGain(MobileUser user, DroneBaseStation drone) {
            Position3D userPos = user.getCurrentPosition();
            Position3D dronePos = drone.getCurrentPosition();
            
            double distance = userPos.distanceTo(dronePos);
            double elevationAngle = calculateElevationAngle(userPos, dronePos);
            
            // LoS probability based on ITU-R models
            double losProb = calculateLoSProbability(distance, elevationAngle);
            boolean isLoS = random.nextDouble() < losProb;
            
            // Path loss calculation
            double pathLoss = calculatePathLoss(distance, elevationAngle, isLoS);
            
            // Fading calculation
            double fadingGain = isLoS ? ricianFading(10.0) : rayleighFading(); // 10 dB K-factor for LoS
            
            // Shadow fading
            double shadowFading = generateShadowFading(isLoS ? 4.0 : 8.0); // dB std dev
            
            // Convert to linear scale
            double totalGain = Math.pow(10, -(pathLoss + shadowFading)/10) * fadingGain;
            
            return totalGain;
        }
        
        private static double calculateElevationAngle(Position3D user, Position3D drone) {
            double horizontalDist = Math.sqrt(Math.pow(drone.getX() - user.getX(), 2) + 
                                            Math.pow(drone.getY() - user.getY(), 2));
            double verticalDist = drone.getZ() - user.getZ();
            
            return Math.atan2(verticalDist, horizontalDist) * 180 / Math.PI;
        }
        
        private static double calculateLoSProbability(double distance, double elevationAngle) {
            // ITU-R P.1410 model
            double theta = Math.toRadians(elevationAngle);
            if (theta > Math.toRadians(90)) return 1.0;
            
            // Simplified urban environment model
            return 1.0 / (1.0 + 0.1 * Math.exp(-0.05 * elevationAngle));
        }
        
        private static double calculatePathLoss(double distance, double elevationAngle, boolean isLoS) {
            // Free space path loss
            double fspl = 20 * Math.log10(distance) + 20 * Math.log10(CARRIER_FREQUENCY) - 147.55;
            
            if (isLoS) {
                // Additional LoS loss
                return fspl + 0.5; // dB
            } else {
                // NLoS additional loss
                double nlosLoss = 20 - 12.5 * Math.log10(elevationAngle + 1);
                return fspl + Math.max(0, nlosLoss);
            }
        }
        
        private static double ricianFading(double kFactorDB) {
            double kLinear = Math.pow(10, kFactorDB/10);
            double variance = 1.0 / (2 * (kLinear + 1));
            
            // Generate Rice distributed random variable
            double s = Math.sqrt(kLinear / (kLinear + 1));
            double real = s + Math.sqrt(variance) * random.nextGaussian();
            double imag = Math.sqrt(variance) * random.nextGaussian();
            
            return real * real + imag * imag;
        }
        
        private static double rayleighFading() {
            // Generate Rayleigh distributed random variable
            double u1 = random.nextDouble();
            double u2 = random.nextDouble();
            
            double real = Math.sqrt(-Math.log(u1)) * Math.cos(2 * Math.PI * u2);
            double imag = Math.sqrt(-Math.log(u1)) * Math.sin(2 * Math.PI * u2);
            
            return 0.5 * (real * real + imag * imag);
        }
        
        private static double generateShadowFading(double stdDevDB) {
            return stdDevDB * random.nextGaussian();
        }
    }
    
    /**
     * Interference Models for Multi-Cell Networks
     */
    public static class InterferenceModel {
        
        /**
         * Calculates co-channel interference from other base stations
         */
        public static double calculateCoChannelInterference(MobileUser user, 
                                                          Object servingStation,
                                                          java.util.List<DroneBaseStation> allDrones,
                                                          java.util.List<GroundBaseStation> allGround) {
            double totalInterference = 0.0;
            
            // Interference from other drones
            for (DroneBaseStation drone : allDrones) {
                if (!drone.equals(servingStation)) {
                    double interferenceGain = AdvancedA2GChannel.calculateChannelGain(user, drone);
                    double transmitPower = drone.getBandwidth() * 0.001; // Simplified power model
                    totalInterference += transmitPower * interferenceGain;
                }
            }
            
            // Interference from ground stations
            for (GroundBaseStation ground : allGround) {
                if (!ground.equals(servingStation)) {
                    double distance = user.getCurrentPosition().distance2DTo(ground.getPosition());
                    double pathLoss = 128.1 + 37.6 * Math.log10(distance/1000.0); // Urban macro model
                    double interferenceGain = Math.pow(10, -pathLoss/10);
                    double transmitPower = ground.getBandwidth() * 0.001;
                    totalInterference += transmitPower * interferenceGain;
                }
            }
            
            return totalInterference;
        }
        
        /**
         * Calculates SINR (Signal-to-Interference-plus-Noise Ratio)
         */
        public static double calculateSINR(double signalPower, double interference, double bandwidth) {
            double noisePower = Math.pow(10, NOISE_POWER_DBM/10) * bandwidth * 1e-3; // Convert to W
            return signalPower / (interference + noisePower);
        }
    }
    
    /**
     * 3D Mobility Models with Realistic Trajectories
     */
    public static class Advanced3DMobility {
        
        /**
         * Random Waypoint 3D Model
         */
        public static class RandomWaypoint3D {
            private Position3D currentPosition;
            private Position3D destination;
            private double speed; // m/s
            private double pauseTime; // seconds
            private double currentPauseTime;
            private final double minX, maxX, minY, maxY, minZ, maxZ;
            
            public RandomWaypoint3D(Position3D initialPos, double minX, double maxX, 
                                  double minY, double maxY, double minZ, double maxZ) {
                this.currentPosition = new Position3D(initialPos.getX(), initialPos.getY(), initialPos.getZ());
                this.minX = minX; this.maxX = maxX;
                this.minY = minY; this.maxY = maxY;
                this.minZ = minZ; this.maxZ = maxZ;
                this.speed = 1.0 + random.nextDouble() * 9.0; // 1-10 m/s
                generateNewDestination();
            }
            
            public Position3D updatePosition(double timeStep) {
                if (currentPauseTime > 0) {
                    currentPauseTime -= timeStep;
                    return currentPosition;
                }
                
                double distanceToDestination = currentPosition.distanceTo(destination);
                double moveDistance = speed * timeStep;
                
                if (moveDistance >= distanceToDestination) {
                    // Reached destination
                    currentPosition = new Position3D(destination.getX(), destination.getY(), destination.getZ());
                    pauseTime = random.nextDouble() * 10.0; // 0-10 seconds pause
                    currentPauseTime = pauseTime;
                    generateNewDestination();
                } else {
                    // Move towards destination
                    double ratio = moveDistance / distanceToDestination;
                    double newX = currentPosition.getX() + ratio * (destination.getX() - currentPosition.getX());
                    double newY = currentPosition.getY() + ratio * (destination.getY() - currentPosition.getY());
                    double newZ = currentPosition.getZ() + ratio * (destination.getZ() - currentPosition.getZ());
                    currentPosition = new Position3D(newX, newY, newZ);
                }
                
                return currentPosition;
            }
            
            private void generateNewDestination() {
                double x = minX + random.nextDouble() * (maxX - minX);
                double y = minY + random.nextDouble() * (maxY - minY);
                double z = minZ + random.nextDouble() * (maxZ - minZ);
                destination = new Position3D(x, y, z);
                speed = 1.0 + random.nextDouble() * 9.0; // New random speed
            }
        }
        
        /**
         * Gauss-Markov 3D Mobility Model
         */
        public static class GaussMarkov3D {
            private Position3D currentPosition;
            private double velocityX, velocityY, velocityZ;
            private final double alpha; // Memory parameter (0-1)
            private final double variance;
            private final double minX, maxX, minY, maxY, minZ, maxZ;
            
            public GaussMarkov3D(Position3D initialPos, double alpha, double variance,
                                double minX, double maxX, double minY, double maxY, 
                                double minZ, double maxZ) {
                this.currentPosition = new Position3D(initialPos.getX(), initialPos.getY(), initialPos.getZ());
                this.alpha = alpha;
                this.variance = variance;
                this.velocityX = random.nextGaussian() * Math.sqrt(variance);
                this.velocityY = random.nextGaussian() * Math.sqrt(variance);
                this.velocityZ = random.nextGaussian() * Math.sqrt(variance) * 0.5; // Less vertical movement
                this.minX = minX; this.maxX = maxX;
                this.minY = minY; this.maxY = maxY;
                this.minZ = minZ; this.maxZ = maxZ;
            }
            
            public Position3D updatePosition(double timeStep) {
                // Update velocities with memory
                velocityX = alpha * velocityX + (1 - alpha) * random.nextGaussian() * Math.sqrt(variance);
                velocityY = alpha * velocityY + (1 - alpha) * random.nextGaussian() * Math.sqrt(variance);
                velocityZ = alpha * velocityZ + (1 - alpha) * random.nextGaussian() * Math.sqrt(variance) * 0.5;
                
                // Update position
                double newX = currentPosition.getX() + velocityX * timeStep;
                double newY = currentPosition.getY() + velocityY * timeStep;
                double newZ = currentPosition.getZ() + velocityZ * timeStep;
                
                // Boundary reflection
                if (newX < minX || newX > maxX) {
                    velocityX = -velocityX;
                    newX = Math.max(minX, Math.min(maxX, newX));
                }
                if (newY < minY || newY > maxY) {
                    velocityY = -velocityY;
                    newY = Math.max(minY, Math.min(maxY, newY));
                }
                if (newZ < minZ || newZ > maxZ) {
                    velocityZ = -velocityZ;
                    newZ = Math.max(minZ, Math.min(maxZ, newZ));
                }
                
                currentPosition = new Position3D(newX, newY, newZ);
                return currentPosition;
            }
        }
    }
    
    /**
     * Energy Harvesting Models for Drones
     */
    public static class EnergyHarvestingModel {
        
        /**
         * Solar energy harvesting model
         */
        public static double calculateSolarHarvesting(double timeOfDay, double weather, double panelArea) {
            // Simplified solar model
            double solarIrradiance = 1000; // W/m^2 at peak
            double efficiency = 0.2; // 20% efficiency
            
            // Time-based factor (assuming 12-hour day cycle)
            double timeFactor = Math.max(0, Math.sin(Math.PI * timeOfDay / 12.0));
            
            // Weather factor (0.1 = stormy, 1.0 = clear)
            double weatherFactor = Math.max(0.1, weather);
            
            return panelArea * solarIrradiance * efficiency * timeFactor * weatherFactor;
        }
        
        /**
         * Wind energy harvesting model
         */
        public static double calculateWindHarvesting(double windSpeed, double turbineArea) {
            // Simplified wind model
            double airDensity = 1.225; // kg/m^3
            double efficiency = 0.35; // 35% efficiency
            double cutInSpeed = 3.0; // m/s
            double cutOutSpeed = 25.0; // m/s
            
            if (windSpeed < cutInSpeed || windSpeed > cutOutSpeed) {
                return 0.0;
            }
            
            // Power = 0.5 * density * area * velocity^3 * efficiency
            return 0.5 * airDensity * turbineArea * Math.pow(windSpeed, 3) * efficiency;
        }
    }
    
    /**
     * Traffic Models for Realistic Data Patterns
     */
    public static class TrafficModel {
        
        /**
         * Generates realistic traffic patterns based on time and user type
         */
        public static double generateTrafficDemand(int hour, String userType, double baseDemand) {
            double timeFactor = calculateTimeBasedFactor(hour);
            double typeFactor = calculateUserTypeFactor(userType);
            double randomVariation = 0.8 + 0.4 * random.nextDouble(); // +/-20% variation
            
            return baseDemand * timeFactor * typeFactor * randomVariation;
        }
        
        private static double calculateTimeBasedFactor(int hour) {
            // Peak hours: 8-10 AM, 12-1 PM, 6-8 PM
            if ((hour >= 8 && hour <= 10) || (hour >= 12 && hour <= 13) || (hour >= 18 && hour <= 20)) {
                return 1.5; // 50% increase during peak
            } else if (hour >= 0 && hour <= 6) {
                return 0.3; // 70% decrease during night
            } else {
                return 1.0; // Normal during other hours
            }
        }
        
        private static double calculateUserTypeFactor(String userType) {
            switch (userType.toLowerCase()) {
                case "business": return 1.3;
                case "gaming": return 1.8;
                case "streaming": return 2.0;
                case "iot": return 0.1;
                case "emergency": return 0.5;
                default: return 1.0;
            }
        }
    }
}