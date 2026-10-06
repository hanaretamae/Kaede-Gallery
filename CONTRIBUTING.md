# Contributing

> [!NOTE]
> This repository is for distribution only and does not accept external issues, pull requests, discussions,
> or other contributions (issues and discussions are disabled; pull requests are limited to collaborators).
> The following procedures are for project developers. See [the Japanese version](CONTRIBUTING.ja.md).

[`docs/design.md`](docs/design.md) is the source of truth for architecture, behavior, privacy, security, and
phase boundaries. Check that changes do not contradict it.

## Development environment

Use the pinned development environment with `nix develop`.

## Checks before submitting

When a Rust dependency changes, regenerate the bundled Rust license report:

```sh
python3 tools/update-rust-license-notices.py
```

Commit the updated `app/assets/licenses/RUST-DEPENDENCY-LICENSES.txt` with the
dependency change.

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources

# Kotlin Multiplatform / Compose
cd kotlin
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain :desktopApp:test :desktopApp:compileKotlin

# Launch the Linux Desktop replacement UI (filesystem Vaults and images)
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" \
  ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable

# Linux Android native library (from the Nix development shell)
rustup target add --toolchain 1.98.1 aarch64-linux-android
PATH="$(dirname "$(rustup which cargo --toolchain 1.98.1)"):$PATH" \
  env "CC_aarch64-linux-android=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
    "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
  ./gradlew :core:rust:cargoBuildAarch64AndroidDebug

# Flutter
cd app
dart format lib test
flutter analyze
flutter test
```

## Rules

> [!IMPORTANT]
> Never edit or write to an Obsidian Vault. Scanners and tests must follow this rule too.

- **Fictional data only:** Add only fictional notes and media to tests and documentation.
- **Languages:** The migration target is Rust and Kotlin. Keep existing Dart/Flutter code working until platform parity is verified; do not add unrelated implementation languages.
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
- The release owner verifies the changelog and tag version before committing and publishing. The release script
  uses the matching version section in `CHANGELOG.md` as the release notes.

## Dependency changes

For each dependency change, document:

- [ ] Why the dependency is needed
- [ ] Maintenance status and license
- [ ] OS permissions and network usage
- [ ] Use of unsafe code or native libraries
