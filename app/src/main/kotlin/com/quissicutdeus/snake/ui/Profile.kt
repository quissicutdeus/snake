package com.quissicutdeus.snake.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.model.Holiday
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlock
import com.quissicutdeus.snake.game.model.Unlocks
import java.time.LocalDate

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

    /**
     * Looks already unlocked that nobody has cheered for yet, e.g. earned by
     * games played before unlocks existed. Shown once on the title screen.
     */
    var alreadyEarned: List<Unlock> by mutableStateOf(
        storage.loadAnnounced().let { told -> Unlocks.reachedBy(stats).filterNot { key(it) in told } },
    )
        private set

    /**
     * Counts a finished game, played while [season]'s holidays were on.
     * Returns the looks it unlocked (usually none), which the game-over screen cheers.
     */
    fun recordGame(
        result: GameResult,
        season: Set<Holiday> = holidaysToday(),
    ): List<Unlock> {
        val before = stats
        stats = before.after(result.score, result.snakesEaten, season)
        storage.saveStats(stats)
        return Unlocks.newlyUnlocked(before, stats).also(::announce)
    }

    /** [alreadyEarned] has been shown, so it won't be shown again after this. */
    fun alreadyEarnedShown() = announce(alreadyEarned)

    /** Hides [alreadyEarned] (tapped away, or the kid went to play). */
    fun dismissAlreadyEarned() {
        alreadyEarned = emptyList()
    }

    private fun announce(unlocks: List<Unlock>) {
        if (unlocks.isNotEmpty()) storage.saveAnnounced(storage.loadAnnounced() + unlocks.map(::key))
    }

    /** How an unlock is saved in [Storage.loadAnnounced], e.g. "Hat.TOP_HAT". */
    private fun key(unlock: Unlock) = "${unlock.item.declaringJavaClass.simpleName}.${unlock.item.name}"

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

/** The holidays that are on today, by this phone's calendar. */
fun holidaysToday(): Set<Holiday> = LocalDate.now().let { Holiday.on(it.monthValue, it.dayOfMonth) }
