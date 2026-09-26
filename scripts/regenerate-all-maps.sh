#!/usr/bin/env bash
set -e

echo "==============================================================================="
echo "               ETHER HISTORICAL CARTOGRAPHIC TENSOR REGENERATOR"
echo "==============================================================================="
echo ""
echo "[ATTENTION / WARNING]"
echo "1. You MUST have downloaded all empirical raw geospatial datasets beforehand:"
echo "   - WorldClim v2.1 GeoTIFFs (wc2.1_10m_bio_1.tif, bio_4.tif, bio_12.tif)"
echo "   - NOAA ETOPO 2022 relief and GEBCO 2023 grids"
echo "   - HYDE 3.4 historical population grids in data/sources/"
echo "   - USGS and Global Energy Monitor (GEM) mineral/petroleum datasets"
echo ""
echo "2. RUNNING THIS SCRIPT WILL OVERWRITE ALL EXISTING CARTOGRAPHIC TENSORS"
echo "   in data/maps/ether/earth/ across all historical epochs (-100,000 to 2060)."
echo ""
echo "==============================================================================="
echo ""

read -r -p "Are you sure you want to proceed and overwrite all maps? (y/N): " CONFIRM
if [[ "$CONFIRM" != [yY] && "$CONFIRM" != [yY][eE][sS] ]]; then
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
