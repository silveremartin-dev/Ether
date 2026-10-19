#!/usr/bin/env bash
# ==============================================================================
# Ether Scientific Platform - Workspace Cleanup Utility
# ==============================================================================
# Safely purges:
# - Orphan UUID test saves in saves/ (preserving canonical RUN-* benchmark saves)
# - Rotated daily logs in logs/*.log (preserving society-simulation.log)
# - Temporary Maven target/ artifacts (optional with --target or --all)
# - Release distributions in dist/ (optional with --dist or --all)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"

cd "$ROOT_DIR"

echo "=========================================================="
echo "   Ether Scientific Platform - Workspace Cleanup Utility  "
echo "=========================================================="
echo "Workspace Root: $ROOT_DIR"
echo ""

CLEAN_SAVES=false
CLEAN_LOGS=false
CLEAN_DIST=false
CLEAN_TARGET=false

if [ "$#" -eq 0 ] || [ "$1" = "--all" ]; then
    CLEAN_SAVES=true
    CLEAN_LOGS=true
elif [ "$1" = "--saves" ]; then
    CLEAN_SAVES=true
elif [ "$1" = "--logs" ]; then
    CLEAN_LOGS=true
elif [ "$1" = "--dist" ]; then
    CLEAN_DIST=true
elif [ "$1" = "--target" ]; then
    CLEAN_TARGET=true
fi

# [1/4] Clean orphan UUID test saves
if [ "$CLEAN_SAVES" = true ] && [ -d "saves" ]; then
    echo "[1/4] Cleaning orphan UUID test saves in saves/..."
    find saves/ -maxdepth 1 -mindepth 1 -type d -name "*-*-*-*-*" -exec rm -rf {} + 2>/dev/null || true
    echo "  Cleaned orphan test runs, preserved canonical RUN-* datasets."
fi

# [2/4] Clean rotated logs
if [ "$CLEAN_LOGS" = true ] && [ -d "logs" ]; then
    echo "[2/4] Cleaning rotated logs in logs/..."
    find logs/ -name "*.2026-*.log" -type f -delete 2>/dev/null || true
    find logs/gcp_batch -name "*.log" -type f -delete 2>/dev/null || true
    echo "  Cleaned rotated logs, preserved active society-simulation.log."
fi

# [3/4] Clean dist packages
if [ "$CLEAN_DIST" = true ] && [ -d "dist" ]; then
    echo "[3/4] Cleaning dist package archives..."
    rm -rf dist/*
    echo "  Cleaned dist/."
fi

# [4/4] Clean Maven target
if [ "$CLEAN_TARGET" = true ] && [ -d "target" ]; then
    echo "[4/4] Cleaning Maven target directory..."
    rm -rf target/
    echo "  Cleaned target/."
fi

echo ""
echo "=========================================================="
echo "   Cleanup Complete! Workspace is sanitized and healthy.   "
echo "=========================================================="
