<p align="center">
  <img alt="Kaede Gallery icon" src="app/assets/branding/kaede-gallery-icon.png" width="128" height="128">
</p>

<h1 align="center">Kaede Gallery</h1>

<p align="center">
  English | <a href="README.ja.md">日本語</a>
</p>

<p align="center">
  A gallery for browsing tagged Obsidian Vault notes — <b>offline and read-only</b>.<br>
  Built with Rust (parser, index, CLI) and Flutter (Linux / Android / Windows UI).
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Linux%20%7C%20Android%20%7C%20Windows-informational">
  <img alt="Rust" src="https://img.shields.io/badge/core-Rust-orange">
  <img alt="Flutter" src="https://img.shields.io/badge/UI-Flutter-02569B">
  <img alt="Offline" src="https://img.shields.io/badge/network-none-success">
</p>

> [!IMPORTANT]
> Kaede Gallery **never writes to your Vault**. The index, thumbnails and settings are stored in
> private app data outside the Vault. There is no network access, telemetry or analytics.

## Contents

- [Features](#features)
- [Using the app](#using-the-app)
- [Privacy and security](#privacy-and-security)
- [Install and run](#install-and-run)
- [Developer information](#developer-information)

## Features

| Area | Details |
| --- | --- |
| Browsing | Switch between per-note and all-media views. Tiles can show a running number and counts of images, memos and related links |
| Paging | Configurable page size, "N–M of Total" indicator, jump to any position, and automatic loading of neighbouring pages while scrolling |
| Search | Note name, `#tag`, `-#tag`, `&#tag` and fuzzy tag-name search |
| Filtering | Hierarchical tags with include / exclude / AND / none. OR within a category, AND across categories. Sort by created or published date (default: created, newest first) |
| Viewer | Full-size media on a black background. Tap to show the author, post text, memos and related links. Swipe, arrow keys or trackpad to switch media |
| Related links | Markdown links and wikilinks (including `![[…]]` and `![](…)`) that point to a note open in the in-app detail view |
| Video | Playback with mpv: speed, single-item loop, mute. Thumbnails use FFmpeg |
| Appearance | Material 3 Expressive-inspired colours, shapes and motion; Material You dynamic colour; system / light / dark / pure black |
| Language | English and Japanese. English is the default unless the system language is Japanese; switch it in Settings → Appearance |
| Note structure | Configurable block order (author, media, post text, post-text end, related, memo), heading names/levels/bullets and frontmatter keys |

> [!NOTE]
> Flutter does not ship every Material 3 Expressive component, so the UI uses standard Material 3 parts.
> Android builds a sideloadable APK and reads a regular folder chosen via SAF, read-only. Verification on
> real devices with other Documents providers is still needed. Windows is built by GitHub Actions and has
> not been verified on real hardware yet.

## Using the app

1. Start the app and choose your Vault folder (read-only).
2. Notes whose tags include a gallery tag (default `source/art`) appear in the grid.
3. Use search and the tag filter panel to narrow down; tap a tile to open the viewer.
4. Tap the viewer to see details (author, post text, memos, related links).

### Example note

This is the kind of note that shows up in the gallery. It is a fictional example; heading names and the
gallery tags can be changed in Settings.

````markdown
---
created: 2026-01-02
tags:
  - source/art
  - source/type/illustration
  - source/count/1
cover: media/sample.png
---
# Summer scenery

![](<media/sample.png>)

# Document
The post text goes here.

## Related
- [Another note](other-note.md)
- [[another-note]]

## Memo
- A memo shown in the viewer details
````

- A YAML frontmatter with non-empty `tags` is required. Notes containing a gallery tag (default
  `source/art`) are shown.
- Embed images and videos with `![](…)` or `![[…]]`.
- Heading names such as "Related" and "Memo" are configurable. The defaults recognise both English
  (`Related`, `Memo`, `Notes`, `Document`) and Japanese (`関連`, `覚書`, `メモ`, `文書`) headings.

### Settings

1. **Appearance** — language (system / 日本語 / English), theme (system / light / dark), pure black, Material You
2. **Paging and list** — page size, count display, tile numbers
3. **Vault** — choose the Vault, rescan
4. **Notes** — note structure and display (block order, headings, frontmatter) and tag settings
   (gallery tags, filter categories, hidden tags, tag colours). Each page has "Reset to defaults"
5. **About** — information and licences, import / export / reset settings, help

Filter categories (count, art style, gender, meta, rating, source, type, work, other) can be edited by exact
path (`source/art`) or whole subtree (`source/count/*`). Per category you can choose whether tags at the third
level and deeper are split by parent tag (off by default).

The note-structure screen shows a fictional sample note and its Markdown reflecting your settings (nothing is
written to the Vault). Changing the structure triggers a rescan. Tag settings only affect display; the Vault and
the index contents are unchanged.

### Notes and the parser

The parser accepts:

- UTF-8 with or without BOM, LF or CRLF
- YAML frontmatter with non-empty `tags`
- Markdown headings, media embeds and links (Markdown and wikilinks)
- "Related" and "Memo" sections (heading names are configurable)

The default hidden tags are `moc`, `add`, `pin` and `source/art`. They only affect category display; notes with
those tags are still searchable and listed.

| Input limit | Value |
| --- | --- |
| Note | 2 MiB |
| Frontmatter | 256 KiB |
| Tags | 256 |
| YAML nesting | 64 levels |

> [!WARNING]
> YAML aliases are rejected before deserialisation (expansion-attack protection). If a real Vault needs aliases,
> replace this with an event-based parser and explicit expansion limits first.

Videos, external pages and Obsidian URIs open only when you press the corresponding button in the viewer.

## Privacy and security

- Vault access is always read-only. Paths are validated before opening, and the index and caches are created only
  outside the Vault.
- Core crates use `forbid(unsafe_code)`. Dependencies are pinned in `Cargo.lock` and network crates are banned in
  `deny.toml`.
- Logs and errors never contain note content.
- SQLite uses the bundled C library of `rusqlite`. This is an explicit exception to the pure-Rust preference, to
  avoid differences between system SQLite builds. It is not a network client.
- The index is a cache: deleting it is safe, it is rebuilt from the Vault.

## Install and run

Prebuilt binaries are attached to GitHub Releases (Android APK, Linux bundle, Windows ZIP) together with
`SHA256SUMS`. Licences of bundled components (mpv, FFmpeg, …) are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); the Linux bundle is distributed as GPL-3.0-or-later because of
its FFmpeg build, while the Android APK only contains LGPL libraries.

### Rust CLI quick start

Enter the dev shell with `nix develop`, or install Rust stable and a C compiler.

```sh
nix develop
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/dummy-vault --notes 100
cargo run --release -p gallery-cli -- scan /tmp/dummy-vault
cargo run --release -p gallery-cli -- categories /tmp/dummy-vault
cargo run --release -p gallery-cli -- list /tmp/dummy-vault source/rating/safe
cargo test --workspace
cargo deny check advisories bans licenses sources
```

<details>
<summary>Index database location</summary>

By default `$XDG_STATE_HOME/vault-gallery/index.sqlite` (or `$HOME/.local/state/vault-gallery/index.sqlite`),
outside the Vault. `--database <path>` changes it, but it must still be outside the Vault. On Unix the state
directory and index file are restricted to the current user. CLI error messages never contain note content,
paths, tags or URLs.

</details>

Scanning ignores hidden paths and Syncthing control files. The index is disposable: notes are re-parsed when the
modification time or size changes, and deleted notes disappear on the next scan. Notes with missing media stay
with a "missing" flag and are re-checked on later scans.

### NixOS / Home Manager

The flake provides `packages.x86_64-linux.default` and `packages.aarch64-linux.default`.
On NixOS import `nixosModules.default` or add the package to `environment.systemPackages`.

```nix
{
  inputs.kaede-gallery.url = "github:hanaretamae/Kaede-Gallery";

  outputs = { self, nixpkgs, kaede-gallery, ... }: {
    nixosConfigurations.my-host = nixpkgs.lib.nixosSystem {
      # ...
      modules = [
        kaede-gallery.nixosModules.default
        ./configuration.nix
      ];
    };
  };
}
```

With Home Manager, import `homeManagerModules.default`, or pass `inputs` to your Home Manager module and use:

```nix
home.packages = [
  inputs.kaede-gallery.packages.${pkgs.stdenv.hostPlatform.system}.default
];
```

The package includes `share/applications/kaede-gallery.desktop`, so it shows up in your launcher after
re-login. To try it directly: `nix profile install github:hanaretamae/Kaede-Gallery`. No binary cache for Kaede
Gallery itself is provided, so the first install builds on your machine (dependencies come from the Nix binary
cache when available).

### Run the Flutter app from source

```sh
nix develop
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

<details>
<summary>Linux notes</summary>

- The Nix shell contains Flutter, Linux desktop build dependencies, Rust, Rustup and FFmpeg (video thumbnails).
- The Rust toolchain is pinned by `rust-toolchain.toml` and fetched by Rustup on first build.
- Video playback uses mpv / libass.
- The Linux runner loads the Rust bridge from a shared library next to the executable; no `LD_LIBRARY_PATH` needed.
- The folder picker is the native GTK dialog and follows the desktop GTK theme.
- The title bar is left to the compositor; no custom GTK header bar is added.
- Re-enter `nix develop` after changing `flake.nix`.
- Image thumbnails use pure-Rust decoders (PNG, JPEG, GIF, WebP); video thumbnails use a Flutter native plugin.
  AVIF shows a placeholder.

</details>

### Build an Android APK

The Nix dev shell includes the Android SDK (API 35/36), Build Tools 36, NDK 28.2, CMake 3.22.1 and JDK 17. To
build arm64 and x86_64 APKs on Linux x86_64, add the Rust Android targets once, then point at the NDK compilers.

```sh
nix develop
rustup target add aarch64-linux-android x86_64-linux-android
cd app
NDK_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
PATH="$(dirname "$(rustup which rustc)"):$PATH" \
  CC_aarch64_linux_android="$NDK_BIN/aarch64-linux-android35-clang" \
  CXX_aarch64_linux_android="$NDK_BIN/aarch64-linux-android35-clang++" \
  AR_aarch64_linux_android="$NDK_BIN/llvm-ar" \
  RANLIB_aarch64_linux_android="$NDK_BIN/llvm-ranlib" \
  CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK_BIN/aarch64-linux-android35-clang" \
  CC_x86_64_linux_android="$NDK_BIN/x86_64-linux-android35-clang" \
  CXX_x86_64_linux_android="$NDK_BIN/x86_64-linux-android35-clang++" \
  AR_x86_64_linux_android="$NDK_BIN/llvm-ar" \
  RANLIB_x86_64_linux_android="$NDK_BIN/llvm-ranlib" \
  CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$NDK_BIN/x86_64-linux-android35-clang" \
  flutter build apk --release --target-platform android-arm64,android-x64
```

The output is `app/build/app/outputs/flutter-apk/app-release.apk`. Without a signing key the build is signed
with the Android debug keystore: fine for local testing, but not for distribution or for updating apps signed
with another key.

On Android, choose a regular folder inside Documents via SAF; the read permission is persisted. Note text is
passed to Rust with a size limit and indexed; images and videos are displayed and played from URIs inside the
chosen folder. The Vault is never copied into app storage. No Syncthing-specific integration is needed.
Selection via SAF, permission persistence across restarts, note indexing, image display and short MP4 playback
were verified on an Android 16 device with a fictional Documents folder; other devices, Documents providers and
media formats still need checking.

### Publish a release

Releases are signed and built locally and attached with the GitHub CLI; no GitHub-hosted runners or Actions
artifacts are used for Android/Linux. Create a dedicated signing key once and back it up safely — losing the key
or its password means existing installs cannot be updated. Create a KeePassXC entry
"Kaede Gallery Android signing" with username `kaede-gallery`, the signing password in the password field and
`release.jks` as an attachment. Protect the database with a strong master password and keep encrypted backups.
Never put the key or password in Git or in a Release.

```sh
nix develop
mkdir -p "$HOME/.local/share/kaede-gallery"
chmod 700 "$HOME/.local/share/kaede-gallery"
keytool -genkeypair -v \
  -keystore "$HOME/.local/share/kaede-gallery/release.jks" \
  -keyalg RSA -keysize 3072 -validity 10000 -alias kaede-gallery
chmod 600 "$HOME/.local/share/kaede-gallery/release.jks"
gh auth login
```

1. Add a release section to `CHANGELOG.md`, bump `version` in `app/pubspec.yaml` (and `flake.nix`), push to `main`.
2. Create and push a tag matching the pubspec version (`vMAJOR.MINOR.PATCH`; replace `vX.Y.Z`):

   ```sh
   git tag -a vX.Y.Z -m vX.Y.Z
   git push origin vX.Y.Z
   ```

3. Build, verify and attach the signed artifacts. `KEEPASSXC_DATABASE` is the absolute path of the `.kdbx`
   database and `KEEPASSXC_KEY_FILE` the key file; never write these paths into the repository.

   ```sh
   KEEPASSXC_DATABASE="/path/to/keepass.kdbx" \
   KEEPASSXC_KEY_FILE="/path/to/key-file.keyx" \
   nix develop --command ./tools/release.sh vX.Y.Z
   ```

The script reads the key and password from the KeePassXC entry (override with `KEEPASSXC_ENTRY` and
`KEEPASSXC_ATTACHMENT`), extracts the key to a mode-700 `XDG_RUNTIME_DIR` temporarily, and removes it after the
build. It asks for the master password twice, then runs the Rust/Flutter tests, builds and verifies signed APKs
(arm64, x86_64, universal) and Linux executables (x86_64, aarch64), and creates the Release. It checks the
tag/version, a clean work tree, pushed `main`/tag and GitHub authentication. Changes after the tag are allowed
only for Markdown files other than `CHANGELOG.md` and the release script. Building aarch64 Linux needs binfmt
(QEMU) or a remote builder; skip it with `SKIP_LINUX_ARCHES=aarch64-linux`. macOS is not supported.

### Windows build

Windows can only be built on Windows, so it uses a manual (`workflow_dispatch`) GitHub Actions workflow
(`.github/workflows/windows.yml`; standard runners only, free for public repositories). After creating the
Release, run "Windows build" from the Actions tab and enter the tag (for example `v1.6.1`). It adds the x64 ZIP to
the Release and updates `SHA256SUMS`. Unzip and run `vault_gallery.exe`. Windows on Arm can run this x64 build
under emulation. A native ARM64 build is unavailable because the bundled Windows video dependencies (libmpv and
ANGLE) are x64-only.

## Developer information

### Performance and test data

Synthetic data of realistic scale can be generated instead of using real clips. Generated data is never committed.

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
tools/benchmark.sh 7806
```

`tools/benchmark.sh` measures the release build and scan times (initial and rescan). `testdata/dummy-vault` in
the repository is fictional data.

### Repository layout

```text
crates/
  gallery-parse/    note parser
  gallery-core/     index, search, queries, thumbnails
  gallery-bridge/   flutter_rust_bridge public API
  gallery-cli/      CLI and dummy Vault generator
app/lib/
  l10n.dart         UI language (tr(ja, en)) and locale resolution
  core_api/         repository layer, providers, settings models
  features/gallery/ settings, tag filter, grid, viewer (split into parts)
docs/design.md      source of truth for the design
```

This repository exists for distribution only and does not accept external issues or pull requests. See
[CONTRIBUTING.md](CONTRIBUTING.md) for developer procedures. Licensed under [LICENSE](LICENSE) (MIT). See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for bundled components such as mpv and FFmpeg.
