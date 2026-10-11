@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.ui.gallery

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.exp
import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorRule
import com.hanaretamae.kaede.core.settings.GalleryTagDisplayPrefixesCodec
import com.hanaretamae.kaede.core.settings.isGalleryTagDisplayed
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings

private const val TRACKPAD_GESTURE_IDLE_MILLIS = 220L
private const val TRACKPAD_SWIPE_THRESHOLD = 48f

data class GalleryViewerStrings(
    val close: String,
    val previous: String,
    val next: String,
    val loading: String,
    val noMedia: String,
    val showDetails: String,
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
    showDetails = "Show details",
    author = "Author",
    link = "Open link",
    memo = "Memo",
    related = "Related",
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
    showDetails = "詳細を表示",
    author = "作者",
    link = "リンクを開く",
    memo = "メモ",
    related = "関連",
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
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onBackHandlerChanged: (((() -> Unit)?) -> Unit)? = null,
    tagColorRules: List<GalleryTagColorRule> = GalleryTagColorCodec.DEFAULT_RULES,
    hiddenTagPrefixes: List<String> = GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN,
    noteStructure: GalleryNoteStructureSettings = GalleryNoteStructureSettings(),
) {
    val state by stateHolder.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val detailsScrollState = rememberScrollState()
    val viewerTopBarColor by animateColorAsState(
        targetValue = if (detailsScrollState.canScrollBackward) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.Transparent
        },
        label = "viewerTopBarColor",
    )
    val motionScheme = remember { MotionScheme.expressive() }

    val density = LocalDensity.current
    val detailsExpansionDistancePx = with(density) { 220.dp.toPx() }
    val detailsPanelExpansionPx = remember(stateHolder) { mutableFloatStateOf(0f) }
    val focusRequester = remember(stateHolder) { FocusRequester() }
    val presentation = remember(stateHolder) {
        mutableStateOf(GalleryViewerPresentationState(detailsVisible = true))
    }
    var playbackOverlayVisible by remember(stateHolder) { mutableStateOf(false) }
    val playbackOverlayShiftPx by animateFloatAsState(
        targetValue = if (
            playbackOverlayVisible &&
            presentation.value.detailsVisible &&
            !presentation.value.fullscreen
        ) {
            detailsExpansionDistancePx * PLAYBACK_OVERLAY_DETAILS_SHIFT
        } else {
            0f
        },
        label = "playbackOverlayDetailsShift",
    )
    var showMediaActions by remember(stateHolder) { mutableStateOf(false) }
    var viewerClosing by remember(stateHolder) { mutableStateOf(false) }
    val mediaScale = remember(stateHolder) { mutableFloatStateOf(1f) }
    val mediaOffset = remember(stateHolder) { mutableStateOf(Offset.Zero) }
    var mediaViewportSize by remember { mutableStateOf(IntSize.Zero) }
    val transformState = rememberTransformableState { centroid, zoom, pan, _ ->
        val oldScale = mediaScale.floatValue
        val newScale = nextImageScale(oldScale, zoom)
        val focus = centroid - Offset(mediaViewportSize.width / 2f, mediaViewportSize.height / 2f)
        mediaOffset.value = galleryViewerZoomOffset(
            mediaOffset.value, focus, oldScale, newScale, pan,
        )
        mediaScale.floatValue = newScale
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
        onDispose { onMediaOnlyChanged?.invoke(false) }
    }
    LaunchedEffect(
        presentation.value.detailsVisible,
        presentation.value.fullscreen,
        onMediaOnlyChanged,
    ) {
        onMediaOnlyChanged?.invoke(
            !presentation.value.detailsVisible && !presentation.value.fullscreen,
        )
    }
    DisposableEffect(stateHolder) {
        onDispose(stateHolder::dispose)
    }
    DisposableEffect(onFullscreenChanged) {
        onDispose { onFullscreenChanged?.invoke(false) }
    }
    fun changeDetailsPanelExpansion(deltaPx: Float) {
        detailsPanelExpansionPx.floatValue =
            (detailsPanelExpansionPx.floatValue + deltaPx).coerceIn(0f, detailsExpansionDistancePx)
    }
    fun exitFullscreen() {
        if (!presentation.value.fullscreen) return
        showMediaActions = false
        presentation.value = presentation.value.exitFullscreen()
        onFullscreenChanged?.invoke(false)
        mediaScale.floatValue = 1f
        mediaOffset.value = Offset.Zero
    }
    fun enterFullscreen() {
        if (presentation.value.fullscreen) return
        showMediaActions = false
        presentation.value = presentation.value.enterFullscreen()
        mediaScale.floatValue = 1f
        mediaOffset.value = Offset.Zero
        onFullscreenChanged?.invoke(true)
    }
    fun toggleFullscreen() {
        if (presentation.value.fullscreen) exitFullscreen() else enterFullscreen()
    }
    SideEffect {
        onBackHandlerChanged?.invoke(
            if (presentation.value.fullscreen) ::exitFullscreen else null,
        )
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
                                exitFullscreen()
                                true
                            } else if (presentation.value.detailsVisible) {
                                presentation.value = presentation.value.toggleMedia()
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
            showMediaActions = false
            val previous = presentation.value
            presentation.value = previous.toggleMedia()
            if (previous.fullscreen) {
                onFullscreenChanged?.invoke(false)
            }
            mediaScale.floatValue = 1f
            mediaOffset.value = Offset.Zero
        }

        val activeMedia = state.selectedMedia
        val mediaState = activeMedia?.let {
            GalleryViewerMediaState(
                media = it,
                location = state.mediaLocation,
                locationLoading = state.mediaLocationLoading,
            )
        }
        val mediaActions = mediaState?.takeIf { it.location != null }?.let { currentMedia ->
            buildList {
                onOpenMedia?.let { open ->
                    add(Triple(strings.openMedia, Icons.AutoMirrored.Filled.OpenInNew) { open(currentMedia) })
                }
                onRevealMedia?.let { reveal ->
                    add(Triple(strings.revealMedia, Icons.Filled.FolderOpen) { reveal(currentMedia) })
                }
                onSetWallpaperMedia?.takeIf {
                    galleryCanSetWallpaper(currentMedia.location, currentMedia.media.isVideo)
                }?.let { setWallpaper ->
                    add(Triple(strings.setWallpaper, Icons.Filled.Wallpaper) { setWallpaper(currentMedia) })
                }
            }
        }.orEmpty()

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = presentation.value.detailsVisible && !presentation.value.fullscreen,
                enter = slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { -it / 2 },
                ) + fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                exit = slideOutVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { -it / 2 },
                ) + fadeOut(animationSpec = motionScheme.defaultEffectsSpec()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(viewerTopBarColor)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KaedeIconButton(
                        label = strings.close,
                        onClick = {
                            viewerClosing = true
                            onClose()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.close)
                    }
                    Spacer(Modifier.width(4.dp))
                    val note = state.note
                    val noteTitle = note?.let { galleryViewerTitle(it.path, it.title) }
                    if (noteTitle != null) {
                        Text(
                            text = noteTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (onFullscreenChanged != null && state.selectedMedia != null) {
                        KaedeIconButton(
                            label = strings.fullscreen,
                            onClick = ::enterFullscreen,
                        ) {
                            Icon(Icons.Filled.Fullscreen, strings.fullscreen)
                        }
                    }
                    if (mediaActions.isNotEmpty()) {
                        KaedeIconButton(
                            label = strings.openMedia,
                            onClick = { showMediaActions = !showMediaActions },
                        ) {
                            Icon(Icons.Filled.MoreVert, strings.openMedia)
                        }
                    }
                    val currentNote = state.note
                    if (currentNote != null && onOpenVaultNote != null) {
                        KaedeIconButton(
                            label = strings.openNoteInObsidian,
                            onClick = { onOpenVaultNote(currentNote.path) },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, strings.openNoteInObsidian)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showMediaActions && mediaActions.isNotEmpty() &&
                    presentation.value.detailsVisible && !presentation.value.fullscreen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Surface(
                        modifier = Modifier.widthIn(min = 200.dp, max = 320.dp).padding(bottom = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column {
                            mediaActions.forEachIndexed { index, (label, icon, action) ->
                                KaedeMenuItem(
                                    text = label,
                                    index = index,
                                    count = mediaActions.size,
                                    leadingIcon = icon,
                                    onClick = {
                                        showMediaActions = false
                                        action()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            val media = state.selectedMedia
            if (state.loading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator()
                    }
                }
            } else if (media == null && state.error == null) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val detailsPanelHeight = maxHeight * galleryViewerDetailsPanelFraction(
                        scrollOffsetPx = detailsPanelExpansionPx.floatValue.roundToInt(),
                        expandDistancePx = detailsExpansionDistancePx,
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .pointerInput(
                                    presentation.value.detailsVisible,
                                    presentation.value.fullscreen,
                                ) {
                                    if (
                                        !presentation.value.detailsVisible ||
                                        presentation.value.fullscreen
                                    ) {
                                        return@pointerInput
                                    }
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
                                            if (event.changes.count { it.pressed } > 1) {
                                                pointerPressed = event.changes.any { it.pressed }
                                                continue
                                            }
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
                                                changeDetailsPanelExpansion(-delta.y)
                                                change.consume()
                                            }
                                            pointerPressed = change.pressed
                                        }
                                    }
                                }
                                .clickable(onClick = ::toggleDetails),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(strings.noMedia, style = MaterialTheme.typography.titleMedium)
                        }
                        AnimatedVisibility(
                            visible = state.note != null &&
                                presentation.value.detailsVisible &&
                                !presentation.value.fullscreen,
                            enter = expandVertically(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) + fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                            exit = shrinkVertically(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) + fadeOut(animationSpec = motionScheme.defaultEffectsSpec()),
                        ) {
                            state.note?.let { note ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .height(detailsPanelHeight)
                                        .verticalScroll(detailsScrollState)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(16.dp),
                                ) {
                                    NoteDetails(
                                        note,
                                        strings,
                                        onExternalLink,
                                        onOpenNote,
                                        onCopyPageUrl = { url ->
                                            clipboardManager.setText(AnnotatedString(url))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(strings.copiedPageUrl)
                                            }
                                        },
                                        tagColorRules,
                                        hiddenTagPrefixes,
                                        noteStructure,
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (media != null) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    var trackpadScrollX by remember(stateHolder) {
                        mutableFloatStateOf(0f)
                    }
                    var trackpadLastEventAt by remember(stateHolder) {
                        mutableLongStateOf(0L)
                    }
                    var trackpadGestureConsumed by remember(stateHolder) {
                        mutableStateOf(false)
                    }
                    val detailsPanelHeight = maxHeight * galleryViewerDetailsPanelFraction(
                        scrollOffsetPx = (
                            detailsPanelExpansionPx.floatValue + playbackOverlayShiftPx
                            ).roundToInt(),
                        expandDistancePx = detailsExpansionDistancePx,
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .pointerInput(
                                    state.selectedMediaIndex,
                                    presentation.value.fullscreen,
                                    media.isVideo,
                                ) {
                                    if (media.isVideo) return@pointerInput
                                    val touchSlop = viewConfiguration.touchSlop
                                    val swipeThreshold = with(density) { 72.dp.toPx() }
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial,
                                        )
                                        var previous = down.position
                                        var total = Offset.Zero
                                        var horizontalDrag = false
                                        var pointerPressed = true
                                        while (pointerPressed) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull {
                                                it.id == down.id
                                            } ?: break
                                            if (event.changes.count { it.pressed } > 1) {
                                                total = Offset.Zero
                                                horizontalDrag = false
                                                previous = change.position
                                                pointerPressed = change.pressed
                                                continue
                                            }
                                            val delta = change.position - previous
                                            previous = change.position
                                            total += delta
                                            if (
                                                !horizontalDrag &&
                                                abs(total.x) > touchSlop &&
                                                abs(total.x) > abs(total.y) &&
                                                mediaScale.floatValue == 1f
                                            ) {
                                                horizontalDrag = true
                                            }
                                            if (horizontalDrag) change.consume()
                                            pointerPressed = change.pressed
                                        }
                                        when (
                                            galleryViewerSwipeDirection(
                                                horizontalOffsetPx = total.x,
                                                verticalOffsetPx = total.y,
                                                touchSlopPx = touchSlop,
                                                swipeThresholdPx = swipeThreshold,
                                                imageZoomed = mediaScale.floatValue != 1f,
                                            )
                                        ) {
                                            GalleryViewerSwipeDirection.NEXT ->
                                                stateHolder.showNextMedia()
                                            GalleryViewerSwipeDirection.PREVIOUS ->
                                                stateHolder.showPreviousMedia()
                                            null -> Unit
                                        }
                                    }
                                }
                                .pointerInput(
                                    presentation.value.detailsVisible,
                                    presentation.value.fullscreen,
                                    media.isVideo,
                                ) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            if (
                                                event.type == PointerEventType.Scroll
                                            ) {
                                                val change = event.changes.firstOrNull()
                                                    ?: continue
                                                if (event.keyboardModifiers.isCtrlPressed && !media.isVideo) {
                                                    val oldScale = mediaScale.floatValue
                                                    val newScale = nextImageScale(
                                                        oldScale, exp(-change.scrollDelta.y * 0.12f),
                                                    )
                                                    val focus = change.position - Offset(
                                                        size.width / 2f, size.height / 2f,
                                                    )
                                                    mediaOffset.value = galleryViewerZoomOffset(
                                                        mediaOffset.value, focus, oldScale, newScale,
                                                    )
                                                    mediaScale.floatValue = newScale
                                                    change.consume()
                                                    continue
                                                }
                                                if (mediaScale.floatValue != 1f) continue
                                                val now = System.currentTimeMillis()
                                                if (
                                                    now - trackpadLastEventAt >
                                                    TRACKPAD_GESTURE_IDLE_MILLIS
                                                ) {
                                                    trackpadScrollX = 0f
                                                    trackpadGestureConsumed = false
                                                }
                                                trackpadLastEventAt = now
                                                val scrollDelta =
                                                    if (event.keyboardModifiers.isShiftPressed &&
                                                        change.scrollDelta.x == 0f
                                                    ) {
                                                        Offset(change.scrollDelta.y, 0f)
                                                    } else {
                                                        change.scrollDelta
                                                    }
                                                if (event.changes.count { it.pressed } > 1) {
                                                    continue
                                                }
                                                if (
                                                    presentation.value.detailsVisible &&
                                                    !presentation.value.fullscreen &&
                                                    !media.isVideo &&
                                                    mediaScale.floatValue == 1f &&
                                                    abs(scrollDelta.y) > abs(scrollDelta.x)
                                                ) {
                                                    changeDetailsPanelExpansion(scrollDelta.y)
                                                }
                                                if (
                                                    abs(scrollDelta.x) > 0f &&
                                                    abs(scrollDelta.x) >= abs(scrollDelta.y) * 0.75f
                                                ) {
                                                    trackpadScrollX += scrollDelta.x * 24f * density.density
                                                    change.consume()
                                                }
                                                if (
                                                    !trackpadGestureConsumed &&
                                                    galleryViewerTrackpadSwipeDirection(
                                                        trackpadScrollX,
                                                        TRACKPAD_SWIPE_THRESHOLD,
                                                        imageZoomed = false,
                                                    ) != null
                                                ) {
                                                    trackpadGestureConsumed = true
                                                    when (
                                                        galleryViewerTrackpadSwipeDirection(
                                                            trackpadScrollX,
                                                            TRACKPAD_SWIPE_THRESHOLD,
                                                            imageZoomed = false,
                                                        )
                                                    ) {
                                                        GalleryViewerSwipeDirection.NEXT ->
                                                            stateHolder.showNextMedia()
                                                        GalleryViewerSwipeDirection.PREVIOUS ->
                                                            stateHolder.showPreviousMedia()
                                                        null -> Unit
                                                    }
                                                    trackpadScrollX = 0f
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .pointerInput(
                                        presentation.value.detailsVisible,
                                        presentation.value.fullscreen,
                                        media.isVideo,
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
                                                    if (event.changes.count { it.pressed } > 1) {
                                                        break
                                                    }
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
                                                    if (draggingVertically && mediaScale.floatValue == 1f) {
                                                        changeDetailsPanelExpansion(-delta.y)
                                                        change.consume()
                                                    }
                                                    pointerPressed = change.pressed
                                                }
                                            }
                                        }
                                    }
                                    .then(
                                        if (media.isVideo) {
                                            Modifier
                                        } else {
                                            Modifier.clickable(
                                                interactionSource = remember {
                                                    MutableInteractionSource()
                                                },
                                                indication = null,
                                                onClick = ::toggleDetails,
                                            )
                                        },
                                    )
                                    .transformable(state = transformState, enabled = !media.isVideo)
                                    .onSizeChanged { mediaViewportSize = it },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!viewerClosing) {
                                    Box(
                                        Modifier.fillMaxSize().graphicsLayer {
                                            scaleX = mediaScale.floatValue
                                            scaleY = mediaScale.floatValue
                                            translationX = mediaOffset.value.x
                                            translationY = mediaOffset.value.y
                                        },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AnimatedContent(
                                            targetState = media,
                                            transitionSpec = {
                                                if (initialState.isVideo || targetState.isVideo) {
                                                    EnterTransition.None togetherWith ExitTransition.None
                                                } else {
                                                    val movingForward =
                                                        state.media.indexOfFirst {
                                                            it.id == targetState.id
                                                        } > state.media.indexOfFirst {
                                                            it.id == initialState.id
                                                        }
                                                    if (movingForward) {
                                                        (
                                                                    slideInHorizontally(tween(220)) { it / 6 } +
                                                                        fadeIn(tween(180))
                                                            ) togetherWith (
                                                                    slideOutHorizontally(tween(220)) { -it / 6 } +
                                                                        fadeOut(tween(140))
                                                            )
                                                    } else {
                                                        (
                                                                    slideInHorizontally(tween(220)) { -it / 6 } +
                                                                        fadeIn(tween(180))
                                                            ) togetherWith (
                                                                    slideOutHorizontally(tween(220)) { it / 6 } +
                                                                        fadeOut(tween(140))
                                                            )
                                                    }
                                                }
                                            },
                                            label = "viewerMediaTransition",
                                        ) { targetMedia ->
                                            val isSelected = targetMedia.id == state.selectedMedia?.id
                                            val targetLocation = if (isSelected) {
                                                state.mediaLocation
                                            } else {
                                                stateHolder.cachedMediaLocation(targetMedia.id)
                                            }
                                            mediaContent(
                                                GalleryViewerMediaState(
                                                    media = targetMedia,
                                                    location = targetLocation,
                                                    locationLoading = isSelected &&
                                                        state.mediaLocationLoading,
                                                    fullscreen = presentation.value.fullscreen,
                                                    prefetchLocations = if (isSelected) {
                                                        state.neighborMediaLocations
                                                    } else {
                                                        emptyList()
                                                    },
                                                    prefetchMediaIds = if (isSelected) {
                                                        state.neighborMediaIds
                                                    } else {
                                                        emptyList()
                                                    },
                                                    onPlaybackOverlayVisibilityChanged = {
                                                        playbackOverlayVisible = it
                                                    },
                                                    onToggleFullscreen = if (onFullscreenChanged != null) {
                                                        ::toggleFullscreen
                                                    } else {
                                                        null
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                }
                                if (presentation.value.fullscreen && !media.isVideo) {
                                    KaedeTooltip(strings.exitFullscreen) {
                                        FilledTonalIconButton(
                                            shapes = IconButtonDefaults.shapes(),
                                            onClick = ::exitFullscreen,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                        ) {
                                            Icon(Icons.Filled.FullscreenExit, strings.exitFullscreen)
                                        }
                                    }
                                }
                                if (state.media.size > 1) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                            .padding(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            .copy(alpha = 0.86f),
                                        shape = MaterialTheme.shapes.large,
                                    ) {
                                        Text(
                                            "${state.selectedMediaIndex + 1} / ${state.media.size}",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = state.note != null &&
                                presentation.value.detailsVisible &&
                                !presentation.value.fullscreen,
                            enter = expandVertically(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) + fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                            exit = shrinkVertically(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) + fadeOut(animationSpec = motionScheme.defaultEffectsSpec()),
                        ) {
                            state.note?.let { note ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .height(detailsPanelHeight)
                                        .verticalScroll(detailsScrollState)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .testTag("viewer-note-details")
                                        .padding(16.dp),
                                ) {
                                    NoteDetails(
                                        note,
                                        strings,
                                        onExternalLink,
                                        onOpenNote,
                                        onCopyPageUrl = { url ->
                                            clipboardManager.setText(AnnotatedString(url))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(strings.copiedPageUrl)
                                            }
                                        },
                                        tagColorRules,
                                        hiddenTagPrefixes,
                                        noteStructure,
                                    )
                                }
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

internal fun galleryViewerZoomOffset(
    offset: Offset,
    focusFromCenter: Offset,
    oldScale: Float,
    newScale: Float,
    pan: Offset = Offset.Zero,
): Offset = if (newScale == 1f) {
    Offset.Zero
} else {
    focusFromCenter - (focusFromCenter - offset) * (newScale / oldScale) + pan
}

internal enum class GalleryViewerSwipeDirection {
    PREVIOUS,
    NEXT,
}

internal fun galleryViewerSwipeDirection(
    horizontalOffsetPx: Float,
    verticalOffsetPx: Float,
    touchSlopPx: Float,
    swipeThresholdPx: Float,
    imageZoomed: Boolean,
): GalleryViewerSwipeDirection? {
    if (
        imageZoomed ||
        abs(horizontalOffsetPx) < swipeThresholdPx ||
        abs(horizontalOffsetPx) <= touchSlopPx ||
        abs(horizontalOffsetPx) <= abs(verticalOffsetPx)
    ) {
        return null
    }

    return if (horizontalOffsetPx < 0) {
        GalleryViewerSwipeDirection.NEXT
    } else {
        GalleryViewerSwipeDirection.PREVIOUS
    }
}

internal fun galleryViewerTrackpadSwipeDirection(
    horizontalScrollPx: Float,
    thresholdPx: Float,
    imageZoomed: Boolean,
): GalleryViewerSwipeDirection? {
    if (imageZoomed || abs(horizontalScrollPx) < thresholdPx) return null
    return if (horizontalScrollPx < 0f) {
        GalleryViewerSwipeDirection.NEXT
    } else {
        GalleryViewerSwipeDirection.PREVIOUS
    }
}

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

internal fun galleryViewerVisibleTags(
    tags: List<String>,
    hiddenPrefixes: List<String>,
): List<String> = tags.filter { tag ->
    isGalleryTagDisplayed(
        tag,
        includedPrefixes = GalleryTagDisplayPrefixesCodec.DEFAULT_INCLUDED,
        hiddenPrefixes = hiddenPrefixes,
    )
}.sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })

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

private const val PLAYBACK_OVERLAY_DETAILS_SHIFT = 0.5f

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteDetails(
    note: GalleryNoteDetail,
    strings: GalleryViewerStrings,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit,
    onCopyPageUrl: (String) -> Unit,
    tagColorRules: List<GalleryTagColorRule>,
    hiddenTagPrefixes: List<String>,
    noteStructure: GalleryNoteStructureSettings,
) {
    Text(note.title, style = MaterialTheme.typography.titleLarge)
    val profileUrl = galleryViewerAuthorProfileUrl(note.authorUrl, note.url)
    val author = galleryViewerAuthor(note.path, note.author, profileUrl)
    val showAuthor = GalleryNoteBlock.AUTHOR !in noteStructure.hiddenBlocks
    val pageUrl = note.url?.takeIf(::isGalleryViewerWebUrl)
    if ((showAuthor && (author != null || profileUrl != null)) || pageUrl != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAuthor && author != null) {
                Text(
                    author,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            profileUrl?.takeIf { showAuthor }?.let { url ->
                KaedeIconButton(label = strings.link, onClick = { onExternalLink(url) }) {
                    Icon(Icons.Filled.Person, strings.openAuthorProfile)
                }
            }
            pageUrl?.let { url ->
                KaedeIconButton(label = strings.link, onClick = { onExternalLink(url) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        strings.openOriginalPage,
                    )
                }
                KaedeIconButton(label = strings.copyPageUrl, onClick = { onCopyPageUrl(url) }) {
                    Icon(Icons.Filled.ContentCopy, strings.copyPageUrl)
                }
            }
        }
    }
    if (note.published != null || note.created != null || note.updated != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                note.published?.let {
                    Text(
                        "${strings.published}: ${it.replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                note.created?.let {
                    Text(
                        "${strings.created}: ${it.replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                note.updated?.let {
                    Text(
                        "${strings.updated}: ${it.replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    val visibleTags = galleryViewerVisibleTags(note.tags, hiddenTagPrefixes)
    if (visibleTags.isNotEmpty()) {
        Text(
            strings.tags,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            visibleTags.forEach { tag ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(
                        GalleryTagColorCodec.colorFor(tag, tagColorRules),
                    ).copy(alpha = 0.22f),
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
    noteDetailContentBlocks(noteStructure)
        .forEach { block ->
            when (block) {
                GalleryNoteBlock.AUTHOR -> {}
                GalleryNoteBlock.POST_TEXT -> {
                    note.bodyText.takeIf(String::isNotBlank)?.let { text ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(text, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                GalleryNoteBlock.MEMO -> {
                    DetailLines(
                        heading = strings.memo,
                        lines = note.memoLines,
                        onExternalLink = onExternalLink,
                        openLinkLabel = strings.link,
                    )
                }
                GalleryNoteBlock.RELATED -> {
                    DetailLines(
                        heading = strings.related,
                        lines = note.relatedLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openLinkLabel = strings.link,
                    )
                }
                GalleryNoteBlock.POST_TEXT_END -> {
                    DetailLines(
                        heading = strings.postTextEnd,
                        lines = note.postTextEndLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openLinkLabel = strings.link,
                    )
                }
                GalleryNoteBlock.MEDIA -> Unit
            }
        }
}

internal fun galleryViewerDetailLineActionLabel(
    line: GalleryDetailLine,
    openLinkLabel: String,
): String? {
    val safeUrls = line.urls.filter(::isGalleryViewerWebUrl)
    return when {
        line.linkedNoteId != null -> line.text
        line.urls.size == 1 && safeUrls.size == 1 -> line.text
        safeUrls.isNotEmpty() -> openLinkLabel
        else -> null
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
    openLinkLabel: String = "",
) {
    if (lines.isEmpty()) return
    Text(
        heading,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            lines.forEach { line ->
                val safeUrls = line.urls.filter(::isGalleryViewerWebUrl)
                val actionLabel = galleryViewerDetailLineActionLabel(line, openLinkLabel)
                val linkedNoteId = line.linkedNoteId
                val modifier = Modifier.padding(start = (line.indentLevel * 12).dp)
                when {
                    linkedNoteId != null -> TextButton(
                        shapes = ButtonDefaults.shapes(),
                        modifier = modifier,
                        onClick = { onOpenNote(linkedNoteId) },
                    ) {
                        Text(actionLabel ?: line.text)
                    }
                    line.urls.size == 1 && safeUrls.size == 1 -> TextButton(
                        shapes = ButtonDefaults.shapes(),
                        modifier = modifier,
                        onClick = { onExternalLink(safeUrls.single()) },
                    ) {
                        Text(actionLabel ?: line.text)
                    }
                    else -> {
                        Text(line.text, modifier = modifier)
                        safeUrls.forEach { url ->
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { onExternalLink(url) }) {
                                Text(actionLabel ?: openLinkLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
