@echo off
cd /d "%~dp0.."
REM Ether Simulation - Database Status Check

echo ========================================
echo Database Status
echo ========================================
echo.

where docker-compose >nul 2>&1
if %errorlevel% equ 0 (
    docker-compose ps
) else (
    docker compose ps
)

echo.
echo ========================================
echo.
echo To view logs: docker compose logs -f postgres
echo.
pause
