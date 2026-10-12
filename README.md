<p align="center">
  <img alt="Kaede Gallery icon" src="kotlin/shared-assets/branding/kaede-gallery-icon.png" width="128" height="128">
</p>

<h1 align="center">Kaede Gallery</h1>

<p align="center">
  English | <a href="README.ja.md">日本語</a>
</p>

<p align="center">
  A gallery for browsing tagged Obsidian Vault notes — <b>offline and read-only</b>.<br>
  Built with Rust (parser, index, CLI) and Kotlin Multiplatform / Compose (Linux / Android / Windows desktop UI).
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Linux%20%7C%20Android%20%7C%20Windows-informational">
  <img alt="Rust" src="https://img.shields.io/badge/core-Rust-orange">
  <img alt="Kotlin Multiplatform" src="https://img.shields.io/badge/UI-Kotlin%20Multiplatform-7F52FF">
  <img alt="Offline by default" src="https://img.shields.io/badge/network-offline%20by%20default-success">
</p>

> [!IMPORTANT]
> Kaede Gallery is **read-only** and never writes to your Vault. The index,
> thumbnails, and settings are stored in private app data outside the Vault.
>
> The app makes no automatic network requests and sends no telemetry or
> analytics. External URLs are handed to your browser only after you choose
> to open them.

## Contents

- [Features](#features)
- [Using the app](#using-the-app)
- [Privacy and security](#privacy-and-security)
- [Install and run](#install-and-run)
- [Developer information](#developer-information)

## Features

- **Browse:** Switch between per-note and all-media views. Tiles can show a
  running number and counts of images, memos, and related links.
- **Page through results:** Set the page size, jump to any position, and load
  neighbouring pages automatically while scrolling.
- **Search:** Find note names and paths with fuzzy matching. Search tags with
  `#tag` (OR), `+#tag` (AND), or `-#tag` (exclude); `&#tag` is also accepted
  as an AND alias.
- **Filter:** Combine hierarchical tag filters with OR within a category and
  AND across categories. Sort by creation or publication date.
- **View notes:** Open media full-screen, then tap to see the author, post text,
  memos, and related links. Swipe, use the arrow keys, or scroll horizontally
  to switch media.
- **Follow related links:** Open linked notes in the in-app detail view.
  Markdown links and wikilinks are supported.
- **Play video:** Control mpv playback speed, looping, and mute. FFmpeg
  generates thumbnails.
- **Choose how the app looks:** Use Material You colours and choose system,
  light, dark, or pure-black themes. Select the system font or Google Sans Flex.
- **Choose a language:** Use English or Japanese. The app defaults to Japanese
  when the system language is Japanese.
- **Customize note structure:** Change the order of author, media, post text,
  related links, and memos. Configure headings and frontmatter keys.

> [!NOTE]
> The app is built with Kotlin Multiplatform / Compose and Rust.
> Android and Linux Desktop are supported build targets.
> Windows x64 and ARM64 packages are available, but Windows runtime behavior
> has not been verified.
>
> [Download the latest release](https://github.com/hanaretamae/Kaede-Gallery/releases).
>
> See [the release guide](docs/releasing.md) for build and release details.

## Using the app

1. Start the app and choose your Vault folder (read-only).
2. Notes whose tags include a gallery tag (default `source/art`) appear in the grid.
3. Use search and the tag filter panel to narrow down; tap a tile to open the viewer.
4. Tap the viewer to see details (author, post text, memos, related links).

### Example note

This is the kind of note that shows up in the gallery. It is a fictional example; heading names and the
gallery tags can be changed in Settings.

```markdown
---
created: 2026-01-02
tags:
  - source/art
  - source/type/illustration
  - source/count/1
cover: media/sample.png
---

# Summer scenery

![](media/sample.png)

# Document

The post text goes here.

## Related

- [Another note](other-note.md)
- [[another-note]]

## Memo

- A memo shown in the viewer details
```

- A YAML frontmatter with non-empty `tags` is required. Notes containing a gallery tag (default
  `source/art`) are shown.
- Embed images and videos with `![](…)` or `![[…]]`.
- Heading names such as "Related" and "Memo" are configurable. The defaults recognise both English
  (`Related`, `Memo`, `Notes`, `Document`) and Japanese (`関連`, `覚書`, `メモ`, `文書`) headings.

### Settings

1. **Appearance** — change the theme, language, and font. Pure black changes
   only the background; other colours retain Material You. Paging, gallery-list
   controls, and the unavailable-media icon have a separate settings page.
2. **Vault** — choose the Vault, rescan
3. **Notes** — note structure and display (block order, headings, frontmatter) and tag settings
   (gallery tags, filter categories, hidden tags, tag colours). Each page has "Reset to defaults"
4. **About** — information and licences, import / export / reset settings, help

On Linux, settings import and export use the system file chooser portal; Android uses the system
Storage Access Framework.

Filter categories (count, art style, gender, meta, rating, source, type, work,
other) can be edited by exact path (`source/art`) or whole subtree
(`source/count/*`). For each category, you can choose whether tags at the third
level and deeper are split by parent tag. This is off by default.

Edit frontmatter aliases at the top of **Note structure**. Tag rules use
add/edit/remove lists.

A separate card at the bottom shows a fictional sample that reflects your
settings and can be copied. The Vault picker shows the default sample instead.
Neither sample is written to the Vault.

Changing the note structure triggers a rescan. Tag settings affect display
only; they do not change the Vault or index contents.

### Notes and the parser

The parser accepts:

- UTF-8 with or without BOM, LF or CRLF
- YAML frontmatter with non-empty `tags`
- Markdown headings, media embeds and links (Markdown and wikilinks)
- "Related" and "Memo" sections (heading names are configurable)

The default hidden tags are `moc`, `add`, `pin` and `source/art`. They only affect category display; notes with
those tags are still searchable and listed.

| Input limit  | Value     |
| ------------ | --------- |
| Note         | 2 MiB     |
| Frontmatter  | 256 KiB   |
| Tags         | 256       |
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

The active application uses Kotlin Multiplatform / Compose and Rust.
Linux Desktop packages and Android debug APKs can be built locally.
Windows x64 and ARM64 packages are built through GitHub Actions.

The manual Windows workflow attaches a ZIP to an existing GitHub Release.
`tools/release.sh` builds and publishes signed Android and Linux downloads after
local checks and secure signing-key retrieval. This privileged release process
does not run in CI.

Licences for bundled components are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

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

### KMP Linux Desktop package

The flake's default package and `.#kmpDesktop` are the Kotlin/Compose Desktop app for x86_64 and aarch64 Linux.
Build and run the native package, or install it from the GitHub flake:

```sh
nix develop
nix build .#kmpDesktop
nix run .#kmpDesktop
nix profile install github:hanaretamae/Kaede-Gallery#kmpDesktop
```

Cross-build the self-contained AArch64 package from an x86_64 Linux host without QEMU/binfmt:

```sh
nix build --system x86_64-linux --no-link .#packages.x86_64-linux.aarch64Cross
nix bundle --system x86_64-linux --out-link /tmp/kaede-gallery-aarch64 .#packages.x86_64-linux.aarch64Cross
```

The cross package compiles the Rust FFI for AArch64 and assembles the AArch64
Compose runtime, JRE, launcher, and Linux libraries on an x86_64 host. A native
`aarch64-linux` package is also available when building on an AArch64 host.
`packages.<system>.default`, `nixosModules.default`, and
`homeManagerModules.default` select the native KMP package. The optional
Actions workflow can also cross-build an AArch64 bundle.

The Nix package and Linux release bundles use pinned `mpv` and `ffmpeg`,
including their runtime libraries, so video playback and thumbnails do not
depend on host executables in `PATH`.

Google Sans Flex remains available as an app appearance option.

See the [latest release](https://github.com/hanaretamae/Kaede-Gallery/releases)
for downloads.

### Run and package the KMP app

For Linux Desktop development and a local distributable, use the `kotlin` Gradle project:

```sh
nix develop
cd kotlin
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable
```

The Nix shell provides JDK 17, Android SDK (API 35–37.1), Build Tools 37,
NDK 28.2, and CMake 3.22.1.

Build Android locally: install the Rust Android targets, configure the NDK
compiler variables, then run `:androidApp:assembleDebug` from `kotlin/`.
The APK is written to `kotlin/androidApp/build/outputs/apk/debug/` and uses
the debug application ID `com.hanaretamae.kaede.kmpdebug`.
See [CONTRIBUTING.md](CONTRIBUTING.md) for setup commands.
This is a debug build, not a signed production release.
Production Android builds require Android 7.0 (API 24) or later.

The KMP app installs separately from the retired Flutter app. Android does not
transfer the old app's settings, cached index, or Vault access permission.
Select your Vault again in KMP. If you still use the old app, export its
settings before removing it, then import the compatible JSON in KMP.

The index and caches are recreated outside the Vault. The Vault itself is never
copied or modified.

Local Android builds remain the normal path.
For an optional hosted build, manually run
`.github/workflows/optional-arm-builds.yml` from the Actions tab.
Choose `android-debug`, `linux-aarch64`, or `both`.
After the workflow is available on GitHub, you can also start it from
`nix develop` using the authenticated GitHub CLI:

```sh
tools/run-optional-builds.sh android-debug
# Choose linux-aarch64 or both instead; an optional second argument selects the ref.
```

It is not triggered by pushes or pull requests. It uploads build artifacts
only; it does not sign production APKs or publish releases.

`.github/workflows/kotlin-windows.yml` runs Windows x64 tests and packaging on
push and pull request. The manual `.github/workflows/windows.yml` workflow
builds a versioned Windows ZIP and attaches it to an existing release when a
tag is supplied.

`tools/release.sh` is the Linux release entry point for signed Android APKs and
Linux bundles. It requires the Rustup 1.98.1 Android targets and retrieves the
signing key from KeePassXC.

Physical Windows runtime behavior has not been verified.
See the [release guide](docs/releasing.md) for local build instructions,
optional Actions builds, and release limitations.

## Developer information

### Performance and test data

Synthetic data of realistic scale can be generated instead of using real clips. Generated data is never committed.

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
tools/benchmark.sh 7806
```

`tools/benchmark.sh` measures release-build and scan times (initial scan and
rescan). The repository's `testdata/dummy-vault` contains fictional data.

### Repository layout

```text
crates/
  gallery-parse/    note parser
  gallery-core/     index, search, queries, thumbnails
  gallery-ffi/      UniFFI API used by Kotlin
  gallery-cli/      CLI and dummy Vault generator
kotlin/
  core/             shared models, repositories, settings and Rust adapter
  ui/app/           shared Compose UI
  androidApp/       Android SAF and media integrations
  desktopApp/       Linux / Windows JVM application
docs/design.md      source of truth for the design
```

This repository is for distribution and does not accept external issues or
pull requests. Developer procedures are in [CONTRIBUTING.md](CONTRIBUTING.md).

Kaede Gallery is licensed under the [MIT License](LICENSE). Licences for
bundled components such as mpv and FFmpeg are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
