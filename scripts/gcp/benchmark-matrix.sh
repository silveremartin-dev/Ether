#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Automated Multi-Scenario x Multi-Resolution Benchmark Suite (Bash)
# Measures steady-state TPS, per-node throughput, and memory scaling.
# ==============================================================================
set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
TICKS="${3:-24}"
OUTPUT_FILE="${4:-logs/benchmarks/matrix_benchmark_results.json}"

SCENARIOS=("OUT_OF_AFRICA" "NEOLITHIZATION" "CLASSICAL" "INDUSTRIAL" "MODERN")
RESOLUTIONS=(2 3 4)

mkdir -p "$(dirname "$OUTPUT_FILE")"

echo "=========================================================="
echo "     ETHER — MULTI-DIMENSIONAL BENCHMARK SUITE MATRIX     "
echo "=========================================================="
echo "  Project ID    : ${PROJECT_ID}"
echo "  Zone          : ${ZONE}"
echo "  Ticks per Run : ${TICKS}"
echo "  Scenarios     : ${SCENARIOS[*]}"
echo "  Resolutions   : ${RESOLUTIONS[*]}"
echo "----------------------------------------------------------"

RESULTS="[]"

for SCENARIO in "${SCENARIOS[@]}"; do
    for RES in "${RESOLUTIONS[@]}"; do
        echo -e "\n>>> [BENCHMARK] Scenario: ${SCENARIO} | H3 Resolution: ${RES} | Ticks: ${TICKS} <<<"
        START_TIME=$(date +%s)
        
        OUTPUT=$(./scripts/gcp/deploy-and-run.sh "${PROJECT_ID}" "${ZONE}" "${SCENARIO}" "${TICKS}" 0 true "${RES}" || true)
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        
        TPS=$(echo "$OUTPUT" | grep -oP 'Throughput:\s+\K[0-9.]+' || echo "0.0")
        CELLS=$(echo "$OUTPUT" | grep -oP 'Initializing grid with \K[0-9]+' || echo "0")
        COHORTS=$(echo "$OUTPUT" | grep -oP 'Initialized \K[0-9]+(?= demographic)' || echo "0")
        
        echo ">>> Result: ${TPS} TPS | Duration: ${DURATION}s | Cohorts: ${COHORTS} | Cells: ${CELLS} <<<"
    done
done

echo "=========================================================="
echo " Benchmark Suite Complete."
echo "=========================================================="
