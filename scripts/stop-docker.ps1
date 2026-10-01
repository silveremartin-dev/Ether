# ==============================================================================
# Ether 1.0 — Stop Database Container (Windows / Cross-platform PowerShell)
# ==============================================================================

Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Ether Simulation - Shutting Down DB..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

docker-compose down
Write-Host "Database container shut down." -ForegroundColor Green
