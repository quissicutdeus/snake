package com.quissicutdeus.sniddle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quissicutdeus.sniddle.PlayClock
import kotlin.random.Random

/**
 * A small grey gear in the title screen's corner: the grown-up check, then
 * the bedtime timer's settings. Kids see a gear that does nothing they can use.
 */
@Composable
internal fun GrownUpsCorner(
    bedtime: Bedtime,
    modifier: Modifier = Modifier,
) {
    // 0: closed, 1: asking the grown-up question, 2: the settings.
    var stage by rememberSaveable { mutableIntStateOf(0) }
    Surface(
        onClick = { stage = 1 },
        modifier = modifier.size(44.dp).semantics { contentDescription = "Grown-ups: bedtime timer" },
        shape = CircleShape,
        color = Color(0x662B2B3A),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) { Text("⚙", fontSize = 24.sp) }
    }
    when (stage) {
        1 -> GrownUpGate(onPass = { stage = 2 }, onCancel = { stage = 0 })
        2 -> BedtimePanel(bedtime, onDone = { stage = 0 })
    }
}

/**
 * "What is 17 × 8?": a teen number times 6 to 9. Easy in a grown-up's head,
 * out of reach for a young child, and past the times tables a school kid
 * knows by heart.
 */
internal fun grownUpQuestion(rng: Random): Pair<Int, Int> = rng.nextInt(13, 20) to rng.nextInt(6, 10)

/**
 * The grown-up check, over the whole screen: one times-table question with
 * the answer typed on the number pad. Nothing to remember or set up, and a
 * wrong answer brings a different question, so a child can't just keep
 * guessing at one.
 */
@Composable
internal fun GrownUpGate(
    onPass: () -> Unit,
    onCancel: () -> Unit,
    rng: Random = Random.Default,
) {
    var question by rememberSaveable { mutableStateOf(grownUpQuestion(rng)) }
    var typed by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }

    fun check() {
        if (typed.trim().toIntOrNull() == question.first * question.second) {
            onPass()
        } else {
            wrong = true
            typed = ""
            question = generateSequence { grownUpQuestion(rng) }.first { it != question }
        }
    }

    GrownUpsCard(onCancel) {
        PanelTitle("Grown-ups only")
        Text(
            "What is ${question.first} × ${question.second}?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = KidColors.Ink,
        )
        if (wrong) Text("Not quite. Try this one.", style = MaterialTheme.typography.titleMedium, color = KidColors.Berry)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filter(Char::isDigit).take(4) },
                label = { Text("Answer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { check() }),
                modifier = Modifier.widthIn(max = 140.dp),
                textStyle = MaterialTheme.typography.titleLarge,
            )
            KidButton("OK", ::check, color = KidColors.DeepGreen, big = false, enabled = typed.isNotBlank())
        }
        KidButton("Cancel", onCancel, color = KidColors.Grape, big = false)
    }
}

/** The bedtime timer's settings: how long a day, and how today is going. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BedtimePanel(
    bedtime: Bedtime,
    onDone: () -> Unit,
) {
    GrownUpsCard(onDone) {
        PanelTitle("Bedtime timer")
        Text(
            "Play time a day, for the whole phone. Only a running game counts; it starts again at midnight.",
            style = MaterialTheme.typography.bodyLarge,
            color = KidColors.Ink,
            textAlign = TextAlign.Center,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Bedtime.CHOICES.forEach { minutes ->
                LimitChip(minutes, picked = minutes == bedtime.limitMinutes) { bedtime.setLimit(minutes) }
            }
        }
        Text(todayLine(bedtime.clock), style = MaterialTheme.typography.titleMedium, color = KidColors.Ink, textAlign = TextAlign.Center)
        KidButton("Done", onDone, color = KidColors.Sky, big = false)
    }
}

/** One of the limits to pick: "Off", "15 min"... */
@Composable
private fun LimitChip(
    minutes: Int,
    picked: Boolean,
    onPick: () -> Unit,
) {
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(50),
        color = if (picked) KidColors.DeepGreen else Color(0xFFDADAE0),
        contentColor = if (picked) Color.White else KidColors.Ink,
    ) {
        Text(
            if (minutes == 0) "Off" else "$minutes min",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** "Played today: 12 min (3 min left)". */
private fun todayLine(clock: PlayClock): String {
    val played = "Played today: ${clock.playedMs / PlayClock.MS_PER_MINUTE} min"
    val left = clock.leftMs() ?: return played
    return "$played (${(left + PlayClock.MS_PER_MINUTE - 1) / PlayClock.MS_PER_MINUTE} min left)"
}

@Composable
private fun PanelTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
        color = KidColors.Ink,
    )
}

/**
 * A white card in the middle of a dimmed screen that nothing behind can be
 * tapped through; back (or [onClose]) closes it.
 */
@Composable
private fun GrownUpsCard(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC2B2B3A))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = KidColors.Panel, modifier = Modifier.widthIn(max = 600.dp).padding(16.dp)) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
        }
    }
}
