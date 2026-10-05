import 'dart:io';

import 'package:flutter_rust_bridge/flutter_rust_bridge_for_generated.dart'
    show ExternalLibrary;
import 'package:path/path.dart' as path;

ExternalLibrary? rustLibraryForCurrentPlatform() {
  if (!Platform.isLinux && !Platform.isWindows) {
    return null;
  }

  final executableDirectory = File(Platform.resolvedExecutable).parent.path;
  final libraryPath = Platform.isWindows
      ? path.join(executableDirectory, 'gallery_bridge.dll')
      : path.join(executableDirectory, 'lib', 'libgallery_bridge.so');
  if (!File(libraryPath).existsSync()) {
    throw FileSystemException(
      'Bundled Rust bridge library was not found',
      libraryPath,
    );
  }
  return ExternalLibrary.open(libraryPath);
}
