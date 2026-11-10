package com.quissicutdeus.snake

import android.content.Context
import androidx.core.content.edit
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.sim.Scoreboard

/** Everything saved on the device: the player's snake look, high scores, player names and the sound switch. */
class Storage(
    context: Context,
) {
    // Named from when the game was "Snake Arena"; renaming it would lose everyone's saved high scores.
    private val prefs = context.getSharedPreferences("snake_arena", Context.MODE_PRIVATE)

    fun loadStyle(): SnakeStyle = SnakeStyle.decode(prefs.getString(KEY_STYLE, null))

    fun saveStyle(style: SnakeStyle) = prefs.edit { putString(KEY_STYLE, style.encode()) }

    fun loadScores(): List<ScoreEntry> = Scoreboard.decode(prefs.getString(KEY_SCORES, null))

    fun saveScores(scores: List<ScoreEntry>) = prefs.edit { putString(KEY_SCORES, Scoreboard.encode(scores)) }

    fun resetScores() = prefs.edit { remove(KEY_SCORES) }

    fun loadSoundOn(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun saveSoundOn(on: Boolean) = prefs.edit { putBoolean(KEY_SOUND, on) }

    fun loadNames(): List<String> = Scoreboard.decodeNames(prefs.getString(KEY_NAMES, null))

    fun saveNames(names: List<String>) = prefs.edit { putString(KEY_NAMES, Scoreboard.encodeNames(names)) }

    /** Adds a finished run to the high scores and remembers the player's name. */
    fun addScore(entry: ScoreEntry) {
        saveScores(Scoreboard.insert(loadScores(), entry))
        saveNames(Scoreboard.rememberName(loadNames(), entry.name))
    }

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_SCORES = "scores"
        const val KEY_NAMES = "names"
        const val KEY_SOUND = "sound"
    }
}
