# Contributing

English: [CONTRIBUTING.md](CONTRIBUTING.md)

> [!NOTE]
> このリポジトリは公開のみを目的としており、Issue・Pull Request・Discussion など外部からの意見や貢献は受け付けていません（Issue・Discussion は無効、Pull Request は collaborator のみ）。以下は開発者向けの手順です。

設計の基準は [`docs/design.md`](docs/design.md) です。
挙動・プライバシー・セキュリティ・フェーズ境界に関わる変更は、設計と矛盾しないか確認してください。

## 開発環境

固定された開発環境を使うため、`nix develop` に入ってください。

## 提出前チェック

Rust 依存を変更した場合は、同梱するライセンス通知を再生成してください。

```sh
python3 tools/update-rust-license-notices.py
```

生成された `app/assets/licenses/RUST-DEPENDENCY-LICENSES.txt` も依存変更と
一緒にコミットしてください。

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources

# Kotlin Multiplatform / Compose
cd kotlin
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain :desktopApp:test :desktopApp:compileKotlin

# Linux Desktop版Compose UIの起動（filesystem Vaultと画像）
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" \
  ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable

# Nix 開発環境からLinux向けAndroid native libraryをビルド
rustup target add --toolchain 1.98.1 aarch64-linux-android
PATH="$(dirname "$(rustup which cargo --toolchain 1.98.1)"):$PATH" \
  env "CC_aarch64-linux-android=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
    "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
  ./gradlew :core:rust:cargoBuildAarch64AndroidDebug

# Flutter
cd app
dart format lib test
flutter analyze
flutter test
```

## ルール

> [!IMPORTANT]
> Vault を編集・書き込みしてはいけません。スキャナーやテストも同様です。

- **架空データのみ**: テスト・ドキュメント・Issue・プルリクエストには、架空のノートとメディアだけを追加します。
- **言語**: 移行先は Rust と Kotlin です。platform parity の確認が終わるまで既存 Dart/Flutter も動作させ、無関係な実装言語を増やさないでください。
- **安全性**: core／parser は `forbid(unsafe_code)`、オフライン、入力上限あり、ログ・エラーにノート内容を含めない。
- **パスと保存先**: パスは開く前に検証し、索引・キャッシュは Vault 外のプライベートなアプリデータに置きます。
- **ネットワーク**: 通信・テレメトリ・広告・リモートのクラッシュレポートに関する依存は追加しません。
- **フェーズ**: 1回に1フェーズずつ完了・検証し、将来フェーズの UI を Phase 1 の core に入れないでください。

## Changelog とリリース

- 各リリースの `CHANGELOG.md` は AI が実装差分と実際の検証結果を基に草案・更新します。
- AI は未確認の動作、実施していないテスト、将来予定を完了済みのように書かず、秘密情報や個人情報を含めません。
- リリース担当者は内容とタグのバージョンを確認してからコミット・公開します。リリーススクリプトは該当バージョンの changelog 節を Release notes として使います。

## 依存関係を変更するとき

プルリクエストに次を明記してください。

- [ ] その依存が必要な理由
- [ ] 保守状況とライセンス
- [ ] OS 権限・ネットワーク利用の有無
- [ ] unsafe コードやネイティブライブラリの使用有無
