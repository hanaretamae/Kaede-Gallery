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

| Area           | Details                                                                                                                                                                |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Browsing       | Switch between per-note and all-media views. Tiles can show a running number and counts of images, memos and related links                                             |
| Paging         | Configurable page size, "N–M of Total" indicator, jump to any position, and automatic loading of neighbouring pages while scrolling                                    |
| Search         | Note name, `#tag`, `-#tag`, `&#tag` and fuzzy tag-name search                                                                                                          |
| Filtering      | Hierarchical tags with include / exclude / AND / none. OR within a category, AND across categories. Sort by created or published date (default: created, newest first) |
| Viewer         | Full-size media on a black background. Tap to show the author, post text, memos and related links. Swipe, arrow keys or trackpad to switch media                       |
| Related links  | Markdown links and wikilinks (including `![[…]]` and `![](…)`) that point to a note open in the in-app detail view                                                     |
| Video          | Playback with mpv: speed, single-item loop, mute. Thumbnails use FFmpeg                                                                                                |
| Appearance     | Material 3 Expressive-inspired colours, shapes and motion; Material You dynamic colour; system / light / dark / pure black                                             |
| Language       | English and Japanese. English is the default unless the system language is Japanese; switch it in Settings → Appearance                                                |
| Note structure | Configurable block order (author, media, post text, post-text end, related, memo), heading names/levels/bullets and frontmatter keys                                   |

> [!NOTE]
> The active UI is Kotlin Multiplatform / Compose. Android and Linux Desktop build paths are available.
> Windows x64 tests, packaging, and a manual release workflow are configured; physical Windows runtime behavior has not
> been verified. Signed Android release publication uses the local release script and must be verified with release credentials.

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

The active build and release paths use Kotlin Multiplatform / Compose and Rust. Linux Desktop packaging, Android debug APK assembly, and Windows x64 CI packaging are available. The manual Windows workflow can attach a Windows ZIP to an existing GitHub Release. `tools/release.sh` builds and publishes signed Android and Linux artifacts after local checks and secure signing-key retrieval; that privileged release procedure is not run as part of CI. Licences of bundled components are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

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

On x86_64, cross-building for aarch64 requires QEMU/binfmt and Nix `extra-platforms`; a native or remote AArch64 builder can use the same command without emulation:

```sh
nix config show | grep '^extra-platforms'
nix build --system aarch64-linux --no-link .#packages.aarch64-linux.kmpDesktop
```

The owner verified this AArch64 package build on 2026-10-09; the builder mechanism was not recorded. The package bundles the Compose Desktop distributable, Linux launcher, and license notices. `packages.<system>.default`, `nixosModules.default`, and `homeManagerModules.default` all select KMP. A self-contained AArch64 Linux bundle is also available through the optional manual Actions workflow described below.

### Run and package the KMP app

For Linux Desktop development and a local distributable, use the `kotlin` Gradle project:

```sh
nix develop
cd kotlin
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable
```

The Nix shell provides JDK 17, the Android SDK (API 35–37), Build Tools 37, NDK 28.2 and CMake 3.22.1. Android
builds normally run locally: install/check the Rust Android targets, configure the NDK compiler variables, then run
`:androidApp:assembleDebug` from `kotlin/`. The APK is written under
`kotlin/androidApp/build/outputs/apk/debug/` and uses the debug application ID `com.hanaretamae.kaede.kmpdebug`.
See [CONTRIBUTING.md](CONTRIBUTING.md) for the exact setup commands. This is a debug build, not the signed production
release.

The production KMP Android application ID is `com.hanaretamae.kaede`, while the previous Flutter Android ID is
`com.hanaretamae.vault_gallery`. Android treats these as separate apps: installing KMP does not update or inherit
Flutter's private settings, cached index, or persisted SAF permission. Select the Vault again in KMP and, if needed,
export settings from the old app and import the compatible JSON through the settings screen. The index/cache is
recreated outside the Vault; the Vault itself is never copied or modified.

Local Android builds remain the normal path. For an optional hosted build, manually run
`.github/workflows/optional-arm-builds.yml` from the Actions tab and choose `android-debug`, `linux-aarch64`, or `both`.
After that workflow is on GitHub, you can also dispatch it from `nix develop` using the authenticated GitHub CLI:

```sh
tools/run-optional-builds.sh android-debug
# Choose linux-aarch64 or both instead; an optional second argument selects the ref.
```

It is not triggered by pushes or pull requests, uploads build artifacts only, and never signs a production APK or
publishes a release. `.github/workflows/kotlin-windows.yml` continues Windows x64 tests and packaging on push and pull
request. The manual `.github/workflows/windows.yml` workflow builds a versioned Windows ZIP and attaches it to an
existing release when a tag is supplied. `tools/release.sh` remains the Linux-hosted release entry point for the
signed Android APK and Linux bundles; it requires the Rustup 1.98.1 Android targets and retrieves the signing key from
KeePassXC. Physical Windows runtime behavior and a real-key Android release have not been verified here.

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
  gallery-ffi/      UniFFI API used by Kotlin
  gallery-cli/      CLI and dummy Vault generator
kotlin/
  core/             shared models, repositories, settings and Rust adapter
  ui/app/           shared Compose UI
  androidApp/       Android SAF and media integrations
  desktopApp/       Linux / Windows JVM application
docs/design.md      source of truth for the design
```

This repository exists for distribution only and does not accept external issues or pull requests. See
[CONTRIBUTING.md](CONTRIBUTING.md) for developer procedures. Licensed under [LICENSE](LICENSE) (MIT). See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for bundled components such as mpv and FFmpeg.
