#!/usr/bin/env bash
set -euo pipefail

# Explicitly 4-byte page-aligns and self-signs personal sideload APKs (NOT for Play Store)
# with APK Signature Schemes v1 + v2 + v3 (and v4 disabled for clean browser installation).

if [[ $# -lt 4 ]]; then
  echo "Usage: $0 <keystore-file> <keystore-pass> <key-alias> <apk-file> [apk-file ...]" >&2
  exit 2
fi

KS_FILE="$1"
KS_PASS="$2"
KS_ALIAS="$3"
shift 3
KEY_PASS="${SIGN_KEY_PASS:-$KS_PASS}"

BUILD_TOOLS_DIR=""
if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}/build-tools" ]]; then
  BUILD_TOOLS_DIR="$(find "${ANDROID_HOME}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
fi

ZIPALIGN="${ZIPALIGN:-${BUILD_TOOLS_DIR:+$BUILD_TOOLS_DIR/zipalign}}"
APKSIGNER="${APKSIGNER:-${BUILD_TOOLS_DIR:+$BUILD_TOOLS_DIR/apksigner}}"

if [[ -z "$ZIPALIGN" || ! -x "$ZIPALIGN" ]]; then
  echo "ERROR: zipalign not found (ANDROID_HOME=${ANDROID_HOME:-unset})" >&2
  exit 2
fi
if [[ -z "$APKSIGNER" || ! -x "$APKSIGNER" ]]; then
  echo "ERROR: apksigner not found (ANDROID_HOME=${ANDROID_HOME:-unset})" >&2
  exit 2
fi

for apk in "$@"; do
  aligned="${apk%.apk}.aligned.apk"
  "$ZIPALIGN" -p -f 4 "$apk" "$aligned"
  mv -f "$aligned" "$apk"
  "$APKSIGNER" sign \
    --ks "$KS_FILE" \
    --ks-pass "pass:$KS_PASS" \
    --ks-key-alias "$KS_ALIAS" \
    --key-pass "pass:$KEY_PASS" \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --v4-signing-enabled false \
    "$apk"
  rm -f "${apk}.idsig"
  echo "Aligned & self-signed (v1+v2+v3, non-Play-Store): $apk"
done
