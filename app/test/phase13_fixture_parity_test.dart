import 'dart:io';

import 'package:flutter_rust_bridge/flutter_rust_bridge_for_generated.dart'
    show ExternalLibrary;
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as path;
import 'package:vault_gallery/core_api/gallery_repository.dart';
import 'package:vault_gallery/core_api/rust_gallery_repository.dart';
import 'package:vault_gallery/platform/vault_platform.dart';
import 'package:vault_gallery/src/rust/frb_generated.dart';

void main() {
  test('Rust repository matches the dummy Vault fixture contract', () async {
    if (!Platform.isLinux) {
      markTestSkipped('This host test loads the Linux Rust bridge.');
    }

    final projectRoot = _findProjectRoot();
    final fixturePath = path.join(projectRoot.path, 'testdata', 'dummy-vault');
    final bridgePath = path.join(
      projectRoot.path,
      'target',
      'debug',
      'libgallery_bridge.so',
    );
    await RustLib.init(externalLibrary: ExternalLibrary.open(bridgePath));

    final temporaryRoot = await Directory.systemTemp.createTemp(
      'kaede-phase13-fixture-parity-',
    );
    addTearDown(() async {
      if (await temporaryRoot.exists()) {
        await temporaryRoot.delete(recursive: true);
      }
    });

    final privateDataPath = path.join(temporaryRoot.path, 'private-data');
    final indexPath = path.join(privateDataPath, 'gallery.sqlite');
    final cachePath = path.join(privateDataPath, 'thumbnails');
    await Directory(cachePath).create(recursive: true);

    final repository = RustGalleryRepository(
      safAccess: const AndroidSafAccess(),
    );
    await repository.prepareAppDataDirectory(privateDataPath, fixturePath);
    await File(path.join(privateDataPath, 'tag-settings.json')).writeAsString(
      '{"noteStructure":{"galleryTagPrefixes":["source/"]}}',
      flush: true,
    );
    await repository.saveVaultPath(privateDataPath, fixturePath);

    final scan = await repository.scan(fixturePath, indexPath);
    expect(scan.notesIndexed, 12);
    expect(scan.warnings, 1);

    final allNotes = await repository.queryNotes(
      fixturePath,
      indexPath,
      const [],
      excludedFilters: const [],
      virtualFilters: const [],
      searchQuery: '',
      sort: const GallerySort(),
      offset: 0,
      limit: 500,
    );
    expect(allNotes, hasLength(12));
    final note = allNotes.singleWhere(
      (candidate) => candidate.path == 'note-000000.md',
    );
    expect(note.path, 'note-000000.md');
    expect(note.title, 'Fictional note 000000');

    final filteredNotes = await repository.queryNotes(
      fixturePath,
      indexPath,
      const ['source/rating/safe'],
      excludedFilters: const [],
      virtualFilters: const [],
      searchQuery: '',
      sort: const GallerySort(),
      offset: 0,
      limit: 10,
    );
    expect(
      await repository.countNotes(
        fixturePath,
        indexPath,
        const ['source/rating/safe'],
        excludedFilters: const [],
        virtualFilters: const [],
        searchQuery: '',
      ),
      3,
    );
    expect(
      filteredNotes.map((candidate) => candidate.path),
      contains('note-000000.md'),
    );

    final detail = await repository.getNoteDetail(
      fixturePath,
      indexPath,
      note.id,
    );
    expect(detail, isNotNull);
    expect(detail!.path, 'note-000000.md');
    expect(detail.title, 'Fictional note 000000');
    expect(detail.tags.toSet(), {
      'source/service/example',
      'source/rating/safe',
      'copyright/original',
    });
    expect(detail.bodyText, 'Synthetic test content only.');

    final memo = detail.memoLines.single;
    expect(memo.text, 'synthetic memo');
    expect(memo.isBullet, isTrue);
    expect(memo.indentLevel, 0);
    expect(memo.urls, isEmpty);

    final related = detail.relatedLines.single;
    expect(related.text, 'fictional link');
    expect(related.urls, ['https://example.invalid/related']);
    expect(related.isBullet, isTrue);
    expect(related.indentLevel, 0);
    expect(related.linkedNoteId, isNull);

    expect(detail.media, hasLength(1));
    final image = detail.media.single;
    expect(image.noteId, note.id);
    expect(image.isVideo, isFalse);
    expect(image.exists, isTrue);
    final imageSourcePath = await repository.getMediaSourcePath(
      fixturePath,
      indexPath,
      image.id,
    );
    expect(imageSourcePath, isNotNull);
    expect(await File(imageSourcePath!).exists(), isTrue);
    expect(
      await File(imageSourcePath).resolveSymbolicLinks(),
      await File(path.join(fixturePath, 'media', 'pixel.png'))
          .resolveSymbolicLinks(),
    );
  });
}

Directory _findProjectRoot() {
  var directory = Directory.current.absolute;
  while (true) {
    final fixture = Directory(
      path.join(directory.path, 'testdata', 'dummy-vault'),
    );
    final bridge = File(
      path.join(directory.path, 'target', 'debug', 'libgallery_bridge.so'),
    );
    if (fixture.existsSync() && bridge.existsSync()) return directory;

    final parent = directory.parent;
    if (parent.path == directory.path) break;
    directory = parent;
  }
  throw StateError(
    'Could not locate testdata/dummy-vault and '
    'target/debug/libgallery_bridge.so from ${Directory.current.path}',
  );
}
