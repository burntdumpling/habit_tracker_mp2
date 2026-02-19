package com.example.application

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.bottom_sheets.SettingsSheet
import com.example.application.bottom_sheets.CalendarSheet
import com.example.application.habit.Habit

@Composable
fun Home(
    onLogout: () -> Unit,
    onAddHabit: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as MyApp).appContainer.habitsRepository
    val habits by repository.habits.collectAsState()

    var isSettingsVisible by remember { mutableStateOf(false) }
    var isCalendarVisible by remember { mutableStateOf(false) }

    // Локальное хранилище прогресса (заглушка)
    val progressMap = remember { mutableStateMapOf<Long, Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхняя панель
        TopPanel(
            onProfileClick = { isSettingsVisible = true },
            onCalendarClick = { isCalendarVisible = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (habits.isEmpty()) {
            // Пустое состояние
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                AddHabitCard(onClick = onAddHabit)
            }
        } else {
            // Список привычек в новом дизайне
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(habits, key = { it.habitId }) { habit ->
                    val progress = progressMap[habit.habitId] ?: 0
                    val target = computeTarget(habit)
                    
                    NewHabitCard(
                        habit = habit,
                        progress = progress,
                        onIncrement = {
                            if (progress < target) {
                                progressMap[habit.habitId] = progress + 1
                            }
                        },
                        onDelete = { repository.deleteHabit(habit.habitId) }
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (isSettingsVisible) {
        SettingsBottomSheet(onDismiss = { isSettingsVisible = false }, onLogout = onLogout)
    }
    if (isCalendarVisible) {
        CalendarBottomSheet(onDismiss = { isCalendarVisible = false })
    }
}

private fun computeTarget(habit: Habit): Int {
    val fullDayTarget = habit.targets.find { it.dayPart == com.example.application.habit.DayPart.FULL_DAY }
    val targetValue = if (fullDayTarget != null) {
        fullDayTarget.targetCount
    } else {
        habit.targets.sumOf { it.targetCount }
    }
    return maxOf(1, targetValue)
}

@Composable
fun NewHabitCard(
    habit: Habit,
    progress: Int,
    onIncrement: () -> Unit,
    onDelete: () -> Unit
) {
    val target = computeTarget(habit)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Слева: Квадрат с иконкой
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(habit.colorArgb).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter, // Заглушка иконки
                    contentDescription = null,
                    tint = Color(habit.colorArgb),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // По центру: Название и прогресс
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "$progress/$target",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Справа: Крупное число и кнопка +
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = progress.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Surface(
                    onClick = onIncrement,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Увеличить",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Кнопка удаления (оставим маленькой рядом)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(start = 4.dp).size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPanel(
    onProfileClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleIconButton(
            iconRes = R.drawable.settings,
            onClick = onProfileClick
        )

        Text(
            text = "Сегодня",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        CircleIconButton(
            iconRes = R.drawable.calendar,
            onClick = onCalendarClick
        )
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
                    imageVector = Icons.Default.Add,
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
fun CalendarBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        CalendarSheet()
    }
}
