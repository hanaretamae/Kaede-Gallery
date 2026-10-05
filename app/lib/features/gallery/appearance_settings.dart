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
        M3ESnackbar.show(
          context,
          message: tr(
            '外観設定を保存できませんでした。',
            'Could not save the appearance settings.',
          ),
        );
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
                      tr('テーマ', 'Theme'),
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: ExpressiveMaterialScope(
                      theme: Theme.of(context),
                      child: M3EButtonGroup(
                        semanticLabel: tr('画面テーマ', 'Screen theme'),
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
                        actions: [
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.brightness_auto),
                            label: Text(tr('システム', 'System')),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.light_mode_outlined),
                            label: Text(tr('ライト', 'Light')),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.dark_mode_outlined),
                            label: Text(tr('ダーク', 'Dark')),
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
                      tr('言語', 'Language'),
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: ExpressiveMaterialScope(
                      theme: Theme.of(context),
                      child: M3EButtonGroup(
                        semanticLabel: tr('表示言語', 'Display language'),
                        type: M3EButtonGroupType.connected,
                        style: M3EButtonStyle.tonal,
                        decoration: galleryChoiceButtonDecoration(colorScheme),
                        selectedIndex: AppLanguage.values.indexOf(
                          appearance.language,
                        ),
                        selectionRequired: true,
                        onSelectedIndexChanged: (index) {
                          if (index == null) return;
                          _save(
                            context,
                            ref,
                            appearance.copyWith(
                              language: AppLanguage.values[index],
                            ),
                          );
                        },
                        actions: [
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.translate),
                            label: Text(tr('システム', 'System')),
                          ),
                          const M3EButtonGroupAction(
                            icon: Icon(Icons.language),
                            label: Text('日本語'),
                          ),
                          const M3EButtonGroupAction(
                            icon: Icon(Icons.language),
                            label: Text('English'),
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
            title: tr('システムカラー（Material You）', 'System color (Material You)'),
            subtitle: tr('システムのアクセントカラーを使用します', 'Use the system accent color'),
            value: appearance.useSystemColor,
            onChanged: (value) =>
                _save(context, ref, appearance.copyWith(useSystemColor: value)),
          ),
          if (appearance.brightness != GalleryBrightnessMode.light)
            _expressiveSwitchTile(
              title: tr('ピュアブラック', 'Pure black'),
              subtitle: tr(
                'ダークテーマの背景面を黒にします。システムカラーはアクセントとして併用できます',
                'Use a black background in the dark theme. The system color can still be used as an accent.',
              ),
              value: appearance.pureBlack,
              onChanged: (value) =>
                  _save(context, ref, appearance.copyWith(pureBlack: value)),
            ),
          M3EListItem(
            headline: tr('初期設定に戻す', 'Reset to defaults'),
            leading: const Icon(Icons.restore),
            onTap: () => _save(
              context,
              ref,
              GalleryAppearance(language: appearance.language),
            ),
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
        tooltip: tr('表示方法', 'Display mode'),
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
          label: tr('ノートごとにまとめる', 'Group by note'),
          selected: mode == GalleryDisplayMode.byNote,
        ),
        M3EMenuSelectable(
          value: GalleryDisplayMode.allMedia,
          label: tr('すべてのメディアを表示', 'Show all media'),
          selected: mode == GalleryDisplayMode.allMedia,
        ),
      ],
    );
  }
}
