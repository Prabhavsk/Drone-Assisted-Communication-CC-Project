package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;

/**
 * Air-to-ground channel helpers.
 *
 * Provides simple, reproducible functions for LoS probability, path loss and SNR/SINR
 * calculations used across the simulator.
 */
public class A2GChannelModel {
    
    // LOS probability regression model coefficients
    private static final double B1 = 9.61;
    private static final double B2 = 0.21;
    private static final double ZETA_LOS = 1.0;
    private static final double ZETA_NLOS = 20.0;
    private static final double CARRIER_FREQ = 2.4e9; // 2.4 GHz
    private static final double SPEED_OF_LIGHT = 3e8;
    private static final double NOISE_POWER_DBM = -110;
    
    public static class A2GChannelResult {
        public final double pathLoss;
        public final double channelGain;
        public final double elevationAngle;
        public final double losProb;
        public final double nlosProb;
        
        public A2GChannelResult(double pathLoss, double channelGain, double elevationAngle, 
                               double losProb, double nlosProb) {
            this.pathLoss = pathLoss;
            this.channelGain = channelGain;
            this.elevationAngle = elevationAngle;
            this.losProb = losProb;
            this.nlosProb = nlosProb;
        }
    }
    
    /**
     * Calculate air-to-ground channel characteristics from user to drone.
     * Computes probabilistic path loss based on elevation angle with LOS/NLOS modeling.
     */
    public static A2GChannelResult calculateA2GChannel(MobileUser ue, DroneBaseStation dbs) {
        Position3D uePos = ue.getCurrentPosition();
        Position3D dbsPos = dbs.getCurrentPosition();
        
        double distance3D = uePos.distanceTo(dbsPos);
        
        // Calculate elevation angle between user and drone
        double heightDiff = dbsPos.getZ() - uePos.getZ();
        double elevationAngle = Math.asin(heightDiff / distance3D);
        double elevationAngleDegrees = Math.toDegrees(elevationAngle);
        
        // LOS probability function based on elevation angle
        double losProb = 1.0 / (1.0 + B1 * Math.exp(-B2 * (elevationAngleDegrees - B1)));
        double nlosProb = 1.0 - losProb;
        
        // Free-space path loss with LoS/NLoS fading
        double freeSpacePathLoss = 20 * Math.log10(4 * Math.PI * CARRIER_FREQ * distance3D / SPEED_OF_LIGHT);
        double losPathLoss = freeSpacePathLoss + ZETA_LOS;
        double nlosPathLoss = freeSpacePathLoss + ZETA_NLOS;
        
        // Expected path loss as weighted combination of LOS and NLOS
        double totalPathLoss = losProb * losPathLoss + nlosProb * nlosPathLoss;
        double channelGain = Math.pow(10, -totalPathLoss / 10.0);
        
        return new A2GChannelResult(totalPathLoss, channelGain, elevationAngle, losProb, nlosProb);
    }
    
    /**
     * Calculate drone-to-MBS (Macro Base Station) channel gain assuming line-of-sight.
     */
    public static double calculateDBSToMBSPathLoss(DroneBaseStation dbs, GroundBaseStation mbs) {
        Position3D dbsPos = dbs.getCurrentPosition();
        Position3D mbsPos = mbs.getPosition();
        
        double distance = dbsPos.distanceTo(mbsPos);
        double freeSpacePathLoss = 20 * Math.log10(4 * Math.PI * CARRIER_FREQ * distance / SPEED_OF_LIGHT);
        double losPathLoss = freeSpacePathLoss + ZETA_LOS;
        
        return Math.pow(10, -losPathLoss / 10.0);
    }
    
    /**
     * Calculate user-to-MBS channel gain for ground direct transmission.
     */
    public static double calculateUEToMBSChannelGain(MobileUser ue, GroundBaseStation mbs) {
        Position3D uePos = ue.getCurrentPosition();
        Position3D mbsPos = mbs.getPosition();
        
        double distance2D = uePos.distance2DTo(mbsPos);
        double delta = 1.0; // Path loss constant
        double kappa = 2.0;  // Path loss exponent
        
        return delta * Math.pow(distance2D, -kappa);
    }
    
    /**
     * Calculate Signal-to-Noise Ratio (SNR) for a given channel.
     */
    public static double calculateSNR(double transmitPower, double channelGain, double bandwidth) {
        double noisePowerWatts = Math.pow(10, NOISE_POWER_DBM / 10.0 - 3) * bandwidth;
        double receivedPower = transmitPower * channelGain;
        return receivedPower / noisePowerWatts;
    }
    
    /**
     * Calculate Signal-to-Interference-plus-Noise Ratio (SINR).
     */
    public static double calculateSINR(double signalPower, double interferencePower, double bandwidth) {
        double noisePowerWatts = Math.pow(10, NOISE_POWER_DBM / 10.0 - 3) * bandwidth;
        return signalPower / (interferencePower + noisePowerWatts);
    }
    
    public static double getB1() { return B1; }
    public static double getB2() { return B2; }
    public static double getZetaLoS() { return ZETA_LOS; }
    public static double getZetaNLoS() { return ZETA_NLOS; }
    public static double getNoisePowerDBM() { return NOISE_POWER_DBM; }
}