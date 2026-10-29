# Predictive Socioeconomic & Environmental Projections (2026–2100)

## 📌 Overview & Scientific Purpose
This directory contains standardized, multi-model predictive socioeconomic, demographic, climatic, and resource trajectories across the $2026 \to 2100$ horizon.
In the **Ether Cliodynamic Epistemic Falsification Architecture**, these datasets serve as competing external empirical baselines against which post-2025 simulated trajectories can be quantitatively benchmarked, scored, and cross-evaluated.

---

## 🏛️ Competing Predictive Paradigm Suites

| Model Suite Identifier | Descriptive Title | Institutional Origin | Theoretical Paradigm |
|---|---|---|---|
| **`PREDICTIVE_SSP1`** | Sustainability & Rapid Decarbonization | IIASA / IPCC AR6 WGIII / IEA Net Zero 2050 | Low challenges to mitigation and adaptation, green technological shift, rapid exergy efficiency, demographic peak then stabilization. |
| **`PREDICTIVE_SSP2`** | Middle-of-the-Road Trend Continuity | UN WPP 2024 / IIASA SSP2 / IEA STEPS | Historical trend continuity, incremental energy transition, demographic plateauing near 10.3 billion in 2084. |
| **`PREDICTIVE_SSP3_WORLD3`** | Regional Rivalry & Resource Scarcity | Club of Rome World3 BAU2 / Herrington (2021) / IPCC SSP3 | Declining fossil fuel EROI, supply chain friction, mineral resource degradation, agricultural strain, and industrial contraction post-2045. |
| **`PREDICTIVE_SSP4`** | Inequality - A Road Divided | IIASA / PNNL GCAM4 / IPCC AR6 WGIII | High international and intra-national stratification: technological elite coexisting with large lower-income populace, unequal education and slower fertility drop. |
| **`PREDICTIVE_SSP5`** | Fossil-Fueled High Growth | IIASA SSP5-8.5 / REMIND-MAGPIE | Unconstrained fossil resource extraction, heavy industrial expansion, rapid technological innovation, severe climate warming (>4.0°C by 2100). |

---

## 📚 Peer-Reviewed Primary Citations & Data Sources

1. **United Nations World Population Prospects (2024 Revision)**:
   * **Agency**: United Nations Department of Economic and Social Affairs (UN DESA), Population Division.
   * **URL**: https://population.un.org/wpp/
   * **Variables**: `worldPopulation`, `urbanizationRate`.

2. **Shared Socioeconomic Pathways (SSPs)**:
   * **Citation**: Riahi, K., van Vuuren, D. P., Kriegler, E., Edmonds, J., O’Neill, B. C., et al. (2017). *The Shared Socioeconomic Pathways and their energy, land use, and greenhouse gas emissions implications: An overview*. **Global Environmental Change**, 42, 153–168. DOI: [10.1016/j.gloenvcha.2016.05.009](https://doi.org/10.1016/j.gloenvcha.2016.05.009).
   * **Host**: IIASA Energy, Climate, and Environment (ECE) Program. SSP Database v3.0 (https://tntcat.iiasa.ac.at/SspDb/).
   * **Variables**: `grossWorldProduct` (Billion 1990 USD), `primaryEnergy` (EJ), `co2Concentration` (ppm), `temperatureAnomaly` (°C).

3. **Club of Rome World3 Model Recalibration (BAU2)**:
   * **Citation**: Herrington, G. (2021). *Update to limits to growth: Comparing the World3 model with empirical data*. **Journal of Industrial Ecology**, 25(3), 614–626. DOI: [10.1111/jiec.13084](https://doi.org/10.1111/jiec.13084).
   * **Variables**: `worldPopulation`, `grossWorldProduct`, `primaryEnergy`.

4. **International Energy Agency (IEA) World Energy Outlook 2023/2024**:
   * **Agency**: IEA, Paris. https://www.iea.org/reports/world-energy-outlook-2023
   * **Scenarios**: Net Zero by 2050 (NZE), Stated Policies Scenario (STEPS).
   * **Variables**: `fossilSharePrimaryEnergy`, `primaryEnergy`.
