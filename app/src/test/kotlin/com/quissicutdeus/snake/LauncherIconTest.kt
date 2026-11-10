package com.quissicutdeus.snake

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Renders the real launcher icon (the adaptive-icon resources, drawn by
 * Android's own graphics code) cut to the shapes launchers use, plus the
 * themed (monochrome) version, into app/build/screenshots/launcher_icon.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LauncherIconTest {
    @Test fun launcherIcon() {
        val icon = RuntimeEnvironment.getApplication().getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val monochrome = checkNotNull(icon.monochrome) { "the icon needs a monochrome layer for themed icons" }
        val tiles =
            listOf(
                circle() to listOf(icon.background, icon.foreground),
                squircle() to listOf(icon.background, icon.foreground),
                roundedSquare() to listOf(icon.background, icon.foreground),
                circle() to listOf(themed(THEMED_BACKGROUND), monochrome.mutate().apply { setTint(THEMED_FOREGROUND) }),
            )
        val tile = VISIBLE * UNIT
        val out = createBitmap(tiles.size * (tile + GAP) + GAP, tile + 2 * GAP)
        val canvas = Canvas(out)
        canvas.drawColor(PAGE)
        tiles.forEachIndexed { i, (mask, layers) ->
            // Each tile shows the centre 72dp of the 108dp canvas, as launchers do.
            canvas.withTranslation((GAP + i * (tile + GAP) - EXTRA * UNIT).toFloat(), (GAP - EXTRA * UNIT).toFloat()) {
                clipPath(mask)
                for (layer in layers) {
                    layer.setBounds(0, 0, CANVAS * UNIT, CANVAS * UNIT)
                    layer.draw(this)
                }
            }
        }
        saveScreenshot(out, "launcher_icon")
    }

    private fun themed(color: Int): Drawable = color.toDrawable()

    private fun circle() = Path().apply { addCircle(MID, MID, RADIUS, Path.Direction.CW) }

    private fun roundedSquare() =
        Path().apply { addRoundRect(MID - RADIUS, MID - RADIUS, MID + RADIUS, MID + RADIUS, CORNER, CORNER, Path.Direction.CW) }

    /** Superellipse |x|^4 + |y|^4 = r^4, close to the "squircle" many launchers use. */
    private fun squircle() =
        Path().apply {
            for (deg in 0 until 360 step 3) {
                val t = Math.toRadians(deg.toDouble())
                val x = MID + RADIUS * sign(cos(t)).toFloat() * abs(cos(t)).pow(0.5).toFloat()
                val y = MID + RADIUS * sign(sin(t)).toFloat() * abs(sin(t)).pow(0.5).toFloat()
                if (deg == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    private companion object {
        const val UNIT = 4 // pixels per dp
        const val CANVAS = 108 // adaptive icon canvas, dp
        const val VISIBLE = 72 // what the launcher shows, dp
        const val EXTRA = (CANVAS - VISIBLE) / 2
        const val GAP = 24
        const val MID = CANVAS * UNIT / 2f
        const val RADIUS = VISIBLE * UNIT / 2f
        const val CORNER = 13f * UNIT
        const val PAGE = 0xFFF4F4F6.toInt()

        // Typical Material You themed-icon colours (light theme).
        const val THEMED_BACKGROUND = 0xFFD9E2FF.toInt()
        const val THEMED_FOREGROUND = 0xFF1B2D61.toInt()
    }
}
