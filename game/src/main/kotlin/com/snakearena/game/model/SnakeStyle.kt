package com.snakearena.game.model

import kotlin.random.Random

enum class Pattern(val label: String) {
    SOLID("Solid"), STRIPES("Stripes"), DOTS("Polka dots"), ZIGZAG("Zigzag"),
    RAINBOW("Rainbow"), CHECKER("Checker")
}

enum class Eyes(val label: String) {
    GOOGLY("Googly"), SLEEPY("Sleepy"), SUNGLASSES("Sunglasses")
}

enum class Hat(val label: String) {
    NONE("None"), CROWN("Crown"), PARTY("Party hat"), BOW("Bow")
}

/**
 * How a snake looks. Plain data: colors are indices into [PALETTE] so a saved
 * style can never hold a weird color. Everything is drawn in code.
 */
data class SnakeStyle(
    val color1: Int = 4,
    val color2: Int = 1,
    val pattern: Pattern = Pattern.STRIPES,
    val eyes: Eyes = Eyes.GOOGLY,
    val hat: Hat = Hat.NONE,
) {
    val bodyColor: Int get() = PALETTE[color1.mod(PALETTE.size)]
    val patternColor: Int get() = PALETTE[color2.mod(PALETTE.size)]

    /** Compact text form for saving, e.g. "4,1,STRIPES,GOOGLY,NONE". */
    fun encode(): String = "$color1,$color2,${pattern.name},${eyes.name},${hat.name}"

    companion object {
        /** Twelve bright, kid-friendly colors (ARGB). */
        val PALETTE = intArrayOf(
            0xFFFF4D4D.toInt(), // red
            0xFFFF9F1C.toInt(), // orange
            0xFFFFD93D.toInt(), // sunny yellow
            0xFFB5E61D.toInt(), // lime
            0xFF2ECC71.toInt(), // green
            0xFF1ABC9C.toInt(), // teal
            0xFF3FA9F5.toInt(), // sky blue
            0xFF3D5AFE.toInt(), // blue
            0xFF9B59B6.toInt(), // purple
            0xFFFF6FB5.toInt(), // pink
            0xFFFFFFFF.toInt(), // white
            0xFF8D6E63.toInt(), // chocolate
        )

        val DEFAULT = SnakeStyle()

        fun random(rng: Random): SnakeStyle {
            val c1 = rng.nextInt(PALETTE.size)
            var c2 = rng.nextInt(PALETTE.size - 1)
            if (c2 >= c1) c2++ // always different from color1
            return SnakeStyle(
                color1 = c1,
                color2 = c2,
                pattern = Pattern.entries[rng.nextInt(Pattern.entries.size)],
                eyes = Eyes.entries[rng.nextInt(Eyes.entries.size)],
                hat = Hat.entries[rng.nextInt(Hat.entries.size)],
            )
        }

        /** Parses [encode] output; anything unreadable falls back to defaults. */
        fun decode(text: String?): SnakeStyle {
            if (text.isNullOrBlank()) return DEFAULT
            val p = text.split(',')
            fun color(i: Int, def: Int) = p.getOrNull(i)?.trim()?.toIntOrNull()?.takeIf { it in PALETTE.indices } ?: def
            return SnakeStyle(
                color1 = color(0, DEFAULT.color1),
                color2 = color(1, DEFAULT.color2),
                pattern = Pattern.entries.firstOrNull { it.name == p.getOrNull(2)?.trim() } ?: DEFAULT.pattern,
                eyes = Eyes.entries.firstOrNull { it.name == p.getOrNull(3)?.trim() } ?: DEFAULT.eyes,
                hat = Hat.entries.firstOrNull { it.name == p.getOrNull(4)?.trim() } ?: DEFAULT.hat,
            )
        }
    }
}
