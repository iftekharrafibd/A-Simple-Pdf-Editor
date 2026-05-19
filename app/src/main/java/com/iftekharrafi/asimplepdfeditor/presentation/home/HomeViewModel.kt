package com.iftekharrafi.asimplepdfeditor.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.use_case.AddRecentPdfUseCase
import com.iftekharrafi.asimplepdfeditor.domain.use_case.DeleteRecentPdfUseCase
import com.iftekharrafi.asimplepdfeditor.domain.use_case.GetRecentPdfsUseCase
import com.iftekharrafi.asimplepdfeditor.utils.getFileName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentPdfsUseCase: GetRecentPdfsUseCase,
    private val addRecentPdfUseCase: AddRecentPdfUseCase,
    private val deleteRecentPdfUseCase: DeleteRecentPdfUseCase
) : ViewModel() {

    // ডেটাবেস থেকে সব রিসেন্ট ফাইল নিয়ে এসে StateFlow তে কনভার্ট করা হচ্ছে
    // এতে ডেটাবেস আপডেট হলে UI অটোমেটিক আপডেট হবে!
    val recentPdfs = getRecentPdfsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // নতুন ফাইল ডেটাবেসে সেভ করার ফাংশন
    fun addRecentFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                // ১. Persistable Permission নেওয়া (খুবই গুরুত্বপূর্ণ!)
                if (uri.scheme == "content") {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                }

                // ২. ডাটবেসে ইনসার্ট (via use case)
                val fileName = uri.getFileName(context)
                val newPdf = RecentPdf(
                    fileUri = uri.toString(),
                    fileName = fileName,
                    lastOpened = System.currentTimeMillis()
                )
                addRecentPdfUseCase(newPdf)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // পিডিএফ শেয়ার করার ফাংশন
    fun sharePdf(pdf: RecentPdf, context: Context) {
        try {
            val file = java.io.File(context.filesDir, pdf.fileName)
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "File does not exist!", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error sharing: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // পিডিএফ এক্সপোর্ট করার ফাংশন
    fun exportPdf(pdf: RecentPdf, context: Context) {
        try {
            val file = java.io.File(context.filesDir, pdf.fileName)
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "File does not exist!", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, pdf.fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/AmarPDF")
            }

            val targetUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (targetUri != null) {
                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                android.widget.Toast.makeText(context, "Exported successfully to Downloads/AmarPDF", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to export PDF", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error exporting: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // পিডিএফ ডিলিট করার ফাংশন
    fun deletePdf(pdf: RecentPdf, context: Context) {
        viewModelScope.launch {
            try {
                // Delete from private internal storage if it exists there
                val file = java.io.File(context.filesDir, pdf.fileName)
                if (file.exists()) {
                    file.delete()
                }
                // Delete entry from database
                deleteRecentPdfUseCase(pdf)
                android.widget.Toast.makeText(context, "Deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error deleting: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}