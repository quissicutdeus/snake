package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.TWO_PI
import com.quissicutdeus.sniddle.game.model.TailPiece
import com.quissicutdeus.sniddle.game.model.World
import kotlin.math.min
import kotlin.random.Random

/** The snail that ate the player, as it looked then. */
data class Eater(
    val name: String,
    val style: SnailStyle,
)

/** Knobs mostly useful for tests and previews. */
data class SimOptions(
    val width: Float = GameConfig.ARENA_WIDTH,
    val height: Float = GameConfig.ARENA_HEIGHT,
    val pelletCount: Int = GameConfig.PELLET_COUNT,
    val aiCount: Int = GameConfig.AI_TARGET_COUNT,
    val spawnPlayer: Boolean = true,
    /** When false, AI snails just keep going straight (for rule tests). */
    val aiEnabled: Boolean = true,
    /** When false, no power-ups appear by themselves (for rule tests). */
    val powerUps: Boolean = true,
    /** Holidays that are on today: computer snails dress up for them. */
    val season: Set<Holiday> = emptySet(),
    /** How hard the game is (a saved game comes back as it was saved, whatever this says). */
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** What the arena floor looks like: grass, autumn leaves or snow. */
    val arena: ArenaSeason = ArenaSeason.GRASS,
)

/**
 * The whole game, independent of any UI. Advance it with [update] (real
 * elapsed time, internally split into fixed 60 Hz [step]s) or [step] directly.
 */
class GameSim(
    seed: Long = Random.nextLong(),
    playerStyle: SnailStyle = SnailStyle.DEFAULT,
    playerName: String = "You",
    internal val options: SimOptions = SimOptions(),
    /** Carry on a saved game: the player comes back as saved, in a freshly made world. */
    resume: SavedRun? = null,
    /** A second kid on the same screen makes this a two-player game (see [Together]). */
    secondPlayer: SecondPlayer? = null,
) {
    val rng = Random(seed)

    /** How hard this game is: as saved when carrying on a game, else from [options]. */
    val difficulty: Difficulty = resume?.difficulty ?: options.difficulty
    val world = World(options.width, options.height)
    internal val grid = SpatialGrid(options.width, options.height)

    /** Where the snails' heads are, rebuilt each step for eating (see [eatFood]). */
    internal val heads = HeadGrid(options.width, options.height)
    private val spawner = Spawner(options.aiCount)
    private val contacts = ArrayList<Contact>()
    private val headPairs = HashSet<Long>()
    private var nextId = 1
    private var accumulator = 0f

    /** Seconds until the next power-up appears. */
    internal var nextPowerUpIn = 0f

    /** Effects queue: filled by steps, cleared at the start of each [update]. */
    val events = ArrayList<GameEvent>()

    /** The player (the first kid, in a two-player game). */
    var player: Snail? = null
        private set

    /** Every player's snail, in the order they joined: just [player], or both kids. */
    val players: List<Snail> get() = playerList
    private val playerList = ArrayList<Snail>(2)

    /** The two-player game's clock and each kid's comings and goings; null in a one-player game. */
    val together: Together? = secondPlayer?.let { Together(it.seconds) }

    /** True once the player has been eaten. The world keeps running behind the game-over screen. */
    var gameOver = false
        internal set

    /** Who ate the player (kept from that moment: the eater may be gone by the time the game-over screen shows). */
    var eatenBy: Eater? = null
        internal set

    /** True while the player is the biggest snail in the arena (see [trackBiggest]). */
    var playerOnTop = false
        internal set

    /** Seconds the player has been the biggest snail this game. */
    var biggestSeconds = 0f
        internal set

    /** Seconds since the last "biggest snail" cheer. */
    var sinceBiggestCheer = Float.MAX_VALUE
        internal set

    init {
        repeat((options.pelletCount * difficulty.foodFactor).toInt()) { world.pellets.add(randomPellet()) }
        if (resume != null) {
            comeBack(resume)
        } else if (secondPlayer != null) {
            if (options.spawnPlayer) spawnTwoKids(playerName, playerStyle, secondPlayer)
        } else if (options.spawnPlayer) {
            addSnail(
                name = playerName,
                style = playerStyle,
                isPlayer = true,
                x = world.width / 2f,
                y = world.height / 2f,
                heading = rng.nextFloat() * TWO_PI,
                length = GameConfig.PLAYER_START_LENGTH,
            ).hearts = difficulty.hearts
        }
        spawner.fill(this)
        placeStartingPowerUps()
        grid.build(world.snails)
        // Already on top at the start (or on coming back) is no news: no cheer.
        playerOnTop = player?.let { isBiggest(it) } ?: false
        together?.let { t -> playerList.forEachIndexed { i, s -> t.kids[i].onTop = isBiggest(s) } }
    }

    /** The player's game so far, or null once it is over (nothing left to carry on). */
    fun snapshot(): SavedRun? {
        val p = player ?: return null
        // A two-player game isn't carried on: after Android closes the app, the kids start afresh.
        if (gameOver || !p.alive || together != null) return null
        return SavedRun(p.name, p.style, p.length, p.bestLength, p.hearts, p.snailsEaten, p.powerTime.toList(), biggestSeconds, difficulty)
    }

    /**
     * Brings the saved player back in the middle of the arena, coiled up so even
     * a very long snail fits, and briefly protected (it was paused a moment ago).
     */
    private fun comeBack(run: SavedRun) {
        val cx = world.width / 2f
        val cy = world.height / 2f
        val s = addSnail(run.name, run.style, isPlayer = true, x = cx, y = cy, heading = 0f, length = run.length)
        s.bestLength = maxOf(run.bestLength, s.length)
        run.powers.forEachIndexed { i, t -> s.powerTime[i] = t }
        s.hearts = run.hearts
        s.snailsEaten = run.snailsEaten
        s.invulnerable = GameConfig.RESUME_INVULNERABLE_SECONDS
        biggestSeconds = run.biggestSeconds
        val coil = Coil.around(cx, cy, s.radius, s.length + GameConfig.TRAIL_SPACING * 2)
        s.setPose(coil.xs, coil.ys, coil.count, coil.heading)
    }

    /** Advances by [elapsedSeconds] of real time. Returns the number of steps run. */
    fun update(
        elapsedSeconds: Float,
        input: SteerInput,
    ): Int {
        oneInput[0] = input
        return update(elapsedSeconds, oneInput)
    }

    /** Like [update], with one [SteerInput] per player (in [players] order). */
    fun update(
        elapsedSeconds: Float,
        inputs: List<SteerInput?>,
    ): Int {
        events.clear()
        accumulator = min(accumulator + elapsedSeconds, GameConfig.STEP * GameConfig.MAX_STEPS_PER_FRAME)
        var steps = 0
        while (accumulator >= GameConfig.STEP) {
            step(inputs)
            accumulator -= GameConfig.STEP
            steps++
        }
        return steps
    }

    /** How far (0..1) real time is into the next step; the renderer blends by this. */
    val renderAlpha: Float get() = accumulator / GameConfig.STEP

    /** One fixed 1/60 s step. */
    fun step(input: SteerInput? = null) {
        oneInput[0] = input
        step(oneInput)
    }

    /** One-player steering, reused so a step allocates nothing. */
    private val oneInput = arrayListOf<SteerInput?>(null)

    /** One fixed 1/60 s step, with one [SteerInput] per player (in [players] order). */
    fun step(inputs: List<SteerInput?>) {
        val dt = GameConfig.STEP
        if (events.size > MAX_QUEUED_EVENTS) events.subList(0, MAX_QUEUED_EVENTS / 2).clear()
        for (s in world.snails) s.rememberPrevious()

        // 1. Steer and move everybody.
        for (s in world.snails) {
            if (!s.alive) continue
            val desired =
                when {
                    s.isPlayer -> steeredHeading(s, inputs)
                    options.aiEnabled -> Ai.desiredHeading(this, s, dt)
                    else -> Float.NaN
                }
            Movement.steer(s, desired, dt)
            if (Movement.advance(s, world, dt) && s.isPlayer) emit(GameEvent.HitWall(s.id, s.x, s.y))
            Movement.tickTimers(s, dt)
        }

        // 2. Food and power-ups.
        eatFood(dt)
        updatePowerUps(dt)

        // 3. Snail vs snail.
        grid.build(world.snails)
        Collisions.findContacts(world.snails, grid, contacts)
        headPairs.clear()
        for (c in contacts) resolveContact(c)

        // 4. Clean up and refill.
        world.snails.removeAll { !it.alive && !it.isPlayer }
        spawner.update(this, dt)
        trackBiggest(dt)
        together?.let { tickTogether(it, dt) }
    }

    fun addSnail(
        name: String,
        style: SnailStyle,
        isPlayer: Boolean,
        x: Float,
        y: Float,
        heading: Float,
        length: Float,
    ): Snail {
        val s = Snail(nextId++, name, style, isPlayer, x, y, heading, length)
        if (isPlayer) {
            playerList.add(s)
            if (player == null) player = s
        } else {
            s.brain = AiBrain(rng)
        }
        s.layStraightBody()
        world.snails.add(s)
        return s
    }

    /**
     * Snails sorted biggest first (live "Biggest snails" list). Drawn every
     * frame, so the same list is refilled each time: read it before asking again.
     */
    fun leaderboard(): List<Snail> {
        board.clear()
        for (i in world.snails.indices) world.snails[i].let { if (it.alive) board.add(it) }
        board.sortWith(BIGGEST_FIRST)
        return board
    }

    private val board = ArrayList<Snail>()

    /** The longest an AI snail may grow right now. */
    fun aiMaxLength(): Float {
        // Two kids: measured against the bigger one, so there's still a snail to beat.
        val ref = (if (together != null) playerList.maxOfOrNull { it.length } else player?.length) ?: GameConfig.PLAYER_START_LENGTH
        return ref * GameConfig.AI_MAX_LENGTH_RATIO
    }

    /** Grows [s] by [amount]; AI snails never grow past [aiMaxLength]. */
    internal fun feed(
        s: Snail,
        amount: Float,
    ) {
        if (s.isPlayer) {
            s.grow(amount)
            return
        }
        val room = aiMaxLength() - s.length
        if (room > 0f) s.grow(kotlin.math.min(amount, room))
    }

    internal fun emit(e: GameEvent) {
        events.add(e)
    }

    /** True the first time a head-to-head pair is seen this step (so it's resolved once). */
    internal fun markHeadPair(
        a: Snail,
        b: Snail,
    ): Boolean {
        val lo = min(a.id, b.id).toLong()
        val hi = maxOf(a.id, b.id).toLong()
        return headPairs.add((lo shl Int.SIZE_BITS) or hi)
    }

    internal fun addTailPiece(p: TailPiece) {
        world.tailPieces.add(p)
        if (world.tailPieces.size > GameConfig.MAX_TAIL_PIECES) {
            world.tailPieces.removeAt(0).gone = true
        }
    }

    companion object {
        /** If nobody drains [events] (tests, previews), old ones are dropped past this many. */
        private const val MAX_QUEUED_EVENTS = 1000

        private val BIGGEST_FIRST = compareByDescending<Snail> { it.length }.thenBy { it.id }
    }
}
