@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ========================================
echo   Ether Simulation - Docker Teardown
echo ========================================
echo.

echo Stopping and removing Docker containers...
docker compose down 2>nul || docker-compose down

if %errorlevel% eq 0 (
    echo.
    echo [SUCCESS] PostgreSQL ^& Redis containers stopped and removed ✓
) else (
    echo.
    echo [ERROR] Failed to stop Docker containers.
)

echo.
echo ========================================
endlocal
