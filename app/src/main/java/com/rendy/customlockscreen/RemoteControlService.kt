package com.rendy.customlockscreen

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rendy.customlockscreen.lock.LockStateStore
import com.rendy.customlockscreen.net.NsdAdvertiser
import com.rendy.customlockscreen.net.RemoteAuthStore
import com.rendy.customlockscreen.net.RemoteCommandHandler
import com.rendy.customlockscreen.net.RemoteControlServer
import com.rendy.customlockscreen.net.RemoteStatus

/**
 * Foreground service running on the Kids device. It opens the TCP control
 * server and advertises the device over NSD so Parents can pair and lock it.
 */
class RemoteControlService : Service(), RemoteCommandHandler {

    private lateinit var authStore: RemoteAuthStore
    private lateinit var lockStateStore: LockStateStore
    private var server: RemoteControlServer? = null
    private var advertiser: NsdAdvertiser? = null

    override fun onCreate() {
        super.onCreate()
        authStore = RemoteAuthStore(this)
        lockStateStore = LockStateStore(this)
        server = RemoteControlServer(authStore, this)
        advertiser = NsdAdvertiser(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        val currentServer = server
        if (currentServer != null && !currentServer.isRunning) {
            currentServer.start()
            advertiser?.start(authStore.deviceName())
            Log.d(TAG, "Remote control server started as ${authStore.deviceName()}")
        }
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        advertiser?.stop()
        isRunning = false
        Log.d(TAG, "Remote control service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // region RemoteCommandHandler

    override fun onLock(mode: String, title: String?, text: String?, pack: String?): Boolean {
        lockStateStore.lock(mode, title, text, pack)
        val intent = Intent(this, DeviceAdminLockScreenService::class.java).apply {
            action = ACTION_REMOTE_LOCK
            putExtra(EXTRA_MODE, mode)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_PACK, pack)
        }
        deliverToLockService(intent)
        return true
    }

    override fun onUnlock(): Boolean {
        lockStateStore.unlock()
        if (isDeviceAdminServiceRunning()) {
            val intent = Intent(this, DeviceAdminLockScreenService::class.java).apply {
                action = ACTION_REMOTE_UNLOCK
            }
            deliverToLockService(intent)
        }
        return true
    }

    override fun currentStatus(): RemoteStatus {
        val state = lockStateStore.current()
        return RemoteStatus(state.locked, state.mode, batteryLevel())
    }

    // endregion

    private fun batteryLevel(): Int {
        return try {
            val manager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    private fun isDeviceAdminServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == DeviceAdminLockScreenService::class.java.name }
    }

    /**
     * Delivers a command to the lock service. When it already runs (foreground)
     * a plain [startService] is enough, which avoids Android 12+ restrictions on
     * starting foreground services from the background.
     */
    private fun deliverToLockService(intent: Intent) {
        try {
            if (isDeviceAdminServiceRunning()) {
                startService(intent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver ${intent.action}: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.remote_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.remote_channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.remote_notification_title))
            .setContentText(getString(R.string.remote_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "RemoteControlService"
        private const val NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "kuncidong_remote_channel"

        const val ACTION_REMOTE_LOCK = "com.rendy.customlockscreen.REMOTE_LOCK"
        const val ACTION_REMOTE_UNLOCK = "com.rendy.customlockscreen.REMOTE_UNLOCK"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_PACK = "extra_pack"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, RemoteControlService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start remote control service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RemoteControlService::class.java))
        }
    }
}
