# vault-gallery

An offline, read-only gallery for tagged Obsidian Vault notes. Phase 1 provides
the Rust parser, indexer, CLI, and fictional Vault generator; the Flutter UI is
planned for later phases.

## Phase 1 quick start

Enter the development shell with Nix, or install Rust stable, a C compiler, and
Python 3:

```sh
nix develop
python3 tools/generate_dummy_vault.py /tmp/dummy-vault --notes 100
cargo run --release -p gallery-cli -- scan /tmp/dummy-vault
cargo run --release -p gallery-cli -- categories /tmp/dummy-vault
cargo run --release -p gallery-cli -- list /tmp/dummy-vault source/rating/safe
cargo test --workspace
cargo deny check advisories bans licenses sources
```

The database defaults to `$XDG_STATE_HOME/vault-gallery/index.sqlite` or
`$HOME/.local/state/vault-gallery/index.sqlite`, outside the Vault. A custom
database path must also be outside the Vault. Unix state directories and index
files are restricted to the current user. CLI error messages omit note content,
paths, tags, and URLs.

The scanner ignores hidden paths and common Syncthing management files. The
index is disposable: changing a note's modification time or size reparses it;
removed notes disappear on the next scan. Missing media is retained as a note
with an existence flag that is refreshed on later scans.

## Parser scope and safety

The parser accepts UTF-8 with an optional BOM, LF/CRLF, YAML frontmatter with a
non-empty `tags` list, Markdown headings, Markdown media embeds and links, and
`関連` / `覚書` sections. The supplied sanitized Vault examples use Markdown
embeds only; Obsidian wikilink embeds are not required. Resource limits
currently cap notes at 2 MiB,
frontmatter at 256 KiB, tags at 256, and YAML nesting at 64 levels.

YAML aliases are rejected before deserialization. The selected
`yaml_serde`/libyaml parser does not expose a configurable alias expansion
budget, so rejecting alias tokens is the conservative way to avoid expansion
attacks. Quoted asterisks and comments are accepted. If representative Vault
notes require aliases, replace this with an event-based parser and an explicit
alias budget before enabling them.

SQLite is provided by `rusqlite` with its `bundled` feature. This embeds the
SQLite C library via FFI and is an explicit exception to the preference for
pure-Rust dependencies; it provides a consistent local index and avoids
system-SQLite variation. It is not a network client. The index is created only
outside the Vault.

The initial exclude prefixes are `moc`, `add`, `pin`, and `source/art`. They
only affect category display; notes tagged `source/art` remain searchable and
listed. Category options omit ancestor tags, and tag filtering uses OR within
one category and AND across categories.

## Performance and fixtures

Generate scale fixtures without using real clips:

```sh
python3 tools/generate_dummy_vault.py /tmp/vault-7806 --notes 7806
python3 tools/generate_dummy_vault.py /tmp/vault-20000 --notes 20000
```

`tools/benchmark.sh` records a release build and scan timing for a requested
note count. Use `--database <path>` before tag arguments to select a custom
index location. Generated scale fixtures are not committed; the repository's
small `testdata/dummy-vault` fixture is fictional.

## Dependency security

Core crates declare `forbid(unsafe_code)`. Dependency versions are locked in
`Cargo.lock`; network-related packages are denied by `deny.toml`. The index is
a cache and can be deleted and rebuilt from the Vault.
