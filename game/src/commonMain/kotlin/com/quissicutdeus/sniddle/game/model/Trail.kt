package com.quissicutdeus.sniddle.game.model

/**
 * The path a head has travelled, newest point first, stored in a growable ring
 * buffer of primitive floats (no per-point allocation). The capacity is always
 * a power of two, so wrapping an index is a bit mask rather than a division.
 */
class Trail(
    initialCapacity: Int = 128,
) {
    private var xs = FloatArray(powerOfTwoAtLeast(initialCapacity))
    private var ys = FloatArray(xs.size)
    private var start = 0

    var size = 0
        private set

    private val capacity get() = xs.size
    private var mask = xs.size - 1

    /** Adds a newest point. */
    fun pushFront(
        x: Float,
        y: Float,
    ) {
        if (size == capacity) grow()
        start = (start - 1) and mask
        xs[start] = x
        ys[start] = y
        size++
    }

    /** Adds an oldest point (used to build an initial body behind a fresh snail). */
    fun pushBack(
        x: Float,
        y: Float,
    ) {
        if (size == capacity) grow()
        val i = (start + size) and mask
        xs[i] = x
        ys[i] = y
        size++
    }

    /** i = 0 is the newest point. */
    fun x(i: Int): Float = xs[(start + i) and mask]

    fun y(i: Int): Float = ys[(start + i) and mask]

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
        mask = newCap - 1
        start = 0
    }

    private companion object {
        fun powerOfTwoAtLeast(n: Int): Int = (maxOf(n, 2) - 1).takeHighestOneBit() shl 1
    }
}
