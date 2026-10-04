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
        title: galleryAppBarTitle(context, '設定'),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsSectionHeading(context, '外観', first: true),
          const _AppearanceSettings(),
          _settingsGroup(context, [
            M3EListItem(
              headline: 'ページングと一覧表示',
              supportingText: '一覧の読み込み単位や件数、タイルの表示を設定します',
              leading: _settingsIcon(context, Icons.view_agenda_outlined),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _PaginationSettingsScreen(),
                ),
              ),
            ),
          ]),
          _settingsSectionHeading(context, '保管庫'),
          _settingsGroup(context, [
            M3EListItem(
              headline: '選択中の Vault',
              supportingText: vaultDisplayName(session.vaultPath),
              leading: Icon(Icons.folder_outlined),
              trailing: M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: 'Vault を切り替え',
                icon: const Icon(Icons.folder_open),
                onPressed: () =>
                    ref.read(vaultSessionProvider.notifier).chooseVault(),
              ),
            ),
            M3EListItem(
              headline: '確認できなかった項目',
              supportingText: warnings == 0
                  ? 'ありません'
                  : '$warnings 件。内容を確認できないため、ギャラリー対象かは判定できません。',
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
                    tooltip: '確認できなかった項目の詳細',
                    icon: const Icon(Icons.info_outline),
                    onPressed: () => M3EDialog.show<void>(
                      context,
                      dialog: M3EDialog(
                        title: '確認できなかった項目の詳細',
                        content: SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                warnings == 0
                                    ? '今回の走査では確認できなかった項目はありません。'
                                    : '今回の走査では $warnings 件を確認できませんでした。',
                              ),
                              const SizedBox(height: 16),
                              const Text(
                                '確認できない理由の例',
                                style: TextStyle(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 6),
                              const Text(
                                '・ファイルやフォルダーを読み取れない\n'
                                '・ノートの文字コードが UTF-8 ではない\n'
                                '・YAML の書式を解析できない\n'
                                '・ノートのサイズやタグ数が上限を超えている',
                              ),
                              const SizedBox(height: 16),
                              const Text(
                                'これらはギャラリー対象外と確定した項目ではありません。'
                                'ノート内のタグを確認できないため、対象かどうかを判定できず、'
                                '一覧にも追加していません。',
                              ),
                              const SizedBox(height: 8),
                              Text(
                                galleryTagPrefixes.isEmpty
                                    ? 'ギャラリー対象タグが未設定のため、正常に読み取れたノートも対象外です。'
                                    : '正常に読み取れたノートのうち、'
                                          '${galleryTagPrefixes.join('・')} タグがないものは対象外です。',
                              ),
                              const SizedBox(height: 16),
                              const Text(
                                '具体的な項目名やノートの内容はこの画面に表示しません。'
                                'アクセス権やファイルの状態を確認してから再走査してください。',
                              ),
                            ],
                          ),
                        ),
                        actions: [
                          M3EButton.text(
                            onPressed: () => Navigator.of(context).pop(),
                            child: const Text('閉じる'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: '再走査',
                    icon: const Icon(Icons.refresh),
                    onPressed: () =>
                        ref.read(vaultSessionProvider.notifier).rescan(),
                  ),
                ],
              ),
            ),
          ]),
          _settingsSectionHeading(context, 'ノート'),
          _settingsGroup(context, [
            M3EListItem(
              headline: 'ノート構造と表示',
              supportingText: 'ノートの項目順や見出しの読み取り、一覧表示を設定します',
              leading: _settingsIcon(context, Icons.account_tree_outlined),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _NoteStructureSettingsScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: 'タグ設定',
              supportingText: 'ギャラリー対象タグ、絞り込み、表示色を設定します',
              leading: _settingsIcon(context, Icons.label_outline, tone: 0),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _TagRulesSettingsScreen(),
                ),
              ),
            ),
          ]),
          _settingsSectionHeading(context, 'このアプリについて'),
          _settingsGroup(context, [
            M3EListItem(
              headline: '情報とライセンス',
              leading: _settingsIcon(context, Icons.info_outline, tone: 1),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _AboutScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: 'インポート・エクスポート・リセット',
              leading: _settingsIcon(context, Icons.storage_outlined, tone: 2),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _DataSettingsScreen(),
                ),
              ),
            ),
            M3EListItem(
              headline: 'ヘルプと使い方',
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
  child: M3EList(
    itemCount: items.length,
    itemBuilder: (context, index) => items[index],
    onTap: (index) => items[index].onTap?.call(),
    color: color ?? Theme.of(context).colorScheme.surfaceContainerHigh,
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
        M3ESnackbar.show(context, message: 'ページング設定を保存できませんでした。');
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'ページングと一覧表示'),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('一覧設定を読み込めませんでした。')),
        data: (settings) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            _settingsSectionHeading(context, '一覧表示', first: true),
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
      title: galleryAppBarTitle(context, 'ヘルプと使い方'),
      leading: _expressiveBackButton(context),
    ),
    body: ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      children: [
        _settingsGroup(context, [
          _helpItem(
            icon: Icons.link,
            title: '関連ノート',
            body: '関連見出し内の Markdown link / image link と Obsidian wikilink / embed は、Vault内のノートを指していればタップして詳細を開けます。',
          ),
          _helpItem(
            icon: Icons.tune,
            title: 'ノート構造',
            body: '項目順の説明アイコンで読み取り方法を確認できます。見出しやFrontmatterキーなど変更できる設定は、各項目の設定ボタンにまとめています。',
          ),
          _helpItem(
            icon: Icons.search,
            title: '検索と絞り込み',
            body:
                'ノート検索は名前・パスに加えて #タグ、-#タグ、&#タグに対応します。下のタグ検索は絞り込み候補の表示だけを絞ります。',
          ),
        ]),
      ],
    ),
  );
}

M3EListItem _helpItem(
  {
  required IconData icon,
  required String title,
  required String body,
}) => M3EListItem(
  headline: title,
  supportingText: body,
  leading: Icon(icon),
);

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
        title: galleryAppBarTitle(context, 'このアプリについて'),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _AboutHero(version: _version),
          _settingsSectionHeading(context, '情報'),
          _settingsGroup(context, [
            M3EListItem(
              headline: 'オープンソースライセンス',
              leading: const Icon(Icons.article_outlined),
              onTap: () => showLicensePage(
                context: context,
                applicationName: 'Kaede Gallery',
                applicationVersion: _version,
              ),
            ),
            M3EListItem(
              headline: 'GitHub リポジトリ',
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
          M3ESnackbar.show(context, message: '設定をJSONで保存しました。');
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
            message: 'Vault 内には保存できません。別の保存先を選択してください。',
          );
        }
        return;
      }
      await File(outputPath.path).writeAsString('$json\n', flush: true);
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定をJSONで保存しました。');
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定をJSONに保存できませんでした。');
      }
    } on FormatException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定を読み取れないため、JSONに保存できませんでした。');
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: error.code == 'VAULT_READ_ONLY'
              ? 'Vault 内には保存できません。別の保存先を選択してください。'
              : '設定ファイルの保存先を開けませんでした。',
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
        throw const FormatException('設定JSONが大きすぎるか、通常ファイルではありません。');
      }
      final decoded = jsonDecode(await file.readAsString());
      if (decoded is! Map<String, dynamic> || decoded['version'] != 1) {
        throw const FormatException('未対応の設定JSONです。');
      }
      final appearanceValue = decoded['appearance'];
      final tagsValue = decoded['tags'];
      if (appearanceValue is! Map<String, dynamic> ||
          tagsValue is! Map<String, dynamic>) {
        throw const FormatException('設定JSONに必要な項目がありません。');
      }
      final appearance = GalleryAppearance.fromJson(appearanceValue);
      final tags = GalleryTagSettings.fromJson(tagsValue);
      await ref.read(galleryTagSettingsProvider.notifier).saveSettings(tags);
      await ref
          .read(galleryAppearanceProvider.notifier)
          .updateAppearance(appearance);
      await ref.read(vaultSessionProvider.notifier).rescan();
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定をインポートしました。');
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定ファイルを読み書きできませんでした。');
      }
    } on FormatException catch (error) {
      if (context.mounted) {
        M3ESnackbar.show(context, message: error.message);
      }
    } on PlatformException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定ファイルを開けませんでした。');
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定が初期化されていません。');
      }
    }
  }

  Future<void> _resetSettings(BuildContext context, WidgetRef ref) async {
    final reset = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: '設定をリセット',
        content: const Text('外観・タグ・ノート構造の設定をすべて既定値に戻します。Vault内のノートは変更しません。'),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('キャンセル'),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('既定値に戻す'),
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
        M3ESnackbar.show(context, message: 'すべての設定を既定値に戻しました。');
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '設定をリセットできませんでした。');
      }
    }
  }

  Future<void> _forgetVault(
    BuildContext context,
    WidgetRef ref,
    VaultSession session,
  ) async {
    final isSafVault = session.vaultPath.startsWith('content://');
    final confirmed = await M3EDialog.show<bool>(
      context,
      dialog: M3EDialog(
        title: 'この Vault を忘れる',
        content: Text(
          '${vaultDisplayName(session.vaultPath)} のアプリ内インデックス、キャッシュ、選択情報'
          '${isSafVault ? 'と読み取りアクセス権' : ''}を削除します。'
          'Vault 内のファイルと外観・タグなどのアプリ設定は変更しません。',
        ),
        actions: [
          M3EButton.text(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('キャンセル'),
          ),
          M3EButton.filled(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('忘れる'),
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
          message: 'Vault のアプリ内データを削除できませんでした。もう一度お試しください。',
        );
      }
    } on StateError {
      if (context.mounted) {
        M3ESnackbar.show(
          context,
          message: 'Vault のアプリ内データを削除できませんでした。もう一度お試しください。',
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(vaultSessionProvider).asData?.value;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'アプリ'),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsGroup(context, [
            M3EListItem(
              headline: '設定をJSONでエクスポート',
              supportingText: '外観とタグ設定を1つのJSONファイルに保存します',
              leading: Icon(Icons.save_alt),
              onTap: () => _exportSettings(context, ref),
            ),
            M3EListItem(
              headline: '設定をJSONからインポート',
              supportingText: '以前にエクスポートした設定を読み込みます',
              leading: Icon(Icons.file_open_outlined),
              onTap: () => _importSettings(context, ref),
            ),
            M3EListItem(
              headline: '設定をリセット',
              supportingText: '外観・ノート設定を既定値に戻します',
              leading: const Icon(Icons.settings_backup_restore_outlined),
              onTap: () => _resetSettings(context, ref),
            ),
            if (session != null)
              M3EListItem(
                headline: 'この Vault を忘れる',
                supportingText: 'Vault の選択情報、インデックスとキャッシュを削除します',
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
                        semanticLabel: 'Kaede Gallery のアイコン',
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
                      'バージョン $version',
                      style: text.labelLarge?.copyWith(color: scheme.onPrimary),
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  'Obsidian Vault 内のノートとメディアを閲覧するオフラインギャラリーです。',
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
