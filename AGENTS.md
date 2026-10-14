# Repository instructions

Before changing code or documentation, read
[`.github/copilot-instructions.md`](.github/copilot-instructions.md) and
[`docs/design.md`](docs/design.md). The design document is authoritative for
architecture, behavior, privacy, security, and phase boundaries. Report
contradictions instead of silently changing those requirements.

The maintained application is Kotlin Multiplatform / Compose with the Rust
parser, core, and UniFFI boundary. Flutter/Dart and the old bridge are retired;
do not reintroduce them as an active application, build, or release path.
Use only fictional fixtures and never access or modify a user's Obsidian Vault.

Preserve unrelated worktree changes and keep each change scoped to the request
and one development phase. Run the narrowest relevant Rust or Gradle checks,
then `git diff --check`. Keep evergreen user-facing documentation
version-neutral. Do not change `VERSION`, create or move tags, publish
artifacts, or claim platform acceptance without explicit authorization and
verified results. Verify the public GitHub Release and its actual assets before
documenting release status.
