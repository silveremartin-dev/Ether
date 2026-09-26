#!/usr/bin/env bash
# ==============================================================================
# Ether — Docker Compose Deployment Helper (Linux / macOS / POSIX)
#
# Usage:
#   ./scripts/docker-deploy.sh [MODE] [SCENARIO] [TICKS] [CELLS] [WORKERS]
#
# MODE options:
#   dev       — Start only postgres + redis (local development, default)
#   headless  — Run a single batch simulation to completion then exit
#   cluster   — Start master + N worker nodes
#
# Examples:
#   ./scripts/docker-deploy.sh
#   ./scripts/docker-deploy.sh headless OUT_OF_AFRICA 1000 10000
#   ./scripts/docker-deploy.sh cluster MESOPOTAMIA_BRONZE_AGE 500 5000 3
# ==============================================================================

set -euo pipefail

MODE="${1:-dev}"
SCENARIO="${2:-OUT_OF_AFRICA}"
TICKS="${3:-500}"
CELLS="${4:-5000}"
WORKERS="${5:-1}"

echo "=========================================================="
echo "     ETHER — DOCKER DEPLOY HELPER"
echo "=========================================================="
echo "  Mode       : ${MODE}"
echo "  Scenario   : ${SCENARIO}"
echo "  Ticks      : ${TICKS}"
echo "  Cells      : ${CELLS}"
echo "  Workers    : ${WORKERS}"
echo "----------------------------------------------------------"

# Ensure .env exists
if [ ! -f ".env" ]; then
  echo "[!] .env file not found. Copying from .env.example..."
  cp .env.example .env
  echo "[!] Please review and edit .env before running in production."
fi

# Export overrides into environment so docker compose picks them up
export SCENARIO="${SCENARIO}"
export TICKS="${TICKS}"
export CELLS="${CELLS}"

case "${MODE}" in
  dev)
    echo "[1/2] Starting infrastructure (postgres + redis)..."
    docker compose up -d postgres redis
    echo ""
    echo "✅ Dev infrastructure is up."
    echo "  PostgreSQL : localhost:54320"
    echo "  Redis      : localhost:6379"
    ;;

  headless)
    echo "[1/2] Starting infrastructure..."
    docker compose up -d postgres redis
    echo "[2/2] Running headless simulation (scenario=${SCENARIO}, ticks=${TICKS}, cells=${CELLS})..."
    docker compose --profile headless up --build ether-headless
    echo ""
    echo "✅ Simulation complete. Results saved to ./saves/"
    ;;

  cluster)
    echo "[1/2] Starting infrastructure..."
    docker compose up -d postgres redis
    echo "[2/2] Starting cluster (master + ${WORKERS} worker(s))..."
    docker compose --profile cluster up -d --build --scale ether-worker="${WORKERS}"
    echo ""
    echo "✅ Cluster is up."
    echo "  Master RPC : localhost:9090"
    echo "  Workers    : ${WORKERS} replica(s)"
    echo ""
    echo "  Monitor logs : docker compose --profile cluster logs -f"
    echo "  Tear down    : docker compose --profile cluster down"
    ;;

  down)
    echo "Stopping all Ether containers..."
    docker compose --profile headless --profile cluster down
    echo "✅ All stopped."
    ;;

  *)
    echo "ERROR: Unknown mode '${MODE}'. Valid: dev | headless | cluster | down"
    exit 1
    ;;
esac
