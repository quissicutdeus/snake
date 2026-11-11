package com.quissicutdeus.snake.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quissicutdeus.snake.ComposeCanvas2D
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.render.PosedSnake

/**
 * "Who's playing?": a picture of each kid's snake to tap, and a "+" for a new
 * kid. With only one kid there's nobody to pick, so it's just a small
 * "+ Add player" that keeps the title screen as tidy as before.
 */
@Composable
fun WhoIsPlaying(
    players: List<PlayerCard>,
    current: Int,
    onPick: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (players.size <= 1) {
        AddPlayerPill(onAdd, modifier.padding(bottom = 8.dp))
        return
    }
    val face = 56.dp
    Row(modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEach { p -> PlayerChip(p, p.id == current, face, { onPick(p.id) }) }
        if (players.size < Storage.MAX_PLAYERS) AddChip(face, onAdd)
    }
}

/** A kid's snake face in a circle with their name under it; the one playing has a yellow ring. */
@Composable
private fun PlayerChip(
    player: PlayerCard,
    isCurrent: Boolean,
    face: Dp,
    onClick: () -> Unit,
) {
    Chip(
        label = player.label,
        face = face,
        onClick = onClick,
        modifier =
            Modifier.semantics {
                selected = isCurrent
                contentDescription = if (isCurrent) "${player.label} is playing" else "Switch to ${player.label}"
            },
        bold = isCurrent,
    ) {
        SnakeHead(
            player.style,
            Modifier
                .size(face)
                .clip(CircleShape)
                .background(if (isCurrent) Color.White else Color(0x99FFFFFF))
                .border(if (isCurrent) 4.dp else 2.dp, if (isCurrent) KidColors.Sunny else Color(0x552B2B3A), CircleShape),
        )
    }
}

@Composable
private fun AddChip(
    face: Dp,
    onClick: () -> Unit,
) {
    Chip("Add", face, onClick, Modifier.semantics { contentDescription = "Add a player" }) {
        Box(
            Modifier
                .size(face)
                .clip(CircleShape)
                .background(KidColors.Sky)
                .border(2.dp, Color(0x552B2B3A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        }
    }
}

/** A round picture over a one-line label, the whole thing one big tap target. */
@Composable
private fun Chip(
    label: String,
    face: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .width(face + 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        val style = MaterialTheme.typography.titleSmall
        ShadowText(
            label,
            if (bold) style.copy(fontWeight = FontWeight.Black) else style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The small "+ Add player" shown while only one kid plays. */
@Composable
private fun AddPlayerPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x55FFFFFF))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        ShadowText("+ Add player", MaterialTheme.typography.titleMedium)
    }
}

/** A close-up of a snake's head (with its hat), looking right, the neck trailing off to the left. */
@Composable
private fun SnakeHead(
    style: SnakeStyle,
    modifier: Modifier = Modifier,
) {
    val posed = remember(style) { PosedSnake(style, HEAD_LENGTH).apply { wave(0f, 0f, 0f, amplitude = 0f) } }
    val adapter = remember { ComposeCanvas2D() }
    Canvas(modifier) {
        val r = GameConfig.radiusFor(HEAD_LENGTH)
        adapter.draw(this) { c ->
            c.save()
            c.translate(size.width / 2f, size.height / 2f)
            c.scale(size.minDimension / (r * FRAME_RADII))
            // Nudge the head down so the face and hat both fit.
            c.translate(-r * 0.15f, r * 0.45f)
            posed.draw(c, 0f)
            c.restore()
        }
    }
}

private const val HEAD_LENGTH = 60f

/** How many snake radii fit across a head picture: the head, hat and a bit of neck. */
private const val FRAME_RADII = 4.4f
