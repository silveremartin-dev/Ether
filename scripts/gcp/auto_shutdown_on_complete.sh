#!/usr/bin/env bash
# ==============================================================================
# Auto-shutdown monitor for Ether historical falsification campaign
# Ensures all JVM processes terminate cleanly upon completion of the simulation run.
# ==============================================================================
PID_TO_WATCH="${1:-11171}"
LOG_FILE="/home/silve/Ether/phase1_run.log"
MONITOR_LOG="/home/silve/Ether/monitor_shutdown.log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting auto-shutdown monitor for PID ${PID_TO_WATCH}..." > "${MONITOR_LOG}"

while kill -0 "${PID_TO_WATCH}" 2>/dev/null; do
    sleep 10
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Target PID ${PID_TO_WATCH} has exited." >> "${MONITOR_LOG}"
sleep 5

# Ensure any lingering child JVM / surefire / maven processes are terminated
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Enforcing clean shutdown of all remaining JVM processes..." >> "${MONITOR_LOG}"
pkill -9 -f surefire || true
pkill -9 -f society-simulation || true
pkill -9 -f maven || true
pkill -9 -f "org.codehaus.plexus.classworlds" || true

echo "[$(date '+%Y-%m-%d %H:%M:%S')] All JVM processes terminated successfully." >> "${MONITOR_LOG}"

# Optional GCP VM power-off if requested
if [ "$2" = "--shutdown-vm" ] || [ "${SHUTDOWN_VM}" = "true" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Initiating GCP VM power-off (sudo shutdown -h now)..." >> "${MONITOR_LOG}"
    sudo shutdown -h now
fi
