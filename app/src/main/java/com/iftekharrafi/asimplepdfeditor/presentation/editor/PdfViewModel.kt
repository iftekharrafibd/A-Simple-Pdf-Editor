package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    // ৩. শুধুমাত্র একটি StateFlow
    private val _state = MutableStateFlow(PdfEditorState())
    val state = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null

    fun loadPdf(uri: Uri) {
        // লোডিং শুরু করার আগে আগের পিডিএফ ক্লিয়ার করে দাও (Bug 1 Fix)
        _state.update { PdfEditorState() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext

                // ফাইলটা আদেও আছে কি না বা পারমিশন আছে কি না চেক করা (Bug 4 Fix)
                fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw Exception("ফাইলটি খুঁজে পাওয়া যাচ্ছে না বা পারমিশন নেই")

                pdfRenderer = PdfRenderer(fileDescriptor!!)
                _state.update {
                    it.copy(
                        pageCount = pdfRenderer!!.pageCount,
                        currentPageIndex = 0
                    )
                }
                renderPage(0)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    // ফাইল না পেলে বা ডিলিট হয়ে গেলে চাইলে এখান থেকে ডাটবেস থেকেও ডিলিট করে দিতে পারো
                }
            }
        }
    }
    private fun renderPage(pageIndex: Int) {
        pdfRenderer?.let { renderer ->
            if (pageIndex < renderer.pageCount) {
                currentPage?.close()
                val page = renderer.openPage(pageIndex)
                currentPage = page

                val bitmap = createBitmap(page.width * 2, page.height * 2)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // State আপডেট (একসাথে ছবি চেঞ্জ এবং টেক্সট ক্লিয়ার)
                _state.update {
                    it.copy(
                        pdfBitmap = bitmap,
                        textOverlay = TextOverlay() // পেজ পাল্টালে টেক্সট রিসেট হবে
                    )
                }
            }
        }
    }

    fun nextPage() {
        val currentState = _state.value
        if (currentState.currentPageIndex < currentState.pageCount - 1) {
            val nextIndex = currentState.currentPageIndex + 1
            _state.update { it.copy(currentPageIndex = nextIndex) }
            renderPage(nextIndex)
        }
    }

    fun previousPage() {
        val currentState = _state.value
        if (currentState.currentPageIndex > 0) {
            val prevIndex = currentState.currentPageIndex - 1
            _state.update { it.copy(currentPageIndex = prevIndex) }
            renderPage(prevIndex)
        }
    }

    fun onTextChanged(newText: String) {
        _state.update { it.copy(textOverlay = it.textOverlay.copy(text = newText)) }
    }

    fun onColorChanged(newColor: Color) {
        _state.update { it.copy(textOverlay = it.textOverlay.copy(color = newColor)) }
    }

    fun onTextTransformed(panX: Float, panY: Float, zoomChange: Float, rotationChange: Float) {
        _state.update { currentState ->
            val currentOverlay = currentState.textOverlay
            currentState.copy(
                textOverlay = currentOverlay.copy(
                    offsetX = currentOverlay.offsetX + panX,
                    offsetY = currentOverlay.offsetY + panY,
                    scale = (currentOverlay.scale * zoomChange).coerceIn(0.5f, 5f),
                    rotation = currentOverlay.rotation + rotationChange
                )
            )
        }
    }

    // টুল সিলেক্ট করা এবং বটম শিট ওপেন করার ফাংশন
    fun selectTool(tool: EditorTool) {
        _state.update {
            it.copy(
                selectedTool = tool,
                isBottomSheetVisible = true // টুল সিলেক্ট করলেই বটম শিট ওপেন হবে
            )
        }
    }

    // বটম শিট বন্ধ করার ফাংশন
    fun dismissBottomSheet() {
        _state.update {
            it.copy(isBottomSheetVisible = false)
        }
    }
// --- ড্রয়িং কন্ট্রোল ফাংশন ---

    // নতুন স্ট্রোক অ্যাড করা
    fun addStroke(stroke: DrawingStroke) {
        _state.update { it.copy(drawnStrokes = it.drawnStrokes + stroke) }
    }

    // ব্রাশের কালার চেঞ্জ করা
    fun setBrushColor(color: Color) {
        _state.update { it.copy(brushColor = color) }
    }

    // ভুল করে কিছু আঁকলে আনডু (Undo) করা
    fun undoLastStroke() {
        val currentStrokes = _state.value.drawnStrokes
        if (currentStrokes.isNotEmpty()) {
            _state.update { it.copy(drawnStrokes = currentStrokes.dropLast(1)) }
        }
    }
    fun saveEntirePdf(editedBitmap: Bitmap, editedPageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isSaving = true) } // সেভিং শুরু

            try {
                val context = getApplication<Application>().applicationContext
                val newPdfDocument = PdfDocument()

                if (pdfRenderer == null) throw Exception("PDF রেন্ডারার চালু নেই")

                val totalPages = pdfRenderer!!.pageCount
// ১. লুপ চালিয়ে এক এক করে সব পেজ প্রসেস করা
                for (i in 0 until totalPages) {
                    if (i == editedPageIndex) {

                        // --- নতুন: Hardware Bitmap কে Software Bitmap এ কনভার্ট করা ---
                        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && editedBitmap.config == Bitmap.Config.HARDWARE) {
                            editedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        } else {
                            editedBitmap
                        }

                        // কনভার্ট করা ছবি দিয়ে পিডিএফের পেজ তৈরি করা
                        val pageInfo = PdfDocument.PageInfo.Builder(softwareBitmap.width, softwareBitmap.height, i + 1).create()
                        val page = newPdfDocument.startPage(pageInfo)

                        // এখন আর ক্র্যাশ করবে না!
                        page.canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
                        newPdfDocument.finishPage(page)

                        // মেমরি লিক রোধ করতে তৈরি করা সফটওয়্যার বিটম্যাপ মুছে ফেলা
                        if (softwareBitmap != editedBitmap) {
                            softwareBitmap.recycle()
                        }
                        // -----------------------------------------------------------

                    } else {
                        // ৩. যদি অন্য পেজ হয়, তবে অরিজিনাল পিডিএফ থেকে রেন্ডার করে বসাবো
                        val originalPage = pdfRenderer!!.openPage(i)
// ... (লুপের ভেতরের বাকি কোড আগের মতোই থাকবে) ...
                        val width = originalPage.width * 2
                        val height = originalPage.height * 2
                        val pageBitmap = createBitmap(width, height)
                        pageBitmap.eraseColor(android.graphics.Color.WHITE)
                        originalPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                        val page = newPdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                        newPdfDocument.finishPage(page)

                        originalPage.close()
                        pageBitmap.recycle()
                    }
                }

                val fileName = "AmarPDF_Pro_${System.currentTimeMillis()}.pdf"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AmarPDF")
                    }

                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            newPdfDocument.writeTo(outputStream)
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val folder = File(downloadsDir, "AmarPDF")
                    if (!folder.exists()) folder.mkdirs()

                    val file = File(folder, fileName)
                    FileOutputStream(file).use { outputStream ->
                        newPdfDocument.writeTo(outputStream)
                    }
                }

                newPdfDocument.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "পুরো PDF সেভ হয়েছে! (Downloads)", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(application.applicationContext, "সেভ করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _state.update { it.copy(isSaving = false) } // সেভিং শেষ
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentPage?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}