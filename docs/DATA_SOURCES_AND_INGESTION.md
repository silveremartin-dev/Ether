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

## 6. Cartographic Matrix Layer Pre-generation & Historical Epoch Standards

For every supported historical epoch (from -100,000 BP to present-day) in `data/maps/ether/<planet>/<year>/`:

### 6.1 Standard 25-Channel Tensor Schema
Every pre-generated epoch directory contains 25 standardized geospatial rasters (2:1 equirectangular PNG, 2048×1024 or higher) reprojected to the planetary Uber H3 discrete global mesh:

1. **Geophysical & Topographic Tensors (Channels 1–6)**:
   - `elevation.png` (Topography & Bathymetry, NOAA ETOPO 2022 / GEBCO).
   - `temperature_annual.png` & `temperature_seasonality.png` (WorldClim v2.1 / Paleoclimate).
   - `precipitation_annual.png` (Total annual moisture flux).
   - `aquifers_groundwater.png` (UNESCO WHYMAP Piezometric water tables).
   - `geothermal_heatflow.png` (IHFC Davies 2013 heat flow grid).
2. **Pedological & Resource Tensors (Channels 7–13)**:
   - `biomes_landcover.png` (NASA MODIS / Paleo-vegetation reconstructions).
   - `soil_organic_carbon.png` & `soil_nitrogen_phosphorus.png` (Liebig nutrient stocks).
   - `coal_deposits.png`, `oil_gas_reserves.png`, `iron_copper_ores.png`, `uranium_deposits.png` (USGS MRDS / BGR).
3. **Cliodynamic & Anthropogenic Tensors (Channels 14–21)**:
   - `demographic_density.png` (HYDE 3.4 log-density gradient).
   - `political_sovereignty.png` (Polity territorial masks & border frontiers).
   - `institutional_complexity.png` (Seshat administrative hierarchy index).
   - `technology_metallurgy.png` (Lithic / Bronze / Iron / Industrial technology isochrones).
   - `trade_corridors.png` (Caravan, riverine, and maritime commercial flux).
   - `linguistic_isoglosses.png` (Glottolog dialect continua).
   - `kinship_structures.png` (Murdock ethnographic descent systems).
   - `sacred_rituals.png` (Sacred geography & ritual architectures).
4. **Epidemiological & Ecological Impact Tensors (Channels 22–25)**:
   - `pathogen_zoonotic_risk.png` (Endemic disease burden & vector suitability).
   - `megafauna_density.png` (Late Pleistocene / Holocene megafauna biomass).
   - `ecological_footprint.png` (Soil salinization, deforestation & degradation).
   - `agricultural_carrying_capacity.png` (Agro-climatic caloric potential).

### 6.2 Provenance Documentation Contract
Each epoch directory MUST include:
- `provenance_and_sources.json`: Machine-readable metadata specifying the exact data provider, DOI, spatial resolution, interpolation kernel (e.g. Kriging, PCHIP, Bilinear), and coordinate reference system.
- `README.md`: English technical narrative detailing archaeological hearths, historical boundaries, paleoclimatic ice-sheet configurations, and methodological assumptions.

### 6.3 Pre-generation Execution Workflow
1. **Automated Rasterization**: Run the offline tensor generator (`HistoricalMapGenerator.java` / GIS ingest runner) to bake equirectangular rasters for keyframe anchor years:
   ```bash
   # Pre-render keyframe tensor layers for Earth scenarios
   mvn test -Dtest=CartographicPreGenerationSuite
   ```
2. **H3 Discrete Mesh Packing**: Rasters are sampled onto H3 cell centroids ($O(N)$ lookup) and saved as packed binary `WorldBuffer` structures for sub-millisecond scenario loading.
3. **Dynamic Scrubbing in Tab 6**: When scrubbing between keyframe anchors on the Comparative Analytics date slider, the engine applies real-time geodesic interpolation, enabling smooth continuous playback across 100,000 years.

---

## 7. Macro-Historical & Cliodynamic Data Benchmarks (27-Variable Suite)

Ether maintains a standardized 27-variable ground truth benchmark suite (`historical_cliodynamic_benchmarks.json`) spanning 100,000 BCE to 2026 CE:

| Benchmark Key | Variable Name | Physical / Macroeconomic Unit | Empirical Source Citation | Epoch Window |
|---|---|---|---|---|
| `worldPopulation` | World Population | Millions of people | McEvedy & Jones (1978), HYDE 3.2, UN WPP (2024) | DEEP_HORIZON |
| `grossWorldProduct` | Gross World Product (GWP) | Billion 1990 Geary-Khamis $ | Maddison Project Database (Bolt & van Zanden 2020) | EARLY_MODERN_500YR |
| `primaryEnergy` | Primary Energy Consumption | Exajoules (EJ) | Vaclav Smil (2017) 'Energy and Civilization', IEA | DEEP_HORIZON |
| `urbanizationRate` | Urbanization Rate | % in settlements > 5k hab. | Paul Bairoch (1988), Chandler (1987), UN Prospects | CLASSICAL_MEDIEVAL |
| `co2Concentration` | Atmospheric $\text{CO}_2$ | Parts Per Million (ppm) | Law Dome / EPICA Dome C Ice Cores, NOAA Mauna Loa | DEEP_HORIZON |
| `temperatureAnomaly` | Surface Temp Anomaly | °C (relative to 1850–1900) | PAGES 2k Consortium (2019), NASA GISS, HadCRUT5 | DEEP_HORIZON |
| `milankovitchInsolation` | Summer Insolation 65°N | $\text{W/m}^2$ | Laskar et al. (2004), Berger (1978) | DEEP_HORIZON |
| `megafaunaIndex` | Megafauna Biomass Index | % of Late Pleistocene Baseline | Sandom et al. (2014), Barnosky (2004), WWF (2024) | DEEP_HORIZON |
| `zeroContainmentScore` | Biogeographical Isolation | Normalized Score [0.0, 1.0] | Crosby (1972) 'Columbian Exchange', Diamond (1997) | DEEP_HORIZON |
| `lifeExpectancyAtBirth` | Life Expectancy ($e_0$) | Years | Riley (2005), UN Population Division, HYDE 3.4 | DEEP_HORIZON |
| `literacyRate` | Adult Literacy Rate | % of adult population | Buringh & van Zanden (2009), UNESCO (2024) | EARLY_MODERN_500YR |
| `currencyDebasement` | Coinage Silver Purity | % pure silver in standard coin | Butcher & Ponting (2014) 'Roman Silver Coinage' | CLASSICAL_MEDIEVAL |
| `eliteOverproductionIndex` | Elite Overproduction | Normalized Index (1.0 = Base) | Peter Turchin (2016) 'Ages of Discord', Seshat DB | CLASSICAL_MEDIEVAL |
| `politicalStressIndex` | Political Stress Index (PSI) | Composite Index [0, 100] | Turchin & Nefedov (2009) 'Secular Cycles' | CLASSICAL_MEDIEVAL |
| `asabiyyahSocialCohesion` | Asabiyyah Solidarity | Normalized Score [0.0, 1.0] | Ibn Khaldun (1377), Turchin (2003) | CLASSICAL_MEDIEVAL |
| `sociopoliticalInstability`| Conflict & Violence Index | Events / Decade | Seshat Global History Databank, COW Project | DEEP_HORIZON |
| `giniInequality` | Wealth & Land Gini | Gini Coefficient [0.0, 1.0] | Walter Scheidel (2017) 'The Great Leveler' | EARLY_MODERN_500YR |
| `realUnskilledWage` | Real Unskilled Wage | Basket Index (100 = Baseline) | Robert C. Allen (2001) 'The Great Divergence' | EARLY_MODERN_500YR |
| `sovereignDebtBurden` | Sovereign Debt / Revenue | % of Gross Revenue | Reinhart & Rogoff (2009), IMF Global Debt DB | EARLY_MODERN_500YR |
| `metalSmeltingPerCapita` | Metal Smelting Output | kg / capita / year | World Steel Association, USGS Mineral Statistics | DEEP_HORIZON |
| `fossilSharePrimaryEnergy`| Fossil Energy Share | % of Total Primary Energy | Vaclav Smil (2017), Our World in Data (2024) | EARLY_MODERN_500YR |
| `globalTradeVolume` | Global Trade Volume | Index (100 = 1913 level) | Federico & Tena-Junguito (2017), WTO (2024) | EARLY_MODERN_500YR |
| `shippingFreightCostIndex`| Real Maritime Freight Cost | Index (100 = 1913 level) | Harley (1988), Mohammed & Williamson (2004) | EARLY_MODERN_500YR |
| `soilErosionRate` | Cumulative Topsoil Loss | % loss relative to Holocene | Montgomery (2007) 'Dirt: Erosion of Civilizations' | DEEP_HORIZON |
| `agriculturalEroei` | Agricultural EROEI | Ratio (Caloric Out / In) | Vaclav Smil (2008), Giampietro et al. (2013) | DEEP_HORIZON |
| `deforestationRate` | Forest Cover Remaining | % of original post-glacial | HYDE 3.2, FAO Global Forest Resources | DEEP_HORIZON |
| `informationSpeed` | Information Travel Speed | Kilometers / day (km/day) | Scheidel (2014) 'Stanford ORBIS Roman Network' | DEEP_HORIZON |

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
