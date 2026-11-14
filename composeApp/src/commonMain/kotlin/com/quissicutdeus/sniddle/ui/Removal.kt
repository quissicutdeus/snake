package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.render.Mood

/** Taking the kid on My Snails off the phone: who they are (for the question) and what to do once they're sure. */
class Removal(
    /** Their snail's name, or "Player 2". */
    val name: String,
    val onRemove: () -> Unit,
)

/**
 * A quiet "Remove player" button that asks first. The question shows the
 * snail and its name, "Keep" is the big easy answer, and removing needs the
 * small button that says exactly what it does, so a stray tap can't do it.
 */
@Composable
internal fun RemovePlayerButton(
    removal: Removal,
    style: SnailStyle,
    modifier: Modifier = Modifier,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    KidButton("Remove player", { asking = true }, modifier, color = Color(0xFF8A8A8A), big = false)
    if (asking) RemoveDialog(removal, style, onKeep = { asking = false })
}

@Composable
private fun RemoveDialog(
    removal: Removal,
    style: SnailStyle,
    onKeep: () -> Unit,
) {
    Dialog(onDismissRequest = onKeep) {
        Surface(shape = RoundedCornerShape(28.dp), color = KidColors.Panel) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SnailPicture(style, Modifier.size(72.dp), mood = Mood.SCARED)
                Text(
                    "Remove ${removal.name}'s snail?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = KidColors.Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Its looks and unlocks go away for good.",
                    style = MaterialTheme.typography.titleMedium,
                    color = KidColors.Ink,
                    textAlign = TextAlign.Center,
                )
                KidButton("Keep", onKeep, color = KidColors.DeepGreen)
                KidButton("Yes, remove ${removal.name}", removal.onRemove, color = KidColors.Berry, big = false)
            }
        }
    }
}
