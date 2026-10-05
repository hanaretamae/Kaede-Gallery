import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

const _linuxLicenseAssets = <String, String>{
  'GNU General Public License 3.0-or-later (Linux media libraries)':
      'assets/licenses/GPL-3.0.txt',
  'GNU Lesser General Public License 2.1-or-later (Linux libplacebo)':
      'assets/licenses/LGPL-2.1.txt',
  'ISC License (libass)': 'assets/licenses/ISC.txt',
};

const _androidLicenseAssets = <String, String>{
  'GNU Lesser General Public License 2.1-or-later (Android media libraries)':
      'assets/licenses/LGPL-2.1.txt',
};

const _windowsLicenseAssets = <String, String>{
  'GNU Lesser General Public License 3.0-or-later (Windows media libraries)':
      'assets/licenses/LGPL-3.0.txt',
  'Apache License 2.0 (Windows SwiftShader)':
      'assets/licenses/SwiftShader-LICENSE.txt',
  'SwiftShader authors': 'assets/licenses/SwiftShader-AUTHORS.txt',
  'Apache License 2.0 (Windows Vulkan Loader)':
      'assets/licenses/Vulkan-Loader-LICENSE.txt',
  'Vulkan Loader copyright notices': 'assets/licenses/Vulkan-Loader-NOTICE.txt',
  'BSD 3-Clause License (Windows ANGLE)': 'assets/licenses/ANGLE-LICENSE.txt',
  'zlib License (Windows zlib)': 'assets/licenses/zlib-LICENSE.txt',
  'Microsoft Direct3D Compiler redistribution notice':
      'assets/licenses/MICROSOFT-WINDOWS-SDK-REDIST.txt',
};

@visibleForTesting
Map<String, String> bundledLicenseAssetsFor(TargetPlatform platform) {
  switch (platform) {
    case TargetPlatform.linux:
      return _linuxLicenseAssets;
    case TargetPlatform.android:
      return _androidLicenseAssets;
    case TargetPlatform.windows:
      return _windowsLicenseAssets;
    case TargetPlatform.fuchsia:
    case TargetPlatform.iOS:
    case TargetPlatform.macOS:
      return const {};
  }
}

void registerBundledLicenses() {
  final targetPlatform = defaultTargetPlatform;
  LicenseRegistry.addLicense(() async* {
    final licenseAssets = <String, String>{
      ...bundledLicenseAssetsFor(targetPlatform),
      'Rust dependency notices and license texts':
          'assets/licenses/RUST-DEPENDENCY-LICENSES.txt',
    };
    yield const LicenseEntryWithLineBreaks(
      ['Bundled native libraries'],
      'The license texts and Rust dependency notices for bundled libraries '
      'are included in the following entries and in the distribution '
      'license-documents. See THIRD_PARTY_NOTICES.md for component sources '
      'and replacement/source information.',
    );
    for (final entry in licenseAssets.entries) {
      yield LicenseEntryWithLineBreaks([
        entry.key,
      ], await rootBundle.loadString(entry.value));
    }
  });
}
