package com.rendy.customlockscreen.kids

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.lock.AppLockGate
import com.rendy.customlockscreen.net.RemoteAuthStore

/**
 * Shows the rotating 6 digit pairing code while open. The code is only valid
 * during this activity's lifetime and rotates every 60 seconds.
 */
class PairingActivity : AppCompatActivity() {

    private lateinit var authStore: RemoteAuthStore
    private lateinit var tvPairCode: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var tvNoParents: TextView
    private lateinit var llParents: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (authStore.pairingCodeSecondsLeft() <= 0) {
                authStore.refreshPairingCode()
            }
            renderCode()
            refreshParents()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLockGate.requireUnlock(this, PairingActivity::class.java)) return
        setContentView(R.layout.activity_pairing)

        authStore = RemoteAuthStore(this)

        tvPairCode = findViewById(R.id.tvPairCode)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvNoParents = findViewById(R.id.tvNoParents)
        llParents = findViewById(R.id.llParents)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        tvDeviceName.text = authStore.deviceName()
        authStore.startPairingSession()
        renderCode()
        refreshParents()
    }

    override fun onResume() {
        super.onResume()
        if (AppLockGate.requireUnlock(this, PairingActivity::class.java)) return
        if (authStore.currentPairingCode() == null) {
            authStore.startPairingSession()
        }
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        super.onDestroy()
        authStore.endPairingSession()
    }

    private fun renderCode() {
        val code = authStore.currentPairingCode()
        if (code == null) {
            tvPairCode.text = getString(R.string.pairing_code_empty)
            tvCountdown.text = getString(R.string.pairing_code_inactive)
            return
        }
        tvPairCode.text = code.chunked(3).joinToString(" ")
        tvCountdown.text = getString(R.string.pairing_code_countdown, authStore.pairingCodeSecondsLeft())
    }

    private fun refreshParents() {
        val parents = authStore.listParents()
        tvNoParents.visibility = if (parents.isEmpty()) TextView.VISIBLE else TextView.GONE

        llParents.removeAllViews()
        for (parent in parents) {
            val row = TextView(this).apply {
                text = getString(R.string.pairing_parent_row, parent.name)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_heading))
                setTextColor(resources.getColor(R.color.content_primary, theme))
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
                compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.space_sm)
                compoundDrawableTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.success, theme))
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.space_md),
                    resources.getDimensionPixelSize(R.dimen.space_lg),
                    resources.getDimensionPixelSize(R.dimen.space_md),
                    resources.getDimensionPixelSize(R.dimen.space_lg)
                )
                setBackgroundResource(R.drawable.permission_item_background)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            llParents.addView(row)
        }
    }
}
