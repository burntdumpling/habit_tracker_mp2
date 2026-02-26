package com.example.application.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.MyApp
import com.example.application.plus.*
import com.example.application.habit.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    habitId: Long,
    onBack: () -> Unit,
    viewModel: CreateHabitViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MyApp).appContainer
    val habitsRepository = appContainer.habitsRepository
    val scope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    
    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }

    val icons = listOf("🏃", "💧", "🛌", "🪥", "🛁", "📚", "🧘", "🍎")
    val selectedEmoji = icons.getOrNull(state.iconId - 1) ?: "❓"

    LaunchedEffect(habitId) {
        val habit = habitsRepository.getHabit(habitId)
        if (habit != null) {
            viewModel.loadHabitForEdit(habit)
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = GrayColor)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = HeaderColor)
                }
                
                Text(
                    text = "Редактировать",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = HeaderColor
                    )
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            habitsRepository.deleteHabit(habitId)
                            onBack()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = GrayColor)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section NAME
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "НАЗВАНИЕ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = GrayColor,
                        unfocusedContainerColor = GrayColor,
                        cursorColor = AccentColor
                    ),
                    shape = ShapeRounded
                )
            }

            // Section ICON AND COLOR
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ИКОНКА И ЦВЕТ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionCard(
                            label = "ИКОНКА",
                            content = { Text(text = selectedEmoji, fontSize = 48.sp) },
                            onClick = { showIconSheet = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionCard(
                            label = "ЦВЕТ",
                            content = {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
                                    drawCircle(color = Color(state.colorArgb))
                                }
                            },
                            onClick = { showColorSheet = true }
                        )
                    }
                }
            }

            // Section GOAL
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ЦЕЛЬ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeRounded,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    PaddingValues(16.dp).let {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.targetCount} раз в день",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderColor
                            )
                        }
                    }
                }
            }

            // Section TRACK DURING
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ВРЕМЯ СУТОК",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                
                TimeOfDayCard(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    title = "ВЕСЬ ДЕНЬ",
                    iconRes = com.example.application.R.drawable.icon_fullday,
                    isSelected = state.dayPart == DayPart.FULL_DAY,
                    onClick = { viewModel.updateDayPart(DayPart.FULL_DAY) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeOfDayCard(
                        modifier = Modifier.weight(1f).height(100.dp),
                        title = "УТРО",
                        iconRes = com.example.application.R.drawable.icon_morning,
                        isSelected = state.dayPart == DayPart.MORNING,
                        onClick = { viewModel.updateDayPart(DayPart.MORNING) }
                    )
                    TimeOfDayCard(
                        modifier = Modifier.weight(1f).height(100.dp),
                        title = "ДЕНЬ",
                        iconRes = com.example.application.R.drawable.icon_day,
                        isSelected = state.dayPart == DayPart.DAY,
                        onClick = { viewModel.updateDayPart(DayPart.DAY) }
                    )
                    TimeOfDayCard(
                        modifier = Modifier.weight(1f).height(100.dp),
                        title = "ВЕЧЕР",
                        iconRes = com.example.application.R.drawable.icon_evening,
                        isSelected = state.dayPart == DayPart.EVENING,
                        onClick = { viewModel.updateDayPart(DayPart.EVENING) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            MainButton(
                text = "СОХРАНИТЬ",
                enabled = state.name.isNotBlank(),
                onClick = {
                    scope.launch {
                        val frequency = when (state.frequencyType) {
                            FrequencyType.DAILY -> Frequency.Daily
                            FrequencyType.WEEKLY -> Frequency.Weekly(state.weekDays.toSet())
                            FrequencyType.EVERY_N_DAYS -> Frequency.EveryNDays(1)
                        }
                        
                        val targets = listOf(HabitTarget(state.dayPart, state.targetCount))
                        
                        val currentHabit = habitsRepository.getHabit(habitId)
                        if (currentHabit != null) {
                            val updatedHabit = currentHabit.copy(
                                name = state.name,
                                iconId = state.iconId,
                                colorArgb = state.colorArgb,
                                frequency = frequency,
                                targets = targets
                            )
                            habitsRepository.updateHabit(updatedHabit)
                        }
                        onBack()
                    }
                }
            )
        }
    }

    if (showIconSheet) {
        IconSelectionSheet(
            icons = icons,
            onIconSelected = { id ->
                viewModel.updateIcon(id)
                showIconSheet = false
            },
            onDismiss = { showIconSheet = false }
        )
    }

    if (showColorSheet) {
        ColorSelectionSheet(
            onColorSelected = { color ->
                viewModel.updateColor(color)
                showColorSheet = false
            },
            onDismiss = { showColorSheet = false }
        )
    }
}
