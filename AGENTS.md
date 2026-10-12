# AI instructions

See [.github/copilot-instructions.md](.github/copilot-instructions.md), then
read [`docs/design.md`](docs/design.md) before changing architecture, behavior,
privacy, security, or phase boundaries. Treat `docs/design.md` as authoritative
and report contradictions instead of silently changing requirements.

The active application is Kotlin Multiplatform / Compose with the Rust parser,
core, and FFI. The Flutter/Dart application and its bridge are retired
historical references; Phase 14 migration is complete. Do not reintroduce a
parallel Flutter/Dart implementation or describe it as an active build or
release path. Treat Flutter-era measurements and acceptance notes as history,
not as evidence of current KMP behavior.

Preserve unrelated worktree changes and complete one development phase at a
time. Use fictional fixtures only; never access or modify a user's Obsidian
Vault. Keep core/parser code safe, offline, bounded, and free of note content
in logs/errors. Validate paths before opening them and keep indexes/caches
outside the Vault in private app data.

Run the narrowest relevant Gradle/Rust checks after each coherent change, then
run `git diff --check`. Follow the bilingual release guides for release work.
Do not bump `VERSION`, create or move tags, publish releases, or claim target
acceptance without explicit authorization and verified results. Verify the
public GitHub Release and its actual assets before documenting release status;
a tag or configured workflow alone is not proof of publication.
