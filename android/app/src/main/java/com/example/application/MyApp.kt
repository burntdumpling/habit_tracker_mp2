package com.example.application

import android.app.Application
import com.example.application.data.AppContainer
import com.example.application.notifications.HabitNotificationChannels

class MyApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        HabitNotificationChannels(this).ensureCreated()
        appContainer = AppContainer(this)
    }
}
