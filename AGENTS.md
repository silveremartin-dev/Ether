# AGENT.MD: Project Philosophy & Development Directives for Ether

## 🎯 1. Core Project Identity & Scope
**Ether is a Physical-based Cliodynamic & Historical Planetary Simulation Engine targeting the Highest Level of Academic Precision and Scientific Rigor.**

The overarching directive of Ether is that **the simulation must strictly respect the highest standards of academic excellence and be fundamentally grounded in authentic, peer-reviewed physical, biological, thermodynamic, chemical, and ecological models**.

The primary goal of Ether is to simulate how **geography, physics, thermodynamics, chemistry, and biology shape human/alien civilizational history, demography, technology, trade, and collapse**.

### Boundary Principles:
* **Cliodynamic Physics (IN SCOPE)**: Physical equations that directly constrain or drive human societies (e.g. Energy Balance Climate Models, Soil N-P-K stoichiometry, Metal Smelting Enthalpy, Water Table Pumping Limits, Wet-Bulb Temperature Lethality, Mechanical Transport Work).
* **Astrophysical Bloat (OUT OF SCOPE / REJECTED)**: Superfluous stellar/optics physics with no impact on historical timescales ($10$ to $50\,000$ years), such as *Rayleigh sky scattering wavelengths*, *deep magnetic dipole solar wind stripping over 500 Myr*, or *micro-météo CFD fluid dynamics*.

---

## ⚡ 2. The Computational ROI Filter
Before adding any new parameter or simulation module, evaluate its **Computational ROI**:

$$\text{Computational ROI} = \frac{\text{Emergent Impact on History, Demographics, & Society}}{\text{CPU Complexity per Tick}}$$

* **High ROI (MUST HAVE)**: Direct analytical formulas $O(1)$ per cell driving mortality, food yield, or energy conversion.
* **Low ROI (REJECT)**: High CPU operations ($O(N^2)$ or fluid solver loops) producing unobservable aesthetic or astrophysic outputs.

---

## 🔒 3. Determinism, Seeded PRNG & Randomness Directives
* **Strict Bitwise Determinism**: When `strictDeterminism = true`, simulation trajectories MUST be 100% bit-identical for identical initial conditions.
* **Empirical Data Sources are Invariant & Authoritative**:
  * Empirical rasters and historical cartographic layers (NOAA ETOPO, GEBCO, WorldClim, HYDE 3.4, UNESCO WHYMAP, USGS MRDS, mineral databases) define the physical ground truth at $t = t_0$.
  * **Strict Ban on Random Variables in Data Ingestion**: It is STRICTLY FORBIDDEN to inject pseudo-random noise (`Random`, `Math.random()`, stochastic jitter) into raw empirical data sources, pre-loaded rasters, or historical boundary conditions.
* **Prohibition of Unseeded Random & Heuristic Noise**:
  * The use of unseeded `new Random()` or `Math.random()` is **STRICTLY PROHIBITED** across all simulation engines, kernels, and procedural generation pipelines.
  * In UI screens (e.g., Planet Creation, Climate, Geology, Scenario Setup), `new Random()` is permitted ONLY on explicit user button click actions to roll a new seed value into a text field.
* **Mandatory Seed Anchor & `DeterministicPRNG`**:
  * All procedural generation and simulation engines MUST systematically anchor their stochastic variables to the scenario seed (`scenario.getSeed()`) or screen-specific seed.
  * Spatial stochastic evaluations across cells MUST use the stateless, order-independent `DeterministicPRNG` (SplitMix64) keyed to `(seed, tick, cellIndex, streamId)`. This prevents cell iteration order or multi-threaded execution from causing trajectory divergence.
* **Biophysical State Variables are Analytical Closed-Form Equations**:
  * Fundamental physical and biological state variables (NPP, biomass stocks, edible forage, timber reserves, temperature, precipitation, evaporation) MUST derive analytically from peer-reviewed closed-form physical equations (e.g., Lieth/Miami NPP model $f(T, P)$). They must NEVER be computed using discrete `switch(biome)` tables with pseudo-random jitter.

---

## 📄 4. Technical Documentation Standard
* **Strict English Language Requirement**: ALL `README.md` files, technical documentation, architectural specifications, mathematical justifications, epoch provenance files, and source code comments MUST be written strictly in English.
* **Mandatory In-Code Javadoc & Algorithmic Commentary**: In addition to external documentation (`README.md`, `docs/*.md`), EVERY Java class, interface, record, enum, method, and field MUST be thoroughly documented directly in the code via rich Javadoc (including `@param`, `@return`, `@throws`, `@see`, physical units, $\LaTeX$ mathematical formulas, and algorithmic step comments).
* **Central Simulation Reference**: All equations, physical constants, historical eras, and heuristic catalog items MUST be documented in **`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`** in English.

---

## 🎨 5. User Interface, Localization & Accessibility Standards
1. **Full Localization (5 Languages: EN, FR, DE, ES, ZH) & Complete Technical Tooltips**:
   * ALL UI components (labels, sliders, buttons, combo box options, table headers, status indicators, tooltips, chart legends, and modal text) MUST be localized across all 5 supported languages (`EN`, `FR`, `DE`, `ES`, `ZH`). Absolutely NO hardcoded user-visible text in Java UI code.
   * EVERY single interactive UI element MUST feature a detailed mouse-over technical tooltip explaining the underlying simulation variable, physical formula, or feature mechanism.
2. **Strict High-Contrast & Consistent Theming**:
   * **Dark Theme**: Dark background with high-luminance (bright/light) text, icons, and control strokes.
   * **Light Theme**: Light background with low-luminance (dark) text, icons, and control strokes.
   * Low contrast combinations (e.g., dark blue/gray text on dark backgrounds, saturated blue combo boxes on dark panels) are strictly forbidden. High visual contrast is required systematically across all panels and themes.

---

## 🗺️ 6. Cartographic Tensor Provenance & Planetary Encapsulation Architecture
* **Planetary Encapsulation (`data/maps/ether/<planet>/`)**:
  * All planet-specific physical datasets, rasters, historical catalogs, and state variables are strictly encapsulated within their respective planetary root `data/maps/ether/<planet>/` (e.g., `data/maps/ether/earth/`).
  * **2D Spatial Rasters (`data/maps/ether/<planet>/<year>/`)**:
    * For every historical epoch (from -100,000 BP to present-day 2025 CE), rasters are stored as 2D spatial layers (Elevation, Density, Biomes, Aquifers, etc.).
    * Every directory contains `provenance_and_sources.json` and `README.md` detailing geophysical and demographic sources.
  * **Discrete Catalogs & Vector Geometries**:
    * `history/events/historical_events_catalog.json`: Historical event records (chronological shocks, wars, scientific milestones).
    * `history/treaties_and_transitions.json`: Gapless epoch transitions and geopolitical boundaries.
    * `history/paleo/`: Paleolithic vector geometries (`hominin_hearths.json`, `ice_sheets.json`, `migration_routes.json` with 19 global migration corridors).
    * `history/leaders/earth_historical_leaders.json`: Historical figures and catalytic leadership interventions.
  * **1D Continuous Variables & State Baselines (`data/maps/ether/<planet>/variables/`)**:
    * Stores scalar time-series trajectories and epoch state initialization vectors in 1 dimension ($t \to \text{value}$).
    * `variables/historical/paleo_sources_1d.json`: Deep Paleolithic ($t_0 < -10\,000\,\text{BP}$) initialized to **`PALEO_SOURCES`** (EPICA Dome C ice cores, PaleoClim v1.2, Biraben 1979 / Kremer 1993).
    * `variables/historical/hyde_maddison_historical_1d.json`: Historical eras ($-10\,000\,\text{BP} \le t_0 \le 2025\,\text{CE}$) initialized to **`HYDE_MADDISON`** (HYDE 3.4, Maddison Project 2020, ECMWF ERA5).
    * `variables/projections/`: Future trajectories ($t_0 \ge 2025\,\text{CE}$) selectable via UI ComboBox between **`SSP1`**, **`SSP2`** (default), **`SSP3`**, **`SSP4`**, **`SSP5`**, and **`ETHER_CALIBRATED`**.
  * **Legacy Compatibility**: Root-level directories (`data/variables/`, `data/history/`, `data/historical_events_catalog/`, `data/presets/leaders/`) are maintained as secondary fallbacks, with canonical priority given to `data/maps/ether/<planet>/`.
* **Zero Double Transition Invariant**: ERA5 observational data governs the recent past until the end of real observations (2024/2025), switching directly to the chosen prospective trajectory with zero ad-hoc intermediate approximations. Monotone cubic Hermite spline interpolation (PCHIP, Fritsch & Carlson 1980) guarantees monotonicity and eliminates Runge overshoot around volcanic and demographic crises (e.g., 1816 Tambora, 536 volcanic winter, 1347 Black Death).

---

## 🔬 7. Scientific Epistemic Falsification & Automated Benchmark Testing Directives
1. **Epistemic Laboratory Identity**:
   * Ether is not merely a generative procedural toy; it is an **epistemic falsification laboratory** designed to evaluate competing cliodynamic and macroeconomic hypotheses against empirical historical datasets (Seshat Global History Databank, Maddison Project, HYDE 3.4, UN FAO, EPICA ice cores).
2. **Two-Tier Ontological Separation**:
   * **Tier 1 (Core Model Physics)**: Invariant physical, chemical, thermodynamic, and biological conservation laws (Energy balance, Darcy piezometric flow, Farquhar photosynthesis, Stull wet-bulb lethality, Lotka metabolic energetics, material yield strength $\sigma_{\text{yield}}$).
   * **Tier 2 (Pluggable Cliodynamic Hypotheses)**: Contested sociological and institutional theories (Boserup vs. Malthus, Turchin SDT vs. Pinker, Smil Exergy vs. Nordhaus DICE, Ostrom vs. Hardin, Acemoglu vs. Geography, Scott vs. State Formation, Henrich vs. Static Retention).
3. **Automated Continuous Falsification Suite**:
   * Every competing paradigm MUST be formalized mathematically as coupled differential equations and benchmarked within **`ScientificModelFalsificationAndValidationSuite.java`**.
   * Tests MUST systematically evaluate parameter sweeps, bifurcation thresholds, domain validity bounds, and historical counter-examples.
   * All results and mathematical derivations MUST be fully documented in **`docs/SCIENTIFIC_MODEL_EVALUATION_AND_FALSIFICATION.md`**.

---

## 📏 8. Zero Arbitrary Hardcoding Directive & Deep-Time Invariant Baselines
1. **Absolute Ban on Arbitrary Magic Constants & Heuristic Shortcuts**:
   * ALL simulation parameters, scaling factors, thresholds, rates, and equations MUST be authentic, peer-reviewed physical, chemical, biological, thermodynamic, or macroeconomic models.
   * **Zero tolerance for approximations, shortcuts, fake constants, arbitrary multiplier tricks, or hardcoded magic numbers**.
   * Every equation must trace to documented scientific literature with its explicit derivation, physical units, and parameters.
   * **MANDATORY PRIOR CONSULTATION**: If a true empirical formula, closed-form differential equation, or peer-reviewed law is unknown or missing, it is STRICTLY FORBIDDEN to improvise a heuristic formula or hardcoded shortcut. You MUST pause and discuss the formulation directly with the user before introducing any working hypothesis.
2. **Deep-Time Invariant Boundary Initialization (-3,000,000 BP to -10,000 BP)**:
   * For prehistoric, Pleistocene, and deep Paleolithic epochs (from $-3\,000\,000$ to $-10\,000$ BP) where formal civilizational infrastructure did not exist:
     * **Literacy Rate**: Invariant baseline $\equiv 0.0\%$ (writing emerges only with proto-cuneiform/hieroglyphs $\sim -3400$ BCE).
     * **Urbanization Rate**: Invariant baseline $\equiv 0.0\%$ (permanent agglomerations $> 5\,000$ hab emerge only in Uruk/Eridu/Jericho $\sim -4000$ BCE).
     * **Institutional Administrative Bureaucracy & Sovereign Debt**: Invariant baseline $\equiv 0.0$.
     * **Currency Debasement & Monetary Elite Indices**: Invariant baseline $\equiv 0.0$.
     * **Anthropogenic Deforestation & Industrial Fossil Energy**: Invariant baseline $\equiv 0.0\%$.
     * **Extractive Metal Smelting**: Invariant baseline $\equiv 0.0\text{ kg/capita}$ (emerges in Chalcolithic $\sim -5000$ BCE).
   * All benchmark catalogs, kernels, and simulation initializers MUST enforce these deep-time baselines across the full $-3\,000\,000$ to $-10\,000$ BP horizon. Any variable outside this historical domain must be anchored to its physical or biological baseline, and never allowed to float or drift to arbitrary non-zero values.


---

## ⚖️ 9. Scientific Parametric Deviation Registry & Epistemic Calibration Provenance
1. **Universal Validity & Invariance Across Epochs**:
   * Any mathematical adjustment, scaling factor, or parameter revision made to a published academic model (e.g., Turchin's SDT, Solow-Swan capital growth, Omran's epidemiological transition, Sandom/Barnosky megafauna, Buringh & van Zanden literacy) MUST possess **universal structural validity across all historical epochs and spatial locations**.
   * Epoch-specific ad-hoc hacks, local conditional tweaks, or calendar-dependent magic coefficients are strictly prohibited.
2. **Mandatory Documentation of Every Departure from Canonical Literature**:
   * Academic authors developed their models with great empirical rigor; any divergence between an author's canonical model and simulation results is a profound epistemic signal that demands deep scrutiny.
   * Whenever a canonical author constant $k_{\text{canonical}}$ is revised or adapted to $k_{\text{ether}}$ (for instance, due to spatial grid discretization, unit translation, or non-linear aggregation over the contiguous H3 mesh), this deviation MUST:
     1. Be explicitly commented in the source code Javadoc with `@see`, original literature citation, and derivation.
     2. Be documented with its mathematical justification in **`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`**.
     3. Be systematically registered in the dedicated central synthesis document: **`docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md`**.
3. **Parametric Deviation Registry Structure**:
   * Every entry in `docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md` must detail:
     * **Model & Author**: Original publication (e.g., Peter Turchin 2016 *Ages of Discord*).
     * **Original Canonical Formulation & Parameter Value**: The theoretical equation and published constant.
     * **Calibrated Simulation Formulation & Value**: The exact formula used in Ether.
     * **Academic Justification / Root Cause of Divergence**: Why the canonical form required spatial/temporal adaptation (e.g., transition from discrete national index to local continuous cellular tensor).
     * **Empirical Validation Grounding**: Empirical historical datasets (Seshat, Maddison, HYDE 3.4, EPICA) confirming the accuracy of the calibrated formulation.

---

## ⚛️ 10. The Physical Observables Invariant, $C^1$ Smoothness & Ban on State-Level Technology
1. **Primary Biophysical Observables as the Sole Dynamical State**:
   * The fundamental state of any simulation cell (`H3Cell`, `WorldBuffer`) MUST consist **exclusively of conserved physical, chemical, thermodynamic, and biological observables**:
     * Biological masses ($N$ human/agent population, $B_{\text{agr}}$ agricultural biomass, $B_{\text{nat}}$ wild biomass).
     * Material and geological stocks (soil depth, N-P-K mineral reserves, groundwater hydraulic head, coal, iron, hydrocarbons).
     * Physical built capital stock ($K$, measured in physical units of structural materials and machinery).
     * Useful energy and exergy flux ($P_{\text{watts}}$, measured in Joules/second or Watts).
     * Thermodynamic state variables (temperature $T$ in Kelvin, moisture in mm, atmospheric $\text{CO}_2$ in ppm).
   * **Absolute Ban on Technology as a State Variable**:
     * Technology is NOT a physical substance and CANNOT be stored as an independent dynamical state variable (e.g., `float[] technologyLevel` in `WorldBuffer` or `Double technologyLevel` in `H3Cell`).
     * It is strictly forbidden to mutate or increment technology via procedural updates (`cell.setTechnologyLevel(...)`).
     * Any technological metric is strictly a **derived a posteriori telemetric diagnostic** computed from physical observables:
       $$T_{\text{emergent}} = f\left(\frac{P_{\text{watts}}}{N}, \; \frac{K_{\text{active}}}{N}\right)$$
       and MUST NEVER feed back into Tier 1 or Tier 2 equations as an artificial gating variable.
2. **Strict $C^1$ Differentiability & Absolute Ban on Cliff Effects**:
   * Nature does not feature infinite-gradient discontinuities at macro scales.
   * All regime transitions, phase changes, and behavioral shifts MUST be mathematically continuous to at least the first derivative ($C^1$ smoothness), using smooth Fermi-Dirac sigmoids $\sigma(x; x_0, s) = \frac{1}{1 + \exp(-(x - x_0)/s)}$ or hyperbolic tangents $\tanh((x - x_0)/s)$.
   * **Strictly Prohibited**: Discontinuous step functions, boolean gating cliffs (`if (tech < 1.25)`, `if (year > 1850)`), and piecewise non-differentiable thresholds. All variables $x$ driving sigmoids must be physical observables (power per capita, capital density, temperature, moisture).
3. **Universality Across Eras and Collective Organisms (Insects, Humans, Aliens)**:
   * All macrosociological equations must hold true universally for any dissipative structure of living agents:
     * Human civilizations across deep time (from Paleolithic hunter-gatherers to modern grids).
     * Non-human collective biosystems (eusocial insects: ant supercolonies, termite mounds, bee hives).
     * Theoretical extraterrestrial intelligent biosystems.
   * Across all these systems, the physical principles remain invariant: structural capital $K$ depreciates via entropy ($\delta K$), energetic metabolism requires continuous power capture ($P$), and organizational complexity scales with demographic network size ($N$).
4. **First-Order Physical Telemetry Invariant & Ban on Cascading Synthetic Scalars**:
   * Telemetry snapshots (`MetricSnapshot`, `HistorySnapshot`, `SimulationMetricsAggregator`) MUST record **first-order conserved physical observables** directly from cellular buffers ($N$, $K$, $P_{\text{watts}}$, $B_{\text{nat}}$, $B_{\text{agr}}$, $B_{\text{livestock}}$, potable water, smelted minerals, occupied surface area, $\text{CO}_2$, Lorenz Gini, institutional stability).
   * It is **STRICTLY FORBIDDEN** to cascade macroscopic or post-hoc indicators (GWP, energy, urbanization, literacy, deforestation, soil erosion, freight cost, EROEI, life expectancy) through a synthetic intermediate scalar such as `avgTech` or `getAverageTechnology()`, or to leak ground-truth benchmark tables / calendar years (`year <= 1492`, `year >= 2025`) into simulated telemetry outputs.
   * When computing emergent *a posteriori* diagnostics (such as Kardashev scale or Garrett-Lotka emergent exergy-capital index), active capital $k_{\text{active}}$ MUST be coupled thermodynamically to active power dissipation $p = P_{\text{watts}}/N$ (Garrett 2011) so post-collapse depopulation ($N \to 0$) never produces spurious technological blow-up ($K/N \to \infty$).
