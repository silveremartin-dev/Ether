# Ether External Empirical Data Sources Catalog

## 1. Overview & Architectural Role
This directory (`data/sources/`) centralizes all **raw, semi-processed, and external empirical datasets** utilized by the Ether simulation platform.

### Strict Boundary Separation:
* **`data/maps/ether/`**: Contains the **derived, finalized planetary map tensors** (25 discrete PNG layers per epoch, H3 hexagonal tessellations) that directly initialize and run simulations in Ether.
* **`data/sources/`**: Contains the **primary scientific empirical datasets** (climatology, geophysics, geology, demography, archaeology, economics) used by offline ingestion pipelines to generate maps and calibrate Tier-1 physics and Tier-2 cliodynamic models.

---

## 2. Catalog of Empirical Datasets

### A. Paleoclimate & Meteorology
* **`pages2k/`**: PAGES 2k Consortium Common Era Surface Temperature Multi-Proxy Database (Phase 2, 647 site records in LiPD format) and Neukom et al. (2019) 2,000-year annual global mean temperature reconstruction ensemble (1–2000 CE).
* **`worldclim/`**: WorldClim v2.1 global empirical bioclimatic rasters (Bio1 annual mean temperature, Bio4 temperature seasonality, Bio12 annual precipitation) at 10-arc-minute and 5-arc-minute resolutions.
* **`paleoclim/`**: High-resolution bioclimatic rasters for the Late Quaternary (LGM, Heinrich Stadials).
* **`pangea/`**: Deep-sea core and paleoceanographic sediment datasets from the PANGAEA digital data library.

### B. Geophysics, Topography & Planetary
* **`gebco/`**: General Bathymetric Chart of the Oceans (GEBCO 2023 Grid) global relief model.
* **`paleomap/`**: Scotese PALEOMAP Project deep-time paleogeographic reconstructions.
* **`ihfc_davies2013`**: International Heat Flow Commission (IHFC) global terrestrial geothermal heat flow grid.
* **`nasa_pds/`**: NASA Planetary Data System topographic grids (MOLA for Mars, LOLA for the Moon).
* **`lpi_lunar/`**: Lunar and Planetary Institute impact crater and geological catalogs.

### C. Hydrogeology & Soil Stoichiometry
* **`whymap_groundwater/`**: UNESCO WHYMAP global groundwater resources, aquifer types, and recharge rates.
* **`usgs/`**: USGS national and global geochemical soil baselines (nitrogen, phosphorus, potassium, organic carbon).

### D. Mineral Resources & Physical Energy
* **`usgs_mrds/`**: USGS Mineral Resources Data System metallic ore reserves (copper, tin, iron, bauxite, rare earths).
* **`iaea_nfcis/`**: International Atomic Energy Agency (IAEA) Nuclear Fuel Cycle Information System uranium/thorium deposits.
* **`wep_world_energy/`**: Global Energy Monitor & USGS World Petroleum Assessment coal, oil, and gas deposits.
* **`bgr_germany/`**: Federal Institute for Geosciences and Natural Resources (Germany) fossil and mineral reserve statistics.

### E. Demography & Archaeology
* **`hyde34/`**: History Database of the Global Environment (HYDE 3.4) Holocene demographic density and agricultural land-use rasters (-10,000 BP to present).
* **`archaeoglobe/`**: Global archaeological land-use synthesis across 146 regional zones.
* **`un_wpp/`**: United Nations Department of Economic and Social Affairs (UN DESA) World Population Prospects (WPP 2022) global, regional, and national demographic series, age pyramids, fertility, and mortality projections (1950–2100).

### F. Economics, Institutions & Political Boundaries
* **`maddison/`**: Maddison Project Database (2020) historical real GDP per capita, population, and real wage indices.
* **`seshat/`**: Seshat Global History Databank polity social complexity, governance hierarchy, and political stress variables.
* **`cshapes/`**: CShapes global historical state boundaries and capital coordinates (1886–present).
* **`dplace/`**: D-PLACE database of places, language, culture, and environmental adaptations.
* **`naturalearth/`**: Natural Earth GIS vector physical and cultural administrative boundaries.
* **`iiasa_ssp/`**: International Institute for Applied Systems Analysis (IIASA) Shared Socioeconomic Pathways (SSP) multi-variable energy, emissions, and macroeconomic transition projections (2010–2100).

---

## 3. Storage & Version Control Rules
In adherence to Ether's data management policies and `.gitignore` directives:
* Heavy raster binaries (`.tif`, `.tiff`, `.nc`, `.asc`), spatial shapefiles (`.shp`), spreadsheets (`.xlsx`), and large archives (`.zip`, `.lpd`) are **strictly excluded from Git tracking**.
* Small spatial indices (e.g. `pages2k_proxy_sites_index.csv`), configuration manifests, and documentation (`README.md`, `provenance_and_sources.json`) remain version-controlled to guarantee reproducible data pipelines.
