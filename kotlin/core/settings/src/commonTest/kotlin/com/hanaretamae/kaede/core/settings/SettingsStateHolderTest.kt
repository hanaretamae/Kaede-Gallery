package com.hanaretamae.kaede.core.settings

import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStateHolderTest {
    @Test
    fun systemLanguageUsesJapaneseOnlyForJapaneseLocaleAndHonorsOverrides() {
        assertEquals(
            LanguagePreference.JAPANESE,
            LanguagePreference.SYSTEM.resolve(LanguagePreference.JAPANESE),
        )
        assertEquals(
            LanguagePreference.ENGLISH,
            LanguagePreference.SYSTEM.resolve(LanguagePreference.ENGLISH),
        )
        assertEquals(
            LanguagePreference.JAPANESE,
            LanguagePreference.JAPANESE.resolve(LanguagePreference.ENGLISH),
        )
    }

    @Test
    fun savesConcurrentUpdatesInOrder() = runTest {
        val repository = FakeSettingsRepository()
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.setPageSize(60)
        holder.setShowTilePosition(true)
        advanceUntilIdle()

        assertEquals(60, holder.state.value.settings?.pageSize)
        assertTrue(holder.state.value.settings?.showTilePosition == true)
        assertEquals(2, repository.saved.size)
        assertEquals(60, repository.saved.last().pageSize)
        assertTrue(repository.saved.last().showTilePosition)
    }

    @Test
    fun rejectsOutOfRangePageSizeWithoutPersisting() = runTest {
        val repository = FakeSettingsRepository()
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.setPageSize(GallerySettings.MAX_PAGE_SIZE + 1)
        advanceUntilIdle()

        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertEquals(GallerySettings.DEFAULT_PAGE_SIZE, holder.state.value.settings?.pageSize)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun validatesAndPersistsFixedColumnCount() = runTest {
        val repository = FakeSettingsRepository()
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.setFixedColumnCount(4)
        advanceUntilIdle()

        assertEquals(4, holder.state.value.settings?.fixedColumnCount)
        assertEquals(4, repository.saved.single().fixedColumnCount)
        holder.setFixedColumnCount(GallerySettings.MAX_FIXED_COLUMN_COUNT + 1)
        advanceUntilIdle()
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        holder.setFixedColumnCount(1)
        advanceUntilIdle()
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun savesValidatedTagDisplayPrefixesWithoutReplacingOtherSettings() = runTest {
        val original = GallerySettings(pageSize = 48, showCounts = false)
        val repository = FakeSettingsRepository(settings = original)
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.saveTagDisplayPrefixes(
            includedPrefixes = listOf("source/type"),
            hiddenPrefixes = listOf("source/type/private"),
        )
        advanceUntilIdle()

        assertEquals(48, holder.state.value.settings?.pageSize)
        assertFalse(holder.state.value.settings?.showCounts == true)
        assertEquals(listOf("source/type"), holder.state.value.settings?.includedTagPrefixes)
        assertEquals(
            listOf("source/type/private"),
            repository.saved.single().hiddenTagPrefixes,
        )

        holder.saveTagDisplayPrefixes(listOf(""), emptyList())
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun savesTagColorsWhileRetainingOtherFlutterTagSettings() = runTest {
        val original = GallerySettings(
            flutterTagSettingsJson = """
                {"includedPrefixes":["*"],"hiddenPrefixes":["private"],"colors":[],
                "tagCategories":{"categories":[{"name":"Art","path":"source/art"}]}}
            """.trimIndent(),
        )
        val repository = FakeSettingsRepository(settings = original)
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        val result = holder.saveTagColorRules(
            listOf(GalleryTagColorRule("source/art", 0xff123456.toInt())),
        )

        assertTrue(result is RepositoryResult.Success)
        assertEquals(
            listOf(GalleryTagColorRule("source/art", 0xff123456.toInt())),
            GalleryTagColorCodec.decodeRules(repository.saved.single().flutterTagSettingsJson),
        )
        assertTrue(
            repository.saved.single().flutterTagSettingsJson.orEmpty()
                .contains(""""tagCategories":{"categories":[{"name":"Art","path":"source/art"}]}"""),
        )
    }

    @Test
    fun savesTagCategoriesAndPreservesOtherFlutterSettings() = runTest {
        val original = GallerySettings(
            flutterTagSettingsJson = """
                {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],
                "noteStructure":{"memoHeadings":["Notes"]},"custom":"kept"}
            """.trimIndent(),
        )
        val repository = FakeSettingsRepository(settings = original)
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()
        val categories = GalleryTagCategorySettings(
            categories = listOf(GalleryTagCategoryRule("Portfolio", "portfolio/*", true)),
            other = GalleryOtherCategorySettings(false, "Unmatched"),
        )

        val result = holder.saveTagCategorySettings(categories)

        assertTrue(result is RepositoryResult.Success)
        val savedJson = repository.saved.single().flutterTagSettingsJson
        assertEquals(
            categories,
            GalleryTagCategoryCodec.decodeSettings(savedJson, japaneseDefaults = false),
        )
        assertTrue(savedJson.orEmpty().contains(""""custom":"kept""""))
        assertTrue(savedJson.orEmpty().contains(""""memoHeadings":["Notes"]"""))
    }

    @Test
    fun savesNoteStructureWithoutChangingGalleryEligibilityOrOtherTagSettings() = runTest {
        val original = GallerySettings(
            galleryTagPrefixes = listOf("portfolio"),
            flutterTagSettingsJson = """
                {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],
                "tagCategories":{"categories":[{"name":"Portfolio","path":"portfolio/*"}]},
                "noteStructure":{"galleryTagPrefixes":["portfolio"],"unknown":"preserved"}}
            """.trimIndent(),
        )
        val repository = FakeSettingsRepository(settings = original)
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()
        val noteStructure = GalleryNoteStructureSettings(
            memoHeadings = listOf("Journal"),
            galleryTagPrefixes = listOf("not-used"),
            linkResolution = GalleryLinkResolution.ABSOLUTE_PATH,
        )

        val result = holder.saveNoteStructureSettings(noteStructure)

        assertTrue(result is RepositoryResult.Success)
        val saved = repository.saved.single()
        assertEquals(listOf("portfolio"), saved.galleryTagPrefixes)
        val decoded = GalleryNoteStructureCodec.decodeSettings(
            saved.flutterTagSettingsJson,
            saved.galleryTagPrefixes,
        )
        assertEquals(listOf("Journal"), decoded.memoHeadings)
        assertEquals(GalleryLinkResolution.ABSOLUTE_PATH, decoded.linkResolution)
        assertTrue(saved.flutterTagSettingsJson.orEmpty().contains(""""unknown":"preserved""""))
        assertTrue(saved.flutterTagSettingsJson.orEmpty().contains(""""tagCategories""""))
    }

    @Test
    fun reportsOversizedTransferExportsAsInvalidRequests() = runTest {
        val repeatedPrefix = "a".repeat(GalleryTagPrefixesCodec.MAX_PREFIX_LENGTH)
        val advancedSettings = """
            {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],"other":"${"x".repeat(13_000)}"}
        """.trimIndent()
        val repository = FakeSettingsRepository(
            settings = GallerySettings(
                includedTagPrefixes = List(GalleryTagPrefixesCodec.MAX_PREFIXES) {
                    repeatedPrefix
                },
                hiddenTagPrefixes = List(GalleryTagPrefixesCodec.MAX_PREFIXES) {
                    repeatedPrefix
                },
                flutterTagSettingsJson = advancedSettings,
            ),
        )
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        assertNull(holder.exportSerialized())
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
    }

    @Test
    fun reportsSaveFailureWithoutPublishingUnsavedSettings() = runTest {
        val repository = FakeSettingsRepository(
            saveResult = RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE),
        )
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.setPageSize(48)
        advanceUntilIdle()

        assertEquals(RepositoryError.STORAGE_UNAVAILABLE, holder.state.value.error)
        assertEquals(48, holder.state.value.settings?.pageSize)
        assertTrue(holder.state.value.hasUnsavedChanges)
        assertFalse(holder.state.value.saving)

        repository.saveResult = RepositoryResult.Success(Unit)
        holder.retrySave()
        advanceUntilIdle()

        assertNull(holder.state.value.error)
        assertFalse(holder.state.value.hasUnsavedChanges)
        assertEquals(48, holder.state.value.settings?.pageSize)
    }

    @Test
    fun resetPersistsExplicitDefaults() = runTest {
        val repository = FakeSettingsRepository(
            settings = GallerySettings(pageSize = 48, showTilePosition = true),
        )
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()

        holder.reset()
        advanceUntilIdle()

        assertEquals(GallerySettings(), holder.state.value.settings)
        assertEquals(GallerySettings(), repository.saved.last())
        assertNull(holder.state.value.error)
    }

    @Test
    fun importsValidatedSettingsAndRejectsMalformedContentWithoutSaving() = runTest {
        val repository = FakeSettingsRepository()
        val holder = SettingsStateHolder(repository, this)
        holder.load()
        advanceUntilIdle()
        val imported = GallerySettings(
            appearance = AppearanceSettings(
                theme = ThemePreference.DARK,
                language = LanguagePreference.JAPANESE,
                useSystemColor = false,
                pureBlack = true,
            ),
            pageSize = 80,
            showLoadedRange = false,
            showTilePosition = true,
            showCounts = false,
        )

        holder.importSerialized(SettingsTransferCodec.encode(imported))
        advanceUntilIdle()

        assertEquals(imported, holder.state.value.settings?.copy(flutterTagSettingsJson = null))
        assertEquals(
            imported,
            repository.saved.single().copy(flutterTagSettingsJson = null),
        )
        assertEquals(SettingsTransferCodec.encode(imported), holder.exportSerialized())

        holder.importSerialized("not a settings file")
        advanceUntilIdle()
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertEquals(imported, holder.state.value.settings?.copy(flutterTagSettingsJson = null))
        assertEquals(1, repository.saved.size)
    }
}

private class FakeSettingsRepository(
    private val settings: GallerySettings = GallerySettings(),
    var saveResult: RepositoryResult<Unit> = RepositoryResult.Success(Unit),
) : SettingsRepository {
    val saved = mutableListOf<GallerySettings>()

    override suspend fun load(): RepositoryResult<GallerySettings> =
        RepositoryResult.Success(settings)

    override suspend fun save(settings: GallerySettings): RepositoryResult<Unit> {
        saved += settings
        return saveResult
    }
}
