# 📡 Telemetry Architecture, First-Order Biophysical Observables & Cliodynamic Metrics

> **Ether Cliodynamic & Planetary Epistemic Laboratory**  
> *Author: Silvere Martin-Michiellot*  
> *Standards Compliance: AGENTS.md §8, §9 & §10 — First-Order Physical Telemetry & Headless Decoupling*  
> *Language: English*

---

## 🎯 1. Architectural Philosophy & The First-Order Telemetry Invariant

In strict adherence to **AGENTS.md §10.4**, the telemetry extraction system is governed by three non-negotiable architectural axioms:

1. **First-Order Conserved Biophysical Observables as the Sole Source of Truth**:
   Telemetry collectors (`SimulationMetricsAggregator`, `MetricSnapshot`, `HistorySnapshot`, `PostHocCliodynamicMetricsExtractor`) MUST sample **conserved physical, chemical, thermodynamic, and demographic observables directly from the cellular buffers** (`WorldBuffer`, `H3Cell`).
2. **Strict Ban on Cascading Synthetic Scalars**:
   It is **STRICTLY FORBIDDEN** to route macroscopic indicators (Gross World Product, Primary Energy, Urbanization, Literacy, Deforestation, Soil Erosion, Freight Shipping Costs, EROEI, Life Expectancy) through an intermediate synthetic scalar such as `avgTech`, `technologyLevel`, or calendar year hacks (`year <= 1492`).
   Every macroeconomic or cliodynamic metric MUST derive directly and analytically from primary observables:
   $$M = f\left(N, \; K, \; P_{\text{watts}}, \; B_{\text{agr}}, \; W_{\text{water}}, \; M_{\text{metal}}, \; \text{Gini}, \; S_{\text{stability}}\right)$$
3. **Headless Execution Decoupling**:
   Heavy analytical evaluations (Seshat complexity index, Lorenz curve Gini, Marchetti logistic transitions, ORBIS routing friction) are computed **strictly a posteriori** from persistent binary snapshots or decoupled batch extractors, leaving the inner high-frequency cellular simulation loop running at full SIMD/DOD memory bandwidth.

---

## 🧱 2. The 12 Conserved First-Order Physical Observables

Every simulation step records twelve fundamental physical state variables aggregated across all active spatial cells. These represent **pure, non-composite, directly measurable physical and biological state quantities**:

| # | Symbol | Observable Name | Physical Unit | Cellular Buffer Source | Aggregation & Integration Formula |
|---|---|---|---|---|---|
| **1** | $N$ | **Human / Agent Population** | Inhabitants | `biomassHuman[i]` | $N = \sum_{i=0}^{C-1} P_i$ |
| **2** | $K$ | **Physical Built Capital Stock** | $1990\,\$$ / kg | `resourceCapital[i]` | $K = \sum_{i=0}^{C-1} K_i$ |
| **3** | $P_{\text{watts}}$ | **Primary Power Dissipation** | Watts ($\text{W}$) | `powerWattsPerCapita[i]` | $P_{\text{watts}} = \sum_{i=0}^{C-1} \left( p_{i, \text{metabolic}} + p_{i, \text{mechanical}} + p_{i, \text{grid}} \right) \cdot P_i$ |
| **4** | $B_{\text{agr}}$ | **Agricultural Crop Biomass** | Metric Tons | `biomassAgriculture[i]` | $B_{\text{agr}} = \sum_{i=0}^{C-1} B_{\text{agr}, i}$ |
| **5** | $B_{\text{nat}}$ | **Wild Ecological Biomass** | Metric Tons | `biomassNatural[i]` | $B_{\text{nat}} = \sum_{i=0}^{C-1} B_{\text{nat}, i}$ |
| **6** | $B_{\text{livestock}}$ | **Domesticated Animal Biomass** | Metric Tons | `biomassFauna[i]` | $B_{\text{livestock}} = \sum_{i=0}^{C-1} B_{\text{livestock}, i}$ |
| **7** | $W_{\text{potable}}$ | **Accessible Potable Water** | $\text{m}^3$ | `resourceWater[i]` | $W_{\text{potable}} = \sum_{i=0}^{C-1} W_{i} \cdot A_{\text{cell}, i} \cdot 10^3$ |
| **8** | $M_{\text{metal}}$ | **Smelted Structural Metallurgy** | Metric Tons | `resourceMetals[i]` | $M_{\text{metal}} = \sum_{i=0}^{C-1} M_{\text{smelted}, i}$ |
| **9** | $A_{\text{hab}}$ | **Occupied Ecumene Surface Area** | $\text{km}^2$ | Spatial geometry | $A_{\text{hab}} = \sum_{i \mid P_i > 0} A_{\text{cell}, i}$ |
| **10** | $\text{CO}_2$ | **Atmospheric Carbon Dioxide** | ppm | Global atmospheric reservoir | $\text{CO}_2(t) = \text{CO}_2(t_0) + \int_{t_0}^t \frac{F_{\text{fossil}} + F_{\text{landuse}} - F_{\text{sink}}}{2.13 \times 10^{12}\,\text{kg/ppm}} \, dt'$ |
| **11** | $\text{Gini}$ | **Lorenz Wealth Inequality** | $[0.0, 1.0]$ | Cell-level wealth distribution | $\text{Gini} = \frac{\sum_{i=1}^C \sum_{j=1}^C |w_i - w_j| \cdot P_i P_j}{2 N \sum_{i=1}^C w_i P_i}$ |
| **12** | $S_{\text{inst}}$ | **Institutional State Stability** | $[0.0, 100.0]$ | `institutionalStability[i]` | $S_{\text{inst}} = \frac{\sum_{i=0}^{C-1} S_i \cdot P_i}{N}$ |

### 2.1. Cellular Buffer Extraction Details
1. **Population ($N$)**: Directly summed from `WorldBuffer.biomassHuman[i]`, representing the total count of living human or agent entities on the planet.
2. **Capital Stock ($K$)**: Directly summed from `WorldBuffer.resourceCapital[i]`, tracking physical structural units (masonry, iron, tools, roads, machinery) in constant 1990 Geary-Khamis dollars.
3. **Power Dissipation ($P_{\text{watts}}$)**: Evaluated per cell as the sum of basal metabolic rate ($80\text{ W/hab}$), animal/somatic mechanical work, and active mechanical/electrical power grids:
   $$p_i = p_{\text{basal}} + p_{\text{mechanical}} + p_{\text{grid}}$$
   and multiplied by cell population $P_i$.
4. **Biomasses ($B_{\text{agr}}, B_{\text{nat}}, B_{\text{livestock}}$)**: First-order mass stocks derived from photosynthetic Miami NPP and trophic conversion loops.
5. **Freshwater ($W_{\text{potable}}$)**: Darcy unconfined aquifer hydraulic storage integrated across cell surface areas.
6. **Carbon Dioxide ($\text{CO}_2$)**: Dynamic atmospheric tracer driven by the Bern carbon cycle and fossil emissions.
7. **Lorenz Inequality ($\text{Gini}$)**: Standard Gini coefficient computed over the demographic distribution of per-capita wealth $w_i = (K_i + B_{\text{agr}, i}) / P_i$.
8. **Institutional Stability ($S_{\text{inst}}$)**: Population-weighted mean of state administrative integrity and tax compliance.

---

## 📐 3. Mathematical Formulations of Emergent Historical Diagnostics

All higher-level civilizational indicators are **derived composites** reconstructed *a posteriori* from the 12 pure observables above. They are **never stored as simulation state** and **never feed back into physical laws**.

### 3.1. Garrett-Lotka Thermodynamic Emergent Technology Index ($T_{\text{emergent}}$)
* **Status**: **Derived Composite Metric** (Computed *a posteriori* for telemetry, UI cards, and historical benchmarking; NEVER a cellular state variable).
* **Location in Code**: `H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex()` / `PostHocCliodynamicMetricsExtractor.computeEmergentTechnologyLevel()`.
* **Literature**: Garrett (2011, 2012); Lotka (1922); White (1943).
* **Principle**: Technology is not an accumulative state scalar. Civilizational advancement is governed by the exosomatic energy ratio ($P_{\text{total}}/P_{\text{basal}}$) and active capital energized by continuous power dissipation.
* **Governing Equations**:
  $$p_{\text{eff}} = \operatorname{softmax}(P_{\text{basal}}, \; P_{\text{total}}, \; 0.15 P_{\text{basal}})$$
  $$\text{MetabolicRatio} = \frac{p_{\text{eff}}}{P_{\text{basal}}}$$
  $$K_{\text{energized}} = \frac{\operatorname{softplus}(p_{\text{eff}} - 0.80 P_{\text{basal}}, \; 20.0)}{\eta_{\text{Garrett}}}$$
  $$K_{\text{active}} = \frac{2 \cdot K_{\text{gross}} \cdot K_{\text{energized}}}{K_{\text{gross}} + K_{\text{energized}} + \epsilon}$$
  $$T_{\text{emergent}} = \operatorname{sclamp}\left(0.50 + 2.15 \log_{10}(\max(1.0, \text{MetabolicRatio})) + 0.58 \log_{10}\left(1.0 + \frac{K_{\text{active}}}{K_{\text{ref}}}\right), \; 0.0, \; 10.0\right)$$
* **Post-Apocalyptic Invariance**: If $N \to 0$ or energy grids fail ($p_{\text{eff}} \to P_{\text{basal}}$), $K_{\text{active}} \to 0$, smoothly returning $T_{\text{emergent}}$ to paleolithic subsistence ($\le 1.0$), preventing ghost-city capital divergence.
* **Post-Apocalyptic Invariance**: If $N \to 0$ or energy grids fail ($p_{\text{eff}} \to P_{\text{basal}}$), $K_{\text{active}} \to 0$, smoothly returning $T_{\text{emergent}}$ to paleolithic subsistence ($\le 1.0$), preventing ghost-city capital divergence.

---

### 3.2. Two-Stage Gross World Product (GWP)
* **Literature**: Maddison (2007); Bolt & van Zanden (2020).
* **Principle**: Unifies Malthusian subsistence economies ($\$450–\$900/\text{capita}$) with Solow-Swan industrial capital deepening without calendar-year gating.
* **Governing Equation**:
  $$Y_{\text{agrarian}}(k_{\text{pc}}) = 450.0 + 800.0 \cdot \tanh\left(\frac{k_{\text{pc}}}{600.0}\right) \quad [1990\,\$]$$
  $$Y_{\text{industrial}}(k_{\text{pc}}, P_{\text{watts}}) = \frac{16500.0}{1.0 + \exp\left(-\left(\frac{P_{\text{watts}} - 1500.0}{450.0}\right)\right)} \cdot \tanh\left(\frac{k_{\text{pc}}}{1200.0}\right)$$
  $$\text{GWP} = \frac{\sum_i P_i \cdot \left(Y_{\text{agrarian}} + Y_{\text{industrial}}\right)}{10^9} \quad [\text{Billion } 1990\,\$]$$

---

### 3.3. Nucleated Urbanization Rate
* **Literature**: Bairoch (1988); Chandler (1987); HYDE 3.4.
* **Principle**: An urban settlement requires physical nucleation of $\ge 5\,000$ inhabitants within a contiguous agglomeration.
* **Governing Equation**:
  $$\text{NucleationFactor}(P_i) = \tanh\left(\frac{\max(0.0, \; P_i - 3000)}{12000}\right)$$
  $$\text{UrbanShare}_i(k_{\text{pc}}, P_i) = \left( \frac{0.085}{1 + e^{-2.0 (\ln(k_{\text{pc}}) - 5.50)}} + \frac{0.75}{1 + e^{-\left(\frac{P_{\text{watts}} - 1200}{350}\right)}} \right) \cdot \text{NucleationFactor}(P_i)$$
  $$\text{UrbanizationRate} = \frac{\sum_i P_i \cdot \text{UrbanShare}_i}{\sum_i P_i} \times 100\%$$

---

### 3.4. Two-Stage Information Transmission Velocity
* **Literature**: Scheidel (2014) ORBIS; UN ITU Historical Telecom Database.
* **Principle**: Physical phase transition separating biological/mechanical transport (horses, rail) from electromagnetic propagation (telegraph, radio, internet).
* **Governing Equation**:
  $$v_{\text{courier}}(k_{\text{pc}}) = 4.0 + 76.0 \cdot \tanh\left(\frac{k_{\text{pc}}}{300.0}\right) \quad [\text{km/day}]$$
  $$v_{\text{rail}}(P_{\text{watts}}) = \frac{2400.0}{1.0 + \exp\left(-\left(\frac{P_{\text{watts}} - 650.0}{150.0}\right)\right)} \quad [\text{km/day}]$$
  $$v_{\text{telecom}}(P_{\text{watts}}, k_{\text{pc}}) = [P_{\text{watts}} > 900 \land k_{\text{pc}} > 600] \implies \frac{3.0 \times 10^8}{1.0 + \exp\left(-\left(\frac{P_{\text{watts}} - 1200.0}{200.0}\right)\right)} \quad [\text{km/day}]$$
  $$v_{\text{info}} = \operatorname{clamp}\left(v_{\text{courier}} + v_{\text{rail}} + v_{\text{telecom}}, \; 4.0, \; 3.0 \times 10^8\right)$$

---

### 3.5. Peter Turchin Structural-Demographic Political Stress Index ($\Psi$)
* **Literature**: Turchin (2003, 2016); Goldstone (1991).
* **Principle**: Measures systemic state breakdown pressure driven by elite overproduction, wage immiseration, and fiscal geopolitical stress.
* **Governing Equation**:
  $$S = 0.50 \cdot \text{FiscalStress} + 0.50 \cdot \text{GeopoliticalTension}$$
  $$\Psi = \text{BandBaseline} + \Phi_{\text{state}}(S_{\text{inst}}) \cdot \left(0.65 \cdot \text{Conflict} + 2.00 \cdot \text{EOI} + 0.35 \cdot S\right)$$
  where $\text{BandBaseline} = 48.0 - 0.25 \cdot (S_{\text{inst}} - 50.0)$, calibrated to Seshat empirical baselines ($[40, 55]$ in stable empires, $[80, 100]$ during state breakdown).

---

### 3.6. Continuous Sagan-Kardashev Planetary Energy Scale ($K_{\text{sagan}}$)
* **Literature**: Kardashev (1964); Sagan (1973).
* **Governing Equation**:
  $$K_{\text{sagan}} = \frac{\log_{10}\left(\max(1.0, \; P_{\text{watts, total}})\right) - 6.0}{10.0}$$
  where $P_{\text{watts, total}} = \sum_i P_{i, \text{watts}} \cdot P_i$.
  * Type 0.70 (1970 CE): $P \approx 10^{13}\,\text{W}$.
  * Type 1.0 (Planetary grid): $P \approx 10^{16}\,\text{W}$.

---

## 💾 4. Persistence Formats & Serialization Architecture

Telemetry is stored hierarchically across three coupled structures:

1. **`MetricSnapshot` (Memory & Per-Tick Vector)**:
   * Compact map of scalar metrics key $\to$ double value.
   * Serialized into compressed binary buffers: `snapshots/snapshot_tick_XXXXXXXXXX.bin.gz`.
2. **`HistorySnapshot` (Macro Timeseries)**:
   * Gapless chronological trajectory of global averages over decades and centuries.
   * Exported as human-readable JSON: `saves/RUN-<scenario>/history.json`.
3. **`SimulationRunRecord`**:
   * Complete provenance manifest containing initial scenario parameters, seed, biophysical organism profile, and empirical validation metrics ($R^2$, RMSE against HYDE 3.4 and Maddison).
