@echo off
cd /d "%~dp0.."
REM Ether Simulation - Stop Script
REM Stops the PostgreSQL database

echo ========================================
echo Ether Simulation - Shutting Down...
echo ========================================
echo.

echo Stopping PostgreSQL database...
where docker-compose >nul 2>&1
if %errorlevel% equ 0 (
    docker-compose down
) else (
    docker compose down
)

if %errorlevel% eq 0 (
    echo.
    echo Database stopped successfully ✓
) else (
    echo.
    echo [ERROR] Failed to stop database
)

echo.
echo ========================================
pause
