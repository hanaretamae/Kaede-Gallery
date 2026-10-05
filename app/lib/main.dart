import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:window_manager/window_manager.dart';

import 'app.dart';
import 'platform/rust_library.dart';
import 'src/rust/frb_generated.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  MediaKit.ensureInitialized();
  if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
    await windowManager.ensureInitialized();
  }
  LicenseRegistry.addLicense(() async* {
    yield const LicenseEntryWithLineBreaks(
      ['mpv / FFmpeg / libass / libplacebo (bundled native libraries)'],
      'Android builds bundle libmpv and FFmpeg under LGPL-2.1-or-later '
      '(media-kit/libmpv-android-video-build v1.1.7, default flavor). '
      'Linux bundles include mpv and FFmpeg (built with --enable-gpl '
      '--enable-version3) and are distributed under GPL-3.0-or-later, plus '
      'libass (ISC) and libplacebo (LGPL-2.1-or-later). '
      'Sources: https://github.com/mpv-player/mpv, https://ffmpeg.org, '
      'https://github.com/NixOS/nixpkgs (revision pinned in flake.lock). '
      'See THIRD_PARTY_NOTICES.md in '
      'https://github.com/hanaretamae/Kaede-Gallery.',
    );
  });
  PaintingBinding.instance.imageCache
    ..maximumSize = 200
    ..maximumSizeBytes = 32 * 1024 * 1024;
  await RustLib.init(externalLibrary: rustLibraryForCurrentPlatform());
  runApp(const ProviderScope(child: VaultGalleryApp()));
}
