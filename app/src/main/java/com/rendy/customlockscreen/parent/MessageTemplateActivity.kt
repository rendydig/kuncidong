package com.rendy.customlockscreen.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.data.MessageTemplate
import com.rendy.customlockscreen.data.QuestionDatabase
import com.rendy.customlockscreen.data.RemoteDao
import com.rendy.customlockscreen.lock.AppLockGate
import kotlinx.coroutines.launch

/** Lets the Parent create, list and delete reusable lock message templates. */
class MessageTemplateActivity : AppCompatActivity() {

    private lateinit var remoteDao: RemoteDao
    private lateinit var adapter: TemplateAdapter
    private lateinit var etTitle: EditText
    private lateinit var etText: EditText
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLockGate.requireUnlock(this, MessageTemplateActivity::class.java)) return
        setContentView(R.layout.activity_message_templates)

        remoteDao = QuestionDatabase.getDatabase(this).remoteDao()

        etTitle = findViewById(R.id.etTemplateTitle)
        etText = findViewById(R.id.etTemplateText)
        tvEmpty = findViewById(R.id.tvTemplatesEmpty)

        adapter = TemplateAdapter { template -> deleteTemplate(template) }
        findViewById<RecyclerView>(R.id.rvTemplates).apply {
            layoutManager = LinearLayoutManager(this@MessageTemplateActivity)
            adapter = this@MessageTemplateActivity.adapter
        }

        findViewById<MaterialButton>(R.id.btnAddTemplate).setOnClickListener { saveTemplate() }
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        loadTemplates()
    }

    override fun onResume() {
        super.onResume()
        if (AppLockGate.requireUnlock(this, MessageTemplateActivity::class.java)) return
    }

    private fun loadTemplates() {
        lifecycleScope.launch {
            seedDefaultsIfEmpty()
            val templates = remoteDao.getAllTemplates()
            adapter.submit(templates)
            tvEmpty.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private suspend fun seedDefaultsIfEmpty() {
        if (remoteDao.templateCount() > 0) return
        remoteDao.insertTemplate(
            MessageTemplate(
                title = "Waktu Belajar",
                text = "Saatnya belajar dulu ya. Selesaikan tugasmu, nanti HP dibuka lagi 😊"
            )
        )
        remoteDao.insertTemplate(
            MessageTemplate(
                title = "Waktunya Istirahat",
                text = "Sudah cukup main HP. Istirahat atau baca buku dulu ya."
            )
        )
        remoteDao.insertTemplate(
            MessageTemplate(
                title = "Waktunya Makan",
                text = "Ayo makan dulu. HP disimpan sebentar ya 🍽️"
            )
        )
    }

    private fun saveTemplate() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val text = etText.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Judul dan isi pesan harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            remoteDao.insertTemplate(MessageTemplate(title = title, text = text))
            etTitle.setText("")
            etText.setText("")
            Toast.makeText(this@MessageTemplateActivity, "Template disimpan", Toast.LENGTH_SHORT).show()
            loadTemplates()
        }
    }

    private fun deleteTemplate(template: MessageTemplate) {
        lifecycleScope.launch {
            remoteDao.deleteTemplate(template)
            loadTemplates()
        }
    }

    private class TemplateAdapter(
        private val onDelete: (MessageTemplate) -> Unit
    ) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

        private val templates = mutableListOf<MessageTemplate>()

        fun submit(newTemplates: List<MessageTemplate>) {
            templates.clear()
            templates.addAll(newTemplates)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_template, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = templates.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(templates[position])
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTemplateTitle)
            private val tvText: TextView = itemView.findViewById(R.id.tvTemplateText)
            private val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteTemplate)

            fun bind(template: MessageTemplate) {
                tvTitle.text = template.title
                tvText.text = template.text
                btnDelete.setOnClickListener { onDelete(template) }
            }
        }
    }
}
