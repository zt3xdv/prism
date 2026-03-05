#!/bin/bash
set -e

# Downloads pre-compiled native libraries from PojavLauncher releases
# These include: gl4es, libopenal, LWJGL natives, pojavexec bridge

OUTPUT_DIR="$(realpath "$1")"

if [ -z "$1" ]; then
    echo "Usage: $0 <output_dir>"
    exit 1
fi

NATIVES_DIR="$OUTPUT_DIR/natives"
mkdir -p "$NATIVES_DIR"
WORK_DIR=$(mktemp -d)

echo "[*] Downloading native libraries..."

# Download the latest PojavLauncher APK and extract native libs
POJAV_APK_URL="https://github.com/PojavLauncherTeam/PojavLauncher/releases/download/gladiolus/PojavLauncher.apk"
echo "[*] Downloading PojavLauncher APK to extract natives..."
curl -sL -o "$WORK_DIR/pojav.apk" "$POJAV_APK_URL"

cd "$WORK_DIR"
unzip -q pojav.apk "lib/arm64-v8a/*" -d extracted/ 2>/dev/null || true

if [ -d "extracted/lib/arm64-v8a" ]; then
    echo "[*] Extracting arm64 native libraries..."
    cp extracted/lib/arm64-v8a/*.so "$NATIVES_DIR/"

    echo "[*] Native libraries extracted:"
    ls -lh "$NATIVES_DIR/"
else
    echo "[!] Failed to extract natives from APK"
    exit 1
fi

# Also extract the LWJGL GLFW shim jar
unzip -q pojav.apk "assets/*" -d extracted/ 2>/dev/null || true
if [ -d "extracted/assets" ]; then
    mkdir -p "$OUTPUT_DIR/lwjgl"
    find extracted/assets -name "*.jar" -exec cp {} "$OUTPUT_DIR/lwjgl/" \; 2>/dev/null || true
    echo "[*] LWJGL shim jars extracted to $OUTPUT_DIR/lwjgl/"
fi

# Extract JRE components if bundled
if [ -d "extracted/assets/components" ]; then
    mkdir -p "$OUTPUT_DIR/jre_components"
    cp -r extracted/assets/components/* "$OUTPUT_DIR/jre_components/" 2>/dev/null || true
    echo "[*] JRE components extracted to $OUTPUT_DIR/jre_components/"
fi

cd /
rm -rf "$WORK_DIR"
echo "[*] Done!"
