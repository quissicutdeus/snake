package com.quissicutdeus.sniddle.game.model

/** One finished run on the device's high-score list. */
data class ScoreEntry(
    val name: String,
    val score: Int,
    val snailsEaten: Int,
    /** The look of the snail used, for the little picture. */
    val style: SnailStyle,
    /** When it was played (epoch millis). Also identifies the entry. */
    val timestamp: Long,
    /** Played in little kid mode (shown with a 🐣 so the list stays fair). */
    val littleKid: Boolean = false,
)
