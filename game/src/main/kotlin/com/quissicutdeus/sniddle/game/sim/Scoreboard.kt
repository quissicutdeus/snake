package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import java.text.BreakIterator

/**
 * The offline, on-device high-score list: ranking, inserting, trimming,
 * player names and a simple text format for saving.
 */
object Scoreboard {
    const val MAX_ENTRIES = 10

    /** Letters or emoji in a name, as a kid counts them (see [fitName]). */
    const val MAX_NAME_LENGTH = 12

    /**
     * UTF-16 code units in a name, so a few letters stacked with accent marks can't fill the
     * screen. Twelve flags or skin-toned emoji fit; twelve family emoji don't.
     */
    const val MAX_NAME_CHARS = 64
    const val MAX_REMEMBERED_NAMES = 8
    const val DEFAULT_NAME = "Player"

    // Fields of a saved line (see encode()).
    private const val FIELD_SCORE = 0
    private const val FIELD_EATEN = 1
    private const val FIELD_TIME = 2
    private const val FIELD_STYLE = 3
    private const val FIELD_NAME = 4
    private const val SAVED_FIELDS = 5

    /**
     * After the name, so older versions (which read the first five fields)
     * still load the list; lines from before it have no sixth field.
     */
    private const val FIELD_LITTLE_KID = 5
    private const val LITTLE_KID = "little"

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

    /** The name the player's snail goes by in a game: what they typed, tidied, or "You". */
    fun snailName(typed: String): String = if (typed.isBlank()) "You" else cleanName(typed)

    /**
     * Tidies a typed name: no control characters, single spaces, cut by [fitName] to at most
     * [MAX_NAME_LENGTH] letters or emoji.
     */
    fun cleanName(raw: String): String {
        val cleaned = raw.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
        return fitName(cleaned).trimEnd().ifEmpty { DEFAULT_NAME }
    }

    /**
     * The longest start of [text] that has at most [maxLength] user-perceived characters (grapheme
     * clusters: a letter with its accents, or a whole emoji such as a family, a flag or a thumbs-up
     * with a skin tone) and at most [MAX_NAME_CHARS] UTF-16 code units. It never cuts inside a
     * cluster, so it never leaves half an emoji or a lone surrogate. [text] comes back unchanged
     * when it already fits.
     *
     * Kids count what they see, so [maxLength] counts clusters. The code-unit bound only matters for
     * a single cluster piled high with combining marks, which would otherwise be one "letter" as
     * tall or wide as the screen; such a cluster is left out whole.
     *
     * Clusters come from [BreakIterator.getCharacterInstance], which follows the Unicode extended
     * grapheme rules (emoji ZWJ sequences included) on JDK 20 and later and on Android, where it is
     * backed by ICU.
     */
    fun fitName(
        text: String,
        maxLength: Int = MAX_NAME_LENGTH,
    ): String {
        val clusters = BreakIterator.getCharacterInstance()
        clusters.setText(text)
        var end = 0
        var count = 0
        while (count < maxLength) {
            val next = clusters.next()
            if (next == BreakIterator.DONE || next > MAX_NAME_CHARS) break
            end = next
            count++
        }
        return if (end == text.length) text else text.substring(0, end)
    }

    /**
     * What a name box shows after an edit from [old] to [new]. An edit that fits is kept. One that
     * only adds to the end (typing, or pasting into an empty box) is cut by [fitName]. Any other
     * edit that would go over the limit is ignored, so typing into the middle of a full name
     * doesn't push letters off the end.
     */
    fun editName(
        old: String,
        new: String,
    ): String {
        val fit = fitName(new)
        return if (fit == new || new.startsWith(old)) fit else old
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
            val fields = listOf(it.score, it.snailsEaten, it.timestamp, it.style.encode(), cleanName(it.name))
            (if (it.littleKid) fields + LITTLE_KID else fields).joinToString("\t")
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
                val little = f.getOrNull(FIELD_LITTLE_KID) == LITTLE_KID
                ScoreEntry(cleanName(f[FIELD_NAME]), score, eaten, SnailStyle.decode(f[FIELD_STYLE]), time, little)
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
