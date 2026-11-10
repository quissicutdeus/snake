package com.snakearena.game.render

import com.snakearena.game.GameConfig
import com.snakearena.game.model.Snake
import com.snakearena.game.model.dist2
import com.snakearena.game.sim.GameSim
import com.snakearena.game.sim.SizeRelation
import com.snakearena.game.sim.compareSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Safe-area insets (screen pixels) the HUD should stay clear of. */
data class HudInsets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f)

/** Draws a whole frame: arena, food, snakes, effects and the HUD. */
object SceneRenderer {

    /** Size unit for screen-space UI: 1 unit ~ 1 dp on a 360 dp phone. */
    fun uiUnit(c: Canvas2D) = min(c.width, c.height) / 360f

    fun moodFor(s: Snake, player: Snake?): Mood {
        if (player == null || s === player || !player.alive) return Mood.HAPPY
        return when (compareSize(s.length, player.length)) {
            SizeRelation.SMALLER -> Mood.SCARED
            SizeRelation.BIGGER -> Mood.ANGRY
            SizeRelation.EQUAL -> Mood.HAPPY
        }
    }

    fun draw(
        c: Canvas2D,
        sim: GameSim,
        cam: Camera,
        fx: Fx,
        time: Float,
        insets: HudInsets = HudInsets(),
        showHud: Boolean = true,
    ) {
        val w = c.width
        val h = c.height
        val ui = uiUnit(c)
        val world = sim.world
        val player = sim.player

        c.fillRect(0f, 0f, w, h, Colors.OUTSIDE)
        c.save()
        c.translate(w / 2f + fx.shakeX, h / 2f + fx.shakeY)
        c.scale(cam.scale)
        c.translate(-cam.x, -cam.y)

        val halfW = w / 2f / cam.scale + 60f
        val halfH = h / 2f / cam.scale + 60f
        val left = cam.x - halfW
        val right = cam.x + halfW
        val top = cam.y - halfH
        val bottom = cam.y + halfH

        drawFloor(c, sim, left, top, right, bottom)

        fun visible(x: Float, y: Float, r: Float) = x + r > left && x - r < right && y + r > top && y - r < bottom
        for (t in world.tailPieces) if (visible(t.x, t.y, t.radius)) SnakeArt.drawTailPiece(c, t, time)
        for (p in world.pellets) if (visible(p.x, p.y, p.radius * 2f)) SnakeArt.drawPellet(c, p, time)

        val reach2 = (halfW + halfH)
        for (s in world.snakes) {
            if (!s.alive || s === player) continue
            val r = reach2 + s.length
            if (dist2(s.x, s.y, cam.x, cam.y) > r * r) continue
            drawSnakeWithBlink(c, s, moodFor(s, player), time)
        }
        if (player != null && player.alive) drawSnakeWithBlink(c, player, Mood.HAPPY, time)

        // Name tags above AI heads.
        val tag = 11f * ui / cam.scale
        for (s in world.snakes) {
            if (!s.alive || s.isPlayer || !visible(s.x, s.y, s.radius * 3f)) continue
            val y = s.y - s.radius * (if (s.style.hat == com.snakearena.game.model.Hat.NONE) 1.35f else 2.05f) - tag * 0.35f
            shadowText(c, s.name, s.x, y, tag, Colors.withAlpha(Colors.WHITE, 0.92f), TextAlign.CENTER, 1.2f * ui / cam.scale)
        }

        drawFx(c, fx, ui / cam.scale)
        c.restore()

        if (fx.hurtFlash > 0f) c.fillRect(0f, 0f, w, h, Colors.withAlpha(Colors.ANGRY_RED, fx.hurtFlash / 0.35f * 0.28f))
        if (showHud && player != null) drawHud(c, sim, player, insets, ui)
    }

    private fun drawSnakeWithBlink(c: Canvas2D, s: Snake, mood: Mood, time: Float) {
        val blink = s.invulnerable > 0f && floor(time * 10f).toInt() % 2 == 0
        SnakeArt.drawSnake(c, s, mood, time, blink)
    }

    private fun drawFloor(c: Canvas2D, sim: GameSim, left: Float, top: Float, right: Float, bottom: Float) {
        val aw = sim.world.width
        val ah = sim.world.height
        // Fence first (it sits just outside the grass).
        val fw = 26f
        c.fillRoundRect(-fw, -fw, aw + 2 * fw, ah + 2 * fw, 34f, Colors.FENCE_DARK)
        c.fillRoundRect(-fw * 0.6f, -fw * 0.6f, aw + 1.2f * fw, ah + 1.2f * fw, 26f, Colors.FENCE)
        c.fillRect(0f, 0f, aw, ah, Colors.GRASS)

        // Grass dots and the odd flower, on a fixed grid so they don't move.
        val span = max(right - left, bottom - top)
        val step = if (span > 2600f) 180f else 90f
        val gx0 = max(0, floor(left / step).toInt())
        val gx1 = min((aw / step).toInt(), ceil(right / step).toInt())
        val gy0 = max(0, floor(top / step).toInt())
        val gy1 = min((ah / step).toInt(), ceil(bottom / step).toInt())
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                val x = gx * step + (if (gy % 2 == 0) 0f else step / 2f)
                val y = gy * step
                if (x <= 10f || x >= aw - 10f || y <= 10f || y >= ah - 10f) continue
                val hash = (gx * 73856093) xor (gy * 19349663)
                if (step == 90f && hash.mod(29) == 0) {
                    drawFlower(c, x, y, FLOWER_COLORS[hash.mod(FLOWER_COLORS.size)])
                } else {
                    c.fillCircle(x, y, 6f, Colors.GRASS_DOT)
                }
            }
        }

        // Fence posts.
        val post = 250f
        var p = 0f
        while (p <= aw) {
            if (p > left - 40f && p < right + 40f) {
                if (top < 0f) c.fillCircle(p, -fw * 0.5f, 15f, Colors.FENCE_DARK)
                if (bottom > ah) c.fillCircle(p, ah + fw * 0.5f, 15f, Colors.FENCE_DARK)
            }
            p += post
        }
        p = 0f
        while (p <= ah) {
            if (p > top - 40f && p < bottom + 40f) {
                if (left < 0f) c.fillCircle(-fw * 0.5f, p, 15f, Colors.FENCE_DARK)
                if (right > aw) c.fillCircle(aw + fw * 0.5f, p, 15f, Colors.FENCE_DARK)
            }
            p += post
        }
    }

    private val FLOWER_COLORS = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF176.toInt(), 0xFFFFB3D9.toInt(), 0xFFB3E5FC.toInt())

    private fun drawFlower(c: Canvas2D, x: Float, y: Float, petal: Int) {
        val r = 6f
        c.fillCircle(x - r, y, r, petal)
        c.fillCircle(x + r, y, r, petal)
        c.fillCircle(x, y - r, r, petal)
        c.fillCircle(x, y + r, r, petal)
        c.fillCircle(x, y, r * 0.8f, 0xFFFFB300.toInt())
    }

    private fun drawFx(c: Canvas2D, fx: Fx, textUnit: Float) {
        for (q in fx.particles) {
            val k = q.life / q.maxLife
            c.fillCircle(q.x, q.y, q.size * (0.4f + 0.6f * k), Colors.withAlpha(q.color, k))
        }
        for (wd in fx.words) {
            val k = wd.life / wd.maxLife
            val grow = 1f + 0.35f * (1f - min(1f, (1f - k) * 6f)) // quick pop, then settle
            val size = wd.size * 0.75f * textUnit * grow
            val a = min(1f, k * 3f)
            shadowText(c, wd.text, wd.x, wd.y, size, Colors.withAlpha(wd.color, a), TextAlign.CENTER, size * 0.08f, a)
        }
    }

    fun shadowText(c: Canvas2D, text: String, x: Float, y: Float, size: Float, color: Int, align: TextAlign, offset: Float, alpha: Float = 1f) {
        c.text(text, x + offset, y + offset, size, Colors.withAlpha(Colors.INK, 0.75f * alpha), align)
        c.text(text, x, y, size, color, align)
    }

    private fun drawHud(c: Canvas2D, sim: GameSim, player: Snake, insets: HudInsets, ui: Float) {
        val left = insets.left + 14f * ui
        val top = insets.top + 12f * ui

        // Hearts.
        val hs = 24f * ui
        for (i in 0 until GameConfig.PLAYER_HEARTS) {
            val cx = left + hs / 2f + i * hs * 1.2f
            val cy = top + hs / 2f
            val full = i < player.hearts
            val heart = SnakeArt.heartPath(cx, cy, hs)
            c.fillPath(heart, if (full) Colors.HEART else Colors.withAlpha(Colors.WHITE, 0.35f))
            c.strokePath(heart, 2f * ui, Colors.withAlpha(Colors.INK, 0.8f))
            if (full) c.fillCircle(cx - hs * 0.2f, cy - hs * 0.12f, hs * 0.1f, Colors.withAlpha(Colors.WHITE, 0.8f))
        }

        // Length (the score) and snakes eaten.
        val scoreY = top + hs + 26f * ui
        shadowText(c, "Length ${GameConfig.scoreFor(player.length)}", left, scoreY, 22f * ui, Colors.WHITE, TextAlign.LEFT, 2f * ui)
        if (player.snakesEaten > 0) {
            shadowText(c, "Snakes eaten: ${player.snakesEaten}", left, scoreY + 19f * ui, 13f * ui, Colors.WHITE, TextAlign.LEFT, 1.5f * ui)
        }

        drawLeaderboard(c, sim, player, insets, ui)
    }

    /** Live "Biggest snakes" list: top 5, your row highlighted (and added below if you're not in it). */
    private fun drawLeaderboard(c: Canvas2D, sim: GameSim, player: Snake, insets: HudInsets, ui: Float) {
        val board = sim.leaderboard()
        val top5 = board.take(5)
        val myRank = board.indexOf(player) + 1
        val extra = myRank > 5
        val rows = top5.size + if (extra) 1 else 0
        val pw = 148f * ui
        val rowH = 17f * ui
        val titleH = 20f * ui
        val ph = titleH + rows * rowH + 8f * ui + if (extra) 6f * ui else 0f
        val x0 = c.width - insets.right - 12f * ui - pw
        val y0 = insets.top + 12f * ui
        c.fillRoundRect(x0, y0, pw, ph, 10f * ui, Colors.withAlpha(Colors.INK, 0.42f))
        c.text("Biggest snakes", x0 + pw / 2f, y0 + 14f * ui, 11f * ui, Colors.withAlpha(Colors.WHITE, 0.9f), TextAlign.CENTER)

        fun row(rank: Int, s: Snake, y: Float) {
            val me = s === player
            if (me) c.fillRoundRect(x0 + 4f * ui, y - rowH + 4f * ui, pw - 8f * ui, rowH, 6f * ui, Colors.GOLD)
            val col = if (me) Colors.INK else Colors.WHITE
            val size = 12f * ui
            c.fillCircle(x0 + 16f * ui, y - 4f * ui, 4.5f * ui, s.style.bodyColor)
            c.text("$rank", x0 + 28f * ui, y, size, col)
            val name = fit(c, if (me) "You" else s.name, size, 70f * ui)
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

    private fun fit(c: Canvas2D, text: String, size: Float, maxW: Float): String {
        if (c.measureText(text, size) <= maxW) return text
        var t = text
        while (t.length > 1 && c.measureText("$t…", size) > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
