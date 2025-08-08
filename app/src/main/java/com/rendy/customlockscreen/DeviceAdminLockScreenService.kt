package com.rendy.customlockscreen

import android.app.*
import android.app.admin.DevicePolicyManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.data.Question

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
        // Inflate the modern CardView-based layout
        val inflater = LayoutInflater.from(this)
        val overlayView = inflater.inflate(R.layout.activity_lock_screen, null)
        
        // Find views and populate with question data
        val tvCategory = overlayView.findViewById<TextView>(R.id.tvCategory)
        val tvQuestion = overlayView.findViewById<TextView>(R.id.tvQuestion)
        val tvFeedback = overlayView.findViewById<TextView>(R.id.tvFeedback)
        
        val btnAnswer1 = overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer1)
        val btnAnswer2 = overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer2)
        val btnAnswer3 = overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer3)
        val btnAnswer4 = overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer4)
        
        val tvAnswer1 = overlayView.findViewById<TextView>(R.id.tvAnswer1)
        val tvAnswer2 = overlayView.findViewById<TextView>(R.id.tvAnswer2)
        val tvAnswer3 = overlayView.findViewById<TextView>(R.id.tvAnswer3)
        val tvAnswer4 = overlayView.findViewById<TextView>(R.id.tvAnswer4)
        
        val answerButtons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        val answerTextViews = listOf(tvAnswer1, tvAnswer2, tvAnswer3, tvAnswer4)
        
        // Populate question data
        tvCategory.text = question.category.displayName
        tvQuestion.text = question.questionText
        tvFeedback.visibility = View.GONE
        
        // Set answer options
        question.options.forEachIndexed { index, option ->
            answerTextViews[index].text = option
        }
        
        // Set click listeners
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswer(index == question.correctAnswerIndex)
            }
        }
        
        return overlayView
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
