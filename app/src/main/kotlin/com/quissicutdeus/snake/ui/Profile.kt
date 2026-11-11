package com.quissicutdeus.snake.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quissicutdeus.snake.SavedPlayer
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.model.Holiday
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlock
import com.quissicutdeus.snake.game.model.Unlocks
import java.time.LocalDate
import kotlin.random.Random

/** One kid on the title screen's "who's playing?" row. */
data class PlayerCard(
    val id: Int,
    /** Their snake's name, or "Player 2" while it has none. */
    val label: String,
    val style: SnakeStyle,
)

/**
 * The saved choices of whoever is playing, and the shared settings, as Compose
 * state: reading one recomposes what shows it, and changing one saves it on
 * the device straight away. [switchTo] swaps in another kid's snake.
 */
class Profile(
    private val storage: Storage,
) {
    var settings: Settings by mutableStateOf(storage.loadSettings())
        private set

    private var saved: SavedPlayer by mutableStateOf(storage.player(storage.loadCurrentPlayer()))

    /** Who's playing now (see [Storage.player]). */
    val playerId: Int get() = saved.id

    var style: SnakeStyle by mutableStateOf(saved.loadStyle())
        private set

    /** What the kid typed as their snake's name ("" for none). */
    var snakeName: String by mutableStateOf(saved.loadSnakeName())
        private set

    /** Lifetime totals (best length, snakes eaten, games played) that unlock looks. */
    var stats: Stats by mutableStateOf(saved.loadStats())
        private set

    /**
     * Looks already unlocked that nobody has cheered for yet, e.g. earned by
     * games played before unlocks existed. Shown once on the title screen.
     */
    var alreadyEarned: List<Unlock> by mutableStateOf(notYetCheered())
        private set

    /** Everyone who plays on this phone, for the title screen. */
    var players: List<PlayerCard> by mutableStateOf(loadPlayers())
        private set

    private fun notYetCheered() = saved.loadAnnounced().let { told -> Unlocks.reachedBy(stats).filterNot { key(it) in told } }

    private fun loadPlayers() =
        storage.loadPlayers().mapIndexed { i, id ->
            val p = storage.player(id)
            PlayerCard(id, p.loadSnakeName().ifBlank { "Player ${i + 1}" }, p.loadStyle())
        }

    /** Makes [id] the one playing (remembered for next time) and shows their snake. */
    fun switchTo(id: Int) {
        if (id == playerId || players.none { it.id == id }) return
        storage.saveCurrentPlayer(id)
        saved = storage.player(id)
        style = saved.loadStyle()
        snakeName = saved.loadSnakeName()
        stats = saved.loadStats()
        alreadyEarned = notYetCheered()
    }

    /**
     * A new kid joins with a random look from the ones open to everyone, and
     * becomes the one playing. False if the phone already has its fill.
     */
    fun addPlayer(rng: Random = Random.Default): Boolean {
        val id = storage.addPlayer(Unlocks.randomStyle(rng, Stats())) ?: return false
        players = loadPlayers()
        switchTo(id)
        return true
    }

    /** Takes kid [id] off the phone (never the last one); if they were playing, another kid takes over. */
    fun removePlayer(id: Int) {
        if (!storage.removePlayer(id)) return
        players = loadPlayers()
        switchTo(storage.loadCurrentPlayer())
    }

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
        saved.saveStats(stats)
        return Unlocks.newlyUnlocked(before, stats).also(::announce)
    }

    /** [alreadyEarned] has been shown, so it won't be shown again after this. */
    fun alreadyEarnedShown() = announce(alreadyEarned)

    /** Hides [alreadyEarned] (tapped away, or the kid went to play). */
    fun dismissAlreadyEarned() {
        alreadyEarned = emptyList()
    }

    private fun announce(unlocks: List<Unlock>) {
        if (unlocks.isNotEmpty()) saved.saveAnnounced(saved.loadAnnounced() + unlocks.map(::key))
    }

    /** How an unlock is saved in [SavedPlayer.loadAnnounced], e.g. "Hat.TOP_HAT". */
    private fun key(unlock: Unlock) = "${unlock.item.declaringJavaClass.simpleName}.${unlock.item.name}"

    fun changeSettings(new: Settings) {
        settings = new
        storage.saveSettings(new)
    }

    fun changeStyle(new: SnakeStyle) {
        style = new
        saved.saveStyle(new)
        players = loadPlayers()
    }

    fun changeSnakeName(new: String) {
        snakeName = new
        saved.saveSnakeName(new)
        players = loadPlayers()
    }
}

/** The holidays that are on today, by this phone's calendar. */
fun holidaysToday(): Set<Holiday> = LocalDate.now().let { Holiday.on(it.monthValue, it.dayOfMonth) }
