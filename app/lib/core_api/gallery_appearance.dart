import 'dart:convert';
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

import '../l10n.dart';
import 'gallery_providers.dart';

enum GalleryBrightnessMode { system, light, dark }

class GalleryAppearance {
  const GalleryAppearance({
    this.brightness = GalleryBrightnessMode.system,
    this.useSystemColor = false,
    this.pureBlack = false,
    this.language = AppLanguage.system,
  });

  final GalleryBrightnessMode brightness;
  final bool useSystemColor;
  final bool pureBlack;
  final AppLanguage language;

  GalleryAppearance copyWith({
    GalleryBrightnessMode? brightness,
    bool? useSystemColor,
    bool? pureBlack,
    AppLanguage? language,
  }) => GalleryAppearance(
    brightness: brightness ?? this.brightness,
    useSystemColor: useSystemColor ?? this.useSystemColor,
    pureBlack: pureBlack ?? this.pureBlack,
    language: language ?? this.language,
  );

  Map<String, Object> toJson() => {
    'brightness': brightness.name,
    'useSystemColor': useSystemColor,
    'pureBlack': pureBlack,
    'language': language.name,
  };

  static GalleryAppearance fromJson(Map<String, dynamic> json) {
    final brightnessName = json['brightness'];
    final useSystemColor = json['useSystemColor'] ?? false;
    final pureBlack = json['pureBlack'];
    final languageName = json['language'] ?? AppLanguage.system.name;
    if (brightnessName is! String ||
        useSystemColor is! bool ||
        pureBlack is! bool ||
        languageName is! String) {
      throw const FormatException('Invalid gallery appearance settings.');
    }
    final brightness = GalleryBrightnessMode.values
        .where((mode) => mode.name == brightnessName)
        .firstOrNull;
    if (brightness == null) {
      throw const FormatException('Unknown gallery brightness setting.');
    }
    // An unknown language (for example from a newer version) falls back to the system language.
    final language =
        AppLanguage.values
            .where((value) => value.name == languageName)
            .firstOrNull ??
        AppLanguage.system;
    return GalleryAppearance(
      brightness: brightness,
      useSystemColor: useSystemColor,
      pureBlack: pureBlack,
      language: language,
    );
  }
}

final galleryAppearanceProvider =
    AsyncNotifierProvider<GalleryAppearanceController, GalleryAppearance>(
      GalleryAppearanceController.new,
    );

class GalleryAppearanceController extends AsyncNotifier<GalleryAppearance> {
  File? _settingsFile;

  @override
  Future<GalleryAppearance> build() async {
    final paths = await ref.read(vaultPlatformProvider).galleryPaths();
    final settingsFile = File(p.join(paths.dataDirectory, 'appearance.json'));
    _settingsFile = settingsFile;
    if (!await settingsFile.exists()) return const GalleryAppearance();
    final decoded = jsonDecode(await settingsFile.readAsString());
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Invalid gallery appearance settings.');
    }
    return GalleryAppearance.fromJson(decoded);
  }

  Future<void> updateAppearance(GalleryAppearance appearance) async {
    final file = _settingsFile;
    if (file == null) {
      throw StateError('Gallery appearance settings are not initialized.');
    }
    await file.parent.create(recursive: true);
    await file.writeAsString(jsonEncode(appearance.toJson()), flush: true);
    state = AsyncData(appearance);
  }

  Future<void> deleteSettings() async {
    final file = _settingsFile;
    if (file == null) {
      throw StateError('Gallery appearance settings are not initialized.');
    }
    if (await file.exists()) await file.delete();
    state = const AsyncData(GalleryAppearance());
  }
}
