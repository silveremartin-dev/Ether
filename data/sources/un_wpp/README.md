# United Nations World Population Prospects (WPP 2022) Dataset

## 🌐 Overview & Epistemic Scope
This directory contains the authentic demographic repository of the **United Nations Department of Economic and Social Affairs (UN DESA) Population Division**, comprising global, regional, and national demographic series from **1950 to 2100**.

It serves as the definitive reference ground truth for **Tier 2 Macroeconomic & Cliodynamic Evaluation** within Ether for modern and future scenarios:
* **Historical Validation (1950–2021)**: Empirical census and civil registration vital statistics.
* **Predictive Projections (2022–2100)**: Probabilistic Bayesian cohort-component projections under the **Medium Fertility Variant** and alternative variants (High, Low, Instant-Replacement).

---

## 📁 Repository Structure
```
data/sources/un_wpp/
├── WPP2022-CSV-data.zip                     # Complete 3.52 GB official UN archive
├── WPP2022_Demographic_Indicators_Medium.csv# Extracted annual demographic matrix (20.5 MB)
├── WPP2022_Demographic_Indicators_notes.csv # Methodological definitions and country notes
├── un_wpp_world_projections_1950_2100.json  # Curated time-series tensor for Ether simulation
├── provider.json                            # Provider metadata standard
├── provider_manifest.json                   # Archive integrity and file registry
├── provenance_and_sources.json              # Epistemic provenance and peer-review citations
└── README.md                                # This document
```

---

## 📊 Core Trajectory Benchmarks (World Total — Medium Variant)

| Year | World Population (Billions) | Total Fertility Rate (TFR) | Life Expectancy ($e_0$ Years) | Crude Death Rate (CDR / 1000) | Crude Birth Rate (CBR / 1000) |
|:---:|:---:|:---:|:---:|:---:|:---:|
| **1950** | 2.50 | 4.86 | 46.5 | 19.5 | 36.8 |
| **1970** | 3.70 | 4.83 | 58.1 | 12.7 | 33.7 |
| **1990** | 5.33 | 3.31 | 64.3 | 9.3 | 25.5 |
| **2000** | 6.14 | 2.78 | 66.8 | 8.6 | 21.6 |
| **2020** | 7.84 | 2.30 | 72.0 | 7.9 | 17.2 |
| **2026** | 8.24 | 2.29 | 73.8 | 7.7 | 16.4 |
| **2030** | 8.55 | 2.27 | 74.6 | 7.9 | 15.9 |
| **2040** | 9.19 | 2.20 | 76.0 | 8.6 | 15.1 |
| **2050** | 9.71 | 2.15 | 77.2 | 9.4 | 14.0 |
| **2060** | 10.07 | 2.06 | 78.3 | 10.2 | 13.0 |
| **2070** | 10.30 | 1.98 | 79.3 | 10.7 | 12.4 |
| **2080** | **10.41 (Peak)** | 1.92 | 80.3 | 11.2 | 11.8 |
| **2090** | 10.42 | 1.88 | 81.2 | 11.5 | 11.2 |
| **2100** | 10.35 | 1.84 | 82.1 | 11.9 | 10.8 |

---

## 🔬 Integration with Ether Simulation
The extracted matrix allows [`HistoricalValidationKernel`](file:///c:/Silvere/Encours/Developpement/Ether/src/main/java/org/ether/society/analytics/HistoricalValidationKernel.java) to evaluate simulation runs against UN projections without decompressing the massive 3.52 GB zip archive during automated unit tests.

### Citation:
> United Nations, Department of Economic and Social Affairs, Population Division (2022). *World Population Prospects 2022: Summary of Results*. UN DESA/POP/2022/TR/NO. 3.
