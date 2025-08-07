package com.dishtech.pomodoroautoalarm

/**
 * Object providing utils related to time.
 */
object TimerUtils {

    /**
     * Converts milliseconds to minutes.
     *
     * @param millis: number of milliseconds.
     */
    fun minutesPartOfMillis(millis: Long): Int {
        return (millis / (60 * 1000L)).toInt()
    }

    /**
     * Returns only the seconds part
     *
     * @param millis: number of milliseconds.
     */
    fun secondsPartOfMillis(millis: Long): Int {
        return (millis / 1000).toInt() % 60
    }

    /**
     * Converts the given minutes to milliseconds.
     *
     * @param minutes: Minutes to be converted.
     */
    fun minutesToMillis(minutes: Int): Long {
        return minutes * 60 * 1000L
    }
}