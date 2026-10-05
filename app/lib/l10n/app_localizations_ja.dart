// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Japanese (`ja`).
class AppLocalizationsJa extends AppLocalizations {
  AppLocalizationsJa([String locale = 'ja']) : super(locale);

  @override
  String get languageEnglish => 'English';

  @override
  String get languageJapanese => '日本語';

  @override
  String get aboutThisApp => 'このアプリについて';

  @override
  String get absolute => '絶対';

  @override
  String get aCategoryWithTheSamePathAlreadyExists => '同じパスのカテゴリーがすでにあります。';

  @override
  String get accessToTheSelectedFolderIsUnavailablePleaseChoo =>
      '選択したフォルダへのアクセス権がありません。Vault を選び直してください。';

  @override
  String activeFilters(Object filterCount) {
    return '$filterCount 件の絞り込み';
  }

  @override
  String get add => '追加';

  @override
  String get addAFilterTag => 'フィルター対象のタグを追加';

  @override
  String addAHeadingFor(Object label) {
    return '$labelの見出しを追加';
  }

  @override
  String get addAHiddenTag => '非表示タグを追加';

  @override
  String addAKeyFor(Object title) {
    return '$title のキーを追加';
  }

  @override
  String get addCategory => 'カテゴリーを追加';

  @override
  String get addColorRule => '色設定を追加';

  @override
  String get addGalleryTargetTag => 'ギャラリー対象タグを追加';

  @override
  String get addHeading => '見出しを追加';

  @override
  String get addKey => 'キーを追加';

  @override
  String get addPath => 'パスを追加';

  @override
  String get addTag => 'タグを追加';

  @override
  String get addTagColor => 'タグ色を追加';

  @override
  String get all => 'すべて';

  @override
  String get allAND => 'すべて (AND)';

  @override
  String get allSettingsWereRestoredToTheirDefaults => 'すべての設定を既定値に戻しました。';

  @override
  String get allTags => 'すべてのタグ (*)';

  @override
  String get alwaysFixedAtTheTopCannotBeReordered => '常に先頭に固定（並べ替え不可）';

  @override
  String amongNotesThatWereReadSuccessfullyThoseWithoutTh(Object param1) {
    return '正常に読み取れたノートのうち、$param1 タグがないものは対象外です。';
  }

  @override
  String get andReadPermission => 'と読み取りアクセス権';

  @override
  String get anOfflineGalleryForBrowsingNotesAndMediaInsideYo =>
      'Obsidian Vault 内のノートとメディアを閲覧するオフラインギャラリーです。';

  @override
  String get any => 'いずれか (+)';

  @override
  String get app => 'アプリ';

  @override
  String get appearance => '外観';

  @override
  String get ascending => '昇順';

  @override
  String get atLeastOneFrontmatterTagKeyIsRequired => 'フロントマターのタグキーは1件以上必要です。';

  @override
  String get atLeastOneKeyIsRequiredToReadTags => 'タグを読み取るキーは最低1つ必要です。';

  @override
  String get author => '投稿者';

  @override
  String get authorExtraction => '投稿者の抽出';

  @override
  String get automaticallyRetrievesTheAuthorFromTheAuthorLink =>
      'ノート本文の先頭にある投稿者リンクから自動で取得します。独立した投稿者キーはありません。';

  @override
  String get automaticallySplitTagsAfterTheThirdLevel => '3層目以降のタグを自動で分ける';

  @override
  String get bodyOrderInTheFictionalMarkdownBelow => '下の架空Markdownの本文順';

  @override
  String get browseMediaFromYourObsidianVaultOffline =>
      'Obsidian Vault のメディアをオフラインで閲覧できます。';

  @override
  String get byDefaultTheIconIsHidden => '既定ではアイコンを表示しません。';

  @override
  String get canAccessTheMediaFile => 'メディアファイルにアクセスできません。';

  @override
  String get cancel => 'キャンセル';

  @override
  String get cannotSaveInsideTheVaultChooseADifferentDestinat =>
      'Vault 内には保存できません。別の保存先を選択してください。';

  @override
  String get categoryName => 'カテゴリー名';

  @override
  String get chooseAnAppToOpenTheImageOrVideo => '画像・動画を開くアプリを選択';

  @override
  String get chooseAnotherVault => '別の Vault を選択';

  @override
  String get chooseVault => 'Vault を選択';

  @override
  String get clearAllSearchAndFilters => '検索と絞り込みをすべて解除';

  @override
  String get close => '閉じる';

  @override
  String color(Object param1) {
    return '色 #$param1';
  }

  @override
  String get color_eac664 => '色';

  @override
  String get configureGalleryTargetTagsFiltersAndDisplayColor =>
      'ギャラリー対象タグ、絞り込み、表示色を設定します';

  @override
  String get configureListLoadingSizeItemCountsAndTileDisplay =>
      '一覧の読み込み単位や件数、タイルの表示を設定します';

  @override
  String get configureNoteItemOrderHeadingParsingAndListDispl =>
      'ノートの項目順や見出しの読み取り、一覧表示を設定します';

  @override
  String get content => 'コンテンツ';

  @override
  String get contentAfterTheHeadingsRegisteredHereIsNotTreate =>
      'ここに登録した見出しから後ろを投稿文として扱いません。この項目は順序に含まれますが、非表示にはできません。';

  @override
  String get copiedTheURL => 'URL をコピーしました。';

  @override
  String get copyMarkdown => 'Markdownをコピー';

  @override
  String get copyPageURL => 'ページ URL をコピー';

  @override
  String get couldnDisplayTheImage => '画像を表示できませんでした。';

  @override
  String get couldnGetTheItemCount => '件数を取得できませんでした';

  @override
  String get couldnLaunchTheFileManager => 'ファイルマネージャーを起動できませんでした。';

  @override
  String get couldnLoadTheList => '一覧を読み込めませんでした。';

  @override
  String get couldnLoadTheNoteDetails => 'ノートの詳細を読み込めませんでした。';

  @override
  String get couldnLoadTheTagList => 'タグ一覧を読み込めませんでした。';

  @override
  String get couldnLoadTheTagSettings => 'タグ設定を読み込めませんでした。';

  @override
  String get couldnOpenItInAnExternalAppCheckForACompatibleAp =>
      '外部アプリで開けませんでした。対応アプリを確認してください。';

  @override
  String get couldnOpenTheMedia => 'メディアを開けませんでした。';

  @override
  String get couldnOpenTheVaultCheckItsLocationAndPermissions =>
      'Vault を開けませんでした。場所とアクセス権を確認してください。';

  @override
  String get couldnOpenTheVaultMedia => 'Vault のメディアを開けませんでした。';

  @override
  String get couldNotDeleteTheVaultSInAppDataPleaseTryAgain =>
      'Vault のアプリ内データを削除できませんでした。もう一度お試しください。';

  @override
  String get couldNotFindTheItemAtThatPosition => '指定位置の項目が見つかりませんでした。';

  @override
  String get couldNotLoadListSettings => '一覧設定を読み込めませんでした。';

  @override
  String get couldNotLoadNoteSettings => 'ノート設定を読み込めませんでした。';

  @override
  String get couldNotLoadTagSettings => 'タグ設定を読み込めませんでした。';

  @override
  String get couldNotOpenTheDestinationForTheSettingsFile =>
      '設定ファイルの保存先を開けませんでした。';

  @override
  String get couldNotOpenTheSettingsFile => '設定ファイルを開けませんでした。';

  @override
  String get couldNotReadOrWriteTheSettingsFile => '設定ファイルを読み書きできませんでした。';

  @override
  String get couldNotResetSettings => '設定をリセットできませんでした。';

  @override
  String get couldNotSaveFrontmatterSettings => 'Frontmatter 設定を保存できませんでした。';

  @override
  String get couldNotSaveNoteStructureSettings => 'ノート構造の設定を保存できませんでした。';

  @override
  String get couldNotSavePagingSettings => 'ページング設定を保存できませんでした。';

  @override
  String get couldNotSaveSettingsToJSON => '設定をJSONに保存できませんでした。';

  @override
  String get couldNotSaveTheAppearanceSettings => '外観設定を保存できませんでした。';

  @override
  String get couldNotSaveToJSONBecauseTheSettingsCouldNotBeRe =>
      '設定を読み取れないため、JSONに保存できませんでした。';

  @override
  String get couldnPlayThisVideo => 'この動画を再生できませんでした。';

  @override
  String get couldnSaveTheTagSettings => 'タグ設定を保存できませんでした。';

  @override
  String get couldnSetAsWallpaper => '壁紙に設定できませんでした。';

  @override
  String get couldnShowTheFileInTheFileManager => 'ファイルマネージャーでファイルを表示できませんでした。';

  @override
  String get couldnSwitchToTheDetailsView => '詳細表示に切り替えられませんでした。';

  @override
  String get couldnToggleFullscreen => '全画面表示を切り替えられませんでした。';

  @override
  String count(Object count) {
    return '件数 $count';
  }

  @override
  String get countingItems => '件数を計算中';

  @override
  String get coverImageVideo => 'カバー画像・動画';

  @override
  String get created => '作成日';

  @override
  String get createdAt => '作成日時';

  @override
  String get dark => 'ダーク';

  @override
  String pageSizeRange(
    Object minPageSize,
    Object maxPageSize,
    Object defaultPageSize,
  ) {
    return '$minPageSize〜$maxPageSize（既定 $defaultPageSize）';
  }

  @override
  String get delete => '削除';

  @override
  String get deleteColorRule => '色設定を削除';

  @override
  String get deleteKey => 'キーを削除';

  @override
  String get deleteTheVaultSelectionIndexAndCache =>
      'Vault の選択情報、インデックスとキャッシュを削除します';

  @override
  String get descending => '降順';

  @override
  String description(Object label) {
    return '$labelの説明';
  }

  @override
  String get detailsOfUncheckedItems => '確認できなかった項目の詳細';

  @override
  String get displayLanguage => '表示言語';

  @override
  String get displayMode => '表示方法';

  @override
  String get document => '# 文書';

  @override
  String get documentHeadingInTheBody => '本文中の # 文書 見出し';

  @override
  String duplicateHiddenNoteBlock(Object entry) {
    return '重複した非表示ノートブロックです: $entry。';
  }

  @override
  String duplicateNoteBlock(Object entry) {
    return '重複したノートブロックです: $entry。';
  }

  @override
  String get editCategory => 'カテゴリーを編集';

  @override
  String get editColor => '色を編集';

  @override
  String get editTagColor => 'タグ色を編集';

  @override
  String get enterANameAndAPathInSourceArtOrSourceCountFormat =>
      '名前と、source/art または source/count/* 形式のパスを入力してください。';

  @override
  String get enterATagPathAndAColorInRRGGBBFormat =>
      'タグパスと #RRGGBB 形式の色を入力してください。';

  @override
  String enterAValueFrom1To(Object _maximumUnboundedGalleryJump) {
    return '1 から $_maximumUnboundedGalleryJump の範囲で入力してください。';
  }

  @override
  String enterAValueFrom1To_6f32bc(Object maximum) {
    return '1 から $maximum の範囲で入力してください。';
  }

  @override
  String get enterFullscreen => '全画面表示';

  @override
  String get examplesOfWhyItemsCouldNotBeChecked => '確認できない理由の例';

  @override
  String get exclude => '除外 (-)';

  @override
  String get exclude_a53aa1 => '除外';

  @override
  String get exitFullscreen => '全画面表示を終了';

  @override
  String get exportSettingsAsJSON => '設定をJSONでエクスポート';

  @override
  String get failedToLoadTheRequestedPosition => '指定位置の読み込みに失敗しました。';

  @override
  String get fictionalAuthorHttpsExampleInvalidAuthorsFiction =>
      '[架空の投稿者](https://example.invalid/authors/fictional)';

  @override
  String get fictionalAuthorLink => '架空の投稿者リンク';

  @override
  String get fictionalEmbeddedImage => '架空の画像埋め込み';

  @override
  String get fictionalNoteExample => '架空のノート例';

  @override
  String get fictionalPostText => '架空の投稿文';

  @override
  String get filesOrFoldersCannotBeReadTheNoteEncodingIsNotUT =>
      '・ファイルやフォルダーを読み取れない\n・ノートの文字コードが UTF-8 ではない\n・YAML の書式を解析できない\n・ノートのサイズやタグ数が上限を超えている';

  @override
  String get filterByTags => 'タグで絞り込む';

  @override
  String get filterCategories => 'フィルターのカテゴリー';

  @override
  String get forget => '忘れる';

  @override
  String get forgetThisVault => 'この Vault を忘れる';

  @override
  String get frontmatterDescription => 'Frontmatter の説明';

  @override
  String get frontmatterKey => 'Frontmatter キー';

  @override
  String get frontmatterSettings => 'Frontmatter の設定';

  @override
  String get frontmatterTags => 'Frontmatterタグ';

  @override
  String get galleryTagSettingsAreNotInitialized => 'タグ設定が初期化されていません。';

  @override
  String get galleryTargetTags => 'ギャラリー対象タグ';

  @override
  String get githubRepository => 'GitHub リポジトリ';

  @override
  String get groupByNote => 'ノートごとにまとめる';

  @override
  String get hasMemo => '覚書あり';

  @override
  String get hasRelated => '関連あり';

  @override
  String get hasVideo => '動画あり';

  @override
  String get headingName => '見出し名';

  @override
  String headingsRecognizedAs(Object label) {
    return '$labelとして認識する見出し';
  }

  @override
  String get helpAndHowToUse => 'ヘルプと使い方';

  @override
  String get hiddenTags => '非表示にするタグ';

  @override
  String get howNoteLinksAreResolved => 'ノートリンクの解決方法';

  @override
  String get httpsExampleInvalidPostsAoikasumi0001NISketchedA =>
      'https://example.invalid/posts/aoikasumi-0001\nn> 雨上がりの窓辺で、架空の青い鳥をスケッチしました。';

  @override
  String get ifOffQuotedLinesAreExcludedFromThePostText =>
      'オフにすると引用行を投稿文から除外します。';

  @override
  String get importExportAndReset => 'インポート・エクスポート・リセット';

  @override
  String get importSettingsFromJSON => '設定をJSONからインポート';

  @override
  String inAppIndexCacheAndSelectionDataWillBeRemovedFile(
    Object param1,
    Object vaultAccessText,
  ) {
    return '$param1 のアプリ内インデックス、キャッシュ、選択情報$vaultAccessTextを削除します。Vault 内のファイルと外観・タグなどのアプリ設定は変更しません。';
  }

  @override
  String get includeInAny => 'いずれかに含める';

  @override
  String get includeQuotesInPostText => '投稿文に引用（> ）を含める';

  @override
  String get information => '情報';

  @override
  String get informationAndLicenses => '情報とライセンス';

  @override
  String invalidBooleanSetting(Object key) {
    return '真偽値設定が不正です: $key。';
  }

  @override
  String invalidFrontmatterKeys(Object key) {
    return 'フロントマターのキー一覧が不正です: $key。';
  }

  @override
  String get invalidFrontmatterKeySettings => 'フロントマターのキー設定が不正です。';

  @override
  String get invalidGalleryItemNumberFlag => 'アイテム番号表示設定が不正です。';

  @override
  String get invalidGalleryNoteStructureSettings => 'ノート構造設定が不正です。';

  @override
  String get invalidGalleryPaginationItemCountFlag => 'アイテム数表示設定が不正です。';

  @override
  String get invalidGalleryPaginationPageSize => 'ギャラリーページサイズが不正です。';

  @override
  String get invalidGalleryPaginationSettings => 'ページネーション設定が不正です。';

  @override
  String get invalidGalleryTagCategorySettings => 'タグカテゴリー設定が不正です。';

  @override
  String get invalidGalleryTagColorRule => 'タグ色ルールが不正です。';

  @override
  String get invalidGalleryTagSettings => 'タグ設定が不正です。';

  @override
  String get invalidHiddenNoteBlockEntry => '非表示ノートブロックの項目が不正です。';

  @override
  String get invalidHiddenNoteBlocks => '非表示ノートブロック設定が不正です。';

  @override
  String get invalidLinkResolutionSetting => 'リンク解決設定が不正です。';

  @override
  String get invalidMissingMediaIconFlag => 'メディアなしアイコン表示設定が不正です。';

  @override
  String get invalidNoteBlockOrder => 'ノートブロック順序が不正です。';

  @override
  String get invalidNoteBlockOrderEntry => 'ノートブロック順序の項目が不正です。';

  @override
  String invalidNoteStructureHeadingList(Object key) {
    return 'ノート構造の見出し一覧が不正です: $key。';
  }

  @override
  String get invalidOtherCategoryName => 'その他カテゴリー名が不正です。';

  @override
  String get invalidOtherCategorySettings => 'その他カテゴリー設定が不正です。';

  @override
  String get invalidTagCategoryList => 'タグカテゴリー一覧が不正です。';

  @override
  String get invalidTagCategoryRule => 'タグカテゴリーのルールが不正です。';

  @override
  String item(Object itemNumber) {
    return '項目 $itemNumber';
  }

  @override
  String items(Object count) {
    return '$count 件';
  }

  @override
  String items_89e724(Object filterState, Object count) {
    return '$filterState、$count 件';
  }

  @override
  String items_98c0e6(Object totalCount) {
    return '$totalCount 件';
  }

  @override
  String items1To(Object maximum) {
    return '1 から $maximum 件目まで';
  }

  @override
  String get itemsThatCouldNotBeChecked => '確認できなかった項目';

  @override
  String itemsTheirContentsCouldNotBeCheckedSoWeCannotDet(Object warnings) {
    return '$warnings 件。内容を確認できないため、ギャラリー対象かは判定できません。';
  }

  @override
  String get jump => '移動';

  @override
  String jumpedToItem(Object param1) {
    return '$param1 件目へ移動しました。';
  }

  @override
  String jumpingToItem(Object param1) {
    return '$param1 件目へ移動しています。';
  }

  @override
  String get jumpToPosition => '指定位置へ移動';

  @override
  String get jumpToPosition_a998c4 => '指定した位置へ移動';

  @override
  String get kaedeGalleryIcon => 'Kaede Gallery のアイコン';

  @override
  String get language => '言語';

  @override
  String get light => 'ライト';

  @override
  String get linkToAFictionalNote => '架空のノートへのリンク';

  @override
  String get listDisplay => '一覧表示';

  @override
  String loadingThePageContainingItem(Object param1) {
    return '$param1 件目のページを読み込んでいます。';
  }

  @override
  String get loadSettingsThatWereExportedEarlier => '以前にエクスポートした設定を読み込みます';

  @override
  String get loopCurrentVideo => '1本をループ';

  @override
  String get markdownLinksImageLinksAndObsidianWikilinksEmbed =>
      '関連見出し内の Markdown link / image link と Obsidian wikilink / embed は、Vault内のノートを指していればタップして詳細を開けます。';

  @override
  String get markdownWikilinkNoteResolution => 'Markdown / Wikilink のノート解決';

  @override
  String get media => 'メディア';

  @override
  String get memo => '覚書';

  @override
  String message0OfItems(Object totalCount) {
    return '$totalCount 件中 0 件';
  }

  @override
  String moveDown(Object label) {
    return '$labelを下へ';
  }

  @override
  String get moveDown_5f88c6 => '下へ';

  @override
  String moveUp(Object label) {
    return '$labelを上へ';
  }

  @override
  String get moveUp_467fe2 => '上へ';

  @override
  String get multipleImages => '複数画像';

  @override
  String get mute => 'ミュート';

  @override
  String get noGalleryTargetTagsAreConfiguredSoEvenNotesThatW =>
      'ギャラリー対象タグが未設定のため、正常に読み取れたノートも対象外です。';

  @override
  String get noHeadingsAreAssigned => '割り当て済みの見出しはありません。';

  @override
  String get noMatchingMedia => '該当するメディアはありません。';

  @override
  String get noMatchingNotes => '該当するノートはありません。';

  @override
  String get noMatchingOptions => '該当する選択肢がありません';

  @override
  String get noMatchingTags => '一致するタグがありません。';

  @override
  String get none => 'ありません';

  @override
  String get noTargetTagsAreSet => '対象タグはありません。';

  @override
  String get noteDetailBlockOrder => 'ノート詳細のブロック順序';

  @override
  String get noteDetailsActions => 'ノート詳細操作';

  @override
  String get noteNameTagTagTag => 'ノート名 / #タグ / -#タグ / &#タグ';

  @override
  String get notes => 'ノート';

  @override
  String get notes_ab7203 => '覚書';

  @override
  String get noteSearchSupportsNamesAndPathsPlusTagTagAndTagT =>
      'ノート検索は名前・パスに加えて #タグ、-#タグ、&#タグに対応します。下のタグ検索は絞り込み候補の表示だけを絞ります。';

  @override
  String get notesInPlainTextQuotesAndCode => '平文・引用・コード内の覚書';

  @override
  String get notesNSoftenTheWindowReflectionsALittleKeepTheBl =>
      '## 覚書\nn- 窓の反射を少し弱める\n - 青の彩度は控えめにする\n 次は夕方の光を試す';

  @override
  String get noteStructure => 'ノート構造';

  @override
  String get noteStructureAndDisplay => 'ノート構造と表示';

  @override
  String get notesWithThisTagOrAnyChildTagAreIncludedMultiple =>
      'このタグ、または下位タグが付いたノートを対象にします。複数指定は OR です。空にすると対象ノートはありません。';

  @override
  String get notSelected => '未選択';

  @override
  String get notSet => '設定なし';

  @override
  String get noVaultIsSelected => '選択中の Vault がありません。';

  @override
  String get observationsAfterTheRain => '# 雨上がりの観測';

  @override
  String ofItems(Object firstItem, Object lastItem, Object totalCount) {
    return '$totalCount 件中 $firstItem - $lastItem 件';
  }

  @override
  String get openAuthorProfile => '投稿者のプロフィールを開く';

  @override
  String get openLink => 'リンクを開く';

  @override
  String get openMedia => 'メディアを開く';

  @override
  String get openNote => 'ノートを開く';

  @override
  String get openNoteInObsidian => 'Obsidian でノートを開く';

  @override
  String get openOriginalPage => '元のページを開く';

  @override
  String get openSourceLicenses => 'オープンソースライセンス';

  @override
  String get other => 'その他';

  @override
  String get otherCategory => 'その他カテゴリー';

  @override
  String get otherCategoryName => 'その他カテゴリーの名前';

  @override
  String get pageSize => 'ページサイズ';

  @override
  String get pagingAndListDisplay => 'ページングと一覧表示';

  @override
  String get pause => '一時停止';

  @override
  String get people => '人数';

  @override
  String get play => '再生';

  @override
  String get postText => '投稿文';

  @override
  String get postTextEnd => '投稿文の終端';

  @override
  String get postURL => '投稿URL';

  @override
  String get preparingToJump => '移動を準備しています。';

  @override
  String get productionNotes => '制作メモ';

  @override
  String get published => '公開日';

  @override
  String get publishedAt => '公開日時';

  @override
  String get pureBlack => 'ピュアブラック';

  @override
  String get readsContentUnderTheRegisteredHeadingsAsRelatedI =>
      '登録した見出し配下を関連項目として読み取ります。Vault内のノートリンクはタップして開けます。';

  @override
  String get readsTextQuotesAndCodeUnderTheRegisteredHeadings =>
      '登録した見出し配下の文章、引用、コードを覚書として読み取ります。';

  @override
  String get related => '関連';

  @override
  String get relatedNColorStudyFictionalColorStudyMd =>
      '## 関連\nn- [色の記録](./fictional-color-study.md)';

  @override
  String get relatedNotes => '関連ノート';

  @override
  String get relative => '相対';

  @override
  String get reloadVaultChanges => 'Vault の変更を再読み込み';

  @override
  String get requireAll => 'すべてに含める';

  @override
  String get rescan => '再走査';

  @override
  String get resetSettings => '設定をリセット';

  @override
  String get resetToDefaults => '初期設定に戻す';

  @override
  String get restoreAllAppearanceTagAndNoteStructureSettingsT =>
      '外観・タグ・ノート構造の設定をすべて既定値に戻します。Vault内のノートは変更しません。';

  @override
  String get restoreAppearanceAndNoteSettingsToTheirDefaults =>
      '外観・ノート設定を既定値に戻します';

  @override
  String get restoreDefaults => '既定値に戻す';

  @override
  String get restoreDefaults_c4cee4 => '初期設定に戻す';

  @override
  String get retrievesTheAuthorNameAndURLFromTheAuthorLinkAtT =>
      '本文冒頭の投稿者リンクから名前とURLを取得します。@ がないリンクにも対応します。';

  @override
  String get save => '保存';

  @override
  String get saveAppearanceAndTagSettingsToOneJSONFile =>
      '外観とタグ設定を1つのJSONファイルに保存します';

  @override
  String get screenTheme => '画面テーマ';

  @override
  String get searchAndFiltering => '検索と絞り込み';

  @override
  String get searchNotes => 'ノートを検索';

  @override
  String get searchTags => 'タグを検索';

  @override
  String get selectedVault => '選択中の Vault';

  @override
  String get selectThisVault => 'この Vault を選択';

  @override
  String get setAsWallpaper => '壁紙に設定しました。';

  @override
  String get setImageAsWallpaper => '画像を壁紙にする';

  @override
  String get settings => '設定';

  @override
  String settings_5e5451(Object label) {
    return '$labelの設定';
  }

  @override
  String get settingsHaveNotBeenInitialized => '設定が初期化されていません。';

  @override
  String get settingsWereImported => '設定をインポートしました。';

  @override
  String get settingsWereSavedAsJSON => '設定をJSONで保存しました。';

  @override
  String get shortest => '最短';

  @override
  String get shortestChoosesTheClosestMatchingNoteWithTheSame =>
      '最短はVault内の同名ノートから参照元に近いものを選びます。相対は現在のノート位置を基準にし、絶対はVaultのルートを基準にします。Vault外へ解決されるリンクは無視します。';

  @override
  String show(Object label) {
    return '$labelを表示';
  }

  @override
  String get showAllMedia => 'すべてのメディアを表示';

  @override
  String get showAnIconForHiddenMedia => '未表示メディアのアイコンを表示';

  @override
  String get showEachTileSPositionInTheList => 'タイルに一覧内の位置（何件目）を表示';

  @override
  String get showFileInFileManager => 'ファイルマネージャーでファイルを表示';

  @override
  String get showLoadedItemCount => '読み込み済み件数を表示';

  @override
  String get showOtherCategory => 'その他カテゴリーを表示';

  @override
  String get showsTheNumberOfItemsCurrentlyDisplayedInTheTopB =>
      '上部バーに現在表示中の件数を表示します。';

  @override
  String get showThemAsSeparateCategoriesForEachParentTag =>
      '親タグごとに別のカテゴリーとして表示します。';

  @override
  String get sortDirection => '並べ替えの向き';

  @override
  String get sortField => '並べ替えの基準';

  @override
  String get specificItemNamesAndNoteContentsAreNotShownOnThi =>
      '具体的な項目名やノートの内容はこの画面に表示しません。アクセス権やファイルの状態を確認してから再走査してください。';

  @override
  String splitDeeperLevels(Object path) {
    return '$path ・ 3層目以降を分割';
  }

  @override
  String get startAtItem => '何件目から表示';

  @override
  String get stopsExtractingPostTextWhenThisHeadingIsReachedH =>
      'この見出しに到達したところで投稿文の抽出を終了します。見出しの階層は問わず、複数の見出し名を登録できます。';

  @override
  String get switchVault => 'Vault を切り替え';

  @override
  String get system => 'システム';

  @override
  String get systemColorMaterialYou => 'システムカラー（Material You）';

  @override
  String get tagColors => 'タグの色';

  @override
  String get tagPath => 'タグパス';

  @override
  String get tags => 'タグ';

  @override
  String get tagSettings => 'タグ設定';

  @override
  String tagsIn(Object title) {
    return '$title のタグ';
  }

  @override
  String get tagsIncludedInFilters => 'フィルターに含めるタグ';

  @override
  String get tagsTitleAndMoreFixed => 'タグ・タイトルなど（固定）';

  @override
  String get theFictionalNoteExampleWasCopied => '架空のノート例をコピーしました。';

  @override
  String get theFrontmatterTagsAndBodyBlockOrderInThisExample =>
      'この例のFrontmatterタグと本文のブロック順は現在の設定を反映しています。Frontmatterは固定で、本文は下のMarkdownの順に並びます。Vaultへ自動保存されません。';

  @override
  String get theItemCountIsStillLoadingButYouCanJumpToAPositi =>
      '件数を計算中ですが、指定した位置へ移動できます';

  @override
  String get theItemCountIsUnavailableButYouCanJumpToAPositio =>
      '件数を取得できませんが、指定した位置へ移動できます';

  @override
  String get theme => 'テーマ';

  @override
  String get theMediaFileWasNotFoundPleaseRescan =>
      'メディアファイルが見つかりません。再走査してください。';

  @override
  String get theNoteWasNotFoundPleaseRescan => 'ノートが見つかりません。再走査してください。';

  @override
  String get thereAreNoCategories => 'カテゴリーはありません';

  @override
  String get thereAreNoFictionalTagsToDisplay => '表示できる架空タグはありません';

  @override
  String get thereAreNoFilterTags => 'フィルター対象のタグはありません';

  @override
  String get thereAreNoHiddenTags => '非表示タグはありません';

  @override
  String get thereAreNoItemsToShow => '表示できる項目がありません';

  @override
  String get thereIsNoMediaToShowInThisNote => 'このノートに表示できるメディアはありません。';

  @override
  String get theseAreTheNoteSImagesAndVideosTheyAreNotDuplica =>
      'ノートの画像・動画です。詳細欄では重複表示せず、閲覧画面のメディア領域に表示します。';

  @override
  String get theseItemsAreNotConfirmedToBeOutsideTheGalleryBe =>
      'これらはギャラリー対象外と確定した項目ではありません。ノート内のタグを確認できないため、対象かどうかを判定できず、一覧にも追加していません。';

  @override
  String get theSelectedAppCouldNotShowTheFile => '選択したアプリでファイルを表示できませんでした。';

  @override
  String get theSettingsJSONIsMissingRequiredFields => '設定JSONに必要な項目がありません。';

  @override
  String get theSettingsJSONIsTooLargeOrIsNotARegularFile =>
      '設定JSONが大きすぎるか、通常ファイルではありません。';

  @override
  String get theTotalIsStillLoadingEnterAPositiveInteger =>
      '件数を計算中です。正の整数を指定できます。';

  @override
  String get thisIsMetadataSuchAsTagsTitlesURLsDatesAndCovers =>
      'タグ、タイトル、URL、日付、カバーなどのメタデータです。詳細欄の先頭に固定され、並べ替えや非表示はできません。';

  @override
  String get thisIsTheBodyPostTextContentBeforeTheHeadingSpec =>
      '本文の投稿文です。投稿文の終端に指定した見出しより前を表示します。';

  @override
  String thisScanCouldNotCheckItems(Object warnings) {
    return '今回の走査では $warnings 件を確認できませんでした。';
  }

  @override
  String get thisScanFoundNoUncheckedItems => '今回の走査では確認できなかった項目はありません。';

  @override
  String get thisSettingsJSONFormatIsNotSupported => '未対応の設定JSONです。';

  @override
  String get title => 'タイトル';

  @override
  String get treatsContentUnderTheseHeadingsAsNotesHeadingDep =>
      '見出し配下の内容を覚書として扱います。見出しの階層は問わず、複数の見出し名を登録できます。';

  @override
  String get treatsItemsUnderTheseHeadingsAsRelatedContentHea =>
      '見出し配下の項目を関連として扱います。見出しの階層は問わず、複数の見出し名を登録できます。';

  @override
  String get turnLoopingOff => 'ループをオフ';

  @override
  String get unableToReadTheNotePleaseCheckAccessPermissions =>
      'ノートを読み込めません。アクセス権を確認してください。';

  @override
  String get unknownLinkResolutionSetting => '不明なリンク解決設定です。';

  @override
  String unknownNoteBlock(Object entry) {
    return '不明なノートブロックです: $entry。';
  }

  @override
  String unknownVisibleNoteBlock(Object entry) {
    return '不明な表示対象ノートブロックです: $entry。';
  }

  @override
  String get unmute => 'ミュートを解除';

  @override
  String get updated => '更新日';

  @override
  String get updatedAt => '更新日時';

  @override
  String get useABlackBackgroundInTheDarkThemeTheSystemColorC =>
      'ダークテーマの背景面を黒にします。システムカラーはアクセントとして併用できます';

  @override
  String get useTheInfoIconsInItemOrderToCheckHowEachSectionI =>
      '項目順の説明アイコンで読み取り方法を確認できます。見出しやFrontmatterキーなど変更できる設定は、各項目の設定ボタンにまとめています。';

  @override
  String get useTheSystemAccentColor => 'システムのアクセントカラーを使用します';

  @override
  String get vault => '保管庫';

  @override
  String version(Object version) {
    return 'バージョン $version';
  }

  @override
  String get video => '動画';

  @override
  String get videoPlaybackPosition => '動画の再生位置';

  @override
  String get viewNoteFormatExample => 'ノート形式の例を見る';

  @override
  String get viewOrCopyAFictionalNoteExample => '架空のノート例を見る・コピー';

  @override
  String get youCanAlsoReviewADisplayExampleThatReflectsTheCu =>
      '設定中の項目順を反映した表示例も確認できます';
}
