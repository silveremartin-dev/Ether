# Data Credits & Sources

This simulation uses real-world datasets from the following sources:

---

## Elevation Data

**ETOPO1 Global Relief Model**

- **Source**: NOAA National Centers for Environmental Information (NCEI)
- **Citation**: Amante, C. and B.W. Eakins, 2009. ETOPO1 1 Arc-Minute Global Relief Model: Procedures, Data Sources and Analysis. NOAA Technical Memorandum NESDIS NGDC-24
- **DOI**: [10.7289/V5C8276M](https://doi.org/10.7289/V5C8276M)
- **Resolution**: 1 arc-minute (~1.8 km)
- **License**: Public Domain

---

## Biome/Ecoregion Data

**WWF Terrestrial Ecoregions**

- **Source**: World Wildlife Fund (WWF)
- **Citation**: Olson, D. M., et al. (2001). Terrestrial Ecoregions of the World: A New Map of Life on Earth. *BioScience*, 51(11), 933-938
- **URL**: <https://www.worldwildlife.org/publications/terrestrial-ecoregions-of-the-world>
- **License**: CC BY 4.0

---

## Mineral Resource Data

**USGS Mineral Resources Data System (MRDS)**

- **Source**: U.S. Geological Survey
- **URL**: <https://mrdata.usgs.gov/mrds/>
- **License**: Public Domain (U.S. Government Work)

---

## Climate Data

**PMIP4 - Paleoclimate Modelling Intercomparison Project**

- **Source**: CMIP6
- **Citation**: Kageyama, M., et al. (2018). The PMIP4 contribution to CMIP6 – Part 1: Overview and over-arching analysis plan. *Geoscientific Model Development*, 11, 1033-1057
- **DOI**: [10.5194/gmd-11-1033-2018](https://doi.org/10.5194/gmd-11-1033-2018)

---

## Historical Population Estimates

**Hyde History Database of the Global Environment**

- **Source**: PBL Netherlands Environmental Assessment Agency
- **Citation**: Klein Goldewijk, K., et al. (2017). Anthropogenic land use estimates for the Holocene – HYDE 3.2. *Earth System Science Data*, 9, 927-953
- **DOI**: [10.5194/essd-9-927-2017](https://doi.org/10.5194/essd-9-927-2017)

---

## Geographic Reference

**Natural Earth**

- **Source**: naturalearthdata.com
- **License**: Public Domain
- **Usage**: Coastlines, boundaries, geographic features

---

## Macro-Historical & Cliodynamic Data Sources

For detailed citations and metric mappings across the 20-variable benchmark suite, see [SIMULATION_EQUATIONS_AND_VARIABLES.md](SIMULATION_EQUATIONS_AND_VARIABLES.md).

### **Seshat: Global History Databank**
- **Source**: Global History Databank / Peter Turchin et al.
- **URL**: <https://github.com/datasets/seshat> | <https://seshatdatabank.info>
- **Citation**: Turchin, P., et al. (2015). *Seshat: The Global History Databank*. Cliodynamics, 6(1), 77-107.
- **Usage**: Elite Overproduction Index (`eliteOverproductionIndex`), Sociopolitical Instability (`sociopoliticalInstability`), Asabiyyah decay parameters.

### **Correlates of War (COW) Project**
- **Source**: University of Michigan
- **URL**: <https://correlatesofwar.org>
- **Citation**: Singer, J. D., & Small, M. (1972 / 2020). *The Wages of War*. Correlates of War Project.
- **Usage**: Interstate warfare, conflict casualty baselines, sociopolitical violence index.

### **Maddison Project Database (2020)**
- **Source**: Groningen Growth and Development Centre (GGDC)
- **URL**: <https://www.rug.nl/ggdc/historicaldevelopment/maddison/>
- **Citation**: Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*.
- **Usage**: Gross World Product (GWP) historical series in 1990 International Dollars.

### **ORBIS Stanford Geospatial Network Model**
- **Source**: Stanford University (Walter Scheidel)
- **URL**: <https://orbis.stanford.edu>
- **Citation**: Scheidel, W. (2014). *ORBIS: The Stanford Geospatial Network Model of the Roman World*.
- **Usage**: Historical information transmission velocity (`informationSpeed`).

---

## Note on Data Processing

All datasets are preprocessed and converted to H3 hexagonal grid format for simulation efficiency. Original data resolutions may be interpolated to match simulation cell sizes. Benchmark JSON files calibrate the `HistoricalValidationKernel`.

