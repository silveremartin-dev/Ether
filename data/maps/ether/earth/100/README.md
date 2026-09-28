# Earth Epoch 100: Classical 100 AD

## 🌍 Overview
This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **100** (Classical 100 AD).

All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

### Context & Archaeological/Historical Horizon
* **Era**: Classical 100 AD
* **Technocomplex**: Roman Concrete, Aqueducts & Silk Road Caravans
* **Social / Family Horizon**: Paterfamilias Households & Confucian Patrilineal Clans
* **Estimated Caloric Baseline**: 2750 kcal/capita/day

## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

### Physical & Climate Layers
* `earth_100_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
* `earth_100_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
* `earth_100_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
* `earth_100_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
* `earth_100_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

### Cliodynamic & Cultural Tensors
* `earth_100_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
* `earth_100_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
* `earth_100_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
* `earth_100_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
* `earth_100_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
* `earth_100_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
* `earth_100_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
* `earth_100_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
* `earth_100_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
* `earth_100_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

### Geological & Energy Resources
* `earth_100_coal.png`: Coal Basins (USGS WoCQI).
* `earth_100_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
* `earth_100_gas.png`: Natural Gas Formations (USGS).
* `earth_100_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
* `earth_100_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
* `earth_100_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
* `earth_100_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
* `earth_100_rare_earths.png`: Critical Rare Earth Elements (REE).
* `earth_100_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
* `earth_100_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
In strict accordance with `AGENTS.md` Directives:
1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

## 📄 Associated Metadata Files
* `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
* `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
