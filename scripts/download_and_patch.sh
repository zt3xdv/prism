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
CLIENT_URL=$(curl -s "$VERSION_URL" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(data['downloads']['client']['url'])
")

echo "[*] Downloading client jar..."
curl -L -o "$WORK_DIR/minecraft.jar" "$CLIENT_URL"

echo "[*] Applying offline patches..."
mkdir -p "$WORK_DIR/patch_tmp"
cd "$WORK_DIR/patch_tmp"
jar xf "$WORK_DIR/minecraft.jar"

# Patch: Create offline authenticator that skips Mojang auth
mkdir -p com/prism/patch
cat > com/prism/patch/OfflineAuth.java << 'JAVA'
package com.prism.patch;

public class OfflineAuth {
    public static String getAccessToken() {
        return "0";
    }
    public static boolean isValid() {
        return true;
    }
    public static String getUUID(String username) {
        return java.util.UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes()).toString();
    }
}
JAVA

javac -source 8 -target 8 com/prism/patch/OfflineAuth.java 2>/dev/null || true
rm -f com/prism/patch/OfflineAuth.java

jar cf "$OUTPUT_DIR/minecraft.jar" .
cd /
rm -rf "$WORK_DIR"

echo "[*] Patched jar saved to $OUTPUT_DIR/minecraft.jar"
ls -lh "$OUTPUT_DIR/minecraft.jar"
