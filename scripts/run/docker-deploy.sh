#!/usr/bin/env bash
# ==============================================================================
# Ether -- Docker Compose Deployment Helper (Linux / macOS)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

MODE="${1:-dev}"
SCENARIO="${2:-OUT_OF_AFRICA}"
TICKS="${3:-500}"
CELLS="${4:-5000}"
WORKERS="${5:-1}"

echo "=========================================================="
echo "     ETHER -- DOCKER DEPLOY HELPER (Linux/macOS)          "
echo "=========================================================="
echo "  Mode       : $MODE"
echo "  Scenario   : $SCENARIO"
echo "  Ticks      : $TICKS"
echo "  Cells      : $CELLS"
echo "  Workers    : $WORKERS"
echo "----------------------------------------------------------"

if [ ! -f ".env" ]; then
    echo "[!] .env file not found. Copying from .env.example..."
    cp .env.example .env
fi

DC="docker compose"
if ! docker compose version >/dev/null 2>&1; then
    DC="docker-compose"
fi

case "$MODE" in
    dev)
        echo "[1/2] Starting infrastructure (postgres + redis)..."
        $DC up -d postgres redis
        echo ""
        echo "[OK] Dev infrastructure is up."
        echo "  PostgreSQL : localhost:54320"
        echo "  Redis      : localhost:6379"
        ;;
    headless)
        echo "[1/2] Starting infrastructure..."
        $DC up -d postgres redis
        echo "[2/2] Running headless simulation..."
        SCENARIO=$SCENARIO TICKS=$TICKS CELLS=$CELLS $DC --profile headless up --build ether-headless
        echo ""
        echo "[OK] Simulation complete. Results saved to ./saves/"
        ;;
    cluster)
        echo "[1/2] Starting infrastructure..."
        $DC up -d postgres redis
        echo "[2/2] Starting cluster (master + $WORKERS workers)..."
        SCENARIO=$SCENARIO TICKS=$TICKS CELLS=$CELLS $DC --profile cluster up -d --build --scale ether-worker=$WORKERS
        echo ""
        echo "[OK] Cluster is up."
        echo "  Master RPC : localhost:9090"
        echo "  Workers    : $WORKERS replica(s)"
        ;;
    down)
        echo "Stopping all Ether containers..."
        $DC --profile headless --profile cluster down
        echo "[OK] All stopped."
        ;;
    *)
        echo "ERROR: Unknown mode '$MODE'. Valid: dev | headless | cluster | down"
        exit 1
        ;;
esac
