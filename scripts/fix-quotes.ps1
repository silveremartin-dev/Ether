$path = 'src/main/java/org/ether/society/engines/tier2/theories/StructuralDemographicBifurcationEngine.java'
$lines = [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'super\(') {
        $lines[$i] = '        super("StructuralDemographicBifurcationEngine", "StructuralDemographicBifurcationEngine", EngineTier.TIER_2_CLIODYNAMIC);'
    }
}
[System.IO.File]::WriteAllLines($path, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Cleaned quotes in StructuralDemographicBifurcationEngine"
