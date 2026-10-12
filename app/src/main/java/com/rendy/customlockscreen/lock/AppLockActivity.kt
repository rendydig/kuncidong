package com.rendy.customlockscreen.lock

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rendy.customlockscreen.MainActivity
import com.rendy.customlockscreen.R

/**
 * Layar gerbang PIN khusus aplikasi. Muncul saat app dibuka/dikembalikan dan
 * PIN app sudah dibuat. Tidak bisa dilewati dengan tombol back.
 */
class AppLockActivity : AppCompatActivity() {

    private lateinit var etPin: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        etPin = findViewById(R.id.etAppPin)
        tvError = findViewById(R.id.tvAppPinError)

        findViewById<Button>(R.id.btnAppPinUnlock).setOnClickListener { attemptUnlock() }
    }

    private fun attemptUnlock() {
        val store = AppPinStore(this)
        if (!store.hasPin()) {
            proceed()
            return
        }

        val pin = etPin.text?.toString()?.trim().orEmpty()
        if (pin.isEmpty()) {
            showError("Masukkan PIN dulu")
            return
        }

        if (store.verifyPin(pin)) {
            AppLock.unlock()
            proceed()
        } else {
            etPin.setText("")
            showError("PIN salah. Coba lagi.")
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = TextView.VISIBLE
    }

    private fun proceed() {
        startActivity(
            Intent(this, targetClass())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    private fun targetClass(): Class<*> {
        val name = intent.getStringExtra(EXTRA_TARGET_CLASS) ?: return MainActivity::class.java
        return try {
            Class.forName(name)
        } catch (e: ClassNotFoundException) {
            MainActivity::class.java
        }
    }

    @Deprecated("PIN gate must not be dismissed with back")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    companion object {
        const val EXTRA_TARGET_CLASS = "extra_target_class"
    }
}
