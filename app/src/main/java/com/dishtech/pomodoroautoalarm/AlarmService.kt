package com.dishtech.pomodoroautoalarm
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class AlarmService : Service() {

    // Media player used to play alarms.
    private var mediaPlayer: MediaPlayer? = null

    // Notification manager used to show the notification.
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ringtoneUriString = intent?.getStringExtra("ALARM_URI")
        val isWork = intent?.getBooleanExtra("IS_WORK", false)
        showNotification(isWork)
        var ringtoneUri = if (ringtoneUriString.isNullOrEmpty()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            ringtoneUriString.toUri()
        }
        if (!isValidSoundUri(ringtoneUri)) {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        playAlarmSound(ringtoneUri)
        return START_STICKY
    }

    /**
     * Returns true if the given URI points to a valid sound resource, false otherwise.
     *
     * @param uri: Uri to be checked.
     */
    private fun isValidSoundUri(uri: Uri?): Boolean {
        return try {
            val mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.let {
                it.release()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Plays the alarm sound using the provided URI
     *
     * @param uri: URI of the alarm sound to play.
     */
    private fun playAlarmSound(uri: Uri) {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    /**
     * Shows a notification informing the user about the timer finished, with a "Stop Alarm" button
     */
    private fun showNotification(isWork: Boolean?) {
        val stopIntent = Intent(this, StopAlarmReceiver::class.java)
        val stopPendingIntent =
            PendingIntent.getBroadcast(this, 0, stopIntent,
                                       PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val chanID = "alarm_channel"
        val text = if (isWork == true) "Work time is over! you can now take a brake." else "Bre" +
                "ak time is Over! you need to get back to work."
        val notification = NotificationCompat.Builder(this, chanID)
            .setContentTitle("Pomodoro Timer")
            .setContentText(text)
            .setSmallIcon(com.google.android.material.R.drawable.btn_radio_on_mtrl)  // Use your custom alarm icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(stopPendingIntent)
            .addAction(
                androidx.appcompat.R.drawable.btn_radio_off_mtrl,
                "Stop Alarm",
                stopPendingIntent
            )
            .build()

        val channel = NotificationChannel(
            chanID,
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the alarm when the service is destroyed
        stopAlarm()
    }

    /**
     * Stops the alarm from playing.
     */
    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                mediaPlayer = null
            }
        }
        stopForeground(true)
        stopSelf()
    }
}