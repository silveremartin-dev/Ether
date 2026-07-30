# Technical Specifications - Human Society Simulation (Ether)

## Version
**2.0.0** - Academic & Multi-Scale Edition (Glassmorphism + DOD Artemis Engine)

## Overview
Ether is an academically-grounded, multi-scale simulation modeling the socio-ecological evolution of human civilizations from pre-Neolithic hunter-gatherer bands to ancient empires and nation-states. It combines a discrete hexagonal geospatial grid (Uber H3), density-based population dynamics, Data-Oriented Design (DOD) performance kernels, and a modern JavaFX interface.

## System Objectives & Academic Scope
1. **Thermodynamic & Ecological Realism**: Biophysical modeling of primary productivity (NPP), carrying capacity ($K$), trophic energy conversion, and resource entropy.
2. **Multi-Scale Spatial Discretization**: Uber H3 grid system (Resolutions 5 to 8) enabling seamless local-to-global simulation.
3. **Data-Oriented High-Performance Engine**: Hybrid engine featuring array-backed DOD memory buffers (`WorldBuffer`, `AgentBuffer`) for SIMD/parallel updates (Demographic, Urban, Environmental, Cultural, and Statistical kernels).
4. **Data-Driven & Satellite Integration**: Geospatial planet generation combining 3D Simplex noise procedural generation with real NASA MOLA, USGS Earth, Magellan Venus, and LRO Lunar elevation/biome datasets.
5. **Civilization Identity & Analytics**: Comprehensive tracking of civilizational identity cards, Gini wealth distribution, demographic transitions, and macroeconomic indicators.

## Technology Stack

### Core
- **Language**: Java 21 (LTS) - Virtual Threads, Records, Pattern Matching, Sealed Interfaces.
- **Build Tool**: Maven 3.9+
- **Geospatial Engine**: Uber H3 (4.1.1)

### Libraries & Frameworks
- **UI Engine**: JavaFX 21 with Glassmorphism CSS design system & multi-language i18n framework (FR, EN, ES, DE, ZH).
- **Data Serialization**: Jackson 2.15+ (JSON configuration presets and scenario saves).
- **Logging & Diagnostics**: SLF4J + Logback.
- **Persistence**: H3Cell Repository & SQL/JSON Save Engine.
- **Parallel Computing**: Java Virtual Threads, Parallel Streams, and OpenCL/TornadoVM integration layer.

## Architecture

### System Layers
1. **UI Layer (JavaFX)**: `MainView`, `PlanetGeneratorPanel`, `ResourceDistributionPanel`, `ScenarioSetupPanel`, `H3MapCanvas`, `StatsPanel`.
2. **Application Layer**: `EtherApp`, `Configuration`, `I18n`, `EventSystem`.
3. **Simulation Layer (Orchestrator)**: `H3SimulationEngine`, `FluxEngine`, `H3ClimateSystem`, `PoliticalSimulationEngine`.
4. **DOD Execution Kernels**:
   - `EnvironmentalKernel`: Resource regeneration, biomass updates, seasonal decay.
   - `DemographicKernel`: Birth/death dynamics, starvation, population growth.
   - `UrbanKernel`: Infrastructure development, cell carrying capacity.
   - `CultureKernel`: Cultural trait diffusion, linguistic/technological drift.
   - `StatisticsKernel`: Real-time Gini calculation, GDP estimation, life expectancy, fertility tracking.
5. **Persistence Layer**: `GameSaveManager`, `Scenario`, `JSON/SQL`.

## Climate & Biome Modeling

### Procedural Planet Generation (Simplex 3D Sphere)
- **Elevation ($e$)**: Multi-octave 3D Simplex noise mapped onto spherical coordinates ($x, y, z = \cos \phi \cos \lambda, \cos \phi \sin \lambda, \sin \phi$).
- **Temperature ($T$)**:
  $$T(\phi, e) = T_{\text{avg}} + \Delta T_{\text{grad}} \cdot \left(\cos \phi - 0.5\right) - \gamma \cdot e$$
  where $\phi$ is latitude, $\Delta T_{\text{grad}}$ is equator-to-pole gradient, and $\gamma \approx 6.5^\circ\text{C/km}$ is the atmospheric thermal lapse rate.
- **Rainfall ($R$)**: Combines 3D noise with latitude circulation belts ($\text{latMod}$) modeling the Intertropical Convergence Zone (ITCZ at $0-10^\circ$), Subtropical High Deserts ($20-40^\circ$), Mid-Latitude Ferrel Cell Rain ($50-70^\circ$), and Polar Aridity ($>80^\circ$).
- **Biome Classification Matrix**:
  - $e < \text{waterLevel} \implies \text{OCEAN} / \text{DEEP\_OCEAN}$
  - $e > 0.8 \implies \text{MOUNTAINS}$; $e > 0.5 \implies \text{HILLS}$
  - $T < -5^\circ\text{C} \implies \text{SNOW}$; $T < 5^\circ\text{C} \implies \text{TUNDRA}$
  - $R < 0.2 \implies \text{DESERT}$; $R < 0.5 \implies \text{PLAINS}$; $R < 0.8 \implies \text{FOREST}$; $R \ge 0.8 \implies \text{JUNGLE}$

### Satellite & Online Map Integration
- Dynamic fetching from USGS & NASA WMS remote services.
- Native import of heightmaps and biomes with spatial alignment and ESRI World File (`.tfw`) geospatial exports.

## Simulation Loop & Multi-Scale Dynamics

### Fast Scale ($\Delta t = 1 \text{ day}$)
- Resource flux and logistical transport (`FluxEngine`).
- Immediate political/diplomatic tension updates (`PoliticalSimulationEngine`).

### Slow Scale ($\Delta t = 30 \text{ days}$)
- Seasonal climate & solar irradiance adjustments (`H3ClimateSystem`).
- Ecological regeneration and biomass progression (`EnvironmentalKernel`).
- Demographic births/deaths and migration (`DemographicKernel`).
- Urban development (`UrbanKernel`) and cultural diffusion (`CultureKernel`).
- Macroeconomic snapshot capturing Gini, GDP, and demographic metrics (`StatisticsKernel`).

## Performance Targets
| Metric | Target | Status |
| :--- | :--- | :--- |
| Tick Rate (175k cells) | >10 TPS | ✅ Achieved |
| FPS (2D Canvas) | >30 FPS | ✅ Achieved |
| FPS (3D Isometric) | >20 FPS | ✅ Achieved |
| Startup Time | <3 s | ✅ Achieved |

## License
**MIT License**  
Copyright (c) 2024-2026 Silvere Martin-Michiellot & Gemini AI Assistant

