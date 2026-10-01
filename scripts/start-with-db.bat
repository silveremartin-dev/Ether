@echo off
cd /d "%~dp0.."
REM Ether Simulation - Startup Script
REM Starts PostgreSQL database and JavaFX application

echo ========================================
echo Ether Simulation - Starting...
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

echo [1/4] Docker is running ✓
echo.

REM Start PostgreSQL with Docker Compose
echo [2/4] Starting PostgreSQL database...
docker-compose up -d

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
docker-compose exec -T postgres pg_isready -U ether >nul 2>&1
if %errorlevel% neq 0 (
    echo       Still waiting...
    timeout /t 2 /nobreak >nul
    goto wait_loop
)

echo       Database is ready ✓
echo.

REM Start the JavaFX application
echo [4/4] Launching Ether Simulation...
echo.

mvn javafx:run

REM Cleanup message
echo.
echo ========================================
echo Application closed.
echo [5/5] Stopping Database...
docker-compose stop
echo Database stopped.
echo ========================================
pause
