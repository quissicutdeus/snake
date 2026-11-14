package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val lightColors =
    lightColorScheme(
        primary = KidColors.Play,
        onPrimary = Color.White,
        secondary = KidColors.Sky,
        onSecondary = Color.White,
        background = KidPalette.Light.menuBackground,
        surface = Color.White,
        onSurface = KidPalette.Light.text,
    )

// The text fields, switches and dialogs Material draws itself, matched to KidPalette.Night.
private val nightColors =
    darkColorScheme(
        primary = KidColors.Play,
        onPrimary = Color.White,
        secondary = KidColors.Sky,
        onSecondary = Color.White,
        background = KidPalette.Night.menuBackground,
        surface = KidPalette.Night.card,
        onSurface = KidPalette.Night.text,
        onSurfaceVariant = KidPalette.Night.subtleText,
        outline = KidPalette.Night.subtleText,
        surfaceVariant = KidPalette.Night.chip,
    )

private val typography =
    Typography().let { t ->
        t.copy(
            displayLarge = t.displayLarge.copy(fontWeight = FontWeight.Black),
            headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Black),
            headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
            bodyLarge = t.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
        )
    }

/**
 * Whether this device can buzz at all (see [com.quissicutdeus.sniddle.Buzzer.canBuzz]); false hides the buzz switch.
 * A fixed fact about the device, set once with the theme, like Compose's own LocalHapticFeedback: passing it
 * down through every screen to the one switch that needs it would be the bigger hidden dependency.
 */
@Suppress("ComposeCompositionLocalUsage")
internal val LocalCanBuzz = staticCompositionLocalOf { true }

/**
 * The menus' look: the light one, or the night one when the phone is set to
 * dark ([night], from the system setting unless a test fixes it).
 */
@Composable
fun SniddleTheme(
    canBuzz: Boolean = true,
    night: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCanBuzz provides canBuzz,
        LocalKidPalette provides if (night) KidPalette.Night else KidPalette.Light,
    ) {
        MaterialTheme(colorScheme = if (night) nightColors else lightColors, typography = typography, content = content)
    }
}
