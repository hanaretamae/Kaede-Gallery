part of 'gallery_screen.dart';

class _GalleryGrid extends ConsumerWidget {
  const _GalleryGrid();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(galleryDisplayModeProvider);
    return mode == GalleryDisplayMode.byNote
        ? const _NoteGrid()
        : const _MediaGrid();
  }
}

class _NoteGrid extends ConsumerWidget {
  const _NoteGrid();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final gallery = ref.watch(galleryItemsProvider);
    final isLoadingMore = ref.watch(galleryItemsLoadingMoreProvider);
    final dataOffset = ref.watch(galleryNotesDataOffsetProvider);
    return gallery.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => const Center(child: Text('一覧を読み込めませんでした。')),
      data: (notes) {
        if (notes.isEmpty) {
          return const Center(child: Text('該当するノートはありません。'));
        }
        return _ThumbnailGrid(
          key: ValueKey(
            'note-grid-${ref.watch(selectedTagsProvider)}-'
            '${ref.watch(excludedTagsProvider)}-'
            '${ref.watch(selectedVirtualFiltersProvider)}-'
            '${ref.watch(gallerySearchQueryProvider)}',
          ),
          itemCount: notes.length,
          itemKeys: [
            for (final note in notes) ValueKey<String>('note-${note.id}'),
          ],
          itemIndicesByKey: {
            for (var index = 0; index < notes.length; index++)
              ValueKey<String>('note-${notes[index].id}'): index,
          },
          isLoadingMore: isLoadingMore,
          hasMore: ref.read(galleryItemsProvider.notifier).hasMore,
          hasPrevious: ref.read(galleryItemsProvider.notifier).hasPrevious,
          dataOffset: dataOffset,
          targetIndex: ref.watch(galleryJumpTargetProvider) == null
              ? null
              : ref.watch(galleryJumpTargetProvider)! - dataOffset,
          targetReady:
              ref.watch(galleryJumpStatusProvider) != GalleryJumpStatus.loading,
          pageSize: ref.watch(galleryPageSizeProvider),
          onNearEnd: () => ref.read(galleryItemsProvider.notifier).loadMore(),
          onNearStart: (preparePrepend) => ref
              .read(galleryItemsProvider.notifier)
              .loadPrevious(beforePrepend: preparePrepend),
          onTargetConsumed: (target) {
            if (ref.read(galleryNotesDataOffsetProvider) != dataOffset) {
              return;
            }
            final absoluteTarget = dataOffset + target;
            final pageStart =
                ((absoluteTarget ~/ ref.read(galleryPageSizeProvider)) *
                        ref.read(galleryPageSizeProvider))
                    .toInt();
            ref.read(galleryVisiblePageStartProvider.notifier).set(pageStart);
            if (ref.read(galleryJumpTargetProvider) == absoluteTarget) {
              ref
                  .read(galleryJumpStatusProvider.notifier)
                  .set(GalleryJumpStatus.completed);
              ref.read(galleryJumpTargetProvider.notifier).set(null);
            }
          },
          onTargetNotFound: () {
            ref
                .read(galleryJumpStatusProvider.notifier)
                .set(GalleryJumpStatus.notFound);
            ref.read(galleryJumpTargetProvider.notifier).set(null);
          },
          onVisiblePageChanged: (start) {
            if (ref.read(galleryNotesDataOffsetProvider) != dataOffset) {
              return;
            }
            final absoluteStart = dataOffset + start;
            if (ref.read(galleryVisiblePageStartProvider) != absoluteStart) {
              ref
                  .read(galleryVisiblePageStartProvider.notifier)
                  .set(absoluteStart);
            }
          },
          onPrefetchIndex: (index) {
            final mediaId = notes[index].representativeMediaId;
            if (mediaId != null) _prefetchGalleryThumbnail(ref, mediaId);
          },
          itemBuilder: (context, index) => _GalleryTile(
            note: notes[index],
            itemNumber: dataOffset + index + 1,
          ),
        );
      },
      skipLoadingOnReload: true,
    );
  }
}

class _MediaGrid extends ConsumerWidget {
  const _MediaGrid();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final media = ref.watch(galleryMediaItemsProvider);
    final isLoadingMore = ref.watch(galleryMediaItemsLoadingMoreProvider);
    final dataOffset = ref.watch(galleryMediaDataOffsetProvider);
    return media.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => const Center(child: Text('一覧を読み込めませんでした。')),
      data: (items) {
        if (items.isEmpty) {
          return const Center(child: Text('該当するメディアはありません。'));
        }
        return _ThumbnailGrid(
          key: ValueKey(
            'media-grid-${ref.watch(selectedTagsProvider)}-'
            '${ref.watch(excludedTagsProvider)}-'
            '${ref.watch(selectedVirtualFiltersProvider)}-'
            '${ref.watch(gallerySearchQueryProvider)}',
          ),
          itemCount: items.length,
          itemKeys: [
            for (final item in items) ValueKey<String>('media-${item.id}'),
          ],
          itemIndicesByKey: {
            for (var index = 0; index < items.length; index++)
              ValueKey<String>('media-${items[index].id}'): index,
          },
          isLoadingMore: isLoadingMore,
          hasMore: ref.read(galleryMediaItemsProvider.notifier).hasMore,
          hasPrevious: ref.read(galleryMediaItemsProvider.notifier).hasPrevious,
          dataOffset: dataOffset,
          targetIndex: ref.watch(galleryJumpTargetProvider) == null
              ? null
              : ref.watch(galleryJumpTargetProvider)! - dataOffset,
          targetReady:
              ref.watch(galleryJumpStatusProvider) != GalleryJumpStatus.loading,
          pageSize: ref.watch(galleryPageSizeProvider),
          onNearEnd: () =>
              ref.read(galleryMediaItemsProvider.notifier).loadMore(),
          onNearStart: (preparePrepend) => ref
              .read(galleryMediaItemsProvider.notifier)
              .loadPrevious(beforePrepend: preparePrepend),
          onTargetConsumed: (target) {
            if (ref.read(galleryMediaDataOffsetProvider) != dataOffset) {
              return;
            }
            final absoluteTarget = dataOffset + target;
            final pageStart =
                ((absoluteTarget ~/ ref.read(galleryPageSizeProvider)) *
                        ref.read(galleryPageSizeProvider))
                    .toInt();
            ref.read(galleryVisiblePageStartProvider.notifier).set(pageStart);
            if (ref.read(galleryJumpTargetProvider) == absoluteTarget) {
              ref
                  .read(galleryJumpStatusProvider.notifier)
                  .set(GalleryJumpStatus.completed);
              ref.read(galleryJumpTargetProvider.notifier).set(null);
            }
          },
          onTargetNotFound: () {
            ref
                .read(galleryJumpStatusProvider.notifier)
                .set(GalleryJumpStatus.notFound);
            ref.read(galleryJumpTargetProvider.notifier).set(null);
          },
          onVisiblePageChanged: (start) {
            if (ref.read(galleryMediaDataOffsetProvider) != dataOffset) {
              return;
            }
            final absoluteStart = dataOffset + start;
            if (ref.read(galleryVisiblePageStartProvider) != absoluteStart) {
              ref
                  .read(galleryVisiblePageStartProvider.notifier)
                  .set(absoluteStart);
            }
          },
          onPrefetchIndex: (index) =>
              _prefetchGalleryThumbnail(ref, items[index].id),
          itemBuilder: (context, index) => _MediaTile(
            item: items[index],
            itemNumber: dataOffset + index + 1,
          ),
        );
      },
      skipLoadingOnReload: true,
    );
  }
}

void _prefetchGalleryThumbnail(WidgetRef ref, int mediaId) {
  final session = ref.read(vaultSessionProvider).asData?.value;
  if (session == null || !session.vaultPath.startsWith('content://')) return;
  final repository = ref.read(galleryRepositoryProvider);
  unawaited(
    repository
        .getThumbnail(
          session.vaultPath,
          session.paths.indexPath,
          session.paths.thumbnailDirectory,
          mediaId,
        )
        .then<void>(
          (_) {},
          onError: (Object error, StackTrace stackTrace) {
            FlutterError.reportError(
              FlutterErrorDetails(
                exception: error,
                stack: stackTrace,
                library: 'gallery thumbnail prefetch',
              ),
            );
          },
        ),
  );
}

class _ThumbnailGrid extends StatefulWidget {
  const _ThumbnailGrid({
    super.key,
    required this.itemCount,
    required this.itemKeys,
    required this.itemIndicesByKey,
    required this.itemBuilder,
    required this.onNearEnd,
    required this.onNearStart,
    required this.isLoadingMore,
    required this.hasMore,
    required this.hasPrevious,
    required this.dataOffset,
    required this.targetIndex,
    required this.targetReady,
    required this.pageSize,
    required this.onTargetConsumed,
    required this.onTargetNotFound,
    required this.onVisiblePageChanged,
    required this.onPrefetchIndex,
  });

  final int itemCount;
  final List<Key> itemKeys;
  final Map<Key, int> itemIndicesByKey;
  final IndexedWidgetBuilder itemBuilder;
  final VoidCallback onNearEnd;
  final ValueChanged<void Function(int)> onNearStart;
  final bool isLoadingMore;
  final bool hasMore;
  final bool hasPrevious;
  final int dataOffset;
  final int? targetIndex;
  final bool targetReady;
  final int pageSize;
  final ValueChanged<int> onTargetConsumed;
  final VoidCallback onTargetNotFound;
  final ValueChanged<int> onVisiblePageChanged;
  final ValueChanged<int> onPrefetchIndex;

  @override
  State<_ThumbnailGrid> createState() => _ThumbnailGridState();
}

class _ThumbnailGridState extends State<_ThumbnailGrid> {
  final ScrollController _scrollController = ScrollController();
  int? _handledTarget;
  int? _requestedAtCount;
  bool _requestedPrevious = false;
  int? _highlightedAbsoluteIndex;
  Timer? _highlightTimer;
  int _columns = 2;
  double _rowExtent = 1;
  final Set<int> _prefetchedIndices = {};

  @override
  void initState() {
    super.initState();
    _scheduleTargetCheck();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _scrollController.hasClients) {
        _prefetchAhead(_scrollController.position);
      }
    });
  }

  @override
  void didUpdateWidget(covariant _ThumbnailGrid oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.dataOffset < oldWidget.dataOffset &&
        _scrollController.hasClients) {
      _highlightTimer?.cancel();
      _highlightedAbsoluteIndex = null;
      _requestedPrevious = false;
      _prefetchedIndices.clear();
    }
    if (oldWidget.isLoadingMore && !widget.isLoadingMore) {
      _requestedPrevious = false;
    }
    if (widget.targetIndex != oldWidget.targetIndex &&
        widget.targetIndex != null) {
      _handledTarget = null;
      _requestedAtCount = null;
      _scheduleTargetCheck();
    } else if (widget.itemCount != oldWidget.itemCount ||
        widget.isLoadingMore != oldWidget.isLoadingMore ||
        widget.hasMore != oldWidget.hasMore ||
        widget.targetReady != oldWidget.targetReady) {
      _scheduleTargetCheck();
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && _scrollController.hasClients) {
          _prefetchAhead(_scrollController.position);
        }
      });
    }
  }

  @override
  void dispose() {
    _highlightTimer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _scheduleTargetCheck() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _checkJumpTarget();
    });
  }

  void _checkJumpTarget() {
    if (widget.targetIndex != null && !widget.targetReady) return;
    final target = widget.targetIndex;
    if (target == null || target == _handledTarget) return;
    if (target < widget.itemCount) {
      if (!_scrollController.hasClients) {
        _scheduleTargetCheck();
        return;
      }

      final row = target ~/ _columns;
      final offset = (12 + row * _rowExtent).clamp(
        0.0,
        _scrollController.position.maxScrollExtent,
      );
      _scrollController.jumpTo(offset);
      _handledTarget = target;
      _highlightTimer?.cancel();
      setState(() {
        _highlightedAbsoluteIndex = widget.dataOffset + target;
      });
      widget.onTargetConsumed(target);
      _highlightTimer = Timer(const Duration(milliseconds: 1500), () {
        if (mounted) setState(() => _highlightedAbsoluteIndex = null);
      });
      return;
    }
    if (widget.isLoadingMore) return;
    if (widget.hasMore && _requestedAtCount != widget.itemCount) {
      _requestedAtCount = widget.itemCount;
      widget.onNearEnd();
      return;
    }
    if (!widget.hasMore) {
      _handledTarget = target;
      widget.onTargetNotFound();
    }
  }

  void _prepareForPrepend(int insertedCount) {
    if (!_scrollController.hasClients || insertedCount <= 0) return;
    final metrics = _scrollController.position;
    final firstVisibleItem =
        ((metrics.pixels - 12) / _rowExtent).floor().clamp(
          0,
          widget.itemCount - 1,
        ) *
        _columns;
    final oldRow = firstVisibleItem ~/ _columns;
    final newRow = (firstVisibleItem + insertedCount) ~/ _columns;
    final restoredOffset = metrics.pixels + (newRow - oldRow) * _rowExtent;
    _scrollController.jumpTo(
      restoredOffset.clamp(metrics.minScrollExtent, metrics.maxScrollExtent),
    );
  }

  void _prefetchAhead(ScrollMetrics metrics) {
    if (widget.itemCount == 0 || _rowExtent <= 0) return;
    final firstVisibleRow =
        ((metrics.pixels - 12).clamp(0.0, double.infinity) / _rowExtent)
            .floor();
    final visibleRows = (metrics.viewportDimension / _rowExtent).ceil();
    final startIndex = (firstVisibleRow + visibleRows) * _columns;
    final endIndex = (startIndex + 2 * _columns).clamp(0, widget.itemCount);
    _prefetchedIndices.removeWhere(
      (index) => index < firstVisibleRow * _columns - 2 * _columns,
    );
    for (var index = startIndex; index < endIndex; index++) {
      if (_prefetchedIndices.add(index)) widget.onPrefetchIndex(index);
    }
  }

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      _columns = (constraints.maxWidth / 190).floor().clamp(2, 8).toInt();
      final gridWidth = constraints.maxWidth - 24;
      final tileWidth = (gridWidth - (_columns - 1) * 10) / _columns;
      _rowExtent = tileWidth / (4 / 5) + 10;
      _scheduleTargetCheck();
      return NotificationListener<ScrollNotification>(
        onNotification: (notification) {
          if (notification is UserScrollNotification &&
              notification.direction != ScrollDirection.idle &&
              _highlightedAbsoluteIndex != null) {
            _highlightTimer?.cancel();
            setState(() => _highlightedAbsoluteIndex = null);
          }
          if (notification is ScrollUpdateNotification ||
              notification is ScrollEndNotification) {
            _prefetchAhead(notification.metrics);
            final top = (notification.metrics.pixels - 12).clamp(
              0.0,
              double.infinity,
            );
            final firstVisibleIndex = (top / _rowExtent).floor() * _columns;
            final pageStart =
                (firstVisibleIndex ~/ widget.pageSize) * widget.pageSize;
            widget.onVisiblePageChanged(pageStart);
          }
          final scrollDelta = notification is ScrollUpdateNotification
              ? notification.scrollDelta
              : null;
          if (scrollDelta != null &&
              scrollDelta > 0 &&
              !widget.isLoadingMore &&
              notification.metrics.pixels >
                  notification.metrics.maxScrollExtent - 600) {
            widget.onNearEnd();
          }
          if (scrollDelta != null &&
              scrollDelta < 0 &&
              !widget.isLoadingMore &&
              widget.hasPrevious &&
              notification.metrics.pixels < 120 &&
              !_requestedPrevious) {
            _requestedPrevious = true;
            widget.onNearStart(_prepareForPrepend);
          }
          return false;
        },
        child: CustomScrollView(
          controller: _scrollController,
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.all(12),
              sliver: SliverGrid(
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: _columns,
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 10,
                  childAspectRatio: 4 / 5,
                ),
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final child = widget.itemBuilder(context, index);
                    final highlighted =
                        _highlightedAbsoluteIndex == widget.dataOffset + index
                        ? Stack(
                            fit: StackFit.expand,
                            children: [
                              child,
                              _GalleryJumpHighlight(
                                key: ValueKey(
                                  'gallery-jump-highlight-${widget.dataOffset + index}',
                                ),
                                color: Theme.of(context).colorScheme.primary,
                                fill: Theme.of(context)
                                    .colorScheme
                                    .primaryContainer,
                              ),
                            ],
                          )
                        : child;
                    return KeyedSubtree(
                      key: widget.itemKeys[index],
                      child: highlighted,
                    );
                  },
                  childCount: widget.itemCount,
                  findChildIndexCallback: (key) => widget.itemIndicesByKey[key],
                ),
              ),
            ),
            if (widget.isLoadingMore)
              const SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: Center(
                    child: SizedBox(
                      width: 28,
                      height: 28,
                      child: CircularProgressIndicator(strokeWidth: 3),
                    ),
                  ),
                ),
              ),
          ],
        ),
      );
    },
  );
}

class _GalleryJumpHighlight extends StatefulWidget {
  const _GalleryJumpHighlight({
    super.key,
    required this.color,
    required this.fill,
  });

  final Color color;
  final Color fill;

  @override
  State<_GalleryJumpHighlight> createState() => _GalleryJumpHighlightState();
}

class _GalleryJumpHighlightState extends State<_GalleryJumpHighlight>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..forward();

  late final Animation<double> _pulse = TweenSequence<double>([
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 0,
        end: 0.34,
      ).chain(CurveTween(curve: GalleryMotion.emphasizedCurve)),
      weight: 22,
    ),
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 0.34,
        end: 0.04,
      ).chain(CurveTween(curve: GalleryMotion.emphasizedCurve)),
      weight: 20,
    ),
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 0.04,
        end: 0.30,
      ).chain(CurveTween(curve: GalleryMotion.emphasizedCurve)),
      weight: 22,
    ),
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 0.30,
        end: 0,
      ).chain(CurveTween(curve: GalleryMotion.emphasizedCurve)),
      weight: 36,
    ),
  ]).animate(_controller);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => IgnorePointer(
    child: AnimatedBuilder(
      animation: _pulse,
      builder: (context, child) => DecoratedBox(
        decoration: BoxDecoration(
          border: Border.all(
            color: widget.color.withValues(alpha: _pulse.value),
            width: 4,
          ),
          borderRadius: BorderRadius.circular(GalleryShape.medium),
          color: widget.fill.withValues(alpha: _pulse.value * 0.55),
          boxShadow: [
            BoxShadow(
              color: widget.color.withValues(alpha: _pulse.value * 0.3),
              blurRadius: 20,
              spreadRadius: 2,
            ),
          ],
        ),
      ),
    ),
  );
}

class _GalleryTile extends ConsumerWidget {
  const _GalleryTile({required this.note, required this.itemNumber});

  final GalleryNote note;
  final int itemNumber;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mediaId = note.representativeMediaId;
    final isGrouped =
        ref.watch(galleryDisplayModeProvider) == GalleryDisplayMode.byNote;
    final showItemNumber = ref.watch(galleryShowTileItemNumberProvider);
    final thumbnail = mediaId == null
        ? null
        : ref.watch(galleryThumbnailProvider(mediaId));
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.medium),
      ),
      child: InkWell(
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute<void>(
            builder: (context) => _NoteViewerScreen(
              noteId: note.id,
              initialMediaId: mediaId,
              initialPreview: thumbnail?.asData?.value,
            ),
          ),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (thumbnail == null)
              _MediaPlaceholder(hasVideo: note.videoCount > 0)
            else
              _GalleryTileThumbnail(
                mediaId: mediaId,
                isVideo:
                    note.mediaCount > 0 && note.videoCount == note.mediaCount,
                thumbnail: thumbnail,
              ),
            if (note.videoCount > 0)
              const Positioned(
                right: 8,
                bottom: 8,
                child: Icon(Icons.play_circle_outline, size: 30),
              ),
            if (isGrouped &&
                (note.mediaCount > 1 ||
                    note.memoCount > 0 ||
                    note.relatedCount > 0))
              Positioned(
                left: 8,
                bottom: 8,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.68),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (note.mediaCount > 1)
                        _TileCount(
                          icon: Icons.photo_library_outlined,
                          count: note.mediaCount,
                        ),
                      if (note.memoCount > 0)
                        _TileCount(
                          icon: Icons.sticky_note_2_outlined,
                          count: note.memoCount,
                        ),
                      if (note.relatedCount > 0)
                        _TileCount(icon: Icons.link, count: note.relatedCount),
                    ],
                  ),
                ),
              ),
            if (showItemNumber) _TileOrdinalBadge(itemNumber: itemNumber),
          ],
        ),
      ),
    );
  }
}

class _TileCount extends StatelessWidget {
  const _TileCount({required this.icon, required this.count});

  final IconData icon;
  final int count;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(horizontal: 3),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: Colors.white),
        const SizedBox(width: 3),
        Text(
          '$count',
          style: Theme.of(context).textTheme.labelSmall
              ?.copyWith(color: Colors.white),
        ),
      ],
    ),
  );
}

class _TileOrdinalBadge extends StatelessWidget {
  const _TileOrdinalBadge({required this.itemNumber});

  final int itemNumber;

  @override
  Widget build(BuildContext context) => Positioned(
    top: 8,
    left: 8,
    child: Container(
      key: ValueKey('gallery-item-number-$itemNumber'),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.68),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(
        '$itemNumber',
        style: Theme.of(context).textTheme.labelSmall
            ?.copyWith(color: Colors.white),
      ),
    ),
  );
}

class _MediaTile extends ConsumerWidget {
  const _MediaTile({required this.item, required this.itemNumber});

  final GalleryMediaItem item;
  final int itemNumber;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final thumbnail = ref.watch(galleryThumbnailProvider(item.id));
    final showItemNumber = ref.watch(galleryShowTileItemNumberProvider);
    final showCounts =
        item.mediaCount > 1 || item.memoCount > 0 || item.relatedCount > 0;
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.medium),
      ),
      child: InkWell(
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute<void>(
            builder: (context) => _NoteViewerScreen(
              noteId: item.noteId,
              initialMediaId: item.id,
              initialPreview: thumbnail.asData?.value,
            ),
          ),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            _GalleryTileThumbnail(
              mediaId: item.id,
              isVideo: item.isVideo,
              thumbnail: thumbnail,
            ),
            if (item.isVideo)
              const Positioned(
                right: 8,
                bottom: 8,
                child: Icon(Icons.play_circle_outline, size: 30),
              ),
            if (showCounts)
              Positioned(
                left: 8,
                bottom: 8,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.68),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (item.mediaCount > 1)
                        _TileCount(
                          icon: Icons.photo_library_outlined,
                          count: item.mediaCount,
                        ),
                      if (item.memoCount > 0)
                        _TileCount(
                          icon: Icons.sticky_note_2_outlined,
                          count: item.memoCount,
                        ),
                      if (item.relatedCount > 0)
                        _TileCount(icon: Icons.link, count: item.relatedCount),
                    ],
                  ),
                ),
              ),
            if (showItemNumber) _TileOrdinalBadge(itemNumber: itemNumber),
          ],
        ),
      ),
    );
  }
}

class _GalleryTileThumbnail extends ConsumerWidget {
  const _GalleryTileThumbnail({
    required this.mediaId,
    required this.isVideo,
    required this.thumbnail,
  });

  final int? mediaId;
  final bool isVideo;
  final AsyncValue<Uint8List?> thumbnail;

  @override
  Widget build(BuildContext context, WidgetRef ref) => LayoutBuilder(
    builder: (context, constraints) {
      final cacheWidth =
          (constraints.maxWidth * MediaQuery.devicePixelRatioOf(context))
              .ceil()
              .clamp(160, 2048)
              .toInt();
      Widget image(Uint8List bytes) => Image.memory(
        bytes,
        cacheWidth: cacheWidth,
        fit: BoxFit.cover,
        gaplessPlayback: true,
        filterQuality: FilterQuality.low,
        errorBuilder: (context, error, stackTrace) =>
            _MediaPlaceholder(hasVideo: isVideo),
      );

      return thumbnail.when(
        loading: () => _MediaPlaceholder(hasVideo: isVideo),
        error: (_, _) => _sourceOrPlaceholder(context, ref, cacheWidth),
        data: (bytes) => bytes != null
            ? image(bytes)
            : _sourceOrPlaceholder(context, ref, cacheWidth),
      );
    },
  );

  Widget _sourceOrPlaceholder(
    BuildContext context,
    WidgetRef ref,
    int cacheWidth,
  ) {
    final id = mediaId;
    if (id == null || isVideo) {
      return _MediaPlaceholder(hasVideo: isVideo);
    }
    final path = ref.watch(galleryMediaSourcePathProvider(id));
    return path.when(
      loading: () => const _MediaPlaceholder(),
      error: (_, _) => const _MediaPlaceholder(),
      data: (value) => value == null
          ? const _MediaPlaceholder()
          : Image.file(
              File(value),
              cacheWidth: cacheWidth,
              fit: BoxFit.cover,
              gaplessPlayback: true,
              filterQuality: FilterQuality.low,
              errorBuilder: (context, error, stackTrace) =>
                  const _MediaPlaceholder(),
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
