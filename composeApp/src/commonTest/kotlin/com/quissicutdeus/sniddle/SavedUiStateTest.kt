package com.quissicutdeus.sniddle

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SavedUiStateTest {
    /** Everything the screens save today: the screen, a game in progress, flags, counters, typed text. */
    @Test fun whatTheScreensSaveComesBackTheSame() {
        val saved =
            mapOf(
                "screen" to listOf("over|120|3|1700000000|12.5|0|1;2;3|Mr. Snoot"),
                "game" to listOf("run3;150.0;150.0;3;0;0.0,0.0;0.0;normal;1;2;3;Mya 🐣"),
                "paused" to listOf(mutableStateOf(true)),
                "stage" to listOf(mutableIntStateOf(2)),
                "typed" to listOf(mutableStateOf("12 + 7 = ")),
                "tip" to listOf(-1),
                "gone" to listOf(null, mutableStateOf<String?>(null)),
                "nested" to listOf(listOf(1, "a:b;c", false)),
            )
        val back = SavedUiState.decode(SavedUiState.encode(saved))!!
        assertEquals(saved.keys, back.keys)
        assertEquals(saved.getValue("screen"), back.getValue("screen"))
        assertEquals(saved.getValue("game"), back.getValue("game"))
        assertEquals(true, assertIs<MutableState<*>>(back.getValue("paused")[0]).value)
        assertEquals(2, assertIs<MutableIntState>(back.getValue("stage")[0]).intValue)
        assertEquals("12 + 7 = ", assertIs<MutableState<*>>(back.getValue("typed")[0]).value)
        assertEquals(listOf<Any?>(-1), back.getValue("tip"))
        assertNull(back.getValue("gone")[0])
        assertNull(assertIs<MutableState<*>>(back.getValue("gone")[1]).value)
        assertEquals(listOf<Any?>(listOf(1, "a:b;c", false)), back.getValue("nested"))
    }

    /** Something the codec can't keep loses only its own key. */
    @Test fun anUnsupportedValueLeavesOutOnlyItsKey() {
        val back = SavedUiState.decode(SavedUiState.encode(mapOf("odd" to listOf(1.5f), "kept" to listOf("yes"))))!!
        assertEquals(mapOf<String, List<Any?>>("kept" to listOf("yes")), back)
    }

    @Test fun anythingButAWholeSaveRestoresNothing() {
        val whole = SavedUiState.encode(mapOf("screen" to listOf("title"), "round" to listOf(mutableIntStateOf(4))))
        assertNull(SavedUiState.decode(""))
        assertNull(SavedUiState.decode("ui0|s1:as1:b"))
        for (cut in 5 until whole.length) assertNull(SavedUiState.decode(whole.substring(0, cut)), "cut at $cut")
        assertEquals(mapOf<String, List<Any?>>(), SavedUiState.decode(SavedUiState.encode(emptyMap())))
    }
}
