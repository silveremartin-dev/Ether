# ==============================================================================
# Ether 1.0 — Launch Master Simulation Suite in Resolution 3 on GCP
# Wrapper invoking launch-overnight-suite.ps1 with -Res3 flag
# ==============================================================================

param (
    [switch]$ForceRegenerate = $false
)

& "$PSScriptRoot\launch-overnight-suite.ps1" -Res3 -ForceRegenerate:$ForceRegenerate
