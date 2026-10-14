# Contributing

English: [CONTRIBUTING.md](CONTRIBUTING.md)

> [!NOTE]
> このリポジトリは配布を目的として保守しています。公開 Issue・Discussion は
> 一般サポート窓口ではなく、Pull Request は collaborator に限定しています。
> 以下は開発者向けの手順です。

変更前に [`AGENTS.md`](AGENTS.md)、[AI 向け指示](.github/copilot-instructions.md)、
[`docs/design.md`](docs/design.md) を確認してください。
アーキテクチャ・挙動・プライバシー・セキュリティ・フェーズ境界については
設計文書を基準とし、矛盾があれば黙って要件を変更せずに報告してください。

## 現行の実装

アプリは Kotlin Multiplatform / Compose Multiplatform で構成し、
Rust がパース・索引・絞り込み・サムネイル・UniFFI 境界を担当します。
Flutter/Dart と旧 bridge は退役済みです。現行のアプリ・ビルド・リリース経路として
再導入しないでください。

## 開発環境

`nix develop` で固定されたツールチェーンを使ってください。
Rust、JDK、Android、native build に必要なツールが含まれます。

## 安全性と実装ルール

- ユーザーの Obsidian Vault にアクセス・編集・書き込みをしないでください。
  テスト・ドキュメントを含め、架空の fixture のみを使います。
- Vault は読み取り専用にします。ファイルを開く前にパスを検証し、
  索引・キャッシュは Vault 外のプライベートなアプリデータに保存します。
- core／parser はオフラインかつ入力上限付きで動作させます。
  ログ・エラーにノート内容、パス、タグ、URL を含めません。
- Rust の `forbid(unsafe_code)` とネットワーク依存禁止を維持します。
- テレメトリ・広告・リモートクラッシュレポートや、無関係な言語・並行 UI を追加しません。
- 英語と日本語の利用者向け文字列・ドキュメントを揃えます。
- 変更は依頼された挙動と一つの開発フェーズに限定します。

## チェック

変更をカバーする最小のチェックを実行してください。
共通 interface・依存関係・複数 platform に影響する場合は、より広い suite も実行します。

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources
```

```sh
# Kotlin Multiplatform / Compose（kotlin/ から）
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests \
  :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain \
  :desktopApp:test :desktopApp:compileKotlin

# Linux Compose Desktop のテストとローカル配布物
./gradlew :desktopApp:test
./gradlew :desktopApp:createDistributable
```

Android debug APK は Android Rust targets のインストールと NDK cross-compiler
の設定後に `:androidApp:assembleDebug` でビルドできます。
platform ごとの設定とリリース手順は[リリースガイド](docs/releasing.ja.md)を参照してください。
debug build は production 署名済みの release ではありません。

チェック後に `git diff --check` を実行してください。

## 依存関係とライセンス

依存関係を変更するときは、必要な理由・保守状況・ライセンス・OS 権限と
ネットワーク利用・unsafe code または native library の有無を記録します。
Rust の依存を変更した場合は、実行時依存のライセンスレポートを再生成します。

```sh
python3 tools/update-rust-license-notices.py
```

## リリース文書

恒常的なドキュメントは version-neutral に保ちます。未確認の挙動・テスト・
platform acceptance・配布物について、確認済みのように記述しないでください。
リリース固有の情報は GitHub Release metadata または versioned release notes に記録します。
[日英のリリースガイド](docs/releasing.ja.md)に従ってください。
リリース担当者の明示的な許可なく `VERSION` を変更したり、tag を作成・移動したり、
配布物を公開したりしないでください。
