package com.dronecomm.enums;

/**
 * Describes the different simulation scenarios used in experiments.
 * Each enum value carries a short human-readable description.
 */
public enum ScenarioType {
    LOW_MOBILITY("Low Mobility - Static and slow moving users"),
    URBAN_HOTSPOT("Urban Hotspot - Dense user concentration"),
    HIGH_MOBILITY("High Mobility - Fast moving users with frequent handoffs"),
    MIXED_TRAFFIC("Mixed Traffic - Various data rate requirements"),
    HOTSPOT_SCENARIO("Hotspot Scenario - Concentrated user areas"),
    ENERGY_CONSTRAINED("Energy Constrained - Limited drone battery capacity");
    
    private final String description;
    
    ScenarioType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}