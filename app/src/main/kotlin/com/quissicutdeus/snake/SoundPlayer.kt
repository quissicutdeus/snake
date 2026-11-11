package com.quissicutdeus.snake

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.quissicutdeus.snake.game.audio.Music
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.Synth
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
        thread(name = "sound-loader", isDaemon = true) {
            val loaded = Sound.entries.associateWith { newTrack(Synth.render(it)) }
            val pcm = Music.render()
            val loop = newTrack(pcm).also { loopForever(it, pcm.size) }
            synchronized(this) {
                if (released) {
                    loaded.values.forEach { it.release() }
                    loop.release()
                } else {
                    tracks = loaded
                    music = loop
                    if (musicWanted) loop.play()
                }
            }
        }
    }

    /** Starts (from where it left off) or pauses the background music loop. */
    fun music(on: Boolean) {
        synchronized(this) {
            musicWanted = on
            val m = music ?: return
            if (on && m.playState != AudioTrack.PLAYSTATE_PLAYING) m.play()
            if (!on && m.playState == AudioTrack.PLAYSTATE_PLAYING) m.pause()
        }
    }

    fun play(sound: Sound) {
        val t = tracks?.get(sound) ?: return
        // Restart from the beginning, even if it is still playing.
        if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
        t.reloadStaticData()
        t.play()
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
            track.stop()
            track.reloadStaticData()
            track.setNotificationMarkerPosition(frames)
            if (musicWanted) track.play()
        }
    }

    private companion object {
        /** setLoopPoints' loop count for "repeat until stopped". */
        const val LOOP_FOREVER = -1
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
        .also { it.write(pcm, 0, pcm.size) }
