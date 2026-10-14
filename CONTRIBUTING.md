# Contributing

> [!NOTE]
> This repository is maintained as a distribution project. Public issues and
> discussions are not used for general support, and pull requests are limited
> to collaborators. This guide describes the project development workflow.
> See [the Japanese version](CONTRIBUTING.ja.md).

Read [`AGENTS.md`](AGENTS.md), [the AI instructions](.github/copilot-instructions.md),
and [`docs/design.md`](docs/design.md) before making changes. The design
document is authoritative for architecture, behavior, privacy, security, and
phase boundaries. Report conflicts rather than silently changing those
requirements.

## Active implementation

The application is Kotlin Multiplatform / Compose Multiplatform, with Rust
providing parsing, indexing, filtering, thumbnails, and the UniFFI boundary.
Flutter/Dart and the old bridge are retired and must not be reintroduced as an
active application, build, or release path.

## Development environment

Use the pinned toolchain with `nix develop`. It provides the Rust, JDK,
Android, and native build tools used by the repository.

## Safety and implementation rules

- Never access, edit, or write to a user's Obsidian Vault. Use fictional
  fixtures only, including in tests and documentation.
- Keep Vault access read-only. Validate paths before opening them and store
  indexes and caches outside the Vault in private application data.
- Keep core/parser behavior offline and bounded. Do not include note content,
  paths, tags, or URLs in logs or errors.
- Preserve the Rust `forbid(unsafe_code)` and no-network-dependency policies.
- Do not add telemetry, advertising, remote crash reporting, or unrelated
  languages or parallel UI implementations.
- Keep English and Japanese user-facing strings and documentation in sync.
- Keep changes scoped to one development phase and the requested behavior.

## Checks

Run the narrowest checks that cover the change. Use the broader suites when
shared interfaces, dependencies, or cross-platform behavior are affected.

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources
```

```sh
# Kotlin Multiplatform / Compose (from kotlin/)
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests \
  :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain \
  :desktopApp:test :desktopApp:compileKotlin

# Linux Compose Desktop tests and local distribution
./gradlew :desktopApp:test
./gradlew :desktopApp:createDistributable
```

The Android debug APK can be built with `:androidApp:assembleDebug` after
installing the Android Rust targets and configuring the NDK cross-compilers.
See [the release guide](docs/releasing.md) for platform-specific setup and
release procedures. A debug build is not a production-signed release.

After checks, run `git diff --check`.

## Dependency and license changes

For a dependency change, record why it is needed, its maintenance status and
license, any OS permissions and network use, and whether it introduces unsafe
code or native libraries. When Rust dependencies change, regenerate the
runtime dependency license report:

```sh
python3 tools/update-rust-license-notices.py
```

## Release documentation

Keep evergreen documentation version-neutral. Do not claim unverified
behavior, tests, platform acceptance, or release assets. Release-specific
information belongs in GitHub Release metadata or versioned release notes.
Follow the [English](docs/releasing.md) and
[Japanese](docs/releasing.ja.md) release guides. Do not change `VERSION`, create or move tags, or
publish artifacts without explicit release-owner authorization.
