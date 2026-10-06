# Changelog

Release notes are drafted from implementation changes and verified results, then reviewed by the release owner.
See [CHANGELOG.ja.md](CHANGELOG.ja.md) for the Japanese changelog and earlier release history.

## [Unreleased]

## [1.6.2] - 2026-10-06

### Fixed

- Fixed the Windows system accent colour not updating until the app was reopened.
- Fixed missing video thumbnails on Windows.
- Fixed "Open in Obsidian" on Windows failing with "Vault not found".
- Fixed "Open with" and "Show in file manager" on Windows opening the wrong thing; the file manager now selects the file.
- Fixed the Open media menu being truncated on desktop; it is now a dialog.

### Changed

- Set a minimum window size (640x480) so at least two images fit side by side.

## [1.6.1] - 2026-10-06

### Fixed

- Fixed Android wallpaper intents for Android package visibility.
- Corrected public installation and contributor documentation.

## [1.6.0] - 2026-10-06

### Added

- Added English UI. English is the default when the system language is not Japanese; choose System, Japanese, or
  English in Appearance settings.
- Added English heading aliases (`Memo`, `Notes`, `Related`, and `Document`) to the defaults.
- Reworked the README into an English-first guide, retained a Japanese edition, moved usage before build and
  installation instructions, and added a Markdown example note.

### Changed

- New filter categories use English default names in English locales; saved settings are unchanged.
