package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.model.GalleryCategory as ModelGalleryCategory
import com.hanaretamae.kaede.core.model.GalleryCategoryOption as ModelGalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryContent as ModelGalleryContent
import com.hanaretamae.kaede.core.model.GalleryDetailLine as ModelGalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GalleryNoteDetail as ModelGalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage as ModelGalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery as ModelGalleryQuery
import com.hanaretamae.kaede.core.model.GallerySortField as ModelGallerySortField
import com.hanaretamae.kaede.core.model.MediaId as ModelMediaId
import com.hanaretamae.kaede.core.model.MediaSummary as ModelMediaSummary
import com.hanaretamae.kaede.core.model.NoteId as ModelNoteId
import com.hanaretamae.kaede.core.model.NoteSummary as ModelNoteSummary
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter as ModelVirtualFilter
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RustGalleryRepository(
    private val session: GallerySessionInterface,
) : GalleryRepository {
    override suspend fun queryPage(
        query: ModelGalleryQuery,
    ): RepositoryResult<ModelGalleryPage> = withContext(Dispatchers.IO) {
        val page = try {
            session.queryPage(query.toRustQuery())
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }

        val totalCount = page.totalCount.toLongOrNull()
            ?: return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        val offset = page.offset.toLongOrNull()
            ?: return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        val entries: List<GalleryEntry?> = when (query.content) {
            ModelGalleryContent.NOTES -> page.notes.map { it.toModel() }
            ModelGalleryContent.MEDIA -> page.media.map { it.toModel() }
        }
        if (entries.any { it == null }) {
            return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        }
        if (
            offset != query.offset ||
            entries.size > query.pageSize ||
            (query.content == ModelGalleryContent.NOTES && page.media.isNotEmpty()) ||
            (query.content == ModelGalleryContent.MEDIA && page.notes.isNotEmpty())
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        }

        RepositoryResult.Success(
            ModelGalleryPage(
                entries = entries.filterNotNull(),
                totalCount = totalCount,
                offset = offset,
            ),
        )
    }

    override suspend fun categories(
        query: ModelGalleryQuery,
    ): RepositoryResult<List<ModelGalleryCategory>> = withContext(Dispatchers.IO) {
        val categories = try {
            session.categories(query.toRustQuery())
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }
        val mapped = categories.map { it.toModel() }
        if (mapped.any { it == null }) {
            RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        } else {
            RepositoryResult.Success(mapped.filterNotNull())
        }
    }

    override suspend fun noteDetail(
        noteId: ModelNoteId,
        safContent: ByteArray?,
    ): RepositoryResult<ModelGalleryNoteDetail?> = withContext(Dispatchers.IO) {
        if (safContent != null && safContent.size > MAX_SAF_NOTE_BYTES) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        val detail = try {
            if (safContent == null) {
                session.noteDetail(noteId.value)
            } else {
                session.noteDetailSaf(noteId.value, safContent)
            }
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }
        val mapped = detail?.toModel()
        if (detail != null && mapped == null) {
            RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        } else {
            RepositoryResult.Success(mapped)
        }
    }

    override suspend fun mediaLocation(
        mediaId: ModelMediaId,
    ): RepositoryResult<String?> = withContext(Dispatchers.IO) {
        try {
            RepositoryResult.Success(session.mediaLocation(mediaId.value))
        } catch (error: GalleryException) {
            RepositoryResult.Failure(error.toRepositoryError())
        }
    }

    override suspend fun thumbnail(
        mediaId: ModelMediaId,
        size: Int,
    ): RepositoryResult<ByteArray?> = withContext(Dispatchers.IO) {
        if (size !in 1..MAX_THUMBNAIL_SIZE) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        val thumbnail = try {
            session.thumbnail(mediaId.value, size.toUInt())
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }
        if (thumbnail != null && thumbnail.size > MAX_THUMBNAIL_BYTES) {
            RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        } else {
            RepositoryResult.Success(thumbnail)
        }
    }
}

private fun ModelGalleryQuery.toRustQuery() = GalleryQuery(
    content = when (content) {
        ModelGalleryContent.NOTES -> GalleryContent.NOTES
        ModelGalleryContent.MEDIA -> GalleryContent.MEDIA
    },
    searchText = searchText,
    includeTags = includeTags.sorted(),
    andTags = andTags.sorted(),
    excludedTags = excludedTags.sorted(),
    virtualFilters = virtualFilters.map { it.toRustFilter() }.sortedBy { it.ordinal },
    sortField = when (sortField) {
        ModelGallerySortField.PUBLISHED -> GallerySortField.PUBLISHED
        ModelGallerySortField.CREATED -> GallerySortField.CREATED
    },
    sortDirection = when (sortDirection) {
        SortDirection.ASCENDING -> GallerySortDirection.ASCENDING
        SortDirection.DESCENDING -> GallerySortDirection.DESCENDING
    },
    offset = offset.toULong(),
    pageSize = pageSize.toUInt(),
)

private fun ModelVirtualFilter.toRustFilter() = when (this) {
    ModelVirtualFilter.MULTIPLE_MEDIA -> VirtualFilter.MULTIPLE_MEDIA
    ModelVirtualFilter.HAS_MEMO -> VirtualFilter.HAS_MEMO
    ModelVirtualFilter.HAS_VIDEO -> VirtualFilter.HAS_VIDEO
    ModelVirtualFilter.HAS_RELATED -> VirtualFilter.HAS_RELATED
}

private fun NoteSummary.toModel(): ModelNoteSummary? = ModelNoteSummary(
    id = ModelNoteId(id),
    path = path,
    title = title,
    representativeMediaId = representativeMediaId?.let(::ModelMediaId),
    mediaCount = mediaCount.toLongOrNull() ?: return null,
    videoCount = videoCount.toLongOrNull() ?: return null,
    memoCount = memoCount.toLongOrNull() ?: return null,
    relatedCount = relatedCount.toLongOrNull() ?: return null,
)

private fun MediaSummary.toModel(): ModelMediaSummary? = ModelMediaSummary(
    id = ModelMediaId(id),
    noteId = ModelNoteId(noteId),
    notePath = notePath,
    isVideo = isVideo,
    exists = exists,
    mediaCount = mediaCount.toLongOrNull() ?: return null,
    memoCount = memoCount.toLongOrNull() ?: return null,
    relatedCount = relatedCount.toLongOrNull() ?: return null,
)

private fun Category.toModel(): ModelGalleryCategory? = ModelGalleryCategory(
    path = path,
    displayName = displayName,
    options = options.map { it.toModel() ?: return null },
    count = count.toLongOrNull() ?: return null,
)

private fun CategoryOption.toModel(): ModelGalleryCategoryOption? = ModelGalleryCategoryOption(
    name = name,
    fullTag = fullTag,
    count = count.toLongOrNull() ?: return null,
    disabled = disabled,
    virtualFilter = virtualFilter?.toModel(),
)

private fun VirtualFilter.toModel() = when (this) {
    VirtualFilter.MULTIPLE_MEDIA -> ModelVirtualFilter.MULTIPLE_MEDIA
    VirtualFilter.HAS_MEMO -> ModelVirtualFilter.HAS_MEMO
    VirtualFilter.HAS_VIDEO -> ModelVirtualFilter.HAS_VIDEO
    VirtualFilter.HAS_RELATED -> ModelVirtualFilter.HAS_RELATED
}

private fun NoteDetail.toModel(): ModelGalleryNoteDetail? {
    val mappedMedia = media.map { it.toModel() ?: return null }
    return ModelGalleryNoteDetail(
        id = ModelNoteId(id),
        path = path,
        title = title,
        author = author,
        authorUrl = authorUrl,
        url = url,
        published = published,
        created = created,
        updated = updated,
        tags = tags,
        bodyText = bodyText,
        memoLines = memoLines.map { it.toModel() },
        relatedLines = relatedLines.map { it.toModel() },
        media = mappedMedia,
    )
}

private fun DetailLine.toModel() = ModelGalleryDetailLine(
    text = text,
    urls = urls,
    isBullet = isBullet,
    indentLevel = indentLevel.toInt(),
    linkedNoteId = linkedNoteId?.let(::ModelNoteId),
)

private fun ULong.toLongOrNull(): Long? =
    if (this <= Long.MAX_VALUE.toULong()) toLong() else null

private fun GalleryException.toRepositoryError(): RepositoryError = when (this) {
    is GalleryException.VaultUnavailable -> RepositoryError.VAULT_UNAVAILABLE
    is GalleryException.InvalidVault -> RepositoryError.INVALID_VAULT
    is GalleryException.StorageUnavailable -> RepositoryError.STORAGE_UNAVAILABLE
    is GalleryException.IndexUnavailable -> RepositoryError.INDEX_UNAVAILABLE
    is GalleryException.InvalidRequest -> RepositoryError.INVALID_REQUEST
    is GalleryException.OperationFailed -> RepositoryError.OPERATION_FAILED
}

private const val MAX_SAF_NOTE_BYTES = 2 * 1024 * 1024
private const val MAX_THUMBNAIL_SIZE = 1_024
private const val MAX_THUMBNAIL_BYTES = 4 * 1024 * 1024
