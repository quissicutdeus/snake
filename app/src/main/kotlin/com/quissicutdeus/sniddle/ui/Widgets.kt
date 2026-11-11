package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quissicutdeus.sniddle.ComposeCanvas2D
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Mood
import com.quissicutdeus.sniddle.game.render.PosedSnail
import kotlin.math.min

/** A big, round, bright button that's easy for small fingers. */
@Composable
fun KidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = KidColors.Play,
    big: Boolean = true,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = if (big) 220.dp else 120.dp, minHeight = if (big) 64.dp else 52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = if (big) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Text with a soft dark shadow so it reads on the grass. */
@Composable
fun ShadowText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        style = style.copy(shadow = Shadow(color = Color(0xAA2B2B3A), offset = Offset(3f, 4f), blurRadius = 6f)),
    )
}

/** Seconds since this composable appeared, updated every frame. */
@Composable
fun rememberAnimationTime(): Float {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = (it - start) / 1_000_000_000f }
    }
    return time
}

/** A snail in [style] slithering in place. */
@Composable
fun WigglingSnail(
    style: SnailStyle,
    modifier: Modifier = Modifier,
    length: Float = 300f,
) {
    val posed = remember(style, length) { PosedSnail(style, length) }
    val adapter = remember { ComposeCanvas2D() }
    val time = rememberAnimationTime()
    Canvas(modifier) {
        // World box that holds the wave: body to the left, head at the right.
        val boxW = length * 0.95f + 70f
        val boxH = 175f
        val s = min(size.width / boxW, size.height / boxH)
        adapter.draw(this) { c ->
            c.save()
            c.translate(size.width / 2f, size.height / 2f)
            c.scale(s)
            posed.wave(boxW / 2f - 40f, 22f, time, amplitude = 24f, wavelength = 150f)
            posed.draw(c, time)
            c.restore()
        }
    }
}

/** A still, curled-up picture of a snail in [style] (used on the score list). */
@Composable
fun SnailPicture(
    style: SnailStyle,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.HAPPY,
) {
    val posed = remember(style) { PosedSnail(style, 520f) }
    val adapter = remember { ComposeCanvas2D() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            adapter.draw(this) { c ->
                c.save()
                c.translate(size.width / 2f, size.height / 2f)
                val diameter = posed.curl(0f, 0f)
                c.scale(min(size.width, size.height) / diameter)
                posed.draw(c, 0f, mood)
                c.restore()
            }
        }
    }
}
