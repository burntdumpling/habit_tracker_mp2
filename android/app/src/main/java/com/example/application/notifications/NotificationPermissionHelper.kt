package com.example.application.notifications

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {

    fun isRuntimePermissionRequired(apiLevel: Int = Build.VERSION.SDK_INT): Boolean {
        return apiLevel >= Build.VERSION_CODES.TIRAMISU
    }

    fun canUseNotificationsNow(
        apiLevel: Int = Build.VERSION.SDK_INT,
        permissionGranted: Boolean
    ): Boolean {
        return !isRuntimePermissionRequired(apiLevel) || permissionGranted
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        if (!isRuntimePermissionRequired()) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun areNotificationsAvailable(context: Context): Boolean {
        return canUseNotificationsNow(permissionGranted = hasPostNotificationsPermission(context))
    }
}
