package com.example.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

private enum class StatsPeriod(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    ALL("Все время")
}

private data class SummaryMetric(
    val title: String,
    val value: String,
    val subtitle: String
)

private data class HabitProgress(
    val name: String,
    val done: Int,
    val target: Int,
    val color: Color
)

private data class StreakMetric(
    val title: String,
    val value: String
)

private data class InsightCard(
    val title: String,
    val value: String,
    val note: String
)

private data class StatisticsUiModel(
    val summary: List<SummaryMetric>,
    val habits: List<HabitProgress>,
    val streaks: List<StreakMetric>,
    val insights: List<InsightCard>
)

@Composable
fun Statistics() {
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }
    val data = remember(selectedPeriod) { mockStatistics(selectedPeriod) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F8FA))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF384358)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Сводка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(data.summary) { metric ->
            SummaryCard(metric)
        }

        item {
            Text(
                text = "Прогресс по привычкам",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(data.habits) { habit ->
            HabitProgressCard(habit)
        }

        item {
            Text(
                text = "Streak и стабильность",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data.streaks.forEach { metric ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(metric.title, color = Color(0xFF6B7280))
                            Text(metric.value, fontWeight = FontWeight.Bold, color = Color(0xFF384358))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Инсайты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(data.insights) { insight ->
            InsightStatCard(insight)
        }
    }
}

@Composable
private fun SummaryCard(metric: SummaryMetric) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(metric.title, color = Color(0xFF6B7280), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(metric.value, fontWeight = FontWeight.Bold, color = Color(0xFF384358), fontSize = 24.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(metric.subtitle, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}

@Composable
private fun HabitProgressCard(habit: HabitProgress) {
    val progress = if (habit.target > 0) habit.done.toFloat() / habit.target.toFloat() else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(habit.name, color = Color(0xFF384358), fontWeight = FontWeight.SemiBold)
                Text("${habit.done}/${habit.target}", color = Color(0xFF6B7280))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(habit.color)
                )
            }
        }
    }
}

@Composable
private fun InsightStatCard(insight: InsightCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(insight.title, color = Color(0xFF6B7280), fontSize = 13.sp)
            Text(insight.value, color = Color(0xFF384358), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(insight.note, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}

private fun mockStatistics(period: StatsPeriod): StatisticsUiModel {
    return when (period) {
        StatsPeriod.WEEK -> StatisticsUiModel(
            summary = listOf(
                SummaryMetric("Выполнено привычек", "29", "за последние 7 дней"),
                SummaryMetric("Completion Rate", "74%", "средний процент выполнения"),
                SummaryMetric("Активные привычки", "6", "в работе сейчас"),
                SummaryMetric("Пропущено", "10", "требуют внимания")
            ),
            habits = listOf(
                HabitProgress("Пить воду", 6, 7, Color(0xFF60A5FA)),
                HabitProgress("Утренняя зарядка", 5, 7, Color(0xFFF59E0B)),
                HabitProgress("Чтение", 4, 7, Color(0xFF34D399)),
                HabitProgress("Сон до 23:00", 3, 7, Color(0xFFFB7185))
            ),
            streaks = listOf(
                StreakMetric("Текущий streak", "8 дней"),
                StreakMetric("Лучший streak", "21 день"),
                StreakMetric("Consistency", "78%")
            ),
            insights = listOf(
                InsightCard("Лучшая привычка", "Пить воду", "6/7 выполнений за неделю"),
                InsightCard("Самая стабильная", "Утренняя зарядка", "выполняется 5 недель подряд"),
                InsightCard("Зона роста", "Сон до 23:00", "лучший день: вторник")
            )
        )

        StatsPeriod.MONTH -> StatisticsUiModel(
            summary = listOf(
                SummaryMetric("Выполнено привычек", "118", "за 30 дней"),
                SummaryMetric("Completion Rate", "68%", "средний процент выполнения"),
                SummaryMetric("Активные привычки", "7", "в работе сейчас"),
                SummaryMetric("Пропущено", "41", "точки для улучшения")
            ),
            habits = listOf(
                HabitProgress("Пить воду", 25, 30, Color(0xFF60A5FA)),
                HabitProgress("Утренняя зарядка", 20, 30, Color(0xFFF59E0B)),
                HabitProgress("Чтение", 18, 30, Color(0xFF34D399)),
                HabitProgress("Сон до 23:00", 15, 30, Color(0xFFFB7185))
            ),
            streaks = listOf(
                StreakMetric("Текущий streak", "8 дней"),
                StreakMetric("Лучший streak", "21 день"),
                StreakMetric("Consistency", "71%")
            ),
            insights = listOf(
                InsightCard("Лучшая привычка", "Пить воду", "самая высокая дисциплина в месяце"),
                InsightCard("Самая стабильная", "Чтение", "без пропусков 3 недели подряд"),
                InsightCard("Зона роста", "Сон до 23:00", "наибольшие просадки по выходным")
            )
        )

        StatsPeriod.ALL -> StatisticsUiModel(
            summary = listOf(
                SummaryMetric("Всего выполнений", "642", "за все время"),
                SummaryMetric("Completion Rate", "72%", "долгосрочный показатель"),
                SummaryMetric("Активные привычки", "9", "текущий пул привычек"),
                SummaryMetric("Лучший месяц", "Февраль", "81% выполнения")
            ),
            habits = listOf(
                HabitProgress("Пить воду", 161, 210, Color(0xFF60A5FA)),
                HabitProgress("Утренняя зарядка", 142, 210, Color(0xFFF59E0B)),
                HabitProgress("Чтение", 133, 210, Color(0xFF34D399)),
                HabitProgress("Сон до 23:00", 117, 210, Color(0xFFFB7185))
            ),
            streaks = listOf(
                StreakMetric("Текущий streak", "8 дней"),
                StreakMetric("Лучший streak", "34 дня"),
                StreakMetric("Consistency", "74%")
            ),
            insights = listOf(
                InsightCard("Лучшая привычка", "Пить воду", "лидер по суммарным выполнениям"),
                InsightCard("Самая стабильная", "Утренняя зарядка", "наименьшая волатильность"),
                InsightCard("Главный инсайт", "Утро продуктивнее", "утренние привычки выполняются чаще")
            )
        )
    }
}
