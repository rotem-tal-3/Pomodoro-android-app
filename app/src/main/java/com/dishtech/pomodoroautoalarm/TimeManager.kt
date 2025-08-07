package com.dishtech.pomodoroautoalarm
import android.os.CountDownTimer

/**
 * A class for managing the timer logic and state. Responsible for switching between the work, break
 * and long breaks timers.
 *
 * @param delegate: A delegate to be updated on ticks and finished timers.
 */
class TimerManager(private val delegate: TimerDelegate) {

    /**
     *  An interface implemented by delegates of the timer manager.
     */
    interface TimerDelegate {
        fun onTick(timeLeft: Long)
        fun onTimerFinished()
    }

    // A timer for internal use.
    private var countDownTimer: CountDownTimer? = null

    // A count of the number work-break cycles since the last long break.
    private var cycleCount = 0

    // Number of work-break cycles before a long break.
    var cyclesBeforeLongBreak: Int = 4
        private set

    // Work time timer in milliseconds.
    var workTimeInMillis: Long = TimerUtils.minutesToMillis(25)
        private set

    // Break time timer in milliseconds.
    var breakTimeInMillis: Long = TimerUtils.minutesToMillis(5)
        private set

    // Long break time timer in milliseconds.
    var longBreakTimeInMillis: Long = TimerUtils.minutesToMillis(15)
        private set

    // A boolean indicating whatever the timer is running or not.
    var isTimerRunning = false
        private set

    // Indicates the remaining time on the timer.
    private var timeLeft: Long = 0L

    // Indicates whatever the timer is currently counting work or a break.
    private var isWork: Boolean = false

    // Indicates whatever the timer is currently running a cycle.
    var isRunningCycle = false
        private set

    /**
     * Starts the Pomodoro cycle.
     */
    fun runCycles() {
        if (isRunningCycle) {
            resumeTimer()
            return
        }
        isRunningCycle = true
        cycleCount = 0
        isWork = true
        startTimer(workTimeInMillis)
    }

    /**
     * Starts the timer with the time set to timeInMillis.
     *
     * @param timeInMillis: The duration of the timer.
     */
    private fun startTimer(timeInMillis: Long) {

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                delegate.onTick(millisUntilFinished)
                timeLeft = millisUntilFinished
            }

            override fun onFinish() {
                delegate.onTimerFinished()
                if (!isWork) {
                    isWork = true
                    startTimer(workTimeInMillis)
                } else if (cycleCount < cyclesBeforeLongBreak) {
                    startBreakCycle()
                } else {
                    startLongBreak()
                }
            }
        }
        countDownTimer?.start()
        isTimerRunning = true
    }

    /**
     * Starts the timer for a break.
     */
    private fun startBreakCycle() {
        cycleCount++
        isWork = false
        startTimer(breakTimeInMillis)
    }

    /**
     * Starts the timer for a long break.
     */
    private fun startLongBreak() {
        cycleCount = 0
        isWork = false
        startTimer(longBreakTimeInMillis)
    }

    /**
     * Resets the state of the timer.
     */
    fun resetTimer() {
        countDownTimer?.cancel()
        cycleCount = 0
        isRunningCycle = false
        delegate.onTick(workTimeInMillis)
        isTimerRunning = false
        isWork = false
    }

    /**
     * Updates the times.
     *
     * @param workTime: The time dedicated for work.
     * @param breakTime: The time dedicated for breaks.
     * @param longBreakTime: The time dedicated for long breaks.
     */
    fun updateTimes(workTime: Int, breakTime: Int, longBreakTime: Int, cycles: Int) {
        workTimeInMillis = TimerUtils.minutesToMillis(workTime)
        breakTimeInMillis = TimerUtils.minutesToMillis(breakTime)
        longBreakTimeInMillis = TimerUtils.minutesToMillis(longBreakTime)
        cyclesBeforeLongBreak = cycles
    }

    /**
     * Stops the timer from running.
     */
    fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
    }

    /**
     * Resumes the timer from the time it last stopped.
     */
    fun resumeTimer() {
        startTimer(timeLeft)
    }

}