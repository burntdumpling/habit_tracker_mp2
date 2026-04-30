package com.example.application.notifications

interface HabitAchievementNotifier {
    fun showDailyCompletion(payload: AchievementNotificationPayload)
    fun showStreakAchievement(payload: AchievementNotificationPayload)
}

class HabitNotificationAchievementNotifier(
    private val notificationHelper: HabitNotificationHelper
) : HabitAchievementNotifier {
    override fun showDailyCompletion(payload: AchievementNotificationPayload) {
        notificationHelper.showDailyCompletion(payload)
    }

    override fun showStreakAchievement(payload: AchievementNotificationPayload) {
        notificationHelper.showStreakAchievement(payload)
    }
}
