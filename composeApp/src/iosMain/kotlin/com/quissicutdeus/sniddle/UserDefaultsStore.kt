package com.quissicutdeus.sniddle

import platform.Foundation.NSUserDefaults

/**
 * iOS's side of [KeyValueStore]: the app's standard user defaults, with the
 * same keys and value types as Android's SharedPreferences. The data lives in
 * the app's container, so it survives the weekly reinstall (same bundle ID).
 */
class UserDefaultsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStore {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun getInt(
        key: String,
        default: Int,
    ): Int = if (contains(key)) defaults.integerForKey(key).toInt() else default

    override fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = if (contains(key)) defaults.boolForKey(key) else default

    override fun getStringSet(key: String): Set<String>? = defaults.arrayForKey(key)?.filterIsInstance<String>()?.toSet()

    override fun contains(key: String): Boolean = defaults.objectForKey(key) != null

    override fun edit(changes: KeyValueStore.Editor.() -> Unit) {
        Changes(defaults).changes()
    }

    private class Changes(
        private val defaults: NSUserDefaults,
    ) : KeyValueStore.Editor {
        override fun putString(
            key: String,
            value: String?,
        ) {
            // As with SharedPreferences, a null string removes the key.
            if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
        }

        override fun putInt(
            key: String,
            value: Int,
        ) {
            defaults.setInteger(value.toLong(), forKey = key)
        }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            defaults.setBool(value, forKey = key)
        }

        override fun putStringSet(
            key: String,
            value: Set<String>,
        ) {
            defaults.setObject(value.toList(), forKey = key)
        }

        override fun remove(key: String) {
            defaults.removeObjectForKey(key)
        }
    }
}
