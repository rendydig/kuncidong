package com.rendy.customlockscreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A reusable message the Parent can send to a Kids device in text lock mode. */
@Entity(tableName = "message_templates")
data class MessageTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String
)
