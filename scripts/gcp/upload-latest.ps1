param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b"
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

$JarPath = "target/society-simulation-1.0.0-beta.1-executable.jar"

if (-not (Test-Path $JarPath)) {
    Write-Host "Building executable JAR locally..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed."
        exit 1
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       ETHER - UPLOAD LATEST CODE TO GCP CLUSTER          " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Project ID : $ProjectId"
Write-Host "  Target Zone: $Zone"
Write-Host "  JAR Path   : $JarPath"
Write-Host "----------------------------------------------------------"

Write-Host "`n[1/4] Preparing directories on ether-master..." -ForegroundColor Yellow
gcloud compute ssh ether-master --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs /opt/ether/data && sudo chmod -R 777 /opt/ether && touch /opt/ether/.env && sudo pkill -9 -f society-simulation || true"

Write-Host "`n[2/4] Uploading latest JAR & scripts to ether-master..." -ForegroundColor Yellow
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet "$JarPath" ether-master:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet docker-compose.yml ether-master:/opt/ether/docker-compose.yml
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet --recurse scripts/init-db.sql scripts/start-headless.sh scripts/start-master.sh scripts/start-worker.sh ether-master:/opt/ether/scripts/

Write-Host "`n[3/4] Preparing directories on ether-worker..." -ForegroundColor Yellow
gcloud compute ssh ether-worker --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs && sudo chmod -R 777 /opt/ether && touch /opt/ether/.env && sudo pkill -9 -f society-simulation || true"

Write-Host "`n[4/4] Uploading latest JAR & scripts to ether-worker..." -ForegroundColor Yellow
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet "$JarPath" ether-worker:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet --recurse scripts/start-worker.sh ether-worker:/opt/ether/scripts/

Write-Host "`n✅ Successfully updated latest code and executable JAR on both ether-master and ether-worker!" -ForegroundColor Green
