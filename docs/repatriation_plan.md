# Plan d'Action, Cartographie Planétaire & Ingestion des Données (Ether 2.0)

Ce document résume l'infrastructure complète d'ingestion cartographique, bathymétrique, paléoclimatique, minérale et planétaire (Terre, Lune, Mars, Vénus, Mercure) pour le moteur de simulation **Ether 2.0**.

---

## 1. Synthèse de l'Audit des Données dans `data/maps/`

Chaque sous-dossier de `data/maps/` contient son fichier de métadonnées standardisé **`provider.json`** et **`provider_manifest.json`** décrivant l'organisation émettrice, la licence, la résolution spatiale, la couverture temporelle, les variables stockées et les citations académiques.

### État de l'Inventaire des Données (18 Répertoires Actifs Qualifiés)

- **🟢 Terre (Altimétrie, Bathymétrie & Hydrologie)** :
  - `usgs/` : NOAA NCEI ETOPO 2022 v1 60s/30s (`bed.tif`, `surface.tif`, `geoid.tif`) - Topographie & Bathymétrie globale.
  - `gebco/` : GEBCO 2024 Sub-ice Topography & Ocean Depth.
  - `whymap_groundwater/` : Base SIG UNESCO / BGR des ressources en eaux souterraines et grands aquifères du monde (`WHYMAP_GWR_v1.zip`).

- **🟢 Terres & Gisements Énergétiques / Minéraux** :
  - `usgs_mrds/` : USGS Mineral Resources Data System (`mrds-csv.zip`) - 300 000+ gisements de Fer, Cuivre, Lithium, Or, Bauxite, Charbon, Pétrole.
  - `bgr_germany/` : BGR Germany Global Energy Study 2023 (`energiedaten_2023_en.xlsx`) - Réserves fossiles, minérales et géothermie.
  - `iaea_nfcis/` : AIEA UDEPO & ThDEPO (`DepositDataList.xlsx`) - Gisements mondiaux d'Uranium et de Thorium.
  - `wep_world_energy/` : World Energy Council & EIA (`global_gdp_energy_proxies.csv`) - Bassins d'hydrocarbures.

- **🟢 Paléoclimats, Normales Climatiques & Tectonique** :
  - `paleoclim/` : PaleoCLIM / CHELSA-Trace21k (Grilles LGM, Holocène, Pliocène, Eemien).
  - `worldclim/` : WorldClim v2.1 (Normales 1970-2000 à ~1 km : bioclimat, températures, précipitations).
  - `paleomap/` : PALEOMAP Project Scotese PaleoDEMs (88 cartes d'élévation Phanérozoïque -540 Ma à aujourd'hui).
  - `pangea/` : PANGAEA Carottes de glace EPICA Dome C (`EDC99_CO2_bern.tab`, `EDC_dD_temp_estim.tab` - $CO_2$ & Température sur 800k ans).

- **🟢 Démographie, Histoire & Sociétés** :
  - `hyde34/` : HYDE 3.4 Baseline Grid (122 archives d'occupation des sols et population de -10 000 BC à 2024 AD).
  - `seshat/` : Seshat Global History Databank (Polities, structures institutionnelles, capitales).
  - `archaeoglobe/` : ArchaeoGLOBE Project (Occupation archéologique des terres).
  - `naturalearth/` : Natural Earth 10m/110m (Vecteurs côtiers, rivières, lacs, frontières).
  - `maddison/` : Maddison Project Database 2023 (Séries historiques de PIB/habitant et population).

- **🟢 Planétologie Comparative (Lune, Mars, Vénus, Mercure)** :
  - `lpi_lunar/` : LPI & NASA PDS LEND (`lro_lend.zip`) - Cartes d'eau polaire, d'hydrogène et d'Hélium-3 du régolithe lunaire.
  - `nasa_pds/` : NASA Planetary Data System - Modèles altimétriques de la Lune (LRO LOLA `ldem_4.lbl`), Mars (MOLA `megr90n000cb.lbl`), Vénus (Magellan Altimetry SAR) et Mercure (MESSENGER MLA).

---

## 2. Consolidation & Nettoyage des Dossiers Redondants

- **`pmip/` & `chelsa/`** : Intégrés nativement via le sous-système **`paleoclim/`** et **`worldclim/`**. Les dossiers répertoires vides ont été supprimés.
- **`esa_geospatial/`** : Le MNE Copernicus GLO-30 étant redondant avec **NOAA ETOPO 2022 v1**, les données d'élévation terrestre et bathymétrique sont centralisées dans **`usgs/`**.

---

## 3. Checklist de Qualification du Code & Tests

- [x] Tous les dossiers de `data/maps/` possèdent des métadonnées valides `provider.json`.
- [x] Les fichiers binaires et archives lourdes (`.tif`, `.tab`, `.xlsx`, `.xls`, `.zip`) sont ignorés dans `.gitignore` pour préserver la légèreté des commits Git tout en étant opérationnels localement.
- [x] Le projet compile à 100% avec `mvn test-compile` (`BUILD SUCCESS`).
