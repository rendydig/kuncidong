package com.rendy.customlockscreen

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rendy.customlockscreen.data.Question
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.kids.KidsSettingsActivity
import com.rendy.customlockscreen.lock.LockStateStore
import com.rendy.customlockscreen.net.LockMode
import com.rendy.customlockscreen.net.RemoteAuthStore
import kotlinx.coroutines.launch

/**
 * Educational lock screen supporting two modes:
 * - [LockMode.QUIZ]: answer a question to unlock.
 * - [LockMode.TEXT]: read a parent message and unlock with the secret PIN, or
 *   let the parent unlock remotely.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var lockStateStore: LockStateStore
    private lateinit var authStore: RemoteAuthStore
    private lateinit var questionRepository: QuestionRepository

    private var mode: String = LockMode.QUIZ

    // Quiz views
    private var tvCategory: TextView? = null
    private var tvQuestion: TextView? = null
    private var tvFeedback: TextView? = null
    private var answerButtons: List<CardView> = emptyList()
    private var answerTextViews: List<TextView> = emptyList()
    private var currentQuestion: Question? = null

    // Text mode views
    private var etUnlockPin: EditText? = null

    private val remoteUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == RemoteControlService.ACTION_REMOTE_UNLOCK) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(2)

        applyLockWindowFlags()

        lockStateStore = LockStateStore(this)
        authStore = RemoteAuthStore(this)
        questionRepository = QuestionRepository(this)

        mode = resolveMode()

        if (mode == LockMode.TEXT) {
            setContentView(R.layout.activity_lock_screen_text)
            setupTextMode()
        } else {
            setContentView(R.layout.activity_lock_screen)
            setupQuizMode()
        }

        registerRemoteUnlockReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(remoteUnlockReceiver)
        } catch (e: Exception) {
            // Already unregistered.
        }
    }

    private fun applyLockWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
    }

    private fun resolveMode(): String {
        intent.getStringExtra(RemoteControlService.EXTRA_MODE)?.let { return it }
        val state = lockStateStore.current()
        return if (state.locked) state.mode else KidsSettingsActivity.getDefaultMode(this)
    }

    private fun currentTitle(): String? =
        intent.getStringExtra(RemoteControlService.EXTRA_TITLE) ?: lockStateStore.current().title

    private fun currentText(): String? =
        intent.getStringExtra(RemoteControlService.EXTRA_TEXT) ?: lockStateStore.current().text

    private fun registerRemoteUnlockReceiver() {
        val filter = IntentFilter(RemoteControlService.ACTION_REMOTE_UNLOCK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(remoteUnlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(remoteUnlockReceiver, filter)
        }
    }

    // region quiz mode

    private fun setupQuizMode() {
        tvCategory = findViewById(R.id.tvCategory)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvFeedback = findViewById(R.id.tvFeedback)

        val btnAnswer1 = findViewById<CardView>(R.id.btnAnswer1)
        val btnAnswer2 = findViewById<CardView>(R.id.btnAnswer2)
        val btnAnswer3 = findViewById<CardView>(R.id.btnAnswer3)
        val btnAnswer4 = findViewById<CardView>(R.id.btnAnswer4)

        val tvAnswer1 = findViewById<TextView>(R.id.tvAnswer1)
        val tvAnswer2 = findViewById<TextView>(R.id.tvAnswer2)
        val tvAnswer3 = findViewById<TextView>(R.id.tvAnswer3)
        val tvAnswer4 = findViewById<TextView>(R.id.tvAnswer4)

        answerButtons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        answerTextViews = listOf(tvAnswer1, tvAnswer2, tvAnswer3, tvAnswer4)

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleAnswerSelected(index) }
        }

        loadNewQuestion()
    }

    private fun loadNewQuestion() {
        lifecycleScope.launch {
            val question = questionRepository.getRandomQuestion()
            if (question != null) {
                currentQuestion = question
                tvCategory?.text = question.category.displayName
                tvQuestion?.text = question.questionText
                question.options.forEachIndexed { index, option ->
                    answerTextViews.getOrNull(index)?.text = option
                }
                resetButtonStates()
                tvFeedback?.visibility = View.GONE
            } else {
                tvQuestion?.text = "Belum ada soal tersedia"
                tvCategory?.text = "Info"
            }
        }
    }

    private fun handleAnswerSelected(selectedIndex: Int) {
        val question = currentQuestion ?: return
        val isCorrect = selectedIndex == question.correctAnswerIndex

        answerButtons.forEach {
            it.isClickable = false
            it.alpha = 0.6f
        }

        answerButtons.forEachIndexed { index, button ->
            when {
                index == question.correctAnswerIndex ->
                    button.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_green))
                index == selectedIndex && !isCorrect ->
                    button.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_red))
            }
        }

        if (isCorrect) {
            tvFeedback?.text = "Benar! Bagus sekali! 🎉"
            tvFeedback?.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            tvFeedback?.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                lockStateStore.unlock()
                finish()
            }, 1500)
        } else {
            tvFeedback?.text = "Salah. Coba lagi! 🤔"
            tvFeedback?.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            tvFeedback?.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({ loadNewQuestion() }, 2000)
        }
    }

    private fun resetButtonStates() {
        answerButtons.forEach { button ->
            button.isClickable = true
            button.alpha = 1.0f
            button.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        }
    }

    // endregion

    // region text mode

    private fun setupTextMode() {
        val tvTextTitle = findViewById<TextView>(R.id.tvTextTitle)
        val tvTextMessage = findViewById<TextView>(R.id.tvTextMessage)
        etUnlockPin = findViewById(R.id.etUnlockPin)

        tvTextTitle.text = currentTitle() ?: "Pesan dari Orang Tua"
        tvTextMessage.text = currentText() ?: "Waktunya istirahat dari HP ya 😊"

        findViewById<Button>(R.id.btnPinUnlock).setOnClickListener { attemptPinUnlock() }
        findViewById<Button>(R.id.btnWaitParent).setOnClickListener {
            Toast.makeText(
                this,
                "Minta orang tua membuka kunci dari HP mereka",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun attemptPinUnlock() {
        val pin = etUnlockPin?.text?.toString()?.trim().orEmpty()
        if (pin.isEmpty()) {
            Toast.makeText(this, "Masukkan PIN dulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (authStore.verifyPin(pin)) {
            lockStateStore.unlock()
            Toast.makeText(this, "🔓 Terbuka", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show()
            etUnlockPin?.setText("")
        }
    }

    // endregion

    @Deprecated("Lock screen must not be dismissed with back")
    override fun onBackPressed() {
        // Block the back button while locked.
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing) {
            finish()
        }
    }
}
