package com.quissicutdeus.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quissicutdeus.snake.game.Difficulty

/** A kid's little kid mode, and how to switch it (see Difficulty.LITTLE). */
class LittleKidMode(
    val on: Boolean,
    val onChange: (Boolean) -> Unit,
)

/** How hard a game for a kid with little kid mode [on] (or off) is. */
fun difficultyFor(on: Boolean): Difficulty = if (on) Difficulty.LITTLE else Difficulty.NORMAL

/** What marks a little-kid game, e.g. next to its high score. */
const val LITTLE_KID_EMOJI = "🐣"

/**
 * My Snake's big "Little kid mode" switch: the whole row flips it, and a
 * line under it says what it does, so a grown-up knows what they're turning on.
 */
@Composable
internal fun LittleKidSwitch(
    mode: LittleKidMode,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (mode.on) Color(0xFFFFF1B8) else Color(0xFFE8F5E0))
            .toggleable(value = mode.on, role = Role.Switch, onValueChange = mode.onChange)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(LITTLE_KID_EMOJI, fontSize = 34.sp, modifier = Modifier.clearAndSetSemantics {})
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text("Little kid mode", style = MaterialTheme.typography.titleLarge, color = KidColors.Ink)
            Text("Gentler snakes and more food", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF55606A))
        }
        Switch(
            checked = mode.on,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = KidColors.Sunny, checkedThumbColor = Color.White),
        )
    }
}

/** The little 🐣 next to a high score played in little kid mode. */
@Composable
internal fun LittleKidMark(modifier: Modifier = Modifier) {
    Text(
        LITTLE_KID_EMOJI,
        modifier.semantics { contentDescription = "Little kid mode" },
        style = MaterialTheme.typography.titleLarge,
    )
}
