package com.rendy.customlockscreen

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.data.Question
import java.text.SimpleDateFormat
import java.util.*

class OverlayLockScreenService : Service() {
    
    companion object {
        private const val TAG = "OverlayLockScreen"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_lock_screen_channel"
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private lateinit var questionRepository: QuestionRepository
    private var currentQuestion: Question? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen turned ON")
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (keyguardManager.isKeyguardLocked) {
                        Log.d(TAG, "Device is locked - showing overlay")
                        showOverlay()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned OFF - hiding overlay")
                    hideOverlay()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User unlocked device - hiding overlay")
                    hideOverlay()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayLockScreenService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_description)
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
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
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
        
        // Use RECEIVER_NOT_EXPORTED for Android 14+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }
    
    private fun showOverlay() {
        if (isOverlayShowing) return
        
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }
        
        serviceScope.launch {
            try {
                val question = questionRepository.getRandomQuestion()
                if (question != null) {
                    currentQuestion = question
                    overlayView = createOverlayView(question)
                    val params = createOverlayParams()
                    windowManager?.addView(overlayView, params)
                    isOverlayShowing = true
                    Log.d(TAG, "Overlay shown")
                } else {
                    Log.w(TAG, "No question available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay: ${e.message}")
            }
        }
    }
    
    private fun hideOverlay() {
        if (!isOverlayShowing || overlayView == null) return
        
        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            isOverlayShowing = false
            Log.d(TAG, "Overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${e.message}")
        }
    }
    
    private fun createOverlayView(question: Question): View {
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.lock_screen_background))
            setPadding(48, 48, 48, 48)
            
            // Time display
            addView(TextView(this@OverlayLockScreenService).apply {
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.glyph_hero))
                setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.text_primary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
            })
            
            // Date display
            addView(TextView(this@OverlayLockScreenService).apply {
                text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date())
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_title))
                setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.text_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 64 }
            })
            
            // Category badge
            addView(TextView(this@OverlayLockScreenService).apply {
                text = question.category.name
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
                setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.text_primary))
                gravity = Gravity.CENTER
                setPadding(24, 8, 24, 8)
                setBackgroundResource(R.drawable.category_badge)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 32 
                }
            })
            
            // Question text
            addView(TextView(this@OverlayLockScreenService).apply {
                text = question.questionText
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_title))
                setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.text_primary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
            })
            
            // Answer buttons
            question.options.forEachIndexed { index, option ->
                addView(Button(this@OverlayLockScreenService).apply {
                    text = option
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_heading))
                    setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.white))
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
            
            // Skip button
            addView(Button(this@OverlayLockScreenService).apply {
                text = getString(R.string.overlay_skip_question)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
                setTextColor(ContextCompat.getColor(this@OverlayLockScreenService, R.color.text_secondary))
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                
                setOnClickListener {
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
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        )
    }
    
    private fun handleAnswer(isCorrect: Boolean) {
        Log.d(TAG, "Answer selected: ${if (isCorrect) "Correct" else "Incorrect"}")
        
        if (isCorrect) {
            // Correct answer - hide overlay and allow unlock
            hideOverlay()
        } else {
            // Incorrect answer - show new question
            hideOverlay()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                showOverlay()
            }, 500)
        }
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
