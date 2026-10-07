package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryUiState(
    val query: GalleryQuery = GalleryQuery(),
    val entries: List<GalleryEntry> = emptyList(),
    val pageOffset: Long = 0,
    val categories: List<GalleryCategory> = emptyList(),
    val totalCount: Long? = null,
    val loading: Boolean = false,
    val error: RepositoryError? = null,
    val categoriesLoading: Boolean = false,
    val categoriesError: RepositoryError? = null,
    val rescanning: Boolean = false,
    val rescanError: RepositoryError? = null,
) {
    val canLoadMore: Boolean
        get() = totalCount?.let { pageOffset + entries.size.toLong() < it } == true
}

class GalleryStateHolder(
    private val repository: GalleryRepository,
    private val scope: CoroutineScope,
    initialQuery: GalleryQuery = GalleryQuery(),
) {
    private val mutableState = MutableStateFlow(GalleryUiState(query = initialQuery.copy(offset = 0)))
    val state: StateFlow<GalleryUiState> = mutableState.asStateFlow()

    private var revision = 0L
    private var requestJob: Job? = null
    private var categoriesJob: Job? = null
    private var searchJob: Job? = null
    private var rescanJob: Job? = null
    private var pendingJumpPageOffset: Long? = null
    private var pendingJumpItemOffset: Long? = null
    private var jumpRestoreState: GalleryUiState? = null

    fun start() {
        refresh()
    }

    fun setQuery(query: GalleryQuery, debounceSearch: Boolean = false) {
        val normalized = query.copy(offset = 0)
        if (normalized == mutableState.value.query) return
        jumpRestoreState = null
        pendingJumpPageOffset = null
        pendingJumpItemOffset = null
        revision++
        requestJob?.cancel()
        categoriesJob?.cancel()
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(
            query = normalized,
            entries = emptyList(),
            pageOffset = 0,
            totalCount = null,
            loading = false,
            error = null,
        )
        requestCategories()
        if (debounceSearch) {
            val currentRevision = revision
            searchJob = scope.launch {
                delay(SEARCH_DEBOUNCE_MILLIS)
                if (revision == currentRevision) requestPage(reset = true)
            }
        } else {
            requestPage(reset = true)
        }
    }

    fun refresh() {
        jumpRestoreState = null
        pendingJumpPageOffset = null
        pendingJumpItemOffset = null
        searchJob?.cancel()
        revision++
        requestJob?.cancel()
        categoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            entries = emptyList(),
            pageOffset = 0,
            totalCount = null,
            loading = false,
            error = null,
        )
        requestCategories()
        requestPage(reset = true)
    }

    fun rescan(operation: suspend () -> RepositoryResult<*>) {
        if (mutableState.value.rescanning) return
        rescanJob?.cancel()
        mutableState.value = mutableState.value.copy(
            rescanning = true,
            rescanError = null,
        )
        rescanJob = scope.launch {
            try {
                when (val result = operation()) {
                    is RepositoryResult.Failure ->
                        mutableState.value = mutableState.value.copy(rescanError = result.error)
                    is RepositoryResult.Success<*> -> {
                        mutableState.value = mutableState.value.copy(rescanError = null)
                        refresh()
                    }
                }
            } finally {
                mutableState.value = mutableState.value.copy(
                    rescanning = false,
                )
            }
        }
    }

    fun loadMore() {
        if (mutableState.value.loading || !mutableState.value.canLoadMore) return
        requestPage(reset = false)
    }

    fun loadPrevious() {
        val current = mutableState.value
        if (current.loading || current.pageOffset == 0L) return
        requestPage(
            reset = false,
            requestedOffset = (current.pageOffset - current.query.pageSize).coerceAtLeast(0),
            prepend = true,
        )
    }

    fun jumpTo(position: Long) {
        if (position !in 1..MAX_GALLERY_POSITION || mutableState.value.loading) return
        val offset = position - 1
        val totalCount = mutableState.value.totalCount
        if (totalCount != null && offset >= totalCount) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return
        }
        jumpRestoreState = mutableState.value
        pendingJumpPageOffset = null
        pendingJumpItemOffset = offset
        revision++
        requestJob?.cancel()
        categoriesJob?.cancel()
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(
            entries = emptyList(),
            totalCount = null,
            loading = false,
            error = null,
        )
        requestCategories()
        val targetPageOffset = offset / mutableState.value.query.pageSize *
            mutableState.value.query.pageSize
        pendingJumpPageOffset = targetPageOffset
        if (targetPageOffset == 0L) {
            requestPage(reset = true, requestedOffset = targetPageOffset)
        } else {
            requestPage(
                reset = true,
                requestedOffset = (targetPageOffset - mutableState.value.query.pageSize)
                    .coerceAtLeast(0),
            )
        }
    }

    fun cancelJump() {
        val restoreState = jumpRestoreState ?: return
        revision++
        requestJob?.cancel()
        categoriesJob?.cancel()
        searchJob?.cancel()
        pendingJumpPageOffset = null
        pendingJumpItemOffset = null
        mutableState.value = restoreState.copy(loading = false)
        jumpRestoreState = null
    }

    fun dismissJumpError() {
        jumpRestoreState = null
    }

    fun cycleTagSelection(tag: String) {
        val query = mutableState.value.query
        val updated = when {
            tag in query.includeTags -> query.copy(
                includeTags = query.includeTags - tag,
                andTags = query.andTags + tag,
            )
            tag in query.andTags -> query.copy(
                andTags = query.andTags - tag,
                excludedTags = query.excludedTags + tag,
            )
            tag in query.excludedTags -> query.copy(excludedTags = query.excludedTags - tag)
            else -> query.copy(includeTags = query.includeTags + tag)
        }
        setQuery(updated)
    }

    fun setVirtualFilter(filter: VirtualFilter, enabled: Boolean) {
        val filters = mutableState.value.query.virtualFilters
        setQuery(
            mutableState.value.query.copy(
                virtualFilters = if (enabled) filters + filter else filters - filter,
            ),
        )
    }

    fun clearFilters() {
        val query = mutableState.value.query
        setQuery(
            query.copy(
                includeTags = emptySet(),
                andTags = emptySet(),
                excludedTags = emptySet(),
                virtualFilters = emptySet(),
            ),
        )
    }

    fun dispose() {
        revision++
        requestJob?.cancel()
        categoriesJob?.cancel()
        searchJob?.cancel()
        rescanJob?.cancel()
    }

    private fun requestPage(
        reset: Boolean,
        requestedOffset: Long? = null,
        prepend: Boolean = false,
    ) {
        if (mutableState.value.loading) return
        val requestRevision = revision
        val current = mutableState.value
        val offset = when {
            requestedOffset != null -> requestedOffset
            reset -> 0L
            else -> current.pageOffset + current.entries.size.toLong()
        }
        val query = current.query.copy(offset = offset)
        mutableState.value = current.copy(loading = true, error = null)
        requestJob = scope.launch {
            val result = repository.queryPage(query)
            if (requestRevision != revision) return@launch
            val latest = mutableState.value
            when (result) {
                is RepositoryResult.Success -> {
                    val page = result.value
                    val targetItemOffset = pendingJumpItemOffset
                    val targetPageOffset = pendingJumpPageOffset
                    val invalidJump = requestedOffset != null && (
                        page.entries.isEmpty() ||
                            (offset == targetPageOffset &&
                                targetItemOffset != null &&
                                targetItemOffset >= offset + page.entries.size)
                        )
                    if (
                        invalidJump ||
                        page.offset != offset ||
                        page.entries.size > query.pageSize ||
                        page.totalCount < page.offset ||
                        page.totalCount - page.offset < page.entries.size ||
                        (page.entries.isEmpty() && page.totalCount > page.offset)
                    ) {
                        pendingJumpPageOffset = null
                        pendingJumpItemOffset = null
                        mutableState.value = latest.copy(
                            loading = false,
                            error = if (invalidJump) {
                                RepositoryError.INVALID_REQUEST
                            } else {
                                RepositoryError.OPERATION_FAILED
                            },
                        )
                    } else {
                        val entries = when {
                            reset -> page.entries
                            prepend -> page.entries + latest.entries
                            else -> latest.entries + page.entries
                        }
                        mutableState.value = latest.copy(
                            entries = entries,
                            pageOffset = if (prepend || reset) offset else latest.pageOffset,
                            totalCount = page.totalCount,
                            loading = false,
                            error = null,
                        )
                        if (targetPageOffset != null && offset != targetPageOffset) {
                            requestPage(
                                reset = false,
                                requestedOffset = targetPageOffset,
                            )
                        } else if (offset == targetPageOffset) {
                            pendingJumpPageOffset = null
                            pendingJumpItemOffset = null
                            jumpRestoreState = null
                        }
                    }
                }
                is RepositoryResult.Failure -> {
                    pendingJumpPageOffset = null
                    pendingJumpItemOffset = null
                    mutableState.value = latest.copy(loading = false, error = result.error)
                }
            }
        }
    }

    private fun requestCategories() {
        val requestRevision = revision
        val query = mutableState.value.query.copy(offset = 0)
        mutableState.value = mutableState.value.copy(
            categoriesLoading = true,
            categoriesError = null,
        )
        categoriesJob = scope.launch {
            val result = repository.categories(query)
            if (requestRevision != revision) return@launch
            val latest = mutableState.value
            when (result) {
                is RepositoryResult.Success -> {
                    mutableState.value = latest.copy(
                        categories = result.value,
                        categoriesLoading = false,
                        categoriesError = null,
                    )
                }
                is RepositoryResult.Failure -> {
                    mutableState.value = latest.copy(
                        categoriesLoading = false,
                        categoriesError = result.error,
                    )
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
        const val MAX_GALLERY_POSITION = 2_147_483_647L
    }
}
