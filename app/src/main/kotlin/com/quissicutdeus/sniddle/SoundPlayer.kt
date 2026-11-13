package com.quissicutdeus.sniddle

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.quissicutdeus.sniddle.game.audio.Music
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.audio.Synth
import kotlin.concurrent.thread

/**
 * Plays the synthesized sound effects and the looping background music. Each sound gets its own preloaded
 * ("static") AudioTrack, so starting one is instant and different sounds can
 * overlap. The samples are made on a background thread at startup; a sound
 * asked for before then is just skipped.
 *
 * Uses the game audio stream: the phone's media volume controls it, and it
 * needs no permission.
 *
 * Sound is never worth ending a game over. If the phone cannot make the tracks,
 * the game plays silently; if a track fails later (the audio system can
 * invalidate one), that sound or the music is skipped. Either way the failure
 * is logged as a warning.
 *
 * [newTrack] turns samples into a ready-to-play track; tests swap in a fake.
 */
class SoundPlayer(
    private val newTrack: (ShortArray) -> AudioTrack = ::staticTrack,
) {
    @Volatile private var tracks: Map<Sound, AudioTrack>? = null

    @Volatile private var music: AudioTrack? = null

    @Volatile private var released = false

    /** Whether music was asked for; remembered so a request before loading finishes still counts. */
    @Volatile private var musicWanted = false

    init {
        thread(name = "sound-loader", isDaemon = true) { load() }
    }

    /**
     * Makes every track, then hands them over. If making any of them fails, the
     * ones already made are released and none are handed over, so the player
     * stays silent, just as it is before loading finishes.
     */
    private fun load() {
        val made = mutableListOf<AudioTrack>()
        val built =
            audioSafely("load the sounds") {
                val loaded = Sound.entries.associateWith { sound -> newTrack(Synth.render(sound)).also { made += it } }
                val pcm = Music.render()
                val loop = newTrack(pcm).also { made += it }
                loopForever(loop, pcm.size)
                loaded to loop
            }
        synchronized(this) {
            if (released || built == null) {
                made.forEach { it.release() }
                return
            }
            val (loaded, loop) = built
            tracks = loaded
            music = loop
            if (musicWanted) audioSafely("start the music") { loop.play() }
        }
    }

    /** Starts (from where it left off) or pauses the background music loop. */
    fun music(on: Boolean) {
        synchronized(this) {
            musicWanted = on
            val m = music ?: return
            audioSafely(if (on) "start the music" else "pause the music") {
                if (on && m.playState != AudioTrack.PLAYSTATE_PLAYING) m.play()
                if (!on && m.playState == AudioTrack.PLAYSTATE_PLAYING) m.pause()
            }
        }
    }

    /** Plays [sound] from its start, cutting off the same sound if it is still going. */
    fun play(sound: Sound) {
        val t = tracks?.get(sound) ?: return
        audioSafely("play $sound") {
            // Restart from the beginning, even if it is still playing.
            if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    fun release() {
        synchronized(this) {
            released = true
            tracks?.values?.forEach { it.release() }
            tracks = null
            music?.release()
            music = null
        }
    }

    /**
     * Makes [track] repeat its [frames] forever. The track's own loop points do
     * that; if a device refuses them, a marker at the end rewinds it instead, so
     * the tune never plays once and then goes quiet.
     */
    private fun loopForever(
        track: AudioTrack,
        frames: Int,
    ) {
        if (track.setLoopPoints(0, frames, LOOP_FOREVER) == AudioTrack.SUCCESS) return
        track.setNotificationMarkerPosition(frames)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) = rewind(t, frames)

                override fun onPeriodicNotification(t: AudioTrack) = Unit
            },
            Handler(Looper.getMainLooper()),
        )
    }

    /**
     * Back to the start of the tune, and playing again if music is still wanted.
     * A track that ran off its end still says it is playing, so it is rewound
     * even when paused: the next [music] (true) then starts it from the top.
     * (Internal rather than private so the marker listener can call it directly.)
     */
    internal fun rewind(
        track: AudioTrack,
        frames: Int,
    ) {
        synchronized(this) {
            // A marker can still arrive after release; that track must not be touched.
            if (released || track !== music) return
            audioSafely("rewind the music") {
                track.stop()
                track.reloadStaticData()
                track.setNotificationMarkerPosition(frames)
                if (musicWanted) track.play()
            }
        }
    }

    /**
     * Runs [block] and returns its result, or logs a warning and returns null if
     * the audio system refuses it. These are the exceptions AudioTrack throws for a
     * device that cannot make a track or a track that has stopped working: the
     * Builder's UnsupportedOperationException, and the IllegalStateException or
     * IllegalArgumentException of a call on a track in the wrong state. Anything
     * else, such as an Error like running out of memory, still propagates.
     */
    private fun <T> audioSafely(
        what: String,
        block: () -> T,
    ): T? =
        try {
            block()
        } catch (e: UnsupportedOperationException) {
            warn(what, e)
        } catch (e: IllegalStateException) {
            warn(what, e)
        } catch (e: IllegalArgumentException) {
            warn(what, e)
        }

    private fun warn(
        what: String,
        e: RuntimeException,
    ): Nothing? {
        Log.w(TAG, "Could not $what; carrying on without it", e)
        return null
    }

    private companion object {
        /** setLoopPoints' loop count for "repeat until stopped". */
        const val LOOP_FOREVER = -1

        const val TAG = "SoundPlayer"
    }
}

private fun staticTrack(pcm: ShortArray): AudioTrack =
    AudioTrack
        .Builder()
        .setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).setAudioFormat(
            AudioFormat
                .Builder()
                .setSampleRate(Synth.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        ).setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(pcm.size * 2)
        .build()
        .also { track ->
            // A static track missing samples is no use. If it is not handed back, free it here: nothing else can.
            var loaded = false
            try {
                val written = track.write(pcm, 0, pcm.size)
                check(written == pcm.size) { "AudioTrack took $written of ${pcm.size} samples" }
                loaded = true
            } finally {
                if (!loaded) track.release()
            }
        }
