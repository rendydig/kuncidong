package com.rendy.customlockscreen

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnEnableAdmin: Button
    private lateinit var btnOverlayPermission: Button
    private lateinit var btnAccessibilityService: Button
    private lateinit var btnStartService: Button
    private lateinit var btnTestLockScreen: Button
    private lateinit var tvStatus: TextView
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            statusHandler.postDelayed(this, 2000) // Update every 2 seconds
        }
    }
    
    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 2
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeDeviceAdmin()
        setupClickListeners()
        updateStatus()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
        statusHandler.post(statusUpdateRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        statusHandler.removeCallbacks(statusUpdateRunnable)
    }
    
    private fun initializeViews() {
        btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)
        btnAccessibilityService = findViewById(R.id.btnAccessibilityService)
        btnStartService = findViewById(R.id.btnStartService)
        btnTestLockScreen = findViewById(R.id.btnTestLockScreen)
        tvStatus = findViewById(R.id.tvStatus)
    }
    
    private fun initializeDeviceAdmin() {
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
    }
    
    private fun setupClickListeners() {
        btnEnableAdmin.setOnClickListener {
            if (isAdminActive()) {
                Toast.makeText(this, "Device Admin is already enabled", Toast.LENGTH_SHORT).show()
            } else {
                requestAdminPermission()
            }
        }
        
        btnOverlayPermission.setOnClickListener {
            if (hasOverlayPermission()) {
                Toast.makeText(this, "Display over other apps permission is already granted", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }
        
        btnAccessibilityService.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Accessibility service is already enabled", Toast.LENGTH_SHORT).show()
            } else {
                requestAccessibilityService()
            }
        }
        
        btnStartService.setOnClickListener {
            if (!isAdminActive()) {
                Toast.makeText(this, "Please enable Device Admin first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!hasOverlayPermission()) {
                Toast.makeText(this, "Please grant Display Over Apps permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isDeviceAdminServiceRunning()) {
                stopDeviceAdminService()
            } else {
                startDeviceAdminService()
            }
        }
        
        btnTestLockScreen.setOnClickListener {
            // Test both notification and direct activity launch
            Toast.makeText(this, "Testing: Creating notification + launching activity", Toast.LENGTH_SHORT).show()
            
            // Test notification
            val serviceIntent = Intent(this, LockScreenService::class.java)
            serviceIntent.action = "TEST_NOTIFICATION"
            startService(serviceIntent)
            
            // Test direct activity launch
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, LockScreenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP or
                           Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }, 1000)
        }
    }
    
    private fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (LockScreenService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val serviceName = "${packageName}/${LockScreenAccessibilityService::class.java.name}"
            return services?.contains(serviceName) == true
        }
        return false
    }
    
    private fun requestAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Find 'Smart Lock Screen' in the list and enable it", Toast.LENGTH_LONG).show()
    }
    
    private fun requestAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable device admin to allow Smart Lock Screen to function properly")
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }
    
    private fun startLockScreenService() {
        // Check overlay permission first
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        
        try {
            val serviceIntent = Intent(this, LockScreenService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Lock Screen Service started", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopLockScreenService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Lock Screen Service stopped", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun startOverlayService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        
        try {
            val serviceIntent = Intent(this, OverlayLockScreenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Educational Lock Screen Overlay started", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to start overlay service: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopOverlayService() {
        val serviceIntent = Intent(this, OverlayLockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Educational Lock Screen Overlay stopped", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun isOverlayServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == OverlayLockScreenService::class.java.name }
    }
    
    private fun startDeviceAdminService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        
        try {
            val serviceIntent = Intent(this, DeviceAdminLockScreenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Educational Lock Screen Controller started", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to start device admin service: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopDeviceAdminService() {
        val serviceIntent = Intent(this, DeviceAdminLockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Educational Lock Screen Controller stopped", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun isDeviceAdminServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == DeviceAdminLockScreenService::class.java.name }
    }
    
    private fun updateStatus() {
        val adminActive = isAdminActive()
        val overlayPermission = hasOverlayPermission()
        val deviceAdminServiceRunning = isDeviceAdminServiceRunning()
        
        val statusText = buildString {
            appendLine("📱 Educational Lock Screen Controller:")
            appendLine()
            appendLine("🔐 Device Admin: ${if (adminActive) "✅ Enabled" else "❌ Disabled"}")
            appendLine("🖥️ Display Over Apps: ${if (overlayPermission) "✅ Granted" else "❌ Not Granted"}")
            appendLine("🎯 Lock Controller: ${if (deviceAdminServiceRunning) "✅ Running" else "❌ Stopped"}")
            appendLine()
            when {
                !adminActive -> appendLine("⚠️ Enable Device Admin to continue")
                !overlayPermission -> appendLine("⚠️ Grant Display Over Apps permission")
                !deviceAdminServiceRunning -> appendLine("⚠️ Start the Educational Lock Screen Controller")
                else -> appendLine("🎉 Ready! Your device will show educational questions")
            }
            appendLine()
            if (deviceAdminServiceRunning) {
                appendLine("🔒 Device will be locked when screen turns on")
                appendLine("📚 Answer questions correctly to unlock")
                appendLine("🚨 Emergency unlock available if needed")
            } else {
                appendLine("💡 This service controls device locking and shows")
                appendLine("   educational questions that must be answered")
                appendLine("   correctly before the device can be unlocked.")
            }
        }
        
        tvStatus.text = statusText
        
        // Update service button text
        btnStartService.text = if (deviceAdminServiceRunning) {
            "🛑 Stop Educational Lock Controller"
        } else {
            "▶️ Start Educational Lock Controller"
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_ENABLE_ADMIN -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Device Admin enabled successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Device Admin is required for the app to work", Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (hasOverlayPermission()) {
                    Toast.makeText(this, "Overlay permission granted! You can now start the service.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "'Display over other apps' permission is required for the lock screen to work", Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
        }
    }
}
