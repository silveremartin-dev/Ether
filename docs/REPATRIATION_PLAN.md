# Planetary Cartography, GIS Repatriation & Ingestion Architecture (Ether 2.0 / 4.0)

> **Strategic Engineering Specification & GIS Repatriation Roadmap**  
> *Ether Simulation Engine — Planetary Ingestion Pipeline, Spatial Tensor Normalization, Metadata Standards, and Storage Governance*

---

## 1. Executive Summary & Pipeline Architecture

The **Ether Simulation Engine** ingests, unifies, and maps global cartographic, bathymetric, paleoclimatic, mineralogical, demographic, and extraterrestrial planetary datasets (Earth, Moon, Mars, Venus, Mercury) onto a standardized **Uber H3 hexagonal grid** (Resolutions 6 to 8).

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PLANETARY GIS REPATRIATION PIPELINE                      │
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
1. **Multi-Source Ingestion**: Heterogeneous geospatial data formats (GeoTIFF rasters, NetCDF grids, ESRI Shapefiles, tabular `.tab`/`.csv` archives) are parsed by dedicated ingestion drivers in `org.ether.gis`.
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

To ensure repository scalability, zero Git bloat, and clean architectural boundaries, the repatriation workflow adheres to the following principles:

1. **Consolidation of Redundant Climate Grids**:
   - Historical and paleoclimate data from PMIP and CHELSA are consolidated directly under `paleoclim/` (CHELSA-Trace21k / PaleoCLIM) and `worldclim/` (WorldClim v2.1 normals).
2. **Unified Altimetry & Bathymetry**:
   - High-resolution terrestrial elevation and ocean depth are standardized on NOAA NCEI ETOPO 2022 v1 (in `usgs/`) and GEBCO 2024 (in `gebco/`), superseding redundant regional DEMs.
3. **Git Hygiene & Large Binary Handling**:
   - All multi-megabyte and gigabyte binary assets (`.tif`, `.tab`, `.xlsx`, `.zip`, `.nc`) are strictly isolated via `.gitignore`.
   - Lightweight metadata manifests and download scripts enable automated, repeatable repatriation on developer workstations and CI/CD runners.

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

## 6. Verification & Code Qualification Checklist

- [x] All active cartographic directories in `data/maps/` contain valid `provider.json` and `provider_manifest.json` descriptors.
- [x] Large raw binary archives are excluded from Git version control via `.gitignore` while maintaining local deterministic availability.
- [x] Pre-cached scenario tensor rasters (`data/cache/`) build cleanly and load deterministically across all scenario benchmarks.
- [x] All reference maps render instantly (< 5 ms) via direct JavaFX canvas blitting (`gc.drawImage`).
- [x] All GIS ingestion engines pass test suites (`mvn test-compile` and `mvn test`).


