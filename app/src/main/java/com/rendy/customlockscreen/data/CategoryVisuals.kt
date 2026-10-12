package com.rendy.customlockscreen.data

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
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

    /**
     * Puts the category icon on a `text_xs` badge at the `icon_sm` size, so the
     * icon matches the badge text instead of towering over it at its 24dp
     * intrinsic size. The badge's tint comes from `app:drawableTint` in the layout.
     */
    fun applyBadgeIcon(textView: TextView, category: String) {
        val icon = ContextCompat.getDrawable(textView.context, iconRes(category)) ?: return
        val size = textView.resources.getDimensionPixelSize(R.dimen.icon_sm)
        icon.setBounds(0, 0, size, size)
        textView.setCompoundDrawablesRelative(icon, null, null, null)
    }
}
