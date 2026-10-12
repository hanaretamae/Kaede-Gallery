# Phase 1 performance baseline

Historical baseline: the Android image/grid benchmark below used the retired
Flutter UI and does not measure the maintained KMP/Compose app. KMP Android
SAF scan and Compose grid-frame measurements are recorded later in this
document for a single API 36 device, provider, and fictional fixture; they do
not characterize other providers, devices, or large-media loading.

Measured on 2026-10-01 using the release CLI and fresh generated fictional
notes/indexes only. Timings are wall-clock values printed by Bash `time`;
fixture generation and Cargo compilation are outside each scan timing.

| Notes | Full scan | Unchanged scan | Indexed | Warnings |
|---:|---:|---:|---:|---:|
| 7,806 | 1.955 s | 0.180 s | 7,806 | 0 |
| 20,000 | 3.454 s | 0.239 s | 20,000 | 0 |

The generated fixture uses a small shared PNG and a five-pattern fictional tag
distribution. Measurements are from this Linux development environment and
are a baseline, not a hardware-independent guarantee. Actual Vault media I/O,
large or malformed notes, memory use, and filtered-query latency were not
measured here. Re-run `tools/benchmark.sh <note-count> <work-directory>` when
the scanner or index changes.

## Phase 4 readiness check

Re-measured on 2026-10-02 with the release CLI and generated fictional data.
Generation and compilation are excluded from scan timings. Query timings
include CLI startup and database opening; command output is discarded.

| Notes | Full scan | Unchanged scan | Categories | Broad tag query | Narrow tag query |
|---:|---:|---:|---:|---:|---:|
| 7,806 | 1.315 s | 0.069 s | 0.032 s | 0.193 s | 0.072 s |
| 20,000 | 3.164 s | 0.139 s | 0.045 s | 0.256 s | 0.238 s |

All scan runs indexed the expected number of notes with zero warnings. The
20,000-note unchanged scan is well below the “few seconds” target, and both
representative tag-filtered list queries completed in under half a second.
These query timings are single wall-clock measurements, not latency
distributions.

The Flutter widget suite (25 tests) and static analysis passed. The gallery
uses a lazy grid and fetches 24 items per page by default. No Flutter code
refactor was indicated by these checks. Smooth scrolling with a large real
Vault, real-media I/O, peak memory, and frame-time profiling were not measured;
those require an interactive target and representative media and remain useful
follow-up checks for Linux and Android. Re-run the expanded benchmark with
`bash tools/benchmark.sh <note-count> <work-directory>`; it measures full and
unchanged scans, category lookup, and broad/narrow tag-filtered list queries.

## Android fictional SAF benchmark

Measured on 2026-10-03 on an Android 16 / API 36 RMX6688 device using
`tools/benchmark-android.sh <device-id>`. The integration test generates 1,000
fictional notes in memory, scans them through the Rust SAF indexer, measures the
private thumbnail cache and Flutter image decoder, and scrolls a 2,000-item
fictional grid. Three runs were taken. Its method channel returns fixture
metadata and image bytes; the test does not select, read, or write a user Vault.
Only the temporary index and thumbnail cache are created under a unique
app-private directory and removed after each run.

| Notes | Rust SAF scan (median; range) | RSS before scan (median) | Peak RSS during scan (median) | Scan RSS increase (median) | Warm thumbnail lookups | Image decode (median) | Scroll frames (median) | p95 frame time (median; range) |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1,000 | 144 ms (132–151) | 497.0 MiB | 506.5 MiB | 9.6 MiB | 1/1 hits (50% of cold + warm lookups) | 12 ms (7–12) | 108 | 12.46 ms (11.41–17.41) |

The RSS values include the integration-test Flutter process; the scan increase
is measured against its pre-scan baseline. The synthetic platform channel does not measure Android
`ContentResolver`/DocumentsProvider I/O or device-specific real-media decoding.
Use this result as a Rust/Flutter/device baseline, not as a claim about large
Vault throughput. The benchmark reports only aggregate metrics, never paths or
note contents.

## Current Rust core recheck

Re-measured on 2026-10-07 with the current working-tree Rust core, release CLI,
and newly generated fictional fixtures. This uses the current default gallery
eligibility prefix (`source/art`), so only one of the generator's five tag
patterns is gallery-index eligible. The scan traverses all generated notes;
the `indexed` CLI count is the eligible subset, not the number of input files.
These results therefore supersede the earlier statement that every generated
note was indexed. Query timings include CLI startup and database opening.

| Input notes | Eligible indexed | Full scan | Unchanged scan | Categories | Broad tag query | Narrow tag query | Warnings |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 7,806 | 1,561 | 1.064 s | 0.094 s | 0.027 s | 0.107 s | 0.021 s | 0 |
| 20,000 | 4,000 | 2.286 s | 0.218 s | 0.039 s | 0.164 s | 0.027 s | 0 |

Fixture generation and compilation are excluded. These are single wall-clock
measurements on this Linux environment, without memory or UI-frame profiling.
The 20,000-input full and unchanged scans remain within the stated few-seconds
target. The connected-device Android benchmark could not be rerun: Flutter
attempted to install Android Build-Tools 36 into the read-only Nix SDK.

## 2026-10-07 current working-tree recheck

Re-ran the Linux CLI benchmark after the KMP note-structure and Desktop video
thumbnail changes, using newly generated fictional fixtures. Fixture generation
and release compilation are excluded; query measurements include CLI startup and
database opening. Each value is a single wall-clock run on this Linux
development environment.

| Input notes | Eligible indexed | Full scan | Unchanged scan | Categories | Broad tag query | Narrow tag query | Warnings |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 7,806 | 1,561 | 0.685 s | 0.057 s | 0.018 s | 0.046 s | 0.015 s | 0 |
| 20,000 | 4,000 | 1.493 s | 0.080 s | 0.022 s | 0.087 s | 0.017 s | 0 |

All scans indexed the expected gallery-eligible subset and reported zero
warnings. These Rust CLI figures do not measure Compose rendering, Android SAF
provider I/O, memory use, or frame timings; the KMP Android measurements below
cover those platform-specific checks.

## 2026-10-10 thumbnail loading redesign

The Android gallery now asks the SAF provider for a bounded thumbnail before
reading and decoding the full media stream, limits concurrent gallery thumbnail
work to four items, and keeps a 32 MiB private in-memory decoded-thumbnail LRU
alongside the existing bounded private disk cache. Gallery image children now
fill their measured 3:4 tile instead of forcing a fixed 220dp height. Android
video thumbnails try provider previews and multiple sync-frame timestamps to
handle MP4 files whose first frame is not decodable. These are implementation
changes, not measured performance results; rerun a generated fictional
image/video fixture benchmark before claiming that slow-media loading is fixed.
Within an Android session, resolved media locations are memoized by media ID and
cleared on rescan to avoid repeating the Rust lookup while scrolling back to
already visited media. This change has not yet been measured separately.
Compose gallery and viewer state holders now dispatch repository/FFI reads to
an injectable background dispatcher, and rapid tag changes are coalesced before
refreshing the gallery and category counts. This prevents synchronous Rust
calls from blocking the UI and avoids redundant filter requests; it does not
reduce the underlying cold SAF read/decode cost. Cold note and display-sized
image loading still need device measurements; the current scan and grid-frame
measurements do not establish a latency comparison with the historical Flutter
application.

## KMP Android SAF and Compose acceptance

Measured on 2026-10-07 on the connected RMX6688 (Android 16 / API 36,
1280x2800, active 120 Hz display) using a dedicated fictional SAF fixture:
2,000 Markdown notes, 246,000 bytes total, all eligible, no media. The folder
was selected through Android's system `ACTION_OPEN_DOCUMENT_TREE` picker; no
user Vault was selected or modified. The measured SAF scan used the device's
Documents provider and the app's normal bounded SAF batch/read path.

| Scenario | Result |
|---|---:|
| Initial selection and full indexing, confirm-to-2,000-note gallery | 21.5 s |
| Explicit unchanged SAF rescan, before scanner scheduling change | 12.7 s |
| Explicit unchanged SAF rescan, after scheduling change (3 runs) | 6.68-6.78 s |
| Explicit unchanged SAF rescan, with shared limit of 16 reads (3 runs) | 3.59-3.95 s |
| App restart with private index reuse, interactive UI visible | within 5.8 s (includes a fixed 3 s wait) |
| Peak sampled app PSS during refresh | 135,281 KiB (132.1 MiB) |

The original scanner awaited reads in groups of four, leaving the next group
idle until the slowest read in the current group finished. It first changed to
a bounded four-read semaphore across each 128-note batch, avoiding those group
barriers. On the same fixture, three unchanged rescans completed in 6.695 s,
6.678 s, and 6.781 s; note reads took 5.327 s, 5.327 s, and 5.419 s, and tree
enumeration took 0.950 s, 0.946 s, and 0.958 s.

A controlled local benchmark increased the shared reader cap to 16 while
retaining the per-note, per-batch, and per-scan byte limits. Three unchanged
rescans then completed in 3.736 s, 3.951 s, and 3.592 s; note reads took
2.412 s, 2.565 s, and 2.282 s, while enumeration took 0.907 s, 0.985 s, and
0.910 s. The fixture remained exactly 2,000 notes and 246,000 bytes. This
meets the broad few-seconds target on this device and Documents provider, but
is not a guarantee for other SAF providers or devices. Each explicit rescan
still reads all note content. A metadata-only shortcut was not introduced
because providers may omit or coarsen modification times, which could silently
leave edited Vault notes stale.

Compose grid scrolling was measured in three runs of 12 fast vertical swipes
through the 2,000-note lazy gallery. The app remained paged, reaching result
offsets beyond 1,000 across the runs.

| Run | Frames | Janky frames | 95th percentile | 99th percentile | Missed VSync |
|---:|---:|---:|---:|---:|---:|
| 1 | 1,214 | 17 (1.40%) | 9 ms | 16 ms | 0 |
| 2 | 1,214 | 16 (1.32%) | 9 ms | 15 ms | 0 |
| 3 | 1,219 | 7 (0.57%) | 8 ms | 12 ms | 0 |

At 120 Hz the frame budget is 8.33 ms; p95 was 8-9 ms with 0.57-1.40%
janky frames and no missed VSync. Visual interaction checks also verified
horizontal scrolling to the trailing Vault/settings actions, unique-note
search, opening note details, and a status-bar overlap fix in the viewer.
The fixture had no media, so KMP thumbnail-cache hits were not applicable;
Android video playback and thumbnail behavior were separately verified with a
fictional media fixture.

The current image-loading path now lets independent Rust thumbnail decodes run
concurrently while serializing only private-cache filesystem operations.
Android viewer loading requests the selected image before sequentially
prefetching neighboring images; neighbor prefetches share the bounded decode
permit used by gallery thumbnails. When no gallery preview is already cached,
Android and Desktop viewers now request the viewer-sized image directly rather
than separately generating/decoding a gallery-sized image first. These
scheduling changes have not been benchmarked against a representative media
Vault, so they do not establish Dart parity or a measured latency improvement;
cold provider I/O and image decode timings remain to be measured on the target
devices. The viewer now resolves adjacent image locations concurrently and
starts bounded Android decode / Desktop thumbnail prefetch for those neighbors
in the background, so a subsequent image switch can use the warm decoded-image
cache. This reduces wait only when prefetch completes before the user switches;
it can increase background I/O and has not yet been measured on-device.

Gallery thumbnail requests are now gated by the Lazy Grid viewport: Android and
Desktop defer loading tiles outside the visible range and start requests as
tiles enter it. This prioritizes the current page without changing image
resolution or encoding. The viewport scheduling change has not been benchmarked
on-device.

Image work now uses a bounded priority scheduler on both platforms: the image
currently open in the viewer takes precedence over visible gallery tiles, and
neighboring viewer prefetches run last. Desktop image work is capped at four
concurrent loads rather than competing without a shared limit. Identical
in-flight requests for the same media and requested size share one provider
read/decode, while decoded results continue to use the existing bounded memory
caches. These scheduling and request-coalescing changes have automated
regression coverage but have not been benchmarked on representative media
Vaults.

Android's previous 128 MiB total note-content scan cap could silently leave
later notes without parsed content in larger Vaults. The shared, still-bounded
SAF limit is now 512 MiB, generated for Android and Rust from `saf-limits.json`.
A fictional staged-scan regression exceeds the old limit without reading a
user Vault; representative device scans are still needed to confirm the
reported missing-note case.
