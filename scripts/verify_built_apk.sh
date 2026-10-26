#!/usr/bin/env bash
set -euo pipefail

# Verifies compiled APKs:
# 1. APK Signature Schemes v1 + v2 + v3 are valid and NOT signed with CN=Android Debug / testkey.
# 2. Merged binary AndroidManifest.xml inside the APK contains zero blocked sensitive permissions.

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <apk-file> [apk-file ...]" >&2
  exit 2
fi

BUILD_TOOLS_DIR=""
if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}/build-tools" ]]; then
  BUILD_TOOLS_DIR="$(find "${ANDROID_HOME}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
fi

APKSIGNER="${APKSIGNER:-${BUILD_TOOLS_DIR:+$BUILD_TOOLS_DIR/apksigner}}"
AAPT2="${AAPT2:-${BUILD_TOOLS_DIR:+$BUILD_TOOLS_DIR/aapt2}}"

if [[ -z "$APKSIGNER" || ! -x "$APKSIGNER" ]]; then
  echo "ERROR: apksigner not found (ANDROID_HOME=${ANDROID_HOME:-unset})" >&2
  exit 2
fi
if [[ -z "$AAPT2" || ! -x "$AAPT2" ]]; then
  echo "ERROR: aapt2 not found (ANDROID_HOME=${ANDROID_HOME:-unset})" >&2
  exit 2
fi

BLOCKED_PERMS=(
  "android.permission.RECEIVE_SMS"
  "android.permission.READ_SMS"
  "android.permission.SYSTEM_ALERT_WINDOW"
  "android.permission.REQUEST_INSTALL_PACKAGES"
  "android.permission.BIND_ACCESSIBILITY_SERVICE"
  "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
)

for apk in "$@"; do
  echo "=== Verifying $apk ==="
  if [[ ! -f "$apk" ]]; then
    echo "ERROR: APK file does not exist: $apk" >&2
    exit 1
  fi

  sig_out="$("$APKSIGNER" verify --verbose --print-certs "$apk")"
  echo "$sig_out"

  for scheme in "Verified using v1 scheme (JAR signing): true" \
                "Verified using v2 scheme (APK Signature Scheme v2): true" \
                "Verified using v3 scheme (APK Signature Scheme v3): true"; do
    if ! grep -Fq "$scheme" <<<"$sig_out"; then
      echo "ERROR: Missing expected signature verification '$scheme' in $apk" >&2
      exit 1
    fi
  done

  if grep -Eiq "CN=Android Debug|android@android\.com" <<<"$sig_out"; then
    echo "ERROR: $apk is signed with blacklisted Android Debug / AOSP testkey!" >&2
    exit 1
  fi

  perm_out="$("$AAPT2" dump permissions "$apk")"
  for blocked in "${BLOCKED_PERMS[@]}"; do
    if grep -Fq "$blocked" <<<"$perm_out"; then
      echo "ERROR: Compiled APK $apk declares blocked permission: $blocked" >&2
      exit 1
    fi
  done

  echo "OK: $apk passed v1+v2+v3 signature & merged-manifest permission checks."
done
