#!/usr/bin/env bash
set -e
cd /home/silve/Ether
echo "=== Starting MacroHistoricalEpochsFalsificationTest Benchmark Campaign ==="
echo "Timestamp: $(date -u)"
nohup mvn test -Pacademic-paper -Dtest=MacroHistoricalEpochsFalsificationTest -Djacoco.skip=true > /home/silve/b3_scenario_benchmark_round.log 2>&1 &
RUN_PID=$!
echo "Benchmark process launched in background with PID: $RUN_PID"
sleep 4
ps aux | grep -E '(mvn|surefirebooter|java)'
