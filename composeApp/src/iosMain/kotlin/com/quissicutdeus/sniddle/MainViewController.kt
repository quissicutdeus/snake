package com.quissicutdeus.sniddle

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.quissicutdeus.sniddle.ui.SniddleApp
import platform.UIKit.UIViewController

/**
 * The whole app for iOS, hosted by SniddleApp.swift: the shared UI with iOS's
 * own saved data, calendar, sound and haptics, as MainActivity does on Android.
 */
@Suppress("FunctionName") // Swift calls it by this name (see iosApp/project.yml).
fun MainViewController(): UIViewController {
    val storage = Storage(UserDefaultsStore())
    val sound = IosSound()
    return ComposeUIViewController {
        SniddleApp(storage, IosCalendar, sound, remember { IosBuzzer() })
    }
}
