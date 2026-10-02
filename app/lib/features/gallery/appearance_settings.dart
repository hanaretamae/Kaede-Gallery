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
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('外観設定を保存できませんでした。')));
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
        Card(
          color: colorScheme.surfaceContainerLow,
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
                  child: SegmentedButton<GalleryBrightnessMode>(
                    segments: const [
                      ButtonSegment(
                        value: GalleryBrightnessMode.system,
                        icon: Icon(Icons.brightness_auto),
                        label: Text('システム'),
                      ),
                      ButtonSegment(
                        value: GalleryBrightnessMode.light,
                        icon: Icon(Icons.light_mode_outlined),
                        label: Text('ライト'),
                      ),
                      ButtonSegment(
                        value: GalleryBrightnessMode.dark,
                        icon: Icon(Icons.dark_mode_outlined),
                        label: Text('ダーク'),
                      ),
                    ],
                    selected: {appearance.brightness},
                    onSelectionChanged: (selection) => _save(
                      context,
                      ref,
                      appearance.copyWith(brightness: selection.single),
                    ),
                  ),
                ),
                const Divider(height: 24, indent: 16, endIndent: 16),
                SwitchListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                  title: const Text('システムカラー（Material You）'),
                  subtitle: const Text('システムのアクセントカラーを使用します'),
                  value: appearance.useSystemColor,
                  onChanged: (value) => _save(
                    context,
                    ref,
                    appearance.copyWith(useSystemColor: value),
                  ),
                ),
                if (appearance.brightness != GalleryBrightnessMode.light)
                  SwitchListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                    title: const Text('ピュアブラック'),
                    subtitle: const Text(
                      'ダークテーマの背景面を黒にします。システムカラーはアクセントとして併用できます',
                    ),
                    value: appearance.pureBlack,
                    onChanged: (value) => _save(
                      context,
                      ref,
                      appearance.copyWith(pureBlack: value),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _DisplayModeButton extends ConsumerWidget {
  const _DisplayModeButton();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(galleryDisplayModeProvider);
    return PopupMenuButton<GalleryDisplayMode>(
      tooltip: '表示方法',
      icon: Icon(
        mode == GalleryDisplayMode.byNote
            ? Icons.grid_view
            : Icons.photo_library_outlined,
      ),
      initialValue: mode,
      onSelected: (value) =>
          ref.read(galleryDisplayModeProvider.notifier).set(value),
      itemBuilder: (context) => [
        PopupMenuItem(
          value: GalleryDisplayMode.byNote,
          child: Row(
            children: [
              const Expanded(child: Text('ノートごとにまとめる')),
              if (mode == GalleryDisplayMode.byNote)
                const Icon(Icons.check, size: 18),
            ],
          ),
        ),
        PopupMenuItem(
          value: GalleryDisplayMode.allMedia,
          child: Row(
            children: [
              const Expanded(child: Text('すべてのメディアを表示')),
              if (mode == GalleryDisplayMode.allMedia)
                const Icon(Icons.check, size: 18),
            ],
          ),
        ),
      ],
    );
  }
}
