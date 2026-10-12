package com.rendy.customlockscreen

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        SmartLockApplication.instance.setAdminEnabled(true)
        Toast.makeText(context, context.getString(R.string.device_admin_enabled_toast), Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        SmartLockApplication.instance.setAdminEnabled(false)
        Toast.makeText(context, context.getString(R.string.device_admin_disabled_toast), Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.admin_disable_warning)
    }
}
