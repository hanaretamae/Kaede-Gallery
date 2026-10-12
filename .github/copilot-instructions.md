# Repository instructions

Read the repository-root [`AGENTS.md`](../AGENTS.md) and
[`docs/design.md`](../docs/design.md) before making changes. `docs/design.md`
is authoritative for architecture, behavior, privacy, security, and phase
boundaries; report contradictions rather than silently changing those
requirements.

The active application is Kotlin Multiplatform / Compose with the Rust parser,
core, and FFI. The Flutter/Dart application and its bridge are retired
historical references; Phase 14 migration is complete. Do not reintroduce a
parallel Flutter/Dart implementation or describe it as an active build or
release path. Treat Flutter-era measurements and acceptance notes as history,
not as evidence of current KMP behavior.

Keep evergreen user-facing documentation version-neutral. Do not add current
release/build status, notes comparing published artifacts with source fixes,
or standalone README statements about source-build-only behavior such as
AppImage font support. Keep necessary implementation details in design
documentation; put release-specific status in GitHub Release metadata or
versioned release notes when needed. Retain older version facts in evergreen
docs only when required for licensing, compatibility, or migration history.

Keep changes scoped to one development phase and to the requested behavior.
Preserve unrelated worktree changes, use fictional fixtures only, and never
access or modify a user's Obsidian Vault. Core/parser changes must remain safe,
offline, bounded, and free of note content in logs or errors. Validate paths
before opening them and keep indexes/caches outside the Vault in private app
data.

Use the narrowest relevant test/build command, then `git diff --check`. Follow
the bilingual release guides for release work. Do not change `VERSION`, create
or move tags, publish artifacts, or state that a platform passed acceptance
without explicit authorization and verified results. Verify the public GitHub
Release and its actual assets before documenting release status; a tag or
configured workflow alone is not proof of publication.
