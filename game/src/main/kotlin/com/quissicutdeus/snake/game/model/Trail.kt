package com.quissicutdeus.snake.game.model

/**
 * The path a head has travelled, newest point first, stored in a growable ring
 * buffer of primitive floats (no per-point allocation).
 */
class Trail(
    initialCapacity: Int = 128,
) {
    private var xs = FloatArray(initialCapacity)
    private var ys = FloatArray(initialCapacity)
    private var start = 0

    var size = 0
        private set

    private val capacity get() = xs.size

    /** Adds a newest point. */
    fun pushFront(
        x: Float,
        y: Float,
    ) {
        if (size == capacity) grow()
        start = (start - 1 + capacity) % capacity
        xs[start] = x
        ys[start] = y
        size++
    }

    /** Adds an oldest point (used to build an initial body behind a fresh snake). */
    fun pushBack(
        x: Float,
        y: Float,
    ) {
        if (size == capacity) grow()
        val i = (start + size) % capacity
        xs[i] = x
        ys[i] = y
        size++
    }

    /** i = 0 is the newest point. */
    fun x(i: Int): Float = xs[(start + i) % capacity]

    fun y(i: Int): Float = ys[(start + i) % capacity]

    /** Keeps only the [newSize] newest points. */
    fun truncate(newSize: Int) {
        if (newSize < size) size = newSize.coerceAtLeast(0)
    }

    fun clear() {
        size = 0
        start = 0
    }

    private fun grow() {
        val newCap = capacity * 2
        val nx = FloatArray(newCap)
        val ny = FloatArray(newCap)
        for (i in 0 until size) {
            nx[i] = x(i)
            ny[i] = y(i)
        }
        xs = nx
        ys = ny
        start = 0
    }
}
