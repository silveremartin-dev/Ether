@echo off
REM ==============================================================================
REM Ether 1.0 — Automated Multi-Scenario x Multi-Resolution Benchmark Suite (Batch)
REM ==============================================================================
setlocal enabledelayedexpansion

set PROJECT_ID=ether-509812
set ZONE=europe-west1-b
set TICKS=24

echo ==========================================================
echo      ETHER -- MULTI-DIMENSIONAL BENCHMARK SUITE MATRIX     
echo ==========================================================
powershell -ExecutionPolicy Bypass -File "%~dp0benchmark-matrix.ps1" -ProjectId "%PROJECT_ID%" -Zone "%ZONE%" -Ticks %TICKS%
