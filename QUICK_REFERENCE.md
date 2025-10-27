# Quick Reference Guide - One-Page Lookup

## File Locations

### Main Classes
```
src/main/java/com/dronecomm/
├── DroneAssistedCommunicationSimulation.java     Main orchestrator (200 simulations)
├── IntegratedResearchSimulation.java             Alternative entry point
├── algorithms/
│   ├── GameTheoreticLoadBalancer.java            Algorithm dispatcher
│   ├── PSCAAlgorithm.java                        P-SCA (Nash)
│   ├── ExactPotentialGame.java                   Nash via potential game
│   ├── AlphaFairnessLoadBalancer.java            Cooperative game (Shapley)
│   └── BaselineAlgorithms.java                   6 baseline algorithms
├── entities/
│   ├── DroneBaseStation.java                     6 drones
│   ├── GroundBaseStation.java                    4 ground stations
│   ├── MobileUser.java                           50-200 users
│   └── Position3D.java                           3D coordinates
├── analysis/
│   ├── ChartGenerator.java                       5 bar charts
│   ├── DetailedDataCollector.java                Extract metrics
│   ├── MathematicalAnalysis.java                 Complexity analysis
│   ├── ResearchPaperAnalysis.java                Validation
│   ├── ResearchPaperCharts.java                  Publication figures
│   ├── ResultsExporter.java                      CSV/analysis export
│   └── StatisticalValidation.java                Stats tests
├── utils/
│   ├── ConfigurationLoader.java                  Config management
│   ├── MetricsCollector.java                     Real-time metrics
│   ├── ResultsAnalyzer.java                      Result analysis
│   └── ResultsWriter.java                        Results export
├── enums/
│   ├── AlgorithmType.java                        10 algorithms
│   └── ScenarioType.java                         6 scenarios
└── simulation/scenarios/
    └── (scenario-specific files)
```

---

## Key Methods by Class

### DroneAssistedCommunicationSimulation.java
```java
main(String[] args)                      // Entry point - Line 51
testNewResearchComponents()              // Validates components - Line 67
runCompleteSimulation()                  // Runs 200 simulations - Line 116
runSingleSimulation(AlgorithmType)       // One simulation - Line 276
printAlgorithmResults()                  // Store results - Line 1098
```

### MetricsCollector.java
```java
collectMetrics()                         // Collect all metrics
getAverageLatency()                      // Returns avg ms
getAverageThroughput()                   // Returns avg bps
getTotalEnergyConsumed()                 // Returns sum J
getAverageLoadVariance()                 // Returns fairness metric
getAverageUserSatisfaction()             // Returns %
```

### Position3D.java
```java
distanceTo(Position3D other)             // 3D Euclidean distance
distance2DTo(Position3D other)           // 2D ground distance
```

### DroneBaseStation.java
```java
connectUser(MobileUser)                  // Add user to station
disconnectUser(MobileUser)               // Remove user
isUserInRange(MobileUser)                // Coverage check
canServeUser(MobileUser)                 // Capacity/energy check
updateEnergy(deltaTime)                  // Drain battery
getCurrentLoad()                         // Load %
```

### MobileUser.java
```java
updatePosition(deltaTime)                // Move per pattern
generateTraffic(deltaTime)               // Generate data
updateSatisfaction()                     // Calculate satisfaction
isSatisfied()                            // satisfaction > 0.7?
```

---

## Configuration Parameters

### From default.properties
```properties
simulation.time=3600                     # 1 hour
simulation.timestep=1.0                  # 1 second per step
simulation.area=5000                     # 5000×5000m
drone.count=6                            # 6 drones
ground.station.count=4                   # 4 ground stations
user.count=100                           # Base user count
game.type=NASH_EQUILIBRIUM               # Algorithm
cooperation.weight=0.6                   # Cooperation factor
energy.importance=0.3                    # Energy weight
qos.importance=0.7                       # QoS weight
```

### Scenario Variations
```
LOW_MOBILITY         → Static users
HIGH_MOBILITY        → Moving users
URBAN_HOTSPOT        → 70% clustered users
MIXED_TRAFFIC        → Mixed patterns
HOTSPOT_SCENARIO     → Dual hotspots
ENERGY_CONSTRAINED   → Limited drone energy
```

### User Counts (4 per scenario)
```
50 users
100 users
150 users
200 users
```

### Algorithms (10 total)
```
Game-Theoretic (4):
1. Nash Equilibrium          # P-SCA + Exact Potential Game
2. Stackelberg Game          # Leader-follower
3. Cooperative Game          # Shapley value
4. Auction-Based             # VCG mechanism

Baselines (6):
5. Random Assignment
6. Round-Robin
7. Greedy Assignment
8. Nearest-Neighbor
9. Load-Balanced
10. Signal-Strength Based
```

---

## Simulation Execution

### Command
```bash
mvn clean verify              # Full build + run
mvn exec:java -DskipTests     # Run simulation only
```

### Execution Timeline
```
1. Maven clean           [Remove target/]
2. Maven validate        [Check POM]
3. Maven compile         [Compile Java]
4. Maven test            [Run tests - skipped]
5. Maven package         [Create JAR]
6. Maven verify
   └─ exec:java
      └─ DroneAssistedCommunicationSimulation.main()
         ├─ testNewResearchComponents()     [Validate]
         └─ runCompleteSimulation()         [200 simulations]
            ├─ For each of 5 scenarios
            │  └─ For each of 4 user counts
            │     └─ For each of 10 algorithms
            │        └─ Run 3600 timesteps
```

### Total Iterations
```
5 scenarios × 4 user counts × 10 algorithms × 3600 timesteps
= 5 × 4 × 10 × 3600
= 720,000 total timesteps
```

---

## Output Files

### After Running Simulation
```
results/
├── csv/simulation_results_<timestamp>.csv
│   └─ All 200 results in table format
├── analysis/
│   ├─ summary_analysis_<timestamp>.txt
│   ├─ detailed_analysis_<timestamp>.txt
│   └─ research_paper_validation_<timestamp>.txt
├── charts/
│   ├─ throughput_comparison.png
│   ├─ latency_comparison.png
│   ├─ energy_consumption.png
│   ├─ user_satisfaction.png
│   └─ scalability_<timestamp>.png
└── research_paper_figures/
    ├─ Figure1_SystemModel.png
    ├─ Figure2_AlgorithmComparison.png
    └─ Table1_*.txt (research paper tables)
```

---

## Metrics Collected

### Per Simulation (200 results)

| Metric | Range | Unit | Lower/Higher |
|--------|-------|------|--------------|
| Throughput | 50-300 | Mbps | Higher=Better |
| Latency | 20-100 | ms | Lower=Better |
| Energy | 500-2000 | J | Lower=Better |
| Load Balance | 0.0-1.0 | - | Higher=Better |
| User Satisfaction | 0%-100% | % | Higher=Better |
| QoS Violations | 0%-20% | % | Lower=Better |

### Time-Series Data (3600 points per metric)

Each simulation collects:
- latency[0..3600] → Average latency at each second
- throughput[0..3600] → Total throughput at each second
- energy[0..3600] → Cumulative energy used
- load_variance[0..3600] → Load distribution fairness
- satisfaction[0..3600] → % satisfied users

---

## Common Code Patterns

### Create Network
```java
List<DroneBaseStation> drones = new ArrayList<>();
for (int i = 0; i < 6; i++) {
    drones.add(new DroneBaseStation(...));
}

List<GroundBaseStation> groundStations = new ArrayList<>();
for (int i = 0; i < 4; i++) {
    groundStations.add(new GroundBaseStation(...));
}

List<MobileUser> users = new ArrayList<>();
for (int i = 0; i < numUsers; i++) {
    users.add(new MobileUser(position, pattern));
}
```

### Collect Metrics
```java
MetricsCollector collector = new MetricsCollector();
collector.setEntities(drones, groundStations, users);

for (double time = 0; time < 3600; time += 1.0) {
    collector.updateSimulationTime(time);
    // ... simulation step ...
    collector.collectMetrics();
}

double avgLatency = collector.getAverageLatency();
```

### Export Results
```java
ResultsWriter writer = new ResultsWriter("results");
writer.writeMetricsToJson(collector, "metrics.json");
writer.writeMetricsToCsv(collector, "metrics.csv");
writer.writeSummaryReport(collector, "HOTSPOT", "summary.txt");

writer.generateLatencyChart(collector.getLatencyOverTime(), "latency.png");
writer.generateThroughputChart(collector.getThroughputOverTime(), "throughput.png");
```

---

## Hardcoding Status

### Fixed 
- UE clustering (Line 525)
- Demo random output values (Lines 1200-1400)

### Remaining Issues 
- 6× Math.random() in auction output (Lines 1478, 1483, 1487-1488, 1492-1493)
- 4× Hardcoded satisfaction values (Lines 1037-1040, 1067-1068)

### Required Constants 
- 7× Energy consumption factors 
- 4× Physics constants
- Scenario factors

---

## Algorithms at a Glance

### Complexity Comparison
```
Algorithm           Time        Space       Rate        Guaranteed
────────────────────────────────────────────────────────────────
Nash Eq.            O(n²m)      O(nm)       Geometric   Yes
Stackelberg         O(n³m)      O(nm)       Linear      Yes
Cooperative         O(2^n)      O(2^n)      Depends     No*
Auction-Based       O(n log n)  O(n)        Immediate   Yes
Random              O(n)        O(1)        N/A         -
Greedy              O(nm)       O(1)        N/A         -
```
*Shapley value exists, but multiple equilibria possible

### Game Theory Basis
```
Nash Equilibrium:
  - Potential game
  - Best response dynamics
  - Convergence to equilibrium

Stackelberg Game:
  - Leader-follower
  - Sequential optimization
  - Parameterized by α-fairness

Cooperative Game:
  - Coalition formation
  - Shapley value distribution
  - Fair allocation

Auction-Based:
  - VCG mechanism
  - Truthful bidding
  - Efficiency guaranteed
```

---

## Documentation Cross-Reference

| Topic | File | Lines |
|-------|------|-------|
| **3D Parameterization** | VISUAL_3D_DIAGRAM.md | 250+ |
| **Parameter Roles** | CODE_LOCATIONS_SCENARIO_ALGORITHM.md | 200+ |
| **Quick Reference** | SCENARIO_ALGORITHM_QUICK_ANSWER.md | 100+ |
| **Algorithms** | ALGORITHMS_COMPREHENSIVE.md | 600+ |
| **Execution Flow** | EXECUTION_FLOW_COMPLETE.md | 800+ |
| **Class Reference** | UTILITY_ENTITY_CLASSES_DOCUMENTATION.md | 1500+ |
| **Issues** | HARDCODING_RANDOMIZATION_COMPREHENSIVE_ANALYSIS.md | 200+ |
| **Index** | DOCUMENTATION_INDEX.md | 300+ |
| **This File** | QUICK_REFERENCE.md | 400+ |

---

## Build & Run Checklist

- [ ] Java 21 installed (`java -version`)
- [ ] Maven 3.9+ installed (`mvn -version`)
- [ ] Navigate to project: `cd DroneCommProject`
- [ ] Run build: `mvn clean verify`
- [ ] Check for errors: `mvn clean verify 2>&1 | grep ERROR`
- [ ] Verify output: `ls results/csv/ | head`
- [ ] Check charts: `ls results/charts/ | wc -l` (should be ≥5)
- [ ] Validate analysis: `ls results/analysis/ | wc -l` (should be ≥3)

---

## Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| "No such file or directory: config.json" | Missing config | Copy from src/main/resources |
| "Out of memory" | Large simulation | Increase heap: `export MAVEN_OPTS="-Xmx4g"` |
| "Cannot find symbol: AlgorithmType" | Compile error | Run `mvn clean compile` first |
| "No results in results/" | Simulation didn't run | Check logs: `mvn clean verify 2>&1 \| tail -50` |
| "Throughput is NaN" | Data collection bug | Check MetricsCollector.collectMetrics() |

---

## Performance Metrics (Expected Ranges)

### Throughput (Mbps)
```
Best algorithms (Nash, Cooperative):    150-250 Mbps
Middle algorithms (Stackelberg, Auction): 100-180 Mbps
Baseline algorithms (Random, Greedy):     50-120 Mbps
```

### Latency (ms)
```
Best:     20-50 ms
Good:     50-75 ms
Fair:     75-100 ms
Poor:     >100 ms
```

### Energy (Joules)
```
Efficient:        500-1000 J
Moderate:      1000-1500 J
High:          1500-2000 J
```

### User Satisfaction (%)
```
Excellent:   >85%
Good:        75-85%
Fair:        60-75%
Poor:        <60%
```

---

## Fast Facts

- **Project Size**: ~10,000 lines of Java code
- **Documentation**: ~5,000 lines of Markdown
- **Compilation Time**: ~10-15 seconds
- **Simulation Time**: ~2-5 minutes for all 200 simulations
- **Total Results**: 200 simulation results with 3600 timesteps each
- **Output Size**: 5-10 MB of CSV/JSON/chart data
- **Algorithms**: 4 game-theoretic + 6 baselines = 10 total
- **Scenarios**: 6 different network conditions
- **Scalability**: Tested with 50-200 users

---

## Useful Commands

```bash
# Build and run
mvn clean verify

# Run only (no rebuild)
mvn exec:java -DskipTests

# Compile only
mvn clean compile

# Run with output to file
mvn clean verify > build.log 2>&1

# Check for errors only
mvn clean verify 2>&1 | grep -E "ERROR|FAIL"

# View latest results
head -20 results/csv/simulation_results_*.csv

# Count simulations completed
wc -l results/csv/simulation_results_*.csv

# List all output files
find results -type f | sort
```

---

## Success Indicators

 **Build Success**:
- Exit code 0 from `mvn clean verify`
- Message: "BUILD SUCCESS"

 **Simulation Completed**:
- `results/csv/simulation_results_*.csv` exists
- File has 200 data rows (1 header + 200 simulations)

 **Charts Generated**:
- 5 PNG files in `results/charts/`
- Files have >0 bytes

 **Analysis Complete**:
- `results/analysis/summary_analysis_*.txt` exists
- `results/analysis/detailed_analysis_*.txt` exists

 **Research Validation**:
- `results/analysis/research_paper_validation_*.txt` exists
- Contains "[OK]" checks for methodology

---

This quick reference covers all essential information. For details, see the full documentation index!
