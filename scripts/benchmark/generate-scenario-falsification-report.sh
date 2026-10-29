#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 - Standardized Cliodynamic Scenario Falsification Report (Linux/macOS)
# ==============================================================================
set -e

RUNS="${1:-RUN-LA-PESTE-NOIRE---INVERSION--CONOMIQUE-POST-F-ODALE--1347--2282 RUN-GUERRES-MONDIALES--RUPTURE-TOTALITAIRE----RE-NUCL-AIRE--1914--5889 RUN-RAMS-S-II--PAX-AEGYPTIACA---B-TISSEURS-MONUMENTAUX---1279--3618}"
OUTPUT_DIR="${2:-scratch}"

echo "================================================================================"
echo " [ETHER] SCENARIO FALSIFICATION AUDIT GENERATOR (HYDE 3.4 / MADDISON / EPICA)   "
echo "================================================================================"
echo "  Target Scenarios : $RUNS"
echo "  Output Directory : $OUTPUT_DIR"
echo "--------------------------------------------------------------------------------"

START_TIME=$(date +%s)

mvn test -Dtest=ScenarioFalsificationReportTest -Druns="$RUNS" -Djacoco.skip=true

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

REPORT_PATH="$OUTPUT_DIR/scenario_falsification_report.md"
if [ -f "$REPORT_PATH" ]; then
    echo "================================================================================"
    echo " [SUCCESS] FALSIFICATION REPORT GENERATED IN ${ELAPSED}s!"
    echo "   Path: $REPORT_PATH"
    echo "================================================================================"
else
    echo "Error: Report file not found at $REPORT_PATH" >&2
    exit 1
fi
