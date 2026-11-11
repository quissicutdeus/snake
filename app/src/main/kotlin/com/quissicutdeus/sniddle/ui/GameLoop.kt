package com.quissicutdeus.sniddle.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.sniddle.ComposeCanvas2D
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.audio.SoundCues
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.render.Buzz
import com.quissicutdeus.sniddle.game.render.Camera
import com.quissicutdeus.sniddle.game.render.Fx
import com.quissicutdeus.sniddle.game.render.Haptics
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SteerInput
import com.quissicutdeus.sniddle.game.sim.Thumbstick

/**
 * One kid's place at the game: their own camera, effects, thumbstick, sounds
 * and buzz, and the piece of screen they're drawn on. A one-player game has
 * one seat filling the screen; two kids have a seat each, side by side.
 */
internal class Seat(
    val stick: Thumbstick,
) {
    val camera = Camera()
    val fx = Fx()
    val haptics = Haptics()
    val soundCues = SoundCues()
    val input = SteerInput()
    val adapter = ComposeCanvas2D()

    // The size of this seat's piece of screen, in pixels (0 until it's laid out).
    private var width = 0f
    private var height = 0f

    /** This seat's piece of screen is now [width] by [height] pixels. */
    fun resize(
        width: Int,
        height: Int,
    ) {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    /** The finger steering [stick], so a second finger can't make it jump. */
    private var pointer: PointerId? = null

    /** Whether the kid was alive last frame: coming back snaps the camera to the new spot. */
    private var wasAlive = true

    /**
     * Steers from this seat's fingers ([changes]): the first finger down
     * steers, and the stick starts where it lands. If that finger lifts while
     * another is still down, the other one takes over from where it is.
     */
    fun steerWith(changes: List<PointerInputChange>) {
        val current = changes.firstOrNull { it.id == pointer && it.pressed }
        if (current != null) {
            stick.move(current.position.x, current.position.y)
            return
        }
        val next = changes.firstOrNull { it.pressed }
        pointer = next?.id
        if (next == null) stick.release() else stick.press(next.position.x, next.position.y)
    }

    /** Turns the thumbstick into this frame's steering for [kid]. */
    fun steer(
        kid: Snail,
        gameOver: Boolean,
    ) {
        if (stick.down && kid.alive && !gameOver) {
            // Inside the dead zone the heading stays as it was.
            stick.heading()?.let { input.set(it) }
        } else {
            input.release()
        }
    }

    /** The camera follows [kid], and jumps straight to them when they come back after being gobbled. */
    fun follow(
        kid: Snail,
        renderAlpha: Float,
        dt: Float,
    ) {
        if (width <= 0f) return
        if (kid.alive && !wasAlive) {
            camera.snapTo(kid.x, kid.y, kid.radius, width, height)
        } else {
            camera.followSnail(kid, renderAlpha, width, height, dt)
        }
        wasAlive = kid.alive
    }
}

/**
 * The frame loop of a game, for one kid or two: every frame it steers each
 * [Seat]'s snail, advances the world once, and runs each seat's effects,
 * sounds, buzz and camera. It also keeps the music going while the game
 * runs, and hands on the result ([makeResult]) when the game ends. Not
 * Compose state: all of this changes every frame.
 *
 * [seats] are in [GameSim.players] order. [overDelaySeconds] is how long the
 * world carries on after the game is over before the result is handed on.
 */
internal class GameLoop<R : Any>(
    val sim: GameSim,
    private val seats: List<Seat>,
    private val overDelaySeconds: Float,
    private val makeResult: () -> R,
) {
    private val inputs = seats.map { it.input }

    /** This frame's sounds, each once whoever they're for (refilled every frame). */
    private val sounds = ArrayList<Sound>()

    /** Seconds played (not counting pauses): the clock the art animates by. */
    var time = 0f
        private set
    private var overTimer = 0f
    private var result: R? = null
    private var reported = false

    /** Whether the music was last told to play. */
    private var musicOn = false

    /**
     * One frame: steer each kid from their own thumbstick, advance the world
     * once, then each seat's effects, sounds and camera. Starts this frame's
     * sounds with [onSound] and returns the strongest buzz, if any.
     */
    fun step(
        dt: Float,
        paused: Boolean,
        onSound: (Sound) -> Unit,
    ): Buzz? {
        var buzz: Buzz? = null
        val kids = sim.players
        val seated = minOf(seats.size, kids.size)
        if (!paused) {
            time += dt
            for (i in 0 until seated) seats[i].steer(kids[i], sim.gameOver)
            sim.update(dt, inputs)
            sounds.clear()
            for (i in 0 until seated) {
                val seat = seats[i]
                val kid = kids[i]
                seat.fx.onEvents(sim, sim.events, kid)
                seat.fx.update(dt)
                val b = seat.haptics.onEvents(sim.events, kid.id, dt)
                if (b != null && (buzz == null || b > buzz)) buzz = b
                val cues = seat.soundCues.onEvents(sim.events, kid, dt)
                for (k in cues.indices) if (cues[k] !in sounds) sounds += cues[k]
            }
            for (k in sounds.indices) onSound(sounds[k])
            if (sim.gameOver) overTimer += dt
        }
        for (i in 0 until seated) seats[i].follow(kids[i], sim.renderAlpha, if (paused) 0f else dt)
        return buzz
    }

    /** Ends the game early (bedtime) by handing [report] how it went, unless it's already over and reported. */
    fun stop(report: (R) -> Unit) {
        if (reported) return
        reported = true
        report(result ?: makeResult().also { result = it })
    }

    /** Once the game is over: the result is taken at once, and handed to [onGameOver] after [overDelaySeconds]. */
    fun finish(onGameOver: (R) -> Unit) {
        if (!sim.gameOver) return
        val r = result ?: makeResult().also { result = it }
        if (!reported && overTimer > overDelaySeconds) {
            reported = true
            onGameOver(r)
        }
    }

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
}

/** What a game screen hears back from its [GameLoop]. */
internal class GameLoopCallbacks<R>(
    val onSound: (Sound) -> Unit,
    val onMusic: (Boolean) -> Unit,
    val onGameOver: (R) -> Unit,
    /** Pause (true) or carry on: the app went to the background, or the back button was pressed. */
    val onPausedChange: (Boolean) -> Unit,
    /** Anything else the screen does each frame, after the world has moved ([Float] is the frame time in seconds). */
    val onFrame: (Float) -> Unit,
)

/**
 * Runs [loop] every frame while its screen is showing, for either kind of
 * game: steps it, buzzes, counts the play towards bedtime ([nap]), hands on
 * the result and keeps the music right. Going to the background pauses, the
 * back button pauses and resumes, and leaving the screen stops the music.
 *
 * Returns a frame counter: reading it while drawing redraws every frame.
 */
@Composable
internal fun <R : Any> rememberGameFrames(
    loop: GameLoop<R>,
    paused: Boolean,
    buzzOn: Boolean,
    nap: BedtimeInGame<R>?,
    callbacks: GameLoopCallbacks<R>,
): () -> Int {
    var frame by remember { mutableIntStateOf(0) }
    val isPaused by rememberUpdatedState(paused)
    val buzz by rememberUpdatedState(buzzOn)
    val current by rememberUpdatedState(callbacks)
    val currentNap by rememberUpdatedState(nap)
    val view = LocalView.current

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { current.onPausedChange(true) }
    BackHandler { current.onPausedChange(!isPaused) }

    // Leaving the game (quit, game over, another app) always stops the music.
    DisposableEffect(loop) { onDispose { current.onMusic(false) } }

    LaunchedEffect(loop) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
                last = now
                val b = loop.step(dt, isPaused, current.onSound)
                if (b != null && buzz) view.performHapticFeedback(hapticFor(b))
                current.onFrame(dt)
                currentNap?.onFrame(loop.sim, dt, isPaused, loop::stop)
                loop.finish(current.onGameOver)
                loop.updateMusic(isPaused, current.onMusic)
                frame++
            }
        }
    }
    return { frame }
}

private const val NANOS_PER_SECOND = 1_000_000_000f

/** A frame never counts for more than this, however long the hitch. */
private const val MAX_FRAME_SECONDS = 0.1f

/**
 * System haptics: no permission needed, and they follow the phone's own
 * "vibration & haptics" setting, so turning that off silences the game too
 * (as does the game's own buzz switch).
 */
internal fun hapticFor(buzz: Buzz): Int =
    when (buzz) {
        Buzz.TAP -> HapticFeedbackConstants.CLOCK_TICK
        Buzz.THUD -> HapticFeedbackConstants.VIRTUAL_KEY
        Buzz.CHEER -> HapticFeedbackConstants.CONFIRM
        Buzz.OUCH -> HapticFeedbackConstants.LONG_PRESS
        Buzz.GOBBLED -> HapticFeedbackConstants.REJECT
    }
