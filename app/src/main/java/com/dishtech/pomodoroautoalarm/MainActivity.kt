package com.dishtech.pomodoroautoalarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

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

    // Alarm manager used to schedule alarms.
    private lateinit var alarmManager: AlarmManager

    // Media player manager used for playing and stopping the alarm.
    private lateinit var mediaPlayerManager: MediaPlayerManager

    // Latest pending intent used.
    private var latestIntent: PendingIntent? = null

    // Request code used for the alarm setting.
    private val ALARM_CODE = 1234

    // Launcher used to launch the ringtone select activity.
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            onAlarmSelected(data);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mediaPlayerManager = MediaPlayerManager(applicationContext)
        loadViews()
        loadAlarm()
        setupTimes()
        setupListeners()
        setupAlarmManger()
        if (!isNotificationsEnabled()) {
            showEnableNotificationDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerManager.onDestroy()
    }

    /**
     * Sets up the alarm manager.
     */
    private fun setupAlarmManger() {
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            showEnablePermissionDialog()
        }
    }

    /**
     * Loads the views controlled by this activity into their respective properties.
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
        timerManager = TimerManager(WeakReference(this),
                                    TimerUtils.minutesToMillis(workTime),
                                    TimerUtils.minutesToMillis(breakTime),
                                    TimerUtils.minutesToMillis(longBreakTime),
                                    cycles)
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
                cancelPendingIntent()
                timerManager.stopTimer()
                startStopButton.text = "Start"
            } else {
                timerManager.runCycles()
                startStopButton.text = "Pause"
                if (alarmManager.canScheduleExactAlarms()) {
                    runBlocking {
                        setAlarm(timerManager.timeLeft, timerManager.isWork)
                    }
                }
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
            cancelPendingIntent()
        }

        workTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        breakTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        longBreakTimeInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
        cyclesInput.addTextChangedListener(createTextChangedWatcher { updateTimes() })
    }

    /**
     * Shows a dialog asking the user to enable notifications.
     */
    fun showEnableNotificationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Notifications are turned off for this app. Would you like to enable them?")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows a dialog asking the user to enable alarm permissions.
     */
    fun showEnablePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Alarm Permissions")
            .setMessage("Alarm permissions are turned off for this app. Would you like to enable them?")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }.setNegativeButton("Cancel", null).show()
    }

    /**
     * Returns true if notifications are enabled, false otherwise.
     */
    fun isNotificationsEnabled(): Boolean {
        val notificationManager = NotificationManagerCompat.from(this)
        return notificationManager.areNotificationsEnabled()
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
            mediaPlayerManager.selectedSoundUri = savedAlarmUri.toUri()
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

    private fun cancelPendingIntent() {
        if (latestIntent != null) {
            alarmManager.cancel(latestIntent!!)
            latestIntent?.cancel()
            latestIntent = null
        }
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
    override fun onTimerFinished(isWork: Boolean) {
        val app = applicationContext as App
        val time = if (isWork) timerManager.breakTimeInMillis else timerManager.workTimeInMillis
        runBlocking {
            setAlarm(time, isWork)
        }
        if (app.appLifecycleObserver.isAppInForeground()) {
            cancelPendingIntent()
            playAlarmSound()
        }
        //startService(serviceIntent)
    }

    /**
     * Schedules an alarm for the given time.
     */
    suspend fun setAlarm(timeInMillis: Long, isWork: Boolean) {
        val serviceIntent = Intent(this.applicationContext,
                                   AlarmReceiver::class.java).apply {
            action = "com.dishtech.pomodoroautoalarm.ALARM_TRIGGERED_ACTION"
            putExtra("ALARM_URI", mediaPlayerManager.selectedSoundUri?.toString() ?: "")
            putExtra("IS_WORK", isWork)
        }
        val delayTime = 100L
        val pendingIntent = PendingIntent.getBroadcast(this.applicationContext, ALARM_CODE,
                                                serviceIntent,
                                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        latestIntent = pendingIntent
        val triggerAt = System.currentTimeMillis() + delayTime + timeInMillis
        alarmManager.setExactAndAllowWhileIdle( AlarmManager.RTC_WAKEUP,
                                                triggerAt,
                                                pendingIntent)
        delay(delayTime) // This is some black magic to ensure the alarm is set.
    }

    /**
     * Plays the alarm sound.
     */
    private fun playAlarmSound() {
        stopAlarm()
        mediaPlayerManager.playAlarmSound()
    }

    /**
     *  Stops the currently playing alarm sound.
     */
    private fun stopAlarm() {
        sendStopAlarmBroadcast()
        mediaPlayerManager.stopAlarm()
    }

    /**
     * Sends a broadcast to stop the alarm.
     */
    private fun sendStopAlarmBroadcast() {
        val stopIntent = Intent(this, StopAlarmReceiver::class.java)
        this.sendBroadcast(stopIntent)
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
        if (data == null) {
            return
        }
        val alarmUri:Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                                    Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        mediaPlayerManager.selectedSoundUri = alarmUri
        persistentStorageManager.saveAlarmUri(alarmUri.toString())
    }
}