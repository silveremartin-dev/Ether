#!/usr/bin/env bash
# ==============================================================================
# Ether Planetary Simulation Platform — Full Code Quality & Naming Audit
# Linux / macOS Bash Parity Partner for scripts/test/audit-full-quality-and-naming.ps1
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PATH="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_PATH"

echo "=========================================================="
echo "   ETHER ENGINE: COMPREHENSIVE ARCHITECTURE & QUALITY AUDIT"
echo "=========================================================="

SRC_MAIN="src/main/java"
TOTAL=$(find "$SRC_MAIN" -name "*.java" | wc -l | tr -d ' ')
echo "Scanning $TOTAL Java source files in $SRC_MAIN..."
echo ""

echo "--- [1. Naming & Archetypes Analysis] ---"
MANAGERS=$(find "$SRC_MAIN" -name "*Manager.java" | wc -l | tr -d ' ')
CATALOGS=$(find "$SRC_MAIN" -name "*Catalog.java" | wc -l | tr -d ' ')
EVALUATORS=$(find "$SRC_MAIN" -name "*Evaluator.java" | wc -l | tr -d ' ')
CALCULATORS=$(find "$SRC_MAIN" -name "*Calculator.java" | wc -l | tr -d ' ')
UTILS=$(find "$SRC_MAIN" \( -name "*Util.java" -o -name "*Utils.java" \) | wc -l | tr -d ' ')
PROC=$(find "$SRC_MAIN" -name "*ProceduralEngine*.java" | wc -l | tr -d ' ')

echo "Legacy *Manager classes:     $MANAGERS"
echo "Legacy *Catalog classes:     $CATALOGS"
echo "Legacy *Evaluator classes:   $EVALUATORS"
echo "Legacy *Calculator classes:  $CALCULATORS"
echo "Legacy *Util/*Utils classes: $UTILS"
echo "ProceduralEngine remnants:   $PROC"

echo ""
echo "--- [2. Line Count Limit (<= 500 Lines)] ---"
OVER_LIMIT=0
while IFS= read -r file; do
    lines=$(wc -l < "$file" | tr -d ' ')
    if (( lines > 500 )); then
        echo "  -> $file ($lines lines)"
        ((OVER_LIMIT++)) || true
    fi
done < <(find "$SRC_MAIN" -name "*.java")
echo "Classes exceeding 500 lines: $OVER_LIMIT"

echo ""
echo "--- [5. Determinism & Random Invariants (Tier 1 & Tier 2)] ---"
UNSEEDED=$(grep -rnE '\bnew\s+Random\(\)|Math\.random\(\)' src/main/java/org/ether/society/engines src/main/java/org/ether/society/core 2>/dev/null | wc -l | tr -d ' ' || true)
echo "Unseeded Random or Math.random() in simulation/engines/core: $UNSEEDED"

echo ""
echo "=========================================================="
echo "   AUDIT SCAN COMPLETE"
echo "=========================================================="
