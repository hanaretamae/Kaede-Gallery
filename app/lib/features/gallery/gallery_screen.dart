import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

import '../../core_api/gallery_providers.dart';
import '../../core_api/gallery_repository.dart';

class GalleryScreen extends ConsumerWidget {
  const GalleryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(vaultSessionProvider);
    return session.when(
      loading: () => const _LoadingScreen(),
      error: (_, _) => _VaultErrorScreen(
        onChooseVault: () =>
            ref.read(vaultSessionProvider.notifier).chooseVault(),
      ),
      data: (vault) {
        if (vault == null) {
          return _VaultPickerScreen(
            onChooseVault: () =>
                ref.read(vaultSessionProvider.notifier).chooseVault(),
          );
        }
        return _GalleryLayout(session: vault);
      },
    );
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();

  @override
  Widget build(BuildContext context) =>
      const Scaffold(body: Center(child: CircularProgressIndicator()));
}

class _VaultPickerScreen extends StatelessWidget {
  const _VaultPickerScreen({required this.onChooseVault});

  final VoidCallback onChooseVault;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(),
    body: Center(
      child: FilledButton.icon(
        onPressed: onChooseVault,
        icon: const Icon(Icons.folder_open),
        label: const Text('Vault を選択'),
      ),
    ),
  );
}

class _VaultErrorScreen extends StatelessWidget {
  const _VaultErrorScreen({required this.onChooseVault});

  final VoidCallback onChooseVault;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(),
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 40),
          const SizedBox(height: 12),
          const Text('Vault を開けませんでした。場所とアクセス権を確認してください。'),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: onChooseVault,
            child: const Text('別の Vault を選択'),
          ),
        ],
      ),
    ),
  );
}

class _GalleryLayout extends ConsumerWidget {
  const _GalleryLayout({required this.session});

  final VaultSession session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wide = MediaQuery.sizeOf(context).width >= 900;
    final filters = ref.watch(selectedTagsProvider);
    final virtualFilters = ref.watch(selectedVirtualFiltersProvider);
    final vaultName = p.basename(session.vaultPath);
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(vaultName, overflow: TextOverflow.ellipsis),
            Text(
              '${session.scanReport.notesIndexed} 件',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
        actions: [
          if (!wide)
            IconButton(
              tooltip: 'タグで絞り込む',
              onPressed: () => showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                builder: (context) => const _TagPanel(),
              ),
              icon: const Icon(Icons.tune),
            ),
          IconButton(
            tooltip: '再走査',
            onPressed: () => ref.read(vaultSessionProvider.notifier).rescan(),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Vault を切り替え',
            onPressed: () =>
                ref.read(vaultSessionProvider.notifier).chooseVault(),
            icon: const Icon(Icons.folder_open),
          ),
        ],
      ),
      body: Column(
        children: [
          _ActiveFilters(filters: filters, virtualFilters: virtualFilters),
          if (session.scanReport.warnings > 0)
            MaterialBanner(
              content: Text(
                '読み込めなかった項目が ${session.scanReport.warnings} 件あります。再走査で再試行できます。',
              ),
              actions: [
                TextButton(
                  onPressed: () =>
                      ref.read(vaultSessionProvider.notifier).rescan(),
                  child: const Text('再走査'),
                ),
              ],
            ),
          Expanded(
            child: Row(
              children: [
                if (wide) const SizedBox(width: 320, child: _TagPanel()),
                if (wide) const VerticalDivider(width: 1),
                const Expanded(child: _GalleryGrid()),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActiveFilters extends ConsumerWidget {
  const _ActiveFilters({required this.filters, required this.virtualFilters});

  final Set<String> filters;
  final Set<GalleryVirtualFilter> virtualFilters;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (filters.isEmpty && virtualFilters.isEmpty) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      child: Row(
        children: [
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  for (final filter in filters)
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: InputChip(
                        label: Text(filter),
                        onDeleted: () => ref
                            .read(selectedTagsProvider.notifier)
                            .toggle(filter),
                      ),
                    ),
                  for (final filter in virtualFilters)
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: InputChip(
                        label: Text(filter.label),
                        onDeleted: () => ref
                            .read(selectedVirtualFiltersProvider.notifier)
                            .toggle(filter),
                      ),
                    ),
                ],
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              ref.read(selectedTagsProvider.notifier).clear();
              ref.read(selectedVirtualFiltersProvider.notifier).clear();
            },
            child: const Text('すべて解除'),
          ),
        ],
      ),
    );
  }
}

class _TagPanel extends ConsumerWidget {
  const _TagPanel();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(galleryCategoriesProvider);
    return categories.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => const Center(child: Text('タグ一覧を読み込めませんでした。')),
      data: (items) => ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Text('タグ', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          for (final category in items)
            _CategoryCard(key: ValueKey(category.path), category: category),
          if (items.isEmpty)
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('タグがありません。先に Vault を走査してください。'),
            ),
        ],
      ),
    );
  }
}

class _CategoryCard extends ConsumerStatefulWidget {
  const _CategoryCard({super.key, required this.category});

  final GalleryCategory category;

  @override
  ConsumerState<_CategoryCard> createState() => _CategoryCardState();
}

class _CategoryCardState extends ConsumerState<_CategoryCard> {
  late final TextEditingController searchController;
  String search = '';

  @override
  void initState() {
    super.initState();
    searchController = TextEditingController()..addListener(_updateSearch);
  }

  @override
  void dispose() {
    searchController
      ..removeListener(_updateSearch)
      ..dispose();
    super.dispose();
  }

  void _updateSearch() {
    final value = searchController.value;
    if (value.composing.isValid && !value.composing.isCollapsed) {
      return;
    }
    if (search != value.text) {
      setState(() => search = value.text);
    }
  }

  @override
  Widget build(BuildContext context) {
    final selected = ref.watch(selectedTagsProvider);
    final selectedVirtual = ref.watch(selectedVirtualFiltersProvider);
    final options = widget.category.options
        .where(
          (option) => option.name.toLowerCase().contains(search.toLowerCase()),
        )
        .toList(growable: false);
    final optionRows = (options.length / 2).ceil().clamp(1, 8);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    widget.category.displayName,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text('${widget.category.count}'),
              ],
            ),
            if (widget.category.options.length > 30)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: TextField(
                  controller: searchController,
                  decoration: const InputDecoration(
                    isDense: true,
                    prefixIcon: Icon(Icons.search),
                    hintText: '検索',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
            const SizedBox(height: 8),
            if (options.isEmpty)
              const SizedBox(
                height: 36,
                child: Center(child: Text('該当する選択肢がありません')),
              )
            else
              SizedBox(
                height: optionRows * 40,
                child: GridView.builder(
                  physics: options.length > 16
                      ? null
                      : const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisExtent: 36,
                    crossAxisSpacing: 6,
                    mainAxisSpacing: 4,
                  ),
                  itemCount: options.length,
                  itemBuilder: (context, index) => _OptionChip(
                    option: options[index],
                    selectedTags: selected,
                    selectedVirtualFilters: selectedVirtual,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _OptionChip extends ConsumerWidget {
  const _OptionChip({
    required this.option,
    required this.selectedTags,
    required this.selectedVirtualFilters,
  });

  final GalleryCategoryOption option;
  final Set<String> selectedTags;
  final Set<GalleryVirtualFilter> selectedVirtualFilters;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final virtualFilter = option.virtualFilter == null
        ? null
        : GalleryVirtualFilter.fromKey(option.virtualFilter!);
    return FilterChip(
      label: SizedBox(
        width: double.infinity,
        child: Row(
          children: [
            Expanded(
              child: Text(
                option.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 4),
            Text(
              '${option.count}',
              style: Theme.of(context).textTheme.labelSmall,
            ),
          ],
        ),
      ),
      selected: virtualFilter == null
          ? selectedTags.contains(option.fullTag)
          : selectedVirtualFilters.contains(virtualFilter),
      onSelected: option.disabled
          ? null
          : (_) {
              if (virtualFilter == null) {
                ref.read(selectedTagsProvider.notifier).toggle(option.fullTag);
              } else {
                ref
                    .read(selectedVirtualFiltersProvider.notifier)
                    .toggle(virtualFilter);
              }
            },
    );
  }
}

class _GalleryGrid extends ConsumerWidget {
  const _GalleryGrid();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final gallery = ref.watch(galleryItemsProvider);
    return gallery.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => const Center(child: Text('一覧を読み込めませんでした。')),
      data: (notes) {
        if (notes.isEmpty) {
          return const Center(child: Text('該当する note はありません。'));
        }
        return LayoutBuilder(
          builder: (context, constraints) {
            final columns = (constraints.maxWidth / 190)
                .floor()
                .clamp(2, 8)
                .toInt();
            return NotificationListener<ScrollNotification>(
              onNotification: (notification) {
                if (notification.metrics.pixels >
                    notification.metrics.maxScrollExtent - 600) {
                  ref.read(galleryItemsProvider.notifier).loadMore();
                }
                return false;
              },
              child: GridView.builder(
                padding: const EdgeInsets.all(12),
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: columns,
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 10,
                  childAspectRatio: 0.78,
                ),
                itemCount: notes.length,
                itemBuilder: (context, index) =>
                    _GalleryTile(note: notes[index]),
              ),
            );
          },
        );
      },
    );
  }
}

class _GalleryTile extends ConsumerWidget {
  const _GalleryTile({required this.note});

  final GalleryNote note;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mediaId = note.representativeMediaId;
    final thumbnail = mediaId == null
        ? null
        : ref.watch(galleryThumbnailProvider(mediaId));
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (thumbnail == null)
                  _MediaPlaceholder(hasVideo: note.videoCount > 0)
                else
                  thumbnail.when(
                    loading: () => const _MediaPlaceholder(),
                    error: (_, _) =>
                        _MediaPlaceholder(hasVideo: note.videoCount > 0),
                    data: (bytes) => bytes == null
                        ? _MediaPlaceholder(hasVideo: note.videoCount > 0)
                        : Image.memory(
                            bytes,
                            fit: BoxFit.cover,
                            gaplessPlayback: true,
                            filterQuality: FilterQuality.low,
                          ),
                  ),
                if (note.videoCount > 0)
                  const Positioned(
                    right: 8,
                    bottom: 8,
                    child: Icon(Icons.play_circle_outline, size: 30),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
            child: Text(
              '${note.mediaCount} 件のメディア',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        ],
      ),
    );
  }
}

class _MediaPlaceholder extends StatelessWidget {
  const _MediaPlaceholder({this.hasVideo = false});

  final bool hasVideo;

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: Theme.of(context).colorScheme.surfaceContainerHighest,
    child: Center(
      child: Icon(
        hasVideo ? Icons.movie_outlined : Icons.image_not_supported_outlined,
        size: 42,
      ),
    ),
  );
}
