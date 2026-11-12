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

$canonicalSuffixes = @(
    "Engine", "Coordinator", "Factory", "Registry", "Repository", 
    "Executor", "Helper", "Formatter", "Service", "Buffer", "Config", 
    "Exception", "Event", "Listener", "Task", "State", "Context", "Model",
    "Section", "Pane", "View", "Renderer", "Action", "Node", "Record",
    "Constants", "Runner", "Builder", "Codec", "Bridge", "Exporter", 
    "Importer", "Generator", "Snapshot", "Aggregator", "Projection", "Vector",
    "Interpolator", "Filter", "Result", "Entry", "Definition", "Descriptor",
    "Layer", "Data", "Profile", "Type", "Category", "Level", "Status", "Metric",
    "Matrix", "Sampler", "Cluster", "Client", "Server", "Protocol", "Message",
    "Payload", "Signature", "Key", "Token", "Credentials", "Policy", "Rule",
    "Schema", "Table", "Entity", "Id", "Ref", "Handle", "Iterator", "Cursor"
)

foreach ($f in $javaFiles) {
    $base = $f.BaseName
    if ($base -match "Manager$") { $legacyManagers += $f.FullName }
    if ($base -match "Catalog$") { $legacyCatalogs += $f.FullName }
    if ($base -match "Evaluator$") { $legacyEvaluators += $f.FullName }
    if ($base -match "Calculator$") { $legacyCalculators += $f.FullName }
    if ($base -match "Util$|Utils$") { $legacyUtils += $f.FullName }
    
    # Check for legacy procedural word
    if ($base -match "ProceduralEngine") {
        $proceduralReferences += $f.FullName
    }
}

Write-Host "Legacy *Manager classes:     $($legacyManagers.Count)"
foreach ($m in $legacyManagers) { Write-Host "  -> $m" }

Write-Host "Legacy *Catalog classes:     $($legacyCatalogs.Count)"
foreach ($c in $legacyCatalogs) { Write-Host "  -> $c" }

Write-Host "Legacy *Evaluator classes:   $($legacyEvaluators.Count)"
foreach ($e in $legacyEvaluators) { Write-Host "  -> $e" }

Write-Host "Legacy *Calculator classes:  $($legacyCalculators.Count)"
foreach ($ca in $legacyCalculators) { Write-Host "  -> $ca" }

Write-Host "Legacy *Util/*Utils classes: $($legacyUtils.Count)"
foreach ($u in $legacyUtils) { Write-Host "  -> $u" }

Write-Host "ProceduralEngine remnants:   $($proceduralReferences.Count)"
foreach ($p in $proceduralReferences) { Write-Host "  -> $p" }

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

foreach ($f in $javaFiles) {
    $content = Get-Content $f.FullName -Raw
    
    # Check if class declaration has preceding javadoc /** ... */
    if ($content -notmatch '(?s)/\*\*.*?\*/\s*(?:public|protected|private|abstract|final|sealed)?\s*(?:class|interface|record|enum)\s+' + [regex]::Escape($f.BaseName)) {
        $missingClassJavadoc += $f.FullName
    }
    
    # Check for French words in comments/code
    # French words common in comments
    if ($content -match '(?i)//.*?(?:pour|avec|dans|cette|classe|moteur|calcul|initialisation|données|modèle|règle|étape|résultat|valeur)') {
        $frenchComments += $f.FullName
    }
}

Write-Host "Classes missing class-level Javadoc: $($missingClassJavadoc.Count)"
for ($i = 0; $i -lt [Math]::Min(10, $missingClassJavadoc.Count); $i++) {
    Write-Host "  -> $($missingClassJavadoc[$i])"
}
if ($missingClassJavadoc.Count -gt 10) {
    Write-Host "  ... and $($missingClassJavadoc.Count - 10) more."
}

Write-Host "`nFiles with French comments/text detected: $($frenchComments.Count)"
for ($i = 0; $i -lt [Math]::Min(10, $frenchComments.Count); $i++) {
    Write-Host "  -> $($frenchComments[$i])"
}
if ($frenchComments.Count -gt 10) {
    Write-Host "  ... and $($frenchComments.Count - 10) more."
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
foreach ($r in $unseededRandomInEngines) {
    Write-Host "  -> $r"
}

Write-Host "`n=========================================================="
Write-Host "   AUDIT SCAN COMPLETE"
Write-Host "=========================================================="
