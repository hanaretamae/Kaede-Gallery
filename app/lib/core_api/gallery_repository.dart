import 'dart:typed_data';

class GalleryCategoryOption {
  const GalleryCategoryOption({
    required this.name,
    required this.fullTag,
    required this.count,
    required this.disabled,
    this.virtualFilter,
  });

  final String name;
  final String fullTag;
  final int count;
  final bool disabled;
  final String? virtualFilter;
}

class GalleryCategory {
  const GalleryCategory({
    required this.path,
    required this.displayName,
    required this.options,
    required this.count,
  });

  final String path;
  final String displayName;
  final List<GalleryCategoryOption> options;
  final int count;
}

class GalleryNote {
  const GalleryNote({
    required this.id,
    required this.path,
    required this.title,
    required this.mediaCount,
    required this.videoCount,
    required this.representativeMediaId,
  });

  final int id;
  final String path;
  final String title;
  final int mediaCount;
  final int videoCount;
  final int? representativeMediaId;
}

class GalleryMediaItem {
  const GalleryMediaItem({
    required this.id,
    required this.noteId,
    required this.isVideo,
    required this.exists,
  });

  final int id;
  final int noteId;
  final bool isVideo;
  final bool exists;
}

class GalleryScanReport {
  const GalleryScanReport({required this.notesIndexed, required this.warnings});

  final int notesIndexed;
  final int warnings;
}

enum GalleryVirtualFilter {
  multipleMedia('multiple_media', '複数画像'),
  hasMemo('has_memo', '覚書あり'),
  hasVideo('has_video', '動画あり'),
  hasRelated('has_related', '関連あり');

  const GalleryVirtualFilter(this.key, this.label);

  final String key;
  final String label;

  static GalleryVirtualFilter fromKey(String key) =>
      values.firstWhere((filter) => filter.key == key);
}

abstract interface class GalleryRepository {
  Future<void> prepareAppDataDirectory(String directoryPath, String vaultPath);
  Future<String?> loadVaultPath(String directoryPath);
  Future<String> saveVaultPath(String directoryPath, String vaultPath);
  Future<GalleryScanReport> scan(String vaultPath, String indexPath);
  Future<List<GalleryCategory>> listCategories(
    String vaultPath,
    String indexPath,
    List<String> filters,
    List<String> virtualFilters,
  );
  Future<List<GalleryNote>> queryNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> virtualFilters,
    required int offset,
    required int limit,
  });
  Future<List<GalleryMediaItem>> queryMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> virtualFilters,
    required int offset,
    required int limit,
  });
  Future<Uint8List?> getThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  );
}
