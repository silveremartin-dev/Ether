#!/usr/bin/env bash
# ==============================================================================
# Ether — All-In-One Multi-OS Release Packager (Linux, macOS, Windows)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

VERSION="${1}"
if [ -z "$VERSION" ]; then
    VERSION=$(grep -m 1 -oP '(?<=<version>)[^<]+' pom.xml || echo "1.0.0-beta.2")
fi

echo "============================================================"
echo " 🪐 ETHER -- BUILDING ALL MULTI-PLATFORM RELEASES v$VERSION"
echo "============================================================"

bash "$SCRIPT_DIR/deploy-release-linux.sh" "$VERSION"
bash "$SCRIPT_DIR/deploy-release-macos.sh" "$VERSION"

echo "============================================================"
echo " [SUCCESS] All multi-OS releases packaged successfully in dist/"
echo "============================================================"
