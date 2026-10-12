package com.rendy.customlockscreen.lock

import android.content.Context
import android.content.SharedPreferences
import com.rendy.customlockscreen.net.LockMode

/**
 * Snapshot of the current lock state. Persisted in [SharedPreferences] so every
 * service (remote control, device admin, timer) observes the same state.
 */
data class LockState(
    val locked: Boolean,
    val mode: String,
    val title: String?,
    val text: String?,
    val pack: String?
)

/** Single source of truth for "is the educational lock active?" across services. */
class LockStateStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lock(mode: String, title: String?, text: String?, pack: String?) {
        prefs.edit()
            .putBoolean(KEY_LOCKED, true)
            .putString(KEY_MODE, mode)
            .putString(KEY_TITLE, title)
            .putString(KEY_TEXT, text)
            .putString(KEY_PACK, pack)
            .apply()
    }

    fun unlock() {
        prefs.edit().putBoolean(KEY_LOCKED, false).apply()
    }

    fun isLocked(): Boolean = prefs.getBoolean(KEY_LOCKED, false)

    fun current(): LockState = LockState(
        locked = prefs.getBoolean(KEY_LOCKED, false),
        mode = prefs.getString(KEY_MODE, LockMode.QUIZ) ?: LockMode.QUIZ,
        title = prefs.getString(KEY_TITLE, null),
        text = prefs.getString(KEY_TEXT, null),
        pack = prefs.getString(KEY_PACK, null)
    )

    companion object {
        private const val PREFS_NAME = "kuncidong_lock_state"
        private const val KEY_LOCKED = "locked"
        private const val KEY_MODE = "mode"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_PACK = "pack"
    }
}
