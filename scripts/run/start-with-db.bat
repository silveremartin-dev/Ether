@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ========================================
echo Ether Simulation - Starting with DB...
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/4] Docker daemon is running ✓
echo.

REM Start PostgreSQL with Docker Compose
echo [2/4] Starting PostgreSQL database...
docker compose up -d postgres 2>nul || docker-compose up -d postgres

if %errorlevel% neq 0 (
    echo [ERROR] Failed to start database!
    pause
    exit /b 1
)

echo       Database container started ✓
echo.

REM Wait for PostgreSQL to be ready
echo [3/4] Waiting for database to be ready...
:wait_loop
docker compose exec -T postgres pg_isready -U ether >nul 2>&1 || docker-compose exec -T postgres pg_isready -U ether >nul 2>&1
if %errorlevel% neq 0 (
    echo       Still waiting for PostgreSQL...
    timeout /t 2 /nobreak >nul
    goto wait_loop
)

echo       Database is ready ✓
echo.

REM Locate or build executable JAR
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

REM Start the JavaFX application
echo [4/4] Launching Ether Simulation with Native SIMD Vectorization...
echo.

java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "!FOUND_JAR!" %*
if %ERRORLEVEL% neq 0 (
    java -Xms2g -Xmx8g -jar "!FOUND_JAR!" %*
)

REM Cleanup message
echo.
echo ========================================
echo Application closed.
echo [5/5] Stopping Database...
docker compose stop postgres 2>nul || docker-compose stop postgres
echo Database stopped.
echo ========================================

endlocal
