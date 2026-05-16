package com.iftekharrafi.asimplepdfeditor.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.use_case.AddRecentPdfUseCase
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
    private val addRecentPdfUseCase: AddRecentPdfUseCase
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
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

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
}