# ==============================================================================
# Ether 1.0 — Docker + App Launcher (Windows / Cross-platform PowerShell)
# ==============================================================================

Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Ether Simulation - Starting with DB..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "[ERROR] Docker is not installed or not in PATH!"
    exit 1
}

Write-Host "[1/4] Docker check passed" -ForegroundColor Green
Write-Host "[2/4] Starting PostgreSQL database..." -ForegroundColor Yellow
docker-compose up -d

Write-Host "[3/4] Waiting for database readiness..." -ForegroundColor Yellow
while ($true) {
    docker-compose exec -T postgres pg_isready -U ether 2>$null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 2
}

Write-Host "      Database is ready" -ForegroundColor Green
Write-Host "[4/4] Launching Ether Simulation..." -ForegroundColor Cyan
mvn javafx:run

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Application closed. Stopping Database..." -ForegroundColor Yellow
docker-compose stop
Write-Host "Database container stopped." -ForegroundColor Green
