@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ========================================
echo Ether Simulation - Quick Start
echo (Running without database)
echo ========================================
echo.

if "%~1"=="--rebuild" (
    echo Forcing clean rebuild of executable JAR...
    call mvn clean package -DskipTests
)

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

set "NEED_BUILD=0"
if not defined FOUND_JAR (
    set "NEED_BUILD=1"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$j = Get-ChildItem -Path target\*executable.jar -ErrorAction SilentlyContinue | Select-Object -First 1; if (-not $j) { exit 1 }; $s = (Get-ChildItem -Path src,pom.xml -Recurse | Measure-Object -Property LastWriteTime -Maximum).Maximum; if ($s -gt $j.LastWriteTime) { exit 1 } else { exit 0 }"
    if !ERRORLEVEL! neq 0 set "NEED_BUILD=1"
)

if "!NEED_BUILD!"=="1" (
    echo ========================================================
    echo [AUTO-BUILD] Source files updated since last build.
    echo Recompiling executable JAR to include latest changes...
    echo ========================================================
    call mvn package -DskipTests
    set "FOUND_JAR="
    for %%F in (target\*executable.jar) do set "FOUND_JAR=%%F"
)

echo Launching Ether with Native SIMD Vectorization ^& G1GC...
java -Dfile.encoding=UTF-8 --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "!FOUND_JAR!" %*
if %ERRORLEVEL% neq 0 (
    java -Dfile.encoding=UTF-8 -Xms2g -Xmx8g -jar "!FOUND_JAR!" %*
)

endlocal
