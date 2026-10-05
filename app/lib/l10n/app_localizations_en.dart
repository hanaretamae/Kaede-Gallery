// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get languageEnglish => 'English';

  @override
  String get languageJapanese => 'Japanese';

  @override
  String get aboutThisApp => 'About this app';

  @override
  String get absolute => 'Absolute';

  @override
  String get aCategoryWithTheSamePathAlreadyExists =>
      'A category with the same path already exists.';

  @override
  String get accessToTheSelectedFolderIsUnavailablePleaseChoo =>
      'Access to the selected folder is unavailable. Please choose the Vault again.';

  @override
  String activeFilters(Object filterCount) {
    return '$filterCount active filters';
  }

  @override
  String get add => 'Add';

  @override
  String get addAFilterTag => 'Add a filter tag';

  @override
  String addAHeadingFor(Object label) {
    return 'Add a heading for $label';
  }

  @override
  String get addAHiddenTag => 'Add a hidden tag';

  @override
  String addAKeyFor(Object title) {
    return 'Add a key for $title';
  }

  @override
  String get addCategory => 'Add category';

  @override
  String get addColorRule => 'Add color rule';

  @override
  String get addGalleryTargetTag => 'Add gallery target tag';

  @override
  String get addHeading => 'Add heading';

  @override
  String get addKey => 'Add key';

  @override
  String get addPath => 'Add path';

  @override
  String get addTag => 'Add tag';

  @override
  String get addTagColor => 'Add tag color';

  @override
  String get all => 'All';

  @override
  String get allAND => 'All (AND)';

  @override
  String get allSettingsWereRestoredToTheirDefaults =>
      'All settings were restored to their defaults.';

  @override
  String get allTags => 'All tags (*)';

  @override
  String get alwaysFixedAtTheTopCannotBeReordered =>
      'Always fixed at the top (cannot be reordered)';

  @override
  String amongNotesThatWereReadSuccessfullyThoseWithoutTh(Object param1) {
    return 'Among notes that were read successfully, those without the $param1 tag are excluded.';
  }

  @override
  String get andReadPermission => ' and read permission';

  @override
  String get anOfflineGalleryForBrowsingNotesAndMediaInsideYo =>
      'An offline gallery for browsing notes and media inside your Obsidian Vault.';

  @override
  String get any => 'Any (+)';

  @override
  String get app => 'App';

  @override
  String get appearance => 'Appearance';

  @override
  String get ascending => 'Ascending';

  @override
  String get atLeastOneFrontmatterTagKeyIsRequired =>
      'At least one frontmatter tag key is required.';

  @override
  String get atLeastOneKeyIsRequiredToReadTags =>
      'At least one key is required to read tags.';

  @override
  String get author => 'Author';

  @override
  String get authorExtraction => 'Author extraction';

  @override
  String get automaticallyRetrievesTheAuthorFromTheAuthorLink =>
      'Automatically retrieves the author from the author link at the start of the note body. There is no separate author key.';

  @override
  String get automaticallySplitTagsAfterTheThirdLevel =>
      'Automatically split tags after the third level';

  @override
  String get bodyOrderInTheFictionalMarkdownBelow =>
      'Body order in the fictional Markdown below';

  @override
  String get browseMediaFromYourObsidianVaultOffline =>
      'Browse media from your Obsidian Vault offline.';

  @override
  String get byDefaultTheIconIsHidden => 'By default, the icon is hidden.';

  @override
  String get canAccessTheMediaFile => 'Can\' access the media file.';

  @override
  String get cancel => 'Cancel';

  @override
  String get cannotSaveInsideTheVaultChooseADifferentDestinat =>
      'Cannot save inside the Vault. Choose a different destination.';

  @override
  String get categoryName => 'Category name';

  @override
  String get chooseAnAppToOpenTheImageOrVideo =>
      'Choose an app to open the image or video';

  @override
  String get chooseAnotherVault => 'Choose another Vault';

  @override
  String get chooseVault => 'Choose Vault';

  @override
  String get clearAllSearchAndFilters => 'Clear all search and filters';

  @override
  String get close => 'Close';

  @override
  String color(Object param1) {
    return 'Color #$param1';
  }

  @override
  String get color_eac664 => 'Color';

  @override
  String get configureGalleryTargetTagsFiltersAndDisplayColor =>
      'Configure gallery target tags, filters, and display colors.';

  @override
  String get configureListLoadingSizeItemCountsAndTileDisplay =>
      'Configure list loading size, item counts, and tile display.';

  @override
  String get configureNoteItemOrderHeadingParsingAndListDispl =>
      'Configure note item order, heading parsing, and list display.';

  @override
  String get content => 'Content';

  @override
  String get contentAfterTheHeadingsRegisteredHereIsNotTreate =>
      'Content after the headings registered here is not treated as post text. This item remains in the order but cannot be hidden.';

  @override
  String get copiedTheURL => 'Copied the URL.';

  @override
  String get copyMarkdown => 'Copy Markdown';

  @override
  String get copyPageURL => 'Copy page URL';

  @override
  String get couldnDisplayTheImage => 'Couldn\' display the image.';

  @override
  String get couldnGetTheItemCount => 'Couldn\' get the item count';

  @override
  String get couldnLaunchTheFileManager => 'Couldn\' launch the file manager.';

  @override
  String get couldnLoadTheList => 'Couldn\' load the list.';

  @override
  String get couldnLoadTheNoteDetails => 'Couldn\' load the note details.';

  @override
  String get couldnLoadTheTagList => 'Couldn\' load the tag list.';

  @override
  String get couldnLoadTheTagSettings => 'Couldn\' load the tag settings.';

  @override
  String get couldnOpenItInAnExternalAppCheckForACompatibleAp =>
      'Couldn\' open it in an external app. Check for a compatible app.';

  @override
  String get couldnOpenTheMedia => 'Couldn\' open the media.';

  @override
  String get couldnOpenTheVaultCheckItsLocationAndPermissions =>
      'Couldn\' open the Vault. Check its location and permissions.';

  @override
  String get couldnOpenTheVaultMedia => 'Couldn\' open the Vault media.';

  @override
  String get couldNotDeleteTheVaultSInAppDataPleaseTryAgain =>
      'Could not delete the Vault\'s in-app data. Please try again.';

  @override
  String get couldNotFindTheItemAtThatPosition =>
      'Could not find the item at that position.';

  @override
  String get couldNotLoadListSettings => 'Could not load list settings.';

  @override
  String get couldNotLoadNoteSettings => 'Could not load note settings.';

  @override
  String get couldNotLoadTagSettings => 'Could not load tag settings.';

  @override
  String get couldNotOpenTheDestinationForTheSettingsFile =>
      'Could not open the destination for the settings file.';

  @override
  String get couldNotOpenTheSettingsFile => 'Could not open the settings file.';

  @override
  String get couldNotReadOrWriteTheSettingsFile =>
      'Could not read or write the settings file.';

  @override
  String get couldNotResetSettings => 'Could not reset settings.';

  @override
  String get couldNotSaveFrontmatterSettings =>
      'Could not save Frontmatter settings.';

  @override
  String get couldNotSaveNoteStructureSettings =>
      'Could not save note structure settings.';

  @override
  String get couldNotSavePagingSettings => 'Could not save paging settings.';

  @override
  String get couldNotSaveSettingsToJSON => 'Could not save settings to JSON.';

  @override
  String get couldNotSaveTheAppearanceSettings =>
      'Could not save the appearance settings.';

  @override
  String get couldNotSaveToJSONBecauseTheSettingsCouldNotBeRe =>
      'Could not save to JSON because the settings could not be read.';

  @override
  String get couldnPlayThisVideo => 'Couldn\' play this video.';

  @override
  String get couldnSaveTheTagSettings => 'Couldn\' save the tag settings.';

  @override
  String get couldnSetAsWallpaper => 'Couldn\' set as wallpaper.';

  @override
  String get couldnShowTheFileInTheFileManager =>
      'Couldn\' show the file in the file manager.';

  @override
  String get couldnSwitchToTheDetailsView =>
      'Couldn\' switch to the details view.';

  @override
  String get couldnToggleFullscreen => 'Couldn\' toggle fullscreen.';

  @override
  String count(Object count) {
    return 'Count $count';
  }

  @override
  String get countingItems => 'Counting items';

  @override
  String get coverImageVideo => 'Cover image / video';

  @override
  String get created => 'Created';

  @override
  String get createdAt => 'Created at';

  @override
  String get dark => 'Dark';

  @override
  String pageSizeRange(
    Object minPageSize,
    Object maxPageSize,
    Object defaultPageSize,
  ) {
    return '$minPageSize–$maxPageSize (default $defaultPageSize)';
  }

  @override
  String get delete => 'Delete';

  @override
  String get deleteColorRule => 'Delete color rule';

  @override
  String get deleteKey => 'Delete key';

  @override
  String get deleteTheVaultSelectionIndexAndCache =>
      'Delete the Vault selection, index, and cache.';

  @override
  String get descending => 'Descending';

  @override
  String description(Object label) {
    return '$label description';
  }

  @override
  String get detailsOfUncheckedItems => 'Details of unchecked items';

  @override
  String get displayLanguage => 'Display language';

  @override
  String get displayMode => 'Display mode';

  @override
  String get document => '# Document';

  @override
  String get documentHeadingInTheBody => '# Document heading in the body';

  @override
  String duplicateHiddenNoteBlock(Object entry) {
    return 'Duplicate hidden note block: $entry.';
  }

  @override
  String duplicateNoteBlock(Object entry) {
    return 'Duplicate note block: $entry.';
  }

  @override
  String get editCategory => 'Edit category';

  @override
  String get editColor => 'Edit color';

  @override
  String get editTagColor => 'Edit tag color';

  @override
  String get enterANameAndAPathInSourceArtOrSourceCountFormat =>
      'Enter a name and a path in source/art or source/count/* format.';

  @override
  String get enterATagPathAndAColorInRRGGBBFormat =>
      'Enter a tag path and a color in #RRGGBB format.';

  @override
  String enterAValueFrom1To(Object _maximumUnboundedGalleryJump) {
    return 'Enter a value from 1 to $_maximumUnboundedGalleryJump.';
  }

  @override
  String enterAValueFrom1To_6f32bc(Object maximum) {
    return 'Enter a value from 1 to $maximum.';
  }

  @override
  String get enterFullscreen => 'Enter fullscreen';

  @override
  String get examplesOfWhyItemsCouldNotBeChecked =>
      'Examples of why items could not be checked';

  @override
  String get exclude => 'Exclude (-)';

  @override
  String get exclude_a53aa1 => 'Exclude';

  @override
  String get exitFullscreen => 'Exit fullscreen';

  @override
  String get exportSettingsAsJSON => 'Export settings as JSON';

  @override
  String get failedToLoadTheRequestedPosition =>
      'Failed to load the requested position.';

  @override
  String get fictionalAuthorHttpsExampleInvalidAuthorsFiction =>
      '[Fictional Author](https://example.invalid/authors/fictional)';

  @override
  String get fictionalAuthorLink => 'Fictional author link';

  @override
  String get fictionalEmbeddedImage => 'Fictional embedded image';

  @override
  String get fictionalNoteExample => 'Fictional note example';

  @override
  String get fictionalPostText => 'Fictional post text';

  @override
  String get filesOrFoldersCannotBeReadTheNoteEncodingIsNotUT =>
      '• Files or folders cannot be read\n• The note encoding is not UTF-8\n• The YAML format cannot be parsed\n• The note size or tag count exceeds the limit';

  @override
  String get filterByTags => 'Filter by tags';

  @override
  String get filterCategories => 'Filter categories';

  @override
  String get forget => 'Forget';

  @override
  String get forgetThisVault => 'Forget this Vault';

  @override
  String get frontmatterDescription => 'Frontmatter description';

  @override
  String get frontmatterKey => 'Frontmatter key';

  @override
  String get frontmatterSettings => 'Frontmatter settings';

  @override
  String get frontmatterTags => 'Frontmatter tags';

  @override
  String get galleryTagSettingsAreNotInitialized =>
      'Gallery tag settings are not initialized.';

  @override
  String get galleryTargetTags => 'Gallery target tags';

  @override
  String get githubRepository => 'GitHub repository';

  @override
  String get groupByNote => 'Group by note';

  @override
  String get hasMemo => 'Has memo';

  @override
  String get hasRelated => 'Has related';

  @override
  String get hasVideo => 'Has video';

  @override
  String get headingName => 'Heading name';

  @override
  String headingsRecognizedAs(Object label) {
    return 'Headings recognized as $label';
  }

  @override
  String get helpAndHowToUse => 'Help and how to use';

  @override
  String get hiddenTags => 'Hidden tags';

  @override
  String get howNoteLinksAreResolved => 'How note links are resolved';

  @override
  String get httpsExampleInvalidPostsAoikasumi0001NISketchedA =>
      'https://example.invalid/posts/aoikasumi-0001\nn> I sketched a fictional blue bird by the window after the rain.';

  @override
  String get ifOffQuotedLinesAreExcludedFromThePostText =>
      'If off, quoted lines are excluded from the post text.';

  @override
  String get importExportAndReset => 'Import, export, and reset';

  @override
  String get importSettingsFromJSON => 'Import settings from JSON';

  @override
  String inAppIndexCacheAndSelectionDataWillBeRemovedFile(
    Object param1,
    Object vaultAccessText,
  ) {
    return '$param1\' in-app index, cache, and selection data$vaultAccessText will be removed. Files inside the Vault and app settings such as appearance and tags will not be changed.';
  }

  @override
  String get includeInAny => 'Include in any';

  @override
  String get includeQuotesInPostText => 'Include quotes (>) in post text';

  @override
  String get information => 'Information';

  @override
  String get informationAndLicenses => 'Information and licenses';

  @override
  String invalidBooleanSetting(Object key) {
    return 'Invalid boolean setting: $key.';
  }

  @override
  String invalidFrontmatterKeys(Object key) {
    return 'Invalid frontmatter keys: $key.';
  }

  @override
  String get invalidFrontmatterKeySettings =>
      'Invalid frontmatter key settings.';

  @override
  String get invalidGalleryItemNumberFlag =>
      'Invalid gallery item number flag.';

  @override
  String get invalidGalleryNoteStructureSettings =>
      'Invalid gallery note structure settings.';

  @override
  String get invalidGalleryPaginationItemCountFlag =>
      'Invalid gallery pagination item count flag.';

  @override
  String get invalidGalleryPaginationPageSize =>
      'Invalid gallery pagination page size.';

  @override
  String get invalidGalleryPaginationSettings =>
      'Invalid gallery pagination settings.';

  @override
  String get invalidGalleryTagCategorySettings =>
      'Invalid gallery tag category settings.';

  @override
  String get invalidGalleryTagColorRule => 'Invalid gallery tag color rule.';

  @override
  String get invalidGalleryTagSettings => 'Invalid gallery tag settings.';

  @override
  String get invalidHiddenNoteBlockEntry => 'Invalid hidden note block entry.';

  @override
  String get invalidHiddenNoteBlocks => 'Invalid hidden note blocks.';

  @override
  String get invalidLinkResolutionSetting => 'Invalid link resolution setting.';

  @override
  String get invalidMissingMediaIconFlag => 'Invalid missing media icon flag.';

  @override
  String get invalidNoteBlockOrder => 'Invalid note block order.';

  @override
  String get invalidNoteBlockOrderEntry => 'Invalid note block order entry.';

  @override
  String invalidNoteStructureHeadingList(Object key) {
    return 'Invalid note structure heading list: $key.';
  }

  @override
  String get invalidOtherCategoryName => 'Invalid other category name.';

  @override
  String get invalidOtherCategorySettings => 'Invalid other category settings.';

  @override
  String get invalidTagCategoryList => 'Invalid tag category list.';

  @override
  String get invalidTagCategoryRule => 'Invalid tag category rule.';

  @override
  String item(Object itemNumber) {
    return 'Item $itemNumber';
  }

  @override
  String items(Object count) {
    return '$count items';
  }

  @override
  String items_89e724(Object filterState, Object count) {
    return '$filterState, $count items';
  }

  @override
  String items_98c0e6(Object totalCount) {
    return '$totalCount items';
  }

  @override
  String items1To(Object maximum) {
    return 'Items 1 to $maximum';
  }

  @override
  String get itemsThatCouldNotBeChecked => 'Items that could not be checked';

  @override
  String itemsTheirContentsCouldNotBeCheckedSoWeCannotDet(Object warnings) {
    return '$warnings items. Their contents could not be checked, so we cannot determine whether they belong in the gallery.';
  }

  @override
  String get jump => 'Jump';

  @override
  String jumpedToItem(Object param1) {
    return 'Jumped to item $param1.';
  }

  @override
  String jumpingToItem(Object param1) {
    return 'Jumping to item $param1.';
  }

  @override
  String get jumpToPosition => 'Jump to position';

  @override
  String get jumpToPosition_a998c4 => 'Jump to position';

  @override
  String get kaedeGalleryIcon => 'Kaede Gallery icon';

  @override
  String get language => 'Language';

  @override
  String get light => 'Light';

  @override
  String get linkToAFictionalNote => 'Link to a fictional note';

  @override
  String get listDisplay => 'List display';

  @override
  String loadingThePageContainingItem(Object param1) {
    return 'Loading the page containing item $param1.';
  }

  @override
  String get loadSettingsThatWereExportedEarlier =>
      'Load settings that were exported earlier.';

  @override
  String get loopCurrentVideo => 'Loop current video';

  @override
  String get markdownLinksImageLinksAndObsidianWikilinksEmbed =>
      'Markdown links / image links and Obsidian wikilinks / embeds inside related headings can be tapped to open details when they point to notes in the Vault.';

  @override
  String get markdownWikilinkNoteResolution =>
      'Markdown / Wikilink note resolution';

  @override
  String get media => 'Media';

  @override
  String get memo => 'Memo';

  @override
  String message0OfItems(Object totalCount) {
    return '0 of $totalCount items';
  }

  @override
  String moveDown(Object label) {
    return 'Move $label down';
  }

  @override
  String get moveDown_5f88c6 => 'Move down';

  @override
  String moveUp(Object label) {
    return 'Move $label up';
  }

  @override
  String get moveUp_467fe2 => 'Move up';

  @override
  String get multipleImages => 'Multiple images';

  @override
  String get mute => 'Mute';

  @override
  String get noGalleryTargetTagsAreConfiguredSoEvenNotesThatW =>
      'No gallery target tags are configured, so even notes that were read successfully are excluded.';

  @override
  String get noHeadingsAreAssigned => 'No headings are assigned.';

  @override
  String get noMatchingMedia => 'No matching media.';

  @override
  String get noMatchingNotes => 'No matching notes.';

  @override
  String get noMatchingOptions => 'No matching options';

  @override
  String get noMatchingTags => 'No matching tags.';

  @override
  String get none => 'None';

  @override
  String get noTargetTagsAreSet => 'No target tags are set.';

  @override
  String get noteDetailBlockOrder => 'Note detail block order';

  @override
  String get noteDetailsActions => 'Note details actions';

  @override
  String get noteNameTagTagTag => 'Note name / #tag / -#tag / &#tag';

  @override
  String get notes => 'Notes';

  @override
  String get notes_ab7203 => 'Notes';

  @override
  String get noteSearchSupportsNamesAndPathsPlusTagTagAndTagT =>
      'Note search supports names and paths, plus #tag, -#tag, and &#tag. The tag search below only narrows the displayed filter candidates.';

  @override
  String get notesInPlainTextQuotesAndCode =>
      'Notes in plain text, quotes, and code';

  @override
  String get notesNSoftenTheWindowReflectionsALittleKeepTheBl =>
      '## Notes\nn- Soften the window reflections a little\n - Keep the blue saturation restrained\n Next, try evening light';

  @override
  String get noteStructure => 'Note structure';

  @override
  String get noteStructureAndDisplay => 'Note structure and display';

  @override
  String get notesWithThisTagOrAnyChildTagAreIncludedMultiple =>
      'Notes with this tag or any child tag are included. Multiple entries use OR. If empty, no notes are included.';

  @override
  String get notSelected => 'Not selected';

  @override
  String get notSet => 'Not set';

  @override
  String get noVaultIsSelected => 'No vault is selected.';

  @override
  String get observationsAfterTheRain => '# Observations After the Rain';

  @override
  String ofItems(Object firstItem, Object lastItem, Object totalCount) {
    return '$firstItem–$lastItem of $totalCount items';
  }

  @override
  String get openAuthorProfile => 'Open author profile';

  @override
  String get openLink => 'Open link';

  @override
  String get openMedia => 'Open media';

  @override
  String get openNote => 'Open note';

  @override
  String get openNoteInObsidian => 'Open note in Obsidian';

  @override
  String get openOriginalPage => 'Open original page';

  @override
  String get openSourceLicenses => 'Open-source licenses';

  @override
  String get other => 'Other';

  @override
  String get otherCategory => 'Other category';

  @override
  String get otherCategoryName => 'Other category name';

  @override
  String get pageSize => 'Page size';

  @override
  String get pagingAndListDisplay => 'Paging and list display';

  @override
  String get pause => 'Pause';

  @override
  String get people => 'People';

  @override
  String get play => 'Play';

  @override
  String get postText => 'Post text';

  @override
  String get postTextEnd => 'Post text end';

  @override
  String get postURL => 'Post URL';

  @override
  String get preparingToJump => 'Preparing to jump.';

  @override
  String get productionNotes => 'Production notes';

  @override
  String get published => 'Published';

  @override
  String get publishedAt => 'Published at';

  @override
  String get pureBlack => 'Pure black';

  @override
  String get readsContentUnderTheRegisteredHeadingsAsRelatedI =>
      'Reads content under the registered headings as related items. Note links inside the Vault can be tapped to open them.';

  @override
  String get readsTextQuotesAndCodeUnderTheRegisteredHeadings =>
      'Reads text, quotes, and code under the registered headings as notes.';

  @override
  String get related => 'Related';

  @override
  String get relatedNColorStudyFictionalColorStudyMd =>
      '## Related\nn- [Color Study](./fictional-color-study.md)';

  @override
  String get relatedNotes => 'Related notes';

  @override
  String get relative => 'Relative';

  @override
  String get reloadVaultChanges => 'Reload Vault changes';

  @override
  String get requireAll => 'Require all';

  @override
  String get rescan => 'Rescan';

  @override
  String get resetSettings => 'Reset settings';

  @override
  String get resetToDefaults => 'Reset to defaults';

  @override
  String get restoreAllAppearanceTagAndNoteStructureSettingsT =>
      'Restore all appearance, tag, and note structure settings to their defaults. Notes inside the Vault will not be changed.';

  @override
  String get restoreAppearanceAndNoteSettingsToTheirDefaults =>
      'Restore appearance and note settings to their defaults.';

  @override
  String get restoreDefaults => 'Restore defaults';

  @override
  String get restoreDefaults_c4cee4 => 'Restore defaults';

  @override
  String get retrievesTheAuthorNameAndURLFromTheAuthorLinkAtT =>
      'Retrieves the author name and URL from the author link at the start of the body. Links without @ are also supported.';

  @override
  String get save => 'Save';

  @override
  String get saveAppearanceAndTagSettingsToOneJSONFile =>
      'Save appearance and tag settings to one JSON file.';

  @override
  String get screenTheme => 'Screen theme';

  @override
  String get searchAndFiltering => 'Search and filtering';

  @override
  String get searchNotes => 'Search notes';

  @override
  String get searchTags => 'Search tags';

  @override
  String get selectedVault => 'Selected Vault';

  @override
  String get selectThisVault => 'Select this vault';

  @override
  String get setAsWallpaper => 'Set as wallpaper.';

  @override
  String get setImageAsWallpaper => 'Set image as wallpaper';

  @override
  String get settings => 'Settings';

  @override
  String settings_5e5451(Object label) {
    return '$label settings';
  }

  @override
  String get settingsHaveNotBeenInitialized =>
      'Settings have not been initialized.';

  @override
  String get settingsWereImported => 'Settings were imported.';

  @override
  String get settingsWereSavedAsJSON => 'Settings were saved as JSON.';

  @override
  String get shortest => 'Shortest';

  @override
  String get shortestChoosesTheClosestMatchingNoteWithTheSame =>
      'Shortest chooses the closest matching note with the same name inside the Vault. Relative uses the current note location as the base, and Absolute uses the Vault root. Links that resolve outside the Vault are ignored.';

  @override
  String show(Object label) {
    return 'Show $label';
  }

  @override
  String get showAllMedia => 'Show all media';

  @override
  String get showAnIconForHiddenMedia => 'Show an icon for hidden media';

  @override
  String get showEachTileSPositionInTheList =>
      'Show each tile\'s position in the list';

  @override
  String get showFileInFileManager => 'Show file in file manager';

  @override
  String get showLoadedItemCount => 'Show loaded item count';

  @override
  String get showOtherCategory => 'Show other category';

  @override
  String get showsTheNumberOfItemsCurrentlyDisplayedInTheTopB =>
      'Shows the number of items currently displayed in the top bar.';

  @override
  String get showThemAsSeparateCategoriesForEachParentTag =>
      'Show them as separate categories for each parent tag.';

  @override
  String get sortDirection => 'Sort direction';

  @override
  String get sortField => 'Sort field';

  @override
  String get specificItemNamesAndNoteContentsAreNotShownOnThi =>
      'Specific item names and note contents are not shown on this screen. Check access permissions and file status, then rescan.';

  @override
  String splitDeeperLevels(Object path) {
    return '$path · split deeper levels';
  }

  @override
  String get startAtItem => 'Start at item';

  @override
  String get stopsExtractingPostTextWhenThisHeadingIsReachedH =>
      'Stops extracting post text when this heading is reached. Heading depth does not matter, and you can register multiple heading names.';

  @override
  String get switchVault => 'Switch Vault';

  @override
  String get system => 'System';

  @override
  String get systemColorMaterialYou => 'System color (Material You)';

  @override
  String get tagColors => 'Tag colors';

  @override
  String get tagPath => 'Tag path';

  @override
  String get tags => 'Tags';

  @override
  String get tagSettings => 'Tag settings';

  @override
  String tagsIn(Object title) {
    return 'Tags in $title';
  }

  @override
  String get tagsIncludedInFilters => 'Tags included in filters';

  @override
  String get tagsTitleAndMoreFixed => 'Tags, title, and more (fixed)';

  @override
  String get theFictionalNoteExampleWasCopied =>
      'The fictional note example was copied.';

  @override
  String get theFrontmatterTagsAndBodyBlockOrderInThisExample =>
      'The Frontmatter tags and body block order in this example reflect the current settings. Frontmatter stays fixed, and the body follows the Markdown order below. It is not saved to the Vault automatically.';

  @override
  String get theItemCountIsStillLoadingButYouCanJumpToAPositi =>
      'The item count is still loading, but you can jump to a position';

  @override
  String get theItemCountIsUnavailableButYouCanJumpToAPositio =>
      'The item count is unavailable, but you can jump to a position';

  @override
  String get theme => 'Theme';

  @override
  String get theMediaFileWasNotFoundPleaseRescan =>
      'The media file was not found. Please rescan.';

  @override
  String get theNoteWasNotFoundPleaseRescan =>
      'The note was not found. Please rescan.';

  @override
  String get thereAreNoCategories => 'There are no categories';

  @override
  String get thereAreNoFictionalTagsToDisplay =>
      'There are no fictional tags to display.';

  @override
  String get thereAreNoFilterTags => 'There are no filter tags.';

  @override
  String get thereAreNoHiddenTags => 'There are no hidden tags.';

  @override
  String get thereAreNoItemsToShow => 'There are no items to show';

  @override
  String get thereIsNoMediaToShowInThisNote =>
      'There is no media to show in this note.';

  @override
  String get theseAreTheNoteSImagesAndVideosTheyAreNotDuplica =>
      'These are the note\'s images and videos. They are not duplicated in the details section and are shown in the media area of the viewer.';

  @override
  String get theseItemsAreNotConfirmedToBeOutsideTheGalleryBe =>
      'These items are not confirmed to be outside the gallery. Because we could not inspect tags inside the notes, we could not determine whether they qualify, so they were not added to the list.';

  @override
  String get theSelectedAppCouldNotShowTheFile =>
      'The selected app could not show the file.';

  @override
  String get theSettingsJSONIsMissingRequiredFields =>
      'The settings JSON is missing required fields.';

  @override
  String get theSettingsJSONIsTooLargeOrIsNotARegularFile =>
      'The settings JSON is too large or is not a regular file.';

  @override
  String get theTotalIsStillLoadingEnterAPositiveInteger =>
      'The total is still loading. Enter a positive integer.';

  @override
  String get thisIsMetadataSuchAsTagsTitlesURLsDatesAndCovers =>
      'This is metadata such as tags, titles, URLs, dates, and covers. It is fixed at the top of the details section and cannot be reordered or hidden.';

  @override
  String get thisIsTheBodyPostTextContentBeforeTheHeadingSpec =>
      'This is the body post text. Content before the heading specified as the end of the post text is shown.';

  @override
  String thisScanCouldNotCheckItems(Object warnings) {
    return 'This scan could not check $warnings items.';
  }

  @override
  String get thisScanFoundNoUncheckedItems =>
      'This scan found no unchecked items.';

  @override
  String get thisSettingsJSONFormatIsNotSupported =>
      'This settings JSON format is not supported.';

  @override
  String get title => 'Title';

  @override
  String get treatsContentUnderTheseHeadingsAsNotesHeadingDep =>
      'Treats content under these headings as notes. Heading depth does not matter, and you can register multiple heading names.';

  @override
  String get treatsItemsUnderTheseHeadingsAsRelatedContentHea =>
      'Treats items under these headings as related content. Heading depth does not matter, and you can register multiple heading names.';

  @override
  String get turnLoopingOff => 'Turn looping off';

  @override
  String get unableToReadTheNotePleaseCheckAccessPermissions =>
      'Unable to read the note. Please check access permissions.';

  @override
  String get unknownLinkResolutionSetting => 'Unknown link resolution setting.';

  @override
  String unknownNoteBlock(Object entry) {
    return 'Unknown note block: $entry.';
  }

  @override
  String unknownVisibleNoteBlock(Object entry) {
    return 'Unknown visible note block: $entry.';
  }

  @override
  String get unmute => 'Unmute';

  @override
  String get updated => 'Updated';

  @override
  String get updatedAt => 'Updated at';

  @override
  String get useABlackBackgroundInTheDarkThemeTheSystemColorC =>
      'Use a black background in the dark theme. The system color can still be used as an accent.';

  @override
  String get useTheInfoIconsInItemOrderToCheckHowEachSectionI =>
      'Use the info icons in item order to check how each section is read. Settings that can be changed, such as headings and Frontmatter keys, are grouped under each section\'s settings button.';

  @override
  String get useTheSystemAccentColor => 'Use the system accent color';

  @override
  String get vault => 'Vault';

  @override
  String version(Object version) {
    return 'Version $version';
  }

  @override
  String get video => 'Video';

  @override
  String get videoPlaybackPosition => 'Video playback position';

  @override
  String get viewNoteFormatExample => 'View note format example';

  @override
  String get viewOrCopyAFictionalNoteExample =>
      'View or copy a fictional note example';

  @override
  String get youCanAlsoReviewADisplayExampleThatReflectsTheCu =>
      'You can also review a display example that reflects the current item order.';
}
