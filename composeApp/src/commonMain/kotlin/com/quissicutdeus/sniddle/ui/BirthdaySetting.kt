package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.Dates

/** A kid's birthday on My Snails, and how to change it (kept together to keep parameter lists short). */
class BirthdaySetting(
    val birthday: Birthday?,
    val onChange: (Birthday?) -> Unit,
)

private val MONTH_NAMES =
    listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

/** "March 12". */
internal fun Birthday.label() = "${MONTH_NAMES[month - 1]} $day"

/**
 * My Snails' birthday button: shows the birthday (or asks for one) and opens
 * a big friendly picker. Only the month and day, never the year: it's for a
 * birthday hat, not for knowing anyone's age.
 */
@Composable
internal fun BirthdayButton(
    setting: BirthdaySetting,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val text = setting.birthday?.let { "🎂 ${it.label()}" } ?: "🎂 Add my birthday"
    KidButton(text, { picking = true }, modifier, color = KidColors.Berry, big = false)
    if (picking) {
        BirthdayDialog(
            setting.birthday,
            onPick = {
                setting.onChange(it)
                picking = false
            },
            onClose = { picking = false },
        )
    }
}

/**
 * First the month (twelve big buttons), then the day (a big number for
 * every day it has, 29 February too). "No birthday" takes it away again.
 */
@Composable
private fun BirthdayDialog(
    current: Birthday?,
    onPick: (Birthday?) -> Unit,
    onClose: () -> Unit,
) {
    // 0 while choosing the month.
    var month by rememberSaveable { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = KidColors.Panel) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    if (month == 0) "When's your birthday?" else "${MONTH_NAMES[month - 1]}: which day?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = KidColors.Text,
                    textAlign = TextAlign.Center,
                )
                if (month == 0) {
                    MonthGrid(current?.month, { month = it })
                    if (current != null) KidButton("No birthday", { onPick(null) }, color = Color(0xFF8A8A8A), big = false)
                    KidButton("Cancel", onClose, color = KidColors.Sky, big = false)
                } else {
                    DayGrid(month, current?.takeIf { it.month == month }?.day, { onPick(Birthday(month, it)) })
                    KidButton("Back", { month = 0 }, color = KidColors.Sky, big = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(
    selected: Int?,
    onPick: (Int) -> Unit,
) {
    FlowRow(
        Modifier.widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MONTH_NAMES.forEachIndexed { i, name ->
            val isSelected = selected == i + 1
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) KidColors.Sky else KidColors.Chip)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Button) { onPick(i + 1) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = if (isSelected) Color.White else KidColors.Text)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayGrid(
    month: Int,
    selected: Int?,
    onPick: (Int) -> Unit,
) {
    FlowRow(
        Modifier.widthIn(max = 392.dp), // seven days to a row
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (day in 1..Dates.daysIn(month)) {
            val isSelected = selected == day
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) KidColors.Berry else KidColors.Sunny)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Button) { onPick(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$day", style = MaterialTheme.typography.titleLarge, color = if (isSelected) Color.White else KidColors.Ink)
            }
        }
    }
}
