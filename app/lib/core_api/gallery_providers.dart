import 'dart:typed_data';

import 'package:fc_native_video_thumbnail/fc_native_video_thumbnail.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'gallery_repository.dart';
import 'gallery_tag_settings.dart';
import 'rust_gallery_repository.dart';
import '../platform/vault_platform.dart';

/// Number of items fetched per gallery page when no pagination setting has
/// loaded yet. The configurable default (also 24) lives in
/// [GalleryPaginationSettings.defaultPageSize].
const galleryPageSize = GalleryPaginationSettings.defaultPageSize;
const galleryAllFilterPrefix = '\u001fAND:';

/// Current configured gallery page size, falling back to the default while
/// settings are still loading or unavailable.
final galleryPageSizeProvider = Provider<int>((ref) {
  final settings = ref.watch(galleryTagSettingsProvider).asData?.value;
  return settings?.pagination.pageSize ?? galleryPageSize;
});

/// Whether the gallery should show a running count of loaded items, per
/// user settings.
final galleryShowItemCountProvider = Provider<bool>((ref) {
  final settings = ref.watch(galleryTagSettingsProvider).asData?.value;
  return settings?.pagination.showItemCount ?? false;
});

final galleryShowTileItemNumberProvider = Provider<bool>((ref) {
  final settings = ref.watch(galleryTagSettingsProvider).asData?.value;
  return settings?.pagination.showItemNumberOnTiles ?? false;
});

final galleryJumpTargetProvider =
    NotifierProvider<GalleryJumpTargetController, int?>(
      GalleryJumpTargetController.new,
    );

class GalleryJumpTargetController extends Notifier<int?> {
  @override
  int? build() => null;

  void set(int? index) => state = index == null || index < 0 ? null : index;
}

final galleryLastJumpIndexProvider =
    NotifierProvider<GalleryLastJumpIndexController, int>(
      GalleryLastJumpIndexController.new,
    );

class GalleryLastJumpIndexController extends Notifier<int> {
  @override
  int build() => 1;

  void set(int index) => state = index < 1 ? 1 : index;
}

enum GalleryJumpStatus {
  idle,
  loading,
  positioning,
  completed,
  notFound,
  failed,
}

final galleryJumpStatusProvider =
    NotifierProvider<GalleryJumpStatusController, GalleryJumpStatus>(
      GalleryJumpStatusController.new,
    );

class GalleryJumpStatusController extends Notifier<GalleryJumpStatus> {
  @override
  GalleryJumpStatus build() => GalleryJumpStatus.idle;

  void set(GalleryJumpStatus status) => state = status;
}

final galleryVisiblePageStartProvider =
    NotifierProvider<GalleryVisiblePageController, int>(
      GalleryVisiblePageController.new,
    );

class GalleryVisiblePageController extends Notifier<int> {
  @override
  int build() => 0;

  void set(int index) => state = index < 0 ? 0 : index;
}

final galleryNotesDataOffsetProvider =
    NotifierProvider<GalleryDataOffsetController, int>(
      GalleryDataOffsetController.new,
    );

final galleryMediaDataOffsetProvider =
    NotifierProvider<GalleryDataOffsetController, int>(
      GalleryDataOffsetController.new,
    );

class GalleryDataOffsetController extends Notifier<int> {
  @override
  int build() => 0;

  void set(int offset) => state = offset < 0 ? 0 : offset;
}

/// Whether the note-grid controller is currently fetching another page.
/// Watched by the grid UI so it can show a footer indicator and block
/// further "near end" triggers until the page finishes loading.
final galleryItemsLoadingMoreProvider =
    NotifierProvider<_LoadingMoreController, bool>(_LoadingMoreController.new);

/// Same as [galleryItemsLoadingMoreProvider] but for the media-grid
/// controller.
final galleryMediaItemsLoadingMoreProvider =
    NotifierProvider<_LoadingMoreController, bool>(_LoadingMoreController.new);

class _LoadingMoreController extends Notifier<bool> {
  @override
  bool build() => false;

  set value(bool next) => state = next;
}

final galleryRepositoryProvider = Provider<GalleryRepository>(
  (ref) => const RustGalleryRepository(),
);

final vaultPlatformProvider = Provider<VaultPlatform>(
  (ref) => const NativeVaultPlatform(),
);

class VaultSession {
  const VaultSession({
    required this.vaultPath,
    required this.paths,
    required this.scanReport,
  });

  final String vaultPath;
  final GalleryPaths paths;
  final GalleryScanReport scanReport;
}

final vaultSessionProvider =
    AsyncNotifierProvider<VaultController, VaultSession?>(VaultController.new);

class VaultController extends AsyncNotifier<VaultSession?> {
  @override
  Future<VaultSession?> build() async {
    final paths = await ref.read(vaultPlatformProvider).galleryPaths();
    final vaultPath = await ref
        .read(galleryRepositoryProvider)
        .loadVaultPath(paths.dataDirectory);
    if (vaultPath == null) {
      return null;
    }
    return _openVault(vaultPath, paths);
  }

  Future<void> chooseVault() async {
    try {
      final platform = ref.read(vaultPlatformProvider);
      final selectedPath = await platform.chooseVault();
      if (selectedPath == null) {
        return;
      }
      state = const AsyncLoading();
      final paths = await platform.galleryPaths();
      state = AsyncData(await _openVault(selectedPath, paths));
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    }
  }

  Future<void> rescan() async {
    final session = state.value;
    if (session == null) {
      return;
    }
    state = const AsyncLoading();
    try {
      final report = await ref
          .read(galleryRepositoryProvider)
          .scan(session.vaultPath, session.paths.indexPath);
      state = AsyncData(
        VaultSession(
          vaultPath: session.vaultPath,
          paths: session.paths,
          scanReport: report,
        ),
      );
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    }
  }

  Future<VaultSession> _openVault(String vaultPath, GalleryPaths paths) async {
    final repository = ref.read(galleryRepositoryProvider);
    await repository.prepareAppDataDirectory(paths.dataDirectory, vaultPath);
    final canonicalPath = await repository.saveVaultPath(
      paths.dataDirectory,
      vaultPath,
    );
    final report = await repository.scan(canonicalPath, paths.indexPath);
    return VaultSession(
      vaultPath: canonicalPath,
      paths: paths,
      scanReport: report,
    );
  }
}

final selectedTagsProvider =
    NotifierProvider<SelectedTagsController, Set<String>>(
      SelectedTagsController.new,
    );

class SelectedTagsController extends Notifier<Set<String>> {
  @override
  Set<String> build() => const {};

  void toggle(String tag) {
    final updated = {...state};
    if (!updated.add(tag)) {
      updated.remove(tag);
    }
    state = Set.unmodifiable(updated);
  }

  void clear() {
    state = const {};
  }

  void setAll(Iterable<String> tags) => state = Set.unmodifiable(tags);
}

final excludedTagsProvider =
    NotifierProvider<ExcludedTagsController, Set<String>>(
      ExcludedTagsController.new,
    );

class ExcludedTagsController extends Notifier<Set<String>> {
  @override
  Set<String> build() => const {};

  void toggle(String tag) {
    final updated = {...state};
    if (!updated.add(tag)) {
      updated.remove(tag);
    }
    state = Set.unmodifiable(updated);
  }

  void clear() => state = const {};

  void setAll(Iterable<String> tags) => state = Set.unmodifiable(tags);
}

final allTagsProvider = NotifierProvider<AllTagsController, Set<String>>(
  AllTagsController.new,
);

class AllTagsController extends Notifier<Set<String>> {
  @override
  Set<String> build() => const {};

  void toggle(String tag) {
    final updated = {...state};
    if (!updated.add(tag)) updated.remove(tag);
    state = Set.unmodifiable(updated);
  }

  void clear() => state = const {};

  void setAll(Iterable<String> tags) => state = Set.unmodifiable(tags);
}

final gallerySearchQueryProvider =
    NotifierProvider<GallerySearchQueryController, String>(
      GallerySearchQueryController.new,
    );

class GallerySearchQueryController extends Notifier<String> {
  @override
  String build() => '';

  void set(String query) => state = query.trim();
  void clear() => state = '';
}

final selectedVirtualFiltersProvider =
    NotifierProvider<
      SelectedVirtualFiltersController,
      Set<GalleryVirtualFilter>
    >(SelectedVirtualFiltersController.new);

class SelectedVirtualFiltersController
    extends Notifier<Set<GalleryVirtualFilter>> {
  @override
  Set<GalleryVirtualFilter> build() => const {};

  void toggle(GalleryVirtualFilter filter) {
    final updated = {...state};
    if (!updated.add(filter)) {
      updated.remove(filter);
    }
    state = Set.unmodifiable(updated);
  }

  void clear() {
    state = const {};
  }
}

/// How the gallery grid groups media: one tile per note (showing just the
/// representative media), or one tile per media item so every image/video a
/// note contains shows up on its own.
enum GalleryDisplayMode { byNote, allMedia }

final galleryDisplayModeProvider =
    NotifierProvider<GalleryDisplayModeController, GalleryDisplayMode>(
      GalleryDisplayModeController.new,
    );

class GalleryDisplayModeController extends Notifier<GalleryDisplayMode> {
  @override
  GalleryDisplayMode build() => GalleryDisplayMode.byNote;

  void set(GalleryDisplayMode mode) => state = mode;
}

final galleryFilteredItemCountProvider = FutureProvider<int>((ref) async {
  final filters = _filtersForQuery(ref);
  final excludedFilters = ref
      .watch(excludedTagsProvider)
      .toList(growable: false);
  final virtualFilters = ref
      .watch(selectedVirtualFiltersProvider)
      .map((filter) => filter.key)
      .toList(growable: false);
  final searchQuery = ref.watch(gallerySearchQueryProvider);
  final mode = ref.watch(galleryDisplayModeProvider);
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) return 0;
  final repository = ref.read(galleryRepositoryProvider);
  return switch (mode) {
    GalleryDisplayMode.byNote => repository.countNotes(
      session.vaultPath,
      session.paths.indexPath,
      filters,
      excludedFilters: excludedFilters,
      virtualFilters: virtualFilters,
      searchQuery: searchQuery,
    ),
    GalleryDisplayMode.allMedia => repository.countMedia(
      session.vaultPath,
      session.paths.indexPath,
      filters,
      excludedFilters: excludedFilters,
      virtualFilters: virtualFilters,
      searchQuery: searchQuery,
    ),
  };
});

final galleryCategoriesProvider = FutureProvider<List<GalleryCategory>>((
  ref,
) async {
  final filters = _filtersForQuery(ref);
  final excludedFilters = ref
      .watch(excludedTagsProvider)
      .toList(growable: false);
  final virtualFilters = ref
      .watch(selectedVirtualFiltersProvider)
      .map((filter) => filter.key)
      .toList(growable: false);
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) {
    return const [];
  }
  final categories = await ref
      .read(galleryRepositoryProvider)
      .listCategories(
        session.vaultPath,
        session.paths.indexPath,
        filters,
        excludedFilters,
        virtualFilters,
      );
  final sourceCategories = categories
      .where(
        (category) =>
            category.path == 'source' || category.path.startsWith('source/'),
      )
      .toList(growable: false);
  final otherCategories = categories
      .where(
        (category) =>
            category.path != 'source' && !category.path.startsWith('source/'),
      )
      .toList(growable: false);
  if (sourceCategories.isEmpty) return categories;
  final sourceCategory = GalleryCategory(
    path: 'source',
    displayName: 'ソース',
    count: sourceCategories
        .map((category) => category.count)
        .fold<int>(0, (largest, count) => count > largest ? count : largest),
    options:
        [
          for (final category in sourceCategories)
            for (final option in category.options)
              GalleryCategoryOption(
                name: _sourceOptionName(
                  category.path,
                  option.fullTag,
                  option.name,
                ),
                fullTag: option.fullTag,
                count: option.count,
                disabled: option.disabled,
                sectionPath: _sourceSectionPath(category.path),
                virtualFilter: option.virtualFilter,
              ),
        ]..sort(
          (left, right) =>
              left.fullTag.toLowerCase().compareTo(right.fullTag.toLowerCase()),
        ),
  );
  return [...otherCategories, sourceCategory]
    ..sort((left, right) => left.path.compareTo(right.path));
});

List<String> _filtersForQuery(Ref ref) => [
  ...ref.watch(selectedTagsProvider),
  ...ref.watch(allTagsProvider).map((tag) => '$galleryAllFilterPrefix$tag'),
];

String _sourceSectionPath(String categoryPath) {
  final segments = categoryPath.split('/');
  return segments.length > 1 ? segments.take(2).join('/') : categoryPath;
}

String _sourceOptionName(String categoryPath, String fullTag, String name) {
  if (fullTag == 'source') return name;
  final sectionPath = _sourceSectionPath(categoryPath);
  if (fullTag == sectionPath) return 'すべて';
  final descendantPrefix = '$sectionPath/';
  if (fullTag.startsWith(descendantPrefix)) {
    return fullTag.substring(descendantPrefix.length);
  }
  return name;
}

final galleryItemsProvider =
    AsyncNotifierProvider<GalleryItemsController, List<GalleryNote>>(
      GalleryItemsController.new,
    );

class GalleryItemsController extends AsyncNotifier<List<GalleryNote>> {
  VaultSession? _session;
  List<String> _filters = const [];
  List<String> _excludedFilters = const [];
  List<String> _virtualFilters = const [];
  String _searchQuery = '';
  int _pageSize = galleryPageSize;
  bool _hasMore = true;
  bool get isLoadingMore => ref.read(galleryItemsLoadingMoreProvider);
  bool get hasMore => _hasMore;
  bool get hasPrevious => ref.read(galleryNotesDataOffsetProvider) > 0;

  @override
  Future<List<GalleryNote>> build() async {
    ref.listen(selectedTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(allTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(excludedTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(selectedVirtualFiltersProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(gallerySearchQueryProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(galleryPageSizeProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(vaultSessionProvider, (_, _) {
      _resetGalleryPosition();
    });
    _filters = _filtersForQuery(ref);
    _excludedFilters = ref.watch(excludedTagsProvider).toList(growable: false);
    _virtualFilters = ref
        .watch(selectedVirtualFiltersProvider)
        .map((filter) => filter.key)
        .toList(growable: false);
    _searchQuery = ref.watch(gallerySearchQueryProvider);
    _pageSize = ref.watch(galleryPageSizeProvider);
    _session = await ref.watch(vaultSessionProvider.future);
    _hasMore = true;
    if (_session == null) {
      return const [];
    }
    final items = await ref
        .read(galleryRepositoryProvider)
        .queryNotes(
          _session!.vaultPath,
          _session!.paths.indexPath,
          _filters,
          excludedFilters: _excludedFilters,
          virtualFilters: _virtualFilters,
          searchQuery: _searchQuery,
          offset: 0,
          limit: _pageSize,
        );
    _hasMore = items.length == _pageSize;
    return items;
  }

  Future<void> loadMore() async {
    final current = state.value;
    final session = _session;
    if (current == null || session == null || !_hasMore || isLoadingMore) {
      return;
    }
    ref.read(galleryItemsLoadingMoreProvider.notifier).value = true;
    try {
      final next = await ref
          .read(galleryRepositoryProvider)
          .queryNotes(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: ref.read(galleryNotesDataOffsetProvider) + current.length,
            limit: _pageSize,
          );
      _hasMore = next.length == _pageSize;
      state = AsyncData([...current, ...next]);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      ref.read(galleryItemsLoadingMoreProvider.notifier).value = false;
    }
  }

  Future<void> loadPrevious({
    void Function(int insertedCount)? beforePrepend,
  }) async {
    final current = state.value;
    final session = _session;
    final currentOffset = ref.read(galleryNotesDataOffsetProvider);
    if (current == null ||
        session == null ||
        currentOffset == 0 ||
        isLoadingMore ||
        ref.read(galleryJumpTargetProvider) != null) {
      return;
    }
    ref.read(galleryItemsLoadingMoreProvider.notifier).value = true;
    try {
      final previousOffset = currentOffset > _pageSize
          ? currentOffset - _pageSize
          : 0;
      final previous = await ref
          .read(galleryRepositoryProvider)
          .queryNotes(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: previousOffset,
            limit: currentOffset - previousOffset,
          );
      beforePrepend?.call(previous.length);
      state = AsyncData([...previous, ...current]);
      ref.read(galleryNotesDataOffsetProvider.notifier).set(previousOffset);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      ref.read(galleryItemsLoadingMoreProvider.notifier).value = false;
    }
  }

  Future<void> jumpTo(int index) async {
    final session = _session;
    if (session == null || index < 0) return;
    final targetPageOffset = (index ~/ _pageSize) * _pageSize;
    final offset = targetPageOffset > 0 ? targetPageOffset - _pageSize : 0;
    final limit = targetPageOffset + _pageSize - offset;
    try {
      final items = await ref
          .read(galleryRepositoryProvider)
          .queryNotes(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: offset,
            limit: limit,
          );
      if (ref.read(galleryJumpTargetProvider) != index ||
          ref.read(galleryJumpStatusProvider) != GalleryJumpStatus.loading) {
        return;
      }
      if (items.isEmpty) {
        ref
            .read(galleryJumpStatusProvider.notifier)
            .set(GalleryJumpStatus.notFound);
        ref.read(galleryJumpTargetProvider.notifier).set(null);
        return;
      }
      ref.read(galleryNotesDataOffsetProvider.notifier).set(offset);
      _hasMore = items.length == limit;
      state = AsyncData(items);
      ref
          .read(galleryJumpStatusProvider.notifier)
          .set(GalleryJumpStatus.positioning);
    } catch (_) {
      if (ref.read(galleryJumpTargetProvider) == index) {
        // The progress dialog reports the failure while the existing page
        // remains visible and scrollable.
        ref
            .read(galleryJumpStatusProvider.notifier)
            .set(GalleryJumpStatus.failed);
        ref.read(galleryJumpTargetProvider.notifier).set(null);
      }
    }
  }

  void _resetGalleryPosition() {
    ref.read(galleryJumpTargetProvider.notifier).set(null);
    ref.read(galleryVisiblePageStartProvider.notifier).set(0);
    ref.read(galleryNotesDataOffsetProvider.notifier).set(0);
  }
}

final galleryMediaItemsProvider =
    AsyncNotifierProvider<GalleryMediaItemsController, List<GalleryMediaItem>>(
      GalleryMediaItemsController.new,
    );

class GalleryMediaItemsController
    extends AsyncNotifier<List<GalleryMediaItem>> {
  VaultSession? _session;
  List<String> _filters = const [];
  List<String> _excludedFilters = const [];
  List<String> _virtualFilters = const [];
  String _searchQuery = '';
  int _pageSize = galleryPageSize;
  bool _hasMore = true;
  bool get isLoadingMore => ref.read(galleryMediaItemsLoadingMoreProvider);
  bool get hasMore => _hasMore;
  bool get hasPrevious => ref.read(galleryMediaDataOffsetProvider) > 0;

  @override
  Future<List<GalleryMediaItem>> build() async {
    ref.listen(selectedTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(allTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(excludedTagsProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(selectedVirtualFiltersProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(gallerySearchQueryProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(galleryPageSizeProvider, (_, _) {
      _resetGalleryPosition();
    });
    ref.listen(vaultSessionProvider, (_, _) {
      _resetGalleryPosition();
    });
    _filters = _filtersForQuery(ref);
    _excludedFilters = ref.watch(excludedTagsProvider).toList(growable: false);
    _virtualFilters = ref
        .watch(selectedVirtualFiltersProvider)
        .map((filter) => filter.key)
        .toList(growable: false);
    _searchQuery = ref.watch(gallerySearchQueryProvider);
    _pageSize = ref.watch(galleryPageSizeProvider);
    _session = await ref.watch(vaultSessionProvider.future);
    _hasMore = true;
    if (_session == null) {
      return const [];
    }
    final items = await ref
        .read(galleryRepositoryProvider)
        .queryMedia(
          _session!.vaultPath,
          _session!.paths.indexPath,
          _filters,
          excludedFilters: _excludedFilters,
          virtualFilters: _virtualFilters,
          searchQuery: _searchQuery,
          offset: 0,
          limit: _pageSize,
        );
    _hasMore = items.length == _pageSize;
    return items;
  }

  Future<void> loadMore() async {
    final current = state.value;
    final session = _session;
    if (current == null || session == null || !_hasMore || isLoadingMore) {
      return;
    }
    ref.read(galleryMediaItemsLoadingMoreProvider.notifier).value = true;
    try {
      final next = await ref
          .read(galleryRepositoryProvider)
          .queryMedia(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: ref.read(galleryMediaDataOffsetProvider) + current.length,
            limit: _pageSize,
          );
      _hasMore = next.length == _pageSize;
      state = AsyncData([...current, ...next]);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      ref.read(galleryMediaItemsLoadingMoreProvider.notifier).value = false;
    }
  }

  Future<void> loadPrevious({
    void Function(int insertedCount)? beforePrepend,
  }) async {
    final current = state.value;
    final session = _session;
    final currentOffset = ref.read(galleryMediaDataOffsetProvider);
    if (current == null ||
        session == null ||
        currentOffset == 0 ||
        isLoadingMore ||
        ref.read(galleryJumpTargetProvider) != null) {
      return;
    }
    ref.read(galleryMediaItemsLoadingMoreProvider.notifier).value = true;
    try {
      final previousOffset = currentOffset > _pageSize
          ? currentOffset - _pageSize
          : 0;
      final previous = await ref
          .read(galleryRepositoryProvider)
          .queryMedia(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: previousOffset,
            limit: currentOffset - previousOffset,
          );
      beforePrepend?.call(previous.length);
      state = AsyncData([...previous, ...current]);
      ref.read(galleryMediaDataOffsetProvider.notifier).set(previousOffset);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      ref.read(galleryMediaItemsLoadingMoreProvider.notifier).value = false;
    }
  }

  Future<void> jumpTo(int index) async {
    final session = _session;
    if (session == null || index < 0) return;
    final targetPageOffset = (index ~/ _pageSize) * _pageSize;
    final offset = targetPageOffset > 0 ? targetPageOffset - _pageSize : 0;
    final limit = targetPageOffset + _pageSize - offset;
    try {
      final items = await ref
          .read(galleryRepositoryProvider)
          .queryMedia(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            excludedFilters: _excludedFilters,
            virtualFilters: _virtualFilters,
            searchQuery: _searchQuery,
            offset: offset,
            limit: limit,
          );
      if (ref.read(galleryJumpTargetProvider) != index ||
          ref.read(galleryJumpStatusProvider) != GalleryJumpStatus.loading) {
        return;
      }
      if (items.isEmpty) {
        ref
            .read(galleryJumpStatusProvider.notifier)
            .set(GalleryJumpStatus.notFound);
        ref.read(galleryJumpTargetProvider.notifier).set(null);
        return;
      }
      ref.read(galleryMediaDataOffsetProvider.notifier).set(offset);
      _hasMore = items.length == limit;
      state = AsyncData(items);
      ref
          .read(galleryJumpStatusProvider.notifier)
          .set(GalleryJumpStatus.positioning);
    } catch (_) {
      if (ref.read(galleryJumpTargetProvider) == index) {
        // The progress dialog reports the failure while the existing page
        // remains visible and scrollable.
        ref
            .read(galleryJumpStatusProvider.notifier)
            .set(GalleryJumpStatus.failed);
        ref.read(galleryJumpTargetProvider.notifier).set(null);
      }
    }
  }

  void _resetGalleryPosition() {
    ref.read(galleryJumpTargetProvider.notifier).set(null);
    ref.read(galleryVisiblePageStartProvider.notifier).set(0);
    ref.read(galleryMediaDataOffsetProvider.notifier).set(0);
  }
}

final galleryThumbnailProvider = FutureProvider.family<Uint8List?, int>((
  ref,
  mediaId,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) {
    return null;
  }
  final repository = ref.read(galleryRepositoryProvider);
  final thumbnail = await repository.getThumbnail(
    session.vaultPath,
    session.paths.indexPath,
    session.paths.thumbnailDirectory,
    mediaId,
  );
  if (thumbnail != null) {
    return thumbnail;
  }
  final videoPath = await repository.getVideoSourcePath(
    session.vaultPath,
    session.paths.indexPath,
    mediaId,
  );
  if (videoPath == null) {
    return null;
  }
  return FcNativeVideoThumbnail().saveThumbnailToBytes(
    srcFile: videoPath,
    width: 320,
    height: 320,
    quality: 75,
  );
});

final galleryNoteDetailProvider =
    FutureProvider.family<GalleryNoteDetail?, int>((ref, noteId) async {
      final session = await ref.watch(vaultSessionProvider.future);
      if (session == null) return null;
      return ref
          .read(galleryRepositoryProvider)
          .getNoteDetail(session.vaultPath, session.paths.indexPath, noteId);
    });

final galleryMediaSourcePathProvider = FutureProvider.family<String?, int>((
  ref,
  mediaId,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) return null;
  return ref
      .read(galleryRepositoryProvider)
      .getMediaSourcePath(session.vaultPath, session.paths.indexPath, mediaId);
});
