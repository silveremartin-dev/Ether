# ⚠️ Disjoint Time Intervals Across Selected Scenarios

The selected trajectories have no overlapping calendar year intersection:

- **Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)** (`HISTORICAL_GROUND_TRUTH`): `[-3000000 .. 2025]`
- **World Wars, Totalitarian Rupture & Nuclear Era (1914 CE)** (`RUN-WORLD-WARS--TOTALITARIAN-RUPTURE---NUCLEAR-ERA--1914-CE--9085`): `[1914 .. 1960]`
- **The Black Death & Post-Feudal Economic Inversion (1347 CE)** (`RUN-THE-BLACK-DEATH---POST-FEUDAL-ECONOMIC-INVERSION--1347-CE--7851`): `[1347 .. 1348]`
- **Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)** (`RUN-RAMESSES-II--PAX-AEGYPTIACA---MONUMENTAL-BUILDERS---1279-BCE--3082`): `[-1279 .. -1278]`
- **Late Bronze Age Collapse Twin B: Counterfactual Resilient Agrarian Network (-1250 -> -1150 BCE)** (`RUN-BRONZE_AGE_COLLAPSE_1200BCE_COUNTERFACTUAL`): `[-1250 .. -1150]`
- **Antonine Plague & Imperial Crisis under Marcus Aurelius (165-180 CE)** (`RUN-ANTONINE-PLAGUE---IMPERIAL-CRISIS-UNDER-MARCUS-AURELIUS--165-180-CE--824`): `[165 .. 179]`
- **Pax Romana & Augustan Stability (0 -> 50 CE): Biophysical Continuity** (`RUN-PAX-ROMANA---AUGUSTAN-STABILITY--0----50-CE---BIOPHYSICAL-CONTINUITY-5367`): `[0 .. 20]`


## ðŸ—ºï¸ 4. 2D Cartographic Fidelity & Spatial Discrepancy Synthesis (Snapshots vs Ground Truth Rasters)

> Direct pixel-to-pixel evaluation between empirical reference rasters (NOAA ETOPO / HYDE 3.4 / Maddison) and projected simulation grids.

| Scenario | Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |
|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **World Wars, Totalitarian Rupture & Nuclear Era (1914 CE)** | Res 3 | 1914 | **0.3999** | **+0.6168** | 62.3% | 57.0Â°N, 60.5Â°E | Ural Mobilization vs Western Front Over-attrition |
| **The Black Death & Post-Feudal Economic Inversion (1347 CE)** | Res 3 | 1347 | **0.3804** | **+0.6113** | 54.1% | 45.7Â°N, 8.4Â°E | Northern Italy Maritime Entry Discontinuity |
| **Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)** | Res 6 | -1279 | **0.4495** | **+0.5244** | 48.2% | 33.0Â°N, 35.2Â°E | Levant Border Window Discontinuity |
| **Late Bronze Age Collapse Twin B: Counterfactual Resilient Agrarian Network (-1250 -> -1150 BCE)** | Res 6 | -1200 | **0.3621** | **+0.5110** | 43.5% | 35.3Â°N, 35.9Â°E | Ugarit & Levantine Maritime Raid Destruction Horizon |
| **Antonine Plague & Imperial Crisis under Marcus Aurelius (165-180 CE)** | Res 6 | 165 | **0.4210** | **+0.5890** | 51.2% | 36.2Â°N, 37.1Â°E | Syrian Limes & Mesopotamia Legionary Vector |
| **Pax Romana & Augustan Stability (0 -> 50 CE): Biophysical Continuity** | Res 6 | 0 | **0.4632** | **+0.6340** | 57.8% | 41.9Â°N, 12.5Â°E | Rome Megacity Agglomeration Core |

---

