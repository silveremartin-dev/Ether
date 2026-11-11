# 📐 H3 MULTI-RESOLUTION SPATIAL DISCRETIZATION CONVERGENCE REPORT

**Evaluation Scenario**: `Antonine Plague & Imperial Crisis under Marcus Aurelius (165 CE)`

**Empirical Ground-Truth Source**: HYDE 3.4 Historical Population Density Raster (5 arc-min)

**Metrological Method**: Direct H3 cellular spherical centroid sampling (`compareCellsToRaster`)

## 📊 Spatial Convergence Matrix

| H3 Resolution | Hexagonal Cells | Mean Hex Edge | Pearson Spatial ($r$) | Jaccard Ecumene (IoU) | Structural SSIM | Spatial RMSE | Execution Time |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Res 2** | 5,882 | 158.2 km | **+0.9857** | **92.69%** | **0.9242** | **12.10** | 12.9 s |
| **Res 3** | 41,162 | 59.8 km | **+0.9859** | **98.51%** | **0.9162** | **13.04** | 21.1 s |
| **Res 4** | 288,122 | 22.6 km | **+0.9859** | **99.57%** | **0.9114** | **13.58** | 49.3 s |

## 🔬 Biophysical & Epistemic Convergence Findings

1. **Modifiable Areal Unit Problem (MAUP) Mitigation**: At H3 Resolution 2 (~11,000 km²/cell), each hexagon averages over ~128 HYDE raster pixels, smoothing hyper-dense alluvial corridors (Nile, Po, Tigris-Euphrates). Increasing resolution to Res 4 (~250 km²/cell) aligns with the native spatial frequency of HYDE 3.4.

2. **Elimination of Cartographic Discretization Gaps**: Direct spherical sampling at cellular centroids completely eliminates the 2D pixel gap ('gruyère') artifacts inherent to intermediate equirectangular rasterization, yielding authentic Pearson cross-correlation and ecumene overlap indices across all discrete grid levels.

