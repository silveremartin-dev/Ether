#!/usr/bin/env bash
# ==============================================================================
# Ether Planetary Simulation Platform — Automated Architecture Compliance Audit
# Linux / macOS Bash Parity Partner for scripts/test/audit-architecture-compliance.ps1
# ==============================================================================
set -euo pipefail

STRICT_EXIT=0
if [[ "${1:-}" == "--strict" || "${1:-}" == "-StrictExitCode" ]]; then
    STRICT_EXIT=1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PATH="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_PATH"

echo "================================================================================"
echo " ETHER ENGINE: ARCHITECTURAL COMPLIANCE & ZERO-DEFECT AUDIT (BASH)"
echo " Reference: docs/CODE_EXCELLENCE_AND_ZERO_DEFECT_DIRECTIVES.md"
echo "================================================================================"

TOTAL_JAVA=$(find src -name "*.java" | wc -l | tr -d ' ')
MAIN_JAVA=$(find src/main/java -name "*.java" ! -name "package-info.java" ! -name "module-info.java" | wc -l | tr -d ' ')
TEST_JAVA=$(find src/test/java -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
echo "Total Java Files Scanned : $TOTAL_JAVA (Main: $MAIN_JAVA | Test: $TEST_JAVA)"

VIOLATIONS=0

# 1. Class Size Ceiling (<= 500 lines for src/main/java)
echo ""
echo "[R1] CLASS SIZE CEILING (<= 500 lines for src/main/java)"
OVER_500=0
while IFS= read -r file; do
    lines=$(wc -l < "$file" | tr -d ' ')
    if (( lines > 500 )); then
        echo "  - $(basename "$file") : $lines lines ($file)"
        ((OVER_500++)) || true
    fi
done < <(find src/main/java -name "*.java" ! -name "package-info.java" ! -name "module-info.java")

if (( OVER_500 == 0 )); then
    echo "  STATUS: PASS (100% of $MAIN_JAVA main production classes are <= 500 lines)"
else
    echo "  STATUS: FAIL ($OVER_500 production file(s) exceed 500 lines)"
    ((VIOLATIONS += OVER_500)) || true
fi

# 2. Zero Silent Exception Swallowing
echo ""
echo "[R2] ZERO SILENT EXCEPTION SWALLOWING (Production sources)"
EMPTY_CATCH=$(grep -rnPzo 'catch\s*\([^)]+\)\s*\{\s*(?://[^\r\n]*\s*|/\*[\s\S]*?\*/\s*)*\}' src/main/java 2>/dev/null | tr '\0' '\n' | grep -c "catch" || true)
if (( EMPTY_CATCH == 0 )); then
    echo "  STATUS: PASS (0 silent empty catch blocks in production sources)"
else
    echo "  STATUS: FAIL ($EMPTY_CATCH silent empty catch block(s) found)"
    ((VIOLATIONS += EMPTY_CATCH)) || true
fi

# 3. Zero Duplicate Class Names Across Production Packages
echo ""
echo "[R3] ZERO DUPLICATE CLASS NAMES ACROSS PRODUCTION PACKAGES"
DUPS=$(find src/main/java -name "*.java" ! -name "package-info.java" ! -name "module-info.java" -exec basename {} \; | sort | uniq -d | wc -l | tr -d ' ')
if (( DUPS == 0 )); then
    echo "  STATUS: PASS (0 duplicate class names across main sources)"
else
    echo "  STATUS: FAIL ($DUPS duplicate class name(s) found)"
    ((VIOLATIONS += DUPS)) || true
fi

echo ""
echo "================================================================================"
echo " AUDIT SUMMARY"
echo "================================================================================"
echo "  R1. Classes > 500 lines in src/main : $OVER_500"
echo "  R2. Silent empty catch blocks       : $EMPTY_CATCH"
echo "  R3. Duplicate class names in main   : $DUPS"
echo "================================================================================"

if (( VIOLATIONS == 0 )); then
    echo "   AUDIT SUCCESS: 100% CODEBASE ARCHITECTURAL COMPLIANCE!"
    exit 0
else
    echo "   AUDIT COMPLETED: $VIOLATIONS issue(s) flagged for remediation."
    if (( STRICT_EXIT == 1 )); then
        exit 1
    fi
    exit 0
fi
