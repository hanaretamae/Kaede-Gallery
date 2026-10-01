import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vault_gallery/app.dart';
import 'package:vault_gallery/core_api/gallery_providers.dart';
import 'package:vault_gallery/core_api/gallery_repository.dart';
import 'package:vault_gallery/platform/vault_platform.dart';

void main() {
  testWidgets('asks for a vault when no location has been saved', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          galleryRepositoryProvider.overrideWithValue(_FakeRepository()),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Vault を選択'), findsOneWidget);
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
          galleryRepositoryProvider.overrideWithValue(repository),
          vaultPlatformProvider.overrideWithValue(_FakeVaultPlatform()),
        ],
        child: const VaultGalleryApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('fictional-vault'), findsOneWidget);
    expect(find.text('1 件'), findsOneWidget);
    expect(find.text('架空の note'), findsNothing);
    expect(find.text('コンテンツ'), findsOneWidget);
    expect(find.text('性別'), findsOneWidget);
    expect(find.textContaining('female'), findsOneWidget);
    expect(
      tester.widget<MaterialApp>(find.byType(MaterialApp)).theme!.useMaterial3,
      isTrue,
    );
    expect(find.text('gender'), findsOneWidget);

    await tester.tap(find.textContaining('複数画像'));
    await tester.pumpAndSettle();
    expect(repository.lastVirtualFilters, contains('multiple_media'));

    await tester.tap(find.text('gender'));
    await tester.pumpAndSettle();
    expect(repository.lastFilters, contains('source/gender'));

    await tester.tap(find.textContaining('female'));
    await tester.pumpAndSettle();

    expect(repository.lastFilters, contains('source/gender/female'));
  });
}

class _FakeVaultPlatform implements VaultPlatform {
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
  _FakeRepository({this.savedPath});

  final String? savedPath;
  List<String> lastFilters = const [];
  List<String> lastVirtualFilters = const [];

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
  Future<GalleryScanReport> scan(String vaultPath, String indexPath) async =>
      const GalleryScanReport(notesIndexed: 1, warnings: 0);

  @override
  Future<List<GalleryCategory>> listCategories(
    String vaultPath,
    String indexPath,
    List<String> filters,
    List<String> virtualFilters,
  ) async => const [
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
      path: 'source/gender',
      displayName: '性別',
      count: 1,
      options: [
        GalleryCategoryOption(
          name: 'female',
          fullTag: 'source/gender/female',
          count: 1,
          disabled: false,
        ),
      ],
    ),
  ];

  @override
  Future<List<GalleryNote>> queryNotes(
    String vaultPath,
    String indexPath,
    List<String> filters, {
    required List<String> virtualFilters,
    required int offset,
    required int limit,
  }) async {
    lastFilters = filters;
    lastVirtualFilters = virtualFilters;
    if (offset > 0) {
      return const [];
    }
    return const [
      GalleryNote(
        id: 1,
        path: 'fictional.md',
        title: '架空の note',
        mediaCount: 0,
        videoCount: 0,
        representativeMediaId: null,
      ),
    ];
  }

  @override
  Future<Uint8List?> getThumbnail(
    String vaultPath,
    String indexPath,
    String cachePath,
    int mediaId,
  ) async => null;
}
