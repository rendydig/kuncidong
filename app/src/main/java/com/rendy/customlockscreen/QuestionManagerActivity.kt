package com.rendy.customlockscreen

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.rendy.customlockscreen.adapters.QuestionPackAdapter
import com.rendy.customlockscreen.adapters.CurrentQuestionsAdapter
import com.rendy.customlockscreen.data.QuestionRepository
import com.rendy.customlockscreen.models.QuestionPack
import com.rendy.customlockscreen.services.GoogleDriveService
import kotlinx.coroutines.launch

class QuestionManagerActivity : AppCompatActivity() {
    
    private lateinit var questionRepository: QuestionRepository
    private lateinit var googleDriveService: GoogleDriveService
    private lateinit var questionPackAdapter: QuestionPackAdapter
    private lateinit var currentQuestionsAdapter: CurrentQuestionsAdapter
    private lateinit var progressDialog: ProgressDialog
    
    private lateinit var rvAvailablePacks: RecyclerView
    private lateinit var rvCurrentQuestions: RecyclerView
    private lateinit var btnRefreshPacks: MaterialButton
    private lateinit var btnViewCurrent: MaterialButton
    private lateinit var tvPacksCount: MaterialTextView
    private lateinit var tvCurrentCount: MaterialTextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_manager)
        
        initializeComponents()
        setupRecyclerViews()
        setupClickListeners()
        loadAvailableQuestionPacks()
    }
    
    private fun initializeComponents() {
        questionRepository = QuestionRepository(this)
        googleDriveService = GoogleDriveService()
        
        rvAvailablePacks = findViewById(R.id.rvAvailablePacks)
        rvCurrentQuestions = findViewById(R.id.rvCurrentQuestions)
        btnRefreshPacks = findViewById(R.id.btnRefreshPacks)
        btnViewCurrent = findViewById(R.id.btnViewCurrent)
        tvPacksCount = findViewById(R.id.tvPacksCount)
        tvCurrentCount = findViewById(R.id.tvCurrentCount)
        
        progressDialog = ProgressDialog(this).apply {
            setMessage("Mengunduh paket soal...")
            setCancelable(false)
        }
    }
    
    private fun setupRecyclerViews() {
        // Available question packs
        questionPackAdapter = QuestionPackAdapter { questionPack ->
            downloadQuestionPack(questionPack)
        }
        rvAvailablePacks.apply {
            layoutManager = LinearLayoutManager(this@QuestionManagerActivity)
            adapter = questionPackAdapter
        }
        
        // Current questions
        currentQuestionsAdapter = CurrentQuestionsAdapter()
        rvCurrentQuestions.apply {
            layoutManager = LinearLayoutManager(this@QuestionManagerActivity)
            adapter = currentQuestionsAdapter
        }
    }
    
    private fun setupClickListeners() {
        btnRefreshPacks.setOnClickListener {
            loadAvailableQuestionPacks()
        }
        
        btnViewCurrent.setOnClickListener {
            loadCurrentQuestions()
        }
    }
    
    private fun loadAvailableQuestionPacks() {
        lifecycleScope.launch {
            try {
                showLoading("Memuat daftar paket soal...")
                val questionPacks = googleDriveService.getAvailableQuestionPacks()
                questionPackAdapter.updatePacks(questionPacks)
                tvPacksCount.text = "Tersedia ${questionPacks.size} paket soal"
                hideLoading()
            } catch (e: Exception) {
                hideLoading()
                showError("Gagal memuat daftar paket soal: ${e.message}")
            }
        }
    }
    
    private fun loadCurrentQuestions() {
        lifecycleScope.launch {
            try {
                val questions = questionRepository.getAllQuestions()
                currentQuestionsAdapter.updateQuestions(questions)
                tvCurrentCount.text = "Total ${questions.size} soal tersimpan"
                
                // Show current questions section
                findViewById<MaterialCardView>(R.id.cardCurrentQuestions).visibility = 
                    android.view.View.VISIBLE
            } catch (e: Exception) {
                showError("Gagal memuat soal saat ini: ${e.message}")
            }
        }
    }
    
    private fun downloadQuestionPack(questionPack: QuestionPack) {
        lifecycleScope.launch {
            try {
                showLoading("Mengunduh ${questionPack.name}...")
                
                // Download questions from Google Drive
                val questions = googleDriveService.downloadQuestionPack(questionPack)
                
                // Replace all questions in database
                questionRepository.replaceAllQuestions(questions)
                
                hideLoading()
                showSuccess("Berhasil mengunduh ${questions.size} soal dari ${questionPack.name}")
                
                // Refresh current questions if visible
                if (findViewById<MaterialCardView>(R.id.cardCurrentQuestions).visibility == 
                    android.view.View.VISIBLE) {
                    loadCurrentQuestions()
                }
                
            } catch (e: Exception) {
                hideLoading()
                showError("Gagal mengunduh paket soal: ${e.message}")
            }
        }
    }
    
    private fun showLoading(message: String) {
        progressDialog.setMessage(message)
        progressDialog.show()
    }
    
    private fun hideLoading() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
