@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ========================================
echo Ether Simulation - Quick Start
echo (Running without database)
echo ========================================
echo.

set "FOUND_JAR="
if exist "bin\ether.jar" (
    set "FOUND_JAR=bin\ether.jar"
) else (
    for %%F in (target\*executable.jar) do set "FOUND_JAR=%%F"
    if not defined FOUND_JAR (
        for %%F in (target\society-simulation-*.jar) do (
            echo "%%F" | findstr /i "sources javadoc" >nul || set "FOUND_JAR=%%F"
        )
    )
)

if not defined FOUND_JAR (
    echo Building optimized executable JAR...
    call mvn clean package -DskipTests
    for %%F in (target\*executable.jar) do set "FOUND_JAR=%%F"
)

echo Launching Ether with Native SIMD Vectorization ^& G1GC...
java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "!FOUND_JAR!" %*
if %ERRORLEVEL% neq 0 (
    java -Xms2g -Xmx8g -jar "!FOUND_JAR!" %*
)

endlocal
