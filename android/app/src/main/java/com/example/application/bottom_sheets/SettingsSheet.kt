package com.example.application.bottom_sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text(text = "Поддержка") },
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
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
        )

        // Секция Аккаунт
        SettingsSection(title = "Аккаунт") {
            DestructiveSettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                text = "Выйти",
                onClick = {
                    appContainer.sessionManager.logout()
                    onLogout()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Секция Общее
        SettingsSection(title = "Общее") {
            SettingsRow(
                icon = Icons.Default.Notifications,
                title = "Разрешение уведомлений",
                subtitle = if (notificationsEnabled) "Включено" else "Выключено",
                onClick = { notificationsEnabled = !notificationsEnabled },
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Секция Безопасность и данные
        SettingsSection(title = "Безопасность и данные") {
            SettingsRow(
                icon = Icons.Default.Lock,
                title = "Безопасность",
                subtitle = "PIN-код и биометрия",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Archive,
                title = "Архив",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Секция О приложении
        SettingsSection(title = "О приложении") {
            SettingsRow(
                icon = Icons.Default.Email,
                title = "Поддержка",
                onClick = { activeDialog = SettingsDialogType.SUPPORT }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Description,
                title = "Политика конфиденциальности",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Share,
                title = "Поделиться с другом",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Feedback,
                title = "Предложения",
                subtitle = "Нашли баг или есть идея?",
                onClick = { activeDialog = SettingsDialogType.PLACEHOLDER }
            )
        }

        // Версия приложения
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Версия: Alpha 52",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// Переиспользуемые компоненты

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DestructiveSettingsRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
