package com.hanaretamae.kaede.core.model

@JvmInline
value class NoteId(val value: Long)

@JvmInline
value class MediaId(val value: Long)

enum class GallerySortField {
    PUBLISHED,
    CREATED,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

enum class GalleryContent {
    NOTES,
    MEDIA,
}

enum class VirtualFilter {
    MULTIPLE_MEDIA,
    HAS_MEMO,
    HAS_VIDEO,
    HAS_RELATED,
}

data class GalleryQuery(
    val content: GalleryContent = GalleryContent.NOTES,
    val searchText: String = "",
    val includeTags: Set<String> = emptySet(),
    val andTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val virtualFilters: Set<VirtualFilter> = emptySet(),
    val sortField: GallerySortField = GallerySortField.CREATED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val offset: Long = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    init {
        require(offset >= 0) { "offset must be non-negative" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "pageSize is out of range" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 24
        const val MAX_PAGE_SIZE = 500
    }
}

sealed interface GalleryEntry {
    val mediaCount: Long
    val memoCount: Long
    val relatedCount: Long
}

data class NoteSummary(
    val id: NoteId,
    val path: String,
    val title: String,
    val representativeMediaId: MediaId?,
    val representativeMediaIsVideo: Boolean = false,
    val representativeMediaExists: Boolean = true,
    override val mediaCount: Long,
    val videoCount: Long,
    override val memoCount: Long,
    override val relatedCount: Long,
) : GalleryEntry

data class MediaSummary(
    val id: MediaId,
    val noteId: NoteId,
    val notePath: String,
    val isVideo: Boolean,
    val exists: Boolean,
    override val mediaCount: Long,
    override val memoCount: Long,
    override val relatedCount: Long,
) : GalleryEntry

data class GalleryPage(
    val entries: List<GalleryEntry>,
    val totalCount: Long,
    val offset: Long,
) {
    init {
        require(totalCount >= 0) { "totalCount must be non-negative" }
        require(offset >= 0) { "offset must be non-negative" }
    }

    val hasNextPage: Boolean
        get() = offset < totalCount && totalCount - offset > entries.size
}

data class GalleryCategory(
    val path: String,
    val displayName: String,
    val options: List<GalleryCategoryOption>,
    val count: Long,
)

data class GalleryCategoryOption(
    val name: String,
    val fullTag: String,
    val count: Long,
    val disabled: Boolean,
    val virtualFilter: VirtualFilter?,
)

data class GalleryDetailLine(
    val text: String,
    val urls: List<String>,
    val isBullet: Boolean,
    val indentLevel: Int,
    val linkedNoteId: NoteId?,
)

data class GalleryNoteDetail(
    val id: NoteId,
    val path: String,
    val title: String,
    val author: String?,
    val authorUrl: String?,
    val url: String?,
    val published: String?,
    val created: String?,
    val updated: String?,
    val tags: List<String>,
    val bodyText: String,
    val memoLines: List<GalleryDetailLine>,
    val relatedLines: List<GalleryDetailLine>,
    val media: List<MediaSummary>,
    val postTextEndLines: List<GalleryDetailLine> = emptyList(),
)
