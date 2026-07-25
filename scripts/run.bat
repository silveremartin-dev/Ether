@echo off
cd /d "%~dp0.."
echo Starting Ether Society Simulation...
call mvn exec:java
pause
