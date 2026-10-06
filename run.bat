@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title Ether Planetary Simulation

echo ============================================================
echo   ETHER - Planetary Cliodynamics Simulation Engine
echo ============================================================
echo.

:: 1. Verify Java Installation
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] Java 21+ is not installed or not in your system PATH.
    echo.
    echo Opening OpenJDK 21 download page in your browser...
    start https://adoptium.net/temurin/releases/?version=21
    echo.
    echo Once installed, double-click this file again to start Ether.
    echo.
    pause
    exit /b 1
)

:: 2. Launch using executable jar if present, else launch via Maven
if exist "bin\ether.jar" (
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin\ether.jar %*
) else if exist "target\society-simulation-1.0.0-beta.2-executable.jar" (
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar target\society-simulation-1.0.0-beta.2-executable.jar %*
) else (
    set "FOUND_JAR="
    for %%F in (target\*executable.jar) do (
        set "FOUND_JAR=%%F"
    )
    if defined FOUND_JAR (
        java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "!FOUND_JAR!" %*
    ) else (
        where mvn >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo [INFO] Running via Maven...
            call mvn javafx:run
        ) else (
            echo [ERROR] Could not find executable JAR or Maven.
            echo Please build the project first via 'mvn clean package'.
            pause
        )
    )
)
endlocal
