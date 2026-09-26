# ==============================================================================
# Ether 1.0 — Javadoc Generation (Windows / Cross-platform PowerShell)
# ==============================================================================

Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "Generating Javadoc..." -ForegroundColor Cyan
mvn javadoc:javadoc
Write-Host "Javadoc generation complete." -ForegroundColor Green
