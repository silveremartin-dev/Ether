$path = 'src/main/java/org/ether/society/engines/tier2/theories/StructuralDemographicBifurcationEngine.java'
$content = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$target = @"
public class StructuralDemographicBifurcationEngine extends AbstractSimulationEngine {
    public StructuralDemographicBifurcationEngine() {
        super("StructuralDemographicBifurcationEngine", "StructuralDemographicBifurcationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(StructuralDemographicBifurcationEngine.class);
"@
$replacement = @"
public class StructuralDemographicBifurcationEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(StructuralDemographicBifurcationEngine.class);
"@
$content = $content.Replace($target, $replacement)
[System.IO.File]::WriteAllText($path, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Removed duplicate constructor in StructuralDemographicBifurcationEngine"
