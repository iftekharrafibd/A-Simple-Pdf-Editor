package com.iftekharrafi.asimplepdfeditor.domain.use_case

import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Adds or updates a PDF entry in the recent files list.
 */
class AddRecentPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(recentPdf: RecentPdf) {
        repository.insertPdf(recentPdf)
    }
}
