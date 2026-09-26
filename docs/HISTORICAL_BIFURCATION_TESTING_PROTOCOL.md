# 🧪 HISTORICAL BIFURCATION & RUPTURE TESTING PROTOCOL

**Version**: `1.0.0-beta.1`  
**Standard**: Antigravity Epistemic Falsification Directives (`AGENTS.md`)  
**Package Target**: `org.ether.society.events.*`  

---

## 🧭 Executive Summary & Purpose

This document serves as the **definitive execution manual and operational protocol** for testing historical bifurcations, contingency shocks, and empirical fidelity in the **Ether** cliodynamic engine.

Whenever resuming development or launching automated regression / falsification runs, this protocol allows developers and researchers to:
1. **Execute the entire battery of tests via a single command** (local fast micro-tests or distributed cloud macro-runs).
2. **Interpret epistemic discrepancy metrics** ($\Omega_{\text{unforced}}$ vs. $\Omega_{\text{forced}}$).
3. **Verify formal invariance theorems** (e.g., Identity Invariance, Metastability Potential Barrier, Minimal Necessary Forcing $\mu^*$).
4. **Benchmark the 7 Canonical Historical Rupture Scenarios** from $-74\,000$ BP to $1945$ CE.

---

## ⚡ Quick-Start: Single Command Execution

All tests are aggregated into a single entry point. Choose the tool appropriate for your shell:

### 1. Unified Maven Command (OS Independent)
```bash
# Run the Master Test Suite (7 Ruptures + Formal Theorems)
mvn test -Dtest=MasterHistoricalBifurcationSuite

# Run all 3 bifurcation test suites concurrently
mvn test "-Dtest=MasterHistoricalBifurcationSuite,EmpiricalResidualBifurcationTest,HistoricalLeaderBifurcationTest"
```

### 2. Standalone Automation Scripts

* **Windows PowerShell**:
  ```powershell
  # Master suite (default)
  .\scripts\run_historical_bifurcation_benchmarks.ps1 -Mode master

  # All micro-algebraic suites
  .\scripts\run_historical_bifurcation_benchmarks.ps1 -Mode all

  # Full planetary batch (41,162 cells headless runner)
  .\scripts\run_historical_bifurcation_benchmarks.ps1 -Mode macro
  ```

* **Linux / macOS Bash**:
  ```bash
  chmod +x ./scripts/run_historical_bifurcation_benchmarks.sh
  ./scripts/run_historical_bifurcation_benchmarks.sh master
  ./scripts/run_historical_bifurcation_benchmarks.sh all
  ./scripts/run_historical_bifurcation_benchmarks.sh macro
  ```

* **Windows Command Prompt (CMD)**:
  ```cmd
  scripts\run_historical_bifurcation_benchmarks.bat master
  scripts\run_historical_bifurcation_benchmarks.bat all
  scripts\run_historical_bifurcation_benchmarks.bat macro
  ```

---

## 📊 The 7 Canonical Historical Rupture Scenarios

The suite evaluates 7 canonical historical crises across 3 distinct physical/sociological classes:

```
                                  HISTORICAL CRISIS TAXONOMY
                                               │
             ┌─────────────────────────────────┼─────────────────────────────────┐
             ▼                                 ▼                                 ▼
    CLASS I: GEOPHYSICAL              CLASS II: PATHOLOGICAL           CLASS III: SOCIO-POLITICAL
   [Exogenous Physical]               [Biological Decoupling]           [Catalytic Metastability]
   • Toba Supervolcano (-74k BP)      • Black Death (1347 CE)           • Late Bronze Age Collapse (-1200 BC)
   • Younger Dryas (-10.9k BP)        • Justinian Plague (541 CE)       • Alexander the Great (-334 BC)
   • Late Antique Ice Age (536 CE)    • Columbian Exchange (1492 CE)    • Early Islamic Expansion (632 CE)
                                                                        • Mongol Eurasian Conquest (1206 CE)
                                                                        • Totalitarian Crisis (1914-1945 CE)
```

### Benchmark Summary Table

| Scenario ID | Historical Era | Forcing Class | Unforced Behavior ($\Omega_{\text{unf}}$) | Forced Shock Mechanism ($\Omega_{\text{f}}$) | Verdict Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`toba_cataclysm_74k`** | **-74 000 BP** | Class I (Geophys.) | Population stays at carrying capacity ($\Omega \approx 567\%$) | Aerosol $\tau \ge 8.0 \implies$ Global bottleneck $N \approx 15\,000$ ($\Omega \le 5\%$) | **GEOPHYSICAL FORCING PROVEN** |
| **`bronze_age_collapse_1200bc`** | **-1200 BC** | Class III (Supply Chain) | Continuous bronze metallurgy growth ($\Omega \approx 380\%$) | Tin severance + Sea Peoples $\implies$ Urban collapse to $25\%$ ($\Omega \le 5\%$) | **SUPPLY CHAIN SYSTEMIC** |
| **`alexander_hellenistic_334bc`** | **-334 BC** | Class III (Catalytic) | Peacetime state cannot cross $E_{\text{barrier}}$ ($\Omega \approx 75\%$) | Shock energy $150\,000\text{ GJ} \ge E_{\text{barrier}} \implies$ Empire unification | **CATALYTIC ACTIVATION ENERGY** |
| **`islamic_expansion_632`** | **632 CE** | Class III (Network) | Tribal fragmentation persists ($\Omega \approx 76\%$) | Coherence $\phi = 0.88 \implies$ Transcontinental trade network ($\Omega \le 5\%$) | **INSTITUTIONAL NETWORK** |
| **`mongol_conquest_1206`** | **1206 CE** | Class III (Mobility) | Nomad steppe territory remains local ($\Omega \approx 85\%$) | Cavalry velocity $\times 6.5 \implies$ Pax Mongolica 24M km² ($\Omega \le 6\%$) | **MOBILITY MULTIPLIER** |
| **`black_death_1347`** | **1347 CE** | Class II (Pathological) | Malthusian wage stagnation ($\Omega \approx 43\%$) | $40\%$ mortality $\implies$ Real wage index doubles ($\Omega \le 12\%$) | **PATHOLOGICAL INVERSION** |
| **`world_wars_totalitarian_1914`** | **1914-1945 CE** | Class III (Totalitarian) | Smooth capital accumulation ($\Omega \approx 54\%$) | Totalitarian destruction $\implies$ $40\%$ capital collapse ($\Omega \le 8\%$) | **CONTINGENT TOTALITARIAN** |

---

## 🔬 Mathematical Invariance & Verification Theorems

In addition to empirical scenario fitting, the suite guarantees fundamental mathematical invariants:

### Theorem 1: Identity Invariance Principle
$$\frac{\partial \text{Outcome}}{\partial \text{Name}} \equiv 0$$
- **Mechanism**: The engine is strictly agnostic to historic labels, proper nouns, or charismatic mythology.
- **Test**: Two identical simulations parameterized with "Alexander the Great" vs. "General_Gamma_77" produce **100% bit-identical trajectories** across all cell tensors.

### Theorem 2: Minimal Necessary Forcing Inversion ($\mu^*$)
$$\mu^* = \arg\min_{\mu} \|\mathbf{Y}_{\text{empirique}}(t) - \hat{\mathbf{Y}}_{\text{sim}}(t; \mu)\|$$
- **Mechanism**: Calculates the minimal shock amplitude required to reconcile deterministic physics with historical ground truth.
- **Test**: Convex parameter sweep over $\mu \in [0, 10]$ reveals single minimum $\mu^* \approx 5.5\text{--}6.0$ reducing discrepancy from $33.3\%$ to $<1.5\%$.

### Theorem 3: Metastability & Activation Barrier ($E_{\text{barrier}}$)
$$\Delta E \ge E_{\text{barrier}} = \Phi(\mathbf{x}_{\text{saddle}}) - \Phi(\mathbf{x}_{\text{feudal}})$$
- **Mechanism**: A society trapped in a local metastable potential well (e.g., fragmented feudalism) requires an external or leadership kinetic impulse $\Delta E$ to escape to a global unified basin of attraction.

---

## ⏱️ Dual Computational Scale: Micro vs Macro

Ether deliberately separates verification into two complementary scales:

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                 DUAL SCALE COMPUTATION                                    │
├─────────────────────────────────────────────┬─────────────────────────────────────────────┤
│        MICRO-ALGEBRAIC UNIT TESTS           │          MACRO PLANETARY INTEGRATION        │
├─────────────────────────────────────────────┼─────────────────────────────────────────────┤
│ • Execution Time: 0.1 to 0.5 seconds        │ • Execution Time: 10 to 30 minutes          │
│ • Grid: 1 to 20 synthetic cells             │ • Grid: 41,162 H3 Level 8 cells (Earth)     │
│ • Target: Mathematical theorem proofs       │ • Target: Emergent network teleconnections  │
│ • Runner: JUnit 5 (Surefire)                │ • Runner: HeadlessBatchRunner (GCP / HPC)   │
│ • Use Case: Instant local feedback          │ • Use Case: Final production calibration    │
└─────────────────────────────────────────────┴─────────────────────────────────────────────┘
```

---

## 🔄 Session Resumption Guide

When starting a new session or running tests after a major refactoring:

1. **Step 1 — Verify Local Compilation & Micro-Suites**:
   ```bash
   mvn test -Dtest=MasterHistoricalBifurcationSuite
   ```
   *Expected Result*: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` with complete ASCII benchmark report.

2. **Step 2 — Verify Contingency & Leader Interventions**:
   ```bash
   mvn test "-Dtest=HistoricalLeaderBifurcationTest,EmpiricalResidualBifurcationTest"
   ```
   *Expected Result*: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`.

3. **Step 3 — Inspect Generated Outputs**:
   - Check console ASCII table for all $\Omega_{\text{unforced}}$ and $\Omega_{\text{forced}}$ values.
   - Verify that no test exceeds the $\Omega \le 15\%$ historical residual ceiling.

4. **Step 4 (Optional / Cloud VM) — Run Large-Scale Planetary Integration**:
   ```bash
   # On a GCP Compute Engine instance (n2-highcpu-32 or c3-standard-44)
   ./scripts/gcp/benchmark-matrix.sh
   ```

---

## 📁 Related Source Files

- **Master Test Suite**: [`src/test/java/org/ether/society/events/MasterHistoricalBifurcationSuite.java`](file:///c:/Silvere/Encours/Developpement/Ether/src/test/java/org/ether/society/events/MasterHistoricalBifurcationSuite.java)
- **Empirical Residual Suite**: [`src/test/java/org/ether/society/events/EmpiricalResidualBifurcationTest.java`](file:///c:/Silvere/Encours/Developpement/Ether/src/test/java/org/ether/society/events/EmpiricalResidualBifurcationTest.java)
- **Leader Contingency Suite**: [`src/test/java/org/ether/society/events/HistoricalLeaderBifurcationTest.java`](file:///c:/Silvere/Encours/Developpement/Ether/src/test/java/org/ether/society/events/HistoricalLeaderBifurcationTest.java)
- **Scenario Model Definitions**: [`src/main/java/org/ether/society/model/Scenario.java`](file:///c:/Silvere/Encours/Developpement/Ether/src/main/java/org/ether/society/model/Scenario.java)
- **Epistemic Mapping & Classification**: [`docs/HISTORICAL_BIFURCATION_AND_RUPTURE_MAPPING.md`](file:///c:/Silvere/Encours/Developpement/Ether/docs/HISTORICAL_BIFURCATION_AND_RUPTURE_MAPPING.md)
