package com.rendy.customlockscreen

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*

class LockScreenTimerService : Service() {
    
    private lateinit var alarmManager: AlarmManager
    
    companion object {
        private const val TAG = "LockScreenTimerService"
        private const val PREFS_NAME = "LockScreenTimerPrefs"
        private const val KEY_NEXT_TRIGGER_TIME = "next_trigger_time"
        private const val ACTION_TRIGGER_LOCK_SCREEN = "com.rendy.customlockscreen.TRIGGER_LOCK_SCREEN"
        
        fun getNextTriggerTime(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_NEXT_TRIGGER_TIME, 0)
        }
        
        private fun setNextTriggerTime(context: Context, time: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_NEXT_TRIGGER_TIME, time).apply()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "LockScreenTimerService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LockScreenTimerService started")
        
        when (intent?.action) {
            ACTION_TRIGGER_LOCK_SCREEN -> {
                triggerLockScreen()
                scheduleNextAlarm()
            }
            else -> {
                scheduleNextAlarm()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelAlarm()
        Log.d(TAG, "LockScreenTimerService destroyed")
    }
    
    private fun scheduleNextAlarm() {
        val interval = SettingsActivity.getTimerInterval(this)
        
        if (interval <= 0) {
            Log.d(TAG, "Timer disabled, not scheduling alarm")
            return
        }
        
        val nextTriggerTime = System.currentTimeMillis() + (interval * 60 * 1000L)
        setNextTriggerTime(this, nextTriggerTime)
        
        val intent = Intent(this, LockScreenTimerService::class.java).apply {
            action = ACTION_TRIGGER_LOCK_SCREEN
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
            
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nextTriggerTime
            }
            
            Log.d(TAG, "Next lock screen scheduled for: ${calendar.time} (in $interval minutes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }
    
    private fun cancelAlarm() {
        val intent = Intent(this, LockScreenTimerService::class.java).apply {
            action = ACTION_TRIGGER_LOCK_SCREEN
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        setNextTriggerTime(this, 0)
        Log.d(TAG, "Alarm cancelled")
    }
    
    private fun triggerLockScreen() {
        Log.d(TAG, "Triggering lock screen via DeviceAdminLockScreenService")
        
        try {
            // First, check if DeviceAdminLockScreenService is running
            val isDeviceAdminServiceRunning = isDeviceAdminServiceRunning()
            
            if (isDeviceAdminServiceRunning) {
                // Service is running, trigger lock screen directly via broadcast
                val triggerIntent = Intent("com.rendy.customlockscreen.TRIGGER_LOCK_SCREEN_NOW")
                triggerIntent.setPackage(packageName)
                sendBroadcast(triggerIntent)
                Log.d(TAG, "Broadcast sent to trigger lock screen")
            } else {
                // Service not running, start it first then trigger
                val serviceIntent = Intent(this, DeviceAdminLockScreenService::class.java)
                serviceIntent.action = "TIMER_TRIGGER"
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Log.d(TAG, "DeviceAdminLockScreenService started with timer trigger")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger DeviceAdminLockScreenService", e)
            
            // Fallback 1: try direct activity launch
            try {
                val lockScreenIntent = Intent(this, LockScreenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP or
                           Intent.FLAG_ACTIVITY_SINGLE_TOP or
                           Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
                startActivity(lockScreenIntent)
                Log.d(TAG, "Lock screen activity started as fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start lock screen activity", e2)
                
                // Fallback 2: try overlay service
                try {
                    val overlayIntent = Intent(this, OverlayLockScreenService::class.java)
                    startService(overlayIntent)
                    Log.d(TAG, "Overlay lock screen service started as final fallback")
                } catch (e3: Exception) {
                    Log.e(TAG, "All fallback methods failed", e3)
                }
            }
        }
    }
    
    private fun isDeviceAdminServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return activityManager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == DeviceAdminLockScreenService::class.java.name }
    }
}
