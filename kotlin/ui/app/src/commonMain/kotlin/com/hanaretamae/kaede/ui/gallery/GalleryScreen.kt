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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Collections
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import kotlin.math.floor

private const val GALLERY_GRID_MIN_CELL_WIDTH_DP = 200f
private const val GALLERY_GRID_HORIZONTAL_PADDING_DP = 24f
private const val GALLERY_GRID_SPACING_DP = 12f
private const val GALLERY_GRID_MINIMUM_COLUMN_COUNT = 2

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
    val coroutineScope = rememberCoroutineScope()
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
    var visibleItemIndex by remember(stateHolder) { mutableStateOf(0) }
    var previousQuery by remember(stateHolder) { mutableStateOf(state.query) }
    val jumpFocusRequester = remember { FocusRequester() }
    fun applyNoteSearch() {
        stateHolder.setQuery(state.query.copy(searchText = noteSearchDraft))
        coroutineScope.launch {
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
                current.canLoadMore &&
                !current.loading &&
                lastVisibleIndex >= prefetchFrom
            ) {
                stateHolder.loadMore()
            }
            if (
                pendingJumpOffset == null &&
                !jumpProgressVisible &&
                current.pageOffset > 0 &&
                !current.loading &&
                firstVisibleIndex <= PREFETCH_THRESHOLD
            ) {
                stateHolder.loadPrevious()
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
                    gridState.scrollToItem((targetOffset - state.pageOffset).toInt())
                    pendingJumpOffset = null
                    jumpProgressVisible = false
                    highlightedJumpOffset = targetOffset
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
                                    Badge { Text(activeCount.toString()) }
                                }
                            },
                        ) {
                            KaedeIconButton(label = strings.filters, onClick = { showFilters = true }) {
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
                            enabled = !state.loading && (state.totalCount ?: 0L) > 0,
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
                    LazyVerticalGrid(
                        state = gridState,
                        columns = if (fixedColumnCount == 0) {
                            GridCells.Fixed(galleryAdaptiveColumnCount(maxWidth.value))
                        } else {
                            GridCells.Fixed(fixedColumnCount)
                        },
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
            }
        }
    }
    if (showFilters) {
        val expanded = filterSheetState.currentValue == SheetValue.Expanded ||
            filterSheetState.targetValue == SheetValue.Expanded
        ModalBottomSheet(
            modifier = Modifier.testTag("GalleryFilterSheet"),
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState,
            dragHandle = if (
                filterSheetState.currentValue == SheetValue.Expanded ||
                filterSheetState.targetValue == SheetValue.Expanded
            ) {
                null
            } else {
                {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(width = 32.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    CircleShape,
                                ),
                        )
                    }
                }
            },
            sheetMaxWidth = Dp.Unspecified,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(
                            MaterialTheme.motionScheme.defaultEffectsSpec(),
                        ) + scaleIn(
                            MaterialTheme.motionScheme.defaultSpatialSpec(),
                            initialScale = 0.85f,
                        ),
                        exit = fadeOut(
                            MaterialTheme.motionScheme.defaultEffectsSpec(),
                        ) + scaleOut(
                            MaterialTheme.motionScheme.defaultSpatialSpec(),
                            targetScale = 0.85f,
                        ),
                    ) {
                        KaedeTooltip(strings.close) {
                            FilledTonalIconButton(
                                shapes = IconButtonDefaults.shapes(),
                                onClick = {
                                    coroutineScope.launch {
                                        filterSheetState.hide()
                                        showFilters = false
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Close, strings.close)
                            }
                        }
                    }
                    if (expanded) Spacer(Modifier.width(8.dp))
                    Text(strings.filters, style = MaterialTheme.typography.titleLarge)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("GalleryFilterSortSurface"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    ConnectedChoiceGroup(
                        options = listOf(GallerySortField.CREATED, GallerySortField.PUBLISHED),
                        selected = state.query.sortField,
                        label = { if (it == GallerySortField.CREATED) strings.created else strings.published },
                        onSelect = { stateHolder.setQuery(state.query.copy(sortField = it)) },
                    )
                    ConnectedChoiceGroup(
                        options = listOf(SortDirection.DESCENDING, SortDirection.ASCENDING),
                        selected = state.query.sortDirection,
                        label = { if (it == SortDirection.DESCENDING) strings.descending else strings.ascending },
                        onSelect = { stateHolder.setQuery(state.query.copy(sortDirection = it)) },
                    )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.TextField(
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
                androidx.compose.material3.TextField(
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                state.categoriesError?.let {
                    Text(
                        strings.error(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                if (state.categoriesLoading && state.categories.isEmpty()) {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    )
                } else if (state.categories.isEmpty()) {
                    Text(strings.noTags, modifier = Modifier.padding(vertical = 16.dp))
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
                                        )
                                    )
                            }
                            if (options.isNotEmpty()) {
                                val expanded = tagFilterSearch.isNotBlank() ||
                                    category.path in expandedCategories
                                item(key = "category:${category.path}") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 4.dp),
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
                                                Text(category.displayName)
                                            }
                                            if (expanded) {
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(
                                                            top = 8.dp,
                                                            bottom = 12.dp,
                                                        ),
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
                    OutlinedTextField(
                        value = positionDraft,
                        onValueChange = {
                            positionDraft = it.filter { char -> char in '0'..'9' }
                            positionError = false
                        },
                        label = { Text(strings.positionInput) },
                        singleLine = true,
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
                            position > MAX_GALLERY_POSITION ||
                            (total != null && position > total)
                        ) {
                            positionError = true
                        } else {
                            pendingJumpOffset = position - 1
                            jumpProgressVisible = true
                            showJumpDialog = false
                            stateHolder.jumpTo(position)
                        }
                    },
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
private const val MAX_GALLERY_POSITION = 2_147_483_647L
private const val JUMP_HIGHLIGHT_MILLIS = 900L

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
    Badge(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(count)
    }
}

@Composable
private fun GalleryTile(
    entry: GalleryEntry,
    strings: GalleryStrings,
    position: Long,
    showPosition: Boolean,
    showCounts: Boolean,
    showMissingMediaIcon: Boolean,
    galleryThumbnail: @Composable (MediaId, Boolean, Boolean) -> Unit,
    isVisible: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val blinkAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
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
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = strings.video,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(7.dp).size(18.dp),
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
    BadgedBox(badge = { Badge { Text(count.toString()) } }) {
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
