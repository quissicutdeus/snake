package com.snakearena.game.preview

import com.snakearena.game.GameConfig
import com.snakearena.game.model.Eyes
import com.snakearena.game.model.Hat
import com.snakearena.game.model.Pattern
import com.snakearena.game.model.SnakeStyle
import com.snakearena.game.render.Camera
import com.snakearena.game.render.Colors
import com.snakearena.game.render.Fx
import com.snakearena.game.render.HudInsets
import com.snakearena.game.render.Mood
import com.snakearena.game.render.PosedSnake
import com.snakearena.game.render.SceneRenderer
import com.snakearena.game.render.TextAlign
import com.snakearena.game.sim.Contact
import com.snakearena.game.sim.GameSim
import com.snakearena.game.sim.SimOptions
import com.snakearena.game.sim.SteerInput
import com.snakearena.game.sim.resolveContact
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
    println("Previews written to ${out.absolutePath}")
}

private const val W = 1600
private const val H = 900

/** Plays a few seconds with a player who wanders around, then draws a frame (with HUD). */
private fun midGame(out: File) {
    val sim = GameSim(seed = 8, playerStyle = SnakeStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, hat = Hat.CROWN))
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
    // Bring a couple of AI snakes into view so the frame shows the faces.
    val ais = sim.world.snakes.filter { !it.isPlayer && it.alive }.sortedBy { it.length }
    placeNear(sim, ais.first(), p.x + 260f, p.y - 150f, 2.6f)
    placeNear(sim, ais.last(), p.x - 330f, p.y + 170f, -0.4f)
    sim.step()
    val c = Java2DCanvas(W, H)
    cam.snapTo(p.x, p.y, p.radius, W.toFloat(), H.toFloat())
    SceneRenderer.draw(c, sim, cam, fx, t, HudInsets())
    c.writePng(File(out, "mid_game.png"))
}

private fun placeNear(sim: GameSim, s: com.snakearena.game.model.Snake, x: Float, y: Float, heading: Float) {
    s.heading = heading
    s.shove(x - s.x, y - s.y)
    s.layStraightBody()
    s.brain = null
}

/** A big player has just bitten through an AI snake: the tail lies on the ground in pieces. */
private fun severedTail(out: File) {
    val sim = GameSim(seed = 1, options = SimOptions(pelletCount = 120, width = 1600f, height = 1000f, aiCount = 0, spawnPlayer = false, aiEnabled = false))
    val victim = sim.addSnake("Noodle", SnakeStyle(color1 = 9, color2 = 10, pattern = Pattern.DOTS, eyes = Eyes.GOOGLY), false, 1000f, 520f, 0f, 520f)
    val attacker = sim.addSnake("You", SnakeStyle(color1 = 4, color2 = 2, pattern = Pattern.ZIGZAG, hat = Hat.PARTY), true, 820f, 650f, -1.4f, 1200f)
    repeat(4) { sim.step() }
    sim.resolveContact(Contact(attacker, victim, victim.segCount / 3))
    repeat(10) { sim.step() }
    val cam = Camera()
    val fx = Fx()
    fx.onEvents(sim, sim.events)
    repeat(8) { fx.update(GameConfig.STEP) }
    cam.snapTo(attacker.x, attacker.y - 60f, attacker.radius, W.toFloat(), H.toFloat())
    cam.scale = 1.2f
    val c = Java2DCanvas(W, H)
    SceneRenderer.draw(c, sim, cam, fx, 1f, showHud = true)
    c.writePng(File(out, "severed_tail.png"))
}

private fun board(name: String, cols: Int, rows: Int, cellW: Int, cellH: Int, out: File, cell: (Java2DCanvas, Int, Int) -> String) {
    val c = Java2DCanvas(cols * cellW, rows * cellH)
    c.fillRect(0f, 0f, c.width, c.height, Colors.GRASS)
    for (r in 0 until rows) for (col in 0 until cols) {
        c.save()
        c.translate(col * cellW.toFloat(), r * cellH.toFloat())
        val label = cell(c, col, r)
        c.text(label, cellW / 2f, cellH - 14f, 22f, Colors.INK, TextAlign.CENTER)
        c.restore()
    }
    c.writePng(File(out, name))
}

private fun patterns(out: File) {
    val entries = Pattern.entries
    board("patterns.png", 3, 2, 520, 300, out) { c, col, r ->
        val pattern = entries[r * 3 + col]
        val ps = PosedSnake(SnakeStyle(color1 = (r * 3 + col) * 2 % 12, color2 = (r * 3 + col * 5 + 2) % 12, pattern = pattern), length = 300f)
        c.save()
        c.scale(1.6f)
        ps.wave(290f, 85f, 0.7f, amplitude = 26f, wavelength = 170f)
        ps.draw(c, 0.7f)
        c.restore()
        pattern.label
    }
}

private fun facesAndHats(out: File) {
    board("faces_hats.png", 4, 3, 420, 300, out) { c, col, r ->
        val hat = Hat.entries[col]
        val eyes = Eyes.entries[r]
        val ps = PosedSnake(SnakeStyle(color1 = 1 + col * 2, color2 = 9 - col, pattern = Pattern.SOLID, eyes = eyes, hat = hat), length = 140f)
        c.save()
        c.scale(2.4f)
        ps.wave(150f, 62f, 1.1f, amplitude = 10f, wavelength = 120f)
        ps.draw(c, 1.1f)
        c.restore()
        "${eyes.label} + ${hat.label}"
    }
}

private fun moods(out: File) {
    board("moods.png", 3, 1, 460, 300, out) { c, col, _ ->
        val mood = Mood.entries[col]
        val ps = PosedSnake(SnakeStyle(color1 = 7, color2 = 2, pattern = Pattern.CHECKER, hat = Hat.BOW), length = 150f)
        c.save()
        c.scale(2.4f)
        ps.wave(150f, 62f, 0.4f, amplitude = 12f, wavelength = 130f)
        ps.draw(c, 0.4f, mood)
        c.restore()
        when (mood) {
            Mood.HAPPY -> "Same size / you"
            Mood.SCARED -> "Smaller than you"
            Mood.ANGRY -> "Bigger than you"
        }
    }
}

/** The little curled pictures used on the high-score list. */
private fun curls(out: File) {
    board("score_pictures.png", 6, 1, 150, 150, out) { c, col, _ ->
        val ps = PosedSnake(SnakeStyle(color1 = col * 2, color2 = (col * 2 + 5) % 12, pattern = Pattern.entries[col], hat = Hat.entries[col % 4]), length = 520f)
        c.save()
        c.translate(75f, 72f)
        val size = ps.curl(0f, 0f)
        c.scale(120f / size)
        ps.draw(c, 0f)
        c.restore()
        ""
    }
}
