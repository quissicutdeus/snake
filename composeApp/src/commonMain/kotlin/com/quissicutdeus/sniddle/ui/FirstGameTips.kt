package com.quissicutdeus.sniddle.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Mood
import com.quissicutdeus.sniddle.game.sim.GameEvent
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.Thumbstick
import kotlin.math.sin

/** The first-game tips, in order. */
enum class Tip { STEER, EAT, DANGER }

/**
 * Which tip is showing, moved on by what the kid does: steering, eating a few
 * dots, then a few seconds of the danger tip. Times are game time, so a pause
 * doesn't use them up. [tip] is Compose state; null once they're all done.
 */
class FirstGameTips(
    start: Tip? = Tip.STEER,
) {
    var tip: Tip? by mutableStateOf(start)
        private set

    private var shownFor = 0f
    private var steered = false
    private var dotsEaten = 0

    /**
     * Call after each frame's game step, while [sim] still holds that step's
     * events. Does nothing while [paused]; calls [onDone] on the frame the last
     * tip finishes.
     */
    fun onFrame(
        sim: GameSim,
        stick: Thumbstick,
        dt: Float,
        paused: Boolean,
        onDone: () -> Unit,
    ) {
        if (paused || sim.gameOver) return
        if (advance(sim, stick, dt) && tip == null) onDone()
    }

    /** Moves on to the next tip if the kid has done this one; true if it moved on. */
    private fun advance(
        sim: GameSim,
        stick: Thumbstick,
        dt: Float,
    ): Boolean {
        val now = tip ?: return false
        val player = sim.player ?: return false
        shownFor += dt
        if (stick.heading() != null) steered = true
        dotsEaten += sim.events.count { it is GameEvent.AteFood && near(it, player.x, player.y, player.radius) }
        val done =
            when (now) {
                Tip.STEER -> steered
                Tip.EAT -> dotsEaten >= DOTS_TO_EAT
                Tip.DANGER -> shownFor >= DANGER_SECONDS
            }
        if (!done || shownFor < MIN_SECONDS) return false
        tip = Tip.entries.getOrNull(now.ordinal + 1)
        shownFor = 0f
        dotsEaten = 0
        return true
    }

    /** Whether food eaten at [e] was eaten by the snail whose head is at ([x], [y]) (the event doesn't say who ate it). */
    private fun near(
        e: GameEvent,
        x: Float,
        y: Float,
        radius: Float,
    ): Boolean {
        val dx = e.x - x
        val dy = e.y - y
        val reach = radius + HEAD_REACH
        return dx * dx + dy * dy <= reach * reach
    }

    companion object {
        /** Each tip stays at least this long, even if the kid already did the thing. */
        const val MIN_SECONDS = 2f

        /** The eating tip goes once the player has eaten this many dots. */
        const val DOTS_TO_EAT = 3

        /** The danger tip (nothing to do for it) stays this long. */
        const val DANGER_SECONDS = 6f

        /** How far past its head a snail eats from (as in SoundCues). */
        private const val HEAD_REACH = 40f

        /** Saves which tip is showing with the game screen. */
        val Saver: Saver<FirstGameTips, Int> =
            Saver(
                save = { it.tip?.ordinal ?: -1 },
                restore = { FirstGameTips(Tip.entries.getOrNull(it)) },
            )
    }
}

/** The tips for game [key], or null when [show] is false. */
@Composable
fun rememberFirstGameTips(
    key: Int,
    show: Boolean,
): FirstGameTips? {
    val tips = rememberSaveable(key, saver = FirstGameTips.Saver) { FirstGameTips() }
    return if (show) tips else null
}

/**
 * The current tip of [tips] (if any, and not [hidden]) as a card along the
 * bottom of the screen, clear of the HUD at the top. Not touchable: a finger
 * landing on it still steers the snail.
 */
@Composable
fun TipCard(
    tips: FirstGameTips?,
    hidden: Boolean,
    style: SnailStyle,
    modifier: Modifier = Modifier,
) {
    if (tips == null) return
    Crossfade(if (hidden) null else tips.tip, modifier.windowInsetsPadding(WindowInsets.safeDrawing), label = "tip") { shown ->
        if (shown == null) return@Crossfade
        val card =
            Modifier
                .padding(16.dp)
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(KidColors.Panel)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        when (shown) {
            Tip.STEER -> SteerTip(card)
            Tip.EAT -> EatTip(style, card)
            Tip.DANGER -> DangerTip(card)
        }
    }
}

@Composable
private fun TipText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, color = KidColors.Ink)
}

@Composable
private fun SteerTip(modifier: Modifier = Modifier) {
    val time = rememberAnimationTime()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 96.dp, height = 64.dp)) { drawDraggingFinger(time) }
        TipText("Drag anywhere to steer", Modifier.padding(start = 12.dp))
    }
}

/** The in-game stick (ring where the thumb landed, knob under it) with a finger sliding it side to side. */
private fun DrawScope.drawDraggingFinger(time: Float) {
    val ring = size.height * 0.3f
    val center = Offset(size.width / 2f, size.height * 0.34f)
    val knob = Offset(center.x + sin(time * 2.4f) * ring * 0.8f, center.y)
    drawCircle(KidColors.Sky.copy(alpha = 0.2f), ring, center)
    drawCircle(KidColors.Sky, ring, center, style = Stroke(2.dp.toPx()))
    drawCircle(KidColors.Sky.copy(alpha = 0.7f), ring * 0.45f, knob)
    drawHand(knob, 9.dp.toPx(), 2.dp.toPx())
}

/** A cartoon hand pointing up, fingertip at [tip]: drawn in ink a little bigger, then in white, so it has an outline. */
private fun DrawScope.drawHand(
    tip: Offset,
    finger: Float,
    line: Float,
) {
    for ((color, grow) in listOf(KidColors.Ink to line, Color.White to 0f)) {
        // The finger, from the tip down into the palm.
        drawRoundRect(
            color,
            Offset(tip.x - finger / 2f - grow, tip.y - finger * 0.3f - grow),
            Size(finger + grow * 2f, finger * 2.6f + grow * 2f),
            CornerRadius(finger / 2f + grow),
        )
        // The palm, off to the right like a right hand.
        drawRoundRect(
            color,
            Offset(tip.x - finger / 2f - grow, tip.y + finger * 1.2f - grow),
            Size(finger * 2.4f + grow * 2f, finger * 1.8f + grow * 2f),
            CornerRadius(finger * 0.7f + grow),
        )
    }
}

@Composable
private fun EatTip(
    style: SnailStyle,
    modifier: Modifier = Modifier,
) {
    val time = rememberAnimationTime()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        WigglingSnail(style, Modifier.size(width = 72.dp, height = 44.dp), length = 140f)
        Canvas(Modifier.size(width = 44.dp, height = 44.dp)) {
            DotColors.forEachIndexed { i, color ->
                val r = 5.dp.toPx() * (1f + 0.15f * sin(time * 4f + i))
                drawCircle(color, r, Offset(size.width * (0.2f + 0.3f * i), size.height / 2f))
            }
        }
        TipText("Eat dots to grow", Modifier.padding(start = 12.dp))
    }
}

private val DotColors = listOf(KidColors.Berry, KidColors.Sunny, KidColors.Sky)

/** Bigger snails look angry and their bite hurts; smaller ones look scared and can be gobbled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DangerTip(modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SnailPicture(OtherSnail, Modifier.size(60.dp), mood = Mood.ANGRY)
            TipText("Bigger ones bite!", Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                SnailPicture(OtherSnail, Modifier.size(38.dp), mood = Mood.SCARED)
            }
            TipText("Gobble smaller ones!", Modifier.padding(start = 8.dp))
        }
    }
}

private val OtherSnail = SnailStyle(color1 = 3, color2 = 5, pattern = Pattern.STRIPES, eyes = Eyes.GOOGLY)
