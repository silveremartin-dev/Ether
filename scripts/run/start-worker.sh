#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Start Cluster Compute Worker Node (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

MASTER_HOST="${1:-127.0.0.1}"
PORT="${2:-9090}"
SECRET="${3:-EtherClusterSecret2026}"

echo "=========================================================="
echo "     ETHER -- STARTING CLUSTER WORKER NODE (Linux)        "
echo "=========================================================="
echo "  Master Host : $MASTER_HOST"
echo "  Port        : $PORT"
echo "----------------------------------------------------------"

EXEC_JAR=""
if [ -f "bin/ether.jar" ]; then
    EXEC_JAR="bin/ether.jar"
elif ls target/*executable.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
elif ls target/society-simulation-*.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/society-simulation-*.jar | grep -v "sources" | grep -v "javadoc" | head -n 1)
fi

if [ -z "$EXEC_JAR" ]; then
    echo "Building executable JAR..."
    mvn clean package -DskipTests
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
fi

echo "Connecting Worker Node to Master at $MASTER_HOST:$PORT..."
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "$EXEC_JAR" --headless --mode=cluster --role=worker --master-host="$MASTER_HOST" --port=$PORT --secret="$SECRET"
