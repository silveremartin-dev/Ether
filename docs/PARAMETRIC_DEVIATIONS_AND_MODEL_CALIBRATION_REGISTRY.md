# ⚖️ Central Registry of Model Calibrations & Literature Parametric Deviations

> **Ether Cliodynamic & Planetary Simulation Laboratory**  
> *Standard: AGENTS.md §8 — Epistemic Integrity, Observable State Invariants & Parsimonious Deviation Discipline*  
> *Scope: Strict and transparent documentation of only those mathematical adaptations, discrete-to-mesh closures, continuous $C^1$ relaxations, and physical state parameterizations where a published formulation was under-specified for macroscopic H3 execution or required an unavoidable $O(1)$ / conserved-stock reformulation.*

---

## 🎯 1. Purpose & Strict Registry Inclusion Criteria (Exceptions are NOT the Rule)

The foundational purpose of this registry is **epistemic transparency, parsimony, and algorithmic reproducibility**:
* **The Norm (`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`)**: Whenever an equation is well-specified in academic literature and implemented directly from published laws (e.g., Stefan-Boltzmann radiation, Clausius-Clapeyron moisture, Brown-Gillooly MTE $B_0 M^{3/4} e^{-E_a / k_B T}$, Bejan-Marden Constructal $M^{1/6}$ locomotion, Preston-Hubbell neutral biodiversity $S \propto A^{0.25}$, Kleidon Carnot atmospheric work, USLE soil loss, Domar-Bohn debt dynamics), it belongs **exclusively in the Canonical Equations Encyclopedia**. Direct scientific laws MUST NOT be logged as deviations.
* **The Exception Log (This Registry)**: This document is strictly reserved for formulations where Ether **had to complete or adapt** the published expression to run deterministically on a conserved H3 hexagonal tensor mesh:
  1. **Under-Specified Macroscopic Closures**: The published paper operated over microscopic individuals, monetary prices, or asset-level machine geometries not stored in conserved `WorldBuffer` SI fields (`kg`, `t`, `GJ`, `persons`).
  2. **Unavoidable $O(1)$ / $C^1$ Numerical Reformulations**: Replacing computationally intractable $O(N^2)$ global matrices, recursive DEM trees, or 3D AGCMs with $O(1)$ analytical closures, or regularizing $C^0$ kinks ($\min$, piecewise switches) into $C^1$ smooth operators.
* **Parsimony of Exceptions**: Entries in this registry are kept to the **strict minimum necessary** for physical closure and real-time mesh execution.

---

## 🔬 2. Category A: Tier 1 Biophysical, Thermodynamic & Geophysical Mesh Closures

### Entry 1: Standard Dynamic Energy Budget (DEB) Cohort Aggregation & Starvation Hazard
* **Subsystem**: `DynamicEnergyBudgetEngine.java`
* **Canonical Literature**: Kooijman, S. A. L. M. (2010). *Dynamic Energy Budget theory for metabolic organisation*. Cambridge University Press (DOI: `10.1017/CBO9780511805400`); Hatton, I. A., et al. (2015). *Science*, 349(6252), aac6284; Cahill, G. F. (1970). *NEJM*, 282(12), 668–675.
* **Canonical Published Formulation**: Microscopic individual ODE system tracking individual structural volume $V$, reserve density $e$, and maturity thresholds $E_H$ per organism.
* **Calibrated Simulation Formulation**:
  $$\frac{dV}{dt} = \frac{\kappa \dot{p}_C - [\dot{p}_M] V}{[E_G]}, \qquad \frac{dE}{dt} = \dot{p}_A - \dot{p}_C, \qquad \mu_{\text{starv}} = \mu_{\max} \left(\frac{\max(0, \Delta V / V_{\text{healthy}})}{w_{\text{crit}}}\right)^2$$
* **Physical Units**: $V$ [$\text{t/cell}$ or $\text{kg/cap}$], $E$ [$\text{GJ}$], $[\dot{p}_M] = 2.25\,\text{GJ/(person}\cdot\text{yr)}$, $[E_G] = 28.0\,\text{GJ/t}$, $\kappa = 0.80$, $w_{\text{crit}} = 0.40$, $T_A = 8\,000\,\text{K}$.
* **Root Cause of Divergence**: Aggregating Kooijman's individual-level state onto cellular demographic cohorts preserves mass-energy conservation at $O(1)$ per cell while replacing arbitrary Verhulst $K$ caps with $C^1$ structural catabolism.
* **Empirical Validation Grounding**: `Add-my-Pet` cross-taxa database ($>1\,000$ species); Keys et al. (1950) Minnesota Starvation Experiment; Cahill (1970).

---

### Entry 2: Ecological Stoichiometry Liebig Softmin & Lavoisier Caloric Harvest Closure
* **Subsystem**: `EcologicalStoichiometryEngine.java`
* **Canonical Literature**: Sterner, R. W., & Elser, J. J. (2002). *Ecological Stoichiometry*. Princeton University Press; Loladze, I. (2014). *eLife*, 3, e02245 (DOI: `10.7554/eLife.02245`).
* **Canonical Published Formulation**: Non-differentiable Liebig minimum $Y = \min_k(R_k / R_{k,0})$ without an explicit cellular standing-crop mass debit.
* **Calibrated Simulation Formulation**:
  $$L(\text{N}, \text{P}, \text{K}) = \operatorname{softmin}\!\left(\frac{\text{N}}{\text{N}_0}, \; \operatorname{softmin}\!\left(\frac{\text{P}}{\text{P}_0}, \frac{\text{K}}{\text{K}_0}, 8.0\right), \; 8.0\right), \qquad D_{\text{prot}}(\text{CO}_2) = 1 - 0.12 \tanh\!\left(\frac{\max(0, \text{CO}_2 - 280)}{350}\right)$$
  $$\Delta M_{\text{harv}} = \min(0.50\,B_{\text{agr}},\; 0.12\,B_{\text{agr}}\,L\,D_{\text{prot}}\,\Delta t), \qquad \Delta F = 0.015\,\Delta M_{\text{harv}}, \qquad \Delta B_{\text{agr}} = 0.20\,B_{\text{agr}}(L D_{\text{prot}} - 1)\Delta t - \Delta M_{\text{harv}}$$
* **Physical Units**: $B_{\text{agr}}, \Delta M_{\text{harv}}$ [$\text{kg}$], $\Delta F$ [$\text{GJ}$], $e_{\text{grain}} = 15.0\,\text{MJ/kg} = 0.015\,\text{GJ/kg}$, $\text{CO}_2$ [$\text{ppm}$].
* **Root Cause of Divergence**: Replaces the $C^0$ Liebig kink with a $C^\infty$ `softmin` and enforces Lavoisier mass conservation by debiting harvested calories directly from `biomassAgriculture`.
* **Empirical Validation Grounding**: FACE $\text{CO}_2$ enrichment trials; Loladze (2014); Myers et al. (2014).

---

### Entry 3: Dimensional Stommel AMOC 2-Box Dynamics & Bipolar Seesaw Closure
* **Subsystem**: `ThermohalineStommelAMOCEngine.java`
* **Canonical Literature**: Stommel, H. (1961). *Tellus*, 13(2), 224–230 (DOI: `10.3402/tellusa.v13i2.9491`); Broecker, W. S. (1998). *Paleoceanography*, 13(2), 119–121; EPICA Community Members (2006). *Nature*, 444, 195–198.
* **Canonical Published Formulation**: Dimensionless 2-box ODE $\dot{T} = -\tau_T^{-1}(T - T_e) - |q|T$, $\dot{S} = F - \tau_S^{-1}(S - S_e) - |q|S$ with prescribed constant freshwater forcing $F$.
* **Calibrated Simulation Formulation**:
  $$q = C \left[ \alpha_T (T_{\text{eq}} - T_{\text{sub}}) - \beta_S (S_{\text{eq}} - S_{\text{sub}}) \right], \qquad \Delta T_{\text{North}} = \frac{-8.0}{1 + \exp\!\left(\frac{q - 8.0}{1.5}\right)}, \qquad \Delta T_{\text{South}} = -0.22 \, \Delta T_{\text{North}}$$
* **Physical Units**: $q$ [$\text{Sv} = 10^6\,\text{m}^3/\text{s}$] ($q_0 = 18.2\,\text{Sv}$), $V_p = 250\,\text{Sv}\cdot\text{yr}$, $\tau_T = 20\,\text{yr}$, $\tau_{\text{diff}} = 500\,\text{yr}$, $S$ [$\text{PSU}$], $\Delta T$ [$\text{K}$].
* **Root Cause of Divergence**: Converts Stommel's dimensionless box system to physical Sverdrups coupled to orbital meltwater discharge and the EPICA $-0.22$ Antarctic seesaw without a 3D OGCM.
* **Empirical Validation Grounding**: NGRIP/GISP2 Greenland & EPICA Dome C ice-core records; Rahmstorf (1995).

---

### Entry 4: Analytical Charney Vegetation-Albedo Fold Bifurcation
* **Subsystem**: `CharneyVegetationAlbedoEngine.java`
* **Canonical Literature**: Charney, J. G. (1975). *Q. J. R. Meteorol. Soc.*, 101, 193–202 (DOI: `10.1002/qj.49710142802`); Claussen, M., et al. (1999). *Geophys. Res. Lett.*, 26(14), 2037–2040.
* **Canonical Published Formulation**: Coupled 3D atmospheric general circulation and dynamic vegetation models solving Navier-Stokes moisture convergence.
* **Calibrated Simulation Formulation**:
  $$\alpha(V) = 0.38 - 0.21 V, \qquad P_{\text{conv}}(V) = 1.65 \, (0.38 - \alpha(V)) \, P_{\text{base}}, \qquad \frac{dV}{dt} = \frac{\sigma(P; 200, 50) - V}{\tau_V}$$
* **Physical Units**: $V \in [0, 1]$, $\alpha \in [0.17, 0.38]$, $P$ [$\text{mm/yr}$], $\tau_V = 10.0\,\text{yr}$.
* **Root Cause of Divergence**: Compresses 3D monsoon-albedo coupling into an $O(1)$ cusp-catastrophe ODE per cell that reproduces the mid-Holocene African Humid Period termination from Milankovitch precession alone.
* **Empirical Validation Grounding**: ODP Site 658C marine dust cores (deMenocal et al. 2000); Lake Mega-Chad paleo-shorelines.

---

### Entry 5: Hack Drainage-Area Closure for Whipple-Tucker Stream Power Incision
* **Subsystem**: `StreamPowerErosionEngine.java`
* **Canonical Literature**: Whipple, K. X., & Tucker, G. E. (1999). *J. Geophys. Res.*, 104(B8), 17661–17674 (DOI: `10.1029/1999JB900120`); Hack, J. T. (1957). *USGS Prof. Paper 294-B*.
* **Canonical Published Formulation**: $\dot{E} = K A^m S^n$ evaluated over recursive global DEM flow-routing trees ($O(N \log N)$ per tick).
* **Calibrated Simulation Formulation**:
  $$\dot{E} = K_{\text{erod}} A_{\text{drainage}}^m S^n \exp\!\left(-\frac{B_{\text{nat}}}{B_{\text{root}}}\right), \qquad A_{\text{drainage}} = \left(\frac{L_{\text{stream}}}{1.40}\right)^{1/0.57}$$
* **Physical Units**: $\dot{E}$ [$\text{m/yr}$], $A_{\text{drainage}}$ [$\text{km}^2$], $m = 0.50, n = 1.00$, $K_{\text{erod}} = 1.2 \times 10^{-4}\,\text{yr}^{-1}$, $B_{\text{root}} = 3\,000\,\text{kg/ha}$.
* **Root Cause of Divergence**: Inverting Hack's scaling law yields local upstream catchment area in $O(1)$ without global topographic tree traversal.
* **Empirical Validation Grounding**: Whipple & Tucker (1999); Montgomery (2007) global erosion synthesis.

---

### Entry 6: Exergy-Coupled Solow-Swan Physical Capital Equilibrium & Lavoisier Stoichiometry
* **Subsystem**: `UrbanKernel.java` & `native/ether-core-native/src/urban.rs`
* **Canonical Literature**: Solow, R. M. (1956). *QJE*, 70(1), 65–94 (DOI: `10.2307/1884513`); Garrett, T. J. (2011). *Climatic Change*, 104(3), 437–455; Krausmann, F., et al. (2017). *PNAS*, 114(8), 1880–1885.
* **Canonical Published Formulation**: Monetary capital accumulation $\dot{K} = s A(t) K^\alpha L^{1-\alpha} - \delta K$ with an exogenous Solow residual $A(t)$ and no material mass balance.
* **Calibrated Simulation Formulation**:
  $$\dot{k}_{\text{pc}} = \delta_0 \, k^*(P_{\text{watts}}, B_{\text{agr}}, N) - \delta_{\text{eff}}(C) \, k_{\text{pc}}$$
  $$k^*_{\text{pow}} = 5 + 580 \tanh\!\left(\frac{\operatorname{softplus}(P_{\text{watts}} - 348, 0.2)}{38}\right) + 62 \left(\frac{\operatorname{softplus}(P_{\text{watts}} - 400, 0.1)}{100}\right)^{1.95}$$
  $$k^* = 5 + \left[(k^*_{\text{pow}} - 5)\,\sigma(N; 50, 15) + 145 \tanh\!\left(\frac{\max(0, B_{\text{agr}})}{180 N + 1}\right)\sigma(N; 50, 15)\right] \cdot \max(\sigma(B_{\text{agr}}; 150, 25), \sigma(P_{\text{watts}}; 520, 25))$$
  with Krausmann raw-material debits ($5\%$ `metalResource`, $7\%$ `woodResource`, $10\%$ `clayResource`) on positive net additions $\Delta K^+ > 0$.
* **Physical Units**: $k_{\text{pc}}, k^*$ [$\text{kg/person}$], $P_{\text{watts}}$ [$\text{W/person}$], $B_{\text{agr}}$ [$\text{kg}$], $\delta_0 = 0.04\,\text{yr}^{-1}$, $\delta_{\text{eff}}(C) \in [0.04, 0.075]\,\text{yr}^{-1}$.
* **Root Cause of Divergence**: Eliminates exogenous `technologyLevel` / $A(t)$ (`AGENTS.md` §5.1), prevents $K/N \to \infty$ divergence during demographic collapse, and enforces Lavoisier mass conservation across Java and Rust.
* **Empirical Validation Grounding**: Krausmann et al. (2017) global material stocks ($1900\text{–}2010$); Smil (2017); Maddison (2020).

---

### Entry 7: Single-Variable Two-Phase Chemical & Radioecological Decay Closure
* **Subsystems**: `EcologicalDegradationEngine.java`, `ThermodynamicMigrationEngine.java`
* **Canonical Literature**: UNSCEAR (2000); Steinhauser, G., Brandl, A., & Johnson, T. E. (2014). *Sci. Total Environ.*, 470, 800–817 (DOI: `10.1016/j.scitotenv.2013.10.029`).
* **Canonical Published Formulation**: Multi-nuclide Bateman decay series $A(t) = \sum_k A_{k,0} e^{-\lambda_k t}$ requiring $K$ separate spatial isotope arrays.
* **Calibrated Simulation Formulation**:
  $$P(t + \Delta t) = \max\!\left(0, \; \min(P(t), 1000)\cdot 0.92^{\Delta t_{\text{mo}}} - 1.2\,\Delta t_{\text{mo}}\right) + \max(0, P(t) - 1000)\exp\!\left(-\frac{\ln 2}{30.0}\,\Delta t_{\text{yr}}\right)$$
* **Physical Units**: $P$ [intensive ecotoxicity/radiotoxicity index], $T_{1/2} = 30.0\,\text{yr}$ ($^{137}\text{Cs}/^{90}\text{Sr}$), exclusion threshold $P_{\text{excl}} = 1\,500$.
* **Root Cause of Divergence**: Compresses fast chemical effluent attenuation and multi-decadal $^{137}\text{Cs}/^{90}\text{Sr}$ exclusion persistence into a single `float[] pollutionLevel` array in `WorldBuffer`.
* **Empirical Validation Grounding**: Chernobyl 30-km Exclusion Zone ($1986\text{–present}$) and Fukushima evacuation persistence (Steinhauser et al. 2014).

---

### Entry 8: Macroscopic Converter Capacity Parameterization ($k_{\text{pc}} \mapsto A_{\text{conv}}, T_H$)
* **Subsystems**: `ThermodynamicPrimeMoversEngine.java`, `RenewableEnergyPhysicsEngine.java`, `KummelAyresExergyEngine.java`
* **Canonical Literature**: Carnot, S. (1824); Betz, A. (1926); Shockley, W., & Queisser, H. J. (1961) (DOI: `10.1063/1.1736034`); Ayres, R. U., & Warr, B. (2009).
* **Canonical Published Formulation**: Carnot, Weisbach, Betz, and Shockley-Queisser laws express power and efficiency in terms of explicit machine swept areas $(A_{\text{rotor}}, A_{\text{PV}})$ and boiler temperatures $T_H$, which are unobservables at macroscopic H3 resolution without factory-level asset registries.
* **Calibrated Simulation Formulation**:
  Maps conserved physical capital per capita $k_{\text{pc}} = K_i / N_i$ smoothly via $C^1$ sigmoids onto installed hydraulic wheel efficiency ($k_{\text{pc}} \gtrsim 120\,\text{kg/cap}$), wind/boiler working temperatures $T_H \in [390, 1\,840]\,\text{K}$ ($k_{\text{pc}} \gtrsim 950\text{–}2\,800\,\text{kg/cap}$), and turbine/PV capture area ($k_{\text{pc}} \gtrsim 4\,500\text{–}5\,000\,\text{kg/cap}$), enforcing $E_{\text{wind}} = E_{\text{solar}} = 0.0\,\text{GJ/yr}$ when $N_i \le 0$ or $k_{\text{pc}} \le 0$ and debiting fuel stocks via $\text{LHV}_{\text{fuel}}$.
* **Physical Units**: $k_{\text{pc}}$ [$\text{kg/cap}$], $T_C, T_H$ [$\text{K}$], $\text{LHV}_{\text{wood}} = 15\,\text{GJ/t}$, $\text{LHV}_{\text{coal}} = 24\,\text{GJ/t}$, $\text{LHV}_{\text{oil}} = 42\,\text{GJ/t}$, $\text{LHV}_{\text{gas}} = 50\,\text{GJ/t}$.
* **Root Cause of Divergence**: Closes machine geometry from conserved `resourceCapital` without introducing a forbidden `technologyLevel` state variable (`AGENTS.md` §5.1).
* **Empirical Validation Grounding**: Smil (2017) prime-mover historical chronology; Ayres & Warr (2009) useful exergy series.

---

## 🏛️ 3. Category B: Tier 2 Cliodynamic, Macroeconomic & Diagnostic Closures

### Entry 9: Garrett-Lotka Emergent Exergy-Capital Index ($\Phi_{\text{exergy}}$)
* **Subsystems**: `H3CellPhysicsAndStateHelper.java`, `WorldBuffer.java`
* **Canonical Literature**: Garrett, T. J. (2011). *Climatic Change*, 104(3), 437–455 (DOI: `10.1007/s10584-010-9943-4`); Lotka, A. J. (1922). *PNAS*, 8(6), 147–151.
* **Canonical Published Formulation**: Linear global wealth-power proportionality $P(t) = \lambda_G W(t)$ expressed in 1990 market US dollars.
* **Calibrated Simulation Formulation**:
  $$\Phi_{\text{exergy}}(p, k_{\text{pc}}) = 0.85 + 0.50 \log_{10}\!\left(1 + \frac{k_{\text{active}}}{120.0}\right) + 0.70 \log_{10}\!\left(1 + \frac{\operatorname{softplus}(p - 348.0, 5.0)}{250.0}\right)$$
* **Physical Units**: $p$ [$\text{W/cap}$] ($p_{\text{somatic}} = 348\,\text{W/cap}$, Kleiber adult basal $97\,\text{W}$), $k_{\text{active}} \le k_{\max}(p)$ [$\text{kg/cap}$], $\Phi_{\text{exergy}}$ [dimensionless diagnostic].
* **Root Cause of Divergence**: Provides a read-only dimensionless thermodynamic index over SI observables without currency units or post-collapse $K/N$ blow-up.
* **Empirical Validation Grounding**: Garrett (2011); Krausmann et al. (2017); Maddison Project Database (2020).

---

### Entry 10: Discrete H3 Darcy-Boussinesq Groundwater Stencil Closure
* **Subsystem**: `AquiferDepletionEngine.java`
* **Canonical Literature**: Darcy, H. (1856); Wada, Y., et al. (2010). *Geophys. Res. Lett.*, 37(20), L20402 (DOI: `10.1029/2010GL044571`).
* **Canonical Published Formulation**: Continuous 2D PDE $S_s \frac{\partial h}{\partial t} = \nabla \cdot (T \nabla h) + R - W$ on structured Cartesian grids.
* **Calibrated Simulation Formulation**:
  $$\frac{\partial h_i}{\partial t} = \frac{1}{S_s} \left[ \sum_{j \in \text{Ring}_1(i)} T_{ij} \frac{h_j - h_i}{d_{ij}} + R_{\text{rech},i} - \frac{N_i w_{\text{crop}}(k_{\text{pc},i}) \eta_{\text{pump}}(p_i, k_{\text{pc},i})}{A_{\text{cell},i}} \right]$$
* **Physical Units**: $h_i$ [$\text{m}$], $A_{\text{cell},i}$ [$\text{m}^2$], $d_{ij}$ [$\text{m}$], $W_i$ [$\text{mm/yr}$], $S_s = 0.01$.
* **Root Cause of Divergence**: Discretizes lateral Boussinesq diffusion across unstructured geodesic H3 Ring-1 neighbors at $O(6)$ per cell with resolution-invariant areal pumping.
* **Empirical Validation Grounding**: UNESCO WHYMAP; FAO AQUASTAT; Wada et al. (2010).

---

### Entry 11: Physical Mass Embodiment of Arthur Combinatorial Evolution
* **Subsystem**: `ArthurCombinatorialTechnologyEngine.java`
* **Canonical Literature**: Arthur, W. B., & Polak, W. (2006). *Complexity*, 11(5), 23–31 (DOI: `10.1002/cplx.20130`); Arthur, W. B. (2009); Kremer, M. (1993). *QJE*, 108(3), 681–716.
* **Canonical Published Formulation**: Combinatorial growth over discrete abstract idea modules $dM_t / dt \propto N M_t^\alpha$ ($\alpha > 1$).
* **Calibrated Simulation Formulation**:
  $$\frac{dK}{dt} = \mu_{\text{comb}} N \left(1 + \frac{k_{\text{pc}}}{120.0}\right)^{1.35} \ln\!\left(1 + \frac{N}{100.0}\right) \frac{1}{1 + k_{\text{pc}} / 50\,000.0} - \delta_{\text{wear}} K$$
* **Physical Units**: $K$ [$\text{kg}$], $k_{\text{pc}}$ [$\text{kg/cap}$], $\mu_{\text{comb}} = 0.045\,\text{kg/(person}\cdot\text{yr)}$, $\delta_{\text{wear}} = 0.008\,\text{yr}^{-1}$.
* **Root Cause of Divergence**: Maps combinatorial variety onto conserved physical tool mass $K$ bounded by `TypeBEnginePassRunner`'s Carnot/Krausmann budget (`AGENTS.md` §4.2).
* **Empirical Validation Grounding**: Kremer (1993); Krausmann et al. (2017).

---

### Entry 12: Chalcolithic Pyrometallurgical Hill Gate for Hotelling Extraction
* **Subsystem**: `HotellingResourceDepletionEngine.java`
* **Canonical Literature**: Hotelling, H. (1931). *JPE*, 39(2), 137–175 (DOI: `10.1086/254195`); Radivojević, M., et al. (2010). *J. Archaeol. Sci.*, 37(11), 2775–2787.
* **Canonical Published Formulation**: Intertemporal exhaustible-resource extraction active for all $t \ge 0$.
* **Calibrated Simulation Formulation**:
  $$E_{\text{kiln}}(k_{\text{pc}}) = \frac{k_{\text{pc}}^3}{k_{\text{pc}}^3 + 65.0^3}, \qquad R_{\text{extract}} = 0.02 N \, E_{\text{kiln}}(k_{\text{pc}}) \cdot 2.5 \tanh\!\left(\frac{\ln(1 + k_{\text{pc}}/50)}{3.5}\right) \frac{M_{\text{ore}}}{M_{\text{ore}} + 100}$$
* **Physical Units**: $k_{\text{chalco}} = 65.0\,\text{kg/cap}$, $R_{\text{extract}}$ [$\text{t/yr}$], $M_{\text{ore}}$ [$\text{t}$].
* **Root Cause of Divergence**: Prevents unphysical Pleistocene ore depletion ($k_{\text{pc}} \approx 5\,\text{kg/cap} \implies E_{\text{kiln}} < 5 \times 10^{-4}$) without calendar date checks (`AGENTS.md` §5.2–5.3).
* **Empirical Validation Grounding**: Radivojević et al. (2010) Balkan Chalcolithic smelting onset ($\sim -5000\,\text{BCE}$).

---

### Entry 13: Physical Carrier Bandwidth Closure for Shannon Channel Capacity
* **Subsystem**: `InformationEntropyEngine.java`
* **Canonical Literature**: Shannon, C. E. (1948). *Bell Syst. Tech. J.*, 27(3), 379–423 (DOI: `10.1002/j.1538-7305.1948.tb01338.x`); Brillouin, L. (1956).
* **Canonical Published Formulation**: $C = B \log_2(1 + S/N)$ for a prescribed electrical bandwidth $B$ [$\text{Hz}$].
* **Calibrated Simulation Formulation**:
  $$B_{\text{carrier}}(k_{\text{pc}}, p) = 1.0 + 0.05\,k_{\text{pc}} + 0.002\,\left(\operatorname{softplus}(p - 348.0, 5.0)\right)^2, \qquad C = B_{\text{carrier}} \log_2\!\left(1 + \frac{10.0}{\mu_{\text{spatial}}}\right)$$
* **Physical Units**: $B_{\text{carrier}}$ [$\text{Hz}$], $C$ [$\text{bits/s}$], $k_{\text{pc}}$ [$\text{kg/cap}$], $p$ [$\text{W/cap}$].
* **Root Cause of Divergence**: Expresses carrier bandwidth continuously from road/print capital and exosomatic electric power without discrete media eras.
* **Empirical Validation Grounding**: Buringh & van Zanden (2009); ITU historical telegraph/telecom series.

---

### Entry 14: Cellular Caloric & Density Normalization of Turchin-Goldstone SDT ($\Psi_i$)
* **Subsystems**: `TurchinGoldstoneSDTEngine.java`, `SimulationMetricsAggregator.java`
* **Canonical Literature**: Goldstone, J. A. (1991); Turchin, P. (2016). *Ages of Discord*. Beresta Books.
* **Canonical Published Formulation**: National-level dimensionless product $\Psi = (w_0/w)(N_{\text{urb}}/N) \cdot (e/e_0)(y_e/y_0) \cdot (D/R)$ over monetary wages and tax ledgers.
* **Calibrated Simulation Formulation**:
  $$\text{MMP}_i = \max\!\left(0.2,\; \frac{1.9}{\max(0.2, E_{\text{cal},i}/N_i)} \tanh\!\left(\frac{\rho_i}{150}\right)\right), \qquad \text{EMP}_i = \max\!\left(0.2,\; \left(\frac{\max(0, G_i - 0.25)}{0.20}\right)^2\right)$$
  $$\text{SFD}_i = \max\!\left(0.2,\; \frac{0.76}{\max(0.2, E_{\text{cal},i}/N_i)} \tanh\!\left(\frac{k_{\text{pc},i}}{800}\right)\right), \qquad \Psi_i = \text{MMP}_i \cdot \text{EMP}_i \cdot \text{SFD}_i$$
* **Physical Units**: $E_{\text{cal},i}/N_i$ [$\text{GJ/(person}\cdot\text{yr)}$], $\rho_i = N_i / A_{\text{cell},i}$ [$\text{hab/km}^2$], $G_i \in [0, 1]$.
* **Root Cause of Divergence**: Evaluates SDT locally on conserved cellular calories and geodesic density without unbounded division-by-zero singularities.
* **Empirical Validation Grounding**: Seshat Global History Databank; Turchin & Nefedov (2009) *Secular Cycles*.

---

### Entry 15: Krugman Core-Periphery Ring-1 + Rest-of-World Mean-Field Decomposition
* **Subsystem**: `KrugmanCorePeripheryEngine.java`
* **Canonical Literature**: Krugman, P. (1991). *JPE*, 99(3), 483–499 (DOI: `10.1086/261763`); Fujita, M., Krugman, P., & Venables, A. J. (1999).
* **Canonical Published Formulation**: All-pairs $R \times R$ Dixit-Stiglitz price index $G_i = [\sum_{j=1}^R \lambda_j T_{ji}^{1-\sigma}]^{\frac{1}{1-\sigma}}$ and wage equation ($O(N^2)$ per tick).
* **Calibrated Simulation Formulation**:
  Evaluates Krugman's exact $(G_i, w_i, \omega_i = w_i G_i^{-\mu_M})$ ($\sigma = 4.0$, $\mu_M = 0.40$) over $\mathcal{N}_i = \{i\} \cup \text{Ring}_1(i) \cup \{0_{\text{RoW}}\}$ using geodesic centroid spacing $d_{ij} = \sqrt{\frac{2}{\sqrt{3}} A_{\text{cell},i}}$, areal shares $\lambda_j \propto K_j / A_j$, and symmetric reference normalization $(G_{\text{sym},i}, w_{\text{sym},i})$.
* **Physical Units**: $A_{\text{cell},i}$ [$\text{km}^2$], $d_{ij}$ [$\text{km}$], $\tau_0 = 0.004\,\text{km}^{-1}$, $\gamma_K = 0.12\,\text{yr}^{-1}$.
* **Root Cause of Divergence**: Reduces $O(N^2)$ global pairwise summation across $2.88 \times 10^5$ H3 cells to $O(6) = O(1)$ per cell while preserving Krugman's exact functional equations and `Contract 3` area invariance.
* **Empirical Validation Grounding**: Krugman (1991); Scheidel ORBIS (2014); Maddison (2020).

---

### Entry 16: Resolution-Invariant Areal Metal Dissipation Concentration
* **Subsystems**: `EntropicMetalDissipationEngine.java`, `InfrastructureEnergyEngine.java`
* **Canonical Literature**: Bihouix, P. (2014); Baccini, P., & Brunner, P. H. (2012). *Metabolism of the Anthroposphere*. MIT Press.
* **Canonical Published Formulation**: Global 0D dissipated metal inventory $\Delta M = M_0(1 - e^{-\lambda t})$ without spatial areal normalization.
* **Calibrated Simulation Formulation**:
  $$\Delta P_i = 0.10 \cdot \frac{\Delta M_i}{A_{\text{cell},i}}, \qquad \lambda_{\text{diss}} = 0.015\,\text{yr}^{-1}$$
* **Physical Units**: $\Delta M_i$ [$\text{t}$], $A_{\text{cell},i}$ [$\text{km}^2$], $\Delta P_i$ [$\text{pollution units/km}^2$].
* **Root Cause of Divergence**: Dividing extensive dissipated metal mass by `cellAreaKm2[i]` guarantees identical intensive ecotoxicity across H3 Resolutions 4–7 (`Contract 3`).
* **Empirical Validation Grounding**: Baccini & Brunner (2012); USGS mineral dissipation balances.

---

### Entry 17: Physical Corn-Wage Ricardian Profit-Squeeze Closure
* **Subsystem**: `RicardoDifferentialLandRentEngine.java`
* **Canonical Literature**: Ricardo, D. (1817); Samuelson, P. A. (1959). *QJE*, 73(1), 1–35 (DOI: `10.2307/1883825`).
* **Canonical Published Formulation**: Monetary corn-price rent and profit squeeze $r = r_0(1 - w_{\text{sub}}/MP_L)$ with hard cutoff at $MP_L = w_{\text{sub}}$.
* **Calibrated Simulation Formulation**:
  $$MP_{L,i} = \beta_A \frac{B_{\text{agr},i}}{N_i}, \qquad \theta_w = \frac{w_{\text{sub}}}{MP_{L,i}}, \qquad s_R = \frac{\operatorname{softplus}(\theta_w - 1, 0, 8.0)}{1 + \operatorname{softplus}(\theta_w - 1, 0, 8.0)}$$
  driving $\Delta K_i = -0.035\,s_R\,K_i\,\Delta t$ and $\Delta G_i = +0.02\,s_R\,\Delta t$ above landed estate threshold $k_{\text{pc},i} \ge 40\,\text{kg/cap}$.
* **Physical Units**: $\beta_A = 0.65$, $w_{\text{sub}} = 0.78\,\text{kg/cap}$ standing crop norm, $k_{\text{floor}} = 40\,\text{kg/cap}$.
* **Root Cause of Divergence**: Formulates Samuelson's Ricardian stationary squeeze directly over physical standing grain and population stocks with $C^\infty$ `softplus` sharpness $8.0$.
* **Empirical Validation Grounding**: Clark (2007) English agricultural land rents ($1200\text{–}1850$).

---

### Entry 18: Smooth $C^\infty$ Lewis & Ranis-Fei Dual-Sector Commercialization Closure
* **Subsystem**: `LewisDualSectorTurningPointEngine.java`
* **Canonical Literature**: Lewis, W. A. (1954). *Manchester School*, 22(2), 139–191 (DOI: `10.1111/j.1467-9957.1954.tb00021.x`); Ranis, G., & Fei, J. C. H. (1961). *AER*, 51(4), 533–565.
* **Canonical Published Formulation**: Piecewise two-sector wage switch $w = \max(\bar{w}_S, MP_{L,A})$ creating a $C^0$ derivative kink at $MP_{L,A} = \bar{w}_S$.
* **Calibrated Simulation Formulation**:
  Evaluates $MP_{L,A} = 0.65\,(B_{\text{agr},i} + 1)/N_i$ against $\bar{w}_S = 0.65$ via smooth logistic transitions across early industrial widening ($k_{\text{pc}} \ge 80\,\text{kg/cap}$) and post-turning-point labor-scarce mechanization ($k_{\text{pc}} \ge 250\,\text{kg/cap}$, $\text{Trade} \ge 20$).
* **Physical Units**: $B_{\text{agr},i}$ [$\text{kg}$], $k_{\text{pc},i}$ [$\text{kg/cap}$].
* **Root Cause of Divergence**: Regularizes the Ranis-Fei commercialization kink onto single-cell `WorldBuffer` state arrays (`AGENTS.md` §5.2).
* **Empirical Validation Grounding**: Ranis & Fei (1961); Minami (1973) Japanese Lewis turning point.

---

### Entry 19: Elapsed-Phase Kondratiev-Schumpeter Long Wave & Scrap Metal Recovery
* **Subsystem**: `KondratievSchumpeterLongWaveEngine.java`
* **Canonical Literature**: Kondratiev, N. D. (1925); Schumpeter, J. A. (1939). *Business Cycles*. McGraw-Hill.
* **Canonical Published Formulation**: Qualitative/statistical $48\text{–}60\,\text{yr}$ price and fixed-asset investment waves without explicit physical scrap mass accounting.
* **Calibrated Simulation Formulation**:
  $$W_k(\tau, k_{\text{pc}}) = \sin\!\left(\frac{2\pi (\tau \bmod 52.0)}{52.0}\right) \cdot \sigma(k_{\text{pc}}; 120.0, 30.0), \qquad \Delta K = (0.015 + 0.025 W_k) K \Delta t$$
  When $W_k < -0.50$, creative-destruction scrapping deducts $\delta_s = 0.015 |W_k| \Delta t$ from $K$ and returns $15\%$ of scrapped mass to `metalResource` via `PhysicalUnitsHelper.kgToMetricTonnes`.
* **Physical Units**: $T_K = 52.0\,\text{yr}$ elapsed integration time $\tau$ (zero calendar-year reference), $k_{\text{floor}} = 120.0\,\text{kg/cap}$, $f_{\text{scrap}} = 0.15$.
* **Root Cause of Divergence**: Anchors Schumpeterian creative destruction in calendar-free elapsed infrastructure age and Lavoisier scrap recovery (`Contract 1 & 2`).
* **Empirical Validation Grounding**: Kondratiev (1925); Freeman & Louçã (2001).

---

### Entry 20: Cellular Pomeranz-Wrigley Subterranean Forest & Organic Involution Closure
* **Subsystem**: `PomeranzGreatDivergenceEngine.java`
* **Canonical Literature**: Pomeranz, K. (2000). *The Great Divergence*. Princeton University Press; Wrigley, E. A. (2010). *Energy and the English Industrial Revolution*. Cambridge University Press.
* **Canonical Published Formulation**: Comparative regional land-equivalence balances ($A_{\text{ghost,coal}} = E_{\text{coal}} / y_{\text{wood}}$) contrasting Britain and the Yangtze Delta.
* **Calibrated Simulation Formulation**:
  $$A_{\text{coal},i} = \frac{M_{\text{coal},i} \cdot \text{LHV}_{\text{coal}}}{y_{\text{wood}}}, \qquad \Phi_{\text{coal},i} = \sigma(A_{\text{coal},i}; A_0, s_A)\,(1 - \sigma(\mu_i; 0.70, 0.15))$$
  $$\Omega_{\text{div},i} = \Phi_{\text{coal},i}\left(1 + 0.80\,\sigma(\text{Trade}_i; 25, 5)\,\sigma(k_{\text{pc},i}; 100, 30)\right)$$
  driving industrial capital breakout when $\Omega_{\text{div},i} > 0.15$, versus organic-economy involution drag and domestic woodland firewood drawdown ($\Delta B_{\text{nat},i} = -0.012(1 - \Phi_{\text{coal},i}) B_{\text{nat},i}\Delta t$) when mineral coal is inaccessible.
* **Physical Units**: $\text{LHV}_{\text{coal}} = 24\,\text{GJ/t}$, $y_{\text{wood}} = 7\,500\,\text{GJ/(km}^2\cdot\text{yr)}$, $A_0 = 1.60\,\text{km}^2$ ($M_{\text{coal}} = 500\,\text{t}$).
* **Root Cause of Divergence**: Translates Wrigley's subterranean forest and Pomeranz's ghost acreage into a continuous $O(1)$ cellular operator over conserved coal and woodland stocks.
* **Empirical Validation Grounding**: Pomeranz (2000); Wrigley (2010); Allen (2009).

---

### Entry 21: Von Thünen Sub-Grid Supply Ring & Rolling-Stock Friction Attenuation
* **Subsystem**: `VonThunenAgriculturalRingsEngine.java`
* **Canonical Literature**: von Thünen, J. H. (1826). *Der isolierte Staat*; Bairoch, P. (1988). *Cities and Economic Development*.
* **Canonical Published Formulation**: Radial point-market rent $R(r) = Y(p - c - f \cdot r)$ over explicit concentric farm rings.
* **Calibrated Simulation Formulation**:
  Resolves sub-cell supply hinterland radius $R_{\text{ring}} = \sqrt{N_i e_{\text{meta}} / (\pi y_{\text{agr},i})}$ from $y_{\text{agr},i} = E_{\text{cal},i} / A_{\text{cell},i}$, deducts the $r_0 = 5\,\text{km}$ village field-walk radius via `softplus`, attenuates modal transport energy loss $c_{\text{eff}} = \frac{c_{\text{mode}}\mu_i}{1 + k_{\text{pc},i}/250}$, and applies the exact circular-disk loss integral $L_{\text{disk}}(u) = 1 - \frac{2}{u^2}[1 - (1+u)e^{-u}]$.
* **Physical Units**: $r_0 = 5.0\,\text{km}$, $c_{\text{land}} = 0.008\,\text{km}^{-1}$, $c_{\text{water}} = 0.0008\,\text{km}^{-1}$, $e_{\text{meta}} = 3.65\,\text{GJ/(person}\cdot\text{yr)}$.
* **Root Cause of Divergence**: Resolves sub-grid urban food-shed logistics analytically on H3 hexagons ($36\text{–}1\,770\,\text{km}^2$) without $O(N^2)$ multi-ring pathfinding.
* **Empirical Validation Grounding**: von Thünen (1826); Clark & Haswell (1967); Bairoch (1988).

---

### Entry 22: Multi-Theory Capital Envelope & Cellular Institutional Closures
* **Subsystems**: `TypeBEnginePassRunner.java`, `EnvironmentalKernel.java` / `environmental.rs`, `TasmanianCulturalRegressionEngine.java`, `AcemogluRobinsonInstitutionsEngine.java`, `AxialAgeMoralizingGodsEngine.java`, `PolanyiFictitiousCommoditiesEngine.java`, `BagehotFractionalReserveEngine.java`, `CordenNearyDutchDiseaseEngine.java`, `InnisStaplesThesisEngine.java`
* **Canonical Literature**: Krausmann et al. (2017); Lieth (1975); Henrich (2004); Acemoglu, Johnson & Robinson (2005); Whitehouse et al. (2019); Polanyi (1944); Bagehot (1873); Corden & Neary (1982); Innis (1930).
* **Canonical Published Formulation**: Standalone macro-institutional models that do not define simultaneous co-execution budgets or conserved cellular material debits.
* **Calibrated Simulation Formulation**:
  1. `TypeBEnginePassRunner` combines overlapping Tier 2 capital derivatives via upper/lower envelopes ($\Delta K_{\text{env}}^+ = \max_e \Delta K_e^+$, $\Delta K_{\text{env}}^- = \min_e \Delta K_e^-$), bounds net positive deepening by remaining Carnot exergy surplus ($s_{\text{marg}} = 0.12$, $e_{\text{embodied}} = 15\,\text{MJ/kg}$), and debits Krausmann stoichiometry ($5\%$ metal, $7\%$ timber, $10\%$ clay).
  2. `TasmanianCulturalRegressionEngine` regularizes Henrich's (2004, Eq. 3) logarithmic loss $\beta \ln(N^* / N_e)$ ($N^* = 5\,000$, $\beta = 0.025\,\text{yr}^{-1}$) via `softplus` so connected populations ($N_e \ge N^*$) incur zero capital atrophy; `AcemogluRobinsonInstitutionsEngine` maps `Gini` onto elite expropriation wedge $\tau_{\text{extr}}$ ($r_K = 0.020\,\text{yr}^{-1}$, $\delta_{\text{inst}} = 0.007\,\text{yr}^{-1}$) gated by $\sigma(k_{\text{pc}}; 100, 30)$; qualitative institutional Type B engines use smooth $C^1$ functions of $(N_i, k_{\text{pc},i}, \text{Trade}_i)$ while debiting upstream stocks; `EnvironmentalKernel` scales Miami NPP to Res-4 reference crop potential ($1\,770.49\,\text{km}^2$).
* **Root Cause of Divergence**: Prevents $M$-fold capital double-counting and *ex nihilo* mass creation when multiple Tier 2 theories are active (`AGENTS.md` §4.2).
* **Empirical Validation Grounding**: Krausmann et al. (2017); Falsification Debates 5, 7 & 11; HYDE 3.4 & Maddison (2020).

---

### Entry 23: Post-Hoc $\Phi_{\text{exergy}}$ Macro-Telemetry Diagnostic Closures
* **Subsystems**: `CliodynamicFormulasRegistry.java`, `H3CellPhysicsAndStateHelper.java`, `DemographicMetricsHelper.java`, `CompiledTelemetryPlan.java`
* **Canonical Literature**: Maddison (2020); Smil (2017); Montgomery (2007); Buringh & van Zanden (2009); Reinhart & Rogoff (2009); Harley (1988); Scheidel (2014).
* **Canonical Published Formulation**: Historical benchmark datasets report monetary GWP (1990 Int\$), adult literacy %, sovereign debt/GDP %, ard/mechanized topsoil loss, freight indices, and dispatch speeds rather than raw SI cell masses.
* **Calibrated Simulation Formulation**:
  Evaluates strictly read-only post-hoc telemetry diagnostics as $C^1$ response functions of conserved cellular observables and $\Phi_{\text{exergy}}(p, k_{\text{active}})$ (`CliodynamicFormulasRegistry`), enforcing exact `0.0` Paleolithic baselines (`AGENTS.md` §5.3) and zero feedback into runtime physics kernels.
* **Root Cause of Divergence**: Bridges conserved SI `WorldBuffer` state variables to the 27 1D historical validation series without storing non-physical state arrays or calendar lookup tables.
* **Empirical Validation Grounding**: `HistoricalValidationKernel` benchmark suite across Paleolithic, Classical, Medieval, Early Modern, and Industrial horizons.

---

### Entry 24: Asymmetric Directed Cultural Affinity & Defensive Grievance Closure
* **Subsystems**: `CulturalEntity.java`, `WarDiplomacyEngine.java`, `H3BufferAndEventCoordinator.java`
* **Canonical Literature**: Shepard, R. N. (1987). *Science*, 237(4820), 1317–1323 (DOI: `10.1126/science.3629243`); Axelrod, R. (1997); Allport, G. W. (1954).
* **Canonical Published Formulation**: Symmetric exponential similarity $A_0(i, j) = \exp(-\lambda \|\mathbf{T}_i - \mathbf{T}_j\|_2)$ ($A_{ij} = A_{ji}$).
* **Calibrated Simulation Formulation**:
  $$A(i \to j) = \operatorname{clamp}\!\left(\exp(-3.5\,d_{ij}) + 0.12\tanh(2.5(T_{j,1} - T_{i,1})) - 0.15\,T_{i,3}|T_{i,3} - T_{j,3}|, \; 0.01, \; 1.0\right)$$
  with active-mesh spatial bounding (`minDistanceSq <= maxRadiusSq`) on polity instantiation and $\Delta t_{\text{yr}}$-scaled wartime affinity erosion incorporating a $1.6\times$ defensive grievance multiplier.
* **Root Cause of Divergence**: Captures directed core-periphery prestige asymmetry, doctrinal exclusivism, and $\Delta t$-invariant defensive irredentism on $N \times N$ polity graphs.
* **Empirical Validation Grounding**: Seshat polity interaction records; regional high-resolution verification runs (`tokugawa_japan`, `mali_empire`, `industrial_1800`).

---

## 📊 4. Central Summary Matrix of All Calibrated Subsystems (Entries 1 to 24)

| # | Category | Subsystem | Canonical Literature | Calibrated Formulation | Root Cause / Mesh Closure |
|---|---|---|---|---|---|
| **1** | Tier 1 | `DynamicEnergyBudgetEngine` | Kooijman (2010), Cahill (1970) | Cohort $(V, E)$ ODE & quadratic $\mu_{\text{starv}}$ | $O(1)$ cellular cohort aggregation of individual DEB |
| **2** | Tier 1 | `EcologicalStoichiometryEngine` | Sterner & Elser (2002), Loladze (2014) | $C^\infty$ `softmin` Liebig & `biomassAgriculture` debit | Smooth gradient & Lavoisier grain-to-GJ mass balance |
| **3** | Tier 1 | `ThermohalineStommelAMOCEngine` | Stommel (1961), Broecker (1998) | Dimensional Sverdrup 2-box & $-0.22$ seesaw | Physical units & orbital meltwater without 3D OGCM |
| **4** | Tier 1 | `CharneyVegetationAlbedoEngine` | Charney (1975), Claussen (1999) | Analytical Sahara $\alpha(V)$ fold bifurcation | $O(1)$ cusp catastrophe replacing 3D AGCMs |
| **5** | Tier 1 | `StreamPowerErosionEngine` | Whipple & Tucker (1999), Hack (1957) | $\dot{E} = K A^m S^n$ via inverted Hack law | $O(1)$ catchment area without global DEM routing |
| **6** | Tier 1 | `UrbanKernel` / `urban.rs` | Solow (1956), Garrett (2011), Krausmann (2017) | $\dot{k}_{\text{pc}} = \delta_0 k^*(P, B_{\text{agr}}, N) - \delta_{\text{eff}} k_{\text{pc}}$ | Exergy-grounded capital without $A(t)$ + Lavoisier debit |
| **7** | Tier 1 | `EcologicalDegradationEngine` | UNSCEAR (2000), Steinhauser (2014) | Chemical ($0.92^{\Delta t_{\text{mo}}}$) + $^{137}\text{Cs}$ ($T_{1/2}=30\,\text{yr}$) | Single-array closure preserving $>80\,\text{yr}$ nuclear zones |
| **8** | Tier 1 | `ThermodynamicPrimeMoversEngine` | Carnot (1824), Betz (1926), Shockley (1961) | $k_{\text{pc}} \mapsto (A_{\text{rotor}}, A_{\text{PV}}, T_H)$ & fuel LHV debit | Closes converter geometry without `technologyLevel` array |
| **9** | Tier 2 | `H3CellPhysicsAndStateHelper` | Garrett (2011), Lotka (1922) | $\Phi_{\text{exergy}}(p, k_{\text{active}})$ diagnostic | Currency-free thermodynamic diagnostic index |
| **10** | Tier 2 | `AquiferDepletionEngine` | Darcy (1856), Wada et al. (2010) | Ring-1 $\sum T_{ij}\Delta h_{ij}/d_{ij}$ & areal pumping | Unstructured geodesic H3 Boussinesq discretization |
| **11** | Tier 2 | `ArthurCombinatorialTechnologyEngine` | Arthur & Polak (2006), Kremer (1993) | $dK/dt \propto N(1 + k_{\text{pc}}/120)^{1.35}$ | Embodies abstract module counts into conserved kg |
| **12** | Tier 2 | `HotellingResourceDepletionEngine` | Hotelling (1931), Radivojević (2010) | Hill furnace gate $k_{\text{pc}}^3/(k_{\text{pc}}^3 + 65^3)$ | Enforces zero Pleistocene smelting without calendar checks |
| **13** | Tier 2 | `InformationEntropyEngine` | Shannon (1948), Brillouin (1956) | $B_{\text{carrier}}(k_{\text{pc}}, p)$ bandwidth closure | Grounds channel capacity in physical roads & power |
| **14** | Tier 2 | `TurchinGoldstoneSDTEngine` | Goldstone (1991), Turchin (2016) | Bounded spatial $\Psi_i$ over $E_{\text{cal},i}/N_i$ | Singularity-free cellular SDT without currency wages |
| **15** | Tier 2 | `KrugmanCorePeripheryEngine` | Krugman (1991), Fujita et al. (1999) | Ring-1 + RoW Dixit-Stiglitz $(G_i, w_i, \omega_i)$ | $O(6)$ geodesic closure replacing $O(N^2)$ all-pairs matrix |
| **16** | Tier 2 | `EntropicMetalDissipationEngine` | Bihouix (2014), Baccini & Brunner (2012) | $\Delta P_i = 0.10\,\Delta M_i / A_{\text{cell},i}$ | Resolution-invariant areal concentration (`Contract 3`) |
| **17** | Tier 2 | `RicardoDifferentialLandRentEngine` | Ricardo (1817), Samuelson (1959) | $MP_L = \beta_A(B_{\text{agr}}/N)$, `softplus` sharpness $8.0$ | Physical grain/capita profit squeeze without prices |
| **18** | Tier 2 | `LewisDualSectorTurningPointEngine` | Lewis (1954), Ranis & Fei (1961) | $C^\infty$ transition across $MP_{L,A} \lessgtr \bar{w}_S$ | Regularizes piecewise wage kink on single-cell arrays |
| **19** | Tier 2 | `KondratievSchumpeterLongWaveEngine` | Kondratiev (1925), Schumpeter (1939) | $\sin(2\pi(\tau \bmod 52)/52)$ & $15\%$ scrap recovery | Calendar-free infrastructure phase & Lavoisier metal scrap |
| **20** | Tier 2 | `PomeranzGreatDivergenceEngine` | Pomeranz (2000), Wrigley (2010) | $A_{\text{coal}} = M_{\text{coal}}\text{LHV}/y_{\text{wood}}$ & wood drawdown | Cellular subterranean forest & colonial ghost acreage |
| **21** | Tier 2 | `VonThunenAgriculturalRingsEngine` | von Thünen (1826), Bairoch (1988) | $L_{\text{disk}}(c_{\text{eff}}R_{\text{ring}})$ analytical integral | Sub-grid circular hinterland integration on H3 cells |
| **22** | Tier 2 | `TypeBEnginePassRunner` & Type B | Krausmann (2017), Henrich (2004), Acemoglu | $\Delta K^+_{\text{phys}} = \min(\max_e \Delta K^+_e, \Delta K_{\text{Carnot}})$ | Zero $M$-fold double-counting & Carnot/Krausmann bounds |
| **23** | Telemetry | `CliodynamicFormulasRegistry` | Maddison (2020), Smil (2017), Montgomery | Read-only $C^1$ diagnostics over $(p, k_{\text{active}})$ | Maps conserved SI states to 27 historical series |
| **24** | Tier 2 | `CulturalEntity` & `WarDiplomacyEngine` | Shepard (1987), Axelrod (1997), Allport (1954) | Directed $A(i \to j)$ prior & $1.6\times$ war grievance | Asymmetric prestige/dogma & $\Delta t_{\text{yr}}$-scaled conflict |
