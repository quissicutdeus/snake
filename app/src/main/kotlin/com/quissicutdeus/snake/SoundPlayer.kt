package com.quissicutdeus.snake

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
 */
class SoundPlayer {
    @Volatile private var tracks: Map<Sound, AudioTrack>? = null

    @Volatile private var music: AudioTrack? = null

    @Volatile private var released = false

    /** Whether music was asked for; remembered so a request before loading finishes still counts. */
    @Volatile private var musicWanted = false

    init {
        thread(name = "sound-loader", isDaemon = true) {
            val loaded = Sound.entries.associateWith { track(Synth.render(it)) }
            val loop =
                track(Music.render()).also {
                    it.setLoopPoints(0, (Music.LOOP_SECONDS * Synth.SAMPLE_RATE).toInt(), -1)
                }
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

    private fun track(pcm: ShortArray): AudioTrack =
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
}
