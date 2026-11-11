package com.quissicutdeus.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Labelled
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.Unlock
import com.quissicutdeus.snake.game.render.Mood
import com.quissicutdeus.snake.game.sim.Scoreboard

@Composable
fun GameOverScreen(
    result: GameResult,
    unlocked: List<Unlock>,
    storage: Storage,
    onSaved: (timestamp: Long) -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qualifies = remember(result) { Scoreboard.qualifies(storage.loadScores(), result.score) }
    val names = remember { storage.loadNames() }

    fun save(name: String) {
        storage.addScore(ScoreEntry(Scoreboard.cleanName(name), result.score, result.snakesEaten, result.style, result.timestamp))
        onSaved(result.timestamp)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(KidColors.Grass)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        if (qualifies && maxWidth > maxHeight) {
            // Landscape: stats and buttons on the left, the name picker on the right.
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameOverStats(result)
                    UnlockedBanner(result, unlocked)
                    Spacer(Modifier.height(20.dp))
                    GameOverButtons(qualifies, onPlayAgain, onHome)
                }
                Spacer(Modifier.size(16.dp))
                WhoPlayedPanel(names, ::save, Modifier.weight(1.1f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GameOverStats(result)
                UnlockedBanner(result, unlocked)
                Spacer(Modifier.height(16.dp))
                if (qualifies) {
                    WhoPlayedPanel(names, ::save)
                    Spacer(Modifier.height(16.dp))
                }
                GameOverButtons(qualifies, onPlayAgain, onHome)
            }
        }
    }
}

@Composable
private fun GameOverStats(
    result: GameResult,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val eater = result.eater
        if (eater == null) {
            ShadowText("Gobbled up!", MaterialTheme.typography.displayLarge)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SnakePicture(eater.style, Modifier.size(72.dp), mood = Mood.ANGRY)
                ShadowText(
                    "${eater.name} gobbled you!",
                    MaterialTheme.typography.headlineLarge,
                    Modifier.padding(start = 10.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SnakePicture(result.style, Modifier.size(96.dp), mood = Mood.SCARED)
            Column(Modifier.padding(start = 12.dp)) {
                ShadowText("Length ${result.score}", MaterialTheme.typography.headlineLarge)
                ShadowText(
                    when (result.snakesEaten) {
                        0 -> "No snakes eaten this time"
                        1 -> "Ate 1 snake!"
                        else -> "Ate ${result.snakesEaten} snakes!"
                    },
                    MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

/** "New hat unlocked!": the player's snake wearing what this game earned. */
@Composable
private fun UnlockedBanner(
    result: GameResult,
    unlocked: List<Unlock>,
    modifier: Modifier = Modifier,
) {
    if (unlocked.isEmpty()) return
    val wearing =
        unlocked.fold(result.style) { s, u ->
            when (val item = u.item) {
                is Hat -> s.copy(hat = item)
                is Eyes -> s.copy(eyes = item)
                is Pattern -> s.copy(pattern = item)
                else -> s
            }
        }
    val names = unlocked.joinToString(" and ") { (it.item as? Labelled)?.label ?: it.item.name }
    Surface(modifier.padding(top = 12.dp), shape = RoundedCornerShape(24.dp), color = KidColors.Sunny) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SnakePicture(wearing, Modifier.size(64.dp))
            Text(
                "Unlocked: $names!",
                style = MaterialTheme.typography.titleLarge,
                color = KidColors.Ink,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun GameOverButtons(
    qualifies: Boolean,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        KidButton("Play again", onPlayAgain, big = !qualifies)
        KidButton("Home", onHome, color = KidColors.Grape, big = false)
    }
}

/** "Who played?": tap a name used before, or type a new one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhoPlayedPanel(
    names: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typed by remember { mutableStateOf("") }
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = KidColors.Panel) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "New high score! Who played?",
                style = MaterialTheme.typography.titleLarge,
                color = KidColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            if (names.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    names.forEach { name -> KidButton(name, { onPick(name) }, color = KidColors.Sky, big = false) }
                }
                Spacer(Modifier.height(12.dp))
            }
            // Side by side when there's room; Save drops below the box on narrow screens.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(Scoreboard.MAX_NAME_LENGTH) },
                    label = { Text(if (names.isEmpty()) "Your name" else "Someone new") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) onPick(typed) }),
                    modifier = Modifier.widthIn(max = 220.dp),
                    textStyle = MaterialTheme.typography.titleLarge,
                )
                KidButton("Save", { onPick(typed) }, color = KidColors.DeepGreen, big = false, enabled = typed.isNotBlank())
            }
        }
    }
}
