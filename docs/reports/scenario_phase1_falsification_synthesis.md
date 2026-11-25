# ⚠️ Disjoint Time Intervals Across Selected Scenarios

The selected trajectories have no overlapping calendar year intersection:

- **Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)** (`HISTORICAL_GROUND_TRUTH`): `[-3000000 .. 2025]`
- **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** (`RUN-ISLAMIC-EXPANSION---CALIPHAL-COMMERCIAL-REVOLUTION--632-CE--3876`): `[632 .. 652]`
- **Carolingian Renaissance and Caliphal Apex (800 CE)** (`RUN-CAROLINGIAN-RENAISSANCE-AND-CALIPHAL-APEX--800-CE--3507`): `[800 .. 820]`
- **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** (`RUN-SONG-DYNASTY---HYDRAULIC-PRE-INDUSTRIALIZATION--1000-CE--1181`): `[1000 .. 1020]`
- **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** (`RUN-FERTILE-CRESCENT---NEOLITHIC-AGRICULTURE---8000-BCE--510`): `[-8000 .. -7980]`
- **Last Glacial Maximum & Solutrean (-20,000 BP)** (`RUN-LAST-GLACIAL-MAXIMUM---SOLUTREAN---20-000-BP--3582`): `[-20000 .. -19980]`

## 📈 Visual Synthesis: Fidelity Curves, 27-Variable Profile & 2D Cartographic Quality

![1. Master 45-Scenario Fidelity Curve](../images/charts/chart1_45_scenarios_fidelity_curve.png)

![2. Quality Profile of All 27 Canonical State Variables](../images/charts/chart2_27_variables_quality_profile.png)

![3. 2D Cartographic Raster Fidelity & 9D Geocultural Tensor Quality Curves](../images/charts/chart3_cartographic_2d_quality_curves.png)

![4. Multi-Scenario x 27-Variable Heatmap Matrix](../images/charts/chart4_multi_scenario_heatmap_matrix.png)

![5. Pillars 6 & 7 Validation Summary](../images/charts/chart5_pillars_6_and_7_validation_summary.png)

![6. H3 Scaling & Computational ROI Pareto Frontier](../images/charts/chart6_engine_scaling_and_roi_pareto.png)

## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis

> Direct H3 cellular spatial sampling between spherical cell centroids and empirical reference rasters (HYDE 3.4).

| Scenario | Spatial Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |
|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | Res 4 | 632 | **0.8288** | **+0.9656** | 98.4% | 8.9°N, 98.8°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | Res 4 | 800 | **0.8213** | **+0.9654** | 98.5% | 8.9°N, 98.8°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | Res 4 | 1000 | **0.7219** | **+0.9603** | 98.5% | 12.0°N, 98.6°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | Res 7 (Win) | -8000 | **0.1909** | **+0.6231** | 98.5% | 33.8°N, 35.6°E | MODERATE CELLULAR FIDELITY - General regional structure aligned, localized demographic gradients exist. |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | Res 4 | -20000 | **0.2328** | **+0.9753** | 97.1% | 19.2°N, -99.0°E | HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth. |

---

## 🏛️ Geocultural Tensor Drift & Reliability Synthesis Across 9 Epochs

> Empirical comparison across 9 orthogonal geocultural tensor layers (isogloss, kinship, rituals, sovereignty, technology, trade, institutional, ecological, pathogen) against historical ground truth rasters.

| Scenario | Epoch | Channel | Pearson (r) | SSIM | Categorical IoU | Epistemic Drift Diagnostic |
|---|:---:|---|:---:|:---:|:---:|:---:|
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Linguistic Isoglosses (Dim 0) | **+0.7604** | **0.6618** | 86.8% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Kinship & Clan Structure (Dim 1) | **+0.7917** | **0.6834** | 86.8% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Sacred Rituals & Solidarity (Dim 2) | **+0.7615** | **0.6874** | 81.3% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Sovereignty & Borders (Dim 3) | **+0.8123** | **0.7925** | 86.9% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Materiality & Tooling (Dim 4) | **+0.9271** | **0.7658** | 88.0% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Trade & Market Access (Dim 5) | **+0.2814** | **0.0892** | 12.9% | 🔴 Cultural Tensor Drift |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Administrative Hierarchy (Dim 6) | **+0.5499** | **0.0738** | 87.6% | 🔴 Cultural Tensor Drift |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Ecological Carrying Capacity (Dim 7) | **+0.9593** | **0.6706** | 88.3% | 🟢 Robust Empirical Alignment |
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | 632 | Sanitary & Pathogen Memory (Dim 8) | **+0.7656** | **0.7131** | 88.0% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Linguistic Isoglosses (Dim 0) | **+0.7768** | **0.7000** | 87.2% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Kinship & Clan Structure (Dim 1) | **+0.7921** | **0.6956** | 87.2% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Sacred Rituals & Solidarity (Dim 2) | **+0.7634** | **0.7124** | 81.7% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Sovereignty & Borders (Dim 3) | **+0.8093** | **0.7926** | 87.3% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Materiality & Tooling (Dim 4) | **+0.9252** | **0.7562** | 88.4% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Trade & Market Access (Dim 5) | **+0.2773** | **0.0797** | 12.4% | 🔴 Cultural Tensor Drift |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Administrative Hierarchy (Dim 6) | **+0.5461** | **0.0711** | 88.0% | 🔴 Cultural Tensor Drift |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Ecological Carrying Capacity (Dim 7) | **+0.9595** | **0.6511** | 88.7% | 🟢 Robust Empirical Alignment |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | 800 | Sanitary & Pathogen Memory (Dim 8) | **+0.7738** | **0.6946** | 88.4% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Linguistic Isoglosses (Dim 0) | **+0.7893** | **0.7675** | 87.5% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Kinship & Clan Structure (Dim 1) | **+0.8067** | **0.7745** | 87.5% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Sacred Rituals & Solidarity (Dim 2) | **+0.7714** | **0.7603** | 81.8% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Sovereignty & Borders (Dim 3) | **+0.8193** | **0.7449** | 87.5% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Materiality & Tooling (Dim 4) | **+0.9263** | **0.7576** | 88.6% | 🟢 Robust Empirical Alignment |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Trade & Market Access (Dim 5) | **+0.2730** | **0.0596** | 12.1% | 🔴 Cultural Tensor Drift |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Administrative Hierarchy (Dim 6) | **+0.5443** | **0.0678** | 88.3% | 🔴 Cultural Tensor Drift |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Ecological Carrying Capacity (Dim 7) | **+0.9534** | **0.5578** | 89.0% | 🟡 Moderate Spatial Gradient |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | 1000 | Sanitary & Pathogen Memory (Dim 8) | **+0.7807** | **0.6160** | 88.6% | 🟢 Robust Empirical Alignment |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Linguistic Isoglosses (Dim 0) | **+0.5414** | **0.0306** | 94.9% | 🔴 Cultural Tensor Drift |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Kinship & Clan Structure (Dim 1) | **+0.5414** | **0.0306** | 94.9% | 🔴 Cultural Tensor Drift |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Sacred Rituals & Solidarity (Dim 2) | **+0.4934** | **0.0464** | 94.9% | 🔴 Cultural Tensor Drift |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Sovereignty & Borders (Dim 3) | **+0.5121** | **0.0290** | 94.9% | 🔴 Cultural Tensor Drift |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Materiality & Tooling (Dim 4) | **+0.7308** | **0.4422** | 95.2% | 🟡 Moderate Spatial Gradient |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Trade & Market Access (Dim 5) | **-0.0873** | **0.0103** | 11.2% | 🔴 Cultural Tensor Drift |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Administrative Hierarchy (Dim 6) | **+0.6130** | **0.7653** | 95.0% | 🟡 Moderate Spatial Gradient |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Ecological Carrying Capacity (Dim 7) | **+0.7275** | **0.3906** | 95.2% | 🟡 Moderate Spatial Gradient |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | -8000 | Sanitary & Pathogen Memory (Dim 8) | **+0.5230** | **0.2280** | 95.2% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Linguistic Isoglosses (Dim 0) | **+0.6717** | **0.0011** | 58.1% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Kinship & Clan Structure (Dim 1) | **+0.6851** | **0.0010** | 58.1% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Sacred Rituals & Solidarity (Dim 2) | **+0.6397** | **0.0040** | 58.1% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Sovereignty & Borders (Dim 3) | **+0.6683** | **0.0013** | 58.1% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Materiality & Tooling (Dim 4) | **+0.8126** | **0.0743** | 85.9% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Trade & Market Access (Dim 5) | **+0.0817** | **0.7290** | 1.9% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Administrative Hierarchy (Dim 6) | **+0.8400** | **0.2011** | 85.2% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Ecological Carrying Capacity (Dim 7) | **+0.7581** | **0.1516** | 75.3% | 🔴 Cultural Tensor Drift |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | -20000 | Sanitary & Pathogen Memory (Dim 8) | **+0.6204** | **0.0086** | 75.2% | 🔴 Cultural Tensor Drift |

---

## ⚡ Planetary Simulation Engine Performance Synthesis Across 9 Historical Scenarios

> Empirical computational throughput, execution latency, and memory footprint recorded across all 9 canonical scenarios.

| Scenario | Spatial Domain | Cells | Duration | TPS | ms/tick | Latency/Cell | Cell Throughput | Peak Heap |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Islamic Expansion & Caliphal Commercial Revolution (632 CE)** | Res 4 | 288,122 | 577.4 s | **0.42** | 2405.9 ms | **8.350 μs** | 119,759 cells/s | 2078 / 8192 MB |
| **Carolingian Renaissance and Caliphal Apex (800 CE)** | Res 4 | 288,122 | 629.8 s | **0.38** | 2624.2 ms | **9.108 μs** | 109,795 cells/s | 4289 / 8192 MB |
| **Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)** | Res 4 | 288,122 | 545.1 s | **0.44** | 2271.1 ms | **7.882 μs** | 126,865 cells/s | 2095 / 8192 MB |
| **Fertile Crescent & Neolithic Agriculture (-8000 BCE)** | Res 7 (Win) | 197,375 | 474.7 s | **0.51** | 1977.7 ms | **10.020 μs** | 99,799 cells/s | 2506 / 8192 MB |
| **Last Glacial Maximum & Solutrean (-20,000 BP)** | Res 4 | 288,122 | 407.0 s | **0.59** | 1695.7 ms | **5.885 μs** | 169,916 cells/s | 4693 / 8192 MB |

---

