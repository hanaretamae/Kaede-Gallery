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
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: seed),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: seed,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const GalleryScreen(),
    );
  }
}
