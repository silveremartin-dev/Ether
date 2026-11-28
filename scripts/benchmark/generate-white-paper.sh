#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 - Master White Paper & Academic Manuscript Compiler (Linux/macOS)
#
# Modes:
#   1. Default (fast recompile from existing saves/):
#      ./scripts/benchmark/generate-white-paper.sh
#   2. Full Academic Falsification Pipeline (Pillars 4, 5, 6, 7 & 8 + White Paper):
#      ./scripts/benchmark/generate-white-paper.sh --with-calibration-and-free-runs
#      (or alias --full-academic-pipeline)
#
# Included suites in full pipeline mode:
#   - HistoricalScenarioCalibrationHarnessTest       (Steady Regimes, Twins & Detective)
#   - CounterfactualInverseFalsificationTest         (Pillar 4: Negative Controls & Knock-Outs)
#   - MasterHistoricalBifurcationSuite               (Pillar 5: Historical Bifurcations)
#   - LongHorizonFreeRunFalsificationTest            (Pillar 6: 500-yr & 250-yr Free-Runs)
#   - SpatialHoldoutCrossValidationTest              (Pillar 7: 7 Non-Eurasian Hold-Outs)
#   - ScientificModelFalsificationAndValidationSuite (Pillar 6 & 8: Benchmarks & Debates)
#   - WhitePaperCompiler#compileMasterWhitePaper     (Charts, White Paper & Manuscript)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

OUTPUT_DIR="target/reports"
FULL_PIPELINE=false

for arg in "$@"; do
    case "$arg" in
        --with-calibration-and-free-runs|--full-academic-pipeline)
            FULL_PIPELINE=true
            ;;
        --output-dir=*)
            OUTPUT_DIR="${arg#*=}"
            ;;
        -*)
            echo "Unknown option: $arg" >&2
            exit 1
            ;;
        *)
            OUTPUT_DIR="$arg"
            ;;
    esac
done

TARGET_FILE="${OUTPUT_DIR}/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
ACADEMIC_FILE="docs/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"

echo "================================================================================"
echo " [ETHER] MASTER WHITE PAPER & ACADEMIC MANUSCRIPT COMPILER"
echo "================================================================================"
echo "  White Paper Output : ${TARGET_FILE}"
echo "  Academic Paper     : ${ACADEMIC_FILE}"
echo "  Full Pipeline Mode : ${FULL_PIPELINE}"
echo "--------------------------------------------------------------------------------"

export MAVEN_OPTS="-Xms4g -Xmx12g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
START_TIME=$(date +%s)

if [ "${FULL_PIPELINE}" = true ]; then
    SUITES="HistoricalScenarioCalibrationHarnessTest,CounterfactualInverseFalsificationTest,MasterHistoricalBifurcationSuite,LongHorizonFreeRunFalsificationTest,SpatialHoldoutCrossValidationTest,ScientificModelFalsificationAndValidationSuite,WhitePaperCompiler#compileMasterWhitePaper"
    echo "[1/2] Executing Complete Academic Falsification Suites (Pillars 4-8) & WhitePaperCompiler..."
    mvn test -Pacademic-paper -Dgroups= "-Dtest=${SUITES}" -Djacoco.skip=true -B
else
    echo "[1/2] Invoking WhitePaperCompiler harness from persisted saves/..."
    mvn test -Dgroups= "-Dtest=WhitePaperCompiler#compileMasterWhitePaper" -Djacoco.skip=true -B
fi

END_TIME=$(date +%s)
ELAPSED=$(( END_TIME - START_TIME ))

test -f "${TARGET_FILE}"
test -f "${ACADEMIC_FILE}"

echo "================================================================================"
echo " [SUCCESS] SCIENTIFIC DOCUMENTS UPDATED IN ${ELAPSED} SECONDS!"
echo "   White Paper    : ${TARGET_FILE}"
echo "   Academic Paper : ${ACADEMIC_FILE}"
echo "================================================================================"
