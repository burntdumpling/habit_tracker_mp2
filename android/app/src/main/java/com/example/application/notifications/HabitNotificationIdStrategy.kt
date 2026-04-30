package com.example.application.notifications

object HabitNotificationIdStrategy {
    private const val REMINDER_PREFIX = 1_000_000
    private const val DAILY_COMPLETION_PREFIX = 2_000_000
    private const val STREAK_ACHIEVEMENT_PREFIX = 3_000_000
    private const val GROUPED_REMINDER_ID = 4_000_001
    private const val RANGE = 900_000

    fun reminderId(habitId: Long): Int {
        return REMINDER_PREFIX + stableId(habitId)
    }

    fun dailyCompletionId(habitId: Long): Int {
        return DAILY_COMPLETION_PREFIX + stableId(habitId)
    }

    fun streakAchievementId(habitId: Long, streakDays: Int?): Int {
        val salt = (streakDays ?: 0).toLong()
        return STREAK_ACHIEVEMENT_PREFIX + stableId(habitId xor (salt shl 32))
    }

    fun groupedReminderId(): Int = GROUPED_REMINDER_ID

    private fun stableId(raw: Long): Int {
        val folded = raw xor (raw ushr 32)
        return ((folded and Long.MAX_VALUE) % RANGE).toInt()
    }
}
