package com.quissicutdeus.sniddle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The menus' colours. The bright ones (buttons, banners, medals) are the same
 * day and night, and so is [Ink], the dark text that goes on them. The rest
 * follow the phone's light or dark setting through [KidPalette]: the menu
 * screens' background, the cards and the text on them. The arena and the
 * snails are the game's own art and look the same either way.
 */
object KidColors {
    val DeepGreen = Color(0xFF2E7D32)

    /** Dark text for bright fills (yellow banners, medals, light snail colours), day and night. */
    val Ink = Color(0xFF2B2B3A)
    val Sunny = Color(0xFFFFC928)
    val Play = Color(0xFFFF7A1A)
    val Sky = Color(0xFF3FA9F5)
    val Grape = Color(0xFF9B59B6)
    val Berry = Color(0xFFFF4D6D)

    /** Behind the menu screens (My Snails, High Scores, game over): green by day, a dark green at night. */
    val MenuBackground: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.menuBackground

    /** The cards the menus sit on. */
    val Panel: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.panel

    /** Text on a [Panel] or a [Chip]. */
    val Text: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.text

    /** Second lines and small print on a [Panel]. */
    val SubtleText: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.subtleText

    /** A choice that isn't picked (a name, a month, a pattern). */
    val Chip: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.chip

    /** A switch's row while it's on (little kid mode). */
    val ChipOn: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.chipOn

    /** A kid's tile (who's playing): [Card] when picked, [CardFaint] when not. */
    val Card: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.card

    val CardFaint: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.cardFaint

    /** A thin edge round something that isn't picked. */
    val OutlineFaint: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.outlineFaint

    /** A locked look's background, and the empty part of its progress bar. */
    val Locked: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.locked

    val Track: Color
        @Composable @ReadOnlyComposable
        get() = LocalKidPalette.current.track
}

/** The colours that change between the light and the night look (see [KidColors]). */
@Immutable
class KidPalette(
    val menuBackground: Color,
    val panel: Color,
    val text: Color,
    val subtleText: Color,
    val chip: Color,
    val chipOn: Color,
    val card: Color,
    val cardFaint: Color,
    val outlineFaint: Color,
    val locked: Color,
    val track: Color,
) {
    companion object {
        val Light =
            KidPalette(
                menuBackground = Color(0xFF9ED96B),
                panel = Color(0xF2FFFFFF),
                text = KidColors.Ink,
                subtleText = Color(0xFF55606A),
                chip = Color(0xFFE8F5E0),
                chipOn = Color(0xFFFFF1B8),
                card = Color.White,
                cardFaint = Color(0x99FFFFFF),
                outlineFaint = Color(0x552B2B3A),
                locked = Color(0xFFEDEDED),
                track = Color(0xFFD6D6D6),
            )

        /**
         * Night: the garden after dark. Deep green behind the menus and
         * slightly lighter green cards with light text: one family of
         * colours, so the bright buttons and the snails glow against it
         * instead of fighting a white card.
         */
        val Night =
            KidPalette(
                menuBackground = Color(0xFF15281D),
                panel = Color(0xF2223A2E),
                text = Color(0xFFEFF5EC),
                subtleText = Color(0xFFADBFB2),
                chip = Color(0xFF34503F),
                chipOn = Color(0xFF5A4A18),
                card = Color(0xFF2C4A3A),
                cardFaint = Color(0x992C4A3A),
                outlineFaint = Color(0x66FFFFFF),
                locked = Color(0xFF34473C),
                track = Color(0xFF4A6355),
            )
    }
}

/**
 * The light or night palette, set once by [SniddleTheme] like MaterialTheme's
 * own colours: every screen reads it, so passing it down would only add a
 * parameter to all of them.
 */
@Suppress("ComposeCompositionLocalUsage")
internal val LocalKidPalette = staticCompositionLocalOf { KidPalette.Light }
