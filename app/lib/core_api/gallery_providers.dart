import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:fc_native_video_thumbnail/fc_native_video_thumbnail.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

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
    if (vaultPath.startsWith('content://') &&
        await const AndroidSafAccess().loadVault() != vaultPath) {
      throw StateError('選択したフォルダへのアクセス権がありません。Vault を選び直してください。');
    }
    if (vaultPath.startsWith('content://')) {
      final cached = await _readSafScanCache(vaultPath, paths);
      if (cached != null) {
        return VaultSession(
          vaultPath: vaultPath,
          paths: paths,
          scanReport: cached,
        );
      }
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
      if (session.vaultPath.startsWith('content://')) {
        await _invalidateSafThumbnailCache(session.paths.thumbnailDirectory);
        await _writeSafScanCache(session.vaultPath, session.paths, report);
      }
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
    if (canonicalPath.startsWith('content://')) {
      await _invalidateSafThumbnailCache(paths.thumbnailDirectory);
      await _writeSafScanCache(canonicalPath, paths, report);
    }
    return VaultSession(
      vaultPath: canonicalPath,
      paths: paths,
      scanReport: report,
    );
  }
}

const _safScanCacheVersion = 1;

File _safScanCacheFile(GalleryPaths paths) =>
    File('${paths.dataDirectory}/saf-scan-cache.json');

Future<GalleryScanReport?> _readSafScanCache(
  String vaultPath,
  GalleryPaths paths,
) async {
  try {
    if (!await File(paths.indexPath).exists()) return null;
    final decoded = jsonDecode(await _safScanCacheFile(paths).readAsString());
    if (decoded is! Map<String, dynamic> ||
        decoded['version'] != _safScanCacheVersion ||
        decoded['vaultPath'] != vaultPath ||
        decoded['indexPath'] != paths.indexPath ||
        decoded['notesIndexed'] is! int ||
        decoded['warnings'] is! int) {
      return null;
    }
    return GalleryScanReport(
      notesIndexed: decoded['notesIndexed']! as int,
      warnings: decoded['warnings']! as int,
    );
  } on FileSystemException {
    return null;
  } on FormatException {
    return null;
  }
}

Future<void> _writeSafScanCache(
  String vaultPath,
  GalleryPaths paths,
  GalleryScanReport report,
) async {
  final cacheFile = _safScanCacheFile(paths);
  await Directory(paths.dataDirectory).create(recursive: true);
  final temporaryFile = File('${cacheFile.path}.tmp');
  await temporaryFile.writeAsString(
    jsonEncode({
      'version': _safScanCacheVersion,
      'vaultPath': vaultPath,
      'indexPath': paths.indexPath,
      'notesIndexed': report.notesIndexed,
      'warnings': report.warnings,
    }),
    flush: true,
  );
  await temporaryFile.rename(cacheFile.path);
}

Future<void> _invalidateSafThumbnailCache(String directoryPath) async {
  final directory = Directory(directoryPath);
  if (!await directory.exists()) return;
  await for (final entity in directory.list(followLinks: false)) {
    if (entity is File &&
        RegExp(r'^saf-(?:v[2-7]-)?[1-9]\d*\.png$')
            .hasMatch(entity.uri.pathSegments.last)) {
      await entity.delete();
    }
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

final gallerySortProvider =
    NotifierProvider<GallerySortController, GallerySort>(
      GallerySortController.new,
    );

class GallerySortController extends Notifier<GallerySort> {
  @override
  GallerySort build() => const GallerySort();

  void setField(GallerySortField field) => state = state.copyWith(field: field);

  void setDirection(GallerySortDirection direction) =>
      state = state.copyWith(direction: direction);
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
  GallerySort _sort = const GallerySort();
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
    ref.listen(gallerySortProvider, (_, _) {
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
    _sort = ref.watch(gallerySortProvider);
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
          sort: _sort,
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
            sort: _sort,
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
            sort: _sort,
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
            sort: _sort,
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
  GallerySort _sort = const GallerySort();
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
    ref.listen(gallerySortProvider, (_, _) {
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
    _sort = ref.watch(gallerySortProvider);
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
          sort: _sort,
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
            sort: _sort,
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
            sort: _sort,
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
            sort: _sort,
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

final galleryThumbnailProvider = FutureProvider.autoDispose
    .family<Uint8List?, int>((ref, mediaId) async {
      final session = await ref.watch(vaultSessionProvider.future);
      if (session == null) {
        return null;
      }
      final repository = ref.read(galleryRepositoryProvider);
      if (session.vaultPath.startsWith('content://')) {
        final videoPath = await repository.getVideoSourcePath(
          session.vaultPath,
          session.paths.indexPath,
          mediaId,
        );
        if (videoPath != null) {
          final thumbnail = await _safVideoThumbnail(
            session.vaultPath,
            videoPath,
            session.paths.thumbnailDirectory,
            mediaId,
          );
          if (thumbnail != null) return thumbnail;
        }
        return repository.getThumbnail(
          session.vaultPath,
          session.paths.indexPath,
          session.paths.thumbnailDirectory,
          mediaId,
        );
      }
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

Future<Uint8List?> _safVideoThumbnail(
  String vaultUri,
  String source,
  String cacheDirectory,
  int mediaId,
) async {
  final cacheFile = File('$cacheDirectory/saf-v6-$mediaId.png');
  if (await cacheFile.exists()) {
    final size = await cacheFile.length();
    if (size > 0 && size <= 4 * 1024 * 1024) {
      await cacheFile.setLastModified(DateTime.now());
      return cacheFile.readAsBytes();
    }
    await cacheFile.delete();
  }
  const safAccess = AndroidSafAccess();
  final descriptor = await safAccess.openMediaFileDescriptor(vaultUri, source);
  if (descriptor == null) return null;
  final player = Player(
    configuration: const PlayerConfiguration(
      muted: true,
      bufferSize: 4 * 1024 * 1024,
      logLevel: MPVLogLevel.error,
    ),
  );
  try {
    final videoController = VideoController(player);
    await videoController.platform.future.timeout(const Duration(seconds: 8));
    final hasVideo = player.stream.videoParams
        .firstWhere((params) => params.w != null && params.h != null)
        .timeout(const Duration(seconds: 12));
    await player.open(Media('fd://$descriptor'));
    final videoParams = await hasVideo;
    final sourceWidth = videoParams.w!;
    final sourceHeight = videoParams.h!;
    if (sourceWidth < 1 ||
        sourceHeight < 1 ||
        sourceWidth > 16384 ||
        sourceHeight > 16384 ||
        sourceWidth * sourceHeight > 16 * 1024 * 1024) {
      return null;
    }
    try {
      await player.stream.position
          .firstWhere((position) => position > Duration.zero)
          .timeout(const Duration(seconds: 3));
    } on TimeoutException {
      return null;
    }
    for (var attempt = 0; attempt < 3; attempt++) {
      final bytes = await player.screenshot(format: 'image/png');
      if (bytes != null && bytes.isNotEmpty) {
        final thumbnail = await _resizeSafVideoThumbnail(bytes, videoParams);
        if (thumbnail != null) {
          await Directory(cacheDirectory).create(recursive: true);
          final temporaryFile = File('${cacheFile.path}.tmp');
          await temporaryFile.writeAsBytes(thumbnail, flush: true);
          await temporaryFile.rename(cacheFile.path);
          return thumbnail;
        }
      }
      await Future<void>.delayed(const Duration(milliseconds: 100));
    }
    return null;
  } finally {
    await player.dispose();
    await safAccess.closeMediaFileDescriptor(descriptor);
  }
}

Future<Uint8List?> _resizeSafVideoThumbnail(
  Uint8List bytes,
  VideoParams params,
) async {
  final width = params.dw ?? params.w;
  final height = params.dh ?? params.h;
  if (width == null ||
      height == null ||
      width < 1 ||
      height < 1 ||
      width > 16384 ||
      height > 16384 ||
      width * height > 16 * 1024 * 1024) {
    return null;
  }
  final scale = 320 / (width > height ? width : height);
  final codec = await ui.instantiateImageCodec(
    bytes,
    targetWidth: (width * scale).round().clamp(1, 320),
    targetHeight: (height * scale).round().clamp(1, 320),
  );
  try {
    final image = (await codec.getNextFrame()).image;
    try {
      final thumbnail = await image.toByteData(format: ui.ImageByteFormat.png);
      if (thumbnail == null) return null;
      return thumbnail.buffer.asUint8List(
        thumbnail.offsetInBytes,
        thumbnail.lengthInBytes,
      );
    } finally {
      image.dispose();
    }
  } finally {
    codec.dispose();
  }
}

final gallerySafImageBytesProvider = FutureProvider.family<Uint8List, String>((
  ref,
  mediaUri,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null || !session.vaultPath.startsWith('content://')) {
    throw StateError('Folder access is not active.');
  }
  return const AndroidSafAccess().readMedia(session.vaultPath, mediaUri);
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
