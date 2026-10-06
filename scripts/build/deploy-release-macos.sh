#!/usr/bin/env bash
# ==============================================================================
# Ether — macOS Release Packager (.command / .tar.gz / .zip + SHA256)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
DIST_DIR="$ROOT_DIR/dist"

cd "$ROOT_DIR"

VERSION="${1}"
if [ -z "$VERSION" ]; then
    VERSION=$(grep -m 1 -oP '(?<=<version>)[^<]+' pom.xml || echo "1.0.0-beta.2")
fi

PKG_NAME="Ether-v$VERSION-macos-universal"
TARGET_DIR="$DIST_DIR/$PKG_NAME"

echo "============================================================"
echo "  ETHER -- MACOS RELEASE PACKAGER (v$VERSION)"
echo "============================================================"

echo "[1/4] Building executable JAR via Maven..."
mvn clean package -DskipTests

echo "[2/4] Assembling directory structure..."
rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR/bin" "$TARGET_DIR/data" "$TARGET_DIR/docs" "$TARGET_DIR/saves" "$TARGET_DIR/logs"

# Locate executable JAR
EXEC_JAR=$(ls "$ROOT_DIR/target/"*executable.jar 2>/dev/null | head -n 1 || ls "$ROOT_DIR/target/society-simulation-"*.jar 2>/dev/null | head -n 1)
if [ -z "$EXEC_JAR" ]; then
    echo "[ERROR] Executable JAR not found in target/"
    exit 1
fi

cp "$EXEC_JAR" "$TARGET_DIR/bin/ether.jar"

if [ -d "$ROOT_DIR/data" ]; then cp -r "$ROOT_DIR/data/"* "$TARGET_DIR/data/" 2>/dev/null || true; fi
if [ -d "$ROOT_DIR/docs" ]; then cp -r "$ROOT_DIR/docs/"* "$TARGET_DIR/docs/" 2>/dev/null || true; fi
cp "$ROOT_DIR/README.md" "$TARGET_DIR/" 2>/dev/null || true
cp "$ROOT_DIR/LICENSE" "$TARGET_DIR/" 2>/dev/null || true
cp "$ROOT_DIR/AGENTS.md" "$TARGET_DIR/" 2>/dev/null || true
cp "$ROOT_DIR/.env.example" "$TARGET_DIR/" 2>/dev/null || true

# Generate macOS script
cat << 'EOF' > "$TARGET_DIR/run.sh"
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  ETHER - Planetary Cliodynamics Simulation Engine"
echo "============================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 21+ is required but not found in PATH."
    echo "Please install OpenJDK 21 via Homebrew (brew install openjdk@21) or Adoptium."
    exit 1
fi

java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xms2g -Xmx12g -XX:+UseG1GC -jar bin/ether.jar "$@" || \
java -Xms2g -Xmx8g -jar bin/ether.jar "$@"
EOF
chmod +x "$TARGET_DIR/run.sh"

# Generate macOS double-clickable .command launcher
cat << 'EOF' > "$TARGET_DIR/run.command"
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
exec ./run.sh "$@"
EOF
chmod +x "$TARGET_DIR/run.command"

echo "[3/4] Creating distribution archives and SHA256 checksums..."
cd "$DIST_DIR"
tar -czf "$PKG_NAME.tar.gz" "$PKG_NAME"
if command -v shasum &>/dev/null; then
    shasum -a 256 "$PKG_NAME.tar.gz" > "$PKG_NAME.tar.gz.sha256"
elif command -v sha256sum &>/dev/null; then
    sha256sum "$PKG_NAME.tar.gz" > "$PKG_NAME.tar.gz.sha256"
fi

if command -v zip &>/dev/null; then
    zip -r "$PKG_NAME.zip" "$PKG_NAME" >/dev/null 2>&1
    if command -v shasum &>/dev/null; then
        shasum -a 256 "$PKG_NAME.zip" > "$PKG_NAME.zip.sha256"
    elif command -v sha256sum &>/dev/null; then
        sha256sum "$PKG_NAME.zip" > "$PKG_NAME.zip.sha256"
    fi
fi

echo "[4/4] macOS release packages created successfully in $DIST_DIR"
