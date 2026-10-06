#!/usr/bin/env bash
# ==============================================================================
# Ether — Javadoc API Documentation Generator (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "============================================================"
echo "  ETHER -- JAVADOC API DOCUMENTATION GENERATOR"
echo "============================================================"
echo ""

echo "[1/2] Invoking Maven Javadoc Plugin..."
mvn javadoc:javadoc -Dshow=protected -Dquiet=true

echo "[2/2] Verifying generated API documentation..."
if [ -f "$ROOT_DIR/target/site/apidocs/index.html" ]; then
    echo ""
    echo "============================================================"
    echo " [SUCCESS] Javadoc successfully generated!"
    echo " Index File: $ROOT_DIR/target/site/apidocs/index.html"
    echo "============================================================"
else
    echo "[WARN] Javadoc output file not found in target/site/apidocs/index.html"
fi
