package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.SnakeStyle

/**
 * The player's side of a game in progress: enough to carry on after Android
 * closes the app in the background. Only the player's snake is kept; the world
 * around it (food, other snakes) is made fresh when the game comes back.
 */
data class SavedRun(
    val name: String,
    val style: SnakeStyle,
    val length: Float,
    val bestLength: Float,
    val hearts: Int,
    val snakesEaten: Int,
    /** Seconds left of each power, indexed by [PowerType.ordinal]. */
    val powers: List<Float>,
) {
    /** A short text form (a few dozen characters), read back by [decode]. */
    fun encode(): String =
        listOf(
            VERSION,
            length.toString(),
            bestLength.toString(),
            hearts.toString(),
            snakesEaten.toString(),
            powers.joinToString(","),
            style.encode(),
            name,
        ).joinToString(SEP)

    companion object {
        private const val VERSION = "run1"
        private const val SEP = ";"
        private const val FIELDS = 8

        // Positions in the saved text (see encode()).
        private const val F_LENGTH = 1
        private const val F_BEST = 2
        private const val F_HEARTS = 3
        private const val F_EATEN = 4
        private const val F_POWERS = 5
        private const val F_STYLE = 6
        private const val F_NAME = 7

        /** Reads [encode] output; null for anything unreadable or from another version. */
        fun decode(text: String?): SavedRun? {
            // The name is last and may itself contain the separator, so split at most FIELDS ways.
            val p = text?.split(SEP, limit = FIELDS) ?: return null
            if (p.size != FIELDS || p[0] != VERSION) return null
            val powers = p[F_POWERS].split(',').map { it.toFloatOrNull() ?: return null }
            val run =
                SavedRun(
                    name = p[F_NAME],
                    style = SnakeStyle.decode(p[F_STYLE]),
                    length = p[F_LENGTH].toFloatOrNull() ?: return null,
                    bestLength = p[F_BEST].toFloatOrNull() ?: return null,
                    hearts = p[F_HEARTS].toIntOrNull() ?: return null,
                    snakesEaten = p[F_EATEN].toIntOrNull() ?: return null,
                    powers = powers,
                )
            return run.takeIf { it.isSane() }
        }

        private fun SavedRun.isSane(): Boolean =
            length.isFinite() && length >= GameConfig.MIN_LENGTH && bestLength >= length &&
                hearts in 1..GameConfig.PLAYER_HEARTS && snakesEaten >= 0 &&
                powers.size == PowerType.entries.size && powers.all { it.isFinite() && it >= 0f }
    }
}
