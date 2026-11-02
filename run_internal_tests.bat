@echo off
setlocal

REM ==============================================================================
REM Ether - Internal Development Test Suite Runner
REM ==============================================================================
REM This script executes internal unit, integration, and kernel tests,
REM safely isolated from the heavy academic/scientific falsification benchmarks.
REM ==============================================================================

cd /d "%~dp0"

echo ===============================================================================
echo            ETHER SIMULATION ENGINE -- INTERNAL TEST HARNESS
echo ===============================================================================
echo.

set TARGET=%~1

if "%TARGET%"=="" goto SHOW_HELP
if /i "%TARGET%"=="--tier1" goto RUN_TIER1
if /i "%TARGET%"=="--tier2" goto RUN_TIER2
if /i "%TARGET%"=="--core" goto RUN_CORE
if /i "%TARGET%"=="--all" goto RUN_ALL
goto RUN_CUSTOM

:SHOW_HELP
echo [INFO] No parameter specified. Running standard internal test suite.
echo        Usage options:
echo          run_internal_tests.bat                 [Runs all internal tests]
echo          run_internal_tests.bat --tier1         [Runs Tier 1 Biophysics ^& Conservation kernels]
echo          run_internal_tests.bat --tier2         [Runs Tier 2 Cliodynamic ^& Historical engines]
echo          run_internal_tests.bat --core          [Runs Core DoD, Panama WorldBuffer ^& Integrators]
echo          run_internal_tests.bat --all           [Runs all non-academic tests]
echo.
goto RUN_ALL

:RUN_TIER1
echo [RUN] Executing Tier 1 Biophysics ^& Conservation Kernels...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=org.ether.society.engines.tier1.**.*Test"
goto FINISH

:RUN_TIER2
echo [RUN] Executing Tier 2 Cliodynamic Theories ^& Historical Engines...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=org.ether.society.engines.tier2.**.*Test"
goto FINISH

:RUN_CORE
echo [RUN] Executing Core DoD, Panama WorldBuffer ^& Symplectic Integrator Tests...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=org.ether.society.core.**.*Test"
goto FINISH

:RUN_ALL
echo [RUN] Executing all internal test suites [excluding heavy academic/benchmarks]...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test
goto FINISH

:RUN_CUSTOM
echo [CUSTOM] Executing custom internal test target: %TARGET%
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=%TARGET%"
goto FINISH

:FINISH
if errorlevel 1 goto FAIL
echo.
echo ===============================================================================
echo   [SUCCESS] ALL INTERNAL TESTS PASSED CLEANLY (0 REGRESSIONS)
echo ===============================================================================
exit /b 0

:FAIL
echo.
echo ===============================================================================
echo   [ERROR] INTERNAL TEST FAILURE DETECTED. Review logs above.
echo ===============================================================================
exit /b 1
