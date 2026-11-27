# AGENTS.md: Project Philosophy, Scientific Constitution & Architectural Contracts for Ether

## 🎯 1. Core Project Identity, Scope & Computational ROI
**Ether is an Integrative Computational Simulation Platform for Cliodynamics, Historical Macroeconomics, and Planetary Ecology.**

Ether provides a **descriptive, exploratory, and heuristic computational sandbox** to study coupled interactions between **geography, physics, thermodynamics, resource ecology, and human civilizational history** over multi-millennial timescales.

### 1.1 Epistemic Humility & Delineated Domains
* **Descriptive & Exploratory Purpose**: Ether is not an oracle of historical inevitability. Discretization over Uber H3 hexagonal meshes, temporal integration steps, and collective behavioral equations are explicit computational hypotheses bounded by physical conservation laws.
* **Two Strictly Delineated Domains**:
  1. **Core Historical Simulation (Canonical Baseline)**: Earth history from $-100\,000\,\text{BP}$ to $2025\,\text{CE}$, anchored to authoritative empirical datasets (NOAA ETOPO, WorldClim, HYDE 3.4, Maddison Project 2020, ECMWF ERA5, EPICA Dome C ice cores, Seshat Databank).
  2. **Exploratory & Speculative Sandbox (Heuristic Extension)**: Post-2025 prospective trajectories (`SSP1`–`SSP5`, `ETHER_CALIBRATED`, resource shocks) and off-Earth scenarios (lunar/Martian/Titan ISRU autarky), explicitly categorized in code and UI as *heuristic exploratory counterfactuals* and never conflated with historical ground truth.

### 1.2 The Computational ROI Filter
$$\text{Computational ROI} = \frac{\text{Emergent Impact on History, Demographics, \& Society}}{\text{CPU Complexity per Tick}}$$
* **Approve ($O(1)$ per cell)**: Direct analytical formulations driving mortality, food yield, exergy conversion, or institutional stability with zero per-tick heap allocations.
* **Reject ($O(N^2)$ / 3D continuum solvers)**: Expensive fluid/matrix loops yielding unobservable aesthetic outputs or violating real-time interactive throughput.

---

## 🔬 2. Two-Tier Ontological Architecture
1. **Tier 1 — Core Biophysical & Thermodynamic Invariants (Universal Cross-Taxa Foundations)**:
   * Applies universally across all epochs and locations. Formulating Tier 1 physics as ad-hoc human-only regressions or discrete `switch(biome)` lookup tables is **strictly forbidden**.
   * Grounded in laws validated across non-human taxa and Earth system physics:
     * **Standard Dynamic Energy Budget (DEB, Kooijman 2010)**: Assimilation, reserve storage $E$, somatic structure $V$, $\kappa$-rule partitioning, and $C^1$ starvation dynamics (`Add-my-Pet`), replacing arbitrary Verhulst $K$ ceilings.
     * **Metabolic Theory of Ecology (MTE, Brown et al. 2004)**: Quarter-power allometry ($B \propto M^{3/4}$) and Arrhenius kinetics ($\exp(-E_a / k_B T)$).
     * **Ecological Stoichiometry (Sterner & Elser 2002)**: Elemental C:N:P mass balance across soils, crops, and food webs.
     * **Constructal Law (Bejan 1996, 2016)**, **Unified Neutral Theory of Biodiversity (Hubbell 2001)**, **Maximum Entropy Production (Kleidon 2010)**, and **Nonlinear Geophysical Dynamics** (Stommel 1961 2-box AMOC, Whipple-Tucker stream power erosion, Charney 1975 vegetation-albedo bifurcation, Berger/Laskar Milankovitch orbital insolation).
   * Human societies are strictly embedded within, and constrained by, these universal non-human biophysical laws.
2. **Tier 2 — Pluggable Cliodynamic & Institutional Hypotheses**:
   * **Type A (Orthogonal Historiographical Debates)**: Competing causal paradigms ($H_0$ vs. $H_1$: Malthus vs. Boserup, Turchin SDT vs. Pinker, Smil Exergy vs. Nordhaus DICE, Ostrom vs. Hardin, Acemoglu vs. Geography, Scott vs. State Formation, Henrich vs. Static Retention) evaluated in `ScientificModelFalsificationAndValidationSuite.java` and documented in `docs/SCIENTIFIC_MODEL_EVALUATION_AND_FALSIFICATION.md`.
   * **Type B (Domain-Bounded Historical & Socio-Technical Engines)**: Specific institutional, infrastructural, or geopolitical regimes active within explicit domain validity conditions (§7).

---

## 🏛️ 3. The Four Canonical Architectural Invariant Contracts ("Zero Interpretation in Engines")
Simulation engines MUST contain **zero ad-hoc geometrical, calendar, or dimensional constants**: only the intrinsic published parameters of the scientific equation may appear inside an engine. Every engine operates within four centralized contracts:

1. **Contract 1 — Single Astronomical Clock (`SimulationTimeStep` & `SimulationTimeCoordinator`)**:
   * Calendar progression is governed exclusively by `SimulationTimeCoordinator.advanceTick(SimulationTimeStep)`, accumulating exact Julian days against `PhysicalUnitsHelper.DAYS_PER_JULIAN_YEAR` ($365.25\,\text{d/yr}$) and `DAYS_PER_SYNODIC_MONTH` ($30.4375\,\text{d/mo}$), with $\Delta t_{\min} = 10^{-4}\,\text{days}$.
   * Approximate `360`-day years, `dtMonthly = dtYears`, or inline `/ 31_557_600.0` divisions inside engines are **strictly forbidden**.
   * **Numerical Integration**: Production ticks use $O(1)$ explicit Euler ($y_{k+1} = y_k + \Delta t \cdot f(y_k)$) for smooth transport/growth, stabilized by exact analytical $L$-stable exponential integrators ($y_{k+1} = y^* + (y_k - y^*) e^{-\lambda \Delta t}$) for stiff decay/relaxation ($\lambda \Delta t \gtrsim 1$). Higher-order RK4 or implicit Newton solvers are reserved for offline verification suites.

2. **Contract 2 — Single International System of Units (`PhysicalUnitsHelper`)**:
   * Every `WorldBuffer` Structure-of-Arrays (SoA) field has one invariant SI storage unit:
     * `humanPopulation`: living individuals [$\text{persons}$].
     * `biomassAgriculture`, `resourceCapital`: kilograms [$\text{kg}$].
     * `biomassNatural`, `biomassLivestock`, `biomassFish`, `woodResource`, `metalResource`, `clayResource`, `resourceCoal`, `resourceOil`, `resourceGas`, `resourceUranium`: metric tonnes [$\text{t} = 1\,000\,\text{kg}$].
     * `foodResource`, `energySolar`, `energyWind`, `energyFire`, `energyFoodConsumed`: Gigajoules [$\text{GJ} = 10^9\,\text{J}$].
   * ALL dimensional conversions MUST pass exclusively through `org.ether.society.model.PhysicalUnitsHelper` (e.g., `metricTonnesToKg`, `kgToMetricTonnes`, `megajoulesToGigajoules`, `wattsToGjPerYear`, `gjPerYearToWatts`, `celsiusToKelvin`, `GJ_PER_MW_YEAR = 31_557.6`). Inline conversion literals (`* 31.688`, `* 1e-3`, `+ 273.15`, `/ 365.25`) are **strictly forbidden**.

3. **Contract 3 — Canonical Geodesic Surface System (`SpatialSurfaceHelper` & `WorldBuffer.cellAreaKm2`)**:
   * Every cell slot $i$ in `WorldBuffer` stores its exact spherical H3 hexagon surface area `cellAreaKm2[i]` scaled by planetary radius $(R_{\text{planet}} / R_{\oplus})^2$, resolved via `SpatialSurfaceHelper`.
   * Hardcoding resolution-dependent cell areas (`/ 36.13`, `/ 100.0`, `/ 1770.49`, `/ 4500.0`, `/ 12000.0`) inside engines or telemetry evaluators is **strictly forbidden**; engines MUST query `buffer.getCellAreaKm2(i)`, `buffer.getPopulationDensityPerKm2(i)`, or `SpatialSurfaceHelper.resolveAreaScaleVsRes4(buffer, i)`.

4. **Contract 4 — Pure `WorldBuffer` I/O, Single-Responsibility Base Laws, Java-Rust Parity & `AgentBuffer` Closure**:
   * **Single Responsibility for Base Physical Capital (`UrbanKernel.java` & `urban.rs`)**: `UrbanKernel` (mathematically synchronized with `native/ether-core-native/src/urban.rs`) is the **sole owner** of baseline Solow-Swan physical capital equilibrium and entropic Tainter turnover ($\dot{k}_{\text{pc}} = \delta_0 k^*(p, B_{\text{agr}}, N) - \delta_{\text{eff}}(C) k_{\text{pc}}$) with Lavoisier raw-material debit. Tier 1 engines (`InfrastructureInertiaEngine`, `ResourceRecyclingEngine`) model only unmet maintenance deficits and bounded scrap recovery; no other kernel or Tier 1 engine may overwrite or duplicate Solow-Swan capital accumulation.
   * **Single Responsibility for Primary Biomass vs. Agrarian Harvest (`EnvironmentalKernel` vs. `DemographicKernel`)**: `EnvironmentalKernel` (`environmental.rs`) is the **sole owner** of primary NPP and standing field biomass (`biomassNatural`, `biomassAgriculture`) and never harvests cultivated crops into `foodResource`. `DemographicKernel` (`demographic.rs`) is the **sole owner** of harvesting cultivated crops, livestock, and fisheries to satisfy cohort metabolic demand.
   * **Finite-Volume Edge Conservation (`FluxKernel` & `flux.rs`)**: Spatial resource diffusion across the 6 H3 neighbors MUST enforce $\Phi_{ij} = -\Phi_{ji}$ with per-edge outflow bounded to $\le \frac{1}{6}$ of source stock so total mass is conserved to exact floating-point precision without negative clipping.
   * **Exact Demographic Closure & Zero `H3Cell` Split-Brain**: `SimulationPipeline` snapshots `WorldBuffer.humanPopulation` before Tier 1b/Tier 2 passes and reconciles survival/growth ratios onto `AgentBuffer.mass` (`H3BufferSyncHelper.reconcileEngineDemographicDeltas`), followed by `InvariantConservationGuard.enforcePositivityAndBounds`. `H3Cell` is strictly a downstream read-only projection updated via `syncBufferToCells()`.

---

## ⚖️ 4. Conservation of Matter & Energy vs. Non-Conservation of Information
1. **Ontological Boundary**:
   * **Conserved Matter & Energy (Lavoisier & 1st/2nd Laws)**: Physical mass stocks (`resourceCapital`, `metalResource`, `woodResource`, `clayResource`, `foodResource`, `biomass*`, `resourceCoal/Oil/Gas/Uranium`, `waterResource`) and thermal/mechanical fluxes (`energyFire`) MUST never be created *ex nihilo*; every positive increment requires solar photosynthesis, geological reserve extraction, or an explicit upstream stock debit.
   * **Non-Rival Information (Romer 1990; Arthur 2009; Henrich 2004; Shannon 1948)**: Ideas, literacy, cultural traits, and institutions are non-conserved informational states that can replicate and recombine without depleting the sender, while their **physical materialization** into machines or infrastructure remains strictly bounded by Carnot exergy and Lavoisier mass.
2. **Multi-Theory Consensus & Lavoisier-Carnot Capital Closure (`TypeBEnginePassRunner`)**:
   * All Tier 2 engines modifying `resourceCapital` MUST implement `supportsCellularDerivatives() = true` (`accumulateCellularDerivatives`) so `TypeBEnginePassRunner` enforces:
     1. **Non-Diluting Envelope (Zero $M$-Fold Double-Counting)**: Overlapping positive investment theories combine via upper envelope ($\Delta K_{\text{env}}^+ = \max_e \Delta K_e^+$) and Tier 2 drag theories via lower envelope ($\Delta K_{\text{env}}^- = \min_e \Delta K_e^-$), while discrete shocks (warfare, disasters) apply directly in `EventPhysicalImpactExecutor`.
     2. **Marginal Carnot Exergy & Krausmann Stoichiometry**: Above Phase 1's Solow-Swan replacement ($\delta_0 K$), Tier 2 net positive capital deepening $\Delta K^+_{\text{phys}}$ [$\text{kg}$] is bounded by remaining net exergy surplus ($\Delta K_{\text{Carnot}} = s_{\text{marg}} \cdot \max(0, E_{\text{cell}} - E_{\text{basal}} - E_{\text{maint}} - E_{\text{repl,P1}}) \Delta t / e_{\text{embodied}}$, $e_{\text{embodied}} = 15\,\text{MJ/kg}$, $s_{\text{marg}} = 0.12$, Phase 1 + Phase 2 $\le 0.25$, Smil 2017) and debits ~5% `metalResource`, ~7% `woodResource`/`biomassNatural`, and ~10% `clayResource` in metric tonnes (Krausmann et al. 2017; remaining ~78% continental crustal stone/sand aggregates on land, strictly stock-bounded at sea).
3. **Mandatory Upstream Debits**:
   * Thermal prime movers and rebound (`ThermodynamicPrimeMoversEngine`, `JevonsParadoxEngine`, `PhysicalEnergyGridEngine`) MUST debit `resourceCoal` ($24\,\text{GJ/t}$), `resourceOil` ($42\,\text{GJ/t}$), `resourceGas` ($50\,\text{GJ/t}$), or `woodResource` ($15\,\text{GJ/t}$) and produce zero thermal power when fuel stocks are exhausted.
   * Food/land conversions (`EcologicalStoichiometryEngine`, `WienerCyberneticHomeostasisEngine`, `PolanyiFictitiousCommoditiesEngine`, `DraftAnimalFodderAllocationEngine`) MUST debit upstream `biomassAgriculture`, `biomassNatural`, or `biomassLivestock`.
   * Crustal metallogenic uplift (`AiryIsostasyCrustalRootEngine`) MUST operate at true geological rates ($\sim 10^{-6}\,\text{yr}^{-1}$).

---

## ⚛️ 5. Physical Observables Invariant, $C^1$ Smoothness & Deep-Time Baselines
1. **Ban on Technology as a Dynamical State Variable**:
   * `WorldBuffer` stores **only conserved physical, chemical, thermodynamic, and biological observables**. Storing an arbitrary `technologyLevel` array is **strictly forbidden**.
   * Any technological index (such as the Garrett-Lotka index $\Phi_{\text{exergy}}(p, k_{\text{active}})$, where $k_{\text{active}}$ is harmonically coupled to active power dissipation $p = P_{\text{watts}}/N$ per Garrett 2011) is strictly an **a posteriori telemetric diagnostic** and never acts as a discontinuous gating cliff inside physics equations.
2. **$C^1$ Smooth Regime Transitions**:
   * Regime transitions MUST use at least $C^1$-differentiable functions ($\sigma(x; x_0, s)$, $\tanh$, `softplus`, `softmin`) driven by physical observables ($p$, $k_{\text{pc}}$, $T$, moisture), never discontinuous calendar or tech step cliffs (`if (year > 1850)`, `if (tech < 1.25)`).
3. **Deep-Time Invariant Boundary Initialization ($-3\,000\,000\,\text{BP}$ to $-10\,000\,\text{BP}$)**:
   * Across all Plio-Pleistocene and Paleolithic epochs prior to sedentary civilization, initializers and telemetry enforce exact zero baselines (`0.0`) for: **Literacy Rate** (emerges $\sim -3400\,\text{BCE}$), **Urbanization Rate** ($> 5\,000\,\text{hab}$, emerges $\sim -4000\,\text{BCE}$), **Sovereign States / Bureaucracy / Debt**, **Currency Debasement & Monetary Elites**, **Anthropogenic Deforestation & Fossil Energy**, and **Extractive Metal Smelting** (emerges $\sim -5000\,\text{BCE}$).

---

## 🔒 6. Strict Bitwise Determinism & Seeded PRNG Directives
* **Bitwise Reproducibility**: When `strictDeterminism = true`, identical initial conditions and seeds MUST produce 100% bit-identical trajectories across single-core, multi-core SIMD, and native Rust execution.
* **Prohibition of Unseeded Randomness**:
  * Unseeded `new Random()`, `Math.random()`, and non-deterministic `UUID.randomUUID()` are **strictly prohibited** across all simulation engines, kernels, domain entities (`Polity`), and data/procedural generators (permitted in UI panels *only* on explicit user "Roll Seed" button clicks).
  * Injecting stochastic jitter into empirical rasters or historical boundary conditions at $t = t_0$ is **strictly forbidden**.
* **Stateless Order-Independent `DeterministicPRNG`**: All stochastic evaluations across cells/cohorts MUST use SplitMix64 `DeterministicPRNG` keyed to `(seed, tick, cellIndex, streamId)` anchored to `scenario.getSeed()`.

---

## 🧭 7. Tier 2 Conflict Taxonomy & Type B Domain Validity Protocol
1. **Tri-Partite Conflict Taxonomy (`EngineConflictAnalyzer`)**:
   * **Category 1 (Zero Conflict — Approved)**: Models a genuinely unrepresented causal mechanism without duplicating existing state derivatives.
   * **Category 2 (Total Conflict — Tier 2 Type A Only)**: Directly contradicts an existing theory on the same observable; NEVER co-executed, formalized as $H_0$ vs. $H_1$ in `ScientificModelFalsificationAndValidationSuite.java`.
   * **Category 3 (Partial Overlap — Forbidden Hybridization)**: Overlapping derivatives on the same variable must either be merged into the canonical engine or unified through `TypeBEnginePassRunner`'s closed-form envelope.
2. **Type B Domain Validity & Default Unchecked (`false`) Rule**:
   * It is **strictly forbidden** to inject hidden internal $k_{\text{pc}}$ or calendar masking sigmoids inside a domain-specific Type B engine to fake universal validity.
   * Every Type B engine documents its **Temporal Window $[t_{\min}, t_{\max}]$** (historical emergence of the phenomenon, not author publication year), **Spatial/Geopolitical Scope**, **$k_{\text{pc}}$ / $P_{\text{watts}}$ Preconditions**, and **Default Activation Rule** in `docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md`.
   * In `AutoEpochScenarioHelper.calibrateTypeBEngines(...)`, any Type B engine whose validity conditions are not met by `(planetKey, startYear, spatialBounds)` MUST default to **`false`** (unchecked):
     * Plio-Pleistocene biocultural/mobility engines ($t \ge -3\,000\,000\,\text{BP}$ / $-300\,000\,\text{BP}$): `true` on valid Earth horizons.
     * Agrarian & state engines ($t \ge -8000\,\text{BCE}$ / $-3000\,\text{BCE}$): `false` for $t < -8000\,\text{BCE}$.
     * Early modern & mercantile engines ($t \ge 1450\,\text{CE}$): `false` for $t < 1450\,\text{CE}$.
     * Industrial, contemporary & off-Earth engines ($t \ge 1750 / 1800 / 1950\,\text{CE}$): `false` in all pre-industrial scenarios.
     * Localized regime or pure-theory sandbox engines (`EdoJapanIsolationEngine`, `RomanImperialCliodynamicEngine`, `AmerindianEcosystemEngine`, `World3PureEngine`, `HandyNasaPureEngine`, etc.): `false` except on their target scenario.
   * **1-to-1 Code Bijection**: Every entry across `ScenarioEngineCatalog`, `AutoEpochScenarioHelper`, and `docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md` MUST map 1-to-1 to a real executable class in `src/main/java/org/ether/society/engines/`.

---

## 📄 8. Dual-Document Scientific Documentation & Calibration Discipline
1. **Strict English & Complete Javadoc Mandate**:
   * ALL source code, comments, Javadoc (`@param`, `@return`, `@throws`, SI units, $\LaTeX$ equations), `README.md`, and `docs/*.md` files MUST be written strictly in **English**.
   * Curated specifications live in `docs/`; transient test, benchmark, and audit reports MUST be written to `target/reports/` (never polluting `docs/` or `scratch/`).
2. **Dual-Document Epistemic Architecture**:
   * **`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md` (The Canonical Norm)**: Exhaustive encyclopedia of all simulation equations, conservation laws, SI units, standard constants, and literature citations implemented as published. Clean literature implementations belong **exclusively** here.
   * **`docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md` (The Exception Log)**: Reserved **strictly** for genuine departures where a published equation was under-specified for macroscopic H3/temporal scales or required an unavoidable $O(1)$ / $C^1$ reformulation. Each entry documents: *Model & Author (DOI)*, *Canonical Published Formulation*, *Calibrated Simulation Formulation*, *Physical Units*, *Root Cause of Divergence*, and *Empirical Validation Grounding*.
3. **Tri-Pronged Calibration & Bug-Fixing Protocol (Zero Overfitting)**:
   * **Category A (Algorithmic & Dimensional Bugs — Mandatory Fix)**: Unit mismatches, formula transcription errors, $\Delta t$ scaling bugs, non-conservation, sign/index errors, or Java-Rust divergence. Fix at root cause with unit tests.
   * **Category B (Universally Invariant Calibrations — Registered)**: Hard natural-science constants are **strictly non-tunable**. Macro-sociological or under-specified closure constants may be calibrated **only** if they hold universal structural validity across **all epochs, scenarios, and continents** (including out-of-sample Pre-Columbian Americas & Sub-Saharan Africa hold-outs and multi-century unguided free-runs) and are dual-registered in both documents above.
   * **Category C (Ad-Hoc Curve-Fitting — Strictly Forbidden)**: Polynomial patches, multi-stage sigmoids, or epoch-gated tweaks to force trajectories through noisy historical reconstructions are **strictly prohibited**. Residual discrepancies are evaluated against the continuous $C^1$ uncertainty envelope:
     $$\tau(t) = \tau_{\text{mod}} + \frac{\tau_{\text{paleo}} - \tau_{\text{mod}}}{1 + \exp\!\left(\frac{t - 1200}{300}\right)}, \qquad \tau_{\text{paleo}} = 50\%, \quad \tau_{\text{mod}} = 5\%$$
     Any Category B/C anomaly report must answer whether a peer-reviewed mechanistic differential equation can model the missing causal link via a 4-part analysis (*Empirical Constat*, *Physical Root Cause*, *Literature ODE/PDE & Units*, *Computational ROI*).

---

## 🗺️ 9. Cartographic Tensor Provenance & Planetary Encapsulation
* **Planetary Encapsulation (`data/maps/ether/<planet>/<year>/`)**:
  * Each epoch directory contains `provenance_and_sources.json`, `README.md`, **19 continuous 16-bit grayscale scalar PNGs** (`elevation`, `temperature`, `precipitation`, `seasonality`, `density`, `technology`, `institutional`, `ecological`, `pathogen`, `coal`, `oil`, `gas`, `uranium`, `helium3`, `iron_copper`, `precious_metals`, `rare_earths`, `geothermal`, `aquifers`), and **6 categorical 24-bit RGB PNGs** (`biomes`, `isogloss`, `kinship`, `rituals`, `sovereignty`, `tradenetwork`).
  * **Historical Catalogs (`data/history/ether/<planet>/`)**: `events/historical_events_catalog.json`, `treaties/treaties_and_transitions.json`, `paleo/` (`hominin_hearths.json`, `ice_sheets.json`, `migration_routes.json`), and `leaders/earth_historical_leaders.json`.
* **1D Continuous Baselines (`data/variables/`) & Zero Double Transition**:
  * `historical/paleo_sources_1d.json` (`PALEO_SOURCES`, $t_0 < -10\,000\,\text{BP}$), `historical/hyde_maddison_historical_1d.json` (`HYDE_MADDISON`, $-10\,000\,\text{BP} \le t_0 \le 2025\,\text{CE}$), and `projections/` (`SSP1`–`SSP5`, `ETHER_CALIBRATED`, $t_0 \ge 2025\,\text{CE}$).
  * Observational ERA5/HYDE/Maddison series transition directly to prospective projections in 2025 with PCHIP monotone cubic Hermite interpolation (Fritsch & Carlson 1980) preventing Runge overshoot.

---

## 🎨 10. User Interface, 5-Language Localization & Accessibility
1. **Complete 6-Bundle Localization (`EN`, `FR`, `DE`, `ES`, `ZH` + `messages.properties`)**:
   * **Zero hardcoded user-facing strings** in `src/main/java/org/ether/society/ui` (`new Label`, `new Button`, `new CheckBox`, `.setText`, `.setTitle`, dialog metadata).
   * **Mandatory Technical Tooltips**: Every interactive UI control MUST provide a localized tooltip explaining the underlying physical variable, SI unit, or equation.
2. **High-Contrast Theme Discipline**: Dark theme uses high-luminance text/strokes on dark surfaces; Light theme uses dark text/strokes on light surfaces (`ThemeManager`). Low-contrast combinations are strictly forbidden.

---

## 🛠️ 11. Code Cleanliness, Architecture Audits & Multi-OS Script Parity
1. **Immediate Deletion & Zero `@Deprecated` Stubs**:
   * `@Deprecated` annotations, zombie shims, and duplicate/superseded kernels or engines are **strictly forbidden**; obsolete code must be deleted immediately and all callers, catalogs, SPI files, and tests updated directly.
   * Keep classes cohesive (~500-line soft guideline; never split cohesive scientific logic into artificial fragments solely to meet a line count).
2. **Mandatory Zero-Defect Audit Suite**: Every structural or UI change MUST pass all three automated audits:
   * `scripts/test/audit-architecture-compliance.ps1` / `.sh` (zero empty `catch`, zero duplicate class names, stateless `final` `*Helper` / `*Formatter` contracts, canonical Singletons & `SimulationEngineModule` compliance).
   * `scripts/test/audit-full-quality-and-naming.ps1` / `.sh` (naming taxonomy, 0 `@Deprecated`, 100% class Javadoc, English-only comments, 0 unseeded `Random`, UTF-8 integrity).
   * `scripts/i18n/audit-i18n-and-hardcoded-strings.ps1` / `.sh` (100% key parity across all 6 bundles, 0 hardcoded UI strings).
3. **Cross-Platform Script Parity & Subdirectory Structure (`scripts/`)**:
   * Every operational script MUST provide both Windows (`.ps1` and/or `.bat`) and POSIX (`.sh`) versions. No scripts may sit loose at the root of `scripts/`; all reside in `scripts/{build,run,test,benchmark,i18n,gcp,gis,ingest}`.
