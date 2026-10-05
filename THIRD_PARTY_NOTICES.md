# サードパーティ通知

Kaede Gallery 本体は [MIT License](LICENSE) です。配布物には次のネイティブ
ライブラリが同梱されます。Dart / Flutter パッケージのライセンスはアプリ内
「オープンソースライセンス」画面に表示されます。Rust 依存は MIT / Apache-2.0 /
Unicode-3.0 / Zlib のみで、`cargo deny check licenses` で検証しています。

## 動画再生・サムネイル用ライブラリ

| 対象 | ライブラリ | ライセンス | 入手元 |
|------|-----------|-----------|--------|
| Android APK | libmpv・FFmpeg（media-kit/libmpv-android-video-build v1.1.7 の `default` ビルド。mpv は `-Dgpl=false`） | LGPL-2.1-or-later | https://github.com/media-kit/libmpv-android-video-build/releases/tag/v1.1.7 |
| Windows ZIP | libmpv・FFmpeg（media-kit/libmpv-win32-video-build 2023-09-24。下記参照） | LGPL-3.0-or-later | https://github.com/media-kit/libmpv-win32-video-build |
| Linux バンドル | mpv 0.41.0・FFmpeg 9.0.1・libass・libplacebo（nixpkgs の `flake.lock` で固定） | FFmpeg は `--enable-gpl --enable-version3` でビルドされており GPL-3.0-or-later | https://github.com/NixOS/nixpkgs （`flake.lock` の rev） |

- Android では上記ライブラリは共有ライブラリ（`.so`）として同梱されており、
  同じ ABI の互換ビルドに差し替えられます。
- **Linux バンドル（`kaede-gallery-*-linux-*`）は GPL-3.0-or-later のライブラリを
  含むため、バンドル全体を GPL-3.0-or-later の条件で配布します**（全文:
  https://www.gnu.org/licenses/gpl-3.0.txt）。MIT の本体コードは GPL-3.0 と互換です。
  本体の完全なソースは本リポジトリ（該当リリースのタグ）で公開しており、
  同梱ライブラリのソースは上記 nixpkgs の固定リビジョンから入手できます。
  Android APK と、MIT のソースコード自体には GPL は及びません。

## Dart / Flutter パッケージ

依存パッケージのライセンスを確認した結果、GPL / AGPL 系はありません。大半は
BSD-3-Clause・MIT・Apache-2.0 です。例外として `dbus` は MPL-2.0 です
（ファイル単位のコピーレフトで、本プロジェクトは未改変のまま利用しています）。
全文はアプリ内「オープンソースライセンス」画面で確認できます。
- 各ライブラリの著作権表示とライセンス全文は、各プロジェクトの配布物を参照してください。
  - mpv: https://github.com/mpv-player/mpv （GPL-2.0-or-later / LGPL-2.1-or-later）
  - FFmpeg: https://ffmpeg.org/legal.html
  - libass: https://github.com/libass/libass （ISC）
  - libplacebo: https://code.videolan.org/videolan/libplacebo （LGPL-2.1-or-later）

## Windows

Windows の ZIP には、`media_kit_libs_windows_video` が取得する libmpv（media-kit/libmpv-win32-video-build
2023-09-24 の `video` ビルド）と ANGLE（OpenGL ES 実装、BSD-3-Clause）が含まれます。libmpv は
`--disable-gpl --enable-version3` でビルドされており、FFmpeg 部分は LGPL-3.0-or-later です
（同梱の DLL で確認）。ソースは https://github.com/media-kit/libmpv-win32-video-build と
https://github.com/mpv-player/mpv から入手できます。libmpv は動的ライブラリ（`libmpv-2.dll`）として
同梱され、差し替えられます。このライブラリは x86_64 のみで、arm64 版は実験的です。

## macOS

macOS は未対応で、配布物はありません。
