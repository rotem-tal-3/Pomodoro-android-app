package com.dishtech.pomodoroautoalarm

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * This class acts as a controller, feeding necessary data from the view to the TimerManager,
 * and receiving data using the TimerDelegate interface.
 */
class MainActivity : AppCompatActivity(), TimerManager.TimerDelegate {

    // Underlying timer manager to manage the timers.
    private lateinit var timerManager: TimerManager

    // Text view showing the remaining time on the clock
    private lateinit var timeTextView: TextView

    // A button for initiating or stopping the timer.
    private lateinit var startStopButton: Button

    // A button used to open the ringtone manager and to choose a sound.
    private lateinit var chooseSoundButton: Button

    // A button used to set the new times set by the user.
    private lateinit var applyTimeButton: Button

    // A button used to stop the alarm sound.
    private lateinit var stopAlarmButton: Button

    // Text input used to get the desired work time from the user.
    private lateinit var workTimeInput: EditText

    // Text input used to get the desired break time from the user.
    private lateinit var breakTimeInput: EditText

    // Text input used to get the desired ling break time from the user.
    private lateinit var longBreakTimeInput: EditText

    // Text input used to get the desired number of work- break time from the user.
    private lateinit var cyclesInput: EditText

    // A variable used to store the sound selected by the user.
    private var selectedSoundUri: Uri? = null

    // Media player for playing and stoping the alarm.
    private var mediaPlayer: MediaPlayer? = null

    // Launcher used to launch the ringtone select activity.
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            onReingtoneSelected(data);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTextView = findViewById(R.id.timeTextView)
        startStopButton = findViewById(R.id.startStopButton)
        chooseSoundButton = findViewById(R.id.chooseSoundButton)
        applyTimeButton = findViewById(R.id.applyTimeButton)
        stopAlarmButton = findViewById(R.id.stopAlarmButton)
        workTimeInput = findViewById(R.id.workTimeInput)
        breakTimeInput = findViewById(R.id.breakTimeInput)
        longBreakTimeInput = findViewById(R.id.longBreakTimeInput)
        cyclesInput = findViewById(R.id.cyclesInput)
        timerManager = TimerManager(this)

        startStopButton.setOnClickListener {
            if (timerManager.isTimerRunning()) {
                timerManager.stopTimer()
                startStopButton.text = "Start"
            } else {
                timerManager.runCycles()
                startStopButton.text = "Stop"
            }
        }

        chooseSoundButton.setOnClickListener {
            openRingtonePicker()
        }

        applyTimeButton.setOnClickListener {
            applyCustomTimes()
        }


        stopAlarmButton.setOnClickListener {
            stopAlarm()
        }
    }

    /**
     * Updates the timer view on tick.
     *
     * @param timeLeft: Time left for the timer in milliseconds
     */
    override fun onTick(timeLeft: Long) {
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        timeTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * A function used to inform this class that the timer has finished.
     */
    override fun onTimerFinished() {
        playAlarmSound()
    }

    /**
     * Plays the alarm sound.
     */
    private fun playAlarmSound() {
        if (selectedSoundUri != null) {
            mediaPlayer = MediaPlayer.create(this, selectedSoundUri)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
        } else {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
        }
    }

    /**
     *  Stops the currently playing alarm sound
     */
    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                mediaPlayer = null
            }
        }
    }

    /**
     * Opens the ringtone picker.
     */
    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        resultLauncher.launch(intent)
    }

    /**
     * Handles ringtone selection.
     */
    fun onReingtoneSelected(data: Intent?) {
        val ringtoneUri: Uri = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
        selectedSoundUri = ringtoneUri
    }

    /**
     * Applies the times set by the users.
     */
    private fun applyCustomTimes() {
        try {
            val workMinutes = workTimeInput.text.toString().toInt()
            val breakMinutes = breakTimeInput.text.toString().toInt()
            val longBreakMinutes = longBreakTimeInput.text.toString().toInt()
            val cycles = cyclesInput.text.toString().toInt()

            timerManager.updateTimes(workMinutes, breakMinutes, longBreakMinutes)
            timerManager.setCyclesBeforeLongBreak(cycles)
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
        }
    }
}