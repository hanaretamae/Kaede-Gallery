# サードパーティ通知

English: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

Kaede Gallery 本体は [MIT License](LICENSE) です。Dart / Flutter パッケージの
ライセンスはアプリ内「オープンソースライセンス」画面に表示されます。
同梱ネイティブライブラリと Rust 依存のライセンス全文も同画面で確認でき、
`app/assets/licenses/` に含まれます。Windows ZIP と Nix パッケージにも同じ
ファイルを収録します。APK 利用者向けにはリリース添付物
`kaede-gallery-third-party-licenses.tar.gz` も提供します。

Rust 依存通知は Android、Windows x86_64 / ARM64、Linux x86_64 / ARM64 の
`gallery-bridge` と `gallery-ffi` の出荷対象 Cargo 依存グラフから生成します。
採用した SPDX ライセンス全文、
対象パッケージ名・バージョン、および依存パッケージに含まれる `NOTICE` /
著作権表示ファイルを収録します。許可するライセンス表現は `about.toml` と
`deny.toml` で管理します。Rust 依存を変更した場合は
`python3 tools/update-rust-license-notices.py` で再生成してください。

## 動画再生・サムネイル用ライブラリ

| 対象           | ライブラリ                                                                                                | ライセンス                                                                         | ライセンス全文                           |
| -------------- | --------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------- |
| Android APK    | libmpv・FFmpeg（`media-kit/libmpv-android-video-build` v1.1.7 の `default` ビルド。mpv は `-Dgpl=false`） | LGPL-2.1-or-later                                                                  | `LGPL-2.1.txt`                           |
| Windows ZIP    | libmpv・FFmpeg（`media-kit/libmpv-win32-video-build` 2023-09-24）                                         | LGPL-3.0-or-later                                                                  | `LGPL-3.0.txt`                           |
| Linux バンドル | mpv 0.41.0・FFmpeg 9.0.1・libass・libplacebo（`flake.lock` で固定）                                       | FFmpeg は `--enable-gpl --enable-version3`。バンドル全体を GPL-3.0-or-later で配布 | `GPL-3.0.txt`、`LGPL-2.1.txt`、`ISC.txt` |

Android の共有ライブラリ（`.so`）と Windows の `libmpv-2.dll` は動的リンクで、
差し替え可能です。ソース / ビルドプロジェクト:

- Android: https://github.com/media-kit/libmpv-android-video-build (v1.1.7)
- Windows: https://github.com/media-kit/libmpv-win32-video-build
- Linux 依存: `flake.lock` で固定した nixpkgs。パッケージ定義とソース参照を含みます
- mpv: https://github.com/mpv-player/mpv
- FFmpeg: https://ffmpeg.org/legal.html
- libass: https://github.com/libass/libass
- libplacebo: https://code.videolan.org/videolan/libplacebo

Linux バンドルの Nix package metadata も、同梱アプリの配布条件と一致する
GPL-3.0-or-later を指定しています。

## Kotlin Multiplatform 依存関係

KMP Android アプリと Compose Desktop アプリには、オフラインで使える
アプリ内ライセンス画面があります。共有ライセンス資産は
`app/assets/licenses/` から配布物へ同梱します。画面には Kotlin、Compose、
AndroidX、Media3 などの実行時依存に対する Apache-2.0（JNA の選択肢にも使用）、
JNA のもう一方の選択肢である LGPL-2.1-or-later、およびパッケージ名・バージョン・
通知・ライセンス全文を
含む Rust 依存レポート（MPL-2.0 の UniFFI 項目を含む）を収録します。

Compose Desktop の Linux / Windows 配布物には JNA 5.19.1 を同梱します。
JNA は Apache-2.0 または LGPL-2.1-or-later のデュアルライセンスです。
同梱する JNA JAR にも `META-INF/LICENSE` の通知とライセンス参照があります。
ソース: https://github.com/java-native-access/jna

## Windows グラフィックスライブラリ

Windows ZIP には `media_kit_libs_windows_video` が使用する ANGLE v1.0.1 の
バイナリアーカイブを含みます。CMake は `ANGLE.7z` を MD5
`e866f13e8d552348058afaafe869b1ed` で固定しています。アーカイブ自体には
ライセンスや通知ファイルがないため、上流のライセンス本文と著作権表示を
アプリ側で同梱します。CMake は以下の実行時ライブラリをコピーします。

| ファイル                      | コンポーネント / ライセンス                                                       | ライセンス全文 / 入手元                                                                                |
| ----------------------------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `libEGL.dll`、`libGLESv2.dll` | ANGLE、BSD-3-Clause                                                               | `ANGLE-LICENSE.txt`; https://github.com/google/angle                                                   |
| `vk_swiftshader.dll`          | SwiftShader、Apache-2.0                                                           | `SwiftShader-LICENSE.txt`、`SwiftShader-AUTHORS.txt`; https://github.com/google/swiftshader            |
| `vulkan-1.dll`                | Vulkan Loader、Apache-2.0                                                         | `Vulkan-Loader-LICENSE.txt`、`Vulkan-Loader-NOTICE.txt`; https://github.com/KhronosGroup/Vulkan-Loader |
| `zlib.dll`                    | zlib                                                                              | `zlib-LICENSE.txt`; https://github.com/madler/zlib/tree/v1.2.13                                        |
| `d3dcompiler_47.dll`          | Microsoft Direct3D Compiler 再配布 DLL。ANGLE の OSS ライセンス対象ではありません | `MICROSOFT-WINDOWS-SDK-REDIST.txt`; https://learn.microsoft.com/en-us/legal/windows-sdk/redist         |

DLL 内の Authenticode 署名は Microsoft Corporation を署名者として示します。
Microsoft は `d3dcompiler_47.dll` を従来型 Windows アプリ向け再配布可能ファイルに
掲載しています。再配布には該当する Windows SDK の条件が適用されます。
ベンダーアーカイブに SDK の正確な版は記録されていないため、通知には公式の
再配布リストへのリンクと、その来歴上の制約を記載しています。
Windows ビルド workflow はパッケージ作成前に Authenticode 署名と
Microsoft の署名者 ID を検証します。
Microsoft DLL は上記 OSS ライセンスで再許諾されるものではありません。

アーカイブには `libc++.dll` もありますが、現在の CMake の同梱ライブラリ一覧では
Windows アプリのバンドルにコピーされません。

## Dart / Flutter パッケージ

Dart / Flutter 依存のライセンス全文は Flutter のライセンスレジストリに含まれ、
アプリ内「オープンソースライセンス」画面に表示されます。`dbus` は MPL-2.0
で未改変利用しています。その全文も他の Dart / Flutter 依存と同じ画面で確認できます。

## Rust 依存

生成された `RUST-DEPENDENCY-LICENSES.txt` は、配布アプリが使う Rust 依存を
ライセンス別に列挙し、パッケージ名・バージョン、ライセンス全文、依存元に含まれる
`NOTICE` / 著作権表示を収録します。`about.toml` で指定した配布対象 7 Rust
ターゲットを確認し、ビルド専用・開発専用依存は除外しています。

## macOS

macOS は未対応で、配布物はありません。
