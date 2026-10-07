# Kaede Gallery design

This document records the implementation contract for the repository. The
approved Kotlin Multiplatform / Compose Multiplatform migration brief is the
source of truth for the target architecture and migration phases. The
historical Flutter behavior documented below remains the compatibility
contract until each replacement has passed parity checks. Report other
conflicts instead of silently overriding either contract.

## Purpose and scope

Provide an offline gallery for media and metadata in an Obsidian Vault,
initially targeting Linux and Android, with Windows support in progress. Rust
owns parsing, scanning, indexing, filtering, and image thumbnails. Kotlin
Multiplatform and Compose Multiplatform are the target application and shared
UI layers; Android and desktop integrations remain platform-specific. The
Vault is always read-only.

The application must not edit, create, delete, or move Vault files; render
Markdown; synchronize data; automatically access the network; send telemetry,
ads, or crash reports; or generate/tag images with AI. Only fictional test
notes and media may be committed.

## Architecture and migration phase order

Target dependency direction:

```text
Compose UI -> Kotlin state/repository -> Rust FFI facade -> gallery-core -> gallery-parse
```

Dependencies point downwards only. `gallery-parse` is pure parsing without
filesystem I/O. `gallery-core` does not know about Kotlin, Compose, or the FFI
facade. Common Kotlin code owns domain-facing models, repositories, state,
settings, navigation, and shared UI. UI calls Rust only through a repository
and a coarse-grained FFI facade; Composables do not call bindings directly.
Platform-specific behavior is injected behind platform capabilities. Android
SAF and Media3 remain separate from desktop filesystem, window, and video
integrations.

1. **Complete (historical):** Rust parser, core, CLI, fictional fixture
   generator, and initial benchmark.
2. **Complete (historical):** Flutter/Linux gallery list with native
   video-frame thumbnails.
3. **Complete (historical):** Viewer, details, Obsidian links, and video.
4. **Complete (historical):** Android, including Storage Access Framework
   support.
5. **In progress (legacy delivery):** Windows x64 builds from a manual GitHub
   Actions workflow. Keep this Flutter release path working while the KMP
   replacement is developed; do not make the KMP migration wait on it. Native
   arm64 builds are blocked because the bundled Windows video dependencies
   (`libmpv` and ANGLE) are x86_64-only; Windows on Arm can run the x64 build
   under emulation. Windows builds are unverified on real hardware.
6. **Complete:** Rust Core readiness. The Rust API, safety, tests, limits, and
   performance baseline have been audited; the Kotlin-facing facade contract
   is documented. Reuse existing functionality instead of duplicating it.
7. **Complete (foundation):** Add Kotlin Multiplatform / Compose Multiplatform
   modules and build/test infrastructure while retaining the Flutter app.
8. **Complete for shipped KMP targets:** Generated Kotlin bindings sit behind
   the read-only repository API. Android and desktop JVM builds pass, and the
   Windows-host JVM ABI and distributable pass CI. The separate `mingwX64`
   library is not used by the Windows Compose application; keep the Flutter
   MSVC release path.
9. **Implemented; parity acceptance tracked in phase 13:** Gallery, Search,
   Filter, and Viewer use shared Compose UI with immutable state, paging, and
   stable keys.
10. **Implemented; parity acceptance tracked in phase 13:** Shared settings,
    localization, theme, and Material 3 Expressive presentation use private
    app data and do not write into the Vault.
11. **Implemented and fixture-accepted:** Android SAF and lifecycle
    integration use the Android-native video backend. The 2,000-note
    unchanged-scan target is met on the tested device/provider with the shared
    bound; see `docs/phase1-performance.md`.
12. **Implemented; runtime acceptance is target-specific:** Linux uses desktop
    filesystem/window/video integrations. Windows x64 JVM packaging and tests
    pass CI, including the embedded-mpv platform-selection path. Physical
    Windows runtime testing is not included in the current acceptance plan.
13. **In progress:** Continue verifying cross-platform functional parity,
    security properties, build/release paths, and measured performance on
    fictional fixtures before retiring any Flutter target.
14. **Deferred:** Remove Flutter only after all replacement targets and
    release paths have passed parity and acceptance checks.
15. **Future platform:** Keep the common architecture extensible to macOS and
    iOS. iOS external Vault access remains blocked on a separate design
    decision; do not imply it is supported by this migration.

Implementation for shared UI, settings, Android, and desktop is present;
phase 13 remains the parity and acceptance gate. Do not treat passing builds
or partial target acceptance as proof of complete migration.

### Phase 13 acceptance status

Acceptance compares observable behavior on the same fictional Vault and settings
fixtures; it does not require pixel-identical rendering. The Flutter app and
its tests remain the reference until every required behavior below is covered
by a KMP test or a recorded target-specific interaction check.

**Owner-approved exception (2026-10-07):** GNOME wallpaper interaction testing
is excluded from Phase 13 acceptance and may be checked or fixed after this
phase. Do not change the host desktop wallpaper during development or
acceptance. This exception applies only to the GNOME wallpaper interaction; it
does not waive other Linux desktop, Android, Windows, or parity checks.

| Area                                                                         | Evidence                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Status                                                                                                                                                                                          |
| ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Rust parsing, indexing, filtering, and FFI                                   | Workspace tests pass against fictional data; Vault immutability and bounded SAF scan behavior are covered.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Verified                                                                                                                                                                                        |
| Shared gallery, search, filters, paging, settings transfer, and note details | KMP state, codec, and UI tests pass; Android API 36 fixture checks cover gallery/search/detail navigation and settings transfer. A disposable 30-note fictional SAF fixture confirmed indexed jumps, backward paging, and filter clearing. Settings-transfer fixtures cover Flutter `showItemCount`, the separate KMP tile-count preference, and older KMP v1 exports. The details panel now follows Flutter's 40%-to-82% expansion over 220dp and unzoomed media-area vertical drags forward to its scroll state. A post-change API 36 check with an unzoomed fictional image confirmed vertical swipes over media expand and collapse the details panel; JVM tests cover the exact density-scaled threshold and hidden/fullscreen/video/zoom suppression. New Flutter and KMP repository-adapter tests independently read the same read-only `testdata/dummy-vault` with temporary private index/cache data and assert matching 12-note/1-warning scans, `note-000000.md` details/image, and `source/rating/safe` filtering. Both tests pass in the pinned Nix development shell. This establishes repository-boundary fixture parity, not side-by-side UI/runtime parity; Android Flutter runtime comparison remains blocked by Digital Wellbeing / Screen Time. | Repository fixture parity verified; live Flutter/KMP UI parity remains pending                                                                                                                  |
| Android SAF, media, and performance                                          | API 36 fixture checks cover SAF selection, image and video display, navigation, and private-index reuse; the 2,000-note scan and scroll measurements are recorded in `docs/phase1-performance.md`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Verified for the recorded device, provider, and scenarios only                                                                                                                                  |
| Linux Compose Desktop                                                        | An earlier packaged-launcher Xvfb smoke test with fresh private app data covered fictional Vault selection, gallery, note detail, fullscreen, file-manager reveal, and external image open. Linux Vault selection now calls `org.freedesktop.portal.FileChooser.OpenFile` with directory mode; on the current Niri host, the portal returned a request handle and displayed its GTK picker. The automated attempt to select `testdata/dummy-vault` was inconclusive because `wtype` key events did not reach the native portal window, so end-to-end selection through Compose remains unverified. JVM tests cover request construction, request/response parsing, and local-directory validation. Direct calls against fictional `media/pixel.png` returned a portal request from `xdg-open` and `()` from `FileManager1.ShowItems`; process inspection showed Koko opening the fixture and Dolphin launched with `--select` for that path. These direct handler calls exercise the current non-GNOME dispatch, but were not clicks through the Compose Desktop UI. GNOME wallpaper interaction is explicitly waived for Phase 13 by the owner-approved exception above; no host wallpaper was changed.                                                            | Direct current non-GNOME handler calls verified; Compose UI interaction pending; GNOME wallpaper check waived                                                                                   |
| Windows Compose Desktop                                                      | [CI run 12](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/37559730491) verifies the earlier Windows-host JVM and distributable path. [CI runs 13](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/37629925629) and 14 reached the new fixture-parity test but failed on Windows path handling. The assertion now checks a separator-normalized media path suffix, avoiding platform-specific `Path` parsing; it passes on Linux, and a Windows-host rerun is pending. Physical-device execution remains outside this acceptance plan.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Prior CI baseline; current Windows-host verification pending                                                                                                                                    |
| Viewer and platform media actions                                            | Shared KMP viewer retains media-only/detail/fullscreen transitions and forwards unzoomed media-area vertical gestures to the note details. On API 36 with a fictional SAF fixture, typed Open displayed image handlers; wildcard Reveal opened Files at the media folder with `pixel.png` selected; the wallpaper intent displayed the destination confirmation, which was canceled without applying a wallpaper. The offline license screen opened bundled Apache-2.0 text. Desktop resolves media canonically inside the Vault, uses Linux `FileManager1.ShowItems` and Windows Explorer selection, and exposes GNOME wallpaper setting. JVM tests cover image eligibility, Vault containment, desktop resource contents, and file-manager arguments. On Niri, direct current Linux media-handler calls opened the fictional image in Koko and launched Dolphin with the item selected; the Compose Desktop UI click path was not exercised. The final Android details-gesture threshold/visibility guard was rechecked on API 36 with an unzoomed image and fictional fixture. GNOME wallpaper interaction is waived for Phase 13; it was not exercised and no wallpaper was changed.                                                                            | Android interactions verified on the recorded API 36 fixture; Linux handler calls directly verified but Compose UI click path and live Flutter/KMP parity pending; GNOME wallpaper check waived |

On 2026-10-07, Rust workspace tests, Flutter's 64 widget tests and analysis,
KMP Rust/settings/UI/desktop JVM tests, Linux Compose Desktop packaging, and
the earlier Android acceptance assembly passed. A fresh `:androidApp:assembleDebug`
also passed with the installed Rustup 1.98.1 compiler and Android targets. The
default Nix `RUSTC` failed that Android build because its standard library is
host-only (`can't find crate for core`/`std`). KMP JVM tests cover the
density-scaled detail-panel geometry, license catalog, and bundled Desktop
license resources; the Android APK and Desktop package contain the offline
license assets. On API 36, the KMP acceptance app
was exercised with two disposable fictional SAF fixtures. The short fixture
verified typed image Open, SAF Reveal selecting `pixel.png`, and the wallpaper
destination prompt (canceled before application). An earlier long fictional note showed media-area swipes expanding and collapsing
the panel, but that check preceded the final threshold conversion and guard. A
fresh API 36 check on the final acceptance APK used a disposable copy of
`testdata/dummy-vault`; on `Fictional note 000004`, a vertical swipe over the
unzoomed image visibly expanded the details panel and the reverse swipe
collapsed it. The exact 220dp threshold and hidden/fullscreen/video/zoom guards
remain covered by JVM tests. The offline license page loaded the bundled
Apache-2.0 text. The installed acceptance app and device fixture were removed
after the check; the separate Flutter release package was not changed.

A Flutter arm64 debug APK was built using a temporary acceptance application ID
and a writable SDK view of the installed Nix Build Tools. Runtime parity could
not be recorded: Android's Digital Wellbeing / Screen Time restriction blocked
the acceptance app during the launch/picker flow, so no Flutter gallery or
viewer comparison was captured. No real Vault was selected or modified. The
earlier Linux Xvfb fixture run covered Vault selection,
gallery, detail, fullscreen, file-manager reveal, and external image opening;
it did not exercise the new GNOME wallpaper action; that interaction is
explicitly waived for Phase 13 by the owner-approved exception above. No
GNOME or Android wallpaper was applied. Windows physical-device testing is not
required by the current plan. KMP's in-app license list is available offline
but groups Maven dependencies rather than matching Flutter's per-package
registry granularity. `cargo fmt --all -- --check` with the Nix rustfmt reported
import-order differences in generated `crates/gallery-bridge/src/frb_generated.rs`;
no Rust files were changed here. New same-fixture Flutter/KMP repository tests
close repository-boundary fixture parity, but not side-by-side UI/runtime parity.
Direct current Linux non-GNOME handlers are verified on Niri, but Compose Desktop
UI interaction and portal-based Vault selection are still unverified. The portal
GTK dialog was observed, but this Niri session did not accept the available
synthetic keyboard input. Windows-host CI runs 13 and 14 failed in the new fixture-parity test on Windows path handling. The assertion now checks a separator-normalized media path suffix instead of parsing a Rust-returned native path as a JVM `Path`; the Linux test passes, and rerunning Windows CI is still required. The pinned Nix development shell provides Flutter, Rust/Cargo, JDK 17, GitHub CLI, KeePassXC, `xdotool`, `wtype`, and `xwd`; it exports explicit `RUSTC`/`RUSTDOC` paths so UniFFI binding generation works under Gradle, but those Nix compiler paths are host-only for Rust Android cross-builds. The installed Rustup 1.98.1 targets were used for the successful current Android debug APK assembly. The 2026-10-07 Flutter and KMP results above are Linux-host results, not Windows-host CI. GitHub CLI authentication is available through the system keyring; Windows-host CI for this revision has not yet run. Phase 13 cannot be closed, so Phase 14 (Flutter removal) remains deferred.

The API 36 viewer follow-up also verified that a note opens with its media
controls and details hidden, a media tap reveals the note details, toolbar
actions can be horizontally reached on the device, and full-screen entry and
Escape return to the expected presentation. Escape from the detail view returns
to media-only mode. This closes the Flutter/KMP viewer visibility mismatch but
does not close the broader cross-implementation parity gate.

### Current KMP implementation status

The repository currently contains the KMP model, repository, Rust binding
adapter, and initial shared Gallery/Search/Filter screens with paging and
filter state, including indexed position jumps. A shared Gallery-to-note-detail
route, related-note navigation, note dates and tags, and same-note media
navigation are also present; the common viewer delegates media rendering to a
platform capability. Linux image display is implemented in `desktopApp`;
Android has an application module with SAF selection, bounded staged scans,
private-index reuse on launch, explicit rescan, bounded image decoding, and a
Media3 video player. API 36 device smoke tests verified fixture selection and
reading, indexed position and linked-note navigation, private-index reuse,
gallery-target-prefix editing and rescan, and visible image tile previews. On
2026-10-07, KMP was also exercised through Android's Documents provider with a
dedicated 2,000-note fictional fixture; search, note details, grid paging, and
horizontal action scrolling worked. Viewer controls now respect the status-bar
safe area. Compose scrolling was measured at the device's active 120 Hz mode;
results and explicit SAF rescan timings are recorded in
`docs/phase1-performance.md`. Android visual/interaction acceptance is
complete for these fixture scenarios. A bounded SAF reader scheduling change
reduced the measured 2,000-note unchanged rescan from 12.7 s to 3.59–3.95 s
with the shared limit of 16 concurrent note reads; these measurements meet
the broad few-seconds target on this device and provider. The app also provides
a confirmed forget-Vault action that removes the private index and releases the
persisted read grant. Its debug APK and Android arm64/x86_64 native libraries
build successfully.
Linux/JVM Compose Desktop packaging is available; a Windows x64 CI job has been
added to verify the JVM FFI ABI and application packaging.
Common appearance, gallery-pagination, and gallery-target-prefix settings
models, state, controls, and Android and desktop private-settings adapters are
present. Both platforms now
support versioned, size-bounded settings-file import/export with Vault-safe
destination checks. Theme selection,
pure-black surfaces, platform system-locale fallback, and Android dynamic-color
scheme injection are wired at the shared app boundary.
Rust selected-Vault storage calls and filesystem session open,
initial scan, index reuse on launch, rescan, and disposal are wrapped by typed
Kotlin repositories; an integration test exercises initial scans, index reuse,
and explicit rescan against a temporary fictional Vault. Both Android and
desktop offer a confirmed forget-Vault action.
The `desktopApp` Compose Desktop module provides Linux and Windows/JVM entrypoints,
directory selection, private settings persistence, Rust filesystem indexing,
index reuse on launch, an explicit rescan action, and bounded-resolution image
display. Shared gallery tiles now load bounded image previews through the
platform boundary. Android also extracts scaled video frames through the SAF
read-only file descriptor on API 27 and later; older Android releases do not
support video tile thumbnails. Linux Desktop extracts one bounded PNG frame
through FFmpeg for gallery tiles. The same extractor supports Windows when an
`ffmpeg` executable is available on `PATH`; Windows runtime behavior has not
yet been verified. A fictional H.264 video was selected from a dedicated SAF
fixture on an API 36 device; the viewer played it and the gallery tile displayed
its magenta video frame. Desktop video playback uses the embedded mpv child
window on Linux and Windows; Windows runtime playback still requires
independent runtime acceptance. The 2026-10-07 KMP fixture run completes visual
acceptance for gallery/search/note-detail surfaces; media playback and
thumbnail behavior were separately smoke-tested on API 36.
Linux and Windows video are rendered in an embedded native child window from a
separate `mpv` process; desktop runtime requires `mpv` and `ffmpeg` on `PATH`.
Launch disables mpv user configuration, scripts, subtitle/audio auto-loading,
and automatic sidecar loading. Desktop video thumbnails limit ffmpeg to local
file input, one frame, 512-pixel bounds, a 4 MiB output cap, a 10-second
timeout, and two concurrent extraction processes. A basic localized About
section now links to the project and license. Windows x64 CI passed for the
current migration branch at commit `076436a`
([workflow run](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/37559570910)).
This verifies the Windows build and packaging path, not execution on physical
Windows hardware or runtime availability of native mpv embedding there.
Android visual/interaction checks are complete for the
documented fixture scenarios, while full cross-platform parity remains
unfinished. Keep Flutter and its release paths until those gaps are verified;
the current Compose UI is not yet a replacement.
KMP now supports editing Flutter-compatible tag color and filter-category
rules. The most-specific color prefix is applied to filter options and viewer
tags; category changes are written to private Rust settings and rescan the
index while preserving base note-structure settings. Transfer continues to
preserve the complete tag-settings JSON. Note-structure aliases and note-detail
block order/visibility are editable and applied to the KMP viewer. The parser
now exposes bounded, link-aware post-text-end lines through both Rust FFI
surfaces and both viewer models, and Flutter and KMP render the block according
to configured order and visibility. This resolves the former design/Flutter
contradiction in favor of the documented viewer contract. Visual acceptance
on API 36 is recorded below; Rust parser/core/FFI tests, KMP JVM adapter/viewer tests, the
Android debug APK build, and a connected API 36 launch smoke test pass for this
change. Flutter analysis and full Flutter widget tests pass with the installed
Rust and Flutter SDK links. The recorded 7,806-note and 20,000-note
measurements cover Rust CLI operations only. Android SAF and Compose
frame-time measurements for the 2,000-note KMP fixture are recorded in
`docs/phase1-performance.md`; the latest measured explicit SAF rescan is within
the broad few-seconds target on the tested device and provider, though this
does not establish performance across all SAF providers. Windows x64 CI passed
on commit `0064076` (see the workflow run linked above); physical Windows
runtime testing is intentionally outside this acceptance plan.

KMP settings transfer now reads and writes Flutter's version-1 JSON appearance
and tag-settings structure while retaining unexposed note-structure fields
verbatim as bounded JSON; supported gallery settings are updated in
place, and the prior line-oriented KMP format remains readable. Settings tests
cover this transfer compatibility, strict validation, bounds, and legacy
imports. KMP exposes Flutter's tag color and category rules, persists category
changes to the private Rust settings document on Android and desktop, and
preserves unrelated note-structure values when applying them. Fresh KMP exports
use Flutter's default tag-color rules and system-accent default, avoiding
changes to those settings on a
Flutter import. KMP also exposes Flutter's included and hidden tag-prefix
rules, persists them in private settings, includes them in transfer JSON, and
applies them to available filter options; oversized exports are reported rather
than written.
KMP's note-structure editor also updates heading/frontmatter aliases, link
resolution, quoted-post-text behavior, note-detail block order, and visibility.
The viewer applies order and visibility to author, post text, memo, related,
and post-text-end blocks; fixed frontmatter fields remain ahead of those
blocks. Media continues to use the platform viewer surface, matching Flutter's
separation.
The KMP viewer now offers an explicit Open in Obsidian action. Android builds
the URI from the selected SAF Vault name and validated relative note path;
Desktop resolves the note canonically inside the selected Vault before
launching the absolute-path URI. Desktop tests cover URI encoding, traversal,
and symlink escapes. The action is visible in the API 36 note-detail toolbar;
the URI was not dispatched because an installed Obsidian handler was not part
of this fixture check.
The KMP viewer toolbar also now derives Flutter-compatible filename title and
author fallbacks, validates HTTP(S) URLs without credentials, and exposes
author-profile, original-page, and copy-URL actions for valid X/Twitter status
links. Shared tests cover URL validation, profile extraction, and metadata
fallbacks. An API 36 fictional-note interaction verified the derived title and
author, all three toolbar actions, and the localized copy confirmation. External
profile and post URLs were not dispatched during the check.

The current parity follow-up maps Flutter's `pagination.showItemCount` to the
KMP loaded-range display without conflating it with tile counts, while retaining
imports of earlier KMP v1 transfer files. Scan warning counts refresh after
successful rescans and the Vault settings expose a privacy-safe warning
explanation. Android media actions now distinguish typed Open, wildcard SAF
Reveal, and supported image wallpaper intents; Linux GNOME wallpaper and
Windows Explorer item selection have corresponding desktop integrations. API 36
fixture interactions confirm Android Open, Reveal, and wallpaper dispatch
through the target chooser/UI without applying a wallpaper. KMP also provides an
offline license list for Apache-2.0, JNA's Apache/LGPL alternatives, and the
Rust/UniFFI report. The list is intentionally grouped and does not yet match
Flutter's per-package dependency registry. The owner-approved Phase 13
exception waives GNOME wallpaper interaction testing; the final Android viewer
gesture path has been rechecked on API 36, with its geometry and suppression
guards covered by JVM tests.
The 2026-10-07 tag-color/category follow-up adds a localized category editor,
strict path/size validation, Flutter JSON round-trip coverage, and private Rust
settings updates on Android and desktop. Category updates preserve other
note-structure fields and rescan the index; a Rust regression test confirms the
new categories override only category rules from the base settings file.

The note-structure follow-up adds validated Flutter-compatible block ordering
and hidden-block settings to KMP transfer and persistence, plus settings
controls and matching viewer behavior for author, post text, memo, and related
blocks. It preserves the Flutter legacy default orders and continues to keep
fixed frontmatter and platform media rendering outside the reorderable content
blocks. Settings and shared-UI JVM tests, Desktop tests, Android compilation,
and debug APK assembly passed; the updated APK was installed and launched on
the connected API 36 device. This launch check does not replace visual or
fixture-based acceptance.

The 2026-10-07 follow-up added a bounded Linux FFmpeg video-tile thumbnail
extractor and a fictional-video extraction test. Kotlin settings/shared-UI and
desktop tests, Android debug APK assembly, and Linux Desktop distribution
packaging pass. The APK was installed and launched on the connected API 36
device; this was a startup smoke check, not a visual/settings interaction test.
The Rust workspace tests, formatting, Clippy, Flutter tests, and Flutter
analysis pass. Current Linux CLI scans of fictional 7,806- and 20,000-note
fixtures indexed the expected eligible subsets with zero warnings; the
measurements are recorded in `docs/phase1-performance.md` and do not replace
Android SAF or Compose frame-time acceptance. At the time of this checkpoint,
Windows CI had not yet verified the then-uncommitted migration changes.

The settings-transfer follow-up passed all 13 settings tests, shared UI JVM
tests, desktop tests, Android Kotlin compilation and debug APK assembly; Rust
formatting and workspace tests also pass. The updated APK was installed and
launched on the connected API 36 device with its activity resumed. A later
API 36 smoke test selected only the synthetic SAF fixture, exported a valid
version-1 settings file to the device's Documents directory outside the
fixture Vault, imported a modified page size (31), then restored the exported
value (24). The generated export file was removed after verification; no Vault
file was written. A separate video fixture test used only a synthetic note and
generated H.264 clip on device storage; it confirmed SAF video playback and
thumbnail decoding/rendering.
The tag-color/category follow-up passed settings and shared-UI tests, the Rust
workspace suite and Clippy, desktop tests, and Android debug APK assembly. That
APK was installed and launched on the connected API 36 device; no Vault was
selected for this smoke launch.

Latest local verification (2026-10-07): Rust workspace tests, formatting,
Clippy, and dependency policy checks pass. Kotlin/JVM tests and compilation,
including generated UniFFI bindings, Android host tests and Android source
compilation, Android arm64 and x86_64 Rust FFI builds, the Compose Android
debug APK, and the Linux Compose Desktop distribution build pass. The Android
build uses the installed Rust 1.98.1 Android target libraries and Android NDK
clang for native cross-compilation; the Nix-packaged Rust compiler alone does
not include those target libraries. All 62 Flutter tests also pass. These
checks validate compilation and packaging; shared UI state-holder tests cover
indexed position jumps and loading pages after a jump. A manual Android API 36
device smoke test selected only `testdata/dummy-vault` through the system folder
picker, loaded the fixture gallery, rendered SAF-backed note details and an
image, verified indexed position navigation, opened a parseable but
gallery-ineligible linked note from the detail view, then force-stopped/restarted
the app and confirmed the private index was reused. A subsequent device test
saved an empty gallery-target prefix, rescanned to zero eligible notes, restored
defaults, and confirmed both fixture notes and image tile previews returned.
On 2026-10-07, a separate fictional SAF fixture verified Android H.264 playback
and exposed a note-tile bug: note summaries had no representative-media kind,
so video covers were sent to the image decoder. Rust summaries and the UniFFI
record now carry representative-media kind and availability; note tiles select
the correct decoder and show the same missing-media state as media tiles. The
same device fixture rendered its magenta video thumbnail.
The Android private-settings writer was corrected to validate the canonical
app-private path; the update and rescan succeeded on-device. The fixture hashes
matched the original fictional fixture before and after these tests. This is a
basic device smoke test, not full Android
acceptance or parity/performance acceptance. Windows x64 JVM-host FFI and
distributable packaging had passed CI for the migration revision at this
checkpoint; later migration changes were subsequently verified by the workflow
run linked above. The bounded
SAF staging integration tests verify cancellation preserves the committed index
and removes its temporary scan file. Settings transfer codec and state tests
cover round-trip, malformed input, bounds, and failed imports; desktop transfer
tests also verify vault-contained export is rejected before creating a file.
The embedded-video launch argument test checks the Linux player disables
mpv's user configuration, scripts, and automatic sidecar loading.

Complete and verify one migration phase before starting the next. During the
transition Flutter and KMP may coexist; do not remove or degrade a working
Flutter target as a shortcut.

## Application language

The existing Flutter UI supports Japanese and English. System language is the default:
Japanese is selected only when the system locale is Japanese; all other system
locales use English. Users can override this with System, Japanese, or English
in Appearance settings. The selection is persisted in private app data with
the other appearance preferences and never changes Vault contents. UI text is
localized at the presentation layer; Rust parser/index contracts and
stored user-provided note/category labels are not translated.

The current Flutter localization uses `gen-l10n`. The English and Japanese source messages
are `app/lib/l10n/app_en.arb` and `app/lib/l10n/app_ja.arb`; keep their message
keys and named placeholders in sync. Run `flutter gen-l10n` from `app/` after
editing either file. Generated Dart files in `app/lib/l10n/` must not be edited
by hand. Widgets use `context.l10n`; `AppL10n.current` is only for code that
cannot receive a `BuildContext`. Preserve these locale and fallback behaviors
in the Kotlin UI and keep the Flutter localization until parity is verified.

Phase 4 delivered a privately sideloaded APK; this does not imply
store-distribution readiness. Android Vault access targets an ordinary
user-selected folder, such as one under
Documents; no Syncthing-specific integration is needed. The current core scans
ordinary filesystem paths, while Android's Storage Access Framework grants
access to document URIs that are not necessarily usable as filesystem paths,
even when the selected folder is an ordinary Documents directory. Android
uses `ACTION_OPEN_DOCUMENT_TREE` with a persisted read-only grant; the native
platform layer enumerates the selected tree and reads bounded note/media
content through `ContentResolver` in batches capped by note count and payload
size, with at most four note reads concurrent per batch. This avoids one
platform-channel round trip per note while keeping memory use bounded. Rust
receives only normalized relative paths and bounded note bytes for
parsing/indexing. Vault files are not copied to app storage; only the
disposable index and thumbnails are stored there.
Media document IDs are resolved beneath the selected tree before opening, and
the SAF path applies document-count, depth, path, note-size, and aggregate-byte
limits. On Android, a successful scan is reused on later launches to avoid
re-reading the whole Vault at startup; the index and scan summary stay in
private app data. The validated relative-path-to-document-URI lookup is also
cached in a private SQLite database, so thumbnails do not need to re-enumerate
deep folder paths after process restart. SAF thumbnails are cached privately
with bounded count and size, and discarded on rescan. Android image reads are
decoded and downscaled natively to display resolution before crossing the
platform channel. On SAF-backed Vaults, video thumbnails use the bundled media
decoder through a validated read-only file descriptor before falling back to
the platform frame extractor. Only the resulting thumbnail is cached in app
data.
SAF note details are loaded on demand by resolving the indexed note's validated
relative path under the selected tree and reading at most 2 MiB through
`ContentResolver`; the bytes are passed directly to Rust for parsing and are
not cached or logged. Media summaries include their parent note path so this
also works when opening a note from the media grid.
Nearby thumbnails are prefetched while scrolling. Rescanning remains explicit,
so users can refresh after changing Vault contents. Keep this behavior
read-only and do not request broad all-files access.
The SAF resource contract is: at most 100,000 entries, depth 64, 32 MiB of
aggregate relative-path bytes, 2 MiB per note, 128 notes and 16 MiB per read
batch, at most 16 concurrent note reads, and 128 MiB of total note content
per scan. Kotlin, Dart, and Rust must enforce compatible bounds; contract tests
must prevent these limits from drifting.
Android performance checks use generated fictional notes/media in a dedicated
test location, never a user's Vault. The repeatable synthetic SAF benchmark
drives the Rust scan and private thumbnail cache on an Android device, with a
mock document-provider channel and a real Flutter image decoder and grid.
Treat it as a Rust/Flutter/device baseline rather than a `ContentResolver`
throughput measurement. Record only aggregate duration, peak memory, cache-hit
rate, and frame timings; do not log paths or note content.
The Linux CLI baseline is not a substitute for Android SAF or Flutter frame
measurements.
An explicit “forget selected Vault” action is separate from settings reset. It
requires confirmation and removes the selected Vault URI/path, its private
index and SQLite sidecars, scan summary, document-URI mapping, and thumbnail
cache, then releases its persisted Android read grant. It does not modify the
Vault or remove global appearance and note-structure settings. Report cleanup
or grant-release failures; never report success when either operation fails.
Opening a media item in another application shows the Android `ACTION_VIEW`
chooser and grants read-only access to the selected media document. On Linux,
the file-manager action uses `org.freedesktop.FileManager1.ShowItems` to open
the containing location and select the media item. Android file-manager
selection opens the selected media document with a read-only grant instead of
opening a folder picker.

Material 3 Expressive has official design guidance, including expanded tonal
color, typographic hierarchy, flexible shape, and more natural motion:
https://m3.material.io/blog/building-with-m3-expressive
and https://developer.android.com/design/ui/wear/guides/get-started/apply.
Material 3 Expressive is an opt-in extension, not an official requirement to
replace every Material 3 widget. This app nevertheless targets app-wide
Expressive treatment with `material_3_expressive` 1.1.5 and `material_ui`.
Use an M3E component wherever the pinned package has a semantically suitable
implementation; where it does not, retain the correct native/platform
component and apply the shared Expressive theme and motion rather than lose
behavior or accessibility. Keep navigation, state management, and business
logic unchanged during UI migration. Verify package APIs against the pinned
release's README, changelog, and examples. The package requires Flutter
3.47.0+ and Dart 3.13.0+.
Official component, color, and motion guidance:
https://m3.material.io/components/buttons/overview,
https://m3.material.io/components/button-groups/overview,
https://m3.material.io/components/segmented-buttons/overview, and
https://m3.material.io/styles/motion/overview/how-it-works.
Use Material You theme roles for button surfaces and foregrounds; selected and
unselected controls must remain distinguishable and legible in light, dark,
system-color, and pure-black themes. Choice groups use primary/on-primary for
the selected state and surface-container-highest/on-surface for the unselected
state, with no elevation or shadow:
https://m3.material.io/styles/color/roles.
Custom animations use the package's verified expressive motion tokens and
respect the operating system's reduced-motion preference. Passive Flutter
Card shells remain where their surface grouping and media hit-target behavior
are needed; ExpansionTile and ReorderableListView retain their nested editing
and drag-reorder semantics. System routes, platform pickers, and media overlays
remain framework-, platform-, or media-specific and are not replaced by
unrelated controls.
The supplied leaf artwork is the app icon on supported Android and Linux
targets; keep the Android adaptive foreground inside its 66dp mask-safe
region:
https://developer.android.com/develop/ui/views/launch/launcher-icons#design_adaptive_icons.
The gallery filter panel uses Expressive search bars for its note and
tag-option searches; preserve the distinction between submitted note queries
and live tag-option filtering. Follow the Material 3 Search guidelines:
https://m3.material.io/components/search/guidelines.
Button Groups use the simple frameless treatment; state fills still use
contrast-safe theme colors. The tag-filter panel shows the exact available
category and option counts from the model without replacing them with aggregate
totals, omitting zeroes, or capping large values.
Main settings navigation cards use the higher surface-container role to
separate tappable actions from the page background. Small app-bar titles use a
bold headline style while remaining in the app bar's title position.
Non-search inputs use the outlined Expressive text-field variant; framework
text fields use the shared transparent outlined input theme. Search bars retain
their dedicated search treatment.

The settings page groups brightness, system accent color, and pure-black
controls in one appearance card, using a connected Material 3 Expressive button
group for the mutually exclusive brightness choice and switches for the
independent preferences. Use connected button groups for mutually exclusive
choices in the gallery sort controls and note-link resolution settings as well.
Pure black applies only to dark mode: the app background remains true black
while item surfaces retain a subtle dark distinction; the selected
system/dynamic scheme continues to supply accent and foreground roles. Video
controls remain transparent over media; the seek
indicator uses the active theme's primary color while text and transport
controls remain high-contrast white.
Gallery tile media/memo/related counts and video indicators have no outlines;
video is identified by an icon with accessible semantics. Note detail
tags use a tag-seeded Material You tonal surface with no tag icon; frontmatter
dates share one surface with their labels, while the selectable note path has no
separate background. Missing/loading media icons are an optional tile setting
and are hidden by default. The optional tile position indicator is a 24dp
tonal marker at the upper left, matching the count-icon background size. The
note title and author occupy the app bar headline and supporting subtitle roles
respectively. The display-mode popup uses the standard Material You menu
surface and selected-state color roles.
Settings are ordered as appearance, list pagination, storage, notes, then
“このアプリについて”. Vault selection and scan status are grouped under
“保管庫”. Note structure and tag-filter visibility remain under “ノート”;
list pagination is a separate top-level setting. Import, export, reset, help,
app information, licenses, and the GitHub repository link are grouped under
“このアプリについて”. Reset writes explicit defaults to settings files; it
does not remove the files or affect Vault notes and the index. Gallery tag
eligibility has its own section heading in tag settings. Pull-to-refresh is
disabled; rescanning remains an explicit toolbar/settings action.
The tag-filter panel also controls gallery ordering by publication or creation
date, ascending or descending. This order applies consistently to note and
media grids and across pagination; notes without the selected date stay last,
and equal dates are ordered by Vault-relative path. The default is newest
creation date first.

## Note parsing and gallery selection

Notes use YAML frontmatter containing a `tags` list and optional `url`,
`published`, `created`, `updated`, and `cover`. UTF-8 BOM and LF/CRLF are
accepted. Dates may remain original strings when not understood. Unknown keys
are ignored. A note is eligible when at least one original tag matches a
configured gallery-target prefix; the default is `source/art`. Prefixes can be changed or cleared in note-structure settings.
Other parseable notes with valid tags are retained in the private index only as
internal-link targets; they are excluded from gallery results, tag counts, and
pagination totals.
Read/parse failures are reported as aggregate scan warnings because their tags
cannot be checked to decide whether they belong in the gallery. Display
exclusions never remove eligible notes from the index. The Flutter gallery requests validated video paths from the core. For
SAF-backed folders, it tries the platform thumbnail API first and falls back to
the bundled media decoder through a validated file descriptor; filesystem
galleries use the platform video-thumbnail backend. Linux development requires
FFmpeg libraries.
The settings screen explains that aggregate warning count, typical read/parse/
limit causes, and that these entries are neither confirmed gallery exclusions
nor added to the gallery. It does not reveal note names or contents.

The actual sanitized note examples provided by the user use Markdown image
embeds, including `![](<relative/path.webp>)` and `.mp4`, and repeat the
frontmatter `cover` in the body. Relative Markdown links and Obsidian wikilinks
in `関連` are resolved to parseable Markdown notes inside the Vault. Image
embeds (`![[note]]`, `![](note.md)`) in related sections are navigable when
their destinations resolve to Markdown notes.
The media order is `cover` followed by Markdown body embeds in source order,
without duplicates. Support image and video extensions, URL-decoded paths,
and missing media. Media paths are relative to
the note and must resolve inside the Vault. Body sections named `関連` and
`覚書` (default aliases also include `Related`, `Memo`, `Notes`, `Document`; the
UI language is Japanese or English, English unless the system language is
Japanese, and is user-selectable) are extracted by heading name at any heading level. Their content is
not restricted to list items: plain text, quoted text, and code-block contents
are retained. Related links can open any parseable indexed note inside the
Vault, including notes not eligible for the gallery. The UI displays extracted
fields and tokens; it does not render
Markdown.

Tag categories use the final tag component as the option and all earlier
components as its category. Parent paths are indexed for matching but are not
independent options when descendants exist. The default UI hides tag prefixes
`moc`, `add`, `pin`, and `source/art`; these are user-editable display rules,
not exclusions from indexing. User tag settings independently define prefixes
available in filter controls, hidden in filter controls and note details, and
tag-color prefixes. These settings are stored in private app data, never in the
Vault. Filter-prefix settings limit the UI's available tag controls; note tags
are still indexed so the gallery can query them and display their metadata.
Tag settings and appearance settings can be exported together as JSON.
Flutter's private `tag-settings.json` contains configurable gallery-target tag
prefixes, Markdown heading aliases for memo, related items, and the end of post
text, and Frontmatter key aliases for tags, title, URL, dates, and cover media.
The KMP settings editor stores its target-prefix override separately in private
`gallery-tag-settings.json`, so it does not discard the remaining parser and
category settings. Defaults retain the current behavior. Changing parsing
settings invalidates scan state and reparses notes so virtual filters and details
agree. Markdown and Wikilink resolution can be set to shortest-path,
note-relative, or Vault-root-relative;
links are only resolved against parseable indexed notes inside the Vault.
Obsidian basename wikilinks also resolve by nearest matching filename, then
exact note title. Filename and eligible-note indexes keep link resolution and
tag filtering bounded without exposing link-only notes in the gallery. The settings
screen groups Vault, scan, note, and data settings; tag display rules are a
separate second-level page under the note category. The note-structure screen
includes an embedded fictional example note that can be viewed as selectable
Markdown and copied, but is never written to the Vault. Settings can be exported
to or imported from versioned JSON, or reset to defaults.
In the note viewer, frontmatter remains fixed first; author, media, post text,
the post-text end marker, related, and memo blocks follow the configured order.
The default order places the post-text end marker after post text, followed by
related and then memo. Media participates in the reorderable list.
Media thumbnails are not repeated in the details panel; the main viewer remains
the only media display. Blocks can be reordered and hidden independently.
Frontmatter keys, memo and related headings/rules, and post-text end headings
open their own settings screens from the block-order list. Each block has an
explanation popup; blocks without independent settings do not show a settings
action. Memo and related use configurable heading names without level or
list-style restrictions. The
fictional Markdown example reflects current block order and tag settings.
List page size and the loaded-item range are configurable; the range is enabled
by default. One-based gallery item numbers on note and media tiles are separately
configurable and disabled by default; the tile label is numeric only. Grouped
note tiles also show a separate media-count badge when a note contains multiple
media items. Note memo and related counts remain visible in both grouped and
all-media tile modes. The gallery shows the indexed total and
currently loaded range, with a direct start-position jump beside the filter
control; display mode is triggered by an icon without a filled button surface.
Counts, ranges, and jump limits reflect active filters and search. A
jump loads the target page and its immediately preceding page (when available),
then scrolls to the target so earlier items remain reachable; normal
scrolling loads subsequent pages and scrolling above the target loads preceding
pages while preserving the visible position before the data update to avoid
rebuilding visible media placeholders during prepends. Progress appears in a cancellable
dialog that closes on success and remains open on failure; the target is
briefly highlighted. The count range follows the visible configured page, not
the number of items prefetched into memory. Thumbnail loading never disables
gallery scrolling. Pull-to-refresh is disabled; use the explicit rescan action.
General usage guidance is grouped under a separate Help screen rather than shown
as a settings-only explanation.
Within a category ordinary included tags are OR; between categories conditions
are AND. A distinct all-tags mode requires every selected tag to match, while
excluded tags remove notes matching any of them. The tag search only filters
visible tag options; the separate note search filters note paths and titles and
accepts `#tag` for included tags, `-#tag` for excluded tags, and `&#tag` for
tags that must all match. Unprefixed note-search terms also match tag names,
in addition to note titles and paths. These query tags affect gallery results
only; they never filter the tag options shown in the filter panel. Both search
fields support bounded typo-tolerant matching, and a non-empty tag search
expands the matching categories while preserving their prior expanded/collapsed
state.
Filter categories are user-configurable rules (`tagCategories` in the private
tag settings): a name plus an exact path (`source/art`) or a subtree path
(`source/count/*`). The most specific matching rule wins, rules sharing a name
form one category, and the defaults are ソース, 人数, アートスタイル, 性別, メタ,
レーティング, タイプ and 作品 (`copyright/*`). Each wildcard rule can optionally
split tags three or more levels below its root into per-parent groups (off by
default). An optional その他 category collects tags matching no rule and is
omitted when empty; it has the same split option. Categories without visible
options are not shown. Selecting a category-root option matches notes with any
tag in that category. The content category also provides virtual filters for
multiple media, video, memo, and related links; these filter options are
separate from persisted tag-category rules.

## Storage, safety, and failure handling

The SQLite index and later thumbnail/config caches are disposable application
data outside the Vault and outside shared media locations. The index is
recreated/reconciled on scanning; changed notes are reparsed and removed notes
are deleted. Missing media remains indexed and is checked again on later
scans. A bad note is a warning, not a reason to abort the whole scan.
Appearance preferences are stored as a small JSON file in the same private
application-support directory; they never modify the Vault.

Do not follow symlinks while walking. Canonicalize media paths and reject
anything outside the canonical Vault root. Reject an index path inside the
Vault and hard-linked database files. Unix index data is owner-only. Do not
include note paths, names, contents, tags, or URLs in logs or errors.

Parser limits: note size 2 MiB, frontmatter 256 KiB, 256 tags, and 64 levels of
YAML nesting. The chosen Serde-compatible YAML parser does not expose an alias
expansion budget; Phase 1 rejects YAML alias tokens rather than risking
expansion exhaustion. This conservative limitation must be documented if
real notes use aliases.

Core crates declare `forbid(unsafe_code)`. Network dependencies are disallowed.
SQLite is explicitly bundled through `rusqlite`/`libsqlite3-sys`, which brings
native C code; this is documented as a conscious dependency trade-off.

## Phase 1 interface

`gallery-cli` offers:

- `scan <vault> [--database <database>]`: index/reconcile eligible Markdown notes and print
  aggregate indexed/warning counts.
- `categories <vault> [--database <database>]`: display non-excluded category options and
  counts.
- `list <vault> [--database <database>] [tag ...]`: list notes matching tag conditions.

The gallery opens a detail viewer from either grid mode. It displays images
with zoom, plays local videos in-app, and shows extracted note metadata, tags,
`覚書`, and `関連` text without rendering Markdown. Media source paths are
returned only for indexed media that still resolves inside the canonical
Vault. The Obsidian `obsidian://open` URI uses the absolute note path for
filesystem Vaults and the selected Vault name plus relative note path for
Android SAF Vaults, which do not expose a stable filesystem path. URI launching
occurs only after an explicit user action. HTTP(S) source links are also
explicit user actions;
relative Markdown note links and Obsidian wikilinks in `関連` resolve to
parseable paths inside the Vault and open in a new in-app viewer. X/Twitter post
URLs produce an explicit profile link for the author. The viewer can choose an
application to open the current media or reveal the selected item in the file
manager, also only after an explicit action.
When a note filename follows `<username>-on-X-<excerpt>`, the viewer title
uses the excerpt. A leading standalone author link is extracted and excluded
from the post text before the `文書`/`関連`/`覚書` section;
unordered/ordered list markers and two-space indentation levels are retained
for memo and related-list presentation without drawing bullet glyphs. The viewer
shows the author above the media by default; the configured block order can
place it in the detail area instead. Post text appears in the detail area
without a post-text label or duplicated author. It fills the app window on
entry, with black background and centered, fit-contained media; details are
initially hidden so media fills the app window. While an image's note details,
source path, or display-sized decode is loading, keep the viewer black rather
than showing the smaller gallery thumbnail. A tap reveals a Material 3
colored details interface with an animated media and details transition.
Opening details resets image zoom. Post text and memo cards use elevated
Material 3 surface containers so they contrast with their parent panel. A post
body containing only empty blockquote markers is not displayed. The appearance
settings independently select system/light/dark brightness, use of the OS
Material You system color, and an optional pure-black dark surface style.
Windows uses the system accent color through the Windows `dynamic_color`
plugin. On Linux, dynamic color prefers the standard XDG Desktop Portal
`org.freedesktop.appearance/accent-color` setting and reapplies the theme when
the portal emits `SettingChanged`; portal-backed desktops such as KDE Plasma
are supported. GTK system accent colors are the fallback; if neither source
is available, the app palette is used. Media
swipes, horizontal trackpad scrolling, and arrow keys
change media with overlays shown or hidden. Pinch and zoom/pan do not toggle
overlays. An explicit full-screen action toggles native display/window
full-screen mode for image and video and hides the details and control overlays.
Tapping the media or pressing Escape returns to details and exits full-screen
mode. The page counter is shown in the media
area when not full-screen. Viewer and filter tags use consistent category
colors and selection remains attached to stable tag identities.
Dates show a space instead of the ISO `T` separator.
On Android, the media-only viewer automatically hides system bars and restores
them when note details are shown. Media controls provide play/pause, seeking,
rate, single-item looping, and mute with fully transparent controls. The
existing display-mode `PopupMenuButton`
uses Flutter's Material 3 menu styling with a selected-item checkmark.
Video playback uses `media_kit`/mpv on Linux; the development environment
provides mpv and libass for its native plugin build. Android playback uses
Flutter's `video_player` plugin and its native Android backend. Keep the
platform-specific playback implementations separate rather than treating the
Linux mpv backend as the Android implementation.

The tag-filter panel has separate tag-option and note-name/path search fields.
A tag chip cycles through OR include, AND include, exclude, and inactive.
Gallery tiles show an icon-free one-based position in its own badge when
enabled. Grouped note tiles show separate memo and related-item counts when
present. Choices with no matches under active filters remain
visible but disabled. Expanded filter sections retain their state while the
category data refreshes. Gallery and category results remain visible while
filter-triggered reloads are pending, avoiding a loading-indicator flash.
On Linux, the folder chooser remains a native GTK dialog
and therefore follows the desktop's GTK/system theme, independently of Flutter
widget themes.

The default database is under `$XDG_STATE_HOME/vault-gallery` or
`$HOME/.local/state/vault-gallery`, never in the Vault. `tools/` generates
fictional notes/media and measures initial and unchanged scan time.

## Deferred decisions and confirmation points

Before implementing parser behavior against real user material, inspect
sanitized examples of image and video-cover notes, including frontmatter,
headings, and embeds. Confirm the actual paths for provisional `source/meta`,
`source/format`, and `source/type` labels, and whether category-wide selection
means “has any tag in this category.” Before Android, confirm personal
sideloading versus store distribution. Obsidian URI path opening follows the
official `obsidian://open?path=...` form. Platform and dependency details are
verified against current official documentation when adopted.

Performance targets are measured, not assumed: current Vault size 7,806 notes,
growth case at least 20,000, a few seconds for an unchanged scan, responsive
filtering, and smooth lazy grid scrolling.
