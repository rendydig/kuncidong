package com.rendy.customlockscreen

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class SmartLockApplication : Application() {
    
    companion object {
        private const val PREFS_NAME = "smart_lock_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_ADMIN_ENABLED = "admin_enabled"
        
        lateinit var instance: SmartLockApplication
            private set
    }
    
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
}
