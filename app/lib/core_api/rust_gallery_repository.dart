import 'dart:io';
import 'dart:typed_data';

import '../platform/vault_platform.dart';

import 'package:vault_gallery/src/rust/api.dart' as rust;

import 'gallery_repository.dart';

final Map<String, Future<Uint8List?>> _safThumbnailLoads = {};

class RustGalleryRepository implements GalleryRepository {
  const RustGalleryRepository({required this.safAccess});

  final SafVaultAccess safAccess;

  @override
  Future<void> prepareAppDataDirectory(
    String directoryPath,
    String vaultPath,
  ) async {
    if (vaultPath.startsWith('content://')) {
      await rust.prepareAppDataDirectorySaf(directoryPath: directoryPath);
    } else {
      await rust.prepareAppDataDirectory(
        directoryPath: directoryPath,
        vaultPath: vaultPath,
      );
    }
  }

  @override
  Future<String?> loadVaultPath(String directoryPath) =>
      rust.loadVaultPath(directoryPath: directoryPath);

  @override
  Future<String> saveVaultPath(String directoryPath, String vaultPath) async {
    final saved = await rust.saveVaultPath(
      directoryPath: directoryPath,
      vaultPath: vaultPath,
    );
    resumeVaultCacheOperations(vaultPath);
    return saved;
  }

  @override
  Future<void> forgetVaultData(String directoryPath, String vaultPath) => rust
      .forgetVaultData(directoryPath: directoryPath, expectedVault: vaultPath);

  @override
  Future<GalleryScanReport> scan(String vaultPath, String indexPath) async {
    final report = vaultPath.startsWith('content://')
        ? await _scanSaf(vaultPath, indexPath)
        : await rust.scan(vaultPath: vaultPath, indexPath: indexPath);
    return GalleryScanReport(
      notesIndexed: report.notesIndexed,
      warnings: report.warnings,
    );
  }

  Future<rust.ScanReport> _scanSaf(String vaultUri, String indexPath) async {
    final access = safAccess;
    final files = await access.listFiles(vaultUri);
    final notes = <rust.SafNote>[];
    final noteFiles = files
        .where(
          (file) =>
              file.path.toLowerCase().endsWith('.md') &&
              !_isIgnoredVaultPath(file.path),
        )
        .toList(growable: false);
    final noteContents = await access.readListedFiles(vaultUri, noteFiles);
    for (final file in noteFiles) {
      notes.add(
        rust.SafNote(
          path: file.path,
          modifiedNanos: file.modifiedNanos,
          size: file.size,
          content: noteContents[file.path],
        ),
      );
    }
    return rust.scanSaf(
      vaultPath: vaultUri,
      indexPath: indexPath,
      notes: notes,
      filePaths: files.map((file) => file.path).toList(growable: false),
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
    required GallerySort sort,
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
      sortField: sort.field.apiValue,
      sortDirection: sort.direction.apiValue,
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
    required GallerySort sort,
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
      sortField: sort.field.apiValue,
      sortDirection: sort.direction.apiValue,
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
  ) async {
    if (!vaultCacheOperationsAllowed(vaultPath)) return null;
    if (vaultPath.startsWith('content://')) {
      final cacheKey = '$vaultPath:$indexPath:$mediaId';
      final pending = _safThumbnailLoads[cacheKey];
      if (pending != null) return pending;
      late final Future<Uint8List?> loading;
      loading = _loadSafThumbnail(vaultPath, indexPath, cachePath, mediaId)
          .whenComplete(() {
            if (identical(_safThumbnailLoads[cacheKey], loading)) {
              _safThumbnailLoads.remove(cacheKey);
            }
          });
      _safThumbnailLoads[cacheKey] = loading;
      trackVaultCacheOperation(vaultPath, loading.then<void>((_) {}));
      return loading;
    }

    final loading = rust.getThumbnail(
      vaultPath: vaultPath,
      indexPath: indexPath,
      cachePath: cachePath,
      mediaId: mediaId,
      size: 320,
    );
    trackVaultCacheOperation(vaultPath, loading.then<void>((_) {}));
    return loading;
  }

  Future<Uint8List?> _loadSafThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  ) async {
    if (mediaId <= 0) return null;
    final thumbnailFile = File('$cachePath/saf-v7-$mediaId.png');
    if (await thumbnailFile.exists()) {
      final size = await thumbnailFile.length();
      if (size > 0 && size <= 4 * 1024 * 1024) {
        await thumbnailFile.setLastModified(DateTime.now());
        return thumbnailFile.readAsBytes();
      }
      await thumbnailFile.delete();
    }
    final relativePath = await rust.getMediaRelativePath(
      vaultPath: vaultPath,
      indexPath: indexPath,
      mediaId: mediaId,
    );
    if (relativePath == null) return null;
    final isVideo =
        await rust.getVideoSourcePath(
          vaultPath: vaultPath,
          indexPath: indexPath,
          mediaId: mediaId,
        ) !=
        null;
    final thumbnail = await safAccess.thumbnail(
      vaultPath,
      relativePath,
      video: isVideo,
    );
    if (thumbnail == null) return null;
    await Directory(cachePath).create(recursive: true);
    final temporaryFile = File('${thumbnailFile.path}.tmp');
    await temporaryFile.writeAsBytes(thumbnail, flush: true);
    await temporaryFile.rename(thumbnailFile.path);
    await _trimSafThumbnailCache(Directory(cachePath));
    return thumbnail;
  }

  @override
  Future<String?> getVideoSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) async {
    final source = await rust.getVideoSourcePath(
      vaultPath: vaultPath,
      indexPath: indexPath,
      mediaId: mediaId,
    );
    if (source == null || !vaultPath.startsWith('content://')) return source;
    return safAccess.resolveFile(vaultPath, source, video: true);
  }

  @override
  Future<String?> getMediaSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) async {
    final source = await rust.getMediaSourcePath(
      vaultPath: vaultPath,
      indexPath: indexPath,
      mediaId: mediaId,
    );
    if (source == null || !vaultPath.startsWith('content://')) return source;
    return safAccess.resolveFile(vaultPath, source);
  }

  @override
  Future<GalleryNoteDetail?> getNoteDetail(
    String vaultPath,
    String indexPath,
    int noteId,
  ) async {
    final rust.NoteDetail? detail;
    if (vaultPath.startsWith('content://')) {
      final path = await rust.getNotePath(
        vaultPath: vaultPath,
        indexPath: indexPath,
        noteId: noteId,
      );
      if (path == null) return null;
      final content = await safAccess.readFile(vaultPath, path);
      if (content == null) {
        throw StateError('ノートを読み込めません。アクセス権を確認してください。');
      }
      detail = await rust.getNoteDetailSaf(
        vaultPath: vaultPath,
        indexPath: indexPath,
        noteId: noteId,
        content: content,
      );
    } else {
      detail = await rust.getNoteDetail(
        vaultPath: vaultPath,
        indexPath: indexPath,
        noteId: noteId,
      );
    }
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

Future<void> _trimSafThumbnailCache(Directory directory) async {
  final thumbnails = <({File file, int size, DateTime modified})>[];
  var totalBytes = 0;
  await for (final entity in directory.list(followLinks: false)) {
    if (entity is! File ||
        !RegExp(r'^saf-(?:v[2-7]-)?[1-9]\d*\.png$')
            .hasMatch(entity.uri.pathSegments.last)) {
      continue;
    }
    final size = await entity.length();
    thumbnails.add((
      file: entity,
      size: size,
      modified: await entity.lastModified(),
    ));
    totalBytes += size;
  }
  thumbnails.sort((left, right) => left.modified.compareTo(right.modified));
  while (thumbnails.length > 256 || totalBytes > 64 * 1024 * 1024) {
    final oldest = thumbnails.removeAt(0);
    await oldest.file.delete();
    totalBytes -= oldest.size;
  }
}

bool _isIgnoredVaultPath(String value) {
  for (final component in value.split('/')) {
    if (component.startsWith('.') ||
        component.contains('.sync-conflict-') ||
        (component.startsWith('.syncthing.') && component.endsWith('.tmp')) ||
        component.startsWith('~syncthing~')) {
      return true;
    }
  }
  return false;
}
