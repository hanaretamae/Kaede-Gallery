part of 'gallery_screen.dart';

class _GallerySettingsScreen extends ConsumerWidget {
  const _GallerySettingsScreen({required this.session});

  final VaultSession session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final warnings = session.scanReport.warnings;
    final galleryTagPrefixes =
        ref
            .watch(galleryTagSettingsProvider)
            .asData
            ?.value
            .noteStructure
            .galleryTagPrefixes ??
        const ['source/'];
    final colorScheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('設定')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsSectionHeading(context, '外観', first: true),
          const _AppearanceSettings(),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.view_agenda_outlined),
              title: const Text('ページングと一覧表示'),
              subtitle: const Text('一覧の読み込み単位や件数表示を設定します'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _PaginationSettingsScreen(),
                ),
              ),
            ),
          ),
          _settingsSectionHeading(context, '保管庫'),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.folder_outlined),
              title: const Text('選択中の Vault'),
              subtitle: Text(vaultDisplayName(session.vaultPath)),
              trailing: IconButton(
                tooltip: 'Vault を切り替え',
                icon: const Icon(Icons.folder_open),
                onPressed: () {
                  Navigator.of(context).pop();
                  ref.read(vaultSessionProvider.notifier).chooseVault();
                },
              ),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: Icon(
                warnings > 0
                    ? Icons.warning_amber_rounded
                    : Icons.check_circle_outline,
                color: warnings > 0
                    ? colorScheme.tertiary
                    : colorScheme.primary,
              ),
              title: const Text('確認できなかった項目'),
              subtitle: Text(
                warnings == 0
                    ? 'ありません'
                    : '$warnings 件。内容を確認できないため、ギャラリー対象かは判定できません。',
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    tooltip: '確認できなかった項目の詳細',
                    icon: const Icon(Icons.info_outline),
                    onPressed: () => showDialog<void>(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: const Text('確認できなかった項目の詳細'),
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
                          TextButton(
                            onPressed: () => Navigator.of(context).pop(),
                            child: const Text('閉じる'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: '再走査',
                    icon: const Icon(Icons.refresh),
                    onPressed: () =>
                        ref.read(vaultSessionProvider.notifier).rescan(),
                  ),
                ],
              ),
            ),
          ),
          _settingsSectionHeading(context, 'ノート'),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.account_tree_outlined),
              title: const Text('ノート構造と表示'),
              subtitle: const Text('ノートの項目順や見出しの読み取り、一覧表示を設定します'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _NoteStructureSettingsScreen(),
                ),
              ),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.label_outline),
              title: const Text('タグ設定'),
              subtitle: const Text('ギャラリー対象タグ、絞り込み、表示色を設定します'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _TagRulesSettingsScreen(),
                ),
              ),
            ),
          ),
          _settingsSectionHeading(context, 'このアプリについて'),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text('情報とライセンス'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _AboutScreen(),
                ),
              ),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.storage_outlined),
              title: const Text('インポート・エクスポート・リセット'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _DataSettingsScreen(),
                ),
              ),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.help_outline),
              title: const Text('ヘルプと使い方'),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (context) => const _GalleryHelpScreen(),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

Widget _settingsSectionHeading(
  BuildContext context,
  String title, {
  bool first = false,
}) => Padding(
  padding: EdgeInsets.fromLTRB(4, first ? 8 : 20, 4, 4),
  child: Text(
    title,
    style: Theme.of(context).textTheme.titleMedium?.copyWith(
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
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('ページング設定を保存できませんでした。')));
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('ページングと一覧表示')),
      body: state.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => const Center(child: Text('一覧設定を読み込めませんでした。')),
        data: (settings) => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text('一覧表示', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
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
      ),
    );
  }
}

class _GalleryHelpScreen extends StatelessWidget {
  const _GalleryHelpScreen();

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('ヘルプと使い方')),
    body: ListView(
      padding: const EdgeInsets.all(20),
      children: const [
        _HelpSection(
          icon: Icons.link,
          title: '関連ノート',
          body: '関連見出し内の Markdown link / image link と Obsidian wikilink / embed は、Vault内のノートを指していればタップして詳細を開けます。',
        ),
        _HelpSection(
          icon: Icons.tune,
          title: 'ノート構造',
          body: '項目順の説明アイコンで読み取り方法を確認できます。見出しやFrontmatterキーなど変更できる設定は、各項目の設定ボタンにまとめています。',
        ),
        _HelpSection(
          icon: Icons.search,
          title: '検索と絞り込み',
          body: 'ノート検索は名前・パスに加えて #タグ、-#タグ、&#タグに対応します。下のタグ検索は絞り込み候補の表示だけを絞ります。',
        ),
      ],
    ),
  );
}

class _HelpSection extends StatelessWidget {
  const _HelpSection({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 12),
    child: ListTile(
      contentPadding: const EdgeInsets.all(16),
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      title: Text(title),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Text(body),
      ),
      isThreeLine: true,
    ),
  );
}

class _AboutScreen extends StatelessWidget {
  const _AboutScreen();

  static const _repositoryUrl = 'https://github.com/hanaretamae/Kaede-Gallery';
  static const _version = String.fromEnvironment(
    'FLUTTER_BUILD_NAME',
    defaultValue: '1.0.0',
  );

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('このアプリについて')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            color: colorScheme.surfaceContainerLow,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Icon(
                    Icons.photo_library_outlined,
                    size: 48,
                    color: colorScheme.primary,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Kaede Gallery',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 4),
                  Text('バージョン $_version'),
                  const SizedBox(height: 16),
                  const Text(
                    'Obsidian Vault 内のノートとメディアを閲覧するオフラインギャラリーです。',
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text('情報', style: Theme.of(context).textTheme.titleLarge),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.info_outline),
                  title: const Text('アプリ情報'),
                  onTap: () => showAboutDialog(
                    context: context,
                    applicationName: 'Kaede Gallery',
                    applicationVersion: _version,
                    applicationLegalese: 'オフラインで動作する Obsidian Vault ギャラリー',
                  ),
                ),
                ListTile(
                  leading: const Icon(Icons.article_outlined),
                  title: const Text('オープンソースライセンス'),
                  onTap: () => showLicensePage(
                    context: context,
                    applicationName: 'Kaede Gallery',
                    applicationVersion: _version,
                  ),
                ),
                ListTile(
                  leading: const Icon(Icons.open_in_new),
                  title: const Text('GitHub リポジトリ'),
                  subtitle: const Text(_repositoryUrl),
                  onTap: () =>
                      _openExternalUri(context, Uri.parse(_repositoryUrl)),
                ),
              ],
            ),
          ),
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
          ScaffoldMessenger.of(context)
              .showSnackBar(const SnackBar(content: Text('設定をJSONで保存しました。')));
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
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Vault 内には保存できません。別の保存先を選択してください。')),
          );
        }
        return;
      }
      await File(outputPath.path).writeAsString('$json\n', flush: true);
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定をJSONで保存しました。')));
      }
    } on FileSystemException {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定をJSONに保存できませんでした。')));
      }
    } on FormatException {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('設定を読み取れないため、JSONに保存できませんでした。')),
        );
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              error.code == 'VAULT_READ_ONLY'
                  ? 'Vault 内には保存できません。別の保存先を選択してください。'
                  : '設定ファイルの保存先を開けませんでした。',
            ),
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
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定をインポートしました。')));
      }
    } on FileSystemException {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('設定ファイルを読み書きできませんでした。')));
      }
    } on FormatException catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(error.message)));
      }
    } on PlatformException {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定ファイルを開けませんでした。')));
      }
    } on StateError {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定が初期化されていません。')));
      }
    }
  }

  Future<void> _resetSettings(BuildContext context, WidgetRef ref) async {
    final reset = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('設定をリセット'),
        content: const Text('外観・タグ・ノート構造の設定をすべて既定値に戻します。Vault内のノートは変更しません。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('キャンセル'),
          ),
          FilledButton(
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
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('すべての設定を既定値に戻しました。')));
      }
    } on FileSystemException {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('設定をリセットできませんでした。')));
      }
    }
  }

  Future<void> _forgetVault(
    BuildContext context,
    WidgetRef ref,
    VaultSession session,
  ) async {
    final isSafVault = session.vaultPath.startsWith('content://');
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('この Vault を忘れる'),
        content: Text(
          '${vaultDisplayName(session.vaultPath)} のアプリ内インデックス、キャッシュ、選択情報'
          '${isSafVault ? 'と読み取りアクセス権' : ''}を削除します。'
          'Vault 内のファイルと外観・タグなどのアプリ設定は変更しません。',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('キャンセル'),
          ),
          FilledButton(
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
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Vault のアプリ内データを削除しました。')));
      }
    } on Exception {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Vault のアプリ内データを削除できませんでした。もう一度お試しください。'),
          ),
        );
      }
    } on StateError {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Vault のアプリ内データを削除できませんでした。もう一度お試しください。'),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colorScheme = Theme.of(context).colorScheme;
    final session = ref.watch(vaultSessionProvider).asData?.value;
    return Scaffold(
      appBar: AppBar(title: const Text('アプリ')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.save_alt),
              title: const Text('設定をJSONでエクスポート'),
              subtitle: const Text('外観とタグ設定を1つのJSONファイルに保存します'),
              onTap: () => _exportSettings(context, ref),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.file_open_outlined),
              title: const Text('設定をJSONからインポート'),
              subtitle: const Text('以前にエクスポートした設定を読み込みます'),
              onTap: () => _importSettings(context, ref),
            ),
          ),
          Card(
            color: colorScheme.surfaceContainerLow,
            child: ListTile(
              leading: const Icon(Icons.settings_backup_restore_outlined),
              title: const Text('設定をリセット'),
              subtitle: const Text('外観・ノート設定を既定値に戻します'),
              onTap: () => _resetSettings(context, ref),
            ),
          ),
          if (session != null)
            Card(
              color: colorScheme.surfaceContainerLow,
              child: ListTile(
                leading: const Icon(Icons.delete_outline),
                title: const Text('この Vault を忘れる'),
                subtitle: const Text('Vault の選択情報、インデックスとキャッシュを削除します'),
                onTap: () => _forgetVault(context, ref, session),
              ),
            ),
        ],
      ),
    );
  }
}
