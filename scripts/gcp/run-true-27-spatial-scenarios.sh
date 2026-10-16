#!/usr/bin/env bash
# Forwarding wrapper for the comprehensive master 45 scenarios suite
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run-all-45-scenarios.sh" "$@"
