package com.quissicutdeus.sniddle.game.sim

import java.text.BreakIterator

/**
 * [BreakIterator.getCharacterInstance] follows the Unicode extended grapheme rules (emoji ZWJ
 * sequences included) on JDK 20 and later and on Android, where it is backed by ICU.
 */
internal actual fun graphemeEnds(text: String): Iterator<Int> {
    val clusters = BreakIterator.getCharacterInstance()
    clusters.setText(text)
    return generateSequence { clusters.next().takeIf { it != BreakIterator.DONE } }.iterator()
}
