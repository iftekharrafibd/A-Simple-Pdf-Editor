package com.iftekharrafi.asimplepdfeditor.domain.repository

import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    fun getAllRecentPdfs(): Flow<List<PdfEntity>>
    suspend fun insertPdf(pdf: PdfEntity)
    suspend fun deletePdf(pdf: PdfEntity)
}