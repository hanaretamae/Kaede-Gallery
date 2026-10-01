import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'gallery_repository.dart';
import 'rust_gallery_repository.dart';
import '../platform/vault_platform.dart';

const galleryPageSize = 150;

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

final galleryCategoriesProvider = FutureProvider<List<GalleryCategory>>((
  ref,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) {
    return const [];
  }
  final filters = ref.watch(selectedTagsProvider).toList(growable: false);
  final virtualFilters = ref
      .watch(selectedVirtualFiltersProvider)
      .map((filter) => filter.key)
      .toList(growable: false);
  return ref
      .read(galleryRepositoryProvider)
      .listCategories(
        session.vaultPath,
        session.paths.indexPath,
        filters,
        virtualFilters,
      );
});

final galleryItemsProvider =
    AsyncNotifierProvider<GalleryItemsController, List<GalleryNote>>(
      GalleryItemsController.new,
    );

class GalleryItemsController extends AsyncNotifier<List<GalleryNote>> {
  VaultSession? _session;
  List<String> _filters = const [];
  List<String> _virtualFilters = const [];
  bool _hasMore = true;
  bool isLoadingMore = false;

  @override
  Future<List<GalleryNote>> build() async {
    _filters = ref.watch(selectedTagsProvider).toList(growable: false);
    _virtualFilters = ref
        .watch(selectedVirtualFiltersProvider)
        .map((filter) => filter.key)
        .toList(growable: false);
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
          virtualFilters: _virtualFilters,
          offset: 0,
          limit: galleryPageSize,
        );
    _hasMore = items.length == galleryPageSize;
    return items;
  }

  Future<void> loadMore() async {
    final current = state.value;
    final session = _session;
    if (current == null || session == null || !_hasMore || isLoadingMore) {
      return;
    }
    isLoadingMore = true;
    try {
      final next = await ref
          .read(galleryRepositoryProvider)
          .queryNotes(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            virtualFilters: _virtualFilters,
            offset: current.length,
            limit: galleryPageSize,
          );
      _hasMore = next.length == galleryPageSize;
      state = AsyncData([...current, ...next]);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      isLoadingMore = false;
    }
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
  List<String> _virtualFilters = const [];
  bool _hasMore = true;
  bool isLoadingMore = false;

  @override
  Future<List<GalleryMediaItem>> build() async {
    _filters = ref.watch(selectedTagsProvider).toList(growable: false);
    _virtualFilters = ref
        .watch(selectedVirtualFiltersProvider)
        .map((filter) => filter.key)
        .toList(growable: false);
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
          virtualFilters: _virtualFilters,
          offset: 0,
          limit: galleryPageSize,
        );
    _hasMore = items.length == galleryPageSize;
    return items;
  }

  Future<void> loadMore() async {
    final current = state.value;
    final session = _session;
    if (current == null || session == null || !_hasMore || isLoadingMore) {
      return;
    }
    isLoadingMore = true;
    try {
      final next = await ref
          .read(galleryRepositoryProvider)
          .queryMedia(
            session.vaultPath,
            session.paths.indexPath,
            _filters,
            virtualFilters: _virtualFilters,
            offset: current.length,
            limit: galleryPageSize,
          );
      _hasMore = next.length == galleryPageSize;
      state = AsyncData([...current, ...next]);
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
    } finally {
      isLoadingMore = false;
    }
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
  return ref
      .read(galleryRepositoryProvider)
      .getThumbnail(
        session.vaultPath,
        session.paths.indexPath,
        session.paths.thumbnailDirectory,
        mediaId,
      );
});
