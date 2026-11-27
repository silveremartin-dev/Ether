#!/usr/bin/env bash
# ==============================================================================
# Ether Scientific Platform - Workspace Cleanup Utility (Linux / macOS / POSIX)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
cd "$ROOT_DIR"

echo "=========================================================="
echo "   Ether Scientific Platform - Workspace Cleanup Utility  "
echo "=========================================================="
echo "Workspace Root: $ROOT_DIR"

CLEAN_SAVES=false
PURGE_ALL_SAVES=false
CLEAN_LOGS=false
PURGE_ALL_LOGS=false
CLEAN_SCRATCH=false
CLEAN_DIST=false
CLEAN_TARGET=false

if [ "$#" -eq 0 ]; then
    CLEAN_SAVES=true
    CLEAN_LOGS=true
    CLEAN_SCRATCH=true
else
    for arg in "$@"; do
        case "$arg" in
            --all)
                CLEAN_SAVES=true
                CLEAN_LOGS=true
                CLEAN_SCRATCH=true
                ;;
            --saves) CLEAN_SAVES=true ;;
            --purge-all-saves) CLEAN_SAVES=true; PURGE_ALL_SAVES=true ;;
            --logs) CLEAN_LOGS=true ;;
            --purge-all-logs) CLEAN_LOGS=true; PURGE_ALL_LOGS=true ;;
            --scratch) CLEAN_SCRATCH=true ;;
            --dist) CLEAN_DIST=true ;;
            --target) CLEAN_TARGET=true ;;
        esac
    done
fi

if [ "$CLEAN_SAVES" = true ] && [ -d "saves" ]; then
    echo "[1/5] Cleaning saves/..."
    if [ "$PURGE_ALL_SAVES" = true ]; then
        rm -rf saves/*
    else
        find saves/ -maxdepth 1 -mindepth 1 -type d \( -name "*-*-*-*-*" -o -name "Test_Save_*" -o -name "AUTOSAVE_*" \) -exec rm -rf {} + 2>/dev/null || true
    fi
fi

if [ "$CLEAN_SCRATCH" = true ]; then
    echo "[2/5] Cleaning scratch/, target/reports/, and stray root files..."
    rm -rf scratch target/reports dependency-reduced-pom.xml
    find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
fi

if [ "$CLEAN_LOGS" = true ] && [ -d "logs" ]; then
    echo "[3/5] Cleaning logs/..."
    if [ "$PURGE_ALL_LOGS" = true ]; then
        rm -rf logs/*
    else
        find logs/ -type f ! -name "society-simulation.log" -delete 2>/dev/null || true
    fi
fi

if [ "$CLEAN_DIST" = true ] && [ -d "dist" ]; then
    echo "[4/5] Cleaning dist/..."
    rm -rf dist/*
fi

if [ "$CLEAN_TARGET" = true ] && [ -d "target" ]; then
    echo "[5/5] Cleaning target/..."
    rm -rf target/
fi

echo "=========================================================="
echo "   Cleanup Complete! Workspace is sanitized and healthy.   "
echo "=========================================================="
