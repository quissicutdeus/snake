package com.quissicutdeus.sniddle

import com.quissicutdeus.sniddle.game.render.Buzz
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/**
 * iOS's [Buzzer]: the Taptic Engine through UIKit's feedback generators, the
 * counterpart of Android's system haptics. No permission needed, and they
 * follow the phone's own Sounds & Haptics setting. iPads have no Taptic
 * Engine, so there these do nothing. Called on the main thread (from the UI).
 */
class IosBuzzer : Buzzer {
    private val tap = UISelectionFeedbackGenerator()
    private val thud = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val ouch = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notify = UINotificationFeedbackGenerator()

    override fun buzz(buzz: Buzz) {
        when (buzz) {
            Buzz.TAP -> tap.selectionChanged()
            Buzz.THUD -> thud.impactOccurred()
            Buzz.CHEER -> notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            Buzz.OUCH -> ouch.impactOccurred()
            Buzz.GOBBLED -> notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
        }
    }
}
