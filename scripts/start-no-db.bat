@echo off
cd /d "%~dp0.."
REM Ether Simulation - Quick Start (No Database)
REM Launches application without database

echo ========================================
echo Ether Simulation - Quick Start
echo (Running without database)
echo ========================================
echo.

if not exist "target\society-simulation-1.0.0-beta.2-executable.jar" (
    echo Building optimized executable JAR...
    call mvn clean package -DskipTests
)

echo Launching Ether with Native SIMD Vectorization ^& G1GC...
java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar target\society-simulation-1.0.0-beta.2-executable.jar

pause
