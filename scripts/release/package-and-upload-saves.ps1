# Package and Upload Simulation Saves / Snapshots
param(
    [string]$Version = "1.0.0-beta.4",
    [switch]$Upload = $false
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Packaging Simulation Saves for GitHub Release (v$Version)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$distDir = Join-Path $root "dist"
if (-not (Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

$savesDir = Join-Path $root "saves"
if (-not (Test-Path $savesDir)) {
    Write-Error "Saves directory not found at $savesDir"
}

# Group runs by era
$part1Dirs = New-Object System.Collections.Generic.List[string]
$part2Dirs = New-Object System.Collections.Generic.List[string]

Get-ChildItem -Directory $savesDir | ForEach-Object {
    $name = $_.Name
    # Part 1: Ancient, Prehistoric, Classical, Medieval (<= 1400 CE)
    if ($name -match "OUT-OF-AFRICA|TOBA|SAHUL|BERINGIA|SOLUTREAN|YOUNGER-DRYAS|FERTILE-CRESCENT|GREEN-SAHARA|ANCIENT-EGYPT|BRONZE|RAMESSES|IRON-AGE|HELLENISTIC|MAURYA|PAX-ROMANA|ANTONINE|LATE-ANTIQUE|ISLAMIC|CAROLINGIAN|SONG|MONGOL|MALI|BLACK-DEATH|MESOAMERICAN") {
        $part1Dirs.Add("saves/$name")
    } else {
        $part2Dirs.Add("saves/$name")
    }
}

Write-Host "Part 1 (Ancient & Medieval): $($part1Dirs.Count) runs" -ForegroundColor Yellow
Write-Host "Part 2 (Modern, Prospective & Exoplanets): $($part2Dirs.Count) runs" -ForegroundColor Yellow

$part1Archive = Join-Path $distDir "Ether-v$Version-saves-part1-ancient-and-medieval.tar.gz"
$part2Archive = Join-Path $distDir "Ether-v$Version-saves-part2-modern-and-exo.tar.gz"

Write-Host "Creating Part 1 archive: $part1Archive" -ForegroundColor Green
& tar -czf $part1Archive $part1Dirs

Write-Host "Creating Part 2 archive: $part2Archive" -ForegroundColor Green
& tar -czf $part2Archive $part2Dirs

$size1 = [math]::Round((Get-Item $part1Archive).Length / 1MB, 2)
$size2 = [math]::Round((Get-Item $part2Archive).Length / 1MB, 2)

Write-Host "✔ Part 1 size: $size1 MB" -ForegroundColor Green
Write-Host "✔ Part 2 size: $size2 MB" -ForegroundColor Green

if ($Upload) {
    Write-Host "Uploading to GitHub Release v$Version..." -ForegroundColor Cyan
    $ghCmd = "gh"
    $ghAvailable = $false
    if (Get-Command "gh" -ErrorAction SilentlyContinue) {
        $ghAvailable = $true
    } elseif (Test-Path "C:\Program Files\GitHub CLI\gh.exe") {
        $ghCmd = "C:\Program Files\GitHub CLI\gh.exe"
        $ghAvailable = $true
    }

    if ($ghAvailable) {
        & $ghCmd release upload "v$Version" $part1Archive $part2Archive --clobber
        Write-Host "✔ Upload complete via GitHub CLI!" -ForegroundColor Green
    } else {
        Write-Host "⚠ 'gh' CLI not found. Artifacts ready in dist/ for CI/manual upload." -ForegroundColor Yellow
    }
}
