#!/usr/bin/env bash
set -euo pipefail

task_keystore_path="${SWIPETUNE_UPLOAD_KEYSTORE:-${HOME}/.android/keystores/swipetune-upload.jks}"
task_key_alias="${SWIPETUNE_UPLOAD_KEY_ALIAS:-swipetune-upload}"
task_keychain_service="dev.swipetune.app.upload"

if [[ ! -f "${task_keystore_path}" ]]; then
  echo "Upload keystore not found: ${task_keystore_path}" >&2
  exit 1
fi

export SWIPETUNE_UPLOAD_KEYSTORE="${task_keystore_path}"
export SWIPETUNE_UPLOAD_KEY_ALIAS="${task_key_alias}"
export SWIPETUNE_UPLOAD_STORE_PASSWORD="${SWIPETUNE_UPLOAD_STORE_PASSWORD:-$(security find-generic-password -s "${task_keychain_service}" -a "${task_key_alias}" -w)}"
export SWIPETUNE_UPLOAD_KEY_PASSWORD="${SWIPETUNE_UPLOAD_KEY_PASSWORD:-${SWIPETUNE_UPLOAD_STORE_PASSWORD}}"

ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}" ./gradlew clean lintRelease testDebugUnitTest bundleRelease

bundle_path="app/build/outputs/bundle/release/app-release.aab"
jarsigner -verify "${bundle_path}" >/dev/null 2>&1
echo "Bundle signature verified."
shasum -a 256 "${bundle_path}"
echo "Play bundle: ${bundle_path}"
