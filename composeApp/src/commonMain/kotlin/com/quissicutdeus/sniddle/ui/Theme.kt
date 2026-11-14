package com.quissicutdeus.sniddle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val colors =
    lightColorScheme(
        primary = KidColors.Play,
        onPrimary = Color.White,
        secondary = KidColors.Sky,
        onSecondary = Color.White,
        background = KidColors.Grass,
        surface = Color.White,
        onSurface = KidColors.Ink,
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

@Composable
fun SniddleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
