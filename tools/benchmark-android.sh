#!/usr/bin/env bash
set -euo pipefail
set +x

fail() {
  printf 'error: %s\n' "$1" >&2
  exit 1
}

if [[ $# -gt 1 ]]; then
  fail "usage: tools/benchmark-android.sh [device-id]"
fi
command -v flutter >/dev/null || fail "Flutter is required; enter nix develop first"
command -v rustup >/dev/null || fail "Rustup is required; enter nix develop first"
command -v adb >/dev/null || fail "ADB is required; enter nix develop first"
[[ -n ${ANDROID_NDK_HOME:-} ]] || fail "enter nix develop to configure the Android NDK"

device_id=${1:-}
if [[ -z $device_id ]]; then
  device_id=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')
fi
[[ -n $device_id ]] || fail "connect an Android device or provide its device ID"
adb -s "$device_id" get-state | grep -qx device ||
  fail "the selected Android device is unavailable"

rustc_path=$(rustup which rustc) || fail "the configured Rust toolchain is unavailable"
rust_toolchain_bin=$(dirname "$rustc_path")
export PATH="$rust_toolchain_bin:$PATH"
export RUSTC="$rustc_path"
ndk_bin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
[[ -x $ndk_bin/aarch64-linux-android35-clang ]] ||
  fail "Android NDK compiler was not found"
export CC_aarch64_linux_android="$ndk_bin/aarch64-linux-android35-clang"
export CXX_aarch64_linux_android="$ndk_bin/aarch64-linux-android35-clang++"
export AR_aarch64_linux_android="$ndk_bin/llvm-ar"
export RANLIB_aarch64_linux_android="$ndk_bin/llvm-ranlib"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$CC_aarch64_linux_android"

cd "$(git rev-parse --show-toplevel)"
(cd app && flutter test integration_test/android_saf_benchmark_test.dart -d "$device_id")
