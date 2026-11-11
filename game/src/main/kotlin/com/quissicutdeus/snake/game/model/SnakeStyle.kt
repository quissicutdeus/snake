package com.quissicutdeus.snake.game.model

import kotlin.random.Random

/** Something with a name to show a kid. */
interface Labelled {
    val label: String
}

enum class Pattern(
    override val label: String,
) : Labelled {
    SOLID("Solid"),
    STRIPES("Stripes"),
    DOTS("Polka dots"),
    ZIGZAG("Zigzag"),
    RAINBOW("Rainbow"),
    CHECKER("Checker"),
    STARS("Stars"),
    FLAG("Red, white & blue"),
}

enum class Eyes(
    override val label: String,
) : Labelled {
    GOOGLY("Googly"),
    SLEEPY("Sleepy"),
    SUNGLASSES("Sunglasses"),
    HEARTS("Heart eyes"),
}

enum class Hat(
    override val label: String,
) : Labelled {
    NONE("None"),
    CROWN("Crown"),
    PARTY("Party hat"),
    BOW("Bow"),
    TOP_HAT("Top hat"),
    WIZARD("Wizard hat"),
    HORNS("Viking horns"),
    SANTA("Santa hat"),
    TURKEY("Turkey hat"),
    GHOST("Ghost hat"),
    STAR_HAT("Star hat"),
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
        val PALETTE =
            intArrayOf(
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

        // Positions in the saved text (see encode()).
        private const val FIELD_COLOR1 = 0
        private const val FIELD_COLOR2 = 1
        private const val FIELD_PATTERN = 2
        private const val FIELD_EYES = 3
        private const val FIELD_HAT = 4

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

            fun color(
                i: Int,
                def: Int,
            ) = p
                .getOrNull(i)
                ?.trim()
                ?.toIntOrNull()
                ?.takeIf { it in PALETTE.indices } ?: def

            fun name(i: Int) = p.getOrNull(i)?.trim()
            return SnakeStyle(
                color1 = color(FIELD_COLOR1, DEFAULT.color1),
                color2 = color(FIELD_COLOR2, DEFAULT.color2),
                pattern = Pattern.entries.firstOrNull { it.name == name(FIELD_PATTERN) } ?: DEFAULT.pattern,
                eyes = Eyes.entries.firstOrNull { it.name == name(FIELD_EYES) } ?: DEFAULT.eyes,
                hat = Hat.entries.firstOrNull { it.name == name(FIELD_HAT) } ?: DEFAULT.hat,
            )
        }
    }
}
