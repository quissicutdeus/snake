package com.quissicutdeus.snake

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Android closing the app in the background and the kid coming back, for a
 * test clock that only moves when told to. Compose's StateRestorationTester
 * waits for the UI to go idle, which a running game never does with the clock
 * stopped, so the old screen is never torn down and nothing is really saved
 * or restored. Here the content is saved, removed and put back with that
 * state across frames the test steps through itself.
 */
class SavedAndRestored {
    internal var saved: Map<String, List<Any?>>? = null
        private set
    internal var shown by mutableStateOf(true)
        private set
    internal var registry: SaveableStateRegistry? = null

    /** Saves everything, takes it off the screen, then brings it back; [frames] steps the clock in between. */
    fun closeAndReopen(frames: () -> Unit) {
        saved = checkNotNull(registry).performSave()
        shown = false
        Snapshot.sendApplyNotifications()
        frames()
        shown = true
        Snapshot.sendApplyNotifications()
        frames()
    }
}

/** Shows [content] so that [app] can save it, take it away and bring it back. */
@Composable
fun Restorable(
    app: SavedAndRestored,
    content: @Composable () -> Unit,
) {
    if (!app.shown) return
    // A fresh registry each time it comes back (the keys inside must stay the same, so no key()).
    val r = remember { SaveableStateRegistry(app.saved) { true } }
    app.registry = r
    CompositionLocalProvider(LocalSaveableStateRegistry provides r) { content() }
}
