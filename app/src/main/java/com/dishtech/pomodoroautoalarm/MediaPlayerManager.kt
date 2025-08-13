package com.dishtech.pomodoroautoalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

class MediaPlayerManager(val context: Context) {

    // URI for the default alarm sound.
    val DEFAULT_ALARM_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    // A variable used to store the sound selected by the user.
    var selectedSoundUri: Uri? = null
        set(value) {
            field = if (isValidSoundUri(value)) {
                value
            } else {
                DEFAULT_ALARM_URI
            }
        }

    // Media player for playing and stopping the alarm.
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Returns true if the given URI points to a valid sound resource, false otherwise.
     *
     * @param uri: Uri to be checked.
     */
    private fun isValidSoundUri(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }
        return try {
            val mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer?.let {
                it.release()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Plays the alarm sound using the selectedSoundUri if exists or defaults to the device's
     * default alarm sound.
     */
    fun playAlarmSound() {
        stopAlarm()
        val uri = selectedSoundUri ?: DEFAULT_ALARM_URI
        mediaPlayer = MediaPlayer.create(context, uri)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        mediaPlayer?.setAudioAttributes(attributes)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    /**
     * Stops the alarm from playing.
     */
    fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                onDestroy()
            }
        }
    }

    /**
     * Releases the media player resources.
     */
    fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}