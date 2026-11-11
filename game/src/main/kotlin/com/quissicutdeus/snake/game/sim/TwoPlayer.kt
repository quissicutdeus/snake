package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.dist2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max

/** The second kid in a two-player game, and how long the game lasts. */
data class SecondPlayer(
    val name: String,
    val style: SnakeStyle,
    val seconds: Float = GameConfig.TWO_PLAYER_SECONDS,
)

/**
 * A two-player game: two kids on one screen, each with their own snake in the
 * same arena. The usual rules hold between them (a bigger head gobbles a
 * smaller one), but a gobbled kid comes back [GameConfig.COME_BACK_SECONDS]
 * later as a fresh small snake instead of sitting out. The game ends on a
 * shared clock, so the younger kid plays exactly as long as the older one.
 */
class Together internal constructor(
    /** How long the whole game lasts. */
    seconds: Float,
) {
    /** Seconds left on the shared clock; the game is over at 0. */
    var timeLeft = seconds
        internal set

    /** Each kid's side of the game, in [GameSim.players] order. */
    val kids: List<KidState> = List(KIDS) { KidState() }

    companion object {
        const val KIDS = 2
    }
}

/** How one kid's two-player game is going. Their snake keeps its best length and snakes eaten through comebacks. */
class KidState internal constructor() {
    /** Seconds until a gobbled kid comes back; 0 while they're playing. */
    var backIn = 0f
        internal set

    /** Who gobbled the kid last (shown while they wait to come back). */
    var gobbledBy: Eater? = null
        internal set

    var timesGobbled = 0
        internal set

    /** True while this kid is the biggest snake in the arena (the other kid counts too). */
    var onTop = false
        internal set

    /** Seconds this kid has been the biggest snake this game. */
    var biggestSeconds = 0f
        internal set

    /** Seconds since this kid's last "biggest snake" cheer. */
    internal var sinceCheer = Float.MAX_VALUE
}

/** True while [s] is the biggest snake in the arena, as the game tracks it (see [trackBiggest]). */
fun GameSim.isOnTop(s: Snake): Boolean {
    val t = together ?: return s === player && playerOnTop
    return t.kids.getOrNull(players.indexOf(s))?.onTop ?: false
}

/**
 * Where player [s] is steered this step, or NaN to keep going. A single input
 * steers every player (one kid); otherwise each player has their own, in
 * [GameSim.players] order.
 */
internal fun GameSim.steeredHeading(
    s: Snake,
    inputs: List<SteerInput?>,
): Float {
    val input = if (inputs.size == 1) inputs[0] else inputs.getOrNull(players.indexOf(s))
    return if (input != null && input.active) input.heading else Float.NaN
}

/** Both kids side by side in the middle, heading north. */
internal fun GameSim.spawnTwoKids(
    firstName: String,
    firstStyle: SnakeStyle,
    second: SecondPlayer,
) {
    val cx = world.width / 2f
    val cy = world.height / 2f
    val north = (-PI / 2).toFloat()
    val gap = GameConfig.TWO_PLAYER_START_GAP
    val length = GameConfig.PLAYER_START_LENGTH
    addSnake(firstName, firstStyle, isPlayer = true, x = cx - gap, y = cy, heading = north, length = length).hearts = difficulty.hearts
    addSnake(second.name, second.style, isPlayer = true, x = cx + gap, y = cy, heading = north, length = length).hearts = difficulty.hearts
}

/** A kid was gobbled by [eater]: they'll be back soon (see [tickTogether]). */
internal fun GameSim.kidGobbled(
    t: Together,
    kid: Snake,
    eater: Snake,
) {
    val state = t.kids.getOrNull(players.indexOf(kid)) ?: return
    state.backIn = GameConfig.COME_BACK_SECONDS
    state.gobbledBy = Eater(eater.name, eater.style)
    state.timesGobbled++
    state.onTop = false
}

/** Runs the shared clock and brings gobbled kids back. The game is over when the clock runs out. */
internal fun GameSim.tickTogether(
    t: Together,
    dt: Float,
) {
    if (gameOver) return
    t.timeLeft = max(0f, t.timeLeft - dt)
    players.forEachIndexed { i, s ->
        val state = t.kids[i]
        if (s.alive || state.backIn <= 0f) return@forEachIndexed
        state.backIn -= dt
        if (state.backIn <= 0f) {
            state.backIn = 0f
            comeBackFresh(s)
        }
    }
    if (t.timeLeft <= 0f) gameOver = true
}

/** Per-kid "biggest snake" tracking, like the one-player [trackBiggest]: a cheer, the crown and the time on top. */
internal fun GameSim.trackBiggestTogether(
    t: Together,
    dt: Float,
) {
    players.forEachIndexed { i, s ->
        val state = t.kids[i]
        state.sinceCheer += dt
        if (!s.alive || gameOver) return@forEachIndexed
        val top = isBiggest(s)
        if (top) state.biggestSeconds += dt
        if (top && !state.onTop && state.sinceCheer >= GameConfig.BIGGEST_CHEER_GAP_SECONDS) {
            state.sinceCheer = 0f
            emit(GameEvent.BecameBiggest(s.x, s.y, s.id))
        }
        state.onTop = top
    }
}

/**
 * Brings a gobbled kid's snake back to life where nobody is close: start
 * length, full hearts, no powers, and blinking safe for a moment. It keeps
 * its best length and snakes eaten, which are the kid's score for the game.
 */
private fun GameSim.comeBackFresh(s: Snake) {
    val (x, y) = quietSpot()
    // Head towards the middle, so the body lies away from it and the kid sees the arena ahead.
    val heading = atan2(world.height / 2f - y, world.width / 2f - x)
    val start = GameConfig.PLAYER_START_LENGTH
    if (s.length > start) s.shrinkTo(start) else s.grow(start - s.length)
    s.setPose(floatArrayOf(x), floatArrayOf(y), 1, heading)
    s.layStraightBody()
    s.rememberPrevious()
    s.alive = true
    s.hearts = difficulty.hearts
    s.powerTime.fill(0f)
    s.steerLock = 0f
    s.knockback = 0f
    s.invulnerable = GameConfig.COME_BACK_INVULNERABLE_SECONDS
}

/** Of a few random spots away from the fence, the one furthest from any other snake's head. */
private fun GameSim.quietSpot(): Pair<Float, Float> {
    val margin = GameConfig.SPAWN_WALL_MARGIN
    var bestX = world.width / 2f
    var bestY = world.height / 2f
    var bestD = -1f
    repeat(QUIET_SPOT_TRIES) {
        val x = margin + rng.nextFloat() * (world.width - 2 * margin)
        val y = margin + rng.nextFloat() * (world.height - 2 * margin)
        var nearest = Float.MAX_VALUE
        for (o in world.snakes) if (o.alive) nearest = minOf(nearest, dist2(x, y, o.x, o.y))
        if (nearest > bestD) {
            bestD = nearest
            bestX = x
            bestY = y
        }
    }
    return bestX to bestY
}

/** Random spots tried when looking for a quiet place to come back. */
private const val QUIET_SPOT_TRIES = 24
