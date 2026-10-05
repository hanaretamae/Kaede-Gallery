
import 'package:flutter/widgets.dart';

/// The user-selectable language. [system] follows the OS: Japanese when the
/// system language is Japanese, English for every other language.
enum AppLanguage { system, ja, en }

/// Process-wide UI language used by [tr].
///
/// UI strings live next to the widgets that show them as Japanese/English
/// pairs, so adding a language-specific string never touches a central file.
abstract final class AppL10n {
  static AppLanguage _language = AppLanguage.system;

  static AppLanguage get language => _language;

  static void apply(AppLanguage language) => _language = language;

  static bool get isJapanese => switch (_language) {
    AppLanguage.ja => true,
    AppLanguage.en => false,
    AppLanguage.system => _systemIsJapanese,
  };

  static bool get _systemIsJapanese =>
      WidgetsBinding.instance.platformDispatcher.locale.languageCode == 'ja';

  static Locale get locale => Locale(isJapanese ? 'ja' : 'en');

  /// Marks every element dirty so strings read through [tr] are re-evaluated
  /// without losing navigation or widget state.
  static void rebuildAll() {
    void visit(Element element) {
      element.markNeedsBuild();
      element.visitChildren(visit);
    }

    WidgetsBinding.instance.rootElement?.visitChildren(visit);
  }
}

/// Returns [ja] when the app language is Japanese, otherwise [en].
String tr(String ja, String en) => AppL10n.isJapanese ? ja : en;
