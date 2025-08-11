package com.rendy.customlockscreen.services

import com.google.gson.Gson
import com.rendy.customlockscreen.data.Question
import com.rendy.customlockscreen.data.QuestionCategory
import com.rendy.customlockscreen.data.QuestionData
import com.rendy.customlockscreen.models.QuestionPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleDriveService {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // Google Drive public folder containing question packs
    // Replace with your actual Google Drive folder ID
    private val driveApiUrl = "https://www.googleapis.com/drive/v3/files"
    private val publicFolderId = "YOUR_GOOGLE_DRIVE_FOLDER_ID" // Replace with actual folder ID
    
    /**
     * Get list of available question packs from Google Drive
     */
    suspend fun getAvailableQuestionPacks(): List<QuestionPack> {
        return withContext(Dispatchers.IO) {
            try {
                // For demo purposes, return hardcoded list
                // In production, you would query Google Drive API
                getDemoQuestionPacks()
            } catch (e: Exception) {
                throw IOException("Failed to fetch question packs: ${e.message}")
            }
        }
    }
    
    /**
     * Download a specific question pack from Google Drive
     */
    suspend fun downloadQuestionPack(questionPack: QuestionPack): List<Question> {
        return withContext(Dispatchers.IO) {
            try {
                // Download the JSON file from Google Drive
                val jsonContent = downloadFileFromDrive(questionPack.fileId)
                
                // Parse JSON to Question objects
                parseQuestionsFromJson(jsonContent)
                
            } catch (e: Exception) {
                throw IOException("Failed to download question pack: ${e.message}")
            }
        }
    }
    
    /**
     * Download file content from Google Drive using direct download link
     */
    private suspend fun downloadFileFromDrive(fileId: String): String {
        return withContext(Dispatchers.IO) {
            // Google Drive direct download URL format
            val downloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
            
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Failed to download file: ${response.code}")
            }
            
            response.body?.string() ?: throw IOException("Empty response body")
        }
    }
    
    /**
     * Parse JSON content to Question objects
     */
    private fun parseQuestionsFromJson(jsonContent: String): List<Question> {
        return try {
            val questionData = gson.fromJson(jsonContent, QuestionData::class.java)
            questionData.questions.mapIndexed { index, questionJson ->
                Question(
                    id = questionJson.id ?: (index + 1),
                    category = QuestionCategory.valueOf(questionJson.category ?: "MATEMATIKA"),
                    questionText = questionJson.question ?: "",
                    options = questionJson.options ?: emptyList(),
                    correctAnswerIndex = questionJson.correctAnswerIndex ?: 0
                )
            }
        } catch (e: Exception) {
            throw IOException("Failed to parse questions JSON: ${e.message}")
        }
    }
    
    /**
     * Demo question packs for testing
     * Replace this with actual Google Drive API calls
     */
    private fun getDemoQuestionPacks(): List<QuestionPack> {
        return listOf(
            QuestionPack(
                id = "1",
                name = "Bank Soal Kelas 1 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 1 SD",
                category = "KELAS_1",
                questionCount = 100,
                fileId = "1y5UyvqpCpkDdbu9ykPCB3OsgYhuB2nQa",
                version = "1.0",
                lastUpdated = "2024-01-15"
            ),
            QuestionPack(
                id = "2",
                name = "Bank Soal Kelas 2 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 2 SD",
                category = "KELAS_2",
                questionCount = 120,
                fileId = "1lH1gP-1n2szCCRbQUEwB6WlfjW_yn5aR",
                version = "1.0",
                lastUpdated = "2024-01-10"
            ),
            QuestionPack(
                id = "3",
                name = "Bank Soal Kelas 3 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 3 SD",
                category = "KELAS_3",
                questionCount = 140,
                fileId = "1nY6HHLNviCEbQa1k98FHGNgaWJwuuVlC",
                version = "1.0",
                lastUpdated = "2024-01-12"
            ),
            QuestionPack(
                id = "4",
                name = "Bank Soal Kelas 4 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 4 SD",
                category = "KELAS_4",
                questionCount = 160,
                fileId = "1-cUWcFiCx92iVYZAVEyL8XQP4UWLrp6C",
                version = "1.0",
                lastUpdated = "2024-01-08"
            ),
            QuestionPack(
                id = "5",
                name = "Bank Soal Kelas 5 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 5 SD",
                category = "KELAS_5",
                questionCount = 180,
                fileId = "17go8KNxIuI7J9y32Z4RX4uCU-2MJR4Qu",
                version = "1.0",
                lastUpdated = "2024-01-20"
            ),
            QuestionPack(
                id = "6",
                name = "Bank Soal Kelas 6 SD",
                description = "Kurikulum Merdeka - Semua mata pelajaran untuk kelas 6 SD",
                category = "KELAS_6",
                questionCount = 200,
                fileId = "1h_8vw7Kf8LWkP0OxUeGv8DMZnThFeaeC",
                version = "1.0",
                lastUpdated = "2024-01-18"
            )
        )
    }
}
