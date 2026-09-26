@echo off
rem ==============================================================================
rem ETHER HISTORICAL BIFURCATION BENCHMARK RUNNER (CMD BATCH)
rem ==============================================================================

set MODE=%1
if "%MODE%"=="" set MODE=master

echo =================================================================
echo   ETHER CLIOCLOUD: HISTORICAL BIFURCATION ^& RUPTURE BENCHMARK    
echo   Execution Mode: %MODE%                                          
echo =================================================================

if "%MODE%"=="master" (
    echo [1/1] Running Master Historical Bifurcation Suite (7 Scenarios + Theorems)...
    call mvn test "-Dtest=MasterHistoricalBifurcationSuite"
    goto end
)

if "%MODE%"=="residual" (
    echo [1/1] Running Empirical Residual ^& Metastability Suite...
    call mvn test "-Dtest=EmpiricalResidualBifurcationTest"
    goto end
)

if "%MODE%"=="leaders" (
    echo [1/1] Running Historical Leader A/B Falsification Suite...
    call mvn test "-Dtest=HistoricalLeaderBifurcationTest"
    goto end
)

if "%MODE%"=="all" (
    echo [1/3] Running Master Historical Bifurcation Suite...
    call mvn test "-Dtest=MasterHistoricalBifurcationSuite"
    echo [2/3] Running Empirical Residual Inversion Suite...
    call mvn test "-Dtest=EmpiricalResidualBifurcationTest"
    echo [3/3] Running Historical Leader A/B Falsification Suite...
    call mvn test "-Dtest=HistoricalLeaderBifurcationTest"
    goto end
)

if "%MODE%"=="macro" (
    echo [PLANETARY BATCH] Running 41,162 H3 cell headless simulations...
    call mvn exec:java "-Dexec.mainClass=org.ether.society.core.headless.HeadlessBatchRunner"
    goto end
)

echo Unknown mode: %MODE%. Choose from: master, residual, leaders, all, macro

:end
echo.
echo [COMPLETED] Bifurcation benchmark execution finished.
