# Rust–Kotlin FFI contract

This document defines the target boundary for the Kotlin migration. It does
not replace or change the current Flutter Rust Bridge API; that API remains in
use until a Kotlin implementation has passed parity checks.

## Ownership and dependency direction

```text
Compose UI
  -> common Kotlin state holder / repository
    -> platform binding adapter
      -> Rust FFI facade
        -> gallery-core
          -> gallery-parse
```

Composable functions must not call generated bindings or perform blocking
filesystem, database, scan, or decode work. The repository exposes typed
Kotlin models and asynchronous operations; generated FFI models stay behind
the adapter. `gallery-core` and `gallery-parse` remain safe Rust libraries and
must not depend on Kotlin, UniFFI, or UI types.

## Binding technology and target qualification

The current KMP implementation uses UniFFI 0.32.0 through the third-party
Kotlin Multiplatform Gradle plugin; this is not yet acceptance of every target
or of the migration as complete. The plugin is not part of Mozilla's UniFFI
project ([plugin overview and target
matrix](https://ubiqueinnovation.github.io/uniffi-kotlin-multiplatform-bindings/),
[Mozilla UniFFI](https://github.com/mozilla/uniffi-rs)). The current plugin
documentation specifies Rust 1.91+, UniFFI 0.32.0, Gradle 9.6.1+, Kotlin
2.4.0+, and Android Gradle Plugin 9.x. The workspace's declared Rust minimum
is 1.85, while its pinned build toolchain is 1.98.1. The plugin's `mingwX64`
target uses GNU Windows ABI, whereas this repository's Flutter bridge targets
`x86_64-pc-windows-msvc`; this difference must be explicitly resolved rather
than assuming the generated library can replace the current Windows bridge.
The desktop Compose app uses the JVM host target, so Windows must instead
verify the plugin's Windows-host Rust/JVM integration and runtime packaging.
The added Windows CI workflow is the first automated check of that path; the
migration phase remains incomplete until it passes and target limitations are
reviewed. Keep the core minimum Rust version unchanged unless an explicit
compatibility review justifies a change; isolate any higher toolchain
requirement to the FFI crate. If target compatibility or supply-chain review
fails, compare a small owned C ABI facade rather than weakening core
boundaries.

The Kotlin adapter uses `kotlinx-coroutines-core` 1.10.2 to dispatch blocking
FFI calls to `Dispatchers.IO`; common tests use its matching test artifact.
These Apache-2.0 multiplatform libraries add no network access, permissions, or
native unsafe boundary.

The Compose Desktop Windows build uses the plugin's JVM host target, which
builds a Windows-host Rust library rather than the separate `mingwX64` static
cinterop target. The Windows CI workflow has successfully built and tested the
KMP host ABI and distributable on the migration branch. This verifies build
and packaging, not execution on physical Windows hardware. Keep the legacy
Flutter MSVC distribution untouched.

Linux Compose Desktop renders local video in an AWT canvas hosted by the
application window, with mpv's native video child window embedded into that
canvas. The launcher passes the validated local media path as a separate
process argument and disables mpv configuration, scripts, network helpers,
and automatic sidecar loading. Linux users need an `mpv` executable with X11 window embedding support on
`PATH`; video playback is not provided on Windows. Linux and Windows gallery
video tiles use `ffmpeg` on `PATH` to extract a single local frame. The
extractor validates the indexed media location, disables network input
protocols, caps the output at 512 pixels and 4 MiB, limits execution to
10 seconds, and runs at most two extractions concurrently. Android continues to
use its SAF-backed native thumbnail path.

## Coarse-grained repository operations

The Rust `gallery-ffi` facade currently exports session open/scan, filesystem
and SAF scans, paged queries, category options, note details (including
bounded SAF note content), validated media locations, bounded thumbnails,
and private selected-Vault storage operations. The Kotlin `GalleryRepository`
adapts paged queries, categories, note details, media locations, and
thumbnails. `RustVaultSelectionRepository` adapts private data preparation and
selected-Vault load/save/clear operations; `RustGallerySessionRepository`
opens filesystem Vault sessions, performs an initial scan for new indexes,
reuses an existing index without scanning at startup, supports explicit
rescans, and exposes disposal through a typed session handle. The filesystem
session API rejects `content://` locators. Android SAF enumeration and scan
submission remain platform-layer work; do not send the whole SAF collection
through `scanSaf` without the bounded batch/transaction protocol or measured
memory evidence described below.
For SAF-backed note details, Android resolves the note path from the bounded
gallery page, validates it beneath the selected tree, and reads no more than
the 2 MiB note limit before passing the bytes to the SAF detail operation.
Media summaries carry their parent note path for details opened from the media
grid; note bytes are never persisted or logged.
The common `core:settings` module defines appearance and gallery
pagination/display settings with a repository interface and state holder.
Android implements that contract with app-private preferences; desktop
persists the matching settings in a bounded properties file under private
application data. A versioned `.kgsettings` transfer format supports
validated import/export on both platforms, with imports capped at 16 KiB and
persisted through the settings repository. Desktop export rejects destinations
inside the selected Vault; Android requires choosing an export folder and
rejects the selected Vault and its descendants before creating a file.
Settings are not Rust FFI operations and must never be persisted in the Vault.

The Kotlin repository should present operations equivalent to:

- `preparePrivateData`, `loadSelectedVault`, `saveSelectedVault`, and
  `forgetSelectedVault`.
- `scanFilesystem` for desktop and a bounded, resumable SAF scan protocol for
  Android, returning only aggregate counts and sanitized status/errors.
- `queryGalleryPage(request)` returning a bounded page of note or media
  summaries, total count, and category options needed for that request.
- `getNotePath(noteId)` returning the indexed, Vault-relative note path for
  platform reads of linked notes that were not present in the current gallery
  page. The path is still validated by the platform adapter before opening.
- `getNoteDetail(noteId)` returning one bounded note detail, memo/related/
  post-text-end lines, and media metadata.
- `getMediaLocation(mediaId)` returning a validated filesystem path or a
  normalized SAF-relative path, never an unchecked caller-provided path.
- `getThumbnail(mediaId, size)` returning at most one bounded thumbnail per
  request, never original media bytes.

Query requests carry the search expression, include/AND/exclude/virtual
filters, sort field/direction, and page offset/size in one value. IDs remain
64-bit across the boundary; do not silently truncate them to 32-bit values.
Page size is bounded to the existing setting range of 1–500 (default 24);
thumbnail dimensions/encoded size also have explicit upper bounds.
No API enumerates records through repeated single-item FFI calls.

## SAF scan protocol

Android owns the document picker, persisted read-only grant, document
enumeration, and bounded document reads. The native adapter must validate the
selected tree and relative document paths before opening content. Rust
revalidates every submitted relative path and enforces the existing shared
limits: 100,000 entries, depth 64, 32 MiB aggregate relative-path bytes,
2 MiB per note, 128 notes / 16 MiB per read batch, four concurrent note
reads, and 128 MiB total note content per scan.

The current Flutter adapter gathers the bounded scan data before one
`scan_saf` FFI call. Kotlin uses the staged scan-session operations
`begin_saf_scan`, `append_saf_scan_batch`, `finish_saf_scan`, and
`cancel_saf_scan`. It writes bounded batches to private staging files, then
streams those files into the Core transaction at finish; it does not retain
the full note collection in memory. Rust revalidates paths and enforces the
same document, path, note, batch, and aggregate-byte limits. Cancellation or a
fatal enumeration/staging error cancels the stage and leaves the previous
committed index intact. Individual unreadable/oversized notes are represented
as missing content and reported through the scan warning count, matching the
existing optional-note-content contract. Finish and cancel explicitly remove
the staging file and surface a cleanup failure; finish removes the staged file
before committing the database transaction. The Kotlin adapter also enforces
the per-batch note-count and byte caps before crossing FFI.

## Data, errors, and privacy

- Never send original image or video bytes across FFI. Resolve media through a
  platform adapter and decode/downscale there. Rust-generated thumbnails may
  cross only as individual, size-bounded thumbnail results.
- Note details and SAF note bytes are input/content-bearing data. Keep their
  size bounded, avoid unnecessary copies, and never put them in errors, logs,
  telemetry, or debug representations.
- Use typed/sanitized error categories; do not return raw paths, SQL errors,
  note contents, or provider messages to logs or UI errors.
- Rust validates the private index/cache location is outside the Vault.
  Android SAF data and caches remain in private app data; the Vault is never
  written to.
- FFI exposes local operations only. No network client, background upload,
  telemetry, or implicit external-link launch is permitted.

## Migration checks

Before the first generated binding ships, add integration tests for private
directory preparation, query/page/count, category filters, note detail,
filesystem media-path validation, thumbnail size limits, SAF scan limits,
sanitized failures, and disposal/reopening of the index. Run them against
fictional fixtures only. Keep the Flutter bridge and its existing checks until
the Kotlin calls produce matching results and the replacement is verified on
each target.
