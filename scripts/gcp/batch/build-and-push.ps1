# ==============================================================================
# Ether — Build Docker Image and Push to Artifact Registry (Windows PowerShell)
#
# Usage:
#   .\scripts\gcp\batch\build-and-push.ps1 [-ProjectId <id>] [-Region <region>] [-Tag <tag>]
# ==============================================================================

param(
    [string]$ProjectId = "ether-509812",
    [string]$Region    = "europe-west1",
    [string]$Tag       = "latest"
)

$ErrorActionPreference = 'Stop'
$JarPath = "target\society-simulation-1.0.0-beta.1-executable.jar"
$Image   = "${Region}-docker.pkg.dev/${ProjectId}/ether-registry/ether-engine:${Tag}"

# Run from project root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location (Join-Path $ScriptDir "..\..\..")
try {

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — BUILD & PUSH TO ARTIFACT REGISTRY" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Project ID : $ProjectId"
Write-Host "  Region     : $Region"
Write-Host "  Image Tag  : $Tag"
Write-Host "  Image URI  : $Image"
Write-Host "----------------------------------------------------------"

# ── Step 1: Build Maven jar ────────────────────────────────────────────────
if (-not (Test-Path $JarPath)) {
    Write-Host "[1/3] Building executable JAR (mvn clean package)..."
    mvn clean package -DskipTests -q
    Write-Host "      JAR built: $JarPath"
} else {
    Write-Host "[1/3] Executable JAR already exists — skipping Maven build."
}

# ── Step 2: Configure Docker auth ─────────────────────────────────────────
Write-Host "[2/3] Configuring Docker authentication for Artifact Registry..."
gcloud auth configure-docker "${Region}-docker.pkg.dev" --quiet
Write-Host "      Docker auth configured."

# ── Step 3: Build and push Docker image ───────────────────────────────────
Write-Host "[3/3] Building Docker image and pushing to Artifact Registry..."
$timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
docker build `
    --tag $Image `
    --label "build.version=$Tag" `
    --label "build.timestamp=$timestamp" `
    .

docker push $Image

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "✅ Image pushed successfully!" -ForegroundColor Green
Write-Host "  URI : $Image"
Write-Host ""
Write-Host "To submit a batch job with this image:"
Write-Host "  .\scripts\gcp\batch\submit-batch-job.ps1 -Scenario OUT_OF_AFRICA -Ticks 1000 -Cells 5000 -ImageTag $Tag"
Write-Host "==========================================================" -ForegroundColor Green

} finally {
    Pop-Location
}
