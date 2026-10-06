package com.hanaretamae.kaede.core.settings

object SettingsTransferCodec {
    const val MAX_BYTES = 16 * 1024
    private const val HEADER = "kaede-gallery-settings=1"

    fun encode(settings: GallerySettings): String = buildString {
        appendLine(HEADER)
        appendLine("theme=${settings.appearance.theme.name}")
        appendLine("language=${settings.appearance.language.name}")
        appendLine("use_system_color=${settings.appearance.useSystemColor}")
        appendLine("pure_black=${settings.appearance.pureBlack}")
        appendLine("page_size=${settings.pageSize}")
        appendLine("show_loaded_range=${settings.showLoadedRange}")
        appendLine("show_tile_position=${settings.showTilePosition}")
        append("show_counts=${settings.showCounts}")
    }

    fun decode(content: String): GallerySettings? {
        if (content.encodeToByteArray().size > MAX_BYTES) return null
        val lines = content.split('\n').map { it.removeSuffix("\r") }
        if (lines.firstOrNull() != HEADER) return null
        val values = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            if (line.isEmpty()) continue
            val separator = line.indexOf('=')
            if (separator <= 0) return null
            val key = line.substring(0, separator)
            if (key !in KEYS || values.put(key, line.substring(separator + 1)) != null) {
                return null
            }
        }
        if (values.keys != KEYS) return null
        val theme = ThemePreference.entries.firstOrNull { it.name == values["theme"] }
            ?: return null
        val language = LanguagePreference.entries.firstOrNull { it.name == values["language"] }
            ?: return null
        val pageSize = values["page_size"]?.toIntOrNull()
            ?.takeIf { it in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE }
            ?: return null
        fun boolean(key: String): Boolean? = when (values[key]) {
            "true" -> true
            "false" -> false
            else -> null
        }
        return GallerySettings(
            appearance = AppearanceSettings(
                theme = theme,
                language = language,
                useSystemColor = boolean("use_system_color") ?: return null,
                pureBlack = boolean("pure_black") ?: return null,
            ),
            pageSize = pageSize,
            showLoadedRange = boolean("show_loaded_range") ?: return null,
            showTilePosition = boolean("show_tile_position") ?: return null,
            showCounts = boolean("show_counts") ?: return null,
        )
    }

    private val KEYS = setOf(
        "theme",
        "language",
        "use_system_color",
        "pure_black",
        "page_size",
        "show_loaded_range",
        "show_tile_position",
        "show_counts",
    )
}
