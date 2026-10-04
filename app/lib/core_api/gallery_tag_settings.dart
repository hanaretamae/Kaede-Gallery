import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

import 'gallery_providers.dart';

class GalleryTagSettings {
  const GalleryTagSettings({
    this.includedPrefixes = defaultIncludedPrefixes,
    this.hiddenPrefixes = defaultHiddenPrefixes,
    this.colors = defaultTagColors,
    this.noteStructure = const GalleryNoteStructureSettings(),
    this.pagination = const GalleryPaginationSettings(),
    this.categories = const GalleryTagCategorySettings(),
  });

  static const defaultIncludedPrefixes = ['*'];
  static const defaultHiddenPrefixes = ['moc', 'add', 'pin', 'source/art'];
  static const defaultTagColors = <TagColorRule>[
    TagColorRule('source/gender/female', 0xFFF59EEE),
    TagColorRule('source/gender/male', 0xFF5D5BEC),
    TagColorRule('source/gender/unknown', 0xFFB17AF5),
    TagColorRule('source/gender/futanari', 0xFFEE8B95),
    TagColorRule('source/type', 0xFFFFAA7F),
    TagColorRule('source/count', 0xFFFFFFFF),
    TagColorRule('source/service/x', 0xFFFFFFFF),
    TagColorRule('source/service/pixiv', 0xFF3F90E9),
    TagColorRule('source/service/pixiv-fanbox', 0xFF3F90E9),
    TagColorRule('source/service/misskey', 0xFFADEA02),
    TagColorRule('source/service/mastodon', 0xFF3459FB),
    TagColorRule('source/rating', 0xFFFFFFFF),
    TagColorRule('source/format', 0xFF91EEE8),
    TagColorRule('source/meta', 0xFF641FFE),
    TagColorRule('copyright', 0xFFFFF950),
    TagColorRule('source/art', 0xFF4DD0E1),
  ];

  final List<String> includedPrefixes;
  final List<String> hiddenPrefixes;
  final List<TagColorRule> colors;
  final GalleryNoteStructureSettings noteStructure;
  final GalleryPaginationSettings pagination;
  final GalleryTagCategorySettings categories;

  GalleryTagSettings copyWith({
    List<String>? includedPrefixes,
    List<String>? hiddenPrefixes,
    List<TagColorRule>? colors,
    GalleryNoteStructureSettings? noteStructure,
    GalleryPaginationSettings? pagination,
    GalleryTagCategorySettings? categories,
  }) => GalleryTagSettings(
    includedPrefixes: includedPrefixes ?? this.includedPrefixes,
    hiddenPrefixes: hiddenPrefixes ?? this.hiddenPrefixes,
    colors: colors ?? this.colors,
    noteStructure: noteStructure ?? this.noteStructure,
    pagination: pagination ?? this.pagination,
    categories: categories ?? this.categories,
  );

  bool includes(String tag) =>
      includedPrefixes.contains('*') ||
      includedPrefixes.any((prefix) => _matchesPrefix(tag, prefix));

  bool hides(String tag) =>
      hiddenPrefixes.any((prefix) => _matchesPrefix(tag, prefix));

  Color colorFor(String tag) {
    final matchingRules =
        colors
            .where((rule) => _matchesPrefix(tag, rule.prefix))
            .toList(growable: false)
          ..sort(
            (left, right) => right.prefix.length.compareTo(left.prefix.length),
          );
    if (matchingRules.isNotEmpty) return Color(matchingRules.first.color);
    const fallback = [
      Color(0xFF80CBC4),
      Color(0xFF90CAF9),
      Color(0xFFCE93D8),
      Color(0xFFFFCC80),
    ];
    final stableHash = tag.codeUnits.fold<int>(
      0,
      (value, unit) => (value * 31 + unit) & 0x7fffffff,
    );
    return fallback[stableHash % fallback.length];
  }

  Map<String, Object> toJson() => {
    'includedPrefixes': includedPrefixes,
    'hiddenPrefixes': hiddenPrefixes,
    'colors': colors.map((rule) => rule.toJson()).toList(growable: false),
    'noteStructure': noteStructure.toJson(),
    'pagination': pagination.toJson(),
    'tagCategories': categories.toJson(),
  };

  static GalleryTagSettings fromJson(Map<String, dynamic> json) {
    final included = json['includedPrefixes'];
    final hidden = json['hiddenPrefixes'];
    final colors = json['colors'];
    final noteStructure = json['noteStructure'];
    final pagination = json['pagination'];
    final tagCategories = json['tagCategories'];
    if (included is! List ||
        !included.every((value) => value is String) ||
        hidden is! List ||
        !hidden.every((value) => value is String) ||
        colors is! List ||
        !colors.every((value) => value is Map<String, dynamic>)) {
      throw const FormatException('Invalid gallery tag settings.');
    }
    if (noteStructure != null && noteStructure is! Map<String, dynamic>) {
      throw const FormatException('Invalid gallery note structure settings.');
    }
    if (pagination != null && pagination is! Map<String, dynamic>) {
      throw const FormatException('Invalid gallery pagination settings.');
    }
    if (tagCategories != null && tagCategories is! Map<String, dynamic>) {
      throw const FormatException('Invalid gallery tag category settings.');
    }
    return GalleryTagSettings(
      categories: tagCategories == null
          ? const GalleryTagCategorySettings()
          : GalleryTagCategorySettings.fromJson(
              tagCategories as Map<String, dynamic>,
            ),
      includedPrefixes: List.unmodifiable(included.cast<String>()),
      hiddenPrefixes: List.unmodifiable(hidden.cast<String>()),
      colors: List.unmodifiable(
        colors.map(
          (value) => TagColorRule.fromJson(value as Map<String, dynamic>),
        ),
      ),
      noteStructure: noteStructure == null
          ? const GalleryNoteStructureSettings()
          : GalleryNoteStructureSettings.fromJson(
              noteStructure as Map<String, dynamic>,
            ),
      pagination: pagination == null
          ? const GalleryPaginationSettings()
          : GalleryPaginationSettings.fromJson(
              pagination as Map<String, dynamic>,
            ),
    );
  }

  static bool _matchesPrefix(String tag, String prefix) =>
      prefix == '*' || tag == prefix || tag.startsWith('$prefix/');
}

/// One filter category. [path] is an exact tag (`source/art`) or a subtree
/// (`source/count/*`). Rules sharing a [name] are shown as one category.
class GalleryTagCategoryRule {
  const GalleryTagCategoryRule(this.name, this.path, {this.splitDeep = false});

  final String name;
  final String path;

  /// Split nested tags under a `/*` path into one category per parent tag.
  final bool splitDeep;

  GalleryTagCategoryRule copyWith({
    String? name,
    String? path,
    bool? splitDeep,
  }) => GalleryTagCategoryRule(
    name ?? this.name,
    path ?? this.path,
    splitDeep: splitDeep ?? this.splitDeep,
  );

  Map<String, Object> toJson() => {
    'name': name,
    'path': path,
    'splitDeep': splitDeep,
  };

  static GalleryTagCategoryRule fromJson(Map<String, dynamic> json) {
    final name = json['name'];
    final path = json['path'];
    if (name is! String ||
        name.trim().isEmpty ||
        utf8.encode(name).length > 128 ||
        path is! String ||
        !isValidCategoryPath(path)) {
      throw const FormatException('Invalid tag category rule.');
    }
    return GalleryTagCategoryRule(
      name,
      path,
      splitDeep: _readBool(json, 'splitDeep'),
    );
  }

  static bool isValidCategoryPath(String path) {
    if (path == '*') return true;
    final base = path.endsWith('/*')
        ? path.substring(0, path.length - 2)
        : path;
    return base.isNotEmpty &&
        utf8.encode(base).length <= 256 &&
        !base.contains('*') &&
        base.split('/').every((segment) => segment.trim().isNotEmpty);
  }
}

class GalleryOtherCategorySettings {
  const GalleryOtherCategorySettings({
    this.enabled = true,
    this.name = 'その他',
    this.splitDeep = false,
  });

  final bool enabled;
  final String name;
  final bool splitDeep;

  GalleryOtherCategorySettings copyWith({
    bool? enabled,
    String? name,
    bool? splitDeep,
  }) => GalleryOtherCategorySettings(
    enabled: enabled ?? this.enabled,
    name: name ?? this.name,
    splitDeep: splitDeep ?? this.splitDeep,
  );

  Map<String, Object> toJson() => {
    'enabled': enabled,
    'name': name,
    'splitDeep': splitDeep,
  };

  static GalleryOtherCategorySettings fromJson(Map<String, dynamic> json) {
    final name = json['name'] ?? 'その他';
    if (name is! String ||
        name.trim().isEmpty ||
        utf8.encode(name).length > 128) {
      throw const FormatException('Invalid other category name.');
    }
    return GalleryOtherCategorySettings(
      enabled: _readBool(json, 'enabled', defaultValue: true),
      name: name,
      splitDeep: _readBool(json, 'splitDeep'),
    );
  }
}

class GalleryTagCategorySettings {
  const GalleryTagCategorySettings({
    this.categories = defaultCategories,
    this.other = const GalleryOtherCategorySettings(),
  });

  static const maxCategories = 64;
  static const defaultCategories = <GalleryTagCategoryRule>[
    GalleryTagCategoryRule('ソース', 'source/art'),
    GalleryTagCategoryRule('人数', 'source/count/*'),
    GalleryTagCategoryRule('アートスタイル', 'source/format/*'),
    GalleryTagCategoryRule('性別', 'source/gender/*'),
    GalleryTagCategoryRule('メタ', 'source/meta/*'),
    GalleryTagCategoryRule('レーティング', 'source/rating/*'),
    GalleryTagCategoryRule('ソース', 'source/*'),
    GalleryTagCategoryRule('タイプ', 'source/type/*'),
    GalleryTagCategoryRule('作品', 'copyright/*'),
  ];

  final List<GalleryTagCategoryRule> categories;
  final GalleryOtherCategorySettings other;

  GalleryTagCategorySettings copyWith({
    List<GalleryTagCategoryRule>? categories,
    GalleryOtherCategorySettings? other,
  }) => GalleryTagCategorySettings(
    categories: categories ?? this.categories,
    other: other ?? this.other,
  );

  Map<String, Object> toJson() => {
    'categories': categories
        .map((rule) => rule.toJson())
        .toList(growable: false),
    'other': other.toJson(),
  };

  static GalleryTagCategorySettings fromJson(Map<String, dynamic> json) {
    final categories = json['categories'];
    final other = json['other'];
    if (categories != null &&
        (categories is! List ||
            categories.length > maxCategories ||
            !categories.every((value) => value is Map<String, dynamic>))) {
      throw const FormatException('Invalid tag category list.');
    }
    if (other != null && other is! Map<String, dynamic>) {
      throw const FormatException('Invalid other category settings.');
    }
    return GalleryTagCategorySettings(
      categories: categories == null
          ? defaultCategories
          : List.unmodifiable(
              (categories as List).map(
                (value) => GalleryTagCategoryRule.fromJson(
                  value as Map<String, dynamic>,
                ),
              ),
            ),
      other: other == null
          ? const GalleryOtherCategorySettings()
          : GalleryOtherCategorySettings.fromJson(
              other as Map<String, dynamic>,
            ),
    );
  }
}

class GalleryNoteStructureSettings {
  const GalleryNoteStructureSettings({
    this.memoHeadings = const ['覚書', 'メモ'],
    this.relatedHeadings = const ['関連'],
    this.postTextEndHeadings = const ['文書'],
    this.galleryTagPrefixes = const ['source/art'],
    this.frontmatter = const GalleryFrontmatterSettings(),
    this.linkResolution = GalleryLinkResolution.relativePath,
    this.postTextIncludeQuote = true,
    this.blockOrder = defaultBlockOrder,
    this.hiddenBlocks = const [],
  });

  /// Default display order for the reorderable blocks. Frontmatter is not
  /// part of this list: it is always shown first and cannot be reordered.
  static const defaultBlockOrder = [
    GalleryNoteBlock.author,
    GalleryNoteBlock.media,
    GalleryNoteBlock.postText,
    GalleryNoteBlock.postTextEnd,
    GalleryNoteBlock.related,
    GalleryNoteBlock.memo,
  ];

  final List<String> memoHeadings;
  final List<String> relatedHeadings;
  final List<String> postTextEndHeadings;
  final List<String> galleryTagPrefixes;
  final GalleryFrontmatterSettings frontmatter;
  final GalleryLinkResolution linkResolution;

  /// When `false`, blockquoted lines are excluded from the extracted post
  /// text instead of being kept with the quote marker stripped.
  final bool postTextIncludeQuote;

  /// Display order of the reorderable note detail blocks. Frontmatter is
  /// fixed and always shown first, so it is not included here.
  final List<GalleryNoteBlock> blockOrder;
  final List<GalleryNoteBlock> hiddenBlocks;

  GalleryNoteStructureSettings copyWith({
    List<String>? memoHeadings,
    List<String>? relatedHeadings,
    List<String>? postTextEndHeadings,
    List<String>? galleryTagPrefixes,
    GalleryFrontmatterSettings? frontmatter,
    GalleryLinkResolution? linkResolution,
    bool? postTextIncludeQuote,
    List<GalleryNoteBlock>? blockOrder,
    List<GalleryNoteBlock>? hiddenBlocks,
  }) => GalleryNoteStructureSettings(
    memoHeadings: memoHeadings ?? this.memoHeadings,
    relatedHeadings: relatedHeadings ?? this.relatedHeadings,
    postTextEndHeadings: postTextEndHeadings ?? this.postTextEndHeadings,
    galleryTagPrefixes: galleryTagPrefixes ?? this.galleryTagPrefixes,
    frontmatter: frontmatter ?? this.frontmatter,
    linkResolution: linkResolution ?? this.linkResolution,
    postTextIncludeQuote: postTextIncludeQuote ?? this.postTextIncludeQuote,
    blockOrder: blockOrder ?? this.blockOrder,
    hiddenBlocks: hiddenBlocks ?? this.hiddenBlocks,
  );

  Map<String, Object> toJson() => {
    'memoHeadings': memoHeadings,
    'relatedHeadings': relatedHeadings,
    'postTextEndHeadings': postTextEndHeadings,
    'galleryTagPrefixes': galleryTagPrefixes,
    'frontmatter': frontmatter.toJson(),
    'linkResolution': linkResolution.name,
    'postTextIncludeQuote': postTextIncludeQuote,
    'blockOrder': blockOrder.map((block) => block.name).toList(growable: false),
    'hiddenBlocks': hiddenBlocks
        .map((block) => block.name)
        .toList(growable: false),
  };

  static GalleryNoteStructureSettings fromJson(Map<String, dynamic> json) {
    List<String> readHeadings(
      String key,
      List<String> defaults, {
      int maxBytes = 256,
    }) {
      final value = json[key];
      if (value == null) return defaults;
      if (value is! List ||
          !value.every(
            (heading) =>
                heading is String &&
                heading.trim().isNotEmpty &&
                utf8.encode(heading).length <= maxBytes,
          )) {
        throw FormatException('Invalid note structure heading list: $key.');
      }
      return List.unmodifiable(value.cast<String>());
    }

    final frontmatter = json['frontmatter'];
    if (frontmatter != null && frontmatter is! Map<String, dynamic>) {
      throw const FormatException('Invalid frontmatter key settings.');
    }
    final linkResolutionValue = json['linkResolution'] ?? 'relativePath';
    if (linkResolutionValue is! String) {
      throw const FormatException('Invalid link resolution setting.');
    }
    final linkResolution = GalleryLinkResolution.values
        .where((mode) => mode.name == linkResolutionValue)
        .firstOrNull;
    if (linkResolution == null) {
      throw const FormatException('Unknown link resolution setting.');
    }

    return GalleryNoteStructureSettings(
      memoHeadings: readHeadings('memoHeadings', const ['覚書', 'メモ']),
      relatedHeadings: readHeadings('relatedHeadings', const ['関連']),
      postTextEndHeadings: readHeadings('postTextEndHeadings', const ['文書']),
      galleryTagPrefixes: readHeadings('galleryTagPrefixes', const [
        'source/art',
      ], maxBytes: 128),
      frontmatter: frontmatter == null
          ? const GalleryFrontmatterSettings()
          : GalleryFrontmatterSettings.fromJson(
              frontmatter as Map<String, dynamic>,
            ),
      linkResolution: linkResolution,
      postTextIncludeQuote: _readBool(
        json,
        'postTextIncludeQuote',
        defaultValue: true,
      ),
      blockOrder: _readBlockOrder(json['blockOrder']),
      hiddenBlocks: _readHiddenBlocks(json['hiddenBlocks']),
    );
  }
}

bool _readBool(
  Map<String, dynamic> json,
  String key, {
  bool defaultValue = false,
}) {
  final value = json[key];
  if (value == null) return defaultValue;
  if (value is! bool) {
    throw FormatException('Invalid boolean setting: $key.');
  }
  return value;
}

List<GalleryNoteBlock> _readBlockOrder(dynamic value) {
  if (value == null) {
    return GalleryNoteStructureSettings.defaultBlockOrder;
  }
  if (value is! List) {
    throw const FormatException('Invalid note block order.');
  }
  final blocks = <GalleryNoteBlock>[];
  for (final entry in value) {
    if (entry is! String) {
      throw const FormatException('Invalid note block order entry.');
    }
    final block = GalleryNoteBlock.values
        .where((candidate) => candidate.name == entry)
        .firstOrNull;
    if (block == null) {
      throw FormatException('Unknown note block: $entry.');
    }
    if (blocks.contains(block)) {
      throw FormatException('Duplicate note block: $entry.');
    }
    blocks.add(block);
  }
  const legacyDefaultOrders = [
    [
      GalleryNoteBlock.author,
      GalleryNoteBlock.media,
      GalleryNoteBlock.postText,
      GalleryNoteBlock.memo,
      GalleryNoteBlock.related,
      GalleryNoteBlock.postTextEnd,
    ],
    [
      GalleryNoteBlock.author,
      GalleryNoteBlock.media,
      GalleryNoteBlock.postText,
      GalleryNoteBlock.related,
      GalleryNoteBlock.memo,
      GalleryNoteBlock.postTextEnd,
    ],
  ];
  if (legacyDefaultOrders.any(
    (legacyOrder) =>
        blocks.length == legacyOrder.length &&
        blocks.toString() == legacyOrder.toString(),
  )) {
    return GalleryNoteStructureSettings.defaultBlockOrder;
  }
  for (final block in GalleryNoteStructureSettings.defaultBlockOrder) {
    if (!blocks.contains(block)) blocks.add(block);
  }
  return List.unmodifiable(blocks);
}

List<GalleryNoteBlock> _readHiddenBlocks(dynamic value) {
  if (value == null) return const [];
  if (value is! List) {
    throw const FormatException('Invalid hidden note blocks.');
  }
  final blocks = <GalleryNoteBlock>[];
  for (final entry in value) {
    if (entry is! String) {
      throw const FormatException('Invalid hidden note block entry.');
    }
    final block = GalleryNoteBlock.values
        .where((candidate) => candidate.name == entry)
        .firstOrNull;
    if (block == null || block == GalleryNoteBlock.postTextEnd) {
      throw FormatException('Unknown visible note block: $entry.');
    }
    if (blocks.contains(block)) {
      throw FormatException('Duplicate hidden note block: $entry.');
    }
    blocks.add(block);
  }
  return List.unmodifiable(blocks);
}

enum GalleryLinkResolution { shortestPath, relativePath, absolutePath }

/// The reorderable blocks shown in a note's detail view. Frontmatter (tags
/// and metadata extracted from YAML frontmatter) is intentionally excluded:
/// it is always shown first and cannot be reordered.
enum GalleryNoteBlock { author, media, postText, memo, related, postTextEnd }

extension GalleryNoteBlockLabel on GalleryNoteBlock {
  String get label => switch (this) {
    GalleryNoteBlock.author => '投稿者',
    GalleryNoteBlock.media => 'メディア',
    GalleryNoteBlock.postText => '投稿文',
    GalleryNoteBlock.memo => '覚書',
    GalleryNoteBlock.related => '関連',
    GalleryNoteBlock.postTextEnd => '投稿文の終端',
  };

  IconData get icon => switch (this) {
    GalleryNoteBlock.author => Icons.person_outline,
    GalleryNoteBlock.media => Icons.image_outlined,
    GalleryNoteBlock.postText => Icons.article_outlined,
    GalleryNoteBlock.memo => Icons.mode_comment_outlined,
    GalleryNoteBlock.related => Icons.link,
    GalleryNoteBlock.postTextEnd => Icons.vertical_align_bottom,
  };
}

class GalleryPaginationSettings {
  const GalleryPaginationSettings({
    this.pageSize = defaultPageSize,
    this.showItemCount = true,
    this.showItemNumberOnTiles = false,
    this.showMissingMediaIcon = false,
  });

  static const defaultPageSize = 24;
  static const minPageSize = 1;
  static const maxPageSize = 500;

  /// Number of items fetched per gallery page, both for the initial load and
  /// for each subsequent "load more" request.
  final int pageSize;

  /// Whether the gallery UI shows a running count of currently loaded items.
  final bool showItemCount;

  /// Whether each gallery tile shows its one-based position in the result list.
  final bool showItemNumberOnTiles;

  /// Whether a missing/loading media thumbnail displays an image or video icon.
  final bool showMissingMediaIcon;

  GalleryPaginationSettings copyWith({
    int? pageSize,
    bool? showItemCount,
    bool? showItemNumberOnTiles,
    bool? showMissingMediaIcon,
  }) => GalleryPaginationSettings(
    pageSize: pageSize ?? this.pageSize,
    showItemCount: showItemCount ?? this.showItemCount,
    showItemNumberOnTiles: showItemNumberOnTiles ?? this.showItemNumberOnTiles,
    showMissingMediaIcon: showMissingMediaIcon ?? this.showMissingMediaIcon,
  );

  Map<String, Object> toJson() => {
    'pageSize': pageSize,
    'showItemCount': showItemCount,
    'showItemNumberOnTiles': showItemNumberOnTiles,
    'showMissingMediaIcon': showMissingMediaIcon,
  };

  static GalleryPaginationSettings fromJson(Map<String, dynamic> json) {
    final pageSize = json['pageSize'] ?? defaultPageSize;
    final showItemCount = json['showItemCount'] ?? true;
    final showItemNumberOnTiles =
        json['showItemNumberOnTiles'] ?? json['showMediaCountOnTiles'] ?? false;
    final showMissingMediaIcon = json['showMissingMediaIcon'] ?? false;
    if (pageSize is! int || pageSize < minPageSize || pageSize > maxPageSize) {
      throw const FormatException('Invalid gallery pagination page size.');
    }
    if (showItemCount is! bool) {
      throw const FormatException(
        'Invalid gallery pagination item count flag.',
      );
    }
    if (showItemNumberOnTiles is! bool) {
      throw const FormatException('Invalid gallery item number flag.');
    }
    if (showMissingMediaIcon is! bool) {
      throw const FormatException('Invalid missing media icon flag.');
    }
    return GalleryPaginationSettings(
      pageSize: pageSize,
      showItemCount: showItemCount,
      showItemNumberOnTiles: showItemNumberOnTiles,
      showMissingMediaIcon: showMissingMediaIcon,
    );
  }
}

class GalleryFrontmatterSettings {
  const GalleryFrontmatterSettings({
    this.tagsKeys = const ['tags'],
    this.titleKeys = const ['title'],
    this.urlKeys = const ['url'],
    this.publishedKeys = const ['published'],
    this.createdKeys = const ['created'],
    this.updatedKeys = const ['updated'],
    this.coverKeys = const ['cover'],
  });

  final List<String> tagsKeys;
  final List<String> titleKeys;
  final List<String> urlKeys;
  final List<String> publishedKeys;
  final List<String> createdKeys;
  final List<String> updatedKeys;
  final List<String> coverKeys;

  GalleryFrontmatterSettings copyWith({
    List<String>? tagsKeys,
    List<String>? titleKeys,
    List<String>? urlKeys,
    List<String>? publishedKeys,
    List<String>? createdKeys,
    List<String>? updatedKeys,
    List<String>? coverKeys,
  }) => GalleryFrontmatterSettings(
    tagsKeys: tagsKeys ?? this.tagsKeys,
    titleKeys: titleKeys ?? this.titleKeys,
    urlKeys: urlKeys ?? this.urlKeys,
    publishedKeys: publishedKeys ?? this.publishedKeys,
    createdKeys: createdKeys ?? this.createdKeys,
    updatedKeys: updatedKeys ?? this.updatedKeys,
    coverKeys: coverKeys ?? this.coverKeys,
  );

  Map<String, Object> toJson() => {
    'tagsKeys': tagsKeys,
    'titleKeys': titleKeys,
    'urlKeys': urlKeys,
    'publishedKeys': publishedKeys,
    'createdKeys': createdKeys,
    'updatedKeys': updatedKeys,
    'coverKeys': coverKeys,
  };

  static GalleryFrontmatterSettings fromJson(Map<String, dynamic> json) {
    List<String> readKeys(String key, List<String> defaults) {
      final value = json[key];
      if (value == null) return defaults;
      if (value is! List ||
          !value.every(
            (entry) =>
                entry is String &&
                entry.trim().isNotEmpty &&
                utf8.encode(entry).length <= 128,
          )) {
        throw FormatException('Invalid frontmatter keys: $key.');
      }
      return List.unmodifiable(value.cast<String>());
    }

    final tagsKeys = readKeys('tagsKeys', const ['tags']);
    if (tagsKeys.isEmpty) {
      throw const FormatException(
        'At least one frontmatter tag key is required.',
      );
    }
    return GalleryFrontmatterSettings(
      tagsKeys: tagsKeys,
      titleKeys: readKeys('titleKeys', const ['title']),
      urlKeys: readKeys('urlKeys', const ['url']),
      publishedKeys: readKeys('publishedKeys', const ['published']),
      createdKeys: readKeys('createdKeys', const ['created']),
      updatedKeys: readKeys('updatedKeys', const ['updated']),
      coverKeys: readKeys('coverKeys', const ['cover']),
    );
  }
}

class TagColorRule {
  const TagColorRule(this.prefix, this.color);

  final String prefix;
  final int color;

  Map<String, Object> toJson() => {
    'prefix': prefix,
    'color': '#${color.toRadixString(16).padLeft(8, '0').substring(2)}',
  };

  static TagColorRule fromJson(Map<String, dynamic> json) {
    final prefix = json['prefix'];
    final color = json['color'];
    if (prefix is! String ||
        prefix.isEmpty ||
        color is! String ||
        !RegExp(r'^#[0-9a-fA-F]{6}$').hasMatch(color)) {
      throw const FormatException('Invalid gallery tag color rule.');
    }
    return TagColorRule(
      prefix,
      int.parse(color.substring(1), radix: 16) | 0xFF000000,
    );
  }
}

final galleryTagSettingsProvider =
    AsyncNotifierProvider<GalleryTagSettingsController, GalleryTagSettings>(
      GalleryTagSettingsController.new,
    );

class GalleryTagSettingsController extends AsyncNotifier<GalleryTagSettings> {
  File? _settingsFile;

  @override
  Future<GalleryTagSettings> build() async {
    final paths = await ref.read(vaultPlatformProvider).galleryPaths();
    final file = File(p.join(paths.dataDirectory, 'tag-settings.json'));
    _settingsFile = file;
    if (!await file.exists()) return const GalleryTagSettings();
    final decoded = jsonDecode(await file.readAsString());
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Invalid gallery tag settings.');
    }
    return GalleryTagSettings.fromJson(decoded);
  }

  Future<void> saveSettings(GalleryTagSettings settings) async {
    final file = _settingsFile;
    if (file == null) {
      throw StateError('Gallery tag settings are not initialized.');
    }
    await file.parent.create(recursive: true);
    await file.writeAsString(jsonEncode(settings.toJson()), flush: true);
    state = AsyncData(settings);
  }

  Future<void> deleteSettings() async {
    final file = _settingsFile;
    if (file == null) {
      throw StateError('Gallery tag settings are not initialized.');
    }
    if (await file.exists()) await file.delete();
    state = const AsyncData(GalleryTagSettings());
  }
}
