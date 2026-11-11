package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle

/**
 * The offline, on-device high-score list: ranking, inserting, trimming,
 * player names and a simple text format for saving.
 */
object Scoreboard {
    const val MAX_ENTRIES = 10
    const val MAX_NAME_LENGTH = 12
    const val MAX_REMEMBERED_NAMES = 8
    const val DEFAULT_NAME = "Player"

    // Fields of a saved line (see encode()).
    private const val FIELD_SCORE = 0
    private const val FIELD_EATEN = 1
    private const val FIELD_TIME = 2
    private const val FIELD_STYLE = 3
    private const val FIELD_NAME = 4
    private const val SAVED_FIELDS = 5

    /** Best first; equal scores keep the older run first (it got there first). */
    private val order = compareByDescending<ScoreEntry> { it.score }.thenBy { it.timestamp }

    fun sorted(entries: List<ScoreEntry>): List<ScoreEntry> = entries.sortedWith(order).take(MAX_ENTRIES)

    /** Would a run with [score] make the list? (Ties with the last place don't.) */
    fun qualifies(
        entries: List<ScoreEntry>,
        score: Int,
    ): Boolean {
        if (score <= 0) return false
        val top = sorted(entries)
        return top.size < MAX_ENTRIES || score > top.last().score
    }

    /** The list with [entry] added in its place (dropping whatever falls off the bottom). */
    fun insert(
        entries: List<ScoreEntry>,
        entry: ScoreEntry,
    ): List<ScoreEntry> = (entries + entry).sortedWith(order).take(MAX_ENTRIES)

    /** The name the player's snake goes by in a game: what they typed, tidied, or "You". */
    fun snakeName(typed: String): String = if (typed.isBlank()) "You" else cleanName(typed)

    /** Tidies a typed name: no control characters, single spaces, at most [MAX_NAME_LENGTH] characters. */
    fun cleanName(raw: String): String {
        val cleaned = raw.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
        val cut = if (cleaned.length > MAX_NAME_LENGTH) cleaned.take(MAX_NAME_LENGTH).trimEnd() else cleaned
        return cut.ifEmpty { DEFAULT_NAME }
    }

    /** Names for the "Who played?" picker: [name] moves to the front, no duplicates (ignoring case). */
    fun rememberName(
        names: List<String>,
        name: String,
    ): List<String> {
        val clean = cleanName(name)
        return (listOf(clean) + names.filter { !it.equals(clean, ignoreCase = true) }).take(MAX_REMEMBERED_NAMES)
    }

    fun encode(entries: List<ScoreEntry>): String =
        entries.joinToString("\n") {
            listOf(it.score, it.snakesEaten, it.timestamp, it.style.encode(), cleanName(it.name)).joinToString("\t")
        }

    /** Parses [encode] output. Broken lines are skipped rather than losing the whole list. */
    fun decode(text: String?): List<ScoreEntry> {
        if (text.isNullOrBlank()) return emptyList()
        return sorted(
            text.lines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < SAVED_FIELDS) return@mapNotNull null
                val score = f[FIELD_SCORE].toIntOrNull() ?: return@mapNotNull null
                val eaten = f[FIELD_EATEN].toIntOrNull() ?: return@mapNotNull null
                val time = f[FIELD_TIME].toLongOrNull() ?: return@mapNotNull null
                ScoreEntry(cleanName(f[FIELD_NAME]), score, eaten, SnakeStyle.decode(f[FIELD_STYLE]), time)
            },
        )
    }

    fun encodeNames(names: List<String>): String = names.joinToString("\n")

    fun decodeNames(text: String?): List<String> =
        text
            .orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_REMEMBERED_NAMES)
}
