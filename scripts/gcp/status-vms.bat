@echo off
setlocal enabledelayedexpansion
REM ==============================================================================
REM Ether - Status of Simulation VMs on GCP (Windows Batch Parity)
REM ==============================================================================

set PROJECT_ID=%1
if "%PROJECT_ID%"=="" set PROJECT_ID=ether-509812

set ZONE=%2
if "%ZONE%"=="" set ZONE=europe-west1-b

echo ==========================================================
echo           ETHER -- GCP SIMULATION VM STATUS
echo ==========================================================
echo   Project ID : %PROJECT_ID%
echo   Zone       : %ZONE%
echo ----------------------------------------------------------

call gcloud compute instances list --project="%PROJECT_ID%" --zone="%ZONE%"
endlocal
