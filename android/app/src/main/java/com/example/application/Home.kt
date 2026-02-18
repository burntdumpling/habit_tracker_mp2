package com.example.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.application.bottom_sheets.SettingsSheet
import com.example.application.bottom_sheets.CalendarSheet


@Composable
fun Home(
    onLogout: () -> Unit
) {
    Sheets(onLogout)
}








private enum class HomeBottomSheet {
    SETTINGS,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheets (
    onLogout: () -> Unit
) {
    var isSheetVisible by remember { mutableStateOf(false) }                                // состояние открыта ли шторка
    var currentSheet by remember { mutableStateOf(HomeBottomSheet.SETTINGS) }               // состояние какая именно шторка открыта

    val sheetState = rememberModalBottomSheetState (
        skipPartiallyExpanded = true
    )

    Box(
        modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
    ) {

        // КНОПКИ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundedIconButton(
                size = 45,
                cornerRadius = 12,
                onClick = {
                    currentSheet = HomeBottomSheet.SETTINGS
                    isSheetVisible = true
                },
                iconRes = R.drawable.settings,
                contentDescription = "Settings",
            )

            RoundedIconButton(
                size = 45,
                cornerRadius = 12,
                onClick = {
                    currentSheet = HomeBottomSheet.CALENDAR
                    isSheetVisible = true
                },
                iconRes = R.drawable.calendar,
                contentDescription = "Calendar",
            )
        }

        // bottom sheet
        if (isSheetVisible) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { isSheetVisible = false }
            ) {
                when (currentSheet) {
                    HomeBottomSheet.SETTINGS -> SettingsSheet(onLogout)
                    HomeBottomSheet.CALENDAR -> CalendarSheet()
                }
            }
        }
    }
}

// Кнопка
@Composable
private fun RoundedIconButton(
    size: Int = 64,
    cornerRadius: Int = 16,
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String,
    background: Color = Color.White,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = background,
        modifier = Modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
