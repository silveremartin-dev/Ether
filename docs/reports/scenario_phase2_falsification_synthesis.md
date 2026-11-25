# ⚠️ Disjoint Time Intervals Across Selected Scenarios

The selected trajectories have no overlapping calendar year intersection:

- **Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)** (`HISTORICAL_GROUND_TRUTH`): `[-3000000 .. 2025]`
- **Pax Romana & Principate of Augustus (0 CE)** (`RUN-PAX-ROMANA---AUGUSTAN-STABILITY--0----50-CE---BIOPHYSICAL-CONTINUITY-5367`): `[0 .. 20]`
- **Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)** (`RUN-RAMESSES-II--PAX-AEGYPTIACA---MONUMENTAL-BUILDERS---1279-BCE--7068`): `[-1279 .. -1259]`
- **Columbian Contact & Transoceanic Ecological Exchange (1492 CE)** (`RUN-EUROPEAN-ARRIVAL-IN-THE-AMERICAS---MICROBIAL-SHOCK--1492-CE--8118`): `[1492 .. 1512]`
- **Tokugawa Shogunate & Sakoku Autarky (1639 CE)** (`RUN-TOKUGAWA-JAPAN---SAKOKU-ISOLATION--1639-CE--7860`): `[1639 .. 1659]`
- **Post-War Reconstruction & The Great Acceleration (1950 CE)** (`RUN-THE-GREAT-ACCELERATION---ANTHROPOCENE--1950-CE--8271`): `[1950 .. 1970]`
- **Out of Africa & Homo Sapiens Expansion (-100,000 BP)** (`RUN-OUT-OF-AFRICA---HOMO-SAPIENS-EXPANSION---100-000-BP--4146`): `[-100000 .. -99980]`


## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis (Phase 2)

| Scenario | Spatial Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |
|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Pax Romana & Principate of Augustus (0 CE)** | Res 4 | 0 | **0.7620** | **+0.9821** | 98.6% | 39.7°N, 118.9°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)** | Res 4 | -1279 | **0.0325** | **+0.9845** | 98.5% | 30.6°N, 31.2°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Columbian Contact & Transoceanic Ecological Exchange (1492 CE)** | Res 4 | 1492 | **0.6385** | **+0.9839** | 98.6% | 19.2°N, -99.0°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Tokugawa Shogunate & Sakoku Autarky (1639 CE)** | Res 4 | 1639 | **0.2828** | **+0.9850** | 98.6% | 37.6°N, 127.1°E | MODERATE CELLULAR FIDELITY - General regional structure aligned, localized demographic gradients exist. |
| **Post-War Reconstruction & The Great Acceleration (1950 CE)** | Res 4 | 1950 | **0.5229** | **+0.9270** | 95.0% | 27.0°N, -109.9°E | MODERATE CELLULAR FIDELITY - General regional structure aligned, localized demographic gradients exist. |
| **Out of Africa & Homo Sapiens Expansion (-100,000 BP)** | Res 3 | -100000 | **0.5499** | **+0.9798** | 97.2% | 25.5°N, 32.0°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |

---

## ⚡ Engine Performance Synthesis Across Phase 2 Scenarios

| Scenario | Domain | Cells | Duration | TPS | ms/tick | Latency/Cell | Cell Throughput | Peak Heap |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Pax Romana & Principate of Augustus (0 CE)** | Res 4 | 288,122 | 447.7 s | **0.54** | 1865.4 ms | **6.474 μs** | 154,456 cells/s | 4464 / 8192 MB |
| **Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)** | Res 4 | 288,122 | 426.0 s | **0.56** | 1775.2 ms | **6.161 μs** | 162,305 cells/s | 4463 / 8192 MB |
| **Columbian Contact & Transoceanic Ecological Exchange (1492 CE)** | Res 4 | 288,122 | 444.3 s | **0.54** | 1851.2 ms | **6.425 μs** | 155,641 cells/s | 4461 / 8192 MB |
| **Tokugawa Shogunate & Sakoku Autarky (1639 CE)** | Res 4 | 288,122 | 444.2 s | **0.54** | 1850.9 ms | **6.424 μs** | 155,670 cells/s | 4557 / 8192 MB |
| **Post-War Reconstruction & The Great Acceleration (1950 CE)** | Res 4 | 288,122 | 569.5 s | **0.42** | 2372.8 ms | **8.236 μs** | 121,425 cells/s | 3112 / 8192 MB |
| **Out of Africa & Homo Sapiens Expansion (-100,000 BP)** | Res 3 | 41,162 | 56.6 s | **4.24** | 235.9 ms | **5.731 μs** | 174,489 cells/s | 1657 / 8192 MB |
