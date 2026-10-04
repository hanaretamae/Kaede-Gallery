import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:dbus/dbus.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show ScrollDirection;
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_3_expressive/material_3_expressive.dart';
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
import '../../platform/linux_file_manager.dart';
import '../../platform/vault_platform.dart';
import 'vault_picker_screen.dart';

part 'gallery_jump_dialog.dart';
part 'gallery_settings_screens.dart';
part 'tag_settings_screens.dart';
part 'note_structure_settings.dart';
part 'appearance_settings.dart';
part 'tag_filter_panel.dart';
part 'gallery_grid.dart';
part 'note_viewer.dart';

M3EListItem _expressiveSwitchTile({
  required String title,
  String? subtitle,
  required bool value,
  required ValueChanged<bool> onChanged,
}) => M3EListItem(
  headline: title,
  supportingText: subtitle,
  onTap: () => onChanged(!value),
  trailing: M3ESwitch(value: value, onChanged: onChanged, semanticLabel: title),
);

/// Leading icon on a tonal circular container, as in Material 3 Expressive
/// settings lists. [tone] cycles through the primary, secondary and tertiary
/// container roles.
Widget _settingsIcon(BuildContext context, IconData icon, {int tone = 0}) {
  final scheme = Theme.of(context).colorScheme;
  final (background, foreground) = switch (tone % 3) {
    0 => (scheme.primaryContainer, scheme.onPrimaryContainer),
    1 => (scheme.secondaryContainer, scheme.onSecondaryContainer),
    _ => (scheme.tertiaryContainer, scheme.onTertiaryContainer),
  };
  return Container(
    width: 40,
    height: 40,
    decoration: BoxDecoration(color: background, shape: BoxShape.circle),
    alignment: Alignment.center,
    child: Icon(icon, size: 24, color: foreground),
  );
}

Widget? _expressiveBackButton(BuildContext context) {
  final navigator = Navigator.of(context);
  if (!navigator.canPop()) return null;
  return M3EIconButton(
    variant: M3EIconButtonVariant.standard,
    icon: const Icon(Icons.arrow_back),
    tooltip: 'Back',
    onPressed: () => navigator.maybePop(),
  );
}

Widget _settingsPanel({
  required BuildContext context,
  required Widget child,
  Color? color,
  EdgeInsetsGeometry margin = const EdgeInsets.only(bottom: 8),
}) => Padding(
  padding: margin,
  child: DecoratedBox(
    decoration: BoxDecoration(
      color: color ?? Theme.of(context).colorScheme.surfaceContainerHigh,
      borderRadius: BorderRadius.circular(16),
    ),
    child: child,
  ),
);

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
          final appTheme = Theme.of(context);
          return VaultPickerScreen(
            onChooseVault: () =>
                ref.read(vaultSessionProvider.notifier).chooseVault(),
            onShowNoteExample: () {
              Navigator.of(context).push<void>(
                MaterialPageRoute<void>(
                  builder: (_) => const _FictionalNoteExampleScreen(),
                ),
              );
            },
            expressiveTheme: expressiveThemeFromMaterial(appTheme),
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
      const Scaffold(body: Center(child: M3EProgressIndicator.circular()));
}

class _VaultErrorScreen extends StatelessWidget {
  const _VaultErrorScreen({required this.onChooseVault});

  final VoidCallback onChooseVault;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: const M3EAppBar.top(),
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 40),
          const SizedBox(height: 12),
          const Text('Vault を開けませんでした。場所とアクセス権を確認してください。'),
          const SizedBox(height: 12),
          M3EButton.filled(
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
      appBar: M3EAppBar.top(
        title: galleryAppBarTitle(context, vaultName),
        subtitleText: rangeLabel,
        actions: [
          M3EIconButton(
            variant: M3EIconButtonVariant.standard,
            tooltip: 'タグで絞り込む',
            onPressed: () => M3EBottomSheet.show<void>(
              context,
              initialValue: M3EBottomSheetValue.fullScreen,
              expandToFullScreen: true,
              fullScreenTitle: 'タグで絞り込む',
              // Closing through the dismiss guard bypasses the sheet's
              // predictive-back transform, which left the sheet misdrawn.
              onDismissRequest: () async => true,
              builder: (context) =>
                  const FractionallySizedBox(child: _TagPanel()),
            ),
            icon: filterCount == 0
                ? const Icon(Icons.tune)
                : M3EBadge(
                    count: filterCount,
                    semanticLabel: '$filterCount 件の絞り込み',
                    child: const Icon(Icons.tune),
                  ),
          ),
          M3EIconButton(
            variant: M3EIconButtonVariant.standard,
            tooltip: totalCount == null
                ? countState.hasError
                      ? '件数を取得できませんが、指定した位置へ移動できます'
                      : '件数を計算中ですが、指定した位置へ移動できます'
                : totalCount == 0
                ? '表示できる項目がありません'
                : '指定した位置へ移動',
            semanticLabel: '指定した位置へ移動',
            onPressed: totalCount == 0
                ? null
                : () => _showGalleryStartIndexDialog(context, ref, totalCount),
            icon: Stack(
              clipBehavior: Clip.none,
              children: [
                const Icon(Icons.format_list_numbered),
                if (totalCount == null && countState.isLoading)
                  const Positioned(
                    right: -3,
                    bottom: -3,
                    child: SizedBox(
                      width: 12,
                      height: 12,
                      child: M3EProgressIndicator.circular(strokeWidth: 2),
                    ),
                  ),
              ],
            ),
          ),
          const _DisplayModeButton(),
          M3EIconButton(
            variant: M3EIconButtonVariant.standard,
            tooltip: 'Vault の変更を再読み込み',
            onPressed: () => ref.read(vaultSessionProvider.notifier).rescan(),
            icon: const Icon(Icons.refresh),
          ),
          M3EIconButton(
            variant: M3EIconButtonVariant.standard,
            tooltip: '設定',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (context) =>
                    _GallerySettingsScreen(initialSession: session),
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
