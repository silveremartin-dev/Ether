@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ============================================================
echo   ETHER -- DATABASE ^& DOCKER INFRASTRUCTURE STATUS
echo ============================================================
echo.

REM 1. Check Docker Daemon
docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [STATUS] Docker Daemon : [OFFLINE / NOT RUNNING]
    echo.
    echo Please launch Docker Desktop to inspect containers.
    echo ============================================================
    exit /b 1
)
echo [STATUS] Docker Daemon : [ONLINE]

REM 2. Container Status
echo.
echo [CONTAINERS]
docker compose ps 2>nul || docker-compose ps

REM 3. Port & Service Checks
echo.
echo [SERVICE HEALTH CHECKS]

REM Check PostgreSQL
docker compose exec -T postgres pg_isready -U ether >nul 2>&1 || docker-compose exec -T postgres pg_isready -U ether >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo   - PostgreSQL (Port 5432/54320) : [ONLINE / READY]
) else (
    echo   - PostgreSQL (Port 5432/54320) : [OFFLINE / UNREACHABLE]
)

REM Check Redis
docker compose exec -T redis redis-cli ping >nul 2>&1 || docker-compose exec -T redis redis-cli ping >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo   - Redis      (Port 6379)       : [ONLINE / PONG]
) else (
    echo   - Redis      (Port 6379)       : [OFFLINE / UNREACHABLE]
)

echo.
echo ============================================================
echo Useful commands:
echo   View Postgres logs : docker compose logs -f postgres
echo   View Redis logs    : docker compose logs -f redis
echo   Start services     : scripts\run\docker-deploy.bat dev
echo   Stop services      : scripts\run\stop-docker.bat
echo ============================================================

endlocal