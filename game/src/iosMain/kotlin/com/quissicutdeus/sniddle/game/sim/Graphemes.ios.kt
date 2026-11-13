package com.quissicutdeus.sniddle.game.sim

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.rangeOfComposedCharacterSequenceAtIndex

/**
 * Foundation's composed character sequences follow the Unicode extended grapheme rules (emoji ZWJ
 * sequences, flags and skin tones included), measured in UTF-16 code units like a Kotlin String.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal actual fun graphemeEnds(text: String): Iterator<Int> {
    val string = NSString.create(string = text)
    return iterator {
        var end = 0
        while (end < text.length) {
            end = string.rangeOfComposedCharacterSequenceAtIndex(end.convert()).useContents { (location + length).toInt() }
            yield(end)
        }
    }
}
