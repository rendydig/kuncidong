package com.rendy.customlockscreen.kids

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.rendy.customlockscreen.DeviceAdminLockScreenService
import com.rendy.customlockscreen.DeviceAdminReceiver
import com.rendy.customlockscreen.LockScreenAccessibilityService
import com.rendy.customlockscreen.LockScreenTimerService
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.RemoteControlService
import com.rendy.customlockscreen.SmartLockApplication
import com.rendy.customlockscreen.data.TimerSettings
import com.rendy.customlockscreen.lock.AppLockGate

/**
 * Kids device home. Shows the compact permission setup, the service toggle and
 * the current status, plus shortcuts to pairing and settings.
 */
class KidsHomeActivity : AppCompatActivity() {

    private lateinit var btnEnableAdmin: LinearLayout
    private lateinit var btnOverlayPermission: LinearLayout
    private lateinit var btnAccessibilityService: LinearLayout
    private lateinit var btnStartService: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var tvPermissionProgress: TextView
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
            statusHandler.postDelayed(this, 2000)
        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLockGate.requireUnlock(this, KidsHomeActivity::class.java)) return
        setContentView(R.layout.activity_kids_home)

        initializeViews()
        initializeDeviceAdmin()
        setupClickListeners()
        updateStatus()
        initializeTimerService()
    }

    override fun onResume() {
        super.onResume()
        if (AppLockGate.requireUnlock(this, KidsHomeActivity::class.java)) return
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
        tvStatus = findViewById(R.id.tvStatus)
        tvPermissionProgress = findViewById(R.id.tvPermissionProgress)
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
                Toast.makeText(this, getString(R.string.kids_home_admin_active), Toast.LENGTH_SHORT).show()
            } else {
                requestAdminPermission()
            }
        }

        btnOverlayPermission.setOnClickListener {
            if (hasOverlayPermission()) {
                Toast.makeText(this, getString(R.string.kids_home_overlay_granted), Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }

        btnAccessibilityService.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, getString(R.string.kids_home_accessibility_active), Toast.LENGTH_SHORT).show()
            } else {
                requestAccessibilityService()
            }
        }

        btnStartService.setOnClickListener { switchLockService.toggle() }

        switchLockService.setOnCheckedChangeListener { _, isChecked ->
            if (!areAllPermissionsGranted()) {
                switchLockService.isChecked = false
                Toast.makeText(this, getString(R.string.kids_home_grant_all_permissions), Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            if (isChecked) startServices() else stopServices()
        }

        findViewById<MaterialCardView>(R.id.btnPairing).setOnClickListener {
            startActivity(Intent(this, PairingActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnKidsSettings).setOnClickListener {
            startActivity(Intent(this, KidsSettingsActivity::class.java))
        }
    }

    private fun startServices() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, getString(R.string.kids_home_need_overlay), Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        startForeground(DeviceAdminLockScreenService::class.java)
        RemoteControlService.start(this)
        SmartLockApplication.instance.setServiceEnabled(true)
        Toast.makeText(this, getString(R.string.kids_home_service_started), Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun stopServices() {
        stopService(Intent(this, DeviceAdminLockScreenService::class.java))
        RemoteControlService.stop(this)
        SmartLockApplication.instance.setServiceEnabled(false)
        Toast.makeText(this, getString(R.string.kids_home_service_stopped), Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun startForeground(serviceClass: Class<*>) {
        val intent = Intent(this, serviceClass)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.kids_home_service_start_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun isAdminActive(): Boolean = devicePolicyManager.isAdminActive(adminComponent)

    private fun hasOverlayPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

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
            val serviceName = "$packageName/${LockScreenAccessibilityService::class.java.name}"
            return services?.contains(serviceName) == true
        }
        return false
    }

    private fun requestAccessibilityService() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, getString(R.string.kids_home_accessibility_hint), Toast.LENGTH_LONG).show()
    }

    private fun requestAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.admin_request_explanation)
            )
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    private fun areAllPermissionsGranted(): Boolean =
        isAdminActive() && hasOverlayPermission() && isAccessibilityServiceEnabled()

    private fun isServiceRunning(className: String): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == className }
    }

    private fun updateStatus() {
        switchAdmin.isChecked = isAdminActive()
        switchOverlay.isChecked = hasOverlayPermission()
        switchAccessibility.isChecked = isAccessibilityServiceEnabled()

        val granted = listOf(isAdminActive(), hasOverlayPermission(), isAccessibilityServiceEnabled())
            .count { it }
        tvPermissionProgress.text = getString(R.string.kids_home_permission_progress, granted)

        val allGranted = granted == 3
        btnStartService.alpha = if (allGranted) 1.0f else 0.5f
        btnStartService.isEnabled = true

        val lockRunning = isServiceRunning(DeviceAdminLockScreenService::class.java.name)
        val remoteRunning = isServiceRunning(RemoteControlService::class.java.name)

        val (statusTextRes, statusIconRes, statusColorRes) = when {
            lockRunning && remoteRunning ->
                Triple(R.string.kids_home_status_active, R.drawable.ic_check_circle, R.color.success_text)
            lockRunning || remoteRunning ->
                Triple(R.string.kids_home_status_partial, R.drawable.ic_warning, R.color.warning_text)
            else ->
                Triple(R.string.kids_home_status_inactive_runtime, R.drawable.ic_cancel, R.color.error_text)
        }
        val statusColor = ContextCompat.getColor(this, statusColorRes)
        tvStatus.text = getString(statusTextRes)
        tvStatus.setTextColor(statusColor)
        tvStatus.compoundDrawableTintList = ColorStateList.valueOf(statusColor)
        tvStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(statusIconRes, 0, 0, 0)

        switchLockService.isChecked = lockRunning || remoteRunning
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ENABLE_ADMIN -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, getString(R.string.kids_home_admin_granted), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.kids_home_admin_required), Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (hasOverlayPermission()) {
                    Toast.makeText(this, getString(R.string.kids_home_overlay_granted_toast), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.kids_home_overlay_required), Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
        }
    }

    private fun initializeTimerService() {
        val interval = TimerSettings.getTimerInterval(this)
        if (interval > 0) {
            startService(Intent(this, LockScreenTimerService::class.java))
        }
    }
}
