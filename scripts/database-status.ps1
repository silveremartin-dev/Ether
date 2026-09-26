# ==============================================================================
# Ether 1.0 — Database Status Check (Windows / Cross-platform PowerShell)
# ==============================================================================

Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Database Container Status" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

docker-compose ps
Write-Host ""
Write-Host "To view logs: docker-compose logs -f postgres" -ForegroundColor Cyan
