package com.quissicutdeus.sniddle

import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.sim.Scoreboard
import com.quissicutdeus.sniddle.ui.Settings

/**
 * Everything saved on the device: the high scores, names used before and the
 * settings, which everyone shares, and each kid's own snail ([player]).
 */
class Storage(
    private val prefs: KeyValueStore,
) {
    fun loadScores(): List<ScoreEntry> = Scoreboard.decode(prefs.getString(KEY_SCORES))

    fun saveScores(scores: List<ScoreEntry>) = prefs.edit { putString(KEY_SCORES, Scoreboard.encode(scores)) }

    fun resetScores() = prefs.edit { remove(KEY_SCORES) }

    fun loadSettings(): Settings =
        Settings(
            soundOn = prefs.getBoolean(KEY_SOUND, true),
            buzzOn = prefs.getBoolean(KEY_BUZZ, true),
            musicOn = prefs.getBoolean(KEY_MUSIC, true),
            bedtimeMinutes = prefs.getInt(KEY_BEDTIME, 0),
        )

    fun saveSettings(settings: Settings) =
        prefs.edit {
            putBoolean(KEY_SOUND, settings.soundOn)
            putBoolean(KEY_BUZZ, settings.buzzOn)
            putBoolean(KEY_MUSIC, settings.musicOn)
            putInt(KEY_BEDTIME, settings.bedtimeMinutes)
        }

    /** Ids of the kids who play on this phone, in the order they joined: just player 0 until a second one joins. */
    fun loadPlayers(): List<Int> =
        prefs
            .getString(KEY_PLAYERS)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 0 until MAX_PLAYERS }
            ?.distinct()
            ?.ifEmpty { null } ?: listOf(0)

    /** Who's playing now: the last kid picked on the title screen. */
    fun loadCurrentPlayer(): Int = loadPlayers().let { ids -> prefs.getInt(KEY_PLAYER, ids[0]).takeIf { it in ids } ?: ids[0] }

    fun saveCurrentPlayer(id: Int) = prefs.edit { putInt(KEY_PLAYER, id) }

    fun player(id: Int): SavedPlayer = SavedPlayer(prefs, id, ::loadScores)

    /**
     * Adds a kid with a snail in [style] and nothing else yet. Returns their
     * id, or null when the phone already has [MAX_PLAYERS].
     */
    fun addPlayer(style: SnailStyle): Int? {
        val ids = loadPlayers()
        val id = (0 until MAX_PLAYERS).firstOrNull { it !in ids } ?: return null
        // Player 0's totals may still be worked out from the high-score list,
        // which the new kid's games are about to join: write them down first.
        if (0 in ids) player(0).let { it.saveStats(it.loadStats()) }
        // Every key written, so nothing is left over from anyone before.
        player(id).apply {
            saveStyle(style)
            saveSnailName("")
            saveStats(Stats())
            saveAnnounced(emptySet())
            saveLittleKid(false)
            saveBirthday(null)
        }
        prefs.edit { putString(KEY_PLAYERS, (ids + id).joinToString(",")) }
        return id
    }

    /**
     * Takes kid [id] off the phone; if they were playing, the first kid left
     * plays next. Never removes the last kid (false then). Everyone else's
     * totals were written down when they joined (see [addPlayer]), so the
     * high-score list can't leak into them, and a kid who later gets [id]
     * has every key written afresh.
     */
    fun removePlayer(id: Int): Boolean {
        val ids = loadPlayers()
        val left = ids - id
        if (left.isEmpty() || left.size == ids.size) return false
        if (loadCurrentPlayer() == id) saveCurrentPlayer(left[0])
        prefs.edit { putString(KEY_PLAYERS, left.joinToString(",")) }
        player(id).clear()
        return true
    }

    fun loadNames(): List<String> = Scoreboard.decodeNames(prefs.getString(KEY_NAMES))

    fun saveNames(names: List<String>) = prefs.edit { putString(KEY_NAMES, Scoreboard.encodeNames(names)) }

    /** Adds a finished run to the high scores and remembers the player's name. */
    fun addScore(entry: ScoreEntry) {
        saveScores(Scoreboard.insert(loadScores(), entry))
        saveNames(Scoreboard.rememberName(loadNames(), entry.name))
    }

    /** The newest "What's new" entry shown on this device, or null if this install has never set it. */
    var newsSeen: Int?
        get() = if (KEY_NEWS_SEEN in prefs) prefs.getInt(KEY_NEWS_SEEN, 0) else null
        set(value) = prefs.edit { if (value == null) remove(KEY_NEWS_SEEN) else putInt(KEY_NEWS_SEEN, value) }

    /** Today's play on this phone for the bedtime timer ([PlayDay.encode]), or null before any. */
    var playDay: String?
        get() = prefs.getString(KEY_PLAY_DAY)
        set(value) = prefs.edit { putString(KEY_PLAY_DAY, value) }

    companion object {
        /** Kids per phone: enough for a family, and their pictures still fit in one row. */
        const val MAX_PLAYERS = 4

        private const val KEY_SCORES = "scores"
        private const val KEY_NAMES = "names"
        private const val KEY_SOUND = "sound"
        private const val KEY_BUZZ = "buzz"
        private const val KEY_MUSIC = "music"
        private const val KEY_PLAYERS = "players"
        private const val KEY_PLAYER = "player"
        private const val KEY_NEWS_SEEN = "news_seen"
        private const val KEY_BEDTIME = "bedtime_minutes"
        private const val KEY_PLAY_DAY = "play_day"
    }
}
