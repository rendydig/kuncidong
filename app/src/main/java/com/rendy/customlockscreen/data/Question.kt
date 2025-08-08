package com.rendy.customlockscreen.data

data class Question(
    val id: Int,
    val category: QuestionCategory,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String? = null
)

enum class QuestionCategory(val displayName: String) {
    MATHEMATICS("Mathematics"),
    SCIENCE("Science"),
    GEOGRAPHY("Geography"),
    HISTORY("History"),
    LANGUAGE("Language"),
    GENERAL_KNOWLEDGE("General Knowledge")
}
