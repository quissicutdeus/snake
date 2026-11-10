package com.quissicutdeus.snake.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quissicutdeus.snake.ComposeCanvas2D
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.render.Camera
import com.quissicutdeus.snake.game.render.Fx
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions
import kotlin.math.cos
import kotlin.math.sin

/** AI snakes roaming the arena behind the menu, with a slowly drifting camera. */
private class AttractMode {
    val sim = GameSim(options = SimOptions(spawnPlayer = false, aiCount = 10, pelletCount = 420))
    val camera = Camera()
    val fx = Fx()
    val adapter = ComposeCanvas2D()
    val noInput =
        com.quissicutdeus.snake.game.sim
            .SteerInput()
    var time = 0f
}

@Composable
private fun ArenaBackground(modifier: Modifier = Modifier) {
    val attract = remember { AttractMode() }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(attract) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                attract.time += dt
                attract.sim.update(dt, attract.noInput)
                frame++
            }
        }
    }
    Canvas(modifier) {
        if (frame < 0) return@Canvas
        val a = attract
        val w = a.sim.world
        val t = a.time * 0.05f
        a.camera.snapTo(w.width / 2f + cos(t) * 900f, w.height / 2f + sin(t * 1.3f) * 700f, 30f, size.width, size.height)
        a.adapter.draw(this) { c -> SceneRenderer.draw(c, a.sim, a.camera, a.fx, a.time, showHud = false) }
    }
}

@Composable
fun TitleScreen(
    style: SnakeStyle,
    onPlay: () -> Unit,
    onMySnake: () -> Unit,
    onHighScores: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        ArenaBackground(Modifier.fillMaxSize())
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            val landscape = maxWidth > maxHeight
            val title: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShadowText(
                        "Sniddle",
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (landscape) 54.sp else 50.sp,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                    WigglingSnake(style, Modifier.size(width = 300.dp, height = if (landscape) 120.dp else 140.dp))
                }
            }
            val buttons: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    KidButton("Play!", onPlay)
                    KidButton("My Snake", onMySnake, color = KidColors.Sky)
                    KidButton("High Scores", onHighScores, color = KidColors.Grape)
                }
            }
            if (landscape) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    title()
                    Spacer(Modifier.width(40.dp))
                    buttons()
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    title()
                    Spacer(Modifier.height(28.dp))
                    buttons()
                }
            }
        }
    }
}
