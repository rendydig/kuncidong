package com.rendy.customlockscreen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var optionNever: CardView
    private lateinit var option1Min: CardView
    private lateinit var option10Min: CardView
    private lateinit var option15Min: CardView
    private lateinit var option30Min: CardView
    
    private lateinit var checkNever: TextView
    private lateinit var check1Min: TextView
    private lateinit var check10Min: TextView
    private lateinit var check15Min: TextView
    private lateinit var check30Min: TextView
    
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvNextTrigger: TextView
    
    private lateinit var btnSave: CardView
    private lateinit var btnBack: CardView
    
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedInterval = 15 // Default 15 minutes
    
    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateTimerStatus()
            statusHandler.postDelayed(this, 1000) // Update every second
        }
    }
    
    companion object {
        const val PREFS_NAME = "LockScreenTimerPrefs"
        const val KEY_TIMER_INTERVAL = "timer_interval"
        const val INTERVAL_NEVER = 0
        const val INTERVAL_1_MIN = 1
        const val INTERVAL_10_MIN = 10
        const val INTERVAL_15_MIN = 15
        const val INTERVAL_30_MIN = 30
        
        fun getTimerInterval(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_TIMER_INTERVAL, INTERVAL_15_MIN)
        }
        
        fun setTimerInterval(context: Context, interval: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_TIMER_INTERVAL, interval).apply()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initializeViews()
        initializePreferences()
        setupClickListeners()
        loadCurrentSettings()
        updateUI()
    }
    
    override fun onResume() {
        super.onResume()
        statusHandler.post(statusUpdateRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        statusHandler.removeCallbacks(statusUpdateRunnable)
    }
    
    private fun initializeViews() {
        optionNever = findViewById(R.id.optionNever)
        option1Min = findViewById(R.id.option1Min)
        option10Min = findViewById(R.id.option10Min)
        option15Min = findViewById(R.id.option15Min)
        option30Min = findViewById(R.id.option30Min)
        
        checkNever = findViewById(R.id.checkNever)
        check1Min = findViewById(R.id.check1Min)
        check10Min = findViewById(R.id.check10Min)
        check15Min = findViewById(R.id.check15Min)
        check30Min = findViewById(R.id.check30Min)
        
        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        tvNextTrigger = findViewById(R.id.tvNextTrigger)
        
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
    }
    
    private fun initializePreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun setupClickListeners() {
        optionNever.setOnClickListener {
            selectOption(INTERVAL_NEVER)
        }
        
        option1Min.setOnClickListener {
            selectOption(INTERVAL_1_MIN)
        }
        
        option10Min.setOnClickListener {
            selectOption(INTERVAL_10_MIN)
        }
        
        option15Min.setOnClickListener {
            selectOption(INTERVAL_15_MIN)
        }
        
        option30Min.setOnClickListener {
            selectOption(INTERVAL_30_MIN)
        }
        
        btnSave.setOnClickListener {
            saveSettings()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadCurrentSettings() {
        selectedInterval = sharedPreferences.getInt(KEY_TIMER_INTERVAL, INTERVAL_15_MIN)
    }
    
    private fun selectOption(interval: Int) {
        selectedInterval = interval
        updateUI()
    }
    
    private fun updateUI() {
        // Hide all checkmarks first
        checkNever.visibility = TextView.GONE
        check1Min.visibility = TextView.GONE
        check10Min.visibility = TextView.GONE
        check15Min.visibility = TextView.GONE
        check30Min.visibility = TextView.GONE
        
        // Show checkmark for selected option
        when (selectedInterval) {
            INTERVAL_NEVER -> checkNever.visibility = TextView.VISIBLE
            INTERVAL_1_MIN -> check1Min.visibility = TextView.VISIBLE
            INTERVAL_10_MIN -> check10Min.visibility = TextView.VISIBLE
            INTERVAL_15_MIN -> check15Min.visibility = TextView.VISIBLE
            INTERVAL_30_MIN -> check30Min.visibility = TextView.VISIBLE
        }
        
        updateTimerStatus()
    }
    
    private fun updateTimerStatus() {
        val statusText = when (selectedInterval) {
            INTERVAL_NEVER -> "Timer tidak aktif"
            INTERVAL_1_MIN -> "Timer aktif: Setiap 1 menit"
            INTERVAL_10_MIN -> "Timer aktif: Setiap 10 menit"
            INTERVAL_15_MIN -> "Timer aktif: Setiap 15 menit"
            INTERVAL_30_MIN -> "Timer aktif: Setiap 30 menit"
            else -> "Timer tidak dikenal"
        }
        
        tvTimerStatus.text = statusText
        
        // Calculate next trigger time
        if (selectedInterval > 0) {
            val nextTriggerTime = LockScreenTimerService.getNextTriggerTime(this)
            if (nextTriggerTime > 0) {
                val remainingMinutes = (nextTriggerTime - System.currentTimeMillis()) / (1000 * 60)
                val remainingSeconds = ((nextTriggerTime - System.currentTimeMillis()) / 1000) % 60
                
                if (remainingMinutes > 0) {
                    tvNextTrigger.text = "Layar kunci berikutnya: ${remainingMinutes} menit ${remainingSeconds} detik lagi"
                } else if (remainingSeconds > 0) {
                    tvNextTrigger.text = "Layar kunci berikutnya: ${remainingSeconds} detik lagi"
                } else {
                    tvNextTrigger.text = "Layar kunci akan muncul segera..."
                }
            } else {
                tvNextTrigger.text = "Timer belum dimulai"
            }
        } else {
            tvNextTrigger.text = "Timer dinonaktifkan"
        }
    }
    
    private fun saveSettings() {
        // Save to SharedPreferences
        sharedPreferences.edit()
            .putInt(KEY_TIMER_INTERVAL, selectedInterval)
            .apply()
        
        // Restart timer service with new interval
        val intent = Intent(this, LockScreenTimerService::class.java)
        stopService(intent)
        
        if (selectedInterval > 0) {
            startService(intent)
            Toast.makeText(this, "Pengaturan disimpan! Timer diatur ulang.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Timer dinonaktifkan.", Toast.LENGTH_SHORT).show()
        }
        
        // Go back to main activity
        finish()
    }
}
