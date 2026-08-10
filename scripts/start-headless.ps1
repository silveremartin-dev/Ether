<#
 .SYNOPSIS
    Starts Ether Engine in pure Headless CLI Mode.
 .DESCRIPTION
    Executes high-throughput batch simulation without JavaFX UI overhead.
 .PARAMETER Scenario
    Historical or procedural scenario preset (e.g., OUT_OF_AFRICA, CLASSICAL, INDUSTRIAL, NEOLITHIZATION).
 .PARAMETER Ticks
    Number of simulation ticks to run (Default: 300).
 .PARAMETER Cells
    Number of H3 grid cells for benchmark (Default: 3000).
#>
param (
    [string]$Scenario = "OUT_OF_AFRICA",
    [int]$Ticks = 300,
    [int]$Cells = 3000
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER 2.0 — HEADLESS ENGINE BATCH RUNNER             " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Scenario Preset : $Scenario" -ForegroundColor Yellow
Write-Host "  Target Ticks    : $Ticks" -ForegroundColor Yellow
Write-Host "  H3 Grid Cells   : $Cells" -ForegroundColor Yellow
Write-Host "----------------------------------------------------------"

$JAR_PATH = "target/society-simulation-2.0.0-SNAPSHOT-jar-with-dependencies.jar"

if (-not (Test-Path $JAR_PATH)) {
    Write-Host "🔨 Building executable JAR..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}

Write-Host "🚀 Executing Headless Batch Run..." -ForegroundColor Green
java -jar $JAR_PATH --scenario=$Scenario --ticks=$Ticks --cells=$Cells --profile
