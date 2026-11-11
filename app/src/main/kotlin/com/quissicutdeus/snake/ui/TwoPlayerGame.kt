package com.quissicutdeus.snake.ui

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import com.quissicutdeus.snake.ComposeCanvas2D
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.SoundCues
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.render.Buzz
import com.quissicutdeus.snake.game.render.Camera
import com.quissicutdeus.snake.game.render.Fx
import com.quissicutdeus.snake.game.render.Haptics
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SecondPlayer
import com.quissicutdeus.snake.game.sim.SimOptions
import com.quissicutdeus.snake.game.sim.SteerInput
import com.quissicutdeus.snake.game.sim.Thumbstick

/** How one kid's two-player game went, for the end screen, the high scores and their unlocks. */
data class KidResult(
    /** Their player id on this phone, or [GUEST_ID]. */
    val playerId: Int,
    val name: String,
    val style: SnakeStyle,
    /** Best length this game (through any comebacks). */
    val score: Int,
    val snakesEaten: Int,
    val biggestSeconds: Int,
    val timesGobbled: Int,
) {
    /** The same as a one-player [GameResult], for the kid's unlocks. */
    fun asGameResult(timestamp: Long) = GameResult(score, snakesEaten, style, timestamp, biggestSeconds = biggestSeconds)
}

/** How a two-player game went: the left kid first. */
data class TogetherResult(
    val kids: List<KidResult>,
    val timestamp: Long,
)

/** The id of a "Player 2" who isn't one of the kids on this phone. */
const val GUEST_ID = -1

/**
 * Two kids play at normal difficulty. Hook for little kid mode: once kids
 * have their own difficulty, use the easier one here only when both kids
 * have it switched on.
 */
internal fun twoPlayerOptions() = SimOptions(season = holidaysToday())

/** After the clock runs out, "Time's up!" shows this long before the end screen. */
private const val TIME_UP_DELAY_SECONDS = 2f

/** One kid's half of the screen: their own camera, effects, thumbstick, sounds and buzz. */
internal class Half(
    val stick: Thumbstick,
) {
    val camera = Camera()
    val fx = Fx()
    val haptics = Haptics()
    val soundCues = SoundCues()
    val input = SteerInput()
    val adapter = ComposeCanvas2D()

    /** The finger steering [stick], so a second finger in the same half can't make it jump. */
    var pointer: PointerId? = null
    var width = 0f
    var height = 0f

    /** Whether the kid was alive last frame: coming back snaps the camera to the new spot. */
    var wasAlive = true
}

/**
 * Mutable per-game objects the frame loop works on (not Compose state). Not
 * saved: if Android closes the app in the background during a two-player
 * game, the kids come back to the title screen (see the app's screen saver).
 */
internal class TwoPlayerGame(
    /** Left kid first. */
    val kids: List<PlayerCard>,
    newStick: () -> Thumbstick,
    seconds: Float,
) {
    val sim =
        GameSim(
            playerStyle = kids[0].style,
            playerName = kids[0].label,
            options = twoPlayerOptions(),
            secondPlayer = SecondPlayer(kids[1].label, kids[1].style, seconds),
        )
    val halves = List(2) { Half(newStick()) }
    private val inputs = halves.map { it.input }
    var time = 0f
        private set
    private var overTimer = 0f
    private var result: TogetherResult? = null
    private var reported = false

    /** Which half each finger started in: it steers that half's snake even if it drifts across. */
    private val owner = HashMap<PointerId, Int>()

    /** Whether the music was last told to play. */
    private var musicOn = false

    /** Seconds left on the shared clock. */
    val timeLeft: Float get() = sim.together!!.timeLeft

    private fun result(): TogetherResult {
        val together = sim.together!!
        val kidResults =
            kids.mapIndexed { i, card ->
                val s = sim.players[i]
                val state = together.kids[i]
                KidResult(card.id, card.label, card.style, s.score, s.snakesEaten, state.biggestSeconds.toInt(), state.timesGobbled)
            }
        return TogetherResult(kidResults, System.currentTimeMillis())
    }

    /** Once the clock has run out: the result is taken at once, and handed on after "Time's up!". */
    fun finish(onGameOver: (TogetherResult) -> Unit) {
        if (!sim.gameOver) return
        val r = result ?: result().also { result = it }
        if (!reported && overTimer > TIME_UP_DELAY_SECONDS) {
            reported = true
            onGameOver(r)
        }
    }

    /** Music plays while the game is running, and stops when it's paused or over. */
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

    /**
     * One frame for both kids: steer each from their own thumbstick, advance
     * the world once, then each half's effects, sounds and camera. Starts this
     * frame's sounds with [onSound] (each sound once, whoever it's for) and
     * returns the stronger of the two kids' buzzes, if any.
     */
    fun step(
        dt: Float,
        paused: Boolean,
        onSound: (Sound) -> Unit,
    ): Buzz? {
        var buzz: Buzz? = null
        if (!paused) {
            time += dt
            halves.forEachIndexed { i, half -> steer(half, sim.players[i]) }
            sim.update(dt, inputs)
            val sounds = LinkedHashSet<Sound>()
            halves.forEachIndexed { i, half ->
                val kid = sim.players[i]
                half.fx.onEvents(sim, sim.events, kid)
                half.fx.update(dt)
                val b = half.haptics.onEvents(sim.events, kid.id, dt)
                if (b != null && (buzz == null || b > buzz)) buzz = b
                sounds += half.soundCues.onEvents(sim.events, kid, dt)
            }
            sounds.forEach(onSound)
            if (sim.gameOver) overTimer += dt
        }
        halves.forEachIndexed { i, half -> follow(half, sim.players[i], if (paused) 0f else dt) }
        return buzz
    }

    private fun steer(
        half: Half,
        kid: Snake,
    ) {
        if (half.stick.down && kid.alive && !sim.gameOver) {
            // Inside the dead zone the heading stays as it was.
            half.stick.heading()?.let { half.input.set(it) }
        } else {
            half.input.release()
        }
    }

    /** The half's camera follows its kid, and jumps straight to them when they come back. */
    private fun follow(
        half: Half,
        kid: Snake,
        dt: Float,
    ) {
        if (half.width <= 0f) return
        if (kid.alive && !half.wasAlive) {
            half.camera.snapTo(kid.x, kid.y, kid.radius, half.width, half.height)
        } else {
            half.camera.followSnake(kid, sim.renderAlpha, half.width, half.height, dt)
        }
        half.wasAlive = kid.alive
    }

    /**
     * Hands each finger to the half it started in ([width] is the whole width
     * in pixels), then steers each half like the one-player game: the first
     * finger down steers, and if it lifts while another one in that half is
     * still down, that one takes over.
     */
    fun steerHalves(
        changes: List<PointerInputChange>,
        width: Int,
    ) {
        for (c in changes) {
            if (c.pressed) owner.getOrPut(c.id) { if (c.position.x < width / 2f) 0 else 1 }
        }
        halves.forEachIndexed { i, half -> steerHalf(half, changes.filter { owner[it.id] == i }) }
        for (c in changes) if (!c.pressed) owner.remove(c.id)
    }

    private fun steerHalf(
        half: Half,
        changes: List<PointerInputChange>,
    ) {
        val stick = half.stick
        val current = changes.firstOrNull { it.id == half.pointer && it.pressed }
        if (current != null) {
            stick.move(current.position.x, current.position.y)
            return
        }
        val next = changes.firstOrNull { it.pressed }
        half.pointer = next?.id
        if (next == null) stick.release() else stick.press(next.position.x, next.position.y)
    }
}
