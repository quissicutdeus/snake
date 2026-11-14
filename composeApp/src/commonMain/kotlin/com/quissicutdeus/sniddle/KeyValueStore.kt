package com.quissicutdeus.sniddle

/**
 * The phone's own small key-value store, where [Storage] keeps everything.
 * Plain Kotlin, so the app's screens don't need Android to save anything:
 * on Android it's the "sniddle" SharedPreferences file ([SharedPreferencesStore]),
 * which the keys and value types must keep matching so that saved games load.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    fun getInt(
        key: String,
        default: Int,
    ): Int

    fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean

    fun getStringSet(key: String): Set<String>?

    operator fun contains(key: String): Boolean

    /** Makes [changes] together, in one write. */
    fun edit(changes: Editor.() -> Unit)

    /** Changes to make in one [edit]. */
    interface Editor {
        fun putString(
            key: String,
            value: String?,
        )

        fun putInt(
            key: String,
            value: Int,
        )

        fun putBoolean(
            key: String,
            value: Boolean,
        )

        fun putStringSet(
            key: String,
            value: Set<String>,
        )

        fun remove(key: String)
    }
}
