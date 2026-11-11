package com.quissicutdeus.sniddle.game.audio

import kotlin.math.pow

/**
 * A short, bouncy background tune that loops during a game: eight bars of
 * C - Am - F - G at 120 beats a minute, a triangle-wave melody over a soft
 * bass and a light tick on the off-beats. Made in code like the effects.
 * Every note dies away before the loop ends, so the seam doesn't click.
 */
object Music {
    private const val BEAT = 0.5f
    private const val EIGHTH = BEAT / 2f
    private const val BAR = BEAT * 4f
    private const val BARS = 8

    /** Length of one time round the loop, in seconds. */
    const val LOOP_SECONDS = BAR * BARS

    /** Peak loudness (of 1.0): well under the effects, so a food pop still cuts through. */
    private const val LEVEL = 0.16f

    // Bass roots, one chord per bar (MIDI note numbers): C2 A1 F1 G1, twice.
    private val ROOTS = intArrayOf(48, 45, 41, 43, 48, 45, 41, 43)

    // Melody in eighth notes, one line per bar; 0 is a rest.
    private val MELODY =
        intArrayOf(
            67,
            0,
            64,
            67,
            69,
            67,
            64,
            0,
            69,
            0,
            72,
            69,
            67,
            64,
            67,
            0,
            69,
            0,
            72,
            74,
            72,
            69,
            67,
            0,
            67,
            71,
            74,
            71,
            67,
            0,
            62,
            0,
            76,
            0,
            74,
            72,
            67,
            0,
            64,
            67,
            69,
            72,
            76,
            0,
            74,
            72,
            69,
            0,
            65,
            69,
            72,
            77,
            76,
            72,
            69,
            0,
            67,
            71,
            74,
            0,
            72,
            0,
            0,
            0,
        )

    fun render(): ShortArray {
        val s = Synth.Builder()
        for (bar in 0 until BARS) {
            val t0 = bar * BAR
            // Bass on beats 1 and 3.
            for (beat in intArrayOf(0, 2)) {
                val f = hz(ROOTS[bar])
                s.tone(t0 + beat * BEAT, 0.45f, f, f, Synth.Wave.TRIANGLE, 0.55f, decay = 5f)
            }
            for (i in 0 until 8) {
                val note = MELODY[bar * 8 + i]
                val start = t0 + i * EIGHTH
                if (note != 0) s.tone(start, 0.22f, hz(note), hz(note), Synth.Wave.TRIANGLE, 0.45f, decay = 7f)
                // A soft tick on every off-beat.
                if (i % 2 == 1) s.noise(start, 0.03f, 0.06f, decay = 90f)
            }
        }
        s.padTo(LOOP_SECONDS)
        return s.toPcm(LEVEL)
    }

    private fun hz(midi: Int): Float = 440f * 2f.pow((midi - 69) / 12f)
}
