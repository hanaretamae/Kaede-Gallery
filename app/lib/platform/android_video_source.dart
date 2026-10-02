import 'dart:io';

import 'package:video_player/video_player.dart';

VideoPlayerController androidVideoControllerForSource(String source) {
  final uri = Uri.tryParse(source);
  if (uri?.scheme == 'content') {
    return VideoPlayerController.contentUri(uri!);
  }
  return VideoPlayerController.file(File(source));
}
