@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.FontPreference
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import com.hanaretamae.kaede.ui.gallery.resources.Res
import com.hanaretamae.kaede.ui.gallery.resources.kaede_gallery_icon

data class GallerySettingsStrings(
    val title: String,
    val close: String,
    val listDisplay: String,
    val vault: String,
    val notes: String,
    val selectedVault: String,
    val changeVault: String,
    val rescan: String,
    val scanWarnings: (Int) -> String,
    val scanWarningDetails: String,
    val scanWarningNone: String,
    val scanWarningExplanation: (Int) -> String,
    val scanWarningCauses: String,
    val scanWarningEligibility: String,
    val scanWarningPrivacy: String,
    val forgetVault: String,
    val forgetVaultTitle: String,
    val forgetVaultMessage: String,
    val forgetVaultConfirm: String,
    val importExportReset: String,
    val tagSettings: String,
    val scanWarningsTitle: String,
    val scanWarningsSummary: (Int) -> String,
    val openSourceLicenses: String,
    val licenseOfflineDescription: String,
    val licenseAppEntry: String,
    val licenseNoticesEntry: String,
    val licenseRuntimeEntry: String,
    val licenseApacheEntry: String,
    val licenseMaterialKolorEntry: String,
    val licenseGoogleSansFlexEntry: String,
    val licenseJnaEntry: String,
    val licenseRustEntry: String,
    val licenseTextUnavailable: String,
    val helpAndUse: String,
    val exampleNote: String,
    val exampleNoteHelp: String,
    val exampleNoteExplanations: List<String>,
    val copyExample: String,
    val addItem: String,
    val editItem: String,
    val removeItem: String,
    val tagName: String,
    val tagColor: String,
    val chooseTagColor: String,
    val invalidItem: String,
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
    val font: String,
    val systemFont: String,
    val expressiveFont: String,
    val pagination: String,
    val fixedColumns: String,
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
    val noteBlockHelp: (GalleryNoteBlock) -> String,
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
    val markdownLink: String,
    val wikiLink: String,
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
    val help: String,
    val relatedNotes: String,
    val searchAndFiltering: String,
    val relatedNotesHelp: String,
    val noteStructureUsageHelp: String,
    val searchAndFilteringHelp: String,
    val pageSize: String,
    val invalidPageSize: String,
    val showLoadedRange: String,
    val showTilePosition: String,
    val showCounts: String,
    val showMissingMediaIcon: String,
    val reset: String,
    val resetTitle: String,
    val resetMessage: String,
    val restoreDefaults: String,
    val cancel: String,
    val save: String,
    val loading: String,
    val retry: String,
    val error: (RepositoryError) -> String,
)

val EnglishGallerySettingsStrings = GallerySettingsStrings(
    title = "Settings",
    close = "Back",
    listDisplay = "Paging and list",
    vault = "Vault",
    notes = "Notes",
    selectedVault = "Selected Vault",
    changeVault = "Change Vault",
    rescan = "Rescan",
    scanWarnings = { "$it items could not be checked" },
    scanWarningDetails = "Details",
    scanWarningNone = "This scan found no unchecked items.",
    scanWarningExplanation = { "This scan could not check $it items." },
    scanWarningCauses = "Files or folders may be unreadable, or a note may use unsupported encoding.",
    scanWarningEligibility = "Unreadable items may still match the configured gallery tags; their names and contents are not used here.",
    scanWarningPrivacy = "Specific item names and note contents are not shown.",
    forgetVault = "Forget Vault",
    forgetVaultTitle = "Forget selected Vault?",
    forgetVaultMessage = "This removes its local index and releases its read grant. Vault files are not changed.",
    forgetVaultConfirm = "Forget",
    importExportReset = "Data",
    tagSettings = "Tag settings",
    scanWarningsTitle = "Items that could not be checked",
    scanWarningsSummary = { if (it == 0) "None" else "$it items" },
    openSourceLicenses = "Open-source licenses",
    licenseOfflineDescription = "License texts are bundled with the app and available offline.",
    licenseAppEntry = "Kaede Gallery — MIT License",
    licenseNoticesEntry = "Third-party notices — platform-specific and optional components",
    licenseRuntimeEntry = "Bundled platform and runtime license texts",
    licenseApacheEntry = "Apache License 2.0 — Kotlin, Compose, AndroidX, Material Color Utilities, Media3, Skydoves ColorPicker, and JNA option",
    licenseMaterialKolorEntry = "MaterialKolor — MIT License",
    licenseGoogleSansFlexEntry = "Google Sans Flex — SIL Open Font License 1.1",
    licenseJnaEntry = "JNA — GNU LGPL 2.1-or-later alternative",
    licenseRustEntry = "Rust and UniFFI dependency notices",
    licenseTextUnavailable = "This license text is unavailable in this installation.",
    helpAndUse = "Help and how to use",
    exampleNote = "Example note",
    exampleNoteHelp = "Fictional Markdown using the current note structure.\nCopying it does not write to your Vault.",
    exampleNoteExplanations = listOf(
        "Frontmatter contains fictional metadata and tags.",
        "The author is inferred from the note's tags.",
        "The embedded image demonstrates media handling.",
        "The post text and optional quote demonstrate body parsing.",
        "Related links and memo headings use the configured aliases.",
    ),
    copyExample = "Copy Markdown",
    addItem = "Add",
    editItem = "Edit",
    removeItem = "Remove",
    tagName = "Tag",
    tagColor = "Color",
    chooseTagColor = "Choose a color",
    invalidItem = "Check the value, duplicates, and the number of items.",
    appearance = "Display",
    theme = "Theme",
    system = "System",
    light = "Light",
    dark = "Dark",
    language = "Language",
    japanese = "Japanese",
    english = "English",
    systemColor = "Use system accent color",
    pureBlack = "Pure black in dark mode",
    font = "Font",
    systemFont = "System",
    expressiveFont = "Google Sans Flex",
    pagination = "Gallery list",
    fixedColumns = "Fixed gallery columns (0 = automatic, or 2–12)",
    galleryEligibility = "Gallery eligibility",
    galleryTagPrefixes = "Target tag prefixes",
    galleryTagPrefixesHelp = "Only notes with a matching tag are shown.\nLeave empty to show no notes.",
    invalidGalleryTagPrefixes = "Use up to 64 unique non-empty prefixes, each at most 128 UTF-8 bytes.",
    tagDisplayRules = "Tag filter visibility",
    includedTagPrefixes = "Included tag prefixes",
    hiddenTagPrefixes = "Hidden tag prefixes",
    tagDisplayRulesHelp = "Included prefixes define available filters; hidden prefixes are removed.\nUse * to include every tag.",
    invalidTagDisplayPrefixes = "Use up to 64 non-empty prefixes, each at most 128 UTF-8 bytes.",
    tagColors = "Tag colors",
    tagColorsHelp = "The longest matching prefix is used.\nColors are included in settings export.",
    invalidTagColors = "Enter up to 64 unique prefixes with six-digit hexadecimal colors.",
    tagCategories = "Tag filter categories",
    tagCategoriesHelp = "Exact paths (source/art) and subtree paths (source/count/*) are supported.\nMore specific rules win.",
    invalidTagCategories = "Check category names and paths; use at most 64 categories.",
    categoryName = "Name",
    categoryPath = "Tag path",
    splitDeep = "Split deep tags",
    otherCategoryName = "Other category name",
    otherCategoryEnabled = "Show unmatched tags in Other",
    addCategory = "Add category",
    removeCategory = "Remove",
    noteStructure = "Note structure",
    noteBlockHelp = { block ->
        when (block) {
            GalleryNoteBlock.AUTHOR -> "Shows the author resolved from the note's tags."
            GalleryNoteBlock.MEDIA -> "Shows images and videos in the main viewer.\nMedia is not repeated in the details panel."
            GalleryNoteBlock.POST_TEXT -> "Shows the post text before its end marker.\nQuoted lines can be included or excluded."
            GalleryNoteBlock.POST_TEXT_END -> "Marks where the post text ends.\nConfigure the heading names used to recognize this boundary."
            GalleryNoteBlock.RELATED -> "Shows related links under the configured headings.\nLinks resolve only within the selected Vault."
            GalleryNoteBlock.MEMO -> "Shows memo text under the configured headings.\nAdd alternative heading names on separate lines."
        }

    },
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
    markdownLink = "Markdown Link",
    wikiLink = "WikiLink",
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
    relatedBlock = "Related",
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
    help = "Help and how to use",
    relatedNotes = "Related notes",
    searchAndFiltering = "Search and filtering",
    relatedNotesHelp = "Markdown links, embedded note links, and Obsidian wikilinks\ncan open related notes when their targets resolve inside the selected Vault.",
    noteStructureUsageHelp = "Heading names and frontmatter keys accept one alias per entry; add alternate names as separate entries.\nChange the order and visibility of viewer sections.",
    searchAndFilteringHelp = "Words fuzzy-match note titles, paths, and tags when Fuzzy search is on; separate words with spaces to require each one. Prefix a word with ~ to fuzzy-match it regardless of the setting, or = to match it as a whole word only.\nUse #tag for a matching tag (OR within its category), +#tag to require that tag (AND), and -#tag to exclude it.\nFor example: ~Mny =portrait #cat +#blue -#draft. The filter sheet also combines tag, category, content, and date filters.",
    pageSize = "Items per page (1–500)",
    invalidPageSize = "Enter a whole number from 1 to 500",
    showLoadedRange = "Show loaded range and total",
    showTilePosition = "Show tile position number",
    showCounts = "Show media, memo, and related counts",
    showMissingMediaIcon = "Show an icon when media is unavailable",
    reset = "Reset to defaults",
    resetTitle = "Reset settings?",
    resetMessage = "Restore display, tag, note-structure, and list settings to their defaults?",
    restoreDefaults = "Restore defaults",
    cancel = "Cancel",
    save = "Save",
    loading = "Loading settings…",
    retry = "Retry",
    error = ::englishError,
)

val JapaneseGallerySettingsStrings = GallerySettingsStrings(
    title = "設定",
    close = "戻る",
    listDisplay = "ページングと一覧",
    vault = "保管庫",
    notes = "ノート",
    selectedVault = "選択中のVault",
    changeVault = "Vaultを変更",
    rescan = "再スキャン",
    scanWarnings = { "確認できなかった項目: $it 件" },
    scanWarningDetails = "詳細",
    scanWarningNone = "今回のスキャンで確認できなかった項目はありません。",
    scanWarningExplanation = { "今回のスキャンでは $it 件を確認できませんでした。" },
    scanWarningCauses = "ファイルやフォルダーを読み取れない、またはノートの文字コードに対応していない場合があります。",
    scanWarningEligibility = "読み取れなかった項目も設定したギャラリー対象タグに一致する可能性があります。名前や内容は表示しません。",
    scanWarningPrivacy = "具体的な項目名やノートの内容は表示しません。",
    forgetVault = "Vaultの登録を解除",
    forgetVaultTitle = "選択中のVaultを登録解除しますか？",
    forgetVaultMessage = "ローカル索引を削除し、読み取り権限を解放します。Vault内のファイルは変更しません。",
    forgetVaultConfirm = "登録解除",
    importExportReset = "データ",
    tagSettings = "タグ設定",
    scanWarningsTitle = "確認できなかった項目",
    scanWarningsSummary = { if (it == 0) "ありません" else "$it 件" },
    openSourceLicenses = "オープンソースライセンス",
    licenseOfflineDescription = "ライセンス本文はアプリに同梱され、オフラインで確認できます。",
    licenseAppEntry = "Kaede Gallery — MIT License",
    licenseNoticesEntry = "サードパーティ通知 — プラットフォーム別・任意コンポーネント",
    licenseRuntimeEntry = "同梱プラットフォーム・実行環境ライセンス本文",
    licenseApacheEntry = "Apache License 2.0 — Kotlin、Compose、AndroidX、Material Color Utilities、Media3、Skydoves ColorPicker、JNA の選択肢",
    licenseMaterialKolorEntry = "MaterialKolor — MIT License",
    licenseGoogleSansFlexEntry = "Google Sans Flex — SIL Open Font License 1.1",
    licenseJnaEntry = "JNA — GNU LGPL 2.1以降の選択肢",
    licenseRustEntry = "Rust・UniFFI依存関係の通知",
    licenseTextUnavailable = "このライセンス本文はインストール内で利用できません。",
    helpAndUse = "ヘルプ・使い方",
    exampleNote = "ノートの例",
    exampleNoteHelp = "現在のノート構造を使った架空のMarkdownです。\nコピーしてもVaultに書き込まれません。",
    exampleNoteExplanations = listOf(
        "フロントマターに架空のメタデータとタグを記載します。",
        "作者はノートのタグから判定されます。",
        "埋め込み画像でメディアの指定方法を示します。",
        "本文と任意の引用行で本文の解析例を示します。",
        "関連リンクとメモ見出しには設定した別名を使います。",
    ),
    copyExample = "Markdownをコピー",
    addItem = "追加",
    editItem = "編集",
    removeItem = "削除",
    tagName = "タグ",
    tagColor = "色",
    chooseTagColor = "色を選択",
    invalidItem = "値・重複・項目数を確認してください。",
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
    font = "フォント",
    systemFont = "システム",
    expressiveFont = "Google Sans Flex",
    pagination = "ギャラリー一覧",
    fixedColumns = "ギャラリー列数（0で自動、または2〜12）",
    galleryEligibility = "ギャラリー対象",
    galleryTagPrefixes = "対象タグの接頭辞",
    galleryTagPrefixesHelp = "一致するタグを持つノートだけを表示します。\n空欄では対象ノートがありません。",
    invalidGalleryTagPrefixes = "重複しない空でない接頭辞を64個まで、各128 UTF-8バイト以内で入力してください。",
    tagDisplayRules = "タグフィルターの表示",
    includedTagPrefixes = "表示対象タグの接頭辞",
    hiddenTagPrefixes = "非表示タグの接頭辞",
    tagDisplayRulesHelp = "表示対象でフィルター候補を限定し、非表示接頭辞に一致する候補を除外します。\n全て表示するには * を指定します。",
    invalidTagDisplayPrefixes = "空でない接頭辞を64個まで、各128 UTF-8バイト以内で入力してください。",
    tagColors = "タグ色",
    tagColorsHelp = "最も長く一致する接頭辞の色を使用します。\n設定のエクスポートにも含まれます。",
    invalidTagColors = "重複しない接頭辞と6桁の16進数カラーを64件まで入力してください。",
    tagCategories = "タグフィルターのカテゴリ",
    tagCategoriesHelp = "完全一致（source/art）とサブツリー（source/count/*）を指定できます。\nより具体的な規則が優先されます。",
    invalidTagCategories = "カテゴリ名とパスを確認してください。カテゴリは64件までです。",
    categoryName = "名前",
    categoryPath = "タグパス",
    splitDeep = "深い階層を分割",
    otherCategoryName = "その他カテゴリ名",
    otherCategoryEnabled = "未分類タグをその他に表示",
    addCategory = "カテゴリを追加",
    removeCategory = "削除",
    noteStructure = "ノート構造",
    noteBlockHelp = { block ->
        when (block) {
            GalleryNoteBlock.AUTHOR -> "ノートのタグから判定した作者を表示します。"
            GalleryNoteBlock.MEDIA -> "画像や動画をメインビュワーに表示します。\n詳細欄にはメディアを重複して表示しません。"
            GalleryNoteBlock.POST_TEXT -> "本文終了の見出しまでの文章を表示します。\n引用行を含めるかどうかを設定できます。"
            GalleryNoteBlock.POST_TEXT_END -> "本文が終了する位置を示します。\n区切りの判定に使う見出し名を設定できます。"
            GalleryNoteBlock.RELATED -> "設定した見出し内の関連リンクを表示します。\nリンクは選択中のVault内でのみ解決します。"
            GalleryNoteBlock.MEMO -> "設定した見出し内のメモを表示します。\n別名は改行して追加できます。"
        }
    },
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
    markdownLink = "Markdown Link",
    wikiLink = "WikiLink",
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
    relatedBlock = "関連",
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
    help = "ヘルプ・使い方",
    relatedNotes = "関連ノート",
    searchAndFiltering = "検索とフィルター",
    relatedNotesHelp = "Markdownリンク、埋め込みノートリンク、Obsidian wikilinkから、\n選択中のVault内にある関連ノートを開けます。",
    noteStructureUsageHelp = "見出し名とフロントマターキーは1項目ずつ追加でき、別名も複数登録できます。\nビューア各項目の順序と表示も変更できます。",
    searchAndFilteringHelp = "あいまい検索がオンの場合、単語でノート名・パス・タグを検索します。空白で区切った単語はすべて一致が必要です。~単語 は設定に関係なくあいまい検索し、=単語 はその単語全体だけに一致します。\n#tag は同じカテゴリ内のタグのいずれか（OR）、+#tag はそのタグを必須（AND）、-#tag は除外です。例: ~ねこ =portrait #cat +#blue -#draft。フィルターシートではタグ・カテゴリ・内容・日付も組み合わせられます。",
    pageSize = "1ページの項目数（1〜500）",
    invalidPageSize = "1〜500の整数を入力してください",
    showLoadedRange = "読み込み済み件数と総数を表示",
    showTilePosition = "タイルの位置番号を表示",
    showCounts = "メディア・メモ・関連リンク数を表示",
    showMissingMediaIcon = "メディアを表示できないときにアイコンを表示",
    reset = "既定値に戻す",
    resetTitle = "設定をリセットしますか？",
    resetMessage = "表示、タグ、ノート構造、一覧の設定を既定値に戻しますか？",
    restoreDefaults = "既定値に戻す",
    cancel = "キャンセル",
    save = "保存",
    loading = "設定を読み込み中…",
    retry = "再試行",
    error = ::japaneseError,
)

internal data class GalleryLicenseDocument(
    val title: String,
    val assetPath: String,
)

internal fun galleryLicenseDocuments(strings: GallerySettingsStrings): List<GalleryLicenseDocument> =
    listOf(
        GalleryLicenseDocument(
            title = strings.licenseAppEntry,
            assetPath = "licenses/Kaede-Gallery-MIT.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseNoticesEntry,
            assetPath = "licenses/THIRD_PARTY_NOTICES.md",
        ),
        GalleryLicenseDocument(
            title = strings.licenseRuntimeEntry,
            assetPath = "licenses/Runtime-LICENSES.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseApacheEntry,
            assetPath = "licenses/Apache-2.0.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseMaterialKolorEntry,
            assetPath = "licenses/MaterialKolor-MIT.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseGoogleSansFlexEntry,
            assetPath = "licenses/GoogleSansFlex-OFL.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseJnaEntry,
            assetPath = "licenses/LGPL-2.1.txt",
        ),
        GalleryLicenseDocument(
            title = strings.licenseRustEntry,
            assetPath = "licenses/RUST-DEPENDENCY-LICENSES.txt",
        ),
    )

private enum class GallerySettingsPage {
    HOME,
    LIST_DISPLAY,
    LICENSES,
    NOTE_STRUCTURE,
    TAG_SETTINGS,
    ABOUT,
    DATA,
    HELP,
    NOTE_EXAMPLE,
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GallerySettingsScreen(
    stateHolder: SettingsStateHolder,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strings: GallerySettingsStrings = EnglishGallerySettingsStrings,
    loadOnEnter: Boolean = true,
    disposeStateHolderOnExit: Boolean = true,
    onExternalLink: (String) -> Unit = {},
    loadLicenseText: (String) -> String? = { null },
    onImportSettings: (((String) -> Unit) -> Unit)? = null,
    onExportSettings: ((String) -> Unit)? = null,
    vaultName: String? = null,
    scanWarningCount: Int? = null,
    onRescan: (suspend () -> RepositoryResult<*>)? = null,
    onChangeVault: (() -> Unit)? = null,
    onForgetVault: (suspend () -> RepositoryResult<*>)? = null,
    openExampleOnStart: Boolean = false,
    exampleOnly: Boolean = false,
    backGestureProgress: Float = 0f,
    onBackHandlerChanged: (((() -> Unit)?) -> Unit)? = null,
) {
    val state by stateHolder.state.collectAsState()
    val scope = rememberCoroutineScope()
    var activePage by remember(stateHolder, openExampleOnStart) {
        mutableStateOf(
            if (openExampleOnStart) GallerySettingsPage.NOTE_EXAMPLE else GallerySettingsPage.HOME,
        )
    }
    var selectedLicense by remember(stateHolder) { mutableStateOf<GalleryLicenseDocument?>(null) }
    val latestActivePage = rememberUpdatedState(activePage)
    val latestSelectedLicense = rememberUpdatedState(selectedLicense)
    val settingsBackHandler = remember {
        {
            if (latestSelectedLicense.value != null) {
                selectedLicense = null
            } else if (latestActivePage.value != GallerySettingsPage.HOME) {
                activePage = GallerySettingsPage.HOME
            }
        }
    }
    val canHandleSettingsBack = selectedLicense != null ||
        (!exampleOnly && activePage != GallerySettingsPage.HOME)
    SideEffect {
        onBackHandlerChanged?.invoke(settingsBackHandler.takeIf { canHandleSettingsBack })
    }
    var confirmForgetVault by remember { mutableStateOf(false) }
    var showScanWarningDetails by remember { mutableStateOf(false) }
    var rescanInProgress by remember(stateHolder) { mutableStateOf(false) }
    var galleryTagPrefixesSaveJob by remember(stateHolder) { mutableStateOf<Job?>(null) }
    var tagDisplayPrefixesSaveJob by remember(stateHolder) { mutableStateOf<Job?>(null) }
    var tagColorRulesSaveJob by remember(stateHolder) { mutableStateOf<Job?>(null) }
    var tagCategorySettingsSaveJob by remember(stateHolder) { mutableStateOf<Job?>(null) }
    var noteStructureSaveJob by remember(stateHolder) { mutableStateOf<Job?>(null) }
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
    var confirmReset by remember { mutableStateOf(false) }
    if (loadOnEnter) {
        LaunchedEffect(stateHolder) { stateHolder.load() }
    }
    if (disposeStateHolderOnExit) {
        DisposableEffect(stateHolder) {
            onDispose(stateHolder::dispose)
        }
    }

    var pageCanScrollBackward by remember { mutableStateOf(false) }
    val settingsTopBarColor = if (pageCanScrollBackward) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }
    val settingsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize()
            .nestedScroll(settingsScrollBehavior.nestedScrollConnection)
            .graphicsLayer {
                val progress = backGestureProgress.coerceIn(-1f, 1f)
                translationX = size.width * progress
                alpha = 1f - kotlin.math.abs(progress) * 0.08f
            },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                scrollBehavior = settingsScrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = settingsTopBarColor,
                    scrolledContainerColor = settingsTopBarColor,
                ),
                title = {
                    Text(
                        selectedLicense?.title ?: activePage.displayName(strings),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    KaedeIconButton(
                        label = strings.close,
                        onClick = {
                            when {
                                selectedLicense != null -> selectedLicense = null
                                exampleOnly -> onClose()
                                activePage == GallerySettingsPage.HOME -> onClose()
                                else -> settingsBackHandler()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.close)
                    }
                },
            )
        },
    ) { insets ->
        AnimatedContent(
            targetState = activePage,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 12 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { -it / 12 })
            },
            label = "settings-page",
        ) { destination ->
        val pageScrollState = rememberScrollState()
        LaunchedEffect(destination, pageScrollState) {
            snapshotFlow { destination == activePage && pageScrollState.canScrollBackward }
                .collect { pageCanScrollBackward = it }
        }
        val page = destination
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(pageScrollState).padding(16.dp),
        ) {
            if (state.loading && state.settings == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(strings.loading, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error?.let { error ->
                Text(strings.error(error), color = MaterialTheme.colorScheme.error)
                if (state.settings == null) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = stateHolder::load) { Text(strings.retry) }
                } else if (state.hasUnsavedChanges) {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = stateHolder::retrySave,
                        enabled = !state.saving,
                    ) { Text(strings.retry) }
                }
            }
            state.settings?.let { settings ->
                if (page == GallerySettingsPage.HOME) {
                SettingsHeading(strings.appearance)
                AppearanceSettingsList(settings, stateHolder, strings)
                SettingsNavigationGroup(
                    entries = listOf(
                        SettingsNavigationEntry(
                            strings.listDisplay,
                            Icons.AutoMirrored.Filled.ViewList,
                        ) { activePage = GallerySettingsPage.LIST_DISPLAY },
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                val hasVaultSettings = vaultName != null || scanWarningCount != null ||
                    onChangeVault != null || onRescan != null || onForgetVault != null
                if (hasVaultSettings) {
                    SettingsHeading(strings.vault)
                    val vaultRows = listOfNotNull(
                        (vaultName != null || onChangeVault != null).takeIf { it },
                        (scanWarningCount != null || onRescan != null).takeIf { it },
                        (onForgetVault != null).takeIf { it },
                    ).size
                    var vaultIndex = 0
                    SettingsGroup {
                        if (vaultName != null || onChangeVault != null) {
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(vaultIndex++, vaultRows),
                                colors = settingsItemColors(),
                                leadingContent = { Icon(Icons.Filled.Folder, null) },
                                supportingContent = vaultName?.let { name -> { Text(name) } },
                                trailingContent = onChangeVault?.let { change ->
                                    {
                                        KaedeIconButton(
                                            label = strings.changeVault,
                                            onClick = change,
                                            enabled = !state.saving && !rescanInProgress,
                                        ) { Icon(Icons.Filled.FolderOpen, strings.changeVault) }
                                    }
                                },
                            ) { Text(strings.selectedVault) }
                        }
                        if (scanWarningCount != null || onRescan != null) {
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(vaultIndex++, vaultRows),
                                colors = settingsItemColors(),
                                leadingContent = {
                                    Icon(Icons.Filled.Info, null)
                                },
                                supportingContent = scanWarningCount?.let { count ->
                                    { Text(strings.scanWarningsSummary(count)) }
                                },
                                trailingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (scanWarningCount != null) {
                                            KaedeIconButton(
                                                label = strings.scanWarningDetails,
                                                onClick = { showScanWarningDetails = true },
                                            ) { Icon(Icons.Filled.Info, strings.scanWarningDetails) }
                                        }
                                        onRescan?.let { rescan ->
                                            if (rescanInProgress) {
                                                LoadingIndicator(Modifier.size(40.dp))
                                            } else {
                                                KaedeIconButton(
                                                    label = strings.rescan,
                                                    onClick = {
                                                        rescanInProgress = true
                                                        scope.launch {
                                                            try {
                                                                when (val result = rescan()) {
                                                                    is RepositoryResult.Failure ->
                                                                        stateHolder.reportError(result.error)
                                                                    is RepositoryResult.Success<*> -> Unit
                                                                }
                                                            } finally {
                                                                rescanInProgress = false
                                                            }
                                                        }
                                                    },
                                                    enabled = !state.saving,
                                                ) { Icon(Icons.Filled.Refresh, strings.rescan) }
                                            }
                                        }
                                    }
                                },
                            ) { Text(strings.scanWarningsTitle) }
                        }
                        if (onForgetVault != null) {
                            SegmentedListItem(
                                onClick = { confirmForgetVault = true },
                                enabled = !state.saving && !rescanInProgress,
                                shapes = ListItemDefaults.segmentedShapes(vaultIndex++, vaultRows),
                                colors = settingsItemColors(),
                                leadingContent = {
                                    Icon(Icons.Filled.LinkOff, null, tint = MaterialTheme.colorScheme.error)
                                },
                            ) { Text(strings.forgetVault, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }

                SettingsHeading(strings.notes)
                SettingsNavigationGroup(
                    entries = listOf(
                        SettingsNavigationEntry(
                            strings.noteStructure,
                            Icons.Filled.AccountTree,
                        ) { activePage = GallerySettingsPage.NOTE_STRUCTURE },
                        SettingsNavigationEntry(
                            strings.tagSettings,
                            Icons.AutoMirrored.Filled.Label,
                        ) { activePage = GallerySettingsPage.TAG_SETTINGS },
                        SettingsNavigationEntry(
                            strings.exampleNote,
                            Icons.Filled.Description,
                        ) { activePage = GallerySettingsPage.NOTE_EXAMPLE },
                    ),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                SettingsHeading(strings.about)
                SettingsNavigationGroup(
                    entries = listOf(
                        SettingsNavigationEntry(strings.about, Icons.Filled.Info) {
                            activePage = GallerySettingsPage.ABOUT
                        },
                        SettingsNavigationEntry(
                            strings.importExportReset,
                            Icons.Filled.SettingsBackupRestore,
                        ) { activePage = GallerySettingsPage.DATA },
                        SettingsNavigationEntry(strings.helpAndUse, Icons.AutoMirrored.Filled.Help) {
                            activePage = GallerySettingsPage.HELP
                        },
                    ),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                }

                if (page == GallerySettingsPage.LIST_DISPLAY) {
                    val pageSizeValid = { value: Int ->
                        value in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE
                    }
                    val fixedColumnsValid = { value: Int ->
                        value == 0 ||
                            value in GallerySettings.MIN_FIXED_COLUMN_COUNT..GallerySettings.MAX_FIXED_COLUMN_COUNT
                    }
                    SettingsPanel(strings.listDisplay) {
                    SettingsGroup {
                        SettingsNumberItem(
                            label = strings.pageSize,
                            value = settings.pageSize,
                            isValid = pageSizeValid,
                            onSave = stateHolder::setPageSize,
                            saveScope = scope,
                            enabled = !state.saving,
                            index = 0,
                            count = 6,
                        )
                        SettingsNumberItem(
                            label = strings.fixedColumns,
                            value = settings.fixedColumnCount,
                            isValid = fixedColumnsValid,
                            onSave = stateHolder::setFixedColumnCount,
                            saveScope = scope,
                            enabled = !state.saving,
                            index = 1,
                            count = 6,
                        )
                        listOf(
                            Triple(strings.showLoadedRange, settings.showLoadedRange, stateHolder::setShowLoadedRange),
                            Triple(strings.showTilePosition, settings.showTilePosition, stateHolder::setShowTilePosition),
                            Triple(strings.showCounts, settings.showCounts, stateHolder::setShowCounts),
                        ).forEachIndexed { offset, (label, checked, onChange) ->
                            SegmentedSettingsSwitch(
                                label = label,
                                checked = checked,
                                onCheckedChange = onChange,
                                index = offset + 2,
                                count = 6,
                            )
                        }
                        SegmentedSettingsSwitch(
                            label = strings.showMissingMediaIcon,
                            checked = settings.showMissingMediaIcon,
                            onCheckedChange = stateHolder::setShowMissingMediaIcon,
                            index = 5,
                            count = 6,
                        )
                    }
                    }
                }

                if (page == GallerySettingsPage.TAG_SETTINGS || page == GallerySettingsPage.NOTE_STRUCTURE) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SettingsGroupSpacing),
                    ) {
                if (page == GallerySettingsPage.TAG_SETTINGS) {
                val galleryTagPrefixes = galleryTagPrefixesDraft
                    .lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                LaunchedEffect(galleryTagPrefixes, settings.galleryTagPrefixes) {
                    galleryTagPrefixesSaveJob?.cancel()
                    if (GalleryTagPrefixesCodec.isValid(galleryTagPrefixes) &&
                        galleryTagPrefixes != settings.galleryTagPrefixes
                    ) {
                        galleryTagPrefixesSaveJob = scope.launch {
                            delay(450)
                            stateHolder.saveGalleryTagPrefixes(galleryTagPrefixes)
                        }
                    }
                }
                SettingsPanel(strings.galleryEligibility) {
                SettingsEditableList(
                    title = strings.galleryTagPrefixes,
                    values = galleryTagPrefixesDraft.toSettingsLines(),
                    onChange = {
                        galleryTagPrefixesDraft = it.joinToString("\n")
                        galleryTagPrefixesError = false
                    },
                    strings = strings,
                    isValid = { GalleryTagPrefixesCodec.isValid(it) },
                    maxItems = GalleryTagPrefixesCodec.MAX_PREFIXES,
                )
                if (galleryTagPrefixesError) {
                    Text(
                        strings.invalidGalleryTagPrefixes,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                }
                SettingsPanel(strings.tagDisplayRules) {
                SettingsEditableList(
                    title = strings.includedTagPrefixes,
                    values = includedTagPrefixesDraft.toSettingsLines(),
                    onChange = {
                        includedTagPrefixesDraft = it.joinToString("\n")
                        tagDisplayPrefixesError = false
                    },
                    strings = strings,
                    isValid = GalleryTagDisplayPrefixesCodec::isValid,
                    maxItems = GalleryTagPrefixesCodec.MAX_PREFIXES,
                )
                SettingsEditableList(
                    title = strings.hiddenTagPrefixes,
                    values = hiddenTagPrefixesDraft.toSettingsLines(),
                    onChange = {
                        hiddenTagPrefixesDraft = it.joinToString("\n")
                        tagDisplayPrefixesError = false
                    },
                    strings = strings,
                    isValid = GalleryTagDisplayPrefixesCodec::isValid,
                    maxItems = GalleryTagPrefixesCodec.MAX_PREFIXES,
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
                LaunchedEffect(
                    includedTagPrefixes,
                    hiddenTagPrefixes,
                    settings.includedTagPrefixes,
                    settings.hiddenTagPrefixes,
                ) {
                    tagDisplayPrefixesSaveJob?.cancel()
                    if (tagDisplayPrefixesValid &&
                        (includedTagPrefixes != settings.includedTagPrefixes ||
                            hiddenTagPrefixes != settings.hiddenTagPrefixes)
                    ) {
                        tagDisplayPrefixesSaveJob = scope.launch {
                            delay(450)
                            stateHolder.saveTagDisplayPrefixes(includedTagPrefixes, hiddenTagPrefixes)
                        }
                    }
                    }
                }
                SettingsPanel(strings.tagColors) {
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
                    mutableStateOf(currentTagColorRules)
                }
                var editingTagColor by remember { mutableStateOf<Int?>(null) }
                var tagColorName by remember { mutableStateOf("") }
                var tagColorValue by remember { mutableStateOf("#5D5BEC") }
                LaunchedEffect(tagColorRulesDraft, currentTagColorRules) {
                    tagColorRulesSaveJob?.cancel()
                    if (storedTagColorRulesValid &&
                        GalleryTagColorCodec.isValid(tagColorRulesDraft) &&
                        tagColorRulesDraft != currentTagColorRules
                    ) {
                        tagColorRulesSaveJob = scope.launch {
                            delay(450)
                            stateHolder.saveTagColorRules(tagColorRulesDraft)
                        }
                    }
                }
                SettingsGroup {
                    tagColorRulesDraft.forEachIndexed { index, rule ->
                        val editRule = {
                            tagColorName = rule.prefix
                            tagColorValue =
                                "#${rule.color.toUInt().toString(16).takeLast(6).uppercase()}"
                            editingTagColor = index
                        }
                        SegmentedListItem(
                            onClick = editRule,
                            shapes = ListItemDefaults.segmentedShapes(
                                index,
                                tagColorRulesDraft.size + 1,
                            ),
                            colors = settingsItemColors(),
                            modifier = Modifier.testTag("TagColorRule_$index"),
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = Color(rule.color),
                                ) {}
                            },
                            supportingContent = {
                                Text("#${rule.color.toUInt().toString(16).takeLast(6).uppercase()}")
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    KaedeIconButton(
                                        label = "${strings.removeItem}: ${rule.prefix}",
                                        onClick = {
                                            tagColorRulesDraft = tagColorRulesDraft.filterIndexed { i, _ ->
                                                i != index
                                            }
                                        },
                                    ) { Icon(Icons.Filled.Delete, "${strings.removeItem}: ${rule.prefix}")                                     }
                                    KaedeIconButton(
                                        label = "${strings.editItem}: ${rule.prefix}",
                                        onClick = editRule,
                                    ) { Icon(Icons.Filled.Edit, "${strings.editItem}: ${rule.prefix}") }
                                }
                            },
                        ) { Text(rule.prefix) }
                    }
                    SettingsActionItem(
                        label = strings.addItem,
                        icon = Icons.Filled.Add,
                        index = tagColorRulesDraft.size,
                        count = tagColorRulesDraft.size + 1,
                        enabled = tagColorRulesDraft.size < GalleryTagColorCodec.MAX_RULES,
                    ) {
                        tagColorName = ""
                        tagColorValue = "#5D5BEC"
                        editingTagColor = -1
                    }
                }
                if (!storedTagColorRulesValid || !GalleryTagColorCodec.isValid(tagColorRulesDraft)) {
                    Text(
                        strings.invalidTagColors,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                editingTagColor?.let { index ->
                    fun updateRules(name: String, colorValue: String) {
                        if (!Regex("#[0-9a-fA-F]{6}").matches(colorValue)) return
                        val color = colorValue.removePrefix("#").toIntOrNull(16) ?: return
                        val rule = GalleryTagColorRule(name.trim(), color or 0xff000000.toInt())
                        val updated = if (index == -1) tagColorRulesDraft + rule else {
                            tagColorRulesDraft.mapIndexed { i, old -> if (i == index) rule else old }
                        }
                        if (GalleryTagColorCodec.isValid(updated)) tagColorRulesDraft = updated
                    }
                    val parsedColor = tagColorValue.takeIf {
                        Regex("#[0-9a-fA-F]{6}").matches(it)
                    }?.removePrefix("#")?.toIntOrNull(16)
                    val candidateRule = parsedColor?.let {
                        GalleryTagColorRule(tagColorName.trim(), it or 0xff000000.toInt())
                    }
                    val candidate = candidateRule?.let { rule ->
                        if (index == -1) tagColorRulesDraft + rule else {
                            tagColorRulesDraft.mapIndexed { i, old -> if (i == index) rule else old }
                        }
                    }
                    val valid = candidate != null && GalleryTagColorCodec.isValid(candidate)
                    AlertDialog(
                        onDismissRequest = { editingTagColor = null },
                        title = { Text(strings.tagColors) },
                        text = {
                            Column(
                                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextField(
                                    value = tagColorName,
                                    onValueChange = {
                                        tagColorName = it
                                        updateRules(it, tagColorValue)
                                    },
                                    label = { Text(strings.tagName) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                )
                                TextField(
                                    value = tagColorValue,
                                    onValueChange = {
                                        tagColorValue = it
                                        updateRules(tagColorName, it)
                                    },
                                    label = { Text(strings.tagColor) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                    leadingIcon = {
                                        candidateRule?.let {
                                            Surface(
                                                modifier = Modifier.size(24.dp),
                                                shape = CircleShape,
                                                color = Color(it.color),
                                            ) {}
                                        }
                                    },
                                )
                                Text(strings.chooseTagColor, style = MaterialTheme.typography.labelMedium)
                                ExpressiveColorPicker(
                                    color = candidateRule?.let { Color(it.color) } ?: Color(0xff5d5bec),
                                    label = strings.chooseTagColor,
                                    onColorChanged = { color ->
                                        val hex = "#${color.toArgb().toUInt().toString(16).takeLast(6).uppercase()}"
                                        tagColorValue = hex
                                        updateRules(tagColorName, hex)
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                                if (!valid) Text(strings.invalidTagColors, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (candidate != null) tagColorRulesDraft = candidate
                                    editingTagColor = null
                                },
                            ) { Text(strings.close) }
                        },
                    )
                }
                }

                SettingsPanel(strings.tagCategories) {
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
                var editingCategory by remember { mutableStateOf<Int?>(null) }
                var categoryRule by remember { mutableStateOf(GalleryTagCategoryRule("", "")) }
                LaunchedEffect(categoryDraft, storedCategorySettings) {
                    tagCategorySettingsSaveJob?.cancel()
                    if (storedCategorySettingsValid &&
                        GalleryTagCategoryCodec.isValid(categoryDraft) &&
                        categoryDraft != storedCategorySettings
                    ) {
                        tagCategorySettingsSaveJob = scope.launch {
                            delay(450)
                            stateHolder.saveTagCategorySettings(categoryDraft)
                        }
                    }
                }
                SettingsGroup {
                categoryDraft.categories.forEachIndexed { index, rule ->
                    SegmentedListItem(
                        onClick = {
                            categoryRule = rule
                            editingCategory = index
                        },
                        shapes = ListItemDefaults.segmentedShapes(index, categoryDraft.categories.size + 1),
                        colors = settingsItemColors(),
                        supportingContent = { Text(rule.path) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                KaedeIconButton(
                                    label = "${strings.removeItem}: ${rule.name}",
                                    onClick = {
                                        categoryDraft = categoryDraft.copy(
                                            categories = categoryDraft.categories.filterIndexed { i, _ ->
                                                i != index
                                            },
                                        )
                                        tagCategoriesError = false
                                    },
                                ) { Icon(Icons.Filled.Delete, "${strings.removeItem}: ${rule.name}") }
                                KaedeIconButton(
                                    label = "${strings.editItem}: ${rule.name}",
                                    onClick = {
                                        categoryRule = rule
                                        editingCategory = index
                                    },
                                ) { Icon(Icons.Filled.Edit, "${strings.editItem}: ${rule.name}") }
                            }
                        },
                    ) { Text(rule.name) }
                }
                SettingsActionItem(
                    label = strings.addItem,
                    icon = Icons.Filled.Add,
                    index = categoryDraft.categories.size,
                    count = categoryDraft.categories.size + 1,
                    onClick = {
                        categoryRule = GalleryTagCategoryRule(
                            name = "${strings.categoryName} ${categoryDraft.categories.size + 1}",
                            path = "",
                        )
                        editingCategory = -1
                    },
                    enabled = categoryDraft.categories.size < GalleryTagCategoryCodec.MAX_CATEGORIES,
                )
                }
                editingCategory?.let { index ->
                    fun updateCategoryRule(updatedRule: GalleryTagCategoryRule) {
                        categoryRule = updatedRule
                        val updated = categoryDraft.copy(
                            categories = if (index == -1) {
                                categoryDraft.categories + updatedRule
                            } else {
                                categoryDraft.categories.mapIndexed { i, rule ->
                                    if (i == index) updatedRule else rule
                                }
                            },
                        )
                        if (GalleryTagCategoryCodec.isValid(updated)) categoryDraft = updated
                    }
                    val candidate = categoryDraft.copy(
                        categories = if (index == -1) categoryDraft.categories + categoryRule else {
                            categoryDraft.categories.mapIndexed { i, rule -> if (i == index) categoryRule else rule }
                        },
                    )
                    val valid = GalleryTagCategoryCodec.isValid(candidate)
                    AlertDialog(
                        onDismissRequest = { editingCategory = null },
                        title = { Text(strings.tagCategories) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextField(
                                    value = categoryRule.name,
                                    onValueChange = {
                                        updateCategoryRule(categoryRule.copy(name = it))
                                    },
                                    label = { Text(strings.categoryName) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                )
                                TextField(
                                    value = categoryRule.path,
                                    onValueChange = {
                                        updateCategoryRule(categoryRule.copy(path = it))
                                    },
                                    label = { Text(strings.categoryPath) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                )
                                SettingsSwitch(
                                    strings.splitDeep, categoryRule.splitDeep,
                                    { updateCategoryRule(categoryRule.copy(splitDeep = it)) },
                                )
                                if (!valid) Text(strings.invalidTagCategories, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = valid,
                                onClick = {
                                    categoryDraft = candidate
                                    editingCategory = null
                                    tagCategoriesError = false
                                },
                            ) { Text(strings.close) }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingCategory = null }) { Text(strings.close) }
                        },
                    )
                }
                SettingsGroup {
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(0, 3),
                    colors = settingsItemColors(),
                ) {
                TextField(
                    value = categoryDraft.other.name,
                    onValueChange = { value ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(name = value),
                        )
                        tagCategoriesError = false
                    },
                    label = { Text(strings.otherCategoryName) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                }
                SettingsSwitch(
                    label = strings.otherCategoryEnabled,
                    index = 1,
                    count = 3,
                    checked = categoryDraft.other.enabled,
                    onCheckedChange = { enabled ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(enabled = enabled),
                        )
                    },
                )
                SettingsSwitch(
                    label = strings.splitDeep,
                    index = 2,
                    count = 3,
                    checked = categoryDraft.other.splitDeep,
                    onCheckedChange = { enabled ->
                        categoryDraft = categoryDraft.copy(
                            other = categoryDraft.other.copy(splitDeep = enabled),
                        )
                    },
                )
                }
                val categorySettingsValid = GalleryTagCategoryCodec.isValid(categoryDraft)
                if (!storedCategorySettingsValid || tagCategoriesError || !categorySettingsValid) {
                    Text(
                        strings.invalidTagCategories,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                }

                }

                }

                if (page == GallerySettingsPage.NOTE_STRUCTURE) {
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
                var noteOptionsVisible by remember { mutableStateOf(false) }
                var selectedNoteOptions by remember { mutableStateOf<GalleryNoteBlock?>(null) }
                LaunchedEffect(noteStructureDraft, storedNoteStructure) {
                    noteStructureSaveJob?.cancel()
                    if (storedNoteStructureValid &&
                        GalleryNoteStructureCodec.isValid(noteStructureDraft) &&
                        noteStructureDraft != storedNoteStructure
                    ) {
                        noteStructureSaveJob = scope.launch {
                            delay(450)
                            stateHolder.saveNoteStructureSettings(noteStructureDraft)
                        }
                    }
                }
                SettingsPanel(strings.frontmatterAliases) {
                    listOf(
                        strings.tagsKeys to noteStructureDraft.frontmatter.tagsKeys,
                        strings.titleKeys to noteStructureDraft.frontmatter.titleKeys,
                        strings.urlKeys to noteStructureDraft.frontmatter.urlKeys,
                        strings.publishedKeys to noteStructureDraft.frontmatter.publishedKeys,
                        strings.createdKeys to noteStructureDraft.frontmatter.createdKeys,
                        strings.updatedKeys to noteStructureDraft.frontmatter.updatedKeys,
                        strings.coverKeys to noteStructureDraft.frontmatter.coverKeys,
                    ).forEachIndexed { index, (label, values) ->
                        SettingsPanel(label) {
                            SettingsEditableList(
                                title = label,
                                values = values,
                                onChange = { keys ->
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
                                    val candidate = noteStructureDraft.copy(frontmatter = updated)
                                    if (GalleryNoteStructureCodec.isValid(candidate)) {
                                        noteStructureDraft = candidate
                                        noteStructureError = false
                                    }
                                },
                                strings = strings,
                                isValid = { keys ->
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
                                    GalleryNoteStructureCodec.isValid(
                                        noteStructureDraft.copy(frontmatter = updated),
                                    )
                                },
                                maxItems = 64,
                            )
                        }
                    }
                }
                if (noteOptionsVisible) {
                    AlertDialog(
                        onDismissRequest = { noteOptionsVisible = false },
                        title = {
                            Text(selectedNoteOptions?.displayName(strings) ?: strings.frontmatterAliases)
                        },
                        text = {
                            Column(
                                modifier = Modifier.heightIn(max = 440.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(SettingsGroupSpacing),
                            ) {
                                if (selectedNoteOptions in listOf(
                                        GalleryNoteBlock.MEMO, GalleryNoteBlock.RELATED,
                                        GalleryNoteBlock.POST_TEXT_END,
                                    )
                                ) {
                                    SettingsPanel(strings.noteStructure) {
                                        val (label, values) = when (selectedNoteOptions) {
                                            GalleryNoteBlock.MEMO -> strings.memoHeadings to noteStructureDraft.memoHeadings
                                            GalleryNoteBlock.RELATED -> strings.relatedHeadings to noteStructureDraft.relatedHeadings
                                            else -> strings.postTextEndHeadings to noteStructureDraft.postTextEndHeadings
                                        }
                                        SettingsEditableList(
                                            title = label,
                                            values = values,
                                            onChange = { headings ->
                                                noteStructureDraft = when (selectedNoteOptions) {
                                                    GalleryNoteBlock.MEMO -> noteStructureDraft.copy(memoHeadings = headings)
                                                    GalleryNoteBlock.RELATED -> noteStructureDraft.copy(relatedHeadings = headings)
                                                    else -> noteStructureDraft.copy(postTextEndHeadings = headings)
                                                }
                                                noteStructureError = false
                                            },
                                            strings = strings,
                                            isValid = { headings ->
                                                GalleryNoteStructureCodec.isValid(
                                                    when (selectedNoteOptions) {
                                                        GalleryNoteBlock.MEMO -> noteStructureDraft.copy(memoHeadings = headings)
                                                        GalleryNoteBlock.RELATED -> noteStructureDraft.copy(relatedHeadings = headings)
                                                        else -> noteStructureDraft.copy(postTextEndHeadings = headings)
                                                    },
                                                )
                                            },
                                            maxItems = 64,
                                        )
                                    }
                                }
                                if (selectedNoteOptions == GalleryNoteBlock.POST_TEXT) {
                                    SettingsGroup {
                                        SegmentedSettingsSwitch(
                                            label = strings.includeQuotedText,
                                            checked = noteStructureDraft.postTextIncludeQuote,
                                            onCheckedChange = { enabled ->
                                                noteStructureDraft = noteStructureDraft.copy(postTextIncludeQuote = enabled)
                                            },
                                            index = 0,
                                            count = 1,
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { noteOptionsVisible = false }) { Text(strings.close) }
                        },
                    )
                }
                SettingsPanel(strings.detailBlockOrder) {
                    SettingsGroup {
                        noteStructureDraft.blockOrder.forEachIndexed { index, block ->
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(
                                    index,
                                    noteStructureDraft.blockOrder.size,
                                ),
                                colors = settingsItemColors(),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                leadingContent = {
                                    Icon(noteBlockIcon(block), contentDescription = null)
                                },
                                trailingContent = {
                                    Row(
                                        modifier = Modifier.widthIn(max = 280.dp),
                                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (block !in listOf(GalleryNoteBlock.AUTHOR, GalleryNoteBlock.MEDIA)) {
                                            KaedeIconButton(label = strings.moveUp, onClick = {
                                                selectedNoteOptions = block
                                                noteOptionsVisible = true
                                            }) { Icon(Icons.Filled.Settings, block.displayName(strings)) }
                                        }
                                        if (block != GalleryNoteBlock.MEDIA && block != GalleryNoteBlock.POST_TEXT_END) {
                                            Switch(
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
                                                modifier = Modifier.size(48.dp).semantics {
                                                    contentDescription = strings.showBlock(block.displayName(strings))
                                                },
                                            )
                                        }
                                        KaedeIconButton(
                                            label = strings.moveDown,
                                            onClick = {
                                                noteStructureDraft = noteStructureDraft.copy(
                                                    blockOrder = noteStructureDraft.blockOrder
                                                        .moveNoteBlock(index, index - 1),
                                                )
                                            },
                                            enabled = index > 0,
                                        ) { Icon(Icons.Filled.ArrowUpward, strings.moveUp) }
                                        KaedeIconButton(
                                            label = strings.moveDown,
                                            onClick = {
                                                noteStructureDraft = noteStructureDraft.copy(
                                                    blockOrder = noteStructureDraft.blockOrder
                                                        .moveNoteBlock(index, index + 1),
                                                )
                                            },
                                            enabled = index < noteStructureDraft.blockOrder.lastIndex,
                                        ) { Icon(Icons.Filled.ArrowDownward, strings.moveDown) }
                                    }
                                },
                            ) { Text(block.displayName(strings)) }
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
                    }
                SettingsPanel(strings.linkResolution) {
                    SettingsGroup {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "${strings.markdownLink} / ${strings.wikiLink}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                ConnectedChoiceGroup(
                                    options = GalleryLinkResolution.entries,
                                    selected = noteStructureDraft.linkResolution,
                                    label = { resolution ->
                                        when (resolution) {
                                            GalleryLinkResolution.SHORTEST_PATH -> strings.shortestPath
                                            GalleryLinkResolution.RELATIVE_PATH -> strings.relativePath
                                            GalleryLinkResolution.ABSOLUTE_PATH -> strings.absolutePath
                                        }
                                    },
                                    iconFor = { resolution ->
                                        when (resolution) {
                                            GalleryLinkResolution.SHORTEST_PATH -> Icons.Filled.LocationSearching
                                            GalleryLinkResolution.RELATIVE_PATH -> Icons.Filled.Link
                                            GalleryLinkResolution.ABSOLUTE_PATH -> Icons.Filled.FolderOpen
                                        }
                                    },
                                    onSelect = { resolution ->
                                        noteStructureDraft = noteStructureDraft.copy(linkResolution = resolution)
                                    },
                                )
                            }
                        }
                    }
                }
                }
                }

                state.settings?.takeIf { page == GallerySettingsPage.NOTE_EXAMPLE }?.let { settings ->
                    val exampleStructure = remember(
                        settings.flutterTagSettingsJson,
                        settings.galleryTagPrefixes,
                    ) {
                        try {
                            GalleryNoteStructureCodec.decodeSettings(
                                settings.flutterTagSettingsJson,
                                settings.galleryTagPrefixes,
                            )
                        } catch (_: IllegalArgumentException) {
                            GalleryNoteStructureSettings(
                                galleryTagPrefixes = settings.galleryTagPrefixes,
                            )
                        }
                    }
                    GalleryExampleNotePage(
                        structure = exampleStructure,
                        strings = strings,
                    )
                }

                if (page == GallerySettingsPage.DATA) {
                    val dataRows = listOfNotNull(onImportSettings, onExportSettings).size + 1
                    var dataIndex = 0
                    SettingsGroup {
                        onImportSettings?.let { import ->
                            SettingsActionItem(
                                label = strings.importSettings,
                                icon = Icons.Filled.FileDownload,
                                index = dataIndex++,
                                count = dataRows,
                                enabled = !state.saving,
                            ) { import(stateHolder::importSerialized) }
                        }
                        onExportSettings?.let { export ->
                            SettingsActionItem(
                                label = strings.exportSettings,
                                icon = Icons.Filled.FileUpload,
                                index = dataIndex++,
                                count = dataRows,
                                enabled = !state.saving,
                            ) { stateHolder.exportSerialized()?.let(export) }
                        }
                        SettingsActionItem(
                            label = strings.reset,
                            icon = Icons.Filled.RestartAlt,
                            index = dataIndex++,
                            count = dataRows,
                            enabled = !state.saving,
                            destructive = true,
                        ) { confirmReset = true }
                    }
                    if (state.saving) {
                        LoadingIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = SettingsGroupSpacing),
                        )
                    }
                }

                if (page == GallerySettingsPage.ABOUT) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.size(176.dp).testTag("AboutLogoFrame"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painterResource(Res.drawable.kaede_gallery_icon),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                        .testTag("AboutLogo"),
                                )
                            }
                            Text("Kaede Gallery", style = MaterialTheme.typography.headlineLarge)
                            Surface(
                                modifier = Modifier.testTag("AboutVersionBadge"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryFixed,
                                contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                            ) {
                                Text(
                                    GalleryAppVersion,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            Text(
                                strings.aboutDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(SettingsGroupSpacing))
                    SettingsGroup {
                        SettingsActionItem(
                            label = strings.repository,
                            icon = Icons.Filled.Public,
                            index = 0,
                            count = 3,
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        ) { onExternalLink("https://github.com/hanaretamae/Kaede-Gallery") }
                        SettingsActionItem(
                            label = strings.license,
                            icon = Icons.Filled.Gavel,
                            index = 1,
                            count = 3,
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        ) {
                            onExternalLink(
                                "https://github.com/hanaretamae/Kaede-Gallery/blob/main/LICENSE",
                            )
                        }
                        SettingsActionItem(
                            label = strings.openSourceLicenses,
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            index = 2,
                            count = 3,
                            trailingIcon = Icons.Filled.ChevronRight,
                        ) {
                            selectedLicense = null
                            activePage = GallerySettingsPage.LICENSES
                        }
                    }
                }

                if (page == GallerySettingsPage.LICENSES) {
                    selectedLicense?.let { license ->
                        val licenseText = remember(license.assetPath, loadLicenseText) {
                            loadLicenseText(license.assetPath)
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ) {
                            if (licenseText == null) {
                                Text(strings.licenseTextUnavailable, Modifier.padding(16.dp))
                            } else {
                                val lines = remember(licenseText) { licenseText.lineSequence().toList() }
                                SelectionContainer {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().heightIn(
                                            min = 240.dp,
                                            max = 560.dp,
                                        ).padding(16.dp),
                                    ) {
                                        items(lines) { line ->
                                            Text(
                                                text = line.ifEmpty { " " },
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } ?: run {
                        Text(
                            strings.licenseOfflineDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        val licenses = galleryLicenseDocuments(strings)
                        SettingsGroup {
                            licenses.forEachIndexed { index, license ->
                                SettingsActionItem(
                                    label = license.title,
                                    icon = Icons.Filled.Description,
                                    index = index,
                                    count = licenses.size,
                                    trailingIcon = Icons.Filled.ChevronRight,
                                ) { selectedLicense = license }
                            }
                        }
                    }
                }

                if (page == GallerySettingsPage.HELP) {
                    val helpEntries = listOf(
                        Triple(strings.relatedNotes, strings.relatedNotesHelp, Icons.AutoMirrored.Filled.Label),
                        Triple(strings.noteStructure, strings.noteStructureUsageHelp, Icons.Filled.AccountTree),
                        Triple(strings.searchAndFiltering, strings.searchAndFilteringHelp, Icons.Filled.Search),
                        Triple(strings.galleryEligibility, strings.galleryTagPrefixesHelp, Icons.Filled.Search),
                        Triple(strings.tagDisplayRules, strings.tagDisplayRulesHelp, Icons.AutoMirrored.Filled.Label),
                        Triple(strings.tagColors, strings.tagColorsHelp, Icons.AutoMirrored.Filled.Label),
                        Triple(strings.tagCategories, strings.tagCategoriesHelp, Icons.AutoMirrored.Filled.Label),
                    ) + GalleryNoteBlock.entries.map { block ->
                            Triple(
                                block.displayName(strings),
                                strings.noteBlockHelp(block),
                                noteBlockIcon(block),
                            )
                        }
                    SettingsGroup {
                        helpEntries.forEachIndexed { index, (title, body, icon) ->
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(index, helpEntries.size),
                                colors = settingsItemColors(),
                                leadingContent = {
                                    Icon(icon, null)
                                },
                                supportingContent = { Text(body) },
                            ) { Text(title) }
                        }
                    }
                }
                if (showScanWarningDetails) {
                    AlertDialog(
                        onDismissRequest = { showScanWarningDetails = false },
                        title = { Text(strings.scanWarningDetails) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val warningCount = scanWarningCount ?: 0
                                Text(
                                    if (warningCount == 0) {
                                        strings.scanWarningNone
                                    } else {
                                        strings.scanWarningExplanation(warningCount)
                                    },
                                )
                                Text(strings.scanWarningCauses)
                                Text(strings.scanWarningEligibility)
                                Text(strings.scanWarningPrivacy)
                            }
                        },
                        confirmButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { showScanWarningDetails = false }) {
                                Text(strings.close)
                            }
                        },
                    )
                }
                if (confirmReset) {
                    AlertDialog(
                        onDismissRequest = { confirmReset = false },
                        title = { Text(strings.resetTitle) },
                        text = { Text(strings.resetMessage) },
                        confirmButton = {
                            TextButton(
                                shapes = ButtonDefaults.shapes(),
                                onClick = {
                                    confirmReset = false
                                    stateHolder.reset()
                                },
                                enabled = !state.saving,
                            ) { Text(strings.restoreDefaults) }
                        },
                        dismissButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmReset = false }) {
                                Text(strings.cancel)
                            }
                        },
                    )
                }
                if (confirmForgetVault) {
                    AlertDialog(
                        onDismissRequest = { confirmForgetVault = false },
                        title = { Text(strings.forgetVaultTitle) },
                        text = { Text(strings.forgetVaultMessage) },
                        confirmButton = {
                            TextButton(
                                shapes = ButtonDefaults.shapes(),
                                onClick = {
                                    confirmForgetVault = false
                                    val forgetVault = onForgetVault ?: return@TextButton
                                    scope.launch {
                                        when (val result = forgetVault()) {
                                            is RepositoryResult.Failure ->
                                                stateHolder.reportError(result.error)
                                            is RepositoryResult.Success<*> -> onClose()
                                        }
                                    }
                                },
                            ) { Text(strings.forgetVaultConfirm) }
                        },
                        dismissButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmForgetVault = false }) {
                                Text(strings.cancel)
                            }
                        },
                    )
                }
            }
        }
    }
}
}

@Composable
private fun GalleryExampleNotePage(
    structure: GalleryNoteStructureSettings = GalleryNoteStructureSettings(),
    strings: GallerySettingsStrings = if (
        platformSystemLanguagePreference() == LanguagePreference.JAPANESE
    ) JapaneseGallerySettingsStrings else EnglishGallerySettingsStrings,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val markdown = remember(structure) { galleryExampleMarkdown(structure) }
    Column(verticalArrangement = Arrangement.spacedBy(SettingsGroupSpacing)) {
        SettingsGroup {
            strings.exampleNoteExplanations.forEachIndexed { index, explanation ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index, strings.exampleNoteExplanations.size),
                    colors = settingsItemColors(),
                    leadingContent = { Text("${index + 1}.") },
                ) {
                    Text(explanation)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            SelectionContainer {
                Text(
                    markdown,
                    modifier = Modifier.padding(16.dp).heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState()),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(markdown)) },
            ) { Text(strings.copyExample) }
        }
    }
}

private fun String.toSettingsLines(): List<String> =
    lineSequence().filter(String::isNotEmpty).toList()

@Composable
private fun SettingsEditableList(
    title: String,
    values: List<String>,
    onChange: (List<String>) -> Unit,
    strings: GallerySettingsStrings,
    isValid: (List<String>) -> Boolean,
    maxItems: Int,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf("") }
    SettingsGroup {
        values.forEachIndexed { index, value ->
            SegmentedListItem(
                onClick = {
                    draft = value
                    editingIndex = index
                },
                shapes = ListItemDefaults.segmentedShapes(index, values.size + 1),
                colors = settingsItemColors(),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KaedeIconButton(
                            label = "${strings.removeItem}: $value",
                            onClick = { onChange(values.filterIndexed { i, _ -> i != index }) },
                        ) {
                            Icon(Icons.Filled.Delete, "${strings.removeItem}: $value")
                        }
                        KaedeIconButton(
                            label = "${strings.editItem}: $value",
                            onClick = {
                                draft = value
                                editingIndex = index
                            },
                        ) { Icon(Icons.Filled.Edit, "${strings.editItem}: $value") }
                    }
                },
            ) { Text(value) }
        }
        SettingsActionItem(
            label = strings.addItem,
            icon = Icons.Filled.Add,
            index = values.size,
            count = values.size + 1,
            enabled = values.size < maxItems,
        ) {
            draft = ""
            editingIndex = -1
        }
    }
    editingIndex?.let { index ->
        val candidate = if (index == -1) values + draft.trim() else {
            values.mapIndexed { i, value -> if (i == index) draft.trim() else value }
        }
        val valid = draft.isNotBlank() && isValid(candidate)
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text(title) },
            text = {
                TextField(
                    value = draft,
                    onValueChange = { value ->
                        draft = value
                        val updatedCandidate = if (index == -1) values + value.trim() else {
                            values.mapIndexed { i, old -> if (i == index) value.trim() else old }
                        }
                        if (value.isNotBlank() && isValid(updatedCandidate)) {
                            onChange(updatedCandidate)
                            if (index == -1) editingIndex = values.size
                        }
                    },
                    label = { Text(title) },
                    singleLine = true,
                    isError = !valid && draft.isNotEmpty(),
                    shape = MaterialTheme.shapes.large,
                    supportingText = { if (!valid) Text(strings.invalidItem) },
                )
            },
            confirmButton = {
                TextButton(onClick = { editingIndex = null }) { Text(strings.close) }
            },
        )
    }
}

internal fun galleryExampleMarkdown(structure: GalleryNoteStructureSettings): String = buildString {
    fun field(keys: List<String>, value: String) {
        keys.firstOrNull()?.let { appendLine("$it: $value") }
    }
    appendLine("---")
    field(structure.frontmatter.titleKeys, "\"Fictional gallery note\"")
    field(structure.frontmatter.urlKeys, "\"https://example.invalid/post\"")
    field(structure.frontmatter.publishedKeys, "\"2026-01-01\"")
    field(structure.frontmatter.createdKeys, "\"2026-01-01\"")
    field(structure.frontmatter.updatedKeys, "\"2026-01-02\"")
    field(structure.frontmatter.coverKeys, "\"media/fictional-image.png\"")
    structure.frontmatter.tagsKeys.firstOrNull()?.let { key ->
        appendLine("$key:")
        structure.galleryTagPrefixes.take(4).forEach { prefix ->
            val escaped = prefix.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
            appendLine("  - \"$escaped\"")
        }
    }
    appendLine("---")
    structure.blockOrder.forEach { block ->
        appendLine()
        when (block) {
            GalleryNoteBlock.AUTHOR -> appendLine("[Fictional author](https://example.invalid/author)")
            GalleryNoteBlock.MEDIA -> appendLine("![[media/fictional-image.png]]")
            GalleryNoteBlock.POST_TEXT -> {
                appendLine("# Fictional post text.")
                if (structure.postTextIncludeQuote) appendLine("> Fictional quotation.")
            }
            GalleryNoteBlock.POST_TEXT_END ->
                structure.postTextEndHeadings.firstOrNull()?.let { appendLine("# $it") }
            GalleryNoteBlock.RELATED -> structure.relatedHeadings.firstOrNull()?.let {
                appendLine("# $it")
                appendLine("- [[fictional-related-note]]")
            }
            GalleryNoteBlock.MEMO -> structure.memoHeadings.firstOrNull()?.let {
                appendLine("# $it")
                appendLine("- Fictional memo.")
            }
        }
    }
}

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

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    )
}

private fun GallerySettingsPage.displayName(strings: GallerySettingsStrings): String =
    when (this) {
        GallerySettingsPage.HOME -> strings.title
        GallerySettingsPage.LIST_DISPLAY -> strings.listDisplay
        GallerySettingsPage.LICENSES -> strings.openSourceLicenses
        GallerySettingsPage.NOTE_STRUCTURE -> strings.noteStructure
        GallerySettingsPage.TAG_SETTINGS -> strings.tagSettings
        GallerySettingsPage.ABOUT -> strings.about
        GallerySettingsPage.DATA -> strings.importExportReset
        GallerySettingsPage.HELP -> strings.helpAndUse
        GallerySettingsPage.NOTE_EXAMPLE -> strings.exampleNote
    }

@Composable
private fun SwitchCheckThumb(checked: Boolean): (@Composable () -> Unit)? =
    if (checked) {
        { Icon(Icons.Filled.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
    } else {
        null
    }

@Composable
private fun SegmentedSettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
) {
    val colors = settingsItemColors()
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = SwitchCheckThumb(checked),
            )
        },
    ) { Text(label) }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int = 0,
    count: Int = 1,
) {
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    )
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = SwitchCheckThumb(checked),
            )
        },
        colors = colors,
    ) { Text(label) }
}

/** Space between independent settings groups; segmented rows inside a group use [ListItemDefaults.SegmentedGap]. */
private val SettingsGroupSpacing = 16.dp

@Composable
private fun AppearanceSettingsList(
    settings: GallerySettings,
    stateHolder: SettingsStateHolder,
    strings: GallerySettingsStrings,
) {
    SettingsGroup {
        val themeRows = if (settings.appearance.theme == ThemePreference.LIGHT) 2 else 3
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, themeRows),
            colors = settingsItemColors(),
            supportingContent = {
                ConnectedChoiceGroup(
                    options = ThemePreference.entries,
                    selected = settings.appearance.theme,
                    label = {
                        when (it) {
                            ThemePreference.SYSTEM -> strings.system
                            ThemePreference.LIGHT -> strings.light
                            ThemePreference.DARK -> strings.dark
                        }
                    },
                    onSelect = stateHolder::setTheme,
                )
            },
        ) { Text(strings.theme) }
        SegmentedSettingsSwitch(
            strings.systemColor, settings.appearance.useSystemColor,
            stateHolder::setUseSystemColor, 1, themeRows,
        )
        if (themeRows == 3) {
            SegmentedSettingsSwitch(
                strings.pureBlack, settings.appearance.pureBlack,
                stateHolder::setPureBlack, 2, themeRows,
            )
        }
    }
    Spacer(Modifier.height(SettingsGroupSpacing))
    SettingsGroup {
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, 1),
            colors = settingsItemColors(),
            supportingContent = {
                ConnectedChoiceGroup(
                    options = FontPreference.entries,
                    selected = settings.appearance.font,
                    label = {
                        if (it == FontPreference.SYSTEM) strings.systemFont else strings.expressiveFont
                    },
                    onSelect = stateHolder::setFont,
                )
            },
        ) { Text(strings.font) }
    }
    Spacer(Modifier.height(SettingsGroupSpacing))
    SettingsGroup {
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, 1),
            colors = settingsItemColors(),
            supportingContent = {
                ConnectedChoiceGroup(
                    options = LanguagePreference.entries,
                    selected = settings.appearance.language,
                    label = {
                        when (it) {
                            LanguagePreference.SYSTEM -> strings.system
                            LanguagePreference.JAPANESE -> strings.japanese
                            LanguagePreference.ENGLISH -> strings.english
                        }
                    },
                    onSelect = stateHolder::setLanguage,
                )
            },
        ) { Text(strings.language) }
    }
    Spacer(Modifier.height(SettingsGroupSpacing))
}

private fun noteBlockIcon(block: GalleryNoteBlock) = when (block) {
    GalleryNoteBlock.AUTHOR -> Icons.Filled.Person
    GalleryNoteBlock.MEDIA -> Icons.Filled.Image
    GalleryNoteBlock.POST_TEXT -> Icons.Filled.Description
    GalleryNoteBlock.POST_TEXT_END -> Icons.Filled.SwapVert
    GalleryNoteBlock.MEMO -> Icons.AutoMirrored.Filled.LibraryBooks
    GalleryNoteBlock.RELATED -> Icons.Filled.Link
}

/** Settings rows sit on `surfaceBright` above the `surfaceContainer` page, like Android 16 settings. */
@Composable
private fun settingsItemColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright)

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            content()
    }
}

@Composable
private fun SettingsIconContainer(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

private class SettingsNavigationEntry(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsNavigationGroup(
    entries: List<SettingsNavigationEntry>,
    containerColor: Color,
    contentColor: Color,
) {
    SettingsGroup {
        entries.forEachIndexed { index, entry ->
            SegmentedListItem(
                onClick = entry.onClick,
                shapes = ListItemDefaults.segmentedShapes(index, entries.size),
                colors = settingsItemColors(),
                modifier = Modifier.fillMaxWidth(),
                leadingContent = { SettingsIconContainer(entry.icon, containerColor, contentColor) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            ) { Text(entry.label) }
        }
    }
}

@Composable
private fun SettingsActionItem(
    label: String,
    icon: ImageVector,
    index: Int,
    count: Int,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = settingsItemColors(),
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            if (trailingIcon == Icons.Filled.ChevronRight) {
                SettingsIconContainer(icon)
            } else {
                Icon(icon, null, tint = if (destructive) colorScheme.error else colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = trailingIcon?.let { { Icon(it, contentDescription = null) } },
    ) {
        Text(label, color = if (destructive) colorScheme.error else Color.Unspecified)
    }
}

@Composable
private fun SettingsNumberItem(
    label: String,
    value: Int,
    isValid: (Int) -> Boolean,
    onSave: (Int) -> Unit,
    saveScope: CoroutineScope,
    enabled: Boolean,
    index: Int,
    count: Int,
) {
    var draft by remember(value) { mutableStateOf(value.toString()) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    val parsed = draft.toIntOrNull()
    val valid = parsed != null && isValid(parsed)
    LaunchedEffect(draft, value, enabled) {
        saveJob?.cancel()
        if (enabled && valid && parsed != value) {
            saveJob = saveScope.launch {
                delay(400)
                onSave(parsed)
            }
        }
    }
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = settingsItemColors(),
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            TextField(
                value = draft,
                onValueChange = { text ->
                    if (text.length <= 3 && text.all { it in '0'..'9' }) draft = text
                },
                singleLine = true,
                isError = draft.isNotEmpty() && !valid,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (enabled && valid) onSave(parsed) },
                ),
                modifier = Modifier.width(88.dp),
            )
        },
    ) { Text(label) }
}
