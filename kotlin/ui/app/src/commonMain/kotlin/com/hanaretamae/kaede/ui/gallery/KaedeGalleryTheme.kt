package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.ThemePreference

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val baseScheme = systemScheme ?: if (darkTheme) {
        darkColorScheme()
    } else {
        expressiveLightColorScheme()
    }
    val colorScheme = if (darkTheme && appearance.pureBlack) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF101010),
            surfaceVariant = Color(0xFF1B1B1B),
        )
    } else {
        baseScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MaterialTheme.shapes.copy(
            extraLarge = RoundedCornerShape(32.dp),
            largeIncreased = RoundedCornerShape(36.dp),
            large = RoundedCornerShape(28.dp),
            medium = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(12.dp),
        ),
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
                .background(colorScheme.background),
        ) {
            content()
        }
    }
}
