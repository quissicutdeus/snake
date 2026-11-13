package com.quissicutdeus.sniddle

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android's side of [KeyValueStore]: the app's SharedPreferences file, read
 * and written exactly as before there was a [KeyValueStore], so phones that
 * update keep their high scores, kids, looks and settings.
 */
class SharedPreferencesStore(
    private val prefs: SharedPreferences,
) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getInt(
        key: String,
        default: Int,
    ): Int = prefs.getInt(key, default)

    override fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = prefs.getBoolean(key, default)

    override fun getStringSet(key: String): Set<String>? = prefs.getStringSet(key, null)?.toSet()

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun edit(changes: KeyValueStore.Editor.() -> Unit) = prefs.edit { Changes(this).changes() }

    private class Changes(
        private val editor: SharedPreferences.Editor,
    ) : KeyValueStore.Editor {
        override fun putString(
            key: String,
            value: String?,
        ) {
            editor.putString(key, value)
        }

        override fun putInt(
            key: String,
            value: Int,
        ) {
            editor.putInt(key, value)
        }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            editor.putBoolean(key, value)
        }

        override fun putStringSet(
            key: String,
            value: Set<String>,
        ) {
            editor.putStringSet(key, value.toSet())
        }

        override fun remove(key: String) {
            editor.remove(key)
        }
    }
}

/** The app's saved data on this phone. */
fun phoneStorage(context: Context): Storage =
    // Renaming this file would lose everyone's saved high scores.
    Storage(SharedPreferencesStore(context.getSharedPreferences("sniddle", Context.MODE_PRIVATE)))
