# Phase 1 performance baseline

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
