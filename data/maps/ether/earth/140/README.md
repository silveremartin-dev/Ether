# Cartographic & Cliodynamic Tensor Documentation — Epoch 140

## 🌍 Overview
* **Planet**: Earth
* **Epoch Year**: 140
* **Resolution**: 2048 x 1024 (Equirectangular / Plate Carrée)
* **Cartographic Standard**: 25 High-Definition Physical, Ecological, Cliodynamic, and Geological Resource Rasters:
  * **19 Continuous Scalar Layers**: **16-bit Grayscale PNG (`TYPE_USHORT_GRAY`, 65,536 levels `[0..65535]`)** (`elevation`, `temperature`, `precipitation`, `seasonality`, `density`, `technology`, `institutional`, `ecological`, `pathogen`, and all 10 geological/hydrological resources).
  * **6 Categorical / Polychrome Layers**: **24-bit RGB PNG (`TYPE_INT_RGB`)** (`biomes`, `sovereignty`, `isogloss`, `kinship`, `rituals`, `tradenetwork`).

---

## 🔬 Two-Tier Ontological Separation & Reconstitution
In accordance with **Rule 6 of AGENTS.md**:
1. **Static Initial Conditions ($t = t_0$)**: Initial state variables are ingested directly from peer-reviewed empirical datasets (NOAA ETOPO 2022, GEBCO 2023, WorldClim v2.1, UNESCO WHYMAP 2022, HYDE 3.4, Seshat ClioPatria, USGS MRDS, IAEA NFCIS, Davies 2013).
2. **Dynamic Simulation Ticks ($t > t_0$)**: Simulation engines (Farquhar photosynthesis, Darcy groundwater recharge, Stull wet-bulb mortality, Turchin SDT demographic oscillations, technological diffusion) run dynamically from these base rasters.

---

## 🗺️ 25 Cartographic Layers & Academic Provenance

### A. Geophysical & Ecological Tensors (5 Rasters)
1. `earth_140_elevation.png`: Global Relief & Bathymetry (NOAA ETOPO 2022 / GEBCO 2023).
2. `earth_140_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Classification.
3. `earth_140_temperature.png`: Annual Mean Surface Temperature with Orographic Lapse Rate (WorldClim v2.1 Bio1).
4. `earth_140_precipitation.png`: Mean Annual Precipitation & Orographic Monsoons (WorldClim v2.1 Bio12).
5. `earth_140_seasonality.png`: Temperature Seasonality Amplitude (WorldClim v2.1 Bio4).

### B. Cliodynamic & Anthropological Tensors (10 Rasters)
6. `earth_140_density.png`: Empirical Demographic Density (HYDE 3.4 / Seshat).
7. `earth_140_sovereignty.png`: Sovereign Polities & Autonomous Domains (Seshat ClioPatria / Natural Earth).
8. `earth_140_isogloss.png`: Ethnolinguistic Phyla & Dialectal Trees (Glottolog 4.8 / WALS).
9. `earth_140_kinship.png`: Spatial Kinship & Family Structures (Murdock D-PLACE / Todd).
10. `earth_140_rituals.png`: Sacred Geography & Religious Traditions (World Sacred Heritage).
11. `earth_140_technology.png`: Technological Complexity Index (Maddison / Archaeo-metallurgy).
12. `earth_140_institutional.png`: Institutional Complexity & State Capacity (Carneiro / Seshat).
13. `earth_140_ecological.png`: Anthropogenic Ecological Footprint & Agro-pastoral Modification.
14. `earth_140_pathogen.png`: Epidemiological Stress Field & Vector Habitats ($R_0$).
15. `earth_140_tradenetwork.png`: Commercial Arteries, Silk Road & Maritime Emporia.

### C. Geological & Energy Resource Tensors (10 Rasters)
16. `earth_140_coal.png`: In-Situ Coal Basins & Surface Outcrops (USGS WCOAL).
17. `earth_140_oil.png`: Hydrocarbon Traps & Bitumen Seeps (USGS World Petroleum Assessment).
18. `earth_140_gas.png`: Natural Gas Sedimentary Reserves (USGS Natural Gas).
19. `earth_140_uranium.png`: Crustal Uranium Provinces (IAEA NFCIS / UDEPO).
20. `earth_140_helium3.png`: Terrestrial Helium-3 (Zero Abundance Baseline).
21. `earth_140_iron_copper.png`: Banded Iron Formations & Copper Belts (USGS MRDS).
22. `earth_140_precious_metals.png`: Historical Gold/Silver Mines & Giant Provinces (USGS MRDS).
23. `earth_140_rare_earths.png`: Critical Minerals, REE & Lithium Salars (USGS REE).
24. `earth_140_geothermal.png`: Terrestrial Mantle Heat Flow (Davies 2013 IHFC).
25. `earth_140_aquifers.png`: Deep Sedimentary Basin Groundwater Aquifers (UNESCO WHYMAP 2022).
