package com.rendy.customlockscreen

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class LockScreenAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "LockScreenAccessibility"
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d(TAG, "Accessibility event: ${event.eventType}, package: ${event.packageName}, class: ${event.className}")
            
            // Detect lock screen related events
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(event)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
            }
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        Log.d(TAG, "Window state changed - Package: $packageName, Class: $className")
        
        // Detect various lock screen indicators
        when {
            // System UI lock screen
            packageName == "com.android.systemui" && (
                className?.contains("Keyguard") == true ||
                className?.contains("LockScreen") == true ||
                className?.contains("StatusBar") == true
            ) -> {
                Log.d(TAG, "System lock screen detected")
                showEducationalLockScreen()
            }
            
            // ColorOS/OPPO specific lock screen
            packageName == "com.coloros.keyguard" ||
            packageName == "com.oppo.lockscreen" ||
            className?.contains("LockScreen") == true -> {
                Log.d(TAG, "ColorOS lock screen detected")
                showEducationalLockScreen()
            }
            
            // Generic lock screen detection
            className?.contains("keyguard", ignoreCase = true) == true ||
            className?.contains("lockscreen", ignoreCase = true) == true -> {
                Log.d(TAG, "Generic lock screen detected")
                showEducationalLockScreen()
            }
        }
    }
    
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Monitor for lock screen content changes
        val packageName = event.packageName?.toString()
        
        if (packageName == "com.android.systemui" || 
            packageName == "com.coloros.keyguard" ||
            packageName == "com.oppo.lockscreen") {
            
            // Check if this is a lock screen unlock attempt
            val text = event.text?.toString()
            if (text?.contains("unlock", ignoreCase = true) == true ||
                text?.contains("pattern", ignoreCase = true) == true ||
                text?.contains("pin", ignoreCase = true) == true) {
                
                Log.d(TAG, "Lock screen unlock attempt detected")
                showEducationalLockScreen()
            }
        }
    }
    
    private fun showEducationalLockScreen() {
        Log.d(TAG, "Attempting to show educational lock screen")
        
        try {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP or
                       Intent.FLAG_ACTIVITY_NO_ANIMATION or
                       Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            
            startActivity(intent)
            Log.d(TAG, "Educational lock screen activity started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start educational lock screen: ${e.message}")
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }
}
