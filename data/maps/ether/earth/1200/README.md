# Earth Epoch 1200: Medieval 1200 AD

## 🌍 Overview
This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **1200** (Medieval 1200 AD).

All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

### Context & Archaeological/Historical Horizon
* **Era**: Medieval 1200 AD
* **Technocomplex**: Movable Type Printing, Compass & Gunpowder Artillery
* **Social / Family Horizon**: Manorial Feudal Serfdom & Stem Households
* **Estimated Caloric Baseline**: 2900 kcal/capita/day

## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

### Physical & Climate Layers
* `earth_1200_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
* `earth_1200_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
* `earth_1200_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
* `earth_1200_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
* `earth_1200_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

### Cliodynamic & Cultural Tensors
* `earth_1200_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
* `earth_1200_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
* `earth_1200_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
* `earth_1200_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
* `earth_1200_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
* `earth_1200_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
* `earth_1200_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
* `earth_1200_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
* `earth_1200_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
* `earth_1200_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

### Geological & Energy Resources
* `earth_1200_coal.png`: Coal Basins (USGS WoCQI).
* `earth_1200_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
* `earth_1200_gas.png`: Natural Gas Formations (USGS).
* `earth_1200_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
* `earth_1200_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
* `earth_1200_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
* `earth_1200_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
* `earth_1200_rare_earths.png`: Critical Rare Earth Elements (REE).
* `earth_1200_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
* `earth_1200_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
In strict accordance with `AGENTS.md` Directives:
1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

## 📄 Associated Metadata Files
* `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
* `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
