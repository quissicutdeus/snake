package com.quissicutdeus.sniddle.game.model

import kotlin.random.Random

/** Lifetime totals on this device, and the holidays played on, which unlock new looks. */
data class Stats(
    val bestScore: Int = 0,
    val snailsEaten: Int = 0,
    val gamesPlayed: Int = 0,
    val holidays: Set<Holiday> = emptySet(),
) {
    /** Stats after one more finished game, played while [season]'s holidays were on. */
    fun after(
        score: Int,
        eaten: Int,
        season: Set<Holiday> = emptySet(),
    ) = Stats(maxOf(bestScore, score), snailsEaten + eaten, gamesPlayed + 1, holidays + season)

    /** "120,11,10", plus ",CHRISTMAS+HALLOWEEN" once any holidays have been played on. */
    fun encode(): String {
        val base = "$bestScore,$snailsEaten,$gamesPlayed"
        return if (holidays.isEmpty()) base else base + "," + holidays.joinToString("+") { it.name }
    }

    companion object {
        /** Reads [encode] output; null if unreadable. Unknown holiday names are skipped. */
        fun decode(text: String?): Stats? {
            val p = text?.split(',') ?: return null
            if (p.size != FIELDS && p.size != FIELDS + 1) return null
            val n = p.take(FIELDS).map { it.trim().toIntOrNull() ?: return null }
            if (n.any { it < 0 }) return null
            val names =
                p
                    .getOrNull(FIELDS)
                    ?.split('+')
                    ?.map { it.trim() }
                    .orEmpty()
            return Stats(n[0], n[1], n[2], Holiday.entries.filterTo(LinkedHashSet()) { it.name in names })
        }

        private const val FIELDS = 3
    }
}

/** What a locked look asks for. */
enum class GoalKind(
    private val text: String,
) {
    LENGTH("Reach length %d"),
    SNAILS("Eat %d snails"),
    GAMES("Play %d games"),

    /** Play one game while a holiday is on; the holiday says when. */
    HOLIDAY("%d holiday game"),
    ;

    fun describe(amount: Int) = text.replace("%d", amount.toString())
}

/** One look ([item]: a [Hat], [Eyes] or [Pattern]) that unlocks at a goal. */
data class Unlock(
    val item: Enum<*>,
    private val kind: GoalKind,
    private val amount: Int,
) {
    /** The holiday this look belongs to, for a [GoalKind.HOLIDAY] goal. */
    private val holiday: Holiday? = if (kind == GoalKind.HOLIDAY) checkNotNull(Holiday.of(item)) { "$item isn't a holiday look" } else null

    /**
     * How this unlock is saved on the phone, e.g. "Hat.TOP_HAT". The type names are
     * written out by hand because the release build renames classes, so a class's
     * own name can change from one update to the next. It's worked out when the
     * unlock is made, so a new kind of look missing from here fails as soon as the
     * unlock list is built instead of saving a name that might not last.
     */
    val key: String =
        when (item) {
            is Hat -> "Hat"
            is Eyes -> "Eyes"
            is Pattern -> "Pattern"
            else -> error("$item has no saved name: add its type to Unlock.key")
        } + "." + item.name

    /** A holiday look, earned by the date rather than by playing a lot. */
    val isHoliday: Boolean get() = holiday != null

    val goal: String get() = holiday?.goal ?: kind.describe(amount)

    /** The number the goal asks for (100 for "Reach length 100"). */
    val target: Int get() = amount

    /** How far [stats] have got toward [target], never more than it. */
    fun progress(stats: Stats): Int =
        minOf(
            amount,
            when (kind) {
                GoalKind.LENGTH -> stats.bestScore
                GoalKind.SNAILS -> stats.snailsEaten
                GoalKind.GAMES -> stats.gamesPlayed
                GoalKind.HOLIDAY -> if (holiday in stats.holidays) amount else 0
            },
        )

    fun reachedBy(stats: Stats): Boolean = progress(stats) >= amount

    /** A cheery nudge saying how much is left ("3 more games to go!"), for tapping a locked look. */
    fun hint(stats: Stats): String {
        val left = amount - progress(stats)
        return when {
            left <= 0 -> "You did it!"
            holiday != null -> holiday.hint
            kind == GoalKind.LENGTH -> "$left more length to go!"
            kind == GoalKind.SNAILS -> "Eat $left more ${if (left == 1) "snail" else "snails"}!"
            else -> "$left more ${if (left == 1) "game" else "games"} to go!"
        }
    }
}

/**
 * Looks that are earned by playing. Everything not listed here is always
 * available. Computer snails may wear locked looks (something to aim for).
 */
object Unlocks {
    private val ALL =
        listOf(
            Unlock(Hat.TOP_HAT, GoalKind.LENGTH, 100),
            Unlock(Pattern.STARS, GoalKind.GAMES, 10),
            Unlock(Eyes.HEARTS, GoalKind.SNAILS, 10),
            Unlock(Hat.WIZARD, GoalKind.LENGTH, 200),
            Unlock(Hat.HORNS, GoalKind.SNAILS, 40),
            Unlock(Hat.PROPELLER, GoalKind.GAMES, 5),
            Unlock(Eyes.STARS, GoalKind.LENGTH, 150),
            Unlock(Hat.PIRATE, GoalKind.SNAILS, 25),
            Unlock(Eyes.SPARKLY, GoalKind.GAMES, 25),
        ) + Holiday.entries.flatMap { h -> h.looks.map { Unlock(it, GoalKind.HOLIDAY, 1) } }

    /** The unlock rule for [item], or null if it's always available. */
    fun ruleFor(item: Enum<*>): Unlock? = ALL.firstOrNull { it.item == item }

    fun isUnlocked(
        item: Enum<*>,
        stats: Stats,
    ): Boolean = ruleFor(item)?.reachedBy(stats) ?: true

    /** Every look that [stats] has unlocked, in [ALL] order. */
    fun reachedBy(stats: Stats): List<Unlock> = ALL.filter { it.reachedBy(stats) }

    /** Looks that [after] has unlocked and [before] hadn't, in [ALL] order. */
    fun newlyUnlocked(
        before: Stats,
        after: Stats,
    ): List<Unlock> = ALL.filter { it.reachedBy(after) && !it.reachedBy(before) }

    /** A random look using only what [stats] has unlocked ("Surprise me!"). */
    fun randomStyle(
        rng: Random,
        stats: Stats,
    ): SnailStyle {
        val s = SnailStyle.random(rng)

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
