@echo off
setlocal

REM ==============================================================================
REM Ether - Academic Paper & Scientific Model Falsification Test Harness
REM ==============================================================================
REM This script executes the peer-reviewed falsification suites formalizing
REM the 8 cliodynamic debates, Popperian counterfactual tests (Steampunk Rome 165 CE),
REM and the Great Man Theory contingency vs macro-determinism benchmarks.
REM ==============================================================================

cd /d "%~dp0"

echo ===============================================================================
echo      ETHER CLIODYNAMICS - ACADEMIC PAPER ^& SCIENTIFIC FALSIFICATION
echo ===============================================================================
echo.

set TARGET=%~1

if "%TARGET%"=="" goto SHOW_HELP
if /i "%TARGET%"=="--debates" goto RUN_DEBATES
if /i "%TARGET%"=="--steampunk" goto RUN_STEAMPUNK
if /i "%TARGET%"=="--great-man" goto RUN_GREAT_MAN
if /i "%TARGET%"=="--all" goto RUN_ALL
goto RUN_CUSTOM

:SHOW_HELP
echo [INFO] No parameter specified. Running the 8 Canonical Scientific Debates.
echo        Usage options:
echo          run_scientific_tests.bat                 [Runs 8 Debates / 24 Tests - ~10s]
echo          run_scientific_tests.bat --debates       [Runs 8 Debates / 24 Tests]
echo          run_scientific_tests.bat --steampunk     [Runs Steampunk Counterfactual Falsifier]
echo          run_scientific_tests.bat --great-man     [Runs Great Man Theory ^& Bifurcation Suite]
echo          run_scientific_tests.bat --all           [Runs All Academic Paper Suites]
echo.
goto RUN_DEBATES

:RUN_DEBATES
echo [RUN] Executing 8 Canonical Scientific Debates: ScientificModelFalsificationAndValidationSuite
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=ScientificModelFalsificationAndValidationSuite"
goto FINISH

:RUN_STEAMPUNK
echo [RUN] Executing Steampunk Rome 165 CE Counterfactual Falsifier: CounterfactualInverseFalsificationTest
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=CounterfactualInverseFalsificationTest"
goto FINISH

:RUN_GREAT_MAN
echo [RUN] Executing Great Man Theory Contingency Benchmarks
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test "-Dtest=HistoricalLeaderBifurcationTest,MasterHistoricalBifurcationSuite,VarianceDecompositionTest,TopologicalConvergenceTest"
goto FINISH

:RUN_ALL
echo [RUN 1/3] Executing 8 Canonical Scientific Debates...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=ScientificModelFalsificationAndValidationSuite"
if errorlevel 1 goto FAIL

echo [RUN 2/3] Executing Great Man Theory Contingency Benchmarks...
call mvn test "-Dtest=HistoricalLeaderBifurcationTest,MasterHistoricalBifurcationSuite,VarianceDecompositionTest,TopologicalConvergenceTest"
if errorlevel 1 goto FAIL

echo [RUN 3/3] Executing Steampunk Rome 165 CE Counterfactual Falsifier...
call mvn test -Pacademic-paper "-Dtest=CounterfactualInverseFalsificationTest"
if errorlevel 1 goto FAIL
goto FINISH

:RUN_CUSTOM
echo [CUSTOM] Executing custom test target: %TARGET%
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=%TARGET%"
goto FINISH

:FINISH
if errorlevel 1 goto FAIL
echo.
echo ===============================================================================
echo   [SUCCESS] SCIENTIFIC FALSIFICATION TESTS PASSED CLEANLY (0 REGRESSIONS)
echo ===============================================================================
exit /b 0

:FAIL
echo.
echo ===============================================================================
echo   [ERROR] SCIENTIFIC TEST FAILURE DETECTED. Review logs above.
echo ===============================================================================
exit /b 1
