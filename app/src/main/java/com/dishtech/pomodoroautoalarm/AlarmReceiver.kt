package com.dishtech.pomodoroautoalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

/**
 * This class acts as a receiver for the alarm, invoking the alarm service.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // Tag used to identify the work request.
        const val POMODORO_ALARM_WORK_TAG = "pomodoro_alarm_work_tag"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }
        val workData = workDataOf(
            "ALARM_URI" to intent?.getStringExtra("ALARM_URI"),
            "IS_WORK" to intent?.getBooleanExtra("IS_WORK", false)
        )
        val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(POMODORO_ALARM_WORK_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
    }
}