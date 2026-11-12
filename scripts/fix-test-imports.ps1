$testFiles = Get-ChildItem -Path src/test/java -Recurse -Filter *.java
foreach ($f in $testFiles) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $modified = $false
    if ($c.Contains('SimulationEngineRegistry') -and -not ($c.Contains('import org.ether.society.core.engine.SimulationEngineRegistry;'))) {
        $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'package\s+([^;]+);', "package `$1;`r`n`r`nimport org.ether.society.core.engine.SimulationEngineRegistry;`r`nimport org.ether.society.core.engine.SimulationEngineModule;")
        $modified = $true
    }
    if ($c.Contains('ProceduralEnginePlugin')) {
        $c = $c.Replace('ProceduralEnginePlugin', 'SimulationEngineModule')
        $modified = $true
    }
    if ($modified) {
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        Write-Output ("Cleaned test imports: " + $f.BaseName)
    }
}
