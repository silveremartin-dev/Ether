@echo off
rem ==============================================================================
rem Ether 1.0 — Package Release Distribution (Windows Command Prompt)
rem ==============================================================================

setlocal enabledelayedexpansion

set VERSION=%~1
if "%VERSION%"=="" set VERSION=1.0.0-beta.1

set ROOT_DIR=%~dp0..
set DIST_DIR=%ROOT_DIR%\dist
set PACKAGE_NAME=Ether-v%VERSION%-standalone
set TARGET_DIR=%DIST_DIR%\%PACKAGE_NAME%

echo ============================================================
echo   Packaging Ether Planetary Simulation v%VERSION%
echo ============================================================

echo [1/4] Building shaded executable JAR via Maven...
cd /d "%ROOT_DIR%"
call mvn clean package "-Dmaven.test.skip=true"

echo [2/4] Preparing output directory: %TARGET_DIR%...
if exist "%TARGET_DIR%" rmdir /s /q "%TARGET_DIR%"
mkdir "%TARGET_DIR%\bin" "%TARGET_DIR%\data" "%TARGET_DIR%\docs" "%TARGET_DIR%\saves" "%TARGET_DIR%\logs"

echo [3/4] Copying artifacts and resources...
copy "%ROOT_DIR%\target\society-simulation-1.0.0-beta.1-executable.jar" "%TARGET_DIR%\bin\ether.jar" >nul 2>&1
if exist "%ROOT_DIR%\data" xcopy /E /I /Y "%ROOT_DIR%\data" "%TARGET_DIR%\data" >nul 2>&1
copy "%ROOT_DIR%\README.md" "%TARGET_DIR%\" >nul 2>&1
copy "%ROOT_DIR%\LICENSE" "%TARGET_DIR%\" >nul 2>&1
copy "%ROOT_DIR%\AGENT.md" "%TARGET_DIR%\" >nul 2>&1
if exist "%ROOT_DIR%\docs" xcopy /E /I /Y "%ROOT_DIR%\docs" "%TARGET_DIR%\docs" >nul 2>&1

(
echo @echo off
echo java --add-modules=jdk.incubator.vector -Xms2g -Xmx8g -jar bin\ether.jar %%*
) > "%TARGET_DIR%\run.bat"

echo [4/4] Release structure ready in: %TARGET_DIR%
echo ============================================================
echo   Package created successfully: %TARGET_DIR%
echo ============================================================

endlocal
