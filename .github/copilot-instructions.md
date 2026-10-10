# Repository instructions

Read the repository-root [`AGENTS.md`](../AGENTS.md) and
[`docs/design.md`](../docs/design.md) before making changes. `docs/design.md`
is authoritative for architecture, behavior, privacy, security, and phase
boundaries; report contradictions rather than silently changing those
requirements.

Keep changes scoped to one development phase and to the requested behavior.
Preserve unrelated worktree changes, use fictional fixtures only, and never
access or modify a user's Obsidian Vault. Core/parser changes must remain safe,
offline, bounded, and free of note content in logs or errors. Validate paths
before opening them and keep indexes/caches outside the Vault in private app
data.

Use the narrowest relevant test/build command, then `git diff --check`. Follow
the bilingual release guides for release work. Do not change `VERSION`, create
or move tags, publish artifacts, or state that a platform passed acceptance
without explicit authorization and verified results.
