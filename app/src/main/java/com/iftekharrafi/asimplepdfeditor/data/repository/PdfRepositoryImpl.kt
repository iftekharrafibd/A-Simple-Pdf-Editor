package com.iftekharrafi.asimplepdfeditor.data.repository

import com.iftekharrafi.asimplepdfeditor.data.local.dao.PdfDao
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val dao: PdfDao
) : PdfRepository {

    override fun getAllRecentPdfs(): Flow<List<RecentPdf>> {
        return dao.getAllRecentPdfs().map { entities ->
            entities.map { it.toRecentPdf() }
        }
    }

    override suspend fun insertPdf(recentPdf: RecentPdf) {
        dao.insertPdf(recentPdf.toEntity())
    }

    override suspend fun deletePdf(recentPdf: RecentPdf) {
        dao.deletePdf(recentPdf.toEntity())
    }

    // --- Entity ↔ Domain Mapping ---

    private fun PdfEntity.toRecentPdf() = RecentPdf(
        fileUri = fileUri,
        fileName = fileName,
        lastOpened = lastOpened
    )

    private fun RecentPdf.toEntity() = PdfEntity(
        fileUri = fileUri,
        fileName = fileName,
        lastOpened = lastOpened
    )
}