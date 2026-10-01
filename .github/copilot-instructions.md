# Repository instructions

- `docs/design.md` is the source of truth for architecture, behavior, privacy,
  security, and phase boundaries. Report contradictions; do not silently
  override the design.
- Complete and verify one development phase at a time.
- Never write to an Obsidian Vault. Only fictional fixtures may be committed.
- Keep core/parser code safe (`forbid(unsafe_code)`), offline, bounded, and
  free of note content in logs/errors.
- Validate all paths before opening and keep all index/cache data outside the
  Vault and in private app data.
- Follow module boundaries and do not implement future-phase UI in the Phase 1
  core.
