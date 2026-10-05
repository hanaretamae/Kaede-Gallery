import 'package:flutter/widgets.dart';

import 'l10n/app_localizations.dart';

/// The user-selectable language. [system] follows the OS: Japanese when the
/// system language is Japanese, English for every other language.
enum AppLanguage { system, ja, en }

/// Resolves the user-selected language and exposes localization to non-widget
/// code that cannot receive a [BuildContext].
abstract final class AppL10n {
  static AppLanguage _language = AppLanguage.system;

  static void apply(AppLanguage language) => _language = language;

  static bool get isJapanese => switch (_language) {
    AppLanguage.ja => true,
    AppLanguage.en => false,
    AppLanguage.system => _systemIsJapanese,
  };

  static bool get _systemIsJapanese =>
      WidgetsBinding.instance.platformDispatcher.locale.languageCode == 'ja';

  static Locale? get locale => switch (_language) {
    AppLanguage.system => null,
    AppLanguage.ja => const Locale('ja'),
    AppLanguage.en => const Locale('en'),
  };

  static Locale get resolvedLocale => Locale(isJapanese ? 'ja' : 'en');

  static AppLocalizations get current => lookupAppLocalizations(resolvedLocale);
}

extension AppLocalizationsContext on BuildContext {
  AppLocalizations get l10n =>
      Localizations.of<AppLocalizations>(this, AppLocalizations) ??
      AppL10n.current;
}
