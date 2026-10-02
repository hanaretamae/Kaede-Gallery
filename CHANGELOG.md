# Changelog

このファイルの各リリース項目は AI が実装差分と検証記録から草案を作成し、
リリース担当者が確認して公開します。

## [Unreleased]

## [1.0.1] - 2026-10-03

### Changed

- Android の署名リリース手順が KeePassXC から鍵とパスワードを一時取得し、Gradle デーモンを無効にするようになりました。
- Rust core のノート登録処理で関連コンテキストをまとめ、strict Clippy の警告を解消しました。

### Verified

- Rust format、strict Clippy、workspace tests、cargo-deny を実行しました。
- Dart format、Flutter analyze、Flutter tests を実行しました。

## [1.0.0] - 2026-10-03

### Added

- Android の Storage Access Framework を使い、選択した Vault を読み取り専用で閲覧できるようにしました。
- NixOS / Home Manager 向けの x86_64 Linux package と module を追加しました。
- Android arm64 APK をローカルで署名ビルドし、GitHub Release に添付する手順を追加しました。

### Verified

- Android 16 の実機で、SAF による選択、再起動後の権限保持、ノート索引、画像表示、短い MP4 の再生を確認しました。
- Rust workspace tests、Flutter analyze、Flutter widget tests を実行しました。
