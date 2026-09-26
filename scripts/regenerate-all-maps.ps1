<#
.SYNOPSIS
    Ether Historical Cartographic Tensor Regenerator.
.DESCRIPTION
    Regenerates all 25 PNG cartographic rasters (2048x1024), cultural registries,
    provenance files, and documentation across all epochs (-100,000 to 2060).
#>

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "               ETHER HISTORICAL CARTOGRAPHIC TENSOR REGENERATOR" -ForegroundColor Cyan
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[ATTENTION / WARNING]" -ForegroundColor Yellow
Write-Host "1. You MUST have downloaded all empirical raw geospatial datasets beforehand:" -ForegroundColor Yellow
Write-Host "   - WorldClim v2.1 GeoTIFFs (wc2.1_10m_bio_1.tif, bio_4.tif, bio_12.tif)"
Write-Host "   - NOAA ETOPO 2022 relief and GEBCO 2023 grids"
Write-Host "   - HYDE 3.4 historical population grids in data/sources/"
Write-Host "   - USGS and Global Energy Monitor (GEM) mineral/petroleum datasets"
Write-Host ""
Write-Host "2. RUNNING THIS SCRIPT WILL OVERWRITE ALL EXISTING CARTOGRAPHIC TENSORS" -ForegroundColor Red
Write-Host "   in data/maps/ether/earth/ across all historical epochs (-100,000 to 2060)." -ForegroundColor Red
Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""

$Confirm = Read-Host "Are you sure you want to proceed and overwrite all maps? (y/N)"
if ($Confirm -notmatch '^[Yy]') {
    Write-Host "[ABORTED] Operation cancelled by user. No maps were modified." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "[*] Launching Master Cartographic Batch Regeneration Suite via Maven..." -ForegroundColor Green
Write-Host ""

mvn test "-Dtest=BatchRegenerateAllScenarioMapsTest" "-DfailIfNoSpecifiedTests=false"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "===============================================================================" -ForegroundColor Green
    Write-Host "[SUCCESS] All 25 layers, cultural registries, provenance, and READMEs" -ForegroundColor Green
    Write-Host "          have been successfully regenerated across all historical epochs!" -ForegroundColor Green
    Write-Host "===============================================================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[ERROR] Map regeneration encountered errors. Please check the logs above." -ForegroundColor Red
    exit 1
}
