import 'dart:typed_data';

import 'package:vault_gallery/src/rust/api.dart' as rust;

import 'gallery_repository.dart';

class RustGalleryRepository implements GalleryRepository {
  const RustGalleryRepository();

  @override
  Future<void> prepareAppDataDirectory(
    String directoryPath,
    String vaultPath,
  ) async {
    await rust.prepareAppDataDirectory(
      directoryPath: directoryPath,
      vaultPath: vaultPath,
    );
  }

  @override
  Future<String?> loadVaultPath(String directoryPath) =>
      rust.loadVaultPath(directoryPath: directoryPath);

  @override
  Future<String> saveVaultPath(String directoryPath, String vaultPath) =>
      rust.saveVaultPath(directoryPath: directoryPath, vaultPath: vaultPath);

  @override
  Future<GalleryScanReport> scan(String vaultPath, String indexPath) async {
    final report = await rust.scan(vaultPath: vaultPath, indexPath: indexPath);
    return GalleryScanReport(
      notesIndexed: report.notesIndexed,
      warnings: report.warnings,
    );
  }

  @override
  Future<List<GalleryCategory>> listCategories(
    String vaultPath,
    String indexPath,
    List<String> filters,
    List<String> virtualFilters,
  ) async {
    final categories = await rust.listCategories(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      virtualFilters: virtualFilters,
    );
    return categories
        .map(
          (category) => GalleryCategory(
            path: category.path,
            displayName: category.displayName,
            count: category.count,
            options: category.options
                .map(
                  (option) => GalleryCategoryOption(
                    name: option.name,
                    fullTag: option.fullTag,
                    count: option.count,
                    disabled: option.disabled,
                    virtualFilter: option.virtualFilter,
                  ),
                )
                .toList(growable: false),
          ),
        )
        .toList(growable: false);
  }

  @override
  Future<List<GalleryNote>> queryNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> virtualFilters,
    required int offset,
    required int limit,
  }) async {
    final notes = await rust.queryNotes(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      virtualFilters: virtualFilters,
      offset: offset,
      limit: limit,
    );
    return notes
        .map(
          (note) => GalleryNote(
            id: note.id,
            path: note.path,
            title: note.title,
            mediaCount: note.mediaCount,
            videoCount: note.videoCount,
            representativeMediaId: note.representativeMediaId,
          ),
        )
        .toList(growable: false);
  }

  @override
  Future<List<GalleryMediaItem>> queryMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> virtualFilters,
    required int offset,
    required int limit,
  }) async {
    final items = await rust.queryMedia(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      virtualFilters: virtualFilters,
      offset: offset,
      limit: limit,
    );
    return items
        .map(
          (item) => GalleryMediaItem(
            id: item.id,
            noteId: item.noteId,
            isVideo: item.isVideo,
            exists: item.exists,
          ),
        )
        .toList(growable: false);
  }

  @override
  Future<Uint8List?> getThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  ) => rust.getThumbnail(
    vaultPath: vaultPath,
    indexPath: indexPath,
    cachePath: cachePath,
    mediaId: mediaId,
    size: 320,
  );
}
