package com.example.application.notifications

import android.content.Context

interface SystemNotificationAvailabilityChecker {
    fun areNotificationsAvailable(): Boolean
}

class AndroidSystemNotificationAvailabilityChecker(
    private val context: Context
) : SystemNotificationAvailabilityChecker {
    override fun areNotificationsAvailable(): Boolean {
        return NotificationPermissionHelper.areNotificationsAvailable(context)
    }
}
