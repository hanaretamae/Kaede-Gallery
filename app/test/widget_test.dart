import 'dart:async';
import 'dart:convert';

import 'package:dbus/dbus.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:video_player/video_player.dart';
import 'package:vault_gallery/app_theme.dart';
import 'package:vault_gallery/app.dart';
import 'package:vault_gallery/core_api/gallery_appearance.dart';
import 'package:vault_gallery/core_api/linux_system_appearance.dart';
import 'package:vault_gallery/core_api/gallery_providers.dart';
import 'package:vault_gallery/core_api/gallery_repository.dart';
import 'package:vault_gallery/core_api/gallery_tag_settings.dart';
import 'package:vault_gallery/platform/vault_platform.dart';
import 'package:vault_gallery/platform/android_video_source.dart';

/// Finds the "content" scrollable to drag during [WidgetController.scrollUntilVisible]
/// calls, explicitly excluding any [Scrollable] created internally by a
/// [TextField]'s [EditableText] (which Flutter always tags with
/// `restorationId: 'editable'`). Without this, `find.byType(Scrollable).last`
/// can flakily resolve to an unrelated, off-screen text field instead of the
/// settings list actually being scrolled.
Finder _contentScrollable() => find
    .byWidgetPredicate(
      (widget) => widget is Scrollable && widget.restorationId != 'editable',
    )
    .last;

Future<void> _scrollNoteStructureUntilVisible(
  WidgetTester tester,
  Finder target,
) async {
  final screenHeight =
      tester.view.physicalSize.height / tester.view.devicePixelRatio;
  for (var attempt = 0; attempt < 30; attempt++) {
    final isVisible =
        target.evaluate().isNotEmpty &&
        tester.getRect(target).top >= 0 &&
        tester.getRect(target).bottom <= screenHeight;
    if (isVisible) break;
    await tester.drag(
      find.byKey(const ValueKey('note-structure-settings-list')),
      const Offset(0, -250),
    );
    await tester.pumpAndSettle();
  }
}

void main() {
  test('SAF Vault names use the selected document tree folder', () {
    expect(
      vaultDisplayName(
        'content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FObsidian%2FArts',
      ),
      'Arts',
    );
    expect(vaultDisplayName('/home/user/Vault'), 'Vault');
  });

  test('Obsidian links target a SAF vault and a relative note path', () {
    final uri = obsidianOpenUri(
      'content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FObsidian%2FArts',
      'Notes/example.md',
    );

    expect(uri.scheme, 'obsidian');
    expect(uri.host, 'open');
    expect(uri.queryParameters, {'vault': 'Arts', 'file': 'Notes/example.md'});
  });

  test('Obsidian links use absolute paths for filesystem vaults', () {
    final uri = obsidianOpenUri('/home/user/Vault', 'Notes/example.md');

    expect(uri.queryParameters, {'path': '/home/user/Vault/Notes/example.md'});
  });

  test('settings export asks for a user-selected JSON destination', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    MethodCall? exportCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          exportCall = call;
          return true;
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    final saved = await const AndroidSafAccess().saveJson(
      'vault-gallery-settings.json',
      '{"version":1}\n',
    );

    expect(saved, isTrue);
    expect(exportCall?.method, 'saveJson');
    expect(exportCall?.arguments, {
      'fileName': 'vault-gallery-settings.json',
      'content': Uint8List.fromList(utf8.encode('{"version":1}\n')),
    });
  });

  test('forgetting an Android Vault asks native code to release its grant', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    MethodCall? forgetCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          forgetCall = call;
          return null;
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    const vaultUri =
        'content://com.android.externalstorage.documents/tree/primary%3AFictional';
    await const AndroidSafAccess().forgetVault(vaultUri);

    expect(forgetCall?.method, 'forgetVault');
    expect(forgetCall?.arguments, {'vaultUri': vaultUri});
  });

  test('Vault forget waits for pending thumbnail cache work', () async {
    const vaultPath = 'fictional-vault-id';
    final pending = Completer<void>();
    var drained = false;
    trackVaultCacheOperation(vaultPath, pending.future);

    final drain = preventVaultCacheOperations(vaultPath).then((_) {
      drained = true;
    });
    expect(vaultCacheOperationsAllowed(vaultPath), isFalse);
    expect(drained, isFalse);

    pending.complete();
    await drain;
    expect(drained, isTrue);
    resumeVaultCacheOperations(vaultPath);
  });

  test('Android viewer requests a display-sized native image decode', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    MethodCall? imageCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          imageCall = call;
          return Uint8List.fromList([1, 2, 3]);
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    final image = await const AndroidSafAccess().readMedia(
      'content://provider/tree/root',
      'content://provider/tree/root/document/media',
    );

    expect(image, [1, 2, 3]);
    expect(imageCall?.method, 'readMediaImage');
    expect(imageCall?.arguments, {
      'vaultUri': 'content://provider/tree/root',
      'mediaUri': 'content://provider/tree/root/document/media',
      'maxDimension': 2048,
    });
  });

  test('file manager action targets the selected file', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    MethodCall? openCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          openCall = call;
          return true;
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    await const AndroidSafAccess().openMedia(
      'content://provider/tree/root',
      'content://provider/tree/root/document/root%2Fmedia%2Fimage.webp',
      revealInFileManager: true,
    );

    expect(openCall?.method, 'openMedia');
    expect(openCall?.arguments, {
      'vaultUri': 'content://provider/tree/root',
      'mediaUri':
          'content://provider/tree/root/document/root%2Fmedia%2Fimage.webp',
      'revealInFileManager': true,
    });
  });

  test('video thumbnail requests use the video frame extractor', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    MethodCall? thumbnailCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          thumbnailCall = call;
          return Uint8List.fromList([1, 2, 3]);
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    final bytes = await const AndroidSafAccess().thumbnail(
      'content://provider/tree/root',
      'folder/clip.mp4',
      video: true,
    );

    expect(bytes, [1, 2, 3]);
    expect(thumbnailCall?.method, 'thumbnail');
    expect(thumbnailCall?.arguments, {
      'vaultUri': 'content://provider/tree/root',
      'path': 'folder/clip.mp4',
      'video': true,
      'size': 320,
    });
  });

  test('Android SAF note reads batch URIs from the tree listing', () async {
    const vaultUri = 'content://provider/tree/root';
    const documentUri = 'content://provider/tree/root/document/root%2Fnote.md';
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    final calls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          return switch (call.method) {
            'listFiles' => [
              {
                'path': 'note.md',
                'modifiedNanos': 0,
                'size': 1,
                'documentUri': documentUri,
              },
            ],
            'readListedFiles' => [
              {
                'path': 'note.md',
                'content': Uint8List.fromList([1]),
              },
            ],
            _ => null,
          };
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    const access = AndroidSafAccess();
    final files = await access.listFiles(vaultUri);
    final contents = await access.readListedFiles(vaultUri, files);

    expect(contents['note.md'], [1]);
    expect(calls.map((call) => call.method), ['listFiles', 'readListedFiles']);
    expect(calls.last.arguments, {
      'vaultUri': vaultUri,
      'documents': [
        {'path': 'note.md', 'documentUri': documentUri, 'size': 1},
      ],
    });
  });

  test('Android SAF listing measures path limits in UTF-8 bytes', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          return [
            {
              'path': '${List.filled(1366, 'あ').join()}.md',
              'modifiedNanos': 0,
              'size': 0,
              'documentUri': 'content://provider/tree/root/document/note',
            },
          ];
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    await expectLater(
      const AndroidSafAccess().listFiles('content://provider/tree/root'),
      throwsA(isA<StateError>()),
    );
  });

  test(
    'Android SAF scan refuses note content above the total byte limit',
    () async {
      const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
      var nativeReadAttempted = false;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            nativeReadAttempted = true;
            return [];
          });
      addTearDown(
        () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null),
      );

      final files = List.generate(
        65,
        (index) => SafFileEntry(
          path: 'note-$index.md',
          modifiedNanos: 0,
          size: 2 * 1024 * 1024,
          documentUri: 'content://provider/tree/root/document/$index',
        ),
      );
      await expectLater(
        const AndroidSafAccess().readListedFiles(
          'content://provider/tree/root',
          files,
        ),
        throwsA(isA<StateError>()),
      );
      expect(nativeReadAttempted, isFalse);
    },
  );

  test('Android SAF note batches stay within the file-count bound', () async {
    const channel = MethodChannel('com.hanaretamae.vault_gallery/saf');
    var batchCount = 0;
    final batchSizes = <int>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          expect(call.method, 'readListedFiles');
          final arguments = call.arguments! as Map<Object?, Object?>;
          final documents = arguments['documents']! as List<Object?>;
          batchCount++;
          batchSizes.add(documents.length);
          return documents
              .map((document) {
                final path = (document! as Map<Object?, Object?>)['path'];
                return {
                  'path': path,
                  'content': Uint8List.fromList([1]),
                };
              })
              .toList(growable: false);
        });
    addTearDown(
      () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null),
    );

    final files = List.generate(
      130,
      (index) => SafFileEntry(
        path: 'note-$index.md',
        modifiedNanos: 0,
        size: 1,
        documentUri: 'content://provider/tree/root/document/$index',
      ),
    );
    final contents = await const AndroidSafAccess().readListedFiles(
      'content://provider/tree/root',
      files,
    );

    expect(batchCount, 2);
    expect(batchSizes, [128, 2]);
    expect(contents.length, 130);
  });

  test('Android video source preserves SAF content URIs', () async {
    final previousPlatform = debugDefaultTargetPlatformOverride;
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    addTearDown(() {
      debugDefaultTargetPlatformOverride = previousPlatform;
    });

    final controller = androidVideoControllerForSource(
      'content://com.android.externalstorage.documents/tree/primary%3ADocuments',
    );
    expect(controller.dataSourceType, DataSourceType.contentUri);
    expect(controller.dataSource, startsWith('content://'));
    await controller.dispose();
  });

  test('Android video source supports validated local paths', () async {
    final previousPlatform = debugDefaultTargetPlatformOverride;
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    addTearDown(() {
      debugDefaultTargetPlatformOverride = previousPlatform;
    });

    final controller = androidVideoControllerForSource(
      '/storage/emulated/0/Documents/Syncthing/clip.mp4',
    );
    expect(controller.dataSourceType, DataSourceType.file);
    expect(controller.dataSource, startsWith('file://'));
    await controller.dispose();
  });

  test('tag rules support prefixes, hiding, color overrides and JSON', () {
    const defaults = GalleryTagSettings();
    expect(defaults.includes('source/type/human'), isTrue);
    expect(defaults.hides('source/art/anime'), isTrue);
    expect(defaults.hides('source/artist'), isFalse);
    expect(defaults.colorFor('source/gender/female'), const Color(0xFFF59EEE));

    final custom = defaults.copyWith(
      includedPrefixes: const ['source/service'],
      hiddenPrefixes: const ['source/service/private'],
      colors: const [
        TagColorRule('source/service', 0xFF112233),
        TagColorRule('source/service/pixiv', 0xFF445566),
      ],
    );
    expect(custom.includes('source/service/pixiv'), isTrue);
    expect(custom.includes('copyright/example'), isFalse);
    expect(custom.hides('source/service/private/one'), isTrue);
    expect(
      custom.colorFor('source/service/pixiv/fan'),
      const Color(0xFF445566),
    );

    final restored = GalleryTagSettings.fromJson(
      jsonDecode(jsonEncode(custom.toJson())) as Map<String, dynamic>,
    );
    expect(restored.includedPrefixes, custom.includedPrefixes);
    expect(restored.hiddenPrefixes, custom.hiddenPrefixes);
    expect(restored.colors.map((rule) => rule.color), [0xFF112233, 0xFF445566]);
    expect(restored.noteStructure.memoHeadings, ['覚書', 'メモ']);
    expect(const GalleryNoteStructureSettings().blockOrder, const [
      GalleryNoteBlock.author,
      GalleryNoteBlock.media,
      GalleryNoteBlock.postText,
      GalleryNoteBlock.postTextEnd,
      GalleryNoteBlock.related,
      GalleryNoteBlock.memo,
    ]);
    final migratedDefault = GalleryNoteStructureSettings.fromJson({
      'blockOrder': [
        'author',
        'media',
        'postText',
        'memo',
        'related',
        'postTextEnd',
      ],
    });
    expect(
      migratedDefault.blockOrder,
      GalleryNoteStructureSettings.defaultBlockOrder,
    );
    final legacyStructure = GalleryNoteStructureSettings.fromJson({
      'memoHeadingLevel': 2,
      'relatedHeadingLevel': 2,
      'memoBulletsRequired': true,
      'relatedBulletsRequired': true,
    });
    expect(legacyStructure.memoHeadings, ['覚書', 'メモ']);
    expect(legacyStructure.toJson().containsKey('memoHeadingLevel'), isFalse);
    final orderedStructure = restored.noteStructure.copyWith(
      blockOrder: const [
        GalleryNoteBlock.media,
        GalleryNoteBlock.author,
        GalleryNoteBlock.postText,
        GalleryNoteBlock.related,
        GalleryNoteBlock.memo,
      ],
    );
    final restoredOrder = GalleryNoteStructureSettings.fromJson(
      jsonDecode(jsonEncode(orderedStructure.toJson())) as Map<String, dynamic>,
    );
    expect(restoredOrder.blockOrder, const [
      GalleryNoteBlock.media,
      GalleryNoteBlock.author,
      GalleryNoteBlock.postText,
      GalleryNoteBlock.related,
      GalleryNoteBlock.memo,
      GalleryNoteBlock.postTextEnd,
    ]);

    final structure = restored.noteStructure.copyWith(
      galleryTagPrefixes: const ['collection/'],
      frontmatter: const GalleryFrontmatterSettings(
        tagsKeys: ['分類', 'tags'],
        titleKeys: ['見出し'],
      ),
      linkResolution: GalleryLinkResolution.absolutePath,
    );
    final restoredStructure = GalleryNoteStructureSettings.fromJson(
      jsonDecode(jsonEncode(structure.toJson())) as Map<String, dynamic>,
    );
    expect(restoredStructure.galleryTagPrefixes, ['collection/']);
    expect(restoredStructure.frontmatter.tagsKeys, ['分類', 'tags']);
    expect(restoredStructure.frontmatter.titleKeys, ['見出し']);
    expect(
      restoredStructure.linkResolution,
      GalleryLinkResolution.absolutePath,
    );
    final pagination = const GalleryPaginationSettings().copyWith(
      pageSize: 48,
      showItemCount: true,
    );
    final restoredPagination = GalleryPaginationSettings.fromJson(
      jsonDecode(jsonEncode(pagination.toJson())) as Map<String, dynamic>,
    );
    expect(restoredPagination.pageSize, 48);
    expect(restoredPagination.showItemCount, isTrue);
    expect(restoredPagination.showItemNumberOnTiles, isFalse);
    final itemNumberPagination = const GalleryPaginationSettings().copyWith(
      showItemNumberOnTiles: true,
    );
    expect(
      GalleryPaginationSettings.fromJson(itemNumberPagination.toJson())
          .showItemNumberOnTiles,
      isTrue,
    );
    expect(const GalleryPaginationSettings().showItemCount, isTrue);
    expect(const GalleryPaginationSettings().showItemNumberOnTiles, isFalse);
    expect(
      GalleryPaginationSettings.fromJson({'showMediaCountOnTiles': true})
          .showItemNumberOnTiles,
      isTrue,
    );
    expect(GalleryPaginationSettings.fromJson(const {}).showItemCount, isTrue);
    expect(
      () =>
          GalleryNoteStructureSettings.fromJson({'linkResolution': 'unknown'}),
      throwsFormatException,
    );
    expect(
      () => GalleryFrontmatterSettings.fromJson({'tagsKeys': <String>[]}),
      throwsFormatException,
    );
  });

  test('shared theme applies Material 3 expressive tokens', () {
    final theme = galleryTheme(const Color(0xFF8FA7D9));

    expect(theme.useMaterial3, isTrue);
    expect(
      (theme.cardTheme.shape! as RoundedRectangleBorder).borderRadius,
      BorderRadius.circular(GalleryShape.large),
    );
    expect(theme.bottomSheetTheme.showDragHandle, isTrue);
    expect(
      theme.filledButtonTheme.style!.minimumSize!.resolve(const {}),
      const Size(48, 48),
    );
    expect(
      theme.iconButtonTheme.style!.minimumSize!.resolve(const {}),
      const Size(48, 48),
    );
  });

  test(
    'Material You palette derives distinct accents from the system seed',
    () {
      final systemScheme = ColorScheme.fromSeed(
        seedColor: const Color(0xFF607DAD),
        brightness: Brightness.light,
      );
      final scheme = materialYouScheme(
        systemScheme,
        brightness: Brightness.light,
      );

      expect(scheme.primary, isNot(scheme.secondary));
      expect(scheme.secondary, isNot(scheme.tertiary));
      expect(scheme.surface, isNot(scheme.surfaceContainerHighest));
    },
  );

  test('pure black preserves accents and distinguishes item surfaces', () {
    final systemScheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF4D69A8),
      brightness: Brightness.dark,
    );
    final theme = galleryTheme(
      const Color(0xFF8FA7D9),
      brightness: Brightness.dark,
      dynamicScheme: systemScheme,
      pureBlack: true,
    );
    final scheme = theme.colorScheme;

    expect(scheme.primary, systemScheme.primary);
    expect(scheme.secondary, systemScheme.secondary);
    expect(scheme.surface, Colors.black);
    expect(scheme.surfaceDim, Colors.black);
    expect(scheme.surfaceBright, Colors.black);
    expect(scheme.surfaceContainerLowest, isNot(Colors.black));
    expect(scheme.surfaceContainerLow, isNot(Colors.black));
    expect(scheme.surfaceContainer, isNot(Colors.black));
    expect(scheme.surfaceContainerHigh, isNot(Colors.black));
    expect(scheme.surfaceContainerHighest, isNot(Colors.black));
    expect(scheme.surfaceContainerLow, isNot(scheme.surfaceContainerHighest));
    expect(theme.scaffoldBackgroundColor, Colors.black);

    final lightSystemScheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF4D69A8),
      brightness: Brightness.light,
    );
    final lightTheme = galleryTheme(
      const Color(0xFF8FA7D9),
      dynamicScheme: lightSystemScheme,
    );
    expect(lightTheme.colorScheme.primary, lightSystemScheme.primary);
  });

  test('appearance preferences are independent', () {
    const darkTheme = GalleryAppearance(brightness: GalleryBrightnessMode.dark);
    final systemColor = darkTheme.copyWith(useSystemColor: true);
    final pureBlack = systemColor.copyWith(pureBlack: true);

    expect(systemColor.brightness, GalleryBrightnessMode.dark);
    expect(systemColor.useSystemColor, isTrue);
    expect(systemColor.pureBlack, isFalse);
    expect(pureBlack.brightness, GalleryBrightnessMode.dark);
    expect(pureBlack.useSystemColor, isTrue);
    expect(pureBlack.pureBlack, isTrue);
    expect(
      GalleryAppearance.fromJson({'brightness': 'dark', 'pureBlack': true})
          .useSystemColor,
      isFalse,
    );
  });

  test('decodes XDG portal RGB accent colors and rejects invalid values', () {
    final accent = DBusVariant(
      DBusStruct([
        const DBusDouble(0.25),
        const DBusDouble(0.5),
        const DBusDouble(1),
      ]),
    );
    expect(
      decodePortalAccentColor(accent),
      const Color.fromARGB(255, 64, 128, 255),
    );
    expect(
      decodePortalAccentColor(DBusVariant(accent)),
      const Color.fromARGB(255, 64, 128, 255),
    );
    expect(
      decodePortalAccentColor(
        DBusStruct([
          const DBusDouble(-0.1),
          const DBusDouble(0.5),
          const DBusDouble(1),
        ]),
      ),
      isNull,
    );
  });

  testWidgets('asks for a vault when no location has been saved', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(_FakeRepository()),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Vault を選択'), findsOneWidget);
  });

  testWidgets('uses the system brightness for the app theme', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(_FakeRepository()),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      Theme.of(tester.element(find.byType(Scaffold).first)).brightness,
      Brightness.dark,
    );
  });

  testWidgets('count picker loads the target page and highlights it directly', (
    tester,
  ) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      totalNotes: 60,
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('60 件中 1 - 24 件'), findsOneWidget);
    await tester.tap(find.byTooltip('指定した位置へ移動'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, '0');
    await tester.tap(find.text('移動'));
    await tester.pumpAndSettle();
    expect(find.text('1 から 60 の範囲で入力してください。'), findsOneWidget);
    await tester.enterText(find.byType(TextField).last, '49');
    await tester.tap(find.text('移動'));
    for (var frame = 0; frame < 8; frame++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    expect(repository.noteQueryOffsets, contains(24));
    expect(repository.noteQueryOffsets, [0, 24]);
    final container = ProviderScope.containerOf(
      tester.element(find.byType(MaterialApp)),
      listen: false,
    );
    final notes = container.read(galleryItemsProvider).requireValue;
    expect(notes, hasLength(36));
    expect(notes.map((note) => note.id), List.generate(36, (i) => i + 25));
    expect(
      find.byKey(const ValueKey('gallery-jump-highlight-48')),
      findsOneWidget,
    );
    expect(
      tester.getSize(find.byKey(const ValueKey('gallery-jump-highlight-48'))),
      tester.getSize(find.byType(Card).first),
    );
    expect(find.text('60 件中 49 - 60 件'), findsOneWidget);
    expect(find.byType(AlertDialog), findsNothing);
    double highlightAlpha() {
      final highlight = find.byKey(const ValueKey('gallery-jump-highlight-48'));
      final decoration =
          tester
                  .widget<DecoratedBox>(
                    find.descendant(
                      of: highlight,
                      matching: find.byType(DecoratedBox),
                    ),
                  )
                  .decoration
              as BoxDecoration;
      return decoration.border!.top.color.a;
    }

    final firstBlink = highlightAlpha();
    await tester.pump(const Duration(milliseconds: 250));
    final betweenBlinks = highlightAlpha();
    await tester.pump(const Duration(milliseconds: 300));
    final secondBlink = highlightAlpha();
    expect(betweenBlinks, lessThan(firstBlink));
    expect(secondBlink, greaterThan(betweenBlinks));
    await container.read(galleryItemsProvider.notifier).loadPrevious();
    await tester.pumpAndSettle();
    expect(repository.noteQueryOffsets.last, 0);
    expect(container.read(galleryItemsProvider).requireValue, hasLength(60));
    await tester.drag(find.byType(CustomScrollView), const Offset(0, -15));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const ValueKey('gallery-jump-highlight-48')),
      findsNothing,
    );
    await tester.tap(find.byTooltip('指定した位置へ移動'));
    await tester.pumpAndSettle();
    expect(find.byType(TextField).last, findsOneWidget);
    expect(
      tester.widget<TextField>(find.byType(TextField).last).controller!.text,
      '49',
    );
    await tester.tap(find.text('キャンセル'));
    await tester.pumpAndSettle();
    await tester.drag(find.byType(CustomScrollView), const Offset(0, 4000));
    await tester.pumpAndSettle();
    expect(repository.noteQueryOffsets, contains(0));
    expect(container.read(galleryItemsProvider).requireValue, hasLength(60));
    expect(find.text('60 件中 1 - 24 件'), findsOneWidget);
  });

  testWidgets('count picker shows cancellable page-loading progress', (
    tester,
  ) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      totalNotes: 60,
    );
    final pendingPage = Completer<List<GalleryNote>>();
    repository.nextMoreNotesCompleter = pendingPage;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('指定した位置へ移動'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, '49');
    await tester.tap(find.text('移動'));
    for (
      var frame = 0;
      frame < 12 && !repository.noteQueryOffsets.contains(48);
      frame++
    ) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    for (var frame = 0; frame < 6; frame++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    expect(repository.noteQueryOffsets, contains(24));
    final container = ProviderScope.containerOf(
      tester.element(find.byType(MaterialApp)),
      listen: false,
    );
    expect(
      container.read(galleryJumpStatusProvider),
      GalleryJumpStatus.loading,
    );
    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.textContaining('ページを読み込んでいます'), findsOneWidget);
    expect(find.text('キャンセル'), findsOneWidget);
    await tester.tap(find.text('キャンセル'));
    await tester.pump(const Duration(milliseconds: 250));
    expect(container.read(galleryJumpTargetProvider), isNull);
    pendingPage.complete(const []);
    await tester.pumpAndSettle();
    expect(container.read(galleryItemsProvider).requireValue, hasLength(24));
  });

  testWidgets('failed position jump keeps its dialog open', (tester) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      totalNotes: 60,
    );
    final failedPage = Completer<List<GalleryNote>>();
    repository.nextMoreNotesCompleter = failedPage;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('指定した位置へ移動'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, '49');
    await tester.tap(find.text('移動'));
    await tester.pump(const Duration(milliseconds: 500));
    failedPage.completeError(StateError('fictional query failure'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('指定位置の読み込みに失敗しました。'), findsOneWidget);
  });

  testWidgets('tag panel changes the date sort for the gallery', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();

    expect(find.text('並び順'), findsNothing);
    expect(
      repository.lastNoteSort,
      const GallerySort(
        field: GallerySortField.created,
        direction: GallerySortDirection.descending,
      ),
    );
    await tester.tap(find.text('作成日'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('昇順'));
    await tester.pumpAndSettle();

    expect(
      repository.lastNoteSort,
      const GallerySort(
        field: GallerySortField.created,
        direction: GallerySortDirection.ascending,
      ),
    );
    expect(repository.noteQueryOffsets.last, 0);
    Navigator.of(tester.element(find.byType(SegmentedButton<GallerySortField>)))
        .pop();
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('表示方法'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('すべてのメディアを表示'));
    await tester.pumpAndSettle();
    expect(repository.lastMediaSort, repository.lastNoteSort);
  });

  testWidgets('shows indexed notes and tag filters', (tester) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    tester.view.physicalSize = const Size(1000, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('fictional-vault'), findsOneWidget);
    expect(find.text('1 件中 1 - 1 件'), findsOneWidget);
    expect(find.text('架空の note'), findsNothing);
    expect(find.byIcon(Icons.photo_library_outlined), findsOneWidget);
    expect(find.byIcon(Icons.sticky_note_2_outlined), findsOneWidget);
    expect(find.byIcon(Icons.link), findsOneWidget);
    expect(find.text('3'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(repository.scanCalls, 1);
    await tester.drag(find.byType(CustomScrollView), const Offset(0, 350));
    await tester.pumpAndSettle();
    expect(repository.scanCalls, 1);
    expect(find.byType(RefreshIndicator), findsNothing);
    final placeholder = find.byIcon(Icons.image_not_supported_outlined);
    final tile = find.ancestor(of: placeholder, matching: find.byType(Card));
    final tileSize = tester.getSize(tile.first);
    expect(tileSize.width / tileSize.height, closeTo(4 / 5, 0.02));
    expect(tester.widget<Card>(tile.first).clipBehavior, Clip.antiAlias);
    expect(find.byType(ClipRRect), findsNothing);
    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    expect(find.text('コンテンツ'), findsOneWidget);
    expect(find.text('ソース'), findsOneWidget);
    expect(
      tester.widget<MaterialApp>(find.byType(MaterialApp)).theme!.useMaterial3,
      isTrue,
    );
    await tester.tap(find.text('コンテンツ'));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('複数画像'));
    await tester.pumpAndSettle();
    expect(repository.lastVirtualFilters, contains('multiple_media'));

    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    expect(find.text('gender/female'), findsOneWidget);
    expect(find.text('test'), findsOneWidget);
    expect(find.text('sub/deep'), findsOneWidget);
    final filterChip = tester.widget<FilterChip>(
      find.widgetWithText(FilterChip, 'gender/female'),
    );
    expect(filterChip.backgroundColor, isNull);
    expect(filterChip.selectedColor, isNull);
    expect(filterChip.materialTapTargetSize, MaterialTapTargetSize.shrinkWrap);
    expect(
      tester.getSize(find.widgetWithText(FilterChip, 'gender/female')).height,
      lessThanOrEqualTo(40),
    );
    final alphaTag = tester.getTopLeft(
      find.widgetWithText(FilterChip, 'alpha'),
    );
    final zebraTag = tester.getTopLeft(
      find.widgetWithText(FilterChip, 'zebra'),
    );
    expect(alphaTag.dx, lessThan(zebraTag.dx));
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();

    expect(repository.lastFilters, contains('source/gender/female'));
    expect(repository.lastCategoryFilters, contains('source/gender/female'));
    Navigator.of(tester.element(find.text('gender/female'))).pop();
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    expect(find.text('gender/female'), findsOneWidget);
    expect(find.text('test'), findsOneWidget);
    expect(find.text('sub/deep'), findsOneWidget);
    final disabledChip = tester.widget<FilterChip>(
      find.byKey(const ValueKey('source/type/disabled:filter')),
    );
    expect(disabledChip.onSelected, isNull);
    await tester.scrollUntilVisible(
      find.text('著作権'),
      200,
      scrollable: _contentScrollable(),
    );
    await tester.tap(find.text('著作権'));
    await tester.pumpAndSettle();
    final disabledCopyrightChip = tester.widget<FilterChip>(
      find.byKey(const ValueKey('copyright/no-matching-notes:filter')),
    );
    expect(disabledCopyrightChip.onSelected, isNull);
    expect(disabledCopyrightChip.labelStyle?.color?.a, lessThan(1));
    expect(disabledCopyrightChip.backgroundColor?.a, lessThan(1));
  });

  testWidgets('filtered result count updates the gallery bar and jump limit', (
    tester,
  ) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      totalNotes: 60,
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _showCountTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('60 件中 1 - 24 件'), findsOneWidget);

    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();
    expect(find.text('1 件中 1 - 1 件'), findsOneWidget);
    Navigator.of(tester.element(find.text('gender/female'))).pop();
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('指定した位置へ移動'));
    await tester.pumpAndSettle();
    expect(find.text('1 から 1 件目まで'), findsOneWidget);
  });

  testWidgets('keeps scan warnings in settings instead of the gallery', (
    tester,
  ) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      warnings: 10,
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          _appearanceSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('確認できなかった項目: 10 件'), findsNothing);
    await tester.tap(find.byTooltip('設定'));
    await tester.pumpAndSettle();

    expect(find.text('設定'), findsOneWidget);
    expect(find.text('テーマ'), findsOneWidget);
    expect(find.text('システム'), findsOneWidget);
    expect(find.text('ライト'), findsOneWidget);
    expect(find.text('ダーク'), findsOneWidget);
    expect(find.text('システムカラー（Material You）'), findsOneWidget);
    expect(find.text('ピュアブラック'), findsOneWidget);
    expect(find.textContaining('アクセントとして併用できます'), findsOneWidget);
    expect(find.byType(SegmentedButton<GalleryBrightnessMode>), findsOneWidget);
    expect(find.byType(SwitchListTile), findsNWidgets(2));
    await tester.tap(find.text('ライト'));
    await tester.pumpAndSettle();
    expect(find.text('ピュアブラック'), findsNothing);
    await tester.tap(find.text('システム'));
    await tester.pumpAndSettle();
    expect(find.text('ピュアブラック'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('確認できなかった項目'),
      300,
      scrollable: _contentScrollable(),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('10 件。内容を確認できない'), findsOneWidget);
    expect(find.textContaining('ギャラリー対象かは判定できません'), findsOneWidget);
    await tester.tap(find.byTooltip('確認できなかった項目の詳細'));
    await tester.pumpAndSettle();

    expect(find.text('確認できなかった項目の詳細'), findsOneWidget);
    expect(find.textContaining('今回の走査では 10 件'), findsOneWidget);
    expect(find.text('確認できない理由の例'), findsOneWidget);
    expect(find.textContaining('source/ タグがないもの'), findsOneWidget);
    expect(find.textContaining('具体的な項目名やノートの内容はこの画面に表示しません。'), findsOneWidget);
    await tester.tap(find.text('閉じる'));
    await tester.pumpAndSettle();
    expect(find.text('確認できなかった項目の詳細'), findsNothing);
    await tester.scrollUntilVisible(
      find.text('このアプリについて'),
      300,
      scrollable: _contentScrollable(),
    );
    expect(find.text('このアプリについて'), findsOneWidget);
    await tester.tap(find.text('インポート・エクスポート・リセット'));
    await tester.pumpAndSettle();
    expect(find.text('設定をJSONからインポート'), findsOneWidget);
    for (final title in ['設定をJSONでエクスポート', '設定をJSONからインポート', '設定をリセット']) {
      final tile = tester.widget<ListTile>(
        find
            .ancestor(of: find.text(title), matching: find.byType(ListTile))
            .first,
      );
      expect(tile.trailing, isNull);
    }
    await tester.tap(find.text('設定をリセット'));
    await tester.pumpAndSettle();
    expect(find.text('既定値に戻す'), findsOneWidget);
    await tester.tap(find.text('既定値に戻す'));
    await tester.pumpAndSettle();
    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('ノート構造と表示'),
      250,
      scrollable: _contentScrollable(),
    );
    await tester.tap(find.text('ノート構造と表示'));
    await tester.pumpAndSettle();
    expect(find.text('ページサイズ'), findsNothing);
    expect(find.text('見出し名で役割を割り当てます'), findsNothing);
    await _scrollNoteStructureUntilVisible(tester, find.byTooltip('投稿者の説明'));
    expect(find.byTooltip('投稿者の設定'), findsNothing);
    expect(find.byTooltip('メディアの設定'), findsNothing);
    await tester.tap(find.byTooltip('投稿者の説明'));
    await tester.pumpAndSettle();
    expect(find.textContaining('@ がないリンクにも対応します。'), findsOneWidget);
    await tester.tap(find.text('閉じる'));
    await tester.pumpAndSettle();
    await _scrollNoteStructureUntilVisible(
      tester,
      find.byTooltip('Frontmatter の設定'),
    );
    expect(find.byTooltip('Frontmatter の設定'), findsOneWidget);
    await tester.tap(find.byTooltip('Frontmatter の設定'));
    await tester.pumpAndSettle();
    expect(find.text('Frontmatter の設定'), findsOneWidget);
    expect(find.textContaining('Frontmatter は詳細欄の先頭に固定されます'), findsNothing);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await _scrollNoteStructureUntilVisible(tester, find.byTooltip('覚書の設定'));
    expect(find.byTooltip('覚書の設定'), findsOneWidget);
    final memoTile = find.ancestor(
      of: find.byTooltip('覚書の設定'),
      matching: find.byType(ListTile),
    );
    expect(
      tester
          .getCenter(
            find.descendant(of: memoTile, matching: find.byType(Switch)),
          )
          .dx,
      greaterThan(tester.getCenter(find.byTooltip('覚書の設定')).dx),
    );
    await tester.tap(find.byTooltip('覚書の設定'));
    await tester.pumpAndSettle();
    expect(find.text('認識条件'), findsNothing);
    expect(find.text('箇条書きのみを対象にする'), findsNothing);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await _scrollNoteStructureUntilVisible(tester, find.byTooltip('関連の設定'));
    expect(find.byTooltip('関連の設定'), findsOneWidget);
    await tester.tap(find.byTooltip('関連の設定'));
    await tester.pumpAndSettle();
    expect(find.text('認識条件'), findsNothing);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await _scrollNoteStructureUntilVisible(tester, find.text('投稿文の終端'));
    expect(find.byTooltip('投稿文の終端の設定'), findsOneWidget);
    await tester.tap(find.byTooltip('投稿文の終端の設定'));
    await tester.pumpAndSettle();
    expect(find.textContaining('この見出しに到達したところで投稿文の抽出を終了します。'), findsOneWidget);
    expect(find.text('文書'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await _scrollNoteStructureUntilVisible(
      tester,
      find.text('Markdown / Wikilink のノート解決'),
    );
    expect(find.text('最短'), findsOneWidget);
    expect(find.text('相対'), findsOneWidget);
    expect(find.text('絶対'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.text('タグ設定'));
    await tester.pumpAndSettle();
    expect(find.text('ギャラリー対象タグ'), findsOneWidget);
    expect(find.text('#source/'), findsOneWidget);
    expect(find.text('フィルターに含めるタグ'), findsOneWidget);
    expect(find.text('非表示にするタグ'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('タグの色'),
      250,
      scrollable: _contentScrollable(),
    );
    expect(find.text('タグの色'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.text('ノート構造と表示'));
    await tester.pumpAndSettle();
    tester.view.physicalSize = const Size(1000, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await _scrollNoteStructureUntilVisible(tester, find.text('架空のノート例を見る・コピー'));
    String? copiedText;
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (call) async {
        if (call.method == 'Clipboard.setData') {
          copiedText =
              (call.arguments as Map<Object?, Object?>)['text'] as String?;
        }
        return null;
      },
    );
    addTearDown(
      () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        null,
      ),
    );
    await tester.tap(find.text('架空のノート例を見る・コピー'));
    await tester.pumpAndSettle();
    expect(find.text('架空のノート例'), findsOneWidget);
    expect(find.text('下の架空Markdownの本文順'), findsOneWidget);
    expect(find.byTooltip('ノート例をコピー'), findsNothing);
    await tester.scrollUntilVisible(
      find.text('Markdownをコピー'),
      200,
      scrollable: _contentScrollable(),
    );
    await tester.tap(find.text('Markdownをコピー'));
    await tester.pumpAndSettle();
    expect(copiedText, contains('fictional-rainy-window.webp'));
    expect(copiedText, contains('source/example'));
    expect(copiedText, contains('[架空の投稿者]'));
    expect(
      copiedText!.indexOf('https://example.invalid/posts/aoikasumi-0001'),
      lessThan(copiedText!.indexOf('# 文書')),
    );
    expect(copiedText!.indexOf('# 文書'), lessThan(copiedText!.indexOf('## 関連')));
    expect(
      copiedText!.indexOf('## 関連'),
      lessThan(copiedText!.indexOf('## 覚書')),
    );
  });

  testWidgets('list pagination has a separate top-level settings page', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('設定'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('ページングと一覧表示'),
      250,
      scrollable: _contentScrollable(),
    );
    await tester.tap(find.text('ページングと一覧表示'));
    await tester.pumpAndSettle();
    expect(find.text('ページングと一覧表示'), findsOneWidget);
    expect(find.text('ページサイズ'), findsOneWidget);
    expect(find.text('タイルに一覧内の位置（何件目）を表示'), findsOneWidget);
  });

  testWidgets('about page shows app name, licenses, and repository link', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(
            _FakeRepository(savedPath: '/fictional-vault'),
          ),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('設定'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('情報とライセンス'),
      250,
      scrollable: _contentScrollable(),
    );
    await tester.ensureVisible(find.text('情報とライセンス'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('情報とライセンス'));
    await tester.pumpAndSettle();
    expect(find.text('Kaede Gallery'), findsOneWidget);
    expect(find.text('GitHub リポジトリ'), findsOneWidget);
    expect(find.text('オープンソースライセンス'), findsOneWidget);
    await tester.tap(find.text('アプリ情報'));
    await tester.pumpAndSettle();
    expect(find.text('Kaede Gallery'), findsNWidgets(2));
  });

  testWidgets('gallery item numbers are opt-in', (tester) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _itemNumberTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byKey(const ValueKey('gallery-item-number-1')), findsOneWidget);
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('gallery-item-number-1')),
        matching: find.text('1'),
      ),
      findsOneWidget,
    );
  });

  testWidgets('keeps old results visible while filter results reload', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    final pendingNotes = Completer<List<GalleryNote>>();
    repository.nextNotesCompleter = pendingNotes;

    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('gender/female'));
    await tester.pump();

    expect(find.byIcon(Icons.image_not_supported_outlined), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsNothing);
    pendingNotes.complete(const []);
    await tester.pumpAndSettle();
    expect(find.text('該当するノートはありません。'), findsOneWidget);
  });

  testWidgets('opens note details and extracts memo, links and metadata', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();

    expect(
      tester.widget<Scaffold>(find.byType(Scaffold).last).backgroundColor,
      Colors.black,
    );
    final viewerPage = find.byType(PageView).last;
    final topOpacityKey = const ValueKey('viewer-top-overlay-opacity');
    final detailsOpacityKey = const ValueKey('viewer-details-opacity');
    expect(
      tester.widget<FadeTransition>(find.byKey(topOpacityKey)).opacity.value,
      0,
    );
    expect(
      tester
          .widget<FadeTransition>(find.byKey(detailsOpacityKey))
          .opacity
          .value,
      0,
    );
    expect(tester.getTopLeft(viewerPage).dy, 0);
    expect(
      tester.getBottomRight(viewerPage).dy,
      closeTo(tester.getSize(viewerPage).height, 0.1),
    );
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    expect(
      tester.widget<FadeTransition>(find.byKey(topOpacityKey)).opacity.value,
      1,
    );
    expect(
      tester
          .widget<FadeTransition>(find.byKey(detailsOpacityKey))
          .opacity
          .value,
      1,
    );
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    expect(
      tester.widget<FadeTransition>(find.byKey(topOpacityKey)).opacity.value,
      0,
    );
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    expect(
      tester.widget<FadeTransition>(find.byKey(topOpacityKey)).opacity.value,
      1,
    );
    expect(
      tester
          .widget<FadeTransition>(find.byKey(detailsOpacityKey))
          .opacity
          .value,
      1,
    );
    expect(
      tester.getTopLeft(find.text('@noone_oO')).dy,
      greaterThanOrEqualTo(tester.getTopLeft(viewerPage).dy),
    );
    expect(tester.getTopLeft(find.text('@noone_oO')).dy, lessThan(60));
    expect(
      tester.getTopLeft(find.text('ONEちゃん')).dy,
      greaterThan(tester.getTopLeft(viewerPage).dy),
    );
    final appTheme = tester
        .widget<MaterialApp>(find.byType(MaterialApp))
        .theme!;
    final headerSurface = find
        .ancestor(
          of: find.text('経験的証拠。').first,
          matching: find.byType(Material),
        )
        .first;
    expect(
      tester.widget<Material>(headerSurface).color,
      appTheme.colorScheme.surface,
    );
    expect(find.text('経験的証拠。'), findsWidgets);
    expect(find.text('Fir3born🤔🔞🇮🇹-on-X-経験的証拠。.md'), findsOneWidget);
    expect(find.text('覚書の内容'), findsOneWidget);
    expect(find.text('関連ノート'), findsOneWidget);
    expect(find.text('Obsidian でノートを開く'), findsNothing);
    expect(find.byTooltip('Obsidian でノートを開く'), findsOneWidget);
    expect(find.byTooltip('ページ URL をコピー'), findsOneWidget);
    expect(find.byTooltip('全画面表示'), findsOneWidget);
    expect(
      tester.getTopLeft(find.byTooltip('Obsidian でノートを開く')).dx,
      greaterThan(tester.getTopLeft(find.byTooltip('ページ URL をコピー')).dx),
    );
    expect(find.text('経験的証拠。'), findsOneWidget);
    expect(find.text('投稿文'), findsNothing);
    expect(find.text('ONEちゃん'), findsOneWidget);
    final postCard = find.ancestor(
      of: find.text('ONEちゃん'),
      matching: find.byType(Card),
    );
    expect(
      tester.widget<Card>(postCard).color,
      appTheme.colorScheme.surfaceContainerHighest,
    );
    expect(
      find.descendant(of: postCard, matching: find.text('@noone_oO')),
      findsNothing,
    );
    expect(find.text('2026-08-13 18:52:08'), findsOneWidget);
    expect(find.text('@noone_oO'), findsOneWidget);
    expect(find.byTooltip('投稿者のプロフィールを開く'), findsOneWidget);
    final memoCard = find.ancestor(
      of: find.text('覚書の内容'),
      matching: find.byType(Card),
    );
    final nestedMemoCard = find.ancestor(
      of: find.text('入れ子の覚書'),
      matching: find.byType(Card),
    );
    expect(
      tester.getTopLeft(nestedMemoCard).dx,
      greaterThan(tester.getTopLeft(memoCard).dx),
    );
    expect(
      find.descendant(of: memoCard, matching: find.byIcon(Icons.circle)),
      findsNothing,
    );
    final relatedCard = find.ancestor(
      of: find.text('関連ノート'),
      matching: find.byType(Card),
    );
    expect(
      find.descendant(of: relatedCard, matching: find.byIcon(Icons.circle)),
      findsNothing,
    );
    expect(
      tester.widget<FadeTransition>(find.byKey(topOpacityKey)).opacity.value,
      1,
    );
    final firstColor =
        tester
                .widget<DecoratedBox>(
                  find
                      .ancestor(
                        of: find.text('source/art'),
                        matching: find.byType(DecoratedBox),
                      )
                      .first,
                )
                .decoration
            as BoxDecoration;
    expect(firstColor.color, const Color(0xFF4DD0E1).withValues(alpha: 0.22));
    expect(firstColor.border, isNotNull);
    final secondColor =
        tester
                .widget<DecoratedBox>(
                  find
                      .ancestor(
                        of: find.text('copyright/pretty-series'),
                        matching: find.byType(DecoratedBox),
                      )
                      .first,
                )
                .decoration
            as BoxDecoration;
    expect(secondColor.color, const Color(0xFFFFF950).withValues(alpha: 0.22));
    Color? viewerTagColor(String tag) {
      final decoration =
          tester
                  .widget<DecoratedBox>(
                    find
                        .ancestor(
                          of: find.text(tag),
                          matching: find.byType(DecoratedBox),
                        )
                        .first,
                  )
                  .decoration
              as BoxDecoration;
      return decoration.color;
    }

    const expectedTagColors = <String, Color>{
      'source/gender/female': Color(0xFFF59EEE),
      'source/gender/male': Color(0xFF5D5BEC),
      'source/gender/unknown': Color(0xFFB17AF5),
      'source/gender/futanari': Color(0xFFEE8B95),
      'source/type/animal': Color(0xFFFFAA7F),
      'source/count/solo': Color(0xFFFFFFFF),
      'source/service/x': Color(0xFFFFFFFF),
      'source/service/pixiv': Color(0xFF3F90E9),
      'source/service/pixiv-fanbox': Color(0xFF3F90E9),
      'source/service/misskey/social': Color(0xFFADEA02),
      'source/service/mastodon/home': Color(0xFF3459FB),
      'source/rating/safe': Color(0xFFFFFFFF),
      'source/format/manga': Color(0xFF91EEE8),
      'source/meta/ai': Color(0xFF641FFE),
    };
    for (final entry in expectedTagColors.entries) {
      expect(
        viewerTagColor(entry.key),
        entry.value.withValues(alpha: 0.22),
        reason: entry.key,
      );
    }
    expect(
      tester.getTopLeft(find.byTooltip('Obsidian でノートを開く')).dx,
      greaterThan(tester.getTopLeft(find.byTooltip('ページ URL をコピー')).dx),
    );
    final relatedLink = find.byKey(const ValueKey('related-note-2'));
    expect(relatedLink, findsOneWidget);
    final relatedButton = tester.widget<TextButton>(relatedLink);
    expect(relatedButton.onPressed, isNotNull);
    expect(
      relatedButton.style!.minimumSize!.resolve(const {})!.height,
      greaterThanOrEqualTo(48),
    );
    expect(tester.getSize(relatedLink).width, greaterThan(250));
    final relatedLabel = tester.widget<Text>(
      find.descendant(of: relatedLink, matching: find.text('関連ノート')),
    );
    expect(relatedLabel.style?.decoration, TextDecoration.underline);
    await tester.ensureVisible(relatedLink);
    await tester.pumpAndSettle();
    await tester.tap(relatedLink);
    await tester.pumpAndSettle();
    expect(repository.lastDetailNoteId, 2);
    const linkedHeaderOpacityKey = ValueKey('viewer-top-overlay-opacity');
    expect(
      tester
          .widget<FadeTransition>(find.byKey(linkedHeaderOpacityKey))
          .opacity
          .value,
      1,
    );
    expect(find.text('linked-target.md'), findsOneWidget);
    await tester.dragFrom(const Offset(400, 520), const Offset(-240, 0));
    await tester.pumpAndSettle();
    expect(find.text('2 / 2'), findsOneWidget);
    expect(
      tester
          .widget<FadeTransition>(find.byKey(linkedHeaderOpacityKey))
          .opacity
          .value,
      1,
    );
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FadeTransition>(find.byKey(linkedHeaderOpacityKey))
          .opacity
          .value,
      0,
    );
    await tester.drag(find.byType(PageView).last, const Offset(240, 0));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FadeTransition>(find.byKey(linkedHeaderOpacityKey))
          .opacity
          .value,
      0,
    );
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FadeTransition>(find.byKey(linkedHeaderOpacityKey))
          .opacity
          .value,
      1,
    );
    expect(find.text('1 / 2'), findsOneWidget);
  });

  testWidgets('applies the configured order to viewer detail blocks', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _reorderedTagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(
            _FakeRepository(savedPath: '/fictional-vault'),
          ),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();

    expect(find.text('画像・動画'), findsNothing);
    expect(find.text('投稿者'), findsOneWidget);
    expect(find.text('@noone_oO'), findsOneWidget);
    expect(find.text('ONEちゃん'), findsOneWidget);
    expect(
      tester.getTopLeft(find.text('ONEちゃん')).dy,
      lessThan(tester.getTopLeft(find.text('@noone_oO')).dy),
    );
  });

  testWidgets('tag and note searches are independent and tags support AND', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    final noteSearchField = find.byWidgetPredicate(
      (widget) =>
          widget is TextField &&
          widget.decoration?.hintText?.startsWith('ノート名 /') == true,
    );
    final tagSearchField = find.byWidgetPredicate(
      (widget) => widget is TextField && widget.decoration?.hintText == 'タグを検索',
    );
    expect(
      tester.getTopLeft(noteSearchField).dy,
      lessThan(tester.getTopLeft(tagSearchField).dy),
    );
    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    await tester.enterText(tagSearchField, 'femal');
    await tester.pumpAndSettle();
    expect(find.text('gender/female'), findsOneWidget);
    expect(repository.lastSearchQuery, isEmpty);
    await tester.enterText(tagSearchField, '');
    await tester.pumpAndSettle();
    expect(find.text('gender/female'), findsOneWidget);
    await tester.enterText(noteSearchField, 'fictinal');
    await tester.tap(find.byTooltip('ノートを検索'));
    await tester.pumpAndSettle();

    expect(repository.lastSearchQuery, 'fictinal');
    const searchWithTags =
        'fictinal #source/gender/female -#copyright/onepeace &#source/type/animal';
    await tester.enterText(noteSearchField, searchWithTags);
    await tester.tap(find.byTooltip('ノートを検索'));
    await tester.pumpAndSettle();
    expect(repository.lastSearchQuery, searchWithTags);
    expect(find.text('gender/female'), findsOneWidget);
    expect(repository.lastFilters, isEmpty);
    expect(repository.lastExcludedFilters, isEmpty);
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();
    expect(repository.lastFilters, contains('source/gender/female'));
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();
    expect(
      repository.lastFilters,
      contains('${galleryAllFilterPrefix}source/gender/female'),
    );
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();
    expect(repository.lastExcludedFilters, contains('source/gender/female'));
    await tester.tap(find.text('gender/female'));
    await tester.pumpAndSettle();
    expect(repository.lastFilters, isEmpty);
    expect(repository.lastExcludedFilters, isEmpty);

    await tester.tap(find.byTooltip('検索と絞り込みをすべて解除'));
    await tester.pumpAndSettle();
    expect(repository.lastFilters, isEmpty);
    expect(repository.lastExcludedFilters, isEmpty);
    expect(repository.lastSearchQuery, isEmpty);
  });

  testWidgets('swipes between a note media in all-media display mode', (
    tester,
  ) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<PopupMenuButton<GalleryDisplayMode>>(
            find.byType(PopupMenuButton<GalleryDisplayMode>),
          )
          .style,
      isNull,
    );
    await tester.tap(find.byTooltip('表示方法'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('すべてのメディアを表示'));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.sticky_note_2_outlined), findsNWidgets(2));
    expect(find.byIcon(Icons.link), findsNWidgets(2));
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();

    final pageView = find.byType(PageView).last;
    expect(pageView, findsOneWidget);
    expect(find.text('1 / 2'), findsOneWidget);
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    await tester.sendKeyEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    expect(find.text('2 / 2'), findsOneWidget);
    await tester.sendKeyEvent(LogicalKeyboardKey.arrowLeft);
    await tester.pumpAndSettle();
    expect(find.text('1 / 2'), findsOneWidget);
    await tester.dragFrom(const Offset(400, 520), const Offset(-240, 0));
    await tester.pumpAndSettle();
    expect(find.text('2 / 2'), findsOneWidget);
  });

  testWidgets('horizontal trackpad scrolling changes viewer media', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(
            _FakeRepository(savedPath: '/fictional-vault'),
          ),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();

    await tester.sendEventToBinding(
      const PointerScrollEvent(
        position: Offset(400, 200),
        scrollDelta: Offset(30, 0),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('2 / 2'), findsOneWidget);
  });

  testWidgets('omits post text section when a note has no post text', (
    tester,
  ) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      postText: '> ',
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();

    expect(find.text('投稿文'), findsNothing);
    expect(find.text('ONEちゃん'), findsNothing);
  });

  testWidgets('offers app selection and file manager actions', (tester) async {
    final repository = _FakeRepository(
      savedPath: '/fictional-vault',
      mediaSourcePath: '/fictional-vault/media/image.webp',
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();
    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('メディアを開く'));
    await tester.pumpAndSettle();

    expect(find.text('画像・動画を開くアプリを選択'), findsOneWidget);
    expect(find.text('ファイルマネージャーでファイルを表示'), findsOneWidget);
    expect(find.textContaining('Android'), findsNothing);
  });

  testWidgets('resets image zoom when details are shown', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(
            _FakeRepository(
              savedPath: '/fictional-vault',
              mediaSourcePath: '/fictional-vault/missing-image.png',
            ),
          ),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(Card).first);
    await tester.pumpAndSettle();

    var viewerImage = tester.widget<InteractiveViewer>(
      find.byType(InteractiveViewer),
    );
    final image = tester.widget<Image>(find.byType(Image).last);
    expect(image.fit, BoxFit.contain);
    expect(image.gaplessPlayback, isTrue);
    viewerImage.transformationController!.value = Matrix4.diagonal3Values(
      2,
      2,
      1,
    );
    await tester.pump();
    expect(viewerImage.transformationController!.value.getMaxScaleOnAxis(), 2);

    await tester.tapAt(const Offset(400, 200));
    await tester.pumpAndSettle();
    viewerImage = tester.widget<InteractiveViewer>(
      find.byType(InteractiveViewer),
    );
    expect(viewerImage.transformationController!.value.getMaxScaleOnAxis(), 1);
  });

  testWidgets('cycles tag through any, all, exclude and none', (tester) async {
    final repository = _FakeRepository(savedPath: '/fictional-vault');
    tester.view.physicalSize = const Size(1000, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          _tagSettingsOverride,
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('タグで絞り込む'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ソース'));
    await tester.pumpAndSettle();
    final option = find.widgetWithText(FilterChip, 'gender/female');
    await tester.tap(option);
    await tester.pumpAndSettle();
    expect(repository.lastFilters, contains('source/gender/female'));
    expect(tester.widget<FilterChip>(option).selected, isTrue);
    expect(repository.lastExcludedFilters, isEmpty);
    expect(find.text('test'), findsOneWidget);
    await tester.tap(option);
    await tester.pumpAndSettle();
    expect(
      repository.lastFilters,
      contains('${galleryAllFilterPrefix}source/gender/female'),
    );
    expect(repository.lastExcludedFilters, isEmpty);
    expect(tester.widget<FilterChip>(option).selected, isTrue);
    await tester.tap(option);
    await tester.pumpAndSettle();
    expect(repository.lastFilters, isEmpty);
    expect(repository.lastExcludedFilters, contains('source/gender/female'));
    expect(tester.widget<FilterChip>(option).selected, isTrue);
    await tester.tap(option);
    await tester.pumpAndSettle();
    expect(repository.lastFilters, isEmpty);
    expect(repository.lastExcludedFilters, isEmpty);
    expect(tester.widget<FilterChip>(option).selected, isFalse);
  });
}

final _tagSettingsOverride = galleryTagSettingsProvider.overrideWith(
  _FakeGalleryTagSettingsController.new,
);

final _appearanceSettingsOverride = galleryAppearanceProvider.overrideWith(
  _FakeGalleryAppearanceController.new,
);

final _showCountTagSettingsOverride = galleryTagSettingsProvider.overrideWith(
  _ShowCountGalleryTagSettingsController.new,
);

final _itemNumberTagSettingsOverride = galleryTagSettingsProvider.overrideWith(
  _ItemNumberGalleryTagSettingsController.new,
);

final _reorderedTagSettingsOverride = galleryTagSettingsProvider.overrideWith(
  _ReorderedGalleryTagSettingsController.new,
);

class _FakeGalleryTagSettingsController extends GalleryTagSettingsController {
  @override
  Future<GalleryTagSettings> build() async =>
      const GalleryTagSettings(hiddenPrefixes: []);

  @override
  Future<void> saveSettings(GalleryTagSettings settings) async {
    state = AsyncData(settings);
  }
}

class _ShowCountGalleryTagSettingsController
    extends _FakeGalleryTagSettingsController {
  @override
  Future<GalleryTagSettings> build() async => const GalleryTagSettings(
    hiddenPrefixes: [],
    pagination: GalleryPaginationSettings(showItemCount: true),
  );
}

class _ItemNumberGalleryTagSettingsController
    extends _FakeGalleryTagSettingsController {
  @override
  Future<GalleryTagSettings> build() async => const GalleryTagSettings(
    hiddenPrefixes: [],
    pagination: GalleryPaginationSettings(showItemNumberOnTiles: true),
  );
}

class _FakeGalleryAppearanceController extends GalleryAppearanceController {
  @override
  Future<GalleryAppearance> build() async => const GalleryAppearance();

  @override
  Future<void> updateAppearance(GalleryAppearance appearance) async {
    state = AsyncData(appearance);
  }
}

class _ReorderedGalleryTagSettingsController
    extends GalleryTagSettingsController {
  @override
  Future<GalleryTagSettings> build() async => const GalleryTagSettings(
    hiddenPrefixes: [],
    noteStructure: GalleryNoteStructureSettings(
      blockOrder: [
        GalleryNoteBlock.postText,
        GalleryNoteBlock.media,
        GalleryNoteBlock.author,
        GalleryNoteBlock.related,
        GalleryNoteBlock.memo,
        GalleryNoteBlock.postTextEnd,
      ],
    ),
  );
}

class _FakeVaultPlatform implements VaultPlatform {
  @override
  SafVaultAccess get safAccess => const AndroidSafAccess();

  @override
  Future<String?> chooseVault() async => null;

  @override
  Future<GalleryPaths> galleryPaths() async => const GalleryPaths(
    dataDirectory: '/private/app-data',
    indexPath: '/private/app-data/index.sqlite',
    thumbnailDirectory: '/private/app-data/thumbnails',
  );
}

class _FakeRepository implements GalleryRepository {
  _FakeRepository({
    this.savedPath,
    this.warnings = 0,
    this.totalNotes = 1,
    this.postText = 'ONEちゃん',
    this.mediaSourcePath,
  });

  final String? savedPath;
  final int warnings;
  final int totalNotes;
  final String postText;
  final String? mediaSourcePath;
  List<String> lastFilters = const [];
  List<String> lastExcludedFilters = const [];
  List<String> lastVirtualFilters = const [];
  List<String> lastCategoryFilters = const [];
  List<String> lastCategoryExcludedFilters = const [];
  List<String> lastCategoryVirtualFilters = const [];
  String lastSearchQuery = '';
  GallerySort? lastNoteSort;
  GallerySort? lastMediaSort;
  int? lastDetailNoteId;
  int? lastNoteOffset;
  final List<int> noteQueryOffsets = [];
  int scanCalls = 0;
  Completer<List<GalleryNote>>? nextNotesCompleter;
  Completer<List<GalleryNote>>? nextMoreNotesCompleter;

  @override
  Future<void> prepareAppDataDirectory(
    String directoryPath,
    String vaultPath,
  ) async {}

  @override
  Future<String?> loadVaultPath(String directoryPath) async => savedPath;

  @override
  Future<String> saveVaultPath(String directoryPath, String vaultPath) async =>
      vaultPath;

  @override
  Future<void> forgetVaultData(String directoryPath, String vaultPath) async {}

  @override
  Future<GalleryScanReport> scan(String vaultPath, String indexPath) async {
    scanCalls++;
    return GalleryScanReport(notesIndexed: totalNotes, warnings: warnings);
  }

  @override
  Future<List<GalleryCategory>> listCategories(
    String vaultPath,
    String indexPath,
    List<String> filters,
    List<String> excludedFilters,
    List<String> virtualFilters,
  ) async {
    lastCategoryFilters = filters;
    lastCategoryExcludedFilters = excludedFilters;
    lastCategoryVirtualFilters = virtualFilters;
    return const [
      GalleryCategory(
        path: '@content',
        displayName: 'コンテンツ',
        count: 1,
        options: [
          GalleryCategoryOption(
            name: 'gender',
            fullTag: 'source/gender',
            count: 1,
            disabled: false,
          ),
          GalleryCategoryOption(
            name: '複数画像',
            fullTag: '@virtual:multiple_media',
            count: 1,
            disabled: false,
            virtualFilter: 'multiple_media',
          ),
        ],
      ),
      GalleryCategory(
        path: 'source',
        displayName: 'ソース',
        count: 1,
        options: [
          GalleryCategoryOption(
            name: 'gender/female',
            fullTag: 'source/gender/female',
            count: 1,
            disabled: false,
          ),
        ],
      ),
      GalleryCategory(
        path: 'source/type',
        displayName: 'タイプ',
        count: 1,
        options: [
          GalleryCategoryOption(
            name: 'zebra',
            fullTag: 'source/type/zebra',
            count: 1,
            disabled: false,
          ),
          GalleryCategoryOption(
            name: 'alpha',
            fullTag: 'source/type/alpha',
            count: 1,
            disabled: false,
          ),
          GalleryCategoryOption(
            name: 'no matching notes',
            fullTag: 'source/type/disabled',
            count: 0,
            disabled: true,
          ),
        ],
      ),
      GalleryCategory(
        path: 'source/test',
        displayName: 'test',
        count: 1,
        options: [
          GalleryCategoryOption(
            name: 'test',
            fullTag: 'source/test',
            count: 1,
            disabled: false,
          ),
        ],
      ),
      GalleryCategory(
        path: 'source/test/sub',
        displayName: 'sub',
        count: 1,
        options: [
          GalleryCategoryOption(
            name: 'sub',
            fullTag: 'source/test/sub',
            count: 1,
            disabled: false,
          ),
          GalleryCategoryOption(
            name: 'deep',
            fullTag: 'source/test/sub/deep',
            count: 1,
            disabled: false,
          ),
        ],
      ),
      GalleryCategory(
        path: 'copyright',
        displayName: '著作権',
        count: 0,
        options: [
          GalleryCategoryOption(
            name: 'no matching notes',
            fullTag: 'copyright/no-matching-notes',
            count: 0,
            disabled: true,
          ),
        ],
      ),
    ];
  }

  @override
  Future<List<GalleryNote>> queryNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
    required GallerySort sort,
    required int offset,
    required int limit,
  }) async {
    lastFilters = filters;
    lastExcludedFilters = excludedFilters;
    lastVirtualFilters = virtualFilters;
    lastSearchQuery = searchQuery;
    lastNoteSort = sort;
    lastNoteOffset = offset;
    noteQueryOffsets.add(offset);
    final pending = nextNotesCompleter;
    if (offset == 0 && pending != null) {
      nextNotesCompleter = null;
      return pending.future;
    }
    final pendingMore = nextMoreNotesCompleter;
    if (offset > 0 && pendingMore != null) {
      nextMoreNotesCompleter = null;
      return pendingMore.future;
    }
    final count = (totalNotes - offset).clamp(0, limit);
    return List.generate(count, (index) {
      final id = offset + index + 1;
      return GalleryNote(
        id: id,
        path: id == 1 ? 'fictional.md' : 'fictional-$id.md',
        title: id == 1 ? '架空の note' : '架空 note $id',
        mediaCount: id == 1 ? 3 : 1,
        videoCount: 0,
        memoCount: id == 1 ? 2 : 0,
        relatedCount: id == 1 ? 1 : 0,
        representativeMediaId: id,
      );
    });
  }

  @override
  Future<int> countNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  }) async {
    if (filters.isEmpty &&
        excludedFilters.isEmpty &&
        virtualFilters.isEmpty &&
        searchQuery.isEmpty) {
      return totalNotes;
    }
    return totalNotes == 0 ? 0 : 1;
  }

  @override
  Future<List<GalleryMediaItem>> queryMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
    required GallerySort sort,
    required int offset,
    required int limit,
  }) async {
    lastMediaSort = sort;
    return const [
      GalleryMediaItem(
        id: 1,
        noteId: 1,
        isVideo: false,
        exists: true,
        mediaCount: 3,
        memoCount: 2,
        relatedCount: 6,
      ),
      GalleryMediaItem(
        id: 2,
        noteId: 1,
        isVideo: false,
        exists: true,
        mediaCount: 3,
        memoCount: 2,
        relatedCount: 6,
      ),
    ];
  }

  @override
  Future<int> countMedia(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> excludedFilters,
    required List<String> virtualFilters,
    required String searchQuery,
  }) async {
    if (filters.isEmpty &&
        excludedFilters.isEmpty &&
        virtualFilters.isEmpty &&
        searchQuery.isEmpty) {
      return 2;
    }
    return 1;
  }

  @override
  Future<Uint8List?> getThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  ) async => null;

  @override
  Future<String?> getVideoSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) async => null;

  @override
  Future<String?> getMediaSourcePath(
    String vaultPath,
    String indexPath,
    int mediaId,
  ) async => mediaSourcePath;

  @override
  Future<GalleryNoteDetail?> getNoteDetail(
    String vaultPath,
    String indexPath,
    int noteId,
  ) async {
    lastDetailNoteId = noteId;
    return GalleryNoteDetail(
      id: noteId,
      path: noteId == 2
          ? 'linked-target.md'
          : 'Fir3born🤔🔞🇮🇹-on-X-経験的証拠。.md',
      title: noteId == 2 ? 'Linked Target Note' : '水滴',
      author: '@noone_oO',
      authorUrl: 'https://x.com/noone_oO',
      url: 'https://x.com/mizutenka/status/2087839612429606915',
      published: '2026-08-13T18:52:08',
      created: null,
      updated: null,
      tags: const [
        'source/art',
        'copyright/pretty-series',
        'source/gender/female',
        'source/gender/male',
        'source/gender/unknown',
        'source/gender/futanari',
        'source/type/animal',
        'source/count/solo',
        'source/service/x',
        'source/service/pixiv',
        'source/service/pixiv-fanbox',
        'source/service/misskey/social',
        'source/service/mastodon/home',
        'source/rating/safe',
        'source/format/manga',
        'source/meta/ai',
      ],
      bodyText: postText,
      memoLines: const [
        GalleryDetailLine(text: '覚書の内容', urls: [], isBullet: true),
        GalleryDetailLine(
          text: '入れ子の覚書',
          urls: [],
          isBullet: true,
          indentLevel: 1,
        ),
      ],
      relatedLines: const [
        GalleryDetailLine(
          text: '関連ノート',
          urls: ['related-note.md'],
          linkedNoteId: 2,
        ),
      ],
      media: const [
        GalleryMediaItem(id: 1, noteId: 1, isVideo: false, exists: true),
        GalleryMediaItem(id: 2, noteId: 1, isVideo: false, exists: true),
      ],
    );
  }
}
