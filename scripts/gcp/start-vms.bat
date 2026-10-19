@echo off
setlocal enabledelayedexpansion
REM ==============================================================================
REM Ether - Start Simulation VMs on GCP (Windows Batch Parity)
REM ==============================================================================

set PROJECT_ID=%1
if "%PROJECT_ID%"=="" set PROJECT_ID=ether-509812

set ZONE=%2
if "%ZONE%"=="" set ZONE=europe-west1-b

echo ==========================================================
echo           ETHER -- STARTING GCP SIMULATION VMs
echo ==========================================================
echo   Project ID : %PROJECT_ID%
echo   Zone       : %ZONE%
echo ----------------------------------------------------------
echo Starting 'ether-master' and 'ether-worker'...

call gcloud compute instances start ether-master ether-worker --zone="%ZONE%" --project="%PROJECT_ID%" --quiet

echo.
echo [OK] All simulation VMs are RUNNING.
call gcloud compute instances list --project="%PROJECT_ID%" --zone="%ZONE%"
endlocal
