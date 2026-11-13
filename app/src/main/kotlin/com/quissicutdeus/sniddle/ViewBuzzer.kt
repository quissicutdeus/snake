package com.quissicutdeus.sniddle

import android.view.HapticFeedbackConstants
import android.view.View
import com.quissicutdeus.sniddle.game.render.Buzz

/**
 * Android's [Buzzer]: system haptics on the app's [view]. No permission
 * needed, and they follow the phone's own "vibration & haptics" setting, so
 * turning that off silences the game too (as does the game's own buzz switch).
 */
class ViewBuzzer(
    private val view: View,
) : Buzzer {
    override fun buzz(buzz: Buzz) {
        view.performHapticFeedback(hapticFor(buzz))
    }

    private fun hapticFor(buzz: Buzz): Int =
        when (buzz) {
            Buzz.TAP -> HapticFeedbackConstants.CLOCK_TICK
            Buzz.THUD -> HapticFeedbackConstants.VIRTUAL_KEY
            Buzz.CHEER -> HapticFeedbackConstants.CONFIRM
            Buzz.OUCH -> HapticFeedbackConstants.LONG_PRESS
            Buzz.GOBBLED -> HapticFeedbackConstants.REJECT
        }
}
