# Earth Epoch 400: Classical 400 AD

## 🌍 Overview
This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **400** (Classical 400 AD).

All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

### Context & Archaeological/Historical Horizon
* **Era**: Classical 400 AD
* **Technocomplex**: Roman Concrete, Aqueducts & Silk Road Caravans
* **Social / Family Horizon**: Paterfamilias Households & Confucian Patrilineal Clans
* **Estimated Caloric Baseline**: 2750 kcal/capita/day

## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

### Physical & Climate Layers
* `earth_400_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
* `earth_400_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
* `earth_400_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
* `earth_400_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
* `earth_400_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

### Cliodynamic & Cultural Tensors
* `earth_400_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
* `earth_400_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
* `earth_400_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
* `earth_400_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
* `earth_400_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
* `earth_400_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
* `earth_400_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
* `earth_400_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
* `earth_400_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
* `earth_400_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

### Geological & Energy Resources
* `earth_400_coal.png`: Coal Basins (USGS WoCQI).
* `earth_400_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
* `earth_400_gas.png`: Natural Gas Formations (USGS).
* `earth_400_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
* `earth_400_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
* `earth_400_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
* `earth_400_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
* `earth_400_rare_earths.png`: Critical Rare Earth Elements (REE).
* `earth_400_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
* `earth_400_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
In strict accordance with `AGENTS.md` Directives:
1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

## 📄 Associated Metadata Files
* `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
* `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
