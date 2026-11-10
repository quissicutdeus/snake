package com.snakearena.game.sim

import com.snakearena.game.model.ScoreEntry
import com.snakearena.game.model.SnakeStyle

/**
 * The offline, on-device high-score list: ranking, inserting, trimming,
 * player names and a simple text format for saving.
 */
object Scoreboard {
    const val MAX_ENTRIES = 10
    const val MAX_NAME_LENGTH = 12
    const val MAX_REMEMBERED_NAMES = 8
    const val DEFAULT_NAME = "Player"

    /** Best first; equal scores keep the older run first (it got there first). */
    private val order = compareByDescending<ScoreEntry> { it.score }.thenBy { it.timestamp }

    fun sorted(entries: List<ScoreEntry>): List<ScoreEntry> = entries.sortedWith(order).take(MAX_ENTRIES)

    /** Would a run with [score] make the list? (Ties with the last place don't.) */
    fun qualifies(entries: List<ScoreEntry>, score: Int): Boolean {
        if (score <= 0) return false
        val top = sorted(entries)
        return top.size < MAX_ENTRIES || score > top.last().score
    }

    /** Result of [insert]: the new list and where the new entry landed (-1 if it didn't make it). */
    data class Insertion(val entries: List<ScoreEntry>, val index: Int)

    fun insert(entries: List<ScoreEntry>, entry: ScoreEntry): Insertion {
        val all = (entries + entry).sortedWith(order).take(MAX_ENTRIES)
        return Insertion(all, all.indexOf(entry))
    }

    /** Tidies a typed name: no control characters, single spaces, at most [MAX_NAME_LENGTH] characters. */
    fun cleanName(raw: String): String {
        val cleaned = raw.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
        val cut = if (cleaned.length > MAX_NAME_LENGTH) cleaned.take(MAX_NAME_LENGTH).trimEnd() else cleaned
        return cut.ifEmpty { DEFAULT_NAME }
    }

    /** Names for the "Who played?" picker: [name] moves to the front, no duplicates (ignoring case). */
    fun rememberName(names: List<String>, name: String): List<String> {
        val clean = cleanName(name)
        return (listOf(clean) + names.filter { !it.equals(clean, ignoreCase = true) }).take(MAX_REMEMBERED_NAMES)
    }

    fun encode(entries: List<ScoreEntry>): String = entries.joinToString("\n") {
        listOf(it.score, it.snakesEaten, it.timestamp, it.style.encode(), cleanName(it.name)).joinToString("\t")
    }

    /** Parses [encode] output. Broken lines are skipped rather than losing the whole list. */
    fun decode(text: String?): List<ScoreEntry> {
        if (text.isNullOrBlank()) return emptyList()
        return sorted(
            text.lines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 5) return@mapNotNull null
                val score = f[0].toIntOrNull() ?: return@mapNotNull null
                val eaten = f[1].toIntOrNull() ?: return@mapNotNull null
                val time = f[2].toLongOrNull() ?: return@mapNotNull null
                ScoreEntry(cleanName(f[4]), score, eaten, SnakeStyle.decode(f[3]), time)
            },
        )
    }

    fun encodeNames(names: List<String>): String = names.joinToString("\n")

    fun decodeNames(text: String?): List<String> =
        text.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_REMEMBERED_NAMES)
}
