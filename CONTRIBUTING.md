# Contributing

Use `nix develop` for the pinned development environment, then run:

```sh
cargo fmt --all -- --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
cargo deny check advisories bans licenses sources
```

Only fictional notes and media may be added to tests, documentation, issues,
or pull requests. Never edit or write into a Vault from the scanner or tests.
Keep parser input limits and Vault-boundary checks in place. Do not add
networking, telemetry, advertising, or remote crash reporting dependencies.

For dependency changes, document why the dependency is needed, its maintenance
and license status, any OS permissions or networking, and whether it uses
unsafe code or native libraries.
