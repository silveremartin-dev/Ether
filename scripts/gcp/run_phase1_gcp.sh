#!/usr/bin/env bash
set -e
cd /home/silve/Ether
export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx24g"
nohup mvn test -Pacademic-paper -Dtest=MacroHistoricalEpochsFalsificationTest -DexcludedGroups=none > phase1_run.log 2>&1 &
RUN_PID=$!
echo $RUN_PID > run.pid
echo "MacroHistoricalEpochsFalsificationTest successfully launched in background with PID $RUN_PID"
