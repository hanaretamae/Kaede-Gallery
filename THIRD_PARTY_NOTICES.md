# サードパーティ通知

Kaede Gallery 本体は [MIT License](LICENSE) です。配布物には次のネイティブ
ライブラリが同梱されます。Dart / Flutter パッケージのライセンスはアプリ内
「オープンソースライセンス」画面に表示されます。Rust 依存は MIT / Apache-2.0 /
Unicode-3.0 / Zlib のみで、`cargo deny check licenses` で検証しています。

## 動画再生・サムネイル用ライブラリ

| 対象 | ライブラリ | ライセンス | 入手元 |
|------|-----------|-----------|--------|
| Android APK | libmpv・FFmpeg（media-kit/libmpv-android-video-build v1.1.7 の `default` ビルド。mpv は `-Dgpl=false`） | LGPL-2.1-or-later | https://github.com/media-kit/libmpv-android-video-build/releases/tag/v1.1.7 |
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

## Windows / macOS

Windows と macOS は未対応で、配布物はありません。対応する際は、同梱する
`media_kit_libs_*` のライセンスを確認してから、この通知を更新してください。
