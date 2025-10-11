package com.dronecomm.algorithms;

import com.dronecomm.entities.DroneBaseStation;
import com.dronecomm.entities.GroundBaseStation;
import com.dronecomm.entities.MobileUser;

public class AFRelayModel {
    
    public static class AFRelayResult {
        public final double directRate;
        public final double relayRate;
        public final double totalRate;
        public final double gamma_is;
        public final double gamma_ij;
        public final double gamma_sj;
        
        public AFRelayResult(double directRate, double relayRate, double totalRate,
                           double gamma_is, double gamma_ij, double gamma_sj) {
            this.directRate = directRate;
            this.relayRate = relayRate;
            this.totalRate = totalRate;
            this.gamma_is = gamma_is;
            this.gamma_ij = gamma_ij;
            this.gamma_sj = gamma_sj;
        }
    }
    
    public static AFRelayResult calculateAFRelayRate(MobileUser ue, DroneBaseStation dbs, 
                                                   GroundBaseStation mbs, double ueTxPower, 
                                                   double dbsTxPower, double bandwidth) {
        
        A2GChannelModel.A2GChannelResult ueToDbsChannel = A2GChannelModel.calculateA2GChannel(ue, dbs);
        double dbsToMbsChannelGain = A2GChannelModel.calculateDBSToMBSPathLoss(dbs, mbs);
        double ueToMbsChannelGain = A2GChannelModel.calculateUEToMBSChannelGain(ue, mbs);
        
        double gamma_ij = A2GChannelModel.calculateSNR(ueTxPower, ueToDbsChannel.channelGain, bandwidth);
        double gamma_sj = A2GChannelModel.calculateSNR(dbsTxPower, dbsToMbsChannelGain, bandwidth);
        double gamma_is = A2GChannelModel.calculateSNR(ueTxPower, ueToMbsChannelGain, bandwidth);
        
        double directRate = bandwidth * Math.log(1 + gamma_is) / Math.log(2);
        
        double afNumerator = gamma_is + (gamma_ij * gamma_sj);
        double afDenominator = 1 + gamma_ij + gamma_sj;
        double relayRate = (bandwidth / 2.0) * Math.log(1 + afNumerator / afDenominator) / Math.log(2);
        
        double totalRate = Math.max(directRate, relayRate);
        
        return new AFRelayResult(directRate, relayRate, totalRate, gamma_is, gamma_ij, gamma_sj);
    }
    
    public static AFRelayResult calculateBestAFRelayRate(MobileUser ue, 
                                                       java.util.List<DroneBaseStation> dbsList,
                                                       GroundBaseStation mbs, double ueTxPower, 
                                                       double dbsTxPower, double bandwidth) {
        AFRelayResult bestResult = null;
        double bestRate = 0.0;
        
        AFRelayResult directOnly = new AFRelayResult(
            bandwidth * Math.log(1 + A2GChannelModel.calculateSNR(ueTxPower, 
                A2GChannelModel.calculateUEToMBSChannelGain(ue, mbs), bandwidth)) / Math.log(2),
            0.0, 0.0, 0.0, 0.0, 0.0
        );
        
        if (directOnly.directRate > bestRate) {
            bestRate = directOnly.directRate;
            bestResult = directOnly;
        }
        
        for (DroneBaseStation dbs : dbsList) {
            AFRelayResult result = calculateAFRelayRate(ue, dbs, mbs, ueTxPower, dbsTxPower, bandwidth);
            
            if (result.totalRate > bestRate) {
                bestRate = result.totalRate;
                bestResult = result;
            }
        }
        
        return bestResult;
    }
    
    public static class TransmissionStrategy {
        public final boolean useRelay;
        public final DroneBaseStation selectedRelay;
        public final double achievableRate;
        
        public TransmissionStrategy(boolean useRelay, DroneBaseStation relay, double rate) {
            this.useRelay = useRelay;
            this.selectedRelay = relay;
            this.achievableRate = rate;
        }
    }
    
    public static TransmissionStrategy getOptimalStrategy(MobileUser ue, 
                                                        java.util.List<DroneBaseStation> dbsList,
                                                        GroundBaseStation mbs, double ueTxPower, 
                                                        double dbsTxPower, double bandwidth) {
        
        double directRate = bandwidth * Math.log(1 + A2GChannelModel.calculateSNR(ueTxPower, 
            A2GChannelModel.calculateUEToMBSChannelGain(ue, mbs), bandwidth)) / Math.log(2);
        
        double bestRelayRate = 0.0;
        DroneBaseStation bestRelay = null;
        
        for (DroneBaseStation dbs : dbsList) {
            AFRelayResult result = calculateAFRelayRate(ue, dbs, mbs, ueTxPower, dbsTxPower, bandwidth);
            
            if (result.relayRate > bestRelayRate) {
                bestRelayRate = result.relayRate;
                bestRelay = dbs;
            }
        }
        
        if (bestRelayRate > directRate && bestRelay != null) {
            return new TransmissionStrategy(true, bestRelay, bestRelayRate);
        } else {
            return new TransmissionStrategy(false, null, directRate);
        }
    }
    
    public static double calculateSINRWithInterference(MobileUser ue, DroneBaseStation servingDbs,
                                                     java.util.List<DroneBaseStation> interferingDbs,
                                                     double txPower, double bandwidth) {
        
        A2GChannelModel.A2GChannelResult signalChannel = A2GChannelModel.calculateA2GChannel(ue, servingDbs);
        double signalPower = txPower * signalChannel.channelGain;
        
        double totalInterference = 0.0;
        for (DroneBaseStation interferer : interferingDbs) {
            if (!interferer.equals(servingDbs)) {
                A2GChannelModel.A2GChannelResult interferenceChannel = A2GChannelModel.calculateA2GChannel(ue, interferer);
                totalInterference += txPower * interferenceChannel.channelGain;
            }
        }
        
        return A2GChannelModel.calculateSINR(signalPower, totalInterference, bandwidth);
    }
    
    public static double calculateOptimalPowerAllocation(MobileUser ue, DroneBaseStation dbs, 
                                                       GroundBaseStation mbs, double maxPower, 
                                                       double bandwidth) {
        
        A2GChannelModel.A2GChannelResult ueToDbsChannel = A2GChannelModel.calculateA2GChannel(ue, dbs);
        double dbsToMbsChannelGain = A2GChannelModel.calculateDBSToMBSPathLoss(dbs, mbs);
        
        double optimalPower = maxPower / 2.0;
        
        double bestRate = 0.0;
        for (double powerRatio = 0.1; powerRatio <= 1.0; powerRatio += 0.1) {
            double uePower = maxPower * powerRatio;
            double dbsPower = maxPower * (1.0 - powerRatio);
            
            AFRelayResult result = calculateAFRelayRate(ue, dbs, mbs, uePower, dbsPower, bandwidth);
            
            if (result.totalRate > bestRate) {
                bestRate = result.totalRate;
                optimalPower = uePower;
            }
        }
        
        return optimalPower;
    }
}