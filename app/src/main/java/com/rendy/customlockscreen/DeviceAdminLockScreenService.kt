package com.rendy.customlockscreen

import android.app.*
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.data.Question
import java.text.SimpleDateFormat
import java.util.*

class DeviceAdminLockScreenService : Service() {
    
    companion object {
        private const val TAG = "DeviceAdminLockScreen"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "device_admin_lock_screen_channel"
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var devicePolicyManager: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    private lateinit var questionRepository: QuestionRepository
    private var currentQuestion: Question? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen turned ON")
                    // Small delay to ensure keyguard is ready
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        if (keyguardManager.isKeyguardLocked) {
                            Log.d(TAG, "Device is locked - showing educational lock screen")
                            lockDeviceAndShowQuestion()
                        }
                    }, 200)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned OFF - hiding overlay")
                    hideOverlay()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User present - but we control unlocking now")
                    // Don't hide overlay on user present - we control when to unlock
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceAdminLockScreenService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        questionRepository = QuestionRepository(this)
        
        createNotificationChannel()
        registerScreenReceiver()
        
        // Initialize questions on first run
        serviceScope.launch {
            questionRepository.initializeQuestions()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Educational Lock Screen Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls device locking for educational purposes"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Educational Lock Screen Active")
            .setContentText("Answer questions to unlock your device")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }
    
    private fun lockDeviceAndShowQuestion() {
        // First, programmatically lock the device using Device Admin
        val admin = adminComponent
        if (admin != null && devicePolicyManager?.isAdminActive(admin) == true) {
            try {
                devicePolicyManager?.lockNow()
                Log.d(TAG, "Device locked programmatically")
                
                // Small delay then show our educational overlay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showEducationalOverlay()
                }, 300)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to lock device: ${e.message}")
                // Fallback to just showing overlay
                showEducationalOverlay()
            }
        } else {
            Log.w(TAG, "Device admin not active - showing overlay only")
            showEducationalOverlay()
        }
    }
    
    private fun showEducationalOverlay() {
        if (isOverlayShowing || !Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Cannot show overlay - already showing or no permission")
            return
        }
        
        serviceScope.launch {
            try {
                val question = questionRepository.getRandomQuestion()
                if (question != null) {
                    currentQuestion = question
                    overlayView = createEducationalOverlayView(question)
                    val params = createOverlayParams()
                    
                    windowManager?.addView(overlayView, params)
                    isOverlayShowing = true
                    Log.d(TAG, "Educational overlay shown successfully")
                } else {
                    Log.w(TAG, "No question available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show educational overlay: ${e.message}")
            }
        }
    }
    
    private fun hideOverlay() {
        if (!isOverlayShowing || overlayView == null) return
        
        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            isOverlayShowing = false
            Log.d(TAG, "Educational overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${e.message}")
        }
    }
    
    private fun createEducationalOverlayView(question: Question): View {
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.lock_screen_background))
            setPadding(48, 48, 48, 48)
            
            // Lock icon and title
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = "🔒 Educational Lock Screen"
                textSize = 24f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.white))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
            })
            
            // Time display
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                textSize = 48f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.white))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            })
            
            // Date display
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date())
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.text_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 48 }
            })
            
            // Instruction text
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = "Answer the question below to unlock your device:"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.text_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
            })
            
            // Category badge
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = question.category.name
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.white))
                gravity = Gravity.CENTER
                setPadding(24, 8, 24, 8)
                setBackgroundResource(R.drawable.category_badge)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 24 
                }
            })
            
            // Question text
            addView(TextView(this@DeviceAdminLockScreenService).apply {
                text = question.questionText
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.white))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
            })
            
            // Answer buttons
            question.options.forEachIndexed { index, option ->
                addView(Button(this@DeviceAdminLockScreenService).apply {
                    text = option
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.white))
                    setBackgroundResource(R.drawable.answer_button_selector)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 16 }
                    
                    setOnClickListener {
                        handleAnswer(index == question.correctAnswerIndex)
                    }
                })
            }
            
            // Emergency unlock button (for safety)
            addView(Button(this@DeviceAdminLockScreenService).apply {
                text = "Emergency Unlock (Skip Question)"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@DeviceAdminLockScreenService, R.color.text_hint))
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
                
                setOnClickListener {
                    // Allow emergency unlock
                    hideOverlay()
                }
            })
        }
    }
    
    private fun createOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
    }
    
    private fun handleAnswer(isCorrect: Boolean) {
        Log.d(TAG, "Answer selected: ${if (isCorrect) "Correct" else "Incorrect"}")
        
        if (isCorrect) {
            // Correct answer - hide overlay and allow normal unlock
            hideOverlay()
            
            // Show success message briefly
            showSuccessMessage()
            
        } else {
            // Incorrect answer - keep device locked and show new question
            showIncorrectFeedback()
            
            // Re-lock device and show new question after delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                hideOverlay()
                lockDeviceAndShowQuestion()
            }, 1500)
        }
    }
    
    private fun showSuccessMessage() {
        // Briefly show success before allowing unlock
        android.widget.Toast.makeText(this, "✅ Correct! Device unlocked", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showIncorrectFeedback() {
        // Show incorrect feedback
        android.widget.Toast.makeText(this, "❌ Incorrect. Try again!", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        
        hideOverlay()
        serviceScope.cancel()
    }
}
