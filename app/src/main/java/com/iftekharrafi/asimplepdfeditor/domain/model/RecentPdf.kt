package com.iftekharrafi.asimplepdfeditor.domain.model

/**
 * Domain model representing a recently opened PDF file.
 * This keeps the domain layer independent of Room's PdfEntity.
 */
data class RecentPdf(
    val fileUri: String,
    val fileName: String,
    val lastOpened: Long
)
