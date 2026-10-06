package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.model.GalleryContent as ModelGalleryContent
import com.hanaretamae.kaede.core.model.GalleryCategory as ModelGalleryCategory
import com.hanaretamae.kaede.core.model.GalleryCategoryOption as ModelGalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryDetailLine as ModelGalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryPage as ModelGalleryPage
import com.hanaretamae.kaede.core.model.GalleryNoteDetail as ModelGalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryQuery as ModelGalleryQuery
import com.hanaretamae.kaede.core.model.GallerySortField as ModelGallerySortField
import com.hanaretamae.kaede.core.model.MediaId as ModelMediaId
import com.hanaretamae.kaede.core.model.MediaSummary as ModelMediaSummary
import com.hanaretamae.kaede.core.model.NoteId as ModelNoteId
import com.hanaretamae.kaede.core.model.NoteSummary as ModelNoteSummary
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter as ModelVirtualFilter
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RustGalleryRepositoryTest {
    @Test
    fun queryPageMapsQueryAndNoteResult() = runTest {
        val session = FakeGallerySession(
            page = GalleryPage(
                notes = listOf(
                    NoteSummary(
                        id = 42,
                        path = "notes/example.md",
                        title = "Example",
                        mediaCount = 2u,
                        videoCount = 1u,
                        memoCount = 3u,
                        relatedCount = 4u,
                        representativeMediaId = 99,
                    ),
                ),
                media = emptyList(),
                totalCount = 101u,
                offset = 24u,
            ),
        )
        val query = ModelGalleryQuery(
            searchText = "#art",
            includeTags = setOf("tag/z", "tag/a"),
            andTags = setOf("tag/and"),
            excludedTags = setOf("tag/excluded"),
            virtualFilters = setOf(ModelVirtualFilter.HAS_VIDEO, ModelVirtualFilter.HAS_MEMO),
            sortField = ModelGallerySortField.PUBLISHED,
            sortDirection = SortDirection.ASCENDING,
            offset = 24,
            pageSize = 48,
        )

        val result = RustGalleryRepository(session).queryPage(query)

        assertEquals(
            GalleryQuery(
                content = GalleryContent.NOTES,
                searchText = "#art",
                includeTags = listOf("tag/a", "tag/z"),
                andTags = listOf("tag/and"),
                excludedTags = listOf("tag/excluded"),
                virtualFilters = listOf(VirtualFilter.HAS_MEMO, VirtualFilter.HAS_VIDEO),
                sortField = GallerySortField.PUBLISHED,
                sortDirection = GallerySortDirection.ASCENDING,
                offset = 24u,
                pageSize = 48u,
            ),
            session.receivedQuery,
        )
        val page = assertIs<RepositoryResult.Success<ModelGalleryPage>>(result).value
        assertEquals(101, page.totalCount)
        assertEquals(24, page.offset)
        assertEquals(
            ModelNoteSummary(
                id = ModelNoteId(42),
                path = "notes/example.md",
                title = "Example",
                representativeMediaId = ModelMediaId(99),
                mediaCount = 2,
                videoCount = 1,
                memoCount = 3,
                relatedCount = 4,
            ),
            page.entries.single(),
        )
    }

    @Test
    fun mapsSanitizedRustErrors() = runTest {
        val session = FakeGallerySession(error = GalleryException.InvalidRequest())

        val result = RustGalleryRepository(session).queryPage(ModelGalleryQuery())

        assertEquals(
            RepositoryResult.Failure(RepositoryError.INVALID_REQUEST),
            result,
        )
    }

    @Test
    fun queryPageMapsMediaResult() = runTest {
        val session = FakeGallerySession(
            page = GalleryPage(
                notes = emptyList(),
                media = listOf(
                    MediaSummary(
                        id = 11,
                        noteId = 42,
                        isVideo = true,
                        exists = false,
                        mediaCount = 2u,
                        memoCount = 1u,
                        relatedCount = 3u,
                    ),
                ),
                totalCount = 1u,
                offset = 0u,
            ),
        )

        val result = RustGalleryRepository(session).queryPage(
            ModelGalleryQuery(content = ModelGalleryContent.MEDIA),
        )

        val page = assertIs<RepositoryResult.Success<ModelGalleryPage>>(result).value
        assertEquals(
            ModelMediaSummary(
                id = ModelMediaId(11),
                noteId = ModelNoteId(42),
                isVideo = true,
                exists = false,
                mediaCount = 2,
                memoCount = 1,
                relatedCount = 3,
            ),
            page.entries.single(),
        )
    }

    @Test
    fun rejectsCountsOutsideModelRange() = runTest {
        val session = FakeGallerySession(
            page = GalleryPage(
                notes = emptyList(),
                media = emptyList(),
                totalCount = ULong.MAX_VALUE,
                offset = 0u,
            ),
        )

        val result = RustGalleryRepository(session).queryPage(ModelGalleryQuery())

        assertEquals(
            RepositoryResult.Failure(RepositoryError.OPERATION_FAILED),
            result,
        )
    }

    @Test
    fun mapsTagCategoriesAndVirtualFilters() = runTest {
        val session = FakeGallerySession(
            categories = listOf(
                Category(
                    path = "type",
                    displayName = "Type",
                    options = listOf(
                        CategoryOption(
                            name = "image",
                            fullTag = "type/image",
                            count = 9u,
                            disabled = false,
                            virtualFilter = VirtualFilter.HAS_VIDEO,
                        ),
                    ),
                    count = 9u,
                ),
            ),
        )

        val result = RustGalleryRepository(session).categories(ModelGalleryQuery())

        assertEquals(
            RepositoryResult.Success(
                listOf(
                    ModelGalleryCategory(
                        path = "type",
                        displayName = "Type",
                        options = listOf(
                            ModelGalleryCategoryOption(
                                name = "image",
                                fullTag = "type/image",
                                count = 9,
                                disabled = false,
                                virtualFilter = ModelVirtualFilter.HAS_VIDEO,
                            ),
                        ),
                        count = 9,
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun mapsNoteDetailsWithoutChangingContent() = runTest {
        val session = FakeGallerySession(
            detail = NoteDetail(
                id = 42,
                path = "notes/example.md",
                title = "Example",
                author = "Fictional Author",
                authorUrl = null,
                url = "https://example.invalid",
                published = "2020-01-02",
                created = null,
                updated = null,
                tags = listOf("type/image"),
                bodyText = "Fictional note body",
                memoLines = listOf(
                    DetailLine(
                        text = "Memo",
                        urls = emptyList(),
                        isBullet = true,
                        indentLevel = 1u,
                        linkedNoteId = null,
                    ),
                ),
                relatedLines = emptyList(),
                media = emptyList(),
            ),
        )

        val result = RustGalleryRepository(session).noteDetail(ModelNoteId(42))

        assertEquals(
            RepositoryResult.Success(
                ModelGalleryNoteDetail(
                    id = ModelNoteId(42),
                    path = "notes/example.md",
                    title = "Example",
                    author = "Fictional Author",
                    authorUrl = null,
                    url = "https://example.invalid",
                    published = "2020-01-02",
                    created = null,
                    updated = null,
                    tags = listOf("type/image"),
                    bodyText = "Fictional note body",
                    memoLines = listOf(
                        ModelGalleryDetailLine(
                            text = "Memo",
                            urls = emptyList(),
                            isBullet = true,
                            indentLevel = 1,
                            linkedNoteId = null,
                        ),
                    ),
                    relatedLines = emptyList(),
                    media = emptyList(),
                ),
            ),
            result,
        )
    }

    @Test
    fun validatesThumbnailRequestsBeforeCrossingFfi() = runTest {
        val session = FakeGallerySession(thumbnailBytes = byteArrayOf(1, 2, 3))
        val repository = RustGalleryRepository(session)

        assertEquals(
            RepositoryResult.Failure(RepositoryError.INVALID_REQUEST),
            repository.thumbnail(ModelMediaId(5), 0),
        )
        assertEquals(0, session.thumbnailCalls)
        val result = repository.thumbnail(ModelMediaId(5), 64)
        val success = assertIs<RepositoryResult.Success<ByteArray?>>(result)
        assertContentEquals(byteArrayOf(1, 2, 3), success.value)
        assertEquals(1, session.thumbnailCalls)
    }

    @Test
    fun mapsValidatedMediaLocation() = runTest {
        val session = FakeGallerySession(mediaLocation = "media/example.jpg")

        assertEquals(
            RepositoryResult.Success("media/example.jpg"),
            RustGalleryRepository(session).mediaLocation(ModelMediaId(5)),
        )
    }
}

private class FakeGallerySession(
    private val page: GalleryPage = GalleryPage(emptyList(), emptyList(), 0u, 0u),
    private val error: GalleryException? = null,
    private val categories: List<Category> = emptyList(),
    private val detail: NoteDetail? = null,
    private val mediaLocation: String? = null,
    private val thumbnailBytes: ByteArray? = null,
) : GallerySessionInterface {
    var receivedQuery: GalleryQuery? = null
        private set
    var thumbnailCalls: Int = 0
        private set

    override fun categories(query: GalleryQuery): List<Category> = categories
    override fun mediaLocation(mediaId: Long): String? = mediaLocation
    override fun noteDetail(noteId: Long): NoteDetail? = detail
    override fun noteDetailSaf(noteId: Long, content: ByteArray): NoteDetail? = detail

    override fun queryPage(query: GalleryQuery): GalleryPage {
        receivedQuery = query
        error?.let { throw it }
        return page
    }

    override fun scan(): ScanReport = kotlin.error("Unexpected call")
    override fun scanSaf(notes: List<SafNote>, filePaths: List<String>): ScanReport =
        kotlin.error("Unexpected call")
    override fun beginSafScan(filePaths: List<String>) = kotlin.error("Unexpected call")
    override fun appendSafScanBatch(notes: List<SafNote>) = kotlin.error("Unexpected call")
    override fun finishSafScan(): ScanReport = kotlin.error("Unexpected call")
    override fun cancelSafScan() = Unit

    override fun thumbnail(mediaId: Long, size: UInt): ByteArray? {
        thumbnailCalls++
        return thumbnailBytes
    }
}
