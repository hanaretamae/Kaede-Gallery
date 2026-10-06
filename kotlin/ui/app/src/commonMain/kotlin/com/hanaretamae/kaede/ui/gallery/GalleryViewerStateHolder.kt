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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryViewerMediaState(
    val media: MediaSummary,
    val location: String?,
    val locationLoading: Boolean,
)

data class GalleryViewerUiState(
    val note: GalleryNoteDetail? = null,
    val media: List<MediaSummary> = emptyList(),
    val selectedMediaIndex: Int = -1,
    val mediaLocation: String? = null,
    val mediaLocationLoading: Boolean = false,
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
) {
    private val mutableState = MutableStateFlow(GalleryViewerUiState(loading = true))
    val state: StateFlow<GalleryViewerUiState> = mutableState.asStateFlow()

    private var revision = 0L
    private var loadJob: Job? = null
    private var locationJob: Job? = null

    fun start() {
        val noteId = when (entry) {
            is NoteSummary -> entry.id
            is MediaSummary -> entry.noteId
        }
        loadJob?.cancel()
        val currentRevision = ++revision
        mutableState.value = GalleryViewerUiState(loading = true)
        loadJob = scope.launch {
            when (val result = repository.noteDetail(noteId)) {
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
                        )
                        loadMediaLocation(currentRevision, detail.media.getOrNull(selectedIndex)?.id)
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
    }

    private fun selectMedia(index: Int) {
        val current = mutableState.value
        if (index !in current.media.indices || index == current.selectedMediaIndex) return
        locationJob?.cancel()
        mutableState.value = current.copy(
            selectedMediaIndex = index,
            mediaLocation = null,
            mediaLocationLoading = true,
            error = null,
        )
        loadMediaLocation(revision, current.media[index].id)
    }

    private fun loadMediaLocation(currentRevision: Long, mediaId: MediaId?) {
        if (mediaId == null) return
        locationJob?.cancel()
        locationJob = scope.launch {
            when (val result = repository.mediaLocation(mediaId)) {
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
