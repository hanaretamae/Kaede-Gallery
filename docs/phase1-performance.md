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
checks during the Android phase. Re-run the expanded benchmark with
`bash tools/benchmark.sh <note-count> <work-directory>`; it measures full and
unchanged scans, category lookup, and broad/narrow tag-filtered list queries.
