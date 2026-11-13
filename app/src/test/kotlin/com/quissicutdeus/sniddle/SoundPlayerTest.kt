package com.quissicutdeus.sniddle

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.audio.Synth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

/**
 * The background music must keep looping even on a phone that refuses
 * AudioTrack loop points: then a marker at the end of the tune rewinds it.
 * And a phone whose audio system refuses tracks must get a silent game, not a
 * crash. Robolectric's AudioTrack ignores loop points and markers, so these
 * tests use a fake track that records what the player asks of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SoundPlayerTest {
    @Test
    fun loopPointsThatWorkNeedNoMarker() {
        val (_, music) = load(AudioTrack.SUCCESS)
        assertNull(music.listener)
    }

    @Test
    fun refusedLoopPointsRewindAndPlayAgainAtTheEnd() {
        val (_, music) = load(AudioTrack.ERROR_INVALID_OPERATION)
        music.reachEnd()
        assertEquals(listOf("stop", "reload", "marker ${music.frames}", "play"), music.events.toList())
    }

    @Test
    fun pausedMusicRewindsWithoutPlaying() {
        val (player, music) = load(AudioTrack.ERROR_INVALID_OPERATION)
        player.music(false)
        music.events.clear()
        music.reachEnd()
        assertEquals(listOf("stop", "reload", "marker ${music.frames}"), music.events.toList())
    }

    @Test
    fun releasedMusicIsLeftAlone() {
        val (player, music) = load(AudioTrack.ERROR_INVALID_OPERATION)
        player.release()
        music.events.clear()
        music.reachEnd()
        assertEquals(emptyList<String>(), music.events.toList())
    }

    @Test
    fun firstTrackRefusedLeavesTheGameSilent() = refusedTrackLeavesTheGameSilent(failingTrack = 0)

    @Test
    fun musicTrackRefusedLeavesTheGameSilent() = refusedTrackLeavesTheGameSilent(failingTrack = Sound.entries.size)

    @Test
    fun tracksThatStopWorkingMidGameAreSkipped() {
        val (player, music) = load(AudioTrack.ERROR_INVALID_OPERATION)
        made.forEach { it.broken = true }
        Sound.entries.forEach { player.play(it) }
        player.music(false)
        player.music(true)
        // Not one of these may throw: a missing sound must not end the game.
        music.reachEnd()
    }

    /**
     * The audio system refuses to make the track at index [failingTrack] (the
     * sounds come first, then the music). Loading must give up without a crash,
     * release every track it had made, and leave the player doing nothing.
     */
    private fun refusedTrackLeavesTheGameSilent(failingTrack: Int) {
        val loader = AtomicReference<Thread>()
        val escaped = AtomicReference<Throwable>()
        val player =
            SoundPlayer { pcm ->
                if (made.size == failingTrack) {
                    // On a phone an exception escaping this thread ends the app; here it would only be printed.
                    Thread.currentThread().setUncaughtExceptionHandler { _, e -> escaped.set(e) }
                    loader.set(Thread.currentThread())
                    throw UnsupportedOperationException("no track for you")
                }
                FakeTrack(pcm.size, AudioTrack.SUCCESS).also { made += it }
            }
        player.music(true)
        val deadline = System.nanoTime() + LOAD_TIMEOUT_NANOS
        while (loader.get() == null) {
            check(System.nanoTime() < deadline) { "the loader never asked for the failing track" }
            Thread.sleep(POLL_MILLIS)
        }
        loader.get().join(LOAD_TIMEOUT_MILLIS)
        check(!loader.get().isAlive) { "the loader never finished" }
        assertNull(escaped.get())
        assertEquals(failingTrack, made.size)
        made.forEach { assertTrue("a track made before the failure was not released", it.released) }
        made.forEach { it.events.clear() }
        Sound.entries.forEach { player.play(it) }
        player.music(false)
        player.music(true)
        player.release()
        assertEquals(emptyList<String>(), made.flatMap { it.events.toList() })
    }

    /** Every track the player under test has made, in order. */
    private val made: MutableList<FakeTrack> = Collections.synchronizedList(mutableListOf())

    /** A player whose music has loaded and started, with the music track's history cleared. */
    private fun load(loopResult: Int): Pair<SoundPlayer, FakeTrack> {
        val player = SoundPlayer { pcm -> FakeTrack(pcm.size, loopResult).also { made += it } }
        player.music(true)
        val deadline = System.nanoTime() + LOAD_TIMEOUT_NANOS
        while (true) {
            val music = synchronized(made) { made.firstOrNull { it.loopPoints != null } }
            if (music != null && "play" in music.events) {
                assertEquals(listOf(0, music.frames, LOOP_FOREVER), music.loopPoints)
                music.events.clear()
                return player to music
            }
            check(System.nanoTime() < deadline) { "the music never loaded and started" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    private class FakeTrack(
        val frames: Int,
        private val loopResult: Int,
    ) : AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build(),
            AudioFormat
                .Builder()
                .setSampleRate(Synth.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            frames * 2,
            MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        ) {
        val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

        @Volatile var loopPoints: List<Int>? = null

        @Volatile var listener: OnPlaybackPositionUpdateListener? = null

        /** Whether the audio system has invalidated this track, so that using it throws. */
        @Volatile var broken = false

        @Volatile var released = false

        @Volatile private var state = PLAYSTATE_STOPPED

        /** What the audio system does when playback passes the marker. */
        fun reachEnd() = checkNotNull(listener) { "no marker listener was set" }.onMarkerReached(this)

        override fun setLoopPoints(
            startInFrames: Int,
            endInFrames: Int,
            loopCount: Int,
        ): Int {
            loopPoints = listOf(startInFrames, endInFrames, loopCount)
            return loopResult
        }

        override fun setNotificationMarkerPosition(markerInFrames: Int): Int {
            events += "marker $markerInFrames"
            return SUCCESS
        }

        override fun setPlaybackPositionUpdateListener(
            listener: OnPlaybackPositionUpdateListener?,
            handler: Handler?,
        ) {
            this.listener = listener
        }

        override fun getPlayState(): Int = state

        override fun play() {
            checkNotBroken()
            events += "play"
            state = PLAYSTATE_PLAYING
        }

        override fun pause() {
            checkNotBroken()
            events += "pause"
            state = PLAYSTATE_PAUSED
        }

        override fun stop() {
            checkNotBroken()
            events += "stop"
            state = PLAYSTATE_STOPPED
        }

        override fun reloadStaticData(): Int {
            checkNotBroken()
            events += "reload"
            return SUCCESS
        }

        override fun release() {
            released = true
            super.release()
        }

        private fun checkNotBroken() = check(!broken) { "the audio system invalidated this track" }
    }

    private companion object {
        const val LOOP_FOREVER = -1
        const val POLL_MILLIS = 10L
        const val LOAD_TIMEOUT_NANOS = 30_000_000_000L
        const val LOAD_TIMEOUT_MILLIS = 30_000L
    }
}
