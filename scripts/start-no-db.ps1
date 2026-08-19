<#
 .SYNOPSIS
    Starts Ether JavaFX application in Database Offline Mode.
 .DESCRIPTION
    Runs the simulation GUI directly using in-memory state without spinning up PostgreSQL Docker container.
#>
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location "$ScriptDir\.."

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Ether Simulation - Quick Start" -ForegroundColor Green
Write-Host "(Running in Database Offline Mode)" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

mvn javafx:run
