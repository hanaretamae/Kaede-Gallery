import 'dart:convert';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';

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

class AndroidSafAccess {
  const AndroidSafAccess();

  static const _channel = MethodChannel('com.hanaretamae.vault_gallery/saf');

  Future<String?> chooseVault() => _channel.invokeMethod<String>('chooseVault');

  Future<String?> loadVault() => _channel.invokeMethod<String>('loadVault');

  Future<List<SafFileEntry>> listFiles(String vaultUri) async {
    final entries = await _channel.invokeListMethod<Object?>('listFiles', {
      'vaultUri': vaultUri,
    });
    if (entries == null) {
      throw StateError('No folder listing was returned.');
    }
    return entries
        .map((entry) {
          if (entry is! Map<Object?, Object?> ||
              entry['path'] is! String ||
              entry['modifiedNanos'] is! int ||
              entry['size'] is! int ||
              entry['documentUri'] is! String ||
              !(entry['documentUri']! as String).startsWith('content://')) {
            throw StateError('An invalid folder entry was returned.');
          }
          return SafFileEntry(
            path: entry['path']! as String,
            modifiedNanos: entry['modifiedNanos']! as int,
            size: entry['size']! as int,
            documentUri: entry['documentUri']! as String,
          );
        })
        .toList(growable: false);
  }

  Future<Map<String, Uint8List?>> readListedFiles(
    String vaultUri,
    List<SafFileEntry> files,
  ) async {
    const maxNoteBytes = 2 * 1024 * 1024;
    const maxBatchBytes = 16 * 1024 * 1024;
    const maxBatchFiles = 128;
    final contents = <String, Uint8List?>{};
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

    for (final file in files) {
      if (file.size > maxNoteBytes) {
        contents[file.path] = null;
        continue;
      }
      final expectedBytes = file.size == 0 ? maxNoteBytes : file.size;
      if (batch.isNotEmpty &&
          (batch.length == maxBatchFiles ||
              batchBytes + expectedBytes > maxBatchBytes)) {
        await flushBatch();
      }
      batch.add(file);
      batchBytes += expectedBytes;
    }
    await flushBatch();
    return contents;
  }

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

  Future<void> openMedia(
    String vaultUri,
    String? mediaUri, {
    bool openFolder = false,
  }) => _channel.invokeMethod<void>('openMedia', {
    'vaultUri': vaultUri,
    'mediaUri': mediaUri,
    'openFolder': openFolder,
  });

  Future<bool> saveJson(String fileName, String contents) async {
    final saved = await _channel.invokeMethod<bool>('saveJson', {
      'fileName': fileName,
      'content': Uint8List.fromList(utf8.encode(contents)),
    });
    return saved ?? false;
  }

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

  Future<int?> openMediaFileDescriptor(String vaultUri, String mediaUri) =>
      _channel.invokeMethod<int>('openMediaFd', {
        'vaultUri': vaultUri,
        'mediaUri': mediaUri,
      });

  Future<void> closeMediaFileDescriptor(int descriptor) =>
      _channel.invokeMethod<void>('closeMediaFd', {'descriptor': descriptor});
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
}

class NativeVaultPlatform implements VaultPlatform {
  const NativeVaultPlatform();

  @override
  Future<String?> chooseVault() => Platform.isAndroid
      ? const AndroidSafAccess().chooseVault()
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
