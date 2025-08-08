package com.rendy.customlockscreen

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.rendy.customlockscreen.data.Question
import com.rendy.customlockscreen.data.QuestionRepository
import java.text.SimpleDateFormat
import java.util.*

class LockScreenActivity : AppCompatActivity() {
    
    private lateinit var tvCategory: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    
    private lateinit var answerButtons: List<Button>
    private lateinit var questionRepository: QuestionRepository
    private lateinit var currentQuestion: Question
    
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTimeAndDate()
            timeHandler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("LockScreenActivity", "onCreate() called - Lock screen activity starting")
        
        // Dismiss the lock screen notification since user opened the activity
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(2) // LOCK_SCREEN_NOTIFICATION_ID
        
        // Enhanced flags for ColorOS/OPPO compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Additional flags for better ColorOS compatibility
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        
        // Set window type for system overlay (requires SYSTEM_ALERT_WINDOW permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        
        // Make it appear over system UI
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        
        setContentView(R.layout.activity_lock_screen)
        
        initializeViews()
        initializeData()
        loadNewQuestion()
        startTimeUpdates()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeUpdateRunnable)
    }
    
    private fun initializeViews() {
        tvCategory = findViewById(R.id.tvCategory)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvTime = findViewById(R.id.tvTime)
        tvDate = findViewById(R.id.tvDate)
        
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)
        
        answerButtons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        
        setupAnswerButtonListeners()
    }
    
    private fun initializeData() {
        questionRepository = QuestionRepository()
    }
    
    private fun setupAnswerButtonListeners() {
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswerSelected(index)
            }
        }
    }
    
    private fun loadNewQuestion() {
        currentQuestion = questionRepository.getRandomQuestion()
        
        tvCategory.text = currentQuestion.category.displayName
        tvQuestion.text = currentQuestion.questionText
        
        // Set answer options
        currentQuestion.options.forEachIndexed { index, option ->
            answerButtons[index].text = option
        }
        
        // Reset button states
        resetButtonStates()
        tvFeedback.visibility = View.GONE
    }
    
    private fun handleAnswerSelected(selectedIndex: Int) {
        val isCorrect = selectedIndex == currentQuestion.correctAnswerIndex
        
        // Disable all buttons
        answerButtons.forEach { it.isEnabled = false }
        
        // Update button colors
        answerButtons.forEachIndexed { index, button ->
            when {
                index == currentQuestion.correctAnswerIndex -> {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.answer_button_correct))
                }
                index == selectedIndex && !isCorrect -> {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.answer_button_incorrect))
                }
            }
        }
        
        // Show feedback
        if (isCorrect) {
            tvFeedback.text = "Correct! Well done! 🎉"
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            tvFeedback.visibility = View.VISIBLE
            
            // Unlock device after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                unlockDevice()
            }, 1500)
        } else {
            tvFeedback.text = "Incorrect. Try again! 🤔"
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            tvFeedback.visibility = View.VISIBLE
            
            // Load new question after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                loadNewQuestion()
            }, 2000)
        }
    }
    
    private fun resetButtonStates() {
        answerButtons.forEach { button ->
            button.isEnabled = true
            button.setBackgroundResource(R.drawable.answer_button_selector)
        }
    }
    
    private fun unlockDevice() {
        // Finish this activity to return to home screen
        finish()
    }
    
    private fun startTimeUpdates() {
        timeHandler.post(timeUpdateRunnable)
    }
    
    private fun updateTimeAndDate() {
        val now = Calendar.getInstance()
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        
        tvTime.text = timeFormat.format(now.time)
        tvDate.text = dateFormat.format(now.time)
    }
    
    override fun onBackPressed() {
        // Prevent back button from closing the lock screen
        // Do nothing
    }
    
    override fun onPause() {
        super.onPause()
        // If the activity is paused and we haven't unlocked, finish it
        // This prevents the lock screen from staying in background
        if (!isFinishing) {
            finish()
        }
    }
}
