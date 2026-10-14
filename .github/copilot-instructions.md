# AI assistant instructions

Follow the repository-wide [`AGENTS.md`](../AGENTS.md) instructions and read
[`docs/design.md`](../docs/design.md) before changing architecture, behavior,
privacy, security, or phase boundaries. If a requested change conflicts with
the design contract, report the conflict rather than silently redefining it.

## Current project

- The active application is Kotlin Multiplatform / Compose Multiplatform.
- Rust owns parsing, scanning, indexing, filtering, and thumbnails. Kotlin
  reaches Rust through repositories and the coarse-grained UniFFI facade;
  Composables do not call bindings directly.
- Android and desktop integrations are platform-specific. Keep dependencies
  pointing from UI to Kotlin state/repositories, then to the Rust FFI and core.
- The Flutter/Dart application and its bridge are retired historical
  references. Phase 14 is complete; do not restore parallel Flutter/Dart
  application, build, or release paths.

## Safety and privacy

- Never access, edit, or write to a user's Obsidian Vault. Use fictional
  fixtures only; do not commit real notes or media.
- Keep Vault access read-only. Validate paths before opening them and keep
  indexes and caches outside the Vault in private application data.
- Keep parser/core operations offline and bounded. Do not place note content,
  paths, tags, or URLs in logs or errors.
- Preserve `forbid(unsafe_code)` in core crates and the prohibition on network
  dependencies. Do not add telemetry, advertising, or remote crash reporting.
- External links and other outside-app actions must remain explicit user
  actions. Do not render Markdown.

## Implementation workflow

- Inspect existing patterns and the related design contract before editing.
- Preserve unrelated worktree changes. Keep work limited to the requested
  behavior and one development phase.
- Keep English and Japanese user-facing strings and evergreen documentation
  consistent. Do not put release-specific build status in evergreen docs.
- Do not use the README for source-build-only caveats or comparisons between
  published artifacts and source fixes. Keep implementation details in the
  design document and release-specific status in release metadata or
  versioned release notes.
- Run the narrowest relevant Gradle or Rust checks and then `git diff --check`.
- Do not change `VERSION`, create or move tags, publish artifacts, or claim
  target acceptance without explicit authorization and verified evidence.
- Before documenting a release, verify the public GitHub Release and its
  actual assets; a tag or configured workflow is not evidence of publication.
