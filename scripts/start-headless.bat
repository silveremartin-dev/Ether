@echo off
rem ==============================================================================
rem Ether 1.0 — Start Headless Engine Batch Runner (Windows Command Prompt)
rem ==============================================================================

setlocal enabledelayedexpansion

set SCENARIO=%~1
if "%SCENARIO%"=="" set SCENARIO=OUT_OF_AFRICA

set TICKS=%~2
if "%TICKS%"=="" set TICKS=300

set CELLS=%~3
if "%CELLS%"=="" set CELLS=3000

echo ==========================================================
echo      ETHER -- HEADLESS ENGINE BATCH RUNNER (v1.0 b1)      
echo ==========================================================
echo   Scenario Preset : %SCENARIO%
echo   Target Ticks    : %TICKS%
echo   H3 Grid Cells   : %CELLS%
echo ----------------------------------------------------------

set JAR_PATH=target\society-simulation-1.0.0-beta.2-executable.jar

if not exist "%JAR_PATH%" (
    echo Building executable JAR...
    call mvn clean package "-Dmaven.test.skip=true"
)

echo Executing Headless Batch Run...
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "%JAR_PATH%" --headless --scenario="%SCENARIO%" --ticks=%TICKS% --cells=%CELLS% --profile

endlocal
