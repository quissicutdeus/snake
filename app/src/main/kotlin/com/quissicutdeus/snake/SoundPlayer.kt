package com.quissicutdeus.snake

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.Synth
import kotlin.concurrent.thread

/**
 * Plays the synthesized sound effects. Each sound gets its own preloaded
 * ("static") AudioTrack, so starting one is instant and different sounds can
 * overlap. The samples are made on a background thread at startup; a sound
 * asked for before then is just skipped.
 *
 * Uses the game audio stream: the phone's media volume controls it, and it
 * needs no permission.
 */
class SoundPlayer {
    @Volatile private var tracks: Map<Sound, AudioTrack>? = null

    @Volatile private var released = false

    init {
        thread(name = "sound-loader", isDaemon = true) {
            val loaded = Sound.entries.associateWith { track(Synth.render(it)) }
            synchronized(this) {
                if (released) loaded.values.forEach { it.release() } else tracks = loaded
            }
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
