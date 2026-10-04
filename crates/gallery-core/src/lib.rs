#![forbid(unsafe_code)]

use gallery_parse::{
    InlineToken, LinkResolutionMode, MediaKind, NoteStructureSettings, ParsedNote,
    TagCategorySettings, expanded_tags, parse_note, parse_note_for_link_target,
    parse_note_structure_settings, parse_tag_category_settings,
};
use image::{ImageDecoder, ImageFormat, ImageReader, Limits};
use rusqlite::{Connection, OptionalExtension, Transaction, params};
use sha2::{Digest, Sha256};
use std::collections::{BTreeMap, BTreeSet};
use std::fs::{self, File, OpenOptions};
use std::io::{Cursor, Read, Write};
use std::path::{Component, Path, PathBuf};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, mpsc};
use std::thread;
use std::time::UNIX_EPOCH;
use thiserror::Error;

#[allow(dead_code)]
mod saf_limits;

use saf_limits::*;

const INDEX_SCHEMA_VERSION: &str = "5";
const MAX_THUMBNAIL_SOURCE_BYTES: u64 = 64 * 1024 * 1024;
const MAX_THUMBNAIL_PIXELS: u64 = 32 * 1024 * 1024;
const MAX_THUMBNAIL_DIMENSION: u32 = 16_384;
const MAX_THUMBNAIL_ALLOCATION: u64 = 192 * 1024 * 1024;
const MAX_THUMBNAIL_SIZE: u32 = 1_024;
static THUMBNAIL_LOCK: Mutex<()> = Mutex::new(());
const VIRTUAL_FILTERS: [(VirtualFilter, &str); 4] = [
    (VirtualFilter::MultipleMedia, "複数画像"),
    (VirtualFilter::HasMemo, "覚書あり"),
    (VirtualFilter::HasVideo, "動画あり"),
    (VirtualFilter::HasRelated, "関連あり"),
];

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VirtualFilter {
    MultipleMedia,
    HasMemo,
    HasVideo,
    HasRelated,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum NoteSortField {
    Published,
    Created,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SortDirection {
    Ascending,
    Descending,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct NoteSort {
    pub field: NoteSortField,
    pub direction: SortDirection,
}

impl Default for NoteSort {
    fn default() -> Self {
        Self {
            field: NoteSortField::Created,
            direction: SortDirection::Descending,
        }
    }
}

#[derive(Debug, Error)]
pub enum CoreError {
    #[error("the vault path is unavailable")]
    VaultUnavailable,
    #[error("the vault path is not a directory")]
    InvalidVault,
    #[error("the vault path could not be accessed")]
    Io,
    #[error("the index could not be read or updated")]
    Database,
    #[error("a bounded scan worker could not be started or completed")]
    Worker,
}

#[derive(Debug, Clone)]
pub struct ScanReport {
    pub notes_indexed: usize,
    pub warnings: usize,
}

#[derive(Debug, Clone)]
pub struct SafNoteDocument {
    pub path: String,
    pub modified_nanos: i64,
    pub size: i64,
    pub content: Option<Vec<u8>>,
}

#[derive(Debug, Clone)]
pub struct CategoryOption {
    pub name: String,
    pub full_tag: String,
    pub count: usize,
    pub disabled: bool,
    pub virtual_filter: Option<VirtualFilter>,
}

#[derive(Debug, Clone)]
pub struct Category {
    pub path: String,
    pub display_name: String,
    pub options: Vec<CategoryOption>,
    pub count: usize,
}

#[derive(Debug, Clone)]
pub struct NoteSummary {
    pub id: i64,
    pub path: String,
    pub title: String,
    pub media_count: usize,
    pub video_count: usize,
    pub memo_count: usize,
    pub related_count: usize,
    pub representative_media_id: Option<i64>,
}

#[derive(Debug, Clone)]
pub struct MediaSummary {
    pub id: i64,
    pub note_id: i64,
    pub is_video: bool,
    pub exists: bool,
    pub media_count: usize,
    pub memo_count: usize,
    pub related_count: usize,
}

#[derive(Debug, Clone)]
pub struct DetailLine {
    pub text: String,
    pub urls: Vec<String>,
    pub is_bullet: bool,
    pub indent_level: u8,
    pub linked_note_id: Option<i64>,
}

#[derive(Debug, Clone)]
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

struct ScanItem {
    path: PathBuf,
    relative: String,
    mtime: i64,
    size: i64,
}

struct NoteInsertContext<'a> {
    relative: &'a str,
    root: &'a Path,
    mtime: i64,
    size: i64,
    eligible: bool,
    available_files: Option<&'a BTreeSet<String>>,
}

pub struct Gallery {
    root: PathBuf,
    saf: bool,
    connection: Connection,
    tag_categories: TagCategorySettings,
    note_structure: NoteStructureSettings,
}

pub fn prepare_private_app_directory(directory: &Path, vault: &Path) -> Result<(), CoreError> {
    let vault_root = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
    let canonical = ensure_private_child_directory(directory, Some(&vault_root))?;
    set_private_directory(&canonical)
}

pub fn prepare_private_app_directory_for_saf(directory: &Path) -> Result<(), CoreError> {
    let canonical = ensure_private_child_directory(directory, None)?;
    set_private_directory(&canonical)
}

pub fn load_selected_vault(directory: &Path) -> Result<Option<String>, CoreError> {
    let path = directory.join("vault-path");
    let metadata = match fs::symlink_metadata(&path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(None),
        Err(_) => return Err(CoreError::Io),
    };
    if !metadata.file_type().is_file() || metadata.len() > 4_096 {
        return Err(CoreError::Io);
    }
    let contents = fs::read_to_string(path).map_err(|_| CoreError::Io)?;
    let value = contents.trim_end_matches(['\r', '\n']);
    if value.is_empty() || value.contains('\0') || value.len() > 4_096 {
        return Err(CoreError::Io);
    }
    Ok(Some(value.to_owned()))
}

pub fn save_selected_vault(directory: &Path, vault: &Path) -> Result<String, CoreError> {
    prepare_private_app_directory(directory, vault)?;
    let canonical_vault = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
    if !canonical_vault.is_dir() {
        return Err(CoreError::InvalidVault);
    }
    let value = canonical_vault.to_str().ok_or(CoreError::Io)?;
    if value.len() > 4_096 || value.contains('\0') {
        return Err(CoreError::Io);
    }
    save_selected_vault_value(directory, value)
}

fn save_selected_vault_value(directory: &Path, value: &str) -> Result<String, CoreError> {
    let path = directory.join("vault-path");
    let temporary = directory.join("vault-path.tmp");
    match fs::symlink_metadata(&temporary) {
        Ok(_) => fs::remove_file(&temporary).map_err(|_| CoreError::Io)?,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
        Err(_) => return Err(CoreError::Io),
    }
    let mut options = OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o600);
    }
    let mut file = options.open(&temporary).map_err(|_| CoreError::Io)?;
    file.write_all(value.as_bytes())
        .map_err(|_| CoreError::Io)?;
    file.sync_all().map_err(|_| CoreError::Io)?;
    fs::rename(&temporary, path).map_err(|_| CoreError::Io)?;
    Ok(value.to_owned())
}

pub fn save_selected_vault_saf(directory: &Path, vault_uri: &str) -> Result<String, CoreError> {
    if !vault_uri.starts_with("content://") || vault_uri.len() > 4_096 || vault_uri.contains('\0') {
        return Err(CoreError::InvalidVault);
    }
    prepare_private_app_directory_for_saf(directory)?;
    save_selected_vault_value(directory, vault_uri)
}

pub fn forget_selected_vault(directory: &Path, expected_vault: &str) -> Result<(), CoreError> {
    if expected_vault.is_empty() || expected_vault.len() > 4_096 || expected_vault.contains('\0') {
        return Err(CoreError::InvalidVault);
    }
    let saf = expected_vault.starts_with("content://");
    let selected = load_selected_vault(directory)?;
    if selected
        .as_deref()
        .is_some_and(|selected| selected != expected_vault)
    {
        return Err(CoreError::VaultUnavailable);
    }

    let metadata = fs::symlink_metadata(directory).map_err(|_| CoreError::Io)?;
    if !metadata.file_type().is_dir() {
        return Err(CoreError::Io);
    }
    let directory = fs::canonicalize(directory).map_err(|_| CoreError::Io)?;
    if !saf {
        let vault = Path::new(expected_vault);
        if !vault.is_absolute()
            || vault
                .components()
                .any(|component| matches!(component, Component::CurDir | Component::ParentDir))
        {
            return Err(CoreError::InvalidVault);
        }
        if directory.starts_with(vault) {
            return Err(CoreError::Io);
        }
        if let Ok(canonical_vault) = fs::canonicalize(vault)
            && canonical_vault.is_dir()
            && directory.starts_with(canonical_vault)
        {
            return Err(CoreError::Io);
        }
    }
    set_private_directory(&directory)?;

    let _guard = THUMBNAIL_LOCK.lock().map_err(|_| CoreError::Worker)?;
    for name in [
        "index.sqlite-journal",
        "index.sqlite-wal",
        "index.sqlite-shm",
        "index.sqlite",
        "saf-scan-cache.json.tmp",
        "saf-scan-cache.json",
        "vault-path.tmp",
    ] {
        remove_private_file(&directory.join(name))?;
    }
    remove_private_directory(&directory.join("thumbnails"), &directory)?;
    remove_private_file(&directory.join("vault-path"))?;
    Ok(())
}

fn remove_private_directory(path: &Path, parent: &Path) -> Result<(), CoreError> {
    let metadata = match fs::symlink_metadata(path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(()),
        Err(_) => return Err(CoreError::Io),
    };
    if !metadata.file_type().is_dir() {
        return Err(CoreError::Io);
    }
    let canonical = fs::canonicalize(path).map_err(|_| CoreError::Io)?;
    if canonical.parent() != Some(parent) {
        return Err(CoreError::Io);
    }
    fs::remove_dir_all(canonical).map_err(|_| CoreError::Io)
}

fn remove_private_file(path: &Path) -> Result<(), CoreError> {
    let metadata = match fs::symlink_metadata(path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(()),
        Err(_) => return Err(CoreError::Io),
    };
    if !metadata.file_type().is_file() {
        return Err(CoreError::Io);
    }
    #[cfg(unix)]
    {
        use std::os::unix::fs::MetadataExt;
        if metadata.nlink() != 1 {
            return Err(CoreError::Io);
        }
    }
    fs::remove_file(path).map_err(|_| CoreError::Io)
}

impl Gallery {
    pub fn open(vault: &Path, database: &Path) -> Result<Self, CoreError> {
        let root = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
        if !root.is_dir() {
            return Err(CoreError::InvalidVault);
        }
        let database_path = resolve_database_path(database, &root)?;
        Self::open_at(
            root.clone(),
            root.to_string_lossy().into_owned(),
            false,
            database_path,
        )
    }

    pub fn open_saf(vault_uri: &str, database: &Path) -> Result<Self, CoreError> {
        if !vault_uri.starts_with("content://") || vault_uri.len() > 4_096 {
            return Err(CoreError::InvalidVault);
        }
        let identity = format!("saf:{vault_uri}");
        let database_path = resolve_saf_database_path(database)?;
        Self::open_at(PathBuf::new(), identity, true, database_path)
    }

    fn open_at(
        root: PathBuf,
        vault_identity: String,
        saf: bool,
        database_path: PathBuf,
    ) -> Result<Self, CoreError> {
        let mut connection = Connection::open(&database_path).map_err(|_| CoreError::Database)?;
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            fs::set_permissions(&database_path, fs::Permissions::from_mode(0o600))
                .map_err(|_| CoreError::Database)?;
        }
        connection
            .execute_batch(
                "CREATE TABLE IF NOT EXISTS meta (
                   key TEXT PRIMARY KEY,
                   value TEXT NOT NULL
                 );",
            )
            .map_err(|_| CoreError::Database)?;
        let schema_version = connection
            .query_row(
                "SELECT value FROM meta WHERE key='schema_version'",
                [],
                |row| row.get::<_, String>(0),
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        if schema_version.as_deref() != Some(INDEX_SCHEMA_VERSION) {
            connection
                .execute_batch(
                    "DROP TABLE IF EXISTS note_tags;
                     DROP TABLE IF EXISTS media;
                     DROP TABLE IF EXISTS scan_state;
                     DROP TABLE IF EXISTS notes;
                     DROP TABLE IF EXISTS tags;
                     DROP TABLE IF EXISTS warnings;",
                )
                .map_err(|_| CoreError::Database)?;
        }
        connection
            .execute_batch(
                "PRAGMA foreign_keys = ON;
                 CREATE TABLE IF NOT EXISTS notes (
                   id INTEGER PRIMARY KEY,
                   path TEXT NOT NULL UNIQUE,
                   title TEXT NOT NULL,
                   url TEXT,
                   published TEXT,
                   created TEXT,
                   updated TEXT,
                   media_count INTEGER NOT NULL,
                   video_count INTEGER NOT NULL,
                   has_memo INTEGER NOT NULL,
                   has_related INTEGER NOT NULL,
                   memo_count INTEGER NOT NULL,
                   related_count INTEGER NOT NULL,
                   mtime INTEGER NOT NULL,
                   size INTEGER NOT NULL,
                   eligible INTEGER NOT NULL DEFAULT 1,
                   filename TEXT NOT NULL
                 );
                 CREATE TABLE IF NOT EXISTS scan_state (
                   path TEXT PRIMARY KEY,
                   mtime INTEGER NOT NULL,
                   size INTEGER NOT NULL
                 );
                 CREATE TABLE IF NOT EXISTS tags (
                   id INTEGER PRIMARY KEY,
                   name TEXT NOT NULL UNIQUE
                 );
                 CREATE TABLE IF NOT EXISTS note_tags (
                   note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                   tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                   PRIMARY KEY(note_id, tag_id)
                 );
                 CREATE TABLE IF NOT EXISTS media (
                   id INTEGER PRIMARY KEY,
                   note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                   ord INTEGER NOT NULL,
                   rel_path TEXT NOT NULL,
                   kind TEXT NOT NULL,
                   exists_flag INTEGER NOT NULL
                 );
                 CREATE TABLE IF NOT EXISTS warnings (
                   id INTEGER PRIMARY KEY,
                   category TEXT NOT NULL
                 );
                 CREATE INDEX IF NOT EXISTS idx_note_tags_tag ON note_tags(tag_id);
                 CREATE INDEX IF NOT EXISTS idx_notes_eligible_id ON notes(id) WHERE eligible=1;
                 CREATE INDEX IF NOT EXISTS idx_notes_filename ON notes(filename);
                 CREATE INDEX IF NOT EXISTS idx_notes_title_lower ON notes(lower(title));
                 CREATE INDEX IF NOT EXISTS idx_notes_related ON notes(id) WHERE eligible=1 AND has_related != 0;
                 CREATE INDEX IF NOT EXISTS idx_notes_memo ON notes(id) WHERE eligible=1 AND has_memo != 0;
                 CREATE INDEX IF NOT EXISTS idx_notes_multiple_media ON notes(id) WHERE eligible=1 AND media_count >= 2;
                 CREATE INDEX IF NOT EXISTS idx_notes_video ON notes(id) WHERE eligible=1 AND video_count >= 1;",
            )
            .map_err(|_| CoreError::Database)?;
        connection
            .execute(
                "INSERT OR REPLACE INTO meta(key,value) VALUES ('schema_version',?1)",
                [INDEX_SCHEMA_VERSION],
            )
            .map_err(|_| CoreError::Database)?;
        let transaction = connection.transaction().map_err(|_| CoreError::Database)?;
        let indexed_vault = transaction
            .query_row("SELECT value FROM meta WHERE key='vault_root'", [], |row| {
                row.get::<_, String>(0)
            })
            .optional()
            .map_err(|_| CoreError::Database)?;
        if indexed_vault.as_deref() != Some(vault_identity.as_str()) {
            transaction
                .execute_batch(
                    "DELETE FROM note_tags;
                     DELETE FROM media;
                     DELETE FROM notes;
                     DELETE FROM tags;
                     DELETE FROM scan_state;
                     DELETE FROM warnings;",
                )
                .map_err(|_| CoreError::Database)?;
        }
        transaction
            .execute(
                "INSERT OR REPLACE INTO meta(key,value) VALUES ('vault_root',?1)",
                [vault_identity.as_str()],
            )
            .map_err(|_| CoreError::Database)?;
        transaction.commit().map_err(|_| CoreError::Database)?;
        let (note_structure, tag_categories) = load_tag_settings(&database_path)?;
        let structure_fingerprint = format!("{note_structure:?}");
        let previous_structure = connection
            .query_row(
                "SELECT value FROM meta WHERE key='note_structure'",
                [],
                |row| row.get::<_, String>(0),
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        if previous_structure.as_deref() != Some(&structure_fingerprint) {
            connection
                .execute("DELETE FROM scan_state", [])
                .map_err(|_| CoreError::Database)?;
            connection
                .execute(
                    "INSERT OR REPLACE INTO meta(key,value) VALUES ('note_structure',?1)",
                    [&structure_fingerprint],
                )
                .map_err(|_| CoreError::Database)?;
        }

        Ok(Self {
            root,
            saf,
            connection,
            tag_categories,
            note_structure,
        })
    }

    pub fn scan(&mut self) -> Result<ScanReport, CoreError> {
        if self.saf {
            return Err(CoreError::InvalidVault);
        }
        let (files, traversal_warnings) = collect_notes(&self.root)?;
        let mut traversal_complete = traversal_warnings == 0;
        let mut warnings = traversal_warnings;
        let transaction = self
            .connection
            .transaction()
            .map_err(|_| CoreError::Database)?;
        transaction
            .execute("DELETE FROM warnings", [])
            .map_err(|_| CoreError::Database)?;

        let mut existing = BTreeMap::new();
        {
            let mut statement = transaction
                .prepare(
                    "SELECT s.path, s.mtime, s.size, COALESCE(n.eligible, 0)
                     FROM scan_state s LEFT JOIN notes n ON n.path=s.path",
                )
                .map_err(|_| CoreError::Database)?;
            let rows = statement
                .query_map([], |row| {
                    Ok((
                        row.get::<_, String>(0)?,
                        row.get::<_, i64>(1)?,
                        row.get::<_, i64>(2)?,
                        row.get::<_, bool>(3)?,
                    ))
                })
                .map_err(|_| CoreError::Database)?;
            for row in rows {
                let (path, mtime, size, indexed) = row.map_err(|_| CoreError::Database)?;
                existing.insert(path, (mtime, size, indexed));
            }
        }
        let mut seen = BTreeSet::new();
        let mut pending = Vec::new();
        let mut unchanged = 0;
        let mut unchanged_indexed = 0;
        for path in files {
            let relative = path.strip_prefix(&self.root).map_err(|_| CoreError::Io)?;
            let Some(relative_string) = relative.to_str().map(|path| path.replace('\\', "/"))
            else {
                warnings += 1;
                traversal_complete = false;
                continue;
            };
            seen.insert(relative_string.clone());
            let metadata = match fs::metadata(&path) {
                Ok(metadata) => metadata,
                Err(_) => {
                    transaction
                        .execute("INSERT INTO warnings(category) VALUES ('read')", [])
                        .map_err(|_| CoreError::Database)?;
                    warnings += 1;
                    continue;
                }
            };
            let size = i64::try_from(metadata.len()).unwrap_or(i64::MAX);
            let mtime = metadata
                .modified()
                .ok()
                .and_then(|time| time.duration_since(std::time::UNIX_EPOCH).ok())
                .map(|duration| {
                    duration
                        .as_secs()
                        .saturating_mul(1_000_000_000)
                        .saturating_add(u64::from(duration.subsec_nanos()))
                })
                .and_then(|time| i64::try_from(time).ok())
                .unwrap_or(0);
            if existing
                .get(&relative_string)
                .is_some_and(|(old_mtime, old_size, indexed)| {
                    if (*old_mtime, *old_size) == (mtime, size) {
                        unchanged += 1;
                        unchanged_indexed += usize::from(*indexed);
                        true
                    } else {
                        false
                    }
                })
            {
                continue;
            }
            transaction
                .execute("DELETE FROM notes WHERE path=?1", [&relative_string])
                .map_err(|_| CoreError::Database)?;
            transaction
                .execute("DELETE FROM scan_state WHERE path=?1", [&relative_string])
                .map_err(|_| CoreError::Database)?;
            pending.push(ScanItem {
                path,
                relative: relative_string,
                mtime,
                size,
            });
        }
        if unchanged != 0 {
            refresh_media_existence(&transaction, &self.root)?;
        }

        let mut notes_indexed = unchanged_indexed;
        let worker_count = thread::available_parallelism()
            .map(|count| count.get())
            .unwrap_or(2)
            .saturating_sub(1)
            .clamp(1, 4)
            .min(pending.len().max(1));
        let pending = Arc::new(pending);
        let next = Arc::new(AtomicUsize::new(0));
        let (sender, receiver) = mpsc::sync_channel(worker_count * 2);
        thread::scope(|scope| -> Result<(), CoreError> {
            let mut workers = Vec::new();
            for worker in 0..worker_count {
                let pending = Arc::clone(&pending);
                let next = Arc::clone(&next);
                let sender = sender.clone();
                let note_structure = self.note_structure.clone();
                let handle = thread::Builder::new()
                    .name(format!("gallery-scan-{worker}"))
                    .spawn_scoped(scope, move || {
                        loop {
                            let index = next.fetch_add(1, Ordering::Relaxed);
                            let Some(item) = pending.get(index) else {
                                break;
                            };
                            if sender
                                .send((index, read_parse_note(&item.path, &note_structure)))
                                .is_err()
                            {
                                break;
                            }
                        }
                    })
                    .map_err(|_| CoreError::Worker)?;
                workers.push(handle);
            }
            drop(sender);
            for _ in 0..pending.len() {
                let (index, result) = receiver.recv().map_err(|_| CoreError::Worker)?;
                let item = pending.get(index).ok_or(CoreError::Worker)?;
                match result {
                    Ok(parsed) => {
                        transaction
                            .execute(
                                "INSERT INTO scan_state(path,mtime,size) VALUES (?1,?2,?3)",
                                params![item.relative, item.mtime, item.size],
                            )
                            .map_err(|_| CoreError::Database)?;
                        let eligible = parsed.tags.iter().any(|tag| {
                            self.note_structure
                                .gallery_tag_prefixes
                                .iter()
                                .any(|prefix| tag_matches_prefix(tag, prefix))
                        });
                        insert_note(
                            &transaction,
                            &parsed,
                            NoteInsertContext {
                                relative: &item.relative,
                                root: &self.root,
                                mtime: item.mtime,
                                size: item.size,
                                eligible,
                                available_files: None,
                            },
                        )?;
                        if eligible {
                            notes_indexed += 1;
                        }
                    }
                    Err(category) => {
                        transaction
                            .execute("INSERT INTO warnings(category) VALUES (?1)", [category])
                            .map_err(|_| CoreError::Database)?;
                        warnings += 1;
                    }
                }
            }
            for worker in workers {
                worker.join().map_err(|_| CoreError::Worker)?;
            }
            Ok(())
        })?;
        if traversal_complete {
            for old_path in existing.keys().filter(|path| !seen.contains(*path)) {
                transaction
                    .execute("DELETE FROM notes WHERE path=?1", [old_path])
                    .map_err(|_| CoreError::Database)?;
                transaction
                    .execute("DELETE FROM scan_state WHERE path=?1", [old_path])
                    .map_err(|_| CoreError::Database)?;
            }
        }
        transaction
            .execute("DELETE FROM tags WHERE NOT EXISTS (SELECT 1 FROM note_tags WHERE note_tags.tag_id=tags.id)", [])
            .map_err(|_| CoreError::Database)?;
        transaction.commit().map_err(|_| CoreError::Database)?;
        Ok(ScanReport {
            notes_indexed,
            warnings,
        })
    }

    pub fn scan_saf(
        &mut self,
        documents: Vec<SafNoteDocument>,
        file_paths: Vec<String>,
    ) -> Result<ScanReport, CoreError> {
        if !self.saf || documents.len() > MAX_SAF_DOCUMENTS || file_paths.len() > MAX_SAF_DOCUMENTS
        {
            return Err(CoreError::InvalidVault);
        }

        let mut available_files = BTreeSet::new();
        let mut path_bytes = 0_usize;
        for path in file_paths {
            if !saf_path_within_limits(&path) {
                return Err(CoreError::InvalidVault);
            }
            path_bytes = path_bytes
                .checked_add(path.len())
                .ok_or(CoreError::InvalidVault)?;
            if path_bytes > MAX_SAF_AGGREGATE_PATH_BYTES {
                return Err(CoreError::InvalidVault);
            }
            if let Some(path) = normalize_note_path(&path)
                && !relative_path_is_ignored(&path)
            {
                available_files.insert(path);
            }
        }

        let mut note_bytes = 0_usize;
        let mut warnings = 0;
        let mut notes_indexed = 0;
        let transaction = self
            .connection
            .transaction()
            .map_err(|_| CoreError::Database)?;
        transaction
            .execute_batch(
                "DELETE FROM note_tags;
                 DELETE FROM media;
                 DELETE FROM notes;
                 DELETE FROM tags;
                 DELETE FROM scan_state;
                 DELETE FROM warnings;",
            )
            .map_err(|_| CoreError::Database)?;

        for document in documents {
            if !saf_path_within_limits(&document.path) {
                return Err(CoreError::InvalidVault);
            }
            path_bytes = path_bytes
                .checked_add(document.path.len())
                .ok_or(CoreError::InvalidVault)?;
            if path_bytes > MAX_SAF_AGGREGATE_PATH_BYTES {
                return Err(CoreError::InvalidVault);
            }
            let Some(relative) = normalize_note_path(&document.path) else {
                warnings += 1;
                transaction
                    .execute("INSERT INTO warnings(category) VALUES ('read')", [])
                    .map_err(|_| CoreError::Database)?;
                continue;
            };
            if relative_path_is_ignored(&relative) {
                continue;
            }
            if !relative.to_ascii_lowercase().ends_with(".md") {
                continue;
            }
            let Some(content) = document.content else {
                warnings += 1;
                transaction
                    .execute("INSERT INTO warnings(category) VALUES ('read')", [])
                    .map_err(|_| CoreError::Database)?;
                continue;
            };
            if document.size > 0 && usize::try_from(document.size).ok() != Some(content.len()) {
                warnings += 1;
                transaction
                    .execute("INSERT INTO warnings(category) VALUES ('read')", [])
                    .map_err(|_| CoreError::Database)?;
                continue;
            }
            if content.len() > MAX_SAF_NOTE_BYTES {
                return Err(CoreError::InvalidVault);
            }
            note_bytes = note_bytes
                .checked_add(content.len())
                .ok_or(CoreError::InvalidVault)?;
            if note_bytes > MAX_SAF_SCAN_NOTE_BYTES {
                return Err(CoreError::InvalidVault);
            }
            let size = i64::try_from(content.len()).map_err(|_| CoreError::InvalidVault)?;
            let parsed = parse_note_bytes(&content, &self.note_structure);
            match parsed {
                Ok(parsed) => {
                    transaction
                        .execute(
                            "INSERT INTO scan_state(path,mtime,size) VALUES (?1,?2,?3)",
                            params![relative, document.modified_nanos, size],
                        )
                        .map_err(|_| CoreError::Database)?;
                    let eligible = parsed.tags.iter().any(|tag| {
                        self.note_structure
                            .gallery_tag_prefixes
                            .iter()
                            .any(|prefix| tag_matches_prefix(tag, prefix))
                    });
                    insert_note(
                        &transaction,
                        &parsed,
                        NoteInsertContext {
                            relative: &relative,
                            root: &self.root,
                            mtime: document.modified_nanos,
                            size,
                            eligible,
                            available_files: Some(&available_files),
                        },
                    )?;
                    notes_indexed += usize::from(eligible);
                }
                Err(category) => {
                    transaction
                        .execute("INSERT INTO warnings(category) VALUES (?1)", [category])
                        .map_err(|_| CoreError::Database)?;
                    warnings += 1;
                }
            }
        }

        transaction.commit().map_err(|_| CoreError::Database)?;
        Ok(ScanReport {
            notes_indexed,
            warnings,
        })
    }

    pub fn categories(&self) -> Result<Vec<Category>, CoreError> {
        self.categories_for(&[])
    }

    pub fn categories_for(&self, filters: &[String]) -> Result<Vec<Category>, CoreError> {
        self.categories_with_filters(filters, &[], &[])
    }

    pub fn categories_with_filters(
        &self,
        filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
    ) -> Result<Vec<Category>, CoreError> {
        self.categories_with_filter_modes(filters, &[], excluded_filters, virtual_filters)
    }

    pub fn categories_with_filter_modes(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
    ) -> Result<Vec<Category>, CoreError> {
        let plan = CategoryPlan::new(&self.tag_categories);
        let mut buckets: BTreeMap<(usize, String), CategoryBucket> = BTreeMap::new();
        let mut statement = self
            .connection
            .prepare("SELECT name FROM tags ORDER BY name")
            .map_err(|_| CoreError::Database)?;
        let tags = statement
            .query_map([], |row| row.get::<_, String>(0))
            .map_err(|_| CoreError::Database)?;
        for tag in tags {
            let tag = tag.map_err(|_| CoreError::Database)?;
            let has_descendant = statement_has_descendant(&self.connection, &tag)?;
            if has_descendant {
                continue;
            }
            if let Some((order, bucket)) = plan.bucket_for(&tag) {
                let sort_key = (order, bucket.path.clone());
                let name = bucket.option_name(&tag);
                buckets
                    .entry(sort_key)
                    .or_insert(bucket)
                    .options
                    .insert(tag, name);
            }
        }
        let mut filter_buckets: BTreeMap<&str, String> = BTreeMap::new();
        for filter in filters.iter().chain(all_filters).chain(excluded_filters) {
            if let Some(path) = plan.path_for_filter(&self.connection, filter)? {
                filter_buckets.insert(filter.as_str(), path);
            }
        }
        let mut categories = Vec::new();
        for bucket in buckets.into_values() {
            let CategoryBucket {
                path,
                display_name,
                whole_tag,
                options,
                ..
            } = bucket;
            let mut category_filters = Vec::new();
            let mut base_filters = Vec::new();
            for filter in filters {
                if filter_buckets
                    .get(filter.as_str())
                    .is_some_and(|bucket| *bucket == path)
                {
                    category_filters.push(filter.clone());
                } else {
                    base_filters.push(filter.clone());
                }
            }
            let mut category_excluded = Vec::new();
            let mut base_excluded = Vec::new();
            for filter in excluded_filters {
                if filter_buckets
                    .get(filter.as_str())
                    .is_some_and(|bucket| *bucket == path)
                {
                    category_excluded.push(filter.clone());
                } else {
                    base_excluded.push(filter.clone());
                }
            }
            let mut category_all_filters = Vec::new();
            let mut base_all_filters = Vec::new();
            for filter in all_filters {
                if filter_buckets
                    .get(filter.as_str())
                    .is_some_and(|bucket| *bucket == path)
                {
                    category_all_filters.push(filter.clone());
                } else {
                    base_all_filters.push(filter.clone());
                }
            }
            let base_matches = self.matching_note_ids_with_all(
                &base_filters,
                &base_all_filters,
                &base_excluded,
                virtual_filters,
            )?;
            let mut selected_ids = BTreeSet::new();
            for filter in &category_filters {
                selected_ids.extend(note_ids_for_tag(&self.connection, filter)?);
            }
            let selected_all_ids = self.intersect_tag_ids(&category_all_filters)?;
            let mut excluded_ids = BTreeSet::new();
            for filter in &category_excluded {
                excluded_ids.extend(note_ids_for_tag(&self.connection, filter)?);
            }
            let mut category_ids = BTreeSet::new();
            let mut category_options = Vec::new();
            for (full_tag, name) in &options {
                let name = name.clone();
                let option_ids = note_ids_for_tag(&self.connection, full_tag)?;
                category_ids.extend(option_ids.iter().copied());
                let mut matching_option_ids = selected_ids
                    .union(&option_ids)
                    .copied()
                    .collect::<BTreeSet<_>>();
                if !category_all_filters.is_empty() {
                    matching_option_ids = matching_option_ids
                        .intersection(&selected_all_ids)
                        .copied()
                        .collect();
                }
                let count = matching_option_ids
                    .intersection(&base_matches)
                    .filter(|id| !excluded_ids.contains(id))
                    .count();
                let active = !selected_ids.is_disjoint(&option_ids)
                    || !excluded_ids.is_disjoint(&option_ids)
                    || category_all_filters.contains(full_tag);
                category_options.push(CategoryOption {
                    count,
                    disabled: count == 0 && !active,
                    name,
                    full_tag: full_tag.clone(),
                    virtual_filter: None,
                });
            }
            let current_category_ids = if category_all_filters.is_empty() {
                category_ids.clone()
            } else {
                category_ids
                    .intersection(&selected_all_ids)
                    .copied()
                    .collect()
            };
            let count = base_matches.intersection(&current_category_ids).count();
            if let Some(whole_tag) = whole_tag {
                category_options.insert(
                    0,
                    CategoryOption {
                        name: "すべて".to_owned(),
                        full_tag: whole_tag,
                        count,
                        disabled: count == 0
                            && selected_ids.is_empty()
                            && category_all_filters.is_empty()
                            && excluded_ids.is_empty(),
                        virtual_filter: None,
                    },
                );
            }
            categories.push(Category {
                path,
                display_name,
                options: category_options,
                count,
            });
        }
        let matched_count = self
            .matching_note_ids_with_all(filters, all_filters, excluded_filters, virtual_filters)?
            .len();
        let mut content_options = Vec::with_capacity(VIRTUAL_FILTERS.len());
        for (filter, name) in VIRTUAL_FILTERS {
            let mut option_filters = virtual_filters.to_vec();
            option_filters.push(filter);
            let count = self
                .matching_note_ids_with_all(
                    filters,
                    all_filters,
                    excluded_filters,
                    &option_filters,
                )?
                .len();
            content_options.push(CategoryOption {
                name: name.to_owned(),
                full_tag: String::new(),
                count,
                disabled: count == 0 && !virtual_filters.contains(&filter),
                virtual_filter: Some(filter),
            });
        }
        categories.insert(
            0,
            Category {
                path: "@content".to_owned(),
                display_name: "コンテンツ".to_owned(),
                options: content_options,
                count: matched_count,
            },
        );
        Ok(categories)
    }

    pub fn query(&self, filters: &[String], limit: usize) -> Result<Vec<NoteSummary>, CoreError> {
        self.query_page(filters, 0, limit)
    }

    pub fn query_page(
        &self,
        filters: &[String],
        offset: usize,
        limit: usize,
    ) -> Result<Vec<NoteSummary>, CoreError> {
        self.query_filtered_page(filters, &[], offset, limit)
    }

    pub fn query_filtered_page(
        &self,
        filters: &[String],
        virtual_filters: &[VirtualFilter],
        offset: usize,
        limit: usize,
    ) -> Result<Vec<NoteSummary>, CoreError> {
        self.query_filtered_page_search(filters, &[], virtual_filters, "", offset, limit)
    }

    pub fn query_filtered_page_search(
        &self,
        filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<NoteSummary>, CoreError> {
        self.query_filtered_page_search_with_all(
            filters,
            &[],
            excluded_filters,
            virtual_filters,
            search_query,
            offset,
            limit,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn query_filtered_page_search_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<NoteSummary>, CoreError> {
        self.query_filtered_page_search_with_all_and_sort(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
            NoteSort::default(),
            offset,
            limit,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn query_filtered_page_search_with_all_and_sort(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        sort: NoteSort,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<NoteSummary>, CoreError> {
        let ordered = self.sorted_note_ids_with_all_and_sort(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
            sort,
        )?;
        let mut result = Vec::new();
        for id in ordered.into_iter().skip(offset).take(limit) {
            let summary = self
                .connection
                .query_row(
                    "SELECT path, title, media_count, video_count, memo_count, related_count
                     FROM notes WHERE id=?1",
                    [id],
                    |row| {
                        Ok(NoteSummary {
                            id,
                            path: row.get(0)?,
                            title: row.get(1)?,
                            media_count: row.get::<_, i64>(2)? as usize,
                            video_count: row.get::<_, i64>(3)? as usize,
                            memo_count: row.get::<_, i64>(4)? as usize,
                            related_count: row.get::<_, i64>(5)? as usize,
                            representative_media_id: self
                                .connection
                                .query_row(
                                    "SELECT id FROM media WHERE note_id=?1 ORDER BY ord LIMIT 1",
                                    [id],
                                    |media_row| media_row.get(0),
                                )
                                .optional()?,
                        })
                    },
                )
                .map_err(|_| CoreError::Database)?;
            result.push(summary);
        }
        Ok(result)
    }

    pub fn count_filtered_notes_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
    ) -> Result<usize, CoreError> {
        Ok(self
            .sorted_note_ids_with_all(
                filters,
                all_filters,
                excluded_filters,
                virtual_filters,
                search_query,
            )?
            .len())
    }

    /// Flattens every (not just representative) media item belonging to the
    /// notes matching `filters`/`virtual_filters`, ordered the same way as
    /// `query_filtered_page` (newest `published` first, then media
    /// appearance order within a note), then paginates across that flat list.
    pub fn query_media_filtered_page(
        &self,
        filters: &[String],
        virtual_filters: &[VirtualFilter],
        offset: usize,
        limit: usize,
    ) -> Result<Vec<MediaSummary>, CoreError> {
        self.query_media_filtered_page_search(filters, &[], virtual_filters, "", offset, limit)
    }

    pub fn query_media_filtered_page_search(
        &self,
        filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<MediaSummary>, CoreError> {
        self.query_media_filtered_page_search_with_all(
            filters,
            &[],
            excluded_filters,
            virtual_filters,
            search_query,
            offset,
            limit,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn query_media_filtered_page_search_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<MediaSummary>, CoreError> {
        self.query_media_filtered_page_search_with_all_and_sort(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
            NoteSort::default(),
            offset,
            limit,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn query_media_filtered_page_search_with_all_and_sort(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        sort: NoteSort,
        offset: usize,
        limit: usize,
    ) -> Result<Vec<MediaSummary>, CoreError> {
        let ordered = self.sorted_note_ids_with_all_and_sort(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
            sort,
        )?;
        let mut result = Vec::new();
        let mut skipped = 0usize;
        for note_id in ordered {
            if result.len() >= limit {
                break;
            }
            let (media_count, memo_count, related_count) = self
                .connection
                .query_row(
                    "SELECT media_count, memo_count, related_count FROM notes WHERE id=?1",
                    [note_id],
                    |row| {
                        Ok((
                            row.get::<_, i64>(0)? as usize,
                            row.get::<_, i64>(1)? as usize,
                            row.get::<_, i64>(2)? as usize,
                        ))
                    },
                )
                .map_err(|_| CoreError::Database)?;
            let mut statement = self
                .connection
                .prepare("SELECT id, kind, exists_flag FROM media WHERE note_id=?1 ORDER BY ord")
                .map_err(|_| CoreError::Database)?;
            let rows = statement
                .query_map([note_id], |row| {
                    Ok((
                        row.get::<_, i64>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, i64>(2)?,
                    ))
                })
                .map_err(|_| CoreError::Database)?;
            for row in rows {
                let (media_id, kind, exists_flag) = row.map_err(|_| CoreError::Database)?;
                if skipped < offset {
                    skipped += 1;
                    continue;
                }
                if result.len() >= limit {
                    break;
                }
                result.push(MediaSummary {
                    id: media_id,
                    note_id,
                    is_video: kind == "video",
                    exists: exists_flag != 0,
                    media_count,
                    memo_count,
                    related_count,
                });
            }
        }
        Ok(result)
    }

    pub fn count_filtered_media_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
    ) -> Result<usize, CoreError> {
        let note_ids = self.sorted_note_ids_with_all(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
        )?;
        let mut statement = self
            .connection
            .prepare("SELECT COUNT(*) FROM media WHERE note_id=?1")
            .map_err(|_| CoreError::Database)?;
        note_ids.into_iter().try_fold(0usize, |count, note_id| {
            let note_media_count = statement
                .query_row([note_id], |row| row.get::<_, i64>(0))
                .map_err(|_| CoreError::Database)?;
            Ok(count.saturating_add(note_media_count as usize))
        })
    }

    fn sorted_note_ids_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
    ) -> Result<Vec<i64>, CoreError> {
        self.sorted_note_ids_with_all_and_sort(
            filters,
            all_filters,
            excluded_filters,
            virtual_filters,
            search_query,
            NoteSort::default(),
        )
    }

    fn sorted_note_ids_with_all_and_sort(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
        search_query: &str,
        sort: NoteSort,
    ) -> Result<Vec<i64>, CoreError> {
        let (search_text, query_filters, query_all_filters, query_excluded_filters) =
            parse_note_search_query(search_query);
        let mut filters = filters.to_vec();
        filters.extend(self.expand_fuzzy_tag_queries(query_filters)?);
        let mut all_filters = all_filters.to_vec();
        all_filters.extend(self.expand_fuzzy_tag_queries(query_all_filters)?);
        let mut excluded_filters = excluded_filters.to_vec();
        excluded_filters.extend(self.expand_fuzzy_tag_queries(query_excluded_filters)?);
        let mut ids = self
            .matching_note_ids_with_all(&filters, &all_filters, &excluded_filters, virtual_filters)?
            .into_iter()
            .collect::<Vec<_>>();
        if !search_text.trim().is_empty() {
            let query = search_text.trim().chars().take(128).collect::<String>();
            let mut searched_ids = Vec::with_capacity(ids.len());
            let mut tag_statement = self
                .connection
                .prepare(
                    "SELECT tags.name FROM note_tags
                     JOIN tags ON tags.id=note_tags.tag_id
                     WHERE note_tags.note_id=?1",
                )
                .map_err(|_| CoreError::Database)?;
            for id in ids {
                let (title, path) = self
                    .connection
                    .query_row("SELECT title, path FROM notes WHERE id=?1", [id], |row| {
                        Ok((row.get::<_, String>(0)?, row.get::<_, String>(1)?))
                    })
                    .map_err(|_| CoreError::Database)?;
                let tags = tag_statement
                    .query_map([id], |row| row.get::<_, String>(0))
                    .map_err(|_| CoreError::Database)?
                    .collect::<Result<Vec<_>, _>>()
                    .map_err(|_| CoreError::Database)?;
                let matches = query.split_whitespace().all(|term| {
                    fuzzy_note_match(&title, &path, term)
                        || tags
                            .iter()
                            .any(|tag| fuzzy_tag_match_score(tag, term).is_some())
                });
                if matches {
                    searched_ids.push(id);
                }
            }
            ids = searched_ids;
        }
        let mut with_date = Vec::with_capacity(ids.len());
        for id in ids {
            let (published, created, path): (Option<String>, Option<String>, String) = self
                .connection
                .query_row(
                    "SELECT published, created, path FROM notes WHERE id=?1",
                    [id],
                    |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
                )
                .map_err(|_| CoreError::Database)?;
            let date = match sort.field {
                NoteSortField::Published => published,
                NoteSortField::Created => created,
            };
            with_date.push((date, path, id));
        }
        with_date.sort_by(|(left_date, left_path, _), (right_date, right_path, _)| {
            let date_order = match (left_date, right_date) {
                (Some(left), Some(right)) => match sort.direction {
                    SortDirection::Ascending => left.cmp(right),
                    SortDirection::Descending => right.cmp(left),
                },
                (Some(_), None) => std::cmp::Ordering::Less,
                (None, Some(_)) => std::cmp::Ordering::Greater,
                (None, None) => std::cmp::Ordering::Equal,
            };
            date_order.then_with(|| left_path.cmp(right_path))
        });
        Ok(with_date.into_iter().map(|(_, _, id)| id).collect())
    }

    fn expand_fuzzy_tag_queries(&self, filters: Vec<String>) -> Result<Vec<String>, CoreError> {
        let mut statement = self
            .connection
            .prepare("SELECT name FROM tags ORDER BY name")
            .map_err(|_| CoreError::Database)?;
        let tags = statement
            .query_map([], |row| row.get::<_, String>(0))
            .map_err(|_| CoreError::Database)?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|_| CoreError::Database)?;
        Ok(filters
            .into_iter()
            .map(|filter| {
                tags.iter()
                    .filter_map(|tag| fuzzy_tag_match_score(tag, &filter).map(|score| (score, tag)))
                    .min_by(|(left_score, left_tag), (right_score, right_tag)| {
                        left_score
                            .cmp(right_score)
                            .then_with(|| left_tag.cmp(right_tag))
                    })
                    .map(|(_, tag)| tag.clone())
                    .unwrap_or(filter)
            })
            .collect())
    }

    fn matching_note_ids_with_all(
        &self,
        filters: &[String],
        all_filters: &[String],
        excluded_filters: &[String],
        virtual_filters: &[VirtualFilter],
    ) -> Result<BTreeSet<i64>, CoreError> {
        let plan = CategoryPlan::new(&self.tag_categories);
        let mut grouped_filters: BTreeMap<String, BTreeSet<String>> = BTreeMap::new();
        for filter in filters {
            let category = plan
                .path_for_filter(&self.connection, filter)?
                .unwrap_or_else(|| filter.clone());
            grouped_filters
                .entry(category)
                .or_default()
                .insert(filter.clone());
        }
        let mut candidates: Option<BTreeSet<i64>> = None;
        for alternatives in grouped_filters.values() {
            let mut category_ids = BTreeSet::new();
            for tag in alternatives {
                category_ids.extend(note_ids_for_tag(&self.connection, tag)?);
            }
            candidates = Some(match candidates {
                None => category_ids,
                Some(current) => current.intersection(&category_ids).copied().collect(),
            });
        }
        if !all_filters.is_empty() {
            let required_ids = self.intersect_tag_ids(all_filters)?;
            candidates = Some(match candidates {
                None => required_ids,
                Some(current) => current.intersection(&required_ids).copied().collect(),
            });
        }
        for virtual_filter in virtual_filters {
            let virtual_ids = note_ids_for_virtual_filter(&self.connection, *virtual_filter)?;
            candidates = Some(match candidates {
                None => virtual_ids,
                Some(current) => current.intersection(&virtual_ids).copied().collect(),
            });
        }
        let mut candidates = match candidates {
            Some(ids) => ids,
            None => all_note_ids(&self.connection)?,
        };
        for tag in excluded_filters {
            for note_id in note_ids_for_tag(&self.connection, tag)? {
                candidates.remove(&note_id);
            }
        }
        Ok(candidates)
    }

    fn intersect_tag_ids(&self, tags: &[String]) -> Result<BTreeSet<i64>, CoreError> {
        let mut matching: Option<BTreeSet<i64>> = None;
        for tag in tags {
            let ids = note_ids_for_tag(&self.connection, tag)?;
            matching = Some(match matching {
                None => ids,
                Some(current) => current.intersection(&ids).copied().collect(),
            });
        }
        Ok(matching.unwrap_or_default())
    }

    pub fn warning_count(&self) -> Result<usize, CoreError> {
        self.connection
            .query_row("SELECT COUNT(*) FROM warnings", [], |row| {
                row.get::<_, i64>(0).map(|n| n as usize)
            })
            .map_err(|_| CoreError::Database)
    }

    pub fn note_path(&self, note_id: i64) -> Result<Option<String>, CoreError> {
        self.connection
            .query_row("SELECT path FROM notes WHERE id=?1", [note_id], |row| {
                row.get::<_, String>(0)
            })
            .optional()
            .map_err(|_| CoreError::Database)
    }

    pub fn get_thumbnail(
        &self,
        media_id: i64,
        size: u32,
        cache_root: &Path,
    ) -> Result<Option<Vec<u8>>, CoreError> {
        if size == 0 || size > MAX_THUMBNAIL_SIZE {
            return Err(CoreError::Io);
        }
        let _guard = THUMBNAIL_LOCK.lock().map_err(|_| CoreError::Worker)?;
        let Some((source, media_identity, is_video)) = self.media_path(media_id)? else {
            return Ok(None);
        };
        if is_video {
            return Ok(None);
        }
        let metadata = fs::metadata(&source).map_err(|_| CoreError::Io)?;
        if !metadata.is_file() || metadata.len() > MAX_THUMBNAIL_SOURCE_BYTES {
            return Ok(None);
        }
        let modified = metadata
            .modified()
            .ok()
            .and_then(|time| time.duration_since(UNIX_EPOCH).ok())
            .map_or(0, |duration| duration.as_nanos());
        let cache_root = prepare_thumbnail_cache(cache_root, &self.root)?;
        let media_shard = cache_root.join(format!("{:02x}", media_id.rem_euclid(256)));
        match fs::symlink_metadata(&media_shard) {
            Ok(metadata) if metadata.file_type().is_dir() => {}
            Ok(_) => return Err(CoreError::Io),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
                fs::create_dir(&media_shard).map_err(|_| CoreError::Io)?;
            }
            Err(_) => return Err(CoreError::Io),
        }
        let media_shard = fs::canonicalize(&media_shard).map_err(|_| CoreError::Io)?;
        if !media_shard.starts_with(&cache_root) {
            return Err(CoreError::Io);
        }
        set_private_directory(&media_shard)?;
        let cache_key = Sha256::digest(media_identity.as_bytes());
        let cache_key = cache_key[..16]
            .iter()
            .map(|byte| format!("{byte:02x}"))
            .collect::<String>();
        let cache_file = media_shard.join(format!(
            "{cache_key}-{modified}-{}-{size}.png",
            metadata.len()
        ));
        let temporary_file = media_shard.join(format!(
            "{cache_key}-{modified}-{size}-{}.tmp",
            std::process::id()
        ));
        if let Ok(cache_metadata) = fs::symlink_metadata(&cache_file) {
            if !cache_metadata.file_type().is_file() || cache_metadata.len() > 4 * 1024 * 1024 {
                return Err(CoreError::Io);
            }
            return fs::read(cache_file).map(Some).map_err(|_| CoreError::Io);
        }

        let mut input = Vec::with_capacity(metadata.len() as usize);
        File::open(source)
            .map_err(|_| CoreError::Io)?
            .take(MAX_THUMBNAIL_SOURCE_BYTES + 1)
            .read_to_end(&mut input)
            .map_err(|_| CoreError::Io)?;
        if input.len() as u64 > MAX_THUMBNAIL_SOURCE_BYTES {
            return Ok(None);
        }
        let reader = match ImageReader::new(Cursor::new(&input)).with_guessed_format() {
            Ok(reader) => reader,
            Err(_) => return Ok(None),
        };
        let mut decoder = match reader.into_decoder() {
            Ok(decoder) => decoder,
            Err(_) => return Ok(None),
        };
        let (width, height) = decoder.dimensions();
        if width == 0
            || height == 0
            || width > MAX_THUMBNAIL_DIMENSION
            || height > MAX_THUMBNAIL_DIMENSION
            || u64::from(width) * u64::from(height) > MAX_THUMBNAIL_PIXELS
        {
            return Ok(None);
        }
        let mut limits = Limits::default();
        limits.max_image_width = Some(MAX_THUMBNAIL_DIMENSION);
        limits.max_image_height = Some(MAX_THUMBNAIL_DIMENSION);
        limits.max_alloc = Some(MAX_THUMBNAIL_ALLOCATION);
        if decoder.set_limits(limits).is_err() {
            return Ok(None);
        }
        let image = match image::DynamicImage::from_decoder(decoder) {
            Ok(image) => image,
            Err(_) => return Ok(None),
        };
        let thumbnail = image.thumbnail(size, size);
        let mut encoded = Cursor::new(Vec::new());
        if thumbnail.write_to(&mut encoded, ImageFormat::Png).is_err() {
            return Ok(None);
        }
        if encoded.get_ref().len() > 4 * 1024 * 1024 {
            return Ok(None);
        }
        match fs::symlink_metadata(&temporary_file) {
            Ok(_) => fs::remove_file(&temporary_file).map_err(|_| CoreError::Io)?,
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
            Err(_) => return Err(CoreError::Io),
        }
        let mut options = OpenOptions::new();
        options.write(true).create_new(true);
        #[cfg(unix)]
        {
            use std::os::unix::fs::OpenOptionsExt;
            options.mode(0o600);
        }
        let mut output = options.open(&temporary_file).map_err(|_| CoreError::Io)?;
        output
            .write_all(encoded.get_ref())
            .map_err(|_| CoreError::Io)?;
        output.sync_all().map_err(|_| CoreError::Io)?;
        drop(output);
        fs::rename(&temporary_file, &cache_file).map_err(|_| CoreError::Io)?;
        Ok(Some(encoded.into_inner()))
    }

    pub fn video_source_path(&self, media_id: i64) -> Result<Option<PathBuf>, CoreError> {
        let Some((source, _, is_video)) = self.media_path(media_id)? else {
            return Ok(None);
        };
        if !is_video {
            return Ok(None);
        }
        let metadata = fs::metadata(&source).map_err(|_| CoreError::Io)?;
        if !metadata.is_file() || metadata.len() > MAX_THUMBNAIL_SOURCE_BYTES {
            return Ok(None);
        }
        Ok(Some(source))
    }

    pub fn media_source_path(&self, media_id: i64) -> Result<Option<PathBuf>, CoreError> {
        let Some((source, _, _)) = self.media_path(media_id)? else {
            return Ok(None);
        };
        let metadata = fs::metadata(&source).map_err(|_| CoreError::Io)?;
        if !metadata.is_file() {
            return Ok(None);
        }
        Ok(Some(source))
    }

    pub fn media_relative_path(&self, media_id: i64) -> Result<Option<String>, CoreError> {
        self.media_relative_path_for_kind(media_id, None)
    }

    pub fn video_media_relative_path(&self, media_id: i64) -> Result<Option<String>, CoreError> {
        self.media_relative_path_for_kind(media_id, Some("video"))
    }

    fn media_relative_path_for_kind(
        &self,
        media_id: i64,
        kind: Option<&str>,
    ) -> Result<Option<String>, CoreError> {
        if !self.saf {
            return Err(CoreError::InvalidVault);
        }
        let media = self
            .connection
            .query_row(
                "SELECT notes.path, media.rel_path, media.exists_flag, media.kind
                 FROM media JOIN notes ON notes.id=media.note_id WHERE media.id=?1",
                [media_id],
                |row| {
                    Ok((
                        row.get::<_, String>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, bool>(2)?,
                        row.get::<_, String>(3)?,
                    ))
                },
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        let Some((note_path, media_path, exists, actual_kind)) = media else {
            return Ok(None);
        };
        if !exists || kind.is_some_and(|kind| kind != actual_kind) {
            return Ok(None);
        }
        Ok(resolve_media_relative_path(&note_path, &media_path))
    }

    pub fn note_detail(&self, note_id: i64) -> Result<Option<NoteDetail>, CoreError> {
        self.note_detail_with_content(note_id, None)
    }

    pub fn note_detail_saf(
        &self,
        note_id: i64,
        content: &[u8],
    ) -> Result<Option<NoteDetail>, CoreError> {
        if !self.saf {
            return Err(CoreError::InvalidVault);
        }
        self.note_detail_with_content(note_id, Some(content))
    }

    fn note_detail_with_content(
        &self,
        note_id: i64,
        content: Option<&[u8]>,
    ) -> Result<Option<NoteDetail>, CoreError> {
        let indexed_note = self
            .connection
            .query_row(
                "SELECT path, eligible FROM notes WHERE id=?1",
                [note_id],
                |row| Ok((row.get::<_, String>(0)?, row.get::<_, bool>(1)?)),
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        let Some((indexed_path, eligible)) = indexed_note else {
            return Ok(None);
        };
        let parsed = if self.saf {
            parse_note_bytes(content.ok_or(CoreError::Io)?, &self.note_structure)
                .map_err(|_| CoreError::Io)?
        } else {
            let path =
                fs::canonicalize(self.root.join(&indexed_path)).map_err(|_| CoreError::Io)?;
            if !path.starts_with(&self.root) || !path.is_file() {
                return Err(CoreError::Io);
            }
            read_parse_note(&path, &self.note_structure).map_err(|_| CoreError::Io)?
        };
        if !eligible {
            let has_media: bool = self
                .connection
                .query_row(
                    "SELECT EXISTS(SELECT 1 FROM media WHERE note_id=?1)",
                    [note_id],
                    |row| row.get(0),
                )
                .map_err(|_| CoreError::Database)?;
            if !has_media {
                for (ordinal, media) in parsed.media.iter().enumerate() {
                    let exists =
                        !self.saf && safe_media_exists(&self.root, &indexed_path, &media.path);
                    self.connection
                        .execute(
                            "INSERT INTO media(note_id,ord,rel_path,kind,exists_flag)
                             VALUES (?1,?2,?3,?4,?5)",
                            params![
                                note_id,
                                ordinal as i64,
                                media.path,
                                if media.kind == MediaKind::Video {
                                    "video"
                                } else {
                                    "image"
                                },
                                exists
                            ],
                        )
                        .map_err(|_| CoreError::Database)?;
                }
            }
        }
        let media = {
            let mut statement = self
                .connection
                .prepare(
                    "SELECT media.id, media.note_id, media.kind, media.exists_flag,
                            notes.media_count, notes.memo_count, notes.related_count
                     FROM media JOIN notes ON notes.id=media.note_id
                     WHERE media.note_id=?1 ORDER BY media.ord",
                )
                .map_err(|_| CoreError::Database)?;
            let rows = statement
                .query_map([note_id], |row| {
                    Ok(MediaSummary {
                        id: row.get(0)?,
                        note_id: row.get(1)?,
                        is_video: row.get::<_, String>(2)? == "video",
                        exists: row.get(3)?,
                        media_count: row.get::<_, i64>(4)? as usize,
                        memo_count: row.get::<_, i64>(5)? as usize,
                        related_count: row.get::<_, i64>(6)? as usize,
                    })
                })
                .map_err(|_| CoreError::Database)?;
            rows.collect::<Result<Vec<_>, _>>()
                .map_err(|_| CoreError::Database)?
        };
        Ok(Some(NoteDetail {
            id: note_id,
            path: indexed_path.clone(),
            title: parsed.title,
            author: parsed.author,
            author_url: parsed.author_url,
            url: parsed.url,
            published: parsed.published,
            created: parsed.created,
            updated: parsed.updated,
            tags: parsed.tags,
            body_text: parsed.body_text,
            memo_lines: parsed
                .memo_lines
                .iter()
                .zip(parsed.memo_bullets.iter())
                .zip(parsed.memo_indent_levels.iter())
                .map(|((line, is_bullet), indent_level)| {
                    detail_line(
                        &self.connection,
                        &indexed_path,
                        line,
                        *is_bullet,
                        *indent_level,
                        self.note_structure.link_resolution,
                    )
                })
                .collect::<Result<Vec<_>, _>>()?,
            related_lines: parsed
                .related_lines
                .iter()
                .zip(parsed.related_bullets.iter())
                .zip(parsed.related_indent_levels.iter())
                .map(|((line, is_bullet), indent_level)| {
                    detail_line(
                        &self.connection,
                        &indexed_path,
                        line,
                        *is_bullet,
                        *indent_level,
                        self.note_structure.link_resolution,
                    )
                })
                .collect::<Result<Vec<_>, _>>()?,
            media,
        }))
    }

    fn media_path(&self, media_id: i64) -> Result<Option<(PathBuf, String, bool)>, CoreError> {
        let media = self
            .connection
            .query_row(
                "SELECT notes.path, media.rel_path, media.exists_flag, media.kind
                 FROM media JOIN notes ON notes.id=media.note_id WHERE media.id=?1",
                [media_id],
                |row| {
                    Ok((
                        row.get::<_, String>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, bool>(2)?,
                        row.get::<_, String>(3)?,
                    ))
                },
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        let Some((note_path, media_path, exists, kind)) = media else {
            return Ok(None);
        };
        if !exists {
            return Ok(None);
        }

        Ok(
            resolve_media(&self.root, &note_path, &media_path).map(|path| {
                (
                    path,
                    format!("{}\0{note_path}\0{media_path}", self.root.display()),
                    kind == "video",
                )
            }),
        )
    }
}

fn detail_line(
    connection: &Connection,
    source_note_path: &str,
    tokens: &[InlineToken],
    is_bullet: bool,
    indent_level: u8,
    link_resolution: LinkResolutionMode,
) -> Result<DetailLine, CoreError> {
    let mut text = String::new();
    let mut urls = Vec::new();
    let mut linked_note_id = None;
    for token in tokens {
        match token {
            InlineToken::Text(value) => text.push_str(value),
            InlineToken::ExternalLink { label, url } => {
                text.push_str(label);
                urls.push(url.clone());
                if linked_note_id.is_none() {
                    linked_note_id =
                        resolve_linked_note_id(connection, source_note_path, url, link_resolution)?;
                }
            }
        }
    }
    Ok(DetailLine {
        text,
        urls,
        is_bullet,
        indent_level,
        linked_note_id,
    })
}

fn resolve_linked_note_id(
    connection: &Connection,
    source_note_path: &str,
    url: &str,
    mode: LinkResolutionMode,
) -> Result<Option<i64>, CoreError> {
    if url.is_empty() || url.starts_with('\\') || url.contains("://") || url.starts_with('#') {
        return Ok(None);
    }
    let target = url.split(['#', '?']).next().unwrap_or_default();
    if target.is_empty() || target.contains('\\') || target.contains('\0') {
        return Ok(None);
    }
    let Ok(decoded_target) = urlencoding::decode(target) else {
        return Ok(None);
    };
    let target = decoded_target.as_ref();
    let root_relative = target.trim_start_matches('/');
    if root_relative.is_empty() {
        return Ok(None);
    }
    let mut candidates = Vec::new();
    if mode != LinkResolutionMode::AbsolutePath && !target.starts_with('/') {
        if let Some(candidate) = resolve_relative_path(source_note_path, target) {
            candidates.push(candidate);
        }
    }
    if mode == LinkResolutionMode::AbsolutePath
        || target.starts_with('/')
        || mode == LinkResolutionMode::ShortestPath
    {
        if let Some(candidate) = normalize_note_path(root_relative) {
            candidates.push(candidate);
        }
    }
    for candidate in candidates.into_iter().flat_map(note_path_variants) {
        let note_id = connection
            .query_row("SELECT id FROM notes WHERE path=?1", [&candidate], |row| {
                row.get::<_, i64>(0)
            })
            .optional()
            .map_err(|_| CoreError::Database)?;
        if note_id.is_some() {
            return Ok(note_id);
        }
    }
    let basename = Path::new(root_relative)
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or(CoreError::Database)?;
    let stem = Path::new(basename)
        .file_stem()
        .and_then(|name| name.to_str())
        .unwrap_or(basename);
    let filename = format!("{stem}.md");
    let source_parts = Path::new(source_note_path)
        .parent()
        .unwrap_or_else(|| Path::new(""))
        .components()
        .filter_map(|component| component.as_os_str().to_str())
        .collect::<Vec<_>>();
    let mut statement = connection
        .prepare(
            "SELECT id, path FROM notes
             WHERE filename=?1",
        )
        .map_err(|_| CoreError::Database)?;
    let rows = statement
        .query_map([&filename], |row| {
            Ok((row.get::<_, i64>(0)?, row.get::<_, String>(1)?))
        })
        .map_err(|_| CoreError::Database)?;
    let mut matches = rows
        .collect::<Result<Vec<_>, _>>()
        .map_err(|_| CoreError::Database)?;
    matches.sort_by_key(|(_, path)| {
        let parts = Path::new(path)
            .parent()
            .unwrap_or_else(|| Path::new(""))
            .components()
            .filter_map(|component| component.as_os_str().to_str())
            .collect::<Vec<_>>();
        let common = source_parts
            .iter()
            .zip(&parts)
            .take_while(|(left, right)| left == right)
            .count();
        (
            source_parts.len() + parts.len() - common * 2,
            path.to_lowercase(),
        )
    });
    if let Some((id, _)) = matches.first() {
        return Ok(Some(*id));
    }

    connection
        .query_row(
            "SELECT id FROM notes WHERE lower(title)=lower(?1) ORDER BY path LIMIT 1",
            [stem],
            |row| row.get(0),
        )
        .optional()
        .map_err(|_| CoreError::Database)
}

fn resolve_relative_path(source_note_path: &str, target: &str) -> Option<String> {
    let parent = Path::new(source_note_path)
        .parent()
        .unwrap_or_else(|| Path::new(""));
    normalize_note_path(&parent.join(target).to_string_lossy())
}

fn tag_matches_prefix(tag: &str, prefix: &str) -> bool {
    let prefix = prefix.trim_end_matches('/');
    tag == prefix
        || tag
            .strip_prefix(prefix)
            .is_some_and(|suffix| suffix.starts_with('/'))
}

fn normalize_note_path(value: &str) -> Option<String> {
    let mut parts = Vec::new();
    for component in Path::new(value).components() {
        match component {
            Component::Normal(value) => parts.push(value.to_str()?.to_owned()),
            Component::CurDir => {}
            Component::ParentDir if !parts.is_empty() => {
                parts.pop();
            }
            Component::ParentDir | Component::RootDir | Component::Prefix(_) => return None,
        }
    }
    (!parts.is_empty()).then(|| parts.join("/"))
}

fn saf_path_within_limits(value: &str) -> bool {
    !value.is_empty()
        && value.len() <= MAX_SAF_RELATIVE_PATH_BYTES
        && !value.starts_with('/')
        && !value.contains('\\')
        && !value.contains('\0')
        && value.bytes().filter(|byte| *byte == b'/').count() <= MAX_SAF_DEPTH
        && !value
            .split('/')
            .any(|component| component.is_empty() || component == "." || component == "..")
}

fn note_path_variants(path: String) -> Vec<String> {
    let mut variants = vec![path.clone()];
    if Path::new(&path).extension().is_none() {
        variants.push(format!("{path}.md"));
    }
    variants
}

fn resolve_database_path(database: &Path, root: &Path) -> Result<PathBuf, CoreError> {
    let resolved = if fs::symlink_metadata(database).is_ok() {
        fs::canonicalize(database).map_err(|_| CoreError::Database)?
    } else {
        let parent = database
            .parent()
            .filter(|parent| !parent.as_os_str().is_empty())
            .unwrap_or_else(|| Path::new("."));
        let canonical_parent = fs::canonicalize(parent).map_err(|_| CoreError::Database)?;
        let name = database.file_name().ok_or(CoreError::Database)?;
        canonical_parent.join(name)
    };
    if resolved.starts_with(root) {
        return Err(CoreError::Database);
    }
    if let Ok(metadata) = fs::metadata(&resolved) {
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            if metadata.nlink() > 1 {
                return Err(CoreError::Database);
            }
        }
        if !metadata.is_file() {
            return Err(CoreError::Database);
        }
    }
    Ok(resolved)
}

fn resolve_saf_database_path(database: &Path) -> Result<PathBuf, CoreError> {
    let resolved = if fs::symlink_metadata(database).is_ok() {
        fs::canonicalize(database).map_err(|_| CoreError::Database)?
    } else {
        let parent = database
            .parent()
            .filter(|parent| !parent.as_os_str().is_empty())
            .unwrap_or_else(|| Path::new("."));
        let canonical_parent = fs::canonicalize(parent).map_err(|_| CoreError::Database)?;
        let name = database.file_name().ok_or(CoreError::Database)?;
        canonical_parent.join(name)
    };
    if let Ok(metadata) = fs::metadata(&resolved) {
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            if metadata.nlink() > 1 {
                return Err(CoreError::Database);
            }
        }
        if !metadata.is_file() {
            return Err(CoreError::Database);
        }
    }
    Ok(resolved)
}

fn all_note_ids(connection: &Connection) -> Result<BTreeSet<i64>, CoreError> {
    let mut statement = connection
        .prepare("SELECT id FROM notes WHERE eligible=1")
        .map_err(|_| CoreError::Database)?;
    statement
        .query_map([], |row| row.get::<_, i64>(0))
        .map_err(|_| CoreError::Database)?
        .collect::<Result<BTreeSet<_>, _>>()
        .map_err(|_| CoreError::Database)
}

fn statement_has_descendant(connection: &Connection, tag: &str) -> Result<bool, CoreError> {
    connection
        .query_row(
            "SELECT EXISTS(SELECT 1 FROM tags WHERE substr(name, 1, length(?1) + 1) = ?1 || '/')",
            [tag],
            |row| row.get(0),
        )
        .map_err(|_| CoreError::Database)
}

fn note_ids_for_tag(connection: &Connection, tag: &str) -> Result<BTreeSet<i64>, CoreError> {
    let mut statement = connection
        .prepare(
            "SELECT nt.note_id FROM note_tags nt JOIN tags t ON nt.tag_id=t.id WHERE t.name=?1",
        )
        .map_err(|_| CoreError::Database)?;
    statement
        .query_map([tag], |row| row.get::<_, i64>(0))
        .map_err(|_| CoreError::Database)?
        .collect::<Result<BTreeSet<_>, _>>()
        .map_err(|_| CoreError::Database)
}

struct CompiledRule {
    group: usize,
    prefix: String,
    wildcard: bool,
    split_deep: bool,
}

struct CategoryGroup {
    name: String,
    path: String,
    whole_tag: Option<String>,
}

struct CategoryBucket {
    path: String,
    display_name: String,
    whole_tag: Option<String>,
    options: BTreeMap<String, String>,
    strip_prefix: String,
}

impl CategoryBucket {
    fn option_name(&self, tag: &str) -> String {
        tag.strip_prefix(self.strip_prefix.as_str())
            .unwrap_or(tag)
            .to_owned()
    }
}

struct CategoryPlan {
    rules: Vec<CompiledRule>,
    groups: Vec<CategoryGroup>,
    other: Option<(String, bool)>,
}

impl CategoryPlan {
    fn new(settings: &TagCategorySettings) -> Self {
        let mut groups: Vec<CategoryGroup> = Vec::new();
        let mut rule_counts: Vec<usize> = Vec::new();
        let mut rules = Vec::with_capacity(settings.categories.len());
        for rule in &settings.categories {
            let wildcard = rule.path.ends_with("/*") || rule.path == "*";
            let prefix = rule
                .path
                .strip_suffix("/*")
                .or_else(|| rule.path.strip_suffix('*'))
                .unwrap_or(&rule.path)
                .to_owned();
            let group = match groups.iter().position(|group| group.name == rule.name) {
                Some(index) => index,
                None => {
                    groups.push(CategoryGroup {
                        name: rule.name.clone(),
                        path: if prefix.is_empty() {
                            "*".to_owned()
                        } else {
                            prefix.clone()
                        },
                        whole_tag: None,
                    });
                    rule_counts.push(0);
                    groups.len() - 1
                }
            };
            rule_counts[group] += 1;
            if wildcard && !prefix.is_empty() && !rule.split_deep {
                groups[group].whole_tag = Some(prefix.clone());
            }
            rules.push(CompiledRule {
                group,
                prefix,
                wildcard,
                split_deep: rule.split_deep,
            });
        }
        for (group, count) in groups.iter_mut().zip(rule_counts) {
            if count != 1 {
                group.whole_tag = None;
            }
        }
        Self {
            rules,
            groups,
            other: settings
                .other
                .enabled
                .then(|| (settings.other.name.clone(), settings.other.split_deep)),
        }
    }

    fn best_rule(&self, tag: &str, filter_mode: bool) -> Option<usize> {
        self.rules
            .iter()
            .enumerate()
            .filter_map(|(index, rule)| {
                let matches = if rule.wildcard {
                    rule.prefix.is_empty()
                        || (filter_mode && tag == rule.prefix)
                        || tag
                            .strip_prefix(rule.prefix.as_str())
                            .is_some_and(|rest| rest.starts_with('/'))
                } else {
                    tag == rule.prefix
                };
                let specificity = rule
                    .prefix
                    .split('/')
                    .filter(|part| !part.is_empty())
                    .count()
                    * 2
                    + usize::from(!rule.wildcard);
                matches.then_some((specificity, usize::MAX - index, index))
            })
            .max()
            .map(|(_, _, index)| index)
    }

    /// The category path a filter belongs to. A filter is either a leaf tag or
    /// a tag prefix selecting a whole subtree.
    fn path_for_filter(
        &self,
        connection: &Connection,
        filter: &str,
    ) -> Result<Option<String>, CoreError> {
        let parent_path = |connection: &Connection| -> Result<Option<String>, CoreError> {
            if statement_has_descendant(connection, filter)? {
                Ok(Some(filter.to_owned()))
            } else {
                Ok(filter.rsplit_once('/').map(|(parent, _)| parent.to_owned()))
            }
        };
        let Some(index) = self.best_rule(filter, true) else {
            let Some((_, split_deep)) = &self.other else {
                return Ok(None);
            };
            if *split_deep && let Some(parent) = parent_path(connection)? {
                return Ok(Some(parent));
            }
            return Ok(Some("@other".to_owned()));
        };
        let rule = &self.rules[index];
        if rule.wildcard
            && rule.split_deep
            && let Some(parent) = parent_path(connection)?
            && parent != rule.prefix
            && parent.starts_with(&format!("{}/", rule.prefix))
        {
            return Ok(Some(parent));
        }
        Ok(Some(self.groups[rule.group].path.clone()))
    }

    fn bucket_for(&self, tag: &str) -> Option<(usize, CategoryBucket)> {
        let Some(index) = self.best_rule(tag, false) else {
            let (name, split_deep) = self.other.as_ref()?;
            return Some((usize::MAX, other_bucket(name, *split_deep, tag)));
        };
        let rule = &self.rules[index];
        let group = &self.groups[rule.group];
        let relative = if rule.wildcard && !rule.prefix.is_empty() {
            tag.strip_prefix(rule.prefix.as_str())
                .and_then(|rest| rest.strip_prefix('/'))
                .unwrap_or(tag)
        } else {
            tag
        };
        if rule.wildcard && rule.split_deep && relative.contains('/') {
            let (parent, _) = tag.rsplit_once('/')?;
            let shown = parent
                .strip_prefix(rule.prefix.as_str())
                .and_then(|rest| rest.strip_prefix('/'))
                .unwrap_or(parent);
            return Some((
                rule.group,
                CategoryBucket {
                    path: parent.to_owned(),
                    display_name: format!("{} / {shown}", group.name),
                    whole_tag: Some(parent.to_owned()),
                    options: BTreeMap::new(),
                    strip_prefix: format!("{parent}/"),
                },
            ));
        }
        Some((
            rule.group,
            CategoryBucket {
                path: group.path.clone(),
                display_name: group.name.clone(),
                whole_tag: group.whole_tag.clone(),
                options: BTreeMap::new(),
                strip_prefix: if rule.wildcard && !rule.prefix.is_empty() {
                    format!("{}/", rule.prefix)
                } else {
                    tag.rsplit_once('/')
                        .map(|(parent, _)| format!("{parent}/"))
                        .unwrap_or_default()
                },
            },
        ))
    }
}

fn other_bucket(name: &str, split_deep: bool, tag: &str) -> CategoryBucket {
    if split_deep && let Some((parent, _)) = tag.rsplit_once('/') {
        return CategoryBucket {
            path: parent.to_owned(),
            display_name: parent.to_owned(),
            whole_tag: Some(parent.to_owned()),
            options: BTreeMap::new(),
            strip_prefix: format!("{parent}/"),
        };
    }
    CategoryBucket {
        path: "@other".to_owned(),
        display_name: name.to_owned(),
        whole_tag: None,
        options: BTreeMap::new(),
        strip_prefix: String::new(),
    }
}

fn parse_note_search_query(query: &str) -> (String, Vec<String>, Vec<String>, Vec<String>) {
    let mut text = Vec::new();
    let mut any_tags = Vec::new();
    let mut all_tags = Vec::new();
    let mut excluded_tags = Vec::new();
    for term in query.split_whitespace() {
        if let Some(tag) = term.strip_prefix("&#").filter(|tag| !tag.is_empty()) {
            all_tags.push(tag.to_owned());
        } else if let Some(tag) = term.strip_prefix("-#").filter(|tag| !tag.is_empty()) {
            excluded_tags.push(tag.to_owned());
        } else if let Some(tag) = term.strip_prefix('#').filter(|tag| !tag.is_empty()) {
            any_tags.push(tag.to_owned());
        } else {
            text.push(term);
        }
    }
    (text.join(" "), any_tags, all_tags, excluded_tags)
}

fn fuzzy_note_match(title: &str, path: &str, query: &str) -> bool {
    let title = title.chars().take(512).collect::<String>().to_lowercase();
    let path = path.chars().take(512).collect::<String>().to_lowercase();
    query.split_whitespace().all(|term| {
        let term = term.to_lowercase();
        if title.contains(&term) || path.contains(&term) {
            return true;
        }
        let tolerance = match term.chars().count() {
            0..=2 => 0,
            3..=5 => 1,
            _ => 2,
        };
        title
            .split(|character: char| !character.is_alphanumeric())
            .chain(path.split(|character: char| !character.is_alphanumeric()))
            .any(|word| fuzzy_word_match(word, &term, tolerance))
    })
}

fn fuzzy_word_match(word: &str, term: &str, tolerance: usize) -> bool {
    if levenshtein_within(word, term, tolerance) {
        return true;
    }
    let word = word.chars().take(512).collect::<Vec<_>>();
    let term = term.chars().take(128).collect::<Vec<_>>();
    let min_length = term.len().saturating_sub(tolerance).max(1);
    let max_length = term.len().saturating_add(tolerance).min(word.len());
    let term = term.iter().collect::<String>();
    (min_length..=max_length).any(|length| {
        word.windows(length).any(|window| {
            let candidate = window.iter().collect::<String>();
            levenshtein_within(&candidate, &term, tolerance)
        })
    })
}

fn levenshtein_within(left: &str, right: &str, tolerance: usize) -> bool {
    if left
        .chars()
        .take(
            right
                .chars()
                .count()
                .saturating_add(tolerance)
                .saturating_add(1),
        )
        .count()
        > right.chars().count().saturating_add(tolerance)
    {
        return false;
    }
    let left = left.chars().collect::<Vec<_>>();
    let right = right.chars().collect::<Vec<_>>();
    if left.len().abs_diff(right.len()) > tolerance {
        return false;
    }
    let mut previous = (0..=right.len()).collect::<Vec<_>>();
    let mut current = vec![0; right.len() + 1];
    for (left_index, left_char) in left.iter().enumerate() {
        current[0] = left_index + 1;
        let mut row_min = current[0];
        for (right_index, right_char) in right.iter().enumerate() {
            current[right_index + 1] = (previous[right_index + 1] + 1)
                .min(current[right_index] + 1)
                .min(previous[right_index] + usize::from(left_char != right_char));
            row_min = row_min.min(current[right_index + 1]);
        }
        if row_min > tolerance {
            return false;
        }
        std::mem::swap(&mut previous, &mut current);
    }
    previous[right.len()] <= tolerance
}

fn fuzzy_tag_match_score(tag: &str, query: &str) -> Option<usize> {
    let tag_parts = tag
        .to_lowercase()
        .split('/')
        .map(str::to_owned)
        .collect::<Vec<_>>();
    let query_parts = query
        .chars()
        .take(128)
        .collect::<String>()
        .to_lowercase()
        .split('/')
        .filter(|part| !part.is_empty())
        .map(str::to_owned)
        .collect::<Vec<_>>();
    if query_parts.is_empty() {
        return None;
    }
    query_parts.iter().try_fold(0usize, |score, query_part| {
        let best_score = tag_parts
            .iter()
            .filter_map(|tag_part| {
                if tag_part == query_part {
                    return Some(0);
                }
                if tag_part.starts_with(query_part) {
                    return Some(1);
                }
                let tolerance = match query_part.chars().count() {
                    0..=2 => 0,
                    3..=5 => 1,
                    _ => 2,
                };
                let distance = levenshtein_distance(tag_part, query_part);
                (distance <= tolerance).then_some(distance + 1)
            })
            .min()?;
        Some(score.saturating_add(best_score))
    })
}

fn levenshtein_distance(left: &str, right: &str) -> usize {
    let left = left.chars().take(128).collect::<Vec<_>>();
    let right = right.chars().take(128).collect::<Vec<_>>();
    let mut previous = (0..=right.len()).collect::<Vec<_>>();
    let mut current = vec![0; right.len() + 1];
    for (left_index, left_char) in left.iter().enumerate() {
        current[0] = left_index + 1;
        for (right_index, right_char) in right.iter().enumerate() {
            current[right_index + 1] = (previous[right_index + 1] + 1)
                .min(current[right_index] + 1)
                .min(previous[right_index] + usize::from(left_char != right_char));
        }
        std::mem::swap(&mut previous, &mut current);
    }
    previous[right.len()]
}

fn note_ids_for_virtual_filter(
    connection: &Connection,
    filter: VirtualFilter,
) -> Result<BTreeSet<i64>, CoreError> {
    let condition = match filter {
        VirtualFilter::MultipleMedia => "media_count >= 2",
        VirtualFilter::HasMemo => "has_memo != 0",
        VirtualFilter::HasVideo => "video_count >= 1",
        VirtualFilter::HasRelated => "has_related != 0",
    };
    let query = format!("SELECT id FROM notes WHERE eligible=1 AND {condition}");
    let mut statement = connection
        .prepare(&query)
        .map_err(|_| CoreError::Database)?;
    statement
        .query_map([], |row| row.get::<_, i64>(0))
        .map_err(|_| CoreError::Database)?
        .collect::<Result<BTreeSet<_>, _>>()
        .map_err(|_| CoreError::Database)
}

/// A single note that did not become a gallery item, with the reason (no
/// paths/tags/titles are logged elsewhere; this is an explicit, opt-in,
/// local-only diagnostic for the person running the CLI against their own
/// Vault, per docs/design.md §15.4).
#[derive(Debug, Clone)]
pub struct NoteDiagnostic {
    pub path: String,
    pub reason: String,
}

/// Walks the Vault like `scan` does, but reports every `.md` file that would
/// not end up in the gallery (read failures, size/parse errors, or a parsed
/// note without a `source/` tag) together with the reason. Does not touch the
/// index; intended for `gallery-cli diagnose` to let a person find out why
/// their note count differs from what they expect.
pub fn diagnose_notes(vault: &Path) -> Result<Vec<NoteDiagnostic>, CoreError> {
    let root = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
    if !root.is_dir() {
        return Err(CoreError::InvalidVault);
    }
    let (files, _traversal_warnings) = collect_notes(&root)?;
    let mut diagnostics = Vec::new();
    for path in files {
        let Some(relative) = path
            .strip_prefix(&root)
            .ok()
            .and_then(|path| path.to_str())
            .map(|path| path.replace('\\', "/"))
        else {
            continue;
        };
        let metadata = match fs::metadata(&path) {
            Ok(metadata) => metadata,
            Err(_) => {
                diagnostics.push(NoteDiagnostic {
                    path: relative,
                    reason: "ファイルを読み込めません".to_owned(),
                });
                continue;
            }
        };
        if metadata.len() > gallery_parse::MAX_NOTE_BYTES as u64 {
            diagnostics.push(NoteDiagnostic {
                path: relative,
                reason: "ファイルサイズの上限を超えています".to_owned(),
            });
            continue;
        }
        let content = match fs::read_to_string(&path) {
            Ok(content) => content,
            Err(_) => {
                diagnostics.push(NoteDiagnostic {
                    path: relative,
                    reason: "文字コードが UTF-8 ではないか、読み込めません".to_owned(),
                });
                continue;
            }
        };
        match parse_note(&content) {
            Ok(parsed) => {
                if !parsed.tags.iter().any(|tag| tag.starts_with("source/")) {
                    diagnostics.push(NoteDiagnostic {
                        path: relative,
                        reason: "source/ から始まるタグがありません".to_owned(),
                    });
                }
            }
            Err(error) => diagnostics.push(NoteDiagnostic {
                path: relative,
                reason: error.to_string(),
            }),
        }
    }
    Ok(diagnostics)
}

fn collect_notes(root: &Path) -> Result<(Vec<PathBuf>, usize), CoreError> {
    let mut pending = vec![root.to_path_buf()];
    let mut notes = Vec::new();
    let mut warnings = 0;
    while let Some(directory) = pending.pop() {
        let entries = match fs::read_dir(&directory) {
            Ok(entries) => entries,
            Err(_) if directory == root => return Err(CoreError::Io),
            Err(_) => {
                warnings += 1;
                continue;
            }
        };
        for entry in entries {
            let entry = match entry {
                Ok(entry) => entry,
                Err(_) => {
                    warnings += 1;
                    continue;
                }
            };
            let file_type = match entry.file_type() {
                Ok(file_type) => file_type,
                Err(_) => {
                    warnings += 1;
                    continue;
                }
            };
            let name = entry.file_name().to_string_lossy().to_string();
            if ignored_name(&name) {
                continue;
            }
            if file_type.is_symlink() {
                continue;
            }
            if file_type.is_dir() {
                pending.push(entry.path());
            } else if file_type.is_file()
                && entry
                    .path()
                    .extension()
                    .is_some_and(|ext| ext.eq_ignore_ascii_case("md"))
            {
                notes.push(entry.path());
            }
        }
    }
    notes.sort();
    Ok((notes, warnings))
}

fn ignored_name(name: &str) -> bool {
    name.starts_with('.')
        || matches!(
            name,
            ".obsidian" | ".trash" | ".stfolder" | ".stversions" | ".stignore"
        )
        || name.contains(".sync-conflict-")
        || (name.starts_with(".syncthing.") && name.ends_with(".tmp"))
        || name.starts_with("~syncthing~")
}

fn load_tag_settings(
    database_path: &Path,
) -> Result<(NoteStructureSettings, TagCategorySettings), CoreError> {
    let Some(contents) = read_tag_settings_file(database_path)? else {
        return Ok((
            NoteStructureSettings::default(),
            TagCategorySettings::default(),
        ));
    };
    Ok((
        parse_note_structure_settings(&contents).map_err(|_| CoreError::Database)?,
        parse_tag_category_settings(&contents).map_err(|_| CoreError::Database)?,
    ))
}

fn read_tag_settings_file(database_path: &Path) -> Result<Option<String>, CoreError> {
    let settings_path = database_path
        .parent()
        .ok_or(CoreError::Database)?
        .join("tag-settings.json");
    let metadata = match fs::symlink_metadata(&settings_path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
            return Ok(None);
        }
        Err(_) => return Err(CoreError::Database),
    };
    if !metadata.file_type().is_file() || metadata.len() > gallery_parse::MAX_SETTINGS_BYTES as u64
    {
        return Err(CoreError::Database);
    }
    #[cfg(unix)]
    {
        use std::os::unix::fs::MetadataExt;
        if metadata.nlink() > 1 {
            return Err(CoreError::Database);
        }
    }
    let canonical_settings = fs::canonicalize(&settings_path).map_err(|_| CoreError::Database)?;
    let canonical_parent = fs::canonicalize(settings_path.parent().ok_or(CoreError::Database)?)
        .map_err(|_| CoreError::Database)?;
    if canonical_settings.parent() != Some(canonical_parent.as_path()) {
        return Err(CoreError::Database);
    }
    fs::read_to_string(canonical_settings)
        .map(Some)
        .map_err(|_| CoreError::Database)
}

fn read_parse_note(
    path: &Path,
    settings: &NoteStructureSettings,
) -> Result<ParsedNote, &'static str> {
    let metadata = fs::metadata(path).map_err(|_| "read")?;
    if metadata.len() > gallery_parse::MAX_NOTE_BYTES as u64 {
        return Err("oversized");
    }
    let file = File::open(path).map_err(|_| "read")?;
    let mut content = String::with_capacity(metadata.len() as usize);
    file.take((gallery_parse::MAX_NOTE_BYTES + 1) as u64)
        .read_to_string(&mut content)
        .map_err(|_| "read")?;
    parse_note_content(&content, settings)
}

fn parse_note_bytes(
    content: &[u8],
    settings: &NoteStructureSettings,
) -> Result<ParsedNote, &'static str> {
    if content.len() > gallery_parse::MAX_NOTE_BYTES {
        return Err("oversized");
    }
    let content = std::str::from_utf8(content).map_err(|_| "read")?;
    parse_note_content(content, settings)
}

fn parse_note_content(
    content: &str,
    settings: &NoteStructureSettings,
) -> Result<ParsedNote, &'static str> {
    parse_note_for_link_target(content, settings).map_err(|error| match error {
        gallery_parse::ParseError::NoteTooLarge
        | gallery_parse::ParseError::FrontmatterTooLarge
        | gallery_parse::ParseError::TooManyTags => "limit",
        _ => "parse",
    })
}

fn insert_note(
    transaction: &Transaction<'_>,
    note: &ParsedNote,
    context: NoteInsertContext<'_>,
) -> Result<(), CoreError> {
    let NoteInsertContext {
        relative,
        root,
        mtime,
        size,
        eligible,
        available_files,
    } = context;
    let filename = Path::new(relative)
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or(CoreError::Database)?;
    let videos = note
        .media
        .iter()
        .filter(|media| media.kind == MediaKind::Video)
        .count();
    transaction
        .execute(
            "INSERT INTO notes(path,title,url,published,created,updated,media_count,video_count,has_memo,has_related,memo_count,related_count,mtime,size,eligible,filename)
             VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16)",
            params![relative, note.title, note.url, note.published, note.created, note.updated, note.media.len() as i64, videos as i64, !note.memo_lines.is_empty(), !note.related_lines.is_empty(), note.memo_lines.len() as i64, note.related_lines.len() as i64, mtime, size, eligible, filename],
        )
        .map_err(|_| CoreError::Database)?;
    let note_id = transaction.last_insert_rowid();
    if eligible {
        for tag in expanded_tags(&note.tags) {
            transaction
                .execute("INSERT OR IGNORE INTO tags(name) VALUES (?1)", [&tag])
                .map_err(|_| CoreError::Database)?;
            let tag_id: i64 = transaction
                .query_row("SELECT id FROM tags WHERE name=?1", [&tag], |row| {
                    row.get(0)
                })
                .map_err(|_| CoreError::Database)?;
            transaction
                .execute(
                    "INSERT INTO note_tags(note_id,tag_id) VALUES (?1,?2)",
                    params![note_id, tag_id],
                )
                .map_err(|_| CoreError::Database)?;
        }
    }
    if eligible {
        for (ordinal, media) in note.media.iter().enumerate() {
            let exists = if let Some(available_files) = available_files {
                resolve_media_relative_path(relative, &media.path)
                    .is_some_and(|path| available_files.contains(&path))
            } else {
                safe_media_exists(root, relative, &media.path)
            };
            transaction
                .execute(
                    "INSERT INTO media(note_id,ord,rel_path,kind,exists_flag) VALUES (?1,?2,?3,?4,?5)",
                    params![
                        note_id,
                        ordinal as i64,
                        media.path,
                        if media.kind == MediaKind::Video {
                            "video"
                        } else {
                            "image"
                        },
                        exists
                    ],
                )
                .map_err(|_| CoreError::Database)?;
        }
    }
    Ok(())
}

fn resolve_media_relative_path(note_path: &str, media_path: &str) -> Option<String> {
    let parent = Path::new(note_path)
        .parent()
        .unwrap_or_else(|| Path::new(""));
    normalize_note_path(&parent.join(media_path).to_string_lossy())
}

fn relative_path_is_ignored(path: &str) -> bool {
    Path::new(path)
        .components()
        .filter_map(|component| match component {
            Component::Normal(name) => name.to_str(),
            _ => None,
        })
        .any(ignored_name)
}

fn refresh_media_existence(transaction: &Transaction<'_>, root: &Path) -> Result<(), CoreError> {
    let media_paths = {
        let mut statement = transaction
            .prepare(
                "SELECT media.id, notes.path, media.rel_path, media.exists_flag
                 FROM media JOIN notes ON media.note_id=notes.id",
            )
            .map_err(|_| CoreError::Database)?;
        let rows = statement
            .query_map([], |row| {
                Ok((
                    row.get::<_, i64>(0)?,
                    row.get::<_, String>(1)?,
                    row.get::<_, String>(2)?,
                    row.get::<_, bool>(3)?,
                ))
            })
            .map_err(|_| CoreError::Database)?;
        rows.collect::<Result<Vec<_>, _>>()
            .map_err(|_| CoreError::Database)?
    };
    let mut update = transaction
        .prepare("UPDATE media SET exists_flag=?1 WHERE id=?2 AND exists_flag != ?1")
        .map_err(|_| CoreError::Database)?;
    for (media_id, note_path, media_path, was_present) in media_paths {
        let exists = safe_media_exists(root, &note_path, &media_path);
        if exists != was_present {
            update
                .execute(params![exists, media_id])
                .map_err(|_| CoreError::Database)?;
        }
    }
    Ok(())
}

fn safe_media_exists(root: &Path, note_path: &str, media_path: &str) -> bool {
    resolve_media(root, note_path, media_path).is_some()
}

fn resolve_media(root: &Path, note_path: &str, media_path: &str) -> Option<PathBuf> {
    let relative_note = Path::new(note_path);
    let parent = relative_note.parent()?;
    let requested = Path::new(media_path);
    if requested.is_absolute()
        || requested
            .components()
            .any(|component| matches!(component, Component::Prefix(_) | Component::RootDir))
    {
        return None;
    }
    let joined = root.join(parent).join(requested);
    let canonical = fs::canonicalize(joined).ok()?;
    (canonical.starts_with(root) && canonical.is_file()).then_some(canonical)
}

fn prepare_thumbnail_cache(cache_root: &Path, vault_root: &Path) -> Result<PathBuf, CoreError> {
    let canonical = ensure_private_child_directory(cache_root, Some(vault_root))?;
    set_private_directory(&canonical)?;
    Ok(canonical)
}

fn ensure_private_child_directory(
    directory: &Path,
    vault_root: Option<&Path>,
) -> Result<PathBuf, CoreError> {
    let absolute = if directory.is_absolute() {
        directory.to_path_buf()
    } else {
        std::env::current_dir()
            .map_err(|_| CoreError::Io)?
            .join(directory)
    };
    let components = absolute.components().collect::<Vec<_>>();
    if components
        .iter()
        .any(|component| matches!(component, Component::CurDir | Component::ParentDir))
    {
        return Err(CoreError::Io);
    }
    let name = absolute.file_name().ok_or(CoreError::Io)?.to_os_string();
    let mut ancestor = absolute.parent().ok_or(CoreError::Io)?.to_path_buf();
    let mut missing = Vec::new();
    let mut canonical_ancestor = loop {
        match fs::canonicalize(&ancestor) {
            Ok(canonical) => break canonical,
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
                missing.push(ancestor.file_name().ok_or(CoreError::Io)?.to_os_string());
                ancestor = ancestor.parent().ok_or(CoreError::Io)?.to_path_buf();
            }
            Err(_) => return Err(CoreError::Io),
        }
    };
    let mut requested = canonical_ancestor.clone();
    for component in missing.iter().rev().chain(std::iter::once(&name)) {
        requested.push(component);
    }
    if vault_root.is_some_and(|vault_root| requested.starts_with(vault_root)) {
        return Err(CoreError::Io);
    }
    for component in missing.iter().rev().chain(std::iter::once(&name)) {
        canonical_ancestor.push(component);
        match fs::symlink_metadata(&canonical_ancestor) {
            Ok(metadata) if metadata.file_type().is_dir() => {}
            Ok(_) => return Err(CoreError::Io),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
                fs::create_dir(&canonical_ancestor).map_err(|_| CoreError::Io)?;
            }
            Err(_) => return Err(CoreError::Io),
        }
        canonical_ancestor = fs::canonicalize(&canonical_ancestor).map_err(|_| CoreError::Io)?;
        if vault_root.is_some_and(|vault_root| canonical_ancestor.starts_with(vault_root)) {
            return Err(CoreError::Io);
        }
    }
    Ok(canonical_ancestor)
}

fn set_private_directory(path: &Path) -> Result<(), CoreError> {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(path, fs::Permissions::from_mode(0o700)).map_err(|_| CoreError::Io)?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn temp_dir() -> PathBuf {
        let id = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("clock")
            .as_nanos();
        std::env::temp_dir().join(format!("gallery-core-{id}"))
    }

    /// Opens a gallery that treats every `source/` note as eligible unless the
    /// test wrote its own settings file.
    fn open_gallery(root: &Path, database: &Path) -> Result<Gallery, CoreError> {
        let settings = database
            .parent()
            .expect("database parent")
            .join("tag-settings.json");
        if !settings.exists() {
            fs::create_dir_all(settings.parent().expect("parent")).expect("create settings dir");
            fs::write(
                &settings,
                r#"{"noteStructure":{"galleryTagPrefixes":["source/"]}}"#,
            )
            .expect("write test settings");
        }
        Gallery::open(root, database)
    }

    #[test]
    fn fuzzy_note_search_handles_cjk_titles() {
        assert!(fuzzy_note_match(
            "雨上がりの観測",
            "雨上がりの観測.md",
            "雨上がりの観測"
        ));
        assert!(fuzzy_note_match(
            "雨上がりの観測",
            "雨上がりの観測.md",
            "雨上か"
        ));
        assert!(!fuzzy_note_match(
            "雨上がりの観測",
            "雨上がりの観測.md",
            "夕暮れ"
        ));
    }

    #[test]
    fn categories_return_tags_for_user_configurable_visibility() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/art, copyright/pretty-series]\n---\n# One\n",
        )
        .expect("write note");
        fs::write(
            root.join("two.md"),
            "---\ntags: [source/type/animal, copyright/onepeace]\n---\n# Two\n",
        )
        .expect("write second note");
        fs::write(
            root.join("three.md"),
            "---\ntags: [source/type/animal, copyright/pretty-series, copyright/onepeace]\n---\n# Three\n",
        )
        .expect("write note with both copyright tags");
        fs::write(
            root.join("four.md"),
            "---\ntags: [source/type/other, copyright/unrelated]\n---\n# Four\n",
        )
        .expect("write note with unrelated copyright tag");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        let report = gallery.scan().expect("scan");
        assert_eq!(report.notes_indexed, 4);
        let categories = gallery.categories().expect("categories");
        assert!(
            categories
                .iter()
                .find(|category| category.display_name == "ソース")
                .is_some_and(|category| category.options.iter().any(|o| o.name == "art"))
        );
        assert!(
            categories
                .iter()
                .any(|category| category.path == "copyright"
                    && category.options.iter().any(|o| o.name == "pretty-series"))
        );
        assert_eq!(
            gallery
                .query(&["source/art".into()], 10)
                .expect("query")
                .len(),
            1
        );
        let filtered_categories = gallery
            .categories_with_filters(&["source/art".into()], &[], &[])
            .expect("filtered categories");
        let copyright = filtered_categories
            .iter()
            .find(|category| category.path == "copyright")
            .expect("copyright category");
        let unmatched_copyright = copyright
            .options
            .iter()
            .find(|option| option.full_tag == "copyright/onepeace")
            .expect("unmatched copyright");
        assert_eq!(unmatched_copyright.count, 0);
        assert!(unmatched_copyright.disabled);
        let both_copyrights = gallery
            .query_filtered_page_search_with_all(
                &[],
                &[
                    "copyright/pretty-series".into(),
                    "copyright/onepeace".into(),
                ],
                &[],
                &[],
                "",
                0,
                10,
            )
            .expect("AND tags")
            .into_iter()
            .map(|note| note.path)
            .collect::<Vec<_>>();
        assert_eq!(both_copyrights, ["three.md"]);
        let and_categories = gallery
            .categories_with_filter_modes(
                &[],
                &[
                    "copyright/pretty-series".into(),
                    "copyright/onepeace".into(),
                ],
                &[],
                &[],
            )
            .expect("categories for required copyright tags");
        let unrelated = and_categories
            .iter()
            .find(|category| category.path == "copyright")
            .expect("copyright category")
            .options
            .iter()
            .find(|option| option.full_tag == "copyright/unrelated")
            .expect("unrelated copyright tag");
        assert_eq!(unrelated.count, 0);
        assert!(unrelated.disabled);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn note_structure_settings_reparse_custom_headings_and_virtual_filters() {
        let root = temp_dir();
        let app_data = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::create_dir_all(&app_data).expect("create app data");
        fs::write(
            root.join("custom.md"),
            "---\ntags: [collection/example]\n---\n# Custom note\nA fictional post.\n## Later\n- keep the blue subtle\n## Sources\n- [study](https://example.invalid/study)\n## End of post\nThis is not post text.\n",
        )
        .expect("write fictional note");
        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"galleryTagPrefixes":["collection/"],"memoHeadings":["Later"],"relatedHeadings":["Sources"],"postTextEndHeadings":["End of post"]}}"#,
        )
        .expect("write custom note structure settings");
        let database = app_data.join("index.sqlite");
        let mut gallery = open_gallery(&root, &database).expect("open with settings");
        gallery.scan().expect("scan with custom structure");
        let note = gallery.query(&[], 10).expect("query").remove(0);
        let detail = gallery
            .note_detail(note.id)
            .expect("read detail")
            .expect("note exists");
        assert_eq!(detail.body_text, "A fictional post.");
        assert_eq!(detail.memo_lines[0].text, "keep the blue subtle");
        assert_eq!(detail.related_lines[0].text, "study");

        let memo_notes = gallery
            .query_filtered_page(&[], &[VirtualFilter::HasMemo], 0, 10)
            .expect("memo filter");
        assert_eq!(memo_notes.len(), 1);

        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"galleryTagPrefixes":["collection/"],"memoHeadings":["Task list"],"relatedHeadings":["Sources"],"postTextEndHeadings":["End of post"]}}"#,
        )
        .expect("change note headings");
        drop(gallery);
        let mut gallery = open_gallery(&root, &database).expect("reopen with settings");
        gallery.scan().expect("rescan after config change");
        let note = gallery.query(&[], 10).expect("query").remove(0);
        let detail = gallery
            .note_detail(note.id)
            .expect("read refreshed detail")
            .expect("note exists");
        assert!(detail.memo_lines.is_empty());
        assert_eq!(
            gallery
                .query_filtered_page(&[], &[VirtualFilter::HasMemo], 0, 10)
                .expect("updated memo filter")
                .len(),
            0
        );
        fs::remove_dir_all(root).expect("remove vault");
        fs::remove_dir_all(app_data).expect("remove app data");
    }

    #[test]
    fn link_resolution_modes_use_vault_bounded_shortest_relative_and_absolute_paths() {
        let root = temp_dir();
        let app_data = temp_dir();
        fs::create_dir_all(root.join("folder/nested")).expect("create nested notes");
        fs::create_dir_all(root.join("archive")).expect("create archive notes");
        fs::create_dir_all(&app_data).expect("create app data");
        fs::write(
            root.join("folder/nested/source.md"),
            "---\ntags: [source/example]\n---\n# Source\n## Related\n- [[target]]\n- [[Nearby target]]\n",
        )
        .expect("write source");
        fs::write(
            root.join("folder/target.md"),
            "---\ntags: [source/example]\n---\n# Nearby target\n",
        )
        .expect("write nearby target");
        fs::write(
            root.join("archive/target.md"),
            "---\ntags: [source/example]\n---\n# Distant target\n",
        )
        .expect("write distant target");
        let database = app_data.join("index.sqlite");
        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"memoHeadings":["Memo"],"relatedHeadings":["Related"],"postTextEndHeadings":["Details"],"linkResolution":"shortestPath","galleryTagPrefixes":["source/"]}}"#,
        )
        .expect("write shortest path setting");
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let source = gallery
            .query(&[], 10)
            .expect("query")
            .into_iter()
            .find(|note| note.path.ends_with("source.md"))
            .expect("source note");
        let detail = gallery
            .note_detail(source.id)
            .expect("detail")
            .expect("source detail");
        let nearest = gallery
            .query(&[], 10)
            .expect("query nearest")
            .into_iter()
            .find(|note| note.path == "folder/target.md")
            .expect("nearest note");
        assert_eq!(detail.related_lines[0].linked_note_id, Some(nearest.id));
        assert_eq!(detail.related_lines[1].linked_note_id, Some(nearest.id));
        drop(gallery);

        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"memoHeadings":["Memo"],"relatedHeadings":["Related"],"postTextEndHeadings":["Details"],"linkResolution":"relativePath","galleryTagPrefixes":["source/"]}}"#,
        )
        .expect("write relative setting");
        let gallery = open_gallery(&root, &database).expect("reopen relative");
        let detail = gallery
            .note_detail(source.id)
            .expect("relative detail")
            .expect("source detail");
        assert_eq!(detail.related_lines[0].linked_note_id, Some(nearest.id));
        assert_eq!(detail.related_lines[1].linked_note_id, Some(nearest.id));
        drop(gallery);

        fs::write(
            root.join("folder/nested/source.md"),
            "---\ntags: [source/example]\n---\n# Source\n## Related\n- [[/target]]\n",
        )
        .expect("replace source with vault-absolute link");
        fs::write(
            root.join("target.md"),
            "---\ntags: [source/example]\n---\n# Root target\n",
        )
        .expect("write root target");
        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"memoHeadings":["Memo"],"relatedHeadings":["Related"],"postTextEndHeadings":["Details"],"linkResolution":"absolutePath","galleryTagPrefixes":["source/"]}}"#,
        )
        .expect("write absolute setting");
        let mut gallery = open_gallery(&root, &database).expect("reopen absolute");
        gallery.scan().expect("rescan absolute");
        let source = gallery
            .query(&[], 10)
            .expect("query source")
            .into_iter()
            .find(|note| note.path.ends_with("source.md"))
            .expect("source note");
        let root_target = gallery
            .query(&[], 10)
            .expect("query root")
            .into_iter()
            .find(|note| note.path == "target.md")
            .expect("root target");
        let detail = gallery
            .note_detail(source.id)
            .expect("absolute detail")
            .expect("source detail");
        assert_eq!(detail.related_lines[0].linked_note_id, Some(root_target.id));
        fs::remove_dir_all(root).expect("remove vault");
        fs::remove_dir_all(app_data).expect("remove app data");
    }

    #[test]
    fn indexes_only_notes_with_source_tags() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("gallery-note.md"),
            "---\ntags: [source/rating/safe]\n---\n# Gallery note\n",
        )
        .expect("write gallery note");
        fs::write(
            root.join("unrelated-note.md"),
            "---\ntags: [moc]\n---\n# Unrelated note\n",
        )
        .expect("write unrelated note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");

        let report = gallery.scan().expect("scan");
        let notes = gallery.query(&[], 10).expect("query");

        assert_eq!(report.notes_indexed, 1);
        assert_eq!(report.warnings, 0);
        assert_eq!(notes.len(), 1);
        assert_eq!(notes[0].path, "gallery-note.md");

        drop(gallery);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn ignores_symlinked_directories_and_hidden_files() {
        let root = temp_dir();
        let outside = temp_dir().with_extension("outside");
        fs::create_dir_all(&root).expect("create root");
        fs::create_dir_all(&outside).expect("create outside");
        fs::write(outside.join("secret.md"), "---\ntags: [source/a]\n---\n")
            .expect("write outside");
        #[cfg(unix)]
        std::os::unix::fs::symlink(&outside, root.join("linked")).expect("symlink");
        fs::write(root.join(".hidden.md"), "---\ntags: [source/a]\n---\n").expect("hidden");
        let (files, warnings) = collect_notes(&root).expect("scan paths");
        assert!(files.is_empty());
        assert_eq!(warnings, 0);
        fs::remove_dir_all(root).expect("remove root");
        fs::remove_dir_all(outside).expect("remove outside");
    }

    #[test]
    fn refuses_to_store_the_index_inside_the_vault() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let result = open_gallery(&root, &root.join("index.sqlite"));
        assert!(matches!(result, Err(CoreError::Database)));
        assert!(!root.join("index.sqlite").exists());
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn malformed_note_is_warned_without_stopping_the_scan() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(root.join("broken.md"), "---\ntags: [broken\n---\n")
            .expect("write malformed note");
        fs::write(
            root.join("valid.md"),
            "---\ntags: [source/rating/safe]\n---\n# Valid\n",
        )
        .expect("write valid note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        let report = gallery.scan().expect("scan");
        assert_eq!(report.notes_indexed, 1);
        assert_eq!(report.warnings, 1);
        assert_eq!(gallery.warning_count().expect("warnings"), 1);
        drop(gallery);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn unchanged_scan_refreshes_missing_media() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/service/example]\ncover: media/image.webp\n---\n# One\n",
        )
        .expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("first scan");
        let missing = gallery
            .connection
            .query_row("SELECT exists_flag FROM media", [], |row| {
                row.get::<_, bool>(0)
            })
            .expect("media existence");
        assert!(!missing);
        fs::create_dir_all(root.join("media")).expect("create media");
        fs::write(root.join("media/image.webp"), b"fictional").expect("write media");
        gallery.scan().expect("unchanged scan");
        let exists = gallery
            .connection
            .query_row("SELECT exists_flag FROM media", [], |row| {
                row.get::<_, bool>(0)
            })
            .expect("media existence");
        assert!(exists);
        drop(gallery);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[cfg(unix)]
    #[test]
    fn media_symlinks_cannot_escape_the_vault() {
        let root = temp_dir();
        let outside = temp_dir().with_extension("media");
        fs::create_dir_all(&root).expect("create vault");
        fs::create_dir_all(&outside).expect("create outside");
        fs::write(outside.join("secret.webp"), b"fictional").expect("write outside media");
        fs::create_dir_all(root.join("media")).expect("create media directory");
        std::os::unix::fs::symlink(outside.join("secret.webp"), root.join("media/linked.webp"))
            .expect("create media symlink");
        assert!(!safe_media_exists(&root, "note.md", "media/linked.webp"));
        fs::remove_dir_all(root).expect("remove vault");
        fs::remove_dir_all(outside).expect("remove outside");
    }

    #[test]
    fn filters_or_within_categories_and_and_between_categories() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/rating/safe, source/gender/female]\n---\n# One\n",
        )
        .expect("write note one");
        fs::write(
            root.join("two.md"),
            "---\ntags: [source/rating/adult, source/gender/female]\n---\n# Two\n",
        )
        .expect("write note two");
        fs::write(
            root.join("three.md"),
            "---\ntags: [source/rating/adult, source/gender/male]\n---\n# Three\n",
        )
        .expect("write note three");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        assert_eq!(
            gallery
                .query(
                    &[
                        "source/rating/safe".into(),
                        "source/rating/adult".into(),
                        "source/gender/female".into()
                    ],
                    10
                )
                .expect("query")
                .len(),
            2
        );
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn configured_categories_split_deep_tags_and_show_other_only_when_present() {
        let base = temp_dir();
        let root = base.join("vault");
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/art, source/service/pixiv, source/count/pair, loose/topic/deep]\n---\n# Fictional\n",
        )
        .expect("write note");
        let database = base.join("index.sqlite");
        fs::write(
            base.join("tag-settings.json"),
            r#"{"noteStructure":{"galleryTagPrefixes":["source/"]},"tagCategories":{"categories":[{"name":"ソース","path":"source/art"},{"name":"ソース","path":"source/*","splitDeep":true},{"name":"人数","path":"source/count/*"}],"other":{"enabled":true,"name":"雑多","splitDeep":false}}}"#,
        )
        .expect("write settings");
        let mut gallery = Gallery::open(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let categories = gallery.categories().expect("categories");
        let titles = categories
            .iter()
            .map(|category| category.display_name.as_str())
            .collect::<Vec<_>>();
        assert_eq!(
            titles,
            ["コンテンツ", "ソース", "ソース / service", "人数", "雑多"]
        );
        let other = categories.last().expect("other category");
        assert_eq!(other.options[0].full_tag, "loose/topic/deep");
        drop(gallery);

        fs::write(
            base.join("tag-settings.json"),
            r#"{"noteStructure":{"galleryTagPrefixes":["source/"]},"tagCategories":{"categories":[{"name":"ソース","path":"source/*"}],"other":{"enabled":false,"name":"その他"}}}"#,
        )
        .expect("write settings");
        let gallery = Gallery::open(&root, &database).expect("reopen");
        let categories = gallery.categories().expect("categories");
        assert_eq!(categories.len(), 2);
        assert_eq!(categories[1].display_name, "ソース");
        assert!(
            categories[1]
                .options
                .iter()
                .all(|option| option.full_tag.starts_with("source"))
        );
        drop(gallery);
        fs::remove_dir_all(base).expect("remove fixtures");
    }

    #[test]
    fn source_categories_share_one_display_root_without_changing_filter_groups() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        for (name, tags) in [
            ("one.md", "source/service/pixiv, source/type/animal"),
            ("two.md", "source/service/mastodon, source/type/human"),
        ] {
            fs::write(
                root.join(name),
                format!("---\ntags: [{tags}]\n---\n# Fictional\n"),
            )
            .expect("write note");
        }
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let categories = gallery.categories().expect("categories");
        let source = categories
            .iter()
            .find(|category| category.display_name == "ソース")
            .expect("source grouping");
        assert_eq!(
            categories
                .iter()
                .filter(|category| category.display_name == "ソース")
                .count(),
            1
        );
        let names = source
            .options
            .iter()
            .map(|option| option.name.as_str())
            .collect::<Vec<_>>();
        assert_eq!(names, ["service/mastodon", "service/pixiv"]);
        assert!(
            categories
                .iter()
                .any(|category| category.path == "source/type" && category.display_name == "タイプ")
        );
        assert_eq!(
            gallery
                .query(
                    &[
                        "source/service/pixiv".to_owned(),
                        "source/type/animal".to_owned(),
                    ],
                    10,
                )
                .expect("filter across source subcategories")
                .len(),
            1
        );
        drop(gallery);
        fs::remove_file(database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn supports_excluded_tags_fuzzy_note_names_and_note_badges() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("portrait.md"),
            "---\nurl: https://example.invalid/post\ntags: [source/service/pixiv, source/type/animal, source/art/hidamari]\npublished: 2026-09-01\n---\n# Fictional Portrait\n[@fictional_author](https://example.invalid/author)\n> fictional post text\n![](portrait.webp)\n## 覚書\n- first\n  - second\n## 関連\n- [related note](https://example.invalid/note)\n- [other note](other.md)\n- [[other.md|wiki other note]]\n- [private note](private%20note.md)\n- ![[private note]]\n- ![](private%20note.md)\n",
        )
        .expect("write portrait");
        fs::write(
            root.join("other.md"),
            "---\ntags: [source/service/mastodon, source/type/human]\n---\n# Other Artwork\n",
        )
        .expect("write other");
        fs::write(
            root.join("private note.md"),
            "# Private Note\nThis note has no frontmatter or tags and is only a link target.\n\n![](linked.webp)\n",
        )
        .expect("write non-gallery link target");
        fs::write(root.join("linked.webp"), b"fictional image fixture")
            .expect("write linked note media fixture");
        fs::write(
            root.join("portrait.webp"),
            b"fictional portrait media fixture",
        )
        .expect("write portrait media fixture");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        let report = gallery.scan().expect("scan");
        assert_eq!(report.notes_indexed, 2);

        let fuzzy = gallery
            .query_filtered_page_search(&[], &[], &[], "fictinal", 0, 10)
            .expect("fuzzy name search");
        assert_eq!(fuzzy.len(), 1);
        assert_eq!(fuzzy[0].title, "Fictional Portrait");
        let tag_name_search = gallery
            .query_filtered_page_search(&[], &[], &[], "hidamari", 0, 10)
            .expect("plain-text tag-name search");
        assert_eq!(tag_name_search.len(), 1);
        assert_eq!(tag_name_search[0].title, "Fictional Portrait");
        let fuzzy_tag_name_search = gallery
            .query_filtered_page_search(&[], &[], &[], "hidamri", 0, 10)
            .expect("fuzzy plain-text tag-name search");
        assert_eq!(fuzzy_tag_name_search.len(), 1);
        assert_eq!(fuzzy_tag_name_search[0].title, "Fictional Portrait");
        let tag_search = gallery
            .query_filtered_page_search(&[], &[], &[], "#source/type/animal", 0, 10)
            .expect("tag search");
        assert_eq!(tag_search.len(), 1);
        assert_eq!(tag_search[0].title, "Fictional Portrait");
        let fuzzy_tag_search = gallery
            .query_filtered_page_search(&[], &[], &[], "#source/type/animl", 0, 10)
            .expect("fuzzy tag search");
        assert_eq!(fuzzy_tag_search.len(), 1);
        assert_eq!(fuzzy_tag_search[0].title, "Fictional Portrait");
        let fuzzy_excluded_tag_search = gallery
            .query_filtered_page_search(&[], &[], &[], "-#source/type/animl", 0, 10)
            .expect("fuzzy excluded tag search");
        assert_eq!(fuzzy_excluded_tag_search.len(), 1);
        assert_eq!(fuzzy_excluded_tag_search[0].title, "Other Artwork");
        let fuzzy_all_tag_search = gallery
            .query_filtered_page_search(&[], &[], &[], "&#source/type/animl", 0, 10)
            .expect("fuzzy all-tags search");
        assert_eq!(fuzzy_all_tag_search.len(), 1);
        assert_eq!(fuzzy_all_tag_search[0].title, "Fictional Portrait");
        let media_search = gallery
            .query_media_filtered_page_search(&[], &[], &[], "#source/type/animl", 0, 10)
            .expect("fuzzy tag media search");
        assert_eq!(media_search.len(), 1);
        assert_eq!(media_search[0].media_count, 1);
        assert_eq!(media_search[0].memo_count, 2);
        assert_eq!(media_search[0].related_count, 6);
        let excluded_tag_search = gallery
            .query_filtered_page_search(&[], &[], &[], "-#source/type/animal", 0, 10)
            .expect("excluded tag search");
        assert_eq!(excluded_tag_search.len(), 1);
        assert_eq!(excluded_tag_search[0].title, "Other Artwork");
        let all_tag_search = gallery
            .query_filtered_page_search(
                &[],
                &[],
                &[],
                "&#source/type/animal &#source/service/pixiv",
                0,
                10,
            )
            .expect("AND tag search");
        assert_eq!(all_tag_search.len(), 1);
        assert_eq!(all_tag_search[0].title, "Fictional Portrait");
        let combined_search = gallery
            .query_filtered_page_search(&[], &[], &[], "Fictional #source/type/animal", 0, 10)
            .expect("combined title and tag search");
        assert_eq!(combined_search.len(), 1);
        assert_eq!(combined_search[0].title, "Fictional Portrait");
        assert_eq!(fuzzy[0].memo_count, 2);
        assert_eq!(fuzzy[0].related_count, 6);
        let detail = gallery
            .note_detail(fuzzy[0].id)
            .expect("read note details")
            .expect("indexed note details");
        assert_eq!(detail.url.as_deref(), Some("https://example.invalid/post"));
        assert_eq!(detail.published.as_deref(), Some("2026-09-01"));
        assert_eq!(detail.body_text, "fictional post text");
        assert_eq!(detail.author.as_deref(), Some("@fictional_author"));
        assert_eq!(
            detail.author_url.as_deref(),
            Some("https://example.invalid/author")
        );
        assert_eq!(detail.memo_lines[0].text, "first");
        assert!(detail.memo_lines[0].is_bullet);
        assert_eq!(detail.memo_lines[0].indent_level, 0);
        assert_eq!(detail.memo_lines[1].indent_level, 1);
        assert_eq!(detail.related_lines[0].text, "related note");
        assert_eq!(
            detail.related_lines[0].urls,
            ["https://example.invalid/note"]
        );
        assert_eq!(detail.related_lines[0].linked_note_id, None);
        assert_eq!(
            detail.related_lines[1].linked_note_id,
            Some(
                gallery
                    .query_filtered_page_search(&[], &[], &[], "Other Artwork", 0, 10)
                    .expect("query linked note")[0]
                    .id
            )
        );
        assert_eq!(
            detail.related_lines[2].linked_note_id,
            detail.related_lines[1].linked_note_id
        );
        assert_eq!(detail.related_lines[2].text, "wiki other note");
        let private_note_id = detail.related_lines[3]
            .linked_note_id
            .expect("non-gallery note link resolves");
        assert_eq!(detail.related_lines[4].text, "private note");
        assert_eq!(
            detail.related_lines[4].linked_note_id,
            Some(private_note_id)
        );
        assert_eq!(detail.related_lines[5].text, "private note");
        assert_eq!(
            detail.related_lines[5].linked_note_id,
            Some(private_note_id)
        );
        let private_note = gallery
            .note_detail(private_note_id)
            .expect("load linked non-gallery note")
            .expect("private note detail");
        assert_eq!(private_note.title, "Private Note");
        assert_eq!(private_note.path, "private note.md");
        assert_eq!(private_note.media.len(), 1);
        assert!(private_note.media[0].exists);
        assert_eq!(detail.related_lines[3].urls, ["private%20note.md"]);
        assert!(
            gallery
                .query_filtered_page_search(&[], &[], &[], "", 0, 10)
                .expect("gallery excludes link-only note")
                .iter()
                .all(|note| note.path != "private note.md")
        );

        let excluded = gallery
            .query_filtered_page_search(&[], &["source/type/animal".to_owned()], &[], "", 0, 10)
            .expect("negative tag search");
        assert_eq!(excluded.len(), 1);
        assert_eq!(excluded[0].path, "other.md");
        let categories = gallery
            .categories_with_filters(&[], &["source/type/animal".to_owned()], &[])
            .expect("categories after exclusion");
        let animal = categories
            .iter()
            .flat_map(|category| &category.options)
            .find(|option| option.full_tag == "source/type/animal")
            .expect("excluded tag remains available");
        assert!(!animal.disabled);

        drop(gallery);
        fs::remove_file(database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn incremental_scan_updates_changed_notes_and_removes_deleted_notes() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let note_path = root.join("one.md");
        fs::write(&note_path, "---\ntags: [source/rating/safe]\n---\n# One\n").expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        assert_eq!(gallery.scan().expect("initial scan").notes_indexed, 1);
        assert_eq!(gallery.scan().expect("unchanged scan").notes_indexed, 1);
        std::thread::sleep(std::time::Duration::from_millis(5));
        fs::write(
            &note_path,
            "---\ntags: [source/rating/adult]\n---\n# Changed\n",
        )
        .expect("change note");
        gallery.scan().expect("changed scan");
        assert_eq!(
            gallery
                .query(&["source/rating/safe".into()], 10)
                .expect("old tag")
                .len(),
            0
        );
        assert_eq!(
            gallery
                .query(&["source/rating/adult".into()], 10)
                .expect("new tag")
                .len(),
            1
        );
        fs::remove_file(note_path).expect("delete note");
        gallery.scan().expect("deleted scan");
        assert!(gallery.query(&[], 10).expect("all notes").is_empty());
        drop(gallery);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn switching_vaults_clears_stale_index_entries() {
        let first_vault = temp_dir();
        let second_vault = temp_dir().with_extension("second");
        fs::create_dir_all(&first_vault).expect("create first vault");
        fs::create_dir_all(&second_vault).expect("create second vault");
        let first_note = first_vault.join("same.md");
        let second_note = second_vault.join("same.md");
        fs::write(&first_note, "---\ntags: [source/example]\n---\n# First\n")
            .expect("write first note");
        fs::write(&second_note, "---\ntags: [source/example]\n---\n# Other\n")
            .expect("write second note");
        let modified = UNIX_EPOCH + std::time::Duration::from_secs(1_700_000_000);
        for path in [&first_note, &second_note] {
            fs::File::options()
                .write(true)
                .open(path)
                .expect("open note")
                .set_times(fs::FileTimes::new().set_modified(modified))
                .expect("set note timestamp");
        }
        let database = first_vault.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            first_vault.file_name().expect("name").to_string_lossy()
        ));

        let mut first = open_gallery(&first_vault, &database).expect("open first vault");
        first.scan().expect("scan first vault");
        drop(first);

        let mut second = open_gallery(&second_vault, &database).expect("open second vault");
        assert!(
            second
                .query(&[], 10)
                .expect("empty switched index")
                .is_empty()
        );
        assert_eq!(second.scan().expect("scan second vault").notes_indexed, 1);
        assert_eq!(
            second.query(&[], 10).expect("second vault notes")[0].title,
            "Other"
        );

        drop(second);
        fs::remove_file(database).expect("remove index");
        fs::remove_dir_all(first_vault).expect("remove first vault");
        fs::remove_dir_all(second_vault).expect("remove second vault");
    }

    #[test]
    fn parse_failures_are_retried_on_later_scans() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let path = root.join("retry.md");
        fs::write(&path, "---\ntags: [invalid\n---\n").expect("write invalid note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        assert_eq!(gallery.scan().expect("bad scan").warnings, 1);
        fs::write(&path, "---\ntags: [source/rating/safe]\n---\n# Valid\n").expect("repair note");
        assert_eq!(gallery.scan().expect("retry scan").notes_indexed, 1);
        assert_eq!(gallery.warning_count().expect("warnings"), 0);
        drop(gallery);
        fs::remove_file(database).expect("remove db");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[cfg(unix)]
    #[test]
    fn rejects_hard_linked_index_files() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let vault_file = root.join("note.md");
        fs::write(&vault_file, "not an index").expect("write note");
        let external_index = root.parent().expect("parent").join(format!(
            "gallery-link-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        fs::hard_link(&vault_file, &external_index).expect("make hard link");
        let result = open_gallery(&root, &external_index);
        assert!(matches!(result, Err(CoreError::Database)));
        fs::remove_file(external_index).expect("remove external link");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn generates_bounded_private_thumbnail_cache_outside_the_vault() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let mut encoded = Cursor::new(Vec::new());
        image::DynamicImage::new_rgb8(24, 16)
            .write_to(&mut encoded, ImageFormat::Png)
            .expect("encode fixture");
        fs::write(root.join("tiny.png"), encoded.into_inner()).expect("write media");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/test]\ncover: tiny.png\n---\n# One\n",
        )
        .expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let cache = root.parent().expect("parent").join(format!(
            "gallery-cache-{}",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let note = gallery.query(&[], 1).expect("query").remove(0);
        let media_id = note.representative_media_id.expect("media id");
        let thumbnail = gallery
            .get_thumbnail(media_id, 320, &cache)
            .expect("thumbnail")
            .expect("decoded image");
        assert_eq!(&thumbnail[..8], b"\x89PNG\r\n\x1a\n");
        assert_eq!(
            gallery
                .get_thumbnail(media_id, 320, &cache)
                .expect("cached thumbnail"),
            Some(thumbnail)
        );
        assert!(matches!(
            gallery.get_thumbnail(media_id, 0, &cache),
            Err(CoreError::Io)
        ));
        assert!(matches!(
            gallery.get_thumbnail(media_id, 320, &root.join("cache")),
            Err(CoreError::Io)
        ));
        assert!(!root.join("cache").exists());
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            assert_eq!(
                fs::metadata(&cache).expect("cache directory").mode() & 0o777,
                0o700
            );
            let shard = fs::read_dir(&cache)
                .expect("cache shard")
                .next()
                .expect("shard")
                .expect("entry")
                .path();
            assert_eq!(
                fs::metadata(shard).expect("shard metadata").mode() & 0o777,
                0o700
            );
        }
        fs::remove_dir_all(&cache).expect("remove thumbnail cache");
        fs::remove_file(&database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn vault_selection_storage_is_private_and_never_created_in_the_vault() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let state = root.parent().expect("parent").join(format!(
            "gallery-state-{}",
            root.file_name().expect("name").to_string_lossy()
        ));
        let selected = save_selected_vault(&state, &root).expect("save selection");
        assert_eq!(
            load_selected_vault(&state).expect("load selection"),
            Some(selected)
        );
        let inside_vault = root.join("vault-gallery");
        assert!(matches!(
            prepare_private_app_directory(&inside_vault, &root),
            Err(CoreError::Io)
        ));
        assert!(!inside_vault.exists());
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            assert_eq!(
                fs::metadata(&state).expect("state metadata").mode() & 0o777,
                0o700
            );
            assert_eq!(
                fs::metadata(state.join("vault-path"))
                    .expect("vault path metadata")
                    .mode()
                    & 0o777,
                0o600
            );
        }
        fs::remove_dir_all(&state).expect("remove state");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn category_counts_respect_active_or_and_filters() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        for (name, gender, rating) in [
            ("one.md", "female", "safe"),
            ("two.md", "male", "safe"),
            ("three.md", "female", "adult"),
        ] {
            fs::write(
                root.join(name),
                format!(
                    "---\ntags: [source/gender/{gender}, source/rating/{rating}, standalone]\n---\n# One\n"
                ),
            )
            .expect("write note");
        }
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let categories = gallery
            .categories_for(&["source/gender/female".to_owned()])
            .expect("filtered categories");
        let gender = categories
            .iter()
            .find(|category| category.path == "source/gender")
            .expect("gender category");
        assert_eq!(gender.count, 3);
        assert_eq!(gender.options[0].name, "すべて");
        assert_eq!(gender.options[0].full_tag, "source/gender");
        assert_eq!(gender.options[0].count, 3);
        assert_eq!(
            gender
                .options
                .iter()
                .find(|option| option.name == "male")
                .expect("male option")
                .count,
            3
        );
        assert_eq!(
            gallery
                .query(&["source/gender".into()], 10)
                .expect("category-wide filter")
                .len(),
            3
        );
        assert_eq!(
            gallery
                .query(&["source/gender".into(), "source/gender/female".into()], 10,)
                .expect("category-wide OR specific filter")
                .len(),
            3
        );
        let rating = categories
            .iter()
            .find(|category| category.path == "source/rating")
            .expect("rating category");
        assert_eq!(rating.count, 2);
        let single_level = gallery
            .categories_for(&["standalone".to_owned()])
            .expect("single-level tag categories");
        assert_eq!(
            single_level
                .iter()
                .find(|category| category.path == "@other")
                .expect("other category")
                .count,
            3
        );
        fs::remove_file(&database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn virtual_content_filters_are_first_class_options() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("many.md"),
            "---\ntags: [source/rating/safe]\ncover: first.png\n---\n# Many\n![](second.png)\n# 文書\n## 覚書\n- keep this\n",
        )
        .expect("write multi-media note");
        let parsed =
            parse_note(&fs::read_to_string(root.join("many.md")).expect("read multi-media note"))
                .expect("parse multi-media note");
        assert_eq!(parsed.memo_lines.len(), 1);
        fs::write(
            root.join("single.md"),
            "---\ntags: [source/rating/safe]\ncover: first.png\n---\n# Single\n",
        )
        .expect("write single-media note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let categories = gallery.categories().expect("categories");
        assert_eq!(categories[0].path, "@content");
        assert_eq!(categories[0].display_name, "コンテンツ");
        let multiple = categories[0]
            .options
            .iter()
            .find(|option| option.virtual_filter == Some(VirtualFilter::MultipleMedia))
            .expect("multiple-media filter");
        assert_eq!(multiple.count, 1);
        let memo = categories[0]
            .options
            .iter()
            .find(|option| option.virtual_filter == Some(VirtualFilter::HasMemo))
            .expect("memo filter");
        assert_eq!(memo.count, 1);
        assert_eq!(
            gallery
                .query_filtered_page(&[], &[VirtualFilter::MultipleMedia], 0, 10)
                .expect("filtered query")
                .len(),
            1
        );
        fs::remove_file(&database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn query_media_flattens_every_media_item_across_notes() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("many.md"),
            "---\ntags: [source/rating/safe]\npublished: 2026-01-02T00:00:00\ncreated: 2026-01-01T00:00:00\ncover: first.png\n---\n# Many\n![](second.mp4)\n",
        )
        .expect("write multi-media note");
        fs::write(
            root.join("single.md"),
            "---\ntags: [source/rating/safe]\npublished: 2026-01-01T00:00:00\ncreated: 2026-01-02T00:00:00\ncover: only.png\n---\n# Single\n",
        )
        .expect("write single-media note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");

        // Grouped by note: two notes, one each.
        assert_eq!(gallery.query(&[], 10).expect("notes").len(), 2);
        let default_order = gallery
            .query(&[], 10)
            .expect("default note order")
            .into_iter()
            .map(|note| note.path)
            .collect::<Vec<_>>();
        assert_eq!(default_order, ["single.md", "many.md"]);
        let created_ascending = gallery
            .query_filtered_page_search_with_all_and_sort(
                &[],
                &[],
                &[],
                &[],
                "",
                NoteSort {
                    field: NoteSortField::Created,
                    direction: SortDirection::Ascending,
                },
                0,
                10,
            )
            .expect("created date ascending")
            .into_iter()
            .map(|note| note.path)
            .collect::<Vec<_>>();
        assert_eq!(created_ascending, ["many.md", "single.md"]);
        let created_descending = gallery
            .query_filtered_page_search_with_all_and_sort(
                &[],
                &[],
                &[],
                &[],
                "",
                NoteSort {
                    field: NoteSortField::Created,
                    direction: SortDirection::Descending,
                },
                0,
                10,
            )
            .expect("created date descending")
            .into_iter()
            .map(|note| note.path)
            .collect::<Vec<_>>();
        assert_eq!(created_descending, ["single.md", "many.md"]);
        let published_ascending_first_page = gallery
            .query_filtered_page_search_with_all_and_sort(
                &[],
                &[],
                &[],
                &[],
                "",
                NoteSort {
                    field: NoteSortField::Published,
                    direction: SortDirection::Ascending,
                },
                0,
                1,
            )
            .expect("published date ascending first page");
        assert_eq!(published_ascending_first_page[0].path, "single.md");
        let published_ascending_second_page = gallery
            .query_filtered_page_search_with_all_and_sort(
                &[],
                &[],
                &[],
                &[],
                "",
                NoteSort {
                    field: NoteSortField::Published,
                    direction: SortDirection::Ascending,
                },
                1,
                1,
            )
            .expect("published date ascending second page");
        assert_eq!(published_ascending_second_page[0].path, "many.md");
        assert_eq!(
            gallery
                .count_filtered_notes_with_all(&[], &[], &[], &[], "")
                .expect("count all notes"),
            2
        );
        assert_eq!(
            gallery
                .count_filtered_notes_with_all(
                    &["source/rating/safe".into()],
                    &[],
                    &[],
                    &[],
                    "Many",
                )
                .expect("count filtered notes"),
            1
        );

        // Flattened: three media items total (two from "many", one from "single"),
        // newest-created note first, in appearance order within a note.
        let media = gallery
            .query_media_filtered_page(&[], &[], 0, 10)
            .expect("media");
        assert_eq!(media.len(), 3);
        assert_eq!(
            gallery
                .count_filtered_media_with_all(&[], &[], &[], &[], "")
                .expect("count all media"),
            3
        );
        assert_eq!(
            gallery
                .count_filtered_media_with_all(
                    &["source/rating/safe".into()],
                    &[],
                    &[],
                    &[],
                    "Many",
                )
                .expect("count filtered media"),
            2
        );
        assert!(!media[0].is_video);
        assert!(!media[1].is_video);
        assert!(media[2].is_video);
        assert_ne!(media[0].note_id, media[1].note_id);
        assert_eq!(media[1].note_id, media[2].note_id);
        let created_media = gallery
            .query_media_filtered_page_search_with_all_and_sort(
                &[],
                &[],
                &[],
                &[],
                "",
                NoteSort {
                    field: NoteSortField::Created,
                    direction: SortDirection::Ascending,
                },
                0,
                10,
            )
            .expect("created date media order");
        assert_eq!(created_media.len(), 3);
        assert_eq!(created_media[0].note_id, created_media[1].note_id);
        assert_ne!(created_media[1].note_id, created_media[2].note_id);

        // Pagination across the flattened list works like the note-level one.
        let first_page = gallery
            .query_media_filtered_page(&[], &[], 0, 2)
            .expect("first page");
        let second_page = gallery
            .query_media_filtered_page(&[], &[], 2, 2)
            .expect("second page");
        assert_eq!(first_page.len(), 2);
        assert_eq!(second_page.len(), 1);
        assert_eq!(second_page[0].id, media[2].id);

        fs::remove_file(&database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn date_sort_keeps_missing_values_last_and_breaks_ties_by_path() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        for (path, date) in [
            ("b.md", Some("2026-01-01")),
            ("a.md", Some("2026-01-01")),
            ("missing.md", None),
        ] {
            let frontmatter = date.map_or_else(String::new, |date| format!("created: {date}\n"));
            fs::write(
                root.join(path),
                format!("---\ntags: [source/rating/safe]\n{frontmatter}---\n# Note\n"),
            )
            .expect("write note");
        }
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        for direction in [SortDirection::Ascending, SortDirection::Descending] {
            let paths = gallery
                .query_filtered_page_search_with_all_and_sort(
                    &[],
                    &[],
                    &[],
                    &[],
                    "",
                    NoteSort {
                        field: NoteSortField::Created,
                        direction,
                    },
                    0,
                    10,
                )
                .expect("sort notes")
                .into_iter()
                .map(|note| note.path)
                .collect::<Vec<_>>();
            assert_eq!(paths, ["a.md", "b.md", "missing.md"]);
        }
        fs::remove_file(&database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn video_source_path_is_only_returned_for_existing_vault_video_media() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(root.join("clip.mp4"), b"fictional video bytes").expect("write video");
        fs::write(root.join("still.png"), b"fictional image bytes").expect("write image");
        fs::write(
            root.join("note.md"),
            "---\ntags: [source/type/video]\ncover: clip.mp4\n---\n# Fictional video\n![](still.png)\n",
        )
        .expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = open_gallery(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let media = gallery
            .query_media_filtered_page(&[], &[], 0, 10)
            .expect("query media");
        assert_eq!(media.len(), 2);
        assert!(media[0].is_video);
        assert!(!media[1].is_video);

        assert_eq!(
            gallery
                .video_source_path(media[0].id)
                .expect("video path")
                .as_deref(),
            Some(
                fs::canonicalize(root.join("clip.mp4"))
                    .expect("canonical")
                    .as_path()
            )
        );
        assert_eq!(
            gallery
                .media_source_path(media[0].id)
                .expect("media path")
                .as_deref(),
            Some(
                fs::canonicalize(root.join("clip.mp4"))
                    .expect("canonical media")
                    .as_path()
            )
        );
        assert_eq!(
            gallery
                .media_source_path(media[1].id)
                .expect("image source path")
                .as_deref(),
            Some(
                fs::canonicalize(root.join("still.png"))
                    .expect("canonical image")
                    .as_path()
            )
        );
        assert!(
            gallery
                .video_source_path(media[1].id)
                .expect("image is not a video")
                .is_none()
        );
        assert!(
            gallery
                .media_source_path(-1)
                .expect("unknown media path")
                .is_none()
        );
        assert!(
            gallery
                .video_source_path(-1)
                .expect("unknown media path")
                .is_none()
        );
        assert!(
            gallery
                .get_thumbnail(media[0].id, 320, &root.parent().unwrap().join("cache"))
                .expect("video has no image thumbnail")
                .is_none()
        );

        drop(gallery);
        fs::remove_file(database).expect("remove index");
        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn diagnose_notes_explains_why_each_note_is_excluded() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("ok.md"),
            "---\ntags: [source/rating/safe]\n---\n# Ok\n",
        )
        .expect("write kept note");
        fs::write(
            root.join("no-source-tag.md"),
            "---\ntags: [moc]\n---\n# No source tag\n",
        )
        .expect("write note without a source tag");
        fs::write(
            root.join("broken.md"),
            "---\ntags: [broken\n---\n# Broken\n",
        )
        .expect("write note with broken frontmatter");
        fs::write(root.join("no-tags.md"), "---\nurl: x\n---\n# No tags\n")
            .expect("write note without tags");

        let diagnostics = diagnose_notes(&root).expect("diagnose");
        assert_eq!(diagnostics.len(), 3);
        let by_path: BTreeMap<_, _> = diagnostics
            .iter()
            .map(|diagnostic| (diagnostic.path.as_str(), diagnostic.reason.as_str()))
            .collect();
        assert!(by_path.contains_key("no-source-tag.md"));
        assert!(by_path.contains_key("broken.md"));
        assert!(by_path.contains_key("no-tags.md"));
        assert!(!by_path.contains_key("ok.md"));

        fs::remove_dir_all(root).expect("remove vault");
    }

    #[test]
    fn saf_limits_match_note_parser_and_accept_only_bounded_paths() {
        assert_eq!(MAX_SAF_NOTE_BYTES, gallery_parse::MAX_NOTE_BYTES);
        assert!(saf_path_within_limits(
            &"a".repeat(MAX_SAF_RELATIVE_PATH_BYTES)
        ));
        assert!(!saf_path_within_limits(
            &"a".repeat(MAX_SAF_RELATIVE_PATH_BYTES + 1)
        ));
        let max_depth_path = format!(
            "{}/note.md",
            (0..MAX_SAF_DEPTH)
                .map(|_| "folder")
                .collect::<Vec<_>>()
                .join("/")
        );
        let over_depth_path = format!("folder/{max_depth_path}");
        assert!(saf_path_within_limits(&max_depth_path));
        assert!(!saf_path_within_limits(&over_depth_path));
        assert!(!saf_path_within_limits("folder/../note.md"));
    }

    #[test]
    fn forgetting_a_vault_removes_only_private_selection_and_index_data() {
        let vault = temp_dir();
        fs::create_dir_all(&vault).expect("create vault");
        let note = vault.join("note.md");
        fs::write(&note, "fictional note").expect("write fictional note");
        let app_data = temp_dir();
        fs::create_dir_all(&app_data).expect("create app data");
        let selected = save_selected_vault(&app_data, &vault).expect("save selection");

        for name in [
            "index.sqlite",
            "index.sqlite-journal",
            "index.sqlite-wal",
            "index.sqlite-shm",
        ] {
            fs::write(app_data.join(name), "private index data").expect("create index file");
        }
        fs::create_dir_all(app_data.join("thumbnails/00")).expect("create thumbnail cache");
        fs::write(
            app_data.join("thumbnails/00/fictional-thumbnail.png"),
            "fictional thumbnail",
        )
        .expect("write thumbnail cache");
        fs::write(app_data.join("saf-scan-cache.json"), "{}").expect("write scan summary");
        fs::write(app_data.join("global-settings.json"), "{}").expect("write global settings");

        assert!(forget_selected_vault(&app_data, "/tmp/another-vault").is_err());
        assert_eq!(
            load_selected_vault(&app_data).expect("selection remains"),
            Some(selected.clone())
        );
        assert!(app_data.join("index.sqlite").exists());

        forget_selected_vault(&app_data, &selected).expect("forget selected vault");
        assert_eq!(
            load_selected_vault(&app_data).expect("selection cleared"),
            None
        );
        for name in [
            "index.sqlite",
            "index.sqlite-journal",
            "index.sqlite-wal",
            "index.sqlite-shm",
            "vault-path",
            "saf-scan-cache.json",
        ] {
            assert!(!app_data.join(name).exists(), "{name} should be removed");
        }
        assert!(!app_data.join("thumbnails").exists());
        assert!(app_data.join("global-settings.json").exists());
        assert_eq!(
            fs::read_to_string(note).expect("vault note remains"),
            "fictional note"
        );
        forget_selected_vault(&app_data, &selected).expect("forget is retryable");

        fs::remove_dir_all(app_data).expect("remove app data");
        fs::remove_dir_all(vault).expect("remove fictional vault");
    }

    #[test]
    fn saf_scans_in_memory_documents_without_copying_vault_data() {
        let app_data = temp_dir();
        fs::create_dir_all(&app_data).expect("create private data directory");
        let database = app_data.join("index.sqlite");
        let vault_uri =
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FNotes";
        assert_eq!(
            save_selected_vault_saf(&app_data, vault_uri).expect("save selected folder"),
            vault_uri
        );
        assert_eq!(
            load_selected_vault(&app_data).expect("load selected folder"),
            Some(vault_uri.to_owned())
        );
        let note = b"---\ntags: [source/rating/safe]\ncover: media/cover.png\n---\n# Sample\n![](media/cover.png)\n![](../../outside.png)\n## \xe9\x96\xa2\xe9\x80\xa3\n- [[Other]]\n";
        fs::write(
            app_data.join("tag-settings.json"),
            r#"{"noteStructure":{"galleryTagPrefixes":["source/"]}}"#,
        )
        .expect("write test settings");
        let mut gallery = Gallery::open_saf(vault_uri, &database).expect("open SAF index");
        let report = gallery
            .scan_saf(
                vec![SafNoteDocument {
                    path: "sample.md".to_owned(),
                    modified_nanos: 1_000,
                    size: note.len() as i64,
                    content: Some(note.to_vec()),
                }],
                vec![
                    "sample.md".to_owned(),
                    "media/cover.png".to_owned(),
                    ".obsidian/config".to_owned(),
                ],
            )
            .expect("scan SAF documents");

        assert_eq!(report.notes_indexed, 1);
        assert_eq!(report.warnings, 0);
        let notes = gallery.query(&[], 10).expect("query SAF notes");
        assert_eq!(notes.len(), 1);
        assert_eq!(notes[0].path, "sample.md");
        let media = gallery
            .query_media_filtered_page(&[], &[], 0, 10)
            .expect("query SAF media");
        assert_eq!(media.len(), 2);
        assert!(media[0].exists);
        assert!(!media[1].exists);
        assert_eq!(
            gallery
                .media_relative_path(media[0].id)
                .expect("resolve SAF media")
                .as_deref(),
            Some("media/cover.png")
        );
        let details = gallery
            .note_detail_saf(notes[0].id, note)
            .expect("parse SAF note details")
            .expect("SAF note exists");
        assert_eq!(details.title, "Sample");
        assert_eq!(details.media.len(), 2);
        assert!(!app_data.join("sample.md").exists());

        drop(gallery);
        fs::remove_dir_all(app_data).expect("remove private data directory");
    }

    #[test]
    fn saf_rejects_invalid_tree_uris_and_unbounded_scans() {
        let app_data = temp_dir();
        fs::create_dir_all(&app_data).expect("create private data directory");
        let database = app_data.join("index.sqlite");
        assert!(Gallery::open_saf("/storage/emulated/0/Documents", &database).is_err());

        let mut gallery = Gallery::open_saf(
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
            &database,
        )
        .expect("open SAF index");
        assert!(
            gallery
                .scan_saf(Vec::new(), vec!["x".to_owned(); MAX_SAF_DOCUMENTS + 1],)
                .is_err()
        );
        drop(gallery);
        fs::remove_dir_all(app_data).expect("remove private data directory");
    }
}
