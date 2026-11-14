package com.quissicutdeus.sniddle

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.quissicutdeus.sniddle.game.render.Buzz

/**
 * Android's [Buzzer]: system haptics on the app's [view]. No permission
 * needed, and they follow the phone's own "vibration & haptics" setting, so
 * turning that off silences the game too (as does the game's own buzz switch).
 * Android 8 to 10 have no CONFIRM and REJECT buzzes: there the cheer and the
 * gobble use the long-press buzz instead.
 */
class ViewBuzzer(
    private val view: View,
) : Buzzer {
    override fun buzz(buzz: Buzz) {
        view.performHapticFeedback(hapticFor(buzz))
    }

    private fun hapticFor(buzz: Buzz): Int {
        val hasConfirmReject = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        return when (buzz) {
            Buzz.TAP -> HapticFeedbackConstants.CLOCK_TICK
            Buzz.THUD -> HapticFeedbackConstants.VIRTUAL_KEY
            Buzz.CHEER -> if (hasConfirmReject) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
            Buzz.OUCH -> HapticFeedbackConstants.LONG_PRESS
            Buzz.GOBBLED -> if (hasConfirmReject) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        }
    }
}
