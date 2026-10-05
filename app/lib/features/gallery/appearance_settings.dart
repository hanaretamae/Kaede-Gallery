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
          message: context.l10n.couldNotSaveTheAppearanceSettings,
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
                      context.l10n.theme,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: ExpressiveMaterialScope(
                      theme: Theme.of(context),
                      child: M3EButtonGroup(
                        semanticLabel: context.l10n.screenTheme,
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
                            label: Text(context.l10n.system),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.light_mode_outlined),
                            label: Text(context.l10n.light),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.dark_mode_outlined),
                            label: Text(context.l10n.dark),
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
                      context.l10n.language,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: ExpressiveMaterialScope(
                      theme: Theme.of(context),
                      child: M3EButtonGroup(
                        semanticLabel: context.l10n.displayLanguage,
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
                            label: Text(context.l10n.system),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.language),
                            label: Text(context.l10n.languageJapanese),
                          ),
                          M3EButtonGroupAction(
                            icon: const Icon(Icons.language),
                            label: Text(context.l10n.languageEnglish),
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
            title: context.l10n.systemColorMaterialYou,
            subtitle: context.l10n.useTheSystemAccentColor,
            value: appearance.useSystemColor,
            onChanged: (value) =>
                _save(context, ref, appearance.copyWith(useSystemColor: value)),
          ),
          if (appearance.brightness != GalleryBrightnessMode.light)
            _expressiveSwitchTile(
              title: context.l10n.pureBlack,
              subtitle:
                  context.l10n.useABlackBackgroundInTheDarkThemeTheSystemColorC,
              value: appearance.pureBlack,
              onChanged: (value) =>
                  _save(context, ref, appearance.copyWith(pureBlack: value)),
            ),
          M3EListItem(
            headline: context.l10n.resetToDefaults,
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
        tooltip: context.l10n.displayMode,
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
          label: context.l10n.groupByNote,
          selected: mode == GalleryDisplayMode.byNote,
        ),
        M3EMenuSelectable(
          value: GalleryDisplayMode.allMedia,
          label: context.l10n.showAllMedia,
          selected: mode == GalleryDisplayMode.allMedia,
        ),
      ],
    );
  }
}
