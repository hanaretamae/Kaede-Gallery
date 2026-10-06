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

        assertEquals(imported, holder.state.value.settings)
        assertEquals(listOf(imported), repository.saved)
        assertEquals(SettingsTransferCodec.encode(imported), holder.exportSerialized())

        holder.importSerialized("not a settings file")
        advanceUntilIdle()
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertEquals(imported, holder.state.value.settings)
        assertEquals(listOf(imported), repository.saved)
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
