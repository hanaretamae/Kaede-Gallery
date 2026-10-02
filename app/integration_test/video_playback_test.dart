import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:media_kit/media_kit.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('plays a locally generated video without network access', (
    tester,
  ) async {
    MediaKit.ensureInitialized();
    final directory = await Directory.systemTemp.createTemp(
      'vault-gallery-playback-test-',
    );
    final videoPath = '${directory.path}/fictional.mp4';
    final result = await Process.run('ffmpeg', [
      '-hide_banner',
      '-loglevel',
      'error',
      '-f',
      'lavfi',
      '-i',
      'color=c=blue:s=160x90:r=10:d=2',
      '-c:v',
      'mpeg4',
      '-pix_fmt',
      'yuv420p',
      '-y',
      videoPath,
    ]);
    expect(result.exitCode, 0);

    final player = Player();
    addTearDown(() async {
      await player.dispose();
      await directory.delete(recursive: true);
    });
    await player.open(Media(videoPath));
    final duration = await player.stream.duration
        .firstWhere((duration) => duration > Duration.zero)
        .timeout(const Duration(seconds: 20));

    expect(duration, greaterThan(Duration.zero));
    await player.setRate(1.5);
    expect(player.state.rate, 1.5);
    await player.setPlaylistMode(PlaylistMode.single);
    expect(player.state.playlistMode, PlaylistMode.single);
    await player.setVolume(0);
    await player.stream.volume
        .firstWhere((volume) => volume == 0)
        .timeout(const Duration(seconds: 5));
    expect(player.state.volume, 0);
  });
}
