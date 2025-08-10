package com.dishtech.pomodoroautoalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A class to mitigate the foreground stop alarm service to the app.
 */
class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.stopService(serviceIntent)
    }

}