# 🗺️ Planetary Cartography, Paleoclimate & Empirical GIS Data Pipeline

**Document Version:** 2.0.0-unified  
**Classification:** Geospatial Data Pipeline, Paleoclimate Forcing & Scientific Provenance  
**Standard:** Cartographic Tensor Provenance Directives (`AGENTS.md`)  
**Package Target:** `org.ether.gis.*`, `org.ether.society.data.*`  

---

## 📋 Table of Contents
1. [Geospatial Ingestion Architecture & Uber H3 Mapping](#1-geospatial-ingestion-architecture--uber-h3-mapping)
2. [The 25 Cartographic Simulation Tensor Layers](#2-the-25-cartographic-simulation-tensor-layers)
3. [Empirical Datasets & Academic Source Attributions](#3-empirical-datasets--academic-source-attributions)
4. [Paleoclimate Forcing & Quaternary Reconstructions](#4-paleoclimate-forcing--quaternary-reconstructions)
5. [Critical Peer Review & Epistemic Decoupling ($t_0$ vs. $t > t_0$)](#5-critical-peer-review--epistemic-decoupling-t_0-vs-t--t_0)

---

## 1. Geospatial Ingestion Architecture & Uber H3 Mapping

Ether ingests global geophysical, paleoclimatic, mineralogical, and demographic datasets across Earth, the Moon, and Mars, normalizing them onto a continuous **Uber H3 hexagonal discrete global grid system (DGGS)**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PLANETARY GIS INGESTION PIPELINE                         │
│                                                                             │
│  Raw External Repositories                                                  │
│  (USGS, NOAA, GEBCO, NASA PDS, IAEA, BGR, Seshat, HYDE 3.4, PANGAEA, WHYMAP)│
│                                   │                                         │
│                                   ▼                                         │
│  Ingestion & Format Normalization                                           │
│  (GeoTIFF, ESRI ASCII Grid, NetCDF, GeoJSON, CSV, Excel, NASA PDS .lbl)     │
│                                   │                                         │
│                                   ▼                                         │
│  Coordinate & Spatial Reprojection (WGS84 EPSG:4326 -> Uber H3 Mesh)        │
│                                   │                                         │
│                                   ▼                                         │
│  Multidimensional Simulation Tensors & Raster Pre-Cache                     │
│  (`WorldBuffer` SoA, Packed Double Buffers, `data/cache/*.png`)              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. The 25 Cartographic Simulation Tensor Layers

Every historical epoch (from -100,000 BP to present-day) is encoded across 25 standardized spatial tensor layers:

| Layer Category | Layer Name | Physical Units | Primary Empirical Source |
| :--- | :--- | :--- | :--- |
| **Geophysical** | `elevation` | Meters ($m$) | NOAA ETOPO 2022, GEBCO 2023 |
| | `bathymetry` | Meters ($m$) | GEBCO Gridded Bathymetry Data |
| | `heat_flux` | $mW / m^2$ | International Heat Flow Commission (IHFC) |
| | `crustal_age` | Million years ($Myr$) | Müller et al. (2019) Oceanic Lithosphere |
| **Hydrological** | `aquifer_volume` | Cubic kilometers ($km^3$) | UNESCO WHYMAP, BGR Global Groundwater |
| | `water_table_depth`| Meters ($m$) | Fan et al. (Science 2013) Global Water Table |
| | `river_discharge` | $m^3 / s$ | Global Runoff Data Centre (GRDC) |
| **Pedological** | `soil_nitrogen` | $g / kg$ soil | SoilGrids 2.0 (ISRIC), FAO Harmonized Soil |
| | `soil_phosphorus`| $mg / kg$ soil | Global Soil Phosphorus Distribution (Yang et al.) |
| | `soil_potassium` | $mg / kg$ soil | ISRIC World Soil Information |
| | `soil_organic_carbon`| % dry mass | Harmonized World Soil Database (HWSD v2.0) |
| **Mineralogical** | `iron_copper_ore`| % mass fraction | USGS Mineral Resources Data System (MRDS) |
| | `coal_reserves` | Million tons ($Mt$) | World Energy Council, BGR Energy Study |
| | `hydrocarbon_reserves`| Barrels oil equiv. | USGS World Petroleum Assessment |
| | `uranium_thorium`| ppm mass concentration| IAEA World Distribution of Uranium (NFCIS) |
| **Bioclimatic** | `temperature` | °Celsius (°C) | ERA5-Land (ECMWF), PMIP4 Paleoclimate |
| | `rainfall` | $mm / year$ | GPCC Global Precipitation, WorldClim 2.1 |
| | `biomes` | Discrete Biome Enum | Olson et al. (WWF Terrestrial Ecoregions) |
| | `natural_vegetation`| Fractional Cover (0-1)| Copernicus Land Cover (100m) |
| **Anthropogenic** | `population_density`| Inhabitants / $km^2$ | HYDE 3.4 (Klein Goldewijk et al.), UN WPP |
| | `agricultural_land`| Fractional Cover (0-1)| FAOSTAT, HYDE 3.4 Cropland & Pasture |
| | `technology_level`| Discrete Tech Index | Maddison Historical Tech Diffusion Index |
| | `sovereignty_id` | Integer Polity ID | Euratlas Historical GIS, CShapes 2.0 |
| | `trade_connectivity`| Graph Centrality Index| Maritime Silk Road & Roman Itineraria Corridors |
| | `asabiyyah_cohesion`| Scalar Index (0-1) | Turchin Seshat Structural-Demographic Tensor |

---

## 3. Empirical Datasets & Academic Source Attributions

Ether strictly decouples speculative heuristics from empirical foundations by anchoring each tensor layer in published, peer-reviewed databases:

1. **HYDE 3.4 (History Database of the Global Environment)**:
   * *Citation*: Klein Goldewijk, K., et al. (2017). *Anthropogenic land use estimates for the Holocene – HYDE 3.2*. Earth System Science Data, 9(2), 927-953.
   * *Coverage*: Spatially explicit historical population, cropland, and grazing land from 10,000 BCE to 2026 CE.
2. **Maddison Project Database 2020**:
   * *Citation*: Bolt, J., & van Zanden, J. L. (2020). *Maddison Style Estimates of the Evolution of the World Economy*.
   * *Coverage*: Historical Gross Domestic Product (GDP), real wages, and population densities across millennia.
3. **Seshat: Global History Databank**:
   * *Citation*: Turchin, P., et al. (2018). *Quantitative historical analysis uncovers a single dimension of complexity that structures global variation in human social organization*. PNAS, 115(2), E144-E151.
   * *Coverage*: Imperial complexity, bureaucratic centralization, military technology, and internal crisis frequencies.
4. **PMIP4 / CMIP6 Paleoclimate Simulations**:
   * *Citation*: Kageyama, M., et al. (2018). *The PMIP4 contribution to CMIP6 – Part 4: Scientific objective and experimental design of the Last Glacial Maximum experiments*. Geoscientific Model Development, 10, 4035-4055.
   * *Coverage*: Atmospheric circulation, sea-surface temperature, and ice sheet extents for LGM (-21,000 BP) and Mid-Holocene (-6,000 BP).

---

## 4. Paleoclimate Forcing & Quaternary Reconstructions

During deep-time simulations (e.g. `out_of_africa_100k` or `younger_dryas_10900bce`), climate is driven by astronomical orbital parameters and ice sheet boundaries:

1. **Milankovitch Orbital Cycles**:
   * **Eccentricity** ($e$, $\sim 100\,000\text{ yr}$ and $\sim 405\,000\text{ yr}$): Modulates seasonal insolation extremes.
   * **Obliquity** ($\varepsilon$, $\sim 41\,000\text{ yr}$, $22.1^\circ$ to $24.5^\circ$): Controls the equator-to-pole thermal gradient.
   * **Climatic Precession** ($\varpi$, $\sim 23\,000\text{ yr}$ and $\sim 19\,000\text{ yr}$): Dictates hemisphere-specific monsoon intensities.
2. **Ice Sheet Geometry & Sea Level Transgression**:
   * During the **Last Glacial Maximum (LGM, -21,000 BP)**, sea level dropped by $\approx -125\text{ meters}$, exposing continental shelves:
     * **Beringia**: Terrestrial land bridge connecting Siberia and Alaska.
     * **Sundaland**: Submerged shelf connecting Indochina, Sumatra, Java, and Borneo.
     * **Doggerland**: Dry land bridge across the North Sea connecting Britain to continental Europe.
   * In Ether, when global sea levels fluctuate, H3 hexagonal cells dynamically toggle between terrestrial (`elevation >= 0`) and maritime (`elevation < 0`), shifting friction and carrying capacities accordingly.

---

## 5. Critical Peer Review & Epistemic Decoupling ($t_0$ vs. $t > t_0$)

To guarantee scientific falsifiability, Ether enforces a strict ontological separation between **Initial Boundary Conditions** and **Dynamical Physical Ticks**:

$$\text{Simulation State at } t = \underbrace{\mathbf{X}(t_0)}_{\text{Empirical Archaeological Tensor}} + \int_{t_0}^{t} \underbrace{\mathbf{F}\left(\mathbf{X}(\tau), \mathbf{u}(\tau)\right)}_{\text{Coupled Physical & Cliodynamic ODEs}} d\tau$$

1. **Initial Conditions ($t = t_0$)**: Formed exclusively from archaeological, geological, and historical data manifests stored in `data/maps/ether/<planet>/<year>/`. No speculative forward-extrapolations are mixed into $t_0$.
2. **Dynamical Trajectory ($t > t_0$)**: Governed entirely by invariant conservation laws (Tier 1) and pluggable cliodynamic hypotheses (Tier 2).
3. **Falsification Protocol**: Any divergence between the simulated state $\mathbf{X}_{\text{sim}}(t)$ and the target empirical benchmark $\mathbf{X}_{\text{obs}}(t)$ is objectively quantified via Normalized Root Mean Square Error (NRMSE) and Kolmogorov-Smirnov distance ($D_{\text{KS}}$), allowing flawed sociological theories to be definitively falsified.
