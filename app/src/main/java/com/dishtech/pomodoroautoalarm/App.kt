package com.dishtech.pomodoroautoalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.dishtech.pomodoroautoalarm.AlarmWorker.Companion.CHANNEL_ID

class App : Application() {

    // Lifecycle observer singleton for app state changes.
    lateinit var appLifecycleObserver: AppLifecycleObserver
        private set

    override fun onCreate() {
        super.onCreate()
        appLifecycleObserver = AppLifecycleObserver(this)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Alarm Notifications",
                                          NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
}