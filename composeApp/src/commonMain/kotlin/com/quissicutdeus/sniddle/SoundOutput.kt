package com.quissicutdeus.sniddle

import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.render.Buzz

/** Where the game's sound effects and music go: on Android, [SoundPlayer]. */
interface SoundOutput {
    /** Plays [sound] from its start. */
    fun play(sound: Sound)

    /** Starts (from where it left off) or pauses the background music. */
    fun music(on: Boolean)
}

/**
 * The phone's little buzz for the big moments: on Android, the system haptics
 * ([ViewBuzzer]), which follow the phone's own vibration setting. The game's
 * own buzz switch is checked before this is asked.
 */
fun interface Buzzer {
    fun buzz(buzz: Buzz)
}
