# vault-gallery design

This document records the implementation contract for the repository. The
user-provided Japanese design in the project brief is the source of truth; this
summary preserves the constraints needed to implement the current phase. If
implementation and brief conflict, report the discrepancy and follow the
brief.

## Purpose and scope

Provide an offline gallery for media and metadata in an Obsidian Vault,
initially targeting NixOS/Linux and Android. Rust owns parsing, scanning,
indexing, filtering, and image thumbnails. Flutter will own the UI and video
decoding/playback. The Vault is always read-only.

The application must not edit, create, delete, or move Vault files; render
Markdown; synchronize data; automatically access the network; send telemetry,
ads, or crash reports; or generate/tag images with AI. Only fictional test
notes and media may be committed.

## Architecture and phase order

```text
Flutter features/platform -> gallery-bridge -> gallery-core -> gallery-parse
```

Dependencies point downwards only. `gallery-parse` is pure parsing without
filesystem I/O. `gallery-core` does not know Flutter or the bridge. Flutter
features do not import each other; the UI calls Rust through a repository
interface and keeps OS-specific behavior in `platform/`.

1. Rust parser, core, CLI, fictional fixture generator, and initial benchmark.
2. Flutter/Linux gallery list.
3. Viewer, details, Obsidian links, and video.
4. Android.
5. Windows/macOS.
6. iOS after agreeing on external Vault access.

Complete and verify one phase before starting another.

## Note parsing and gallery selection

Notes use YAML frontmatter containing a `tags` list and optional `url`,
`published`, `created`, `updated`, and `cover`. UTF-8 BOM and LF/CRLF are
accepted. Dates may remain original strings when not understood. Unknown keys
are ignored. A note is eligible when at least one original tag starts with
`source/`; display exclusions never remove it from the index.

The actual sanitized note examples provided by the user use Markdown image
embeds, including `![](<relative/path.webp>)` and `.mp4`, and repeat the
frontmatter `cover` in the body. The user confirmed that Markdown links are
used and wikilinks are not needed for this Vault.
The media order is `cover` followed by Markdown body embeds in source order,
without duplicates. Support image and video extensions, URL-decoded paths,
and missing media. Media paths are relative to
the note and must resolve inside the Vault. Body sections named `関連` and
`覚書` are extracted by heading name, not heading level. The UI displays
extracted fields and tokens; it does not render Markdown.

Tag categories use the final tag component as the option and all earlier
components as its category. Parent paths are indexed for matching but are not
independent options when descendants exist. Initial excluded path prefixes
are `moc`, `add`, `pin`, and `source/art`; exclusions apply only to display.
Within a category selected tags are OR; between categories conditions are AND.
Initial category labels are configurable, and missing labels fall back to the
tag path. The supplied examples confirm `source/type` and `source/format` as
category paths; they do not establish `source/meta`. Category-wide options and
virtual-content filters are planned for the UI/API phase. The interpretation
of category-wide selection remains the design document's stated assumption:
“has any tag in this category.”

## Storage, safety, and failure handling

The SQLite index and later thumbnail/config caches are disposable application
data outside the Vault and outside shared media locations. The index is
recreated/reconciled on scanning; changed notes are reparsed and removed notes
are deleted. Missing media remains indexed and is checked again on later
scans. A bad note is a warning, not a reason to abort the whole scan.

Do not follow symlinks while walking. Canonicalize media paths and reject
anything outside the canonical Vault root. Reject an index path inside the
Vault and hard-linked database files. Unix index data is owner-only. Do not
include note paths, names, contents, tags, or URLs in logs or errors.

Parser limits: note size 2 MiB, frontmatter 256 KiB, 256 tags, and 64 levels of
YAML nesting. The chosen Serde-compatible YAML parser does not expose an alias
expansion budget; Phase 1 rejects YAML alias tokens rather than risking
expansion exhaustion. This conservative limitation must be documented if
real notes use aliases.

Core crates declare `forbid(unsafe_code)`. Network dependencies are disallowed.
SQLite is explicitly bundled through `rusqlite`/`libsqlite3-sys`, which brings
native C code; this is documented as a conscious dependency trade-off.

## Phase 1 interface

`gallery-cli` offers:

- `scan <vault> [--database <database>]`: index/reconcile eligible Markdown notes and print
  aggregate indexed/warning counts.
- `categories <vault> [--database <database>]`: display non-excluded category options and
  counts.
- `list <vault> [--database <database>] [tag ...]`: list notes matching tag conditions.

The default database is under `$XDG_STATE_HOME/vault-gallery` or
`$HOME/.local/state/vault-gallery`, never in the Vault. `tools/` generates
fictional notes/media and measures initial and unchanged scan time.

## Deferred decisions and confirmation points

Before implementing parser behavior against real user material, inspect
sanitized examples of image and video-cover notes, including frontmatter,
headings, and embeds. Confirm the actual paths for provisional `source/meta`,
`source/format`, and `source/type` labels, and whether category-wide selection
means “has any tag in this category.” Before Android, confirm personal
sideloading versus store distribution. Before Obsidian integration, confirm
the Vault name on each device and verify current URI behavior. Platform and
dependency details are verified against current official documentation when
adopted.

Performance targets are measured, not assumed: current Vault size 7,806 notes,
growth case at least 20,000, a few seconds for an unchanged scan, responsive
filtering, and smooth lazy grid scrolling.
