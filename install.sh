#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  ETHER - Physicalist Planetary Cliodynamics Engine"
echo "============================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[!] Java 21+ not found. Please install OpenJDK 21 or higher."
    exit 1
fi

echo "[OK] Java detected:"
java -version
echo ""

if [ -f "target/society-simulation-1.0.0-beta.1-executable.jar" ]; then
    echo "[INFO] Launching standalone executable JAR..."
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar target/society-simulation-1.0.0-beta.1-executable.jar "$@"
elif command -v mvn &> /dev/null; then
    echo "[INFO] Starting Ether via Maven..."
    mvn javafx:run
else
    echo "[ERROR] Executable JAR not found and Maven is not installed."
    echo "Please build the project first."
    exit 1
fi
