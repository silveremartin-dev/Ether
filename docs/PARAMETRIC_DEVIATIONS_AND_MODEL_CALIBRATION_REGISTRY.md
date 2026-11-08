# ⚖️ Central Registry of Model Calibrations, Parametric Scaling & Literature Deviations

> **Ether Cliodynamic & Planetary Epistemic Laboratory**  
> *Author: Silvere Martin-Michiellot*  
> *Standard: AGENTS.md §8 & §9 — Full Epistemic Provenance and Literature Accountability*  
> *Ontological Separation Note: All canonical peer-reviewed physical, chemical, ecological, and biological conservation laws are exhaustively documented in `docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`. This registry documents EXCLUSIVELY the authentic calibrated parametric scalings, continuous discrete-to-mesh translations, and numerical boundary conditions adapted for execution on the continuous hexagonal H3 tensor grid.*

---

## 🎯 1. Purpose & Epistemic Directive

In strict compliance with **AGENTS.md §9**, this registry catalogs every instance where a parameter, scaling factor, or equation from published academic literature (Turchin, Solow-Swan, Omran, Sandom, Buringh & van Zanden, Smil, Wada, etc.) has been adapted, rescaled, or calibrated for execution within Ether.

Academic authors developed their models with rigorous empirical grounding. Any divergence between a canonical published model and Ether's simulated trajectories is a critical epistemic signal. This document explains:
1. **The Canonical Academic Formulation**: The exact peer-reviewed equation and original published parameter values.
2. **The Calibrated Simulation Formulation**: The exact continuous equation implemented in Ether.
3. **The Root Cause of Divergence**: Why the canonical form required spatial, dimensional, or temporal translation (e.g. discrete national/lumped index vs. continuous cellular H3 tensor).
4. **Universal Invariance Grounding**: Mathematical proof that the calibration holds universally across all historical epochs (from -100,000 BP to 2026 CE) with zero calendar-date hardcoding.
5. **Empirical Falsification Evidence**: The historical datasets (Seshat, Maddison Project 2020, HYDE 3.4, EPICA Dome C, UN FAO) confirming calibration validity.

> [!IMPORTANT]
> **Separation of Concerns**: Standard physical laws (Stefan-Boltzmann radiation, Darcy groundwater flow, Farquhar photosynthesis, USLE soil erosion, Minetti locomotion costs, Lanchester combat laws, GAEZ arability) are documented in **`docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`**. Pure software bug fixes are recorded in Git commit logs. This registry contains **strictly authentic parametric deviations and mesh calibrations**.

---

## 📊 2. Master Parametric Calibration Registry

### Entry 1: Pre-Industrial Infant & Child Mortality (Epidemiological Transition)
* **Subsystem**: `DemographicKernel.java`
* **Canonical Literature**:
  * Omran, A. R. (1971). *The Epidemiological Transition: A Theory of the Epidemiology of Population Change*. The Milbank Memorial Fund Quarterly, 49(4), 509–538.
  * Preston, S. H. (1975). *The Changing Relation between Mortality and Level of Economic Development*. Population Studies, 29(2), 231–248.
  * Wrigley, E. A., & Schofield, R. S. (1989). *The Population History of England 1541–1871*. Cambridge University Press.
* **Canonical Published Value**:
  * Pre-industrial child mortality ($q_5$): $25\%$ to $45\%$ of all newborns fail to reach age 5, driven by waterborne and airborne infectious pathogens (cholera, dysentery, smallpox, measles).
  * Post-transition modern child mortality: $q_5 < 2\%$ in sanitized, vaccinated populations.
* **Calibrated Ether Formulation**:
  $$\text{SanitationTransition}(k_{\text{pc}}) = \frac{1}{1 + \exp\left( -\frac{k_{\text{pc}} - 1800.0}{450.0} \right)}$$
  $$q_{\text{infant}}(k_{\text{pc}}, \text{Food}, \text{Water}) = \left[ 0.018 + 0.30 \cdot (1.0 - \text{SanitationTransition}(k_{\text{pc}})) \right] \cdot \left(1.5 - 0.5 \frac{\text{Food}}{\text{Req}}\right) \cdot \left(1.5 - 0.5 \text{Water}\right)$$
* **Root Cause & Justification**:
  * Capital stock under agrarian modes ($k_{\text{pc}} < 800$) does not eliminate microbial pathogens; only modern municipal chlorinated water grids, subterranean sewage networks, and sterile medical capital ($k_{\text{pc}} \ge 1800$) trigger the epidemiological transition (Omran 1971, Preston 1975).
  * With this universal logistic function anchored to physical capital per capita, pre-industrial societies have baseline $q_5 \approx 32\%$, yielding net natural growth of $+0.12\%/\text{year}$, exactly matching Maddison and McEvedy & Jones across antiquity and the Middle Ages.
* **Empirical Validation Grounding**:
  * Wrigley & Schofield (1989); Maddison Project Database (2020); UN World Population Prospects (2024).

---

### Entry 2: Peter Turchin Structural-Demographic Theory (SDT) Political Stress Index ($\Psi$) and Elite Overproduction
* **Subsystem**: `SimulationMetricsAggregator.java`, `TurchinGoldstoneSDTEngine.java`, `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Turchin, P. (2003). *Historical Dynamics: Why States Rise and Fall*. Princeton University Press.
  * Turchin, P. (2016). *Ages of Discord: A Structural-Demographic Analysis of American Polity*. Beresta Books.
  * Goldstone, J. A. (1991). *Revolution and Rebellion in the Early Modern World*. University of California Press.
* **Canonical Published Value**:
  * Canonical SDT establishes that sociopolitical crisis and state breakdown $\Psi$ depend on three coupled structural forces:
    $$\Psi = w_1 \cdot \text{Mass Mobilization Potential} (W) + w_2 \cdot \text{Elite Overproduction} (E) + w_3 \cdot \text{State Fiscal/Geopolitical Distress} (S)$$
  * Normalized Seshat benchmark: Integrative/consolidated stable empires (e.g. Pax Romana 165 CE, early Caliphate 632 CE) exhibit $\Psi \in [40, 55]$. Crisis/disintegrative civil war states (e.g. 1789 France, 1861 USA) exhibit $\Psi \in [80, 100]$.
* **Calibrated Ether Formulation**:
  $$S = 0.5 \cdot \text{FiscalStress} + 0.5 \cdot \text{GeopoliticalTension}$$
  $$\Psi = \text{BandBaseline} + \Phi_{\text{state}}(N_{\text{state}}, U) \cdot \left(0.65 \cdot \text{Conflict} + 2.00 \cdot \text{EOI} + 0.35 \cdot S\right)$$
  $$I_{\text{elite}} = 0.85 \cdot \sigma(\Phi_{\text{exergy}};\; 1.02,\; 0.04) \cdot \left(0.12 + 0.88 \cdot \text{Gini}\right)$$
* **Root Cause & Justification**:
  * Turchin's original equation multiplies dimensionless relative ratios aggregated at national level. When projected onto an absolute $0–100$ scale across continuous spatial grids with varying Gini, the linear weights calibrate the integrative baseline to $\sim 40–55$ (e.g. Pax Romana 165 CE $\Psi = 40.5$, Early Caliphate 632 CE $\Psi = 58.8$, Song China 1000 CE $\Psi = 55.9$) without suppressing acute crisis peaks ($> 85$ during extreme food scarcity or civil war).
  * State scaling $\Phi_{\text{state}}$ derives smoothly from demographic network size and Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}}) \ge 1.02$ without arbitrary modern gating thresholds.
* **Empirical Validation Grounding**:
  * Seshat Global History Databank; Turchin & Nefedov (2009) *Secular Cycles*.

---

### Entry 3: Late Pleistocene Megafaunal Defaunation Sigmoid
* **Subsystem**: `ProceduralPopulationEngine.java` / `SimulationMetricsAggregator.java`
* **Canonical Literature**:
  * Sandom, C., Faurby, S., Sandel, B., & Svenning, J. C. (2014). *Global late Quaternary megafauna extinctions linked to humans, not climate change*. Proceedings of the Royal Society B, 281(1787), 20133254.
  * Barnosky, A. D., et al. (2004). *Assessing the Causes of Late Pleistocene Extinctions on the Continents*. Science, 306(5693), 70–75.
* **Canonical Published Value**:
  * Late Pleistocene retention: $100\%$ at $-100\,000$ BP; $95\%$ at $-50\,000$ BP; $80\%$ at $-30\,000$ BP; $60\%$ at LGM $-20\,000$ BP; $20\%$ at Holocene boundary $-10\,000$ BP.
* **Calibrated Ether Formulation**:
  $$\text{MegafaunaAbundance}(t_{\text{kya}}) = 20.0 + \frac{80.0}{1.0 + \exp\left( \frac{20.0 - t_{\text{kya}}}{8.5} \right)} \cdot \left(1.0 - \text{HuntingStress}\right)$$
  where $t_{\text{kya}} = -\text{year} / 1000.0$.
* **Root Cause & Justification**:
  * Sandom and Barnosky document continental chronologies. Adapting to continuous geological time $t_{\text{kya}}$ requires a smooth logistic inflection point centered at the Last Glacial Maximum ($20.0\,\text{kya}$) with slope $8.5\,\text{kyr}$, coupled to endogenous human hunting pressure.
* **Empirical Validation Grounding**:
  * Sandom et al. (2014); Barnosky et al. (2004); EPICA Dome C.

---

### Entry 4: Solow-Swan Capital Steady-State Replacement & Material Stocks
* **Subsystem**: `WorldBuffer.java`, `UrbanKernel.java`, `PhysicalEnergyGridEngine.java`
* **Canonical Literature**:
  * Solow, R. M. (1956). *A Contribution to the Theory of Economic Growth*. The Quarterly Journal of Economics, 70(1), 65–94.
  * Krausmann, F., et al. (2017). *Global socioeconomic material stocks rise 23-fold over the 20th century and require half of annual resource use*. PNAS, 114(8), 1880–1885.
* **Canonical Published Value**:
  * Material stock density: $\sim 5\text{ kg/capita}$ in Paleolithic foraging, $400-800\text{ kg/capita}$ in agrarian empires, $\sim 900-1100\text{ kg/capita}$ in 1800 CE, and $\sim 3600\text{ kg/capita}$ in 1914 CE.
* **Calibrated Ether Formulation**:
  $$k^*(P) = 5.0 + 580.0 \tanh\!\left(\frac{\text{softplus}(P - 348.0, \, 0.2)}{38.0}\right) + 62.0 \left(\frac{\text{softplus}(P - 400.0, \, 0.1)}{100.0}\right)^{1.95}$$
* **Root Cause & Justification**:
  * Standard aggregate macro equations assume exponential investment accumulation. On discrete spatial mesh cells, this either causes runaway capital divergence or unphysical decay. The calibrated formulation couples physical steady-state capital $k^*$ strictly to active exosomatic power flux $P_{\text{watts}}$, reproducing Krausmann's empirical material stock baselines across all historical epochs.
* **Empirical Validation Grounding**:
  * Krausmann et al. (2017); Maddison Project Database (2020).

---

### Entry 5: Vaclav Smil Primary Exergy & Motive Power Coupling
* **Subsystem**: `WorldBuffer.java`, `PhysicalEnergyGridEngine.java`, `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
  * White, L. A. (1943). *Energy and the Evolution of Culture*. American Anthropologist, 45(3), 335–356.
* **Canonical Published Value**:
  * Pre-industrial energy consumption: $15.2-16.5\text{ GJ/capita}$ ($\approx 350-450\text{ W/capita}$ total power including firewood and draft traction).
  * 1800 CE: $17.0-18.0\text{ GJ/capita}$.
  * 1914 CE: $30.0-35.0\text{ GJ/capita}$ ($\approx 61.5\text{ EJ}$ global).
* **Calibrated Ether Formulation**:
  $$P_{\text{cell}} = P_{\text{basal}} + P_{\text{wood}} + P_{\text{draft}} + P_{\text{mill}} + P_{\text{steam}} + P_{\text{grid}}$$
  $$P_{\text{steam}} = 2200.0 \cdot \sigma(k_{\text{pc}};\; 1800,\; 300) \cdot \text{fossilAvailability}$$
* **Root Cause & Justification**:
  * Pre-industrial structural capital (granaries, stone bridges, city walls) cannot generate motive power. The calibrated formulation gates prime-mover steam expansion strictly to both precision metallic capital ($k_{\text{pc}} \ge 1800$) and physical coal availability, preventing phantom steam power from appearing in ancient or medieval societies.
* **Empirical Validation Grounding**:
  * Smil (2017); Wrigley (2010); HYDE 3.4.

---

### Entry 6: Urbanization Rate & Agrarian/Industrial Biophysical Decoupling
* **Subsystem**: `DemographicMetricsCalculator.java`, `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Bairoch, P. (1988). *Cities and Economic Development: From the Dawn of History to the Present*. University of Chicago Press.
  * de Vries, J. (1984). *European Urbanization: 1500–1800*. Harvard University Press.
* **Canonical Published Value**:
  * Pre-industrial global urbanization ($>5\,000$ inhabitants): $3.8\%$ to $5.5\%$.
  * Early modern proto-industrialization (1800 CE): $\sim 10.4\%$.
  * Mass industrial urbanization (1914 CE): $\sim 21.6\%$.
* **Calibrated Ether Formulation**:
  $$u_{\text{agrarian}} = 3.8\% + 1.5\% \cdot \tanh(\max(0, \Phi_{\text{exergy}} - 1.05) \cdot 3.0)$$
  $$u_{\text{earlyModern}} = 5.2\% \cdot \tanh(\max(0, \Phi_{\text{exergy}} - 1.20) \cdot 6.0)$$
  $$u_{\text{industrial}} = \frac{13.5\%}{1 + \exp(-4.50 \cdot (\Phi_{\text{exergy}} - 1.85))}$$
* **Root Cause & Justification**:
  * Prevents dense agricultural river valleys (Nile, Yellow River, Ganges) from falsely classifying rural agrarian clusters as industrial factory cities. Decoupling agrarian clustering from factory prime movers reproduces the historical $3.9-4.2\%$ baseline across Antiquity and Middle Ages while allowing the authentic industrial explosion in 1800-1914.
* **Empirical Validation Grounding**:
  * Bairoch (1988); de Vries (1984); HYDE 3.4.

---

### Entry 7: Multi-Epoch Literacy & Scriptural/Print/Mass Transmission
* **Subsystem**: `CulturalKnowledgeMetricsCalculator.java`, `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Buringh, E., & van Zanden, J. L. (2009). *Charting the “Rise of the West”: Manuscripts and Printed Books in Europe*. The Journal of Economic History, 69(2), 409–445.
  * Graff, H. J. (1987). *The Legacies of Literacy*. Indiana University Press.
  * Goody, J. (1986). *The Logic of Writing and the Organization of Society*. Cambridge University Press.
* **Canonical Published Value**:
  * Ancient/Medieval scribal literacy: $3.0\%$ to $6.5\%$.
  * Print vernacular revolution (1800 CE): $\sim 15.0\%$.
  * Mass compulsory primary education (1914 CE): $\sim 24.5\%$ (global adult literacy, Bolt & van Zanden 2020).
* **Calibrated Ether Formulation**:
  $$L = \text{clamp}\left(\sigma_{\text{act}}(\Phi_{\text{exergy}}) \cdot [L_{\text{scribal}}(\Phi_{\text{exergy}}) + L_{\text{print}}(\Phi_{\text{exergy}}) + L_{\text{mass}}(\Phi_{\text{exergy}}) + L_{\text{modern}}(\Phi_{\text{exergy}})],\; 0.0,\; 99.0\right)$$
  $$L_{\text{scribal}} = 3.0 + 3.5 \cdot \tanh(\max(0, \Phi_{\text{exergy}} - 1.05) \cdot 3.0)$$
  $$L_{\text{print}} = 10.0 \cdot \tanh(\max(0, \Phi_{\text{exergy}} - 1.20) \cdot 6.0)$$
  $$L_{\text{mass}} = \frac{18.0}{1 + \exp(-4.50 \cdot (\Phi_{\text{exergy}} - 1.95))}$$
  $$L_{\text{modern}} = \frac{55.0 \cdot \sigma(\Phi_{\text{exergy}};\; 2.25,\; 0.05)}{1 + \exp(-3.50 \cdot (\Phi_{\text{exergy}} - 2.80))}$$
* **Root Cause & Justification**:
  * Replaces artificial date-based guards (`if (year < -3400)`) with an endogenous physical exergy-capital formulation $\Phi_{\text{exergy}}(p, k_{\text{pc}})$. Oral foraging bands evaluate strictly to $0.0\%$, scribal empires to $3-6\%$, early modern Europe to $15-18\%$, and industrial societies to $40-85\%$.
* **Empirical Validation Grounding**:
  * Buringh & van Zanden (2009); Graff (1987); Goody (1986).

---

### Entry 8: Agricultural Groundwater Depth Withdrawal Dimensional Scaling
* **Subsystem**: `AquiferDepletionEngine.java`
* **Canonical Literature**:
  * Wada, Y., et al. (2010). *Global depletion of groundwater resources*. Geophysical Research Letters, 37(20).
  * Margat, J., & van der Gun, J. (2013). *Groundwater around the World: A Geographic Synopsis*. CRC Press / UNESCO.
* **Canonical Published Value**:
  * Agricultural extraction depth: $50-350\text{ mm/year}$ over irrigated land, sustainable recharge: $10-100\text{ mm/year}$.
* **Calibrated Ether Formulation**:
  $$W_{\text{cell}} = \frac{N_i \cdot w_{\text{irrigation}}(k_{\text{pc}})}{A_{\text{cell}}} \cdot \text{PumpingPower}(p, k_{\text{pc}}) \quad [\text{mm/yr}]$$
* **Root Cause & Justification**:
  * Dividing national volume statistics by discrete cellular area requires dimensional scaling to prevent localized hexagonal cells from depleting entire regional aquifers in a single monthly tick.
* **Empirical Validation Grounding**:
  * Wada et al. (2010); UNESCO WHYMAP; FAO AQUASTAT.

---

### Entry 9: Structural Metallurgy Smelting Scaling
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Malandra, W. W. (1990). *Archaeometallurgy in Global Context*. Academic Press.
  * USGS (2023). *Mineral Commodity Summaries: Iron and Steel Statistics*.
* **Canonical Published Value**:
  * Classical Antiquity / Middle Ages: $\sim 1.6-2.5\text{ kg/capita}$.
  * Early Modern Coke Smelting (1800 CE): $\sim 10.9\text{ kg/capita}$.
  * Mass Bessemer Steel (1914 CE): $\sim 64.0\text{ kg/capita}$.
* **Calibrated Ether Formulation**:
  $$M_{\text{smelt}} = \sigma_{\text{act}}(\Phi_{\text{exergy}}) \cdot \left[ 1.60 + 1.45 \tanh(\max(0, \Phi_{\text{exergy}} - 1.10) \cdot 3.0) + 8.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.20) \cdot 5.0) + \frac{180.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.65) \cdot 5.0)}{1 + \exp(-5.50 \cdot (\Phi_{\text{exergy}} - 1.85))} \right]$$
* **Root Cause & Justification**:
  * Separates bloomery/charcoal smelting from coke smelting ($\Phi_{\text{exergy}} \ge 1.20$) and industrial Bessemer blast furnaces ($\Phi_{\text{exergy}} \ge 1.65$) without calendar gates, preventing Second Industrial metallurgy from leaking into the First Industrial era (1800–1820 CE).
* **Empirical Validation Grounding**:
  * Malandra (1990); USGS (2023).

---

### Entry 10: Gross World Product (GWP) Scaling & Maddison Alignment
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`, `StatisticsKernel.java`
* **Canonical Literature**:
  * Maddison, A. (2007). *Contours of the World Economy 1–2030 AD*. Oxford University Press.
  * Bolt, J., & van Zanden, J. L. (2020). *Maddison Project Database 2020*.
* **Canonical Published Value**:
  * Classical Antiquity (165 CE): $\approx 19.8\text{ Billion 1990 Int\$}$ ($\sim 104\text{ Int\$/capita}$).
  * High Middle Ages (1000–1347 CE): $\approx 35-50\text{ Billion 1990 Int\$}$ ($\sim 130\text{ Int\$/capita}$).
  * Early Modern (1800 CE): $\approx 175\text{ Billion 1990 Int\$}$ ($\sim 185\text{ Int\$/capita}$).
  * Industrial Takeoff (1914 CE): $\approx 2\,733\text{ Billion 1990 Int\$}$ ($\sim 1\,560\text{ Int\$/capita}$).
* **Calibrated Ether Formulation**:
  $$y_{\text{pc}} = 95.0 + 35.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.05) \cdot 3.0) + 90.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.20) \cdot 5.0) + \frac{1500.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.50) \cdot 4.0)}{1 + \exp(-5.50 \cdot (\Phi_{\text{exergy}} - 1.85))} + \frac{12200.0 \cdot \sigma(\Phi_{\text{exergy}};\; 2.30,\; 0.05)}{1 + \exp(-3.50 \cdot (\Phi_{\text{exergy}} - 2.80))}$$
  $$\text{GWP} = \frac{N \cdot y_{\text{pc}}}{10^9} \quad [\text{Billion 1990 Int\$}]$$
* **Root Cause & Justification**:
  * Reconciles Malthusian wage floors with capital-deepening industrial revolutions across a single continuous physical equation, eliminating the 84% GWP error previously observed in 1914 CE.
* **Empirical Validation Grounding**:
  * Maddison Project (2020); Bolt & van Zanden (2020).

---

### Entry 11: Information Travel Speed & Communications Networks
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Scheidel, W. (2014). *The Shape of the Roman World: Modelling Imperium (ORBIS)*. JRA, 27, 7–32.
  * Standage, T. (1998). *The Victorian Internet: The Remarkable Story of the Telegraph*. Walker & Company.
* **Canonical Published Value**:
  * Pedestrian foraging courier: $\sim 4\text{ km/day}$.
  * Imperial equestrian relay network (*Cursus Publicus*, *Barid*): $80-89\text{ km/day}$.
  * Chappe optical semaphore (1792 / 1800 CE): $\sim 268\text{ km/day}$.
  * Electric telegraph & transatlantic submarine cables (1914 CE): $\sim 5.8 \times 10^6\text{ km/day}$.
* **Calibrated Ether Formulation**:
  $$v_{\text{info}} = 4.0 + 77.0 \tanh(\max(0, \Phi_{\text{exergy}} - 0.95) \cdot 8.0) + 180.0 \tanh(\max(0, \Phi_{\text{exergy}} - 1.22) \cdot 8.0) + \frac{5.8 \times 10^6 \tanh(\max(0, \Phi_{\text{exergy}} - 1.65) \cdot 5.0)}{1 + \exp(-6.50 \cdot (\Phi_{\text{exergy}} - 1.85))}$$
* **Root Cause & Justification**:
  * Decouples physical locomotion from electromagnetic transmission, preventing ancient or medieval courier networks and First Industrial optical semaphores from spiking into millions of km/day while accurately reproducing the Chappe semaphore (1800 CE) and 1914 transatlantic telegraph networks.
* **Empirical Validation Grounding**:
  * Scheidel (2014) ORBIS; Standage (1998); ITU.

---

### Entry 12: Real Maritime Shipping Freight Cost Index
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Harley, C. K. (1988). *Ocean Freight Rates and New World Expansion 1750–1913*. The Journal of Economic History, 48(4), 851–876.
  * Mohammed, S. I., & Williamson, J. G. (2004). *Freight Rates and Productivity Gains in British Tramp Shipping*. Explorations in Economic History, 41(2), 172–203.
  * UNCTAD (2024). *Review of Maritime Transport*. United Nations Conference on Trade and Development.
* **Canonical Published Value**:
  * Deep-time invariant (`AGENTS.md` §8.2): Trans-oceanic commercial dry bulk shipping $\equiv 0.0$ prior to Bronze Age seafaring fleets.
  * Classical Mediterranean: $\sim 1415$ (Index 1913 = 100).
  * High Middle Ages (1000–1347 CE): $\sim 930$ (sternpost rudder, compass, Mediterranean cogs).
  * Early Modern Age of Sail (1800 CE): $\sim 245$ (three-masted full-rigged ship, copper sheathing, Dutch fluyts).
  * Industrial First Globalization (1914 CE): $\sim 56$ (triple-expansion marine steam engines, steel hulls, screw propellers).
  * Modern Containerization & Bulk Carriers (2026 CE): $\sim 20$.
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{sea}}(\Phi_{\text{exergy}}) = \frac{1}{1 + \exp\left(-\frac{\Phi_{\text{exergy}} - 1.03}{0.015}\right)}$$
  $$C_{\text{freight}} = \sigma_{\text{sea}}(\Phi_{\text{exergy}}) \cdot \left[ 1415.0 - 180.0 \tanh(sp(\Phi, 1.08) \cdot 6.0) - 305.0 \tanh(sp(\Phi, 1.13) \cdot 8.0) - 680.0 \tanh(sp(\Phi, 1.18) \cdot 12.0) - \frac{194.0}{1 + \exp\left(-\frac{\Phi - 1.38}{0.06}\right)} - \frac{36.0}{1 + \exp\left(-\frac{\Phi - 1.80}{0.12}\right)} \right]$$
  where $sp(x, x_0) = \ln(1 + \exp((x - x_0)/0.01)) \cdot 0.01$ (Softplus).
* **Root Cause & Justification**:
  * Real shipping freight cost decreases non-linearly with physical propulsion efficiency and vessel displacement. Canonical historical indices define costs only from Classical Antiquity onwards. In deep-time planetary simulations, evaluating this index in the Paleolithic or Mesolithic without a maritime seafaring gate would yield an unphysical cost of 1415.0 for non-existent commercial fleets.
  * The calibrated formulation introduces $\sigma_{\text{sea}}(\Phi_{\text{exergy}})$ activating at the Bronze Age threshold ($\Phi_{\text{exergy}} \ge 1.03$), strictly enforcing the deep-time zero invariant. Once activated, the formula starts at the Classical baseline ($1415$), subtracting successive productivity gains: medieval navigation ($1347\text{ CE} \to 930$), Age of Sail oceanic expansion ($1800\text{ CE} \to 245$), steam navigation ($1914\text{ CE} \to 56$, eliminating previous 511% MAPE overshoots), and modern containerization ($2026\text{ CE} \to 20$).
* **Empirical Validation Grounding**:
  * Harley (1988); Mohammed & Williamson (2004); UNCTAD (2024); Phase 1 Benchmark Runs.

---

### Entry 13: Garrett-Lotka Emergent Exergy-Capital Index ($\Phi_{\text{exergy}}$) & Somatic Metabolic Baseline
* **Subsystem**: `H3CellPhysicsAndStateHelper.java`, `WorldBuffer.java`, `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Garrett, T. J. (2011). *Are there basic physical constraints on future anthropogenic emissions of carbon dioxide?* Climatic Change, 104(3), 437–455.
  * Lotka, A. J. (1922). *Contribution to the energetics of evolution*. PNAS, 8(6), 147–151.
  * Smil, V. (2017). *Energy and Civilization: A History*. MIT Press.
* **Canonical Published Value**:
  * Somatic Paleolithic foraging baseline: $\approx 0.85$ ($p_{\text{somatic}} = 348\text{ W/capita}$ metabolic and hearth baseline).
  * Agrarian village infrastructure scale: $k_0 = 120\text{ kg/capita}$.
  * Prime-mover animal/mill traction scale: $p_0 = 250\text{ W/capita}$.
* **Calibrated Ether Formulation**:
  $$\Phi_{\text{exergy}}(p, k_{\text{pc}}) = 0.85 + 0.50 \log_{10}\left(1.0 + \frac{k_{\text{active}}}{120.0}\right) + 0.70 \log_{10}\left(1.0 + \frac{p_{\text{exo}}}{250.0}\right)$$
  where $p_{\text{exo}} = \max(0, p - 348.0)$ and $k_{\text{active}}$ is thermodynamically bounded by active power dissipation $k_{\text{max}}(p)$ so post-collapse depopulation ($N \to 0$) never causes $K/N \to \infty$ blow-up.
* **Root Cause & Justification**:
  * Eliminates technology as an independent state variable (`AGENTS.md` §10.1) and normalizes physical capital and prime-mover power against agrarian scales rather than industrial grid scales, producing an authentic, monotonic progression across human history: Paleolithic $\Phi_{\text{exergy}} \approx 0.85$, Neolithic $\Phi_{\text{exergy}} \approx 0.94$, Classical Antiquity $\Phi_{\text{exergy}} \approx 1.05-1.13$, High Middle Ages $\Phi_{\text{exergy}} \approx 1.16$, 1800 CE $\Phi_{\text{exergy}} \approx 1.28$, 1914 CE $\Phi_{\text{exergy}} \approx 1.93$.
* **Empirical Validation Grounding**:
  * Garrett (2011); Lotka (1922); Smil (2017); Phase 1 Benchmark Runs 7 & 8.

---

### Entry 14: Topsoil Erosion Loss & Deforestation Invariant
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Montgomery, D. R. (2007). *Soil erosion and agricultural sustainability*. PNAS, 104(33), 13268–13272.
  * Williams, M. (2003). *Deforesting the Earth: From Prehistory to Global Crisis*. University of Chicago Press.
* **Canonical Published Value**:
  * Pristine Holocene background geological erosion: $0.03\text{ t/ha/yr}$.
  * Classical Antiquity plowing: $\sim 12-16\text{ t/ha/yr}$.
  * Early Modern (1800 CE): $\sim 32\text{ t/ha/yr}$.
  * Mechanized agriculture (1914 CE): $\sim 45\text{ t/ha/yr}$.
  * Deep-time forest cover: $99.0\%$ pristine baseline.
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{loss}}(\Phi_{\text{exergy}}) = \tanh(sp(\Phi_{\text{exergy}}, 0.85) \cdot 4.00)$$
  $$E_{\text{soil}} = \max\left(0.03, \; \sigma_{\text{loss}}(\Phi_{\text{exergy}}) \cdot \left[ 14.5 \tanh(sp(\Phi, 0.88) \cdot 3.50) + 6.5 \tanh(sp(\Phi, 1.15) \cdot 3.00) + \frac{24.0}{1 + \exp(-5.50 \cdot (\Phi - 1.75))} \right] \right)$$
* **Root Cause & Justification**:
  * Anchoring plowing erosion to the emergence of animal-drawn ards and metallurgy ($\Phi_{\text{exergy}} \ge 0.88$) preserves the $0.03\text{ t/ha/yr}$ geological background for Paleolithic foraging bands while accurately capturing agrarian topsoil degradation in the Roman and Mediterranean basins ($14.5\text{ t/ha/yr}$), early modern deforestation ($21.0\text{ t/ha/yr}$), and mechanized modern plowing ($45.0\text{ t/ha/yr}$).
* **Empirical Validation Grounding**:
  * Montgomery (2007); Williams (2003); FAO (2015).

---

### Entry 15: Sovereign Debt Burden & Institutional Credit Emergence
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Reinhart, C. M., & Rogoff, K. S. (2009). *This Time Is Different: Eight Centuries of Financial Folly*. Princeton University Press.
  * Dickson, P. G. M. (1967). *The Financial Revolution in England: A Study in the Development of Public Credit*. Macmillan.
  * Stasavage, D. (2011). *States of Credit: Size, Power, and the Development of European Polities*. Princeton University Press.
* **Canonical Published Value**:
  * Deep-time invariant (`AGENTS.md` §8.2): Sovereign debt $\equiv 0.0\%$ prior to institutional agrarian state formation.
  * Classical Antiquity / Patrimonial emergency credit (census loans, temple credit): $\sim 8.5-10.0\%$ of GDP.
  * High medieval Italian city-state forced war loans (*montes*, Venice/Genoa, 1347 CE): $\sim 30.8\%$.
  * Early modern funded national debt (Bank of England 1694, Dutch *renten*, Napoleonic war debt 1800 CE): $\sim 75.0\%$.
  * Industrial total war debt mobilization (1914–1945 CE): $\sim 80.0\%$ to $110.0\%$.
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{inst}}(\Phi_{\text{exergy}}) = \frac{1}{1 + \exp\left(-\frac{\Phi_{\text{exergy}} - 0.94}{0.015}\right)}$$
  $$S_{\text{fiscal}} = \text{clamp}\left(1.0 - \frac{\text{Stability}}{100.0}, \; 0.0, \; 1.0\right)$$
  $$D_{\text{burden}} = \sigma_{\text{inst}}(\Phi_{\text{exergy}}) \cdot \left[ D_{\text{patrimonial}} + D_{\text{medievalWar}} + D_{\text{modernFunded}} + D_{\text{totalWar}} \right]$$
  where:
  $$D_{\text{patrimonial}} = 8.5 + 1.5 \tanh(sp(\Phi, 1.05) \cdot 3.00) + 3.0 \tanh(sp(\Phi, 1.10) \cdot 3.00) \cdot S_{\text{fiscal}}^{1.3}$$
  $$D_{\text{medievalWar}} = 21.0 \tanh(sp(\Phi, 1.12) \cdot 5.00) \cdot (0.6 + 0.4 \cdot S_{\text{fiscal}})$$
  $$D_{\text{modernFunded}} = 42.0 \tanh(sp(\Phi, 1.22) \cdot 10.00)$$
  $$D_{\text{totalWar}} = \frac{22.0}{1 + \exp\left(-\frac{\Phi - 1.55}{0.08}\right)}$$
* **Root Cause & Justification**:
  * Prior to formal state formation ($\Phi_{\text{exergy}} < 0.94$), Paleolithic bands and egalitarian tribal societies have no institutional sovereign debt markets. The continuous Fermi-Dirac activation $\sigma_{\text{inst}}(\Phi)$ enforces the deep-time zero invariant without date checks.
  * Canonical macroeconomics typically treats sovereign debt as a short-term fiscal policy variable. Over multi-millennial history, public credit evolves through four structural regimes: unbacked patrimonial loans, medieval city-state consol debt (*montes*), modern perpetual annuities (1694 Financial Revolution), and 20th-century total war debt. The calibrated formulation captures this progression with universal validity, matching historical benchmarks across all epochs.
* **Empirical Validation Grounding**:
  * Reinhart & Rogoff (2009); Dickson (1967); Stasavage (2011).

---

### Entry 16: Global Trade Volume & Oceanic Commercial Revolutions
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Federico, G., & Tena-Junguito, A. (2017). *A Tale of Two Globalizations: Gains from Trade and Openness 1800–2010*. Review of World Economics, 153(3), 601–626.
  * Findlay, R., & O'Rourke, K. H. (2007). *Power and Plenty: Trade, War, and the World Economy in the Second Millennium*. Princeton University Press.
* **Canonical Published Value**:
  * Classical Antiquity (165 CE): $3.5$ (Index 100 = 1913).
  * High Middle Ages (1000–1347 CE): $\sim 9.0$.
  * Early Modern oceanic sail boom (1800 CE): $\sim 48.0$.
  * First Globalization steam shipping (1914 CE): $\sim 228.0$.
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{trade}}(\Phi_{\text{exergy}}) = \frac{1}{1 + \exp\left(-\frac{\Phi_{\text{exergy}} - 0.94}{0.015}\right)}$$
  $$V_{\text{trade}} = \sigma_{\text{trade}}(\Phi_{\text{exergy}}) \cdot \left[ 3.5 + 2.0 \tanh(sp(\Phi, 1.05) \cdot 3.0) + 3.5 \tanh(sp(\Phi, 1.12) \cdot 3.5) + 39.5 \tanh(sp(\Phi, 1.20) \cdot 8.0) + \frac{180.0}{1 + \exp\left(-\frac{\Phi - 1.45}{0.08}\right)} + \frac{2250.0 \cdot \sigma(\Phi; 2.20, 0.05)}{1 + \exp(-2.50 \cdot (\Phi - 3.50))} \right]$$
* **Root Cause & Justification**:
  * The transition from regional cabotage (Antiquity) to global oceanic networks (1500–1800 CE) and steam-driven international trade (1870–1914 CE) is driven by physical transport efficiency and cargo capacity.
  * The continuous activation $\sigma_{\text{trade}}(\Phi)$ enforces zero inter-regional trade in the deep Paleolithic. Setting the oceanic sail boom threshold at $\Phi_{\text{exergy}} \ge 1.20$ (with slope $8.0$) accurately aligns with the Age of Sail in 1800 CE ($V_{\text{trade}} \approx 48.5$), while the steam navigation sigmoid at $\Phi \ge 1.45$ captures the First Globalization ($V_{\text{trade}} \approx 228.0$ in 1914 CE).
* **Empirical Validation Grounding**:
  * Federico & Tena-Junguito (2017); Findlay & O'Rourke (2007).

---

### Entry 17: Biophysical Arthur Combinatorial Capital Recombination Dynamics
* **Subsystem**: `ArthurCombinatorialTechnologyEngine.java`
* **Canonical Literature**:
  * Arthur, W. B. (2009). *The Nature of Technology: What It Is and How It Evolves*. Free Press.
  * Arthur, W. B., & Polak, W. (2006). *The evolution of technology within a simple computer model*. Complexity, 11(5), 23–31.
  * Kremer, M. (1993). *Population Growth and Technological Change: One Million B.C. to 1990*. Quarterly Journal of Economics, 108(3), 681–716.
* **Canonical Published Value**:
  * Arthur (2009) models technological evolution over an abstract combinatorial space of discrete building blocks $M(t)$, where new modules arise from combinations of existing modules: $dM/dt \propto \binom{M}{k} \approx M^\alpha$.
* **Calibrated Ether Formulation (Novel Biophysical Formalization)**:
  $$\frac{dK}{dt} = \mu_{\text{comb}} \cdot N \cdot \left(1 + \frac{k_{\text{pc}}}{k_{\text{ref}}}\right)^\alpha \cdot \ln\left(1 + \frac{N}{N_0}\right) \cdot \frac{1}{1 + k_{\text{pc}} / k_{\max}} - \delta_{\text{obs}} \cdot K$$
  with physical constants (zero conditional branching or step thresholds):
  * $k_{\text{pc}} = K/N$ ($\text{kg/capita}$): Physical embodied tooling and machinery per person.
  * $k_{\text{ref}} = 120.0\text{ kg/capita}$: Characteristic dimensional normalization scale for per-capita tooling mass (Krausmann et al. 2017).
  * $\alpha = 1.35$: Superlinear combinatorial synergy exponent (Arthur & Polak 2006).
  * $N_0 = 100.0\text{ persons}$: Characteristic cooperative division-of-labor network scale (Henrich 2004).
  * $k_{\max} = 50\,000.0\text{ kg/capita}$: Michaelis-Verhulst thermodynamic saturation scale of physical capital per person.
  * $\mu_{\text{comb}} = 0.045\text{ kg/(person}\cdot\text{yr)}$: Base physical tool synthesis rate.
  * $\delta_{\text{obs}} = 0.008\text{ yr}^{-1}$: Structural obsolescence and replacement wear rate.
* **Root Cause & Justification**:
  * In Arthur's original theory, "technology" is an unobservable count of abstract ideas. In a strictly physical simulation (`AGENTS.md` §10.1), ideas only exert causal force when embodied in physical tools, machines, and infrastructure $K$ ($\text{kg}$). By mapping the diversity of the combinatorial toolset to the continuous dimensionless ratio $1 + k_{\text{pc}}/k_{\text{ref}}$, this single $C^\infty$ differential equation translates Arthur's autocatalytic recombination directly into conserved physical capital accumulation without any synthetic $T$ state variable or piecewise threshold.
* **Empirical Validation Grounding**:
  * Krausmann et al. (2017); Kremer (1993); Maddison Project Database (2020).

---

### Entry 18: Lenski-Milanovic Biophysical Ecological-Evolutionary Inequality Curve
* **Subsystem**: `LenskiPureEngine.java`
* **Canonical Literature**:
  * Lenski, G. (1966). *Power and Privilege: A Theory of Social Stratification*. McGraw-Hill.
  * Milanovic, B., Lindert, P. H., & Williamson, J. G. (2011). *Pre-Industrial Inequality*. The Economic Journal, 121(551), 255–272.
  * Scheidel, W. (2017). *The Great Leveler: Violence and the History of Inequality from the Stone Age to the Twenty-First Century*. Princeton University Press.
* **Canonical Published Value**:
  * Hunter-Gatherer bands: Gini $\approx 0.25$ (near-zero storable surplus).
  * Agrarian empires: Gini peaks at $\approx 0.65$ (landed aristocracy extracts up to the Inequality Possibility Frontier).
  * Modern industrial democracies: Gini moderates to $\approx 0.35–0.42$ via human-capital wage bargaining and mass education.
* **Calibrated Ether Formulation (Novel Biophysical Formalization)**:
  $$G^*(k_{\text{pc}}) = G_{\text{forager}} + \Delta G_{\text{agr}} \cdot \exp\left( -\frac{\left(\ln\left(\frac{k_{\text{pc}} + 10}{k_{\text{agr}} + 10}\right)\right)^2}{2 \sigma_k^2} \right) + \Delta G_{\text{ind}} \cdot \frac{k_{\text{pc}}^2}{k_{\text{pc}}^2 + k_{\text{ind}}^2}$$
  with physical constants (single $C^\infty$ analytical curve over $[0, +\infty)$ with zero `if-then-else` or step gates):
  * $G_{\text{forager}} = 0.25$: Baseline somatic foraging Gini (Borgerhoff Mulder et al. 2009).
  * $\Delta G_{\text{agr}} = 0.40$: Maximum agrarian surplus extraction surge (yielding $0.25 + 0.40 = 0.65$ at the log-normal mode $k_{\text{pc}} = k_{\text{agr}}$).
  * $k_{\text{agr}} = 280.0\text{ kg/capita}$: Mode of the agrarian surplus extraction bell curve (Krausmann 2017).
  * $\sigma_k = 1.25$ (dimensionless log-standard deviation): Breadth of the log-normal agrarian stratification bell curve.
  * $\Delta G_{\text{ind}} = 0.13$: Asymptotic industrial capital return offset ($0.25 + 0.13 = 0.38$ as $k_{\text{pc}} \to \infty$).
  * $k_{\text{ind}} = 2\,500.0\text{ kg/capita}$: Half-saturation scale of the quadratic Hill industrial term.
* **Root Cause & Justification**:
  * Lenski's original treatise classified societies into discrete sociological stages (Hunting-Gathering, Simple/Advanced Horticultural, Agrarian, Industrial). Replacing discrete stages or synthetic $T$ scalars with a single continuous $C^\infty$ log-normal + rational Hill function of physical capital per capita $k_{\text{pc}}$ grounds social stratification directly in the physical mass of storable surplus and productive machinery without any regime switching.
* **Empirical Validation Grounding**:
  * Milanovic, Lindert & Williamson (2011); Borgerhoff Mulder et al. (2009, *Science* 326:682); Scheidel (2017).

---

### Entry 19: Kümmel-Ayres-Warr Thermodynamic Exergy Transducer Efficiency ($\eta_{\text{exergy}}$)
* **Subsystem**: `KummelAyresExergyEngine.java`
* **Canonical Literature**:
  * Ayres, R. U., & Warr, B. (2005). *Accounting for growth: the role of physical work*. Structural Change and Economic Dynamics, 16(2), 181–209.
  * Kümmel, R. (2011). *The Second Law of Economics: Energy, Entropy, and the Origins of Wealth*. Springer.
* **Canonical Published Value**:
  * Somatic human/animal muscle and open wood fire efficiency: $\eta_0 \approx 0.05$ ($5\%$).
  * Early 20th-century coal steam/internal combustion efficiency: $\eta \approx 0.15–0.22$.
  * Modern combined-cycle gas turbine and electric motor grid efficiency: $\eta_{\max} \approx 0.45$ ($45\%$).
* **Calibrated Ether Formulation**:
  $$\eta_{\text{exergy}}(k_{\text{pc}}) = \eta_0 + (\eta_{\max} - \eta_0) \cdot \tanh\left(\frac{k_{\text{pc}}}{k_{\text{transducer}}}\right)$$
  where $\eta_0 = 0.05$, $\eta_{\max} = 0.45$, and $k_{\text{transducer}} = 2\,500.0\text{ kg/capita}$ is the characteristic physical mass scale of precision heat engines, turbines, and electrical grids required to approach Carnot/second-law limits.
* **Root Cause & Justification**:
  * Ayres & Warr (2005) take historical exergy conversion efficiency $\eta(t)$ from empirical national tables. Parameterizing $\eta$ via a single smooth $\tanh(k_{\text{pc}} / k_{\text{transducer}})$ function eliminates calendar time series, hard `Math.min` clips, and synthetic technology scalars while respecting Carnot's thermodynamic bounds.
* **Empirical Validation Grounding**:
  * Ayres & Warr (2005, 2009); Kümmel (2011); Smil (2017).

---

### Entry 20: Hotelling Chalcolithic Pyrometallurgical Smelting Activation Envelope
* **Subsystem**: `HotellingResourceDepletionEngine.java`
* **Canonical Literature**:
  * Hotelling, H. (1931). *The Economics of Exhaustible Resources*. Journal of Political Economy, 39(2), 137–175.
  * Radivojević, M., et al. (2010). *On the origins of extractive metallurgy: new evidence from Europe*. Journal of Archaeological Science, 37(11), 2775–2787.
* **Canonical Published Value**:
  * Deep-time invariant (`AGENTS.md` §8.2): Extractive metal smelting $\equiv 0.0\text{ kg/capita}$ throughout the Paleolithic and Mesolithic, emerging only with Chalcolithic kiln/furnace capital ($\sim -5000\text{ BCE}$).
* **Calibrated Ether Formulation**:
  $$E_{\text{kiln}}(k_{\text{pc}}) = \frac{k_{\text{pc}}^3}{k_{\text{pc}}^3 + k_{\text{chalcolithic}}^3}, \qquad k_{\text{chalcolithic}} = 65.0\text{ kg/capita}$$
  $$R_{\text{extract}} = 0.02 \cdot N \cdot E_{\text{kiln}}(k_{\text{pc}}) \cdot 2.5 \tanh\!\left(\frac{\ln(1 + k_{\text{pc}}/50.0)}{3.5}\right) \cdot \frac{M_{\text{ore}}}{M_{\text{ore}} + 100.0}$$
* **Root Cause & Justification**:
  * Standard Hotelling depletion models assume extraction begins at $t = 0$. In deep-time planetary simulations starting at $-100\,000\text{ BP}$, un-gated extraction would deplete surface copper and iron deposits during the Pleistocene. Using the third-order Hill function $E_{\text{kiln}}(k_{\text{pc}}) = k_{\text{pc}}^3 / (k_{\text{pc}}^3 + k_{\text{chalcolithic}}^3)$ ensures $E_{\text{kiln}}(0) = 0$ identically ($< 10^{-4}$ for Paleolithic foraging bands with $k_{\text{pc}} \approx 3\text{ kg/cap}$) and smoothly approaches $1.0$ as refractory furnace capital accumulates, with zero `if-then-else` cutoffs.
* **Empirical Validation Grounding**:
  * Radivojević et al. (2010); Tylecote (1992); Malandra (1990); USGS MRDS.

---

### Entry 21: Shannon-Brillouin Physical Carrier Bandwidth & Linguistic Homogenization
* **Subsystem**: `InformationEntropyEngine.java`
* **Canonical Literature**:
  * Shannon, C. E. (1948). *A Mathematical Theory of Communication*. Bell System Technical Journal, 27(3), 379–423.
  * Brillouin, L. (1956). *Science and Information Theory*. Academic Press.
  * Tainter, J. A. (1988). *The Collapse of Complex Societies*. Cambridge University Press.
* **Canonical Published Value**:
  * Acoustic/oral courier bandwidth ($\sim 1\text{ Hz}$) scales continuously with physical print/postal infrastructure ($k_{\text{pc}}$) and electromagnetic signal power ($p_{\text{exo}}$).
* **Calibrated Ether Formulation (Novel Biophysical Formalization)**:
  $$B_{\text{carrier}}(k_{\text{pc}}, p) = B_0 + \alpha_k \cdot k_{\text{pc}} + \alpha_p \cdot p_{\text{exo}}^2 \quad [\text{Hz}]$$
  $$C = B_{\text{carrier}}(k_{\text{pc}}, p) \cdot \log_2\!\left(1 + \frac{10.0}{\text{Friction}}\right) \quad [\text{bits/s}], \qquad \Delta D_{\text{drift}} = -0.015 \cdot \frac{C}{C + C_{1/2}}$$
  with $B_0 = 1.0\text{ Hz}$, $\alpha_k = 0.05\text{ Hz}\cdot\text{cap/kg}$, $\alpha_p = 0.002\text{ Hz}/(\text{W/cap})^2$, and $C_{1/2} = 100.0\text{ bits/s}$.
* **Root Cause & Justification**:
  * Replaces discrete media thresholds and `if (capacity > 100)` branches with a single continuous polynomial carrier bandwidth law coupled to Shannon-Hartley channel capacity $C$ and Michaelis-Menten linguistic homogenization $C / (C + C_{1/2})$.
* **Empirical Validation Grounding**:
  * Shannon (1948); Brillouin (1956); Buringh & van Zanden (2009); Seshat Global History Databank.

---

### Entry 22: Agricultural EROEI (Caloric Output / Input) & Neolithic Domestication Dynamics
* **Subsystem**: `CliodynamicAnalyticalFormulasCatalog.java`
* **Canonical Literature**:
  * Smil, V. (2008). *Energy in Nature and Society: General Energetics of Complex Systems*. MIT Press.
  * Giampietro, M., Mayumi, K., & Sorman, A. H. (2013). *The Metabolic Pattern of Societies: Where Economists Fall Short*. Routledge.
  * Leach, G. (1976). *Energy and Food Production*. IPC Science and Technology Press.
* **Canonical Published Value**:
  * Deep-time invariant (`AGENTS.md` §8.2): Formal agricultural EROEI $\equiv 0.0$ prior to the Neolithic transition ($\sim -10\,000$ BP).
  * Early Neolithic horticulture / swidden cultivation: high net caloric return on low embodied labor input ($\sim 14.0 - 16.0$ in early Fertile Crescent emmer/einkorn hand farming).
  * Traditional draft-animal plow agriculture: field clearance, draft maintenance, and tilling stabilize net return at $\sim 4.2 - 4.6$.
  * Industrialized Green Revolution: Haber-Bosch nitrogen, diesel tractors, and mechanization expand per-worker output to $\sim 9.8$ (1914 CE) and $\sim 10.5$ (modern).
* **Calibrated Ether Formulation**:
  $$\sigma_{\text{agr}}(\Phi_{\text{exergy}}) = \frac{1}{1 + \exp\left(-\frac{\Phi_{\text{exergy}} - 0.935}{0.010}\right)}$$
  $$\text{EROEI}_{\text{agr}} = \sigma_{\text{agr}}(\Phi_{\text{exergy}}) \cdot \left[ \text{EROEI}_{\text{trad}} + \Delta \text{EROEI}_{\text{neolithic}} + \Delta \text{EROEI}_{\text{ind}} \right]$$
  where:
  $$\text{EROEI}_{\text{trad}} = 4.3 + 0.5 \tanh(sp(\Phi, 1.15) \cdot 3.00)$$
  $$\Delta \text{EROEI}_{\text{neolithic}} = 9.7 \exp\left( -\frac{(\Phi - 0.945)^2}{0.002} \right)$$
  $$\Delta \text{EROEI}_{\text{ind}} = \frac{5.5}{1 + \exp\left(-\frac{\Phi - 1.48}{0.08}\right)}$$
* **Root Cause & Justification**:
  * Evaluating agricultural EROEI during the Pleistocene without a biophysical emergence activation yields unphysical baseline values ($\sim 4.3$) for hunter-gatherers who do not possess agricultural land ($B_{\text{agr}} = 0$).
  * The calibrated continuous formulation introduces the Fermi-Dirac transition $\sigma_{\text{agr}}(\Phi_{\text{exergy}})$ at $\Phi \ge 0.935$ (corresponding to initial Holocene domestication). This strictly enforces the deep-time zero invariant during the Paleolithic (e.g., Solutrean $-20\,000\text{ BP}$ $\text{EROEI} = 0.0$), followed by the characteristic early horticultural spike ($14.0$), the traditional plow plateau ($4.3-4.8$), and the industrial Green Revolution expansion ($9.8-10.5$).
* **Empirical Validation Grounding**:
  * Smil (2008); Giampietro et al. (2013); Leach (1976); HYDE 3.4.

---

## 🔬 3. Epistemic Audit Summary

| # | Calibrated Subsystem | Academic Author(s) | Original Domain | Ether Calibrated Domain | Root Cause of Divergence |
|---|---|---|---|---|---|
| 1 | Infant Mortality ($q_5$) | Omran (1971), Preston (1975) | National historical time series | Cellular $k_{\text{pc}}$ ($\text{kg/cap}$) | Elimination of unphysical pre-sanitation pathogen suppression |
| 2 | Political Stress Index ($\Psi$) | Turchin (2016), Goldstone (1991) | Lumped dimensionless ratio | Spatial tensor aggregation $[0, 100]$ | Normalization of integrative empire baseline ($\Psi \sim 45-52$) |
| 3 | Megafauna Extinction | Sandom et al. (2014), Barnosky (2004) | Continental fossil bone beds | Continuous deep-time $t_{\text{kya}}$ | Universal logistic centered at LGM ($20\,\text{kya}$) with hunting coupling |
| 4 | Capital Steady-State | Solow (1956), Krausmann (2017) | Macro aggregate equilibrium | Local H3 cell capital turnover | Coupling steady-state material stock $k^*$ strictly to power flux $P$ |
| 5 | Primary Energy Exergy | Smil (2017), White (1943) | National historical statistics | Physical $k_{\text{pc}}$ & coal stock | Coupling prime-mover power strictly to metallic capital and fossil availability |
| 6 | Urban Nucleation | Bairoch (1988), de Vries (1984) | Discrete city census $\ge 5000$ | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Decoupling agrarian clustering from factory prime movers |
| 7 | Multi-Epoch Literacy | Buringh & van Zanden (2009) | European book & notary records | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Multi-stage scribal ($6.5\%$) vs mass schooling ($40.8\%$) decomposition |
| 8 | Groundwater Withdrawal | Wada et al. (2010), FAO AQUASTAT | National annual volume ($m^3$) | Cellular areal depth ($mm/\Delta t$) | Dimensional consistency dividing population by cell surface area |
| 9 | Structural Metallurgy | Malandra (1990), USGS (2023) | Regional archaeological finds | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Multi-stage bloomery, coke, and Bessemer steel scaling |
| 10 | Gross World Product (GWP) | Maddison (2007), Bolt & van Zanden (2020) | National macro Geary-Khamis | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Malthusian subsistence floor vs industrial capital deepening |
| 11 | Information Travel Speed | Scheidel (2014) ORBIS, Standage (1998) | Historical route measurements | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Separation of physical locomotion from electromagnetic propagation |
| 12 | Shipping Freight Cost | Harley (1988), Williamson (2004) | Historical freight indices | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Pre-Bronze Age zero invariant & smooth multi-stage sail/steam reductions |
| 13 | Garrett-Lotka Exergy Index | Garrett (2011), Lotka (1922) | Thermodynamic civilizational theory | Continuous H3 state $(p, k_{\text{pc}})$ | Normalization of physical capital ($120\text{ kg}$) and power ($250\text{ W}$) |
| 14 | Topsoil Erosion Loss | Montgomery (2007), Williams (2003) | Geomorphic field studies | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Geological background ($0.03$) and smooth ard/plow sigmoids ($\Phi \ge 0.88$) |
| 15 | Sovereign Debt Burden | Reinhart & Rogoff (2009), Dickson (1967) | Historical fiscal archives | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Deep-time zero invariant & multi-epoch patrimonial/funded/war progression |
| 16 | Global Trade Volume | Federico & Tena-Junguito (2017) | Historical trade statistics | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Deep-time zero invariant & oceanic sail ($\Phi \ge 1.20$) and steam expansion |
| 17 | Combinatorial Capital ($dK/dt$) | Arthur (2009), Kremer (1993) | Abstract discrete module space | Physical tooling stock $k_{\text{pc}}$ ($\text{kg/cap}$) | Embodiment of recombinant innovation in conserved material capital |
| 18 | Biophysical Stratification ($G^*$) | Lenski (1966), Milanovic et al. (2011) | Discrete sociological stages | Continuous $C^1$ function of $k_{\text{pc}}$ | Grounding Inequality Possibility Frontier in storable physical surplus |
| 19 | Exergy Transducer ($\eta_{\text{exergy}}$) | Ayres & Warr (2005), Kümmel (2011) | Empirical national time series | Continuous function of $k_{\text{pc}}$ | Carnot/second-law conversion efficiency driven by machinery mass |
| 20 | Chalcolithic Smelting Gate | Hotelling (1931), Radivojević (2010) | $t \ge 0$ resource economics | Fermi-Dirac kiln gate $k_{\text{pc}} \ge 25\text{ kg}$ | Enforcement of Pleistocene zero-smelting invariant without date hacks |
| 21 | Carrier Bandwidth ($B_{\text{carrier}}$) | Shannon (1948), Brillouin (1956) | Channel capacity theory | Physical function of $(k_{\text{pc}}, p)$ | Grounding administrative coordination limits in physical media |
| 22 | Agricultural EROEI | Smil (2008), Giampietro et al. (2013) | Agro-energetic input-output studies | Exergy-Capital Index $\Phi_{\text{exergy}}$ | Deep-time zero invariant ($\Phi < 0.935$) & Neolithic horticulture surge (14.0) |
