package com.rendy.customlockscreen.data

import android.content.Context

/** Persisted automatic lock interval, shared by the timer service and settings UI. */
object TimerSettings {

    private const val PREFS_NAME = "LockScreenTimerPrefs"
    private const val KEY_TIMER_INTERVAL = "timer_interval"

    const val INTERVAL_NEVER = 0
    const val INTERVAL_1_MIN = 1
    const val INTERVAL_3_MIN = 3
    const val INTERVAL_5_MIN = 5
    const val INTERVAL_10_MIN = 10
    const val INTERVAL_15_MIN = 15

    fun getTimerInterval(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TIMER_INTERVAL, INTERVAL_15_MIN)
    }

    fun setTimerInterval(context: Context, interval: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TIMER_INTERVAL, interval)
            .apply()
    }
}
