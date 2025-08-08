package com.rendy.customlockscreen

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        SmartLockApplication.instance.setAdminEnabled(true)
        Toast.makeText(context, "Device Admin enabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        SmartLockApplication.instance.setAdminEnabled(false)
        Toast.makeText(context, "Device Admin disabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "This will disable the Smart Lock Screen functionality"
    }
}
