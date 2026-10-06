package com.hanaretamae.kaede.core.repository

import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteId

interface GalleryRepository {
    suspend fun queryPage(query: GalleryQuery): RepositoryResult<GalleryPage>
    suspend fun categories(query: GalleryQuery): RepositoryResult<List<GalleryCategory>>
    suspend fun noteDetail(
        noteId: NoteId,
        safContent: ByteArray? = null,
    ): RepositoryResult<GalleryNoteDetail?>
    suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?>
    suspend fun thumbnail(mediaId: MediaId, size: Int): RepositoryResult<ByteArray?>
}

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>
}

enum class RepositoryError {
    VAULT_UNAVAILABLE,
    INVALID_VAULT,
    STORAGE_UNAVAILABLE,
    INDEX_UNAVAILABLE,
    INVALID_REQUEST,
    OPERATION_FAILED,
}
