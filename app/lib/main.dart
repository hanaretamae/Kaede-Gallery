import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:window_manager/window_manager.dart';

import 'app.dart';
import 'licenses/bundled_licenses.dart';
import 'platform/rust_library.dart';
import 'src/rust/frb_generated.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  MediaKit.ensureInitialized();
  if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
    await windowManager.ensureInitialized();
  }
  registerBundledLicenses();
  PaintingBinding.instance.imageCache
    ..maximumSize = 200
    ..maximumSizeBytes = 32 * 1024 * 1024;
  await RustLib.init(externalLibrary: rustLibraryForCurrentPlatform());
  runApp(const ProviderScope(child: VaultGalleryApp()));
}
