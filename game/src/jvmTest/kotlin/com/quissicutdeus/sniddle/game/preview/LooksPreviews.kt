package com.quissicutdeus.sniddle.game.preview

import com.quissicutdeus.sniddle.game.model.EyeColor
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Colors
import com.quissicutdeus.sniddle.game.render.Mood
import com.quissicutdeus.sniddle.game.render.PosedSnail
import com.quissicutdeus.sniddle.game.render.TextAlign
import java.io.File

/**
 * Close-ups for checking hats and eyes: hats_close.png (the everyday hats big
 * on each season's ground, then small as in a game) and eyes_moods.png (every
 * pair of eyes with every mood, big and small) and eye_colors.png (every pair
 * of eyes in each eye colour).
 */
internal fun looksPreviews(out: File) {
    hatsClose(out)
    eyesMoods(out)
    eyeColors(out)
}

/** Every pair of eyes in each eye colour (light colours too), big and happy then small and scared. */
private fun eyeColors(out: File) {
    val looks =
        listOf(
            Triple(7, 2, EyeColor.CLASSIC) to "classic",
            Triple(7, 2, EyeColor.BODY) to "blue body",
            Triple(0, 4, EyeColor.SHELL) to "green shell",
            Triple(10, 6, EyeColor.BODY) to "white body",
            Triple(8, 2, EyeColor.SHELL) to "yellow shell",
        )
    val cellW = 300
    val smallW = 130
    val cellH = 200
    val c = Java2DCanvas(looks.size * (cellW + smallW), Eyes.entries.size * cellH)
    c.fillRect(0f, 0f, c.width, c.height, Colors.GRASS)
    for ((row, eyes) in Eyes.entries.withIndex()) {
        for ((col, look) in looks.withIndex()) {
            val (c1, c2, eyeColor) = look.first
            val style = SnailStyle(color1 = c1, color2 = c2, pattern = Pattern.SOLID, eyes = eyes, eyeColor = eyeColor)
            val left = col * (cellW + smallW).toFloat()
            c.save()
            c.translate(left, row * cellH.toFloat())
            c.save()
            c.scale(1.8f)
            PosedSnail(style, length = 120f).apply { wave(125f, 45f, 0.4f, amplitude = 8f, wavelength = 110f) }.draw(c, 0.4f)
            c.restore()
            c.text("${eyes.label}, ${look.second}", cellW / 2f, cellH - 12f, 18f, Colors.INK, TextAlign.CENTER)
            c.translate(cellW.toFloat(), 0f)
            PosedSnail(style, length = 90f).apply { wave(100f, 80f, 0.4f, amplitude = 6f, wavelength = 60f) }.draw(c, 0.4f, Mood.SCARED)
            c.restore()
        }
    }
    c.writePng(File(out, "eye_colors.png"))
}

private val GROUNDS = listOf(Colors.GRASS, 0xFFC4D47A.toInt(), 0xFFC6D8E7.toInt())

private fun hatsClose(out: File) {
    val hats = Hat.entries.filter { it.ordinal <= Hat.PIRATE.ordinal && it != Hat.NONE }
    val cellW = 420
    val bigH = 300
    val smallH = 150
    val c = Java2DCanvas(hats.size * cellW, GROUNDS.size * bigH + GROUNDS.size * smallH)
    for ((row, ground) in GROUNDS.withIndex()) {
        val top = row * bigH.toFloat()
        val smallTop = GROUNDS.size * bigH + row * smallH.toFloat()
        c.fillRect(0f, top, c.width, bigH.toFloat(), ground)
        c.fillRect(0f, smallTop, c.width, smallH.toFloat(), ground)
        for ((col, hat) in hats.withIndex()) {
            val style = SnailStyle(color1 = (col * 5 + row * 3) % 12, color2 = (col * 7 + 2) % 12, pattern = Pattern.SOLID, hat = hat)
            c.save()
            c.translate(col * cellW.toFloat(), top)
            c.save()
            c.scale(2.4f)
            PosedSnail(style, length = 140f).apply { wave(150f, 62f, 1.1f, amplitude = 10f, wavelength = 120f) }.draw(c, 1.1f)
            c.restore()
            c.text(hat.label, cellW / 2f, bigH - 14f, 22f, Colors.INK, TextAlign.CENTER)
            c.restore()
            // Small, about as big as a little snail looks in a game.
            c.save()
            c.translate(col * cellW.toFloat(), smallTop)
            PosedSnail(style, length = 90f).apply { wave(240f, 85f, 1.1f, amplitude = 6f, wavelength = 60f) }.draw(c, 1.1f)
            c.restore()
        }
    }
    c.writePng(File(out, "hats_close.png"))
}

private fun eyesMoods(out: File) {
    val moods = Mood.entries
    val cellW = 380
    val cellH = 260
    val smallW = 170
    val c = Java2DCanvas(moods.size * cellW + 2 * smallW, Eyes.entries.size * cellH)
    c.fillRect(0f, 0f, c.width, c.height, Colors.GRASS)
    for ((row, eyes) in Eyes.entries.withIndex()) {
        val style = SnailStyle(color1 = (row * 5 + 1) % 12, color2 = (row * 3 + 8) % 12, pattern = Pattern.STRIPES, eyes = eyes)
        for ((col, mood) in moods.withIndex()) {
            c.save()
            c.translate(col * cellW.toFloat(), row * cellH.toFloat())
            c.save()
            c.scale(2.2f)
            PosedSnail(style, length = 140f).apply { wave(140f, 55f, 0.4f, amplitude = 10f, wavelength = 120f) }.draw(c, 0.4f, mood)
            c.restore()
            c.text("${eyes.label}: ${mood.name.lowercase()}", cellW / 2f, cellH - 12f, 20f, Colors.INK, TextAlign.CENTER)
            c.restore()
        }
        // Small and scared, small and angry: how they look across the arena.
        for ((k, mood) in listOf(Mood.SCARED, Mood.ANGRY).withIndex()) {
            c.save()
            c.translate(moods.size * cellW + k * smallW.toFloat(), row * cellH.toFloat())
            PosedSnail(style, length = 90f).apply { wave(110f, 110f, 0.4f, amplitude = 6f, wavelength = 60f) }.draw(c, 0.4f, mood)
            c.restore()
        }
    }
    c.writePng(File(out, "eyes_moods.png"))
}
