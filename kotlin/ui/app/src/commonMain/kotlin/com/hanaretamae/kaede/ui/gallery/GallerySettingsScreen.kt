package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import com.hanaretamae.kaede.core.settings.ThemePreference

data class GallerySettingsStrings(
    val title: String,
    val close: String,
    val appearance: String,
    val theme: String,
    val system: String,
    val light: String,
    val dark: String,
    val language: String,
    val japanese: String,
    val english: String,
    val systemColor: String,
    val pureBlack: String,
    val pagination: String,
    val about: String,
    val aboutDescription: String,
    val repository: String,
    val license: String,
    val importSettings: String,
    val exportSettings: String,
    val transferSettings: String,
    val pageSize: String,
    val invalidPageSize: String,
    val showLoadedRange: String,
    val showTilePosition: String,
    val showCounts: String,
    val reset: String,
    val save: String,
    val loading: String,
    val retry: String,
    val error: (RepositoryError) -> String,
)

val EnglishGallerySettingsStrings = GallerySettingsStrings(
    title = "Settings",
    close = "Back",
    appearance = "Appearance",
    theme = "Theme",
    system = "System",
    light = "Light",
    dark = "Dark",
    language = "Language",
    japanese = "Japanese",
    english = "English",
    systemColor = "Use system accent color",
    pureBlack = "Pure black in dark mode",
    pagination = "Gallery list",
    about = "About this app",
    aboutDescription = "Kaede Gallery is an offline, read-only gallery for Obsidian Vaults.",
    repository = "Project website",
    license = "License",
    importSettings = "Import settings",
    exportSettings = "Export settings",
    transferSettings = "Settings file",
    pageSize = "Items per page (1–500)",
    invalidPageSize = "Enter a whole number from 1 to 500",
    showLoadedRange = "Show loaded range and total",
    showTilePosition = "Show tile position number",
    showCounts = "Show media, memo, and related counts",
    reset = "Reset to defaults",
    save = "Save page size",
    loading = "Loading settings…",
    retry = "Retry",
    error = ::englishError,
)

val JapaneseGallerySettingsStrings = GallerySettingsStrings(
    title = "設定",
    close = "戻る",
    appearance = "外観",
    theme = "テーマ",
    system = "システム",
    light = "ライト",
    dark = "ダーク",
    language = "言語",
    japanese = "日本語",
    english = "英語",
    systemColor = "システムのアクセントカラーを使用",
    pureBlack = "ダークモードで純黒を使用",
    pagination = "ギャラリー一覧",
    about = "このアプリについて",
    aboutDescription = "Kaede GalleryはObsidian Vault用のオフライン・読み取り専用ギャラリーです。",
    repository = "プロジェクトサイト",
    license = "ライセンス",
    importSettings = "設定をインポート",
    exportSettings = "設定をエクスポート",
    transferSettings = "設定ファイル",
    pageSize = "1ページの項目数（1〜500）",
    invalidPageSize = "1〜500の整数を入力してください",
    showLoadedRange = "読み込み済み件数と総数を表示",
    showTilePosition = "タイルの位置番号を表示",
    showCounts = "メディア・メモ・関連リンク数を表示",
    reset = "既定値に戻す",
    save = "ページ件数を保存",
    loading = "設定を読み込み中…",
    retry = "再試行",
    error = ::japaneseError,
)

@Composable
fun GallerySettingsScreen(
    stateHolder: SettingsStateHolder,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strings: GallerySettingsStrings = EnglishGallerySettingsStrings,
    loadOnEnter: Boolean = true,
    onExternalLink: (String) -> Unit = {},
    onImportSettings: (((String) -> Unit) -> Unit)? = null,
    onExportSettings: ((String) -> Unit)? = null,
) {
    val state by stateHolder.state.collectAsState()
    var pageSizeDraft by remember(state.settings?.pageSize) {
        mutableStateOf(state.settings?.pageSize?.toString().orEmpty())
    }
    if (loadOnEnter) {
        LaunchedEffect(stateHolder) { stateHolder.load() }
    }
    DisposableEffect(stateHolder) {
        onDispose(stateHolder::dispose)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(strings.close) }
            }
            if (state.loading && state.settings == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(strings.loading, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error?.let { error ->
                Text(strings.error(error), color = MaterialTheme.colorScheme.error)
                if (state.settings == null) {
                    TextButton(onClick = stateHolder::load) { Text(strings.retry) }
                } else if (state.hasUnsavedChanges) {
                    TextButton(
                        onClick = stateHolder::retrySave,
                        enabled = !state.saving,
                    ) { Text(strings.retry) }
                }
            }
            state.settings?.let { settings ->
                SettingsHeading(strings.appearance)
                Text(strings.theme, style = MaterialTheme.typography.titleMedium)
                ChoiceRow {
                    ThemePreference.entries.forEach { value ->
                        FilterChip(
                            selected = settings.appearance.theme == value,
                            onClick = { stateHolder.setTheme(value) },
                            label = {
                                Text(
                                    when (value) {
                                        ThemePreference.SYSTEM -> strings.system
                                        ThemePreference.LIGHT -> strings.light
                                        ThemePreference.DARK -> strings.dark
                                    },
                                )
                            },
                        )
                    }
                }
                Text(strings.language, style = MaterialTheme.typography.titleMedium)
                ChoiceRow {
                    LanguagePreference.entries.forEach { value ->
                        FilterChip(
                            selected = settings.appearance.language == value,
                            onClick = { stateHolder.setLanguage(value) },
                            label = {
                                Text(
                                    when (value) {
                                        LanguagePreference.SYSTEM -> strings.system
                                        LanguagePreference.JAPANESE -> strings.japanese
                                        LanguagePreference.ENGLISH -> strings.english
                                    },
                                )
                            },
                        )
                    }
                }
                SettingsSwitch(
                    label = strings.systemColor,
                    checked = settings.appearance.useSystemColor,
                    onCheckedChange = stateHolder::setUseSystemColor,
                )
                if (settings.appearance.theme != ThemePreference.LIGHT) {
                    SettingsSwitch(
                        label = strings.pureBlack,
                        checked = settings.appearance.pureBlack,
                        onCheckedChange = stateHolder::setPureBlack,
                    )
                }

                SettingsHeading(strings.pagination)
                OutlinedTextField(
                    value = pageSizeDraft,
                    onValueChange = { value ->
                        if (value.all { it in '0'..'9' }) pageSizeDraft = value
                    },
                    label = { Text(strings.pageSize) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val pageSize = pageSizeDraft.toIntOrNull()
                if (
                    pageSizeDraft.isNotEmpty() &&
                    (pageSize == null || pageSize !in
                        GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE)
                ) {
                    Text(
                        strings.invalidPageSize,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = { pageSize?.let(stateHolder::setPageSize) },
                    enabled = pageSize != null &&
                        pageSize in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE &&
                        pageSize != settings.pageSize &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }
                SettingsSwitch(
                    label = strings.showLoadedRange,
                    checked = settings.showLoadedRange,
                    onCheckedChange = stateHolder::setShowLoadedRange,
                )
                SettingsSwitch(
                    label = strings.showTilePosition,
                    checked = settings.showTilePosition,
                    onCheckedChange = stateHolder::setShowTilePosition,
                )
                SettingsSwitch(
                    label = strings.showCounts,
                    checked = settings.showCounts,
                    onCheckedChange = stateHolder::setShowCounts,
                )
                TextButton(
                    onClick = stateHolder::reset,
                    enabled = !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.reset) }
                if (state.saving) {
                    Text(strings.loading, style = MaterialTheme.typography.labelMedium)
                }
                SettingsHeading(strings.about)
                Text(strings.aboutDescription)
                TextButton(
                    onClick = { onExternalLink("https://github.com/hanaretamae/Kaede-Gallery") },
                ) {
                    Text(strings.repository)
                }
                TextButton(
                    onClick = {
                        onExternalLink("https://github.com/hanaretamae/Kaede-Gallery/blob/main/LICENSE")
                    },
                ) {
                    Text(strings.license)
                }
                if (onImportSettings != null || onExportSettings != null) {
                    SettingsHeading(strings.transferSettings)
                    onImportSettings?.let { import ->
                        TextButton(onClick = { import(stateHolder::importSerialized) }) {
                            Text(strings.importSettings)
                        }
                    }
                    onExportSettings?.let { export ->
                        TextButton(
                            onClick = { stateHolder.exportSerialized()?.let(export) },
                            enabled = !state.saving,
                        ) {
                            Text(strings.exportSettings)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ChoiceRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
