package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.model.Snake
import kotlin.math.floor

/**
 * Coarse grid listing, for every cell, the live snakes whose heads could reach
 * some point in that cell, so each piece of food only looks at the few snakes
 * that could possibly eat or pull it. Rebuilt once per step.
 *
 * Snakes are referred to by their index in the list given to [build], and each
 * cell lists them in that order, so callers keep "first snake in the list
 * wins" rules exactly as they were.
 */
internal class HeadGrid(
    width: Float,
    height: Float,
    private val cellSize: Float = 128f,
) {
    private val cols = (width / cellSize).toInt() + 1
    private val rows = (height / cellSize).toInt() + 1
    private val cellStart = IntArray(cols * rows + 1)
    private val cellFill = IntArray(cols * rows)
    private var entries = IntArray(INITIAL_CAPACITY)

    /** Lists each live snake in every cell within [reach] of its head (in x and y). */
    fun build(
        snakes: List<Snake>,
        reach: Float,
    ) {
        cellFill.fill(0)
        var total = 0
        for (i in snakes.indices) {
            if (!cover(snakes[i], reach)) continue
            for (r in r0..r1) {
                for (c in c0..c1) cellFill[r * cols + c]++
            }
            total += (r1 - r0 + 1) * (c1 - c0 + 1)
        }
        if (total > entries.size) entries = IntArray(maxOf(total, entries.size * 2))
        var acc = 0
        for (c in 0 until cols * rows) {
            cellStart[c] = acc
            acc += cellFill[c]
            cellFill[c] = 0
        }
        cellStart[cols * rows] = acc
        // Walking the snakes in list order keeps each cell's indices ascending.
        for (i in snakes.indices) {
            if (!cover(snakes[i], reach)) continue
            for (r in r0..r1) {
                for (c in c0..c1) {
                    val cell = r * cols + c
                    entries[cellStart[cell] + cellFill[cell]++] = i
                }
            }
        }
    }

    // Cell range covered by the snake given to the latest [cover].
    private var c0 = 0
    private var c1 = 0
    private var r0 = 0
    private var r1 = 0

    /** Sets [c0]..[c1] x [r0]..[r1] to the cells within [reach] of [s]'s head; false if [s] is dead. */
    private fun cover(
        s: Snake,
        reach: Float,
    ): Boolean {
        if (!s.alive) return false
        c0 = col(s.x - reach)
        c1 = col(s.x + reach)
        r0 = row(s.y - reach)
        r1 = row(s.y + reach)
        return true
    }

    /** The cell holding (x, y); its snakes are [startOf] (cell) until [startOf] (cell + 1). */
    fun cellAt(
        x: Float,
        y: Float,
    ): Int = row(y) * cols + col(x)

    fun startOf(cell: Int): Int = cellStart[cell]

    /** A list index stored at position [e] (see [cellAt]). */
    fun snakeIndexAt(e: Int): Int = entries[e]

    private fun col(x: Float) = floor(x / cellSize).toInt().coerceIn(0, cols - 1)

    private fun row(y: Float) = floor(y / cellSize).toInt().coerceIn(0, rows - 1)

    private companion object {
        const val INITIAL_CAPACITY = 512
    }
}
