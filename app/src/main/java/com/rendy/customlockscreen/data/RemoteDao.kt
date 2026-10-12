package com.rendy.customlockscreen.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteDao {

    // region paired parents

    @Query("SELECT * FROM paired_parents ORDER BY pairedAt DESC")
    suspend fun getAllParents(): List<PairedParent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParents(parents: List<PairedParent>)

    @Query("DELETE FROM paired_parents")
    suspend fun deleteAllParents()

    // endregion

    // region message templates

    @Query("SELECT * FROM message_templates ORDER BY id DESC")
    suspend fun getAllTemplates(): List<MessageTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: MessageTemplate)

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun templateCount(): Int

    @Query("DELETE FROM message_templates")
    suspend fun deleteAllTemplates()

    // endregion
}
