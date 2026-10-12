package com.rendy.customlockscreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A parent device paired with this Kids device, kept for display in the Kids UI.
 * The auth token itself is never stored in clear text, only its hash.
 */
@Entity(tableName = "paired_parents")
data class PairedParent(
    @PrimaryKey
    val id: String,
    val name: String,
    val tokenHash: String,
    val pairedAt: Long
)
