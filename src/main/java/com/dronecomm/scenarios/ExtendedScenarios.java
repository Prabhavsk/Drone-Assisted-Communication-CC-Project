package com.dronecomm.scenarios;

import com.dronecomm.entities.MobileUser;
import com.dronecomm.entities.Position3D;
import java.util.*;

public class ExtendedScenarios {
    
    public enum ScenarioType {
        HIGH_DENSITY_URBAN,
        RURAL_SPARSE,
        HIGHWAY_MOBILITY,
        DISASTER_RECOVERY,
        SPORTS_EVENT,
        FACTORY_IOT,
        SMART_CITY,
        MOUNTAIN_TERRAIN,
        COASTAL_MARINE,
        AIRPORT_COVERAGE,
        CAMPUS_NETWORK,
        EMERGENCY_RESPONSE
    }
    
    public static class ScenarioConfig {
        public final int userCount;
        public final int droneCount;
        public final int mbsCount;
        public final double areaSizeKm;
        public final double maxAltitudeM;
        public final double mobilitySpeedMps;
        public final double dataRateMbps;
        public final String description;
        
        public ScenarioConfig(int userCount, int droneCount, int mbsCount, 
                            double areaSizeKm, double maxAltitudeM, double mobilitySpeedMps,
                            double dataRateMbps, String description) {
            this.userCount = userCount;
            this.droneCount = droneCount;
            this.mbsCount = mbsCount;
            this.areaSizeKm = areaSizeKm;
            this.maxAltitudeM = maxAltitudeM;
            this.mobilitySpeedMps = mobilitySpeedMps;
            this.dataRateMbps = dataRateMbps;
            this.description = description;
        }
    }
    
    public static Map<ScenarioType, ScenarioConfig> getExtendedScenarios() {
        Map<ScenarioType, ScenarioConfig> scenarios = new HashMap<>();
        
        scenarios.put(ScenarioType.HIGH_DENSITY_URBAN, new ScenarioConfig(
            500, 15, 8, 2.0, 200, 5, 50, "Dense urban environment with skyscrapers"
        ));
        
        scenarios.put(ScenarioType.RURAL_SPARSE, new ScenarioConfig(
            80, 6, 3, 10.0, 300, 15, 25, "Rural area with sparse population"
        ));
        
        scenarios.put(ScenarioType.HIGHWAY_MOBILITY, new ScenarioConfig(
            300, 12, 5, 15.0, 150, 30, 100, "Highway with high-speed vehicles"
        ));
        
        scenarios.put(ScenarioType.DISASTER_RECOVERY, new ScenarioConfig(
            250, 20, 2, 5.0, 100, 2, 10, "Post-disaster emergency communications"
        ));
        
        scenarios.put(ScenarioType.SPORTS_EVENT, new ScenarioConfig(
            2000, 25, 6, 1.0, 120, 1, 200, "Stadium with massive user concentration"
        ));
        
        scenarios.put(ScenarioType.FACTORY_IOT, new ScenarioConfig(
            1000, 8, 4, 0.5, 50, 5, 5, "Industrial IoT with many low-data devices"
        ));
        
        scenarios.put(ScenarioType.SMART_CITY, new ScenarioConfig(
            800, 18, 10, 8.0, 180, 12, 75, "Smart city with mixed traffic patterns"
        ));
        
        scenarios.put(ScenarioType.MOUNTAIN_TERRAIN, new ScenarioConfig(
            120, 10, 3, 20.0, 500, 8, 20, "Mountainous terrain with altitude challenges"
        ));
        
        scenarios.put(ScenarioType.COASTAL_MARINE, new ScenarioConfig(
            200, 12, 4, 25.0, 100, 20, 30, "Coastal area with marine vehicles"
        ));
        
        scenarios.put(ScenarioType.AIRPORT_COVERAGE, new ScenarioConfig(
            600, 16, 8, 3.0, 80, 25, 150, "Airport with flight restrictions"
        ));
        
        scenarios.put(ScenarioType.CAMPUS_NETWORK, new ScenarioConfig(
            1500, 14, 7, 4.0, 100, 3, 80, "University campus network"
        ));
        
        scenarios.put(ScenarioType.EMERGENCY_RESPONSE, new ScenarioConfig(
            350, 22, 1, 6.0, 200, 10, 40, "Emergency response scenario"
        ));
        
        return scenarios;
    }
    
    public static List<MobileUser> generateScenarioUsers(ScenarioType type, ScenarioConfig config) {
        List<MobileUser> users = new ArrayList<>();
        Random random = new Random(42);
        
        double areaMeters = config.areaSizeKm * 1000;
        
        for (int i = 0; i < config.userCount; i++) {
            double x, y, z = 1.5;
            
            switch (type) {
                case HIGH_DENSITY_URBAN:
                    x = generateClusteredPosition(random, areaMeters, 0.7);
                    y = generateClusteredPosition(random, areaMeters, 0.7);
                    break;
                    
                case HIGHWAY_MOBILITY:
                    x = random.nextDouble() * areaMeters;
                    y = random.nextGaussian() * 50 + areaMeters/2;
                    break;
                    
                case SPORTS_EVENT:
                    double radius = 200 + random.nextGaussian() * 50;
                    double angle = random.nextDouble() * 2 * Math.PI;
                    x = areaMeters/2 + radius * Math.cos(angle);
                    y = areaMeters/2 + radius * Math.sin(angle);
                    break;
                    
                case DISASTER_RECOVERY:
                    if (random.nextDouble() < 0.8) {
                        x = random.nextGaussian() * 300 + areaMeters/2;
                        y = random.nextGaussian() * 300 + areaMeters/2;
                    } else {
                        x = random.nextDouble() * areaMeters;
                        y = random.nextDouble() * areaMeters;
                    }
                    break;
                    
                default:
                    x = random.nextDouble() * areaMeters;
                    y = random.nextDouble() * areaMeters;
                    break;
            }
            
            x = Math.max(0, Math.min(areaMeters, x));
            y = Math.max(0, Math.min(areaMeters, y));
            
            Position3D position = new Position3D(x, y, z);
            double dataRate = config.dataRateMbps * (0.5 + random.nextDouble());
            double velocity = config.mobilitySpeedMps * random.nextDouble();
            
            MobileUser user = new MobileUser(position, MobileUser.MovementPattern.RANDOM_WALK);
            user.setDataRate(dataRate);
            user.setMovementSpeed(velocity);
            users.add(user);
        }
        
        return users;
    }
    
    private static double generateClusteredPosition(Random random, double areaSize, double clusterFactor) {
        if (random.nextDouble() < clusterFactor) {
            int clusters = 3;
            int selectedCluster = random.nextInt(clusters);
            double clusterCenter = (selectedCluster + 0.5) * areaSize / clusters;
            return Math.max(0, Math.min(areaSize, 
                clusterCenter + random.nextGaussian() * areaSize * 0.1));
        } else {
            return random.nextDouble() * areaSize;
        }
    }
    
    public static class TrafficPattern {
        public final double peakHourMultiplier;
        public final double burstProbability;
        public final double baseLatencyMs;
        public final int priorityLevel;
        
        public TrafficPattern(double peakMultiplier, double burstProb, 
                            double latency, int priority) {
            this.peakHourMultiplier = peakMultiplier;
            this.burstProbability = burstProb;
            this.baseLatencyMs = latency;
            this.priorityLevel = priority;
        }
    }
    
    public static Map<String, TrafficPattern> getTrafficPatterns() {
        Map<String, TrafficPattern> patterns = new HashMap<>();
        
        patterns.put("VIDEO_STREAMING", new TrafficPattern(3.0, 0.1, 50, 2));
        patterns.put("VOICE_CALL", new TrafficPattern(1.5, 0.8, 10, 1));
        patterns.put("FILE_DOWNLOAD", new TrafficPattern(2.0, 0.3, 100, 3));
        patterns.put("WEB_BROWSING", new TrafficPattern(1.2, 0.4, 200, 4));
        patterns.put("IOT_SENSOR", new TrafficPattern(1.0, 0.05, 500, 5));
        patterns.put("AUGMENTED_REALITY", new TrafficPattern(4.0, 0.2, 5, 1));
        patterns.put("AUTONOMOUS_VEHICLE", new TrafficPattern(2.5, 0.9, 1, 1));
        patterns.put("EMERGENCY_SERVICE", new TrafficPattern(5.0, 1.0, 1, 0));
        
        return patterns;
    }
    
    public static class WeatherCondition {
        public final double visibilityKm;
        public final double windSpeedMps;
        public final double temperatureC;
        public final double humidityPercent;
        public final String condition;
        
        public WeatherCondition(double visibility, double windSpeed, 
                              double temperature, double humidity, String condition) {
            this.visibilityKm = visibility;
            this.windSpeedMps = windSpeed;
            this.temperatureC = temperature;
            this.humidityPercent = humidity;
            this.condition = condition;
        }
    }
    
    public static List<WeatherCondition> getWeatherConditions() {
        return Arrays.asList(
            new WeatherCondition(15, 5, 20, 60, "CLEAR"),
            new WeatherCondition(8, 12, 15, 80, "RAIN"),
            new WeatherCondition(2, 8, 10, 90, "FOG"),
            new WeatherCondition(10, 20, 25, 40, "WINDY"),
            new WeatherCondition(5, 15, 5, 70, "SNOW"),
            new WeatherCondition(12, 7, 30, 55, "PARTLY_CLOUDY")
        );
    }
}