package com.rendy.customlockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Start the lock screen service if it was previously enabled
            if (SmartLockApplication.instance.isServiceEnabled()) {
                try {
                    val serviceIntent = Intent(context, LockScreenService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("BootReceiver", "Failed to start service: ${e.message}")
                }
            }
        }
    }
}
