# Ether Simulation — Differential Equations & State Variable Specification

> **Master Technical & Mathematical Specification**  
> *Version 4.0.0 — Physicalist & Cliodynamic Differential Equation Systems*

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

The evolution of human agent cohorts is governed by Kleiber metabolic scaling, energy intake, and Gompertz-Makeham mortality.

#### 1. Kleiber's $3/4$ Allometric Metabolic Scaling Law
Basal metabolic energy consumption for a human biomass cohort $M_{\text{hum}}$ is given by:
$$B_{\text{basal}} = B_0 \cdot M_{\text{hum}}^{3/4} \quad \left(B_0 = 3.39 \text{ W/kg}^{3/4}\right)$$

The internal energy reserve differential is updated as:
$$\frac{dE_{\text{cohort}}}{dt} = \dot{E}_{\text{ingested}} - \left( \frac{B_{\text{basal}}}{86,400} + M_{\text{hum}}^{1.1} \times 1000 \right) \cdot \Delta t$$

#### 2. Birth Rate & Biomass Genesis
$$\frac{dM_{\text{hum, births}}}{dt} = M_{\text{hum}} \cdot f(E_{\text{cohort}}, M_{\text{hum}})$$
$$f(E, M) = \begin{cases} 0.05 \cdot \left(1 - \frac{M}{2000}\right) & \text{if } E \ge 50.0 \text{ J} \\ 0.01 \cdot \left(1 - \frac{M}{2000}\right) & \text{otherwise} \end{cases}$$

#### 3. Gompertz-Makeham Mortality Rate
Instantaneous mortality rate $\mu(t_{\text{age}})$ incorporates biological senescence and famine shock:
$$\mu(t_{\text{age}}) = \alpha + \beta \cdot e^{\gamma \cdot t_{\text{age}}} + \mu_{\text{famine}}$$
- $\alpha = 0.0002 \text{ yr}^{-1}$ (Background environmental risk)
- $\beta = 0.00003 \text{ yr}^{-1}$, $\gamma = 0.085 \text{ yr}^{-1}$ (Gompertz senescence growth)
- $\mu_{\text{famine}} = 0.2 \text{ yr}^{-1}$ when internal energy reserve drops below zero ($E_{\text{cohort}} < 0$).

---

### B. Ecological & Arrhenius Biochemical Kinetics (`EnvironmentalKernel`)

Temporal variation of the cell food stock $F_i$ is expressed by the ODE:
$$\frac{dF_i}{dt} = \text{Prod}_{\text{biome}} \cdot k_{\text{Arrhenius}}(T_i) \cdot f_{\text{hydrology}}(R_i) - \lambda_{\text{decay}} \cdot F_i$$

#### Johnson-Eyring Arrhenius Temperature Response Function
$$k_{\text{Arrhenius}}(T) = \frac{\exp\left(-\frac{E_a}{R \cdot T_{\text{Kelvin}}}\right)}{\exp\left(-\frac{E_a}{R \cdot T_{\text{opt}}}\right)} \cdot f_{\text{denaturation}}(T)$$
- $E_a = 54,000 \text{ J/mol}$ (Enzyme activation energy)
- $R = 8.31446 \text{ J/(mol}\cdot\text{K)}$ (Universal gas constant)
- $T_{\text{opt}} = 298.15 \text{ K} \quad (25^\circ\text{C})$

---

### C. Onsager Thermodynamic Population Transport (`ThermodynamicMigrationEngine`)

Demographic movement between adjacent H3 cells $i$ and $j$ derives from the gradient of **per-capita free energy potential** $\Phi_i$:
$$\Phi_i = \frac{F_i + W_i}{N_i + 1}$$
$$\Delta \Phi_{ij} = \Phi_j - \Phi_i$$

The Onsager transport flux $J_{ij}$ across neighboring cell boundaries is:
$$J_{ij} = M_0 \cdot \Delta \Phi_{ij} \cdot N_i$$
$$\frac{dN_i}{dt} = - \sum_{j \in \text{Neighbors}(i)} J_{ij}, \quad \frac{dN_j}{dt} = + \sum_{j \in \text{Neighbors}(i)} J_{ij}$$
- $M_0 = 0.05$ (Onsager mobility coefficient).

---

### D. Climate & Radiative Greenhouse Forcing (`GreenhouseEngine`)

Radiative forcing $\Delta F$ (in $\text{W/m}^2$) from atmospheric $\text{CO}_2$ (ppm) and $\text{CH}_4$ (ppb) is modeled as:
$$\Delta F = 5.35 \cdot \ln\left(\frac{[\text{CO}_2]}{280.0}\right) + 0.036 \cdot \left(\sqrt{[\text{CH}_4]} - \sqrt{720.0}\right)$$

Global temperature anomaly $\Delta T$ and sea level shift $\Delta h_{\text{sea}}$ satisfy:
$$\Delta T = \lambda_{\text{climate}} \cdot \Delta F \quad (\lambda_{\text{climate}} = 0.8 \text{ }^\circ\text{C / (W/m}^2))$$
$$\Delta h_{\text{sea}} = 42.5 \cdot \Delta T \text{ meters}$$

---

### E. Tainter Institutional Complexity & Infrastructure Collapse (`UrbanKernel`)

Capital accumulation and maintenance overhead follow Joseph Tainter's theory of diminishing returns on complexity:
$$C_i = \ln(1 + 0.1 \cdot K_i)$$
$$\Sigma_{\text{maint}} = C_i^{\theta} \cdot 1000 \text{ Joules} \quad (\theta = 1.15)$$
$$\frac{dK_i}{dt} = P_i \cdot N_i \cdot 0.1 - \Sigma_{\text{maint}}$$

When maintenance cost $\Sigma_{\text{maint}}$ exceeds total economic output ($P_i \cdot N_i \cdot 0.1$), net capital delta $\frac{dK_i}{dt}$ becomes negative, causing infrastructure decay and rapid urban simplification.

---

## 5. Pluggable Formula Engine & Dynamic Formula Editor (`PluggableStatEngine`)

Ether includes an interactive, high-performance **Pluggable Formula Engine** (`PluggableStatEngine`) paired with a dynamic visual editor (`PluggableFormulaEditorDialog`). This system allows researchers and users to write custom mathematical and statistical expressions over the spatial H3 grid, compute metrics live, evaluate custom forcing factors, and import/export formula libraries via `.properties` or `.json` files.

### A. Accessible State Variable Index

The formula evaluator resolves scalar and array variables across all `H3Cell` nodes and the optimized `WorldBuffer`:

| Domain | Variable Identifier(s) | Description / Type |
| :--- | :--- | :--- |
| **Socio-Economy & Capital** | `wealth`, `gdp`, `capital`, `resourcecapital` | Accumulated capital stock and economic output per cell. |
| | `tech`, `technology`, `technologylevel` | Scientific and technological advancement index. |
| | `giniindex`, `gini` | Wealth/resource inequality distribution coefficient ($0.0 - 1.0$). |
| | `work`, `resourcework` | Productive workforce capacity ($N_{\text{adult}} \times T_{\text{tech}}$). |
| **Demographics & Cohorts** | `population`, `pop`, `biomasshuman` | Total human headcount per cell. |
| | `popyouth`, `popadult`, `popelderly` | Major demographic age cohorts (youth, adult, elderly). |
| | `pop0to4` ... `pop80plus` | 10 fine-grained 5-year age cohort arrays. |
| | `fertility`, `lifespan`, `age` | Total fertility rate, average lifespan, and mean population age. |
| | `epidemicinfected`, `epidemicrecovered` | SIR epidemiological infection and recovery counters. |
| **Climate & Geophysics** | `temperature`, `temp` | Surface ambient temperature ($^\circ\text{C}$). |
| | `rainfall`, `rain` | Annual precipitation ($\text{mm/yr}$). |
| | `elevation`, `alt` | Surface elevation above sea level ($\text{m}$). |
| | `pollution`, `pollutionlevel` | Environmental pollution concentration index. |
| | `albedo`, `dynamicalbedo` | Surface dynamic solar reflectance coefficient. |
| | `mantleheat`, `mantleheatflow` | Geothermal heat flow ($\text{mW/m}^2$). |
| **Biomass & Resources** | `food`, `foodresource` | Available edible biomass reserves. |
| | `water`, `aquifer`, `freshwateraquifer` | Freshwater table and aquifer reserves ($\text{m}^3$). |
| | `wood`, `metal`, `preciousmetal`, `clay` | Raw material reserves. |
| | `soilcarbon`, `soilorganiccarbon` | Soil organic carbon sequestration stock. |
| | `biomassnatural`, `biomasslivestock`, `biomassfish`, `biomassagriculture` | Primary natural, livestock, aquatic, and crop biomass stocks. |
| **Energy & Metabolism** | `energywind`, `energysolar`, `energyfire`, `energyslaves`, `energyfoodconsumed` | Extracted and consumed power flows ($\text{Joules}$). |

### B. Mathematical Operators & Aggregators

Formulas support nested algebraic operations and spatial aggregations:

* **Spatial Aggregators**: `SUM(var)`, `AVG(var)`, `MEDIAN(var)`, `VAR(var)`, `STDDEV(var)`, `MIN(var)`, `MAX(var)`, `GINI(var)`, `COUNT(var)`, `RANGE(var)`.
* **Algebraic & Transcendental Functions**: `+`, `-`, `*`, `/`, `%`, `^`, `SQRT()`, `ABS()`, `LOG()`, `EXP()`, `ROUND()`.

#### Example Expressions:
* **Per Capita Food Surplus**: `SUM(food) / COUNT(population)`
* **Societal Institutional Complexity**: `MAX(population * tech * (1 + giniindex))`
* **Environmental Degradation Shock**: `STDDEV(pollution) * AVG(popadult)`

### C. Library Persistence & Sharing

Custom formula sets are persisted or shared via standard `.properties` files (or `.json` bundles) using `importFormulasFromFile(File)` and `exportFormulasToFile(File)` without requiring application re-compilation or dynamic Java bytecode loading.

---

## 6. Test Suite Validation Results

All differential schemes, formula evaluators, and state variable bounds are validated by the automated test suite:
```text
[INFO] Results:
[INFO] Tests run: 102, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

