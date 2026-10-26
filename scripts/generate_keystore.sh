#!/usr/bin/env bash
set -euo pipefail

# Generates strong 4096-bit RSA (SHA256withRSA) PKCS12 keystores for Release & Debug builds.
# Avoids CN=Android Debug / AOSP testkey Distinguished Names that trigger OEM AV false positives.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="${KEYSTORE_DIR:-$ROOT_DIR/keystore}"
mkdir -p "$KEYSTORE_DIR"

RELEASE_STORE="$KEYSTORE_DIR/release.p12"
RELEASE_ALIAS="${KEYSTORE_ENTRY_ALIAS:-microg-release}"
RELEASE_PASS="${KEYSTORE_PASSWORD:-$(openssl rand -hex 16)}"

DEBUG_STORE="$KEYSTORE_DIR/debug.p12"
DEBUG_ALIAS="${DEBUG_KEYSTORE_ENTRY_ALIAS:-microg-dev}"
DEBUG_PASS="${DEBUG_KEYSTORE_PASSWORD:-$(openssl rand -hex 16)}"

if [[ ! -f "$RELEASE_STORE" ]]; then
  keytool -genkeypair \
    -v \
    -storetype PKCS12 \
    -keystore "$RELEASE_STORE" \
    -alias "$RELEASE_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -sigalg SHA256withRSA \
    -validity 10000 \
    -storepass "$RELEASE_PASS" \
    -keypass "$RELEASE_PASS" \
    -dname "CN=MicroG RE Release, OU=Mobile Engineering, O=MicroG RE Rebuild, L=Phnom Penh, ST=Phnom Penh, C=KH"
  echo "Created release keystore: $RELEASE_STORE"
else
  echo "Release keystore already exists: $RELEASE_STORE"
fi

if [[ ! -f "$DEBUG_STORE" ]]; then
  keytool -genkeypair \
    -v \
    -storetype PKCS12 \
    -keystore "$DEBUG_STORE" \
    -alias "$DEBUG_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -sigalg SHA256withRSA \
    -validity 10000 \
    -storepass "$DEBUG_PASS" \
    -keypass "$DEBUG_PASS" \
    -dname "CN=MicroG RE Dev Build, OU=Mobile Engineering, O=MicroG RE Rebuild, L=Phnom Penh, ST=Phnom Penh, C=KH"
  echo "Created debug keystore: $DEBUG_STORE"
else
  echo "Debug keystore already exists: $DEBUG_STORE"
fi

echo ""
echo "Add these lines to local.properties (do NOT commit local.properties or keystore/):"
cat <<EOF
signing.storeFile=$RELEASE_STORE
signing.storePassword=$RELEASE_PASS
signing.keyAlias=$RELEASE_ALIAS
signing.keyPassword=$RELEASE_PASS
signing.debugStoreFile=$DEBUG_STORE
signing.debugStorePassword=$DEBUG_PASS
signing.debugKeyAlias=$DEBUG_ALIAS
signing.debugKeyPassword=$DEBUG_PASS
EOF
