package com.example.application.bottom_sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.data.settings.HabitStatusFilter
import com.example.application.settings.SettingsAction
import com.example.application.settings.SettingsDialogType
import com.example.application.settings.SettingsUiState

private val SheetHorizontalPadding = 20.dp
private val SectionSpacing = 18.dp
private val SectionShape = RoundedCornerShape(24.dp)
private val RowIconShape = RoundedCornerShape(16.dp)
private val DialogShape = RoundedCornerShape(28.dp)

@Composable
fun SettingsSheet(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit
) {
    SettingsDialogs(uiState = uiState, onAction = onAction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SheetHorizontalPadding)
            .padding(bottom = 28.dp)
    ) {
        SettingsHeader(versionLabel = uiState.appVersionLabel)

        SettingsSection(
            title = "Основное",
            description = "То, что влияет на ежедневный экран"
        ) {
            SettingsRow(
                icon = Icons.Default.FilterList,
                title = "Отображение привычек",
                subtitle = uiState.habitStatusFilterSubtitle,
                emphasis = true,
                onClick = { onAction(SettingsAction.OpenStatusFilter) }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Notifications,
                title = "Уведомления",
                subtitle = uiState.notificationsSubtitle,
                onClick = { onAction(SettingsAction.ToggleNotifications) },
                trailing = {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { checked -> onAction(SettingsAction.SetNotificationsEnabled(checked)) },
                        modifier = Modifier.scale(0.86f)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(SectionSpacing))

        SettingsSection(
            title = "Безопасность и данные",
            description = "Настройки, которые появятся в следующих версиях"
        ) {
            SettingsRow(
                icon = Icons.Default.Lock,
                title = "Безопасность",
                subtitle = "PIN-код и биометрия",
                onClick = { onAction(SettingsAction.OpenSecurityPlaceholder) }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Archive,
                title = "Архив",
                subtitle = "Скрытые и завершённые привычки",
                onClick = { onAction(SettingsAction.OpenArchivePlaceholder) }
            )
        }

        Spacer(modifier = Modifier.height(SectionSpacing))

        SettingsSection(
            title = "О приложении",
            description = "Связь, документы и обратная связь"
        ) {
            SettingsRow(
                icon = Icons.Default.Email,
                title = "Поддержка",
                subtitle = "Написать или скопировать email",
                onClick = { onAction(SettingsAction.OpenSupport) }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Description,
                title = "Политика конфиденциальности",
                subtitle = "Раздел готовится",
                onClick = { onAction(SettingsAction.OpenPrivacyPlaceholder) }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Share,
                title = "Поделиться с другом",
                subtitle = "Скоро будет доступно",
                onClick = { onAction(SettingsAction.OpenSharePlaceholder) }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Feedback,
                title = "Предложения",
                subtitle = "Нашли баг или есть идея?",
                onClick = { onAction(SettingsAction.OpenFeedbackPlaceholder) }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        DestructiveSettingsRow(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Выйти из аккаунта",
            subtitle = "Потребуется повторный вход",
            onClick = { onAction(SettingsAction.LogoutClicked) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.appVersionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SettingsDialogs(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit
) {
    when (uiState.activeDialog) {
        SettingsDialogType.PLACEHOLDER -> {
            SettingsInfoDialog(
                title = uiState.dialogTitle,
                message = uiState.dialogMessage,
                confirmText = "Понятно",
                onConfirm = { onAction(SettingsAction.DismissDialog) },
                onDismiss = { onAction(SettingsAction.DismissDialog) }
            )
        }

        SettingsDialogType.SUPPORT -> {
            SettingsInfoDialog(
                title = uiState.dialogTitle,
                message = uiState.dialogMessage,
                confirmText = "Написать",
                dismissText = "Скопировать",
                onConfirm = { onAction(SettingsAction.SupportSendEmailClicked) },
                onDismissButton = { onAction(SettingsAction.SupportCopyEmailClicked) },
                onDismiss = { onAction(SettingsAction.DismissDialog) }
            )
        }

        SettingsDialogType.LOGOUT_CONFIRM -> {
            SettingsInfoDialog(
                title = uiState.dialogTitle,
                message = uiState.dialogMessage,
                confirmText = "Выйти",
                dismissText = "Отмена",
                destructiveConfirm = true,
                onConfirm = { onAction(SettingsAction.ConfirmLogout) },
                onDismissButton = { onAction(SettingsAction.DismissDialog) },
                onDismiss = { onAction(SettingsAction.DismissDialog) }
            )
        }

        SettingsDialogType.STATUS_FILTER -> {
            AlertDialog(
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                shape = DialogShape,
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = uiState.dialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = uiState.dialogMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HabitStatusFilter.entries.forEach { filter ->
                            StatusFilterOptionRow(
                                title = filter.asTitle(),
                                selected = filter == uiState.selectedHabitStatusFilter,
                                onClick = { onAction(SettingsAction.SelectHabitStatusFilter(filter)) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onAction(SettingsAction.DismissDialog) }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        SettingsDialogType.NONE -> Unit
    }
}

@Composable
private fun SettingsInfoDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    destructiveConfirm: Boolean = false,
    onDismissButton: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (destructiveConfirm) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismissButton ?: onDismiss) {
                    Text(text = dismissText)
                }
            }
        }
    )
}

@Composable
private fun SettingsHeader(versionLabel: String) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .background(
                    color = colors.onSurfaceVariant.copy(alpha = 0.28f),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.primaryContainer.copy(alpha = 0.42f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.10f),
                                colors.tertiary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Управление аккаунтом, уведомлениями и отображением привычек.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.surface.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = versionLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFilterOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        } else {
            Color.Transparent
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = SectionShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
    trailing: @Composable (() -> Unit)? = null,
    emphasis: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, emphasis = emphasis)
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DestructiveSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = SectionShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RowIconShape,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    emphasis: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (emphasis) {
        colors.primaryContainer.copy(alpha = 0.74f)
    } else {
        colors.surfaceVariant.copy(alpha = 0.72f)
    }
    val iconColor = if (emphasis) colors.primary else colors.onSurfaceVariant

    Surface(
        shape = RowIconShape,
        color = containerColor
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .padding(10.dp)
                .size(22.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun HabitStatusFilter.asTitle(): String {
    return when (this) {
        HabitStatusFilter.ALL -> "Все"
        HabitStatusFilter.ACTIVE -> "Активные"
        HabitStatusFilter.PAUSED -> "Приостановленные"
        HabitStatusFilter.FINISHED -> "Завершённые"
    }
}
