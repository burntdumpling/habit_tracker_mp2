package com.example.application.bottom_sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private val mskLocale: Locale = Locale.forLanguageTag("ru-RU")
private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", mskLocale)
private val dayNames = listOf(
    "\u041F\u043D",
    "\u0412\u0442",
    "\u0421\u0440",
    "\u0427\u0442",
    "\u041F\u0442",
    "\u0421\u0431",
    "\u0412\u0441"
)

@Composable
fun CalendarSheet(
    initialDate: LocalDate,
    habits: List<Habit>,
    onDateSelected: (LocalDate) -> Unit,
    onClose: () -> Unit
) {
    var visibleMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }

    val markedDates = remember(visibleMonth, habits) {
        (1..visibleMonth.lengthOfMonth())
            .map { visibleMonth.atDay(it) }
            .filter { date -> habits.any { habit -> habit.isScheduledFor(date) } }
            .toSet()
    }

    val firstOfMonth = visibleMonth.atDay(1)
    val leadingPlaceholders = firstOfMonth.dayOfWeek.value - 1 // Monday = 1
    val days = buildList<LocalDate?> {
        repeat(leadingPlaceholders) { add(null) }
        addAll((1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) })
    }
    val rowCount = ceil(days.size / 7f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                Text(text = "\u2039", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = visibleMonth.atDay(1).format(monthTitleFormatter),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                Text(text = "\u203A", style = MaterialTheme.typography.titleLarge)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        repeat(rowCount) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val date = days.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            CalendarDayCell(
                                date = date,
                                isSelected = date == initialDate,
                                hasHabit = markedDates.contains(date),
                                onClick = {
                                    onDateSelected(date)
                                    onClose()
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasHabit: Boolean,
    onClick: () -> Unit
) {
    val outlineColor = if (isSelected) Color(0xFF4F8CFF) else Color.Transparent
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = outlineColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        if (hasHabit) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF4F8CFF))
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun Habit.isScheduledFor(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    if (endDate != null && date.isAfter(endDate)) return false

    return when (val f = frequency) {
        is Frequency.Daily -> true
        is Frequency.Weekly -> date.dayOfWeek in f.weekDays
        is Frequency.EveryNDays -> {
            val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
            daysFromStart >= 0 && daysFromStart % f.n.toLong() == 0L
        }
    }
}
