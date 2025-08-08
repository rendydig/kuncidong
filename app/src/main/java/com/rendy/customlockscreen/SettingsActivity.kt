package com.rendy.customlockscreen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var spinnerTimerInterval: Spinner
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvNextTrigger: TextView
    
    private lateinit var btnSave: CardView
    private lateinit var btnBack: CardView
    
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedInterval = 15 // Default 15 minutes
    
    private val timerOptions = arrayOf(
        "Tidak Pernah",
        "Setiap 1 Menit", 
        "Setiap 3 Menit",
        "Setiap 5 Menit",
        "Setiap 10 Menit",
        "Setiap 15 Menit"
    )
    
    private val timerValues = arrayOf(0, 1, 3, 5, 10, 15)
    
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
        const val INTERVAL_3_MIN = 3
        const val INTERVAL_5_MIN = 5
        const val INTERVAL_10_MIN = 10
        const val INTERVAL_15_MIN = 15
        
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
        updateTimerStatus()
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
        spinnerTimerInterval = findViewById(R.id.spinnerTimerInterval)
        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        tvNextTrigger = findViewById(R.id.tvNextTrigger)
        
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        
        setupSpinner()
    }
    
    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimerInterval.adapter = adapter
        
        spinnerTimerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedInterval = timerValues[position]
                updateTimerStatus()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun initializePreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveSettings()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadCurrentSettings() {
        selectedInterval = sharedPreferences.getInt(KEY_TIMER_INTERVAL, INTERVAL_15_MIN)
        
        // Set spinner selection based on current interval
        val position = timerValues.indexOf(selectedInterval)
        if (position >= 0) {
            spinnerTimerInterval.setSelection(position)
        }
    }
    
    private fun updateTimerStatus() {
        val statusText = when (selectedInterval) {
            INTERVAL_NEVER -> "Timer tidak aktif"
            INTERVAL_1_MIN -> "Timer aktif: Setiap 1 menit"
            INTERVAL_3_MIN -> "Timer aktif: Setiap 3 menit"
            INTERVAL_5_MIN -> "Timer aktif: Setiap 5 menit"
            INTERVAL_10_MIN -> "Timer aktif: Setiap 10 menit"
            INTERVAL_15_MIN -> "Timer aktif: Setiap 15 menit"
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
