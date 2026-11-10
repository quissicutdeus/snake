package com.quissicutdeus.snake.game.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Every sound effect in the game. */
enum class Sound {
    /** Eating a food dot (three pitches, so a run of them doesn't drone). */
    PELLET_LOW,
    PELLET_MID,
    PELLET_HIGH,

    /** Bumping the fence. */
    WALL,

    /** Two quick bites: the player ate a snake. */
    CHOMP,

    /** The player cut a tail. */
    SNIP,

    /** Bounced off a snake about the same size. */
    BOING,

    /** Super Armor stopped a hit. */
    CLANG,

    /** Hurt, or lost the tail. */
    OUCH,

    /** Picked up a power-up: a rising arpeggio. */
    POWER_UP,

    /** Gobbled: a slide down and a sad "wah-wah". */
    GOBBLED,
}

/**
 * Makes the game's sound effects from scratch (sine, triangle and square waves
 * plus noise), so there are no audio files to license or ship. Pure Kotlin: the
 * app plays the samples, tests check them, and they always come out the same.
 */
object Synth {
    const val SAMPLE_RATE = 22_050

    /** Peak loudness (of 1.0): effects sit under whatever the phone is playing. */
    private const val MASTER = 0.55f

    /** Fade in/out this long so no sound starts or ends with a click. */
    private const val EDGE_SECONDS = 0.004f

    fun render(sound: Sound): ShortArray {
        val s = Builder()
        when (sound) {
            Sound.PELLET_LOW -> pellet(s, 1f)
            Sound.PELLET_MID -> pellet(s, 1.19f)
            Sound.PELLET_HIGH -> pellet(s, 1.41f)
            Sound.WALL -> s.tone(0f, 0.07f, 140f, 70f, Wave.SINE, 0.7f, decay = 40f)
            Sound.CHOMP -> chomp(s)
            Sound.SNIP -> snip(s)
            Sound.BOING -> boing(s)
            Sound.CLANG -> clang(s)
            Sound.OUCH -> s.tone(0f, 0.26f, 620f, 240f, Wave.SQUARE, 0.35f, decay = 6f)
            Sound.POWER_UP -> powerUp(s)
            Sound.GOBBLED -> gobbled(s)
        }
        return s.toPcm(MASTER * loudness(sound))
    }

    /** Relative loudness: the constant little sounds stay soft, the big moments don't. */
    private fun loudness(sound: Sound): Float =
        when (sound) {
            Sound.PELLET_LOW, Sound.PELLET_MID, Sound.PELLET_HIGH -> 0.35f
            Sound.WALL -> 0.5f
            Sound.SNIP, Sound.BOING, Sound.CLANG -> 0.8f
            Sound.CHOMP, Sound.OUCH, Sound.POWER_UP, Sound.GOBBLED -> 1f
        }

    private fun pellet(
        s: Builder,
        pitch: Float,
    ) = s.tone(0f, 0.07f, 700f * pitch, 1100f * pitch, Wave.SINE, 0.45f, decay = 30f)

    private fun chomp(s: Builder) {
        for (start in floatArrayOf(0f, 0.11f)) {
            s.tone(start, 0.09f, 300f, 130f, Wave.SQUARE, 0.35f, decay = 18f)
            s.noise(start, 0.03f, 0.35f, decay = 60f)
        }
    }

    private fun snip(s: Builder) {
        s.noise(0f, 0.05f, 0.5f, decay = 70f)
        s.tone(0.01f, 0.06f, 2200f, 1500f, Wave.TRIANGLE, 0.35f, decay = 40f)
    }

    private fun boing(s: Builder) =
        s.tone(0f, 0.4f, 0f, 0f, Wave.SINE, 0.8f, decay = 7f) { t ->
            // A spring: wobbling pitch that settles while rising a little.
            170f + 140f * t + 90f * sin(2f * PI.toFloat() * 11f * t) * exp(-5f * t)
        }

    private fun clang(s: Builder) {
        // Inharmonic partials ring like metal.
        for ((freq, level) in listOf(523f to 0.35f, 1307f to 0.25f, 2011f to 0.18f, 2790f to 0.12f)) {
            s.tone(0f, 0.45f, freq, freq, Wave.SINE, level, decay = 9f)
        }
        s.noise(0f, 0.02f, 0.3f, decay = 120f)
    }

    private fun powerUp(s: Builder) {
        val notes = floatArrayOf(523f, 659f, 784f, 1047f) // C E G C
        notes.forEachIndexed { i, f ->
            s.tone(i * 0.07f, if (i == notes.lastIndex) 0.22f else 0.08f, f, f, Wave.TRIANGLE, 0.5f, decay = 8f)
        }
    }

    private fun gobbled(s: Builder) {
        s.tone(0f, 0.38f, 420f, 90f, Wave.TRIANGLE, 0.6f, decay = 2f)
        s.noise(0f, 0.3f, 0.12f, decay = 8f)
        // "Wah-wah": two sad notes that sag.
        s.tone(0.42f, 0.2f, 392f, 370f, Wave.SQUARE, 0.25f, decay = 4f)
        s.tone(0.64f, 0.34f, 330f, 300f, Wave.SQUARE, 0.25f, decay = 3f)
    }

    private enum class Wave { SINE, TRIANGLE, SQUARE }

    /** Mixes notes and noise bursts into a float buffer that grows as needed. */
    private class Builder {
        private var buf = FloatArray(SAMPLE_RATE)
        private var length = 0
        private val rng = Random(SEED)

        private fun mix(
            start: Float,
            seconds: Float,
            sample: (i: Int, t: Float) -> Float,
        ) {
            val first = (start * SAMPLE_RATE).roundToInt()
            val n = (seconds * SAMPLE_RATE).roundToInt()
            val end = first + n
            if (end > buf.size) buf = buf.copyOf(maxOf(end, buf.size * 2))
            val edge = (EDGE_SECONDS * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
            for (i in 0 until n) {
                val fade = min(1f, min(i, n - 1 - i).toFloat() / edge)
                buf[first + i] += sample(i, i.toFloat() / SAMPLE_RATE) * fade
            }
            length = maxOf(length, end)
        }

        /**
         * A note sliding from [from] to [to] Hz (or following [freqAt]),
         * dying away at [decay] per second.
         */
        fun tone(
            start: Float,
            seconds: Float,
            from: Float,
            to: Float,
            wave: Wave,
            level: Float,
            decay: Float,
            freqAt: ((Float) -> Float)? = null,
        ) {
            var phase = 0f
            mix(start, seconds) { _, t ->
                val f = freqAt?.invoke(t) ?: (from + (to - from) * (t / seconds))
                phase = (phase + f / SAMPLE_RATE) % 1f
                level * exp(-decay * t) * shape(wave, phase)
            }
        }

        fun noise(
            start: Float,
            seconds: Float,
            level: Float,
            decay: Float,
        ) = mix(start, seconds) { _, t -> level * exp(-decay * t) * (rng.nextFloat() * 2f - 1f) }

        /** Scaled so the loudest sample is [peakLevel] of full scale. */
        fun toPcm(peakLevel: Float): ShortArray {
            var peak = 0f
            for (i in 0 until length) peak = maxOf(peak, abs(buf[i]))
            val gain = if (peak > 0f) peakLevel / peak else 0f
            return ShortArray(length) { (buf[it] * gain * Short.MAX_VALUE).roundToInt().toShort() }
        }

        private fun shape(
            wave: Wave,
            phase: Float,
        ): Float =
            when (wave) {
                Wave.SINE -> sin(2f * PI.toFloat() * phase)

                Wave.TRIANGLE -> 4f * abs(phase - 0.5f) - 1f

                // Softened square (fewer harsh harmonics than a hard one).
                Wave.SQUARE -> (sin(2f * PI.toFloat() * phase) * 3f).coerceIn(-1f, 1f)
            }
    }

    private const val SEED = 7
}
