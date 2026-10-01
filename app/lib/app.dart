import 'package:flutter/material.dart';

import 'features/gallery/gallery_screen.dart';

class VaultGalleryApp extends StatelessWidget {
  const VaultGalleryApp({super.key});

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF8FA7D9);
    return MaterialApp(
      title: 'Vault Gallery',
      themeMode: ThemeMode.system,
      theme: _galleryTheme(seed),
      darkTheme: _galleryTheme(seed, brightness: Brightness.dark),
      home: const GalleryScreen(),
    );
  }
}

ThemeData _galleryTheme(
  Color seed, {
  Brightness brightness = Brightness.light,
}) {
  final colorScheme = ColorScheme.fromSeed(
    seedColor: seed,
    brightness: brightness,
  );
  return ThemeData(
    colorScheme: colorScheme,
    useMaterial3: true,
    appBarTheme: const AppBarTheme(scrolledUnderElevation: 0),
    cardTheme: CardThemeData(
      color: colorScheme.surfaceContainerLow,
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
    ),
    chipTheme: ChipThemeData(
      showCheckmark: true,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      side: BorderSide.none,
      backgroundColor: colorScheme.surfaceContainerHighest,
      selectedColor: colorScheme.secondaryContainer,
      labelStyle: TextStyle(color: colorScheme.onSurface),
      secondaryLabelStyle: TextStyle(color: colorScheme.onSecondaryContainer),
    ),
  );
}
