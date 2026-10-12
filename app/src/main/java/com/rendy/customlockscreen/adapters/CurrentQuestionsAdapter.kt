package com.rendy.customlockscreen.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.data.CategoryVisuals
import com.rendy.customlockscreen.data.Question

class CurrentQuestionsAdapter : RecyclerView.Adapter<CurrentQuestionsAdapter.QuestionViewHolder>() {
    
    private var questions = listOf<Question>()
    
    fun updateQuestions(newQuestions: List<Question>) {
        questions = newQuestions
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_current_question, parent, false)
        return QuestionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position + 1)
    }
    
    override fun getItemCount(): Int = questions.size
    
    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuestionNumber: MaterialTextView = itemView.findViewById(R.id.tvQuestionNumber)
        private val tvQuestionText: MaterialTextView = itemView.findViewById(R.id.tvQuestionText)
        private val tvQuestionCategory: MaterialTextView = itemView.findViewById(R.id.tvQuestionCategory)
        private val tvCorrectAnswer: MaterialTextView = itemView.findViewById(R.id.tvCorrectAnswer)
        
        fun bind(question: Question, number: Int) {
            val context = itemView.context
            tvQuestionNumber.text = context.getString(R.string.question_item_number_format, number)
            tvQuestionText.text = question.questionText
            tvQuestionCategory.text = question.category.name
            tvQuestionCategory.setCompoundDrawablesRelativeWithIntrinsicBounds(
                CategoryVisuals.iconRes(question.category.name), 0, 0, 0
            )
            
            val correctAnswer = if (question.correctAnswerIndex < question.options.size) {
                question.options[question.correctAnswerIndex]
            } else {
                context.getString(R.string.question_item_answer_invalid)
            }
            tvCorrectAnswer.text = context.getString(R.string.question_item_answer_format, correctAnswer)
            
            // Set category-based colors
            tvQuestionCategory.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, CategoryVisuals.colorRes(question.category.name))
            )
        }
    }
}
