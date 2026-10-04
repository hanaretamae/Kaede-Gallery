import 'dart:typed_data';

enum GallerySortField {
  created('created', '作成日'),
  published('published', '公開日');

  const GallerySortField(this.apiValue, this.label);

  final String apiValue;
  final String label;
}

enum GallerySortDirection {
  ascending('ascending', '昇順'),
  descending('descending', '降順');

  const GallerySortDirection(this.apiValue, this.label);

  final String apiValue;
  final String label;
}

class GallerySort {
  const GallerySort({
    this.field = GallerySortField.created,
    this.direction = GallerySortDirection.descending,
  });

  final GallerySortField field;
  final GallerySortDirection direction;

  GallerySort copyWith({
    GallerySortField? field,
    GallerySortDirection? direction,
  }) => GallerySort(
    field: field ?? this.field,
    direction: direction ?? this.direction,
  );

  @override
  bool operator ==(Object other) =>
      other is GallerySort &&
      other.field == field &&
      other.direction == direction;

  @override
  int get hashCode => Object.hash(field, direction);
}

class GalleryCategoryOption {
  const GalleryCategoryOption({
    required this.name,
    required this.fullTag,
    required this.count,
    required this.disabled,
    this.sectionPath,
    this.virtualFilter,
  });

  final String name;
  final String fullTag;
  final int count;
  final bool disabled;
  final String? sectionPath;
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
  final int? count;
}

class GalleryNote {
  const GalleryNote({
    required this.id,
    required this.path,
    required this.title,
    required this.mediaCount,
    required this.videoCount,
    required this.memoCount,
    required this.relatedCount,
    required this.representativeMediaId,
  });

  final int id;
  final String path;
  final String title;
  final int mediaCount;
  final int videoCount;
  final int memoCount;
  final int relatedCount;
  final int? representativeMediaId;
}

class GalleryMediaItem {
  const GalleryMediaItem({
    required this.id,
    required this.noteId,
    required this.isVideo,
    required this.exists,
    this.mediaCount = 1,
    this.memoCount = 0,
    this.relatedCount = 0,
  });

  final int id;
  final int noteId;
  final bool isVideo;
  final bool exists;
  final int mediaCount;
  final int memoCount;
  final int relatedCount;
}

class GalleryDetailLine {
  const GalleryDetailLine({
    required this.text,
    required this.urls,
    this.isBullet = false,
    this.indentLevel = 0,
    this.linkedNoteId,
  });

  final String text;
  final List<String> urls;
  final bool isBullet;
  final int indentLevel;
  final int? linkedNoteId;
}

class GalleryNoteDetail {
  const GalleryNoteDetail({
    required this.id,
    required this.path,
    required this.title,
    required this.tags,
    required this.author,
    required this.authorUrl,
    required this.bodyText,
    required this.memoLines,
    required this.relatedLines,
    required this.media,
    this.url,
    this.published,
    this.created,
    this.updated,
  });

  final int id;
  final String path;
  final String title;
  final String? author;
  final String? authorUrl;
  final String? url;
  final String? published;
  final String? created;
  final String? updated;
  final List<String> tags;
  final String bodyText;
  final List<GalleryDetailLine> memoLines;
  final List<GalleryDetailLine> relatedLines;
  final List<GalleryMediaItem> media;
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
  Future<void> forgetVaultData(String directoryPath, String vaultPath);
  Future<GalleryScanReport> scan(String vaultPath, String indexPath);
  Future<List<GalleryCategory>> listCategories(
    String vaultPath,
    String indexPath,
    List<String> filters,
    List<String> excludedFilters,
    List<String> virtualFilters,
  );
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
  });
  Future<int> countNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  });
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
  });
  Future<int> countMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  });
  Future<Uint8List?> getThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  );
  Future<String?> getVideoSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  );
  Future<String?> getMediaSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  );
  Future<GalleryNoteDetail?> getNoteDetail(
    String vaultPath,
    String indexPath,
    int noteId,
  );
}
