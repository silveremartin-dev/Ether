#!/usr/bin/env bash
# ==============================================================================
# Ether — Database & Docker Infrastructure Status Check (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "============================================================"
echo "  ETHER -- DATABASE & DOCKER INFRASTRUCTURE STATUS"
echo "============================================================"
echo ""

if ! docker info >/dev/null 2>&1; then
    echo "[STATUS] Docker Daemon : [OFFLINE / NOT RUNNING]"
    echo ""
    echo "Please start the Docker daemon to inspect containers."
    echo "============================================================"
    exit 1
fi
echo "[STATUS] Docker Daemon : [ONLINE]"

echo ""
echo "[CONTAINERS]"
if docker compose version >/dev/null 2>&1; then
    docker compose ps
else
    docker-compose ps
fi

echo ""
echo "[SERVICE HEALTH CHECKS]"

# Check PostgreSQL
if (docker compose exec -T postgres pg_isready -U ether 2>/dev/null || docker-compose exec -T postgres pg_isready -U ether 2>/dev/null); then
    echo "  - PostgreSQL (Port 5432/54320) : [ONLINE / READY]"
else
    echo "  - PostgreSQL (Port 5432/54320) : [OFFLINE / UNREACHABLE]"
fi

# Check Redis
if (docker compose exec -T redis redis-cli ping 2>/dev/null || docker-compose exec -T redis redis-cli ping 2>/dev/null); then
    echo "  - Redis      (Port 6379)       : [ONLINE / PONG]"
else
    echo "  - Redis      (Port 6379)       : [OFFLINE / UNREACHABLE]"
fi

echo ""
echo "============================================================"
echo "Useful commands:"
echo "  View Postgres logs : docker compose logs -f postgres"
echo "  View Redis logs    : docker compose logs -f redis"
echo "  Start services     : ./scripts/run/docker-deploy.sh dev"
echo "  Stop services      : ./scripts/run/stop-docker.sh"
echo "============================================================"
