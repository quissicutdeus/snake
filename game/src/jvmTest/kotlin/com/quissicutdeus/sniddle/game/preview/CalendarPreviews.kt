package com.quissicutdeus.sniddle.game.preview

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Camera
import com.quissicutdeus.sniddle.game.render.Colors
import com.quissicutdeus.sniddle.game.render.Fx
import com.quissicutdeus.sniddle.game.render.HudInsets
import com.quissicutdeus.sniddle.game.render.Mood
import com.quissicutdeus.sniddle.game.render.PosedSnail
import com.quissicutdeus.sniddle.game.render.SceneRenderer
import com.quissicutdeus.sniddle.game.render.TextAlign
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.game.sim.SteerInput
import java.io.File
import kotlin.math.sin

/**
 * Previews for the calendar: the arena on each floor (arena_grass.png,
 * arena_space.png and so on, each with the HUD, name tags, a white snail and
 * food of every colour), the big things some floors have (floor_*.png: the
 * planets and comet in space, the bow on the wrapping paper) and the calendar
 * looks up close (calendar_looks.png).
 */
internal fun calendarPreviews(out: File) {
    for (season in ArenaSeason.entries) arena(out, season)
    floorFeature(out, ArenaSeason.SPACE, 0.28f, 0.3f, "space_planet")
    floorFeature(out, ArenaSeason.SPACE, 0.7f, 0.22f, "space_comet")
    floorFeature(out, ArenaSeason.SPACE, 0.74f, 0.72f, "space_blue_planet")
    floorFeature(out, ArenaSeason.WRAPPING_PAPER, 0.38f, 0.58f, "wrapping_paper_bow")
    floorFeature(out, ArenaSeason.DIRT, 0.5f, 0.5f, "dirt_close")
    floorFeature(out, ArenaSeason.DIRT, 0.03f, 0.97f, "dirt_corner")
    floorFeature(out, ArenaSeason.SPACE, 0.97f, 0.03f, "space_corner")
    floorFeature(out, ArenaSeason.WRAPPING_PAPER, 0.03f, 0.03f, "wrapping_paper_corner")
    colorsInSpace(out)
    looks(out)
}

/** Every snail colour, happy and angry, on the space floor: the one dark floor, where a dark outline could get lost. */
private fun colorsInSpace(out: File) {
    val cellW = 300
    val cellH = 170
    val colors = SnailStyle.PALETTE.size
    val c = Java2DCanvas(colors / 2 * cellW, 4 * cellH)
    c.fillRect(0f, 0f, c.width, c.height, 0xFF262C5E.toInt())
    for (i in 0 until colors) {
        for ((k, mood) in listOf(Mood.HAPPY, Mood.ANGRY).withIndex()) {
            c.save()
            c.translate((i % (colors / 2)) * cellW.toFloat(), ((i / (colors / 2)) * 2 + k) * cellH.toFloat())
            val ps = PosedSnail(SnailStyle(color1 = i, color2 = (i + 3) % colors, pattern = Pattern.SOLID), length = 200f)
            ps.wave(250f, 85f, 0f, amplitude = 14f, wavelength = 140f)
            ps.draw(c, 0.9f, mood)
            c.restore()
        }
    }
    c.writePng(File(out, "space_colors.png"))
}

/** The floor around ([fx], [fy]) (fractions of the arena), with the computer snails about but no HUD. */
private fun floorFeature(
    out: File,
    season: ArenaSeason,
    fx: Float,
    fy: Float,
    name: String,
) {
    val sim = GameSim(seed = 3, options = SimOptions(arena = season, spawnPlayer = false))
    repeat(60) { sim.step() }
    val cam = Camera()
    cam.snapTo(sim.world.width * fx, sim.world.height * fy, GameConfig.START_RADIUS * 1.6f, W.toFloat(), H.toFloat())
    val c = Java2DCanvas(W, H)
    SceneRenderer.draw(c, sim, cam, Fx(), 1f, showHud = false)
    c.writePng(File(out, "floor_$name.png"))
}

private const val W = 1600
private const val H = 900

private fun arena(
    out: File,
    season: ArenaSeason,
) {
    val white = SnailStyle(color1 = 10, color2 = 6, pattern = Pattern.STRIPES, eyes = Eyes.GOOGLY, hat = Hat.BIRTHDAY_CAKE)
    val sim = GameSim(seed = 8, playerStyle = white, options = SimOptions(arena = season, season = setOf(Holiday.BIRTHDAY)))
    val cam = Camera()
    val fx = Fx()
    val input = SteerInput()
    val p = sim.player!!
    p.grow(250f)
    var t = 0f
    repeat(60 * 6) {
        input.set(sin(t * 0.6f) * 2.2f + t * 0.15f)
        sim.update(GameConfig.STEP, input)
        fx.onEvents(sim, sim.events)
        fx.update(GameConfig.STEP)
        t += GameConfig.STEP
    }
    val c = Java2DCanvas(W, H)
    cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
    SceneRenderer.draw(c, sim, cam, fx, t, HudInsets())
    c.writePng(File(out, "arena_${season.name.lowercase()}.png"))
}

/** The hearts pattern and the three new hats, big, on each season's ground. */
private fun looks(out: File) {
    val styles =
        listOf(
            SnailStyle(color1 = 9, color2 = 0, pattern = Pattern.HEARTS, eyes = Eyes.HEARTS, hat = Hat.NONE) to "Hearts",
            SnailStyle(color1 = 4, color2 = 2, pattern = Pattern.SOLID, hat = Hat.SHAMROCK) to "Shamrock hat",
            SnailStyle(color1 = 6, color2 = 10, pattern = Pattern.DOTS, hat = Hat.BUNNY) to "Bunny ears",
            SnailStyle(color1 = 8, color2 = 2, pattern = Pattern.STRIPES, hat = Hat.BIRTHDAY_CAKE) to "Birthday cake",
            SnailStyle(color1 = 1, color2 = 9, pattern = Pattern.SOLID, hat = Hat.PARTY) to "Party hat (to compare)",
        )
    val grounds = listOf(Colors.GRASS, 0xFFC4D47A.toInt(), 0xFFC6D8E7.toInt())
    val cellW = 520
    val cellH = 320
    val c = Java2DCanvas(styles.size * cellW, grounds.size * cellH)
    for ((row, ground) in grounds.withIndex()) {
        c.fillRect(0f, row * cellH.toFloat(), c.width, cellH.toFloat(), ground)
        for ((col, look) in styles.withIndex()) {
            c.save()
            c.translate(col * cellW.toFloat(), row * cellH.toFloat())
            c.save()
            c.scale(2.2f)
            val ps = PosedSnail(look.first, length = 200f)
            ps.wave(200f, 80f, 0.9f, amplitude = 14f, wavelength = 140f)
            ps.draw(c, 0.9f)
            c.restore()
            c.text(look.second, cellW / 2f, cellH - 14f, 22f, Colors.INK, TextAlign.CENTER)
            c.restore()
        }
    }
    c.writePng(File(out, "calendar_looks.png"))
}
