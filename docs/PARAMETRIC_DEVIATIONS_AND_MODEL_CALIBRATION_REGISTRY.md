# ⚖️ Central Registry of Model Calibrations & Literature Parametric Deviations

> **Ether Cliodynamic & Planetary Simulation Laboratory**  
> *Standard: AGENTS.md §8, §9, §10 & §11 — Epistemic Integrity, Observable State Invariants & Continuous Confidence Envelopes*  
> *Scope: Exhaustive and transparent documentation of all mathematical adaptations, discrete-to-mesh translations, continuous relaxations, and material/taxonomic parameter calibrations applied to published canonical equations for execution on the continuous H3 hexagonal tensor mesh.*

---

## 🎯 1. Purpose & Strict Registry Inclusion Criteria (Exceptions are NOT the Rule)

The foundational purpose of this registry is **epistemic transparency and algorithmic reproducibility**:
* **The Norm (`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`)**: Whenever an equation is well-specified in academic literature and implemented cleanly as expected without ambiguity or departure (e.g., Stefan-Boltzmann radiation, hydrostatic pressure, Henry's gas solubility, Stull wet-bulb, standard allometric power laws, Sloan-Buffett clathrate phase equilibria), it belongs **exclusively in the Canonical Equations Encyclopedia**. Direct scientific laws MUST NOT be logged as deviations.
* **The Exception Log (This Registry)**: This document is strictly reserved for things parameterized or formulated **not as originally expected by the literature**, specifically:
  1. **Under-Specified Formulations**: The published paper left the equation under-specified for macroscopic spatial mesh execution, requiring an unavoidable non-standard parameterization.
  2. **Unavoidable Algorithmic Departures**: Cases where we had no real choice but to deviate from the author's formulation (e.g., replacing computationally intractable 3D continuum field solvers with non-trivial $O(1)$ analytical closures, or replacing published discrete step cliffs with continuous $C^1$ relaxations to prevent numerical divergence).
* **Parsimony of Exceptions**: If models are implemented cleanly from first principles, entries in this registry must remain rare exceptions relative to the total equation catalog. A low entry count is proof of fidelity, not a lack of transparency.

---

## 🔬 2. Category A: Tier 1 Biophysical, Thermodynamic & Geophysical Model Adaptations

### Entry 1: Standard Dynamic Energy Budget (DEB) Cohort Aggregation & Starvation Hazard
* **Subsystem**: `DynamicEnergyBudgetEngine.java`
* **Canonical Literature**:
  * Kooijman, S. A. L. M. (2010). *Dynamic Energy Budget theory for metabolic organisation*. Cambridge University Press.
  * Jager, T., Martin, B. T., & Baveco, J. M. (2013). *Alternative angels: Predicting rethink dynamic energy budget theory*. Journal of Theoretical Biology, 328, 85–93.
  * Hatton, I. A., McCann, K. S., Fryxell, J. M., et al. (2015). *The predator-prey power law: Biomass scaling across terrestrial and aquatic biomes*. Science, 349(6252), aac6284.
  * Cahill, G. F. (1970). *Starvation in man*. New England Journal of Medicine, 282(12), 668–675.
* **Canonical Published Formulation**:
  * Microscopic individual differential system tracking individual structural volume $V$, reserve density $e$, and maturity $E_H$ with continuous developmental life stages (embryo $\to$ juvenile $\to$ adult); macro-herbivore community standing biomass scales sub-linearly with primary producer density $\rho_{\text{herb}} = c \cdot \rho_{\text{prod}}^{3/4}$ (Hatton et al. 2015).
* **Calibrated Ether Formulation**:
  * Scaled to mean demographic cohorts and macro-herbivore guilds per H3 cell ($O(1)$ CPU complexity):
    $$\frac{dV}{dt} = \frac{\kappa \dot{p}_C - [\dot{p}_M] V}{[E_G]}, \qquad \frac{dE}{dt} = \dot{p}_A - \dot{p}_C$$
    $$\dot{p}_R = \max(0.0, \; (1 - \kappa) \dot{p}_C - [\dot{p}_J]), \qquad \mu_{\text{starv}} = \mu_{\max} \cdot \left(\frac{\max(0, \Delta V / V_{\text{healthy}})}{w_{\text{crit}}}\right)^2$$
    $$\rho_{\text{herb}} = c_{\text{Hatton}} \cdot \left(\frac{B_{\text{natural}}}{A_{\text{cell}}}\right)^{3/4}, \qquad B_{\text{herb}} = \rho_{\text{herb}} \cdot A_{\text{cell}}$$
* **Parameters, Material Properties & Physical Units**:
  * $\kappa = 0.80$: Somatic allocation fraction [dimensionless, 80% soma / 20% reproduction].
  * $[E_G] = 28.0\text{ GJ/metric ton}$: Specific synthesis cost of structural biomass [$\text{GJ/t} \equiv \text{MJ/kg}$].
  * $[\dot{p}_M] = 2.25\text{ GJ/(person}\cdot\text{year)}$: Adult basal human somatic maintenance demand ($\approx 1475\text{ kcal/day}$).
  * $\{\dot{p}_{Am}\} = 4.80\text{ GJ/(person}\cdot\text{year)}$: Maximum ingestion saturation flux ($\approx 3140\text{ kcal/day}$).
  * $X_K = 1.20\text{ GJ/capita}$: Holling Type II half-saturation constant for consumable food.
  * $w_{\text{crit}} = 0.40$: Critical fractional lean structural mass loss triggering fatal starvation (Cahill 1970; Keys et al. 1950).
  * $T_A = 8000.0\text{ K}$: Arrhenius activation temperature for metabolic kinetics.
* **Justification for Deviation & Idiosyncrasy**:
  * Microscopic DEB tracks millions of discrete individuals. In Ether, cellular cohort aggregation maintains exact thermodynamic mass-energy conservation at $O(1)$ per cell. Replaces phenomenological Verhulst capacity ($K_{\text{pop}}$) and discrete step-cliff starvation with continuous $C^1$ structural tissue catabolism and quadratic actuarial hazard.
* **Empirical Validation**:
  * Validated on $>1\,000$ non-human animal species (`Add-my-Pet`); Minnesota Starvation Experiment (Keys et al. 1950); Cahill (1970).

---

### Entry 2: Metabolic Theory of Ecology (MTE) Multi-Taxon Allometry & Operative Thermal Kinetics
* **Subsystem**: `MetabolicEcologyEngine.java`
* **Canonical Literature**:
  * Brown, J. H., Gillooly, J. F., Allen, A. P., Savage, V. M., & West, G. B. (2004). *Toward a metabolic theory of ecology*. Ecology, 85(7), 1771–1789.
  * Gillooly, J. F., et al. (2001). *Effects of size and temperature on metabolic rate*. Science, 293(5538), 2248–2251.
* **Canonical Published Formulation**:
  * Whole-organism basal metabolic power: $B(M, T) = B_0 M^{3/4} \exp(-E_a / (k_B T))$, with $E_a \approx 0.65\text{ eV}$.
* **Calibrated Ether Formulation**:
  * Extended across heterogeneous spatial cells by establishing explicit homeothermic vs. ectothermic boundary conditions:
    $$B(M, T_{\text{op}}) = B_{0, \text{guild}} \cdot M^{3/4} \cdot \exp\left(-\frac{E_a}{k_B T_{\text{op}}}\right)$$
    with $T_{\text{op}} = 310.15\text{ K}$ (37°C core) for endotherms, and $T_{\text{op}} = T_{\text{surf, kelvin}}$ for ectotherms and plants.
* **Parameters, Material Properties & Physical Units**:
  * $k_B = 8.617333 \times 10^{-5}\text{ eV/K}$: Boltzmann constant.
  * $E_a = 0.65\text{ eV}$: Universal respiratory enzyme activation energy.
  * $B_{0,\text{endo}} = 4.85 \times 10^8\text{ W}\cdot\text{kg}^{-3/4}$: Endotherm metabolic normalization coefficient.
  * $B_{0,\text{ecto}} = 1.08 \times 10^8\text{ W}\cdot\text{kg}^{-3/4}$: Ectotherm vertebrate normalization coefficient.
  * $B_{0,\text{plant}} = 6.57 \times 10^7\text{ W}\cdot\text{kg}^{-3/4}$: Vascular plant normalization coefficient.
  * $r_0 = 5.5 \times 10^{10}\text{ yr}^{-1}\cdot\text{kg}^{1/4}$: Intrinsic maximum growth rate coefficient.
  * $t_0 = 1.8 \times 10^{-11}\text{ yr}\cdot\text{kg}^{-1/4}$: Generation time coefficient.
* **Justification for Deviation & Idiosyncrasy**:
  * Published MTE studies evaluate individual organisms in laboratory respiration chambers. In Ether, MTE unifies livestock fodder demand, wild megafauna carrying capacity, and demographic generation times directly from local temperature and body mass without arbitrary biome-specific tables.
* **Empirical Validation**:
  * Validated across 8 orders of biological magnitude (0.002 kg pygmy shrew to 5000 kg woolly mammoth, slope $0.750 \pm 0.001$).

---

### Entry 3: Ecological Stoichiometry Liebig Softmin & Atmospheric Carbon Dilution
* **Subsystem**: `EcologicalStoichiometryEngine.java`
* **Canonical Literature**:
  * Sterner, R. W., & Elser, J. J. (2002). *Ecological Stoichiometry: The Biology of Elements from Molecules to the Biosphere*. Princeton University Press.
  * Loladze, I. (2002, 2014). *Hidden shift of the ionome of plants with rising atmospheric CO2*. eLife, 3, e02245.
  * Myers, S. S., et al. (2014). *Increasing CO2 threatens human nutrition*. Nature, 510(7503), 139–142.
* **Canonical Published Formulation**:
  * Discontinuous Liebig Law of the Minimum: $Y = \min(R_1/k_1, R_2/k_2, \dots, R_n/k_n)$ with non-differentiable gradient jumps.
* **Calibrated Ether Formulation**:
  * Continuous $C^\infty$ softmin representation of multi-nutrient soil balance (N-P-K) coupled to atmospheric carbon dilution and Lavoisier-conserved SI caloric conversion ($e_{\text{grain}} = 15.0\text{ MJ/kg} = 0.015\text{ GJ/kg}$, debiting harvested mass from `biomassAgriculture`):
    $$L(\text{N}, \text{P}, \text{K}) = \text{softmin}\left(\frac{\text{N}}{\text{N}_0}, \; \text{softmin}\left(\frac{\text{P}}{\text{P}_0}, \; \frac{\text{K}}{\text{K}_0}, \; 8.0\right), \; 8.0\right)$$
    $$D_{\text{protein}}(\text{CO}_2) = 1.0 - \delta_{\max} \cdot \tanh\left(\frac{\max(0, \text{CO}_2 - 280.0)}{350.0}\right)$$
    $$\Delta M_{\text{harv}} = \min(0.50\,B_{\text{agr}},\; 0.12\,B_{\text{agr}}\,L\,D_{\text{protein}}\,\Delta t), \qquad \Delta F = \Delta M_{\text{harv}} \cdot 0.015\text{ GJ/kg}, \qquad \Delta B_{\text{agr}} = 0.20\,B_{\text{agr}}(L D_{\text{protein}} - 1)\Delta t - \Delta M_{\text{harv}}$$
* **Parameters, Material Properties & Physical Units**:
  * $\text{C:N:P} = 106:16:1$ molar ($\approx 41.1 : 7.23 : 1.0$ by mass): Redfield marine invariant.
  * Plant foliar C fraction $= 0.45$: Terrestrial autotroph carbon mass fraction.
  * $\text{N}_{\text{demand}} = 20.0\text{ kg N / ton dry biomass}$: Crop nitrogen stoichiometric demand.
  * $\text{P}_{\text{demand}} = 2.5\text{ kg P / ton dry biomass}$: Crop phosphorus stoichiometric demand.
  * $\text{CO}_{2,\text{preind}} = 280.0\text{ ppm}$: Pre-industrial atmospheric carbon dioxide baseline.
  * $\delta_{\max} = 0.12$: Maximum nutritional protein dilution penalty ($12\%$) under high $\text{CO}_2$ enrichment.
* **Justification for Deviation & Idiosyncrasy**:
  * Replaces discrete non-smooth $\min(\dots)$ with a differentiable softmin function, preventing artificial numerical oscillations in Euler/Runge-Kutta numerical integration while embedding Loladze's global agronomic protein dilution.
* **Empirical Validation**:
  * FACE (Free-Air $\text{CO}_2$ Enrichment) global experiments; Myers et al. (2014); Loladze (2014).

---

### Entry 4: Constructal Law Locomotion Allometry & Catchment Stream Length Closure
* **Subsystem**: `ConstructalFlowArchitectureEngine.java`
* **Canonical Literature**:
  * Bejan, A. (1996, 2016). *Shape and Structure, from Engineering to Nature*. Cambridge University Press.
  * Bejan, A., & Marden, J. H. (2006). *Unifying constructal theory for scale effects in running, swimming and flying*. Journal of Experimental Biology, 209(2), 238–248.
  * Hack, J. T. (1957). *Studies of longitudinal stream profiles in Virginia and Maryland*. USGS Professional Paper 294-B.
* **Canonical Published Formulation**:
  * Constructal cruising speed $V \sim \rho_{\text{body}}^{-1/6} g^{1/2} M^{1/6}$; Murray's vascular bifurcation $d_0^3 = \sum d_i^3$; Hack's river scaling $L \sim A^{0.57}$.
* **Calibrated Ether Formulation**:
  * Locomotion speed evaluated continuously over medium friction to determine maximum daily movement radius and terrain transit resistance:
    $$V_{\text{cruise}}(M, \text{medium}) = C_{\text{medium}} \cdot M^{1/6}$$
    $$L_{\text{stream}} = c_{\text{hack}} \cdot A_{\text{drainage}}^{0.57}$$
* **Parameters, Material Properties & Physical Units**:
  * $C_{\text{air}} = 15.0\text{ m/s}\cdot\text{kg}^{-1/6}$: Aerial flying constructal coefficient.
  * $C_{\text{land}} = 3.85\text{ m/s}\cdot\text{kg}^{-1/6}$: Terrestrial running constructal coefficient.
  * $C_{\text{water}} = 1.25\text{ m/s}\cdot\text{kg}^{-1/6}$: Aquatic swimming constructal coefficient.
  * $c_{\text{hack}} = 1.40\text{ km}^{-0.14}$: Fluvial mainstream length coefficient.
  * $\mu_{\text{friction,min}} = 0.20$: Minimum physical terrain transit friction.
* **Justification for Deviation & Idiosyncrasy**:
  * Replaces arbitrary pathfinding terrain cost tables with continuous thermodynamic allometry coupling animal body mass and river geometry directly to energy dissipation minimization.
* **Empirical Validation**:
  * Bejan & Marden (2006); Hack (1957); Murray (1926).

---

### Entry 5: Unified Neutral Theory of Biodiversity (UNTB) Power-Law Extinction Debt
* **Subsystem**: `UnifiedNeutralBiodiversityEngine.java`
* **Canonical Literature**:
  * Hubbell, S. P. (2001). *The Unified Neutral Theory of Biodiversity and Biogeography*. Princeton University Press.
  * Volkov, I., et al. (2003). *Neutral theory and relative species abundance in ecology*. Nature, 424(6952), 1035–1037.
* **Canonical Published Formulation**:
  * Metacommunity master equation for zero-sum ecological drift and species-area scaling $S(A) = c A^z$ with $z \approx 0.25$.
* **Calibrated Ether Formulation**:
  * Continuous species richness retention index $\mathcal{B}$ evaluated from standing natural biomass stock:
    $$\theta = 2 J_M \nu, \qquad \mathcal{B} = \left(\frac{B_{\text{nat}}}{B_{\text{pristine}}}\right)^z$$
* **Parameters, Material Properties & Physical Units**:
  * $z = 0.25$: Canonical Preston-Hubbell species-area scaling exponent.
  * $\nu = 1.0 \times 10^{-6}$: Baseline speciation/point-mutation rate per birth.
  * $B_{\text{ref}} = 100.0\text{ metric tons/cell}$: Reference climax forest biomass density.
* **Justification for Deviation & Idiosyncrasy**:
  * Eliminates discrete species enumeration in favor of continuous power-law retention $\mathcal{B} \in [0, 1]$, capturing non-linear extinction debt buffering (where 50% habitat loss retains ~84% of species richness) at $O(1)$ CPU complexity.
* **Empirical Validation**:
  * Barro Colorado Island 50ha tropical forest census; Hubbell (2001); Preston (1962).

---

### Entry 6: Maximum Entropy Production Principle (MEPP) Climate Carnot Dissipation
* **Subsystem**: `MaximumEntropyProductionEngine.java`
* **Canonical Literature**:
  * Kleidon, A. (2010, 2016). *Thermodynamic Foundations of the Earth System*. Cambridge University Press.
  * Priestley, C. H. B., & Taylor, R. J. (1972). *On the assessment of surface heat flux and evaporation using large-scale parameters*. Monthly Weather Review, 100(2), 81–92.
* **Canonical Published Formulation**:
  * Planetary heat engine maximizing work dissipation: $G_{\text{ke}} = \frac{1}{4} \eta_{\text{carnot}} I_{\text{net}}$, $\eta_{\text{carnot}} = (T_s - T_e)/T_s$.
* **Calibrated Ether Formulation**:
  * Closed-form analytical evaluation of atmospheric kinetic wind power and equilibrium precipitation directly from astronomical latitudinal irradiance $I_{\text{surf}}(\phi) = S_0(1 - \alpha_{\text{p}})\max(0.08,\cos\phi)$ [$\text{W/m}^2$] (decoupled from anthropogenic `WorldBuffer.energySolar` [$\text{GJ/yr}$]):
    $$\eta_{\text{carnot}} = \frac{\max(0, T_{\text{surf, K}} - T_e)}{T_{\text{surf, K}}}, \qquad P_{\text{precip, eq}} = 1.26 \cdot \frac{\Delta}{\Delta + \gamma} \cdot \frac{R_{\text{net}}}{\lambda_v} \quad [\text{mm/year}]$$
* **Parameters, Material Properties & Physical Units**:
  * $T_e = 255.0\text{ K}$: Planetary radiative equilibrium temperature ($\approx -18^\circ\text{C}$).
  * $\lambda_v = 2.45 \times 10^6\text{ J/kg}$: Latent heat of vaporization of water.
  * $\alpha_{\text{PT}} = 1.26$: Canonical Priestley-Taylor advection coefficient.
  * $P_{\text{wind,max}} = 1.25\text{ W/m}^2$: Characteristic kinetic dissipation boundary.
* **Justification for Deviation & Idiosyncrasy**:
  * Bypasses heavy 3D atmospheric Navier-Stokes numerical solvers by utilizing non-equilibrium thermodynamic maximization to derive realistic planetary wind power ($2\text{--}4\text{ W/m}^2$) and mean annual precipitation (~1000 mm/year) at $O(1)$ complexity.
* **Empirical Validation**:
  * Kleidon & Miller (2011); ERA5 ECMWF Reanalysis; Priestley & Taylor (1972).

---

### Entry 7: Stommel AMOC 2-Box Dynamics, Clausius-Clapeyron Moisture & Bipolar Seesaw
* **Subsystem**: `ThermohalineStommelAMOCEngine.java`
* **Canonical Literature**:
  * Stommel, H. (1961). *Thermohaline convection with two stable regimes of flow*. Tellus, 13(2), 224–230.
  * Broecker, W. S. (1998). *Paleocean circulation during the last deglaciation: A bipolar seesaw?* Paleoceanography, 13(2), 119–121.
  * EPICA Community Members (2006). *One-to-one coupling of glacial climate variability in Greenland and Antarctica*. Nature, 444, 195–198.
* **Canonical Published Formulation**:
  * Non-dimensional 2-box differential system with linear thermal drive and non-linear quadratic haline counter-drive: $\dot{T} = -\tau_T^{-1}(T - T_e) - |q| T$, $\dot{S} = F - \tau_S^{-1}(S - S_e) - |q| S$.
* **Calibrated Ether Formulation**:
  * Dimensional Sverdrup formulation with dynamic sub-cycling ($dt \le 1.0\text{ yr}$), atmospheric Clausius-Clapeyron moisture feedback ($F_{\text{fresh}} \propto q/q_0$), and Broecker/EPICA bipolar seesaw:
    $$q = C \cdot \left[ \alpha_T (T_{\text{eq}} - T_{\text{subpolar}}) - \beta_S (S_{\text{eq}} - S_{\text{subpolar}}) \right]$$
    $$\Delta T_{\text{North}} = -8.0 \cdot \frac{1}{1 + \exp\left( \frac{q - 8.0}{1.5} \right)}, \qquad \Delta T_{\text{South}} = -0.22 \cdot \Delta T_{\text{North}}$$
* **Parameters, Material Properties & Physical Units**:
  * $q_0 = 18.2\text{ Sv}$: Pre-industrial active AMOC overturning volume flux ($1\text{ Sv} = 10^6\text{ m}^3/\text{s}$).
  * $\tau_T = 20.0\text{ yr}$: Thermal atmospheric relaxation timescale.
  * $\tau_{\text{diff}} = 500.0\text{ yr}$: Slow haline diffusion timescale (creates multi-centennial salt memory).
  * $V_p = 250.0\text{ Sv}\cdot\text{yr}$: Polar ocean box effective volume.
  * $C = 11\,500.0$: Convective density-flux scaling constant.
  * $\eta_{\text{gyre}} = 0.55$: Wind-driven subtropical-subpolar gyre exchange coefficient.
  * Broecker seesaw ratio $= -0.22$: Compensatory Antarctic warming fraction relative to North Atlantic cooling.
* **Justification for Deviation & Idiosyncrasy**:
  * Stommel's 1961 paper used dimensionless variables and fixed freshwater flux. Ether introduces dimensional physical units (Sv, PSU, °C), coupled Milankovitch meltwater forcing, and atmospheric Clausius-Clapeyron rainfall reduction ($q \to 0 \implies \text{cold stadial drought}$) enabling realistic recovery after Heinrich events without artificial triggers.
* **Empirical Validation**:
  * EPICA Dome C ice core; Greenland GISP2/NGRIP $\delta^{18}\text{O}$; Stommel (1961); Rahmstorf (1995).

---

### Entry 8: Charney Dynamic Vegetation-Albedo Bistability & Sahara Cusp Catastrophe
* **Subsystem**: `CharneyVegetationAlbedoEngine.java`
* **Canonical Literature**:
  * Charney, J. G. (1975). *Dynamics of deserts and drought in the Sahel*. Q. J. R. Meteorol. Soc., 101, 193–202.
  * Claussen, M., et al. (1999). *Simulation of an abrupt change in Saharan vegetation in the mid-Holocene*. Geophysical Research Letters, 26(14), 2037–2040.
  * deMenocal, P., et al. (2000). *Abrupt onset and termination of the African Humid Period*. Quaternary Science Reviews, 19(1-5), 347–361.
* **Canonical Published Formulation**:
  * Heavy coupled atmosphere-ocean-vegetation models (AGCMs) requiring Navier-Stokes wind and moisture advection.
* **Calibrated Ether Formulation**:
  * Analytical fold bifurcation coupling vegetation fraction $V$, dynamic albedo $\alpha(V)$, and convective precipitation to the 23,000-year Milankovitch orbital precession cycle:
    $$\alpha(V) = 0.38 - 0.21 V, \qquad P_{\text{convect}}(V) = \beta_{\text{charney}} \cdot (0.38 - \alpha(V)) \cdot P_{\text{baseline}}$$
    $$\frac{dV}{dt} = \frac{V^*(P) - V}{\tau_V}, \qquad V^*(P) = \frac{1}{1 + \exp\left(-\frac{P - P_{50}}{s_P}\right)}$$
* **Parameters, Material Properties & Physical Units**:
  * $\alpha_{\text{sand}} = 0.38$: Bare desert sand albedo [dimensionless].
  * $\alpha_{\text{veg}} = 0.17$: Dense savanna/grassland albedo [dimensionless].
  * $\tau_V = 10.0\text{ yr}$: Vegetation succession and soil memory relaxation timescale.
  * $P_{50} = 200.0\text{ mm/yr}$: Half-saturation precipitation for vegetative cover.
  * $s_P = 50.0\text{ mm/yr}$: Logistic transition width.
  * $A_{\text{orbital}} = 180.0\text{ mm/yr}$: Maximum monsoonal precipitation anomaly driven by summer insolation.
  * $\beta_{\text{charney}} = 1.65$: Dimensionless Charney bio-geophysical feedback gain.
* **Justification for Deviation & Idiosyncrasy**:
  * Replaces 3D general circulation atmospheric models with an analytical cusp catastrophe that accurately reproduces the rapid collapse of the "Green Sahara" at $\approx -4100\text{ BCE}$ into hyper-arid desert without arbitrary date checks.
* **Empirical Validation**:
  * Lake Mega-Chad paleo-shorelines; Mauritanian marine dust cores (ODP Site 658C); Claussen et al. (1999).

---

### Entry 9: Howard-Kerby / Whipple-Tucker Stream Power Incision & Bagnold Siltation
* **Subsystem**: `StreamPowerErosionEngine.java`
* **Canonical Literature**:
  * Howard, A. D., & Kerby, G. (1983). *Channel changes in badlands*. Geol. Soc. Am. Bull., 94(6), 739–752.
  * Whipple, K. X., & Tucker, G. E. (1999). *Dynamics of the stream-power river incision model*. J. Geophys. Res., 104(B8), 17661–17674.
  * Bagnold, R. A. (1966). *An approach to the sediment transport problem from general physics*. USGS Professional Paper 422-I.
* **Canonical Published Formulation**:
  * Recursive topographic digital elevation model (DEM) flow routing: $\dot{E} = K A^m S^n$ requiring global recursive flow accumulation trees ($O(N \log N)$ or $O(N^2)$).
* **Calibrated Ether Formulation**:
  * Closed-form analytical closure using Hack's Law ($L \propto A^{0.57}$) to estimate upstream drainage area $A_{\text{drainage}}$ in $O(1)$ directly from stream length, coupled to root cohesion protection and downstream alluvial silt deposition:
    $$\dot{E} = K_{\text{erod}} A_{\text{drainage}}^m S^n \cdot \exp\left(-\frac{B_{\text{nat}}}{B_{\text{root}}}\right) \quad [\text{m/year}]$$
    $$\Delta \text{SOC}_{\text{alluvial}} = q_{\text{sed}} \cdot C_{\text{org}} \cdot \frac{A_{\text{cell}}}{A_{\text{floodplain}}} \quad [\text{g/kg}]$$
* **Parameters, Material Properties & Physical Units**:
  * $m = 0.50$: Drainage area exponent (fluvial stream power scaling).
  * $n = 1.00$: Channel slope gradient exponent.
  * $K_{\text{erod}} = 1.2 \times 10^{-4}\text{ m}^{0.0}\text{ yr}^{-1}$: Bedrock/alluvial erodibility coefficient for sedimentary catchments.
  * $B_{\text{root}} = 3000.0\text{ kg/ha}$: Root cohesion reference biomass density.
  * $C_{\text{org}} = 0.025$: Organic carbon mass fraction in transported river sediment ($2.5\%$).
* **Justification for Deviation & Idiosyncrasy**:
  * High-resolution DEM flow accumulation is computationally impossible across a planetary H3 mesh over multi-millennial timescales. The constructal Hack closure delivers first-principles fluvial erosion and natural floodplain soil replenishment (Nile, Tigris-Euphrates, Yellow River) in $O(1)$ per cell.
* **Empirical Validation**:
  * Whipple & Tucker (1999); USDA alluvial soil records; Montgomery (2007).

---

## 🏛️ 3. Category B: Tier 2 Cliodynamic, Macroeconomic & Spatial Mesh Adaptations

### Entry 10: Garrett-Lotka Emergent Exergy-Capital Normalization ($\Phi_{\text{exergy}}$)
* **Subsystem**: `H3CellPhysicsAndStateHelper.java`, `WorldBuffer.java`
* **Canonical Literature**:
  * Garrett, T. J. (2011). *Are there basic physical constraints on future anthropogenic emissions of carbon dioxide?* Climatic Change, 104(3), 437–455.
  * Lotka, A. J. (1922). *Contribution to the energetics of evolution*. PNAS, 8(6), 147–151.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Canonical Published Formulation**:
  * Garrett models global civilization as a thermodynamic heat engine where cumulative economic wealth is coupled to primary power consumption via constant $\eta \approx 9.7\text{ mW/\$}$. Lotka establishes the maximum power principle of biological evolution.
* **Calibrated Ether Formulation**:
  $$\Phi_{\text{exergy}}(p, k_{\text{pc}}) = 0.85 + 0.50 \log_{10}\left(1.0 + \frac{k_{\text{active}}}{k_0}\right) + 0.70 \log_{10}\left(1.0 + \frac{\operatorname{softplus}(p - p_{\text{somatic}}, 5.0)}{p_0}\right)$$
* **Parameters & Physical Units**:
  * $p$: Total mechanical, thermal, and electrical power per capita [$\text{W/capita}$].
  * $B_{\text{basal}}(65\,\text{kg}) = 4.24 \times 65^{3/4} \approx 97.0\,\text{W/capita}$: Kleiber basal endosomatic metabolic rate (`OrganismBiophysicalProfile.HOMO_SAPIENS.basalMetabolicPowerWatts()`), unified across cellular and DAG telemetry evaluators.
  * $p_{\text{somatic}} = 348.0\text{ W/capita}$: Basal human metabolic rate ($97.0\text{ W}$), somatic work ($39.0\text{ W}$), plus domestic wood hearth heating ($212.0\text{ W}$) (Smil 2017).
  * $p_0 = 250.0\text{ W/capita}$: Characteristic scale of agrarian draft animal traction and water/wind milling.
  * $k_{\text{pc}} = K / N$: Total structural and mechanical physical capital stock per person [$\text{kg/capita}$].
  * $k_0 = 120.0\text{ kg/capita}$: Characteristic mass scale of traditional agrarian village infrastructure (tools, granaries, masonry) (Krausmann et al. 2017).
  * $k_{\text{active}}$: Active capital coupled to power dissipation ($k_{\text{active}} \le k_{\max}(p)$), preventing post-collapse depopulation divergence ($N \to 0$).
* **Justification for Deviation**:
  * Eliminates arbitrary state-level technology variables (`AGENTS.md` §10). Maps the continuous physical state $(p, k_{\text{pc}})$ to a dimensionless scale:
    * $\Phi_{\text{exergy}} \approx 0.85$: Somatic Paleolithic foraging band ($k_{\text{pc}} < 10\text{ kg/cap}$, $p = 348\text{ W/cap}$).
    * $\Phi_{\text{exergy}} \approx 1.05\text{--}1.15$: Classical Antiquity / High Middle Ages ($k_{\text{pc}} \approx 300\text{ kg/cap}$, $p \approx 450\text{ W/cap}$).
    * $\Phi_{\text{exergy}} \approx 1.25$: Early Modern era (1800 CE, $k_{\text{pc}} \approx 900\text{ kg/cap}$, $p \approx 550\text{ W/cap}$).
    * $\Phi_{\text{exergy}} \approx 1.95$: Industrial era (1914 CE, $k_{\text{pc}} \approx 3600\text{ kg/cap}$, $p \approx 1200\text{ W/cap}$).
* **Empirical Validation**:
  * Garrett (2011); Krausmann et al. (2017); Maddison Project Database (2020).

---

### Entry 11: Non-Linear Boussinesq Groundwater Mesh Closure & Darcy Transmissivity on H3 Hexagons
* **Subsystem**: `AquiferDepletionEngine.java`
* **Canonical Literature**:
  * Wada, Y., et al. (2010). *Global depletion of groundwater resources*. Geophysical Research Letters, 37(20), L20402.
  * Margat, J., & van der Gun, J. (2013). *Groundwater around the World: A Geographic Synopsis*. CRC Press / UNESCO.
* **Canonical Published Formulation**:
  * Continuous 2D non-linear Boussinesq PDE $\nabla \cdot (T \nabla h) + R - W = S_s \frac{\partial h}{\partial t}$ solved on structured Cartesian rasters ($0.5^\circ \times 0.5^\circ$) or macro-watershed compartments with volumetric withdrawals $V_{\text{abs}}$ [$\text{km}^3/\text{year}$].
* **Calibrated Ether Formulation**:
  * Discretized across spherical geodesic hexagonal neighbors with distance-weighted Darcy transmissivity and resolution-invariant intensive withdrawal depth:
    $$W_i = \frac{N_i \cdot w_{\text{crop}}(k_{\text{pc}})}{A_{\text{cell}}} \cdot \eta_{\text{pump}}(p, k_{\text{pc}}) \quad [\text{mm/year}]$$
    $$\frac{\partial h_i}{\partial t} = \frac{1}{S_s} \left[ \sum_{j \in \text{H3Neighbors}} T_{ij} \frac{h_j - h_i}{d_{ij}} + R_{\text{recharge}, i} - W_i \right]$$
* **Parameters & Physical Units**:
  * $N_i$: Cell population [inhabitants].
  * $w_{\text{crop}}$: Per-capita agricultural water requirement [$\text{m}^3/(\text{person}\cdot\text{year})$].
  * $A_{\text{cell}}$: Hexagonal cell surface area [$\text{m}^2$] ($A_{\text{cell}} \approx 1.1 \times 10^{10}\text{ m}^2$ at H3 Resolution 4).
  * $W_i$: Local water withdrawal depth [$\text{mm/year}$].
  * $S_s = 0.01$: Specific storage coefficient [dimensionless].
  * $T_{ij}$: Aquifer hydraulic transmissivity [$\text{m}^2/\text{day}$].
  * $d_{ij}$: Inter-cell geodesic centroid distance [$\text{m}$].
  * $h$: Piezometric hydraulic head [$\text{m}$].
* **Justification for Deviation**:
  * Transposes continuous 2D Boussinesq aquifer flow onto an unstructured discrete H3 spherical hexagonal mesh via distance-weighted inter-cell transmissivity $T_{ij}/d_{ij}$ and intensive withdrawal depth $W_i$, ensuring rigorous local and regional mass conservation without requiring heavy iterative sparse-matrix PDE solvers across 288,000 cells per tick.
* **Empirical Validation**:
  * UNESCO WHYMAP; FAO AQUASTAT; Wada et al. (2010).

---

### Entry 12: Arthur Combinatorial Capital Evolution Dynamics
* **Subsystem**: `ArthurCombinatorialTechnologyEngine.java`
* **Canonical Literature**:
  * Arthur, W. B. (2009). *The Nature of Technology: What It Is and How It Evolves*. Free Press.
  * Arthur, W. B., & Polak, W. (2006). *The evolution of technology within a simple computer model*. Complexity, 11(5), 23–31.
  * Kremer, M. (1993). *Population Growth and Technological Change: One Million B.C. to 1990*. Quarterly Journal of Economics, 108(3), 681–716.
* **Canonical Published Formulation**:
  * Arthur formalizes technology evolution over an abstract graph of discrete functional modules $M(t)$, with recombinant growth $dM/dt \propto M^\alpha$ ($\alpha > 1$).
* **Calibrated Ether Formulation**:
  $$\frac{dK}{dt} = \mu_{\text{comb}} \cdot N \cdot \left(1.0 + \frac{k_{\text{pc}}}{k_{\text{ref}}}\right)^\alpha \cdot \ln\left(1.0 + \frac{N}{N_0}\right) \cdot \frac{1}{1.0 + k_{\text{pc}} / k_{\max}} - \delta_{\text{wear}} \cdot K$$
* **Parameters & Physical Units**:
  * $K$: Embodied physical tooling and machinery capital [$\text{kg}$].
  * $k_{\text{pc}} = K / N$: Capital density [$\text{kg/capita}$].
  * $k_{\text{ref}} = 120.0\text{ kg/capita}$: Normalization scale for traditional agrarian tool mass.
  * $\alpha = 1.35$: Superlinear combinatorial exponent (Arthur & Polak 2006).
  * $N_0 = 100.0\text{ persons}$: Characteristic cooperative division-of-labor network threshold (Henrich 2004).
  * $k_{\max} = 50\,000.0\text{ kg/capita}$: Thermodynamic saturation scale of physical machinery.
  * $\mu_{\text{comb}} = 0.045\text{ kg/(person}\cdot\text{year)}$: Baseline physical tool synthesis rate.
  * $\delta_{\text{wear}} = 0.008\text{ year}^{-1}$: Structural obsolescence and physical replacement wear rate.
* **Justification for Deviation**:
  * Arthur's canonical model operates on abstract idea counts. In Ether, technological capability must be embodied in physical mass $K$ ($\text{kg}$) to obey thermodynamic conservation laws. The term $(1 + k_{\text{pc}}/k_{\text{ref}})^\alpha$ maps combinatorial variety directly onto the density of physical tools.
* **Empirical Validation**:
  * Krausmann et al. (2017); Kremer (1993); Maddison Project Database (2020).

---

### Entry 13: Hotelling Chalcolithic Smelting Envelope
* **Subsystem**: `HotellingResourceDepletionEngine.java`
* **Canonical Literature**:
  * Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137–175.
  * Radivojević, M., et al. (2010). *On the origins of extractive metallurgy: new evidence from Europe*. Journal of Archaeological Science, 37(11), 2775–2787.
* **Canonical Published Formulation**:
  * Standard resource depletion assumes extraction occurs across all $t \ge 0$.
* **Calibrated Ether Formulation**:
  $$E_{\text{kiln}}(k_{\text{pc}}) = \frac{k_{\text{pc}}^3}{k_{\text{pc}}^3 + k_{\text{chalcolithic}}^3}, \qquad k_{\text{chalcolithic}} = 65.0\text{ kg/capita}$$
  $$R_{\text{extract}} = 0.02 \cdot N \cdot E_{\text{kiln}}(k_{\text{pc}}) \cdot 2.5 \tanh\!\left(\frac{\ln(1.0 + k_{\text{pc}}/50.0)}{3.5}\right) \cdot \frac{M_{\text{ore}}}{M_{\text{ore}} + 100.0}$$
* **Parameters & Physical Units**:
  * $E_{\text{kiln}}$: Dimensionless furnace activation factor [$0.0 - 1.0$].
  * $k_{\text{chalcolithic}} = 65.0\text{ kg/capita}$: Physical mass of refractory clay, bellows, and tuyères required for pyrometallurgical reduction.
  * $R_{\text{extract}}$: Ore extraction rate [$\text{metric tons/year}$].
  * $M_{\text{ore}}$: Remaining in-situ mineral ore reserve [$\text{metric tons}$].
* **Justification for Deviation**:
  * In simulations spanning $-100\,000\text{ BP}$ to present, unbounded Hotelling extraction would deplete copper and iron ores during the Pleistocene. The third-order Hill function $E_{\text{kiln}}(k_{\text{pc}})$ enforces zero metal smelting for Paleolithic hunter-gatherers ($k_{\text{pc}} \approx 3\text{ kg/cap} \implies E_{\text{kiln}} < 10^{-4}$) and smoothly activates during the Chalcolithic without date checks.
* **Empirical Validation**:
  * Radivojević et al. (2010); Tylecote (1992); USGS MRDS.

---

### Entry 14: Shannon-Brillouin Physical Carrier Bandwidth
* **Subsystem**: `InformationEntropyEngine.java`
* **Canonical Literature**:
  * Shannon, C. E. (1948). *A Mathematical Theory of Communication*. Bell System Technical Journal, 27(3), 379–423.
  * Brillouin, L. (1956). *Science and Information Theory*. Academic Press.
* **Canonical Published Formulation**:
  * Hartley-Shannon channel capacity: $C = B \log_2(1 + S/N)$ [$\text{bits/second}$] for abstract transmission channels.
* **Calibrated Ether Formulation**:
  $$B_{\text{carrier}}(k_{\text{pc}}, p) = B_0 + \alpha_k \cdot k_{\text{pc}} + \alpha_p \cdot (\operatorname{softplus}(p - 348.0, 5.0))^2 \quad [\text{Hz}]$$
  $$C = B_{\text{carrier}}(k_{\text{pc}}, p) \cdot \log_2\!\left(1.0 + \frac{10.0}{\text{Friction}_{\text{spatial}}}\right) \quad [\text{bits/second}]$$
* **Parameters & Physical Units**:
  * $B_{\text{carrier}}$: Physical communication channel bandwidth [$\text{Hz}$].
  * $B_0 = 1.0\text{ Hz}$: Acoustic oral and runner courier baseline.
  * $\alpha_k = 0.05\text{ Hz}\cdot\text{capita/kg}$: Bandwidth expansion from physical road, postal, and printing capital.
  * $\alpha_p = 0.002\text{ Hz}/(\text{W/capita})^2$: Bandwidth expansion from electromagnetic power grids.
  * $\text{Friction}_{\text{spatial}}$: Terrain, distance, and linguistic boundary resistance [dimensionless].
* **Justification for Deviation**:
  * Replaces discrete media classifications with a continuous physical relation linking channel capacity directly to structural infrastructure ($k_{\text{pc}}$) and power dissipation ($p$).
* **Empirical Validation**:
  * Shannon (1948); Buringh & van Zanden (2009); ITU.

---

### Entry 15: Turchin SDT Spatial Tensor Normalization
* **Subsystem**: `TurchinGoldstoneSDTEngine.java`, `SimulationMetricsAggregator.java`
* **Canonical Literature**:
  * Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American Polity*. Beresta Books.
  * Goldstone, J. A. (1991). *Revolution and Rebellion in the Early Modern World*. University of California Press.
* **Canonical Published Formulation**:
  * Canonical SDT multiplies dimensionless macro ratios:
    $$\Psi = \text{MMP} \cdot \text{EMP} \cdot \text{SFD} = \left(\frac{w_0}{w} \frac{N}{K}\right) \cdot \left(\frac{E}{N} \frac{y_e}{y_0}\right) \cdot \left(\frac{\text{Debt}}{\text{Rev}}\right)$$
* **Calibrated Ether Formulation**:
  $$\text{MMP}_i = \max\!\left(0.2,\; \frac{1.9}{\max(0.2, E_{\text{cal},i} / N_i)} \cdot \tanh\!\left(\frac{\rho_i}{150.0}\right)\right)$$
  $$\text{EMP}_i = \max\!\left(0.2,\; \left(\frac{\max(0.0, \text{Gini}_i - 0.25)}{0.20}\right)^2\right)$$
  $$\text{SFD}_i = \max\!\left(0.2,\; 0.4 \cdot \frac{1.9}{\max(0.2, E_{\text{cal},i} / N_i)} \cdot \tanh\!\left(\frac{k_{\text{pc},i}}{800.0}\right)\right), \qquad \Psi_i = \text{MMP}_i \cdot \text{EMP}_i \cdot \text{SFD}_i$$
* **Parameters & Physical Units**:
  * $\Psi_i$: Cellular Structural-Demographic Political Stress Index [dimensionless].
  * $E_{\text{cal},i} / N_i$: Total available edible caloric stock per person [$\text{GJ/(capita}\cdot\text{yr)}$] via `WorldBufferComponentAdapter.computeAvailableEdibleCaloricGJ` (`Contract 2 & 4`), normalized against subsistence benchmark $1.9\text{--}3.362\,\text{GJ/(capita}\cdot\text{yr)}$.
  * $\rho_i = N_i / A_{\text{cell},i}$: Local geodesic population density [$\text{hab/km}^2$].
  * $\text{Gini}_i$: Local inequality coefficient [$0.0 - 1.0$].
* **Justification for Deviation**:
  * Canonical SDT formulas produce unbounded dimensionless ratios when applied to local grid cells. The calibrated formulation maps SDT continuously onto the standard Seshat index scale $[0, 100]$:
    * Stable integrative empires: $\Psi \in [40, 55]$.
    * Acute revolutionary / state breakdown crisis: $\Psi \in [80, 100]$.
* **Empirical Validation**:
  * Seshat Global History Databank; Turchin & Nefedov (2009) *Secular Cycles*.

---

### Entry 16: USLE Topsoil Erosion Agrarian Ard & Mechanized Tillage Activation
* **Subsystem**: `PedologyKernel.java` & `CliodynamicFormulasRegistry.java`
* **Canonical Literature**:
  * Montgomery, D. R. (2007). *Soil erosion and agricultural sustainability*. PNAS, 104(33), 13268–13272.
  * Wischmeier, W. H., & Smith, D. D. (1978). *Predicting rainfall erosion losses*. USDA Agricultural Handbook No. 537.
* **Canonical Published Formulation**:
  * Universal Soil Loss Equation: $A = R \cdot K_{\text{soil}} \cdot LS \cdot C \cdot P$ [$\text{t/ha/year}$].
* **Calibrated Ether Formulation**:
  $$E_{\text{soil}}(\Phi) = \operatorname{clamp}\!\left( 23.5 \tanh\!\left(\max(0.0, \Phi_{\text{exergy}} - 0.93)^{1.30} \cdot 2.4\right) + \frac{42.0}{1.0 + \exp\!\left(-\frac{\Phi_{\text{exergy}} - 2.05}{0.25}\right)}, \; 0.0, \; 75.0 \right)$$
* **Parameters & Physical Units**:
  * $E_{\text{soil}}$: Topsoil loss rate [$\text{metric tons/ha/year}$].
  * $0.03\text{ t/ha/year}$: Pristine Holocene background geological weathering rate (Montgomery 2007).
  * $\Phi_{\text{exergy}}$: Garrett-Lotka Exergy-Capital Index [dimensionless].
  * $C_{\text{slope}}$: Topographic slope factor derived from elevation gradients.
* **Justification for Deviation**:
  * Standard USLE models calculate erosion directly from active agricultural land practices. In deep-time simulations, evaluating agricultural plowing without an agrarian threshold would impose modern or historical erosion rates on Pleistocene hunter-gatherers. The threshold $\Phi_{\text{exergy}} \ge 0.88$ preserves pristine geological erosion for foragers while activating human topsoil loss upon the adoption of animal-drawn ards and cultivation.
* **Empirical Validation**:
  * Montgomery (2007); FAO (2015); HYDE 3.4.

---

### Entry 17: Krugman Core-Periphery Local Ring-1 + Mean-Field H3 Discretization
* **Subsystem**: `KrugmanCorePeripheryEngine.java`
* **Canonical Literature**:
  * Krugman, P. (1991). *Increasing Returns and Economic Geography*. Journal of Political Economy, 99(3), 483–499.
  * Fujita, M., Krugman, P., & Venables, A. J. (1999). *The Spatial Economy: Cities, Regions, and International Trade*. MIT Press.
* **Canonical Published Formulation**:
  * All-to-all $R$-region Dixit-Stiglitz price index $G_i = [\sum_{j=1}^R \lambda_j T_{ji}^{1-\sigma}]^{\frac{1}{1-\sigma}}$ and nominal wage $w_i = [\sum_{j=1}^R Y_j T_{ij}^{1-\sigma} G_j^{\sigma-1}]^{\frac{1}{\sigma}}$, which requires an $O(N^2)$ pairwise matrix across $N \approx 2.88 \times 10^5$ H3 cells.
* **Calibrated Ether Formulation**:
  * Evaluates Krugman's exact Dixit-Stiglitz price index $G_i$, nominal wage $w_i$, and real wage $\omega_i = w_i G_i^{-\mu_M}$ ($\sigma = 4.0$, $\mu_M = 0.40$) over the local hexagonal trading set $\mathcal{N}_i = \{i\} \cup \text{Ring}_1(i) \cup \{0_{\text{RoW}}\}$ using geodesic centroid distance $d_{ij} = \sqrt{\frac{2}{\sqrt{3}} A_{\text{cell},i}}$, normalized areal manufacturing shares $\lambda_j \propto K_j / A_j$, and normalized areal income shares $Y_j \propto (N_j / A_j)(1 + k_{\text{pc},j}/250)$, normalized against the symmetric reference $(G_{\text{sym},i}, w_{\text{sym},i})$.
* **Parameters & Physical Units**:
  * $\sigma = 4.0$, $\mu_M = 0.40$, $\tau_0 = 0.004\,\text{km}^{-1}$, $\gamma_K = 0.12\,\text{yr}^{-1}$; $A_{\text{cell},i}$ [$\text{km}^2$]; $K_i$ [$\text{kg}$].
* **Justification for Deviation**:
  * Replaces the intractable $O(N^2)$ global all-pairs sum with an $O(6) = O(1)$ Ring-1 + Rest-of-World mean-field decomposition on the spherical H3 mesh while preserving Krugman's exact Dixit-Stiglitz / Iceberg functional form and resolution invariance (`Contract 3`).
* **Empirical Validation**:
  * Krugman (1991); Scheidel ORBIS (2014); Maddison Project Database (2020).

---

### Entry 18: Areal Entropic Metal Dissipation & Cellular Pollution Concentration
* **Subsystems**: `EntropicMetalDissipationEngine.java`, `InfrastructureEnergyEngine.java`
* **Canonical Literature**:
  * Bihouix, P. (2014). *L'Âge des low tech: Vers une civilisation techniquement soutenable*. Éditions du Seuil.
* **Canonical Published Formulation**:
  * Global 0D aggregate mass balances where dissipated non-ferrous and structural metals $\Delta M = M_0 (1 - e^{-\lambda t})$ accumulate in an abstract global inventory without spatial concentration dynamics.
* **Calibrated Ether Formulation**:
  * Transformed on variable-resolution H3 discrete meshes into an intensive areal concentration ($\text{units/km}^2$) coupled to natural biogeochemical attenuation:
    $$\Delta P_i = 0.10 \cdot \frac{\Delta M_i}{A_{\text{cell}, i}} \quad [\text{pollution units} / \text{km}^2]$$
* **Parameters & Physical Units**:
  * $\lambda_{\text{diss}} = 0.015\text{ yr}^{-1}$: Structural and industrial metal entropic dissipation rate.
  * $A_{\text{cell}, i}$: Hexagonal cell surface area [$\text{km}^2$] ($A_{\text{cell}} \approx 1\,770.49\text{ km}^2$ at Res 4).
  * $\Delta M_i$: Local unrecovered scrap and dissipated metal mass [$\text{metric tons}$].
* **Justification for Deviation**:
  * Dividing extensive dissipated metal mass by cell surface area $A_{\text{cell}, i}$ guarantees dimensional resolution invariance across all H3 mesh levels (Res 4 to Res 7), allowing local biogeochemical ecosystems to absorb low-density pre-industrial trace wear naturally while capturing high-density industrial contamination hotspots without artificial gating constants.
* **Empirical Validation**:
  * Bihouix (2014); Baccini & Brunner (2012) *Metabolism of the Anthroposphere*; USGS Minerals Information.

---

### Entry 19: Ricardian Marginal Product & Smooth Profit-Squeeze Closure
* **Subsystem**: `RicardoDifferentialLandRentEngine.java`
* **Canonical Literature**:
  * Ricardo, D. (1817). *On the Principles of Political Economy and Taxation*. John Murray.
  * Samuelson, P. A. (1959). *A modern treatment of the Ricardian economy*. Quarterly Journal of Economics, 73(1), 1–35.
* **Canonical Published Formulation**:
  * $MP_L = \beta_A (Y_A / L)$, differential land rent $R_{\text{land}} = (1 - \beta_A) Y_A$, and profit rate $r = r_0 (1 - w_{\text{sub}} / MP_L)$ formulated over monetary corn prices and discrete landlord/capitalist/laborer classes.
* **Calibrated Ether Formulation**:
  * Evaluates Samuelson's physical marginal product $MP_L = \beta_A (B_{\text{agr},i} / N_i)$ ($\beta_A = 0.65$) against subsistence grain norm $w_{\text{sub}} = 0.78\,\text{kg/cap}$, mapping the corn-wage ratio $\theta_w = w_{\text{sub}} / MP_L$ via a $C^\infty$ softplus saturation $s_R = \frac{\operatorname{softplus}(\theta_w - 1, 0, 0.20)}{1 + \operatorname{softplus}(\theta_w - 1, 0, 0.20)}$ onto physical capital drag ($\Delta K = -0.035 s_R K \Delta t$) and Gini expansion ($\Delta G = +0.02 s_R \Delta t$) above landed estate threshold $k_{\text{pc}} \ge 40\,\text{kg/cap}$.
* **Parameters, Material Properties & Physical Units**:
  * $\beta_A = 0.65$, $w_{\text{sub}} = 0.78\,\text{kg/cap}$, $k_{\text{floor}} = 40\,\text{kg/cap}$.
* **Justification for Deviation**:
  * Expresses Ricardian stationary-state profit compression directly over conserved `WorldBuffer` stocks (`biomassAgriculture`, `humanPopulation`, `resourceCapital`) without non-conserved currency prices.
* **Empirical Validation**:
  * Ricardo (1817); Samuelson (1959); Clark (2007) English agricultural land rents.

---

### Entry 20: Lewis & Ranis-Fei Dual-Sector Commercialization Closure
* **Subsystem**: `LewisDualSectorTurningPointEngine.java`
* **Canonical Literature**:
  * Lewis, W. A. (1954). *Economic Development with Unlimited Supplies of Labour*. The Manchester School, 22(2), 139–191.
  * Ranis, G., & Fei, J. C. H. (1961). *A Theory of Economic Development*. American Economic Review, 51(4), 533–565.
* **Canonical Published Formulation**:
  * Piecewise wage switch $w = \max(\bar{w}_S, MP_{L,A})$ between separate rural and urban sector balance sheets, which creates a $C^0$ derivative kink at $MP_{L,A} = \bar{w}_S$.
* **Calibrated Ether Formulation**:
  * Evaluates $MP_{L,A} = \beta_A (B_{\text{agr},i} + 1) / N_i$ ($\beta_A = 0.65$, $\bar{w}_S = 0.65$) with smooth $C^\infty$ logistic transitions across early modern widening ($k_{\text{pc}} \ge 80\,\text{kg/cap}$) and post-turning-point mechanization ($k_{\text{pc}} \ge 250\,\text{kg/cap}$, $\text{Trade} \ge 20$), coupling `dResourceCapital`, `dGiniIndex`, and `dInstitutionalComplexity`.
* **Parameters, Material Properties & Physical Units**:
  * $k_{\text{onset}} = 80\,\text{kg/cap}$, $k_{\text{turn}} = 250\,\text{kg/cap}$, $\beta_A = 0.65$, $\bar{w}_S = 0.65$.
* **Justification for Deviation**:
  * Regularizes the Ranis-Fei commercialization kink into a $C^\infty$ cellular transition over single-cell `WorldBuffer` state arrays.
* **Empirical Validation**:
  * Lewis (1954); Ranis & Fei (1961); Minami (1973) Japanese industrialization.

---

### Entry 21: Kondratiev-Schumpeter Long Wave Cycles & Creative Destruction
* **Subsystem**: `KondratievSchumpeterLongWaveEngine.java`
* **Canonical Literature**:
  * Kondratiev, N. D. (1925). *The Major Economic Cycles*. Voprosy Konyunktury, 1(1), 28–79.
  * Schumpeter, J. A. (1939). *Business Cycles: A Theoretical, Historical, and Statistical Analysis of the Capitalist Process*. McGraw-Hill.
* **Canonical Published Formulation**:
  * 50-to-60-year macroeconomic infrastructure waves driven by the lifespans of large fixed physical capital investments (canals, railways, electrification, highways) followed by creative destruction recessions where obsolete capital is liquidated.
* **Calibrated Ether Formulation**:
  $$\phi(t) = \sin\left(\frac{2\pi \cdot (t - 1780)}{52}\right)$$
  $$\text{CreativeDestruction} = \max\left(0, \; -\phi(t)\right) \cdot \sigma(k_{\text{pc}}, 150, 30)$$
  $$\Delta K_{\text{liquidated}} = -0.015 \cdot \text{CreativeDestruction} \cdot K \cdot \Delta t$$
  $$\Delta M_{\text{recycled}} = +0.30 \cdot |\Delta K_{\text{liquidated}}|$$
* **Parameters, Material Properties & Physical Units**:
  * $T_{\text{wave}} = 52.0\text{ yr}$: Mean historical period of long infrastructure replacement cycles.
  * $t_0 = 1780.0\text{ CE}$: Historical baseline synchronization (First Industrial Revolution).
  * $f_{\text{scrap}} = 0.30$: Fraction of obsolete industrial physical capital recovered as reusable scrap metal.
* **Justification for Deviation**:
  * Couples capital depreciation directly to physical metal recovery, avoiding arbitrary financial credit spikes and anchoring long waves in thermodynamic infrastructure replacement rhythms.
* **Empirical Validation**:
  * Kondratiev (1925); Schumpeter (1939); Freeman & Louçã (2001) *As Time Goes By*.

---

### Entry 22: Pomeranz Great Divergence Coal-Water Proximity & Transatlantic Ghost Acreage Relief
* **Subsystem**: `PomeranzGreatDivergenceEngine.java`
* **Canonical Literature**:
  * Pomeranz, K. (2000). *The Great Divergence: China, Europe, and the Making of the Modern World Economy*. Princeton University Press.
  * Allen, R. C. (2009). *The British Industrial Revolution in Global Perspective*. Cambridge University Press.
  * Wrigley, E. A. (2010). *Energy and the English Industrial Revolution*. Cambridge University Press.
* **Canonical Published Formulation**:
  * Explains why Western Europe (Britain) broke out of the organic agrarian ceiling while the Lower Yangtze (Jiangnan) involuted: Britain had fortuitously located coal deposits adjacent to water transport networks and accessed transoceanic "ghost acreage" (sugar, cotton, timber) relieving domestic ecological bottlenecks.
* **Calibrated Ether Formulation**:
  $$\text{CoalProximity} = \sigma(\text{Coal}, 500, 100) \cdot (1 - \sigma(\mu_{\text{friction}}, 0.70, 0.15))$$
  $$\text{GhostAcreage} = \sigma(\text{Trade}, 25, 5) \cdot \sigma(k_{\text{pc}}, 100, 30)$$
  $$\mathcal{P}_{\text{divergence}} = \text{CoalProximity} \cdot (1.0 + 0.80 \cdot \text{GhostAcreage})$$
  $$\Delta K_{\text{breakout}} = +0.055 \cdot \mathcal{P}_{\text{divergence}} \cdot K \cdot \Delta t$$
* **Parameters, Material Properties & Physical Units**:
  * $\text{Coal}_{\text{thresh}} = 500.0\text{ t}$: Minimum commercial coal deposit required for thermal prime mover takeoff.
  * $\mu_{\text{trans\_max}} = 0.70$: Overland transport friction threshold; water transport ($\mu < 0.50$) allows viable bulk coal movement.
  * $k_{\text{takeoff}} = 0.055\text{ yr}^{-1}$: Industrial takeoff growth rate under joint energy/ghost acreage relief.
* **Justification for Deviation**:
  * Resolves the historiographical Great Divergence debate as a continuous $O(1)$ spatial biophysical equation on H3 cells, testing Acemoglu's institutional primacy hypothesis against Pomeranzian thermodynamic energy breakout.
* **Empirical Validation**:
  * Pomeranz (2000); Allen (2009); Maddison Project 2020; Wrigley (2010).

---

### Entry 23: Solow-Swan Physical Capital Accumulation with Ayres-Garrett Exergy Feedback
* **Subsystem**: `UrbanKernel.java` & `urban.rs`
* **Canonical Literature**:
  * Solow, R. M. (1956). *A contribution to the theory of economic growth*. The Quarterly Journal of Economics, 70(1), 65–94.
  * Swan, T. W. (1956). *Economic growth and capital accumulation*. Economic Record, 32(2), 334–361.
  * Garrett, T. J. (2011). *Are there basic physical constraints on future anthropogenic emissions of carbon dioxide?* Climatic Change, 104(3), 437–455.
  * Ayres, R. U., & Warr, B. (2009). *The Economic Growth Engine: How Energy and Work Drive Material Prosperity*. Edward Elgar Publishing.
  * Krausmann, F., et al. (2017). *Global socioeconomic material stocks rise 23-fold over the 20th century and require half of annual resource use*. Proceedings of the National Academy of Sciences, 114(8), 1880–1885.
* **Canonical Published Formulation**:
  * In canonical neoclassical growth theory (Solow 1956), capital accumulation is governed by an aggregate differential equation driven by an exogenous savings rate $s$ and technological progress $A(t)$:
    $$\dot{K} = s Y - \delta K = s A K^\alpha L^{1-\alpha} - \delta K$$
  * Garrett (2011) proposes that global physical economic capacity is thermodynamically tied to current primary power consumption: $P(t) = \lambda \cdot K(t)$, where $\lambda \approx 9.7 \pm 0.3\text{ mW per 1990 USD}$.
* **Calibrated Ether Formulation**:
  * To eliminate abstract non-physical variables ($A(t)$, abstract currency $Y$), prevent double-counting with pluggable Tier 2 theories, and avoid numerical singularities during demographic collapses ($N \to 0$), Ether establishes `UrbanKernel` / `urban.rs` as the single-responsibility owner of baseline physical capital $k_{\text{pc}} = K / N$ (kg of durable material infrastructure, tools, masonry, and machinery per capita; Krausmann et al. 2017) coupled to useful somatic + exosomatic power per capita $P_{\text{watts}}$ (including domesticated draft animal traction) and Tainter institutional maintenance drag $\delta_{\text{eff}}(C)$:
    $$\dot{k}_{\text{pc}} = \delta_0 \cdot k^*(P_{\text{watts}}, B_{\text{agr}}, N) - \delta_{\text{eff}}(C) \cdot k_{\text{pc}}$$
    $$\delta_{\text{eff}}(C) = \operatorname{softmin}\!\left(0.075,\; \delta_0 + 0.0008 \cdot \max(0, \operatorname{softmin}(5.0, C, 4.0))^{1.5},\; 40.0\right)$$
  * The thermodynamic and sedentary agrarian steady-state target capital $k^*$ scales smoothly above somatic/hearth subsistence ($P_0 \approx 348\text{ W}$):
    $$k^*_{\text{pow}} = 5.0 + 580.0 \tanh\left(\frac{\text{softplus}(P_{\text{watts}} - 348.0, 0.2)}{38.0}\right) + 62.0 \cdot \left(\frac{\text{softplus}(P_{\text{watts}} - 400.0, 0.1)}{100.0}\right)^{1.95}$$
    $$k^*_{\text{agr}} = 145.0 \tanh\!\left(\frac{\max(0, B_{\text{agr}})}{180 N + 1}\right) \cdot \sigma(N, 50, 15)$$
    $$k^* = 5.0 + \left[(k^*_{\text{pow}} - 5.0)\,\sigma(N, 50, 15) + k^*_{\text{agr}}\right] \cdot \text{sedentaryCapability}$$
    with Lavoisier raw-material debit ($5\%$ metal, $7\%$ timber, $10\%$ clay) applied on net positive capital additions $\Delta K^+ > 0$ in both Java and Rust.
* **Parameters, Material Properties & Physical Units**:
  * $k_{\text{pc}}$: Physical capital stock per capita [$\text{kg/person}$] ($5\text{ kg}$ Paleolithic tools, $150\text{--}450\text{ kg}$ sedentary agrarian masonry/granaries/plows, $4\,500\text{--}16\,500\text{ kg}$ industrial built environment; Krausmann et al. 2017).
  * $P_{\text{watts}}$: Continuous useful power dissipation per capita [$\text{W/person}$] (somatic basal + fire + draft animal traction + hydraulic/wind + fossil/electric grid).
  * $\delta_0 = 0.04\text{ yr}^{-1}$: Baseline physical infrastructure entropic turnover rate ($4\%/\text{yr}$), rising to $\delta_{\text{eff}}(C) \le 0.075\text{ yr}^{-1}$ under high Tainter complexity overhead.
  * $\text{sedentaryCapability} = \max(\sigma(B_{\text{agr}}, 150, 25), \sigma(P_{\text{watts}}, 520, 25))$: Biophysical mobility constraint preventing mobile hunter-gatherer bands from accumulating stationary capital stock.
* **Academic Justification & Root Cause of Divergence**:
  1. *Replacement of Abstract Technology with Conserved Exergy*: Canonical Solow-Swan treats technology $A$ as an exogenous residual (Solow residual) without energy conservation constraints. In Ether, capital accumulation is grounded in physical thermodynamics: structural machinery cannot be built or maintained without continuous useful power dissipation.
  2. *Singularity Prevention during Population Shocks*: In standard neoclassical models, if population drops precipitously ($N \to 0$), capital per capita diverges ($K/N \to \infty$). Coupling steady-state capital $k^*$ and gross investment directly to active physical power ensures that abandoned infrastructure depreciates entropicly ($\delta$) toward zero without numerical divergence.
* **Empirical Validation**:
  * Krausmann et al. (2017), PNAS material stock database 1900–2010; Maddison Project Database (2020); Ayres & Warr (2009); Garrett (2011).

---

### Entry 24: Two-Phase Radioecological Contamination Decay & Exclusion Zone Habitation Closure
* **Subsystem**: `EcologicalDegradationEngine.java` & `ThermodynamicMigrationEngine.java`
* **Canonical Literature**:
  * UNSCEAR (2000). *Sources and Effects of Ionizing Radiation*. United Nations Scientific Committee on the Effects of Atomic Radiation, New York.
  * Steinhauser, G., Brandl, A., & Johnson, T. E. (2014). *Comparison of the Chernobyl and Fukushima nuclear accidents: a review of the environmental impacts*. Science of the Total Environment, 470, 800–817.
  * Smith, J. T., & Beresford, N. A. (2005). *Chernobyl—Catastrophe and Consequences*. Springer-Praxis.
* **Canonical Published Formulation**:
  * Nuclear radiotoxicity decay is physically governed by a multi-isotope exponential decay series:
    $$A(t) = \sum_{k} A_k(0) \cdot e^{-\lambda_k t}$$
    where short-lived isotopes ($^{131}\text{I}$, $T_{1/2} = 8.02\text{ days}$) vanish within months, while dominant long-lived bioaccumulative fission products ($^{137}\text{Cs}$, $T_{1/2} = 30.17\text{ years}$; $^{90}\text{Sr}$, $T_{1/2} = 28.79\text{ years}$) govern multi-decadal environmental contamination.
* **Calibrated Ether Formulation**:
  * Because simulation cells track a single continuous conserved intensive variable `pollutionLevel` ($\text{arbitrary ecotoxicity/radiotoxicity index}$, where $P \ge 1\,500$ represents a lethal exclusion zone and nuclear meltdowns inject $P \approx 10\,000\text{--}12\,000$), applying a uniform monthly chemical degradation rate ($0.92^{\Delta t}$) would erroneously clear nuclear disaster zones within $3\text{ to }4\text{ years}$.
  * Ether parameterizes an exact two-phase decay closure:
    $$\Delta P_{\text{chem}} = \max\left(0.0, \; \min(P, 1000.0) \cdot 0.92^{\Delta t_{\text{months}}} - 1.2 \cdot \Delta t_{\text{months}}\right)$$
    $$P_{\text{nuclear}}(t + \Delta t) = \max(0.0, P(t) - 1000.0) \cdot \exp\left(-\frac{\ln 2}{30.0} \cdot \Delta t_{\text{years}}\right)$$
    $$P(t + \Delta t) = \Delta P_{\text{chem}} + P_{\text{nuclear}}(t + \Delta t)$$
  * Spatial migration closure (`ThermodynamicMigrationEngine.java`): If $P_i > 1500.0$, destination acceptance $p_{\text{accept}} = 0.0$ and departure chemical potential $\mu_i$ drives total evacuation.
* **Parameters, Material Properties & Physical Units**:
  * $P$: Intensive ecotoxicity/radiotoxicity concentration index [$0\text{--}25\,000$].
  * $\lambda_{\text{nuclear}} = 0.023105\text{ yr}^{-1}$: Effective radioactive decay constant matching $^{137}\text{Cs}$ and $^{90}\text{Sr}$ ($T_{1/2} = 30.0\text{ years}$).
  * $P_{\text{exclusion}} = 1500.0$: Habitation threshold corresponding to $> 1.48\text{ MBq/m}^2$ ($40\text{ Ci/km}^2$) $^{137}\text{Cs}$ soil contamination.
* **Academic Justification & Root Cause of Divergence**:
  * *Single State Variable Memory Closure*: Resolving individual isotopic decay chains ($^{131}\text{I}$, $^{134}\text{Cs}$, $^{137}\text{Cs}$, $^{90}\text{Sr}$, $^{239}\text{Pu}$) across 288,122 H3 cells would add massive memory overhead ($O(K \cdot N)$ memory tensors). The two-phase piecewise closure preserves exact physical behavior: short-term industrial/organic effluent decays rapidly, while severe nuclear disasters (Chernobyl, Fukushima, nuclear strikes) remain uninhabitable exclusion zones for $> 80\text{ years}$, exactly as observed empirically.
* **Empirical Validation**:
  * Chernobyl Nuclear Exclusion Zone (1986–present, 30 km restricted zone remains uninhabitable after 38 years; Steinhauser et al. 2014); Fukushima Daiichi evacuation zones (2011–present).

---

### Entry 25: Converter Capital-Area Parameterization for Prime Movers & Renewables
* **Subsystems**: `ThermodynamicPrimeMoversEngine.java`, `RenewableEnergyPhysicsEngine.java`, `KummelAyresExergyEngine.java`
* **Canonical Literature**:
  * Carnot, S. (1824); Betz, A. (1926); Shockley, W., & Queisser, H. J. (1961); Ayres, R. U., & Warr, B. (2009); Smil, V. (2017).
* **Canonical Published Formulation**:
  * Published physical laws give conversion efficiency and power as functions of explicit machine geometry and reservoir temperatures ($P_{\text{Betz}} = \frac{8}{27}\rho A_{\text{rotor}} v^3$, $P_{\text{PV}} = \eta_{\text{SQ}} I_{\text{surf}} A_{\text{PV}}$, $\eta_{\text{th}} = \varepsilon_{\text{II}}(1 - T_C/T_H)$), leaving installed rotor/panel swept area and boiler pressure-vessel temperature $T_H$ unspecified in a macroscopic historical model without asset-level factory inventories.
* **Calibrated Ether Formulation**:
  * Maps aggregate per-capita physical capital $k_{\text{pc}} = K_i / N_i$ [$\text{kg/cap}$] smoothly onto installed converter capacity and working-fluid temperature ($k_{\text{pc}} \gtrsim 120\,\text{kg/cap}$ hydraulic mills, $320\,\text{kg/cap}$ wind sails, $950\,\text{kg/cap}$ coal steam boilers, $2\,800\,\text{kg/cap}$ internal combustion, $4\,500\text{–}5\,000\,\text{kg/cap}$ utility wind turbines & photovoltaics), with exact zero anthropogenic capture ($E_{\text{wind}} = E_{\text{solar}} = 0.0\,\text{GJ/yr}$) when $N_i \le 0$ or $k_{\text{pc}} \le 0$, and converts Watts to `WorldBuffer` annual fluxes via `PhysicalUnitsHelper.wattsToGjPerYear`.
* **Parameters & Physical Units**:
  * $T_C \in [220, 330]\,\text{K}$, $T_H \in [390, 1\,840]\,\text{K}$, $\text{LHV}_{\text{coal}} = 24\,\text{GJ/t}$, $\text{LHV}_{\text{oil}} = 42\,\text{GJ/t}$, $G_{\text{ke,max}} = 1.25\,\text{W/m}^2$.
* **Academic Justification & Root Cause of Divergence**:
  * Enforces `AGENTS.md` §5.1 (zero `technologyLevel` state array) and `Contract 2` (decoupling meteorological wind/insolation from anthropogenic harnessed energy) while obeying Carnot, Betz, Kleidon, and First-Law fuel enthalpy bounds.
* **Empirical Validation**:
  * Smil (2017) historical prime-mover chronology; Ayres & Warr (2009) 1900–2005 Second-Law exergy series.

---

### Entry 26: Von Thünen Sub-Grid Supply Ring & Rolling-Stock Friction Attenuation
* **Subsystem**: `VonThunenAgriculturalRingsEngine.java`
* **Canonical Literature**:
  * von Thünen, J. H. (1826). *Der isolierte Staat*; Chisholm, M. (1962); Bairoch, P. (1988).
* **Canonical Published Formulation**:
  * Continuous radial bid-rent $R(r) = Y(p - c - f \cdot r)$ around a point market surrounded by concentric rings of explicit farm plots.
* **Calibrated Ether Formulation**:
  * Resolves the sub-cell supply hinterland radius $R_{\text{ring}} = \sqrt{N_i e_{\text{meta}} / (\pi y_{\text{agr},i})}$ from cellular edible caloric density $y_{\text{agr},i} = E_{\text{cal},i} / A_{\text{cell},i}$, deducts the $r_0 = 5\,\text{km}$ local village field-walk radius via `softplus`, attenuates modal transport energy share $c_{\text{eff}} = \frac{c_{\text{mode}} \mu_i}{1 + k_{\text{pc},i}/250}$ ($c_{\text{land}} = 0.008\,\text{km}^{-1}$, $c_{\text{water}} = 0.0008\,\text{km}^{-1}$), and integrates $1 - e^{-c_{\text{eff}} r}$ analytically over the circular disk.
* **Parameters & Physical Units**:
  * $r_0 = 5.0\,\text{km}$, $c_{\text{land}} = 0.008\,\text{km}^{-1}$, $c_{\text{water}} = 0.0008\,\text{km}^{-1}$, $e_{\text{meta}} = 3.65\,\text{GJ/(person}\cdot\text{yr)}$.
* **Academic Justification & Root Cause of Divergence**:
  * At H3 Res 4–6 ($36\text{–}1\,770\,\text{km}^2$), urban food-shed rings sit partially sub-grid; analytical disk integration reproduces Bairoch's (1988) inland vs. coastal urban ceiling without $O(N^2)$ multi-ring pathfinding or arbitrary population step caps.
* **Empirical Validation**:
  * von Thünen (1826); Clark & Haswell (1967) *Economics of Subsistence Agriculture*; Bairoch (1988).

---

### Entry 27: Multi-Theory Capital Envelope & Cellular Institutional Closures
* **Subsystems**: `TypeBEnginePassRunner.java`, `EnvironmentalKernel.java` / `environmental.rs`, `AxialAgeMoralizingGodsEngine.java`, `PolanyiFictitiousCommoditiesEngine.java`, `BagehotFractionalReserveEngine.java`, `CordenNearyDutchDiseaseEngine.java`, `InnisStaplesThesisEngine.java`
* **Canonical Literature**:
  * Krausmann et al. (2017); Lieth (1975); Whitehouse et al. (2019); Polanyi (1944); Bagehot (1873); Corden & Neary (1982); Innis (1930).
* **Canonical Published Formulation**:
  * Institutional macro-theories are published either as qualitative comparative-historical mechanisms (Polanyi, Bagehot, Innis) or standalone single-mechanism models that do not specify how multiple active theories share a conserved cellular energy/material budget.
* **Calibrated Ether Formulation**:
  * `TypeBEnginePassRunner` combines overlapping Tier 2 capital derivatives via non-diluting upper/lower envelopes ($\Delta K_{\text{env}}^+ = \max_e \Delta K_e^+$, $\Delta K_{\text{env}}^- = \min_e \Delta K_e^-$), caps net positive Tier 2 capital deepening by remaining Carnot exergy surplus ($s_{\text{marg}} = 0.12$, $e_{\text{embodied}} = 15\,\text{MJ/kg}$) after Phase 1 Solow-Swan replacement, and debits Krausmann stoichiometry ($5\%$ metal, $7\%$ timber, $10\%$ clay). Institutional Type B engines map qualitative thresholds onto smooth $C^1$ logistic functions of $(N_i, k_{\text{pc},i}, \text{Trade}_i)$ while strictly debiting upstream physical stocks (`biomassNatural`, `metalResource`). In `EnvironmentalKernel` / `environmental.rs`, Res-4 reference crop potential scales Miami NPP across $1\,770.49\,\text{km}^2 = 177\,049\,\text{ha}$ ($B_{\text{ag,base}} = \operatorname{clamp}(1\,800\,\text{NPP}\cdot f_{\text{slope}}\cdot f_{\text{frost}}, 500, 3.6\times 10^6)\,\text{GJ}$).
* **Academic Justification & Root Cause of Divergence**:
  * Prevents $M$-fold capital double-counting and *ex nihilo* mass creation when multiple Tier 2 theories are co-activated (`AGENTS.md` §4.2).
* **Empirical Validation**:
  * Krausmann et al. (2017); HYDE 3.4 & Maddison (2020) regional/global runs.

---

### Entry 28: Post-Hoc $\Phi_{\text{exergy}}$ Macro-Telemetry Diagnostic Closures
* **Subsystems**: `CliodynamicFormulasRegistry.java`, `H3CellPhysicsAndStateHelper.java`, `DemographicMetricsHelper.java`, `CompiledTelemetryPlan.java`
* **Canonical Literature**:
  * Maddison (2020); Smil (2017); Buringh & van Zanden (2009); Reinhart & Rogoff (2009); Harley (1988); Federico & Tena-Junguito (2017); Scheidel (2014); Standage (1998); Crosby (1972).
* **Canonical Published Formulation**:
  * Empirical historical series report macro-indicators (GWP in 1990 Int\$, adult literacy %, sovereign debt/GDP %, shipping freight index, relay/telegraph dispatch speed, biotic isolation) across calendar time, whereas simulation state arrays store only conserved physical masses, populations, and energy fluxes.
* **Calibrated Ether Formulation**:
  * Evaluates post-hoc read-only telemetry diagnostics as smooth $C^1$ functions of conserved cellular observables and the Garrett-Lotka index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (`CliodynamicFormulasRegistry`), enforcing exact zero Paleolithic baselines (`AGENTS.md` §5.3) and zero feedback into runtime physics kernels.
* **Academic Justification & Root Cause of Divergence**:
  * Bridges conserved SI physical state variables (`kg`, `t`, `GJ`, `persons`) to historical benchmark series in `HistoricalValidationKernel` without storing non-physical state variables or calendar-year lookup tables.
* **Empirical Validation**:
  * Audited against 27 1D historical variables (`HYDE 3.4`, `Maddison 2020`, `Seshat`) across Paleolithic, Classical, Medieval, Early Modern, and Industrial benchmarks.

---

### Entry 29: Asymmetric Directed Cultural Affinity & Defensive Grievance Closure
* **Subsystems**: `CulturalAffinityMatrixInitializer`, `WarDiplomacyEngine.java`, `H3BufferAndEventCoordinator.java`
* **Canonical Literature**:
  * Shepard, R. N. (1987); Axelrod, R. (1997); Allport, G. W. (1954); Bueno de Mesquita, B. (1981).
* **Canonical Published Formulation**:
  * Symmetric Euclidean trait distance $A_0(i, j) = \exp(-\lambda \|\mathbf{T}_i - \mathbf{T}_j\|_2)$ ($A_{ij} = A_{ji}$).
* **Calibrated Ether Formulation**:
  * Augments $A_0(i, j)$ ($\lambda = 3.5$) with directional status prestige $+0.12 \tanh(2.5(T_{j,1} - T_{i,1}))$ and dogmatic exclusivism $-0.15 T_{i,3}|T_{i,3} - T_{j,3}|$, restricts polity instantiation to active mesh bounds (`minDistanceSq <= maxRadiusSq`), and scales wartime bilateral affinity erosion continuously by $\Delta t_{\text{yr}}$ with a $1.6\times$ defensive grievance multiplier.
* **Academic Justification & Root Cause of Divergence**:
  * Captures asymmetric tributary assimilation vs. defensive irredentism in directed $N \times N$ polity graphs while guaranteeing $\Delta t$-invariance and regional mesh isolation.
* **Empirical Validation**:
  * Seshat Global History Databank polity interaction histories; regional high-resolution runs (`tokugawa_japan`, `mali_empire`, `industrial_1800`).

---

## 📊 4. Central Summary Matrix of All Calibrated Subsystems (Entries 1 to 29)

| # | Category | Subsystem | Canonical Literature | Calibrated Ether Formulation | Specific Adaptation / Material Calibration |
|---|---|---|---|---|---|
| **1** | Tier 1 | DEB Cohort Aggregation | Kooijman (2010), Cahill (1970) | $\kappa$-rule, somatic $V$ vs reserve $E$, quadratic $\mu_{\text{starv}}$ | Microscopic individual ODE aggregated to cell cohorts; $w_{\text{crit}} = 0.40$, $[\dot{p}_M] = 2.25\text{ GJ/yr}$ |
| **2** | Tier 1 | MTE Cross-Taxa Kinetics | Brown et al. (2004), Gillooly (2001) | $B_0 M^{3/4} \exp(-E_a / k_B T_{\text{op}})$ | Operative thermal boundary: 310.15 K homeothermy vs ambient ectotherm; $E_a = 0.65\text{ eV}$ |
| **3** | Tier 1 | Ecological Stoichiometry | Sterner & Elser (2002), Loladze (2014) | Continuous softmin Liebig N-P-K & $\text{CO}_2$ protein dilution | Differentiable $C^\infty$ softmin replaces $\min()$; Redfield mass $41.1 : 7.23 : 1.0$, $\delta_{\max} = 12\%$ |
| **4** | Tier 1 | Constructal Flow Law | Bejan (1996, 2016), Hack (1957) | $V \sim M^{1/6}$ animal speed & Hack $L = 1.4 A^{0.57}$ | Locomotion terrain friction coupling & $O(1)$ stream power catchment closure |
| **5** | Tier 1 | Neutral Biodiversity UNTB | Hubbell (2001), Volkov (2003) | $\theta = 2 J_M \nu$, species retention $\mathcal{B} = (B/B_0)^z$ | Replaces scalar biodiversity with continuous power-law extinction debt ($z = 0.25$) |
| **6** | Tier 1 | MEPP Climate Carnot | Kleidon (2010, 2016), Priestley (1972) | $\eta_{\text{carnot}} = (T_s - T_e)/T_s$, Priestley-Taylor MEP | Analytical thermodynamic wind work ($G_{\text{ke}}$) and precipitation without 3D Navier-Stokes |
| **7** | Tier 1 | Dynamic AMOC 2-Box | Stommel (1961), Broecker (1998) | Sub-stepped $(T_p, S_p)$ dynamics with bipolar seesaw | Dimensional Sverdrups, Clausius-Clapeyron freshwater coupling, and bipolar seesaw ratio $-0.22$ |
| **8** | Tier 1 | Charney Sahara Bistability | Charney (1975), Claussen (1999) | Precession-driven vegetation-albedo fold bifurcation | $O(1)$ analytical fold catastrophe replacing 3D AGCMs; $P_{50} = 200\text{ mm/yr}, \tau_V = 10\text{ yr}$ |
| **9** | Tier 1 | Stream Power Erosion | Howard & Kerby (1983), Bagnold (1966) | $\dot{E} = K A^m S^n$ via Hack closure & alluvial siltation | $O(1)$ drainage area estimation replaces recursive DEM flow routing; $K_{\text{erod}} = 1.2 \times 10^{-4}$ |
| **10** | Tier 2 | $\Phi_{\text{exergy}}$ Index | Garrett (2011), Lotka (1922) | Logarithmic scaling of $(k_{\text{active}}, p_{\text{exo}})$ | Replaces abstract technology with continuous physical observables $(p_{\text{somatic}} = 348\text{ W})$ |
| **11** | Tier 2 | Aquifer Darcy-Boussinesq | Wada et al. (2010), Margat & van der Gun (2013) | $W_i = (N_i \cdot w_{\text{crop}}) / A_{\text{cell}}$, discrete $\sum T_{ij} \Delta h_{ij} / d_{ij}$ | Discretization of 2D Boussinesq flow on H3 mesh with distance-weighted transmissivity |
| **12** | Tier 2 | Combinatorial Capital | Arthur (2009), Kremer (1993) | $dK/dt \propto N (1 + k/k_0)^\alpha$ | Embodies abstract ideas into conserved physical capital mass ($k_0 = 120\text{ kg/cap}, \alpha = 1.35$) |
| **13** | Tier 2 | Chalcolithic Gate | Hotelling (1931), Radivojević (2010) | Third-order Hill furnace gate $k^3/(k^3+65^3)$ | Enforces Pleistocene zero-smelting without calendar hacks ($k_{\text{chalco}} = 65\text{ kg/cap}$) |
| **14** | Tier 2 | Carrier Bandwidth | Shannon (1948), Brillouin (1956) | $B = B_0 + \alpha_k k_{\text{pc}} + \alpha_p p_{\text{exo}}^2$ | Grounds communication bandwidth in physical roads and power ($B_0 = 1\text{ Hz}$) |
| **15** | Tier 2 | SDT Stress Index | Turchin (2016), Goldstone (1991) | Normalized spatial $\Psi \in [0, 100]$ over $E_{\text{cal},i}$ | Spatial normalization of local demographic/caloric/Gini stress to Seshat scale |
| **16** | Tier 2 | Soil Erosion Gate | Montgomery (2007), USDA | Two-regime ard + mechanized tillage $E_{\text{soil}}(\Phi)$ | Preserves $0\%$ Paleolithic baseline; separates ard plow from deep mechanized tillage |
| **17** | Tier 2 | Krugman Agglomeration | Krugman (1991), Fujita et al. (1999) | Local Ring-1 + RoW Dixit-Stiglitz $(G_i, w_i, \omega_i)$ | $O(6)$ geodesic hexagonal evaluation replacing $O(N^2)$ global all-pairs sum |
| **18** | Tier 2 | Areal Entropic Dissipation | Bihouix (2014) | $\Delta P_i \propto \Delta M_i / A_{\text{cell}, i}$ | Resolution-invariant intensive areal concentration on H3 mesh ($\lambda_{\text{diss}} = 0.015\text{ yr}^{-1}$) |
| **19** | Tier 2 | Ricardo Differential Rent | Ricardo (1817), Samuelson (1959) | $MP_L = \beta_A (B_{\text{agr}}/N)$, smooth corn-wage squeeze | Expresses Ricardian profit squeeze over physical grain/population stocks without currency prices |
| **20** | Tier 2 | Lewis Dual-Sector Model | Lewis (1954), Ranis & Fei (1961) | Smooth commercialization transition across $MP_{L,A} \lessgtr \bar{w}_S$ | Regularizes Ranis-Fei piecewise wage kink on single-cell `WorldBuffer` arrays |
| **21** | Tier 2 | Kondratiev Long Wave | Kondratiev (1925), Schumpeter (1939) | $\phi(t) = \sin(2\pi(t-1780)/52)$, creative destruction trough | Infrastructure cycle coupled to physical capital scrapping and scrap metal recovery |
| **22** | Tier 2 | Pomeranz Great Divergence | Pomeranz (2000), Allen (2009), Wrigley | $\mathcal{P}_{\text{div}} = \text{CoalProx} \cdot (1 + 0.8 \cdot \text{GhostAcreage})$ | Energy transport proximity & transoceanic land relief driving takeoff vs organic involution |
| **23** | Tier 1 | Solow-Swan Capital Accumulation | Solow (1956), Garrett (2011), Krausmann (2017) | $\dot{k}_{\text{pc}} = \delta_0 k^*(P_{\text{watts}}, B_{\text{agr}}, N) - \delta_{\text{eff}}(C) k_{\text{pc}}$ | Single-responsibility base capital ODE with sedentary agrarian masonry stock & Lavoisier debit |
| **24** | Tier 1 | Two-Phase Radioecological Decay | UNSCEAR (2000), Steinhauser et al. (2014) | Piecewise chemical ($0.92^{\Delta t}$) vs $^{137}\text{Cs}$ half-life ($T_{1/2}=30\text{ yr}$) | Single-variable memory closure ensuring nuclear exclusion zones ($P>1500$) remain uninhabitable for $>80\text{ yr}$ |
| **25** | Tier 1 | Prime-Mover & Renewable Capacity | Carnot (1824), Betz (1926), Shockley (1961) | $k_{\text{pc}} \mapsto (A_{\text{rotor}}, A_{\text{PV}}, T_H)$ with $0\,\text{GJ/yr}$ uninhabited | Parameterizes converter area and boiler $T_H$ from conserved $K/N$ without `technologyLevel` state |
| **26** | Tier 2 | Von Thünen Disk Closure | von Thünen (1826), Bairoch (1988) | $L(u) = 1 - \frac{2}{u^2}[1 - (1+u)e^{-u}]$ over $R_{\text{ring}}$ | Sub-grid circular hinterland analytical integration on discrete H3 cells |
| **27** | Tier 2 | Multi-Theory Capital Envelope | Krausmann (2017), Polanyi, Bagehot, Corden | $\Delta K^+_{\text{phys}} = \min(\max_e \Delta K^+_e, \Delta K_{\text{Carnot}})$ | Eliminates $M$-fold capital double-counting and enforces Lavoisier/Carnot bounds |
| **28** | Telemetry | Post-Hoc $\Phi_{\text{exergy}}$ Diagnostics | Maddison (2020), Smil (2017), Reinhart (2009) | Read-only $C^1$ response functions over $(p, k_{\text{pc}})$ | Maps conserved SI states to 27 historical validation series with zero kernel feedback |
| **29** | Tier 2 | Asymmetric Cultural Affinity | Shepard (1987), Axelrod (1997), Allport (1954) | Directed $A(i \to j)$ prestige/dogma prior & $1.6\times$ war grievance | Captures asymmetric core-periphery assimilation and $\Delta t_{\text{yr}}$-scaled frontline conflict |
