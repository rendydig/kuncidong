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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnEnableAdmin: LinearLayout
    private lateinit var btnOverlayPermission: LinearLayout
    private lateinit var btnAccessibilityService: LinearLayout
    private lateinit var btnStartService: MaterialCardView
    private lateinit var btnSettings: MaterialCardView
    private lateinit var btnTestLockScreen: MaterialCardView
    private lateinit var btnQuestionManager: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var switchAdmin: Switch
    private lateinit var switchOverlay: Switch
    private lateinit var switchAccessibility: Switch
    private lateinit var switchLockService: Switch
    
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
        initializeTimerService()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
        statusHandler.post(statusUpdateRunnable)
        initializeTimerService() // Restart timer service in case settings changed
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
        btnSettings = findViewById(R.id.btnSettings)
        btnTestLockScreen = findViewById(R.id.btnTestLockScreen)
        btnQuestionManager = findViewById(R.id.btnQuestionManager)
        tvStatus = findViewById(R.id.tvStatus)
        switchAdmin = findViewById(R.id.switchAdmin)
        switchOverlay = findViewById(R.id.switchOverlay)
        switchAccessibility = findViewById(R.id.switchAccessibility)
        switchLockService = findViewById(R.id.switchLockService)
    }
    
    private fun initializeDeviceAdmin() {
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
    }
    
    private fun setupClickListeners() {
        btnEnableAdmin.setOnClickListener {
            if (isAdminActive()) {
                Toast.makeText(this, "Admin perangkat sudah aktif", Toast.LENGTH_SHORT).show()
            } else {
                requestAdminPermission()
            }
        }
        
        btnOverlayPermission.setOnClickListener {
            if (hasOverlayPermission()) {
                Toast.makeText(this, "Izin tampilan over apps sudah diberikan", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }
        
        btnAccessibilityService.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Layanan aksesibilitas sudah aktif", Toast.LENGTH_SHORT).show()
            } else {
                requestAccessibilityService()
            }
        }
        
        btnStartService.setOnClickListener {
            // This card now just shows the switch, clicking the card toggles the switch
            if (areAllPermissionsGranted()) {
                switchLockService.toggle()
            } else {
                Toast.makeText(this, "Harap aktifkan semua izin terlebih dahulu", Toast.LENGTH_LONG).show()
            }
        }
        
        switchLockService.setOnCheckedChangeListener { _, isChecked ->
            if (!areAllPermissionsGranted()) {
                // Revert switch state if permissions not granted
                switchLockService.isChecked = false
                Toast.makeText(this, "Harap aktifkan semua izin terlebih dahulu", Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            
            if (isChecked) {
                startDeviceAdminService()
            } else {
                stopDeviceAdminService()
            }
        }
        
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        btnTestLockScreen.setOnClickListener {
            // Test original lock screen with Material Design improvements
            Toast.makeText(this, "🚀 Testing Lock Screen with Material Design", Toast.LENGTH_SHORT).show()
            
            // Launch original lock screen activity
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        
        btnQuestionManager.setOnClickListener {
            val intent = Intent(this, QuestionManagerActivity::class.java)
            startActivity(intent)
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
        Toast.makeText(this, "Cari 'Kunci Dong' di daftar dan aktifkan", Toast.LENGTH_LONG).show()
    }
    
    private fun requestAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable device admin untuk memungkinkan Smart Lock Screen berfungsi dengan baik")
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }
    
    private fun startLockScreenService() {
        // Check overlay permission first
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Harap berikan izin 'Display over other apps' terlebih dahulu", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Layar Kunci Edukasi dimulai", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Gagal memulai layanan: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopLockScreenService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Layar Kunci Edukasi berhenti", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun startOverlayService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Harap berikan izin 'Display over other apps' terlebih dahulu", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Layar Kunci Edukasi dimulai", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Gagal memulai layanan overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopOverlayService() {
        val serviceIntent = Intent(this, OverlayLockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Layar Kunci Edukasi berhenti", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun isOverlayServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == OverlayLockScreenService::class.java.name }
    }
    
    private fun startDeviceAdminService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Harap berikan izin 'Display over other apps' terlebih dahulu", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Pengontrol Kunci Layar Edukasi dimulai", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Gagal memulai layanan admin perangkat: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }
    
    private fun stopDeviceAdminService() {
        val serviceIntent = Intent(this, DeviceAdminLockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Pengontrol Kunci Layar Edukasi berhenti", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun isDeviceAdminServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == DeviceAdminLockScreenService::class.java.name }
    }
    
    private fun updateStatus() {
        val isServiceRunning = isServiceRunning()
        val isDeviceAdminServiceRunning = isDeviceAdminServiceRunning()
        
        // Update permission switches
        switchAdmin.isChecked = isAdminActive()
        switchOverlay.isChecked = hasOverlayPermission()
        switchAccessibility.isChecked = isAccessibilityServiceEnabled()
        
        // Update main button state
        val allPermissionsGranted = areAllPermissionsGranted()
        btnStartService.alpha = if (allPermissionsGranted) 1.0f else 0.5f
        btnStartService.isEnabled = allPermissionsGranted
        
        val statusText = when {
            isDeviceAdminServiceRunning -> "Layar Kunci Edukatif: Aktif ✅"
            isServiceRunning -> "Layanan Berjalan: Sebagian ⚠️"
            else -> "Layanan: Tidak Aktif ❌"
        }
        
        tvStatus.text = statusText
        
        // Update service switch state
        switchLockService.isChecked = isDeviceAdminServiceRunning
    }
    
    private fun areAllPermissionsGranted(): Boolean {
        return isAdminActive() && hasOverlayPermission() && isAccessibilityServiceEnabled()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_ENABLE_ADMIN -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Admin Perangkat berhasil diaktifkan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Admin Perangkat diperlukan untuk aplikasi ini", Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (hasOverlayPermission()) {
                    Toast.makeText(this, "Izin tampilan di atas aplikasi diberikan! Anda sekarang dapat memulai layanan.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Izin tampilan di atas aplikasi diperlukan untuk layar kunci bekerja", Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
        }
    }
    
    private fun initializeTimerService() {
        // Start timer service if interval is set and not disabled
        val timerInterval = SettingsActivity.getTimerInterval(this)
        if (timerInterval > 0) {
            val intent = Intent(this, LockScreenTimerService::class.java)
            startService(intent)
        }
    }
}
