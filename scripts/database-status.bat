@echo off
cd /d "%~dp0.."
REM Ether Simulation - Database Status Check

echo ========================================
echo Database Status
echo ========================================
echo.

docker-compose ps

echo.
echo ========================================
echo.
echo To view logs: docker-compose logs -f postgres
echo.
pause
