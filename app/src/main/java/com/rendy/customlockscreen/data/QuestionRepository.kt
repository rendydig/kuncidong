package com.rendy.customlockscreen.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class QuestionRepository(private val context: Context) {
    
    private val database = QuestionDatabase.getDatabase(context)
    private val questionDao = database.questionDao()
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("question_prefs", Context.MODE_PRIVATE)
    
    private fun loadQuestionsFromAssets(): List<Question> {
        return try {
            val inputStream = context.assets.open("questions.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val questionData = gson.fromJson(jsonString, QuestionData::class.java)
            questionData.questions.map { it.toQuestion() }
        } catch (e: IOException) {
            // Fallback to default questions if JSON file is not found
            getDefaultQuestions()
        } catch (e: Exception) {
            // Fallback to default questions if parsing fails
            getDefaultQuestions()
        }
    }
    
    private fun getDefaultQuestions(): List<Question> {
        return listOf(
            Question(1, QuestionCategory.MATHEMATICS, "Berapa hasil dari 1 + 10?", 
                listOf("11", "10", "12", "9"), 0),
            Question(2, QuestionCategory.MATHEMATICS, "Berapa hasil dari 2 + 9?", 
                listOf("11", "10", "12", "13"), 0)
        )
    }
    
    suspend fun initializeQuestions() {
        withContext(Dispatchers.IO) {
            val isFirstTime = sharedPreferences.getBoolean("is_first_time", true)
            if (isFirstTime) {
                val questions = loadQuestionsFromAssets()
                questionDao.insertQuestions(questions)
                sharedPreferences.edit().putBoolean("is_first_time", false).apply()
            }
        }
    }
    
    suspend fun getRandomQuestion(): Question? {
        return withContext(Dispatchers.IO) {
            val count = questionDao.getQuestionCount()
            if (count == 0) {
                initializeQuestions()
            }
            questionDao.getRandomQuestion()
        }
    }
    
    suspend fun getQuestionsByCategory(category: QuestionCategory): List<Question> {
        return withContext(Dispatchers.IO) {
            questionDao.getQuestionsByCategory(category)
        }
    }
    
    suspend fun getAllQuestions(): List<Question> {
        return withContext(Dispatchers.IO) {
            questionDao.getAllQuestions()
        }
    }
    
    suspend fun addCustomQuestion(question: Question) {
        withContext(Dispatchers.IO) {
            questionDao.insertQuestions(listOf(question))
        }
    }
    
    suspend fun resetQuestions() {
        withContext(Dispatchers.IO) {
            questionDao.deleteAllQuestions()
            val questions = loadQuestionsFromAssets()
            questionDao.insertQuestions(questions)
        }
    }
}
