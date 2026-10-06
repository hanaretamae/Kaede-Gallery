import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dynamic_color/dynamic_color.dart' as dynamic_color;

import 'app_theme.dart';
import 'core_api/gallery_appearance.dart';
import 'core_api/desktop_system_appearance.dart';
import 'core_api/linux_system_appearance.dart';
import 'l10n.dart';
import 'l10n/app_localizations.dart';
import 'features/gallery/gallery_screen.dart';

class VaultGalleryApp extends ConsumerWidget {
  const VaultGalleryApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    const seed = Color(0xFF8FA7D9);
    final appearance =
        ref.watch(galleryAppearanceProvider).asData?.value ??
        const GalleryAppearance();
    AppL10n.apply(appearance.language);
    final portalAccent = appearance.useSystemColor
        ? ref.watch(linuxPortalAccentColorProvider).asData?.value ??
              ref.watch(windowsAccentColorProvider).asData?.value
        : null;
    return dynamic_color.DynamicColorBuilder(
      builder: (lightDynamic, darkDynamic) {
        final lightSystemScheme = portalAccent == null
            ? lightDynamic == null
                  ? null
                  : ColorScheme.fromSeed(
                      seedColor: lightDynamic.primary,

                      brightness: Brightness.light,
                    )
            : ColorScheme.fromSeed(seedColor: portalAccent);
        final darkSystemScheme = portalAccent == null
            ? darkDynamic == null
                  ? null
                  : ColorScheme.fromSeed(
                      seedColor: darkDynamic.primary,

                      brightness: Brightness.dark,
                    )
            : ColorScheme.fromSeed(
                seedColor: portalAccent,

                brightness: Brightness.dark,
              );
        return MaterialApp(
          title: 'Kaede Gallery',
          locale: AppL10n.locale,
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          themeMode: switch (appearance.brightness) {
            GalleryBrightnessMode.system => ThemeMode.system,
            GalleryBrightnessMode.light => ThemeMode.light,
            GalleryBrightnessMode.dark => ThemeMode.dark,
          },
          theme: galleryTheme(
            seed,
            dynamicScheme: appearance.useSystemColor ? lightSystemScheme : null,
          ),
          darkTheme: galleryTheme(
            seed,
            brightness: Brightness.dark,
            dynamicScheme: appearance.useSystemColor ? darkSystemScheme : null,
            pureBlack: appearance.pureBlack,
          ),
          builder: (context, child) {
            final theme = Theme.of(context);
            return ExpressiveMaterialScope(
              theme: theme,
              child: child ?? const SizedBox.shrink(),
            );
          },
          home: const GalleryScreen(),
        );
      },
    );
  }
}
