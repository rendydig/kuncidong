package com.rendy.customlockscreen.lock

import android.app.Activity
import android.content.Intent

/**
 * Gerbang PIN khusus aplikasi.
 *
 * Bila PIN app sudah dibuat dan sesi belum terbuka, [activity] diarahkan ke
 * [AppLockActivity] lalu ditutup.
 */
object AppLockGate {

    /**
     * @return `true` bila [activity] dialihkan ke layar PIN; pemanggil harus
     *   berhenti dan tidak melanjutkan (mis. `return` dari `onCreate`/`onResume`).
     */
    fun requireUnlock(activity: Activity, targetClass: Class<*>): Boolean {
        if (!AppPinStore(activity).hasPin() || AppLock.isUnlocked()) return false

        activity.startActivity(
            Intent(activity, AppLockActivity::class.java)
                .putExtra(AppLockActivity.EXTRA_TARGET_CLASS, targetClass.name)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        activity.finish()
        return true
    }
}
