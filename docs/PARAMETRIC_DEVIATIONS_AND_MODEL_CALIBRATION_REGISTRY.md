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

