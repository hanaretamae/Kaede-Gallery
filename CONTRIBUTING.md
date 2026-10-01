# Contributing

固定された開発環境を使うため `nix develop` に入り、次を実行してください。

```sh
cargo fmt --all -- --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
cargo deny check advisories bans licenses sources
```

テスト・ドキュメント・Issue・プルリクエストには、架空の note とメディアのみを追加できます。
スキャナーやテストから Vault を編集・書き込みしないでください。
パーサーの入力上限と Vault 境界のチェックは常に維持してください。
ネットワーク通信・テレメトリ・広告・リモートのクラッシュレポート送信に関する依存関係は追加しないでください。

依存関係を変更する場合は、その依存が必要な理由、保守状況とライセンス、
OS の権限やネットワーク利用の有無、unsafe コードやネイティブライブラリを使うかどうかを明記してください。
