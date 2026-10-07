package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hanaretamae.kaede.core.model.GalleryEntry
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.GalleryScanSummary
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureCodec
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.platformSystemLanguagePreference
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import com.hanaretamae.kaede.core.settings.resolve
import com.hanaretamae.kaede.core.settings.ThemePreference

@Composable
fun KaedeGalleryApp(
    repository: GalleryRepository,
    settingsRepository: SettingsRepository,
    galleryThumbnail: @Composable (MediaId, Boolean) -> Unit = { _, _ -> },
    mediaContent: @Composable (GalleryViewerMediaState) -> Unit,
    onExternalLink: (String) -> Unit,
    loadLicenseText: (String) -> String? = { null },
    systemLanguage: LanguagePreference = platformSystemLanguagePreference(),
    dynamicColorScheme: ColorScheme? = null,
    dynamicColorSchemeProvider: (@Composable (ThemePreference) -> ColorScheme?)? = null,
    galleryStrings: GalleryStrings? = null,
    viewerStrings: GalleryViewerStrings? = null,
    settingsStrings: GallerySettingsStrings? = null,
    onViewerFullscreenChanged: ((Boolean) -> Unit)? = null,
    onViewerMediaOnlyChanged: ((Boolean) -> Unit)? = null,
    onOpenMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onRevealMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onSetWallpaperMedia: ((GalleryViewerMediaState) -> Unit)? = null,
    onOpenVaultNote: ((String) -> Unit)? = null,
    onRescan: (suspend () -> RepositoryResult<*>)? = null,
    onChangeVault: (() -> Unit)? = null,
    onForgetVault: (suspend () -> RepositoryResult<*>)? = null,
    vaultName: String? = null,
    scanWarningCount: Int? = null,
    onRustTagSettingsChanged: (suspend (GallerySettings) -> RepositoryResult<*>)? = null,
    onImportSettings: (((String) -> Unit) -> Unit)? = null,
    onExportSettings: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val settingsStateHolder = remember(settingsRepository, scope) {
        SettingsStateHolder(settingsRepository, scope)
    }
    val settingsState by settingsStateHolder.state.collectAsState()
    var selectedEntries by remember { mutableStateOf<List<GalleryEntry>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var latestScanWarningCount by remember(repository) { mutableStateOf(scanWarningCount) }
    LaunchedEffect(repository, scanWarningCount) {
        latestScanWarningCount = scanWarningCount
    }
    fun recordScanSummary(result: RepositoryResult<*>): RepositoryResult<*> {
        if (result is RepositoryResult.Success<*>) {
            val summary = result.value as? GalleryScanSummary
            if (summary != null) {
                latestScanWarningCount = summary.warnings
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            }
        }
        return result
    }
    val rescanWithSummary: (suspend () -> RepositoryResult<*>)? = onRescan?.let { rescan ->
        suspend { recordScanSummary(rescan()) }
    }
    val rustTagSettingsWithSummary:
        (suspend (GallerySettings) -> RepositoryResult<*>)? = onRustTagSettingsChanged?.let {
        update ->
        suspend { settings -> recordScanSummary(update(settings)) }
    }
    LaunchedEffect(settingsStateHolder) { settingsStateHolder.load() }
    DisposableEffect(settingsStateHolder) {
        onDispose(settingsStateHolder::dispose)
    }

    val settings = settingsState.settings
    val tagColorRules = remember(settings?.flutterTagSettingsJson) {
        try {
            GalleryTagColorCodec.decodeRules(settings?.flutterTagSettingsJson)
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
    val noteStructure = remember(settings?.flutterTagSettingsJson, settings?.galleryTagPrefixes) {
        val prefixes = settings?.galleryTagPrefixes ?: GalleryNoteStructureSettings().galleryTagPrefixes
        try {
            GalleryNoteStructureCodec.decodeSettings(settings?.flutterTagSettingsJson, prefixes)
        } catch (_: IllegalArgumentException) {
            GalleryNoteStructureSettings(galleryTagPrefixes = prefixes)
        }
    }
    var appliedRustTagSettings by remember(repository) {
        mutableStateOf<Pair<List<String>, String?>?>(null)
    }
    LaunchedEffect(
        settings?.galleryTagPrefixes,
        settings?.flutterTagSettingsJson,
        settingsState.saving,
        settingsState.hasUnsavedChanges,
        settingsState.error,
        onRustTagSettingsChanged,
    ) {
        val currentSettings = settings ?: return@LaunchedEffect
        if (settingsState.saving || settingsState.hasUnsavedChanges || settingsState.error != null) {
            return@LaunchedEffect
        }
        val currentRustTagSettings =
            currentSettings.galleryTagPrefixes.toList() to currentSettings.flutterTagSettingsJson
        val applied = appliedRustTagSettings
        if (applied == null) {
            appliedRustTagSettings = currentRustTagSettings
        } else if (applied != currentRustTagSettings) {
            val apply = rustTagSettingsWithSummary ?: return@LaunchedEffect
            when (val result = apply(currentSettings)) {
                is RepositoryResult.Success<*> -> {
                    appliedRustTagSettings = currentRustTagSettings
                }
                is RepositoryResult.Failure -> settingsStateHolder.reportError(result.error)
            }
        }
    }
    val language = (settings?.appearance?.language ?: LanguagePreference.SYSTEM)
        .resolve(systemLanguage)
    val japanese = language == LanguagePreference.JAPANESE
    val activeGalleryStrings = galleryStrings
        ?: if (japanese) JapaneseGalleryStrings else EnglishGalleryStrings
    val activeViewerStrings = viewerStrings
        ?: if (japanese) JapaneseGalleryViewerStrings else EnglishGalleryViewerStrings
    val activeSettingsStrings = settingsStrings
        ?: if (japanese) JapaneseGallerySettingsStrings else EnglishGallerySettingsStrings
    val activeDynamicColorScheme = dynamicColorSchemeProvider?.invoke(
        settings?.appearance?.theme ?: ThemePreference.SYSTEM,
    ) ?: dynamicColorScheme

    KaedeGalleryTheme(
        appearance = settings?.appearance ?: AppearanceSettings(),
        dynamicColorScheme = activeDynamicColorScheme,
    ) {
        if (settings == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (settingsState.loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            settingsState.error?.let(activeGalleryStrings.error)
                                ?: activeGalleryStrings.loading,
                        )
                        TextButton(onClick = settingsStateHolder::load) {
                            Text(activeSettingsStrings.retry)
                        }
                    }
                }
            }
        } else {
            val galleryStateHolder = remember(repository, scope, settings.pageSize) {
                GalleryStateHolder(
                    repository = repository,
                    scope = scope,
                    initialQuery = GalleryQuery(pageSize = settings.pageSize),
                )
            }
            when {
                showSettings -> GallerySettingsScreen(
                    stateHolder = settingsStateHolder,
                    onClose = { showSettings = false },
                    strings = activeSettingsStrings,
                    loadOnEnter = false,
                    onExternalLink = onExternalLink,
                    loadLicenseText = loadLicenseText,
                    onImportSettings = onImportSettings,
                    onExportSettings = onExportSettings,
                    onRescan = rescanWithSummary,
                    onChangeVault = onChangeVault,
                    onForgetVault = onForgetVault,
                    vaultName = vaultName,
                    scanWarningCount = latestScanWarningCount,
                )
                selectedEntries.isNotEmpty() -> {
                    val entry = selectedEntries.last()
                    val viewerStateHolder = remember(repository, scope, entry) {
                        GalleryViewerStateHolder(repository, scope, entry)
                    }
                    GalleryViewerScreen(
                        stateHolder = viewerStateHolder,
                        onClose = { selectedEntries = selectedEntries.dropLast(1) },
                        strings = activeViewerStrings,
                        onFullscreenChanged = onViewerFullscreenChanged,
                        onMediaOnlyChanged = onViewerMediaOnlyChanged,
                        onOpenMedia = onOpenMedia,
                        onRevealMedia = onRevealMedia,
                        onSetWallpaperMedia = onSetWallpaperMedia,
                        onOpenVaultNote = onOpenVaultNote,
                        mediaContent = mediaContent,
                        onExternalLink = onExternalLink,
                        onOpenNote = { noteId ->
                            selectedEntries = selectedEntries + noteId.asNoteEntry()
                        },
                        tagColorRules = tagColorRules,
                        noteStructure = noteStructure,
                    )
                }
                else -> GalleryScreen(
                    stateHolder = galleryStateHolder,
                    onEntrySelected = { selectedEntries = selectedEntries + it },
                    onSettings = { showSettings = true },
                    strings = activeGalleryStrings,
                    showTilePosition = settings.showTilePosition,
                    showLoadedRange = settings.showLoadedRange,
                    showCounts = settings.showCounts,
                    showMissingMediaIcon = settings.showMissingMediaIcon,
                    includedTagPrefixes = settings.includedTagPrefixes,
                    hiddenTagPrefixes = settings.hiddenTagPrefixes,
                    tagColorRules = tagColorRules,
                    galleryThumbnail = galleryThumbnail,
                    onRescan = rescanWithSummary,
                    onChangeVault = onChangeVault,
                    onForgetVault = onForgetVault,
                )
            }
        }

    }
}

private fun NoteId.asNoteEntry() = NoteSummary(
    id = this,
    path = "",
    title = "",
    representativeMediaId = null,
    mediaCount = 0,
    videoCount = 0,
    memoCount = 0,
    relatedCount = 0,
)
