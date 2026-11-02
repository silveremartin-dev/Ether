# Earth Epoch -11000: Late Glacial 11 000 BP

## 🌍 Overview
This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **-11000** (Late Glacial 11 000 BP).

All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

### Context & Archaeological/Historical Horizon
* **Era**: Late Glacial 11 000 BP
* **Technocomplex**: Natufian / Azilian Microlithic Sickles & Ground Stone
* **Social / Family Horizon**: Egalitarian Forager Multi-Family Band
* **Estimated Caloric Baseline**: 2250 kcal/capita/day

## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

### Physical & Climate Layers
* `earth_-11000_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
* `earth_-11000_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
* `earth_-11000_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
* `earth_-11000_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
* `earth_-11000_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

### Cliodynamic & Cultural Tensors
* `earth_-11000_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
* `earth_-11000_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
* `earth_-11000_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
* `earth_-11000_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
* `earth_-11000_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
* `earth_-11000_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
* `earth_-11000_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
* `earth_-11000_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
* `earth_-11000_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
* `earth_-11000_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

### Geological & Energy Resources
* `earth_-11000_coal.png`: Coal Basins (USGS WoCQI).
* `earth_-11000_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
* `earth_-11000_gas.png`: Natural Gas Formations (USGS).
* `earth_-11000_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
* `earth_-11000_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
* `earth_-11000_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
* `earth_-11000_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
* `earth_-11000_rare_earths.png`: Critical Rare Earth Elements (REE).
* `earth_-11000_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
* `earth_-11000_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
In strict accordance with `AGENTS.md` Directives:
1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

## 📄 Associated Metadata Files
* `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
* `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
