@echo off
rem ==============================================================================
rem Ether Planetary Simulation Engine — 1-Click Master Benchmark Suite (Windows)
rem Runs the complete 5-axis empirical benchmark suite (Spatial, Temporal, Backends,
rem Physics Tiers, Historical Epochs) and prints the comprehensive report matrix.
rem ==============================================================================

setlocal EnableDelayedExpansion
cd /d "%~dp0\..\.."

echo ==============================================================================
echo   ETHER PLANETARY SIMULATION ENGINE — 1-CLICK 5-AXIS BENCHMARK SUITE
echo ==============================================================================
echo Working Directory: %CD%

rem Verify JAR exists or trigger build
if not exist "target\society-simulation-1.0.0-beta.2-executable.jar" (
    echo [*] Executable JAR not found. Building shaded JAR with Maven...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [ERROR] Maven build failed.
        exit /b 1
    )
)

echo [*] Launching 5-Axis Real Planetary Simulation Engine Benchmarks...
java -XX:+UseG1GC -Xms8g -Xmx16g --enable-native-access=ALL-UNNAMED -cp "target\society-simulation-1.0.0-beta.2-executable.jar" org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner %*

echo.
echo [*] Benchmark execution completed successfully.
pause
