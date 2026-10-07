package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.RepositoryResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class Phase13FixtureParityTest {
    @Test
    fun scansReadOnlyDummyVaultAndMatchesFlutterNoteDetailsContract() = runTest {
        val fixture = findDummyVault()
        assertTrue(Files.isRegularFile(fixture.resolve("broken-note.md")))
        assertTrue(Files.isRegularFile(fixture.resolve("note-000000.md")))

        val temporaryDirectory = Files.createTempDirectory("kaede-phase13-fixture-test")
        val privateData = temporaryDirectory.resolve("private")
        var handle: GallerySessionHandle? = null
        try {
            Files.createDirectories(privateData)
            Files.writeString(
                privateData.resolve("tag-settings.json"),
                """{"noteStructure":{"galleryTagPrefixes":["source/"]}}""",
            )
            val opened = RustGallerySessionRepository().openFilesystem(
                privateDataDirectory = privateData.toString(),
                vaultPath = fixture.toString(),
                indexPath = privateData.resolve("index.sqlite").toString(),
                thumbnailCachePath = privateData.resolve("thumbnails").toString(),
            )
            val openedHandle = assertIs<RepositoryResult.Success<GallerySessionHandle>>(opened).value
            handle = openedHandle

            assertEquals(12L, openedHandle.initialScan.notesIndexed)
            assertEquals(1L, openedHandle.initialScan.warnings)

            val page = assertIs<RepositoryResult.Success<GalleryPage>>(
                openedHandle.gallery.queryPage(GalleryQuery(pageSize = 100)),
            ).value
            assertEquals(12L, page.totalCount)
            val note = page.entries.filterIsInstance<NoteSummary>()
                .single { it.path == "note-000000.md" }
            assertEquals(
                RepositoryResult.Success("note-000000.md"),
                openedHandle.gallery.notePath(note.id),
            )

            val detail = assertNotNull(
                assertIs<RepositoryResult.Success<GalleryNoteDetail?>>(
                    openedHandle.gallery.noteDetail(note.id),
                ).value,
            )
            assertEquals("note-000000.md", detail.path)
            assertEquals("Fictional note 000000", detail.title)
            assertEquals(
                setOf(
                    "source/service/example",
                    "source/rating/safe",
                    "copyright/original",
                ),
                detail.tags.toSet(),
            )
            assertEquals("Synthetic test content only.", detail.bodyText)

            val memo = detail.memoLines.single()
            assertEquals("synthetic memo", memo.text)
            assertTrue(memo.isBullet)
            assertEquals(0, memo.indentLevel)
            assertEquals(emptyList(), memo.urls)

            val related = detail.relatedLines.single()
            assertEquals("fictional link", related.text)
            assertEquals(listOf("https://example.invalid/related"), related.urls)
            assertTrue(related.isBullet)
            assertEquals(0, related.indentLevel)
            assertEquals(null, related.linkedNoteId)

            val image = detail.media.single()
            assertEquals(note.id, image.noteId)
            assertEquals("note-000000.md", image.notePath)
            assertEquals(false, image.isVideo)
            assertEquals(true, image.exists)
            val mediaSourcePath = assertNotNull(
                assertIs<RepositoryResult.Success<String?>>(
                    openedHandle.gallery.mediaLocation(image.id),
                ).value,
            )
            assertTrue(
                Files.isSameFile(
                    Path.of(mediaSourcePath),
                    fixture.resolve("media/pixel.png"),
                ),
                "The returned media source should identify the fixture image.",
            )

            val filteredPage = assertIs<RepositoryResult.Success<GalleryPage>>(
                openedHandle.gallery.queryPage(
                    GalleryQuery(
                        includeTags = setOf("source/rating/safe"),
                        pageSize = 100,
                    ),
                ),
            ).value
            assertTrue(
                filteredPage.entries.filterIsInstance<NoteSummary>()
                    .any { it.path == "note-000000.md" },
            )

        } finally {
            try {
                handle?.close()
            } finally {
                Files.walk(temporaryDirectory).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
                }
            }
        }
    }

    private fun findDummyVault(): Path {
        var directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (true) {
            val candidate = directory.resolve("testdata/dummy-vault")
            if (Files.isDirectory(candidate)) return candidate.toRealPath()
            directory = directory.parent ?: error("testdata/dummy-vault was not found")
        }
    }
}
