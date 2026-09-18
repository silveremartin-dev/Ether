#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  ETHER - Planetary Cliodynamics Simulation Engine"
echo "============================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java runtime not found in PATH."
    echo "Please install OpenJDK 21 or higher (e.g. sudo apt install openjdk-21-jre or brew install openjdk@21)."
    exit 1
fi

if [ -f "bin/ether.jar" ]; then
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin/ether.jar "$@" || \
    java -Xmx4g -jar bin/ether.jar "$@"
elif [ -f "target/society-simulation-1.0.0-beta.1-executable.jar" ]; then
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar target/society-simulation-1.0.0-beta.1-executable.jar "$@" || \
    java -Xmx4g -jar target/society-simulation-1.0.0-beta.1-executable.jar "$@"
elif ls target/*executable.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "$EXEC_JAR" "$@" || \
    java -Xmx4g -jar "$EXEC_JAR" "$@"
elif command -v mvn &> /dev/null; then
    echo "[INFO] Running via Maven..."
    mvn javafx:run
else
    echo "[ERROR] Could not find executable JAR or Maven."
    echo "Please build the project first via 'mvn clean package'."
    exit 1
fi
