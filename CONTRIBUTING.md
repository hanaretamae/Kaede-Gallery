# Contributing

> [!NOTE]
> This repository is for distribution only and does not accept external issues, pull requests, discussions,
> or other contributions (issues and discussions are disabled; pull requests are limited to collaborators).
> The following procedures are for project developers. See [the Japanese version](CONTRIBUTING.ja.md).

[`docs/design.md`](docs/design.md) is the source of truth for architecture, behavior, privacy, security, and
phase boundaries. Check that changes do not contradict it.

## Development environment

Use the pinned development environment with `nix develop`. On Linux, the shell adds GLib's library directory to `LD_LIBRARY_PATH` so the Portal client can load GIO through JNA.

## Checks before submitting

When a Rust dependency changes, regenerate the bundled Rust license report:

```sh
python3 tools/update-rust-license-notices.py
```

The generator updates `kotlin/shared-assets/licenses/RUST-DEPENDENCY-LICENSES.txt`, which is packaged by both Android and Desktop apps.

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources

# Kotlin Multiplatform / Compose (from the repository root)
cd kotlin
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain :desktopApp:test :desktopApp:compileKotlin

# Run Linux Compose Desktop and create its distributable
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" \
  ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable

# Android debug APK on Linux x86_64 (inside nix develop)
set -euo pipefail
# Stop if either target is missing; do not launch Gradle and wait for its retry loop.
export RUSTUP_TOOLCHAIN=1.98.1
rustup target list --installed --toolchain "$RUSTUP_TOOLCHAIN" | grep -Fx aarch64-linux-android
rustup target list --installed --toolchain "$RUSTUP_TOOLCHAIN" | grep -Fx x86_64-linux-android
export RUSTC="$(rustup which rustc --toolchain "$RUSTUP_TOOLCHAIN")"
export RUSTDOC="$(rustup which rustdoc --toolchain "$RUSTUP_TOOLCHAIN")"
export PATH="$(dirname "$RUSTC"):$PATH"
NDK_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
export CC_aarch64_linux_android="$NDK_BIN/aarch64-linux-android23-clang"
export CXX_aarch64_linux_android="$NDK_BIN/aarch64-linux-android23-clang++"
export AR_aarch64_linux_android="$NDK_BIN/llvm-ar"
export RANLIB_aarch64_linux_android="$NDK_BIN/llvm-ranlib"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK_BIN/aarch64-linux-android23-clang"
export CC_x86_64_linux_android="$NDK_BIN/x86_64-linux-android23-clang"
export CXX_x86_64_linux_android="$NDK_BIN/x86_64-linux-android23-clang++"
export AR_x86_64_linux_android="$NDK_BIN/llvm-ar"
export RANLIB_x86_64_linux_android="$NDK_BIN/llvm-ranlib"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$NDK_BIN/x86_64-linux-android23-clang"
./gradlew :androidApp:assembleDebug
```

This creates a locally debug-signed APK under `kotlin/androidApp/build/outputs/apk/debug/` for the configured Android ABIs. It does not use the production signing key. Keep local builds as the normal path; the optional Actions workflow is manual-only and can build the Android debug APK, the Linux AArch64 bundle, or both. It never runs on push/pull request and does not publish a release. Once the workflow is present on GitHub, dispatch it from the Nix shell with:

```sh
tools/run-optional-builds.sh android-debug
# Or choose linux-aarch64 or both; an optional second argument selects the ref.
tools/run-optional-builds.sh both main
```

The script requires an authenticated GitHub CLI (`gh auth login`).

## Rules

> [!IMPORTANT]
> Never edit or write to an Obsidian Vault. Scanners and tests must follow this rule too.

- **Fictional data only:** Add only fictional notes and media to tests and documentation.
- **Languages:** Rust and Kotlin/Compose are the active implementation; Flutter is retired. Do not reintroduce a parallel UI or add unrelated implementation languages.
- **Safety:** Core and parser code must use `forbid(unsafe_code)`, remain offline and bounded, and never put note
  content in logs or errors.
- **Paths and storage:** Validate paths before opening them. Store indexes and caches outside the Vault in
  private app data.
- **Network:** Do not add dependencies for networking, telemetry, advertising, or remote crash reporting.
- **Phases:** Complete and verify one development phase at a time. Do not add future-phase UI to the Phase 1 core.

## Changelog and releases

- Draft changelog entries from the implementation diff and actual verification results.
- Do not describe unverified behavior, tests that were not run, or future plans as completed. Do not include
  secrets or personal information.
- The release owner verifies the changelog and tag version before committing and publishing. `VERSION` is the
  single application version source. `tools/release.sh` builds a signed KMP Android APK and x86_64/aarch64 Linux bundles; aarch64 builds on x86_64 require QEMU/binfmt or a remote builder. It uses
  the Rustup 1.98.1 Android targets already installed on the host and never retries target installation. The manual
  Windows workflow packages KMP and attaches its ZIP to an existing GitHub Release when dispatched with a tag.
  The separate optional Android/Linux AArch64 build workflow is `workflow_dispatch` only and never handles release
  signing or publication. Release tags match `VERSION` exactly and have no `v` prefix; beta tags such as `2.0.0b1`
  are published as GitHub prereleases. Release publication still requires the release owner's real credentials and
  successful target-specific checks.

## Dependency changes

For each dependency change, document:

- [ ] Why the dependency is needed
- [ ] Maintenance status and license
- [ ] OS permissions and network usage
- [ ] Use of unsafe code or native libraries
