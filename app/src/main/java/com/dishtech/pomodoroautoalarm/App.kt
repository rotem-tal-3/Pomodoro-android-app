package com.dishtech.pomodoroautoalarm

import android.app.Application

class App : Application() {

    // Lifecycle observer singleton for app state changes.
    lateinit var appLifecycleObserver: AppLifecycleObserver
        private set

    override fun onCreate() {
        super.onCreate()
        appLifecycleObserver = AppLifecycleObserver(this)
    }
}