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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.SnakeStyle
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

/** Customize your snake. Everything is unlocked; every change is saved straight away. */
@Composable
fun MySnakeScreen(
    style: SnakeStyle,
    onChange: (SnakeStyle) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(KidColors.Grass)
            .safeDrawingPadding(),
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize().padding(12.dp)) {
                SnakePreview(style, onChange, onDone, 170.dp, Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.size(12.dp))
                StyleControls(style, onChange, Modifier.weight(1.1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                SnakePreview(style, onChange, onDone, 150.dp, Modifier.fillMaxWidth())
                Spacer(Modifier.size(10.dp))
                StyleControls(style, onChange, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** Title, the live wiggling snake, and the Surprise me / Done buttons. */
@Composable
private fun SnakePreview(
    style: SnakeStyle,
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
            KidButton("Surprise me!", { onChange(SnakeStyle.random(rng)) }, color = KidColors.Berry, big = false)
            KidButton("Done", onDone, color = KidColors.DeepGreen, big = false)
        }
    }
}

@Composable
private fun StyleControls(
    style: SnakeStyle,
    onChange: (SnakeStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = KidColors.Panel) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Section("Body color") {
                Swatches(style.color1, { onChange(style.copy(color1 = it)) })
            }
            Section("Pattern color") {
                Swatches(style.color2, { onChange(style.copy(color2 = it)) })
            }
            Section("Pattern") {
                Choices(Pattern.entries, style.pattern, { it.label }, { onChange(style.copy(pattern = it)) })
            }
            Section("Eyes") {
                Choices(Eyes.entries, style.eyes, { it.label }, { onChange(style.copy(eyes = it)) })
            }
            Section("Hat") {
                Choices(Hat.entries, style.hat, { it.label }, { onChange(style.copy(hat = it)) })
            }
        }
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) KidColors.Sky else Color(0xFFE8F5E0))
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.RadioButton) { onPick(option) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(label(option), style = MaterialTheme.typography.titleMedium, color = if (isSelected) Color.White else KidColors.Ink)
            }
        }
    }
}
