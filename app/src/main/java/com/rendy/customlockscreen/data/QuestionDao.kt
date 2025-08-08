package com.rendy.customlockscreen.data

import androidx.room.*

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<Question>

    @Query("SELECT * FROM questions WHERE category = :category")
    suspend fun getQuestionsByCategory(category: QuestionCategory): List<Question>

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuestion(): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()
}
