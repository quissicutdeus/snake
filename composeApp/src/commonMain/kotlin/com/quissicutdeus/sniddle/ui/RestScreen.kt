package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Mood
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * The home screen with the bedtime timer: the title screen ([content]) (plus the
 * grown-ups' gear), or, once today's play is used up, the rest screen in its
 * place. A new day lifts it: it looks again whenever the app comes back and
 * once a minute.
 */
@Composable
internal fun BedtimeHome(
    bedtime: Bedtime,
    style: SnailStyle,
    onMySnail: () -> Unit,
    onHighScores: () -> Unit,
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { bedtime.refresh() }
    LaunchedEffect(bedtime) {
        // Straight away too: a game can end the usual way after time ran out.
        while (true) {
            bedtime.refresh()
            delay(LOOK_AGAIN_MS)
        }
    }
    if (bedtime.timeUp) {
        RestScreen(style, bedtime.lastGame, onMySnail, onHighScores, onMoreTime = bedtime::grantMore)
    } else {
        Box(Modifier.fillMaxSize()) {
            content()
            GrownUpsCorner(bedtime, Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(12.dp))
        }
    }
}

private const val LOOK_AGAIN_MS = 60_000L

/** Night sky behind the sleeping snail. */
private val Night = Color(0xFF243B6B)

/**
 * "Time to rest, snail!": the kid's snail curled up asleep, instead of Play.
 * My Snails and the high scores still work. A grown-up can give
 * [Bedtime.EXTRA_MINUTES] more, after the grown-up question.
 */
@Composable
internal fun RestScreen(
    style: SnailStyle,
    lastGame: BedtimeGame?,
    onMySnail: () -> Unit,
    onHighScores: () -> Unit,
    onMoreTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(Night)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            val words: @Composable () -> Unit = {
                RestWords(lastGame, onMySnail, onHighScores, onGrownUps = { asking = true })
            }
            if (maxWidth > maxHeight) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SleepingSnail(style, 200.dp)
                    Box(Modifier.width(24.dp))
                    Box(Modifier.widthIn(max = 520.dp)) { words() }
                }
            } else {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SleepingSnail(style, 220.dp)
                    words()
                }
            }
        }
        if (asking) {
            GrownUpGate(
                onPass = {
                    asking = false
                    onMoreTime()
                },
                onCancel = { asking = false },
            )
        }
    }
}

@Composable
private fun RestWords(
    lastGame: BedtimeGame?,
    onMySnail: () -> Unit,
    onHighScores: () -> Unit,
    onGrownUps: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ShadowText("Time to rest, snail! 😴", MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center))
        ShadowText("See you tomorrow!", MaterialTheme.typography.titleLarge)
        if (lastGame != null) {
            ShadowText("What a game: length ${lastGame.result.score}!", MaterialTheme.typography.titleMedium)
            if (lastGame.unlocked.isNotEmpty()) UnlockBanner(lastGame.result.style, lastGame.unlocked, "Unlocked:")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KidButton("My Snails", onMySnail, color = KidColors.Sky, big = false)
            KidButton("High Scores", onHighScores, color = KidColors.Grape, big = false)
        }
        TextButton(onClick = onGrownUps) {
            Text(
                "Grown-ups: ${Bedtime.EXTRA_MINUTES} more minutes",
                color = Color(0xCCFFFFFF),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** The kid's snail curled up with its eyes shut, under a moon, with Zs floating up. */
@Composable
private fun SleepingSnail(
    style: SnailStyle,
    size: Dp,
) {
    val time = rememberAnimationTime()
    Box(Modifier.size(size)) {
        Text("🌙", fontSize = 40.sp, modifier = Modifier.align(Alignment.TopStart))
        SnailPicture(style, Modifier.fillMaxSize().padding(top = 24.dp), mood = Mood.ASLEEP)
        listOf(18, 24, 32).forEachIndexed { i, sp ->
            val bob = (sin(time * 1.6f + i) * 5f).dp
            Text(
                "Z",
                color = Color.White,
                fontSize = sp.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-44 + i * 18).dp, y = (54 - i * 26).dp + bob),
            )
        }
    }
}
