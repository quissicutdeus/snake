package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quissicutdeus.sniddle.game.model.Unlock

/** Below this height the end screen tightens up (a big phone held sideways). */
private val CompactHeight = 520.dp

/**
 * After a two-player game: both snails, both lengths, who was biggest, and a
 * cheer for both. Each kid's score is already on the high-score list if it
 * made it, and their new looks are shown under their snail.
 */
@Composable
fun TogetherOverScreen(
    result: TogetherResult,
    unlocked: List<List<Unlock>>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val best = result.kids.maxOf { it.score }
    val winners = result.kids.filter { it.score == best }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(KidColors.MenuBackground)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val compact = maxHeight < CompactHeight
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShadowText(
                if (winners.size > 1) "Same size! 🤝" else "${winners[0].name} is the biggest!",
                if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                result.kids.forEachIndexed { i, kid ->
                    KidPanel(kid, isBiggest = kid.score == best && winners.size == 1, unlocked.getOrNull(i).orEmpty(), compact)
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            ShadowText(
                "Great teamwork, ${result.kids.joinToString(" and ") { it.name }}!",
                MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(if (compact) 10.dp else 20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                KidButton("Play again", onPlayAgain)
                KidButton("Home", onHome, color = KidColors.Grape, big = false)
            }
        }
    }
}

private val PanelWidth = 300.dp

/** One kid's game: their snail, name, length and snails eaten, a crown for the biggest, and any new looks. */
@Composable
private fun KidPanel(
    kid: KidResult,
    isBiggest: Boolean,
    unlocked: List<Unlock>,
    compact: Boolean,
) {
    Surface(
        Modifier.width(PanelWidth),
        shape = RoundedCornerShape(24.dp),
        color = if (isBiggest) KidColors.Sunny else KidColors.Panel,
    ) {
        // The biggest kid's card is yellow, which keeps dark text day and night.
        val onCard = if (isBiggest) KidColors.Ink else KidColors.Text
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val picture: Dp = if (compact) 64.dp else 110.dp
            Row(verticalAlignment = Alignment.CenterVertically) {
                SnailPicture(kid.style, Modifier.size(picture))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        (if (isBiggest) "👑 " else "") + kid.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = onCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("Length ${kid.score}", style = MaterialTheme.typography.titleLarge, color = onCard)
                    Text(eatenLine(kid.snailsEaten), style = MaterialTheme.typography.titleSmall, color = onCard)
                }
            }
            if (unlocked.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                UnlockBanner(kid.style, unlocked, "Unlocked:")
            }
            val extra =
                listOfNotNull(
                    "Biggest snail for ${duration(kid.biggestSeconds)}".takeIf { kid.biggestSeconds > 0 },
                    comebacks(kid.timesGobbled),
                )
            if (!compact && extra.isNotEmpty()) {
                Text(
                    extra.joinToString("\n"),
                    style = MaterialTheme.typography.titleSmall,
                    // Softer than the lines above it: by day the same 80% Ink as before.
                    color = onCard.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** "Came back 2 times!" (after being gobbled), or null if they never were. */
private fun comebacks(times: Int) =
    when (times) {
        0 -> null
        1 -> "Came back once!"
        else -> "Came back $times times!"
    }

private fun eatenLine(eaten: Int) =
    when (eaten) {
        0 -> "No snails eaten"
        1 -> "Ate 1 snail!"
        else -> "Ate $eaten snails!"
    }
