#![forbid(unsafe_code)]

use gallery_parse::{MediaKind, ParsedNote, expanded_tags, parse_note};
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

const DEFAULT_EXCLUDES: [&str; 4] = ["moc", "add", "pin", "source/art"];
const INDEX_SCHEMA_VERSION: &str = "1";
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
    pub representative_media_id: Option<i64>,
}

struct ScanItem {
    path: PathBuf,
    relative: String,
    mtime: i64,
    size: i64,
}

pub struct Gallery {
    root: PathBuf,
    connection: Connection,
    excludes: Vec<String>,
    labels: BTreeMap<String, String>,
}

pub fn prepare_private_app_directory(directory: &Path, vault: &Path) -> Result<(), CoreError> {
    let vault_root = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
    let canonical = ensure_private_child_directory(directory, &vault_root)?;
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

impl Gallery {
    pub fn open(vault: &Path, database: &Path) -> Result<Self, CoreError> {
        let root = fs::canonicalize(vault).map_err(|_| CoreError::VaultUnavailable)?;
        if !root.is_dir() {
            return Err(CoreError::InvalidVault);
        }
        let database_path = resolve_database_path(database, &root)?;
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
                   mtime INTEGER NOT NULL,
                   size INTEGER NOT NULL
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
                 CREATE INDEX IF NOT EXISTS idx_note_tags_tag ON note_tags(tag_id);",
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
        let vault_identity = root.to_string_lossy();
        if indexed_vault.as_deref() != Some(vault_identity.as_ref()) {
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
                [vault_identity.as_ref()],
            )
            .map_err(|_| CoreError::Database)?;
        transaction.commit().map_err(|_| CoreError::Database)?;

        Ok(Self {
            root,
            connection,
            excludes: DEFAULT_EXCLUDES
                .iter()
                .map(|tag| (*tag).to_owned())
                .collect(),
            labels: default_labels(),
        })
    }

    pub fn scan(&mut self) -> Result<ScanReport, CoreError> {
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
                    "SELECT s.path, s.mtime, s.size, n.id IS NOT NULL
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
        if unchanged_indexed != 0 {
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
                let handle = thread::Builder::new()
                    .name(format!("gallery-scan-{worker}"))
                    .spawn_scoped(scope, move || {
                        loop {
                            let index = next.fetch_add(1, Ordering::Relaxed);
                            let Some(item) = pending.get(index) else {
                                break;
                            };
                            if sender.send((index, read_parse_note(&item.path))).is_err() {
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
                        if parsed.tags.iter().any(|tag| tag.starts_with("source/")) {
                            insert_note(
                                &transaction,
                                &item.relative,
                                &parsed,
                                &self.root,
                                item.mtime,
                                item.size,
                            )?;
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

    pub fn categories(&self) -> Result<Vec<Category>, CoreError> {
        self.categories_for(&[])
    }

    pub fn categories_for(&self, filters: &[String]) -> Result<Vec<Category>, CoreError> {
        self.categories_with_filters(filters, &[])
    }

    pub fn categories_with_filters(
        &self,
        filters: &[String],
        virtual_filters: &[VirtualFilter],
    ) -> Result<Vec<Category>, CoreError> {
        let mut category_tags: BTreeMap<String, BTreeSet<String>> = BTreeMap::new();
        let mut statement = self
            .connection
            .prepare("SELECT name FROM tags ORDER BY name")
            .map_err(|_| CoreError::Database)?;
        let tags = statement
            .query_map([], |row| row.get::<_, String>(0))
            .map_err(|_| CoreError::Database)?;
        for tag in tags {
            let tag = tag.map_err(|_| CoreError::Database)?;
            if is_excluded(&tag, &self.excludes) {
                continue;
            }
            let has_descendant = statement_has_descendant(&self.connection, &tag)?;
            if has_descendant {
                continue;
            }
            let category = tag
                .rsplit_once('/')
                .map(|(category, _)| category.to_owned())
                .unwrap_or_else(|| "その他".to_owned());
            category_tags.entry(category).or_default().insert(tag);
        }
        let mut categories = Vec::new();
        for (path, options) in category_tags {
            let display_name = self
                .labels
                .get(&path)
                .cloned()
                .unwrap_or_else(|| path.clone());
            let mut category_filters = Vec::new();
            let mut base_filters = Vec::new();
            for filter in filters {
                if filter_category(&self.connection, filter)? == path {
                    category_filters.push(filter.clone());
                } else {
                    base_filters.push(filter.clone());
                }
            }
            let base_matches = self.matching_note_ids(&base_filters, virtual_filters)?;
            let mut selected_ids = BTreeSet::new();
            for filter in &category_filters {
                selected_ids.extend(note_ids_for_tag(&self.connection, filter)?);
            }
            let mut category_ids = BTreeSet::new();
            let mut category_options = Vec::new();
            for full_tag in &options {
                let name = full_tag
                    .rsplit('/')
                    .next()
                    .unwrap_or(full_tag.as_str())
                    .to_owned();
                let option_ids = note_ids_for_tag(&self.connection, full_tag)?;
                category_ids.extend(option_ids.iter().copied());
                let count = selected_ids
                    .union(&option_ids)
                    .filter(|id| base_matches.contains(id))
                    .count();
                category_options.push(CategoryOption {
                    count,
                    disabled: count == 0,
                    name,
                    full_tag: full_tag.clone(),
                    virtual_filter: None,
                });
            }
            let count = base_matches.intersection(&category_ids).count();
            if path != "その他" {
                category_options.insert(
                    0,
                    CategoryOption {
                        name: path.rsplit('/').next().unwrap_or(path.as_str()).to_owned(),
                        full_tag: path.clone(),
                        count,
                        disabled: count == 0,
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
        let matched_count = self.matching_note_ids(filters, virtual_filters)?.len();
        let mut content_options = Vec::with_capacity(VIRTUAL_FILTERS.len());
        for (filter, name) in VIRTUAL_FILTERS {
            let mut option_filters = virtual_filters.to_vec();
            option_filters.push(filter);
            let count = self.matching_note_ids(filters, &option_filters)?.len();
            content_options.push(CategoryOption {
                name: name.to_owned(),
                full_tag: String::new(),
                count,
                disabled: count == 0,
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
        let ids = self.matching_note_ids(filters, virtual_filters)?;
        let mut result = Vec::new();
        for id in ids {
            let (published, summary) = self
                .connection
                .query_row(
                    "SELECT published, path, title, media_count, video_count FROM notes WHERE id=?1",
                    [id],
                    |row| {
                        Ok((
                            row.get::<_, Option<String>>(0)?,
                            NoteSummary {
                                id,
                                path: row.get(1)?,
                                title: row.get(2)?,
                                media_count: row.get::<_, i64>(3)? as usize,
                                video_count: row.get::<_, i64>(4)? as usize,
                                representative_media_id: self
                                    .connection
                                    .query_row(
                                        "SELECT id FROM media WHERE note_id=?1 ORDER BY ord LIMIT 1",
                                        [id],
                                        |media_row| media_row.get(0),
                                    )
                                    .optional()?,
                            },
                        ))
                    },
                )
                .map_err(|_| CoreError::Database)?;
            result.push((published, summary));
        }
        result.sort_by(|(left, _), (right, _)| match (left, right) {
            (Some(left), Some(right)) => right.cmp(left),
            (Some(_), None) => std::cmp::Ordering::Less,
            (None, Some(_)) => std::cmp::Ordering::Greater,
            (None, None) => std::cmp::Ordering::Equal,
        });
        Ok(result
            .into_iter()
            .skip(offset)
            .take(limit)
            .map(|(_, summary)| summary)
            .collect())
    }

    fn matching_note_ids(
        &self,
        filters: &[String],
        virtual_filters: &[VirtualFilter],
    ) -> Result<BTreeSet<i64>, CoreError> {
        let mut grouped_filters: BTreeMap<String, BTreeSet<String>> = BTreeMap::new();
        for filter in filters {
            let category = filter_category(&self.connection, filter)?;
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
        for virtual_filter in virtual_filters {
            let virtual_ids = note_ids_for_virtual_filter(&self.connection, *virtual_filter)?;
            candidates = Some(match candidates {
                None => virtual_ids,
                Some(current) => current.intersection(&virtual_ids).copied().collect(),
            });
        }
        Ok(match candidates {
            Some(ids) => ids,
            None => all_note_ids(&self.connection)?,
        })
    }

    pub fn warning_count(&self) -> Result<usize, CoreError> {
        self.connection
            .query_row("SELECT COUNT(*) FROM warnings", [], |row| {
                row.get::<_, i64>(0).map(|n| n as usize)
            })
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
        let Some((source, media_identity)) = self.media_path(media_id)? else {
            return Ok(None);
        };
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

    fn media_path(&self, media_id: i64) -> Result<Option<(PathBuf, String)>, CoreError> {
        let media = self
            .connection
            .query_row(
                "SELECT notes.path, media.rel_path, media.exists_flag
                 FROM media JOIN notes ON notes.id=media.note_id WHERE media.id=?1",
                [media_id],
                |row| {
                    Ok((
                        row.get::<_, String>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, bool>(2)?,
                    ))
                },
            )
            .optional()
            .map_err(|_| CoreError::Database)?;
        let Some((note_path, media_path, exists)) = media else {
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
                )
            }),
        )
    }
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

fn all_note_ids(connection: &Connection) -> Result<BTreeSet<i64>, CoreError> {
    let mut statement = connection
        .prepare("SELECT id FROM notes")
        .map_err(|_| CoreError::Database)?;
    statement
        .query_map([], |row| row.get::<_, i64>(0))
        .map_err(|_| CoreError::Database)?
        .collect::<Result<BTreeSet<_>, _>>()
        .map_err(|_| CoreError::Database)
}

fn default_labels() -> BTreeMap<String, String> {
    [
        ("source/service", "ソース"),
        ("source/rating", "レーティング"),
        ("source/gender", "性別"),
        ("source/count", "人数"),
        ("copyright", "著作権"),
        ("source/meta", "メタ"),
        ("source/format", "アートスタイル"),
        ("source/type", "タイプ"),
    ]
    .into_iter()
    .map(|(key, value)| (key.to_owned(), value.to_owned()))
    .collect()
}

fn is_excluded(tag: &str, excludes: &[String]) -> bool {
    excludes
        .iter()
        .any(|prefix| tag == prefix || tag.starts_with(&format!("{prefix}/")))
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

fn filter_category(connection: &Connection, tag: &str) -> Result<String, CoreError> {
    if !tag.contains('/') {
        return Ok("その他".to_owned());
    }
    if statement_has_descendant(connection, tag)? {
        return Ok(tag.to_owned());
    }
    Ok(tag
        .rsplit_once('/')
        .map(|(category, _)| category.to_owned())
        .unwrap_or_else(|| "その他".to_owned()))
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
    let query = format!("SELECT id FROM notes WHERE {condition}");
    let mut statement = connection
        .prepare(&query)
        .map_err(|_| CoreError::Database)?;
    statement
        .query_map([], |row| row.get::<_, i64>(0))
        .map_err(|_| CoreError::Database)?
        .collect::<Result<BTreeSet<_>, _>>()
        .map_err(|_| CoreError::Database)
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

fn read_parse_note(path: &Path) -> Result<ParsedNote, &'static str> {
    let metadata = fs::metadata(path).map_err(|_| "read")?;
    if metadata.len() > gallery_parse::MAX_NOTE_BYTES as u64 {
        return Err("oversized");
    }
    let file = File::open(path).map_err(|_| "read")?;
    let mut content = String::with_capacity(metadata.len() as usize);
    file.take((gallery_parse::MAX_NOTE_BYTES + 1) as u64)
        .read_to_string(&mut content)
        .map_err(|_| "read")?;
    parse_note(&content).map_err(|error| match error {
        gallery_parse::ParseError::NoteTooLarge
        | gallery_parse::ParseError::FrontmatterTooLarge
        | gallery_parse::ParseError::TooManyTags => "limit",
        _ => "parse",
    })
}

fn insert_note(
    transaction: &Transaction<'_>,
    relative: &str,
    note: &ParsedNote,
    root: &Path,
    mtime: i64,
    size: i64,
) -> Result<bool, CoreError> {
    let videos = note
        .media
        .iter()
        .filter(|media| media.kind == MediaKind::Video)
        .count();
    transaction
        .execute(
            "INSERT INTO notes(path,title,url,published,created,updated,media_count,video_count,has_memo,has_related,mtime,size) VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12)",
            params![relative, note.title, note.url, note.published, note.created, note.updated, note.media.len() as i64, videos as i64, !note.memo_lines.is_empty(), !note.related_lines.is_empty(), mtime, size],
        )
        .map_err(|_| CoreError::Database)?;
    let note_id = transaction.last_insert_rowid();
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
    for (ordinal, media) in note.media.iter().enumerate() {
        let exists = safe_media_exists(root, relative, &media.path);
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
    Ok(true)
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
    let canonical = ensure_private_child_directory(cache_root, vault_root)?;
    set_private_directory(&canonical)?;
    Ok(canonical)
}

fn ensure_private_child_directory(
    directory: &Path,
    vault_root: &Path,
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
    if requested.starts_with(vault_root) {
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
        if canonical_ancestor.starts_with(vault_root) {
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

    #[test]
    fn includes_art_notes_but_hides_excluded_tags_and_filters() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        fs::write(
            root.join("one.md"),
            "---\ntags: [source/art, copyright/pin]\n---\n# One\n",
        )
        .expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = Gallery::open(&root, &database).expect("open");
        let report = gallery.scan().expect("scan");
        assert_eq!(report.notes_indexed, 1);
        let categories = gallery.categories().expect("categories");
        assert!(categories.iter().all(|category| category.path != "source"));
        assert!(
            categories
                .iter()
                .any(|category| category.path == "copyright"
                    && category.options.iter().any(|o| o.name == "pin"))
        );
        assert_eq!(
            gallery
                .query(&["source/art".into()], 10)
                .expect("query")
                .len(),
            1
        );
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
        let result = Gallery::open(&root, &root.join("index.sqlite"));
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
    fn incremental_scan_updates_changed_notes_and_removes_deleted_notes() {
        let root = temp_dir();
        fs::create_dir_all(&root).expect("create vault");
        let note_path = root.join("one.md");
        fs::write(&note_path, "---\ntags: [source/rating/safe]\n---\n# One\n").expect("write note");
        let database = root.parent().expect("parent").join(format!(
            "gallery-{}.sqlite",
            root.file_name().expect("name").to_string_lossy()
        ));
        let mut gallery = Gallery::open(&root, &database).expect("open");
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

        let mut first = Gallery::open(&first_vault, &database).expect("open first vault");
        first.scan().expect("scan first vault");
        drop(first);

        let mut second = Gallery::open(&second_vault, &database).expect("open second vault");
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
        let result = Gallery::open(&root, &external_index);
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
        gallery.scan().expect("scan");
        let categories = gallery
            .categories_for(&["source/gender/female".to_owned()])
            .expect("filtered categories");
        let gender = categories
            .iter()
            .find(|category| category.path == "source/gender")
            .expect("gender category");
        assert_eq!(gender.count, 3);
        assert_eq!(gender.options[0].name, "gender");
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
                .find(|category| category.path == "その他")
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
        let mut gallery = Gallery::open(&root, &database).expect("open");
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
}
