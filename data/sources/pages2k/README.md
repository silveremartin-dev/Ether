# PAGES 2k Consortium Paleoclimate Temperature Synthesis

## 1. Overview & Dataset Identity
This directory contains the standardized **PAGES 2k Consortium Common Era Surface Temperature Dataset** (Phase 2, version 2.2.0) and the global multi-method ensemble reconstruction from Neukom et al. (2019).

* **Temporal Scope**: Common Era (CE), from year **1 CE to 2000 CE** (annual discrete resolution $\Delta t = 1.0\text{ yr}$).
* **Spatial Scope**: Global multiplex covering **647 temperature-sensitive paleoclimate proxy records** across 8 continental and oceanic domains.
* **Physical Observable**: Near-surface temperature anomaly $\Delta T(t)$ in degrees Celsius ($^\circ\text{C}$), standardized relative to the **1961–1990 CE** climatological baseline period.

---

## 2. Directory Contents

| File | Format | Size | Description |
| :--- | :---: | :---: | :--- |
| `Pages2kTemperature2_2_0.zip` | ZIP (LiPD) | 6.55 MB | Master archive of 647 individual site-level `.lpd` records containing linked JSON-LD metadata and raw measurement time-series. |
| `pages2k_proxy_sites_index.csv` | CSV | 78 KB | Consolidated spatial index of all 647 proxy sites: coordinates (`latitude`, `longitude`, `elevation_m`), region, archive type, dataset ID, and site name. |
| `pages2k_global_temperature_reconstruction_1_2000ce.tsv` | TSV | 115 KB | Annual 2,000-year global mean surface temperature (GMST) series reporting the 50th percentile (median) and 95% Bayesian credible range ($[2.5\%, 97.5\%]$). |
| `provenance_and_sources.json` | JSON | 3.5 KB | Formal machine-readable provenance metadata, citation DOIs, and validation attributes adhering to Ether's data contract. |

---

## 3. Physical & Methodological Foundations

### 3.1. Proxy Archive Breakdown
The 647 empirical records encompass 7 distinct geophysical and biological proxy archives:
1. **Dendroclimatology (Tree Rings)**: Annual tree-ring width (TRW) and maximum latewood density (MXD), primarily capturing warm-season continental temperatures.
2. **Glaciology (Ice Cores)**: Stable isotopic ratios ($\delta^{18}\text{O}$, $\delta\text{D}$) and melt layers from polar ice sheets (Greenland, Antarctica) and high-altitude tropical glaciers.
3. **Marine Sclerochronology (Corals)**: Annual and sub-annual $\text{Sr}/\text{Ca}$ elemental ratios and $\delta^{18}\text{O}$ reflecting tropical sea surface temperatures (SST).
4. **Limnogeology (Lake Sediments)**: Varve thickness, biogenic silica, and chironomid assemblages.
5. **Marine Sediments**: Alkenone unsaturation indices ($U^K_{37}$) and foraminiferal $\text{Mg}/\text{Ca}$ ratios.
6. **Speleothems (Cave Formations)**: Calcite $\delta^{18}\text{O}$ time-series dated via high-precision $^{230}\text{Th}/U$ mass spectrometry.
7. **Borehole Geothermics**: Deep subsurface temperature-depth profiles recording low-frequency secular warming trends.

### 3.2. Statistical Ensemble & Reconstruction Methods
The global temperature series (`pages2k_global_temperature_reconstruction_1_2000ce.tsv`) represents a 7,000-member reconstruction ensemble computed across six distinct statistical reconstruction frameworks:
1. **BHM**: Bayesian Hierarchical Modeling
2. **CPS**: Composite Plus Scale
3. **DA**: Data Assimilation (paleoclimate data assimilation with GCM priors)
4. **M08**: Regularized Expectation Maximization (Mann et al., 2008)
5. **OIE**: Optimal Information Extraction
6. **PCR**: Principal Component Regression

Each year $t \in [1, 2000]$ includes the median anomaly $\Delta T_{\text{median}}(t)$ alongside the 2.5th and 97.5th percentiles:
$$\Delta T_{95\%}(t) = \big[ \Delta T_{2.5\%}(t), \, \Delta T_{97.5\%}(t) \big]$$

---

## 4. Operational Integration in Ether

In Ether's architectural framework, PAGES 2k serves two Tier-1 and Tier-2 functions:

### 4.1. Tier-1 Regional Paleoclimate Forcing
Rather than using synthetic or stationary climatic baselines during Common Era historical runs, Ether's energy balance model interpolates the nearest spatial proxy anomalies $\Delta T_{\text{proxy}}(x, t)$ onto the discrete hexagonal grid (Uber H3), capturing authentic historical climate fluctuations:
* **Roman Warm Period** (c. $1\text{ to }250\text{ CE}$): Relative thermal stability in the Mediterranean basin.
* **Late Antique Little Ice Age (LALIA)** ($536\text{ to }660\text{ CE}$): Multi-decadal cooling triggered by low-latitude volcanic aerosol veils (536, 540, 547 CE).
* **Medieval Climate Anomaly (MCA)** (c. $950\text{ to }1250\text{ CE}$): Regional warming anomalies across the North Atlantic and Eurasian wheat belts.
* **Little Ice Age (LIA)** (c. $1300\text{ to }1850\text{ CE}$): Multi-century cooling episodes driving agricultural growing season contraction in Northern Eurasia and China.

### 4.2. Scientific Epistemic Falsification (Exogenous Shock vs. Endogenous Decay)
PAGES 2k acts as an empirical ground truth for resolving competing cliodynamic hypotheses:
$$\text{Agricultural Yield}_i(t) = f\Big( \text{Tech}_i(t), \, \boldsymbol{\Phi}_{\text{soil}, i}(t), \, T_{\text{baseline}, i} + \Delta T_{\text{PAGES2k}, i}(t) \Big)$$
By coupling real reconstructed temperature anomalies into the simulation, researchers can test whether imperial crises (e.g., the Crisis of the Third Century in Rome, the Ming-Qing transition of 1644) were triggered by exogenous climatic crop failures or by endogenous structural-demographic crises (Turchin's Political Stress Index $\Psi_{\text{PSI}}$).

---

## 5. Primary References & Citations

1. **PAGES 2k Consortium** (2017). *A global multiproxy database for temperature reconstructions of the Common Era*. **Scientific Data**, 4, 170088. [doi:10.1038/sdata.2017.88](https://doi.org/10.1038/sdata.2017.88)
2. **Neukom, R., Steiger, N., Gómez-Navarro, J. J., Wang, J., & Werner, J. P.** (2019). *Consistent multidecadal variability in global temperature reconstructions and simulations over the Common Era*. **Nature Geoscience**, 12(8), 643–649. [doi:10.1038/s41561-019-0400-0](https://doi.org/10.1038/s41561-019-0400-0)
