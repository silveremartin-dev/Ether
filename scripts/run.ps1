# ==============================================================================
# Ether 1.0 — Run Simulation UI (Windows / Cross-platform PowerShell)
# ==============================================================================

Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "Starting Ether Society Simulation..." -ForegroundColor Cyan
mvn javafx:run
