# PowerShell script to harmonize simulation engines to extend AbstractSimulationEngine
param(
    [string]$TargetDir = "src/main/java/org/ether/society/engines"
)

$allEngineFiles = Get-ChildItem -Path $TargetDir -Recurse -Filter "*Engine.java"
$modifiedCount = 0

foreach ($file in $allEngineFiles) {
    $content = [IO.File]::ReadAllText($file.FullName)
    
    # Skip if already implementing contract
    if ($content.Contains("extends AbstractSimulationEngine") -or 
        $content.Contains("implements SimulationEngineModule") -or 
        $content.Contains("implements ProceduralEnginePlugin")) {
        continue
    }

    $fileName = $file.BaseName
    $isTier1 = $file.FullName.Contains("tier1")
    $tierEnum = if ($isTier1) { "EngineTier.TIER_1_BIOPHYSICAL" } else { "EngineTier.TIER_2_CLIODYNAMIC" }

    # Detect methods
    $hasProcessHybrid = $content.Contains("processHybrid(")
    $hasProcess = $content.Contains("void process(") -and -not $content.Contains("@Override`n    public void process(")
    $hasResetState = $content.Contains("void resetState()")

    # Generate contract implementation methods
    $methodsToAdd = "`n    public $fileName() {`n        super(`"$fileName`", `"$fileName`", $tierEnum);`n    }`n"

    if ($hasProcessHybrid) {
        $methodsToAdd += "`n    @Override`n    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {`n        processHybrid(cells, deltaYears);`n    }`n"
    } elseif ($hasProcess) {
        # Check if process is static
        if ($content -match "static\s+void\s+process\(") {
            $methodsToAdd += "`n    @Override`n    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {`n        process(cells, deltaYears);`n    }`n"
        }
    }

    if ($hasResetState) {
        $methodsToAdd += "`n    @Override`n    public void reset() {`n        resetState();`n    }`n"
    }

    # Add imports
    $importBlock = "import org.ether.society.core.engine.AbstractSimulationEngine;`nimport org.ether.society.core.engine.EngineTier;`n"
    if (-not $content.Contains("import org.ether.society.core.engine.AbstractSimulationEngine;")) {
        $packageEnd = $content.IndexOf(";")
        if ($packageEnd -gt 0) {
            $content = $content.Substring(0, $packageEnd + 1) + "`n`n" + $importBlock + $content.Substring($packageEnd + 1).TrimStart()
        }
    }

    # Replace class declaration
    $classRegex = "(public\s+(?:abstract\s+)?class\s+$fileName)(\s*\{|\s+implements\s+[^{]+\{)"
    if ($content -match "public\s+(?:abstract\s+)?class\s+$fileName\s*\{") {
        $targetStr = $Matches[0]
        $replacement = "public class $fileName extends AbstractSimulationEngine {" + $methodsToAdd
        $content = $content.Replace($targetStr, $replacement)
        [IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
        $modifiedCount++
        Write-Host "Harmonized: $fileName ($tierEnum)" -ForegroundColor Green
    } else {
        Write-Host "Skipped (complex signature): $fileName" -ForegroundColor Yellow
    }
}

Write-Host "`nTotal engines harmonized: $modifiedCount" -ForegroundColor Cyan
