#!/usr/bin/env bash
# Builds the uniffi-kotlin-multiplatform runtime for Windows ARM64.
# The published runtime-jvm JAR has no win32-aarch64 library, so JNA cannot
# load it on ARM64 Windows. Run on a Windows ARM64 host with Rust installed.
set -euo pipefail

tag="v1.3.0"
commit="20c1190053cb29c6c5e8bb07d366323baa2ed96b"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dest="$root/kotlin/core/rust/src/jvmMain/resources/win32-aarch64"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

git clone --quiet --depth 1 --branch "$tag" \
  https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings "$work/src"
test "$(git -C "$work/src" rev-parse HEAD)" = "$commit"

(cd "$work/src" && cargo build --release --locked -p uniffi-runtime \
  --target aarch64-pc-windows-msvc --target-dir "$work/target")

mkdir -p "$dest"
cp "$work/target/aarch64-pc-windows-msvc/release/uniffi_runtime.dll" "$dest/"
echo "Built $dest/uniffi_runtime.dll"
