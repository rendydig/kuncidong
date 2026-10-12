package com.rendy.customlockscreen.parent

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.data.MessageTemplate
import com.rendy.customlockscreen.net.LockMode

/** A fully specified lock instruction sent to one or more Kids devices. */
data class LockCommand(
    val mode: String,
    val title: String?,
    val text: String?,
    val pack: String? = null
)

/**
 * Asks the Parent which kind of lock to apply: an educational quiz or a text
 * message (picked from saved templates or typed freely).
 */
object LockCommandDialog {

    private const val FREE_TEXT_LABEL = "✏️ Ketik pesan sendiri"

    fun show(
        context: Context,
        targetLabel: String,
        templates: List<MessageTemplate>,
        onConfirm: (LockCommand) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_lock_command, null)
        val tvTarget = view.findViewById<TextView>(R.id.tvLockTarget)
        val rbText = view.findViewById<RadioButton>(R.id.rbText)
        val textSection = view.findViewById<LinearLayout>(R.id.textSection)
        val spinner = view.findViewById<Spinner>(R.id.spinnerTemplate)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etText = view.findViewById<EditText>(R.id.etText)

        tvTarget.text = targetLabel

        val labels = mutableListOf(FREE_TEXT_LABEL)
        labels.addAll(templates.map { it.title })
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val template = templates.getOrNull(position - 1) ?: return
                etTitle.setText(template.title)
                etText.setText(template.text)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        rbText.setOnCheckedChangeListener { _, checked ->
            textSection.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Kunci HP Anak")
            .setView(view)
            .setPositiveButton("🔒 Kunci", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (rbText.isChecked && etText.text.isBlank()) {
                    Toast.makeText(context, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val command = if (rbText.isChecked) {
                    LockCommand(
                        mode = LockMode.TEXT,
                        title = etTitle.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                        text = etText.text?.toString()?.trim()
                    )
                } else {
                    LockCommand(mode = LockMode.QUIZ, title = null, text = null)
                }
                dialog.dismiss()
                onConfirm(command)
            }
        }

        dialog.show()
    }
}
