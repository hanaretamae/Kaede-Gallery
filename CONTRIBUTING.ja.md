# Contributing

English: [CONTRIBUTING.md](CONTRIBUTING.md)

> [!NOTE]
> このリポジトリは公開のみを目的としており、Issue・Pull Request・Discussion など外部からの意見や貢献は受け付けていません（Issue・Discussion は無効、Pull Request は collaborator のみ）。以下は開発者向けの手順です。

設計の基準は [`docs/design.md`](docs/design.md) です。
挙動・プライバシー・セキュリティ・フェーズ境界に関わる変更は、設計と矛盾しないか確認してください。

## 開発環境

固定された開発環境を使うため、`nix develop` に入ってください。LinuxではPortalクライアントがJNA経由でGIOを読み込めるよう、開発シェルがGLibのライブラリディレクトリを`LD_LIBRARY_PATH`に追加します。

## 提出前チェック

Rust 依存を変更した場合は、同梱するライセンス通知を再生成してください。

```sh
python3 tools/update-rust-license-notices.py
```

Android と Desktop の両アプリが同梱する `kotlin/shared-assets/licenses/RUST-DEPENDENCY-LICENSES.txt` を更新します。

```sh
# Rust
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
cargo deny check advisories bans licenses sources

# Kotlin Multiplatform / Compose（リポジトリ root から）
cd kotlin
./gradlew :core:model:allTests :core:settings:allTests :core:rust:allTests :ui:app:allTests :ui:app:compileKotlinJvm :ui:app:compileAndroidMain :desktopApp:test :desktopApp:compileKotlin

# Linux Compose Desktop の起動と配布物作成
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" \
  ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable

# Linux x86_64 で Android debug APK をビルド（nix develop 内）
set -euo pipefail
# target がなければ停止し、Gradle の retry loop を開始しないでください。
export RUSTUP_TOOLCHAIN=1.98.1
rustup target list --installed --toolchain "$RUSTUP_TOOLCHAIN" | grep -Fx aarch64-linux-android
rustup target list --installed --toolchain "$RUSTUP_TOOLCHAIN" | grep -Fx x86_64-linux-android
export RUSTC="$(rustup which rustc --toolchain "$RUSTUP_TOOLCHAIN")"
export RUSTDOC="$(rustup which rustdoc --toolchain "$RUSTUP_TOOLCHAIN")"
export PATH="$(dirname "$RUSTC"):$PATH"
NDK_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
export CC_aarch64_linux_android="$NDK_BIN/aarch64-linux-android23-clang"
export CXX_aarch64_linux_android="$NDK_BIN/aarch64-linux-android23-clang++"
export AR_aarch64_linux_android="$NDK_BIN/llvm-ar"
export RANLIB_aarch64_linux_android="$NDK_BIN/llvm-ranlib"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK_BIN/aarch64-linux-android23-clang"
export CC_x86_64_linux_android="$NDK_BIN/x86_64-linux-android23-clang"
export CXX_x86_64_linux_android="$NDK_BIN/x86_64-linux-android23-clang++"
export AR_x86_64_linux_android="$NDK_BIN/llvm-ar"
export RANLIB_x86_64_linux_android="$NDK_BIN/llvm-ranlib"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$NDK_BIN/x86_64-linux-android23-clang"
./gradlew :androidApp:assembleDebug
```

APK は `kotlin/androidApp/build/outputs/apk/debug/` に作成されます。debug 用の署名であり、production の署名鍵は使いません。通常はローカルビルドを使ってください。任意の GitHub Actions workflow は手動起動のみで、Android debug APK、Linux AArch64 bundle、または両方をビルドできます。push／pull request では起動せず、リリース公開もしません。Workflow が GitHub 上で利用可能になった後は、Nix shell から次のコマンドで起動できます。

```sh
tools/run-optional-builds.sh android-debug
# linux-aarch64 / both も選べます。第2引数は任意の ref です。
tools/run-optional-builds.sh both main
```

GitHub CLI (`gh auth login`) で認証しておく必要があります。ローカル・任意Actionsでのビルドとリリース全体は[リリース手順](docs/releasing.ja.md)を参照してください。

## ルール

> [!IMPORTANT]
> Vault を編集・書き込みしてはいけません。スキャナーやテストも同様です。

- **架空データのみ**: テスト・ドキュメント・Issue・プルリクエストには、架空のノートとメディアだけを追加します。
- **言語**: 現在の実装は Rust と Kotlin/Compose です。Flutter は退役しました。並行する UI を再導入せず、無関係な実装言語も増やさないでください。
- **安全性**: core／parser は `forbid(unsafe_code)`、オフライン、入力上限あり、ログ・エラーにノート内容を含めない。
- **パスと保存先**: パスは開く前に検証し、索引・キャッシュは Vault 外のプライベートなアプリデータに置きます。
- **ネットワーク**: 通信・テレメトリ・広告・リモートのクラッシュレポートに関する依存は追加しません。
- **フェーズ**: 1回に1フェーズずつ完了・検証し、将来フェーズの UI を Phase 1 の core に入れないでください。

## Changelog とリリース

- 各リリースの `CHANGELOG.md` は AI が実装差分と実際の検証結果を基に草案・更新します。
- AI は未確認の動作、実施していないテスト、将来予定を完了済みのように書かず、秘密情報や個人情報を含めません。
- リリース担当者は内容とタグのバージョンを確認してからコミット・公開します。アプリの唯一の version source は `VERSION` です。`tools/release.sh` は署名済み KMP Android APK と x86_64 / aarch64 Linux bundle をビルドします。AArch64 Linux package は x86_64 上で Rust FFI をクロスコンパイルし、AArch64 用の Compose native runtime、JRE、launcher、library を組み立てます。QEMU/binfmt や remote builder は不要です。Rustup 1.98.1 の Android targets は事前にインストール済みのものだけを使います。タグは `VERSION` と完全一致させ、`v` を付けません。`2.0.0b1` のような beta タグは GitHub の prerelease として公開します。Windows の手動 workflow は KMP を package 化し、tag を指定して起動すると既存 GitHub Release に ZIP を添付します。別の Android/Linux AArch64 workflow は `workflow_dispatch` のみで、リリース署名・公開には使いません。Release 公開にはリリース担当者の認証情報と対象環境の検証が必要です。

## 依存関係を変更するとき

プルリクエストに次を明記してください。

- [ ] その依存が必要な理由
- [ ] 保守状況とライセンス
- [ ] OS 権限・ネットワーク利用の有無
- [ ] unsafe コードやネイティブライブラリの使用有無
