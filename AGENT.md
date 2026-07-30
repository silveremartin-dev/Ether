# 🤖 AGENT.md - Project Vision, Academic Standards & Technical Goals

**Project Name:** Ether (Human Civilization & Socio-Ecological Simulation Engine)  
**Lead Architect:** Silvere Martin-Michiellot  
**Target Scope:** Pre-Neolithic Hunter-Gatherer Eras (-20,000 BCE) $\to$ Neolithic Agricultural Revolution $\to$ Bronze/Iron Age Empire Formation $\to$ State Systems  
**Academic Requirement Level:** High Academic & Physical Rigor (Thermodynamics, Evolutionary Biology, Spatial Ecology, Macroeconomics)

---

## 🎯 1. Project Objectives & Core Philosophy

Ether is a multi-scale, data-driven simulation framework designed to model the emerging socio-ecological complexity of human societies. Unlike arbitrary strategy games or simplified agent models, Ether enforces **academic-grade physical, biophysical, and thermodynamic principles**:

1. **Thermodynamic & Energetic Foundations**:
   - Society as an open thermodynamic system dissipating energy to maintain internal structure (Order/Negentropy).
   - Energy flows measured in Joules / Megajoules / Calories (Solar Irradiance $\to$ Net Primary Productivity $\to$ Trophic Energy Harvest $\to$ Human Metabolic & Technological Work).
2. **Evolutionary & Ecological Biology**:
   - Malthusian & Boserupian demographic dynamics bounded by local carrying capacity $K(\mathbf{x}, t)$.
   - Trophic cascades: Solar radiation $\to$ Flora/Biomass $\to$ Herbivores/Livestock $\to$ Humans.
   - Liebig's Law of the Minimum: Population growth and carrying capacity are constrained by the scarcest critical resource (Freshwater, Arable Land, Energy, Metals).
3. **Multi-Scale Spatial Discretization (Uber H3)**:
   - World representation discretized on a global hexagonal grid (Uber H3, Resolutions 5 to 8).
   - Hexagonal topology ensures uniform spatial distance, eliminating projection distortions present in square raster grids.
4. **Identity Card Simulation ("Fiches d'Identité")**:
   - Each H3 cell and political entity maintains a dynamic identity card tracking geographic, ecological, demographic, cultural, and economic variables.

---

## 🏛️ 2. Architectural Blueprint (Reverse-Engineered from Codebase)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      JavaFX Glassmorphism UI Layer                       │
│ ┌─────────────────┐ ┌────────────────────────┐ ┌──────────────────────┐ │
│ │ Planet Generator│ │ Resources & Ecology    │ │ Scenario & World     │ │
│ └─────────────────┘ └────────────────────────┘ └──────────────────────┘ │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│                    H3SimulationEngine (Orchestrator)                    │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │             Fast Scale (DT = 1 Day): FluxEngine (Supply/Trade)       │ │
│ ├─────────────────────────────────────────────────────────────────────┤ │
│ │ Slow Scale (DT = 30 Days): DOD Kernels (WorldBuffer & AgentBuffer)  │ │
│ │  ├── EnvironmentalKernel (Biomass, NPP, Solar Irradiance)          │ │
│ │  ├── DemographicKernel   (Births, Starvation, Natural Death)        │ │
│ │  ├── UrbanKernel         (Infrastructure, Urban Sprawl, K)          │ │
│ │  ├── CultureKernel       (Linguistic/Tech Drift, Innovation)        │ │
│ │  └── StatisticsKernel    (Gini, GDP, Life Expectancy, Fertility)    │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Subsystems:
- **Artemis Layer**: Continuous field/density modeling across H3 cells (Population Density, Biomass Density, Resource Stocks).
- **Data-Oriented Design (DOD)**: Flat primitive array buffers (`WorldBuffer`, `AgentBuffer`) for CPU cache locality and SIMD execution.
- **Procedural Climate & Satellite Engine**: Spherical 3D Simplex noise with thermal lapse rates, ITCZ/subtropical precipitation belts, and USGS/NASA WMS satellite data integration.

---

## 🌿 3. Ecological & Resource Subsystem ("Ressources & Écologie")

The **Resources & Ecology** panel configures the planet-wide initial conditions across 8 fundamental biophysical parameters:

| Parameter | UI Control | Internal Model Variable | Physical / Academic Unit |
| :--- | :--- | :--- | :--- |
| **Forest Density & Wood** | `woodDensitySlider` | `woodResource` / `BiomassNatural` | Metric Tons / $km^2$ (Lignin Energy Reservoir) |
| **Agricultural Crop Yield** | `cropYieldSlider` | `cropMass` / `foodResource` | $kcal/ha/year$ (Photosynthetic Primary Production) |
| **Wild Game & Animal Density** | `gameFaunaSlider` | `wildFoodMass` | $kg/km^2$ (Herbivore Secondary Biomass) |
| **Livestock Capacity** | `livestockCapSlider` | `livestockMass` | Head / $km^2$ (Domesticated Carrying Capacity) |
| **Metal Ore Deposits** | `metalOresSlider` | `resourceMetal` | Metric Tons (Iron/Copper Ore Stock) |
| **Precious Ores** | `preciousOresSlider` | `resourcePrecious` | Metric Tons (Gold/Silver Ore Stock) |
| **Stone & Quarry Quality** | `stoneQualitySlider` | `stoneQuality` | Structural Mass Index (Megastructure Construction) |
| **Marine Life & Fish Abundance**| `fishAbundanceSlider`| `biomassFish` | Metric Tons / $km^2$ (Aquatic Trophic Yield) |

---

## 🚀 4. Academic Roadmap & Future Guidelines for Agents

Any future code or architectural contributions must adhere to the following strict principles:
1. **Never hardcode arbitrary game logic**: Always ground formulas in physical differential equations (e.g., Logistic growth $\frac{dP}{dt} = r P (1 - P/K)$, Advection-Diffusion equations for migration, Entropy functions for metallurgy).
2. **Preserve High-Performance DOD Buffers**: Maintain cache alignment in `WorldBuffer` and `AgentBuffer` when adding new state variables.
3. **i18n Parity**: All new UI components must maintain complete internationalization across supported languages (`messages_fr.properties`, `messages_en.properties`, etc.).
4. **Seamless Identity Card Updates**: Ensure cell-level and nation-level statistics are exposed to `StatisticsKernel` and `StatsPanel`.
