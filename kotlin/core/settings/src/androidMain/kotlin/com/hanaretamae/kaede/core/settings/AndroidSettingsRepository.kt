package com.hanaretamae.kaede.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSettingsRepository(context: Context) : SettingsRepository {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): RepositoryResult<GallerySettings> = withContext(Dispatchers.IO) {
        try {
            val theme = when (preferences.getString(KEY_THEME, ThemePreference.SYSTEM.name)) {
                ThemePreference.SYSTEM.name -> ThemePreference.SYSTEM
                ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                ThemePreference.DARK.name -> ThemePreference.DARK
                else -> return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            }
            val language = when (
                preferences.getString(KEY_LANGUAGE, LanguagePreference.SYSTEM.name)
            ) {
                LanguagePreference.SYSTEM.name -> LanguagePreference.SYSTEM
                LanguagePreference.JAPANESE.name -> LanguagePreference.JAPANESE
                LanguagePreference.ENGLISH.name -> LanguagePreference.ENGLISH
                else -> return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            }
            val pageSize = preferences.getInt(KEY_PAGE_SIZE, GallerySettings.DEFAULT_PAGE_SIZE)
            if (pageSize !in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE) {
                return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            }
            RepositoryResult.Success(
                GallerySettings(
                    appearance = AppearanceSettings(
                        theme = theme,
                        language = language,
                        useSystemColor = preferences.getBoolean(KEY_SYSTEM_COLOR, true),
                        pureBlack = preferences.getBoolean(KEY_PURE_BLACK, false),
                    ),
                    pageSize = pageSize,
                    showLoadedRange = preferences.getBoolean(KEY_SHOW_RANGE, true),
                    showTilePosition = preferences.getBoolean(KEY_TILE_POSITION, false),
                    showCounts = preferences.getBoolean(KEY_SHOW_COUNTS, true),
                ),
            )
        } catch (_: ClassCastException) {
            RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        } catch (_: SecurityException) {
            RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
    }

    override suspend fun save(settings: GallerySettings): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val saved = preferences.edit()
                    .putString(KEY_THEME, settings.appearance.theme.name)
                    .putString(KEY_LANGUAGE, settings.appearance.language.name)
                    .putBoolean(KEY_SYSTEM_COLOR, settings.appearance.useSystemColor)
                    .putBoolean(KEY_PURE_BLACK, settings.appearance.pureBlack)
                    .putInt(KEY_PAGE_SIZE, settings.pageSize)
                    .putBoolean(KEY_SHOW_RANGE, settings.showLoadedRange)
                    .putBoolean(KEY_TILE_POSITION, settings.showTilePosition)
                    .putBoolean(KEY_SHOW_COUNTS, settings.showCounts)
                    .commit()
                if (saved) {
                    RepositoryResult.Success(Unit)
                } else {
                    RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
                }
            } catch (_: SecurityException) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            }
        }

    private companion object {
        const val PREFERENCES_NAME = "kaede_gallery_settings"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_SYSTEM_COLOR = "use_system_color"
        const val KEY_PURE_BLACK = "pure_black"
        const val KEY_PAGE_SIZE = "page_size"
        const val KEY_SHOW_RANGE = "show_loaded_range"
        const val KEY_TILE_POSITION = "show_tile_position"
        const val KEY_SHOW_COUNTS = "show_counts"
    }
}
