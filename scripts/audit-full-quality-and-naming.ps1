# Full Architectural, Naming, Documentation and Code Quality Audit for Ether Engine
Write-Host "=========================================================="
Write-Host "   ETHER ENGINE: COMPREHENSIVE ARCHITECTURE & QUALITY AUDIT"
Write-Host "=========================================================="

$srcMain = "src/main/java"
$javaFiles = Get-ChildItem -Path $srcMain -Filter "*.java" -Recurse

Write-Host "Scanning $($javaFiles.Count) Java source files in $srcMain...`n"

# 1. ARCHETYPES & NAMING CHECK
Write-Host "--- [1. Naming & Archetypes Analysis] ---"
$legacyManagers = @()
$legacyCatalogs = @()
$legacyEvaluators = @()
$legacyCalculators = @()
$legacyUtils = @()
$proceduralReferences = @()

foreach ($f in $javaFiles) {
    $base = $f.BaseName
    if ($base -match "Manager$") { $legacyManagers += $f.FullName }
    if ($base -match "Catalog$") { $legacyCatalogs += $f.FullName }
    if ($base -match "Evaluator$") { $legacyEvaluators += $f.FullName }
    if ($base -match "Calculator$") { $legacyCalculators += $f.FullName }
    if ($base -match "Util$|Utils$") { $legacyUtils += $f.FullName }
    
    if ($base -match "ProceduralEngine") {
        $proceduralReferences += $f.FullName
    }
}

Write-Host "Legacy *Manager classes:     $($legacyManagers.Count)"
Write-Host "Legacy *Catalog classes:     $($legacyCatalogs.Count)"
Write-Host "Legacy *Evaluator classes:   $($legacyEvaluators.Count)"
Write-Host "Legacy *Calculator classes:  $($legacyCalculators.Count)"
Write-Host "Legacy *Util/*Utils classes: $($legacyUtils.Count)"
Write-Host "ProceduralEngine remnants:   $($proceduralReferences.Count)"

# 2. FILE SIZE LIMIT (<= 500 lines)
Write-Host "`n--- [2. Line Count Limit (<= 500 Lines)] ---"
$overLimit = @()
foreach ($f in $javaFiles) {
    $lines = (Get-Content $f.FullName).Count
    if ($lines -gt 500) {
        $overLimit += [PSCustomObject]@{ File = $f.FullName; Lines = $lines }
    }
}
Write-Host "Classes exceeding 500 lines: $($overLimit.Count)"
foreach ($ol in $overLimit) {
    Write-Host "  -> $($ol.File) ($($ol.Lines) lines)"
}

# 3. JAVADOC & DOCUMENTATION COVERAGE
Write-Host "`n--- [3. Javadoc & Documentation Coverage] ---"
$missingClassJavadoc = @()
$frenchComments = @()

$frenchWords = @('\bpour\b', '\bavec\b', '\bdans\b', '\bcette\b', '\bclasse\b', '\bmoteur\b', '\bcalcul\b', '\binitialisation\b', '\bdonnées\b', '\bmodèle\b', '\brègle\b', '\bétape\b', '\brésultat\b', '\bvaleur\b')
$frenchPattern = '(?i)//.*?(?:' + ($frenchWords -join '|') + ')'

foreach ($f in $javaFiles) {
    if ($f.Name -eq "package-info.java") { continue }
    $content = Get-Content $f.FullName -Raw
    
    # Accurate detection: find class/interface/record/enum declaration and check preceding comments including any annotations (@Entity, @Table, etc.)
    $declPattern = '(?m)^((?:@\w+(?:\([^\)]*\))?\s*)*(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static)\s+)*(?:class|interface|record|enum)\s+' + [regex]::Escape($f.BaseName) + '\b)'
    if ($content -match $declPattern) {
        $match = [regex]::Match($content, $declPattern)
        $before = $content.Substring(0, $match.Index).TrimEnd()
        if (-not $before.EndsWith('*/')) {
            $missingClassJavadoc += $f.FullName
        }
    }
    
    # Check for actual French keywords in single line comments
    if ($content -match $frenchPattern) {
        $frenchComments += $f.FullName
    }
}

Write-Host "Classes missing class-level Javadoc: $($missingClassJavadoc.Count)"
foreach ($m in $missingClassJavadoc) {
    Write-Host "  -> $m"
}

Write-Host "`nFiles with French comments/text detected: $($frenchComments.Count)"
foreach ($fc in $frenchComments) {
    Write-Host "  -> $fc"
}

# 4. ZERO SILENT EXCEPTIONS
Write-Host "`n--- [4. Zero Silent Exception Swallowing] ---"
$swallowedExceptions = @()
foreach ($f in $javaFiles) {
    $content = Get-Content $f.FullName -Raw
    if ($content -match 'catch\s*\([^\)]+\)\s*\{\s*\}' -or $content -match 'catch\s*\([^\)]*ignored[^\)]*\)\s*\{\s*\}') {
        $swallowedExceptions += $f.FullName
    }
}
Write-Host "Silent exception swallowing violations: $($swallowedExceptions.Count)"
foreach ($se in $swallowedExceptions) {
    Write-Host "  -> $se"
}

# 5. RANDOM / DETERMINISM AUDIT
Write-Host "`n--- [5. Determinism & Random Invariants (Tier 1 & Tier 2)] ---"
$unseededRandomInEngines = @()
foreach ($f in $javaFiles) {
    if ($f.FullName -match 'engines|simulation|core|generation') {
        $content = Get-Content $f.FullName -Raw
        if ($content -match '\bnew\s+Random\(\)' -or $content -match 'Math\.random\(\)') {
            $unseededRandomInEngines += $f.FullName
        }
    }
}
Write-Host "Unseeded Random or Math.random() in simulation/engines/core: $($unseededRandomInEngines.Count)"

Write-Host "`n=========================================================="
Write-Host "   AUDIT SCAN COMPLETE"
Write-Host "=========================================================="
