# Drone Communication Simulation Project

## Overview
This project simulates drone-assisted communication scenarios using advanced algorithms and analysis tools. It includes various modules for channel modeling, load balancing, and game-theoretic approaches.

## Prerequisites
If you do not have Java and Maven installed, follow these steps:

1. **Install Java Development Kit (JDK):**
   - Download the latest JDK from [Oracle](https://www.oracle.com/java/technologies/javase-downloads.html) or [OpenJDK](https://openjdk.org/install/).
   - Follow the installation instructions for your operating system.
   - Verify the installation:
     ```bash
     java -version
     ```

2. **Install Maven:**
   - Download Maven from the [Apache Maven website](https://maven.apache.org/download.cgi).
   - Follow the installation instructions for your operating system.
   - Verify the installation:
     ```bash
     mvn -version
     ```

3. **Set Environment Variables:**
   - Ensure `JAVA_HOME` and `MAVEN_HOME` are set in your system's environment variables.
   - Add `JAVA_HOME/bin` and `MAVEN_HOME/bin` to your system's PATH.

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Prabhavsk/DroneCommProject.git
   ```
2. Navigate to the project directory:
   ```bash
   cd DroneCommProject
   ```
3. Install dependencies using Maven:
   ```bash
   mvn clean install
   ```

## Running the Simulation
1. Compile the project:
   ```bash
   mvn compile
   ```
2. Run the main simulation class:
   ```bash
   mvn exec:java -Dexec.mainClass="com.dronecomm.DroneAssistedCommunicationSimulation"
   ```

If the above command does not work, ensure the `pom.xml` file is correctly configured and all dependencies are installed.

### Example Usage
- After running the simulation, you should see logs indicating the progress of the simulation.
- Results will be saved in the `results/` directory.

### Outputs
The simulation generates the following outputs:

1. **Charts**:
   - Visualizations of communication metrics such as throughput, latency, energy consumption, and QoS violations.
   - Saved as `.png` files in the `results/charts/` directory.

2. **Analysis Reports**:
   - Detailed performance metrics and summaries.
   - Saved as `.txt` or `.csv` files in the `results/analysis/` directory.

3. **Research Paper Outputs**:
   - Figures and tables for research validation.
   - Saved in the `results/research_paper_figures/` directory.

### Troubleshooting
- **Issue**: `mvn exec:java` fails with a `ClassNotFoundException`.
  - **Solution**: Ensure the `pom.xml` file includes the `exec-maven-plugin` and all dependencies are correctly specified.
- **Issue**: Results are not generated.
  - **Solution**: Verify write permissions for the `results/` directory and check the logs for errors.
- **Issue**: Charts are not displayed correctly.
  - **Solution**: Ensure the `results/charts/` directory exists and is writable.

## Results
The simulation validates the research paper's findings on game-theoretic load balancing in drone-assisted communication networks. Key findings include:

- Cooperative Game Theory demonstrated the best overall performance.
- Auction-based approaches excelled in high-density scenarios.
- Energy optimization significantly influenced drone deployment strategies.

### Sample Output
- **Chart**: `results/charts/throughput_comparison.png`
- **Report**: `results/analysis/summary_analysis.txt`
- **Research Figure**: `results/research_paper_figures/Figure1_SystemModel.png`

## Project Structure
- `src/main/java`: Contains the source code for the simulation.
- `lib/`: Includes external libraries required for the project.
- `results/`: Stores output files such as charts, analysis results, and research paper figures.

## Contributing
Feel free to fork the repository and submit pull requests for improvements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.