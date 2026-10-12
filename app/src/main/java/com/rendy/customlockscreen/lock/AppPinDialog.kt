package com.rendy.customlockscreen.lock

import android.app.Activity
import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

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
            hint = "PIN aplikasi baru (min. 4 angka)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val etConfirm = EditText(activity).apply {
            hint = "Ulangi PIN aplikasi baru"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        container.addView(etPin)
        container.addView(etConfirm)

        val builder = AlertDialog.Builder(activity)
            .setTitle(if (hasPin) "Ubah PIN Aplikasi" else "Buat PIN Aplikasi")
            .setView(container)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
        if (hasPin) builder.setNeutralButton("Hapus PIN", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = etPin.text?.toString()?.trim().orEmpty()
                val confirm = etConfirm.text?.toString()?.trim().orEmpty()
                when {
                    pin.length < 4 -> toast(activity, "PIN minimal 4 angka")
                    pin != confirm -> toast(activity, "PIN tidak sama")
                    else -> {
                        store.setPin(pin)
                        dialog.dismiss()
                        toast(activity, "PIN aplikasi disimpan")
                        onChanged()
                    }
                }
            }
            if (hasPin) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    store.clearPin()
                    dialog.dismiss()
                    toast(activity, "PIN aplikasi dihapus")
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
