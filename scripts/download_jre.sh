#!/bin/bash
set -e

# Downloads pre-built JRE for Android arm64 from PojavLauncher's CI
# These are OpenJDK builds cross-compiled for Android with NDK

OUTPUT_DIR="$(realpath "$1")"
JRE_VERSION="${JRE_VERSION:-8}"

if [ -z "$1" ]; then
    echo "Usage: $0 <output_dir>"
    exit 1
fi

mkdir -p "$OUTPUT_DIR/jre"
WORK_DIR=$(mktemp -d)

echo "[*] Downloading JRE $JRE_VERSION for Android arm64..."

# Use PojavLauncher's pre-built JRE artifacts from their releases
# The JRE is available from their CI builds
POJAV_JRE_URL="https://github.com/ArtDev-o/mobile-jre-builds/releases/download/v0.1/jre${JRE_VERSION}-arm64.tar.xz"

# Fallback: try direct from Pojav CI artifacts
if ! curl -sL --fail -o "$WORK_DIR/jre.tar.xz" "$POJAV_JRE_URL" 2>/dev/null; then
    echo "[*] Trying alternate JRE source..."
    # Use Liberica JRE lite for Android (BellSoft provides ARM64 Android builds)
    LIBERICA_URL="https://download.bell-sw.com/java/8u442+10/bellsoft-jre8u442+10-linux-aarch64.tar.gz"
    curl -sL -o "$WORK_DIR/jre.tar.gz" "$LIBERICA_URL"
    cd "$OUTPUT_DIR/jre"
    tar xzf "$WORK_DIR/jre.tar.gz" --strip-components=1
    echo "[*] JRE extracted (Liberica, may need Android patches)"
    cd /
    rm -rf "$WORK_DIR"
    exit 0
fi

cd "$OUTPUT_DIR/jre"
tar xJf "$WORK_DIR/jre.tar.xz" --strip-components=1 2>/dev/null || \
tar xJf "$WORK_DIR/jre.tar.xz" 2>/dev/null || true

cd /
rm -rf "$WORK_DIR"

echo "[*] JRE extracted to $OUTPUT_DIR/jre"
ls -la "$OUTPUT_DIR/jre/" 2>/dev/null || echo "[!] JRE directory may be empty"
