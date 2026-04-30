package com.example.application.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionHelperTest {

    @Test
    fun isRuntimePermissionRequired_returnsFalseBelow33() {
        assertFalse(NotificationPermissionHelper.isRuntimePermissionRequired(apiLevel = 32))
    }

    @Test
    fun isRuntimePermissionRequired_returnsTrueFrom33() {
        assertTrue(NotificationPermissionHelper.isRuntimePermissionRequired(apiLevel = 33))
    }

    @Test
    fun canUseNotificationsNow_returnsTrueWhenPermissionNotRequired() {
        assertTrue(
            NotificationPermissionHelper.canUseNotificationsNow(
                apiLevel = 31,
                permissionGranted = false
            )
        )
    }

    @Test
    fun canUseNotificationsNow_returnsFalseWhenPermissionRequiredAndDenied() {
        assertFalse(
            NotificationPermissionHelper.canUseNotificationsNow(
                apiLevel = 34,
                permissionGranted = false
            )
        )
    }
}
