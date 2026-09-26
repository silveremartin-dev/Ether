param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$Scenario = "OUT_OF_AFRICA",
    [int]$Ticks = 1000,
    [int]$Cells = 5000,
    [switch]$ClusterMode = $false,
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "       ETHER - REMOTE GCP DEPLOYMENT AND EXECUTION        "
Write-Host "=========================================================="
Write-Host "  Project ID     : $ProjectId"
Write-Host "  Target Zone    : $Zone"
Write-Host "  Scenario       : $Scenario"
Write-Host "  Ticks to Run   : $Ticks"
Write-Host "  H3 Grid Cells  : $Cells"
Write-Host "  Cluster Mode   : $ClusterMode"
Write-Host "----------------------------------------------------------"

$JarPath = "target/society-simulation-1.0.0-beta.1-executable.jar"

if (-not $SkipBuild -or -not (Test-Path $JarPath)) {
    Write-Host "[1/5] Building executable JAR locally..."
    mvn clean package "-Dmaven.test.skip=true"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed."
        exit 1
    }
} else {
    Write-Host "[1/5] Using existing executable JAR ($JarPath)..."
}

Write-Host "[2/5] Preparing remote directories on ether-master..."
gcloud compute ssh ether-master --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs /opt/ether/data && sudo chmod -R 777 /opt/ether"

Write-Host "[3/5] Uploading JAR and configuration files to ether-master..."
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet "$JarPath" ether-master:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet docker-compose.yml ether-master:/opt/ether/docker-compose.yml
gcloud compute scp --zone=$Zone --project=$ProjectId --quiet --recurse scripts/init-db.sql scripts/start-headless.sh scripts/start-master.sh scripts/start-worker.sh ether-master:/opt/ether/scripts/

Write-Host "[4/5] Initializing PostgreSQL container on ether-master..."
gcloud compute ssh ether-master --zone=$Zone --project=$ProjectId --quiet --command="cd /opt/ether && sudo docker-compose up -d postgres"

if ($ClusterMode) {
    Write-Host "[5/5] Cluster mode enabled: setting up ether-worker..."
    $masterInternalIp = (gcloud compute instances describe ether-master --zone=$Zone --project=$ProjectId --format="get(networkInterfaces[0].networkIP)").Trim()
    Write-Host "Master Internal IP: $masterInternalIp"

    gcloud compute ssh ether-worker --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs && sudo chmod -R 777 /opt/ether"
    gcloud compute scp --zone=$Zone --project=$ProjectId --quiet "$JarPath" ether-worker:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
    gcloud compute scp --zone=$Zone --project=$ProjectId --quiet --recurse scripts/start-worker.sh ether-worker:/opt/ether/scripts/

    Write-Host "Launching Worker node in background..."
    gcloud compute ssh ether-worker --zone=$Zone --project=$ProjectId --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && nohup ./scripts/start-worker.sh $masterInternalIp 9090 EtherClusterSecret2026 > logs/worker.log 2>&1 &"

    Start-Sleep -Seconds 4

    Write-Host "Executing Master Simulation Node in Cluster Mode..."
    gcloud compute ssh ether-master --zone=$Zone --project=$ProjectId --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && ./scripts/start-master.sh '$Scenario' 9090 EtherClusterSecret2026 $Ticks $Cells"
} else {
    Write-Host "[5/5] Launching Headless Simulation on ether-master..."
    gcloud compute ssh ether-master --zone=$Zone --project=$ProjectId --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && ./scripts/start-headless.sh '$Scenario' $Ticks $Cells"
}

Write-Host "Simulation execution finished."
Write-Host "Run .\scripts\gcp\fetch-results.ps1 to download snapshots for local replay."
