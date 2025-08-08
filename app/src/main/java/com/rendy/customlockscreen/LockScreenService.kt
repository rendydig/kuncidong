package com.rendy.customlockscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LockScreenService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val LOCK_SCREEN_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "smart_lock_channel"
        private const val LOCK_SCREEN_CHANNEL_ID = "lock_screen_channel"
    }
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    android.util.Log.d("LockScreenService", "Screen turned OFF")
                }
                Intent.ACTION_USER_PRESENT -> {
                    android.util.Log.d("LockScreenService", "User present - device unlocked")
                }
                Intent.ACTION_SCREEN_ON -> {
                    android.util.Log.d("LockScreenService", "Screen turned ON - trying both notification and direct launch")
                    // Check if device is actually locked before showing our screen
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    if (keyguardManager.isKeyguardLocked) {
                        android.util.Log.d("LockScreenService", "Device is locked - trying multiple approaches")
                        
                        // Try direct activity launch first
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showLockScreen()
                        }, 500)
                        
                        // Also show notification as backup
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showLockScreenNotification()
                        }, 1000)
                    } else {
                        android.util.Log.d("LockScreenService", "Device is not locked - skipping")
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle test action
        if (intent?.action == "TEST_NOTIFICATION") {
            android.util.Log.d("LockScreenService", "Test action received - creating notification")
            showLockScreenNotification()
            return START_NOT_STICKY
        }
        
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            SmartLockApplication.instance.setServiceEnabled(true)
        } catch (e: SecurityException) {
            // Handle permission denial gracefully
            android.util.Log.e("LockScreenService", "Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        SmartLockApplication.instance.setServiceEnabled(false)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Service notification channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Kunci Dong Edukasi Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the smart lock screen active"
                setShowBadge(false)
            }
            
            // Lock screen notification channel
            val lockScreenChannel = NotificationChannel(
                LOCK_SCREEN_CHANNEL_ID,
                "Educational Lock Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows educational questions when device is locked"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(lockScreenChannel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Lock Screen Active")
            .setContentText("Educational lock screen is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        
        // Use RECEIVER_NOT_EXPORTED for Android 14+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }
    
    private fun showLockScreenNotification() {
        android.util.Log.d("LockScreenService", "Creating lock screen notification")
        
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, LOCK_SCREEN_CHANNEL_ID)
            .setContentTitle("🎓 Educational Question Ready!")
            .setContentText("Tap to answer a question and unlock your device")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(LOCK_SCREEN_NOTIFICATION_ID, notification)
        
        android.util.Log.d("LockScreenService", "Lock screen notification created")
    }
    
    private fun showLockScreen() {
        android.util.Log.d("LockScreenService", "showLockScreen() called - attempting to start LockScreenActivity")
        try {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP or
                       Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(intent)
            android.util.Log.d("LockScreenService", "LockScreenActivity started successfully")
        } catch (e: Exception) {
            android.util.Log.e("LockScreenService", "Failed to start LockScreenActivity: ${e.message}")
        }
    }
}
