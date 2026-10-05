# Third-party notices

Kaede Gallery itself is licensed under the [MIT License](LICENSE). The distributed applications bundle the
native libraries listed below. Dart and Flutter package licenses are displayed in the in-app
**Open-source licenses** screen. Rust dependencies are limited to MIT, Apache-2.0, Unicode-3.0, and Zlib and
are checked with `cargo deny check licenses`. See [the Japanese version](THIRD_PARTY_NOTICES.ja.md).

## Video playback and thumbnail libraries

| Target | Libraries | License | Source |
| --- | --- | --- | --- |
| Android APK | libmpv and FFmpeg (`media-kit/libmpv-android-video-build` v1.1.7 `default` build; mpv uses `-Dgpl=false`) | LGPL-2.1-or-later | https://github.com/media-kit/libmpv-android-video-build/releases/tag/v1.1.7 |
| Windows ZIP | libmpv and FFmpeg (`media-kit/libmpv-win32-video-build`, 2023-09-24; see below) | LGPL-3.0-or-later | https://github.com/media-kit/libmpv-win32-video-build |
| Linux bundle | mpv 0.41.0, FFmpeg 9.0.1, libass, and libplacebo (pinned by `flake.lock`) | FFmpeg is built with `--enable-gpl --enable-version3`; GPL-3.0-or-later | https://github.com/NixOS/nixpkgs (revision pinned in `flake.lock`) |

- Android includes these libraries as shared objects (`.so`), replaceable with compatible builds for the same ABI.
- **The Linux bundles (`kaede-gallery-*-linux-*`) include GPL-3.0-or-later libraries and are distributed under
  GPL-3.0-or-later terms** (full text: https://www.gnu.org/licenses/gpl-3.0.txt). The MIT-licensed application
  code is compatible with GPL-3.0. The complete application source is available in this repository at the
  corresponding release tag; sources for bundled libraries are available from the pinned nixpkgs revision.
  The GPL does not extend to the Android APK or to the MIT-licensed source code itself.

## Dart and Flutter packages

The dependency audit found no GPL/AGPL-family packages. Most use BSD-3-Clause, MIT, or Apache-2.0. The
exception is `dbus` under MPL-2.0 (file-level copyleft); this project uses it unmodified. Full license texts
are available in the in-app **Open-source licenses** screen.

Refer to each project for copyright notices and full license texts:

- mpv: https://github.com/mpv-player/mpv (GPL-2.0-or-later / LGPL-2.1-or-later)
- FFmpeg: https://ffmpeg.org/legal.html
- libass: https://github.com/libass/libass (ISC)
- libplacebo: https://code.videolan.org/videolan/libplacebo (LGPL-2.1-or-later)

## Windows

The Windows ZIP includes libmpv obtained by `media_kit_libs_windows_video` (the `video` build from
`media-kit/libmpv-win32-video-build`, 2023-09-24) and ANGLE (an OpenGL ES implementation, BSD-3-Clause).
libmpv is built with `--disable-gpl --enable-version3`; the FFmpeg components use LGPL-3.0-or-later, as
verified from the bundled DLLs. Sources are available from https://github.com/media-kit/libmpv-win32-video-build
and https://github.com/mpv-player/mpv. libmpv is included as the replaceable dynamic library `libmpv-2.dll`.
It is x86_64 only; the Windows ARM64 build is experimental.

## macOS

macOS is unsupported and no macOS artifacts are distributed.
