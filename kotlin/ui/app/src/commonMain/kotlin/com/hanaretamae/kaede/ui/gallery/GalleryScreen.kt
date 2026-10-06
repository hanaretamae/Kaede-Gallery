package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.model.GalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GallerySortField
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.launch

data class GalleryStrings(
    val title: String,
    val search: String,
    val notes: String,
    val media: String,
    val loading: String,
    val noItems: String,
    val loadMore: String,
    val retry: String,
    val reload: String,
    val filters: String,
    val settings: String,
    val changeVault: String,
    val forgetVault: String,
    val forgetVaultTitle: String,
    val forgetVaultMessage: String,
    val forgetVaultConfirm: String,
    val cancel: String,
    val clearFilters: String,
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
    val count: (Long, Long) -> String,
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
    loadMore = "Load more",
    retry = "Retry",
    reload = "Reload",
    filters = "Filters",
    settings = "Settings",
    changeVault = "Change Vault",
    forgetVault = "Forget Vault",
    forgetVaultTitle = "Forget selected Vault?",
    forgetVaultMessage = "This removes its local index and thumbnails. Vault files are not changed.",
    forgetVaultConfirm = "Forget",
    cancel = "Cancel",
    clearFilters = "Clear filters",
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
    counts = { media, memo, related -> "$media media · $memo memos · $related links" },
    error = ::englishError,
    count = { loaded, total -> "$loaded of $total items" },
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
    loadMore = "さらに読み込む",
    retry = "再試行",
    reload = "再読み込み",
    filters = "フィルター",
    settings = "設定",
    changeVault = "Vaultを変更",
    forgetVault = "Vaultの登録を解除",
    forgetVaultTitle = "選択中のVaultを登録解除しますか？",
    forgetVaultMessage = "ローカルの索引とサムネイルを削除します。Vault内のファイルは変更しません。",
    forgetVaultConfirm = "登録解除",
    cancel = "キャンセル",
    clearFilters = "フィルターを解除",
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
    counts = { media, memo, related -> "画像 $media · メモ $memo · 関連リンク $related" },
    error = ::japaneseError,
    count = { loaded, total -> "$loaded / $total 件" },
    totalCount = { "$it 件" },
    optionCount = { "$it 件" },
    virtualLabel = ::japaneseVirtualLabel,
)

@OptIn(ExperimentalMaterial3Api::class)
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
    onRescan: (suspend () -> RepositoryResult<*>)? = null,
    onChangeVault: (() -> Unit)? = null,
    onForgetVault: (suspend () -> RepositoryResult<*>)? = null,
) {
    val state by stateHolder.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showFilters by remember(stateHolder) { mutableStateOf(false) }
    var confirmForget by remember(stateHolder) { mutableStateOf(false) }
    var forgetError by remember(stateHolder) { mutableStateOf<RepositoryError?>(null) }
    var tagFilterSearch by remember(stateHolder) { mutableStateOf("") }
    LaunchedEffect(stateHolder) {
        stateHolder.start()
    }
    DisposableEffect(stateHolder) {
        onDispose(stateHolder::dispose)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
        ) {
            Text(
                text = strings.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            OutlinedTextField(
                value = state.query.searchText,
                onValueChange = {
                    stateHolder.setQuery(state.query.copy(searchText = it), debounceSearch = true)
                },
                label = { Text(strings.search) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            PrimaryTabRow(
                selectedTabIndex = if (state.query.content == GalleryContent.NOTES) 0 else 1,
            ) {
                Tab(
                    selected = state.query.content == GalleryContent.NOTES,
                    onClick = {
                        stateHolder.setQuery(state.query.copy(content = GalleryContent.NOTES))
                    },
                    text = { Text(strings.notes) },
                )
                Tab(
                    selected = state.query.content == GalleryContent.MEDIA,
                    onClick = {
                        stateHolder.setQuery(state.query.copy(content = GalleryContent.MEDIA))
                    },
                    text = { Text(strings.media) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val activeCount = state.query.includeTags.size +
                    state.query.andTags.size +
                    state.query.excludedTags.size +
                    state.query.virtualFilters.size
                TextButton(onClick = { showFilters = true }) {
                    Text(if (activeCount == 0) strings.filters else "${strings.filters} ($activeCount)")
                }
                TextButton(
                    onClick = {
                        val field = if (state.query.sortField == GallerySortField.CREATED) {
                            GallerySortField.PUBLISHED
                        } else {
                            GallerySortField.CREATED
                        }
                        stateHolder.setQuery(state.query.copy(sortField = field))
                    },
                ) {
                    Text(
                        if (state.query.sortField == GallerySortField.CREATED) {
                            strings.created
                        } else {
                            strings.published
                        },
                    )
                }
                TextButton(
                    onClick = {
                        val direction = if (state.query.sortDirection == SortDirection.ASCENDING) {
                            SortDirection.DESCENDING
                        } else {
                            SortDirection.ASCENDING
                        }
                        stateHolder.setQuery(state.query.copy(sortDirection = direction))
                    },
                ) {
                    Text(
                        if (state.query.sortDirection == SortDirection.ASCENDING) {
                            strings.ascending
                        } else {
                            strings.descending
                        },
                    )
                }
                TextButton(
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
                    Text(strings.reload)
                }
                if (onChangeVault != null) {
                    TextButton(onClick = onChangeVault) { Text(strings.changeVault) }
                }
                if (onForgetVault != null) {
                    TextButton(onClick = { confirmForget = true }) {
                        Text(strings.forgetVault)
                    }
                }
                TextButton(onClick = onSettings) { Text(strings.settings) }
            }
            forgetError?.let { error ->
                Text(
                    strings.error(error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.totalCount?.let {
                        if (showLoadedRange) {
                            strings.count(state.entries.size.toLong(), it)
                        } else {
                            strings.totalCount(it)
                        }
                    } ?: strings.loading,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
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
                        TextButton(onClick = stateHolder::refresh) {
                            Text(strings.retry)
                        }
                    }

                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 176.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                            position = index + 1,
                            showPosition = showTilePosition,
                            showCounts = showCounts,
                            onClick = { onEntrySelected(entry) },
                        )
                    }
                    if (state.canLoadMore || state.error != null) {
                        item(key = "load-more") {
                            TextButton(
                                onClick = stateHolder::loadMore,
                                enabled = !state.loading && state.canLoadMore,
                            ) {
                                Text(strings.loadMore)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings.filters, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = stateHolder::clearFilters) {
                        Text(strings.clearFilters)
                    }
                }
                OutlinedTextField(
                    value = tagFilterSearch,
                    onValueChange = { tagFilterSearch = it },
                    label = { Text(strings.filterSearch) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                state.categoriesError?.let {
                    Text(
                        strings.error(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (state.categoriesLoading && state.categories.isEmpty()) {
                    Text(strings.loadingTags, modifier = Modifier.padding(16.dp))
                } else if (state.categories.isEmpty()) {
                    Text(strings.noTags, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        state.categories.forEach { category ->
                            val options = category.options.filter { option ->
                                val label = option.virtualFilter?.let { strings.virtualLabel(it) }
                                    ?: option.name
                                tagFilterSearch.isBlank() ||
                                    tagOptionMatches(
                                        category = category.displayName,
                                        name = label,
                                        fullTag = option.fullTag,
                                        query = tagFilterSearch,
                                    )
                            }
                            if (options.isNotEmpty()) {
                                item(key = "category:${category.path}") {
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                                    )
                                }
                                options.forEach { option ->
                                    val virtual = option.virtualFilter
                                    val selectedMode = when {
                                        virtual != null && virtual in state.query.virtualFilters ->
                                            strings.includeAny
                                        virtual != null -> strings.inactive
                                        option.fullTag in state.query.includeTags -> strings.includeAny
                                        option.fullTag in state.query.andTags -> strings.includeAll
                                        option.fullTag in state.query.excludedTags -> strings.exclude
                                        else -> strings.inactive
                                    }
                                    item(
                                        key = virtual?.let { "virtual:${it.name}" }
                                            ?: "tag:${option.fullTag}",
                                    ) {
                                        FilterOption(
                                            option = option,
                                            label = virtual?.let { strings.virtualLabel(it) } ?: option.name,
                                            category = option.fullTag.takeIf { virtual == null }
                                                ?: category.displayName,
                                            selectedMode = selectedMode,
                                            optionCount = strings.optionCount(option.count),
                                            strings = strings,
                                            onClick = {
                                                if (virtual == null) {
                                                    stateHolder.cycleTagSelection(option.fullTag)
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
    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text(strings.forgetVaultTitle) },
            text = { Text(strings.forgetVaultMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val forget = onForgetVault ?: return@TextButton
                        confirmForget = false
                        forgetError = null
                        scope.launch {
                            when (val result = forget()) {
                                is RepositoryResult.Failure -> forgetError = result.error
                                is RepositoryResult.Success<*> -> Unit
                            }
                        }
                    },
                ) { Text(strings.forgetVaultConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { confirmForget = false }) { Text(strings.cancel) }
            },
        )
    }
}

@Composable
private fun FilterOption(
    option: GalleryCategoryOption,
    label: String,
    category: String,
    selectedMode: String,
    optionCount: String,
    strings: GalleryStrings,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = !option.disabled || selectedMode != strings.inactive,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Text(optionCount, style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(selectedMode, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GalleryTile(
    entry: GalleryEntry,
    strings: GalleryStrings,
    position: Int,
    showPosition: Boolean,
    showCounts: Boolean,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (showPosition) {
                Surface(
                    modifier = Modifier.padding(bottom = 8.dp),
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
            when (entry) {
                is NoteSummary -> {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text(entry.path, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (showCounts) {
                        Text(strings.counts(entry.mediaCount, entry.memoCount, entry.relatedCount))
                    }
                }
                is MediaSummary -> {
                    Text(
                        text = if (entry.isVideo) strings.video else strings.image,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Media ${entry.id.value}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (showCounts) {
                        Text(strings.counts(entry.mediaCount, entry.memoCount, entry.relatedCount))
                    }
                    if (!entry.exists) {
                        Spacer(Modifier.height(8.dp))
                        Text(strings.missingMedia, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun GalleryEntry.stableKey(): String = when (this) {
    is NoteSummary -> "note:${id.value}"
    is MediaSummary -> "media:${id.value}"
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
    VirtualFilter.HAS_RELATED -> "Has related links"
}

private fun japaneseVirtualLabel(filter: VirtualFilter): String = when (filter) {
    VirtualFilter.MULTIPLE_MEDIA -> "複数メディア"
    VirtualFilter.HAS_MEMO -> "メモあり"
    VirtualFilter.HAS_VIDEO -> "動画あり"
    VirtualFilter.HAS_RELATED -> "関連リンクあり"
}
