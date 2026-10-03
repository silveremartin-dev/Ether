#!/bin/bash
set -e
echo '==============================================================================='
echo '   ETHER PLANETARY SIMULATION ENGINE: OVERNIGHT HIGH-FIDELITY BATCH RUNNER   '
echo '==============================================================================='
echo "Timestamp: $(date -u)"
echo "Working Directory: /opt/ether"
echo "Spatial Resolution: H3 Res 5 (2,016,842 cells, ~8.5 km)"
echo "Uniform Time Step: dt = 7.0 days"
echo "Target Suite: 9 Master Epochs + 27 Bifurcations + 25 Engine Ablations"
echo '==============================================================================='

mkdir -p /opt/ether/logs/gcp_batch /opt/ether/logs/calibration /opt/ether/saves
cd /opt/ether

# Launch full master campaign
java -Xms8g -Xmx26g \
     --add-modules=jdk.incubator.vector \
     -cp society-simulation.jar \
     org.ether.society.analytics.HistoricalScenarioCalibrationHarness \
     2>&1 | tee /opt/ether/logs/gcp_batch/overnight_execution.log

echo '>>> Packaging Results Archive...'
tar -czf /opt/ether/logs/gcp_batch/master_results_archive.tar.gz \
    /opt/ether/logs/calibration/ \
    /opt/ether/logs/gcp_batch/overnight_execution.log

echo ">>> Computation Complete at: $(date -u)"
echo '>>> Triggering Immediate Auto-Shutdown Guard ($0.00 Idle Cost)...'
sudo shutdown -h now
