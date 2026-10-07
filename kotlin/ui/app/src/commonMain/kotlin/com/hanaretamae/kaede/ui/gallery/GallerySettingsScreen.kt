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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryTagDisplayPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorRule
import com.hanaretamae.kaede.core.settings.GalleryOtherCategorySettings
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryRule
import com.hanaretamae.kaede.core.settings.GalleryTagCategorySettings
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureCodec
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryFrontmatterSettings
import com.hanaretamae.kaede.core.settings.GalleryLinkResolution
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import com.hanaretamae.kaede.core.settings.ThemePreference
import com.hanaretamae.kaede.core.settings.platformSystemLanguagePreference
import com.hanaretamae.kaede.core.settings.resolve
import kotlinx.coroutines.launch

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
    val galleryEligibility: String,
    val galleryTagPrefixes: String,
    val galleryTagPrefixesHelp: String,
    val invalidGalleryTagPrefixes: String,
    val tagDisplayRules: String,
    val includedTagPrefixes: String,
    val hiddenTagPrefixes: String,
    val tagDisplayRulesHelp: String,
    val invalidTagDisplayPrefixes: String,
    val tagColors: String,
    val tagColorsHelp: String,
    val invalidTagColors: String,
    val tagCategories: String,
    val tagCategoriesHelp: String,
    val invalidTagCategories: String,
    val categoryName: String,
    val categoryPath: String,
    val splitDeep: String,
    val otherCategoryName: String,
    val otherCategoryEnabled: String,
    val addCategory: String,
    val removeCategory: String,
    val noteStructure: String,
    val noteStructureHelp: String,
    val memoHeadings: String,
    val relatedHeadings: String,
    val postTextEndHeadings: String,
    val frontmatterAliases: String,
    val tagsKeys: String,
    val titleKeys: String,
    val urlKeys: String,
    val publishedKeys: String,
    val createdKeys: String,
    val updatedKeys: String,
    val coverKeys: String,
    val linkResolution: String,
    val shortestPath: String,
    val relativePath: String,
    val absolutePath: String,
    val includeQuotedText: String,
    val detailBlockOrder: String,
    val authorBlock: String,
    val mediaBlock: String,
    val postTextBlock: String,
    val postTextEndBlock: String,
    val memoBlock: String,
    val relatedBlock: String,
    val moveUp: String,
    val moveDown: String,
    val showBlock: (String) -> String,
    val invalidNoteStructure: String,
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
    val showMissingMediaIcon: String,
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
    galleryEligibility = "Gallery eligibility",
    galleryTagPrefixes = "Target tag prefixes (one per line)",
    galleryTagPrefixesHelp = "Only notes with a matching tag are shown. Leave empty to show no notes.",
    invalidGalleryTagPrefixes = "Use up to 64 unique non-empty prefixes, each at most 128 UTF-8 bytes.",
    tagDisplayRules = "Tag filter visibility",
    includedTagPrefixes = "Included tag prefixes (one per line)",
    hiddenTagPrefixes = "Hidden tag prefixes (one per line)",
    tagDisplayRulesHelp = "Included prefixes define available filters; hidden prefixes are removed. Use * to include every tag.",
    invalidTagDisplayPrefixes = "Use up to 64 non-empty prefixes, each at most 128 UTF-8 bytes.",
    tagColors = "Tag colors (one prefix=#RRGGBB per line)",
    tagColorsHelp = "The longest matching prefix is used. Colors are included in settings export.",
    invalidTagColors = "Enter up to 64 unique prefixes with six-digit hexadecimal colors.",
    tagCategories = "Tag filter categories",
    tagCategoriesHelp = "Exact paths (source/art) and subtree paths (source/count/*) are supported. More specific rules win.",
    invalidTagCategories = "Check category names and paths; use at most 64 categories.",
    categoryName = "Name",
    categoryPath = "Tag path",
    splitDeep = "Split deep tags",
    otherCategoryName = "Other category name",
    otherCategoryEnabled = "Show unmatched tags in Other",
    addCategory = "Add category",
    removeCategory = "Remove",
    noteStructure = "Note structure",
    noteStructureHelp = "Heading names and frontmatter keys accept one alias per line.",
    memoHeadings = "Memo headings",
    relatedHeadings = "Related headings",
    postTextEndHeadings = "Post-text end headings",
    frontmatterAliases = "Frontmatter aliases",
    tagsKeys = "Tags keys",
    titleKeys = "Title keys",
    urlKeys = "URL keys",
    publishedKeys = "Published keys",
    createdKeys = "Created keys",
    updatedKeys = "Updated keys",
    coverKeys = "Cover keys",
    linkResolution = "Internal link resolution",
    shortestPath = "Shortest path",
    relativePath = "Relative path",
    absolutePath = "Absolute path",
    includeQuotedText = "Include quoted lines in post text",
    detailBlockOrder = "Note detail block order and visibility",
    authorBlock = "Author",
    mediaBlock = "Media",
    postTextBlock = "Post text",
    postTextEndBlock = "Post text end",
    memoBlock = "Memo",
    relatedBlock = "Related links",
    moveUp = "Up",
    moveDown = "Down",
    showBlock = { "Show $it" },
    invalidNoteStructure = "Check heading aliases and frontmatter keys. Tags requires at least one key.",
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
    showMissingMediaIcon = "Show an icon when media is unavailable",
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
    galleryEligibility = "ギャラリー対象",
    galleryTagPrefixes = "対象タグの接頭辞（1行に1つ）",
    galleryTagPrefixesHelp = "一致するタグを持つノートだけを表示します。空欄では対象ノートがありません。",
    invalidGalleryTagPrefixes = "重複しない空でない接頭辞を64個まで、各128 UTF-8バイト以内で入力してください。",
    tagDisplayRules = "タグフィルターの表示",
    includedTagPrefixes = "表示対象タグの接頭辞（1行に1つ）",
    hiddenTagPrefixes = "非表示タグの接頭辞（1行に1つ）",
    tagDisplayRulesHelp = "表示対象でフィルター候補を限定し、非表示接頭辞に一致する候補を除外します。全て表示するには * を指定します。",
    invalidTagDisplayPrefixes = "空でない接頭辞を64個まで、各128 UTF-8バイト以内で入力してください。",
    tagColors = "タグ色（1行に「接頭辞=#RRGGBB」）",
    tagColorsHelp = "最も長く一致する接頭辞の色を使用します。設定のエクスポートにも含まれます。",
    invalidTagColors = "重複しない接頭辞と6桁の16進数カラーを64件まで入力してください。",
    tagCategories = "タグフィルターのカテゴリ",
    tagCategoriesHelp = "完全一致（source/art）とサブツリー（source/count/*）を指定できます。より具体的な規則が優先されます。",
    invalidTagCategories = "カテゴリ名とパスを確認してください。カテゴリは64件までです。",
    categoryName = "名前",
    categoryPath = "タグパス",
    splitDeep = "深い階層を分割",
    otherCategoryName = "その他カテゴリ名",
    otherCategoryEnabled = "未分類タグをその他に表示",
    addCategory = "カテゴリを追加",
    removeCategory = "削除",
    noteStructure = "ノート構造",
    noteStructureHelp = "見出し名とフロントマターキーは1行に1つ入力してください。",
    memoHeadings = "メモ見出し",
    relatedHeadings = "関連見出し",
    postTextEndHeadings = "本文終端見出し",
    frontmatterAliases = "フロントマター別名",
    tagsKeys = "タグキー",
    titleKeys = "タイトルキー",
    urlKeys = "URLキー",
    publishedKeys = "公開日キー",
    createdKeys = "作成日キー",
    updatedKeys = "更新日キー",
    coverKeys = "カバーキー",
    linkResolution = "内部リンクの解決",
    shortestPath = "最短パス",
    relativePath = "相対パス",
    absolutePath = "絶対パス",
    includeQuotedText = "引用行を本文に含める",
    detailBlockOrder = "ノート詳細の表示順と表示設定",
    authorBlock = "作者",
    mediaBlock = "メディア",
    postTextBlock = "本文",
    postTextEndBlock = "本文終端",
    memoBlock = "メモ",
    relatedBlock = "関連リンク",
    moveUp = "上へ",
    moveDown = "下へ",
    showBlock = { "$it を表示" },
    invalidNoteStructure = "見出し名とフロントマターキーを確認してください。タグキーは1つ以上必要です。",
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
    showMissingMediaIcon = "メディアを表示できないときにアイコンを表示",
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
    val scope = rememberCoroutineScope()
    var pageSizeDraft by remember(state.settings?.pageSize) {
        mutableStateOf(state.settings?.pageSize?.toString().orEmpty())
    }
    var galleryTagPrefixesDraft by remember(state.settings?.galleryTagPrefixes) {
        mutableStateOf(state.settings?.galleryTagPrefixes?.joinToString("\n").orEmpty())
    }
    var galleryTagPrefixesError by remember { mutableStateOf(false) }
    var includedTagPrefixesDraft by remember(state.settings?.includedTagPrefixes) {
        mutableStateOf(state.settings?.includedTagPrefixes?.joinToString("\n").orEmpty())
    }
    var hiddenTagPrefixesDraft by remember(state.settings?.hiddenTagPrefixes) {
        mutableStateOf(state.settings?.hiddenTagPrefixes?.joinToString("\n").orEmpty())
    }
    var tagDisplayPrefixesError by remember { mutableStateOf(false) }
    var tagColorRulesError by remember { mutableStateOf(false) }
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

                SettingsHeading(strings.galleryEligibility)
                OutlinedTextField(
                    value = galleryTagPrefixesDraft,
                    onValueChange = {
                        galleryTagPrefixesDraft = it
                        galleryTagPrefixesError = false
                    },
                    label = { Text(strings.galleryTagPrefixes) },
                    supportingText = { Text(strings.galleryTagPrefixesHelp) },
                    isError = galleryTagPrefixesError,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (galleryTagPrefixesError) {
                    Text(
                        strings.invalidGalleryTagPrefixes,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val galleryTagPrefixes = galleryTagPrefixesDraft
                    .lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toList()
                Button(
                    onClick = {
                        if (!GalleryTagPrefixesCodec.isValid(galleryTagPrefixes)) {
                            galleryTagPrefixesError = true
                        } else {
                            scope.launch {
                                stateHolder.saveGalleryTagPrefixes(galleryTagPrefixes)
                            }
                        }
                    },
                    enabled = GalleryTagPrefixesCodec.isValid(galleryTagPrefixes) &&
                        (galleryTagPrefixes != settings.galleryTagPrefixes || state.error != null) &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }
                SettingsHeading(strings.tagDisplayRules)
                Text(
                    strings.tagDisplayRulesHelp,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = includedTagPrefixesDraft,
                    onValueChange = {
                        includedTagPrefixesDraft = it
                        tagDisplayPrefixesError = false
                    },
                    label = { Text(strings.includedTagPrefixes) },
                    isError = tagDisplayPrefixesError,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hiddenTagPrefixesDraft,
                    onValueChange = {
                        hiddenTagPrefixesDraft = it
                        tagDisplayPrefixesError = false
                    },
                    label = { Text(strings.hiddenTagPrefixes) },
                    isError = tagDisplayPrefixesError,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (tagDisplayPrefixesError) {
                    Text(
                        strings.invalidTagDisplayPrefixes,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val includedTagPrefixes = includedTagPrefixesDraft
                    .lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                val hiddenTagPrefixes = hiddenTagPrefixesDraft
                    .lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                val tagDisplayPrefixesValid =
                    GalleryTagDisplayPrefixesCodec.isValid(includedTagPrefixes) &&
                        GalleryTagDisplayPrefixesCodec.isValid(hiddenTagPrefixes)
                Button(
                    onClick = {
                        if (!tagDisplayPrefixesValid) {
                            tagDisplayPrefixesError = true
                        } else {
                            scope.launch {
                                stateHolder.saveTagDisplayPrefixes(
                                    includedTagPrefixes,
                                    hiddenTagPrefixes,
                                )
                            }
                        }
                    },
                    enabled = tagDisplayPrefixesValid &&
                        (
                            includedTagPrefixes != settings.includedTagPrefixes ||
                                hiddenTagPrefixes != settings.hiddenTagPrefixes ||
                                state.error != null
                            ) &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }
                val (currentTagColorRules, storedTagColorRulesValid) =
                    remember(settings.flutterTagSettingsJson) {
                        try {
                            GalleryTagColorCodec.decodeRules(settings.flutterTagSettingsJson)
                                .let { it to true }
                        } catch (_: IllegalArgumentException) {
                            emptyList<GalleryTagColorRule>() to false
                        }
                    }
                var tagColorRulesDraft by remember(settings.flutterTagSettingsJson) {
                    mutableStateOf(currentTagColorRules.toEditorText())
                }
                OutlinedTextField(
                    value = tagColorRulesDraft,
                    onValueChange = {
                        tagColorRulesDraft = it
                        tagColorRulesError = false
                    },
                    label = { Text(strings.tagColors) },
                    supportingText = { Text(strings.tagColorsHelp) },
                    isError = tagColorRulesError,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                val parsedTagColorRules = tagColorRulesDraft.toTagColorRules()
                val tagColorRulesValid = parsedTagColorRules != null &&
                    GalleryTagColorCodec.isValid(parsedTagColorRules)
                if (!storedTagColorRulesValid || tagColorRulesError || !tagColorRulesValid) {
                    Text(
                        strings.invalidTagColors,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        val rules = parsedTagColorRules
                        if (rules == null || !GalleryTagColorCodec.isValid(rules)) {
                            tagColorRulesError = true
                        } else {
                            scope.launch { stateHolder.saveTagColorRules(rules) }
                        }
                    },
                    enabled = storedTagColorRulesValid && tagColorRulesValid &&
                        (parsedTagColorRules != currentTagColorRules || state.error != null) &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }

                val japaneseDefaults =
                    settings.appearance.language.resolve(platformSystemLanguagePreference()) ==
                        LanguagePreference.JAPANESE
                val (storedCategorySettings, storedCategorySettingsValid) =
                    remember(settings.flutterTagSettingsJson, japaneseDefaults) {
                        try {
                            GalleryTagCategoryCodec.decodeSettings(
                                settings.flutterTagSettingsJson,
                                japaneseDefaults,
                            ).let { it to true }
                        } catch (_: IllegalArgumentException) {
                            GalleryTagCategoryCodec.decodeSettings(null, japaneseDefaults) to false
                        }
                    }
                var categoryDraft by remember(
                    settings.flutterTagSettingsJson,
                    japaneseDefaults,
                ) {
                    mutableStateOf(storedCategorySettings)
                }
                var tagCategoriesError by remember { mutableStateOf(false) }
                SettingsHeading(strings.tagCategories)
                Text(strings.tagCategoriesHelp, style = MaterialTheme.typography.bodySmall)
                categoryDraft.categories.forEachIndexed { index, rule ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = rule.name,
                                onValueChange = { value ->
                                    categoryDraft = categoryDraft.copy(
                                        categories = categoryDraft.categories.mapIndexed { i, item ->
                                            if (i == index) item.copy(name = value) else item
                                        },
                                    )
                                    tagCategoriesError = false
                                },
                                label = { Text(strings.categoryName) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    categoryDraft = categoryDraft.copy(
                                        categories = categoryDraft.categories.filterIndexed { i, _ ->
                                            i != index
                                        },
                                    )
                                    tagCategoriesError = false
                                },
                            ) { Text(strings.removeCategory) }
                        }
                        OutlinedTextField(
                            value = rule.path,
                            onValueChange = { value ->
                                categoryDraft = categoryDraft.copy(
                                    categories = categoryDraft.categories.mapIndexed { i, item ->
                                        if (i == index) item.copy(path = value) else item
                                    },
                                )
                                tagCategoriesError = false
                            },
                            label = { Text(strings.categoryPath) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SettingsSwitch(
                            label = strings.splitDeep,
                            checked = rule.splitDeep,
                            onCheckedChange = { enabled ->
                                categoryDraft = categoryDraft.copy(
                                    categories = categoryDraft.categories.mapIndexed { i, item ->
                                        if (i == index) item.copy(splitDeep = enabled) else item
                                    },
                                )
                            },
                        )
                    }
                }
                TextButton(
                    onClick = {
                        categoryDraft = categoryDraft.copy(
                            categories = categoryDraft.categories + GalleryTagCategoryRule(
                                name = "${strings.categoryName} ${categoryDraft.categories.size + 1}",
                                path = "",
                            ),
                        )
                    },
                    enabled = categoryDraft.categories.size < GalleryTagCategoryCodec.MAX_CATEGORIES,
                ) { Text(strings.addCategory) }
                OutlinedTextField(
                    value = categoryDraft.other.name,
                    onValueChange = { value ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(name = value),
                        )
                        tagCategoriesError = false
                    },
                    label = { Text(strings.otherCategoryName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsSwitch(
                    label = strings.otherCategoryEnabled,
                    checked = categoryDraft.other.enabled,
                    onCheckedChange = { enabled ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(enabled = enabled),
                        )
                    },
                )
                SettingsSwitch(
                    label = strings.splitDeep,
                    checked = categoryDraft.other.splitDeep,
                    onCheckedChange = { enabled ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(splitDeep = enabled),
                        )
                    },
                )
                val categorySettingsValid = GalleryTagCategoryCodec.isValid(categoryDraft)
                if (!storedCategorySettingsValid || tagCategoriesError || !categorySettingsValid) {
                    Text(
                        strings.invalidTagCategories,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        if (!categorySettingsValid) {
                            tagCategoriesError = true
                        } else {
                            scope.launch { stateHolder.saveTagCategorySettings(categoryDraft) }
                        }
                    },
                    enabled = categorySettingsValid &&
                        categoryDraft != storedCategorySettings &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }

                val (storedNoteStructure, storedNoteStructureValid) =
                    remember(settings.flutterTagSettingsJson, settings.galleryTagPrefixes) {
                        try {
                            GalleryNoteStructureCodec.decodeSettings(
                                settings.flutterTagSettingsJson,
                                settings.galleryTagPrefixes,
                            ).let { it to true }
                        } catch (_: IllegalArgumentException) {
                            GalleryNoteStructureCodec.decodeSettings(
                                null,
                                settings.galleryTagPrefixes,
                            ) to false
                        }
                    }
                var noteStructureDraft by remember(
                    settings.flutterTagSettingsJson,
                    settings.galleryTagPrefixes,
                ) {
                    mutableStateOf(storedNoteStructure)
                }
                var noteStructureError by remember { mutableStateOf(false) }
                SettingsHeading(strings.noteStructure)
                Text(strings.noteStructureHelp, style = MaterialTheme.typography.bodySmall)
                listOf(
                    strings.memoHeadings to noteStructureDraft.memoHeadings,
                    strings.relatedHeadings to noteStructureDraft.relatedHeadings,
                    strings.postTextEndHeadings to noteStructureDraft.postTextEndHeadings,
                ).forEachIndexed { index, (label, values) ->
                    OutlinedTextField(
                        value = values.joinToString("\n"),
                        onValueChange = { text ->
                            val headings = text.toSettingsLines()
                            noteStructureDraft = when (index) {
                                0 -> noteStructureDraft.copy(memoHeadings = headings)
                                1 -> noteStructureDraft.copy(relatedHeadings = headings)
                                else -> noteStructureDraft.copy(postTextEndHeadings = headings)
                            }
                            noteStructureError = false
                        },
                        label = { Text(label) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SettingsHeading(strings.frontmatterAliases)
                listOf(
                    strings.tagsKeys to noteStructureDraft.frontmatter.tagsKeys,
                    strings.titleKeys to noteStructureDraft.frontmatter.titleKeys,
                    strings.urlKeys to noteStructureDraft.frontmatter.urlKeys,
                    strings.publishedKeys to noteStructureDraft.frontmatter.publishedKeys,
                    strings.createdKeys to noteStructureDraft.frontmatter.createdKeys,
                    strings.updatedKeys to noteStructureDraft.frontmatter.updatedKeys,
                    strings.coverKeys to noteStructureDraft.frontmatter.coverKeys,
                ).forEachIndexed { index, (label, values) ->
                    OutlinedTextField(
                        value = values.joinToString("\n"),
                        onValueChange = { text ->
                            val keys = text.toSettingsLines()
                            val current = noteStructureDraft.frontmatter
                            val updated = when (index) {
                                0 -> current.copy(tagsKeys = keys)
                                1 -> current.copy(titleKeys = keys)
                                2 -> current.copy(urlKeys = keys)
                                3 -> current.copy(publishedKeys = keys)
                                4 -> current.copy(createdKeys = keys)
                                5 -> current.copy(updatedKeys = keys)
                                else -> current.copy(coverKeys = keys)
                            }
                            noteStructureDraft = noteStructureDraft.copy(frontmatter = updated)
                            noteStructureError = false
                        },
                        label = { Text(label) },
                        minLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(strings.linkResolution, style = MaterialTheme.typography.titleMedium)
                ChoiceRow {
                    GalleryLinkResolution.entries.forEach { resolution ->
                        FilterChip(
                            selected = noteStructureDraft.linkResolution == resolution,
                            onClick = {
                                noteStructureDraft =
                                    noteStructureDraft.copy(linkResolution = resolution)
                            },
                            label = {
                                Text(
                                    when (resolution) {
                                        GalleryLinkResolution.SHORTEST_PATH -> strings.shortestPath
                                        GalleryLinkResolution.RELATIVE_PATH -> strings.relativePath
                                        GalleryLinkResolution.ABSOLUTE_PATH -> strings.absolutePath
                                    },
                                )
                            },
                        )
                    }
                }
                SettingsSwitch(
                    label = strings.includeQuotedText,
                    checked = noteStructureDraft.postTextIncludeQuote,
                    onCheckedChange = { enabled ->
                        noteStructureDraft =
                            noteStructureDraft.copy(postTextIncludeQuote = enabled)
                    },
                )
                SettingsHeading(strings.detailBlockOrder)
                noteStructureDraft.blockOrder.forEachIndexed { index, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            block.displayName(strings),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                noteStructureDraft = noteStructureDraft.copy(
                                    blockOrder = noteStructureDraft.blockOrder
                                        .moveNoteBlock(index, index - 1),
                                )
                            },
                            enabled = index > 0,
                        ) { Text(strings.moveUp) }
                        TextButton(
                            onClick = {
                                noteStructureDraft = noteStructureDraft.copy(
                                    blockOrder = noteStructureDraft.blockOrder
                                        .moveNoteBlock(index, index + 1),
                                )
                            },
                            enabled = index < noteStructureDraft.blockOrder.lastIndex,
                        ) { Text(strings.moveDown) }
                    }
                    if (block in GalleryNoteBlock.entries.filter {
                            it != GalleryNoteBlock.MEDIA &&
                                it != GalleryNoteBlock.POST_TEXT_END
                        }
                    ) {
                        SettingsSwitch(
                            label = strings.showBlock(block.displayName(strings)),
                            checked = block !in noteStructureDraft.hiddenBlocks,
                            onCheckedChange = { visible ->
                                noteStructureDraft = noteStructureDraft.copy(
                                    hiddenBlocks = if (visible) {
                                        noteStructureDraft.hiddenBlocks - block
                                    } else {
                                        noteStructureDraft.hiddenBlocks + block
                                    },
                                )
                            },
                        )
                    }
                }
                val noteStructureValid = GalleryNoteStructureCodec.isValid(noteStructureDraft)
                if (!storedNoteStructureValid || noteStructureError || !noteStructureValid) {
                    Text(
                        strings.invalidNoteStructure,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        if (!noteStructureValid) {
                            noteStructureError = true
                        } else {
                            scope.launch {
                                stateHolder.saveNoteStructureSettings(noteStructureDraft)
                            }
                        }
                    },
                    enabled = noteStructureValid &&
                        noteStructureDraft != storedNoteStructure &&
                        !state.saving,
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(strings.save) }

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
                SettingsSwitch(
                    label = strings.showMissingMediaIcon,
                    checked = settings.showMissingMediaIcon,
                    onCheckedChange = stateHolder::setShowMissingMediaIcon,
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

private fun List<GalleryTagColorRule>.toEditorText(): String = joinToString("\n") { rule ->
    "${rule.prefix}=#${rule.color.toUInt().toString(16).takeLast(6)}"
}

private fun String.toSettingsLines(): List<String> =
    lineSequence().filter(String::isNotEmpty).toList()

private fun List<GalleryNoteBlock>.moveNoteBlock(from: Int, to: Int): List<GalleryNoteBlock> {
    if (from !in indices || to !in indices) return this
    return toMutableList().also { blocks ->
        val block = blocks.removeAt(from)
        blocks.add(to, block)
    }
}

private fun GalleryNoteBlock.displayName(strings: GallerySettingsStrings): String =
    when (this) {
        GalleryNoteBlock.AUTHOR -> strings.authorBlock
        GalleryNoteBlock.MEDIA -> strings.mediaBlock
        GalleryNoteBlock.POST_TEXT -> strings.postTextBlock
        GalleryNoteBlock.POST_TEXT_END -> strings.postTextEndBlock
        GalleryNoteBlock.RELATED -> strings.relatedBlock
        GalleryNoteBlock.MEMO -> strings.memoBlock
    }

private fun String.toTagColorRules(): List<GalleryTagColorRule>? {
    val lines = lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.size > GalleryTagColorCodec.MAX_RULES) return null
    return lines.map { line ->
        val separator = line.lastIndexOf("=#")
        if (separator <= 0 || line.length - separator != 8) return null
        val color = line.substring(separator + 2).toIntOrNull(16) ?: return null
        GalleryTagColorRule(line.substring(0, separator), color or 0xff000000.toInt())
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
