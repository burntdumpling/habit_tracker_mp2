package com.example.application.plus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.application.R
import com.example.application.MyApp
import com.example.application.habit.*
import java.time.LocalDate
import java.time.DayOfWeek
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

// Design System Constants
val BgColor = Color(0xFFF7F8FA)
val HeaderColor = Color(0xFF384358)
val AccentColor = Color(0xFFEAA065)
val GrayColor = Color(0xFFF0F1F5)
val ShapeRounded = RoundedCornerShape(16.dp)
val ShapeButton = CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Plus(
    habitIdToEdit: Long? = null,
    onHabitCreated: () -> Unit = {},
    viewModel: CreateHabitViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MyApp).appContainer
    val habitsRepository = appContainer.habitsRepository
    val scope = rememberCoroutineScope()

    LaunchedEffect(habitIdToEdit) {
        if (habitIdToEdit != null) {
            val habit = habitsRepository.getHabit(habitIdToEdit)
            if (habit != null) {
                viewModel.loadHabitForEdit(habit)
            }
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            WizardTopBar(
                currentStep = state.currentStep,
                onBack = {
                    if (state.currentStep == 1) onHabitCreated() else viewModel.prevStep()
                },
                onClose = onHabitCreated
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (state.currentStep) {
                1 -> Step1(onNavigateNext = viewModel::nextStep)
                2 -> Step2(
                    name = state.name,
                    onNameChange = viewModel::updateName,
                    onNavigateNext = viewModel::nextStep
                )
                3 -> Step3(
                    state = state,
                    onIconChange = viewModel::updateIcon,
                    onColorChange = viewModel::updateColor,
                    onNavigateNext = viewModel::nextStep
                )
                4 -> Step4(
                    selectedType = state.frequencyType,
                    selectedDays = state.weekDays,
                    onTypeChange = viewModel::updateFrequencyType,
                    onDaysChange = viewModel::updateWeekDays,
                    onNavigateNext = viewModel::nextStep
                )
                5 -> Step5(
                    targetCount = state.targetCount,
                    onTargetChange = viewModel::updateTargetCount,
                    onNavigateNext = viewModel::nextStep
                )
                6 -> Step6(
                    selectedPart = state.dayPart,
                    onPartChange = viewModel::updateDayPart,
                    onNavigateNext = viewModel::nextStep
                )
                7 -> Step7(
                    state = state,
                    onAddHabit = {
                        scope.launch {
                            val frequency = when (state.frequencyType) {
                                FrequencyType.DAILY -> Frequency.Daily
                                FrequencyType.WEEKLY -> Frequency.Weekly(state.weekDays.toSet())
                                FrequencyType.EVERY_N_DAYS -> Frequency.EveryNDays(1)
                            }
                            
                            val targets = listOf(HabitTarget(state.dayPart, state.targetCount))
                            val editId = state.editingHabitId

                            if (editId != null) {
                                val currentHabit = habitsRepository.getHabit(editId)
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
                            } else {
                                val request = CreateHabitRequest(
                                    name = state.name,
                                    iconId = state.iconId,
                                    colorArgb = state.colorArgb,
                                    startDate = LocalDate.now(),
                                    endDate = null,
                                    frequency = frequency,
                                    targets = targets
                                )
                                habitsRepository.addHabit(request)
                            }
                            
                            onHabitCreated()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WizardTopBar(
    currentStep: Int,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentStep > 1) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = GrayColor)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = HeaderColor)
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        LinearProgressIndicator(
            progress = { currentStep / 7f },
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
            color = AccentColor,
            trackColor = Color(0xFFEBECEF),
            strokeCap = StrokeCap.Round
        )

        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(containerColor = GrayColor)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = HeaderColor)
        }
    }
}

@Composable
fun Step1(onNavigateNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Новая привычка",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onNavigateNext() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = GrayColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = HeaderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Создать свою привычку",
                    color = HeaderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Популярные привычки",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = HeaderColor
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PopularHabitCard("Спорт", "🏃", Color(0xFFFFE0E0))
            PopularHabitCard("Пить воду", "💧", Color(0xFFE0F0FF))
            PopularHabitCard("Заправить постель", "🛌", Color(0xFFE0E0FF))
        }
    }
}

@Composable
fun PopularHabitCard(name: String, emoji: String, iconBackgroundColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = ShapeRounded
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconBackgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = HeaderColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun Step2(
    name: String,
    onNameChange: (String) -> Unit,
    onNavigateNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Как назовем новую привычку?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Напр. Пить воду", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = GrayColor,
                unfocusedContainerColor = GrayColor,
                cursorColor = AccentColor
            ),
            shape = ShapeRounded
        )

        Spacer(modifier = Modifier.weight(1f))

        MainButton(
            text = "ПРОДОЛЖИТЬ",
            enabled = name.isNotBlank(),
            onClick = onNavigateNext
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3(
    state: CreateHabitDraftState,
    onIconChange: (Int) -> Unit,
    onColorChange: (Int) -> Unit,
    onNavigateNext: () -> Unit
) {
    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }

    val icons = listOf("🏃", "💧", "🛌", "🪥", "🛁", "📚", "🧘", "🍎")
    val selectedEmoji = icons.getOrNull(state.iconId - 1) ?: "❓"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Выбери иконку и цвет для '${state.name}'",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectionCard(
                label = "ИКОНКА",
                content = { Text(text = selectedEmoji, fontSize = 48.sp) },
                onClick = { showIconSheet = true }
            )

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

        Spacer(modifier = Modifier.weight(1f))

        MainButton(text = "ПРОДОЛЖИТЬ", onClick = onNavigateNext)
    }

    if (showIconSheet) {
        IconSelectionSheet(
            icons = icons,
            onIconSelected = { id ->
                onIconChange(id)
                showIconSheet = false
            },
            onDismiss = { showIconSheet = false }
        )
    }

    if (showColorSheet) {
        ColorSelectionSheet(
            onColorSelected = { color ->
                onColorChange(color)
                showColorSheet = false
            },
            onDismiss = { showColorSheet = false }
        )
    }
}

@Composable
fun SelectionCard(label: String, content: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = ShapeRounded
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step4(
    selectedType: FrequencyType,
    selectedDays: List<DayOfWeek>,
    onTypeChange: (FrequencyType) -> Unit,
    onDaysChange: (List<DayOfWeek>) -> Unit,
    onNavigateNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Как часто ты хочешь выполнять эту привычку?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrequencyButtonStyled(
                text = "ЕЖЕДНЕВНО",
                isSelected = selectedType == FrequencyType.DAILY,
                onClick = { onTypeChange(FrequencyType.DAILY) }
            )
            FrequencyButtonStyled(
                text = "ЕЖЕНЕДЕЛЬНО",
                isSelected = selectedType == FrequencyType.WEEKLY,
                onClick = { onTypeChange(FrequencyType.WEEKLY) }
            )
            FrequencyButtonStyled(
                text = "КАЖДЫЕ N ДНЕЙ",
                isSelected = selectedType == FrequencyType.EVERY_N_DAYS,
                onClick = { onTypeChange(FrequencyType.EVERY_N_DAYS) }
            )
        }

        if (selectedType == FrequencyType.WEEKLY) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "ПО ЭТИМ ДНЯМ",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf(
                    "П" to DayOfWeek.MONDAY,
                    "В" to DayOfWeek.TUESDAY,
                    "С" to DayOfWeek.WEDNESDAY,
                    "Ч" to DayOfWeek.THURSDAY,
                    "П" to DayOfWeek.FRIDAY,
                    "С" to DayOfWeek.SATURDAY,
                    "В" to DayOfWeek.SUNDAY
                )
                
                days.forEach { (label, day) ->
                    val isDaySelected = selectedDays.contains(day)
                    DayToggleButtonStyled(
                        label = label,
                        isSelected = isDaySelected,
                        onClick = {
                            val newList = if (isDaySelected) {
                                selectedDays.filter { it != day }
                            } else {
                                selectedDays + day
                            }
                            onDaysChange(newList)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        MainButton(text = "ПРОДОЛЖИТЬ", onClick = onNavigateNext)
    }
}

@Composable
fun FrequencyButtonStyled(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        color = if (isSelected) AccentColor else Color.White,
        contentColor = if (isSelected) Color.White else HeaderColor,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun DayToggleButtonStyled(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = if (isSelected) AccentColor else Color.White,
        contentColor = if (isSelected) Color.White else HeaderColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step5(
    targetCount: Int,
    onTargetChange: (Int) -> Unit,
    onNavigateNext: () -> Unit
) {
    var textValue by remember { mutableStateOf(targetCount.toString()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Какая у тебя цель на день?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = textValue,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    textValue = it
                    it.toIntOrNull()?.let { count -> onTargetChange(count) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = ShapeRounded,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = GrayColor,
                unfocusedContainerColor = GrayColor,
                cursorColor = AccentColor
            )
        )
        
        Text(
            text = "РАЗ В ДЕНЬ ИЛИ ЧАЩЕ",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        MainButton(
            text = "ПРОДОЛЖИТЬ",
            enabled = (textValue.toIntOrNull() ?: 0) > 0,
            onClick = onNavigateNext
        )
    }
}

@Composable
fun Step6(
    selectedPart: DayPart,
    onPartChange: (DayPart) -> Unit,
    onNavigateNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "В какое время суток нужно выполнять привычку?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        TimeOfDayCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            title = "ВЕСЬ ДЕНЬ",
            iconRes = R.drawable.icon_fullday,
            isSelected = selectedPart == DayPart.FULL_DAY,
            onClick = { onPartChange(DayPart.FULL_DAY) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeOfDayCard(
                modifier = Modifier.weight(1f).height(100.dp),
                title = "УТРО",
                iconRes = R.drawable.icon_morning,
                isSelected = selectedPart == DayPart.MORNING,
                onClick = { onPartChange(DayPart.MORNING) }
            )
            TimeOfDayCard(
                modifier = Modifier.weight(1f).height(100.dp),
                title = "ДЕНЬ",
                iconRes = R.drawable.icon_day,
                isSelected = selectedPart == DayPart.DAY,
                onClick = { onPartChange(DayPart.DAY) }
            )
            TimeOfDayCard(
                modifier = Modifier.weight(1f).height(100.dp),
                title = "ВЕЧЕР",
                iconRes = R.drawable.icon_evening,
                isSelected = selectedPart == DayPart.EVENING,
                onClick = { onPartChange(DayPart.EVENING) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        MainButton(text = "ПРОДОЛЖИТЬ", onClick = onNavigateNext)
    }
}

@Composable
fun TimeOfDayCard(
    modifier: Modifier,
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White else GrayColor,
        contentColor = if (isSelected) HeaderColor else Color.Gray,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step7(
    state: CreateHabitDraftState,
    onAddHabit: () -> Unit
) {
    val icons = listOf("🏃", "💧", "🛌", "🪥", "🛁", "📚", "🧘", "🍎")
    val selectedEmoji = icons.getOrNull(state.iconId - 1) ?: "❓"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Всё готово! Проверим?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = HeaderColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = ShapeRounded
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedEmoji, fontSize = 40.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = state.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HeaderColor)
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(28.dp)) {
                        drawCircle(color = Color(state.colorArgb))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = GrayColor)
                
                SummaryItemStyled("Частота", when(state.frequencyType) {
                    FrequencyType.DAILY -> "Ежедневно"
                    FrequencyType.WEEKLY -> "Еженедельно (${state.weekDays.size} дн.)"
                    FrequencyType.EVERY_N_DAYS -> "Раз в несколько дней"
                })
                
                SummaryItemStyled("Цель", "${state.targetCount} раз(а)")
                
                SummaryItemStyled("Время суток", when(state.dayPart) {
                    DayPart.FULL_DAY -> "Весь день"
                    DayPart.MORNING -> "Утро"
                    DayPart.DAY -> "День"
                    DayPart.EVENING -> "Вечер"
                })
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        MainButton(
            text = if (state.editingHabitId != null) "СОХРАНИТЬ ИЗМЕНЕНИЯ" else "ДОБАВИТЬ ПРИВЫЧКУ",
            onClick = onAddHabit
        )
    }
}

@Composable
fun SummaryItemStyled(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Bold, color = HeaderColor)
    }
}

@Composable
fun MainButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentColor,
            disabledContainerColor = AccentColor.copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        shape = ShapeButton
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSelectionSheet(
    icons: List<String>,
    onIconSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Выберите иконку",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HeaderColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(icons.size) { index ->
                    val iconId = index + 1
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onIconSelected(iconId) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icons[index], fontSize = 32.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelectionSheet(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(),
        0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(),
        0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFFF5722.toInt()
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Выберите цвет",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HeaderColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(colors.size) { index ->
                    val color = colors[index]
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onColorSelected(color) }
                    ) {
                        drawCircle(color = Color(color))
                    }
                }
            }
        }
    }
}
