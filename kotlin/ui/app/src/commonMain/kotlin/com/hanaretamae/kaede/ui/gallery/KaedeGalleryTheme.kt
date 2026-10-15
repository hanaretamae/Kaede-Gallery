package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme as seededColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.FontPreference
import com.hanaretamae.kaede.core.settings.ThemePreference
import com.hanaretamae.kaede.ui.gallery.resources.Res
import com.hanaretamae.kaede.ui.gallery.resources.google_sans_flex

/** Maple-leaf red from the launcher artwork; seeds the non-system Material You scheme. */
internal val KaedeSeedColor = Color(0xFFD0342C)

internal fun kaedeFallbackColorScheme(darkTheme: Boolean): ColorScheme =
    seededColorScheme(
        seedColor = KaedeSeedColor,
        isDark = darkTheme,
        style = PaletteStyle.Expressive,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

internal fun pureBlackColorScheme(baseScheme: ColorScheme): ColorScheme = baseScheme.copy(
    background = Color.Black,
)

internal fun settingsListBackdropColor(colorScheme: ColorScheme): Color = colorScheme.background

internal fun settingsListItemColor(colorScheme: ColorScheme): Color =
    colorScheme.surfaceContainer

internal fun exampleNoteFrameBorderColor(colorScheme: ColorScheme): Color =
    colorScheme.outline

internal fun resolveKaedeColorScheme(
    appearance: AppearanceSettings,
    darkTheme: Boolean,
    systemColorScheme: ColorScheme?,
    fallbackColorScheme: ColorScheme,
): ColorScheme {
    val baseScheme = if (appearance.useSystemColor) {
        systemColorScheme ?: fallbackColorScheme
    } else {
        fallbackColorScheme
    }
    return if (darkTheme && appearance.pureBlack) {
        pureBlackColorScheme(baseScheme)
    } else {
        baseScheme
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun KaedeGalleryTheme(
    appearance: AppearanceSettings,
    dynamicColorScheme: ColorScheme? = null,
    systemDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance.theme) {
        ThemePreference.SYSTEM -> systemDarkTheme ?: isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val fallbackScheme = remember(darkTheme) { kaedeFallbackColorScheme(darkTheme) }
    val colorScheme = resolveKaedeColorScheme(
        appearance = appearance,
        darkTheme = darkTheme,
        systemColorScheme = dynamicColorScheme,
        fallbackColorScheme = fallbackScheme,
    )

    val animatedScheme = animateColorScheme(colorScheme)
    val googleSansFlexFontFamily = FontFamily(
        Font(Res.font.google_sans_flex, weight = FontWeight.Normal),
    )
    val typography = remember(appearance.font) {
        if (appearance.font == FontPreference.GOOGLE_SANS_FLEX) {
            Typography().withFontFamily(googleSansFlexFontFamily)
        } else {
            Typography()
        }
    }

    MaterialExpressiveTheme(
        colorScheme = animatedScheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(),
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
                .background(animatedScheme.background),
        ) {
            content()
        }
    }
}

private fun Typography.withFontFamily(fontFamily: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

/** Cross-fades every color role so theme, palette, and pure-black changes do not snap. */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec: AnimationSpec<Color> = remember { MotionScheme.expressive().slowEffectsSpec() }

    @Composable
    fun animate(color: Color): Color = animateColorAsState(color, spec, label = "themeColor").value

    return target.copy(
        primary = animate(target.primary),
        onPrimary = animate(target.onPrimary),
        primaryContainer = animate(target.primaryContainer),
        onPrimaryContainer = animate(target.onPrimaryContainer),
        inversePrimary = animate(target.inversePrimary),
        secondary = animate(target.secondary),
        onSecondary = animate(target.onSecondary),
        secondaryContainer = animate(target.secondaryContainer),
        onSecondaryContainer = animate(target.onSecondaryContainer),
        tertiary = animate(target.tertiary),
        onTertiary = animate(target.onTertiary),
        tertiaryContainer = animate(target.tertiaryContainer),
        onTertiaryContainer = animate(target.onTertiaryContainer),
        background = animate(target.background),
        onBackground = animate(target.onBackground),
        surface = animate(target.surface),
        onSurface = animate(target.onSurface),
        surfaceVariant = animate(target.surfaceVariant),
        onSurfaceVariant = animate(target.onSurfaceVariant),
        surfaceTint = animate(target.surfaceTint),
        inverseSurface = animate(target.inverseSurface),
        inverseOnSurface = animate(target.inverseOnSurface),
        error = animate(target.error),
        onError = animate(target.onError),
        errorContainer = animate(target.errorContainer),
        onErrorContainer = animate(target.onErrorContainer),
        outline = animate(target.outline),
        outlineVariant = animate(target.outlineVariant),
        scrim = animate(target.scrim),
        surfaceBright = animate(target.surfaceBright),
        surfaceDim = animate(target.surfaceDim),
        surfaceContainer = animate(target.surfaceContainer),
        surfaceContainerHigh = animate(target.surfaceContainerHigh),
        surfaceContainerHighest = animate(target.surfaceContainerHighest),
        surfaceContainerLow = animate(target.surfaceContainerLow),
        surfaceContainerLowest = animate(target.surfaceContainerLowest),
        primaryFixed = animate(target.primaryFixed),
        primaryFixedDim = animate(target.primaryFixedDim),
        onPrimaryFixed = animate(target.onPrimaryFixed),
        onPrimaryFixedVariant = animate(target.onPrimaryFixedVariant),
        secondaryFixed = animate(target.secondaryFixed),
        secondaryFixedDim = animate(target.secondaryFixedDim),
        onSecondaryFixed = animate(target.onSecondaryFixed),
        onSecondaryFixedVariant = animate(target.onSecondaryFixedVariant),
        tertiaryFixed = animate(target.tertiaryFixed),
        tertiaryFixedDim = animate(target.tertiaryFixedDim),
        onTertiaryFixed = animate(target.onTertiaryFixed),
        onTertiaryFixedVariant = animate(target.onTertiaryFixedVariant),
    )
}
