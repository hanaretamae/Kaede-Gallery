import 'dart:convert';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';

import 'saf_limits.g.dart';

final _blockedVaultCacheOperations = <String>{};
final _pendingVaultCacheOperations = <String, Set<Future<void>>>{};

bool vaultCacheOperationsAllowed(String vaultPath) =>
    !_blockedVaultCacheOperations.contains(vaultPath);

void resumeVaultCacheOperations(String vaultPath) {
  _blockedVaultCacheOperations.remove(vaultPath);
}

void trackVaultCacheOperation(String vaultPath, Future<void> operation) {
  if (!vaultCacheOperationsAllowed(vaultPath)) return;
  final pending = _pendingVaultCacheOperations.putIfAbsent(
    vaultPath,
    () => <Future<void>>{},
  );
  pending.add(operation);
  operation.then<void>(
    (_) => _finishVaultCacheOperation(vaultPath, pending, operation),
    onError: (Object _, StackTrace _) =>
        _finishVaultCacheOperation(vaultPath, pending, operation),
  );
}

Future<void> preventVaultCacheOperations(String vaultPath) async {
  _blockedVaultCacheOperations.add(vaultPath);
  final pending = _pendingVaultCacheOperations[vaultPath]?.toList() ?? [];
  await Future.wait<void>(
    pending.map(
      (operation) =>
          operation.then<void>((_) {}, onError: (Object _, StackTrace _) {}),
    ),
  ).timeout(const Duration(seconds: 30));
}

void _finishVaultCacheOperation(
  String vaultPath,
  Set<Future<void>> pending,
  Future<void> operation,
) {
  pending.remove(operation);
  if (pending.isEmpty) _pendingVaultCacheOperations.remove(vaultPath);
}

class SafFileEntry {
  const SafFileEntry({
    required this.path,
    required this.modifiedNanos,
    required this.size,
    required this.documentUri,
  });

  final String path;
  final int modifiedNanos;
  final int size;
  final String documentUri;
}

abstract interface class SafVaultAccess {
  Future<String?> chooseVault();
  Future<String?> loadVault();
  Future<void> forgetVault(String vaultUri);
  Future<List<SafFileEntry>> listFiles(String vaultUri);
  Future<Map<String, Uint8List?>> readListedFiles(
    String vaultUri,
    List<SafFileEntry> files,
  );
  Future<Uint8List?> readFile(String vaultUri, String relativePath);
  Future<String?> resolveFile(
    String vaultUri,
    String relativePath, {
    bool video = false,
  });
  Future<Uint8List> readMedia(String vaultUri, String mediaUri);
  Future<void> openMedia(
    String vaultUri,
    String? mediaUri, {
    bool revealInFileManager = false,
    bool setAsWallpaper = false,
  });
  Future<bool> saveJson(String fileName, String contents);
  Future<Uint8List?> thumbnail(
    String vaultUri,
    String relativePath, {
    required bool video,
  });
  Future<int?> openMediaFileDescriptor(String vaultUri, String mediaUri);
  Future<void> closeMediaFileDescriptor(int descriptor);
}

String vaultDisplayName(String vaultPath) {
  final uri = Uri.tryParse(vaultPath);
  if (uri == null || uri.scheme != 'content') return path.basename(vaultPath);
  final segments = uri.pathSegments;
  final treeIndex = segments.indexOf('tree');
  if (treeIndex < 0 || treeIndex + 1 >= segments.length) return vaultPath;
  final documentId = segments[treeIndex + 1];
  final separator = documentId.indexOf(':');
  final relativePath = separator < 0
      ? documentId
      : documentId.substring(separator + 1);
  final name = path.posix.basename(relativePath);
  return name.isEmpty ? vaultPath : name;
}

Uri obsidianOpenUri(String vaultPath, String notePath) {
  final queryParameters = vaultPath.startsWith('content://')
      ? {'vault': vaultDisplayName(vaultPath), 'file': notePath}
      : {'path': path.join(vaultPath, notePath)};
  return Uri(
    scheme: 'obsidian',
    host: 'open',
    queryParameters: queryParameters,
  );
}

class AndroidSafAccess implements SafVaultAccess {
  const AndroidSafAccess();

  static const _channel = MethodChannel('com.hanaretamae.vault_gallery/saf');

  @override
  Future<String?> chooseVault() async {
    final vaultUri = await _channel.invokeMethod<String>('chooseVault');
    if (vaultUri != null) resumeVaultCacheOperations(vaultUri);
    return vaultUri;
  }

  @override
  Future<String?> loadVault() => _channel.invokeMethod<String>('loadVault');

  @override
  Future<void> forgetVault(String vaultUri) async {
    await preventVaultCacheOperations(vaultUri);
    await _channel.invokeMethod<void>('forgetVault', {'vaultUri': vaultUri});
  }

  @override
  Future<List<SafFileEntry>> listFiles(String vaultUri) async {
    final entries = await _channel.invokeListMethod<Object?>('listFiles', {
      'vaultUri': vaultUri,
    });
    if (entries == null) {
      throw StateError('No folder listing was returned.');
    }
    if (entries.length > SafLimits.maxDocuments) {
      throw StateError('The folder listing exceeds its size limit.');
    }
    var aggregatePathBytes = 0;
    final seenPaths = <String>{};
    return entries
        .map((entry) {
          if (entry is! Map<Object?, Object?> ||
              entry['path'] is! String ||
              entry['modifiedNanos'] is! int ||
              (entry['modifiedNanos']! as int) < 0 ||
              entry['size'] is! int ||
              (entry['size']! as int) < 0 ||
              entry['documentUri'] is! String ||
              !(entry['documentUri']! as String).startsWith('content://')) {
            throw StateError('An invalid folder entry was returned.');
          }
          final relativePath = entry['path']! as String;
          if (!_isWithinSafPathLimits(relativePath)) {
            throw StateError('An invalid folder path was returned.');
          }
          if (!seenPaths.add(relativePath)) {
            throw StateError('A duplicate folder path was returned.');
          }
          aggregatePathBytes += utf8.encode(relativePath).length;
          if (aggregatePathBytes > SafLimits.maxAggregatePathBytes) {
            throw StateError('The folder listing exceeds its path-size limit.');
          }
          return SafFileEntry(
            path: relativePath,
            modifiedNanos: entry['modifiedNanos']! as int,
            size: entry['size']! as int,
            documentUri: entry['documentUri']! as String,
          );
        })
        .toList(growable: false);
  }

  @override
  Future<Map<String, Uint8List?>> readListedFiles(
    String vaultUri,
    List<SafFileEntry> files,
  ) async {
    if (files.length > SafLimits.maxDocuments) {
      throw StateError('The folder contains too many notes.');
    }
    final contents = <String, Uint8List?>{};
    final boundedFiles = <({SafFileEntry file, int expectedBytes})>[];
    var scanBytes = 0;
    var aggregatePathBytes = 0;
    for (final file in files) {
      if (!_isWithinSafPathLimits(file.path) ||
          !file.documentUri.startsWith('content://') ||
          file.size < 0 ||
          file.modifiedNanos < 0) {
        throw StateError('An invalid folder entry was returned.');
      }
      aggregatePathBytes += utf8.encode(file.path).length;
      if (aggregatePathBytes > SafLimits.maxAggregatePathBytes) {
        throw StateError('The folder listing exceeds its path-size limit.');
      }
      if (file.size > SafLimits.maxNoteBytes) {
        contents[file.path] = null;
        continue;
      }
      final expectedBytes = file.size == 0
          ? SafLimits.maxNoteBytes
          : file.size < SafLimits.maxNoteBytes
          ? file.size + 1
          : SafLimits.maxNoteBytes;
      if (scanBytes + expectedBytes > SafLimits.maxScanNoteBytes) {
        throw StateError('The selected folder exceeds its note-size limit.');
      }
      scanBytes += expectedBytes;
      boundedFiles.add((file: file, expectedBytes: expectedBytes));
    }

    var batch = <SafFileEntry>[];
    var batchBytes = 0;

    Future<void> flushBatch() async {
      if (batch.isEmpty) return;
      final documents = await _channel.invokeListMethod<Object?>(
        'readListedFiles',
        {
          'vaultUri': vaultUri,
          'documents': batch
              .map(
                (file) => {
                  'path': file.path,
                  'documentUri': file.documentUri,
                  'size': file.size,
                },
              )
              .toList(growable: false),
        },
      );
      if (documents == null || documents.length != batch.length) {
        throw StateError('An incomplete note batch was returned.');
      }
      for (var index = 0; index < documents.length; index++) {
        final document = documents[index];
        final file = batch[index];
        if (document is! Map<Object?, Object?> ||
            document['path'] != file.path ||
            (document['content'] != null &&
                document['content'] is! Uint8List)) {
          throw StateError('An invalid note batch was returned.');
        }
        contents[file.path] = document['content'] as Uint8List?;
      }
      batch = [];
      batchBytes = 0;
    }

    for (final item in boundedFiles) {
      final file = item.file;
      final expectedBytes = item.expectedBytes;
      if (batch.isNotEmpty &&
          (batch.length == SafLimits.maxReadBatchNotes ||
              batchBytes + expectedBytes > SafLimits.maxReadBatchBytes)) {
        await flushBatch();
      }
      batch.add(file);
      batchBytes += expectedBytes;
    }
    await flushBatch();
    return contents;
  }

  @override
  Future<Uint8List?> readFile(String vaultUri, String relativePath) async {
    try {
      return await _channel.invokeMethod<Uint8List>('readFile', {
        'vaultUri': vaultUri,
        'path': relativePath,
      });
    } on PlatformException catch (error) {
      if (error.code == 'SAF_IO') return null;
      rethrow;
    }
  }

  @override
  Future<String?> resolveFile(
    String vaultUri,
    String relativePath, {
    bool video = false,
  }) async {
    final uri = await _channel.invokeMethod<String>('resolveFile', {
      'vaultUri': vaultUri,
      'path': relativePath,
      'video': video,
    });
    if (uri == null && video) return null;
    if (uri == null || !uri.startsWith('content://')) {
      throw StateError('The selected file could not be resolved.');
    }
    return uri;
  }

  @override
  Future<Uint8List> readMedia(String vaultUri, String mediaUri) async {
    final bytes = await _channel.invokeMethod<Uint8List>('readMediaImage', {
      'vaultUri': vaultUri,
      'mediaUri': mediaUri,
      'maxDimension': 2048,
    });
    if (bytes == null) {
      throw StateError('No media content was returned.');
    }
    return bytes;
  }

  @override
  Future<void> openMedia(
    String vaultUri,
    String? mediaUri, {
    bool revealInFileManager = false,
    bool setAsWallpaper = false,
  }) => _channel.invokeMethod<void>('openMedia', {
    'vaultUri': vaultUri,
    'mediaUri': mediaUri,
    'revealInFileManager': revealInFileManager,
    if (setAsWallpaper) 'setAsWallpaper': true,
  });

  @override
  Future<bool> saveJson(String fileName, String contents) async {
    final saved = await _channel.invokeMethod<bool>('saveJson', {
      'fileName': fileName,
      'content': Uint8List.fromList(utf8.encode(contents)),
    });
    return saved ?? false;
  }

  @override
  Future<Uint8List?> thumbnail(
    String vaultUri,
    String relativePath, {
    required bool video,
  }) => _channel.invokeMethod<Uint8List>('thumbnail', {
    'vaultUri': vaultUri,
    'path': relativePath,
    'video': video,
    'size': 320,
  });

  @override
  Future<int?> openMediaFileDescriptor(String vaultUri, String mediaUri) =>
      _channel.invokeMethod<int>('openMediaFd', {
        'vaultUri': vaultUri,
        'mediaUri': mediaUri,
      });

  @override
  Future<void> closeMediaFileDescriptor(int descriptor) =>
      _channel.invokeMethod<void>('closeMediaFd', {'descriptor': descriptor});
}

bool _isWithinSafPathLimits(String value) {
  final segments = value.split('/');
  return value.isNotEmpty &&
      utf8.encode(value).length <= SafLimits.maxRelativePathBytes &&
      !value.startsWith('/') &&
      !value.contains('\\') &&
      !value.contains('\u0000') &&
      segments.length - 1 <= SafLimits.maxDepth &&
      segments.every(
        (segment) => segment.isNotEmpty && segment != '.' && segment != '..',
      );
}

class GalleryPaths {
  const GalleryPaths({
    required this.dataDirectory,
    required this.indexPath,
    required this.thumbnailDirectory,
  });

  final String dataDirectory;
  final String indexPath;
  final String thumbnailDirectory;
}

abstract interface class VaultPlatform {
  Future<String?> chooseVault();
  Future<GalleryPaths> galleryPaths();
  SafVaultAccess get safAccess;
}

class NativeVaultPlatform implements VaultPlatform {
  const NativeVaultPlatform();

  @override
  SafVaultAccess get safAccess => const AndroidSafAccess();

  @override
  Future<String?> chooseVault() => Platform.isAndroid
      ? safAccess.chooseVault()
      : getDirectoryPath(confirmButtonText: 'この Vault を選択');

  @override
  Future<GalleryPaths> galleryPaths() async {
    final support = await getApplicationSupportDirectory();
    final directory = path.join(support.path, 'vault-gallery');
    return GalleryPaths(
      dataDirectory: directory,
      indexPath: path.join(directory, 'index.sqlite'),
      thumbnailDirectory: path.join(directory, 'thumbnails'),
    );
  }
}
