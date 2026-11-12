$theoryFiles = Get-ChildItem -Path src/main/java/org/ether/society/engines/tier2/theories -Filter *.java
foreach ($f in $theoryFiles) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    if ($c -match 'implements ProceduralEnginePlugin') {
        $name = $f.BaseName
        # Replace class declaration
        $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'public class ' + [System.Text.RegularExpressions.Regex]::Escape($name) + '\s+implements\s+[^\{]+\{', "public class $name extends AbstractSimulationEngine {`r`n    public $name() {`r`n        super(`"$name`", `"$name`", EngineTier.TIER_2_CLIODYNAMIC);`r`n    }")
        if (-not ($c.Contains('import org.ether.society.core.engine.AbstractSimulationEngine;'))) {
            $c = $c.Replace('package org.ether.society.engines.tier2.theories;', "package org.ether.society.engines.tier2.theories;`r`n`r`nimport org.ether.society.core.engine.AbstractSimulationEngine;`r`nimport org.ether.society.core.engine.EngineTier;")
        }
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        Write-Output ("Migrated theory: " + $name)
    }
}

$histFiles = Get-ChildItem -Path src/main/java/org/ether/society/engines/tier2/historical -Filter *.java
foreach ($f in $histFiles) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    if ($c -match 'implements ProceduralEnginePlugin') {
        $name = $f.BaseName
        $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'public class ' + [System.Text.RegularExpressions.Regex]::Escape($name) + '\s+implements\s+[^\{]+\{', "public class $name extends AbstractSimulationEngine {`r`n    public $name() {`r`n        super(`"$name`", `"$name`", EngineTier.TIER_2_CLIODYNAMIC);`r`n    }")
        if (-not ($c.Contains('import org.ether.society.core.engine.AbstractSimulationEngine;'))) {
            $c = $c.Replace('package org.ether.society.engines.tier2.historical;', "package org.ether.society.engines.tier2.historical;`r`n`r`nimport org.ether.society.core.engine.AbstractSimulationEngine;`r`nimport org.ether.society.core.engine.EngineTier;")
        }
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        Write-Output ("Migrated historical: " + $name)
    }
}

$tier1Files = Get-ChildItem -Path src/main/java/org/ether/society/engines/tier1 -Filter *.java
foreach ($f in $tier1Files) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    if ($c -match 'implements ProceduralEnginePlugin') {
        $name = $f.BaseName
        $c = [System.Text.RegularExpressions.Regex]::Replace($c, 'public class ' + [System.Text.RegularExpressions.Regex]::Escape($name) + '\s+implements\s+[^\{]+\{', "public class $name extends AbstractSimulationEngine {`r`n    public $name() {`r`n        super(`"$name`", `"$name`", EngineTier.TIER_1_BIOPHYSICAL);`r`n    }")
        if (-not ($c.Contains('import org.ether.society.core.engine.AbstractSimulationEngine;'))) {
            $c = $c.Replace('package org.ether.society.engines.tier1;', "package org.ether.society.engines.tier1;`r`n`r`nimport org.ether.society.core.engine.AbstractSimulationEngine;`r`nimport org.ether.society.core.engine.EngineTier;")
        }
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        Write-Output ("Migrated tier1: " + $name)
    }
}
