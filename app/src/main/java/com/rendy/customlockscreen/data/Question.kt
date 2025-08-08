package com.rendy.customlockscreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "questions")
@TypeConverters(Converters::class)
data class Question(
    @PrimaryKey
    val id: Int,
    val category: QuestionCategory,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String? = null
)

enum class QuestionCategory(val displayName: String) {
    MATHEMATICS("Matematika"),
    SCIENCE("IPA (Ilmu Pengetahuan Alam)"),
    GEOGRAPHY("IPS (Geografi)"),
    HISTORY("IPS (Sejarah)"),
    LANGUAGE("Bahasa Indonesia"),
    GENERAL_KNOWLEDGE("Pengetahuan Umum")
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromQuestionCategory(category: QuestionCategory): String {
        return category.name
    }

    @TypeConverter
    fun toQuestionCategory(category: String): QuestionCategory {
        return QuestionCategory.valueOf(category)
    }
}
