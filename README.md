<p align="center">
  <img alt="Kaede Gallery icon" src="kotlin/shared-assets/branding/kaede-gallery-icon.png" width="128" height="128">
</p>

<h1 align="center">Kaede Gallery</h1>

<p align="center">
  English | <a href="README.ja.md">日本語</a>
</p>

<p align="center">
  An offline, read-only gallery for tagged Obsidian Vault notes.<br>
  The application uses Kotlin Multiplatform / Compose and a Rust parser, index, and FFI.
</p>

<p align="center">
  <img alt="App platforms" src="https://img.shields.io/badge/platform-Android%20%7C%20Linux%20%7C%20Windows%20x64-informational">
  <img alt="Windows build and package checks" src="https://img.shields.io/badge/Windows-JVM%20%2F%20package%20checks-informational">
  <img alt="Rust core" src="https://img.shields.io/badge/core-Rust-orange">
  <img alt="Kotlin Multiplatform and Compose UI" src="https://img.shields.io/badge/UI-Kotlin%20Multiplatform%20%2F%20Compose-7F52FF">
  <img alt="Offline by default" src="https://img.shields.io/badge/network-offline%20by%20default-success">
</p>

> [!IMPORTANT]
> Kaede Gallery never writes to your Vault. Indexes, thumbnails, and settings
> are kept in private application data outside it.
>
> The application makes no automatic network requests and sends no telemetry
> or analytics. An external URL is opened only after you choose to open it.

## Features

- Browse notes or all of their media, with configurable paging and gallery layout.
- Search note names and paths, including fuzzy matches and tag expressions:
  `#tag` (OR), `+#tag` (AND), or `-#tag` (exclude).
- Filter with hierarchical tags and sort by creation or publication date.
- View images and videos, with note details, memos, and related links.
- Follow Markdown links and Obsidian wikilinks to notes in the selected Vault.
- Configure the language, appearance, note headings, frontmatter keys, and tag rules.

The shared interface is available in English and Japanese. Video playback and
thumbnail extraction use the platform integrations provided by the application.

## Use the app

1. Choose a Vault folder. The app requests read-only access.
2. Add a gallery tag to notes you want to browse. The default gallery tag is
   `source/art`.
3. Search or filter the gallery, then open a tile to view its media and details.

For example, a fictional note may look like this:

```markdown
---
created: 2026-01-02
tags:
  - source/art
  - source/type/illustration
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

Notes need YAML frontmatter with a non-empty `tags` list. Images and videos can
be embedded with `![](…)` or `![[…]]`. Heading names such as `Related` and
`Memo` can be configured; Japanese headings are supported as well.

The parser accepts UTF-8 with or without a BOM and LF or CRLF line endings.
Input is bounded: notes up to 2 MiB, frontmatter up to 256 KiB, up to 256 tags,
and YAML nesting up to 64 levels. YAML aliases are rejected before
deserialization to prevent expansion attacks.

## Privacy and safety

- The Vault is always read-only. Paths are validated before files are opened.
- Indexes and caches are disposable and stored outside the Vault in private app
  data.
- Core and parser code run offline, use bounded inputs, and do not put note
  content in logs or errors.
- The Rust core forbids unsafe code. Network dependencies are disallowed.
- SQLite uses the bundled library through `rusqlite` to avoid differences
  between system SQLite builds.

## Downloads and source builds

Visit [GitHub Releases](https://github.com/hanaretamae/Kaede-Gallery/releases)
for published downloads. Build and verification instructions are in
[CONTRIBUTING.md](CONTRIBUTING.md); release procedures are in
[the release guide](docs/releasing.md).

To exercise the Rust CLI with fictional data in the pinned development
environment:

```sh
nix develop
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/kaede-gallery-demo --notes 100
cargo run --release -p gallery-cli -- scan /tmp/kaede-gallery-demo
```

The repository's `testdata/dummy-vault` also contains fictional notes and media.
Do not use a personal Vault as test data.

## Project information

- [Design and implementation contract](docs/design.md)
- [Developer setup and checks](CONTRIBUTING.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- [MIT License](LICENSE)
