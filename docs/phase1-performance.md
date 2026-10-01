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
