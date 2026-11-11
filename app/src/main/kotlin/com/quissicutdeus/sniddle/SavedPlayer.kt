package com.quissicutdeus.sniddle

import android.content.SharedPreferences
import androidx.core.content.edit
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats

/**
 * One kid's saved snail: look, name, unlock totals and which unlocks they've
 * been cheered for. Player 0 uses the keys from before there were players
 * ("style", "stats"...), so an updated phone keeps its snail without any
 * copying; other players put "p2." and so on in front of the same keys.
 */
class SavedPlayer internal constructor(
    private val prefs: SharedPreferences,
    val id: Int,
    /** The shared high-score list, which player 0's totals start from (see [loadStats]). */
    private val oldGames: () -> List<ScoreEntry>,
) {
    private fun key(base: String) = if (id == 0) base else "p$id.$base"

    fun loadStyle(): SnailStyle = SnailStyle.decode(prefs.getString(key(KEY_STYLE), null))

    fun saveStyle(style: SnailStyle) = prefs.edit { putString(key(KEY_STYLE), style.encode()) }

    /** The name the kid gave their snail ("" for none). */
    fun loadSnailName(): String = prefs.getString(key(KEY_SNAIL_NAME), null).orEmpty()

    fun saveSnailName(name: String) = prefs.edit { putString(key(KEY_SNAIL_NAME), name) }

    /**
     * Lifetime totals that unlock new looks. Player 0 starts from the
     * high-score list the first time, so games from before unlocks existed
     * count too. Everyone else starts from nothing.
     */
    fun loadStats(): Stats =
        Stats.decode(prefs.getString(key(KEY_STATS), null)) ?: if (id == 0) {
            oldGames().let { scores -> Stats(scores.maxOfOrNull { it.score } ?: 0, scores.sumOf { it.snailsEaten }, scores.size) }
        } else {
            Stats()
        }

    fun saveStats(stats: Stats) = prefs.edit { putString(key(KEY_STATS), stats.encode()) }

    /** Unlocked looks this kid has been told about ("Hat.TOP_HAT"...); any others get a cheer on the title screen. */
    fun loadAnnounced(): Set<String> = prefs.getStringSet(key(KEY_ANNOUNCED), null)?.toSet().orEmpty()

    fun saveAnnounced(keys: Set<String>) = prefs.edit { putStringSet(key(KEY_ANNOUNCED), keys.toSet()) }

    /** Little kid mode: gentler games for this kid (see Difficulty.LITTLE). Off unless switched on. */
    fun loadLittleKid(): Boolean = prefs.getBoolean(key(KEY_LITTLE_KID), false)

    fun saveLittleKid(on: Boolean) = prefs.edit { putBoolean(key(KEY_LITTLE_KID), on) }

    /** The kid's birthday (month and day only), or null if they haven't said. */
    fun loadBirthday(): Birthday? = Birthday.decode(prefs.getInt(key(KEY_BIRTHDAY), 0))

    fun saveBirthday(birthday: Birthday?) =
        prefs.edit {
            if (birthday == null) remove(key(KEY_BIRTHDAY)) else putInt(key(KEY_BIRTHDAY), birthday.encode())
        }

    /** Forgets this kid's snail altogether (their high scores stay: those are everyone's). */
    fun clear() =
        prefs.edit {
            listOf(
                KEY_STYLE,
                KEY_SNAIL_NAME,
                KEY_STATS,
                KEY_ANNOUNCED,
                KEY_LITTLE_KID,
                KEY_BIRTHDAY,
            ).forEach { remove(key(it)) }
        }

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_SNAIL_NAME = "snail_name"
        const val KEY_STATS = "stats"
        const val KEY_ANNOUNCED = "announced"
        const val KEY_LITTLE_KID = "little_kid"
        const val KEY_BIRTHDAY = "birthday"
    }
}
