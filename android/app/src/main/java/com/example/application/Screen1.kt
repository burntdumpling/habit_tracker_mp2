package com.example.application

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi



private data class Task(
    val id: Int,
    val title: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Screen1() {

    // выбранный период (сохраняется при повороте экрана)
    var selectedSlot by rememberSaveable { mutableStateOf(TimeSlot.MORNING) }

    // демо-данные: задачи по периодам (потом заменишь на реальные)
    val tasksBySlot = remember {
        mapOf(
            TimeSlot.MORNING to listOf(
                Task(1, "Приготовить завтрак"),
                Task(2, "Зарядка"),
                Task(3, "Почистить кухню")
            ),
            TimeSlot.DAY to listOf(
                Task(4, "Учёба / пары"),
                Task(5, "Сделать домашку"),
                Task(6, "Прогулка")
            ),
            TimeSlot.EVENING to listOf(
                Task(7, "Уборка"),
                Task(8, "Приготовить ужин")
            ),
            TimeSlot.NIGHT to listOf(
                Task(9, "Чтение"),
                Task(10, "Сон")
            )
        )
    }

    val visibleTasks = tasksBySlot[selectedSlot].orEmpty()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // ===== Пустое место под календарь (позже добавим) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )

        // ===== Переключатель периодов дня: конечный скролл + без overscroll =====
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(TimeSlot.entries) { slot ->
                    val isSelected = (slot == selectedSlot)

                    Box(
                        modifier = Modifier
                            .clickable { selectedSlot = slot }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = slot.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ===== Список задач (меняется от выбранного периода) =====
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleTasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // заглушка под иконку
                    Box(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = task.title)
                }
            }
        }
    }
}
