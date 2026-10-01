import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'platform/rust_library.dart';
import 'src/rust/frb_generated.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  PaintingBinding.instance.imageCache
    ..maximumSize = 400
    ..maximumSizeBytes = 64 * 1024 * 1024;
  await RustLib.init(externalLibrary: rustLibraryForCurrentPlatform());
  runApp(const ProviderScope(child: VaultGalleryApp()));
}
