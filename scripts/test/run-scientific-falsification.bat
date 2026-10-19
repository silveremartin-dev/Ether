@echo off
setlocal enabledelayedexpansion
REM ==============================================================================
REM Ether - Scientific Model Falsification & Epistemic Validation Suite Runner
REM ==============================================================================
cd /d "%~dp0..\.."

echo ==============================================================
echo   🔬 ETHER SCIENTIFIC MODEL FALSIFICATION SUITE
echo ==============================================================
echo [INFO] Executing all 24 cliodynamic and epistemic debate benchmarks...
echo.

set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=ScientificModelFalsificationAndValidationSuite" %*

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================================
    echo   ✅ ALL SCIENTIFIC DEBATES PASSED (24/24 FALSIFICATION CHECKS)
    echo ==============================================================
) else (
    echo.
    echo ==============================================================
    echo   ❌ SCIENTIFIC FALSIFICATION REGRESSION DETECTED
    echo ==============================================================
)

exit /b %ERRORLEVEL%
