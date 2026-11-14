package com.quissicutdeus.sniddle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Happy birthday, Caleb!" on the title screen, on the kid's birthday. Until
 * they've got the birthday cake hat, it says how: play a game today.
 */
@Composable
fun HappyBirthday(
    name: String,
    hatEarned: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(bottom = 8.dp)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KidColors.Berry)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (name.isBlank()) "🎂 Happy birthday! 🎉" else "🎂 Happy birthday, $name! 🎉",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (!hatEarned) {
            Text(
                "Play a game to get your birthday cake hat!",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One bit of confetti: where it flies from the burst, and how it looks. */
private class Piece(
    rng: Random,
) {
    private val angle = -PI.toFloat() * (0.1f + 0.8f * rng.nextFloat())
    private val speed = 0.5f + 0.9f * rng.nextFloat()
    val vx = cos(angle) * speed
    val vy = sin(angle) * speed
    val spin = (rng.nextFloat() - 0.5f) * 1440f
    val color = CONFETTI[rng.nextInt(CONFETTI.size)]
    val wide = rng.nextBoolean()
}

private val CONFETTI =
    listOf(KidColors.Berry, KidColors.Sunny, KidColors.Sky, KidColors.Grape, Color(0xFF2ECC71), Color.White, KidColors.Play)

private const val CONFETTI_PIECES = 110
private const val CONFETTI_MS = 3600

/**
 * A burst of confetti over the whole screen, once each time [key] changes
 * (so picking the birthday kid on the title screen sets it off). It doesn't
 * take any taps: everything underneath still works.
 */
@Composable
fun BirthdayConfetti(
    key: Any,
    modifier: Modifier = Modifier,
) {
    val pieces = remember(key) { List(CONFETTI_PIECES) { Piece(Random(key.hashCode() * 31 + it)) } }
    val t = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { t.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
    Canvas(modifier) {
        val k = t.value
        if (k <= 0f || k >= 1f) return@Canvas
        val unit = size.height
        val secs = k * CONFETTI_MS / 1000f
        // Out from just above the middle, then falling and fading.
        val ox = size.width / 2f
        val oy = size.height * 0.4f
        val fade = if (k > 0.7f) (1f - k) / 0.3f else 1f
        val piece = Size(7.dp.toPx(), 12.dp.toPx())
        for (p in pieces) {
            val x = ox + p.vx * unit * secs * 0.8f
            val y = oy + (p.vy * secs + 0.45f * secs * secs) * unit * 0.8f
            val s = if (p.wide) Size(piece.height, piece.width) else piece
            rotate(p.spin * secs, Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - s.width / 2f, y - s.height / 2f), s)
            }
        }
    }
}
