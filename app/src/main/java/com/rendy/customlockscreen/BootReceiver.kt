package com.rendy.customlockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        if (!SmartLockApplication.instance.isServiceEnabled()) return
        if (SmartLockApplication.instance.getRole() != SmartLockApplication.ROLE_KIDS) return

        startForeground(context, DeviceAdminLockScreenService::class.java)
        RemoteControlService.start(context)
    }

    private fun startForeground(context: Context, serviceClass: Class<*>) {
        try {
            val serviceIntent = Intent(context, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: SecurityException) {
            Log.e("BootReceiver", "Failed to start ${serviceClass.simpleName}: ${e.message}")
        }
    }
}
