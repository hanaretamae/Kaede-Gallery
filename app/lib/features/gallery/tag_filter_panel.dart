part of 'gallery_screen.dart';

bool _matchesTagSearch(String text, String query) {
  final normalized = text.toLowerCase();
  return query.split(RegExp(r'\s+')).where((term) => term.isNotEmpty).any((
    term,
  ) {
    final boundedTerm = String.fromCharCodes(term.runes.take(128));
    if (normalized.contains(boundedTerm)) return true;
    final parts = normalized.split(RegExp(r'[\s/_-]+'));
    final termLength = boundedTerm.runes.length;
    final tolerance = termLength < 3
        ? 0
        : termLength < 6
        ? 1
        : 2;
    return parts.any((part) => _fuzzyWordMatches(part, boundedTerm, tolerance));
  });
}

bool _fuzzyWordMatches(String word, String term, int tolerance) {
  final wordRunes = word.runes.take(128).toList(growable: false);
  final termRunes = term.runes.toList(growable: false);
  if (wordRunes.isEmpty || termRunes.isEmpty) return false;
  if ((wordRunes.length - termRunes.length).abs() <= tolerance &&
      _levenshteinDistance(word, term) <= tolerance) {
    return true;
  }
  final minLength = (termRunes.length - tolerance).clamp(1, 128).toInt();
  final maxLength = (termRunes.length + tolerance)
      .clamp(1, wordRunes.length)
      .toInt();
  if (minLength > maxLength) return false;
  for (var length = minLength; length <= maxLength; length++) {
    for (var start = 0; start + length <= wordRunes.length; start++) {
      final part = String.fromCharCodes(wordRunes.skip(start).take(length));
      if (_levenshteinDistance(part, term) <= tolerance) return true;
    }
  }
  return false;
}

int _levenshteinDistance(String left, String right) {
  final leftRunes = left.runes.toList(growable: false);
  final rightRunes = right.runes.toList(growable: false);
  var previous = List<int>.generate(rightRunes.length + 1, (index) => index);
  var current = List<int>.filled(rightRunes.length + 1, 0);
  for (var leftIndex = 0; leftIndex < leftRunes.length; leftIndex++) {
    current[0] = leftIndex + 1;
    for (var rightIndex = 0; rightIndex < rightRunes.length; rightIndex++) {
      final substitutionCost = leftRunes[leftIndex] == rightRunes[rightIndex]
          ? 0
          : 1;
      current[rightIndex + 1] = [
        previous[rightIndex + 1] + 1,
        current[rightIndex] + 1,
        previous[rightIndex] + substitutionCost,
      ].reduce((left, right) => left < right ? left : right);
    }
    final swap = previous;
    previous = current;
    current = swap;
  }
  return previous[rightRunes.length];
}

class _TagPanel extends ConsumerStatefulWidget {
  const _TagPanel();

  @override
  ConsumerState<_TagPanel> createState() => _TagPanelState();
}

class _TagPanelState extends ConsumerState<_TagPanel> {
  late final TextEditingController tagSearchController;
  late final TextEditingController noteSearchController;
  final Set<String> expandedCategories = {};
  String tagSearch = '';

  @override
  void initState() {
    super.initState();
    tagSearchController = TextEditingController()
      ..addListener(_onSearchChanged);
    noteSearchController = TextEditingController(
      text: ref.read(gallerySearchQueryProvider),
    );
    _onSearchChanged();
  }

  @override
  void dispose() {
    tagSearchController
      ..removeListener(_onSearchChanged)
      ..dispose();
    noteSearchController.dispose();
    super.dispose();
  }

  void _onSearchChanged() {
    final value = tagSearchController.text
        .split(RegExp(r'\s+'))
        .where((term) => term.isNotEmpty)
        .join(' ')
        .toLowerCase();
    if (tagSearch != value) setState(() => tagSearch = value);
  }

  void _applyNoteSearch() {
    ref
        .read(gallerySearchQueryProvider.notifier)
        .set(noteSearchController.text);
    FocusScope.of(context).unfocus();
  }

  void _clearAll() {
    tagSearchController.clear();
    noteSearchController.clear();
    ref.read(selectedTagsProvider.notifier).clear();
    ref.read(allTagsProvider.notifier).clear();
    ref.read(excludedTagsProvider.notifier).clear();
    ref.read(selectedVirtualFiltersProvider.notifier).clear();
    ref.read(gallerySearchQueryProvider.notifier).clear();
  }

  Widget _filterLegend(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
    child: Wrap(
      spacing: 14,
      runSpacing: 6,
      children: [
        _FilterLegendItem(icon: Icons.add, label: 'いずれか (+)'),
        _FilterLegendItem(icon: Icons.done_all, label: 'すべて (AND)'),
        _FilterLegendItem(icon: Icons.remove, label: '除外 (-)'),
      ],
    ),
  );

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(galleryCategoriesProvider);
    return categories.when(
      loading: () => const Center(child: M3EProgressIndicator.circular()),
      error: (_, _) => const Center(child: Text('タグ一覧を読み込めませんでした。')),
      data: (items) {
        final tagSettingsState = ref.watch(galleryTagSettingsProvider);
        return tagSettingsState.when(
          loading: () => const Center(child: M3EProgressIndicator.circular()),
          error: (_, _) => const Center(child: Text('タグ設定を読み込めませんでした。')),
          data: (tagSettings) {
            final matchingCategories = items
                .map((category) {
                  bool optionMatches(GalleryCategoryOption option) {
                    final eligible =
                        option.virtualFilter != null ||
                        (tagSettings.includes(option.fullTag) &&
                            !tagSettings.hides(option.fullTag));
                    return eligible &&
                        (tagSearch.isEmpty ||
                            _matchesTagSearch(
                              '${category.displayName} ${option.sectionPath ?? ''} ${option.name} ${option.fullTag}',
                              tagSearch,
                            ));
                  }

                  final options = category.options
                      .where(optionMatches)
                      .toList(growable: false);
                  return GalleryCategory(
                    path: category.path,
                    displayName: category.displayName,
                    count: category.count,
                    options: options,
                  );
                })
                .where((category) => category.options.isNotEmpty)
                .toList(growable: false);
            final matchingTagPaths = matchingCategories
                .map((category) => category.path)
                .toSet();
            final sort = ref.watch(gallerySortProvider);
            return Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
                  child: _settingsPanel(
                    context: context,
                    margin: EdgeInsets.zero,
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          ExpressiveMaterialScope(
                            theme: Theme.of(context),
                            child: M3EButtonGroup(
                              semanticLabel: '並べ替えの基準',
                              type: M3EButtonGroupType.connected,
                              style: M3EButtonStyle.tonal,
                              decoration: galleryChoiceButtonDecoration(
                                Theme.of(context).colorScheme,
                              ),
                              selectedIndex: GallerySortField.values.indexOf(
                                sort.field,
                              ),
                              selectionRequired: true,
                              onSelectedIndexChanged: (index) {
                                if (index == null) return;
                                ref
                                    .read(gallerySortProvider.notifier)
                                    .setField(GallerySortField.values[index]);
                              },
                              actions: [
                                for (final field in GallerySortField.values)
                                  M3EButtonGroupAction(
                                    label: Text(field.label),
                                  ),
                              ],
                            ),
                          ),
                          ExpressiveMaterialScope(
                            theme: Theme.of(context),
                            child: M3EButtonGroup(
                              semanticLabel: '並べ替えの向き',
                              type: M3EButtonGroupType.connected,
                              style: M3EButtonStyle.tonal,
                              decoration: galleryChoiceButtonDecoration(
                                Theme.of(context).colorScheme,
                              ),
                              selectedIndex: GallerySortDirection.values
                                  .indexOf(sort.direction),
                              selectionRequired: true,
                              onSelectedIndexChanged: (index) {
                                if (index == null) return;
                                ref
                                    .read(gallerySortProvider.notifier)
                                    .setDirection(
                                      GallerySortDirection.values[index],
                                    );
                              },
                              actions: [
                                for (final direction
                                    in GallerySortDirection.values)
                                  M3EButtonGroupAction(
                                    icon: Icon(
                                      direction ==
                                              GallerySortDirection.ascending
                                          ? Icons.arrow_upward
                                          : Icons.arrow_downward,
                                    ),
                                    label: Text(direction.label),
                                  ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                  child: Row(
                    children: [
                      Expanded(
                        child: ExpressiveMaterialScope(
                          theme: Theme.of(context),
                          child: M3ESearchBar(
                            controller: noteSearchController,
                            hintText: 'ノート名 / #タグ / -#タグ / &#タグ',
                            leading: const Icon(Icons.article_outlined),
                            textInputAction: TextInputAction.search,
                            onSubmitted: (_) => _applyNoteSearch(),
                            margin: 0,
                            focusedMargin: 0,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      M3EIconButton(
                        variant: M3EIconButtonVariant.tonal,
                        tooltip: 'ノートを検索',
                        onPressed: _applyNoteSearch,
                        icon: const Icon(Icons.search),
                      ),
                      M3EIconButton(
                        variant: M3EIconButtonVariant.standard,
                        tooltip: '検索と絞り込みをすべて解除',
                        onPressed: _clearAll,
                        icon: const Icon(Icons.backspace_outlined),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
                  child: Row(
                    children: [
                      Expanded(
                        child: ExpressiveMaterialScope(
                          theme: Theme.of(context),
                          child: M3ESearchBar(
                            controller: tagSearchController,
                            leading: const Icon(Icons.sell_outlined),
                            hintText: 'タグを検索',
                            textInputAction: TextInputAction.search,
                            margin: 0,
                            focusedMargin: 0,
                          ),
                        ),
                      ),
                      // 上段の検索・解除ボタン（8 + 48 + 48）と幅を揃える
                      const SizedBox(width: 104),
                    ],
                  ),
                ),
                _filterLegend(context),
                Expanded(
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
                    itemCount: matchingCategories.isEmpty
                        ? 1
                        : matchingCategories.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      if (matchingCategories.isEmpty) {
                        return const Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('一致するタグがありません。'),
                        );
                      }
                      final category = matchingCategories[index];
                      return _CategoryCard(
                        key: ValueKey(category.path),
                        category: category,
                        expanded:
                            tagSearch.isNotEmpty &&
                                matchingTagPaths.contains(category.path) ||
                            expandedCategories.contains(category.path),
                        onExpandedChanged: (expanded) {
                          if (tagSearch.isNotEmpty) return;
                          setState(() {
                            if (expanded) {
                              expandedCategories.add(category.path);
                            } else {
                              expandedCategories.remove(category.path);
                            }
                          });
                        },
                      );
                    },
                  ),
                ),
              ],
            );
          },
        );
      },
      skipLoadingOnReload: true,
    );
  }
}

class _FilterLegendItem extends StatelessWidget {
  const _FilterLegendItem({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Icon(icon, size: 16, color: Theme.of(context).colorScheme.primary),
      const SizedBox(width: 4),
      Text(label, style: Theme.of(context).textTheme.labelSmall),
    ],
  );
}

/// カテゴリごとの折りたたみ可能なタグ枠。色と形で選択状態を示す。
class _CategoryCard extends ConsumerWidget {
  const _CategoryCard({
    super.key,
    required this.category,
    required this.expanded,
    required this.onExpandedChanged,
  });

  final GalleryCategory category;
  final bool expanded;
  final ValueChanged<bool> onExpandedChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(selectedTagsProvider);
    final allRequired = ref.watch(allTagsProvider);
    final excluded = ref.watch(excludedTagsProvider);
    final selectedVirtual = ref.watch(selectedVirtualFiltersProvider);
    final hasSelection = category.options.any(
      (option) => option.virtualFilter == null
          ? selected.contains(option.fullTag) ||
                allRequired.contains(option.fullTag) ||
                excluded.contains(option.fullTag)
          : selectedVirtual.contains(
              GalleryVirtualFilter.fromKey(option.virtualFilter!),
            ),
    );
    final options = category.options;
    final colorScheme = Theme.of(context).colorScheme;

    return M3ECard(
      variant: M3ECardVariant.filled,
      focusable: false,
      showFocusRing: false,
      showFocusFill: false,
      trackHover: false,
      padding: EdgeInsets.zero,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      color: colorScheme.surfaceContainerLow,
      borderRadius: BorderRadius.circular(GalleryShape.medium),
      border: BorderSide(
        color: hasSelection ? colorScheme.primary : colorScheme.outlineVariant,
        width: hasSelection ? 1.5 : 1,
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 4, 12, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Semantics(
              expanded: expanded,
              child: M3ETappable(
                onTap: () => onExpandedChanged(!expanded),
                semanticLabel: category.displayName,
                builder: (context, state) => M3EStateLayerOverlay(
                  state: state,
                  color: colorScheme.primary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(GalleryShape.medium),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            category.displayName,
                            style: Theme.of(context).textTheme.titleMedium
                                ?.copyWith(fontWeight: FontWeight.w600),
                          ),
                        ),
                        if (hasSelection)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: Icon(
                              Icons.check_circle,
                              size: 18,
                              color: colorScheme.primary,
                            ),
                          ),
                        if (category.count case final count?)
                          _TagCountPill(count: count),
                        AnimatedRotation(
                          turns: expanded ? 0.5 : 0,
                          duration: GalleryMotion.duration(GalleryMotion.short),
                          curve: GalleryMotion.emphasizedCurve,
                          child: const Icon(Icons.expand_more),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            AnimatedSize(
              alignment: Alignment.topCenter,
              duration: GalleryMotion.duration(GalleryMotion.medium),
              curve: GalleryMotion.emphasizedCurve,
              child: expanded
                  ? Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (options.isEmpty)
                          const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8),
                            child: Text('該当する選択肢がありません'),
                          )
                        else ...[
                          for (final section in _optionSections(options))
                            _buildOptionSection(
                              context,
                              section.key == category.path
                                  ? ''
                                  : section.key.replaceFirst(
                                      '${category.path}/',
                                      '',
                                    ),
                              section.value,
                              selectedTags: selected,
                              allRequiredTags: allRequired,
                              selectedVirtualFilters: selectedVirtual,
                            ),
                        ],
                      ],
                    )
                  : const SizedBox.shrink(),
            ),
          ],
        ),
      ),
    );
  }

  List<MapEntry<String, List<GalleryCategoryOption>>> _optionSections(
    List<GalleryCategoryOption> options,
  ) {
    final sections = <String, List<GalleryCategoryOption>>{};
    for (final option in options) {
      sections
          .putIfAbsent(option.sectionPath ?? category.path, () => [])
          .add(option);
    }
    return sections.entries.toList(growable: false);
  }

  Widget _buildOptionSection(
    BuildContext context,
    String title,
    List<GalleryCategoryOption> options, {
    required Set<String> selectedTags,
    required Set<String> allRequiredTags,
    required Set<GalleryVirtualFilter> selectedVirtualFilters,
    int? count,
  }) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (count != null || title.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
                if (count != null) _TagCountPill(count: count),
              ],
            ),
          ),
        if (options.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: Text('該当する選択肢がありません'),
          )
        else
          M3EChipGroup(
            groupLabel: '$title のタグ',
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final option in options)
                  _OptionChip(
                    option: option,
                    selectedTags: selectedTags,
                    allRequiredTags: allRequiredTags,
                    selectedVirtualFilters: selectedVirtualFilters,
                    isWholeCategory:
                        option.fullTag == (option.sectionPath ?? category.path),
                  ),
              ],
            ),
          ),
      ],
    ),
  );
}

class _TagCountPill extends StatelessWidget {
  const _TagCountPill({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      label: '件数 $count',
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: scheme.secondaryContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          child: Text(
            '$count',
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: scheme.onSecondaryContainer,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}

class _OptionChip extends ConsumerWidget {
  const _OptionChip({
    required this.option,
    required this.selectedTags,
    required this.allRequiredTags,
    required this.selectedVirtualFilters,
    required this.isWholeCategory,
  });

  final GalleryCategoryOption option;
  final Set<String> selectedTags;
  final Set<String> allRequiredTags;
  final Set<GalleryVirtualFilter> selectedVirtualFilters;
  final bool isWholeCategory;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final virtualFilter = option.virtualFilter == null
        ? null
        : GalleryVirtualFilter.fromKey(option.virtualFilter!);
    final excludedTags = ref.watch(excludedTagsProvider);
    final isIncluded = selectedTags.contains(option.fullTag);
    final isAllRequired = allRequiredTags.contains(option.fullTag);
    final isExcluded = excludedTags.contains(option.fullTag);
    final selected = virtualFilter == null
        ? isIncluded || isAllRequired || isExcluded
        : selectedVirtualFilters.contains(virtualFilter);
    final filterState = isIncluded
        ? 'いずれかに含める'
        : isAllRequired
        ? 'すべてに含める'
        : isExcluded
        ? '除外'
        : '未選択';
    return Semantics(
      label: option.fullTag,
      value: '$filterState、${option.count} 件',
      enabled: !option.disabled,
      toggled: selected || isAllRequired,
      child: M3EChip(
        key: ValueKey('${option.virtualFilter ?? option.fullTag}:filter'),
        label: option.name,
        type: M3EChipType.filter,
        selected: selected || isAllRequired,
        leading: isIncluded
            ? const Icon(Icons.add, size: 16)
            : isAllRequired
            ? const Icon(Icons.done_all, size: 16)
            : isExcluded
            ? const Icon(Icons.remove, size: 16)
            : isWholeCategory
            ? const Icon(Icons.all_inclusive, size: 16)
            : null,
        trailing: Text(
          '${option.count}',
          maxLines: 1,
          softWrap: false,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurface,
            fontWeight: FontWeight.w600,
          ),
        ),
        onPressed: option.disabled
            ? null
            : () {
                if (virtualFilter == null) {
                  final included = ref.read(selectedTagsProvider.notifier);
                  final all = ref.read(allTagsProvider.notifier);
                  final excluded = ref.read(excludedTagsProvider.notifier);
                  if (isIncluded) {
                    included.toggle(option.fullTag);
                    all.toggle(option.fullTag);
                  } else if (isAllRequired) {
                    all.toggle(option.fullTag);
                    excluded.toggle(option.fullTag);
                  } else if (isExcluded) {
                    excluded.toggle(option.fullTag);
                  } else {
                    included.toggle(option.fullTag);
                  }
                } else {
                  ref
                      .read(selectedVirtualFiltersProvider.notifier)
                      .toggle(virtualFilter);
                }
              },
      ),
    );
  }
}
