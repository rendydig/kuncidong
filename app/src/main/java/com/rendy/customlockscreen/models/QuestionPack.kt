package com.rendy.customlockscreen.models

data class QuestionPack(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val questionCount: Int,
    val fileId: String, // Google Drive file ID
    val version: String,
    val lastUpdated: String
)
