package com.quissicutdeus.snake.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The player's switches, saved on the device. */
data class Settings(
    val soundOn: Boolean = true,
    val buzzOn: Boolean = true,
)

/** Sound and buzz switches as two round picture buttons, for the title and pause screens. */
@Composable
fun SettingsButtons(
    settings: Settings,
    onChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchButton(
            on = settings.buzzOn,
            name = "Buzz",
            onToggle = { onChange(settings.copy(buzzOn = !settings.buzzOn)) },
        ) { drawBuzz(settings.buzzOn) }
        SwitchButton(
            on = settings.soundOn,
            name = "Sound",
            onToggle = { onChange(settings.copy(soundOn = !settings.soundOn)) },
        ) { drawSpeaker(settings.soundOn) }
    }
}

@Composable
private fun SwitchButton(
    on: Boolean,
    name: String,
    onToggle: () -> Unit,
    icon: DrawScope.() -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier =
            Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = if (on) "$name on. Tap to turn it off." else "$name off. Tap to turn it on."
                },
        shape = CircleShape,
        color = Color(0x662B2B3A),
    ) {
        Canvas(Modifier.padding(14.dp), onDraw = icon)
    }
}

/** A speaker with sound waves (on) or a cross (off). */
private fun DrawScope.drawSpeaker(on: Boolean) {
    val w = size.width
    val h = size.height
    val speaker =
        Path().apply {
            moveTo(0f, h * 0.35f)
            lineTo(w * 0.22f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.5f, h * 0.92f)
            lineTo(w * 0.22f, h * 0.65f)
            lineTo(0f, h * 0.65f)
            close()
        }
    drawPath(speaker, Color.White)
    val stroke = Stroke(w * 0.09f, cap = StrokeCap.Round)
    if (on) {
        for (r in floatArrayOf(0.22f, 0.4f)) {
            drawArc(
                Color.White,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * (0.5f - r), h * (0.5f - r)),
                size = Size(w * 2 * r, h * 2 * r),
                style = stroke,
            )
        }
    } else {
        drawLine(Color.White, Offset(w * 0.66f, h * 0.32f), Offset(w * 0.98f, h * 0.68f), stroke.width, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.98f, h * 0.32f), Offset(w * 0.66f, h * 0.68f), stroke.width, StrokeCap.Round)
    }
}

/** A phone shaking (wiggle marks on both sides), or a phone crossed out. */
private fun DrawScope.drawBuzz(on: Boolean) {
    val w = size.width
    val h = size.height
    val line = w * 0.09f
    drawRoundRect(
        Color.White,
        topLeft = Offset(w * 0.3f, h * 0.05f),
        size = Size(w * 0.4f, h * 0.9f),
        cornerRadius = CornerRadius(w * 0.08f),
        style = Stroke(line),
    )
    if (on) {
        for (side in floatArrayOf(-1f, 1f)) {
            val x = w * 0.5f + side * w * 0.38f
            val zig =
                Path().apply {
                    moveTo(x, h * 0.25f)
                    lineTo(x + side * w * 0.08f, h * 0.38f)
                    lineTo(x, h * 0.5f)
                    lineTo(x + side * w * 0.08f, h * 0.62f)
                    lineTo(x, h * 0.75f)
                }
            drawPath(zig, Color.White, style = Stroke(line * 0.8f, cap = StrokeCap.Round))
        }
    } else {
        drawLine(Color.White, Offset(w * 0.08f, h * 0.08f), Offset(w * 0.92f, h * 0.92f), line, StrokeCap.Round)
    }
}
