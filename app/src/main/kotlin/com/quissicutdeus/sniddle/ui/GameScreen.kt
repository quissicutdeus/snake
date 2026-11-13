package com.quissicutdeus.sniddle.ui

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
import com.quissicutdeus.sniddle.ComposeCanvas2D
import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.audio.SoundCues
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Buzz
import com.quissicutdeus.sniddle.game.render.Camera
import com.quissicutdeus.sniddle.game.render.Fx
import com.quissicutdeus.sniddle.game.render.Haptics
import com.quissicutdeus.sniddle.game.render.HudInsets
import com.quissicutdeus.sniddle.game.render.SceneRenderer
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SavedRun
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.game.sim.SteerInput
import com.quissicutdeus.sniddle.game.sim.Thumbstick
import kotlin.time.Clock

/**
 * A one-player game: the world and the kid's [Seat], which fills the screen,
 * run by the shared [GameLoop] (not Compose state: it all changes every frame).
 */
private class GameHolder(
    style: SnailStyle,
    stick: Thumbstick,
    resume: SavedRun? = null,
    playerName: String = "You",
    /** Today's holidays and arena season (see [Profile.todaysOptions]); its difficulty is ignored when carrying on a saved game. */
    options: SimOptions,
) {
    val sim = GameSim(playerStyle = style, playerName = playerName, options = options, resume = resume)
    val seat = Seat(stick)

    /** After being gobbled, the world keeps going a moment (the GULP! effect) before the game-over screen. */
    val loop = GameLoop(sim, listOf(seat), GAME_OVER_DELAY_SECONDS, ::result)

    /** How the game went, for the game-over screen. */
    private fun result(): GameResult {
        val p = sim.player!!
        val little = sim.difficulty == Difficulty.LITTLE
        val now = Clock.System.now().toEpochMilliseconds()
        return GameResult(p.score, p.snailsEaten, p.style, now, sim.eatenBy, sim.biggestSeconds.toInt(), little)
    }
}

/** Size of the round pause button at the top centre (the HUD keeps clear of it). */
internal val PauseButtonSize = 52.dp

/** Thumbstick: drags shorter than this keep the snail going straight. */
internal val StickDeadZone = 12.dp

/** Thumbstick: how far the thumb gets from the centre before the centre follows it. */
internal val StickLeash = 56.dp

@Composable
fun GameScreen(
    key: Int,
    style: SnailStyle,
    playerName: String,
    onGameOver: (GameResult) -> Unit,
    onQuit: () -> Unit,
    /** Today's holidays (and birthday) and arena season, see [Profile.todaysOptions]. */
    options: SimOptions,
    modifier: Modifier = Modifier,
    settings: Settings = Settings(),
    onSettingsChange: (Settings) -> Unit = {},
    onSound: (Sound) -> Unit = {},
    onMusic: (Boolean) -> Unit = {},
    /** Show the first-game tips; [onTipsDone] runs once the kid has seen them all. */
    showTips: Boolean = false,
    onTipsDone: () -> Unit = {},
    /** The bedtime timer, if any: it counts the play and can stop the game at a calm moment. */
    bedtime: GameBedtime<GameResult>? = null,
    /** This kid plays in little kid mode (a saved game carries on as it was saved, though). */
    littleKid: Boolean = false,
) {
    val game = rememberGame(key, style, playerName, options.copy(difficulty = difficultyFor(littleKid)))
    var paused by rememberSaveable(key) { mutableStateOf(false) }
    val tips = rememberFirstGameTips(key, showTips)
    val nap = rememberBedtimeInGame(key, bedtime)
    val currentOnTipsDone by rememberUpdatedState(onTipsDone)
    // Going to the background pauses; the back button pauses / resumes.
    val frame =
        rememberGameFrames(
            game.loop,
            paused,
            settings.buzzOn,
            nap,
            GameLoopCallbacks(onSound, onMusic, onGameOver, { paused = it }) { dt ->
                tips?.onFrame(game.sim, game.seat.stick, dt, paused, currentOnTipsDone)
            },
        )

    Box(modifier.fillMaxSize()) {
        ArenaCanvas(game, frame, rememberHudInsets(), Modifier.fillMaxSize())
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
        TipCard(tips, hidden = paused || game.sim.gameOver, style, Modifier.align(Alignment.BottomCenter))
        BedtimeNote(nap, Modifier.align(Alignment.TopCenter))
        if (paused) {
            PauseOverlay(settings, onSettingsChange, onResume = { paused = false }, onQuit = onQuit)
        }
    }
}

/**
 * A new game for [key], or the one saved with the screen: if Android closes
 * the app in the background, the player's snail is saved with the screen and
 * comes back (paused, as hard as it was) in a fresh world. See SavedRun.
 */
@Composable
private fun rememberGame(
    key: Int,
    style: SnailStyle,
    playerName: String,
    options: SimOptions,
): GameHolder {
    val density = LocalDensity.current
    val newStick = { with(density) { Thumbstick(deadZone = StickDeadZone.toPx(), leash = StickLeash.toPx()) } }
    val saver =
        Saver<GameHolder, String>(
            save = { it.sim.snapshot()?.encode() },
            restore = { text -> SavedRun.decode(text)?.let { GameHolder(style, newStick(), it, options = options) } },
        )
    return rememberSaveable(key, saver = saver) { GameHolder(style, newStick(), playerName = playerName, options = options) }
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
                contentDescription = "Snail arena. Touch anywhere and drag your thumb the way your snail should go."
            }.onSizeChanged {
                game.seat.resize(it.width, it.height)
            }.pointerInput(game) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        game.seat.steerWith(event.changes)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        if (frame() < 0) return@Canvas // reading the frame counter makes this redraw every frame
        val seat = game.seat
        seat.adapter.draw(this) { c ->
            SceneRenderer.draw(c, game.sim, seat.camera, seat.fx, game.loop.time, insets, showHud = !game.sim.gameOver)
        }
        if (seat.stick.down && !game.sim.gameOver) drawStick(seat.stick)
    }
}

/** A faint ring where the thumb landed and a knob under the thumb. */
internal fun DrawScope.drawStick(stick: Thumbstick) {
    val center = Offset(stick.centerX, stick.centerY)
    val ring = StickLeash.toPx()
    drawCircle(Color.White.copy(alpha = 0.12f), ring, center)
    drawCircle(Color.White.copy(alpha = 0.35f), ring, center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.45f), ring * 0.45f, Offset(stick.thumbX, stick.thumbY))
}

@Composable
internal fun PauseButton(
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
internal fun PauseOverlay(
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
