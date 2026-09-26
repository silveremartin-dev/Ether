@echo off
REM ==============================================================================
REM Ether -- Docker Compose Deployment Helper (Windows CMD)
REM
REM Usage:
REM   scripts\docker-deploy.bat [MODE] [SCENARIO] [TICKS] [CELLS] [WORKERS]
REM
REM MODE options:
REM   dev       -- Start only postgres + redis (local development, default)
REM   headless  -- Run a single batch simulation to completion then exit
REM   cluster   -- Start master + N worker nodes
REM
REM Examples:
REM   scripts\docker-deploy.bat
REM   scripts\docker-deploy.bat headless OUT_OF_AFRICA 1000 10000
REM   scripts\docker-deploy.bat cluster MESOPOTAMIA_BRONZE_AGE 500 5000 3
REM ==============================================================================

setlocal enabledelayedexpansion

set MODE=%~1
if "%MODE%"=="" set MODE=dev

set SCENARIO=%~2
if "%SCENARIO%"=="" set SCENARIO=OUT_OF_AFRICA

set TICKS=%~3
if "%TICKS%"=="" set TICKS=500

set CELLS=%~4
if "%CELLS%"=="" set CELLS=5000

set WORKERS=%~5
if "%WORKERS%"=="" set WORKERS=1

echo ==========================================================
echo      ETHER -- DOCKER DEPLOY HELPER
echo ==========================================================
echo   Mode       : %MODE%
echo   Scenario   : %SCENARIO%
echo   Ticks      : %TICKS%
echo   Cells      : %CELLS%
echo   Workers    : %WORKERS%
echo ----------------------------------------------------------

REM Ensure .env exists
if not exist ".env" (
    echo [!] .env file not found. Copying from .env.example...
    copy .env.example .env
    echo [!] Please review and edit .env before running in production.
)

if /i "%MODE%"=="dev" goto :dev
if /i "%MODE%"=="headless" goto :headless
if /i "%MODE%"=="cluster" goto :cluster
if /i "%MODE%"=="down" goto :down

echo ERROR: Unknown mode '%MODE%'. Valid: dev ^| headless ^| cluster ^| down
exit /b 1

:dev
echo [1/2] Starting infrastructure (postgres + redis)...
docker compose up -d postgres redis
echo.
echo [OK] Dev infrastructure is up.
echo   PostgreSQL : localhost:54320
echo   Redis      : localhost:6379
goto :end

:headless
echo [1/2] Starting infrastructure...
docker compose up -d postgres redis
echo [2/2] Running headless simulation...
set SCENARIO=%SCENARIO%
set TICKS=%TICKS%
set CELLS=%CELLS%
docker compose --profile headless up --build ether-headless
echo.
echo [OK] Simulation complete. Results saved to .\saves\
goto :end

:cluster
echo [1/2] Starting infrastructure...
docker compose up -d postgres redis
echo [2/2] Starting cluster (master + %WORKERS% workers)...
set SCENARIO=%SCENARIO%
set TICKS=%TICKS%
set CELLS=%CELLS%
docker compose --profile cluster up -d --build --scale ether-worker=%WORKERS%
echo.
echo [OK] Cluster is up.
echo   Master RPC : localhost:9090
echo   Workers    : %WORKERS% replica(s)
goto :end

:down
echo Stopping all Ether containers...
docker compose --profile headless --profile cluster down
echo [OK] All stopped.
goto :end

:end
endlocal
