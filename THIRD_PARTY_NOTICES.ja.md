# サードパーティ通知

Kaede Gallery のアプリケーションコードは [MIT License](LICENSE) です。
現在の Android / Compose Desktop アプリは Kotlin Multiplatform / Compose と Rust で構成します。
オフラインのアプリ内「オープンソースライセンス」画面と配布物には
`kotlin/shared-assets/licenses/` のライセンス文書を同梱します。

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
| Android APK       | AndroidX Media3、Kotlin / Compose、Rust UniFFI bindings                         | AndroidX および Kotlin / Compose の実行時コンポーネントは Apache-2.0 です。Rust の通知とライセンス全文は APK 内のオフライン画面と Release archive に含まれます。                                                                |
| Windows x64 ZIP   | Compose Desktop、JNA 5.19.1、Rust UniFFI bindings                               | JNA は Apache-2.0 または LGPL-2.1-or-later のデュアルライセンスです。ZIP には両方の選択肢を収録します。Windows で動画再生・サムネイル抽出を使うには `mpv` と `ffmpeg` が `PATH` に必要です。実行ファイルは ZIP に同梱しません。 |
| Linux Nix package | Compose Desktop、JNA 5.19.1、nixpkgs の `mpv`、`ffmpeg`、`libass`、`libplacebo` | package wrapper は nixpkgs で固定した実行時依存を使います。FFmpeg は GPL 対応でビルドされるため、Linux package は GPL-3.0-or-later で配布します。`GPL-3.0.txt`、`LGPL-2.1.txt`、`ISC.txt` を参照してください。                  |

Compose Desktop の Linux / Windows 配布物には JNA 5.19.1 を同梱します。
JNA JAR にも `META-INF/LICENSE` の通知とライセンス参照があります。
ソース: <https://github.com/java-native-access/jna>

## 同梱ライセンスファイル

KMP アプリは `kotlin/shared-assets/licenses/` の共有ライセンス資産を配布物へ同梱します。
Kotlin、Compose、AndroidX、Media3 の実行時グループ向け Apache-2.0、JNA の
Apache-2.0 / LGPL-2.1-or-later、該当するプラットフォーム・実行時通知、および
MPL-2.0 の UniFFI 項目を含む生成済み Rust 依存レポートを収録します。
Linux Nix package と Windows ZIP には本体ライセンスとこの通知文書も含まれます。

## Rust 依存関係

`RUST-DEPENDENCY-LICENSES.txt` は出荷アプリの Rust 依存をライセンス別に列挙し、
パッケージ名・バージョン・ライセンス全文・依存元に含まれる `NOTICE` / 著作権表示を収録します。
`gallery-ffi` から到達できる実行時依存のみが対象です。

## macOS

macOS は未対応で、配布物はありません。
