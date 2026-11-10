package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.dist2
import kotlin.math.floor

/**
 * Uniform grid over the arena holding every body circle of every snake, so a
 * head only tests the circles near it. Rebuilt once per step.
 */
class SpatialGrid(
    private val width: Float,
    private val height: Float,
    private val cellSize: Float = 96f,
) {
    private val cols = (width / cellSize).toInt() + 1
    private val rows = (height / cellSize).toInt() + 1
    private val cellStart = IntArray(cols * rows + 1)
    private val cellFill = IntArray(cols * rows)
    private var entrySnake = arrayOfNulls<Snake>(1024)
    private var entrySeg = IntArray(1024)
    private var cellOf = IntArray(1024)

    fun build(snakes: List<Snake>) {
        var total = 0
        for (s in snakes) if (s.alive) total += s.segCount
        if (total > entrySeg.size) {
            val n = maxOf(total, entrySeg.size * 2)
            entrySnake = arrayOfNulls(n)
            entrySeg = IntArray(n)
            cellOf = IntArray(n)
        }
        cellFill.fill(0)
        // Pass 1: count per cell.
        var k = 0
        for (s in snakes) {
            if (!s.alive) continue
            for (i in 0 until s.segCount) {
                val c = cellIndex(s.segX[i], s.segY[i])
                cellOf[k++] = c
                cellFill[c]++
            }
        }
        // Prefix sums.
        var acc = 0
        for (c in 0 until cols * rows) {
            cellStart[c] = acc
            acc += cellFill[c]
            cellFill[c] = 0
        }
        cellStart[cols * rows] = acc
        // Pass 2: place entries.
        k = 0
        for (s in snakes) {
            if (!s.alive) continue
            for (i in 0 until s.segCount) {
                val c = cellOf[k++]
                val slot = cellStart[c] + cellFill[c]++
                entrySnake[slot] = s
                entrySeg[slot] = i
            }
        }
    }

    private fun col(x: Float) = floor(x / cellSize).toInt().coerceIn(0, cols - 1)

    private fun row(y: Float) = floor(y / cellSize).toInt().coerceIn(0, rows - 1)

    private fun cellIndex(
        x: Float,
        y: Float,
    ) = row(y) * cols + col(x)

    /** Calls [fn] for every body circle stored in cells touching the box around (x, y). */
    inline fun query(
        x: Float,
        y: Float,
        reach: Float,
        fn: (Snake, Int) -> Unit,
    ) {
        forEachInBox(x - reach, y - reach, x + reach, y + reach, fn)
    }

    @PublishedApi
    internal inline fun forEachInBox(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        fn: (Snake, Int) -> Unit,
    ) {
        val c0 = colOf(x0)
        val c1 = colOf(x1)
        val r0 = rowOf(y0)
        val r1 = rowOf(y1)
        for (r in r0..r1) {
            for (c in c0..c1) {
                val cell = r * columns + c
                for (e in startOf(cell) until startOf(cell + 1)) {
                    val s = snakeAt(e)
                    val seg = segAt(e)
                    if (s.alive && seg < s.segCount) fn(s, seg)
                }
            }
        }
    }

    @PublishedApi internal val columns get() = cols

    @PublishedApi internal fun colOf(x: Float) = col(x)

    @PublishedApi internal fun rowOf(y: Float) = row(y)

    @PublishedApi internal fun startOf(cell: Int) = cellStart[cell]

    @PublishedApi internal fun snakeAt(e: Int) = entrySnake[e]!!

    @PublishedApi internal fun segAt(e: Int) = entrySeg[e]
}

/** Snake [a]'s head touches body circle [seg] of snake [b] (seg 0 = b's head). */
class Contact(
    val a: Snake,
    val b: Snake,
    val seg: Int,
)

object Collisions {
    /**
     * Finds at most one contact per head: the touched circle closest to the
     * other snake's head (head-to-head wins over body), nearest first on ties.
     */
    fun findContacts(
        snakes: List<Snake>,
        grid: SpatialGrid,
        out: MutableList<Contact>,
    ) {
        out.clear()
        for (a in snakes) {
            if (!a.alive) continue
            val ar = a.radius
            var best: Snake? = null
            var bestSeg = Int.MAX_VALUE
            var bestD2 = Float.MAX_VALUE
            grid.query(a.x, a.y, ar + GameConfig.MAX_RADIUS) { b, seg ->
                if (b !== a) {
                    val reach = ar + b.segR[seg]
                    val d2 = dist2(a.x, a.y, b.segX[seg], b.segY[seg])
                    val closerToHead = seg < bestSeg || (seg == bestSeg && d2 < bestD2)
                    if (d2 < reach * reach && closerToHead) {
                        best = b
                        bestSeg = seg
                        bestD2 = d2
                    }
                }
            }
            best?.let { out.add(Contact(a, it, bestSeg)) }
        }
    }
}
