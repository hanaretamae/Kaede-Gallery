import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_ja.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ja'),
  ];

  /// No description provided for @languageEnglish.
  ///
  /// In en, this message translates to:
  /// **'English'**
  String get languageEnglish;

  /// No description provided for @languageJapanese.
  ///
  /// In en, this message translates to:
  /// **'Japanese'**
  String get languageJapanese;

  /// No description provided for @aboutThisApp.
  ///
  /// In en, this message translates to:
  /// **'About this app'**
  String get aboutThisApp;

  /// No description provided for @absolute.
  ///
  /// In en, this message translates to:
  /// **'Absolute'**
  String get absolute;

  /// No description provided for @aCategoryWithTheSamePathAlreadyExists.
  ///
  /// In en, this message translates to:
  /// **'A category with the same path already exists.'**
  String get aCategoryWithTheSamePathAlreadyExists;

  /// No description provided for @accessToTheSelectedFolderIsUnavailablePleaseChoo.
  ///
  /// In en, this message translates to:
  /// **'Access to the selected folder is unavailable. Please choose the Vault again.'**
  String get accessToTheSelectedFolderIsUnavailablePleaseChoo;

  /// {filterCount} active filters
  ///
  /// In en, this message translates to:
  /// **'{filterCount} active filters'**
  String activeFilters(Object filterCount);

  /// No description provided for @add.
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get add;

  /// No description provided for @addAFilterTag.
  ///
  /// In en, this message translates to:
  /// **'Add a filter tag'**
  String get addAFilterTag;

  /// Add a heading for {label}
  ///
  /// In en, this message translates to:
  /// **'Add a heading for {label}'**
  String addAHeadingFor(Object label);

  /// No description provided for @addAHiddenTag.
  ///
  /// In en, this message translates to:
  /// **'Add a hidden tag'**
  String get addAHiddenTag;

  /// Add a key for {title}
  ///
  /// In en, this message translates to:
  /// **'Add a key for {title}'**
  String addAKeyFor(Object title);

  /// No description provided for @addCategory.
  ///
  /// In en, this message translates to:
  /// **'Add category'**
  String get addCategory;

  /// No description provided for @addColorRule.
  ///
  /// In en, this message translates to:
  /// **'Add color rule'**
  String get addColorRule;

  /// No description provided for @addGalleryTargetTag.
  ///
  /// In en, this message translates to:
  /// **'Add gallery target tag'**
  String get addGalleryTargetTag;

  /// No description provided for @addHeading.
  ///
  /// In en, this message translates to:
  /// **'Add heading'**
  String get addHeading;

  /// No description provided for @addKey.
  ///
  /// In en, this message translates to:
  /// **'Add key'**
  String get addKey;

  /// No description provided for @addPath.
  ///
  /// In en, this message translates to:
  /// **'Add path'**
  String get addPath;

  /// No description provided for @addTag.
  ///
  /// In en, this message translates to:
  /// **'Add tag'**
  String get addTag;

  /// No description provided for @addTagColor.
  ///
  /// In en, this message translates to:
  /// **'Add tag color'**
  String get addTagColor;

  /// No description provided for @all.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get all;

  /// No description provided for @allAND.
  ///
  /// In en, this message translates to:
  /// **'All (AND)'**
  String get allAND;

  /// No description provided for @allSettingsWereRestoredToTheirDefaults.
  ///
  /// In en, this message translates to:
  /// **'All settings were restored to their defaults.'**
  String get allSettingsWereRestoredToTheirDefaults;

  /// No description provided for @allTags.
  ///
  /// In en, this message translates to:
  /// **'All tags (*)'**
  String get allTags;

  /// No description provided for @alwaysFixedAtTheTopCannotBeReordered.
  ///
  /// In en, this message translates to:
  /// **'Always fixed at the top (cannot be reordered)'**
  String get alwaysFixedAtTheTopCannotBeReordered;

  /// Among notes that were read successfully, those without the {param1} tag are excluded.
  ///
  /// In en, this message translates to:
  /// **'Among notes that were read successfully, those without the {param1} tag are excluded.'**
  String amongNotesThatWereReadSuccessfullyThoseWithoutTh(Object param1);

  /// No description provided for @andReadPermission.
  ///
  /// In en, this message translates to:
  /// **' and read permission'**
  String get andReadPermission;

  /// No description provided for @anOfflineGalleryForBrowsingNotesAndMediaInsideYo.
  ///
  /// In en, this message translates to:
  /// **'An offline gallery for browsing notes and media inside your Obsidian Vault.'**
  String get anOfflineGalleryForBrowsingNotesAndMediaInsideYo;

  /// No description provided for @any.
  ///
  /// In en, this message translates to:
  /// **'Any (+)'**
  String get any;

  /// No description provided for @app.
  ///
  /// In en, this message translates to:
  /// **'App'**
  String get app;

  /// No description provided for @appearance.
  ///
  /// In en, this message translates to:
  /// **'Appearance'**
  String get appearance;

  /// No description provided for @ascending.
  ///
  /// In en, this message translates to:
  /// **'Ascending'**
  String get ascending;

  /// No description provided for @atLeastOneFrontmatterTagKeyIsRequired.
  ///
  /// In en, this message translates to:
  /// **'At least one frontmatter tag key is required.'**
  String get atLeastOneFrontmatterTagKeyIsRequired;

  /// No description provided for @atLeastOneKeyIsRequiredToReadTags.
  ///
  /// In en, this message translates to:
  /// **'At least one key is required to read tags.'**
  String get atLeastOneKeyIsRequiredToReadTags;

  /// No description provided for @author.
  ///
  /// In en, this message translates to:
  /// **'Author'**
  String get author;

  /// No description provided for @authorExtraction.
  ///
  /// In en, this message translates to:
  /// **'Author extraction'**
  String get authorExtraction;

  /// No description provided for @automaticallyRetrievesTheAuthorFromTheAuthorLink.
  ///
  /// In en, this message translates to:
  /// **'Automatically retrieves the author from the author link at the start of the note body. There is no separate author key.'**
  String get automaticallyRetrievesTheAuthorFromTheAuthorLink;

  /// No description provided for @automaticallySplitTagsAfterTheThirdLevel.
  ///
  /// In en, this message translates to:
  /// **'Automatically split tags after the third level'**
  String get automaticallySplitTagsAfterTheThirdLevel;

  /// No description provided for @bodyOrderInTheFictionalMarkdownBelow.
  ///
  /// In en, this message translates to:
  /// **'Body order in the fictional Markdown below'**
  String get bodyOrderInTheFictionalMarkdownBelow;

  /// No description provided for @browseMediaFromYourObsidianVaultOffline.
  ///
  /// In en, this message translates to:
  /// **'Browse media from your Obsidian Vault offline.'**
  String get browseMediaFromYourObsidianVaultOffline;

  /// No description provided for @byDefaultTheIconIsHidden.
  ///
  /// In en, this message translates to:
  /// **'By default, the icon is hidden.'**
  String get byDefaultTheIconIsHidden;

  /// No description provided for @canAccessTheMediaFile.
  ///
  /// In en, this message translates to:
  /// **'Can\' access the media file.'**
  String get canAccessTheMediaFile;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @cannotSaveInsideTheVaultChooseADifferentDestinat.
  ///
  /// In en, this message translates to:
  /// **'Cannot save inside the Vault. Choose a different destination.'**
  String get cannotSaveInsideTheVaultChooseADifferentDestinat;

  /// No description provided for @categoryName.
  ///
  /// In en, this message translates to:
  /// **'Category name'**
  String get categoryName;

  /// No description provided for @chooseAnAppToOpenTheImageOrVideo.
  ///
  /// In en, this message translates to:
  /// **'Choose an app to open the image or video'**
  String get chooseAnAppToOpenTheImageOrVideo;

  /// No description provided for @chooseAnotherVault.
  ///
  /// In en, this message translates to:
  /// **'Choose another Vault'**
  String get chooseAnotherVault;

  /// No description provided for @chooseVault.
  ///
  /// In en, this message translates to:
  /// **'Choose Vault'**
  String get chooseVault;

  /// No description provided for @clearAllSearchAndFilters.
  ///
  /// In en, this message translates to:
  /// **'Clear all search and filters'**
  String get clearAllSearchAndFilters;

  /// No description provided for @close.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get close;

  /// Color #{param1}
  ///
  /// In en, this message translates to:
  /// **'Color #{param1}'**
  String color(Object param1);

  /// No description provided for @color_eac664.
  ///
  /// In en, this message translates to:
  /// **'Color'**
  String get color_eac664;

  /// No description provided for @configureGalleryTargetTagsFiltersAndDisplayColor.
  ///
  /// In en, this message translates to:
  /// **'Configure gallery target tags, filters, and display colors.'**
  String get configureGalleryTargetTagsFiltersAndDisplayColor;

  /// No description provided for @configureListLoadingSizeItemCountsAndTileDisplay.
  ///
  /// In en, this message translates to:
  /// **'Configure list loading size, item counts, and tile display.'**
  String get configureListLoadingSizeItemCountsAndTileDisplay;

  /// No description provided for @configureNoteItemOrderHeadingParsingAndListDispl.
  ///
  /// In en, this message translates to:
  /// **'Configure note item order, heading parsing, and list display.'**
  String get configureNoteItemOrderHeadingParsingAndListDispl;

  /// No description provided for @content.
  ///
  /// In en, this message translates to:
  /// **'Content'**
  String get content;

  /// No description provided for @contentAfterTheHeadingsRegisteredHereIsNotTreate.
  ///
  /// In en, this message translates to:
  /// **'Content after the headings registered here is not treated as post text. This item remains in the order but cannot be hidden.'**
  String get contentAfterTheHeadingsRegisteredHereIsNotTreate;

  /// No description provided for @copiedTheURL.
  ///
  /// In en, this message translates to:
  /// **'Copied the URL.'**
  String get copiedTheURL;

  /// No description provided for @copyMarkdown.
  ///
  /// In en, this message translates to:
  /// **'Copy Markdown'**
  String get copyMarkdown;

  /// No description provided for @copyPageURL.
  ///
  /// In en, this message translates to:
  /// **'Copy page URL'**
  String get copyPageURL;

  /// No description provided for @couldnDisplayTheImage.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' display the image.'**
  String get couldnDisplayTheImage;

  /// No description provided for @couldnGetTheItemCount.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' get the item count'**
  String get couldnGetTheItemCount;

  /// No description provided for @couldnLaunchTheFileManager.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' launch the file manager.'**
  String get couldnLaunchTheFileManager;

  /// No description provided for @couldnLoadTheList.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' load the list.'**
  String get couldnLoadTheList;

  /// No description provided for @couldnLoadTheNoteDetails.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' load the note details.'**
  String get couldnLoadTheNoteDetails;

  /// No description provided for @couldnLoadTheTagList.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' load the tag list.'**
  String get couldnLoadTheTagList;

  /// No description provided for @couldnLoadTheTagSettings.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' load the tag settings.'**
  String get couldnLoadTheTagSettings;

  /// No description provided for @couldnOpenItInAnExternalAppCheckForACompatibleAp.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' open it in an external app. Check for a compatible app.'**
  String get couldnOpenItInAnExternalAppCheckForACompatibleAp;

  /// No description provided for @couldnOpenTheMedia.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' open the media.'**
  String get couldnOpenTheMedia;

  /// No description provided for @couldnOpenTheVaultCheckItsLocationAndPermissions.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' open the Vault. Check its location and permissions.'**
  String get couldnOpenTheVaultCheckItsLocationAndPermissions;

  /// No description provided for @couldnOpenTheVaultMedia.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' open the Vault media.'**
  String get couldnOpenTheVaultMedia;

  /// No description provided for @couldNotDeleteTheVaultSInAppDataPleaseTryAgain.
  ///
  /// In en, this message translates to:
  /// **'Could not delete the Vault\'s in-app data. Please try again.'**
  String get couldNotDeleteTheVaultSInAppDataPleaseTryAgain;

  /// No description provided for @couldNotFindTheItemAtThatPosition.
  ///
  /// In en, this message translates to:
  /// **'Could not find the item at that position.'**
  String get couldNotFindTheItemAtThatPosition;

  /// No description provided for @couldNotLoadListSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not load list settings.'**
  String get couldNotLoadListSettings;

  /// No description provided for @couldNotLoadNoteSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not load note settings.'**
  String get couldNotLoadNoteSettings;

  /// No description provided for @couldNotLoadTagSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not load tag settings.'**
  String get couldNotLoadTagSettings;

  /// No description provided for @couldNotOpenTheDestinationForTheSettingsFile.
  ///
  /// In en, this message translates to:
  /// **'Could not open the destination for the settings file.'**
  String get couldNotOpenTheDestinationForTheSettingsFile;

  /// No description provided for @couldNotOpenTheSettingsFile.
  ///
  /// In en, this message translates to:
  /// **'Could not open the settings file.'**
  String get couldNotOpenTheSettingsFile;

  /// No description provided for @couldNotReadOrWriteTheSettingsFile.
  ///
  /// In en, this message translates to:
  /// **'Could not read or write the settings file.'**
  String get couldNotReadOrWriteTheSettingsFile;

  /// No description provided for @couldNotResetSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not reset settings.'**
  String get couldNotResetSettings;

  /// No description provided for @couldNotSaveFrontmatterSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not save Frontmatter settings.'**
  String get couldNotSaveFrontmatterSettings;

  /// No description provided for @couldNotSaveNoteStructureSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not save note structure settings.'**
  String get couldNotSaveNoteStructureSettings;

  /// No description provided for @couldNotSavePagingSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not save paging settings.'**
  String get couldNotSavePagingSettings;

  /// No description provided for @couldNotSaveSettingsToJSON.
  ///
  /// In en, this message translates to:
  /// **'Could not save settings to JSON.'**
  String get couldNotSaveSettingsToJSON;

  /// No description provided for @couldNotSaveTheAppearanceSettings.
  ///
  /// In en, this message translates to:
  /// **'Could not save the appearance settings.'**
  String get couldNotSaveTheAppearanceSettings;

  /// No description provided for @couldNotSaveToJSONBecauseTheSettingsCouldNotBeRe.
  ///
  /// In en, this message translates to:
  /// **'Could not save to JSON because the settings could not be read.'**
  String get couldNotSaveToJSONBecauseTheSettingsCouldNotBeRe;

  /// No description provided for @couldnPlayThisVideo.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' play this video.'**
  String get couldnPlayThisVideo;

  /// No description provided for @couldnSaveTheTagSettings.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' save the tag settings.'**
  String get couldnSaveTheTagSettings;

  /// No description provided for @couldnSetAsWallpaper.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' set as wallpaper.'**
  String get couldnSetAsWallpaper;

  /// No description provided for @couldnShowTheFileInTheFileManager.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' show the file in the file manager.'**
  String get couldnShowTheFileInTheFileManager;

  /// No description provided for @couldnSwitchToTheDetailsView.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' switch to the details view.'**
  String get couldnSwitchToTheDetailsView;

  /// No description provided for @couldnToggleFullscreen.
  ///
  /// In en, this message translates to:
  /// **'Couldn\' toggle fullscreen.'**
  String get couldnToggleFullscreen;

  /// Count {count}
  ///
  /// In en, this message translates to:
  /// **'Count {count}'**
  String count(Object count);

  /// No description provided for @countingItems.
  ///
  /// In en, this message translates to:
  /// **'Counting items'**
  String get countingItems;

  /// No description provided for @coverImageVideo.
  ///
  /// In en, this message translates to:
  /// **'Cover image / video'**
  String get coverImageVideo;

  /// No description provided for @created.
  ///
  /// In en, this message translates to:
  /// **'Created'**
  String get created;

  /// No description provided for @createdAt.
  ///
  /// In en, this message translates to:
  /// **'Created at'**
  String get createdAt;

  /// No description provided for @dark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get dark;

  /// {minPageSize}–{maxPageSize} (default {defaultPageSize})
  ///
  /// In en, this message translates to:
  /// **'{minPageSize}–{maxPageSize} (default {defaultPageSize})'**
  String pageSizeRange(
    Object minPageSize,
    Object maxPageSize,
    Object defaultPageSize,
  );

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @deleteColorRule.
  ///
  /// In en, this message translates to:
  /// **'Delete color rule'**
  String get deleteColorRule;

  /// No description provided for @deleteKey.
  ///
  /// In en, this message translates to:
  /// **'Delete key'**
  String get deleteKey;

  /// No description provided for @deleteTheVaultSelectionIndexAndCache.
  ///
  /// In en, this message translates to:
  /// **'Delete the Vault selection, index, and cache.'**
  String get deleteTheVaultSelectionIndexAndCache;

  /// No description provided for @descending.
  ///
  /// In en, this message translates to:
  /// **'Descending'**
  String get descending;

  /// {label} description
  ///
  /// In en, this message translates to:
  /// **'{label} description'**
  String description(Object label);

  /// No description provided for @detailsOfUncheckedItems.
  ///
  /// In en, this message translates to:
  /// **'Details of unchecked items'**
  String get detailsOfUncheckedItems;

  /// No description provided for @displayLanguage.
  ///
  /// In en, this message translates to:
  /// **'Display language'**
  String get displayLanguage;

  /// No description provided for @displayMode.
  ///
  /// In en, this message translates to:
  /// **'Display mode'**
  String get displayMode;

  /// No description provided for @document.
  ///
  /// In en, this message translates to:
  /// **'# Document'**
  String get document;

  /// No description provided for @documentHeadingInTheBody.
  ///
  /// In en, this message translates to:
  /// **'# Document heading in the body'**
  String get documentHeadingInTheBody;

  /// Duplicate hidden note block: {entry}.
  ///
  /// In en, this message translates to:
  /// **'Duplicate hidden note block: {entry}.'**
  String duplicateHiddenNoteBlock(Object entry);

  /// Duplicate note block: {entry}.
  ///
  /// In en, this message translates to:
  /// **'Duplicate note block: {entry}.'**
  String duplicateNoteBlock(Object entry);

  /// No description provided for @editCategory.
  ///
  /// In en, this message translates to:
  /// **'Edit category'**
  String get editCategory;

  /// No description provided for @editColor.
  ///
  /// In en, this message translates to:
  /// **'Edit color'**
  String get editColor;

  /// No description provided for @editTagColor.
  ///
  /// In en, this message translates to:
  /// **'Edit tag color'**
  String get editTagColor;

  /// No description provided for @enterANameAndAPathInSourceArtOrSourceCountFormat.
  ///
  /// In en, this message translates to:
  /// **'Enter a name and a path in source/art or source/count/* format.'**
  String get enterANameAndAPathInSourceArtOrSourceCountFormat;

  /// No description provided for @enterATagPathAndAColorInRRGGBBFormat.
  ///
  /// In en, this message translates to:
  /// **'Enter a tag path and a color in #RRGGBB format.'**
  String get enterATagPathAndAColorInRRGGBBFormat;

  /// Enter a value from 1 to {_maximumUnboundedGalleryJump}.
  ///
  /// In en, this message translates to:
  /// **'Enter a value from 1 to {_maximumUnboundedGalleryJump}.'**
  String enterAValueFrom1To(Object _maximumUnboundedGalleryJump);

  /// Enter a value from 1 to {maximum}.
  ///
  /// In en, this message translates to:
  /// **'Enter a value from 1 to {maximum}.'**
  String enterAValueFrom1To_6f32bc(Object maximum);

  /// No description provided for @enterFullscreen.
  ///
  /// In en, this message translates to:
  /// **'Enter fullscreen'**
  String get enterFullscreen;

  /// No description provided for @examplesOfWhyItemsCouldNotBeChecked.
  ///
  /// In en, this message translates to:
  /// **'Examples of why items could not be checked'**
  String get examplesOfWhyItemsCouldNotBeChecked;

  /// No description provided for @exclude.
  ///
  /// In en, this message translates to:
  /// **'Exclude (-)'**
  String get exclude;

  /// No description provided for @exclude_a53aa1.
  ///
  /// In en, this message translates to:
  /// **'Exclude'**
  String get exclude_a53aa1;

  /// No description provided for @exitFullscreen.
  ///
  /// In en, this message translates to:
  /// **'Exit fullscreen'**
  String get exitFullscreen;

  /// No description provided for @exportSettingsAsJSON.
  ///
  /// In en, this message translates to:
  /// **'Export settings as JSON'**
  String get exportSettingsAsJSON;

  /// No description provided for @failedToLoadTheRequestedPosition.
  ///
  /// In en, this message translates to:
  /// **'Failed to load the requested position.'**
  String get failedToLoadTheRequestedPosition;

  /// No description provided for @fictionalAuthorHttpsExampleInvalidAuthorsFiction.
  ///
  /// In en, this message translates to:
  /// **'[Fictional Author](https://example.invalid/authors/fictional)'**
  String get fictionalAuthorHttpsExampleInvalidAuthorsFiction;

  /// No description provided for @fictionalAuthorLink.
  ///
  /// In en, this message translates to:
  /// **'Fictional author link'**
  String get fictionalAuthorLink;

  /// No description provided for @fictionalEmbeddedImage.
  ///
  /// In en, this message translates to:
  /// **'Fictional embedded image'**
  String get fictionalEmbeddedImage;

  /// No description provided for @fictionalNoteExample.
  ///
  /// In en, this message translates to:
  /// **'Fictional note example'**
  String get fictionalNoteExample;

  /// No description provided for @fictionalPostText.
  ///
  /// In en, this message translates to:
  /// **'Fictional post text'**
  String get fictionalPostText;

  /// No description provided for @filesOrFoldersCannotBeReadTheNoteEncodingIsNotUT.
  ///
  /// In en, this message translates to:
  /// **'• Files or folders cannot be read\n• The note encoding is not UTF-8\n• The YAML format cannot be parsed\n• The note size or tag count exceeds the limit'**
  String get filesOrFoldersCannotBeReadTheNoteEncodingIsNotUT;

  /// No description provided for @filterByTags.
  ///
  /// In en, this message translates to:
  /// **'Filter by tags'**
  String get filterByTags;

  /// No description provided for @filterCategories.
  ///
  /// In en, this message translates to:
  /// **'Filter categories'**
  String get filterCategories;

  /// No description provided for @forget.
  ///
  /// In en, this message translates to:
  /// **'Forget'**
  String get forget;

  /// No description provided for @forgetThisVault.
  ///
  /// In en, this message translates to:
  /// **'Forget this Vault'**
  String get forgetThisVault;

  /// No description provided for @frontmatterDescription.
  ///
  /// In en, this message translates to:
  /// **'Frontmatter description'**
  String get frontmatterDescription;

  /// No description provided for @frontmatterKey.
  ///
  /// In en, this message translates to:
  /// **'Frontmatter key'**
  String get frontmatterKey;

  /// No description provided for @frontmatterSettings.
  ///
  /// In en, this message translates to:
  /// **'Frontmatter settings'**
  String get frontmatterSettings;

  /// No description provided for @frontmatterTags.
  ///
  /// In en, this message translates to:
  /// **'Frontmatter tags'**
  String get frontmatterTags;

  /// No description provided for @galleryTagSettingsAreNotInitialized.
  ///
  /// In en, this message translates to:
  /// **'Gallery tag settings are not initialized.'**
  String get galleryTagSettingsAreNotInitialized;

  /// No description provided for @galleryTargetTags.
  ///
  /// In en, this message translates to:
  /// **'Gallery target tags'**
  String get galleryTargetTags;

  /// No description provided for @githubRepository.
  ///
  /// In en, this message translates to:
  /// **'GitHub repository'**
  String get githubRepository;

  /// No description provided for @groupByNote.
  ///
  /// In en, this message translates to:
  /// **'Group by note'**
  String get groupByNote;

  /// No description provided for @hasMemo.
  ///
  /// In en, this message translates to:
  /// **'Has memo'**
  String get hasMemo;

  /// No description provided for @hasRelated.
  ///
  /// In en, this message translates to:
  /// **'Has related'**
  String get hasRelated;

  /// No description provided for @hasVideo.
  ///
  /// In en, this message translates to:
  /// **'Has video'**
  String get hasVideo;

  /// No description provided for @headingName.
  ///
  /// In en, this message translates to:
  /// **'Heading name'**
  String get headingName;

  /// Headings recognized as {label}
  ///
  /// In en, this message translates to:
  /// **'Headings recognized as {label}'**
  String headingsRecognizedAs(Object label);

  /// No description provided for @helpAndHowToUse.
  ///
  /// In en, this message translates to:
  /// **'Help and how to use'**
  String get helpAndHowToUse;

  /// No description provided for @hiddenTags.
  ///
  /// In en, this message translates to:
  /// **'Hidden tags'**
  String get hiddenTags;

  /// No description provided for @howNoteLinksAreResolved.
  ///
  /// In en, this message translates to:
  /// **'How note links are resolved'**
  String get howNoteLinksAreResolved;

  /// No description provided for @httpsExampleInvalidPostsAoikasumi0001NISketchedA.
  ///
  /// In en, this message translates to:
  /// **'https://example.invalid/posts/aoikasumi-0001\nn> I sketched a fictional blue bird by the window after the rain.'**
  String get httpsExampleInvalidPostsAoikasumi0001NISketchedA;

  /// No description provided for @ifOffQuotedLinesAreExcludedFromThePostText.
  ///
  /// In en, this message translates to:
  /// **'If off, quoted lines are excluded from the post text.'**
  String get ifOffQuotedLinesAreExcludedFromThePostText;

  /// No description provided for @importExportAndReset.
  ///
  /// In en, this message translates to:
  /// **'Import, export, and reset'**
  String get importExportAndReset;

  /// No description provided for @importSettingsFromJSON.
  ///
  /// In en, this message translates to:
  /// **'Import settings from JSON'**
  String get importSettingsFromJSON;

  /// {param1}' in-app index, cache, and selection data{vaultAccessText} will be removed. Files inside the Vault and app settings such as appearance and tags will not be changed.
  ///
  /// In en, this message translates to:
  /// **'{param1}\' in-app index, cache, and selection data{vaultAccessText} will be removed. Files inside the Vault and app settings such as appearance and tags will not be changed.'**
  String inAppIndexCacheAndSelectionDataWillBeRemovedFile(
    Object param1,
    Object vaultAccessText,
  );

  /// No description provided for @includeInAny.
  ///
  /// In en, this message translates to:
  /// **'Include in any'**
  String get includeInAny;

  /// No description provided for @includeQuotesInPostText.
  ///
  /// In en, this message translates to:
  /// **'Include quotes (>) in post text'**
  String get includeQuotesInPostText;

  /// No description provided for @information.
  ///
  /// In en, this message translates to:
  /// **'Information'**
  String get information;

  /// No description provided for @informationAndLicenses.
  ///
  /// In en, this message translates to:
  /// **'Information and licenses'**
  String get informationAndLicenses;

  /// Invalid boolean setting: {key}.
  ///
  /// In en, this message translates to:
  /// **'Invalid boolean setting: {key}.'**
  String invalidBooleanSetting(Object key);

  /// Invalid frontmatter keys: {key}.
  ///
  /// In en, this message translates to:
  /// **'Invalid frontmatter keys: {key}.'**
  String invalidFrontmatterKeys(Object key);

  /// No description provided for @invalidFrontmatterKeySettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid frontmatter key settings.'**
  String get invalidFrontmatterKeySettings;

  /// No description provided for @invalidGalleryItemNumberFlag.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery item number flag.'**
  String get invalidGalleryItemNumberFlag;

  /// No description provided for @invalidGalleryNoteStructureSettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery note structure settings.'**
  String get invalidGalleryNoteStructureSettings;

  /// No description provided for @invalidGalleryPaginationItemCountFlag.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery pagination item count flag.'**
  String get invalidGalleryPaginationItemCountFlag;

  /// No description provided for @invalidGalleryPaginationPageSize.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery pagination page size.'**
  String get invalidGalleryPaginationPageSize;

  /// No description provided for @invalidGalleryPaginationSettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery pagination settings.'**
  String get invalidGalleryPaginationSettings;

  /// No description provided for @invalidGalleryTagCategorySettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery tag category settings.'**
  String get invalidGalleryTagCategorySettings;

  /// No description provided for @invalidGalleryTagColorRule.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery tag color rule.'**
  String get invalidGalleryTagColorRule;

  /// No description provided for @invalidGalleryTagSettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid gallery tag settings.'**
  String get invalidGalleryTagSettings;

  /// No description provided for @invalidHiddenNoteBlockEntry.
  ///
  /// In en, this message translates to:
  /// **'Invalid hidden note block entry.'**
  String get invalidHiddenNoteBlockEntry;

  /// No description provided for @invalidHiddenNoteBlocks.
  ///
  /// In en, this message translates to:
  /// **'Invalid hidden note blocks.'**
  String get invalidHiddenNoteBlocks;

  /// No description provided for @invalidLinkResolutionSetting.
  ///
  /// In en, this message translates to:
  /// **'Invalid link resolution setting.'**
  String get invalidLinkResolutionSetting;

  /// No description provided for @invalidMissingMediaIconFlag.
  ///
  /// In en, this message translates to:
  /// **'Invalid missing media icon flag.'**
  String get invalidMissingMediaIconFlag;

  /// No description provided for @invalidNoteBlockOrder.
  ///
  /// In en, this message translates to:
  /// **'Invalid note block order.'**
  String get invalidNoteBlockOrder;

  /// No description provided for @invalidNoteBlockOrderEntry.
  ///
  /// In en, this message translates to:
  /// **'Invalid note block order entry.'**
  String get invalidNoteBlockOrderEntry;

  /// Invalid note structure heading list: {key}.
  ///
  /// In en, this message translates to:
  /// **'Invalid note structure heading list: {key}.'**
  String invalidNoteStructureHeadingList(Object key);

  /// No description provided for @invalidOtherCategoryName.
  ///
  /// In en, this message translates to:
  /// **'Invalid other category name.'**
  String get invalidOtherCategoryName;

  /// No description provided for @invalidOtherCategorySettings.
  ///
  /// In en, this message translates to:
  /// **'Invalid other category settings.'**
  String get invalidOtherCategorySettings;

  /// No description provided for @invalidTagCategoryList.
  ///
  /// In en, this message translates to:
  /// **'Invalid tag category list.'**
  String get invalidTagCategoryList;

  /// No description provided for @invalidTagCategoryRule.
  ///
  /// In en, this message translates to:
  /// **'Invalid tag category rule.'**
  String get invalidTagCategoryRule;

  /// Item {itemNumber}
  ///
  /// In en, this message translates to:
  /// **'Item {itemNumber}'**
  String item(Object itemNumber);

  /// {count} items
  ///
  /// In en, this message translates to:
  /// **'{count} items'**
  String items(Object count);

  /// {filterState}, {count} items
  ///
  /// In en, this message translates to:
  /// **'{filterState}, {count} items'**
  String items_89e724(Object filterState, Object count);

  /// {totalCount} items
  ///
  /// In en, this message translates to:
  /// **'{totalCount} items'**
  String items_98c0e6(Object totalCount);

  /// Items 1 to {maximum}
  ///
  /// In en, this message translates to:
  /// **'Items 1 to {maximum}'**
  String items1To(Object maximum);

  /// No description provided for @itemsThatCouldNotBeChecked.
  ///
  /// In en, this message translates to:
  /// **'Items that could not be checked'**
  String get itemsThatCouldNotBeChecked;

  /// {warnings} items. Their contents could not be checked, so we cannot determine whether they belong in the gallery.
  ///
  /// In en, this message translates to:
  /// **'{warnings} items. Their contents could not be checked, so we cannot determine whether they belong in the gallery.'**
  String itemsTheirContentsCouldNotBeCheckedSoWeCannotDet(Object warnings);

  /// No description provided for @jump.
  ///
  /// In en, this message translates to:
  /// **'Jump'**
  String get jump;

  /// Jumped to item {param1}.
  ///
  /// In en, this message translates to:
  /// **'Jumped to item {param1}.'**
  String jumpedToItem(Object param1);

  /// Jumping to item {param1}.
  ///
  /// In en, this message translates to:
  /// **'Jumping to item {param1}.'**
  String jumpingToItem(Object param1);

  /// No description provided for @jumpToPosition.
  ///
  /// In en, this message translates to:
  /// **'Jump to position'**
  String get jumpToPosition;

  /// No description provided for @jumpToPosition_a998c4.
  ///
  /// In en, this message translates to:
  /// **'Jump to position'**
  String get jumpToPosition_a998c4;

  /// No description provided for @kaedeGalleryIcon.
  ///
  /// In en, this message translates to:
  /// **'Kaede Gallery icon'**
  String get kaedeGalleryIcon;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @light.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get light;

  /// No description provided for @linkToAFictionalNote.
  ///
  /// In en, this message translates to:
  /// **'Link to a fictional note'**
  String get linkToAFictionalNote;

  /// No description provided for @listDisplay.
  ///
  /// In en, this message translates to:
  /// **'List display'**
  String get listDisplay;

  /// Loading the page containing item {param1}.
  ///
  /// In en, this message translates to:
  /// **'Loading the page containing item {param1}.'**
  String loadingThePageContainingItem(Object param1);

  /// No description provided for @loadSettingsThatWereExportedEarlier.
  ///
  /// In en, this message translates to:
  /// **'Load settings that were exported earlier.'**
  String get loadSettingsThatWereExportedEarlier;

  /// No description provided for @loopCurrentVideo.
  ///
  /// In en, this message translates to:
  /// **'Loop current video'**
  String get loopCurrentVideo;

  /// No description provided for @markdownLinksImageLinksAndObsidianWikilinksEmbed.
  ///
  /// In en, this message translates to:
  /// **'Markdown links / image links and Obsidian wikilinks / embeds inside related headings can be tapped to open details when they point to notes in the Vault.'**
  String get markdownLinksImageLinksAndObsidianWikilinksEmbed;

  /// No description provided for @markdownWikilinkNoteResolution.
  ///
  /// In en, this message translates to:
  /// **'Markdown / Wikilink note resolution'**
  String get markdownWikilinkNoteResolution;

  /// No description provided for @media.
  ///
  /// In en, this message translates to:
  /// **'Media'**
  String get media;

  /// No description provided for @memo.
  ///
  /// In en, this message translates to:
  /// **'Memo'**
  String get memo;

  /// 0 of {totalCount} items
  ///
  /// In en, this message translates to:
  /// **'0 of {totalCount} items'**
  String message0OfItems(Object totalCount);

  /// Move {label} down
  ///
  /// In en, this message translates to:
  /// **'Move {label} down'**
  String moveDown(Object label);

  /// No description provided for @moveDown_5f88c6.
  ///
  /// In en, this message translates to:
  /// **'Move down'**
  String get moveDown_5f88c6;

  /// Move {label} up
  ///
  /// In en, this message translates to:
  /// **'Move {label} up'**
  String moveUp(Object label);

  /// No description provided for @moveUp_467fe2.
  ///
  /// In en, this message translates to:
  /// **'Move up'**
  String get moveUp_467fe2;

  /// No description provided for @multipleImages.
  ///
  /// In en, this message translates to:
  /// **'Multiple images'**
  String get multipleImages;

  /// No description provided for @mute.
  ///
  /// In en, this message translates to:
  /// **'Mute'**
  String get mute;

  /// No description provided for @noGalleryTargetTagsAreConfiguredSoEvenNotesThatW.
  ///
  /// In en, this message translates to:
  /// **'No gallery target tags are configured, so even notes that were read successfully are excluded.'**
  String get noGalleryTargetTagsAreConfiguredSoEvenNotesThatW;

  /// No description provided for @noHeadingsAreAssigned.
  ///
  /// In en, this message translates to:
  /// **'No headings are assigned.'**
  String get noHeadingsAreAssigned;

  /// No description provided for @noMatchingMedia.
  ///
  /// In en, this message translates to:
  /// **'No matching media.'**
  String get noMatchingMedia;

  /// No description provided for @noMatchingNotes.
  ///
  /// In en, this message translates to:
  /// **'No matching notes.'**
  String get noMatchingNotes;

  /// No description provided for @noMatchingOptions.
  ///
  /// In en, this message translates to:
  /// **'No matching options'**
  String get noMatchingOptions;

  /// No description provided for @noMatchingTags.
  ///
  /// In en, this message translates to:
  /// **'No matching tags.'**
  String get noMatchingTags;

  /// No description provided for @none.
  ///
  /// In en, this message translates to:
  /// **'None'**
  String get none;

  /// No description provided for @noTargetTagsAreSet.
  ///
  /// In en, this message translates to:
  /// **'No target tags are set.'**
  String get noTargetTagsAreSet;

  /// No description provided for @noteDetailBlockOrder.
  ///
  /// In en, this message translates to:
  /// **'Note detail block order'**
  String get noteDetailBlockOrder;

  /// No description provided for @noteDetailsActions.
  ///
  /// In en, this message translates to:
  /// **'Note details actions'**
  String get noteDetailsActions;

  /// No description provided for @noteNameTagTagTag.
  ///
  /// In en, this message translates to:
  /// **'Note name / #tag / -#tag / &#tag'**
  String get noteNameTagTagTag;

  /// No description provided for @notes.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get notes;

  /// No description provided for @notes_ab7203.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get notes_ab7203;

  /// No description provided for @noteSearchSupportsNamesAndPathsPlusTagTagAndTagT.
  ///
  /// In en, this message translates to:
  /// **'Note search supports names and paths, plus #tag, -#tag, and &#tag. The tag search below only narrows the displayed filter candidates.'**
  String get noteSearchSupportsNamesAndPathsPlusTagTagAndTagT;

  /// No description provided for @notesInPlainTextQuotesAndCode.
  ///
  /// In en, this message translates to:
  /// **'Notes in plain text, quotes, and code'**
  String get notesInPlainTextQuotesAndCode;

  /// No description provided for @notesNSoftenTheWindowReflectionsALittleKeepTheBl.
  ///
  /// In en, this message translates to:
  /// **'## Notes\nn- Soften the window reflections a little\n - Keep the blue saturation restrained\n Next, try evening light'**
  String get notesNSoftenTheWindowReflectionsALittleKeepTheBl;

  /// No description provided for @noteStructure.
  ///
  /// In en, this message translates to:
  /// **'Note structure'**
  String get noteStructure;

  /// No description provided for @noteStructureAndDisplay.
  ///
  /// In en, this message translates to:
  /// **'Note structure and display'**
  String get noteStructureAndDisplay;

  /// No description provided for @notesWithThisTagOrAnyChildTagAreIncludedMultiple.
  ///
  /// In en, this message translates to:
  /// **'Notes with this tag or any child tag are included. Multiple entries use OR. If empty, no notes are included.'**
  String get notesWithThisTagOrAnyChildTagAreIncludedMultiple;

  /// No description provided for @notSelected.
  ///
  /// In en, this message translates to:
  /// **'Not selected'**
  String get notSelected;

  /// No description provided for @notSet.
  ///
  /// In en, this message translates to:
  /// **'Not set'**
  String get notSet;

  /// No description provided for @noVaultIsSelected.
  ///
  /// In en, this message translates to:
  /// **'No vault is selected.'**
  String get noVaultIsSelected;

  /// No description provided for @observationsAfterTheRain.
  ///
  /// In en, this message translates to:
  /// **'# Observations After the Rain'**
  String get observationsAfterTheRain;

  /// {firstItem}–{lastItem} of {totalCount} items
  ///
  /// In en, this message translates to:
  /// **'{firstItem}–{lastItem} of {totalCount} items'**
  String ofItems(Object firstItem, Object lastItem, Object totalCount);

  /// No description provided for @openAuthorProfile.
  ///
  /// In en, this message translates to:
  /// **'Open author profile'**
  String get openAuthorProfile;

  /// No description provided for @openLink.
  ///
  /// In en, this message translates to:
  /// **'Open link'**
  String get openLink;

  /// No description provided for @openMedia.
  ///
  /// In en, this message translates to:
  /// **'Open media'**
  String get openMedia;

  /// No description provided for @openNote.
  ///
  /// In en, this message translates to:
  /// **'Open note'**
  String get openNote;

  /// No description provided for @openNoteInObsidian.
  ///
  /// In en, this message translates to:
  /// **'Open note in Obsidian'**
  String get openNoteInObsidian;

  /// No description provided for @openOriginalPage.
  ///
  /// In en, this message translates to:
  /// **'Open original page'**
  String get openOriginalPage;

  /// No description provided for @openSourceLicenses.
  ///
  /// In en, this message translates to:
  /// **'Open-source licenses'**
  String get openSourceLicenses;

  /// No description provided for @other.
  ///
  /// In en, this message translates to:
  /// **'Other'**
  String get other;

  /// No description provided for @otherCategory.
  ///
  /// In en, this message translates to:
  /// **'Other category'**
  String get otherCategory;

  /// No description provided for @otherCategoryName.
  ///
  /// In en, this message translates to:
  /// **'Other category name'**
  String get otherCategoryName;

  /// No description provided for @pageSize.
  ///
  /// In en, this message translates to:
  /// **'Page size'**
  String get pageSize;

  /// No description provided for @pagingAndListDisplay.
  ///
  /// In en, this message translates to:
  /// **'Paging and list display'**
  String get pagingAndListDisplay;

  /// No description provided for @pause.
  ///
  /// In en, this message translates to:
  /// **'Pause'**
  String get pause;

  /// No description provided for @people.
  ///
  /// In en, this message translates to:
  /// **'People'**
  String get people;

  /// No description provided for @play.
  ///
  /// In en, this message translates to:
  /// **'Play'**
  String get play;

  /// No description provided for @postText.
  ///
  /// In en, this message translates to:
  /// **'Post text'**
  String get postText;

  /// No description provided for @postTextEnd.
  ///
  /// In en, this message translates to:
  /// **'Post text end'**
  String get postTextEnd;

  /// No description provided for @postURL.
  ///
  /// In en, this message translates to:
  /// **'Post URL'**
  String get postURL;

  /// No description provided for @preparingToJump.
  ///
  /// In en, this message translates to:
  /// **'Preparing to jump.'**
  String get preparingToJump;

  /// No description provided for @productionNotes.
  ///
  /// In en, this message translates to:
  /// **'Production notes'**
  String get productionNotes;

  /// No description provided for @published.
  ///
  /// In en, this message translates to:
  /// **'Published'**
  String get published;

  /// No description provided for @publishedAt.
  ///
  /// In en, this message translates to:
  /// **'Published at'**
  String get publishedAt;

  /// No description provided for @pureBlack.
  ///
  /// In en, this message translates to:
  /// **'Pure black'**
  String get pureBlack;

  /// No description provided for @readsContentUnderTheRegisteredHeadingsAsRelatedI.
  ///
  /// In en, this message translates to:
  /// **'Reads content under the registered headings as related items. Note links inside the Vault can be tapped to open them.'**
  String get readsContentUnderTheRegisteredHeadingsAsRelatedI;

  /// No description provided for @readsTextQuotesAndCodeUnderTheRegisteredHeadings.
  ///
  /// In en, this message translates to:
  /// **'Reads text, quotes, and code under the registered headings as notes.'**
  String get readsTextQuotesAndCodeUnderTheRegisteredHeadings;

  /// No description provided for @related.
  ///
  /// In en, this message translates to:
  /// **'Related'**
  String get related;

  /// No description provided for @relatedNColorStudyFictionalColorStudyMd.
  ///
  /// In en, this message translates to:
  /// **'## Related\nn- [Color Study](./fictional-color-study.md)'**
  String get relatedNColorStudyFictionalColorStudyMd;

  /// No description provided for @relatedNotes.
  ///
  /// In en, this message translates to:
  /// **'Related notes'**
  String get relatedNotes;

  /// No description provided for @relative.
  ///
  /// In en, this message translates to:
  /// **'Relative'**
  String get relative;

  /// No description provided for @reloadVaultChanges.
  ///
  /// In en, this message translates to:
  /// **'Reload Vault changes'**
  String get reloadVaultChanges;

  /// No description provided for @requireAll.
  ///
  /// In en, this message translates to:
  /// **'Require all'**
  String get requireAll;

  /// No description provided for @rescan.
  ///
  /// In en, this message translates to:
  /// **'Rescan'**
  String get rescan;

  /// No description provided for @resetSettings.
  ///
  /// In en, this message translates to:
  /// **'Reset settings'**
  String get resetSettings;

  /// No description provided for @resetToDefaults.
  ///
  /// In en, this message translates to:
  /// **'Reset to defaults'**
  String get resetToDefaults;

  /// No description provided for @restoreAllAppearanceTagAndNoteStructureSettingsT.
  ///
  /// In en, this message translates to:
  /// **'Restore all appearance, tag, and note structure settings to their defaults. Notes inside the Vault will not be changed.'**
  String get restoreAllAppearanceTagAndNoteStructureSettingsT;

  /// No description provided for @restoreAppearanceAndNoteSettingsToTheirDefaults.
  ///
  /// In en, this message translates to:
  /// **'Restore appearance and note settings to their defaults.'**
  String get restoreAppearanceAndNoteSettingsToTheirDefaults;

  /// No description provided for @restoreDefaults.
  ///
  /// In en, this message translates to:
  /// **'Restore defaults'**
  String get restoreDefaults;

  /// No description provided for @restoreDefaults_c4cee4.
  ///
  /// In en, this message translates to:
  /// **'Restore defaults'**
  String get restoreDefaults_c4cee4;

  /// No description provided for @retrievesTheAuthorNameAndURLFromTheAuthorLinkAtT.
  ///
  /// In en, this message translates to:
  /// **'Retrieves the author name and URL from the author link at the start of the body. Links without @ are also supported.'**
  String get retrievesTheAuthorNameAndURLFromTheAuthorLinkAtT;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @saveAppearanceAndTagSettingsToOneJSONFile.
  ///
  /// In en, this message translates to:
  /// **'Save appearance and tag settings to one JSON file.'**
  String get saveAppearanceAndTagSettingsToOneJSONFile;

  /// No description provided for @screenTheme.
  ///
  /// In en, this message translates to:
  /// **'Screen theme'**
  String get screenTheme;

  /// No description provided for @searchAndFiltering.
  ///
  /// In en, this message translates to:
  /// **'Search and filtering'**
  String get searchAndFiltering;

  /// No description provided for @searchNotes.
  ///
  /// In en, this message translates to:
  /// **'Search notes'**
  String get searchNotes;

  /// No description provided for @searchTags.
  ///
  /// In en, this message translates to:
  /// **'Search tags'**
  String get searchTags;

  /// No description provided for @selectedVault.
  ///
  /// In en, this message translates to:
  /// **'Selected Vault'**
  String get selectedVault;

  /// No description provided for @selectThisVault.
  ///
  /// In en, this message translates to:
  /// **'Select this vault'**
  String get selectThisVault;

  /// No description provided for @setAsWallpaper.
  ///
  /// In en, this message translates to:
  /// **'Set as wallpaper.'**
  String get setAsWallpaper;

  /// No description provided for @setImageAsWallpaper.
  ///
  /// In en, this message translates to:
  /// **'Set image as wallpaper'**
  String get setImageAsWallpaper;

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// {label} settings
  ///
  /// In en, this message translates to:
  /// **'{label} settings'**
  String settings_5e5451(Object label);

  /// No description provided for @settingsHaveNotBeenInitialized.
  ///
  /// In en, this message translates to:
  /// **'Settings have not been initialized.'**
  String get settingsHaveNotBeenInitialized;

  /// No description provided for @settingsWereImported.
  ///
  /// In en, this message translates to:
  /// **'Settings were imported.'**
  String get settingsWereImported;

  /// No description provided for @settingsWereSavedAsJSON.
  ///
  /// In en, this message translates to:
  /// **'Settings were saved as JSON.'**
  String get settingsWereSavedAsJSON;

  /// No description provided for @shortest.
  ///
  /// In en, this message translates to:
  /// **'Shortest'**
  String get shortest;

  /// No description provided for @shortestChoosesTheClosestMatchingNoteWithTheSame.
  ///
  /// In en, this message translates to:
  /// **'Shortest chooses the closest matching note with the same name inside the Vault. Relative uses the current note location as the base, and Absolute uses the Vault root. Links that resolve outside the Vault are ignored.'**
  String get shortestChoosesTheClosestMatchingNoteWithTheSame;

  /// Show {label}
  ///
  /// In en, this message translates to:
  /// **'Show {label}'**
  String show(Object label);

  /// No description provided for @showAllMedia.
  ///
  /// In en, this message translates to:
  /// **'Show all media'**
  String get showAllMedia;

  /// No description provided for @showAnIconForHiddenMedia.
  ///
  /// In en, this message translates to:
  /// **'Show an icon for hidden media'**
  String get showAnIconForHiddenMedia;

  /// No description provided for @showEachTileSPositionInTheList.
  ///
  /// In en, this message translates to:
  /// **'Show each tile\'s position in the list'**
  String get showEachTileSPositionInTheList;

  /// No description provided for @showFileInFileManager.
  ///
  /// In en, this message translates to:
  /// **'Show file in file manager'**
  String get showFileInFileManager;

  /// No description provided for @showLoadedItemCount.
  ///
  /// In en, this message translates to:
  /// **'Show loaded item count'**
  String get showLoadedItemCount;

  /// No description provided for @showOtherCategory.
  ///
  /// In en, this message translates to:
  /// **'Show other category'**
  String get showOtherCategory;

  /// No description provided for @showsTheNumberOfItemsCurrentlyDisplayedInTheTopB.
  ///
  /// In en, this message translates to:
  /// **'Shows the number of items currently displayed in the top bar.'**
  String get showsTheNumberOfItemsCurrentlyDisplayedInTheTopB;

  /// No description provided for @showThemAsSeparateCategoriesForEachParentTag.
  ///
  /// In en, this message translates to:
  /// **'Show them as separate categories for each parent tag.'**
  String get showThemAsSeparateCategoriesForEachParentTag;

  /// No description provided for @sortDirection.
  ///
  /// In en, this message translates to:
  /// **'Sort direction'**
  String get sortDirection;

  /// No description provided for @sortField.
  ///
  /// In en, this message translates to:
  /// **'Sort field'**
  String get sortField;

  /// No description provided for @specificItemNamesAndNoteContentsAreNotShownOnThi.
  ///
  /// In en, this message translates to:
  /// **'Specific item names and note contents are not shown on this screen. Check access permissions and file status, then rescan.'**
  String get specificItemNamesAndNoteContentsAreNotShownOnThi;

  /// {path} · split deeper levels
  ///
  /// In en, this message translates to:
  /// **'{path} · split deeper levels'**
  String splitDeeperLevels(Object path);

  /// No description provided for @startAtItem.
  ///
  /// In en, this message translates to:
  /// **'Start at item'**
  String get startAtItem;

  /// No description provided for @stopsExtractingPostTextWhenThisHeadingIsReachedH.
  ///
  /// In en, this message translates to:
  /// **'Stops extracting post text when this heading is reached. Heading depth does not matter, and you can register multiple heading names.'**
  String get stopsExtractingPostTextWhenThisHeadingIsReachedH;

  /// No description provided for @switchVault.
  ///
  /// In en, this message translates to:
  /// **'Switch Vault'**
  String get switchVault;

  /// No description provided for @system.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get system;

  /// No description provided for @systemColorMaterialYou.
  ///
  /// In en, this message translates to:
  /// **'System color (Material You)'**
  String get systemColorMaterialYou;

  /// No description provided for @tagColors.
  ///
  /// In en, this message translates to:
  /// **'Tag colors'**
  String get tagColors;

  /// No description provided for @tagPath.
  ///
  /// In en, this message translates to:
  /// **'Tag path'**
  String get tagPath;

  /// No description provided for @tags.
  ///
  /// In en, this message translates to:
  /// **'Tags'**
  String get tags;

  /// No description provided for @tagSettings.
  ///
  /// In en, this message translates to:
  /// **'Tag settings'**
  String get tagSettings;

  /// Tags in {title}
  ///
  /// In en, this message translates to:
  /// **'Tags in {title}'**
  String tagsIn(Object title);

  /// No description provided for @tagsIncludedInFilters.
  ///
  /// In en, this message translates to:
  /// **'Tags included in filters'**
  String get tagsIncludedInFilters;

  /// No description provided for @tagsTitleAndMoreFixed.
  ///
  /// In en, this message translates to:
  /// **'Tags, title, and more (fixed)'**
  String get tagsTitleAndMoreFixed;

  /// No description provided for @theFictionalNoteExampleWasCopied.
  ///
  /// In en, this message translates to:
  /// **'The fictional note example was copied.'**
  String get theFictionalNoteExampleWasCopied;

  /// No description provided for @theFrontmatterTagsAndBodyBlockOrderInThisExample.
  ///
  /// In en, this message translates to:
  /// **'The Frontmatter tags and body block order in this example reflect the current settings. Frontmatter stays fixed, and the body follows the Markdown order below. It is not saved to the Vault automatically.'**
  String get theFrontmatterTagsAndBodyBlockOrderInThisExample;

  /// No description provided for @theItemCountIsStillLoadingButYouCanJumpToAPositi.
  ///
  /// In en, this message translates to:
  /// **'The item count is still loading, but you can jump to a position'**
  String get theItemCountIsStillLoadingButYouCanJumpToAPositi;

  /// No description provided for @theItemCountIsUnavailableButYouCanJumpToAPositio.
  ///
  /// In en, this message translates to:
  /// **'The item count is unavailable, but you can jump to a position'**
  String get theItemCountIsUnavailableButYouCanJumpToAPositio;

  /// No description provided for @theme.
  ///
  /// In en, this message translates to:
  /// **'Theme'**
  String get theme;

  /// No description provided for @theMediaFileWasNotFoundPleaseRescan.
  ///
  /// In en, this message translates to:
  /// **'The media file was not found. Please rescan.'**
  String get theMediaFileWasNotFoundPleaseRescan;

  /// No description provided for @theNoteWasNotFoundPleaseRescan.
  ///
  /// In en, this message translates to:
  /// **'The note was not found. Please rescan.'**
  String get theNoteWasNotFoundPleaseRescan;

  /// No description provided for @thereAreNoCategories.
  ///
  /// In en, this message translates to:
  /// **'There are no categories'**
  String get thereAreNoCategories;

  /// No description provided for @thereAreNoFictionalTagsToDisplay.
  ///
  /// In en, this message translates to:
  /// **'There are no fictional tags to display.'**
  String get thereAreNoFictionalTagsToDisplay;

  /// No description provided for @thereAreNoFilterTags.
  ///
  /// In en, this message translates to:
  /// **'There are no filter tags.'**
  String get thereAreNoFilterTags;

  /// No description provided for @thereAreNoHiddenTags.
  ///
  /// In en, this message translates to:
  /// **'There are no hidden tags.'**
  String get thereAreNoHiddenTags;

  /// No description provided for @thereAreNoItemsToShow.
  ///
  /// In en, this message translates to:
  /// **'There are no items to show'**
  String get thereAreNoItemsToShow;

  /// No description provided for @thereIsNoMediaToShowInThisNote.
  ///
  /// In en, this message translates to:
  /// **'There is no media to show in this note.'**
  String get thereIsNoMediaToShowInThisNote;

  /// No description provided for @theseAreTheNoteSImagesAndVideosTheyAreNotDuplica.
  ///
  /// In en, this message translates to:
  /// **'These are the note\'s images and videos. They are not duplicated in the details section and are shown in the media area of the viewer.'**
  String get theseAreTheNoteSImagesAndVideosTheyAreNotDuplica;

  /// No description provided for @theseItemsAreNotConfirmedToBeOutsideTheGalleryBe.
  ///
  /// In en, this message translates to:
  /// **'These items are not confirmed to be outside the gallery. Because we could not inspect tags inside the notes, we could not determine whether they qualify, so they were not added to the list.'**
  String get theseItemsAreNotConfirmedToBeOutsideTheGalleryBe;

  /// No description provided for @theSelectedAppCouldNotShowTheFile.
  ///
  /// In en, this message translates to:
  /// **'The selected app could not show the file.'**
  String get theSelectedAppCouldNotShowTheFile;

  /// No description provided for @theSettingsJSONIsMissingRequiredFields.
  ///
  /// In en, this message translates to:
  /// **'The settings JSON is missing required fields.'**
  String get theSettingsJSONIsMissingRequiredFields;

  /// No description provided for @theSettingsJSONIsTooLargeOrIsNotARegularFile.
  ///
  /// In en, this message translates to:
  /// **'The settings JSON is too large or is not a regular file.'**
  String get theSettingsJSONIsTooLargeOrIsNotARegularFile;

  /// No description provided for @theTotalIsStillLoadingEnterAPositiveInteger.
  ///
  /// In en, this message translates to:
  /// **'The total is still loading. Enter a positive integer.'**
  String get theTotalIsStillLoadingEnterAPositiveInteger;

  /// No description provided for @thisIsMetadataSuchAsTagsTitlesURLsDatesAndCovers.
  ///
  /// In en, this message translates to:
  /// **'This is metadata such as tags, titles, URLs, dates, and covers. It is fixed at the top of the details section and cannot be reordered or hidden.'**
  String get thisIsMetadataSuchAsTagsTitlesURLsDatesAndCovers;

  /// No description provided for @thisIsTheBodyPostTextContentBeforeTheHeadingSpec.
  ///
  /// In en, this message translates to:
  /// **'This is the body post text. Content before the heading specified as the end of the post text is shown.'**
  String get thisIsTheBodyPostTextContentBeforeTheHeadingSpec;

  /// This scan could not check {warnings} items.
  ///
  /// In en, this message translates to:
  /// **'This scan could not check {warnings} items.'**
  String thisScanCouldNotCheckItems(Object warnings);

  /// No description provided for @thisScanFoundNoUncheckedItems.
  ///
  /// In en, this message translates to:
  /// **'This scan found no unchecked items.'**
  String get thisScanFoundNoUncheckedItems;

  /// No description provided for @thisSettingsJSONFormatIsNotSupported.
  ///
  /// In en, this message translates to:
  /// **'This settings JSON format is not supported.'**
  String get thisSettingsJSONFormatIsNotSupported;

  /// No description provided for @title.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get title;

  /// No description provided for @treatsContentUnderTheseHeadingsAsNotesHeadingDep.
  ///
  /// In en, this message translates to:
  /// **'Treats content under these headings as notes. Heading depth does not matter, and you can register multiple heading names.'**
  String get treatsContentUnderTheseHeadingsAsNotesHeadingDep;

  /// No description provided for @treatsItemsUnderTheseHeadingsAsRelatedContentHea.
  ///
  /// In en, this message translates to:
  /// **'Treats items under these headings as related content. Heading depth does not matter, and you can register multiple heading names.'**
  String get treatsItemsUnderTheseHeadingsAsRelatedContentHea;

  /// No description provided for @turnLoopingOff.
  ///
  /// In en, this message translates to:
  /// **'Turn looping off'**
  String get turnLoopingOff;

  /// No description provided for @unableToReadTheNotePleaseCheckAccessPermissions.
  ///
  /// In en, this message translates to:
  /// **'Unable to read the note. Please check access permissions.'**
  String get unableToReadTheNotePleaseCheckAccessPermissions;

  /// No description provided for @unknownLinkResolutionSetting.
  ///
  /// In en, this message translates to:
  /// **'Unknown link resolution setting.'**
  String get unknownLinkResolutionSetting;

  /// Unknown note block: {entry}.
  ///
  /// In en, this message translates to:
  /// **'Unknown note block: {entry}.'**
  String unknownNoteBlock(Object entry);

  /// Unknown visible note block: {entry}.
  ///
  /// In en, this message translates to:
  /// **'Unknown visible note block: {entry}.'**
  String unknownVisibleNoteBlock(Object entry);

  /// No description provided for @unmute.
  ///
  /// In en, this message translates to:
  /// **'Unmute'**
  String get unmute;

  /// No description provided for @updated.
  ///
  /// In en, this message translates to:
  /// **'Updated'**
  String get updated;

  /// No description provided for @updatedAt.
  ///
  /// In en, this message translates to:
  /// **'Updated at'**
  String get updatedAt;

  /// No description provided for @useABlackBackgroundInTheDarkThemeTheSystemColorC.
  ///
  /// In en, this message translates to:
  /// **'Use a black background in the dark theme. The system color can still be used as an accent.'**
  String get useABlackBackgroundInTheDarkThemeTheSystemColorC;

  /// No description provided for @useTheInfoIconsInItemOrderToCheckHowEachSectionI.
  ///
  /// In en, this message translates to:
  /// **'Use the info icons in item order to check how each section is read. Settings that can be changed, such as headings and Frontmatter keys, are grouped under each section\'s settings button.'**
  String get useTheInfoIconsInItemOrderToCheckHowEachSectionI;

  /// No description provided for @useTheSystemAccentColor.
  ///
  /// In en, this message translates to:
  /// **'Use the system accent color'**
  String get useTheSystemAccentColor;

  /// No description provided for @vault.
  ///
  /// In en, this message translates to:
  /// **'Vault'**
  String get vault;

  /// Version {version}
  ///
  /// In en, this message translates to:
  /// **'Version {version}'**
  String version(Object version);

  /// No description provided for @video.
  ///
  /// In en, this message translates to:
  /// **'Video'**
  String get video;

  /// No description provided for @videoPlaybackPosition.
  ///
  /// In en, this message translates to:
  /// **'Video playback position'**
  String get videoPlaybackPosition;

  /// No description provided for @viewNoteFormatExample.
  ///
  /// In en, this message translates to:
  /// **'View note format example'**
  String get viewNoteFormatExample;

  /// No description provided for @viewOrCopyAFictionalNoteExample.
  ///
  /// In en, this message translates to:
  /// **'View or copy a fictional note example'**
  String get viewOrCopyAFictionalNoteExample;

  /// No description provided for @youCanAlsoReviewADisplayExampleThatReflectsTheCu.
  ///
  /// In en, this message translates to:
  /// **'You can also review a display example that reflects the current item order.'**
  String get youCanAlsoReviewADisplayExampleThatReflectsTheCu;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'ja'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'ja':
      return AppLocalizationsJa();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
