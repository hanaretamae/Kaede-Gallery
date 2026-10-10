# Third-party notices

Kaede Gallery's application code is licensed under the [MIT License](LICENSE).
The active Android and Compose Desktop applications are built with Kotlin
Multiplatform / Compose and Rust. Their offline in-app **Open-source licenses**
viewer and distribution archives use the license texts in
`kotlin/shared-assets/licenses/`.
The in-app viewer and Android/Desktop app resources also include the project
MIT license, this notice, and a generated bundle of the platform/runtime license
texts. These bundled license resources are assembled during the Kotlin build
from the checked-in license files and notices.

The Rust dependency report is generated from the shipped `gallery-ffi` Cargo
dependency graph. It lists the package names and versions, selected SPDX license
texts, and dependency `NOTICE` or copyright files. The accepted license
expressions are maintained in `about.toml` and `deny.toml`. Regenerate the report
after changing Rust dependencies with:

```sh
python3 tools/update-rust-license-notices.py
```

The report covers the seven Rust target triples configured in `about.toml`; it
excludes build-only and development-only dependencies.

## Platform and media dependencies

| Target            | Runtime components                                                               | License and distribution notes                                                                                                                                                                                                                 |
| ----------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android APK       | AndroidX Media3, Kotlin/Compose, Skydoves Compose ColorPicker 1.3.0, Rust UniFFI bindings | AndroidX, Kotlin/Compose, and Skydoves Compose ColorPicker use Apache-2.0. Rust notices and license texts are included in the APK's offline license viewer and release archive.                                                                            |
| Windows x64 ZIP   | Compose Desktop, Skydoves Compose ColorPicker 1.3.0, JNA 5.19.1, Rust UniFFI bindings | Skydoves Compose ColorPicker uses Apache-2.0. JNA is dual-licensed under Apache-2.0 or LGPL-2.1-or-later. The ZIP includes both license choices. Windows runtime video playback and thumbnail extraction require `mpv` and `ffmpeg` on `PATH`; those executables are not bundled in the ZIP. |
| Linux Nix package | Compose Desktop, MaterialKolor 6.0.0-beta01, Skydoves Compose ColorPicker 1.3.0, JNA 5.19.1, nixpkgs `mpv`, `ffmpeg`, `libass`, and `libplacebo` | MaterialKolor and its upstream Material Color Utilities are used to generate the KDE/XDG accent-based M3 Expressive color scheme; both use MIT/Apache-2.0 terms. Skydoves Compose ColorPicker uses Apache-2.0. The package wrapper uses pinned nixpkgs runtime dependencies. FFmpeg is built with GPL support, so the Linux package is distributed under GPL-3.0-or-later; see `Apache-2.0.txt`, `GPL-3.0.txt`, `LGPL-2.1.txt`, and `ISC.txt`. |

The optional Google Sans Flex typeface is bundled with the shared UI under the
SIL Open Font License 1.1. Its license is included in the offline license
viewer and `kotlin/shared-assets/licenses/GoogleSansFlex-OFL.txt`.

The Compose Desktop Linux and Windows distributions bundle JNA 5.19.1. Its JAR
also contains the `META-INF/LICENSE` notice and license references. Source:
<https://github.com/java-native-access/jna>

## Bundled license files

The KMP applications package the shared license assets from
`kotlin/shared-assets/licenses/`. They include Apache-2.0 for Kotlin, Compose,
AndroidX, Media3, Skydoves Compose ColorPicker, and Material Color Utilities; MIT and Apache-2.0 for
MaterialKolor; Apache-2.0 and LGPL-2.1-or-later for JNA;
platform/runtime notices where applicable; and the generated Rust dependency
report, including the MPL-2.0 UniFFI entry. The Linux Nix package and Windows ZIP
also include the project license and this notice file.

## Rust dependencies

`RUST-DEPENDENCY-LICENSES.txt` lists Rust packages in the shipped application
dependency graph by license, package name, and version, followed by the license
texts and any included dependency `NOTICE` or copyright files. Only runtime
dependencies reachable from `gallery-ffi` are included.

## macOS

macOS is not supported and no macOS distribution is provided.
