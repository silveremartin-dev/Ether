# Ether Simulation — Historical Benchmarks & Empirical Validation Suite

> **Master Academic & Technical Specification**: 20-Variable Historical Benchmark Catalogue, Empirical Data Repositories (Seshat, COW, Maddison, HYDE 3.2, ORBIS, PAGES 2k), Statistical Goodness-of-Fit Equations (RMSE & $R^2$), and Historical Calibration Test Suite.

---

## 1. Overview & Academic Data Sources

The **Ether Macro-Historical Benchmark Suite** (`historical_cliodynamic_benchmarks.json`) provides a standardized empirical ground-truth dataset spanning **10,000 BCE to 2026 CE**.

This suite enables rigorous academic validation of Ether's coupled physical and cliodynamic trajectories via `HistoricalValidationKernel` and `HistoricalAutoCalibrationTest`. Model performance is evaluated by computing **Root Mean Square Error (RMSE)** and the **Coefficient of Determination ($R^2$)** across 20 empirical target variables.

### Key Data Repositories & Citations

1. **Seshat: Global History Databank**: Turchin, P., et al. (2015). *Seshat: The Global History Databank*. Cliodynamics, 6(1), 77-107. (Calibrates `eliteOverproductionIndex` and `sociopoliticalInstability`).
2. **Correlates of War (COW) Project**: Singer, J. D., & Small, M. (1972 / 2020). *The Wages of War*. (Calibrates interstate warfare casualties and conflict frequency).
3. **Maddison Project Database (2020)**: Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*. (Calibrates `grossWorldProduct` from 1 CE to 2026 CE in 1990 International Dollars).
4. **HYDE 3.2 (History Database of the Global Environment)**: Klein Goldewijk, K., et al. (2017). *Anthropogenic land use estimates for the Holocene – HYDE 3.2*. ESSD, 9, 927-953. (Calibrates `worldPopulation` and `deforestationRate`).
5. **McEvedy & Jones / UN Population Prospects**: McEvedy, C., & Jones, R. (1978). *Atlas of World Population History*. Penguin Books. (Calibrates ancient and medieval demographic baselines).
6. **ORBIS (Stanford Geospatial Network Model of the Roman World)**: Scheidel, W. (2014). *ORBIS: The Stanford Geospatial Network Model*. (Calibrates `informationSpeed` in km/day).
7. **Palaeoclimate Datasets (PAGES 2k, NASA GISS, EPICA Ice Core)**: PAGES 2k Consortium (2019). *Consistent multidecadal variability in global temperature reconstructions*. Nature Geoscience, 12, 643–649. (Calibrates `temperatureAnomaly` and `co2Concentration`).
8. **Real Wages & Inequality**: Allen, R. C. (2001). *The Great Divergence in European Wages*. AJAE, 61(3), 411-447. Scheidel, W. (2017). *The Great Leveler*. Princeton Univ Press. (Calibrates `realUnskilledWage` and `giniInequality`).
9. **Energy & Soil Dynamics**: Smil, V. (2017). *Energy and Civilization: A History*. MIT Press. Montgomery, D. R. (2007). *Dirt: The Erosion of Civilizations*. Univ of California Press. (Calibrates `primaryEnergy`, `agriculturalEroei`, and `soilErosionRate`).

---

## 2. Complete 20-Variable Benchmark Catalogue

| Variable ID | Variable Name | Physical Unit | Time Horizon | Primary Academic Source |
| :--- | :--- | :--- | :--- | :--- |
| `worldPopulation` | Global Human Population | Millions of people | 10,000 BCE – 2026 CE | HYDE 3.2, McEvedy & Jones, UN WPP |
| `grossWorldProduct` | Gross World Product (GWP) | Billion 1990 Int. $ | 1 CE – 2026 CE | Maddison Project Database (2020) |
| `primaryEnergy` | Primary Energy Consumption | Exajoules (EJ/yr) | 1800 CE – 2026 CE | Vaclav Smil (2017), IEA |
| `urbanizationRate` | Urbanization Rate | % > 5,000 pop | 3,000 BCE – 2026 CE | Bairoch (1988), Chandler (1987), UN |
| `co2Concentration` | Atmospheric $\text{CO}_2$ | PPM | 10,000 BCE – 2026 CE | EPICA Ice Core, NOAA Mauna Loa |
| `literacyRate` | Adult Literacy Rate | % of adults | 1,500 CE – 2026 CE | Buringh & van Zanden (2009), UNESCO |
| `currencyDebasement` | Denarius Silver Purity | % Silver Content | 27 BCE – 274 CE | Butcher & Ponting (2014) |
| `eliteOverproductionIndex` | Elite Overproduction Index | Index (1.0 = baseline)| 500 BCE – 2026 CE | Peter Turchin (2016), Seshat Databank |
| `realUnskilledWage` | Purchasing Power | Index (100 = Subsist) | 1,300 CE – 2026 CE | Robert C. Allen (2001) |
| `politicalStressIndex` | Political Stress Index (PSI) | Composite Score 0-100 | 500 BCE – 2026 CE | Turchin & Nefedov (2009) |
| `asabiyyahSocialCohesion` | Asabiyyah Solidarity | Score 0.0 – 1.0 | 3,000 BCE – 2026 CE | Ibn Khaldun (1377), Turchin (2003) |
| `sociopoliticalInstability` | Sociopolitical Instability | Events / Decade | 1,000 BCE – 2026 CE | Seshat Databank, COW Project |
| `giniInequality` | Wealth & Land Gini | Gini Index 0.0 – 1.0 | 1 CE – 2026 CE | Walter Scheidel (2017), Milanovic |
| `sovereignDebtBurden` | Sovereign Debt Ratio | % of GDP / State Rev | 1,600 CE – 2026 CE | Reinhart & Rogoff (2009), IMF |
| `globalTradeVolume` | Global Trade Index | Index (100 = 1913 CE) | 1,500 CE – 2026 CE | Federico & Tena-Junguito (2017), WTO |
| `temperatureAnomaly` | Temperature Anomaly | °C vs 1850-1900 | 10,000 BCE – 2026 CE | PAGES 2k Consortium, NASA GISS |
| `soilErosionRate` | Topsoil Cumulative Erosion | % cumulative loss | 4,000 BCE – 2026 CE | Montgomery (2007), FAO |
| `agriculturalEroei` | Agricultural EROEI | Output/Input Ratio | 8,000 BCE – 2026 CE | Vaclav Smil (2008), Giampietro |
| `deforestationRate` | Remaining Forest Cover | % of original area | 8,000 BCE – 2026 CE | HYDE 3.2, FAO FRA |
| `informationSpeed` | Information Velocity | km / day | 500 BCE – 2026 CE | Scheidel (2014) ORBIS |

---

## 3. Mathematical Evaluation & Statistical Metrics

The `HistoricalValidationKernel` measures trajectory alignment using two standard goodness-of-fit metrics:

### 1. Root Mean Square Error (RMSE)
$$\text{RMSE} = \sqrt{\frac{1}{N} \sum_{k=1}^{N} \left( y_{\text{sim}}(t_k) - y_{\text{obs}}(t_k) \right)^2 }$$
where $y_{\text{sim}}(t_k)$ is the simulated value at time step $t_k$ and $y_{\text{obs}}(t_k)$ is the empirical benchmark observations from historical databases.

### 2. Coefficient of Determination ($R^2$)
$$R^2 = 1 - \frac{\sum_{k=1}^{N} \left( y_{\text{obs}}(t_k) - y_{\text{sim}}(t_k) \right)^2}{\sum_{k=1}^{N} \left( y_{\text{obs}}(t_k) - \bar{y}_{\text{obs}} \right)^2}$$
where $\bar{y}_{\text{obs}} = \frac{1}{N} \sum_{k=1}^{N} y_{\text{obs}}(t_k)$ represents the mean of empirical observations. An $R^2 \ge 0.85$ indicates strong empirical calibration fit across historical epochs.

---

## 4. Historical Calibration Test Integration

```java
// Load empirical cliodynamic benchmark dataset
HistoricalBenchmarkSuite suite = HistoricalBenchmarkSuite.loadFromResource(
    "/historical_cliodynamic_benchmarks.json"
);

// Evaluate simulation trajectory against empirical data
HistoricalValidationKernel kernel = new HistoricalValidationKernel();
ValidationResult result = kernel.evaluateTrajectory(
    simulatedData, 
    suite.getVariable("eliteOverproductionIndex")
);

System.out.printf("Cliodynamic Fit -> RMSE: %.4f | R²: %.4f%n", 
    result.getRMSE(), 
    result.getRSquared()
);
```
