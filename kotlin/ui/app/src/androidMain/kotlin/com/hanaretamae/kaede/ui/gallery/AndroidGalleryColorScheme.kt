package com.hanaretamae.kaede.ui.gallery

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import com.hanaretamae.kaede.core.settings.ThemePreference

@Composable
fun androidDynamicColorScheme(
    context: Context,
    theme: ThemePreference,
): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val dark = when (theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
