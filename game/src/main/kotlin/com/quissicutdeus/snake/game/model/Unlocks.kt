package com.quissicutdeus.snake.game.model

import kotlin.random.Random

/** Lifetime totals on this device, which unlock new looks. */
data class Stats(
    val bestScore: Int = 0,
    val snakesEaten: Int = 0,
    val gamesPlayed: Int = 0,
) {
    /** Stats after one more finished game. */
    fun after(
        score: Int,
        eaten: Int,
    ) = Stats(maxOf(bestScore, score), snakesEaten + eaten, gamesPlayed + 1)

    fun encode(): String = "$bestScore,$snakesEaten,$gamesPlayed"

    companion object {
        /** Reads [encode] output; null if unreadable. */
        fun decode(text: String?): Stats? {
            val p = text?.split(',')?.map { it.trim().toIntOrNull() ?: return null } ?: return null
            if (p.size != FIELDS || p.any { it < 0 }) return null
            return Stats(p[0], p[1], p[2])
        }

        private const val FIELDS = 3
    }
}

/** What a locked look asks for. */
enum class GoalKind(
    private val text: String,
) {
    LENGTH("Reach length %d"),
    SNAKES("Eat %d snakes"),
    GAMES("Play %d games"),
    ;

    fun describe(amount: Int) = text.format(amount)
}

/** One look ([item]: a [Hat], [Eyes] or [Pattern]) that unlocks at a goal. */
data class Unlock(
    val item: Enum<*>,
    private val kind: GoalKind,
    private val amount: Int,
) {
    val goal: String get() = kind.describe(amount)

    fun reachedBy(stats: Stats): Boolean =
        when (kind) {
            GoalKind.LENGTH -> stats.bestScore >= amount
            GoalKind.SNAKES -> stats.snakesEaten >= amount
            GoalKind.GAMES -> stats.gamesPlayed >= amount
        }
}

/**
 * Looks that are earned by playing. Everything not listed here is always
 * available. Computer snakes may wear locked looks (something to aim for).
 */
object Unlocks {
    private val ALL =
        listOf(
            Unlock(Hat.TOP_HAT, GoalKind.LENGTH, 100),
            Unlock(Pattern.STARS, GoalKind.GAMES, 10),
            Unlock(Eyes.HEARTS, GoalKind.SNAKES, 10),
            Unlock(Hat.WIZARD, GoalKind.LENGTH, 200),
            Unlock(Hat.HORNS, GoalKind.SNAKES, 40),
        )

    /** The unlock rule for [item], or null if it's always available. */
    fun ruleFor(item: Enum<*>): Unlock? = ALL.firstOrNull { it.item == item }

    fun isUnlocked(
        item: Enum<*>,
        stats: Stats,
    ): Boolean = ruleFor(item)?.reachedBy(stats) ?: true

    /** Looks that [after] has unlocked and [before] hadn't, in [ALL] order. */
    fun newlyUnlocked(
        before: Stats,
        after: Stats,
    ): List<Unlock> = ALL.filter { it.reachedBy(after) && !it.reachedBy(before) }

    /** A random look using only what [stats] has unlocked ("Surprise me!"). */
    fun randomStyle(
        rng: Random,
        stats: Stats,
    ): SnakeStyle {
        val s = SnakeStyle.random(rng)

        fun <T : Enum<*>> pick(
            current: T,
            all: List<T>,
        ): T = if (isUnlocked(current, stats)) current else all.filter { isUnlocked(it, stats) }.random(rng)
        return s.copy(
            pattern = pick(s.pattern, Pattern.entries),
            eyes = pick(s.eyes, Eyes.entries),
            hat = pick(s.hat, Hat.entries),
        )
    }
}
