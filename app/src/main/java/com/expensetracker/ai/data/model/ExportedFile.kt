package com.expensetracker.ai.data.model

data class ExportedFile(
    val id: String,
    val name: String,
    val createdTime: Long,
    val webViewLink: String? = null
)
