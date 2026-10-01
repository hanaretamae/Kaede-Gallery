import 'dart:io';

import 'package:flutter_rust_bridge/flutter_rust_bridge_for_generated.dart'
    show ExternalLibrary;
import 'package:path/path.dart' as path;

ExternalLibrary? rustLibraryForCurrentPlatform() {
  if (!Platform.isLinux) {
    return null;
  }

  final libraryPath = path.join(
    File(Platform.resolvedExecutable).parent.path,
    'lib',
    'libgallery_bridge.so',
  );
  if (!File(libraryPath).existsSync()) {
    throw FileSystemException(
      'Bundled Rust bridge library was not found',
      libraryPath,
    );
  }
  return ExternalLibrary.open(libraryPath);
}
