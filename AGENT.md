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

