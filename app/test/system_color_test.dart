import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vault_gallery/app.dart';
import 'package:vault_gallery/core_api/gallery_appearance.dart';
import 'package:vault_gallery/core_api/linux_system_appearance.dart';

class _FixedAppearance extends GalleryAppearanceController {
  _FixedAppearance(this.value);
  final GalleryAppearance value;
  @override
  Future<GalleryAppearance> build() async => value;
}

Future<Color> _primary(WidgetTester tester, bool useSystemColor) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        galleryAppearanceProvider.overrideWith(
          () => _FixedAppearance(
            GalleryAppearance(
              useSystemColor: useSystemColor,
              brightness: GalleryBrightnessMode.light,
            ),
          ),
        ),
        linuxPortalAccentColorProvider.overrideWith(
          (ref) => Stream.value(const Color(0xFFE5484D)),
        ),
      ],
      child: const VaultGalleryApp(),
    ),
  );
  for (var i = 0; i < 5; i++) {
    await tester.pump(const Duration(milliseconds: 50));
  }
  await tester.pump();
  return Theme.of(tester.element(find.byType(Scaffold).first))
      .colorScheme
      .primary;
}

void main() {
  testWidgets('system colour setting changes the theme', (tester) async {
    final off = await _primary(tester, false);
    await tester.pumpWidget(const SizedBox());
    final on = await _primary(tester, true);
    expect(on, isNot(off));
  });
}
