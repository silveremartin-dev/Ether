@echo off
setlocal

REM ==============================================================================
REM Ether - Academic Paper & Scientific Model Falsification Test Harness
REM ==============================================================================
REM This script executes the peer-reviewed falsification suites formalizing
REM the 8 cliodynamic debates, Popperian counterfactual tests (Steampunk Rome 165 CE),
REM Great Man Theory contingency vs macro-determinism, multiscale isolation & convergence,
REM empirical ground truth benchmarks, Bayesian inverse inference, and formal verification.
REM All test classes are consolidated in the dedicated package org.ether.society.falsification.*
REM ==============================================================================

cd /d "%~dp0"

echo ===============================================================================
echo      ETHER CLIODYNAMICS - ACADEMIC PAPER ^& SCIENTIFIC FALSIFICATION
echo ===============================================================================
echo.

set TARGET=%~1

if "%TARGET%"=="" goto SHOW_HELP
if /i "%TARGET%"=="--formal" goto RUN_FORMAL
if /i "%TARGET%"=="--multiscale" goto RUN_MULTISCALE
if /i "%TARGET%"=="--inference" goto RUN_INFERENCE
if /i "%TARGET%"=="--debates" goto RUN_DEBATES
if /i "%TARGET%"=="--contingency" goto RUN_CONTINGENCY
if /i "%TARGET%"=="--great-man" goto RUN_CONTINGENCY
if /i "%TARGET%"=="--counterfactual" goto RUN_COUNTERFACTUAL
if /i "%TARGET%"=="--steampunk" goto RUN_COUNTERFACTUAL
if /i "%TARGET%"=="--benchmark" goto RUN_BENCHMARK
if /i "%TARGET%"=="--benchmarks" goto RUN_BENCHMARK
if /i "%TARGET%"=="--all" goto RUN_ALL
goto RUN_CUSTOM

:SHOW_HELP
echo [INFO] Usage options (Organized in 7 Bottom-Up Epistemological Pillars):
echo          run_scientific_tests.bat --formal        [Pillar 1: Formal Invariants ^& Physical Conservation (Tier 1)]
echo          run_scientific_tests.bat --multiscale    [Pillar 2: Multiscale Discretization, Fénétrange ^& Isolation]
echo          run_scientific_tests.bat --inference     [Pillar 3: Bayesian ABC-SMC, Identifiability ^& Sobol Sensitivity]
echo          run_scientific_tests.bat --debates       [Pillar 4: 8 Canonical Cliodynamic Debates (Tier 2)]
echo          run_scientific_tests.bat --contingency   [Pillar 5: Historical Contingency ^& Great Man Theory Falsification]
echo          run_scientific_tests.bat --counterfactual [Pillar 6: Popperian Negative Controls (Steampunk Rome 165 CE)]
echo          run_scientific_tests.bat --benchmark     [Pillar 7: Deep-Time Empirical Historical Benchmarks]
echo          run_scientific_tests.bat --all           [Runs All 7 Pillars sequentially in org.ether.society.falsification.*]
echo.
goto RUN_FORMAL

:RUN_FORMAL
echo [RUN] Executing Pillar 1: Formal Invariants ^& Physical Conservation (org.ether.society.falsification.formal.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
goto FINISH

:RUN_MULTISCALE
echo [RUN] Executing Pillar 2: Multiscale Spatiotemporal Discretization ^& Domain Truncation (org.ether.society.falsification.multiscale.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
goto FINISH

:RUN_INFERENCE
echo [RUN] Executing Pillar 3: Bayesian Inverse Inference ^& Global Identifiability (org.ether.society.falsification.inference.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
goto FINISH

:RUN_DEBATES
echo [RUN] Executing Pillar 4: The 8 Canonical Historiographical Debates (org.ether.society.falsification.debates.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
goto FINISH

:RUN_CONTINGENCY
echo [RUN] Executing Pillar 5: Historical Contingency ^& Macro-Determinism (org.ether.society.falsification.contingency.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
goto FINISH

:RUN_COUNTERFACTUAL
echo [RUN] Executing Pillar 6: Popperian Falsification ^& Negative Controls (org.ether.society.falsification.counterfactual.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
goto FINISH

:RUN_BENCHMARK
echo [RUN] Executing Pillar 7: Deep-Time Empirical Historical Benchmarks (org.ether.society.falsification.benchmark.*)
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
goto FINISH

:RUN_ALL
echo [RUN 1/7] Executing Pillar 1: Formal Invariants ^& Physical Conservation...
set MAVEN_OPTS=--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
if errorlevel 1 goto FAIL

echo [RUN 2/7] Executing Pillar 2: Multiscale Spatiotemporal Discretization ^& Isolation...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
if errorlevel 1 goto FAIL

echo [RUN 3/7] Executing Pillar 3: Bayesian Inverse Inference ^& Identifiability...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
if errorlevel 1 goto FAIL

echo [RUN 4/7] Executing Pillar 4: The 8 Canonical Historiographical Debates...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
if errorlevel 1 goto FAIL

echo [RUN 5/7] Executing Pillar 5: Historical Contingency ^& Macro-Determinism...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
if errorlevel 1 goto FAIL

echo [RUN 6/7] Executing Pillar 6: Popperian Falsification ^& Negative Controls...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
if errorlevel 1 goto FAIL

echo [RUN 7/7] Executing Pillar 7: Deep-Time Empirical Historical Benchmarks...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
if errorlevel 1 goto FAIL

echo [RUN 3/7] Executing Pillar 3: Great Man Contingency vs Determinism...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
if errorlevel 1 goto FAIL

echo [RUN 4/7] Executing Pillar 4: Multiscale Spatio-Temporal Invariance ^& Isolation...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
if errorlevel 1 goto FAIL

echo [RUN 5/7] Executing Pillar 5: Empirical Ground-Truth Benchmarks...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
if errorlevel 1 goto FAIL

echo [RUN 6/7] Executing Pillar 6: Bayesian Inverse Inference ^& Sobol Sensitivity...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
if errorlevel 1 goto FAIL

echo [RUN 7/7] Executing Pillar 7: Offline Formal Verification ^& Physical Invariants...
call mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
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
echo   [ERROR] SCIENTIFIC FALSIFICATION TEST SUITE FAILED. Review logs above.
echo ===============================================================================
exit /b 1
