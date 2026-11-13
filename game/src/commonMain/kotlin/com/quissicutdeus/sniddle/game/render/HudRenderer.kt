package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.isOnTop
import kotlin.math.min

/** Screen-space HUD: hearts, length (with a crown while you're the biggest), and the live "Biggest snails" list. */
object HudRenderer {
    private val path = Path2D()

    // Texts drawn every frame, made once per number rather than once per frame.
    private val lengthText = NumberText("Length ")
    private val eatenText = NumberText("Snails eaten: ")
    private val numberText = NumberText()

    /** Names already shortened to fit the list, at text size [fittedSize] in a row [fittedWidth] wide. */
    private val fitted = HashMap<String, String>()
    private var fittedSize = 0f
    private var fittedWidth = 0f

    fun draw(
        c: Canvas2D,
        sim: GameSim,
        player: Snail,
        insets: HudInsets,
        ui: Float,
    ) {
        val left = insets.left + 14f * ui
        val top = insets.top + 12f * ui

        // Hearts.
        val hs = 24f * ui
        for (i in 0 until sim.difficulty.hearts) {
            val cx = left + hs / 2f + i * hs * 1.2f
            val cy = top + hs / 2f
            val full = i < player.hearts
            val heart = heartPath(cx, cy, hs)
            c.fillPath(heart, if (full) Colors.HEART else Colors.withAlpha(Colors.WHITE, 0.35f))
            c.strokePath(heart, 2f * ui, Colors.withAlpha(Colors.INK, 0.8f))
            if (full) c.fillCircle(cx - hs * 0.2f, cy - hs * 0.12f, hs * 0.1f, Colors.withAlpha(Colors.WHITE, 0.8f))
        }

        // Length (the score) and snails eaten.
        val scoreY = top + hs + 26f * ui
        val lengthText = lengthText.of(GameConfig.scoreFor(player.length))
        SceneRenderer.shadowText(c, lengthText, left, scoreY, 22f * ui, Colors.WHITE, TextAlign.LEFT, 2f * ui)
        // A crown while you're the biggest snail around.
        if (sim.isOnTop(player)) {
            val crownX = left + c.measureText(lengthText, 22f * ui) + 22f * ui
            HatArt.drawHat(c, crownX, scoreY - 2f * ui, 26f * ui, Hat.CROWN, player.style)
        }
        if (player.snailsEaten > 0) {
            SceneRenderer.shadowText(
                c,
                eatenText.of(player.snailsEaten),
                left,
                scoreY + 19f * ui,
                13f * ui,
                Colors.WHITE,
                TextAlign.LEFT,
                1.5f * ui,
            )
        }

        drawPowers(c, player, left, scoreY + 34f * ui, ui)
        drawLeaderboard(c, sim, player, insets, ui)
    }

    /** One row per active power: its orb, its name and a bar showing the time left. */
    private fun drawPowers(
        c: Canvas2D,
        player: Snail,
        left: Float,
        top: Float,
        ui: Float,
    ) {
        var y = top + 10f * ui
        for (type in PowerType.entries) {
            val timeLeft = player.powerLeft(type)
            if (timeLeft <= 0f) continue
            val col = PowerUpArt.color(type)
            PowerUpArt.drawOrb(c, type, left + 10f * ui, y, 10f * ui)
            SceneRenderer.shadowText(c, type.label, left + 26f * ui, y - 1f * ui, 12f * ui, Colors.WHITE, TextAlign.LEFT, 1.2f * ui)
            val barW = 84f * ui
            c.fillRoundRect(left + 26f * ui, y + 3f * ui, barW, 5f * ui, 2.5f * ui, Colors.withAlpha(Colors.INK, 0.4f))
            c.fillRoundRect(left + 26f * ui, y + 3f * ui, barW * timeLeft / type.seconds, 5f * ui, 2.5f * ui, col)
            y += 26f * ui
        }
    }

    /** Live "Biggest snails" list: top 5, your row highlighted (and added below if you're not in it). */
    private fun drawLeaderboard(
        c: Canvas2D,
        sim: GameSim,
        player: Snail,
        insets: HudInsets,
        ui: Float,
    ) {
        val board = sim.leaderboard()
        val shown = min(board.size, TOP_ROWS)
        val myRank = board.indexOf(player) + 1
        val extra = myRank > TOP_ROWS
        val rows = shown + if (extra) 1 else 0
        // Full width when there's room; narrower on a portrait phone so it stays
        // clear of whatever sits at the top centre (the pause button).
        val right = c.width - insets.right - 12f * ui
        val pw = min(148f * ui, right - (c.width + insets.topCenter) / 2f - 8f * ui)
        val rowH = 17f * ui
        val titleH = 20f * ui
        val ph = titleH + rows * rowH + 8f * ui + if (extra) 6f * ui else 0f
        val x0 = right - pw
        val y0 = insets.top + 12f * ui
        c.fillRoundRect(x0, y0, pw, ph, 10f * ui, Colors.withAlpha(Colors.INK, 0.42f))
        c.text("Biggest snails", x0 + pw / 2f, y0 + 14f * ui, 11f * ui, Colors.withAlpha(Colors.WHITE, 0.9f), TextAlign.CENTER)

        fun row(
            rank: Int,
            s: Snail,
            y: Float,
        ) {
            val me = s === player
            if (me) c.fillRoundRect(x0 + 4f * ui, y - rowH + 4f * ui, pw - 8f * ui, rowH, 6f * ui, Colors.GOLD)
            val col = if (me) Colors.INK else Colors.WHITE
            val size = 12f * ui
            c.fillCircle(x0 + 16f * ui, y - 4f * ui, 4.5f * ui, s.style.bodyColor)
            // The biggest snail wears a little crown on its colour dot.
            if (rank == 1) {
                HatArt.crownOutline(c, x0 + 16f * ui, y - 9f * ui, 9f * ui, 2.2f * ui)
                HatArt.drawHat(c, x0 + 16f * ui, y - 9f * ui, 9f * ui, Hat.CROWN, s.style)
            }
            c.text(numberText.of(rank), x0 + 28f * ui, y, size, col)
            val name = fit(c, s.name, size, pw - 78f * ui)
            c.text(name, x0 + 44f * ui, y, size, col, bold = me)
            c.text(numberText.of(GameConfig.scoreFor(s.length)), x0 + pw - 10f * ui, y, size, col, TextAlign.RIGHT)
        }

        var y = y0 + titleH + rowH - 2f * ui
        for (i in 0 until shown) {
            row(i + 1, board[i], y)
            y += rowH
        }
        if (extra) {
            c.text("…", x0 + pw / 2f, y - 8f * ui, 12f * ui, Colors.WHITE, TextAlign.CENTER)
            y += 6f * ui
            row(myRank, player, y)
        }
    }

    /** [text], cut short with an ellipsis if it is wider than [maxW]. Remembered, as the same names come up every frame. */
    private fun fit(
        c: Canvas2D,
        text: String,
        size: Float,
        maxW: Float,
    ): String {
        // A different screen size fits differently.
        if (size != fittedSize || maxW != fittedWidth || fitted.size > MAX_FITTED) {
            fitted.clear()
            fittedSize = size
            fittedWidth = maxW
        }
        return fitted.getOrPut(text) {
            if (c.measureText(text, size) <= maxW) return@getOrPut text
            var t = text
            while (t.length > 1 && c.measureText("$t…", size) > maxW) t = t.dropLast(1)
            "$t…"
        }
    }

    /** Rows in the "Biggest snails" list (the player gets one more below it when not among them). */
    private const val TOP_ROWS = 5

    /** More remembered names than this and [fit] starts over (snails come and go all game). */
    private const val MAX_FITTED = 64

    /**
     * "[prefix]n" for small whole numbers, each made the first time it is
     * asked for: the HUD draws the same few numbers every frame.
     */
    private class NumberText(
        private val prefix: String = "",
    ) {
        private val made = arrayOfNulls<String>(REMEMBERED)

        fun of(n: Int): String = if (n in 0 until REMEMBERED) made[n] ?: "$prefix$n".also { made[n] = it } else "$prefix$n"

        private companion object {
            /** Scores above this (a snail over 20 000 long) just make their text each frame. */
            const val REMEMBERED = 4096
        }
    }

    /** A heart shape centred at (x, y) with total width [size]. */
    fun heartPath(
        x: Float,
        y: Float,
        size: Float,
    ): Path2D {
        val s = size / 2f
        path.reset()
        path.moveTo(x, y + s * 0.9f)
        path.cubicTo(x - s * 1.25f, y + s * 0.05f, x - s * 0.95f, y - s * 1.05f, x, y - s * 0.4f)
        path.cubicTo(x + s * 0.95f, y - s * 1.05f, x + s * 1.25f, y + s * 0.05f, x, y + s * 0.9f)
        path.close()
        return path
    }
}
