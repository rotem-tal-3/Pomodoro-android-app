package com.dishtech.pomodoroautoalarm
import android.content.Context
import android.content.SharedPreferences

/**
 * Class for managing the persistent storage.
 */
class PersistentStorageManager(context: Context) {

    // Saved shared preferences.
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PomodoroPreferences", Context.MODE_PRIVATE)

    // Key for the work time.
    private val WORK_TIME_KEY = "work_time"

    // Key for the break time.
    private val BREAK_TIME_KEY = "break_time"

    // Key for the long break time.
    private val LONG_BREAK_TIME_KEY = "long_break_time"

    // Key for the number of cycles.
    private val CYCLES_KEY = "cycles"

    // Key for the alarm sound.
    private val ALARM_KEY = "alarm_sound"

    // Default work time in minutes.
    private val DEFAULT_WORK_TIME = 25

    // Default break time in minutes.
    private val DEFAULT_BREAK_TIME = 5

    // Default long break time in minutes.
    private val DEFAULT_LONG_BREAK_TIME = 15

    // Default number of cycles.
    private val DEFAULT_CYCLES = 4

    // Default alarm sound URI.
    private val DEFAULT_ALARM_URI = ""

    /**
     * Saves the given times to the persistent storage.
     *
     * @param workTime: Work time in minutes.
     * @param breakTime: Break time in minutes.
     * @param longBreakTime: Long break time in minutes.
     * @param cycles: Number of cycles.
     */
    fun saveTimes(workTime: Int, breakTime: Int, longBreakTime: Int, cycles: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(WORK_TIME_KEY, workTime)
        editor.putInt(BREAK_TIME_KEY, breakTime)
        editor.putInt(LONG_BREAK_TIME_KEY, longBreakTime)
        editor.putInt(CYCLES_KEY, cycles)
        editor.apply()
    }

    /**
     * Saves the alarm Uri to the persistent storage.
     *
     * @param uri: String representing the URI of the selected alarm.
     */
    fun saveAlarmUri(uri: String) {
        val editor = sharedPreferences.edit()
        editor.putString(ALARM_KEY, uri)
        editor.apply()
    }

    /**
     * Returns the saved work time from the persistent storage, or the default if none is saved.
     */
    fun getWorkTime(): Int {
        return sharedPreferences.getInt(WORK_TIME_KEY, DEFAULT_WORK_TIME)
    }

    /**
     * Returns the saved break time from the persistent storage, or the default if none is saved.
     */
    fun getBreakTime(): Int {
        return sharedPreferences.getInt(BREAK_TIME_KEY, DEFAULT_BREAK_TIME)
    }

    /**
     * Returns the saved long break time from the persistent storage, or the default if none is
     * saved.
     */
    fun getLongBreakTime(): Int {
        return sharedPreferences.getInt(LONG_BREAK_TIME_KEY, DEFAULT_LONG_BREAK_TIME)
    }

    /**
     * Returns the saved number of cycles from the persistent storage, or the default if none is
     * saved.
     */
    fun getCycles(): Int {
        return sharedPreferences.getInt(CYCLES_KEY, DEFAULT_CYCLES)
    }

    /**
     * Returns the saved alarm URI from the persistent storage, or the default if none is saved.
     */
    fun getAlarmUri(): String {
        return sharedPreferences.getString(ALARM_KEY, DEFAULT_ALARM_URI) ?: DEFAULT_ALARM_URI
    }
}
