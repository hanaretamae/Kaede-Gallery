part of 'gallery_screen.dart';

class _NoteStructureSettingsScreen extends ConsumerWidget {
  const _NoteStructureSettingsScreen();

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
        M3ESnackbar.show(context, message: 'ノート構造の設定を保存できませんでした。');
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'ノート構造'),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('タグ設定を読み込めませんでした。')),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            key: const ValueKey('note-structure-settings-list'),
            padding: const EdgeInsets.all(20),
            children: [
              Text(
                'ノート詳細のブロック順序',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 12),
              _BlockOrderCard(
                blockOrder: structure.blockOrder,
                hiddenBlocks: structure.hiddenBlocks,
                onReorder: (order) => _save(
                  context,
                  ref,
                  settings.copyWith(
                    noteStructure: structure.copyWith(blockOrder: order),
                  ),
                ),
                onToggleVisibility: (block, visible) {
                  final hidden = [...structure.hiddenBlocks];
                  if (visible) {
                    hidden.remove(block);
                  } else if (!hidden.contains(block)) {
                    hidden.add(block);
                  }
                  _save(
                    context,
                    ref,
                    settings.copyWith(
                      noteStructure: structure.copyWith(hiddenBlocks: hidden),
                    ),
                  );
                },
                onConfigure: (block) => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (context) =>
                        _NoteBlockSettingsScreen(block: block),
                  ),
                ),
                onReset: () => _save(
                  context,
                  ref,
                  settings.copyWith(
                    noteStructure: structure.copyWith(
                      blockOrder:
                          GalleryNoteStructureSettings.defaultBlockOrder,
                      hiddenBlocks: const [],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              _settingsPanel(
                context: context,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Markdown / Wikilink のノート解決',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      ExpressiveMaterialScope(
                        theme: Theme.of(context),
                        child: M3EButtonGroup(
                          semanticLabel: 'ノートリンクの解決方法',
                          type: M3EButtonGroupType.connected,
                          style: M3EButtonStyle.tonal,
                          decoration: galleryChoiceButtonDecoration(scheme),
                          selectedIndex: GalleryLinkResolution.values.indexOf(
                            structure.linkResolution,
                          ),
                          selectionRequired: true,
                          onSelectedIndexChanged: (index) {
                            if (index == null) return;
                            _save(
                              context,
                              ref,
                              settings.copyWith(
                                noteStructure: structure.copyWith(
                                  linkResolution:
                                      GalleryLinkResolution.values[index],
                                ),
                              ),
                            );
                          },
                          actions: const [
                            M3EButtonGroupAction(label: Text('最短')),
                            M3EButtonGroupAction(label: Text('相対')),
                            M3EButtonGroupAction(label: Text('絶対')),
                          ],
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        '最短はVault内の同名ノートから参照元に近いものを選びます。相対は現在のノート位置を基準にし、絶対はVaultのルートを基準にします。Vault外へ解決されるリンクは無視します。',
                      ),
                      _ResetDefaultsButton(
                        onPressed: () => _save(
                          context,
                          ref,
                          settings.copyWith(
                            noteStructure: structure.copyWith(
                              linkResolution:
                                  const GalleryNoteStructureSettings()
                                      .linkResolution,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              _settingsGroup(context, [
                M3EListItem(
                  headline: '架空のノート例を見る・コピー',
                  supportingText: '設定中の項目順を反映した表示例も確認できます',
                  leading: const Icon(Icons.content_copy_outlined),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (context) => const _FictionalNoteExampleScreen(),
                    ),
                  ),
                ),
              ]),
            ],
          );
        },
        skipLoadingOnReload: true,
      ),
    );
  }
}

class _NoteBlockSettingsScreen extends ConsumerWidget {
  const _NoteBlockSettingsScreen({required this.block});

  final GalleryNoteBlock block;

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
        M3ESnackbar.show(context, message: 'ノート構造の設定を保存できませんでした。');
      }
    }
  }

  Future<String?> _askHeading(BuildContext context) => M3EDialog.show<String>(
    context,
    dialog: _TagPrefixDialog(
      title: '${block.label}の見出しを追加',
      label: '見出し名',
      hint: '制作メモ',
      normalizeTagPrefix: false,
    ),
  );

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsState = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, '${block.label}の設定'),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('ノート設定を読み込めませんでした。')),
        data: (settings) {
          final structure = settings.noteStructure;
          final headings = switch (block) {
            GalleryNoteBlock.memo => structure.memoHeadings,
            GalleryNoteBlock.related => structure.relatedHeadings,
            GalleryNoteBlock.postTextEnd => structure.postTextEndHeadings,
            _ => const <String>[],
          };
          void updateHeadings(List<String> updated) {
            final next = switch (block) {
              GalleryNoteBlock.memo => structure.copyWith(
                memoHeadings: updated,
              ),
              GalleryNoteBlock.related => structure.copyWith(
                relatedHeadings: updated,
              ),
              GalleryNoteBlock.postTextEnd => structure.copyWith(
                postTextEndHeadings: updated,
              ),
              _ => structure,
            };
            _save(context, ref, settings.copyWith(noteStructure: next));
          }

          return ListView(
            padding: const EdgeInsets.all(20),
            children: [
              if (block == GalleryNoteBlock.author)
                _settingsGroup(context, [
                  M3EListItem(
                    headline: '投稿者の抽出',
                    supportingText:
                        'ノート本文の先頭にある投稿者リンクから自動で取得します。独立した投稿者キーはありません。',
                    leading: Icon(Icons.info_outline),
                  ),
                ])
              else if (block == GalleryNoteBlock.postText)
                _settingsGroup(context, [
                  _expressiveSwitchTile(
                    title: '投稿文に引用（> ）を含める',
                    subtitle: 'オフにすると引用行を投稿文から除外します。',
                    value: structure.postTextIncludeQuote,
                    onChanged: (value) => _save(
                      context,
                      ref,
                      settings.copyWith(
                        noteStructure: structure.copyWith(
                          postTextIncludeQuote: value,
                        ),
                      ),
                    ),
                  ),
                ])
              else ...[
                _HeadingRuleCard(
                  title: '${block.label}として認識する見出し',
                  description: switch (block) {
                    GalleryNoteBlock.memo =>
                      '見出し配下の内容を覚書として扱います。見出しの階層は問わず、複数の見出し名を登録できます。',
                    GalleryNoteBlock.related =>
                      '見出し配下の項目を関連として扱います。見出しの階層は問わず、複数の見出し名を登録できます。',
                    _ =>
                      'この見出しに到達したところで投稿文の抽出を終了します。見出しの階層は問わず、複数の見出し名を登録できます。',
                  },
                  headings: headings,
                  onAdd: () async {
                    final heading = await _askHeading(context);
                    if (!context.mounted ||
                        heading == null ||
                        heading.trim().isEmpty ||
                        headings.contains(heading.trim())) {
                      return;
                    }
                    updateHeadings([...headings, heading.trim()]);
                  },
                  onRemove: (heading) => updateHeadings(
                    headings.where((item) => item != heading).toList(),
                  ),
                  onReset: () {
                    const defaults = GalleryNoteStructureSettings();
                    updateHeadings(switch (block) {
                      GalleryNoteBlock.memo => defaults.memoHeadings,
                      GalleryNoteBlock.related => defaults.relatedHeadings,
                      _ => defaults.postTextEndHeadings,
                    });
                  },
                ),
              ],
            ],
          );
        },
        skipLoadingOnReload: true,
      ),
    );
  }
}

class _FrontmatterSettingsScreen extends ConsumerWidget {
  const _FrontmatterSettingsScreen();

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
        M3ESnackbar.show(context, message: 'Frontmatter 設定を保存できませんでした。');
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, 'Frontmatter の設定'),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => const Center(child: Text('ノート設定を読み込めませんでした。')),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            padding: const EdgeInsets.all(20),
            children: [
              for (final (title, keys, update, defaults) in [
                (
                  'タグ',
                  structure.frontmatter.tagsKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(tagsKeys: values),
                  const GalleryFrontmatterSettings().tagsKeys,
                ),
                (
                  'タイトル',
                  structure.frontmatter.titleKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(titleKeys: values),
                  const GalleryFrontmatterSettings().titleKeys,
                ),
                (
                  '投稿URL',
                  structure.frontmatter.urlKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(urlKeys: values),
                  const GalleryFrontmatterSettings().urlKeys,
                ),
                (
                  '公開日時',
                  structure.frontmatter.publishedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(publishedKeys: values),
                  const GalleryFrontmatterSettings().publishedKeys,
                ),
                (
                  '作成日時',
                  structure.frontmatter.createdKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(createdKeys: values),
                  const GalleryFrontmatterSettings().createdKeys,
                ),
                (
                  '更新日時',
                  structure.frontmatter.updatedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(updatedKeys: values),
                  const GalleryFrontmatterSettings().updatedKeys,
                ),
                (
                  'カバー画像・動画',
                  structure.frontmatter.coverKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(coverKeys: values),
                  const GalleryFrontmatterSettings().coverKeys,
                ),
              ])
                _FrontmatterKeyCard(
                  title: title,
                  keys: keys,
                  defaults: defaults,
                  allowEmpty: title != 'タグ',
                  onChanged: (values) => _save(
                    context,
                    ref,
                    settings.copyWith(
                      noteStructure: structure.copyWith(
                        frontmatter: update(values),
                      ),
                    ),
                  ),
                ),
            ],
          );
        },
        skipLoadingOnReload: true,
      ),
    );
  }
}

/// Drag-to-reorder control for the non-frontmatter note-detail blocks.
/// Frontmatter itself is shown as a fixed, non-draggable first row because
/// it must always be pinned ahead of the other blocks.
bool _noteBlockHasSettings(GalleryNoteBlock block) => switch (block) {
  GalleryNoteBlock.postText ||
  GalleryNoteBlock.memo ||
  GalleryNoteBlock.related ||
  GalleryNoteBlock.postTextEnd => true,
  GalleryNoteBlock.author || GalleryNoteBlock.media => false,
};

String _noteBlockDescription(GalleryNoteBlock block) => switch (block) {
  GalleryNoteBlock.author => '本文冒頭の投稿者リンクから名前とURLを取得します。@ がないリンクにも対応します。',
  GalleryNoteBlock.media => 'ノートの画像・動画です。詳細欄では重複表示せず、閲覧画面のメディア領域に表示します。',
  GalleryNoteBlock.postText => '本文の投稿文です。投稿文の終端に指定した見出しより前を表示します。',
  GalleryNoteBlock.postTextEnd =>
    'ここに登録した見出しから後ろを投稿文として扱いません。この項目は順序に含まれますが、非表示にはできません。',
  GalleryNoteBlock.related =>
    '登録した見出し配下を関連項目として読み取ります。Vault内のノートリンクはタップして開けます。',
  GalleryNoteBlock.memo => '登録した見出し配下の文章、引用、コードを覚書として読み取ります。',
};

Future<void> _showNoteStructureInfo(
  BuildContext context,
  String title,
  String description,
) => M3EDialog.show<void>(
  context,
  dialog: M3EDialog(
    title: title,
    content: Text(description),
    actions: [
      M3EButton.text(
        onPressed: () => Navigator.of(context).pop(),
        child: const Text('閉じる'),
      ),
    ],
  ),
);

class _BlockOrderCard extends StatelessWidget {
  const _BlockOrderCard({
    required this.blockOrder,
    required this.hiddenBlocks,
    required this.onReorder,
    required this.onToggleVisibility,
    required this.onConfigure,
    required this.onReset,
  });

  final VoidCallback onReset;
  final List<GalleryNoteBlock> blockOrder;
  final List<GalleryNoteBlock> hiddenBlocks;
  final ValueChanged<List<GalleryNoteBlock>> onReorder;
  final void Function(GalleryNoteBlock block, bool visible) onToggleVisibility;
  final ValueChanged<GalleryNoteBlock> onConfigure;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        _settingsGroup(context, [
          M3EListItem(
            headline: 'Frontmatter',
            supportingText: '常に先頭に固定（並べ替え不可）',
            leading: Icon(Icons.push_pin_outlined, color: scheme.primary),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                M3EIconButton(
                  variant: M3EIconButtonVariant.standard,
                  tooltip: 'Frontmatter の説明',
                  onPressed: () => _showNoteStructureInfo(
                    context,
                    'Frontmatter',
                    'タグ、タイトル、URL、日付、カバーなどのメタデータです。'
                        '詳細欄の先頭に固定され、並べ替えや非表示はできません。',
                  ),
                  icon: const Icon(Icons.info_outline),
                ),
                M3EIconButton(
                  variant: M3EIconButtonVariant.standard,
                  tooltip: 'Frontmatter の設定',
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (context) => const _FrontmatterSettingsScreen(),
                    ),
                  ),
                  icon: const Icon(Icons.tune),
                ),
              ],
            ),
          ),
        ]),
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: M3ETheme(
            data: _listContainerTheme(context, scheme.surfaceContainerHigh),
            child: ReorderableListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              buildDefaultDragHandles: false,
              itemCount: blockOrder.length,
              onReorderItem: (oldIndex, newIndex) {
                final updated = [...blockOrder];
                final moved = updated.removeAt(oldIndex);
                updated.insert(newIndex, moved);
                onReorder(updated);
              },
              itemBuilder: (context, index) {
                final block = blockOrder[index];
                return Padding(
                  key: ValueKey(block),
                  padding: const EdgeInsets.only(bottom: 2),
                  child: M3EListItem(
                    headline: block.label,
                    leading: Icon(block.icon),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        M3EIconButton(
                          variant: M3EIconButtonVariant.standard,
                          tooltip: '${block.label}の説明',
                          onPressed: () => _showNoteStructureInfo(
                            context,
                            block.label,
                            _noteBlockDescription(block),
                          ),
                          icon: const Icon(Icons.info_outline),
                        ),
                        if (_noteBlockHasSettings(block))
                          M3EIconButton(
                            variant: M3EIconButtonVariant.standard,
                            tooltip: '${block.label}の設定',
                            onPressed: () => onConfigure(block),
                            icon: const Icon(Icons.tune),
                          ),
                        if (block != GalleryNoteBlock.postTextEnd &&
                            block != GalleryNoteBlock.media)
                          M3ESwitch(
                            value: !hiddenBlocks.contains(block),
                            onChanged: (value) =>
                                onToggleVisibility(block, value),
                            semanticLabel: '${block.label}を表示',
                          ),
                        ReorderableDragStartListener(
                          index: index,
                          child: const Padding(
                            padding: EdgeInsets.all(8),
                            child: Icon(Icons.drag_handle),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ),
        _ResetDefaultsButton(onPressed: onReset),
      ],
    );
  }
}

/// Small right-aligned action that restores one settings group to defaults.
class _ResetDefaultsButton extends StatelessWidget {
  const _ResetDefaultsButton({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => Align(
    alignment: Alignment.centerRight,
    child: M3EButton.icon(
      style: M3EButtonStyle.text,
      onPressed: onPressed,
      icon: const Icon(Icons.restore),
      label: const Text('初期設定に戻す'),
    ),
  );
}

/// Editor for the page size and whether the gallery shows a running loaded
/// item count.
class _PaginationSettingsCard extends StatefulWidget {
  const _PaginationSettingsCard({
    required this.pagination,
    required this.onChanged,
  });

  final GalleryPaginationSettings pagination;
  final ValueChanged<GalleryPaginationSettings> onChanged;

  @override
  State<_PaginationSettingsCard> createState() =>
      _PaginationSettingsCardState();
}

class _PaginationSettingsCardState extends State<_PaginationSettingsCard> {
  late final TextEditingController _pageSizeController = TextEditingController(
    text: '${widget.pagination.pageSize}',
  );

  @override
  void didUpdateWidget(covariant _PaginationSettingsCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    final text = '${widget.pagination.pageSize}';
    if (_pageSizeController.text != text) {
      _pageSizeController.text = text;
    }
  }

  @override
  void dispose() {
    _pageSizeController.dispose();
    super.dispose();
  }

  void _submitPageSize(String value) {
    final parsed = int.tryParse(value.trim());
    if (parsed == null ||
        parsed < GalleryPaginationSettings.minPageSize ||
        parsed > GalleryPaginationSettings.maxPageSize) {
      _pageSizeController.text = '${widget.pagination.pageSize}';
      return;
    }
    widget.onChanged(widget.pagination.copyWith(pageSize: parsed));
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _settingsPanel(
          context: context,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: M3ETextField(
              controller: _pageSizeController,
              keyboardType: TextInputType.number,
              label: 'ページサイズ',
              variant: M3ETextFieldVariant.outlined,
              supportingText:
                  '${GalleryPaginationSettings.minPageSize}〜'
                  '${GalleryPaginationSettings.maxPageSize}（既定 '
                  '${GalleryPaginationSettings.defaultPageSize}）',
              onSubmitted: _submitPageSize,
              onEditingComplete: () =>
                  _submitPageSize(_pageSizeController.text),
            ),
          ),
        ),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: '読み込み済み件数を表示',
            subtitle: '上部バーに現在表示中の件数を表示します。',
            value: widget.pagination.showItemCount,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemCount: value),
            ),
          ),
          _expressiveSwitchTile(
            title: 'タイルに一覧内の位置（何件目）を表示',
            value: widget.pagination.showItemNumberOnTiles,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemNumberOnTiles: value),
            ),
          ),
          _expressiveSwitchTile(
            title: '未表示メディアのアイコンを表示',
            subtitle: '既定ではアイコンを表示しません。',
            value: widget.pagination.showMissingMediaIcon,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showMissingMediaIcon: value),
            ),
          ),
        ]),
        _ResetDefaultsButton(
          onPressed: () => widget.onChanged(const GalleryPaginationSettings()),
        ),
      ],
    );
  }
}

class _HeadingRuleCard extends StatelessWidget {
  const _HeadingRuleCard({
    required this.title,
    required this.description,
    required this.headings,
    required this.onAdd,
    required this.onRemove,
    required this.onReset,
  });

  final VoidCallback onReset;
  final String title;
  final String description;
  final List<String> headings;
  final VoidCallback onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    return _settingsPanel(
      context: context,
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            Text(description, style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 12),
            if (headings.isEmpty)
              const Text('割り当て済みの見出しはありません。')
            else
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final heading in headings)
                    M3EChip(
                      label: heading,
                      type: M3EChipType.input,
                      onDeleted: () => onRemove(heading),
                    ),
                ],
              ),
            Align(
              alignment: Alignment.centerRight,
              child: M3EButton.icon(
                style: M3EButtonStyle.text,
                onPressed: onAdd,
                icon: const Icon(Icons.add),
                label: const Text('見出しを追加'),
              ),
            ),
            _ResetDefaultsButton(onPressed: onReset),
          ],
        ),
      ),
    );
  }
}

class _GalleryTagPrefixCard extends StatelessWidget {
  const _GalleryTagPrefixCard({
    required this.prefixes,
    required this.onChanged,
  });

  static const _defaults = ['source/art'];

  final List<String> prefixes;
  final ValueChanged<List<String>> onChanged;

  Future<void> _add(BuildContext context) async {
    final prefix = await M3EDialog.show<String>(
      context,
      dialog: const _TagPrefixDialog(title: 'ギャラリー対象タグを追加', hint: 'source'),
    );
    if (!context.mounted ||
        prefix == null ||
        prefixes.any(
          (value) => value.replaceFirst(RegExp(r'/+$'), '') == prefix,
        )) {
      return;
    }
    onChanged([...prefixes, prefix]);
  }

  @override
  Widget build(BuildContext context) => _settingsPanel(
    context: context,
    margin: const EdgeInsets.only(bottom: 16),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'このタグ、または下位タグが付いたノートを対象にします。複数指定は OR です。'
            '空にすると対象ノートはありません。',
          ),
          const SizedBox(height: 12),
          if (prefixes.isEmpty)
            const Text('対象タグはありません。')
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final prefix in prefixes)
                  M3EChip(
                    label: '#${prefix.replaceFirst(RegExp(r'/+$'), '')}/',
                    type: M3EChipType.input,
                    onDeleted: () => onChanged(
                      prefixes
                          .where((item) => item != prefix)
                          .toList(growable: false),
                    ),
                  ),
              ],
            ),
          Align(
            alignment: Alignment.centerRight,
            child: M3EButton.icon(
              style: M3EButtonStyle.text,
              onPressed: () => _add(context),
              icon: const Icon(Icons.add),
              label: const Text('タグを追加'),
            ),
          ),
          _ResetDefaultsButton(onPressed: () => onChanged(_defaults)),
        ],
      ),
    ),
  );
}

class _FrontmatterKeyCard extends StatelessWidget {
  const _FrontmatterKeyCard({
    required this.title,
    required this.keys,
    required this.onChanged,
    required this.defaults,
    this.allowEmpty = true,
  });

  final List<String> defaults;

  final String title;
  final List<String> keys;
  final ValueChanged<List<String>> onChanged;
  final bool allowEmpty;

  Future<void> _addKey(BuildContext context) async {
    final key = await M3EDialog.show<String>(
      context,
      dialog: _TagPrefixDialog(
        title: '$title のキーを追加',
        label: 'Frontmatter キー',
        hint: 'custom_$title',
        normalizeTagPrefix: false,
      ),
    );
    if (!context.mounted || key == null || key.trim().isEmpty) return;
    if (!keys.contains(key.trim())) {
      onChanged([...keys, key.trim()]);
    }
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: M3EList(
      itemCount: 1,
      itemBuilder: (context, index) => M3EListItem(
        headline: title,
        supportingText: keys.isEmpty ? '設定なし' : keys.join(' / '),
        expanded: M3EExpandableExpanded.content(
          Column(
            children: [
              for (final key in keys)
                M3EListItem(
                  headline: key,
                  trailing: M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: 'キーを削除',
                    icon: const Icon(Icons.remove_circle_outline),
                    onPressed: () {
                      if (!allowEmpty && keys.length == 1) {
                        M3ESnackbar.show(
                          context,
                          message: 'タグを読み取るキーは最低1つ必要です。',
                        );
                        return;
                      }
                      onChanged(
                        keys
                            .where((item) => item != key)
                            .toList(growable: false),
                      );
                    },
                  ),
                ),
              M3EListItem(
                headline: 'キーを追加',
                leading: const Icon(Icons.add),
                onTap: () => _addKey(context),
              ),
              M3EListItem(
                headline: '初期設定に戻す',
                leading: const Icon(Icons.restore),
                onTap: () => onChanged(defaults),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class _FictionalNoteExampleScreen extends ConsumerWidget {
  const _FictionalNoteExampleScreen();

  Future<void> _copyMarkdown(BuildContext context, String markdown) async {
    await Clipboard.setData(ClipboardData(text: markdown));
    if (context.mounted) {
      M3ESnackbar.show(context, message: '架空のノート例をコピーしました。');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings =
        ref.watch(galleryTagSettingsProvider).asData?.value ??
        const GalleryTagSettings();
    final markdown = _fictionalNoteExample(settings);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, '架空のノート例'),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _settingsPanel(
            context: context,
            color: Theme.of(context).colorScheme.secondaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                'この例のFrontmatterタグと本文のブロック順は現在の設定を反映しています。'
                'Frontmatterは固定で、本文は下のMarkdownの順に並びます。Vaultへ自動保存されません。',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onSecondaryContainer,
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          const _FictionalNoteLayoutPreview(),
          const SizedBox(height: 12),
          _settingsPanel(
            context: context,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: SelectableText(
                markdown,
                style: Theme.of(context).textTheme.bodyMedium
                    ?.copyWith(fontFamily: 'monospace', height: 1.45),
              ),
            ),
          ),
          Align(
            alignment: Alignment.centerRight,
            child: M3EButton.icon(
              style: M3EButtonStyle.filled,
              onPressed: () => _copyMarkdown(context, markdown),
              icon: const Icon(Icons.copy),
              label: const Text('Markdownをコピー'),
            ),
          ),
        ],
      ),
    );
  }
}

class _FictionalNoteLayoutPreview extends ConsumerWidget {
  const _FictionalNoteLayoutPreview();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings =
        ref.watch(galleryTagSettingsProvider).asData?.value ??
        const GalleryTagSettings();
    final order = settings.noteStructure.blockOrder;
    final hidden = settings.noteStructure.hiddenBlocks;
    final tags = _fictionalNoteTags(settings);
    final scheme = Theme.of(context).colorScheme;
    return _settingsPanel(
      context: context,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Frontmatterタグ',
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tag in tags) _ViewerTagChip(tag: tag),
                if (tags.isEmpty) const Text('表示できる架空タグはありません'),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              '下の架空Markdownの本文順',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            M3EListItem(
              headline: 'Frontmatter',
              supportingText: 'タグ・タイトルなど（固定）',
              leading: Icon(Icons.push_pin_outlined, color: scheme.primary),
            ),
            for (final block in order.where(
              (block) =>
                  block == GalleryNoteBlock.postTextEnd ||
                  !hidden.contains(block),
            ))
              M3EListItem(
                key: ValueKey('fictional-${block.name}'),
                headline: block.label,
                supportingText: switch (block) {
                  GalleryNoteBlock.author => '架空の投稿者リンク',
                  GalleryNoteBlock.media => '架空の画像埋め込み',
                  GalleryNoteBlock.postText => '架空の投稿文',
                  GalleryNoteBlock.memo => '平文・引用・コード内の覚書',
                  GalleryNoteBlock.related => '架空のノートへのリンク',
                  GalleryNoteBlock.postTextEnd => '本文中の # 文書 見出し',
                },
                leading: Icon(block.icon),
              ),
          ],
        ),
      ),
    );
  }
}

M3EThemeData _listContainerTheme(BuildContext context, Color color) {
  final theme = M3ETheme.of(context);
  return theme.copyWith(
    listTheme: theme.listTheme.copyWith(
      item: theme.listTheme.item.copyWith(containerColor: color),
    ),
  );
}
