import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

import 'vault_platform.dart';

Future<Uint8List?> createAndroidSafVideoThumbnail(
  String vaultUri,
  String source,
  String cacheDirectory,
  int mediaId, {
  required SafVaultAccess safAccess,
}) {
  if (!vaultCacheOperationsAllowed(vaultUri)) return Future.value(null);
  final loading = _createAndroidSafVideoThumbnail(
    vaultUri,
    source,
    cacheDirectory,
    mediaId,
    safAccess: safAccess,
  );
  trackVaultCacheOperation(vaultUri, loading.then<void>((_) {}));
  return loading;
}

Future<Uint8List?> _createAndroidSafVideoThumbnail(
  String vaultUri,
  String source,
  String cacheDirectory,
  int mediaId, {
  required SafVaultAccess safAccess,
}) async {
  final cacheFile = File('$cacheDirectory/saf-v6-$mediaId.png');
  if (await cacheFile.exists()) {
    final size = await cacheFile.length();
    if (size > 0 && size <= 4 * 1024 * 1024) {
      await cacheFile.setLastModified(DateTime.now());
      return cacheFile.readAsBytes();
    }
    await cacheFile.delete();
  }
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
