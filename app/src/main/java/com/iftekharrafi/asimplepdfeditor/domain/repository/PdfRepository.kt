package com.iftekharrafi.asimplepdfeditor.domain.repository

import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    fun getAllRecentPdfs(): Flow<List<RecentPdf>>
    suspend fun insertPdf(recentPdf: RecentPdf)
    suspend fun deletePdf(recentPdf: RecentPdf)
}