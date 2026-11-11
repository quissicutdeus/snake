package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Food
import com.quissicutdeus.snake.game.model.Pellet
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.TWO_PI
import com.quissicutdeus.snake.game.model.TailPiece
import com.quissicutdeus.snake.game.model.World
import com.quissicutdeus.snake.game.model.dist2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** The snake that ate the player, as it looked then. */
data class Eater(
    val name: String,
    val style: SnakeStyle,
)

/** Knobs mostly useful for tests and previews. */
data class SimOptions(
    val width: Float = GameConfig.ARENA_WIDTH,
    val height: Float = GameConfig.ARENA_HEIGHT,
    val pelletCount: Int = GameConfig.PELLET_COUNT,
    val aiCount: Int = GameConfig.AI_TARGET_COUNT,
    val spawnPlayer: Boolean = true,
    /** When false, AI snakes just keep going straight (for rule tests). */
    val aiEnabled: Boolean = true,
    /** When false, no power-ups appear by themselves (for rule tests). */
    val powerUps: Boolean = true,
)

/**
 * The whole game, independent of any UI. Advance it with [update] (real
 * elapsed time, internally split into fixed 60 Hz [step]s) or [step] directly.
 */
class GameSim(
    seed: Long = Random.nextLong(),
    playerStyle: SnakeStyle = SnakeStyle.DEFAULT,
    playerName: String = "You",
    internal val options: SimOptions = SimOptions(),
    /** Carry on a saved game: the player comes back as saved, in a freshly made world. */
    resume: SavedRun? = null,
) {
    val rng = Random(seed)
    val world = World(options.width, options.height)
    internal val grid = SpatialGrid(options.width, options.height)
    private val heads = HeadGrid(options.width, options.height)
    private val spawner = Spawner(options.aiCount)
    private val contacts = ArrayList<Contact>()
    private val headPairs = HashSet<Long>()
    private var nextId = 1
    private var accumulator = 0f

    /** Seconds until the next power-up appears. */
    internal var nextPowerUpIn = 0f

    /** Effects queue: filled by steps, cleared at the start of each [update]. */
    val events = ArrayList<GameEvent>()

    var player: Snake? = null
        private set

    /** True once the player has been eaten. The world keeps running behind the game-over screen. */
    var gameOver = false
        internal set

    /** Who ate the player (kept from that moment: the eater may be gone by the time the game-over screen shows). */
    var eatenBy: Eater? = null
        internal set

    init {
        repeat(options.pelletCount) { world.pellets.add(randomPellet()) }
        if (resume != null) {
            comeBack(resume)
        } else if (options.spawnPlayer) {
            addSnake(
                name = playerName,
                style = playerStyle,
                isPlayer = true,
                x = world.width / 2f,
                y = world.height / 2f,
                heading = rng.nextFloat() * TWO_PI,
                length = GameConfig.PLAYER_START_LENGTH,
            )
        }
        spawner.fill(this)
        placeStartingPowerUps()
        grid.build(world.snakes)
    }

    /** The player's game so far, or null once it is over (nothing left to carry on). */
    fun snapshot(): SavedRun? {
        val p = player ?: return null
        if (gameOver || !p.alive) return null
        return SavedRun(p.name, p.style, p.length, p.bestLength, p.hearts, p.snakesEaten, p.powerTime.toList())
    }

    /**
     * Brings the saved player back in the middle of the arena, coiled up so even
     * a very long snake fits, and briefly protected (it was paused a moment ago).
     */
    private fun comeBack(run: SavedRun) {
        val cx = world.width / 2f
        val cy = world.height / 2f
        val s = addSnake(run.name, run.style, isPlayer = true, x = cx, y = cy, heading = 0f, length = run.length)
        s.bestLength = maxOf(run.bestLength, s.length)
        run.powers.forEachIndexed { i, t -> s.powerTime[i] = t }
        s.hearts = run.hearts
        s.snakesEaten = run.snakesEaten
        s.invulnerable = GameConfig.RESUME_INVULNERABLE_SECONDS
        val coil = Coil.around(cx, cy, s.radius, s.length + GameConfig.TRAIL_SPACING * 2)
        s.setPose(coil.xs, coil.ys, coil.count, coil.heading)
    }

    /** Advances by [elapsedSeconds] of real time. Returns the number of steps run. */
    fun update(
        elapsedSeconds: Float,
        input: SteerInput,
    ): Int {
        events.clear()
        accumulator = min(accumulator + elapsedSeconds, GameConfig.STEP * GameConfig.MAX_STEPS_PER_FRAME)
        var steps = 0
        while (accumulator >= GameConfig.STEP) {
            step(input)
            accumulator -= GameConfig.STEP
            steps++
        }
        return steps
    }

    /** How far (0..1) real time is into the next step; the renderer blends by this. */
    val renderAlpha: Float get() = accumulator / GameConfig.STEP

    /** One fixed 1/60 s step. */
    fun step(input: SteerInput? = null) {
        val dt = GameConfig.STEP
        world.time += dt
        if (events.size > MAX_QUEUED_EVENTS) events.subList(0, MAX_QUEUED_EVENTS / 2).clear()
        for (s in world.snakes) s.rememberPrevious()

        // 1. Steer and move everybody.
        for (s in world.snakes) {
            if (!s.alive) continue
            val desired =
                when {
                    s.isPlayer -> if (input != null && input.active) input.heading else Float.NaN
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

        // 3. Snake vs snake.
        grid.build(world.snakes)
        Collisions.findContacts(world.snakes, grid, contacts)
        headPairs.clear()
        for (c in contacts) resolveContact(c)

        // 4. Clean up and refill.
        world.snakes.removeAll { !it.alive && !it.isPlayer }
        spawner.update(this, dt)
    }

    fun addSnake(
        name: String,
        style: SnakeStyle,
        isPlayer: Boolean,
        x: Float,
        y: Float,
        heading: Float,
        length: Float,
    ): Snake {
        val s = Snake(nextId++, name, style, isPlayer, x, y, heading, length)
        if (isPlayer) player = s else s.brain = AiBrain(rng)
        s.layStraightBody()
        world.snakes.add(s)
        return s
    }

    /** Snakes sorted biggest first (live "Biggest snakes" list). */
    fun leaderboard(): List<Snake> = world.snakes.filter { it.alive }.sortedWith(compareByDescending<Snake> { it.length }.thenBy { it.id })

    /** The longest an AI snake may grow right now. */
    fun aiMaxLength(): Float {
        val ref = player?.length ?: GameConfig.PLAYER_START_LENGTH
        return ref * GameConfig.AI_MAX_LENGTH_RATIO
    }

    /** Grows [s] by [amount]; AI snakes never grow past [aiMaxLength]. */
    internal fun feed(
        s: Snake,
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
        a: Snake,
        b: Snake,
    ): Boolean {
        val lo = min(a.id, b.id).toLong()
        val hi = maxOf(a.id, b.id).toLong()
        return headPairs.add((lo shl Int.SIZE_BITS) or hi)
    }

    internal fun pickAiName(): String {
        val used =
            world.snakes
                .filter { it.alive }
                .map { it.name }
                .toSet()
        val free = AI_NAMES.filter { it !in used }
        return if (free.isNotEmpty()) free[rng.nextInt(free.size)] else AI_NAMES[rng.nextInt(AI_NAMES.size)]
    }

    internal fun addTailPiece(p: TailPiece) {
        world.tailPieces.add(p)
        if (world.tailPieces.size > GameConfig.MAX_TAIL_PIECES) {
            world.tailPieces.removeAt(0).gone = true
        }
    }

    private fun randomPellet(): Pellet {
        val big = rng.nextFloat() < GameConfig.BIG_PELLET_CHANCE
        val margin = 40f
        return Pellet(
            x = margin + rng.nextFloat() * (world.width - 2 * margin),
            y = margin + rng.nextFloat() * (world.height - 2 * margin),
            value = if (big) GameConfig.BIG_PELLET_VALUE else GameConfig.PELLET_VALUE,
            radius = if (big) GameConfig.BIG_PELLET_RADIUS else GameConfig.PELLET_RADIUS,
            color = PELLET_COLORS[rng.nextInt(PELLET_COLORS.size)],
            phase = rng.nextFloat() * TWO_PI,
        )
    }

    private fun eatFood(dt: Float) {
        heads.build(world.snakes, foodReach(dt))
        val pellets = world.pellets
        for (i in pellets.indices) {
            val p = pellets[i]
            p.age += dt
            if (!eatenNow(p, dt)) continue
            p.gone = true
            emit(GameEvent.AteFood(p.x, p.y, p.color))
            pellets[i] = randomPellet()
        }
        world.tailPieces.removeAll { updateTailPiece(it, dt) }
    }

    /** Ages a dropped tail piece and lets snakes eat it. Returns true when it should go away. */
    private fun updateTailPiece(
        t: TailPiece,
        dt: Float,
    ): Boolean {
        t.age += dt
        val expired = t.age > GameConfig.TAIL_PIECE_LIFETIME
        val eaten = !expired && eatenNow(t, dt)
        if (eaten) emit(GameEvent.AteFood(t.x, t.y, t.style.bodyColor))
        t.gone = expired || eaten
        return t.gone
    }

    /**
     * True if a snake's head reaches [food] and eats it; snakes close by pull it
     * nearer. Snakes are tried in list order (the first one to reach it eats
     * it), skipping only those too far away for [tryEat] to do anything.
     */
    private fun eatenNow(
        food: Food,
        dt: Float,
    ): Boolean {
        val cell = heads.cellAt(food.x, food.y)
        for (e in heads.startOf(cell) until heads.startOf(cell + 1)) {
            val s = world.snakes[heads.snakeIndexAt(e)]
            if (s.alive && tryEat(s, food, dt)) return true
        }
        return false
    }

    /**
     * Beyond this distance from a head, [tryEat] can neither eat nor pull any
     * food this step: the furthest any snake reaches for the biggest food,
     * plus how far food may already have been pulled by the snakes tried
     * before it (each pull moves it at most SUCTION_SPEED * dt).
     */
    private fun foodReach(dt: Float): Float {
        var foodR = 0f
        for (p in world.pellets) foodR = maxOf(foodR, p.radius)
        for (t in world.tailPieces) foodR = maxOf(foodR, t.radius)
        val grab = maxOf(GameConfig.MAX_RADIUS + GameConfig.EAT_MARGIN, 2f * GameConfig.MAX_RADIUS + GameConfig.SUCTION_RANGE)
        val drift = world.snakes.size * GameConfig.SUCTION_SPEED * dt
        return grab + foodR + drift + 1f // + 1: slack for float rounding
    }

    /** Eats the food if the head touches it; otherwise pulls nearby food a little closer. */
    private fun tryEat(
        s: Snake,
        food: Food,
        dt: Float,
    ): Boolean {
        val r = s.radius
        val d2 = dist2(s.x, s.y, food.x, food.y)
        val eatR = r + food.radius + GameConfig.EAT_MARGIN
        if (d2 < eatR * eatR) {
            feed(s, food.value)
            s.sinceAte = 0f
            return true
        }
        val suck = r * 2f + GameConfig.SUCTION_RANGE + food.radius
        if (d2 < suck * suck) {
            val d = sqrt(d2)
            val step = min(d, GameConfig.SUCTION_SPEED * dt)
            food.x += (s.x - food.x) / d * step
            food.y += (s.y - food.y) / d * step
        }
        return false
    }

    companion object {
        /** If nobody drains [events] (tests, previews), old ones are dropped past this many. */
        private const val MAX_QUEUED_EVENTS = 1000

        private val AI_NAMES =
            listOf(
                "Noodle",
                "Sir Hiss",
                "Wiggles",
                "Slinky",
                "Zippy",
                "Captain Coil",
                "Spaghetti",
                "Squiggle",
                "Lord Wiggleton",
                "Twisty",
                "Pickle",
                "Sprinkles",
                "Doodle",
                "Mr. Snoot",
                "Lady Slither",
                "Jellybean",
                "Pretzel",
                "Waffles",
                "Bean Sprout",
                "Ziggy",
                "Professor Scales",
                "Bubbles",
                "Sir Noodles",
                "Taco",
                "Marshmallow",
                "Shoelace",
                "Gummy",
                "Princess Hiss",
                "Wobble",
                "Scooter",
                "Pudding",
                "Captain Zig",
                "Snickerdoodle",
                "Lil' Fang",
                "Mustard",
                "Tater Tot",
                "Rascal",
                "Biscuit",
            )

        /** Bright candy colors for food dots (ARGB). */
        private val PELLET_COLORS =
            intArrayOf(
                0xFFFF5252.toInt(),
                0xFFFFAB40.toInt(),
                0xFFFFEB3B.toInt(),
                0xFF69F0AE.toInt(),
                0xFF40C4FF.toInt(),
                0xFF7C4DFF.toInt(),
                0xFFFF4081.toInt(),
                0xFFFFFFFF.toInt(),
            )
    }
}
