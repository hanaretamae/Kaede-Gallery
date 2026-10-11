@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.ui.gallery

import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs

private sealed interface GalleryDestination {
    data object Gallery : GalleryDestination
    data object Settings : GalleryDestination
    data class Viewer(val entries: List<GalleryEntry>) : GalleryDestination
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun KaedeGalleryApp(
    repository: GalleryRepository,
    settingsRepository: SettingsRepository,
    galleryThumbnail: @Composable (MediaId, Boolean, Boolean) -> Unit = { _, _, _ -> },
    mediaContent: @Composable (GalleryViewerMediaState) -> Unit,
    onExternalLink: (String) -> Unit,
    loadLicenseText: (String) -> String? = { null },
    systemLanguage: LanguagePreference = platformSystemLanguagePreference(),
    dynamicColorScheme: ColorScheme? = null,
    dynamicColorSchemeProvider: (@Composable (ThemePreference) -> ColorScheme?)? = null,
    systemDarkTheme: Boolean? = null,
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
    rescanOnStart: Boolean = false,
    onChangeVault: (() -> Unit)? = null,
    onForgetVault: (suspend () -> RepositoryResult<*>)? = null,
    vaultName: String? = null,
    scanWarningCount: Int? = null,
    onRustTagSettingsChanged: (suspend (GallerySettings) -> RepositoryResult<*>)? = null,
    onImportSettings: (((String) -> Unit) -> Unit)? = null,
    onExportSettings: ((String) -> Unit)? = null,
    onBackHandlerChanged: (((() -> Unit)?) -> Unit)? = null,
    backGestureProgress: Float = 0f,
    onReady: () -> Unit = {},
    settingsStateHolder: SettingsStateHolder? = null,
) {
    val scope = rememberCoroutineScope()
    val ownsSettingsStateHolder = settingsStateHolder == null
    val activeSettingsStateHolder = remember(settingsRepository, scope, settingsStateHolder) {
        settingsStateHolder ?: SettingsStateHolder(settingsRepository, scope)
    }
    val settingsState by activeSettingsStateHolder.state.collectAsState()
    var selectedEntries by remember { mutableStateOf<List<GalleryEntry>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var viewerBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }
    var settingsBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }
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
    LaunchedEffect(activeSettingsStateHolder, ownsSettingsStateHolder) {
        if (ownsSettingsStateHolder) activeSettingsStateHolder.load()
    }
    DisposableEffect(activeSettingsStateHolder, ownsSettingsStateHolder) {
        if (ownsSettingsStateHolder) {
            onDispose(activeSettingsStateHolder::dispose)
        } else {
            onDispose {}
        }
    }

    val settings = settingsState.settings
    val settingsResolved = settings != null || !settingsState.loading
    LaunchedEffect(settingsResolved) { if (settingsResolved) onReady() }
    val latestShowSettings = rememberUpdatedState(showSettings)
    val latestSelectedEntries = rememberUpdatedState(selectedEntries)
    val systemBackHandler = remember {
        {
            when {
                latestShowSettings.value -> showSettings = false
                latestSelectedEntries.value.isNotEmpty() -> {
                    selectedEntries = latestSelectedEntries.value.dropLast(1)
                }
            }
        }
    }
    val canHandleSystemBack = showSettings || selectedEntries.isNotEmpty()
    SideEffect {
        onBackHandlerChanged?.invoke(
            if (showSettings) {
                settingsBackHandler ?: systemBackHandler.takeIf { canHandleSystemBack }
            } else {
                viewerBackHandler ?: systemBackHandler.takeIf { canHandleSystemBack }
            },
        )
    }
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
                is RepositoryResult.Failure -> activeSettingsStateHolder.reportError(result.error)
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
        systemDarkTheme = systemDarkTheme,
    ) {
        if (settings == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (settingsState.loading) {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            settingsState.error?.let(activeGalleryStrings.error)
                                ?: activeGalleryStrings.loading,
                        )
                        TextButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = activeSettingsStateHolder::load,
                        ) {
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
            DisposableEffect(galleryStateHolder) {
                onDispose(galleryStateHolder::dispose)
            }
            val galleryGridState = rememberLazyGridState()
            val motionScheme = remember { MotionScheme.expressive() }
            val destination = when {
                showSettings -> GalleryDestination.Settings
                selectedEntries.isNotEmpty() -> GalleryDestination.Viewer(selectedEntries.toList())
                else -> GalleryDestination.Gallery
            }
            val rootBackGestureProgress =
                if (showSettings && settingsBackHandler != null) 0f else backGestureProgress
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val progress = rootBackGestureProgress.coerceIn(-1f, 1f)
                    translationX = size.width * progress
                    alpha = 1f - abs(progress) * 0.08f
                },
                transitionSpec = {
                    val target = targetState
                    val initial = initialState
                    val forward = when {
                        target is GalleryDestination.Viewer &&
                            initial !is GalleryDestination.Viewer -> true
                        target is GalleryDestination.Viewer &&
                            initial is GalleryDestination.Viewer ->
                            target.entries.size > initial.entries.size
                        target is GalleryDestination.Settings &&
                            initial !is GalleryDestination.Settings -> true
                        else -> false
                    }
                    if (
                        target is GalleryDestination.Viewer &&
                        initial !is GalleryDestination.Viewer
                    ) {
                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                            fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                    } else if (forward) {
                        (
                            slideInHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) { it / 3 } +
                                fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
                            ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) { -it / 3 } +
                                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                            )
                    } else {
                        (
                            slideInHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) { -it / 3 } +
                                fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
                            ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                            ) { it / 3 } +
                                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                            )
                    }
                },
                label = "gallery-navigation",
            ) { currentDestination ->
                when (currentDestination) {
                    GalleryDestination.Settings -> GallerySettingsScreen(
                        stateHolder = activeSettingsStateHolder,
                        onClose = { showSettings = false },
                        strings = activeSettingsStrings,
                        loadOnEnter = false,
                        disposeStateHolderOnExit = false,
                        onExternalLink = onExternalLink,
                        loadLicenseText = loadLicenseText,
                        onImportSettings = onImportSettings,
                        onExportSettings = onExportSettings,
                        onRescan = rescanWithSummary,
                        onChangeVault = onChangeVault,
                        onForgetVault = onForgetVault,
                        vaultName = vaultName,
                        scanWarningCount = latestScanWarningCount,
                        backGestureProgress = if (settingsBackHandler != null) {
                            backGestureProgress
                        } else {
                            0f
                        },
                        onBackHandlerChanged = { backHandler ->
                            settingsBackHandler = backHandler
                            if (showSettings) {
                                onBackHandlerChanged?.invoke(
                                    backHandler
                                        ?: systemBackHandler.takeIf { canHandleSystemBack },
                                )
                            }
                        },
                    )
                    is GalleryDestination.Viewer -> {
                    val entry = currentDestination.entries.last()
                    val viewerStateHolder = remember(repository, scope, entry) {
                        GalleryViewerStateHolder(repository, scope, entry)
                    }
                    GalleryViewerScreen(
                        stateHolder = viewerStateHolder,
                        onClose = {
                            viewerBackHandler = null
                            selectedEntries = selectedEntries.dropLast(1)
                        },
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
                        onBackHandlerChanged = { backHandler ->
                            viewerBackHandler = backHandler
                            onBackHandlerChanged?.invoke(
                                backHandler ?: systemBackHandler.takeIf { canHandleSystemBack },
                            )
                        },
                        tagColorRules = tagColorRules,
                        hiddenTagPrefixes = settings.hiddenTagPrefixes,
                        noteStructure = noteStructure,
                    )
                }
                    GalleryDestination.Gallery -> GalleryScreen(
                        stateHolder = galleryStateHolder,
                        gridState = galleryGridState,
                        onEntrySelected = { selectedEntries = selectedEntries + it },
                        onSettings = { showSettings = true },
                        vaultName = vaultName,
                        strings = activeGalleryStrings,
                        showTilePosition = settings.showTilePosition,
                        showLoadedRange = settings.showLoadedRange,
                        showCounts = settings.showCounts,
                        showMissingMediaIcon = settings.showMissingMediaIcon,
                        fixedColumnCount = settings.fixedColumnCount,
                        includedTagPrefixes = settings.includedTagPrefixes,
                        hiddenTagPrefixes = settings.hiddenTagPrefixes,
                        tagColorRules = tagColorRules,
                        galleryThumbnail = galleryThumbnail,
                        onRescan = rescanWithSummary,
                        rescanOnStart = rescanOnStart,
                    )
                }
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
