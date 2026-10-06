package com.hanaretamae.kaede.core.settings

import java.util.Locale

actual fun platformSystemLanguagePreference(): LanguagePreference =
    if (Locale.getDefault().language.equals("ja", ignoreCase = true)) {
        LanguagePreference.JAPANESE
    } else {
        LanguagePreference.ENGLISH
    }
