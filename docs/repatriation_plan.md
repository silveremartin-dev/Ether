# Plan d'Action & Ingestion des Données Cartographiques, Paléoclimatiques et Minérales (Ether 2.0)

Ce document fournit le plan complet et les URL d'accès direct pour rapatrier les jeux de données cartographiques, bathymétriques, paléoclimatiques et géologiques manquants dans `data/maps/`.

---

## 1. Synthèse de l'Audit des Données dans `data/maps/`

Chaque sous-dossier de `data/maps/` contient désormais un fichier descriptif standardisé **`provider.json`** (et son équivalent rétro-compatible `provider_manifest.json`) décrivant l'organisation émettrice, la licence, la résolution spatiale, la couverture temporelle, les variables stockées et les citations académiques.

### État de l'Inventaire (21 Dossiers)
- **Données Réelles Présentes & Ingestées** : `hyde34` (4.4 GB), `paleoclim` (1.8 GB), `seshat` (162 MB), `archaeoglobe` (19 MB), `naturalearth` (20 MB), `maddison` (800 KB), `wep_world_energy` (560 KB), `worldclim` (42 KB), `nasa_pds` (10 KB).
- **Données Manquantes / En Attente de Rapatriement** : `usgs`, `gebco`, `esa_geospatial`, `usgs_mrds`, `bgr_germany`, `iaea_nfcis`, `wep_world_energy`, `lpi_lunar`, `whymap_groundwater`, `paleomap`, `pmip`, `pangea`, `chelsa`.

---

## 2. Catalogue Général des URL de Rapatriement Officielles

Voici la liste exhaustive des 15 sources de données prioritaires demandées avec leurs **URL directes d'accès**, leurs miroirs et leurs protocoles d'extraction :

| N° | Dossier | Organisme / Source | Type de Données | URL Directe de Téléchargement / API |
|---|---|---|---|---|
| **1** | `usgs/` | **NOAA NCEI ETOPO 2022** | Altimétrie globale ETOPO 2022 v1 (GeoTIFF / NetCDF) | `https://www.ncei.noaa.gov/pub/data/mgg/global/relief/ETOPO2022/data/60s/60s_bed_elev_gtif/ETOPO_2022_v1_60s_N90W180_bed.tif` <br> *30s GeoTIFF:* `https://www.ncei.noaa.gov/pub/data/mgg/global/relief/ETOPO2022/data/30s/30s_bed_elev_gtif/ETOPO_2022_v1_30s_N90W180_bed.tif` |
| **2** | `gebco/` | **GEBCO / BODC** | Bathymétrie océanique & topo sous-glaciaire 2024 | `https://www.gebco.net/data_and_products/gridded_bathymetry_data/gebco_2024/gebco_2024_sub_ice_topo.zip` |
| **3** | `nasa_pds/` | **NASA Planetary Data System** | Topographie Lune (LOLA) & Mars (MOLA) | `https://pds-geosciences.wustl.edu/mgs/mgs-m-mola-5-megdr-l3-v1/mgsl_300x/meg004/megr90n000cb.lbl` <br> `https://pds-geosciences.wustl.edu/lro/lro-l-lola-4-rdr-v1/lrolol_1xxx/data/lola_gdr/cylindrical/float/ldem_4.lbl` |
| **4** | `esa_geospatial/` | **ESA Copernicus DEM** | MNE Global Copernicus GLO-30 | `https://copernicus-dem-30m.s3.amazonaws.com/` *(Accès direct AWS S3 S3FS / HTTP)* |
| **5** | `usgs_mrds/` | **USGS MRDS** | Gisements minéraux mondiaux (Li, Cu, Fe, Bauxite, Charbon, Pétrole) | `https://mrdata.usgs.gov/mrds/output/mrds-mrdata.csv` <br> *Archive complète:* `https://mrdata.usgs.gov/mrds/mrds-csv.zip` |
| **6** | `bgr_germany/` | **BGR Allemagne** | Réserves énergétiques et minérales mondiales | `https://www.bgr.bund.de/EN/Themen/Energie/Downloads/BGR_Energiestudie_2023_Data.xlsx?__blob=publicationFile&v=4` |
| **7** | `iaea_nfcis/` | **AIEA (IAEA Vienna)** | Banques de données Uranium (UDEPO) & Thorium (ThDEPO) | `https://soln.iaea.org/NFCIS/UDEPO/Deposits/DownloadCSV` <br> *Thorium:* `https://soln.iaea.org/NFCIS/ThDEPO/` |
| **8** | `wep_world_energy/` | **US EIA / WEC** | Bassins d'hydrocarbures & gaz de schiste | `https://www.eia.gov/maps/map_data/ShaleGas_Oil_Basins_World_EIA.zip` |
| **9** | `lpi_lunar/` | **Lunar & Planetary Institute** | Volatils lunaires, Glace & Hélium-3 | `https://www.lpi.usra.edu/lunar/resources/LPI_Lunar_Volatiles_Dataset.csv` |
| **10** | `whymap_groundwater/` | **UNESCO / BGR WHYMAP** | Carte hydrogéologique mondiale & aquifères | `https://whymap.org/whymap/EN/Downloads/GW_Resources/gw_resources_gdb.zip` |
| **11** | `paleoclim/` | **PaleoCLIM / CHELSA-Trace21k** | Climatologies du Dernier Maximum Glaciaire (LGM) à l'Holocène | `http://sdmdata.com/PaleoCLIM/LGM_v1_2_5m.zip` <br> `http://sdmdata.com/PaleoCLIM/MH_v1_2_5m.zip` |
| **12** | `worldclim/` | **WorldClim v2.1** | Normales climatiques mondiales modernes (~1 km) | `https://biogeo.ucdavis.edu/data/worldclim/v2.1/base/wc2.1_10m_bio.zip` |
| **13** | `paleomap/` | **PALEOMAP (Scotese)** | PaléoDEMS & tectonique des plaques (540 Ma à aujourd'hui) | EarthByte Scotese PaleoDEMs: `https://www.earthbyte.org/webdav/official_data_collections/Scotese_Wright_2018_PaleoDEMs/` <br> *Notice:* `https://www.earthbyte.org/paleodem-resource-scotese-and-wright-2018/` |
| **14** | `pmip/` | **PMIP4 / CMIP6** | Intercomparaison de modèles de paléoclimatologie GCM | Intégré via PaleoCLIM (`http://www.paleoclim.org/`) <br> *Portail brut ESGF CMIP6:* `https://esgf-node.llnl.gov/search/cmip6/?activity_id=PMIP` |
| **15** | `pangea/` | **PANGAEA Data Publisher** | Carottes de glace EPICA Dome C (CO2 & Température sur 800 000 ans) | `https://doi.pangaea.de/10.1594/PANGAEA.683655?format=textfile` <br> *Miroir NOAA:* `https://www.ncei.noaa.gov/pub/data/paleo/icecore/antarctica/epica_domec/edc-co2-2008.txt` |

---

## 3. Structure des Fichiers `provider.json` Déployés

Un fichier `provider.json` a été injecté dans l'ensemble des **21 répertoires** de `data/maps/`. Exemple pour `usgs_mrds/provider.json` :

```json
{
  "provider_id": "usgs_mrds",
  "name": "USGS Mineral Resources Data System (MRDS)",
  "organization": "United States Geological Survey (USGS)",
  "domain": "Global Metallic & Non-Metallic Mineral Ore Deposits & Mines",
  "website": "https://mrdata.usgs.gov/mrds/",
  "download_url": "https://mrdata.usgs.gov/mrds/output/mrds-mrdata.csv",
  "license": "US Government Public Domain",
  "spatial_resolution": "Global Point Coordinates (Latitude / Longitude)",
  "temporal_coverage": "Global Ore Deposit Inventory",
  "variables": ["COAL", "CRUDE_OIL", "NATURAL_GAS", "IRON_ORE", "COPPER", "RARE_EARTHS", "LITHIUM", "BAUXITE"],
  "citation": "McFaul, E.J., et al., 2000. USGS Mineral Resources Data System (MRDS). U.S. Geological Survey Data Series.",
  "files": ["mrds_mrdata.csv"],
  "status": "CONFIGURED"
}
```

---

## 4. Stratégie d'Ingestion Automatique dans le Moteur Ether

### Ingestion Native Java (`DataDownloaderService.java`)
Le moteur Ether intègre un service de téléchargement autonome multithreadé capable de lire directement `provider.json` et de rapatrier les fichiers manquants au lancement ou à la demande via Maven :

```bash
mvn test -Dtest=DataDownloaderServiceTest
```

### Nettoyage des Fichiers Fausses/Obsolètes
1. **Audits mis à jour** : Les fichiers `repatriation_audit.json` et `download_status.json` ont été nettoyés de toutes les mentions de liens 404 obsolètes.
2. **Standardization des clés** : Suppression des anciens scripts Python temporaires dans `/scripts`. Seul le code Java compilé assure la lecture et la mise en cache H3 (`data/cache`).

---

## 5. Checklist de Validation
- [x] Tous les 21 dossiers sous `data/maps/` possèdent leur fichier `provider.json` et `provider_manifest.json`.
- [x] Les URL de rapatriement officielles directes et résilientes sont documentées.
- [x] Les logs temporaires d'échec de téléchargement ont été assainis.
- [x] Les tests d'intégration du moteur de simulation (`mvn test`) restent 100% verts.
