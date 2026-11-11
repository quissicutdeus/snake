package com.quissicutdeus.snake.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.snake.ComposeCanvas2D
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.SoundCues
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.render.Buzz
import com.quissicutdeus.snake.game.render.Camera
import com.quissicutdeus.snake.game.render.Fx
import com.quissicutdeus.snake.game.render.Haptics
import com.quissicutdeus.snake.game.render.HudInsets
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SavedRun
import com.quissicutdeus.snake.game.sim.SteerInput
import com.quissicutdeus.snake.game.sim.Thumbstick

/** Mutable per-game objects the frame loop works on (not Compose state: they change every frame). */
private class GameHolder(
    style: SnakeStyle,
    val stick: Thumbstick,
    resume: SavedRun? = null,
    playerName: String = "You",
) {
    val sim = GameSim(playerStyle = style, playerName = playerName, resume = resume)
    val camera = Camera()
    val fx = Fx()
    val haptics = Haptics()
    val soundCues = SoundCues()
    val input = SteerInput()
    val adapter = ComposeCanvas2D()
    var time = 0f
    var width = 0f
    var height = 0f

    /** The finger steering [stick], so a second finger can't make it jump. */
    var stickPointer: PointerId? = null
    var overTimer = 0f

    /** Whether the music was last told to play. */
    private var musicOn = false

    /** Music plays while the game is actually running, and stops when it's paused or over. */
    fun updateMusic(
        paused: Boolean,
        onMusic: (Boolean) -> Unit,
    ) {
        val now = !paused && !sim.gameOver
        if (now != musicOn) {
            musicOn = now
            onMusic(now)
        }
    }

    var reported = false

    /** How the game went, for the game-over screen. */
    fun result(): GameResult {
        val p = sim.player!!
        return GameResult(p.score, p.snakesEaten, p.style, System.currentTimeMillis(), sim.eatenBy, sim.biggestSeconds.toInt())
    }
}

/** Size of the round pause button at the top centre (the HUD keeps clear of it). */
private val PauseButtonSize = 52.dp

/** Thumbstick: drags shorter than this keep the snake going straight. */
private val StickDeadZone = 12.dp

/** Thumbstick: how far the thumb gets from the centre before the centre follows it. */
private val StickLeash = 56.dp

@Composable
fun GameScreen(
    key: Int,
    style: SnakeStyle,
    playerName: String,
    onGameOver: (GameResult) -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    settings: Settings = Settings(),
    onSettingsChange: (Settings) -> Unit = {},
    onSound: (Sound) -> Unit = {},
    onMusic: (Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val newStick = { with(density) { Thumbstick(deadZone = StickDeadZone.toPx(), leash = StickLeash.toPx()) } }
    // If Android closes the app in the background, the player's snake is saved
    // with the screen and comes back (paused) in a fresh world. See SavedRun.
    val saver =
        Saver<GameHolder, String>(
            save = { it.sim.snapshot()?.encode() },
            restore = { text -> SavedRun.decode(text)?.let { GameHolder(style, newStick(), it) } },
        )
    val game = rememberSaveable(key, saver = saver) { GameHolder(style, newStick(), playerName = playerName) }
    var paused by rememberSaveable(key) { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    val currentOnGameOver by rememberUpdatedState(onGameOver)
    val currentOnSound by rememberUpdatedState(onSound)
    val currentOnMusic by rememberUpdatedState(onMusic)
    val buzzOn by rememberUpdatedState(settings.buzzOn)
    val view = LocalView.current

    // Pause when the app goes to the background; back button pauses / resumes.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { paused = true }
    BackHandler { paused = !paused }

    // Leaving the game (quit, game over, another app) always stops the music.
    DisposableEffect(game) { onDispose { currentOnMusic(false) } }

    LaunchedEffect(game) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                val buzz = step(game, dt, paused, currentOnSound)
                if (buzz != null && buzzOn) view.performHapticFeedback(hapticFor(buzz))
                if (game.sim.gameOver && !game.reported && game.overTimer > GAME_OVER_DELAY_SECONDS) {
                    game.reported = true
                    currentOnGameOver(game.result())
                }
                game.updateMusic(paused, currentOnMusic)
                frame++
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        ArenaCanvas(game, { frame }, rememberHudInsets(), Modifier.fillMaxSize())
        if (!game.sim.gameOver && !paused) {
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
            PauseOverlay(settings, onSettingsChange, onResume = { paused = false }, onQuit = onQuit)
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
            .semantics {
                contentDescription = "Snake arena. Touch anywhere and drag your thumb the way your snake should go."
            }.onSizeChanged {
                game.width = it.width.toFloat()
                game.height = it.height.toFloat()
            }.pointerInput(game) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        steerWith(game, event.changes)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        if (frame() < 0) return@Canvas // reading the frame counter makes this redraw every frame
        game.adapter.draw(this) { c ->
            SceneRenderer.draw(c, game.sim, game.camera, game.fx, game.time, insets, showHud = !game.sim.gameOver)
        }
        if (game.stick.down && !game.sim.gameOver) drawStick(game.stick)
    }
}

/**
 * The first finger down steers; the stick starts where it lands. If that finger
 * lifts while another is still down, the other one takes over from where it is.
 */
private fun steerWith(
    game: GameHolder,
    changes: List<PointerInputChange>,
) {
    val stick = game.stick
    val current = changes.firstOrNull { it.id == game.stickPointer && it.pressed }
    if (current != null) {
        stick.move(current.position.x, current.position.y)
        return
    }
    val next = changes.firstOrNull { it.pressed }
    game.stickPointer = next?.id
    if (next == null) stick.release() else stick.press(next.position.x, next.position.y)
}

/** A faint ring where the thumb landed and a knob under the thumb. */
private fun DrawScope.drawStick(stick: Thumbstick) {
    val center = Offset(stick.centerX, stick.centerY)
    val ring = StickLeash.toPx()
    drawCircle(Color.White.copy(alpha = 0.12f), ring, center)
    drawCircle(Color.White.copy(alpha = 0.35f), ring, center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.45f), ring * 0.45f, Offset(stick.thumbX, stick.thumbY))
}

/**
 * One frame: steer from the thumbstick, advance the world, effects and camera.
 * Starts this frame's sounds with [onSound], and returns the buzz to play, if any.
 */
private fun step(
    game: GameHolder,
    dt: Float,
    paused: Boolean,
    onSound: (Sound) -> Unit,
): Buzz? {
    val sim = game.sim
    val p = sim.player ?: return null
    var buzz: Buzz? = null
    if (!paused) {
        game.time += dt
        if (game.stick.down && p.alive) {
            // Inside the dead zone the heading stays as it was.
            game.stick.heading()?.let { game.input.set(it) }
        } else {
            game.input.release()
        }
        sim.update(dt, game.input)
        game.fx.onEvents(sim, sim.events)
        buzz = game.haptics.onEvents(sim.events, p.id, dt)
        game.soundCues.onEvents(sim.events, p, dt).forEach(onSound)
        game.fx.update(dt)
        if (sim.gameOver) game.overTimer += dt
    }
    if (game.width > 0f) {
        game.camera.followSnake(p, sim.renderAlpha, game.width, game.height, if (paused) 0f else dt)
    }
    return buzz
}

/**
 * System haptics: no permission needed, and they follow the phone's own
 * "vibration & haptics" setting, so turning that off silences the game too
 * (as does the game's own buzz switch).
 */
private fun hapticFor(buzz: Buzz): Int =
    when (buzz) {
        Buzz.TAP -> HapticFeedbackConstants.CLOCK_TICK
        Buzz.THUD -> HapticFeedbackConstants.VIRTUAL_KEY
        Buzz.CHEER -> HapticFeedbackConstants.CONFIRM
        Buzz.OUCH -> HapticFeedbackConstants.LONG_PRESS
        Buzz.GOBBLED -> HapticFeedbackConstants.REJECT
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
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
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
            Spacer(Modifier.height(20.dp))
            SettingsButtons(settings, onSettingsChange)
        }
    }
}
