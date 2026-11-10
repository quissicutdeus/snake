package com.snakearena.game.model

/** One finished run on the device's high-score list. */
data class ScoreEntry(
    val name: String,
    val score: Int,
    val snakesEaten: Int,
    /** The look of the snake used, for the little picture. */
    val style: SnakeStyle,
    /** When it was played (epoch millis). Also identifies the entry. */
    val timestamp: Long,
)
