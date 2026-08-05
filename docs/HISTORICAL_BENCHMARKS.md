# Macro-Historical & Cliodynamic Benchmark Suite Documentation

## Overview

The **Ether Macro-Historical Benchmark Suite** (stored in `src/main/resources/historical_cliodynamic_benchmarks.json`) provides a standardized 20-variable empirical dataset spanning from **10,000 BCE to 2026 CE**. 

This suite serves as the empirical ground truth for model validation via the `HistoricalValidationKernel` and the `HistoricalAutoCalibrationTest`, allowing Ether to compute Root Mean Square Error (RMSE) and Coefficient of Determination ($R^2$) to verify physical and social simulation fidelity.

---

## Data Sources & Academic Citations

### 1. Seshat: Global History Databank
- **Repository / Web**: [https://github.com/datasets/seshat](https://github.com/datasets/seshat) | [https://seshatdatabank.info](https://seshatdatabank.info)
- **Academic Citation**: Turchin, P., Brennan, R., Currie, T. E., Feeney, K. C., François, P., et al. (2015). *Seshat: The Global History Databank*. Cliodynamics: The Journal of Quantitative History and Cultural Evolution, 6(1), 77-107.
- **Variables Calibrated**:
  - `eliteOverproductionIndex`: Normalized ratio of elite aspirants competing for fixed leadership positions.
  - `sociopoliticalInstability`: Frequency and severity of revolts, civil wars, assassinations, and structural state crises.

### 2. Correlates of War (COW) Project
- **Web**: [https://correlatesofwar.org](https://correlatesofwar.org)
- **Academic Citation**: Singer, J. D., & Small, M. (1972 / 2020). *The Wages of War, 1816-1965: A Statistical Handbook*. John Wiley & Sons / Correlates of War Project Data Sets.
- **Variables Calibrated**:
  - `sociopoliticalInstability`: Interstate violence, intra-state war casualty rates, and crisis frequency.

### 3. Maddison Project Database (2020)
- **Web**: [https://www.rug.nl/ggdc/historicaldevelopment/maddison/](https://www.rug.nl/ggdc/historicaldevelopment/maddison/)
- **Academic Citation**: Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*. Groningen Growth and Development Centre, Research Memorandum GD-174.
- **Variables Calibrated**:
  - `grossWorldProduct`: Global economic output in Billion 1990 International Dollars from 1 CE to 2026 CE.

### 4. HYDE 3.2 (History Database of the Global Environment)
- **Web**: [https://kb-pbl.github.io/hyde/](https://kb-pbl.github.io/hyde/)
- **Academic Citation**: Klein Goldewijk, K., Beusen, A., Doelman, J., & Stehfest, E. (2017). *Anthropogenic land use estimates for the Holocene – HYDE 3.2*. Earth System Science Data, 9(2), 927-953.
- **Variables Calibrated**:
  - `worldPopulation`: Global demographic time-series from 10,000 BCE.
  - `deforestationRate`: Percentage of post-glacial forest cover remaining.

### 5. McEvedy & Jones / UN Population & Urbanization Prospects
- **Academic Citation**: McEvedy, C., & Jones, R. (1978). *Atlas of World Population History*. Facts on File / Penguin Books.
- **Variables Calibrated**:
  - `worldPopulation`: Classical and medieval global population baseline.
  - `urbanizationRate`: Share of global population in urban settlements >5,000 inhabitants (supplemented by Bairoch 1988 & Chandler 1987).

### 6. ORBIS: The Stanford Geospatial Network Model of the Roman World
- **Web**: [https://orbis.stanford.edu](https://orbis.stanford.edu)
- **Academic Citation**: Scheidel, W. (2014). *ORBIS: The Stanford Geospatial Network Model of the Roman World*. Stanford University.
- **Variables Calibrated**:
  - `informationSpeed`: Speed of news, orders, and knowledge transmission in km/day across distance.

### 7. Palaeoclimate & Atmospheric Datasets (PAGES 2k, NASA GISS, HadCRUT5, Law Dome / EPICA)
- **Citations**: PAGES 2k Consortium (2019). *Consistent multidecadal variability in global temperature reconstructions*. Nature Geoscience, 12, 643–649.
- **Variables Calibrated**:
  - `temperatureAnomaly`: Global mean surface temperature deviation relative to 1850-1900 pre-industrial baseline (°C).
  - `co2Concentration`: Atmospheric CO₂ concentration in parts per million (ppm).

### 8. Historical Real Wages & Purchasing Power (Robert C. Allen)
- **Academic Citation**: Allen, R. C. (2001). *The Great Divergence in European Wages and Prices from the Middle Ages to the First World War*. Explorations in Economic History, 38(4), 411-447.
- **Variables Calibrated**:
  - `realUnskilledWage`: Purchasing power index of unskilled urban/rural laborers relative to subsistence basket baseline.

### 9. Wealth & Land Inequality (Walter Scheidel & Branko Milanovic)
- **Academic Citation**: Scheidel, W. (2017). *The Great Leveler: Violence and the History of Wealth from the Stone Age to the Twenty-First Century*. Princeton University Press.
- **Variables Calibrated**:
  - `giniInequality`: Wealth and land ownership Gini index (0.0 to 1.0).

### 10. Sovereign Debt Burden (Reinhart & Rogoff & IMF)
- **Academic Citation**: Reinhart, C. M., & Rogoff, K. S. (2009). *This Time Is Different: Eight Centuries of Financial Folly*. Princeton University Press.
- **Variables Calibrated**:
  - `sovereignDebtBurden`: Government debt as a percentage of gross product/state revenue.

### 11. Energy & Ecological Productivity (Vaclav Smil & Montgomery)
- **Academic Citations**:
  - Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
  - Montgomery, D. R. (2007). *Dirt: The Erosion of Civilizations*. University of California Press.
- **Variables Calibrated**:
  - `primaryEnergy`: Primary global energy harvest (Exajoules).
  - `agriculturalEroei`: Caloric output to input net energy return ratio.
  - `soilErosionRate`: Topsoil degradation and salinization percentage loss.

---

## Complete 20-Variable Benchmark Catalogue

| Variable ID | Name | Unit | Primary Source(s) |
| :--- | :--- | :--- | :--- |
| `worldPopulation` | World Population | Millions of people | McEvedy & Jones, HYDE 3.2, UN WPP |
| `grossWorldProduct` | Gross World Product (GWP) | Billion 1990 Int. $ | Maddison Project Database (2020) |
| `primaryEnergy` | Primary Energy Consumption | Exajoules (EJ) | Vaclav Smil (2017), IEA |
| `urbanizationRate` | Urbanization Rate | % > 5k pop | Bairoch (1988), Chandler (1987), UN |
| `co2Concentration` | Atmospheric CO₂ | PPM | Law Dome / EPICA, NOAA Mauna Loa |
| `literacyRate` | Adult Literacy Rate | % | Buringh & van Zanden (2009), UNESCO |
| `currencyDebasement` | Coin Silver Purity | % Silver in Denarius | Butcher & Ponting (2014) |
| `eliteOverproductionIndex` | Elite Overproduction | Index (1.0 = bal) | Peter Turchin (2016), **Seshat Databank** |
| `realUnskilledWage` | Purchasing Power | Index (100 = Sub. Bsk) | Robert C. Allen (2001) |
| `politicalStressIndex` | Political Stress Index (PSI) | Composite Score 0-100 | Turchin & Nefedov (2009) |
| `asabiyyahSocialCohesion` | Asabiyyah Solidarity | Score 0.0 - 1.0 | Ibn Khaldun (1377), Turchin (2003) |
| `sociopoliticalInstability` | Sociopolitical Instability | Events / Dec. Score | **Seshat Databank**, **COW Project** |
| `giniInequality` | Wealth & Land Gini | Gini Index 0.0 - 1.0 | Walter Scheidel (2017), Milanovic |
| `sovereignDebtBurden` | Sovereign Debt Burden | % of GDP / State Rev | Reinhart & Rogoff (2009), IMF |
| `globalTradeVolume` | Global Trade Volume | Index (100 = 1913) | Federico & Tena-Junguito (2017), WTO |
| `temperatureAnomaly` | Temperature Anomaly | °C vs 1850-1900 baseline | PAGES 2k Consortium, NASA GISS |
| `soilErosionRate` | Topsoil Erosion Loss | % cumulative loss | Montgomery (2007), FAO |
| `agriculturalEroei` | Agricultural EROEI | Output/Input Ratio | Vaclav Smil (2008), Giampietro |
| `deforestationRate` | Remaining Forest Cover | % original area | HYDE 3.2, FAO FRA |
| `informationSpeed` | Information Speed | km / day | Scheidel (2014) **ORBIS** |

---

## Model Calibration Integration

The empirical benchmark metrics are automatically loaded at runtime by `HistoricalValidationKernel.java`:

```java
// Loads historical cliodynamic benchmarks suite
HistoricalBenchmarkSuite suite = HistoricalBenchmarkSuite.loadFromResource("/historical_cliodynamic_benchmarks.json");

// Evaluates model trajectory against empirical data points
ValidationResult result = kernel.evaluateTrajectory(simulatedData, suite.getVariable("eliteOverproductionIndex"));
double rmse = result.getRMSE();
double rSquared = result.getRSquared();
```

This guarantees that physical and social simulation models in Ether remain continuously anchored in empirical quantitative history.
