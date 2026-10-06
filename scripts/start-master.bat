@echo off
rem ==============================================================================
rem Ether 1.0 — Start Cluster Master Server (Windows Command Prompt)
rem ==============================================================================

setlocal enabledelayedexpansion

set SCENARIO=%~1
if "%SCENARIO%"=="" set SCENARIO=OUT_OF_AFRICA

set PORT=%~2
if "%PORT%"=="" set PORT=9090

set SECRET=%~3
if "%SECRET%"=="" set SECRET=EtherClusterSecret2026

set TICKS=%~4
if "%TICKS%"=="" set TICKS=500

set CELLS=%~5
if "%CELLS%"=="" set CELLS=10000

echo ==========================================================
echo      ETHER -- STARTING CLUSTER MASTER SERVER (v1.0 b1)     
echo ==========================================================
echo   Scenario Preset : %SCENARIO%
echo   Port            : %PORT%
echo   Target Ticks    : %TICKS%
echo   H3 Grid Cells   : %CELLS%
echo ----------------------------------------------------------

set JAR_PATH=target\society-simulation-1.0.0-beta.2-executable.jar

if not exist "%JAR_PATH%" (
    echo Building executable JAR...
    call mvn clean package "-Dmaven.test.skip=true"
)

echo Launching Master Node Server...
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "%JAR_PATH%" --headless --mode=cluster --role=master --port=%PORT% --secret="%SECRET%" --scenario="%SCENARIO%" --ticks=%TICKS% --cells=%CELLS% --profile

endlocal
