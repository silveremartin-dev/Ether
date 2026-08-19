#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Database Status Check
# ==============================================================================
cd "$(dirname "$0")/.."

echo "========================================"
echo "Database Container Status"
echo "========================================"
docker-compose ps
echo ""
echo "To view logs: docker-compose logs -f postgres"
