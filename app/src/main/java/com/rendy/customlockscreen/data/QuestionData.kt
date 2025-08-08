package com.rendy.customlockscreen.data

import com.google.gson.annotations.SerializedName

data class QuestionData(
    @SerializedName("questions")
    val questions: List<QuestionJson>
)

data class QuestionJson(
    @SerializedName("id")
    val id: Int,
    @SerializedName("category")
    val category: String,
    @SerializedName("question")
    val question: String,
    @SerializedName("options")
    val options: List<String>,
    @SerializedName("correctAnswerIndex")
    val correctAnswerIndex: Int
) {
    fun toQuestion(): Question {
        return Question(
            id = id,
            category = QuestionCategory.valueOf(category),
            question = question,
            options = options,
            correctAnswerIndex = correctAnswerIndex
        )
    }
}
