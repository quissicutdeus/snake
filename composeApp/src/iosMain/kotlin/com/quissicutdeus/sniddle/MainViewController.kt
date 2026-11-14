package com.quissicutdeus.sniddle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.window.ComposeUIViewController
import com.quissicutdeus.sniddle.ui.SniddleApp
import platform.UIKit.UIViewController

/**
 * The whole app for iOS, hosted by SniddleApp.swift: the shared UI with iOS's
 * own saved data, calendar, sound and haptics, as MainActivity does on Android,
 * and the screens' saved state kept across iOS closing the app ([PersistedUiState]).
 */
@Suppress("FunctionName") // Swift calls it by this name (see iosApp/project.yml).
fun MainViewController(): UIViewController {
    val storage = Storage(UserDefaultsStore())
    val sound = IosSound()
    val uiState = PersistedUiState()
    return ComposeUIViewController {
        CompositionLocalProvider(LocalSaveableStateRegistry provides uiState.registry) {
            SniddleApp(storage, IosCalendar, sound, remember { IosBuzzer() })
        }
    }
}
