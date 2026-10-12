package com.rendy.customlockscreen.kids

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.rendy.customlockscreen.LockScreenActivity
import com.rendy.customlockscreen.LockScreenTimerService
import com.rendy.customlockscreen.QuestionManagerActivity
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.RoleSelectionActivity
import com.rendy.customlockscreen.SmartLockApplication
import com.rendy.customlockscreen.data.TimerSettings
import com.rendy.customlockscreen.lock.AppLockGate
import com.rendy.customlockscreen.lock.AppPinDialog
import com.rendy.customlockscreen.lock.AppPinStore
import com.rendy.customlockscreen.net.LockMode
import com.rendy.customlockscreen.net.RemoteAuthStore

/** Consolidated settings for the Kids device. */
class KidsSettingsActivity : AppCompatActivity() {

    private lateinit var authStore: RemoteAuthStore
    private lateinit var appPinStore: AppPinStore
    private lateinit var spinnerTimer: Spinner
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvPinStatus: TextView
    private lateinit var tvAppPinStatus: TextView
    private lateinit var tvNoParents: TextView
    private lateinit var llParents: LinearLayout
    private lateinit var rbDefaultQuiz: RadioButton
    private lateinit var rbDefaultText: RadioButton

    private val timerOptions = arrayOf(
        "Tidak Pernah",
        "Setiap 1 Menit",
        "Setiap 3 Menit",
        "Setiap 5 Menit",
        "Setiap 10 Menit",
        "Setiap 15 Menit"
    )
    private val timerValues = arrayOf(0, 1, 3, 5, 10, 15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLockGate.requireUnlock(this, KidsSettingsActivity::class.java)) return
        setContentView(R.layout.activity_kids_settings)

        authStore = RemoteAuthStore(this)
        appPinStore = AppPinStore(this)

        spinnerTimer = findViewById(R.id.spinnerTimerInterval)
        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        tvPinStatus = findViewById(R.id.tvPinStatus)
        tvAppPinStatus = findViewById(R.id.tvAppPinStatus)
        tvNoParents = findViewById(R.id.tvNoParentsSettings)
        llParents = findViewById(R.id.llParentsSettings)
        rbDefaultQuiz = findViewById(R.id.rbDefaultQuiz)
        rbDefaultText = findViewById(R.id.rbDefaultText)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnChangePin).setOnClickListener { showChangePinDialog() }
        findViewById<MaterialButton>(R.id.btnChangeAppPin).setOnClickListener {
            AppPinDialog.show(this) { refreshAppPinStatus() }
        }
        findViewById<MaterialButton>(R.id.btnManageQuestions).setOnClickListener {
            startActivity(Intent(this, QuestionManagerActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTestLock).setOnClickListener {
            startActivity(Intent(this, LockScreenActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnSwitchRole).setOnClickListener {
            SmartLockApplication.instance.setRole(SmartLockApplication.ROLE_UNSET)
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }

        setupTimerSpinner()
        setupDefaultMode()
        refreshPinStatus()
        refreshAppPinStatus()
        refreshParents()
    }

    override fun onResume() {
        super.onResume()
        if (AppLockGate.requireUnlock(this, KidsSettingsActivity::class.java)) return
        refreshPinStatus()
        refreshAppPinStatus()
        refreshParents()
    }

    // region timer

    private fun setupTimerSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = adapter

        val current = TimerSettings.getTimerInterval(this)
        val index = timerValues.indexOf(current)
        if (index >= 0) spinnerTimer.setSelection(index)
        updateTimerStatus(current)

        spinnerTimer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val interval = timerValues[position]
                TimerSettings.setTimerInterval(this@KidsSettingsActivity, interval)
                val intent = Intent(this@KidsSettingsActivity, LockScreenTimerService::class.java)
                stopService(intent)
                if (interval > 0) startService(intent)
                updateTimerStatus(interval)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updateTimerStatus(interval: Int) {
        tvTimerStatus.text = if (interval <= 0) {
            "Kunci otomatis dimatikan"
        } else {
            "Kunci otomatis setiap $interval menit"
        }
    }

    // endregion

    // region default mode

    private fun setupDefaultMode() {
        val mode = getDefaultMode(this)
        if (mode == LockMode.TEXT) rbDefaultText.isChecked = true else rbDefaultQuiz.isChecked = true

        rbDefaultQuiz.setOnCheckedChangeListener { _, checked ->
            if (checked) setDefaultMode(this, LockMode.QUIZ)
        }
        rbDefaultText.setOnCheckedChangeListener { _, checked ->
            if (checked) setDefaultMode(this, LockMode.TEXT)
        }
    }

    // endregion

    // region PIN

    private fun refreshPinStatus() {
        tvPinStatus.text = if (authStore.hasPin()) "PIN sudah diatur" else "Belum diatur"
    }

    private fun refreshAppPinStatus() {
        tvAppPinStatus.text = if (appPinStore.hasPin()) {
            "Aktif - app minta PIN saat dibuka"
        } else {
            "Belum diatur - app bisa dibuka bebas"
        }
    }

    private fun showChangePinDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val etPin = EditText(this).apply {
            hint = "PIN baru (min. 4 angka)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val etConfirm = EditText(this).apply {
            hint = "Ulangi PIN baru"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        container.addView(etPin)
        container.addView(etConfirm)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ubah PIN Rahasia")
            .setView(container)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = etPin.text?.toString()?.trim().orEmpty()
                val confirm = etConfirm.text?.toString()?.trim().orEmpty()
                when {
                    pin.length < 4 -> Toast.makeText(this, "PIN minimal 4 angka", Toast.LENGTH_SHORT).show()
                    pin != confirm -> Toast.makeText(this, "PIN tidak sama", Toast.LENGTH_SHORT).show()
                    else -> {
                        authStore.setPin(pin)
                        dialog.dismiss()
                        refreshPinStatus()
                        Toast.makeText(this, "PIN disimpan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    // endregion

    // region parents

    private fun refreshParents() {
        val parents = authStore.listParents()
        tvNoParents.visibility = if (parents.isEmpty()) TextView.VISIBLE else TextView.GONE
        llParents.removeAllViews()

        for (parent in parents) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 16, 8, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val name = TextView(this).apply {
                text = parent.name
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val unpair = MaterialButton(this).apply {
                text = "Lepas"
                textSize = 12f
                minWidth = 0
                setOnClickListener {
                    authStore.unpairParent(parent.id)
                    refreshParents()
                    Toast.makeText(this@KidsSettingsActivity, "Dilepas", Toast.LENGTH_SHORT).show()
                }
            }

            row.addView(name)
            row.addView(unpair)
            llParents.addView(row)
        }
    }

    // endregion

    companion object {
        private const val PREFS_NAME = "kuncidong_kids_settings"
        private const val KEY_DEFAULT_MODE = "default_lock_mode"

        /** Lock mode used when the local timer triggers a lock. */
        fun getDefaultMode(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_DEFAULT_MODE, LockMode.QUIZ) ?: LockMode.QUIZ
        }

        fun setDefaultMode(context: Context, mode: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DEFAULT_MODE, mode)
                .apply()
        }
    }
}
