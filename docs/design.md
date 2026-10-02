# vault-gallery design

This document records the implementation contract for the repository. The
user-provided Japanese design in the project brief is the source of truth; this
summary preserves the constraints needed to implement the current phase. If
implementation and brief conflict, report the discrepancy and follow the
brief.

## Purpose and scope

Provide an offline gallery for media and metadata in an Obsidian Vault,
initially targeting NixOS/Linux and Android. Rust owns parsing, scanning,
indexing, filtering, and image thumbnails. Flutter will own the UI and video
decoding/playback. The Vault is always read-only.

The application must not edit, create, delete, or move Vault files; render
Markdown; synchronize data; automatically access the network; send telemetry,
ads, or crash reports; or generate/tag images with AI. Only fictional test
notes and media may be committed.

## Architecture and phase order

```text
Flutter features/platform -> gallery-bridge -> gallery-core -> gallery-parse
```

Dependencies point downwards only. `gallery-parse` is pure parsing without
filesystem I/O. `gallery-core` does not know Flutter or the bridge. Flutter
features do not import each other; the UI calls Rust through a repository
interface and keeps OS-specific behavior in `platform/`.

1. Rust parser, core, CLI, fictional fixture generator, and initial benchmark.
2. Flutter/Linux gallery list with native video-frame thumbnails.
3. Viewer, details, Obsidian links, and video.
4. Android.
5. Windows/macOS.
6. iOS after agreeing on external Vault access.

Complete and verify one phase before starting another.

Phase 4 initially targets a privately sideloaded APK; this does not imply
store-distribution readiness. Android Vault access is an open implementation
boundary: the target is an ordinary user-selected folder, such as one under
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
Nearby thumbnails are prefetched while scrolling. Rescanning remains explicit,
so users can refresh after changing Vault contents. Keep this behavior
read-only and do not request broad all-files access.
Opening a media item's containing folder offers the read-only folder document to
the system `ACTION_VIEW` resolver so the user can select an installed file
manager. Some file managers may not support directory documents; report that
failure rather than opening a folder picker. Opening an individual media file
may also use a read-only `ACTION_VIEW` grant.

Material 3 Expressive has official design guidance, including expanded tonal
color, typographic hierarchy, flexible shape, and more natural motion:
https://m3.material.io/blog/building-with-m3-expressive
and https://developer.android.com/design/ui/wear/guides/get-started/apply.
Flutter's built-in Material library provides stable Material 3 components, but
not the full Material 3 Expressive component set or its complete token system.
The app therefore applies those design principles through a shared Flutter
Material 3 theme (including component shapes, touch targets, surfaces, and
motion tokens) and native Material components; it must not claim pixel-level or
component-level conformance to the full Expressive system. Do not add a
community component package to replace Flutter's Material library.

The settings page groups brightness, system accent color, and pure-black
controls in one appearance card, using a segmented control for the mutually
exclusive brightness choice and switches for the independent preferences.
Pure black applies only to dark mode: the app background remains true black
while item surfaces retain a subtle dark distinction; the selected
system/dynamic scheme continues to supply accent and foreground roles. Video
controls remain transparent over media; the seek
indicator uses the active theme's primary color while text and transport
controls remain high-contrast white.
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
configured gallery-target prefix; the default is `source/`, preserving current
behavior. Prefixes can be changed or cleared in note-structure settings.
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
`覚書` are extracted by heading name at any heading level. Their content is
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
The same tag-settings JSON contains configurable gallery-target tag prefixes,
Markdown heading aliases for memo, related items, and the end of post text, and
Frontmatter key aliases for tags, title, URL, dates, and cover media. Defaults
retain the current behavior. Changing parsing settings invalidates scan state
and reparses notes so virtual filters and details agree. Markdown and Wikilink
resolution can be set to shortest-path, note-relative, or Vault-root-relative;
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
gallery scrolling. Pulling down at the top of the gallery starts a rescan.
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
Initial category labels are configurable, and missing labels fall back to the
tag path. The Flutter filter panel presents all `source/...` categories under
one collapsible “ソース” heading; the original subcategories remain separate
filter groups, preserving their OR/AND matching behavior. The supplied
examples confirm `source/type` and `source/format` as
category paths; they do not establish `source/meta`. Category-wide options and
virtual-content filters are planned for the UI/API phase. The interpretation
of category-wide selection remains the design document's stated assumption:
“has any tag in this category.”

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
Vault. The Obsidian `obsidian://open` URI uses the absolute note path to avoid
ambiguity when vaults have duplicate names; URI launching occurs only after an
explicit user action. HTTP(S) source links are also explicit user actions;
relative Markdown note links and Obsidian wikilinks in `関連` resolve to
parseable paths inside the Vault and open in a new in-app viewer. X/Twitter post
URLs produce an explicit profile link for the author. The viewer can open the
current media in its system-default application or open its containing
directory in the file manager, also only after an explicit action.
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
On Linux, dynamic color prefers the standard XDG Desktop Portal
`org.freedesktop.appearance/accent-color` setting, supporting portal-backed
desktops such as KDE Plasma. GTK system accent colors are the fallback; if
neither source is available, the app palette is used. Media
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
provides mpv and libass for its native plugin build. Android ExoPlayer belongs
to the later Android phase; ExoPlayer is an Android Media3 backend and cannot
replace the Linux mpv backend.

The tag-filter panel has separate tag-option and note-name/path search fields.
A tag chip cycles through OR include, AND include, exclude, and inactive.
Gallery tiles show an icon-free one-based position in its own badge when
enabled. Grouped note tiles show separate memo and related-item counts when
present. Within the single source heading, display
sections stop at the second path component (for example `source/test`); deeper
tag paths remain visibly nested beneath that section without changing their
filter matching semantics. Choices with no matches under active filters remain
visible but disabled. Expanded filter sections retain their state while the
category data refreshes. Gallery and category results remain visible while
filter-triggered reloads are pending, avoiding a loading-indicator flash.
The folder chooser remains a native GTK dialog
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
