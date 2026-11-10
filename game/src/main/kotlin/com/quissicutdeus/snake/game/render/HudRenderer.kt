package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.sim.GameSim
import kotlin.math.min

/** Screen-space HUD: hearts, length, and the live "Biggest snakes" list. */
object HudRenderer {
    private val path = Path2D()

    fun draw(
        c: Canvas2D,
        sim: GameSim,
        player: Snake,
        insets: HudInsets,
        ui: Float,
    ) {
        val left = insets.left + 14f * ui
        val top = insets.top + 12f * ui

        // Hearts.
        val hs = 24f * ui
        for (i in 0 until GameConfig.PLAYER_HEARTS) {
            val cx = left + hs / 2f + i * hs * 1.2f
            val cy = top + hs / 2f
            val full = i < player.hearts
            val heart = heartPath(cx, cy, hs)
            c.fillPath(heart, if (full) Colors.HEART else Colors.withAlpha(Colors.WHITE, 0.35f))
            c.strokePath(heart, 2f * ui, Colors.withAlpha(Colors.INK, 0.8f))
            if (full) c.fillCircle(cx - hs * 0.2f, cy - hs * 0.12f, hs * 0.1f, Colors.withAlpha(Colors.WHITE, 0.8f))
        }

        // Length (the score) and snakes eaten.
        val scoreY = top + hs + 26f * ui
        SceneRenderer.shadowText(
            c,
            "Length ${GameConfig.scoreFor(player.length)}",
            left,
            scoreY,
            22f * ui,
            Colors.WHITE,
            TextAlign.LEFT,
            2f * ui,
        )
        if (player.snakesEaten > 0) {
            SceneRenderer.shadowText(
                c,
                "Snakes eaten: ${player.snakesEaten}",
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
        player: Snake,
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

    /** Live "Biggest snakes" list: top 5, your row highlighted (and added below if you're not in it). */
    private fun drawLeaderboard(
        c: Canvas2D,
        sim: GameSim,
        player: Snake,
        insets: HudInsets,
        ui: Float,
    ) {
        val board = sim.leaderboard()
        val top5 = board.take(5)
        val myRank = board.indexOf(player) + 1
        val extra = myRank > 5
        val rows = top5.size + if (extra) 1 else 0
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
        c.text("Biggest snakes", x0 + pw / 2f, y0 + 14f * ui, 11f * ui, Colors.withAlpha(Colors.WHITE, 0.9f), TextAlign.CENTER)

        fun row(
            rank: Int,
            s: Snake,
            y: Float,
        ) {
            val me = s === player
            if (me) c.fillRoundRect(x0 + 4f * ui, y - rowH + 4f * ui, pw - 8f * ui, rowH, 6f * ui, Colors.GOLD)
            val col = if (me) Colors.INK else Colors.WHITE
            val size = 12f * ui
            c.fillCircle(x0 + 16f * ui, y - 4f * ui, 4.5f * ui, s.style.bodyColor)
            c.text("$rank", x0 + 28f * ui, y, size, col)
            val name = fit(c, if (me) "You" else s.name, size, pw - 78f * ui)
            c.text(name, x0 + 44f * ui, y, size, col, bold = me)
            c.text("${GameConfig.scoreFor(s.length)}", x0 + pw - 10f * ui, y, size, col, TextAlign.RIGHT)
        }

        var y = y0 + titleH + rowH - 2f * ui
        top5.forEachIndexed { i, s ->
            row(i + 1, s, y)
            y += rowH
        }
        if (extra) {
            c.text("…", x0 + pw / 2f, y - 8f * ui, 12f * ui, Colors.WHITE, TextAlign.CENTER)
            y += 6f * ui
            row(myRank, player, y)
        }
    }

    private fun fit(
        c: Canvas2D,
        text: String,
        size: Float,
        maxW: Float,
    ): String {
        if (c.measureText(text, size) <= maxW) return text
        var t = text
        while (t.length > 1 && c.measureText("$t…", size) > maxW) t = t.dropLast(1)
        return "$t…"
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
