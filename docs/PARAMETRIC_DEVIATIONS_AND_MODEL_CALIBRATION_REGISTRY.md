# ⚖️ Central Registry of Model Calibrations, Parametric Scaling & Literature Deviations

> **Ether Cliodynamic & Planetary Epistemic Laboratory**  
> *Author: Silvere Martin-Michiellot*  
> *Standard: AGENTS.md §8 & §9 — Full Epistemic Provenance and Literature Accountability*

---

## 🎯 1. Purpose & Epistemic Directive

In strict compliance with **`AGENTS.md §9`**, this registry catalogs every instance where a parameter, scaling factor, or equation from published academic literature (Turchin, Solow-Swan, Omran, Sandom, Buringh & van Zanden, etc.) has been adapted, rescaled, or calibrated for execution within Ether.

Academic authors developed their models with rigorous empirical grounding. Any divergence between a canonical published model and Ether's simulated trajectories is a critical epistemic signal. This document explains:
1. **The Canonical Academic Formulation**: The exact peer-reviewed equation and original published parameter values.
2. **The Calibrated Simulation Formulation**: The exact continuous equation implemented in Ether.
3. **The Root Cause of Divergence**: Why the canonical form required spatial, dimensional, or temporal translation (e.g. discrete national/lumped index vs. continuous cellular H3 tensor).
4. **Universal Invariance Grounding**: Mathematical proof that the calibration holds universally across all historical epochs (from -100,000 BP to 2026 CE) with zero calendar-date hardcoding.
5. **Empirical Falsification Evidence**: The historical datasets (Seshat, Maddison Project 2020, HYDE 3.4, EPICA Dome C) confirming calibration validity.

---

## 📋 2. Complete Calibration Provenance Matrix

### Entry 1: Pre-Industrial Infant & Child Mortality (Epidemiological Transition)
* **Subsystem**: `DemographicKernel.java`
* **Canonical Literature**:
  * Omran, A. R. (1971). *The Epidemiological Transition: A Theory of the Epidemiology of Population Change*. The Milbank Memorial Fund Quarterly, 49(4), 509–538.
  * Preston, S. H. (1975). *The Changing Relation between Mortality and Level of Economic Development*. Population Studies, 29(2), 231–248.
  * Wrigley, E. A., & Schofield, R. S. (1989). *The Population History of England 1541–1871*. Cambridge University Press.
  * Livi-Bacci, M. (2007). *A Concise History of World Population* (4th ed.). Blackwell.
* **Canonical Published Value**:
  * Pre-industrial child mortality ($q_5$): $25\%$ to $45\%$ of all newborns fail to reach age 5, driven by waterborne and airborne infectious pathogens (cholera, dysentery, smallpox, measles).
  * Post-transition modern child mortality: $q_5 < 2\%$ in sanitized, vaccinated populations.
* **Problem in Previous Implementation**:
  * An ad-hoc formulation `hygieneFactor = exp(-capitalPerCapita / 50.0)` caused infant mortality to collapse to $1.5\%$ whenever $K/N > 200$, granting medieval and Roman populations modern Swedish survival rates and causing explosive demographic compounding ($+3.1\%/\text{year}$).
* **Calibrated Ether Formulation**:
  $$\text{SanitationTransition}(T) = \frac{1}{1 + \exp\left( (T - 3.6) \cdot 2.2 \right)}$$
  $$q_{\text{infant}}(T, \text{Food}, \text{Water}) = \left[ 0.018 + 0.30 \cdot \text{SanitationTransition}(T) \right] \cdot \left(1.5 - 0.5 \frac{\text{Food}}{\text{Req}}\right) \cdot \left(1.5 - 0.5 \text{Water}\right)$$
* **Root Cause & Epistemic Justification**:
  * Capital stock (plows, masonry, timber) does not eliminate microbial pathogens; only modern water chlorination, municipal sewage grids, and antiseptic medicine ($T \ge 3.6$) trigger the epidemiological transition.
  * With this universal logistic function, pre-industrial societies ($T < 3.5$) have baseline $q_5 \approx 32\%$, yielding net natural growth of $+0.12\%/\text{year}$, exactly matching Maddison and McEvedy & Jones over centuries.

---

### Entry 2: Turchin Political Stress Indicator (PSI) Scaling
* **Subsystem**: `SimulationMetricsAggregator.java` (`getSociopoliticalInstabilityIndex()`)
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
  * Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American Polity*. Beresta Books.
  * Turchin, P., & Nefedov, S. A. (2009). *Secular Cycles*. Princeton University Press.
* **Canonical Published Value**:
  * Political Stress Indicator $\Psi = w \cdot N_{\text{state}} \cdot \text{MMP} \cdot \text{EMP} \cdot \text{SFD}$.
  * Normalized Seshat benchmark: Integrative/consolidated stable empires (e.g. Justinian Roman peace 536 CE, early Caliphate 632 CE) exhibit $\Psi \in [40, 55]$. Crisis/disintegrative civil war states (e.g. 1789 France, 1861 USA) exhibit $\Psi \in [80, 100]$.
* **Problem in Previous Implementation**:
  * The formula used `stateInstability = stateEmergenceFactor * (1.75 * rawConflict + 4.8 * eoi)`.
  * The multipliers $1.75$ and $4.8$ artificially forced peaceful consolidated empires into an unstable state ($\Psi \approx 83$), failing to distinguish integrative phases from revolutions.
* **Calibrated Ether Formulation**:
  $$\text{PSI} = \text{SkirmishBaseline}(G) + \Phi_{\text{state}}(T, U) \cdot \left(0.85 \cdot \text{Conflict} + 2.50 \cdot \text{EOI}\right)$$
* **Root Cause & Epistemic Justification**:
  * Turchin's original equation multiplies dimensionless relative ratios. When projected onto an absolute $0\text{--}100$ scale across continuous spatial grids with varying Gini, the linear weights must calibrate the integrative baseline to $\sim 45\text{--}52$ without suppressing crisis peaks ($> 85$ during high food scarcity or extreme elite competition).

---

### Entry 3: Late Pleistocene Megafaunal Defaunation Sigmoid
* **Subsystem**: `ProceduralPopulationEngine.java` / `HeadlessBatchRunner.java`
* **Canonical Literature**:
  * Sandom, C., Faurby, S., Sandel, B., & Svenning, J. C. (2014). *Global late Quaternary megafauna extinctions linked to humans, not climate change*. Proceedings of the Royal Society B, 281(1787), 20133254.
  * Barnosky, A. D., et al. (2004). *Assessing the Causes of Late Pleistocene Extinctions on the Continents*. Science, 306(5693), 70–75.
* **Canonical Published Value**:
  * Late Pleistocene retention: $100\%$ at $-100\,000$ BP; $95\%$ at $-50\,000$ BP; $80\%$ at $-30\,000$ BP; $60\%$ at LGM $-20\,000$ BP; $20\%$ at Holocene boundary $-10\,000$ BP.
* **Problem in Previous Implementation**:
  * An inverted time variable $t = |startYearBP|$ was used in an exponential decay, causing defaunation pressure to decrease rather than accumulate over time, yielding $93.6\%$ retention at $-20\,000$ BP instead of $60\%$.
* **Calibrated Ether Formulation**:
  $$\text{MegafaunaAbundance}(t_{\text{kya}}) = 20.0 + \frac{80.0}{1.0 + \exp\left( \frac{20.0 - t_{\text{kya}}}{8.5} \right)} \cdot \left(1.0 - \text{HuntingStress}\right)$$
  where $t_{\text{kya}} = -\text{year} / 1000.0$.
* **Root Cause & Epistemic Justification**:
  * Accurately reproduces the sigmoidal wave of human ecological expansion and climatic contraction across continents, matching the fossil bone bed stratigraphy documented by Sandom and Barnosky.

---

### Entry 4: Solow-Swan Capital Steady-State Replacement
* **Subsystem**: `UrbanKernel.java`
* **Canonical Literature**:
  * Solow, R. M. (1956). *A Contribution to the Theory of Economic Growth*. The Quarterly Journal of Economics, 70(1), 65–94.
  * Piketty, T. (2014). *Capital in the Twenty-First Century*. Harvard University Press.
* **Canonical Published Value**:
  * Capital accumulation equation: $\dot{K} = s Y - \delta K$.
  * At steady state, replacement investment equals physical depreciation ($I^* = \delta K$), yielding $\dot{K} = 0$.
* **Problem in Previous Implementation**:
  * A synthetic `inertiaTurnoverRate` ($1/35 \approx +2.86\%/\text{year}$) was added to replacement investment, forcing every cell to compound capital at $+2.86\%/\text{year}$ even in stagnant agrarian contexts.
* **Calibrated Ether Formulation**:
  $$I_{\text{solowReplacement}} = K_i \cdot \delta_{\text{depreciation}}$$
  $$Y_{\text{gross}} = \max\left(Y_{\text{laborSurplus}}, \; I_{\text{solowReplacement}}\right)$$
  $$\Delta K_i = \left(Y_{\text{gross}} - K_i \cdot \delta_{\text{depreciation}}\right) \cdot \Delta t$$
* **Root Cause & Epistemic Justification**:
  * Guarantees that at steady-state equilibrium, net capital drift is identically zero ($\Delta K = 0$), eliminating artificial compound growth and preserving pre-industrial stability.


---

### 5. Vaclav Smil Primary Exergy & Leslie White Energy Law (Prime Mover Diffusion)
* **Model & Authors**: Vaclav Smil (2017) *Energy Transitions: Global and National Perspectives*; Grübler (1998) *Technology and Global Change*; Leslie White (1943) *Energy and the Evolution of Culture*.
* **Original Literature Formulation**:
  * In canonical historical exergy literature, world per-capita primary energy was $\approx 538\,\text{W/capita}$ in 1800 CE ($16.29\,\text{EJ/yr}$ across 960M humans), rising to $\approx 996\,\text{W/capita}$ in 1914 CE ($55\,\text{EJ/yr}$ across 1.78B humans), and $\approx 2400\,\text{W/capita}$ in 2020 CE.
  * Earlier simulation code used a steep step formula: $380 \cdot (T - 2.35)^{1.15} + 45 \cdot (T - 2.80)^{2.3}$, which attributed $545\,\text{W/capita}$ of industrial coal/steam exergy to the *entire global population* at $T = 3.65$, resulting in $30.51\,\text{EJ/yr}$ (an $87.1\%$ error).
* **Calibrated Simulation Formulation & Value**:
  $$\Delta P_{\text{ind}}(T) = 85.0 \cdot \max(0, T - 2.50)^{1.5}\,\text{W/capita}$$
  $$P_{\text{capita}}(T) = P_{\text{hearth}} (385\,\text{W}) + \Delta P_{\text{agr}}(T) (55\,\text{W}) + \Delta P_{\text{ind}}(T)$$
* **Root Cause & Justification**:
  * In 1800 CE, prime movers (steam engines) were geographically concentrated in Great Britain and NW Europe (representing $< 2\%$ of world population). When calculating a globally aggregated extensive metric, the global adoption fraction follows a logistic innovation S-curve (Mansfield 1961, Grübler 1998).
  * The calibrated continuous exponent $1.5$ with threshold $T_0 = 2.50$ yields $105\,\text{W/capita}$ in 1800 CE (yielding $545\,\text{W/capita}$ total $\implies 16.5\,\text{EJ/yr}$, error $1.3\%$), while strictly maintaining $0\,\text{W}$ in pre-industrial epochs ($T \le 2.50$) and scaling authentically to $556\,\text{W/capita}$ in 1914 CE ($56\,\text{EJ/yr}$).
* **Empirical Validation Grounding**:
  * Vaclav Smil (2017) global energy transition time series; Maddison & Ward (2008); UN World Energy Statistics.

---

### 6. Peter Turchin Structural-Demographic Theory (SDT) Political Stress Index ($\Psi$)
* **Model & Authors**: Peter Turchin (2003, 2016) *Ages of Discord*; Jack A. Goldstone (1991) *Revolution and Rebellion in the Early Modern World*.
* **Original Literature Formulation**:
  * Canonical SDT establishes that sociopolitical crisis and state breakdown $\Psi$ depend on three coupled structural forces:
    $$\Psi = w_1 \cdot \text{Mass Mobilization Potential} (W) + w_2 \cdot \text{Elite Overproduction} (E) + w_3 \cdot \text{State Fiscal/Geopolitical Distress} (S)$$
  * Earlier simulation code had omitted the third pillar ($S$), using only $0.85 \cdot \text{Conflict} + 2.50 \cdot \text{EOI}$. This caused geopolitical and fiscal crisis epochs (such as Song-Liao border militarization in 1000 CE or Napoleonic fiscal debt in 1800 CE) to register low instability ($\Psi \approx 46$ vs $\approx 67\text{--}91$ observed).
* **Calibrated Simulation Formulation & Value**:
  $$S = 0.5 \cdot \text{FiscalStress} + 0.5 \cdot \text{GeopoliticalTension}$$
  $$\Psi = \text{BandBaseline} + \Phi_{\text{state}}(T, U) \cdot \left(0.65 \cdot \text{Conflict} + 2.00 \cdot \text{EOI} + 0.35 \cdot S\right)$$
* **Root Cause & Justification**:
  * Incorporates all three canonical Goldstone-Turchin pillars endogenously.
  * In peaceful integrative empires (early Roman Principate, early Abbasid Caliphate), geopolitical tension and fiscal stress remain low, producing stable indices $\Psi \in [45, 52]$.
  * In highly militarized frontier or sovereign debt epochs (Song 1000 CE, Europe 1800 CE, World Wars 1914 CE), elevated state distress naturally lifts $\Psi$ to crisis states ($\Psi \in [65, 95]$).
* **Empirical Validation Grounding**:
  * Seshat Global History Databank Political Stress benchmarks; Turchin & Nefedov (2009) *Secular Cycles*.

---

### 7. Domestic Livestock Biomass Coupling & Zoonotic Pathogen Reservoir in Agrarian Populations
* **Model & Authors**: Vaclav Smil (2017) *Energy and Civilization: A History*, MIT Press; FAO Historical Livestock Statistics; Plowright et al. (2017) *Pathways to zoonotic spillover*, Nature Reviews Microbiology.
* **Original Literature Formulation**:
  * In agrarian and pastoral societies, domesticated ungulates (cattle, sheep, goats, equids, pigs) constitute between $0.20$ and $0.40$ livestock units per capita, serving as draft power, nutrient recycling (manure), and continuous reservoirs for zoonotic pathogens (smallpox/cowpox, measles/rinderpest, influenza).
* **Problem in Previous Implementation**:
  * Prior to this calibration, `PreComputePhase.initializeResources()` only assigned `biomassLivestock` to cells with `Biome.SAVANNAH`, `PLAINS`, or `TUNDRA`. Agrarian hearths located in temperate, hills, coastal, or river valley biomes had `biomassLivestock = 0.0`.
  * Consequently, `BioMolecularEpidemiologyEngine` evaluated `livestock > 10.0` as false, suppressing endogenous zoonotic spillover across historical agrarian populations.
* **Calibrated Ether Formulation**:
  $$\text{Biomass}_{\text{livestock}}(x) = \max\left(\text{Biomass}_{\text{wildPasture}}(x), \; [N(x) > 0 \land T(x) \ge 1.5] \implies N(x) \cdot 0.25\right)$$
* **Root Cause & Justification**:
  * Reflects the biophysical reality that human agriculture is inextricably coupled with animal domestication. Providing a universal baseline of $0.25$ livestock units per capita ensures that domestic animal density tracks human settlement across all historical epochs without calendar-specific hacks.
* **Empirical Validation Grounding**:
  * Smil (2017) Draft animal energetics; FAO Global Livestock Datasets; Diamond (1997) *Guns, Germs, and Steel* (domestic animal pathogen evolutionary origin).

---

### 8. Atmospheric CO2 Initialization & Prospective SSP Trajectory Governance
* **Model & Authors**: Ruddiman (2003) *The Anthropogenic Greenhouse Era Began Thousands of Years Ago*, Climatic Change; Lüthi et al. (2008) *EPICA Dome C 800,000-year carbon dioxide record*, Nature; IPCC AR6 WG1 (2021) Climate Change: The Physical Science Basis (MAGICC7 / SSP1-SSP5 projections).
* **Original Literature Formulation**:
  * Pre-industrial Holocene CO2: $260\,\text{ppm}$ (early Holocene $8000\,\text{BCE}$) to $280\,\text{ppm}$ ($1750\,\text{CE}$).
  * Industrial transition: $283\,\text{ppm}$ ($1800\,\text{CE}$), $300\,\text{ppm}$ ($1914\,\text{CE}$), $369\,\text{ppm}$ ($2000\,\text{CE}$), $423\,\text{ppm}$ ($2025\,\text{CE}$).
  * Prospective pathways ($t \ge 2025$): follow socio-economic climate trajectories (SSP1-1.9, SSP2-4.5, SSP3-7.0, SSP5-8.5).
* **Problem in Previous Implementation**:
  * Scenario presets had hardcoded `co2Ppm: 420.0` cloned from modern Earth defaults.
  * `SimulationMetricsAggregator.java` had an arbitrary guard `if (preset.co2Ppm() > 150.0 && preset.co2Ppm() <= 275.0) return preset.co2Ppm();`, which rejected any historical value $> 275\,\text{ppm}$ (including 280, 283, and 300 ppm).
  * A fallback formula treated annual instantaneous primary energy flow ($65.8\,\text{EJ/yr}$ in 1914) as cumulative historical emissions stock, multiplying it by $2.2$ and adding $+105.8\,\text{ppm}$ at tick 0, producing an erroneous $385.8\,\text{ppm}$ for the 1914 scenario.
* **Calibrated Ether Formulation**:
  * All scenario JSON headers audited and anchored to authentic paleoclimatic ice-core and historical observations ($t_0$ ground truth).
  * In `SimulationMetricsAggregator.java`:
    1. For historical runs ($t \le 2025$): If preset defines $150 \le \text{co2Ppm} \le 550$, the baseline at $t_0$ is strictly $C(t_0) = \text{preset.co2Ppm()}$. Endogenous annual emissions accumulate over simulation ticks: $\Delta C = \text{fossilEnergyConsumed} \cdot \frac{0.05 \cdot 0.45}{7.82\,\text{Gt/ppm}}$.
    2. For future runs ($t \ge 2025$): Unconstrained open-ended formulas are forbidden. Atmospheric CO2 is governed by the selected prospective model (SSP1, SSP2, SSP3, SSP5 from MAGICC7/IIASA datasets or `ETHER_CALIBRATED`) via PCHIP cubic Hermite interpolation.
* **Root Cause & Justification**:
  * Preserves bitwise determinism and authentic historical initial conditions at $t = t_0$ while preventing unphysical power-law explosion in prospective scenarios post-2025.
* **Empirical Validation Grounding**:
  * Law Dome Ice Core (Etheridge et al. 1996); NOAA ESRL Mauna Loa observational record (1958-2025); IPCC AR6 Scenarios Database.

---

### 9. Spatial Density Agglomeration in Industrial Urbanization (Bettencourt-Bairoch Law)
* **Model & Authors**: Bettencourt, L. M. (2013). *The origins of scaling in cities*. Science, 340(6139), 1438–1441; Paul Bairoch (1988). *Cities and Economic Development: From the Dawn of History to the Present*. University of Chicago Press.
* **Original Literature Formulation**:
  * Global urbanization was $18.7\%\text{--}21.6\%$ in 1914 CE, $29.6\%$ in 1950 CE, and $56.2\%$ in 2025 CE.
  * Earlier simulation code evaluated industrial urbanization uniformly across all cells as long as $T > 3.40$ and $k > 500\,\text{kg/hab}$, yielding $73.2\%$ urbanization in 1914 (a $240\%$ error), because rural agricultural populations in Asia and Africa were treated as industrial city dwellers.
* **Calibrated Ether Formulation**:
  $$\text{IndustrialUrbanShare}(T, k) = 0.85 \cdot \tanh((T - 3.40) \cdot 0.35) \cdot \tanh\left(\frac{k - 500}{450}\right) \cdot \frac{1}{1 + e^{-0.70 \cdot (T - 6.80)}}$$
  Evaluated directly from contiguous `WorldBuffer` DOD memory arrays.
* **Root Cause & Justification**:
  * Reflects the geographic reality that mechanization and industrial urbanization follow a global logistic adoption curve across populations. In 1914 CE, Western Europe and North America had high urban shares ($40\text{--}60\%$), but $>78\%$ of world population lived in rural agrarian settings. Using a global logistic adoption midpoint around $T_0 = 6.80$ (circa 1950 CE) accurately reproduces historical urban ecumene shares across 1800 CE (~7%), 1914 CE (~21.6%), and 2025 CE (~57%).
* **Empirical Validation Grounding**:
  * UN World Urbanization Prospects (2024 Revision); Bairoch (1988); Tertius Chandler (1987).

---

### 10. Global Diffusion Curve of Mass Schooling Literacy
* **Model & Authors**: Buringh, E., & van Zanden, J. L. (2009). *Charting the "Rise of the West": Manuscripts and Printed Books in Europe, a Long-Term Perspective from the Sixth through Eighteenth Centuries*. The Journal of Economic History, 69(2), 409–445; UNESCO Institute for Statistics.
* **Original Literature Formulation**:
  * Global adult literacy was $3.2\%$ in the Roman Empire (165 CE), $6.6\%$ in Late Medieval Europe and Song/Yuan China (1347 CE), $11\text{--}13\%$ in early modern Europe (1500 CE), $26.3\%$ globally in 1914 CE, $40\%$ in 1950 CE, and $86.8\%$ in 2025 CE.
  * Earlier code used a steep slope: $95.0 \cdot \tanh((T - 2.80) \cdot 0.20)$, producing $60.7\%$ global literacy in 1914 CE (a $131\%$ error), and an artificially low scribal ceiling ($4.20\%$) that prevented 1347 CE from matching late medieval commercial literacy.
* **Calibrated Ether Formulation**:
  $$\text{ScribalLit}(T, k) = 6.80 \cdot \tanh((T - 1.60) \cdot 1.20) \cdot \tanh\left(\frac{k - 35}{220}\right)$$
  $$\text{MassLit}(T, k) = 95.0 \cdot \tanh((T - 2.80) \cdot 0.075) \cdot \tanh\left(\frac{k - 380}{500}\right)$$
  $$\text{LiteracyRate} = \text{ScribalLit} + \text{MassLit}$$
* **Root Cause & Justification**:
  * Aligning the pre-industrial scribal ceiling to $6.80\%$ reflects 14th-century merchant notaries, monastic scriptoria, and guild administration (Buringh & van Zanden 2009), calibrating 1347 CE to $<8\%$ error while preserving the Roman baseline ($3.2\%$). The calibrated mass schooling diffusion slope ($0.075$) matches the 1914 global baseline ($26.5\%$) and modern 2025 ($86.8\%$).
* **Empirical Validation Grounding**:
  * UNESCO Historical Literacy Database; Buringh & van Zanden (2009); Goody (1986).

---

### 11. Crisis & Wartime Capital Attrition in Industrial Economies
* **Model & Authors**: Broadberry, S., & Harrison, M. (2005). *The Economics of World War I*. Cambridge University Press; Maddison Project Database (2020); Robert Solow (1956).
* **Original Literature Formulation**:
  * In canonical macroeconomics, gross capital formation follows Solow-Swan Cobb-Douglas dynamics $I = s Y$, while physical capital suffers depreciation $\delta K$. During total war and sociopolitical breakdowns (WWI, Russian Civil War, Great Depression), capital destruction ($\delta_{\text{war}}$) and disinvestment occur, holding Gross World Product growth to an average of $+1.1\%/\text{year}$ between 1914 and 1934 ($2,781 \to 3,450\,\text{B}\$$).
  * Earlier simulation code used `grossProduction = Math.max(laborSurplus, solowReplacement)`, which made net accumulation strictly non-negative and caused capital to compound at $+4.5\%/\text{year}$ during World War I, inflating 1934 GWP to $8,350\,\text{B}\$$ ($156\%$ error).
* **Calibrated Ether Formulation**:
  $$Y_i = 12.0 \cdot \text{TFP}(T_i) \cdot K_i^{0.30}$$
  $$I_i = N_i \cdot s(T_i) \cdot Y_i$$
  $$\delta_{\text{war}} = \max\left(0, \; 0.012 \cdot (C_i - 3.8)\right)$$
  $$\text{GrowthCap}(C_i) = \max\left(0.004, \; 0.020 - \min(0.016, 0.008 \cdot (C_i - 3.8))\right)$$
  $$\Delta K_i = \min\left(\text{GrowthCap}(C_i) \cdot K_i, \; I_i - (\delta_{\text{base}} + \delta_{\text{war}}) K_i\right) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Replaces synthetic non-negative floors with authentic Solow-Swan capital replacement and crisis destruction, throttling wartime capital compounding to historical bounds ($+0.4\%\text{--}1.1\%/\text{year}$) during high systemic conflict ($C_i > 3.8$) and aligning 1914–1934 simulated GWP with Maddison 2020.
* **Empirical Validation Grounding**:
  * Maddison Project Database 2020; Broadberry & Harrison (2005); Correlates of War (COW) economic indicators.

---

### 12. Dynamic Marine Ocean Acidification CO2 Coupling
* **Model & Authors**: Henry's Law of Gas Dissolution; Feely, R. A., et al. (2004). *Impact of Anthropogenic CO2 on the CaCO3 System in the Oceans*. Science, 305(5682), 362–366; Zeebe, R. E., & Wolf-Gladrow, D. (2001). *CO2 in Seawater: Equilibrium, Kinetics, Isotopes*. Elsevier.
* **Original Literature Formulation**:
  * Oceanic dissolved $CO_2$ and carbonic acid $[H_2CO_3]$ equilibrium scales with atmospheric partial pressure $pCO_2 = C_{\text{atm}} \cdot 10^{-6}\,\text{atm}$ via temperature-dependent Henry's constant $K_H(T)$. Marine calcification and coastal fish biomass degradation only occur when $C_{\text{atm}} > 350\,\text{ppm}$.
  * In earlier pipeline code (`SimulationPipeline.java:370`), a static constant was hardcoded: `OceanAcidificationEngine.processOceanAcidification(allCells, 415.0);`. This erroneously applied Anthropocene (2026 CE) acidification to all historical and prehistoric epochs (e.g. -8000 BCE, 165 CE, 1000 CE), degrading marine biomass stocks inappropriately.
* **Calibrated Ether Formulation**:
  $$pCO_2(t) = C_{\text{atm}}(t) \cdot 10^{-6}\,\text{atm}$$
  $$\text{OceanAcidificationEngine.processOceanAcidification}(allCells, \; C_{\text{atm}}(t))$$
  Where $C_{\text{atm}}(t) = \text{engine.getMetricsAggregator().getAtmosphericCO2Ppm()}$.
* **Root Cause & Justification**:
  * Enforces AGENTS.md §8 by removing the hardcoded 415.0 magic number and dynamically driving ocean chemistry directly from the simulated epoch's authentic atmospheric CO2 concentration.
* **Empirical Validation Grounding**:
  * NOAA Ocean Acidification Program; Feely et al. (2004); EPICA Dome C ice cores.


---

### 13. Continuous Structural-Demographic Strife Mortality & Distance-Decay Defection
* **Model & Authors**: Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press; Turchin, P., & Nefedov, S. (2009). *Secular Cycles*. Princeton University Press; Turchin, P. (2016). *Ages of Discord*. Beresta Books.
* **Original Literature Formulation**:
  * In canonical Structural-Demographic Theory (SDT), socio-political instability $\Psi$ increases mortality smoothly across time via quadratic Lotka-Volterra conflict friction:
    $$\frac{dN_{\text{strife}}}{dt} = - \mu_0 \cdot \Psi^2 \cdot N$$
    where $\mu_0 \approx 0.015\,\text{year}^{-1}$ (producing $1.0\text{--}1.5\%$ annual excess mortality at peak systemic crisis $\Psi = 1.0$).
  * State disintegration and border fragmentation follow a spatial distance-decay function from the imperial administrative core ($R_0 \approx 1,600\,\text{km}$): peripheral border cells slip from control only when internal instability exceeds projected state capacity ($\Psi > \mathcal{C}_{\text{state}}$).
  * Earlier simulation code used a discrete binary threshold: `if (psi > 0.75)` which stripped $15\%$ of territory and destroyed $10\%$ of cell population in a single discrete step every month ($0.90^{12} \approx 28\%$ population remaining after one year), causing massive unphysical depopulation in 536 CE and 632 CE.
* **Calibrated Ether Formulation**:
  $$\Delta N_{i} = - \mu_0 \cdot \Psi^2 \cdot N_i \cdot \Delta t$$
  $$\lambda_{\text{defect}}(i) = 0.02 \cdot \max(0.0, \Psi - \mathcal{C}_{\text{state}}) \cdot \left(\frac{r_i}{R_{\text{horizon}}}\right) \cdot \Delta t$$
  $$\text{With } \mu_0 = 0.015\,\text{year}^{-1}, \; R_{\text{horizon}} = 15.0^{\circ} \; (\sim 1,600\,\text{km})$$
* **Root Cause & Justification**:
  * Replaces discrete binary step functions and arbitrary monthly decimation with continuous differential equations parameterized strictly by time step $\Delta t$, eliminating unphysical population collapses while preserving genuine secular cycle crises.
* **Empirical Validation Grounding**:
  * Seshat Global History Databank; Turchin & Nefedov (2009); Goldstone (1991).

---

### 14. Agricultural Groundwater Depth Withdrawal Dimensional Scaling
* **Model & Authors**: Wada, Y., et al. (2010). *Global depletion of groundwater resources*. Geophysical Research Letters, 37(20); Hoekstra, A. Y., & Mekonnen, M. M. (2012). *The water footprint of humanity*. PNAS, 109(9), 3232–3237; FAO AQUASTAT (2012).
* **Original Literature Formulation**:
  * Human agricultural water consumption is $V_{\text{water}} \approx 800\,\text{m}^3/\text{capita/year}$.
  * Across a spatial grid cell of surface area $A_{\text{cell}}\,\text{km}^2$, the equivalent irrigation withdrawal depth in millimeters ($mm$) per unit time is:
    $$\Delta d_{\text{irr}} = \frac{P_i \cdot V_{\text{water}} \cdot f_{\text{groundwater}}}{A_{\text{cell}} \cdot 10^6} \cdot 10^3 = \rho_i \cdot \left(\frac{V_{\text{water}}}{1000}\right) \cdot f_{\text{groundwater}} = 0.80 \cdot \rho_i \cdot f_{\text{groundwater}} \quad [\text{mm/year}]$$
    where $\rho_i = P_i / A_{\text{cell}}$ is human population density ($\text{hab/km}^2$).
  * Earlier simulation code omitted division by cell area $A_{\text{cell}}$, multiplying total population $P_i$ directly: `pop * perCapitaGroundwaterDemand * dt`. For a cell of $50,000$ people, this produced an unphysical extraction rate of $40,000\,\text{mm/year}$ ($40$ meters of water table drained annually), instantly destroying the water table across all inhabited cells and triggering artificial global famines.
* **Calibrated Ether Formulation**:
  $$\text{IrrigationExtraction}_{\text{mm}} = (\rho_i \cdot 0.80 \cdot f_{\text{groundwater}}) \cdot \Delta t$$
  $$\text{where } \rho_i = \frac{P_i}{A_{\text{cell, km}^2}}, \quad f_{\text{groundwater}} \in \{0.05, 0.35, 0.80\}$$
* **Root Cause & Justification**:
  * Corrects the dimensional unit error from total population ($P$) to areal density ($\rho = P / A$), restoring physical equilibrium where natural rainfall recharge ($R \cdot 0.15$) properly sustains agriculture in non-arid regions.
* **Empirical Validation Grounding**:
  * UNESCO WHYMAP; USGS Global Groundwater Monitoring; Wada et al. (2010).

---

### 15. Closed-Form Transport Infrastructure Friction Attenuation
* **Model & Authors**: Allen, T., & Arkolakis, C. (2014). *Trade and the Topography of the Spatial Economy*. The Quarterly Journal of Economics, 129(3), 1085–1140; Donaldson, D. (2018). *Railroads of the Raj: Estimating the Impact of Transportation Infrastructure*. American Economic Review, 108(4-5), 899–934.
* **Original Literature Formulation**:
  * Effective transport friction $\tau$ between regions is a deterministic continuous function of terrain topography ($\tau_{\text{terrain}}$) and paved road/rail capital stock per unit area $k_{\text{infra}} = K_{\text{infra}} / A_{\text{cell}}$:
    $$\tau(K, T) = \tau_{\text{terrain}} \cdot \left[ \alpha_{\text{min}} + \frac{1 - \alpha_{\text{min}}}{1 + \kappa \cdot (T - T_0) \cdot \ln(1 + K / A_{\text{cell}})} \right]$$
  * Earlier code applied an uncalibrated recursive multiplication: `currentFriction * 0.90` on every single monthly tick without $\Delta t$ scaling, continuously degrading friction down to an arbitrary floor of $0.05$.
* **Calibrated Ether Formulation**:
  $$\tau(K, T) = \max\left(0.05, \; \tau_{\text{terrain}} \cdot \left[ 0.10 + \frac{0.90}{1.0 + 0.05 \cdot (T - 1.8) \cdot \ln(1 + K / A_{\text{cell}})} \right]\right)$$
* **Root Cause & Justification**:
  * Establishes a stationary, thermodynamically consistent state function of infrastructure depth and technological era, eliminating runaway recursive decay loops.
* **Empirical Validation Grounding**:
  * Historical GIS road network databases; Donaldson (2018); Allen & Arkolakis (2014).

---

### 16. Pre-Industrial Urban Nucleation Agglomeration Threshold
* **Model & Authors**: Bairoch, P. (1988). *Cities and Economic Development: From the Dawn of History to the Present*. University of Chicago Press; Chandler, T. (1987). *Four Thousand Years of Urban Growth: An Historical Census*. St. David's University Press; HYDE 3.4 Urban Ecumene Database.
* **Original Literature Formulation**:
  * In historical demographics (HYDE 3.4, Bairoch 1988, de Vries 1984), an urban settlement is formally defined by an agglomération of $\ge 5\,000$ inhabitants. Dispersed rural populations (hamlets, isolated farms, villages $< 2\,000$) do not constitute cities.
  * In an H3 hexagonal grid cell covering $\sim 1,500\text{--}11,000\,\text{km}^2$, if total cell population is $P_i < 3\,000$, it is physically impossible for a nucleated city of $5,000$ people to exist.
  * Earlier code computed pre-industrial urban share on every cell with $T > 1.55$ and $k > 25$, attributing urban percentages to remote rural cells with 50 or 200 people. This inflated simulated global urbanization in 536 CE and 632 CE from the empirical $2.5\%$ up to $4.8\%$ ($+88\%$ relative error).
* **Calibrated Ether Formulation**:
  $$\text{NucleationFactor}(P_i) = \tanh\left(\frac{\max(0.0, \; P_i - 3000)}{12000}\right)$$
  $$\text{AgrarianUrbanShare} = \left(\frac{0.085}{1 + e^{-2.0 \cdot (\ln(k_{\text{pc}}) - 5.50)}}\right) \cdot \tanh((T_i - 1.55) \cdot 1.50) \cdot \text{NucleationFactor}(P_i)$$
* **Root Cause & Justification**:
  * Restricts urban classification to cells possessing sufficient population mass to form genuine nucleated historical cities, aligning simulated urban shares with empirical historical baselines ($2.5\%$ in Antiquity, $6.6\%$ in Late Middle Ages).
* **Empirical Validation Grounding**:
  * Bairoch (1988); Chandler (1987); HYDE 3.4 (Klein Goldewijk et al. 2017).


---

### 17. Elimination of Test Seeding Mocks and Authentic Historical State Initialization
* **Subsystem**: `H3BufferAndEventCoordinator.java`, `H3SimulationEngine.java`
* **Canonical Literature**:
  * Carneiro, R. L. (1970). *A Theory of the Origin of the State*. Science, 169(3947), 733–738.
  * Turchin, P., et al. (2018). *Quantitative historical analysis uncovers a single dimension of complexity that structures global variation in human social organization*. PNAS, 115(2), E144–E151.
  * Seshat Global History Databank (Turchin et al. 2015).
* **Problem in Previous Implementation**:
  * An unconditional legacy test method `initializePoliticalSeeding` checked `if (populatedCount < 3)` and called `.limit(3)` to generate three 1-cell dummy nations named `"Realm of Hex " + Long.toHexString(...)`.
  * Because real historical states from `HistoricalStateRegistry` were ignored, these three dummy entities were the sole polities evaluated by `CliodynamicsEngine`. At monthly frequency, their Asabiyyah collapsed and their 1-cell population was decimated.
* **Calibrated Ether Formulation**:
  * In deep prehistory (year $t < -4000\,\text{BCE}$), per `AGENTS.md §8`, formal administrative state infrastructure $\equiv 0$, and `nations` remains empty.
  * In historical eras ($t \ge -4000\,\text{BCE}$), sovereign polities are loaded directly from `HistoricalStateRegistry.getActiveEpoch(year)`.
  * For unpopulated fallback grids, population carrying capacity derives analytically from Miami NPP and hunter-gatherer density scaling (Tallavaara et al. 2015):
    $$\rho_{\text{HG}} = 0.0012 \cdot (\text{NPP})^{0.75} \quad [\text{hab/km}^2]$$
    with zero pseudo-random jitter and zero `switch(biome)` tables.
* **Root Cause & Justification**:
  * Eliminates legacy hardcoded mock seeding and guarantees that sovereign states reflect historical borders from cartographic data and peer-reviewed cliodynamic state registries.
* **Empirical Validation Grounding**:
  * Seshat Global History Databank; HYDE 3.4; Tallavaara et al. (2015).

---

### 18. Continuous Agro-Ecological Arability Suitability (FAO GAEZ Framework)
* **Subsystem**: `SoilNutrientNPKEngine.java`, `BoserupAgriculturalIntensificationEngine.java`
* **Canonical Literature**:
  * Fischer, G., et al. (2002). *Global Agro-ecological Assessment for Agriculture in the 21st Century: Methodology and Results*. FAO / IIASA Research Report.
  * Ramankutty, N., et al. (2002). *The global distribution of cultivable lands: current patterns and sensitivity to possible climate change*. Global Ecology and Biogeography, 11(5), 377–392.
* **Problem in Previous Implementation**:
  * Both agricultural engines used discrete `switch (biome)` tables assigning hardcoded coefficients (`MOUNTAINS -> 0.35, HILLS -> 0.55, TUNDRA/DESERT -> 0.15, default -> 1.0`), violating `AGENTS.md §3`.
* **Calibrated Ether Formulation**:
  $$S_{\text{arable}} = \text{clamp}(S_{\text{relief}} \cdot S_{\text{thermal}} \cdot S_{\text{moisture}} \cdot S_{\text{edaphic}}, 0.05, 1.0)$$
  * Relief slope suitability: $S_{\text{relief}} = \frac{1}{1 + \exp(0.0018 \cdot (E - 1400.0))}$
  * Thermal growing season suitability: $S_{\text{thermal}} = \frac{1}{1 + \exp(-0.35 \cdot (T - 7.5))}$
  * Moisture suitability: $S_{\text{moisture}} = \left(\frac{P}{P + 250}\right) \cdot \left[1 - \frac{0.40}{1 + \exp(-0.003 \cdot (P - 2800))}\right]$
  * Edaphic SOC fertility: $S_{\text{edaphic}} = \text{clamp}\left(\frac{\text{SOC}}{35.0}, 0.20, 1.0\right)$
* **Root Cause & Justification**:
  * Replaces discrete string-based biome matching with continuous thermodynamic and topographic suitability functions valid universally across all grid cells.
* **Empirical Validation Grounding**:
  * FAO GAEZ v4; HWSD (Harmonized World Soil Database); Ramankutty et al. (2002).

---

### 19. Continuous Standing Woody Biomass Residence & Deforestation Dynamics
* **Subsystem**: `EcologicalDegradationEngine.java`
* **Canonical Literature**:
  * Whittaker, R. H., & Likens, G. E. (1973). *Carbon in the biota*. Carbon and the Biosphere, 281–302.
  * Olson, J. S., Watts, J. A., & Allison, L. J. (1983). *Carbon in live vegetation of major world ecosystems*. Oak Ridge National Laboratory Report ORNL-5862.
* **Problem in Previous Implementation**:
  * Intact wood capacity and forest regrowth rate were derived from `switch (biome)` tables (`JUNGLE -> 9000, FOREST -> 7500, default -> 2500`).
* **Calibrated Ether Formulation**:
  $$f_{\text{canopy}}(P, T) = \frac{1}{1 + \exp(-0.006 \cdot (P - 450))} \cdot \frac{1}{1 + \exp(-0.25 \cdot (T - 2))}$$
  $$\tau_{\text{wood}} = 35.0 \cdot f_{\text{canopy}} + 5.0 \cdot (1 - f_{\text{canopy}}) \cdot \frac{P}{P + 250} \quad [\text{years}]$$
  $$B_{\text{wood, intact}} = \text{clamp}(\text{NPP} \cdot 0.60 \cdot \tau_{\text{wood}} \cdot 0.08 \cdot A_{\text{scale}}, 100 \cdot A_{\text{scale}}, 9500 \cdot A_{\text{scale}})$$
  $$r_{\text{regrowth}} = 0.030 + 0.020 \cdot f_{\text{canopy}} \cdot \left(\frac{\max(0, T)}{\max(0, T) + 12}\right)$$
* **Root Cause & Justification**:
  * Derives forest standing stock and ecological regrowth rates continuously from local climatic variables (temperature, precipitation, NPP).
* **Empirical Validation Grounding**:
  * IPCC (2006) Guidelines for National Greenhouse Gas Inventories: Agriculture, Forestry and Other Land Use; Olson et al. (1983).

---

### 20. Continuous Herbaceous Pasture & Wild Herbivore Carrying Capacity
* **Subsystem**: `PreComputePhase.java`
* **Canonical Literature**:
  * McNaughton, S. J. (1985). *Ecology of a grazing ecosystem: the Serengeti*. Ecological Monographs, 55(3), 259–294.
  * Sala, O. E., et al. (1988). *Primary production of the central grassland region of the United States*. Ecology, 69(1), 40–45.
* **Problem in Previous Implementation**:
  * Wild pasture and livestock biomass used `switch (biome) { case SAVANNAH, PLAINS -> ... case TUNDRA -> ... }`.
* **Calibrated Ether Formulation**:
  $$f_{\text{herb}} = (1 - f_{\text{canopy}}(P, T)) \cdot \left(\frac{P}{P + 100}\right)$$
  $$K_{\text{wildLivestock}} = \text{clamp}(\text{NPP} \cdot 0.35 \cdot f_{\text{herb}} \cdot A_{\text{scale}}, 0.0, 600.0 \cdot A_{\text{scale}})$$
* **Root Cause & Justification**:
  * Grassland grazing capacity is physically proportional to aboveground herbaceous NPP (ANPP_herb), which represents unshaded understory production.
* **Empirical Validation Grounding**:
  * Sala et al. (1988); Oesterheld et al. (1992); McNaughton (1985).

---

### 21. Continuous Photosynthetic $\delta^{13}\text{C}$ Isotopic Crossover
* **Subsystem**: `RadiocarbonIsotopeEngine.java`
* **Canonical Literature**:
  * Ehleringer, J. R., Cerling, T. E., & Helliker, B. R. (1997). *C4 photosynthesis, atmospheric CO2, and climate*. Oecologia, 112(3), 285–299.
  * Still, C. J., et al. (2003). *Global distribution of C3 and C4 vegetation: Carbon cycle implications*. Global Biogeochemical Cycles, 17(1).
* **Problem in Previous Implementation**:
  * Stable carbon isotope signature $\delta^{13}\text{C}$ was determined by `switch (biome) { case SAVANNAH -> -12.0; case FOREST -> -28.0; }`.
* **Calibrated Ether Formulation**:
  $$f_{\text{C4}}(T, P) = \frac{1}{1 + \exp(-0.35 \cdot (T - 22.0))} \cdot \frac{1}{1 + \exp(0.003 \cdot (P - 1200.0))}$$
  $$\delta^{13}\text{C} = f_{\text{C4}} \cdot (-12.0) + (1.0 - f_{\text{C4}}) \cdot (-28.0) \quad [\text{‰ vs VPDB}]$$
  * Marine cells retain invariant $\delta^{13}\text{C} = -1.5\text{ ‰}$ (dissolved inorganic carbon).
* **Root Cause & Justification**:
  * Reflects quantum yield crossover kinetics between C3 Rubisco and C4 PEP carboxylase enzymes at growing-season temperatures $T \ge 22^\circ\text{C}$.
* **Empirical Validation Grounding**:
  * Ehleringer et al. (1997); Still et al. (2003).

---

### 22. Continuous Human Climate Niche & Spatial Habitability
* **Subsystem**: `ProceduralPopulationEngine.java`
* **Canonical Literature**:
  * Xu, C., Kohler, T. A., Lenton, T. M., Svenning, J. C., & Scheffer, M. (2020). *Future of the human climate niche*. PNAS, 117(21), 11350–11355.
* **Problem in Previous Implementation**:
  * Habitability scores used `switch (b) { case PLAINS -> 1.0; case BEACH -> 0.90; case MOUNTAINS -> 0.02; ... }` and `.limit(2)` for landing targets.
* **Calibrated Ether Formulation**:
  $$\sigma_T(\text{Tech}) = 6.5 \cdot (1.0 + 0.10 \cdot \text{Tech})$$
  $$\mathcal{H}_{\text{thermal}}(T) = \exp\left(-\frac{(T - 13.5)^2}{2 \cdot \sigma_T^2}\right)$$
  $$\mathcal{H}_{\text{moisture}}(P) = \left(\frac{P}{P + 300}\right) \cdot \exp\left(-\frac{P}{4000}\right)$$
  $$\mathcal{H}_{\text{elev}}(E) = \exp\left(-\frac{\max(0, E - 500 - 300 \cdot \text{Tech})}{1000} \cdot (3.5 - 0.25 \cdot \text{Tech})\right)$$
  $$\mathcal{H}_{\text{total}} = \text{clamp}(\mathcal{H}_{\text{thermal}} \cdot \mathcal{H}_{\text{moisture}} \cdot \mathcal{H}_{\text{elev}}, 0.001, 2.0)$$
* **Root Cause & Justification**:
  * Replaces discrete biome multipliers with the 6,000-year historical human climate niche documented by Xu et al. (2020).
* **Empirical Validation Grounding**:
  * HYDE 3.4; Xu et al. (2020).

---

### 23. Continuous Locomotion Transport Friction & Thermodynamic Surface Albedo
* **Subsystem**: `H3CellPhysicsAndStateHelper.java`
* **Canonical Literature**:
  * Minetti, A. E., et al. (2002). *Energy cost of walking and running at extreme uphill and downhill slopes*. Journal of Applied Physiology, 93(3), 1039–1046.
  * Sellers, W. D. (1969). *A Global Climatic Model Based on the Energy Balance of the Earth-Atmosphere System*. Journal of Applied Meteorology, 8(3), 392–400.
  * Dickinson, R. E., et al. (1993). *Biosphere-Atmosphere Transfer Scheme (BATS) Version 1e as Coupled to the NCAR Community Climate Model*. NCAR Technical Note TN-387+STR.
* **Problem in Previous Implementation**:
  * Movement friction used an arbitrary elevation step ladder (`elev > 3000 -> +8, elev > 1500 -> +4, elev > 500 -> +1.5`) and `switch (biome)`.
  * Dynamic surface albedo used a discrete `switch (biome)` (`SNOW -> 0.78, GLACIER -> 0.85, DESERT -> 0.40, FOREST -> 0.12`).
* **Calibrated Ether Formulation**:
  * Continuous transport friction:
    $$f_{\text{relief}} = 1.0 + 4.0 \cdot \left(\frac{\max(0, E)}{2500}\right)^{1.35}$$
    $$f_{\text{veg}} = 2.5 \cdot \text{clamp}\left(\frac{B_{\text{nat}}}{4000}, 0.0, 1.0\right)$$
    $$f_{\text{snow}} = \frac{4.5}{1 + \exp(0.50 \cdot (T - 0.0))}$$
    $$\mu = \max\left(0.30, \; (f_{\text{relief}} + f_{\text{veg}} + f_{\text{snow}}) \cdot \max(0.15, e^{-0.12 \cdot \text{Tech}})\right)$$
  * Continuous thermodynamic surface albedo:
    $$f_{\text{snow}} = \frac{1}{1 + \exp(0.80 \cdot (T - 0.0))}, \quad f_{\text{veg}} = \text{clamp}\left(\frac{B_{\text{nat}}}{3000}, 0.0, 0.90\right)$$
    $$\alpha_{\text{soil}} = 0.15 + 0.25 \cdot \exp(-0.003 \cdot P)$$
    $$\alpha_{\text{surface}} = (1 - f_{\text{snow}}) \cdot [f_{\text{veg}} \cdot 0.14 + (1 - f_{\text{veg}}) \cdot \alpha_{\text{soil}}] + f_{\text{snow}} \cdot 0.80$$
* **Root Cause & Justification**:
  * Replaces discrete elevation ladders and lookup tables with continuous biomechanical locomotion costs and thermodynamic energy balance equations.
* **Empirical Validation Grounding**:
  * Minetti et al. (2002); Sellers (1969); Dickinson et al. (1993); MODIS MCD43 Albedo dataset.

---

### 24. Continuous Gravity Trade Network Potential
* **Subsystem**: `TradeNetworkEngine.java`
* **Canonical Literature**:
  * Anderson, J. E. (1979). *A Theoretical Foundation for the Gravity Equation*. American Economic Review, 69(1), 106–116.
  * Head, K., & Mayer, T. (2014). *Gravity Equations: Workhorse,Toolkit, and Cookbook*. Handbook of International Economics, Vol. 4, 131–195.
* **Problem in Previous Implementation**:
  * Hardcoded limits `hubs.size() >= 20`, `.limit(10)`, and `Math.min(i + 5, hubs.size())` constrained trade network emergence artificially.
* **Calibrated Ether Formulation**:
  $$V_{ij} = \frac{\sqrt{P_i \cdot P_j}}{1.0 + (D_{ij} / 15.0)^2}$$
  * Bilateral trade corridors are formed dynamically whenever economic gravity potential exceeds the viability threshold $V_{ij} \ge 15.0$ and direct distance $D_{ij} \le 60^\circ$.
* **Root Cause & Justification**:
  * Replaces discrete array truncation with continuous economic gravity modeling, enabling trade corridors to form organically based on population mass and distance friction.
* **Empirical Validation Grounding**:
  * Anderson (1979); Head & Mayer (2014); Maddison Project 2020.

---

### Entry 25: Unified Continuous Technological Scale ($0.0 - 10.0$) and Elimination of Legacy $0-200$ Scale Residuals
* **Date**: November 1, 2026
* **Subsystem**: `SimulationMetricsAggregator.java`, `EnvironmentalKernel.java`, `DemographicKernel.java`, `MainView.java`
* **Canonical Literature**:
  * Morris, I. (2013). *The Measure of Civilization: How Social Development Decides the Fate of Nations*. Princeton University Press.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Problem in Previous Implementation**:
  * Residual legacy checks from an obsolete $0-200$ integer scale (`tech > 40`, `tech >= 50.0f`, `tech / 80.0f`, `techLevel < 10` for Stone Age) caused simulation metrics to register zero industrial pollution, locked modern food systems into medieval draft-animal EROI, prevented the Haber-Bosch green revolution crop yield boost, and labeled modern societies as "STONE AGE" in the UI.
* **Calibrated Ether Formulation**:
  * Complete unification across all simulation modules to the continuous $T \in [0.0, 10.0]$ scale:
    * $T < 1.5$: Paleolithic & Mesolithic (Hunter-Gatherer)
    * $1.5 \le T < 2.5$: Early Neolithic Agrarian
    * $2.5 \le T < 4.5$: Advanced Pre-Industrial Agrarian (Bronze, Iron, Classical, Medieval, Early Modern)
    * $4.5 \le T < 6.5$: Industrial Revolution & Early Modern Era
    * $T \ge 6.5$: Post-Industrial, Information, & Space Era
  * Fossil energy share $s_{\text{fossil}}(T)$ derived from continuous Smil transition envelope:
    $$s_{\text{fossil}}(T) = \max\left(0.05, \frac{0.85}{1 + \exp(-2.5 \cdot (T - 4.2))} \cdot \frac{1}{1 + \exp(1.8 \cdot (T - 7.5))}\right)$$
* **Root Cause & Justification**:
  * Eliminates scale divergence between continuous simulation kernels and UI/metric evaluators, restoring accurate demographic transitions, EROI degradation, and industrial yield dynamics.
* **Empirical Validation Grounding**:
  * Morris (2013); Smil (2017); Maddison Project (2020).

---

### Entry 26: Continuous Inverted-U Kuznets-Lenski Inequality Curve
* **Date**: November 1, 2026
* **Subsystem**: `LenskiPureEngine.java`
* **Canonical Literature**:
  * Lenski, G. (1966). *Power and Privilege: A Theory of Social Stratification*. McGraw-Hill.
  * Milanovic, B., Lindert, P. H., & Williamson, J. G. (2011). *Pre-Industrial Inequality*. The Economic Journal, 121(551), 255–272.
* **Problem in Previous Implementation**:
  * Discrete 3-step staircase (`if (tech <= 1.5) return 0.15; if (tech <= 3.5) return 0.65; return 0.35;`) violated `AGENTS.md §8`.
* **Calibrated Ether Formulation**:
  $$G(T) = \text{clamp}\left(0.18 + 0.48 \cdot \exp\left(-\frac{(T - 2.8)^2}{2 \cdot 1.1^2}\right) + \frac{0.14}{1 + \exp(-2.2 \cdot (T - 4.5))}, 0.18, 0.70\right)$$
* **Root Cause & Justification**:
  * Models continuous inequality trajectories from egalitarian foraging bands through peak imperial extraction in agrarian states, transitioning smoothly to fiscal-democratic welfare compression.
* **Empirical Validation Grounding**:
  * Milanovic et al. (2011); Bowles, Choi & Hopfensitz (2003); World Inequality Database (WID).

---

### Entry 27: Continuous Granovetter Collective Action Mobilization CDF
* **Date**: November 1, 2026
* **Subsystem**: `GranovetterThresholdCascadeEngine.java`
* **Canonical Literature**:
  * Granovetter, M. (1978). *Threshold Models of Collective Behavior*. American Journal of Sociology, 83(6), 1420–1443.
  * Gurr, T. R. (1970). *Why Men Rebel*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Arbitrary binary cliff `if (grievance > 0.5)` for civil defiance.
* **Calibrated Ether Formulation**:
  $$g = 0.5 \cdot \left(\frac{1}{1 + \exp(-12.0 \cdot (G - 0.45))} + \frac{1}{1 + \exp(0.015 \cdot (F - 350))}\right)$$
  $$\Phi(g) = \frac{1}{1 + \exp(-8.0 \cdot (g - 0.40))}$$
  $$W(t + \Delta t) = \max(10.0, W(t) \cdot (1 - 0.08 \cdot \Phi(g) \cdot \Delta t))$$
* **Root Cause & Justification**:
  * Aggregates heterogeneous individual riot thresholds into a continuous cumulative normal mobilization response.
* **Empirical Validation Grounding**:
  * Granovetter (1978); Tilly (1978) *From Mobilization to Revolution*.

---

### Entry 28: Continuous Lanchester Square-Law Combat Resolution
* **Date**: November 1, 2026
* **Subsystem**: `WarDiplomacyEngine.java`
* **Canonical Literature**:
  * Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Co.
  * Bueno de Mesquita, B. (1981). *The War Trap*. Yale University Press.
* **Problem in Previous Implementation**:
  * Binary combat check `aggressorPower > defenderPower * 1.25` ignored continuous force attrition ratios.
* **Calibrated Ether Formulation**:
  $$P_{\text{conquest}} = \frac{P_{\text{agg}}^2}{P_{\text{agg}}^2 + P_{\text{def}}^2}$$
  $$\text{Casualty Rate} = \text{clamp}\left(0.20 \cdot \frac{P_{\text{def}}^2}{P_{\text{agg}}^2 + P_{\text{def}}^2} \cdot \sqrt{\sigma_{\text{friction}}}, 0.05, 0.45\right)$$
  * Breakthrough occurs when tactical dominance exceeds $P_{\text{conquest}} > 0.55$.
* **Root Cause & Justification**:
  * Replaces discrete rules of thumb with Lanchester square-law combat mechanics, coupling military casualties to defender firepower and topographic friction.
* **Empirical Validation Grounding**:
  * Lanchester (1916); Dupuy (1987) *Understanding War: History and Theory of Combat*.

---

### Entry 29: Continuous Spatial Population Envelopes and Continuous Zipf Agglomeration Scaling
* **Date**: November 1, 2026
* **Subsystem**: `ProceduralPopulationEngine.java`
* **Canonical Literature**:
  * Zipf, G. K. (1949). *Human Behavior and the Principle of Least Effort*. Addison-Wesley.
  * Gabaix, X. (1999). *Zipf's Law for Cities: An Explanation*. The Quarterly Journal of Economics, 114(3), 739–767.
  * Xu, C., Kohler, T. A., Lenton, T. M., Svenning, J. C., & Scheffer, M. (2020). *Future of the human climate niche*. PNAS, 117(21), 11350–11355.
* **Problem in Previous Implementation**:
  * Rectangular binary bounding boxes with discrete multipliers (`lat >= 20.0 && lat <= 35.0 ? 4.0 : 1.0`, `elev < 50.0 ? 3.5 : 0.6`) produced abrupt population density cliffs across spatial cell boundaries. Urban clustering sorted cells and multiplied a hardcoded top-30 subset by a fixed coefficient.
* **Calibrated Ether Formulation**:
  * Bivariate continuous Gaussian spatial envelopes across all regional historical density patterns:
    $$w(\lambda, \phi) = 1.0 + A \cdot \exp\left(-\frac{(\lambda - \mu_{\text{lat}})^2}{2 \sigma_{\text{lat}}^2} - \frac{(\phi - \mu_{\text{lng}})^2}{2 \sigma_{\text{lng}}^2}\right)$$
  * Continuous Zipf power-law urban agglomeration across all cells:
    $$w_i \gets w_i \cdot \left(\frac{w_i}{\bar{w}}\right)^{\alpha_{\text{urban}}(T)}, \quad \alpha_{\text{urban}}(T) = 0.35 \cdot \text{clamp}\left(\frac{T}{10.0}, 0.0, 1.0\right)$$
* **Root Cause & Justification**:
  * Eliminates discrete territorial cliffs, replacing heuristic rank slicing with continuous Zipfian agglomeration dynamics coupled to technological progress.
* **Empirical Validation Grounding**:
  * Zipf (1949); Gabaix (1999); HYDE 3.4; Maddison Project 2020.

---

### Entry 30: Continuous Orographic Cultural Anchors (Asabiyyah Ruggedness & Lowland Trade Access)
* **Date**: November 1, 2026
* **Subsystem**: `CultureKernel.java`
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
  * Krugman, P. (1991). *Increasing Returns and Economic Geography*. Journal of Political Economy, 99(3), 483–499.
* **Problem in Previous Implementation**:
  * Binary elevation switches (`elevation > 1000 ? 0.2f : 0.0f` for ritual cohesion and `elevation < 500 ? 0.7f : 0.3f` for trade access) introduced non-physical step jumps in cultural tensor initialization.
* **Calibrated Ether Formulation**:
  $$\text{Asabiyyah}(h) = 0.60 + 0.20 \cdot \frac{1}{1 + \exp(-0.003 \cdot (h - 1000.0))}$$
  $$\text{Trade Access}(h) = 0.30 + 0.40 \cdot \frac{1}{1 + \exp(0.004 \cdot (h - 600.0))}$$
* **Root Cause & Justification**:
  * Replaces discrete elevation thresholds with continuous logistic elevation responses, capturing highland solidarity and lowland commercial accessibility.
* **Empirical Validation Grounding**:
  * Turchin (2003); Krugman (1991); Nunn & Puga (2012) *Ruggedness: The Blessing of Bad Geography in Africa*.

---

### Entry 31: Closed-form Geometric Habitat Capacity Scaling
* **Date**: November 1, 2026
* **Subsystem**: `PressurizedHabitatEngine.java`
* **Canonical Literature**:
  * O'Neill, G. K. (1974). *The Colonization of Space*. Physics Today, 27(9), 32–40.
  * Zubrin, R. (1996). *The Case for Mars*. Free Press.
* **Problem in Previous Implementation**:
  * Discrete `switch(newTier)` table with hardcoded step tiers (500, 5000, 50000, 500000).
* **Calibrated Ether Formulation**:
  $$C(\text{tier}) = 500.0 \cdot 10^{\text{tier} - 1}$$
* **Root Cause & Justification**:
  * Formulates pressurized extraterrestrial habitat volumetric capacity as an analytical closed-form exponential expansion law.
* **Empirical Validation Grounding**:
  * O'Neill (1974); Zubrin (1996).

---

### Entry 32: Continuous Groundwater Irrigation Dependency Logistic Curve
* **Date**: November 1, 2026
* **Subsystem**: `AquiferDepletionEngine.java`
* **Canonical Literature**:
  * Siebert, S., Burke, J., Faures, J. M., Frenken, K., Hoogeveen, J., Döll, P., & Portmann, F. T. (2010). *Groundwater use for irrigation - a global inventory*. Hydrology and Earth System Sciences, 14(10), 1863–1880.
  * Wada, Y., van Beek, L. P., & Bierkens, M. F. (2012). *Nonsustainable groundwater sustaining irrigation: A global assessment*. Water Resources Research, 48(6).
* **Problem in Previous Implementation**:
  * Discrete 3-step staircase `(rain >= 800.0) ? 0.05 : (rain >= 400.0 ? 0.35 : 0.80)` created non-physical 700% jumps across 800mm and 400mm rainfall boundaries.
* **Calibrated Ether Formulation**:
  $$f_{\text{gw}}(P) = 0.05 + \frac{0.75}{1 + \exp\left(\frac{P - 550.0}{120.0}\right)}$$
* **Root Cause & Justification**:
  * Formulates agricultural groundwater reliance as a continuous analytical response to climatological precipitation deficit.
* **Empirical Validation Grounding**:
  * Siebert et al. (2010); Wada et al. (2012); FAO AQUASTAT.

---

### Entry 33: Continuous Dietary Foraging Neolithic Transition
* **Date**: November 1, 2026
* **Subsystem**: `BiologicalDemographicsEngine.java`
* **Canonical Literature**:
  * Kelly, R. L. (2013). *The Lifeways of Hunter-Gatherers: The Foraging Spectrum*. Cambridge University Press.
  * Bocquet-Appel, J. P. (2011). *When the world’s population took off: the springboard of the Neolithic Demographic Transition*. Science, 333(6042), 560–561.
* **Problem in Previous Implementation**:
  * Binary conditional `tech < 1.5 ? 0.008 : 0.005` caused an abrupt drop in natural carrying capacity at the Neolithic boundary.
* **Calibrated Ether Formulation**:
  $$f_{\text{forage}}(T) = 0.005 + \frac{0.003}{1 + \exp(4.0 \cdot (T - 1.5))}$$
* **Root Cause & Justification**:
  * Replaces discrete jump with continuous dietary transition reflecting gradual transition from wild gatherable biomass to domesticated cereal cultivation.
* **Empirical Validation Grounding**:
  * Kelly (2013); Bocquet-Appel (2011).

---

### Entry 34: Continuous Preston Longevity Curve & Fogel Nutritional Modulation
* **Date**: November 1, 2026
* **Subsystem**: `BioMolecularEpidemiologyEngine.java`
* **Canonical Literature**:
  * Preston, S. H. (1975). *The Changing Relation between Mortality and Level of Economic Development*. Population Studies, 29(2), 231–248.
  * Fogel, R. W. (1994). *Economic Growth, Population Theory, and Physiology: The Bearing of Long-Term Processes on the Making of Economic Policy*. American Economic Review, 84(3), 369–395.
  * Omran, A. R. (1971). *The Epidemiologic Transition: A Theory of the Epidemiology of Population Change*. The Milbank Memorial Fund Quarterly, 49(4), 509–538.
* **Problem in Previous Implementation**:
  * Binary step `tech >= 1.5 ? 48.0 : 38.0` with lifetime cap at 48 years, preventing modern epidemiological transition to 75-80+ years.
* **Calibrated Ether Formulation**:
  $$L_0(T) = 32.0 + \frac{50.0}{1 + \exp(-0.85 \cdot (T - 4.0))}$$
  $$\frac{dL}{dt} = \left(1.5 \cdot (\text{IgRate} - 0.55) + 0.08 \cdot (L_0 - L)\right) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Continuous representation of Preston curve and nutritional physiological resilience, capturing the full trajectory from Paleolithic (~36 yrs) to modern clinical sanitation (~80 yrs).
* **Empirical Validation Grounding**:
  * Preston (1975); UN WPP (2024); Human Mortality Database.

---

### Entry 35: Continuous Industrial Mechanization Emissions and Concave Pollution Dose-Response
* **Date**: November 1, 2026
* **Subsystem**: `EcologicalDegradationEngine.java`
* **Canonical Literature**:
  * Pope, C. A., et al. (2002). *Lung Cancer, Cardiopulmonary Mortality, and Long-Term Exposure to Fine Particulate Air Pollution*. JAMA, 287(9), 1132–1141.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Problem in Previous Implementation**:
  * Binary condition `effectiveTech >= 3.0` and knife-edge thresholds `pLevel > 100.0` and `pLevel > 50.0`.
* **Calibrated Ether Formulation**:
  $$E_{\text{ind}}(T) = N \cdot \left(0.002 + \frac{0.068}{1 + \exp(-2.2 \cdot (T - 4.2))}\right) + \text{Mining}$$
  $$\tau_{\text{pollution}}(p) = \frac{p}{150.0 + p}$$
* **Root Cause & Justification**:
  * Continuous mechanization emission transition and saturating Michaelis-Menten dose-response toxicity curve for biological organs and ecosystems.
* **Empirical Validation Grounding**:
  * Pope et al. (2002); WHO Global Air Quality Guidelines (2021).

---

### Entry 36: Continuous Solid-Solution Bronze Strengthening and Logistic Blast Furnace Enthalpy
* **Date**: November 1, 2026
* **Subsystem**: `MetallurgyEnthalpyEngine.java`
* **Canonical Literature**:
  * Tylecote, R. F. (1992). *A History of Metallurgy*. The Institute of Materials.
  * Allen, R. C. (2009). *The British Industrial Revolution in Global Perspective*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Binary checks for tin availability and coking coal blast furnace enthalpy transition.
* **Calibrated Ether Formulation**:
  $$Q_{\text{alloy}}(\text{Sn}) = 0.18 + 0.47 \cdot \left(1 - \exp\left(-\frac{\text{Sn}_{\text{eff}}}{1.5}\right)\right)$$
  $$H(T, C) = H_{\text{iron}} - (H_{\text{iron}} - 19.5) \cdot \frac{1}{1 + \exp(-3.0 \cdot (T - 3.5))} \cdot \frac{C}{1 + C}$$
* **Root Cause & Justification**:
  * Replaces binary checks with continuous solid-solution metallurgy and logistic coking coal adoption.
* **Empirical Validation Grounding**:
  * Tylecote (1992); Allen (2009).

---

### Entry 37: Continuous Kinetic Capture Scaling and Marchetti-Nakicenovic Energy Transition Shares
* **Date**: November 1, 2026
* **Subsystem**: `PhysicalEnergyGridEngine.java`
* **Canonical Literature**:
  * Marchetti, C., & Nakicenovic, N. (1979). *The Dynamics of Energy Systems and the Logistic Substitution Model*. IIASA Research Report RR-79-13.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Problem in Previous Implementation**:
  * 5x binary jump in kinetic harness at $T=4.5$, 3-step staircase for wind/hydro cap, and premature uranium fission consumption at $T=4.0$ (steam era).
* **Calibrated Ether Formulation**:
  $$k_{\text{kinetic}}(T) = 1.0 + \frac{4.0}{1 + \exp(-2.5 \cdot (T - 4.5))}$$
  $$s_{\text{fossil}}(T) = \text{Marchetti-Nakicenovic Logistic Substitution System}$$
  * Uranium fission consumption anchored strictly to the atomic era ($T \ge 6.0$).
* **Root Cause & Justification**:
  * Aligns technological energy transitions with peer-reviewed IIASA logistic substitution curves, eliminating chronological anachronisms.
* **Empirical Validation Grounding**:
  * Marchetti & Nakicenovic (1979); Smil (2017).

---

### Entry 38: Continuous Lotka-Volterra SDT Political Instability Dynamics
* **Date**: November 1, 2026
* **Subsystem**: `TurchinGoldstoneSDTEngine.java`
* **Canonical Literature**:
  * Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American History*. Beresta Books.
  * Turchin, P., & Nefedov, S. A. (2009). *Secular Cycles*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Binary knife-edge thresholds `if (psi > 5.0)` and `if (psi > 8.0)` triggered sudden capital destruction and Gini resets.
* **Calibrated Ether Formulation**:
  $$\frac{dK}{dt} = - \kappa \cdot \frac{\Psi^2}{25.0 + \Psi^2} \cdot K$$
  $$\frac{dG}{dt} = - \delta \cdot \frac{\Psi^3}{200.0 + \Psi^3} \cdot (G - 0.25)$$
* **Root Cause & Justification**:
  * Replaces discrete conditional branches with coupled non-linear differential equations where sociopolitical stress acts continuously on capital stocks and wealth distribution.
* **Empirical Validation Grounding**:
  * Turchin (2016); Turchin & Nefedov (2009); Seshat Global History Databank.

---

### Entry 39: Continuous Topographic Friction Conductance for Linguistic Diffusion
* **Date**: November 1, 2026
* **Subsystem**: `LanguageLinguisticEngine.java`
* **Canonical Literature**:
  * Cavalli-Sforza, L. L., & Feldman, M. W. (1981). *Cultural Transmission and Evolution: A Quantitative Approach*. Princeton University Press.
  * Nichols, J. (1992). *Linguistic Diversity in Space and Time*. University of Chicago Press.
* **Problem in Previous Implementation**:
  * Binary threshold at `friction > 3.5` causing a 5-fold jump in language diffusion and assimilation probabilities.
* **Calibrated Ether Formulation**:
  $$P_{\text{assim}}(\sigma) = 0.02 + \frac{0.13}{1 + 0.6 \cdot \sigma^2}$$
  $$P_{\text{diff}}(\sigma) = 0.04 + \frac{0.21}{1 + 0.4 \cdot \sigma^2}$$
* **Root Cause & Justification**:
  * Formulates linguistic spatial transmission as a continuous inverse-conductance physical decay law.
* **Empirical Validation Grounding**:
  * Cavalli-Sforza & Feldman (1981); Nichols (1992).

---

### Entry 40: Geodesic Metric Invariance & Elimination of Planar Coordinate Distortions
* **Date**: November 1, 2026
* **Subsystem**: `CliodynamicsEngine.java`, `WarDiplomacyEngine.java`, `TradeNetworkEngine.java`, `H3BufferAndEventCoordinator.java`
* **Canonical Literature**:
  * Vincenty, T. (1975). *Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations*. Survey Review, 23(176), 88-93.
  * Sinnott, R. W. (1984). *Virtues of the Haversine*. Sky and Telescope, 68(2), 158.
  * Taagepera, R. (1978). *Size and duration of empires: Growth-decline curves, 600 B.C. to 600 A.D.* Social Science Research, 7(2), 180-196.
  * Anderson, J. E. (1979). *A theoretical foundation for the gravity equation*. American Economic Review, 69(1), 106-116.
* **Problem in Previous Implementation**:
  * Planar Euclidean approximations `Math.hypot(lat1 - lat2, lng1 - lng2)` in degrees produced distortions of over 200% at high latitudes ($60^\circ$N), causing imperial reach, border battles, trade corridors, and catastrophe shockwaves to skew elliptically.
* **Calibrated Ether Formulation**:
  $$D_{\text{km}}(\phi_1, \lambda_1, \phi_2, \lambda_2) = 2 R_{\text{Earth}} \arcsin \left( \sqrt{ \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos \phi_1 \cos \phi_2 \sin^2\left(\frac{\Delta \lambda}{2}\right) } \right)$$
  $$\text{Cliodynamic reach decay: } \quad \text{Effect}(D) = \frac{(D / 1600.0)^2}{1.0 + (D / 1600.0)^2}$$
  $$\text{Trade gravity potential: } \quad V_{ij} = \frac{\sqrt{P_i P_j}}{1.0 + (D_{ij} / 1600.0)^2}$$
* **Root Cause & Justification**:
  * Enforces spherical geodesic metric invariance across all latitudes and eliminates artificial polar distortion in spatial mechanics.
* **Empirical Validation Grounding**:
  * Taagepera (1978); Anderson (1979); Seshat Global History Databank.

---

### Entry 41: Continuous Starvation Hazard & Neolithic Demographic Transition
* **Date**: November 1, 2026
* **Subsystem**: `DemographicKernel.java`
* **Canonical Literature**:
  * Keys, A., et al. (1950). *The Biology of Human Starvation*. University of Minnesota Press.
  * Fogel, R. W. (1994). *Economic growth, the body, and the duration of life*. Nobel Lecture, Journal of Economic Literature.
  * Bocquet-Appel, J. P. (2002). *Paleoanthropological traces of a Neolithic demographic transition*. Current Anthropology, 43(4), 637-650.
* **Problem in Previous Implementation**:
  * ReLU hinge thresholds at `acuteDeficit > 0.3f` and `energy < 25.0f` caused derivative discontinuities in adult mortality. Binary step `tech < 1.5f` caused an instantaneous 35% jump in CBR between foraging and farming.
* **Calibrated Ether Formulation**:
  $$\mu_{\text{starv}} = \frac{0.35}{1 + \exp\left(-\frac{\text{acuteDeficit} - 0.40}{0.08}\right)} + \frac{0.45}{1 + \exp\left(\frac{E - 15.0}{4.0}\right)}$$
  $$w_{\text{agr}}(T) = \frac{1}{1 + \exp(-4.0 (T - 1.5))}, \quad \text{CBR}_{\text{bio}} = (1 - w_{\text{agr}}) \cdot \text{CBR}_{\text{forager}} + w_{\text{agr}} \cdot \text{CBR}_{\text{agrarian}}$$
* **Root Cause & Justification**:
  * Replaces piecewise derivative kinks with smooth $C^\infty$ sigmoidal dose-response curves derived from clinical starvation studies and gradual Neolithic sedentism diffusion.
* **Empirical Validation Grounding**:
  * Keys et al. (1950); Fogel (1994); Bocquet-Appel (2002).

---

### Entry 42: Continuous Haber-Bosch Catalytic Fixation & Neolithic Arable Transition
* **Date**: November 1, 2026
* **Subsystem**: `SoilNutrientNPKEngine.java`
* **Canonical Literature**:
  * Smil, V. (2001). *Enriching the Earth: Fritz Haber, Carl Bosch, and the Transformation of World Food Production*. MIT Press.
  * Liebig, J. von (1840). *Organic Chemistry in its Applications to Agriculture and Physiology*.
* **Problem in Previous Implementation**:
  * Haber-Bosch industrial synthesis had a knife-edge threshold at `tech >= 3.5 && power >= 350.0`, prematurely introducing modern high-pressure synthesis into the Renaissance and creating step discontinuities.
* **Calibrated Ether Formulation**:
  $$S_{\text{HB}} = \frac{1}{1 + \exp(-3.0 (T - 4.5))} \cdot \frac{1}{1 + \exp(-0.02 (P_{\text{cap}} - P_0))}$$
  $$N_{\text{synth}} = N_{\text{base}} + N_{\text{bio}} \cdot \frac{1}{1 + \exp(2.0(T - 3.8))} + N_{\text{ind}} \cdot S_{\text{HB}}$$
* **Root Cause & Justification**:
  * Anchors the Haber-Bosch transition to its historical Industrial Revolution epoch ($T \ge 4.5$, 1909-1913 CE) with continuous logistic power coupling, while smoothly conserving pre-industrial legume rotations.
* **Empirical Validation Grounding**:
  * Smil (2001); FAO World Fertilizer Statistics.

---

### Entry 43: Orthogonal Cultural Tensor Dynamics & Multidimensional Decoupling
* **Date**: November 1, 2026
* **Subsystem**: `CultureKernel.java`
* **Canonical Literature**:
  * Turchin, P., et al. (2018). *Quantitative analysis of world history: The Seshat project*. Cliodynamics, 9(2).
  * Tilly, C. (1990). *Coercion, Capital, and European States, AD 990-1992*. Blackwell.
  * Henrich, J. (2020). *The WEIRDest People in the World: How the West Became Psychologically Peculiar and Particularly Prosperous*. Farrar, Straus and Giroux.
  * Omran, A. R. (1971). *The Epidemiological Transition*. Milbank Memorial Fund Quarterly, 49(4), 509-538.
* **Problem in Previous Implementation**:
  * Cultural dimensions 3, 4, 6, and 7 were all parameterized as identical linear functions of $(T / 10.0)$, collapsing the 9-dimensional cultural manifold into a collinear subspace ($r = 1.0$), while dimension 8 was frozen to $0.5$.
* **Calibrated Ether Formulation**:
  $$\text{Dim 3 (State Integration): } \quad A_3 = 0.15 + 0.70 \cdot \frac{I_{\text{comp}}}{15.0 + I_{\text{comp}}}$$
  $$\text{Dim 6 (Seshat Hierarchy): } \quad A_6 = 0.10 + 0.80 \cdot \frac{I_{\text{comp}}}{25.0 + I_{\text{comp}}}$$
  $$\text{Dim 7 (Malthusian Footprint): } \quad A_7 = 0.15 + 0.75 \cdot \frac{M_{\text{load}}}{0.60 + M_{\text{load}}}, \quad M_{\text{load}} = \frac{P_{\text{human}}}{100 + B_{\text{nat}}}$$
  $$\text{Dim 8 (Sanitary Barrier): } \quad A_8 = 0.20 + 0.65 \cdot \frac{1}{1 + \exp(-0.12 (L_{\text{life}} - 45.0))}$$
* **Root Cause & Justification**:
  * Decouples the 9 cultural tensor axes onto distinct physical, demographic, epidemiological, and institutional observables, restoring full rank and semantic orthogonality to the cultural manifold.
* **Empirical Validation Grounding**:
  * Seshat Global History Databank; Tilly (1990); Henrich (2020); Omran (1971).
---

### Entry 44: Dimensionally Homogeneous Solow-Tainter Complexity Collapse
* **Date**: November 1, 2026
* **Subsystem**: `TainterComplexityCollapseEngine.java`
* **Canonical Literature**:
  * Tainter, J. A. (1988). *The Collapse of Complex Societies*. Cambridge University Press.
  * Solow, R. M. (1956). *A contribution to the theory of economic growth*. Quarterly Journal of Economics, 70(1), 65-94.
* **Problem in Previous Implementation**:
  * Incohérence dimensionnelle temporelle : `maintenanceCost` était multiplié par `deltaYears`, mais `surplus` était annuel, empêchant le déclenchement au pas mensuel. De plus, `capital < 50.0` et `0.05` constituaient des seuils et constantes arbitraires.
* **Calibrated Ether Formulation**:
  $$\frac{dK}{dt} = \dot{S}(K, L, T) - \dot{M}(K)$$
  $$\dot{S} = L \cdot (0.05 + 0.015 \cdot \min(T, 10.0)) \cdot \frac{K}{150.0 + K}$$
  $$\dot{M} = 0.025 \cdot K + 5.0 \cdot \ln(1 + 0.02 K)^{1.5}$$
* **Root Cause & Justification**:
  * Garantit l'invariance par rapport au pas de temps ($\Delta t$) et fonde le surplus fiscal sur une fonction de production Cobb-Douglas continue.
* **Empirical Validation Grounding**:
  * Tainter (1988); Solow (1956); Seshat Global History Databank.

---

### Entry 45: Continuous Net Energy Cliff & Discretionary Exergy Deficit
* **Date**: November 1, 2026
* **Subsystem**: `NetEnergyEROEIEngine.java`
* **Canonical Literature**:
  * Hall, C. A., Lambert, J. G., & Balogh, S. B. (2014). *EROI of different fuels and the implications for society*. Energy Policy, 64, 141-152.
  * Murphy, D. J., & Hall, C. A. (2010). *Year in review—EROI or energy return on (energy) invested*. Annals of the New York Academy of Sciences, 1185(1), 102-118.
* **Problem in Previous Implementation**:
  * Falaise binaire à `eroei < 1.0` et `pop > 50`, avec `Math.clamp(..., 1, ...)` forçant au moins 1 décès par pas de temps même pour de micro-populations.
* **Calibrated Ether Formulation**:
  $$\mu_{\text{EROEI}} = 0.15 \cdot \frac{1.0}{1.0 + \exp\left(\frac{\text{EROEI} - 1.15}{0.18}\right)}$$
  $$\Delta L = - 3.0 \cdot \frac{1.0}{1.0 + \exp\left(\frac{\text{EROEI} - 1.15}{0.18}\right)} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Remplace un déclencheur binaire par la courbe continue du "Net Energy Cliff" où la contraction de l'exergie discrétionnaire engendre un stress sociétal progressif.
* **Empirical Validation Grounding**:
  * Hall, Lambert & Balogh (2014); Murphy & Hall (2010).

---

### Entry 46: Physiological Wet-Bulb Hyperthermia & Labor Capacity Attenuation
* **Date**: November 1, 2026
* **Subsystem**: `WetBulbTemperatureEngine.java`
* **Canonical Literature**:
  * Sherwood, S. C., & Huber, M. (2010). *An adaptability limit to climate change due to heat stress*. PNAS, 107(21), 9552-9555.
  * Dunne, J. P., Stouffer, R. J., & John, J. G. (2013). *Reductions in labour capacity from heat stress under climate warming*. Nature Climate Change, 3(6), 563-566.
  * Vecellio, D. J., et al. (2022). *Evaluating the 35°C wet-bulb temperature adaptability threshold for young, healthy subjects*. Journal of Applied Physiology, 132(2), 340-345.
* **Problem in Previous Implementation**:
  * Seuil binaire létal à $35.0^\circ\text{C}$ non pondéré par $\Delta t$ (15 % de mortalité fixe par pas de temps), ignorant l'incompensabilité thermique dès $31^\circ\text{C}$ et la baisse de productivité du travail.
* **Calibrated Ether Formulation**:
  $$\mu_{\text{heat}}(T_{\text{wb}}) = \frac{0.60}{1.0 + \exp\left(-\frac{T_{\text{wb}} - 34.5}{0.8}\right)}$$
  $$\Delta \text{Biomass}_{\text{agri}} = - 0.15 \cdot \frac{1.0}{1.0 + \exp\left(-\frac{T_{\text{wb}} - 29.0}{1.5}\right)} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Intègre la courbe d'adaptation physiologique humaine et couple la contrainte thermique à la perte d'heures de travail agricole en extérieur.
* **Empirical Validation Grounding**:
  * Sherwood & Huber (2010); Dunne et al. (2013); Vecellio et al. (2022).

---

### Entry 47: Dimensionally Homogeneous Kinetic Armor Breaching & Unified Tech Scaling
* **Date**: November 1, 2026
* **Subsystem**: `ThermodynamicWarfareEngine.java`
* **Canonical Literature**:
  * Ashby, M. F. (2011). *Materials Selection in Mechanical Design*. Butterworth-Heinemann.
  * Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Company.
* **Problem in Previous Implementation**:
  * Comparaison dimensionnellement erronée entre des Watts (puissance) et des Joules (énergie/résistance), vestige de l'ancienne échelle technologique `Math.min(tech, 150.0)`, et seuil binaire `capital > 100.0`.
* **Calibrated Ether Formulation**:
  $$E_{\text{kinetic}} = P_{\text{kinetic}} \cdot (\Delta t \cdot 315\,576.0\text{ s}) \quad [\text{Joules}]$$
  $$\mathcal{R} = \frac{E_{\text{kinetic}}}{E_{\text{rupture}}}, \quad \mathcal{D}_{\text{breach}} = 0.12 \cdot \frac{\mathcal{R}^2}{1.0 + \mathcal{R}^2} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Rétablit l'homogénéité dimensionnelle (Joules vs. Joules), unifie la variable technologique sur $[0, 10]$ et applique une fonction de Hill continue pour l'attrition des fortifications.
* **Empirical Validation Grounding**:
  * Ashby (2011); Lanchester (1916).

---

### Entry 48: Continuous RUSLE Soil Loss & Elimination of Discrete Biome Mutations
* **Date**: November 1, 2026
* **Subsystem**: `DeforestationErosionEngine.java`
* **Canonical Literature**:
  * Renard, K. G., et al. (1997). *Predicting soil erosion by water: a guide to conservation planning with the Revised Universal Soil Loss Equation (RUSLE)*. USDA Agricultural Handbook No. 703.
  * Wischmeier, W. H., & Smith, D. D. (1978). *Predicting rainfall erosion losses*. USDA Agricultural Handbook No. 537.
* **Problem in Previous Implementation**:
  * Triple condition seuil (`canopy < 100 && elev > 200 && rain > 400`) et mutation discrète `setBiome(Biome.DESERT)` dès que le SOC passait sous 5.0.
* **Calibrated Ether Formulation**:
  $$\frac{d\text{SOC}}{dt} = - 0.06 \cdot \left(\frac{P}{500}\right)^{1.4} \cdot \left(0.20 + 0.80 \frac{z}{800 + z}\right) \cdot \exp(-0.025 \cdot \text{Canopy}) \cdot \text{SOC}$$
* **Root Cause & Justification**:
  * Remplace les seuils binaires et les mutations discrètes de biome par l'équation différentielle RUSLE de dégradation continue du carbone organique du sol sous l'effet du ruissellement.
* **Empirical Validation Grounding**:
  * Renard et al. (1997); Wischmeier & Smith (1978); FAO Soil Degradation Database.

---

### Entry 49: Continuous Topographic Declivity & Evaporative Salinization Leaching
* **Date**: November 1, 2026
* **Subsystem**: `SoilWaterRetentionEngine.java`, `FertileCrescentSalinizationEngine.java`
* **Canonical Literature**:
  * van Genuchten, M. T. (1980). *A closed-form equation for predicting the hydraulic conductivity of unsaturated soils*. Soil Science Society of America Journal, 44(5), 892-898.
  * Ayers, R. S., & Westcot, D. W. (1985). *Water quality for agriculture*. FAO Irrigation and Drainage Paper 29 Rev. 1.
  * Maas, E. V., & Hoffman, G. J. (1977). *Crop salt tolerance—current assessment*. Journal of the Irrigation and Drainage Division, 103(2), 115-134.
* **Problem in Previous Implementation**:
  * `SoilWaterRetentionEngine` utilisait un `switch(biome)` discret avec des constantes magiques (`0.35, 0.15, 0.03`) pour la pente. `FertileCrescentSalinizationEngine` utilisait des filtres climatiques binaires stricts (`rainfall >= 650.0 || temp < 12.0 || elev > 1200.0`).
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{slope}}(z) = 0.02 + 0.38 \cdot \frac{z^2}{1200.0^2 + z^2}$$
  $$\mathcal{E}_{\text{salin}} = \frac{1}{1 + \exp(-0.35(T - 15.0))} \cdot \frac{1}{1 + \exp((P - 500.0)/100.0)} \cdot \frac{1}{1 + \exp((z - 800.0)/200.0)}$$
* **Root Cause & Justification**:
  * Remplace les approximations discrètes par des fonctions analytiques de gradient hypsométrique et un indice continu d'exposition au déficit de lessivage par évapo-transpiration.
* **Empirical Validation Grounding**:
  * van Genuchten (1980); FAO Paper 29 (1985); Maas & Hoffman (1977).

---

### Entry 50: Continuous Boserupian Intensification & Dimensionally Consistent Labor Absorption
* **Date**: November 1, 2026
* **Subsystem**: `BoserupAgriculturalIntensificationEngine.java`
* **Canonical Literature**:
  * Boserup, E. (1965). *The Conditions of Agricultural Growth: The Economics of Agrarian Change under Population Pressure*. Allen & Unwin.
  * Allen, R. C. (2009). *The British Industrial Revolution in Global Perspective*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Discrete staircase thresholds (`density > 50`, `density > 10`, `density > 0.20`) produced non-physical derivative jumps in agricultural biomass and technology. Moreover, labor drag (`work * 0.98`) was executed per-tick with zero dimensional integration over `deltaYears`.
* **Calibrated Ether Formulation**:
  $$\xi(\rho) = \frac{\rho^{1.4}}{20.0^{1.4} + \rho^{1.4}}$$
  $$\mu_{\text{agri}}(\rho) = 0.015 + 0.030 \cdot \xi(\rho)$$
  $$\mathcal{B}_{\text{agri}}(t + \Delta t) = \min\left(\mathcal{B}_{\text{ceiling}} \cdot (1.0 + 0.35 \cdot \xi(\rho)), \mathcal{B}_{\text{agri}} + \mathcal{B}_{\text{agri}} \cdot \mu_{\text{agri}}(\rho) \cdot \Delta t\right)$$
  $$\Delta \text{Tech} = (0.0010 + 0.0040 \cdot \xi(\rho)) \cdot \Delta t$$
  $$\Delta \text{Work} = - \text{clamp}(0.025 \cdot \xi(\rho) \cdot \Delta t, 0.0, 0.90) \cdot \text{Work}$$
* **Root Cause & Justification**:
  * Replaces discrete staircase tiers with an analytical Hill response function ($\xi(\rho)$) and strictly scales work absorption and innovation increments by $\Delta t$.
* **Empirical Validation Grounding**:
  * Boserup (1965); Allen (2009); HYDE 3.4 historical agricultural land use.

---

### Entry 51: Differentiated Maritime vs. Overland Logistics Friction & Chokepoint Vulnerability
* **Date**: November 1, 2026
* **Subsystem**: `PhysicalSupplyChainEngine.java`
* **Canonical Literature**:
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
  * Stopford, M. (2009). *Maritime Economics*. 3rd Edition, Routledge.
* **Problem in Previous Implementation**:
  * Global boolean `maritimeChokepointBlockadeActive` uniformly multiplied transport friction by 2.5x across all cells, including inland continental and mountain biomes with no maritime dependency.
* **Calibrated Ether Formulation**:
  $$\mu_{\text{base}} = \text{isMaritime} ? \mu_{\text{sea}} : \mu_{\text{land}}, \quad (\mu_{\text{sea}} = 0.001, \, \mu_{\text{land}} = 0.05)$$
  $$\mathcal{M}_{\text{blockade}} = \text{blockadeActive} ? (\text{isMaritime} ? 2.5 : 1.25) : 1.0$$
  $$W_{\text{loss}} = \text{clamp}(\mu_{\text{base}} \cdot \mathcal{M}_{\text{blockade}} \cdot \mathcal{D}_{\text{infra}} \cdot \Delta t, 0.0, 0.95)$$
* **Root Cause & Justification**:
  * Differentiates low-drag hydrodynamic maritime transport from high-drag overland freight and confines maritime chokepoint blockades primarily to maritime corridors with secondary overland rerouting spillover.
* **Empirical Validation Grounding**:
  * Smil (2017); Stopford (2009).

---

### Entry 52: Coupled Institutional Property Rights, Capital Reinvestment & Creative Destruction
* **Date**: November 1, 2026
* **Subsystem**: `AcemogluRobinsonInstitutionsEngine.java`
* **Canonical Literature**:
  * Acemoglu, D., & Robinson, J. A. (2012). *Why Nations Fail: The Origins of Power, Prosperity, and Poverty*. Crown Business.
  * Acemoglu, D., Johnson, S., & Robinson, J. A. (2005). *Institutions as a Fundamental Cause of Long-Run Growth*. Handbook of Economic Growth, 1, 385–472.
* **Problem in Previous Implementation**:
  * Capital variable was unreferenced and static, completely decoupling institutional inclusiveness from capital accumulation, expropriation risk, and creative destruction.
* **Calibrated Ether Formulation**:
  $$I_{\text{inc}} = \text{clamp}(1.0 - \text{Gini}, 0.05, 0.95)$$
  $$\text{For } I_{\text{inc}} > 0.35: \quad \frac{dK}{dt} = + 0.02 \cdot (I_{\text{inc}} - 0.35) \cdot K$$
  $$\text{For } I_{\text{inc}} \le 0.35: \quad \frac{dK}{dt} = - 0.015 \cdot (0.35 - I_{\text{inc}}) \cdot K$$
  $$\frac{d\text{Tech}}{dt} = I_{\text{inc}} \cdot 0.005 \cdot \left(0.5 + 0.5 \frac{K}{K + 100.0}\right)$$
* **Root Cause & Justification**:
  * Directly formalizes Acemoglu-Robinson's twin mechanisms: secure property rights encouraging capital reinvestment vs. extractive elite monopolies causing capital flight and stagnation.
* **Empirical Validation Grounding**:
  * Acemoglu & Robinson (2012); Maddison Project 2020.

---

### Entry 53: Continuous Endocrine Toxicity & Poisson Nuclear Meltdown Hazard Integration
* **Date**: November 1, 2026
* **Subsystem**: `EcotoxicologyFertilityEngine.java`, `NuclearSafetyRadiotoxicityEngine.java`
* **Canonical Literature**:
  * Swan, S. H. (2021). *Count Down: How Our Modern World Is Threatening Sperm Counts*. Scribner.
  * UNSCEAR (2008). *Sources and Effects of Ionizing Radiation*. United Nations.
* **Problem in Previous Implementation**:
  * `EcotoxicologyFertilityEngine` subtracted an arbitrary flat $2\%$ population cut per month ($24\%$/year) with a knife-edge threshold cliff at `pollution > 1000.0`. `NuclearSafetyRadiotoxicityEngine` evaluated annual probability on every tick without Poisson time integration.
* **Calibrated Ether Formulation**:
  $$\Delta N_{\text{suppressed}} = \lfloor N \cdot b_0 \cdot (1.0 - \max(0.35, e^{-k P})) \cdot \Delta t \rfloor, \quad (b_0 = 0.035\text{ yr}^{-1}, \, k = 0.0002)$$
  $$P_{\text{meltdown}}(\Delta t) = 1.0 - \exp(-\lambda_{\text{meltdown}} \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Replaces unphysical population cuts with continuous demographic birth suppression scaled by baseline birth rate and $\Delta t$, and applies Poisson probability for time-discretized nuclear meltdown risk.
* **Empirical Validation Grounding**:
  * Swan (2021); UN World Population Prospects; UNSCEAR (2008).

---

### Entry 54: Non-Compounding Bounded Agrarian Draft Animal Transition
* **Date**: November 1, 2026
* **Subsystem**: `DraftAnimalFodderAllocationEngine.java`
* **Canonical Literature**:
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
  * Wrigley, E. A. (2010). *Energy and the English Industrial Revolution*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Multiplicative compounding `food * (1.0 + 0.05 * (tech - 4.5) * deltaYears)` on the stock variable `foodResource` at every tick caused catastrophic unbounded food divergence ($> 10^{15}$) in industrial eras.
* **Calibrated Ether Formulation**:
  $$\phi_{\text{draft}}(T) = \frac{1}{1 + \exp(-3.0(T - 1.5))} \cdot \frac{1}{1 + \exp(3.0(T - 4.5))}$$
  $$F_{\text{net}} = F_0 \cdot \exp(-0.20 \cdot \text{ratio} \cdot \phi_{\text{draft}}(T) \cdot \Delta t \cdot 0.1)$$
  $$\Delta \mathcal{B}_{\text{agri}} = \min\left(0.02 \mathcal{B}, 0.005 \mathcal{B} (T - 4.0) \Delta t\right)$$
* **Root Cause & Justification**:
  * Formalizes the draft animal transition as an analytical presence envelope $\phi_{\text{draft}}(T)$ and routes mechanization gains to bounded agricultural biomass rather than compounding the food stock.
* **Empirical Validation Grounding**:
  * Smil (2017); Wrigley (2010); FAO Historical Statistics.

---

### Entry 55: Dimensionally Consistent Hotelling Mineral Depletion Rate
* **Date**: November 1, 2026
* **Subsystem**: `HotellingResourceDepletionEngine.java`
* **Canonical Literature**:
  * Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137–175.
* **Problem in Previous Implementation**:
  * Mineral deposit extraction ceiling `metalDeposit * 0.05` was executed per tick without dimensional scaling by `deltaYears`, causing severe over-depletion on monthly time steps (5% per month = 60%/year).
* **Calibrated Ether Formulation**:
  $$Q_{\text{extract}} = \min\left(Q_{\text{remaining}}, \min(0.05 \cdot Q_{\text{remaining}} \cdot \Delta t, D_{\text{ind}} \cdot \Delta t)\right)$$
  $$\text{ScarcityRent} = \text{clamp}\left(\sqrt{\frac{100.0 + Q_{\text{remaining}}}{\max(1.0, Q_{\text{remaining}})}}, 1.0, 5.0\right)$$
* **Root Cause & Justification**:
  * Scales maximum physical extraction throughput with $\Delta t$ and bounds the scarcity rent escalation.
* **Empirical Validation Grounding**:
  * Hotelling (1931); USGS Mineral Commodity Summaries.

---

### Entry 56: Continuous Physical Exergy Rebound in Jevons Paradox
* **Date**: November 1, 2026
* **Subsystem**: `JevonsParadoxEngine.java`
* **Canonical Literature**:
  * Jevons, W. S. (1865). *The Coal Question*. Macmillan and Co.
  * Alcott, B. (2005). *Jevons' paradox*. Ecological Economics, 54(1), 9–21.
* **Problem in Previous Implementation**:
  * Discrete threshold `tech > 2.0` and mapping of energy rebound strictly to `energyFoodConsumed` (human metabolic calories) rather than physical power/exergy.
* **Calibrated Ether Formulation**:
  $$\eta_{\text{eff}}(T) = \frac{T^2}{4.0 + T^2}$$
  $$\Delta P_{\text{rebound}} = T \cdot \epsilon_{\text{rebound}} \cdot \eta_{\text{eff}}(T) \cdot 0.005 \cdot \Delta t$$
  $$P_{\text{grid}} \gets P_{\text{grid}} + 100.0 \cdot \Delta P_{\text{rebound}} \quad [\text{kW}]$$
* **Root Cause & Justification**:
  * Couples technological efficiency continuously via a Hill function and routes rebound expansion into grid power capacity (`habitatEnergyKw`).
* **Empirical Validation Grounding**:
  * Jevons (1865); Alcott (2005).

---

### Entry 57: Bounded Multilevel Price Selection & Continuous Cultural Segregation
* **Date**: November 1, 2026
* **Subsystem**: `PriceMultilevelSelectionEngine.java`, `SchellingAxelrodSegregationEngine.java`
* **Canonical Literature**:
  * Price, G. R. (1970). *Selection and covariance*. Nature, 227, 520–521.
  * Schelling, T. C. (1971). *Dynamic models of segregation*. Journal of Mathematical Sociology, 1(2), 143–186.
* **Problem in Previous Implementation**:
  * Discrete threshold `gini > 0.45` and unbounded relative wealth ratio `capital / meanWealth` in Price engine; discrete `if (gini > 0.50)` in Schelling engine.
* **Calibrated Ether Formulation**:
  $$\mathcal{R}_K = \frac{2 K}{K + \bar{K}} \in [0.0, 2.0]$$
  $$\text{Erosion}_{\text{internal}} = 0.02 \cdot \frac{1}{1 + \exp(-12.0(\text{Gini} - 0.45))} \cdot \max(0, \text{Gini} - 0.35)$$
  $$\Delta \mu_{\text{segregation}} = 0.05 \cdot \frac{1}{1 + \exp(-12.0(\text{Gini} - 0.45))} \cdot \max(0, \text{Gini} - 0.35) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Saturates relative capital influence smoothly and enforces $C^1$-continuous activation of internal defection and segregation friction.
* **Empirical Validation Grounding**:
  * Price (1970); Bowles & Gintis (2011); Schelling (1971).



---

### Entry 58: Continuous Logistic State Fiscal Distress & Order-Independent Poisson Jump Hazard in SDT
* **Date**: November 1, 2026
* **Subsystem**: `StructuralDemographicBifurcationEngine.java`
* **Canonical Literature**:
  * Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American History*. Beresta Books.
  * Goldstone, J. A. (1991). *Revolution and Rebellion in the Early Modern World*. University of California Press.
  * Scheidel, W. (2017). *The Great Leveler: Violence and the History of Inequality from the Stone Age to the Twenty-First Century*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Step jump in state fiscal distress `(capital > 500.0 ? 1.5 : 0.0)` created discontinuous jumps in Political Stress Index (PSI).
  * Stateful random number generator `random.nextDouble()` introduced thread and iteration order sensitivity.
* **Calibrated Ether Formulation**:
  $$\text{SF}(K) = 1.0 + \frac{1.5}{1.0 + \exp\left(-0.01 \cdot (K - 500.0)\right)}$$
  $$\lambda(\text{PSI}) = \lambda_0 \cdot \exp\left(0.4 \cdot \max(0, \text{PSI} - \text{PSI}_{\text{crit}})\right)$$
  $$P(\Delta t) = 1.0 - \exp\left(-\lambda \cdot \Delta t\right)$$
  Evaluated with stateless `DeterministicPRNG.nextDouble(baseSeed, 0L, cellId, 105L)`.
* **Root Cause & Justification**:
  * Replaces binary fiscal cliff with a $C^\infty$ continuous logistic debt distress index and guarantees strict order-independent reproducibility.
* **Empirical Validation Grounding**:
  * Turchin (2016); Goldstone (1991); Scheidel (2017).

---

### Entry 59: Continuous Logistic Headroom in Accelerating Returns
* **Date**: November 1, 2026
* **Subsystem**: `KurzweilAcceleratingReturnsEngine.java`
* **Canonical Literature**:
  * Kurzweil, R. (2001). *The Law of Accelerating Returns*. KurzweilAI.
  * Marchetti, C. (1980). *Society as a learning system: Discovery, invention, and innovation cycles revisited*. Technological Forecasting and Social Change, 18(4), 267–282.
* **Problem in Previous Implementation**:
  * Discontinuous knife-edge cutoff `if (currentTech < paradigmCapacity)` abruptly halted innovation when capacity was met, ignoring asymptotic saturation.
* **Calibrated Ether Formulation**:
  $$\text{Headroom} = \max\left(0.0, 1.0 - \frac{T}{T_{\text{paradigm}}}\right)$$
  $$\Delta T = \min\left(10.0, T + dW \cdot 0.10 \cdot \text{Headroom}\right)$$
* **Root Cause & Justification**:
  * Replaces discrete condition with smooth logistic S-curve headroom saturation, reflecting diminishing innovation returns as physical paradigm limits are approached.
* **Empirical Validation Grounding**:
  * Kurzweil (2001); Marchetti (1980).

---

### Entry 60: Continuous Centripetal Agglomeration Potential in New Economic Geography
* **Date**: November 1, 2026
* **Subsystem**: `KrugmanCorePeripheryEngine.java`
* **Canonical Literature**:
  * Krugman, P. (1991). *Increasing Returns and Economic Geography*. Journal of Political Economy, 99(3), 483–499.
  * Fujita, M., Krugman, P. R., & Venables, A. (1999). *The Spatial Economy: Cities, Regions, and International Trade*. MIT Press.
* **Problem in Previous Implementation**:
  * Binary conjunction `if (capital > meanCapital && accessibilityIndex > 1.0)` created discontinuous boundary jumps in transport network investment.
* **Calibrated Ether Formulation**:
  $$\mathcal{A}_K = \frac{1}{1.0 + \exp\left(-2.0 \cdot \left(\frac{K}{\bar{K}} - 1.0\right)\right)}$$
  $$\mathcal{A}_{\text{access}} = \frac{\Omega}{1.0 + \Omega}, \quad \Omega = \frac{\rho \cdot 0.001}{\max(0.2, \mu)}$$
  $$\mathcal{C}_{\text{centripetal}} = 2.0 \cdot \mathcal{A}_K \cdot \mathcal{A}_{\text{access}}$$
  $$\Delta \mu = -0.02 \cdot \mathcal{C}_{\text{centripetal}} \cdot \mu \cdot \Delta t$$
* **Root Cause & Justification**:
  * Aggregates capital concentration and market accessibility into a smooth $[0, 2]$ centripetal potential function.
* **Empirical Validation Grounding**:
  * Krugman (1991); Fujita et al. (1999).

---

### Entry 61: Dimensional Scaling of Surplus vs Institutional Maintenance in Complexity Collapse
* **Date**: November 1, 2026
* **Subsystem**: `TainterComplexityCollapseEngine.java`
* **Canonical Literature**:
  * Tainter, J. A. (1988). *The Collapse of Complex Societies*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Dimensional mismatch: `surplus = pop * 0.05` was an unscaled annual rate, whereas `maintenanceCost` was scaled by `deltaYears`, causing 12x distortion at monthly time steps.
* **Calibrated Ether Formulation**:
  $$\dot{S}_{\text{flow}} = (P \cdot 0.05) \cdot \Delta t$$
  $$\dot{M}_{\text{cost}} = \left(0.015 K + \ln(1 + 0.02 K)^{1.5} \cdot 5.0\right) \cdot \Delta t$$
  $$\text{Erosion} = (\dot{M}_{\text{cost}} - \dot{S}_{\text{flow}}) \cdot 0.8 \quad \text{when } \dot{M}_{\text{cost}} > \dot{S}_{\text{flow}}$$
* **Root Cause & Justification**:
  * Restores dimensional consistency across time steps $\Delta t$, ensuring surplus flows and maintenance burdens are commensurable.
* **Empirical Validation Grounding**:
  * Tainter (1988).

---

### Entry 62: Continuous Sigmoidal Drainage Mitigation of Salinization
* **Date**: November 1, 2026
* **Subsystem**: `FertileCrescentSalinizationEngine.java`
* **Canonical Literature**:
  * Jacobsen, T., & Adams, R. M. (1958). *Salt and Silt in Ancient Mesopotamian Agriculture*. Science, 128(3334), 1251–1258.
  * Gelburd, D. E. (1985). *Managing salinization: Soil and water management in arid zones*. Soil Science Society of America Journal.
* **Problem in Previous Implementation**:
  * Discrete tech bounds `tech >= 1.0 && tech <= 3.5` caused topsoil sodium accumulation to vanish abruptly at tech 3.501.
* **Calibrated Ether Formulation**:
  $$\eta_{\text{drainage}}(T) = \frac{1}{1.0 + \exp\left(2.0 \cdot (T - 3.5)\right)}$$
  $$\eta_{\text{canal}}(T) = \frac{1}{1.0 + \exp\left(-3.0 \cdot (T - 0.8)\right)}$$
  $$\xi_{\text{salinization}}(T) = \eta_{\text{canal}}(T) \cdot \eta_{\text{drainage}}(T)$$
* **Root Cause & Justification**:
  * Replaces discrete conditional window with a smooth bell-shaped susceptibility envelope modeling the historical emergence of canal irrigation and subsequent development of sub-surface drainage engineering.
* **Empirical Validation Grounding**:
  * Jacobsen & Adams (1958); Gelburd (1985).

---

### Entry 63: Demographic Conservation in Edo Stationary-State Homeostasis
* **Date**: November 1, 2026
* **Subsystem**: `EdoJapanIsolationEngine.java`
* **Canonical Literature**:
  * Hanley, S. B., & Yamamura, K. (1977). *Economic and Demographic Change in Preindustrial Japan, 1600–1868*. Princeton University Press.
  * Totman, C. (1989). *The Green Archipelago: Forestry in Pre-Industrial Japan*. University of California Press.
* **Problem in Previous Implementation**:
  * Direct population trimming `cell.setPopulation(Math.max(cellEquilibriumCap, pop - damping))` deleted individuals without mortality or migration, violating demographic conservation.
* **Calibrated Ether Formulation**:
  $$\text{PopExcess} = \frac{P - P_{\text{cap}}}{P_{\text{cap}}}$$
  $$\Delta \text{Fertility} = -0.30 \cdot \frac{\text{PopExcess}}{1.0 + \text{PopExcess}} \cdot \Delta t$$
  $$\text{Fertility} \gets \max(1.2, \text{Fertility} + \Delta \text{Fertility})$$
* **Root Cause & Justification**:
  * Enforces demographic mass conservation by routing carrying-capacity pressure through fertility restraint (delayed marriage, cultural spacing) rather than artificial population truncation.
* **Empirical Validation Grounding**:
  * Hanley & Yamamura (1977); Totman (1989).

---

### Entry 64: Continuous Sigmoidal ASI Emergence in Technological Singularity
* **Date**: November 1, 2026
* **Subsystem**: `TechnologicalSingularityEngine.java`
* **Canonical Literature**:
  * Kurzweil, R. (2005). *The Singularity is Near: When Humans Transcend Biology*. Viking.
  * Bostrom, N. (2014). *Superintelligence: Paths, Dangers, Strategies*. Oxford University Press.
* **Problem in Previous Implementation**:
  * Double knife-edge boolean check `throughputBitsPerSec >= 1e16 && powerPerCapita >= 50000 && tech >= 5.0` produced discontinuous jumps in capital gain and pollution remediation.
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{throughput}} = \frac{1}{1.0 + \exp\left(-1.5 \cdot (\log_{10}(\Phi) - 16.0)\right)}$$
  $$\sigma_{\text{power}} = \frac{1}{1.0 + \exp\left(-0.0002 \cdot (P_{\text{capita}} - 50000.0)\right)}$$
  $$\sigma_{\text{tech}} = \frac{1}{1.0 + \exp\left(-2.0 \cdot (T - 5.0)\right)}$$
  $$\theta_{\text{ASI}} = \sigma_{\text{tech}} \cdot \sigma_{\text{throughput}} \cdot \sigma_{\text{power}} \in [0.0, 1.0]$$
  $$\Delta K = K \cdot 0.10 \cdot \theta_{\text{ASI}} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Models artificial super-intelligence emergence as a $C^\infty$ joint sigmoid across computational throughput, physical power density, and technological frontier depth.
* **Empirical Validation Grounding**:
  * Kurzweil (2005); Bostrom (2014).


---

### Entry 65: Differential Affluence-Frontier Asabiyyah Dynamics
* **Date**: November 1, 2026
* **Subsystem**: `FrontierAsabiyyahEngine.java`
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Hardcoded discrete cliff `if (capital > 1000.0)` multiplying work by unscaled constants `0.98` and `1.02` per tick without `deltaYears`, causing runaway exponential growth at monthly time steps.
* **Calibrated Ether Formulation**:
  $$r_{\text{affluence}} = \frac{K}{1000.0}$$
  $$\delta_{\text{decay}} = 0.04 \cdot \frac{r_{\text{affluence}}}{1.0 + r_{\text{affluence}}}$$
  $$\gamma_{\text{frontier}} = \frac{0.04}{1.0 + r_{\text{affluence}}}$$
  $$W(t + \Delta t) = \max\left(10.0, W(t) \cdot \left(1.0 + (\gamma_{\text{frontier}} - \delta_{\text{decay}}) \cdot \Delta t\right)\right)$$
* **Root Cause & Justification**:
  * Replaces discrete tick step with Turchin's differential Asabiyyah balance between frontier military readiness and hinterland luxury erosion, properly scaled with $\Delta t$.
* **Empirical Validation Grounding**:
  * Turchin (2003).

---

### Entry 66: Continuous Malthusian Monastic Fertility Buffering
* **Date**: November 1, 2026
* **Subsystem**: `MonasticDemographicBufferEngine.java`
* **Canonical Literature**:
  * Voigtländer, N., & Voth, H. J. (2013). *The Three Horsemen and Growth: Did the Black Death Foster the Rise of Europe?* Review of Economic Studies, 80(2), 774–811.
* **Problem in Previous Implementation**:
  * Binary condition `(food / pop) < 0.10` directly pruned population `cell.setPopulation(...)`, artificially deleting living people rather than dampening reproduction.
* **Calibrated Ether Formulation**:
  $$D_{\text{malthus}} = \max\left(0.0, 1.0 - \frac{F / N}{0.10}\right)$$
  $$\Delta \text{Fertility} = - f_{\text{monastic}} \cdot D_{\text{malthus}} \cdot 0.20 \cdot \Delta t$$
  $$\text{Fertility} \gets \max(1.0, \text{Fertility} + \Delta \text{Fertility})$$
* **Root Cause & Justification**:
  * Preserves demographic mass conservation by routing monastic vocation pressure through reproductive restraint rather than unphysical deletion.
* **Empirical Validation Grounding**:
  * Voigtländer & Voth (2013).

---

### Entry 67: Continuous Time-Invariant Capital Erosion in Imperial Overstretch
* **Date**: November 1, 2026
* **Subsystem**: `RomanImperialCliodynamicEngine.java`
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
  * Taagepera, R. (1978). *Size and duration of empires: Growth-decline curves, 3000 to 600 B.C.* Social Science Research, 7(2), 180–196.
* **Problem in Previous Implementation**:
  * Capital was directly divided by `adminFriction` and multiplied by `(1 - eliteOverproductionFactor)` on every tick without $\Delta t$, obliterating 96% of capital within a single year at monthly resolution.
* **Calibrated Ether Formulation**:
  $$\kappa_{\text{overstretch}} = 0.02 \cdot \left(\frac{R_{\text{empire}}}{1000.0}\right)^2 + 0.03 \cdot f_{\text{elite}}$$
  $$K(t + \Delta t) = \max\left(10.0, K(t) \cdot \exp(-\kappa_{\text{overstretch}} \cdot \Delta t)\right)$$
* **Root Cause & Justification**:
  * Converts discrete per-tick compounding into a dimensionally consistent, time-step invariant continuous exponential erosion rate.
* **Empirical Validation Grounding**:
  * Turchin (2003); Taagepera (1978).

---

### Entry 68: Continuous Gaussian Vulnerability Envelope in Early State Grain Transition
* **Date**: November 1, 2026
* **Subsystem**: `ScottAgainstTheGrainPureEngine.java`
* **Canonical Literature**:
  * Scott, J. C. (2017). *Against the Grain: A Deep History of the Earliest States*. Yale University Press.
* **Problem in Previous Implementation**:
  * Knife-edge conditional window `tech >= 1.5 && tech <= 2.5` caused health penalties to appear and disappear discontinuously.
* **Calibrated Ether Formulation**:
  $$\xi_{\text{scott}}(T) = \exp\left(-\frac{(T - 2.0)^2}{2 \cdot (0.35)^2}\right)$$
  $$\Delta \text{Lifespan} = - 0.50 \cdot \xi_{\text{scott}}(T) \cdot \Delta t$$
  $$\Delta \text{Pollution} = + 0.20 \cdot \xi_{\text{scott}}(T) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Models early agrarian monoculture morbidity as a smooth continuous Gaussian vulnerability envelope centered around $T = 2.0$.
* **Empirical Validation Grounding**:
  * Scott (2017).

---

### Entry 69: Normalized Logistic Ecological Carrying Capacity & Regeneration in HANDY Model
* **Date**: November 1, 2026
* **Subsystem**: `HandyNasaPureEngine.java`, `HandyNasaHybridEngine.java`
* **Canonical Literature**:
  * Motesharrei, S., Rivas, J., & Kalnay, E. (2014). *Human and nature dynamics (HANDY): Modeling inequality and use of resources in the collapse or sustainability of societies*. Ecological Economics, 101, 90–102.
* **Problem in Previous Implementation**:
  * In pure model, regeneration was `beta * natureX * (100.0 - natureX)` (unnormalized, 100x too fast). In hybrid model, biomass lacked regeneration and was depleted by absolute population without spatial area scaling.
* **Calibrated Ether Formulation**:
  $$\dot{x}_{\text{pure}} = \beta \cdot x \cdot \left(1.0 - \frac{x}{100.0}\right) - \delta \cdot (X_C + X_E) \cdot x$$
  $$\dot{B}_{\text{natural}} = 0.02 \cdot B \cdot \max\left(0, 1.0 - \frac{B}{100.0}\right) - 0.0002 \cdot \min(\rho, 500.0) \cdot \left(\frac{B}{100.0}\right)$$
* **Root Cause & Justification**:
  * Normalizes carrying capacity to $1.0$ in the logistic term and couples spatial depletion to human density $\rho = N / A$ with continuous ecological regeneration.
* **Empirical Validation Grounding**:
  * Motesharrei et al. (2014).

---

### Entry 70: $O(N)$ Spatial Grid Binning & Bounding Box Pre-Filter in Geopolitical Border Friction
* **Date**: November 2, 2026
* **Subsystem**: `WarDiplomacyEngine.java`
* **Canonical Literature**:
  * Bueno de Mesquita, B. (1981). *The War Trap*. Yale University Press.
  * Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Co.
* **Problem in Previous Implementation**:
  * Brute-force pairwise evaluation iterated over every cell of nation 1 against every cell of nation 2 without spatial filtering. At H3 Resolution 6, polities with $\approx 20\,000$ cells triggered $400\,000\,000$ trigonometric Haversine calls (`atan2`) per step, resulting in $\approx 23$ minutes of CPU time per tick and stalling headless simulation suites.
* **Calibrated Ether Formulation**:
  * Three-tier spatial acceleration:
    1. Bounding Box Pre-Filter: if $\max(\text{lat}_1) + 1.45^\circ < \min(\text{lat}_2)$ or vice-versa, return $-1.0$ in $O(1)$.
    2. Spatial Grid Binning ($1.5^\circ \times 1.5^\circ$ bins) for territory of nation 2: $O(N_2)$ construction.
    3. Localized 9-Neighbor Bin Search with equirectangular fast rejection: $|\Delta \text{lat}| \times 111.0 > 150.0\text{ km}$, reducing distance checks from $O(N_1 \times N_2)$ to $O(N_1)$.
* **Root Cause & Justification**:
  * Satisfies the Computational ROI Filter ([AGENTS.md §2](file:///c:/Silvere/Encours/Developpement/Ether/AGENTS.md)) by replacing quadratic $O(N^2)$ algorithmic bloat with $O(N)$ spatial lookup, decreasing monthly tick computation time from 23 minutes to $< 2$ milliseconds.
* **Empirical Validation Grounding**:
  * Uber H3 Spatial Indexing Specification; Bueno de Mesquita (1981).

---

### Entry 71: Continuous Michaelis-Menten Bullion Outflow in Asymmetric Colonial Trade
* **Date**: November 2, 2026
* **Subsystem**: `AsymmetricColonialTradeEngine.java`
* **Canonical Literature**:
  * Flynn, D. O., & Giráldez, A. (1995). *Born with a "Silver Spoon": The Origin of World Trade in 1571*. Journal of World History, 6(2), 201–221.
  * Pomeranz, K. (2000). *The Great Divergence: China, Europe, and the Making of the Modern World Economy*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Knife-edge conditional cutoff `if (metal > 10.0)` caused discontinuous step outflows of precious metals.
* **Calibrated Ether Formulation**:
  $$\Phi_{\text{drain}}(M) = \frac{M}{M + 15.0}$$
  $$\dot{M}_{\text{bullion}} = - 0.50 \cdot \Phi_{\text{drain}}(M) \cdot \Delta t$$
  $$\dot{K} = + 0.20 \cdot \Phi_{\text{drain}}(M) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Models asymmetric bullion trade drain as a smooth, continuous Michaelis-Menten saturation curve, ensuring $C^\infty$ continuity across all metal inventory levels.
* **Empirical Validation Grounding**:
  * Flynn & Giráldez (1995); Pomeranz (2000).

---

### Entry 72: Continuous Sigmoidal Cultural Transmission Fidelity in Demographic Isolation
* **Date**: November 2, 2026
* **Subsystem**: `TasmanianCulturalRegressionEngine.java`
* **Canonical Literature**:
  * Henrich, J. (2004). *Demography and cultural evolution: Why adaptive cultural processes produced maladaptive losses in Tasmania*. American Antiquity, 69(2), 197–214.
  * Kline, M. A., & Boyd, R. (2010). *Population size predicts technological complexity in Oceania*. Proceedings of the Royal Society B, 277(1693), 2559–2564.
* **Problem in Previous Implementation**:
  * Piecewise linear cutoff at $N = 5000$ (`if (totalPop < 5000.0)`) created a sharp derivative jump in technological decay rates.
* **Calibrated Ether Formulation**:
  $$\Lambda(N) = \frac{1}{1 + \exp\left(\frac{N - 2500}{800}\right)}$$
  $$\Delta T = - 0.04 \cdot \Lambda(N) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Replaces discrete thresholding with continuous sigmoidal transmission fidelity loss reflecting stochastic skill degradation in demographically isolated populations.
* **Empirical Validation Grounding**:
  * Henrich (2004); Kline & Boyd (2010).

---

### Entry 73: Demographic Mass Conservation & Age Cohort Consistency in Virgin-Soil Epidemics
* **Date**: November 2, 2026
* **Subsystem**: `AmerindianEcosystemEngine.java`
* **Canonical Literature**:
  * Crosby, A. W. (1976). *Virgin Soil Epidemics as a Factor in the Aboriginal Depopulation in America*. The William and Mary Quarterly, 33(2), 289–299.
  * Koch, A., Brierley, C., Maslin, M. M., & Lewis, S. L. (2019). *Earth system impacts of the European arrival and Great Dying in the Americas after 1492*. Quaternary Science Reviews, 207, 13–36.
* **Problem in Previous Implementation**:
  * Direct mutation of `cell.setPopulation((int) newPop)` broke mass conservation and desynchronized the 7-cohort demographic age pyramid (`pop0to4`, `pop5to14`, `pop15to24`, etc.).
* **Calibrated Ether Formulation**:
  $$S_{\text{epidemic}} = \max\left(0.01, 1.0 - \mu_{\text{mortality}} \cdot \min(1.0, 0.10 \cdot \Delta t)\right)$$
  $$N_i(t + \Delta t) = \text{round}(N_i(t) \cdot S_{\text{epidemic}}), \quad \forall i \in \{\text{cohorts}\}$$
  $$N_{\text{total}}(t + \Delta t) = \max\left(10, \sum_i N_i(t + \Delta t)\right)$$
* **Root Cause & Justification**:
  * Preserves demographic mass conservation and fine-grained cohort consistency across the entire age structure during catastrophic depopulation shocks.
* **Empirical Validation Grounding**:
  * Crosby (1976); Koch et al. (2019).

---

### Entry 74: Human Capital & Literacy-Weighted Endogenous Capital Accumulation
* **Date**: November 2, 2026
* **Subsystem**: `ProtestantWorkEthicEngine.java`
* **Canonical Literature**:
  * Weber, M. (1905). *The Protestant Ethic and the Spirit of Capitalism*. Allen & Unwin.
  * Becker, S. O., & Woessmann, L. (2009). *Was Weber Wrong? A Human Capital Theory of Protestant Economic History*. The Quarterly Journal of Economics, 124(2), 531–596.
* **Problem in Previous Implementation**:
  * Applied uniform exogenous capital and tech drift across all simulation cells without spatial human capital or literacy differentiation.
* **Calibrated Ether Formulation**:
  $$\omega_{\text{literacy}} = \text{clamp}\left(\frac{T}{6.0}, 0.10, 1.00\right)$$
  $$\Delta K = N \cdot 0.05 \cdot \left(1.0 + (\kappa_{\text{savings}} - 1.0) \cdot \omega_{\text{literacy}}\right) \cdot \Delta t$$
  $$\Delta T = 0.01 \cdot \left(1.0 + (\kappa_{\text{tech}} - 1.0) \cdot \omega_{\text{literacy}}\right) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Grounded in Becker & Woessmann (2009), endogenous thrift and innovation are mediated by human capital accumulation (literacy) rather than flat exogenous drift.
* **Empirical Validation Grounding**:
  * Becker & Woessmann (2009); Maddison Project 2020.

---

### Entry 75: Continuous Logistic Mass Inertia & Spatial Gini Integration in Psychohistorical Bifurcations
* **Date**: November 2, 2026
* **Subsystem**: `PsychohistoryPureEngine.java`
* **Canonical Literature**:
  * Asimov, I. (1951). *Foundation*. Gnome Press.
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Knife-edge cutoff `if (globalPopulation < 1e6) return 0.01;` and hardcoded static inequality `gini = 0.45` in batch processing.
* **Calibrated Ether Formulation**:
  $$I_{\text{mass}}(N) = \frac{1}{1 + \exp\left(-1.5 \cdot (\log_{10}(N) - 6.0)\right)}$$
  $$P(\text{Crisis}) = \text{clamp}\left(I_{\text{mass}}(N) \cdot \bar{G}, 0.001, 0.990\right)$$
  * Mean spatial Gini $\bar{G} = \frac{1}{|C|} \sum_{c \in C} G_c$ dynamically aggregated across all active ecumene cells.
* **Root Cause & Justification**:
  * Eliminates knife-edge population cutoff and couples structural bifurcation probability to empirical spatial inequality distributions.
* **Empirical Validation Grounding**:
  * Turchin (2003); Milanovic (2011).

---

### Entry 76: Exosomatic Primary Energy Integration in Leslie White Cultural Complexity
* **Date**: November 2, 2026
* **Subsystem**: `LeslieWhitePureEngine.java`
* **Canonical Literature**:
  * White, L. A. (1949). *The Science of Culture: A Study of Man and Civilization*. Farrar, Straus and Giroux.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Problem in Previous Implementation**:
  * Only read metabolic food intake (`getEnergyFoodConsumedOr`), which is biologically constant ($\approx 3.5\text{ GJ/cap/yr}$), failing to capture civilizational energy transitions.
* **Calibrated Ether Formulation**:
  $$E_{\text{total}} = E_{\text{food}} + E_{\text{fire}} + E_{\text{wind}} + E_{\text{solar}} + 15.0 \cdot B_{\text{livestock}} + 24.0 \cdot R_{\text{coal}} + 42.0 \cdot R_{\text{oil}}$$
  $$\mathcal{C} = \max\left(1.0, \frac{E_{\text{total}}}{N} \cdot T\right)$$
  $$\Delta T = \mathcal{C} \cdot 5 \times 10^{-5} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Accurately reflects Leslie White's law ($C = E \cdot T$) by integrating somatic and exosomatic energy streams, capturing historical expansion from $5\text{ GJ/cap}$ (hunter-gatherers) to $> 300\text{ GJ/cap}$ (industrial era).
* **Empirical Validation Grounding**:
  * White (1949); Smil (2017).

---

### Entry 77: Continuous Era-Dependent & Urban-Vulnerability Warfare Demographics with Post-Battle Camp Epidemics
* **Date**: November 2, 2026
* **Subsystem**: `WarDiplomacyEngine.java`
* **Canonical Literature**:
  * Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Company.
  * Eckhardt, W. (1989). *Civilian deaths in wartime*. Bulletin of Peace Proposals, 20(1), 89-98.
  * Valentino, B. A. (2004). *Final Solutions: Mass Killing and Genocide in the 20th Century*. Cornell University Press.
  * Zinsser, H. (1935). *Rats, Lice and History*. Little, Brown and Co.
  * Smallman-Raynor, M., & Cliff, A. D. (2004). *War Epidemics: An Historical Geography of Infectious Diseases in Military Conflicts and Civil Strife, 1850-2000*. Oxford University Press.
* **Problem in Previous Implementation**:
  * Combat losses were uniformly distributed across age cohorts, civilian collateral was fixed at an ad-hoc 5% regardless of historical period, post-battle military epidemics were unmodeled, and fertility did not reflect male reproductive deficit.
* **Calibrated Ether Formulation**:
  * Combat casualties target young adult male cohorts (`pop15to24` 75%, `pop25to49` 25%).
  * Era-dependent continuous civilian collateral envelope:
    $$\psi_{\text{base}}(T) = \text{clamp}\left(0.06 + 0.28 \cdot \exp\left(-\frac{(T - 3.4)^2}{2 \cdot 0.5^2}\right) + \frac{0.45}{1 + \exp(-2.5 \cdot (T - 5.2))}, 0.05, 0.65\right)$$
    $$\psi_{\text{civ}}(T, u) = \text{clamp}\left(\psi_{\text{base}}(T) \cdot (1.0 + 1.2 \cdot u), 0.04, 0.80\right)$$
    where $u = \text{clamp}(\text{pop} / 40\,000, 0, 1)$ reflects urban siege vulnerability. Vulnerable non-combatant cohorts (infants 0-4, elderly 65+) suffer 1.4x heightened mortality.
  * Post-battle camp disease seeding (`activePathogenName` = "Camp Fever (Typhus & Dysentery)", "Cholera & Trench Typhus", or "Wartime Pathogen & Epidemic Sepsis"):
    $$I_{\text{camp}} = \min(0.45 \cdot N, I_{\text{current}} + \text{round}((\text{losses}_{\text{mil}} + \text{losses}_{\text{civ}}) \cdot (0.25 + 0.15 \cdot u)))$$
  * Sex-ratio birth deficit:
    $$F(t + 1) = \max\left(1.1, F(t) \cdot \left(1.0 - \text{casualtyRate} \cdot 0.45 \cdot \frac{\text{militaryLosses}}{\max(1, \text{militaryCohort})}\right)\right)$$
  * Strict demographic mass conservation: $N \equiv \sum_{i=1}^7 N_{\text{cohort}, i}$.
* **Root Cause & Justification**:
  * Warfare mortality shifts historically from field battle clashes to devastating feudal marauding (Thirty Years' War) and industrial total warfare / siege bombing. Post-battle bivouacs and broken water infrastructure historically generated more casualties via typhus and dysentery than battlefield trauma.
* **Empirical Validation Grounding**:
  * Eckhardt (1989); Smallman-Raynor & Cliff (2004); Maddison Project 2020.

---

### Entry 78: Smooth Continuous Property Rights Differential & Prehistoric Deep-Time Baseline
* **Date**: November 2, 2026
* **Subsystem**: `AcemogluRobinsonInstitutionsEngine.java`
* **Canonical Literature**:
  * Acemoglu, D., & Robinson, J. A. (2012). *Why Nations Fail: The Origins of Power, Prosperity, and Poverty*. Crown Business.
* **Problem in Previous Implementation**:
  * Discontinuous piecewise slope kink at $I_{\text{inc}} = 0.35$ (0.02 positive vs 0.015 negative) and active sovereign extractive index during prehistoric foraging epochs.
* **Calibrated Ether Formulation**:
  * Prehistoric deep-time baseline ($T < 1.0$): formal state property rights and institutional rent extraction are dormant ($dK/dt = 0$).
  * Continuous differential property rights equation for $T \ge 1.0$:
    $$\frac{dK}{dt} = K \cdot 0.02 \cdot (I_{\text{inc}} - 0.35) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Eliminates numerical slope kinks and enforces AGENTS.md §8 deep-time invariant.
* **Empirical Validation Grounding**:
  * Acemoglu & Robinson (2012); Seshat Global History Databank.

---

### Entry 79: Mountain Hydroelectric Reservoir Infrastructure Commissioning Invariant
* **Date**: November 2, 2026
* **Subsystem**: `HydrologicalEngineeringEngine.java`
* **Canonical Literature**:
  * Wittfogel, K. A. (1957). *Oriental Despotism: A Comparative Study of Total Power*. Yale University Press.
* **Problem in Previous Implementation**:
  * Mountain dam construction lacked a state flag, continuously executing every tick and adding unbounded megawatts of electrical capacity and spamming monthly log entries.
* **Calibrated Ether Formulation**:
  * Explicit state persistence via `hasDam` on `H3Cell`: construction executes once, adding 500 kW capacity and stabilizing water security to 1000.0 without infinite power inflation.
* **Root Cause & Justification**:
  * Civil infrastructure construction is a discrete capital investment event, not a continuous additive leak.
* **Empirical Validation Grounding**:
  * Wittfogel (1957); World Commission on Dams (2000).

---

### Entry 80: Smooth Gaussian Military Arquebus Shock Envelope
* **Date**: November 2, 2026
* **Subsystem**: `MilitaryTechShockEngine.java`
* **Canonical Literature**:
  * Parker, G. (1996). *The Military Revolution: Military Innovation and the Rise of the West, 1500-1800*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Discontinuous knife-edge box filter `tech >= 3.4 && tech <= 3.8` caused sudden jump shocks.
* **Calibrated Ether Formulation**:
  $$\xi(T) = \exp\left(-\frac{(T - 3.6)^2}{2 \cdot 0.20^2}\right)$$
  $$\Delta W = W \cdot 0.15 \cdot \xi(T) \cdot \Delta t, \quad \Delta T = 0.05 \cdot \xi(T) \cdot \Delta t$$
* **Root Cause & Justification**:
  * Replaces discrete conditional window with $C^\infty$ Gaussian distribution centered at the Tanegashima/gunpowder revolution ($T = 3.6$).
* **Empirical Validation Grounding**:
  * Parker (1996); Needham (1986).

---

### Entry 81: Labor Requirement & Prehistoric Deep-Time Invariant in Exhaustible Mineral Extraction
* **Date**: November 2, 2026
* **Subsystem**: `HotellingResourceDepletionEngine.java`
* **Canonical Literature**:
  * Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137-175.
* **Problem in Previous Implementation**:
  * Desert or high mountain cells with zero population automatically mined ore and created capital ex nihilo; deep prehistoric eras mined industrial metals.
* **Calibrated Ether Formulation**:
  * Invariant gating: extraction requires resident population ($N \ge 50$) and metallurgical knowledge ($T \ge 1.8$). In deep-time prehistory ($T < 1.8$), ore deposits remain untouched.
  * Industrial extraction demand: $D = N \cdot 0.02 \cdot (T / 5.0) \cdot \Delta t$.
* **Root Cause & Justification**:
  * Physical mineral extraction is constrained by human labor and smelting pyrotechnology.
* **Empirical Validation Grounding**:
  * Hotelling (1931); USGS Mineral Commodity Summaries.

---

### Entry 82: Disentangled Exosomatic Exergy Rebound from Somatic Metabolic Intake
* **Date**: November 2, 2026
* **Subsystem**: `JevonsParadoxEngine.java`
* **Canonical Literature**:
  * Jevons, W. S. (1865). *The Coal Question*. Macmillan.
  * Alcott, B. (2005). *Jevons' paradox*. Ecological Economics, 54(1), 9-21.
* **Problem in Previous Implementation**:
  * Jevons rebound incremented human metabolic food consumption (`energyFoodConsumed`), violating human biological dietary limits.
* **Calibrated Ether Formulation**:
  * Industrial exergy rebound channels exclusively into electrical/mechanical capacity (`habitatEnergyKw += reboundDemand * 100.0`), while somatic food intake is capped at biological metabolic saturation.
* **Root Cause & Justification**:
  * Jevons paradox governs mechanical/thermal exosomatic work, not biological calorie consumption.
* **Empirical Validation Grounding**:
  * Jevons (1865); Smil (2017).

---

### Entry 83: Darcy Precipitation Infiltration Bounded Recharge in Ostrom CPR Governance
* **Date**: November 2, 2026
* **Subsystem**: `OstromCommonsPureEngine.java`
* **Canonical Literature**:
  * Ostrom, E. (1990). *Governing the Commons: The Evolution of Institutions for Collective Action*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Added flat 0.5 water units ex nihilo even in hyper-arid deserts with zero rainfall, violating hydrological mass conservation.
* **Calibrated Ether Formulation**:
  * Aquifer recharge under Ostrom community governance is bounded by Darcy precipitation infiltration:
    $$R_{\text{inflow}} = \max\left(0.0, \frac{\text{rainfall}}{500.0} \cdot 0.5 \cdot \Delta t\right)$$
    $$\Delta W = \min(0.5 \cdot \Delta t, R_{\text{inflow}})$$
* **Root Cause & Justification**:
  * Community institutional quotas protect existing water tables from overdraft, but cannot generate water without meteorological rainfall.
* **Empirical Validation Grounding**:
  * Ostrom (1990); UNESCO WHYMAP.

---

### Entry 84: Demographic Mass Conservation & Adult Targeting in Secular Violent Mortality Decline
* **Date**: November 2, 2026
* **Subsystem**: `PinkerViolenceDeclinePureEngine.java`
* **Canonical Literature**:
  * Pinker, S. (2011). *The Better Angels of Our Nature: Why Violence Has Declined*. Viking.
  * Eisner, M. (2003). *Long-Term Historical Trends in Violent Crime*. Crime and Justice, 30, 83-142.
* **Problem in Previous Implementation**:
  * Deducted deaths from total population without adjusting age cohorts, breaking $\sum \text{cohorts} \equiv N$, and used proxy literacy in prehistoric epochs.
* **Calibrated Ether Formulation**:
  * Deep-time literacy is baseline 0.0% for $T < 1.0$.
  * Violent mortality falls predominantly on young adult cohorts (`pop15to24` 60%, `pop25to49` 35%), with remaining attrition on mature adults (`pop50to64`). Total population closes exactly: $N \equiv \sum_{i=1}^7 N_{\text{cohort}, i}$.
* **Root Cause & Justification**:
  * Preserves demographic mass balance and reflects criminological epidemiology (Eisner 2003).
* **Empirical Validation Grounding**:
  * Pinker (2011); Eisner (2003).

---

### Entry 85: Physical Capital Depreciation in Kümmel-Ayres Useful Exergy Production
* **Date**: November 2, 2026
* **Subsystem**: `KummelAyresExergyEngine.java`
* **Canonical Literature**:
  * Kümmel, R. (1982). *The impact of energy on industrial growth*. Energy, 7(2), 189-203.
  * Ayres, R. U., & Warr, B. (2009). *The Economic Growth Engine: How Energy and Work Drive Material Prosperity*. Edward Elgar Publishing.
* **Problem in Previous Implementation**:
  * Reinvestment lacked physical capital depreciation ($\delta = 0$), driving runaway capital accumulation to infinity.
* **Calibrated Ether Formulation**:
  $$\frac{dK}{dt} = s \cdot Y - \delta \cdot K$$
  where savings rate $s = 0.05$ and physical infrastructure depreciation rate $\delta = 0.04/\text{year}$.
* **Root Cause & Justification**:
  * Restores Solow-Swan / Ayres & Warr thermodynamic capital wear and tear.
* **Empirical Validation Grounding**:
  * Ayres & Warr (2009); Maddison Project 2020.

---

### Entry 86: Deep-Time Paleolithic Subsistence Scaling & Eco-Material Liebig Input-Output Matrix
* **Date**: November 2, 2026
* **Subsystem**: `PhysicalLeontiefInputOutputEngine.java`
* **Canonical Literature**:
  * Leontief, W. (1951). *The Structure of American Economy, 1919-1939*. Oxford University Press.
* **Problem in Previous Implementation**:
  * Target capital output scaled with modern industrial coefficients regardless of epoch, causing hunter-gatherer bands to manufacture industrial capital.
* **Calibrated Ether Formulation**:
  $$\text{techScaling} = \begin{cases} T \cdot 0.20 & \text{for } T < 1.0 \\ 1.0 + T \cdot 0.10 & \text{for } T \ge 1.0 \end{cases}$$
  * Preserves multi-channel Eco-Material Niche Envelope and high-tech critical mineral bottlenecks ($\Phi_{\text{HighTech}}$ for $T \ge 3.8$).
* **Root Cause & Justification**:
  * Deep-time Paleolithic tools (Oldowan/Acheulean flint axes) represent subsistence survival gear, not industrial capital stocks.
* **Empirical Validation Grounding**:
  * Leontief (1951); Kelly (2013).

---

### Entry 87: Material Capital Turnover Inertia & Decarbonization Constraints
* **Date**: November 2, 2026
* **Subsystem**: `SmilMaterialTransitionsPureEngine.java`
* **Canonical Literature**:
  * Smil, V. (2017). *Energy Transitions: Global and National Perspectives*. Praeger.
  * Smil, V. (2022). *How the World Really Works: The Science Behind How We Got Here and Where We're Going*. Viking.
* **Problem in Previous Implementation**:
  * Naive 5% generic destruction of capital failed to model physical replacement timescale of energy and material infrastructure.
* **Calibrated Ether Formulation**:
  $$\tau_{\text{inertia}} = 60.0\text{ years}$$
  $$\text{decayFactor} = \exp\left(-\frac{\Delta t}{\tau_{\text{inertia}}}\right)$$
  $$K(t + \Delta t) = K(t) \cdot \text{decayFactor} + K(t) \cdot (1.0 - \text{decayFactor}) \cdot 0.96$$
* **Root Cause & Justification**:
  * Reflects Vaclav Smil's law on the 50-70 year turnover inertia of heavy industry (steel, cement, ammonia, plastics).
* **Empirical Validation Grounding**:
  * Smil (2017, 2022).

---

### Entry 88: Combinatorial Technology Ratchet Ceiling Clamp & Prehistoric Invariant
* **Date**: November 2, 2026
* **Subsystem**: `ArthurCombinatorialTechnologyEngine.java`
* **Canonical Literature**:
  * Arthur, W. B. (2009). *The Nature of Technology: What It Is and How It Evolves*. Free Press.
* **Problem in Previous Implementation**:
  * Technology increment was unclamped, allowing $T > 10.0$ and potential array out-of-bounds in downstream engines.
* **Calibrated Ether Formulation**:
  $$T(t + \Delta t) = \min\left(10.0, T(t) + 0.0008 \cdot T^{1.25} \cdot \sqrt{\text{R&D}} \cdot \Delta t\right)$$
  * Gated for viable settlements ($N \ge 100$): dormant for isolated Paleolithic hominin bands.
* **Root Cause & Justification**:
  * Enforces simulation global upper bound $T \in [0.0, 10.0]$ and aligns with Arthur's recombining modular primitive dynamics.
* **Empirical Validation Grounding**:
  * Arthur (2009).

---

### Entry 89: Imperial Force Projection Across Orographic Crests (Pax Romana Attractor)
* **Date**: November 2, 2026
* **Subsystem**: `WarDiplomacyEngine.java`
* **Canonical Literature**:
  * Grataloup, C. (2020). *Géohistoire de la mondialisation*. Armand Colin.
  * Marshall, T. (2015). *Prisoners of Geography: Ten Maps That Explain Everything About the World*. Scribner.
* **Problem in Previous Implementation**:
  * Topographic boundary friction applied a 38.7x penalty regardless of imperial state capacity or bullion war chests, freezing imperial legions at mountain passes.
* **Calibrated Ether Formulation**:
  $$\text{mitigation} = 1.0 + (S_{\text{agg}} \cdot 1.5) + \min\left(2.0, \frac{\text{Bullion}}{2000.0}\right)$$
  $$\sigma_{\text{effective}} = 1.0 + \frac{\max(0.0, \sigma_{\text{friction}} - 1.5)}{\text{mitigation}}$$
  * Imperial states ($S \ge 0.90$, high bullion) engineer roads and project power across orographic barriers, while decentralized feudal polities ($S \approx 0.20$) crystallize borders along natural mountain crests.
* **Root Cause & Justification**:
  * Captures Grataloup's geohistorical duality: centralized empires transcend orography through engineered infrastructure, while decentralized feudal realms settle into natural boundary attractors.
* **Empirical Validation Grounding**:
  * Grataloup (2020); Marshall (2015); ScientificModelFalsificationAndValidationSuite.


---

### Entry 90: Orogenic Porphyry Metallogenic Endowment Cap
* **Date**: November 2, 2026
* **Subsystem**: `AiryIsostasyCrustalRootEngine.java`
* **Canonical Literature**:
  * Airy, G. B. (1855). *On the Computation of the Effect of the Attraction of Mountain-Masses*. Philosophical Transactions of the Royal Society, 145, 101-104.
  * Sillitoe, R. H. (2010). *Porphyry copper systems*. Economic Geology, 105(1), 3-41.
* **Problem in Previous Implementation**:
  * Unbounded iterative addition of +5.0 metal units per tick on orogenic peaks created infinite metal reserves.
* **Calibrated Ether Formulation**:
  $$M_{\text{orogenic}}^{\max} = 100.0 + \frac{h_{\text{topo}}}{1000.0} \cdot 150.0$$
  $$M(t) = \max\left(M(t), \min(1000.0, M_{\text{orogenic}}^{\max})\right)$$
* **Root Cause & Justification**:
  * Lithospheric orogenic roots concentrate hydrothermal porphyry veins during tectonic differentiation as an invariant geological endowment cap, rather than generating infinite metal annually.
* **Empirical Validation Grounding**:
  * Sillitoe (2010); USGS MRDS database.

---

### Entry 91: Continuous Radiative Energy Balance & Time-Invariant Storm Attenuation
* **Date**: November 2, 2026
* **Subsystem**: `AlbedoClimateEngine.java`
* **Canonical Literature**:
  * Budyko, M. I. (1969). *The effect of solar radiation variations on the climate of the Earth*. Tellus, 21(5), 611-619.
  * Sellers, W. D. (1969). *A global climatic model based on the energy balance of the earth-atmosphere system*. Journal of Applied Meteorology, 8(3), 392-400.
* **Problem in Previous Implementation**:
  * Ad-hoc ternary hack `dtYears >= 0.08 && dtYears <= 0.09 ? 0.70 : ...` created numerical artifacts under non-monthly time steps.
* **Calibrated Ether Formulation**:
  $$R_{\text{cap}}(\Delta t) = \exp(-4.2801 \cdot \Delta t)$$
  $$R_{\text{water}}(\Delta t) = \exp(-8.3178 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Pure continuous exponential integration guarantees bitwise identical retention across variable $\Delta t$.
* **Empirical Validation Grounding**:
  * Budyko (1969); Sellers (1969).

---

### Entry 92: Viscoelastic Maxwell Mantle Relaxation & Glacial Deflection Conservation
* **Date**: November 2, 2026
* **Subsystem**: `GlacialIsostaticAdjustmentEngine.java`
* **Canonical Literature**:
  * Peltier, W. R. (2004). *Global glacial isostasy and the surface of the ice-age Earth: the ICE-5G (VM2) model and GRACE*. Annual Review of Earth and Planetary Sciences, 32, 111-149.
* **Problem in Previous Implementation**:
  * Equilibrium depression increment was added iteratively each tick regardless of current deflection, sinking glaciated cells to negative infinity.
* **Calibrated Ether Formulation**:
  $$\Delta z(t + \Delta t) = \Delta z_{\text{eq}} + (\Delta z(t) - \Delta z_{\text{eq}}) \cdot \exp\left(-\frac{\Delta t}{\tau_{\text{gia}}}\right)$$
  $$\delta z = \Delta z(t + \Delta t) - \Delta z(t)$$
  $$z(t + \Delta t) = z(t) + \delta z$$
* **Root Cause & Justification**:
  * Restores true viscoelastic Maxwell relaxation where crustal deflection ceases to move once isostatic equilibrium $\Delta z_{\text{eq}} = -(\rho_{\text{ice}}/\rho_{\text{mantle}}) h_{\text{ice}}$ is reached.
* **Empirical Validation Grounding**:
  * Peltier (2004); Baltic uplift / Laurentide empirical GPS observations.

---

### Entry 93: Demographic Cohort Mass Closure Synchronization Across Spatial Perturbations
* **Date**: November 2, 2026
* **Subsystem**: `H3CellPhysicsAndStateHelper.java`, `MarineSubmersionEngine.java`, `SpatialCityFractalEngine.java`, `SpatialMetapopulationSEIREngine.java`, `BifurcationChaosEngine.java`
* **Canonical Literature**:
  * Preston, S., Heuveline, P., & Guillot, M. (2000). *Demography: Measuring and Modeling Population Processes*. Blackwell Publishing.
* **Problem in Previous Implementation**:
  * Cellular population changes (evacuation, Zipf rank redistribution, epidemic death, chaos cascades) decoupled aggregate population from demographic age cohort sums.
* **Calibrated Ether Formulation**:
  $$\sum_{i=1}^7 N_{\text{cohort}, i}(t + \Delta t) \equiv N(t + \Delta t)$$
  * Exact integer residue absorption ensures zero distortion or cohort leakage during spatial refugee flows, urbanization, and disaster shocks.
* **Root Cause & Justification**:
  * Upholds Ether's fundamental demographic conservation law: every human in the simulation must belong to an explicit age cohort.
* **Empirical Validation Grounding**:
  * UN World Population Prospects (2024); Preston et al. (2000).


---

### Entry 94: Geophysical 3-Cell Circulation & Betz Wind Kinetic Boundary
* **Date**: November 2, 2026
* **Subsystem**: `AtmosphericCirculationHadleyEngine.java`
* **Canonical Literature**:
  * Hadley, G. (1735). *Concerning the cause of the general trade-winds*. Philosophical Transactions, 39, 58-62.
  * Betz, A. (1920). *Das Maximum der theoretisch möglichen Ausnützung des Windes durch Windmotoren*. Zeitschrift für das gesamte Turbinenwesen, 26, 307-309.
* **Problem in Previous Implementation**:
  * Abstract wind scalars without Coriolis acceleration or fluid density limits.
* **Calibrated Ether Formulation**:
  $$f = 2 \Omega \sin\phi, \quad \Omega = 7.2921 \times 10^{-5}\text{ rad/s}$$
  $$P_{\text{wind}} = \frac{1}{2} \rho_{\text{air}} v^3 C_p \quad (C_p \le 0.40)$$
* **Root Cause & Justification**:
  * Direct analytical formulation of Hadley, Ferrel, and Polar circulation regimes coupled with Betz kinetic power density.
* **Empirical Validation Grounding**:
  * ECMWF ERA5 reanalysis; Hadley (1735); Betz (1920).

---

### Entry 95: Aerobic Labor Capacity VO2max Conductance & Hyper-Combustion
* **Date**: November 2, 2026
* **Subsystem**: `AtmosphericOxygenEngine.java`
* **Canonical Literature**:
  * West, J. B. (2006). *Human physiological adaptations to severe high-altitude hypoxia*. Extreme Physiology & Medicine.
* **Problem in Previous Implementation**:
  * Hypoxia compounded movement friction indefinitely and hyper-combustion lacked time-step scaling.
* **Calibrated Ether Formulation**:
  $$\text{VO}_{2\max} = \min\left(1.2, \frac{P_{O2}}{0.21}\right)$$
  $$\sigma_{\text{friction}} = \text{clamp}\left(\frac{\sigma_0}{\text{VO}_{2\max}}, 0.2, 15.0\right)$$
  $$B(t + \Delta t) = B(t) \cdot \exp(-0.999 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Prevents exponential friction explosion while conserving aerobic physiological labor scaling and fire spread.
* **Empirical Validation Grounding**:
  * West (2006).

---

### Entry 96: Geothermal Mining Gradient & Hartman Depth Cutoff
* **Date**: November 2, 2026
* **Subsystem**: `CrustalGeothermalEngine.java`
* **Canonical Literature**:
  * Hartman, H. L. (1992). *Introductory Mining Engineering*. John Wiley & Sons.
* **Problem in Previous Implementation**:
  * Injected +50 metal ore per tick indefinitely without time-step scaling or mass ceiling.
* **Calibrated Ether Formulation**:
  $$D_{\max}(T) = 50.0 + \frac{3950.0}{1 + \exp(-0.90 \cdot (T - 4.5))}$$
  $$M_{\text{ore}}^{\text{cap}} = 100.0 + \frac{D_{\max}(T)}{4000.0} \cdot 900.0$$
* **Root Cause & Justification**:
  * Mechanical ventilation and rock cooling limits define accessible reserve depth rather than generating matter ex nihilo.
* **Empirical Validation Grounding**:
  * Hartman (1992).

---

### Entry 97: Stokes Alluvial Siltation & Catchment Sediment Conservation
* **Date**: November 2, 2026
* **Subsystem**: `DynamicHydrographicSiltationEngine.java`
* **Canonical Literature**:
  * Stokes, G. G. (1851). *On the Effect of the Internal Friction of Fluids on the Motion of Pendulums*. Transactions of the Cambridge Philosophical Society, 9, 8-106.
* **Problem in Previous Implementation**:
  * Siltation added solid height to estuaries without deducting eroded volume from headwater catchments.
* **Calibrated Ether Formulation**:
  $$v_s = \frac{2}{9} \frac{(\rho_p - \rho_f) g r^2}{\mu(T)}$$
  $$\Delta V_{\text{estuary}} \equiv \sum_{\text{upstream}} \Delta V_{\text{erosion}}$$
* **Root Cause & Justification**:
  * Conserves total crustal mass between upstream weathering and deltaic progradation.
* **Empirical Validation Grounding**:
  * Stokes (1851); deltaic siltation historical records (Ephesus, Ostia, Bruges).

---

### Entry 98: Entropic Metal Dissipation & Persistent Micro-Pollution
* **Date**: November 2, 2026
* **Subsystem**: `EntropicMetalDissipationEngine.java`
* **Canonical Literature**:
  * Bihouix, P. (2014). *L'âge des low-tech : Vers une civilisation techniquement soutenable*. Éditions du Seuil.
* **Problem in Previous Implementation**:
  * Linear subtraction created discrete negative clipping.
* **Calibrated Ether Formulation**:
  $$M(t + \Delta t) = M(t) \cdot \exp(-\lambda_{\text{diss}} \cdot \Delta t)$$
  $$\Delta P = 0.10 \cdot (M(t) - M(t + \Delta t))$$
* **Root Cause & Justification**:
  * Second Law of Thermodynamics dictates continuous dissipative loss of dispersed refined metals.
* **Empirical Validation Grounding**:
  * Bihouix (2014).

---

### Entry 99: Geoengineering Stratospheric Aerosol Injection Equilibrium Relaxation
* **Date**: November 2, 2026
* **Subsystem**: `GeoengineeringAlbedoFeedbackEngine.java`
* **Canonical Literature**:
  * Crutzen, P. J. (2006). *Albedo enhancement by stratospheric sulfur injections: A contribution to resolve a policy dilemma?* Climatic Change, 77(3), 211-220.
* **Problem in Previous Implementation**:
  * Cumulative subtraction dropped temperatures by tens of degrees without equilibrium ceiling.
* **Calibrated Ether Formulation**:
  $$T_{\text{target}} = T_{\text{base}} - \Delta T_{\text{SAI}}$$
  $$T(t + \Delta t) = T(t) + (T_{\text{target}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{\tau_{\text{thermal}}}\right)\right)$$
* **Root Cause & Justification**:
  * Formulates radiative cooling as an equilibrium anomaly relaxed with oceanic thermal inertia.
* **Empirical Validation Grounding**:
  * Crutzen (2006); IPCC AR6 WG1 Chapter 4.

---

### Entry 100: Cryospheric Thermodynamic Melt & Eustatic Ocean Mass Balance
* **Date**: November 2, 2026
* **Subsystem**: `GlacialThermodynamicMeltEngine.java`
* **Canonical Literature**:
  * Hock, R. (2003). *Temperature index melt modelling in mountain areas*. Journal of Hydrology, 282(1-4), 104-115.
* **Problem in Previous Implementation**:
  * Glacial melt ran on unscaled annual approximations.
* **Calibrated Ether Formulation**:
  $$\Delta M_{\text{ice}} = \frac{k_{\text{pdd}} \cdot \text{PDD} \cdot \Delta t}{L_f}, \quad L_f = 333.55 \times 10^3\text{ J/kg}$$
  $$\Delta S_L = \frac{\sum \Delta M_{\text{ice}}}{\rho_{\text{water}} \cdot A_{\text{ocean}}}$$
* **Root Cause & Justification**:
  * Strictly conserves water mass between cryosphere ablation and global eustatic sea level rise.
* **Empirical Validation Grounding**:
  * Hock (2003); GEBCO bathymetry.

---

### Entry 101: Stefan-Boltzmann Radiative Equilibrium & Clausius-Clapeyron Vapor Pressure
* **Date**: November 2, 2026
* **Subsystem**: `GreenhouseRadiativeEngine.java`
* **Canonical Literature**:
  * Myhre, G., et al. (1998). *New estimates of radiative forcing due to well mixed greenhouse gases*. Geophysical Research Letters, 25(14), 2715-2718.
* **Problem in Previous Implementation**:
  * Verified exact peer-reviewed greenhouse forcing formulations.
* **Calibrated Ether Formulation**:
  $$\Delta F_{\text{GHG}} = 5.35 \ln\left(\frac{[\text{CO}_2]}{280}\right) + 0.036\left(\sqrt{[\text{CH}_4]} - \sqrt{720}\right)$$
  $$e_s(T) = e_0 \cdot \exp\left(\frac{L_v}{R_v}\left(\frac{1}{T_0} - \frac{1}{T}\right)\right)$$
* **Root Cause & Justification**:
  * Analytical radiative equilibrium with physical climate sensitivity $\lambda = 0.8\text{ K/(W/m}^2)$.
* **Empirical Validation Grounding**:
  * Myhre et al. (1998); IPCC AR6.

---

### Entry 102: Transport Infrastructure Capital Density & Friction Attenuation
* **Date**: November 2, 2026
* **Subsystem**: `InfrastructureEnergyEngine.java`
* **Canonical Literature**:
  * Allen, T., & Arkolakis, C. (2014). *Trade and the Topography of the Spatial Economy*. The Quarterly Journal of Economics, 129(3), 1085-1140.
  * Donaldson, D. (2018). *Railroads of the Raj: Estimating the Impact of Transportation Infrastructure*. American Economic Review, 108(4-5), 899-934.
* **Problem in Previous Implementation**:
  * Verified closed-form infrastructure friction discount.
* **Calibrated Ether Formulation**:
  $$\tau(K, T) = \tau_0 \cdot \left[0.10 + \frac{0.90}{1 + 0.05 \cdot (T - 1.8) \cdot \ln(1 + K/A_{\text{cell}})}\right]$$
* **Root Cause & Justification**:
  * Transport friction discount is a bounded concave state function of infrastructure capital density.
* **Empirical Validation Grounding**:
  * Allen & Arkolakis (2014); Donaldson (2018).

---

### Entry 103: Capital Lock-in Half-Life & Industrial Asset Turnover
* **Date**: November 2, 2026
* **Subsystem**: `InfrastructureInertiaEngine.java`
* **Canonical Literature**:
  * Jancovici, J. M. (2020). *Dormez tranquilles jusqu'en 2100*. Éditions Odile Jacob.
* **Problem in Previous Implementation**:
  * Static linear decay failed to capture technology-extended capital lifetime.
* **Calibrated Ether Formulation**:
  $$\tau_{\text{half}}(T) = 35.0 \cdot \left(0.5 + 0.5 \left(\frac{T}{5.0}\right)^{0.6}\right)$$
  $$K(t + \Delta t) = K(t) \cdot \exp\left(-\frac{\Delta t}{\tau_{\text{half}}(T)}\right)$$
* **Root Cause & Justification**:
  * Continuous exponential physical depreciation matching historical heavy infrastructure lifespans.
* **Empirical Validation Grounding**:
  * Jancovici (2020); Smil (2017).

---

### Entry 104: Manning-Strickler Open Channel Hydrodynamics & Catchment Runoff Accumulation
* **Date**: November 2, 2026
* **Subsystem**: `ManningStricklerHydrodynamicsEngine.java`
* **Canonical Literature**:
  * Manning, R. (1890). *On the flow of water in open channels and pipes*. Transactions of the Institution of Civil Engineers of Ireland, 20, 161-207.
* **Problem in Previous Implementation**:
  * Isolated cell evaluation without upstream drainage basin routing or transboundary river inflow.
* **Calibrated Ether Formulation**:
  $$v = \frac{1}{n} R_h^{2/3} S^{1/2}, \quad Q = v \cdot A$$
  * Allogenic transboundary catchment boundary condition for windowed historical river basins (Nile, Tigris-Euphrates, Indus).
* **Root Cause & Justification**:
  * Riparian civilizational floodplains depend on upstream snowmelt/catchment precipitation outside arid lowland cells.
* **Empirical Validation Grounding**:
  * UN FAO AQUASTAT; GRDC Global Runoff Data Centre.

---

### Entry 105: Zimov Pleistocene Mammoth Steppe Overkill & Trophic Shrub Encroachment
* **Date**: November 2, 2026
* **Subsystem**: `MegafaunaEcosystemEngine.java`
* **Canonical Literature**:
  * Zimov, S. A., et al. (2012). *Steppe-tundra transition to forest and moss tundra: driven by Pleistocene megafaunal extinction*. Ecosystems.
* **Problem in Previous Implementation**:
  * Verified continuous foraging pressure ratio and soil carbon decay.
* **Calibrated Ether Formulation**:
  $$\Phi_{\text{hunt}} = \frac{N \cdot \omega_{\text{wild}}}{B_{\text{wild}}}, \quad \omega_{\text{wild}} = \frac{1}{1 + B_{\text{live}} / (0.5 N)}$$
  $$\Delta B_{\text{wild}} = - \min(0.20, 0.02 \cdot \Phi_{\text{hunt}}) \cdot \Delta t \cdot B_{\text{wild}}$$
* **Root Cause & Justification**:
  * Pastoral substitution decouples hunting pressure across eras, matching empirical megafaunal extirpation timings.
* **Empirical Validation Grounding**:
  * Sandom et al. (2014); Barnosky et al. (2004).


---

### Entry 106: Milankovitch Insolation Forcing & High-Latitude Polar Amplification
* **Date**: November 2, 2026
* **Subsystem**: `MilankovitchOrbitalEngine.java`
* **Canonical Literature**:
  * Milankovitch, M. (1941). *Kanon der Erdbestrahlung und seine Anwendung auf das Eiszeitenproblem*. Royal Serbian Academy Special Publication 133.
  * Berger, A. (1978). *Long-term variations of daily insolation and Quaternary climatic changes*. Journal of the Atmospheric Sciences, 35(12), 2362-2367.
* **Problem in Previous Implementation**:
  * Temperature was iteratively shifted by `localShift * 0.01` per tick without equilibrium relaxation.
* **Calibrated Ether Formulation**:
  $$S(\phi, \delta) = \frac{S_0}{\pi} \left(\frac{1 + e\cos\nu}{1 - e^2}\right)^2 \left[H_0 \sin\phi \sin\delta + \cos\phi \cos\delta \sin H_0\right]$$
  $$T(t + \Delta t) = T(t) + (T_{\text{target}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{\tau}\right)\right)$$
* **Root Cause & Justification**:
  * Thermal relaxation towards orbital insolation equilibrium prevents numerical runaway.
* **Empirical Validation Grounding**:
  * Berger (1978); EPICA Dome C ice core records.

---

### Entry 107: Stratospheric Soot Solar Attenuation & Nuclear Winter Freezing
* **Date**: November 2, 2026
* **Subsystem**: `NuclearWarfareClimateEngine.java`
* **Canonical Literature**:
  * Robock, A., et al. (2007). *Climatic consequences of regional nuclear conflicts*. Atmospheric Chemistry and Physics, 7(8), 2003-2012.
* **Problem in Previous Implementation**:
  * Subtracted temperature anomaly iteratively each tick, causing -150°C global freeze.
* **Calibrated Ether Formulation**:
  $$\mathcal{T}_{\text{solar}} = \exp(-\tau_{\text{soot}})$$
  $$\Delta T_{\text{cool}} = -15.0 \cdot (1.0 - \mathcal{T}_{\text{solar}})$$
  $$T(t + \Delta t) = T(t) + (T_{\text{base}} + \Delta T_{\text{cool}} - T(t)) \cdot \kappa_{\text{relax}}$$
* **Root Cause & Justification**:
  * Formulates soot-induced cooling as a bounded anomaly relative to base insolation.
* **Empirical Validation Grounding**:
  * Robock et al. (2007); Turco et al. (1983) TTAPS study.

---

### Entry 108: Bihouix Ore Grade Commutation Exponent & Subsurface Extraction Work
* **Date**: November 2, 2026
* **Subsystem**: `OreGradeThermodynamicsEngine.java`
* **Canonical Literature**:
  * Bihouix, P. (2014). *L'âge des low-tech*. Seuil.
* **Problem in Previous Implementation**:
  * Verified ore concentration decay and mechanical work deduction.
* **Calibrated Ether Formulation**:
  $$E_{\text{extract}} = E_0 \cdot \left(\frac{C_{\text{ref}}}{C_{\text{ore}}}\right)^{1.5}$$
* **Root Cause & Justification**:
  * Exponential energy escalation as ore grade approaches crustal background concentration.
* **Empirical Validation Grounding**:
  * USGS Mineral Commodity Summaries; Bihouix (2014).

---

### Entry 109: CFC Stratospheric Photolysis & Crop Photosynthetic UV-B Attenuation
* **Date**: November 2, 2026
* **Subsystem**: `OzoneLayerDepletionEngine.java`
* **Canonical Literature**:
  * Molina, M. J., & Rowland, F. S. (1974). *Stratospheric sink for chlorofluoromethanes: chlorine atom-catalysed destruction of ozone*. Nature, 249(5460), 810-812.
* **Problem in Previous Implementation**:
  * Crop damage multiplier lacked time-step scaling $\Delta t$.
* **Calibrated Ether Formulation**:
  $$B_{\text{agri}}(t + \Delta t) = B_{\text{agri}}(t) \cdot \exp(-0.999 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Time-step invariant cellular DNA UV-B damage decay.
* **Empirical Validation Grounding**:
  * Molina & Rowland (1974); UNEP Ozone Secretariat.

---

### Entry 110: Cryospheric Permafrost Thaw & Methane Outgassing Kinetics
* **Date**: November 2, 2026
* **Subsystem**: `PermafrostThawEngine.java`
* **Canonical Literature**:
  * Schuur, E. A., et al. (2015). *Climate change and the permafrost carbon feedback*. Nature, 520(7546), 171-179.
* **Problem in Previous Implementation**:
  * Methane release and soil carbon loss lacked $\Delta t$ scaling.
* **Calibrated Ether Formulation**:
  $$\dot{M}_{\text{CH4}} = \max(0.0, T) \cdot 0.15 \cdot \Delta t$$
  $$\text{SOC}(t + \Delta t) = \max(5.0, \text{SOC}(t) - 0.20 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Continuous permafrost carbon release scaled to elapsed decimal years.
* **Empirical Validation Grounding**:
  * Schuur et al. (2015).

---

### Entry 111: Physical Superconductivity Work Transmission & Photosynthetic Multipliers
* **Date**: November 2, 2026
* **Subsystem**: `PhysicalLawEngine.java`
* **Canonical Literature**:
  * Bardeen, J., Cooper, L. N., & Schrieffer, J. R. (1957). *Theory of superconductivity*. Physical Review, 108(5), 1175.
* **Problem in Previous Implementation**:
  * Room-temperature superconductivity applied unscaled 1.05 compound multiplier every tick.
* **Calibrated Ether Formulation**:
  $$W(t + \Delta t) = W(t) \cdot (1.0 + 0.05 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Eliminates discrete time-step bias in exploratory alternate physical constants.
* **Empirical Validation Grounding**:
  * BCS Theory (1957).

---

### Entry 112: Coulomb Dry Friction vs Navier-Stokes Hydrodynamic Transport Work
* **Date**: November 2, 2026
* **Subsystem**: `PhysicsTransportEngine.java`
* **Canonical Literature**:
  * Coulomb, C. A. (1785). *Théorie des machines simples*. Mémoires de Mathématique et de Physique de l'Académie Royale des Sciences.
* **Problem in Previous Implementation**:
  * Verified mechanical freight work $W = \mu m g d$ deduction in GJ.
* **Calibrated Ether Formulation**:
  $$W_{\text{transport}} = \mu_{\text{biome}} \cdot m_{\text{cargo}} \cdot g \cdot d$$
  $$\mu_{\text{water}} \approx 0.001 \ll \mu_{\text{land}} \in [0.05, 0.25]$$
* **Root Cause & Justification**:
  * Direct mechanical work replaces heuristic abstract trade transaction costs.
* **Empirical Validation Grounding**:
  * Coulomb (1785); Bairoch (1997).

---

### Entry 113: Rockström-Steffen 9 Planetary Boundaries Resilience Matrix
* **Date**: November 2, 2026
* **Subsystem**: `PlanetaryBoundariesEngine.java`
* **Canonical Literature**:
  * Rockström, J., et al. (2009). *A safe operating space for humanity*. Nature, 461(7263), 472-475.
  * Steffen, W., et al. (2015). *Planetary boundaries: Guiding human development on a changing planet*. Science, 347(6223).
* **Problem in Previous Implementation**:
  * Verified comprehensive 9-boundary assessment vector.
* **Calibrated Ether Formulation**:
  $$\mathbf{R}_{\text{boundaries}} = \langle R_{\text{clim}}, R_{\text{bio}}, R_{\text{water}}, R_{\text{npk}}, R_{\text{land}}, R_{\text{ocean}}, R_{\text{ozone}}, R_{\text{aerosol}}, R_{\text{chem}} \rangle \in [0, 1]^9$$
* **Root Cause & Justification**:
  * Systematic real-time tracking of biospheric safe operating space.
* **Empirical Validation Grounding**:
  * Rockström et al. (2009); Steffen et al. (2015).


---

### Entry 114: Betz Limit Aerodynamics & Top-of-Atmosphere Solar Transmittance
* **Date**: November 2, 2026
* **Subsystem**: `RenewableEnergyPhysicsEngine.java`
* **Canonical Literature**:
  * Betz, A. (1920). *Das Maximum der theoretisch möglichen Ausnützung des Windes durch Windmotoren*.
* **Problem in Previous Implementation**:
  * Verified Betz limit $C_p = 0.593$ and TOA solar flux $S_0 = 1361\text{ W/m}^2$.
* **Calibrated Ether Formulation**:
  $$P_{\text{wind}} = \frac{1}{2} \rho_{\text{air}} v^3 \cdot 0.593, \quad S_{\text{surf}} = S_0 \cos\theta \cdot 0.70$$
* **Root Cause & Justification**:
  * Static invariant caching prevents unnecessary recomputation of fixed astronomical baselines.
* **Empirical Validation Grounding**:
  * Betz (1920); Kopp & Lean (2011).

---

### Entry 115: Circular Economy Capital Recovery & Enthalpy Discount
* **Date**: November 2, 2026
* **Subsystem**: `ResourceRecyclingEngine.java`
* **Canonical Literature**:
  * Ayres, R. U. (1994). *Industrial Ecology: Towards Closing the Materials Cycle*. Edward Elgar Publishing.
* **Problem in Previous Implementation**:
  * Verified recycling reduces physical capital depreciation rather than creating perpetual compound capital.
* **Calibrated Ether Formulation**:
  $$\eta_{\text{recyc}} = \min(1.0, K/5000) \cdot \min(0.85, 0.12 \cdot T)$$
  $$\Delta K_{\text{recyc}} = K \cdot \eta_{\text{recyc}} \cdot 0.025 \cdot \Delta t$$
* **Root Cause & Justification**:
  * Recycling operates as an offset against physical 4% annual depreciation, bounded below 100%.
* **Empirical Validation Grounding**:
  * Ayres (1994); UNEP International Resource Panel.

---

### Entry 116: Eustatic Sea Level Biome Transitions & Continental Shelf Emergence
* **Date**: November 2, 2026
* **Subsystem**: `SeaLevelTransitionEngine.java`
* **Canonical Literature**:
  * Lambeck, K., et al. (2014). *Sea level and global ice volumes from the Last Glacial Maximum to the Holocene*. PNAS, 111(43), 15296-15303.
* **Problem in Previous Implementation**:
  * Verified dynamic elevation offset thresholding for Doggerland/Beringia and marine submersion.
* **Calibrated Ether Formulation**:
  $$\text{Submerged} \iff z_{\text{elev}} < \Delta S_L$$
* **Root Cause & Justification**:
  * Dynamically morphs continental shelves between marine and terrestrial biomes during glacial regressions.
* **Empirical Validation Grounding**:
  * Lambeck et al. (2014); GEBCO 2024 bathymetry.

---

### Entry 117: Cultivar Artificial Selection & Photosynthetic Yield Ceilings
* **Date**: November 2, 2026
* **Subsystem**: `SelectiveBreedingEngine.java`
* **Canonical Literature**:
  * Evans, L. T. (1993). *Crop Evolution, Adaptation and Yield*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Verified Green Revolution breeding factor bounded by technological era and cell surface area.
* **Calibrated Ether Formulation**:
  $$\lambda_{\text{yield}}(T) = 1.0 + \min(2.5, 0.35 \cdot T)$$
  * Strict prehistoric invariant baseline: $\lambda_{\text{yield}} \equiv 1.0$ for $T < 1.0$.
* **Root Cause & Justification**:
  * Artificial breeding gains require settled agrarian cultivar domestication, dormant during foraging eras.
* **Empirical Validation Grounding**:
  * Evans (1993); Evenson & Gollin (2003) Science.

---

### Entry 118: Onsager Reciprocal Demographic Migration & Topological Cache
* **Date**: November 2, 2026
* **Subsystem**: `ThermodynamicMigrationEngine.java`
* **Canonical Literature**:
  * Onsager, L. (1931). *Reciprocal relations in irreversible processes I & II*. Physical Review, 37(4), 405; 38(12), 2265.
* **Problem in Previous Implementation**:
  * Verified $O(1)$ precomputed topological adjacency and free energy potential flux.
* **Calibrated Ether Formulation**:
  $$\Phi_i = \frac{\text{Food}_i + \text{Water}_i}{N_i + 1}, \quad J_{ij} = M_0 (\Phi_j - \Phi_i) N_i$$
* **Root Cause & Justification**:
  * Thermodynamic free energy gradient drives human migratory flow without unseeded stochastic jitter.
* **Empirical Validation Grounding**:
  * Onsager (1931); Ravenstein (1885) Laws of Migration.

---

### Entry 119: AMOC Convective Collapse & Mid-Latitude Thermal Equilibrium Drop
* **Date**: November 2, 2026
* **Subsystem**: `ThermohalineOceanEngine.java`
* **Canonical Literature**:
  * Rahmstorf, S. (2002). *Ocean circulation and climate during the past 120,000 years*. Nature, 419(6903), 207-214.
* **Problem in Previous Implementation**:
  * Subtracted 8°C iteratively each tick during AMOC collapse, freezing mid-latitudes to absolute zero.
* **Calibrated Ether Formulation**:
  $$T_{\text{target}} = T_{\text{baseline}} - 8.0^\circ\text{C}$$
  $$T(t + \Delta t) = T(t) + (T_{\text{target}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{2.0}\right)\right)$$
* **Root Cause & Justification**:
  * AMOC collapse sets an equilibrium high-latitude cooling target relaxed with oceanic thermal inertia.
* **Empirical Validation Grounding**:
  * Rahmstorf (2002); Younger Dryas Greenland ice core records.

---

### Entry 120: Stommel 2-Box Overturning AMOC Bifurcation & Salinity Feedback
* **Date**: November 2, 2026
* **Subsystem**: `ThermohalineStommelAMOCEngine.java`
* **Canonical Literature**:
  * Stommel, H. (1961). *Thermohaline convection with two stable regimes of flow*. Tellus, 13(2), 224-230.
* **Problem in Previous Implementation**:
  * Shifted local temperature iteratively each tick without target equilibrium.
* **Calibrated Ether Formulation**:
  $$q = C \cdot [\alpha_T (T_{\text{eq}} - T_{\text{pole}}) - \beta_S (S_{\text{eq}} - S_{\text{pole}})]$$
  $$T(t + \Delta t) = T(t) + (T_{\text{target}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{2.0}\right)\right)$$
* **Root Cause & Justification**:
  * Henry Stommel's canonical non-linear density overturning with bistable hysteresis.
* **Empirical Validation Grounding**:
  * Stommel (1961); Ditlevsen & Ditlevsen (2023) Nature Communications.

---

### Entry 121: Pleistocene Rewilding Snow Trampling & Albedo Optics Bound
* **Date**: November 2, 2026
* **Subsystem**: `TrophicEcosystemEngine.java`
* **Canonical Literature**:
  * Zimov, S. A. (2005). *Pleistocene Park: return of the mammoth's ecosystem*. Science, 308(5723), 796-798.
* **Problem in Previous Implementation**:
  * Albedo was incremented iteratively each tick, saturating rapidly to 0.80.
* **Calibrated Ether Formulation**:
  $$\alpha(t) = \alpha_{\text{base}} + \min(0.04, 0.002 \cdot \rho_{\text{megafauna}})$$
  $$\text{SOC}(t + \Delta t) = \text{SOC}(t) + 0.05 \cdot \rho_{\text{megafauna}} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Grassland maintenance bounds albedo increase within optical physical limits of steppe vegetation.
* **Empirical Validation Grounding**:
  * Zimov (2005); Macias-Fauria et al. (2020) Philosophical Transactions B.

---

### Entry 122: Oke Boundary-Layer Urban Heat Island Climatology
* **Date**: November 2, 2026
* **Subsystem**: `UrbanThermodynamicsEngine.java`
* **Canonical Literature**:
  * Oke, T. R. (1982). *The energetic basis of the urban heat island*. Quarterly Journal of the Royal Meteorological Society, 108(455), 1-24.
* **Problem in Previous Implementation**:
  * Added UHI heat increment iteratively every tick to cell temperature.
* **Calibrated Ether Formulation**:
  $$\Delta T_{\text{UHI}} = 0.5 \cdot \log_{10}\left(\frac{N}{1000}\right)$$
  $$T(t + \Delta t) = T(t) + (T_{\text{base}} + \Delta T_{\text{UHI}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{0.5}\right)\right)$$
* **Root Cause & Justification**:
  * UHI is an equilibrium thermal anomaly of built environment thermal inertia, not an infinite heater.
* **Empirical Validation Grounding**:
  * Oke (1982); Manoli et al. (2019) Nature.

---

### Entry 123: AI Autonomous Planetary Regulation & Productive Frontier Bounds
* **Date**: November 2, 2026
* **Subsystem**: `AiAutonomousRegulationPureEngine.java`
* **Canonical Literature**:
  * Wiener, N. (1948). *Cybernetics: Or Control and Communication in the Animal and the Machine*. MIT Press.
* **Problem in Previous Implementation**:
  * Work output compounded exponentially at 5% annually without production frontier ceiling.
* **Calibrated Ether Formulation**:
  $$W_{\max} = 1.5 N + 0.8 K$$
  $$W(t + \Delta t) = W(t) + (W_{\max} - W(t)) \cdot (1 - \exp(-0.05 \cdot \Delta t))$$
* **Root Cause & Justification**:
  * Autonomous optimization eliminates logistical deadweight loss but cannot exceed physical capital/labor capacity.
* **Empirical Validation Grounding**:
  * Wiener (1948); Leontief (1986).

---

### Entry 124: Marvin Harris Cultural Materialism Infrastructure Cascade
* **Date**: November 2, 2026
* **Subsystem**: `CulturalMaterialismPureEngine.java`
* **Canonical Literature**:
  * Harris, M. (1979). *Cultural Materialism: The Struggle for a Science of Culture*. Random House.
* **Problem in Previous Implementation**:
  * Verified infrastructure food deficit causing structural economic capital decay.
* **Calibrated Ether Formulation**:
  $$\frac{\text{Food}}{N} < 0.05 \implies K(t + \Delta t) = K(t) \cdot \max(0.0, 1.0 - 0.10 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Caloric deficit at the base of the infrastructural pyramid forces political and capital collapse.
* **Empirical Validation Grounding**:
  * Harris (1979); Diamond (2005) Collapse.

---

### Entry 125: Kardashev Planetary Power Classification
* **Date**: November 2, 2026
* **Subsystem**: `KardashevPureEngine.java`
* **Canonical Literature**:
  * Kardashev, N. S. (1964). *Transmission of Information by Extraterrestrial Civilizations*. Soviet Astronomy, 8, 217.
  * Sagan, C. (1973). *The Cosmic Connection*. Doubleday.
* **Problem in Previous Implementation**:
  * Verified closed-form logarithmic power conversion.
* **Calibrated Ether Formulation**:
  $$K = \frac{\log_{10}(P_{\text{watts}}) - 6.0}{10.0}$$
* **Root Cause & Justification**:
  * Type I planetary civilizational threshold at $P = 10^{16}\text{ W}$.
* **Empirical Validation Grounding**:
  * Kardashev (1964); Sagan (1973).


---

### Entry 126: Hamilton Kin Selection Outgroup Scarcity Stress
* **Date**: November 2, 2026
* **Subsystem**: `KinSelectionHamiltonEngine.java`
* **Canonical Literature**:
  * Hamilton, W. D. (1964). *The genetical evolution of social behaviour I and II*. Journal of Theoretical Biology, 7(1), 1-52.
  * Bowles, S. (2006). *Group competition, reproductive leveling, and the evolution of human altruism*. Science, 314(5805), 1569-1572.
* **Problem in Previous Implementation**:
  * Verified continuous logistic food stress response on defensive labor.
* **Calibrated Ether Formulation**:
  $$r \cdot B > C$$
  $$\sigma_{\text{scarcity}} = \frac{1}{1 + \exp(20.0 \cdot (\text{food/capita} - 0.05))}$$
* **Root Cause & Justification**:
  * Continuous outgroup hostility emergence under existential resource scarcity.
* **Empirical Validation Grounding**:
  * Hamilton (1964); Bowles (2006).

---

### Entry 127: Hydraulic Polder Reclamation & Solar TOA Radiance Bound
* **Date**: November 2, 2026
* **Subsystem**: `LandReclamationEngine.java`
* **Canonical Literature**:
  * van Veen, J. (1948). *Dredge, Drain, Reclaim: The Art of a Nation*. Martinus Nijhoff.
* **Problem in Previous Implementation**:
  * Solar energy and soil carbon were scaled by days without decimal year conversion, exceeding TOA limits.
* **Calibrated Ether Formulation**:
  $$\text{SOC}(t + \Delta t) = \min(120.0, \text{SOC}(t) + 20.0 \cdot \Delta t)$$
  $$S_{\text{solar}} = \min(1000.0, S_{\text{solar}} + 50.0 \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Polder land formation respects astronomical top-of-atmosphere solar flux constraints.
* **Empirical Validation Grounding**:
  * van Veen (1948); Dutch Zuiderzee Works empirical data.

---

### Entry 128: Archimedean Hydrodynamic Transport & Solow Maritime Capital Saturation
* **Date**: November 2, 2026
* **Subsystem**: `MaritimeHighwayEngine.java`
* **Canonical Literature**:
  * Solow, R. M. (1956). *A contribution to the theory of economic growth*. The Quarterly Journal of Economics, 70(1), 65-94.
* **Problem in Previous Implementation**:
  * Verified Solow maritime capital saturation envelope.
* **Calibrated Ether Formulation**:
  $$K_{\text{maritime}}^{\text{eq}} = 65.0 \cdot T^{1.6}$$
  $$\text{saturation} = \text{clamp}\left(1.0 - \frac{K/N}{K_{\text{maritime}}^{\text{eq}}}, 0.02, 1.0\right)$$
* **Root Cause & Justification**:
  * Eliminates infinite capital accumulation on maritime coastlines through diminishing returns.
* **Empirical Validation Grounding**:
  * Solow (1956); Braudel (1972) The Mediterranean.

---

### Entry 129: Nordhaus DICE Quadratic Climate Damage Function
* **Date**: November 2, 2026
* **Subsystem**: `NordhausDicePureEngine.java`, `NordhausDiceHybridEngine.java`
* **Canonical Literature**:
  * Nordhaus, W. D., & Sztorc, P. (2013). *DICE 2013R: Introduction and User's Manual*. Yale University.
* **Problem in Previous Implementation**:
  * Verified canonical quadratic damage coupling directly into cellular capital.
* **Calibrated Ether Formulation**:
  $$D(T) = \min(0.50, 0.00236 \cdot \Delta T^2)$$
  $$K(t + \Delta t) = K(t) \cdot \exp(-D(T) \cdot \Delta t)$$
* **Root Cause & Justification**:
  * Textbook implementation of William Nordhaus's Nobel Prize-winning integrated assessment model.
* **Empirical Validation Grounding**:
  * Nordhaus & Sztorc (2013).

---

### Entry 130: Lahire Pro-Sociality Self-Domestication & Deep-Time Baseline
* **Date**: November 2, 2026
* **Subsystem**: `SelfDomesticationEngine.java`
* **Canonical Literature**:
  * Lahire, B. (2023). *Les structures fondamentales des sociétés humaines*. La Découverte.
  * Henrich, J. (2004). *Demography and cultural evolution: Why adaptive cultural processes produced maladaptive losses in Tasmania*. American Antiquity.
* **Problem in Previous Implementation**:
  * Self-domestication pushed Paleolithic foragers past technological era bounds.
* **Calibrated Ether Formulation**:
  $$\text{For } T \ge 1.0: \quad \Delta T = 0.002 \cdot \log_{10}(1 + \rho_{\text{pop}}) \cdot \Delta t$$
  $$\text{For } T < 1.0: \quad \Delta T \equiv 0.0$$
* **Root Cause & Justification**:
  * Enforces invariant prehistoric baseline: social learning in deep time shapes language and prosocial norms without artificial industrial technology jumps.
* **Empirical Validation Grounding**:
  * Lahire (2023); Henrich (2004).

---

### Entry 131: Buss Evolutionary Mating Skew & Young Adult Male Cohort Mobilization
* **Date**: November 2, 2026
* **Subsystem**: `SexualSelectionMatingEngine.java`
* **Canonical Literature**:
  * Buss, D. M. (1989). *Sex differences in human mate preferences: Evolutionary hypotheses tested in 37 cultures*. Behavioral and Brain Sciences, 12(1), 1-14.
  * Henrich, J., Boyd, R., & Richerson, P. J. (2012). *The puzzle of monogamous marriage*. Philosophical Transactions of the Royal Society B, 367(1589), 657-669.
* **Problem in Previous Implementation**:
  * Binary threshold `capital > 500` applied infinite work boost.
* **Calibrated Ether Formulation**:
  $$\text{skew} = \frac{1}{1 + \exp(-10.0 \cdot (G - 0.45))}$$
  $$W_{\text{mobilized}}^{\max} = W_0 \cdot \left[1.0 + 0.12 \cdot \text{skew} \cdot \left(\frac{N_{15-24}}{0.15 N}\right)\right]$$
* **Root Cause & Justification**:
  * Polygynous inequality drives young adult male high-risk striving, bounded strictly by demographic cohort size.
* **Empirical Validation Grounding**:
  * Buss (1989); Henrich et al. (2012).

---

### Entry 132: Meadows World3 Canonical 5-Subsystem Differential Feedback
* **Date**: November 2, 2026
* **Subsystem**: `World3PureEngine.java`, `World3HybridEngine.java`, `World3CouplingEngine.java`
* **Canonical Literature**:
  * Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). *The Limits to Growth*. Universe Books.
* **Problem in Previous Implementation**:
  * Verified textbook non-linear FCAOR capital sinks and persistent pollution assimilation.
* **Calibrated Ether Formulation**:
  $$\text{FCAOR} = 0.05 + 0.90 \cdot \left(1.0 - \frac{R}{R_0}\right)^2$$
  $$\text{LE} = 40.0 \cdot \min(M_{\text{food}}, M_{\text{heal}}, M_{\text{poll}}, M_{\text{crowd}})$$
* **Root Cause & Justification**:
  * Direct cellular tensor integration of the MIT Club of Rome global feedback model.
* **Empirical Validation Grounding**:
  * Meadows et al. (1972, 2004); Herrington (2021) Journal of Industrial Ecology.

---

### Entry 133: Predictive Cliodynamic Multi-Hazard Alert Evaluation
* **Date**: November 2, 2026
* **Subsystem**: `CliodynamicAdvisorEngine.java`
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
* **Problem in Previous Implementation**:
  * Verified real-time telemetry assessment across planetary boundaries.
* **Calibrated Ether Formulation**:
  $$\text{Alert} \iff \max(R_{\text{clim}}, R_{\text{water}}, R_{\text{npk}}, \tau_{\text{soot}}) > \theta_{\text{critical}}$$
* **Root Cause & Justification**:
  * Real-time localized advisory telemetry without altering underlying physical states.
* **Empirical Validation Grounding**:
  * Turchin (2003); I18n localization standard.

---

### Entry 134: Multilateral Atmospheric Quota Governance & Trade Coordination
* **Date**: November 2, 2026
* **Subsystem**: `CoGovernanceTradeEngine.java`
* **Canonical Literature**:
  * Ostrom, E. (1990). *Governing the Commons: The Evolution of Institutions for Collective Action*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Verified carbon treaty pollution capping.
* **Calibrated Ether Formulation**:
  $$P(t + \Delta t) = P(t) \cdot \exp(-0.02 \cdot \Delta t) \quad \text{for } P > 200$$
* **Root Cause & Justification**:
  * Institutional polycentric governance limits atmospheric tragedy of the commons.
* **Empirical Validation Grounding**:
  * Ostrom (1990); Montreal Protocol (1987).

---

### Entry 135: Sociological Value Vectors & Soil Organic Stewardship
* **Date**: November 2, 2026
* **Subsystem**: `CulturalSociologyEngine.java`
* **Canonical Literature**:
  * Inglehart, R., & Welzel, C. (2005). *Modernization, Cultural Change, and Democracy*. Cambridge University Press.
* **Problem in Previous Implementation**:
  * Verified cultural value vectors (Collectivism, Stewardship) modulating SOC labor.
* **Calibrated Ether Formulation**:
  $$\Delta \text{SOC} = (\nu_{\text{stewardship}} - 0.50) \cdot 0.10 \cdot \Delta t$$
* **Root Cause & Justification**:
  * Cultural attitudes towards ecological conservation dictate somatic maintenance work.
* **Empirical Validation Grounding**:
  * World Values Survey; Inglehart & Welzel (2005).

---

### Entry 136: Shannon Information Channel Capacity & Linguistic Drift Suppression
* **Date**: November 2, 2026
* **Subsystem**: `InformationEntropyEngine.java`
* **Canonical Literature**:
  * Shannon, C. E. (1948). *A mathematical theory of communication*. Bell System Technical Journal, 27(3), 379-423.
* **Problem in Previous Implementation**:
  * Verified Shannon capacity $C = B \log_2(1 + S/N)$ suppressing dialect drift.
* **Calibrated Ether Formulation**:
  $$C = B(T) \cdot \log_2\left(1 + \frac{S}{N}\right), \quad B(T) = 1.0 + T^3 \cdot 1000.0$$
* **Root Cause & Justification**:
  * High-bandwidth telecommunications eliminate geographic isolation and dialect divergence.
* **Empirical Validation Grounding**:
  * Shannon (1948); Cavalli-Sforza & Feldman (1981).

---

### Entry 137: In-Situ Resource Utilization (ISRU) Autarky & Off-World Logistics
* **Date**: November 2, 2026
* **Subsystem**: `IsruAutarkyAndSpaceColonizationEngine.java`
* **Canonical Literature**:
  * Zubrin, R. (1996). *The Case for Mars: The Plan to Settle the Red Planet and Why We Must*. Free Press.
* **Problem in Previous Implementation**:
  * Verified off-world regolith sintering and dome social cohesion (Asabiyyah).
* **Calibrated Ether Formulation**:
  $$\eta_{\text{isru}} = \text{clamp}(0.20 + 0.15 \cdot T, 0.20, 2.50)$$
  $$\Delta M_{\text{regolith}} = N \cdot 0.50 \cdot \eta_{\text{isru}} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Closed-loop environmental support and local mineral harvesting drive extraterrestrial autarky.
* **Empirical Validation Grounding**:
  * Zubrin (1996); NASA ISRU Capability Roadmap.

---

### Entry 138: Jacobsen-Adams Mesopotamian Salinization & Maas-Hoffman Yield Response
* **Date**: November 2, 2026
* **Subsystem**: `SoilSalinizationHydrologyEngine.java`
* **Canonical Literature**:
  * Jacobsen, T., & Adams, R. M. (1958). *Salt and Silt in Ancient Mesopotamian Agriculture*. Science, 128(3334), 1251-1258.
  * Maas, E. V., & Hoffman, G. J. (1977). *Crop salt tolerance-current assessment*. Journal of the Irrigation and Drainage Division, 103(2), 115-134.
* **Problem in Previous Implementation**:
  * Verified mass balance $\frac{d[\text{Salts}]}{dt} = \dot{S}_{\text{irr}} + \dot{S}_{\text{capillary}} - \dot{S}_{\text{drain}}$ and Maas-Hoffman yield attenuation.
* **Calibrated Ether Formulation**:
  $$Y(S) = Y_0 \cdot \max(0.10, 1.0 - 0.06 \cdot \max(0.0, S - 2.0))$$
* **Root Cause & Justification**:
  * Authentic historical model of the Sumerian agricultural collapse (-2400 to -1700 BCE).
* **Empirical Validation Grounding**:
  * Jacobsen & Adams (1958); FAO Irrigation and Drainage Paper 29.

---

### Entry 139: Closed-Loop Model Predictive AI Governance (Leviathan Optimizer)
* **Date**: November 2, 2026
* **Subsystem**: `SovereignAIGovernanceEngine.java`
* **Canonical Literature**:
  * Bostrom, N. (2014). *Superintelligence: Paths, Dangers, Strategies*. Oxford University Press.
* **Problem in Previous Implementation**:
  * Verified multi-mode AI feedback governance (Unified Leviathan, Geopolitical Competition, Soft Nudging).
* **Calibrated Ether Formulation**:
  $$\Delta P = (P - P_{\text{target}}) \cdot 0.15 \cdot \alpha_{\text{opt}} \cdot \Delta t$$
* **Root Cause & Justification**:
  * Cybernetic closed-loop policy interventions to stabilize planetary socio-ecological state vectors.
* **Empirical Validation Grounding**:
  * Wiener (1948); Bostrom (2014).

---

### Entry 140: Physical Historical Era Power Flux & Directed Technical Change
* **Date**: November 2, 2026
* **Subsystem**: `TechTreeEngine.java`
* **Canonical Literature**:
  * Allen, R. C. (2009). *The British Industrial Revolution in Global Perspective*. Cambridge University Press.
  * Acemoglu, D. (2002). *Directed technical change*. The Review of Economic Studies, 69(4), 781-809.
  * Henrich, J. (2004). *Demography and cultural evolution*. American Antiquity.
* **Problem in Previous Implementation**:
  * Verified power per capita, material yield strength $\sigma_{\text{yield}}$, and Boserup-Allen-Acemoglu bottleneck substitution.
* **Calibrated Ether Formulation**:
  $$\Delta T = \Delta T_{\text{ratchet}}(\sqrt{N}, K/N) + \Delta T_{\text{bottleneck}}(\Sigma_{\text{stress}}, \Psi_{\text{latent}})$$
* **Root Cause & Justification**:
  * Replaces abstract tech levels with physical thermodynamics and material science thresholds.
* **Empirical Validation Grounding**:
  * Allen (2009); Acemoglu (2002); Smil (2017).

---

### Entry 141: Planetary Terraforming Radiative Forcing Equilibrium Relaxation
* **Date**: November 2, 2026
* **Subsystem**: `TerraformingEngine.java`
* **Canonical Literature**:
  * Sagan, C. (1973). *Planetary engineering on Mars*. Icarus, 20(4), 513-514.
  * McKay, C. P., Toon, O. B., & Kasting, J. F. (1991). *Making Mars habitable*. Nature, 352(6335), 489-496.
* **Problem in Previous Implementation**:
  * Accumulated global temperature delta linearly each tick, causing runaway heating.
* **Calibrated Ether Formulation**:
  $$\Delta F_{\text{terra}} = 5.35 \ln\left(\frac{[\text{CO}_2]}{280}\right) + (\mu_{\text{mirror}} - 1.0) \cdot 1361.0$$
  $$T(t + \Delta t) = T(t) + (T_{\text{base}} + 0.8 \Delta F_{\text{terra}} - T(t)) \cdot \left(1 - \exp\left(-\frac{\Delta t}{5.0}\right)\right)$$
* **Root Cause & Justification**:
  * Planetary engineering warming is bounded by radiative equilibrium with atmospheric thermal inertia.
* **Empirical Validation Grounding**:
  * McKay et al. (1991); Sagan (1973).

---

### Entry 142: West-Bettencourt Urban Allometry & Metabolic Scaling
* **Date**: November 2, 2026
* **Subsystem**: `WestBettencourtAllometryEngine.java`
* **Canonical Literature**:
  * Bettencourt, L. M., Lobo, J., Helbing, D., Kühnert, C., & West, G. B. (2007). *Growth, innovation, scaling, and the pace of life in cities*. PNAS, 104(17), 7301-7306.
* **Problem in Previous Implementation**:
  * Verified super-linear socio-economic output ($N^{1.15}$) and sub-linear infrastructure network economy ($N^{0.85}$).
* **Calibrated Ether Formulation**:
  $$Y \propto N^{1.15}, \quad I_{\text{infra}} \propto N^{0.85}$$
* **Root Cause & Justification**:
  * Fractal geometry of urban distribution and social interaction networks.
* **Empirical Validation Grounding**:
  * Bettencourt et al. (2007); West (2017) Scale.
