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
        title: galleryAppBarTitle(context, tr('設定', 'Settings')),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsSectionHeading(context, tr('外観', 'Appearance'), first: true),
          const _AppearanceSettings(),
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('ページングと一覧表示', 'Paging and list display'),
              supportingText: tr(
                '一覧の読み込み単位や件数、タイルの表示を設定します',
                'Configure list loading size, item counts, and tile display.',
              ),
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
          _settingsSectionHeading(context, tr('保管庫', 'Vault')),
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('選択中の Vault', 'Selected Vault'),
              supportingText: vaultDisplayName(session.vaultPath),
              leading: Icon(Icons.folder_outlined),
              trailing: M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: tr('Vault を切り替え', 'Switch Vault'),
                icon: const Icon(Icons.folder_open),
                onPressed: () =>
                    ref.read(vaultSessionProvider.notifier).chooseVault(),
              ),
            ),
            M3EListItem(
              headline: tr('確認できなかった項目', 'Items that could not be checked'),
              supportingText: warnings == 0
                  ? tr('ありません', 'None')
                  : tr(
                      '$warnings 件。内容を確認できないため、ギャラリー対象かは判定できません。',
                      '$warnings items. Their contents could not be checked, so we cannot determine whether they belong in the gallery.',
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
                    tooltip: tr('確認できなかった項目の詳細', 'Details of unchecked items'),
                    icon: const Icon(Icons.info_outline),
                    onPressed: () => M3EDialog.show<void>(
                      context,
                      dialog: M3EDialog(
                        title: tr(
                          '確認できなかった項目の詳細',
                          'Details of unchecked items',
                        ),
                        content: SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                warnings == 0
                                    ? tr(
                                        '今回の走査では確認できなかった項目はありません。',
                                        'This scan found no unchecked items.',
                                      )
                                    : tr(
                                        '今回の走査では $warnings 件を確認できませんでした。',
                                        'This scan could not check $warnings items.',
                                      ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                tr(
                                  '確認できない理由の例',
                                  'Examples of why items could not be checked',
                                ),
                                style: TextStyle(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                tr(
                                  '・ファイルやフォルダーを読み取れない\n'
                                      '・ノートの文字コードが UTF-8 ではない\n'
                                      '・YAML の書式を解析できない\n'
                                      '・ノートのサイズやタグ数が上限を超えている',
                                  '• Files or folders cannot be read\n'
                                      '• The note encoding is not UTF-8\n'
                                      '• The YAML format cannot be parsed\n'
                                      '• The note size or tag count exceeds the limit',
                                ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                tr(
                                  'これらはギャラリー対象外と確定した項目ではありません。'
                                      'ノート内のタグを確認できないため、対象かどうかを判定できず、'
                                      '一覧にも追加していません。',
                                  'These items are not confirmed to be outside the gallery. '
                                      'Because we could not inspect tags inside the notes, we could not determine whether they qualify, '
                                      'so they were not added to the list.',
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                galleryTagPrefixes.isEmpty
                                    ? tr(
                                        'ギャラリー対象タグが未設定のため、正常に読み取れたノートも対象外です。',
                                        'No gallery target tags are configured, so even notes that were read successfully are excluded.',
                                      )
                                    : tr(
                                        '正常に読み取れたノートのうち、'
                                            '${galleryTagPrefixes.join('・')} タグがないものは対象外です。',
                                        'Among notes that were read successfully, those without the ${galleryTagPrefixes.join('・')} tag are excluded.',
                                      ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                tr(
                                  '具体的な項目名やノートの内容はこの画面に表示しません。'
                                      'アクセス権やファイルの状態を確認してから再走査してください。',
                                  'Specific item names and note contents are not shown on this screen. '
                                      'Check access permissions and file status, then rescan.',
                                ),
                              ),
                            ],
                          ),
                        ),
                        actions: [
                          M3EButton.text(
                            onPressed: () => Navigator.of(context).pop(),
                            child: Text(tr('閉じる', 'Close')),
                          ),
                        ],
                      ),
                    ),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: tr('再走査', 'Rescan'),
                    icon: const Icon(Icons.refresh),
                    onPressed: () =>
                        ref.read(vaultSessionProvider.notifier).rescan(),
                  ),
                ],
              ),
            ),
          ]),
          _settingsSectionHeading(context, tr('ノート', 'Notes')),
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('ノート構造と表示', 'Note structure and display'),
              supportingText: tr(
                'ノートの項目順や見出しの読み取り、一覧表示を設定します',
                'Configure note item order, heading parsing, and list display.',
              ),
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
              headline: tr('タグ設定', 'Tag settings'),
              supportingText: tr(
                'ギャラリー対象タグ、絞り込み、表示色を設定します',
                'Configure gallery target tags, filters, and display colors.',
              ),
              leading: _settingsIcon(context, Icons.label_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _TagRulesSettingsScreen(),
                ),
              ),
            ),
          ]),
          _settingsSectionHeading(context, tr('このアプリについて', 'About this app')),
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('情報とライセンス', 'Information and licenses'),
              leading: _settingsIcon(context, Icons.info_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _AboutScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: tr('インポート・エクスポート・リセット', 'Import, export, and reset'),
              leading: _settingsIcon(context, Icons.storage_outlined, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _DataSettingsScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: tr('ヘルプと使い方', 'Help and how to use'),
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
          message: tr('ページング設定を保存できませんでした。', 'Could not save paging settings.'),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(
          context,
          tr('ページングと一覧表示', 'Paging and list display'),
        ),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => Center(
          child: Text(tr('一覧設定を読み込めませんでした。', 'Could not load list settings.')),
        ),
        data: (settings) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            _settingsSectionHeading(
              context,
              tr('一覧表示', 'List display'),
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
      title: galleryAppBarTitle(context, tr('ヘルプと使い方', 'Help and how to use')),
      leading: _expressiveBackButton(context),
    ),
    body: ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      children: [
        _settingsGroup(context, [
          _helpItem(
            icon: Icons.link,
            title: tr('関連ノート', 'Related notes'),
            body: tr(
              '関連見出し内の Markdown link / image link と Obsidian wikilink / embed は、Vault内のノートを指していればタップして詳細を開けます。',
              'Markdown links / image links and Obsidian wikilinks / embeds inside related headings can be tapped to open details when they point to notes in the Vault.',
            ),
          ),
          _helpItem(
            icon: Icons.tune,
            title: tr('ノート構造', 'Note structure'),
            body: tr(
              '項目順の説明アイコンで読み取り方法を確認できます。見出しやFrontmatterキーなど変更できる設定は、各項目の設定ボタンにまとめています。',
              "Use the info icons in item order to check how each section is read. Settings that can be changed, such as headings and Frontmatter keys, are grouped under each section's settings button.",
            ),
          ),
          _helpItem(
            icon: Icons.search,
            title: tr('検索と絞り込み', 'Search and filtering'),
            body: tr(
              'ノート検索は名前・パスに加えて #タグ、-#タグ、&#タグに対応します。下のタグ検索は絞り込み候補の表示だけを絞ります。',
              'Note search supports names and paths, plus #tag, -#tag, and &#tag. The tag search below only narrows the displayed filter candidates.',
            ),
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
        title: galleryAppBarTitle(context, tr('このアプリについて', 'About this app')),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _AboutHero(version: _version),
          _settingsSectionHeading(context, tr('情報', 'Information')),
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('オープンソースライセンス', 'Open-source licenses'),
              leading: const Icon(Icons.article_outlined),
              onTap: () => showLicensePage(
                context: context,
                applicationName: 'Kaede Gallery',
                applicationVersion: _version,
              ),
            ),
            M3EListItem(
              headline: tr('GitHub リポジトリ', 'GitHub repository'),
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
            message: tr('設定をJSONで保存しました。', 'Settings were saved as JSON.'),
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
            message: tr(
              'Vault 内には保存できません。別の保存先を選択してください。',
              'Cannot save inside the Vault. Choose a different destination.',
            ),
          );
        }
        return;
      }
      await File(outputPath.path).writeAsString('$json\n', flush: true);
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr('設定をJSONで保存しました。', 'Settings were saved as JSON.'),
        );
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr(
            '設定をJSONに保存できませんでした。',
            'Could not save settings to JSON.',
          ),
        );
      }
    } on FormatException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr(
            '設定を読み取れないため、JSONに保存できませんでした。',
            'Could not save to JSON because the settings could not be read.',
          ),
        );
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: error.code == 'VAULT_READ_ONLY'
              ? tr(
                  'Vault 内には保存できません。別の保存先を選択してください。',
                  'Cannot save inside the Vault. Choose a different destination.',
                )
              : tr(
                  '設定ファイルの保存先を開けませんでした。',
                  'Could not open the destination for the settings file.',
                ),
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
          tr(
            '設定JSONが大きすぎるか、通常ファイルではありません。',
            'The settings JSON is too large or is not a regular file.',
          ),
        );
      }
      final decoded = jsonDecode(await file.readAsString());
      if (decoded is! Map<String, dynamic> || decoded['version'] != 1) {
        throw FormatException(
          tr('未対応の設定JSONです。', 'This settings JSON format is not supported.'),
        );
      }
      final appearanceValue = decoded['appearance'];
      final tagsValue = decoded['tags'];
      if (appearanceValue is! Map<String, dynamic> ||
          tagsValue is! Map<String, dynamic>) {
        throw FormatException(
          tr(
            '設定JSONに必要な項目がありません。',
            'The settings JSON is missing required fields.',
          ),
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
        M3ESnackbar.show(
          context,
          message: tr('設定をインポートしました。', 'Settings were imported.'),
        );
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr(
            '設定ファイルを読み書きできませんでした。',
            'Could not read or write the settings file.',
          ),
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
          message: tr('設定ファイルを開けませんでした。', 'Could not open the settings file.'),
        );
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr('設定が初期化されていません。', 'Settings have not been initialized.'),
        );
      }
    }
  }

  Future<void> _resetSettings(BuildContext context, WidgetRef ref) async {
    final reset = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: tr('設定をリセット', 'Reset settings'),
        content: Text(
          tr(
            '外観・タグ・ノート構造の設定をすべて既定値に戻します。Vault内のノートは変更しません。',
            'Restore all appearance, tag, and note structure settings to their defaults. Notes inside the Vault will not be changed.',
          ),
        ),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(tr('キャンセル', 'Cancel')),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(tr('既定値に戻す', 'Restore defaults')),
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
          message: tr(
            'すべての設定を既定値に戻しました。',
            'All settings were restored to their defaults.',
          ),
        );
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr('設定をリセットできませんでした。', 'Could not reset settings.'),
        );
      }
    }
  }

  Future<void> _forgetVault(
    BuildContext context,
    WidgetRef ref,
    VaultSession session,
  ) async {
    final isSafVault = session.vaultPath.startsWith('content://');
    final vaultAccessText = isSafVault
        ? tr('と読み取りアクセス権', ' and read permission')
        : '';
    final confirmed = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: tr('この Vault を忘れる', 'Forget this Vault'),
        content: Text(
          tr(
            '${vaultDisplayName(session.vaultPath)} のアプリ内インデックス、キャッシュ、選択情報'
                '$vaultAccessTextを削除します。'
                'Vault 内のファイルと外観・タグなどのアプリ設定は変更しません。',
            '${vaultDisplayName(session.vaultPath)}\'s in-app index, cache, and selection data'
                '$vaultAccessText will be removed.'
                ' Files inside the Vault and app settings such as appearance and tags will not be changed.',
          ),
        ),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(tr('キャンセル', 'Cancel')),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(tr('忘れる', 'Forget')),
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
          message: tr(
            'Vault のアプリ内データを削除できませんでした。もう一度お試しください。',
            "Could not delete the Vault's in-app data. Please try again.",
          ),
        );
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: tr(
            'Vault のアプリ内データを削除できませんでした。もう一度お試しください。',
            "Could not delete the Vault's in-app data. Please try again.",
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(vaultSessionProvider).asData?.value;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, tr('アプリ', 'App')),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsGroup(context, [
            M3EListItem(
              headline: tr('設定をJSONでエクスポート', 'Export settings as JSON'),
              supportingText: tr(
                '外観とタグ設定を1つのJSONファイルに保存します',
                'Save appearance and tag settings to one JSON file.',
              ),
              leading: Icon(Icons.save_alt),
              onTap: () => _exportSettings(context, ref),
            ),
            M3EListItem(
              headline: tr('設定をJSONからインポート', 'Import settings from JSON'),
              supportingText: tr(
                '以前にエクスポートした設定を読み込みます',
                'Load settings that were exported earlier.',
              ),
              leading: Icon(Icons.file_open_outlined),
              onTap: () => _importSettings(context, ref),
            ),
            M3EListItem(
              headline: tr('設定をリセット', 'Reset settings'),
              supportingText: tr(
                '外観・ノート設定を既定値に戻します',
                'Restore appearance and note settings to their defaults.',
              ),
              leading: const Icon(Icons.settings_backup_restore_outlined),
              onTap: () => _resetSettings(context, ref),
            ),
            if (session != null)
              M3EListItem(
                headline: tr('この Vault を忘れる', 'Forget this Vault'),
                supportingText: tr(
                  'Vault の選択情報、インデックスとキャッシュを削除します',
                  'Delete the Vault selection, index, and cache.',
                ),
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
                    semanticLabel: tr(
                      'Kaede Gallery のアイコン',
                      'Kaede Gallery icon',
                    ),
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
                      tr('バージョン $version', 'Version $version'),
                      style: text.labelLarge?.copyWith(color: scheme.onPrimary),
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  tr(
                    'Obsidian Vault 内のノートとメディアを閲覧するオフラインギャラリーです。',
                    'An offline gallery for browsing notes and media inside your Obsidian Vault.',
                  ),
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
