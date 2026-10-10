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
    val fixedColumnCount: Int = 0,
    val galleryTagPrefixes: List<String> = GalleryTagPrefixesCodec.DEFAULT_PREFIXES,
    val includedTagPrefixes: List<String> = GalleryTagDisplayPrefixesCodec.DEFAULT_INCLUDED,
    val hiddenTagPrefixes: List<String> = GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN,
    val showMissingMediaIcon: Boolean = false,
    val showLoadedRange: Boolean = true,
    val showTilePosition: Boolean = false,
    val showCounts: Boolean = true,
    val flutterTagSettingsJson: String? = null,
) {
    init {
        require(pageSize in MIN_PAGE_SIZE..MAX_PAGE_SIZE) { "pageSize is out of range" }
        require(
            fixedColumnCount == 0 ||
                fixedColumnCount in MIN_FIXED_COLUMN_COUNT..MAX_FIXED_COLUMN_COUNT,
        ) {
            "fixedColumnCount is out of range"
        }
        require(GalleryTagPrefixesCodec.isValid(galleryTagPrefixes)) {
            "galleryTagPrefixes are invalid"
        }
        require(GalleryTagDisplayPrefixesCodec.isValid(includedTagPrefixes)) {
            "includedTagPrefixes are invalid"
        }
        require(GalleryTagDisplayPrefixesCodec.isValid(hiddenTagPrefixes)) {
            "hiddenTagPrefixes are invalid"
        }
        require(
            flutterTagSettingsJson == null ||
                flutterTagSettingsJson.encodeToByteArray().size <=
                SettingsTransferCodec.MAX_BYTES - 1024,
        ) { "flutterTagSettingsJson exceeds its size limit" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 24
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 500
        const val MAX_FIXED_COLUMN_COUNT = 12
        const val MIN_FIXED_COLUMN_COUNT = 2
    }
}
