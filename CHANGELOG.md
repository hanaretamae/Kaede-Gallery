# Changelog

このファイルの各リリース項目は AI が実装差分と検証記録から草案を作成し、
リリース担当者が確認して公開します。

## [Unreleased]

## [1.2.1] - 2026-10-03

### Fixed

- ビューアーの詳細表示と画像単体表示を切り替えるとき、パネルの移動と上下 UI のフェードを同じアニメーションに揃えました。

### Changed

- Android のビルド番号を `7` に更新し、Flutter アプリのバージョンを `1.2.1` にしました。

## [1.2.0] - 2026-10-03

### Changed

- ギャラリー共通状態と、動画・SAF画像サムネイル処理のプロバイダーを分離しました。
- ビューアーの詳細スクロールで画面全体を再構築せず、メディア領域と詳細パネルの配置だけを更新するようにしました。
- ギャラリータイルの二重クリップをなくし、スクロール時の描画負荷を軽減しました。
- Android のビルド番号を `6` に更新し、Flutter アプリのバージョンを `1.2.0` にしました。
- ドキュメントの対応フェーズと Linux / Android の説明を現状に合わせました。

## [1.1.0] - 2026-10-03

### Added

- タグ絞り込みパネルで、作成日・公開日と昇順・降順を選んで一覧を並べ替えられるようにしました。

### Fixed

- ビューアーで画像全体の読み込み前に小さいサムネイルが表示されるちらつきをなくしました。

### Changed

- アプリのバージョンを `1.1.0`（Android build number `5`）に更新しました。
- 一覧の既定の並び順を作成日降順にしました。

## [1.0.3] - 2026-10-03

### Changed

- Home Manager のインストール例を `inputs.kaede-gallery` を使う設定に更新しました。
- アプリのバージョンを `1.0.3`（Android build number `4`）に更新しました。

## [1.0.2] - 2026-10-03

### Fixed

- Nix パッケージにデスクトップエントリを追加し、アプリランチャーから起動できるようにしました。

### Changed

- NixOS / Home Manager module で利用するシステム属性を `pkgs.stdenv.hostPlatform.system` に更新しました。
- アプリのバージョンを `1.0.2`（Android build number `3`）に更新しました。
- Android リリース時の KeePassXC データベースとキーファイルのパスを必須の実行時指定にしました。

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
