#![forbid(unsafe_code)]

use serde::Deserialize;
use std::collections::HashSet;
use thiserror::Error;

pub const MAX_NOTE_BYTES: usize = 2 * 1024 * 1024;
pub const MAX_FRONTMATTER_BYTES: usize = 256 * 1024;
pub const MAX_TAGS: usize = 256;
pub const MAX_YAML_DEPTH: usize = 64;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParsedNote {
    pub title: String,
    pub url: Option<String>,
    pub published: Option<String>,
    pub created: Option<String>,
    pub updated: Option<String>,
    pub tags: Vec<String>,
    pub media: Vec<MediaReference>,
    pub memo_lines: Vec<Vec<InlineToken>>,
    pub related_lines: Vec<Vec<InlineToken>>,
    pub body_text: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum InlineToken {
    Text(String),
    ExternalLink { label: String, url: String },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MediaReference {
    pub path: String,
    pub kind: MediaKind,
}

#[test]
fn tokenizes_markdown_links_and_bare_urls_without_rendering_markdown() {
    let tokens = tokenize_inline(
        "source [display](https://example.invalid/path) and https://example.invalid/plain.",
    );
    assert_eq!(
        tokens,
        [
            InlineToken::Text("source ".to_owned()),
            InlineToken::ExternalLink {
                label: "display".to_owned(),
                url: "https://example.invalid/path".to_owned()
            },
            InlineToken::Text(" and ".to_owned()),
            InlineToken::ExternalLink {
                label: "https://example.invalid/plain".to_owned(),
                url: "https://example.invalid/plain".to_owned()
            },
            InlineToken::Text(".".to_owned())
        ]
    );
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MediaKind {
    Image,
    Video,
}

#[derive(Debug, Error, PartialEq, Eq)]
pub enum ParseError {
    #[error("note exceeds the configured size limit")]
    NoteTooLarge,
    #[error("frontmatter exceeds the configured size limit")]
    FrontmatterTooLarge,
    #[error("frontmatter is missing or malformed")]
    InvalidFrontmatter,
    #[error("frontmatter contains unsupported YAML aliases")]
    UnsupportedYamlAliases,
    #[error("frontmatter nesting exceeds the configured limit")]
    YamlTooDeep,
    #[error("frontmatter tags are missing or invalid")]
    MissingTags,
    #[error("frontmatter contains too many tags")]
    TooManyTags,
}

#[derive(Debug, Default, Deserialize)]
struct Frontmatter {
    url: Option<serde_yaml::Value>,
    published: Option<serde_yaml::Value>,
    created: Option<serde_yaml::Value>,
    updated: Option<serde_yaml::Value>,
    cover: Option<String>,
    tags: Option<Vec<String>>,
}

pub fn parse_note(input: &str) -> Result<ParsedNote, ParseError> {
    if input.len() > MAX_NOTE_BYTES {
        return Err(ParseError::NoteTooLarge);
    }

    let input = input.strip_prefix('\u{feff}').unwrap_or(input);
    let normalized = input.replace("\r\n", "\n");
    let (yaml, body) = split_frontmatter(&normalized)?;
    if yaml.len() > MAX_FRONTMATTER_BYTES {
        return Err(ParseError::FrontmatterTooLarge);
    }
    if exceeds_yaml_depth(yaml) {
        return Err(ParseError::YamlTooDeep);
    }
    if contains_alias_token(yaml) {
        // The selected serde-compatible parser has no public alias expansion
        // limit. Reject aliases before deserialization to bound expansion.
        return Err(ParseError::UnsupportedYamlAliases);
    }
    let frontmatter: Frontmatter =
        serde_yaml::from_str(yaml).map_err(|_| ParseError::InvalidFrontmatter)?;
    let tags = frontmatter.tags.ok_or(ParseError::MissingTags)?;
    if tags.is_empty() {
        return Err(ParseError::MissingTags);
    }
    if tags.len() > MAX_TAGS {
        return Err(ParseError::TooManyTags);
    }

    let (title, body_without_title) = extract_title(body);
    let sections = extract_sections(body_without_title);
    let mut media = Vec::new();
    if let Some(cover) = frontmatter.cover.as_deref() {
        push_media(&mut media, cover);
    }
    for reference in extract_media(body_without_title) {
        push_media(&mut media, &reference);
    }

    Ok(ParsedNote {
        title,
        url: url_string(frontmatter.url),
        published: value_string(frontmatter.published),
        created: value_string(frontmatter.created),
        updated: value_string(frontmatter.updated),
        tags,
        media,
        memo_lines: sections.memo,
        related_lines: sections.related,
        body_text: clean_markdown_text(post_text(body_without_title)),
    })
}

fn split_frontmatter(input: &str) -> Result<(&str, &str), ParseError> {
    let rest = input
        .strip_prefix("---\n")
        .ok_or(ParseError::InvalidFrontmatter)?;
    let end = rest.find("\n---").ok_or(ParseError::InvalidFrontmatter)?;
    let yaml = &rest[..end];
    let body_start = end + 4;
    let body = rest[body_start..]
        .strip_prefix('\n')
        .unwrap_or(&rest[body_start..]);
    Ok((yaml, body))
}

fn contains_alias_token(yaml: &str) -> bool {
    let bytes = yaml.as_bytes();
    let mut quote = None;
    let mut comment = false;
    let mut index = 0;
    while index < bytes.len() {
        let byte = bytes[index];
        if byte == b'\n' {
            comment = false;
            index += 1;
            continue;
        }
        if comment {
            index += 1;
            continue;
        }
        match quote {
            Some(b'"') => {
                if byte == b'\\' {
                    index = (index + 2).min(bytes.len());
                    continue;
                }
                if byte == b'"' {
                    quote = None;
                }
            }
            Some(b'\'') => {
                if byte == b'\'' {
                    if bytes.get(index + 1) == Some(&b'\'') {
                        index += 2;
                        continue;
                    }
                    quote = None;
                }
            }
            _ => match byte {
                b'"' | b'\'' => quote = Some(byte),
                b'#' if index == 0 || bytes[index - 1].is_ascii_whitespace() => comment = true,
                b'*' if bytes.get(index + 1).is_some_and(|next| {
                    next.is_ascii_alphanumeric() || *next == b'_' || *next == b'-'
                }) && (index == 0
                    || bytes[index - 1].is_ascii_whitespace()
                    || matches!(bytes[index - 1], b'[' | b'{' | b',' | b':')) =>
                {
                    return true;
                }
                _ => {}
            },
        }
        index += 1;
    }
    false
}

fn exceeds_yaml_depth(yaml: &str) -> bool {
    let mut flow_depth = 0usize;
    let mut quote = None;
    let mut comment = false;
    for line in yaml.lines() {
        if line.chars().take_while(|ch| *ch == ' ').count() > MAX_YAML_DEPTH {
            return true;
        }
        let mut sequence_depth = 0usize;
        let mut cursor = line.trim_start();
        while let Some(rest) = cursor.strip_prefix("- ") {
            sequence_depth += 1;
            cursor = rest.trim_start();
        }
        if sequence_depth > MAX_YAML_DEPTH {
            return true;
        }
        for byte in line.bytes() {
            if comment {
                break;
            }
            match quote {
                Some(b'"') => {
                    if byte == b'\\' {
                        continue;
                    }
                    if byte == b'"' {
                        quote = None;
                    }
                }
                Some(b'\'') => {
                    if byte == b'\'' {
                        quote = None;
                    }
                }
                _ => match byte {
                    b'"' | b'\'' => quote = Some(byte),
                    b'#' => comment = true,
                    b'[' | b'{' => {
                        flow_depth += 1;
                        if flow_depth > MAX_YAML_DEPTH {
                            return true;
                        }
                    }
                    b']' | b'}' => flow_depth = flow_depth.saturating_sub(1),
                    _ => {}
                },
            }
        }
        comment = false;
    }
    false
}

fn value_string(value: Option<serde_yaml::Value>) -> Option<String> {
    value.and_then(|value| match value {
        serde_yaml::Value::String(value) => Some(value),
        serde_yaml::Value::Number(value) => Some(value.to_string()),
        serde_yaml::Value::Null => None,
        other => serde_yaml::to_string(&other)
            .ok()
            .map(|value| value.trim().to_owned()),
    })
}

/// `url` is usually a scalar string, but some notes store it as a single-item
/// YAML list (e.g. from clients that always emit a sequence). Accept both
/// forms, taking the first entry of a list.
fn url_string(value: Option<serde_yaml::Value>) -> Option<String> {
    match value {
        Some(serde_yaml::Value::Sequence(items)) => {
            items.into_iter().find_map(|item| value_string(Some(item)))
        }
        other => value_string(other),
    }
}

fn extract_title(body: &str) -> (String, &str) {
    for line in body.lines() {
        let trimmed = line.trim();
        if let Some(title) = trimmed.strip_prefix("# ") {
            return (title.trim().to_owned(), body);
        }
    }
    let title = body
        .lines()
        .find(|line| !line.trim().is_empty())
        .unwrap_or("Untitled")
        .trim()
        .trim_start_matches('#')
        .trim()
        .to_owned();
    (title, body)
}

#[derive(Default)]
struct Sections {
    memo: Vec<Vec<InlineToken>>,
    related: Vec<Vec<InlineToken>>,
}

fn extract_sections(body: &str) -> Sections {
    let mut sections = Sections::default();
    let mut current: Option<&str> = None;
    for line in body.lines() {
        if let Some((_, heading)) = heading(line) {
            let name = heading.trim().trim_matches('#').trim();
            current = match name {
                "覚書" | "メモ" => Some("memo"),
                "関連" => Some("related"),
                _ => None,
            };
            continue;
        }
        let Some(section) = current else {
            continue;
        };
        let line = line.trim();
        let line = line
            .strip_prefix("- ")
            .or_else(|| line.strip_prefix("* "))
            .or_else(|| line.strip_prefix("+ "))
            .unwrap_or(line)
            .trim();
        if line.is_empty()
            || line.starts_with("![")
            || line.chars().all(|ch| matches!(ch, '-' | '*' | '+' | ' '))
        {
            continue;
        }
        if section == "memo" {
            sections.memo.push(tokenize_inline(line));
        } else {
            sections.related.push(tokenize_inline(line));
        }
    }
    sections
}

fn heading(line: &str) -> Option<(usize, &str)> {
    let trimmed = line.trim_start();
    let level = trimmed.chars().take_while(|ch| *ch == '#').count();
    if (1..=6).contains(&level) && trimmed.as_bytes().get(level) == Some(&b' ') {
        Some((level, trimmed[level..].trim()))
    } else {
        None
    }
}

fn post_text(body: &str) -> &str {
    let mut skipped_title = false;
    let mut end = body.len();
    for (offset, line) in body.split_inclusive('\n').scan(0, |position, line| {
        let offset = *position;
        *position += line.len();
        Some((offset, line))
    }) {
        if let Some((_, name)) = heading(line) {
            if name == "文書" {
                end = offset;
                break;
            }
            if !skipped_title {
                skipped_title = true;
                continue;
            }
        }
        if !skipped_title {
            if line.trim().is_empty() {
                continue;
            }
            skipped_title = true;
        }
    }
    &body[..end]
}

fn extract_media(body: &str) -> Vec<String> {
    let mut result = Vec::new();
    let mut rest = body;
    while let Some(start) = rest.find("![") {
        rest = &rest[start + 2..];
        let Some(close_alt) = rest.find("](") else {
            break;
        };
        rest = &rest[close_alt + 2..];
        let (path, consumed) = if let Some(inside) = rest.strip_prefix('<') {
            inside.find('>').map(|end| (&inside[..end], end + 2))
        } else {
            rest.find(')').map(|end| (&rest[..end], end + 1))
        }
        .unwrap_or(("", 0));
        if consumed == 0 {
            break;
        }
        let path = path.trim();
        if !path.is_empty() {
            result.push(path.to_owned());
        }
        rest = &rest[consumed..];
    }
    result
}

fn decode_path(path: &str) -> String {
    urlencoding::decode(path)
        .map(|value| value.into_owned())
        .unwrap_or_else(|_| path.to_owned())
}

fn push_media(media: &mut Vec<MediaReference>, path: &str) {
    let path = decode_path(path.trim());
    if path.is_empty() || media.iter().any(|item| item.path == path) {
        return;
    }
    let extension = path
        .rsplit('/')
        .next()
        .unwrap_or(&path)
        .rsplit('.')
        .next()
        .unwrap_or("")
        .to_ascii_lowercase();
    let kind = match extension.as_str() {
        "png" | "jpg" | "jpeg" | "webp" | "gif" | "avif" => MediaKind::Image,
        "mp4" | "webm" | "mov" | "mkv" => MediaKind::Video,
        _ => return,
    };
    media.push(MediaReference { path, kind });
}

fn clean_markdown_text(body: &str) -> String {
    body.lines()
        .filter(|line| heading(line).is_none() && !line.trim().starts_with("!["))
        .map(|line| {
            let line = line
                .trim()
                .strip_prefix("> ")
                .unwrap_or(line.trim())
                .strip_prefix("- ")
                .or_else(|| line.trim().strip_prefix("* "))
                .or_else(|| line.trim().strip_prefix("+ "))
                .unwrap_or(line.trim());
            tokenize_inline(line)
                .into_iter()
                .map(|token| match token {
                    InlineToken::Text(text) => clean_markdown_markers(&text),
                    InlineToken::ExternalLink { label, .. } => label,
                })
                .collect::<String>()
        })
        .filter(|line| !line.is_empty())
        .collect::<Vec<_>>()
        .join("\n")
}

fn clean_markdown_markers(text: &str) -> String {
    let mut cleaned = text.to_owned();
    for marker in ["**", "__", "~~", "`", "*", "_"] {
        cleaned = cleaned.replace(marker, "");
    }
    cleaned
}

fn tokenize_inline(line: &str) -> Vec<InlineToken> {
    let mut tokens = Vec::new();
    let mut cursor = 0;
    while cursor < line.len() {
        let remainder = &line[cursor..];
        let markdown_link = remainder.find('[').and_then(|start| {
            let label_start = start + 1;
            let label_end = remainder[label_start..].find("](")? + label_start;
            let url_start = label_end + 2;
            let url_end = remainder[url_start..].find(')')? + url_start;
            Some((start, label_start, label_end, url_start, url_end))
        });
        let bare_url = ["https://", "http://"]
            .into_iter()
            .filter_map(|scheme| remainder.find(scheme).map(|at| (at, scheme)))
            .min_by_key(|(at, _)| *at)
            .map(|(start, scheme)| {
                let end = remainder[start..]
                    .find(char::is_whitespace)
                    .map(|offset| start + offset)
                    .unwrap_or(remainder.len());
                let mut end = end;
                while end > start
                    && matches!(
                        remainder.as_bytes()[end - 1],
                        b'.' | b',' | b';' | b'!' | b')' | b']'
                    )
                {
                    end -= 1;
                }
                (start, start + scheme.len(), end)
            });

        let link_start = markdown_link.as_ref().map(|link| link.0);
        let url_start = bare_url.as_ref().map(|url| url.0);
        let next = match (link_start, url_start) {
            (Some(link), Some(url)) if url < link => Some((url, false)),
            (Some(link), _) => Some((link, true)),
            (_, Some(url)) => Some((url, false)),
            _ => None,
        };
        let Some((start, is_markdown_link)) = next else {
            push_text_token(&mut tokens, &line[cursor..]);
            break;
        };
        push_text_token(&mut tokens, &line[cursor..cursor + start]);
        if is_markdown_link {
            if let Some((_, label_start, label_end, url_start, url_end)) = markdown_link {
                tokens.push(InlineToken::ExternalLink {
                    label: remainder[label_start..label_end].to_owned(),
                    url: remainder[url_start..url_end].to_owned(),
                });
                cursor += url_end + 1;
            }
        } else if let Some((url_start, _, url_end)) = bare_url {
            tokens.push(InlineToken::ExternalLink {
                label: remainder[url_start..url_end].to_owned(),
                url: remainder[url_start..url_end].to_owned(),
            });
            cursor += url_end;
        }
    }
    tokens
}

fn push_text_token(tokens: &mut Vec<InlineToken>, text: &str) {
    if !text.is_empty() {
        tokens.push(InlineToken::Text(text.to_owned()));
    }
}

pub fn expanded_tags(tags: &[String]) -> Vec<String> {
    let mut expanded = Vec::new();
    let mut seen = HashSet::new();
    for tag in tags {
        let mut path = String::new();
        for component in tag.split('/').filter(|part| !part.is_empty()) {
            if !path.is_empty() {
                path.push('/');
            }
            path.push_str(component);
            if seen.insert(path.clone()) {
                expanded.push(path.clone());
            }
        }
    }
    expanded
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_bom_crlf_video_cover_and_sections() {
        let input = "\u{feff}---\r\nurl: https://example.invalid/post\r\ntags:\r\n  - source/service/example\r\ncover: ../media/clip.mp4\r\npublished: 2026-04-23T16:51:02\r\n---\r\n# Fictional author\r\nFictional **post text**\r\n\r\n# 文書\r\n## 関連\r\n- [related note](https://example.invalid/note)\r\n## 覚書\r\n-\r\n- remember this\r\n![](<image%20one.webp>)\r\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.title, "Fictional author");
        assert_eq!(note.media.len(), 2);
        assert_eq!(note.media[0].kind, MediaKind::Video);
        assert_eq!(note.media[1].path, "image one.webp");
        assert_eq!(
            note.memo_lines,
            [vec![InlineToken::Text("remember this".to_owned())]]
        );
        assert_eq!(
            note.related_lines,
            [vec![InlineToken::ExternalLink {
                label: "related note".to_owned(),
                url: "https://example.invalid/note".to_owned()
            }]]
        );
        assert_eq!(note.published.as_deref(), Some("2026-04-23T16:51:02"));
    }

    #[test]
    fn parses_markdown_paths_with_angle_brackets() {
        let input = "---\ntags: [source/art]\n---\n# title\n![](<images/my%20image.jpg>)\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.media[0].path, "images/my image.jpg");
    }

    #[test]
    fn parses_the_supplied_markdown_only_note_shape() {
        let input = "---\nurl: https://example.invalid/post\ntags:\n  - source/service/x\n  - source/art\n  - source/type/human\n  - source/type/creature\n  - source/gender/female\n  - source/gender/male\n  - copyright/original\n  - source/format/anime\n  - source/count/pair\npublished: 2026-08-14T10:57:29\ncreated: 2026-09-30T17:26:34\nupdated: 2026-09-30T17:28\ncover: ../../media/clip.mp4\n---\n# Fictional title\nA post text with [source link](https://example.invalid/source).\n\n# 文書\n## 関連\n-\n## 覚書\n-\n![](<../../media/clip.mp4>)\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.title, "Fictional title");
        assert_eq!(note.tags.len(), 9);
        assert_eq!(note.media.len(), 1);
        assert_eq!(note.media[0].kind, MediaKind::Video);
        assert_eq!(note.body_text, "A post text with source link.");
        assert!(note.related_lines.is_empty());
        assert!(note.memo_lines.is_empty());
    }

    #[test]
    fn accepts_url_as_a_single_item_yaml_list() {
        let input =
            "---\nurl:\n  - https://example.invalid/post\ntags:\n  - source/art\n---\n# title\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.url.as_deref(), Some("https://example.invalid/post"));
    }

    #[test]
    fn rejects_missing_tags_and_aliases() {
        assert_eq!(
            parse_note("---\ntitle: x\n---\n"),
            Err(ParseError::MissingTags)
        );
        assert_eq!(
            parse_note("---\ntags: &tag [source/a]\nother: *tag\n---\n"),
            Err(ParseError::UnsupportedYamlAliases)
        );
    }

    #[test]
    fn does_not_treat_asterisks_in_quotes_or_comments_as_aliases() {
        let input = "---\ntags: [source/a]\nnote: \"a * b\"\n# *example\n---\n# title\n";
        assert!(parse_note(input).is_ok());
    }

    #[test]
    fn expands_hierarchical_tags_once() {
        assert_eq!(
            expanded_tags(&["a/b/c".into(), "a/b/d".into()]),
            ["a", "a/b", "a/b/c", "a/b/d"]
        );
    }

    #[test]
    fn enforces_input_and_tag_limits() {
        assert_eq!(
            parse_note(&"x".repeat(MAX_NOTE_BYTES + 1)),
            Err(ParseError::NoteTooLarge)
        );
        let oversized_frontmatter = format!(
            "---\ntags: [source/a]\ncomment: {}\n---\n",
            "x".repeat(MAX_FRONTMATTER_BYTES)
        );
        assert_eq!(
            parse_note(&oversized_frontmatter),
            Err(ParseError::FrontmatterTooLarge)
        );
        let tags = (0..=MAX_TAGS)
            .map(|index| format!("  - source/tag-{index}"))
            .collect::<Vec<_>>()
            .join("\n");
        assert_eq!(
            parse_note(&format!("---\ntags:\n{tags}\n---\n")),
            Err(ParseError::TooManyTags)
        );
        let nested = format!(
            "---\ntags: [source/a]\nfield:\n{}value: 1\n---\n",
            " ".repeat(MAX_YAML_DEPTH + 1)
        );
        assert_eq!(parse_note(&nested), Err(ParseError::YamlTooDeep));
    }
}
