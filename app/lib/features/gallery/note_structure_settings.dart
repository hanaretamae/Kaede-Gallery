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
        M3ESnackbar.show(
          context,
          message: tr(
            'ノート構造の設定を保存できませんでした。',
            'Could not save note structure settings.',
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, tr('ノート構造', 'Note structure')),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => Center(
          child: Text(tr('タグ設定を読み込めませんでした。', 'Could not load tag settings.')),
        ),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            key: const ValueKey('note-structure-settings-list'),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            children: [
              _settingsSectionHeading(
                context,
                tr('ノート詳細のブロック順序', 'Note detail block order'),
                first: true,
              ),
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
                        tr(
                          'Markdown / Wikilink のノート解決',
                          'Markdown / Wikilink note resolution',
                        ),
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      ExpressiveMaterialScope(
                        theme: Theme.of(context),
                        child: M3EButtonGroup(
                          semanticLabel: tr(
                            'ノートリンクの解決方法',
                            'How note links are resolved',
                          ),
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
                          actions: [
                            M3EButtonGroupAction(
                              label: Text(tr('最短', 'Shortest')),
                            ),
                            M3EButtonGroupAction(
                              label: Text(tr('相対', 'Relative')),
                            ),
                            M3EButtonGroupAction(
                              label: Text(tr('絶対', 'Absolute')),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        tr(
                          '最短はVault内の同名ノートから参照元に近いものを選びます。相対は現在のノート位置を基準にし、絶対はVaultのルートを基準にします。Vault外へ解決されるリンクは無視します。',
                          'Shortest chooses the closest matching note with the same name inside the Vault. Relative uses the current note location as the base, and Absolute uses the Vault root. Links that resolve outside the Vault are ignored.',
                        ),
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
                  headline: tr(
                    '架空のノート例を見る・コピー',
                    'View or copy a fictional note example',
                  ),
                  supportingText: tr(
                    '設定中の項目順を反映した表示例も確認できます',
                    'You can also review a display example that reflects the current item order.',
                  ),
                  leading: _settingsIcon(
                    context,
                    Icons.content_copy_outlined,
                    tone: 1,
                  ),
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
        M3ESnackbar.show(
          context,
          message: tr(
            'ノート構造の設定を保存できませんでした。',
            'Could not save note structure settings.',
          ),
        );
      }
    }
  }

  Future<String?> _askHeading(BuildContext context) => M3EDialog.show<String>(
    context,
    dialog: _TagPrefixDialog(
      title: tr('${block.label}の見出しを追加', 'Add a heading for ${block.label}'),
      label: tr('見出し名', 'Heading name'),
      hint: tr('制作メモ', 'Production notes'),
      normalizeTagPrefix: false,
    ),
  );

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsState = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(
          context,
          tr('${block.label}の設定', '${block.label} settings'),
        ),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => Center(
          child: Text(tr('ノート設定を読み込めませんでした。', 'Could not load note settings.')),
        ),
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
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            children: [
              if (block == GalleryNoteBlock.author)
                _settingsGroup(context, [
                  M3EListItem(
                    headline: tr('投稿者の抽出', 'Author extraction'),
                    supportingText: tr(
                      'ノート本文の先頭にある投稿者リンクから自動で取得します。独立した投稿者キーはありません。',
                      'Automatically retrieves the author from the author link at the start of the note body. There is no separate author key.',
                    ),
                    leading: Icon(Icons.info_outline),
                  ),
                ])
              else if (block == GalleryNoteBlock.postText)
                _settingsGroup(context, [
                  _expressiveSwitchTile(
                    title: tr(
                      '投稿文に引用（> ）を含める',
                      'Include quotes (>) in post text',
                    ),
                    subtitle: tr(
                      'オフにすると引用行を投稿文から除外します。',
                      'If off, quoted lines are excluded from the post text.',
                    ),
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
                  M3EListItem(
                    headline: tr('初期設定に戻す', 'Restore defaults'),
                    leading: Icon(Icons.restore),
                    onTap: () => _save(
                      context,
                      ref,
                      settings.copyWith(
                        noteStructure: structure.copyWith(
                          postTextIncludeQuote:
                              const GalleryNoteStructureSettings()
                                  .postTextIncludeQuote,
                        ),
                      ),
                    ),
                  ),
                ])
              else ...[
                _HeadingRuleCard(
                  title: tr(
                    '${block.label}として認識する見出し',
                    'Headings recognized as ${block.label}',
                  ),
                  description: switch (block) {
                    GalleryNoteBlock.memo => tr(
                      '見出し配下の内容を覚書として扱います。見出しの階層は問わず、複数の見出し名を登録できます。',
                      'Treats content under these headings as notes. Heading depth does not matter, and you can register multiple heading names.',
                    ),
                    GalleryNoteBlock.related => tr(
                      '見出し配下の項目を関連として扱います。見出しの階層は問わず、複数の見出し名を登録できます。',
                      'Treats items under these headings as related content. Heading depth does not matter, and you can register multiple heading names.',
                    ),
                    _ => tr(
                      'この見出しに到達したところで投稿文の抽出を終了します。見出しの階層は問わず、複数の見出し名を登録できます。',
                      'Stops extracting post text when this heading is reached. Heading depth does not matter, and you can register multiple heading names.',
                    ),
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
        M3ESnackbar.show(
          context,
          message: tr(
            'Frontmatter 設定を保存できませんでした。',
            'Could not save Frontmatter settings.',
          ),
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
          tr('Frontmatter の設定', 'Frontmatter settings'),
        ),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => Center(
          child: Text(tr('ノート設定を読み込めませんでした。', 'Could not load note settings.')),
        ),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            children: [
              for (final (title, keys, update, defaults, allowEmpty) in [
                (
                  tr('タグ', 'Tags'),
                  structure.frontmatter.tagsKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(tagsKeys: values),
                  const GalleryFrontmatterSettings().tagsKeys,
                  false,
                ),
                (
                  tr('タイトル', 'Title'),
                  structure.frontmatter.titleKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(titleKeys: values),
                  const GalleryFrontmatterSettings().titleKeys,
                  true,
                ),
                (
                  tr('投稿URL', 'Post URL'),
                  structure.frontmatter.urlKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(urlKeys: values),
                  const GalleryFrontmatterSettings().urlKeys,
                  true,
                ),
                (
                  tr('公開日時', 'Published at'),
                  structure.frontmatter.publishedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(publishedKeys: values),
                  const GalleryFrontmatterSettings().publishedKeys,
                  true,
                ),
                (
                  tr('作成日時', 'Created at'),
                  structure.frontmatter.createdKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(createdKeys: values),
                  const GalleryFrontmatterSettings().createdKeys,
                  true,
                ),
                (
                  tr('更新日時', 'Updated at'),
                  structure.frontmatter.updatedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(updatedKeys: values),
                  const GalleryFrontmatterSettings().updatedKeys,
                  true,
                ),
                (
                  tr('カバー画像・動画', 'Cover image / video'),
                  structure.frontmatter.coverKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(coverKeys: values),
                  const GalleryFrontmatterSettings().coverKeys,
                  true,
                ),
              ])
                _FrontmatterKeyCard(
                  title: title,
                  keys: keys,
                  defaults: defaults,
                  allowEmpty: allowEmpty,
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
  GalleryNoteBlock.author => tr(
    '本文冒頭の投稿者リンクから名前とURLを取得します。@ がないリンクにも対応します。',
    'Retrieves the author name and URL from the author link at the start of the body. Links without @ are also supported.',
  ),
  GalleryNoteBlock.media => tr(
    'ノートの画像・動画です。詳細欄では重複表示せず、閲覧画面のメディア領域に表示します。',
    "These are the note's images and videos. They are not duplicated in the details section and are shown in the media area of the viewer.",
  ),
  GalleryNoteBlock.postText => tr(
    '本文の投稿文です。投稿文の終端に指定した見出しより前を表示します。',
    'This is the body post text. Content before the heading specified as the end of the post text is shown.',
  ),
  GalleryNoteBlock.postTextEnd => tr(
    'ここに登録した見出しから後ろを投稿文として扱いません。この項目は順序に含まれますが、非表示にはできません。',
    'Content after the headings registered here is not treated as post text. This item remains in the order but cannot be hidden.',
  ),
  GalleryNoteBlock.related => tr(
    '登録した見出し配下を関連項目として読み取ります。Vault内のノートリンクはタップして開けます。',
    'Reads content under the registered headings as related items. Note links inside the Vault can be tapped to open them.',
  ),
  GalleryNoteBlock.memo => tr(
    '登録した見出し配下の文章、引用、コードを覚書として読み取ります。',
    'Reads text, quotes, and code under the registered headings as notes.',
  ),
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
        child: Text(tr('閉じる', 'Close')),
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
    void move(int index, int delta) {
      final target = index + delta;
      if (target < 0 || target >= blockOrder.length) return;
      final updated = [...blockOrder];
      updated.insert(target, updated.removeAt(index));
      onReorder(updated);
    }

    return _settingsGroup(context, [
      M3EListItem(
        headline: 'Frontmatter',
        supportingText: tr(
          '常に先頭に固定（並べ替え不可）',
          'Always fixed at the top (cannot be reordered)',
        ),
        leading: Icon(Icons.push_pin_outlined),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: tr('Frontmatter の説明', 'Frontmatter description'),
              onPressed: () => _showNoteStructureInfo(
                context,
                'Frontmatter',
                tr(
                  'タグ、タイトル、URL、日付、カバーなどのメタデータです。'
                      '詳細欄の先頭に固定され、並べ替えや非表示はできません。',
                  'This is metadata such as tags, titles, URLs, dates, and covers. It is fixed at the top of the details section and cannot be reordered or hidden.',
                ),
              ),
              icon: const Icon(Icons.info_outline),
            ),
            M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: tr('Frontmatter の設定', 'Frontmatter settings'),
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
      for (final (index, block) in blockOrder.indexed)
        M3EListItem(
          key: ValueKey(block),
          headline: block.label,
          leading: Icon(block.icon),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: tr('${block.label}の説明', '${block.label} description'),
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
                  tooltip: tr('${block.label}の設定', '${block.label} settings'),
                  onPressed: () => onConfigure(block),
                  icon: const Icon(Icons.tune),
                ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: tr('${block.label}を上へ', 'Move ${block.label} up'),
                onPressed: index == 0 ? null : () => move(index, -1),
                icon: const Icon(Icons.arrow_upward),
              ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: tr('${block.label}を下へ', 'Move ${block.label} down'),
                onPressed: index == blockOrder.length - 1
                    ? null
                    : () => move(index, 1),
                icon: const Icon(Icons.arrow_downward),
              ),
              if (block != GalleryNoteBlock.postTextEnd &&
                  block != GalleryNoteBlock.media)
                M3ESwitch(
                  value: !hiddenBlocks.contains(block),
                  onChanged: (value) => onToggleVisibility(block, value),
                  semanticLabel: tr('${block.label}を表示', 'Show ${block.label}'),
                )
              else
                const SizedBox(width: 52),
            ],
          ),
        ),
      M3EListItem(
        headline: tr('初期設定に戻す', 'Restore defaults'),
        leading: Icon(Icons.restore),
        onTap: onReset,
      ),
    ]);
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
      label: Text(tr('初期設定に戻す', 'Restore defaults')),
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
              label: tr('ページサイズ', 'Page size'),
              variant: M3ETextFieldVariant.outlined,
              supportingText: tr(
                '${GalleryPaginationSettings.minPageSize}〜'
                    '${GalleryPaginationSettings.maxPageSize}（既定 '
                    '${GalleryPaginationSettings.defaultPageSize}）',
                '${GalleryPaginationSettings.minPageSize}–'
                    '${GalleryPaginationSettings.maxPageSize} (default '
                    '${GalleryPaginationSettings.defaultPageSize})',
              ),
              onSubmitted: _submitPageSize,
              onEditingComplete: () =>
                  _submitPageSize(_pageSizeController.text),
            ),
          ),
        ),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: tr('読み込み済み件数を表示', 'Show loaded item count'),
            subtitle: tr(
              '上部バーに現在表示中の件数を表示します。',
              'Shows the number of items currently displayed in the top bar.',
            ),
            value: widget.pagination.showItemCount,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemCount: value),
            ),
          ),
          _expressiveSwitchTile(
            title: tr(
              'タイルに一覧内の位置（何件目）を表示',
              "Show each tile's position in the list",
            ),
            value: widget.pagination.showItemNumberOnTiles,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemNumberOnTiles: value),
            ),
          ),
          _expressiveSwitchTile(
            title: tr('未表示メディアのアイコンを表示', 'Show an icon for hidden media'),
            subtitle: tr('既定ではアイコンを表示しません。', 'By default, the icon is hidden.'),
            value: widget.pagination.showMissingMediaIcon,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showMissingMediaIcon: value),
            ),
          ),
          M3EListItem(
            headline: tr('初期設定に戻す', 'Restore defaults'),
            leading: Icon(Icons.restore),
            onTap: () => widget.onChanged(const GalleryPaginationSettings()),
          ),
        ]),
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
              Text(tr('割り当て済みの見出しはありません。', 'No headings are assigned.'))
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
                label: Text(tr('見出しを追加', 'Add heading')),
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
      dialog: _TagPrefixDialog(
        title: tr('ギャラリー対象タグを追加', 'Add gallery target tag'),
        hint: 'source',
      ),
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
          Text(
            tr(
              'このタグ、または下位タグが付いたノートを対象にします。複数指定は OR です。'
                  '空にすると対象ノートはありません。',
              'Notes with this tag or any child tag are included. Multiple entries use OR. If empty, no notes are included.',
            ),
          ),
          const SizedBox(height: 12),
          if (prefixes.isEmpty)
            Text(tr('対象タグはありません。', 'No target tags are set.'))
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
              label: Text(tr('タグを追加', 'Add tag')),
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
        title: tr('$title のキーを追加', 'Add a key for $title'),
        label: tr('Frontmatter キー', 'Frontmatter key'),
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
        supportingText: keys.isEmpty ? tr('設定なし', 'Not set') : keys.join(' / '),
        expanded: M3EExpandableExpanded.content(
          Column(
            children: [
              for (final key in keys)
                M3EListItem(
                  headline: key,
                  trailing: M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: tr('キーを削除', 'Delete key'),
                    icon: const Icon(Icons.remove_circle_outline),
                    onPressed: () {
                      if (!allowEmpty && keys.length == 1) {
                        M3ESnackbar.show(
                          context,
                          message: tr(
                            'タグを読み取るキーは最低1つ必要です。',
                            'At least one key is required to read tags.',
                          ),
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
                headline: tr('キーを追加', 'Add key'),
                leading: Icon(Icons.add),
                onTap: () => _addKey(context),
              ),
              M3EListItem(
                headline: tr('初期設定に戻す', 'Restore defaults'),
                leading: Icon(Icons.restore),
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
      M3ESnackbar.show(
        context,
        message: tr(
          '架空のノート例をコピーしました。',
          'The fictional note example was copied.',
        ),
      );
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
        title: galleryAppBarTitle(
          context,
          tr('架空のノート例', 'Fictional note example'),
        ),
        leading: _expressiveBackButton(context),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _settingsPanel(
            context: context,
            color: Theme.of(context).colorScheme.secondaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                tr(
                  'この例のFrontmatterタグと本文のブロック順は現在の設定を反映しています。'
                      'Frontmatterは固定で、本文は下のMarkdownの順に並びます。Vaultへ自動保存されません。',
                  'The Frontmatter tags and body block order in this example reflect the current settings. Frontmatter stays fixed, and the body follows the Markdown order below. It is not saved to the Vault automatically.',
                ),
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
              label: Text(tr('Markdownをコピー', 'Copy Markdown')),
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
    return _settingsPanel(
      context: context,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              tr('Frontmatterタグ', 'Frontmatter tags'),
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tag in tags) _ViewerTagChip(tag: tag),
                if (tags.isEmpty)
                  Text(
                    tr(
                      '表示できる架空タグはありません',
                      'There are no fictional tags to display.',
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              tr(
                '下の架空Markdownの本文順',
                'Body order in the fictional Markdown below',
              ),
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            _settingsGroup(
              context,
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              [
                M3EListItem(
                  headline: 'Frontmatter',
                  supportingText: tr(
                    'タグ・タイトルなど（固定）',
                    'Tags, title, and more (fixed)',
                  ),
                  leading: const Icon(Icons.push_pin_outlined),
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
                      GalleryNoteBlock.author => tr(
                        '架空の投稿者リンク',
                        'Fictional author link',
                      ),
                      GalleryNoteBlock.media => tr(
                        '架空の画像埋め込み',
                        'Fictional embedded image',
                      ),
                      GalleryNoteBlock.postText => tr(
                        '架空の投稿文',
                        'Fictional post text',
                      ),
                      GalleryNoteBlock.memo => tr(
                        '平文・引用・コード内の覚書',
                        'Notes in plain text, quotes, and code',
                      ),
                      GalleryNoteBlock.related => tr(
                        '架空のノートへのリンク',
                        'Link to a fictional note',
                      ),
                      GalleryNoteBlock.postTextEnd => tr(
                        '本文中の # 文書 見出し',
                        '# Document heading in the body',
                      ),
                    },
                    leading: Icon(block.icon),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
