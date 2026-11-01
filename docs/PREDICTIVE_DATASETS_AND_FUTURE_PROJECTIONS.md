# Predictive Benchmark Datasets & Multi-Variable Future Projections (2025–2100+)

## 1. Context & Epistemic Rationale
Historical empirical databases (such as Maddison 2020, HYDE 3.4, Law Dome ice cores, and ECMWF ERA5) provide rigorous observational data up to the contemporary frontier (**2025 CE**). To evaluate post-2025 trajectories and future planetary boundary scenarios without introducing arbitrary synthetic curves or speculative shortcuts, Ether incorporates standardized multi-variable projections from **authoritative international integrated assessment models (IAMs)**, **climate model intercomparisons (CMIP6)**, and **demographic consensus agencies**:

1. **KNMI Climate Explorer / CMIP6 Multi-Model Ensemble**: Global Mean Surface Temperature (GMST) projections across SSP1-2.6, SSP2-4.5, SSP3-7.0, and SSP5-8.5 anchored to the peer-reviewed pre-industrial baseline ($1850\text{–}1900$ at $+13.70^\circ\text{C}$).
2. **United Nations Department of Economic and Social Affairs (UN DESA)** — *World Population Prospects 2024 Revision* (Medium, Low, High probabilistic variants).
3. **Integrated Assessment Modeling Consortium (IAMC) / IIASA Shared Socioeconomic Pathways (SSPs 1 to 5)**: Standardized energy, economic output, carbon emissions, and land-use trajectories from MESSAGEix-GLOBIOM, IMAGE 3.0, REMIND-MAgPIE, AIM/CGE, and GCAM4.
4. **Club of Rome / MIT System Dynamics Group** — *Limits to Growth World3 Standard Run & Herrington (2021) Empirical Recalibration*.
5. **Ether Biophysical Calibrated Projection (2025–2200 CE)**: Fully coupled biophysical cliodynamic projection extending beyond 2100 to evaluate long-term thermodynamic carrying capacities and resource depletion.

---

## 2. Integrated Sources & Detailed Methodology

### A. KNMI Climate Explorer / CMIP6 Multimodel Ensemble (`cmip6_knmi_temperatures_1d.json`)
* **Source Organization**: Royal Netherlands Meteorological Institute (KNMI) & World Climate Research Programme (WCRP) CMIP6.
* **Data Access**: [KNMI Climate Explorer CMIP6 Scenario Portal](https://climexp.knmi.nl/)
* **Pre-Industrial Temperature Baseline**:
  * Historical pre-industrial reference period: **1850–1900 mean GMST $\equiv +13.70^\circ\text{C}$** ($286.85\text{ K}$), derived from the Berkeley Earth / HadCRUT5 / ERA5 continuous climatology.
  * Anomalies $\Delta T(t)$ in CMIP6 models are added directly to this $+13.70^\circ\text{C}$ baseline to yield absolute global mean temperatures.
* **Scenarios Covered (1850–2100)**:
  * `SSP1-2.6`: Sustainable transition, radiative forcing stabilizes near $2.6\text{ W/m}^2$, temperature peak at $\approx +1.8^\circ\text{C}$ above pre-industrial before slow cooling.
  * `SSP2-4.5`: Intermediate / Middle of the Road scenario, stabilization near $4.5\text{ W/m}^2$, GMST reaches $\approx +2.7^\circ\text{C}$ by 2100.
  * `SSP3-7.0`: High-forcing regional rivalry and fragmentation, forcing reaches $7.0\text{ W/m}^2$, GMST reaches $\approx +3.6^\circ\text{C}$ by 2100.
  * `SSP5-8.5`: Extreme fossil-fueled development, forcing reaches $8.5\text{ W/m}^2$, GMST exceeds $+4.4^\circ\text{C}$ by 2100.

### B. United Nations World Population Prospects (WPP 2024 Revision) (`un_wpp_2024_world_projections_1d.json`)
* **Agency**: UN DESA Population Division (July 2024 Release).
* **Official Data Portal**: [https://population.un.org/wpp/](https://population.un.org/wpp/)
* **Methodology**: Probabilistic cohort-component projection synthesizing 237 national population series, Bayesian total fertility rate (TFR) distributions, and Gompertz-Makeham survival projections.
* **Variants Ingested**:
  * **Medium Variant (Consensus Baseline)**: Global population reaches $8.16\text{ B}$ (2025), $8.52\text{ B}$ (2030), $9.70\text{ B}$ (2050), peaks at $\approx 10.29\text{ B}$ in 2084, and declines gently to $10.18\text{ B}$ by 2100.
  * **Low Variant**: Rapid fertility decline, global peak at $\approx 8.8\text{ B}$ around 2050, dropping to $7.3\text{ B}$ by 2100.
  * **High Variant**: Persistent high fertility, reaches $12.3\text{ B}$ by 2100.

### C. IAMC Shared Socioeconomic Pathways 1 to 5 (`ssp1_iamc_ingested_1d.json` to `ssp5_iamc_ingested_1d.json`)
* **Consortium**: IIASA Energy, Climate, and Environment (ECE) Program & Integrated Assessment Modeling Consortium (IAMC).
* **Official Database**: [https://tntcat.iiasa.ac.at/SspDb/](https://tntcat.iiasa.ac.at/SspDb/)
* **Five Canonical Pathways**:
  * **SSP1 ("Sustainability")**: Low challenges to mitigation and adaptation, green technological transition, high resource efficiency, early population peak.
  * **SSP2 ("Middle of the Road")**: Historical trends of technological and social development continue, moderate economic and demographic expansion.
  * **SSP3 ("Regional Rivalry")**: Resurgent nationalism, security-focused regional blocks, low international cooperation, high vulnerability to environmental shocks.
  * **SSP4 ("Inequality")**: High social stratification, knowledge and wealth concentrated in high-tech enclaves, vulnerable subsistence masses.
  * **SSP5 ("Fossil-Fueled Development")**: High energy consumption, rapid GDP growth driven by fossil exploitation, high faith in technological fixes.

### D. Ether Biophysical Calibrated Projection (`ether_calibrated_projection_1d.json`)
* **Domain Window**: **2025 CE to 2200 CE**.
* **Methodology**: Evaluates coupled physical feedback equations (Hotelling fossil depletion, 2D Darcy aquifer pumping limits, van Genuchten soil water retention, Liebig soil N-P-K nutrient exhaustion, Stull wet-bulb lethality) forward into the 22nd century.
* **Purpose**: Serves as the continuous high-temporal-resolution benchmark for post-2100 planetary simulations where standard IAMs terminate.

---

## 3. Storage Directory & File Hierarchy

Following the **Planetary Encapsulation Architecture** (`AGENTS.md` Section 6), all predictive benchmark series are stored within Earth's planetary data tree:

```
data/ether/earth/variables/projections/
├── predictive_socioeconomic_projections_2026_2100.json # Multi-variable aggregated benchmark
├── cmip6_knmi_temperatures_1d.json                     # CMIP6 GMST time series (+13.70°C pre-ind baseline)
├── un_wpp_2024_world_projections_1d.json               # UN WPP 2024 demographic series (Medium, Low, High)
├── ssp1_iamc_ingested_1d.json                          # Ingested IIASA SSP1 pathway
├── ssp2_iamc_ingested_1d.json                          # Ingested IIASA SSP2 pathway
├── ssp3_iamc_ingested_1d.json                          # Ingested IIASA SSP3 pathway
├── ssp4_iamc_ingested_1d.json                          # Ingested IIASA SSP4 pathway
├── ssp5_iamc_ingested_1d.json                          # Ingested IIASA SSP5 pathway
└── ether_calibrated_projection_1d.json                 # Ether internal simulation run (2025–2200 CE)
```

> **Legacy Fallback**: Secondary copies are maintained in `data/variables/projections/` and packaged in `src/main/resources/data/benchmarks/` to ensure 100% offline standalone capability.

---

## 4. Mathematical Interpolation & Transition Continuity

### A. The 2025 Boundary Invariant
Historical observational series (`hyde_maddison_historical_1d.json`) terminate in **2025 CE**, where they seamlessly join the user's selected prospective pathway (`SSP1` through `SSP5` or `ETHER_CALIBRATED`).

### B. Shape-Preserving PCHIP Interpolation
To avoid Runge's phenomenon (wild polynomial oscillations between sparse 10-year or 5-year IAM projection reporting timesteps), Ether implements the **Fritsch & Carlson (1980) Monotone Piecewise Cubic Hermite Interpolating Polynomial (PCHIP)**:
$$\left.\frac{df}{dt}\right|_{t_k} = 0 \quad \text{whenever} \quad \Delta_{k-1} \Delta_k \le 0$$
This guarantees that prospective variables (such as population $N(t)$, GDP $Y(t)$, and primary energy $E(t)$) remain strictly monotonic between inflection points and never overshoot into unphysical or negative values.
