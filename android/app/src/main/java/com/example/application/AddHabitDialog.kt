package com.example.application

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape



@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape: Shape = RoundedCornerShape(24.dp)

        var title by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
        var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
        var selectedSlot by rememberSaveable { mutableStateOf(TimeSlot.MORNING) }

        val scroll = rememberScrollState()

        Surface(
            shape = shape,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scroll)
            ) {
                // Верхняя строка: пустое место слева + "Сохранить" справа
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSave) {
                        Text("Сохранить")
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") }
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text("Описание") }
                )

                Spacer(Modifier.height(16.dp))

                // Заглушки под блоки (иконки/цвет/что-то ещё)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(56.dp))
                    Box(modifier = Modifier.size(56.dp))
                    Box(modifier = Modifier.weight(1f).height(56.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Повторять / Каждый день (пока просто текст)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Повторять")
                    Text("Каждый день")
                }

                Spacer(Modifier.height(16.dp))

                // Выбор периода дня: прокручивается, кликается, НЕ круговой
                Text("Время дня")
                Spacer(Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(TimeSlot.entries) { slot ->
                        val selected = (slot == selectedSlot)

                        Box(
                            modifier = Modifier
                                .clickable { selectedSlot = slot }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = slot.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Напоминания (пока просто статичный блок)
                Text("Напоминание")
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderRow(time = "08:00")
                    ReminderRow(time = "09:30")

                    TextButton(onClick = { /* позже добавим */ }) {
                        Text("Добавить время")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReminderRow(time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(time)
        Spacer(Modifier.weight(1f))
        // Заглушка под "редактировать" / иконку
        Text("✎")
    }
}
