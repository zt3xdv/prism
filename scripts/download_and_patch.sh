#!/bin/bash
set -e

MC_VERSION="${MC_VERSION:-1.20.4}"
OUTPUT_DIR="$(realpath "$1")"

if [ -z "$1" ]; then
    echo "Usage: $0 <output_dir>"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
WORK_DIR=$(mktemp -d)

# ==========================================
# 1. Download Minecraft client jar
# ==========================================
echo "[*] Fetching version manifest..."
MANIFEST_URL="https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
VERSION_URL=$(curl -s "$MANIFEST_URL" | python3 -c "
import json, sys
data = json.load(sys.stdin)
for v in data['versions']:
    if v['id'] == '$MC_VERSION':
        print(v['url'])
        break
")

if [ -z "$VERSION_URL" ]; then
    echo "[!] Version $MC_VERSION not found"
    exit 1
fi

echo "[*] Fetching version $MC_VERSION metadata..."
VERSION_JSON=$(curl -s "$VERSION_URL")
CLIENT_URL=$(echo "$VERSION_JSON" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(data['downloads']['client']['url'])
")

echo "[*] Downloading client jar..."
curl -L -o "$OUTPUT_DIR/minecraft.jar" "$CLIENT_URL"

# Save version json for library resolution at runtime
echo "$VERSION_JSON" > "$OUTPUT_DIR/version.json"

echo "[*] Patched jar saved to $OUTPUT_DIR/minecraft.jar"
ls -lh "$OUTPUT_DIR/minecraft.jar"

# ==========================================
# 2. Download Minecraft libraries
# ==========================================
echo "[*] Downloading Minecraft libraries..."
LIBS_DIR="$OUTPUT_DIR/libraries"
mkdir -p "$LIBS_DIR"

echo "$VERSION_JSON" | python3 -c "
import json, sys
data = json.load(sys.stdin)
for lib in data.get('libraries', []):
    dl = lib.get('downloads', {}).get('artifact')
    if dl:
        print(dl['url'] + ' ' + dl['path'])
" | while read -r url path; do
    dir=$(dirname "$LIBS_DIR/$path")
    mkdir -p "$dir"
    if [ ! -f "$LIBS_DIR/$path" ]; then
        echo "  -> $path"
        curl -sL -o "$LIBS_DIR/$path" "$url" || true
    fi
done

echo "[*] Libraries downloaded to $LIBS_DIR"

rm -rf "$WORK_DIR"
echo "[*] Done!"
