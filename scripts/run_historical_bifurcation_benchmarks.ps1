# ==============================================================================
# ETHER HISTORICAL BIFURCATION BENCHMARK RUNNER (POWERSHELL)
# ==============================================================================
# Single-entry script to execute either:
#   1. Micro-Algebraic Epistemic Tests (fast unit validation: ~1s)
#   2. Canonical 7 Ruptures Master Suite (algebraic & empirical residual: ~2s)
#   3. Full Planetary Multi-Decadal Runs (41,162 cells via HeadlessBatchRunner)
# ==============================================================================

param (
    [ValidateSet("all", "master", "residual", "leaders", "macro")]
    [string]$Mode = "master"
)

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  ETHER CLIOCLOUD: HISTORICAL BIFURCATION & RUPTURE BENCHMARK    " -ForegroundColor Yellow
Write-Host "  Execution Mode: $Mode                                          " -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Cyan

switch ($Mode) {
    "master" {
        Write-Host "[1/1] Running Master Historical Bifurcation Suite (7 Scenarios + Theorems)..." -ForegroundColor White
        mvn test "-Dtest=MasterHistoricalBifurcationSuite"
    }
    "residual" {
        Write-Host "[1/1] Running Empirical Residual & Metastability Suite..." -ForegroundColor White
        mvn test "-Dtest=EmpiricalResidualBifurcationTest"
    }
    "leaders" {
        Write-Host "[1/1] Running Historical Leader A/B Falsification Suite..." -ForegroundColor White
        mvn test "-Dtest=HistoricalLeaderBifurcationTest"
    }
    "all" {
        Write-Host "[1/3] Running Master Historical Bifurcation Suite..." -ForegroundColor White
        mvn test "-Dtest=MasterHistoricalBifurcationSuite"
        Write-Host "[2/3] Running Empirical Residual Inversion Suite..." -ForegroundColor White
        mvn test "-Dtest=EmpiricalResidualBifurcationTest"
        Write-Host "[3/3] Running Historical Leader A/B Falsification Suite..." -ForegroundColor White
        mvn test "-Dtest=HistoricalLeaderBifurcationTest"
    }
    "macro" {
        Write-Host "[PLANETARY BATCH] Running 41,162 H3 cell headless simulations..." -ForegroundColor Yellow
        Write-Host "Note: Full planetary integration requires multi-threaded HPC / GCP VM." -ForegroundColor Yellow
        mvn exec:java -Dexec.mainClass="org.ether.society.core.headless.HeadlessBatchRunner"
    }
}

Write-Host "`n[COMPLETED] Bifurcation benchmark execution finished." -ForegroundColor Green
