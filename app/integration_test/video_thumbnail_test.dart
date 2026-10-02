import 'dart:io';

import 'package:fc_native_video_thumbnail/fc_native_video_thumbnail.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('extracts a thumbnail from a local video', (tester) async {
    final directory = await Directory.systemTemp.createTemp(
      'vault-gallery-video-test-',
    );
    addTearDown(() => directory.delete(recursive: true));
    final videoPath = '${directory.path}/fictional.mp4';
    final result = await Process.run('ffmpeg', [
      '-hide_banner',
      '-loglevel',
      'error',
      '-f',
      'lavfi',
      '-i',
      'color=c=red:s=64x48:r=1',
      '-frames:v',
      '1',
      '-c:v',
      'mpeg4',
      '-y',
      videoPath,
    ]);
    expect(result.exitCode, 0);

    final thumbnail = await FcNativeVideoThumbnail().saveThumbnailToBytes(
      srcFile: videoPath,
      width: 320,
      height: 320,
      quality: 75,
    );

    expect(thumbnail, isNotNull);
    expect(thumbnail!.take(2).toList(), [0xff, 0xd8]);
  });
}
