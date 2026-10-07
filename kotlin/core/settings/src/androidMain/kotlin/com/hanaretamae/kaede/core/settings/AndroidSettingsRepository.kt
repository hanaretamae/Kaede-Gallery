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
            val galleryTagPrefixes = preferences.getString(
                KEY_GALLERY_TAG_PREFIXES,
                GalleryTagPrefixesCodec.encodeStorage(GalleryTagPrefixesCodec.DEFAULT_PREFIXES),
            )?.let(GalleryTagPrefixesCodec::decodeStorage)
                ?: return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val includedTagPrefixes = preferences.getString(
                KEY_INCLUDED_TAG_PREFIXES,
                GalleryTagDisplayPrefixesCodec.encodeStorage(
                    GalleryTagDisplayPrefixesCodec.DEFAULT_INCLUDED,
                ),
            )?.let(GalleryTagDisplayPrefixesCodec::decodeStorage)
                ?: return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val hiddenTagPrefixes = preferences.getString(
                KEY_HIDDEN_TAG_PREFIXES,
                GalleryTagDisplayPrefixesCodec.encodeStorage(
                    GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN,
                ),
            )?.let(GalleryTagDisplayPrefixesCodec::decodeStorage)
                ?: return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val flutterTagSettingsJson = preferences.getString(KEY_FLUTTER_TAG_SETTINGS, null)
            if (
                flutterTagSettingsJson != null &&
                flutterTagSettingsJson.encodeToByteArray().size >
                SettingsTransferCodec.MAX_BYTES - 1024
            ) {
                return@withContext RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            }
            RepositoryResult.Success(
                GallerySettings(
                    appearance = AppearanceSettings(
                        theme = theme,
                        language = language,
                        useSystemColor = preferences.getBoolean(
                            KEY_SYSTEM_COLOR,
                            AppearanceSettings().useSystemColor,
                        ),
                        pureBlack = preferences.getBoolean(KEY_PURE_BLACK, false),
                    ),
                    pageSize = pageSize,
                    galleryTagPrefixes = galleryTagPrefixes,
                    includedTagPrefixes = includedTagPrefixes,
                    hiddenTagPrefixes = hiddenTagPrefixes,
                    showMissingMediaIcon = preferences.getBoolean(KEY_MISSING_MEDIA_ICON, false),
                    showLoadedRange = preferences.getBoolean(KEY_SHOW_RANGE, true),
                    showTilePosition = preferences.getBoolean(KEY_TILE_POSITION, false),
                    showCounts = preferences.getBoolean(KEY_SHOW_COUNTS, true),
                    flutterTagSettingsJson = flutterTagSettingsJson,
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
                    .putString(
                        KEY_GALLERY_TAG_PREFIXES,
                        GalleryTagPrefixesCodec.encodeStorage(settings.galleryTagPrefixes),
                    )
                    .putString(
                        KEY_INCLUDED_TAG_PREFIXES,
                        GalleryTagDisplayPrefixesCodec.encodeStorage(
                            settings.includedTagPrefixes,
                        ),
                    )
                    .putString(
                        KEY_HIDDEN_TAG_PREFIXES,
                        GalleryTagDisplayPrefixesCodec.encodeStorage(settings.hiddenTagPrefixes),
                    )
                    .putBoolean(KEY_MISSING_MEDIA_ICON, settings.showMissingMediaIcon)
                    .putBoolean(KEY_SHOW_RANGE, settings.showLoadedRange)
                    .putBoolean(KEY_TILE_POSITION, settings.showTilePosition)
                    .putBoolean(KEY_SHOW_COUNTS, settings.showCounts)
                    .also {
                        val tagSettings = settings.flutterTagSettingsJson
                        if (tagSettings == null) {
                            it.remove(KEY_FLUTTER_TAG_SETTINGS)
                        } else {
                            it.putString(KEY_FLUTTER_TAG_SETTINGS, tagSettings)
                        }
                    }
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
        const val KEY_GALLERY_TAG_PREFIXES = "gallery_tag_prefixes"
        const val KEY_INCLUDED_TAG_PREFIXES = "included_tag_prefixes"
        const val KEY_HIDDEN_TAG_PREFIXES = "hidden_tag_prefixes"
        const val KEY_MISSING_MEDIA_ICON = "show_missing_media_icon"
        const val KEY_SHOW_RANGE = "show_loaded_range"
        const val KEY_TILE_POSITION = "show_tile_position"
        const val KEY_SHOW_COUNTS = "show_counts"
        const val KEY_FLUTTER_TAG_SETTINGS = "flutter_tag_settings_json"
    }
}
