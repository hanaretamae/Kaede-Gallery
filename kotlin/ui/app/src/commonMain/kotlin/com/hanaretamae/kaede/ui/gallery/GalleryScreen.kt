@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.ui.gallery

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SegmentedListItem
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.model.GalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.GallerySortField
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.GalleryTagDisplayPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorRule
import com.hanaretamae.kaede.core.settings.isGalleryTagDisplayed
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.floor

private const val GALLERY_GRID_MIN_CELL_WIDTH_DP = 200f
private const val GALLERY_GRID_HORIZONTAL_PADDING_DP = 24f
private const val GALLERY_GRID_SPACING_DP = 12f
private const val GALLERY_GRID_MINIMUM_COLUMN_COUNT = 2
private const val GALLERY_SCROLLBAR_HIDE_DELAY_MILLIS = 900L

internal fun galleryAdaptiveColumnCount(availableWidthDp: Float): Int {
    if (!availableWidthDp.isFinite() || availableWidthDp <= 0f) {
        return GALLERY_GRID_MINIMUM_COLUMN_COUNT
    }
    val availableGridWidth = availableWidthDp - GALLERY_GRID_HORIZONTAL_PADDING_DP
    val count = floor(
        (availableGridWidth + GALLERY_GRID_SPACING_DP) /
            (GALLERY_GRID_MIN_CELL_WIDTH_DP + GALLERY_GRID_SPACING_DP),
    ).toInt()
    return count.coerceAtLeast(GALLERY_GRID_MINIMUM_COLUMN_COUNT)
}

data class GalleryStrings(
    val title: String,
    val search: String,
    val notes: String,
    val media: String,
    val loading: String,
    val noItems: String,
    val retry: String,
    val reload: String,
    val filters: String,
    val settings: String,
    val grid: String,
    val list: String,
    val changeVault: String,
    val forgetVault: String,
    val forgetVaultTitle: String,
    val forgetVaultMessage: String,
    val forgetVaultConfirm: String,
    val cancel: String,
    val jump: String,
    val jumpTitle: String,
    val positionInput: String,
    val invalidPosition: String,
    val jumpLoading: (Long) -> String,
    val jumpFailed: String,
    val close: String,
    val clearFilters: String,
    val searchNotes: String,
    val clearSearchText: String,
    val applySearch: String,
    val clearAllSearchAndFilters: String,
    val filterSearch: String,
    val fuzzySearch: String,
    val fuzzySearchTerm: String,
    val exactWordTerm: String,
    val loadingTags: String,
    val noTags: String,
    val includeAny: String,
    val includeAll: String,
    val exclude: String,
    val inactive: String,
    val created: String,
    val published: String,
    val ascending: String,
    val descending: String,
    val image: String,
    val video: String,
    val missingMedia: String,
    val counts: (Long, Long, Long) -> String,
    val error: (RepositoryError) -> String,
    val count: (Long, Long, Long) -> String,
    val totalCount: (Long) -> String,
    val optionCount: (Long) -> String,
    val scrollbarPosition: (Long, Long) -> String,
    val virtualLabel: (VirtualFilter) -> String,
)

val EnglishGalleryStrings = GalleryStrings(
    title = "Kaede Gallery",
    search = "Search notes and tags",
    notes = "Notes",
    media = "Media",
    loading = "Loading…",
    noItems = "No items found",
    retry = "Retry",
    reload = "Reload",
    filters = "Filters",
    settings = "Settings",
    grid = "Grid",
    list = "List",
    changeVault = "Change Vault",
    forgetVault = "Forget Vault",
    forgetVaultTitle = "Forget selected Vault?",
    forgetVaultMessage = "This removes its local index and thumbnails. Vault files are not changed.",
    forgetVaultConfirm = "Forget",
    cancel = "Cancel",
    jump = "Jump",
    jumpTitle = "Jump to position",
    positionInput = "Item position",
    invalidPosition = "Enter a valid item position.",
    jumpLoading = { "Loading items around position $it…" },
    jumpFailed = "The requested position could not be loaded.",
    close = "Close",
    clearFilters = "Clear filters",
    searchNotes = "Search notes",
    clearSearchText = "Clear search text",
    applySearch = "Search",
    clearAllSearchAndFilters = "Clear all",
    filterSearch = "Filter tags",
    fuzzySearch = "Fuzzy search",
    fuzzySearchTerm = "Fuzzy (~word)",
    exactWordTerm = "Exact word (=word)",
    loadingTags = "Loading tags…",
    noTags = "No matching tags",
    includeAny = "Include (OR)",
    includeAll = "Include (AND)",
    exclude = "Exclude",
    inactive = "Inactive",
    created = "Created",
    published = "Published",
    ascending = "Ascending",
    descending = "Descending",
    image = "Image",
    video = "Video",
    missingMedia = "Media file unavailable",
    counts = { media, memo, related -> "$media media · $memo memos · $related related" },
    error = ::englishError,
    count = { first, last, total ->
        if (last == 0L) "0 of $total items" else "$first–$last of $total items"
    },
    totalCount = { "$it items" },
    optionCount = { "$it" },
    scrollbarPosition = { position, total -> "$position / $total" },
    virtualLabel = ::englishVirtualLabel,
)

val JapaneseGalleryStrings = GalleryStrings(
    title = "Kaede Gallery",
    search = "ノート・タグを検索",
    notes = "ノート",
    media = "メディア",
    loading = "読み込み中…",
    noItems = "項目がありません",
    retry = "再試行",
    reload = "再読み込み",
    filters = "フィルター",
    settings = "設定",
    grid = "グリッド",
    list = "リスト",
    changeVault = "Vaultを変更",
    forgetVault = "Vaultの登録を解除",
    forgetVaultTitle = "選択中のVaultを登録解除しますか？",
    forgetVaultMessage = "ローカルの索引とサムネイルを削除します。Vault内のファイルは変更しません。",
    forgetVaultConfirm = "登録解除",
    cancel = "キャンセル",
    jump = "移動",
    jumpTitle = "位置を指定して移動",
    positionInput = "項目の位置",
    invalidPosition = "有効な位置を入力してください。",
    jumpLoading = { "$it 件目の周辺を読み込み中…" },
    jumpFailed = "指定位置を読み込めませんでした。",
    close = "閉じる",
    clearFilters = "フィルターを解除",
    searchNotes = "ノートを検索",
    clearSearchText = "検索文字列を消去",
    applySearch = "検索",
    clearAllSearchAndFilters = "検索とフィルターを解除",
    filterSearch = "タグを検索",
    fuzzySearch = "あいまい検索",
    fuzzySearchTerm = "あいまい (~word)",
    exactWordTerm = "単語一致 (=word)",
    loadingTags = "タグを読み込み中…",
    noTags = "一致するタグがありません",
    includeAny = "いずれかを含む",
    includeAll = "すべてを含む",
    exclude = "除外",
    inactive = "未選択",
    created = "作成日",
    published = "公開日",
    ascending = "昇順",
    descending = "降順",
    image = "画像",
    video = "動画",
    missingMedia = "メディアファイルを利用できません",
    counts = { media, memo, related -> "画像 $media · メモ $memo · 関連 $related" },
    error = ::japaneseError,
    count = { first, last, total ->
        if (last == 0L) "0 / $total 件" else "$first〜$last / $total 件"
    },
    totalCount = { "$it 件" },
    optionCount = { "$it" },
    scrollbarPosition = { position, total -> "$position / $total" },
    virtualLabel = ::japaneseVirtualLabel,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    stateHolder: GalleryStateHolder,
    onEntrySelected: (GalleryEntry) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    strings: GalleryStrings = EnglishGalleryStrings,
    showTilePosition: Boolean = false,
    showLoadedRange: Boolean = true,
    showCounts: Boolean = true,
    showMissingMediaIcon: Boolean = false,
    fixedColumnCount: Int = 0,
    gridState: LazyGridState = rememberLazyGridState(),
    vaultName: String? = null,
    includedTagPrefixes: List<String> = GalleryTagDisplayPrefixesCodec.DEFAULT_INCLUDED,
    hiddenTagPrefixes: List<String> = GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN,
    tagColorRules: List<GalleryTagColorRule> = GalleryTagColorCodec.DEFAULT_RULES,
    galleryThumbnail: @Composable (MediaId, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onRescan: (suspend () -> RepositoryResult<*>)? = null,
    rescanOnStart: Boolean = false,
) {
    val state by stateHolder.state.collectAsState()
    val visibleGridIndices by remember(gridState) {
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo.mapTo(mutableSetOf()) { it.index }
        }
    }
    val galleryCoroutineScope = rememberCoroutineScope()
    var showFilters by remember(stateHolder) { mutableStateOf(false) }
    val filterSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded,
        ),
    )
    var contentMenuExpanded by remember(stateHolder) { mutableStateOf(false) }
    var tagFilterSearch by remember(stateHolder) { mutableStateOf("") }
    var expandedCategories by remember(stateHolder) { mutableStateOf(emptySet<String>()) }
    var expandedBeforeTagSearch by remember(stateHolder) { mutableStateOf<Set<String>?>(null) }
    var wasSearchingTags by remember(stateHolder) { mutableStateOf(false) }
    var noteSearchDraft by remember(stateHolder, showFilters) {
        mutableStateOf(state.query.searchText)
    }
    var showJumpDialog by remember(stateHolder) { mutableStateOf(false) }
    var positionDraft by remember(stateHolder) { mutableStateOf("") }
    var positionError by remember(stateHolder) { mutableStateOf(false) }
    var pendingJumpOffset by remember(stateHolder) { mutableStateOf<Long?>(null) }
    var jumpProgressVisible by remember(stateHolder) { mutableStateOf(false) }
    var highlightedJumpOffset by remember(stateHolder) { mutableStateOf<Long?>(null) }
    var jumpHighlightRevision by remember(stateHolder) { mutableIntStateOf(0) }
    var showScrollbar by remember(gridState) { mutableStateOf(false) }
    var draggingScrollbar by remember(gridState) { mutableStateOf(false) }
    val scrollbarInteractionSource = remember(gridState) { MutableInteractionSource() }
    val hoveringScrollbar by scrollbarInteractionSource.collectIsHoveredAsState()
    var scrollbarDragFraction by remember(gridState) { mutableStateOf<Float?>(null) }
    val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showScrollbar) 1f else 0f,
        animationSpec = tween(180),
        label = "galleryScrollbarAlpha",
    )
    var visibleItemIndex by remember(stateHolder) { mutableStateOf(0) }
    var previousQuery by remember(stateHolder) { mutableStateOf(state.query) }
    val jumpFocusRequester = remember { FocusRequester() }
    fun applyNoteSearch() {
        stateHolder.setQuery(state.query.copy(searchText = noteSearchDraft))
        galleryCoroutineScope.launch {
            filterSheetState.hide()
            showFilters = false
        }
    }
    LaunchedEffect(tagFilterSearch) {
        val searching = tagFilterSearch.isNotBlank()
        if (searching && !wasSearchingTags) {
            expandedBeforeTagSearch = expandedCategories
        } else if (!searching && wasSearchingTags) {
            expandedCategories = expandedBeforeTagSearch.orEmpty()
            expandedBeforeTagSearch = null
        }
        wasSearchingTags = searching
    }
    LaunchedEffect(stateHolder) {
        stateHolder.start(if (rescanOnStart) onRescan else null)
    }
    LaunchedEffect(state.query) {
        if (previousQuery != state.query) {
            previousQuery = state.query
            pendingJumpOffset = null
            jumpProgressVisible = false
            highlightedJumpOffset = null
            visibleItemIndex = 0
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(showJumpDialog) {
        if (showJumpDialog) jumpFocusRequester.requestFocus()
    }
    LaunchedEffect(gridState, stateHolder) {
        combine(
            stateHolder.state,
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            },
            snapshotFlow { gridState.firstVisibleItemIndex },
        ) { current, lastVisibleIndex, firstVisibleIndex ->
            Triple(current, lastVisibleIndex, firstVisibleIndex)
        }.collect { (current, lastVisibleIndex, firstVisibleIndex) ->
            visibleItemIndex = firstVisibleIndex
            val prefetchFrom = (current.entries.size - PREFETCH_THRESHOLD).coerceAtLeast(0)
            if (
                !current.searching &&
                current.canLoadMore &&
                !current.loading &&
                lastVisibleIndex >= prefetchFrom
            ) {
                stateHolder.loadMore()
            }
            if (
                pendingJumpOffset == null &&
                !jumpProgressVisible &&
                !current.searching &&
                current.pageOffset > 0 &&
                !current.loading &&
                firstVisibleIndex <= PREFETCH_THRESHOLD
            ) {
                stateHolder.loadPrevious()
            }
        }
    }
    LaunchedEffect(gridState, draggingScrollbar, hoveringScrollbar) {
        if (draggingScrollbar || hoveringScrollbar) {
            showScrollbar = true
        } else {
            snapshotFlow { gridState.isScrollInProgress }.collectLatest { scrolling ->
                if (scrolling) {
                    showScrollbar = true
                } else {
                    delay(GALLERY_SCROLLBAR_HIDE_DELAY_MILLIS)
                    showScrollbar = false
                }
            }
        }
    }
    LaunchedEffect(state.pageOffset, state.loading, state.entries.size, pendingJumpOffset) {
        val targetOffset = pendingJumpOffset
        if (targetOffset != null && !state.loading) {
            when {
                state.error != null -> pendingJumpOffset = null
                state.entries.isNotEmpty() &&
                    targetOffset >= state.pageOffset &&
                    targetOffset < state.pageOffset + state.entries.size -> {
                    gridState.animateScrollToItem((targetOffset - state.pageOffset).toInt())
                    pendingJumpOffset = null
                    jumpProgressVisible = false
                    highlightedJumpOffset = targetOffset
                    jumpHighlightRevision++
                    delay(JUMP_HIGHLIGHT_MILLIS)
                    if (highlightedJumpOffset == targetOffset) {
                        highlightedJumpOffset = null
                    }
                }
            }
        }
    }
    val visibleRange = galleryVisibleRange(
        pageOffset = state.pageOffset,
        entryCount = state.entries.size,
        totalCount = state.totalCount ?: 0,
        visibleItemIndex = visibleItemIndex,
        pageSize = state.query.pageSize,
    )

    val galleryScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val galleryTopBarColor = if (
        gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
    ) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(galleryScrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = galleryScrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = galleryTopBarColor,
                        scrolledContainerColor = galleryTopBarColor,
                    ),
                    title = {
                        Text(
                            text = vaultName ?: strings.title,
                            fontWeight = if (vaultName != null) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = {
                        Text(
                            text = state.totalCount?.let { total ->
                                if (showLoadedRange) {
                                    visibleRange?.let { range ->
                                        strings.count(range.first, range.last, total)
                                    } ?: strings.totalCount(total)
                                } else {
                                    strings.totalCount(total)
                                }
                            } ?: strings.loading,
                            maxLines = 1,
                        )
                    },
                    actions = {
                        val activeCount = state.query.includeTags.size +
                            state.query.andTags.size +
                            state.query.excludedTags.size +
                            state.query.virtualFilters.size +
                            (if (state.query.searchText.isBlank()) 0 else 1)
                        BadgedBox(
                            badge = {
                                if (activeCount > 0) {
                                    GalleryNumberBadge { Text(activeCount.toString()) }
                                }
                            },
                        ) {
                            KaedeIconButton(
                                label = strings.filters,
                                onClick = { showFilters = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            ) {
                                Icon(Icons.Filled.FilterList, strings.filters)
                            }
                        }
                        KaedeIconButton(
                            label = strings.jump,
                            onClick = {
                                positionDraft = ""
                                positionError = false
                                showJumpDialog = true
                            },
                            enabled = !state.loading && !state.searching &&
                                (state.totalCount ?: 0L) > 0,
                            colors = IconButtonDefaults.iconButtonColors(
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(Icons.Filled.LocationSearching, strings.jump)
                        }
                        Box {
                            KaedeIconButton(
                                label = if (state.query.content == GalleryContent.NOTES) {
                                    strings.notes
                                } else {
                                    strings.media
                                },
                                onClick = { contentMenuExpanded = true },
                            ) {
                                Icon(
                                    imageVector = if (state.query.content == GalleryContent.NOTES) {
                                        Icons.AutoMirrored.Filled.StickyNote2
                                    } else {
                                        Icons.Filled.Image
                                    },
                                    contentDescription = if (
                                        state.query.content == GalleryContent.NOTES
                                    ) {
                                        strings.notes
                                    } else {
                                        strings.media
                                    },
                                )
                            }
                            KaedeMenu(
                                expanded = contentMenuExpanded,
                                onDismissRequest = { contentMenuExpanded = false },
                            ) {
                                KaedeSelectableMenuItem(
                                    text = strings.notes,
                                    selected = state.query.content == GalleryContent.NOTES,
                                    index = 0,
                                    count = 2,
                                    leadingIcon = Icons.AutoMirrored.Filled.StickyNote2,
                                    onClick = {
                                        contentMenuExpanded = false
                                        stateHolder.setQuery(
                                            state.query.copy(content = GalleryContent.NOTES),
                                        )
                                    },
                                )
                                KaedeSelectableMenuItem(
                                    text = strings.media,
                                    selected = state.query.content == GalleryContent.MEDIA,
                                    index = 1,
                                    count = 2,
                                    leadingIcon = Icons.Filled.Image,
                                    onClick = {
                                        contentMenuExpanded = false
                                        stateHolder.setQuery(
                                            state.query.copy(content = GalleryContent.MEDIA),
                                        )
                                    },
                                )
                            }
                        }
                        KaedeIconButton(
                            label = strings.reload,
                            onClick = {
                                val rescan = onRescan
                                if (rescan == null) {
                                    stateHolder.refresh()
                                } else {
                                    stateHolder.rescan(rescan)
                                }
                            },
                            enabled = !state.loading && !state.rescanning,
                            colors = IconButtonDefaults.iconButtonColors(
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(Icons.Filled.Refresh, strings.reload)
                        }
                        KaedeIconButton(label = strings.settings, onClick = onSettings) {
                            Icon(Icons.Filled.Settings, strings.settings)
                        }
                    },
                )
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
        ) {
            if (jumpProgressVisible) {
                val targetOffset = pendingJumpOffset
                AlertDialog(
                    onDismissRequest = {
                        if (!state.loading) jumpProgressVisible = false
                    },
                    title = { Text(strings.jumpTitle) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.loading) {
                                LoadingIndicator(
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Text(
                                if (state.error == null && state.loading) {
                                    strings.jumpLoading((targetOffset ?: 0) + 1)
                                } else {
                                    strings.jumpFailed
                                },
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                if (state.loading) {
                                    stateHolder.cancelJump()
                                    pendingJumpOffset = null
                                } else {
                                    stateHolder.dismissJumpError()
                                }
                                jumpProgressVisible = false
                            },
                        ) {
                            Text(if (state.loading) strings.cancel else strings.close)
                        }
                    },
                )
            }
            val error = state.error
            error?.let {
                Text(
                    text = strings.error(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            state.rescanError?.let {
                Text(
                    text = strings.error(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            AnimatedVisibility(visible = state.searching || state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.entries.isEmpty() && !state.loading) {
                Column(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = error?.let(strings.error) ?: strings.noItems)
                    if (error != null) {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = stateHolder::refresh) {
                            Text(strings.retry)
                        }
                    }

                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    val galleryWidthDp = maxWidth.value
                    val columnCount = if (fixedColumnCount == 0) {
                        galleryAdaptiveColumnCount(galleryWidthDp)
                    } else {
                        fixedColumnCount
                    }
                    Box(Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                items = state.entries,
                                key = { _, entry -> entry.stableKey() },
                            ) { index, entry ->
                                GalleryTile(
                                    entry = entry,
                                    strings = strings,
                                    position = state.pageOffset + index + 1L,
                                    showPosition = showTilePosition,
                                    showCounts = showCounts,
                                    showMissingMediaIcon = showMissingMediaIcon,
                                    galleryThumbnail = galleryThumbnail,
                                    isVisible = index in visibleGridIndices,
                                    highlighted = highlightedJumpOffset == state.pageOffset + index,
                                    highlightRevision = jumpHighlightRevision,
                                    onClick = { onEntrySelected(entry) },
                                )
                            }
                            if (state.error != null && state.entries.isNotEmpty()) {
                                item(key = "load-more") {
                                    TextButton(
                                        shapes = ButtonDefaults.shapes(),
                                        onClick = stateHolder::loadMore,
                                        enabled = !state.loading,
                                    ) {
                                        Text(strings.retry)
                                    }
                                }
                            }
                        }
                        val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo.filter {
                            it.index < state.entries.size
                        }
                        val visibleRows = visibleItemsInfo.map { it.offset.y }.distinct().size
                        val totalItems = state.totalCount ?: 0L
                        val totalRows = if (totalItems == 0L) {
                            0L
                        } else {
                            (totalItems - 1) / columnCount + 1
                        }
                        if (totalRows > visibleRows && visibleRows > 0 && !state.searching) {
                            BoxWithConstraints(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(160.dp)
                                    .zIndex(1f),
                            ) {
                                val trackHeight = (maxHeight - 24.dp).coerceAtLeast(32.dp)
                                val thumbHeight = (trackHeight *
                                    (visibleRows.toDouble() / totalRows.toDouble()).toFloat())
                                    .coerceIn(32.dp, trackHeight)
                                val lastPosition = (totalRows - visibleRows).coerceAtLeast(1L) *
                                    columnCount
                                val currentPosition = (state.pageOffset +
                                    gridState.firstVisibleItemIndex).coerceAtLeast(0L)
                                val firstVisibleItem = visibleItemsInfo.firstOrNull()
                                val withinRowFraction = firstVisibleItem?.size?.height
                                    ?.takeIf { it > 0 }
                                    ?.let {
                                        gridState.firstVisibleItemScrollOffset.toFloat() / it
                                    } ?: 0f
                                val targetFraction = galleryScrollbarFraction(
                                    currentPosition = currentPosition,
                                    lastPosition = lastPosition,
                                    scrollOffsetFraction = withinRowFraction,
                                    itemsPerRow = columnCount,
                                )
                                val animatedFraction by animateFloatAsState(
                                    targetValue = targetFraction,
                                    animationSpec = spring(stiffness = 700f),
                                    label = "galleryScrollbarPosition",
                                )
                                val fraction = scrollbarDragFraction ?: animatedFraction
                                val thumbOffset = 12.dp + (trackHeight - thumbHeight) * fraction
                                Box(
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(12.dp)
                                        .padding(vertical = 12.dp)
                                        .testTag("gallery-scrollbar-track")
                                        .hoverable(scrollbarInteractionSource)
                                        .pointerInput(
                                            totalItems,
                                            visibleRows,
                                            columnCount,
                                            state.loading,
                                            state.searching,
                                        ) {
                                            if (state.loading || state.searching) {
                                                return@pointerInput
                                            }
                                            fun fractionAt(y: Float): Float =
                                                (y / size.height).coerceIn(0f, 1f)

                                            try {
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(
                                                        requireUnconsumed = false,
                                                        pass = PointerEventPass.Initial,
                                                    )
                                                    draggingScrollbar = true
                                                    scrollbarDragFraction =
                                                        fractionAt(down.position.y)
                                                    down.consume()
                                                    var pressed = true
                                                    while (pressed) {
                                                        val event = awaitPointerEvent(
                                                            PointerEventPass.Initial,
                                                        )
                                                        val change = event.changes.firstOrNull {
                                                            it.id == down.id
                                                        } ?: break
                                                        scrollbarDragFraction =
                                                            fractionAt(change.position.y)
                                                        change.consume()
                                                        pressed = change.pressed
                                                    }
                                                    scrollbarDragFraction?.let { dragFraction ->
                                                        val target = galleryScrollbarPosition(
                                                            dragFraction,
                                                            totalItems,
                                                            visibleRows,
                                                            columnCount,
                                                            visibleRows,
                                                        )
                                                        pendingJumpOffset = target
                                                        jumpHighlightRevision++
                                                        stateHolder.jumpTo(target + 1)
                                                    }
                                                    draggingScrollbar = false
                                                    scrollbarDragFraction = null
                                                }
                                            } finally {
                                                draggingScrollbar = false
                                                scrollbarDragFraction = null
                                            }
                                        },
                                )
                                Box(
                                    Modifier.align(Alignment.TopEnd)
                                        .offset(y = thumbOffset)
                                        .width(4.dp)
                                        .height(thumbHeight)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.72f * scrollbarAlpha,
                                            ),
                                            CircleShape,
                                        ),
                                )
                                if (draggingScrollbar || hoveringScrollbar) {
                                    val shownFraction =
                                        scrollbarDragFraction ?: fraction
                                    val targetPosition =
                                        galleryScrollbarPosition(
                                            shownFraction,
                                            totalItems,
                                            visibleRows,
                                            columnCount,
                                            visibleRows,
                                        ) + 1L
                                    Surface(
                                        modifier = Modifier
                                            .zIndex(2f)
                                            .align(Alignment.TopEnd)
                                            .offset(
                                                x = (-20).dp,
                                                y = thumbOffset + thumbHeight / 2 - 20.dp,
                                            )
                                            .testTag("gallery-scrollbar-position"),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        tonalElevation = 3.dp,
                                    ) {
                                        Text(
                                            strings.scrollbarPosition(
                                                targetPosition.coerceAtMost(
                                                    state.totalCount ?: targetPosition,
                                                ),
                                                totalItems,
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
            }
        }
    }
    if (showFilters) {
        ModalBottomSheet(
            modifier = Modifier.testTag("GalleryFilterSheet"),
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState,
            sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings.filters, style = MaterialTheme.typography.titleLarge)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("GalleryFilterSortSurface"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConnectedChoiceGroup(
                            options = listOf(
                                GallerySortField.CREATED,
                                GallerySortField.PUBLISHED,
                            ),
                            selected = state.query.sortField,
                            label = {
                                if (it == GallerySortField.CREATED) strings.created
                                else strings.published
                            },
                            onSelect = {
                                stateHolder.setQuery(state.query.copy(sortField = it))
                            },
                        )
                        ConnectedChoiceGroup(
                            options = listOf(
                                SortDirection.DESCENDING,
                                SortDirection.ASCENDING,
                            ),
                            selected = state.query.sortDirection,
                            label = {
                                if (it == SortDirection.DESCENDING) strings.descending
                                else strings.ascending
                            },
                            onSelect = {
                                stateHolder.setQuery(state.query.copy(sortDirection = it))
                            },
                        )
                    }
                }
                SegmentedListItem(
                    onClick = {
                        stateHolder.setQuery(
                            state.query.copy(
                                fuzzySearchEnabled = !state.query.fuzzySearchEnabled,
                            ),
                            debounceSearch = true,
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    trailingContent = {
                        Switch(
                            checked = state.query.fuzzySearchEnabled,
                            onCheckedChange = null,
                            thumbContent = if (state.query.fuzzySearchEnabled) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Text(strings.fuzzySearch)
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = noteSearchDraft,
                        onValueChange = { noteSearchDraft = it },
                        placeholder = { Text(strings.searchNotes) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { applyNoteSearch() },
                        ),
                        shape = CircleShape,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    KaedeTooltip(strings.applySearch) {
                        FilledTonalIconButton(
                            modifier = Modifier.size(56.dp),
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            onClick = { applyNoteSearch() },
                        ) { Icon(Icons.Filled.Search, strings.applySearch, Modifier.size(28.dp)) }
                    }
                    KaedeTooltip(strings.clearAllSearchAndFilters) {
                        FilledTonalIconButton(
                            modifier = Modifier.size(56.dp),
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            onClick = {
                                noteSearchDraft = ""
                                tagFilterSearch = ""
                                stateHolder.setQuery(
                                    state.query.copy(
                                        searchText = "",
                                        includeTags = emptySet(),
                                        andTags = emptySet(),
                                        excludedTags = emptySet(),
                                        virtualFilters = emptySet(),
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                strings.clearAllSearchAndFilters,
                                Modifier.size(28.dp),
                            )
                        }
                    }
                }
                TextField(
                    value = tagFilterSearch,
                    onValueChange = { tagFilterSearch = it },
                    placeholder = { Text(strings.filterSearch) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                    },
                    shape = CircleShape,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(
                        Icons.Filled.Add to "${strings.includeAny} (#tag)",
                        Icons.Filled.DoneAll to "${strings.includeAll} (+#tag)",
                        Icons.Filled.Remove to "${strings.exclude} (-#tag)",
                        Icons.Filled.Search to strings.fuzzySearchTerm,
                        Icons.Filled.Check to strings.exactWordTerm,
                    ).forEach { (icon, label) ->
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.categoriesError?.let {
                    Text(
                        strings.error(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (state.categoriesLoading && state.categories.isEmpty()) {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    )
                } else if (state.categories.isEmpty()) {
                    Text(strings.noTags, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        state.categories.forEach { category ->
                            val options = category.options.filter { option ->
                                val visible = option.virtualFilter != null ||
                                    isGalleryTagDisplayed(
                                        option.fullTag,
                                        includedTagPrefixes,
                                        hiddenTagPrefixes,
                                    )
                                val label = option.virtualFilter?.let { strings.virtualLabel(it) }
                                    ?: option.name
                                visible && (
                                    tagFilterSearch.isBlank() ||
                                        tagOptionMatches(
                                            category = category.displayName,
                                            name = label,
                                            fullTag = option.fullTag,
                                            query = tagFilterSearch,
                                            fuzzySearchEnabled = state.query.fuzzySearchEnabled,
                                        )
                                    )
                            }
                            if (options.isNotEmpty()) {
                                val expanded = tagFilterSearch.isNotBlank() ||
                                    category.path in expandedCategories
                                item(key = "category:${category.path}") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .testTag("GalleryFilterCategorySurface"),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ) {
                                        Column {
                                            SegmentedListItem(
                                                onClick = {
                                                    if (tagFilterSearch.isBlank()) {
                                                        expandedCategories = if (expanded) {
                                                            expandedCategories - category.path
                                                        } else {
                                                            expandedCategories + category.path
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shapes = ListItemDefaults.segmentedShapes(0, 1),
                                                colors = ListItemDefaults.segmentedColors(
                                                    containerColor = Color.Transparent,
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                trailingContent = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        FilterCountBadge(strings.optionCount(category.count))
                                                        Icon(
                                                            imageVector = if (expanded) {
                                                                Icons.Filled.ExpandLess
                                                            } else {
                                                                Icons.Filled.ExpandMore
                                                            },
                                                            contentDescription = null,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Text(
                                                    category.displayName,
                                                    modifier = Modifier.padding(start = 16.dp),
                                                )
                                            }
                                            if (expanded) {
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(horizontal = 16.dp)
                                                        .padding(top = 8.dp, bottom = 12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    options.forEach { option ->
                                                        val virtual = option.virtualFilter
                                                        val selectedMode = when {
                                                            virtual != null &&
                                                                virtual in state.query.virtualFilters ->
                                                                strings.includeAny
                                                            virtual != null -> strings.inactive
                                                            option.fullTag in state.query.includeTags ->
                                                                strings.includeAny
                                                            option.fullTag in state.query.andTags ->
                                                                strings.includeAll
                                                            option.fullTag in state.query.excludedTags ->
                                                                strings.exclude
                                                            else -> strings.inactive
                                                        }
                                                        FilterOption(
                                                            option = option,
                                                            label = virtual?.let {
                                                                strings.virtualLabel(it)
                                                            } ?: option.name,
                                                            selectedMode = selectedMode,
                                                            color = Color(
                                                                GalleryTagColorCodec.colorFor(
                                                                    option.fullTag,
                                                                    tagColorRules,
                                                                ),
                                                            ),
                                                            strings = strings,
                                                            onClick = {
                                                                if (virtual == null) {
                                                                    stateHolder.cycleTagSelection(
                                                                        option.fullTag,
                                                                    )
                                                                } else {
                                                                    stateHolder.setVirtualFilter(
                                                                        virtual,
                                                                        virtual !in state.query.virtualFilters,
                                                                    )
                                                                }
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text(strings.jumpTitle) },
            text = {
                Column {
                    TextField(
                        value = positionDraft,
                        onValueChange = {
                            positionDraft = it.filter { char -> char in '0'..'9' }
                            positionError = false
                        },
                        label = { Text(strings.positionInput) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        isError = positionError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.focusRequester(jumpFocusRequester),
                    )
                    if (positionError) {
                        Text(
                            strings.invalidPosition,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        val position = positionDraft.toLongOrNull()
                        val total = state.totalCount
                        if (
                            position == null ||
                            position < 1 ||
                            (total != null && position > total)
                        ) {
                            positionError = true
                        } else {
                            pendingJumpOffset = position - 1
                            jumpHighlightRevision++
                            jumpProgressVisible = true
                            showJumpDialog = false
                            stateHolder.jumpTo(position)
                        }
                    },
                    enabled = !state.loading && !state.searching,
                ) { Text(strings.jump) }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { showJumpDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GalleryLoadingScreen(
    vaultName: String? = null,
    strings: GalleryStrings = EnglishGalleryStrings,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(vaultName ?: strings.title) }) },
    ) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
    }
}

private const val PREFETCH_THRESHOLD = 4
private const val JUMP_HIGHLIGHT_MILLIS = 900L

internal fun galleryScrollbarFraction(currentPosition: Long, lastPosition: Long): Float =
    galleryScrollbarFraction(currentPosition, lastPosition, 0f, 1)

internal fun galleryScrollbarFraction(
    currentPosition: Long,
    lastPosition: Long,
    scrollOffsetFraction: Float,
    itemsPerRow: Int,
): Float =
    ((currentPosition.toDouble() +
        scrollOffsetFraction.coerceIn(0f, 1f).toDouble() * itemsPerRow.coerceAtLeast(1)) /
        lastPosition.coerceAtLeast(1L).toDouble())
        .toFloat()
        .coerceIn(0f, 1f)

internal fun galleryScrollbarPosition(
    fraction: Float,
    totalItems: Long,
    visibleItems: Int,
    itemsPerRow: Int = 1,
    visibleRows: Int = visibleItems,
): Long {
    val columnCount = itemsPerRow.coerceAtLeast(1)
    val totalRows = if (totalItems == 0L) {
        0L
    } else {
        (totalItems - 1) / columnCount + 1
    }
    val lastRow = (totalRows - visibleRows.coerceAtLeast(0)).coerceAtLeast(0L)
    val maximumPosition = (totalItems - 1).coerceAtLeast(0L)
    val lastPosition = if (lastRow > maximumPosition / columnCount) {
        maximumPosition
    } else {
        lastRow * columnCount
    }
    return (
        fraction.coerceIn(0f, 1f).toDouble() * lastPosition.toDouble()
        ).toLong()
}

@Composable
private fun FilterOption(
    option: GalleryCategoryOption,
    label: String,
    selectedMode: String,
    color: Color,
    strings: GalleryStrings,
    onClick: () -> Unit,
) {
    val selected = selectedMode != strings.inactive
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = !option.disabled || selected,
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = when (selectedMode) {
                        strings.includeAny -> Icons.Filled.Check
                        strings.includeAll -> Icons.Filled.DoneAll
                        strings.exclude -> Icons.Filled.Close
                        else -> Icons.Filled.Check
                    },
                    contentDescription = selectedMode,
                )
            } else {
                Box(Modifier.size(10.dp).background(color, CircleShape))
            }
        },
        label = { Text(label) },
        trailingIcon = { FilterCountBadge(strings.optionCount(option.count)) },
    )
}

@Composable
private fun FilterCountBadge(count: String) {
    GalleryNumberBadge { Text(count) }
}

@Composable
private fun GalleryNumberBadge(content: @Composable RowScope.() -> Unit) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        content = content,
    )
}

@Composable
private fun GalleryTile(
    modifier: Modifier = Modifier,
    entry: GalleryEntry,
    strings: GalleryStrings,
    position: Long,
    showPosition: Boolean,
    showCounts: Boolean,
    showMissingMediaIcon: Boolean,
    galleryThumbnail: @Composable (MediaId, Boolean, Boolean) -> Unit,
    isVisible: Boolean,
    highlighted: Boolean,
    highlightRevision: Int,
    onClick: () -> Unit,
) {
    val blinkAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlighted, highlightRevision) {
        if (highlighted) {
            blinkAlpha.snapTo(0f)
            repeat(2) {
                blinkAlpha.animateTo(1f, tween(durationMillis = 180))
                blinkAlpha.animateTo(0f, tween(durationMillis = 180))
            }
        } else {
            blinkAlpha.snapTo(0f)
        }
    }
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = modifier
            .animateContentSize(tween(220))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = blinkAlpha.value),
                shape = shape,
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.primaryContainer,
                blinkAlpha.value,
            ),
        ),
        shape = shape,
    ) {
        val (mediaId, isVideo, mediaCount, memoCount, relatedCount, exists) = when (entry) {
            is NoteSummary -> GalleryTileData(
                mediaId = entry.representativeMediaId,
                isVideo = entry.representativeMediaIsVideo,
                mediaCount = entry.mediaCount,
                memoCount = entry.memoCount,
                relatedCount = entry.relatedCount,
                exists = entry.representativeMediaExists,
            )
            is MediaSummary -> GalleryTileData(
                mediaId = entry.id,
                isVideo = entry.isVideo,
                mediaCount = entry.mediaCount,
                memoCount = entry.memoCount,
                relatedCount = entry.relatedCount,
                exists = entry.exists,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            if (mediaId != null && exists) {
                galleryThumbnail(mediaId, isVideo, isVisible)
            } else if (showMissingMediaIcon || mediaId == null) {
                Icon(
                    imageVector = if (isVideo) Icons.Filled.VideoLibrary else Icons.Filled.Image,
                    contentDescription = if (isVideo) strings.video else strings.image,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
            if (showPosition) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = position.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (isVideo) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = strings.video,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
            }
            if (showCounts) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (mediaCount > 1) {
                        GalleryCountBadge(Icons.Filled.Collections, mediaCount, strings.image)
                    }
                    if (memoCount > 0) {
                        GalleryCountBadge(
                            Icons.AutoMirrored.Filled.StickyNote2,
                            memoCount,
                            strings.notes,
                        )
                    }
                    if (relatedCount > 0) {
                        GalleryCountBadge(Icons.Filled.Link, relatedCount, strings.filters)
                    }
                }
            }
        }
    }
}

private data class GalleryTileData(
    val mediaId: MediaId?,
    val isVideo: Boolean,
    val mediaCount: Long,
    val memoCount: Long,
    val relatedCount: Long,
    val exists: Boolean,
)

@Composable
private fun GalleryCountBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Long,
    description: String,
) {
    BadgedBox(badge = { GalleryNumberBadge { Text(count.toString()) } }) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(7.dp).size(18.dp),
            )
        }
    }
}

private fun GalleryEntry.stableKey(): String = when (this) {
    is NoteSummary -> "note:${id.value}"
    is MediaSummary -> "media:${id.value}"
}

internal fun galleryVisibleRange(
    pageOffset: Long,
    entryCount: Int,
    totalCount: Long,
    visibleItemIndex: Int,
    pageSize: Int,
): LongRange? {
    if (pageOffset < 0 || entryCount <= 0 || totalCount < 0 || pageSize <= 0) return null
    val itemIndex = visibleItemIndex.coerceIn(0, entryCount - 1)
    val absoluteIndex = pageOffset + itemIndex
    val pageStart = absoluteIndex / pageSize * pageSize
    val first = pageStart + 1
    val last = minOf(pageStart + pageSize, totalCount)
    return if (first <= last) first..last else null
}

internal fun englishError(error: RepositoryError): String = when (error) {
    RepositoryError.VAULT_UNAVAILABLE -> "Vault unavailable"
    RepositoryError.INVALID_VAULT -> "Invalid Vault"
    RepositoryError.STORAGE_UNAVAILABLE -> "Private storage unavailable"
    RepositoryError.INDEX_UNAVAILABLE -> "Index unavailable"
    RepositoryError.INVALID_REQUEST -> "Invalid request"
    RepositoryError.OPERATION_FAILED -> "Operation failed"
}

internal fun japaneseError(error: RepositoryError): String = when (error) {
    RepositoryError.VAULT_UNAVAILABLE -> "保管庫を利用できません"
    RepositoryError.INVALID_VAULT -> "保管庫が無効です"
    RepositoryError.STORAGE_UNAVAILABLE -> "アプリの保存領域を利用できません"
    RepositoryError.INDEX_UNAVAILABLE -> "索引を利用できません"
    RepositoryError.INVALID_REQUEST -> "無効なリクエストです"
    RepositoryError.OPERATION_FAILED -> "操作に失敗しました"
}

private fun englishVirtualLabel(filter: VirtualFilter): String = when (filter) {
    VirtualFilter.MULTIPLE_MEDIA -> "Multiple media"
    VirtualFilter.HAS_MEMO -> "Has memo"
    VirtualFilter.HAS_VIDEO -> "Has video"
    VirtualFilter.HAS_RELATED -> "Has related"
}

private fun japaneseVirtualLabel(filter: VirtualFilter): String = when (filter) {
    VirtualFilter.MULTIPLE_MEDIA -> "複数メディア"
    VirtualFilter.HAS_MEMO -> "メモあり"
    VirtualFilter.HAS_VIDEO -> "動画あり"
    VirtualFilter.HAS_RELATED -> "関連あり"
}
