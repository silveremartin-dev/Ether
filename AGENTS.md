# AGENT.MD: Project Philosophy & Development Directives for Ether

## 🎯 1. Core Project Identity & Scope
**Ether is a Physical-based Cliodynamic & Historical Planetary Simulation Engine.**

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

## 🔒 3. Determinism & Performance Options
* All performance shortcuts (sparse cell skips, multi-frequency climate ticks, spatial range truncations) MUST be toggleable via `SimulationPerformanceConfig`.
* When `strictDeterminism = true`, simulation trajectories MUST be 100% bit-identical for identical initial conditions.

---

## 📄 4. Technical Documentation Standard
* **Strict English Language Requirement**: ALL `README.md` files, technical documentation, architectural specifications, mathematical justifications, epoch provenance files, and source code comments MUST be written strictly in English.
* All equations, physical constants, historical eras, and heuristic catalog items MUST be documented in **`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`** in English.

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

## 🗺️ 6. Cartographic Tensor Provenance & Historical Epoch Standards
For every historical epoch (from -100,000 BP to present-day) supported in `data/maps/ether/<planet>/<year>/`:
1. **Exhaustive Layer Documentation**:
   * Every directory MUST contain `provenance_and_sources.json` and `README.md` detailing the exact geophysical, ecological, demographic, and sociological sources for all 25 map layers.
   * Empirical raster layers (Elevation, Bathymetry, Water tables, Minerals) MUST cite exact global geological datasets (e.g. NOAA ETOPO 2022, GEBCO 2023, UNESCO WHYMAP, USGS MRDS, IAEA NFCIS).
2. **Reconstitution Rationale & Physical Decoupling**:
   * Reconstructed cliodynamic layers (Cultural tensors, Kinship, Isogloss, Carrying capacity, Demographic density) MUST document all underlying physiological, metabolic, and archaeological formulas.
   * Documentation MUST explicitly decouple the static initial conditions ($t = t_0$) from the dynamical simulation engines running per tick ($t > t_0$), enabling transparent diagnostic, calibration, and scientific falsification.

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



