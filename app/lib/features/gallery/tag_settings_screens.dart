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
        M3ESnackbar.show(
          context,
          message: tr('タグ設定を保存できませんでした。', 'Couldn\'t save the tag settings.'),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsState = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, tr('タグ設定', 'Tag settings')),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => Center(
          child: Text(
            tr('タグ設定を読み込めませんでした。', 'Couldn\'t load the tag settings.'),
          ),
        ),
        data: (settings) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            _settingsSectionHeading(
              context,
              tr('ギャラリー対象タグ', 'Gallery target tags'),
              first: true,
            ),
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
            _settingsSectionHeading(
              context,
              tr('フィルターに含めるタグ', 'Tags included in filters'),
            ),
            _TagPrefixList(
              prefixes: settings.includedPrefixes,
              emptyText: tr('フィルター対象のタグはありません', 'There are no filter tags.'),
              onAdd: () async {
                final prefix = await _askPrefix(
                  context,
                  tr('フィルター対象のタグを追加', 'Add a filter tag'),
                );
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
            _settingsSectionHeading(
              context,
              tr('フィルターのカテゴリー', 'Filter categories'),
            ),
            _TagCategoryRuleList(
              settings: settings.categories,
              onChanged: (categories) => _save(
                context,
                ref,
                settings.copyWith(categories: categories),
              ),
            ),
            _settingsSectionHeading(context, tr('非表示にするタグ', 'Hidden tags')),
            _TagPrefixList(
              prefixes: settings.hiddenPrefixes,
              emptyText: tr('非表示タグはありません', 'There are no hidden tags.'),
              onAdd: () async {
                final prefix = await _askPrefix(
                  context,
                  tr('非表示タグを追加', 'Add a hidden tag'),
                );
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
            _settingsSectionHeading(context, tr('タグの色', 'Tag colors')),
            _settingsGroup(context, [
              for (final rule in settings.colors)
                M3EListItem(
                  headline: rule.prefix,
                  supportingText:
                      '#${rule.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
                  leading: Semantics(
                    label: tr(
                      '色 #${rule.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
                      'Color #${rule.color.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}',
                    ),
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
                        tooltip: tr('色を編集', 'Edit color'),
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
                        tooltip: tr('色設定を削除', 'Delete color rule'),
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
                headline: tr('色設定を追加', 'Add color rule'),
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
                headline: tr('初期設定に戻す', 'Reset to defaults'),
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

  void _notifyDuplicate(BuildContext context) => M3ESnackbar.show(
    context,
    message: tr(
      '同じパスのカテゴリーがすでにあります。',
      'A category with the same path already exists.',
    ),
  );

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
          if (rules.isEmpty)
            M3EListItem(headline: tr('カテゴリーはありません', 'There are no categories')),
          for (final (index, rule) in rules.indexed)
            M3EListItem(
              headline: rule.name,
              supportingText: rule.splitDeep
                  ? tr(
                      '${rule.path} ・ 3層目以降を分割',
                      '${rule.path} · split deeper levels',
                    )
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
                    tooltip: tr('上へ', 'Move up'),
                    icon: const Icon(Icons.arrow_upward),
                    onPressed: index == 0 ? null : () => _move(index, -1),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: tr('下へ', 'Move down'),
                    icon: const Icon(Icons.arrow_downward),
                    onPressed: index == rules.length - 1
                        ? null
                        : () => _move(index, 1),
                  ),
                  M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: tr('削除', 'Delete'),
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () => _replaceRules([...rules]..removeAt(index)),
                  ),
                ],
              ),
            ),
          if (rules.length < GalleryTagCategorySettings.maxCategories)
            M3EListItem(
              headline: tr('カテゴリーを追加', 'Add category'),
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
            headline: tr('初期設定に戻す', 'Reset to defaults'),
            leading: Icon(Icons.restore),
            onTap: () => onChanged(
              settings.copyWith(
                categories: GalleryTagCategorySettings.defaultCategories,
              ),
            ),
          ),
        ]),
        const SizedBox(height: 12),
        _settingsSectionHeading(context, tr('その他カテゴリー', 'Other category')),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: tr('その他カテゴリーを表示', 'Show other category'),
            value: other.enabled,
            onChanged: (value) => onChanged(
              settings.copyWith(other: other.copyWith(enabled: value)),
            ),
          ),
          M3EListItem(
            headline: tr('カテゴリー名', 'Category name'),
            supportingText: other.name,
            leading: Icon(Icons.edit_outlined),
            onTap: () async {
              final name = await M3EDialog.show<String>(
                context,
                dialog: _TagPrefixDialog(
                  title: tr('その他カテゴリーの名前', 'Other category name'),
                  label: tr('カテゴリー名', 'Category name'),
                  hint: tr('その他', 'Other'),
                  normalizeTagPrefix: false,
                  confirmLabel: tr('保存', 'Save'),
                ),
              );
              if (!context.mounted || name == null) return;
              onChanged(settings.copyWith(other: other.copyWith(name: name)));
            },
          ),
          _expressiveSwitchTile(
            title: tr(
              '3層目以降のタグを自動で分ける',
              'Automatically split tags after the third level',
            ),
            subtitle: tr(
              '親タグごとに別のカテゴリーとして表示します。',
              'Show them as separate categories for each parent tag.',
            ),
            value: other.splitDeep,
            onChanged: (value) => onChanged(
              settings.copyWith(other: other.copyWith(splitDeep: value)),
            ),
          ),
          M3EListItem(
            headline: tr('初期設定に戻す', 'Reset to defaults'),
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
        () => validationError = tr(
          '名前と、source/art または source/count/* 形式のパスを入力してください。',
          'Enter a name and a path in source/art or source/count/* format.',
        ),
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
      title: widget.initial == null
          ? tr('カテゴリーを追加', 'Add category')
          : tr('カテゴリーを編集', 'Edit category'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          M3ETextField(
            controller: nameController,
            autofocus: true,
            label: tr('カテゴリー名', 'Category name'),
            placeholder: tr('人数', 'People'),
            variant: M3ETextFieldVariant.outlined,
          ),
          const SizedBox(height: 12),
          M3ETextField(
            controller: pathController,
            label: tr('タグパス', 'Tag path'),
            placeholder: 'source/count/*',
            errorText: validationError,
            variant: M3ETextFieldVariant.outlined,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: Text(
                  tr(
                    '3層目以降のタグを自動で分ける',
                    'Automatically split tags after the third level',
                  ),
                ),
              ),
              M3ESwitch(
                value: splitDeep && isWildcard,
                onChanged: isWildcard
                    ? (value) => setState(() => splitDeep = value)
                    : null,
                semanticLabel: tr(
                  '3層目以降のタグを自動で分ける',
                  'Automatically split tags after the third level',
                ),
              ),
            ],
          ),
        ],
      ),
      actions: [
        M3EButton.text(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('キャンセル', 'Cancel')),
        ),
        M3EButton.filled(onPressed: _submit, child: Text(tr('保存', 'Save'))),
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
            headline: prefix == '*' ? tr('すべてのタグ (*)', 'All tags (*)') : prefix,
            trailing: M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: tr('削除', 'Delete'),
              icon: const Icon(Icons.delete_outline),
              onPressed: () => onRemove(prefix),
            ),
          ),
      M3EListItem(
        headline: tr('パスを追加', 'Add path'),
        leading: Icon(Icons.add),
        onTap: onAdd,
      ),
      M3EListItem(
        headline: tr('初期設定に戻す', 'Reset to defaults'),
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
      label: widget.label == 'タグパス' ? tr('タグパス', 'Tag path') : widget.label,
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
        child: Text(tr('キャンセル', 'Cancel')),
      ),
      M3EButton.filled(
        onPressed: _normalizedPrefix.isEmpty
            ? null
            : () => Navigator.of(context).pop(_normalizedPrefix),
        child: Text(
          widget.confirmLabel == '追加' ? tr('追加', 'Add') : widget.confirmLabel,
        ),
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
      setState(
        () => validationError = tr(
          'タグパスと #RRGGBB 形式の色を入力してください。',
          'Enter a tag path and a color in #RRGGBB format.',
        ),
      );
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
    title: widget.initial == null
        ? tr('タグ色を追加', 'Add tag color')
        : tr('タグ色を編集', 'Edit tag color'),
    content: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        M3ETextField(
          controller: prefixController,
          autofocus: true,
          label: tr('タグパス', 'Tag path'),
          placeholder: 'source/type',
          variant: M3ETextFieldVariant.outlined,
        ),
        const SizedBox(height: 12),
        M3ETextField(
          controller: colorController,
          textCapitalization: TextCapitalization.characters,
          label: tr('色', 'Color'),
          placeholder: '#RRGGBB',
          errorText: validationError,
          variant: M3ETextFieldVariant.outlined,
        ),
      ],
    ),
    actions: [
      M3EButton.text(
        onPressed: () => Navigator.of(context).pop(),
        child: Text(tr('キャンセル', 'Cancel')),
      ),
      M3EButton.filled(onPressed: _submit, child: Text(tr('保存', 'Save'))),
    ],
  );
}
