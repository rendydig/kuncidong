package com.rendy.customlockscreen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.rendy.customlockscreen.lock.AppLock

class SmartLockApplication : Application(), Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val PREFS_NAME = "smart_lock_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_ADMIN_ENABLED = "admin_enabled"
        private const val KEY_ROLE = "app_role"

        /** Role has not been chosen yet - the app shows the role picker. */
        const val ROLE_UNSET = "unset"

        /** Parent device: discovers and locks Kids devices. */
        const val ROLE_PARENT = "parent"

        /** Kids device: hosts the remote control server and the lock screen. */
        const val ROLE_KIDS = "kids"

        lateinit var instance: SmartLockApplication
            private set
    }
    
    private lateinit var sharedPreferences: SharedPreferences

    /** Jumlah aktivitas yang sedang started; dipakai untuk mendeteksi app ditinggalkan. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        registerActivityLifecycleCallbacks(this)
    }
    
    fun isServiceEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SERVICE_ENABLED, false)
    }
    
    fun setServiceEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }
    
    fun isAdminEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ADMIN_ENABLED, false)
    }
    
    fun setAdminEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ADMIN_ENABLED, enabled).apply()
    }

    /** The role chosen on first launch, one of [ROLE_UNSET], [ROLE_PARENT] or [ROLE_KIDS]. */
    fun getRole(): String {
        return sharedPreferences.getString(KEY_ROLE, ROLE_UNSET) ?: ROLE_UNSET
    }

    fun setRole(role: String) {
        sharedPreferences.edit().putString(KEY_ROLE, role).apply()
        AppLock.lock()
    }

    fun hasRole(): Boolean = getRole() != ROLE_UNSET

    // region app lock

    /**
     * Menandai app terkunci saat tidak ada lagi aktivitas yang berjalan
     * (app ditinggalkan). Perubahan konfigurasi (mis. rotasi) diabaikan agar
     * tidak salah mengunci.
     */
    override fun onActivityStarted(activity: Activity) {
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedActivities > 0) startedActivities--
        if (startedActivities == 0 && !activity.isChangingConfigurations) {
            AppLock.lock()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    // endregion
}
