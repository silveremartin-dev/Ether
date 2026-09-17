# Data Sources, Scientific Citations & Software Credits

> **Comprehensive Attribution & Scientific Provenance Catalog**  
> *Ether Simulation Engine — Empirical Datasets, Theoretical Literature, Mathematical Foundations, and Software Libraries*

---

## 1. Global & Planetary Geospatial Datasets

| Dataset | Issuing Institution / Authors | Resolution / Coverage | License | Citation / URL |
|---|---|---|---|---|
| **ETOPO1 Global Relief Model** | NOAA National Centers for Environmental Information (NCEI) | 1 arc-minute (~1.8 km) global relief & bathymetry | Public Domain (U.S. Govt) | Amante & Eakins (2009), NOAA Tech Memo NESDIS NGDC-24. [DOI: 10.7289/V5C8276M](https://doi.org/10.7289/V5C8276M) |
| **GEBCO 2023 Grid** | General Bathymetric Chart of the Oceans / IHO-IOC | 15 arc-second sub-surface bathymetry & elevation | Public Domain / CC BY 4.0 | GEBCO Compilation Group (2023). [https://www.gebco.net](https://www.gebco.net) |
| **WWF Terrestrial Ecoregions** | World Wildlife Fund (WWF) | Global biomes & 867 ecoregions | CC BY 4.0 | Olson, D. M., et al. (2001). *Terrestrial Ecoregions of the World: A New Map of Life on Earth*. BioScience, 51(11), 933-938. |
| **USGS MRDS** | U.S. Geological Survey | Global mineral deposits, ores, and mines | Public Domain (USGS) | Mineral Resources Data System. [https://mrdata.usgs.gov/mrds/](https://mrdata.usgs.gov/mrds/) |
| **PMIP4 / CMIP6 Paleoclimate** | World Climate Research Programme (WCRP) | Mid-Holocene, LGM, LIG climate grids | Open Access / CMIP6 terms | Kageyama, M., et al. (2018). *The PMIP4 contribution to CMIP6*. Geoscientific Model Development, 11, 1033-1057. [DOI: 10.5194/gmd-11-1033-2018](https://doi.org/10.5194/gmd-11-1033-2018) |
| **HYDE 3.2 / 3.4** | Netherlands Environmental Assessment Agency (PBL) | Historical population, cropland, and pasture (10,000 BC–Present) | CC BY 4.0 | Klein Goldewijk, K., et al. (2017). *Anthropogenic land use estimates for the Holocene*. Earth System Science Data, 9, 927-953. [DOI: 10.5194/essd-9-927-2017](https://doi.org/10.5194/essd-9-927-2017) |
| **Natural Earth** | Natural Earth Contributors | Coastlines, boundaries, rivers, physical geography | Public Domain | [https://www.naturalearthdata.com](https://www.naturalearthdata.com) |
| **NASA GIBS** | NASA Earth Science Data and Information System | Global near real-time & historical planetary imagery | Public Domain (NASA) | [https://wiki.earthdata.nasa.gov/display/GIBS](https://wiki.earthdata.nasa.gov/display/GIBS) |
| **NASA PDS Planetary Cartography** | NASA Planetary Data System / USGS Astrogeology | Lunar LRO LOLA DEM, Mars MGS MOLA DEM, Venus Magellan SAR, Mercury MESSENGER MLA DEM | Public Domain (NASA) | [https://pds.nasa.gov](https://pds.nasa.gov) |
| **IAEA PRIS** | International Atomic Energy Agency | Global nuclear power reactor database & operational safety data | Open Data (IAEA) | [https://pris.iaea.org](https://pris.iaea.org) |
| **BGR World Energy Resources** | Federal Institute for Geosciences and Natural Resources (Germany) | Global fossil energy reserves, uranium, and geothermal flux | Open Access (BGR) | BGR (2022). *Energy Study: Reserves, Resources and Availability of Energy Raw Materials*. |
| **PANGAEA** | Data Publisher for Earth & Environmental Science | Marine sediment cores, paleoclimate proxies, ice core $\delta^{18}\text{O}$ and $\text{CO}_2$ | CC BY 3.0 / CC BY 4.0 | [https://www.pangaea.de](https://www.pangaea.de) |
| **WHYMAP Groundwater Data** | UNESCO / BGR | Global groundwater aquifer geometry, thickness & recharge rates | CC BY-NC-SA 3.0 IGO | Struckmeier, W., et al. (2008). *WHYMAP Groundwater Resources of the World*. |

---

## 2. Macro-Historical & Cliodynamic Data Benchmarks

| Project / Archive | Lead Institution / Authors | Coverage | Benchmark Usage in Ether | Citation / URL |
|---|---|---|---|---|
| **Seshat: Global History Databank** | Seshat / Evolution Institute (Peter Turchin et al.) | 400+ historical polities across 10,000 years | Elite Overproduction, Sociopolitical Instability, Asabiyyah decay parameters | Turchin, P., et al. (2015). *Seshat: The Global History Databank*. Cliodynamics, 6(1), 77-107. [https://seshatdatabank.info](https://seshatdatabank.info) |
| **Correlates of War (COW)** | University of Michigan (Singer & Small) | Interstate, intra-state, and non-state wars (1816–Present) | Conflict casualty baselines, sociopolitical violence escalation | Singer, J. D., & Small, M. (1972 / 2020). *The Wages of War*. Correlates of War Project. [https://correlatesofwar.org](https://correlatesofwar.org) |
| **Maddison Project Database (2020)** | Groningen Growth and Development Centre (GGDC) | Historical GDP per capita & Gross World Product (GWP) | Real economic output calibration in 1990 International Geary-Khamis dollars | Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*. [https://www.rug.nl/ggdc/historicaldevelopment/maddison/](https://www.rug.nl/ggdc/historicaldevelopment/maddison/) |
| **ORBIS Geospatial Network** | Stanford University (Walter Scheidel) | Roman transportation network, freight velocity, and travel costs | Historical information transmission velocity & transport friction | Scheidel, W. (2014). *ORBIS: The Stanford Geospatial Network Model of the Roman World*. [https://orbis.stanford.edu](https://orbis.stanford.edu) |
| **World Bank & FAOSTAT** | World Bank & UN Food and Agriculture Organization | Agricultural crop yields, macronutrient production, fertilizer usage | Calibrating van Genuchten soil water, N-P-K harvest depletion & EROEI | [https://data.worldbank.org](https://data.worldbank.org) \| [https://www.fao.org/faostat](https://www.fao.org/faostat) |

---

## 3. Scientific Literature & Mathematical Foundations

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

## 4. Software Dependencies & Open-Source Libraries

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
