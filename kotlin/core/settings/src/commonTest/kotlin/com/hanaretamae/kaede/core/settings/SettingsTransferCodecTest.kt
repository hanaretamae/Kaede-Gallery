package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsTransferCodecTest {
    @Test
    fun roundTripsEverySetting() {
        val settings = GallerySettings(
            appearance = AppearanceSettings(
                theme = ThemePreference.DARK,
                language = LanguagePreference.JAPANESE,
                useSystemColor = false,
                pureBlack = true,
            ),
            pageSize = 90,
            showLoadedRange = false,
            showTilePosition = true,
            showCounts = false,
        )

        assertEquals(settings, SettingsTransferCodec.decode(SettingsTransferCodec.encode(settings)))
        assertEquals(
            settings,
            SettingsTransferCodec.decode(SettingsTransferCodec.encode(settings).replace("\n", "\r\n")),
        )
    }

    @Test
    fun rejectsUnsupportedOrMalformedSettings() {
        val valid = SettingsTransferCodec.encode(GallerySettings())
        for (invalid in listOf(
            valid.replace("=1", "=2"),
            valid.replace("page_size=24", "page_size=0"),
            valid.replace("show_counts=true", "show_counts=maybe"),
            valid.replace("show_counts=true", "show_counts=true\nunexpected=value"),
            valid.replace("show_counts=true", "show_counts=true\npage_size=24"),
            valid + "x".repeat(SettingsTransferCodec.MAX_BYTES),
        )) {
            assertNull(SettingsTransferCodec.decode(invalid))
        }
    }
}
