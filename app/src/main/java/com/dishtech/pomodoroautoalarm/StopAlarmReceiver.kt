package com.dishtech.pomodoroautoalarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * A class to mitigate the foreground stop alarm service to the app.
 */
class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context.applicationContext)
            .cancelAllWorkByTag(AlarmReceiver.POMODORO_ALARM_WORK_TAG)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmWorker.NOTIFICATION_ID)
    }

}