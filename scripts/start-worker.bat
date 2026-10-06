@echo off
rem ==============================================================================
rem Ether 1.0 — Start Cluster Compute Worker Node (Windows Command Prompt)
rem ==============================================================================

setlocal enabledelayedexpansion

set MASTER_HOST=%~1
if "%MASTER_HOST%"=="" set MASTER_HOST=127.0.0.1

set PORT=%~2
if "%PORT%"=="" set PORT=9090

set SECRET=%~3
if "%SECRET%"=="" set SECRET=EtherClusterSecret2026

echo ==========================================================
echo      ETHER -- STARTING CLUSTER WORKER NODE (v1.0 b1)       
echo ==========================================================
echo   Master Host : %MASTER_HOST%
echo   Port        : %PORT%
echo ----------------------------------------------------------

set JAR_PATH=target\society-simulation-1.0.0-beta.2-executable.jar

if not exist "%JAR_PATH%" (
    echo Building executable JAR...
    call mvn clean package "-Dmaven.test.skip=true"
)

echo Connecting Worker Node to Master at %MASTER_HOST%:%PORT%...
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "%JAR_PATH%" --headless --mode=cluster --role=worker --master-host="%MASTER_HOST%" --port=%PORT% --secret="%SECRET%"

endlocal
