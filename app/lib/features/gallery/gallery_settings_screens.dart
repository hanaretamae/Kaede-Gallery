part of 'gallery_screen.dart';

class _GallerySettingsScreen extends ConsumerWidget {
  const _GallerySettingsScreen({required this.initialSession});

  final VaultSession initialSession;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session =
        ref.watch(vaultSessionProvider).asData?.value ?? initialSession;
    final warnings = session.scanReport.warnings;
    final galleryTagPrefixes =
        ref
            .watch(galleryTagSettingsProvider)
            .asData
            ?.value
            .noteStructure
            .galleryTagPrefixes ??
        const ['source/'];
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, context.l10n.settings),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsSectionHeading(
            context,
            context.l10n.appearance,
            first: true,
          ),
          const _AppearanceSettings(),
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.pagingAndListDisplay,
              supportingText:
                  context.l10n.configureListLoadingSizeItemCountsAndTileDisplay,
              leading: _settingsIcon(
                context,
                Icons.view_agenda_outlined,
                tone: 1,
              ),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _PaginationSettingsScreen(),
                ),
              ),
            ),
          ]),
          _settingsSectionHeading(context, context.l10n.vault),
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.selectedVault,
              supportingText: vaultDisplayName(session.vaultPath),
              leading: Icon(Icons.folder_outlined),
              trailing: M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.switchVault,
                icon: const Icon(Icons.folder_open),
                onPressed: () =>
                    ref.read(vaultSessionProvider.notifier).chooseVault(),
              ),
            ),
            M3EListItem(
              headline: context.l10n.itemsThatCouldNotBeChecked,
              supportingText: warnings == 0
                  ? context.l10n.none
                  : context.l10n
                        .itemsTheirContentsCouldNotBeCheckedSoWeCannotDet(
                          warnings,
                        ),
              leading: Icon(
                warnings > 0
                    ? Icons.warning_amber_rounded
                    : Icons.check_circle_outline,
                color: warnings > 0
                    ? Theme.of(context).colorScheme.tertiary
                    : Theme.of(context).colorScheme.primary,
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: context.l10n.detailsOfUncheckedItems,
                    icon: const Icon(Icons.info_outline),
                    onPressed: () => M3EDialog.show<void>(
                      context,
                      dialog: M3EDialog(
                        title: context.l10n.detailsOfUncheckedItems,
                        content: SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                warnings == 0
                                    ? context.l10n.thisScanFoundNoUncheckedItems
                                    : context.l10n.thisScanCouldNotCheckItems(
                                        warnings,
                                      ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                context
                                    .l10n
                                    .examplesOfWhyItemsCouldNotBeChecked,
                                style: TextStyle(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                context
                                    .l10n
                                    .filesOrFoldersCannotBeReadTheNoteEncodingIsNotUT,
                              ),
                              const SizedBox(height: 16),
                              Text(
                                context
                                    .l10n
                                    .theseItemsAreNotConfirmedToBeOutsideTheGalleryBe,
                              ),
                              const SizedBox(height: 8),
                              Text(
                                galleryTagPrefixes.isEmpty
                                    ? context
                                          .l10n
                                          .noGalleryTargetTagsAreConfiguredSoEvenNotesThatW
                                    : context.l10n
                                          .amongNotesThatWereReadSuccessfullyThoseWithoutTh(
                                            galleryTagPrefixes.join('・'),
                                          ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                context
                                    .l10n
                                    .specificItemNamesAndNoteContentsAreNotShownOnThi,
                              ),
                            ],
                          ),
                        ),
                        actions: [
                          M3EButton.text(
                            onPressed: () => Navigator.of(context).pop(),
                            child: Text(context.l10n.close),
                          ),
                        ],
                      ),
                    ),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: context.l10n.rescan,
                    icon: const Icon(Icons.refresh),
                    onPressed: () =>
                        ref.read(vaultSessionProvider.notifier).rescan(),
                  ),
                ],
              ),
            ),
          ]),
          _settingsSectionHeading(context, context.l10n.notes),
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.noteStructureAndDisplay,
              supportingText:
                  context.l10n.configureNoteItemOrderHeadingParsingAndListDispl,
              leading: _settingsIcon(
                context,
                Icons.account_tree_outlined,
                tone: 1,
              ),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _NoteStructureSettingsScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: context.l10n.tagSettings,
              supportingText:
                  context.l10n.configureGalleryTargetTagsFiltersAndDisplayColor,
              leading: _settingsIcon(context, Icons.label_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _TagRulesSettingsScreen(),
                ),
              ),
            ),
          ]),
          _settingsSectionHeading(context, context.l10n.aboutThisApp),
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.informationAndLicenses,
              leading: _settingsIcon(context, Icons.info_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _AboutScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: context.l10n.importExportAndReset,
              leading: _settingsIcon(context, Icons.storage_outlined, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _DataSettingsScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: context.l10n.helpAndHowToUse,
              leading: _settingsIcon(context, Icons.help_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _GalleryHelpScreen(),
                ),
              ),
            ),
          ]),
        ],
      ),
    );
  }
}

Widget _settingsGroup(
  BuildContext context,
  List<M3EListItem> items, {
  Color? color,
}) => Padding(
  padding: const EdgeInsets.only(bottom: 12),
  child: Builder(
    builder: (context) {
      // M3EListItem draws headlines with bodyLarge; bolden it for this list only.
      final theme = M3ETheme.of(context);
      final type = theme.typeScale;
      return M3ETheme(
        data: theme.copyWith(
          typeScale: M3ETypeScale(
            displayLarge: type.displayLarge,
            displayMedium: type.displayMedium,
            displaySmall: type.displaySmall,
            headlineLarge: type.headlineLarge,
            headlineMedium: type.headlineMedium,
            headlineSmall: type.headlineSmall,
            titleLarge: type.titleLarge,
            titleMedium: type.titleMedium,
            titleSmall: type.titleSmall,
            bodyLarge: type.bodyLarge.copyWith(fontWeight: FontWeight.bold),
            bodyMedium: type.bodyMedium,
            bodySmall: type.bodySmall,
            labelLarge: type.labelLarge,
            labelMedium: type.labelMedium,
            labelSmall: type.labelSmall,
          ),
        ),
        child: M3EList(
          itemCount: items.length,
          itemBuilder: (context, index) => items[index],
          onTap: (index) => items[index].onTap?.call(),
          color: color ?? Theme.of(context).colorScheme.surfaceContainerHigh,
        ),
      );
    },
  ),
);

Widget _settingsSectionHeading(
  BuildContext context,
  String title, {
  bool first = false,
}) => Padding(
  padding: EdgeInsets.fromLTRB(16, first ? 8 : 24, 16, 8),
  child: Text(
    title,
    style: Theme.of(context).textTheme.titleSmall?.copyWith(
      color: Theme.of(context).colorScheme.primary,
      fontWeight: FontWeight.w600,
    ),
  ),
);

class _PaginationSettingsScreen extends ConsumerWidget {
  const _PaginationSettingsScreen();

  Future<void> _save(
    BuildContext context,
    WidgetRef ref,
    GalleryTagSettings settings,
  ) async {
    try {
      await ref
          .read(galleryTagSettingsProvider.notifier)
          .saveSettings(settings);
      await ref.read(vaultSessionProvider.notifier).rescan();
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotSavePagingSettings,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, context.l10n.pagingAndListDisplay),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) =>
            Center(child: Text(context.l10n.couldNotLoadListSettings)),
        data: (settings) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            _settingsSectionHeading(
              context,
              context.l10n.listDisplay,
              first: true,
            ),
            _PaginationSettingsCard(
              pagination: settings.pagination,
              onChanged: (pagination) => _save(
                context,
                ref,
                settings.copyWith(pagination: pagination),
              ),
            ),
          ],
        ),
        skipLoadingOnReload: true,
      ),
    );
  }
}

class _GalleryHelpScreen extends StatelessWidget {
  const _GalleryHelpScreen();

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: M3EAppBar.top(
      title: galleryAppBarTitle(context, context.l10n.helpAndHowToUse),
      leading: _expressiveBackButton(context),
    ),
    body: ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      children: [
        _settingsGroup(context, [
          _helpItem(
            icon: Icons.link,
            title: context.l10n.relatedNotes,
            body: context.l10n.markdownLinksImageLinksAndObsidianWikilinksEmbed,
          ),
          _helpItem(
            icon: Icons.tune,
            title: context.l10n.noteStructure,
            body: context.l10n.useTheInfoIconsInItemOrderToCheckHowEachSectionI,
          ),
          _helpItem(
            icon: Icons.search,
            title: context.l10n.searchAndFiltering,
            body: context.l10n.noteSearchSupportsNamesAndPathsPlusTagTagAndTagT,
          ),
        ]),
      ],
    ),
  );
}

M3EListItem _helpItem({
  required IconData icon,
  required String title,
  required String body,
}) => M3EListItem(headline: title, supportingText: body, leading: Icon(icon));

class _AboutScreen extends StatelessWidget {
  const _AboutScreen();

  static const _repositoryUrl = 'https://github.com/hanaretamae/Kaede-Gallery';
  static const _version = String.fromEnvironment(
    'FLUTTER_BUILD_NAME',
    defaultValue: '1.0.0',
  );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, context.l10n.aboutThisApp),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _AboutHero(version: _version),
          _settingsSectionHeading(context, context.l10n.information),
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.openSourceLicenses,
              leading: const Icon(Icons.article_outlined),
              onTap: () => showLicensePage(
                context: context,
                applicationName: 'Kaede Gallery',
                applicationVersion: _version,
              ),
            ),
            M3EListItem(
              headline: context.l10n.githubRepository,
              supportingText: _repositoryUrl,
              leading: Icon(Icons.open_in_new),
              onTap: () => _openExternalUri(context, Uri.parse(_repositoryUrl)),
            ),
          ]),
        ],
      ),
    );
  }
}

/// Combines settings import/export with the destructive reset/delete
/// actions in a single "アプリ" screen, as requested, instead of four
/// separate top-level cards.
class _DataSettingsScreen extends ConsumerWidget {
  const _DataSettingsScreen();

  Future<void> _exportSettings(BuildContext context, WidgetRef ref) async {
    try {
      final appearance = await ref.read(galleryAppearanceProvider.future);
      final tagSettings = await ref.read(galleryTagSettingsProvider.future);
      final json = const JsonEncoder.withIndent('  ').convert({
        'version': 1,
        'appearance': appearance.toJson(),
        'tags': tagSettings.toJson(),
      });
      if (Platform.isAndroid) {
        final saved = await ref
            .read(vaultPlatformProvider)
            .safAccess
            .saveJson('vault-gallery-settings.json', '$json\n');
        if (saved && context.mounted) {
          M3ESnackbar.show(
            context,
            message: context.l10n.settingsWereSavedAsJSON,
          );
        }
        return;
      }
      final outputPath = await getSaveLocation(
        suggestedName: 'vault-gallery-settings.json',
        acceptedTypeGroups: [
          const XTypeGroup(label: 'JSON', extensions: ['json']),
        ],
      );
      if (outputPath == null) return;
      final session = ref.read(vaultSessionProvider).asData?.value;
      if (await _destinationIsInsideVault(outputPath.path, session)) {
        if (context.mounted) {
          M3ESnackbar.show(
            context,
            message:
                context.l10n.cannotSaveInsideTheVaultChooseADifferentDestinat,
          );
        }
        return;
      }
      await File(outputPath.path).writeAsString('$json\n', flush: true);
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.settingsWereSavedAsJSON,
        );
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotSaveSettingsToJSON,
        );
      }
    } on FormatException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message:
              context.l10n.couldNotSaveToJSONBecauseTheSettingsCouldNotBeRe,
        );
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: error.code == 'VAULT_READ_ONLY'
              ? context.l10n.cannotSaveInsideTheVaultChooseADifferentDestinat
              : context.l10n.couldNotOpenTheDestinationForTheSettingsFile,
        );
      }
    }
  }

  Future<bool> _destinationIsInsideVault(
    String destination,
    VaultSession? session,
  ) async {
    if (session == null || session.vaultPath.startsWith('content://')) {
      return false;
    }
    final vaultRoot = await Directory(session.vaultPath).resolveSymbolicLinks();
    final outputFile = File(destination);
    late final String resolvedDestination;
    if (await outputFile.exists()) {
      resolvedDestination = await outputFile.resolveSymbolicLinks();
    } else {
      final parent = await Directory(p.dirname(destination))
          .resolveSymbolicLinks();
      resolvedDestination = p.join(parent, p.basename(destination));
    }
    return p.isWithin(vaultRoot, resolvedDestination);
  }

  Future<void> _importSettings(BuildContext context, WidgetRef ref) async {
    try {
      final selected = await openFile(
        acceptedTypeGroups: [
          const XTypeGroup(label: 'JSON', extensions: ['json']),
        ],
      );
      if (selected == null) return;
      final file = File(selected.path);
      final metadata = await file.stat();
      if (metadata.type != FileSystemEntityType.file ||
          metadata.size > 1024 * 1024) {
        throw FormatException(
          AppL10n.current.theSettingsJSONIsTooLargeOrIsNotARegularFile,
        );
      }
      final decoded = jsonDecode(await file.readAsString());
      if (decoded is! Map<String, dynamic> || decoded['version'] != 1) {
        throw FormatException(
          AppL10n.current.thisSettingsJSONFormatIsNotSupported,
        );
      }
      final appearanceValue = decoded['appearance'];
      final tagsValue = decoded['tags'];
      if (appearanceValue is! Map<String, dynamic> ||
          tagsValue is! Map<String, dynamic>) {
        throw FormatException(
          AppL10n.current.theSettingsJSONIsMissingRequiredFields,
        );
      }
      final appearance = GalleryAppearance.fromJson(appearanceValue);
      final tags = GalleryTagSettings.fromJson(tagsValue);
      await ref.read(galleryTagSettingsProvider.notifier).saveSettings(tags);
      await ref
          .read(galleryAppearanceProvider.notifier)
          .updateAppearance(appearance);
      await ref.read(vaultSessionProvider.notifier).rescan();
      if (context.mounted) {
        M3ESnackbar.show(context, message: context.l10n.settingsWereImported);
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotReadOrWriteTheSettingsFile,
        );
      }
    } on FormatException catch (error) {
      if (context.mounted) {
        M3ESnackbar.show(context, message: error.message);
      }
    } on PlatformException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotOpenTheSettingsFile,
        );
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.settingsHaveNotBeenInitialized,
        );
      }
    }
  }

  Future<void> _resetSettings(BuildContext context, WidgetRef ref) async {
    final reset = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: context.l10n.resetSettings,
        content: Text(
          context.l10n.restoreAllAppearanceTagAndNoteStructureSettingsT,
        ),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(context.l10n.cancel),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(context.l10n.restoreDefaults),
          ),
        ],
      ),
    );
    if (!context.mounted || reset != true) return;
    try {
      await ref
          .read(galleryTagSettingsProvider.notifier)
          .saveSettings(const GalleryTagSettings());
      await ref
          .read(galleryAppearanceProvider.notifier)
          .updateAppearance(const GalleryAppearance());
      await ref.read(vaultSessionProvider.notifier).rescan();
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.allSettingsWereRestoredToTheirDefaults,
        );
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: context.l10n.couldNotResetSettings);
      }
    }
  }

  Future<void> _forgetVault(
    BuildContext context,
    WidgetRef ref,
    VaultSession session,
  ) async {
    final isSafVault = session.vaultPath.startsWith('content://');
    final vaultAccessText = isSafVault ? context.l10n.andReadPermission : '';
    final confirmed = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: context.l10n.forgetThisVault,
        content: Text(
          context.l10n.inAppIndexCacheAndSelectionDataWillBeRemovedFile(
            vaultDisplayName(session.vaultPath),
            vaultAccessText,
          ),
        ),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(context.l10n.cancel),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(context.l10n.forget),
          ),
        ],
      ),
    );
    if (!context.mounted || confirmed != true) return;
    try {
      await ref.read(vaultSessionProvider.notifier).forgetVault();
      if (context.mounted) {
        Navigator.of(context).popUntil((route) => route.isFirst);
      }
    } on Exception {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotDeleteTheVaultSInAppDataPleaseTryAgain,
        );
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: context.l10n.couldNotDeleteTheVaultSInAppDataPleaseTryAgain,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(vaultSessionProvider).asData?.value;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, context.l10n.app),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsGroup(context, [
            M3EListItem(
              headline: context.l10n.exportSettingsAsJSON,
              supportingText:
                  context.l10n.saveAppearanceAndTagSettingsToOneJSONFile,
              leading: Icon(Icons.save_alt),
              onTap: () => _exportSettings(context, ref),
            ),
            M3EListItem(
              headline: context.l10n.importSettingsFromJSON,
              supportingText: context.l10n.loadSettingsThatWereExportedEarlier,
              leading: Icon(Icons.file_open_outlined),
              onTap: () => _importSettings(context, ref),
            ),
            M3EListItem(
              headline: context.l10n.resetSettings,
              supportingText:
                  context.l10n.restoreAppearanceAndNoteSettingsToTheirDefaults,
              leading: const Icon(Icons.settings_backup_restore_outlined),
              onTap: () => _resetSettings(context, ref),
            ),
            if (session != null)
              M3EListItem(
                headline: context.l10n.forgetThisVault,
                supportingText:
                    context.l10n.deleteTheVaultSelectionIndexAndCache,
                leading: Icon(Icons.delete_outline),
                onTap: () => _forgetVault(context, ref, session),
              ),
          ]),
        ],
      ),
    );
  }
}

/// Expressive hero card: large rounded tonal container, oversized icon and a
/// pill-shaped version badge.
class _AboutHero extends StatelessWidget {
  const _AboutHero({required this.version});

  final String version;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(top: 8, bottom: 8),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: scheme.primaryContainer,
          borderRadius: BorderRadius.circular(GalleryShape.extraLarge),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 32, 24, 28),
          child: SizedBox(
            width: double.infinity,
            child: Column(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(GalleryShape.large),
                  child: Image.asset(
                    'assets/branding/kaede-gallery-icon.png',
                    width: 88,
                    height: 88,
                    semanticLabel: context.l10n.kaedeGalleryIcon,
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  'Kaede Gallery',
                  style: text.headlineMedium?.copyWith(
                    color: scheme.onPrimaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 10),
                DecoratedBox(
                  decoration: BoxDecoration(
                    color: scheme.primary,
                    borderRadius: BorderRadius.circular(100),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 6,
                    ),
                    child: Text(
                      context.l10n.version(version),
                      style: text.labelLarge?.copyWith(color: scheme.onPrimary),
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  context.l10n.anOfflineGalleryForBrowsingNotesAndMediaInsideYo,
                  textAlign: TextAlign.center,
                  style: text.bodyLarge?.copyWith(
                    color: scheme.onPrimaryContainer,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
