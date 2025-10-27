DroneAssistedCommunicationSimulation.main()
        ↓
1. testNewResearchComponents()  (validate research components work)
        ↓
2. runCompleteSimulation()  (MAIN LOOP - 200 simulations)
        ↓
   For 5 Scenarios × 4 User Counts × 10 Algorithms:
        ├─ Create network topology
        │  ├─ createGroundStations() → 4 ground base stations
        │  ├─ createDroneStations() → 6 drone base stations
        │  └─ createMobileUsers() → 50/100/150/200 users
        │
        ├─ Run time-stepped simulation (3600 seconds)
        │  ├─ Update user positions
        │  ├─ Execute load balancing algorithm:
        │  │  ├─ GameTheoreticLoadBalancer (for Nash/Stackelberg/Cooperative/Auction)
        │  │  │  ├─ PSCAAlgorithm (user association)
        │  │  │  ├─ ExactPotentialGame (drone positioning)
        │  │  │  ├─ VCGAuctionMechanism (auction)
        │  │  │  ├─ AlphaFairnessLoadBalancer (cooperative)
        │  │  │  └─ GameTheoreticLoadBalancer.Stackelberg (leader-follower)
        │  │  └─ BaselineAlgorithms (for 6 baseline algorithms)
        │  ├─ Update drone positions & energy
        │  └─ Collect metrics each timestep
        │
        └─ Store results for this simulation
        ↓
3. Export results to CSV (results/csv/)
        ↓
4. Generate charts (results/charts/)
        ↓
5. Generate research paper analysis (results/analysis/)
        ↓
6. Print final report