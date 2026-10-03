import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';
import 'package:vault_gallery/core_api/rust_gallery_repository.dart';
import 'package:vault_gallery/platform/vault_platform.dart';
import 'package:vault_gallery/platform/rust_library.dart';
import 'package:vault_gallery/src/rust/frb_generated.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('measures fictional Android SAF scan, cache, and scroll', (
    tester,
  ) async {
    await RustLib.init(externalLibrary: rustLibraryForCurrentPlatform());
    const vaultUri = 'content://fictional.invalid/tree/benchmark';
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    const noteCount = 1000;
    final mediaUri =
        'content://fictional.invalid/tree/benchmark/document/media%2Fpixel.png';
    final imageBytes = Uint8List.fromList(
      base64Decode(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGMwSJgAAAHkASHKbNIyAAAAAElFTkSuQmCC',
      ),
    );
    final noteContents = List.generate(
      noteCount,
      (index) => Uint8List.fromList(
        utf8.encode(
          '---\ntags: [source/benchmark]\ncover: media/pixel.png\n---\n'
          '# Fictional note $index\nGenerated benchmark content only.\n',
        ),
      ),
      growable: false,
    );
    var thumbnailReads = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'listFiles') {
            return [
              ...List.generate(
                noteCount,
                (index) => {
                  'path': 'note-$index.md',
                  'modifiedNanos': index,
                  'size': noteContents[index].length,
                  'documentUri':
                      'content://fictional.invalid/tree/benchmark/document/note-$index',
                },
                growable: false,
              ),
              {
                'path': 'media/pixel.png',
                'modifiedNanos': 0,
                'size': imageBytes.length,
                'documentUri': mediaUri,
              },
            ];
          }
          if (call.method == 'readListedFiles') {
            final arguments = call.arguments! as Map<Object?, Object?>;
            final documents = arguments['documents']! as List<Object?>;
            return documents
                .map((document) {
                  final notePath =
                      (document! as Map<Object?, Object?>)['path']! as String;
                  final noteIndex = int.parse(
                    notePath.split('-').last.split('.').first,
                  );
                  return {'path': notePath, 'content': noteContents[noteIndex]};
                })
                .toList(growable: false);
          }
          if (call.method == 'thumbnail') {
            thumbnailReads++;
            return imageBytes;
          }
          return null;
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    final dataRoot = await getApplicationSupportDirectory();
    final workDirectory = Directory(
      path.join(
        dataRoot.path,
        'fictional-android-saf-benchmark-${DateTime.now().microsecondsSinceEpoch}',
      ),
    );
    final dataDirectory = Directory(
      path.join(workDirectory.path, 'vault-data'),
    );
    final indexPath = path.join(dataDirectory.path, 'index.sqlite');
    final cacheDirectory = path.join(dataDirectory.path, 'thumbnails');
    final access = const AndroidSafAccess();
    final repository = RustGalleryRepository(safAccess: access);

    await repository.prepareAppDataDirectory(dataDirectory.path, vaultUri);
    await repository.saveVaultPath(dataDirectory.path, vaultUri);

    final baselineRssBytes = ProcessInfo.currentRss;
    var peakRssBytes = baselineRssBytes;
    final rssSampler = Timer.periodic(const Duration(milliseconds: 10), (_) {
      peakRssBytes = max(peakRssBytes, ProcessInfo.currentRss);
    });
    final scanWatch = Stopwatch()..start();
    try {
      final scan = await repository.scan(vaultUri, indexPath);
      scanWatch.stop();
      await repository.getThumbnail(vaultUri, indexPath, cacheDirectory, 1);
      final thumbnailCallCountBeforeHit = thumbnailReads;
      await repository.getThumbnail(vaultUri, indexPath, cacheDirectory, 1);
      rssSampler.cancel();
      peakRssBytes = max(peakRssBytes, ProcessInfo.currentRss);

      final frameTimings = <FrameTiming>[];
      SchedulerBinding.instance.addTimingsCallback(frameTimings.addAll);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: GridView.builder(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 4,
              ),
              itemCount: 2000,
              itemBuilder: (context, index) =>
                  Center(child: Text('Fictional item $index')),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.fling(find.byType(GridView), const Offset(0, -1800), 1800);
      await tester.pumpAndSettle();
      await tester.pump(const Duration(milliseconds: 100));
      SchedulerBinding.instance.removeTimingsCallback(frameTimings.addAll);
      final frameMicros =
          frameTimings.map((timing) => timing.totalSpan.inMicroseconds).toList()
            ..sort();
      final p95FrameMicros = frameMicros.isEmpty
          ? 0
          : frameMicros[((frameMicros.length - 1) * 0.95).ceil()];
      final thumbnailCacheHits = thumbnailReads == thumbnailCallCountBeforeHit
          ? 1
          : 0;

      expect(scan.notesIndexed, noteCount);
      expect(scan.warnings, 0);
      expect(thumbnailReads, 1);
      expect(thumbnailCacheHits, 1);
      expect(frameMicros, isNotEmpty);
      final decodeWatch = Stopwatch()..start();
      final codec = await ui.instantiateImageCodec(imageBytes);
      final frame = await codec.getNextFrame();
      decodeWatch.stop();
      frame.image.dispose();
      codec.dispose();
      debugPrint(
        'ANDROID_SAF_BENCHMARK '
        '{"notes":$noteCount,"scan_ms":${scanWatch.elapsedMilliseconds},'
        '"baseline_rss_bytes":$baselineRssBytes,"peak_rss_bytes":$peakRssBytes,'
        '"scan_rss_delta_bytes":${max(0, peakRssBytes - baselineRssBytes)},'
        '"thumbnail_cache_hits":$thumbnailCacheHits,"thumbnail_lookups":2,'
        '"thumbnail_cache_hit_rate":0.5,"image_decode_ms":${decodeWatch.elapsedMilliseconds},'
        '"scroll_frames":${frameMicros.length},"scroll_p95_frame_us":$p95FrameMicros}',
      );
    } finally {
      rssSampler.cancel();
      await repository.forgetVaultData(dataDirectory.path, vaultUri);
      if (await workDirectory.exists()) {
        await workDirectory.delete(recursive: true);
      }
    }
  });
}
