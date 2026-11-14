package com.quissicutdeus.sniddle

import androidx.compose.runtime.saveable.SaveableStateRegistry
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillTerminateNotification

/**
 * What the screens keep with rememberSaveable, kept across iOS closing the app.
 *
 * Android keeps that state when it closes an app in the background, so a kid
 * coming back finds their game (paused) where they left it. iOS throws it
 * away, so this registry stands in for Android's: it writes the state to the
 * app's user defaults whenever the app goes to the background, and hands it
 * back when the app starts again. The screens don't change at all.
 *
 * iOS can't tell a kid swiping the app away from the system closing it, so a
 * save is only used if the app was left less than [FRESH_FOR_SECONDS] ago:
 * back after lunch, the game is there; the next day, Sniddle starts at the
 * title screen. A save from another build is ignored too (the weekly reinstall
 * often brings a new one): a screen that changed what it saves must not be
 * handed the old build's values.
 */
internal class PersistedUiState(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    val registry: SaveableStateRegistry = SaveableStateRegistry(restored(), canBeSaved = { true })

    init {
        val center = NSNotificationCenter.defaultCenter
        val main = NSOperationQueue.mainQueue
        center.addObserverForName(UIApplicationDidEnterBackgroundNotification, `object` = null, queue = main) { _ -> save() }
        center.addObserverForName(UIApplicationWillTerminateNotification, `object` = null, queue = main) { _ -> save() }
    }

    private fun restored(): Map<String, List<Any?>>? {
        val text = defaults.stringForKey(KEY_STATE) ?: return null
        val age = now() - defaults.doubleForKey(KEY_SAVED_AT)
        val sameBuild = defaults.stringForKey(KEY_BUILD) == build()
        return if (sameBuild && age in 0.0..FRESH_FOR_SECONDS) SavedUiState.decode(text) else null
    }

    private fun save() {
        defaults.setObject(SavedUiState.encode(registry.performSave()), forKey = KEY_STATE)
        defaults.setDouble(now(), forKey = KEY_SAVED_AT)
        defaults.setObject(build(), forKey = KEY_BUILD)
    }

    /** This build's number (CFBundleVersion: CI's run number, so every build differs). */
    private fun build(): String = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""

    private fun now() = NSDate().timeIntervalSince1970

    private companion object {
        // Separate from the game's own saved data (Storage), which never expires.
        const val KEY_STATE = "ios.uiState"
        const val KEY_SAVED_AT = "ios.uiStateSavedAt"
        const val KEY_BUILD = "ios.uiStateBuild"

        /** Twelve hours. */
        const val FRESH_FOR_SECONDS = 12.0 * 60 * 60
    }
}
