package com.dishtech.pomodoroautoalarm

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * This class acts as a view model, feeding necessary data from the view to the TimerManager,
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

    // A button used to stop the alarm sound.
    private lateinit var stopAlarmButton: Button

    // A button used to reset the cycle.
    private lateinit var resetButton: Button

    // Text input used to get the desired work time from the user.
    private lateinit var workTimeInput: EditText

    // Text input used to get the desired break time from the user.
    private lateinit var breakTimeInput: EditText

    // Text input used to get the desired ling break time from the user.
    private lateinit var longBreakTimeInput: EditText

    // Text input used to get the desired number of work- break time from the user.
    private lateinit var cyclesInput: EditText

    // Persistent storage manager used to retrieve user preferences.
    private lateinit var persistentStorageManager: PersistentStorageManager

    // A variable used to store the sound selected by the user.
    private var selectedSoundUri: Uri? = null

    // Media player for playing and stopping the alarm.
    private var mediaPlayer: MediaPlayer? = null

    // Launcher used to launch the ringtone select activity.
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            onAlarmSelected(data);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadViews()
        loadAlarm()
        setupTimes()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Loads the views controlled by this activity into their respective propertiees.
     */
    private fun loadViews() {
        persistentStorageManager = PersistentStorageManager(this)
        timeTextView = findViewById(R.id.timeTextView)
        startStopButton = findViewById(R.id.startStopButton)
        chooseSoundButton = findViewById(R.id.chooseSoundButton)
        stopAlarmButton = findViewById(R.id.stopAlarmButton)
        workTimeInput = findViewById(R.id.workTimeInput)
        breakTimeInput = findViewById(R.id.breakTimeInput)
        longBreakTimeInput = findViewById(R.id.longBreakTimeInput)
        cyclesInput = findViewById(R.id.cyclesInput)
        resetButton = findViewById(R.id.resetButton)
    }

    /**
     * Sets up the times loaded from the persistent storage into the timer manager and their
     * respective views.
     */
    private fun setupTimes() {
        val workTime = persistentStorageManager.getWorkTime()
        val breakTime = persistentStorageManager.getBreakTime()
        val longBreakTime = persistentStorageManager.getLongBreakTime()
        val cycles = persistentStorageManager.getCycles()
        timerManager = TimerManager(this, TimerUtils.minutesToMillis(workTime),
                                    TimerUtils.minutesToMillis(breakTime),
                                    TimerUtils.minutesToMillis(longBreakTime), cycles)
        setTimeTextViewToTime(timerManager.workTimeInMillis)
        workTimeInput.setText(workTime.toString())
        breakTimeInput.setText(breakTime.toString())
        longBreakTimeInput.setText(longBreakTime.toString())
        cyclesInput.setText(cycles.toString())
    }

    /**
     * Sets up the user action listeners.
     */
    private fun setupListeners() {
        startStopButton.setOnClickListener {
            if (timerManager.isTimerRunning) {
                timerManager.stopTimer()
                startStopButton.text = "Start"
            } else {
                timerManager.runCycles()
                startStopButton.text = "Pause"
            }
        }

        chooseSoundButton.setOnClickListener {
            openAlarmPicker()
        }

        stopAlarmButton.setOnClickListener {
            stopAlarm()
        }

        resetButton.setOnClickListener {
            timerManager.resetTimer()
            setTimeTextViewToTime(timerManager.workTimeInMillis)
            startStopButton.text = "Start"
        }

        workTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        breakTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        longBreakTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        cyclesInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
    }

    /**
     * Creates a text watcher invoking afterChange when a text field has been changed.
     *
     * @param afterChange: A function to be invoked after the change.
     */
    private fun createTextChangedWatcher(afterChange: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                afterChange()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    /**
     * Loads the alarm URI from the persistent storage.
     */
    private fun loadAlarm() {
        val savedAlarmUri = persistentStorageManager.getAlarmUri()
        if (savedAlarmUri.isNotEmpty()) {
            val uri = Uri.parse(savedAlarmUri)
            if (isValidSoundUri(uri)) {
                Toast.makeText(this, "is valid", Toast.LENGTH_SHORT).show()
                selectedSoundUri = uri
            }

        }
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
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Updates the timerManager times according to the current input in the text fields. On
     * NumberFormatException reverts the text display to the current times set in the timerManager.
     */
    private fun updateTimes() {
        try {
            val workMinutes = workTimeInput.text.toString().toIntOrNull() ?: TimerUtils.minutesPartOfMillis(timerManager.workTimeInMillis)
            val breakMinutes = breakTimeInput.text.toString().toIntOrNull() ?: TimerUtils.minutesPartOfMillis(timerManager.breakTimeInMillis)
            val longBreakMinutes = longBreakTimeInput.text.toString().toIntOrNull() ?: TimerUtils.minutesPartOfMillis(timerManager.longBreakTimeInMillis)
            val cycles = cyclesInput.text.toString().toIntOrNull() ?: timerManager.cyclesBeforeLongBreak
            timerManager.updateTimes(workMinutes, breakMinutes, longBreakMinutes, cycles)
            if (!timerManager.isRunningCycle) {
                setTimeTextViewToTime(timerManager.workTimeInMillis)
            }
            persistentStorageManager.saveTimes(workMinutes, breakMinutes, longBreakMinutes,
                                               cycles)
        } catch (e: NumberFormatException) {
            workTimeInput.setText(TimerUtils.minutesPartOfMillis(timerManager.workTimeInMillis).toString())
            breakTimeInput.setText(TimerUtils.minutesPartOfMillis(timerManager.breakTimeInMillis).toString())
            longBreakTimeInput.setText(TimerUtils.minutesPartOfMillis(timerManager.longBreakTimeInMillis))
            cyclesInput.setText(timerManager.cyclesBeforeLongBreak)
        }
    }

    private fun setTimeTextViewToTime(time: Long) {
        val minutes = TimerUtils.minutesPartOfMillis(time)
        val seconds = TimerUtils.secondsPartOfMillis(time)
        timeTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Updates the timer view on tick.
     *
     * @param timeLeft: Time left for the timer in milliseconds
     */
    override fun onTick(timeLeft: Long) {
        setTimeTextViewToTime(timeLeft)
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
        val soundUri = if (selectedSoundUri != null) selectedSoundUri else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, soundUri)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        mediaPlayer?.setAudioAttributes(attributes)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
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
     * Opens the alarm picker.
     */
    private fun openAlarmPicker() {
        stopAlarm()
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        resultLauncher.launch(intent)
    }

    /**
     * Handles alarm selection.
     */
    private fun onAlarmSelected(data: Intent?) {
        val alarmUri: Uri = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
        selectedSoundUri = alarmUri
        persistentStorageManager.saveAlarmUri(alarmUri.toString())
    }
}