# サードパーティ通知

Kaede Gallery のアプリケーションコードは [MIT License](LICENSE) です。
現在の Android / Compose Desktop アプリは Kotlin Multiplatform / Compose と Rust で構成します。
オフラインのアプリ内「オープンソースライセンス」画面と配布物には
`kotlin/shared-assets/licenses/` のライセンス文書を同梱します。
アプリ内ビューアと Android / Desktop のアプリ資産には、プロジェクトの MIT License、
この通知文書、およびプラットフォーム・実行環境ライセンスをまとめた生成ファイルも含めます。
これらは Kotlin ビルド時に管理対象のライセンス文書から生成します。

Rust 依存レポートは、配布アプリが使う `gallery-ffi` の Cargo 依存グラフから生成します。
パッケージ名・バージョン、採用した SPDX ライセンス全文、依存パッケージ内の
`NOTICE` / 著作権表示を収録します。許可するライセンス表現は `about.toml` と
`deny.toml` で管理します。Rust 依存を変更した場合は次を実行してください。

```sh
python3 tools/update-rust-license-notices.py
```

レポートの対象は `about.toml` に記載した 7 つの Rust target triple です。
ビルド専用・開発専用依存は含めません。

## プラットフォームとメディア依存関係

| 対象              | 実行時コンポーネント                                                            | ライセンス・配布に関する注記                                                                                                                                                                                                    |
| ----------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android APK       | AndroidX Media3、Kotlin / Compose、Skydoves Compose ColorPicker 1.3.0、Rust UniFFI bindings | AndroidX、Kotlin / Compose、および Skydoves Compose ColorPicker は Apache-2.0 です。Rust の通知とライセンス全文は APK 内のオフライン画面と Release archive に含まれます。                                                         |
| Windows ZIP（x64 / ARM64） | Compose Desktop、Skydoves Compose ColorPicker 1.3.0、JNA 5.19.1、Rust UniFFI bindings | Skydoves Compose ColorPicker は Apache-2.0 です。JNA は Apache-2.0 または LGPL-2.1-or-later のデュアルライセンスです。ZIP には両方の選択肢を収録します。ZIP の `tools/` に、固定した GPL ビルドの `mpv.exe` と `ffmpeg.exe`（zhongfly/mpv-winbuild の 2026-10-11-88eb9f2e73、CI で SHA-256 を検証）を同梱します。これらの実行ファイルは GPL-3.0-or-later の条件で配布されます（`licenses/GPL-3.0.txt` を参照）。対応するソースは mpv-winbuild と upstream の mpv / FFmpeg から入手できます。 |
| 安定版 2.0.0 Linux 配布物 | `PATH` 上のシステム提供 `mpv` と `ffmpeg` | 公開済みの 2.0.0 `.tar.zst` と AppImage はメディアツールを再配布しません。ユーザーが個別にインストールします。 |
| 2.0.1 以降の Linux リリース bundle | 固定 nixpkgs `mpv`、`ffmpeg`、`libass`、`libplacebo` を含む自己完結Nix bundle | 固定した nixpkgs のソースとpackage定義は `flake.lock` に記録されています。2.0.1 の構成では両メディアツールと実行時依存、サードパーティ通知、ライセンス本文を同梱します。FFmpegはGPL対応ビルドです。`licenses/GPL-3.0.txt`、`licenses/LGPL-2.1.txt`、`licenses/LGPL-3.0.txt`、`licenses/ISC.txt` を参照してください。mpv と FFmpeg のソースプロジェクトは [mpv](https://github.com/mpv-player/mpv) と [FFmpeg](https://ffmpeg.org/) です。 |
| Linux Nix package | Compose Desktop、MaterialKolor 6.0.0-beta01、Skydoves Compose ColorPicker 1.3.0、JNA 5.19.1、nixpkgs の `mpv`、`ffmpeg`、`libass`、`libplacebo` | MaterialKolor と Material Color Utilities は KDE/XDG のアクセント色を使った M3 Expressive 配色生成に使い、MIT / Apache-2.0 条件です。Skydoves Compose ColorPicker は Apache-2.0 です。package wrapper は nixpkgs で固定した実行時依存を使います。FFmpeg は GPL 対応でビルドされるため、Linux package は GPL-3.0-or-later で配布します。`Apache-2.0.txt`、`GPL-3.0.txt`、`LGPL-2.1.txt`、`ISC.txt` を参照してください。 |

Compose Desktop の Linux / Windows 配布物には JNA 5.19.1 を同梱します。
JNA JAR にも `META-INF/LICENSE` の通知とライセンス参照があります。
ソース: <https://github.com/java-native-access/jna>

任意で選択できる Google Sans Flex フォントは SIL Open Font License 1.1 に基づいて
共有 UI に同梱します。ライセンス本文はオフラインビューアと
`kotlin/shared-assets/licenses/GoogleSansFlex-OFL.txt` に収録します。

## 同梱ライセンスファイル

KMP アプリは `kotlin/shared-assets/licenses/` の共有ライセンス資産を配布物へ同梱します。
Kotlin、Compose、AndroidX、Media3、Skydoves Compose ColorPicker の実行時グループ向け Apache-2.0、JNA の
Apache-2.0 / LGPL-2.1-or-later、該当するプラットフォーム・実行時通知、および
MPL-2.0 の UniFFI 項目を含む生成済み Rust 依存レポートを収録します。
Linux Nix package と Windows ZIP には本体ライセンスとこの通知文書も含まれます。

## Rust 依存関係

`RUST-DEPENDENCY-LICENSES.txt` は出荷アプリの Rust 依存をライセンス別に列挙し、
パッケージ名・バージョン・ライセンス全文・依存元に含まれる `NOTICE` / 著作権表示を収録します。
`gallery-ffi` から到達できる実行時依存のみが対象です。

## macOS

macOS は未対応で、配布物はありません。
