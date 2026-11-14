package com.quissicutdeus.sniddle

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * What the screens keep with rememberSaveable, as one line of text and back.
 *
 * Android keeps that state itself when it closes the app in the background,
 * so the game is still there when a kid comes back. iOS doesn't, so the iOS
 * app writes it to its saved data with this (see PersistedUiState in iosMain).
 *
 * Only what the screens actually save is supported: null, booleans, ints,
 * strings, lists of them, and a MutableState or MutableIntState holding one.
 * A key holding anything else is left out, so that one piece of state starts
 * afresh instead of breaking the rest. Text that isn't a whole, well-formed
 * save (from an older version, or cut short) decodes to null: nothing restored.
 */
internal object SavedUiState {
    private const val VERSION = "ui1|"

    /** Ends a whole save, so text cut off between two entries isn't taken for a shorter save. */
    private const val END = '.'

    fun encode(values: Map<String, List<Any?>>): String {
        val out = StringBuilder(VERSION)
        for ((key, list) in values) {
            val entry = StringBuilder()
            if (!write(entry, list)) continue
            writeString(out, key)
            out.append(entry)
        }
        return out.append(END).toString()
    }

    fun decode(text: String): Map<String, List<Any?>>? {
        if (!text.startsWith(VERSION)) return null
        val reader = Reader(text, VERSION.length)
        val values = LinkedHashMap<String, List<Any?>>()
        while (!reader.atEnd(END)) {
            val key = reader.value() as? String ?: return null
            val list = reader.value() as? List<*> ?: return null
            values[key] = list
        }
        return if (reader.failed) null else values
    }

    /** Appends [value]; false (leaving [out] in a mess the caller throws away) if it can't be saved. */
    private fun write(
        out: StringBuilder,
        value: Any?,
    ): Boolean =
        when (value) {
            null -> {
                out.append('n')
                true
            }

            is Boolean -> {
                out.append(if (value) 'T' else 'F')
                true
            }

            is Int -> {
                out.append('i').append(value).append(';')
                true
            }

            is String -> {
                writeString(out, value)
                true
            }

            is List<*> -> {
                out.append('L').append(value.size).append(';')
                value.all { write(out, it) }
            }

            is MutableIntState -> {
                out.append('I').append(value.intValue).append(';')
                true
            }

            is MutableState<*> -> {
                out.append('S')
                write(out, value.value)
            }

            else -> {
                false
            }
        }

    private fun writeString(
        out: StringBuilder,
        s: String,
    ) {
        out
            .append('s')
            .append(s.length)
            .append(':')
            .append(s)
    }

    private class Reader(
        private val text: String,
        private var at: Int,
    ) {
        var failed = false
            private set

        val done get() = failed || at >= text.length

        /** True at the final [end] (nothing after it); a text that runs out first has [failed]. */
        fun atEnd(end: Char): Boolean {
            if (done) {
                failed = true
                return true
            }
            return text[at] == end && at == text.length - 1
        }

        /** The next value, or null with [failed] set if the text is malformed. */
        fun value(): Any? {
            if (done) return fail()
            return when (text[at++]) {
                'n' -> null
                'T' -> true
                'F' -> false
                'i' -> number(';')
                'I' -> number(';')?.let { mutableIntStateOf(it) }
                'S' -> value().let { if (failed) null else mutableStateOf(it) }
                's' -> string()
                'L' -> list()
                else -> fail()
            }
        }

        private fun number(end: Char): Int? {
            val stop = text.indexOf(end, at)
            if (stop < 0) return fail()
            val n = text.substring(at, stop).toIntOrNull() ?: return fail()
            at = stop + 1
            return n
        }

        private fun string(): String? {
            val length = number(':') ?: return null
            if (length < 0 || at + length > text.length) return fail()
            return text.substring(at, at + length).also { at += length }
        }

        private fun list(): List<Any?>? {
            val size = number(';') ?: return null
            if (size < 0) return fail()
            val items = ArrayList<Any?>(size)
            repeat(size) {
                val item = value()
                if (failed) return null
                items += item
            }
            return items
        }

        private fun fail(): Nothing? {
            failed = true
            return null
        }
    }
}
