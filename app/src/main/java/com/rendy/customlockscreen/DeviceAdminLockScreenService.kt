package com.rendy.customlockscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.rendy.customlockscreen.data.Question
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.kids.KidsSettingsActivity
import com.rendy.customlockscreen.lock.LockStateStore
import com.rendy.customlockscreen.net.LockMode
import com.rendy.customlockscreen.net.RemoteAuthStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Device admin service that performs the actual locking. It reacts to the local
 * timer and to remote commands delivered by [RemoteControlService], then shows
 * the quiz or text overlay depending on the requested mode.
 */
class DeviceAdminLockScreenService : Service() {

    companion object {
        private const val TAG = "DeviceAdminLockScreen"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "device_admin_lock_screen_channel"
        private const val DEFAULT_TEXT = "Waktunya istirahat dari HP ya 😊"
        private const val TRIGGER_LOCK_SCREEN_NOW = "com.rendy.customlockscreen.TRIGGER_LOCK_SCREEN_NOW"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var devicePolicyManager: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    private lateinit var questionRepository: QuestionRepository
    private lateinit var lockStateStore: LockStateStore
    private lateinit var authStore: RemoteAuthStore
    private var currentQuestion: Question? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    if (keyguardManager.isKeyguardLocked) {
                        lockAndShow(defaultMode(), null, null)
                    }
                }

                Intent.ACTION_SCREEN_OFF -> hideOverlay()

                Intent.ACTION_USER_PRESENT -> {
                    // Unlocking is controlled by this service, not the keyguard.
                }

                TRIGGER_LOCK_SCREEN_NOW -> lockAndShow(defaultMode(), null, null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        questionRepository = QuestionRepository(this)
        lockStateStore = LockStateStore(this)
        authStore = RemoteAuthStore(this)

        createNotificationChannel()
        registerScreenReceiver()

        serviceScope.launch { questionRepository.initializeQuestions() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            "TIMER_TRIGGER" -> lockAndShow(defaultMode(), null, null)

            RemoteControlService.ACTION_REMOTE_LOCK -> {
                val mode = intent.getStringExtra(RemoteControlService.EXTRA_MODE) ?: LockMode.QUIZ
                lockAndShow(
                    mode,
                    intent.getStringExtra(RemoteControlService.EXTRA_TITLE),
                    intent.getStringExtra(RemoteControlService.EXTRA_TEXT)
                )
            }

            RemoteControlService.ACTION_REMOTE_UNLOCK -> {
                hideOverlay()
                lockStateStore.unlock()
                showToast("🔓 Orang tua membuka kunci")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun defaultMode(): String = KidsSettingsActivity.getDefaultMode(this)

    // region locking

    private fun lockAndShow(mode: String, title: String?, text: String?) {
        lockStateStore.lock(mode, title, text, null)

        val admin = adminComponent
        if (admin != null && devicePolicyManager?.isAdminActive(admin) == true) {
            try {
                devicePolicyManager?.lockNow()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to lock device: ${e.message}")
            }
        } else {
            Log.w(TAG, "Device admin not active - showing overlay only")
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showOverlay(mode, title, text)
        }, 300)
    }

    private fun showOverlay(mode: String, title: String?, text: String?) {
        if (isOverlayShowing || !Settings.canDrawOverlays(this)) return

        if (mode == LockMode.TEXT) {
            overlayView = createTextOverlayView(title, text)
            isOverlayShowing = true
            try {
                windowManager?.addView(overlayView, createOverlayParams())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show text overlay: ${e.message}")
                isOverlayShowing = false
            }
            return
        }

        serviceScope.launch {
            val question = questionRepository.getRandomQuestion()
            if (question == null) {
                Log.w(TAG, "No question available")
                return@launch
            }
            currentQuestion = question
            overlayView = createQuizOverlayView(question)
            try {
                windowManager?.addView(overlayView, createOverlayParams())
                isOverlayShowing = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show quiz overlay: ${e.message}")
                isOverlayShowing = false
            }
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${e.message}")
        }
        overlayView = null
        isOverlayShowing = false
    }

    private fun createQuizOverlayView(question: Question): View {
        val overlayView = LayoutInflater.from(this).inflate(R.layout.activity_lock_screen, null)

        val tvCategory = overlayView.findViewById<TextView>(R.id.tvCategory)
        val tvQuestion = overlayView.findViewById<TextView>(R.id.tvQuestion)
        val tvFeedback = overlayView.findViewById<TextView>(R.id.tvFeedback)

        val answerButtons = listOf(
            overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer1),
            overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer2),
            overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer3),
            overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.btnAnswer4)
        )
        val answerTextViews = listOf(
            overlayView.findViewById<TextView>(R.id.tvAnswer1),
            overlayView.findViewById<TextView>(R.id.tvAnswer2),
            overlayView.findViewById<TextView>(R.id.tvAnswer3),
            overlayView.findViewById<TextView>(R.id.tvAnswer4)
        )

        tvCategory.text = question.category.displayName
        tvQuestion.text = question.questionText
        tvFeedback.visibility = View.GONE

        question.options.forEachIndexed { index, option ->
            answerTextViews.getOrNull(index)?.text = option
        }

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleAnswer(index == question.correctAnswerIndex) }
        }

        return overlayView
    }

    private fun createTextOverlayView(title: String?, text: String?): View {
        val overlayView = LayoutInflater.from(this).inflate(R.layout.activity_lock_screen_text, null)

        val tvTitle = overlayView.findViewById<TextView>(R.id.tvTextTitle)
        val tvMessage = overlayView.findViewById<TextView>(R.id.tvTextMessage)
        val etPin = overlayView.findViewById<EditText>(R.id.etUnlockPin)

        tvTitle.text = title ?: "Pesan dari Orang Tua"
        tvMessage.text = text ?: DEFAULT_TEXT

        overlayView.findViewById<Button>(R.id.btnPinUnlock).setOnClickListener {
            val pin = etPin.text?.toString()?.trim().orEmpty()
            when {
                pin.isEmpty() -> showToast("Masukkan PIN dulu")
                authStore.verifyPin(pin) -> {
                    hideOverlay()
                    lockStateStore.unlock()
                    showToast("🔓 Terbuka")
                }
                else -> {
                    etPin.setText("")
                    showToast("PIN salah")
                }
            }
        }

        overlayView.findViewById<Button>(R.id.btnWaitParent).setOnClickListener {
            showToast("Minta orang tua membuka dari HP mereka")
        }

        return overlayView
    }

    private fun createOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
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
        if (isCorrect) {
            hideOverlay()
            lockStateStore.unlock()
            showToast("✅ Benar! Perangkat terbuka")
        } else {
            showToast("❌ Salah. Coba lagi!")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                hideOverlay()
                lockAndShow(LockMode.QUIZ, null, null)
            }, 1500)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // endregion

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(TRIGGER_LOCK_SCREEN_NOW)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengontrol Kunci Layar Edukasi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls device locking for educational purposes"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Pengontrol Kunci Layar Edukasi Aktif")
            .setContentText("Jawab pertanyaan atau masukkan PIN untuk membuka")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        hideOverlay()
        serviceScope.cancel()
    }
}
