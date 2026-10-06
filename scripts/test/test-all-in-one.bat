@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ===============================================================================
echo            ETHER SIMULATION ENGINE -- ALL-IN-ONE TEST HARNESS                  
echo ===============================================================================
echo.

set FAILURES=0

echo [1/4] Running Java Compilation and Unit Tests...
call mvn test -Dtest=*Test -DfailIfNoSpecifiedTests=false
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Unit tests encountered errors!
    set /a FAILURES+=1
) else (
    echo [PASS] Unit tests completed successfully.
)

echo.
echo [2/4] Running Master Historical Bifurcation Suite...
call mvn test -Dtest=MasterHistoricalBifurcationSuite -DfailIfNoSpecifiedTests=false
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Master Historical Bifurcation Suite failed!
    set /a FAILURES+=1
) else (
    echo [PASS] Master Historical Bifurcation Suite passed.
)

echo.
echo [3/4] Running Empirical Residual and Metastability Suite...
call mvn test -Dtest=EmpiricalResidualBifurcationTest -DfailIfNoSpecifiedTests=false
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Empirical Residual Suite failed!
    set /a FAILURES+=1
) else (
    echo [PASS] Empirical Residual Suite passed.
)

echo.
echo [4/4] Running Historical Leader A/B Falsification Suite...
call mvn test -Dtest=HistoricalLeaderBifurcationTest -DfailIfNoSpecifiedTests=false
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Historical Leader Suite failed!
    set /a FAILURES+=1
) else (
    echo [PASS] Historical Leader Suite passed.
)

echo.
echo ===============================================================================
if %FAILURES% equ 0 (
    echo [SUCCESS] ALL TEST SUITES PASSED CLEANLY (0 failures)!
    echo ===============================================================================
    exit /b 0
) else (
    echo [ERROR] %FAILURES% TEST SUITE(S) FAILED. Please review the logs above.
    echo ===============================================================================
    exit /b 1
)

endlocal
