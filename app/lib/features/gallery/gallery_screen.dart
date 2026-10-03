import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show ScrollDirection;
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_selector/file_selector.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'package:path/path.dart' as p;
import 'package:url_launcher/url_launcher.dart';
import 'package:video_player/video_player.dart';
import 'package:window_manager/window_manager.dart';

import '../../app_theme.dart';
import '../../core_api/gallery_appearance.dart';
import '../../core_api/gallery_media_providers.dart';
import '../../core_api/gallery_providers.dart';
import '../../core_api/gallery_repository.dart';
import '../../core_api/gallery_tag_settings.dart';
import '../../platform/android_video_source.dart';
import '../../platform/vault_platform.dart';

part 'gallery_jump_dialog.dart';
part 'gallery_settings_screens.dart';
part 'tag_settings_screens.dart';
part 'note_structure_settings.dart';
part 'appearance_settings.dart';
part 'tag_filter_panel.dart';
part 'gallery_grid.dart';
part 'note_viewer.dart';

class GalleryScreen extends ConsumerWidget {
  const GalleryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(vaultSessionProvider);
    return session.when(
      loading: () => const _LoadingScreen(),
      error: (_, _) => _VaultErrorScreen(
        onChooseVault: () =>
            ref.read(vaultSessionProvider.notifier).chooseVault(),
      ),
      data: (vault) {
        if (vault == null) {
          return _VaultPickerScreen(
            onChooseVault: () =>
                ref.read(vaultSessionProvider.notifier).chooseVault(),
          );
        }
        return _GalleryLayout(session: vault);
      },
    );
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();

  @override
  Widget build(BuildContext context) =>
      const Scaffold(body: Center(child: CircularProgressIndicator()));
}

class _VaultPickerScreen extends StatelessWidget {
  const _VaultPickerScreen({required this.onChooseVault});

  final VoidCallback onChooseVault;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 88,
                height: 88,
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(GalleryShape.extraLarge),
                ),
                child: Icon(
                  Icons.photo_library_outlined,
                  size: 42,
                  color: colorScheme.onPrimaryContainer,
                ),
              ),
              const SizedBox(height: 24),
              Text(
                'Kaede Gallery',
                style: Theme.of(context).textTheme.headlineMedium
                    ?.copyWith(fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 8),
              Text(
                'Obsidian Vault のメディアをオフラインで閲覧できます。',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge
                    ?.copyWith(color: colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: onChooseVault,
                icon: const Icon(Icons.folder_open),
                label: const Text('Vault を選択'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _VaultErrorScreen extends StatelessWidget {
  const _VaultErrorScreen({required this.onChooseVault});

  final VoidCallback onChooseVault;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(),
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 40),
          const SizedBox(height: 12),
          const Text('Vault を開けませんでした。場所とアクセス権を確認してください。'),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: onChooseVault,
            child: const Text('別の Vault を選択'),
          ),
        ],
      ),
    ),
  );
}

class _GalleryLayout extends ConsumerWidget {
  const _GalleryLayout({required this.session});

  final VaultSession session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filterCount =
        ref.watch(selectedTagsProvider).length +
        ref.watch(allTagsProvider).length +
        ref.watch(excludedTagsProvider).length +
        ref.watch(selectedVirtualFiltersProvider).length +
        (ref.watch(gallerySearchQueryProvider).isEmpty ? 0 : 1);
    final vaultName = vaultDisplayName(session.vaultPath);
    final showItemCount = ref.watch(galleryShowItemCountProvider);
    final displayMode = ref.watch(galleryDisplayModeProvider);
    final loadedCount = showItemCount
        ? (displayMode == GalleryDisplayMode.byNote
              ? ref.watch(galleryItemsProvider).asData?.value.length
              : ref.watch(galleryMediaItemsProvider).asData?.value.length)
        : null;
    final startIndex = ref.watch(galleryVisiblePageStartProvider);
    final pageSize = ref.watch(galleryPageSizeProvider);
    final countState = ref.watch(galleryFilteredItemCountProvider);
    final totalCount = countState.asData?.value;
    final rangeLabel = countState.hasError
        ? '件数を取得できませんでした'
        : totalCount == null
        ? '件数を計算中'
        : loadedCount == null
        ? '$totalCount 件'
        : loadedCount == 0
        ? '$totalCount 件中 0 件'
        : '$totalCount 件中 ${startIndex + 1} - '
              '${(startIndex + pageSize).clamp(0, totalCount)} 件';
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(vaultName, overflow: TextOverflow.ellipsis),
            Text(rangeLabel, style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'タグで絞り込む',
            onPressed: () => showModalBottomSheet<void>(
              context: context,
              showDragHandle: true,
              isScrollControlled: true,
              builder: (context) => const FractionallySizedBox(
                heightFactor: 0.9,
                child: _TagPanel(),
              ),
            ),
            icon: Badge(
              isLabelVisible: filterCount > 0,
              label: Text('$filterCount'),
              child: const Icon(Icons.tune),
            ),
          ),
          if (totalCount != null && totalCount > 0)
            IconButton(
              tooltip: '指定した位置へ移動',
              onPressed: () =>
                  _showGalleryStartIndexDialog(context, ref, totalCount),
              icon: const Icon(Icons.format_list_numbered),
            ),
          const _DisplayModeButton(),
          IconButton(
            tooltip: 'Vault の変更を再読み込み',
            onPressed: () => ref.read(vaultSessionProvider.notifier).rescan(),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: '設定',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (context) => _GallerySettingsScreen(session: session),
              ),
            ),
            icon: const Icon(Icons.settings_outlined),
          ),
        ],
      ),
      body: const _GalleryGrid(),
    );
  }
}
