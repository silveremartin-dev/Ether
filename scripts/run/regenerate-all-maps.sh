#!/usr/bin/env bash
# ==============================================================================
# Ether Historical Cartographic Tensor Regenerator (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "==============================================================================="
echo "               ETHER HISTORICAL CARTOGRAPHIC TENSOR REGENERATOR                "
echo "==============================================================================="
echo ""
echo "[ATTENTION / WARNING]"
echo "1. You MUST have downloaded all empirical raw geospatial datasets beforehand."
echo "2. RUNNING THIS SCRIPT WILL OVERWRITE ALL EXISTING CARTOGRAPHIC TENSORS"
echo "   in data/maps/ether/earth/ across all historical epochs (-100,000 to 2060)."
echo ""
echo "==============================================================================="
echo ""

read -p "Are you sure you want to proceed and overwrite all maps? (y/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo "[ABORTED] Operation cancelled by user. No maps were modified."
    exit 0
fi

echo ""
echo "[*] Launching Master Cartographic Batch Regeneration Suite via Maven..."
echo ""

mvn test -Dtest=BatchRegenerateAllScenarioMapsTest -DfailIfNoSpecifiedTests=false

echo ""
echo "==============================================================================="
echo "[SUCCESS] All 25 layers, cultural registries, provenance, and READMEs"
echo "          have been successfully regenerated across all historical epochs!"
echo "==============================================================================="
