# ==============================================================================
# Ether — Docker Compose Deployment Helper (Windows PowerShell)
#
# Usage:
#   .\scripts\docker-deploy.ps1 [Mode] [Scenario] [Ticks] [Cells] [Workers]
#
# Mode options:
#   dev       — Start only postgres + redis (local development, default)
#   headless  — Run a single batch simulation to completion then exit
#   cluster   — Start master + N worker nodes
#
# Examples:
#   .\scripts\docker-deploy.ps1
#   .\scripts\docker-deploy.ps1 headless OUT_OF_AFRICA 1000 10000
#   .\scripts\docker-deploy.ps1 cluster MESOPOTAMIA_BRONZE_AGE 500 5000 3
# ==============================================================================

param(
    [string]$Mode     = "dev",
    [string]$Scenario = "OUT_OF_AFRICA",
    [int]   $Ticks    = 500,
    [int]   $Cells    = 5000,
    [int]   $Workers  = 1
)

$ErrorActionPreference = 'Stop'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — DOCKER DEPLOY HELPER" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Mode       : $Mode"
Write-Host "  Scenario   : $Scenario"
Write-Host "  Ticks      : $Ticks"
Write-Host "  Cells      : $Cells"
Write-Host "  Workers    : $Workers"
Write-Host "----------------------------------------------------------"

# Ensure .env exists
if (-not (Test-Path ".env")) {
    Write-Host "[!] .env file not found. Copying from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "[!] Please review and edit .env before running in production." -ForegroundColor Yellow
}

# Export overrides for docker compose
$env:SCENARIO = $Scenario
$env:TICKS    = $Ticks
$env:CELLS    = $Cells

switch ($Mode) {
    "dev" {
        Write-Host "[1/2] Starting infrastructure (postgres + redis)..."
        docker compose up -d postgres redis
        Write-Host ""
        Write-Host "✅ Dev infrastructure is up." -ForegroundColor Green
        Write-Host "  PostgreSQL : localhost:54320"
        Write-Host "  Redis      : localhost:6379"
    }

    "headless" {
        Write-Host "[1/2] Starting infrastructure..."
        docker compose up -d postgres redis
        Write-Host "[2/2] Running headless simulation (scenario=$Scenario, ticks=$Ticks, cells=$Cells)..."
        docker compose --profile headless up --build ether-headless
        Write-Host ""
        Write-Host "✅ Simulation complete. Results saved to ./saves/" -ForegroundColor Green
    }

    "cluster" {
        Write-Host "[1/2] Starting infrastructure..."
        docker compose up -d postgres redis
        Write-Host "[2/2] Starting cluster (master + $Workers worker(s))..."
        docker compose --profile cluster up -d --build --scale "ether-worker=$Workers"
        Write-Host ""
        Write-Host "✅ Cluster is up." -ForegroundColor Green
        Write-Host "  Master RPC : localhost:9090"
        Write-Host "  Workers    : $Workers replica(s)"
        Write-Host ""
        Write-Host "  Monitor logs : docker compose --profile cluster logs -f"
        Write-Host "  Tear down    : docker compose --profile cluster down"
    }

    "down" {
        Write-Host "Stopping all Ether containers..."
        docker compose --profile headless --profile cluster down
        Write-Host "✅ All stopped." -ForegroundColor Green
    }

    default {
        Write-Error "Unknown mode '$Mode'. Valid: dev | headless | cluster | down"
        exit 1
    }
}
