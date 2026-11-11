package com.quissicutdeus.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Labelled
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import com.quissicutdeus.snake.game.sim.Scoreboard
import kotlin.random.Random

private val COLOR_NAMES =
    listOf(
        "Red",
        "Orange",
        "Yellow",
        "Lime",
        "Green",
        "Teal",
        "Sky blue",
        "Blue",
        "Purple",
        "Pink",
        "White",
        "Chocolate",
    )

/** Customize your snake and name it. Every change is saved straight away. */
@Composable
fun MySnakeScreen(
    style: SnakeStyle,
    onChange: (SnakeStyle) -> Unit,
    onDone: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    knownNames: List<String>,
    stats: Stats,
    modifier: Modifier = Modifier,
) {
    val naming = Naming(name, onNameChange, knownNames)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(KidColors.Grass)
            .safeDrawingPadding(),
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize().padding(12.dp)) {
                SnakePreview(style, stats, onChange, onDone, 170.dp, Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.size(12.dp))
                StyleControls(style, stats, onChange, naming, Modifier.weight(1.1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                SnakePreview(style, stats, onChange, onDone, 150.dp, Modifier.fillMaxWidth())
                Spacer(Modifier.size(10.dp))
                StyleControls(style, stats, onChange, naming, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** Title, the live wiggling snake, and the Surprise me / Done buttons. */
@Composable
private fun SnakePreview(
    style: SnakeStyle,
    stats: Stats,
    onChange: (SnakeStyle) -> Unit,
    onDone: () -> Unit,
    snakeHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val rng = remember { Random(System.nanoTime()) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        ShadowText("My Snake", MaterialTheme.typography.headlineLarge)
        WigglingSnake(style, Modifier.fillMaxWidth().height(snakeHeight))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KidButton("Surprise me!", { onChange(Unlocks.randomStyle(rng, stats)) }, color = KidColors.Berry, big = false)
            KidButton("Done", onDone, color = KidColors.DeepGreen, big = false)
        }
    }
}

@Composable
private fun StyleControls(
    style: SnakeStyle,
    stats: Stats,
    onChange: (SnakeStyle) -> Unit,
    naming: Naming,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = KidColors.Panel) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Section("Name") {
                NamePicker(naming)
            }
            Section("Body color") {
                Swatches(style.color1, { onChange(style.copy(color1 = it)) })
            }
            Section("Pattern color") {
                Swatches(style.color2, { onChange(style.copy(color2 = it)) })
            }
            Section("Pattern") {
                Choices(Pattern.entries, style.pattern, stats, { onChange(style.copy(pattern = it)) })
            }
            Section("Eyes") {
                Choices(Eyes.entries, style.eyes, stats, { onChange(style.copy(eyes = it)) })
            }
            Section("Hat") {
                Choices(Hat.entries, style.hat, stats, { onChange(style.copy(hat = it)) })
            }
        }
    }
}

/** The snake's name and how to change it (kept together to keep parameter lists short). */
private class Naming(
    val name: String,
    val onChange: (String) -> Unit,
    val known: List<String>,
)

/** Tap a name used before (from the high scores), or type one. Blank means "You". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NamePicker(naming: Naming) {
    val current = Scoreboard.snakeName(naming.name)
    val options = (listOf("You") + naming.known).distinct()
    if (options.size > 1) {
        NameChoices(options, current, { naming.onChange(if (it == "You") "" else it) })
        Spacer(Modifier.height(10.dp))
    }
    OutlinedTextField(
        value = naming.name,
        onValueChange = { naming.onChange(it.take(Scoreboard.MAX_NAME_LENGTH)) },
        label = { Text("Type a name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.widthIn(max = 260.dp),
        textStyle = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.padding(bottom = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = KidColors.Ink)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(
    selected: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnakeStyle.PALETTE.forEachIndexed { i, argb ->
            val isSelected = i == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(if (isSelected) 4.dp else 2.dp, if (isSelected) KidColors.Ink else Color(0x332B2B3A), CircleShape)
                    .semantics {
                        contentDescription = COLOR_NAMES[i]
                        this.selected = isSelected
                    }.clickable(role = Role.RadioButton) { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.titleLarge,
                        color =
                            if (i == 10 ||
                                i == 2
                            ) {
                                KidColors.Ink
                            } else {
                                Color.White
                            },
                    )
                }
            }
        }
    }
}

/** Pattern, eye or hat choices. Locked ones are dimmed with their goal and can't be picked yet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    stats: Stats,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) where T : Enum<T>, T : Labelled {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val goal = Unlocks.ruleFor(option)?.takeUnless { it.reachedBy(stats) }?.goal
            Pill(option.label, option == selected, goal, { onPick(option) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameChoices(
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> Pill(option, option == selected, null, { onPick(option) }) }
    }
}

/** A rounded choice button; with a [lockedGoal] it's greyed out, shows a lock and the goal, and can't be tapped. */
@Composable
private fun Pill(
    label: String,
    isSelected: Boolean,
    lockedGoal: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = lockedGoal != null
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    isSelected -> KidColors.Sky
                    locked -> Color(0xFFEDEDED)
                    else -> Color(0xFFE8F5E0)
                },
            ).semantics {
                this.selected = isSelected
                if (locked) contentDescription = "$label, locked: $lockedGoal"
            }.clickable(enabled = !locked, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = if (locked) 6.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒 $label", style = MaterialTheme.typography.titleMedium, color = Color(0xFF8A8A8A))
                Text(lockedGoal, style = MaterialTheme.typography.labelMedium, color = Color(0xFF8A8A8A))
            }
        } else {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (isSelected) Color.White else KidColors.Ink)
        }
    }
}
