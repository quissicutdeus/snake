package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One line of "What's new": [number]s only ever go up, so the newest one seen is all a device needs to remember. */
data class News(
    val number: Int,
    val emoji: String,
    val line: String,
)

/**
 * What each update brought, in kid words, oldest first. Add new lines at the
 * end with the next number; a device that has seen up to number N is shown
 * the ones after it once, on the title screen. Fresh installs skip all of
 * these (see [DeviceNotes]), so only add what kids who already play would
 * notice.
 */
val WhatsNew: List<News> =
    listOf(
        News(1, "🎅", "Holiday hats to win!"),
        News(2, "🧒", "Every kid gets their own snail!"),
        News(3, "🐣", "Little kid mode in My Snails!"),
        News(4, "🎂", "Birthday hats and bunny ears!"),
        News(5, "👫", "Play together on one screen!"),
        News(6, "🐌", "The snakes are snails now, with slime trails!"),
        News(7, "🤠", "New hats and eyes, and eyes in your snail's colors!"),
        News(8, "🪐", "New places to play: garden dirt, outer space, and wrapping paper at Christmas!"),
        News(9, "🌙", "A night look when your phone is in dark mode!"),
        News(10, "🐰", "Holiday hats now last the whole month!"),
    )

/**
 * The "New!" card on the title screen, listing [news]. Like
 * [AlreadyEarnedBanner]: [onShown] runs once it's on screen (so it's never
 * shown again) and a tap makes it go away.
 */
@Composable
fun WhatsNewCard(
    news: List<News>,
    onShown: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (news.isEmpty()) return
    LaunchedEffect(news) { onShown() }
    BoxWithConstraints(
        modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .widthIn(max = 400.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KidColors.Sunny)
            .clickable(onClickLabel = "Close", onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Side by side when there's room, the lines wrapping beside "New!" (so even a
        // cramped title has a line to show); "New!" goes on top on a narrow phone.
        if (maxWidth >= SIDE_BY_SIDE_WIDTH) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NewPill()
                NewestThatFit(2.dp, Modifier.weight(1f)) { NewsLines(news) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NewPill()
                NewestThatFit(2.dp) { NewsLines(news) }
            }
        }
    }
}

/** Below this width the "New!" pill goes above the lines instead of beside them. */
private val SIDE_BY_SIDE_WIDTH = 300.dp

@Composable
private fun NewPill() {
    Text(
        "New!",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(KidColors.Berry)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun NewsLines(news: List<News>) {
    news.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, fontSize = 24.sp)
            Text(
                item.line,
                style = MaterialTheme.typography.titleMedium,
                color = KidColors.Ink,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * Lines one under another, [gap] apart, oldest first; when there isn't room
 * for them all (a short phone in landscape, with every kid's picture above),
 * the oldest are left out, so the newest always shows.
 */
@Composable
private fun NewestThatFit(
    gap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val lines = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)) }
        var first = lines.size
        var height = 0
        while (first > 0) {
            val more = lines[first - 1].height + if (first < lines.size) gapPx else 0
            if (first < lines.size && height + more > constraints.maxHeight) break
            height += more
            first--
        }
        val shown = lines.subList(first, lines.size)
        val width = (shown.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            shown.forEach {
                it.placeRelative(0, y)
                y += it.height + gapPx
            }
        }
    }
}
