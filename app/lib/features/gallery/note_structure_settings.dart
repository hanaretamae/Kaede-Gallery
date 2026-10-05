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
          message: context.l10n.couldNotSaveNoteStructureSettings,
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
        title: galleryAppBarTitle(context, context.l10n.noteStructure),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) =>
            Center(child: Text(context.l10n.couldNotLoadTagSettings)),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            key: const ValueKey('note-structure-settings-list'),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            children: [
              _settingsSectionHeading(
                context,
                context.l10n.noteDetailBlockOrder,
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
                        context.l10n.markdownWikilinkNoteResolution,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      ExpressiveMaterialScope(
                        theme: Theme.of(context),
                        child: M3EButtonGroup(
                          semanticLabel: context.l10n.howNoteLinksAreResolved,
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
                              label: Text(context.l10n.shortest),
                            ),
                            M3EButtonGroupAction(
                              label: Text(context.l10n.relative),
                            ),
                            M3EButtonGroupAction(
                              label: Text(context.l10n.absolute),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        context
                            .l10n
                            .shortestChoosesTheClosestMatchingNoteWithTheSame,
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
                  headline: context.l10n.viewOrCopyAFictionalNoteExample,
                  supportingText: context
                      .l10n
                      .youCanAlsoReviewADisplayExampleThatReflectsTheCu,
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
          message: context.l10n.couldNotSaveNoteStructureSettings,
        );
      }
    }
  }

  Future<String?> _askHeading(BuildContext context) => M3EDialog.show<String>(
    context,
    dialog: _TagPrefixDialog(
      title: context.l10n.addAHeadingFor(block.label),
      label: context.l10n.headingName,
      hint: context.l10n.productionNotes,
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
          context.l10n.settings_5e5451(block.label),
        ),
        leading: _expressiveBackButton(context),
      ),
      body: settingsState.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) =>
            Center(child: Text(context.l10n.couldNotLoadNoteSettings)),
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
                    headline: context.l10n.authorExtraction,
                    supportingText: context
                        .l10n
                        .automaticallyRetrievesTheAuthorFromTheAuthorLink,
                    leading: Icon(Icons.info_outline),
                  ),
                ])
              else if (block == GalleryNoteBlock.postText)
                _settingsGroup(context, [
                  _expressiveSwitchTile(
                    title: context.l10n.includeQuotesInPostText,
                    subtitle:
                        context.l10n.ifOffQuotedLinesAreExcludedFromThePostText,
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
                    headline: context.l10n.restoreDefaults_c4cee4,
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
                  title: context.l10n.headingsRecognizedAs(block.label),
                  description: switch (block) {
                    GalleryNoteBlock.memo =>
                      context
                          .l10n
                          .treatsContentUnderTheseHeadingsAsNotesHeadingDep,
                    GalleryNoteBlock.related =>
                      context
                          .l10n
                          .treatsItemsUnderTheseHeadingsAsRelatedContentHea,
                    _ =>
                      context
                          .l10n
                          .stopsExtractingPostTextWhenThisHeadingIsReachedH,
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
          message: context.l10n.couldNotSaveFrontmatterSettings,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(galleryTagSettingsProvider);
    return Scaffold(
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, context.l10n.frontmatterSettings),
        leading: _expressiveBackButton(context),
      ),
      body: state.when(
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) =>
            Center(child: Text(context.l10n.couldNotLoadNoteSettings)),
        data: (settings) {
          final structure = settings.noteStructure;
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            children: [
              for (final (title, keys, update, defaults, allowEmpty) in [
                (
                  context.l10n.tags,
                  structure.frontmatter.tagsKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(tagsKeys: values),
                  const GalleryFrontmatterSettings().tagsKeys,
                  false,
                ),
                (
                  context.l10n.title,
                  structure.frontmatter.titleKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(titleKeys: values),
                  const GalleryFrontmatterSettings().titleKeys,
                  true,
                ),
                (
                  context.l10n.postURL,
                  structure.frontmatter.urlKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(urlKeys: values),
                  const GalleryFrontmatterSettings().urlKeys,
                  true,
                ),
                (
                  context.l10n.publishedAt,
                  structure.frontmatter.publishedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(publishedKeys: values),
                  const GalleryFrontmatterSettings().publishedKeys,
                  true,
                ),
                (
                  context.l10n.createdAt,
                  structure.frontmatter.createdKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(createdKeys: values),
                  const GalleryFrontmatterSettings().createdKeys,
                  true,
                ),
                (
                  context.l10n.updatedAt,
                  structure.frontmatter.updatedKeys,
                  (List<String> values) =>
                      structure.frontmatter.copyWith(updatedKeys: values),
                  const GalleryFrontmatterSettings().updatedKeys,
                  true,
                ),
                (
                  context.l10n.coverImageVideo,
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
  GalleryNoteBlock.author =>
    AppL10n.current.retrievesTheAuthorNameAndURLFromTheAuthorLinkAtT,
  GalleryNoteBlock.media =>
    AppL10n.current.theseAreTheNoteSImagesAndVideosTheyAreNotDuplica,
  GalleryNoteBlock.postText =>
    AppL10n.current.thisIsTheBodyPostTextContentBeforeTheHeadingSpec,
  GalleryNoteBlock.postTextEnd =>
    AppL10n.current.contentAfterTheHeadingsRegisteredHereIsNotTreate,
  GalleryNoteBlock.related =>
    AppL10n.current.readsContentUnderTheRegisteredHeadingsAsRelatedI,
  GalleryNoteBlock.memo =>
    AppL10n.current.readsTextQuotesAndCodeUnderTheRegisteredHeadings,
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
        child: Text(context.l10n.close),
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
        supportingText: context.l10n.alwaysFixedAtTheTopCannotBeReordered,
        leading: Icon(Icons.push_pin_outlined),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: context.l10n.frontmatterDescription,
              onPressed: () => _showNoteStructureInfo(
                context,
                'Frontmatter',
                context.l10n.thisIsMetadataSuchAsTagsTitlesURLsDatesAndCovers,
              ),
              icon: const Icon(Icons.info_outline),
            ),
            M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: context.l10n.frontmatterSettings,
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
                tooltip: context.l10n.description(block.label),
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
                  tooltip: context.l10n.settings_5e5451(block.label),
                  onPressed: () => onConfigure(block),
                  icon: const Icon(Icons.tune),
                ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.moveUp(block.label),
                onPressed: index == 0 ? null : () => move(index, -1),
                icon: const Icon(Icons.arrow_upward),
              ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.moveDown(block.label),
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
                  semanticLabel: context.l10n.show(block.label),
                )
              else
                const SizedBox(width: 52),
            ],
          ),
        ),
      M3EListItem(
        headline: context.l10n.restoreDefaults_c4cee4,
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
      label: Text(context.l10n.restoreDefaults_c4cee4),
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
              label: context.l10n.pageSize,
              variant: M3ETextFieldVariant.outlined,
              supportingText: context.l10n.pageSizeRange(
                GalleryPaginationSettings.minPageSize,
                GalleryPaginationSettings.maxPageSize,
                GalleryPaginationSettings.defaultPageSize,
              ),
              onSubmitted: _submitPageSize,
              onEditingComplete: () =>
                  _submitPageSize(_pageSizeController.text),
            ),
          ),
        ),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: context.l10n.showLoadedItemCount,
            subtitle:
                context.l10n.showsTheNumberOfItemsCurrentlyDisplayedInTheTopB,
            value: widget.pagination.showItemCount,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemCount: value),
            ),
          ),
          _expressiveSwitchTile(
            title: context.l10n.showEachTileSPositionInTheList,
            value: widget.pagination.showItemNumberOnTiles,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showItemNumberOnTiles: value),
            ),
          ),
          _expressiveSwitchTile(
            title: context.l10n.showAnIconForHiddenMedia,
            subtitle: context.l10n.byDefaultTheIconIsHidden,
            value: widget.pagination.showMissingMediaIcon,
            onChanged: (value) => widget.onChanged(
              widget.pagination.copyWith(showMissingMediaIcon: value),
            ),
          ),
          M3EListItem(
            headline: context.l10n.restoreDefaults_c4cee4,
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
              Text(context.l10n.noHeadingsAreAssigned)
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
                label: Text(context.l10n.addHeading),
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
        title: context.l10n.addGalleryTargetTag,
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
          Text(context.l10n.notesWithThisTagOrAnyChildTagAreIncludedMultiple),
          const SizedBox(height: 12),
          if (prefixes.isEmpty)
            Text(context.l10n.noTargetTagsAreSet)
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
              label: Text(context.l10n.addTag),
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
        title: context.l10n.addAKeyFor(title),
        label: context.l10n.frontmatterKey,
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
        supportingText: keys.isEmpty ? context.l10n.notSet : keys.join(' / '),
        expanded: M3EExpandableExpanded.content(
          Column(
            children: [
              for (final key in keys)
                M3EListItem(
                  headline: key,
                  trailing: M3EIconButton(
                    variant: M3EIconButtonVariant.standard,
                    tooltip: context.l10n.deleteKey,
                    icon: const Icon(Icons.remove_circle_outline),
                    onPressed: () {
                      if (!allowEmpty && keys.length == 1) {
                        M3ESnackbar.show(
                          context,
                          message:
                              context.l10n.atLeastOneKeyIsRequiredToReadTags,
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
                headline: context.l10n.addKey,
                leading: Icon(Icons.add),
                onTap: () => _addKey(context),
              ),
              M3EListItem(
                headline: context.l10n.restoreDefaults_c4cee4,
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
        message: context.l10n.theFictionalNoteExampleWasCopied,
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
        title: galleryAppBarTitle(context, context.l10n.fictionalNoteExample),
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
                context.l10n.theFrontmatterTagsAndBodyBlockOrderInThisExample,
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
              label: Text(context.l10n.copyMarkdown),
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
              context.l10n.frontmatterTags,
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tag in tags) _ViewerTagChip(tag: tag),
                if (tags.isEmpty)
                  Text(context.l10n.thereAreNoFictionalTagsToDisplay),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              context.l10n.bodyOrderInTheFictionalMarkdownBelow,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            _settingsGroup(
              context,
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              [
                M3EListItem(
                  headline: 'Frontmatter',
                  supportingText: context.l10n.tagsTitleAndMoreFixed,
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
                      GalleryNoteBlock.author =>
                        context.l10n.fictionalAuthorLink,
                      GalleryNoteBlock.media =>
                        context.l10n.fictionalEmbeddedImage,
                      GalleryNoteBlock.postText =>
                        context.l10n.fictionalPostText,
                      GalleryNoteBlock.memo =>
                        context.l10n.notesInPlainTextQuotesAndCode,
                      GalleryNoteBlock.related =>
                        context.l10n.linkToAFictionalNote,
                      GalleryNoteBlock.postTextEnd =>
                        context.l10n.documentHeadingInTheBody,
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
