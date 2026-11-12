$files = Get-ChildItem -Path src/main/java/org/ether/society/engines -Recurse -Filter *.java | Where-Object { 
    $txt = [System.IO.File]::ReadAllText($_.FullName)
    $txt -match 'implements ProceduralEnginePlugin'
}
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'implements\s+ProceduralEnginePlugin\s*,\s*SimulationEngineModule', 'extends AbstractSimulationEngine')
    $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'implements\s+SimulationEngineModule\s*,\s*ProceduralEnginePlugin', 'extends AbstractSimulationEngine')
    $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'implements\s+ProceduralEnginePlugin', 'extends AbstractSimulationEngine')
    if (-not ($c.Contains('import org.ether.society.core.engine.AbstractSimulationEngine;'))) {
        $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'package\s+([^;]+);', "package `$1;`r`n`r`nimport org.ether.society.core.engine.AbstractSimulationEngine;`r`nimport org.ether.society.core.engine.EngineTier;")
    }
    [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("Cleaned plugin interface on: " + $f.BaseName)
}
