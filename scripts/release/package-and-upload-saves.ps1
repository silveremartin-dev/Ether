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

# Deduplicate save directories: keep only the newest run for each scenario preset
Write-Host "`n>>> Deduplicating save runs in saves/..." -ForegroundColor Yellow
Get-ChildItem -Directory $savesDir | Where-Object { $_.Name -like "RUN-*" } | Group-Object { $_.Name -replace '-+\d+$', '' } | Where-Object Count -gt 1 | ForEach-Object {
    $sorted = $_.Group | Sort-Object LastWriteTime -Descending
    $keep = $sorted[0].Name
    $toRemove = $sorted | Select-Object -Skip 1
    foreach ($old in $toRemove) {
        Write-Host "  [Purge] Removing older duplicate save: $($old.Name)" -ForegroundColor DarkGray
        Remove-Item -Recurse -Force $old.FullName
    }
    Write-Host "  [Retain] Preserving newest complete save: $keep" -ForegroundColor Green
}

# 4-Part grouping to strictly respect GitHub's 2 GiB (2048 MB) per-asset limit
$part1Dirs = New-Object System.Collections.Generic.List[string]
$part2Dirs = New-Object System.Collections.Generic.List[string]
$part3Dirs = New-Object System.Collections.Generic.List[string]
$part4Dirs = New-Object System.Collections.Generic.List[string]

Get-ChildItem -Directory $savesDir | ForEach-Object {
    $name = $_.Name
    if (-not ($name -like "RUN-*")) { return }
    $dirRef = "saves/$name"

    # Part 1: Deep Prehistory & Antiquity (-100,000 BP to 500 CE)
    if ($name -match "OUT-OF-AFRICA|TOBA|SAHUL|BERINGIA|SOLUTREAN|YOUNGER-DRYAS|FERTILE-CRESCENT|GREEN-SAHARA|ANCIENT-EGYPT|BRONZE|RAMESSES|IRON-AGE|HELLENISTIC|MAURYA|PAX-ROMANA|ANTONINE|MESOAMERICAN") {
        $part1Dirs.Add($dirRef)
    }
    # Part 2: Medieval & Early Modern (500 CE to 1800 CE)
    elseif ($name -match "LATE-ANTIQUE|ISLAMIC|CAROLINGIAN|SONG|MONGOL|MALI|BLACK-DEATH|PRE-COLUMBIAN|EUROPEAN-ARRIVAL|TOKUGAWA") {
        $part2Dirs.Add($dirRef)
    }
    # Part 4: Off-Earth Space Colonies & Exoplanets
    elseif ($name -match "SHACKLETON|LUNAR|MARTIAN|ARCHIPELAGO|VENUSIAN|OCEAN-WORLD|BOREAS|SOLAR-FORGE|MERCURY|TITAN|INTERSTELLAR|GAIA|TIDALLY-LOCKED|EYEBALL") {
        $part4Dirs.Add($dirRef)
    }
    # Part 3: Industrial Revolution, 20th Century & Future Earth Trajectories
    else {
        $part3Dirs.Add($dirRef)
    }
}

Write-Host "Part 1 (Prehistory & Antiquity): $($part1Dirs.Count) runs" -ForegroundColor Yellow
Write-Host "Part 2 (Medieval & Early Modern): $($part2Dirs.Count) runs" -ForegroundColor Yellow
Write-Host "Part 3 (Modern, Anthropocene & Future Earth): $($part3Dirs.Count) runs" -ForegroundColor Yellow
Write-Host "Part 4 (Off-Earth & Exoplanets): $($part4Dirs.Count) runs" -ForegroundColor Yellow

$p1Archive = Join-Path $distDir "Ether-v$Version-saves-part1-prehistory-and-antiquity.tar.gz"
$p2Archive = Join-Path $distDir "Ether-v$Version-saves-part2-medieval-and-early-modern.tar.gz"
$p3Archive = Join-Path $distDir "Ether-v$Version-saves-part3-modern-and-future-earth.tar.gz"
$p4Archive = Join-Path $distDir "Ether-v$Version-saves-part4-off-earth-and-exoplanets.tar.gz"

Write-Host "`nCreating Part 1 archive: $p1Archive" -ForegroundColor Green
& tar -czf $p1Archive $part1Dirs
$s1 = [math]::Round((Get-Item $p1Archive).Length / 1MB, 2)
Write-Host "✔ Part 1 size: $s1 MB" -ForegroundColor Green

Write-Host "`nCreating Part 2 archive: $p2Archive" -ForegroundColor Green
& tar -czf $p2Archive $part2Dirs
$s2 = [math]::Round((Get-Item $p2Archive).Length / 1MB, 2)
Write-Host "✔ Part 2 size: $s2 MB" -ForegroundColor Green

Write-Host "`nCreating Part 3 archive: $p3Archive" -ForegroundColor Green
& tar -czf $p3Archive $part3Dirs
$s3 = [math]::Round((Get-Item $p3Archive).Length / 1MB, 2)
Write-Host "✔ Part 3 size: $s3 MB" -ForegroundColor Green

Write-Host "`nCreating Part 4 archive: $p4Archive" -ForegroundColor Green
& tar -czf $p4Archive $part4Dirs
$s4 = [math]::Round((Get-Item $p4Archive).Length / 1MB, 2)
Write-Host "✔ Part 4 size: $s4 MB" -ForegroundColor Green

# Remove legacy 2-part archives if present
Remove-Item -Force (Join-Path $distDir "Ether-v$Version-saves-part1-ancient-and-medieval.tar.gz") -ErrorAction SilentlyContinue
Remove-Item -Force (Join-Path $distDir "Ether-v$Version-saves-part2-modern-and-exo.tar.gz") -ErrorAction SilentlyContinue

if ($Upload) {
    Write-Host "`nUploading 4 archives to GitHub Release v$Version..." -ForegroundColor Cyan
    if (-not $env:GH_TOKEN) {
        try {
            $cred = ("protocol=https`nhost=github.com`n`n" | git credential fill 2>$null | Out-String)
            $tokenLine = ($cred -split "`n" | Where-Object { $_ -like "password=*" })
            if ($tokenLine) {
                $env:GH_TOKEN = $tokenLine.Substring(9).Trim()
            }
        } catch {}
    }

    $ghCmd = "gh"
    $ghAvailable = $false
    if (Get-Command "gh" -ErrorAction SilentlyContinue) {
        $ghAvailable = $true
    } elseif (Test-Path "C:\Program Files\GitHub CLI\gh.exe") {
        $ghCmd = "C:\Program Files\GitHub CLI\gh.exe"
        $ghAvailable = $true
    }

    if ($ghAvailable) {
        & $ghCmd release upload "v$Version" $p1Archive $p2Archive $p3Archive $p4Archive --clobber
        Write-Host "✔ Upload complete via GitHub CLI!" -ForegroundColor Green
    } else {
        Write-Host "⚠ 'gh' CLI not found. Artifacts ready in dist/ for CI/manual upload." -ForegroundColor Yellow
    }
}
