#![forbid(unsafe_code)]

//! Generates a fictional Vault for development and performance tests.

use std::env;
use std::fmt::Write as _;
use std::fs;
use std::path::PathBuf;
use std::process::ExitCode;

const TAGS: [[&str; 3]; 5] = [
    [
        "source/service/example",
        "source/rating/safe",
        "copyright/original",
    ],
    [
        "source/service/example",
        "source/type/human",
        "source/gender/female",
    ],
    [
        "source/service/example",
        "source/type/furry",
        "source/gender/male",
    ],
    [
        "source/service/example",
        "source/format/anime",
        "source/count/pair",
    ],
    ["source/service/example", "copyright/pin", "source/art"],
];

fn main() -> ExitCode {
    match run() {
        Ok(()) => ExitCode::SUCCESS,
        Err(message) => {
            eprintln!("error: {message}");
            ExitCode::FAILURE
        }
    }
}

fn run() -> Result<(), String> {
    let usage = || "usage: generate-dummy-vault <output-directory> [--notes <count>]".to_owned();
    let mut args = env::args().skip(1);
    let output = PathBuf::from(args.next().ok_or_else(usage)?);
    let mut notes: usize = 100;
    while let Some(argument) = args.next() {
        if argument != "--notes" {
            return Err(usage());
        }
        notes = args
            .next()
            .and_then(|value| value.parse().ok())
            .ok_or_else(usage)?;
    }
    let media = output.join("media");
    fs::create_dir_all(&media).map_err(|_| "the output directory could not be created")?;
    fs::write(media.join("pixel.png"), tiny_png()).map_err(|_| "the image could not be written")?;
    for index in 0..notes {
        let tags = TAGS[index % TAGS.len()];
        let video_cover = index % 10 == 1;
        let cover = if video_cover {
            "media/missing-video.mp4"
        } else {
            "media/pixel.png"
        };
        let mut note = String::from("---\n");
        let _ = writeln!(note, "created: 2026-01-{:02}", index % 28 + 1);
        note.push_str("tags:\n");
        for tag in tags {
            let _ = writeln!(note, "  - {tag}");
        }
        let _ = write!(
            note,
            "cover: {cover}\n---\n# Fictional note {index:06}\nSynthetic test content only.\n\n\
             # 文書\n## 関連\n- [fictional link](https://example.invalid/related)\n\
             ## 覚書\n- synthetic memo\n![](<{cover}>)\n"
        );
        fs::write(output.join(format!("note-{index:06}.md")), note)
            .map_err(|_| "a note could not be written")?;
    }
    println!("generated {notes} fictional notes at {}", output.display());
    Ok(())
}

fn crc32(bytes: &[u8]) -> u32 {
    let mut crc = 0xFFFF_FFFF_u32;
    for byte in bytes {
        crc ^= u32::from(*byte);
        for _ in 0..8 {
            crc = if crc & 1 == 1 {
                (crc >> 1) ^ 0xEDB8_8320
            } else {
                crc >> 1
            };
        }
    }
    !crc
}

fn adler32(bytes: &[u8]) -> u32 {
    let (mut a, mut b) = (1_u32, 0_u32);
    for byte in bytes {
        a = (a + u32::from(*byte)) % 65_521;
        b = (b + a) % 65_521;
    }
    (b << 16) | a
}

fn chunk(kind: &[u8; 4], data: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(data.len() + 12);
    out.extend_from_slice(&(data.len() as u32).to_be_bytes());
    let mut body = kind.to_vec();
    body.extend_from_slice(data);
    out.extend_from_slice(&body);
    out.extend_from_slice(&crc32(&body).to_be_bytes());
    out
}

/// A 1x1 RGB PNG using a stored (uncompressed) deflate block.
fn tiny_png() -> Vec<u8> {
    let raw = [0x00, 0x30, 0x60, 0x90];
    let mut zlib = vec![
        0x78,
        0x01,
        0x01,
        raw.len() as u8,
        0x00,
        !(raw.len() as u8),
        0xFF,
    ];
    zlib.extend_from_slice(&raw);
    zlib.extend_from_slice(&adler32(&raw).to_be_bytes());
    let mut ihdr = Vec::new();
    ihdr.extend_from_slice(&1_u32.to_be_bytes());
    ihdr.extend_from_slice(&1_u32.to_be_bytes());
    ihdr.extend_from_slice(&[8, 2, 0, 0, 0]);
    let mut png = b"\x89PNG\r\n\x1a\n".to_vec();
    png.extend(chunk(b"IHDR", &ihdr));
    png.extend(chunk(b"IDAT", &zlib));
    png.extend(chunk(b"IEND", &[]));
    png
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn png_has_valid_signature_and_crc() {
        let png = tiny_png();
        assert_eq!(&png[..8], b"\x89PNG\r\n\x1a\n");
        assert_eq!(crc32(b"IEND"), 0xAE42_6082);
    }
}
