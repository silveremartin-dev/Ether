# Ether Simulation — Differential Equations & State Variable Specification

> **Master Technical & Mathematical Specification**  
> *Version 4.0.0 — Physicalist & Cliodynamic Differential Equation Systems, Pluggable Formula Engine, and Empirical Validation Suite*

---

## 1. Overview & Multi-Scale Temporal Architecture

The **Ether Engine** models planetary environmental and human societal dynamics across a global Uber H3 hexagonal grid ($175,000+$ cells at Resolution 6–8) using high-performance Data-Oriented Design structures (`WorldBuffer`, `Cell`, `AgentBuffer`). State evolution is computed by solving a coupled system of deterministic and stochastic ordinary and partial differential equations (ODEs/PDEs).

### Multi-Scale Temporal Decoupling

Ether decouples numerical integration across two discrete time scales:

- **Fast Tick Scale ($\Delta t_{\text{fast}} = 1 \text{ day} = 86,400 \text{ s}$)**:
  - Resolves instantaneous thermodynamic resource flux and logistics (`FluxEngine`).
  - Calculates daily market price equilibria based on local supply and demand ratios.
  - Computes viral epidemiological transmission across neighboring spatial nodes.

- **Slow Tick Scale ($\Delta t_{\text{slow}} = 30 \text{ days} \approx 2.592 \times 10^6 \text{ s} = \Delta t / 31,557,600 \text{ yr}$)**:
  - Resolves Arrhenius metabolic kinetics and demographic cohort aging (`DemographicKernel`).
  - Computes primary ecological biomass production and soil N-P-K nutrient depletion (`EnvironmentalKernel`).
  - Simulates population transport driven by Onsager free-energy thermodynamic gradients (`ThermodynamicMigrationEngine`).
  - Models institutional complexity capital accumulation and Tainter collapse (`UrbanKernel`).

---

## 2. Complete Catalog of State Variables & Parameters

### A. Geographic & Environmental State Variables

| Variable Name | Symbol | SI Unit | Module | Range / Default | Physical Description & Behavioral Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Elevation** | $z$ | $\text{m}$ | `H3Cell` / `Cell` | $-11,000$ to $+8,848$ | Topographic elevation above sea level. Governs biome assignment, hydrostatic pressure, and transport friction. |
| **Temperature** | $T$ | ${^\circ\text{C}}$ / $\text{K}$ | `AtmosphericEngine` | $-50.0$ to $+50.0$ | Ambient surface temperature derived from solar radiance and radiative greenhouse forcing. |
| **Precipitation** | $R$ | $\text{mm/yr}$ | `AtmosphericEngine` | $0.0$ to $5,000.0$ | Annual rainfall derived from Hadley cell circulation and orographic lifting. |
| **Food Resource Stock** | $F$ | $\text{units}$ | `EnvironmentalKernel` | $0.0$ to $\infty$ | Edible biomass available per cell. Regenerates via Arrhenius kinetics and is consumed by human/livestock cohorts. |
| **Radiative Forcing** | $\Delta F_{\text{CO2}}$ | $\text{W/m}^2$ | `GreenhouseEngine` | $0.0$ to $+10.0$ | Radiative forcing anomaly from atmospheric $\text{CO}_2$ and $\text{CH}_4$ concentrations. |
| **Sea Level Anomaly** | $h_{\text{sea}}$ | $\text{m}$ | `GreenhouseEngine` | $-120.0$ to $+70.0$ | Global sea level shift driven by ocean thermal expansion and ice sheet melt. |
| **Accessible Aquifer** | $W_{\text{aqua}}$ | $\text{m}^3$ | `HydrographyEngine` | $0.0$ to $10^9$ | Subsurface freshwater table. Depleted by irrigation/cities and recharged by precipitation. |
| **Soil Nutrients (N-P-K)** | $N_{\text{npk}}$ | $\text{kg/ha}$ | `SoilEngine` | $0.0$ to $1.0$ | Soil fertility index governing agricultural crop yields and degradation under intensive farming. |

### B. Demographic & Metabolic State Variables

| Variable Name | Symbol | SI Unit | Module | Range / Default | Physical Description & Behavioral Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Human Population** | $N$ | $\text{individuals}$ | `DemographicKernel` | $0$ to $10^7$ | Discrete human headcount on cell. Serialized as `humanCount` or `localPopulation`. |
| **Human Biomass** | $M_{\text{hum}}$ | $\text{kg}$ | `DemographicKernel` | $0.0$ to $\infty$ | Total mass of human agent cohorts ($M_{\text{hum}} \approx N \times 50.0\text{ kg}$). Governs Kleiber metabolic baseline. |
| **Cohort Age** | $t_{\text{age}}$ | $\text{years}$ | `DemographicKernel` | $0.0$ to $100.0$ | Elapsed time since cohort genesis. Determines Gompertz actuarial senescence mortality. |
| **Cohort Internal Energy** | $E_{\text{cohort}}$ | $\text{Joules}$ | `DemographicKernel` | $-\infty$ to $\infty$ | Stored metabolic energy reserve available for physical work, reproduction, and survival. |
| **Cell Lifespan** | $L_{\text{cell}}$ | $\text{years}$ | `Cell` | $20.0$ to $85.0$ (Def: $40$) | Expected average life expectancy under local sanitary and dietary conditions. |
| **Cell Fertility** | $f_{\text{cell}}$ | $\text{children/woman}$| `Cell` | $0.0$ to $8.0$ (Def: $6.0$) | Total fertility rate parameter governing potential birth rate per female cohort. |

### C. Energy, Infrastructure & Cliodynamic State Variables

| Variable Name | Symbol | SI Unit | Module | Range / Default | Physical Description & Behavioral Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Economic Potential** | $P_i$ | $\text{value index}$ | `FluxEngine` | $0.0$ to $\infty$ | Ratio of local commodity supply to demand. Drives trade gradient flows and price formation. |
| **Institutional Complexity** | $C_i$ | $\text{adimensional}$ | `UrbanKernel` | $0.0$ to $10.0$ | Tainter complexity metric of organizational overhead ($C_i = \ln(1 + 0.1 K_i)$). |
| **Capital Stock** | $K_i$ | $\text{Joules / \$}$ | `UrbanKernel` | $0.0$ to $\infty$ | Accumulated physical infrastructure and industrial machinery. Requires maintenance energy. |
| **Net Energy EROEI** | $\text{EROEI}$ | $\text{ratio}$ | `NetEnergyEngine` | $0.5$ to $100.0$ | Energy Return on Energy Invested for local primary energy extraction (fossil, solar, nuclear). |
| **Gini Inequality** | $G$ | $0.0 - 1.0$ | `HANDY / Lenski` | $0.20$ to $0.85$ | Wealth and resource distribution inequality index among elite and commoner cohorts. |
| **Frontier Asabiyyah** | $A$ | $0.0 - 1.0$ | `TurchinEngine` | $0.0$ to $1.0$ | Collective solidarity score. Forged along hostile frontiers and decayed in affluent hinterlands. |
| **Elite Overproduction** | $I_{\text{elite}}$ | $\text{index}$ | `TurchinEngine` | $0.5$ to $5.0$ | Ratio of elite aspirants to available structural positions. Triggers political instability. |

---

## 3. Mathematical Systems of Differential Equations

### A. Demographic Metabolism & Actuarial Dynamics (`DemographicKernel`)

#### 1. Kleiber's $3/4$ Allometric Metabolic Scaling Law
$$B_{\text{basal}} = B_0 \cdot M_{\text{hum}}^{3/4} \quad \left(B_0 = 3.39 \text{ W/kg}^{3/4}\right)$$

$$\frac{dE_{\text{cohort}}}{dt} = \dot{E}_{\text{ingested}} - \left( \frac{B_{\text{basal}}}{86,400} + M_{\text{hum}}^{1.1} \times 1000 \right) \cdot \Delta t$$

#### 2. Gompertz-Makeham Mortality Rate
$$\mu(t_{\text{age}}) = \alpha + \beta \cdot e^{\gamma \cdot t_{\text{age}}} + \mu_{\text{famine}}$$

---

### B. Ecological & Arrhenius Biochemical Kinetics (`EnvironmentalKernel`)

$$\frac{dF_i}{dt} = \text{Prod}_{\text{biome}} \cdot k_{\text{Arrhenius}}(T_i) \cdot f_{\text{hydrology}}(R_i) - \lambda_{\text{decay}} \cdot F_i$$

---

### C. Onsager Thermodynamic Population Transport (`ThermodynamicMigrationEngine`)

$$\Phi_i = \frac{F_i + W_i}{N_i + 1}, \quad \Delta \Phi_{ij} = \Phi_j - \Phi_i$$
$$J_{ij} = M_0 \cdot \Delta \Phi_{ij} \cdot N_i$$

---

### D. Tainter Institutional Complexity & Infrastructure Collapse (`UrbanKernel`)

$$C_i = \ln(1 + 0.1 \cdot K_i)$$
$$\Sigma_{\text{maint}} = C_i^{\theta} \cdot 1000 \text{ Joules} \quad (\theta = 1.15)$$
$$\frac{dK_i}{dt} = P_i \cdot N_i \cdot 0.1 - \Sigma_{\text{maint}}$$

---

## 4. Pluggable Formula Engine & Dynamic Formula Editor (`PluggableStatEngine`)

Ether includes an interactive, high-performance **Pluggable Formula Engine** (`PluggableStatEngine`) allowing researchers to write custom expressions over spatial H3 cell buffers.

### Accessible Variable Domain
- **Socio-Economy**: `wealth`, `gdp`, `capital`, `tech`, `giniindex`, `work`.
- **Demographics**: `population`, `popyouth`, `popadult`, `popelderly`, `fertility`, `lifespan`.
- **Climate & Land**: `temperature`, `rainfall`, `elevation`, `pollution`, `albedo`.
- **Resources & Energy**: `food`, `water`, `wood`, `metal`, `energysolar`, `energywind`.

---

## 5. Historical Benchmarks & Empirical Validation Suite

Ether integrates a 20-variable empirical validation suite (`historical_cliodynamic_benchmarks.json`) spanning **10,000 BCE to 2026 CE** sourced from **Seshat**, **Maddison Project**, **HYDE 3.4**, **ORBIS**, and **COW**.

### Statistical Metrics
1. **Root Mean Square Error (RMSE)**:
   $$\text{RMSE} = \sqrt{\frac{1}{N} \sum_{k=1}^{N} \left( y_{\text{sim}}(t_k) - y_{\text{obs}}(t_k) \right)^2 }$$

2. **Coefficient of Determination ($R^2$)**:
   $$R^2 = 1 - \frac{\sum_{k=1}^{N} \left( y_{\text{obs}}(t_k) - y_{\text{sim}}(t_k) \right)^2}{\sum_{k=1}^{N} \left( y_{\text{obs}}(t_k) - \bar{y}_{\text{obs}} \right)^2}$$

   An $R^2 \ge 0.85$ indicates strong empirical calibration fit across historical epochs.
