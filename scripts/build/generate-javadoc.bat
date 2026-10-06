@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ============================================================
echo   ETHER -- JAVADOC API DOCUMENTATION GENERATOR
echo ============================================================
echo.

echo [1/2] Invoking Maven Javadoc Plugin...
call mvn javadoc:javadoc -Dshow=protected -Dquiet=true

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Javadoc generation failed! Check compiler logs.
    exit /b 1
)

echo [2/2] Verifying generated API documentation...
if exist "target\site\apidocs\index.html" (
    echo.
    echo ============================================================
    echo  [SUCCESS] Javadoc successfully generated!
    echo  Index File: %CD%\target\site\apidocs\index.html
    echo ============================================================
) else (
    echo [WARN] Javadoc output file not found in target\site\apidocs\index.html
)

endlocal
