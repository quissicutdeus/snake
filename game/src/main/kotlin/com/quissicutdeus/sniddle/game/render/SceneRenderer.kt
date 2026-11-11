package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist2
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SizeRelation
import com.quissicutdeus.sniddle.game.sim.fightRelationTo
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

/** Draws a whole frame: arena, food, snails, effects and the HUD. */
object SceneRenderer {
    /** Size unit for screen-space UI: 1 unit ~ 1 dp on a 360 dp phone. */
    fun uiUnit(c: Canvas2D) = min(c.width, c.height) / 360f

    fun moodFor(
        s: Snail,
        player: Snail?,
    ): Mood {
        if (player == null || s === player || !player.alive) return Mood.HAPPY
        // Fight relation: with Super Strength, every other snail is scared of you.
        return when (s.fightRelationTo(player)) {
            SizeRelation.SMALLER -> Mood.SCARED
            SizeRelation.BIGGER -> Mood.ANGRY
            SizeRelation.EQUAL -> Mood.HAPPY
        }
    }

    /**
     * The part of the world on screen this frame (plus a margin), in world
     * units. Outside [draw] it is everywhere, so pictures of snails (menus,
     * previews) are never cut short.
     */
    internal object View {
        var left = Float.NEGATIVE_INFINITY
            private set
        var top = Float.NEGATIVE_INFINITY
            private set
        var right = Float.POSITIVE_INFINITY
            private set
        var bottom = Float.POSITIVE_INFINITY
            private set

        /** Back to "everywhere", once the world has been drawn. */
        fun clear() {
            left = Float.NEGATIVE_INFINITY
            top = Float.NEGATIVE_INFINITY
            right = Float.POSITIVE_INFINITY
            bottom = Float.POSITIVE_INFINITY
        }

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

        /** Rough test for a whole snail: its body can't reach further than its length from the head. */
        fun mayShow(s: Snail): Boolean {
            val reach = (right - left + bottom - top) / 2f + s.length
            return dist2(s.x, s.y, (left + right) / 2f, (top + bottom) / 2f) <= reach * reach
        }
    }

    /**
     * Draws the frame as [me] sees it: their HUD, warning arrows and the moods
     * of the others. With two kids, each half of the screen is drawn with its
     * own kid as [me] (and its own camera and effects).
     */
    fun draw(
        c: Canvas2D,
        sim: GameSim,
        cam: Camera,
        fx: Fx,
        time: Float,
        insets: HudInsets = HudInsets(),
        showHud: Boolean = true,
        me: Snail? = sim.player,
    ) {
        val ui = uiUnit(c)
        c.fillRect(0f, 0f, c.width, c.height, ArenaArt.outside(sim.options.arena))
        c.save()
        c.translate(c.width / 2f + fx.shakeX, c.height / 2f + fx.shakeY)
        c.scale(cam.scale)
        c.translate(-cam.x, -cam.y)
        View.set(cam, c.width, c.height)

        ArenaArt.drawFloor(c, sim.options.arena, sim.world.width, sim.world.height)
        drawFood(c, sim, time)
        drawSnails(c, sim, me, time)
        // Name tags only in play, not behind the menus.
        if (showHud) drawNameTags(c, sim, me, ui / cam.scale)
        drawFx(c, fx, ui / cam.scale)
        c.restore()
        View.clear()

        if (showHud) {
            drawPowerUpMarkers(c, sim, me, cam, ui)
            EdgeMarkers.drawDangers(c, sim, me, cam, ui, time)
        }
        if (fx.hurtFlash > 0f) {
            c.fillRect(0f, 0f, c.width, c.height, Colors.withAlpha(Colors.ANGRY_RED, fx.hurtFlash / Fx.HURT_FLASH_SECONDS * 0.28f))
        }
        if (showHud && me != null) HudRenderer.draw(c, sim, me, insets, ui)
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
        me: Snail?,
        cam: Camera,
        ui: Float,
    ) {
        val p = me?.takeIf { it.alive } ?: return
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

    /** Everyone else first (AI snails, and the other kid if there are two), the player on top. */
    private fun drawSnails(
        c: Canvas2D,
        sim: GameSim,
        player: Snail?,
        time: Float,
    ) {
        for (s in sim.world.snails) {
            if (s.alive && s !== player && View.mayShow(s)) drawSnailWithBlink(c, s, moodFor(s, player), time, sim.renderAlpha)
        }
        if (player != null && player.alive) drawSnailWithBlink(c, player, Mood.HAPPY, time, sim.renderAlpha)
    }

    /** Names above everyone else's heads. [unit] is one UI unit in world units (so text keeps its screen size). */
    private fun drawNameTags(
        c: Canvas2D,
        sim: GameSim,
        me: Snail?,
        unit: Float,
    ) {
        val size = 11f * unit
        for (s in sim.world.snails) {
            if (!s.alive || s === me || !View.contains(s.x, s.y, s.radius * 3f)) continue
            // Snails were drawn already, so drawX/drawY hold this frame's head position.
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

    private fun drawSnailWithBlink(
        c: Canvas2D,
        s: Snail,
        mood: Mood,
        time: Float,
        alpha: Float,
    ) {
        val blink = s.invulnerable > 0f && floor(time * 10f).toInt() % 2 == 0
        SnailArt.drawSnail(c, s, mood, time, blink, alpha)
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
            comicText(c, wd.text, wd.x, wd.y, size, wd.color, a)
        }
    }

    /**
     * A floating comic word: white letters in a thick outline of [color], ringed
     * in ink and dropped onto a soft shadow, so it reads on grass and on any snail.
     */
    @Suppress("LongParameterList") // the text, where, how big, and its colour and fade
    private fun comicText(
        c: Canvas2D,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        alpha: Float,
    ) {
        val rim = size * 0.2f
        val edge = rim + size * 0.1f
        val off = size * 0.07f
        c.strokeText(text, x + off, y + off, size, edge, Colors.withAlpha(Colors.INK, 0.45f * alpha), TextAlign.CENTER)
        c.strokeText(text, x, y, size, edge, Colors.withAlpha(Colors.INK, 0.85f * alpha), TextAlign.CENTER)
        c.strokeText(text, x, y, size, rim, Colors.withAlpha(color, alpha), TextAlign.CENTER)
        c.text(text, x, y, size, Colors.withAlpha(Colors.WHITE, alpha), TextAlign.CENTER)
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
