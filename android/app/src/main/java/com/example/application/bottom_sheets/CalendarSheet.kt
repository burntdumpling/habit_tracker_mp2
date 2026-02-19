package com.example.application.bottom_sheets

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSheet(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onClose: () -> Unit
) {
    // 1) МСК (для "сегодня" и любых сравнений по МСК)
    val mskZoneId = remember { ZoneId.of("Europe/Moscow") }

    // 2) ВАЖНО: millis <-> date делаем через UTC — это убирает смещение "-1 день"
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    // Автоприменение и закрытие только после реального клика (первое значение пропускаем)
    LaunchedEffect(pickerState) {
        snapshotFlow { pickerState.selectedDateMillis }
            .filterNotNull()
            .distinctUntilChanged()
            .drop(1)
            .map { millis -> Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate() }
            .collect { pickedDate ->
                // Если тебе где-то нужно сравнение "сегодня" по МСК:
                // val todayMsk = LocalDate.now(mskZoneId)

                onDateSelected(pickedDate)
                onClose()
            }
    }

    DatePicker(
        state = pickerState,
        title = null,
        headline = null,
        showModeToggle = false,
        colors = DatePickerDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
