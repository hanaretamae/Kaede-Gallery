package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryViewerMediaState(
    val media: MediaSummary,
    val location: String?,
    val locationLoading: Boolean,
    val unavailableLabel: String = "Media unavailable.",
    /** Whether the viewer is in full screen; video players show it on their own control. */
    val fullscreen: Boolean = false,
    val onToggleFullscreen: (() -> Unit)? = null,
    val onPlaybackOverlayVisibilityChanged: ((Boolean) -> Unit)? = null,
    val playbackStrings: GalleryPlaybackStrings = EnglishGalleryPlaybackStrings,
    /** Locations of adjacent media that platforms may decode ahead of time. */
    val prefetchLocations: List<String> = emptyList(),
    /** IDs of adjacent media that platforms may load ahead of time. */
    val prefetchMediaIds: List<MediaId> = emptyList(),
)

data class GalleryViewerUiState(
    val note: GalleryNoteDetail? = null,
    val media: List<MediaSummary> = emptyList(),
    val selectedMediaIndex: Int = -1,
    val mediaLocation: String? = null,
    val mediaLocationLoading: Boolean = false,
    val neighborMediaLocations: List<String> = emptyList(),
    val neighborMediaIds: List<MediaId> = emptyList(),
    val loading: Boolean = false,
    val error: RepositoryError? = null,
) {
    val selectedMedia: MediaSummary?
        get() = media.getOrNull(selectedMediaIndex)
}

class GalleryViewerStateHolder(
    private val repository: GalleryRepository,
    private val scope: CoroutineScope,
    private val entry: GalleryEntry,
    private val repositoryDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow(GalleryViewerUiState(loading = true))
    val state: StateFlow<GalleryViewerUiState> = mutableState.asStateFlow()

    private var revision = 0L
    private var loadJob: Job? = null
    private var locationJob: Job? = null
    private var prefetchJob: Job? = null
    private val locationCache = LinkedHashMap<MediaId, String>()

    fun cachedMediaLocation(mediaId: MediaId): String? = locationCache[mediaId]

    fun start() {
        val noteId = when (entry) {
            is NoteSummary -> entry.id
            is MediaSummary -> entry.noteId
        }
        loadJob?.cancel()
        val currentRevision = ++revision
        mutableState.value = GalleryViewerUiState(loading = true)
        loadJob = scope.launch {
            val earlyLocation = (entry as? MediaSummary)?.let { media ->
                async(repositoryDispatcher) { media.id to repository.mediaLocation(media.id) }
            }
            val detailResult = withContext(repositoryDispatcher) { repository.noteDetail(noteId) }
            when (val result = detailResult) {
                is RepositoryResult.Failure -> {
                    if (currentRevision == revision) {
                        mutableState.value = GalleryViewerUiState(error = result.error)
                    }
                }
                is RepositoryResult.Success -> {
                    if (currentRevision != revision) return@launch
                    val detail = result.value
                    if (detail == null || detail.id != noteId) {
                        mutableState.value = GalleryViewerUiState(
                            error = RepositoryError.OPERATION_FAILED,
                        )
                    } else if (
                        detail.media.any { it.noteId != detail.id } ||
                        detail.media.map { it.id }.toSet().size != detail.media.size
                    ) {
                        mutableState.value = GalleryViewerUiState(
                            error = RepositoryError.OPERATION_FAILED,
                        )
                    } else {
                        val selectedIndex = initialMediaIndex(detail.media)
                        if (selectedIndex == null) {
                            mutableState.value = GalleryViewerUiState(
                                error = RepositoryError.OPERATION_FAILED,
                            )
                            return@launch
                        }
                        mutableState.value = GalleryViewerUiState(
                            note = detail,
                            media = detail.media,
                            selectedMediaIndex = selectedIndex,
                            mediaLocation = detail.media.getOrNull(selectedIndex)?.id?.let(locationCache::get),
                            mediaLocationLoading = selectedIndex >= 0 &&
                                detail.media[selectedIndex].id !in locationCache,
                            neighborMediaIds = neighborIndices(detail.media, selectedIndex)
                                .map { detail.media[it].id },
                        )
                        prefetchNeighbors(currentRevision)
                        earlyLocation?.await()?.let { (id, location) ->
                            if (currentRevision != revision) return@launch
                            if (location is RepositoryResult.Success) {
                                location.value?.let { rememberLocation(id, it) }
                            }
                        }
                        loadMediaLocation(currentRevision, mutableState.value.selectedMedia?.id)
                    }
                }
            }
        }
    }

    fun showPreviousMedia() {
        selectMedia(mutableState.value.selectedMediaIndex - 1)
    }

    fun showNextMedia() {
        selectMedia(mutableState.value.selectedMediaIndex + 1)
    }

    fun dispose() {
        revision++
        loadJob?.cancel()
        locationJob?.cancel()
        prefetchJob?.cancel()
        locationCache.clear()
    }

    private fun selectMedia(index: Int) {
        val current = mutableState.value
        if (index !in current.media.indices || index == current.selectedMediaIndex) return
        locationJob?.cancel()
        val cached = locationCache[current.media[index].id]
        mutableState.value = current.copy(
            selectedMediaIndex = index,
            mediaLocation = cached,
            mediaLocationLoading = cached == null,
            neighborMediaIds = neighborIndices(current.media, index)
                .map { current.media[it].id },
            neighborMediaLocations = emptyList(),
            error = null,
        )
        prefetchNeighbors(revision)
        if (cached == null) {
            loadMediaLocation(revision, current.media[index].id)
        }
    }

    private fun loadMediaLocation(currentRevision: Long, mediaId: MediaId?) {
        if (mediaId == null || currentRevision != revision ||
            mutableState.value.selectedMedia?.id != mediaId
        ) return
        locationJob?.cancel()
        locationCache[mediaId]?.let { cached ->
            mutableState.value = mutableState.value.copy(
                mediaLocation = cached,
                mediaLocationLoading = false,
                error = null,
            )
            return
        }
        locationJob = scope.launch {
            when (val result = withContext(repositoryDispatcher) {
                repository.mediaLocation(mediaId)
            }) {
                is RepositoryResult.Failure -> {
                    if (currentRevision == revision &&
                        mutableState.value.selectedMedia?.id == mediaId
                    ) {
                        mutableState.value = mutableState.value.copy(
                            mediaLocationLoading = false,
                            error = result.error,
                        )
                    }
                }
                is RepositoryResult.Success -> {
                    if (currentRevision == revision &&
                        mutableState.value.selectedMedia?.id == mediaId
                    ) {
                        result.value?.let { rememberLocation(mediaId, it) }
                        mutableState.value = mutableState.value.copy(
                            mediaLocation = result.value,
                            mediaLocationLoading = false,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    private fun rememberLocation(mediaId: MediaId, location: String) {
        locationCache.remove(mediaId)
        locationCache[mediaId] = location
        while (locationCache.size > MAX_CACHED_LOCATIONS) {
            locationCache.remove(locationCache.keys.first())
        }
    }

    private fun neighborLocations(media: List<MediaSummary>, index: Int): List<String> =
        neighborIndices(media, index).mapNotNull { locationCache[media[it].id] }

    private fun neighborIndices(media: List<MediaSummary>, index: Int): List<Int> =
        listOf(index + 1, index - 1, index + 2).filter { it in media.indices && !media[it].isVideo }

    /** Resolves adjacent media locations so switching does not wait on the repository. */
    private fun prefetchNeighbors(currentRevision: Long) {
        prefetchJob?.cancel()
        val snapshot = mutableState.value
        val missing = neighborIndices(snapshot.media, snapshot.selectedMediaIndex)
            .map { snapshot.media[it].id }
            .filterNot(locationCache::containsKey)
        prefetchJob = scope.launch {
            val results = coroutineScope {
                missing.map { mediaId ->
                    async(repositoryDispatcher) {
                        mediaId to repository.mediaLocation(mediaId)
                    }
                }.awaitAll()
            }
            if (currentRevision != revision) return@launch
            results.forEach { (mediaId, result) ->
                if (result is RepositoryResult.Success) {
                    result.value?.let { rememberLocation(mediaId, it) }
                }
            }
            val current = mutableState.value
            mutableState.value = current.copy(
                neighborMediaLocations = neighborLocations(current.media, current.selectedMediaIndex),
            )
        }
    }

    private fun initialMediaIndex(media: List<MediaSummary>): Int? {
        val targetId = when (entry) {
            is NoteSummary -> entry.representativeMediaId
            is MediaSummary -> entry.id
        }
        val found = media.indexOfFirst { it.id == targetId }
        return when {
            media.isEmpty() -> -1
            found >= 0 -> found
            entry is MediaSummary -> null
            else -> 0
        }
    }
}
private const val MAX_CACHED_LOCATIONS = 64
