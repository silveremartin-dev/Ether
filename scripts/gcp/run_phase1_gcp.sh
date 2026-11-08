#!/usr/bin/env bash
set -e
cd /home/silve/Ether
export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xms12g -Xmx40g"
nohup bash -c 'mvn test -Pacademic-paper -Dtest=SpatialHoldoutCrossValidationTest,LongHorizonFreeRunFalsificationTest,MacroHistoricalEpochsFalsificationTest -DexcludedGroups=none > phase1_run.log 2>&1; sudo shutdown -h now' >/dev/null 2>&1 &
RUN_PID=$!
echo $RUN_PID > run.pid
echo "Full Pillar 6 (SpatialHoldout, LongHorizonFreeRun, MacroHistoricalEpochs) launched on 12-vCPU/48GB GCP VM with PID $RUN_PID (Auto-shutdown active)"
