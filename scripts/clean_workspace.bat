@echo off
REM ==============================================================================
REM Ether - Workspace Cleanup Utility Batch Wrapper
REM ==============================================================================

setlocal
set SCRIPT_DIR=%~dp0
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%clean_workspace.ps1" %*
endlocal
