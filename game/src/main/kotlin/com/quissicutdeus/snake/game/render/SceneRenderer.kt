package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.dist2
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SizeRelation
import com.quissicutdeus.snake.game.sim.fightRelationTo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Safe-area insets (screen pixels) the HUD should stay clear of, plus the
 * width of a button centred at the top of the screen ([topCenter]).
 */
data class HudInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val topCenter: Float = 0f,
)

/** Draws a whole frame: arena, food, snakes, effects and the HUD. */
object SceneRenderer {
    /** Size unit for screen-space UI: 1 unit ~ 1 dp on a 360 dp phone. */
    fun uiUnit(c: Canvas2D) = min(c.width, c.height) / 360f

    fun moodFor(
        s: Snake,
        player: Snake?,
    ): Mood {
        if (player == null || s === player || !player.alive) return Mood.HAPPY
        // Fight relation: with Super Strength, every other snake is scared of you.
        return when (s.fightRelationTo(player)) {
            SizeRelation.SMALLER -> Mood.SCARED
            SizeRelation.BIGGER -> Mood.ANGRY
            SizeRelation.EQUAL -> Mood.HAPPY
        }
    }

    /** The part of the world on screen this frame (plus a margin), in world units. */
    private object View {
        var left = 0f
            private set
        var top = 0f
            private set
        var right = 0f
            private set
        var bottom = 0f
            private set

        fun set(
            cam: Camera,
            w: Float,
            h: Float,
        ) {
            val halfW = w / 2f / cam.scale + 60f
            val halfH = h / 2f / cam.scale + 60f
            left = cam.x - halfW
            right = cam.x + halfW
            top = cam.y - halfH
            bottom = cam.y + halfH
        }

        fun contains(
            x: Float,
            y: Float,
            r: Float,
        ): Boolean = x + r > left && x - r < right && y + r > top && y - r < bottom

        /** Rough test for a whole snake: its body can't reach further than its length from the head. */
        fun mayShow(s: Snake): Boolean {
            val reach = (right - left + bottom - top) / 2f + s.length
            return dist2(s.x, s.y, (left + right) / 2f, (top + bottom) / 2f) <= reach * reach
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
        val ui = uiUnit(c)
        c.fillRect(0f, 0f, c.width, c.height, Colors.OUTSIDE)
        c.save()
        c.translate(c.width / 2f + fx.shakeX, c.height / 2f + fx.shakeY)
        c.scale(cam.scale)
        c.translate(-cam.x, -cam.y)
        View.set(cam, c.width, c.height)

        drawFloor(c, sim.world.width, sim.world.height)
        drawFood(c, sim, time)
        drawSnakes(c, sim, time)
        // Name tags only in play, not behind the menus.
        if (showHud) drawNameTags(c, sim, ui / cam.scale)
        drawFx(c, fx, ui / cam.scale)
        c.restore()

        if (showHud) {
            drawPowerUpMarkers(c, sim, cam, ui)
            EdgeMarkers.drawDangers(c, sim, cam, ui, time)
        }
        if (fx.hurtFlash > 0f) {
            c.fillRect(0f, 0f, c.width, c.height, Colors.withAlpha(Colors.ANGRY_RED, fx.hurtFlash / Fx.HURT_FLASH_SECONDS * 0.28f))
        }
        val player = sim.player
        if (showHud && player != null) HudRenderer.draw(c, sim, player, insets, ui)
    }

    private fun drawFood(
        c: Canvas2D,
        sim: GameSim,
        time: Float,
    ) {
        for (t in sim.world.tailPieces) if (View.contains(t.x, t.y, t.radius)) FoodArt.drawTailPiece(c, t, time)
        for (p in sim.world.pellets) if (View.contains(p.x, p.y, p.radius * 2f)) FoodArt.drawPellet(c, p, time)
        val glow = GameConfig.POWER_UP_RADIUS * 2.5f
        for (u in sim.world.powerUps) if (View.contains(u.x, u.y, glow)) PowerUpArt.drawItem(c, u, time)
    }

    /** Off-screen power-ups near the player show as little glowing arrows at the screen edge. */
    private fun drawPowerUpMarkers(
        c: Canvas2D,
        sim: GameSim,
        cam: Camera,
        ui: Float,
    ) {
        val p = sim.player?.takeIf { it.alive } ?: return
        for (u in sim.world.powerUps) {
            val near = dist2(u.x, u.y, p.x, p.y) <= MARKER_RANGE * MARKER_RANGE
            val dx = cam.worldToScreenX(u.x, c.width) - c.width / 2f
            val dy = cam.worldToScreenY(u.y, c.height) - c.height / 2f
            if (!near || !EdgeMarkers.place(c, dx, dy, ui)) continue
            EdgeMarkers.drawArrow(c, PowerUpArt.color(u.type), ui)
            PowerUpArt.drawOrb(c, u.type, EdgeMarkers.x, EdgeMarkers.y, 10f * ui)
        }
    }

    /** Off-screen power-ups closer than this get an arrow. */
    private const val MARKER_RANGE = 2400f

    /** AI snakes first, the player on top. */
    private fun drawSnakes(
        c: Canvas2D,
        sim: GameSim,
        time: Float,
    ) {
        val player = sim.player
        for (s in sim.world.snakes) {
            if (s.alive && s !== player && View.mayShow(s)) drawSnakeWithBlink(c, s, moodFor(s, player), time, sim.renderAlpha)
        }
        if (player != null && player.alive) drawSnakeWithBlink(c, player, Mood.HAPPY, time, sim.renderAlpha)
    }

    /** Names above AI heads. [unit] is one UI unit in world units (so text keeps its screen size). */
    private fun drawNameTags(
        c: Canvas2D,
        sim: GameSim,
        unit: Float,
    ) {
        val size = 11f * unit
        for (s in sim.world.snakes) {
            if (!s.alive || s.isPlayer || !View.contains(s.x, s.y, s.radius * 3f)) continue
            // Snakes were drawn already, so drawX/drawY hold this frame's head position.
            val above = s.radius * (if (s.style.hat == Hat.NONE) 1.35f else 2.05f) + size * 0.35f
            shadowText(
                c,
                s.name,
                s.drawX[0],
                s.drawY[0] - above,
                size,
                Colors.withAlpha(Colors.WHITE, 0.92f),
                TextAlign.CENTER,
                1.2f * unit,
            )
        }
    }

    private fun drawSnakeWithBlink(
        c: Canvas2D,
        s: Snake,
        mood: Mood,
        time: Float,
        alpha: Float,
    ) {
        val blink = s.invulnerable > 0f && floor(time * 10f).toInt() % 2 == 0
        SnakeArt.drawSnake(c, s, mood, time, blink, alpha)
    }

    private const val FENCE_WIDTH = 26f

    private fun drawFloor(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        // Fence first (it sits just outside the grass).
        val fw = FENCE_WIDTH
        c.fillRoundRect(-fw, -fw, aw + 2 * fw, ah + 2 * fw, 34f, Colors.FENCE_DARK)
        c.fillRoundRect(-fw * 0.6f, -fw * 0.6f, aw + 1.2f * fw, ah + 1.2f * fw, 26f, Colors.FENCE)
        c.fillRect(0f, 0f, aw, ah, Colors.GRASS)
        drawGrass(c, aw, ah)
        drawFencePosts(c, aw, ah)
    }

    /** Grass dots and the odd flower, on a fixed grid so they don't move. */
    private fun drawGrass(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        val span = max(View.right - View.left, View.bottom - View.top)
        val step = if (span > 2600f) 180f else 90f
        val gx0 = max(0, floor(View.left / step).toInt())
        val gx1 = min((aw / step).toInt(), ceil(View.right / step).toInt())
        val gy0 = max(0, floor(View.top / step).toInt())
        val gy1 = min((ah / step).toInt(), ceil(View.bottom / step).toInt())
        val inner = 10f
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                val x = gx * step + (if (gy % 2 == 0) 0f else step / 2f)
                val y = gy * step
                val insideFence = x in inner..(aw - inner) && y in inner..(ah - inner)
                val hash = (gx * 73856093) xor (gy * 19349663)
                when {
                    !insideFence -> {}

                    step == 90f && hash.mod(29) == 0 -> {
                        drawFlower(c, x, y, FLOWER_COLORS[hash.mod(FLOWER_COLORS.size)])
                    }

                    else -> {
                        c.fillCircle(x, y, 6f, Colors.GRASS_DOT)
                    }
                }
            }
        }
    }

    private fun drawFencePosts(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        val post = 250f
        val r = 15f
        val off = FENCE_WIDTH * 0.5f
        var p = 0f
        while (p <= aw) {
            if (View.contains(p, -off, r)) c.fillCircle(p, -off, r, Colors.FENCE_DARK)
            if (View.contains(p, ah + off, r)) c.fillCircle(p, ah + off, r, Colors.FENCE_DARK)
            p += post
        }
        p = 0f
        while (p <= ah) {
            if (View.contains(-off, p, r)) c.fillCircle(-off, p, r, Colors.FENCE_DARK)
            if (View.contains(aw + off, p, r)) c.fillCircle(aw + off, p, r, Colors.FENCE_DARK)
            p += post
        }
    }

    private val FLOWER_COLORS = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF176.toInt(), 0xFFFFB3D9.toInt(), 0xFFB3E5FC.toInt())

    private fun drawFlower(
        c: Canvas2D,
        x: Float,
        y: Float,
        petal: Int,
    ) {
        val r = 6f
        c.fillCircle(x - r, y, r, petal)
        c.fillCircle(x + r, y, r, petal)
        c.fillCircle(x, y - r, r, petal)
        c.fillCircle(x, y + r, r, petal)
        c.fillCircle(x, y, r * 0.8f, 0xFFFFB300.toInt())
    }

    private fun drawFx(
        c: Canvas2D,
        fx: Fx,
        textUnit: Float,
    ) {
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

    fun shadowText(
        c: Canvas2D,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: TextAlign,
        offset: Float,
        alpha: Float = 1f,
    ) {
        c.text(text, x + offset, y + offset, size, Colors.withAlpha(Colors.INK, 0.75f * alpha), align)
        c.text(text, x, y, size, color, align)
    }
}
