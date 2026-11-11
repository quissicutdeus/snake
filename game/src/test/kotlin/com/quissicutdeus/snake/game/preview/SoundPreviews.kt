package com.quissicutdeus.snake.game.preview

import com.quissicutdeus.snake.game.audio.Music
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.Synth
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes every sound effect, and the music loop, as a WAV file into the directory given as the first argument. */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "build/sounds")
    dir.mkdirs()
    for (s in Sound.entries) {
        val file = File(dir, "${s.name.lowercase()}.wav")
        file.writeBytes(wav(Synth.render(s)))
        println(file)
    }
    val music = File(dir, "music_loop.wav")
    music.writeBytes(wav(Music.render()))
    println(music)
}

/** 16-bit mono PCM in a minimal RIFF/WAVE container. */
private fun wav(pcm: ShortArray): ByteArray {
    val data = pcm.size * 2
    val b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
    b
        .put("fmt ".toByteArray())
        .putInt(16)
        .putShort(1)
        .putShort(1)
    b
        .putInt(Synth.SAMPLE_RATE)
        .putInt(Synth.SAMPLE_RATE * 2)
        .putShort(2)
        .putShort(16)
    b.put("data".toByteArray()).putInt(data)
    for (v in pcm) b.putShort(v)
    return b.array()
}
