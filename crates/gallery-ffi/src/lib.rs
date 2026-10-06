use gallery_core::{
    Category as CoreCategory, CoreError, DetailLine as CoreDetailLine, Gallery,
    MediaSummary as CoreMediaSummary, NoteDetail as CoreNoteDetail, NoteSort, NoteSortField,
    NoteSummary as CoreNoteSummary, SafNoteDocument, ScanReport as CoreScanReport,
    SortDirection as CoreSortDirection, VirtualFilter as CoreVirtualFilter, forget_selected_vault,
    load_selected_vault, prepare_private_app_directory, prepare_private_app_directory_for_saf,
    save_selected_vault, save_selected_vault_saf,
};
use std::fs::{self, File, OpenOptions};
use std::io::{BufReader, BufWriter, Read, Write};
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use thiserror::Error;

const MAX_SAF_DOCUMENTS: usize = 100_000;
const MAX_SAF_DEPTH: usize = 64;
const MAX_SAF_PATH_BYTES: usize = 4_096;
const MAX_SAF_AGGREGATE_PATH_BYTES: usize = 32 * 1024 * 1024;
const MAX_SAF_NOTE_BYTES: usize = 2 * 1024 * 1024;
const MAX_SAF_BATCH_NOTES: usize = 128;
const MAX_SAF_BATCH_BYTES: usize = 16 * 1024 * 1024;
const MAX_SAF_SCAN_BYTES: usize = 128 * 1024 * 1024;
static NEXT_SCAN_FILE: AtomicU64 = AtomicU64::new(0);

#[derive(Debug, Clone, Error, uniffi::Error)]
pub enum GalleryError {
    #[error("Vault unavailable")]
    VaultUnavailable,
    #[error("Invalid Vault")]
    InvalidVault,
    #[error("Private storage unavailable")]
    StorageUnavailable,
    #[error("Index unavailable")]
    IndexUnavailable,
    #[error("Operation failed")]
    OperationFailed,
    #[error("Invalid request")]
    InvalidRequest,
}

impl From<CoreError> for GalleryError {
    fn from(error: CoreError) -> Self {
        match error {
            CoreError::VaultUnavailable => Self::VaultUnavailable,
            CoreError::InvalidVault => Self::InvalidVault,
            CoreError::Io => Self::StorageUnavailable,
            CoreError::Database => Self::IndexUnavailable,
            CoreError::Worker => Self::OperationFailed,
        }
    }
}

#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum GalleryContent {
    Notes,
    Media,
}

#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum GallerySortField {
    Published,
    Created,
}

#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum GallerySortDirection {
    Ascending,
    Descending,
}

#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum VirtualFilter {
    MultipleMedia,
    HasMemo,
    HasVideo,
    HasRelated,
}

impl From<VirtualFilter> for CoreVirtualFilter {
    fn from(filter: VirtualFilter) -> Self {
        match filter {
            VirtualFilter::MultipleMedia => Self::MultipleMedia,
            VirtualFilter::HasMemo => Self::HasMemo,
            VirtualFilter::HasVideo => Self::HasVideo,
            VirtualFilter::HasRelated => Self::HasRelated,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct GalleryQuery {
    pub content: GalleryContent,
    pub search_text: String,
    pub include_tags: Vec<String>,
    pub and_tags: Vec<String>,
    pub excluded_tags: Vec<String>,
    pub virtual_filters: Vec<VirtualFilter>,
    pub sort_field: GallerySortField,
    pub sort_direction: GallerySortDirection,
    pub offset: u64,
    pub page_size: u32,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct NoteSummary {
    pub id: i64,
    pub path: String,
    pub title: String,
    pub media_count: u64,
    pub video_count: u64,
    pub memo_count: u64,
    pub related_count: u64,
    pub representative_media_id: Option<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct MediaSummary {
    pub id: i64,
    pub note_id: i64,
    pub note_path: String,
    pub is_video: bool,
    pub exists: bool,
    pub media_count: u64,
    pub memo_count: u64,
    pub related_count: u64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct GalleryPage {
    pub notes: Vec<NoteSummary>,
    pub media: Vec<MediaSummary>,
    pub total_count: u64,
    pub offset: u64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct ScanReport {
    pub notes_indexed: u64,
    pub warnings: u64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SafNote {
    pub path: String,
    pub modified_nanos: i64,
    pub size: i64,
    pub content: Option<Vec<u8>>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct CategoryOption {
    pub name: String,
    pub full_tag: String,
    pub count: u64,
    pub disabled: bool,
    pub virtual_filter: Option<VirtualFilter>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Category {
    pub path: String,
    pub display_name: String,
    pub options: Vec<CategoryOption>,
    pub count: u64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct DetailLine {
    pub text: String,
    pub urls: Vec<String>,
    pub is_bullet: bool,
    pub indent_level: u8,
    pub linked_note_id: Option<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct NoteDetail {
    pub id: i64,
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
    pub media: Vec<MediaSummary>,
}

#[derive(uniffi::Object)]
pub struct GallerySession {
    gallery: Mutex<Gallery>,
    vault_path: String,
    cache_path: String,
    saf_scan: Mutex<Option<SafScanStage>>,
}

struct SafScanStage {
    path: std::path::PathBuf,
    output: Option<BufWriter<File>>,
    file_paths: Vec<String>,
    document_count: usize,
    note_bytes: usize,
    path_bytes: usize,
}

impl SafScanStage {
    fn cleanup(&mut self) -> Result<(), GalleryError> {
        self.output.take();
        match fs::remove_file(&self.path) {
            Ok(()) => Ok(()),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(()),
            Err(_) => Err(GalleryError::StorageUnavailable),
        }
    }
}

impl Drop for SafScanStage {
    fn drop(&mut self) {
        self.output.take();
        let _ = fs::remove_file(&self.path);
    }
}

struct SafNoteReader<R> {
    reader: Option<R>,
    staged_path: PathBuf,
    finished: bool,
}

impl<R: Read> SafNoteReader<R> {
    fn new(reader: R, staged_path: PathBuf) -> Self {
        Self {
            reader: Some(reader),
            staged_path,
            finished: false,
        }
    }

    fn read(&mut self, buffer: &mut [u8]) -> Result<usize, CoreError> {
        self.reader
            .as_mut()
            .ok_or(CoreError::Io)?
            .read(buffer)
            .map_err(|_| CoreError::Io)
    }

    fn read_exact(&mut self, buffer: &mut [u8]) -> Result<(), CoreError> {
        self.reader
            .as_mut()
            .ok_or(CoreError::Io)?
            .read_exact(buffer)
            .map_err(|_| CoreError::Io)
    }

    fn remove_staged_file(&mut self) -> Result<(), CoreError> {
        self.reader.take();
        match fs::remove_file(&self.staged_path) {
            Ok(()) => Ok(()),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(()),
            Err(_) => Err(CoreError::Io),
        }
    }

    fn read_u32(&mut self) -> Result<u32, CoreError> {
        let mut bytes = [0_u8; 4];
        self.read_exact(&mut bytes)?;
        Ok(u32::from_le_bytes(bytes))
    }
}

impl<R: Read> Iterator for SafNoteReader<R> {
    type Item = Result<SafNoteDocument, CoreError>;

    fn next(&mut self) -> Option<Self::Item> {
        if self.finished {
            return None;
        }
        let mut path_length = [0_u8; 4];
        match self.read(&mut path_length[..1]) {
            Ok(0) => {
                self.finished = true;
                return self.remove_staged_file().err().map(Err);
            }
            Ok(1) => match self.read_exact(&mut path_length[1..]) {
                Ok(()) => {}
                Err(error) => {
                    self.finished = true;
                    return Some(Err(error));
                }
            },
            Err(error) => {
                self.finished = true;
                return Some(Err(error));
            }
            Ok(_) => {
                self.finished = true;
                return Some(Err(CoreError::Io));
            }
        }
        let result = (|| {
            let path_length = u32::from_le_bytes(path_length) as usize;
            if path_length == 0 || path_length > MAX_SAF_PATH_BYTES {
                return Err(CoreError::Io);
            }
            let mut path = vec![0; path_length];
            self.read_exact(&mut path)?;
            let path = String::from_utf8(path).map_err(|_| CoreError::Io)?;
            let mut modified = [0_u8; 8];
            let mut size = [0_u8; 8];
            self.read_exact(&mut modified)?;
            self.read_exact(&mut size)?;
            let content_length = self.read_u32()?;
            let content = if content_length == u32::MAX {
                None
            } else {
                let content_length = content_length as usize;
                if content_length > MAX_SAF_NOTE_BYTES {
                    return Err(CoreError::Io);
                }
                let mut content = vec![0; content_length];
                self.read_exact(&mut content)?;
                Some(content)
            };
            Ok(SafNoteDocument {
                path,
                modified_nanos: i64::from_le_bytes(modified),
                size: i64::from_le_bytes(size),
                content,
            })
        })();
        if result.is_err() {
            self.finished = true;
        }
        Some(result)
    }
}

fn validate_saf_relative_path(path: &str) -> Result<(), GalleryError> {
    if path.is_empty()
        || path.len() > MAX_SAF_PATH_BYTES
        || path.starts_with('/')
        || path.contains('\\')
        || path.contains('\0')
        || path.split('/').count() > MAX_SAF_DEPTH
        || path
            .split('/')
            .any(|segment| segment.is_empty() || segment == "." || segment == "..")
    {
        return Err(GalleryError::InvalidRequest);
    }
    Ok(())
}

fn write_saf_note<W: Write>(writer: &mut W, note: SafNote) -> std::io::Result<()> {
    writer.write_all(&(note.path.len() as u32).to_le_bytes())?;
    writer.write_all(note.path.as_bytes())?;
    writer.write_all(&note.modified_nanos.to_le_bytes())?;
    writer.write_all(&note.size.to_le_bytes())?;
    match note.content {
        Some(content) => {
            writer.write_all(&(content.len() as u32).to_le_bytes())?;
            writer.write_all(&content)?;
        }
        None => writer.write_all(&u32::MAX.to_le_bytes())?,
    }
    Ok(())
}

#[uniffi::export]
impl GallerySession {
    #[uniffi::constructor]
    pub fn open(
        vault_path: String,
        index_path: String,
        cache_path: String,
    ) -> Result<Arc<Self>, GalleryError> {
        let gallery = if vault_path.starts_with("content://") {
            Gallery::open_saf(&vault_path, Path::new(&index_path))
        } else {
            Gallery::open(Path::new(&vault_path), Path::new(&index_path))
        }
        .map_err(GalleryError::from)?;
        Ok(Arc::new(Self {
            gallery: Mutex::new(gallery),
            vault_path,
            cache_path,
            saf_scan: Mutex::new(None),
        }))
    }

    pub fn scan(&self) -> Result<ScanReport, GalleryError> {
        let mut gallery = self
            .gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        gallery
            .scan()
            .map(to_scan_report)
            .map_err(GalleryError::from)
    }

    pub fn scan_saf(
        &self,
        notes: Vec<SafNote>,
        file_paths: Vec<String>,
    ) -> Result<ScanReport, GalleryError> {
        let documents = notes
            .into_iter()
            .map(|note| SafNoteDocument {
                path: note.path,
                modified_nanos: note.modified_nanos,
                size: note.size,
                content: note.content,
            })
            .collect();
        let mut gallery = self
            .gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        gallery
            .scan_saf(documents, file_paths)
            .map(to_scan_report)
            .map_err(GalleryError::from)
    }

    pub fn begin_saf_scan(&self, file_paths: Vec<String>) -> Result<(), GalleryError> {
        if !self.vault_path.starts_with("content://") {
            return Err(GalleryError::InvalidRequest);
        }
        let mut scan = self
            .saf_scan
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        if scan.is_some() {
            return Err(GalleryError::InvalidRequest);
        }
        if file_paths.len() > MAX_SAF_DOCUMENTS {
            return Err(GalleryError::InvalidRequest);
        }
        let mut path_bytes = 0_usize;
        for path in &file_paths {
            validate_saf_relative_path(path)?;
            path_bytes = path_bytes
                .checked_add(path.len())
                .ok_or(GalleryError::InvalidRequest)?;
            if path_bytes > MAX_SAF_AGGREGATE_PATH_BYTES {
                return Err(GalleryError::InvalidRequest);
            }
        }
        let cache_path = Path::new(&self.cache_path);
        let parent = cache_path
            .parent()
            .ok_or(GalleryError::StorageUnavailable)?;
        let parent = fs::canonicalize(parent).map_err(|_| GalleryError::StorageUnavailable)?;
        if !parent.is_dir() {
            return Err(GalleryError::StorageUnavailable);
        }
        let mut file = None;
        let mut path = None;
        for _ in 0..16 {
            let id = NEXT_SCAN_FILE.fetch_add(1, Ordering::Relaxed);
            let candidate = parent.join(format!(".kaede-saf-scan-{}-{id}.tmp", std::process::id()));
            let mut options = OpenOptions::new();
            options.write(true).read(true).create_new(true);
            #[cfg(unix)]
            {
                use std::os::unix::fs::OpenOptionsExt;
                options.mode(0o600);
            }
            match options.open(&candidate) {
                Ok(opened) => {
                    file = Some(opened);
                    path = Some(candidate);
                    break;
                }
                Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => continue,
                Err(_) => return Err(GalleryError::StorageUnavailable),
            }
        }
        let file = file.ok_or(GalleryError::StorageUnavailable)?;
        *scan = Some(SafScanStage {
            path: path.ok_or(GalleryError::StorageUnavailable)?,
            output: Some(BufWriter::new(file)),
            file_paths,
            document_count: 0,
            note_bytes: 0,
            path_bytes,
        });
        Ok(())
    }

    pub fn append_saf_scan_batch(&self, notes: Vec<SafNote>) -> Result<(), GalleryError> {
        let mut scan = self
            .saf_scan
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        let stage = scan.as_mut().ok_or(GalleryError::InvalidRequest)?;
        if notes.len() > MAX_SAF_BATCH_NOTES {
            return Err(GalleryError::InvalidRequest);
        }
        let mut batch_bytes = 0_usize;
        for note in &notes {
            validate_saf_relative_path(&note.path)?;
            if note.modified_nanos < 0 || note.size < 0 {
                return Err(GalleryError::InvalidRequest);
            }
            let content_bytes = note.content.as_ref().map_or(0, Vec::len);
            if content_bytes > MAX_SAF_NOTE_BYTES
                || (note.size > 0
                    && note.content.as_ref().is_some_and(|content| {
                        usize::try_from(note.size).ok() != Some(content.len())
                    }))
            {
                return Err(GalleryError::InvalidRequest);
            }
            batch_bytes = batch_bytes
                .checked_add(content_bytes)
                .ok_or(GalleryError::InvalidRequest)?;
            if batch_bytes > MAX_SAF_BATCH_BYTES {
                return Err(GalleryError::InvalidRequest);
            }
        }
        let new_count = stage
            .document_count
            .checked_add(notes.len())
            .ok_or(GalleryError::InvalidRequest)?;
        let new_note_bytes = stage
            .note_bytes
            .checked_add(batch_bytes)
            .ok_or(GalleryError::InvalidRequest)?;
        let new_path_bytes = notes.iter().try_fold(stage.path_bytes, |total, note| {
            total.checked_add(note.path.len())
        });
        if new_count > MAX_SAF_DOCUMENTS
            || new_note_bytes > MAX_SAF_SCAN_BYTES
            || new_path_bytes.is_none_or(|bytes| bytes > MAX_SAF_AGGREGATE_PATH_BYTES)
        {
            return Err(GalleryError::InvalidRequest);
        }
        let output = stage.output.as_mut().ok_or(GalleryError::OperationFailed)?;
        for note in notes {
            write_saf_note(output, note).map_err(|_| GalleryError::StorageUnavailable)?;
        }
        stage.document_count = new_count;
        stage.note_bytes = new_note_bytes;
        stage.path_bytes = new_path_bytes.ok_or(GalleryError::InvalidRequest)?;
        Ok(())
    }

    pub fn finish_saf_scan(&self) -> Result<ScanReport, GalleryError> {
        if !self.vault_path.starts_with("content://") {
            return Err(GalleryError::InvalidRequest);
        }
        let mut stage = self
            .saf_scan
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?
            .take()
            .ok_or(GalleryError::InvalidRequest)?;
        let path = stage.path.clone();
        let file_paths = std::mem::take(&mut stage.file_paths);
        if let Some(mut output) = stage.output.take() {
            output
                .flush()
                .map_err(|_| GalleryError::StorageUnavailable)?;
            output
                .get_ref()
                .sync_all()
                .map_err(|_| GalleryError::StorageUnavailable)?;
        }
        let input = File::open(&path).map_err(|_| GalleryError::StorageUnavailable)?;
        let mut gallery = self
            .gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        let result = gallery
            .scan_saf_iter(file_paths, SafNoteReader::new(BufReader::new(input), path))
            .map(to_scan_report)
            .map_err(GalleryError::from);
        drop(gallery);
        drop(stage);
        result
    }

    pub fn cancel_saf_scan(&self) -> Result<(), GalleryError> {
        let mut scan = self
            .saf_scan
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        if let Some(mut stage) = scan.take() {
            stage.cleanup()?;
        }
        Ok(())
    }

    pub fn query_page(&self, query: GalleryQuery) -> Result<GalleryPage, GalleryError> {
        let offset = usize::try_from(query.offset).map_err(|_| GalleryError::InvalidRequest)?;
        let limit = usize::try_from(query.page_size).map_err(|_| GalleryError::InvalidRequest)?;
        if limit == 0 || limit > 500 {
            return Err(GalleryError::InvalidRequest);
        }
        let virtual_filters = query
            .virtual_filters
            .iter()
            .copied()
            .map(CoreVirtualFilter::from)
            .collect::<Vec<_>>();
        let sort = NoteSort {
            field: match query.sort_field {
                GallerySortField::Published => NoteSortField::Published,
                GallerySortField::Created => NoteSortField::Created,
            },
            direction: match query.sort_direction {
                GallerySortDirection::Ascending => CoreSortDirection::Ascending,
                GallerySortDirection::Descending => CoreSortDirection::Descending,
            },
        };
        let gallery = self
            .gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        let (notes, media, count) = match query.content {
            GalleryContent::Notes => {
                let notes = gallery
                    .query_filtered_page_search_with_all_and_sort(
                        &query.include_tags,
                        &query.and_tags,
                        &query.excluded_tags,
                        &virtual_filters,
                        &query.search_text,
                        sort,
                        offset,
                        limit,
                    )
                    .map_err(GalleryError::from)?
                    .into_iter()
                    .map(to_note_summary)
                    .collect();
                let count = gallery
                    .count_filtered_notes_with_all(
                        &query.include_tags,
                        &query.and_tags,
                        &query.excluded_tags,
                        &virtual_filters,
                        &query.search_text,
                    )
                    .map_err(GalleryError::from)?;
                (notes, Vec::new(), count)
            }
            GalleryContent::Media => {
                let media = gallery
                    .query_media_filtered_page_search_with_all_and_sort(
                        &query.include_tags,
                        &query.and_tags,
                        &query.excluded_tags,
                        &virtual_filters,
                        &query.search_text,
                        sort,
                        offset,
                        limit,
                    )
                    .map_err(GalleryError::from)?
                    .into_iter()
                    .map(to_media_summary)
                    .collect();
                let count = gallery
                    .count_filtered_media_with_all(
                        &query.include_tags,
                        &query.and_tags,
                        &query.excluded_tags,
                        &virtual_filters,
                        &query.search_text,
                    )
                    .map_err(GalleryError::from)?;
                (Vec::new(), media, count)
            }
        };
        Ok(GalleryPage {
            notes,
            media,
            total_count: count as u64,
            offset: query.offset,
        })
    }

    pub fn categories(&self, query: GalleryQuery) -> Result<Vec<Category>, GalleryError> {
        let virtual_filters = query
            .virtual_filters
            .iter()
            .copied()
            .map(CoreVirtualFilter::from)
            .collect::<Vec<_>>();
        self.gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?
            .categories_with_filter_modes(
                &query.include_tags,
                &query.and_tags,
                &query.excluded_tags,
                &virtual_filters,
            )
            .map(|categories| categories.into_iter().map(to_category).collect())
            .map_err(GalleryError::from)
    }

    pub fn note_detail(&self, note_id: i64) -> Result<Option<NoteDetail>, GalleryError> {
        self.gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?
            .note_detail(note_id)
            .map(|detail| detail.map(to_note_detail))
            .map_err(GalleryError::from)
    }

    pub fn note_detail_saf(
        &self,
        note_id: i64,
        content: Vec<u8>,
    ) -> Result<Option<NoteDetail>, GalleryError> {
        if content.len() > gallery_parse::MAX_NOTE_BYTES {
            return Err(GalleryError::InvalidRequest);
        }
        self.gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?
            .note_detail_saf(note_id, &content)
            .map(|detail| detail.map(to_note_detail))
            .map_err(GalleryError::from)
    }

    pub fn media_location(&self, media_id: i64) -> Result<Option<String>, GalleryError> {
        let gallery = self
            .gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?;
        if self.vault_path.starts_with("content://") {
            return gallery
                .media_relative_path(media_id)
                .map_err(GalleryError::from);
        }
        gallery
            .media_source_path(media_id)
            .map_err(GalleryError::from)?
            .map(|path| {
                path.into_os_string()
                    .into_string()
                    .map_err(|_| GalleryError::OperationFailed)
            })
            .transpose()
    }

    pub fn thumbnail(&self, media_id: i64, size: u32) -> Result<Option<Vec<u8>>, GalleryError> {
        self.gallery
            .lock()
            .map_err(|_| GalleryError::OperationFailed)?
            .get_thumbnail(media_id, size, Path::new(&self.cache_path))
            .map_err(GalleryError::from)
    }
}

#[uniffi::export]
pub fn prepare_private_data(
    directory_path: String,
    vault_path: String,
) -> Result<(), GalleryError> {
    if vault_path.starts_with("content://") {
        prepare_private_app_directory_for_saf(Path::new(&directory_path))
    } else {
        prepare_private_app_directory(Path::new(&directory_path), Path::new(&vault_path))
    }
    .map_err(GalleryError::from)
}

#[uniffi::export]
pub fn load_selected_vault_path(directory_path: String) -> Result<Option<String>, GalleryError> {
    load_selected_vault(Path::new(&directory_path)).map_err(GalleryError::from)
}

#[uniffi::export]
pub fn save_selected_vault_path(
    directory_path: String,
    vault_path: String,
) -> Result<String, GalleryError> {
    if vault_path.starts_with("content://") {
        save_selected_vault_saf(Path::new(&directory_path), &vault_path)
    } else {
        save_selected_vault(Path::new(&directory_path), Path::new(&vault_path))
    }
    .map_err(GalleryError::from)
}

#[uniffi::export]
pub fn forget_selected_vault_data(
    directory_path: String,
    expected_vault_path: String,
) -> Result<(), GalleryError> {
    forget_selected_vault(Path::new(&directory_path), &expected_vault_path)
        .map_err(GalleryError::from)
}

fn to_scan_report(report: CoreScanReport) -> ScanReport {
    ScanReport {
        notes_indexed: report.notes_indexed as u64,
        warnings: report.warnings as u64,
    }
}

fn to_note_summary(note: CoreNoteSummary) -> NoteSummary {
    NoteSummary {
        id: note.id,
        path: note.path,
        title: note.title,
        media_count: note.media_count as u64,
        video_count: note.video_count as u64,
        memo_count: note.memo_count as u64,
        related_count: note.related_count as u64,
        representative_media_id: note.representative_media_id,
    }
}

fn to_media_summary(media: CoreMediaSummary) -> MediaSummary {
    MediaSummary {
        id: media.id,
        note_id: media.note_id,
        note_path: media.note_path,
        is_video: media.is_video,
        exists: media.exists,
        media_count: media.media_count as u64,
        memo_count: media.memo_count as u64,
        related_count: media.related_count as u64,
    }
}

fn to_category(category: CoreCategory) -> Category {
    Category {
        path: category.path,
        display_name: category.display_name,
        options: category
            .options
            .into_iter()
            .map(|option| CategoryOption {
                name: option.name,
                full_tag: option.full_tag,
                count: option.count as u64,
                disabled: option.disabled,
                virtual_filter: option.virtual_filter.map(|filter| match filter {
                    CoreVirtualFilter::MultipleMedia => VirtualFilter::MultipleMedia,
                    CoreVirtualFilter::HasMemo => VirtualFilter::HasMemo,
                    CoreVirtualFilter::HasVideo => VirtualFilter::HasVideo,
                    CoreVirtualFilter::HasRelated => VirtualFilter::HasRelated,
                }),
            })
            .collect(),
        count: category.count as u64,
    }
}

fn to_detail_line(line: CoreDetailLine) -> DetailLine {
    DetailLine {
        text: line.text,
        urls: line.urls,
        is_bullet: line.is_bullet,
        indent_level: line.indent_level,
        linked_note_id: line.linked_note_id,
    }
}

fn to_note_detail(note: CoreNoteDetail) -> NoteDetail {
    NoteDetail {
        id: note.id,
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
        memo_lines: note.memo_lines.into_iter().map(to_detail_line).collect(),
        related_lines: note.related_lines.into_iter().map(to_detail_line).collect(),
        media: note.media.into_iter().map(to_media_summary).collect(),
    }
}

uniffi::setup_scaffolding!();

#[cfg(test)]
mod tests {
    use super::{
        GalleryContent, GalleryQuery, GallerySession, GallerySortDirection, GallerySortField,
        SafNote,
    };
    use std::fs;
    use std::sync::atomic::{AtomicUsize, Ordering};

    static NEXT_TEST: AtomicUsize = AtomicUsize::new(0);

    #[test]
    fn scans_and_queries_a_fictional_vault_without_modifying_it() {
        let root = std::env::temp_dir().join(format!(
            "gallery-ffi-{}-{}",
            std::process::id(),
            NEXT_TEST.fetch_add(1, Ordering::Relaxed),
        ));
        let vault = root.join("vault");
        let private = root.join("private");
        fs::create_dir_all(&vault).expect("create fictional vault");
        fs::create_dir_all(&private).expect("create private app data");
        let note_path = vault.join("fictional-note.md");
        let note = "---\ntags: [source/art]\n---\n# Fictional Note\nA fictional caption.\n";
        fs::write(&note_path, note).expect("write fictional note");

        let session = GallerySession::open(
            vault.to_string_lossy().into_owned(),
            private.join("index.sqlite").to_string_lossy().into_owned(),
            private.join("thumbnails").to_string_lossy().into_owned(),
        )
        .expect("open fictional vault");
        let report = session.scan().expect("scan fictional vault");
        assert_eq!(report.notes_indexed, 1);
        assert_eq!(report.warnings, 0);

        let page = session
            .query_page(GalleryQuery {
                content: GalleryContent::Notes,
                search_text: "Fictional".to_owned(),
                include_tags: Vec::new(),
                and_tags: Vec::new(),
                excluded_tags: Vec::new(),
                virtual_filters: Vec::new(),
                sort_field: GallerySortField::Created,
                sort_direction: GallerySortDirection::Descending,
                offset: 0,
                page_size: 24,
            })
            .expect("query fictional note");
        assert_eq!(page.total_count, 1);
        assert_eq!(page.notes.len(), 1);
        assert_eq!(page.notes[0].title, "Fictional Note");
        assert_eq!(fs::read_to_string(note_path).expect("read fixture"), note);

        drop(session);
        fs::remove_dir_all(root).expect("remove fictional fixture");
    }

    #[test]
    fn rejects_unbounded_pages_before_querying() {
        let root = std::env::temp_dir().join(format!(
            "gallery-ffi-invalid-{}-{}",
            std::process::id(),
            NEXT_TEST.fetch_add(1, Ordering::Relaxed),
        ));
        let vault = root.join("vault");
        let private = root.join("private");
        fs::create_dir_all(&vault).expect("create fictional vault");
        fs::create_dir_all(&private).expect("create private app data");
        let session = GallerySession::open(
            vault.to_string_lossy().into_owned(),
            private.join("index.sqlite").to_string_lossy().into_owned(),
            private.join("thumbnails").to_string_lossy().into_owned(),
        )
        .expect("open fictional vault");
        let result = session.query_page(GalleryQuery {
            content: GalleryContent::Notes,
            search_text: String::new(),
            include_tags: Vec::new(),
            and_tags: Vec::new(),
            excluded_tags: Vec::new(),
            virtual_filters: Vec::new(),
            sort_field: GallerySortField::Created,
            sort_direction: GallerySortDirection::Descending,
            offset: 0,
            page_size: 501,
        });
        assert!(matches!(result, Err(super::GalleryError::InvalidRequest)));

        drop(session);
        fs::remove_dir_all(root).expect("remove fictional fixture");
    }

    #[test]
    fn bounded_saf_batches_commit_only_when_scan_finishes() {
        let root = std::env::temp_dir().join(format!(
            "gallery-ffi-saf-{}-{}",
            std::process::id(),
            NEXT_TEST.fetch_add(1, Ordering::Relaxed),
        ));
        let private = root.join("private");
        fs::create_dir_all(&private).expect("create private app data");
        let vault_uri =
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FFictional";
        let session = GallerySession::open(
            vault_uri.to_owned(),
            private.join("index.sqlite").to_string_lossy().into_owned(),
            private.join("thumbnails").to_string_lossy().into_owned(),
        )
        .expect("open fictional SAF vault");
        let note = b"---\ntags: [source/art]\n---\n# Fictional SAF note\n";
        session
            .begin_saf_scan(vec!["fictional.md".to_owned()])
            .expect("begin bounded scan");
        session
            .append_saf_scan_batch(vec![SafNote {
                path: "fictional.md".to_owned(),
                modified_nanos: 1,
                size: note.len() as i64,
                content: Some(note.to_vec()),
            }])
            .expect("append bounded note batch");
        let report = session.finish_saf_scan().expect("commit completed scan");
        assert_eq!(report.notes_indexed, 1);
        assert!(
            fs::read_dir(&private)
                .expect("read private scan directory")
                .all(|entry| !entry
                    .expect("read scan staging entry")
                    .file_name()
                    .to_string_lossy()
                    .starts_with(".kaede-saf-scan-"))
        );

        session
            .begin_saf_scan(vec!["fictional.md".to_owned()])
            .expect("begin replacement scan");
        session
            .append_saf_scan_batch(vec![SafNote {
                path: "fictional.md".to_owned(),
                modified_nanos: 2,
                size: note.len() as i64,
                content: Some(note.to_vec()),
            }])
            .expect("stage replacement note");
        session.cancel_saf_scan().expect("cancel incomplete scan");
        assert!(
            fs::read_dir(&private)
                .expect("read private scan directory")
                .all(|entry| !entry
                    .expect("read scan staging entry")
                    .file_name()
                    .to_string_lossy()
                    .starts_with(".kaede-saf-scan-"))
        );
        let page = session
            .query_page(GalleryQuery {
                content: GalleryContent::Notes,
                search_text: "Fictional SAF".to_owned(),
                include_tags: Vec::new(),
                and_tags: Vec::new(),
                excluded_tags: Vec::new(),
                virtual_filters: Vec::new(),
                sort_field: GallerySortField::Created,
                sort_direction: GallerySortDirection::Descending,
                offset: 0,
                page_size: 24,
            })
            .expect("query last committed index");
        assert_eq!(page.total_count, 1);
        assert_eq!(page.notes[0].title, "Fictional SAF note");

        drop(session);
        fs::remove_dir_all(root).expect("remove fictional fixture");
    }

    #[test]
    fn bounded_saf_batches_reject_invalid_paths_and_oversized_batches() {
        let root = std::env::temp_dir().join(format!(
            "gallery-ffi-saf-limits-{}-{}",
            std::process::id(),
            NEXT_TEST.fetch_add(1, Ordering::Relaxed),
        ));
        let private = root.join("private");
        fs::create_dir_all(&private).expect("create private app data");
        let session = GallerySession::open(
            "content://com.android.externalstorage.documents/tree/primary%3AFictional".to_owned(),
            private.join("index.sqlite").to_string_lossy().into_owned(),
            private.join("thumbnails").to_string_lossy().into_owned(),
        )
        .expect("open fictional SAF vault");
        assert!(matches!(
            session.begin_saf_scan(vec!["../outside.md".to_owned()]),
            Err(super::GalleryError::InvalidRequest)
        ));
        session
            .begin_saf_scan(Vec::new())
            .expect("begin empty fictional scan");
        let oversized = (0..129)
            .map(|index| SafNote {
                path: format!("{index}.md"),
                modified_nanos: 0,
                size: 0,
                content: None,
            })
            .collect();
        assert!(matches!(
            session.append_saf_scan_batch(oversized),
            Err(super::GalleryError::InvalidRequest)
        ));
        session.cancel_saf_scan().expect("cancel rejected scan");

        drop(session);
        fs::remove_dir_all(root).expect("remove fictional fixture");
    }
}
