# Changelog

Release notes are drafted from implementation changes and verified results, then reviewed by the release owner.
See [CHANGELOG.ja.md](CHANGELOG.ja.md) for the Japanese changelog and earlier release history.

## [2.1.3] - Draft

### Added

- Configure frontmatter author keys, defaulting to `author`, and provide localized note examples that reflect current settings.

### Changed

- Keep automatic gallery columns at 0 and allow fixed counts from 1 through 12.
- Rename Note and Tags settings and clarify frontmatter and tag-filter controls.
- Keep the controls shown after video auto-hide consistent on Android and Desktop.

### Fixed

- Correct bundled AppImage font discovery on NixOS.

## [2.1.2] - Draft

### Fixed

- Preserve touch-scroll momentum when dragging the Desktop gallery.
- Correct Linux X11 window-size synchronization for rotated, scaled displays.

## [2.1.1] - Draft

### Added

- Enable direct vertical drag scrolling in the Desktop gallery while preserving wheel and trackpad scrolling.

### Changed

- Reduce the default Desktop window size and adapt gallery columns to the available width and display density.

## [2.1.0] - Draft

### Added

- Add page-scoped reset actions to Note structure and Tag settings.
- Save named gallery searches and restore their search and filter criteria.
- Open gallery results filtered by a tag selected in note details.
- Show selected tag and content-filter counts beside their filter categories.
- View saved-search criteria and edit them to update the saved search.

### Changed

- Remove the unavailable-media icon option from Paging and list settings.
- Separate the Note structure reset action from its link-resolution controls.
- Remove the Japanese reference translation of the MIT License from the repository and Windows packages.
- Use a Material 3 Expressive menu for saved searches.

### Fixed

- Bundle Noto Sans CJK with AppImage packages so Japanese text can render when no matching host font is installed.

## [2.0.2] - Draft

### Fixed

- Make AppImages use the host's Fontconfig configuration by default, so
  installed system fonts, including Japanese fonts, can be found.

## [2.0.1] - Draft

### Fixed

- Keep the video surface usable while collapsing viewer details with reverse scrolling.
- Suppress mpv's non-subtitle on-screen display so playback controls remain in the app.

### Changed

- Bundle pinned mpv and FFmpeg with Linux release executables and AppImages, including their runtime closure and license materials.

## [2.0.0rc2] - Draft

### Fixed

- Windows: show the full Compose video controls and connect playback, seeking, speed, looping, and mute actions to mpv.
- Stabilized detail-panel gestures so resizing and note-content scrolling happen in sequence instead of competing during a drag.
- Handle Windows extended-length Vault paths in the picker, startup display, and settings export; keep Vault removal from leaving the app loading.
- Open an already-running Windows Explorer window with the selected media item.

### Changed

- Windows distributions place the Japanese license reference and English/Japanese README and third-party notices beside `Kaede-Gallery.exe`.

## [2.0.0rc1] - Draft

### Fixed

- Fixed viewer image loading and video-player fullscreen controls returning to the in-app details view in one action.

### Changed

- Linux release bundles no longer include FFmpeg or mpv and are distributed as `.tar.zst` archives; install `mpv` and `ffmpeg` on `PATH` for video playback and thumbnails.
- Windows: fixed verbatim (`\\?\`) paths that broke image/video loading and "open"/"show in file manager"; opening notes in Obsidian no longer goes through the browser; the system accent color and theme are applied; the Vault picker uses the modern folder dialog; leaving fullscreen relayouts the window; forgetting a Vault no longer loads forever. A bundled `tools/ffmpeg.exe` or `tools/mpv.exe` next to the launcher is preferred over `PATH`. The Windows ZIP now ships `Kaede-Gallery.exe` with LICENSE and README files,. A Windows ARM64 build was added; the uniffi runtime DLL is built from the pinned upstream tag because its JAR lacks `win32-aarch64`. Build-only, not hardware-verified.
- Linux releases also ship `.AppImage` files, built with a Nix-pinned `appimagetool`; the Windows ZIP bundles pinned GPL `mpv.exe` and `ffmpeg.exe` under `tools/`.
- Viewing a related item keeps the in-app details view; the link-resolution heading matches other settings rows.
- Refined gallery loading, viewer transitions, responsive controls, and appearance settings.

## [2.0.0b5] - 2026-10-11 (Beta)

### Added

- Added fuzzy-search controls and `~word` / `=word` search syntax, plus gallery loading feedback and a position popup for the draggable scrollbar.
- Added responsive gallery paging and viewer/detail transitions, horizontal trackpad navigation, and improved Android video-thumbnail fallbacks.

### Fixed

- Kept count badges in the Material You secondary-container roles instead of the default error red.
- Corrected detail-panel scrolling and media sizing, fullscreen behavior, playback-speed transitions, and the scrollbar position popup.
- Aligned light, dark, system-accent, fallback, and pure-black appearance behavior with the Material 3 Expressive color roles.

### Changed

- Updated fallback palettes to the MaterialKolor Expressive 2025 palette and consolidated system/fallback/pure-black scheme selection.
- Refined the filter and Vault-selection surfaces around Material 3 Expressive components.

## [2.0.0] - 2026-10-12

### Added

- Replaced the retired Flutter app and bridge with Kotlin Multiplatform / Compose and the Rust parser, core, and FFI as the active application and release path.
- Added the shared gallery, search, tag/date filters, paging, note viewer, Obsidian link handling, and configurable note structure across Android and Desktop.
- Added Compose video controls for play/pause, seeking, playback speed (0.25×–2×), looping, and mute; Linux uses mpv for embedded video rendering.
- Added Android universal, arm64-v8a, and x86_64 APK packaging, Linux x86_64 and AArch64 Desktop bundles, and Windows x64 and ARM64 packaging workflows.
- Added Linux XDG Desktop Portal system color-scheme and accent-color support, responsive/fixed gallery columns, English/Japanese UI, and import/export for settings.
- Added offline third-party license notices to application distributions.

### Fixed

- Improved Android SAF media handling with provider previews, bounded thumbnail decoding, private caching, and additional video-frame fallbacks.
- Fixed Windows media opening, file-manager selection, Obsidian links, video thumbnails, system accent refresh, and Desktop archive launcher discovery.
- Fixed Android and Desktop video looping/restart behavior, viewer navigation and Back handling, gallery/detail toolbar layout, and bounded cache/path handling.
- Preserved a minimum video area while expanding viewer details and made reverse scrolling collapse the panel after note content reaches its top.
- Disabled mpv's on-screen controller and default input bindings so video interaction stays in the app's Compose controls.
- Preserved Material You tinting in pure-black mode and improved video seek/control presentation.

### Changed

- Reworked the gallery and settings navigation, filters, note details, themes, and controls around Material 3 Expressive components and responsive layouts.
- Moved Vault selection, settings, indexes, and caches to read-only Vault access with app-private persistence; the application does not write into the Vault.
- Linux `.tar.zst` bundles and `.AppImage` files use `mpv` for playback and `ffmpeg` for thumbnails from `PATH`; these executables are not bundled.
- Raised Android's minimum supported version to API 24 for the current Material 3 Expressive dependencies and standardized release versioning through `VERSION`.

## [2.0.0b4] - 2026-10-11 (Beta)

### Added

- Added XDG Desktop Portal system color-scheme and accent-color support on Linux, including KDE Plasma, GNOME, and COSMIC portal backends, with an Expressive Material 3 color scheme.
- Added Compose video controls on Linux for play/pause, seeking, playback speed, looping, and mute; mpv is retained only as the embedded video renderer.
- Added selectable fixed gallery column counts and responsive columns with a 200dp minimum tile width.

### Fixed

- Improved Android SAF media thumbnail loading with provider-generated previews, bounded concurrent decoding, a private in-memory cache, and additional MP4 frame fallbacks.
- Corrected gallery and detail toolbar layout, filter category expansion behavior, and trackpad media navigation.

### Changed

- Refined gallery, filters, settings, and viewer surfaces to use Material 3 Expressive components and layouts; moved the Notes/Media menu next to position navigation.
- Updated system-color defaults and documented the Linux appearance and in-app video behavior.

## [2.0.0b3] - 2026-10-10 (Beta)

### Added

- Added universal, arm64-v8a, and x86_64 Android APK variants to release packaging. GitHub Release titles now contain only the version.
- Added an x86_64-hosted AArch64 Linux cross-build for the Rust FFI and self-contained Desktop bundle, without QEMU/binfmt.
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
