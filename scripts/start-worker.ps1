<#
 .SYNOPSIS
    Starts Ether Cluster Compute Worker Node.
 .DESCRIPTION
    Connects to an existing Master Cluster Orchestrator, receives spatial H3 cell chunk assignment,
    and executes parallel simulation ticks.
 .PARAMETER MasterHost
    IP address or hostname of the Master Cluster Server (Default: 127.0.0.1).
 .PARAMETER Port
    TCP / gRPC cluster communication port (Default: 9090).
 .PARAMETER Secret
    AES-256 GCM cluster authentication secret token.
#>
param (
    [string]$MasterHost = "127.0.0.1",
    [int]$Port = 9090,
    [string]$Secret = "EtherClusterSecret2026"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER -- STARTING CLUSTER WORKER NODE (v1.0 b1)      " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Master Host : $MasterHost" -ForegroundColor Yellow
Write-Host "  Port        : $Port" -ForegroundColor Yellow
Write-Host "----------------------------------------------------------"

$JAR_PATH = "target/society-simulation-1.0.0-beta.1-executable.jar"

if (-not (Test-Path $JAR_PATH)) {
    Write-Host "🔨 Building executable JAR..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}

Write-Host "🔗 Connecting Worker Node to Master at ${MasterHost}:${Port}..." -ForegroundColor Green
java --add-modules jdk.incubator.vector -jar $JAR_PATH --headless --mode=cluster --role=worker --master-host=$MasterHost --port=$Port --secret=$Secret
