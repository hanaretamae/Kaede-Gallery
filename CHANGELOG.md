# Changelog

Release notes are drafted from implementation changes and verified results, then reviewed by the release owner.
See [CHANGELOG.ja.md](CHANGELOG.ja.md) for the Japanese changelog and earlier release history.

## [2.0.0b3] - 2026-10-09 (Beta)

### Added

- Added universal, arm64-v8a, and x86_64 Android APK variants to release packaging. GitHub Release titles now contain only the version.
- Applied the supplied adaptive launcher icon to Android.
- Adopted the latest published Compose Multiplatform Material 3 Expressive alpha and Android Material 3 beta, including expressive motion and theme shapes.

### Fixed

- Fixed Windows archive packaging to locate the generated Compose Desktop launcher instead of assuming a fixed executable path.
- Restored Android system/dynamic colors on initial Vault selection and handled Back within Settings and the viewer.
- Centered note-loading feedback, restored horizontal media swipes, and enabled looping video playback on Android and Desktop.
- Cached validated SAF document IDs and bounded image thumbnails in private app data to avoid repeated folder traversal.

### Changed

- Reworked the gallery around a Vault-name/count app bar, Notes/Media tabs, icon actions, and image-first tiles with count badges.
- Moved search, tag, date, and sort filters into the filter surface; kept Vault change/forget actions in Settings with icon-led list navigation.
- Raised the Android minimum supported version to API 24 and compile SDK to API 37.1 for the current Material 3 Expressive dependencies.

## [2.0.0b2] - 2026-10-09 (Beta)

### Fixed

- Fixed the AArch64 Linux Desktop Nix dependency cache; the declared `aarch64-linux` package now builds successfully.

### Changed

- The Gradle dependency updater now supports per-system cache updates and merges new artifacts without dropping existing platform entries.

## [2.0.0b1] - 2026-10-08 (Beta)

### Changed

- Retired Flutter and its bridge; Kotlin Multiplatform / Compose with the Rust FFI is now the sole active app and release path.
- Unified application versioning in `VERSION` and moved Android, Linux, and Windows packaging to the KMP/Rust toolchain.
- Declared Nix Desktop packages for x86_64 and aarch64 Linux; the aarch64 build still needs local QEMU/binfmt verification.
- Use the unprefixed `2.0.0b1` Git tag and publish beta versions as GitHub prereleases.

## [Unreleased]

## [1.6.2] - 2026-10-06

### Fixed

- Fixed the Windows system accent colour not updating until the app was reopened.
- Fixed missing video thumbnails on Windows.
- Fixed "Open in Obsidian" on Windows failing with "Vault not found".
- Fixed "Open with" and "Show in file manager" on Windows opening the wrong thing; the file manager now selects the file.
- Fixed the Open media menu being truncated on desktop; it is now a dialog.
- Pure black no longer discards Material You tinting on lists, cards and sheets.
- Renamed the executable to `kaede_gallery` (`kaede_gallery.exe` on Windows).
- Linux application ID is now `com.hanaretamae.kaede_gallery` and the desktop file matches it, so panels such as Waybar can show the app icon.
- Video controls are now separate Material 3 Expressive buttons and an opaque time chip; the seek bar has no frame.
- Settings choice buttons use the muted secondary container instead of the saturated primary colour. The system accent is used as-is (tonal-spot palette) so primary matches the OS accent.
- System colour now uses the vibrant variant (same hue as the OS accent, more saturated surfaces) so enabling Material You is clearly visible.

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
