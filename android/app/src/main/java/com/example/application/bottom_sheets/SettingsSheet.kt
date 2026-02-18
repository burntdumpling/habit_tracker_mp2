package com.example.application.bottom_sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.MyApp

// Enum для управления состоянием диалогов
private enum class SettingsDialogType {
    NONE,
    PLACEHOLDER,
    SUPPORT
}

@Composable
fun SettingsSheet(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MyApp).appContainer
    val clipboardManager = LocalClipboardManager.current
    
    // Состояния
    var notificationsEnabled by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf(SettingsDialogType.NONE) }

    // Reusable chevron for trailing icon
    val chevron: @Composable () -> Unit = {
        Text(
            text = "›",
            fontSize = 28.sp,
            color = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    // Обработка диалогов
    when (activeDialog) {
        SettingsDialogType.PLACEHOLDER -> {
            AlertDialog(
                onDismissRequest = { activeDialog = SettingsDialogType.NONE },
                title = { Text(text = "Скоро будет") },
                text = { Text(text = "Функция в разработке") },
                confirmButton = {
                    TextButton(onClick = { activeDialog = SettingsDialogType.NONE }) {
                        Text("Ок")
                    }
                }
            )
        }
        SettingsDialogType.SUPPORT -> {
            val email = "khusainov@sfedu.ru"
            AlertDialog(
                onDismissRequest = { activeDialog = SettingsDialogType.NONE },
                title = { Text(text = "Контакт саппорт") },
                text = { Text(text = email) },
                confirmButton = {
                    TextButton(onClick = { activeDialog = SettingsDialogType.NONE }) {
                        Text("Ок")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(email))
                        activeDialog = SettingsDialogType.NONE
                    }) {
                        Text("Скопировать")
                    }
                }
            )
        }
        SettingsDialogType.NONE -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Заголовок
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        SettingsDivider()

        // Секция Аккаунт
        SettingsSection(title = "Аккаунт") {
            DestructiveSettingsRow(
                text = "Выйти",
                onClick = {
                    appContainer.sessionManager.logout()
                    onLogout()
                }
            )
        }

        SettingsDivider()

        // Секция Общее
        SettingsSection(title = "Общее") {
            SettingsRow(
                title = "Разрешение уведомлений",
                subtitle = if (notificationsEnabled) "Включено" else "Выключено",
                onClick = { notificationsEnabled = !notificationsEnabled },
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            )
        }

        SettingsDivider()

        // Секция Безопасность и данные
        SettingsSection(title = "Безопасность и данные") {
            SettingsRow(
                title = "Безопасность",
                subtitle = "PIN-код и биометрия",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER },
                trailing = chevron
            )
            SettingsRow(
                title = "Архив",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER },
                trailing = chevron
            )
        }

        SettingsDivider()

        // Секция О приложении
        SettingsSection(title = "О приложении") {
            SettingsRow(
                title = "Контакт саппорт",
                onClick = { activeDialog = SettingsDialogType.SUPPORT },
                trailing = chevron
            )
            SettingsRow(
                title = "Политика конфиденциальности",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER },
                trailing = chevron
            )
            SettingsRow(
                title = "Поделиться с другом",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER },
                trailing = chevron
            )
            SettingsRow(
                title = "Предложения",
                subtitle = "Нашли баг или есть идея?",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER },
                trailing = chevron
            )
        }

        // Версия приложения
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Версия: Alpha 52",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Переиспользуемые компоненты

@Composable
fun DestructiveSettingsRow(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
