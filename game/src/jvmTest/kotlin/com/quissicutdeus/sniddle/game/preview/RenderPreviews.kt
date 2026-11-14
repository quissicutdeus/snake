package com.quissicutdeus.sniddle.game.preview

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.PowerUp
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Camera
import com.quissicutdeus.sniddle.game.render.Colors
import com.quissicutdeus.sniddle.game.render.Fx
import com.quissicutdeus.sniddle.game.render.HudInsets
import com.quissicutdeus.sniddle.game.render.Mood
import com.quissicutdeus.sniddle.game.render.PosedSnail
import com.quissicutdeus.sniddle.game.render.SceneRenderer
import com.quissicutdeus.sniddle.game.render.TextAlign
import com.quissicutdeus.sniddle.game.sim.Contact
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.game.sim.SteerInput
import com.quissicutdeus.sniddle.game.sim.resolveContact
import java.io.File
import kotlin.math.sin

/**
 * Renders the real game art to PNGs so it can be checked without a phone:
 *   ./gradlew :game:renderPreviews  ->  game/build/previews/
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "build/previews")
    midGame(out)
    severedTail(out)
    patterns(out)
    facesAndHats(out)
    moods(out)
    curls(out)
    powerUps(out)
    dangerArrows(out)
    dangerArrows(out, ArenaSeason.SPACE)
    biggestSnail(out)
    portraitHud(out)
    calendarPreviews(out)
    println("Previews written to ${out.absolutePath}")
}

private const val W = 1600
private const val H = 900

/** Plays a few seconds with a player who wanders around, then draws a frame (with HUD). */
private fun midGame(out: File) {
    val sim = GameSim(seed = 8, playerStyle = SnailStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, hat = Hat.CROWN))
    val cam = Camera()
    val fx = Fx()
    val input = SteerInput()
    var t = 0f
    val p = sim.player!!
    p.grow(250f) // a bit bigger than at the very start
    repeat(60 * 9) { i ->
        input.set(sin(t * 0.6f) * 2.2f + t * 0.15f)
        sim.update(GameConfig.STEP, input)
        fx.onEvents(sim, sim.events)
        fx.update(GameConfig.STEP)
        cam.follow(p.x, p.y, p.radius, W.toFloat(), H.toFloat(), GameConfig.STEP)
        t += GameConfig.STEP
        if (i == 60 * 9 - 1 || sim.gameOver) return@repeat
    }
    // Add a smaller and a bigger snail right next to the player so the frame shows both faces.
    nearby(sim, "Squiggle", p.length * 0.5f, p.x + 260f, p.y - 150f, 2.6f)
    nearby(sim, "Lil' Fang", p.length * 1.6f, p.x - 330f, p.y + 170f, -0.4f)
    sim.step()
    val c = Java2DCanvas(W, H)
    cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
    SceneRenderer.draw(c, sim, cam, fx, t, HudInsets())
    c.writePng(File(out, "mid_game.png"))
}

private fun nearby(
    sim: GameSim,
    name: String,
    length: Float,
    x: Float,
    y: Float,
    heading: Float,
) {
    val s = sim.addSnail(name, SnailStyle.random(kotlin.random.Random(name.hashCode())), false, x, y, heading, length)
    sim.brains.remove(s) // hold still-ish for the picture
}

/** Bigger snails coming at the player from off-screen get red warning arrows; one heading away doesn't. */
private fun dangerArrows(
    out: File,
    arena: ArenaSeason = ArenaSeason.GRASS,
) {
    val sim = GameSim(seed = 4, options = SimOptions(aiCount = 0, pelletCount = 160, spawnPlayer = false, powerUps = false, arena = arena))
    val p = sim.addSnail("You", SnailStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES), true, 2000f, 2000f, 0.3f, 300f)

    fun bigOne(
        name: String,
        color: Int,
        x: Float,
        y: Float,
        heading: Float,
    ) = sim.addSnail(name, SnailStyle(color1 = color, color2 = 10, pattern = Pattern.DOTS), false, x, y, heading, 1200f).also {
        sim.brains.remove(it)
    }
    bigOne("From the left", 0, 1100f, 2100f, 0.1f)
    bigOne("From above", 8, 2300f, 1250f, 1.9f)
    bigOne("Going away", 4, 2900f, 2200f, 0f)
    sim.step()
    val cam = Camera()
    cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
    val c = Java2DCanvas(W, H)
    SceneRenderer.draw(c, sim, cam, Fx(), 0.2f)
    // The grass one is the usual; space, the one dark floor, to check the arrows still read there.
    c.writePng(File(out, if (arena == ArenaSeason.GRASS) "danger_arrows.png" else "danger_arrows_${arena.name.lowercase()}.png"))
}

/** The player has just become the biggest snail: confetti, a cheer, and a crown by the length. */
private fun biggestSnail(out: File) {
    val sim = GameSim(seed = 6, options = SimOptions(aiCount = 0, pelletCount = 160, spawnPlayer = false, powerUps = false))
    val p =
        sim.addSnail(
            "You",
            SnailStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, hat = Hat.PARTY),
            true,
            2000f,
            2000f,
            0.3f,
            900f,
        )
    nearby(sim, "Squiggle", 600f, 2350f, 1800f, 2.6f)
    nearby(sim, "Lil' Fang", 400f, 1600f, 2250f, -0.4f)
    val fx = Fx()
    val cam = Camera()
    cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
    repeat(20) {
        sim.update(GameConfig.STEP, SteerInput())
        fx.onEvents(sim, sim.events)
        fx.update(GameConfig.STEP)
    }
    val c = Java2DCanvas(W, H)
    SceneRenderer.draw(c, sim, cam, fx, 0.3f, HudInsets())
    c.writePng(File(out, "biggest_snail.png"))
}

/**
 * A portrait phone (720x1600 at 2x, pause button at the top centre), where the
 * "Biggest snails" panel narrows. A long-named computer snail holds the crown.
 */
private fun portraitHud(out: File) {
    val w = 720
    val h = 1600
    val sim = GameSim(seed = 5, options = SimOptions(aiCount = 0, pelletCount = 120, spawnPlayer = false, powerUps = false))
    val p = sim.addSnail("You", SnailStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES), true, 2000f, 2000f, 0.3f, 300f)
    nearby(sim, "Sir Wigglesworth the Third", 1400f, 2200f, 1700f, 2.6f)
    nearby(sim, "Squiggle", 500f, 1800f, 2300f, -0.4f)
    sim.step()
    val cam = Camera()
    cam.snapTo(p.x, p.y, p.radius, w.toFloat(), h.toFloat())
    val c = Java2DCanvas(w, h)
    SceneRenderer.draw(c, sim, cam, Fx(), 0.2f, HudInsets(top = 48f, topCenter = 104f))
    c.writePng(File(out, "portrait_hud.png"))
}

/** A big player has just bitten through an AI snail: the tail lies on the ground in pieces. */
private fun severedTail(out: File) {
    val sim =
        GameSim(
            seed = 1,
            options = SimOptions(pelletCount = 120, width = 1600f, height = 1000f, aiCount = 0, spawnPlayer = false, aiEnabled = false),
        )
    val victim =
        sim.addSnail(
            "Noodle",
            SnailStyle(color1 = 9, color2 = 10, pattern = Pattern.DOTS, eyes = Eyes.GOOGLY),
            false,
            1000f,
            520f,
            0f,
            520f,
        )
    val attacker =
        sim.addSnail(
            "You",
            SnailStyle(color1 = 4, color2 = 2, pattern = Pattern.ZIGZAG, hat = Hat.PARTY),
            true,
            820f,
            650f,
            -1.4f,
            1200f,
        )
    repeat(4) { sim.step() }
    sim.resolveContact(Contact(attacker, victim, victim.segCount / 3))
    repeat(10) { sim.step() }
    val cam = Camera()
    val fx = Fx()
    fx.onEvents(sim, sim.events)
    repeat(8) { fx.update(GameConfig.STEP) }
    cam.snapTo(attacker.x, attacker.y - 60f, attacker.radius, W.toFloat(), H.toFloat())
    val c = Java2DCanvas(W, H)
    SceneRenderer.draw(c, sim, cam, fx, 1f, showHud = true)
    c.writePng(File(out, "severed_tail.png"))
}

private fun board(
    name: String,
    cols: Int,
    rows: Int,
    cellW: Int,
    cellH: Int,
    out: File,
    cell: (Java2DCanvas, Int, Int) -> String,
) {
    val c = Java2DCanvas(cols * cellW, rows * cellH)
    c.fillRect(0f, 0f, c.width, c.height, Colors.GRASS)
    for (r in 0 until rows) {
        for (col in 0 until cols) {
            c.save()
            c.translate(col * cellW.toFloat(), r * cellH.toFloat())
            val label = cell(c, col, r)
            c.text(label, cellW / 2f, cellH - 14f, 22f, Colors.INK, TextAlign.CENTER)
            c.restore()
        }
    }
    c.writePng(File(out, name))
}

private fun patterns(out: File) {
    val entries = Pattern.entries
    val cols = 5
    board("patterns.png", cols, (entries.size + cols - 1) / cols, 520, 300, out) { c, col, r ->
        val i = r * cols + col
        if (i >= entries.size) return@board ""
        val pattern = entries[i]
        val ps =
            PosedSnail(SnailStyle(color1 = i * 2 % 12, color2 = (i * 5 + 2) % 12, pattern = pattern), length = 300f)
        c.save()
        c.scale(1.6f)
        ps.wave(290f, 85f, 0.7f, amplitude = 26f, wavelength = 170f)
        ps.draw(c, 0.7f)
        c.restore()
        pattern.label
    }
}

private fun facesAndHats(out: File) {
    board("faces_hats.png", Hat.entries.size, Eyes.entries.size, 420, 300, out) { c, col, r ->
        val hat = Hat.entries[col]
        val eyes = Eyes.entries[r]
        val ps =
            PosedSnail(
                SnailStyle(color1 = (1 + col * 2) % 12, color2 = (9 - col + 12) % 12, pattern = Pattern.SOLID, eyes = eyes, hat = hat),
                length = 140f,
            )
        c.save()
        c.scale(2.4f)
        ps.wave(150f, 62f, 1.1f, amplitude = 10f, wavelength = 120f)
        ps.draw(c, 1.1f)
        c.restore()
        "${eyes.label} + ${hat.label}"
    }
}

private fun moods(out: File) {
    board("moods.png", 4, 1, 460, 300, out) { c, col, _ ->
        val mood = Mood.entries[col]
        val ps = PosedSnail(SnailStyle(color1 = 7, color2 = 2, pattern = Pattern.CHECKER, hat = Hat.BOW), length = 150f)
        c.save()
        c.scale(2.4f)
        ps.wave(150f, 62f, 0.4f, amplitude = 12f, wavelength = 130f)
        ps.draw(c, 0.4f, mood)
        c.restore()
        when (mood) {
            Mood.HAPPY -> "Same size / you"
            Mood.SCARED -> "Smaller than you"
            Mood.ANGRY -> "Bigger than you"
            Mood.ASLEEP -> "Bedtime"
        }
    }
}

/** The little curled pictures used on the high-score list. */
private fun curls(out: File) {
    board("score_pictures.png", 6, 1, 150, 150, out) { c, col, _ ->
        val ps =
            PosedSnail(
                SnailStyle(
                    color1 = col * 2,
                    color2 = (col * 2 + 5) % 12,
                    pattern = Pattern.entries[col],
                    hat =
                        Hat.entries[
                            col %
                                4,
                        ],
                ),
                length = 520f,
            )
        c.save()
        c.translate(75f, 72f)
        val size = ps.curl(0f, 0f)
        c.scale(120f / size)
        ps.draw(c, 0f)
        c.restore()
        ""
    }
}

/** The three power-ups on the map, Super Strength scaring a bigger snail, and Armor + Speed in action. */
private fun powerUps(out: File) {
    fun scene(
        name: String,
        powers: List<PowerType>,
        setup: (GameSim, com.quissicutdeus.sniddle.game.model.Snail) -> Unit,
    ) {
        val sim = GameSim(seed = 3, options = SimOptions(aiCount = 0, pelletCount = 160, spawnPlayer = false, powerUps = false))
        val p =
            sim.addSnail(
                "You",
                SnailStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, hat = Hat.CROWN),
                true,
                2000f,
                2000f,
                0.3f,
                500f,
            )
        powers.forEach { p.givePower(it) }
        setup(sim, p)
        val fx = Fx()
        val cam = Camera()
        var t = 0f
        repeat(40) {
            sim.update(GameConfig.STEP, SteerInput())
            fx.onEvents(sim, sim.events)
            fx.update(GameConfig.STEP)
            t += GameConfig.STEP
        }
        cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
        val c = Java2DCanvas(W, H)
        SceneRenderer.draw(c, sim, cam, fx, t + 0.3f)
        c.writePng(File(out, name))
    }
    scene("power_ups.png", listOf(PowerType.STRENGTH)) { sim, p ->
        PowerType.entries.forEachIndexed { i, type -> sim.world.powerUps.add(PowerUp(type, p.x + 160f + i * 140f, p.y - 260f)) }
        sim.world.powerUps.add(PowerUp(PowerType.ARMOR, p.x - 1500f, p.y + 500f)) // off-screen: edge arrow
        val big =
            sim.addSnail(
                "Big Bob",
                SnailStyle(color1 = 0, color2 = 10, pattern = Pattern.DOTS),
                false,
                p.x + 150f,
                p.y + 230f,
                3.1f,
                1400f,
            )
        sim.brains.remove(big)
    }
    scene("power_ups_armor_speed.png", listOf(PowerType.ARMOR, PowerType.SPEED)) { sim, p ->
        val big =
            sim.addSnail(
                "Chomper",
                SnailStyle(color1 = 8, color2 = 2, pattern = Pattern.ZIGZAG),
                false,
                p.x + 260f,
                p.y - 120f,
                2.3f,
                1500f,
            )
        sim.brains.remove(big)
    }
}
