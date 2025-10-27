# Execution Flow: `mvn clean verify`

## Maven Build Lifecycle Phases

When you run `mvn clean verify`, Maven executes the following phases in order:

```
clean → validate → compile → test → package → verify
```

---

## PHASE 1: CLEAN
**Command**: `mvn clean`

**What happens**:
- Deletes `target/` directory
- All compiled classes removed
- Previous build artifacts cleared

---

## PHASE 2: VALIDATE
**Nothing to validate** - No custom validation rules defined

---

## PHASE 3: COMPILE
**Plugin**: Maven Compiler Plugin (maven-compiler-plugin)

**Actions**:
1. Finds all `.java` files in `src/main/java/`
2. Compiles them using Java 21 compiler
3. Places `.class` files in `target/classes/`
4. Copies resources from `src/main/resources/` to `target/classes/`

**Output**:
```
target/
├── classes/
│   ├── com/
│   │   └── dronecomm/
│   │       ├── algorithms/
│   │       │   ├── PSCAAlgorithm.class
│   │       │   ├── ExactPotentialGame.class
│   │       │   ├── GameTheoreticLoadBalancer.class
│   │       │   ├── AlphaFairnessLoadBalancer.class
│   │       │   ├── VCGAuctionMechanism.class
│   │       │   ├── BaselineAlgorithms.class
│   │       │   └── [... more algorithm classes]
│   │       ├── analysis/
│   │       │   ├── ChartGenerator.class
│   │       │   ├── ResultsExporter.class
│   │       │   └── [... more analysis classes]
│   │       ├── entities/
│   │       ├── enums/
│   │       ├── utils/
│   │       └── DroneAssistedCommunicationSimulation.class
│   └── config.json
│       default.properties
```

---

## PHASE 4: TEST
**Plugin**: Maven Surefire Plugin (maven-surefire-plugin)

**What happens**:
1. Looks for test files in `src/test/java/` matching pattern `Test*.java` or `*Test.java`
2. Runs JUnit tests
3. Generates test reports in `target/surefire-reports/`

**Status**: ⚠️ **No tests found** (no test files in this project)

---

## PHASE 5: PACKAGE
**Plugin**: Maven Assembly Plugin

**Actions**:
1. Creates `.jar` file with all compiled classes
2. Bundles dependencies
3. Creates executable JAR with manifest

**Output**:
```
target/
├── drone-assisted-communication-1.0.0.jar
├── drone-assisted-communication-1.0.0-jar-with-dependencies.jar
```

---

## PHASE 6: VERIFY ← **YOUR SIMULATION RUNS HERE**

**Plugin**: Exec Maven Plugin (exec-maven-plugin)

**Configuration** (from pom.xml):
```xml
<execution>
    <id>run-main-on-verify</id>
    <phase>verify</phase>
    <goals>
        <goal>java</goal>
    </goals>
    <configuration>
        <mainClass>com.dronecomm.DroneAssistedCommunicationSimulation</mainClass>
    </configuration>
</execution>
```

**What happens**:
1. Maven runs the main class: `com.dronecomm.DroneAssistedCommunicationSimulation`
2. Executes `public static void main(String[] args)` method
3. Simulation begins

---

# EXECUTION FLOW: INSIDE THE SIMULATION

## Step 1: main() Entry Point
**File**: `DroneAssistedCommunicationSimulation.java` (Line 51)

```java
public static void main(String[] args) {
    System.out.println("=================================================================");
    System.out.println("   ENHANCED DRONE-ASSISTED COMMUNICATION NETWORK SIMULATION");
    System.out.println("=================================================================");
    
    DroneAssistedCommunicationSimulation simulation = new DroneAssistedCommunicationSimulation();
    
    simulation.testNewResearchComponents();  // ← Step 2
    simulation.runCompleteSimulation();       // ← Step 3
}
```

**What gets instantiated**:
- `ResultsExporter` - Exports results to CSV
- `MetricsCollector` - Collects simulation metrics
- `Random` - Random number generator

---

## Step 2: testNewResearchComponents() (Lines 67-113)
**Purpose**: Verify all research components work correctly

**Creates test entities**:
- `MobileUser` → Tests user movement and data rates
- `DroneBaseStation` → Tests drone functionality
- `GroundBaseStation` → Tests ground station functionality

**Tests components**:
- ✅ A2GChannelModel
- ✅ AFRelayModel
- ✅ AlphaFairnessLoadBalancer
- ✅ PSCAAlgorithm
- ✅ ExactPotentialGame
- ✅ AGCTLBProblemFormulation

**Output**: 
```
Testing New Research Paper Components...
============================================================
Test entities created successfully
   - Mobile User at (1000, 1000, 0)
   - Drone BS at (1200, 1200, 100)
   - Ground BS at (1500, 1500, 10)

NEW RESEARCH COMPONENTS INTEGRATED!
   + A2GChannelModel - Probabilistic channel modeling
   + [... more components listed]
```

---

## Step 3: runCompleteSimulation() (Lines 116-221)
**Purpose**: Execute the main simulation across all scenarios and algorithms

### Sub-Step 3.1: Iterate Through Scenarios
**File**: `DroneAssistedCommunicationSimulation.java` (Lines 128-191)

```
For each ScenarioType (5 scenarios):
  └─ LOW_MOBILITY
  └─ HIGH_MOBILITY
  └─ URBAN_HOTSPOT
  └─ MIXED_TRAFFIC
  └─ ENERGY_CONSTRAINED
     └─ For each user count (4 counts: 50, 100, 150, 200):
         └─ For each AlgorithmType (10 algorithms):
             └─ NASH_EQUILIBRIUM
             └─ STACKELBERG_GAME
             └─ COOPERATIVE_GAME
             └─ AUCTION_BASED
             └─ RANDOM_ASSIGNMENT
             └─ ROUND_ROBIN
             └─ GREEDY_ASSIGNMENT
             └─ NEAREST_NEIGHBOR
             └─ LOAD_BALANCED
             └─ SIGNAL_STRENGTH
```

**Total simulations**: 5 scenarios × 4 user counts × 10 algorithms = **200 simulations**

### Sub-Step 3.2: Create Network Topology
**Files Called**:
- `createGroundStations()` (Lines 369-381) → Creates 4 ground stations
- `createDroneStations()` (Lines 384-400) → Creates 6 drone stations
- `createMobileUsers()` (Lines 403-438) → Creates users based on scenario

**Ground Stations Created** (Line 369+):
```
GBS-1 at (1000, 1000, 0) - Southwest
GBS-2 at (4000, 1000, 0) - Southeast
GBS-3 at (1000, 4000, 0) - Northwest
GBS-4 at (4000, 4000, 0) - Northeast
```

**Drone Stations Created** (Line 384+):
```
DBS-1 to DBS-6 at altitude 150m
Positions vary by scenario
```

**Mobile Users Created** (Line 403+):
```
Users: 50, 100, 150, or 200 depending on iteration
Positions: Scenario-dependent (hotspots, random, etc.)
Movement: Scenario-dependent (static, random walk, etc.)
```

### Sub-Step 3.3: Run Simulation for Each Algorithm
**File**: `runSingleSimulationWithTopology()` (Lines 230-267)

**For Game-Theoretic Algorithms** (Nash, Stackelberg, Cooperative, Auction):

```
1. Call GameTheoreticLoadBalancer
   └─ runTimeSteppedSimulation()
```

**For Baseline Algorithms** (Random, Round-Robin, Greedy, etc.):

```
1. Call executeBaselineAlgorithm()
   └─ Call BaselineAlgorithms methods
      └─ BaselineAlgorithms.java
```

---

## Step 4: Deep Dive - runTimeSteppedSimulation()
**File**: `DroneAssistedCommunicationSimulation.java` (Lines 580-650)

**Executes the main time-stepping loop**:

```java
while (currentTime < SIMULATION_TIME) {  // 0 to 3600 seconds
    
    // 4.1: Update User Positions
    updateUserPositions(users, TIME_STEP);
    
    // 4.2: Execute Load Balancing Algorithm
    if (game-theoretic) {
        GameTheoreticLoadBalancer.LoadBalancingResult lbResult = 
            loadBalancer.balanceLoad(...);
    } else {
        BaselineAlgorithms.BaselineResult result = 
            executeBaselineAlgorithm(...);
    }
    
    // 4.3: Update Drone Positions
    updateDronePositions(drones, TIME_STEP);
    
    // 4.4: Update Drone Energy
    updateDroneEnergy(drones, TIME_STEP);
    
    // 4.5: Collect Metrics for This Timestep
    collectTimeStepMetrics(currentTime, drones, grounds, users,
                          lbResult, results, scenario, algorithm);
    
    // 4.6: Increment Time
    currentTime += TIME_STEP;
}
```

**Timeline**: 3600 iterations (one per second) for 1 hour of simulation

### Sub-Step 4.2: Execute Load Balancing Algorithm

#### **If Algorithm is Nash Equilibrium (AGC-TLB from Paper)**:

**File**: `GameTheoreticLoadBalancer.java` (executeNashEquilibriumWithResearchAlgorithms)

```
1. Call PSCAAlgorithm
   ├─ File: PSCAAlgorithm.java
   ├─ Algorithm: Penalty-based Successive Convex Approximation
   ├─ Optimizes: User associations with fixed drone positions
   ├─ Output: x_ij assignments
   └─ Time: ~0.1-0.5 seconds per iteration

2. Call ExactPotentialGame
   ├─ File: ExactPotentialGame.java
   ├─ Algorithm: Constrained Gibbs-Sampling
   ├─ Optimizes: Drone positions with fixed user associations
   ├─ Output: q_j drone positions
   └─ Time: ~0.2-1.0 seconds per iteration

3. Iterate until convergence
   ├─ MAX_ITERATIONS: 50-100
   ├─ Convergence criteria: Objective improvement < threshold
   └─ Total time per algorithm call: ~5-50 seconds
```

#### **If Algorithm is Stackelberg Game**:

**File**: `GameTheoreticLoadBalancer.java` (executeStackelbergGameWithResearchAlgorithms)

```
Phase 1: Leader Optimization (Drones move)
  └─ For each drone j:
     └─ Search nearby positions (grid search)
     └─ Find position that minimizes load

Phase 2: Follower Best Response (Users respond)
  └─ For each user i:
     └─ Calculate achievable rate from each drone
     └─ Assign to drone with best rate

Phase 3: Leader Adaptation
  └─ For iteration 1 to MAX_STACKELBERG_ITERATIONS (5):
     └─ Repeat Phase 1
     └─ Repeat Phase 2
     └─ Check convergence
```

#### **If Algorithm is Cooperative Game**:

**File**: `AlphaFairnessLoadBalancer.java` (executeCooperativeGameStrategy)

```
1. Calculate Shapley Values
   ├─ File: AlphaFairnessLoadBalancer.java
   ├─ For each station i:
   │  └─ Calculate φ_i = fair share
   ├─ Enumerates 2^m coalitions (m = 10 stations)
   └─ Time: O(2^m) ≈ 1-2 seconds

2. Allocate users based on Shapley values
   ├─ Each station gets load proportional to φ_i
   └─ Min-Max fairness: maximize minimum load
```

#### **If Algorithm is Auction-Based (VCG)**:

**File**: `VCGAuctionMechanism.java` (runAuction)

```
1. Collect User Valuations
   ├─ v_ij = achievable rate for user i at station j
   └─ For each (user, station) pair

2. Winner Determination (Greedy)
   ├─ Sort by valuation (descending)
   ├─ For each pair (i,j):
   │  └─ If user i unassigned AND station j has capacity:
   │     └─ Assign user i to station j
   └─ Time: O(n log n) ≈ 10-100ms

3. Calculate Vickrey Prices
   ├─ For each winning user:
   │  └─ Price = social welfare loss caused by winner
   └─ Time: O(n²) worst case

4. Report Allocation
   └─ Assignments and prices
```

#### **If Algorithm is Baseline (Random, Greedy, etc.)**:

**File**: `BaselineAlgorithms.java`

```
RANDOM_ASSIGNMENT:
  └─ For each user:
     └─ Assign to random station

ROUND_ROBIN:
  └─ Cycle through stations

GREEDY_ASSIGNMENT:
  └─ For each user:
     └─ Assign to station with best rate

NEAREST_NEIGHBOR:
  └─ For each user:
     └─ Assign to closest station

LOAD_BALANCED:
  └─ Assign to station with least users

SIGNAL_STRENGTH:
  └─ Assign to station with best channel gain
```

### Sub-Step 4.5: Collect Metrics
**File**: `DroneAssistedCommunicationSimulation.java` (Lines 740-759)

**Metrics calculated each timestep**:

1. `calculateTotalThroughputFromAssignments()` 
   - Sum of achievable rates for all users
   
2. `calculateAverageLatencyFromAssignments()`
   - Queueing delay in buffers
   
3. `calculateTotalEnergyConsumptionFromAssignments()`
   - Power × time for all drones and stations
   
4. `calculateLoadBalanceIndexFromAssignments()`
   - Fairness of load distribution (std deviation of loads)
   
5. `calculateHandoffRate()`
   - How often users change stations
   
6. `calculateUserSatisfactionFromAssignments()`
   - Percentage meeting QoS requirements

**Storage**:
```java
results.recordTimeStep(currentTime, throughput, latency, energy,
                       loadBalance, handoff, satisfaction);
```

---

## Step 5: Finalize Results
**File**: `DroneAssistedCommunicationSimulation.java` (Line 643+)

```java
results.finalizeResults(SIMULATION_TIME);
// Calculates averages and final statistics
```

**Aggregates 3600 timestep values into**:
- Average throughput
- Average latency
- Total energy consumption
- Load balance index
- User satisfaction percentage

---

## Step 6: Print Algorithm Results
**File**: `DroneAssistedCommunicationSimulation.java` (Lines 1095-1199)

```java
printAlgorithmResults(algorithm, results);
  ├─ Stores results in lastSimulationResults
  ├─ Prints basic metrics
  └─ Calls algorithm-specific detail printer:
     ├─ printNashEquilibriumResearchDetails()
     ├─ printStackelbergGameResearchDetails()
     ├─ printCooperativeGameResearchDetails()
     └─ printAuctionBasedResearchDetails()
```

**Each calls helper methods like**:
- `getConstraintSatisfactionValue()` → Uses lastSimulationResults
- `getLosPathLossValue()` → Uses lastSimulationResults
- `getAlphaParameterValue()` → Uses lastSimulationResults
- etc.

---

## Step 7: Export Results
**File**: `DroneAssistedCommunicationSimulation.java` (Line 217)

```java
resultsExporter.exportSimulationResults(allResults);
```

**File**: `ResultsExporter.java`

**Creates output files**:
```
results/
├── csv/
│   └── simulation_results_[timestamp].csv
├── analysis/
│   ├── summary_analysis_[timestamp].txt
│   ├── detailed_analysis_[timestamp].txt
│   └── research_paper_validation_[timestamp].txt
└── charts/
    ├── throughput_comparison_[timestamp].png
    ├── latency_comparison_[timestamp].png
    ├── energy_consumption_[timestamp].png
    ├── user_satisfaction_[timestamp].png
    ├── scalability_[timestamp].png
    └── algorithm_performance_[timestamp].png
```

---

## Step 8: Generate Research Paper Analysis
**File**: `DroneAssistedCommunicationSimulation.java` (Lines 222-223)

```java
com.dronecomm.analysis.ResearchPaperAnalysis paperAnalysis = 
    new com.dronecomm.analysis.ResearchPaperAnalysis();
paperAnalysis.generateResearchPaperComparison(allResults);
```

**File**: `ResearchPaperAnalysis.java`

**Validates**:
- Paper's theoretical results vs simulation
- Algorithm convergence
- Performance metrics match expected ranges
- Comparison with baseline schemes

---

## Step 9: Generate Final Report
**File**: `DroneAssistedCommunicationSimulation.java` (Line 225)

```java
generateFinalReport();
```

**Creates comprehensive summary**:
- Best algorithm for each metric
- Scenario-specific analysis
- Overall conclusions

---

# Complete Execution Timeline

```
┌─────────────────────────────────────────────────────────────┐
│ mvn clean verify                                            │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ Maven: clean (delete target/)                              │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ Maven: compile (javac to target/classes/)                  │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ Maven: test (no tests found, skip)                         │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ Maven: package (create JAR)                                │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ Maven: verify → Run main class                             │
│ DroneAssistedCommunicationSimulation.main()                │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 1. Print header & create simulation instance              │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. testNewResearchComponents()                             │
│    - Create test entities                                 │
│    - Test all research components                         │
│    - Print validation results                             │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. runCompleteSimulation() - MAIN LOOP                     │
│    200 simulations (5 scenarios × 4 user counts × 10 algos)│
└─────────────────────────────────────────────────────────────┘
        ↓
    ┌───────────────────────────────────────┐
    │ For each scenario:                     │
    │   └─ For each user count:              │
    │      └─ For each algorithm:            │
    │         ├─ Create network topology     │
    │         ├─ Run 3600-second simulation  │
    │         │  (1 timestep per second)    │
    │         ├─ Collect metrics each step   │
    │         ├─ Store results               │
    │         └─ Print results & details     │
    └───────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Export all results to CSV                              │
│    File: results/csv/simulation_results_[timestamp].csv    │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Generate charts (if JFreeChart available)              │
│    File: results/charts/*.png                             │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Generate research paper analysis                        │
│    File: results/analysis/research_paper_validation_*.txt  │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Generate final summary report                           │
│    File: results/analysis/summary_analysis_[timestamp].txt │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. Print completion message                                │
│    "SIMULATION COMPLETE"                                   │
│    "Results exported to 'results' directory"              │
└─────────────────────────────────────────────────────────────┘
```

---

# Key Java Files and Their Roles

| File | Role | Called From |
|------|------|-------------|
| `DroneAssistedCommunicationSimulation.java` | Main entry point, orchestrates simulation | main() entry |
| `GameTheoreticLoadBalancer.java` | Executes game-theoretic algorithms (Nash, Stackelberg, etc.) | DroneAssistedCommunicationSimulation.runTimeSteppedSimulation() |
| `PSCAAlgorithm.java` | Solves user association sub-problem | GameTheoreticLoadBalancer (Nash) |
| `ExactPotentialGame.java` | Solves DBS deployment sub-problem | GameTheoreticLoadBalancer (Nash) |
| `VCGAuctionMechanism.java` | Implements auction-based algorithm | GameTheoreticLoadBalancer |
| `AlphaFairnessLoadBalancer.java` | Implements cooperative game with Shapley values | GameTheoreticLoadBalancer |
| `BaselineAlgorithms.java` | Implements 6 baseline algorithms | DroneAssistedCommunicationSimulation.runTimeSteppedSimulation() |
| `ResultsExporter.java` | Exports results to CSV | DroneAssistedCommunicationSimulation.runCompleteSimulation() |
| `ChartGenerator.java` | Generates visualization charts | DroneAssistedCommunicationSimulation.runCompleteSimulation() |
| `ResearchPaperAnalysis.java` | Validates simulation vs paper | DroneAssistedCommunicationSimulation.runCompleteSimulation() |
| `A2GChannelModel.java` | Air-to-ground channel modeling | GameTheoreticLoadBalancer, BaselineAlgorithms |
| `AFRelayModel.java` | Amplify-and-forward relay modeling | GameTheoreticLoadBalancer, BaselineAlgorithms |
| `MetricsCollector.java` | Collects performance metrics | Used by collectors throughout |

---

# Summary

When you run `mvn clean verify`:

1. **Maven clean** → Removes previous builds
2. **Maven compile** → Compiles 29 Java files
3. **Maven test** → No tests found, skipped
4. **Maven package** → Creates JAR file
5. **Maven verify** → Runs main class
   - Executes **200 simulations** (5 scenarios × 4 user counts × 10 algorithms)
   - Each simulation runs **3600 timesteps** (1 hour at 1-second intervals)
   - Collects metrics at each timestep
   - Exports results to CSV, images, and analysis reports
   - Total runtime: **5-30 minutes** depending on system

**Total execution**: ~200 × 3600 = **720,000 timesteps** across all algorithms and scenarios
