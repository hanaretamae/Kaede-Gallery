package com.hanaretamae.kaede.core.settings

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class LanguagePreference {
    SYSTEM,
    JAPANESE,
    ENGLISH,
}

fun LanguagePreference.resolve(systemLanguage: LanguagePreference): LanguagePreference =
    when (this) {
        LanguagePreference.SYSTEM ->
            if (systemLanguage == LanguagePreference.JAPANESE) {
                LanguagePreference.JAPANESE
            } else {
                LanguagePreference.ENGLISH
            }
        LanguagePreference.JAPANESE -> LanguagePreference.JAPANESE
        LanguagePreference.ENGLISH -> LanguagePreference.ENGLISH
    }

data class AppearanceSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: LanguagePreference = LanguagePreference.SYSTEM,
    val useSystemColor: Boolean = true,
    val pureBlack: Boolean = false,
)

data class GallerySettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val showLoadedRange: Boolean = true,
    val showTilePosition: Boolean = false,
    val showCounts: Boolean = true,
) {
    init {
        require(pageSize in MIN_PAGE_SIZE..MAX_PAGE_SIZE) { "pageSize is out of range" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 24
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 500
    }
}
