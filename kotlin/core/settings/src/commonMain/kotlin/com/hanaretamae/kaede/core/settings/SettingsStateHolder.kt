package com.hanaretamae.kaede.core.settings

import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsUiState(
    val settings: GallerySettings? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: RepositoryError? = null,
)

class SettingsStateHolder(
    private val repository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()
    private val saveMutex = Mutex()
    private var loadJob: Job? = null
    private var loadRevision = 0L
    private var pendingSettings: GallerySettings? = null

    fun load() {
        loadJob?.cancel()
        val revision = ++loadRevision
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        loadJob = scope.launch {
            saveMutex.withLock {
                if (revision != loadRevision) return@withLock
                when (val result = repository.load()) {
                    is RepositoryResult.Success -> {
                        if (revision == loadRevision) {
                            mutableState.value = SettingsUiState(
                                settings = result.value,
                                loading = false,
                            )
                            pendingSettings = null
                        }
                    }
                    is RepositoryResult.Failure -> {
                        if (revision == loadRevision) {
                            mutableState.value = mutableState.value.copy(
                                loading = false,
                                error = result.error,
                            )
                        }
                    }
                }
            }
        }
    }

    fun setAppearance(appearance: AppearanceSettings) {
        update { it.copy(appearance = appearance) }
    }

    fun setTheme(theme: ThemePreference) {
        update { it.copy(appearance = it.appearance.copy(theme = theme)) }
    }

    fun setLanguage(language: LanguagePreference) {
        update { it.copy(appearance = it.appearance.copy(language = language)) }
    }

    fun setUseSystemColor(enabled: Boolean) {
        update { it.copy(appearance = it.appearance.copy(useSystemColor = enabled)) }
    }

    fun setPureBlack(enabled: Boolean) {
        update { it.copy(appearance = it.appearance.copy(pureBlack = enabled)) }
    }

    fun setPageSize(pageSize: Int) {
        if (pageSize !in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return
        }
        update { it.copy(pageSize = pageSize) }
    }

    fun setGalleryTagPrefixes(prefixes: List<String>) {
        if (!GalleryTagPrefixesCodec.isValid(prefixes)) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return
        }
        update { it.copy(galleryTagPrefixes = prefixes.toList()) }
    }

    suspend fun saveGalleryTagPrefixes(prefixes: List<String>): RepositoryResult<Unit> {
        if (!GalleryTagPrefixesCodec.isValid(prefixes)) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        return saveMutex.withLock {
            val current = mutableState.value.settings
                ?: return@withLock RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            persistLocked(current.copy(galleryTagPrefixes = prefixes.toList()))
        }
    }

    suspend fun saveTagDisplayPrefixes(
        includedPrefixes: List<String>,
        hiddenPrefixes: List<String>,
    ): RepositoryResult<Unit> {
        if (
            !GalleryTagDisplayPrefixesCodec.isValid(includedPrefixes) ||
            !GalleryTagDisplayPrefixesCodec.isValid(hiddenPrefixes)
        ) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        return saveMutex.withLock {
            val current = mutableState.value.settings
                ?: return@withLock RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            persistLocked(
                current.copy(
                    includedTagPrefixes = includedPrefixes.toList(),
                    hiddenTagPrefixes = hiddenPrefixes.toList(),
                ),
            )
        }
    }

    suspend fun saveTagColorRules(rules: List<GalleryTagColorRule>): RepositoryResult<Unit> {
        if (!GalleryTagColorCodec.isValid(rules)) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        return saveMutex.withLock {
            val current = mutableState.value.settings
                ?: return@withLock RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val updated = try {
                current.copy(
                    flutterTagSettingsJson = GalleryTagColorCodec.update(
                        current.flutterTagSettingsJson,
                        rules,
                    ),
                )
            } catch (_: IllegalArgumentException) {
                mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
                return@withLock RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
            }
            persistLocked(updated)
        }
    }

    suspend fun saveTagCategorySettings(
        categorySettings: GalleryTagCategorySettings,
    ): RepositoryResult<Unit> {
        if (!GalleryTagCategoryCodec.isValid(categorySettings)) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        return saveMutex.withLock {
            val current = mutableState.value.settings
                ?: return@withLock RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val updated = try {
                current.copy(
                    flutterTagSettingsJson = GalleryTagCategoryCodec.update(
                        current.flutterTagSettingsJson,
                        categorySettings,
                    ),
                )
            } catch (_: IllegalArgumentException) {
                mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
                return@withLock RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
            }
            persistLocked(updated)
        }
    }

    suspend fun saveNoteStructureSettings(
        noteStructure: GalleryNoteStructureSettings,
    ): RepositoryResult<Unit> {
        return saveMutex.withLock {
            val current = mutableState.value.settings
                ?: return@withLock RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            val settings = noteStructure.copy(galleryTagPrefixes = current.galleryTagPrefixes)
            if (!GalleryNoteStructureCodec.isValid(settings)) {
                mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
                return@withLock RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
            }
            val updated = try {
                current.copy(
                    flutterTagSettingsJson = GalleryNoteStructureCodec.update(
                        current.flutterTagSettingsJson,
                        settings,
                    ),
                )
            } catch (_: IllegalArgumentException) {
                mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
                return@withLock RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
            }
            persistLocked(updated)
        }
    }

    fun reportError(error: RepositoryError) {
        mutableState.value = mutableState.value.copy(error = error)
    }

    fun setShowLoadedRange(enabled: Boolean) {
        update { it.copy(showLoadedRange = enabled) }
    }

    fun setShowTilePosition(enabled: Boolean) {
        update { it.copy(showTilePosition = enabled) }
    }

    fun setShowCounts(enabled: Boolean) {
        update { it.copy(showCounts = enabled) }
    }

    fun setShowMissingMediaIcon(enabled: Boolean) {
        update { it.copy(showMissingMediaIcon = enabled) }
    }

    fun reset() {
        update { GallerySettings() }
    }

    fun exportSerialized(): String? {
        val settings = mutableState.value.settings ?: return null
        return try {
            SettingsTransferCodec.encode(settings)
        } catch (_: IllegalArgumentException) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.INVALID_REQUEST)
            null
        }
    }

    fun importSerialized(content: String) {
        val imported = SettingsTransferCodec.decode(content)
        if (imported == null) {
            mutableState.value = mutableState.value.copy(
                error = RepositoryError.INVALID_REQUEST,
            )
            return
        }
        scope.launch {
            saveMutex.withLock { persistLocked(imported) }
        }
    }

    fun retrySave() {
        scope.launch {
            saveMutex.withLock {
                pendingSettings?.let { persistLocked(it) }
            }
        }
    }

    fun dispose() {
        loadRevision++
        loadJob?.cancel()
    }

    private fun update(transform: (GallerySettings) -> GallerySettings) {
        if (mutableState.value.settings == null) {
            mutableState.value = mutableState.value.copy(error = RepositoryError.OPERATION_FAILED)
            return
        }
        scope.launch {
            saveMutex.withLock {
                val currentState = mutableState.value
                val current = currentState.settings
                if (current == null) {
                    mutableState.value = currentState.copy(error = RepositoryError.OPERATION_FAILED)
                    return@withLock
                }
                val updated = transform(current)
                persistLocked(updated)
            }
        }
    }

    private suspend fun persistLocked(updated: GallerySettings): RepositoryResult<Unit> {
        mutableState.value = mutableState.value.copy(saving = true, error = null)
        return when (val result = repository.save(updated)) {
            is RepositoryResult.Success -> {
                pendingSettings = null
                mutableState.value = mutableState.value.copy(
                    settings = updated,
                    saving = false,
                    hasUnsavedChanges = false,
                    error = null,
                )
                result
            }
            is RepositoryResult.Failure -> {
                pendingSettings = updated
                mutableState.value = mutableState.value.copy(
                    settings = updated,
                    saving = false,
                    hasUnsavedChanges = true,
                    error = result.error,
                )
                result
            }
        }
    }
}
