package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewerStateHolderTest {
    @Test
    fun loadsDetailsAndNavigatesMediaWithoutExposingLocationInStateErrors() = runTest {
        val first = media(21)
        val second = media(22)
        val repository = ViewerRepository(
            detail = detail(listOf(first, second)),
            locations = mapOf(first.id to "media/first.jpg", second.id to "media/second.jpg"),
        )
        val holder = testGalleryViewerStateHolder(repository, note(first.id))

        holder.start()
        advanceUntilIdle()

        assertEquals("Sample note", holder.state.value.note?.title)
        assertEquals(first, holder.state.value.selectedMedia)
        assertEquals("media/first.jpg", holder.state.value.mediaLocation)
        assertFalse(holder.state.value.mediaLocationLoading)

        holder.showNextMedia()
        advanceUntilIdle()
        assertEquals(second, holder.state.value.selectedMedia)
        assertEquals("media/second.jpg", holder.state.value.mediaLocation)
        assertFalse(holder.state.value.mediaLocationLoading)

        holder.showNextMedia()
        assertEquals(second, holder.state.value.selectedMedia)
        holder.dispose()
    }

    @Test
    fun missingDetailReturnsSanitizedFailure() = runTest {
        val holder = testGalleryViewerStateHolder(
            ViewerRepository(detail = null),
            note(MediaId(21)),
        )

        holder.start()
        advanceUntilIdle()

        assertEquals(RepositoryError.OPERATION_FAILED, holder.state.value.error)
        assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.note)
    }

    @Test
    fun mediaEntrySelectsItsOwnMedia() = runTest {
        val first = media(21)
        val selected = media(22)
        val holder = testGalleryViewerStateHolder(
            ViewerRepository(detail = detail(listOf(first, selected))),
            selected,
        )

        holder.start()
        advanceUntilIdle()

        assertEquals(selected, holder.state.value.selectedMedia)
        assertEquals(1, holder.state.value.selectedMediaIndex)
    }

    @Test
    fun viewerResolvesAdjacentImageLocationsInParallelForPrefetch() = runTest {
        val first = media(21)
        val second = media(22)
        val third = media(23)
        val neighborGate = CompletableDeferred<Unit>()
        val requestedLocations = mutableSetOf<MediaId>()
        val repository = object : GalleryRepository by ViewerRepository(
            detail = detail(listOf(first, second, third)),
        ) {
            override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> {
                requestedLocations += mediaId
                if (mediaId != first.id) neighborGate.await()
                return RepositoryResult.Success("media/${mediaId.value}.jpg")
            }
        }
        val holder = testGalleryViewerStateHolder(repository, first)

        holder.start()
        runCurrent()

        assertEquals(listOf(second.id, third.id), holder.state.value.neighborMediaIds)
        assertTrue(
            requestedLocations.containsAll(listOf(second.id, third.id)),
            "Adjacent locations should resolve concurrently while the selected image loads.",
        )

        neighborGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(
            listOf("media/22.jpg", "media/23.jpg"),
            holder.state.value.neighborMediaLocations,
        )
        holder.dispose()
    }

    @Test
    fun noteDetailBecomesVisibleBeforeSlowMediaLocationFinishes() = runTest {
        val media = media(21)
        val locationGate = CompletableDeferred<Unit>()
        val repository = object : GalleryRepository by ViewerRepository(detail(listOf(media))) {
            override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> {
                locationGate.await()
                return RepositoryResult.Success("media/fictional.png")
            }
        }
        val holder = testGalleryViewerStateHolder(repository, media)
        holder.start()
        runCurrent()
        assertEquals("Sample note", holder.state.value.note?.title)
        assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.mediaLocation)
        locationGate.complete(Unit)
        advanceUntilIdle()
        assertEquals("media/fictional.png", holder.state.value.mediaLocation)
        holder.dispose()
    }

    @Test
    fun lateInitialLocationDoesNotReplaceMediaSelectedWhileLoading() = runTest {
        val first = media(21)
        val second = media(22)
        val locationGate = CompletableDeferred<Unit>()
        val repository = object : GalleryRepository by ViewerRepository(detail(listOf(first, second))) {
            override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> {
                if (mediaId == first.id) locationGate.await()
                return RepositoryResult.Success(
                    if (mediaId == first.id) "media/first.png" else "media/second.png",
                )
            }
        }
        val holder = testGalleryViewerStateHolder(repository, first)
        holder.start()
        runCurrent()
        holder.showNextMedia()
        runCurrent()
        assertEquals("media/second.png", holder.state.value.mediaLocation)
        locationGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(second, holder.state.value.selectedMedia)
        assertEquals("media/second.png", holder.state.value.mediaLocation)
        holder.dispose()
    }
}

private fun TestScope.testGalleryViewerStateHolder(
    repository: GalleryRepository,
    entry: GalleryEntry,
) = GalleryViewerStateHolder(
    repository = repository,
    scope = this,
    entry = entry,
    repositoryDispatcher = StandardTestDispatcher(testScheduler),
)

private class ViewerRepository(
    private val detail: GalleryNoteDetail?,
    private val locations: Map<MediaId, String?> = emptyMap(),
) : GalleryRepository {
    override suspend fun queryPage(query: GalleryQuery) =
        RepositoryResult.Success(GalleryPage(emptyList(), 0, query.offset))

    override suspend fun categories(query: GalleryQuery) =
        RepositoryResult.Success(emptyList<GalleryCategory>())

    override suspend fun noteDetail(
        noteId: NoteId,
        safContent: ByteArray?,
    ) = RepositoryResult.Success(detail)

    override suspend fun notePath(noteId: NoteId): RepositoryResult<String?> =
        RepositoryResult.Success(null)

    override suspend fun mediaLocation(mediaId: MediaId) =
        RepositoryResult.Success(locations[mediaId])

    override suspend fun thumbnail(mediaId: MediaId, size: Int): RepositoryResult<ByteArray?> =
        RepositoryResult.Success(null)
}

private fun note(representative: MediaId) = NoteSummary(
    id = NoteId(7),
    path = "notes/sample.md",
    title = "Sample note",
    representativeMediaId = representative,
    mediaCount = 2,
    videoCount = 0,
    memoCount = 1,
    relatedCount = 1,
)

private fun media(id: Long) = MediaSummary(
    id = MediaId(id),
    noteId = NoteId(7),
    notePath = "notes/sample.md",
    isVideo = false,
    exists = true,
    mediaCount = 2,
    memoCount = 1,
    relatedCount = 1,
)

private fun detail(media: List<MediaSummary>) = GalleryNoteDetail(
    id = NoteId(7),
    path = "notes/sample.md",
    title = "Sample note",
    author = "Fictional author",
    authorUrl = null,
    url = null,
    published = null,
    created = null,
    updated = null,
    tags = emptyList(),
    bodyText = "Fictional body text",
    memoLines = listOf(GalleryDetailLine("Fictional memo", emptyList(), false, 0, null)),
    relatedLines = emptyList(),
    media = media,
)
