package com.rendy.customlockscreen.kids

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
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
import com.google.android.material.card.MaterialCardView
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

    private val timerOptions by lazy { resources.getStringArray(R.array.kids_settings_timer_options) }
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
        findViewById<MaterialCardView>(R.id.cardChangePin).setOnClickListener { showChangePinDialog() }
        findViewById<MaterialCardView>(R.id.cardChangeAppPin).setOnClickListener {
            AppPinDialog.show(this) { refreshAppPinStatus() }
        }
        findViewById<MaterialCardView>(R.id.cardManageQuestions).setOnClickListener {
            startActivity(Intent(this, QuestionManagerActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardTestLock).setOnClickListener {
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
            getString(R.string.kids_settings_timer_off)
        } else {
            getString(R.string.kids_settings_timer_every, interval)
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
        tvPinStatus.text =
            if (authStore.hasPin()) getString(R.string.pin_status_set) else getString(R.string.pin_status_unset)
    }

    private fun refreshAppPinStatus() {
        tvAppPinStatus.text = if (appPinStore.hasPin()) {
            getString(R.string.kids_settings_app_pin_active)
        } else {
            getString(R.string.kids_settings_app_pin_unset)
        }
    }

    private fun showChangePinDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val etPin = EditText(this).apply {
            hint = getString(R.string.kids_settings_new_pin_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val etConfirm = EditText(this).apply {
            hint = getString(R.string.kids_settings_confirm_pin_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        container.addView(etPin)
        container.addView(etConfirm)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.kids_settings_pin_dialog_title)
            .setView(container)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = etPin.text?.toString()?.trim().orEmpty()
                val confirm = etConfirm.text?.toString()?.trim().orEmpty()
                when {
                    pin.length < 4 -> Toast.makeText(this, getString(R.string.pin_error_min), Toast.LENGTH_SHORT).show()
                    pin != confirm -> Toast.makeText(this, getString(R.string.pin_error_mismatch), Toast.LENGTH_SHORT).show()
                    else -> {
                        authStore.setPin(pin)
                        dialog.dismiss()
                        refreshPinStatus()
                        Toast.makeText(this, getString(R.string.kids_settings_pin_saved), Toast.LENGTH_SHORT).show()
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
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_heading))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val unpair = MaterialButton(this).apply {
                text = getString(R.string.action_unpair)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_label))
                minWidth = 0
                setOnClickListener {
                    authStore.unpairParent(parent.id)
                    refreshParents()
                    Toast.makeText(this@KidsSettingsActivity, getString(R.string.action_unpaired), Toast.LENGTH_SHORT).show()
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
