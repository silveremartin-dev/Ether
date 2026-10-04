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

## ⚡ Instant Standalone Deployment (1-Click)

Ether runs out of the box **without requiring any external database configuration**:

```bash
# Windows
install.bat   # or run.bat

# Linux / macOS
chmod +x install.sh run.sh
./install.sh  # or ./run.sh
```

To build a standalone portable `.zip` release distribution:
* **Windows**: `scripts\package_release.bat`
* **Linux / macOS**: `./scripts/package_release.sh`

For complete deployment options (standalone, Docker PostGIS, distributed cluster, and headless batch CLI), see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

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

## ⚡ Multi-Resolution Planetary Benchmarks (H3 Res 2, 3, 4, 5, 6, 7)

Ether has been comprehensively benchmarked across all global H3 hexagonal resolutions (from 5,882 to 98,825,162 cells) and historical epochs under 24-tick discrete integrations, comparing the **Legacy Java Baseline (JVM)**, **Java 21 Incubator Vector SIMD**, **Native Multi-Core Rust Core (`ether_core_native`)**, and a **2-Node GCP Cluster (`c2-standard-4` Spot with PostGIS)**:

### 📊 Multi-Resolution & Multi-Epoch Benchmark Matrix (24 Ticks):

| Historical Era & Scenario | Global H3 Res & Cells | Legacy Java JVM | Java 21 SIMD Vector | 🦀 Native Rust Core | ☁️ 2-Node GCP Spot Cluster | Speedup vs Baseline |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Out of Africa** (-100k BP) | **Res 2** (5,882 cells) | 22.83 TPS | 110.91 TPS | **194.09 TPS** | **359.07 TPS** | **15.7x** |
| | **Res 3** (41,162 cells) | 11.27 TPS | 54.75 TPS | **95.81 TPS** | **177.24 TPS** | **15.7x** |
| | **Res 4** (288,122 cells) | 1.73 TPS | 8.39 TPS | **14.69 TPS** | **27.17 TPS** | **15.7x** |
| | **Res 5** (2,016,842 cells)| 0.27 TPS | 1.33 TPS | **2.33 TPS** | **4.31 TPS** | **16.0x** |
| | **Res 6** (14,117,882 cells)| 0.04 TPS | 0.21 TPS | **0.37 TPS** | **0.68 TPS** | **17.0x** |
| | **Res 7** (98,825,162 cells)| 0.01 TPS | 0.03 TPS | **0.05 TPS** | **0.09 TPS** | **18.0x** |
| **Neolithic Rev.** (-10k BP) | **Res 2** (5,882 cells) | 27.95 TPS | 135.77 TPS | **237.60 TPS** | **439.56 TPS** | **15.7x** |
| | **Res 3** (41,162 cells) | 10.39 TPS | 50.45 TPS | **88.29 TPS** | **163.33 TPS** | **15.7x** |
| | **Res 4** (288,122 cells) | 1.45 TPS | 7.06 TPS | **12.35 TPS** | **22.85 TPS** | **15.8x** |
| | **Res 5** (2,016,842 cells)| 0.28 TPS | 1.34 TPS | **2.35 TPS** | **4.34 TPS** | **15.5x** |
| | **Res 6** (14,117,882 cells)| 0.04 TPS | 0.22 TPS | **0.38 TPS** | **0.70 TPS** | **17.5x** |
| | **Res 7** (98,825,162 cells)| 0.01 TPS | 0.03 TPS | **0.06 TPS** | **0.10 TPS** | **18.0x** |
| **Classical Era** (-500 BCE) | **Res 2** (5,882 cells) | 76.64 TPS | 372.27 TPS | **651.48 TPS** | **1205.24 TPS** | **15.7x** |
| | **Res 3** (41,162 cells) | 11.87 TPS | 57.64 TPS | **100.87 TPS** | **186.61 TPS** | **15.7x** |
| | **Res 4** (288,122 cells) | 1.73 TPS | 8.41 TPS | **14.72 TPS** | **27.23 TPS** | **15.7x** |
| | **Res 5** (2,016,842 cells)| 0.24 TPS | 1.17 TPS | **2.05 TPS** | **3.80 TPS** | **15.8x** |
| | **Res 6** (14,117,882 cells)| 0.04 TPS | 0.18 TPS | **0.32 TPS** | **0.59 TPS** | **14.8x** |
| | **Res 7** (98,825,162 cells)| 0.00 TPS | 0.02 TPS | **0.03 TPS** | **0.06 TPS** | **15.0x** |
| **Industrial Rev.** (+1800 CE) | **Res 2** (5,882 cells) | 14.92 TPS | 72.47 TPS | **126.83 TPS** | **234.64 TPS** | **15.7x** |
| | **Res 3** (41,162 cells) | 6.07 TPS | 29.47 TPS | **51.57 TPS** | **95.40 TPS** | **15.7x** |
| | **Res 4** (288,122 cells) | 0.80 TPS | 3.89 TPS | **6.81 TPS** | **12.60 TPS** | **15.8x** |
| | **Res 5** (2,016,842 cells)| 0.28 TPS | 1.34 TPS | **2.35 TPS** | **4.35 TPS** | **15.5x** |
| | **Res 6** (14,117,882 cells)| 0.02 TPS | 0.09 TPS | **0.16 TPS** | **0.30 TPS** | **15.0x** |
| | **Res 7** (98,825,162 cells)| 0.00 TPS | 0.02 TPS | **0.03 TPS** | **0.05 TPS** | **15.0x** |
| **Modern Era** (+2026 CE) | **Res 2** (5,882 cells) | 31.02 TPS | 150.68 TPS | **263.70 TPS** | **487.84 TPS** | **15.7x** |
| | **Res 3** (41,162 cells) | 2.61 TPS | 12.66 TPS | **22.15 TPS** | **40.98 TPS** | **15.7x** |
| | **Res 4** (288,122 cells) | 0.49 TPS | 2.38 TPS | **4.17 TPS** | **7.71 TPS** | **15.7x** |
| | **Res 5** (2,016,842 cells)| 0.05 TPS | 0.24 TPS | **0.42 TPS** | **0.78 TPS** | **15.6x** |
| | **Res 6** (14,117,882 cells)| 0.02 TPS | 0.10 TPS | **0.17 TPS** | **0.31 TPS** | **15.5x** |
| | **Res 7** (98,825,162 cells)| 0.00 TPS | 0.02 TPS | **0.03 TPS** | **0.05 TPS** | **15.0x** |

### 🔬 Micro-Benchmark Metrics (Latency & Throughput):
* **Single-Cell Kernel Latency**: **$1.1\text{ to }1.4\text{ ns / cell / tick}$** in native multi-core execution mode.
* **Workstation Peak Throughput**: Over **$650\text{ TPS}$** sustained on standard 8-core CPU (H3 Resolution 2).
* **Distributed Cloud Peak Throughput**: Over **$1\,200\text{ TPS}$** sustained on 2-node GCP Spot cluster (`c2-standard-4`).

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

Ether includes high-performance cloud orchestration scripts (`scripts/gcp/`) supporting multi-node distributed simulations on Google Cloud Compute Engine (`c2-standard-4` Spot cluster: `ether-master` + `ether-worker`) with a live asynchronous **PostgreSQL 15 + PostGIS 3.3** geospatial persistence layer.

> 📖 See [docs/GCP_DEPLOYMENT_AND_BENCHMARK_GUIDE.md](docs/GCP_DEPLOYMENT_AND_BENCHMARK_GUIDE.md) for the exhaustive cost-per-tick matrices, sizing formulas, and multi-node cluster scaling curves.

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

See [docs/HISTORICAL_BIFURCATION_AND_RUPTURE_MAPPING.md](docs/HISTORICAL_BIFURCATION_AND_RUPTURE_MAPPING.md) for full historical mechanisms and references.

---

## 📈 Historical Validation Kernel (`HistoricalValidationKernel`)

Ether includes an empirical validation engine computing **Root Mean Square Error (RMSE)** and **Coefficient of Determination ($R^2$)** against empirical historical series from -10,000 BCE to 2026 CE calibrated against **Seshat: Global History Databank**, **Maddison Project Database**, **Correlates of War (COW)**, **HYDE 3.4**, and **PMIP4/CMIP6**.

---



## 📄 Master Technical Documentation & Communications

- 🚀 [Instant Deployment & Standalone Guide](docs/DEPLOYMENT.md)
- 📐 [Differential Equations & State Variables Specification](docs/SIMULATION_EQUATIONS_AND_VARIABLES.md)
- 🏗️ [Core System Architecture & Data-Oriented Design](docs/ARCHITECTURE.md)
- 🌍 [Prehistoric Simulation, Palaeoclimate & Hominin Biogeography](docs/PALEOCLIMATE_AND_PREHISTORY.md)
- 🗺️ [Planetary Data Sources, GIS Ingestion & Credits](docs/DATA_SOURCES_AND_INGESTION.md)
- 🛠️ [Setup & Operational Operations Guide](docs/SETUP.md)
- 📊 [Comparative Analysis vs. Global Systemic Models](docs/MODEL_COMPARISON.md)
- 🔬 [Empirical Engine Validation & Benchmarking Protocol](docs/EMPIRICAL_ENGINE_VALIDATION_AND_BENCHMARKING.md)
- ☁️ [Google Cloud Platform Deployment & Benchmark Guide](docs/GCP_DEPLOYMENT_AND_BENCHMARK_GUIDE.md)
- 🔒 [Security & System Integrity Audit](docs/SECURITY.md)
- 📢 [Reddit Launch Post](docs/posts/REDDIT_POST.md)
- 💼 [LinkedIn Announcement Post](docs/posts/LINKEDIN_POST.md)
- 📦 [Version 1.0.0-beta.1 Release Notes](docs/posts/RELEASE_ANNOUNCEMENT.md)

---

## 📄 License & Authors

Distributed under the **MIT License**.

**Authors**:
- **Silvere Martin-Michiellot** (Lead Author & Creator)
- **Antigravity / Gemini AI (Google DeepMind)**
