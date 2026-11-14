package com.quissicutdeus.sniddle

import androidx.compose.ui.test.onNodeWithContentDescription
import com.quissicutdeus.sniddle.ui.Settings
import com.quissicutdeus.sniddle.ui.SettingsButtons
import com.quissicutdeus.sniddle.ui.SniddleTheme
import org.junit.Test

/** iPads can't buzz: there the game shows no buzz switch, just music and sound. */
class BuzzSwitchTest : ScreenTest() {
    @Test fun aDeviceThatCanBuzzHasTheBuzzSwitch() {
        compose.setContent { SniddleTheme { SettingsButtons(Settings(), {}) } }
        compose.onNodeWithContentDescription("Buzz on. Tap to turn it off.").assertExists()
        compose.onNodeWithContentDescription("Sound on. Tap to turn it off.").assertExists()
    }

    @Test fun aDeviceThatCantBuzzHasNoBuzzSwitch() {
        compose.setContent { SniddleTheme(canBuzz = false) { SettingsButtons(Settings(), {}) } }
        compose.onNodeWithContentDescription("Buzz on. Tap to turn it off.").assertDoesNotExist()
        compose.onNodeWithContentDescription("Music on. Tap to turn it off.").assertExists()
        compose.onNodeWithContentDescription("Sound on. Tap to turn it off.").assertExists()
    }
}
