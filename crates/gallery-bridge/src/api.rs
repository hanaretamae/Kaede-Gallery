use flutter_rust_bridge::frb;
use gallery_core::{
    Category as CoreCategory, CoreError, DetailLine as CoreDetailLine, Gallery,
    MediaSummary as CoreMediaSummary, NoteDetail as CoreNoteDetail, NoteSummary as CoreNoteSummary,
    ScanReport as CoreScanReport, VirtualFilter as CoreVirtualFilter, load_selected_vault,
    prepare_private_app_directory, save_selected_vault,
};
use std::path::Path;

#[frb(non_opaque)]
#[derive(Clone)]
pub struct ScanReport {
    pub notes_indexed: u32,
    pub warnings: u32,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct CategoryOption {
    pub name: String,
    pub full_tag: String,
    pub count: u32,
    pub disabled: bool,
    pub virtual_filter: Option<String>,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct Category {
    pub path: String,
    pub display_name: String,
    pub options: Vec<CategoryOption>,
    pub count: u32,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct NoteSummary {
    pub id: u32,
    pub path: String,
    pub title: String,
    pub media_count: u32,
    pub video_count: u32,
    pub memo_count: u32,
    pub related_count: u32,
    pub representative_media_id: Option<u32>,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct MediaItem {
    pub id: u32,
    pub note_id: u32,
    pub is_video: bool,
    pub exists: bool,
    pub media_count: u32,
    pub memo_count: u32,
    pub related_count: u32,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct DetailLine {
    pub text: String,
    pub urls: Vec<String>,
    pub is_bullet: bool,
    pub indent_level: u8,
    pub linked_note_id: Option<u32>,
}

#[frb(non_opaque)]
#[derive(Clone)]
pub struct NoteDetail {
    pub id: u32,
    pub path: String,
    pub title: String,
    pub author: Option<String>,
    pub author_url: Option<String>,
    pub url: Option<String>,
    pub published: Option<String>,
    pub created: Option<String>,
    pub updated: Option<String>,
    pub tags: Vec<String>,
    pub body_text: String,
    pub memo_lines: Vec<DetailLine>,
    pub related_lines: Vec<DetailLine>,
    pub media: Vec<MediaItem>,
}

fn open_gallery(vault_path: &str, index_path: &str) -> Result<Gallery, String> {
    Gallery::open(Path::new(vault_path), Path::new(index_path)).map_err(error_message)
}

fn error_message(error: CoreError) -> String {
    error.to_string()
}

pub fn prepare_app_data_directory(
    directory_path: String,
    vault_path: String,
) -> Result<(), String> {
    prepare_private_app_directory(Path::new(&directory_path), Path::new(&vault_path))
        .map_err(error_message)
}

pub fn load_vault_path(directory_path: String) -> Result<Option<String>, String> {
    load_selected_vault(Path::new(&directory_path)).map_err(error_message)
}

pub fn save_vault_path(directory_path: String, vault_path: String) -> Result<String, String> {
    save_selected_vault(Path::new(&directory_path), Path::new(&vault_path)).map_err(error_message)
}

pub fn scan(vault_path: String, index_path: String) -> Result<ScanReport, String> {
    open_gallery(&vault_path, &index_path)?
        .scan()
        .map(scan_report)
        .map_err(error_message)
}

pub fn list_categories(
    vault_path: String,
    index_path: String,
    filters: Vec<String>,
    excluded_filters: Vec<String>,
    virtual_filters: Vec<String>,
) -> Result<Vec<Category>, String> {
    let virtual_filters = parse_virtual_filters(virtual_filters)?;
    let (filters, all_filters) = split_filter_modes(filters);
    open_gallery(&vault_path, &index_path)?
        .categories_with_filter_modes(&filters, &all_filters, &excluded_filters, &virtual_filters)
        .map(|categories| categories.into_iter().map(category).collect())
        .map_err(error_message)
}

#[allow(clippy::too_many_arguments)]
pub fn query_notes(
    vault_path: String,
    index_path: String,
    filters: Vec<String>,
    excluded_filters: Vec<String>,
    virtual_filters: Vec<String>,
    search_query: String,
    offset: u32,
    limit: u32,
) -> Result<Vec<NoteSummary>, String> {
    let virtual_filters = parse_virtual_filters(virtual_filters)?;
    let (filters, all_filters) = split_filter_modes(filters);
    open_gallery(&vault_path, &index_path)?
        .query_filtered_page_search_with_all(
            &filters,
            &all_filters,
            &excluded_filters,
            &virtual_filters,
            &search_query,
            offset as usize,
            limit as usize,
        )
        .map(|notes| notes.into_iter().map(note_summary).collect())
        .map_err(error_message)
}

pub fn count_notes(
    vault_path: String,
    index_path: String,
    filters: Vec<String>,
    excluded_filters: Vec<String>,
    virtual_filters: Vec<String>,
    search_query: String,
) -> Result<u32, String> {
    let virtual_filters = parse_virtual_filters(virtual_filters)?;
    let (filters, all_filters) = split_filter_modes(filters);
    open_gallery(&vault_path, &index_path)?
        .count_filtered_notes_with_all(
            &filters,
            &all_filters,
            &excluded_filters,
            &virtual_filters,
            &search_query,
        )
        .map(|count| count.min(u32::MAX as usize) as u32)
        .map_err(error_message)
}

#[allow(clippy::too_many_arguments)]
pub fn query_media(
    vault_path: String,
    index_path: String,
    filters: Vec<String>,
    excluded_filters: Vec<String>,
    virtual_filters: Vec<String>,
    search_query: String,
    offset: u32,
    limit: u32,
) -> Result<Vec<MediaItem>, String> {
    let virtual_filters = parse_virtual_filters(virtual_filters)?;
    let (filters, all_filters) = split_filter_modes(filters);
    open_gallery(&vault_path, &index_path)?
        .query_media_filtered_page_search_with_all(
            &filters,
            &all_filters,
            &excluded_filters,
            &virtual_filters,
            &search_query,
            offset as usize,
            limit as usize,
        )
        .map(|items| items.into_iter().map(media_item).collect())
        .map_err(error_message)
}

pub fn count_media(
    vault_path: String,
    index_path: String,
    filters: Vec<String>,
    excluded_filters: Vec<String>,
    virtual_filters: Vec<String>,
    search_query: String,
) -> Result<u32, String> {
    let virtual_filters = parse_virtual_filters(virtual_filters)?;
    let (filters, all_filters) = split_filter_modes(filters);
    open_gallery(&vault_path, &index_path)?
        .count_filtered_media_with_all(
            &filters,
            &all_filters,
            &excluded_filters,
            &virtual_filters,
            &search_query,
        )
        .map(|count| count.min(u32::MAX as usize) as u32)
        .map_err(error_message)
}

const ALL_FILTER_PREFIX: &str = "\u{1f}AND:";

fn split_filter_modes(filters: Vec<String>) -> (Vec<String>, Vec<String>) {
    let mut any_filters = Vec::new();
    let mut all_filters = Vec::new();
    for filter in filters {
        if let Some(tag) = filter.strip_prefix(ALL_FILTER_PREFIX) {
            all_filters.push(tag.to_owned());
        } else {
            any_filters.push(filter);
        }
    }
    (any_filters, all_filters)
}

fn scan_report(report: CoreScanReport) -> ScanReport {
    ScanReport {
        notes_indexed: report.notes_indexed.min(u32::MAX as usize) as u32,
        warnings: report.warnings.min(u32::MAX as usize) as u32,
    }
}

fn category(category: CoreCategory) -> Category {
    Category {
        path: category.path,
        display_name: category.display_name,
        options: category
            .options
            .into_iter()
            .map(|option| CategoryOption {
                name: option.name,
                full_tag: option.full_tag,
                count: option.count.min(u32::MAX as usize) as u32,
                disabled: option.disabled,
                virtual_filter: option.virtual_filter.map(virtual_filter_key),
            })
            .collect(),
        count: category.count.min(u32::MAX as usize) as u32,
    }
}

fn parse_virtual_filters(filters: Vec<String>) -> Result<Vec<CoreVirtualFilter>, String> {
    filters
        .into_iter()
        .map(|filter| match filter.as_str() {
            "multiple_media" => Ok(CoreVirtualFilter::MultipleMedia),
            "has_memo" => Ok(CoreVirtualFilter::HasMemo),
            "has_video" => Ok(CoreVirtualFilter::HasVideo),
            "has_related" => Ok(CoreVirtualFilter::HasRelated),
            _ => Err("unknown virtual filter".to_owned()),
        })
        .collect()
}

fn virtual_filter_key(filter: CoreVirtualFilter) -> String {
    match filter {
        CoreVirtualFilter::MultipleMedia => "multiple_media",
        CoreVirtualFilter::HasMemo => "has_memo",
        CoreVirtualFilter::HasVideo => "has_video",
        CoreVirtualFilter::HasRelated => "has_related",
    }
    .to_owned()
}

fn note_summary(note: CoreNoteSummary) -> NoteSummary {
    NoteSummary {
        id: note.id.min(u32::MAX as i64) as u32,
        path: note.path,
        title: note.title,
        media_count: note.media_count.min(u32::MAX as usize) as u32,
        video_count: note.video_count.min(u32::MAX as usize) as u32,
        memo_count: note.memo_count.min(u32::MAX as usize) as u32,
        related_count: note.related_count.min(u32::MAX as usize) as u32,
        representative_media_id: note
            .representative_media_id
            .map(|id| id.min(u32::MAX as i64) as u32),
    }
}

fn media_item(item: CoreMediaSummary) -> MediaItem {
    MediaItem {
        id: item.id.min(u32::MAX as i64) as u32,
        note_id: item.note_id.min(u32::MAX as i64) as u32,
        is_video: item.is_video,
        exists: item.exists,
        media_count: item.media_count.min(u32::MAX as usize) as u32,
        memo_count: item.memo_count.min(u32::MAX as usize) as u32,
        related_count: item.related_count.min(u32::MAX as usize) as u32,
    }
}

fn detail_line(line: CoreDetailLine) -> DetailLine {
    DetailLine {
        text: line.text,
        urls: line.urls,
        is_bullet: line.is_bullet,
        indent_level: line.indent_level,
        linked_note_id: line
            .linked_note_id
            .map(|note_id| note_id.min(u32::MAX as i64) as u32),
    }
}

fn note_detail(note: CoreNoteDetail) -> NoteDetail {
    NoteDetail {
        id: note.id.min(u32::MAX as i64) as u32,
        path: note.path,
        title: note.title,
        author: note.author,
        author_url: note.author_url,
        url: note.url,
        published: note.published,
        created: note.created,
        updated: note.updated,
        tags: note.tags,
        body_text: note.body_text,
        memo_lines: note.memo_lines.into_iter().map(detail_line).collect(),
        related_lines: note.related_lines.into_iter().map(detail_line).collect(),
        media: note.media.into_iter().map(media_item).collect(),
    }
}

pub fn get_note_detail(
    vault_path: String,
    index_path: String,
    note_id: u32,
) -> Result<Option<NoteDetail>, String> {
    open_gallery(&vault_path, &index_path)?
        .note_detail(i64::from(note_id))
        .map(|note| note.map(note_detail))
        .map_err(error_message)
}

pub fn get_media_source_path(
    vault_path: String,
    index_path: String,
    media_id: u32,
) -> Result<Option<String>, String> {
    let path = open_gallery(&vault_path, &index_path)?
        .media_source_path(i64::from(media_id))
        .map_err(error_message)?;
    path.map(|path| {
        path.into_os_string()
            .into_string()
            .map_err(|_| error_message(CoreError::Io))
    })
    .transpose()
}

pub fn warning_count(vault_path: String, index_path: String) -> Result<u32, String> {
    open_gallery(&vault_path, &index_path)?
        .warning_count()
        .map(|count| count.min(u32::MAX as usize) as u32)
        .map_err(error_message)
}

pub fn get_thumbnail(
    vault_path: String,
    index_path: String,
    cache_path: String,
    media_id: u32,
    size: u32,
) -> Result<Option<Vec<u8>>, String> {
    open_gallery(&vault_path, &index_path)?
        .get_thumbnail(i64::from(media_id), size, Path::new(&cache_path))
        .map_err(error_message)
}

pub fn get_video_source_path(
    vault_path: String,
    index_path: String,
    media_id: u32,
) -> Result<Option<String>, String> {
    let path = open_gallery(&vault_path, &index_path)?
        .video_source_path(i64::from(media_id))
        .map_err(error_message)?;
    path.map(|path| {
        path.into_os_string()
            .into_string()
            .map_err(|_| error_message(CoreError::Io))
    })
    .transpose()
}

#[cfg(test)]
mod tests {
    use super::{ALL_FILTER_PREFIX, split_filter_modes};

    #[test]
    fn separates_and_filters_from_any_filters() {
        let (any_filters, all_filters) = split_filter_modes(vec![
            "copyright/pretty-series".to_owned(),
            format!("{ALL_FILTER_PREFIX}copyright/onepeace"),
        ]);
        assert_eq!(any_filters, ["copyright/pretty-series"]);
        assert_eq!(all_filters, ["copyright/onepeace"]);
    }
}
