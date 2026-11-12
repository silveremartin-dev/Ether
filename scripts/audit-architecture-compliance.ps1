# Architecture Compliance & Code Purity Auditor for Ether Engine
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   ETHER ENGINE: ARCHITECTURAL COMPLIANCE AUDIT" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$srcDir = "src/main/java"
$allJavaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter *.java
$totalFiles = $allJavaFiles.Count

Write-Host "Scanning $totalFiles Java source files in $srcDir..." -ForegroundColor Yellow

$violationsCount = 0

# 1. Check classes exceeding 500 lines
Write-Host "`n--- [Rule 1: Class Size <= 500 Lines] ---" -ForegroundColor White
$longFiles = @()
foreach ($f in $allJavaFiles) {
    $lines = (Get-Content $f.FullName).Count
    if ($lines -gt 500) {
        $relPath = $f.FullName.Replace((Get-Location).Path + "\", "")
        $longFiles += [PSCustomObject]@{ Lines = $lines; File = $relPath }
    }
}
if ($longFiles.Count -gt 0) {
    $violationsCount += $longFiles.Count
    Write-Host "FAILED: $($longFiles.Count) files exceed 500 lines:" -ForegroundColor Red
    $longFiles | Sort-Object Lines -Descending | Format-Table -AutoSize | Out-String | Write-Host
} else {
    Write-Host "PASSED: All classes are within the 500-line ceiling." -ForegroundColor Green
}

# 2. Check for empty exception swallowing
Write-Host "--- [Rule 2: No Silent Exception Swallowing] ---" -ForegroundColor White
$swallowMatches = Select-String -Path "$srcDir\*.java" -Pattern "catch\s*\([A-Za-z0-9_]+\s+ignored\)\s*\{\s*\}" -AllMatches
if ($swallowMatches.Count -gt 0) {
    $violationsCount += $swallowMatches.Count
    Write-Host "FAILED: Found $($swallowMatches.Count) empty catch blocks:" -ForegroundColor Red
    $swallowMatches | Select-Object -First 10 | ForEach-Object {
        $p = $_.Path.Replace((Get-Location).Path + "\", "")
        Write-Host "  $p : line $($_.LineNumber)" -ForegroundColor Yellow
    }
} else {
    Write-Host "PASSED: Zero silent exception swallowing detected." -ForegroundColor Green
}

# 3. Check Engine contract compliance
Write-Host "`n--- [Rule 3: Simulation Engines Contract Compliance] ---" -ForegroundColor White
$engineFiles = Get-ChildItem -Path "$srcDir/org/ether/society/engines" -Recurse -Filter "*Engine.java"
$uncontractedEngines = @()
foreach ($f in $engineFiles) {
    $content = [IO.File]::ReadAllText($f.FullName)
    $implementsContract = $content.Contains("implements ProceduralEnginePlugin") -or 
                          $content.Contains("implements SimulationEngineModule") -or 
                          $content.Contains("extends AbstractSimulationEngine")
    if (-not $implementsContract) {
        $rel = $f.FullName.Replace((Get-Location).Path + "\", "")
        $uncontractedEngines += $rel
    }
}
if ($uncontractedEngines.Count -gt 0) {
    Write-Host "WARNING: $($uncontractedEngines.Count) engines do not implement standard engine contracts:" -ForegroundColor Yellow
    $uncontractedEngines | ForEach-Object { Write-Host "  - $_" -ForegroundColor DarkYellow }
} else {
    Write-Host "PASSED: All simulation engines implement standard engine interfaces." -ForegroundColor Green
}

# Summary
Write-Host "`n==========================================================" -ForegroundColor Cyan
if ($violationsCount -eq 0) {
    Write-Host "   AUDIT SUCCESS: 100% CODEBASE COMPLIANCE!" -ForegroundColor Green
} else {
    Write-Host "   AUDIT COMPLETED: $violationsCount issues flagged for remediation." -ForegroundColor Magenta
}
Write-Host "==========================================================" -ForegroundColor Cyan
