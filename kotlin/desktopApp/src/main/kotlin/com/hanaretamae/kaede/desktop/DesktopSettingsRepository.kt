package com.hanaretamae.kaede.desktop

import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.GalleryTagDisplayPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.core.settings.SettingsTransferCodec
import com.hanaretamae.kaede.core.settings.ThemePreference
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.Properties
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopSettingsRepository(
    private val settingsFile: Path,
) : SettingsRepository {
    override suspend fun load(): RepositoryResult<GallerySettings> =
        withContext(Dispatchers.IO) {
            try {
                if (!hasPrivateParent()) {
                    return@withContext RepositoryResult.Failure(
                        RepositoryError.STORAGE_UNAVAILABLE,
                    )
                }
                if (!Files.exists(settingsFile, LinkOption.NOFOLLOW_LINKS)) {
                    return@withContext RepositoryResult.Success(GallerySettings())
                }
                if (
                    Files.isSymbolicLink(settingsFile) ||
                    !Files.isRegularFile(settingsFile, LinkOption.NOFOLLOW_LINKS) ||
                    Files.size(settingsFile) > MAX_SETTINGS_BYTES
                ) {
                    return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                }
                val properties = Properties()
                Files.newInputStream(settingsFile).use(properties::load)
                val defaults = GallerySettings()
                val theme = properties.getProperty(KEY_THEME, defaults.appearance.theme.name)
                    .let { value -> ThemePreference.entries.firstOrNull { it.name == value } }
                    ?: return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                val language =
                    properties.getProperty(KEY_LANGUAGE, defaults.appearance.language.name)
                        .let { value ->
                            LanguagePreference.entries.firstOrNull { it.name == value }
                        }
                        ?: return@withContext RepositoryResult.Failure(
                            RepositoryError.OPERATION_FAILED,
                        )
                val pageSizeText = properties.getProperty(KEY_PAGE_SIZE)
                val pageSize = if (pageSizeText == null) {
                    defaults.pageSize
                } else {
                    pageSizeText.toIntOrNull()
                        ?: return@withContext RepositoryResult.Failure(
                            RepositoryError.OPERATION_FAILED,
                        )
                }
                val useSystemColor = properties.strictBoolean(
                    KEY_SYSTEM_COLOR,
                    defaults.appearance.useSystemColor,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val pureBlack = properties.strictBoolean(
                    KEY_PURE_BLACK,
                    defaults.appearance.pureBlack,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val showRange = properties.strictBoolean(
                    KEY_SHOW_RANGE,
                    defaults.showLoadedRange,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val showTilePosition = properties.strictBoolean(
                    KEY_TILE_POSITION,
                    defaults.showTilePosition,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val showCounts = properties.strictBoolean(
                    KEY_SHOW_COUNTS,
                    defaults.showCounts,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val showMissingMediaIcon = properties.strictBoolean(
                    KEY_MISSING_MEDIA_ICON,
                    defaults.showMissingMediaIcon,
                ) ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.OPERATION_FAILED,
                )
                val flutterTagSettingsJson = properties.getProperty(KEY_FLUTTER_TAG_SETTINGS)
                    ?.takeIf {
                        it.encodeToByteArray().size <= SettingsTransferCodec.MAX_BYTES - 1024
                    }
                    ?: properties.getProperty(KEY_FLUTTER_TAG_SETTINGS)?.let {
                        return@withContext RepositoryResult.Failure(
                            RepositoryError.OPERATION_FAILED,
                        )
                    }
                val galleryTagPrefixes = properties.getProperty(
                    KEY_GALLERY_TAG_PREFIXES,
                    GalleryTagPrefixesCodec.encodeStorage(defaults.galleryTagPrefixes),
                ).let(GalleryTagPrefixesCodec::decodeStorage)
                    ?: return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                val includedTagPrefixes = properties.getProperty(
                    KEY_INCLUDED_TAG_PREFIXES,
                    GalleryTagDisplayPrefixesCodec.encodeStorage(defaults.includedTagPrefixes),
                ).let(GalleryTagDisplayPrefixesCodec::decodeStorage)
                    ?: return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                val hiddenTagPrefixes = properties.getProperty(
                    KEY_HIDDEN_TAG_PREFIXES,
                    GalleryTagDisplayPrefixesCodec.encodeStorage(defaults.hiddenTagPrefixes),
                ).let(GalleryTagDisplayPrefixesCodec::decodeStorage)
                    ?: return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                if (pageSize !in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE) {
                    return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                }
                RepositoryResult.Success(
                    GallerySettings(
                        appearance = AppearanceSettings(
                            theme = theme,
                            language = language,
                            useSystemColor = useSystemColor,
                            pureBlack = pureBlack,
                        ),
                        pageSize = pageSize,
                        galleryTagPrefixes = galleryTagPrefixes,
                        includedTagPrefixes = includedTagPrefixes,
                        hiddenTagPrefixes = hiddenTagPrefixes,
                        showMissingMediaIcon = showMissingMediaIcon,
                        showLoadedRange = showRange,
                        showTilePosition = showTilePosition,
                        showCounts = showCounts,
                        flutterTagSettingsJson = flutterTagSettingsJson,
                    ),
                )
            } catch (_: SecurityException) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            } catch (_: IOException) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            }
        }

    override suspend fun save(settings: GallerySettings): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val parent = settingsFile.parent
                ?: return@withContext RepositoryResult.Failure(
                    RepositoryError.STORAGE_UNAVAILABLE,
                )
            val temporary = parent.resolve(".settings-${UUID.randomUUID()}.tmp")
            val result = try {
                when {
                    !hasPrivateParent() ->
                        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
                    Files.exists(settingsFile, LinkOption.NOFOLLOW_LINKS) &&
                        (
                            Files.isSymbolicLink(settingsFile) ||
                                !Files.isRegularFile(settingsFile, LinkOption.NOFOLLOW_LINKS)
                            ) -> RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
                    else -> {
                        val properties = Properties().apply {
                            setProperty(KEY_THEME, settings.appearance.theme.name)
                            setProperty(KEY_LANGUAGE, settings.appearance.language.name)
                            setProperty(
                                KEY_SYSTEM_COLOR,
                                settings.appearance.useSystemColor.toString(),
                            )
                            setProperty(KEY_PURE_BLACK, settings.appearance.pureBlack.toString())
                            setProperty(KEY_PAGE_SIZE, settings.pageSize.toString())
                            setProperty(
                                KEY_GALLERY_TAG_PREFIXES,
                                GalleryTagPrefixesCodec.encodeStorage(settings.galleryTagPrefixes),
                            )
                            setProperty(
                                KEY_INCLUDED_TAG_PREFIXES,
                                GalleryTagDisplayPrefixesCodec.encodeStorage(
                                    settings.includedTagPrefixes,
                                ),
                            )
                            setProperty(
                                KEY_HIDDEN_TAG_PREFIXES,
                                GalleryTagDisplayPrefixesCodec.encodeStorage(
                                    settings.hiddenTagPrefixes,
                                ),
                            )
                            setProperty(
                                KEY_MISSING_MEDIA_ICON,
                                settings.showMissingMediaIcon.toString(),
                            )
                            setProperty(KEY_SHOW_RANGE, settings.showLoadedRange.toString())
                            setProperty(KEY_TILE_POSITION, settings.showTilePosition.toString())
                            setProperty(KEY_SHOW_COUNTS, settings.showCounts.toString())
                            settings.flutterTagSettingsJson?.let {
                                setProperty(KEY_FLUTTER_TAG_SETTINGS, it)
                            }
                        }
                        createPrivateFile(temporary)
                        Files.newOutputStream(temporary).use { output ->
                            properties.store(output, null)
                            output.flush()
                        }
                        try {
                            Files.move(
                                temporary,
                                settingsFile,
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                        } catch (_: AtomicMoveNotSupportedException) {
                            Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING)
                        }
                        RepositoryResult.Success(Unit)
                    }
                }
            } catch (_: SecurityException) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            } catch (_: IOException) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            }
            val cleanupFailed = try {
                Files.deleteIfExists(temporary)
                false
            } catch (_: SecurityException) {
                true
            } catch (_: IOException) {
                true
            }
            if (cleanupFailed) {
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            } else {
                result
            }
        }

    private fun hasPrivateParent(): Boolean {
        val parent = settingsFile.parent ?: return false
        return !Files.isSymbolicLink(parent) &&
            Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
    }

    private fun createPrivateFile(path: Path) {
        try {
            Files.createFile(
                path,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")),
            )
        } catch (_: UnsupportedOperationException) {
            Files.createFile(path)
        }
    }

    private companion object {
        const val MAX_SETTINGS_BYTES = 128 * 1024L
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

private fun Properties.strictBoolean(key: String, default: Boolean): Boolean? =
    when (getProperty(key) ?: default.toString()) {
        "true" -> true
        "false" -> false
        else -> null
    }
