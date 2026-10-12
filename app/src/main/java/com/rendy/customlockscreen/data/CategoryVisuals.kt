package com.rendy.customlockscreen.data

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.rendy.customlockscreen.R

/**
 * Category colour + icon mapping, shared by the question and question-pack adapters.
 */
object CategoryVisuals {

    @ColorRes
    fun colorRes(category: String): Int = when (category) {
        "MATEMATIKA" -> R.color.category_math
        "ILMU PENGETAHUAN" -> R.color.category_science
        "GEOGRAFI" -> R.color.category_geography
        "SEJARAH" -> R.color.category_history
        "BAHASA" -> R.color.category_language
        else -> R.color.primary
    }

    @DrawableRes
    fun iconRes(category: String): Int = when (category) {
        "MATEMATIKA" -> R.drawable.ic_calculate
        "ILMU PENGETAHUAN" -> R.drawable.ic_science
        "GEOGRAFI" -> R.drawable.ic_public
        "SEJARAH" -> R.drawable.ic_history_edu
        "BAHASA" -> R.drawable.ic_menu_book
        else -> R.drawable.ic_help
    }
}
