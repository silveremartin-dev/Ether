# Ether Cartographic & GIS Data Repository (`data/maps/`)

All spatial, geological, climate, and demographic datasets in the **Ether Simulation Engine** are organized strictly by accredited provider directory under `data/maps/<provider_id>/`.

## Directory & Provider Inventory

1. **`usgs/`**: United States Geological Survey (ETOPO1, SRTM, GTOPO30).
2. **`gebco/`**: General Bathymetric Chart of the Oceans (Bathymetry Grids).
3. **`nasa_pds/`**: NASA Planetary Data System (Moon LOLA, Mars MOLA, Venus Magellan, Mercury MLA).
4. **`esa_geospatial/`**: European Space Agency & Copernicus DEM GLO-30.
5. **`usgs_mrds/`**: USGS Mineral Resources Data System (Coal, Oil, Natural Gas, Iron, Copper, Rare Earths, Lithium, Bauxite).
6. **`bgr_germany/`**: Bundesanstalt für Geowissenschaften und Rohstoffe (Global Energy & Mineral Reserves).
7. **`iaea_nfcis/`**: International Atomic Energy Agency UDEPO & ThDEPO (Uranium & Thorium Databases).
8. **`wep_world_energy/`**: World Energy Council & EIA (Fossil Energy Basins).
9. **`lpi_lunar/`**: Lunar and Planetary Institute (Lunar Helium-3 & Regolith Volatiles).
10. **`whymap_groundwater/`**: UNESCO / BGR WHYMAP (World Hydrogeological Map & Global Aquifers).
11. **`hyde34/`**: Utrecht University Vault (HYDE 3.4 Demographics -10,000 BC to 2024 AD).
12. **`naturalearth/`**: Natural Earth Physical & Cultural Vector Datasets (GeoJSON).
13. **`seshat/`**: Seshat Global History Databank (Polity & Cliodynamic Benchmarks).
14. **`paleoclim/`**: CHELSA-Trace21k / PaleoCLIM Project.
15. **`chelsa/`**: CHELSA High-Resolution Climatology.
16. **`worldclim/`**: WorldClim Climate Normals.
17. **`paleomap/`**: PALEOMAP Project (C.R. Scotese).
18. **`pmip/`**: Paleoclimate Modelling Intercomparison Project.
19. **`pangea/`**: PANGAEA Data Publisher for Earth & Environmental Science.
20. **`archaeoglobe/`**: Archaeoglobe Archaeological Land Use Project.
21. **`maddison/`**: Maddison Project Historical GDP & Population Databank.
22. **`ihfc_davies2013/`**: International Heat Flow Commission — Davies (2013) Global Heat Flow Dataset.
23. **`cache/`**: Pre-cached Scenario Raster PNG Tensors.

## Provider Datasets & Location Matrix

| Provider Directory | Filename | Description | Format | Resolution / Size |
|---|---|---|---|---|
| `ihfc_davies2013/` | `heat_flow_2deg.csv` | Global mantle heat flow (Mean, Median, Error) in mW/m² — **Geology Tensor 4.2.8 (MANTLE_HEAT)**. Sourced from Geochemistry, Geophysics, Geosystems, doi:10.1002/ggge.20271. Consumed by `HistoricalMapGenerator.generateCleanMantleHeatMap()`. | CSV | 2° × 2° lon-lat regular grid (16,200 points) |

## Repository Audit & Verification Datasets (`data/maps/`)

- **`repatriation_audit.json`**: Automated audit tracking provider authenticity, file counts, and dataset sampling across all 22 cartographic providers.
- **`download_status.json`**: Real-time status manifest for large GIS asset downloads and local synchronization.

## Geological Tensor Layer Mapping

| Layer Index | Tensor ID | Provider Directory | Map File / Source |
|---|---|---|---|
| 0 | `COAL` | `usgs_mrds/`, `bgr_germany/` | USGS MRDS Coal Basins |
| 1 | `CRUDE_OIL` | `wep_world_energy/` | WEC Petroleum Basins |
| 2 | `NATURAL_GAS` | `wep_world_energy/`, `bgr_germany/` | WEC/BGR Gas Fields |
| 3 | `URANIUM` | `iaea_nfcis/` | IAEA UDEPO Database |
| 4 | `HELIUM_3` | `lpi_lunar/` | NASA/LPI Lunar Regolith |
| 5 | `IRON_COPPER` | `usgs_mrds/` | USGS BIF & Porphyry Copper |
| 6 | `PRECIOUS_REE` | `usgs_mrds/` | USGS Gold, PGMs & REE |
| 7 | `MANTLE_HEAT` | `ihfc_davies2013/` | **`ihfc_davies2013/heat_flow_2deg.csv`** — Davies (2013) 2° grid |
| 8 | `FRESHWATER_AQUIFERS` | `whymap_groundwater/` | UNESCO WHYMAP Global Aquifer Map |
