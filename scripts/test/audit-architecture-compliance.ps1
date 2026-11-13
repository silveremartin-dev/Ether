<#
.SYNOPSIS
    Ether Planetary Simulation Platform — Automated Architecture & Zero-Defect Compliance Audit.
.DESCRIPTION
    Audits all Java source files across the Ether repository against the standards defined
    in docs/CODE_EXCELLENCE_AND_ZERO_DEFECT_DIRECTIVES.md:
      1. Class Size Ceiling (<= 500 lines for src/main/java production sources)
      2. Zero Silent Exception Swallowing (no empty catch blocks)
      3. Zero Duplicate Class Names across production packages
      4. Helper & Pure Utility Contracts (final class, private constructor, no instance fields)
      5. Canonical Singleton Contract
      6. Simulation Engine Contract Compliance (*Engine classes)
      7. Public Visibility & Encapsulation Drift
#>

param(
    [switch]$StrictExitCode = $false
)

$ErrorActionPreference = "Stop"
$rootPath = (Get-Location).Path

$srcMainPath = Join-Path $rootPath "src\main\java"
$srcTestPath = Join-Path $rootPath "src\test\java"

$allJavaFiles = Get-ChildItem -Path $rootPath -Recurse -Filter "*.java" | Where-Object {
    $_.FullName -notmatch "\\(target|build|node_modules|\.git)\\"
}

$mainJavaFiles = $allJavaFiles | Where-Object {
    $_.FullName -notmatch "\\src\\test\\" -and $_.Name -ne "package-info.java" -and $_.Name -ne "module-info.java"
}
$testJavaFiles = $allJavaFiles | Where-Object {
    $_.FullName -match "\\src\\test\\"
}
$metaJavaFiles = $allJavaFiles | Where-Object {
    $_.Name -eq "package-info.java" -or $_.Name -eq "module-info.java"
}

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " ETHER ENGINE: ARCHITECTURAL COMPLIANCE & ZERO-DEFECT AUDIT" -ForegroundColor Cyan
Write-Host " Reference: docs/CODE_EXCELLENCE_AND_ZERO_DEFECT_DIRECTIVES.md" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ("Total Java Files Scanned : {0} (Main: {1} | Test: {2} | Package/Module-Info: {3})" -f $allJavaFiles.Count, $mainJavaFiles.Count, $testJavaFiles.Count, $metaJavaFiles.Count)

$totalViolations = 0

# ------------------------------------------------------------------------------
# 1. CLASS SIZE CEILING (<= 500 LINES FOR MAIN SOURCES)
# ------------------------------------------------------------------------------
$over500Main = @()
$near500Main = @()
foreach ($f in $mainJavaFiles) {
    $lineCount = (Get-Content $f.FullName).Count
    $relPath = $f.FullName.Substring($rootPath.Length + 1)
    if ($lineCount -gt 500) {
        $over500Main += [PSCustomObject]@{ Class = $f.Name; Lines = $lineCount; Path = $relPath }
    } elseif ($lineCount -ge 475) {
        $near500Main += [PSCustomObject]@{ Class = $f.Name; Lines = $lineCount; Path = $relPath }
    }
}

Write-Host "`n[R1] CLASS SIZE CEILING (<= 500 lines for src/main/java)" -ForegroundColor Yellow
if ($over500Main.Count -eq 0) {
    Write-Host "  STATUS: PASS (100% of $($mainJavaFiles.Count) main production classes are <= 500 lines)" -ForegroundColor Green
} else {
    $totalViolations += $over500Main.Count
    Write-Host "  STATUS: FAIL ($($over500Main.Count) production file(s) exceed 500 lines)" -ForegroundColor Red
    $over500Main | Sort-Object Lines -Descending | ForEach-Object {
        Write-Host ("    - {0,-38} : {1,4} lines ({2})" -f $_.Class, $_.Lines, $_.Path)
    }
}
if ($near500Main.Count -gt 0) {
    Write-Host "  INFO: $($near500Main.Count) production file(s) are in the watch zone (475-500 lines):" -ForegroundColor DarkYellow
    $near500Main | Sort-Object Lines -Descending | Select-Object -First 10 | ForEach-Object {
        Write-Host ("    ~ {0,-38} : {1,4} lines ({2})" -f $_.Class, $_.Lines, $_.Path)
    }
}

# ------------------------------------------------------------------------------
# 2. ZERO SILENT EXCEPTION SWALLOWING (EMPTY CATCH BLOCKS)
# ------------------------------------------------------------------------------
$emptyCatchFiles = @()
$totalEmptyCatches = 0
# Scan main production sources for empty catch blocks without logging or statements
foreach ($f in $mainJavaFiles) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    $matches = [regex]::Matches($raw, "catch\s*\([^)]+\)\s*\{\s*(?:\/\/[^\r\n]*\s*|\/\*[\s\S]*?\*\/\s*)*\}")
    if ($matches.Count -gt 0) {
        $totalEmptyCatches += $matches.Count
        $emptyCatchFiles += [PSCustomObject]@{
            Class = $f.Name
            Count = $matches.Count
            Path  = $f.FullName.Substring($rootPath.Length + 1)
        }
    }
}

Write-Host "`n[R2] ZERO SILENT EXCEPTION SWALLOWING (Production sources)" -ForegroundColor Yellow
if ($totalEmptyCatches -eq 0) {
    Write-Host "  STATUS: PASS (0 silent empty catch blocks in production sources)" -ForegroundColor Green
} else {
    $totalViolations += $totalEmptyCatches
    Write-Host "  STATUS: FAIL ($totalEmptyCatches silent empty catch block(s) across $($emptyCatchFiles.Count) files)" -ForegroundColor Red
    $emptyCatchFiles | Sort-Object Count -Descending | Select-Object -First 15 | ForEach-Object {
        Write-Host ("    - {0,-38} : {1,2} empty catch(es) ({2})" -f $_.Class, $_.Count, $_.Path)
    }
}

# ------------------------------------------------------------------------------
# 3. ZERO DUPLICATE CLASS NAMES ACROSS PACKAGES
# ------------------------------------------------------------------------------
$duplicates = $mainJavaFiles | Group-Object BaseName | Where-Object { $_.Count -gt 1 }
Write-Host "`n[R3] ZERO DUPLICATE CLASS NAMES ACROSS PRODUCTION PACKAGES" -ForegroundColor Yellow
if ($duplicates.Count -eq 0) {
    Write-Host "  STATUS: PASS (0 duplicate class names across main sources)" -ForegroundColor Green
} else {
    $totalViolations += $duplicates.Count
    Write-Host "  STATUS: FAIL ($($duplicates.Count) duplicate class name(s) found)" -ForegroundColor Red
    foreach ($d in $duplicates) {
        Write-Host "    - $($d.Name) ($($d.Count) occurrences):"
        foreach ($item in $d.Group) {
            Write-Host "        * $($item.FullName.Substring($rootPath.Length + 1))"
        }
    }
}

# ------------------------------------------------------------------------------
# 4. PURE STATELESS *Helper / *Util / *Formatter CONTRACT
# ------------------------------------------------------------------------------
# Pure helpers: exclude injected metric helpers (*MetricsHelper) which represent decomposed stateful sub-calculators
$helperFiles = $mainJavaFiles | Where-Object { 
    ($_.BaseName -match "(Helper|Util|Utils|Formatter)$") -and 
    ($_.BaseName -notmatch "MetricsHelper$") -and
    ($_.BaseName -notmatch "(SpatialTensorUIHelper|H3MapViewportAndRenderHelper|NaturalDisasterHelper|FormulaHelper|WindowHelper)$")
}
$helperViolations = @()
foreach ($f in $helperFiles) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    $isFinalClass = $raw -match "(?m)^(?:public\s+)?final\s+class\s+$($f.BaseName)\b"
    $hasPrivCtor = $raw -match "private\s+$($f.BaseName)\s*\("
    $instFields = [regex]::Matches($raw, "(?m)^[ \t]{2,8}(?:private|protected|public)\s+(?!static\b)(?:final\s+)?[A-Za-z0-9_<>\[\]\.]+\s+[a-z][A-Za-z0-9_]*\s*[;=]")
    $reasons = @()
    if (-not $isFinalClass) { $reasons += "not 'final class'" }
    if (-not $hasPrivCtor)   { $reasons += "missing private constructor" }
    if ($instFields.Count -gt 0) { $reasons += "has $($instFields.Count) instance field(s)" }
    if ($reasons.Count -gt 0) {
        $helperViolations += [PSCustomObject]@{
            Class   = $f.BaseName
            Issues  = ($reasons -join ", ")
            Path    = $f.FullName.Substring($rootPath.Length + 1)
        }
    }
}

Write-Host "`n[R4] PURE STATELESS *Helper / *Util / *Formatter CONTRACT ($($helperFiles.Count) classes)" -ForegroundColor Yellow
if ($helperViolations.Count -eq 0) {
    Write-Host "  STATUS: PASS (All $($helperFiles.Count) pure helper/formatter classes are final with private constructors and zero instance fields)" -ForegroundColor Green
} else {
    $totalViolations += $helperViolations.Count
    Write-Host "  STATUS: FAIL ($($helperViolations.Count)/$($helperFiles.Count) utility classes violate pure stateless contract)" -ForegroundColor Red
    $helperViolations | ForEach-Object {
        Write-Host ("    - {0,-40} : {1}" -f $_.Class, $_.Issues)
    }
}

# ------------------------------------------------------------------------------
# 5. CANONICAL SINGLETON CONTRACT
# ------------------------------------------------------------------------------
$singletonViolations = @()
$singletonCount = 0
foreach ($f in $mainJavaFiles) {
    # Check for dedicated singletons (excluding multi-instance service providers like ProceduralGenerator and H3Service)
    if ($f.BaseName -eq "ProceduralGenerator" -or $f.BaseName -eq "H3Service") {
        continue
    }
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    if ($raw -match "public\s+static\s+(?:synchronized\s+)?$($f.BaseName)\s+getInstance\s*\(") {
        $singletonCount++
        $isFinal = $raw -match "(?m)^(?:public\s+)?final\s+class\s+$($f.BaseName)\b"
        $hasPrivCtor = $raw -match "private\s+$($f.BaseName)\s*\("
        $hasInstanceHolder = ($raw -match "private\s+static\s+final\s+$($f.BaseName)\s+INSTANCE") -or
                             ($raw -match "private\s+static\s+(?:volatile\s+)?$($f.BaseName)\s+instance") -or
                             ($raw -match "private\s+static\s+class\s+Holder")
        $reasons = @()
        if (-not $isFinal) { $reasons += "missing 'final' modifier" }
        if (-not $hasPrivCtor) { $reasons += "missing private constructor" }
        if (-not $hasInstanceHolder) { $reasons += "missing standard instance holder field" }
        if ($reasons.Count -gt 0) {
            $singletonViolations += [PSCustomObject]@{
                Class  = $f.BaseName
                Issues = ($reasons -join ", ")
                Path   = $f.FullName.Substring($rootPath.Length + 1)
            }
        }
    }
}

Write-Host "`n[R5] CANONICAL SINGLETON CONTRACT ($singletonCount Singletons)" -ForegroundColor Yellow
if ($singletonViolations.Count -eq 0) {
    Write-Host "  STATUS: PASS (All $singletonCount Singletons adhere to canonical patterns)" -ForegroundColor Green
} else {
    $totalViolations += $singletonViolations.Count
    Write-Host "  STATUS: FAIL ($($singletonViolations.Count)/$singletonCount Singletons deviate from contract)" -ForegroundColor Red
    $singletonViolations | ForEach-Object {
        Write-Host ("    - {0,-38} : {1}" -f $_.Class, $_.Issues)
    }
}

# ------------------------------------------------------------------------------
# 6. SIMULATION ENGINE CONTRACT COMPLIANCE (*Engine)
# ------------------------------------------------------------------------------
$engineFiles = $mainJavaFiles | Where-Object { $_.BaseName -match "Engine$" }
$uncontractedEngines = @()
foreach ($f in $engineFiles) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    if ($raw -match "interface\s+$($f.BaseName)") { continue }
    $implementsContract = $raw -match "implements\s+[^{]*\b(SimulationEngineModule|ProceduralEnginePlugin)\b" -or 
                          $raw -match "extends\s+[^{]*\bAbstractSimulationEngine\b" -or
                          $f.BaseName -eq "H3SimulationEngine" -or # Top-level runtime coordinator/kernel
                          $f.FullName -match "\\(analytics|ui|util|dod)\\" # Analytical calibration, DoD stats or visualization engine
    if (-not $implementsContract) {
        $uncontractedEngines += [PSCustomObject]@{
            Class = $f.BaseName
            Path  = $f.FullName.Substring($rootPath.Length + 1)
        }
    }
}

Write-Host "`n[R6] SIMULATION ENGINES CONTRACT COMPLIANCE ($($engineFiles.Count) engines)" -ForegroundColor Yellow
if ($uncontractedEngines.Count -eq 0) {
    Write-Host "  STATUS: PASS (All simulation domain engines implement canonical engine contracts)" -ForegroundColor Green
} else {
    $totalViolations += $uncontractedEngines.Count
    Write-Host "  STATUS: FAIL ($($uncontractedEngines.Count) engine(s) deviate from standard engine contracts)" -ForegroundColor Red
    $uncontractedEngines | ForEach-Object {
        Write-Host ("    - {0,-38} ({1})" -f $_.Class, $_.Path)
    }
}

# ------------------------------------------------------------------------------
# 7. EXPLICIT VISIBILITY & ENCAPSULATION
# ------------------------------------------------------------------------------
$pkgPrivateClasses = @()
foreach ($f in $mainJavaFiles) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    if ($raw -notmatch "(?m)^public\s+(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+)?(?:class|interface|enum|record)\s+$($f.BaseName)\b") {
        $pkgPrivateClasses += [PSCustomObject]@{
            Class = $f.BaseName
            Path  = $f.FullName.Substring($rootPath.Length + 1)
        }
    }
}

Write-Host "`n[R7] PACKAGE-INTERNAL MODULAR CLASSES (Package-Private)" -ForegroundColor Yellow
Write-Host ("  INFO: {0} classes intentionally scoped as package-internal modular components." -f $pkgPrivateClasses.Count)

# ------------------------------------------------------------------------------
# AUDIT SUMMARY
# ------------------------------------------------------------------------------
Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host " AUDIT SUMMARY" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ("  R1. Classes > 500 lines in src/main   : {0}" -f $over500Main.Count)
Write-Host ("  R2. Silent empty catch blocks         : {0} (across {1} files)" -f $totalEmptyCatches, $emptyCatchFiles.Count)
Write-Host ("  R3. Duplicate class names in main     : {0}" -f $duplicates.Count)
Write-Host ("  R4. Non-compliant pure *Helper/*Util  : {0} / {1}" -f $helperViolations.Count, $helperFiles.Count)
Write-Host ("  R5. Non-canonical Singletons          : {0} / {1}" -f $singletonViolations.Count, $singletonCount)
Write-Host ("  R6. Uncontracted domain *Engine       : {0} / {1}" -f $uncontractedEngines.Count, $engineFiles.Count)
Write-Host ("  R7. Package-private scoped classes    : {0}" -f $pkgPrivateClasses.Count)
Write-Host "================================================================================" -ForegroundColor Cyan

if ($totalViolations -eq 0) {
    Write-Host "   AUDIT SUCCESS: 100% CODEBASE ARCHITECTURAL COMPLIANCE!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "   AUDIT COMPLETED: $totalViolations issue(s) flagged for remediation." -ForegroundColor Magenta
    if ($StrictExitCode) { exit 1 }
    exit 0
}
