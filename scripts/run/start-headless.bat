@echo off
rem ==============================================================================
rem Ether 1.0 — Start Headless Engine Batch Runner (Windows Command Prompt)
rem ==============================================================================

setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

set SCENARIO=%~1
if "%SCENARIO%"=="" set SCENARIO=OUT_OF_AFRICA

set TICKS=%~2
if "%TICKS%"=="" set TICKS=300

set CELLS=%~3
if "%CELLS%"=="" set CELLS=3000

echo ==========================================================
echo      ETHER -- HEADLESS ENGINE BATCH RUNNER                
echo ==========================================================
echo   Scenario Preset : %SCENARIO%
echo   Target Ticks    : %TICKS%
echo   H3 Grid Cells   : %CELLS%
echo ----------------------------------------------------------

set "JAR_PATH="
if exist "bin\ether.jar" (
    set "JAR_PATH=bin\ether.jar"
) else (
    for %%F in (target\*executable.jar) do set "JAR_PATH=%%F"
    if not defined JAR_PATH (
        for %%F in (target\society-simulation-*.jar) do (
            echo "%%F" | findstr /i "sources javadoc" >nul || set "JAR_PATH=%%F"
        )
    )
)

if not defined JAR_PATH (
    echo Building executable JAR...
    call mvn clean package "-Dmaven.test.skip=true"
    for %%F in (target\*executable.jar) do set "JAR_PATH=%%F"
)

echo Executing Headless Batch Run...
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "!JAR_PATH!" --headless --scenario="%SCENARIO%" --ticks=%TICKS% --cells=%CELLS% --profile

endlocal
