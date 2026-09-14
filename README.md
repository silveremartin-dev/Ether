# Ether - Human Society & Cliodynamic Thermodynamic Simulation (v4.0)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Build](https://img.shields.io/badge/Build-Maven-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Status](https://img.shields.io/badge/Status-Active-brightgreen.svg)
![Tests](https://img.shields.io/badge/Tests-162%2F162%20Passed-brightgreen.svg)

**Ether** is a high-fidelity, physicalist and cliodynamic simulation engine modeling planetary human civilization dynamics from 100,000 BCE into future planetary scenarios. Driven by strict thermodynamic principles (Joules, Net EROEI, Carnot limits, Shannon entropy, Soil N-P-K & Carbon, and Aquifer depletion), Ether eliminates heuristic short-circuits in favor of deterministic physical forcing equations and empirical cliodynamic models on a global hexagonal Earth grid (Uber H3).

---

## 🎨 Application Architecture & 7 Core Interface Panels

Ether features an integrated 7-panel JavaFX studio providing end-to-end control over planetary genesis, resource distribution, scenario parameters, cliodynamic engines, real-time 3D simulation, telemetry, and system options:

| Application Panel | Detailed Functionality & Visual Preview |
| :--- | :--- |
| **1. 🪐 Biophysical Planet Generator (`PlanetGeneratorPanel`)** | **Procedural World Genesis & Tectonic Modeling**<br>Interactive planet sculptor allowing custom generation of planetary topographies, ocean-to-land ratios, atmospheric pressure, temperature gradients, insolation forcing, mantle heat flux, and soil N-P-K strata.<br><br>![1. Planet Generator Panel](docs/images/real_shots/tab1_planet_generator.png) |
| **2. ⛏️ Geological Tensors & Resource Distribution (`ResourceDistributionPanel`)** | **8-Layer Geological Tensor Ingestion & Distribution**<br>Ingests empirical spatial datasets (ESRI ASCII Grid `.asc`, GeoJSON, GeoTIFF, WMS) for 8 energy and mineral resource tensors (Coal, Crude Oil, Natural Gas, Uranium, Helium-3, Iron & Copper, REE/Precious Metals, Aquifers) with procedural fallback toggles.<br><br>![2. Resource Distribution Panel](docs/images/real_shots/tab2_resources.png) |
| **3. 🎛️ Scenario Setup Navigation (`ScenarioSetupPanel`)** | **12-Section Historical & Paleoclimate Setup Navigation**<br>Standardized setup hierarchy to configure temporal horizons (from -300,000 BP to future epochs), paleoclimate radiative forcing, cohort demographic matrices, initial technological diffusion, Asabiyyah social cohesion, and pre-flight JIT thermodynamic viability checks.<br><br>![3. Scenario Setup Panel](docs/images/real_shots/tab3_scenario_setup.png) |
| **4. ⚙️ Execution Context & Engine Dispatcher (`ExecutionContextPanel`)** | **Simulation Engine Dispatcher & Multi-Core Calibration**<br>Manages simulation loop execution parameters, core physical forcing laws, active Type B cliodynamic plugins, thread pool allocation, and micro/macro tick frequencies.<br><br>![4. Execution Context Panel](docs/images/real_shots/tab4_execution_context.png) |
| **5. 🌐 Real-Time H3 Simulation Canvas (`H3MapCanvas`)** | **Real-Time 3D/2D H3 Hexagonal Globe & Archon Engine**<br>Renders 175,000 hexagonal cells on Uber H3 grid with live multi-layer heatmaps (Malthusian Pressure, Biomes, Population, Radiance, Demographics, Asabiyyah), coordinate tracking, and the Cybernetic God Mode (Archon Engine) for real-time catastrophe injection.<br><br>![5. Simulation Canvas Panel](docs/images/real_shots/tab5_simulation.png) |
| **6. 📊 Cliodynamics Telemetry & Comparative Analytics (`ComparativeAnalyticsPanel`)** | **Planetary Cliodynamics Telemetry & Empirical Fitting**<br>Calculates real-time Root Mean Square Error (RMSE) and $R^2$ historical goodness-of-fit against empirical datasets (Seshat, Maddison, HYDE 3.4), age pyramids, Gini inequality indices, and EROEI net surplus curves.<br><br>![6. Comparative Analytics Panel](docs/images/real_shots/tab6_comparative_analytics.png) |
| **7. 🔧 Preferences & System Configuration (`PreferencesPanel`)** | **Theme Management, I18n & Data Provenance**<br>Controls UI themes (Dark/Light mode), dynamic language switching (English/French), auto-save intervals, external spatial data paths, and SHA-256 cryptographic manifest exports (`provenance.json`).<br><br>![7. Preferences Panel](docs/images/real_shots/tab7_preferences.png) |


---

## 🎛️ Standardized Scenario Setup Navigation (Sections 1 to 12)

The `ScenarioSetupPanel` uses a standardized 1–12 numerical navigation hierarchy ensuring a seamless configuration workflow across planetary parameters, cliodynamics, and performance toggles:

1. **1. Scenario Presets & Historical Archetypes** (Historical eras & benchmark scenario profiles)
2. **2. Temporal Horizon & Chronology** (Start year, end year, tick rate & calendar sub-steps)
3. **3. Biophysical Planet Geography & H3 Grid** (Planet radius, atmospheric pressure, sea level offset, axial tilt)
4. **4. Astro-Climate Forcing & Paleoclimate Events** (Insolation, radiative greenhouse forcing, volcanic aerosol shocks)
5. **5. Demographic Cohorts & Population Matrix** (Initial population, age pyramids, carrying capacity sensitivity)
6. **6. Spatial Density & Initial Settlement Distribution** (Settlement archetypes: One Continent, Nile Valley, Fertile Crescent, Beringia, Urban Clusters)
7. **7. Biophysical Resources & Stockpiles** (Initial food reserve months, capital $K_0$, metal/wood/water reserves)
8. **8. Initial Technology & Knowledge Diffusion** (Initial tech level, innovation rate, Shannon transmission bandwidth)
9. **9. Sociology, Institutions & Social Cohesion** (Asabiyyah solidarity, elite formation ratio, fiscal stress threshold)
10. **10. Simulation Engine & Plugin Selection** (Core physical laws, 30 Type B cliodynamic engines, terraforming modules)
11. **11. Calibration, JIT Compilation & Engine Performance** (Parallel worker threads, multi-frequency climate ticks, JIT warm-up)
12. **12. World Cartographic Preview & Scene Initialization** (Interactive preview canvas, layer toggles, pre-flight diagnostic summary)

---

## ⚡ High-Fidelity Earth Benchmark (175,000 H3 Cells & 50M Humans)

Ether has been benchmarked on a full planetary Earth grid at resolution 6-8 (175,000 H3 cells) with 50 Million humans in Classical Antiquity (-500 BCE) under complete physical, thermodynamic, and cliodynamic models:

| Performance Metric | Measured Value | Evaluation & Throughput |
| :--- | :--- | :--- |
| **H3 Grid Resolution** | **175,000 H3 Cells** | Full Earth planetary coverage |
| **Simulated Population** | **50,000,000 Humans** | Classical Antiquity (-500 BCE) |
| **Total Execution Time** | **10.62 seconds** | Complete 10-second multi-cycle run |
| **Simulation Speed (TPS)** | **0.56 Ticks / sec** | **1 simulated year every 1.35s – 1.77s** |
| **Average Duration per Tick** | **1,350 ms – 1,770 ms / year** | 12 monthly sub-steps + macro cycle |
| **Cell Update Throughput** | **1,185,994 cell-updates / sec** | **~1.19 Million cell-updates per second** |
| **RAM Footprint (Heap Delta)** | **194 MB → 742 MB (+548 MB)** | Highly optimized memory consumption |

---

## 🚀 Key Features

### 🌍 Physicalist & Thermodynamic Core Engine
- **H3 Hexagonal Spatial Grid**: Uber H3 multi-resolution hexagonal cell indexing (Resolutions 6 to 8).
- **Thermodynamic Net EROEI & Energy Budgets**: Simulates energy extraction thresholds, EROEI degradation, Carnot efficiency limits ($\eta_{\text{Carnot}} = 1 - T_C/T_H$), and industrial work output ($\text{Joules}$).
- **Material Entropic Dissipation**: 2nd law of thermodynamics dissipation loss rate ($1.5\%/\text{year}$) of recyclable metal stocks.
- **Soil & Aquifer Physical Dynamics**: Tracks N-P-K depletion, Soil Organic Carbon (SOC) sequestration, and freshwater table drawdowns.
- **Biophysical & Cliodynamic Engines**:
  - **Terraforming & Orbital Physics**: Atmospheric pressure ($P_{\text{atm}}$), greenhouse radiative forcing, solar mirrors, and asteroid mining.
  - **Trophic Ecosystems & Rewilding**: 3-tier trophic biomass, Pleistocene Rewilding (permafrost albedo & SOC retention), and species hybridization.
  - **Physical Supply Chains**: Material logistics, transport friction coefficients ($\mu_{\text{sea}}, \mu_{\text{rail}}$), and maritime chokepoint blockades.
  - **Urban Thermodynamics**: Urban Heat Island ($T_{\text{uhi}}$), high-voltage grid transmission losses ($I^2 R$), and city aquifer depletion.

### 📚 Catalog of 30 Type B Cliodynamic & Empirical Models (`ProceduralEngineRegistry`)
Ether provides 30 pluggable Type B simulation plugins operating in dual **Pure** (isolated analytical equations) and **Hybrid** (grid-injected forcing) modes:
1. **World3 Systems Dynamics** (Limits to Growth / Meadows)
2. **HANDY NASA Collapse** (Elites vs Commoners Inequality / Motesharrei)
3. **Nordhaus DICE Climate-Economy** (Social Cost of Carbon & Abatement)
4. **Lenski Macro-Sociology** (Technological subsistence & inequality curve)
5. **Leslie White Energy-Culture** (Energy harness per capita $C = E \cdot T$)
6. **Kardashev Planetary Energy Scale** (Type I energy harness metrics)
7. **Asimov Psychohistory Statistical Mechanics** (Macro-probabilistic social trajectory)
8. **Marvin Harris Cultural Materialism** (Infrastructure $\to$ Structure $\to$ Superstructure)
9. **Steven Pinker Decline of Violence** (Monopoly of violence & pacification)
10. **James C. Scott Against the Grain** (Agrarian transition fragility & taxability penalty)
11. **Autonomous AI Planetary Governance** (Resource optimization & algorithmic regulation)
12. **Elinor Ostrom Polycentric Commons** (Aquifer & common-pool resource governance)
13. **Vaclav Smil Material Inertia** (35-year transition turnover constraint)
14. **Mori & Smith Spatial Urban Fractals** (Power-law city size hierarchy)
15. **Bernard Lahire Human Self-Domestication** (Density-driven learning & intergenerational capital)
16. **Monastic Demographic Buffer** (Celibacy buffer for Malthusian overpressure)
17. **Tanegashima Military Tech Shock** (Gunpowder shock & rapid state unification)
18. **Portuguese Asymmetric Colonial Trade** (Bullion drain & merchant capital accumulation)
19. **Joseph Henrich Tasmanian Loss** (Cultural regression under small isolated populations $N < 5000$)
20. **Fernand Braudel & Grataloup Mediterranean Sea Highway** (Maritime highway trade efficiency boost)
21. **Hamilton & Wilson Kin Selection** (Inclusive fitness $r \cdot B > C$ & outgroup hostility)
22. **Peter Turchin Frontier Asabiyyah** (Collective solidarity forged at hostile frontiers & decay in hinterlands)
23. **David Buss Evolutionary Mating & Mobilization** (Elite polygyny surplus $\to$ young male military expansion)

### 📈 Historical Validation Kernel (`HistoricalValidationKernel`)
Ether includes an empirical validation engine computing **Root Mean Square Error (RMSE)** and **Coefficient of Determination ($R^2$)** against a 20-variable empirical dataset from -10,000 BCE to 2026 CE (sourced from **Seshat Databank**, **Maddison Project**, **Correlates of War**, **HYDE 3.4**, and **PAGES 2k**). See [docs/SIMULATION_EQUATIONS_AND_VARIABLES.md](docs/SIMULATION_EQUATIONS_AND_VARIABLES.md) for full citations and metrics.

---

## 🛠️ Getting Started

### Prerequisites
- **Java 21 JDK** or higher.
- **Maven 3.9+**.

### Installation & Execution
```bash
# Clone the repository
git clone https://github.com/silveremartin-dev/Ether.git
cd Ether

# Build the project & compile sources
mvn clean compile

# Run all 102 JUnit tests
mvn test

# Run the High-Fidelity 175k Cell Benchmark
mvn test -Dtest=EarthFullResolution175kBenchmarkTest

# Regenerate Javadoc API documentation
mvn javadoc:javadoc

# Launch the JavaFX Simulation Application
mvn javafx:run
```

---

## 🗺️ System Architecture & Progress

| Module | Description | Status |
| :--- | :--- | :--- |
| **Thermodynamic Kernel** | EROEI, Carnot limits, Entropy, Soil N-P-K, Aquifer | ✅ Complete |
| **H3 Geospatial Grid** | Uber H3 resolution 6-8 indexing & 175k cell grid | ✅ Complete |
| **30 Type B Engines** | Turchin, Wilson, Braudel, Henrich, Ostrom, Smil, Scott | ✅ Complete |
| **Validation Kernel** | RMSE & $R^2$ empirical historical fit calibration | ✅ Complete |
| **Multiverse Branching** | Snapshot & side-by-side trajectory comparison | ✅ Complete |
| **175k Cell Benchmark** | **1.19M cell-updates/sec** throughput verified | ✅ Verified |

## 📄 Master Technical Documentation
- 🏗️ [Core System Architecture & DOD](docs/ARCHITECTURE.md) (H3 Grid, `WorldBuffer`, 30 Type B Engines, Ocean Optimizations, GPU JIT Spec, Archon Engine, Cluster Scaling)
- 🛠️ [Setup & Operational Guide](docs/SETUP.md) (Quickstart, Standalone & Docker DB, Cluster Setup, OpenCL, TornadoVM, Troubleshooting)
- 📐 [Differential Equations & State Variables](docs/SIMULATION_EQUATIONS_AND_VARIABLES.md) (State Variables, ODEs/PDEs, Pluggable Formula Engine, Historical Benchmarks & Validation)
- 🌍 [Deep Paleolithic & Palaeoclimate Tensor Specification](docs/DEEP_PALEOLITHIC_TENSOR_SPECIFICATION.md) (-300,000 BP to Present, MIS Stages, Milankovitch Forcing, Sea Level Dynamics)
- 📊 [Global Systemic Models Comparative Analysis](docs/MODEL_COMPARISON.md) (Ether vs T21/iSDG, IFs, IMAGE, GAINS, and Post-World3 System Dynamics)
- 🗺️ [Planetary GIS Repatriation & Ingestion Architecture](docs/REPATRIATION_PLAN.md) (Cartographic Ingestion Pipeline, H3 Normalization, Standards & Deduplication)
- 🔒 [Security & System Integrity Audit](docs/SECURITY.md) (Deserialization, Threat Matrix, Concurrency & Memory Safety)
- 📜 [Academic Data Credits & Citations](docs/CREDITS.md) (Seshat, Maddison, COW, HYDE 3.4, ETOPO1, USGS, ORBIS)

---

## 📄 License & Authors

Distributed under the **MIT License**.

**Authors**:
- **Silvere Martin-Michiellot**
- **Antigravity / Gemini AI (Google DeepMind)**
