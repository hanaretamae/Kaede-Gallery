# Third-party notices

Kaede Gallery application code is licensed under the [MIT License](LICENSE).
The application also uses third-party libraries and, for desktop
distributions, media tools under their own licenses. The in-app **Open-source
licenses** viewer and distributions include the project license, this notice,
and license texts assembled from `kotlin/shared-assets/licenses/`.

## Components

| Component or distribution | Third-party components | License information |
| --- | --- | --- |
| Android application | Kotlin and Compose, AndroidX, Media3, MaterialKolor, Skydoves Compose ColorPicker, and Rust UniFFI dependencies | Kotlin/Compose, AndroidX, Media3, Material Color Utilities, and Skydoves Compose ColorPicker use Apache-2.0. MaterialKolor is MIT-licensed. Rust runtime dependencies are listed in `RUST-DEPENDENCY-LICENSES.txt`. Android video playback uses Media3. |
| Compose Desktop application | Compose, MaterialKolor, Skydoves Compose ColorPicker, JNA, and Rust UniFFI dependencies | MaterialKolor uses MIT. Material Color Utilities and Skydoves Compose ColorPicker use Apache-2.0. JNA is dual-licensed under Apache-2.0 or LGPL-2.1-or-later; the distribution includes both license texts. Rust runtime dependencies are listed in `RUST-DEPENDENCY-LICENSES.txt`. |
| Linux desktop packages | Desktop application plus the Nix runtime closure, including mpv, FFmpeg, libass, and libplacebo | The Nix inputs are locked in `flake.lock`. FFmpeg is built with GPL support; the package includes GPL-3.0-or-later terms and the applicable LGPL and other runtime license texts. |
| Windows desktop distributions | Desktop application and the bundled `mpv.exe` and `ffmpeg.exe` media tools | The media executables are GPL-licensed builds published by [mpv-winbuild](https://github.com/zhongfly/mpv-winbuild); the distribution includes GPL-3.0-or-later terms. See the upstream [mpv](https://github.com/mpv-player/mpv) and [FFmpeg](https://ffmpeg.org/) projects for source information. |

The optional Google Sans Flex typeface is bundled under the SIL Open Font
License 1.1. Its license is in
`kotlin/shared-assets/licenses/GoogleSansFlex-OFL.txt`.

The shared license assets also contain platform and runtime notices for
components such as ANGLE, SwiftShader, Vulkan Loader, and the Windows SDK
redistributables. The exact set included with a distribution depends on its
platform runtime and is assembled by the build.

## Rust dependency report

`RUST-DEPENDENCY-LICENSES.txt` is generated from the shipped `gallery-ffi`
Cargo dependency graph. It lists runtime packages, versions, selected SPDX
license texts, and dependency `NOTICE` or copyright files for the target
triples configured in `about.toml`. Build-only and development-only
dependencies are excluded. Accepted license expressions are maintained in
`about.toml` and `deny.toml`.

After changing Rust dependencies, regenerate the report:

```sh
python3 tools/update-rust-license-notices.py
```

## License files

The shared assets under `kotlin/shared-assets/licenses/` include the license
texts and notices used by the application and its distributions, including
Apache-2.0, MIT, GPL-3.0, LGPL-2.1, LGPL-3.0, ISC, zlib, and the generated Rust
dependency report. The build assembles these into the offline license viewer
and distribution resources.
