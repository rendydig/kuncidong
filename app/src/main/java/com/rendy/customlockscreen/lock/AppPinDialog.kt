package com.rendy.customlockscreen.lock

import android.app.Activity
import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.rendy.customlockscreen.R

/**
 * Dialog bersama untuk membuat, mengubah, atau menghapus PIN khusus aplikasi.
 * Dipakai dari Pengaturan HP Anak maupun Pengaturan HP Orang Tua.
 */
object AppPinDialog {

    fun show(activity: Activity, onChanged: () -> Unit = {}) {
        val store = AppPinStore(activity)
        val hasPin = store.hasPin()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val etPin = EditText(activity).apply {
            hint = activity.getString(R.string.app_pin_new_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val etConfirm = EditText(activity).apply {
            hint = activity.getString(R.string.app_pin_confirm_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        container.addView(etPin)
        container.addView(etConfirm)

        val builder = AlertDialog.Builder(activity)
            .setTitle(if (hasPin) R.string.app_pin_change_title else R.string.app_pin_create_title)
            .setView(container)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
        if (hasPin) builder.setNeutralButton(R.string.app_pin_delete, null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = etPin.text?.toString()?.trim().orEmpty()
                val confirm = etConfirm.text?.toString()?.trim().orEmpty()
                when {
                    pin.length < 4 -> toast(activity, activity.getString(R.string.pin_error_min))
                    pin != confirm -> toast(activity, activity.getString(R.string.pin_error_mismatch))
                    else -> {
                        store.setPin(pin)
                        dialog.dismiss()
                        toast(activity, activity.getString(R.string.app_pin_saved))
                        onChanged()
                    }
                }
            }
            if (hasPin) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    store.clearPin()
                    dialog.dismiss()
                    toast(activity, activity.getString(R.string.app_pin_deleted))
                    onChanged()
                }
            }
        }
        dialog.show()
    }

    private fun toast(activity: Activity, message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
