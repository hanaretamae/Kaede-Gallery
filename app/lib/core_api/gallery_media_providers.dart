import 'dart:typed_data';

import 'package:fc_native_video_thumbnail/fc_native_video_thumbnail.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'gallery_providers.dart';
import '../platform/android_video_thumbnail.dart';
import '../platform/vault_platform.dart';

final galleryThumbnailProvider = FutureProvider.autoDispose
    .family<Uint8List?, int>((ref, mediaId) async {
      final session = await ref.watch(vaultSessionProvider.future);
      if (session == null) {
        return null;
      }
      final repository = ref.read(galleryRepositoryProvider);
      if (session.vaultPath.startsWith('content://')) {
        final videoPath = await repository.getVideoSourcePath(
          session.vaultPath,
          session.paths.indexPath,
          mediaId,
        );
        if (videoPath != null) {
          final thumbnail = await createAndroidSafVideoThumbnail(
            session.vaultPath,
            videoPath,
            session.paths.thumbnailDirectory,
            mediaId,
          );
          if (thumbnail != null) return thumbnail;
        }
        return repository.getThumbnail(
          session.vaultPath,
          session.paths.indexPath,
          session.paths.thumbnailDirectory,
          mediaId,
        );
      }
      final thumbnail = await repository.getThumbnail(
        session.vaultPath,
        session.paths.indexPath,
        session.paths.thumbnailDirectory,
        mediaId,
      );
      if (thumbnail != null) {
        return thumbnail;
      }
      final videoPath = await repository.getVideoSourcePath(
        session.vaultPath,
        session.paths.indexPath,
        mediaId,
      );
      if (videoPath == null) {
        return null;
      }
      return FcNativeVideoThumbnail().saveThumbnailToBytes(
        srcFile: videoPath,
        width: 320,
        height: 320,
        quality: 75,
      );
    });

final gallerySafImageBytesProvider = FutureProvider.family<Uint8List, String>((
  ref,
  mediaUri,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null || !session.vaultPath.startsWith('content://')) {
    throw StateError('Folder access is not active.');
  }
  return const AndroidSafAccess().readMedia(session.vaultPath, mediaUri);
});

final galleryMediaSourcePathProvider = FutureProvider.family<String?, int>((
  ref,
  mediaId,
) async {
  final session = await ref.watch(vaultSessionProvider.future);
  if (session == null) return null;
  return ref
      .read(galleryRepositoryProvider)
      .getMediaSourcePath(session.vaultPath, session.paths.indexPath, mediaId);
});
