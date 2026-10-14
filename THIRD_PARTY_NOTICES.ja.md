# サードパーティ通知

Kaede Gallery のアプリケーションコードは
[MIT License](LICENSE) です。アプリは第三者ライブラリを使用し、
Desktop 配布物にはそれぞれ異なるライセンスのメディアツールも含みます。
アプリ内の「オープンソースライセンス」画面と配布物には、本体のライセンス、
この通知、および `kotlin/shared-assets/licenses/` から組み立てたライセンス文書を収録します。

## コンポーネント

| コンポーネント／配布物 | 第三者コンポーネント | ライセンス情報 |
| --- | --- | --- |
| Android アプリ | Kotlin / Compose、AndroidX、Media3、MaterialKolor、Skydoves Compose ColorPicker、Rust UniFFI 依存関係 | Kotlin / Compose、AndroidX、Media3、Material Color Utilities、Skydoves Compose ColorPicker は Apache-2.0 です。MaterialKolor は MIT です。Rust の実行時依存は `RUST-DEPENDENCY-LICENSES.txt` に記載します。Android の動画再生には Media3 を使います。 |
| Compose Desktop アプリ | Compose、MaterialKolor、Skydoves Compose ColorPicker、JNA、Rust UniFFI 依存関係 | MaterialKolor は MIT、Material Color Utilities と Skydoves Compose ColorPicker は Apache-2.0 です。JNA は Apache-2.0 または LGPL-2.1-or-later のデュアルライセンスで、配布物には両方のライセンス文を収録します。Rust の実行時依存は `RUST-DEPENDENCY-LICENSES.txt` に記載します。 |
| Linux Desktop package | Desktop アプリと Nix runtime closure（mpv、FFmpeg、libass、libplacebo を含む） | Nix の依存関係は `flake.lock` で固定します。FFmpeg は GPL 対応でビルドし、package には GPL-3.0-or-later の条件と、該当する LGPL その他 runtime のライセンス文を含めます。 |
| Windows Desktop 配布物 | Desktop アプリと同梱の `mpv.exe`・`ffmpeg.exe` | メディア実行ファイルは [mpv-winbuild](https://github.com/zhongfly/mpv-winbuild) が公開する GPL ライセンスの build で、配布物には GPL-3.0-or-later の条件を含めます。ソース情報は upstream の [mpv](https://github.com/mpv-player/mpv) と [FFmpeg](https://ffmpeg.org/) を参照してください。 |

任意で選択できる Google Sans Flex フォントは SIL Open Font License 1.1
に基づいて同梱します。ライセンスは
`kotlin/shared-assets/licenses/GoogleSansFlex-OFL.txt` にあります。

共有ライセンス資産には ANGLE、SwiftShader、Vulkan Loader、Windows SDK
再頒布可能ファイルなど、platform・runtime に関する通知も含みます。
配布物ごとの runtime に応じて、build 時に必要な文書を組み立てます。

## Rust 依存レポート

`RUST-DEPENDENCY-LICENSES.txt` は、配布アプリが使う `gallery-ffi` の
Cargo 依存グラフから生成します。`about.toml` に設定した target triple の
実行時 package・version・選択した SPDX ライセンス全文と、依存元に含まれる
`NOTICE` / 著作権表示を列挙します。ビルド専用・開発専用依存は除外します。
許可するライセンス表現は `about.toml` と `deny.toml` で管理します。

Rust の依存関係を変更した場合はレポートを再生成してください。

```sh
python3 tools/update-rust-license-notices.py
```

## ライセンスファイル

`kotlin/shared-assets/licenses/` には、アプリと配布物で使うライセンス文書・通知を置きます。
Apache-2.0、MIT、GPL-3.0、LGPL-2.1、LGPL-3.0、ISC、zlib、および生成済み Rust
依存レポートなどを含みます。build 時にオフラインのライセンス画面と配布用資産へ組み立てます。
