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

:: 2. Launch Strategy
:: Priority A: Standalone release distribution (bin\ether.jar)
if exist "bin\ether.jar" (
    echo [INFO] Launching Ether standalone package...
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin\ether.jar %*
) else if exist "pom.xml" (
    :: Priority B: Development workspace -> Check Maven
    where mvn >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        if "%~1"=="--jar" (
            echo [INFO] Rebuilding executable JAR...
            call mvn clean package -DskipTests
            for %%F in (target\*executable.jar) do (
                java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "%%F" %*
                goto :done
            )
        ) else (
            echo [INFO] Compiling and running latest code via Maven (JavaFX)...
            call mvn javafx:run
        )
    ) else (
        :: Maven not in PATH -> Fall back to target JAR
        set "FOUND_JAR="
        for %%F in (target\*executable.jar) do set "FOUND_JAR=%%F"
        if defined FOUND_JAR (
            echo [INFO] Launching existing build: !FOUND_JAR!
            java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "!FOUND_JAR!" %*
        ) else (
            echo [ERROR] Maven not found and no executable JAR in target\.
            echo Please install Apache Maven or build the project.
            pause
        )
    )
) else (
    :: Priority C: Loose folder with target JAR
    set "FOUND_JAR="
    for %%F in (target\*executable.jar) do set "FOUND_JAR=%%F"
    if defined FOUND_JAR (
        java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "!FOUND_JAR!" %*
    ) else (
        echo [ERROR] No runnable artifact found.
        pause
    )
)
:done
endlocal
