package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.ThemePreference

@Composable
fun KaedeGalleryTheme(
    appearance: AppearanceSettings,
    dynamicColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance.theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val systemScheme = dynamicColorScheme.takeIf { appearance.useSystemColor }
    val baseScheme = systemScheme ?: if (darkTheme) darkColorScheme() else lightColorScheme()
    val colorScheme = if (darkTheme && appearance.pureBlack) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF101010),
            surfaceVariant = Color(0xFF1B1B1B),
        )
    } else {
        baseScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
