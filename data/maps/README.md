# Ether Cartographic & GIS Data Repository (`data/maps/`)

> **Authoritative Read-Only Repository & Cartographic Catalog**  
> *Ether Simulation Engine — Spatial Data Ingestion, Reference Rasters & Tensor Layer Provenance*

---

## 1. Overview & Storage Architecture

The `data/maps/` directory is the **authoritative, read-only spatial repository** for the Ether Simulation Engine. It contains:
1. **Accredited Provider Repositories**: Raw geospatial data, vector shapes, GeoTIFFs, and tabular datasets categorized under `data/maps/<provider_id>/`.
2. **Standardized Reference Rasters**: High-fidelity, uncompressed 2:1 equirectangular PNG reference rasters directly consumed by the UI and simulation core.
3. **Procedural & Cache Pipeline**: All recolorizations, runtime scenario rasters, and derived layers are cached deterministically in `data/cache/`.

### Zero-Latency Rendering Engine
When browsing reference maps in **Tab 1 (Planet Generator)**, **Tab 2 (Resources & Ecology)**, and **Tab 3 (Historical Scenarios)**:
- Pre-loaded raster images are blitted directly onto JavaFX graphics contexts via `gc.drawImage(...)`.
- Zero CPU/noise recomputation occurs on map selection (< 5 ms latency).
- Procedural recomputations are restricted strictly to procedural mode parameter modifications.

---

## 2. Tab 1 — Planet Generator & Climate Reference Maps

| Reference Map | Disk Location | Source Dataset / Provider | Native Format / Resolution | Description & Academic Citation |
|---|---|---|---|---|
| **Elevation / Topography** | `data/maps/earth_elevation.png` | NOAA NCEI ETOPO 2022 v1 / NASA Blue Marble (`usgs/`, `gebco/`) | 21600×10800 DEM (15 arc-sec) | Global relief combining high-resolution bathymetry and land topography. |
| **Mean Temperature** | `data/maps/earth_temperature.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_1.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Annual Mean Surface Temperature (-50°C to +50°C), Fick & Hijmans (2017). |
| **Precipitation / Moisture** | `data/maps/earth_precipitation.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_12.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Annual Total Precipitation (0 to 3,000+ mm/year), Fick & Hijmans (2017). |
| **Seasonality / Thermal Range**| `data/maps/earth_seasonality.png` | WorldClim v2.1 Climatology (`worldclim/bio_10m/wc2.1_10m_bio_4.tif`) | GeoTIFF (10 arc-min / ~18.5 km) | Temperature Seasonality (Standard Deviation × 100), Fick & Hijmans (2017). |

---

## 3. Tab 2 — Resources, Ecology & Geology Tensors

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

---

## 4. Tab 3 — Historical Scenarios & Cliodynamics

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

## 5. Planetary Presets & Subdirectory Structure

In addition to Earth, Ether includes complete, authentic reference maps for other celestial bodies organized into dedicated preset directories under `data/maps/`:

### Planetary Presets Catalog

| Preset Subfolder | Body | Available Reference Layers | Authentic Primary Source Sensor / Dataset |
|---|---|---|---|
| `terre/` (alias `earth/`) | **Earth** | Elevation, Temperature, Precipitation, Seasonality, Biomes, Coal, Oil, Gas, Uranium, Helium-3, Iron/Copper, Precious Metals/REE, Geothermal, Aquifers | NOAA ETOPO 2022, WorldClim 2.1, MODIS MCD12C1, USGS MRDS (300k+), WEP/BGR, IAEA UDEPO, IHFC Davies 2013, UNESCO WHYMAP |
| `lune/` (alias `moon/`) | **Moon** | Elevation, Biomes (Regolith/Maria), Temperature, Helium-3 (Solar wind implantation), Iron/Copper (Ilmenite FeTiO3), Aquifers (Polar PSR ice) | NASA LRO LOLA DEM (118m/px), LEND neutron spectrometer, Lunar Prospector gamma ray, Apollo landing sites |
| `mars/` | **Mars** | Elevation, Biomes (Areography/Basins), Temperature, Iron/Copper (Hematite/Ferric dust), Aquifers (Utopia Planitia/Polar ice), Geothermal (Mantle hotspots) | NASA MGS MOLA MEGDR DEM (463m/px), Mars Global Surveyor TES, Mars Express OMEGA, InSight heat flux |
| `venus/` | **Venus** | Elevation, Biomes (Volcanic plains/Coronae), Temperature, Geothermal (Coronae mantle upwelling) | NASA Magellan Radar Altimetry GXDR (4.6km/px), Venus Express VIRTIS |
| `mercure/` (alias `mercury/`) | **Mercury** | Elevation, Biomes (Smooth plains/Caloris), Temperature, Aquifers (Permanently shadowed polar ice) | NASA MESSENGER MLA DEM (500m/px), MDIS multispectral |

---

## 6. Directory Structure & Ether Hierarchical Layout

```
data/maps/
├── README.md                          # Authoritative Data Catalog (This Document)
├── repatriation_audit.json            # Automated checksum and integrity audit
├── download_status.json               # Remote GIS asset sync status
│
├── ether/                             # Primary Engine Data Directory
│   ├── terre/ (alias: earth/)         # Earth full raster layers (Tab 1, Tab 2)
│   ├── lune/ (alias: moon/)           # Moon reference layers
│   ├── mars/                          # Mars reference layers
│   ├── venus/                         # Venus reference layers
│   └── mercure/ (alias: mercury/)     # Mercury reference layers
│
├── terre/ (alias: earth/)             # Root preset fallback mirrors
├── lune/ (alias: moon/)               # Root preset fallback mirrors
├── mars/                              # Root preset fallback mirrors
├── venus/                             # Root preset fallback mirrors
├── mercure/ (alias: mercury/)         # Root preset fallback mirrors
│
├── archaeoglobe/                      # Land Use Archaeological Survey
├── bgr_germany/                       # Federal Institute for Geosciences (Germany)
├── gebco/                             # General Bathymetric Chart of the Oceans
├── hyde34/                            # HYDE 3.4 Historical Demographics
├── iaea_nfcis/                        # IAEA Uranium & Thorium Database
├── ihfc_davies2013/                   # International Heat Flow Commission
├── lpi_lunar/                         # Lunar and Planetary Institute
├── maddison/                          # Maddison Historical Economic Statistics
├── nasa_pds/                          # NASA Planetary Data System (Moon, Mars, Venus, Mercury)
├── naturalearth/                      # Natural Earth Vectors
├── paleoclim/                         # CHELSA-Trace21k Climatology
├── paleomap/                          # C.R. Scotese Paleogeographic Reconstructions
├── pangea/                            # PANGAEA Earth Science Archives
├── seshat/                            # Seshat Global History Databank
├── usgs/                              # ETOPO / SRTM Topography
├── usgs_mrds/                         # USGS Mineral Resources Data System
├── wep_world_energy/                  # World Energy Council Energy Reserves
├── whymap_groundwater/                # UNESCO WHYMAP Groundwater
└── worldclim/                         # WorldClim v2.1 GeoTIFF Climatology
```

---

## 7. Raw GIS Datasets Acquisition & Provenance Catalog

For academic simulation fidelity at the highest standard, the exact external GIS shapefiles, GeoTIFFs, and geodatabases can be downloaded from accredited public repositories and placed into `data/maps/`:

### 1. Fossil Energy & Petroleum Basins (Coal, Oil, Gas)
- **EIA World Shale & Conventional Basins Shapefile**
  - **Provider**: U.S. Energy Information Administration (EIA) / Advanced Resources International
  - **Data Content**: Full GIS polygon boundaries for 137 worldwide sedimentary shale and conventional hydrocarbon basins.
  - **Direct Download URL**: `https://www.eia.gov/maps/map_data/ShaleGas_Oil_Basins_World_EIA.zip`
  - **Target Location**: `data/maps/wep_world_energy/ShaleGas_Oil_Basins_World_EIA.zip`
- **USGS World Petroleum Assessment TPS (Total Petroleum Systems)**
  - **Provider**: United States Geological Survey (USGS) World Energy Project
  - **Data Content**: Spatial boundaries and quantitative assessment of world petroleum systems and assessment units.
  - **Direct Download URL**: `https://pubs.usgs.gov/dds/dds-060/` (ESRI Shapefiles / Geodatabase)
  - **Target Location**: `data/maps/wep_world_energy/usgs_world_petroleum/`
- **Global Energy Monitor (GEM) Trackers**
  - **Global Coal Mine Tracker**: `https://globalenergymonitor.org/projects/global-coal-mine-tracker/` (4,300+ operational/proposed coal mines with exact lat/lon and annual metric tonnage)
  - **Global Oil & Gas Extraction Tracker**: `https://globalenergymonitor.org/projects/global-oil-gas-extraction-tracker/` (Global upstream oil and gas extraction units)
  - **Target Location**: `data/maps/wep_world_energy/gem_trackers/`

### 2. Nuclear Energy & Uranium Deposits
- **IAEA UDEPO (World Distribution of Uranium Deposits)**
  - **Provider**: International Atomic Energy Agency (IAEA) NFCIS
  - **Data Content**: 7,200+ geocoded deposits with deposit class (Unconformity-related, Sandstone, Proterozoic quartz-pebble conglomerate, Breccia complex/Olympic Dam), resource category (RAR, IR), and mined out tonnage.
  - **Direct Download URL**: `https://infcis.iaea.org/UDEPO/Deposits` (CSV / KML / Shapefile export)
  - **Target Location**: `data/maps/iaea_nfcis/iaea_udepo_deposits.csv`

### 3. Planetary Topography & Remote Sensing (NASA PDS)
- **Moon (LOLA DEM & Water Ice)**: NASA PDS Geosciences Node (`https://pds-geosciences.wustl.edu/lro/lro-l-lola-3-rdr-v1/lrolol_1xxx/`)
- **Mars (MOLA MEGDR Topography)**: NASA PDS Geosciences Node (`https://pds-geosciences.wustl.edu/mgs/mgs-m-mola-5-megdr-l3-v1/mgsl_300x/`)
- **Venus (Magellan Radar Altimetry GXDR / C3-MDIR)**: USGS Astrogeology Venus GIS Portal (`https://astrogeology.usgs.gov/search/map/Venus/Magellan/`)
- **Mercury (MESSENGER MLA DEM)**: NASA PDS MESSENGER Node (`https://pds-geosciences.wustl.edu/messenger/`)


