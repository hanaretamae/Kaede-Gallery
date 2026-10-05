import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vault_gallery/licenses/bundled_licenses.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('selects native license texts for each shipped platform', () async {
    final androidAssets = bundledLicenseAssetsFor(TargetPlatform.android);
    final linuxAssets = bundledLicenseAssetsFor(TargetPlatform.linux);
    final windowsAssets = bundledLicenseAssetsFor(TargetPlatform.windows);

    expect(
      androidAssets.keys,
      contains(
        'GNU Lesser General Public License 2.1-or-later (Android media libraries)',
      ),
    );
    expect(
      linuxAssets.keys,
      contains(
        'GNU Lesser General Public License 2.1-or-later (Linux libplacebo)',
      ),
    );
    expect(
      windowsAssets['BSD 3-Clause License (Windows ANGLE)'],
      'assets/licenses/ANGLE-LICENSE.txt',
    );
    expect(
      windowsAssets['Apache License 2.0 (Windows SwiftShader)'],
      'assets/licenses/SwiftShader-LICENSE.txt',
    );
    expect(
      windowsAssets['Apache License 2.0 (Windows Vulkan Loader)'],
      'assets/licenses/Vulkan-Loader-LICENSE.txt',
    );
    expect(
      windowsAssets['Microsoft Direct3D Compiler redistribution notice'],
      'assets/licenses/MICROSOFT-WINDOWS-SDK-REDIST.txt',
    );
    for (final entry in windowsAssets.entries) {
      expect(
        await rootBundle.loadString(entry.value),
        isNotEmpty,
        reason: entry.key,
      );
    }
  });

  test('registers full bundled native and Rust license texts', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.linux;
    try {
      registerBundledLicenses();
    } finally {
      debugDefaultTargetPlatformOverride = null;
    }
    final entries = await LicenseRegistry.licenses.toList();
    expect(
      entries.any(
        (entry) => entry.packages.contains('Bundled native libraries'),
      ),
      isTrue,
    );
    final gplEntry = entries.singleWhere(
      (entry) => entry.packages.contains(
        'GNU General Public License 3.0-or-later (Linux media libraries)',
      ),
    );
    final gplText = gplEntry.paragraphs
        .map((paragraph) => paragraph.text)
        .join('\n');
    expect(gplText, contains('GNU GENERAL PUBLIC LICENSE'));

    final lgplEntry = entries.singleWhere(
      (entry) => entry.packages.contains(
        'GNU Lesser General Public License 2.1-or-later (Linux libplacebo)',
      ),
    );
    final lgplText = lgplEntry.paragraphs
        .map((paragraph) => paragraph.text)
        .join('\n');
    expect(lgplText, contains('GNU LESSER GENERAL PUBLIC LICENSE'));

    final rustEntry = entries.singleWhere(
      (entry) =>
          entry.packages.contains('Rust dependency notices and license texts'),
    );
    expect(
      rustEntry.paragraphs.map((paragraph) => paragraph.text).join('\n'),
      contains('Used by:'),
    );
  });
}
