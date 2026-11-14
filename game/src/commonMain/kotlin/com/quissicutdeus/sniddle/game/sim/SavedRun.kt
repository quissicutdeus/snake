package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.SnailStyle

/**
 * The player's side of a game in progress: enough to carry on after Android
 * closes the app in the background. Only the player's snail is kept; the world
 * around it (food, other snails) is made fresh when the game comes back.
 */
data class SavedRun(
    val name: String,
    val style: SnailStyle,
    val length: Float,
    val bestLength: Float,
    val hearts: Int,
    val snailsEaten: Int,
    /** Seconds left of each power, indexed by [PowerType.ordinal]. */
    val powers: List<Float>,
    /** Seconds spent as the biggest snail so far. */
    val biggestSeconds: Float = 0f,
    /** How hard the game is: it carries on just as hard. */
    val difficulty: Difficulty = Difficulty.NORMAL,
    /**
     * The floor the game was on: it carries on on the same one. Null for a
     * game saved before floors were picked per game, which comes back on
     * whatever floor the new game's options say (today's season's).
     */
    val arena: ArenaSeason? = null,
) {
    /** A short text form (a few dozen characters), read back by [decode]. */
    fun encode(): String =
        listOf(
            VERSION,
            length.toString(),
            bestLength.toString(),
            hearts.toString(),
            snailsEaten.toString(),
            powers.joinToString(","),
            biggestSeconds.toString(),
            difficulty.key,
            arena?.key.orEmpty(),
            style.encode(),
            name,
        ).joinToString(SEP)

    companion object {
        private const val VERSION = "run4"
        private const val SEP = ";"
        private const val FIELDS = 11

        // How many fields a version has once it keeps each of these.
        private const val WITH_BIGGEST = 9
        private const val WITH_DIFFICULTY = 10
        private const val WITH_ARENA = 11

        /** Field counts of older versions, which come back as they can. */
        private val OLD_VERSIONS =
            mapOf(
                // Before floors were picked per game: no arena.
                "run3" to WITH_DIFFICULTY,
                // Before little kid mode: no difficulty either.
                "run2" to WITH_BIGGEST,
                // Before "biggest snail" time was kept: no time either.
                "run1" to WITH_BIGGEST - 1,
            )

        // Positions in the saved text (see encode()).
        private const val F_LENGTH = 1
        private const val F_BEST = 2
        private const val F_HEARTS = 3
        private const val F_EATEN = 4
        private const val F_POWERS = 5
        private const val F_BIGGEST = 6
        private const val F_DIFFICULTY = 7
        private const val F_ARENA = 8

        /** Reads [encode] output (or an older form); null for anything unreadable. */
        fun decode(text: String?): SavedRun? {
            if (text == null) return null
            val version = text.substringBefore(SEP)
            val fields = if (version == VERSION) FIELDS else OLD_VERSIONS[version] ?: return null
            // The name is last and may itself contain the separator, so split at most `fields` ways.
            val p = text.split(SEP, limit = fields)
            if (p.size != fields) return null
            val powers = p[F_POWERS].split(',').map { it.toFloatOrNull() ?: return null }
            val biggest = if (fields >= WITH_BIGGEST) p[F_BIGGEST].toFloatOrNull() ?: return null else 0f
            val difficulty =
                if (fields >= WITH_DIFFICULTY) Difficulty.byKey(p[F_DIFFICULTY]) ?: return null else Difficulty.NORMAL
            val run =
                SavedRun(
                    name = p[fields - 1],
                    style = SnailStyle.decode(p[fields - 2]),
                    length = p[F_LENGTH].toFloatOrNull() ?: return null,
                    bestLength = p[F_BEST].toFloatOrNull() ?: return null,
                    hearts = p[F_HEARTS].toIntOrNull() ?: return null,
                    snailsEaten = p[F_EATEN].toIntOrNull() ?: return null,
                    powers = powers,
                    biggestSeconds = biggest,
                    difficulty = difficulty,
                    arena = arenaIn(p, fields),
                )
            return run.takeIf { it.isSane() }
        }

        /** The saved floor, if this version kept one. One this version doesn't know is no reason to lose the game: null, today's. */
        private fun arenaIn(
            p: List<String>,
            fields: Int,
        ): ArenaSeason? = if (fields >= WITH_ARENA) ArenaSeason.byKey(p[F_ARENA]) else null

        private fun SavedRun.isSane(): Boolean =
            length.isFinite() && length >= GameConfig.MIN_LENGTH && bestLength >= length &&
                hearts in 1..difficulty.hearts && snailsEaten >= 0 &&
                powers.size == PowerType.entries.size && powers.all { it.isFinite() && it >= 0f } &&
                biggestSeconds.isFinite() && biggestSeconds >= 0f
    }
}
