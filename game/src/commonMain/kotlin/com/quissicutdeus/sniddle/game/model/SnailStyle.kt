package com.quissicutdeus.sniddle.game.model

import kotlin.random.Random

/** Something with a name to show a kid. */
interface Labelled {
    val label: String
}

// Looks are saved by name (see SnailStyle.encode), never by position, so new
// ones can go anywhere in these lists; the order is the order My Snails shows.
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
    HEARTS("Hearts"),
}

enum class Eyes(
    override val label: String,
) : Labelled {
    GOOGLY("Googly"),
    SLEEPY("Sleepy"),
    SUNGLASSES("Sunglasses"),
    HEARTS("Heart eyes"),
    GLASSES("Glasses"),
    STARS("Star eyes"),
    SPARKLY("Sparkly eyes"),
}

/** What colour the coloured part of the eyes is (the eye whites always stay white). */
enum class EyeColor(
    override val label: String,
) : Labelled {
    /** Dark, as snails always had: the default, so older saved snails look the same. */
    CLASSIC("Classic"),

    /** The snail's body colour ([SnailStyle.color1]). */
    BODY("Body color"),

    /** The snail's shell colour ([SnailStyle.color2]). */
    SHELL("Shell color"),
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
    COWBOY("Cowboy hat"),
    FLOWERS("Flower crown"),
    PROPELLER("Propeller cap"),
    PIRATE("Pirate hat"),
    SANTA("Santa hat"),
    TURKEY("Turkey hat"),
    GHOST("Ghost hat"),
    STAR_HAT("Star hat"),
    SHAMROCK("Shamrock hat"),
    BUNNY("Bunny ears"),
    BIRTHDAY_CAKE("Birthday cake"),
}

/**
 * How a snail looks. Plain data: colors are indices into [PALETTE] so a saved
 * style can never hold a weird color. Everything is drawn in code.
 */
data class SnailStyle(
    val color1: Int = 4,
    val color2: Int = 1,
    val pattern: Pattern = Pattern.STRIPES,
    val eyes: Eyes = Eyes.GOOGLY,
    val hat: Hat = Hat.NONE,
    val eyeColor: EyeColor = EyeColor.CLASSIC,
) {
    val bodyColor: Int get() = PALETTE[color1.mod(PALETTE.size)]
    val patternColor: Int get() = PALETTE[color2.mod(PALETTE.size)]

    /** The colour of the eyes' coloured part, or null for the classic dark eyes. */
    val irisColor: Int?
        get() =
            when (eyeColor) {
                EyeColor.CLASSIC -> null
                EyeColor.BODY -> bodyColor
                EyeColor.SHELL -> patternColor
            }

    /**
     * Compact text form for saving, e.g. "4,1,STRIPES,GOOGLY,NONE,CLASSIC". New
     * fields only ever go on the end, so older saved text still reads.
     */
    fun encode(): String = "$color1,$color2,${pattern.name},${eyes.name},${hat.name},${eyeColor.name}"

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

        val DEFAULT = SnailStyle()

        // Positions in the saved text (see encode()).
        private const val FIELD_COLOR1 = 0
        private const val FIELD_COLOR2 = 1
        private const val FIELD_PATTERN = 2
        private const val FIELD_EYES = 3
        private const val FIELD_HAT = 4
        private const val FIELD_EYE_COLOR = 5 // missing before eye colours: classic

        fun random(rng: Random): SnailStyle {
            val c1 = rng.nextInt(PALETTE.size)
            var c2 = rng.nextInt(PALETTE.size - 1)
            if (c2 >= c1) c2++ // always different from color1
            return SnailStyle(
                color1 = c1,
                color2 = c2,
                pattern = Pattern.entries[rng.nextInt(Pattern.entries.size)],
                eyes = Eyes.entries[rng.nextInt(Eyes.entries.size)],
                hat = Hat.entries[rng.nextInt(Hat.entries.size)],
                eyeColor = EyeColor.entries[rng.nextInt(EyeColor.entries.size)],
            )
        }

        /** Parses [encode] output; anything unreadable falls back to defaults. */
        fun decode(text: String?): SnailStyle {
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
            return SnailStyle(
                color1 = color(FIELD_COLOR1, DEFAULT.color1),
                color2 = color(FIELD_COLOR2, DEFAULT.color2),
                pattern = Pattern.entries.firstOrNull { it.name == name(FIELD_PATTERN) } ?: DEFAULT.pattern,
                eyes = Eyes.entries.firstOrNull { it.name == name(FIELD_EYES) } ?: DEFAULT.eyes,
                hat = Hat.entries.firstOrNull { it.name == name(FIELD_HAT) } ?: DEFAULT.hat,
                eyeColor = EyeColor.entries.firstOrNull { it.name == name(FIELD_EYE_COLOR) } ?: DEFAULT.eyeColor,
            )
        }
    }
}
