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
    echo "[INFO] Launching Ether standalone package..."
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin/ether.jar "$@" || \
    java -Xmx4g -jar bin/ether.jar "$@"
elif [ -f "pom.xml" ] && command -v mvn &> /dev/null; then
    if [ "$1" = "--jar" ]; then
        echo "[INFO] Rebuilding and launching executable JAR..."
        mvn clean package -DskipTests
        EXEC_JAR=$(ls target/*executable.jar 2>/dev/null | head -n 1)
        java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "$EXEC_JAR" "$@" || \
        java -Xmx4g -jar "$EXEC_JAR" "$@"
    else
        echo "[INFO] Compiling and running latest code via Maven (JavaFX)..."
        mvn javafx:run
    fi
elif ls target/*executable.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
    echo "[INFO] Launching target JAR: $EXEC_JAR"
    java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar "$EXEC_JAR" "$@" || \
    java -Xmx4g -jar "$EXEC_JAR" "$@"
else
    echo "[ERROR] Could not find executable JAR or Maven."
    echo "Please build the project first via 'mvn clean package'."
    exit 1
fi
