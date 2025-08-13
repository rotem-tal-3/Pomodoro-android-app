package com.dishtech.pomodoroautoalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * This class acts as a background worker used to start the alarm.
 */
class AlarmWorker(val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Media player manager used to play alarms.
    private var mediaPlayerManager: MediaPlayerManager = MediaPlayerManager(appContext)
    companion object {
        // Notification ID and channel ID used to identify the notification.
        const val NOTIFICATION_ID = 123

        // Channel ID used to identify the notification channel.
        const val CHANNEL_ID = "pom_alarm_channel"

        // Request code used to identify the pending intent.
        const val INTENT_CODE = 987

        // Action used to stop the alarm via the notification.
        const val ACTION_STOP_ALARM_VIA_NOTIFICATION = "com.dishtech.pomodoroautoalarm.ACTION_STOP_ALARM_VIA_NOTIFICATION"
    }

    override suspend fun doWork(): Result {
        try {
            val alarmUriString = inputData.getString("ALARM_URI")
            mediaPlayerManager.selectedSoundUri = alarmUriString?.toUri()
            val isWork = inputData.getBoolean("IS_WORK", false)
            try {
                setForeground(createForegroundInfo(isWork))
                mediaPlayerManager.playAlarmSound()
                delay(30000)
            } catch (e: Exception) {
                Log.e("AlarmWorker", "Error in foregrounding", e)
                return Result.failure()
            } finally {
                mediaPlayerManager.onDestroy()
            }
            return Result.success()
        } catch (_: CancellationException) {
            mediaPlayerManager.onDestroy()
            return Result.success()
        } catch (e: Exception) {
            mediaPlayerManager.onDestroy()
            Log.e("AlarmWorker", "Error in doWork", e)
            return Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(
            inputData.getBoolean("IS_WORK", false)
        )
    }

    /**
     * Creates the notification for the alarm with a button to stop the alarm, using the given
     * isWork to determine the text content of the notification.
     *
     * @param isWork: True if the alarm is for the end of a work session, false otherwise.
     */
    private fun createForegroundInfo(isWork: Boolean): ForegroundInfo {
        val stopIntent = Intent(appContext,
                                StopAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM_VIA_NOTIFICATION
        }
        val stopPendingIntent =
            PendingIntent.getBroadcast(appContext, INTENT_CODE, stopIntent,
                                       PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (isWork) "Work Session Over!" else "Break Over!"
        val text = if (isWork) "Work time is over! you can now take a brake." else "Bre" +
                "ak time is over! you need to get back to work."

        val channel = NotificationChannel(CHANNEL_ID, "Alarm Notifications",
                                          NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(applicationContext,
                                                             CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_notification_clear_all,
                       "Stop alarm", stopPendingIntent)

        if (notificationBuilder.build().contentIntent == null) {
            notificationBuilder.setContentIntent(stopPendingIntent)
        }
        val notification = notificationBuilder.build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}