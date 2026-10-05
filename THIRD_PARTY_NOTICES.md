# Third-party notices

Kaede Gallery itself is licensed under the [MIT License](LICENSE). Dart and
Flutter package licenses are shown in the in-app **Open-source licenses**
screen. Full license texts for bundled native libraries and Rust dependencies
are also shown there and shipped in `app/assets/licenses/`; Windows ZIPs and
Nix packages include the same files. The release includes a
`kaede-gallery-third-party-licenses.tar.gz` archive for APK recipients.

Rust dependency notices are generated from the `gallery-bridge` Cargo
dependency graph for Android, Windows x86_64/ARM64, and Linux x86_64/ARM64.
The report includes selected SPDX license texts, the package names and
versions using them, and any dependency `NOTICE` or copyright files. The
accepted license expressions are maintained in `about.toml` and `deny.toml`.
Regenerate the report after changing Rust dependencies with
`python3 tools/update-rust-license-notices.py`.

## Video playback and thumbnail libraries

| Target | Libraries | License | License text |
| --- | --- | --- | --- |
| Android APK | libmpv and FFmpeg (`media-kit/libmpv-android-video-build` v1.1.7 `default` build; mpv uses `-Dgpl=false`) | LGPL-2.1-or-later | `LGPL-2.1.txt` |
| Windows ZIP | libmpv and FFmpeg (`media-kit/libmpv-win32-video-build`, 2023-09-24) | LGPL-3.0-or-later | `LGPL-3.0.txt` |
| Linux bundle | mpv 0.41.0, FFmpeg 9.0.1, libass, and libplacebo (pinned by `flake.lock`) | FFmpeg uses `--enable-gpl --enable-version3`; the bundled application is distributed under GPL-3.0-or-later | `GPL-3.0.txt`, `LGPL-2.1.txt`, `ISC.txt` |

The Android shared libraries (`.so`) and Windows `libmpv-2.dll` are dynamically
linked and replaceable. Source/build projects are available at:

- Android: https://github.com/media-kit/libmpv-android-video-build (v1.1.7)
- Windows: https://github.com/media-kit/libmpv-win32-video-build
- Linux dependencies: the nixpkgs revision pinned in `flake.lock`, with
  package build definitions and source references
- mpv: https://github.com/mpv-player/mpv
- FFmpeg: https://ffmpeg.org/legal.html
- libass: https://github.com/libass/libass
- libplacebo: https://code.videolan.org/videolan/libplacebo

The Linux bundle's Nix package metadata identifies GPL-3.0-or-later, matching
the license terms of the bundled application.

## Windows graphics libraries

The Windows ZIP includes the prebuilt ANGLE v1.0.1 archive used by
`media_kit_libs_windows_video`. Its CMake build pins `ANGLE.7z` with MD5
`e866f13e8d552348058afaafe869b1ed`. The archive itself contains no license or
notice files; the upstream license texts and copyright notices are supplied
with this application. CMake copies these runtime libraries:

| Runtime file(s) | Component and license | License text / source |
| --- | --- | --- |
| `libEGL.dll`, `libGLESv2.dll` | ANGLE, BSD-3-Clause | `ANGLE-LICENSE.txt`; https://github.com/google/angle |
| `vk_swiftshader.dll` | SwiftShader, Apache-2.0 | `SwiftShader-LICENSE.txt`, `SwiftShader-AUTHORS.txt`; https://github.com/google/swiftshader |
| `vulkan-1.dll` | Vulkan Loader, Apache-2.0 | `Vulkan-Loader-LICENSE.txt`, `Vulkan-Loader-NOTICE.txt`; https://github.com/KhronosGroup/Vulkan-Loader |
| `zlib.dll` | zlib | `zlib-LICENSE.txt`; https://github.com/madler/zlib/tree/v1.2.13 |
| `d3dcompiler_47.dll` | Microsoft Direct3D Compiler redistributable; not an open-source ANGLE component | `MICROSOFT-WINDOWS-SDK-REDIST.txt`; https://learn.microsoft.com/en-us/legal/windows-sdk/redist |

The DLL's embedded Authenticode signature identifies Microsoft Corporation.
Microsoft lists `d3dcompiler_47.dll` as redistributable for classic Windows
applications; redistribution is subject to the applicable Windows SDK terms.
The vendor archive does not record the exact SDK version, so the notice links
the official redistributable list and preserves that provenance limitation.
The Windows build workflow validates the DLL's Authenticode signature and
Microsoft signer identity before packaging.
The Microsoft DLL is not relicensed under the open-source licenses above.

The archive also contains `libc++.dll`, but the current CMake bundled-library
list does not copy it into the Windows application bundle.

## Dart and Flutter packages

The Dart/Flutter dependency license texts are included by Flutter in its
generated license registry and displayed in the in-app **Open-source licenses**
screen. `dbus` is used unmodified under MPL-2.0. Its full text is shown with
the other Dart/Flutter package licenses.

## Rust dependencies

The generated `RUST-DEPENDENCY-LICENSES.txt` lists the Rust packages in the
shipped application dependency graph by license, package name, and version,
followed by the license texts and any included dependency `NOTICE` or
copyright files. The report covers the six shipped Rust target triples
configured in `about.toml`; build-only and development-only dependencies are
excluded.

## macOS

macOS is unsupported and no macOS artifacts are distributed.
