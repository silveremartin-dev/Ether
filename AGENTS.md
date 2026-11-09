# AGENT.MD: Project Philosophy & Development Directives for Ether

## 🎯 1. Core Project Identity & Scope
**Ether is an Integrative Computational Simulation Platform for Cliodynamics, Historical Macroeconomics, and Planetary Ecology.**

The overarching goal of Ether is to provide a **descriptive, exploratory, and heuristic computational sandbox** designed to simulate and study coupled interactions between **geography, physics, thermodynamics, resource ecology, and human civilizational history** over multi-millennial timescales.

### Epistemic Humility & Modeling Principles:
* **Descriptive & Exploratory Purpose**: Ether does not claim to be an absolute oracle of historical inevitability. It is a research tool and computational sandbox designed to explore counterfactuals, evaluate dynamic feedback loops, and observe emergent macro-patterns under thermodynamic and biophysical constraints.
* **Transparent Assumptions & Structural Limits**: Every macroscopic model is an abstraction. Discretization over spatial meshes (H3 hexagons), temporal aggregation (monthly/annual ticks), and mathematical simplifications of collective human behavior must be explicitly acknowledged as computational hypotheses, not dogmatic physical certitudes.
* **Two Delineated Domains**:
  1. **Core Historical Simulation (IN SCOPE - Canonical)**: Earth history from $-100\,000$ BP to $2025$ CE, anchored to authoritative empirical datasets (NOAA ETOPO, WorldClim, HYDE 3.4, Maddison Project 2020, ECMWF ERA5, EPICA ice cores).
  2. **Exploratory / Speculative Sandbox (HEURISTIC EXTENSION)**: Post-2025 prospective trajectories (SSPs, resource shocks) and off-Earth scenarios (lunar/martian ISRU autarky). These must be clearly categorized in code and UI as *heuristic exploratory counterfactuals*, and never conflated with historical ground-truth baselines.

---

## ⚡ 2. The Computational ROI Filter
Before adding any new parameter or simulation module, evaluate its **Computational ROI**:

$$\text{Computational ROI} = \frac{\text{Emergent Impact on History, Demographics, & Society}}{\text{CPU Complexity per Tick}}$$

* **High ROI (MUST HAVE)**: Direct analytical formulations $O(1)$ per cell driving mortality, food yield, or energy conversion.
* **Low ROI (REJECT)**: High CPU operations ($O(N^2)$ or dense fluid solver loops) producing unobservable aesthetic or hyper-specialized astrophysic outputs.

---

## 🔒 3. Determinism, Seeded PRNG & Randomness Directives
* **Strict Bitwise Determinism**: When `strictDeterminism = true`, simulation trajectories MUST be 100% bit-identical for identical initial conditions.
* **Empirical Data Sources are Invariant & Authoritative**:
  * Empirical rasters and historical cartographic layers define the physical ground truth at $t = t_0$.
  * **Strict Ban on Random Variables in Data Ingestion**: It is STRICTLY FORBIDDEN to inject pseudo-random noise (`Random`, `Math.random()`, stochastic jitter) into raw empirical data sources, pre-loaded rasters, or historical boundary conditions.
* **Prohibition of Unseeded Random & Heuristic Noise**:
  * The use of unseeded `new Random()` or `Math.random()` is **STRICTLY PROHIBITED** across all simulation engines, kernels, and procedural generation pipelines.
  * In UI screens (e.g., Planet Creation, Climate, Geology, Scenario Setup), `new Random()` is permitted ONLY on explicit user button click actions to roll a new seed value into a text field.
* **Mandatory Seed Anchor & `DeterministicPRNG`**:
  * All procedural generation and simulation engines MUST systematically anchor their stochastic variables to the scenario seed (`scenario.getSeed()`) or screen-specific seed.
  * Spatial stochastic evaluations across cells MUST use the stateless, order-independent `DeterministicPRNG` (SplitMix64) keyed to `(seed, tick, cellIndex, streamId)`. This prevents cell iteration order or multi-threaded execution from causing trajectory divergence.
* **Biophysical State Variables are Analytical Formulations Validated Across Non-Human Taxa**:
  * Fundamental physical and biological state variables (NPP, biomass stocks, edible forage, timber reserves, temperature, precipitation, evaporation, metabolic dissipation) derive from established scientific equations (e.g., Lieth/Miami NPP model $f(T, P)$, Kooijman DEB reserve-structure dynamics, Brown et al. MTE allometry, Sterner & Elser stoichiometry). They must never be computed using arbitrary discrete `switch(biome)` tables with pseudo-random jitter or anthropocentric phenomenological shortcuts.

---

## 📄 4. Technical Documentation Standard & Dual-Document Epistemic Architecture
* **Strict English Language Requirement**: ALL `README.md` files, technical documentation, architectural specifications, mathematical justifications, epoch provenance files, and source code comments MUST be written strictly in English.
* **Mandatory In-Code Javadoc & Algorithmic Commentary**: In addition to external documentation (`README.md`, `docs/*.md`), EVERY Java class, interface, record, enum, method, and field MUST be thoroughly documented directly in the code via rich Javadoc (including `@param`, `@return`, `@throws`, `@see`, physical units, $\LaTeX$ mathematical formulas, and algorithmic step comments).
* **The Dual-Document Epistemic Architecture**:
  The simulation's mathematical and scientific integrity is anchored by two complementary, authoritative central reference documents with strictly distinct epistemic scopes:
  1. **Canonical Equations Encyclopedia (`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`)**:
     * **The Canonical Baseline & Operational Norm**: Exhaustive reference of ALL simulation equations, physical conservation laws, biological invariants, thermodynamic principles, and state variables across the entire platform.
     * Documents theoretical derivations, physical dimensions, SI units, standard empirical constants, and academic citations when the model is implemented faithfully and directly as specified by the literature.
     * The overwhelming majority of equations in Ether reside exclusively here; implementing an equation cleanly as published requires NO deviation entry.
  2. **Mandatory Deviation & Calibration Registry (`docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md`)**:
     * **The Exception Log (Exceptions are NOT the Rule)**: Auditable registry reserved STRICTLY for genuine departures and non-standard parameterizations.
     * Applies ONLY when a canonical literature equation is under-specified for our spatial/temporal scale, or when we were forced to parameterize/reformulate differently from what the author expected (e.g., non-trivial analytical $O(1)$ closures replacing 3D continuum field solvers, continuous $C^1$ relaxations replacing discrete author cliffs, or non-obvious parameter calibrations).
     * Standard equations implemented cleanly as expected MUST NOT be logged here; a low count of entries relative to the total equation catalog reflects epistemic fidelity, not a lack of transparency.

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
  * **Discrete Catalogs & Vector Geometries (`data/history/ether/<planet>/`)**:
    * `data/history/ether/<planet>/events/historical_events_catalog.json`: Historical event records (chronological shocks, wars, scientific milestones).
    * `data/history/ether/<planet>/treaties/treaties_and_transitions.json`: Gapless epoch transitions and geopolitical boundaries.
    * `data/history/ether/<planet>/paleo/`: Paleolithic vector geometries (`hominin_hearths.json`, `ice_sheets.json`, `migration_routes.json` with 19 global migration corridors).
    * `data/history/ether/<planet>/leaders/earth_historical_leaders.json`: Historical figures and catalytic leadership interventions.
  * **1D Continuous Variables & State Baselines (`data/variables/`)**:
    * Stores scalar time-series trajectories and epoch state initialization vectors in 1 dimension ($t \to \text{value}$).
    * `data/variables/historical/paleo_sources_1d.json`: Deep Paleolithic ($t_0 < -10\,000\,\text{BP}$) initialized to **`PALEO_SOURCES`** (EPICA Dome C ice cores, PaleoClim v1.2, Biraben 1979 / Kremer 1993).
    * `data/variables/historical/hyde_maddison_historical_1d.json`: Historical eras ($-10\,000\,\text{BP} \le t_0 \le 2025\,\text{CE}$) initialized to **`HYDE_MADDISON`** (HYDE 3.4, Maddison Project 2020, ECMWF ERA5).
    * `data/variables/projections/`: Future trajectories ($t_0 \ge 2025\,\text{CE}$) selectable via UI ComboBox between **`SSP1`**, **`SSP2`** (default), **`SSP3`**, **`SSP4`**, **`SSP5`**, and **`ETHER_CALIBRATED`**.
* **Zero Double Transition Invariant**: ERA5 observational data governs the recent past until the end of real observations (2024/2025), switching directly to the chosen prospective trajectory with zero ad-hoc intermediate approximations. Monotone cubic Hermite spline interpolation (PCHIP, Fritsch & Carlson 1980) guarantees monotonicity and eliminates Runge overshoot around volcanic and demographic crises (e.g., 1816 Tambora, 536 volcanic winter, 1347 Black Death).

---

## 🔬 7. Hypothesis Exploration, Benchmarking & Sensitivity Testing
1. **Hypothesis Evaluation Framework**:
   * Ether serves as a computational laboratory to evaluate and compare competing cliodynamic and macroeconomic hypotheses against empirical historical datasets (Seshat Global History Databank, Maddison Project, HYDE 3.4, UN FAO, EPICA ice cores).
2. **Two-Tier Ontological Separation**:
   * **Tier 1 (Core Biophysical & Thermodynamic Invariants — Cross-Taxa Biological Foundations)**:
     * Conservation laws and exact analytical mechanics of physics, chemistry, thermodynamics, and bioenergetics.
     * **Strict Ban on Anthropocentric & Purely Empirical Curve-Fits**: Tier 1 models MUST NOT be formulated as ad-hoc, human-only equations or phenomenological regressions fitted to historical milestones. Instead, their mathematical foundations derive from exact bioenergetic and physical theories systematically validated across general, non-human biological populations and taxa across the tree of life, as well as conserved Earth system dynamics:
       * *Standard Dynamic Energy Budget (DEB, S.A.L.M. Kooijman 2010)*: Assimilation, metabolic reserve storage $E$, structural somatic growth $V$, $\kappa$-rule partitioning, and $C^1$ starvation survival validated on $>1\,000$ non-human animal and organismal species across the tree of life (`Add-my-Pet`), fundamentally replacing anthropocentric carrying-capacity ceilings ($K_{\text{verhulst}}$).
       * *Metabolic Theory of Ecology (MTE, Brown, West, Enquist et al. 2004)*: Allometric metabolic scaling ($B \propto M^{3/4}$) and Arrhenius-Boltzmann temperature kinetics ($\exp(-E_a / k_B T)$) driving biological rates, generation times, and basal subsistence across all taxa (microorganisms, plants, ectotherms, endotherms).
       * *Ecological Stoichiometry (Sterner & Elser 2002)*: Elemental mass conservation of C:N:P ratios and biochemical homeostasis across food webs, crop nutrition, and soil nutrient depletion.
       * *Constructal Flow Architecture (A. Bejan 1996, 2016)*: Flow geometry evolution minimizing flow resistance (Murray’s vascular tree law, $V \sim M^{1/6}$ animal cruising speed, Hack's fluvial law $L \sim A^{0.57}$).
       * *Unified Neutral Theory of Biodiversity & Biogeography (UNTB, S. P. Hubbell 2001)*: Zero-sum ecological drift, fundamental biodiversity numbers $\theta = 2 J_M \nu$, and power-law extinction debt across contiguous habitats.
       * *Maximum Entropy Production Principle (MEPP, A. Kleidon 2010)*: Non-equilibrium thermodynamic Carnot efficiency governing planetary wind generation, moisture transport, and hydrologic dissipation.
       * *Nonlinear Geophysical Dynamics*: Stommel (1961) 2-box thermohaline circulation with haline memory and Broecker bipolar seesaw, Howard-Kerby / Whipple-Tucker stream power catchment erosion, and Charney (1975) vegetation-albedo fold bifurcation.
     * **Human Civilizational Embedding**: Human societies and demographics in Ether are strictly embedded within, and constrained by, these universal non-human biophysical laws; they never operate as magic exceptions to thermodynamic conservation.
   * **Tier 2 (Pluggable Cliodynamic Hypotheses)**: Contested sociological and institutional theories (Boserup vs. Malthus, Turchin SDT vs. Pinker, Smil Exergy vs. Nordhaus DICE, Ostrom vs. Hardin, Acemoglu vs. Geography, Scott vs. State Formation, Henrich vs. Static Retention).
3. **Automated Sensitivity & Calibration Suite**:
   * Competing paradigms are formalized mathematically as coupled differential equations and benchmarked within **`ScientificModelFalsificationAndValidationSuite.java`**.
   * Tests evaluate qualitative and quantitative response envelopes, sensitivity parameters, and historical benchmark correlations without claiming infallible sociological proof.
   * All results and mathematical derivations are documented in **`docs/SCIENTIFIC_MODEL_EVALUATION_AND_FALSIFICATION.md`**.

---

## 📏 8. Empirical Anchors & Deep-Time Baseline Transparency
1. **Empirical Historical Anchoring**:
   * Real historical trajectories reflect specific geographic, ecological, and cultural contingencies. Where analytical first-principles alone cannot uniquely determine an initial condition (e.g., early hominin migration corridors, initial Holocene hearth locations, specific imperial cores), Ether uses **explicit empirical anchors** drawn from archaeological and cartographic records.
   * All empirical priors, spatial coordinates, and calibrated thresholds must be documented with literature provenance and justification.
2. **Deep-Time Invariant Boundary Initialization (-3,000,000 BP to -10,000 BP)**:
   * For prehistoric, Pleistocene, and deep Paleolithic epochs (from $-3\,000\,000$ to $-10\,000$ BP) where formal civilizational infrastructure did not exist:
     * **Literacy Rate**: Baseline $\equiv 0.0\%$ (writing emerges only with proto-cuneiform/hieroglyphs $\sim -3400$ BCE).
     * **Urbanization Rate**: Baseline $\equiv 0.0\%$ (permanent agglomerations $> 5\,000$ hab emerge only in Uruk/Eridu/Jericho $\sim -4000$ BCE).
     * **Institutional Administrative Bureaucracy & Sovereign Debt**: Baseline $\equiv 0.0$.
     * **Currency Debasement & Monetary Elite Indices**: Baseline $\equiv 0.0$.
     * **Anthropogenic Deforestation & Industrial Fossil Energy**: Baseline $\equiv 0.0\%$.
     * **Extractive Metal Smelting**: Baseline $\equiv 0.0\text{ kg/capita}$ (emerges in Chalcolithic $\sim -5000$ BCE).
   * All benchmark catalogs, kernels, and simulation initializers enforce these deep-time baselines across the full $-3\,000\,000$ to $-10\,000$ BP horizon.

---

## ⚖️ 9. Scientific Parametric Deviation Registry & Epistemic Calibration Provenance
1. **Universal Validity & Invariance Across Epochs**:
   * Any mathematical adjustment, scaling factor, or parameter revision made to a published academic model (e.g., Turchin's SDT, Solow-Swan capital growth, Kooijman DEB, Brown MTE, Sandom/Barnosky megafauna) MUST possess **universal structural validity across all historical epochs and spatial locations**.
   * Epoch-specific ad-hoc hacks, local conditional tweaks, or calendar-dependent magic coefficients are strictly avoided.
2. **Strict Criterion for Deviations vs. Canonical Equations (Exceptions are NOT the Rule)**:
   * **The Norm (`SIMULATION_EQUATIONS_AND_VARIABLES.md`)**: Whenever an equation is well-specified in academic literature and implemented cleanly as expected without ambiguity or departure, it belongs **exclusively in the Canonical Equations Encyclopedia**. Self-evident formulations and direct scientific laws (e.g., Stefan-Boltzmann radiation, hydrostatic pressure, Henry's gas solubility, Stull wet-bulb, standard allometric power laws) MUST NOT be logged as deviations.
   * **The Exception Log (`PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md`)**: This document is strictly reserved for things parameterized or formulated **not as originally expected by the literature**, specifically:
     1. Cases where the published paper left the equation **under-specified** for macroscopic spatial or temporal execution, requiring an unavoidable non-standard parameterization.
     2. Cases where we had **no real choice but to deviate** from the canonical author formulation (e.g., replacing an intractable 3D continuum fluid solver with an $O(1)$ analytical closure, or replacing a published discrete step cliff with a continuous $C^1$ sigmoid to prevent numerical divergence).
   * **Parsimony of Exceptions**: If code is implemented properly from first principles, entries in the deviation registry should be rare exceptions relative to the total equation catalog. A low entry count is proof of fidelity, not a lack of transparency.
3. **Tier 1 Models in the Deviation Registry**:
   * Tier 1 models are **neither automatically included nor unconditionally exempt**.
   * A Tier 1 engine enters the registry **if and only if** an authentic, unexpected departure or macroscopic closure was required (e.g., Hack's law closure replacing recursive DEM flow routing in stream power erosion, DEB microscopic individual ODE aggregated into a cellular cohort tensor, continuous Liebig softmin replacing the non-differentiable minimum).
   * Direct, clean implementations of invariant physics operating as published (e.g., thermodynamic phase boundaries, hydrostatic heads, standard allometric exponents) remain solely in `docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`.
4. **Parametric Deviation Registry Structure**:
   * Every entry in `docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md` details:
     * **Model & Author**: Complete literature citation (authors, year, title, journal/publisher, DOI).
     * **Canonical Published Formulation**: The theoretical equation and published author parameters directly from the paper.
     * **Calibrated Simulation Formulation & Value**: The exact mathematical formula and parameters implemented in Ether.
     * **Parameters, Material Properties & Physical Units**: SI units, material coefficients, and cross-taxa biological constants.
     * **Academic Justification / Root Cause of Divergence**: Why the canonical form required spatial/temporal adaptation (e.g., transition from microscopic individual ODE to cellular cohort tensor, continuous $C^1$ smoothing, $O(1)$ analytical closure).
     * **Empirical Validation Grounding**: Empirical historical/geophysical datasets (Seshat, Maddison, HYDE 3.4, EPICA ice cores, ERA5, USGS) confirming the predictive accuracy of the formulation.

---

## ⚛️ 10. The Physical Observables Invariant, $C^1$ Smoothness & Ban on State-Level Technology
1. **Primary Biophysical Observables as the Sole Dynamical State**:
   * The fundamental state of any simulation cell (`H3Cell`, `WorldBuffer`) consists **exclusively of conserved physical, chemical, thermodynamic, and biological observables**:
     * Biological masses ($N$ human/agent population, $B_{\text{agr}}$ agricultural biomass, $B_{\text{nat}}$ wild biomass).
     * Material and geological stocks (soil depth, N-P-K mineral reserves, groundwater hydraulic head, coal, iron, hydrocarbons).
     * Physical built capital stock ($K$, measured in physical units of structural materials and machinery).
     * Useful energy and exergy flux ($P_{\text{watts}}$, measured in Joules/second or Watts).
     * Thermodynamic state variables (temperature $T$ in Kelvin, moisture in mm, atmospheric $\text{CO}_2$ in ppm).
   * **Ban on Technology as an Arbitrary State Variable**:
     * Technology is not an isolated physical fluid and is not stored as an independent magical state variable (e.g., `float[] technologyLevel`).
     * Any technological metric is strictly a **derived a posteriori telemetric diagnostic** computed from physical observables:
       $$T_{\text{emergent}} = f\left(\frac{P_{\text{watts}}}{N}, \; \frac{K_{\text{active}}}{N}\right)$$
       and does not feed back as an artificial gating cliff into Tier 1 or Tier 2 equations.
2. **$C^1$ Differentiability & Smooth Transitions**:
   * Transitions between developmental or technological regimes are modeled using mathematically continuous functions to at least the first derivative ($C^1$ smoothness), such as smooth Fermi-Dirac sigmoids $\sigma(x; x_0, s) = \frac{1}{1 + \exp(-(x - x_0)/s)}$ or hyperbolic tangents $\tanh((x - x_0)/s)$, reflecting gradual technological adoption and logistic substitution (Marchetti & Nakicenovic 1979).
   * Discontinuous step cliffs (`if (tech < 1.25)`, `if (year > 1850)`) are avoided; driving variables $x$ are physical observables (power per capita, capital density, temperature, moisture).
3. **First-Order Physical Telemetry Invariant**:
   * Telemetry snapshots (`MetricSnapshot`, `HistorySnapshot`, `SimulationMetricsAggregator`) record first-order observables directly from cellular buffers ($N$, $K$, $P_{\text{watts}}$, $B_{\text{nat}}$, $B_{\text{agr}}$, $B_{\text{livestock}}$, potable water, smelted minerals, occupied surface area, $\text{CO}_2$, Lorenz Gini, institutional stability).
   * Active capital $k_{\text{active}}$ is coupled thermodynamically to active power dissipation $p = P_{\text{watts}}/N$ (Garrett 2011) so post-collapse depopulation ($N \to 0$) does not produce spurious divergence.

---

## 🚫 11. Epistemic Humility, Ban on Empirical Curve-Fitting & Continuous Confidence Envelopes
1. **Strict Ban on Empirical Curve-Fitting & Multi-Stage Sigmoid Tuning**:
   * It is **strictly forbidden** to add ad-hoc non-linear polynomial patches, piecewise sigmoids, or arbitrary exponent tuning to force a mathematical equation through empirical benchmark milestones.
   * Macro-historical data sources (HYDE 3.4, Maddison, EPICA, Seshat) are reconstructions containing inherent uncertainties, not infallible physical constants. Forcing equations to pass exactly through historical points constitutes fatal overfitting.
   * All equations must reflect canonical peer-reviewed first-principles or single parsimonious functional forms. Missing non-linearities must be resolved by implementing the authentic mechanistic causal engines (e.g. prime mover thermodynamics, municipal water grids, printing press economics, sovereign bond markets, naval hydrodynamics, Von Thünen transport rings), never by phenomenological curve-fitting.
2. **Continuous Temporal Error Tolerance Envelope $\tau(t)$**:
   * Test assertions and falsification suites must evaluate trajectories against continuous, differentiable ($C^1$) confidence bands reflecting the true empirical uncertainty of historical data:
     $$\tau(t) = \tau_{\text{mod}} + \frac{\tau_{\text{paleo}} - \tau_{\text{mod}}}{1 + \exp\left(\frac{t - t_{\text{trans}}}{s_{\text{trans}}}\right)}$$
     with $\tau_{\text{paleo}} = 50\%$, $\tau_{\text{mod}} = 5\%$, $t_{\text{trans}} = 1200\text{ CE}$, and $s_{\text{trans}} = 300\text{ years}$.
   * **Deep Time ($-3\,000\,000$ to $-10\,000$ BP)**: $\sim 40\text{--}50\%$ relative uncertainty (archaeological orders of magnitude).
   * **Agrarian Antiquity & Middle Ages ($-10\,000$ BP to $1500$ CE)**: $\sim 25\text{--}35\%$ uncertainty.
   * **Early Modern (1500 to 1900 CE)**: $\sim 15\text{--}20\%$ in 1800 CE, narrowing to $\sim 10\%$ in 1900 CE.
   * **Contemporary (1950 to 2025 CE)**: Asymptotic stabilization at $\sim 5\%$ (empirical margin of global census and satellite observations).
   * **Prospective ($> 2025$ CE)**: Fixed at the 2025 physical baseline with scenario branching (SSP1-SSP5).
3. **Context-Dependent Activation of Tier 2 Type B Scenario Engines (No Hidden Internal Gates)**:
   * Unlike Tier 1 biophysical conservation engines (which apply universally at all times and locations), Tier 2 Type B engines model specific historical, institutional, technological, capital, or geopolitical regimes (e.g., *World3* industrial growth-to-limits, *Columbian Exchange* transoceanic contact, *Tokugawa Sakoku* isolation, *Pleistocene Mammoth-Bone Habitation*).
   * It is **strictly forbidden** to give a domain-specific Type B engine artificial universal validity by injecting hidden conditional sigmoids (`sigmoid(kpc, 1200, 300)`) inside its internal differential equations.
   * Instead, every Type B engine has explicit **Domain Validity Conditions** (temporal window $[t_{\min}, t_{\max}]$, spatial/geopolitical scope, and physical capital/technological preconditions). If a scenario's epoch ($t_0$), planet, or spatial/geopolitical context does not satisfy a Type B engine's validity conditions, that engine **MUST be unchecked (`false`) by default** in `AutoEpochScenarioHelper.calibrateTypeBEngines(...)` and scenario presets, leaving the user full freedom to manually toggle it in UI/Scenario setups.
4. **Spatial Hold-Out & Free-Run Validation**:
   * All models calibrated on Eurasian data must pass out-of-sample hold-out validation on the Pre-Columbian Americas and Sub-Saharan Africa without parametric alteration.
   * The simulation must support unguided multi-century "Free-Run" execution without divergence, extinction, or unphysical negative states.

---

## ⚖️ 12. Tri-Partite Model Conflict Taxonomy & Epistemic Overlap Protocol
Prior to designing or implementing any proposed Type B simulation engine, an exhaustive conflict analysis must be performed against the existing engine registry (`EngineConflictAnalyzer`):

1. **Category 1: Zero Conflict (Structural Gap Fillers - APPROVED)**:
   * **Definition**: The candidate engine introduces a genuine, unmodeled causal mechanism operating on previously unrepresented biophysical flows, energetic transducers, infrastructure networks, or institutional feedbacks.
   * **Validation Requirement**: It must not duplicate, override, or compete with any existing state derivative ($dY/dt$). It must seamlessly integrate with existing conserved state tensors without perturbing core mass-energy conservation invariants.
2. **Category 2: Total Conflict (Orthogonal Cliodynamic Debates - TIER 2 TYPE A ONLY)**:
   * **Definition**: The candidate model directly contradicts an existing model regarding the causal driver of the exact same macro-phenomenon (e.g., Malthus vs. Boserup on carrying capacity, Acemoglu vs. Geographic Determinism on divergence, Turchin SDT cycles vs. Pinker secular pacification).
   * **Strict Directive**: **NEVER simultaneously execute both models in the same simulation run.** Total conflicts MUST be formalized as a pluggable **Canonical Historiographical Debate (Tier 2 - Type A)** with explicit $H_0$ (null hypothesis) and $H_1$ (alternative hypothesis) branches within `ScientificModelFalsificationAndValidationSuite.java`. The user selects which hypothesis to run, or the automated benchmark arbitrates via empirical out-of-sample likelihood.
3. **Category 3: Partial Conflict & Epistemic Overlap (BANNED HYBRIDIZATION - RESOLUTION REQUIRED)**:
   * **Definition**: Two engines model partially overlapping aspects of the same target observable (e.g., both modifying real wage $w$, capital growth $dK/dt$, mortality $\mu$, or soil salinization) through disjoint or uncoordinated causal paths.
   * **The Overlap Danger**: Co-execution results in double counting, numerical resonance/oscillations, or chaotic interference where neither theory can be scientifically evaluated or falsified.
   * **Mandatory Resolution Protocol**:
     1. **Refactor & Harmonize**: If the existing engine already captures the primary first-order biophysical mechanic, enrich that existing engine rather than creating a fragmented pseudo-duplicate.
     2. **Orthogonal Variable Partitioning**: Strictly partition the dynamical targets so that no two engines ever apply competing uncoordinated derivatives to the same scalar state variable without an explicit unified closed-form equilibrium operator.
     3. **Rejection of Redundant Ad-Hoc Modules**: If a proposed historical phenomenon (e.g., "The Great Divergence") emerges organically from the coupled interplay of base physical observables (exergy, transport friction, coal accessibility), implementing a dedicated ad-hoc engine is strictly prohibited as phenomenological overfitting.
4. **Canonical Purity Inside Domain Scope & Explicit Domain Selection**:
   * Within its documented domain of validity, every scientific model implemented in Ether must preserve its pure canonical mathematical formulation without hardcoded regional hacks (`if (inBritain)`), calendar-year cliffs (`if (year > 1750)`), or hidden $k_{\text{pc}}$ masking sigmoids.
   * Domain applicability across epochs and geopolitical settings is enforced transparently at the **scenario configuration layer** (`Scenario.typeBEngineStates`, `AutoEpochScenarioHelper`) by unchecking (`false`) Type B engines whose validity conditions are not met by the scenario.

---

## 🧭 13. Tier 2 Type B Engine Domain Validity & Default Scenario Activation Protocol
Because **Tier 2 Type B** engines model specific historical, institutional, technological, capital-dependent, or geopolitical mechanisms rather than universal thermodynamic conservation laws (Tier 1), every Tier 2 Type B engine MUST follow this four-part specification:

1. **Mandatory Documentation of Validity Conditions (`docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md` & `docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`)**:
   Every Tier 2 Type B engine must explicitly document:
   * **Temporal Validity Window ($[t_{\min}, t_{\max}]$)**: The historical epoch range during which the underlying socio-technical or ecological regime operates (distinguishing the **historical emergence date** of the phenomenon from the modern publication year of its academic author).
   * **Spatial & Geopolitical Context**: The geographic, ecological, or political configuration required (e.g., Pleistocene periglacial steppe, Afro-Eurasian nomadic-sedentary frontier, irrigated arid river basin, post-1492 transoceanic contact, sovereign parliamentary bond market, global industrial economy, extraterrestrial ISRU colony).
   * **Technological, Physical Capital ($k_{\text{pc}}$), & Exergy ($P_{\text{watts}}$) Preconditions**: The material and energetic stocks presupposed by the model's equations.
   * **Default Activation State (`true` / `false`)**: The deterministic rule governing whether the engine is checked by default for a given scenario `(planetKey, startYear, spatialBounds)`.
2. **Default Unchecked (`false`) Rule When Conditions Are Not Met**:
   * If a scenario's epoch $t_0$, planetary body, or geopolitical/spatial context lies outside a Type B engine's validity conditions, the engine **must not be executed by default** (`false` in `AutoEpochScenarioHelper.calibrateTypeBEngines`).
   * Specifically:
     * **Plio-Pleistocene & Paleolithic Biocultural/Mobility Engines** ($t \ge -3\,000\,000\text{ BP}$, e.g., `SelfDomesticationEngine`, `SexualSelectionMatingEngine`, `KinSelectionHamiltonEngine`, `PriceMultilevelSelectionEngine`, and from $-300\,000\text{ BP}$ / $-60\,000\text{ BP}$, `LanguageLinguisticEngine`, `CulturalSociologyEngine`, `DynamicMaritimeRoutingGraph`) are checked (`true`) across their valid Earth evolutionary horizons.
     * **Agrarian, State & Imperial Engines** ($t \ge -8000\text{ BCE}$ or $t \ge -3000\text{ BCE}$, e.g., `BoserupAgriculturalIntensificationEngine`, `DraftAnimalFodderAllocationEngine`, `SoilSalinizationHydrologyEngine`, `TurchinGoldstoneSDTEngine`, `TainterComplexityCollapseEngine`, `BarfieldNomadicSteppeEmpireEngine`) are unchecked (`false`) in Plio-Pleistocene and Paleolithic scenarios ($t < -8000\text{ BCE}$) and checked (`true`) only once sedentary agriculture or state polities exist.
     * **Early Modern & Mercantile Engines** ($t \ge 1450\text{ CE}$, e.g., `PhysicalMediaPrintingEngine`, `AsymmetricColonialTradeEngine`, `CrosbyColumbianExchangeEngine`, `SovereignTreasuryBondEngine`, `NavalHydrodynamicsFreightEngine`) are unchecked (`false`) in pre-1450 CE scenarios.
     * **Industrial, Contemporary & Prospective Engines** ($t \ge 1750 / 1800 / 1950\text{ CE}$, e.g., `PomeranzGreatDivergenceEngine`, `RicardoDifferentialLandRentEngine`, `KondratievSchumpeterLongWaveEngine`, `JevonsParadoxEngine`, `UrbanThermodynamicsEngine`, `PublicSanitationWaterGridEngine`, `BagehotFractionalReserveEngine`, `PhysicalLeontiefInputOutputEngine`, `World3CouplingEngine`, `NuclearSafetyRadiotoxicityEngine`, `IsruAutarkyAndSpaceColonizationEngine`, `TerraformingEngine`) are unchecked (`false`) in all pre-industrial scenarios.
     * **Localized / Regime-Specific & Pure-Theory Sandbox Engines** (e.g., `AmerindianEcosystemEngine`, `RomanImperialCliodynamicEngine`, `EdoJapanIsolationEngine`, `MonasticDemographicBufferEngine`, `TasmanianCulturalRegressionEngine`, `NuclearWarfareClimateEngine`, `GeoengineeringAlbedoFeedbackEngine`, `World3PureEngine`, `HandyNasaPureEngine`) are unchecked (`false`) by default on general global scenarios unless the scenario specifically targets their historical period/region or theoretical benchmark.
3. **Strict 1-to-1 Code Bijection**:
   * Every single entry in `ScenarioEngineCatalog.getOptionalEngines()`, `AutoEpochScenarioHelper.calibrateTypeBEngines(...)`, and `docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md` (86 verified Type B engines) MUST correspond 1-to-1 to a real, executable Java class in `src/main/java/org/ether/society/engines/`. Unimplemented placeholder names or deleted engine references are strictly forbidden.


---

## 🗑️ 14. Code Cleanliness Directive: Ban on `@Deprecated` Annotations & Immediate Code Deletion
1. **Strict Ban on `@Deprecated` Annotations & Zombie Stubs**:
   * It is **strictly forbidden** to retain obsolete, superseded, duplicated, or conflicting code by annotating classes or methods with `@Deprecated` or by creating forwarding shims/wrappers.
   * Do not keep deprecated legacy stubs under the pretext of backwards compatibility when cleaning up or refactoring simulation engines and models.
2. **Immediate and Complete Deletion**:
   * When an engine, kernel, helper, or mathematical formulation is superseded, merged, or identified as a redundant duplicate, **the obsolete code must be immediately and permanently deleted** from the repository.
3. **Exhaustive Upstream & Downstream Refactoring**:
   * Whenever obsolete code is deleted:
     1. All callers, catalogs (`ScenarioHistoricalEnginesCatalog`, `ScenarioOptionalEnginesCatalog`), factories, runners (`TypeBEnginePassRunner`), scenario initializers (`AutoEpochScenarioHelper`), and SPI descriptors (`META-INF/services/org.ether.society.engines.ProceduralEnginePlugin`) MUST be updated to point directly to the canonical replacement.
     2. All corresponding unit tests and benchmark test suites must be updated to test the active canonical engine directly without leaving references to deleted classes.


---

## 🎯 15. The Tri-Pronged Calibration & Bug Fixing Directive (Zero Overfitting Mandate)
During all calibration, benchmark falsification, and testing phases, any code modification across engines, kernels, and metrics MUST strictly adhere to the following three mutually exclusive rules:

1. **Category A: Genuine Algorithmic & Dimensional Bug Fixes (Permitted & Mandatory)**:
   * Applies strictly to authentic code or mathematical implementation bugs:
     * Physical unit mismatches and dimensional scaling errors (e.g., Watts vs. Joules/sec, years vs. days vs. monthly steps $\Delta t$, kg vs. metric tons, per capita vs. aggregate).
     * Formula transcription errors deviating from cited academic literature.
     * Temporal integration step scaling bugs ($\Delta t$ compounding errors) or numerical integration order errors.
     * Off-by-one errors, inverted signs, array index/coordinate misalignments, or null pointer dereferences.
   * These must be cleanly fixed directly at their algorithmic root cause.

2. **Category B: Universally Invariant Empirical Calibration Constants (Permitted with Strict Registration)**:
   * When an empirical scaling constant, coefficient, or threshold in a canonical equation requires calibration because literature values were under-specified or empirically ill-tuned:
     * **Universal Invariance Mandate**: The calibrated constant MUST hold universal structural validity across **ALL scenarios, epochs (from $-100\,000\text{ BP}$ to prospective horizons), and spatial locations**. It is **STRICTLY PROHIBITED** to attach, gate, or conditionalize a constant to a specific epoch, scenario, or historical contingency.
     * **Mandatory Dual-Registration**: Every calibrated deviation or non-standard constant MUST be recorded in `docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md` and updated in `docs/SIMULATION_EQUATIONS_AND_VARIABLES.md` with academic justification and empirical dataset provenance.

3. **Category C: Absolute Ban on Ad-Hoc Adjustments & Overfitting (Strictly Enforced)**:
   * It is **STRICTLY FORBIDDEN** to modify, patch, fudge, or tweak any other aspect of the code, differential equations, or simulation states to force simulation trajectories through empirical data points.
   * If an observed discrepancy between simulation telemetry and empirical history cannot be resolved as an authentic bug (Category A) or as a universally invariant, documented calibration constant (Category B), **NO CODE CHANGE IS PERMITTED**.
   * In such cases, the residual discrepancy must be reported transparently as an epistemic limit or emergent divergence; the problem originates elsewhere in the coupled complex system, and introducing ad-hoc patches constitutes fatal overfitting.

4. **Mandatory Epistemic Response Protocol for Category B & C Diagnostic Analyses**:
   * Whenever a Category B (Macro Dynamics Feedback Loops & Empirical Divergences) or Category C (Macro-Structural Boundaries & Discretization Limits) anomaly is reported or analyzed, the analysis MUST systematically address the following mandatory question:
     * **Mandatory Diagnostic Question**: *« Est-ce qu'on a de quoi modéliser fidèlement par un modèle de meilleure qualité ou un modèle supplémentaire autonome basé sur des équations reconnues de la littérature ? »*
   * **Required 4-Fold Diagnostic Structure for Every Anomaly**:
     1. **Empirical Constat**: Exact quantitative divergence (MAPE %, peak drift onset, spatial RMSE).
     2. **Physical / Dynamical Root Cause**: Exact mechanistic reason for the divergence in the coupled system.
     3. **Authoritative Literature Model & Differential Equations**: Specific published academic equations (authors, year, exact mathematical ODE/PDE formulation, state variables, SI units) capable of modeling the phenomenon authentically without ad-hoc curve fitting.
     4. **Computational ROI & Implementation Viability**: Evaluation of $O(1)$ complexity, $C^1$ continuity, and suitability for the continuous H3 tensor mesh.
