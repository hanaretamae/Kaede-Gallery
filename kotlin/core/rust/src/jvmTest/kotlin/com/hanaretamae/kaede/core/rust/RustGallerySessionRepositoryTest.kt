package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.repository.SafScanNote
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class RustGallerySessionRepositoryTest {
    @Test
    fun opensScansQueriesAndClosesFictionalFilesystemVault() = runTest {
        val temporaryDirectory = Files.createTempDirectory("kaede-session-test")
        try {
            val vault = Files.createDirectory(temporaryDirectory.resolve("vault"))
            Files.writeString(
                vault.resolve("fictional.md"),
                """
                ---
                tags:
                  - source/art
                ---
                # Fictional note
                Synthetic test fixture.
                """.trimIndent(),
            )
            val privateData = temporaryDirectory.resolve("private")
            val repository = RustGallerySessionRepository()
            val opened = repository.openFilesystem(
                privateDataDirectory = privateData.toString(),
                vaultPath = vault.toString(),
                indexPath = privateData.resolve("index.sqlite").toString(),
                thumbnailCachePath = privateData.resolve("thumbnails").toString(),
            )
            val handle: GallerySessionHandle = when (opened) {
                is RepositoryResult.Success -> opened.value
                is RepositoryResult.Failure -> error("Fictional Vault session did not open")
            }
            try {
                val selectionRepository = RustVaultSelectionRepository()
                assertIs<RepositoryResult.Success<String>>(
                    selectionRepository.saveSelected(
                        privateDataDirectory = privateData.toString(),
                        vaultLocator = vault.toString(),
                    ),
                )
                val selected = assertIs<RepositoryResult.Success<String?>>(
                    selectionRepository.loadSelected(privateData.toString()),
                )
                assertEquals(vault.toString(), selected.value)
                assertIs<RepositoryResult.Success<*>>(
                    handle.gallery.queryPage(GalleryQuery(pageSize = 10)),
                )
                assertIs<RepositoryResult.Success<*>>(handle.rescan())
            } finally {
                handle.close()
            }

            Files.writeString(
                vault.resolve("new-fictional.md"),
                """
                ---
                tags:
                  - source/art
                ---
                # Newly added fictional note
                """.trimIndent(),
            )
            val reopened = repository.openFilesystem(
                privateDataDirectory = privateData.toString(),
                vaultPath = vault.toString(),
                indexPath = privateData.resolve("index.sqlite").toString(),
                thumbnailCachePath = privateData.resolve("thumbnails").toString(),
                performScan = false,
            )
            val reusedHandle = assertIs<RepositoryResult.Success<GallerySessionHandle>>(
                reopened,
            ).value
            try {
                val reusedPage = assertIs<RepositoryResult.Success<GalleryPage>>(
                    reusedHandle.gallery.queryPage(GalleryQuery(pageSize = 10)),
                ).value
                assertEquals(1L, reusedPage.totalCount)
                assertIs<RepositoryResult.Success<*>>(reusedHandle.rescan())
                val rescannedPage = assertIs<RepositoryResult.Success<GalleryPage>>(
                    reusedHandle.gallery.queryPage(GalleryQuery(pageSize = 10)),
                ).value
                assertEquals(2L, rescannedPage.totalCount)
            } finally {
                reusedHandle.close()
            }
        } finally {
            Files.walk(temporaryDirectory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun streamsBoundedSafBatchesAndCancellationPreservesCommittedIndex() = runTest {
        val temporaryDirectory = Files.createTempDirectory("kaede-saf-session-test")
        try {
            val privateData = temporaryDirectory.resolve("private")
            val uri =
                "content://com.android.externalstorage.documents/tree/primary%3AFictional"
            val repository = RustGallerySessionRepository()
            val firstNote = """
                ---
                tags: [source/art]
                ---
                # First fictional note
            """.trimIndent().encodeToByteArray()
            val opened = repository.openSaf(
                privateDataDirectory = privateData.toString(),
                vaultUri = uri,
                indexPath = privateData.resolve("gallery.sqlite").toString(),
                thumbnailCachePath = privateData.resolve("thumbnails").toString(),
                filePaths = listOf("first.md"),
                batches = flowOf(
                    listOf(
                        SafScanNote(
                            path = "first.md",
                            modifiedNanos = 1,
                            size = firstNote.size.toLong(),
                            content = firstNote,
                        ),
                    ),
                ),
            )
            val handle = assertIs<RepositoryResult.Success<GallerySessionHandle>>(opened).value
            try {
                val before = assertIs<RepositoryResult.Success<GalleryPage>>(
                    handle.gallery.queryPage(GalleryQuery(pageSize = 10)),
                ).value
                assertEquals(1L, before.totalCount)

                assertFailsWith<CancellationException> {
                    handle.rescanSaf(
                        filePaths = listOf("first.md", "second.md"),
                        batches = flow {
                            emit(
                                listOf(
                                    SafScanNote(
                                        path = "second.md",
                                        modifiedNanos = 2,
                                        size = firstNote.size.toLong(),
                                        content = firstNote,
                                    ),
                                ),
                            )
                            throw CancellationException()
                        },
                    )
                }
                val after = assertIs<RepositoryResult.Success<GalleryPage>>(
                    handle.gallery.queryPage(GalleryQuery(pageSize = 10)),
                ).value
                assertEquals(1L, after.totalCount)
            } finally {
                handle.close()
            }
        } finally {
            Files.walk(temporaryDirectory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
