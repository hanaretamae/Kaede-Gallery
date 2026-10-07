#![forbid(unsafe_code)]

use serde::Deserialize;
use std::collections::HashSet;
use thiserror::Error;

pub const MAX_NOTE_BYTES: usize = 2 * 1024 * 1024;
pub const MAX_FRONTMATTER_BYTES: usize = 256 * 1024;
pub const MAX_TAGS: usize = 256;
pub const MAX_YAML_DEPTH: usize = 64;
pub const MAX_SETTINGS_BYTES: usize = 64 * 1024;
const DEFAULT_MEMO_HEADINGS: [&str; 4] = ["覚書", "メモ", "Memo", "Notes"];
const DEFAULT_RELATED_HEADINGS: [&str; 2] = ["関連", "Related"];
const DEFAULT_POST_TEXT_END_HEADINGS: [&str; 2] = ["文書", "Document"];

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct NoteStructureSettings {
    pub memo_headings: Vec<String>,
    pub related_headings: Vec<String>,
    pub post_text_end_headings: Vec<String>,
    pub gallery_tag_prefixes: Vec<String>,
    pub frontmatter: FrontmatterSettings,
    pub link_resolution: LinkResolutionMode,
    /// When `false`, blockquote (`> `) lines are excluded from the extracted
    /// post text instead of being kept with the quote marker stripped.
    pub post_text_include_quote: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct FrontmatterSettings {
    pub tags_keys: Vec<String>,
    pub title_keys: Vec<String>,
    pub url_keys: Vec<String>,
    pub published_keys: Vec<String>,
    pub created_keys: Vec<String>,
    pub updated_keys: Vec<String>,
    pub cover_keys: Vec<String>,
}

impl Default for FrontmatterSettings {
    fn default() -> Self {
        Self {
            tags_keys: vec!["tags".into()],
            title_keys: vec!["title".into()],
            url_keys: vec!["url".into()],
            published_keys: vec!["published".into()],
            created_keys: vec!["created".into()],
            updated_keys: vec!["updated".into()],
            cover_keys: vec!["cover".into()],
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum LinkResolutionMode {
    ShortestPath,
    #[default]
    RelativePath,
    AbsolutePath,
}

impl Default for NoteStructureSettings {
    fn default() -> Self {
        Self {
            memo_headings: DEFAULT_MEMO_HEADINGS
                .iter()
                .map(|value| (*value).into())
                .collect(),
            related_headings: DEFAULT_RELATED_HEADINGS
                .iter()
                .map(|value| (*value).into())
                .collect(),
            post_text_end_headings: DEFAULT_POST_TEXT_END_HEADINGS
                .iter()
                .map(|value| (*value).into())
                .collect(),
            gallery_tag_prefixes: vec!["source/art".into()],
            frontmatter: FrontmatterSettings::default(),
            link_resolution: LinkResolutionMode::default(),
            post_text_include_quote: true,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParsedNote {
    pub title: String,
    pub author: Option<String>,
    pub author_url: Option<String>,
    pub url: Option<String>,
    pub published: Option<String>,
    pub created: Option<String>,
    pub updated: Option<String>,
    pub tags: Vec<String>,
    pub media: Vec<MediaReference>,
    pub memo_lines: Vec<Vec<InlineToken>>,
    pub memo_bullets: Vec<bool>,
    pub memo_indent_levels: Vec<u8>,
    pub related_lines: Vec<Vec<InlineToken>>,
    pub related_bullets: Vec<bool>,
    pub related_indent_levels: Vec<u8>,
    pub post_text_end_lines: Vec<Vec<InlineToken>>,
    pub post_text_end_bullets: Vec<bool>,
    pub post_text_end_indent_levels: Vec<u8>,
    pub body_text: String,
}

#[test]
fn tokenizes_obsidian_wikilinks_as_internal_link_targets() {
    assert_eq!(
        tokenize_inline("See [[../notes/target.md#Heading|Target note]]"),
        [
            InlineToken::Text("See ".to_owned()),
            InlineToken::ExternalLink {
                label: "Target note".to_owned(),
                url: "../notes/target.md#Heading".to_owned(),
            },
        ]
    );
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

#[test]
fn tokenizes_angle_bracket_markdown_destinations() {
    assert_eq!(
        tokenize_inline("[spaced note](<./notes/my%20note.md#Heading>)"),
        [InlineToken::ExternalLink {
            label: "spaced note".to_owned(),
            url: "./notes/my%20note.md#Heading".to_owned(),
        }]
    );
}

#[test]
fn tokenizes_embedded_markdown_and_wikilinks_as_navigable_targets() {
    assert_eq!(
        tokenize_inline("![[linked note]] ![](notes/linked%20note.md)"),
        [
            InlineToken::ExternalLink {
                label: "linked note".to_owned(),
                url: "linked note".to_owned(),
            },
            InlineToken::Text(" ".to_owned()),
            InlineToken::ExternalLink {
                label: "linked note".to_owned(),
                url: "notes/linked%20note.md".to_owned(),
            },
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
    #[error("settings exceed the configured size limit")]
    SettingsTooLarge,
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

pub fn parse_note(input: &str) -> Result<ParsedNote, ParseError> {
    parse_note_with_settings(input, &NoteStructureSettings::default())
}

pub fn parse_note_with_settings(
    input: &str,
    settings: &NoteStructureSettings,
) -> Result<ParsedNote, ParseError> {
    parse_note_inner(input, settings, false)
}

pub fn parse_note_for_link_target(
    input: &str,
    settings: &NoteStructureSettings,
) -> Result<ParsedNote, ParseError> {
    parse_note_inner(input, settings, true)
}

fn parse_note_inner(
    input: &str,
    settings: &NoteStructureSettings,
    allow_missing_tags: bool,
) -> Result<ParsedNote, ParseError> {
    if input.len() > MAX_NOTE_BYTES {
        return Err(ParseError::NoteTooLarge);
    }

    let input = input.strip_prefix('\u{feff}').unwrap_or(input);
    let normalized = input.replace("\r\n", "\n");
    let (yaml, body) = if allow_missing_tags && !normalized.starts_with("---\n") {
        ("", normalized.as_str())
    } else {
        split_frontmatter(&normalized)?
    };
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
    let frontmatter: serde_yaml::Mapping =
        serde_yaml::from_str(yaml).map_err(|_| ParseError::InvalidFrontmatter)?;
    let tags_value = first_frontmatter_value(&frontmatter, &settings.frontmatter.tags_keys);
    let tags = match tags_value {
        None if allow_missing_tags => Vec::new(),
        None => return Err(ParseError::MissingTags),
        Some(serde_yaml::Value::Sequence(values)) => values
            .iter()
            .map(frontmatter_string)
            .collect::<Option<Vec<_>>>()
            .ok_or(ParseError::MissingTags)?,
        Some(serde_yaml::Value::String(value)) => value
            .split(',')
            .map(str::trim)
            .filter(|tag| !tag.is_empty())
            .map(ToOwned::to_owned)
            .collect(),
        Some(_) => return Err(ParseError::MissingTags),
    };
    if tags.is_empty() && !allow_missing_tags {
        return Err(ParseError::MissingTags);
    }
    if tags.len() > MAX_TAGS {
        return Err(ParseError::TooManyTags);
    }

    let (body_title, body_without_title) = extract_title(body);
    let title = first_frontmatter_value(&frontmatter, &settings.frontmatter.title_keys)
        .and_then(frontmatter_string)
        .unwrap_or(body_title);
    let sections = extract_sections(body_without_title, settings);
    let (author, author_url) = post_author(body_without_title, settings)
        .map(|(label, url)| (Some(label), Some(url)))
        .unwrap_or((None, None));
    let mut media = Vec::new();
    if let Some(cover) = first_frontmatter_value(&frontmatter, &settings.frontmatter.cover_keys)
        .and_then(frontmatter_string)
    {
        push_media(&mut media, &cover);
    }
    for reference in extract_media(body_without_title) {
        push_media(&mut media, &reference);
    }

    Ok(ParsedNote {
        title,
        author,
        author_url,
        url: url_string(configured_value(
            &frontmatter,
            &settings.frontmatter.url_keys,
        )),
        published: value_string(configured_value(
            &frontmatter,
            &settings.frontmatter.published_keys,
        )),
        created: value_string(configured_value(
            &frontmatter,
            &settings.frontmatter.created_keys,
        )),
        updated: value_string(configured_value(
            &frontmatter,
            &settings.frontmatter.updated_keys,
        )),
        tags,
        media,
        memo_lines: sections.memo,
        memo_bullets: sections.memo_bullets,
        memo_indent_levels: sections.memo_indent_levels,
        related_lines: sections.related,
        related_bullets: sections.related_bullets,
        related_indent_levels: sections.related_indent_levels,
        post_text_end_lines: sections.post_text_end,
        post_text_end_bullets: sections.post_text_end_bullets,
        post_text_end_indent_levels: sections.post_text_end_indent_levels,
        body_text: clean_markdown_text(post_text(body_without_title, settings), settings),
    })
}

pub fn parse_note_structure_settings(input: &str) -> Result<NoteStructureSettings, ParseError> {
    if input.len() > MAX_SETTINGS_BYTES {
        return Err(ParseError::SettingsTooLarge);
    }
    if exceeds_yaml_depth(input) {
        return Err(ParseError::YamlTooDeep);
    }
    #[derive(Deserialize, Default)]
    #[serde(default, rename_all = "camelCase")]
    struct SettingsFile {
        note_structure: NoteStructureSettings,
    }
    let settings = serde_yaml::from_str::<SettingsFile>(input)
        .map_err(|_| ParseError::InvalidFrontmatter)?
        .note_structure;
    for heading in settings
        .memo_headings
        .iter()
        .chain(settings.related_headings.iter())
        .chain(settings.post_text_end_headings.iter())
    {
        if heading.trim().is_empty() || heading.len() > 256 {
            return Err(ParseError::InvalidFrontmatter);
        }
    }
    for key in settings
        .frontmatter
        .tags_keys
        .iter()
        .chain(settings.frontmatter.title_keys.iter())
        .chain(settings.frontmatter.url_keys.iter())
        .chain(settings.frontmatter.published_keys.iter())
        .chain(settings.frontmatter.created_keys.iter())
        .chain(settings.frontmatter.updated_keys.iter())
        .chain(settings.frontmatter.cover_keys.iter())
    {
        if key.trim().is_empty() || key.len() > 128 {
            return Err(ParseError::InvalidFrontmatter);
        }
    }
    if settings.frontmatter.tags_keys.is_empty()
        || settings
            .gallery_tag_prefixes
            .iter()
            .any(|prefix| prefix.trim().is_empty() || prefix.len() > 128)
    {
        return Err(ParseError::InvalidFrontmatter);
    }
    Ok(settings)
}

pub const MAX_TAG_CATEGORIES: usize = 64;

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct TagCategorySettings {
    pub categories: Vec<TagCategoryRule>,
    pub other: OtherCategorySettings,
}

/// One filter category. `path` is either an exact tag (`source/art`) or a
/// subtree (`source/count/*`). Rules sharing a `name` are shown as a single
/// category.
#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TagCategoryRule {
    pub name: String,
    pub path: String,
    #[serde(default)]
    pub split_deep: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct OtherCategorySettings {
    pub enabled: bool,
    pub name: String,
    pub split_deep: bool,
}

impl Default for OtherCategorySettings {
    fn default() -> Self {
        Self {
            enabled: true,
            name: "その他".into(),
            split_deep: false,
        }
    }
}

impl Default for TagCategorySettings {
    fn default() -> Self {
        let rule = |name: &str, path: &str| TagCategoryRule {
            name: name.into(),
            path: path.into(),
            split_deep: false,
        };
        Self {
            categories: vec![
                rule("ソース", "source/art"),
                rule("人数", "source/count/*"),
                rule("アートスタイル", "source/format/*"),
                rule("性別", "source/gender/*"),
                rule("メタ", "source/meta/*"),
                rule("レーティング", "source/rating/*"),
                rule("ソース", "source/*"),
                rule("タイプ", "source/type/*"),
                rule("作品", "copyright/*"),
            ],
            other: OtherCategorySettings::default(),
        }
    }
}

pub fn parse_tag_category_settings(input: &str) -> Result<TagCategorySettings, ParseError> {
    Ok(parse_optional_tag_category_settings(input)?.unwrap_or_default())
}

pub fn parse_optional_tag_category_settings(
    input: &str,
) -> Result<Option<TagCategorySettings>, ParseError> {
    if input.len() > MAX_SETTINGS_BYTES {
        return Err(ParseError::SettingsTooLarge);
    }
    if exceeds_yaml_depth(input) {
        return Err(ParseError::YamlTooDeep);
    }
    #[derive(Deserialize, Default)]
    #[serde(default, rename_all = "camelCase")]
    struct SettingsFile {
        tag_categories: Option<TagCategorySettings>,
    }
    let Some(settings) = serde_yaml::from_str::<SettingsFile>(input)
        .map_err(|_| ParseError::InvalidFrontmatter)?
        .tag_categories
    else {
        return Ok(None);
    };
    if settings.categories.len() > MAX_TAG_CATEGORIES
        || settings.other.name.trim().is_empty()
        || settings.other.name.len() > 128
    {
        return Err(ParseError::InvalidFrontmatter);
    }
    for rule in &settings.categories {
        let path = rule.path.strip_suffix("/*").unwrap_or(&rule.path);
        let valid_path = rule.path == "*"
            || (!path.is_empty()
                && path.len() <= 256
                && !path.contains('*')
                && path.split('/').all(|segment| !segment.trim().is_empty()));
        if rule.name.trim().is_empty() || rule.name.len() > 128 || !valid_path {
            return Err(ParseError::InvalidFrontmatter);
        }
    }
    Ok(Some(settings))
}

fn first_frontmatter_value<'a>(
    frontmatter: &'a serde_yaml::Mapping,
    keys: &[String],
) -> Option<&'a serde_yaml::Value> {
    keys.iter()
        .find_map(|key| frontmatter.get(serde_yaml::Value::String(key.clone())))
}

fn configured_value(
    frontmatter: &serde_yaml::Mapping,
    keys: &[String],
) -> Option<serde_yaml::Value> {
    first_frontmatter_value(frontmatter, keys).cloned()
}

fn frontmatter_string(value: &serde_yaml::Value) -> Option<String> {
    match value {
        serde_yaml::Value::String(value) => Some(value.clone()),
        serde_yaml::Value::Number(value) => Some(value.to_string()),
        _ => None,
    }
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
    memo_bullets: Vec<bool>,
    memo_indent_levels: Vec<u8>,
    related: Vec<Vec<InlineToken>>,
    related_bullets: Vec<bool>,
    related_indent_levels: Vec<u8>,
    post_text_end: Vec<Vec<InlineToken>>,
    post_text_end_bullets: Vec<bool>,
    post_text_end_indent_levels: Vec<u8>,
}

fn extract_sections(body: &str, settings: &NoteStructureSettings) -> Sections {
    let mut sections = Sections::default();
    let mut current: Option<&str> = None;
    for line in body.lines() {
        if let Some((_, heading)) = heading(line) {
            let name = heading.trim().trim_matches('#').trim();
            let memo_matches = settings.memo_headings.iter().any(|heading| heading == name);
            let related_matches = settings
                .related_headings
                .iter()
                .any(|heading| heading == name);
            let post_text_end_matches = settings
                .post_text_end_headings
                .iter()
                .any(|heading| heading == name);
            current = if memo_matches {
                Some("memo")
            } else if related_matches {
                Some("related")
            } else if post_text_end_matches {
                Some("postTextEnd")
            } else {
                None
            };
            continue;
        }
        let Some(section) = current else {
            continue;
        };
        let trimmed = line.trim();
        if trimmed.starts_with("```") || trimmed.starts_with("~~~") {
            continue;
        }
        let indentation = line
            .chars()
            .take_while(|ch| matches!(ch, ' ' | '\t'))
            .map(|ch| if ch == '\t' { 4 } else { 1 })
            .sum::<usize>();
        let mut line = line.trim_start();
        while let Some(quoted) = line.strip_prefix('>') {
            line = quoted.trim_start();
        }
        let (line, is_bullet) = strip_list_marker(line);
        let line = line.trim();
        if line.is_empty()
            || is_media_embed(line)
            || line.chars().all(|ch| matches!(ch, '-' | '*' | '+' | ' '))
        {
            continue;
        }
        match section {
            "memo" => {
                sections.memo.push(tokenize_inline(line));
                sections.memo_bullets.push(is_bullet);
                sections
                    .memo_indent_levels
                    .push((indentation / 2).min(8) as u8);
            }
            "related" => {
                sections.related.push(tokenize_inline(line));
                sections.related_bullets.push(is_bullet);
                sections
                    .related_indent_levels
                    .push((indentation / 2).min(8) as u8);
            }
            _ => {
                sections.post_text_end.push(tokenize_inline(line));
                sections.post_text_end_bullets.push(is_bullet);
                sections
                    .post_text_end_indent_levels
                    .push((indentation / 2).min(8) as u8);
            }
        }
    }
    sections
}

fn is_media_embed(line: &str) -> bool {
    let target = if let Some(target) = line.strip_prefix("![[") {
        target
            .split_once("]]")
            .map(|(target, _)| target.split_once('|').map_or(target, |(target, _)| target))
    } else if let Some(target) = line.strip_prefix("![") {
        target
            .split_once("](")
            .and_then(|(_, target)| target.split_once(')'))
            .map(|(target, _)| target.trim().trim_matches(['<', '>']))
    } else {
        None
    };
    let Some(extension) = target
        .and_then(|target| target.rsplit('/').next())
        .and_then(|name| name.rsplit_once('.').map(|(_, extension)| extension))
    else {
        return false;
    };
    matches!(
        extension.to_ascii_lowercase().as_str(),
        "png" | "jpg" | "jpeg" | "webp" | "gif" | "avif" | "mp4" | "webm" | "mov" | "mkv"
    )
}

fn strip_list_marker(line: &str) -> (&str, bool) {
    for marker in ["- ", "* ", "+ "] {
        if let Some(rest) = line.strip_prefix(marker) {
            return (rest, true);
        }
    }
    let digit_count = line
        .bytes()
        .take_while(|byte| byte.is_ascii_digit())
        .count();
    if digit_count > 0
        && line
            .as_bytes()
            .get(digit_count)
            .is_some_and(|marker| *marker == b'.' || *marker == b')')
        && line.as_bytes().get(digit_count + 1) == Some(&b' ')
    {
        return (&line[digit_count + 2..], true);
    }
    (line, false)
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

fn is_content_heading(name: &str, settings: &NoteStructureSettings) -> bool {
    settings
        .memo_headings
        .iter()
        .chain(settings.related_headings.iter())
        .chain(settings.post_text_end_headings.iter())
        .any(|heading| heading == name)
}

fn post_text<'a>(body: &'a str, settings: &NoteStructureSettings) -> &'a str {
    let mut end = body.len();
    for (offset, line) in body.split_inclusive('\n').scan(0, |position, line| {
        let offset = *position;
        *position += line.len();
        Some((offset, line))
    }) {
        if let Some((_, name)) = heading(line) {
            if is_content_heading(name.trim().trim_matches('#').trim(), settings) {
                end = offset;
                break;
            }
            continue;
        }
    }
    &body[..end]
}

fn post_author(body: &str, settings: &NoteStructureSettings) -> Option<(String, String)> {
    for line in body.lines() {
        if let Some((_, name)) = heading(line) {
            if is_content_heading(name.trim().trim_matches('#').trim(), settings) {
                return None;
            }
            continue;
        }
        if line.trim().is_empty() {
            continue;
        }
        if line.trim_start().starts_with("![") {
            continue;
        }
        return standalone_author(line);
    }
    None
}

fn standalone_author(line: &str) -> Option<(String, String)> {
    let tokens = tokenize_inline(line.trim());
    match tokens.as_slice() {
        [InlineToken::ExternalLink { label, url }]
            if !label.trim().is_empty()
                && (url.starts_with("https://") || url.starts_with("http://")) =>
        {
            Some((label.trim().to_owned(), url.clone()))
        }
        _ => None,
    }
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

fn clean_markdown_text(body: &str, settings: &NoteStructureSettings) -> String {
    let mut awaiting_author = true;
    body.lines()
        .filter_map(|line| {
            if heading(line).is_some() || line.trim().starts_with("![") {
                return None;
            }
            let mut line = line.trim();
            if awaiting_author && line.is_empty() {
                return None;
            }
            if awaiting_author {
                awaiting_author = false;
                if standalone_author(line).is_some() {
                    return None;
                }
            }
            if !settings.post_text_include_quote && line.starts_with("> ") {
                return None;
            }
            while let Some(quoted) = line.strip_prefix("> ") {
                line = quoted.trim_start();
            }
            let (line, _) = strip_list_marker(line);
            Some(
                tokenize_inline(line)
                    .into_iter()
                    .map(|token| match token {
                        InlineToken::Text(text) => clean_markdown_markers(&text),
                        InlineToken::ExternalLink { label, .. } => label,
                    })
                    .collect::<String>(),
            )
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
        let markdown_link = remainder.find("](").and_then(|close| {
            let label_open = remainder[..close].rfind('[')?;
            (!remainder[label_open..].starts_with("[[")).then(|| {
                let label_start = label_open + 1;
                let label_end = close;
                let url_start = label_end + 2;
                let url_end = remainder[url_start..].find(')')? + url_start;
                Some((label_open, label_start, label_end, url_start, url_end))
            })?
        });
        let wiki_link = remainder.find("[[").and_then(|start| {
            let inner_start = start + 2;
            let inner_end = remainder[inner_start..].find("]]")? + inner_start;
            let target = remainder[inner_start..inner_end]
                .split_once('|')
                .map_or(&remainder[inner_start..inner_end], |(target, _)| target)
                .trim();
            let label = remainder[inner_start..inner_end]
                .split_once('|')
                .map_or(target, |(_, label)| label.trim());
            (!target.is_empty()).then_some((start, inner_end + 2, target, label))
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
        let wiki_start = wiki_link.as_ref().map(|link| link.0);
        let url_start = bare_url.as_ref().map(|url| url.0);
        let next = [
            link_start.map(|start| (start, 0)),
            wiki_start.map(|start| (start, 1)),
            url_start.map(|start| (start, 2)),
        ]
        .into_iter()
        .flatten()
        .min_by_key(|(start, _)| *start);
        let Some((start, link_kind)) = next else {
            push_text_token(&mut tokens, &line[cursor..]);
            break;
        };
        let link_prefix = if remainder[..start].ends_with('!') {
            start - 1
        } else {
            start
        };
        push_text_token(&mut tokens, &line[cursor..cursor + link_prefix]);
        if link_kind == 0 {
            if let Some((_, label_start, label_end, url_start, url_end)) = markdown_link {
                let target = markdown_link_target(&remainder[url_start..url_end]);
                let label = &remainder[label_start..label_end];
                tokens.push(InlineToken::ExternalLink {
                    label: if label.is_empty() {
                        link_target_label(target)
                    } else {
                        label.to_owned()
                    },
                    url: target.to_owned(),
                });
                cursor += url_end + 1;
            }
        } else if link_kind == 1 {
            if let Some((_, consumed, target, label)) = wiki_link {
                tokens.push(InlineToken::ExternalLink {
                    label: label.to_owned(),
                    url: target.to_owned(),
                });
                cursor += consumed;
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

fn markdown_link_target(destination: &str) -> &str {
    let destination = destination.trim();
    if let Some(destination) = destination.strip_prefix('<') {
        return destination
            .find('>')
            .map_or(destination, |end| &destination[..end]);
    }
    destination.split_whitespace().next().unwrap_or(destination)
}

fn link_target_label(target: &str) -> String {
    let target = target.split(['#', '?']).next().unwrap_or(target);
    let decoded = urlencoding::decode(target)
        .map(|value| value.into_owned())
        .unwrap_or_else(|_| target.to_owned());
    decoded
        .rsplit('/')
        .next()
        .unwrap_or(&decoded)
        .trim_end_matches(".md")
        .to_owned()
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
    fn configurable_headings_are_independent_of_heading_depth() {
        let input = "---\ntags: [source/example]\n---\n# Example\nFictional post\n\n## Notes for later\n- keep the blue subtle\n### References\n- [study](https://example.invalid/study)\n### End of caption\nNot part of the post text\n";
        let settings = NoteStructureSettings {
            memo_headings: vec!["Notes for later".to_owned()],
            related_headings: vec!["References".to_owned()],
            post_text_end_headings: vec!["End of caption".to_owned()],
            ..NoteStructureSettings::default()
        };
        let note = parse_note_with_settings(input, &settings).expect("valid note");
        assert_eq!(note.body_text, "Fictional post");
        assert_eq!(note.memo_lines.len(), 1);
        assert_eq!(note.related_lines.len(), 1);

        let settings = parse_note_structure_settings(
            r#"{"noteStructure":{"memoHeadings":["Notes"],"relatedHeadings":["Links"],"postTextEndHeadings":["Details"]}}"#,
        )
        .expect("valid settings");
        assert_eq!(settings.memo_headings, ["Notes"]);
        assert_eq!(settings.related_headings, ["Links"]);
        assert_eq!(settings.post_text_end_headings, ["Details"]);
        assert_eq!(settings.gallery_tag_prefixes, ["source/art"]);
        let custom_prefixes = parse_note_structure_settings(
            r#"{"noteStructure":{"galleryTagPrefixes":["portfolio","set/"]}}"#,
        )
        .expect("valid gallery tag prefixes");
        assert_eq!(custom_prefixes.gallery_tag_prefixes, ["portfolio", "set/"]);
        assert_eq!(
            parse_note_structure_settings(r#"{"noteStructure":{"frontmatter":{"tagsKeys":[]}}}"#),
            Err(ParseError::InvalidFrontmatter)
        );
    }

    #[test]
    fn configurable_frontmatter_aliases_keep_existing_defaults() {
        let input = "---\nlabels: [source/example]\nname: YAML title\npage: https://example.invalid/post\nposted: 2026-10-01\nthumbnail: image.webp\n---\n# Body title\nCaption\n";
        let settings = NoteStructureSettings {
            frontmatter: FrontmatterSettings {
                tags_keys: vec!["labels".into()],
                title_keys: vec!["name".into()],
                url_keys: vec!["page".into()],
                published_keys: vec!["posted".into()],
                cover_keys: vec!["thumbnail".into()],
                ..FrontmatterSettings::default()
            },
            ..NoteStructureSettings::default()
        };
        let note = parse_note_with_settings(input, &settings).expect("valid note");
        assert_eq!(note.tags, ["source/example"]);
        assert_eq!(note.title, "YAML title");
        assert_eq!(note.url.as_deref(), Some("https://example.invalid/post"));
        assert_eq!(note.published.as_deref(), Some("2026-10-01"));
        assert_eq!(note.media[0].path, "image.webp");
    }

    #[test]
    fn parses_bom_crlf_video_cover_and_sections() {
        let input = "\u{feff}---\r\nurl: https://example.invalid/post\r\ntags:\r\n  - source/service/example\r\ncover: ../media/clip.mp4\r\npublished: 2026-04-23T16:51:02\r\n---\r\n# Fictional author\r\nFictional **post text**\r\n\r\n# 文書\r\n- fictional ending text with [source](https://example.invalid/source)\r\n## 関連\r\n- [related note](https://example.invalid/note)\r\n## 覚書\r\n-\r\n- remember this\r\n![](<image%20one.webp>)\r\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.title, "Fictional author");
        assert_eq!(note.author, None);
        assert_eq!(note.media.len(), 2);
        assert_eq!(note.media[0].kind, MediaKind::Video);
        assert_eq!(note.media[1].path, "image one.webp");
        assert_eq!(
            note.memo_lines,
            [vec![InlineToken::Text("remember this".to_owned())]]
        );
        assert_eq!(note.memo_bullets, [true]);
        assert_eq!(note.memo_indent_levels, [0]);
        assert_eq!(
            note.related_lines,
            [vec![InlineToken::ExternalLink {
                label: "related note".to_owned(),
                url: "https://example.invalid/note".to_owned()
            }]]
        );
        assert_eq!(note.related_bullets, [true]);
        assert_eq!(
            note.post_text_end_lines,
            [vec![
                InlineToken::Text("fictional ending text with ".to_owned()),
                InlineToken::ExternalLink {
                    label: "source".to_owned(),
                    url: "https://example.invalid/source".to_owned(),
                },
            ]],
        );
        assert_eq!(note.post_text_end_bullets, [true]);
        assert_eq!(note.post_text_end_indent_levels, [0]);
        assert_eq!(note.published.as_deref(), Some("2026-04-23T16:51:02"));
    }

    #[test]
    fn preserves_nested_memo_indentation_levels() {
        let input = "---\ntags: [source/art]\n---\n# title\n# 文書\n## 覚書\n- root\n  - child\n    - grandchild\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(
            note.memo_lines,
            [
                vec![InlineToken::Text("root".to_owned())],
                vec![InlineToken::Text("child".to_owned())],
                vec![InlineToken::Text("grandchild".to_owned())],
            ]
        );
        assert_eq!(note.memo_indent_levels, [0, 1, 2]);
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
    fn supplied_frontmatter_cover_and_body_embed_are_one_media_item() {
        let input = "---\nurl: https://example.invalid/posts/aoikasumi-0001\ntags:\n  - source/service/example\n  - source/art\n  - source/rating/safe\n  - source/type/illustration\npublished: 2026-09-18T14:20:00\ncreated: 2026-09-18T15:00:00\nupdated: 2026-09-18T15:10:00\ncover: ../../media/fictional-rainy-window.webp\n---\n# 雨上がりの観測\n\nurl\n\n![](<../../media/fictional-rainy-window.webp>)\n\n> 雨上がりの窓辺で、架空の青い鳥をスケッチしました。\n\n# 文書\n\n## 関連\n\n- [色の記録](./fictional-color-study.md)\n\n## 覚書\n\n- 窓の反射を少し弱める\n  - 青の彩度は控えめにする\n- 次は夕方の光を試す\n";
        let note = parse_note(input).expect("valid sample note");
        assert_eq!(note.media.len(), 1);
        assert_eq!(
            note.media[0].path,
            "../../media/fictional-rainy-window.webp"
        );
        assert_eq!(note.memo_bullets, [true, true, true]);
        assert_eq!(note.related_bullets, [true]);
    }

    #[test]
    fn separates_leading_author_link_from_post_text() {
        let input = "---\nurl: https://x.com/noone_oO/status/123\ntags: [source/art]\n---\n# Post title\n[@noone_oO](https://x.com/noone_oO)\n\n![](image.webp)\n\n> ONEちゃん\n";
        let note = parse_note(input).expect("valid note");
        assert_eq!(note.author.as_deref(), Some("@noone_oO"));
        assert_eq!(note.author_url.as_deref(), Some("https://x.com/noone_oO"));
        assert_eq!(note.body_text, "ONEちゃん");
        assert!(!note.body_text.contains("noone_oO"));

        let input = "---\ntags: [source/art]\n---\n# Post title\n".to_owned()
            + "![](fictional.webp)\n\n"
            + "[fictional artist](https://example.invalid/artist)\n\nPost text";
        let note = parse_note(&input).expect("valid note");
        assert_eq!(note.author.as_deref(), Some("fictional artist"));
        assert_eq!(
            note.author_url.as_deref(),
            Some("https://example.invalid/artist")
        );
        assert_eq!(note.body_text, "Post text");
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
    fn memo_and_related_capture_all_content_at_any_heading_depth() {
        let input = "---\ntags: [source/example]\n---\n# Title\nPost\n\n## 覚書\nplain memo\n> quoted memo\n```text\ncode memo\n```\n\n### 関連\nplain related\n- [related note](./other.md)\n";
        let settings = parse_note_structure_settings(
            r#"{"noteStructure":{"memoHeadingLevel":3,"relatedHeadingLevel":2,"memoBulletsRequired":true,"relatedBulletsRequired":true}}"#,
        )
        .expect("legacy conditions are ignored");
        let note = parse_note_with_settings(input, &settings).expect("valid note");
        assert_eq!(note.memo_lines.len(), 3);
        assert_eq!(
            note.memo_lines,
            [
                vec![InlineToken::Text("plain memo".to_owned())],
                vec![InlineToken::Text("quoted memo".to_owned())],
                vec![InlineToken::Text("code memo".to_owned())],
            ]
        );
        assert_eq!(note.related_lines.len(), 2);
        assert_eq!(
            note.related_lines[0],
            vec![InlineToken::Text("plain related".to_owned())]
        );
        assert!(matches!(
            note.related_lines[1].as_slice(),
            [InlineToken::ExternalLink { label, url }]
                if label == "related note" && url == "./other.md"
        ));

        assert!(
            parse_note_structure_settings(r#"{"noteStructure":{"memoHeadingLevel":7}}"#).is_ok()
        );
    }

    #[test]
    fn legacy_bullet_only_settings_no_longer_filter_section_content() {
        let input = "---\ntags: [source/example]\n---\n# Title\nPost\n\n## 覚書\nnot a bullet\n- a bullet\n\n## 関連\n- a related bullet\nnot a bullet related\n";
        let settings = parse_note_structure_settings(
            r#"{"noteStructure":{"memoBulletsRequired":true,"relatedBulletsRequired":true}}"#,
        )
        .expect("legacy list restriction is ignored");
        let note = parse_note_with_settings(input, &settings).expect("valid note");
        assert_eq!(note.memo_lines.len(), 2);
        assert_eq!(
            note.memo_lines[0],
            vec![InlineToken::Text("not a bullet".to_owned())]
        );
        assert_eq!(note.related_lines.len(), 2);
        assert_eq!(
            note.related_lines[1],
            vec![InlineToken::Text("not a bullet related".to_owned())]
        );
    }

    #[test]
    fn post_text_include_quote_toggle_controls_blockquote_lines() {
        let input = "---\ntags: [source/example]\n---\n# Title\nPlain line\n> quoted line\n";
        let note = parse_note(input).expect("valid note with default settings");
        assert_eq!(note.body_text, "Plain line\nquoted line");

        let settings = NoteStructureSettings {
            post_text_include_quote: false,
            ..NoteStructureSettings::default()
        };
        let note = parse_note_with_settings(input, &settings).expect("valid note");
        assert_eq!(note.body_text, "Plain line");

        let settings =
            parse_note_structure_settings(r#"{"noteStructure":{"postTextIncludeQuote":false}}"#)
                .expect("valid settings");
        assert!(!settings.post_text_include_quote);
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

    #[test]
    fn parses_and_validates_tag_category_settings() {
        let defaults = parse_tag_category_settings("{}").expect("defaults");
        assert_eq!(defaults, TagCategorySettings::default());
        assert!(defaults.categories.iter().all(|rule| !rule.split_deep));
        let custom = parse_tag_category_settings(
            r#"{"tagCategories":{"categories":[{"name":"A","path":"x/*","splitDeep":true}],"other":{"enabled":false}}}"#,
        )
        .expect("custom");
        assert_eq!(custom.categories.len(), 1);
        assert!(custom.categories[0].split_deep);
        assert!(!custom.other.enabled);
        assert!(
            parse_tag_category_settings(
                r#"{"tagCategories":{"categories":[{"name":"A","path":"x//y"}]}}"#
            )
            .is_err()
        );
        assert!(
            parse_tag_category_settings(
                r#"{"tagCategories":{"categories":[{"name":" ","path":"x"}]}}"#
            )
            .is_err()
        );
    }
}
