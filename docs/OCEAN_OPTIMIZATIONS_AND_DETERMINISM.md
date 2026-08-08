# Ether Simulation — Ocean Optimizations, Physical Distortion & Determinism Guide

> **Master Technical Document**: Performance Tuning, Abyssal Macro-Aggregation, Coastal Navigation Filtering, Multi-Rate Frequency Ticking, and Numerical Determinism Benchmarks.  
> *Note: This specification is integrated into Section 4 of the Master Architecture Document ([docs/ARCHITECTURE.md](ARCHITECTURE.md)).*  
> **Document Version**: 4.0.0  
> **Authors**: Silvere Martin-Michiellot & Google DeepMind Team  

---

## 1. Context & Performance Challenges

On a global Uber H3 Earth grid (Resolutions 6 to 8, spanning 175,000 to over 1,200,000 hexagonal cells), the oceanic surface covers **70.8% of the planet**. Evaluating unpopulated abyssal ocean cells with the same fine spatio-temporal resolution as alluvial plains or dense urban nodes introduces massive computational overhead.

To resolve this inefficiency while giving researchers precise control over physical fidelity, the **Ether Engine** incorporates three major ocean and transport optimization toggles. These flags are stored inside the `Scenario` data model (ensuring experiment reproducibility and portability) and can be toggled via `ScenarioSetupPanel` or `SimulationPerformanceConfig`.

---

## 2. Detailed Technical Analysis of the 3 Optimization Toggles

### 1️⃣ Abyssal Ocean Cell Macro-Aggregation (`oceanMacroAggregationEnabled`)

- **Mechanism & Algorithm**:  
  Ocean cells with negative elevation below sterile thresholds ($z < -200\text{ m}$) lacking maritime trade infrastructure are virtually aggregated into macro-blocks. Thermohaline circulation and water mass balances are computed at the macro-block scale rather than individually per discrete H3 hexagon cell.
- **Performance Impact**:  
  Throughput increase of **+300% to +600%** (4x to 7x reduction in fluid update workload).
- **Physical Distortion Introduced**:  
  - Spatial smoothing of micro-local temperature, salinity, and acidification gradients in deep abyssal trenches.
  - Dampening of micro-vortex structures and localized albedo variations.
- **Impact on Determinism**:  
  - When strict determinism is enforced (`strictDeterminism = true`), this option is automatically disabled to guarantee bit-identical cell-by-cell integration.
  - When enabled, execution remains self-deterministic (reproducible with the same random `seed`), but numerical trajectories diverge slightly from 1:1 cell integration.

---

### 2️⃣ Coastal Navigation & Littoral Transport Filtering (`coastalNavigationOnlyEnabled`)

- **Mechanism & Algorithm**:  
  Restricts $A^*$ pathfinding and agent transport mobility to continental shelf and littoral coastal hexagons. Open-sea transoceanic navigation is filtered out until relevant maritime technologies (compass, caravel, sextant, steam propulsion) are unlocked in the `TechnologyTree`.
- **Performance Impact**:  
  Throughput increase of **+200% to +400%** during agent transport and trade flux evaluation (eliminates pathfinding exploration over empty deep-sea nodes).
- **Physical Distortion Introduced**:  
  - Prevents accidental transoceanic agent drift in pre-industrial or ancient historical epochs.
  - Introduces a slight coastal confinement bias prior to achieving technological navigation milestones.
- **Impact on Determinism**:  
  - Fully deterministic under modified logical graph rules. Produces identical results across identical runs.

---

### 3️⃣ Ocean Multi-Rate Frequency Ticking (`oceanMultiRateTickingEnabled`)

- **Mechanism & Algorithm**:  
  Executes slow-evolving fluid and marine ecological engines (`ThermohalineOceanEngine`, `OceanAcidificationEngine`) at a reduced sub-sampling frequency ($\Delta t_{\text{ocean}} = N \times \Delta t_{\text{atmosphere}}$, e.g. evaluating ocean dynamics every 5 or 10 ticks instead of every monthly step).
- **Performance Impact**:  
  Throughput increase of **+150% to +300%** on planetary climate loop execution.
- **Physical Distortion Introduced**:  
  - Step-wise temporal lag (temporal aliasing) during sudden cataclysmic climate shocks (e.g., stratospheric aerosol injection from VEI-7 volcanic eruptions like Tambora or Samalas).
  - Minor dampening of seasonal surface water thermal response.
- **Impact on Determinism**:  
  - Sensitive to sub-sampling rate $N$. Deterministic for a fixed rate $N$ and random seed, but results diverge from unit-frequency integration.

---

## 3. Comparative Performance & Determinism Matrix

| Optimization Flag / Mode | Simulation Speedup (TPS) | Physical Distortion | Numerical Determinism Level |
| :--- | :--- | :--- | :--- |
| **Strict Determinism Mode** (`strictDeterminism = true`) | Baseline (1.0x) | **0% (Absolute Physical Precision)** | **Bit-Identical (100%)** |
| **Ocean Macro-Aggregation** (`oceanMacroAggregationEnabled`) | **+300% to +600%** | Abyssal spatial gradient smoothing | Reproducible Heuristic |
| **Coastal Navigation Only** (`coastalNavigationOnlyEnabled`) | **+200% to +400%** | Coastal confinement pre-tech | 100% Deterministic (Modified Graph) |
| **Ocean Multi-Rate Ticking** (`oceanMultiRateTickingEnabled`) | **+150% to +300%** | Temporal aliasing during shocks | Sensitive to Sub-Sampling Rate $N$ |

---

## 4. Operational Configuration Guidelines

1. **Scientific Calibration & Precision Research**:  
   Disable all ocean optimizations or enable `strictDeterminism = true`. This guarantees maximum numerical integration accuracy for peer-reviewed historical calibration.
2. **Multi-Millennial Interactive Runs**:  
   Enable all three optimization flags to achieve high frame rates (> 50–100 TPS) across long historical time spans.
3. **Regional Cliodynamic Sub-Grid Modeling**:  
   Combine spatial clipping (`clippingEnabled = true`) with macro-aggregation to focus compute resources on targeted land basins (e.g. Mesopotamia, Mediterranean, Yellow River Basin) without incurring overhead on external ocean regions.
