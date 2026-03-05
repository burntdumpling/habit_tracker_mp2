package com.example.application

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.application.bottom_sheets.SettingsSheet
import com.example.application.bottom_sheets.CalendarSheet
import com.example.application.habit.Habit
import com.example.application.habit.DayPart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale




import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HomeViewModelFactory(private val repository: com.example.application.data.habits.HabitsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    onLogout: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as MyApp).appContainer.habitsRepository
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
    
    val habits by viewModel.habits.collectAsState()
    val mskZoneId = remember { ZoneId.of("Europe/Moscow") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(mskZoneId)) }
    var selectedDayPart by remember { mutableStateOf(DayPart.FULL_DAY) }

    var isSettingsVisible by remember { mutableStateOf(false) }
    var isCalendarVisible by remember { mutableStateOf(false) }

    // Используем progressMap из ViewModel
    val progressMap = viewModel.progressMap

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхняя панель
        TopPanel(
            selectedDate = selectedDate,
            mskZoneId = mskZoneId,
            onProfileClick = { isSettingsVisible = true },
            onCalendarClick = { isCalendarVisible = true }
        )

        // Фильтр по времени суток
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterItems = listOf(
                DayPart.FULL_DAY to R.drawable.icon_fullday,
                DayPart.MORNING to R.drawable.icon_morning,
                DayPart.DAY to R.drawable.icon_day,
                DayPart.EVENING to R.drawable.icon_evening
            )

            filterItems.forEach { (part, iconRes) ->
                val isSelected = selectedDayPart == part
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { selectedDayPart = part },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) Color(0xFF384358) else Color(0xFF384358).copy(alpha = 0.3f)
                    )
                }
            }
        }

        val isCurrentTabEmpty = if (selectedDayPart == DayPart.FULL_DAY) {
            habits.isEmpty()
        } else {
            habits.none { it.targets.firstOrNull()?.dayPart == selectedDayPart }
        }

        if (isCurrentTabEmpty) {
            // Пустое состояние
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                AddHabitCard(onClick = onAddHabit)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (selectedDayPart == DayPart.FULL_DAY) {
                    val allDay = habits.filter { it.targets.firstOrNull()?.dayPart == DayPart.FULL_DAY }
                    val morning = habits.filter { it.targets.firstOrNull()?.dayPart == DayPart.MORNING }
                    val day = habits.filter { it.targets.firstOrNull()?.dayPart == DayPart.DAY }
                    val evening = habits.filter { it.targets.firstOrNull()?.dayPart == DayPart.EVENING }

                    if (allDay.isNotEmpty()) {
                        item { SectionHeader("ВЕСЬ ДЕНЬ") }
                        items(allDay, key = { "allday_${it.habitId}" }) { habit ->
                            HabitSwipeItemWrapper(habit, progressMap, onEditHabit, viewModel, DayPart.FULL_DAY, selectedDate)
                        }
                    }
                    if (morning.isNotEmpty()) {
                        item { SectionHeader("УТРО") }
                        items(morning, key = { "morning_${it.habitId}" }) { habit ->
                            HabitSwipeItemWrapper(habit, progressMap, onEditHabit, viewModel, DayPart.MORNING, selectedDate)
                        }
                    }
                    if (day.isNotEmpty()) {
                        item { SectionHeader("ДЕНЬ") }
                        items(day, key = { "day_${it.habitId}" }) { habit ->
                            HabitSwipeItemWrapper(habit, progressMap, onEditHabit, viewModel, DayPart.DAY, selectedDate)
                        }
                    }
                    if (evening.isNotEmpty()) {
                        item { SectionHeader("ВЕЧЕР") }
                        items(evening, key = { "evening_${it.habitId}" }) { habit ->
                            HabitSwipeItemWrapper(habit, progressMap, onEditHabit, viewModel, DayPart.EVENING, selectedDate)
                        }
                    }
                } else {
                    val filtered = habits.filter { it.targets.firstOrNull()?.dayPart == selectedDayPart }
                    items(filtered, key = { "filtered_${it.habitId}" }) { habit ->
                        HabitSwipeItemWrapper(habit, progressMap, onEditHabit, viewModel, selectedDayPart, selectedDate)
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (isSettingsVisible) {
        SettingsBottomSheet(onDismiss = { isSettingsVisible = false }, onLogout = onLogout)
    }
    if (isCalendarVisible) {
        CalendarBottomSheet(
            selectedDate = selectedDate,
            onDismiss = { isCalendarVisible = false },
            onDateSelected = { selectedDate = it }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color(0xFF384358).copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun HabitSwipeItemWrapper(
    habit: Habit,
    progressMap: Map<Long, Int>,
    onEditHabit: (Long) -> Unit,
    viewModel: HomeViewModel,
    dayPart: DayPart,
    date: LocalDate
) {
    val progress = progressMap[habit.habitId] ?: 0
    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe")

    Box(modifier = Modifier.fillMaxWidth()) {
        // Подложка с кнопкой редактирования (Задний план)
        Surface(
            onClick = {
                offsetX = 0f // Возвращаем карточку на место перед переходом
                onEditHabit(habit.habitId)
            },
            modifier = Modifier
                .matchParentSize() // Точно повторяет размер HabitCard
                .padding(bottom = 8.dp), // Учитываем отступ HabitCard
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE0E0E0) // Серый цвет, как на макете
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = Color.DarkGray,
                    modifier = Modifier.padding(end = 28.dp)
                )
            }
        }

        // Сама карточка (Передний план, который двигается)
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // При отпускании пальца: либо защелкиваем в открытом виде, либо закрываем
                            offsetX = if (offsetX < -actionWidthPx / 2) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Ограничиваем свайп только влево на ширину кнопки
                            offsetX = (offsetX + dragAmount).coerceIn(-actionWidthPx, 0f)
                        }
                    )
                }
        ) {
            HabitCard(
                habit = habit,
                currentCount = progress,
                onPlusClick = {
                    val target = computeTarget(habit)
                    if (progress < target) {
                        viewModel.incrementProgress(habit, dayPart, date)
                    }
                }
            )
        }
    }
}

private fun computeTarget(habit: Habit): Int {
    val fullDayTarget = habit.targets.find { it.dayPart == DayPart.FULL_DAY }
    val targetValue = if (fullDayTarget != null) {
        fullDayTarget.targetCount
    } else {
        habit.targets.sumOf { it.targetCount }
    }
    return maxOf(1, targetValue)
}

@Composable
fun HabitCard(
    habit: Habit,
    currentCount: Int,
    onPlusClick: () -> Unit
) {
    val icons = listOf("🏃", "💧", "🛌", "🪥", "🛁", "📚", "🧘", "🍎")
    val selectedEmoji = icons.getOrNull(habit.iconId - 1) ?: "❓"
    val targetCount = computeTarget(habit)
    val isCompleted = currentCount >= targetCount
    val habitColor = Color(habit.colorArgb)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isCompleted) habitColor else Color.White,
        contentColor = if (isCompleted) Color.White else Color(0xFF384358),
        shadowElevation = if (isCompleted) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Слева: Круг с эмодзи
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = (if (isCompleted) Color.White else habitColor).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = selectedEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // По центру: Название и прогресс
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$currentCount / $targetCount",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                    color = (if (isCompleted) Color.White else Color.Gray).copy(alpha = 0.7f)
                )
            }

            // Справа: Кнопка +
            Surface(
                onClick = onPlusClick,
                shape = CircleShape,
                color = (if (isCompleted) Color.White else habitColor).copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Увеличить",
                        tint = if (isCompleted) habitColor else habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPanel(
    selectedDate: LocalDate,
    mskZoneId: ZoneId,
    onProfileClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    val todayMsk = remember { LocalDate.now(mskZoneId) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM", Locale("ru", "RU")) }

    val title = when (selectedDate) {
        todayMsk -> "Сегодня"
        todayMsk.plusDays(1) -> "Завтра"
        todayMsk.minusDays(1) -> "Вчера"
        else -> selectedDate.format(formatter)
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleIconButton(iconRes = R.drawable.settings, onClick = onProfileClick)

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        CircleIconButton(iconRes = R.drawable.calendar, onClick = onCalendarClick)
    }
}


@Composable
fun CircleIconButton(
    iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(48.dp),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
fun AddHabitCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Новая привычка",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit, onLogout: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        SettingsSheet(onLogout = onLogout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarBottomSheet(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        CalendarSheet(
            initialDate = selectedDate,
            onDateSelected = onDateSelected,
            onClose = onDismiss
        )
    }
}


