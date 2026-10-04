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
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'タグ設定'),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('タグ設定を読み込めませんでした。')),
        data: (settings) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            _settingsSectionHeading(context, 'ギャラリー対象タグ', first: true),
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
            _settingsSectionHeading(context, 'フィルターに含めるタグ'),
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
              onReset: () => _save(
                context,
                ref,
                settings.copyWith(
                  includedPrefixes: GalleryTagSettings.defaultIncludedPrefixes,
                ),
              ),
            ),
            _settingsSectionHeading(context, 'フィルターのカテゴリー'),
            _TagCategoryRuleList(
              settings: settings.categories,
              onChanged: (categories) => _save(
                context,
                ref,
                settings.copyWith(categories: categories),
              ),
            ),
            _settingsSectionHeading(context, '非表示にするタグ'),
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
              onReset: () => _save(
                context,
                ref,
                settings.copyWith(
                  hiddenPrefixes: GalleryTagSettings.defaultHiddenPrefixes,
                ),
              ),
            ),
            _settingsSectionHeading(context, 'タグの色'),
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
                leading: Icon(Icons.add),
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
              M3EListItem(
                headline: '初期設定に戻す',
                leading: Icon(Icons.restore),
                onTap: () => _save(
                  context,
                  ref,
                  settings.copyWith(
                    colors: GalleryTagSettings.defaultTagColors,
                  ),
                ),
              ),
            ]),
          ],
        ),
        skipLoadingOnReload: true,
      ),
    );
  }
}

class _TagCategoryRuleList extends StatelessWidget {
  const _TagCategoryRuleList({required this.settings, required this.onChanged});

  final GalleryTagCategorySettings settings;
  final ValueChanged<GalleryTagCategorySettings> onChanged;

  Future<GalleryTagCategoryRule?> _ask(
    BuildContext context, {
    GalleryTagCategoryRule? initial,
  }) => M3EDialog.show<GalleryTagCategoryRule>(
    context,
    dialog: _TagCategoryRuleDialog(initial: initial),
  );

  bool _pathTaken(String path, {int? except}) => settings.categories.indexed
      .any((entry) => entry.$1 != except && entry.$2.path == path);

  void _notifyDuplicate(BuildContext context) =>
      M3ESnackbar.show(context, message: '同じパスのカテゴリーがすでにあります。');

  void _replaceRules(List<GalleryTagCategoryRule> rules) =>
      onChanged(settings.copyWith(categories: List.unmodifiable(rules)));

  void _move(int index, int delta) {
    final target = index + delta;
    if (target < 0 || target >= settings.categories.length) return;
    final rules = [...settings.categories];
    rules.insert(target, rules.removeAt(index));
    _replaceRules(rules);
  }

  @override
  Widget build(BuildContext context) {
    final rules = settings.categories;
    final other = settings.other;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _settingsGroup(context, [
          if (rules.isEmpty) const M3EListItem(headline: 'カテゴリーはありません'),
          for (final (index, rule) in rules.indexed)
            M3EListItem(
              headline: rule.name,
              supportingText: rule.splitDeep
                  ? '${rule.path} ・ 3層目以降を分割'
                  : rule.path,
              onTap: () async {
                final updated = await _ask(context, initial: rule);
                if (!context.mounted || updated == null) return;
                if (_pathTaken(updated.path, except: index)) {
                  _notifyDuplicate(context);
                  return;
                }
                _replaceRules([...rules]..[index] = updated);
              },
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: '上へ',
                    icon: const Icon(Icons.arrow_upward),
                    onPressed: index == 0 ? null : () => _move(index, -1),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: '下へ',
                    icon: const Icon(Icons.arrow_downward),
                    onPressed: index == rules.length - 1
                        ? null
                        : () => _move(index, 1),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: '削除',
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () => _replaceRules([...rules]..removeAt(index)),
                  ),
                ],
              ),
            ),
          if (rules.length < GalleryTagCategorySettings.maxCategories)
            M3EListItem(
              headline: 'カテゴリーを追加',
              leading: Icon(Icons.add),
              onTap: () async {
                final added = await _ask(context);
                if (!context.mounted || added == null) return;
                if (_pathTaken(added.path)) {
                  _notifyDuplicate(context);
                  return;
                }
                _replaceRules([...rules, added]);
              },
            ),
          M3EListItem(
            headline: '初期設定に戻す',
            leading: Icon(Icons.restore),
            onTap: () => onChanged(
              settings.copyWith(
                categories: GalleryTagCategorySettings.defaultCategories,
              ),
            ),
          ),
        ]),
        const SizedBox(height: 12),
        _settingsSectionHeading(context, 'その他カテゴリー'),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: 'その他カテゴリーを表示',
            value: other.enabled,
            onChanged: (value) => onChanged(
              settings.copyWith(other: other.copyWith(enabled: value)),
            ),
          ),
          M3EListItem(
            headline: 'カテゴリー名',
            supportingText: other.name,
            leading: Icon(Icons.edit_outlined),
            onTap: () async {
              final name = await M3EDialog.show<String>(
                context,
                dialog: const _TagPrefixDialog(
                  title: 'その他カテゴリーの名前',
                  label: 'カテゴリー名',
                  hint: 'その他',
                  normalizeTagPrefix: false,
                  confirmLabel: '保存',
                ),
              );
              if (!context.mounted || name == null) return;
              onChanged(settings.copyWith(other: other.copyWith(name: name)));
            },
          ),
          _expressiveSwitchTile(
            title: '3層目以降のタグを自動で分ける',
            subtitle: '親タグごとに別のカテゴリーとして表示します。',
            value: other.splitDeep,
            onChanged: (value) => onChanged(
              settings.copyWith(other: other.copyWith(splitDeep: value)),
            ),
          ),
          M3EListItem(
            headline: '初期設定に戻す',
            leading: Icon(Icons.restore),
            onTap: () => onChanged(
              settings.copyWith(other: const GalleryOtherCategorySettings()),
            ),
          ),
        ]),
      ],
    );
  }
}

class _TagCategoryRuleDialog extends StatefulWidget {
  const _TagCategoryRuleDialog({this.initial});

  final GalleryTagCategoryRule? initial;

  @override
  State<_TagCategoryRuleDialog> createState() => _TagCategoryRuleDialogState();
}

class _TagCategoryRuleDialogState extends State<_TagCategoryRuleDialog> {
  late final TextEditingController nameController;
  late final TextEditingController pathController;
  late bool splitDeep;
  String? validationError;

  @override
  void initState() {
    super.initState();
    nameController = TextEditingController(text: widget.initial?.name);
    pathController = TextEditingController(text: widget.initial?.path);
    splitDeep = widget.initial?.splitDeep ?? false;
  }

  @override
  void dispose() {
    nameController.dispose();
    pathController.dispose();
    super.dispose();
  }

  void _submit() {
    final name = nameController.text.trim();
    final path = pathController.text
        .trim()
        .replaceFirst(RegExp(r'^#'), '')
        .replaceFirst(RegExp(r'/+$'), '');
    if (name.isEmpty ||
        utf8.encode(name).length > 128 ||
        !GalleryTagCategoryRule.isValidCategoryPath(path)) {
      setState(
        () => validationError =
            '名前と、source/art または source/count/* 形式のパスを入力してください。',
      );
      return;
    }
    Navigator.of(context).pop(
      GalleryTagCategoryRule(
        name,
        path,
        splitDeep: splitDeep && path.endsWith('*'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isWildcard = pathController.text.trim().endsWith('*');
    return M3EDialog(
      title: widget.initial == null ? 'カテゴリーを追加' : 'カテゴリーを編集',
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          M3ETextField(
            controller: nameController,
            autofocus: true,
            label: 'カテゴリー名',
            placeholder: '人数',
            variant: M3ETextFieldVariant.outlined,
          ),
          const SizedBox(height: 12),
          M3ETextField(
            controller: pathController,
            label: 'タグパス',
            placeholder: 'source/count/*',
            errorText: validationError,
            variant: M3ETextFieldVariant.outlined,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Expanded(child: Text('3層目以降のタグを自動で分ける')),
              M3ESwitch(
                value: splitDeep && isWildcard,
                onChanged: isWildcard
                    ? (value) => setState(() => splitDeep = value)
                    : null,
                semanticLabel: '3層目以降のタグを自動で分ける',
              ),
            ],
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
}

class _TagPrefixList extends StatelessWidget {
  const _TagPrefixList({
    required this.prefixes,
    required this.emptyText,
    required this.onAdd,
    required this.onRemove,
    required this.onReset,
  });

  final VoidCallback onReset;
  final List<String> prefixes;
  final String emptyText;
  final VoidCallback onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
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
        leading: Icon(Icons.add),
        onTap: onAdd,
      ),
      M3EListItem(
        headline: '初期設定に戻す',
        leading: Icon(Icons.restore),
        onTap: onReset,
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
    this.confirmLabel = '追加',
  });

  final String title;
  final String label;
  final String hint;
  final bool normalizeTagPrefix;
  final String confirmLabel;

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
      onChanged: (_) => setState(() {}),
      onSubmitted: (_) {
        if (_normalizedPrefix.isNotEmpty) {
          Navigator.of(context).pop(_normalizedPrefix);
        }
      },
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
        child: Text(widget.confirmLabel),
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
