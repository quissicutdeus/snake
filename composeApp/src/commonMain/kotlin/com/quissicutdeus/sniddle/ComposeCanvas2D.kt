package com.quissicutdeus.sniddle

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.quissicutdeus.sniddle.game.render.Canvas2D

/**
 * The game's [Canvas2D] drawn onto a Compose [DrawScope]. Each platform draws
 * through its own native canvas: Android's `android.graphics`, iOS's Skia.
 * Drawing a frame allocates nothing.
 */
interface ComposeCanvas2D : Canvas2D {
    /** Runs [block] with this canvas bound to the scope's drawing surface. */
    fun draw(
        scope: DrawScope,
        block: (Canvas2D) -> Unit,
    )
}

/** A new canvas adapter for this platform. */
expect fun ComposeCanvas2D(): ComposeCanvas2D
