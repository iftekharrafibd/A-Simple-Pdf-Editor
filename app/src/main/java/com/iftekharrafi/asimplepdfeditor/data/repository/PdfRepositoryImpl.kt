package com.iftekharrafi.asimplepdfeditor.data.repository

import com.iftekharrafi.asimplepdfeditor.data.local.dao.PdfDao
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// @Inject constructor এর মাধ্যমে Hilt অটোমেটিকভাবে PdfDao সাপ্লাই দেবে
class PdfRepositoryImpl @Inject constructor(
    private val dao: PdfDao
) : PdfRepository {

    override fun getAllRecentPdfs(): Flow<List<PdfEntity>> {
        return dao.getAllRecentPdfs()
    }

    override suspend fun insertPdf(pdf: PdfEntity) {
        dao.insertPdf(pdf)
    }

    override suspend fun deletePdf(pdf: PdfEntity) {
        dao.deletePdf(pdf)
    }
}