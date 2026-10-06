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
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
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
    mediaContent: @Composable (GalleryViewerMediaState) -> Unit,
    onExternalLink: (String) -> Unit,
    systemLanguage: LanguagePreference = platformSystemLanguagePreference(),
    dynamicColorScheme: ColorScheme? = null,
    dynamicColorSchemeProvider: (@Composable (ThemePreference) -> ColorScheme?)? = null,
    galleryStrings: GalleryStrings? = null,
    viewerStrings: GalleryViewerStrings? = null,
    settingsStrings: GallerySettingsStrings? = null,
    onRescan: (suspend () -> RepositoryResult<*>)? = null,
    onChangeVault: (() -> Unit)? = null,
    onForgetVault: (suspend () -> RepositoryResult<*>)? = null,
    onImportSettings: (((String) -> Unit) -> Unit)? = null,
    onExportSettings: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val settingsStateHolder = remember(settingsRepository, scope) {
        SettingsStateHolder(settingsRepository, scope)
    }
    val settingsState by settingsStateHolder.state.collectAsState()
    var selectedEntry by remember { mutableStateOf<GalleryEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(settingsStateHolder) { settingsStateHolder.load() }
    DisposableEffect(settingsStateHolder) {
        onDispose(settingsStateHolder::dispose)
    }

    val settings = settingsState.settings
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
                    onImportSettings = onImportSettings,
                    onExportSettings = onExportSettings,
                )
                selectedEntry != null -> {
                    val entry = requireNotNull(selectedEntry)
                    val viewerStateHolder = remember(repository, scope, entry) {
                        GalleryViewerStateHolder(repository, scope, entry)
                    }
                    GalleryViewerScreen(
                        stateHolder = viewerStateHolder,
                        onClose = { selectedEntry = null },
                        strings = activeViewerStrings,
                        mediaContent = mediaContent,
                        onExternalLink = onExternalLink,
                    )
                }
                else -> GalleryScreen(
                    stateHolder = galleryStateHolder,
                    onEntrySelected = { selectedEntry = it },
                    onSettings = { showSettings = true },
                    strings = activeGalleryStrings,
                    showTilePosition = settings.showTilePosition,
                    showLoadedRange = settings.showLoadedRange,
                    showCounts = settings.showCounts,
                    onRescan = onRescan,
                    onChangeVault = onChangeVault,
                    onForgetVault = onForgetVault,
                )
            }
        }
    }
}
