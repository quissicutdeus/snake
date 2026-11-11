package com.quissicutdeus.snake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.render.HudInsets
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.render.TwoPlayerArt
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions
import com.quissicutdeus.snake.game.sim.Thumbstick
import kotlin.math.ceil

/** In the last seconds the clock turns red. */
private const val HURRY_SECONDS = 10

/** Snake colours lighter than this get dark letters on their name tag. */
private const val LIGHT_COLOUR = 0.6f

/** A gobbled kid sees the GULP! this long before "Oops! Back in 3…". */
private const val GULP_SECONDS = 0.7f

/** The HUD in each half keeps this far from the middle of the screen, clear of the pause button and the clock. */
private val SeamClearance = 56.dp

/**
 * Two kids on one screen, side by side: each half follows its own kid's
 * snake, and each kid steers by touching their own half. [kids] has the left
 * kid first. The game lasts [seconds]; [prepare] lets tests set the world up.
 */
@Composable
fun TwoPlayerScreen(
    key: Int,
    kids: List<PlayerCard>,
    onGameOver: (TogetherResult) -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    settings: Settings = Settings(),
    onSettingsChange: (Settings) -> Unit = {},
    onSound: (Sound) -> Unit = {},
    onMusic: (Boolean) -> Unit = {},
    /** Today's holidays and arena, and how gentle (see [Profile.togetherOptions]). */
    options: SimOptions = SimOptions(),
    /** The bedtime timer, if any: it counts the play and can stop the game at a calm moment. */
    bedtime: GameBedtime<TogetherResult>? = null,
    seconds: Float = GameConfig.TWO_PLAYER_SECONDS,
    prepare: (GameSim) -> Unit = {},
) {
    val density = LocalDensity.current
    val game =
        remember(key) {
            val newStick = { with(density) { Thumbstick(deadZone = StickDeadZone.toPx(), leash = StickLeash.toPx()) } }
            TwoPlayerGame(kids, newStick, seconds, options).also { prepare(it.sim) }
        }
    val nap = rememberBedtimeInGame(key, bedtime)
    var paused by remember(key) { mutableStateOf(false) }
    var secondsLeft by remember(key) { mutableIntStateOf(ceil(seconds).toInt()) }
    var frame by remember { mutableIntStateOf(0) }
    val currentOnGameOver by rememberUpdatedState(onGameOver)
    val currentOnSound by rememberUpdatedState(onSound)
    val currentOnMusic by rememberUpdatedState(onMusic)
    val buzzOn by rememberUpdatedState(settings.buzzOn)
    val view = LocalView.current

    // Pause both halves when the app goes to the background; back pauses / resumes.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { paused = true }
    BackHandler { paused = !paused }
    DisposableEffect(game) { onDispose { currentOnMusic(false) } }

    LaunchedEffect(game) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                val buzz = game.step(dt, paused, currentOnSound)
                if (buzz != null && buzzOn) view.performHapticFeedback(hapticFor(buzz))
                secondsLeft = ceil(game.timeLeft).toInt()
                nap?.onFrame(game.sim, dt, paused, game::stop)
                game.finish(currentOnGameOver)
                game.updateMusic(paused, currentOnMusic)
                frame++
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        Halves(game, { frame }, Modifier.fillMaxSize())
        TopMiddle(secondsLeft, showPause = !paused && secondsLeft > 0, onPause = { paused = true })
        if (secondsLeft == 0) TimeUp()
        // Under the shared clock.
        BedtimeNote(nap, Modifier.align(Alignment.TopCenter).padding(top = 40.dp))
        if (paused) PauseOverlay(settings, onSettingsChange, onResume = { paused = false }, onQuit = onQuit)
    }
}

/** The two halves, a line between them, each kid's name, and the thumbsticks on top (touches go to the half they start in). */
@Composable
private fun Halves(
    game: TwoPlayerGame,
    frame: () -> Int,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .pointerInput(game) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        game.steerHalves(event.changes, width)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            game.halves.indices.forEach { i -> HalfArena(game, i, frame) }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(4.dp)
                .background(KidColors.Ink),
        )
        Row(Modifier.fillMaxSize()) {
            game.kids.forEach { kid -> NameTag(kid) }
        }
        Canvas(Modifier.fillMaxSize()) {
            if (frame() < 0 || game.sim.gameOver) return@Canvas
            game.halves.forEach { if (it.stick.down) drawStick(it.stick) }
        }
    }
}

/** One kid's half: the arena as their camera sees it, clipped to the half, redrawn every frame. */
@Composable
private fun RowScope.HalfArena(
    game: TwoPlayerGame,
    index: Int,
    frame: () -> Int,
) {
    val half = game.halves[index]
    val insets = rememberHalfInsets(left = index == 0)
    val kid = game.kids[index]
    Canvas(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clipToBounds()
            .semantics { contentDescription = "${kid.label}'s half. Touch here and drag the way your snake should go." }
            .onSizeChanged {
                half.width = it.width.toFloat()
                half.height = it.height.toFloat()
            },
    ) {
        if (frame() < 0) return@Canvas // reading the frame counter makes this redraw every frame
        val sim = game.sim
        val me = sim.players[index]
        val state = sim.together!!.kids[index]
        half.adapter.draw(this) { c ->
            SceneRenderer.draw(c, sim, half.camera, half.fx, game.time, insets, showHud = !sim.gameOver, me = me)
            // The GULP! plays first, then "Oops!" takes over the half.
            val oops = state.backIn > 0f && state.backIn < GameConfig.COME_BACK_SECONDS - GULP_SECONDS
            if (!me.alive && oops) TwoPlayerArt.drawComingBack(c, state.backIn, state.gobbledBy?.name)
        }
    }
}

/** The kid's name in their snake's colour, at the bottom of their half, so everyone knows whose side is whose. */
@Composable
private fun RowScope.NameTag(kid: PlayerCard) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            kid.label,
            style = MaterialTheme.typography.titleMedium,
            // Dark letters on a light snake colour (a white snake), white ones otherwise.
            color = if (Color(kid.style.bodyColor).luminance() > LIGHT_COLOUR) KidColors.Ink else Color.White,
            modifier =
                Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(kid.style.bodyColor).copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 2.dp),
        )
    }
}

/** Safe-area insets for one half's HUD, kept clear of the pause button and the clock in the middle. */
@Composable
private fun rememberHalfInsets(left: Boolean): HudInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing
    val seam = with(density) { SeamClearance.toPx() }
    return HudInsets(
        left = if (left) safe.getLeft(density, layoutDirection).toFloat() else seam,
        top = safe.getTop(density).toFloat(),
        right = if (left) seam else safe.getRight(density, layoutDirection).toFloat(),
    )
}

/** The pause button and, under it, the shared clock. */
@Composable
private fun BoxScope.TopMiddle(
    secondsLeft: Int,
    showPause: Boolean,
    onPause: () -> Unit,
) {
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showPause) PauseButton(onClick = onPause) else Box(Modifier.size(PauseButtonSize))
        Text(
            clock(secondsLeft),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .semantics { contentDescription = "Time left: ${clock(secondsLeft)}" }
                    .clip(RoundedCornerShape(50))
                    .background(if (secondsLeft <= HURRY_SECONDS) KidColors.Berry else Color(0x992B2B3A))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}

/** "2:41". */
internal fun clock(seconds: Int): String = "${seconds / SECONDS_IN_MINUTE}:${(seconds % SECONDS_IN_MINUTE).toString().padStart(2, '0')}"

private const val SECONDS_IN_MINUTE = 60

@Composable
private fun BoxScope.TimeUp() {
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        ShadowText("Time's up!", MaterialTheme.typography.displayLarge)
        Box(Modifier.height(4.dp))
        ShadowText("Who's biggest?", MaterialTheme.typography.headlineLarge)
    }
}
