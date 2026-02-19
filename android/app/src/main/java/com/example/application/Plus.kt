package com.example.application

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.habit.*
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Plus(
    onHabitCreated: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as MyApp).appContainer.habitsRepository
    
    var name by remember { mutableStateOf("") }
    var isWeekly by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val daysOfWeek = DayOfWeek.values()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Новая привычка",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                errorMessage = null
            },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Частота",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = !isWeekly,
                onClick = { isWeekly = false }
            )
            Text("Ежедневно", modifier = Modifier.padding(end = 16.dp))
            
            RadioButton(
                selected = isWeekly,
                onClick = { isWeekly = true }
            )
            Text("По дням недели")
        }

        if (isWeekly) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        label = { 
                            Text(
                                text = when(day) {
                                    DayOfWeek.MONDAY -> "Пн"
                                    DayOfWeek.TUESDAY -> "Вт"
                                    DayOfWeek.WEDNESDAY -> "Ср"
                                    DayOfWeek.THURSDAY -> "Чт"
                                    DayOfWeek.FRIDAY -> "Пт"
                                    DayOfWeek.SATURDAY -> "Сб"
                                    DayOfWeek.SUNDAY -> "Вс"
                                }
                            ) 
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Название не может быть пустым"
                    return@Button
                }
                
                val frequency = if (isWeekly) {
                    if (selectedDays.isEmpty()) {
                        errorMessage = "Выберите хотя бы один день"
                        return@Button
                    }
                    Frequency.Weekly(selectedDays)
                } else {
                    Frequency.Daily
                }

                val request = CreateHabitRequest(
                    name = name,
                    iconId = 0,
                    colorArgb = 0xFF6750A4.toInt(),
                    startDate = LocalDate.now(),
                    endDate = null,
                    frequency = frequency,
                    targets = listOf(HabitTarget(DayPart.FULL_DAY, 1))
                )

                repository.addHabit(request)
                    .onSuccess {
                        onHabitCreated()
                    }
                    .onFailure {
                        errorMessage = it.message ?: "Ошибка при создании"
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать привычку")
        }
    }
}
