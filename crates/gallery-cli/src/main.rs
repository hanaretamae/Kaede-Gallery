#![forbid(unsafe_code)]

use gallery_core::{Gallery, diagnose_notes};
use std::env;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

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
    let mut args = env::args().skip(1);
    let command = args.next().ok_or_else(usage)?;
    let vault = PathBuf::from(args.next().ok_or_else(usage)?);
    if command == "diagnose" {
        let diagnostics = diagnose_notes(&vault).map_err(|error| error.to_string())?;
        for diagnostic in &diagnostics {
            println!("{}\t{}", diagnostic.path, diagnostic.reason);
        }
        println!("skipped={}", diagnostics.len());
        return Ok(());
    }
    let mut database = None;
    let mut filters = Vec::new();
    let mut remaining = args;
    while let Some(argument) = remaining.next() {
        if argument == "--database" {
            if database.is_some() {
                return Err(usage());
            }
            database = Some(PathBuf::from(remaining.next().ok_or_else(usage)?));
        } else if command == "list" {
            filters.push(argument);
        } else {
            return Err(usage());
        }
    }
    let database = match database {
        Some(path) => path,
        None => default_database_path(&vault)?,
    };
    let mut gallery = Gallery::open(&vault, &database).map_err(|error| error.to_string())?;
    match command.as_str() {
        "scan" => {
            let report = gallery.scan().map_err(|error| error.to_string())?;
            println!(
                "indexed={} warnings={}",
                report.notes_indexed, report.warnings
            );
        }
        "categories" => {
            let categories = gallery.categories().map_err(|error| error.to_string())?;
            for category in categories {
                println!("[{}] {}", category.display_name, category.path);
                for option in category.options {
                    println!("  {} ({}) [{}]", option.name, option.count, option.full_tag);
                }
            }
        }
        "list" => {
            for note in gallery
                .query(&filters, 200)
                .map_err(|error| error.to_string())?
            {
                println!(
                    "{}\t{}\tmedia={} videos={}",
                    note.path, note.title, note.media_count, note.video_count
                );
            }
        }
        _ => return Err(usage()),
    }
    Ok(())
}

fn default_database_path(vault: &Path) -> Result<PathBuf, String> {
    let base = env::var_os("XDG_STATE_HOME")
        .map(PathBuf::from)
        .or_else(|| env::var_os("HOME").map(|home| Path::new(&home).join(".local/state")))
        .ok_or_else(|| "a private state directory is unavailable".to_owned())?;
    let directory = base.join("vault-gallery");
    ensure_outside_vault(&directory, vault)?;
    std::fs::create_dir_all(&directory)
        .map_err(|_| "the private index directory could not be created".to_owned())?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        std::fs::set_permissions(&directory, std::fs::Permissions::from_mode(0o700))
            .map_err(|_| "the private index directory permissions could not be set".to_owned())?;
    }
    Ok(directory.join("index.sqlite"))
}

fn ensure_outside_vault(candidate: &Path, vault: &Path) -> Result<(), String> {
    let root =
        std::fs::canonicalize(vault).map_err(|_| "the Vault path is unavailable".to_owned())?;
    let absolute = if candidate.is_absolute() {
        candidate.to_path_buf()
    } else {
        env::current_dir()
            .map_err(|_| "the application state path is unavailable".to_owned())?
            .join(candidate)
    };
    let mut ancestor = absolute.as_path();
    while !ancestor.exists() {
        ancestor = ancestor
            .parent()
            .ok_or_else(|| "the application state path is invalid".to_owned())?;
    }
    let canonical_ancestor = std::fs::canonicalize(ancestor)
        .map_err(|_| "the application state path is unavailable".to_owned())?;
    let suffix = absolute
        .strip_prefix(ancestor)
        .map_err(|_| "the application state path is invalid".to_owned())?;
    if canonical_ancestor.join(suffix).starts_with(root) {
        return Err("the index must be stored outside the Vault".to_owned());
    }
    Ok(())
}

fn usage() -> String {
    "usage: gallery-cli <scan|categories|list|diagnose> <vault-path> [--database <database-path>] [tag ...]"
        .to_owned()
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
        env::temp_dir().join(format!("gallery-cli-{id}"))
    }

    #[test]
    fn refuses_to_create_default_index_inside_vault() {
        let root = temp_dir();
        std::fs::create_dir_all(&root).expect("create vault");
        let result = ensure_outside_vault(&root.join("new-index"), &root);
        assert!(result.is_err());
        assert!(!root.join("new-index").exists());
        std::fs::remove_dir_all(root).expect("remove vault");
    }
}
