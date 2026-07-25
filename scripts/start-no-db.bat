@echo off
cd /d "%~dp0.."
REM Ether Simulation - Quick Start (No Database)
REM Launches application without database

echo ========================================
echo Ether Simulation - Quick Start
echo (Running without database)
echo ========================================
echo.

mvn javafx:run

pause
