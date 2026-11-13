package com.quissicutdeus.sniddle.game.sim

/**
 * Where each user-perceived character (grapheme cluster) of [text] ends, in order, as UTF-16
 * offsets into [text]: a letter with its accents, or a whole emoji such as a family, a flag or a
 * thumbs-up with a skin tone, is one cluster. The last end is `text.length`; an empty [text] has
 * none.
 *
 * Each platform asks its own Unicode text library, which follows the extended grapheme rules
 * (emoji ZWJ sequences included). ScoreboardTest checks the cases [Scoreboard.fitName] relies on.
 */
internal expect fun graphemeEnds(text: String): Iterator<Int>
