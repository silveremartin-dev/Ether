# Ether - Human Society & Cliodynamic Thermodynamic Simulation

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Rust](https://img.shields.io/badge/Rust-1.80+-red.svg)
![Build](https://img.shields.io/badge/Build-Maven%20%7C%20Cargo-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Status](https://img.shields.io/badge/Status-Active-brightgreen.svg)
![Localization](https://img.shields.io/badge/Languages-EN%20%7C%20FR%20%7C%20DE%20%7C%20ES%20%7C%20ZH-blueviolet.svg)
![Tests](https://img.shields.io/badge/Tests-162%2F162%20Passed-brightgreen.svg)

**Ether** is a high-fidelity, physicalist and cliodynamic simulation engine modeling planetary human civilization dynamics from 100,000 BCE into future planetary scenarios. Driven by strict thermodynamic principles (Joules, Net EROEI, Carnot limits, Shannon entropy, Soil N-P-K & Carbon, and Aquifer depletion), Ether eliminates heuristic shortcuts in favor of deterministic physical forcing equations and empirical cliodynamic models on a global hexagonal grid (Uber H3).

---

## ⚡ Instant Standalone Deployment (1-Click) & 100% Offline Capability

Ether runs out of the box **without requiring any external database configuration or internet connection**:
* **100% Offline Operational**: All core baseline historical scenarios, planet archetypes, ecology models, and cliodynamic equations are embedded directly inside the JAR. You can run, experiment, modify parameters, and save completely offline.
* **Ultra-Lightweight Distribution (~44 MB)**: Portable standalone distribution package (`dist/Ether-v1.0.0-beta.2-standalone.zip`).
* **Non-Blocking Background Sync (Optional)**: When connected to the internet, official extra scenarios, maps (`data/maps/ether/**`), chronicles (`data/events/`), and snapshots (`saves/**`) sync automatically in the background without stalling the interface.
* **Branch & Customization Protection**: Local user edits (changed H3 resolution, cohort size, time steps, custom scenarios, user saves) are protected via SHA-256 and are **never overwritten**. Reference scenarios always remain accessible alongside user custom presets.

```bash
# Windows
run.bat

# Linux
chmod +x run.sh
./run.sh

# macOS (double-click run.command in Finder, or run via terminal)
chmod +x run.sh run.command
./run.command
```

To build a standalone portable release distribution:
* **Standalone Release (.zip + .tar.gz + SHA256)**: `powershell -ExecutionPolicy Bypass -File scripts\build\package-release.ps1 -Platform standalone`

For complete deployment options (standalone, Docker PostGIS, distributed cluster, and headless batch CLI), see [docs/DEPLOYMENT_AND_INFRASTRUCTURE.md](docs/DEPLOYMENT_AND_INFRASTRUCTURE.md).

---

## 🎨 Application Architecture & 7 Core Interface Panels

![Ether Application Showcase](docs/ether_demo.gif)

Ether features an integrated 7-panel JavaFX studio providing end-to-end control over planetary genesis, resource distribution, scenario parameters, cliodynamic engines, real-time 3D simulation, telemetry, and system options:

| Application Panel | Detailed Functionality & Visual Preview |
| :--- | :--- |
| **1. 🪐 Biophysical Planet Generator (`PlanetGeneratorPanel`)** | **Procedural World Genesis & Tectonic Modeling**<br>Interactive planet sculptor allowing custom generation of planetary topographies, ocean-to-land ratios, atmospheric pressure, temperature gradients, insolation forcing, mantle heat flux, and soil N-P-K strata.<br><br>![1. Planet Generator Panel](docs/images/screenshots/tab1_planet_generator.png) |
| **2. ⛏️ Geological Tensors & Resource Distribution (`ResourceDistributionPanel`)** | **8-Layer Geological Tensor Ingestion & Distribution**<br>Ingests empirical spatial datasets (ESRI ASCII Grid `.asc`, GeoJSON, GeoTIFF, WMS) for 8 energy and mineral resource tensors (Coal, Crude Oil, Natural Gas, Uranium, Helium-3, Iron & Copper, REE/Precious Metals, Aquifers) with procedural fallback toggles.<br><br>![2. Resource Distribution Panel](docs/images/screenshots/tab2_resources.png) |
| **3. 🎛️ Scenario Setup Navigation (`ScenarioSetupPanel`)** | **12-Section Historical & Paleoclimate Setup Navigation**<br>Standardized setup hierarchy to configure temporal horizons (from -300,000 BP to future epochs), paleoclimate radiative forcing, cohort demographic matrices, initial technological diffusion, Asabiyyah social cohesion, and pre-flight JIT thermodynamic viability checks.<br><br>![3. Scenario Setup Panel](docs/images/screenshots/tab3_scenario_setup.png) |
| **4. ⚙️ Execution Context & Engine Dispatcher (`ExecutionContextPanel`)** | **Simulation Engine Dispatcher & Multi-Core Hardware Calibration**<br>Manages simulation loop execution parameters, core physical forcing laws, active Type B cliodynamic plugins, thread pool allocation, and micro/macro tick frequencies. Features direct toggling between the **Native Multi-Core Rust Engine**, **Java 21 Incubator Vector SIMD**, **OpenCL GPU Compute Shaders**, and **Deterministic CPU Safe Mode**.<br><br>![4. Execution Context Panel](docs/images/screenshots/tab4_execution_context.png) |
| **5. 🌐 Real-Time H3 Simulation Canvas (`H3MapCanvas`)** | **Real-Time 3D/2D H3 Hexagonal Globe & Archon God Mode Engine**<br>Renders up to 98+ million hexagonal cells on Uber H3 grid with live multi-layer heatmaps (Malthusian Pressure, Biomes, Population, Radiance, Demographics, Asabiyyah), coordinate tracking, and the Cybernetic God Mode (Archon Engine) for real-time catastrophe injection and parameter perturbation.<br><br>![5. Simulation Canvas Panel](docs/images/screenshots/tab5_simulation.png)<br><br>*Cybernetic God Mode (Archon Intervention Console):*<br>![Archon God Mode Panel](docs/images/screenshots/god_mode_panel.png) |
| **6. 📊 Cliodynamics Telemetry & Comparative Analytics (`ComparativeAnalyticsPanel`)** | **Planetary Cliodynamics Telemetry & Empirical Fitting**<br>Calculates real-time Root Mean Square Error (RMSE) and $R^2$ historical goodness-of-fit against empirical datasets (Seshat, Maddison, HYDE 3.4), age pyramids, Gini inequality indices, and EROEI net surplus curves.<br><br>![6. Comparative Analytics Panel](docs/images/screenshots/tab6_comparative_analytics.png) |
| **7. 🔧 Preferences & System Configuration (`PreferencesPanel`)** | **Theme Management, Full i18n & Data Provenance**<br>Controls UI themes (Dark/Light mode), 5-language localization (`EN`, `FR`, `DE`, `ES`, `ZH`), auto-save intervals, external spatial data paths, and SHA-256 cryptographic manifest exports (`provenance.json`).<br><br>![7. Preferences Panel](docs/images/screenshots/tab7_preferences.png) |

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
10. **10. Simulation Engine & Plugin Selection** (Core physical laws, 50+ Type B cliodynamic engines, terraforming modules)
11. **11. Calibration, JIT Compilation & Engine Performance** (Parallel worker threads, multi-frequency climate ticks, JIT warm-up)
12. **12. World Cartographic Preview & Scene Initialization** (Interactive preview canvas, layer toggles, pre-flight diagnostic summary)

---

## 🦀 High-Throughput Native Rust Engine Architecture (`ether-core-native`)

To enable seamless multi-millennial planetary integrations across fine-grained H3 hexagonal grids (up to Resolution 7 with 98.8M cells), Ether features a dedicated **Native Parallel Rust Compute Core** (`native/ether-core-native/` compiling to `ether_core_native.dll` / `libether_core_native.so` / `libether_core_native.dylib`):

### 🏗️ Data-Oriented Design (DOD) & Parallel Kernel Pipeline
* **Flat Memory Contiguity**: Eliminates JVM object-pointer indirection via flat, cache-aligned Structure-of-Arrays (SoA) buffers (`WorldBuffer` and `AgentBuffer`) shared directly across zero-copy FFI boundaries.
* **Rayon Multi-Threaded Work-Stealing**: Dispatches cell chunks across all CPU cores with lock-free parallel iterators.
* **Auto-Vectorized SIMD**: Explicit target CPU feature compilation (`target-cpu=native`, AVX2 / AVX-512) providing vectorized evaluation of thermodynamic state transitions.
* **Bit-Exact IEEE-754 Determinism**: Matches the Java reference implementation with bit-identical fidelity ($\Delta_{\text{IEEE-754}} = 0.0$), guaranteeing scientific reproducibility across local and distributed cluster execution modes.

---

## ⚡ Empirical Planetary Simulation Engine Benchmarks (Ground Truth)

Ether evaluates the **complete planetary simulation pipeline** (`H3SimulationEngine.java` + `SimulationPipeline.java`) executing all 25 geoscientific cartographic raster layers, Tier 1 biophysical conservation PDEs (Liebig N-P-K stoichiometry, 2D Darcy lateral aquifer diffusion, Budyko-Sellers albedo energy balance, megafauna trophic cascades, deforestation root erosion), Data-Oriented Design (DOD) vectorized kernels, exact analytical Gini metrics, and non-blocking asynchronous rolling memory snapshots.

* **1-Click Automation Scripts**: `scripts/benchmark/run-all-benchmarks.bat` (Windows) or `scripts/benchmark/run-all-benchmarks.sh` (Linux/macOS)
* **Direct CLI Execution**: `java -jar society-simulation.jar --benchmark` or `mvn test -Dtest=ComprehensiveRealEngineBenchmarkTest`
* **Official Benchmark Scenario**: `BENCHMARK_STANDARD` (Full planetary Earth or Regional Windowed Cradle, configurable across `--tier1` or `--tier2` via CLI)
* **Exhaustive Report, Audit & Profiling**: [`docs/BENCHMARKS.md`](docs/BENCHMARKS.md)

---

### 📍 Axis 1: Spatial Grid Scaling Matrix (Res 2 to Res 7 Global Earth)

Empirically evaluated on full planetary Earth grids ($\Delta t = 30\text{ days}$, `strictDeterminism = true`):

| Scenario & Grid Level | H3 Resolution | Total Hexagons | Cell Spacing | Engine Mode | 💻 Workstation (4 Cores, 8 GB Heap) | ☁️ GCP VM (`c2d-standard-8`, 24 GB Heap) | Single-Cell Latency (GCP) | 10,000 Ticks Wall-Clock Duration | Snapshot Footprint (24x Cadence) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Global Macro Baseline** | **Res 2** | **5,882** | ~110 km | OOP Engine | **3.94 TPS** (253.8 ms) | **21.49 TPS** (46.5 ms) | 7.91 μs / cell | **7.8 minutes** | **11.8 MB** (0.5 MB / snap) |
| **Earth Global Standard** | **Res 3** | **41,162** | ~42 km | OOP Engine | **1.28 TPS** (784.1 ms) | **9.69 TPS** (103.2 ms) | 2.51 μs / cell | **17.2 minutes** | **82.9 MB** (3.5 MB / snap) |
| **Regional Windowed Cradle** | **Res 5** | **40,305** | ~6 km | OOP Engine | **1.39 TPS** (719.2 ms) | **9.60 TPS** (104.1 ms) | 2.58 μs / cell | **17.4 minutes** | **81.2 MB** (3.4 MB / snap) |
| **Earth Global High-Fi** | **Res 4** | **288,122** | ~16 km | OOP Engine | **0.27 TPS** (3,760.4 ms) | **1.56 TPS** (640.1 ms) | 2.22 μs / cell | **1.8h (106 min)** | **580.3 MB** (24.2 MB / snap) |
| **Earth Global Ultra-Fi** | **Res 5** | **2,016,842** | ~6 km | OOP Engine | *RAM Limit (>16 GB)* | **0.10 TPS** (9,543.3 ms) | 4.73 μs / cell | **26.5h** | **4.06 GB** (169 MB / snap) |
| **Earth Global Sub-Regional** | **Res 6** | **14,117,882** | ~2.3 km | DOD Streaming | **0.05 TPS** (20,468.1 ms) | **0.18 TPS** (5,446.5 ms) | 0.39 μs / cell | **15.1h** | **15.5 GB** (646 MB / snap) |
| **Earth Global High-Density** | **Res 7** | **98,825,162** | ~0.86 km | DOD Streaming | **0.01 TPS** (119,803.1 ms) | **0.03 TPS** (38,248.8 ms) | 0.39 μs / cell | **106.2h** | **108.6 GB** (4.5 GB / snap) |

---

### ⏱️ Axis 2: Temporal Integration Step Resolution Sweep ($\Delta t$)

Evaluated on the standard reference grid (**Earth Global Res 3, 41,162 Hexagons**, Multi-Core):

| Temporal Integration Step ($\Delta t$) | Step Days | Temps Simulé pour 10 000 ticks | 💻 Temps Exécution 10k ticks (Workstation 4C) | ☁️ Temps Exécution 10k ticks (GCP 8 vCPUs) | Measured TPS (GCP) | Latency ms/tick (GCP) | Single-Cell Latency |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Daily** ($\Delta t = 1\text{d}$) | 1.0 day | **27.4 ans** (0.3 siècles) | **53.7 minutes** | **10.4 minutes** | **15.97 TPS** | 62.6 ms | 1.52 μs |
| **Weekly** ($\Delta t = 7\text{d}$) | 7.0 days | **191.6 ans** (0.2 millénaires) | **43.6 minutes** | **8.3 minutes** | **20.07 TPS** | 49.8 ms | 1.21 μs |
| **Monthly** ($\Delta t = 30\text{d}$) | 30.0 days | **821.4 ans** (0.8 millénaires) | **1.2h (70 min)** | **11.6 minutes** | **14.35 TPS** | 69.7 ms | 1.69 μs |
| **Annual** ($\Delta t = 365\text{d}$) | 365.3 days | **10 000.0 ans** (10.0 millénaires) | **1.1h (68 min)** | **16.6 minutes** | **10.05 TPS** | 99.5 ms | 2.42 μs |

---

### ⚙️ Axis 3: Compute Backend Engine Architecture

Measured on **Earth Global Res 3 (41,162 Hexagons)** under identical initial conditions:

| Backend Architecture Mode | Hardware Threads | 💻 Workstation TPS | 💻 Workstation Latency | ☁️ GCP VM TPS | ☁️ GCP VM Latency | Single-Cell Latency (GCP) | Measured Speedup vs 1C (GCP) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Java JIT (Monocœur, 1 Thread)** | 1 Thread | **2.93 TPS** | 341.8 ms | **15.60 TPS** | 64.1 ms | 1,557.5 ns | **1.00x (Baseline)** |
| **Java 21 SIMD Vector (Multicœur)** | 4C (Local) / 8C (GCP) | **5.79 TPS** | 172.6 ms | **20.39 TPS** | 49.0 ms | 1,191.2 ns | **1.31x** |
| **Native Rust Core (AVX-512 + Rayon)** | 4C (Local) / 8C (GCP) | **4.62 TPS** | 216.4 ms | **16.07 TPS** | 62.2 ms | 1,511.8 ns | **1.03x** |

---

### 🔬 Axis 4: Physics Coupling Tiers (Tier 1 vs Tier 1 + Tier 2 Cliodynamics)

Evaluated on **Earth Global Res 3 (41,162 Hexagons)**:

| Coupling Tier Configuration | Tier 2 State | Included Physical & Cliodynamic Sub-Systems | Measured TPS (GCP) | Latency ms/tick (GCP) | Single-Cell Latency | Cliodynamic Overhead vs Tier 1 |
| :--- | :---: | :--- | :---: | :---: | :---: | :---: |
| **Tier 1 Invariant Conservation PDEs Only** | **DISABLED** | Atmospheric radiation, Farquhar photosynthesis, Liebig N-P-K soil dynamics, 2D Darcy aquifer diffusion, megafauna trophic cascades, deforestation root cohesion, DOD demographics/culture. | **18.36 TPS** | 54.5 ms | 1.32 μs | **0.0% (Baseline)** |
| **Tier 1 + Tier 2 Coupled Cliodynamics** | **ENABLED** | Tier 1 physics + Turchin SDT, Tainter collapse, World3 capital-resource feedback, Acemoglu institutions, Dynamic trade networks, War & Diplomacy, Asabiyyah. | **19.83 TPS** | 50.4 ms | 1.23 μs | **< 10%** (Amortized via populated domain partition) |

---

### ⏳ Axis 5: Historical Epochs & Demographic Scale

Measured on **Earth Global Res 3 (41,162 Hexagons)** across 5 landmark historical epochs:

| Historical Epoch Preset | Associated Year | Initial Population ($T_0$) | Initial Tech Level | 💻 Workstation TPS | 💻 Workstation ms/tick | ☁️ GCP VM TPS | ☁️ GCP VM ms/tick | Final Population ($T_{10}$) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Out of Africa** | **100,000 BCE** | **100,000** | 1.0 (Paleolithic) | **7.51 TPS** | 133.2 ms | **15.44 TPS** | 64.8 ms | 100,617 |
| **Neolithic Revolution** | **10,000 BCE** | **5,000,000** | 2.0 (Early Agrarian) | **1.62 TPS** | 617.8 ms | **3.96 TPS** | 252.6 ms | 5,012,907 |
| **Classical Antiquity** | **500 BCE** | **150,000,000** | 3.5 (Agrarian Empires) | **0.06 TPS** | 17,976.4 ms | **0.13 TPS** | 7,496.0 ms | 146,947,099 |
| **Industrial Revolution** | **1800 CE** | **1,000,000,000** | 6.0 (Steam & Coal) | **0.01 TPS** | 73,016.2 ms | **0.02 TPS** | 45,230.5 ms | 949,778,185 |
| **Modern Anthropocene** | **2026 CE** | **8,000,000,000** | 9.5 (Globalized) | **0.05 TPS** | 20,446.7 ms | **0.08 TPS** | 12,232.5 ms | 656,267,804 |

> 💡 **Pure In-Memory DOD Micro-Kernels (`--benchmark-dod`)**:  
> In complete isolation from physical PDE diffusion, cartographic rasters, and disk serialization, pure DOD contiguous array kernels run at **55 to 195 TPS** (Res 2–3) and **7.4 TPS** (Res 4). See [docs/BENCHMARKS.md](docs/BENCHMARKS.md) for the detailed micro-benchmark vs macro-engine distinction and the full code audit.

---

## 🖥️ Command-Line Interface (CLI) & Tab 4 Parity

All execution options configured in **Tab 4 (Execution Context Panel)** are 100% controllable via standard command-line flags:

```bash
# 1. Native Rust Multi-Core execution on H3 Resolution 4 with 8 threads
java -jar society-simulation.jar --engine=rust --threads=8 --res=4 --scenario=INDUSTRIAL --ticks=500

# 2. GPU OpenCL Compute Shaders execution in headless batch mode
java -jar society-simulation.jar --gpu --headless --res=3 --scenario=NEOLITHIC --ticks=1000

# 3. Distributed Cluster execution (Master Orchestrator)
java -jar society-simulation.jar --mode=cluster --role=master --port=9090 --engine=rust --res=4

# 4. Distributed Cluster execution (Worker Agent Node)
java -jar society-simulation.jar --mode=cluster --role=worker --master-host=10.0.0.1 --port=9090

# 5. Single-Core Deterministic Safe Mode
java -jar society-simulation.jar --single-core --safe --res=2 --scenario=OUT_OF_AFRICA
```

| CLI Parameter | Alternative Flag | Description & Tab 4 Equivalent |
| :--- | :--- | :--- |
| `--engine=<rust\|gpu\|simd\|cpu\|safe>` | `-e <type>` | Select compute backend (Native Rust, OpenCL GPU, Java SIMD, CPU JIT, SW Safe) |
| `--rust` / `--native` | `-` | Force Native Rust Core (`ether_core_native.dll`/`.so`/`.dylib` with Rayon + AVX-512) |
| `--gpu` / `--opencl` | `-` | Force OpenCL GPU Compute Shader Pipeline |
| `--simd` / `--vector` | `-` | Force Java 21 Incubator Vector SIMD Engine |
| `--single-core` | `--monocoeur` | Run in single-threaded / single-core mode |
| `--multi-core` | `--multicoeur` | Run in parallel multi-core mode (all available CPUs) |
| `--threads=<N>` / `--cores=<N>` | `-` | Explicitly allocate N CPU worker threads |
| `--mode=<local\|cluster>` | `--cluster` | Select local workstation or distributed network cluster |
| `--role=<master\|worker>` | `-` | Set node role in cluster mode (Default: `master`) |
| `--master-host=<IP>` | `-` | Master IP address or DNS hostname (for worker nodes) |
| `--port=<PORT>` | `-` | Cluster communication port (Default: `9090`) |
| `--res=<2\|3\|4\|5>` | `-r <N>` | Set H3 planetary grid resolution level (Res 2: 5.8k, Res 3: 41k, Res 4: 288k, Res 5: 2.01M cells) |
| `--scenario=<NAME>` | `-s <NAME>` | Historical epoch preset (`OUT_OF_AFRICA`, `NEOLITHIC`, `CLASSICAL`, `INDUSTRIAL`, `MODERN`) |
| `--ticks=<N>` | `-t <N>` | Target simulation tick count |
| `--headless` | `-h` | Run in headless CLI batch mode without JavaFX display overhead |


---

## ☁️ Google Cloud Platform Distributed Multi-Node Benchmarks

Ether includes high-performance cloud orchestration scripts (`scripts/gcp/`) supporting multi-node distributed simulations on Google Cloud Compute Engine (`c2-standard-16` / `c2-standard-4` Spot cluster: `ether-master` + `ether-worker`) with a live asynchronous **PostgreSQL 15 + PostGIS 3.3** geospatial persistence layer.

> 📖 See [docs/BENCHMARKS.md](docs/BENCHMARKS.md) and [docs/DEPLOYMENT_AND_INFRASTRUCTURE.md](docs/DEPLOYMENT_AND_INFRASTRUCTURE.md) for the exhaustive cost-per-tick matrices, sizing formulas, and multi-node cluster scaling curves.

All snapshots, world state matrices (`topology.bin.gz`, `state.bin.gz`, `metadata.json`, `scenario.json`, `history.json`), and PostGIS spatial tables can be synchronized to the local machine in 1-click via `./scripts/gcp/fetch-results.sh` for seamless interactive replay in the Ether GUI.

---

## 🔬 Scientific Engine Architecture & Ontological Separation

Ether couples two rigorous tiers of scientific engines:
* **Tier 1 — Core Fundamental Planetary Physics (~30-40 Engines)**:
  - Celestial mechanics & Milankovitch insolation cycles (100k/41k/23k yr)
  - Stefan-Boltzmann radiative balance & Clausius-Clapeyron moisture
  - 3-cell atmospheric circulation (Hadley/Ferrel/Polar) with Coriolis wind fields
  - Stommel AMOC 2-box thermohaline ocean conveyor
  - 2D lateral Darcy porous aquifer depletion & Manning-Strickler hydrodynamics
  - Viscoelastic Glacial Isostatic Adjustment (GIA) & Positive Degree-Day (PDD) melt
  - Farquhar FvCB photosynthesis, van Genuchten soil water retention, N-P-K nutrient depletion
  - Stull wet-bulb temperature hyperthermia lethality & Gompertz-Makeham cohort demography
* **Tier 2 — Optional Cliodynamic & Institutional Engines (~50+ Engines)**:
  - Turchin-Goldstone Structural-Demographic Theory (SDT) & Elite Overproduction
  - Tainter diminishing returns on organizational complexity
  - Kümmel-Ayres-Warr biophysical exergy growth ($Y = A K^\alpha L^\beta E^\gamma$)
  - West-Bettencourt urban allometric scaling ($Y \propto N^{1.15}$)
  - NASA HANDY & Limits to Growth (World3) non-linear carrying capacity collapse
  - Price equation multi-level selection of altruism & Boserupian agricultural intensification
  - W. Brian Arthur combinatorial technology evolution & Hotelling resource depletion
  - 30+ Type B historical scenario engines (Edo isolation, Roman hyperinflation, Fertile Crescent salinization, Asabiyyah decay, Wittfogel hydraulic despotism).

See [docs/SIMULATION_EQUATIONS_AND_VARIABLES.md](docs/SIMULATION_EQUATIONS_AND_VARIABLES.md) for full LaTeX mathematical formulations.

---

## 🏛️ 4-Axis Scientific Epistemic Falsification & 27 Bifurcation Suite

Ether acts as an **epistemic falsification laboratory** benchmarking competing macroeconomic hypotheses against empirical historical datasets (HYDE 3.4, Maddison 2020, Seshat Databank):

```
╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                        ETHER 4-AXIS EMPIRICAL FALSIFICATION & BENCHMARK SUITE                                                ║
╠══════════════════════════════════════╦════════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║ 1. Great Men vs. Physical Attractor  ║ Evaluates charismatic leaders (Alexander -334, Genghis 1206, Napoleon 1800) measuring relaxation time  ║
║                                      ║ τ_relax ≤ 120 years back to underlying biophysical carrying capacity and trade potential.                 ║
╠══════════════════════════════════════╬════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║ 2. Archaeological Detective Anomaly  ║ Monitors spectral discrepancy derivative dΩ/dt ≥ 0.015 yr⁻¹ to detect missing historical events          ║
║                                      ║ (Harappa -1900 drought, Roman 3rd c. crisis, Maya 800 CE karst collapse).                                 ║
╠══════════════════════════════════════╬════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║ 3. Master 9-Epoch Continuous Mirror  ║ Pure unforced integration across 9 canonical epochs from -100,000 BP to 2026 CE (Option B).               ║
╠══════════════════════════════════════╬════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║ 4. Pluggable Engine Ablation Audit   ║ Benchmarks marginal ΔRMSE sensitivity and ΔTPS compute savings of 25+ Tier-2 engines (proving World3 is    ║
║                                      ║ vital post-1900 but disabling it pre-1900 saves +18.4% CPU without fidelity loss).                        ║
╚══════════════════════════════════════╩════════════════════════════════════════════════════════════════════════════════════════════════════════╝
```

---

## 🗺️ 27 Canonical Historical Bifurcation & Rupture Scenarios

Ether formalizes and evaluates 27 canonical historical bifurcation tipping points across three ontological forcing classes (Geophysical, Epidemiological, Socio-Political):

| # | Preset Key & Title | Epoch Window | Forcing Class | Key Biophysical & Cliodynamic Mechanism |
| :-: | :--- | :---: | :---: | :--- |
| **01** | `out_of_africa_100k` : Out-of-Africa Dispersal | -100k ➔ -50k | Class I (MIS-5) | Coastal foraging corridors & genetic drift |
| **02** | `toba_cataclysm_74k` : Toba VEI-8 Volcanic Winter | -74k ➔ -50k | Class I (Aerosols) | Optical depth $\tau \ge 8$, human bottleneck ($N_e \approx 10\text{k}$) |
| **03** | `sahul` : Maritime Settlement of Australia | -50k ➔ -10k | Class III (Maritime) | Wallace line crossing & fire-stick farming |
| **04** | `beringia` : Kelp Highway American Colonization | -25k ➔ -10k | Class I (LGM Sea) | Beringian standstill & Pacific coast migration |
| **05** | `lgm_solutrean` : Last Glacial Maximum Refugia | -20k ➔ -12k | Class I (Ice Sheet) | Thermal refugia & tailored clothing insulation |
| **06** | `younger_dryas` : Natufian Agrarian Sedentism | -10.9k ➔ -9.5k | Class I (AMOC) | Abrupt cooling & wild cereal storage silos |
| **07** | `fertile_crescent` : Neolithic Revolution | -8000 ➔ -5000 | Class III (Agrarian) | Grain surplus, taxation cages & zoonotic disease |
| **08** | `green_sahara` : African Humid Period | -6000 ➔ -3500 | Class I (Orbital) | Mega-Lake Chad & trans-Saharan pastoral migration |
| **09** | `ancient_egypt` : Hydraulic Basin Unification | -3000 ➔ -1000 | Class III (Hydraulic) | Centralized Nile flood basin coordination |
| **10** | `assyrian_empire` : Mesopotamian Soil Salinization | -1900 ➔ -600 | Class I/III (Salts) | Soil salinization & imperial military expansion |
| **11** | `bronze_age_collapse_1200bc` : Sea Peoples Rupture | -1200 ➔ -900 | Class I/III (Drought) | Cascading Eastern Mediterranean palatial collapse |
| **12** | `early_iron_age` : Iron Metallurgy Diffusion | -1000 ➔ -300 | Class III (Metal) | Democratization of cheap iron tools & weapons |
| **13** | `alexander_hellenistic_334bc` : Macedonian Conquest | -334 ➔ -150 | Class III (Conquest) | Achaemenid bullion monetization & Greek koinè |
| **14** | `maurya_empire` : Ashoka Moral State & Rice | -300 ➔ +100 | Class III (Ethics) | Wet-rice surplus & non-violent coordination |
| **15** | `roman_empire` : Pax Romana & Turchin SDT | 0 ➔ +476 | Class III (Institutions)| Maritime trade highways & elite overproduction |
| **16** | `late_antique_ice_age` : 536 CE Volcanic Anomaly | 536 ➔ +650 | Class I/II (Volc+Plague)| Double stratospheric veil & Justinian plague |
| **17** | `islamic_expansion_632` : Arab Agrarian Revolution | 632 ➔ +900 | Class III (Asabiyyah) | Qanat aquifer irrigation & trade unification |
| **18** | `song_dynasty` : Hydraulic Coal Smelting | 1000 ➔ +1279 | Class III (Exergy) | Hydraulic machinery, paper money & coke smelting |
| **19** | `mongol_conquest_1206` : Steppe Nomad Shock | 1206 ➔ +1368 | Class III (Nomad) | Composite bow cavalry & Qanat destructuring |
| **20** | `mali_empire` : Trans-Saharan Gold/Salt Surge | 1324 ➔ +1591 | Class III (Trade) | Trans-Saharan gold monetisation & Mansa Musa |
| **21** | `black_death_1347` : Yersinia Pestis Rupture | 1347 ➔ +1450 | Class II (Pathogen) | -50% labor supply shock & feudal wage inversion |
| **22** | `americas_1491` : Pre-Columbian Intensive Agro | 1491 ➔ +1650 | Class III (Ecosystem) | Chinampas, terra preta & high Andean density |
| **23** | `columbian_contact` : Virgin Soil Epidemics | 1492 ➔ +1650 | Class II (Smallpox) | -90% indigenous mortality & Orbis CO2 dip |
| **24** | `tokugawa_japan` : Sakoku Autarkic Equilibrium | 1639 ➔ +1853 | Class III (Autarky) | Zero-growth circular agro-forestry equilibrium |
| **25** | `industrial_1800` : Coal & Steam Thermodynamic Leap | 1800 ➔ +1900 | Class III (Fossil E) | Escape from organic economy & Jevons paradox |
| **26** | `world_wars_totalitarian_1914` : Total Industrial War | 1914 ➔ +1960 | Class III (Total War) | Haber-Bosch nitrogen, nuclear weapons & state |
| **27** | `anthropocene_2000` : The Great Acceleration | 2000 ➔ +2100 | Class III (Global Net) | Planetary boundary overshoot & EROEI net energy |

See [docs/HISTORICAL_BIFURCATIONS_AND_TESTING.md](docs/HISTORICAL_BIFURCATIONS_AND_TESTING.md) for full historical mechanisms and references.

---

## 📈 Historical Validation Kernel (`HistoricalValidationKernel`)

Ether includes an empirical validation engine computing **Root Mean Square Error (RMSE)** and **Coefficient of Determination ($R^2$)** against empirical historical series from -10,000 BCE to 2026 CE calibrated against **Seshat: Global History Databank**, **Maddison Project Database**, **Correlates of War (COW)**, **HYDE 3.4**, and **PMIP4/CMIP6**.

---

## 📄 Master Technical Documentation & Communications

- 🚀 [Deployment & Infrastructure Guide (Local, Docker & GCP Cloud)](docs/DEPLOYMENT_AND_INFRASTRUCTURE.md)
- ⚡ [Unified Planetary Benchmark Suite (Single-Node & Cluster Scaling)](docs/BENCHMARKS.md)
- 📐 [Differential Equations & State Variables Specification](docs/SIMULATION_EQUATIONS_AND_VARIABLES.md)
- 🏗️ [Core System Architecture & Data-Oriented Design](docs/ARCHITECTURE.md)
- 🔬 [Scientific Model Evaluation & Falsification Suite](docs/SCIENTIFIC_MODEL_EVALUATION_AND_FALSIFICATION.md)
- 🌋 [Historical Bifurcations, Tipping Points & Empirical Calibration](docs/HISTORICAL_BIFURCATIONS_AND_TESTING.md)
- 🗺️ [Planetary Cartography, Paleoclimate & GIS Data Pipeline](docs/CARTOGRAPHY_AND_PALEO_DATA.md)
- 📊 [Comparative Analysis vs. Global Systemic Models](docs/MODEL_COMPARISON.md)
- 🔒 [Security & System Integrity Audit](docs/SECURITY.md)
- 📢 [Reddit Launch Post](docs/posts/REDDIT_POST.md)
- 💼 [LinkedIn Announcement Post](docs/posts/LINKEDIN_POST.md)
- 📦 [Version 1.0.0-beta.2 Release Notes](docs/posts/RELEASE_ANNOUNCEMENT.md)

---

## 📄 License & Authors

Distributed under the **MIT License**.

**Authors**:
- **Silvere Martin-Michiellot** (Lead Author & Creator)
- **Antigravity / Gemini AI (Google DeepMind)**
