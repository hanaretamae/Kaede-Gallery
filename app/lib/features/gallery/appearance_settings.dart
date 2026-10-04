part of 'gallery_screen.dart';

class _AppearanceSettings extends ConsumerWidget {
  const _AppearanceSettings();

  Future<void> _save(
    BuildContext context,
    WidgetRef ref,
    GalleryAppearance appearance,
  ) async {
    try {
      await ref
          .read(galleryAppearanceProvider.notifier)
          .updateAppearance(appearance);
    } on FileSystemException {
      if (context.mounted) {
        M3ESnackbar.show(context, message: '外観設定を保存できませんでした。');
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appearance =
        ref.watch(galleryAppearanceProvider).asData?.value ??
        const GalleryAppearance();
    final colorScheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: colorScheme.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                    child: Text(
                      'テーマ',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: ExpressiveMaterialScope(
                      theme: Theme.of(context),
                      child: M3EButtonGroup(
                        semanticLabel: '画面テーマ',
                        type: M3EButtonGroupType.connected,
                        style: M3EButtonStyle.tonal,
                        decoration: galleryChoiceButtonDecoration(colorScheme),
                        selectedIndex: GalleryBrightnessMode.values.indexOf(
                          appearance.brightness,
                        ),
                        selectionRequired: true,
                        onSelectedIndexChanged: (index) {
                          if (index == null) return;
                          _save(
                            context,
                            ref,
                            appearance.copyWith(
                              brightness: GalleryBrightnessMode.values[index],
                            ),
                          );
                        },
                        actions: const [
                          M3EButtonGroupAction(
                            icon: Icon(Icons.brightness_auto),
                            label: Text('システム'),
                          ),
                          M3EButtonGroupAction(
                            icon: Icon(Icons.light_mode_outlined),
                            label: Text('ライト'),
                          ),
                          M3EButtonGroupAction(
                            icon: Icon(Icons.dark_mode_outlined),
                            label: Text('ダーク'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        _settingsGroup(context, [
          _expressiveSwitchTile(
            title: 'システムカラー（Material You）',
            subtitle: 'システムのアクセントカラーを使用します',
            value: appearance.useSystemColor,
            onChanged: (value) =>
                _save(context, ref, appearance.copyWith(useSystemColor: value)),
          ),
          if (appearance.brightness != GalleryBrightnessMode.light)
            _expressiveSwitchTile(
              title: 'ピュアブラック',
              subtitle: 'ダークテーマの背景面を黒にします。システムカラーはアクセントとして併用できます',
              value: appearance.pureBlack,
              onChanged: (value) =>
                  _save(context, ref, appearance.copyWith(pureBlack: value)),
            ),
        ]),
      ],
    );
  }
}

class _DisplayModeButton extends ConsumerWidget {
  const _DisplayModeButton();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(galleryDisplayModeProvider);
    return M3EMenu(
      colorStyle: M3EMenuColorStyle.standard,
      selectedValue: mode,
      onSelected: (value) {
        if (value is GalleryDisplayMode) {
          ref.read(galleryDisplayModeProvider.notifier).set(value);
        }
      },
      anchorBuilder: (context, open) => M3EIconButton(
        variant: M3EIconButtonVariant.standard,
        tooltip: '表示方法',
        onPressed: open,
        icon: Icon(
          mode == GalleryDisplayMode.byNote
              ? Icons.grid_view
              : Icons.photo_library_outlined,
        ),
      ),
      children: [
        M3EMenuSelectable(
          value: GalleryDisplayMode.byNote,
          label: 'ノートごとにまとめる',
          selected: mode == GalleryDisplayMode.byNote,
        ),
        M3EMenuSelectable(
          value: GalleryDisplayMode.allMedia,
          label: 'すべてのメディアを表示',
          selected: mode == GalleryDisplayMode.allMedia,
        ),
      ],
    );
  }
}
