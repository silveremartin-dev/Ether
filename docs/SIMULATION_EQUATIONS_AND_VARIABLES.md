# Ether Simulation  -  Differential Equations & State Variable Specification

> **Master Technical, Physical & Mathematical Specification**
> *Author: Silvere Martin-Michiellot*
> *Standards: AGENTS.md §8, §9, §10 & §11  -  Strict Separation between Core Model Physics (Tier 1) and Optional Cliodynamics (Tier 2)*
> *Verification Standard: Continuous Temporal Error Tolerance Envelope $\tau(t)$, Spatial Hold-Out Cross-Validation & Multi-Century Free-Run Stability*

---

## 1. Architectural Epistemology & Two-Tier Separation

The **Ether Engine** strictly partitions its computational mathematical models into two ontological tiers:

```
+----------------------------------------------------------------------------------------------------------+
|                        TIER 1 : CORE MODEL PHYSICS (Fundamental Invariant Physics)                       |
|   • Universal Conservation Laws (Mass, Energy, Momentum, Charge, Entropy)                                |
|   • Celestial Mechanics & Orbital Forcing (Milankovitch Cycles 100k / 41k / 23k)                         |
|   • Thermodynamics, Radiation & Cryosphere (Stefan-Boltzmann, Clausius-Clapeyron, PDD Glacial Melt)       |
|   • Geophysical Fluid Dynamics (Coriolis, 3-Cell Atmospheric Hadley/Ferrel/Polar Circulation)           |
|   • Porous Hydrogeology (2D Darcy Aquifer Diffusion), Free-Surface Hydraulics (Manning-Strickler)        |
|   • Solid Geophysics & Isostasy (Viscoelastic Glacial Isostatic Adjustment, Airy Crustal Roots)          |
|   • Hydraulic Pedology (van Genuchten AWC) & Alluvial Sedimentation (Stokes Settling Velocity)           |
|   • Radiative Transfer (Beer-Lambert LAI) & Biochemical Photosynthesis (Farquhar FvCB)                   |
|   • Metabolic Biophysics (Kleiber 3/4 Allometry, Gompertz-Makeham Senescence, Stull Wet-Bulb Lethality)  |
|   • Population Genetics & Radiochronology (Kimura Neutral SDE Diffusion, C-14 Decay, d13C Fractionation) |
|   • Prime Movers & Motive Power (Somatic, Draft Animal, Hydro/Aeolian Mill, Steam Rankine, Grid)         |
|   • Mechanical Friction Drag (Coulomb/Navier) & Chemical Smelting Enthalpy (Metallurgy)                  |
+----------------------------------------------------------------------------------------------------------+
                                                    |
                                                    | Biophysical Forcings & Feedback Loops
                                                    v
+----------------------------------------------------------------------------------------------------------+
|                  TIER 2 : OPTIONAL PLUGINS & CLIODYNAMICS (Phenomenological & Debated)                   |
|                                                                                                          |
|   SUBSECTION A  -  TYPE A: PLUGGABLE SCIENTIFIC MACRO-HISTORICAL DEBATES                                   |
|   • Boserup Endogenous Agricultural Intensification vs. Malthusian Carrying Capacity Depletion           |
|   • Peter Turchin Structural-Demographic Theory (SDT) Secular Cycles vs. Steven Pinker Pacification     |
|   • Vaclav Smil / Kümmel-Ayres Physical Exergy Economics vs. William Nordhaus Neoclassical DICE         |
|   • Elinor Ostrom Polycentric CPR Governance vs. Garrett Hardin Leviathan Tragedy of the Commons         |
|   • Acemoglu-Robinson Institutional Primacy vs. Sachs-Diamond Biogeographical Friction                   |
|   • James C. Scott Early Cereal State Parasitism vs. Wetland Forager Resilience                          |
|   • Joseph Henrich Demographic Cultural Transmission Loss (Tasmanian Effect) vs. Static Retention        |
|                                                                                                          |
|   SUBSECTION B  -  TYPE B: SCENARIO HEURISTICS & HISTORICAL PHENOMENOLOGICAL ENGINES                       |
|   • Municipal Public Sanitation & Subterranean Sewerage Grids (Omran Epidemiological Transition)         |
|   • Physical Substrates & Movable Type Printing Revolution (Buringh & van Zanden Knowledge Diffusion)    |
|   • Sovereign Treasury Balance Sheets & Bond Markets (Reinhart & Rogoff Fiscal Cycles)                   |
|   • Naval Hydrodynamics & Cargo Hull Displacement (Harley Maritime Freight Scaling)                      |
|   • Spatial Concentric Agrarian Locational Rent Rings (Von Thünen Land Use Logistics)                     |
|   • Amerindian Controlled Agro-Pyrogenic Fire Ecology, Edo Japan Sakoku Island Isolation, etc.           |
+----------------------------------------------------------------------------------------------------------+
```

---

## 2. Multi-Scale Temporal Decoupling & Numerical Integration

Ether decouples numerical integration across three discrete physical time scales on the Uber H3 hexagonal grid ($175,000+$ cells at Resolution 6-8) via the `MultiScaleSymplecticIntegrator`:

- **Tier 1  -  Fast Daily Scale ($\Delta t_{\text{daily}} = 1 \text{ day} = 86,400 \text{ s}$)**:
  - Resolves symmetric finite-volume mass and logistics fluxes under Courant-Friedrichs-Lewy (CFL) limits (`FluxKernel`).
  - Evaluates instantaneous wet-bulb temperature and hyperthermia thresholds (`WetBulbTemperatureEngine`).
  - Computes zonal wind vectors under Coriolis parameter $f = 2\Omega \sin\phi$ (`AtmosphericCirculationHadleyEngine`).
  - Resolves active localized epidemiological transmissions via sparse active sets (`SpatialMetapopulationSEIREngine`).
  - Updates tactical political borders, legitimacy pressure, and military maneuvers (`WarDiplomacyEngine`).

- **Tier 2  -  Monthly Biophysical Sub-Step Scale ($\Delta t_{\text{monthly}} = 30 \text{ days} \approx 2.592 \times 10^6 \text{ s}$)**:
  - Updates Milankovitch astronomical orbital parameters ($e$, $\varepsilon$, $\varpi$) and TOA solar insolation (`MilankovitchOrbitalEngine`).
  - Solves 2D lateral piezometric aquifer diffusion (Darcy flux in `AquiferDepletionEngine`).
  - Integrates cryospheric thermodynamic melt ($L_f = 333.55\text{ kJ/kg}$) and eustatic sea level adjustments (`GlacialThermodynamicMeltEngine`).
  - Resolves van Genuchten soil water retention, N-P-K soil nutrient depletion, and Stokes siltation kinetics.
  - Integrates Farquhar FvCB photosynthesis, Beer-Lambert light interception, and soil organic carbon decay.
  - Updates cohort demographic aging, Gompertz-Makeham senescence, and Kimura genetic drift.
  - Integrates coupled biophysical, thermodynamic, and socio-institutional differential operators across spatial cells and sovereign polities.

- **Tier 3  -  Annual Macro Physics Scale ($\Delta t_{\text{annual}} = 365 \text{ days} \approx 31.5576 \times 10^6 \text{ s}$)**:
  - Deep mineral ore grade degradation and Hotelling depletion dynamics (`OreGradeThermodynamicsEngine`).
  - Vaclav Smil primary energy transition inertia (35-year turnover time for physical infrastructure).
  - Integrates Viscoelastic Glacial Isostatic Adjustment (GIA, $\tau \approx 4000\text{ yr}$).
  - Radiocarbon $^{14}\text{C}$ decay calibration and geochemical $\delta^{13}\text{C}$ fractionation.
  - Tainter organizational complexity maintenance drag & diminishing marginal returns.
  - Long-term historical telemetry validation (RMSE & $R^2$ against Seshat, Maddison, HYDE 3.4).

### 2.1 Sequential Operator Splitting Architecture (Lie-Trotter Scheme) & Error Bounds

To maintain $O(N)$ computational efficiency per tick across the global mesh while coupling heterogeneous physical and socioeconomic processes, Ether solves the coupled partial differential equations through sequential **Lie-Trotter operator splitting**:

$$\frac{\partial \mathbf{u}}{\partial t} = \mathcal{A}_{\text{env}}(\mathbf{u}) + \mathcal{B}_{\text{demog}}(\mathbf{u}) + \mathcal{C}_{\text{flux}}(\mathbf{u}) + \mathcal{D}_{\text{econ}}(\mathbf{u}) + \mathcal{E}_{\text{inst}}(\mathbf{u})$$

The simulation advances the global state vector $\mathbf{u}(t)$ to $\mathbf{u}(t + \Delta t)$ by composing individual fractional-step propagators:

$$\mathbf{u}(t + \Delta t) \approx \exp\left(\Delta t \mathcal{E}_{\text{inst}}\right) \exp\left(\Delta t \mathcal{D}_{\text{econ}}\right) \exp\left(\Delta t \mathcal{C}_{\text{flux}}\right) \exp\left(\Delta t \mathcal{B}_{\text{demog}}\right) \exp\left(\Delta t \mathcal{A}_{\text{env}}\right) \mathbf{u}(t)$$

#### Splitting Error Bound & Lie Bracket Commutators:
Because the underlying operators do not strictly commute ($[\mathcal{A}_{\text{env}}, \mathcal{B}_{\text{demog}}] \neq 0$, e.g., agricultural yield depends on prior population labor, while demographic survival depends on harvest), sequential splitting introduces a first-order truncation error governed by the Baker-Campbell-Hausdorff (BCH) expansion:

$$\mathcal{E}_{\text{split}}(\Delta t) = \frac{1}{2} (\Delta t)^2 \sum_{i < j} [\mathcal{O}_i, \mathcal{O}_j] + \mathcal{O}\left((\Delta t)^3\right)$$

where $[\mathcal{O}_i, \mathcal{O}_j] = \mathcal{O}_i \mathcal{O}_j - \mathcal{O}_j \mathcal{O}_i$ is the Lie commutator bracket. In Ether:
1. **Biophysical conservation is strictly maintained**: Mass, energy, and atomic constituents are conserved exactly at each sub-step regardless of operator ordering.
2. **Numerical stability over multi-millennial runs**: Sub-stepping fast ecological fluxes ($\Delta t_{\text{daily}}$) inside the annual macro-loop bounds the operator commutator norm $\|[\mathcal{A}, \mathcal{B}]\|$, eliminating spurious numerical drift while avoiding the prohibitive $O(N^3)$ cost of monolithic implicit Jacobian solvers.

### 2.2 Continuous Temporal Error Tolerance Envelope $\tau(t)$ & Epistemic Verification Standard

In accordance with **AGENTS.md §11**, Ether bans empirical curve-fitting and arbitrary parameter tuning to force trajectories through historical benchmark points. Empirical historical datasets (HYDE 3.4, Maddison Project 2020, Seshat, EPICA Dome C) represent historical reconstructions subject to increasing epistemic uncertainties the further one travels into the past.

#### 1. Continuous Temporal Error Tolerance Function:
Verification and falsification suites evaluate model trajectories against a continuously differentiable ($C^1$) confidence envelope:

$$\tau(t) = \tau_{\text{mod}} + \frac{\tau_{\text{paleo}} - \tau_{\text{mod}}}{1 + \exp\left(\frac{t - t_{\text{trans}}}{s_{\text{trans}}}\right)}$$

with canonical parameters:
- $\tau_{\text{paleo}} = 0.50$ ($50\%$ relative uncertainty in the deep Pleistocene and Paleolithic)
- $\tau_{\text{mod}} = 0.05$ ($5\%$ asymptotic relative uncertainty in the contemporary satellite and census era)
- $t_{\text{trans}} = 1200\text{ CE}$ (inflection point of documentary and fiscal records)
- $s_{\text{trans}} = 300\text{ years}$ (characteristic transition scale)

#### 2. Historical Epoch Uncertainty Envelopes:
- **Deep Time ($-3\,000\,000\text{ BP}$ to $-10\,000\text{ BP}$)**: $\tau \approx 45\text{--}50\%$. Archeological bone beds, lithic scatters, and ice-core proxies capture order-of-magnitude demographic densities and global climate anomalies.

### 2.3 Astronomical Time Coordinate & Numerical Integration Principles

#### 1. Exact Astronomical Constants (IAU 1976 / 2012):
Temporal integration across multi-millennial horizons is anchored to the International Astronomical Union (IAU) Julian year and synodic month:
$$J_{\text{yr}} = 365.25\,\text{d}, \qquad M_{\text{syn}} = 30.4375\,\text{d}, \qquad \tau_{\text{d}} = 86\,400\,\text{s}, \qquad \tau_{\text{yr}} = J_{\text{yr}} \cdot \tau_{\text{d}} = 3.15576 \times 10^7\,\text{s}$$

For a time increment $\Delta t_{\text{d}} \ge 10^{-4}\,\text{d}$:
$$\Delta t_{\text{yr}} = \frac{\Delta t_{\text{d}}}{J_{\text{yr}}}, \qquad \Delta t_{\text{mo}} = \frac{\Delta t_{\text{d}}}{M_{\text{syn}}}, \qquad \Delta t_{\text{s}} = \Delta t_{\text{d}} \cdot \tau_{\text{d}}$$

#### 2. Numerical Integration Strategy & The Computational ROI Filter:
The numerical solver architecture partitions differential systems based on their mathematical stiffness and computational return on investment (Computational ROI):

1. **Option A — First-Order Explicit Euler with Unified $\Delta t$ (Production Baseline)**:
   - Evaluates continuous state transitions and spatial fluxes as:
     $$\mathbf{y}_{k+1} = \mathbf{y}_k + \Delta t \cdot \mathbf{f}(\mathbf{y}_k)$$
   - **Computational ROI Filter Compliance**: Strict $\mathcal{O}(1)$ operations per cell, zero object allocations per tick, zero GC pressure, and optimal auto-vectorization across the DOD `WorldBuffer` flat contiguous primitive arrays.
   - Truncation error is $\mathcal{O}(\Delta t)$. Numerical drift on non-linear limit cycles (such as Turchin SDT secular cycles) is bounded by continuous $C^1$ Fermi-Dirac and hyperbolic tangent saturations.

2. **Option C Closed-Form Analytical Exponential Integration (Stiff Dissipative & Relaxation Terms)**:
   - For rapid physical dissipation, thermal relaxation, radioactive decay, or crisis starvation mortality:
     $$\frac{dy}{dt} = -\lambda (y - y^*), \quad \lambda \gg 1$$
   - A naive explicit Euler update $y_{k+1} = y_k - \lambda \Delta t (y_k - y^*)$ exhibits catastrophic numerical oscillations and non-physical negative states whenever $\Delta t > 2 / \lambda$.
   - Ether systematically resolves such stiff terms using the exact analytical exponential operator:
     $$y_{k+1} = y^* + (y_k - y^*) \cdot \exp(-\lambda \cdot \Delta t)$$
   - This formulation is **unconditionally $L$-stable**, preserves physical non-negativity across arbitrary step sizes, and incurs negligible CPU overhead ($\mathcal{O}(1)$ scalar transcendental evaluation).

3. **Epistemic Delimitation of Higher-Order & Full Implicit Solvers (Option B & Full Option C)**:
   - **Higher-Order Multistep / Runge-Kutta 4th Order (RK4 - Option B)**: While offering $\mathcal{O}(\Delta t^4)$ local precision and exact phase preservation on conservative oscillators, standard RK4 quadruples derivative evaluations ($4 \times$ CPU complexity) and triples intermediate buffer memory bandwidth ($\mathcal{O}(4N)$ memory cache footprint), severely degrading interactive framerates.
   - **Monolithic Implicit Newton-Raphson (Full Option C)**: Eliminates stiffness limits but requires computing and inverting high-dimensional cellular Jacobian matrices ($\mathcal{O}(M^3)$ per cell), which is computationally intractable for $175\,000+$ cell meshes in real time.
   - Consequently, Option B and full implicit Newton schemes are restricted to offline mathematical benchmarking suites (`ScientificModelFalsificationAndValidationSuite.java`); production simulation runs rely on Option A stabilized by analytical exponential closures.
- **Agrarian Antiquity & High Middle Ages ($-10\,000\text{ BP}$ to $1500\text{ CE}$)**: $\tau \approx 25\text{--}35\%$. Textual tax records, grain censuses, and numismatic surveys provide regional population bounds.
- **Early Modern & First Industrial Era ($1500\text{ CE}$ to $1900\text{ CE}$)**: $\tau \approx 15\text{--}20\%$ at $1800\text{ CE}$, tightening to $\sim 10\%$ by $1900\text{ CE}$ with national censuses, bill of mortality records, and international trade statistics.
- **Contemporary Era ($1950\text{ CE}$ to $2025\text{ CE}$)**: $\tau \to 5\%$. Governed by global census databases (UN WPP), satellite remote sensing, and national macroeconomic accounts.
- **Prospective Horizon ($> 2025\text{ CE}$)**: Evaluated at the $5\%$ baseline with branching scenario trajectories (SSP1 through SSP5).

#### 3. Spatial Hold-Out & Free-Run Stability Requirements:
- **Spatial Out-of-Sample Hold-Out Validation**: Models parameterized on Eurasian historical trajectories must pass validation on the Pre-Columbian Americas (e.g. Mesoamerican maize agglomeration without draft animals or iron) and Sub-Saharan Africa (e.g. Nok iron bloomery within tsetse fly zones without cattle traction) with zero ad-hoc parametric alterations.
- **Multi-Century Unguided Free-Run Stability**: The simulation must execute open-loop multi-century runs ($>500\text{ years}$) without boundary interventions, maintaining thermodynamic conservation of mass and energy while avoiding population divergence, extinction, or negative state variables.

---
## 3. Tier 1  -  Core Model Physics Specifications (One-by-One)

### Section A: Celestial Mechanics, Radiation & Global Thermodynamics

#### 1. Astronomical Milankovitch Orbital Forcing (`MilankovitchOrbitalEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Berger, A. (1978). *Long-term variations of daily insolation and Quaternary climatic changes*. Journal of Atmospheric Sciences, 35(12), 2362-2367; Laskar, J., et al. (2004). *A long-term numerical solution for the insolation quantities of the Earth*. Astronomy & Astrophysics, 428(1), 261-285.
* **Governing Equations**:
  Keplerian orbital variations governing top-of-atmosphere (TOA) daily insolation $S(\phi, \delta)$:
  $$S(\phi, \delta) = \frac{S_0}{\pi} \left(\frac{1 + e\cos\nu}{1 - e^2}\right)^2 \cdot \left[ H_0 \sin\phi \sin\delta + \cos\phi \cos\delta \sin H_0 \right]$$
  where hour angle $H_0 = \arccos(-\tan\phi \tan\delta)$ and solar declination $\delta = \arcsin(\sin\varepsilon \sin\lambda)$.
* **Parameters & Physical Units**:
  * $S_0 = 1361.0\text{ W/m}^2$: Solar irradiance constant at 1 AU [$\text{W/m}^2$].
  * $e(t) \in [0.005, 0.058]$: Orbital eccentricity [dimensionless] (100,000-year cycle).
  * $\varepsilon(t) \in [22.1^\circ, 24.5^\circ]$: Planetary obliquity/axial tilt [degrees] (41,000-year cycle).
  * $\varpi(t)$: Geocentric solar ecliptic longitude of perihelion [degrees] ($\varpi_0 = 282.93^\circ$ at J2000.0); climatic precession index $e \sin\varpi$ (23,000-year cycle).
  * $\phi$: Geographic latitude [radians].
  * $\nu$: True anomaly along Keplerian orbit [radians].
* **Computational Complexity**: $O(1)$ per cell; evaluated on Monthly Step ($\Delta t = 30\text{ days}$).

#### 2. Stefan-Boltzmann Radiative Balance & Albedo Feedback (`AlbedoClimateEngine`, `GreenhouseRadiativeEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Budyko, M. I. (1969). *The effect of solar radiation variations on the climate of the Earth*. Tellus, 21(5), 611-619; Sellers, W. D. (1969). *A global climatic model based on the energy balance of the earth-atmosphere system*. Journal of Applied Meteorology, 8(3), 392-400.
* **Governing Equations**:
  $$\epsilon \sigma T_{\text{surface}}^4 = \frac{S(\phi)}{4} (1 - \alpha_{\text{albedo}}) + \Delta F_{\text{greenhouse}}$$
  $$\alpha_{\text{albedo}} = f_{\text{ice}}\alpha_{\text{ice}} + f_{\text{veg}}\alpha_{\text{veg}} + f_{\text{soil}}\alpha_{\text{soil}} + f_{\text{water}}\alpha_{\text{water}}$$
* **Parameters & Physical Units**:
  * $\sigma = 5.670374 \times 10^{-8}\text{ W/(m}^2\text{K}^4)$: Stefan-Boltzmann physical constant.
  * $\epsilon = 0.98$: Surface infrared emissivity [dimensionless].
  * $T_{\text{surface}}$: Absolute surface skin temperature [$\text{Kelvin}$].
  * $\alpha_{\text{albedo}}$: Cell composite surface albedo [dimensionless, $0.0 - 1.0$].
  * $\alpha_{\text{ice}} = 0.75$, $\alpha_{\text{veg}} = 0.15$, $\alpha_{\text{soil}} = 0.28$, $\alpha_{\text{water}} = 0.06$: End-member surface albedos.
  * $\Delta F_{\text{greenhouse}}$: Net downward longwave greenhouse radiative forcing [$\text{W/m}^2$].
* **Computational Complexity**: $O(1)$ per cell.

#### 3. Clausius-Clapeyron Atmospheric Moisture Capacity (`GreenhouseRadiativeEngine`, `MaximumEntropyProductionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Clausius, R. (1850). *Über die bewegende Kraft der Wärme*. Annalen der Physik, 155(3), 368-397; Bolton, D. (1980). *The computation of equivalent potential temperature*. Monthly Weather Review, 108(7), 1046-1053.
* **Governing Equations**:
  $$e_{\text{sat}}(T) = e_0 \cdot \exp\left( \frac{L_v}{R_v} \left( \frac{1}{T_0} - \frac{1}{T} \right) \right)$$
  $$q_{\text{sat}} = \frac{\epsilon_{\text{wv}} e_{\text{sat}}(T)}{P_{\text{atm}} - (1 - \epsilon_{\text{wv}}) e_{\text{sat}}(T)}$$
* **Parameters & Physical Units**:
  * $e_{\text{sat}}$: Saturation water vapor pressure [$\text{hPa}$].
  * $e_0 = 6.112\text{ hPa}$: Saturation vapor pressure at triple point $T_0 = 273.16\text{ K}$.
  * $L_v = 2.501 \times 10^6\text{ J/kg}$: Latent heat of vaporization of water.
  * $R_v = 461.5\text{ J/(kg}\cdot\text{K)}$: Specific gas constant for water vapor ($L_v / R_v \approx 5423\text{ K}$).
  * $q_{\text{sat}}$: Saturation specific humidity [$\text{kg/kg}$].
  * $P_{\text{atm}}$: Atmospheric barometric pressure [$\text{hPa}$].
  * $\epsilon_{\text{wv}} = 0.622$: Molecular mass ratio of water vapor to dry air.
* **Computational Complexity**: $O(1)$ per cell.

#### 4. 3-Cell Atmospheric Circulation & Coriolis Acceleration (`AtmosphericCirculationHadleyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Lorenz, E. N. (1967). *The Nature and Theory of the General Circulation of the Atmosphere*. World Meteorological Organization; Holton, J. R. (2004). *An Introduction to Dynamic Meteorology*. Elsevier Academic Press.
* **Governing Equations**:
  $$f = 2\Omega \sin\phi, \quad \mathbf{v}_{\text{geostrophic}} = -\frac{1}{\rho_{\text{air}} f} \nabla P \times \hat{\mathbf{k}}$$
  $$\frac{\partial \mathbf{u}}{\partial t} + (\mathbf{u} \cdot \nabla)\mathbf{u} = -\frac{1}{\rho_{\text{air}}} \nabla P - f \hat{\mathbf{k}} \times \mathbf{u} - \kappa_{\text{drag}} \mathbf{u}$$
  $$p_{\text{wind}}(\phi) = \frac{1}{2} \rho_{\text{air}} \|\mathbf{u}(\phi)\|^3 \quad [\text{W/m}^2]$$
* **Parameters & Physical Units**:
  * $\Omega = 7.292115 \times 10^{-5}\text{ rad/s}$: Earth sidereal rotational angular velocity.
  * $f$: Coriolis parameter [$\text{s}^{-1}$].
  * $\rho_{\text{air}}$: Atmospheric density [$\text{kg/m}^3$] ($\approx 1.225\text{ kg/m}^3$ at sea level).
  * $P$: Atmospheric pressure field [$\text{Pa}$].
  * $\mathbf{u} = (u, v)$: Horizontal wind velocity vector [$\text{m/s}$], resolving trade winds ($[0^\circ, 30^\circ]$), westerlies ($[30^\circ, 60^\circ]$), and polar easterlies ($[60^\circ, 90^\circ]$).
  * $p_{\text{wind}}(\phi)$: Meteorological boundary-layer kinetic wind power density [$\text{W/m}^2$].
* **Computational Complexity**: $O(1)$ per cell.

#### 5. Greenhouse Radiative Forcing (`GreenhouseRadiativeEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Myhre, G., et al. (1998). *New estimates of radiative forcing due to well mixed greenhouse gases*. Geophysical Research Letters, 25(14), 2715-2718; IPCC AR6 WG1 (2021).
* **Governing Equations**:
  $$\Delta F_{\text{GHG}} = 5.35 \cdot \ln\left( \frac{C_{\text{CO2}}}{C_{\text{CO2}, 0}} \right) + 0.036 \cdot (\sqrt{M_{\text{CH4}}} - \sqrt{M_0}) - [f(M_{\text{CH4}}, N_0) - f(M_0, N_0)]$$
* **Parameters & Physical Units**:
  * $\Delta F_{\text{GHG}}$: Radiative forcing perturbation at the tropopause [$\text{W/m}^2$].
  * $C_{\text{CO2}}$: Atmospheric carbon dioxide mixing ratio [$\text{ppm}$].
  * $C_{\text{CO2}, 0} = 280.0\text{ ppm}$: Pre-industrial Holocene baseline concentration.
  * $M_{\text{CH4}}$: Atmospheric methane mixing ratio [$\text{ppb}$].
  * $M_0 = 700.0\text{ ppb}$: Pre-industrial methane baseline.
* **Computational Complexity**: $O(1)$ per cell.

#### 6. Stommel 2-Box AMOC Thermohaline Circulation (`ThermohalineStommelAMOCEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Stommel, H. (1961). *Thermohaline convection with two stable regimes of flow*. Tellus, 13(2), 224-230; Rahmstorf, S. (1995). *Bifurcations of the Atlantic thermohaline circulation in two different climate models*. Nature, 378(6553), 145-149; Broecker, W. S. (1998). *Paleoceanography*, 13(2), 119-121; EPICA Community Members (2006). *Nature*, 444, 195-198.
* **Governing Equations**:
  $$q_{\text{AMOC}} = k \left| \alpha_T (T_e - T_p) - \beta_S (S_e - S_p) \right|$$
  $$\frac{d(T_e - T_p)}{dt} = -\frac{(T_e - T_p) - \Delta T^*}{\tau_T} - \frac{2 \, q_{\text{AMOC}}}{V_p} (T_e - T_p)$$
  $$\frac{d(S_e - S_p)}{dt} = \frac{2 \, F_w S_0}{V_p} - \frac{2 \, q_{\text{AMOC}}}{V_p} (S_e - S_p)$$
* **Parameters & Physical Units**:
  * $q_{\text{AMOC}}$: Overturning volumetric transport [$\text{Sverdrups, } 1\text{ Sv} = 10^6\text{ m}^3\text{/s}$], baseline unperturbed $q_0 \approx 18.2\text{ Sv}$.
  * $C = 11500.0$: Hydrodynamic Stommel coupling constant.
  * $\alpha_T = 2.0 \times 10^{-4}\text{ K}^{-1}$: Thermal expansion coefficient of seawater.
  * $\beta_S = 7.5 \times 10^{-4}\text{ PSU}^{-1}$: Haline contraction coefficient of seawater.
  * $\eta_{\text{gyre}} = 0.55$: Effective temperature contrast across North Atlantic subtropical/subpolar gyres.
  * $\tau_T = 20.0\text{ years}$: Fast atmospheric thermal relaxation timescale.
  * $\tau_{\text{diff}} = 500.0\text{ years}$: Multi-centennial slow haline memory and diffusion timescale.
  * $V_p = 250.0\text{ Sv-years}$: Convective subpolar box effective volume.
  * $F_{\text{melt}}$: Meltwater discharge flux [$\text{Sv}$], driven endogenously by Milankovitch 65°N summer orbital insolation anomaly and polar ice sheet runoff.
  * Bipolar Seesaw: Validated against Greenland GRIP / Antarctic EPICA ice cores showing anti-phase oceanic heat redistribution ($\Delta T_{\text{South}} / \Delta T_{\text{North}} = -0.22$).
* **Computational Complexity**: $O(1)$ dynamic box integration sub-stepped for unconditional stability across deep-time millenia.

#### 7. Dynamic Fluvial Sedimentation & Stokes Settling (`DynamicHydrographicSiltationEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Stokes, G. G. (1851). *On the effect of the internal friction of fluids on the motion of pendulums*. Transactions of the Cambridge Philosophical Society, 9, 8-106; Einstein, H. A. (1950). *The bed-load function for sediment transportation in open channel flows*. USDA Technical Bulletin No. 1026.
* **Governing Equations**:
  $$v_{\text{settling}} = \frac{2}{9} \frac{(\rho_{\text{sed}} - \rho_{\text{water}}) g r^2}{\mu_{\text{water}}}$$
  $$\frac{\partial S_{\text{alluvial}}}{\partial t} = Q_{\text{inflow}} C_{\text{sed}} - v_{\text{settling}} S_{\text{alluvial}} A_{\text{river}}$$
* **Parameters & Physical Units**:
  * $v_{\text{settling}}$: Particle settling terminal velocity [$\text{m/s}$].
  * $\rho_{\text{sed}} = 2650.0\text{ kg/m}^3$: Quartz sediment density.
  * $\rho_{\text{water}} = 1000.0\text{ kg/m}^3$: Water density.
  * $r$: Mean sediment grain radius [$\text{meters}$] (e.g. $2.0 \times 10^{-5}\text{ m}$ for silt).
  * $\mu_{\text{water}} = 1.002 \times 10^{-3}\text{ Pa}\cdot\text{s}$: Dynamic viscosity of water at 20°C.
  * $S_{\text{alluvial}}$: Deposited alluvial topsoil layer mass [$\text{kg/m}^2$].
* **Computational Complexity**: $O(1)$ per fluvial cell.

#### 8. Manning-Strickler Open-Channel Hydraulics (`ManningStricklerHydrodynamicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Manning, R. (1890). *On the flow of water in open channels and pipes*. Transactions of the Institution of Civil Engineers of Ireland, 20, 161-207; Chow, V. T. (1959). *Open-Channel Hydraulics*. McGraw-Hill.
* **Governing Equations**:
  $$V = \frac{1}{n} R_h^{2/3} S_0^{1/2}, \quad Q = V \cdot A_{\text{cross}}$$
* **Parameters & Physical Units**:
  * $V$: Mean cross-sectional flow velocity [$\text{m/s}$].
  * $n$: Manning roughness coefficient [$\text{s/m}^{1/3}$] ($0.025 - 0.050$ for natural rivers).
  * $R_h = A_{\text{cross}} / P_{\text{wetted}}$: Hydraulic radius [$\text{meters}$].
  * $S_0 = |\nabla z|$: Energy slope / channel bed gradient [dimensionless].
  * $Q$: Fluvial volumetric discharge [$\text{m}^3\text{/s}$].
  * $A_{\text{cross}}$: Channel wetted cross-sectional area [$\text{m}^2$].
* **Computational Complexity**: $O(1)$ per river reach.

#### 9. 2D Lateral Darcy Aquifer Depletion (`AquiferDepletionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Darcy, H. (1856). *Les fontaines publiques de la ville de Dijon*. Victor Dalmont; Theis, C. V. (1935). *The relation between the lowering of the piezometric surface and the rate and duration of discharge of a well using ground-water storage*. Eos, Transactions American Geophysical Union, 16(2), 519-524.
* **Governing Equations**:
  $$S_s \frac{\partial h}{\partial t} = \nabla \cdot (K_{\text{hydraulic}} \nabla h) + R_{\text{recharge}} - W_{\text{pumping}}$$
* **Parameters & Physical Units**:
  * $h$: Hydraulic piezometric head [$\text{meters}$].
  * $S_s$: Specific storage coefficient of porous media [$\text{m}^{-1}$].
  * $K_{\text{hydraulic}}$: Hydraulic conductivity tensor [$\text{m/s}$] ($10^{-5}\text{ m/s}$ in sandstone).
  * $R_{\text{recharge}}$: Rainfall percolation recharge [$\text{m/s}$].
  * $W_{\text{pumping}}$: Anthropogenic well extraction flux per unit area [$\text{m/s}$].
* **Computational Complexity**: $O(6)$ per hexagonal cell (Ring-1 stencil).

---

### Section B: Solid Earth, Geophysics & Cryosphere

#### 10. Airy Isostasy & Crustal Root Compensation (`AiryIsostasyCrustalRootEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Airy, G. B. (1855). *On the computation of the effect of the attraction of mountain-masses*. Philosophical Transactions of the Royal Society of London, 145, 101-104; Turcotte, D. L., & Schubert, G. (2014). *Geodynamics*. Cambridge University Press.
* **Governing Equations**:
  $$t_{\text{root}} = h_{\text{topo}} \cdot \frac{\rho_{\text{crust}}}{\rho_{\text{mantle}} - \rho_{\text{crust}}}$$
* **Parameters & Physical Units**:
  * $t_{\text{root}}$: Crustal root penetration depth into upper asthenosphere [$\text{meters}$].
  * $h_{\text{topo}}$: Surface topographic elevation above geoid [$\text{meters}$].
  * $\rho_{\text{crust}} = 2700.0\text{ kg/m}^3$: Mean continental granitic crustal density.
  * $\rho_{\text{mantle}} = 3300.0\text{ kg/m}^3$: Sub-crustal lithospheric mantle peridotite density.
* **Computational Complexity**: $O(1)$ per cell.

#### 11. Viscoelastic Glacial Isostatic Adjustment (`GlacialIsostaticAdjustmentEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Peltier, W. R. (2004). *Global glacial isostasy and the surface of the ice-age Earth: the ICE-5G (VM2) model and GRACE*. Annual Review of Earth and Planetary Sciences, 32, 111-149.
* **Governing Equations**:
  $$\frac{\partial w_{\text{crust}}}{\partial t} = -\frac{1}{\tau_{\text{GIA}}} \left( w_{\text{crust}} - \frac{\rho_{\text{ice}}}{\rho_{\text{mantle}}} h_{\text{ice}} \right) + \kappa_{\text{flexure}} \nabla^2 w_{\text{crust}}$$
* **Parameters & Physical Units**:
  * $w_{\text{crust}}$: Downward crustal deflection [$\text{meters}$].
  * $h_{\text{ice}}$: Glacial ice sheet thickness [$\text{meters}$].
  * $\rho_{\text{ice}} = 917.0\text{ kg/m}^3$: Glacial ice density.
  * $\tau_{\text{GIA}} \approx 4000.0\text{ years}$: Mantle Maxwell viscoelastic relaxation timescale.
  * $\kappa_{\text{flexure}}$: Lithospheric flexural rigidity parameter [$\text{m}^2\text{/year}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 12. Glacial Thermodynamic Positive Degree-Day Melt (`GlacialThermodynamicMeltEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Reeh, N. (1991). *Parameterization of melt rate and surface temperature on the Greenland ice sheet*. Polarforschung, 59(3), 113-128; Braithwaite, R. J. (1995). *Positive degree-day factors for ablation on the Greenland ice sheet*. Journal of Glaciology, 41(137), 153-160.
* **Governing Equations**:
  $$\text{PDD} = \int_{\text{year}} \max(0, T_{\text{surface}}(t) - T_0) \, dt$$
  $$M_{\text{melt}} = \text{PDD} \cdot \alpha_{\text{pdd}}, \quad h_{\text{ice}}(t+\Delta t) = \max\left(0, h_{\text{ice}}(t) + P_{\text{snow}} - \frac{M_{\text{melt}}}{\rho_{\text{ice}}}\right)$$
* **Parameters & Physical Units**:
  * $\text{PDD}$: Positive Degree-Day sum [$\text{K}\cdot\text{days}$].
  * $T_0 = 273.15\text{ K}$: Ice melting point.
  * $\alpha_{\text{pdd}} = 8.0\text{ mm water equivalent}/(\text{K}\cdot\text{day})$: Ice ablation degree-day factor.
  * $M_{\text{melt}}$: Meltwater mass production [$\text{kg/m}^2$].
  * $P_{\text{snow}}$: Solid snow precipitation accumulation [$\text{meters of ice equivalent}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 13. Permafrost Thaw & Methane Clathrate Release (`PermafrostThawEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Schuur, E. A. G., et al. (2015). *Climate change and the permafrost carbon feedback*. Nature, 520(7546), 171-179.
* **Governing Equations**:
  $$\frac{\partial T_{\text{soil}}(z)}{\partial t} = \alpha_{\text{th}} \frac{\partial^2 T_{\text{soil}}}{\partial z^2}, \quad F_{\text{CH4}} = k_{\text{clathrate}} \cdot \max(0, z_{\text{active}} - z_{\text{permafrost}})$$
* **Parameters & Physical Units**:
  * $T_{\text{soil}}(z)$: Subsurface ground temperature profile [$\text{Kelvin}$].
  * $\alpha_{\text{th}}$: Soil thermal diffusivity [$\text{m}^2\text{/s}$].
  * $z_{\text{active}}$: Seasonally thawed active layer depth [$\text{meters}$].
  * $z_{\text{permafrost}}$: Permafrost table boundary depth [$\text{meters}$].
  * $F_{\text{CH4}}$: Biogenic methane gas release flux [$\text{g CH}_4\text{/(m}^2\cdot\text{year)}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 13.1. Marine Methane Clathrate Thermodynamic Stability & Dissociation (`MethaneClathrateStabilityEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Dickens, G. R., et al. (1995). *Dissociation of oceanic methane hydrate as a cause of the carbon isotope excursion at the end of the Paleocene*. Paleoceanography, 10(6), 965-971; Buffett, B., & Archer, D. (2004). *Global inventory of methane clathrate: sensitivity to changes in the deep ocean*. Global Biogeochemical Cycles, 18(4); Archer, D., et al. (2009). *Ocean methane hydrates as a slow tipping point in the global carbon cycle*. PNAS, 106(49), 20596-20601; Sloan, E. D. (1998). *Clathrate Hydrates of Natural Gases*. Marcel Dekker.
* **Governing Equations**:
  $$T_{\text{eq}}(P) = T_{\text{base}} + \alpha_{\text{hydrate}} \cdot \ln\left(\frac{P}{P_0}\right) - \Delta T_{\text{salinity}} \quad [^\circ\text{C}]$$
  $$P(z) = \frac{\rho_{\text{sw}} \cdot g \cdot z}{10^6} \quad [\text{MPa}], \qquad Z_{\text{GHSZ}} = \max\left(0, \; \frac{T_{\text{eq}}(P(z)) - T_{\text{bottom}}}{G_T}\right) \quad [\text{m}]$$
  $$\Phi_{\text{CH}_4} = \kappa_{\text{dissoc}} \cdot \max(0, T_{\text{bottom}} - T_{\text{bottom,baseline}}) \cdot \Delta t \quad [\text{ppm / yr}]$$
* **Parameters & Physical Units**:
  * $\rho_{\text{sw}} = 1025.0\text{ kg/m}^3$: Seawater density.
  * $P_0 = 2.50\text{ MPa}$: Onset pressure for clathrate stability ($\approx 250\text{ m}$ water depth).
  * $T_{\text{base}} = -1.50^\circ\text{C}$: Phase temperature at reference pressure $P_0$.
  * $\alpha_{\text{hydrate}} = 9.50^\circ\text{C}$: Logarithmic phase boundary slope.
  * $\Delta T_{\text{salinity}} = 1.20^\circ\text{C}$: Seawater freezing/hydrate point depression (35 PSU).
  * $G_T = 0.035^\circ\text{C/m}$ ($35^\circ\text{C/km}$): Sediment geothermal temperature gradient.
  * $T_{\text{bottom,baseline}} = 2.0^\circ\text{C}$: Pre-industrial deep ocean bottom water temperature.
* **Epistemic Invariant**: Provides a first-principles thermodynamic phase boundary coupling ocean warming to catastrophic methane outgassing (e.g. PETM, post-2025 deep warming) without arbitrary step triggers.
* **Computational Complexity**: $O(1)$ per marine cell.

#### 14. Crustal Geothermal Heat Flux (`CrustalGeothermalEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Pollack, H. N., Hurter, S. J., & Johnson, J. R. (1993). *Heat flow from the Earth's interior: Analysis of the global data set*. Reviews of Geophysics, 31(3), 267-280.
* **Governing Equations**:
  $$q_{\text{geo}} = -k_{\text{rock}} \frac{\partial T}{\partial z} + H_{\text{radiogenic}}, \quad H_{\text{radiogenic}} = \rho_{\text{crust}} \sum_i C_i H_i$$
* **Parameters & Physical Units**:
  * $q_{\text{geo}}$: Surface conductive geothermal heat flux [$\text{mW/m}^2$] (global mean $\approx 65\text{ mW/m}^2$ continental, $\approx 100\text{ mW/m}^2$ oceanic).
  * $k_{\text{rock}} = 2.5\text{ W/(m}\cdot\text{K)}$: Crustal rock thermal conductivity.
  * $H_{\text{radiogenic}}$: Radiogenic heat production from $^{238}\text{U}$, $^{235}\text{U}$, $^{232}\text{Th}$, $^{40}\text{K}$ decay [$\text{W/m}^3$].
* **Computational Complexity**: $O(1)$ per cell.

#### 15. Eustatic Sea-Level Transition & Coastal Inundation (`SeaLevelTransitionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Lambeck, K., et al. (2014). *Sea level and global ice volumes from the Last Glacial Maximum to the Holocene*. PNAS, 111(43), 15296-15303.
* **Governing Equations**:
  $$\Delta z_{\text{sea}} = \frac{\Delta V_{\text{ice\_melt}}}{A_{\text{ocean}}} + \alpha_{\text{thermo}} V_{\text{ocean}} \Delta T_{\text{ocean}}$$
* **Parameters & Physical Units**:
  * $\Delta z_{\text{sea}}$: Global eustatic sea level anomaly relative to present [$\text{meters}$] (e.g. $-125\text{ m}$ at Last Glacial Maximum $-20\,000\text{ BP}$).
  * $\Delta V_{\text{ice\_melt}}$: Net volume change of grounded continental ice sheets [$\text{m}^3$].
  * $A_{\text{ocean}} = 3.61 \times 10^{14}\text{ m}^2$: Global open ocean surface area.
  * $\alpha_{\text{thermo}} = 1.5 \times 10^{-4}\text{ K}^{-1}$: Global ocean thermosteric expansion coefficient.
* **Computational Complexity**: $O(1)$ globally.

---

### Section C: Pedology, Agronomy, Biogeochemistry & Atmosphere

#### 16. van Genuchten Soil Moisture Retention (`SoilWaterRetentionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: van Genuchten, M. T. (1980). *A closed-form equation for predicting the hydraulic conductivity of unsaturated soils*. Soil Science Society of America Journal, 44(5), 892-898.
* **Governing Equations**:
  $$\theta(h) = \theta_r + \frac{\theta_s - \theta_r}{\left[ 1 + |\alpha_{\text{vg}} h|^{n_{\text{vg}}} \right]^{m_{\text{vg}}}}, \quad m_{\text{vg}} = 1 - 1/n_{\text{vg}}$$
* **Parameters & Physical Units**:
  * $\theta$: Volumetric soil water content [$\text{m}^3/\text{m}^3$].
  * $\theta_r$: Residual soil water content [$\text{m}^3/\text{m}^3$] ($0.04 - 0.08$).
  * $\theta_s$: Saturated soil water content / porosity [$\text{m}^3/\text{m}^3$] ($0.40 - 0.50$).
  * $h$: Soil matric suction pressure head [$\text{meters}$ of water].
  * $\alpha_{\text{vg}}$, $n_{\text{vg}}$: Empirical soil pore geometry shape parameters.
  * Plant Available Water Capacity (AWC) evaluated between field capacity ($-33\text{ kPa}$) and permanent wilting point ($-1500\text{ kPa}$).
* **Computational Complexity**: $O(1)$ per cell.

#### 16.1. Plant Xylem Hydraulic Cavitation & Runaway Embolism (`XylemHydraulicCavitationEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Tyree, M. T., & Sperry, J. S. (1989). *Vulnerability of xylem to cavitation and embolism*. Annual Review of Plant Physiology and Plant Molecular Biology, 40(1), 19-36; Choat, B., et al. (2012). *Global convergence in the vulnerability of forests to drought*. Nature, 491(7426), 752-755; McDowell, N., et al. (2008). *Mechanisms of plant survival and mortality during drought: why do some plants survive while others succumb to drought?* New Phytologist, 178(4), 719-739.
* **Governing Equations**:
  $$\Psi_{\text{soil}}(S_w) = -\Psi_e \cdot S_w^{-b} \quad [\text{MPa}]$$
  $$\text{PLC}(\Psi) = \frac{100.0}{1.0 + \left(\frac{|\Psi|}{|\Psi_{50}|}\right)^a} \quad [\%]$$
  $$\mu_{\text{dieback}} = \mu_{\max} \cdot \left(\frac{\max(0, \text{PLC} - 50.0)}{50.0}\right)^2 \quad [\text{year}^{-1}]$$
* **Parameters & Physical Units**:
  * $\Psi_e = 0.035\text{ MPa}$: Air-entry suction potential for standard loam soils.
  * $b = 4.5$: Campbell soil water retention exponent.
  * $a = 3.5$: Vulnerability curve Weibull shape parameter.
  * $\Psi_{50}$: Species-specific water potential causing $50\%$ loss of hydraulic conductivity (Crops: $-2.5\text{ MPa}$, Deciduous: $-4.2\text{ MPa}$, Conifers: $-5.8\text{ MPa}$, Desert sclerophylls: $-8.5\text{ MPa}$).
  * $\mu_{\max} = 0.40\text{ yr}^{-1}$: Maximum annual forest canopy dieback rate under complete cavitation.
* **Epistemic Invariant**: Replaces arbitrary linear drought yield losses with the universal non-linear cavitation physics responsible for major historical agrarian collapses (Akkad 4.2 ka BP, Maya Classic drought, 1876-1878 Great Drought).
* **Computational Complexity**: $O(1)$ per cell.

#### 17. Soil N-P-K Stoichiometry & Liebig Minimum Law (`SoilNutrientNPKEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: von Liebig, J. (1840). *Die organische Chemie in ihrer Anwendung auf Agricultur und Physiologie*. Vieweg; Smil, V. (2000). *Enriching the Earth: Fritz Haber, Carl Bosch, and the Transformation of World Food Production*. MIT Press.
* **Governing Equations**:
  $$\frac{dN}{dt} = R_{\text{biofix}} + F_{\text{manure}} + F_{\text{mineral}} - Y_{\text{crop}} \kappa_N - \lambda_{\text{leach}} N$$
  $$\frac{dP}{dt} = W_{\text{weathering}} + F_{\text{phosphorus}} - Y_{\text{crop}} \kappa_P, \quad \frac{dK_{\text{soil}}}{dt} = W_K - Y_{\text{crop}} \kappa_K$$
  $$Y_{\text{crop}} = Y_{\text{pot}} \cdot \min\left( \frac{N}{N_0}, \frac{P}{P_0}, \frac{K_{\text{soil}}}{K_0}, \frac{\theta}{\theta_0} \right)$$
* **Parameters & Physical Units**:
  * $N, P, K_{\text{soil}}$: Plant-available bio-element soil stocks [$\text{kg/ha}$].
  * $Y_{\text{crop}}$: Actual agricultural crop yield [$\text{metric tons/ha}$].
  * $Y_{\text{pot}}$: Potential yield under optimal climate and unlimited nutrients [$\text{t/ha}$].
  * $\kappa_N, \kappa_P, \kappa_K$: Stoichiometric mineral uptake coefficients per unit harvest [$\text{kg/metric ton}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 18. Universal Soil Loss Equation (USLE) Pedology (`DeforestationErosionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Wischmeier, W. H., & Smith, D. D. (1978). *Predicting rainfall erosion losses*. USDA Agricultural Handbook No. 537; Montgomery, D. R. (2007). *Soil erosion and agricultural sustainability*. PNAS, 104(33), 13268-13272.
* **Governing Equations**:
  $$E_{\text{soil}} = R_{\text{rain}} \cdot K_{\text{erod}} \cdot LS_{\text{topo}} \cdot C_{\text{cover}} \cdot P_{\text{practice}}$$
  $$C_{\text{cover}} = \exp(-\gamma_{\text{veg}} \cdot f_{\text{forest}})$$
* **Parameters & Physical Units**:
  * $E_{\text{soil}}$: Annual soil detachment rate [$\text{metric tons/(ha}\cdot\text{year)}$].
  * $R_{\text{rain}}$: Rainfall erosivity factor [$\text{MJ}\cdot\text{mm/(ha}\cdot\text{h}\cdot\text{year)}$].
  * $K_{\text{erod}}$: Soil erodibility factor [$\text{t}\cdot\text{ha}\cdot\text{h/(ha}\cdot\text{MJ}\cdot\text{mm)}$].
  * $LS_{\text{topo}}$: Topographic slope length and steepness factor [dimensionless].
  * $C_{\text{cover}}$: Vegetative canopy cover protection factor [dimensionless, $0.001 - 1.0$].
  * $P_{\text{practice}}$: Agricultural contouring and conservation practice factor [dimensionless].
* **Computational Complexity**: $O(1)$ per cell.

#### 19. Aeolian Saltation & Dust Aerosol Mobilization (`AlbedoClimateEngine`, `DeforestationErosionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Gillette, D. A. (1978). *A wind tunnel simulation of the erosion of soil*. Atmospheric Environment, 12(8), 1735-1738; Shao, Y. (2008). *Physics and Modelling of Wind Erosion*. Springer.
* **Governing Equations**:
  $$F_{\text{dust}} = c_{\text{dust}} \cdot u_*^3 \left(1 - \frac{u_{*\text{crit}}^2}{u_*^2}\right) \cdot (1 - \text{SoilMoisture}) \cdot (1 - f_{\text{veg}})$$
* **Parameters & Physical Units**:
  * $F_{\text{dust}}$: Vertical dust aerosol mass emission flux [$\text{kg/(m}^2\cdot\text{s)}$].
  * $u_* = \sqrt{\tau_{\text{wind}} / \rho_{\text{air}}}$: Friction wind velocity at surface roughness height [$\text{m/s}$].
  * $u_{*\text{crit}}$: Threshold friction velocity required to dislodge dry sand/silt grains [$\text{m/s}$] ($\approx 0.25 - 0.40\text{ m/s}$).
  * $c_{\text{dust}}$: Soil texture mass mobilization coefficient [$\text{kg}\cdot\text{s}^2\text{/m}^5$].
* **Computational Complexity**: $O(1)$ per cell.

#### 20. Ocean Carbonate Chemistry & Acidification (`OceanAcidificationEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Zeebe, R. E., & Wolf-Gladrow, D. (2001). *CO2 in Seawater: Equilibrium, Kinetics, Isotopes*. Elsevier Oceanography Series; Feely, R. A., et al. (2004). *Impact of anthropogenic CO2 on the CaCO3 system in the oceans*. Science, 305(5682), 362-366.
* **Governing Equations**:
  $$[\text{H}^+] = \frac{K_1 K_2 [\text{CO}_2]}{[\text{CO}_3^{2-}]}, \quad \Omega_{\text{arag}} = \frac{[\text{Ca}^{2+}][\text{CO}_3^{2-}]}{K_{sp, \text{arag}}}$$
* **Parameters & Physical Units**:
  * $[\text{H}^+]$: Hydrogen ion concentration; $\text{pH} = -\log_{10}[\text{H}^+]$ [dimensionless].
  * $K_1, K_2$: Carbonic acid dissociation constants in seawater.
  * $[\text{CO}_3^{2-}]$: Dissolved carbonate ion concentration [$\mu\text{mol/kg}$].
  * $\Omega_{\text{arag}}$: Aragonite saturation state [dimensionless]. Undersaturation ($\Omega_{\text{arag}} < 1.0$) causes chemical dissolution of calcareous marine shells, collapsing coral reefs and shell-bearing fisheries.
* **Computational Complexity**: $O(1)$ per marine cell.

#### 21. Atmospheric Oxygen Photochemical Mass Balance (`AtmosphericOxygenEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kump, L. R., Kasting, J. F., & Crane, R. G. (2010). *The Earth System*. Prentice Hall; Berner, R. A. (2006). *GEOCARBSULF: a combined model for Phanerozoic atmospheric O2 and CO2*. Geochimica et Cosmochimica Acta, 70(23), 5653-5664.
* **Governing Equations**:
  $$\frac{d M_{\text{O2}}}{dt} = \sum_{i \in \text{cells}} \left( \text{NPP}_i - \text{HeterotrophicRespiration}_i \right) - F_{\text{combustion}} - F_{\text{mineral\_oxidation}}$$
* **Parameters & Physical Units**:
  * $M_{\text{O2}}$: Total atmospheric oxygen reservoir mass [$\text{kg}$] ($1.18 \times 10^{18}\text{ kg}$, $20.95\%\text{ by volume}$).
  * $\text{NPP}_i$: Net primary productivity net oxygen release [$\text{kg O}_2\text{/year}$].
  * $F_{\text{combustion}}$: Industrial fossil and biomass combustion oxygen consumption [$\text{kg O}_2\text{/year}$].
* **Computational Complexity**: $O(1)$ globally.

#### 22. Chapman Ozone Photolysis & Catalytic Depletion (`OzoneLayerDepletionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Chapman, S. (1930). *A theory of upper-atmospheric ozone*. Memoirs of the Royal Meteorological Society, 3, 103-125; Molina, M. J., & Rowland, F. S. (1974). *Stratospheric sink for chlorofluoromethanes: chlorine atom-catalysed destruction of ozone*. Nature, 249(5460), 810-812.
* **Governing Equations**:
  $$\frac{d[\text{O}_3]}{dt} = 2 J_{\text{O2}}[\text{O}_2] - k_1[\text{O}][\text{O}_3] - \sum_X k_X [X][\text{O}_3]$$
* **Parameters & Physical Units**:
  * $[\text{O}_3]$: Stratospheric ozone column density [$\text{Dobson Units (DU)}$] (baseline $\sim 300\text{ DU}$).
  * $J_{\text{O2}}$: Photolysis rate coefficient of molecular oxygen under UV-C ($\lambda < 242\text{ nm}$) [$\text{s}^{-1}$].
  * $X \in \{\text{NO}_x, \text{ClO}_x, \text{HO}_x\}$: Catalytic radical species concentrations [$\text{molecules/cm}^3$].
* **Computational Complexity**: $O(1)$ per cell.

#### 23. Soil Salinization Hydrological Mass-Balance (`SoilSalinizationHydrologyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Jacobsen, T., & Adams, R. M. (1958). *Salt and silt in ancient Mesopotamian agriculture*. Science, 128(3334), 1251-1258; Maas, E. V., & Hoffman, G. J. (1977). *Crop salt tolerance - current assessment*. Journal of the Irrigation and Drainage Division, 103(2), 115-134.
* **Governing Equations**:
  $$\frac{d [\text{Salts}]_{\text{soil}}}{dt} = \frac{Q_{\text{irr}} \cdot [\text{Salts}]_{\text{water}}}{h_{\text{root}} \cdot \theta_{\text{field}}} + Q_{\text{capillary}} - \frac{Q_{\text{drain}} \cdot [\text{Salts}]_{\text{leach}}}{h_{\text{root}} \cdot \theta_{\text{field}}} - \gamma_{\text{flush}}(P)$$
  $$Y_{\text{crop}}(t) = Y_0 \cdot \max\left(0.10, \; 1.0 - 0.06 \cdot \max(0.0, [\text{Salts}]_{\text{soil}} - 2.0)\right)$$
* **Parameters & Physical Units**:
  * $[\text{Salts}]_{\text{soil}}$: Soil electrical conductivity / salinity concentration [$\text{dS/m}$].
  * $Q_{\text{irr}}$: Irrigation water inflow depth [$\text{mm/year}$].
  * $h_{\text{root}}$: Active root zone depth [$\text{meters}$].
  * $\theta_{\text{field}}$: Soil moisture content at field capacity [dimensionless].
  * $Q_{\text{drain}}$: Subsurface drainage outflow [$\text{mm/year}$].
  * Maas-Hoffman threshold ($2.0\text{ dS/m}$): Beyond $2.0\text{ dS/m}$, grain yield drops by $6\%$ per $\text{dS/m}$, collapsing wheat and forcing substitution with salt-tolerant barley.
* **Computational Complexity**: $O(1)$ per land cell; executed in Pass 2.

---

### Section D: Biophysics, Demography, Ecology & Genetics

#### 24. Stull Psychrometric Wet-Bulb Lethality (`WetBulbTemperatureEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Stull, R. (2011). *Wet-bulb temperature from relative humidity and air temperature*. Journal of Applied Meteorology and Climatology, 50(11), 2267-2269; Sherwood, S. C., & Huber, M. (2010). *An adaptability limit to climate change due to heat stress*. PNAS, 107(21), 9552-9555.
* **Governing Equations**:
  $$T_{\text{wb}} = T \cdot \text{atan}\left(0.151977 \sqrt{\text{RH} + 8.313659}\right) + \text{atan}(T + \text{RH}) - \text{atan}(\text{RH} - 1.676331) + 0.00391838 \text{RH}^{3/2} \text{atan}(0.023101 \text{RH}) - 4.686035$$
  $$\mu_{\text{hyperthermia}} = \max\left(0.0, \; \frac{T_{\text{wb}} - 31.0}{35.0 - 31.0}\right)^3$$
* **Parameters & Physical Units**:
  * $T$: Dry-bulb ambient air temperature [$^\circ\text{C}$].
  * $\text{RH}$: Relative humidity [$\%$].
  * $T_{\text{wb}}$: Wet-bulb thermodynamic temperature [$^\circ\text{C}$].
  * $\mu_{\text{hyperthermia}}$: Heat stress mortality hazard rate [$\text{year}^{-1}$].
  * Physiological boundary ($T_{\text{wb}} \ge 35^\circ\text{C}$): Human skin cannot dissipate metabolic heat via perspiration; fatal hyperthermia occurs within 6 hours without active refrigeration.
* **Computational Complexity**: $O(1)$ per cell; evaluated on Daily Step ($\Delta t = 1\text{ day}$).

#### 25. Kleiber Allometry, Euler-Lotka Renewal & Gompertz-Makeham Senescence (`DemographicKernel`, `BiologicalDemographicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kleiber, M. (1932). *Body size and metabolism*. Hilgardia, 6(11), 315–353; Gompertz, B. (1825). *On the nature of the function expressive of the law of human mortality*. Philosophical Transactions of the Royal Society, 115, 513–583; Makeham, W. M. (1860). *On the law of mortality*. Journal of the Institute of Actuaries, 8(6), 301–310; Sharpe, F. R., & Lotka, A. J. (1911). *A problem in age-distribution*. Philosophical Magazine, 21(124), 435–438.
* **Governing Equations**:
  $$B_{\text{basal}} = B_0 M_{\text{body}}^{3/4}, \qquad \mu(a) = \alpha_{\text{makeham}} + \beta_{\text{gompertz}} \exp(\gamma_{\text{age}} a)$$
  $$l(a) = \exp\!\left(-\int_0^a \mu(u) \, du\right), \qquad \int_{\alpha}^{\omega} e^{-r a} \, l(a) \, m(a) \, da = 1$$
* **Parameters & Physical Units**:
  * $a$: Cohort age [$\text{yr}$]; $[\alpha, \omega]$: reproductive age window [$\text{yr}$].
  * $\mu(a)$: Age-specific actuarial mortality hazard rate [$\text{yr}^{-1}$], with Makeham background hazard $\alpha_{\text{makeham}}$ and Gompertz senescence parameters $(\beta_{\text{gompertz}}, \gamma_{\text{age}})$.
  * $l(a) \in (0, 1]$: Cohort survival probability from birth to age $a$; $m(a)$: age-specific maternity schedule [$\text{yr}^{-1}$]; $r$: intrinsic Euler-Lotka growth rate [$\text{yr}^{-1}$].
* **Computational Complexity**: $O(7)$ age cohorts per cell.

#### 25.1. Ecotoxicological Endocrine Disruption & Fertility Attenuation (`EcotoxicologyFertilityEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Colborn, T., vom Saal, F. S., & Soto, A. M. (1993). *Developmental effects of endocrine-disrupting chemicals in wildlife and humans*. Environmental Health Perspectives, 101(5), 378-384; Swan, S. H., et al. (2017). *Temporal trends in sperm count: a systematic review and meta-regression analysis*. Human Reproduction Update, 23(6), 646-659.
* **Governing Equations**:
  $$\frac{d C_{\text{tox}}}{dt} = \kappa_{\text{emission}} \cdot \left(\frac{P_{\text{industrial}}}{N}\right) - \lambda_{\text{decay}} C_{\text{tox}}$$
  $$b_{\text{eff}} = b_{\text{base}} \cdot \exp(-\beta_{\text{disruption}} \cdot C_{\text{tox}})$$
* **Parameters & Physical Units**:
  * $C_{\text{tox}}$: Environmental endocrine disruptor concentration [$\mu\text{g/L}$].
  * $\kappa_{\text{emission}}$: Chemical effluent release rate per unit industrial power [$\mu\text{g}/(\text{L}\cdot\text{W}\cdot\text{year)}$].
  * $\lambda_{\text{decay}}$: Environmental persistence breakdown rate [$\text{year}^{-1}$].
  * $b_{\text{eff}}$: Attenuated biological birth rate [$\text{births/(person}\cdot\text{year)}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 25.2. Bergmann-Allen Ecomorphology & Thermoregulatory Scaling (`BergmannAllenMorphologyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Bergmann, C. (1847). *Ueber die Verhältnisse der Wärmeökonomie der Thiere zu ihrer Grösse*. Göttinger Studien, 3(1), 595–708; Allen, J. A. (1877). *The influence of physical conditions in the genesis of species*. Radical Review, 1, 108–140; Du Bois, D., & Du Bois, E. F. (1916). *A formula to estimate the approximate surface area if height and weight be known*. Archives of Internal Medicine, 17(6), 863–871; Scholander, P. F., Hock, R., Walters, V., Johnson, F., & Irving, L. (1950). *Heat regulation in some arctic and tropical mammals and birds*. Biological Bulletin, 99(2), 237–258.
* **Governing Equations**:
  $$\frac{A_s}{M} = c_{\text{geom}} \, M^{-1/3}, \qquad B(T) = \max\!\left(B_{\text{basal}}, \; g_{\text{th}} A_s (T_{\text{core}} - T)\right), \qquad T_{\text{lc}} = T_{\text{core}} - \frac{B_{\text{basal}}}{g_{\text{th}} A_s}$$
* **Parameters & Physical Units**:
  * $A_s / M$: Specific body surface-to-mass ratio [$\text{m}^2/\text{kg}$] scaling geometrically as $M^{-1/3}$ ($c_{\text{geom}} \approx 0.048\,\text{m}^2\cdot\text{kg}^{-2/3}$, Du Bois & Du Bois 1916).
  * $g_{\text{th}}$: Whole-body thermal conductance [$\text{W/(m}^2\cdot\text{K)}$]; $T_{\text{core}}$: homeothermic core temperature [$\text{K}$]; $T_{\text{lc}}$: Scholander-Irving lower critical thermoneutral temperature [$\text{K}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 25.3. Charnov Life-History Invariants & Reproductive Allocation (`LifeHistoryInvariantsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Charnov, E. L. (1993). *Life History Invariants: Some Explorations of Symmetry in Evolutionary Ecology*. Oxford University Press; Stearns, S. C. (1992). *The Evolution of Life Histories*. Oxford University Press; Promislow, D. E. L., & Harvey, P. H. (1990). *Living fast and dying young: A comparative analysis of life-history variation among mammals*. Journal of Zoology, 220(3), 417-437.
* **Governing Equations**:
  $$\mu_{\text{adult}} \cdot \alpha = C_{\text{Charnov}} \approx 1.80 \pm 0.20, \qquad \alpha^*(\mu_{\text{adult}}) = \frac{C_{\text{Charnov}}}{\mu_{\text{adult}}}$$
  $$l_\alpha = \exp(-\mu_{\text{juv}} \cdot \alpha) = \exp(-q_{\text{juv}} \cdot C_{\text{Charnov}}), \qquad b^*(\mu_{\text{adult}}) = \frac{\mu_{\text{adult}}}{l_\alpha}$$
* **Parameters & Physical Units**:
  * $C_{\text{charnov}} = 1.80$: Conserved mammalian/primate life-history invariant [dimensionless].
  * $q_{\text{juv}} = 0.65$: Ratio of juvenile mortality hazard to adult mortality hazard.
  * $\alpha_{\min} = 12.0\text{ years}$, $\alpha_{\max} = 24.0\text{ years}$: Physiological hominin boundaries on reproductive maturity age.
  * $\mu_0 = 0.08\text{ yr}^{-1}$: Baseline intrinsic adult mortality rate.
* **Epistemic Invariant**: Derives dynamic maturity age and fertility acceleration under crisis mortality hazards directly from evolutionary optimization rather than ad-hoc demographic tables.
* **Computational Complexity**: $O(1)$ per cell.

#### 26. Kimura Neutral Genetic Drift & Wright-Fisher Diffusion (`BioMolecularEpidemiologyEngine`, `SelectiveBreedingEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kimura, M. (1968). *Evolutionary rate at the molecular level*. Nature, 217(5129), 624-626; Wright, S. (1931). *Evolution in Mendelian populations*. Genetics, 16(2), 97-159.
* **Governing Equations**:
  $$\frac{\partial \phi(p, t)}{\partial t} = -\frac{\partial}{\partial p}\left[ s p (1 - p) \phi(p, t) \right] + \frac{1}{2} \frac{\partial^2}{\partial p^2}\left[ \frac{p (1 - p)}{2 N_e} \phi(p, t) \right]$$
* **Parameters & Physical Units**:
  * $p \in [0, 1]$: Allele frequency in the local gene pool.
  * $\phi(p, t)$: Probability density of allele frequency $p$ at time $t$.
  * $s$: Selective fitness advantage or disadvantage [dimensionless].
  * $N_e$: Effective genetic population size [individuals].
* **Computational Complexity**: $O(1)$ per allele locus per cell.

#### 27. Radiocarbon $^{14}\text{C}$ & Isotope Fractionation (`RadiocarbonIsotopeEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Libby, W. F. (1952). *Radiocarbon Dating*. University of Chicago Press; Reimer, P. J., et al. (2020). *The IntCal20 Northern Hemisphere radiocarbon age calibration curve (0-55 cal kBP)*. Radiocarbon, 62(4), 725-757.
* **Governing Equations**:
  $$\frac{d N_{14}}{dt} = Q_{\text{cosmic}}(t) - \lambda_{14} N_{14}, \quad \lambda_{14} = \frac{\ln 2}{T_{1/2}} \approx 1.2097 \times 10^{-4}\text{ year}^{-1}$$
  $$\text{Age}_{\text{BP}} = -\frac{1}{\lambda_{14}} \ln\left( \frac{A_{\text{sample}}}{A_0} \right)$$
* **Parameters & Physical Units**:
  * $T_{1/2} = 5730.0\text{ years}$: Radiocarbon $^{14}\text{C}$ physical half-life.
  * $Q_{\text{cosmic}}(t)$: Upper atmospheric cosmic-ray spallation production rate [$\text{atoms/(cm}^2\cdot\text{s)}$].
  * $A_{\text{sample}} / A_0$: Residual $^{14}\text{C} / ^{12}\text{C}$ activity ratio relative to pre-bomb baseline.
* **Computational Complexity**: $O(1)$ per archaeological sample.

#### 28. Spatial Metapopulation SEIR-V Epidemic Diffusion (`SpatialMetapopulationSEIREngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kermack, W. O., & McKendrick, A. G. (1927). *A contribution to the mathematical theory of epidemics*. Proceedings of the Royal Society A, 115(772), 700-721; Colizza, V., & Vespignani, A. (2007). *Invasion threshold in heterogeneous metapopulation networks*. Physical Review Letters, 99(14), 148701.
* **Governing Equations**:
  $$\frac{dS_i}{dt} = -\beta_0 \exp(-\delta_T |T_i - 20|) S_i \frac{I_i}{N_i} - \sum_{j \in \text{Ring1}} M_{ji} S_i + \sum_{j} M_{ij} S_j$$
  $$\frac{dE_i}{dt} = \beta_i S_i \frac{I_i}{N_i} - \sigma_{\text{latent}} E_i, \quad \frac{dI_i}{dt} = \sigma_{\text{latent}} E_i - \gamma_{\text{recov}} I_i - \mu_{\text{lethal}} I_i$$
  $$\frac{dR_i}{dt} = \gamma_{\text{recov}} I_i - \omega_{\text{waning}} R_i$$
* **Parameters & Physical Units**:
  * $S, E, I, R$: Susceptible, Exposed, Infectious, and Recovered demographic counts [persons].
  * $\beta_0$: Pathogen contact transmission rate [$\text{day}^{-1}$].
  * $\sigma_{\text{latent}} = 1 / d_{\text{incubation}}$: Rate of progression from exposed to infectious [$\text{day}^{-1}$].
  * $\gamma_{\text{recov}} = 1 / d_{\text{infectious}}$: Recovery rate [$\text{day}^{-1}$].
  * $\mu_{\text{lethal}}$: Case fatality rate [$\text{day}^{-1}$].
  * $M_{ij}$: Inter-cell human mobility transport matrix [$\text{persons/day}$].
* **Computational Complexity**: $O(K_{\text{active}})$ sparse active set evaluation.

#### 29. Trophic Ecosystem Biomass Thermodynamics (`TrophicEcosystemEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Lindeman, R. L. (1942). *The trophic-dynamic aspect of ecology*. Ecology, 23(4), 399-417; Odum, E. P. (1968). *Energy flow in ecosystems: a historical review*. American Zoologist, 8(1), 11-18.
* **Governing Equations**:
  $$\frac{d B_{\text{herb}}}{dt} = \eta_{\text{trophic}} \cdot G_{\text{veg}}(B_{\text{veg}}) - m_{\text{herb}} B_{\text{herb}} - C_{\text{pred}}(B_{\text{herb}}, B_{\text{carn}}) - H_{\text{human}}(B_{\text{herb}})$$
  $$\frac{d B_{\text{carn}}}{dt} = \eta_{\text{trophic}} \cdot C_{\text{pred}}(B_{\text{herb}}, B_{\text{carn}}) - m_{\text{carn}} B_{\text{carn}}$$
* **Parameters & Physical Units**:
  * $B_{\text{veg}}, B_{\text{herb}}, B_{\text{carn}}$: Biomass densities of primary producers, primary consumers, and secondary carnivores [$\text{kg/m}^2$].
  * $\eta_{\text{trophic}} \approx 0.10$: Lindeman trophic transfer efficiency ($10\%$ ecological law).
  * $m$: Metabolic respiration and senescence mortality rate [$\text{year}^{-1}$].
  * $H_{\text{human}}$: Human hunting and foraging extraction rate [$\text{kg/(m}^2\cdot\text{year)}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 30. Quaternary Megafauna Predation Dynamics (`MegafaunaEcosystemEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Sandom, C., et al. (2014). *Global late Quaternary megafauna extinctions linked to humans, not climate change*. Proceedings of the Royal Society B, 281(1787), 20133254; Barnosky, A. D., et al. (2004). *Assessing the Causes of Late Pleistocene Extinctions on the Continents*. Science, 306(5693), 70-75.
* **Governing Equations**:
  $$\frac{d M}{dt} = r_m \cdot M \cdot \left(1 - \frac{M}{K_{\text{veg}}}\right) - q_{\text{hunt}} \cdot N_{\text{hunter}} \cdot \frac{M}{M + M_{1/2}}$$
* **Parameters & Physical Units**:
  * $M$: Megafaunal herbivore biomass [$\text{metric tons}$].
  * $r_m = 0.05\text{ year}^{-1}$: Intrinsic slow intrinsic reproduction rate of large mammals ($>44\text{ kg}$).
  * $K_{\text{veg}}$: Carrying capacity dictated by Farquhar net primary productivity [$\text{metric tons}$].
  * $q_{\text{hunt}}$: Human hunting efficiency parameter [$\text{tons/(hunter}\cdot\text{year)}$].
  * $M_{1/2}$: Half-saturation constant of megafauna density for hunting encounters [$\text{metric tons}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 31. Selective Breeding & Agronomic Selection (`SelectiveBreedingEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Lush, J. L. (1937). *Animal Breeding Plans*. Iowa State College Press; Falconer, D. S. (1960). *Introduction to Quantitative Genetics*. Oliver & Boyd.
* **Governing Equations**:
  $$\Delta \bar{z} = h^2 \cdot S_{\text{selection}} = h^2 \cdot (\bar{z}_{\text{selected}} - \bar{z}_{\text{population}})$$
  $$Y_{\text{crop}}(t+\Delta t) = Y_{\text{crop}}(t) + \Delta \bar{z}_{\text{yield}} \cdot \Delta t$$
* **Parameters & Physical Units**:
  * $\bar{z}$: Mean phenotypic quantitative trait value (grain size, seed retention, lactation yield).
  * $h^2 = V_A / V_P$: Narrow-sense heritability coefficient [dimensionless, $0.2 - 0.5$].
  * $S_{\text{selection}}$: Selection differential imposed by artificial human breeding [trait units].
* **Computational Complexity**: $O(1)$ per domesticated crop/livestock clade.

#### 31.1. Multi-Trophic Biomass Dynamics & Ecological Degradation (`EcologicalDegradationEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: May, R. M. (1973). *Stability and Complexity in Model Ecosystems*. Princeton University Press.
* **Governing Equations**:
  $$\frac{d B_i}{dt} = r_i B_i \left(1 - \frac{B_i}{K_i}\right) - \sum_{j \ne i} \alpha_{ij} B_i B_j - \Delta B_{\text{human}}$$
* **Parameters & Physical Units**:
  * $B_i$: Biomass density of species/functional guild $i$ [$\text{kg/ha}$].
  * $\alpha_{ij}$: Inter-species competition or predation matrix coefficient.
  * $\Delta B_{\text{human}}$: Biomass removal via logging, clearing, and grazing.
* **Computational Complexity**: $O(G^2)$ for $G$ functional ecological guilds.

#### 31.2. Critical Slowing Down & Saddle-Node Tipping Points (`CharneyVegetationAlbedoEngine`, `BifurcationChaosEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Scheffer, M., et al. (2009). *Early-warning signals for critical transitions*. Nature, 461(7260), 53-59; Kramers, H. A. (1940). *Brownian motion in a field of force and the diffusion model of chemical reactions*. Physica, 7(4), 284-304.
* **Governing Equations**:
  $$\frac{dx}{dt} = -\frac{dV(x)}{dx} + \sigma_{\text{noise}} \, dW_t = \mu_{\text{bifurc}} - x^2 + \sigma_{\text{noise}} \, dW_t$$
  $$\text{ACF}(1) = \exp(\lambda \Delta t) \to 1.0, \quad \text{Var}(x) = \frac{\sigma_{\text{noise}}^2}{2 |\lambda|} \to +\infty \quad \text{as } \mu_{\text{bifurc}} \to 0^+$$
* **Parameters & Physical Units**:
  * $x$: System ecological or demographic state coordinate.
  * $\mu_{\text{bifurc}}$: Bifurcation control parameter distance to saddle-node fold.
  * $\lambda = -2\sqrt{\mu_{\text{bifurc}}}$: Restorative eigenvalue [$\text{year}^{-1}$].
  * $\text{ACF}(1)$: Lag-1 temporal autocorrelation [dimensionless, $0.0 - 1.0$].
* **Computational Complexity**: $O(1)$ per monitored ecosystem/societal buffer.

#### 31.3. Standard Dynamic Energy Budget (DEB) Thermodynamics (`DynamicEnergyBudgetEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kooijman, S. A. L. M. (2010). *Dynamic Energy Budget theory for metabolic organisation*. Cambridge University Press; Jager, T., Martin, B. T., & Baveco, J. M. (2013). *Alternative angels. Predicting rethink dynamic energy budget theory*. Journal of Theoretical Biology, 328, 85-93; Nisbet, R. M., et al. (2000). *From molecules to ecosystems through dynamic energy budget models*. Journal of Animal Ecology, 69(6), 913-926; Hatton, I. A., et al. (2015). *The predator-prey power law: Biomass scaling across terrestrial and aquatic biomes*. Science, 349(6252), aac6284.
* **Governing Equations**:
  $$\frac{dV}{dt} = \frac{\kappa \dot{p}_C - [\dot{p}_M] V}{[E_G]}, \qquad \frac{dE}{dt} = \dot{p}_A - \dot{p}_C$$
  $$k(T) = \exp\left(\frac{T_A}{T_{\text{ref}}} - \frac{T_A}{T}\right), \qquad f(X) = \frac{X}{X + X_K}$$
  $$\dot{p}_A = \{\dot{p}_{Am}\} \cdot f(X) \cdot k(T), \qquad \dot{p}_M = [\dot{p}_M] \cdot V$$
  $$\dot{p}_R = \max(0.0, \; (1 - \kappa) \dot{p}_C - [\dot{p}_J]), \qquad \mu_{\text{starv}} = \mu_{\text{max}} \cdot \left(\frac{\max(0, \Delta V / V_{\text{healthy}})}{w_{\text{crit}}}\right)^2$$
  $$\rho_{\text{herb}} = c_{\text{Hatton}} \cdot \rho_{\text{prod}}^{3/4} \quad (c_{\text{Hatton}} = 0.056\;(\text{t/km}^2)^{1/4})$$
* **Parameters & Physical Units**:
  * $V$: Structural functional tissue biomass [metric tons per cell or kg/capita].
  * $E$: Mobilizable biochemical reserve energy pool [$\text{GJ}$].
  * $\kappa = 0.80$: Somatic allocation fraction (80% soma: maintenance + growth; 20% reproduction/maturity).
  * $[\dot{p}_M]$: Somatic maintenance rate [$\text{GJ/(person}\cdot\text{year)}$ or $\text{GJ/(ton}\cdot\text{year)}$].
  * $[E_G] = 28.0\text{ GJ/ton}$: Energy required to synthesize a unit of structural mass.
  * $w_{\text{crit}} = 0.40$: Critical relative structural loss threshold triggering lethal emaciation.
  * $T_A = 8000.0\text{ K}$, $T_{\text{ref}} = 293.15\text{ K}$: Arrhenius activation constants.
* **Epistemic Invariant**: Replaces arbitrary carrying capacity ceilings ($K$) and discrete starvation step-cliffs with continuous $C^1$ energetic balance and endogenous gestation fecundity.
* **Computational Complexity**: $O(1)$ per cell.

#### 31.4. Universal Metabolic Theory of Ecology (MTE) Kinetics (`MetabolicEcologyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Brown, J. H., Gillooly, J. F., Allen, A. P., Savage, V. M., & West, G. B. (2004). *Toward a metabolic theory of ecology*. Ecology, 85(7), 1771-1789; Gillooly, J. F., et al. (2001). *Effects of size and temperature on metabolic rate*. Science, 293(5538), 2248-2251.
* **Governing Equations**:
  $$B(M, T) = B_0 \cdot M^{3/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)$$
  $$r_{\text{max}}(M, T) = r_0 \cdot M^{-1/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right), \qquad t_{\text{gen}}(M, T) = t_0 \cdot M^{1/4} \cdot \exp\left(\frac{E_a}{k_B T}\right)$$
* **Parameters & Physical Units**:
  * $M$: Individual/taxon body mass [$\text{kg}$].
  * $T$: Operating body or ambient temperature [$\text{Kelvin}$].
  * $k_B = 8.617333 \times 10^{-5}\text{ eV/K}$: Boltzmann thermodynamic constant.
  * $E_a = 0.65\text{ eV}$: Universal activation energy for aerobic respiratory enzyme kinetics.
  * $B$: Basal metabolic power dissipation [$\text{Watts}$].
  * $r_{\text{max}}$: Maximum intrinsic demographic reproduction rate [$\text{year}^{-1}$].
  * $t_{\text{gen}}$: Characteristic biological generation time [$\text{years}$].
* **Epistemic Invariant**: Validated across thousands of non-human clades (mammals, reptiles, fish, birds, invertebrates, vascular plants).
* **Computational Complexity**: $O(1)$ per taxon guild per cell.

#### 31.5. Ecological Stoichiometry & Element Allocation (`EcologicalStoichiometryEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Sterner, R. W., & Elser, J. J. (2002). *Ecological Stoichiometry: The Biology of Elements from Molecules to the Biosphere*. Princeton University Press; Redfield, A. C. (1958). *The biological control of chemical factors in the environment*. American Scientist, 46(3), 205-221; Loladze, I. (2002, 2014). *Hidden shift of the ionome of plants with rising atmospheric CO2*. eLife, 3, e02245.
* **Governing Equations**:
  $$\text{C} : \text{N} : \text{P}_{\text{molar}} = 106 : 16 : 1 \implies \frac{M_{\text{C}}}{M_{\text{P}}} \approx 41.1, \quad \frac{M_{\text{N}}}{M_{\text{P}}} \approx 7.23 \text{ (by mass)}$$
  $$L_{\text{Liebig}}(\text{N}, \text{P}, \text{K}) = \min\!\left(1, \; \frac{[\text{N}]}{[\text{N}]_{\text{opt}}}, \; \frac{[\text{P}]}{[\text{P}]_{\text{opt}}}, \; \frac{[\text{K}]}{[\text{K}]_{\text{opt}}}\right)$$
  $$\frac{[\text{N}]_{\text{plant}}(C_a)}{[\text{N}]_{\text{plant}}(C_0)} = \left(\frac{C_a}{C_0}\right)^{-\beta_{\text{ionome}}}$$
* **Parameters & Physical Units**:
  * $L_{\text{Liebig}} \in [0, 1]$: Liebig law of the minimum multi-elemental yield limitation factor.
  * $\beta_{\text{ionome}} > 0$: Elevated-$\text{CO}_2$ plant mineral and protein stoichiometric dilution exponent (Loladze 2002, 2014).
* **Epistemic Invariant**: Mass-balance conservation of chemical element ratios without ad-hoc single-variable cliffs.
* **Computational Complexity**: $O(1)$ per cell.

#### 31.6. Constructal Law of Flow Architecture & Locomotion Allometry (`ConstructalFlowArchitectureEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Bejan, A. (1996). *Street network theory of organization in nature*. Journal of Advanced Transportation, 30(2), 85-107; Bejan, A., & Marden, J. H. (2006). *Unifying constructal theory for scale effects in running, swimming and flying*. Journal of Experimental Biology, 209(2), 238-248; Bejan, A. (2016). *The Physics of Life*. St. Martin's Press.
* **Governing Equations**:
  $$V_{\text{opt}}(M) = V_0 \cdot M_{\text{body}}^{1/6}, \qquad f_{\text{stride}}(M) = f_0 \cdot M_{\text{body}}^{-1/6}$$
  $$d_{\text{parent}}^3 = \sum_{i=1}^n d_{\text{daughter}, i}^3 \quad (\text{Murray's Law}), \qquad L_{\text{stream}} = c_{\text{hack}} \cdot A_{\text{basin}}^{0.57} \quad (\text{Hack's Law})$$
* **Parameters & Physical Units**:
  * $V_{0,\text{run}} = 3.85\text{ m/s}\cdot\text{kg}^{-1/6}$, $V_{0,\text{fly}} = 11.80\text{ m/s}\cdot\text{kg}^{-1/6}$, $V_{0,\text{swim}} = 1.25\text{ m/s}\cdot\text{kg}^{-1/6}$.
  * $f_0 = 2.50\text{ Hz}\cdot\text{kg}^{1/6}$: Basal stride cycling frequency.
  * $c_{\text{hack}} = 1.40\text{ km}^{-0.14}$: Catchment drainage length coefficient.
* **Epistemic Invariant**: Universal thermodynamic derivation of animal migration speed, river channel geometry, and vascular minimization without empirical pathfinding shortcuts.
* **Computational Complexity**: $O(1)$ per cell.

#### 31.7. Unified Neutral Theory of Biodiversity & Biogeography (UNTB) (`UnifiedNeutralBiodiversityEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Hubbell, S. P. (2001). *The Unified Neutral Theory of Biodiversity and Biogeography*. Princeton University Press; Volkov, I., et al. (2003). *Neutral theory and relative species abundance in ecology*. Nature, 424(6952), 1035-1037.
* **Governing Equations**:
  $$\theta = 2 J_M \nu, \qquad S(A) = c \cdot A^z \quad (z \approx 0.25)$$
  $$S = \theta \ln\left(1 + \frac{J_M}{\theta}\right), \qquad \mathcal{B}_{\text{retention}} = \left(\frac{B_{\text{remaining}}}{B_{\text{pristine}}}\right)^z$$
* **Parameters & Physical Units**:
  * $\theta$: Fundamental biodiversity number [dimensionless].
  * $J_M$: Metacommunity carrying capacity biomass [tons].
  * $\nu = 1.0 \times 10^{-6}$: Baseline speciation / evolutionary innovation rate.
  * $z = 0.25$: Universal Preston-Hubbell continental species-area exponent.
* **Epistemic Invariant**: Replaces arbitrary scalar biodiversity indices with an analytical zero-sum neutral drift and defaunation debt formulation.
* **Computational Complexity**: $O(1)$ per cell.

#### 31.8. Maximum Entropy Production Principle (MEPP) & Planetary Climate Engine (`MaximumEntropyProductionEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kleidon, A. (2010). *Life, hierarchy, and the thermodynamic machinery of planet Earth*. Physics of Life Reviews, 7(4), 424-460; Kleidon, A. (2016). *Thermodynamic Foundations of the Earth System*. Cambridge University Press; Paltridge, G. W. (1975). *Q. J. R. Meteorol. Soc.*
* **Governing Equations**:
  $$I_{\text{surf}}(\phi) = S_0 (1 - \alpha_{\text{p}}) \cdot \max(0.08, \cos\phi) \;[\text{W/m}^2], \qquad \eta_{\text{carnot}} = \frac{T_s - T_e}{T_s} \approx 0.115, \qquad G_{\text{ke}} = \frac{1}{4} \eta_{\text{carnot}} \cdot I_{\text{net}} \approx 2.5\text{--}3.5\text{ W/m}^2$$
  $$P_{\text{precip}} = E = \left(\frac{\Delta}{\Delta + \gamma}\right) \frac{R_n}{\lambda_v} \cdot 3.15576 \times 10^7\text{ s/yr}$$
* **Parameters & Physical Units**:
  * $I_{\text{surf}}(\phi)$: Incident surface shortwave solar irradiance [$\text{W/m}^2$].
  * $T_e = 255.0\text{ K}$: Planetary radiative emission temperature.
  * $\lambda_v = 2.45 \times 10^6\text{ J/kg}$: Latent heat of vaporization of water.
  * $\gamma = 0.66\text{ hPa/K}$: Psychrometric constant.
  * $P_{\text{wind,max}} = 1.25\text{ W/m}^2$: Physical Betz-Kleidon upper limit on renewable wind kinetic extraction.
* **Epistemic Invariant**: Derives global precipitation, atmospheric work, and kinetic dissipation directly from solar thermodynamics without heavy 3D fluid solvers.
* **Computational Complexity**: $O(1)$ per cell.

---

### Section E: Energetics, Thermodynamics & Material Infrastructure

#### 32. Entropic Metal Corrosion & Material Dissipation (`EntropicMetalDissipationEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Fontana, M. G. (1986). *Corrosion Engineering*. McGraw-Hill; Ashby, M. F. (2012). *Materials and the Environment: Eco-Informed Material Choice*. Butterworth-Heinemann.
* **Governing Equations**:
  $$\frac{d M_{\text{metal}}}{dt} = -k_{\text{corrosion}}(\text{Salinity}, \text{Humidity}) \cdot M_{\text{metal}} - \delta_{\text{abrasion}} M_{\text{metal}}$$
  $$k_{\text{corrosion}} = k_0 \cdot \exp\left( -\frac{E_a}{R T} \right) \cdot (1.0 + 3.0 f_{\text{salinity}}) \cdot f_{\text{humidity}}$$
* **Parameters & Physical Units**:
  * $M_{\text{metal}}$: Physical stock of structural iron, bronze, or copper tools [$\text{kg}$].
  * $k_{\text{corrosion}}$: Arrhenius chemical corrosion degradation rate [$\text{year}^{-1}$].
  * $E_a = 45.0\text{ kJ/mol}$: Activation energy for oxidative rust kinetics.
  * $\delta_{\text{abrasion}}$: Mechanical wear rate during agricultural and mining labor [$\text{year}^{-1}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 33. Ore Grade Exergy Degradation & Extraction Work (`OreGradeThermodynamicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Chapman, P. F., & Roberts, F. (1983). *Metal Resources and Energy*. Butterworths; Bardi, U. (2014). *The Extractive Zone: Physical Limits to Resource Depletion*. Springer.
* **Governing Equations**:
  $$W_{\text{extraction}} = \frac{W_{\text{ideal}}}{g_{\text{ore}}} = \frac{-\Delta G_{\text{reduction}}}{g_0 \cdot \exp(-\kappa_{\text{deplete}} \cdot C_{\text{cumul}})}$$
* **Parameters & Physical Units**:
  * $W_{\text{extraction}}$: Useful thermodynamic exergy required to extract 1 kg of pure metal [$\text{MJ/kg}$].
  * $g_{\text{ore}}$: Mass fraction of pure metal in raw mined ore [dimensionless, e.g. $0.60$ for high-grade hematite down to $0.005$ for degraded copper porphyry].
  * $\Delta G_{\text{reduction}}$: Gibbs free energy of chemical oxide reduction [$\text{MJ/kg}$].
  * $C_{\text{cumul}}$: Cumulative historical tonnage extracted from the geological deposit [$\text{metric tons}$].
* **Computational Complexity**: $O(1)$ per extraction site.

#### 34. Net Energy EROEI & Physical Surpluses (`NetEnergyEROEIEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Hall, C. A. S., Cleveland, C. J., & Kaufmann, R. (1986). *Energy and Resource Quality: The Ecology of the Economic Process*. Wiley; Murphy, D. J., & Hall, C. A. S. (2010). *Year in review - EROI or energy return on (energy) invested*. Annals of the New York Academy of Sciences, 1185(1), 102-118.
* **Governing Equations**:
  $$\text{EROEI} = \frac{E_{\text{gross}}}{E_{\text{invested}}}, \quad E_{\text{net}} = E_{\text{gross}} \cdot \left(1 - \frac{1}{\text{EROEI}}\right)$$
* **Parameters & Physical Units**:
  * $E_{\text{gross}}$: Total physical exergy harvested from primary energy source [$\text{Joules}$].
  * $E_{\text{invested}}$: Auxiliary energy consumed in exploration, extraction, refining, and delivery [$\text{Joules}$].
  * $E_{\text{net}}$: Net exergy available to society for non-extractive services (food, housing, education) [$\text{Joules}$].
  * EROEI cliff: As $\text{EROEI} \to 1.0$, net energy drops to zero, triggering immediate systemic collapse regardless of financial prices.
* **Computational Complexity**: $O(1)$ per energy source.

#### 35. Physical Transport Work & Friction Drag (`PhysicsTransportEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Minetti, A. E., et al. (2002). *Energy cost of walking and running on extreme slopes*. Journal of Applied Physiology, 93(3), 1039-1046; Allen, T., & Arkolakis, C. (2014). *Trade and the Topography of the Spatial Economy*. Quarterly Journal of Economics, 129(3), 1085-1140.
* **Governing Equations**:
  $$W_{\text{transport}} = M_{\text{cargo}} \cdot d_{ij} \cdot \left[ \mu_{\text{friction}}(\text{Mode}) \cdot g + g \cdot \max(0, \nabla z) \right]$$
  $$C_{\text{Minetti}}(\text{slope}) = 280.5 \, s^5 - 58.7 \, s^4 - 26.8 \, s^3 + 4.19 \, s^2 + 0.38 \, s + 0.155 \quad [\text{J/(kg}\cdot\text{m)}]$$
* **Parameters & Physical Units**:
  * $W_{\text{transport}}$: Mechanical work consumed moving cargo [$\text{Joules}$].
  * $M_{\text{cargo}}$: Transported cargo mass [$\text{kg}$].
  * $d_{ij}$: Geodesic distance along path [$\text{meters}$].
  * $\mu_{\text{friction}}$: Rolling/sliding friction coefficient:
    * Pack porter pedestrian: $\mu \approx 0.20$
    * Pack animal (mule/camel): $\mu \approx 0.12$
    * Paved Roman road wagon: $\mu \approx 0.05$
    * Fluvial downstream barge: $\mu \approx 0.008$
    * Ocean sailing ship: $\mu \approx 0.002$
    * Steel rail train: $\mu \approx 0.001$
* **Computational Complexity**: $O(1)$ per transport link.

#### 35.1. Transport Infrastructure & Iceberg Trade-Cost Attenuation (`InfrastructureEnergyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Samuelson, P. A. (1954). *The transfer problem and transport costs, II*. Economic Journal, 64(254), 264–289; Allen, T., & Arkolakis, C. (2014). *Trade and the Topography of the Spatial Economy*. Quarterly Journal of Economics, 129(3), 1085–1140; Donaldson, D. (2018). *Railroads of the Raj: Estimating the impact of transportation infrastructure*. American Economic Review, 108(4), 899–934.
* **Governing Equations**:
  $$\tau_{ij} = \exp\!\left(\int_{\gamma_{ij}} \mu(\mathbf{x}) \, ds\right), \qquad \mu(\mathbf{x}) = \mu_0(\nabla z, T, B_{\text{veg}}) \cdot \left(1 + \frac{K_{\text{infra}}(\mathbf{x})}{A(\mathbf{x}) \cdot k_0}\right)^{-\zeta}$$
* **Parameters & Physical Units**:
  * $\tau_{ij} \ge 1$: Samuelson iceberg trade-cost factor between origin $i$ and destination $j$ [dimensionless].
  * $\mu(\mathbf{x})$: Instantaneous spatial transit resistance per unit distance [$\text{km}^{-1}$].
  * $K_{\text{infra}} / A$: Physical transport infrastructure density (roads, bridges, canals, rails) [$\text{kg/km}^2$].
  * $\zeta > 0$: Transport infrastructure trade-cost elasticity (Donaldson 2018).
* **Computational Complexity**: $O(1)$ per cell.

#### 36. Pyrometallurgical Reduction Enthalpy (`MetallurgyEnthalpyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kubaschewski, O., Alcock, C. B., & Spencer, P. J. (1993). *Materials Thermochemistry*. Pergamon Press; Malandra, W. W. (1990). *Archaeometallurgy in Global Context*. Academic Press.
* **Governing Equations**:
  $$\Delta H_{\text{smelt}} = \Delta H^\circ_f(\text{metal}) + \Delta H^\circ_f(\text{gas}) - \Delta H^\circ_f(\text{ore}) + \int_{T_{\text{ambient}}}^{T_{\text{melt}}} C_p \, dT + L_{\text{fusion}}$$
  $$M_{\text{smelted}} = \min\left( M_{\text{ore}}, \; \frac{P_{\text{thermal}} \cdot \Delta t}{\Delta H_{\text{smelt}}} \cdot \eta_{\text{furnace}}(k_{\text{pc}}) \right)$$
* **Parameters & Physical Units**:
  * $\Delta H_{\text{smelt}}$: Total enthalpy required per kg of metal produced [$\text{J/kg}$] ($6.8\text{ MJ/kg}$ for iron bloomery, $4.2\text{ MJ/kg}$ for copper).
  * $P_{\text{thermal}}$: High-temperature thermal energy flux from charcoal/coal combustion [$\text{Watts}$].
  * $\eta_{\text{furnace}}$: Thermal containment efficiency of refractory furnace masonry [dimensionless, $0.05 - 0.70$].
  * $M_{\text{smelted}}$: Produced elemental metal ingot mass [$\text{kg}$].
* **Computational Complexity**: $O(1)$ per pyrometallurgical site.

#### 37. Physical Leontief Input-Output Mass Balance (`PhysicalLeontiefInputOutputEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Leontief, W. (1951). *The Structure of American Economy, 1919-1939*. Oxford University Press; Duchin, F., & Levine, S. H. (2010). *Embodied resource accounting in a physical input-output framework*. Journal of Industrial Ecology.
* **Governing Equations**:
  $$\mathbf{x} = (\mathbf{I} - \mathbf{A})^{-1} \mathbf{y}$$
  $$\sum_j A_{ij} x_j + y_i = x_i \quad \forall i \in \{\text{Biomass, Ore, Timber, Energy, Machinery}\}$$
* **Parameters & Physical Units**:
  * $\mathbf{x}$: Gross physical output vector [$\text{kg/year}$ or $\text{GJ/year}$].
  * $\mathbf{y}$: Final societal net consumption vector [$\text{kg/year}$].
  * $\mathbf{A}$: Inter-industry physical input requirements matrix [dimensionless or physical conversion ratios].
* **Computational Complexity**: $O(M^3)$ matrix inversion for $M$ economic sectors per regional market.

#### 38. Physical Energy Grid Power Flow & Joule Dissipation (`PhysicalEnergyGridEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Grainger, J. J., & Stevenson, W. D. (1994). *Power System Analysis*. McGraw-Hill.
* **Governing Equations**:
  $$P_{\text{loss}} = I^2 R = \frac{P_{\text{trans}}^2 R_{\text{line}}}{V_{\text{grid}}^2 \cos^2\theta}, \quad \nabla \cdot \mathbf{J} = 0$$
* **Parameters & Physical Units**:
  * $P_{\text{loss}}$: Joule heat dissipation loss along transmission line [$\text{Watts}$].
  * $P_{\text{trans}}$: Transmitted active electrical power [$\text{Watts}$].
  * $R_{\text{line}} = \rho_{\text{copper}} \cdot \frac{L_{\text{wire}}}{A_{\text{wire}}}$: Conductor ohmic electrical resistance [$\Omega$].
  * $V_{\text{grid}}$: Grid alternating voltage [$\text{Volts}$].
* **Computational Complexity**: $O(\text{grid lines})$ per electrical region.

#### 39. Renewable Energy Thermodynamic Limits (`RenewableEnergyPhysicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Betz, A. (1926). *Wind-Energie und ihre Ausnutzung durch Windmühlen*. Vandenhoeck & Ruprecht; Shockley, W., & Queisser, H. J. (1961). *Detailed balance limit of efficiency of p-n junction solar cells*. Journal of Applied Physics, 32(3), 510–519; Kleidon, A. (2010). *Life, hierarchy, and the thermodynamic machinery of planet Earth*. Physics of Life Reviews, 7(4), 424–460.
* **Governing Equations**:
  $$P_{\text{Betz}} = \frac{16}{27} \cdot \frac{1}{2} \rho_{\text{air}} A_{\text{rotor}} v_{\text{wind}}^3, \qquad P_{\text{wind}} \le G_{\text{ke,max}} \cdot A_{\text{surface}} \quad [\text{W}]$$
  $$P_{\text{PV}} = \eta_{\text{SQ}}(E_g) \cdot I_{\text{surf}}(\phi) \cdot A_{\text{PV}}, \qquad \eta_{\text{SQ}}(1.34\,\text{eV}) \le 0.337 \quad [\text{W}]$$
* **Parameters & Physical Units**:
  * $C_{p,\max} = 16/27 \approx 0.5926$: Betz aerodynamic power extraction limit for an open-flow actuator disk.
  * $G_{\text{ke,max}} \approx 1.25\,\text{W/m}^2$: Kleidon atmospheric kinetic-energy generation limit per unit surface area.
  * $\eta_{\text{SQ}}(E_g)$: Shockley-Queisser radiative detailed-balance limit for a single-junction semiconductor of bandgap $E_g$.
  * $I_{\text{surf}}(\phi)$: Incident surface solar irradiance at latitude $\phi$ [$\text{W/m}^2$].
* **Computational Complexity**: $O(1)$ per cell.

#### 40. Urban Thermodynamics & Heat Island (`UrbanThermodynamicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Oke, T. R. (1982). *The energetic basis of the urban heat island*. Quarterly Journal of the Royal Meteorological Society, 108(455), 1-24.
* **Governing Equations**:
  $$\Delta T_{\text{UHI}} = \frac{Q_{\text{waste\_heat}} + R_{\text{stored}}}{\rho_{\text{air}} C_p h_{\text{canopy}}} \cdot \tau_{\text{ventilation}}$$
* **Parameters & Physical Units**:
  * $\Delta T_{\text{UHI}}$: Urban Heat Island temperature anomaly above rural surrounding [$^\circ\text{C}$].
  * $Q_{\text{waste\_heat}}$: Anthropogenic power dissipation from machines, heating, and metabolism [$\text{W/m}^2$].
  * $h_{\text{canopy}}$: Urban architectural canyon height [$\text{meters}$].
  * $\tau_{\text{ventilation}}$: Aerodynamic wind flushing time constant [$\text{seconds}$].
* **Computational Complexity**: $O(1)$ per urban cell.

#### 41. Planetary Boundaries Earth System Matrix (`PlanetaryBoundariesEvaluator`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Rockström, J., et al. (2009). *Planetary boundaries: exploring the safe operating space for humanity*. Ecology and Society, 14(2); Steffen, W., et al. (2015). *Planetary boundaries: Guiding human development on a changing planet*. Science, 347(6223), 1259855.
* **Governing Equations**:
  $$I_{\text{planetary\_stress}} = \sum_{k=1}^9 w_k \left( \frac{\max(0, X_k - X_{k, \text{safe}})}{X_{k, \text{danger}} - X_{k, \text{safe}}} \right)^2$$
* **Parameters & Physical Units**:
  * $X_k$: State value of planetary boundary $k$ (Atmospheric $\text{CO}_2$, Ocean acidification $\Omega_{\text{arag}}$, Stratospheric $\text{O}_3$, Freshwater consumption, Biogeochemical N/P flows, Land use change, Aerosol loading, Chemical novel entities, Biosphere integrity).
  * $X_{k, \text{safe}}, X_{k, \text{danger}}$: Quantified scientific Holocene boundary and high-risk threshold.
* **Computational Complexity**: $O(1)$ globally.

#### 42. Radiotoxicity & Nuclear Fallout Dispersion (`NuclearSafetyRadiotoxicityEngine`, `NuclearWarfareClimateEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Robock, A., et al. (2007). *Climatic consequences of regional nuclear conflicts*. Atmospheric Chemistry and Physics, 7(8), 2003-2012; Glasstone, S., & Dolan, P. J. (1977). *The Effects of Nuclear Weapons*. US Department of Defense.
* **Governing Equations**:
  $$A_{\text{rad}}(t) = A_0 \sum_i f_i \exp(-\lambda_i t), \quad \Delta T_{\text{nuclear\_winter}} = -T_{\text{drop}} \cdot \left(1.0 - \exp(-\kappa_{\text{soot}} M_{\text{soot}})\right)$$
* **Parameters & Physical Units**:
  * $A_{\text{rad}}$: Fission byproduct radioactivity inventory [$\text{Becquerels (Bq)}$].
  * $\lambda_i$: Decay constant for isotope $i$ ($^{131}\text{I}$, $^{137}\text{Cs}$, $^{90}\text{Sr}$).
  * $M_{\text{soot}}$: Stratospheric black carbon soot injection mass [$\text{Tg}$].
  * $\Delta T_{\text{nuclear\_winter}}$: Global surface cooling anomaly [$^\circ\text{C}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 42.1. Photosynthetic Quantum Yield & Superconducting Conduction Bounds (`PhysicalLawEngine`)
* **Model Classification**: Tier 1 (Counterfactual Physical Parameter Perturbation)
* **Academic Literature Reference**: Zhu, X.-G., Long, S. P., & Ort, D. R. (2008). *What is the maximum efficiency with which photosynthesis can convert solar energy into biomass?* Current Opinion in Biotechnology, 19(2), 153–159; Bardeen, J., Cooper, L. N., & Schrieffer, J. R. (1957). *Theory of superconductivity*. Physical Review, 108(5), 1175–1204.
* **Governing Equations**:
  $$\epsilon_{\text{photo}} = \beta_{\text{PAR}} \cdot \alpha_{\text{abs}} \cdot \phi_{\text{quantum}} \cdot (1 - r_{\text{photoresp}} - r_{\text{resp}}), \qquad R_{\text{cond}}(T) = \begin{cases} 0 & T < T_c \\ \rho(T) \frac{L}{A} & T \ge T_c \end{cases}$$
* **Parameters & Physical Units**:
  * $\epsilon_{\text{photo}}$: Theoretical solar-to-biomass conversion ceiling ($\le 0.046$ for $\text{C}_3$ and $\le 0.060$ for $\text{C}_4$ canopies; Zhu et al. 2008).
  * $T_c$: Critical superconducting phase-transition temperature [$\text{K}$] below which ohmic Joule dissipation $P_{\text{loss}} = I^2 R_{\text{cond}} = 0$ (Bardeen et al. 1957).
* **Computational Complexity**: $O(1)$ per cell (exact identity no-op unless counterfactual physical constants are perturbed).

#### 42.2. Thermodynamic Prime Movers & Useful Exergy Conversion (`ThermodynamicPrimeMoversEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Carnot, S. (1824). *Réflexions sur la puissance motrice du feu*. Bachelier; Weisbach, J. (1845). *Lehrbuch der Ingenieur- und Maschinen-Mechanik*. Vieweg; Ayres, R. U., & Warr, B. (2009). *The Economic Growth Engine*. Edward Elgar; Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Governing Equations**:
  $$\eta_{\text{Carnot}} = 1 - \frac{T_C}{T_H}, \qquad \eta_{\text{th}} = \varepsilon_{\text{II}} \left(1 - \frac{T_C}{T_H}\right), \qquad \dot{W}_{\text{shaft}} = \eta_{\text{th}} \, \dot{Q}_{\text{fuel}}$$
  $$P_{\text{hydro}} = \eta_w \, \rho_w \, g \, Q \, H, \qquad P_{\text{muscle}} = \eta_{\text{muscle}} \, \dot{E}_{\text{metabolic}}$$
  $$\frac{dM_{\text{fuel}}}{dt} = -\frac{\dot{Q}_{\text{fuel}}}{\text{LHV}_{\text{fuel}}} = -\frac{\dot{W}_{\text{shaft}}}{\eta_{\text{th}} \cdot \text{LHV}_{\text{fuel}}}$$
* **Parameters & Physical Units**:
  * $T_C, T_H$: Absolute cold-sink ambient temperature and hot-reservoir combustion/working-fluid temperature [$\text{K}$].
  * $\varepsilon_{\text{II}} \in (0, 1)$: Second-Law exergetic effectiveness of the heat engine cycle (Ayres & Warr 2009).
  * $\rho_w = 1\,000\,\text{kg/m}^3$, $g = 9.80665\,\text{m/s}^2$, $Q$ [$\text{m}^3\text{/s}$], $H$ [$\text{m}$], $\eta_w \in [0.20, 0.85]$: Weisbach hydraulic wheel/turbine parameters.
  * $\text{LHV}_{\text{fuel}}$: Specific lower heating value ($15\,\text{GJ/t}$ dry wood, $24\,\text{GJ/t}$ bituminous coal, $42\,\text{GJ/t}$ crude oil, $50\,\text{GJ/t}$ natural gas; Smil 2017).
* **Computational Complexity**: $O(1)$ per cell.

---
## 4. Tier 2  -  Optional Cliodynamic & Phenomenological Engines (One-by-One)

### Subsection A: Macro-Theoretical Cliodynamic Plugins

#### 43. Kümmel-Ayres-Warr Biophysical Exergy Growth (`KummelAyresExergyEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Kümmel, R. (1982). *The impact of energy on industrial growth*. Energy, 7(2), 189–203; Kümmel, R. (2011). *The Second Law of Economics*. Springer; Ayres, R. U., & Warr, B. (2005). *Accounting for growth: the role of physical work*. Structural Change and Economic Dynamics, 16(2), 181–209.
* **Governing Equations**:
  $$Y = A_0 \, K^\alpha \, L^\beta \, U^\gamma, \qquad \alpha + \beta + \gamma = 1, \qquad U = \eta_{\text{exergy}} \, E_{\text{primary}}$$
  $$\eta_{\text{exergy}} = \varepsilon_{\text{II}} \left(1 - \frac{T_C}{T_H}\right)$$
* **Parameters & Physical Units**:
  * $Y$: Aggregate physical economic output [$\text{Int\$/yr}$].
  * $K$: Productive physical capital stock [$\text{kg}$].
  * $L$: Human labor input [$\text{person-hours/yr}$].
  * $U$: Useful exergy / physical work flux delivered to transformation processes [$\text{W}$].
  * $\alpha = 0.25, \beta = 0.25, \gamma = 0.50$: Constant-returns output elasticities estimated for industrial economies (Ayres & Warr 2005, 2009).
* **Computational Complexity**: $O(1)$ per cell.

#### 44. West-Bettencourt Urban Allometric Scaling (`WestBettencourtAllometryEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Bettencourt, L. M. A., et al. (2007). *Growth, innovation, scaling, and the pace of life in cities*. PNAS, 104(17), 7301-7306; West, G. (2017). *Scale: The Universal Laws of Growth, Innovation, Sustainability, and the Pace of Life*. Penguin Press.
* **Governing Equations**:
  $$Y_{\text{socioeconomic}} = Y_0 \cdot N^{1.15}, \quad I_{\text{infrastructure}} = I_0 \cdot N^{0.85}$$
* **Parameters & Physical Units**:
  * $N$: City nucleated population [inhabitants].
  * $Y_{\text{socioeconomic}}$: Superlinear metric (economic output, patents, crime, infectious transmission) scaling as $N^{1.15}$.
  * $I_{\text{infrastructure}}$: Sublinear infrastructural metric (road surface, pipe length, electrical cables) scaling as $N^{0.85}$.
* **Computational Complexity**: $O(1)$ per urban cluster.

#### 45. Price Equation Multi-Level Altruistic Selection (`PriceMultilevelSelectionEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Price, G. R. (1970). *Selection and covariance*. Nature, 227(5257), 520-521; Bowles, S., & Gintis, H. (2011). *A Cooperative Species: Human Reciprocity and Its Evolution*. Princeton University Press.
* **Governing Equations**:
  $$\Delta \bar{z} = \frac{1}{\bar{w}} \operatorname{Cov}(w_g, z_g) + \frac{1}{\bar{w}} \mathbb{E}[w_g \Delta z_g]$$
* **Parameters & Physical Units**:
  * $\bar{z}$: Population-wide mean altruistic cooperation trait [dimensionless, $0.0 - 1.0$].
  * $\bar{w}$: Average demographic fitness across all groups.
  * $\operatorname{Cov}(w_g, z_g)$: Between-group selection covariance (high inter-group warfare rewards cohesive altruistic polities).
  * $\mathbb{E}[w_g \Delta z_g]$: Within-group selection expectation (selfish defectors out-reproduce altruists within the group).
* **Computational Complexity**: $O(\text{polities})$ per monthly tick.

#### 45.1. Kin Selection & Hamilton Rule Inclusive Fitness (`KinSelectionHamiltonEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Hamilton, W. D. (1964). *The genetical evolution of social behaviour. I & II*. Journal of Theoretical Biology, 7(1), 1–52.
* **Governing Equations**:
  $$r_{ij} \, B_j - C_i > 0, \qquad \Delta w_{\text{incl},i} = -C_i + \sum_{j \neq i} r_{ij} B_j$$
* **Parameters & Physical Units**:
  * $r_{ij} \in [0, 1]$: Coefficient of genealogical relatedness between actor $i$ and recipient $j$.
  * $B_j, C_i$: Reproductive fitness benefit conferred on recipient $j$ and direct fitness cost incurred by actor $i$.
* **Computational Complexity**: $O(1)$ per cell.

#### 46. Boserup Endogenous Agricultural Intensification (`BoserupAgriculturalIntensificationEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Boserup, E. (1965). *The Conditions of Agricultural Growth: The Economics of Agrarian Change under Population Pressure*. Allen & Unwin.
* **Governing Equations**:
  $$\text{FallowIndex } F = F_0 \cdot \left(\frac{N}{A_{\text{arable}}}\right)^{-\gamma_{\text{boserup}}}, \quad Y = Y_0 \cdot \left(\frac{L_{\text{labor}}}{A}\right)^{\theta_{\text{involution}}}$$
* **Parameters & Physical Units**:
  * $F$: Multi-cropping frequency (forest-fallow $\to$ bush-fallow $\to$ annual cropping $\to$ multi-cropping irrigation).
  * $N / A_{\text{arable}}$: Demographic density per unit arable land [$\text{inhabitants/km}^2$].
  * $\theta_{\text{involution}} \approx 0.65$: Decreasing marginal return to labor under manual terracing and weeding (Geertz agricultural involution).
* **Computational Complexity**: $O(1)$ per agricultural cell.

#### 47. Arthur Recombinant Combinatorial Evolution & Kremer Scale Growth (`ArthurCombinatorialTechnologyEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Kremer, M. (1993). *Population growth and technological change: One million B.C. to 1990*. Quarterly Journal of Economics, 108(3), 681–716; Arthur, W. B., & Polak, W. (2006). *The evolution of technology within a simple computer model*. Complexity, 11(5), 23–31; Arthur, W. B. (2009). *The Nature of Technology*. Free Press.
* **Governing Equations**:
  $$\frac{dM_t}{dt} = \mu_c \, N \, \binom{M_t}{m} \propto \mu_c \, N \, M_t^\alpha, \qquad \frac{\dot{A}}{A} = g \, N \, A^\phi$$
* **Parameters & Physical Units**:
  * $M_t$: Repertoire of active combinatorial technical modules.
  * $N$: Interacting population size of innovators and practitioners [persons] (Kremer 1993).
  * $\alpha > 1$: Combinatorial synergy exponent from multi-component recombination (Arthur 2009).
* **Computational Complexity**: $O(1)$ per cell.

#### 48. Hotelling Non-Renewable Resource Depletion Rent (`HotellingResourceDepletionEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137-175.
* **Governing Equations**:
  $$\frac{d P_{\text{resource}}}{dt} = r_{\text{interest}} \cdot P_{\text{resource}} - \frac{\partial C(R, Q)}{\partial R}$$
* **Parameters & Physical Units**:
  * $P_{\text{resource}}$: In-situ scarcity rent of mineral or fossil stock [$\text{\$/t}$].
  * $r_{\text{interest}}$: Social discount interest rate [$\text{yr}^{-1}$].
  * $C(R, Q)$: Marginal extraction cost function increasing as remaining stock $R$ depletes.
* **Computational Complexity**: $O(1)$ per mineral extraction node.

#### 49. Tainter Diminishing Marginal Returns on Sociopolitical Complexity (`TainterComplexityCollapseEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Tainter, J. A. (1988). *The Collapse of Complex Societies*. Cambridge University Press.
* **Governing Equations**:
  $$B(C) = B_0 + b \ln(1 + C), \qquad M(C) = m_0 \, C^\gamma \quad (\gamma > 1), \qquad \frac{d(B - M)}{dC} = \frac{b}{1 + C} - \gamma m_0 C^{\gamma - 1}$$
* **Parameters & Physical Units**:
  * $C$: Sociopolitical and bureaucratic differentiation complexity [dimensionless].
  * $B(C)$: Concave problem-solving benefit function ($\frac{dB}{dC} > 0, \frac{d^2B}{dC^2} < 0$, Tainter 1988).
  * $M(C)$: Convex per-capita energetic/fiscal maintenance cost ($\gamma > 1$); systemic simplification ensues when $\frac{dB}{dC} < \frac{dM}{dC}$.
* **Computational Complexity**: $O(1)$ per cell.

#### 50. NASA HANDY Socio-Ecological Predator-Prey Collapse (`HandyNasaHybridEngine`, `HandyNasaPureEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). *Human and nature dynamics (HANDY): Modeling inequality and use of resources in the collapse or sustainability of societies*. Ecological Economics, 101, 90-102.
* **Governing Equations**:
  $$\frac{dx_c}{dt} = \alpha_c x_c - \beta_c x_c, \quad \frac{dx_e}{dt} = \alpha_e x_e - \beta_e x_e, \quad \frac{dy}{dt} = \gamma y \left(1 - \frac{y}{K_y}\right) - \delta x \frac{y}{K_y}$$
* **Parameters & Physical Units**:
  * $x_c, x_e$: Commoner and elite population cohorts [persons] ($x = x_c + \kappa_{\text{ineq}} x_e$ effective extraction-weighted population in Motesharrei et al. 2014).
  * $y$: Regenerative natural ecological resource stock ($K_y$: carrying capacity; $\gamma$: intrinsic regeneration rate; $\delta$: per-capita depletion rate).
  * $\alpha, \beta$: Birth and famine-induced mortality rates.
* **Computational Complexity**: $O(1)$ per regional macro-cell.

#### 51. Meadows et al. World3 Limits to Growth System Dynamics (`World3CouplingEngine`, `World3PureEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Meadows, D. H., et al. (1972). *The Limits to Growth*. Universe Books; Meadows, D. L., et al. (1974). *Dynamics of Growth in a Finite World*. Wright-Allen Press.
* **Governing Equations**:
  12-state coupled nonlinear differential system linking persistent industrial pollution ($P_{\text{poll}}$), non-renewable natural resources ($R_{\text{nonren}}$), industrial capital stock ($K_{\text{ind}}$), agricultural capital ($K_{\text{agr}}$), and fertility cohorts.
* **Parameters & Physical Units**:
  * $P_{\text{poll}}$: Cumulative persistent environmental pollution [toxic index units].
  * $R_{\text{nonren}}$: Remaining fossil and mineral reserve fraction [$0.0 - 1.0$].
* **Computational Complexity**: $O(1)$ globally.

#### 52. Turchin-Goldstone Structural-Demographic Theory (SDT) Secular Cycles (`TurchinGoldstoneSDTEngine`, `StructuralDemographicBifurcationEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Goldstone, J. A. (1991). *Revolution and Rebellion in the Early Modern World*. University of California Press; Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press; Turchin, P. (2016). *Ages of Discord*. Beresta Books.
* **Governing Equations**:
  $$\Psi = \text{MMP} \cdot \text{EMP} \cdot \text{SFD}$$
  $$\text{MMP} = \frac{w_0}{w} \cdot \frac{N_{\text{urb}}}{N}, \qquad \text{EMP} = \frac{N_e}{N} \cdot \frac{y_e}{y_0}, \qquad \text{SFD} = \frac{D_{\text{state}}}{R_{\text{tax}}}$$
* **Parameters & Physical Units**:
  * $\Psi$: Multiplicative Political Stress Index [dimensionless] (Goldstone 1991; Turchin 2016).
  * $\text{MMP}$: Mass Mobilization Potential driven by inverse real wage $w_0 / w$ and urban youth/nucleation share $N_{\text{urb}} / N$.
  * $\text{EMP}$: Elite Mobilization Potential (elite overproduction $N_e / N$ and intra-elite relative income $y_e / y_0$).
  * $\text{SFD}$: State Fiscal Distress (sovereign public debt $D_{\text{state}}$ relative to fiscal revenue $R_{\text{tax}}$).
* **Computational Complexity**: $O(1)$ per cell / polity.

#### 52.1. Ibn Khaldun Asabiyyah Dynamics & Frontier Cohesion (`CliodynamicsEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Ibn Khaldun (1377). *The Muqaddimah: An Introduction to History*; Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press, Ch. 4.
* **Governing Equations**:
  $$\frac{dS}{dt} = r_0 \, S (1 - S) \cdot \mathbb{I}_{\text{frontier}} - \delta_0 \, S$$
* **Parameters & Physical Units**:
  * $S \in (0, 1]$: Collective solidarity (*asabiyyah*) capacity, growing logistically at rate $r_0$ [$\text{yr}^{-1}$] along metaethnic frontiers ($\mathbb{I}_{\text{frontier}} = 1$) and decaying at rate $\delta_0$ [$\text{yr}^{-1}$] in pacified imperial interiors (Turchin 2003).
* **Computational Complexity**: $O(1)$ per cell.

#### 53. Acemoglu-Robinson Property Rights & Expropriation Risk (`AcemogluRobinsonInstitutionsEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Acemoglu, D., Johnson, S., & Robinson, J. A. (2001). *The colonial origins of comparative development*. American Economic Review, 91(5), 1369–1401; Acemoglu, D., Johnson, S., & Robinson, J. A. (2005). *Institutions as a fundamental cause of long-run growth*. Handbook of Economic Growth, Vol. 1A, 385–472; Acemoglu, D., & Robinson, J. A. (2012). *Why Nations Fail*. Crown.
* **Governing Equations**:
  $$r_{\text{net}} = (1 - \tau_{\text{extr}}) \frac{\partial F(K, L)}{\partial K} - \delta, \qquad \frac{dK}{dt} = s_I \, r_{\text{net}} \, K$$
* **Parameters & Physical Units**:
  * $\tau_{\text{extr}} \in [0, 1]$: Effective elite expropriation and rent-extraction wedge on private output returns (Acemoglu et al. 2005).
  * $r_{\text{net}}$: Net private rate of return on productive physical capital $K$ [$\text{yr}^{-1}$].
* **Computational Complexity**: $O(1)$ per cell / polity.

#### 54. Granovetter Collective Action Threshold Cascades (`GranovetterThresholdCascadeEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Granovetter, M. (1978). *Threshold models of collective behavior*. American Journal of Sociology, 83(6), 1420-1443.
* **Governing Equations**:
  $$f_{\text{active}}(t + \Delta t) = \int_0^{f_{\text{active}}(t)} \mathcal{N}(\mu_{\text{grievance}}, \sigma^2) \, d\theta$$
* **Parameters & Physical Units**:
  * $f_{\text{active}}$: Proportion of population actively rioting or joining revolutionary mobilization [$0.0 - 1.0$].
  * $\theta \sim \mathcal{N}(\mu_{\text{grievance}}, \sigma^2)$: Individual threshold distribution of required participating peers before an agent joins collective action.
* **Computational Complexity**: $O(1)$ per urban node.

#### 55. Spatial Metapopulation SEIR Epidemic Migration (`SpatialMetapopulationSEIREngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Balcan, D., et al. (2009). *Multiscale mobility networks and the spatial spreading of infectious diseases*. PNAS, 106(51), 21484-21489.
* **Governing Equations**:
  $$\Delta I_i = \beta_i S_i \frac{I_i}{N_i} + \sum_{j \in \text{Ring1}} T_{ji} \frac{I_j}{N_j} S_i$$
* **Parameters & Physical Units**:
  * $T_{ji}$: Directional human transit volume along trade corridors [$\text{persons/day}$].
* **Computational Complexity**: $O(6)$ per infected cell.

#### 56. Krugman New Economic Geography Core-Periphery Agglomeration (`KrugmanCorePeripheryEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Krugman, P. (1991). *Increasing returns and economic geography*. Journal of Political Economy, 99(3), 483–499; Fujita, M., Krugman, P., & Venables, A. J. (1999). *The Spatial Economy*. MIT Press.
* **Governing Equations**:
  $$T_{ij} = \exp(\tau \, d_{ij}), \qquad G_i = \left[ \sum_j \lambda_j \, T_{ji}^{1 - \sigma} \right]^{\frac{1}{1 - \sigma}}$$
  $$w_i = \left[ \sum_j Y_j \, T_{ij}^{1 - \sigma} \, G_j^{\sigma - 1} \right]^{\frac{1}{\sigma}}, \qquad \omega_i = w_i \, G_i^{-\mu_M}, \qquad \frac{d\lambda_i}{dt} = \gamma \, \lambda_i (\omega_i - \bar{\omega})$$
* **Parameters & Physical Units**:
  * $T_{ij} \ge 1$: Samuelson iceberg transport cost factor over geodesic distance $d_{ij}$ [$\text{km}$] at rate $\tau$ [$\text{km}^{-1}$].
  * $\sigma > 1$: Dixit-Stiglitz elasticity of substitution among differentiated manufactured varieties ($\sigma = 4.0$, Krugman 1991).
  * $\mu_M \in (0, 1)$: Expenditure share on manufactured goods ($\mu_M = 0.40$, Krugman 1991).
  * $G_i, w_i, \omega_i$: Regional Dixit-Stiglitz manufacturing price index, nominal manufacturing wage, and real wage driving replicator agglomeration $\dot{\lambda}_i$.
* **Computational Complexity**: $O(6)$ local neighborhood evaluation per cell.

#### 57. Schelling-Axelrod Spatial Homophily & Segregation (`SchellingAxelrodSegregationEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Schelling, T. C. (1971). *Dynamic models of segregation*. Journal of Mathematical Sociology, 1(2), 143-186; Axelrod, R. (1997). *The dissemination of culture: A model with local convergence and global polarization*. Journal of Conflict Resolution, 41(2), 203-226.
* **Governing Equations**:
  $$P(\text{Relocate}_i) = \begin{cases} 1.0 & \text{if } \frac{N_{\text{same\_trait}}}{N_{\text{neighbors}}} < \tau_{\text{tolerance}} \\ 0.0 & \text{otherwise} \end{cases}$$
* **Parameters & Physical Units**:
  * $\tau_{\text{tolerance}} \in [0.2, 0.7]$: Agent homophily tolerance threshold.
* **Computational Complexity**: $O(6)$ per cell.

---

### Subsection B: Type B Scenario Heuristics & Historical Plugins (`org.ether.society.engines.tier2`)

#### 58. Frontier Cohesion & Asabiyyah Regeneration (`FrontierAsabiyyahEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Turchin, P. (2003). *Historical Dynamics*.
* **Governing Equations**:
  $$\frac{dA}{dt} = r_a (1 - A) \cdot \text{ConflictPressure} - \delta_a A \cdot \text{PaxImperia}$$
* **Parameters & Physical Units**:
  * $A \in [0, 1]$: Social cohesion index.
* **Computational Complexity**: $O(1)$ per frontier cell.

#### 59. Amerindian Anthropogenic Agro-Pyrogenic Fire Ecology (`AmerindianEcosystemEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Pyne, S. J. (1997). *World Fire: The Culture of Fire on Earth*. University of Washington Press; Denevan, W. M. (1992). *The pristine myth: The landscape of the Americas in 1492*. Annals of the Association of American Geographers, 82(3), 369-385.
* **Governing Equations**:
  $$\frac{d B_{\text{pyro}}}{dt} = P_{\text{ignition}} \cdot (1.0 - \text{Moisture}) - K_{\text{regrowth}} B_{\text{pyro}}$$
* **Parameters & Physical Units**:
  * $B_{\text{pyro}}$: Pyrogenically maintained grassland/savanna biomass density [$\text{kg/ha}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 60. Roman Imperial Fiscal-Tributary Flow & Coinage Debasement (`RomanImperialCliodynamicEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Hopkins, K. (1980). *Taxes and trade in the Roman Empire (200 B.C.–A.D. 400)*. Journal of Roman Studies, 70, 101–125; Butcher, K., & Ponting, M. (2014). *The Metallurgy of Roman Silver Coinage*. Cambridge University Press; Fisher, I. (1911). *The Purchasing Power of Money*. Macmillan.
* **Governing Equations**:
  $$\phi_{\text{Ag}}(t) = \frac{M_{\text{Ag}}(t)}{M_{\text{coin}}(t)}, \qquad M_{\text{coin}} V = P Y$$
* **Parameters & Physical Units**:
  * $\phi_{\text{Ag}} \in (0, 1]$: Silver fineness fraction of minted imperial coinage ($M_{\text{Ag}}$: bullion silver mass; $M_{\text{coin}}$: total circulating coin mass) coupled to price level $P$ via Fisher's equation of exchange.
* **Computational Complexity**: $O(1)$ per imperial province.

#### 62. Tokugawa Japan Sakoku Autarkic Forestry (`EdoJapanIsolationEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Totman, C. (1989). *The Green Archipelago: Forestry in Pre-Industrial Japan*. University of California Press.
* **Governing Equations**:
  $$\frac{d M_{\text{timber}}}{dt} = \text{SilvicultureYield}(k_{\text{pc}}) - C_{\text{domestic}}, \quad \text{TradeLeakage} \equiv 0$$
* **Parameters & Physical Units**:
  * $M_{\text{timber}}$: Insular timber stock [$\text{metric tons}$].
* **Computational Complexity**: $O(1)$ per insular cell.

#### 63. Asymmetric Mercantilist Colonial Extraction (`AsymmetricColonialTradeEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Wallerstein, I. (1974). *The Modern World-System*. Academic Press.
* **Governing Equations**:
  $$\Phi_{\text{surplus}} = Q_{\text{raw}} \cdot (P_{\text{metropole}} - P_{\text{colony}}) \cdot (1.0 - \text{RebellionRisk})$$
* **Parameters & Physical Units**:
  * $\Phi_{\text{surplus}}$: Extracted colonial bullion/commodity surplus [$\text{\$/year}$].
* **Computational Complexity**: $O(1)$ per trade link.

#### 64. Wittfogel Hydraulic Despotism (`HydrologicalEngineeringEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Wittfogel, K. A. (1957). *Oriental Despotism: A Comparative Study of Total Power*. Yale University Press.
* **Governing Equations**:
  $$\text{BureaucraticCentralization} = \alpha_{\text{canal}} \cdot \frac{K_{\text{canal}}}{A_{\text{irrigated}}}$$
* **Parameters & Physical Units**:
  * $K_{\text{canal}}$: Earthwork dike and irrigation canal structural capital [$\text{kg}$].
* **Computational Complexity**: $O(1)$ per alluvial cell.

#### 65. Eustatic Marine Submersion & Climate Displacement (`MarineSubmersionEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Nicholls, R. J., & Cazenave, A. (2010). *Sea-level rise and its impact on coastal zones*. Science, 328(5985), 1517-1520.
* **Governing Equations**:
  $$N_{\text{displaced}} = \sum_{z_i < z_{\text{sea}}} N_i, \quad \mathbf{F}_{\text{flee}} = -\nabla z_{\text{elevation}}$$
* **Parameters & Physical Units**:
  * $N_{\text{displaced}}$: Inundated coastal population requiring relocation.
* **Computational Complexity**: $O(1)$ per coastal cell.

#### 66. Maritime Highway Cabotage (`MaritimeHighwayEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Braudel, F. (1949). *La Méditerranée et le Monde Méditerranéen à l'époque de Philippe II*. Armand Colin.
* **Governing Equations**:
  $$\mathbf{v}_{\text{effective}} = \mathbf{v}_{\text{hull}} + \mathbf{u}_{\text{ocean\_current}} + \mathbf{w}_{\text{wind}} \cdot \hat{\mathbf{d}}$$
* **Parameters & Physical Units**:
  * $\mathbf{v}_{\text{effective}}$: Net maritime cargo transit vector [$\text{km/day}$].
* **Computational Complexity**: $O(1)$ per sea lane.

#### 67. Dutch Polder Drainage & Land Reclamation (`LandReclamationEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: van Veen, J. (1955). *Dredge, Drain, Reclaim: The Art of a Nation*. Martinus Nijhoff.
* **Governing Equations**:
  $$\Delta A_{\text{polder}} = \frac{E_{\text{windmill\_joules}}}{\rho_{\text{water}} g \Delta h_{\text{lift}}}$$
* **Parameters & Physical Units**:
  * $\Delta A_{\text{polder}}$: Reclaimed fertile arable land area [$\text{m}^2$].
* **Computational Complexity**: $O(1)$ per coastal cell.

#### 68. Monastic Demographic Buffer & Literacy Preservation (`MonasticDemographicBufferEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Goody, J. (1983). *The Development of the Family and Marriage in Europe*. Cambridge University Press.
* **Governing Equations**:
  $$\frac{d K_{\text{manuscripts}}}{dt} = \alpha_{\text{scriptorium}} N_{\text{monks}} - \delta_{\text{raid}} K_{\text{manuscripts}}$$
* **Parameters & Physical Units**:
  * $K_{\text{manuscripts}}$: Preserved codices and parchment library volume [manuscript units].
* **Computational Complexity**: $O(1)$ per monastic settlement.

#### 69. Military Technological Revolution Shock (`MilitaryTechShockEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Parker, G. (1996). *The Military Revolution: Military Innovation and the Rise of the West, 1500-1800*. Cambridge University Press.
* **Governing Equations**:
  $$\Delta \text{Lethality} = \text{Lethality}_0 \cdot \exp(\kappa_{\text{gunpowder}} \cdot k_{\text{metallurgy}})$$
* **Parameters & Physical Units**:
  * $k_{\text{metallurgy}}$: Cast-bronze / forged-iron ordnance capital density [$\text{kg/capita}$].
* **Computational Complexity**: $O(1)$ per military polity.

#### 70. Weberian Protestant Work Ethic (`ProtestantWorkEthicEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Weber, M. (1905). *Die protestantische Ethik und der Geist des Kapitalismus*. Archiv für Sozialwissenschaft und Sozialpolitik.
* **Governing Equations**:
  $$s_{\text{reinvestment}} = s_{\text{base}} + \Delta s \cdot \text{CalvinistAdherence}$$
* **Parameters & Physical Units**:
  * $s_{\text{reinvestment}}$: Propensity to save and accumulate physical machinery [$0.05 - 0.25$].
* **Computational Complexity**: $O(1)$ per polity.

#### 71. Pinker Secular Pacification Function (`PinkerViolenceDeclinePureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Pinker, S. (2011). *The Better Angels of Our Nature: Why Violence Has Declined*. Viking.
* **Governing Equations**:
  $$\mu_{\text{homicide}} = \mu_0 \cdot \exp(-\lambda_{\text{leviathan}} \cdot \text{StateMonopolyOfViolence})$$
* **Parameters & Physical Units**:
  * $\mu_{\text{homicide}}$: Interpersonal homicide rate per 100,000 per year.
* **Computational Complexity**: $O(1)$ per polity.

#### 72. Elinor Ostrom Polycentric Commons Governance (`OstromCommonsPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Ostrom, E. (1990). *Governing the Commons: The Evolution of Institutions for Collective Action*. Cambridge University Press.
* **Governing Equations**:
  $$\Delta W_{\text{resource}} = \begin{cases} 0.0 & \text{if } \text{GraduatedSanctions} \ge \tau_{\text{ostrom}} \\ -\Delta_{\text{tragedy}} & \text{otherwise} \end{cases}$$
* **Parameters & Physical Units**:
  * $\Delta_{\text{tragedy}}$: Over-exploitation depletion penalty under unmonitored open access.
* **Computational Complexity**: $O(1)$ per common pool resource.

#### 73. James C. Scott Anti-State Agro-Evasion (`ScottAgainstTheGrainPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Scott, J. C. (2009). *The Art of Not Being Governed: An Anarchist History of Upland Southeast Asia*. Yale University Press; Scott, J. C. (2017). *Against the Grain*. Yale University Press.
* **Governing Equations**:
  $$P_{\text{evade}} = \frac{1}{1 + \exp\!\left(-\beta (\tau_{\text{grain}} - c_{\text{upland}})\right)}, \qquad M_{\text{evasion}} = N_{\text{valley}} \cdot P_{\text{evade}}$$
* **Parameters & Physical Units**:
  * $M_{\text{evasion}}$: Out-migration rate of peasants fleeing state cereal taxation to non-state highlands (Zomia).
* **Computational Complexity**: $O(1)$ per frontier cell.

#### 74. Human Self-Domestication Syndrome (`SelfDomesticationEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Wrangham, R. (2019). *The Goodness Paradox: The Strange Relationship Between Peace and Violence in Human Evolution*. Pantheon Books.
* **Governing Equations**:
  $$\frac{d \bar{A}_{\text{reactive}}}{dt} = -s_{\text{coalition}} \cdot \bar{A}_{\text{reactive}}$$
* **Parameters & Physical Units**:
  * $\bar{A}_{\text{reactive}}$: Population mean reactive aggressive response threshold.
* **Computational Complexity**: $O(1)$ per cohort.

#### 75. Sexual Selection, Operational Sex Ratio & Polygyny Mating Skew (`SexualSelectionMatingEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Bateman, A. J. (1948). *Intra-sexual selection in Drosophila*. Heredity, 2(3), 349–368; Emlen, S. T., & Oring, L. W. (1977). *Ecology, sexual selection, and the evolution of mating systems*. Science, 197(4300), 215–223; Henrich, J., Boyd, R., & Richerson, P. J. (2012). *The puzzle of monogamous marriage*. Philosophical Transactions of the Royal Society B, 367(1589), 657–669.
* **Governing Equations**:
  $$\text{OSR} = \frac{N_{m,\text{unpaired}}}{N_{f,\text{receptive}}}, \qquad u_m = 1 - \frac{N_f}{N_m \, \bar{w}_{\text{mated}}}$$
* **Parameters & Physical Units**:
  * $\text{OSR}$: Operational sex ratio (Emlen & Oring 1977); $u_m \in [0, 1)$: fraction of unmated adult males generated when mean wives per mated male $\bar{w}_{\text{mated}} > 1$ (Henrich et al. 2012).
* **Computational Complexity**: $O(1)$ per polity.

#### 76. Vaclav Smil Primary Energy Transition Turnover (`SmilMaterialTransitionsPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Smil, V. (2010). *Energy Transitions: History, Requirements, Prospects*. Praeger.
* **Governing Equations**:
  $$\frac{d S_{\text{energy}}}{dt} = \frac{1}{\tau_{\text{smil}}} (S_{\text{target}} - S_{\text{energy}}), \quad \tau_{\text{smil}} \approx 35 \text{ to } 50\text{ years}$$
* **Parameters & Physical Units**:
  * $S_{\text{energy}}$: Fractional share of primary energy source (wood $\to$ coal $\to$ hydrocarbons $\to$ renewables).
* **Computational Complexity**: $O(1)$ globally.

#### 77. Mandelbrot Urban Fractal Geometry (`SpatialCityFractalEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Batty, M., & Longley, P. A. (1994). *Fractal Cities: A Geometry of Form and Function*. Academic Press.
* **Governing Equations**:
  $$N(r) \propto r^{-D_f}, \quad D_f \in [1.60, 1.90]$$
* **Parameters & Physical Units**:
  * $D_f$: Urban radial fractal dimension.
* **Computational Complexity**: $O(1)$ per urban cluster.

#### 78. Henrich Gumbel Skill Transmission & Tasmanian Effect (`TasmanianCulturalRegressionEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Henrich, J. (2004). *Demography and cultural evolution: How adaptive cultural processes can produce maladaptive losses — the Tasmanian case*. American Antiquity, 69(2), 197–214.
* **Governing Equations**:
  $$\Delta \bar{z} = -\alpha + \beta \left(\gamma_E + \ln N_e\right) = \beta \ln\!\left(\frac{N_e}{N^*}\right), \qquad N^* = \exp\!\left(\frac{\alpha}{\beta} - \gamma_E\right)$$
* **Parameters & Physical Units**:
  * $\Delta \bar{z}$: Expected change per generation in mean population skill $\bar{z}$ under oblique/selective imitation from the most skilled model in a Gumbel extreme-value pool of size $N_e$ (Henrich 2004, Eq. 3).
  * $\alpha > 0$: Systematic transmission bias/imperfection; $\beta > 0$: dispersion of learner skill innovations; $\gamma_E \approx 0.5772$: Euler-Mascheroni constant; $N^*$: critical effective learner pool below which $\Delta \bar{z} < 0$.
* **Computational Complexity**: $O(1)$ per isolated landmass.

#### 79. Nordhaus DICE Climate-Economy Damage (`NordhausDicePureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Nordhaus, W. D. (1992). *An optimal transition path for controlling greenhouse gases*. Science, 258(5086), 1315-1319.
* **Governing Equations**:
  $$Y_{\text{net}} = (1 - \Omega(\Delta T)) Y_{\text{gross}}, \quad \Omega(\Delta T) = \psi_1 \Delta T + \psi_2 \Delta T^2$$
* **Parameters & Physical Units**:
  * $\psi_1 = 0.0, \psi_2 = 0.00284$: Quadratic GDP damage coefficient per $^\circ\text{C}^2$.
* **Computational Complexity**: $O(1)$ per climate region.

#### 80. Leslie White Energetic Evolution (`LeslieWhitePureEngine`, `WhiteEnergyEvolutionEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: White, L. A. (1943). *Energy and the evolution of culture*. American Anthropologist, 45(3), 335–356.
* **Governing Equations**:
  $$C = E \times T_{\text{eff}}$$
* **Parameters & Physical Units**:
  * $C$: Degree of cultural development; $E$: annual energy harnessed per capita [$\text{J/(person}\cdot\text{yr)}$]; $T_{\text{eff}}$: thermodynamic efficiency of technological tools putting energy to work.
* **Computational Complexity**: $O(1)$ per polity.

#### 81. Gerhard Lenski Social Stratification & Milanovic-Piketty Dynamic Inequality (`LenskiPureEngine`, `LenskiEcologicalEvolutionaryEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Lenski, G. (1966). *Power and Privilege: A Theory of Social Stratification*. McGraw-Hill; Milanovic, B., Lindert, P. H., & Williamson, J. G. (2011). *Pre-Industrial Inequality*. Economic Journal, 121(551), 255–272; Piketty, T. (2014). *Capital in the Twenty-First Century*. Harvard University Press.
* **Governing Equations**:
  $$G_{\max}(\alpha) = \frac{\alpha - 1}{\alpha} (1 - \varepsilon), \qquad \alpha = \frac{y}{s_{\min}}, \qquad \beta = \frac{s}{g}, \qquad r > g$$
* **Parameters & Physical Units**:
  * $G_{\max}(\alpha)$: Milanovic-Lindert-Williamson (2011) Inequality Possibility Frontier (IPF) as a function of mean income $y$ in multiples $\alpha = y / s_{\min}$ of physiological subsistence $s_{\min}$, with elite population share $\varepsilon$.
  * $r - g$: Rate of return on capital $r$ minus macroeconomic growth rate $g$ driving wealth-to-income concentration $\beta = K / Y$ (Piketty 2014).
* **Computational Complexity**: $O(1)$ per cell / polity.

#### 82. Marvin Harris Cultural Materialism (`CulturalMaterialismPureEngine`, `HarrisCulturalMaterialismEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Harris, M. (1979). *Cultural Materialism: The Struggle for a Science of Culture*. Random House.
* **Governing Equations**:
  $$\pi_{\text{practice}} = E_{\text{yield}} - E_{\text{input}}, \qquad p^* = \arg\max_{p} \left(E_{\text{yield}}(p) - E_{\text{input}}(p)\right)$$
* **Parameters & Physical Units**:
  * Evaluates ecological-energetic selection of subsistence practices and cultural taboos from net caloric balance [$\text{J/yr}$].
* **Computational Complexity**: $O(1)$ per cultural node.

#### 83. Multi-Hazard Early Warning Threshold Indicator (`CliodynamicAdvisor`)
* **Model Classification**: Tier 2 - Type B (Diagnostic Early-Warning Module)
* **Academic Literature Reference**: Scheffer, M., et al. (2009). *Early-warning signals for critical transitions*. Nature, 461(7260), 53–59.
* **Governing Equations**:
  $$\mathbf{\Omega}_{\text{risk}} = \left[ \mathbb{I}(T_w \ge T_{w,\text{crit}}), \; \mathbb{I}(L_{\text{Liebig}} \le L_{\min}), \; \mathbb{I}(h_{\text{aq}} \le h_{\min}), \; \mathbb{I}(\Psi \ge \Psi_{\text{crit}}) \right]$$
* **Computational Complexity**: $O(1)$ per cell.

#### 84. Axial Age Moralizing Gods & Prosocial Policing (`AxialAgeMoralizingGodsEngine`)
* **Model Classification**: Tier 2 - Type B (Institutional & Cognitive Cliodynamics)
* **Academic Literature Reference**: Norenzayan, A. (2013). *Big Gods: How Religion Transformed Cooperation and Conflict*. Princeton University Press; Whitehouse, H., et al. (2019). *Complex societies precede moralizing gods throughout world history*. Nature, 568(7751), 226–229.
* **Governing Equations**:
  $$P(\text{MoralizingGods} \mid \text{SPC}_1) = \frac{1}{1 + \exp\!\left(-\beta (\log_{10} N_{\text{polity}} - \log_{10} N_c)\right)}, \qquad N_c \approx 10^5\text{–}10^6\text{ persons}$$
* **Parameters & Physical Units**:
  * $N_{\text{polity}}$: Polity population scale where anonymous exchange exceeds direct Dunbar/kin reputation monitoring (Whitehouse et al. 2019 Seshat threshold).
* **Computational Complexity**: $O(1)$ per cell.

#### 85. Barfield Nomadic Steppe Shadow Empire & Extortion Symbiosis (`BarfieldNomadicSteppeEmpireEngine`)
* **Model Classification**: Tier 2 - Type A / B (Geopolitical Cliodynamics)
* **Academic Literature Reference**: Barfield, T. J. (1989). *The Perilous Frontier: Nomadic Empires and China, 221 BC to AD 1757*. Blackwell; Di Cosmo, N. (2002). *Ancient China and Its Enemies*. Cambridge University Press.
* **Governing Equations**:
  $$\frac{dC_{\text{steppe}}}{dt} = \eta_{\text{redist}} \, \Phi_{\text{tribute}}(Y_{\text{sedentary}}) - \delta_c \, C_{\text{steppe}}$$
* **Parameters & Physical Units**:
  * $\Phi_{\text{tribute}}(Y_{\text{sedentary}})$: External tributary surplus extracted from the adjacent sedentary agrarian empire; when $Y_{\text{sedentary}} \to 0 \implies \Phi_{\text{tribute}} = 0$, the steppe confederation $C_{\text{steppe}}$ undergoes synchronous centrifugal collapse ($\dot{C}_{\text{steppe}} = -\delta_c C_{\text{steppe}}$).
* **Computational Complexity**: $O(1)$ per pastoral cell.

#### 85.1. Polanyi Fictitious Commodities & The Double Movement (`PolanyiFictitiousCommoditiesEngine`)
* **Model Classification**: Tier 2 - Type B (Institutional Macroeconomics)
* **Academic Literature Reference**: Polanyi, K. (1944). *The Great Transformation: The Political and Economic Origins of Our Time*. Farrar & Rinehart.
* **Governing Equations**:
  $$\frac{dA_{\text{enclosed}}}{dt} = \kappa_m \, m_{\text{market}} \, A_{\text{commons}}, \qquad \frac{dS_{\text{counter}}}{dt} = \gamma_p \, m_{\text{market}} \, \max\!\left(0, \; 1 - \frac{c_{\text{food}}}{c_{\text{sub}}}\right)$$
* **Parameters & Physical Units**:
  * $A_{\text{commons}}, A_{\text{enclosed}}$: Customary commons vs. market-enclosed land/biomass stock.
  * $S_{\text{counter}}$: Protective social countermovement ("Double Movement") triggered when market exposure erodes subsistence security $c_{\text{food}} / c_{\text{sub}}$.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.2. Bagehot Fractional-Reserve Liquidity & Debt Deflation (`BagehotFractionalReserveEngine`)
* **Model Classification**: Tier 2 - Type B (Financial Macroeconomics & Banking Crises)
* **Academic Literature Reference**: Bagehot, W. (1873). *Lombard Street: A Description of the Money Market*. Henry S. King; Fisher, I. (1933). *The Debt-Deflation Theory of Great Depressions*. Econometrica, 1(4), 337–357.
* **Governing Equations**:
  $$M = \frac{1 + c_d}{r_R + c_d} \, B_{\text{base}}, \qquad d_{\text{real}} = \frac{D_{\text{nom}}}{P}, \qquad \frac{dP}{dt} = -\kappa_f \, \max(0, L_{\text{demand}} - R_{\text{LOLR}})$$
* **Parameters & Physical Units**:
  * $M / B_{\text{base}}$: Fractional-reserve credit multiplier over specie/base money $B_{\text{base}}$ with reserve ratio $r_R$ and currency-deposit ratio $c_d$.
  * $R_{\text{LOLR}}$: Central-bank Lender-of-Last-Resort liquidity injection arresting Fisherian debt-deflation liquidation ($d_{\text{real}} = D_{\text{nom}}/P$).
* **Computational Complexity**: $O(1)$ per cell.

#### 85.3. Corden-Neary Dutch Disease & Resource Curse (`CordenNearyDutchDiseaseEngine`)
* **Model Classification**: Tier 2 - Type B (Open-Economy Macroeconomics)
* **Academic Literature Reference**: Corden, W. M., & Neary, J. P. (1982). *Booming Sector and De-Industrialisation in a Small Open Economy*. Economic Journal, 92(368), 825–848; Auty, R. (1993). *Sustaining Development in Mineral Economies*. Routledge.
* **Governing Equations**:
  $$L_B + L_M + L_N = L, \qquad w = P_B \frac{\partial F_B}{\partial L_B} = P_M \frac{\partial F_M}{\partial L_M} = P_N \frac{\partial F_N}{\partial L_N}, \qquad \frac{\partial L_M}{\partial P_B} < 0$$
* **Parameters & Physical Units**:
  * $B, M, N$: Booming extractive sector, lagging traded manufacturing/diversified sector, and non-tradable services sector; a boom in $B$ raises real wages $w$ and relative non-tradable price $P_N / P_M$ (resource-movement + spending effects), crowding out $L_M$ and $Y_M$.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.4. Harold Innis Staples Thesis & Export Frontier Dependency (`InnisStaplesThesisEngine`)
* **Model Classification**: Tier 2 - Type B (Spatial Economic History)
* **Academic Literature Reference**: Mackintosh, W. A. (1923). *Economic Factors in Canadian History*. Canadian Historical Review, 4(1), 12–25; Innis, H. A. (1930). *The Fur Trade in Canada*. University of Toronto Press; Watkins, M. H. (1963). *A staple theory of economic growth*. Canadian Journal of Economics and Political Science, 29(2), 141–158.
* **Governing Equations**:
  $$Y_{\text{frontier}} = P_S Q_S + \sum_k \ell_k P_S Q_S, \qquad \frac{dR_S}{dt} = -Q_S$$
* **Parameters & Physical Units**:
  * $Q_S$: Raw staple extraction flux from natural stock $R_S$; $\ell_k$: backward, forward, and fiscal-demand linkage multipliers (Watkins 1963).
* **Computational Complexity**: $O(1)$ per cell.

#### 85.5. Norbert Wiener Cybernetic Negative-Feedback Homeostasis (`WienerCyberneticHomeostasisEngine`)
* **Model Classification**: Tier 2 - Type B (Control Theory & Homeostasis)
* **Academic Literature Reference**: Wiener, N. (1948). *Cybernetics: Or Control and Communication in the Animal and the Machine*. MIT Press; Ashby, W. R. (1956). *An Introduction to Cybernetics*. Chapman & Hall.
* **Governing Equations**:
  $$e(t) = r_{\text{set}} - y(t), \qquad u(t) = K_p \, e(t)$$
* **Computational Complexity**: $O(1)$ per cell.

#### 85.6. Alfred Crosby Columbian Exchange & Virgin-Soil Epidemics (`CrosbyColumbianExchangeEngine`)
* **Model Classification**: Tier 2 - Type B (Historical Ecological Transitions & Epidemiology)
* **Academic Literature Reference**: Crosby, A. W. (1972). *The Columbian Exchange: Biological and Cultural Consequences of 1492*. Greenwood Press; Anderson, R. M., & May, R. M. (1991). *Infectious Diseases of Humans*. Oxford University Press.
* **Governing Equations**:
  $$1 - s_\infty = \exp\!\left(-\mathcal{R}_0 (1 - s_\infty)\right) \quad (S_0 \approx N_0 \text{ virgin-soil final-size relation})$$
* **Computational Complexity**: $O(1)$ per cell.

#### 86. Law of Accelerating Returns (`KurzweilAcceleratingReturnsEngine`)
* **Model Classification**: Tier 2 - Type B (Prospective Exploratory Heuristic)
* **Academic Literature Reference**: Moore, G. E. (1965). *Cramming more components onto integrated circuits*. Electronics, 38(8), 114–117; Kurzweil, R. (2005). *The Singularity Is Near*. Viking; Landauer, R. (1961). *Irreversibility and heat generation in the computing process*. IBM J. Res. Dev., 5(3), 183–191.
* **Governing Equations**:
  $$\frac{dC}{dt} = r(C) \, C, \qquad E_{\text{bit}} \ge k_B T \ln 2 \quad (\text{Landauer thermodynamic bound})$$
* **Computational Complexity**: $O(1)$ per cycle.

#### 87. Geoengineering Stratospheric Aerosol Albedo Feedback (`GeoengineeringAlbedoFeedbackEngine`)
* **Model Classification**: Tier 2 - Type B (Prospective Exploratory Heuristic)
* **Academic Literature Reference**: Crutzen, P. J. (2006). *Albedo enhancement by stratospheric sulfur injections*. Climatic Change, 77(3), 211–220.
* **Governing Equations**:
  $$\Delta F_{\text{SRM}} = -\frac{S_0}{4} \, T_{\text{atm}}^2 (1 - A_c) \, \Delta \alpha_{\text{strat}}(M_{\text{SO}_2})$$
* **Computational Complexity**: $O(1)$ per body.

#### 88. Closed-Loop Bioregenerative Life-Support Habitat (`PressurizedHabitatEngine`)
* **Model Classification**: Tier 2 - Type B (Off-Earth Exploratory Heuristic)
* **Academic Literature Reference**: Eckart, P. (1996). *Spaceflight Life Support and Biospherics*. Microcosm Press / Kluwer; Wheeler, R. M. (2017). *Agriculture for space*. Open Agriculture, 2(1), 14–32.
* **Governing Equations**:
  $$\frac{dm_{\text{O}_2}}{dt} = \dot{m}_{\text{photo,O}_2} + \dot{m}_{\text{electrolysis,O}_2} - N \dot{m}_{\text{resp,O}_2} - \dot{m}_{\text{leak}}(\Delta P)$$
* **Computational Complexity**: $O(1)$ per habitat cell.

#### 88.1. In-Situ Resource Utilization (ISRU) Mass Closure (`IsruAutarkyAndSpaceColonizationEngine`)
* **Model Classification**: Tier 2 - Type B (Off-Earth Exploratory Heuristic)
* **Academic Literature Reference**: Anand, M., et al. (2012). *Lunar ISRU: A review*. Planetary and Space Science, 74(1), 42–48.
* **Governing Equations**:
  $$R_{\text{ISRU}} = \frac{\dot{M}_{\text{local}}}{\dot{M}_{\text{local}} + \dot{M}_{\text{import}}}$$
* **Computational Complexity**: $O(1)$ per habitat cell.

#### 89. Cultural Transmission & Shannon Information Entropy (`InformationEntropyEngine`, `CulturalSociologyEngine`)
* **Model Classification**: Tier 2 - Type B (Cultural Evolution)
* **Academic Literature Reference**: Shannon, C. E. (1948). *A mathematical theory of communication*. Bell System Technical Journal, 27(3), 379–423; Cavalli-Sforza, L. L., & Feldman, M. W. (1981). *Cultural Transmission and Evolution*. Princeton University Press.
* **Governing Equations**:
  $$H = -\sum_i p_i \ln p_i, \qquad C_{\text{channel}} = B \log_2\!\left(1 + \frac{S}{N_0 B}\right)$$
* **Computational Complexity**: $O(1)$ per cell.

#### 90. Glottochronological Linguistic Divergence (`LanguageLinguisticEngine`)
* **Model Classification**: Tier 2 - Type B (Historical Linguistics)
* **Academic Literature Reference**: Swadesh, M. (1952). *Lexico-statistic dating of prehistoric ethnic contacts*. Proceedings of the American Philosophical Society, 96(4), 452–463.
* **Governing Equations**:
  $$c(t) = \exp(-2 \lambda_g t), \qquad \lambda_g \approx 0.14\text{–}0.19\,\text{kyr}^{-1}$$
* **Computational Complexity**: $O(1)$ per dialect community.

#### 93. Lanchester Attrition & Structural Armor Penetration (`ThermodynamicWarfareEngine`)
* **Model Classification**: Tier 2 - Type B (Conflict Mechanics)
* **Academic Literature Reference**: Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Co.; Tate, A. (1967). *A theory for the deceleration of long rods after impact*. Journal of the Mechanics and Physics of Solids, 15(6), 387–399; Ashby, M. F. (2011). *Materials Selection in Mechanical Design*. Butterworth-Heinemann.
* **Governing Equations**:
  $$\frac{dA}{dt} = -\beta B, \qquad \frac{dB}{dt} = -\alpha A \implies \alpha (A_0^2 - A^2) = \beta (B_0^2 - B^2)$$
  $$\frac{1}{2} \rho_p v_0^2 > R_t \approx c_y \sigma_y \quad (\text{hydrodynamic/plastic penetration threshold})$$
* **Parameters & Physical Units**:
  * $A, B$: Opposing force strengths under Lanchester's Square Law with lethality coefficients $\alpha, \beta$ [$\text{yr}^{-1}$].
  * $\sigma_y$: Fortification or armor material yield strength [$\text{Pa}$].
* **Computational Complexity**: $O(1)$ per contested cell.

#### 94. Gravity & Random-Utility Spatial Migration (`ThermodynamicMigrationEngine`)
* **Model Classification**: Tier 2 - Type B (Spatial Demography)
* **Academic Literature Reference**: Ravenstein, E. G. (1885). *The laws of migration*. J. Stat. Soc. London, 48(2), 167–235; Zipf, G. K. (1946). *The $P_1 P_2 / D$ hypothesis*. American Sociological Review, 11(6), 677–686; McFadden, D. (1974). *Conditional logit analysis of qualitative choice behavior*.
* **Governing Equations**:
  $$M_{ij} = G_{\text{mig}} \frac{N_i N_j}{d_{ij}^\gamma} \exp\!\left(\beta (U_j - U_i - C_{ij})\right)$$
* **Computational Complexity**: $O(6)$ Ring-1 adjacency evaluation.

#### 95. Jevons Paradox & Direct Energy Rebound (`JevonsParadoxEngine`)
* **Model Classification**: Tier 2 - Type B (Energy Economics)
* **Academic Literature Reference**: Jevons, W. S. (1865). *The Coal Question*. Macmillan; Saunders, H. D. (1992). *The Khazzoom-Brookes postulate and neoclassical growth*. Energy Journal, 13(4), 131–148; Sorrell, S. (2009). *Jevons' Paradox revisited*. Energy Policy, 37(4), 1456–1469.
* **Governing Equations**:
  $$\varepsilon_{\eta}^{E} = -1 - \varepsilon_{P_S}^{S}, \qquad \varepsilon_{P_S}^{S} < -1 \implies \frac{\partial E}{\partial \eta} > 0 \quad (\text{backfire})$$
* **Computational Complexity**: $O(1)$ per cell.

#### 96. Deterministic Lorenz Strange Attractor (`BifurcationChaosEngine`)
* **Model Classification**: Tier 2 - Type B (Nonlinear Dynamical Sandbox)
* **Academic Literature Reference**: Lorenz, E. N. (1963). *Deterministic nonperiodic flow*. Journal of the Atmospheric Sciences, 20(2), 130–141.
* **Governing Equations**:
  $$\dot{x} = \sigma (y - x), \qquad \dot{y} = x (\rho - z) - y, \qquad \dot{z} = x y - \beta z$$
* **Computational Complexity**: $O(1)$ per node.

#### 97. Nelson-Phelps Technology Diffusion & Directed Technical Change (`PhysicalCapitalDiffusionEngine`)
* **Model Classification**: Tier 2 - Type B (Macroeconomic Diffusion)
* **Academic Literature Reference**: Nelson, R. R., & Phelps, E. S. (1966). *Investment in humans, technological diffusion, and economic growth*. American Economic Review, 56(1/2), 69–75; Hicks, J. R. (1932). *The Theory of Wages*. Macmillan; Acemoglu, D. (2002). *Directed technical change*. Quarterly Journal of Economics, 117(3), 781–809.
* **Governing Equations**:
  $$\frac{dA_i}{dt} = \phi(h_i) \left(T(t) - A_i(t)\right), \qquad \frac{A_M}{A_Z} = \left(\frac{P_M}{P_Z}\right)^{\frac{1}{\varepsilon - 1}} \left(\frac{Z}{M}\right)^{\frac{\sigma - 1}{\varepsilon - 1}}$$
* **Parameters & Physical Units**:
  * $\phi(h_i) > 0$: Catch-up diffusion rate toward frontier $T(t)$ (Nelson & Phelps 1966).
  * $Z, M$: Scarce organic vs. abundant mineral input factors driving Hicks-Acemoglu induced factor-saving capital deepening when gross elasticity of substitution $\sigma > 1$.
* **Computational Complexity**: $O(6)$ per cell.

#### 98. Tinbergen Gravity Equation of Trade (`TradeNetworkRouter`)
* **Model Classification**: Tier 2 - Type B (International Trade)
* **Academic Literature Reference**: Tinbergen, J. (1962). *Shaping the World Economy*. Twentieth Century Fund; Anderson, J. E., & van Wincoop, E. (2003). *Gravity with gravitas*. American Economic Review, 93(1), 170–192.
* **Governing Equations**:
  $$\Phi_{ij} = G \frac{Y_i Y_j}{D_{ij}^\gamma} \left(\frac{\tau_{ij}}{\Pi_i P_j}\right)^{1 - \sigma}$$
* **Computational Complexity**: $O(\text{candidate links})$.

#### 99. Preston Curve & Clean-Water Epidemiological Transition (`PublicSanitationWaterGridEngine`)
* **Model Classification**: Tier 2 - Type B (Demographic & Public Health Transition)
* **Academic Literature Reference**: Omran, A. R. (1971). *The epidemiological transition*. Milbank Mem. Fund Q., 49(4), 509–538; Preston, S. H. (1975). *The changing relation between mortality and level of economic development*. Population Studies, 29(2), 231–248; Cutler, D., & Miller, G. (2005). *The role of public health improvements in health advances*. Demography, 42(1), 1–22.
* **Governing Equations**:
  $$e_0(y) = e_{0,\min} + \beta_P \ln\!\left(1 + \frac{y}{y_0}\right), \qquad \ln m_i = \alpha_i - \gamma_{\text{water}} \, \mathbb{I}_{\text{clean},i}$$
* **Parameters & Physical Units**:
  * $e_0(y)$: Concave Preston (1975) life expectancy at birth [$\text{yr}$] as a function of real per-capita material provision $y$.
  * $\gamma_{\text{water}} > 0$: Log-mortality reduction from municipal clean-water filtration and chlorination (Cutler & Miller 2005).
* **Computational Complexity**: $O(1)$ per cell.

#### 100. Book Production & Human Capital Accumulation (`PhysicalMediaPrintingEngine`)
* **Model Classification**: Tier 2 - Type B (Economic History of Knowledge)
* **Academic Literature Reference**: Buringh, E., & van Zanden, J. L. (2009). *Charting the "Rise of the West": Manuscripts and printed books in Europe, a long-term perspective*. Journal of Economic History, 69(2), 409–445; Dittmar, J. E. (2011). *Information technology and economic change: the impact of the printing press*. Quarterly Journal of Economics, 126(3), 1133–1172.
* **Governing Equations**:
  $$\frac{dB_{\text{books}}}{dt} = Q_{\text{print}} - \delta_b B_{\text{books}}, \qquad \ln Q_{\text{books},i} = \alpha_0 + \alpha_1 \ln N_i + \alpha_2 \ln\left(\frac{1}{P_{\text{book}}}\right)$$
* **Computational Complexity**: $O(1)$ per cell.

#### 101. Domar-Bohn Sovereign Debt Sustainability Dynamics (`SovereignTreasuryBondEngine`)
* **Model Classification**: Tier 2 - Type B (Fiscal Macroeconomics)
* **Academic Literature Reference**: Domar, E. D. (1944). *The "burden of the debt" and the national income*. American Economic Review, 34(4), 798–827; Bohn, H. (1998). *The behavior of U.S. public debt and deficits*. Quarterly Journal of Economics, 113(3), 949–963; Reinhart, C. M., & Rogoff, K. S. (2009). *This Time Is Different*. Princeton University Press.
* **Governing Equations**:
  $$\frac{dd}{dt} = (r - g) \, d + p_{\text{def}}, \qquad d = \frac{D_{\text{sovereign}}}{Y}$$
* **Parameters & Physical Units**:
  * $d$: Sovereign debt-to-output ratio [dimensionless]; $r$: effective interest rate on public debt [$\text{yr}^{-1}$]; $g$: real output growth rate [$\text{yr}^{-1}$]; $p_{\text{def}} = (G - T)/Y$: primary fiscal deficit ratio.
* **Computational Complexity**: $O(1)$ per polity.

#### 102. Froude Ship Resistance & Maritime Freight Economics (`NavalHydrodynamicsFreightEngine`)
* **Model Classification**: Tier 2 - Type B (Transport Physics & Economic History)
* **Academic Literature Reference**: Froude, W. (1874). *On Experiments with H.M.S. Greyhound*. Trans. Inst. Naval Architects, 15, 36–73; Harley, C. K. (1988). *Ocean freight rates and new world expansion, 1750–1913*. Journal of Economic History, 48(4), 851–876.
* **Governing Equations**:
  $$R_T = \frac{1}{2} \rho_w S v^2 \left( C_F(\text{Re}) + C_R(\text{Fn}) \right), \qquad \text{Re} = \frac{v L}{\nu}, \qquad \text{Fn} = \frac{v}{\sqrt{g L}}$$
* **Parameters & Physical Units**:
  * $R_T$: Total hull hydrodynamic drag [$\text{N}$] decomposed into viscous skin-friction coefficient $C_F(\text{Re})$ and wave-making residuary coefficient $C_R(\text{Fn})$ over wetted surface area $S$ [$\text{m}^2$] and waterline length $L$ [$\text{m}$].
* **Computational Complexity**: $O(1)$ per maritime corridor.

#### 103. Von Thünen Locational Land Rent & Hinterland Grain Transport (`VonThunenAgriculturalRingsEngine`)
* **Model Classification**: Tier 2 - Type B (Spatial Agricultural Economics)
* **Academic Literature Reference**: von Thünen, J. H. (1826). *Der isolierte Staat in Beziehung auf Landwirtschaft und Nationalökonomie*. Perthes; Chisholm, M. (1962). *Rural Settlement and Land Use*. Hutchinson; Bairoch, P. (1988). *Cities and Economic Development*. University of Chicago Press.
* **Governing Equations**:
  $$R(r) = Y (p - c - f \cdot r), \qquad r_{\max} = \frac{p - c}{f}$$
  $$L_{\text{disk}}(c R) = 1 - \frac{1}{\pi R^2} \int_0^R e^{-c r} \, 2\pi r \, dr = 1 - \frac{2}{(c R)^2}\left[1 - (1 + c R) e^{-c R}\right]$$
* **Parameters & Physical Units**:
  * $R(r)$: Locational land rent per unit area at distance $r$ [$\text{km}$] from market with yield $Y$, market price $p$, production cost $c$, and freight rate $f$.
  * $L_{\text{disk}}(c R) \in [0, 1)$: Exact disk-integrated share of harvested grain consumed in transit over circular supply ring radius $R = \sqrt{N e_{\text{meta}} / (\pi y_{\text{agr}})}$ at modal energy-loss rate $c$ [$\text{km}^{-1}$] ($\approx 0.008\,\text{km}^{-1}$ ox-wagon vs. $\approx 0.0008\,\text{km}^{-1}$ waterway barge).
* **Computational Complexity**: $O(1)$ per cell.

#### 104. Ricardian Differential Land Rent & Stationary Profit Squeeze (`RicardoDifferentialLandRentEngine`)
* **Model Classification**: Tier 2 - Type B (Classical Political Economy)
* **Academic Literature Reference**: Ricardo, D. (1817). *On the Principles of Political Economy and Taxation*. John Murray; Samuelson, P. A. (1959). *A modern treatment of the Ricardian economy*. Quarterly Journal of Economics, 73(1), 1–35.
* **Governing Equations**:
  $$Y_A = A_0 \, T_{\text{land}}^{1 - \beta_A} L^{\beta_A}, \qquad MP_L = \frac{\partial Y_A}{\partial L} = \beta_A \frac{Y_A}{L}, \qquad R_{\text{land}} = Y_A - L \cdot MP_L = (1 - \beta_A) Y_A$$
  $$r_{\text{profit}} = r_0 \left(1 - \frac{w_{\text{sub}}}{MP_L}\right)$$
* **Parameters & Physical Units**:
  * $MP_L$: Marginal product of labor on the agricultural extensive/intensive margin ($0 < \beta_A < 1$).
  * $R_{\text{land}}$: Differential rent accruing to landowners; as population growth drives $MP_L \to w_{\text{sub}}$, capitalist profit $r_{\text{profit}} \to 0$ (Ricardian stationary state).
* **Computational Complexity**: $O(1)$ per cell.

#### 105. Lewis & Ranis-Fei Dual-Sector Structural Transformation (`LewisDualSectorTurningPointEngine`)
* **Model Classification**: Tier 2 - Type B (Developmental Macroeconomics)
* **Academic Literature Reference**: Lewis, W. A. (1954). *Economic Development with Unlimited Supplies of Labour*. The Manchester School, 22(2), 139–191; Ranis, G., & Fei, J. C. H. (1961). *A Theory of Economic Development*. American Economic Review, 51(4), 533–565.
* **Governing Equations**:
  $$w = \max\!\left(\bar{w}_S, \; MP_{L,A}\right), \qquad MP_{L,A} = \frac{\partial Y_A}{\partial L_A}, \qquad \frac{dK_M}{dt} = s_\pi \left(Y_M - w \, L_M\right)$$
* **Parameters & Physical Units**:
  * $\bar{w}_S$: Institutional subsistence wage during the unlimited rural labor phase ($MP_{L,A} < \bar{w}_S$).
  * Commercialization / Lewis Turning Point ($MP_{L,A} = \bar{w}_S$): Rural labor surplus is exhausted, real wages rise with marginal productivity ($w = MP_{L,A}$), and modern producers substitute capital machinery $K_M$ for labor.
* **Computational Complexity**: $O(1)$ per cell.

#### 106. Kondratiev-Schumpeter Long Waves & Vintage Creative Destruction (`KondratievSchumpeterLongWaveEngine`)
* **Model Classification**: Tier 2 - Type B (Macroeconomic Cycles)
* **Academic Literature Reference**: Kondratiev, N. D. (1925). *The Major Economic Cycles*. Voprosy Konyunktury, 1(1), 28–79; Schumpeter, J. A. (1939). *Business Cycles*. McGraw-Hill.
* **Governing Equations**:
  $$I(t) = I_0 \left[1 + \alpha_K \sin\!\left(\frac{2\pi (t - t_0)}{T_K}\right)\right], \qquad \frac{dK}{dt} = I(t) - \delta(t) K$$
* **Parameters & Physical Units**:
  * $T_K \approx 48\text{–}60\,\text{yr}$: Characteristic replacement period of durable transport and power infrastructures ($52\,\text{yr}$ canonical period), with accelerated scrapping $\delta(t)$ during Schumpeterian creative-destruction troughs.
* **Computational Complexity**: $O(1)$ per cell.

#### 107. Pomeranz-Wrigley Subterranean Forest & Colonial Ghost Acreage (`PomeranzGreatDivergenceEngine`)
* **Model Classification**: Tier 2 - Type B (Comparative Economic History)
* **Academic Literature Reference**: Pomeranz, K. (2000). *The Great Divergence: China, Europe, and the Making of the Modern World Economy*. Princeton University Press; Wrigley, E. A. (2010). *Energy and the English Industrial Revolution*. Cambridge University Press.
* **Governing Equations**:
  $$A_{\text{ghost,coal}} = \frac{E_{\text{coal}}}{y_{\text{wood}}}, \qquad A_{\text{ghost,trade}} = \sum_c \frac{M_{\text{import},c}}{y_{\text{land},c}}, \qquad A_{\text{eff}} = A_{\text{dom}} + A_{\text{ghost,coal}} + A_{\text{ghost,trade}}$$
* **Parameters & Physical Units**:
  * $A_{\text{ghost,coal}}$: "Subterranean forest" land-area equivalent [$\text{km}^2$] liberated by substituting coal enthalpy $E_{\text{coal}}$ for sustainable coppice forestry yield $y_{\text{wood}}$ (Wrigley 2010).
  * $A_{\text{ghost,trade}}$: Transoceanic colonial ghost acreage [$\text{km}^2$] embodied in imported land-intensive commodities $M_{\text{import},c}$ (timber, cotton, sugar, grain; Pomeranz 2000).
* **Computational Complexity**: $O(1)$ per cell.

#### 108. Bueno de Mesquita Expected-Utility War Trap & Contest Success (`WarDiplomacyEngine`)
* **Model Classification**: Tier 2 - Type B (Geopolitical Conflict Theory)
* **Academic Literature Reference**: Bueno de Mesquita, B. (1981). *The War Trap*. Yale University Press; Hirshleifer, J. (1989). *Conflict and rent-seeking success functions: Ratio vs. difference models of relative success*. Public Choice, 63(2), 101–112; Lanchester, F. W. (1916).
* **Governing Equations**:
  $$E(U_{i \to j}) = p_{ij} \, U_i(\Delta V_j) + (1 - p_{ij}) \, U_i(-\Delta L_i) - C_i - U_i(Q_{ij}), \qquad p_{ij} = \frac{\Phi_i^m}{\Phi_i^m + \Phi_j^m} \quad (m = 2)$$
* **Parameters & Physical Units**:
  * $E(U_{i \to j})$: Net expected utility of initiating conflict against polity $j$ relative to status quo $Q_{ij}$ (Bueno de Mesquita 1981).
  * $p_{ij} \in (0, 1)$: Tullock-Hirshleifer-Lanchester contest success probability with Square-Law decisiveness exponent $m = 2$ over military capabilities $\Phi_i, \Phi_j$.
* **Computational Complexity**: $O(P^2)$ across $P$ active polities.

---
## 5. Computational Complexity & Integration Benchmark Matrix

| Subsystem Classification | Temporal Tier | Time Step ($\Delta t$) | Computational Complexity Per Cell | Typical Execution Time (175k Cells) |
|---|---|---|---|---|
| **Flux & Logistics (`FluxKernel`)** | Tier 1 | Daily ($\Delta t = 1\text{d}$) | $O(6)$ neighbor volume balance | $3.2\text{ ms}$ |
| **Wet-Bulb Temperature (`WetBulbTemperatureEngine`)** | Tier 1 | Daily ($\Delta t = 1\text{d}$) | Analytical polynomial $O(1)$ | $0.4\text{ ms}$ |
| **Atmospheric Circulation (`AtmosphericCirculationHadleyEngine`)** | Tier 1 | Daily ($\Delta t = 1\text{d}$) | Geostrophic balance $O(1)$ | $0.6\text{ ms}$ |
| **Epidemiological Diffusion (`SpatialMetapopulationSEIREngine`)** | Tier 1 | Daily ($\Delta t = 1\text{d}$) | Sparse active set $O(K_{\text{active}})$ | $0.8\text{ ms}$ |
| **Milankovitch Insolation (`MilankovitchOrbitalEngine`)** | Tier 2 | Monthly ($\Delta t = 30\text{d}$) | Keplerian orbital matrix $O(1)$ | $0.5\text{ ms}$ |
| **2D Darcy Aquifer (`AquiferDepletionEngine`)** | Tier 2 | Monthly ($\Delta t = 30\text{d}$) | 2D stencil diffusion $O(6)$ | $2.1\text{ ms}$ |
| **Glacial Thermodynamic Melt (`GlacialThermodynamicMeltEngine`)** | Tier 2 | Monthly ($\Delta t = 30\text{d}$) | PDD enthalpy balance $O(1)$ | $0.3\text{ ms}$ |
| **Soil N-P-K & van Genuchten** | Tier 2 | Monthly ($\Delta t = 30\text{d}$) | Soil retention closed form $O(1)$ | $1.2\text{ ms}$ |
| **Demographics & Senescence (`BiologicalDemographicsEngine`)** | Tier 2 | Monthly ($\Delta t = 30\text{d}$) | Gompertz-Makeham cohorts $O(1)$ | $1.8\text{ ms}$ |
| **Smil Infrastructure Turnover (`SmilMaterialTransitionsPureEngine`)** | Tier 3 | Annual ($\Delta t = 365\text{d}$) | Exponential turnover $O(1)$ | $0.1\text{ ms}$ |
| **Ore Grade Exergy Depletion (`OreGradeThermodynamicsEngine`)** | Tier 3 | Annual ($\Delta t = 365\text{d}$) | Hyperbolic exergy work $O(1)$ | $0.1\text{ ms}$ |
| **Viscoelastic GIA (`GlacialIsostaticAdjustmentEngine`)** | Tier 3 | Annual ($\Delta t = 365\text{d}$) | Relaxation PDE $O(1)$ | $0.4\text{ ms}$ |

---

## 6. Spatiotemporal Cartographic Tensors & Cultural Affinity Matrix

### 6.1 Multi-Channel Cartographic Tensor Architecture (2048 x 1024 Equirectangular)
The simulation state at $T_0$ is initialized from a coupled tensor stack following a strict two-format standard:
1. **24-bit Categorical & Polychrome RGB Entity Tensors (`TYPE_INT_RGB`)**:
   - **Biomes (`biomes.png`)**: $\text{RGB}(R,G,B) \leftrightarrow 14\text{ Holdridge-Whittaker Bioclimatic Classes}$.
   - **Tensor 0 (Isoglosses / Languages)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Language Family / Glottolog Clade ID}$.
   - **Tensor 1 (Kinship & Clan Structures)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Lineage System (Patrilineal, Matrilineal, Bilateral Foraging Band)}$.
   - **Tensor 2 (Rituals / Sacred Geography)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Religious & Ritual Tradition Cluster}$.
   - **Tensor 3 (Politico-Military Sovereignty & Polities)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Polity ID / Sovereign Capital Jurisdiction}$.
   - **Tensor 5 (Trade Corridors & Hubs)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Overland, Maritime & Riverine Conductance Arteries}$.
2. **16-bit Continuous Intensity Grayscale Tensors (`TYPE_USHORT_GRAY`, $[0, 65535]$, 65,536 levels)**:
   - **Elevation & Bathymetry (`elevation.png`)**: $[-11\,000\text{ m}, +8\,848\text{ m}]$ with sea-level ($0\text{ m}$) anchored at normalized datum $0.478$ ($u_{16} = 31\,326$, vertical resolution $\approx 0.26\text{–}0.35\text{ m}$).
   - **Climate Scalars (`temperature.png`, `precipitation.png`, `seasonality.png`)**: Surface temperature ($[-50\,^\circ\text{C}, +50\,^\circ\text{C}]$, resolution $\approx 0.0015\,^\circ\text{C}$), annual precipitation ($[0, 4\,000\text{ mm/yr}]$, square-root encoded), and seasonal thermal amplitude ($[1, 61\,^\circ\text{C}]$).
   - **Demographic Density (`density.png`)**: Bijective 16-bit logarithmic areal population density encoding conserving population integrals $N_{\text{cell}} = \iint_{\Omega_{\text{cell}}} \rho(\mathbf{x}) \, dA$ over spherical surface element $dA = R^2 \cos\phi \, d\phi \, d\lambda$:
     $$u_{16}(\rho) = \operatorname{round}\!\left(65\,535 \cdot \frac{\ln(1 + \rho / d_0)}{\ln(1 + D_{\text{ref}} / d_0)}\right), \qquad \rho(u_{16}) = d_0 \left[\left(1 + \frac{D_{\text{ref}}}{d_0}\right)^{u_{16} / 65\,535} - 1\right]$$
     with $d_0 = 0.1\,\text{hab/km}^2$, $D_{\text{ref}} = 25\,000\,\text{hab/km}^2$, and exact uninhabited identity $u_{16}(0) \equiv 0 \iff \rho(0) \equiv 0.0$.
   - **Tensor 4 (Materiality & Technologies)**: $\tau(\mathbf{x}) \in [0, 1]$ ($u_{16} / 65535$) lithic / metallurgical complexity.
   - **Tensor 6 (Institutional Complexity)**: $\mathcal{I}(\mathbf{x}) \in [0, 1]$ ($u_{16} / 65535$) legal codification and administrative depth (Seshat).
   - **Tensor 7 (Ecological Footprint & Degradation)**: $D(\mathbf{x}) \in [0, 1]$ ($u_{16} / 65535$) HYDE 3.4 agro-pastoral land-use strain, soil salinization, and deforestation.
   - **Tensor 8 (Epidemiological Pathogen Immunity)**: $P(\mathbf{x}) \in [0, 1]$ ($u_{16} / 65535$) endemic vector burden and acquired immunity.
   - **10 Geological & Hydrological Resource Tensors**: Coal, Oil, Gas, Uranium, He-3, Iron/Copper, Precious Metals, Rare Earths, Geothermal Heat, and Aquifers encoded in 16-bit grayscale ($[0, 65535]$).

### 6.2 Cultural Distance & Intergroup Contact Theory ($N \times N$)
* **Academic Literature Reference**: Shepard, R. N. (1987). *Toward a universal law of generalization for psychological science*. Science, 237(4820), 1317–1323; Axelrod, R. (1997). *The dissemination of culture*. Journal of Conflict Resolution, 41(2), 203–226; Allport, G. W. (1954). *The Nature of Prejudice*. Addison-Wesley; Cavalli-Sforza, L. L., & Feldman, M. W. (1981).
* **Governing Equations**:
  $$D_{ij} = \left( \sum_{k=1}^{K} w_k |T_{i,k} - T_{j,k}|^2 \right)^{1/2}, \qquad A_0(i, j) = \exp(-\lambda D_{ij}), \qquad \frac{dA_{ij}}{dt} = \kappa_+ \Phi_{\text{contact},ij}(1 - A_{ij}) - \kappa_- C_{\text{conflict},ij} A_{ij}$$
* **Parameters & Physical Units**:
  * $\mathbf{T}_i \in [0, 1]^K$: Ethnographic and institutional trait vector (language family, kinship structure, subsistence mode, ritual norm).
  * $A_0(i, j) \in (0, 1]$: Shepard exponential similarity generalization law ($A_0(i, i) = 1$).
  * $\Phi_{\text{contact},ij}, C_{\text{conflict},ij}$: Peaceful trade contact vs. active inter-polity conflict intensity (Allport 1954; Axelrod 1997). Asymmetric prestige/dogma priors and defensive war grievance closures are documented in `docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md` (Entry 24).

---

## 7. High-Performance Computing (HPC) & Data-Oriented Design (DOD)

To sustain real-time execution across $175,000+$ H3 hexagonal cells without garbage collection pauses, Ether enforces:
1. **Struct-of-Arrays (SoA) Buffers**: Primitive arrays (`long[] population`, `float[] elevation`, `float[] availableWater`, `float[] capitalStock`) aligned to 64-byte cache lines.
2. **Hilbert Curve Cell Ordering**: Cells sorted along a 2D space-filling Hilbert curve ensuring $94\%$ L1/L2 CPU cache hit rates during spatial stencil evaluations.
3. **AVX-512 Vectorized Kinetics**: SIMD environmental and metabolic updates computing 8 or 16 cells per instruction cycle.
4. **Deterministic Stateless PRNG**: Spatial stochastic events evaluated via `DeterministicPRNG.splitMix64(seed, tick, cellIndex, streamId)`, guaranteeing bit-identical determinism across multi-threaded runs.

---
## 8. Canonical Macro-Observables & Published Diagnostic Equations

Runtime simulation kernels integrate only conserved physical, chemical, thermodynamic, and demographic state variables. Post-hoc planetary and regional diagnostics derive from canonical published laws below; any macroscopic closure or $\Phi_{\text{exergy}}$ parameterization required for discrete H3 meshes is documented separately in [`docs/PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md`](PARAMETRIC_DEVIATIONS_AND_MODEL_CALIBRATION_REGISTRY.md).

### 8.1. Energetic, Thermodynamic & Planetary Power Laws

#### 1. Garrett Thermodynamic Wealth-Power Relation
* **Academic Literature Reference**: Garrett, T. J. (2011). *Are there basic physical constraints on future anthropogenic emissions of carbon dioxide?* Climatic Change, 104(3), 437-455; Lotka, A. J. (1922). *Contribution to the energetics of evolution*. PNAS, 8(6), 147-151.
* **Governing Equation**:
  $$P(t) = \lambda_G \, W(t), \qquad W(t) = \int_{-\infty}^{t} Y_{\text{net}}(\tau) \, d\tau$$
* **Parameters & Physical Units**:
  * $P(t)$: Primary civilization power dissipation [$\text{W}$].
  * $W(t)$: Cumulative active physical wealth stock; $\lambda_G$: thermodynamic dissipation constant linking maintained material structure to continuous power consumption.

#### 2. Kardashev-Sagan Planetary Power Scale ($K_{\text{Sagan}}$)
* **Academic Literature Reference**: Kardashev, N. S. (1964). *Transmission of information by extraterrestrial civilizations*. Soviet Astronomy, 8, 217-221; Sagan, C. (1973). *The Cosmic Connection*. Doubleday.
* **Governing Equation**:
  $$K_{\text{Sagan}} = \frac{\log_{10}(P_{\text{total}}) - 6}{10}$$
* **Parameters & Physical Units**:
  * $P_{\text{total}} = \sum_i P_i$: Total planetary civilization power dissipation [$\text{W}$] ($P_{\text{total}} = 10^{16}\,\text{W} \iff K_{\text{Sagan}} = 1.0$).

#### 3. Marchetti-Nakicenovic Logistic Primary Energy Substitution
* **Academic Literature Reference**: Fisher, J. C., & Pry, R. H. (1971). *A simple substitution model of technological change*. Technological Forecasting and Social Change, 3, 75-88; Marchetti, C. (1977). *Primary energy substitution models*. Technological Forecasting and Social Change, 10(4), 345-356; Smil, V. (2017).
* **Governing Equation**:
  $$\ln\!\left(\frac{f_k(t)}{1 - f_k(t)}\right) = \alpha_k (t - t_{0,k}), \qquad \sum_k f_k(t) = 1$$
* **Parameters & Physical Units**:
  * $f_k \in (0, 1)$: Fractional market share of primary energy carrier $k$ (biomass, coal, oil, gas, nuclear, renewables).

#### 4. Agro-Ecological Energy Return on Investment ($\text{EROEI}_{\text{agr}}$)
* **Academic Literature Reference**: Leach, G. (1976). *Energy and Food Production*. IPC Science and Technology Press; Smil, V. (2008). *Energy in Nature and Society*. MIT Press.
* **Governing Equation**:
  $$\text{EROEI}_{\text{agr}} = \frac{E_{\text{edible\_harvest}}}{E_{\text{ag\_input}}}$$
* **Parameters & Physical Units**:
  * Dimensionless ratio of harvested metabolizable food energy [$\text{J}$] to direct and embodied agronomic energy inputs [$\text{J}$].

### 8.2. Demographic, Inequality & Fiscal Accounting Identities

#### 5. Total Population & Urban Nucleation Share ($N_{\text{world}}, U$)
* **Academic Literature Reference**: Bairoch, P. (1988). *Cities and Economic Development*. University of Chicago Press; de Vries, J. (1984). *European Urbanization 1500-1800*. Harvard University Press.
* **Governing Equations**:
  $$N_{\text{world}} = \sum_{i} \sum_{c=1}^{7} N_{i,c}, \qquad U = \frac{\sum_{i} N_i \, \mathbb{I}(N_{\text{cluster},i} \ge N_{\text{urb}})}{\sum_{i} N_i}$$
* **Parameters & Physical Units**:
  * $N_{\text{urb}} = 5\,000\,\text{inhabitants}$: Bairoch (1988) historical urban settlement threshold.

#### 6. Lorenz Curve & Gini Coefficient ($G$)
* **Academic Literature Reference**: Gini, C. (1912). *Variabilita e mutabilita*; Milanovic, B., Lindert, P. H., & Williamson, J. G. (2011). *Pre-Industrial Inequality*. Economic Journal, 121(551), 255-272.
* **Governing Equation**:
  $$G = \frac{\sum_{i=1}^{n} \sum_{j=1}^{n} w_i w_j |y_i - y_j|}{2 \, \bar{y} \left(\sum_{i=1}^{n} w_i\right)^2} = 1 - 2 \int_0^1 L(p) \, dp$$
* **Parameters & Physical Units**:
  * $L(p)$: Cumulative share of wealth or income held by the bottom $p \in [0, 1]$ population fraction; $G \in [0, 1]$.

#### 7. Allen Welfare Ratio (Real Unskilled Wage)
* **Academic Literature Reference**: Allen, R. C. (2001). *The great divergence in European wages and prices from the Middle Ages to the First World War*. Explorations in Economic History, 38(4), 411-447.
* **Governing Equation**:
  $$WR = \frac{w_{\text{annual}}}{n_{\text{hh}} \cdot P_{\text{basket}}}$$
* **Parameters & Physical Units**:
  * $WR$: Dimensionless household subsistence welfare ratio of annual unskilled laborer earnings $w_{\text{annual}}$ to the cost of $n_{\text{hh}}$ physiological subsistence baskets $P_{\text{basket}}$.

#### 8. Seigniorage & Bullion Debasement Accounting
* **Academic Literature Reference**: Cipolla, C. M. (1963). *Currency depreciation in medieval Europe*. Economic History Review, 15(3), 413-422; Sargent, T. J., & Velde, F. R. (2002). *The Big Problem of Small Change*. Princeton University Press; Butcher, K., & Ponting, M. (2014).
* **Governing Equation**:
  $$S_{\text{seign}} = M_{\text{mint}} \left(\frac{1}{\phi_{\text{fine}}} - \frac{1}{\phi_0}\right) - C_{\text{brassage}}$$
* **Parameters & Physical Units**:
  * $\phi_{\text{fine}} \in (0, 1]$: Precious metal fineness fraction of minted coinage relative to par standard $\phi_0$.

### 8.3. Atmospheric Carbon Mass Balance, Radiative Forcing & Geophysical Decay

#### 9. Airborne Anthropogenic Carbon Mass Balance & Myhre Forcing
* **Academic Literature Reference**: Myhre, G., et al. (1998). *New estimates of radiative forcing due to well mixed greenhouse gases*. Geophysical Research Letters, 25(14), 2715-2718; Ballantyne, A. P., et al. (2012). *Audit of the global carbon budget: estimate errors and their impact on uptake uncertainty*. Biogeosciences, 9, 2585-2607; IPCC AR6 WG1 (2021).
* **Governing Equations**:
  $$\frac{dC_{\text{CO}_2}}{dt} = \frac{f_{\text{airborne}} \cdot \dot{M}_{\text{C,anth}}}{\kappa_{\text{GtC/ppm}}}, \qquad \Delta F_{\text{CO}_2} = 5.35 \ln\!\left(\frac{C_{\text{CO}_2}}{C_{\text{CO}_2,0}}\right), \qquad \Delta T_{\text{eq}} = \lambda_{\text{ECS}} \, \Delta F_{\text{CO}_2}$$
* **Parameters & Physical Units**:
  * $\dot{M}_{\text{C,anth}}$: Anthropogenic fossil + land-use carbon emission flux [$\text{GtC/yr}$].
  * $f_{\text{airborne}} \approx 0.44$: Mean airborne retention fraction (Ballantyne et al. 2012).
  * $\kappa_{\text{GtC/ppm}} = 2.124\,\text{GtC/ppm}$: Atmospheric carbon conversion mass constant.
  * $\lambda_{\text{ECS}} \approx 0.80\,\text{K/(W}\cdot\text{m}^{-2}\text{)}$: Planck + feedback climate sensitivity parameter ($\sim 3.0\,\text{K}$ per $\text{CO}_2$ doubling).

#### 10. Beer-Lambert Stratospheric Volcanic Aerosol Attenuation & Radioactive Decay
* **Academic Literature Reference**: Robock, A. (2000). *Volcanic eruptions and climate*. Reviews of Geophysics, 38(2), 191-219; Rutherford, E., & Soddy, F. (1902). *The cause and nature of radioactivity*; Steinhauser, G., Brandl, A., & Johnson, T. E. (2014). *Comparison of the Chernobyl and Fukushima nuclear accidents*. Science of the Total Environment, 470, 800-817.
* **Governing Equations**:
  $$I_{\text{TOA,eff}}(t) = I_0 \exp(-\tau_{\text{aer}}(t)), \qquad \frac{d\tau_{\text{aer}}}{dt} = -\frac{\tau_{\text{aer}}}{\tau_{\text{washout}}}$$
  $$A(t) = \sum_k A_{k,0} \exp(-\lambda_k t), \qquad \lambda_k = \frac{\ln 2}{T_{1/2,k}}$$
* **Parameters & Physical Units**:
  * $\tau_{\text{aer}}$: Stratospheric sulfate/soot optical depth [dimensionless] with e-folding residence time $\tau_{\text{washout}} \approx 1\text{-}2\,\text{yr}$ (Robock 2000).
  * $T_{1/2,k}$: Radionuclide half-life ($^{137}\text{Cs}$: $30.17\,\text{yr}$; $^{90}\text{Sr}$: $28.79\,\text{yr}$; $^{131}\text{I}$: $8.02\,\text{d}$).
