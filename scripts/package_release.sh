#!/usr/bin/env bash
# Package Ether into a self-contained standalone distribution archive (.tar.gz / .zip)
set -e

VERSION="${1:-2.0.0}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( dirname "$SCRIPT_DIR" )"
DIST_DIR="$ROOT_DIR/dist"
PACKAGE_NAME="Ether-v$VERSION-standalone"
TARGET_DIR="$DIST_DIR/$PACKAGE_NAME"

echo "============================================================"
echo " 🚀 Packaging Ether Planetary Simulation v$VERSION"
echo "============================================================"

# 1. Build Fat JAR
echo "[1/4] Building shaded executable JAR via Maven..."
cd "$ROOT_DIR"
mvn clean package -DskipTests

# 2. Prepare Directory Structure
echo "[2/4] Preparing output directory: $TARGET_DIR..."
rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR/bin" "$TARGET_DIR/data" "$TARGET_DIR/docs" "$TARGET_DIR/saves" "$TARGET_DIR/logs"

# 3. Copy Binaries and Assets
echo "[3/4] Copying artifacts and resources..."
cp "$ROOT_DIR/target/society-simulation-2.0.0-SNAPSHOT-executable.jar" "$TARGET_DIR/bin/ether.jar" || \
cp "$ROOT_DIR/target/"*executable.jar "$TARGET_DIR/bin/ether.jar"

if [ -d "$ROOT_DIR/data" ]; then
    cp -r "$ROOT_DIR/data/"* "$TARGET_DIR/data/" 2>/dev/null || true
fi

cp "$ROOT_DIR/README.md" "$TARGET_DIR/" 2>/dev/null || true
cp "$ROOT_DIR/LICENSE" "$TARGET_DIR/" 2>/dev/null || true
cp "$ROOT_DIR/AGENT.md" "$TARGET_DIR/" 2>/dev/null || true
cp -r "$ROOT_DIR/docs/"* "$TARGET_DIR/docs/" 2>/dev/null || true

# Standalone run.sh
cat << 'EOF' > "$TARGET_DIR/run.sh"
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  ETHER - Planetary Cliodynamics Simulation Engine"
echo "============================================================"

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 21+ is required but not found in PATH."
    echo "Please install OpenJDK 21 or higher."
    exit 1
fi

java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin/ether.jar "$@" || \
java -Xmx4g -jar bin/ether.jar "$@"
EOF
chmod +x "$TARGET_DIR/run.sh"

# Standalone install.sh
cat << 'EOF' > "$TARGET_DIR/install.sh"
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  Ether Instant Setup & Environment Verification"
echo "============================================================"

if ! command -v java &> /dev/null; then
    echo "[!] Java 21+ not found. Please install OpenJDK 21+ (e.g. sudo apt install openjdk-21-jre)."
    exit 1
fi

echo "[OK] Java detected:"
java -version
echo ""
echo "[OK] Ready! Launching Ether..."
./run.sh
EOF
chmod +x "$TARGET_DIR/install.sh"

# 4. Create Archive
echo "[4/4] Creating distribution archive..."
cd "$DIST_DIR"
tar -czf "$PACKAGE_NAME.tar.gz" "$PACKAGE_NAME"
zip -r "$PACKAGE_NAME.zip" "$PACKAGE_NAME" >/dev/null 2>&1 || true

echo "============================================================"
echo " ✨ Package created successfully: $DIST_DIR/$PACKAGE_NAME.tar.gz"
echo "============================================================"
