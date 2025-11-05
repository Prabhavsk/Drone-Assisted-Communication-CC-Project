# Drone-Assisted Communication Simulation

This project simulates dynamic drone-assisted communication scenarios, focusing on advanced game-theoretic algorithms for load balancing and resource allocation. It models a heterogeneous network (HetNet) where mobile Unmanned Aerial Vehicles (UAVs) act as base stations to support fixed ground stations and serve mobile users.

## Core Features

- Simulates a HetNet with **6 Drones**, **4 Ground Stations**, and **50-200 Mobile Users**
- Implements **4 Game-Theoretic algorithms** for drone deployment and load balancing
- Includes **6 Baseline algorithms** for performance comparison
- Runs a comprehensive batch of **200 simulation permutations** (10 algorithms × 5 scenarios × 4 user counts)
- Generates detailed CSV reports, analysis files, and performance charts

## Algorithms Implemented

This simulation compares the performance of ten different algorithms.

### Game-Theoretic Algorithms

1. **Nash Equilibrium**: Based on a Potential Game framework using Best Response Dynamics
2. **Stackelberg Game**: A Leader-Follower model for sequential optimization
3. **Cooperative Game**: Models Coalition Formation and uses the Shapley Value for fair allocation
4. **Auction-Based**: Utilizes a Vickrey-Clarke-Groves (VCG) mechanism for efficient allocation

### Baseline Algorithms

1. Random Assignment
2. Round-Robin
3. Greedy Assignment
4. Nearest-Neighbor
5. Load-Balanced (Simple)
6. Signal-Strength Based

## Simulation Environment

The simulation is configured via the `default.properties` file.

### Network Entities

- **DroneBaseStation.java**: 6 mobile UAVs that model energy drain
- **GroundBaseStation.java**: 4 fixed terrestrial base stations
- **MobileUser.java**: 50, 100, 150, or 200 mobile users with varying mobility
- **Position3D.java**: Handles all 3D and 2D distance calculations

### Simulation Scenarios

The algorithms are tested against 5 distinct scenarios to measure robustness.

| Scenario | Description |
|----------|-------------|
| LOW_MOBILITY | Users are static or near-static |
| HIGH_MOBILITY | Users move according to a mobility model |
| URBAN_HOTSPOT | 70% of users are clustered in a small area |
| MIXED_TRAFFIC | Users have different and mixed traffic patterns |
| ENERGY_CONSTRAINED | Drones have limited battery life |

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or newer
- **Apache Maven**: Used to compile the project, manage dependencies, and execute the simulation

## How to Run

This project is designed for one-command execution. The command will automatically compile the project and run the entire 200-simulation experiment.

From the project's root directory, run:

```bash
mvn clean verify
```

** Warning**: This is a computationally intensive process. It executes 200 separate 1-hour (3600-timestep) simulations, for a total of 720,000 timesteps. This will take a significant amount of time and will generate all result files in the `results/` directory upon completion.

## Outputs and Analysis

### Output File Structure

All outputs are saved to the `results/` directory, which is organized as follows:

- `results/csv/`: Contains the primary data output, including `simulation_results_<timestamp>.csv`
- `results/charts/`: Contains auto-generated PNG charts comparing algorithm performance
- `results/analysis/`: Contains text files with a summary and detailed analysis

### Key Performance Indicators (KPIs)

The simulation collects a wide range of metrics via `MetricsCollector.java`. The final CSV report provides the following:

| Metric | Unit | Goal |
|--------|------|------|
| Throughput | Mbps | Higher |
| Latency | ms | Lower |
| Total Energy | Joules (J) | Lower |
| Load Variance | 0.0 - 1.0 | Higher |
| User Satisfaction | % | Higher |
| QoS Violations | % | Lower |

### Success Indicators

A simulation run is considered fully successful if it meets all the following criteria:

1. The `mvn clean verify` command terminates with a "BUILD SUCCESS" message
2. The file `results/csv/simulation_results_<timestamp>.csv` exists
3. The CSV file contains 201 lines (1 header row + 200 data rows)
4. The `results/charts/` directory contains ≥ 5 non-empty PNG files
