package com.example.application

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.application.data.statistics.StatisticsComputedData
import com.example.application.data.statistics.StatisticsDayPoint
import com.example.application.data.statistics.StatisticsHabitMetrics
import com.example.application.data.statistics.StatisticsPeriod
import com.example.application.statistics.StatisticsUiState
import com.example.application.statistics.StatisticsViewModel
import com.example.application.statistics.StatisticsViewModelFactory
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private data class SummaryMetric(
    val title: String,
    val value: String,
    val subtitle: String
)

private data class InsightCard(
    val title: String,
    val value: String,
    val note: String
)

private val ruLocale: Locale = Locale.forLanguageTag("ru-RU")
private val compactDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", ruLocale)
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", ruLocale)
private val ScreenBackground = Color(0xFFF3F6FB)
private val AppCardShape = RoundedCornerShape(20.dp)
private val HeroBrush = Brush.linearGradient(
    listOf(
        Color(0xFF2C3E66),
        Color(0xFF506B9E),
        Color(0xFF6B8BC2)
    )
)
private val AccentColor = Color(0xFF4F8CFF)

@Composable
fun Statistics() {
    val context = LocalContext.current
    val repository = (context.applicationContext as MyApp).appContainer.statisticsRepository
    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModelFactory(repository = repository)
    )
    val uiState by statisticsViewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.data == null -> StatisticsLoadingState()
        uiState.errorMessage != null && uiState.data == null -> {
            StatisticsErrorState(
                message = uiState.errorMessage ?: "\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043A\u0443",
                onRetry = statisticsViewModel::retry
            )
        }
        uiState.data != null -> {
            StatisticsContent(
                uiState = uiState,
                onPeriodChange = statisticsViewModel::changePeriod,
                onRetry = statisticsViewModel::retry
            )
        }
        else -> StatisticsLoadingState()
    }
}

@Composable
private fun StatisticsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(modifier = Modifier.width(180.dp))
            Text(
                text = "\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430 \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043A\u0438...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatisticsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                color = Color(0xFFB91C1C),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRetry) {
                Text("\u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C")
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    onPeriodChange: (StatisticsPeriod) -> Unit,
    onRetry: () -> Unit
) {
    val data = uiState.data ?: return
    val summaryMetrics = buildSummaryMetrics(data)
    val insightCards = buildInsightCards(data)
    val hasNoPeriodData = data.overview.totalPlanned == 0 &&
        data.overview.totalCompleted == 0 &&
        data.habits.isEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StatisticsHeader(
                uiState = uiState,
                data = data,
                onPeriodChange = onPeriodChange
            )
        }

        if (uiState.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    trackColor = Color(0xFFD9E1F0)
                )
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F4)),
                    shape = AppCardShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = Color(0xFFB91C1C),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onRetry) {
                            Text("\u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C")
                        }
                    }
                }
            }
        }

        item { HeroOverviewCard(data = data) }

        item { SectionTitle("\u0414\u0438\u043D\u0430\u043C\u0438\u043A\u0430 \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u044F") }
        item { CompletionTrendChartCard(points = data.timeline) }

        item { SectionTitle("\u0420\u0430\u0441\u043F\u0440\u0435\u0434\u0435\u043B\u0435\u043D\u0438\u0435 \u0430\u043A\u0442\u0438\u0432\u043D\u043E\u0441\u0442\u0438") }
        item { HabitDistributionChartCard(habits = data.habits) }

        if (hasNoPeriodData) {
            item {
                EmptyInfoCard(
                    title = "\u041F\u043E\u043A\u0430 \u043D\u0435\u0442 \u0434\u0430\u043D\u043D\u044B\u0445 \u0437\u0430 \u043F\u0435\u0440\u0438\u043E\u0434",
                    note = "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0440\u0443\u0433\u043E\u0439 \u043F\u0435\u0440\u0438\u043E\u0434 \u0438\u043B\u0438 \u043D\u0430\u0447\u043D\u0438\u0442\u0435 \u043E\u0442\u043C\u0435\u0447\u0430\u0442\u044C \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u0435 \u043F\u0440\u0438\u0432\u044B\u0447\u0435\u043A."
                )
            }
        }

        item { SectionTitle("\u0421\u0432\u043E\u0434\u043A\u0430") }
        item { SummaryDashboard(metrics = summaryMetrics) }

        item { SectionTitle("\u041F\u0440\u043E\u0433\u0440\u0435\u0441\u0441 \u043F\u043E \u043F\u0440\u0438\u0432\u044B\u0447\u043A\u0430\u043C") }

        if (data.habits.isEmpty()) {
            item {
                EmptyInfoCard(
                    title = "\u041D\u0435\u0442 \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0445 \u043F\u0440\u0438\u0432\u044B\u0447\u0435\u043A",
                    note = "\u0412 \u044D\u0442\u043E\u043C \u043F\u0435\u0440\u0438\u043E\u0434\u0435 \u043D\u0435 \u0431\u044B\u043B\u043E \u043F\u0440\u0438\u0432\u044B\u0447\u0435\u043A \u0434\u043B\u044F \u0440\u0430\u0441\u0447\u0435\u0442\u0430 \u043F\u0440\u043E\u0433\u0440\u0435\u0441\u0441\u0430."
                )
            }
        } else {
            items(data.habits, key = { it.habitId }) { habit ->
                HabitProgressCard(
                    name = habit.name,
                    done = habit.doneTotal,
                    target = habit.targetTotal,
                    rate = habit.completionRatePercent,
                    colorArgb = habit.colorArgb
                )
            }
        }

        item { SectionTitle("\u0421\u0435\u0440\u0438\u0438 \u0438 \u0441\u0442\u0430\u0431\u0438\u043B\u044C\u043D\u043E\u0441\u0442\u044C") }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = AppCardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StreakRow(title = "\u0422\u0435\u043A\u0443\u0449\u0430\u044F \u0441\u0435\u0440\u0438\u044F", value = formatDaysLabel(data.streaks.currentStreakDays))
                    StreakRow(title = "\u041B\u0443\u0447\u0448\u0430\u044F \u0441\u0435\u0440\u0438\u044F", value = formatDaysLabel(data.streaks.bestStreakDays))
                    StreakRow(title = "\u0421\u0442\u0430\u0431\u0438\u043B\u044C\u043D\u043E\u0441\u0442\u044C", value = formatPercent(data.streaks.consistencyPercent))
                }
            }
        }

        item { SectionTitle("\u0418\u043D\u0441\u0430\u0439\u0442\u044B") }
        items(insightCards) { insight -> InsightStatCard(insight) }
    }
}

@Composable
private fun StatisticsHeader(
    uiState: StatisticsUiState,
    data: StatisticsComputedData,
    onPeriodChange: (StatisticsPeriod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043A\u0430",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatDateRange(data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticsPeriod.entries.forEach { period ->
                FilterChip(
                    selected = uiState.selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.label()) }
                )
            }
        }
    }
}

@Composable
private fun HeroOverviewCard(data: StatisticsComputedData) {
    val overview = data.overview
    val progress = overview.completionRatePercent.coerceIn(0, 100) / 100f

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroBrush)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "\u041E\u0431\u0449\u0438\u0439 \u0440\u0435\u0437\u0443\u043B\u044C\u0442\u0430\u0442",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = formatPercent(overview.completionRatePercent),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${formatCount(overview.totalCompleted)} / ${formatCount(overview.totalPlanned)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.24f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroStatPill(
                    title = "\u0412\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u043E",
                    value = formatCount(overview.totalCompleted),
                    modifier = Modifier.weight(1f)
                )
                HeroStatPill(
                    title = "\u0410\u043A\u0442\u0438\u0432\u043D\u044B\u0435",
                    value = formatCount(overview.activeHabitsCount),
                    modifier = Modifier.weight(1f)
                )
                HeroStatPill(
                    title = "\u041F\u0440\u043E\u043F\u0443\u0449\u0435\u043D\u043E",
                    value = formatCount(overview.missedCount),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.17f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun CompletionTrendChartCard(points: List<StatisticsDayPoint>) {
    val planColor = Color(0xFFA8B4CA)
    val factColor = AccentColor

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "\u0412\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u0435 \u043F\u043E \u0434\u043D\u044F\u043C",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "\u041F\u043B\u0430\u043D \u0438 \u0444\u0430\u043A\u0442 \u0437\u0430 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0439 \u043F\u0435\u0440\u0438\u043E\u0434",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (points.isEmpty()) {
                Text(
                    text = "\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0434\u0430\u043D\u043D\u044B\u0445 \u0434\u043B\u044F \u0433\u0440\u0430\u0444\u0438\u043A\u0430",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChartLegendItem(
                    color = factColor,
                    label = "\u0424\u0430\u043A\u0442",
                    asLine = false
                )
                ChartLegendItem(
                    color = planColor,
                    label = "\u041F\u043B\u0430\u043D",
                    asLine = true
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val maxY = max(1, points.maxOf { max(it.plannedCount, it.completedCount) })
                val chartWidth = size.width
                val chartHeight = size.height
                val slotWidth = chartWidth / points.size
                val barWidth = (slotWidth * 0.5f).coerceAtLeast(8f)

                repeat(4) { index ->
                    val y = chartHeight * (index / 3f)
                    drawLine(
                        color = Color(0xFFE8EDF5),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(chartWidth, y),
                        strokeWidth = 1f
                    )
                }

                points.forEachIndexed { index, point ->
                    val centerX = (index * slotWidth) + slotWidth / 2f
                    val completedHeight = (point.completedCount.toFloat() / maxY.toFloat()) * chartHeight
                    val plannedY = chartHeight - (point.plannedCount.toFloat() / maxY.toFloat()) * chartHeight

                    drawRoundRect(
                        color = factColor,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = centerX - (barWidth / 2f),
                            y = chartHeight - completedHeight
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            width = barWidth,
                            height = completedHeight
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    drawLine(
                        color = planColor,
                        start = androidx.compose.ui.geometry.Offset(
                            x = centerX - (barWidth * 0.7f),
                            y = plannedY
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            x = centerX + (barWidth * 0.7f),
                            y = plannedY
                        ),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val first = points.firstOrNull()?.date
                val middle = points.getOrNull(points.size / 2)?.date
                val last = points.lastOrNull()?.date

                Text(
                    text = first?.format(shortDateFormatter).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = middle?.format(shortDateFormatter).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = last?.format(shortDateFormatter).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    asLine: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (asLine) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HabitDistributionChartCard(habits: List<StatisticsHabitMetrics>) {
    val topHabits = habits
        .filter { it.plannedDays > 0 }
        .sortedByDescending { it.doneTotal }
        .take(5)
    val totalDone = topHabits.sumOf { it.doneTotal.coerceAtLeast(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "\u0412\u043A\u043B\u0430\u0434 \u043F\u0440\u0438\u0432\u044B\u0447\u0435\u043A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "\u0420\u0430\u0441\u043F\u0440\u0435\u0434\u0435\u043B\u0435\u043D\u0438\u0435 \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u0439 \u043C\u0435\u0436\u0434\u0443 \u043F\u0440\u0438\u0432\u044B\u0447\u043A\u0430\u043C\u0438",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (topHabits.isEmpty() || totalDone <= 0) {
                Text(
                    text = "\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0434\u0430\u043D\u043D\u044B\u0445 \u0434\u043B\u044F \u0440\u0430\u0441\u043F\u0440\u0435\u0434\u0435\u043B\u0435\u043D\u0438\u044F",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 360.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DistributionDonutChart(topHabits = topHabits, totalDone = totalDone)
                        DistributionLegend(topHabits = topHabits, totalDone = totalDone)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DistributionDonutChart(
                            topHabits = topHabits,
                            totalDone = totalDone,
                            modifier = Modifier.weight(1f)
                        )
                        DistributionLegend(
                            topHabits = topHabits,
                            totalDone = totalDone,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionDonutChart(
    topHabits: List<StatisticsHabitMetrics>,
    totalDone: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(170.dp)) {
            var start = -90f
            topHabits.forEach { habit ->
                val sweep = 360f * (habit.doneTotal.toFloat() / totalDone.toFloat())
                drawArc(
                    color = safeHabitColor(habit.colorArgb),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 34f, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatCount(totalDone),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "\u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u0439",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DistributionLegend(
    topHabits: List<StatisticsHabitMetrics>,
    totalDone: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topHabits.forEach { habit ->
            val share = if (totalDone > 0) {
                ((habit.doneTotal * 100f) / totalDone).toInt().coerceIn(0, 100)
            } else {
                0
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(safeHabitColor(habit.colorArgb))
                )
                Text(
                    text = habit.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${share}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryDashboard(metrics: List<SummaryMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { metric ->
                    Box(modifier = Modifier.weight(1f)) {
                        SummaryCard(metric = metric)
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StreakRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
@Composable
private fun EmptyInfoCard(
    title: String,
    note: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SummaryCard(metric: SummaryMetric) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = metric.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = metric.value,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = metric.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HabitProgressCard(
    name: String,
    done: Int,
    target: Int,
    rate: Int,
    colorArgb: Int
) {
    val safeTarget = target.coerceAtLeast(0)
    val safeDone = done.coerceAtLeast(0)
    val progress = if (safeTarget > 0) safeDone.toFloat() / safeTarget.toFloat() else 0f
    val progressText = if (safeTarget > 0) {
        "${formatCount(safeDone)} / ${formatCount(safeTarget)}"
    } else {
        "${formatCount(safeDone)} / \u2014"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "\u0412\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u0435 ${formatPercent(rate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = progressText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE5EBF5))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(safeHabitColor(colorArgb))
                )
            }
        }
    }
}

@Composable
private fun InsightStatCard(insight: InsightCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = insight.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = insight.value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = insight.note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildSummaryMetrics(data: StatisticsComputedData): List<SummaryMetric> {
    val overview = data.overview
    return listOf(
        SummaryMetric(
            title = "\u0412\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u043E",
            value = formatCount(overview.totalCompleted),
            subtitle = if (overview.totalPlanned > 0) {
                "\u0438\u0437 ${formatCount(overview.totalPlanned)} \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0445"
            } else {
                "\u0417\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0445 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439 \u043D\u0435\u0442"
            }
        ),
        SummaryMetric(
            title = "\u041F\u0440\u043E\u0446\u0435\u043D\u0442 \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u044F",
            value = formatPercent(overview.completionRatePercent),
            subtitle = "\u0434\u043E\u043B\u044F \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u043D\u044B\u0445 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439"
        ),
        SummaryMetric(
            title = "\u0410\u043A\u0442\u0438\u0432\u043D\u044B\u0435 \u043F\u0440\u0438\u0432\u044B\u0447\u043A\u0438",
            value = formatCount(overview.activeHabitsCount),
            subtitle = "\u0431\u044B\u043B\u0438 \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u044B \u0432 \u043F\u0435\u0440\u0438\u043E\u0434\u0435"
        ),
        SummaryMetric(
            title = "\u041F\u0440\u043E\u043F\u0443\u0449\u0435\u043D\u043E",
            value = formatCount(overview.missedCount),
            subtitle = if (overview.missedCount > 0) {
                "\u043D\u0435\u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u043D\u044B\u0445 \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0445"
            } else {
                "\u041F\u0440\u043E\u043F\u0443\u0441\u043A\u043E\u0432 \u043D\u0435\u0442"
            }
        )
    )
}

private fun buildInsightCards(data: StatisticsComputedData): List<InsightCard> {
    fun habitNameById(id: Long?): String = insightHabitName(id, data)

    return listOf(
        InsightCard(
            title = "\u041B\u0443\u0447\u0448\u0430\u044F \u043F\u0440\u0438\u0432\u044B\u0447\u043A\u0430",
            value = habitNameById(data.insights.bestHabitId),
            note = if (data.insights.bestHabitId == null) "\u041F\u043E\u043A\u0430 \u043D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0434\u0430\u043D\u043D\u044B\u0445" else "\u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u044B\u0439 \u043F\u0440\u043E\u0446\u0435\u043D\u0442 \u0432\u044B\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u044F"
        ),
        InsightCard(
            title = "\u0421\u0430\u043C\u0430\u044F \u0441\u0442\u0430\u0431\u0438\u043B\u044C\u043D\u0430\u044F",
            value = habitNameById(data.insights.mostStableHabitId),
            note = if (data.insights.mostStableHabitId == null) "\u041F\u043E\u043A\u0430 \u043D\u0435\u0442 \u0443\u0441\u0442\u043E\u0439\u0447\u0438\u0432\u043E\u0433\u043E \u043B\u0438\u0434\u0435\u0440\u0430" else "\u0411\u043E\u043B\u044C\u0448\u0435 \u0432\u0441\u0435\u0433\u043E \u0443\u0441\u043F\u0435\u0448\u043D\u043E \u0437\u0430\u043A\u0440\u044B\u0442\u044B\u0445 \u0434\u043D\u0435\u0439"
        ),
        InsightCard(
            title = "\u0417\u043E\u043D\u0430 \u0440\u043E\u0441\u0442\u0430",
            value = habitNameById(data.insights.growthHabitId),
            note = if (data.insights.growthHabitId == null) "\u0420\u043E\u0441\u0442 \u0432 \u043F\u0435\u0440\u0438\u043E\u0434\u0435 \u043F\u043E\u043A\u0430 \u043D\u0435 \u0432\u044B\u044F\u0432\u043B\u0435\u043D" else "\u041B\u0443\u0447\u0448\u0438\u0439 \u043F\u0440\u043E\u0433\u0440\u0435\u0441\u0441 \u0432\u043E \u0432\u0442\u043E\u0440\u043E\u0439 \u043F\u043E\u043B\u043E\u0432\u0438\u043D\u0435 \u043F\u0435\u0440\u0438\u043E\u0434\u0430"
        )
    )
}

private fun StatisticsPeriod.label(): String {
    return when (this) {
        StatisticsPeriod.WEEK -> "\u041D\u0435\u0434\u0435\u043B\u044F"
        StatisticsPeriod.MONTH -> "\u041C\u0435\u0441\u044F\u0446"
        StatisticsPeriod.ALL -> "\u0412\u0441\u0435 \u0432\u0440\u0435\u043C\u044F"
    }
}

private fun formatCount(value: Int): String {
    return NumberFormat.getIntegerInstance(ruLocale).format(value.coerceAtLeast(0))
}

private fun formatPercent(value: Int): String {
    return "${value.coerceIn(0, 100)}%"
}

private fun formatDaysLabel(days: Int): String {
    val value = days.coerceAtLeast(0)
    return "$value ${daysWord(value)}"
}

private fun daysWord(value: Int): String {
    val n = value % 100
    return when {
        n in 11..14 -> "\u0434\u043D\u0435\u0439"
        value % 10 == 1 -> "\u0434\u0435\u043D\u044C"
        value % 10 in 2..4 -> "\u0434\u043D\u044F"
        else -> "\u0434\u043D\u0435\u0439"
    }
}

private fun formatDateRange(data: StatisticsComputedData): String {
    val from = data.dateFrom.format(compactDateFormatter)
    val to = data.dateTo.format(compactDateFormatter)
    return if (data.dateFrom == data.dateTo) {
        "\u041F\u0435\u0440\u0438\u043E\u0434: $from"
    } else {
        "\u041F\u0435\u0440\u0438\u043E\u0434: $from - $to"
    }
}

private fun insightHabitName(id: Long?, data: StatisticsComputedData): String {
    if (id == null) return "\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0434\u0430\u043D\u043D\u044B\u0445"
    return data.habits.find { it.habitId == id }?.name ?: "\u041F\u043E\u043A\u0430 \u043D\u0435\u0442 \u0434\u0430\u043D\u043D\u044B\u0445"
}

private fun safeHabitColor(colorArgb: Int): Color {
    val color = Color(colorArgb)
    return if (color.alpha <= 0.05f) AccentColor else color
}
