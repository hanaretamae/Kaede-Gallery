part of 'gallery_screen.dart';

class _TagRulesSettingsScreen extends ConsumerWidget {
  const _TagRulesSettingsScreen();

  Future<String?> _askPrefix(BuildContext context, String title) =>
      M3EDialog.show<String>(context, dialog: _TagPrefixDialog(title: title));

  Future<TagColorRule?> _askColor(
    BuildContext context, {
    TagColorRule? initial,
  }) => M3EDialog.show<TagColorRule>(
    context,
    dialog: _TagColorRuleDialog(initial: initial),
  );

  Future<void> _save(
    BuildContext context,
    WidgetRef ref,
    GalleryTagSettings settings, {
    bool rescan = false,
  }) async {
    try {
      await ref
          .read(galleryTagSettingsProvider.notifier)
          .saveSettings(settings);
      if (rescan) {
        await ref.read(vaultSessionProvider.notifier).rescan();
      }
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: 'タグ設定を保存できませんでした。');
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsState = ref.watch(galleryTagSettingsProvider);
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'タグ設定'),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('タグ設定を読み込めませんでした。')),
        data: (settings) => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text('ギャラリー対象タグ', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            _GalleryTagPrefixCard(
              prefixes: settings.noteStructure.galleryTagPrefixes,
              onChanged: (prefixes) => _save(
                context,
                ref,
                settings.copyWith(
                  noteStructure: settings.noteStructure.copyWith(
                    galleryTagPrefixes: prefixes,
                  ),
                ),
                rescan: true,
              ),
            ),
            Text('フィルターに含めるタグ', style: Theme.of(context).textTheme.titleLarge),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                'タグ絞り込みに含めるパスです。* はすべてを表します。ノートの索引は維持し、フィルター項目の表示だけを制御します。',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            _TagPrefixList(
              prefixes: settings.includedPrefixes,
              emptyText: 'フィルター対象のタグはありません',
              onAdd: () async {
                final prefix = await _askPrefix(context, 'フィルター対象のタグを追加');
                if (!context.mounted) return;
                if (prefix == null || prefix.isEmpty) return;
                if (settings.includedPrefixes.contains(prefix)) return;
                await _save(
                  context,
                  ref,
                  settings.copyWith(
                    includedPrefixes: [...settings.includedPrefixes, prefix],
                  ),
                );
              },
              onRemove: (prefix) => _save(
                context,
                ref,
                settings.copyWith(
                  includedPrefixes: settings.includedPrefixes
                      .where((item) => item != prefix)
                      .toList(growable: false),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text('非表示にするタグ', style: Theme.of(context).textTheme.titleLarge),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                '一致したタグは絞り込み一覧とノート詳細のタグ表示から隠します。',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            _TagPrefixList(
              prefixes: settings.hiddenPrefixes,
              emptyText: '非表示タグはありません',
              onAdd: () async {
                final prefix = await _askPrefix(context, '非表示タグを追加');
                if (!context.mounted) return;
                if (prefix == null || prefix.isEmpty) return;
                if (settings.hiddenPrefixes.contains(prefix)) return;
                await _save(
                  context,
                  ref,
                  settings.copyWith(
                    hiddenPrefixes: [...settings.hiddenPrefixes, prefix],
                  ),
                );
              },
              onRemove: (prefix) => _save(
                context,
                ref,
                settings.copyWith(
                  hiddenPrefixes: settings.hiddenPrefixes
                      .where((item) => item != prefix)
                      .toList(growable: false),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text('タグの色', style: Theme.of(context).textTheme.titleLarge),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                'パスの一致範囲に色を適用します。より長いパスの設定が優先されます。',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            _settingsGroup(context, [
              for (final rule in settings.colors)
                M3EListItem(
                  headline: rule.prefix,
                  supportingText:
                      '#${rule.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
                  leading: Semantics(
                    label:
                        '色 #${rule.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
                    child: SizedBox.square(
                      dimension: 28,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: Color(rule.color),
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      M3EIconButton(
                        variant: M3EIconButtonVariant.standard,
                        tooltip: '色を編集',
                        icon: const Icon(Icons.edit_outlined),
                        onPressed: () async {
                          final updated = await _askColor(
                            context,
                            initial: rule,
                          );
                          if (!context.mounted) return;
                          if (updated == null) return;
                          final colors =
                              settings.colors
                                  .where((item) => item.prefix != rule.prefix)
                                  .toList(growable: true)
                                ..add(updated);
                          await _save(
                            context,
                            ref,
                            settings.copyWith(colors: colors),
                          );
                        },
                      ),
                      M3EIconButton(
                        variant: M3EIconButtonVariant.standard,
                        tooltip: '色設定を削除',
                        icon: const Icon(Icons.delete_outline),
                        onPressed: () => _save(
                          context,
                          ref,
                          settings.copyWith(
                            colors: settings.colors
                                .where((item) => item.prefix != rule.prefix)
                                .toList(growable: false),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              M3EListItem(
                headline: '色設定を追加',
                leading: Icon(Icons.add, color: scheme.primary),
                onTap: () async {
                  final added = await _askColor(context);
                  if (!context.mounted) return;
                  if (added == null) return;
                  final colors =
                      settings.colors
                          .where((item) => item.prefix != added.prefix)
                          .toList(growable: true)
                        ..add(added);
                  await _save(context, ref, settings.copyWith(colors: colors));
                },
              ),
            ]),
          ],
        ),
        skipLoadingOnReload: true,
      ),
    );
  }
}

class _TagPrefixList extends StatelessWidget {
  const _TagPrefixList({
    required this.prefixes,
    required this.emptyText,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> prefixes;
  final String emptyText;
  final VoidCallback onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return _settingsGroup(context, [
      if (prefixes.isEmpty)
        M3EListItem(headline: emptyText)
      else
        for (final prefix in prefixes)
          M3EListItem(
            headline: prefix == '*' ? 'すべてのタグ (*)' : prefix,
            trailing: M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: '削除',
              icon: const Icon(Icons.delete_outline),
              onPressed: () => onRemove(prefix),
            ),
          ),
      M3EListItem(
        headline: 'パスを追加',
        leading: Icon(Icons.add, color: scheme.primary),
        onTap: onAdd,
      ),
    ]);
  }
}

class _TagPrefixDialog extends StatefulWidget {
  const _TagPrefixDialog({
    required this.title,
    this.label = 'タグパス',
    this.hint = 'source/service',
    this.normalizeTagPrefix = true,
  });

  final String title;
  final String label;
  final String hint;
  final bool normalizeTagPrefix;

  @override
  State<_TagPrefixDialog> createState() => _TagPrefixDialogState();
}

class _TagPrefixDialogState extends State<_TagPrefixDialog> {
  late final TextEditingController controller;

  @override
  void initState() {
    super.initState();
    controller = TextEditingController();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => M3EDialog(
    title: widget.title,
    content: M3ETextField(
      controller: controller,
      autofocus: true,
      label: widget.label,
      placeholder: widget.hint,
      variant: M3ETextFieldVariant.outlined,
      onSubmitted: (_) => Navigator.of(context).pop(_normalizedPrefix),
    ),
    actions: [
      M3EButton.text(
        onPressed: () => Navigator.of(context).pop(),
        child: const Text('キャンセル'),
      ),
      M3EButton.filled(
        onPressed: _normalizedPrefix.isEmpty
            ? null
            : () => Navigator.of(context).pop(_normalizedPrefix),
        child: const Text('追加'),
      ),
    ],
  );

  String get _normalizedPrefix {
    final value = controller.text.trim();
    if (!widget.normalizeTagPrefix) return value;
    return value
        .replaceFirst(RegExp(r'^#'), '')
        .replaceFirst(RegExp(r'/+$'), '');
  }
}

class _TagColorRuleDialog extends StatefulWidget {
  const _TagColorRuleDialog({this.initial});

  final TagColorRule? initial;

  @override
  State<_TagColorRuleDialog> createState() => _TagColorRuleDialogState();
}

class _TagColorRuleDialogState extends State<_TagColorRuleDialog> {
  late final TextEditingController prefixController;
  late final TextEditingController colorController;
  String? validationError;

  @override
  void initState() {
    super.initState();
    prefixController = TextEditingController(text: widget.initial?.prefix);
    colorController = TextEditingController(
      text: widget.initial == null
          ? '#FFFFFF'
          : '#${widget.initial!.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
    );
  }

  @override
  void dispose() {
    prefixController.dispose();
    colorController.dispose();
    super.dispose();
  }

  void _submit() {
    final prefix = prefixController.text.trim().replaceFirst(
      RegExp(r'/+$'),
      '',
    );
    final colorText = colorController.text.trim();
    if (prefix.isEmpty || !RegExp(r'^#[0-9a-fA-F]{6}$').hasMatch(colorText)) {
      setState(() => validationError = 'タグパスと #RRGGBB 形式の色を入力してください。');
      return;
    }
    Navigator.of(context).pop(
      TagColorRule(
        prefix,
        int.parse(colorText.substring(1), radix: 16) | 0xFF000000,
      ),
    );
  }

  @override
  Widget build(BuildContext context) => M3EDialog(
    title: widget.initial == null ? 'タグ色を追加' : 'タグ色を編集',
    content: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        M3ETextField(
          controller: prefixController,
          autofocus: true,
          label: 'タグパス',
          placeholder: 'source/type',
          variant: M3ETextFieldVariant.outlined,
        ),
        const SizedBox(height: 12),
        M3ETextField(
          controller: colorController,
          textCapitalization: TextCapitalization.characters,
          label: '色',
          placeholder: '#RRGGBB',
          errorText: validationError,
          variant: M3ETextFieldVariant.outlined,
        ),
      ],
    ),
    actions: [
      M3EButton.text(
        onPressed: () => Navigator.of(context).pop(),
        child: const Text('キャンセル'),
      ),
      M3EButton.filled(onPressed: _submit, child: const Text('保存')),
    ],
  );
}
