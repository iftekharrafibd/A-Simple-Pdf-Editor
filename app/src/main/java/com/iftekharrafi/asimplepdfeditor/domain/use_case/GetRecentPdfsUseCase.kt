package com.iftekharrafi.asimplepdfeditor.domain.use_case

import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves all recently opened PDFs, ordered by most recent first.
 */
class GetRecentPdfsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    operator fun invoke(): Flow<List<RecentPdf>> {
        return repository.getAllRecentPdfs()
    }
}
