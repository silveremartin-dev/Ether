<#
 .SYNOPSIS
    Starts Ether Distributed Compute Master Node Server.
 .DESCRIPTION
    Launches Ether in Cluster Master Orchestrator mode, listening for incoming worker nodes,
    dispatching scenarios, and managing spatial H3 cell partitioning.
 .PARAMETER Scenario
    Historical or procedural scenario preset (e.g., OUT_OF_AFRICA, CLASSICAL, INDUSTRIAL, NEOLITHIZATION).
 .PARAMETER Port
    TCP / gRPC cluster communication port (Default: 9090).
 .PARAMETER Secret
    AES-256 GCM cluster authentication secret token.
 .PARAMETER Ticks
    Number of simulation ticks to execute.
#>
param (
    [string]$Scenario = "OUT_OF_AFRICA",
    [int]$Port = 9090,
    [string]$Secret = "EtherClusterSecret2026",
    [int]$Ticks = 500,
    [int]$Cells = 10000
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER 2.0 — STARTING CLUSTER MASTER SERVER           " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Scenario Preset : $Scenario" -ForegroundColor Yellow
Write-Host "  Port            : $Port" -ForegroundColor Yellow
Write-Host "  Target Ticks    : $Ticks" -ForegroundColor Yellow
Write-Host "  H3 Grid Cells   : $Cells" -ForegroundColor Yellow
Write-Host "----------------------------------------------------------"

$JAR_PATH = "target/society-simulation-2.0.0-SNAPSHOT-jar-with-dependencies.jar"

if (-not (Test-Path $JAR_PATH)) {
    Write-Host "🔨 Building executable JAR..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}

Write-Host "🚀 Launching Master Node Server..." -ForegroundColor Green
java -jar $JAR_PATH --mode=cluster --role=master --port=$Port --secret=$Secret --scenario=$Scenario --ticks=$Ticks --cells=$Cells --profile
