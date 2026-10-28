# Predictive Benchmark Datasets & Multi-Variable Future Projections (2026–2100)

## 1. Context & Epistemic Rationale
Historical empirical databases (such as Maddison 2020, HYDE 3.4, Law Dome ice cores, and HadCRUT5) terminate at the current historical frontier (**2026 CE**). To evaluate post-2026 trajectories and future exoplanetary or planetary boundary scenarios without introducing arbitrary synthetic curves, Ether incorporates multi-variable projections from **authoritative international integrated assessment models (IAMs)** and **demographic consensus agencies**:

1. **United Nations Department of Economic and Social Affairs (UN DESA)** — *World Population Prospects 2024 Revision*
2. **Intergovernmental Panel on Climate Change (IPCC AR6) / International Institute for Applied Systems Analysis (IIASA)** — *Shared Socioeconomic Pathways (SSP2: "Middle of the Road")*
3. **Club of Rome / MIT System Dynamics Group** — *Limits to Growth World3 Standard Run & Herrington (2021) Empirical Recalibration*

---

## 2. Integrated Sources & Access Protocols

### A. United Nations World Population Prospects (WPP 2024)
* **Agency**: UN DESA Population Division
* **Official Data Portal**: [https://population.un.org/wpp/](https://population.un.org/wpp/)
* **Direct Download**: [UN WPP 2024 CSV & Data Download Center](https://population.un.org/wpp/Download/Standard/MostUsed/)
* **Methodology**: Probabilistic demographic projection combining Bayesian hierarchical models for total fertility rate (TFR) and life expectancy $e_0$ with cohort-component integration across 237 countries and territories.
* **Key Milestone Trajectory (Median Variant)**:
  * **2026**: $8\,160\text{ M}$
  * **2030**: $8\,520\text{ M}$
  * **2050**: $9\,700\text{ M}$
  * **2084 (Global Peak)**: $10\,290\text{ M}$ ($10.29\text{ billion}$)
  * **2100**: $10\,180\text{ M}$ (gentle contraction)

### B. IPCC AR6 / IIASA Shared Socioeconomic Pathways (SSP2)
* **Consortium**: IIASA Energy, Climate, and Environment (ECE) Program & Integrated Assessment Modeling Consortium (IAMC)
* **Official Database Explorer**: [https://tntcat.iiasa.ac.at/SspDb/](https://tntcat.iiasa.ac.at/SspDb/)
* **Primary Integrated Models**:
  * **MESSAGEix-GLOBIOM** (IIASA)
  * **IMAGE 3.0** (PBL Netherlands Environmental Assessment Agency)
  * **MAGICC7** (Reduced-complexity carbon cycle and climate model)
* **Direct API / Query Instructions**:
  Users can query the IIASA SSP Public Database by filtering:
  * Scenario: `SSP2-Baseline` or `SSP2-45`
  * Model: `MESSAGEix-GLOBIOM 1.0` or `IMAGE 3.0.1`
  * Variables: `Primary Energy`, `GDP|PPP`, `Emissions|CO2`, `Concentration|CO2`, `Temperature|Global Mean`
* **Coupled Projections Matrix (SSP2 / SSP2-4.5)**:

| Year | Population (M) | Gross World Product (B$ 1990) | Primary Energy (EJ/yr) | Atmospheric $\text{CO}_2$ (ppm) | Temp Anomaly ($^\circ\text{C}$) | Urbanization (%) |
|---|---|---|---|---|---|---|
| **2026** | 8,160 | 105,000 | 620 | 424 | +1.30 | 57.0% |
| **2030** | 8,520 | 125,000 | 670 | 435 | +1.50 | 60.4% |
| **2040** | 9,180 | 158,000 | 745 | 458 | +1.80 | 64.8% |
| **2050** | 9,700 | 195,000 | 820 | 480 | +2.10 | 68.6% |
| **2060** | 10,020 | 235,000 | 890 | 500 | +2.30 | 72.0% |
| **2080** | 10,290 | 315,000 | 990 | 528 | +2.58 | 77.0% |
| **2100** | 10,180 | 380,000 | 1,050 | 540 | +2.70 | 80.0% |

### C. Club of Rome World3 Model (BAU2 Empirical Calibration)
* **Foundational Reference**: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). *The Limits to Growth*.
* **Modern Empirical Evaluation**: Herrington, G. (2021). "Update to limits to growth: Comparing the World3 model with empirical data". *Journal of Industrial Ecology*, 25(3), 614-626. DOI: [10.1111/jiec.13084](https://doi.org/10.1111/jiec.13084).
* **System Dynamics Behavior**:
  * **2020–2035**: Continued growth in industrial output per capita, while non-renewable resource capital depletion escalates.
  * **2035–2045**: Peak in industrial output and food per capita due to rising capital allocation required for resource extraction.
  * **2045–2050**: Peak global population (~9.5 billion), followed by a demographic contraction under unmitigated pollution and resource exhaustion.

---

## 3. Storage Directory & File Hierarchy

1. **Production Runtime Resource**:
   `src/main/resources/data/benchmarks/predictive_socioeconomic_projections_2026_2100.json`
2. **Engine Repository Copy**:
   `data/benchmarks/predictive_socioeconomic_projections_2026_2100.json`
3. **Unified 27-Variable Core Benchmark**:
   `src/main/resources/historical_cliodynamic_benchmarks.json` (smoothly continuous from -100,000 BCE to 2100 CE).
