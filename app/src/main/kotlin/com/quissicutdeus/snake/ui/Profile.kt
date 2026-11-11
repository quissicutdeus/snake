package com.quissicutdeus.snake.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlock
import com.quissicutdeus.snake.game.model.Unlocks

/**
 * The player's saved choices as Compose state: reading one recomposes what
 * shows it, and changing one saves it on the device straight away.
 */
class Profile(
    private val storage: Storage,
) {
    var settings: Settings by mutableStateOf(storage.loadSettings())
        private set

    var style: SnakeStyle by mutableStateOf(storage.loadStyle())
        private set

    /** What the player typed as their snake's name ("" for none). */
    var snakeName: String by mutableStateOf(storage.loadSnakeName())
        private set

    /** Lifetime totals (best length, snakes eaten, games played) that unlock looks. */
    var stats: Stats by mutableStateOf(storage.loadStats())
        private set

    /** Counts a finished game. Returns the looks it unlocked (usually none). */
    fun recordGame(result: GameResult): List<Unlock> {
        val before = stats
        stats = before.after(result.score, result.snakesEaten)
        storage.saveStats(stats)
        return Unlocks.newlyUnlocked(before, stats)
    }

    fun changeSettings(new: Settings) {
        settings = new
        storage.saveSettings(new)
    }

    fun changeStyle(new: SnakeStyle) {
        style = new
        storage.saveStyle(new)
    }

    fun changeSnakeName(new: String) {
        snakeName = new
        storage.saveSnakeName(new)
    }
}
