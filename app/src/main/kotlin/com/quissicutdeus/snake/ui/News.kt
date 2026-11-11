package com.quissicutdeus.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
        News(2, "🧒", "Every kid gets their own snake!"),
    )

/**
 * The "New!" card on the title screen, listing [news]. Like
 * [AlreadyEarnedBanner]: [onShown] runs once it's on screen (so it's never
 * shown again) and a tap makes it go away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsNewCard(
    news: List<News>,
    onShown: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (news.isEmpty()) return
    LaunchedEffect(news) { onShown() }
    // Side by side when there's room; "New!" goes on top on a narrow phone.
    FlowRow(
        modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .widthIn(max = 400.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KidColors.Sunny)
            .clickable(onClickLabel = "Close", onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    }
}
