package com.example.application.data

import android.content.Context
import androidx.work.WorkManager
import com.example.application.data.auth.AuthApi
import com.example.application.data.auth.AuthRepository
import com.example.application.data.network.ApiClient
import com.example.application.data.notifications.DataStoreNotificationSettingsRepository
import com.example.application.data.notifications.DataStorePendingReminderRepository
import com.example.application.data.notifications.NotificationSettingsRepository
import com.example.application.data.notifications.PendingReminderRepository
import com.example.application.notifications.HabitNotificationHelper
import com.example.application.notifications.HabitAchievementEvaluator
import com.example.application.notifications.DefaultHabitAchievementEvaluator
import com.example.application.notifications.BackendHabitCompletionChecker
import com.example.application.notifications.HabitStreakCalculator
import com.example.application.notifications.DefaultHabitStreakCalculator
import com.example.application.notifications.HabitNotificationAchievementNotifier
import com.example.application.notifications.NextReminderCalculator
import com.example.application.notifications.NotificationSchedulingCoordinator
import com.example.application.notifications.NotificationScheduler
import com.example.application.notifications.AndroidSystemNotificationAvailabilityChecker
import com.example.application.notifications.GroupedReminderDispatchScheduler
import com.example.application.notifications.GroupedReminderDispatcher
import com.example.application.notifications.ReminderDispatchDecider
import com.example.application.notifications.WorkManagerNotificationScheduler
import com.example.application.data.settings.DataStoreSettingsPreferencesRepository
import com.example.application.data.settings.SettingsPreferencesRepository
import com.example.application.data.session.SessionManager
import com.example.application.data.statistics.NetworkStatisticsRepository
import com.example.application.data.statistics.StatisticsRepository
import com.example.application.data.storage.SharedPrefsTokenStorage
import com.example.application.data.storage.TokenStorage

class AppContainer(
    context: Context
) {
    private val appContext = context.applicationContext

    val tokenStorage: TokenStorage = SharedPrefsTokenStorage(appContext)
    val sessionManager: SessionManager = SessionManager(tokenStorage)
    val settingsPreferencesRepository: SettingsPreferencesRepository =
        DataStoreSettingsPreferencesRepository(appContext)
    val notificationSettingsRepository: NotificationSettingsRepository =
        DataStoreNotificationSettingsRepository(appContext)
    val pendingReminderRepository: PendingReminderRepository =
        DataStorePendingReminderRepository(appContext)
    val habitNotificationHelper: HabitNotificationHelper = HabitNotificationHelper(appContext)
    val reminderDispatchDecider: ReminderDispatchDecider = ReminderDispatchDecider()
    val habitAchievementNotifier = HabitNotificationAchievementNotifier(habitNotificationHelper)
    val systemNotificationAvailabilityChecker = AndroidSystemNotificationAvailabilityChecker(appContext)
    private val apiClient: ApiClient = ApiClient(tokenStorage, sessionManager)
    val authApi: AuthApi = apiClient.createAuthApi()
    val authRepository: AuthRepository = AuthRepository(authApi, tokenStorage, sessionManager)
    
    private val habitApi: com.example.application.data.network.api.HabitApiService = apiClient.createHabitApi()
    val habitsRepository: com.example.application.data.habits.HabitsRepository = com.example.application.data.habits.NetworkHabitsRepository(habitApi)
    val statisticsRepository: StatisticsRepository = NetworkStatisticsRepository(habitApi)
    private val workManager: WorkManager = WorkManager.getInstance(appContext)
    val groupedReminderDispatchScheduler: GroupedReminderDispatchScheduler =
        GroupedReminderDispatchScheduler(workManager)
    private val nextReminderCalculator: NextReminderCalculator = NextReminderCalculator()
    val habitCompletionChecker = BackendHabitCompletionChecker(habitsRepository)
    val habitStreakCalculator: HabitStreakCalculator = DefaultHabitStreakCalculator(habitsRepository)
    val groupedReminderDispatcher: GroupedReminderDispatcher = GroupedReminderDispatcher(
        pendingReminderRepository = pendingReminderRepository,
        reminderDispatchDecider = reminderDispatchDecider,
        reminderNotifier = habitNotificationHelper
    )
    val habitAchievementEvaluator: HabitAchievementEvaluator = DefaultHabitAchievementEvaluator(
        habitsRepository = habitsRepository,
        notificationSettingsRepository = notificationSettingsRepository,
        habitCompletionChecker = habitCompletionChecker,
        habitStreakCalculator = habitStreakCalculator,
        achievementNotifier = habitAchievementNotifier,
        notificationAvailabilityChecker = systemNotificationAvailabilityChecker
    )
    val notificationScheduler: NotificationScheduler = WorkManagerNotificationScheduler(
        workManager = workManager,
        habitsRepository = habitsRepository,
        notificationSettingsRepository = notificationSettingsRepository,
        habitCompletionChecker = habitCompletionChecker,
        nextReminderCalculator = nextReminderCalculator
    )
    val notificationSchedulingCoordinator: NotificationSchedulingCoordinator =
        NotificationSchedulingCoordinator(
            notificationSettingsRepository = notificationSettingsRepository,
            notificationScheduler = notificationScheduler,
            habitCompletionChecker = habitCompletionChecker
        )
}
