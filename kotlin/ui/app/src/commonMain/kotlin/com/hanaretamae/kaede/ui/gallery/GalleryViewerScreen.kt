package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorRule
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings

data class GalleryViewerStrings(
    val close: String,
    val previous: String,
    val next: String,
    val loading: String,
    val noMedia: String,
    val author: String,
    val link: String,
    val memo: String,
    val related: String,
    val postTextEnd: String,
    val fullscreen: String,
    val exitFullscreen: String,
    val openMedia: String,
    val revealMedia: String,
    val setWallpaper: String,
    val published: String,
    val created: String,
    val updated: String,
    val tags: String,
    val openNote: String,
    val openNoteInObsidian: String,
    val openAuthorProfile: String,
    val openOriginalPage: String,
    val copyPageUrl: String,
    val copiedPageUrl: String,
    val error: (RepositoryError) -> String,
)

val EnglishGalleryViewerStrings = GalleryViewerStrings(
    close = "Close",
    previous = "Previous",
    next = "Next",
    loading = "Loading note…",
    noMedia = "This note has no media",
    author = "Author",
    link = "Open link",
    memo = "Memo",
    related = "Related links",
    postTextEnd = "Post text end",
    fullscreen = "Full screen",
    exitFullscreen = "Exit full screen",
    openMedia = "Open media",
    revealMedia = "Show media in file manager",
    setWallpaper = "Set as wallpaper",
    published = "Published",
    created = "Created",
    updated = "Updated",
    tags = "Tags",
    openNote = "Open note",
    openNoteInObsidian = "Open in Obsidian",
    openAuthorProfile = "Open author profile",
    openOriginalPage = "Open original page",
    copyPageUrl = "Copy page URL",
    copiedPageUrl = "URL copied",
    error = ::englishError,
)

val JapaneseGalleryViewerStrings = GalleryViewerStrings(
    close = "閉じる",
    previous = "前へ",
    next = "次へ",
    loading = "ノートを読み込み中…",
    noMedia = "このノートにメディアはありません",
    author = "作者",
    link = "リンクを開く",
    memo = "メモ",
    related = "関連リンク",
    postTextEnd = "本文終端",
    fullscreen = "全画面表示",
    exitFullscreen = "全画面表示を終了",
    openMedia = "メディアを開く",
    revealMedia = "ファイルマネージャーで表示",
    setWallpaper = "画像を壁紙にする",
    published = "公開日",
    created = "作成日",
    updated = "更新日",
    tags = "タグ",
    openNote = "ノートを開く",
    openNoteInObsidian = "Obsidianで開く",
    openAuthorProfile = "作者のプロフィールを開く",
    openOriginalPage = "元のページを開く",
    copyPageUrl = "ページURLをコピー",
    copiedPageUrl = "URLをコピーしました",
    error = ::japaneseError,
)

/**
 * The platform renderer owns media decoding and receives the resolved media
 * state; the common viewer never opens a path or launches an external URL.
 */
@Composable
fun GalleryViewerScreen(
    stateHolder: GalleryViewerStateHolder,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strings: GalleryViewerStrings = EnglishGalleryViewerStrings,
    onFullscreenChanged: ((Boolean) -> Unit)? = null,
    onMediaOnlyChanged: ((Boolean) -> Unit)? = null,
    onOpenMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onRevealMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onSetWallpaperMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onOpenVaultNote: ((String) -> Unit)? = null,
    mediaContent: @Composable (GalleryViewerMediaState) -> Unit,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit = {},
    tagColorRules: List<GalleryTagColorRule> = GalleryTagColorCodec.DEFAULT_RULES,
    noteStructure: GalleryNoteStructureSettings = GalleryNoteStructureSettings(),
) {
    val state by stateHolder.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val horizontalOffset = remember(stateHolder) { mutableFloatStateOf(0f) }
    val detailsScrollState = rememberScrollState()

    val density = LocalDensity.current
    val focusRequester = remember(stateHolder) { FocusRequester() }
    val presentation = remember(stateHolder) {
        mutableStateOf(GalleryViewerPresentationState())
    }
    val detailsForwardingScrollState = rememberScrollableState { delta ->
        if (presentation.value.detailsVisible && !presentation.value.fullscreen) {
            detailsScrollState.dispatchRawDelta(delta)
        } else {
            0f
        }
    }
    val mediaScale = remember(stateHolder) { mutableFloatStateOf(1f) }
    val mediaOffset = remember(stateHolder) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { centroid, zoom, pan, _ ->
        val oldScale = mediaScale.floatValue
        val newScale = nextImageScale(oldScale, zoom)
        val appliedZoom = newScale / oldScale
        mediaScale.floatValue = newScale
        mediaOffset.value = if (newScale == 1f) {
            Offset.Zero
        } else {
            (mediaOffset.value + centroid) * appliedZoom - centroid + pan
        }
    }
    LaunchedEffect(state.selectedMedia?.id) {
        mediaScale.floatValue = 1f
        mediaOffset.value = Offset.Zero
    }
    LaunchedEffect(stateHolder) {
        stateHolder.start()
        focusRequester.requestFocus()
    }
    DisposableEffect(stateHolder) {
        onMediaOnlyChanged?.invoke(true)
        onDispose { onMediaOnlyChanged?.invoke(false) }
    }
    DisposableEffect(stateHolder) {
        onDispose(stateHolder::dispose)
    }
    DisposableEffect(onFullscreenChanged) {
        onDispose { onFullscreenChanged?.invoke(false) }
    }

    Surface(
        modifier = modifier.fillMaxSize().focusRequester(focusRequester)
            .focusable().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            stateHolder.showPreviousMedia()
                            true
                        }
                        Key.DirectionRight -> {
                            stateHolder.showNextMedia()
                            true
                        }
                        Key.Escape -> {
                            if (presentation.value.fullscreen) {
                                presentation.value = presentation.value.exitFullscreen()
                                onFullscreenChanged?.invoke(false)
                                onMediaOnlyChanged?.invoke(false)
                                mediaScale.floatValue = 1f
                                mediaOffset.value = Offset.Zero
                                true
                            } else if (presentation.value.detailsVisible) {
                                presentation.value = presentation.value.toggleMedia()
                                onMediaOnlyChanged?.invoke(true)
                                mediaScale.floatValue = 1f
                                mediaOffset.value = Offset.Zero
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
            },
        color = Color.Black,
        contentColor = Color.White,
    ) {
        fun toggleDetails() {
            val previous = presentation.value
            presentation.value = previous.toggleMedia()
            if (previous.fullscreen) {
                onFullscreenChanged?.invoke(false)
            }
            if (presentation.value.detailsVisible) {
                onMediaOnlyChanged?.invoke(false)
            } else {
                onMediaOnlyChanged?.invoke(true)
            }
            mediaScale.floatValue = 1f
            mediaOffset.value = Offset.Zero
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (presentation.value.detailsVisible && !presentation.value.fullscreen) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text(strings.close) }
                    Spacer(Modifier.width(8.dp))
                    val note = state.note
                    val noteTitle = note?.let { galleryViewerTitle(it.path, it.title) }
                    if (noteTitle != null) {
                        Text(noteTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    val authorProfileUrl = note?.let {
                        galleryViewerAuthorProfileUrl(it.authorUrl, it.url)
                    }
                    val noteAuthor = note?.let {
                        galleryViewerAuthor(it.path, it.author, authorProfileUrl)
                    }
                    if (
                        noteAuthor != null &&
                        galleryViewerShowsAuthorAboveMedia(noteStructure)
                    ) {
                        Text(noteAuthor, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (authorProfileUrl != null) {
                        TextButton(onClick = { onExternalLink(authorProfileUrl) }) {
                            Text(strings.openAuthorProfile)
                        }
                    }
                    val originalUrl = note?.url?.takeIf(::isGalleryViewerWebUrl)
                    if (originalUrl != null) {
                        TextButton(onClick = { onExternalLink(originalUrl) }) {
                            Text(strings.openOriginalPage)
                        }
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(originalUrl))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(strings.copiedPageUrl)
                                }
                            },
                        ) {
                            Text(strings.copyPageUrl)
                        }
                    }
                    if (note != null && onOpenVaultNote != null) {
                        TextButton(onClick = { onOpenVaultNote(note.path) }) {
                            Text(strings.openNoteInObsidian)
                        }
                    }
                    if (state.selectedMedia != null) {
                        Text("${state.selectedMediaIndex + 1} / ${state.media.size}")
                        val activeMedia = state.selectedMedia
                        val mediaState = activeMedia?.let {
                            GalleryViewerMediaState(
                                media = it,
                                location = state.mediaLocation,
                                locationLoading = state.mediaLocationLoading,
                            )
                        }
                        if (mediaState?.location != null) {
                            onOpenMedia?.let { open ->
                                TextButton(onClick = { open(mediaState) }) {
                                    Text(strings.openMedia)
                                }
                            }
                            onRevealMedia?.let { reveal ->
                                TextButton(onClick = { reveal(mediaState) }) {
                                    Text(strings.revealMedia)
                                }
                            }
                            onSetWallpaperMedia?.takeIf {
                                galleryCanSetWallpaper(mediaState.location, mediaState.media.isVideo)
                            }?.let { setWallpaper ->
                                TextButton(onClick = { setWallpaper(mediaState) }) {
                                    Text(strings.setWallpaper)
                                }
                            }
                        }
                        TextButton(
                            onClick = stateHolder::showPreviousMedia,
                            enabled = state.selectedMediaIndex > 0,
                        ) { Text(strings.previous) }
                        TextButton(
                            onClick = stateHolder::showNextMedia,
                            enabled = state.selectedMediaIndex < state.media.lastIndex,
                        ) { Text(strings.next) }
                    }
                    if (onFullscreenChanged != null) {
                        TextButton(
                            onClick = {
                                presentation.value = presentation.value.enterFullscreen()
                                mediaScale.floatValue = 1f
                                mediaOffset.value = Offset.Zero
                                onFullscreenChanged(true)
                            },
                        ) {
                            Text(strings.fullscreen)
                        }
                    }
                }
            }

            val media = state.selectedMedia
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(strings.loading, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (media == null && state.error == null) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val detailsExpandDistancePx = with(density) { 220.dp.toPx() }
                    val detailsPanelHeight = maxHeight * galleryViewerDetailsPanelFraction(
                        scrollOffsetPx = detailsScrollState.value,
                        expandDistancePx = detailsExpandDistancePx,
                    )
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .scrollable(
                                orientation = Orientation.Vertical,
                                state = detailsForwardingScrollState,
                            ),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .clickable(onClick = ::toggleDetails),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(strings.noMedia, style = MaterialTheme.typography.titleMedium)
                        }
                        state.note?.takeIf {
                            presentation.value.detailsVisible && !presentation.value.fullscreen
                        }?.let { note ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .height(detailsPanelHeight)
                                    .verticalScroll(detailsScrollState)
                                    .background(Color(0xFF151515))
                                    .padding(16.dp),
                            ) {
                                NoteDetails(
                                    note,
                                    strings,
                                    onExternalLink,
                                    onOpenNote,
                                    tagColorRules,
                                    noteStructure,
                                )
                            }
                        }
                    }
                }
            } else if (media != null) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val detailsExpandDistancePx = with(density) { 220.dp.toPx() }
                    val detailsPanelHeight = maxHeight * galleryViewerDetailsPanelFraction(
                        scrollOffsetPx = detailsScrollState.value,
                        expandDistancePx = detailsExpandDistancePx,
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .scrollable(
                                    orientation = Orientation.Vertical,
                                    state = detailsForwardingScrollState,
                                )
                                .scrollable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberScrollableState { delta ->
                                        horizontalOffset.floatValue += delta
                                        val threshold = with(density) { 72.dp.toPx() }
                                        when {
                                            horizontalOffset.floatValue <= -threshold -> {
                                                stateHolder.showNextMedia()
                                                horizontalOffset.floatValue = 0f
                                            }
                                            horizontalOffset.floatValue >= threshold -> {
                                                stateHolder.showPreviousMedia()
                                                horizontalOffset.floatValue = 0f
                                            }
                                        }
                                        delta
                                    },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .pointerInput(
                                        presentation.value.detailsVisible,
                                        presentation.value.fullscreen,
                                        media.isVideo,
                                        mediaScale.floatValue,
                                    ) {
                                        val canForwardScroll = galleryViewerCanForwardMediaVerticalScroll(
                                            detailsVisible = presentation.value.detailsVisible,
                                            fullscreen = presentation.value.fullscreen,
                                            isVideo = media.isVideo,
                                            imageZoomed = mediaScale.floatValue != 1f,
                                        )
                                        if (canForwardScroll) {
                                            val touchSlop = viewConfiguration.touchSlop
                                            awaitEachGesture {
                                                val down = awaitFirstDown(
                                                    requireUnconsumed = false,
                                                    pass = PointerEventPass.Initial,
                                                )
                                                var previous = down.position
                                                var total = Offset.Zero
                                                var draggingVertically = false
                                                var pointerPressed = true
                                                while (pointerPressed) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    val change = event.changes.firstOrNull {
                                                        it.id == down.id
                                                    } ?: break
                                                    val delta = change.position - previous
                                                    previous = change.position
                                                    if (!draggingVertically) {
                                                        total += delta
                                                        if (
                                                            abs(total.y) > touchSlop &&
                                                            abs(total.y) > abs(total.x)
                                                        ) {
                                                            draggingVertically = true
                                                        }
                                                    }
                                                    if (draggingVertically) {
                                                        detailsScrollState.dispatchRawDelta(-delta.y)
                                                        change.consume()
                                                    }
                                                    pointerPressed = change.pressed
                                                }
                                            }
                                        }
                                    }
                                    .clickable(onClick = ::toggleDetails)
                                    .transformable(state = transformState, enabled = !media.isVideo)
                                    .graphicsLayer {
                                        scaleX = mediaScale.floatValue
                                        scaleY = mediaScale.floatValue
                                        translationX = mediaOffset.value.x
                                        translationY = mediaOffset.value.y
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                mediaContent(
                                    GalleryViewerMediaState(
                                        media = media,
                                        location = state.mediaLocation,
                                        locationLoading = state.mediaLocationLoading,
                                    ),
                                )
                            }
                        }
                        state.note?.takeIf {
                            presentation.value.detailsVisible && !presentation.value.fullscreen
                        }?.let { note ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .height(detailsPanelHeight)
                                    .verticalScroll(detailsScrollState)
                                    .background(Color(0xFF151515))
                                    .padding(16.dp),
                            ) {
                                NoteDetails(
                                    note,
                                    strings,
                                    onExternalLink,
                                    onOpenNote,
                                    tagColorRules,
                                    noteStructure,
                                )
                            }
                        }
                    }
                }
            }
            state.error?.let {
                Text(
                    strings.error(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

internal fun galleryCanSetWallpaper(location: String?, isVideo: Boolean): Boolean {
    if (isVideo || location == null) return false
    val normalized = location.lowercase().substringBefore('?').substringBefore('#')
    return listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif")
        .any(normalized::endsWith)
}

internal fun nextImageScale(current: Float, zoom: Float): Float =
    (current * zoom).coerceIn(1f, 5f)

internal fun isGalleryViewerWebUrl(value: String): Boolean {
    val separator = value.indexOf("://")
    if (separator <= 0) return false
    val scheme = value.substring(0, separator).lowercase()
    if (scheme != "http" && scheme != "https") return false
    val authority = value.substring(separator + 3).substringBeforeAny('/', '?', '#')
    if (authority.isEmpty() || '@' in authority) return false
    val host = authority.substringBefore(':').lowercase()
    val port = authority.substringAfter(':', "")
    val portNumber = port.toIntOrNull()
    if (':' in authority &&
        (port.isEmpty() || port.any { !it.isDigit() } || portNumber == null || portNumber !in 1..65535)
    ) {
        return false
    }
    if (host.isEmpty() || !host.split('.').all { label ->
            label.isNotEmpty() &&
                label.first().isLetterOrDigit() &&
                label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    ) {
        return false
    }
    return value.none(Char::isWhitespace)
}

internal fun galleryViewerTitle(notePath: String, fallbackTitle: String): String {
    val basename = notePath.substringAfterLast('/')
    val filename = basename.substringBeforeLast('.', basename)
    val separator = filename.indexOf("-on-X-")
    return if (separator >= 0 && separator + 6 < filename.length) {
        filename.substring(separator + 6)
    } else {
        fallbackTitle
    }
}

internal fun galleryViewerAuthorProfileUrl(
    authorUrl: String?,
    postUrl: String?,
): String? {
    authorUrl?.takeIf(::isGalleryViewerWebUrl)?.let { return it }
    val url = postUrl?.takeIf(::isGalleryViewerWebUrl) ?: return null
    val schemeSeparator = url.indexOf("://")
    val host = url.substring(schemeSeparator + 3)
        .substringBeforeAny('/', '?', '#')
        .substringBefore(':')
        .lowercase()
    if (host !in setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com")) return null
    val path = url.substring(schemeSeparator + 3)
        .substringAfter('/', "")
        .substringBeforeAny('?', '#')
    val segments = path.split('/')
    if (segments.size < 3 || segments[1] != "status") return null
    val username = segments[0]
    if (username.isEmpty() || username.any { !it.isLetterOrDigit() && it != '_' }) return null
    return "https://x.com/$username"
}

internal fun galleryViewerAuthor(
    notePath: String,
    author: String?,
    profileUrl: String?,
): String? {
    author?.takeIf(String::isNotBlank)?.let { return it }
    if (profileUrl != null) {
        val profilePath = profileUrl.substringAfter("://")
            .substringAfter('/', "")
            .substringBeforeAny('?', '#')
        val username = profilePath.substringBefore('/').takeIf(String::isNotEmpty)
        if (username != null) return "@$username"
    }
    val basename = notePath.substringAfterLast('/')
    val filename = basename.substringBeforeLast('.', basename)
    val separator = filename.indexOf("-on-X-")
    return filename.takeIf { separator > 0 }?.substring(0, separator)
}

internal fun galleryViewerShowsAuthorAboveMedia(
    noteStructure: GalleryNoteStructureSettings,
): Boolean {
    val authorPosition = noteStructure.blockOrder.indexOf(GalleryNoteBlock.AUTHOR)
    val mediaPosition = noteStructure.blockOrder.indexOf(GalleryNoteBlock.MEDIA)
    return authorPosition >= 0 &&
        mediaPosition >= 0 &&
        authorPosition < mediaPosition &&
        GalleryNoteBlock.AUTHOR !in noteStructure.hiddenBlocks
}

private fun String.substringBeforeAny(vararg delimiters: Char): String {
    val end = delimiters.map { indexOf(it) }.filter { it >= 0 }.minOrNull() ?: length
    return substring(0, end)
}

internal fun galleryViewerCanForwardMediaVerticalScroll(
    detailsVisible: Boolean,
    fullscreen: Boolean,
    isVideo: Boolean,
    imageZoomed: Boolean,
): Boolean = detailsVisible && !fullscreen && !isVideo && !imageZoomed

internal fun galleryViewerDetailsPanelFraction(
    scrollOffsetPx: Int,
    expandDistancePx: Float = 220f,
): Float {
    val baseFraction = 0.4f
    val maximumFraction = 0.82f
    val expansion = (scrollOffsetPx / expandDistancePx).coerceIn(0f, 1f)
    return baseFraction + (maximumFraction - baseFraction) * expansion
}

internal data class GalleryViewerPresentationState(
    val detailsVisible: Boolean = false,
    val fullscreen: Boolean = false,
) {
    fun toggleMedia(): GalleryViewerPresentationState =
        if (fullscreen || !detailsVisible) {
            copy(detailsVisible = true, fullscreen = false)
        } else {
            copy(detailsVisible = false)
        }

    fun enterFullscreen(): GalleryViewerPresentationState =
        copy(detailsVisible = false, fullscreen = true)

    fun exitFullscreen(): GalleryViewerPresentationState =
        copy(detailsVisible = true, fullscreen = false)
}

@Composable
private fun NoteDetails(
    note: GalleryNoteDetail,
    strings: GalleryViewerStrings,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit,
    tagColorRules: List<GalleryTagColorRule>,
    noteStructure: GalleryNoteStructureSettings,
) {
    Text(note.title, style = MaterialTheme.typography.titleLarge)
    note.published?.let { Text("${strings.published}: ${it.replace('T', ' ')}") }
    note.created?.let { Text("${strings.created}: ${it.replace('T', ' ')}") }
    note.updated?.let { Text("${strings.updated}: ${it.replace('T', ' ')}") }
    if (note.tags.isNotEmpty()) {
        Text(strings.tags, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            note.tags.forEach { tag ->
                AssistChip(
                    onClick = {},
                    label = { Text(tag) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(
                            GalleryTagColorCodec.colorFor(tag, tagColorRules),
                        ).copy(alpha = 0.22f),
                    ),
                )
            }
        }
    }
    noteDetailContentBlocks(noteStructure)
        .forEach { block ->
            when (block) {
                GalleryNoteBlock.AUTHOR -> {
                    note.author?.let { Text("${strings.author}: $it") }
                    note.authorUrl?.let { url ->
                        TextButton(onClick = { onExternalLink(url) }) { Text(strings.link) }
                    }
                    note.url?.let { url ->
                        TextButton(onClick = { onExternalLink(url) }) { Text(strings.link) }
                    }
                }
                GalleryNoteBlock.POST_TEXT -> {
                    note.bodyText.takeIf(String::isNotBlank)?.let { Text(it) }
                }
                GalleryNoteBlock.MEMO -> {
                    DetailLines(
                        heading = strings.memo,
                        lines = note.memoLines,
                        onExternalLink = onExternalLink,
                    )
                }
                GalleryNoteBlock.RELATED -> {
                    DetailLines(
                        heading = strings.related,
                        lines = note.relatedLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openNoteLabel = strings.openNote,
                    )
                }
                GalleryNoteBlock.POST_TEXT_END -> {
                    DetailLines(
                        heading = strings.postTextEnd,
                        lines = note.postTextEndLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openNoteLabel = strings.openNote,
                    )
                }
                GalleryNoteBlock.MEDIA -> Unit
            }
        }
}

internal fun noteDetailContentBlocks(
    noteStructure: GalleryNoteStructureSettings,
): List<GalleryNoteBlock> {
    return noteStructure.blockOrder.filter {
        it in RENDERED_NOTE_DETAIL_BLOCKS && it !in noteStructure.hiddenBlocks
    }
}

private val RENDERED_NOTE_DETAIL_BLOCKS = setOf(
    GalleryNoteBlock.AUTHOR,
    GalleryNoteBlock.POST_TEXT,
    GalleryNoteBlock.MEMO,
    GalleryNoteBlock.RELATED,
    GalleryNoteBlock.POST_TEXT_END,
)

@Composable
private fun DetailLines(
    heading: String,
    lines: List<GalleryDetailLine>,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit = {},
    openNoteLabel: String = "",
) {
    if (lines.isEmpty()) return
    Text(heading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    lines.forEach { line ->
        Text(
            line.text,
            modifier = Modifier.padding(start = (line.indentLevel * 12).dp),
        )
        line.urls.forEach { url ->
            TextButton(onClick = { onExternalLink(url) }) { Text(url) }
        }
        line.linkedNoteId?.let { noteId ->
            TextButton(onClick = { onOpenNote(noteId) }) { Text(openNoteLabel) }
        }
    }
}
