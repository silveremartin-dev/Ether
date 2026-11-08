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
  - Resolves symmetric finite-volume mass and logistics fluxes under Courant-Friedrichs-Lewy (CFL) limits (`FluxEngine`).
  - Evaluates instantaneous wet-bulb temperature and hyperthermia thresholds (`WetBulbTemperatureEngine`).
  - Computes zonal wind vectors under Coriolis parameter $f = 2\Omega \sin\phi$ (`AtmosphericCirculationHadleyEngine`).
  - Resolves active localized epidemiological transmissions via sparse active sets (`SpatialMetapopulationSEIREngine`).
  - Updates tactical political borders, legitimacy pressure, and military maneuvers (`PoliticalSimulationEngine`).

- **Tier 2  -  Monthly Biophysical Sub-Step Scale ($\Delta t_{\text{monthly}} = 30 \text{ days} \approx 2.592 \times 10^6 \text{ s}$)**:
  - Updates Milankovitch astronomical orbital parameters ($e$, $\varepsilon$, $\varpi$) and TOA solar insolation (`MilankovitchOrbitalEngine`).
  - Solves 2D lateral piezometric aquifer diffusion (Darcy flux in `AquiferDepletionEngine`).
  - Integrates cryospheric thermodynamic melt ($L_f = 333.55\text{ kJ/kg}$) and eustatic sea level adjustments (`GlacialThermodynamicMeltEngine`).
  - Resolves van Genuchten soil water retention, N-P-K soil nutrient depletion, and Stokes siltation kinetics.
  - Integrates Farquhar FvCB photosynthesis, Beer-Lambert light interception, and soil organic carbon decay.
  - Updates cohort demographic aging, Gompertz-Makeham senescence, and Kimura genetic drift.
  - Evaluates registered Type B cliodynamic plugins (`TypeBEnginePassRunner`).

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
  * $\varpi(t)$: Longitude of perihelion [degrees]; climatic precession index $e \sin\varpi$ (23,000-year cycle).
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

#### 3. Clausius-Clapeyron Atmospheric Moisture Capacity (`AtmosphericMoistureEngine`)
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
* **Parameters & Physical Units**:
  * $\Omega = 7.292115 \times 10^{-5}\text{ rad/s}$: Earth sidereal rotational angular velocity.
  * $f$: Coriolis parameter [$\text{s}^{-1}$].
  * $\rho_{\text{air}}$: Atmospheric density [$\text{kg/m}^3$] ($\approx 1.225\text{ kg/m}^3$ at sea level).
  * $P$: Atmospheric pressure field [$\text{Pa}$].
  * $\mathbf{u} = (u, v)$: Horizontal wind velocity vector [$\text{m/s}$], resolving trade winds ($[0^\circ, 30^\circ]$), westerlies ($[30^\circ, 60^\circ]$), and polar easterlies ($[60^\circ, 90^\circ]$).
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
  $$q_{\text{AMOC}} = \operatorname{softplus}\left( C \cdot \left[ \alpha_T \cdot \eta_{\text{gyre}} (T_e - T_p) - \beta_S (S_e - S_p) \right], \; 1.5 \right)$$
  $$\frac{dT_p}{dt} = \frac{T_p^* - T_p}{\tau_T}$$
  $$\frac{dS_p}{dt} = \frac{\max(0, q_{\text{AMOC}})}{V_p} (S_e - S_p) - \frac{\left(F_0 \frac{\max(0, q)}{q_0} + F_{\text{melt}}\right) S_0}{V_p} + \frac{S_p^* - S_p}{\tau_{\text{diff}}}$$
  $$\Delta T_{\text{North}} = -8.0 \cdot \frac{1}{1 + \exp\left( \frac{q_{\text{AMOC}} - 8.0}{1.5} \right)} \qquad \Delta T_{\text{South}} = -0.22 \cdot \Delta T_{\text{North}}$$
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

#### 19. Dust Storm Aerosol Mobilization & Transport (`DustStormEngine`)
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

#### 25. Kleiber Allometry, Primiparity & Gompertz-Makeham Senescence (`DemographicKernel`, `BiologicalDemographicsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Kleiber, M. (1932). *Body size and metabolism*. Hilgardia, 6(11), 315-353; Gompertz, B. (1825). *On the nature of the function expressive of the law of human mortality*. Philosophical Transactions of the Royal Society, 115, 513-583; Makeham, W. M. (1860). *On the law of mortality*. Journal of the Institute of Actuaries, 8(6), 301-310.
* **Governing Equations**:
  $$B_{\text{basal}} = B_0 M_{\text{body}}^{3/4}, \quad E_{\text{metabolic}} = 3.362\text{ GJ/(person}\cdot\text{year)}$$
  $$\mu(a) = \alpha_{\text{makeham}} + \beta_{\text{gompertz}} \cdot \exp(\gamma_{\text{age}} a) + \mu_{\text{starvation}}(\text{Food})$$
  $$a_{\text{primiparity}} = 14.0 + 10.0 \cdot (1.0 - C_{\text{kinship}}) + 6.0 \cdot \tanh\left(\frac{k_{\text{pc}}}{800.0}\right)$$
* **Parameters & Physical Units**:
  * $a$: Cohort age [$\text{years}$].
  * $\mu(a)$: Age-specific mortality hazard rate [$\text{year}^{-1}$].
  * $\alpha_{\text{makeham}} = 0.001\text{ yr}^{-1}$: Age-independent background accidental mortality.
  * $\beta_{\text{gompertz}} = 5.0 \times 10^{-5}\text{ yr}^{-1}$: Initial biological vulnerability.
  * $\gamma_{\text{age}} = 0.085\text{ yr}^{-1}$: Actuarial aging rate doubling every $\sim 8.1$ years.
  * $a_{\text{primiparity}}$: Age at first childbearing [$\text{years}$].
  * $E_{\text{reproduction}} = 0.80\text{ GJ/birth}$: Maternal gestation and lactation bioenergetic cost.
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
* **Academic Literature Reference**: Bergmann, C. (1847). *Ueber die Verhältnisse der Wärmeökonomie der Thiere zu ihrer Grösse*. Göttinger Studien, 3(1), 595-708; Allen, J. A. (1877). *The influence of Physical conditions in the genesis of species*. Radical Review, 1, 108-140; Ruff, C. B. (1994). *Morphological adaptation to climate in modern and fossil hominids*. American Journal of Physical Anthropology, 37(S19), 65-107; Foster, J. B. (1964). *Evolution of mammals on islands*. Nature, 202(4929), 234-235.
* **Governing Equations**:
  $$M^*(T) = M_{\text{ref}} \cdot \left[1.0 + \beta_{\text{bergmann}} \cdot \tanh\left(\frac{T_{\text{ref}} - T}{s_T}\right)\right] \quad [\text{tons}]$$
  $$\frac{SA}{M} = c_{\text{geom}} \cdot M_{\text{kg}}^{-1/3} \quad [\text{m}^2/\text{kg}]$$
  $$[\dot{p}_M](T, M) = [\dot{p}_{M0}] \cdot \left[1.0 + \gamma_{\text{thermo}} \cdot \frac{SA}{M} \cdot \max(0, T_{\text{comfort}} - T)\right]$$
* **Parameters & Physical Units**:
  * $M_{\text{ref}} = 0.050\text{ tons}$ ($50.0\text{ kg}$): Reference human structural lean mass.
  * $T_{\text{ref}} = 15.0^\circ\text{C}$, $s_T = 18.0^\circ\text{C}$: Baseline temperature and transition width.
  * $\beta_{\text{bergmann}} = 0.25$: Bergmann latitudinal mass variation amplitude ($\pm 25\%$).
  * $c_{\text{geom}} = 0.048\text{ m}^2\cdot\text{kg}^{1/3}$: Human allometric surface coefficient (Du Bois & Du Bois 1916).
  * $T_{\text{comfort}} = 20.0^\circ\text{C}$: Thermoregulatory metabolic boundary.
  * $\gamma_{\text{thermo}} = 0.035$: Conductance scaling coefficient.
* **Epistemic Invariant**: Couples paleotemperature to body mass and DEB somatic maintenance across human and mammalian taxa without arbitrary local constants.
* **Computational Complexity**: $O(1)$ per cell.

#### 25.3. Charnov Life-History Invariants & Reproductive Allocation (`LifeHistoryInvariantsEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Charnov, E. L. (1993). *Life History Invariants: Some Explorations of Symmetry in Evolutionary Ecology*. Oxford University Press; Stearns, S. C. (1992). *The Evolution of Life Histories*. Oxford University Press; Promislow, D. E. L., & Harvey, P. H. (1990). *Living fast and dying young: A comparative analysis of life-history variation among mammals*. Journal of Zoology, 220(3), 417-437.
* **Governing Equations**:
  $$\mu_{\text{adult}} \cdot \alpha = C_{\text{charnov}} \approx 1.80 \pm 0.20$$
  $$\alpha^*(\mu) = \operatorname{clamp}\left(\frac{C_{\text{charnov}}}{\mu_{\text{total}}}, \; \alpha_{\min}, \; \alpha_{\max}\right) \quad [\text{years}]$$
  $$l_\alpha = \exp(-\mu_{\text{juv}} \cdot \alpha) = \exp(-q_{\text{juv}} \cdot C_{\text{charnov}}), \qquad b^*(\mu) = \frac{\mu_{\text{adult}}}{l_\alpha}$$
* **Parameters & Physical Units**:
  * $C_{\text{charnov}} = 1.80$: Conserved mammalian/primate life-history invariant [dimensionless].
  * $q_{\text{juv}} = 0.65$: Ratio of juvenile mortality hazard to adult mortality hazard.
  * $\alpha_{\min} = 12.0\text{ years}$, $\alpha_{\max} = 24.0\text{ years}$: Physiological hominin boundaries on reproductive maturity age.
  * $\mu_0 = 0.08\text{ yr}^{-1}$: Baseline intrinsic adult mortality rate.
* **Epistemic Invariant**: Derives dynamic maturity age and fertility acceleration under crisis mortality hazards directly from evolutionary optimization rather than ad-hoc demographic tables.
* **Computational Complexity**: $O(1)$ per cell.

#### 26. Kimura Neutral Genetic Drift & Wright-Fisher Diffusion (`GeneticAdaptationEngine`)
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

#### 29. Trophic Ecosystem Biomass Thermodynamics (`BiodiversityTrophicEngine`, `TrophicEcosystemEngine`)
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

#### 31.2. Critical Slowing Down & Saddle-Node Tipping Points (`CriticalSlowingDownTippingPointEngine`)
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
* **Academic Literature Reference**: Kooijman, S. A. L. M. (2010). *Dynamic Energy Budget theory for metabolic organisation*. Cambridge University Press; Jager, T., Martin, B. T., & Baveco, J. M. (2013). *Alternative angels. Predicting rethink dynamic energy budget theory*. Journal of Theoretical Biology, 328, 85-93; Nisbet, R. M., et al. (2000). *From molecules to ecosystems through dynamic energy budget models*. Journal of Animal Ecology, 69(6), 913-926.
* **Governing Equations**:
  $$\frac{dV}{dt} = \frac{\kappa \dot{p}_C - [\dot{p}_M] V}{[E_G]}, \qquad \frac{dE}{dt} = \dot{p}_A - \dot{p}_C$$
  $$k(T) = \exp\left(\frac{T_A}{T_{\text{ref}}} - \frac{T_A}{T}\right), \qquad f(X) = \frac{X}{X + X_K}$$
  $$\dot{p}_A = \{\dot{p}_{Am}\} \cdot f(X) \cdot k(T), \qquad \dot{p}_M = [\dot{p}_M] \cdot V$$
  $$\dot{p}_R = \max(0.0, \; (1 - \kappa) \dot{p}_C - [\dot{p}_J]), \qquad \mu_{\text{starv}} = \mu_{\text{max}} \cdot \left(\frac{\max(0, \Delta V / V_{\text{healthy}})}{w_{\text{crit}}}\right)^2$$
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
  $$\text{Redfield Ratio}_{\text{marine}} = 106\text{ C} : 16\text{ N} : 1\text{ P} \implies \frac{\text{C}}{\text{P}} \approx 41.1, \; \frac{\text{N}}{\text{P}} \approx 7.23 \text{ by mass}$$
  $$L_{\text{Liebig}}(\text{N}, \text{P}, \text{K}) = \text{softmin}\left(\frac{\text{N}}{\text{N}_{\text{opt}}}, \; \text{softmin}\left(\frac{\text{P}}{\text{P}_{\text{opt}}}, \; \frac{\text{K}}{\text{K}_{\text{opt}}}, \; 8.0\right), \; 8.0\right)$$
  $$D_{\text{protein}}(\text{CO}_2) = 1.0 - \delta_{\text{max}} \cdot \tanh\left(\frac{\max(0, \text{CO}_2 - 280.0)}{350.0}\right)$$
* **Parameters & Physical Units**:
  * $L_{\text{Liebig}} \in [0.05, 1.0]$: Continuous $C^1$ multi-nutrient limitation scalar.
  * $D_{\text{protein}} \in [0.88, 1.0]$: Atmospheric carbon enrichment protein dilution coefficient ($\sim 12\%$ maximum grain protein dilution).
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
  $$\eta_{\text{carnot}} = \frac{T_s - T_e}{T_s} \approx 0.115, \qquad G_{\text{ke}} = \frac{1}{4} \eta_{\text{carnot}} \cdot I_{\text{net}} \approx 2.5\text{--}3.5\text{ W/m}^2$$
  $$P_{\text{precip}} = E = \left(\frac{\Delta}{\Delta + \gamma}\right) \frac{R_n}{\lambda_v} \cdot 3.15576 \times 10^7\text{ s/yr}$$
* **Parameters & Physical Units**:
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

#### 35.1. Transport Infrastructure Capital & Spatial Friction Attenuation (`InfrastructureEnergyEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Donaldson, D. (2018). *Railroads of the Raj: Estimating the impact of transportation infrastructure*. American Economic Review, 108(4), 899-934.
* **Governing Equations**:
  $$k_{\text{infra}} = \frac{K_{\text{infra}}}{\max(10.0, A_{\text{cell}})} \quad [\text{kg/km}^2]$$
  $$\tau_{\text{eff}} = \tau_0 \cdot \left[ 0.10 + \frac{0.90}{1.0 + 0.05 \cdot \ln(1.0 + k_{\text{infra}})} \right]$$
* **Parameters & Physical Units**:
  * $K_{\text{infra}}$: Physical mass of paved roads, canals, and bridges [$\text{kg}$].
  * $\tau_{\text{eff}}$: Effective transport friction multiplier [dimensionless, $0.10 - 1.0$].
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
* **Academic Literature Reference**: Betz, A. (1926). *Wind-Energie und ihre Ausnutzung durch Windmühlen*. Vandenhoeck & Ruprecht; Shockley, W., & Queisser, H. J. (1961). *Detailed balance limit of efficiency of p-n junction solar cells*. Journal of Applied Physics, 32(3), 510-519.
* **Governing Equations**:
  $$P_{\text{wind, max}} = \frac{16}{27} \cdot \frac{1}{2} \rho_{\text{air}} A_{\text{rotor}} v_{\text{wind}}^3 \quad [\text{Watts}]$$
  $$P_{\text{solar, max}} = S(\phi) \cdot \eta_{\text{SQ}} \cdot A_{\text{panel}}, \quad \eta_{\text{SQ}} \approx 33.7\%$$
* **Parameters & Physical Units**:
  * $16/27 \approx 59.3\%$: Betz aerodynamic extraction ceiling for open fluid turbines.
  * $\eta_{\text{SQ}}$: Shockley-Queisser single-junction bandgap quantum limit.
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

#### 41. Planetary Boundaries Earth System Matrix (`PlanetaryBoundariesEngine`)
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

#### 42.1. Fundamental Physical Constants Scaling (`PhysicalLawEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Barrow, J. D. (2002). *The Constants of Nature*. Pantheon Books.
* **Governing Equations**:
  $$B_{\text{nat}}(t + \Delta t) = B_{\text{nat}}(t) \cdot \left[1.0 + (\gamma_{\text{photo}} - 1.0) \cdot 0.02 \cdot \Delta t\right]$$
  $$W_{\text{eff}}(t + \Delta t) = W_{\text{eff}}(t) \cdot \left[1.0 + \mathbb{I}_{T_c \ge 300\text{ K}} \cdot 0.05 \cdot \Delta t\right]$$
* **Parameters & Physical Units**:
  * $\gamma_{\text{photo}}$: Dimensionless scaling factor on photosynthetic quantum yield.
  * $\mathbb{I}_{T_c \ge 300\text{ K}}$: Superconductivity indicator eliminating line resistance ($I^2 R = 0$).
* **Computational Complexity**: $O(1)$ per cell.

#### 42.2. Thermodynamic Prime Movers & Useful Exergy Conversion (`ThermodynamicPrimeMoversEngine`)
* **Model Classification**: Tier 1 (Fundamental Invariant Physics)
* **Academic Literature Reference**: Smil, V. (2017). *Energy and Civilization: A History*. MIT Press; Ayres, R. U., & Warr, B. (2009). *The Economic Growth Engine: How Energy and Work Drive Material Prosperity*. Edward Elgar.
* **Governing Equations**:
  $$P_{\text{total}} = P_{\text{somatic}} + P_{\text{draft}} + P_{\text{hydraulic}} + P_{\text{wind}} + P_{\text{steam}} + P_{\text{grid}} \quad [\text{Watts}]$$
  $$P_{\text{somatic}} = N_i \cdot 100.0\text{ W}, \quad P_{\text{draft}} = \min(B_{\text{livestock}}, 2.0 N_i) \cdot 450.0 \cdot \sigma_{\text{yoke}}(k_{\text{pc}})$$
  $$P_{\text{steam}} = \eta_{\text{carnot}}(T_{\text{steam}}, T_{\text{cold}}) \cdot \frac{\text{CoalExtraction} \cdot \text{LHV}_{\text{coal}}}{\Delta t} \cdot \sigma_{\text{boiler}}(k_{\text{steam}})$$
  $$P_{\text{useful}} = \sum_m \eta_m \cdot P_m$$
* **Parameters & Physical Units**:
  * $P$: Mechanical, thermal, and electrical power generation [$\text{Watts}$].
  * $P_{\text{useful}}$: Delivered useful physical work [$\text{Watts}$].
  * $\sigma_{\text{yoke}}(k_{\text{pc}})$: Horse collar / heavy ard capital threshold ($k_{\text{pc}} \ge 80\text{ kg/cap}$).
  * $\sigma_{\text{boiler}}(k_{\text{steam}})$: High-pressure metallurgy capital threshold ($k_{\text{pc}} \ge 1800\text{ kg/cap}$).
  * $\eta_m$: Thermodynamic second-law conversion efficiency for prime mover $m$:
    * Human muscle labor: $\eta \approx 0.18$
    * Draft ox/horse: $\eta \approx 0.12$
    * Water/wind mill: $\eta \approx 0.35$
    * Newcomen atmospheric steam engine: $\eta \approx 0.005$
    * Watt condensing steam engine: $\eta \approx 0.04$
    * Corliss / Triple-expansion marine steam: $\eta \approx 0.18$
    * Parsons steam turbine & electric grid: $\eta \approx 0.38 - 0.45$
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 3.2.

---
## 4. Tier 2  -  Optional Cliodynamic & Phenomenological Engines (One-by-One)

### Subsection A: Macro-Theoretical Cliodynamic Plugins (`org.ether.society.procedural.tier2`)

#### 43. Kümmel-Ayres-Warr Biophysical Exergy Growth (`KummelAyresExergyEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Kümmel, R. (1982). *The impact of energy on industrial growth*. Energy, 7(2), 189-203; Ayres, R. U., & Warr, B. (2005). *Accounting for growth: the role of physical work*. Structural Change and Economic Dynamics, 16(2), 181-209.
* **Governing Equations**:
  $$Y = A \cdot K^\alpha L^\beta E^\gamma, \quad \alpha + \beta + \gamma = 1$$
  $$\alpha = a_0 \frac{K}{Y}, \quad \beta = b_0 \frac{L}{Y}, \quad \gamma = 1 - \alpha - \beta$$
* **Parameters & Physical Units**:
  * $Y$: Physical economic output [$\text{Geary-Khamis Int\$}$].
  * $K$: Productive physical capital stock [$\text{kg}$ or constant $\$$].
  * $L$: Labor input [$\text{worker-hours}$].
  * $E$: Useful thermodynamic exergy work delivered by prime movers [$\text{Joules}$].
  * $\alpha, \beta, \gamma$: Dynamic output elasticities governed by physical capital-exergy deepening.
* **Computational Complexity**: $O(1)$ per economic polity.

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
* **Academic Literature Reference**: Hamilton, W. D. (1964). *The genetical evolution of social behaviour. I & II*. Journal of Theoretical Biology, 7(1), 1-52.
* **Governing Equations**:
  $$r_{\text{relate}} \cdot B_{\text{recipient}} > C_{\text{actor}}$$
  $$\Delta F_{\text{hostility}} = \kappa_{\text{scarcity}} \cdot (1.0 - r_{\text{cultural}}) \cdot \max\left(0, 1.0 - \frac{E_{\text{cal}}}{E_{\text{req}}}\right)$$
* **Parameters & Physical Units**:
  * $r_{\text{relate}} \in [0, 1]$: Genetic or linguistic/cultural relatedness.
  * $B, C$: Caloric or reproductive fitness benefit and cost [$\text{Joules}$ or surviving offspring].
  * $\Delta F_{\text{hostility}}$: Outgroup violent raiding probability under famine conditions.
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

#### 47. Arthur Recombinant Combinatorial Capital Evolution (`ArthurCombinatorialTechnologyEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Arthur, W. B. (2009). *The Nature of Technology: What It Is and How It Evolves*. Free Press.
* **Governing Equations**:
  $$\frac{dK}{dt} = \mu_{\text{comb}} \cdot N \cdot \left(1 + \frac{k_{\text{pc}}}{120.0}\right)^{1.35} \cdot \ln\left(1 + \frac{N}{100.0}\right) \cdot \frac{1}{1 + k_{\text{pc}}/50000.0} - \delta_{\text{wear}} K$$
* **Parameters & Physical Units**:
  * $K$: Physical capital tooling and machine inventory [$\text{kg}$].
  * $k_{\text{pc}} = K / N$: Capital density [$\text{kg/capita}$].
  * $\mu_{\text{comb}} = 0.045\text{ kg/(person}\cdot\text{year)}$: Tool synthesis rate.
* **Computational Complexity**: $O(1)$ per cell.

#### 48. Hotelling Non-Renewable Resource Depletion Rent (`HotellingResourceDepletionEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137-175.
* **Governing Equations**:
  $$\frac{d P_{\text{resource}}}{dt} = r_{\text{interest}} \cdot P_{\text{resource}} - \frac{\partial C(R, Q)}{\partial R}$$
* **Parameters & Physical Units**:
  * $P_{\text{resource}}$: In-situ scarcity rent of mineral or fossil stock [$\text{\$/metric ton}$].
  * $r_{\text{interest}}$: Social discount interest rate [$\text{year}^{-1}$].
  * $C(R, Q)$: Marginal extraction cost function increasing as remaining stock $R$ depletes.
* **Computational Complexity**: $O(1)$ per mineral extraction node.

#### 49. Tainter Diminishing Returns on Organizational Complexity (`TainterComplexityCollapseEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Tainter, J. A. (1988). *The Collapse of Complex Societies*. Cambridge University Press.
* **Governing Equations**:
  $$\text{MarginalReturn } MR = \frac{d B_{\text{benefit}}}{d C_{\text{org}}} = \frac{B_0}{1 + \exp(\alpha C_{\text{org}})} - \kappa_{\text{maintenance}} C_{\text{org}}^{1.15}$$
* **Parameters & Physical Units**:
  * $C_{\text{org}}$: Administrative complexity, bureaucracy size, and institutional specialization [index units].
  * $B_{\text{benefit}}$: Solved societal problems and coordinated public goods output.
  * Systemic collapse trigger: When $MR \le 0$, escalating overhead taxes cause popular revolt or sudden systemic dissolution upon external shock.
* **Computational Complexity**: $O(1)$ per polity.

#### 50. NASA HANDY Socio-Ecological Predator-Prey Collapse (`HandyNasaHybridEngine`, `HandyNasaPureEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). *Human and nature dynamics (HANDY): Modeling inequality and use of resources in the collapse or sustainability of societies*. Ecological Economics, 101, 90-102.
* **Governing Equations**:
  $$\frac{dx_c}{dt} = \alpha_c x_c - \beta_c x_c, \quad \frac{dx_e}{dt} = \alpha_e x_e - \beta_e x_e, \quad \frac{dy}{dt} = \gamma y (K_y - y) - C(x_c, x_e, y)$$
* **Parameters & Physical Units**:
  * $x_c, x_e$: Commoner and elite population cohorts [persons].
  * $y$: Regenerative natural ecological capital / resource stock [biomass units].
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

#### 52. Turchin-Goldstone Structural-Demographic Theory (SDT) Secular Cycles (`TurchinGoldstoneSDTEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press; Turchin, P. (2016). *Ages of Discord*. Beresta Books; Goldstone, J. A. (1991). *Revolution and Rebellion in the Early Modern World*. University of California Press.
* **Governing Equations**:
  $$\Psi = \text{MMP} \cdot \text{EMP} \cdot \text{SFD}$$
  $$\text{MMP} = \frac{w_0}{w} \frac{N}{K_{\text{carrying}}}, \quad \text{EMP} = \frac{E}{N} \frac{y_{\text{elite}}}{y_0}, \quad \text{SFD} = \frac{\text{Debt}}{\text{Revenue}}$$
* **Parameters & Physical Units**:
  * $\Psi$: Political Stress Index [normalized on $[0, 100]$ in Ether].
  * $\text{MMP}$: Mass Mobilization Potential driven by declining real wages and land scarcity.
  * $\text{EMP}$: Elite Overproduction driven by wealth concentration and competition for fixed state offices.
  * $\text{SFD}$: State Fiscal Distress driven by sovereign borrowing costs and tax evasion.
* **Computational Complexity**: $O(1)$ per polity.

#### 52.1. Ibn Khaldun Asabiyyah Dynamics & Frontier Strife (`CliodynamicsEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Ibn Khaldun (1377). *The Muqaddimah: An Introduction to History*; Turchin, P. (2003).
* **Governing Equations**:
  $$\frac{dS}{dt} = r_{\text{front}} (1 - S) \cdot \mathbb{I}_{\text{frontier}} - \delta_{\text{core}} S \cdot \mathbb{I}_{\text{core}}$$
  $$\mu_{\text{strife}} = \mu_0 \cdot \Psi^2 \cdot \Delta t \quad (\mu_0 = 0.015\text{ yr}^{-1})$$
* **Parameters & Physical Units**:
  * $S \in [0.05, 1.0]$: Group solidarity / Asabiyyah collective action capacity.
  * $\mathbb{I}_{\text{frontier}}$: Active conflict border indicator.
  * $\mu_{\text{strife}}$: Annual excess demographic mortality during revolutionary civil war [$\text{year}^{-1}$].
* **Computational Complexity**: $O(1)$ per cell.

#### 53. Acemoglu-Robinson Inclusive vs. Extractive Institutions (`AcemogluRobinsonInstitutionsEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Acemoglu, D., & Robinson, J. A. (2012). *Why Nations Fail: The Origins of Power, Prosperity, and Poverty*. Crown Business; Acemoglu, D., Johnson, S., & Robinson, J. A. (2001). *The colonial origins of comparative development*. American Economic Review, 91(5), 1369-1401.
* **Governing Equations**:
  $$\frac{d I_{\text{inc}}}{dt} = \theta_1 \cdot \text{CivilSocietyStrength} - \theta_2 \cdot \text{EliteRentSeeking}$$
  $$\Delta K = K \cdot 0.02 \cdot (I_{\text{inc}} - 0.35) \cdot \Delta t$$
* **Parameters & Physical Units**:
  * $I_{\text{inc}} \in [0.0, 1.0]$: Broad-based property rights and rule of law index.
  * $\Delta K$: Physical capital investment accumulation rate.
* **Computational Complexity**: $O(1)$ per polity.

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
* **Academic Literature Reference**: Krugman, P. (1991). *Increasing returns and economic geography*. Journal of Political Economy, 99(3), 483-499; Fujita, M., Krugman, P., & Venables, A. J. (1999). *The Spatial Economy*. MIT Press.
* **Governing Equations**:
  $$\omega_i = \left( \sum_j Y_j T_{ij}^{1-\sigma} G_j^{\sigma-1} \right)^{1/\sigma}, \quad \frac{d \lambda_i}{dt} = \gamma_{\text{mig}} \cdot \left(\frac{\omega_i}{\bar{\omega}} - 1.0\right) \lambda_i$$
* **Parameters & Physical Units**:
  * $\omega_i$: Real wage index in region $i$.
  * $\lambda_i$: Share of total industrial workforce residing in region $i$.
  * $\sigma > 1$: Elasticity of substitution between manufactured varieties.
  * $T_{ij} \ge 1$: Iceberg transport cost factor.
* **Computational Complexity**: $O(N \log N)$ across urban trading hubs.

#### 57. Schelling-Axelrod Spatial Homophily & Segregation (`SchellingAxelrodSegregationEngine`)
* **Model Classification**: Tier 2 - Type A (Pluggable Cliodynamic Academic Debate)
* **Academic Literature Reference**: Schelling, T. C. (1971). *Dynamic models of segregation*. Journal of Mathematical Sociology, 1(2), 143-186; Axelrod, R. (1997). *The dissemination of culture: A model with local convergence and global polarization*. Journal of Conflict Resolution, 41(2), 203-226.
* **Governing Equations**:
  $$P(\text{Relocate}_i) = \begin{cases} 1.0 & \text{if } \frac{N_{\text{same\_trait}}}{N_{\text{neighbors}}} < \tau_{\text{tolerance}} \\ 0.0 & \text{otherwise} \end{cases}$$
* **Parameters & Physical Units**:
  * $\tau_{\text{tolerance}} \in [0.2, 0.7]$: Agent homophily tolerance threshold.
* **Computational Complexity**: $O(6)$ per cell.

---

### Subsection B: Type B Scenario Heuristics & Historical Plugins (`org.ether.society.procedural.typeb`)

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

#### 60. Roman Imperial Expansion & Currency Debasement (`RomanImperialCliodynamicEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Butcher, K., & Ponting, M. (2014). *The Metallurgy of Roman Silver Coinage: From the Reforms of Nero to the Reform of Septimius Severus*. Cambridge University Press; Hopkins, K. (1980). *Taxes and trade in the Roman Empire*. Journal of Roman Studies, 70, 101-125.
* **Governing Equations**:
  $$\text{Purity}_{t+1} = \text{Purity}_t \cdot \left(1.0 - \frac{\max(0, \text{MilitaryPayDeficit})}{\text{SilverReserves} + 1.0}\right)$$
* **Parameters & Physical Units**:
  * $\text{Purity}$: Fineness percentage of silver denarius/antoninianus [$5.0 - 100.0\%$].
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
  $$M_{\text{evasion}} = N_{\text{valley}} \cdot \sigma(\text{TaxRate}_{\text{grain}} - \text{HighlandForagingCost})$$
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

#### 75. Sexual Selection & Polygyny Mating Skew (`SexualSelectionMatingEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Henrich, J., Boyd, R., & Richerson, P. J. (2012). *The puzzle of monogamous marriage*. Philosophical Transactions of the Royal Society B, 367(1589), 657-669.
* **Governing Equations**:
  $$\text{UnrestRisk} = 1.0 + \gamma_{\text{skew}} \cdot (\text{Gini}_{\text{wives}} - 0.20)$$
* **Parameters & Physical Units**:
  * $\text{Gini}_{\text{wives}}$: Inequality in mating distribution.
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

#### 78. Tasmanian Demographic Cultural Atrophy (`TasmanianCulturalRegressionEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Henrich, J. (2004). *Demography and cultural evolution: Why adaptive cultural processes produce maladaptive losses - the Tasmanian effect*. American Antiquity, 69(2), 197-214.
* **Governing Equations**:
  $$\frac{dK}{dt} = \begin{cases} -\delta_{\text{atrophy}} \left(1 - \frac{N}{N_c}\right) (K - K_{\text{somatic}}) & \text{if } N < N_c \\ +\mu_{\text{innov}} N \sqrt{\frac{N}{N_c}} & \text{if } N \ge N_c \end{cases}$$
* **Parameters & Physical Units**:
  * $N_c = 5000.0\text{ persons}$: Critical learner network threshold below which copying errors exceed innovation.
* **Computational Complexity**: $O(1)$ per isolated landmass.

#### 79. Nordhaus DICE Climate-Economy Damage (`NordhausDicePureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Nordhaus, W. D. (1992). *An optimal transition path for controlling greenhouse gases*. Science, 258(5086), 1315-1319.
* **Governing Equations**:
  $$Y_{\text{net}} = (1 - \Omega(\Delta T)) Y_{\text{gross}}, \quad \Omega(\Delta T) = \psi_1 \Delta T + \psi_2 \Delta T^2$$
* **Parameters & Physical Units**:
  * $\psi_1 = 0.0, \psi_2 = 0.00284$: Quadratic GDP damage coefficient per $^\circ\text{C}^2$.
* **Computational Complexity**: $O(1)$ per climate region.

#### 80. Planetary Kardashev Scale Rating (`KardashevPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Kardashev, N. S. (1964). *Transmission of information by extraterrestrial civilizations*. Soviet Astronomy, 8, 217-221.
* **Governing Equations**:
  $$K_{\text{sagan}} = \frac{\log_{10}(P_{\text{total}}) - 6.0}{10.0}$$
* **Parameters & Physical Units**:
  * $P_{\text{total}}$: Civilization total energy dissipation [$\text{Watts}$].
* **Computational Complexity**: $O(1)$ globally.

#### 81. Leslie White Energetic Evolution (`LeslieWhitePureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: White, L. A. (1943). *Energy and the evolution of culture*. American Anthropologist, 45(3), 335-356.
* **Governing Equations**:
  $$C = E \times T_{\text{eff}} = p_{\text{exo}} \times \eta_{\text{transducer}}(k_{\text{pc}})$$
* **Parameters & Physical Units**:
  * $C$: Degree of cultural development.
* **Computational Complexity**: $O(1)$ per polity.

#### 82. Gerhard Lenski Social Stratification Curve (`LenskiPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Lenski, G. (1966). *Power and Privilege: A Theory of Social Stratification*. McGraw-Hill; Milanovic, B., et al. (2011).
* **Governing Equations**:
  $$G^*(k_{\text{pc}}) = 0.25 + 0.40 \exp\left( -\frac{\left(\ln\left(\frac{k_{\text{pc}}+10}{290}\right)\right)^2}{2 \cdot 1.25^2} \right) + 0.13 \frac{k_{\text{pc}}^2}{k_{\text{pc}}^2 + 2500^2}$$
* **Parameters & Physical Units**:
  * $G^*$: Equilibrium Gini coefficient [$0.25 - 0.65$].
* **Computational Complexity**: $O(1)$ per society.

#### 83. Marvin Harris Cultural Materialism (`CulturalMaterialismPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Harris, M. (1979). *Cultural Materialism: The Struggle for a Science of Culture*. Random House.
* **Governing Equations**:
  $$\text{NormAdoption} = \arg\max(\text{CaloricYield} - \text{MetabolicWork})$$
* **Parameters & Physical Units**:
  * Evaluates institutional adoption of sacred taboos (e.g. sacred cows, pork taboos) based on net agricultural thermodynamics.
* **Computational Complexity**: $O(1)$ per cultural node.

#### 84. Asimov Psychohistory Macro-Stochastic Dynamics (`PsychohistoryPureEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Turchin, P. (2008). *Arise 'cliodynamics'*. Nature, 454(7200), 34-35.
* **Governing Equations**:
  $$d\mathbf{X}_t = \mathbf{A}(\mathbf{X}_t) \, dt + \mathbf{\Sigma} \, d\mathbf{W}_t$$
* **Parameters & Physical Units**:
  * Evaluates statistical predictability decay under large $N$ ensembles.
* **Computational Complexity**: $O(1)$ per macro step.

#### 85. Cybernetic Autonomous AI Planetary Regulation (`SovereignAIGovernanceEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Beer, S. (1972). *Brain of the Firm: The Managerial Cybernetics of Organization*. Allen Lane.
* **Governing Equations**:
  $$\mathbf{u}^*(t) = \arg\min_{\mathbf{u}} \int_t^{t+H} \left( \|\mathbf{x}(\tau) - \mathbf{x}_{\text{safe}}\|^2_Q + \|\mathbf{u}(\tau)\|^2_R \right) d\tau$$
* **Computational Complexity**: $O(H \cdot \text{states})$.

#### 85.1. Multi-Hazard Early Warning Predictive Advisor (`CliodynamicAdvisorEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Governing Equations**:
  $$\mathbf{\Omega}_{\text{risk}} = \left[ \mathbb{I}(T_w \ge 31), \mathbb{I}([\text{NPK}] \le 0.15), \mathbb{I}(h_{\text{piezo}} \le 5), \mathbb{I}(\Psi \ge 70), \mathbb{I}(D \ge 1.5 Y) \right]$$
* **Computational Complexity**: $O(1)$ per cell.

#### 85.2. Axial Age Moralizing Gods & Prosocial Policing (`AxialAgeMoralizingGodsEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Whitehouse, H., et al. (2019). *Complex societies precede moralizing gods throughout world history*. Nature, 568(7751), 226-229; Norenzayan, A. (2013). *Big Gods: How Religion Transformed Cooperation and Conflict*. Princeton University Press.
* **Governing Equations**:
  $$\Theta_{\text{moral}} = \sigma\left(\frac{N - 100\,000}{15\,000}\right) \cdot \sigma\left(\frac{k_{\text{pc}} - 80}{20}\right)$$
  $$\tau_{\text{friction}} = \max\left(0.20, \; 1.0 - 0.35 \cdot \Theta_{\text{moral}}\right), \quad \Delta K_{\text{civic}} = K \cdot 0.015 \cdot \Theta_{\text{moral}} \cdot \Delta t$$
* **Parameters & Physical Units**:
  * $\Theta_{\text{moral}} \in [0, 1]$: Prosocial institutional trust scaling index.
  * $N_{\text{crit}} = 100\,000$: Seshat empirical anonymous scale threshold.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.3. Barfield Nomadic Steppe Shadow Empire & Extortion Symbiosis (`BarfieldNomadicSteppeEmpireEngine`)
* **Model Classification**: Tier 2 - Type A / B (Canonical Cliodynamic Debate & Scenario Plugin)
* **Academic Literature Reference**: Barfield, T. J. (1989). *The Perilous Frontier: Nomadic Empires and China, 221 BC to AD 1757*. Blackwell; Di Cosmo, N. (2002). *Ancient China and Its Enemies*. Cambridge University Press.
* **Governing Equations**:
  $$\Phi_{\text{tribute}} = \gamma_{\text{extort}} \cdot \min\left(K_{\text{sedentary}} \cdot 0.035, \; N_{\text{nomad}} \cdot 25.0\right) \cdot \Theta_{\text{vulnerability}}$$
  $$\frac{dK_{\text{nomad}}}{dt} = \Phi_{\text{tribute}} - \delta_{\text{pastoral}} K_{\text{nomad}}, \quad \left.\frac{dW_{\text{cohesion}}}{dt}\right|_{\Phi = 0} = -0.05 \cdot W_{\text{cohesion}}$$
* **Parameters & Physical Units**:
  * $\Phi_{\text{tribute}}$: Extorted cross-border bullion and agricultural subsidies [$\text{kg}$ or $\text{\$}$].
  * $W_{\text{cohesion}}$: Inter-clan military alignment and Khagan authority index.
* **Computational Complexity**: $O(1)$ per pastoral cell.

#### 85.4. Polanyi Fictitious Commodities & The Great Transformation (`PolanyiFictitiousCommoditiesEngine`)
* **Model Classification**: Tier 2 - Type B (Institutional Macroeconomics & Historical Transition)
* **Academic Literature Reference**: Polanyi, K. (1944). *The Great Transformation: The Political and Economic Origins of Our Time*. Beacon Press; Block, F. (2003). *Karl Polanyi and the Writing of The Great Transformation*. Theory and Society.
* **Governing Equations**:
  $$\mathcal{M}_{\text{com}} = \sigma\left(\frac{K}{N} - 80.0, \; 20.0\right) \cdot \sigma\left(\text{TradeNet} - 15.0, \; 5.0\right)$$
  $$\Delta Y_{\text{alloc}} = +0.08 \cdot \mathcal{M}_{\text{com}} \cdot Y_{\text{agri}} \cdot \Delta t$$
  $$\text{Dislocation} = \mathcal{M}_{\text{com}} \cdot \max\left(0, \; 1 - \frac{\text{Food}}{1.20 \cdot N}\right) \cdot 0.15$$
  $$\Delta \text{Friction}_{\text{social}} = +0.05 \cdot \text{Dislocation} \cdot \Delta t$$
* **Parameters & Physical Units**:
  * $\mathcal{M}_{\text{com}} \in [0, 1]$: Market commodification index of customary land and labor.
  * Dislocation: Subsistence vulnerability under dissolved customary communal buffers.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.5. Bagehot Fractional-Reserve Liquidity & Lender of Last Resort (`BagehotFractionalReserveEngine`)
* **Model Classification**: Tier 2 - Type B (Financial Macroeconomics & Banking Crises)
* **Academic Literature Reference**: Bagehot, W. (1873). *Lombard Street: A Description of the Money Market*. Henry S. King; Fisher, I. (1933). *The Debt-Deflation Theory of Great Depressions*. Econometrica.
* **Governing Equations**:
  $$M_{\text{credit}} = 1.0 + 0.35 \cdot \sigma\left(\frac{K}{N} - 150.0, \; 35.0\right) \cdot \sigma\left(\text{TradeNet} - 15.0, \; 5.0\right)$$
  $$\text{Loss}_{\text{freeze}} = 0.06 \cdot M_{\text{credit}} \cdot \text{Stress}_{\text{systemic}} \cdot \Delta t$$
  $$\Delta K_{\text{deflation}} = \begin{cases} 0 & \text{if } M_{\text{reserves}}/N \ge 15.0\text{ kg/cap (Bagehot Rule)} \\ -K \cdot \text{Loss}_{\text{freeze}} & \text{if unbuffered debt deflation} \end{cases}$$
* **Parameters & Physical Units**:
  * $M_{\text{credit}}$: Endogenous commercial credit multiplier.
  * $M_{\text{reserves}}$: State or central bank liquid bullion reserves.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.6. Corden-Neary Dutch Disease & Resource Curse (`CordenNearyDutchDiseaseEngine`)
* **Model Classification**: Tier 2 - Type B (Structural Macroeconomics & Mineral Booms)
* **Academic Literature Reference**: Corden, W. M., & Neary, J. P. (1982). *Booming Sector and De-Industrialisation in a Small Open Economy*. The Economic Journal, 92(368), 825-848; Auty, R. (1993). *Sustaining Development in Mineral Economies*. Routledge.
* **Governing Equations**:
  $$R_{\text{boom}} = \frac{M_{\text{metal}} + 0.5 M_{\text{coal}} + 0.8 M_{\text{oil}}}{K + M_{\text{metal}} + 1.0}$$
  $$\Delta Y_{\text{crowd-out}} = -0.03 \cdot \sigma\left(R_{\text{boom}} - 0.25, \; 0.08\right) \cdot Y_{\text{agri}} \cdot \Delta t$$
  $$\Delta \text{Friction}_{\text{port}} = +0.02 \cdot \sigma\left(R_{\text{boom}} - 0.25, \; 0.08\right) \cdot \Delta t$$
* **Parameters & Physical Units**:
  * $R_{\text{boom}}$: Extractive rent share in total cellular output.
* **Computational Complexity**: $O(1)$ per cell.

#### 85.7. Harold Innis Staples Thesis & Export Frontier Dependency (`InnisStaplesThesisEngine`)
* **Model Classification**: Tier 2 - Type B (Spatial Economic History & Resource Corridors)
* **Academic Literature Reference**: Innis, H. A. (1930). *The Fur Trade in Canada: An Introduction to Canadian Economic History*. University of Toronto Press; Mackintosh, W. A. (1923). *Economic Factors in Canadian History*. Canadian Historical Review.
* **Governing Equations**:
  $$S_{\text{ratio}} = \frac{0.40 B_{\text{nat}} + M_{\text{metal}}}{10.0 \cdot N + 1.0}$$
  $$\Delta \text{Trade}_{\text{export}} = +0.04 \cdot \sigma\left(S_{\text{ratio}} - 25.0, \; 8.0\right) \cdot \Delta t$$
  $$\Delta B_{\text{nat}} = -0.01 \cdot \sigma\left(S_{\text{ratio}} - 25.0, \; 8.0\right) \cdot B_{\text{nat}} \cdot \Delta t$$
* **Computational Complexity**: $O(1)$ per cell.

#### 85.8. Norbert Wiener Cybernetic Negative-Feedback Homeostasis (`WienerCyberneticHomeostasisEngine`)
* **Model Classification**: Tier 2 - Type B (System Dynamics & Control Theory)
* **Academic Literature Reference**: Wiener, N. (1948). *Cybernetics: Or Control and Communication in the Animal and the Machine*. MIT Press; Ashby, W. R. (1956). *An Introduction to Cybernetics*. Chapman & Hall.
* **Governing Equations**:
  $$e(t) = \frac{\text{Food}/N - \text{Setpoint}}{\text{Setpoint}}, \quad \Delta W = -\kappa_{\text{gain}} \cdot e(t) \cdot \Delta t$$
* **Computational Complexity**: $O(1)$ per cell.

#### 85.9. Alfred Crosby Columbian Exchange & Virgin-Soil Epidemics (`CrosbyColumbianExchangeEngine`)
* **Model Classification**: Tier 2 - Type B (Historical Ecological Transitions & Epidemiology)
* **Academic Literature Reference**: Crosby, A. W. (1972). *The Columbian Exchange: Biological and Cultural Consequences of 1492*. Greenwood Press; Dobyns, H. F. (1983). *Their Number Become Thinned*. University of Tennessee Press.
* **Governing Equations**:
  $$W_{\text{muscle}} \le 40.0 \quad \text{if } B_{\text{livestock}} < 1.0\text{ and } k_{\text{pc}} < 1\,500\text{ kg/cap}$$
  $$N(t + \Delta t) = N(t) \cdot \left[1.0 - 0.90 \cdot \min(1.0, \; 0.10 \cdot \Delta t)\right] \quad \text{upon Contact}$$
* **Computational Complexity**: $O(1)$ per cell.

#### 86. Technological Singularity & Accelerating Returns (`TechnologicalSingularityEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Kurzweil, R. (2005). *The Singularity Is Near*. Viking.
* **Governing Equations**:
  $$\frac{dC_{\text{compute}}}{dt} = \kappa \cdot C_{\text{compute}} \cdot \exp(\alpha t)$$
* **Computational Complexity**: $O(1)$ per cycle.

#### 87. Geoengineering Aerosol Albedo Feedback (`GeoengineeringAlbedoFeedbackEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Crutzen, P. J. (2006). *Albedo enhancement by stratospheric sulfur injections*. Climatic Change, 77(3), 211-220.
* **Governing Equations**:
  $$\Delta \alpha_{\text{albedo}} = -\kappa_{\text{aerosol}} M_{\text{SO2}}$$
* **Computational Complexity**: $O(1)$ per body.

#### 88. Closed-Loop Paraterraforming Domes (`LifeSupportDomeEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Wheeler, R. M. (2017). *Agriculture for space: People and places paving the way*. Open Agriculture, 2(1), 14-32.
* **Governing Equations**:
  $$\frac{d O_2}{dt} = P_{\text{hydroponic}} - C_{\text{human}} - \text{Leakage}(P_{\text{dome}}, P_{\text{ambient}})$$
* **Computational Complexity**: $O(1)$ per facility.

#### 88.1. In-Situ Resource Utilization (ISRU) Autarky (`IsruAutarkyAndSpaceColonizationEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Anand, M., et al. (2012). *Lunar ISRU: A review*. Planetary and Space Science, 74(1), 42-48.
* **Governing Equations**:
  $$R_{\text{autarky}} = \frac{K_{\text{isru}}}{K_{\text{isru}} + M_{\text{import}}}$$
* **Computational Complexity**: $O(1)$ per habitat cell.

#### 89. Cultural Sociology & Memetic Drift (`InformationEntropyEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Cavalli-Sforza, L. L., & Feldman, M. W. (1981). *Cultural Transmission and Evolution*. Princeton University Press.
* **Governing Equations**:
  $$H_{\text{culture}} = -\sum_i p_i \ln p_i, \quad dp_i = \nabla^2 p_i \, dt + \sigma \, dW_{i, t}$$
* **Computational Complexity**: $O(1)$ per cultural vector.

#### 90. Glottochronological Linguistic Divergence (`LanguageLinguisticEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Swadesh, M. (1952). *Lexico-statistic dating of prehistoric ethnic contacts*. Proceedings of the American Philosophical Society, 96(4), 452-463.
* **Governing Equations**:
  $$d_{\text{cognate}} = \exp(-2 \lambda_{\text{glotto}} t), \quad \lambda_{\text{glotto}} \approx 0.19 / \text{millennium}$$
* **Computational Complexity**: $O(1)$ per dialect community.

#### 91. Religious Proselytization & Sectarian Polarization (`ReligionIdeologyEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Stark, R., & Bainbridge, W. S. (1987). *A Theory of Religion*. Peter Lang.
* **Governing Equations**:
  $$\frac{dR_i}{dt} = \beta_{\text{pros}} R_i (1 - R_i) - \delta_{\text{schism}} R_i^2$$
* **Computational Complexity**: $O(1)$ per religious group.

#### 92. Financial Banking Fractional Reserves & Credit Leverage (`FinancialBankingEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Minsky, H. P. (1986). *Stabilizing an Unstable Economy*. Yale University Press.
* **Governing Equations**:
  $$\text{Leverage} = \frac{\text{Assets}}{\text{Equity}}, \quad \frac{d\text{Debt}}{dt} = r \cdot \text{Debt} - \text{Repay}$$
* **Computational Complexity**: $O(1)$ per banking node.

#### 93. Kinetic Warfare & Fortification Yield Breaching (`ThermodynamicWarfareEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Lanchester, F. W. (1916). *Aircraft in Warfare: The Dawn of the Fourth Arm*. Constable and Company.
* **Governing Equations**:
  $$P_{\text{kinetic}} = N_{\text{soldiers}} \cdot (50.0 + p_{\text{exo}} \cdot \eta_{\text{ordnance}}) \quad [\text{Watts}]$$
  $$\text{Resistance} = \sigma_{\text{yield}} \cdot d_{\text{wall}} \cdot A_{\text{cross}} \quad (\sigma_{\text{yield}} \in [20, 2000]\text{ MPa})$$
* **Computational Complexity**: $O(1)$ per battlefield.

#### 94. Thermodynamic Spatial Gravitational Migration (`ThermodynamicMigrationEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Ravenstein, E. G. (1885). *The laws of migration*. Journal of the Statistical Society of London, 48(2), 167-235; Zipf, G. K. (1946). *The P1 P2/D hypothesis: on the intercity movement of persons*. American Sociological Review, 11(6), 677-686.
* **Governing Equations**:
  $$M_{ij} = G_{\text{mig}} \cdot \frac{N_i N_j}{d_{ij}^2} \cdot \exp\left( \frac{\text{Utility}_j - \text{Utility}_i}{k_B T_{\text{friction}}} \right)$$
* **Computational Complexity**: $O(1)$ Ring-1 adjacency lookup.

#### 95. Jevons Paradox & Exergy Rebound (`JevonsParadoxEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Jevons, W. S. (1865). *The Coal Question*. Macmillan; Alcott, B. (2005). *Jevons' paradox*. Ecological Economics, 54(1), 9-21.
* **Governing Equations**:
  $$\text{Consumption} = \frac{\text{Work}}{\eta_{\text{converter}}}, \quad \epsilon_{\text{elasticity}} > 1.0 \implies \frac{d\text{Consumption}}{d\eta} > 0$$
* **Computational Complexity**: $O(1)$ per sector.

#### 96. Non-Linear Bifurcation Chaos (`BifurcationChaosEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Lorenz, E. N. (1963). *Deterministic nonperiodic flow*. Journal of the Atmospheric Sciences, 20(2), 130-141.
* **Governing Equations**:
  $$\dot{x} = \sigma_{\text{lorenz}} (y - x), \quad \dot{y} = x (\rho - z) - y, \quad \dot{z} = x y - \beta z$$
* **Computational Complexity**: $O(1)$ per node.

#### 97. Spatial Capital Tooling Diffusion (`TechTreeEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Acemoglu, D. (2002). *Directed technical change*. The Review of Economic Studies, 69(4), 781-809.
* **Governing Equations**:
  $$\Delta K_{j \leftarrow i} = \kappa_{\text{trade}} \cdot N_j \cdot \frac{\max(0, k_{\text{pc}, i} - k_{\text{pc}, j})}{\max(1.0, 0.1 \cdot \text{Friction}_{ij})} \cdot \Delta t$$
* **Computational Complexity**: $O(\text{active corridors})$.

#### 98. Spatial Trade Gravity Corridors (`TradeNetworkEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Tinbergen, J. (1962). *Shaping the World Economy*. Twentieth Century Fund.
* **Governing Equations**:
  $$\Phi_{ij} = G \cdot \frac{N_i N_j}{D_{ij}^\gamma} \cdot \exp(-\lambda_{\text{frict}} \cdot \text{Friction}_{ij})$$
* **Computational Complexity**: $O(\text{candidate links})$.

#### 99. Municipal Public Sanitation & Potable Water Grid (`PublicSanitationWaterGridEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Omran, A. R. (1971). *The epidemiological transition: a theory of the epidemiology of population change*. Milbank Memorial Fund Quarterly, 49(4), 509-538; Cutler, D., & Miller, G. (2005). *The role of public health improvements in health advances: The twentieth-century United States*. Demography, 42(1), 1-22.
* **Governing Equations**:
  $$f_{\text{clean}} = \frac{K_{\text{pipe}}}{K_{\text{pipe}} + K_{\text{water}, 0}} \cdot \min\left(1.0, \frac{W_{\text{fresh}}}{W_{\text{req}} \cdot N_i}\right)$$
  $$f_{\text{sewer}} = \frac{k_{\text{sewer}}^2}{k_{\text{sewer}}^2 + 650.0^2}$$
  $$q_{\text{infant}} = 0.015 + 0.305 \cdot (1.0 - f_{\text{clean}})^{1.5} \cdot (1.0 - 0.5 f_{\text{sewer}}) \cdot \left(1.5 - 0.5 \frac{\text{Food}}{\text{Req}}\right)$$
* **Parameters & Physical Units**:
  * $f_{\text{clean}}$: Potable treated water coverage fraction [$0.0 - 1.0$].
  * $f_{\text{sewer}}$: Subterranean sewage drainage coverage fraction [$0.0 - 1.0$].
  * $q_{\text{infant}}$: Under-5 child mortality rate [$0.015 - 0.32$]. Pre-industrial baseline $\approx 32\%$, modern sanitised floor $\approx 1.5\%$.
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 2.

#### 100. Physical Media & Movable Type Printing Revolution (`PhysicalMediaPrintingEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Buringh, E., & van Zanden, J. L. (2009). *Charting the "Rise of the West": Manuscripts and printed books in Europe, a long-term perspective*. Journal of Economic History, 69(2), 409-445; Dittmar, J. E. (2011). *Information technology and economic change: the impact of the printing press*. Quarterly Journal of Economics, 126(3), 1133-1172.
* **Governing Equations**:
  $$Q_{\text{books}} = \alpha_{\text{scribe}} N_{\text{scribes}} + \alpha_{\text{press}} K_{\text{press}} \cdot \sigma_{\text{paper}}(k_{\text{mill}})$$
  $$\frac{dB_{\text{stock}}}{dt} = Q_{\text{books}} - \delta_{\text{wear}} B_{\text{stock}}, \quad B_{\text{pc}} = \frac{B_{\text{stock}}}{N_i}$$
  $$\text{Literacy} = \text{Literacy}_{\max} \cdot \frac{B_{\text{pc}}}{B_{\text{pc}} + 2.5} \cdot \frac{k_{\text{edu}}}{k_{\text{edu}} + 150.0}$$
* **Parameters & Physical Units**:
  * $\alpha_{\text{scribe}} = 2.0\text{ books/year}$: Manual scriptorium monk copying speed.
  * $\alpha_{\text{press}} = 1500.0\text{ books/year}$: Movable metal type press throughput.
  * $B_{\text{pc}}$: Physical book stock per person [books/capita].
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 2.

#### 101. Sovereign Treasury & Perpetual Bond Markets (`SovereignTreasuryBondEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Reinhart, C. M., & Rogoff, K. S. (2009). *This Time Is Different: Eight Centuries of Financial Folly*. Princeton University Press; Dickson, P. G. M. (1967). *The Financial Revolution in England*. Macmillan; Stasavage, D. (2011). *States of Credit*. Princeton University Press.
* **Governing Equations**:
  $$\text{Deficit} = G_{\text{military}} + G_{\text{admin}} + r_{\text{bond}} D_{\text{sovereign}} - T_{\text{revenue}}$$
  $$\frac{dD_{\text{sovereign}}}{dt} = \max(0, \text{Deficit}) - \text{Amortization}$$
  $$r_{\text{bond}} = r_0 + 0.10 \cdot \left(\frac{D_{\text{sovereign}}}{Y_{\text{polity}}}\right)^2 \cdot \frac{1}{\text{Legitimacy} + 0.1}$$
* **Parameters & Physical Units**:
  * $D_{\text{sovereign}}$: Outstanding sovereign government debt [$\text{Geary-Khamis Int\$}$].
  * $r_{\text{bond}}$: Sovereign yield spread on government debt [$0.03 - 0.25$].
* **Computational Complexity**: $O(1)$ per polity; executed in Pass 3.4.

#### 102. Naval Hydrodynamics & Cargo Freight Economics (`NavalHydrodynamicsFreightEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: Froude, W. (1874). *On the Resistance of Ships*. British Association for the Advancement of Science; Harley, C. K. (1988). *Ocean freight rates and new world expansion 1750-1913*. Journal of Economic History, 48(4), 851-876.
* **Governing Equations**:
  $$R_{\text{total}} = \frac{1}{2} C_f \rho_{\text{water}} S_{\text{wetted}} v^2 + \frac{1}{2} C_r \rho_{\text{water}} \nabla^{2/3} v^2$$
  $$C_{\text{freight}}(\Phi) = \sigma_{\text{sea}}(\Phi) \cdot \left[ 20.0 + \frac{1400.0}{1.0 + \left(\frac{\max(0, \Phi - 1.02)}{0.22}\right)^3} \right]$$
* **Parameters & Physical Units**:
  * $R_{\text{total}}$: Hydrodynamic total drag force [$\text{Newtons}$].
  * $C_{\text{freight}}$: Real maritime dry bulk shipping freight index (1913=100) [$20.0 - 1415.0$].
* **Computational Complexity**: $O(1)$ per maritime corridor; executed in Pass 2.

#### 103. Spatial Agrarian Concentric Rings & Urban Food Logistics (`VonThunenAgriculturalRingsEngine`)
* **Model Classification**: Tier 2 - Type B (Scenario Heuristic Engine)
* **Academic Literature Reference**: von Thünen, J. H. (1826). *Der isolirte Staat in Beziehung auf Landwirthschaft und Nationalökonomie*. Leopold; Chisholm, M. (1962). *Rural Settlement and Land Use*. Hutchinson.
* **Governing Equations**:
  $$R_{\text{rent}}(d) = Y \cdot (P_{\text{market}} - C_{\text{production}}) - Y \cdot F_{\text{freight}} \cdot d$$
  $$S_{\text{export}} = \max\left(0, \text{Harvest} - N_{\text{rural}} \cdot \text{Req}_{\text{subsistence}}\right) \cdot \exp(-\lambda_{\text{trans}} \cdot d)$$
* **Parameters & Physical Units**:
  * $R_{\text{rent}}(d)$: Economic locational land rent at distance $d$ from urban center [$\text{\$/ha}$].
  * $S_{\text{export}}$: Deliverable caloric food surplus exported to feed central nucleated city [$\text{kcal/year}$].
* **Computational Complexity**: $O(6)$ per agricultural cell; executed in Pass 2.

#### 104. Differential Land Rent & Margin Pressure (`RicardoDifferentialLandRentEngine`)
* **Model Classification**: Tier 2 - Type B (Thematic Theory Engine)
* **Academic Literature Reference**: Ricardo, D. (1817). *On the Principles of Political Economy and Taxation*. John Murray; Sraffa, P. (1960). *Production of Commodities by Means of Commodities*. Cambridge University Press.
* **Governing Equations**:
  $$\text{Margin}_{\text{ext}} = \sigma\left(\frac{N_i}{B_{\text{agr}, i} + 1} - 1.25, \; 0.40\right)$$
  $$\frac{dK_i}{dt} = -0.06 \cdot \text{Margin}_{\text{ext}} \cdot K_i$$
  $$\frac{d\text{Gini}_i}{dt} = +0.02 \cdot \text{Margin}_{\text{ext}}$$
* **Parameters & Physical Units**:
  * $N_i$: Cellular human population [inhabitants].
  * $B_{\text{agr}, i}$: Standing agricultural biomass [$\text{tons}$].
  * $\text{Margin}_{\text{ext}}$: Extensive and intensive land frontier pressure index $[0.0, 1.0]$.
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 3.2.

#### 105. Dual-Sector Structural Transformation & Lewis Turning Point (`LewisDualSectorTurningPointEngine`)
* **Model Classification**: Tier 2 - Type B (Thematic Theory Engine)
* **Academic Literature Reference**: Lewis, W. A. (1954). *Economic Development with Unlimited Supplies of Labour*. The Manchester School, 22(2), 139–191; Ranis, G., & Fei, J. C. (1961). *A Theory of Economic Development*. American Economic Review.
* **Governing Equations**:
  $$\text{SurplusAbsorption} = \sigma(k_{\text{pc}}, 60, 25) \cdot (1 - \sigma(k_{\text{pc}}, 250, 40))$$
  $$\text{TurningPointIntensity} = \sigma(k_{\text{pc}}, 250, 40) \cdot \sigma(\text{Trade}, 20, 5)$$
  $$\frac{dK_i}{dt} = \left(0.030 \cdot \text{SurplusAbsorption} + 0.045 \cdot \text{TurningPointIntensity}\right) \cdot K_i$$
* **Parameters & Physical Units**:
  * $k_{\text{pc}}$: Physical capital per capita [$\text{kg/person}$].
  * $\text{TurningPointIntensity}$: Post-turning point labor scarcity and mechanization induction index $[0.0, 1.0]$.
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 3.2.

#### 106. Long Wave Infrastructure Cycles & Creative Destruction (`KondratievSchumpeterLongWaveEngine`)
* **Model Classification**: Tier 2 - Type B (Thematic Theory Engine)
* **Academic Literature Reference**: Kondratiev, N. D. (1925). *The Major Economic Cycles*; Schumpeter, J. A. (1939). *Business Cycles*. McGraw-Hill; Freeman, C., & Louçã, F. (2001). *As Time Goes By*. Oxford University Press.
* **Governing Equations**:
  $$\phi(t) = \sin\left(\frac{2\pi \cdot (t - 1780)}{52}\right)$$
  $$\text{CreativeDestruction} = \max\left(0, \; -\phi(t)\right) \cdot \sigma(k_{\text{pc}}, 150, 30)$$
  $$\Delta K_{\text{liquidated}} = -0.015 \cdot \text{CreativeDestruction} \cdot K_i \cdot \Delta t$$
  $$\Delta M_{\text{recycled}} = +0.30 \cdot |\Delta K_{\text{liquidated}}|$$
* **Parameters & Physical Units**:
  * $\phi(t)$: Harmonic phase of the 52-year Kondratiev infrastructure cycle $[-1.0, 1.0]$.
  * $\Delta M_{\text{recycled}}$: Reclaimed scrap metal and reusable alloy feedstock [$\text{kg}$].
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 3.2.

#### 107. Energy Transport Proximity & Transatlantic Ghost Acreage (`PomeranzGreatDivergenceEngine`)
* **Model Classification**: Tier 2 - Type B (Thematic Theory Engine)
* **Academic Literature Reference**: Pomeranz, K. (2000). *The Great Divergence: China, Europe, and the Making of the Modern World Economy*. Princeton University Press; Allen, R. C. (2009). *The British Industrial Revolution in Global Perspective*. Cambridge University Press; Wrigley, E. A. (2010). *Energy and the English Industrial Revolution*. Cambridge University Press.
* **Governing Equations**:
  $$\text{CoalProximity} = \sigma(\text{Coal}, 500, 100) \cdot (1 - \sigma(\mu_{\text{friction}}, 0.70, 0.15))$$
  $$\text{GhostAcreage} = \sigma(\text{Trade}, 25, 5) \cdot \sigma(k_{\text{pc}}, 100, 30)$$
  $$\mathcal{P}_{\text{divergence}} = \text{CoalProximity} \cdot (1.0 + 0.80 \cdot \text{GhostAcreage})$$
  $$\frac{dK_i}{dt} = +0.055 \cdot \mathcal{P}_{\text{divergence}} \cdot K_i$$
* **Parameters & Physical Units**:
  * $\text{CoalProximity}$: Transport-accessible thermal fossil energy index $[0.0, 1.0]$.
  * $\text{GhostAcreage}$: Transoceanic agricultural land relief index $[0.0, 1.0]$.
  * $\mathcal{P}_{\text{divergence}}$: Industrial takeoff potential $[0.0, 1.8]$.
* **Computational Complexity**: $O(1)$ per cell; executed in Pass 3.2.

---
## 5. Computational Complexity & Integration Benchmark Matrix

| Subsystem Classification | Temporal Tier | Time Step ($\Delta t$) | Computational Complexity Per Cell | Typical Execution Time (175k Cells) |
|---|---|---|---|---|
| **Flux & Logistics (`FluxEngine`)** | Tier 1 | Daily ($\Delta t = 1\text{d}$) | $O(6)$ neighbor volume balance | $3.2\text{ ms}$ |
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
The simulation state at $T_0$ is initialized from a coupled tensor stack:
1. **24-bit Categorical RGB Entity Tensors**:
   - **Tensor 0 (Isoglosses / Languages)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Language Family / Glottolog Clade ID}$.
   - **Tensor 1 (Kinship & Clan Structures)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Lineage System (Patrilineal, Matrilineal, Bilateral Foraging Band)}$.
   - **Tensor 3 (Politico-Military Sovereignty & Polities)**: $\text{RGB}(R,G,B) \leftrightarrow \text{Polity ID / Sovereign Capital Jurisdiction}$.
2. **8-bit Continuous Intensity Grayscale Tensors ($[0, 255]$)**:
   - **Demographic Density**: $\rho(\mathbf{x}) = \rho_{\max} \cdot (G(\mathbf{x}) / 255)^\gamma$.
   - **Tensor 2 (Rituals / Asabiyyah)**: $A(\mathbf{x}) \in [0, 1]$ social cohesion and sacred norms.
   - **Tensor 4 (Materiality & Technologies)**: $\tau(\mathbf{x}) \in [0, 1]$ lithic / metallurgical complexity.
   - **Tensor 5 (Trade Corridors & Hubs)**: $C(\mathbf{x}) \in [0, 1]$ caravan / maritime conductance.
   - **Tensor 6 (Institutional Complexity)**: $\mathcal{I}(\mathbf{x}) \in [0, 1]$ legal codification and administrative depth (Seshat).
   - **Tensor 7 (Ecological Footprint & Degradation)**: $D(\mathbf{x}) \in [0, 1]$ soil salinization, erosion, deforestation.
   - **10 Geological Tensors**: Coal, Oil, Gas, Uranium, He-3, Iron/Copper, Precious Metals, Rare Earths, Geothermal Heat, Aquifers.

### 6.2 Pairwise Cultural Affinity & Distance Metric
For any two cultural entities $i$ and $j$, phenotypic/cultural distance $\text{Dist}_{ij}$ and symmetric affinity $\text{Affinity}_{ij}$ are given by:
$$\text{Dist}_{ij} = \sqrt{\sum_{k=1}^K w_k \left( T_{i,k} - T_{j,k} \right)^2}, \quad \text{Affinity}_{ij} = \exp\left( -\lambda \cdot \text{Dist}_{ij} \right) \in (0, 1]$$
where $\lambda \approx 3.5$, covering linguistic distance (ASJP phonological divergence), kinship incompatibility, Asabiyyah divergence, and institutional hierarchy.

---

## 7. High-Performance Computing (HPC) & Data-Oriented Design (DOD)

To sustain real-time execution across $175,000+$ H3 hexagonal cells without garbage collection pauses, Ether enforces:
1. **Struct-of-Arrays (SoA) Buffers**: Primitive arrays (`long[] population`, `float[] elevation`, `float[] availableWater`, `float[] capitalStock`) aligned to 64-byte cache lines.
2. **Hilbert Curve Cell Ordering**: Cells sorted along a 2D space-filling Hilbert curve ensuring $94\%$ L1/L2 CPU cache hit rates during spatial stencil evaluations.
3. **AVX-512 Vectorized Kinetics**: SIMD environmental and metabolic updates computing 8 or 16 cells per instruction cycle.
4. **Deterministic Stateless PRNG**: Spatial stochastic events evaluated via `DeterministicPRNG.splitMix64(seed, tick, cellIndex, streamId)`, guaranteeing bit-identical determinism across multi-threaded runs.

---
## 8. Post-Hoc Cliodynamic Macro-Indicators & Telemetry Specifications

In strict adherence to the Physical Observables Invariant (**AGENTS.md §10**) and the Ban on Curve-Fitting (**AGENTS.md §11**), runtime simulation kernels update only conserved physical, chemical, thermodynamic, and demographic observables. All 27 macro-indicators and cliodynamic metrics are evaluated post-hoc using closed-form analytical equations grounded in empirical literature from `CliodynamicAnalyticalFormulasCatalog.java` and `MacroeconomicMetricsCalculator.java`.

### 8.1. Macroeconomic & Energy Metabolism Indicators

#### 1. Gross World Product ($Y_{\text{GWP}}$)
* **Model Classification**: Tier 1 Thermodynamic Output Diagnostic
* **Academic Literature Reference**: Maddison, A. (2007). *Contours of the World Economy 1-2030 AD*. Oxford University Press; Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*; Solow, R. M. (1956).
* **Output Unit**: Billions of 1990 Geary-Khamis International Dollars [$\text{G\$}$].
* **Governing Equation**:
  $$y_{\text{pc}}(\Phi) = y_{\text{subsistence}} \cdot \left(\frac{\max(0.85, \Phi_{\text{exergy}})}{0.85}\right)^{3.5}$$
  $$Y_{\text{GWP}} = \frac{N_{\text{world}} \cdot y_{\text{pc}}(\Phi)}{10^9}$$
* **Parameters & Physical Units**:
  * $N_{\text{world}}$: Total global human population [inhabitants].
  * $\Phi_{\text{exergy}}$: Garrett-Lotka Exergy-Capital Index [dimensionless, $0.85 - 3.50$].
  * $y_{\text{subsistence}} = 100.0\text{ Int\$/capita}$: Somatic foraging subsistence income floor.

#### 2. Global Primary Energy Harvest ($E_{\text{primary}}$)
* **Model Classification**: Tier 1 Physical Energy Telemetry
* **Academic Literature Reference**: Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Output Unit**: Exajoules per year [$\text{EJ/year}$, $10^{18}\text{ Joules/year}$].
* **Governing Equation**:
  $$e_{\text{pc}}(\Phi) = e_{\text{somatic}} \cdot \left(\frac{\max(0.85, \Phi_{\text{exergy}})}{0.85}\right)^{2.2} \quad [\text{GJ/(person}\cdot\text{year)}]$$
  $$E_{\text{primary}} = \frac{N_{\text{world}} \cdot e_{\text{pc}}(\Phi) \cdot 10^9}{10^{18}}$$
* **Parameters & Physical Units**:
  * $e_{\text{somatic}} = 15.0\text{ GJ/(person}\cdot\text{year)}$: Basal somatic food intake ($3.36\text{ GJ}$) plus domestic biomass heating ($11.64\text{ GJ}$).

#### 3. Fossil Fuel Share in Primary Energy ($f_{\text{fossil}}$)
* **Model Classification**: Tier 1 Physical Mix Diagnostic
* **Academic Literature Reference**: Smil, V. (2017); Ritchie, H., et al. (2020). *Energy*. Our World in Data.
* **Output Unit**: Percentage of primary energy [$\%$].
* **Governing Equation**:
  $$f_{\text{fossil}}(\Phi) = \operatorname{clamp}\left( \frac{85.0}{1.0 + \exp(-(\Phi_{\text{exergy}} - 1.55) / 0.18)}, \; 0.0, \; 95.0 \right)$$
* **Parameters & Physical Units**:
  * $\Phi_{\text{exergy}} = 1.55$: Inflection point corresponding to the First Industrial Revolution transition from biomass to coal.

#### 4. Agricultural Energy Return on Investment ($\text{EROEI}_{\text{agr}}$)
* **Model Classification**: Tier 1 Agro-Energetic Diagnostic
* **Academic Literature Reference**: Smil, V. (2008). *Energy in Nature and Society*. MIT Press; Leach, G. (1976). *Energy and Food Production*. IPC Science and Technology Press.
* **Output Unit**: Dimensionless caloric output / input ratio.
* **Governing Equation**:
  $$\text{EROEI}_{\text{agr}}(\Phi) = \sigma_{\text{agr}}(\Phi) \cdot \left[ 4.5 + \frac{6.0}{1.0 + \exp(-(\Phi_{\text{exergy}} - 1.50) / 0.20)} \right]$$
  $$\sigma_{\text{agr}}(\Phi) = \tanh(\max(0.0, \Phi_{\text{exergy}} - 0.935) \cdot 5.0)$$
* **Parameters & Physical Units**:
  * $\sigma_{\text{agr}}$: Neolithic domestication activation factor enforcing $\text{EROEI}_{\text{agr}} \equiv 0.0$ for Pleistocene hunter-gatherers.
  * Baseline $4.5$: Traditional plow agriculture return.
  * Ceiling $10.5$: Mechanized agriculture supported by Haber-Bosch fertilizers and diesel tractors.

---

### 8.2. Urban, Human Capital & Institutional Indicators

#### 5. Urbanization Rate ($U$)
* **Model Classification**: Tier 1 Demographic State Observable
* **Academic Literature Reference**: Bairoch, P. (1988). *Cities and Economic Development*. University of Chicago Press; de Vries, J. (1984). *European Urbanization: 1500-1800*. Harvard University Press.
* **Output Unit**: Percentage of total population [$\%$].
* **Governing Equation**:
  * Direct State Observable (`DemographicMetricsCalculator.java`):
    $$U = \frac{\sum_{i \in \text{nucleated}} N_i}{N_{\text{total}}} \times 100.0, \quad \text{where } i \in \text{nucleated} \iff (N_i \ge 5000 \text{ or } \rho_i \ge 150\text{ hab/km}^2)$$
  * Analytical Closed-Form (`CliodynamicAnalyticalFormulasCatalog.java`):
    $$U(\Phi) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.95) \cdot 5.0) \cdot \frac{85.0}{1.0 + \exp(-(\Phi - 1.65) / 0.30)}, \; 0.0, \; 90.0 \right)$$

#### 6. Adult Literacy Rate ($L$)
* **Model Classification**: Tier 2 Cultural Telemetry
* **Academic Literature Reference**: Buringh, E., & van Zanden, J. L. (2009). *Charting the "Rise of the West"*. Journal of Economic History; Graff, H. J. (1987). *The Legacies of Literacy*. Indiana University Press.
* **Output Unit**: Percentage of adult population [$\%$].
* **Governing Equation**:
  * Direct State Observable (`CulturalKnowledgeMetricsCalculator.java`):
    $$L = L_{\max} \cdot \frac{B_{\text{pc}}}{B_{\text{pc}} + 2.5} \cdot \frac{k_{\text{edu}}}{k_{\text{edu}} + 150.0}$$
  * Analytical Closed-Form (`CliodynamicAnalyticalFormulasCatalog.java`):
    $$L(\Phi) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.95) \cdot 5.0) \cdot \frac{98.0}{1.0 + \exp(-(\Phi - 1.60) / 0.28)}, \; 0.0, \; 99.0 \right)$$

#### 7. Cohort Life Expectancy at Birth ($e_0$)
* **Model Classification**: Tier 1 Demographic Telemetry
* **Academic Literature Reference**: Omran, A. R. (1971). *The Epidemiological Transition*; Preston, S. H. (1975). *The Changing Relation between Mortality and Level of Economic Development*.
* **Output Unit**: Mean expected lifespan in years [$\text{years}$].
* **Governing Equation**:
  $$e_0(\Phi, \text{Food}) = \operatorname{clamp}\left( 28.0 + \frac{52.0}{1.0 + \exp(-(\Phi_{\text{exergy}} - 1.50) / 0.25)} + \operatorname{clamp}\left(\frac{\text{Food} - 2000.0}{500.0}, -8.0, 5.0\right), \; 20.0, \; 85.0 \right)$$
* **Parameters & Physical Units**:
  * Pre-industrial baseline: $28.0\text{ years}$.
  * Modern post-sanitation ceiling: $80.0\text{ years}$.
  * Nutritional perturbation: $\pm 5.0\text{ years}$ based on caloric intake relative to $2000\text{ kcal/capita/day}$.

#### 8. Elite Overproduction Index ($\text{EOI}$)
* **Model Classification**: Tier 2 - Type A Cliodynamic Diagnostic
* **Academic Literature Reference**: Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American Polity*. Beresta Books.
* **Output Unit**: Dimensionless ratio.
* **Governing Equation**:
  $$\text{EOI}(\Phi, S) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.94) \cdot 4.0) \cdot \left[1.0 + 2.5 \cdot \left(1.0 - \frac{S}{100.0}\right)\right], \; 0.0, \; 5.0 \right)$$
* **Parameters & Physical Units**:
  * $S$: Institutional political stability score [$0.0 - 100.0$].

#### 9. Political Stress Index ($\Psi$, PSI)
* **Model Classification**: Tier 2 - Type A Cliodynamic Stress Metric
* **Academic Literature Reference**: Turchin, P. (2003, 2016); Goldstone, J. A. (1991).
* **Output Unit**: Index points on $[0.0, 100.0]$.
* **Governing Equation**:
  $$\Psi(\Phi, S) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.95) \cdot 3.0) \cdot \left[ 25.0 + 70.0 \cdot \left(1.0 - \frac{S}{100.0}\right) \right], \; 0.0, \; 100.0 \right)$$
* **Parameters & Physical Units**:
  * Pax / Integrative Empire baseline: $\Psi \approx 40 - 55$.
  * State Breakdown / Civil War crisis: $\Psi > 80$.

#### 10. Asabiyyah Social Cohesion ($S_{\text{asabiyyah}}$)
* **Model Classification**: Tier 2 - Type A Sociological Diagnostic
* **Academic Literature Reference**: Ibn Khaldun (1377). *The Muqaddimah*; Turchin, P. (2003).
* **Output Unit**: Dimensionless collective solidarity score [$0.05, 1.0$].
* **Governing Equation**:
  $$S_{\text{asabiyyah}}(\Phi, S) = \operatorname{clamp}\left( 0.50 \cdot \frac{S}{100.0} + \frac{0.50}{1.0 + 0.30 \max(0.0, \Phi_{\text{exergy}} - 0.85)}, \; 0.05, \; 1.0 \right)$$

#### 11. Sociopolitical Instability Index ($\text{SPI}$)
* **Model Classification**: Tier 2 - Type A Macro-Instability Metric
* **Academic Literature Reference**: Turchin, P., & Nefedov, S. (2009). *Secular Cycles*. Princeton University Press.
* **Output Unit**: Index points on $[0.0, 100.0]$.
* **Governing Equation**:
  $$\text{SPI}(\Phi, S) = \operatorname{clamp}\left( 10.0 + 85.0 \cdot \left(1.0 - \frac{S}{100.0}\right) \cdot \tanh(\max(0.0, \Phi - 0.95) \cdot 3.0), \; 0.0, \; 100.0 \right)$$

#### 12. Gini Wealth & Income Inequality ($G$)
* **Model Classification**: Tier 2 - Type A Stratification Diagnostic
* **Academic Literature Reference**: Milanovic, B., Lindert, P. H., & Williamson, J. G. (2011). *Pre-Industrial Inequality*. Economic Journal; Scheidel, W. (2017). *The Great Leveler*.
* **Output Unit**: Gini coefficient [$0.18 - 0.75$].
* **Governing Equation**:
  $$G(\Phi) = \operatorname{clamp}\left( 0.23 + 0.25 \cdot \tanh(\max(0.0, \Phi_{\text{exergy}} - 0.90) \cdot 2.2), \; 0.18, \; 0.75 \right)$$

#### 13. Coinage Precious Metal Purity / Currency Debasement ($D_{\text{currency}}$)
* **Model Classification**: Tier 2 - Type B Monetary Policy Indicator
* **Academic Literature Reference**: Butcher, K., & Ponting, M. (2014); Sargent, T. J., & Velde, F. R. (2002). *The Big Problem of Small Change*. Princeton University Press.
* **Output Unit**: Precious metal purity percentage [$\%$].
* **Governing Equation**:
  $$P_{\text{coin}}(\Phi, S) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.95) \cdot 4.0) \cdot 100.0 \cdot \left[ 1.0 - 0.70 \left(1.0 - \frac{S}{100.0}\right) \right], \; 5.0, \; 100.0 \right)$$

#### 14. Sovereign Debt Burden ($D_{\text{sovereign}}$)
* **Model Classification**: Tier 2 - Type B Fiscal Debt Telemetry
* **Academic Literature Reference**: Reinhart, C. M., & Rogoff, K. S. (2009). *This Time Is Different: Eight Centuries of Financial Folly*. Princeton University Press.
* **Output Unit**: Debt as percentage of annual GDP [$\%$].
* **Governing Equation**:
  $$D_{\text{sovereign}}(\Phi, S) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.94) \cdot 5.0) \cdot \left[ 10.0 + 80.0 \left(1.0 - \frac{S}{100.0}\right) + 20.0 \tanh(\max(0.0, \Phi - 1.20)) \right], \; 0.0, \; 250.0 \right)$$

---

### 8.3. Logistics, Communications & Environmental Indicators

#### 15. Global Maritime Trade Volume ($V_{\text{trade}}$)
* **Model Classification**: Tier 2 - Type B Trade Volume Telemetry
* **Academic Literature Reference**: Federico, G., & Tena-Junguito, A. (2017). *A Tale of Two Globalizations*. Review of World Economics; Tinbergen, J. (1962).
* **Output Unit**: Trade volume index (1913 = 100).
* **Governing Equation**:
  $$V_{\text{trade}}(\Phi) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.94) \cdot 5.0) \cdot 3.5 \cdot \left(\frac{\max(0.94, \Phi)}{0.94}\right)^{4.5}, \; 0.0, \; 3000.0 \right)$$

#### 16. Real Maritime Shipping Freight Cost ($C_{\text{freight}}$)
* **Model Classification**: Tier 2 - Type B Transport Economics Indicator
* **Academic Literature Reference**: Harley, C. K. (1988). *Ocean Freight Rates and New World Expansion 1750-1913*. Journal of Economic History; Mohammed, S. I., & Williamson, J. G. (2004).
* **Output Unit**: Real freight cost index (1913 = 100).
* **Governing Equation**:
  $$C_{\text{freight}}(\Phi) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 1.02) \cdot 10.0) \cdot \left[ 20.0 + \frac{1400.0}{1.0 + \left(\frac{\max(0.0, \Phi - 1.02)}{0.22}\right)^3} \right], \; 15.0, \; 1415.0 \right)$$

#### 17. Long-Distance Information Travel Speed ($v_{\text{info}}$)
* **Model Classification**: Tier 1 Communications Physical Diagnostic
* **Academic Literature Reference**: Scheidel, W. (2014). *The Shape of the Roman World: Modelling Imperium (ORBIS)*. JRA; Standage, T. (1998). *The Victorian Internet*. Walker & Company.
* **Output Unit**: Velocity in kilometers per day [$\text{km/day}$].
* **Governing Equation**:
  $$v_{\text{info}}(\Phi) = \operatorname{clamp}\left( 30.0 + 200.0 \operatorname{softplus}(\Phi - 1.10, 0.0, 5.0) + \frac{1.0 \times 10^7}{1.0 + \exp(-(\Phi - 1.70) / 0.12)}, \; 4.0, \; 3.0 \times 10^8 \right)$$
* **Parameters & Physical Units**:
  * Ancient courier relay: $30 - 80\text{ km/day}$.
  * Chappe optical semaphore (1792): $250 - 350\text{ km/day}$.
  * Electric copper telegraph: $\sim 10^7\text{ km/day}$.
  * Speed of light in vacuum ceiling: $3.0 \times 10^8\text{ km/day}$ ($300,000\text{ km/s}$).

#### 18. Per-Capita Structural Metal Smelting Production ($M_{\text{smelt}}$)
* **Model Classification**: Tier 1 Chemical Smelting Output
* **Academic Literature Reference**: Malandra, W. W. (1990). *Archaeometallurgy in Global Context*; USGS Mineral Commodity Summaries (2023).
* **Output Unit**: Kilograms per person per year [$\text{kg/(person}\cdot\text{year)}$].
* **Governing Equation**:
  * Direct State Observable (`MacroeconomicMetricsCalculator.java`): Evaluates actual elemental iron/copper output produced by `MetallurgyEnthalpyEngine`.
  * Analytical Closed-Form (`CliodynamicAnalyticalFormulasCatalog.java`):
    $$M_{\text{smelt}}(\Phi) = \operatorname{clamp}\left( \tanh(\max(0.0, \Phi - 0.90) \cdot 4.0) \cdot \left[ 1.5 + 60.0 (\operatorname{softplus}(\Phi - 1.35, 0.0, 5.0))^2 \right], \; 0.0, \; 350.0 \right)$$

#### 19. Real Unskilled Wage Purchasing Power ($W_{\text{unskilled}}$)
* **Model Classification**: Tier 1 Economic Welfare Diagnostic
* **Academic Literature Reference**: Allen, R. C. (2001). *The great divergence in European wages and prices from the Middle Ages to the First World War*. Explorations in Economic History, 38(4), 411-447; Clark, G. (2005).
* **Output Unit**: Purchasing power index (London 1750 = 100).
* **Governing Equation**:
  * Direct State Observable (`MacroeconomicMetricsCalculator.java`):
    $$W_{\text{unskilled}} = \operatorname{clamp}\left( 100.0 \cdot \frac{\text{Food}_{\text{pc}}}{2000.0} \cdot (1.5 - \text{Gini}), \; 50.0, \; 1000.0 \right)$$
  * Analytical Closed-Form (`CliodynamicAnalyticalFormulasCatalog.java`):
    $$W_{\text{unskilled}}(\Phi) = \operatorname{clamp}\left( 100.0 + 150.0 (\operatorname{softplus}(\Phi - 1.20, 0.0, 10.0))^{1.8}, \; 50.0, \; 1000.0 \right)$$

#### 20. Pristine Forest Cover Remaining ($F_{\text{forest}}$)
* **Model Classification**: Tier 1 Ecological State Observable
* **Academic Literature Reference**: Williams, M. (2003). *Deforesting the Earth: From Prehistory to Global Crisis*. University of Chicago Press; Kaplan, J. O., et al. (2009).
* **Output Unit**: Percentage of pre-anthropogenic forest area [$\%$].
* **Governing Equation**:
  $$F_{\text{forest}}(\Phi) = \operatorname{clamp}\left( 99.0 - 55.0 \cdot \tanh(\max(0.0, \Phi_{\text{exergy}} - 0.935) \cdot 1.8), \; 30.0, \; 99.0 \right)$$

#### 21. Topsoil Erosion Loss Rate ($E_{\text{soil}}$)
* **Model Classification**: Tier 1 Geomorphic Transport Diagnostic
* **Academic Literature Reference**: Montgomery, D. R. (2007). *Soil erosion and agricultural sustainability*. PNAS, 104(33), 13268-13272.
* **Output Unit**: Metric tons per hectare per year [$\text{t/(ha}\cdot\text{year)}$].
* **Governing Equation**:
  $$E_{\text{soil}}(\Phi) = \operatorname{clamp}\left( 0.03 + 35.0 \cdot \tanh(\max(0.0, \Phi_{\text{exergy}} - 0.88) \cdot 2.5), \; 0.03, \; 70.0 \right)$$

#### 22. Atmospheric $\text{CO}_2$ Concentration ($C_{\text{CO2}}$)
* **Model Classification**: Tier 1 Atmospheric Reservoir Observable
* **Academic Literature Reference**: Lüthi, D., et al. (2008). *High-resolution carbon dioxide concentration record 650,000-800,000 years before present*. Nature, 453(7193), 379-382 (EPICA Dome C); Keeling, C. D., et al. (2005).
* **Output Unit**: Parts per million by volume [$\text{ppm}$].
* **Governing Equation**:
  For $t < 1850\text{ CE}$, retrieved directly from EPICA Dome C ice cores (PCHIP monotone spline interpolation).
  For modern industrial and prospective epochs ($t \ge 1850\text{ CE}$):
  $$C_{\text{CO2}}(\Phi) = 280.0 + 22.0 \tanh(\operatorname{softplus}(\Phi - 3.65, 50.0) \cdot 1.10) + \sigma(\Phi; 4.60, 0.05) \cdot \frac{125.0}{1.0 + \exp(-2.00 (\Phi - 6.40))}$$

#### 23. Mean Global Surface Temperature Anomaly ($\Delta T$)
* **Model Classification**: Tier 1 Climate Diagnostic
* **Academic Literature Reference**: Myhre, G., et al. (1998); IPCC AR6 WG1 (2021).
* **Output Unit**: Degrees Celsius relative to pre-industrial Holocene [$^\circ\text{C}$].
* **Governing Equation**:
  $$\Delta F = 5.35 \cdot \ln\left( \frac{\max(150.0, C_{\text{CO2}})}{280.0} \right) \quad [\text{W/m}^2]$$
  $$\Delta T = \Delta F \cdot \lambda_{\text{climate}} = \Delta F \cdot 0.80 \quad [^\circ\text{C}]$$
* **Parameters & Physical Units**:
  * $\lambda_{\text{climate}} = 0.80^\circ\text{C}/(\text{W/m}^2)$: Equilibrium climate sensitivity parameter ($\sim 3.0^\circ\text{C}$ per doubling of $\text{CO}_2$).

#### 24. Biogeographical Zero-Containment Compliance ($\Omega_{\text{containment}}$)
* **Model Classification**: Tier 2 - Type B Ecological Quarantine Metric
* **Academic Literature Reference**: Crosby, A. W. (1972). *The Columbian Exchange: Biological and Cultural Consequences of 1492*. Greenwood Press; Elton, C. S. (1958). *The Ecology of Invasions by Animals and Plants*. Methuen.
* **Output Unit**: Biotic isolation index in $[0.04, 1.00]$.
* **Governing Equation**:
  $$\Omega_{\text{containment}}(V_{\text{trade}}) = \operatorname{clamp}\left( 1.00 - 0.62 \tanh\left(\frac{V}{19.5}\right) - 0.21 \tanh\left(\frac{\operatorname{softplus}(V - 16, 50)}{110}\right) - 0.13 \tanh\left(\frac{\operatorname{softplus}(V - 400, 50)}{1200}\right), \; 0.04, \; 1.00 \right)$$
* **Parameters & Physical Units**:
  * Driven endogenously by cumulative trade connectivity ($V_{\text{trade}}$), with zero calendar date gating.

#### 25. Planetary Kardashev Energy Rating ($K_{\text{sagan}}$)
* **Model Classification**: Tier 1 Planetary Power Diagnostic
* **Academic Literature Reference**: Kardashev, N. S. (1964); Sagan, C. (1973). *The Cosmic Connection*. Doubleday.
* **Output Unit**: Dimensionless continuous scale index.
* **Governing Equation**:
  $$K_{\text{sagan}} = \frac{\log_{10}(\max(1.0, P_{\text{watts/capita}} \cdot N_{\text{world}})) - 6.0}{10.0}$$
* **Parameters & Physical Units**:
  * Global power dissipation across all human civilization ($10^{13}\text{ W} \implies K \approx 0.73$).

#### 26. Emergent Technology Diagnostic ($T_{\text{emergent}}$)
* **Model Classification**: Tier 1 Post-Hoc Synthetic State Diagnostic
* **Academic Literature Reference**: Lotka, A. J. (1922); Garrett, T. J. (2011); Ashby, M. F. (2011).
* **Output Unit**: Synthetic technology index in $[0.1, 10.0]$.
* **Governing Equation**:
  $$T_{\text{emergent}} = \Phi_{\text{exergy}}(p, k_{\text{pc}}) = \text{H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex}(p, k_{\text{pc}}, 80.0)$$
* **Epistemic Invariant**:
  * Reconstituted strictly *a posteriori* from physical observables ($p = P_{\text{watts}}/N$ in $\text{Watts}$ and $k_{\text{pc}} = K/N$ in $\text{kg/capita}$).
  * Technology is never stored as an independent state variable in cellular memory (`WorldBuffer` or `H3Cell`).

#### 27. Krugman Core-Periphery Centripetal & Centrifugal Capital Agglomeration (`KrugmanCorePeripheryEngine.java`)
* **Model Classification**: Tier 2 - Type B Spatial Economic Geography Engine
* **Academic Literature Reference**: Krugman, P. (1991). *Increasing Returns and Economic Geography*. Journal of Political Economy, 99(3), 483–499; Fujita, M., Krugman, P., & Venables, A. J. (1999). *The Spatial Economy*. MIT Press.
* **Governing Equations**:
  $$\text{Access}_i = \frac{10^{-3} N_i}{\max(0.2, \mu_{\text{friction}, i})}, \qquad A_{\text{cap}, i} = \frac{1}{1 + \exp\!\left(-2.0 \left(\frac{K_i}{\bar{K}} - 1.0\right)\right)}, \qquad \Phi_{\text{centripetal}, i} = 2.0 \, A_{\text{cap}, i} \, \frac{\text{Access}_i}{1.0 + \text{Access}_i}$$
  $$\mu_{\text{friction}, i}(t + \Delta t) = \max\!\left(0.1, \; \mu_{\text{friction}, i}(t) \cdot (1.0 - 0.02 \, \Phi_{\text{centripetal}, i} \, \Delta t)\right)$$
  $$\frac{dK_i}{dt} = K_i \cdot \operatorname{clamp}\!\left( \left(0.015 \, \Phi_{\text{centripetal}, i} - 0.01 \, \frac{K_i / \bar{K}}{\max(1.0, \text{Access}_i)}\right), \; -0.05, \; +0.08 \right)$$
* **Physical & Economic Interpretation**: Low-friction coastal and riverine hubs ($\mu_{\text{friction}} \ll 1$) attract net capital accumulation ($dK_i/dt > 0$) while high-friction continental hinterlands experience centrifugal congestion dispersion, endogenously generating core-periphery Lorenz Gini differentiation ($|d| = 2.20$).

#### 28. Total Planetary Population ($N_{\text{world}}$)
* **Model Classification**: Tier 1 Demographic Mass State Observable
* **Academic Literature Reference**: Biraben, J. N. (1979). *Essai sur l'évolution du nombre des hommes*. Population; McEvedy, C., & Jones, R. (1978). *Atlas of World Population History*. Penguin Books; UN World Population Prospects (2024).
* **Output Unit**: Total living human count [persons].
* **Governing Equation**:
  $$N_{\text{world}} = \sum_{i \in \text{AllCells}} \sum_{c=1}^7 N_{i, c}$$
* **Parameters & Physical Units**:
  * Direct discrete sum of all 7 living demographic age cohorts across all $175,000+$ active spatial cells.

#### 29. Areal Pollution Concentration Normalization & Closed-Form Movement Friction (`EntropicMetalDissipationEngine.java`, `InfrastructureEnergyEngine.java`)
* **Model Classification**: Tier 1 Intensive State Invariance
* **Academic Literature Reference**: Georgescu-Roegen, N. (1971). *The Entropy Law and the Economic Process*; Ayres, R. U. (1998); Swan, G. S., et al. (2018).
* **Governing Equations**:
  * **Intensive Areal Pollution Increment** (`EntropicMetalDissipationEngine.java`):
    $$\Delta P_i = 0.1 \cdot \frac{\Delta M_{\text{dissipated}, i}}{A_{\text{cell}, i}} \quad [\text{pollution units}/\text{km}^2]$$
    Converts extensive dissipated metallic mass $\Delta M_{\text{dissipated}, i}$ ($\text{kg}$) across cell area $A_{\text{cell}, i}$ ($\text{km}^2$, e.g. $1\,770.49\text{ km}^2$ at `Res 4`) into an intensive areal concentration matching `EcologicalDegradationEngine.java` and `EcotoxicologyFertilityEngine.java` ($P_{50} = 100.0$), with zero hidden $k_{\text{pc}}$ threshold gates.
  * **Idempotent Closed-Form Movement Friction** (`InfrastructureEnergyEngine.java`):
    $$\mu_{\text{friction}, i} = \text{H3CellPhysicsAndStateHelper.calculateMovementFriction}(\text{cell}_i)$$
    Computes movement friction as a pure closed-form state function of terrain slope, vegetation density, moisture, and infrastructure stock, preventing cumulative tick-over-tick multiplicative decay.

#### 30. Charney Dynamic Vegetation-Albedo Bistability & Sahara Cusp Catastrophe (`CharneyVegetationAlbedoEngine.java`)
* **Model Classification**: Tier 1 Coupled Climate-Vegetation Dynamics
* **Academic Literature Reference**: Charney, J. G. (1975). *Dynamics of deserts and drought in the Sahel*. Q. J. R. Meteorol. Soc., 101, 193-202; Brovkin, V., et al. (1998). *Journal of Geophysical Research*, 103(D24), 31671-31685; Claussen, M., et al. (1999). *Geophysical Research Letters*, 26(14), 2037-2040.
* **Governing Equations**:
  $$V_{\text{eq}} = \frac{1}{1 + \exp\left( -\frac{P_{\text{annual}} - P_{50}}{s_P} \right)}, \qquad \frac{dV}{dt} = \frac{V_{\text{eq}} - V}{\tau_V}$$
  $$\alpha(V) = \alpha_{\text{bare}} - (\alpha_{\text{bare}} - \alpha_{\text{veg}}) \cdot V$$
  $$P_{\text{convect}}(V) = \beta_{\text{charney}} \cdot (\alpha_{\text{bare}} - \alpha(V)) \cdot P_{\text{baseline}}$$
  $$\Delta P_{\text{orbital}}(t) = A_{\text{orbital}} \cdot \cos\left( 2\pi \frac{t - t_{\text{AHP}}}{T_{\text{precession}}} \right)$$
* **Physical Mechanics & Historical Benchmark**:
  * Milankovitch precession ($T_{\text{precession}} = 23\,000\text{ years}$, peaking at $t_{\text{AHP}} \approx -7000\text{ BCE}$) drives African monsoon northward during the African Humid Period (AHP).
  * High grass/steppe cover lowers surface albedo from $\alpha \approx 0.38$ (bare sand) to $\approx 0.17$ (vegetated steppe), intensifying convective convergence ($+180\text{ mm/yr}$).
  * As orbital precession wanes, rainfall crosses critical fold bifurcation threshold $P_{\text{crit}} \approx 200\text{ mm/yr}$, triggering abrupt vegetation collapse between $-4100\text{ BCE}$ and $-2000\text{ BCE}$ into hyper-arid Sahara desert without ad-hoc piecewise tuning.
* **Computational Complexity**: $O(1)$ per land cell.

#### 31. Catchment Stream Power Fluvial Incision & Bagnold Silt Replenishment (`StreamPowerErosionEngine.java`)
* **Model Classification**: Tier 1 Geomorphological & Fluvial Transport Mechanics
* **Academic Literature Reference**: Howard, A. D., & Kerby, G. (1983). *Geological Society of America Bulletin*, 94(6), 739-752; Whipple, K. X., & Tucker, G. E. (1999). *Journal of Geophysical Research*, 104(B8), 17661-17674; Bagnold, R. A. (1966). *USGS Professional Paper 422-I*; Hack, J. T. (1957). *USGS Professional Paper 294-B*.
* **Governing Equations**:
  $$A_{\text{drainage}} = \max\left( A_{\text{cell}}, \; A_{\text{cell}} \cdot \left(\frac{L_{\text{flow}}}{L_0}\right)^{1 / h} \right) \quad \text{with } h \approx 0.57 \text{ (Hack's Law)}$$
  $$\dot{E}_{\text{fluvial}} = K_{\text{erod}} \cdot A_{\text{drainage}}^m \cdot S_{\text{slope}}^n \cdot \exp\left( -\frac{B_{\text{nat}} + B_{\text{agr}}}{B_{\text{ref}}} \right)$$
  $$q_{\text{sediment}} = \omega_{\text{stream}} \cdot \left( \frac{\dot{E}}{g \cdot (\rho_s - \rho_w)} \right) \cdot \eta_{\text{bagnold}}$$
  $$\Delta \text{SOC}_{\text{alluvial}} = q_{\text{sediment}} \cdot C_{\text{organic}} \cdot \frac{A_{\text{cell}}}{A_{\text{floodplain}}}$$
* **Physical Mechanics & Historical Benchmark**:
  * Couples high-energy headwater catchment incision with lowland/floodplain nutrient replenishment (Nile, Tigris-Euphrates, Indus, Yellow River).
  * Vegetative root cohesion protects soil ($K_{\text{erod}} \propto \exp(-B/B_{\text{ref}})$); anthropogenic deforestation precipitates rapid topsoil flushing.
  * Lowland sedimentation restores Soil Organic Carbon (SOC) up to $+1.5\text{ g/kg/decade}$, underpinning multi-millennial agrarian carrying capacity without artificial fertilizer.
* **Computational Complexity**: $O(1)$ per cell.

#### 32. Macro-Scale Thermodynamic Prime Movers & Exergy Transduction (`ThermodynamicPrimeMoversEngine.java`)
* **Model Classification**: Tier 1 Physical Prime Mover Transduction
* **Academic Literature Reference**: Smil, V. (2017). *Energy and Civilization: A History*. MIT Press; Ayres, R. U., & Warr, B. (2009). *The Economic Growth Engine*. Edward Elgar; Carnot, S. (1824). *Réflexions sur la puissance motrice du feu*.
* **Governing Equations**:
  $$P_{\text{draft}} = N_{\text{draft}} \cdot p_{\text{animal}} \cdot \left(\frac{k_{\text{pc}}}{k_{\text{pc}} + k_{\text{harness}}}\right), \qquad P_{\text{mill}} = K_{\text{mill}} \cdot \rho_{\text{water}} g Q H \cdot \eta_{\text{waterwheel}}$$
  $$P_{\text{thermal}} = \dot{M}_{\text{fuel}} \cdot \text{LHV} \cdot \eta_{\text{carnot}}(T_{\text{combustion}}, T_{\text{sink}}) \cdot \sigma_{\text{engine}}(k_{\text{pc}})$$
  $$P_{\text{watts/capita}} = \frac{P_{\text{somatic}} + P_{\text{draft}} + P_{\text{mill}} + P_{\text{thermal}}}{N_{\text{human}}}$$
* **Parameters & Physical Units**:
  * Unifies animal draft power, water/wind mechanical mills, Newcomen/Watt steam engines, and internal combustion turbines into conserved physical Wattage dissipation per capita ($p = P_{\text{watts}}/N$), driving endogenous Garrett-Lotka developmental progression $\Phi_{\text{exergy}}$.
* **Computational Complexity**: $O(1)$ per cell.

#### 33. Tier 2 Type B Engine Domain Validity Conditions & Default Scenario Activation (`AutoEpochScenarioHelper.java`)
* **Model Classification**: Tier 2 - Type B Epistemic Scope & Scenario Activation Protocol (`AGENTS.md` §8.2, §11.3, §12.4, §13)
* **Comprehensive Catalog Reference**: See [`docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md`](TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md) for the complete **86-entry** specification table (strictly 1-to-1 with executable Java engine classes).
* **Governing Epistemic Principle**:
  * Unlike Tier 1 biophysical conservation laws (which operate permanently across all epochs and planets), Tier 2 Type B engines model context-dependent historical, institutional, technological, capital, or geopolitical regimes across the full **$-3\,000\,000\text{ BP}$ to $2200\text{ CE}$** simulation horizon.
  * Rather than injecting hidden conditional sigmoids inside a Type B engine's differential equations to give it artificial validity outside its historical domain, `AutoEpochScenarioHelper.calibrateTypeBEngines(planetKey, year)` evaluates each engine's explicit **Domain Validity Conditions** (Planetary Scope, Temporal Window $[t_{\min}, t_{\max}]$ up to $2200\text{ CE}$, Physical Capital $k_{\text{pc}}$ & Exergy $P_{\text{watts}}$ Preconditions, and Geopolitical Regime Scope) and **unchecks (`false`) by default** any Type B engine whose validity conditions are not met by the scenario:
    1. **Deep-Time Biocultural, Evolutionary & Forager-Mobility Engines ($[-3\,000\,000\text{ BP}, 2200\text{ CE}]$, 8 engines)**: Active (`true`) from Plio-Pleistocene hominin emergence (`SelfDomesticationEngine`, `SexualSelectionMatingEngine`, `KinSelectionHamiltonEngine`, `PriceMultilevelSelectionEngine`), Middle Paleolithic symbolic language (`LanguageLinguisticEngine`, `CulturalSociologyEngine` from $-300\,000\text{ BP}$), and Upper Paleolithic maritime navigation (`DynamicMaritimeRoutingGraph` from $-60\,000\text{ BP}$) through $2200\text{ CE}$.
    2. **Neolithic, Bronze Age & Classical Agrarian-State Engines ($[-8\,000 / -3\,000\text{ BCE}, 2200\text{ CE}]$, 24 engines)**: Unchecked (`false`) across the Plio-Pleistocene and Paleolithic ($t_0 < -8\,000\text{ BCE}$); active (`true`) once sedentary farming, livestock traction, urban markets, and state fiscal structures exist. Long-term institutional and structural engines remain active up to $2200\text{ CE}$, while bounded historical regimes deactivate at their realistic upper bound (`ScottAgainstTheGrainPureEngine` $[-4\,000\text{ BCE}, 1500\text{ CE}]$, `FrontierAsabiyyahEngine` $[-3\,000\text{ BCE}, 1800\text{ CE}]$, `DraftAnimalFodderAllocationEngine` $[-3\,000\text{ BCE}, 1950\text{ CE}]$ superseded by tractor mechanization).
    3. **Iron Age, Medieval & Early Modern Mercantile Engines ($[-1\,000\text{ BCE} \dots 1700\text{ CE}, 2200\text{ CE}]$, 13 engines)**: Unchecked (`false`) prior to their historical emergence; bounded regimes deactivate after their historical era (`BarfieldNomadicSteppeEmpireEngine` $[-1\,000\text{ BCE}, 1750\text{ CE}]$, `CrosbyColumbianExchangeEngine` $[1492\text{ CE}, 1750\text{ CE}]$, `ProtestantWorkEthicEngine` $[1550\text{ CE}, 1950\text{ CE}]$, `AsymmetricColonialTradeEngine` $[1492\text{ CE}, 1960\text{ CE}]$, `InnisStaplesThesisEngine` $[1700\text{ CE}, 1960\text{ CE}]$, `PhysicalMediaPrintingEngine` $[1450\text{ CE}, 2010\text{ CE}]$ superseded by digital networks), while permanent fiscal/institutional structures (`SovereignTreasuryBondEngine`, `TocquevilleEliteFrustrationEngine`, `CantillonMonetaryInjectionEngine`, `AxialAgeMoralizingGodsEngine`) remain active through $2200\text{ CE}$.
    4. **Industrial, Fossil-Exergy & Modern Macroeconomic Engines ($[1750 / 1800 / 1850\text{ CE}, 2200\text{ CE}]$, 13 engines)**: Unchecked (`false`) across all pre-industrial scenarios; transitional industrial regimes deactivate at their realistic historical/future bound (`PomeranzGreatDivergenceEngine` $[1750\text{ CE}, 1914\text{ CE}]$, `RicardoDifferentialLandRentEngine` $[1750\text{ CE}, 1950\text{ CE}]$, `LewisDualSectorTurningPointEngine` $[1850\text{ CE}, 2050\text{ CE}]$, `CordenNearyDutchDiseaseEngine` $[1850\text{ CE}, 2060\text{ CE}]$), while enduring thermodynamic, financial, and urban-grid laws (`KondratievSchumpeterLongWaveEngine`, `KummelAyresExergyEngine`, `JevonsParadoxEngine`, `PublicSanitationWaterGridEngine`, `BagehotFractionalReserveEngine`, `EasterlinRelativeIncomeFertilityEngine`, `MarxCapitalAccumulationEngine`, `HotellingExhaustibleResourceEngine`, `SenEntitlementFamineEngine`) remain active up to $2200\text{ CE}$.
    5. **Contemporary, Great Acceleration & Prospective Off-Earth Engines ($[1900 / 1950 / 2035 / 2050 / 2100\text{ CE}, 2200\text{ CE}]$, 15 engines)**: Unchecked (`false`) prior to the 20th century on Earth; bounded 20th/21st-century regimes deactivate at their physical horizon (`KurzweilAcceleratingReturnsEngine` $[1990\text{ CE}, 2060\text{ CE}]$ at atomic/Landauer silicon limits, `World3CouplingEngine` $[1950\text{ CE}, 2100\text{ CE}]$ at the MIT Club of Rome horizon), long-term modern couplings remain active up to $2200\text{ CE}$ (`PhysicalLeontiefInputOutputEngine`, `PhysicalSupplyChainEngine`, `EcotoxicologyFertilityEngine`, `NuclearSafetyRadiotoxicityEngine`, `InformationEntropyEngine`, etc.), and prospective engines activate in future windows up to $2200\text{ CE}$ (`GeoengineeringAlbedoFeedbackEngine` $[2035\text{ CE}, 2200\text{ CE}]$, `IsruAutarkyAndSpaceColonizationEngine` $[2050\text{ CE}, 2200\text{ CE}]$, `TerraformingEngine` $[2100\text{ CE}, 2200\text{ CE}]$ on Earth and all epochs on Moon/Mars).
    6. **Specialized Regional, Counterfactual & Pure-Theory Sandbox Engines (13 engines)**: Systematically unchecked (`false`) by default on general global auto-scenarios (`AmerindianEcosystemEngine`, `RomanImperialCliodynamicEngine`, `EdoJapanIsolationEngine`, `MonasticDemographicBufferEngine`, `World3PureEngine`, `HandyNasaPureEngine`, `NordhausDiceHybridEngine`, `NordhausDicePureEngine`, `NuclearWarfareClimateEngine`, `BifurcationChaosEngine`, etc.), and enabled only in their dedicated regional/thematic presets or when explicitly checked by the user.

---
