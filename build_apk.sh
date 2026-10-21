#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$HOME/output}"

export JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/tools/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=================================================="
echo "Starting ComicPulse APK Build Process..."
echo "=================================================="
echo "JAVA_HOME:    $JAVA_HOME"
echo "ANDROID_HOME: $ANDROID_HOME"

# 1. Dynamically read versionName from app/build.gradle.kts
VERSION_NAME=$(grep 'versionName' "$SCRIPT_DIR/app/build.gradle.kts" | sed -E 's/.*versionName\s*=\s*"([^"]+)".*/\1/')
if [ -z "$VERSION_NAME" ]; then
    VERSION_NAME="latest"
fi
echo "Resolved App Version: $VERSION_NAME"

# 2. Compile release APK
cd "$SCRIPT_DIR"
chmod +x ./gradlew

# Ensure release keystore exists
mkdir -p "$SCRIPT_DIR/keystore"
if [ ! -f "$SCRIPT_DIR/keystore/comicpulse-release.jks" ]; then
    echo "Release keystore not found; generating default release key..."
    "$JAVA_HOME/bin/keytool" -genkeypair -v \
      -keystore "$SCRIPT_DIR/keystore/comicpulse-release.jks" \
      -alias comicpulse \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass comicpulse \
      -keypass comicpulse \
      -dname "CN=ComicPulse, OU=Mobile, O=ComicPulse, L=Sao Paulo, ST=SP, C=BR"
fi

./gradlew assembleRelease "$@"

# 3. Ensure output directory exists and copy APK with version name
mkdir -p "$OUTPUT_DIR"
SRC_APK="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
DEST_APK="$OUTPUT_DIR/comicpulse_${VERSION_NAME}.apk"
DEST_APK_ALT="$OUTPUT_DIR/comic_pulse_${VERSION_NAME}.apk"

if [ -f "$SRC_APK" ]; then
    cp -f "$SRC_APK" "$DEST_APK"
    cp -f "$SRC_APK" "$DEST_APK_ALT"
    echo "=================================================="
    echo "Build Successful!"
    echo "APK copied to: $DEST_APK"
    echo "APK copied to: $DEST_APK_ALT"
    echo "=================================================="
else
    echo "Error: Compiled APK not found at $SRC_APK" >&2
    exit 1
fi
