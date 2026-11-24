# ⚠️ Disjoint Time Intervals Across Selected Scenarios

The selected trajectories have no overlapping calendar year intersection:

- **Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)** (`HISTORICAL_GROUND_TRUTH`): `[-3000000 .. 2025]`
- **Late Bronze Age Collapse & Sea Peoples Cataclysm (-1200 BCE)** (`RUN-LATE-BRONZE-AGE-COLLAPSE---SEA-PEOPLES---1200-BCE--2544`): `[-1200 .. -1180]`
- **Mongol Conquest & Eurasian Steppe Unification (1206 CE)** (`RUN-THE-MONGOL-EMPIRE---EURASIAN-RUPTURE--1206-CE--3090`): `[1206 .. 1226]`


## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis (Phase 2)

| Scenario | Spatial Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |
|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Late Bronze Age Collapse & Sea Peoples Cataclysm (-1200 BCE)** | Res 4 | -1200 | **0.4000** | **+0.9871** | 98.5% | 19.2°N, -99.0°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Mongol Conquest & Eurasian Steppe Unification (1206 CE)** | Res 4 | 1206 | **0.9476** | **+0.9802** | 98.6% | 19.2°N, -99.0°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |

---

## ⚡ Engine Performance Synthesis Across Phase 2 Scenarios

| Scenario | Domain | Cells | Duration | TPS | ms/tick | Latency/Cell | Cell Throughput | Peak Heap |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Late Bronze Age Collapse & Sea Peoples Cataclysm (-1200 BCE)** | Res 4 | 288,122 | 765.0 s | **0.31** | 3187.6 ms | **11.063 μs** | 90,387 cells/s | 4413 / 8192 MB |
| **Mongol Conquest & Eurasian Steppe Unification (1206 CE)** | Res 4 | 288,122 | 603.6 s | **0.40** | 2514.8 ms | **8.728 μs** | 114,569 cells/s | 4649 / 8192 MB |
