package com.dronecomm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Load simulation configuration from JSON or properties files.
 * Falls back to sensible defaults when files are missing or invalid.
 */
public class ConfigurationLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    
    private JsonNode config;
    private Properties properties;
    
    public ConfigurationLoader() {
        loadDefaultConfiguration();
    }
    
    public ConfigurationLoader(String configFilePath) {
        loadConfiguration(configFilePath);
    }
    
    private void loadDefaultConfiguration() {
        try {
            // Load default properties from classpath
            properties = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("default.properties");
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                logger.warn("Default properties file not found, using hardcoded defaults");
                setHardcodedDefaults();
            }
            
            // Load default JSON config
            ObjectMapper mapper = new ObjectMapper();
            InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("config.json");
            if (jsonStream != null) {
                config = mapper.readTree(jsonStream);
            } else {
                logger.warn("Default config.json not found, using empty configuration");
                config = mapper.createObjectNode();
            }
            
        } catch (IOException e) {
            logger.error("Failed to load default configuration", e);
            setHardcodedDefaults();
        }
    }
    
    private void loadConfiguration(String configFilePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readTree(new File(configFilePath));
            logger.info("Loaded configuration from: {}", configFilePath);
        } catch (IOException e) {
            logger.error("Failed to load configuration from: {}", configFilePath, e);
            loadDefaultConfiguration();
        }
    }
    
    private void setHardcodedDefaults() {
        properties = new Properties();
        properties.setProperty("simulation.time", "3600");
        properties.setProperty("simulation.timestep", "1.0");
        properties.setProperty("simulation.area", "5000");
        properties.setProperty("drone.count", "4");
        properties.setProperty("ground.station.count", "4");
        properties.setProperty("user.count", "100");
        properties.setProperty("game.type", "NASH_EQUILIBRIUM");
        properties.setProperty("cooperation.weight", "0.6");
        properties.setProperty("energy.importance", "0.3");
        properties.setProperty("qos.importance", "0.7");
    }
    
    // Getter methods for configuration values
    public double getSimulationTime() {
        return getDoubleProperty("simulation.time", 3600.0);
    }
    
    public double getTimeStep() {
        return getDoubleProperty("simulation.timestep", 1.0);
    }
    
    public int getSimulationArea() {
        return getIntProperty("simulation.area", 5000);
    }
    
    public int getDroneCount() {
        return getIntProperty("drone.count", 4);
    }
    
    public int getGroundStationCount() {
        return getIntProperty("ground.station.count", 4);
    }
    
    public int getUserCount() {
        return getIntProperty("user.count", 100);
    }
    
    public String getGameType() {
        return getStringProperty("game.type", "NASH_EQUILIBRIUM");
    }
    
    public double getCooperationWeight() {
        return getDoubleProperty("cooperation.weight", 0.6);
    }
    
    public double getEnergyImportance() {
        return getDoubleProperty("energy.importance", 0.3);
    }
    
    public double getQosImportance() {
        return getDoubleProperty("qos.importance", 0.7);
    }
    
    // Helper methods
    private String getStringProperty(String key, String defaultValue) {
        if (properties != null && properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return defaultValue;
    }
    
    private int getIntProperty(String key, int defaultValue) {
        try {
            if (properties != null && properties.containsKey(key)) {
                return Integer.parseInt(properties.getProperty(key));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property {}, using default", key);
        }
        return defaultValue;
    }
    
    private double getDoubleProperty(String key, double defaultValue) {
        try {
            if (properties != null && properties.containsKey(key)) {
                return Double.parseDouble(properties.getProperty(key));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid double value for property {}, using default", key);
        }
        return defaultValue;
    }
    
    public JsonNode getConfig() {
        return config;
    }
    
    public Properties getProperties() {
        return properties;
    }
}