# Planetary Data Sources, GIS Ingestion Pipeline & Scientific Provenance

> **Master Geospatial Specification & Attribution Catalog**  
> *Ether Simulation Engine — Planetary Ingestion Pipeline, Spatial Layer Normalization, Empirical Datasets, Academic Citations, and Software Credits*

---

## 1. Executive Summary & Ingestion Architecture

The **Ether Simulation Engine** ingests, unifies, and maps global cartographic, bathymetric, paleoclimatic, mineralogical, demographic, and extraterrestrial planetary datasets (Earth, Moon, Mars, Venus, Mercury) onto a standardized **Uber H3 hexagonal grid** (Resolutions 6 to 8).

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

> [!NOTE]
> For the authoritative catalog of active cartographic directories, geological tensor layer indices, and file matrix details, see [data/maps/README.md](../data/maps/README.md).

---

## 2. Ingestion Pipeline & Spatial Normalization

### Data Processing Workflow
1. **Multi-Source Ingestion**: Heterogeneous geospatial data formats (GeoTIFF rasters, NetCDF grids, ESRI Shapefiles, tabular `.tab`/`.csv` archives) are parsed by dedicated ingestion drivers in `org.ether.gis` and `org.ether.society.data`.
2. **Geodetic & Coordinate Normalization**: All spatial rasters and vector geometries are re-projected to WGS84 (`EPSG:4326`) and interpolated across the planetary Uber H3 discrete global grid system.
3. **Tensor Layer Packing**: Spatially indexed attributes (elevation, bathymetry, heat flow, aquifer volume, mineral occurrences) are packed into contiguous `WorldBuffer` Structure-of-Arrays (SoA) memory structures for GPU/SIMD execution.
4. **Deterministic Pre-Caching**: For high-frequency scenario initialization, static planetary layers are pre-rendered into deterministic PNG/binary tensor caches under `data/cache/` and `data/maps/cache/`.

---

## 3. Metadata Standards & Governance Specification

Every provider directory under `data/maps/<provider_id>/` is governed by strict metadata contracts:

1. **`provider.json`**:
   - Specifies provider identity, issuing institution, license type, spatial resolution, temporal coverage, and contact information.
2. **`provider_manifest.json`**:
   - Contains dataset file checksums (SHA-256), coordinate reference systems, bounding boxes, physical units, and peer-reviewed academic citations.
3. **Automated Provenance & Verification**:
   - `repatriation_audit.json` tracks checksum validation, file availability, and coverage integrity across all integrated GIS layers.

---

## 4. Deduplication & Storage Optimization Strategy

To ensure repository scalability, zero Git bloat, and clean architectural boundaries, the dataset management workflow adheres to the following principles:

1. **Consolidation of Redundant Climate Grids**:
   - Historical and paleoclimate data from PMIP and CHELSA are consolidated directly under `paleoclim/` (CHELSA-Trace21k / PaleoCLIM) and `worldclim/` (WorldClim v2.1 normals).
2. **Unified Altimetry & Bathymetry**:
   - High-resolution terrestrial elevation and ocean depth are standardized on NOAA NCEI ETOPO 2022 v1 (in `usgs/`) and GEBCO 2024 (in `gebco/`), superseding redundant regional DEMs.
3. **Git Hygiene & Large Binary Handling**:
   - All multi-megabyte and gigabyte binary assets (`.tif`, `.tab`, `.xlsx`, `.zip`, `.nc`) are strictly isolated via `.gitignore`.
   - Lightweight metadata manifests and download scripts enable automated, repeatable ingestion on developer workstations and CI/CD runners.

---

## 5. UI Integration & Reference Raster Matrix (Tabs 1, 2 & 3)

All reference maps are stored as uncompressed 2:1 equirectangular rasters under `data/maps/` (read-only) and blitted directly onto JavaFX graphics contexts (`gc.drawImage`), achieving sub-5ms rendering latency and eliminating real-time noise generation overhead.

### Tab 1 — Planet Generator & Climate Reference Maps
| Reference Map | Disk Location | Source Dataset / Provider | Native Format / Resolution | Citation / Description |
|---|---|---|---|---|
| **Elevation / Topography** | `data/maps/earth_elevation.png` | NOAA NCEI ETOPO 2022 v1 / NASA Blue Marble (`usgs/`, `gebco/`) | 21600×10800 DEM (15 arc-sec) | Global relief combining high-resolution bathymetry and land topography. |
| **Mean Temperature** | `data/maps/earth_temperature.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_1.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Annual Mean Surface Temperature (-50°C to +50°C), Fick & Hijmans (2017). |
| **Precipitation / Moisture** | `data/maps/earth_precipitation.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_12.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Annual Total Precipitation (0 to 3,000+ mm/year), Fick & Hijmans (2017). |
| **Seasonality / Thermal Range**| `data/maps/earth_seasonality.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_4.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Temperature Seasonality (Standard Deviation × 100), Fick & Hijmans (2017). |

### Tab 2 — Resources, Ecology & Geology Tensors
| Geological / Resource Tensor | Disk Location | Source Provider & Raw File | Academic Citation / Description |
|---|---|---|---|
| **Biomes / Land Cover** | `data/maps/earth_biomes.png` | NASA MODIS MCD12C1 Land Cover Type 1 (IGBP) | NASA Terra/Aqua MODIS Land Cover Climate Modeling Grid (0.05° resolution). |
| **Coal Basins** | `data/maps/earth_coal.png` | USGS MRDS & BGR Germany (`usgs_mrds/`, `bgr_germany/`) | Global distribution of anthracitic, bituminous, and lignite coal deposits. |
| **Crude Oil Fields** | `data/maps/earth_oil.png` | World Energy Council (WEC) & EIA Petroleum Basins (`wep_world_energy/`) | Proven conventional & unconventional petroleum reservoirs. |
| **Natural Gas Reserves** | `data/maps/earth_gas.png` | WEC / BGR Global Natural Gas Database (`wep_world_energy/`, `bgr_germany/`) | Associated and non-associated natural gas fields. |
| **Uranium Deposits** | `data/maps/earth_uranium.png` | IAEA UDEPO World Distribution of Uranium Deposits (`iaea_nfcis/`) | International Atomic Energy Agency Nuclear Fuel Cycle Information System. |
| **Helium-3 / Regolith** | `data/maps/earth_helium3.png` | Planetary Baselines / Lunar Regolith Modeling (`lpi_lunar/`) | Modeled volatile isotopic concentrations and trace mantle anomalies. |
| **Iron & Copper Deposits** | `data/maps/earth_iron_copper.png` | USGS MRDS Porphyry & Banded Iron Formations (`usgs_mrds/`) | Major global iron ore and copper porphyry belts. |
| **Precious Metals & REE** | `data/maps/earth_precious_metals.png` | USGS MRDS Gold, Platinum Group & Rare Earth Minerals (`usgs_mrds/`) | High-value mineral veins and carbonatite rare-earth complexes. |
| **Geothermal / Mantle Heat** | `data/maps/earth_geothermal.png` | IHFC Davies (2013) Global Heat Flow Grid (`ihfc_davies2013/heat_flow_2deg.csv`) | Global heat flow in mW/m² (Davies 2013, G-cubed, doi:10.1002/ggge.20271). |
| **Freshwater Aquifers** | `data/maps/earth_aquifers.png` | UNESCO / BGR WHYMAP Global Groundwater (`whymap_groundwater/`) | World Hydrogeological Map mega-aquifer systems and groundwater reserves. |

### Tab 3 — Historical Scenarios & Cliodynamics
| Historical Scenario Tensor | Cache Location / Source | Primary Provider & Base Datasets | Theoretical & Empirical Foundation |
|---|---|---|---|
| **Demographic Density (HYDE 3.4)** | `data/cache/*_density.png` | Utrecht University HYDE 3.4 (`hyde34/`) | Klein Goldewijk et al. (2017) Anthropogenic land use and population density (-10,000 BC to 2024 AD). |
| **Institutional Complexity** | `data/cache/*_institutional.png` | Seshat Global History Databank (`seshat/`) | Turchin et al. (2018) Quantitative historical polity governance and administrative hierarchy. |
| **Linguistic & Isogloss Boundaries**| `data/cache/*_isogloss.png` | Ethnologue / Glottolog / Natural Earth | Global linguistic family distribution and dialect continua. |
| **Kinship & Social Organization** | `data/cache/*_kinship.png` | Seshat Global History Databank (`seshat/`) | Murdock Ethnographic Atlas & Seshat social structural typologies. |
| **Rituals & Sacred Centers** | `data/cache/*_rituals.png` | Seshat / D-PLACE Databank | Religious architectures, ritual practice intensity, and sacred geographic nodes. |
| **Sovereignty & State Power** | `data/cache/*_sovereignty.png` | Centennia Historical Atlas / Seshat | Territorial control, sovereign frontiers, and tributary state boundaries. |
| **Technology & Metallurgy Level** | `data/cache/*_technology.png` | Seshat / Archaeoglobe Project (`archaeoglobe/`) | Stephens et al. (2019) Archaeological milestone diffusion (Iron, Bronze, Agriculture). |
| **Trade Networks & Silk Routes** | `data/cache/*_tradenetwork.png` | Seshat / Ancient World Mapping Center | Maritime, caravan, and riverine commercial conduits. |
| **Pathogen & Epidemic Risk** | `data/cache/*_pathogen.png` | GBD / Historical Epidemiology Databank | Biome-specific vector burdens, zoonotic reservoirs, and epidemic corridors. |
| **Historical GDP & Production** | Maddison Project Database (`maddison/`) | Maddison Historical Statistics (Bolt & van Zanden, 2020) | Per capita output and regional economic productivity over time. |

---

## 6. Global & Planetary Geospatial Datasets Attribution

| Dataset | Issuing Institution / Authors | Resolution / Coverage | License | Citation / URL |
|---|---|---|---|---|
| **ETOPO1 Global Relief Model** | NOAA National Centers for Environmental Information (NCEI) | 1 arc-minute (~1.8 km) global relief & bathymetry | Public Domain (U.S. Govt) | Amante & Eakins (2009), NOAA Tech Memo NESDIS NGDC-24. [DOI: 10.7289/V5C8276M](https://doi.org/10.7289/V5C8276M) |
| **GEBCO 2023 / 2024 Grid** | General Bathymetric Chart of the Oceans / IHO-IOC | 15 arc-second sub-surface bathymetry & elevation | Public Domain / CC BY 4.0 | GEBCO Compilation Group (2023). [https://www.gebco.net](https://www.gebco.net) |
| **WWF Terrestrial Ecoregions** | World Wildlife Fund (WWF) | Global biomes & 867 ecoregions | CC BY 4.0 | Olson, D. M., et al. (2001). *Terrestrial Ecoregions of the World: A New Map of Life on Earth*. BioScience, 51(11), 933-938. |
| **USGS MRDS** | U.S. Geological Survey | Global mineral deposits, ores, and mines | Public Domain (USGS) | Mineral Resources Data System. [https://mrdata.usgs.gov/mrds/](https://mrdata.usgs.gov/mrds/) |
| **PMIP4 / CMIP6 Paleoclimate** | World Climate Research Programme (WCRP) | Mid-Holocene, LGM, LIG climate grids | Open Access / CMIP6 terms | Kageyama, M., et al. (2018). *The PMIP4 contribution to CMIP6*. Geoscientific Model Development, 11, 1033-1057. [DOI: 10.5194/gmd-11-1033-2018](https://doi.org/10.5194/gmd-11-1033-2018) |
| **HYDE 3.2 / 3.4** | Netherlands Environmental Assessment Agency (PBL) | Historical population, cropland, and pasture (-10,000 BC–Present) | CC BY 4.0 | Klein Goldewijk, K., et al. (2017). *Anthropogenic land use estimates for the Holocene*. Earth System Science Data, 9, 927-953. [DOI: 10.5194/essd-9-927-2017](https://doi.org/10.5194/essd-9-927-2017) |
| **Natural Earth** | Natural Earth Contributors | Coastlines, boundaries, rivers, physical geography | Public Domain | [https://www.naturalearthdata.com](https://www.naturalearthdata.com) |
| **NASA GIBS** | NASA Earth Science Data and Information System | Global near real-time & historical planetary imagery | Public Domain (NASA) | [https://wiki.earthdata.nasa.gov/display/GIBS](https://wiki.earthdata.nasa.gov/display/GIBS) |
| **NASA PDS Planetary Cartography** | NASA Planetary Data System / USGS Astrogeology | Lunar LRO LOLA DEM, Mars MGS MOLA DEM, Venus Magellan SAR, Mercury MESSENGER MLA DEM | Public Domain (NASA) | [https://pds.nasa.gov](https://pds.nasa.gov) |
| **IAEA PRIS** | International Atomic Energy Agency | Global nuclear power reactor database & operational safety data | Open Data (IAEA) | [https://pris.iaea.org](https://pris.iaea.org) |
| **BGR World Energy Resources** | Federal Institute for Geosciences and Natural Resources (Germany) | Global fossil energy reserves, uranium, and geothermal flux | Open Access (BGR) | BGR (2022). *Energy Study: Reserves, Resources and Availability of Energy Raw Materials*. |
| **PANGAEA** | Data Publisher for Earth & Environmental Science | Marine sediment cores, paleoclimate proxies, ice core $\delta^{18}\text{O}$ and $\text{CO}_2$ | CC BY 3.0 / CC BY 4.0 | [https://www.pangaea.de](https://www.pangaea.de) |
| **WHYMAP Groundwater Data** | UNESCO / BGR | Global groundwater aquifer geometry, thickness & recharge rates | CC BY-NC-SA 3.0 IGO | Struckmeier, W., et al. (2008). *WHYMAP Groundwater Resources of the World*. |

---

## 7. Macro-Historical & Cliodynamic Data Benchmarks

| Project / Archive | Lead Institution / Authors | Coverage | Benchmark Usage in Ether | Citation / URL |
|---|---|---|---|---|
| **Seshat: Global History Databank** | Seshat / Evolution Institute (Peter Turchin et al.) | 400+ historical polities across 10,000 years | Elite Overproduction, Sociopolitical Instability, Asabiyyah decay parameters | Turchin, P., et al. (2015). *Seshat: The Global History Databank*. Cliodynamics, 6(1), 77-107. [https://seshatdatabank.info](https://seshatdatabank.info) |
| **Correlates of War (COW)** | University of Michigan (Singer & Small) | Interstate, intra-state, and non-state wars (1816–Present) | Conflict casualty baselines, sociopolitical violence escalation | Singer, J. D., & Small, M. (1972 / 2020). *The Wages of War*. Correlates of War Project. [https://correlatesofwar.org](https://correlatesofwar.org) |
| **Maddison Project Database (2020)** | Groningen Growth and Development Centre (GGDC) | Historical GDP per capita & Gross World Product (GWP) | Real economic output calibration in 1990 International Geary-Khamis dollars | Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*. [https://www.rug.nl/ggdc/historicaldevelopment/maddison/](https://www.rug.nl/ggdc/historicaldevelopment/maddison/) |
| **ORBIS Geospatial Network** | Stanford University (Walter Scheidel) | Roman transportation network, freight velocity, and travel costs | Historical information transmission velocity & transport friction | Scheidel, W. (2014). *ORBIS: The Stanford Geospatial Network Model of the Roman World*. [https://orbis.stanford.edu](https://orbis.stanford.edu) |
| **World Bank & FAOSTAT** | World Bank & UN Food and Agriculture Organization | Agricultural crop yields, macronutrient production, fertilizer usage | Calibrating van Genuchten soil water, N-P-K harvest depletion & EROEI | [https://data.worldbank.org](https://data.worldbank.org) \| [https://www.fao.org/faostat](https://www.fao.org/faostat) |

---

## 8. Scientific Literature & Mathematical Foundations

### Celestial Mechanics, Climate & Geophysics
* **Milankovitch, M.** (1941). *Kanon der Erdbestrahlung und seine Anwendung auf das Eiszeitenproblem*. Royal Serbian Academy Special Publication 133.
* **Stefan, J.** (1879). *Über die Beziehung zwischen der Wärmestrahlung und der Temperatur*. Sitzungsberichte der Mathematisch-Naturwissenschaftlichen Classe der Kaiserlichen Akademie der Wissenschaften, 79, 391–428.
* **Clausius, R.** (1850). *Ueber die bewegende Kraft der Wärme und die Gesetze, welche sich daraus für die Wärmelehre selbst ableiten lassen*. Annalen der Physik, 155(3), 368-397.
* **Stommel, H.** (1961). *Thermohaline convection with two stable regimes of flow*. Tellus, 13(2), 224-230.
* **Farquhar, G. D., von Caemmerer, S., & Berry, J. A.** (1980). *A biochemical model of photosynthetic $\text{CO}_2$ assimilation in leaves of C3 species*. Planta, 149(1), 78-90.
* **van Genuchten, M. T.** (1980). *A closed-form equation for predicting the hydraulic conductivity of unsaturated soils*. Soil Science Society of America Journal, 44(5), 892-898.
* **Stull, R.** (2011). *Wet-bulb temperature from relative humidity and air temperature*. Journal of Applied Meteorology and Climatology, 50(11), 2267-2269.
* **Rockström, J., et al.** (2009) & **Steffen, W., et al.** (2015). *Planetary boundaries: Guiding human development on a changing planet*. Science, 347(6223), 1259855.

### Macro-Sociology, Cliodynamics & Complex Systems
* **Turchin, P.** (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
* **Turchin, P.** (2016). *Ages of Discord: A Structural-Demographic Analysis of American Polity*. Beresta Books.
* **Tainter, J. A.** (1988). *The Collapse of Complex Societies*. Cambridge University Press.
* **Motesharrei, S., Rivas, J., & Kalnay, E.** (2014). *Human and nature dynamics (HANDY): Modeling inequality and use of resources in the collapse or sustainability of societies*. Ecological Economics, 101, 90-102.
* **Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W.** (1972). *The Limits to Growth*. Universe Books / Club of Rome.
* **Kümmel, R.** (2011). *The Second Law of Economics: Energy, Entropy, and Wealth Creation*. Springer.
* **Ayres, R. U., & Warr, B.** (2009). *The Economic Growth Engine: How Energy and Work Drive Material Prosperity*. Edward Elgar Publishing.
* **West, G. B.** (2017). *Scale: The Universal Laws of Growth, Innovation, Sustainability, and the Pace of Life in Organisms, Cities, Economies, and Companies*. Penguin Press.
* **Bettencourt, L. M. A., et al.** (2007). *Growth, innovation, scaling, and the pace of life in cities*. PNAS, 104(17), 7301-7306.
* **Price, G. R.** (1970). *Selection and covariance*. Nature, 227(5257), 520-521.
* **Boserup, E.** (1965). *The Conditions of Agricultural Growth: The Economics of Agrarian Change under Population Pressure*. Allen & Unwin.
* **Arthur, W. B.** (2009). *The Nature of Technology: What It Is and How It Evolves*. Free Press.
* **Hotelling, H.** (1931). *The economics of exhaustible resources*. Journal of Political Economy, 39(2), 137-175.
* **Acemoglu, D., & Robinson, J. A.** (2012). *Why Nations Fail: The Origins of Power, Prosperity, and Poverty*. Crown Business.
* **Granovetter, M.** (1978). *Threshold models of collective behavior*. American Journal of Sociology, 83(6), 1420-1443.
* **Krugman, P.** (1991). *Increasing returns and economic geography*. Journal of Political Economy, 99(3), 483-499.
* **Schelling, T. C.** (1971). *Dynamic models of segregation*. Journal of Mathematical Sociology, 1(2), 143-186.
* **Axelrod, R.** (1997). *The dissemination of culture: A model with local convergence and global polarization*. Journal of Conflict Resolution, 41(2), 203-226.
* **Smil, V.** (2010). *Energy Transitions: History, Requirements, Prospects*. Praeger.
* **Smil, V.** (2017). *Energy and Civilization: A History*. MIT Press.
* **Ostrom, E.** (1990). *Governing the Commons: The Evolution of Institutions for Collective Action*. Cambridge University Press.
* **Scott, J. C.** (2017). *Against the Grain: A Deep History of the Earliest States*. Yale University Press.
* **Pinker, S.** (2011). *The Better Angels of Our Nature: Why Violence Has Declined*. Viking.
* **Nordhaus, W. D.** (2017). *Integrated Assessment Models of Climate Change (DICE-2016R)*. PNAS, 114(7), 1518-1523.

---

## 9. Software Dependencies & Open-Source Libraries

| Library / Tool | Version | License | Role in Ether |
|---|---|---|---|
| **Uber H3** | 4.1.1 | Apache 2.0 | Hexagonal hierarchical discrete global spatial grid indexing |
| **OpenJFX / JavaFX** | 21.0.1 | GPLv2 with Classpath Exception | Multi-platform GUI desktop presentation layer & 2D/3D map canvas |
| **FasterXML Jackson** | 2.16.0 | Apache 2.0 | High-performance JSON deserialization & scenario configuration parser |
| **SLF4J & Logback** | 2.0.9 / 1.4.14 | MIT / EPL 1.0 | Logging framework, execution profiling, and telemetry tracing |
| **JUnit 5 / AssertJ / Mockito** | 5.10.1 / 3.24.2 / 5.7.0 | EPL 2.0 / Apache 2.0 / MIT | Automated unit, property, and determinism verification test suites |
| **HikariCP** | 5.1.0 | Apache 2.0 | High-performance zero-overhead JDBC connection pool for PostGIS |
| **PostgreSQL & PostGIS JDBC** | 42.7.1 / 2023.1.0 | BSD / LGPL | Relational geospatial persistence for historical archive telemetry |
| **Hibernate Spatial** | 6.4.1.Final | LGPL 2.1 | Spatial ORM bridging Java domain entities to PostGIS geometry columns |
| **GeoTools** | 30.1 | LGPL 2.1 | Multi-format GIS ingestion driver (GeoTIFF, Shapefile, NetCDF parsing) |
| **TornadoVM** | 1.0.9-SNAPSHOT | Apache 2.0 / GPLv2+CE | Dynamic JIT compilation of Java bytecode to OpenCL / NVIDIA PTX GPU kernels |
| **Spotless Plugin** | 2.41.0 | Apache 2.0 | Automated Java code formatting enforcing AOSP / Google Java Style |
| **JaCoCo** | 0.8.12 | EPL 2.0 | Automated code coverage analysis for physical and numerical routines |

---

## 10. Trademarks & Attribution Notice

* **Uber H3** is a registered trademark of Uber Technologies, Inc.
* **Java** is a registered trademark of Oracle Corporation and/or its affiliates.
* **NASA**, **NOAA**, **USGS**, **IAEA**, and **WWF** logos and dataset names are trademarks of their respective organizations and are used here under fair academic attribution.
