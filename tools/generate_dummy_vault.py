#!/usr/bin/env python3
"""Generate a fictional Vault for development and performance tests."""

import argparse
import struct
import zlib
from pathlib import Path


TAGS = [
    ("source/service/example", "source/rating/safe", "copyright/original"),
    ("source/service/example", "source/type/human", "source/gender/female"),
    ("source/service/example", "source/type/furry", "source/gender/male"),
    ("source/service/example", "source/format/anime", "source/count/pair"),
    ("source/service/example", "copyright/pin", "source/art"),
]


def tiny_png() -> bytes:
    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(b"\x00\x30\x60\x90"))
        + chunk(b"IEND", b"")
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path)
    parser.add_argument("--notes", type=int, default=100)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    media_dir = args.output / "media"
    media_dir.mkdir(exist_ok=True)
    (media_dir / "pixel.png").write_bytes(tiny_png())

    for index in range(args.notes):
        tags = TAGS[index % len(TAGS)]
        video_cover = index % 10 == 1
        cover = "media/missing-video.mp4" if video_cover else "media/pixel.png"
        tag_yaml = "\n".join(f"  - {tag}" for tag in tags)
        note = (
            "---\n"
            f"created: 2026-01-{index % 28 + 1:02d}\n"
            "tags:\n"
            f"{tag_yaml}\n"
            f"cover: {cover}\n"
            "---\n"
            f"# Fictional note {index:06d}\n"
            "Synthetic test content only.\n\n"
            "# 文書\n## 関連\n- [fictional link](https://example.invalid/related)\n"
            "## 覚書\n- synthetic memo\n"
            + (f"![](<{cover}>)\n" if video_cover else "![](<media/pixel.png>)\n")
        )
        (args.output / f"note-{index:06d}.md").write_text(note, encoding="utf-8")
    print(f"generated {args.notes} fictional notes at {args.output}")


if __name__ == "__main__":
    main()
