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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.rendy.customlockscreen.data.Question
import com.rendy.customlockscreen.data.QuestionRepository

class LockScreenActivity : AppCompatActivity() {
    
    private lateinit var tvCategory: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvFeedback: TextView
    
    private lateinit var btnAnswer1: CardView
    private lateinit var btnAnswer2: CardView
    private lateinit var btnAnswer3: CardView
    private lateinit var btnAnswer4: CardView
    
    private lateinit var tvAnswer1: TextView
    private lateinit var tvAnswer2: TextView
    private lateinit var tvAnswer3: TextView
    private lateinit var tvAnswer4: TextView
    
    private lateinit var answerButtons: List<CardView>
    private lateinit var answerTextViews: List<TextView>
    private lateinit var questionRepository: QuestionRepository
    private lateinit var currentQuestion: Question
    

    
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
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    private fun initializeViews() {
        tvCategory = findViewById(R.id.tvCategory)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvFeedback = findViewById(R.id.tvFeedback)
        
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)
        
        tvAnswer1 = findViewById(R.id.tvAnswer1)
        tvAnswer2 = findViewById(R.id.tvAnswer2)
        tvAnswer3 = findViewById(R.id.tvAnswer3)
        tvAnswer4 = findViewById(R.id.tvAnswer4)
        
        answerButtons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        answerTextViews = listOf(tvAnswer1, tvAnswer2, tvAnswer3, tvAnswer4)
        
        setupAnswerButtonListeners()
    }
    
    private fun initializeData() {
        questionRepository = QuestionRepository(this)
    }
    
    private fun setupAnswerButtonListeners() {
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswerSelected(index)
            }
        }
    }
    
    private fun loadNewQuestion() {
        lifecycleScope.launch {
            val question = questionRepository.getRandomQuestion()
            if (question != null) {
                currentQuestion = question
                
                tvCategory.text = currentQuestion.category.displayName
                tvQuestion.text = currentQuestion.questionText
                
                // Set answer options
                currentQuestion.options.forEachIndexed { index, option ->
                    answerTextViews[index].text = option
                }
                
                // Reset button states
                resetButtonStates()
                tvFeedback.visibility = View.GONE
            } else {
                // Handle case when no question is available
                tvQuestion.text = "No questions available"
                tvCategory.text = "Error"
            }
        }
    }
    
    private fun handleAnswerSelected(selectedIndex: Int) {
        val isCorrect = selectedIndex == currentQuestion.correctAnswerIndex
        
        // Disable all buttons
        answerButtons.forEach { 
            it.isClickable = false
            it.alpha = 0.6f
        }
        
        // Update button colors
        answerButtons.forEachIndexed { index, button ->
            when {
                index == currentQuestion.correctAnswerIndex -> {
                    button.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_green))
                }
                index == selectedIndex && !isCorrect -> {
                    button.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_red))
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
            button.isClickable = true
            button.alpha = 1.0f
            button.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        }
    }
    
    private fun unlockDevice() {
        // Finish this activity to return to home screen
        finish()
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
