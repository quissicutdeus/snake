package com.quissicutdeus.sniddle.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quissicutdeus.sniddle.Day
import com.quissicutdeus.sniddle.PhoneCalendar
import com.quissicutdeus.sniddle.SavedPlayer
import com.quissicutdeus.sniddle.Storage
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.model.Unlock
import com.quissicutdeus.sniddle.game.model.Unlocks
import com.quissicutdeus.sniddle.game.sim.SimOptions
import kotlin.random.Random

/** One kid on the title screen's "who's playing?" row. */
data class PlayerCard(
    val id: Int,
    /** Their snail's name, or "Player 2" while it has none. */
    val label: String,
    val style: SnailStyle,
)

/**
 * The saved choices of whoever is playing, and the shared settings, as Compose
 * state: reading one recomposes what shows it, and changing one saves it on
 * the device straight away. [switchTo] swaps in another kid's snail.
 */
class Profile(
    private val storage: Storage,
    /** Today's date (a fixed one in tests). */
    val calendar: PhoneCalendar,
) {
    var settings: Settings by mutableStateOf(storage.loadSettings())
        private set

    private var saved: SavedPlayer by mutableStateOf(storage.player(storage.loadCurrentPlayer()))

    /** Who's playing now (see [Storage.player]). */
    val playerId: Int get() = saved.id

    var style: SnailStyle by mutableStateOf(saved.loadStyle())
        private set

    /** What the kid typed as their snail's name ("" for none). */
    var snailName: String by mutableStateOf(saved.loadSnailName())
        private set

    /** The kid's birthday, month and day, or null if not set. */
    var birthday: Birthday? by mutableStateOf(saved.loadBirthday())
        private set

    /** Lifetime totals (best length, snails eaten, games played) that unlock looks. */
    var stats: Stats by mutableStateOf(saved.loadStats())
        private set

    /**
     * Looks already unlocked that nobody has cheered for yet, e.g. earned by
     * games played before unlocks existed. Shown once on the title screen.
     */
    var alreadyEarned: List<Unlock> by mutableStateOf(notYetCheered())
        private set

    /** Little kid mode: this kid plays gentler games. */
    var littleKid: Boolean by mutableStateOf(saved.loadLittleKid())
        private set

    /** Everyone who plays on this phone, for the title screen. */
    var players: List<PlayerCard> by mutableStateOf(loadPlayers())
        private set

    private fun notYetCheered() = saved.loadAnnounced().let { told -> Unlocks.reachedBy(stats).filterNot { it.key in told } }

    private fun loadPlayers() =
        storage.loadPlayers().mapIndexed { i, id ->
            val p = storage.player(id)
            PlayerCard(id, p.loadSnailName().ifBlank { "Player ${i + 1}" }, p.loadStyle())
        }

    /** Makes [id] the one playing (remembered for next time) and shows their snail. */
    fun switchTo(id: Int) {
        if (id == playerId || players.none { it.id == id }) return
        storage.saveCurrentPlayer(id)
        saved = storage.player(id)
        style = saved.loadStyle()
        snailName = saved.loadSnailName()
        birthday = saved.loadBirthday()
        stats = saved.loadStats()
        littleKid = saved.loadLittleKid()
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
        season: Set<Holiday> = holidaysToday(birthday, calendar.today()),
    ): List<Unlock> {
        val before = stats
        stats = before.after(result.score, result.snailsEaten, season)
        saved.saveStats(stats)
        return Unlocks.newlyUnlocked(before, stats).also(::announce)
    }

    /**
     * Like [recordGame], for kid [id] whether or not they're the one picked
     * (two kids play together). Nothing for anyone who isn't on this phone.
     */
    fun recordGameFor(
        id: Int,
        result: GameResult,
        /** The holidays on; null for today's, with that kid's own birthday. */
        season: Set<Holiday>? = null,
    ): List<Unlock> {
        if (id == playerId) return recordGame(result, season ?: holidaysToday(birthday, calendar.today()))
        if (players.none { it.id == id }) return emptyList()
        val kid = storage.player(id)
        val before = kid.loadStats()
        val after = before.after(result.score, result.snailsEaten, season ?: holidaysToday(kid.loadBirthday(), calendar.today()))
        kid.saveStats(after)
        val unlocked = Unlocks.newlyUnlocked(before, after)
        if (unlocked.isNotEmpty()) kid.saveAnnounced(kid.loadAnnounced() + unlocked.map { it.key })
        return unlocked
    }

    /** [alreadyEarned] has been shown, so it won't be shown again after this. */
    fun alreadyEarnedShown() = announce(alreadyEarned)

    /** Hides [alreadyEarned] (tapped away, or the kid went to play). */
    fun dismissAlreadyEarned() {
        alreadyEarned = emptyList()
    }

    private fun announce(unlocks: List<Unlock>) {
        if (unlocks.isNotEmpty()) saved.saveAnnounced(saved.loadAnnounced() + unlocks.map { it.key })
    }

    fun changeSettings(new: Settings) {
        settings = new
        storage.saveSettings(new)
    }

    fun changeStyle(new: SnailStyle) {
        style = new
        saved.saveStyle(new)
        players = loadPlayers()
    }

    fun changeLittleKid(on: Boolean) {
        littleKid = on
        saved.saveLittleKid(on)
    }

    fun changeBirthday(new: Birthday?) {
        birthday = new
        saved.saveBirthday(new)
    }

    /** It's this kid's birthday today. */
    val isBirthday: Boolean get() = Holiday.BIRTHDAY in holidaysToday(birthday, calendar.today())

    /** A game for this kid today: the holidays that are on (and their birthday), on this season's arena. */
    fun todaysOptions(): SimOptions = todaysOptions(birthday, calendar.today())

    /**
     * A two-player game for this kid and kid [partnerId] ([GUEST_ID] for a
     * "Player 2" who isn't on the phone) today: everyone's holidays, with
     * either kid's birthday, on this season's arena. Gentle (little kid mode)
     * only when both kids have it on; a guest counts as not little.
     */
    fun togetherOptions(partnerId: Int): SimOptions {
        val partner = storage.player(partnerId).takeIf { players.any { it.id == partnerId } }
        val mine = todaysOptions()
        val theirs = holidaysToday(partner?.loadBirthday(), calendar.today())
        val bothLittle = littleKid && partner?.loadLittleKid() == true
        return mine.copy(season = mine.season + theirs, difficulty = difficultyFor(bothLittle))
    }

    fun changeSnailName(new: String) {
        snailName = new
        saved.saveSnailName(new)
        players = loadPlayers()
    }
}

/** The holidays that are on [today] (by this phone's calendar), with [Holiday.BIRTHDAY] if it's the [birthday]. */
fun holidaysToday(
    birthday: Birthday?,
    today: Day,
): Set<Holiday> = Holiday.on(today.year, today.month, today.dayOfMonth, birthday)

/** A game [today]: its holidays (with [birthday], if it's then) for the computer snails, and the season's arena. */
fun todaysOptions(
    birthday: Birthday?,
    today: Day,
): SimOptions = SimOptions(season = holidaysToday(birthday, today), arena = ArenaSeason.forMonth(today.month))
