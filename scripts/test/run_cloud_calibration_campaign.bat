@echo off
setlocal enabledelayedexpansion
REM ==============================================================================
REM Ether - Cloud Calibration Campaign Runner (Windows Batch Parity)
REM ==============================================================================
cd /d "%~dp0..\.."

if not exist "logs\campaign" mkdir "logs\campaign"

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set dt=%%I
set TIMESTAMP=%dt:~0,8%_%dt:~8,6%
set LOG_FILE=logs\campaign\calibration_campaign_%TIMESTAMP%.log

echo ============================================================== >> "%LOG_FILE%"
echo   🚀 ETHER MASTER CLOUD CAMPAIGN STARTED: %DATE% %TIME% >> "%LOG_FILE%"
echo ============================================================== >> "%LOG_FILE%"
echo [INFO] Logging output to %LOG_FILE%

set MAVEN_OPTS=-Xms4g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=HistoricalScenarioCalibrationHarnessTest" "-DargLine=-Xms4g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED" >> "%LOG_FILE%" 2>&1

echo ============================================================== >> "%LOG_FILE%"
echo   ✨ ETHER MASTER CLOUD CAMPAIGN FINISHED: %DATE% %TIME% >> "%LOG_FILE%"
echo ============================================================== >> "%LOG_FILE%"

echo [DONE] Calibration campaign finished with exit code %ERRORLEVEL%.
endlocal
