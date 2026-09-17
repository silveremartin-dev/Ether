@echo off
setlocal
cd /d "%~dp0"
title Ether - Instant Setup & Launcher

echo ============================================================
echo   ETHER - Physicalist Planetary Cliodynamics Engine
echo ============================================================
echo.

:: 1. Check for Java 21+
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] Java runtime was not found in your PATH.
    echo Opening OpenJDK 21+ download page...
    start https://adoptium.net/
    echo.
    echo Please install JDK 21 or higher, then run this script again.
    pause
    exit /b 1
)

echo [OK] Java detected:
java -version
echo.

:: 2. Check if shaded JAR is built; if not, build or run via Maven
if exist "target\society-simulation-1.0.0-beta.1-executable.jar" (
    echo [INFO] Launching standalone executable JAR...
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar target\society-simulation-1.0.0-beta.1-executable.jar %*
) else (
    where mvn >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [INFO] Building and starting Ether via Maven...
        call mvn javafx:run
    ) else (
        echo [ERROR] Maven not found and executable JAR not built yet.
        echo Please run 'scripts\package_release.ps1' or install Maven.
        pause
    )
)
endlocal
