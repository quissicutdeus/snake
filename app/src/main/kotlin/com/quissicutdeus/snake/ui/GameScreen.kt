package com.quissicutdeus.snake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.snake.ComposeCanvas2D
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.render.Camera
import com.quissicutdeus.snake.game.render.Fx
import com.quissicutdeus.snake.game.render.HudInsets
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SteerInput
import kotlin.math.atan2

/** Mutable per-game objects the frame loop works on (not Compose state: they change every frame). */
private class GameHolder(
    style: SnakeStyle,
) {
    val sim = GameSim(playerStyle = style)
    val camera = Camera()
    val fx = Fx()
    val input = SteerInput()
    val adapter = ComposeCanvas2D()
    var time = 0f
    var width = 0f
    var height = 0f
    var fingerDown = false
    var fingerX = 0f
    var fingerY = 0f
    var overTimer = 0f
    var reported = false
}

/** Size of the round pause button at the top centre (the HUD keeps clear of it). */
private val PauseButtonSize = 52.dp

@Composable
fun GameScreen(
    key: Int,
    style: SnakeStyle,
    onGameOver: (GameResult) -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = remember(key) { GameHolder(style) }
    var paused by remember(key) { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    val currentOnGameOver by rememberUpdatedState(onGameOver)
    val deadZone = with(LocalDensity.current) { 14.dp.toPx() }

    // Pause when the app goes to the background; back button pauses / resumes.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { paused = true }
    BackHandler { paused = !paused }

    LaunchedEffect(game) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                step(game, dt, paused, deadZone)
                if (game.sim.gameOver && !game.reported && game.overTimer > GAME_OVER_DELAY_SECONDS) {
                    game.reported = true
                    val p = game.sim.player!!
                    currentOnGameOver(GameResult(p.score, p.snakesEaten, p.style, System.currentTimeMillis()))
                }
                frame++
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        ArenaCanvas(game, { frame }, rememberHudInsets(), Modifier.fillMaxSize())
        if (!game.sim.gameOver) {
            PauseButton(
                onClick = { paused = true },
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 10.dp),
            )
        }
        if (paused) {
            PauseOverlay(onResume = { paused = false }, onQuit = onQuit)
        }
    }
}

/** After being gobbled, the world keeps going this long (the GULP! effect) before the game-over screen. */
private const val GAME_OVER_DELAY_SECONDS = 1.6f

/** Safe-area insets for the HUD, plus room at the top centre for the pause button. */
@Composable
private fun rememberHudInsets(): HudInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing
    return HudInsets(
        left = safe.getLeft(density, layoutDirection).toFloat(),
        top = safe.getTop(density).toFloat(),
        right = safe.getRight(density, layoutDirection).toFloat(),
        topCenter = with(density) { PauseButtonSize.toPx() },
    )
}

/** The arena, redrawn every frame ([frame] is read while drawing), steered by touch. */
@Composable
private fun ArenaCanvas(
    game: GameHolder,
    frame: () -> Int,
    insets: HudInsets,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .semantics { contentDescription = "Snake arena. Touch and hold to steer your snake towards your finger." }
            .onSizeChanged {
                game.width = it.width.toFloat()
                game.height = it.height.toFloat()
            }.pointerInput(game) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val touch = event.changes.lastOrNull { it.pressed }
                        game.fingerDown = touch != null
                        if (touch != null) {
                            game.fingerX = touch.position.x
                            game.fingerY = touch.position.y
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        if (frame() < 0) return@Canvas // reading the frame counter makes this redraw every frame
        game.adapter.draw(this) { c ->
            SceneRenderer.draw(c, game.sim, game.camera, game.fx, game.time, insets, showHud = !game.sim.gameOver)
        }
    }
}

/** One frame: steer from the finger, advance the world, effects and camera. */
private fun step(
    game: GameHolder,
    dt: Float,
    paused: Boolean,
    deadZone: Float,
) {
    val sim = game.sim
    val p = sim.player ?: return
    if (!paused) {
        game.time += dt
        if (game.fingerDown && p.alive && game.width > 0f) {
            val hx = game.camera.worldToScreenX(p.x, game.width)
            val hy = game.camera.worldToScreenY(p.y, game.height)
            val dx = game.fingerX - hx
            val dy = game.fingerY - hy
            // Right on top of the head: keep the current heading instead of spinning.
            if (dx * dx + dy * dy > deadZone * deadZone) game.input.set(atan2(dy, dx))
        } else {
            game.input.release()
        }
        sim.update(dt, game.input)
        game.fx.onEvents(sim, sim.events)
        game.fx.update(dt)
        if (sim.gameOver) game.overTimer += dt
    }
    if (game.width > 0f) {
        game.camera.followSnake(p, sim.renderAlpha, game.width, game.height, if (paused) 0f else dt)
    }
}

@Composable
private fun PauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .size(PauseButtonSize)
                .semantics { contentDescription = "Pause" },
        shape = CircleShape,
        color = Color(0x662B2B3A),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("❚❚", fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0x992B2B3A))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ShadowText("Paused", MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(24.dp))
            KidButton("Keep playing", onResume)
            Spacer(Modifier.height(14.dp))
            KidButton("Quit to menu", onQuit, color = KidColors.Grape, big = false)
        }
    }
}
