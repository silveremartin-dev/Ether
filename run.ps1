# Ether Planetary Simulation Launcher
$Host.UI.RawUI.WindowTitle = "Ether Planetary Simulation"
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ETHER - Planetary Cliodynamics Simulation Engine" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$JavaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $JavaCmd) {
    Write-Host "[ERROR] Java runtime not found in PATH." -ForegroundColor Red
    Write-Host "Please install OpenJDK 21 or higher from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit..."
    exit 1
}

$JarPath = $null
if (Test-Path (Join-Path $PSScriptRoot "bin\ether.jar")) {
    $JarPath = Join-Path $PSScriptRoot "bin\ether.jar"
} elseif (Test-Path (Join-Path $PSScriptRoot "target\society-simulation-1.0.0-beta.1-executable.jar")) {
    $JarPath = Join-Path $PSScriptRoot "target\society-simulation-1.0.0-beta.1-executable.jar"
} else {
    $FoundJar = Get-ChildItem (Join-Path $PSScriptRoot "target") -Filter "*executable.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($FoundJar) {
        $JarPath = $FoundJar.FullName
    }
}

if ($JarPath) {
    $JvmArgs = @(
        "--add-modules=jdk.incubator.vector",
        "--enable-native-access=ALL-UNNAMED",
        "-Xmx4g",
        "-jar",
        "$JarPath"
    ) + $args
    Write-Host "[INFO] Starting Ether simulation engine..." -ForegroundColor Green
    & java @JvmArgs
} else {
    Write-Host "[INFO] Running via Maven..." -ForegroundColor Yellow
    mvn javafx:run
}
