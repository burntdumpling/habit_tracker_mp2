package com.example.application

import android.app.Application
import com.example.application.data.AppContainer

class MyApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
