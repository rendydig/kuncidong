package com.rendy.customlockscreen.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.models.QuestionPack

class QuestionPackAdapter(
    private val onDownloadClick: (QuestionPack) -> Unit
) : RecyclerView.Adapter<QuestionPackAdapter.QuestionPackViewHolder>() {
    
    private var questionPacks = listOf<QuestionPack>()
    
    fun updatePacks(packs: List<QuestionPack>) {
        questionPacks = packs
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionPackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_pack, parent, false)
        return QuestionPackViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: QuestionPackViewHolder, position: Int) {
        holder.bind(questionPacks[position])
    }
    
    override fun getItemCount(): Int = questionPacks.size
    
    inner class QuestionPackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPack: MaterialCardView = itemView.findViewById(R.id.cardPack)
        private val tvPackName: MaterialTextView = itemView.findViewById(R.id.tvPackName)
        private val tvPackDescription: MaterialTextView = itemView.findViewById(R.id.tvPackDescription)
        private val tvPackInfo: MaterialTextView = itemView.findViewById(R.id.tvPackInfo)
        private val tvPackCategory: MaterialTextView = itemView.findViewById(R.id.tvPackCategory)
        private val btnDownload: MaterialButton = itemView.findViewById(R.id.btnDownload)
        
        fun bind(questionPack: QuestionPack) {
            tvPackName.text = questionPack.name
            tvPackDescription.text = questionPack.description
            tvPackInfo.text = itemView.context.getString(
                R.string.pack_item_info_format,
                questionPack.questionCount,
                questionPack.version,
                questionPack.lastUpdated
            )
            tvPackCategory.text = getCategoryEmoji(questionPack.category) + " " + questionPack.category
            
            btnDownload.setOnClickListener {
                onDownloadClick(questionPack)
            }
            
            // Set category-based colors for grade levels
            val categoryColor = when (questionPack.category) {
                "MATEMATIKA" -> R.color.category_math
                "ILMU PENGETAHUAN" -> R.color.category_science
                "GEOGRAFI" -> R.color.category_geography
                "SEJARAH" -> R.color.category_history
                "BAHASA" -> R.color.category_language
                else -> R.color.primary_blue
            }
            
            tvPackCategory.setBackgroundResource(categoryColor)
        }
        
        private fun getCategoryEmoji(category: String): String {
            return when (category) {
                "MATEMATIKA" -> "🔢"
                "ILMU PENGETAHUAN" -> "🔬"
                "GEOGRAFI" -> "🌍"
                "SEJARAH" -> "📜"
                "BAHASA" -> "📚"
                else -> "❓"
            }
        }
    }
}
