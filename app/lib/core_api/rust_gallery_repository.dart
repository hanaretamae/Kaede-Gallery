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
    List<String> excludedFilters,
    List<String> virtualFilters,
  ) async {
    final categories = await rust.listCategories(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      excludedFilters: excludedFilters,
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
                    sectionPath: category.path,
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
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
    required int offset,
    required int limit,
  }) async {
    final notes = await rust.queryNotes(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      excludedFilters: excludedFilters,
      virtualFilters: virtualFilters,
      searchQuery: searchQuery,
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
            memoCount: note.memoCount,
            relatedCount: note.relatedCount,
            representativeMediaId: note.representativeMediaId,
          ),
        )
        .toList(growable: false);
  }

  @override
  Future<int> countNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  }) => rust.countNotes(
    vaultPath: vaultPath,
    indexPath: indexPath,
    filters: filters,
    excludedFilters: excludedFilters,
    virtualFilters: virtualFilters,
    searchQuery: searchQuery,
  );

  @override
  Future<List<GalleryMediaItem>> queryMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
    required int offset,
    required int limit,
  }) async {
    final items = await rust.queryMedia(
      vaultPath: vaultPath,
      indexPath: indexPath,
      filters: filters,
      excludedFilters: excludedFilters,
      virtualFilters: virtualFilters,
      searchQuery: searchQuery,
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
            mediaCount: item.mediaCount,
            memoCount: item.memoCount,
            relatedCount: item.relatedCount,
          ),
        )
        .toList(growable: false);
  }

  @override
  Future<int> countMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  }) => rust.countMedia(
    vaultPath: vaultPath,
    indexPath: indexPath,
    filters: filters,
    excludedFilters: excludedFilters,
    virtualFilters: virtualFilters,
    searchQuery: searchQuery,
  );

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

  @override
  Future<String?> getVideoSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) => rust.getVideoSourcePath(
    vaultPath: vaultPath,
    indexPath: indexPath,
    mediaId: mediaId,
  );

  @override
  Future<String?> getMediaSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) => rust.getMediaSourcePath(
    vaultPath: vaultPath,
    indexPath: indexPath,
    mediaId: mediaId,
  );

  @override
  Future<GalleryNoteDetail?> getNoteDetail(
    String vaultPath,
    String indexPath,
    int noteId,
  ) async {
    final detail = await rust.getNoteDetail(
      vaultPath: vaultPath,
      indexPath: indexPath,
      noteId: noteId,
    );
    if (detail == null) return null;
    return GalleryNoteDetail(
      id: detail.id,
      path: detail.path,
      title: detail.title,
      author: detail.author,
      authorUrl: detail.authorUrl,
      url: detail.url,
      published: detail.published,
      created: detail.created,
      updated: detail.updated,
      tags: detail.tags,
      bodyText: detail.bodyText,
      memoLines: detail.memoLines
          .map(
            (line) => GalleryDetailLine(
              text: line.text,
              urls: line.urls,
              isBullet: line.isBullet,
              indentLevel: line.indentLevel,
              linkedNoteId: line.linkedNoteId,
            ),
          )
          .toList(growable: false),
      relatedLines: detail.relatedLines
          .map(
            (line) => GalleryDetailLine(
              text: line.text,
              urls: line.urls,
              isBullet: line.isBullet,
              indentLevel: line.indentLevel,
              linkedNoteId: line.linkedNoteId,
            ),
          )
          .toList(growable: false),
      media: detail.media
          .map(
            (item) => GalleryMediaItem(
              id: item.id,
              noteId: item.noteId,
              isVideo: item.isVideo,
              exists: item.exists,
              mediaCount: item.mediaCount,
              memoCount: item.memoCount,
              relatedCount: item.relatedCount,
            ),
          )
          .toList(growable: false),
    );
  }
}
