package com.quissicutdeus.snake.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.model.ScoreEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HighScoresScreen(
    storage: Storage,
    highlight: Long?,
    onPlay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scores by remember { mutableStateOf(storage.loadScores()) }
    Box(
        modifier
            .fillMaxSize()
            .background(KidColors.Grass)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShadowText("High Scores", MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            ScoreList(
                scores,
                highlight,
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            // Buttons wrap onto a second line on narrow (portrait) screens.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                KidButton("Back", onBack, color = KidColors.Grape, big = false)
                KidButton("Play!", onPlay, big = false)
                if (scores.isNotEmpty()) {
                    HoldToReset({
                        storage.resetScores()
                        scores = emptyList()
                    })
                }
            }
        }
    }
}

/** The top-10 list, scrolled so that the [highlight]ed (new) entry is in view. */
@Composable
private fun ScoreList(
    scores: List<ScoreEntry>,
    highlight: Long?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(highlight) {
        val i = scores.indexOfFirst { it.timestamp == highlight }
        // Bring the new entry into view, but don't scroll the top of the list away needlessly.
        val info = listState.layoutInfo
        val fullyVisible = info.visibleItemsInfo.any { it.index == i && it.offset >= 0 && it.offset + it.size <= info.viewportEndOffset }
        if (i >= 0 && !fullyVisible) listState.animateScrollToItem((i - 1).coerceAtLeast(0))
    }
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = KidColors.Panel) {
        if (scores.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "No scores yet.\nGo play and get big!",
                    style = MaterialTheme.typography.titleLarge,
                    color = KidColors.Ink,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(state = listState, contentPadding = PaddingValues(10.dp)) {
                itemsIndexed(scores, key = { _, e -> e.timestamp }) { i, e -> ScoreRow(i + 1, e, e.timestamp == highlight) }
            }
        }
    }
}

@Composable
private fun ScoreRow(
    rank: Int,
    e: ScoreEntry,
    highlighted: Boolean,
) {
    val medal =
        when (rank) {
            1 -> Color(0xFFFFC928)
            2 -> Color(0xFFC0C8D0)
            3 -> Color(0xFFE0A060)
            else -> Color(0xFFE8F5E0)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) KidColors.Sunny else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(medal),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", style = MaterialTheme.typography.titleLarge, color = KidColors.Ink)
        }
        SnakePicture(e.style, Modifier.padding(horizontal = 8.dp).size(56.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.name,
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleLarge,
                    color = KidColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (e.littleKid) LittleKidMark(Modifier.padding(start = 6.dp))
            }
            Text(
                when (e.snakesEaten) {
                    0 -> "no snakes eaten"
                    1 -> "ate 1 snake"
                    else -> "ate ${e.snakesEaten} snakes"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF55606A),
            )
        }
        Text("${e.score}", style = MaterialTheme.typography.headlineMedium, color = KidColors.Ink)
    }
}

/** Resetting needs a 3-second press so it can't happen by accident. */
@Composable
private fun HoldToReset(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var holding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = if (holding) tween(3000, easing = LinearEasing) else tween(250),
        label = "reset",
    )
    LaunchedEffect(progress, holding) {
        if (holding && progress >= 1f) {
            holding = false
            onReset()
        }
    }
    Box(
        modifier
            .height(52.dp)
            .width(190.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x55FFFFFF))
            .semantics { contentDescription = "Hold for 3 seconds to reset high scores" }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    holding = true
                    tryAwaitRelease()
                    holding = false
                })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(KidColors.Berry),
        )
        Text(
            if (holding) "Keep holding…" else "Hold to reset",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = KidColors.Ink,
            textAlign = TextAlign.Center,
        )
    }
}
